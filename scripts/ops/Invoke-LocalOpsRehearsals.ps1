param(
    [string]$ProjectRoot = (Join-Path $PSScriptRoot '..\..')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
$reportRoot = Join-Path $resolvedProjectRoot 'build\ops-rehearsal'
$logRoot = Join-Path $reportRoot 'logs'
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$reportPath = Join-Path $reportRoot "local-ops-rehearsal-$timestamp.md"
$scrubSqlPath = Join-Path $reportRoot "scrub-rehearsal-$timestamp.sql"
$tempPasswordPath = Join-Path $reportRoot "ops-reset-temp-password-$timestamp.txt"

$dbUrl = 'jdbc:mysql://localhost:3307/bootsync?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Seoul&characterEncoding=UTF-8'
$dbUsername = 'bootsync'
$dbPassword = 'bootsync'
$bootRunArgs = '--args=--spring.profiles.active=local --server.port=18080'

$seedUsernameReset = 'ops_reset_target'
$seedUsernamePurge = 'ops_purge_due_target'
$seedUsernameScrub = 'ops_scrub_target'
$temporaryPassword = 'ops-reset-after-456'

New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null
Set-Content -Path $tempPasswordPath -Value $temporaryPassword -Encoding UTF8

function Invoke-InProject {
    param(
        [scriptblock]$ScriptBlock
    )

    Push-Location $resolvedProjectRoot
    try {
        & $ScriptBlock
    } finally {
        Pop-Location
    }
}

function Invoke-DockerComposeMysql {
    $logPath = Join-Path $logRoot "docker-compose-up-$timestamp.log"
    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        Invoke-InProject {
            & docker compose up -d mysql 2>&1 | Tee-Object -FilePath $logPath | Out-Null
            if ($LASTEXITCODE -ne 0) {
                throw "docker compose up failed"
            }
        }
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    return $logPath
}

function Wait-ForMysql {
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
        & docker exec bootsync-mysql mysqladmin ping -h localhost -proot 2>$null | Out-Null
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
        if ($LASTEXITCODE -eq 0) {
            return
        }
        Start-Sleep -Seconds 2
    }
    throw 'MySQL container did not become ready in time.'
}

function Invoke-MySqlScalar {
    param(
        [string]$Sql
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $result = & docker exec bootsync-mysql mysql -N -B "-u$dbUsername" "-p$dbPassword" bootsync -e $Sql
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -ne 0) {
        throw "MySQL query failed: $Sql"
    }
    return (($result | Out-String).Trim())
}

function Invoke-MySqlScript {
    param(
        [string]$Sql
    )

    $previousErrorActionPreference = $ErrorActionPreference
    $ErrorActionPreference = 'Continue'
    try {
        $Sql | & docker exec -i bootsync-mysql mysql "-u$dbUsername" "-p$dbPassword" bootsync
    } finally {
        $ErrorActionPreference = $previousErrorActionPreference
    }
    if ($LASTEXITCODE -ne 0) {
        throw 'MySQL script execution failed.'
    }
}

function Invoke-BootRun {
    param(
        [hashtable]$Environment,
        [string]$LogName
    )

    $logPath = Join-Path $logRoot $LogName
    $previousValues = @{}

    foreach ($key in $Environment.Keys) {
        $current = Get-Item "Env:$key" -ErrorAction SilentlyContinue
        $previousValues[$key] = if ($null -ne $current) { $current.Value } else { $null }
        Set-Item "Env:$key" ([string]$Environment[$key])
    }

    try {
        $previousErrorActionPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        try {
            Invoke-InProject {
                & .\gradlew.bat bootRun $bootRunArgs 2>&1 | Tee-Object -FilePath $logPath | Out-Null
                if ($LASTEXITCODE -ne 0) {
                    throw "bootRun failed for $LogName"
                }
            }
        } finally {
            $ErrorActionPreference = $previousErrorActionPreference
        }
    } finally {
        foreach ($key in $Environment.Keys) {
            if ($null -eq $previousValues[$key]) {
                Remove-Item "Env:$key" -ErrorAction SilentlyContinue
            } else {
                Set-Item "Env:$key" $previousValues[$key]
            }
        }
    }

    return $logPath
}

function Merge-Environment {
    param(
        [hashtable]$Base,
        [hashtable]$Overrides
    )

    $merged = @{}
    foreach ($key in $Base.Keys) {
        $merged[$key] = $Base[$key]
    }
    foreach ($key in $Overrides.Keys) {
        $merged[$key] = $Overrides[$key]
    }
    return $merged
}

function Get-CompletionLine {
    param(
        [string]$LogPath,
        [string]$Pattern
    )

    $match = Select-String -Path $LogPath -Pattern $Pattern | Select-Object -Last 1
    if ($null -eq $match) {
        throw "Expected log pattern not found in ${LogPath}: $Pattern"
    }
    return $match.Line.Trim()
}

function Get-EntityCounts {
    param(
        [string]$MemberId
    )

    return [ordered]@{
        member = Invoke-MySqlScalar "SELECT COUNT(*) FROM member WHERE id = $MemberId;"
        attendance_record = Invoke-MySqlScalar "SELECT COUNT(*) FROM attendance_record WHERE member_id = $MemberId;"
        attendance_audit_log_active = Invoke-MySqlScalar "SELECT COUNT(*) FROM attendance_audit_log WHERE member_id = $MemberId OR changed_by_member_id = $MemberId;"
        snippet = Invoke-MySqlScalar "SELECT COUNT(*) FROM snippet WHERE member_id = $MemberId;"
        snippet_tag = Invoke-MySqlScalar "SELECT COUNT(*) FROM snippet_tag WHERE member_id = $MemberId;"
        tag = Invoke-MySqlScalar "SELECT COUNT(*) FROM tag WHERE member_id = $MemberId;"
        recovery_email_verification_token = Invoke-MySqlScalar "SELECT COUNT(*) FROM recovery_email_verification_token WHERE member_id = $MemberId;"
    }
}

function Format-Counts {
    param(
        [hashtable]$Counts
    )

    return ($Counts.GetEnumerator() | ForEach-Object { "- $($_.Key): $($_.Value)" }) -join [Environment]::NewLine
}

$composeLogPath = Invoke-DockerComposeMysql
Wait-ForMysql

$commonEnvironment = @{
    'DB_URL' = $dbUrl
    'DB_USERNAME' = $dbUsername
    'DB_PASSWORD' = $dbPassword
}

$seedLogPath = Invoke-BootRun (
    (Merge-Environment -Base $commonEnvironment -Overrides @{
        'APP_OPERATIONS_REHEARSAL_SEED_ENABLED' = 'true'
        'APP_OPERATIONS_REHEARSAL_SEED_CLOSE_CONTEXT_AFTER_RUN' = 'true'
    })
) "seed-$timestamp.log"
$seedLogLine = Get-CompletionLine -LogPath $seedLogPath -Pattern 'BootSync ops rehearsal seed completed'

$resetBeforeUpdatedAt = Invoke-MySqlScalar "SELECT DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s.%f') FROM member WHERE username = '$seedUsernameReset';"
$passwordResetLogPath = Invoke-BootRun (
    (Merge-Environment -Base $commonEnvironment -Overrides @{
        'APP_OPERATIONS_PASSWORD_RESET_ENABLED' = 'true'
        'APP_OPERATIONS_PASSWORD_RESET_USERNAME' = $seedUsernameReset
        'APP_OPERATIONS_PASSWORD_RESET_TEMPORARY_PASSWORD_FILE' = $tempPasswordPath
        'APP_OPERATIONS_PASSWORD_RESET_ACTOR' = 'local-ops-admin'
        'APP_OPERATIONS_PASSWORD_RESET_REASON' = 'local password reset rehearsal'
        'APP_OPERATIONS_PASSWORD_RESET_CLOSE_CONTEXT_AFTER_RUN' = 'true'
    })
) "password-reset-$timestamp.log"
$resetAfterUpdatedAt = Invoke-MySqlScalar "SELECT DATE_FORMAT(updated_at, '%Y-%m-%d %H:%i:%s.%f') FROM member WHERE username = '$seedUsernameReset';"
$passwordResetLogLine = Get-CompletionLine -LogPath $passwordResetLogPath -Pattern 'BootSync operator password reset completed'

$purgeMemberId = Invoke-MySqlScalar "SELECT id FROM member WHERE username = '$seedUsernamePurge';"
$purgeAuditLogId = Invoke-MySqlScalar "SELECT id FROM attendance_audit_log WHERE member_id = $purgeMemberId ORDER BY id DESC LIMIT 1;"
$purgeBeforeCounts = Get-EntityCounts -MemberId $purgeMemberId
$purgeLogPath = Invoke-BootRun (
    (Merge-Environment -Base $commonEnvironment -Overrides @{
        'APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ENABLED' = 'true'
        'APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_ACTOR' = 'local-ops-admin'
        'APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_REASON' = 'local purge rehearsal'
        'APP_ACCOUNT_DELETION_PURGE_RUN_ONCE_CLOSE_CONTEXT_AFTER_RUN' = 'true'
    })
) "purge-$timestamp.log"
$purgeAfterCounts = Get-EntityCounts -MemberId $purgeMemberId
$purgeAuditState = Invoke-MySqlScalar "SELECT CONCAT(COALESCE(CAST(member_id AS CHAR), 'NULL'), '/', COALESCE(CAST(changed_by_member_id AS CHAR), 'NULL'), '/', COALESCE(request_ip_hmac, 'NULL')) FROM attendance_audit_log WHERE id = $purgeAuditLogId;"
$purgeLogLine = Get-CompletionLine -LogPath $purgeLogPath -Pattern 'BootSync one-shot account deletion purge completed'

$scrubMemberId = Invoke-MySqlScalar "SELECT id FROM member WHERE username = '$seedUsernameScrub';"
$scrubAuditLogId = Invoke-MySqlScalar "SELECT id FROM attendance_audit_log WHERE member_id = $scrubMemberId ORDER BY id DESC LIMIT 1;"
$scrubBeforeCounts = Get-EntityCounts -MemberId $scrubMemberId
$scrubSql = @"
DELETE FROM snippet_tag WHERE member_id = $scrubMemberId;
DELETE FROM snippet WHERE member_id = $scrubMemberId;
DELETE FROM tag WHERE member_id = $scrubMemberId;
DELETE FROM attendance_record WHERE member_id = $scrubMemberId;
DELETE FROM recovery_email_verification_token WHERE member_id = $scrubMemberId;
UPDATE attendance_audit_log
   SET attendance_record_id = NULL,
       member_id = NULL,
       changed_by_member_id = NULL,
       request_ip_hmac = NULL
 WHERE member_id = $scrubMemberId
    OR changed_by_member_id = $scrubMemberId;
DELETE FROM member WHERE id = $scrubMemberId;
"@
Set-Content -Path $scrubSqlPath -Value $scrubSql -Encoding UTF8
Invoke-MySqlScript -Sql $scrubSql
$scrubAfterCounts = Get-EntityCounts -MemberId $scrubMemberId
$scrubAuditState = Invoke-MySqlScalar "SELECT CONCAT(COALESCE(CAST(member_id AS CHAR), 'NULL'), '/', COALESCE(CAST(changed_by_member_id AS CHAR), 'NULL'), '/', COALESCE(request_ip_hmac, 'NULL')) FROM attendance_audit_log WHERE id = $scrubAuditLogId;"

$reportLines = @(
    '# BootSync Local Ops Rehearsal',
    '',
    "- executed_at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')",
    '- environment: local + docker compose mysql',
    "- docker_log: $composeLogPath",
    "- seed_log: $seedLogPath",
    "- password_reset_log: $passwordResetLogPath",
    "- purge_log: $purgeLogPath",
    "- scrub_sql: $scrubSqlPath",
    '',
    '## Seed',
    '',
    $seedLogLine,
    '',
    '## Password Reset Rehearsal',
    '',
    "- username: $seedUsernameReset",
    "- updated_at before: $resetBeforeUpdatedAt",
    "- updated_at after: $resetAfterUpdatedAt",
    "- temporary_password_file: $tempPasswordPath",
    $passwordResetLogLine,
    '',
    '## Purge One-Shot Rehearsal',
    '',
    "- username: $seedUsernamePurge",
    "- member_id: $purgeMemberId",
    '- counts_before:',
    (Format-Counts -Counts $purgeBeforeCounts),
    '- counts_after:',
    (Format-Counts -Counts $purgeAfterCounts),
    "- audit_row_after: $purgeAuditState",
    $purgeLogLine,
    '',
    '## Restore Scrub Rehearsal',
    '',
    "- username: $seedUsernameScrub",
    "- member_id: $scrubMemberId",
    '- counts_before:',
    (Format-Counts -Counts $scrubBeforeCounts),
    '- counts_after:',
    (Format-Counts -Counts $scrubAfterCounts),
    "- audit_row_after: $scrubAuditState",
    '- scrub_result: manual scrub SQL completed successfully'
)

Set-Content -Path $reportPath -Value $reportLines -Encoding UTF8
Write-Output "Local ops rehearsal report written to $reportPath"

[CmdletBinding()]
param(
    [string]$ProjectRoot,
    [string]$BackupDir,
    [ValidateSet('docker', 'tcp')]
    [string]$Mode = 'docker',
    [string]$ContainerName = 'bootsync-mysql',
    [string]$DatabaseName = 'bootsync',
    [string]$MySqlUser = $(if ([string]::IsNullOrWhiteSpace($env:DB_USERNAME)) { 'bootsync' } else { $env:DB_USERNAME }),
    [AllowEmptyString()]
    [string]$MySqlPassword,
    [string]$MySqlPasswordEnvVarName = 'MYSQL_PASSWORD',
    [string]$DbHost,
    [int]$DbPort = 3306,
    [string]$DbUrl = $env:DB_URL,
    [string]$MySqlSslMode = $env:MYSQL_SSL_MODE,
    [string]$Bucket = $env:BACKUP_S3_BUCKET,
    [string]$AwsRegion = $env:AWS_REGION,
    [string]$AwsProfile = $env:AWS_PROFILE,
    [string]$DailyPrefix = 'daily',
    [string]$WeeklyPrefix = 'weekly',
    [switch]$SkipUpload,
    [switch]$ForceWeekly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Join-Path (Split-Path -Parent $PSCommandPath) '..\..'
}
$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($BackupDir)) {
    $BackupDir = Join-Path $resolvedProjectRoot 'build\ops-backup'
}

$backupRoot = (Resolve-Path (New-Item -ItemType Directory -Force -Path $BackupDir)).Path
$logRoot = Join-Path $backupRoot 'logs'
$reportRoot = Join-Path $backupRoot 'reports'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null

function Require-Command {
    param(
        [string]$Name
    )

    if (-not (Get-Command $Name -ErrorAction SilentlyContinue)) {
        throw "필수 명령을 찾을 수 없습니다: $Name"
    }
}

function Write-Utf8NoBomFile {
    param(
        [string]$Path,
        [AllowNull()]
        [string]$Content
    )

    $encoding = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $encoding)
}

function Format-ProcessArgument {
    param(
        [AllowNull()]
        [string]$Value
    )

    if ($null -eq $Value) {
        return '""'
    }

    if ($Value -notmatch '[\s"]') {
        return $Value
    }

    $builder = New-Object System.Text.StringBuilder
    [void]$builder.Append('"')
    $pendingBackslashes = 0

    foreach ($character in $Value.ToCharArray()) {
        if ($character -eq '\') {
            $pendingBackslashes += 1
            continue
        }

        if ($character -eq '"') {
            [void]$builder.Append('\', ($pendingBackslashes * 2) + 1)
            [void]$builder.Append('"')
            $pendingBackslashes = 0
            continue
        }

        if ($pendingBackslashes -gt 0) {
            [void]$builder.Append('\', $pendingBackslashes)
            $pendingBackslashes = 0
        }
        [void]$builder.Append($character)
    }

    if ($pendingBackslashes -gt 0) {
        [void]$builder.Append('\', $pendingBackslashes * 2)
    }
    [void]$builder.Append('"')
    return $builder.ToString()
}

function Invoke-ExternalCommand {
    param(
        [string]$FilePath,
        [string[]]$ArgumentList,
        [string]$StandardOutputPath,
        [string]$StandardErrorPath,
        [hashtable]$EnvironmentVariables = @{}
    )

    Remove-Item -Path $StandardOutputPath, $StandardErrorPath -Force -ErrorAction SilentlyContinue
    $arguments = ($ArgumentList | ForEach-Object { Format-ProcessArgument -Value $_ }) -join ' '
    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $FilePath
    $startInfo.Arguments = $arguments
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true

    foreach ($entry in $EnvironmentVariables.GetEnumerator()) {
        $startInfo.Environment[$entry.Key] = [string]$entry.Value
    }

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    Write-Utf8NoBomFile -Path $StandardOutputPath -Content $stdout
    Write-Utf8NoBomFile -Path $StandardErrorPath -Content $stderr

    if ($process.ExitCode -ne 0) {
        throw "명령 실행이 실패했습니다: $FilePath $($ArgumentList -join ' ')`n$($stderr.Trim())"
    }
}

function Invoke-AwsS3Copy {
    param(
        [string]$SourcePath,
        [string]$S3Uri,
        [string]$LogPrefix
    )

    $stdoutPath = Join-Path $logRoot "$LogPrefix.stdout.log"
    $stderrPath = Join-Path $logRoot "$LogPrefix.stderr.log"
    $arguments = @('s3', 'cp', $SourcePath, $S3Uri, '--region', $AwsRegion)
    if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) {
        $arguments += @('--profile', $AwsProfile)
    }

    Invoke-ExternalCommand -FilePath 'aws' -ArgumentList $arguments -StandardOutputPath $stdoutPath -StandardErrorPath $stderrPath
    return @{
        stdout = $stdoutPath
        stderr = $stderrPath
    }
}

function Get-EnvironmentValue {
    param(
        [string]$Name
    )

    if ([string]::IsNullOrWhiteSpace($Name)) {
        return $null
    }

    $environmentItem = Get-Item -Path "Env:$Name" -ErrorAction SilentlyContinue
    if ($null -eq $environmentItem) {
        return $null
    }

    return [string]$environmentItem.Value
}

function Resolve-MySqlPassword {
    param(
        [bool]$PasswordProvided,
        [AllowEmptyString()]
        [string]$ExplicitPassword,
        [string]$EnvVarName
    )

    if ($PasswordProvided) {
        return $ExplicitPassword
    }

    $envPassword = Get-EnvironmentValue -Name $EnvVarName
    if ($null -ne $envPassword) {
        return $envPassword
    }

    $dbPassword = Get-EnvironmentValue -Name 'DB_PASSWORD'
    if ($null -ne $dbPassword) {
        return $dbPassword
    }

    throw "tcp 모드에서는 MySQL 비밀번호가 필요합니다. -MySqlPassword 또는 환경변수 $EnvVarName, DB_PASSWORD를 준비하세요."
}

function Parse-JdbcMySqlUrl {
    param(
        [string]$JdbcUrl
    )

    if ([string]::IsNullOrWhiteSpace($JdbcUrl)) {
        return $null
    }

    $match = [regex]::Match($JdbcUrl.Trim(), '^jdbc:mysql://(?<host>[^:/?#]+)(:(?<port>\d+))?/(?<database>[^?;]+)')
    if (-not $match.Success) {
        return $null
    }

    $port = if ($match.Groups['port'].Success) { [int]$match.Groups['port'].Value } else { 3306 }
    return @{
        host = $match.Groups['host'].Value
        port = $port
        database = $match.Groups['database'].Value
    }
}

function Resolve-TcpConnection {
    param(
        [string]$HostName,
        [int]$PortNumber,
        [string]$JdbcUrl,
        [string]$DefaultDatabaseName
    )

    $parsed = Parse-JdbcMySqlUrl -JdbcUrl $JdbcUrl
    $resolvedHost = $HostName
    $resolvedPort = $PortNumber
    $resolvedDatabaseName = $DefaultDatabaseName

    if ([string]::IsNullOrWhiteSpace($resolvedHost) -and $null -ne $parsed) {
        $resolvedHost = $parsed.host
        if ($PortNumber -eq 3306) {
            $resolvedPort = $parsed.port
        }
        if ([string]::IsNullOrWhiteSpace($DefaultDatabaseName) -and -not [string]::IsNullOrWhiteSpace($parsed.database)) {
            $resolvedDatabaseName = $parsed.database
        }
    }

    if ([string]::IsNullOrWhiteSpace($resolvedHost)) {
        throw 'tcp 모드에서는 -DbHost 또는 DB_URL 환경변수가 필요합니다.'
    }

    return @{
        host = $resolvedHost
        port = $resolvedPort
        database = $resolvedDatabaseName
    }
}

$passwordProvided = $PSBoundParameters.ContainsKey('MySqlPassword')

if ($Mode -eq 'docker') {
    Require-Command -Name 'docker'
} else {
    Require-Command -Name 'mysqldump'
}

if (-not $SkipUpload) {
    Require-Command -Name 'aws'
    if ([string]::IsNullOrWhiteSpace($Bucket)) {
        throw 'S3 업로드를 수행하려면 BACKUP_S3_BUCKET 또는 -Bucket 값이 필요합니다.'
    }
    if ([string]::IsNullOrWhiteSpace($AwsRegion)) {
        throw 'S3 업로드를 수행하려면 AWS_REGION 또는 -AwsRegion 값이 필요합니다.'
    }
}

$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$fileBase = "bootsync-$timestamp"
$dumpPath = Join-Path $backupRoot "$fileBase.sql"
$containerDumpPath = "/tmp/$fileBase.sql"
$dumpStdoutPath = Join-Path $logRoot "$fileBase-mysqldump.stdout.log"
$dumpStderrPath = Join-Path $logRoot "$fileBase-mysqldump.stderr.log"
$copyStdoutPath = Join-Path $logRoot "$fileBase-docker-cp.stdout.log"
$copyStderrPath = Join-Path $logRoot "$fileBase-docker-cp.stderr.log"
$cleanupStdoutPath = Join-Path $logRoot "$fileBase-cleanup.stdout.log"
$cleanupStderrPath = Join-Path $logRoot "$fileBase-cleanup.stderr.log"
$manifestPath = Join-Path $backupRoot "$fileBase.manifest.json"
$reportPath = Join-Path $reportRoot "$fileBase.md"

$reportTargetLabel = $ContainerName
$reportTargetType = 'container'
$connectionMetadata = @{
    mode = $Mode
    containerName = $null
    dbHost = $null
    dbPort = $null
    mySqlSslMode = $null
}

if ($Mode -eq 'docker') {
    $passwordReference = '$' + $MySqlPasswordEnvVarName
    $dumpCommand = "exec mysqldump -u$MySqlUser -p`"$passwordReference`" --databases $DatabaseName --single-transaction --routines --triggers --set-gtid-purged=OFF --default-character-set=utf8mb4 --no-tablespaces > $containerDumpPath"

    Invoke-ExternalCommand `
        -FilePath 'docker' `
        -ArgumentList @('exec', $ContainerName, 'sh', '-lc', $dumpCommand) `
        -StandardOutputPath $dumpStdoutPath `
        -StandardErrorPath $dumpStderrPath

    Invoke-ExternalCommand `
        -FilePath 'docker' `
        -ArgumentList @('cp', "${ContainerName}:$containerDumpPath", $dumpPath) `
        -StandardOutputPath $copyStdoutPath `
        -StandardErrorPath $copyStderrPath

    try {
        Invoke-ExternalCommand `
            -FilePath 'docker' `
            -ArgumentList @('exec', $ContainerName, 'rm', '-f', $containerDumpPath) `
            -StandardOutputPath $cleanupStdoutPath `
            -StandardErrorPath $cleanupStderrPath
    } catch {
        Write-Warning "컨테이너 임시 덤프 파일 정리에 실패했습니다: $containerDumpPath"
    }

    $connectionMetadata.containerName = $ContainerName
} else {
    $resolvedPassword = Resolve-MySqlPassword `
        -PasswordProvided $passwordProvided `
        -ExplicitPassword $MySqlPassword `
        -EnvVarName $MySqlPasswordEnvVarName
    $connection = Resolve-TcpConnection `
        -HostName $DbHost `
        -PortNumber $DbPort `
        -JdbcUrl $DbUrl `
        -DefaultDatabaseName $DatabaseName

    $dumpArguments = @(
        "--host=$($connection.host)",
        "--port=$($connection.port)",
        "--user=$MySqlUser",
        '--databases',
        $connection.database,
        '--single-transaction',
        '--routines',
        '--triggers',
        '--set-gtid-purged=OFF',
        '--default-character-set=utf8mb4',
        '--no-tablespaces',
        "--result-file=$dumpPath"
    )
    if (-not [string]::IsNullOrWhiteSpace($MySqlSslMode)) {
        $dumpArguments += "--ssl-mode=$MySqlSslMode"
    }

    Invoke-ExternalCommand `
        -FilePath 'mysqldump' `
        -ArgumentList $dumpArguments `
        -StandardOutputPath $dumpStdoutPath `
        -StandardErrorPath $dumpStderrPath `
        -EnvironmentVariables @{ MYSQL_PWD = $resolvedPassword }

    $reportTargetType = 'endpoint'
    $reportTargetLabel = "$($connection.host):$($connection.port)"
    $connectionMetadata.dbHost = $connection.host
    $connectionMetadata.dbPort = $connection.port
    $connectionMetadata.mySqlSslMode = if ([string]::IsNullOrWhiteSpace($MySqlSslMode)) { $null } else { $MySqlSslMode }
}

$dumpFile = Get-Item -Path $dumpPath
if ($dumpFile.Length -lt 100) {
    throw "덤프 파일 크기가 비정상적으로 작습니다: $($dumpFile.Length) bytes"
}

$hash = Get-FileHash -Path $dumpPath -Algorithm SHA256
$shouldWriteWeekly = $ForceWeekly -or (Get-Date).DayOfWeek -eq [System.DayOfWeek]::Sunday

$manifest = [ordered]@{
    createdAt = (Get-Date).ToString('o')
    mode = $Mode
    containerName = $connectionMetadata.containerName
    dbHost = $connectionMetadata.dbHost
    dbPort = $connectionMetadata.dbPort
    mySqlSslMode = $connectionMetadata.mySqlSslMode
    databaseName = $DatabaseName
    mysqlUser = $MySqlUser
    dumpPath = $dumpPath
    dumpSizeBytes = $dumpFile.Length
    dumpSha256 = $hash.Hash
    dumpStderrLog = $dumpStderrPath
    skipUpload = [bool]$SkipUpload
    bucket = if ([string]::IsNullOrWhiteSpace($Bucket)) { $null } else { $Bucket }
    awsRegion = if ([string]::IsNullOrWhiteSpace($AwsRegion)) { $null } else { $AwsRegion }
    wroteWeeklyCopy = [bool]$shouldWriteWeekly
    uploadedObjects = @()
}
$manifest | ConvertTo-Json -Depth 6 | Set-Content -Path $manifestPath -Encoding UTF8

$uploadedObjects = @()
if (-not $SkipUpload) {
    $dailyDumpKey = "$DailyPrefix/$($dumpFile.Name)"
    $dailyManifestKey = "$DailyPrefix/$fileBase.manifest.json"
    $dailyDumpUri = "s3://$Bucket/$dailyDumpKey"
    $dailyManifestUri = "s3://$Bucket/$dailyManifestKey"

    $dailyDumpLogs = Invoke-AwsS3Copy -SourcePath $dumpPath -S3Uri $dailyDumpUri -LogPrefix "$fileBase-daily-dump"
    $dailyManifestLogs = Invoke-AwsS3Copy -SourcePath $manifestPath -S3Uri $dailyManifestUri -LogPrefix "$fileBase-daily-manifest"

    $uploadedObjects += @(
        @{
            scope = 'daily'
            type = 'dump'
            s3Uri = $dailyDumpUri
            stdoutLog = $dailyDumpLogs.stdout
            stderrLog = $dailyDumpLogs.stderr
        },
        @{
            scope = 'daily'
            type = 'manifest'
            s3Uri = $dailyManifestUri
            stdoutLog = $dailyManifestLogs.stdout
            stderrLog = $dailyManifestLogs.stderr
        }
    )

    if ($shouldWriteWeekly) {
        $weeklyDumpKey = "$WeeklyPrefix/$($dumpFile.Name)"
        $weeklyManifestKey = "$WeeklyPrefix/$fileBase.manifest.json"
        $weeklyDumpUri = "s3://$Bucket/$weeklyDumpKey"
        $weeklyManifestUri = "s3://$Bucket/$weeklyManifestKey"

        $weeklyDumpLogs = Invoke-AwsS3Copy -SourcePath $dumpPath -S3Uri $weeklyDumpUri -LogPrefix "$fileBase-weekly-dump"
        $weeklyManifestLogs = Invoke-AwsS3Copy -SourcePath $manifestPath -S3Uri $weeklyManifestUri -LogPrefix "$fileBase-weekly-manifest"

        $uploadedObjects += @(
            @{
                scope = 'weekly'
                type = 'dump'
                s3Uri = $weeklyDumpUri
                stdoutLog = $weeklyDumpLogs.stdout
                stderrLog = $weeklyDumpLogs.stderr
            },
            @{
                scope = 'weekly'
                type = 'manifest'
                s3Uri = $weeklyManifestUri
                stdoutLog = $weeklyManifestLogs.stdout
                stderrLog = $weeklyManifestLogs.stderr
            }
        )
    }

    $manifest.uploadedObjects = $uploadedObjects
    $manifest | ConvertTo-Json -Depth 6 | Set-Content -Path $manifestPath -Encoding UTF8
}

$reportLines = [System.Collections.Generic.List[string]]::new()
$reportLines.Add('# BootSync Backup Automation Report')
$reportLines.Add('')
$reportLines.Add("- executed_at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss K')")
$reportLines.Add("- mode: $Mode")
$reportLines.Add("- ${reportTargetType}: $reportTargetLabel")
$reportLines.Add("- database: $DatabaseName")
$reportLines.Add("- dump_path: $dumpPath")
$reportLines.Add("- dump_size_bytes: $($dumpFile.Length)")
$reportLines.Add("- dump_sha256: $($hash.Hash)")
$reportLines.Add("- manifest_path: $manifestPath")
$reportLines.Add("- dump_stderr_log: $dumpStderrPath")
$reportLines.Add("- upload_status: $(if ($SkipUpload) { 'SKIPPED' } else { 'COMPLETED' })")
$reportLines.Add("- weekly_copy: $(if ($shouldWriteWeekly) { 'YES' } else { 'NO' })")
if ($Mode -eq 'tcp' -and -not [string]::IsNullOrWhiteSpace($MySqlSslMode)) {
    $reportLines.Add("- mysql_ssl_mode: $MySqlSslMode")
}

if ($uploadedObjects.Count -gt 0) {
    $reportLines.Add('')
    $reportLines.Add('## Uploaded Objects')
    $reportLines.Add('')
    foreach ($uploadedObject in $uploadedObjects) {
        $reportLines.Add("- [$($uploadedObject.scope)] $($uploadedObject.type): $($uploadedObject.s3Uri)")
    }
}

Set-Content -Path $reportPath -Value $reportLines -Encoding UTF8
Write-Output "BootSync backup automation report written to $reportPath"

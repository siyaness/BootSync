[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string]$TargetEmail,

    [string]$ProjectRoot = (Join-Path $PSScriptRoot '..\..'),
    [string]$BaseUrl = 'http://localhost:18080',
    [string]$PublicBaseUrl = $env:APP_PUBLIC_BASE_URL,
    [string]$FromAddress = $env:APP_RECOVERY_EMAIL_FROM,
    [string]$MailHost = $env:MAIL_HOST,
    [string]$MailPort = $(if ($env:MAIL_PORT) { $env:MAIL_PORT } else { '587' }),
    [string]$MailUsername = $env:MAIL_USERNAME,
    [string]$Username,
    [string]$Password = 'smtp-test-123!',
    [string]$DisplayName = 'SMTP Test',
    [string]$ReportPath,
    [switch]$SkipHealthCheck,
    [switch]$SkipCertificateCheck,
    [switch]$WriteReportOnly
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$reportRoot = Join-Path $resolvedProjectRoot 'build\ops-smtp\reports'
$logRoot = Join-Path $resolvedProjectRoot 'build\ops-smtp\logs'
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

if ([string]::IsNullOrWhiteSpace($PublicBaseUrl)) {
    $PublicBaseUrl = $BaseUrl
}

if ([string]::IsNullOrWhiteSpace($Username)) {
    $Username = ('smtp' + (Get-Date -Format 'MMddHHmmss')).ToLowerInvariant()
}

if ($Username.Length -gt 20) {
    throw 'username은 20자를 넘길 수 없습니다.'
}

if ($Password.Length -lt 10) {
    throw 'password는 10자 이상이어야 합니다.'
}

if ($DisplayName.Length -lt 2) {
    throw 'displayName은 2자 이상이어야 합니다.'
}

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $reportPath = Join-Path $reportRoot "smtp-smoke-test-$timestamp.md"
} else {
    $reportPath = $ReportPath
    $parent = Split-Path -Parent $reportPath
    if (-not [string]::IsNullOrWhiteSpace($parent)) {
        New-Item -ItemType Directory -Force -Path $parent | Out-Null
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

function Mask-Email {
    param(
        [AllowNull()]
        [string]$Email
    )

    if ([string]::IsNullOrWhiteSpace($Email) -or -not $Email.Contains('@')) {
        return $Email
    }

    $parts = $Email.Split('@', 2)
    $local = $parts[0]
    $domain = $parts[1]

    if ($local.Length -le 2) {
        $maskedLocal = $local.Substring(0, 1) + '*'
    } else {
        $maskedLocal = $local.Substring(0, 2) + ('*' * ($local.Length - 2))
    }

    if ($domain.Length -le 2) {
        $maskedDomain = $domain.Substring(0, 1) + '*'
    } else {
        $maskedDomain = $domain.Substring(0, 2) + ('*' * ($domain.Length - 2))
    }

    return "$maskedLocal@$maskedDomain"
}

function Invoke-JsonRequest {
    param(
        [Parameter(Mandatory = $true)]
        [ValidateSet('GET', 'POST')]
        [string]$Method,
        [Parameter(Mandatory = $true)]
        [string]$Uri,
        [Parameter(Mandatory = $true)]
        [Microsoft.PowerShell.Commands.WebRequestSession]$WebSession,
        [hashtable]$Headers,
        [AllowNull()]
        [string]$Body
    )

    $params = @{
        Method              = $Method
        Uri                 = $Uri
        WebSession          = $WebSession
        Headers             = $Headers
        ContentType         = 'application/json'
        SkipCertificateCheck = $SkipCertificateCheck.IsPresent
    }

    if ($null -ne $Body) {
        $params.Body = $Body
    }

    return Invoke-RestMethod @params
}

function Wait-ForHttpHealth {
    param(
        [string]$Uri
    )

    for ($attempt = 1; $attempt -le 60; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $Uri -Method GET -SkipCertificateCheck:$SkipCertificateCheck.IsPresent
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
            Start-Sleep -Seconds 2
            continue
        }

        Start-Sleep -Seconds 2
    }

    throw "헬스체크 응답을 받지 못했습니다: $Uri"
}

function Write-SmokeReport {
    param(
        [string]$StatusSummary,
        [AllowNull()]
        [string]$SessionState,
        [AllowNull()]
        [string]$PendingEmailMasked,
        [AllowNull()]
        [string]$PendingPurposeLabel,
        [AllowNull()]
        [string]$FailureMessage
    )

    $report = @"
# SMTP 실메일 스모크 테스트

- 실행 시각: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')
- 실행 환경: app base url = $BaseUrl
- 공개 base url: $PublicBaseUrl
- 상태 요약: $StatusSummary

## 앱 설정 기록

- APP_RECOVERY_EMAIL_FROM: $FromAddress
- MAIL_HOST: $MailHost
- MAIL_PORT: $MailPort
- MAIL_USERNAME configured: $([bool](-not [string]::IsNullOrWhiteSpace($MailUsername)))
- APP_RECOVERY_EMAIL_MAIL_ENABLED: true (실행 전 앱에 반영돼 있어야 함)
- APP_RECOVERY_EMAIL_DEVELOPMENT_PREVIEW_ENABLED: false (실행 전 앱에 반영돼 있어야 함)

## 발송 트리거 정보

- 회원가입 username: $Username
- display name: $DisplayName
- recovery email: $(Mask-Email $TargetEmail)
- session 상태: $SessionState
- pending purpose: $PendingPurposeLabel
- pending email: $PendingEmailMasked

## 수동 확인 체크리스트

1. 메일함에서 실제 메일이 도착했는지 확인
2. 제목이 `[BootSync] 회원가입 복구 이메일 인증`인지 확인
3. 링크 도메인이 $PublicBaseUrl 기준인지 확인
4. 링크 진입 후 confirm 완료
5. verified 상태 반영 확인
6. 필요하면 resend cooldown도 별도로 확인

## 실패 메모

$FailureMessage
"@

    Write-Utf8NoBomFile -Path $reportPath -Content $report.Trim() + [Environment]::NewLine
}

if ($WriteReportOnly) {
    Write-SmokeReport `
        -StatusSummary '준비용 초안 생성' `
        -SessionState '미실행' `
        -PendingEmailMasked (Mask-Email $TargetEmail) `
        -PendingPurposeLabel '회원가입 복구 이메일 인증' `
        -FailureMessage '- 실제 앱 호출 전 준비용 초안입니다.'

    Write-Output "SMTP 스모크 테스트 초안을 생성했습니다: $reportPath"
    exit 0
}

if ([string]::IsNullOrWhiteSpace($FromAddress)) {
    throw 'APP_RECOVERY_EMAIL_FROM 또는 -FromAddress 값이 필요합니다.'
}

if ([string]::IsNullOrWhiteSpace($MailHost)) {
    throw 'MAIL_HOST 또는 -MailHost 값이 필요합니다.'
}

if (-not $SkipHealthCheck) {
    Wait-ForHttpHealth -Uri ($BaseUrl.TrimEnd('/') + '/actuator/health')
}

$webSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$anonymousSession = Invoke-JsonRequest `
    -Method GET `
    -Uri ($BaseUrl.TrimEnd('/') + '/api/auth/session') `
    -WebSession $webSession `
    -Headers @{} `
    -Body $null

$csrfHeaderName = $anonymousSession.csrf.headerName
$csrfToken = $anonymousSession.csrf.token
$headers = @{
    $csrfHeaderName = $csrfToken
}

$payload = @{
    username      = $Username
    password      = $Password
    displayName   = $DisplayName
    recoveryEmail = $TargetEmail
} | ConvertTo-Json

try {
    Invoke-JsonRequest `
        -Method POST `
        -Uri ($BaseUrl.TrimEnd('/') + '/api/auth/signup') `
        -WebSession $webSession `
        -Headers $headers `
        -Body $payload | Out-Null
} catch {
    Write-SmokeReport `
        -StatusSummary '발송 트리거 실패' `
        -SessionState 'signup 실패' `
        -PendingEmailMasked (Mask-Email $TargetEmail) `
        -PendingPurposeLabel '회원가입 복구 이메일 인증' `
        -FailureMessage ('- signup 호출 실패: ' + $_.Exception.Message)
    throw
}

$authenticatedSession = Invoke-JsonRequest `
    -Method GET `
    -Uri ($BaseUrl.TrimEnd('/') + '/api/auth/session') `
    -WebSession $webSession `
    -Headers @{} `
    -Body $null

if (-not $authenticatedSession.authenticated) {
    Write-SmokeReport `
        -StatusSummary '발송 트리거 후 세션 미인증' `
        -SessionState 'signup 후 인증 실패' `
        -PendingEmailMasked (Mask-Email $TargetEmail) `
        -PendingPurposeLabel '회원가입 복구 이메일 인증' `
        -FailureMessage '- signup 후 세션이 인증 상태로 유지되지 않았습니다.'
    throw 'signup 이후 인증 세션을 확인하지 못했습니다.'
}

$recoveryStatus = $authenticatedSession.recoveryEmailStatus
$pendingPurposeLabel = if ($null -ne $recoveryStatus) { $recoveryStatus.pendingPurposeLabel } else { $null }
$pendingEmailMasked = if ($null -ne $recoveryStatus) { $recoveryStatus.maskedPendingRecoveryEmail } else { $null }
$hasPendingVerification = if ($null -ne $recoveryStatus) { [bool]$recoveryStatus.hasPendingVerification } else { $false }

$statusSummary = if ($hasPendingVerification) {
    '발송 트리거 완료, pending verification 확인'
} else {
    '발송 트리거는 완료됐지만 pending verification 응답은 확인되지 않음'
}

$failureMessage = if ($hasPendingVerification) {
    '- 앱 기준 pending verification 상태는 정상입니다. 이제 메일함 확인과 링크 confirm만 수동으로 완료하면 됩니다.'
} else {
    '- signup은 성공했지만 session 응답에서 pending verification 상태가 기대와 다릅니다. 앱 로그와 DB 상태를 함께 확인해 주세요.'
}

Write-SmokeReport `
    -StatusSummary $statusSummary `
    -SessionState 'signup 성공 / authenticated=true' `
    -PendingEmailMasked $pendingEmailMasked `
    -PendingPurposeLabel $pendingPurposeLabel `
    -FailureMessage $failureMessage

Write-Output "SMTP 스모크 테스트 트리거를 완료했습니다."
Write-Output "username: $Username"
Write-Output "report: $reportPath"
Write-Output "pendingVerification: $hasPendingVerification"

[CmdletBinding()]
param(
    [string]$ProjectRoot,
    [string]$BaseUrl = 'http://localhost:18080',
    [string]$UsernamePrefix = 'smoke',
    [string]$Password = 'before-password',
    [string]$UpdatedPassword = 'after-password',
    [string]$DisplayName = 'Smoke User',
    [string]$UpdatedDisplayName = 'Updated Smoke',
    [string]$ReportPath,
    [switch]$SkipHealthCheck
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Join-Path $PSScriptRoot '..\..'
}

$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
$timestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$reportRoot = Join-Path $resolvedProjectRoot 'build\local-smoke\reports'
New-Item -ItemType Directory -Force -Path $reportRoot | Out-Null

if ([string]::IsNullOrWhiteSpace($ReportPath)) {
    $ReportPath = Join-Path $reportRoot "local-session-smoke-$timestamp.md"
} else {
    $reportParent = Split-Path -Parent $ReportPath
    if (-not [string]::IsNullOrWhiteSpace($reportParent)) {
        New-Item -ItemType Directory -Force -Path $reportParent | Out-Null
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

function Wait-ForHealth {
    param(
        [string]$HealthUrl
    )

    for ($attempt = 1; $attempt -le 60; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $HealthUrl -Method GET -SkipHttpErrorCheck
            if ($response.StatusCode -eq 200) {
                return
            }
        } catch {
        }
        Start-Sleep -Seconds 2
    }

    throw "Health check did not become ready: $HealthUrl"
}

function Get-SessionState {
    param(
        [Microsoft.PowerShell.Commands.WebRequestSession]$WebSession
    )

    Invoke-RestMethod -Method GET -Uri ($BaseUrl.TrimEnd('/') + '/api/auth/session') -WebSession $WebSession
}

function Get-CsrfHeaders {
    param($SessionState)

    return @{
        $SessionState.csrf.headerName = $SessionState.csrf.token
    }
}

function Invoke-JsonRequest {
    param(
        [ValidateSet('GET', 'POST', 'PATCH')]
        [string]$Method,
        [string]$Path,
        [Microsoft.PowerShell.Commands.WebRequestSession]$WebSession,
        [hashtable]$Headers = @{},
        [AllowNull()]
        [object]$Body = $null,
        [switch]$UseRestMethod
    )

    $uri = $BaseUrl.TrimEnd('/') + $Path
    $payload = if ($null -ne $Body) { $Body | ConvertTo-Json -Depth 10 } else { $null }

    if ($UseRestMethod) {
        $params = @{
            Method     = $Method
            Uri        = $uri
            WebSession = $WebSession
            Headers    = $Headers
        }
        if ($null -ne $payload) {
            $params.ContentType = 'application/json'
            $params.Body = $payload
        }
        return Invoke-RestMethod @params
    }

    $params = @{
        Method             = $Method
        Uri                = $uri
        WebSession         = $WebSession
        Headers            = $Headers
        SkipHttpErrorCheck = $true
    }
    if ($null -ne $payload) {
        $params.ContentType = 'application/json'
        $params.Body = $payload
    }
    return Invoke-WebRequest @params
}

function Assert-Condition {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

function Get-RedirectResponse {
    param(
        [string]$Path
    )

    try {
        Invoke-WebRequest `
            -Method GET `
            -Uri ($BaseUrl.TrimEnd('/') + $Path) `
            -MaximumRedirection 0 `
            -ErrorAction Stop
    } catch {
        if ($null -ne $_.Exception.Response) {
            return $_.Exception.Response
        }
        throw
    }
}

$baseUrlTrimmed = $BaseUrl.TrimEnd('/')

if (-not $SkipHealthCheck) {
    Wait-ForHealth -HealthUrl ($baseUrlTrimmed + '/actuator/health/liveness')
}

$anonymousRedirect = Get-RedirectResponse -Path '/app/dashboard'
Assert-Condition `
    -Condition ($anonymousRedirect.StatusCode -ge 300 -and $anonymousRedirect.StatusCode -lt 400) `
    -Message 'Anonymous protected route did not redirect.'
Assert-Condition `
    -Condition (
        $anonymousRedirect.Headers.Location -eq '/app/login?next=%2Fdashboard' `
            -or $anonymousRedirect.Headers.Location -eq ($baseUrlTrimmed + '/app/login?next=%2Fdashboard')
    ) `
    -Message "Unexpected protected route redirect: $($anonymousRedirect.Headers.Location)"

$demoSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$demoSessionState = Get-SessionState -WebSession $demoSession
$demoLoginResponse = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/auth/login' `
    -WebSession $demoSession `
    -Headers (Get-CsrfHeaders -SessionState $demoSessionState) `
    -Body @{ username = 'd'; password = 'd' }
Assert-Condition -Condition ($demoLoginResponse.StatusCode -eq 204) -Message 'Demo login failed.'
$demoAfterLogin = Get-SessionState -WebSession $demoSession
Assert-Condition -Condition ($demoAfterLogin.authenticated -eq $true) -Message 'Demo session is not authenticated after login.'
Assert-Condition -Condition ($demoAfterLogin.user.username -eq 'd') -Message 'Demo session user mismatch.'

$username = ($UsernamePrefix + (Get-Date -Format 'MMddHHmmss')).ToLowerInvariant()
if ($username.Length -gt 20) {
    $username = $username.Substring(0, 20)
}
$signupEmail = "$username@example.com"

$signupSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$signupSeed = Get-SessionState -WebSession $signupSession
$signupResponse = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/auth/signup' `
    -WebSession $signupSession `
    -Headers (Get-CsrfHeaders -SessionState $signupSeed) `
    -Body @{
        username      = $username
        password      = $Password
        displayName   = $DisplayName
        recoveryEmail = $signupEmail
    }
Assert-Condition -Condition ($signupResponse.StatusCode -eq 201) -Message 'Signup failed.'

$afterSignup = Get-SessionState -WebSession $signupSession
Assert-Condition -Condition ($afterSignup.authenticated -eq $true) -Message 'Signup session is not authenticated.'
Assert-Condition -Condition ($afterSignup.user.username -eq $username) -Message 'Signup session user mismatch.'
Assert-Condition `
    -Condition (-not [string]::IsNullOrWhiteSpace($afterSignup.recoveryEmailStatus.developmentPreviewPath)) `
    -Message 'Signup verification preview link is missing.'

$previewPath = [string]$afterSignup.recoveryEmailStatus.developmentPreviewPath
$tokenMatch = [regex]::Match($previewPath, 'token=([^&]+)')
Assert-Condition -Condition $tokenMatch.Success -Message 'Could not extract signup verification token.'
$verificationToken = $tokenMatch.Groups[1].Value

Invoke-JsonRequest `
    -Method POST `
    -Path '/api/recovery-email/confirm' `
    -WebSession $signupSession `
    -Headers (Get-CsrfHeaders -SessionState $afterSignup) `
    -Body @{
        purpose = 'signup'
        token   = $verificationToken
    } `
    -UseRestMethod | Out-Null

$afterConfirm = Get-SessionState -WebSession $signupSession
Assert-Condition -Condition ($afterConfirm.user.emailVerified -eq $true) -Message 'Recovery email was not verified.'

$profileUpdate = Invoke-JsonRequest `
    -Method PATCH `
    -Path '/api/settings/profile' `
    -WebSession $signupSession `
    -Headers (Get-CsrfHeaders -SessionState $afterConfirm) `
    -Body @{ displayName = $UpdatedDisplayName }
Assert-Condition -Condition ($profileUpdate.StatusCode -eq 204) -Message 'Profile update failed.'

$afterProfile = Get-SessionState -WebSession $signupSession
Assert-Condition -Condition ($afterProfile.user.displayName -eq $UpdatedDisplayName) -Message 'Display name was not refreshed in current session.'

$passwordChange = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/settings/password' `
    -WebSession $signupSession `
    -Headers (Get-CsrfHeaders -SessionState $afterProfile) `
    -Body @{
        currentPassword    = $Password
        newPassword        = $UpdatedPassword
        newPasswordConfirm = $UpdatedPassword
    }
Assert-Condition -Condition ($passwordChange.StatusCode -eq 204) -Message 'Password change failed.'

$afterPasswordChange = Get-SessionState -WebSession $signupSession
Assert-Condition -Condition ($afterPasswordChange.authenticated -eq $true) -Message 'Current session was lost after password change.'

$oldPasswordSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$oldPasswordSeed = Get-SessionState -WebSession $oldPasswordSession
$oldPasswordLogin = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/auth/login' `
    -WebSession $oldPasswordSession `
    -Headers (Get-CsrfHeaders -SessionState $oldPasswordSeed) `
    -Body @{ username = $username; password = $Password }
Assert-Condition -Condition ($oldPasswordLogin.StatusCode -eq 401) -Message 'Old password unexpectedly still works.'

$newPasswordSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$newPasswordSeed = Get-SessionState -WebSession $newPasswordSession
$newPasswordLogin = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/auth/login' `
    -WebSession $newPasswordSession `
    -Headers (Get-CsrfHeaders -SessionState $newPasswordSeed) `
    -Body @{ username = $username; password = $UpdatedPassword }
Assert-Condition -Condition ($newPasswordLogin.StatusCode -eq 204) -Message 'Updated password login failed.'

$afterNewPasswordLogin = Get-SessionState -WebSession $newPasswordSession
Assert-Condition -Condition ($afterNewPasswordLogin.authenticated -eq $true) -Message 'New password session is not authenticated.'

$accountDeletion = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/settings/account-deletion' `
    -WebSession $signupSession `
    -Headers (Get-CsrfHeaders -SessionState $afterPasswordChange) `
    -Body @{ currentPassword = $UpdatedPassword }
Assert-Condition -Condition ($accountDeletion.StatusCode -eq 204) -Message 'Account deletion request failed.'

$afterDeletion = Get-SessionState -WebSession $signupSession
Assert-Condition -Condition ($afterDeletion.authenticated -eq $false) -Message 'Current session still authenticated after account deletion request.'

$deletedUserLoginSession = [Microsoft.PowerShell.Commands.WebRequestSession]::new()
$deletedUserLoginSeed = Get-SessionState -WebSession $deletedUserLoginSession
$deletedUserLogin = Invoke-JsonRequest `
    -Method POST `
    -Path '/api/auth/login' `
    -WebSession $deletedUserLoginSession `
    -Headers (Get-CsrfHeaders -SessionState $deletedUserLoginSeed) `
    -Body @{ username = $username; password = $UpdatedPassword }
Assert-Condition -Condition ($deletedUserLogin.StatusCode -eq 401) -Message 'Deleted account unexpectedly still logs in.'

$report = @"
# Local Session Smoke Test

- Executed at: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss zzz')
- Base URL: $BaseUrl
- Username: $username
- Recovery email: $signupEmail

## Checks

- Anonymous /app/dashboard redirect preserved next
- Demo login (`d / d`) created authenticated session
- Signup created authenticated session
- Signup recovery email verification confirmed successfully
- Profile update refreshed current session display name
- Password change kept current session alive
- Old password login failed
- New password login succeeded
- Account deletion request logged out current session
- Deleted account login blocked
"@

Write-Utf8NoBomFile -Path $ReportPath -Content ($report.Trim() + [Environment]::NewLine)

[PSCustomObject]@{
    status = 'ok'
    reportPath = $ReportPath
    baseUrl = $BaseUrl
    username = $username
} | ConvertTo-Json -Depth 5

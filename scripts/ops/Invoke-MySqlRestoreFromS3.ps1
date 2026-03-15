[CmdletBinding(SupportsShouldProcess = $true)]
param(
    [string]$ProjectRoot,
    [string]$RestoreDir,
    [string]$SourceFile,
    [string]$S3Key,
    [string]$Bucket = $env:BACKUP_S3_BUCKET,
    [string]$AwsRegion = $env:AWS_REGION,
    [string]$AwsProfile = $env:AWS_PROFILE,
    [string]$ContainerName = 'bootsync-mysql',
    [string]$DatabaseName = 'bootsync',
    [string]$MySqlUser = 'bootsync',
    [string]$MySqlPasswordEnvVarName = 'MYSQL_PASSWORD'
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

if ([string]::IsNullOrWhiteSpace($SourceFile) -and [string]::IsNullOrWhiteSpace($S3Key)) {
    throw '복원은 -SourceFile 또는 -S3Key 중 하나가 필요합니다.'
}

if (-not [string]::IsNullOrWhiteSpace($SourceFile) -and -not [string]::IsNullOrWhiteSpace($S3Key)) {
    throw '복원은 -SourceFile 또는 -S3Key 중 하나만 지정해야 합니다.'
}

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
        [string]$StandardErrorPath
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

Require-Command -Name 'docker'

if ([string]::IsNullOrWhiteSpace($ProjectRoot)) {
    $ProjectRoot = Join-Path (Split-Path -Parent $PSCommandPath) '..\..'
}
$resolvedProjectRoot = (Resolve-Path $ProjectRoot).Path
if ([string]::IsNullOrWhiteSpace($RestoreDir)) {
    $RestoreDir = Join-Path $resolvedProjectRoot 'build\ops-restore'
}
$resolvedRestoreDir = (Resolve-Path (New-Item -ItemType Directory -Force -Path $RestoreDir)).Path
$logRoot = Join-Path $resolvedRestoreDir 'logs'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

$restoreTimestamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$restoreFilePath = $SourceFile

if (-not [string]::IsNullOrWhiteSpace($S3Key)) {
    Require-Command -Name 'aws'
    if ([string]::IsNullOrWhiteSpace($Bucket)) {
        throw 'S3Key 복원에는 BACKUP_S3_BUCKET 또는 -Bucket 값이 필요합니다.'
    }
    if ([string]::IsNullOrWhiteSpace($AwsRegion)) {
        throw 'S3Key 복원에는 AWS_REGION 또는 -AwsRegion 값이 필요합니다.'
    }

    $downloadName = Split-Path -Path $S3Key -Leaf
    $restoreFilePath = Join-Path $resolvedRestoreDir $downloadName
    $downloadStdoutPath = Join-Path $logRoot "restore-download-$restoreTimestamp.stdout.log"
    $downloadStderrPath = Join-Path $logRoot "restore-download-$restoreTimestamp.stderr.log"
    $downloadArgs = @('s3', 'cp', "s3://$Bucket/$S3Key", $restoreFilePath, '--region', $AwsRegion)
    if (-not [string]::IsNullOrWhiteSpace($AwsProfile)) {
        $downloadArgs += @('--profile', $AwsProfile)
    }

    if ($PSCmdlet.ShouldProcess("s3://$Bucket/$S3Key", 'Download restore dump')) {
        Invoke-ExternalCommand -FilePath 'aws' -ArgumentList $downloadArgs -StandardOutputPath $downloadStdoutPath -StandardErrorPath $downloadStderrPath
    }
}

if ([string]::IsNullOrWhiteSpace($restoreFilePath)) {
    throw '복원 파일 경로를 확인할 수 없습니다.'
}

$restoreContainerPath = "/tmp/$(Split-Path -Path $restoreFilePath -Leaf)"
$copyStdoutPath = Join-Path $logRoot "restore-copy-$restoreTimestamp.stdout.log"
$copyStderrPath = Join-Path $logRoot "restore-copy-$restoreTimestamp.stderr.log"
$restoreStdoutPath = Join-Path $logRoot "restore-exec-$restoreTimestamp.stdout.log"
$restoreStderrPath = Join-Path $logRoot "restore-exec-$restoreTimestamp.stderr.log"
$passwordReference = '$' + $MySqlPasswordEnvVarName
$restoreCommand = "exec mysql -u$MySqlUser -p`"$passwordReference`" $DatabaseName < $restoreContainerPath"

if ($PSCmdlet.ShouldProcess($ContainerName, "Copy $restoreFilePath to $restoreContainerPath")) {
    Invoke-ExternalCommand `
        -FilePath 'docker' `
        -ArgumentList @('cp', $restoreFilePath, "$ContainerName`:$restoreContainerPath") `
        -StandardOutputPath $copyStdoutPath `
        -StandardErrorPath $copyStderrPath
}

if ($PSCmdlet.ShouldProcess($ContainerName, "Restore $DatabaseName from $restoreContainerPath")) {
    Invoke-ExternalCommand `
        -FilePath 'docker' `
        -ArgumentList @('exec', $ContainerName, 'sh', '-lc', $restoreCommand) `
        -StandardOutputPath $restoreStdoutPath `
        -StandardErrorPath $restoreStderrPath
}

Write-Output "BootSync restore command prepared for $restoreFilePath"

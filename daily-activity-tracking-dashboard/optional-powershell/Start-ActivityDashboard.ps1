[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$NoBuild
)

$ErrorActionPreference = 'Stop'

$trackerDirectory = Split-Path -Parent $PSScriptRoot
$dashboardDirectory = Join-Path $trackerDirectory 'activity-dashboard'
$composeFile = Join-Path $dashboardDirectory 'compose.yaml'
$envFile = Join-Path $dashboardDirectory '.env'

$dockerCommand = Get-Command docker.exe -ErrorAction SilentlyContinue
if (-not $dockerCommand) {
    throw 'Docker Desktop CLI is required. Install Docker Desktop and reopen PowerShell.'
}
if (-not (Test-Path -LiteralPath $composeFile -PathType Leaf)) {
    throw "Docker Compose configuration was not found: $composeFile"
}
if (-not (Test-Path -LiteralPath (Join-Path $dashboardDirectory 'backend-service\Dockerfile') -PathType Leaf)) {
    throw 'Backend Dockerfile is missing.'
}

$dockerReady = $false
$dockerInfo = $null
$dockerDeadline = (Get-Date).AddMinutes(5)
while ((Get-Date) -lt $dockerDeadline) {
    $dockerInfo = & $dockerCommand.Source info --format '{{.ServerVersion}}' 2>&1
    if ($LASTEXITCODE -eq 0) {
        $dockerReady = $true
        break
    }
    Start-Sleep -Seconds 5
}
if (-not $dockerReady) {
    throw "Docker engine is not ready after five minutes. Start Docker Desktop and retry. $dockerInfo"
}

$documentsFolder = [Environment]::GetFolderPath('MyDocuments')
if ([string]::IsNullOrWhiteSpace($documentsFolder)) {
    throw "The current user's Documents folder could not be resolved."
}
$archiveDirectory = Join-Path $documentsFolder 'Activity-log'
New-Item -ItemType Directory -Path $archiveDirectory -Force | Out-Null

$archivePathForCompose = [System.IO.Path]::GetFullPath($archiveDirectory).Replace('\', '/')
$envContent = @(
    'TRACKER_SOURCE_HOST_PATH=..'
    "TRACKER_ARCHIVE_HOST_PATH=$archivePathForCompose"
    'DASHBOARD_PORT=8080'
    'TRACKER_ROLLOVER_CRON=0 5 0 * * *'
) -join "`n"
[System.IO.File]::WriteAllText($envFile, $envContent + "`n", [System.Text.UTF8Encoding]::new($false))

$composeArguments = @('compose', '--file', $composeFile, 'up', '-d')
if (-not $NoBuild) {
    $composeArguments += '--build'
}
& $dockerCommand.Source @composeArguments
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed with exit code $LASTEXITCODE. Check Docker Desktop and the build output."
}

$backendUrl = 'http://127.0.0.1:8080'
$backendReady = $false
$deadline = (Get-Date).AddMinutes(5)
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "$backendUrl/api/health" -TimeoutSec 3
        if ($health.status -eq 'ok') {
            $backendReady = $true
            break
        }
    }
    catch {
        Start-Sleep -Seconds 3
    }
}
if (-not $backendReady) {
    $logs = & $dockerCommand.Source compose --file $composeFile logs --tail 40 2>&1
    throw "The Dockerized backend did not become healthy within five minutes. Container output:`n$logs"
}

$page = Invoke-WebRequest -Uri "$backendUrl/" -TimeoutSec 10 -UseBasicParsing
if ($page.StatusCode -ne 200 -or $page.Content -notmatch 'Activity dashboard') {
    throw "The dashboard page did not return the expected content at $backendUrl/."
}

Write-Output "Docker backend is healthy at $backendUrl."
Write-Output "Tracker source: $trackerDirectory"
Write-Output "Archive folder: $archiveDirectory"
if (-not $NoBrowser) {
    Start-Process $backendUrl
}

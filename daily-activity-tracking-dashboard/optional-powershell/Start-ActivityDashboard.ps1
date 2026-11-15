[CmdletBinding()]
param(
    [switch]$NoBrowser,
    [switch]$NoBuild,
    [string]$TrackerDirectory
)

$ErrorActionPreference = 'Stop'

$projectDirectory = Split-Path -Parent $PSScriptRoot
if (Test-Path -LiteralPath (Join-Path $projectDirectory 'compose.yaml') -PathType Leaf) {
    $dashboardDirectory = $projectDirectory
}
elseif (Test-Path -LiteralPath (Join-Path $projectDirectory 'activity-dashboard\compose.yaml') -PathType Leaf) {
    $dashboardDirectory = Join-Path $projectDirectory 'activity-dashboard'
}
else {
    throw "Docker Compose configuration was not found in $projectDirectory or its activity-dashboard subfolder."
}

if ([string]::IsNullOrWhiteSpace($TrackerDirectory)) {
    $documentsFolder = [Environment]::GetFolderPath('MyDocuments')
    $defaultTrackerDirectory = Join-Path $documentsFolder 'Tracker-Docs'
    if (Test-Path -LiteralPath $defaultTrackerDirectory -PathType Container) {
        $TrackerDirectory = $defaultTrackerDirectory
    }
    elseif ($dashboardDirectory -ne $projectDirectory) {
        $TrackerDirectory = $projectDirectory
    }
    elseif (@(Get-ChildItem -LiteralPath (Split-Path -Parent $projectDirectory) -Filter 'Week-*-Activity.md' -File -ErrorAction SilentlyContinue).Count -gt 0) {
        $TrackerDirectory = Split-Path -Parent $projectDirectory
    }
    elseif (@(Get-ChildItem -LiteralPath $projectDirectory -Filter 'Week-*-Activity.md' -File -ErrorAction SilentlyContinue).Count -gt 0) {
        $TrackerDirectory = $projectDirectory
    }
    else {
        throw 'Could not locate the tracker folder. Supply it with -TrackerDirectory.'
    }
}
$trackerDirectory = [System.IO.Path]::GetFullPath($TrackerDirectory)
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
if (-not (Test-Path -LiteralPath (Join-Path $dashboardDirectory 'financial-analysis-service\Dockerfile') -PathType Leaf)) {
    throw 'Financial-analysis Dockerfile is missing.'
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
$archiveDirectory = Join-Path $trackerDirectory 'Activity-log'
$financialDocumentsDirectory = Join-Path $trackerDirectory 'Monthly-Expenditure'
New-Item -ItemType Directory -Path $archiveDirectory -Force | Out-Null
if (-not (Test-Path -LiteralPath $financialDocumentsDirectory -PathType Container)) {
    throw "Monthly financial documents folder was not found: $financialDocumentsDirectory"
}

$archivePathForCompose = [System.IO.Path]::GetFullPath($archiveDirectory).Replace('\', '/')
$trackerPathForCompose = $trackerDirectory.Replace('\', '/')
$financialPathForCompose = [System.IO.Path]::GetFullPath($financialDocumentsDirectory).Replace('\', '/')
$envContent = @(
    "TRACKER_SOURCE_HOST_PATH=$trackerPathForCompose"
    "TRACKER_ARCHIVE_HOST_PATH=$archivePathForCompose"
    "FINANCIAL_DOCUMENTS_HOST_PATH=$financialPathForCompose"
    'DASHBOARD_PORT=8080'
    'TRACKER_ROLLOVER_CRON=0 5 0 * * *'
) -join "`n"
[System.IO.File]::WriteAllText($envFile, $envContent + "`n", [System.Text.UTF8Encoding]::new($false))

$composeArguments = @('compose', '--project-name', 'activity-dashboard', '--file', $composeFile, 'up', '-d')
if (-not $NoBuild) {
    $composeArguments += '--build'
}
& $dockerCommand.Source @composeArguments
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose failed with exit code $LASTEXITCODE. Check Docker Desktop and the build output."
}

$backendUrl = 'http://127.0.0.1:8080'
$backendReady = $false
$financialServiceReady = $false
$deadline = (Get-Date).AddMinutes(5)
while ((Get-Date) -lt $deadline) {
    try {
        $health = Invoke-RestMethod -Uri "$backendUrl/api/health" -TimeoutSec 3
        if ($health.status -eq 'ok') {
            $backendReady = $true
            $financeHealth = Invoke-RestMethod -Uri "$backendUrl/api/finance/health" -TimeoutSec 3
            if ($financeHealth.status -eq 'ok') {
                $financialServiceReady = $true
                break
            }
        }
    }
    catch {
        Start-Sleep -Seconds 3
    }
}
if (-not $backendReady -or -not $financialServiceReady) {
    $logs = & $dockerCommand.Source compose --project-name activity-dashboard --file $composeFile logs --tail 40 2>&1
    throw "The activity and financial-analysis services did not become healthy within five minutes. Container output:`n$logs"
}

$page = Invoke-WebRequest -Uri "$backendUrl/" -TimeoutSec 10 -UseBasicParsing
if ($page.StatusCode -ne 200 -or $page.Content -notmatch 'Activity dashboard') {
    throw "The dashboard page did not return the expected content at $backendUrl/."
}

Write-Output "Docker activity and financial-analysis services are healthy at $backendUrl."
Write-Output "Tracker source: $trackerDirectory"
Write-Output "Archive folder: $archiveDirectory"
if (-not $NoBrowser) {
    Start-Process $backendUrl
}

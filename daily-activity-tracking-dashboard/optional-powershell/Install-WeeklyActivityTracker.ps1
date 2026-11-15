[CmdletBinding()]
param(
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
$launcher = Join-Path $PSScriptRoot 'Start-ActivityDashboard.ps1'
$composeFile = Join-Path $dashboardDirectory 'compose.yaml'

if (-not (Test-Path -LiteralPath $composeFile -PathType Leaf)) {
    throw "Docker Compose configuration was not found: $composeFile"
}
$dockerCommand = Get-Command docker.exe -ErrorAction SilentlyContinue
if (-not $dockerCommand) {
    throw 'Docker Desktop CLI is required. Install Docker Desktop and reopen PowerShell.'
}
$composeVersion = & $dockerCommand.Source compose version 2>&1
if ($LASTEXITCODE -ne 0) {
    throw "Docker Compose is unavailable: $composeVersion"
}

$documentsFolder = [Environment]::GetFolderPath('MyDocuments')
if ([string]::IsNullOrWhiteSpace($documentsFolder)) {
    throw "The current user's Documents folder could not be resolved."
}
New-Item -ItemType Directory -Path (Join-Path $trackerDirectory 'Activity-log') -Force | Out-Null

$taskName = "Activity Tracker Dashboard ($env:USERNAME)"
$oldTaskName = "Activity Tracker - Archive and Reset ($env:USERNAME)"
$legacyTaskName = 'Weekly Activity Tracker - Archive and Reset'
$powerShell = Join-Path $PSHOME 'powershell.exe'
$taskAction = New-ScheduledTaskAction `
    -Execute $powerShell `
    -Argument "-NoProfile -NonInteractive -ExecutionPolicy Bypass -File `"$launcher`" -TrackerDirectory `"$trackerDirectory`" -NoBrowser -NoBuild" `
    -WorkingDirectory $dashboardDirectory
$taskTriggers = @(
    (New-ScheduledTaskTrigger -Daily -At 12:05AM),
    (New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME)
)
$taskPrincipal = New-ScheduledTaskPrincipal `
    -UserId "$env:USERDOMAIN\$env:USERNAME" `
    -LogonType Interactive `
    -RunLevel Limited
$taskSettings = New-ScheduledTaskSettingsSet `
    -StartWhenAvailable `
    -AllowStartIfOnBatteries `
    -DontStopIfGoingOnBatteries `
    -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit ([TimeSpan]::Zero)

foreach ($obsoleteTaskName in @($oldTaskName, $legacyTaskName)) {
    $obsoleteTask = Get-ScheduledTask -TaskName $obsoleteTaskName -ErrorAction SilentlyContinue
    if ($null -eq $obsoleteTask) {
        continue
    }
    $owner = ($obsoleteTask.Principal.UserId -split '\\')[-1]
    if ($owner -ieq $env:USERNAME) {
        Unregister-ScheduledTask -TaskName $obsoleteTaskName -Confirm:$false
        Write-Output "Removed this user's previous rollover task: $obsoleteTaskName"
    }
    else {
        Write-Warning "A task named '$obsoleteTaskName' belongs to '$($obsoleteTask.Principal.UserId)' and was left unchanged."
    }
}

Register-ScheduledTask `
    -TaskName $taskName `
    -Action $taskAction `
    -Trigger $taskTriggers `
    -Principal $taskPrincipal `
    -Settings $taskSettings `
    -Description 'Starts the Dockerized local activity dashboard and its Java weekly rollover service.' `
    -Force | Out-Null

Write-Output "Registered Docker service task: $taskName"
Write-Output 'Docker Desktop must be set to start when you sign in for unattended container startup.'
Write-Output 'Starting and health-checking the container now...'
& $powerShell -NoProfile -ExecutionPolicy Bypass -File $launcher -TrackerDirectory $trackerDirectory -NoBrowser
if ($LASTEXITCODE -ne 0) {
    throw "Could not start the Dockerized activity dashboard. Launcher exit code: $LASTEXITCODE"
}
Write-Output 'The backend is running in Docker and will restart unless explicitly stopped.'

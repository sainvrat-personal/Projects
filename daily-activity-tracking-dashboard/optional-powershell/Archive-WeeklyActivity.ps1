[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

$healthEndpoint = 'http://127.0.0.1:8080/api/health'
$endpoint = 'http://127.0.0.1:8080/api/rollover'
try {
    $serviceRunning = $false
    try {
        $health = Invoke-RestMethod -Uri $healthEndpoint -TimeoutSec 2
        $serviceRunning = $health.status -eq 'ok'
    }
    catch {
        $serviceRunning = $false
    }

    if (-not $serviceRunning) {
        $launcher = Join-Path $PSScriptRoot 'Start-ActivityDashboard.ps1'
        if (-not (Test-Path -LiteralPath $launcher -PathType Leaf)) {
            throw "Docker service launcher was not found: $launcher"
        }
        & powershell.exe -NoProfile -ExecutionPolicy Bypass -File $launcher -NoBrowser
        if ($LASTEXITCODE -ne 0) {
            throw "The Java service launcher failed with exit code $LASTEXITCODE."
        }
    }

    $result = Invoke-RestMethod -Uri $endpoint -Method Post -TimeoutSec 30
    Write-Output $result.message
}
catch {
    throw "The Dockerized tracker service could not run the weekly rollover. Verify Docker Desktop is running, then inspect the container output. Details: $($_.Exception.Message)"
}

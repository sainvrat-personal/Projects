[CmdletBinding()]
param(
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'

$packageFiles = @(
   'optional-powershell\Archive-WeeklyActivity.ps1',
   'optional-powershell\Install-WeeklyActivityTracker.ps1',
   'optional-powershell\activity-tracker-system.md',
   'optional-powershell\Build-ActivityTrackerPackage.ps1',
   'optional-powershell\Start-ActivityDashboard.ps1'
)
$readmeName = 'README-Setup.txt'
$readme = @'
OPTIONAL POWERSHELL HELPERS

This ZIP contains only the optional PowerShell helper scripts and their guide.
It does NOT contain the Java backend, dashboard UI, weekly template, or any
activity records.

REQUIRED COMPANION DOCKER SERVICE
Download and extract Activity-Dashboard-Java-Service.zip first. Then extract
this ZIP into the same tracker root, preserving the folder layout so that:
 <tracker-root>\activity-dashboard\backend-service\
 <tracker-root>\optional-powershell\
are siblings. The helpers start and configure the Dockerized Java backend;
they do not implement the dashboard or weekly rollover themselves.

The service requires Docker Desktop with Docker Compose v2. Java and Maven
run inside the container build and need not be installed on Windows.
Python is not required.

INSTALL AUTOMATIC STARTUP
From the tracker root, run:
 powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Install-WeeklyActivityTracker.ps1"
This creates Tracker-Docs\Activity-log and registers a
per-user task to start the Docker backend at sign-in with a daily recovery start.
The Java service inside the container performs weekly-sheet creation and rollover.

START THE DASHBOARD
 powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Start-ActivityDashboard.ps1"
The dashboard is served at http://127.0.0.1:8080.

MANUAL ROLLOVER
 powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Archive-WeeklyActivity.ps1"
This helper starts the Docker service if needed and calls its rollover API.

For helper-specific commands, task pause/removal, troubleshooting, and archive
configuration, read optional-powershell\activity-tracker-system.md.
For Java service configuration and API details, read
activity-dashboard\README.md from the companion Java package.
'@

$packageEntries = foreach ($fileName in $packageFiles) {
    $sourcePath = Join-Path (Split-Path -Parent $PSScriptRoot) $fileName
    if (-not (Test-Path -LiteralPath $sourcePath -PathType Leaf)) {
        throw "Required package file is missing: $sourcePath"
    }
    [pscustomobject]@{
        SourcePath = $sourcePath
        EntryName = $fileName.Replace('\', '/')
    }
}

if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    $OutputPath = Join-Path (Split-Path -Parent $PSScriptRoot) 'Activity-Tracker-Powershell-Service.zip'
}

$resolvedOutputPath = [System.IO.Path]::GetFullPath($OutputPath)
$outputFolder = Split-Path -Parent $resolvedOutputPath
if (-not (Test-Path -LiteralPath $outputFolder -PathType Container)) {
    New-Item -ItemType Directory -Path $outputFolder -Force | Out-Null
}

$temporaryZip = Join-Path $outputFolder ('Activity-Tracker-Service-{0}.tmp' -f [guid]::NewGuid())
$fileStream = $null
$zipArchive = $null

try {
    Add-Type -AssemblyName System.IO.Compression
    $fileStream = [System.IO.File]::Open(
        $temporaryZip,
        [System.IO.FileMode]::CreateNew,
        [System.IO.FileAccess]::ReadWrite,
        [System.IO.FileShare]::None
    )
    $zipArchive = [System.IO.Compression.ZipArchive]::new(
        $fileStream,
        [System.IO.Compression.ZipArchiveMode]::Create,
        $false
    )

    foreach ($packageEntry in $packageEntries) {
        $entry = $zipArchive.CreateEntry(
            $packageEntry.EntryName,
            [System.IO.Compression.CompressionLevel]::Optimal
        )
        $entryStream = $entry.Open()
        $sourceStream = [System.IO.File]::OpenRead($packageEntry.SourcePath)
        try {
            $sourceStream.CopyTo($entryStream)
        }
        finally {
            $sourceStream.Dispose()
            $entryStream.Dispose()
        }
    }

    $readmeEntry = $zipArchive.CreateEntry(
        $readmeName,
        [System.IO.Compression.CompressionLevel]::Optimal
    )
    $readmeStream = $readmeEntry.Open()
    $writer = [System.IO.StreamWriter]::new(
        $readmeStream,
        [System.Text.UTF8Encoding]::new($false)
    )
    try {
        $writer.Write($readme)
    }
    finally {
        $writer.Dispose()
    }

    $zipArchive.Dispose()
    $zipArchive = $null
    $fileStream.Dispose()
    $fileStream = $null

    Move-Item -LiteralPath $temporaryZip -Destination $resolvedOutputPath -Force
    Write-Output "Created shareable tracker package: $resolvedOutputPath"
    Write-Output "Included $($packageEntries.Count) PowerShell helper/documentation files, plus README-Setup.txt."
    Write-Output 'Excluded: Java backend, dashboard UI, weekly template, dated activity sheets, archives, and run logs.'
}
finally {
    if ($null -ne $zipArchive) {
        $zipArchive.Dispose()
    }
    if ($null -ne $fileStream) {
        $fileStream.Dispose()
    }
    if (Test-Path -LiteralPath $temporaryZip -PathType Leaf) {
        Remove-Item -LiteralPath $temporaryZip -Force
    }
}

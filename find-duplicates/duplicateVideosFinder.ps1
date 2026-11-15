$path = "C:\Toshiba Data"
$outputFile = "C:\Toshiba Data\DuplicateVideos.md"
$processedFolder = "C:\Toshiba Data\ProcessedVideos"

# Create folder if it doesn't exist
if (!(Test-Path $processedFolder)) {
    New-Item -ItemType Directory -Path $processedFolder | Out-Null
}

# Define video extensions to check
$videoExtensions = @(".mp4", ".mkv", ".avi", ".mov", ".wmv", ".flv")

# Get all video files
$files = Get-ChildItem -Path $path -Recurse -File | 
         Where-Object { $videoExtensions -contains $_.Extension.ToLower() }

$totalFiles = $files.Count
$counter = 0

# Group files by Name + Size
$groups = $files | Group-Object { "$($_.Name)-$($_.Length)" }

# Prepare Markdown output
$mdOutput = @()
$duplicateGroups = 0
$totalDuplicateFiles = 0

foreach ($group in $groups) {
    $counter++
    $progress = [math]::Round(($counter / $totalFiles) * 100, 2)
    Write-Progress -Activity "Processing videos..." -Status "$progress% complete" -PercentComplete $progress

    if ($group.Count -gt 1) {
        $duplicateGroups++
        $totalDuplicateFiles += $group.Count
        $mdOutput += "### Duplicate Group (Name+Size: $($group.Name))"
        
        # Sort files by LastWriteTime descending (newest first)
        $sortedFiles = $group.Group | Sort-Object LastWriteTime -Descending
        
        # Keep the newest file
        $fileToKeep = $sortedFiles[0]
        $mdOutput += "- KEPT: $($fileToKeep.FullName) | Size: $([math]::Round($fileToKeep.Length / 1MB, 2)) MB | Last Modified: $($fileToKeep.LastWriteTime)"
        
        # Move kept file to processed folder
        Move-Item -Path $fileToKeep.FullName -Destination $processedFolder -Force

        # Delete duplicates
        foreach ($file in $sortedFiles[1..($sortedFiles.Count - 1)]) {
            $mdOutput += "- DELETED: $($file.FullName) | Size: $([math]::Round($file.Length / 1MB, 2)) MB | Last Modified: $($file.LastWriteTime)"
            Remove-Item -Path $file.FullName -Force
        }
        $mdOutput += ""
    } else {
        # Non-duplicate files also moved to processed folder
        Move-Item -Path $group.Group[0].FullName -Destination $processedFolder -Force
    }
}

# Add summary at top
$summary = @()
$summary += "# Duplicate Video Files Report"
$summary += ""
$summary += "**Summary**"
$summary += "- Total Duplicate Groups: $duplicateGroups"
$summary += "- Total Duplicate Files Processed: $totalDuplicateFiles"
$summary += "- All videos moved to: $processedFolder"
$summary += ""

$finalOutput = $summary + $mdOutput
$finalOutput | Out-File $outputFile
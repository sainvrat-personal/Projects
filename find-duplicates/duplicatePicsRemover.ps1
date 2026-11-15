$path = "C:\Toshiba Data"
$outputFile = "C:\Toshiba Data\DuplicatePhotos.md"

# Define photo extensions to check
$photoExtensions = @(".jpg", ".jpeg", ".png", ".gif", ".bmp", ".tiff", ".webp")

# Get all photo files
$files = Get-ChildItem -Path $path -Recurse -File | 
         Where-Object { $photoExtensions -contains $_.Extension.ToLower() }

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
    Write-Progress -Activity "Processing photos..." -Status "$progress% complete" -PercentComplete $progress

    if ($group.Count -gt 1) {
        $duplicateGroups++
        $totalDuplicateFiles += $group.Count
        $mdOutput += "### Duplicate Group (Name+Size: $($group.Name))"
        
        # Randomly select one file to keep
        $random = Get-Random -Minimum 0 -Maximum $group.Count
        $fileToKeep = $group.Group[$random]
        $mdOutput += "- KEPT: $($fileToKeep.FullName) | Size: $([math]::Round($fileToKeep.Length / 1MB, 2)) MB | Last Modified: $($fileToKeep.LastWriteTime)"
        
        # Delete all other duplicates
        foreach ($file in $group.Group) {
            if ($file.FullName -ne $fileToKeep.FullName) {
                $mdOutput += "- DELETED: $($file.FullName) | Size: $([math]::Round($file.Length / 1MB, 2)) MB | Last Modified: $($file.LastWriteTime)"
                Remove-Item -Path $file.FullName -Force
            }
        }
        $mdOutput += ""
    }
}

# Add summary at top
$summary = @()
$summary += "# Duplicate Photo Files Report"
$summary += ""
$summary += "**Summary**"
$summary += "- Total Duplicate Groups: $duplicateGroups"
$summary += "- Total Duplicate Files Processed: $totalDuplicateFiles"
$summary += "- One random copy kept in each group, others deleted"
$summary += ""

$finalOutput = $summary + $mdOutput
$finalOutput | Out-File $outputFile
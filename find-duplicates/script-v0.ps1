$path = "C:\Toshiba Data"
$hashes = @{}

Get-ChildItem -Path $path -Recurse -File | ForEach-Object {
    try {
        $hash = (Get-FileHash $_.FullName -ErrorAction Stop).Hash
        if (![string]::IsNullOrEmpty($hash)) {
            if ($hashes.ContainsKey($hash)) {
                $hashes[$hash] += , $_
            } else {
                $hashes[$hash] = @($_)
            }
        }
    } catch {
        Write-Host "Skipping file (cannot hash): $($_.FullName)"
    }
}

# Prepare summary counts
$duplicateGroups = 0
$totalDuplicateFiles = 0

foreach ($entry in $hashes.GetEnumerator()) {
    if ($entry.Value.Count -gt 1) {
        $duplicateGroups++
        $totalDuplicateFiles += $entry.Value.Count
    }
}

# Build Markdown output
$mdOutput = @()
$mdOutput += "# Duplicate Files Report"
$mdOutput += ""
$mdOutput += "**Summary**"
$mdOutput += "- Total Duplicate Groups: $duplicateGroups"
$mdOutput += "- Total Duplicate Files: $totalDuplicateFiles"
$mdOutput += ""

foreach ($entry in $hashes.GetEnumerator()) {
    if ($entry.Value.Count -gt 1) {
        $mdOutput += "### Duplicate Group (Hash: $($entry.Key))"
        
        # Sort files by LastWriteTime descending (newest first)
        $sortedFiles = $entry.Value | Sort-Object LastWriteTime -Descending
        
        foreach ($file in $sortedFiles) {
            $sizeMB = [math]::Round($file.Length / 1MB, 2)
            $lastModified = $file.LastWriteTime
            $mdOutput += "- $($file.FullName) | Size: $sizeMB MB | Last Modified: $lastModified"
        }
        $mdOutput += ""
    }
}

$mdOutput | Out-File "C:\Toshiba Data\DuplicateFiles.md"
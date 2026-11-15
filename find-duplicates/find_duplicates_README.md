# Duplicate File Finder

A Python script to find duplicate files (identical content) on Windows. Reports paths, sizes, last modified dates, and whether filenames match.

## Prerequisites

- **Python 3.6+** (check with `python --version` or `py -3 --version`)

If Python isn't installed: [Download from python.org](https://www.python.org/downloads/) — during install, check "Add Python to PATH".

## Quick Start

1. Copy `find_duplicates.py` to your Windows PC (e.g. Desktop or `C:\Scripts`).
2. Open **Command Prompt** or **PowerShell**.
3. Run:

```cmd
python find_duplicates.py C:\Users\YourName\Documents
```

Replace `C:\Users\YourName\Documents` with the folder you want to scan.

## Usage Examples

### Scan entire user folder

```cmd
python find_duplicates.py C:\Users\YourName
```

### Exclude large/system folders (faster scan)

```cmd
python find_duplicates.py C:\ --exclude "C:\Windows,C:\Program Files,C:\Program Files (x86),C:\$Recycle.Bin"
```

### Only videos and photos

```cmd
python find_duplicates.py C:\Users\YourName\Pictures --extensions ".mp4,.avi,.mkv,.jpg,.jpeg,.png,.gif,.bmp"
```

### Save report to file

```cmd
python find_duplicates.py C:\Users\YourName\Documents --output duplicates_report.txt
```

### CSV output (for Excel)

```cmd
python find_duplicates.py C:\Users\YourName\Documents --output report.csv --csv
```

### Skip tiny files (e.g. under 1KB)

```cmd
python find_duplicates.py C:\ --min-size 1024
```

## Output Columns

| Field | Description |
|-------|-------------|
| **Path** | Full path to the file |
| **Size** | File size (bytes + human-readable) |
| **Last Modified** | Date/time the file was last changed |
| **Same Content** | YES if file content is identical (SHA-256 hash) |
| **Same Filename** | YES if all duplicates have the same filename |
| **Is Code Repo** | Yes if file is inside a `.git` or `.hg` folder |
| **Recommended Keep** | Which copy the script suggests keeping |

## Which Copy to Keep (Code Repos)?

The script recommends a copy based on:
1. **Prefer `.git` presence** — If one path is under a Git repo and another isn’t, it prefers the Git repo.
2. **Prefer newest** — Among similar copies, it prefers the one with the latest modification date.

Use this as a guide only. Before deleting:
- Open both copies and compare.
- Check if one is under version control (has `.git`).
- Make sure the one you keep builds/runs correctly.
- Back up important data.

## Performance Notes

- **Videos**: Large video files are hashed in chunks, so scanning may take time.
- **Exclusions**: Use `--exclude` for `C:\Windows`, `C:\Program Files`, etc. to speed up scans.
- **Extensions**: Use `--extensions` to scan only certain types (e.g. videos/photos).

## Batch File (Double-Click)

Create `find_duplicates.bat`:

```batch
@echo off
echo Duplicate File Finder
echo.
set /p DIR="Enter folder to scan (or press Enter for current): "
if "%DIR%"=="" set DIR=%CD%
python "%~dp0find_duplicates.py" "%DIR%" --output "%~dp0duplicates_report.txt"
pause
```

Place it next to `find_duplicates.py` and double-click to run.

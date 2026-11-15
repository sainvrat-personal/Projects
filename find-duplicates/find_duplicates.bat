@echo off
echo Duplicate File Finder
echo ---------------------
echo.
set /p DIR="Enter folder to scan (e.g. C:\Users\YourName) or press Enter for current folder: "
if "%DIR%"=="" set DIR=%CD%
echo.
echo Scanning %DIR% ...
echo Excluding Windows/Program Files for faster scan.
echo.
python "%~dp0find_duplicates.py" "%DIR%" --exclude "C:\Windows,C:\Program Files,C:\Program Files (x86),C:\$Recycle.Bin" --output "%~dp0duplicates_report.txt"
echo.
echo Report saved to: %~dp0duplicates_report.txt
pause

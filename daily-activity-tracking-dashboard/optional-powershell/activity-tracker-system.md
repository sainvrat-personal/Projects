# Optional PowerShell Helpers

This guide covers only the optional Windows PowerShell scripts in this folder. The scripts do not read this guide or the weekly Markdown template at runtime. Docker Desktop with Docker Compose v2 is required; host Java and Maven are not.

The dashboard and rollover are provided by the Java services in this project. The default local data root is `%USERPROFILE%\OneDrive\Documents\Tracker-Docs`: active weekly Markdown files are at its root, archived weeks and `activity-tracker.log` are in `Activity-log`, and monthly PDFs are in `Monthly-Expenditure`.

## Scripts

| Script | Purpose |
|---|---|
| `Start-ActivityDashboard.ps1` | Configures tracker/archive/financial-document paths in `.env`, builds/starts Docker Compose, checks both backends, and opens the dashboard. |
| `Install-WeeklyActivityTracker.ps1` | Creates the archive folder and registers a per-user Scheduled Task to start the Docker service at sign-in and daily for recovery. |
| `Archive-WeeklyActivity.ps1` | Starts the Docker service if needed, then calls `POST /api/rollover`; it contains no rollover implementation. |
| `Build-ActivityTrackerPackage.ps1` | Builds the PowerShell-only helper ZIP from an explicit allowlist. |

PowerShell and the Windows `ScheduledTasks` module are needed for task installation and management. Java and Maven run only inside the container build. Python is not required.

## Run the helpers

Run commands from the project root (the parent directory of `optional-powershell`). The scripts default to the Documents `Tracker-Docs` folder when it exists; otherwise, pass the data root with `-TrackerDirectory`.

Start the dashboard and build the image:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Start-ActivityDashboard.ps1"
```

To select a different data root:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Start-ActivityDashboard.ps1" -TrackerDirectory "C:\path\to\daily-tracking"
```

Install or repair automatic startup:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Install-WeeklyActivityTracker.ps1"
```

Pass the same `-TrackerDirectory` argument to the installer when the documents are outside the default `Tracker-Docs` folder. The selected data root must contain both `Activity-log` and `Monthly-Expenditure`.

Manually request rollover:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Archive-WeeklyActivity.ps1"
```

Build the PowerShell-only helper ZIP:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Build-ActivityTrackerPackage.ps1"
```

The builder writes `Activity-Tracker-Powershell-Service.zip` in the project root. It contains only the optional PowerShell scripts and helper documentation; it excludes Java source, dashboard UI, templates, PDFs, and personal activity records.

## Scheduled task

The task is named `Activity Tracker Dashboard (username)`. It starts at sign-in and has a daily 12:05 AM recovery trigger. The container's Java scheduler performs rollover; Task Scheduler does not move or modify weekly sheets. Enable Docker Desktop's **Start Docker Desktop when you sign in** setting so the container's `unless-stopped` policy can bring it back after a Windows restart.

Inspect status and last result:

```powershell
$taskName = "Activity Tracker Dashboard ($env:USERNAME)"
Get-ScheduledTask -TaskName $taskName
Get-ScheduledTaskInfo -TaskName $taskName
```

Disable or enable scheduled starts:

```powershell
Disable-ScheduledTask -TaskName "Activity Tracker Dashboard ($env:USERNAME)"
Enable-ScheduledTask -TaskName "Activity Tracker Dashboard ($env:USERNAME)"
```

Remove only the task:

```powershell
Unregister-ScheduledTask -TaskName "Activity Tracker Dashboard ($env:USERNAME)" -Confirm:$false
```

Disabling or removing the task does not stop an already-running container. To pause the dashboard and rollover, run `docker compose --project-name activity-dashboard stop` from the Compose directory. Removing the task leaves scripts, image, tracker sheets, archives, and logs in place.

The installer replaces this user's prior task, `Activity Tracker - Archive and Reset (username)`. It removes the legacy task `Weekly Activity Tracker - Archive and Reset` only when its recorded owner matches the current user.

## Notes

- The Java service embeds the weekly sheet template. The Markdown reference template is not read by these scripts or by rollover.
- Keep each person's tracker, archive, and financial document folders private and separate.
- See `README.md` in the Java package for Docker configuration, rollover behavior, dashboard metrics, and API details.

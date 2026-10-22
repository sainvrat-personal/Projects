# Optional PowerShell Helpers

This guide covers only the optional Windows PowerShell scripts in this folder. The scripts do not read this guide or the weekly Markdown template at runtime. Docker Desktop with Docker Compose v2 is required; host Java and Maven are not.

The dashboard and rollover are provided by the separate `Activity-Dashboard-Java-Service.zip`. Extract that package first, then extract this helper ZIP into the same tracker root so `activity-dashboard` and `optional-powershell` are sibling folders.

## Scripts

| Script | Purpose |
|---|---|
| `Start-ActivityDashboard.ps1` | Configures tracker/archive host paths in `.env`, builds/starts Docker Compose, checks health, and opens the dashboard. |
| `Install-WeeklyActivityTracker.ps1` | Creates the archive folder and registers a per-user Scheduled Task to start the Docker service at sign-in and daily for recovery. |
| `Archive-WeeklyActivity.ps1` | Starts the Docker service if needed, then calls `POST /api/rollover`; it contains no rollover implementation. |
| `Build-ActivityTrackerPackage.ps1` | Builds the PowerShell-only helper ZIP from an explicit allowlist. |

PowerShell and the Windows `ScheduledTasks` module are needed for task installation and management. Java and Maven run only inside the container build. Python is not required.

## Run the helpers

Run commands from the tracker root (the parent directory of `optional-powershell`).

Start the dashboard and build the image:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Start-ActivityDashboard.ps1"
```

Install or repair automatic startup:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Install-WeeklyActivityTracker.ps1"
```

Manually request rollover:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Archive-WeeklyActivity.ps1"
```

Build the PowerShell-only helper ZIP:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ".\optional-powershell\Build-ActivityTrackerPackage.ps1"
```

The builder writes `Activity-Tracker-Service.zip` in the tracker root. Despite its historical filename, it contains only the optional PowerShell scripts and helper documentation; it excludes Java source, dashboard UI, template, and personal records. The separate Java-service ZIP must also be extracted into the same tracker root.

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

Disabling or removing the task does not stop an already-running container. To pause the dashboard and rollover, run `docker compose stop` from `activity-dashboard`. Removing the task leaves scripts, image, tracker sheets, archives, and logs in place.

The installer replaces this user's prior task, `Activity Tracker - Archive and Reset (username)`. It removes the legacy task `Weekly Activity Tracker - Archive and Reset` only when its recorded owner matches the current user.

## Notes

- The Java service embeds the weekly sheet template. The Markdown reference template is not read by these scripts or by rollover.
- Keep each person's tracker and archive folders private and separate.
- See `activity-dashboard\README.md` in the Java package for Docker configuration, rollover behavior, dashboard metrics, and API details.

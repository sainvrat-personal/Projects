# Activity Dashboard — Low-Level Design

## 1. Package layout

```text
activity-dashboard/
├── compose.yaml
├── .env.example
├── README.md
├── architecture.md
├── HLD.md
├── LLD.md
├── Weekly-Activity-Template.md
├── frontend-service/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── static/{index.html,app.js,styles.css}
└── backend-service/
    ├── Dockerfile
    ├── pom.xml
    └── src/
        ├── main/java/local/activitytracker/dashboard/
        │   ├── ActivityDashboardApplication.java
        │   ├── ActivityCategory.java
        │   ├── ActivityClassifier.java
        │   ├── DashboardController.java
        │   ├── DashboardResponse.java
        │   ├── WeeklyActivityRolloverService.java
        │   └── WeeklyActivityScanner.java
        ├── main/resources/
        │   └── application.properties
        └── test/java/local/activitytracker/dashboard/
            ├── WeeklyActivityRolloverServiceTest.java
            └── WeeklyActivityScannerTest.java
```

## 2. HTTP API

The browser connects to the Nginx frontend through the host loopback port. Nginx serves UI files and proxies backend API/documentation requests over the private Compose network to backend port `8080`.

### OpenAPI and Swagger UI

The `springdoc-openapi-starter-webmvc-ui` dependency generates OpenAPI 3 documentation from the Spring MVC controller and its OpenAPI annotations. The backend exposes:

- `GET /v3/api-docs` — generated OpenAPI JSON.
- `GET /swagger-ui.html` — frontend-proxied Swagger UI entry point for interactive API exploration.

Both features are explicitly enabled in backend `application.properties`. Nginx proxies their paths so they remain available on the frontend origin; the backend itself is not published to the host.

### Nginx routing

`frontend-service/nginx.conf` serves static files from `/usr/share/nginx/html` and forwards these paths without rewriting the URI:

- `/api/` to `http://backend:8080`.
- `/swagger-ui.html` and `/swagger-ui/` to the backend's Swagger UI endpoints and assets.
- `/v3/api-docs` and nested paths such as `/v3/api-docs/swagger-config` to the backend's generated OpenAPI endpoints.

All other paths are resolved from the frontend static root. The UI continues to call the relative `/api` path, preserving same-origin browser requests and avoiding CORS configuration.

| Method and path | Parameters | Success result | Error behavior |
|---|---|---|---|
| `GET /api/health` | None | `{"status":"ok"}` | Standard Spring error response if the application cannot serve the request. |
| `GET /api/dashboard` | Optional `from` and `to`, each `yyyy-MM-dd` | `DashboardResponse` JSON | Invalid date syntax is rejected by Spring MVC. `from > to` returns HTTP 400. Missing/unreadable tracker paths fail rather than being treated as empty data. |
| `POST /api/rollover` | None | `{"status":"ok","message":"Weekly activity sheet is up to date."}` | I/O failure maps to HTTP 500 with a rollover failure message. |

The dashboard response shape is:

```json
{
  "from": "2026-10-01",
  "to": "2026-10-31",
  "filesScanned": 4,
  "taskCount": 18,
  "activeDays": 9,
  "knownDurationMinutes": 315,
  "categories": [
    {
      "category": "Household",
      "taskCount": 5,
      "activeDays": 3,
      "knownDurationMinutes": 120
    }
  ],
  "dailyBreakdown": [
    {"date": "2026-10-01", "day": "Thursday", "taskCount": 2}
  ],
  "activities": [
    {
      "date": "2026-10-01",
      "day": "Thursday",
      "category": "Household",
      "description": "Washed dishes",
      "durationMinutes": 30,
      "sourceFile": "Week-28.09.2026-04.10.2026-Activity.md"
    }
  ],
  "warnings": []
}
```

`from`/`to` in the response reflect requested bounds when supplied; otherwise they are the earliest/latest activity dates found, or `null` when there are no activities. Duration values are `null` per activity when not explicitly recorded; total duration sums only known values.

## 3. Scanner and Markdown parsing

### File discovery and validation

1. Scan the configured source directory followed by configured archive directories.
2. Consider regular files whose names start with `Week-` and end with `-Activity.md`.
3. Require the strict weekly name shape `Week-dd.MM.yyyy-dd.MM.yyyy-Activity.md`.
4. Validate that the start date is Monday and end date is exactly six days later.
5. For duplicate valid filenames across directories, retain the most recently modified copy and add a warning for the ignored copy.
6. Read accepted files as UTF-8. Invalid date ranges are skipped with warnings.

The scanner throws an explicit error when the source path is missing or is not a directory. A configured optional archive path that does not exist is skipped. A path that exists but is not a directory is an error.

### Activity extraction

- The recognized weekday headings are `## Monday` through `## Sunday`.
- Tasks are recognized under a corresponding `### Completed` section.
- Task line syntax is a top-level bullet using `-`, `*`, or `+`; optional Markdown task checkboxes (`[ ]`, `[x]`) are supported.
- Empty task text is ignored.
- The calendar date for each activity is calculated as the filename's Monday plus the weekday offset.
- Weekly summary sections and other headings are not parsed as activities.

### Annotation handling

Example:

```markdown
- Washed dishes [duration: 30m] [category: Household]
```

- Duration grammar: `[duration: ...]`, case-insensitive, supporting hours and/or minutes such as `[duration: 1h 15m]`. Positive values within integer-minute range are recorded.
- Category grammar: `[category: ...]`, case-insensitive. A supported `ActivityCategory` label or enum-style name is accepted.
- A recognized category annotation takes precedence over inference. Unknown category annotations produce a warning and fall back to inference.
- If no valid explicit category exists, the classifier counts matching configured keywords per category and selects the category with the highest positive score; ties follow enum order. If there is no match, the result is `Other`.
- Recognized annotation text is removed from the activity description shown in the UI.
- Malformed, overflowing, zero, or otherwise invalid duration data does not contribute to duration totals and is warned about where detected.

Supported category labels: `Household`, `Health`, `Work / Study`, `Errands`, `Family / Social`, `Leisure`, `Personal`, and `Other`.

### Aggregation

After date filtering, the scanner:

- Sorts activities by date, category, then description.
- Counts tasks and distinct active dates.
- Sums only non-null durations.
- Groups categories into task count, distinct active-day count, and known-duration total.
- Groups daily activity counts by date.
- Returns distinct warnings and the count of unique accepted files scanned.

## 4. Rollover behavior

### Triggering

`WeeklyActivityRolloverService` runs from both:

- `ApplicationReadyEvent`, to create/catch up immediately after startup.
- Spring `@Scheduled`, using `tracker.rollover.cron` (default `0 5 0 * * *`).
- The manual `POST /api/rollover` endpoint, which invokes the same operation.

The service operation is synchronized to prevent overlapping rollover runs within one application process.

### Processing algorithm

1. Determine the current Monday and Sunday from the current local date.
2. Create tracker and archive directories if they do not exist.
3. Derive the current filename: `Week-dd.MM.yyyy-dd.MM.yyyy-Activity.md`.
4. Create the current weekly file using the embedded Monday–Sunday template and dated week range, using `CREATE_NEW`. If it already exists, preserve it unchanged.
5. Enumerate regular files in the tracker directory in filename order.
6. For each matching weekly filename, strictly parse both dates and verify Monday-to-Sunday validity.
7. Ignore invalid or non-prior weeks. For an earlier valid week, move it to the archive unless an identical target filename already exists.
8. On archive conflict, leave both source and destination untouched and log a warning.
9. Append each rollover event to `activity-tracker.log` in the archive directory and emit it to the Java logger.

Rollover does not overwrite the current sheet, does not remove files when an archive conflict occurs, and does not process nonconforming weekly filenames.

## 5. Browser behavior

- `frontend-service/static/index.html` defines filter controls, KPI elements, category/day summaries, activity table, warnings, and status messaging.
- `app.js` calls `GET /api/dashboard` on page load and on filter/refresh actions.
- Query parameters are included only for populated date inputs. “Show all” clears both values and reloads.
- Rendering uses DOM element creation and `textContent` for activity values.
- Failed HTTP requests or fetch errors show a status message; successful responses update all dashboard regions.
- `frontend-service/static/styles.css` provides presentation and responsive layout; it contains no service integration logic.

## 6. Configuration reference

| Setting | Default / Compose value | Description |
|---|---|---|
| `DASHBOARD_PORT` | `8080` | Host loopback port mapped to frontend container port `80`. |
| `TRACKER_SOURCE_HOST_PATH` | `..` in `.env.example` | Host path bind-mounted to `/data/tracker`. |
| `TRACKER_ARCHIVE_HOST_PATH` | Required by Compose | Host path bind-mounted to `/data/archive`. |
| `TRACKER_SOURCE_DIRECTORY` | `/data/tracker` in Compose | Application path for active sheets. |
| `TRACKER_ARCHIVE_DIRECTORY` | `/data/archive` in Compose | Application path for archive scans, rollover moves, and log. |
| `TRACKER_LEGACY_ARCHIVE_DIRECTORY` | Empty | Optional additional read-only scanner directory. |
| `TRACKER_ROLLOVER_CRON` | `0 5 0 * * *` | Spring cron expression, including seconds. |
| `TZ` | `Asia/Kolkata` in Compose | Container timezone. |
| `JAVA_TOOL_OPTIONS` | `-Duser.timezone=Asia/Kolkata` in Compose | Java process timezone. |

Application defaults are also declared in `application.properties`; Compose overrides source/archive paths and supplies the rollover schedule.

## 7. Build and tests

- `backend-service/Dockerfile` runs `mvn --batch-mode package` in a Maven 3.9.9 / Java 17 builder stage and copies the resulting executable JAR into a Java 17 JRE runtime image.
- `WeeklyActivityScannerTest` covers scanner and parsing behavior.
- `WeeklyActivityRolloverServiceTest` covers weekly sheet creation, preservation, and archiving behavior.
- Docker Compose starts separate `frontend` and `backend` services. Only the frontend port is published to `127.0.0.1`; tracker/archive host directories are mounted into the backend only.
- The frontend image is built from `frontend-service/Dockerfile` using the dashboard static files and Nginx configuration. The backend image is built from `backend-service/Dockerfile`.

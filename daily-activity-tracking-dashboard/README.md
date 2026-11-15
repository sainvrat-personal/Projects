# Activity Dashboard Services

This package contains a local frontend, activity backend, and financial-analysis backend, orchestrated by Docker Compose. The Nginx frontend serves the dashboard UI and reverse-proxies API requests to the Java services. The activity backend scans Markdown sheets and handles weekly rollover. The financial-analysis backend reads monthly expenditure PDFs and performs local text extraction/OCR. The source package includes a blank `Weekly-Activity-Template.md` reference copy. Windows helper scripts are distributed separately in the PowerShell-only ZIP.

## Requirements

- Docker Desktop with Docker Compose v2

## Package layout

- `backend-service/`: Spring Boot API, scanning and rollover logic, tests, and configuration.
- `financial-analysis-service/`: separate Spring Boot API for local monthly PDF analysis.
- `frontend-service/`: Nginx configuration and static dashboard UI.
- `README.md`: setup, configuration, and API instructions.
- `architecture.md`, `HLD.md`, `LLD.md`: architecture diagram and high-/low-level design documentation.
- `Weekly-Activity-Template.md`: blank Monday-Sunday tracker template. The backend generates its own dated copy at startup.
- `compose.yaml`, service Dockerfiles: build and run the frontend and both backend containers.
- `.env.example`: example host bind-mount and rollover configuration.

## Design documentation

- [Architecture](./architecture.md): system context, service integrations, and deployment diagram.
- [High-level design (HLD)](./HLD.md): components, runtime flows, and system-level decisions.
- [Low-level design (LLD)](./LLD.md): API contracts, parsing rules, rollover behavior, and configuration.

The frontend container serves the static UI and is the only service published to host loopback. It proxies `/api/` and Swagger paths to the activity backend, and `/api/finance` to the financial-analysis backend over the private Compose network. Backend containers are not published directly to the host. Docker builds the Spring Boot JARs inside Java 17/Maven builder images, so Java and Maven do not need to be installed on the host. Containers use `restart: unless-stopped`; start Docker Desktop at sign-in for them to return after a Windows restart. The activity backend uses Asia/Kolkata time so its default 12:05 AM rollover matches the local schedule.

## Configure tracker and archive folders

The activity service reads Markdown files from the tracker folder and archive folder. Financial PDFs are read from the monthly expenditure folder, mounted read-only into the separate analysis service. The provided PowerShell launcher defaults to the current user's Documents `Tracker-Docs` folder, with active weekly sheets at the root, archived sheets under `Activity-log`, and PDFs under `Monthly-Expenditure`. It writes `.env` beside `compose.yaml`. For manual Docker Compose use, copy `.env.example` to `.env` and set all host paths:

```powershell
Copy-Item .\.env.example .\.env
notepad .\.env
```

`TRACKER_SOURCE_HOST_PATH` and `TRACKER_ARCHIVE_HOST_PATH` are mounted into the activity backend as `/data/tracker` and `/data/archive`. `FINANCIAL_DOCUMENTS_HOST_PATH` is mounted read-only into the financial service as `/data/financial`. Rollover creates the active/archive directories if missing; the PDF folder must already exist. These local documents are not uploaded or sent to external APIs.

## Build and run with Docker Compose

From this directory:

```powershell
Copy-Item .\.env.example .\.env
# Edit all tracker and financial-document host paths in .env.
docker compose up --build -d
```

Docker builds and tests the Java services using pinned Java 17 and Maven 3.9.9 builder images, then runs them in Java 17 runtime images. The financial service includes local Tesseract OCR for scanned PDFs. Docker also builds the Nginx frontend image. Open `http://127.0.0.1:8080/`. Only the frontend is published to host loopback; both backends are reachable only through the private Compose network.

Inspect the service and logs:

```powershell
docker compose ps
docker compose logs -f frontend backend financial-analysis
```

Stop or restart the service:

```powershell
docker compose stop
docker compose start
```

`docker compose down` removes the stopped container but preserves mounted tracker/archive files and the built image.

## Weekly rollover

At startup, the service ensures that the current Monday-to-Sunday Markdown sheet exists, preserving it if present. It archives valid weekly sheets from earlier weeks into the configured archive directory. It repeats this check daily at 12:05 AM by default. Startup also catches up after the service has been stopped.

The dated sheet is created in `TRACKER_SOURCE_DIRECTORY`, not beside this README or the reference template. The template shown in `Weekly-Activity-Template.md` is a documentation/reference copy; the Java service uses its own embedded template, so moving this Markdown file does not affect rollover.

The cron expression can be changed with `TRACKER_ROLLOVER_CRON` in `.env` (Spring cron format, including seconds). Rollover events and archive conflicts are written to `activity-tracker.log` in the archive directory and to the Java service log.

To have rollover happen unattended, leave the container running and enable Docker Desktop to start at Windows sign-in. The Java service's scheduled rollover runs inside the container.

The manual rollover endpoint is:

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8080/api/rollover" -Method Post
```

The endpoint only works while the service is running.

## Dashboard API

```text
GET  http://127.0.0.1:8080/api/health
GET  http://127.0.0.1:8080/api/dashboard
GET  http://127.0.0.1:8080/api/dashboard?from=2026-10-01&to=2026-10-31
POST http://127.0.0.1:8080/api/rollover
GET  http://127.0.0.1:8080/api/finance/health
GET  http://127.0.0.1:8080/api/finance/months
GET  http://127.0.0.1:8080/api/finance?month=2026-08
```

## Financial analysis

The **Financial analysis** section provides a month dropdown populated from `Monthly-Expenditure/<year>/<month>/`. It scans PDF files for that month, omitting filenames containing `Pani camper` or `Dudh Hisab` because that ledger is duplicated in the other document. Text-based PDFs are parsed locally; scanned PDFs use the local Tesseract OCR engine. For monthly expenditure, each PDF's stated final-used or expenditure total takes precedence over its extracted detail rows; rows from that PDF are not counted again. Rows are used for documents without an explicit total. The category breakdown is based on recognized detail rows and may not reconcile with a document's summary total. OCR, transaction recognition, and category assignment are heuristic, so verify all values against the source PDFs. The service does not modify PDFs or transmit financial data.

Supported endpoints are `GET /api/finance/months` and `GET /api/finance?month=yyyy-MM`. If a PDF has no readable text or no recognized transaction rows, the dashboard shows a review warning instead of treating unknown amounts as zero.

Interactive Swagger UI is available at `http://127.0.0.1:8080/swagger-ui.html`; the generated OpenAPI JSON is at `http://127.0.0.1:8080/v3/api-docs`. Nginx reverse-proxies these paths to the backend. As with the dashboard and API, they are reachable from the host only through the frontend's loopback binding.

The dashboard reports task count, active days, category summaries, and explicitly recorded time. It counts top-level bullets under each day's `### Completed` section. Add annotations to a bullet to set its category or time:

```markdown
- Washed dishes [duration: 30m] [category: Household]
```

Unannotated time is not guessed. Categories are heuristic unless explicitly specified.

## Safety and privacy

- The frontend binds to host loopback and the backend has no host-published port.
- Dashboard scanning is read-only. The rollover changes only the configured tracker and archive folders.
- Financial PDFs are mounted read-only and are analyzed locally by the separate financial-analysis service.
- The financial analysis is heuristic and is not accounting, tax, investment, or other professional advice.
- Existing current-week sheets are preserved.
- If a same-name archive already exists with identical contents, the redundant active-folder copy is removed. If the contents differ, both files are left in place and the conflict is logged.
- This source package contains no dated activity sheets, archives, or logs. Keep your own activity records private.
- The PowerShell launcher and installer are optional and available in the separate helper ZIP.

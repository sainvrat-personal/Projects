# Activity Dashboard Services

This package contains a local frontend and backend, orchestrated by Docker Compose. The Nginx frontend serves the dashboard UI and reverse-proxies API and Swagger requests to the Spring Boot backend. The backend handles activity scanning and weekly-sheet creation and archiving. The source package includes a blank `Weekly-Activity-Template.md` reference copy. Windows helper scripts are distributed separately in the PowerShell-only ZIP.

## Requirements

- Docker Desktop with Docker Compose v2

## Package layout

- `backend-service/`: Spring Boot API, scanning and rollover logic, tests, and configuration.
- `frontend-service/`: Nginx configuration and static dashboard UI.
- `README.md`: setup, configuration, and API instructions.
- `architecture.md`, `HLD.md`, `LLD.md`: architecture diagram and high-/low-level design documentation.
- `Weekly-Activity-Template.md`: blank Monday-Sunday tracker template. The backend generates its own dated copy at startup.
- `compose.yaml`, service Dockerfiles: build and run the frontend and backend containers.
- `.env.example`: example host bind-mount and rollover configuration.

## Design documentation

- [Architecture](./architecture.md): system context, service integrations, and deployment diagram.
- [High-level design (HLD)](./HLD.md): components, runtime flows, and system-level decisions.
- [Low-level design (LLD)](./LLD.md): API contracts, parsing rules, rollover behavior, and configuration.

The frontend container serves the static UI and is the only service published to host loopback. It proxies `/api/`, `/swagger-ui`, and `/v3/api-docs` to the backend over the private Compose network. The backend container is not published directly to the host. Docker builds the Spring Boot JAR inside a Java 17/Maven container image, so Java and Maven do not need to be installed on the host. Both containers use `restart: unless-stopped`; start Docker Desktop at sign-in for them to return after a Windows restart. The backend uses Asia/Kolkata time so its default 12:05 AM rollover matches the local schedule.

## Configure tracker and archive folders

The service reads Markdown files from the configured tracker folder and its archive folder. The provided PowerShell launcher creates `activity-dashboard\.env` with the current tracker directory and the current user's `Documents\Activity-log`. For manual Docker Compose use, copy `.env.example` to `.env` and set host paths. The source path should point to the folder containing your active `Week-...-Activity.md` sheet:

```powershell
Copy-Item .\.env.example .\.env
notepad .\.env
```

`TRACKER_SOURCE_HOST_PATH` and `TRACKER_ARCHIVE_HOST_PATH` are host paths mounted into the container as `/data/tracker` and `/data/archive`. The Java application creates either directory if missing. Do not mount another person's or a shared archive.

## Build and run with Docker Compose

From this directory:

```powershell
Copy-Item .\.env.example .\.env
# Edit TRACKER_SOURCE_HOST_PATH and TRACKER_ARCHIVE_HOST_PATH in .env.
docker compose up --build -d
```

Docker builds and tests the backend using the pinned Java 17 and Maven 3.9.9 builder image, then runs the JAR in a Java 17 runtime image. It also builds the Nginx frontend image. Open `http://127.0.0.1:8080/`. Only the frontend is published to host loopback; the backend is reachable through the private Compose network.

Inspect the service and logs:

```powershell
docker compose ps
docker compose logs -f frontend backend
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
```

Interactive Swagger UI is available at `http://127.0.0.1:8080/swagger-ui.html`; the generated OpenAPI JSON is at `http://127.0.0.1:8080/v3/api-docs`. Nginx reverse-proxies these paths to the backend. As with the dashboard and API, they are reachable from the host only through the frontend's loopback binding.

The dashboard reports task count, active days, category summaries, and explicitly recorded time. It counts top-level bullets under each day's `### Completed` section. Add annotations to a bullet to set its category or time:

```markdown
- Washed dishes [duration: 30m] [category: Household]
```

Unannotated time is not guessed. Categories are heuristic unless explicitly specified.

## Safety and privacy

- The frontend binds to host loopback and the backend has no host-published port.
- Dashboard scanning is read-only. The rollover changes only the configured tracker and archive folders.
- Existing current-week sheets are preserved.
- If a same-name archive already exists, both copies are left in place and the conflict is logged.
- This source package contains no dated activity sheets, archives, or logs. Keep your own activity records private.
- The PowerShell launcher and installer are optional and available in the separate helper ZIP.

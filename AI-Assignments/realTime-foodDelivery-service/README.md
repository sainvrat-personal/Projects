# SwiftEats — Real-Time Food Delivery Platform

SwiftEats is a scalable food delivery backend for Maharashtra, built as **six Spring Boot microservices** sharing a **`platform-lib`** domain library, with Redis, Kafka, and RabbitMQ. **backend-service** (:8080) is the single public API gateway for UIs and Swagger. It implements Assignment 1 (orders, menu, GPS tracking) and Assignment 2 (delivery failure analytics).

## Repository layout

```
realTime-foodDelivery-service/
├── docker-compose.yml          # Full local stack (API + infra + three UIs)
├── ARCHITECTURE.md             # Design rationale and diagrams
├── API-SPECIFICATION.yml       # OpenAPI 3.0 spec
├── PROJECT_STRUCTURE.md        # Module/folder guide
├── CHAT_HISTORY.md             # AI-assisted design journey (summary)
├── COVERAGE.md                 # Unit test coverage report instructions
├── DOMAIN_MODEL.md             # A1 ↔ A2 field mapping
├── HIGH_LEVEL_DESIGN.md
├── LOW_LEVEL_DESIGN.md
├── services/
│   ├── platform-lib/           # Shared domain code (JPA, controllers, workers)
│   ├── backend-service/        # Gateway + auth + tracking (8080)
│   ├── entities-service/       # Admin entity CRUD (8081)
│   ├── order-service/          # Order lifecycle (8082)
│   ├── payment-service/        # Payment worker (8083)
│   ├── refund-service/         # Refund flow (8084)
│   ├── analytics-service/      # Analytics APIs (8085)
│   └── swifteats-api/          # Legacy monolith (optional local dev)
├── ui-service/                 # Customer SPA — browse, order, track
├── admin-dashboard/            # Ops SPA — onboarding, order states, CSV import
├── analytics-dashboard/        # Analytics SPA — Assignment 2 UC1–UC6
├── postman/                    # Postman collection + local environment
├── scripts/                    # e2e-smoke.sh, analytics output helpers
└── third-assignment-sample-data-set/  # Assignment 2 CSV files
```

## Prerequisites

- **Docker** and **Docker Compose** (recommended)
- Or locally: JDK 17+, Maven 3.9+, PostgreSQL 15, Redis 7, RabbitMQ 3.13, Kafka 3.7
- For UI local dev (without Docker): **Node.js 18+** and npm

## Quick start (Docker Compose)

From the repository root (where `docker-compose.yml` lives):

```bash
cp .env.example .env
docker compose up --build
```

Wait until `backend-service` is healthy. Then use:

| Service | URL |
|---------|-----|
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Customer UI | http://localhost:3000 |
| Admin UI | http://localhost:3001 |
| Analytics UI | http://localhost:3002 |

For UI demo walkthroughs, see **[How to run (UI)](#how-to-run-ui)** below.

> **Local demo only:** Do not expose Docker ports to untrusted networks without rotating all API keys and tokens in `.env`.

### Credentials (local)

Copy [`.env.example`](./.env.example) to `.env`. **Do not commit `.env` or reuse these values in shared deployments.**

| Setting | Purpose |
|---------|---------|
| `ADMIN_API_KEY` | Header `X-Admin-Api-Key` for `/api/v1/admin/*` |
| `CUSTOMER_ID` + `CUSTOMER_API_TOKEN` | Headers `X-Customer-Id` + `X-Customer-Api-Key` for orders/tracking |
| `DRIVER_ID` + `DRIVER_API_TOKEN` | Header `X-Driver-Api-Key` for GPS ingest |
| PostgreSQL | `swifteats` / `swifteats` (db: `swifteats`) |
| RabbitMQ | `guest` / `guest` |

### Services started

| Service    | Port(s)        | Purpose                          |
|------------|----------------|----------------------------------|
| backend-service | 8080        | Gateway + auth + tracking + Swagger hub |
| entities-service | 8081       | Admin restaurant/entity CRUD     |
| order-service | 8082          | Order lifecycle APIs             |
| payment-service | 8083        | Async payment processing         |
| refund-service | 8084         | Refund initiation → success      |
| analytics-service | 8085      | CSV import + insight APIs        |
| ui-service | 3000 | Customer browse / order / track |
| admin-dashboard | 3001 | Restaurant & order ops      |
| analytics-dashboard | 3002 | Assignment 2 insights UI |
| postgres   | 5432           | Operational + analytics schemas  |
| redis      | 6379           | Menu cache + GPS hot path        |
| rabbitmq   | 5672, 15672    | Async payment queue (+ mgmt UI)  |
| kafka      | 9092           | GPS location stream              |

Flyway runs via the **`db-migrate`** one-shot container before any API service starts (all JVM services wait for `service_completed_successfully`).

### Build a single service (Maven)

From `services/`:

```bash
mvn -pl order-service -am package
java -jar order-service/target/order-service-*.jar
```

Unified Swagger: http://localhost:8080/swagger-ui.html (dropdown lists all six service specs).

## How to run (UI)

Three React SPAs wrap the same REST API. In Docker, each container serves static files via nginx and proxies `/api` to `backend-service:8080`. In local dev, Vite proxies `/api` to `http://localhost:8080`.

### Start the UIs

**Docker (recommended — backend + all three UIs):**

```bash
cp .env.example .env
docker compose up --build
```

**Local dev only (backend must already be running on port 8080):**

```bash
# Terminal 1 — customer UI (port 3000)
cd ui-service && cp .env.example .env && npm install && npm run dev

# Terminal 2 — admin UI (port 3001)
cd admin-dashboard && cp .env.example .env && npm install && npm run dev

# Terminal 3 — analytics UI (port 3002)
cd analytics-dashboard && cp .env.example .env && npm install && npm run dev
```

| UI | URL | Folder | Purpose |
|----|-----|--------|---------|
| Customer | http://localhost:3000 | `ui-service/` | Browse restaurants, place orders, track delivery |
| Admin | http://localhost:3001 | `admin-dashboard/` | Onboard restaurants, advance order states, import CSV |
| Analytics | http://localhost:3002 | `analytics-dashboard/` | Run Assignment 2 UC1–UC6 insight queries |

Default demo credentials: sign in at http://localhost:3000/login with `demo.customer@example.com` / `Demo@123`, or register a new account.

### UI demo walkthrough

End-to-end flow across all three apps (Assignment 1 + Assignment 2):

1. **Browse and order (Customer UI — http://localhost:3000)**
   - Open **Restaurants**, filter by city (e.g. Pune), and pick a restaurant (seed names show **`[T] `** prefix, e.g. `[T] Misal House`).
   - Add menu items to the cart and click **Place order**.
   - Note the order ID shown after checkout (you are redirected to tracking).

2. **Wait for payment confirmation**
   - On the track page, refresh until status moves from `PENDING_PAYMENT` to `CONFIRMED` (~1–2 seconds with RabbitMQ running).

3. **Advance kitchen / delivery (Admin UI — http://localhost:3001)**
   - Go to **Orders**, paste the order ID, set status to `PREPARING`, click **Update state**.
   - Repeat with `OUT_FOR_DELIVERY` (assigns a demo driver for GPS tracking).
   - Optionally set `DELIVERED` when the demo is complete.

4. **Live tracking (Customer UI — http://localhost:3000/track)**
   - With `GPS_SIMULATOR_ENABLED=true` in `docker-compose.yml` (default), driver location updates appear automatically after step 3.
   - The track page polls every few seconds and shows lat/lng once the order is `OUT_FOR_DELIVERY`.

5. **Import analytics data (Admin UI — http://localhost:3001/data)**
   - Click **Import sample CSV data** (loads `third-assignment-sample-data-set/` into the `analytics.*` schema).
   - Requires the API container with `SWIFTEATS_DATASET_PATH` mounted (default in docker-compose).

6. **Run insight queries (Analytics UI — http://localhost:3002)**
   - Select a use-case tab (UC1–UC6), adjust parameters if needed, click **Run analysis**.
   - Example: **UC1 Delays** → city `Pune`, date `2025-06-06`.
   - Example: **UC4 Compare cities** → Pune vs Mumbai, month `2025-08`.
   - Results show narrative text, recommendations, and bar charts.

Per-app READMEs with extra detail: [`ui-service/README.md`](./ui-service/README.md), [`admin-dashboard/README.md`](./admin-dashboard/README.md), [`analytics-dashboard/README.md`](./analytics-dashboard/README.md).

## Authentication

### Customer accounts

Customers **register and sign in** via the API or Customer UI (`/register`, `/login`). On success the API returns:

- `customerId` — UUID stored in the `customer` table
- `apiToken` — session token (sent as `X-Customer-Api-Key` on subsequent requests)

**Demo account** (seeded on startup): `demo.customer@example.com` / `Demo@123` — profile name `[T] Demo Customer`

| Endpoint | Auth | Purpose |
|----------|------|---------|
| `POST /api/v1/auth/register` | None | Create account (name, email, phone, password, address) |
| `POST /api/v1/auth/login` | None | Login with email or phone + password |
| `GET /api/v1/auth/me` | Customer headers | Read profile |
| `PATCH /api/v1/auth/profile` | Customer headers | Update profile |

Customer profile fields stored in `customer` table: name, email, phone, password hash, default delivery address (line1/line2, city, state, pincode), API token.

### Temporary data naming

Seed and user-created **display names** are prefixed with **`[T] `** to mark temporary/demo data:

| Source | Examples |
|--------|----------|
| Flyway seeds | `[T] Misal House`, `[T] Kolhapuri Misal`, `[T] Demo Customer`, `[T] Aarav Patil` |
| Admin create (restaurant/menu/cuisine) | Request `Pune Biryani Co` → stored `[T] Pune Biryani Co` |
| Customer register/profile | Request `Jane Doe` → stored `[T] Jane Doe` |

Implemented in `TemporaryDataLabels` (`platform-lib`). UIs and API responses show the prefixed names.

### Protected routes

| Actor | Headers | Protected routes |
|-------|---------|------------------|
| Admin | `X-Admin-Api-Key` | `/api/v1/admin/**` |
| Customer | `X-Customer-Id`, `X-Customer-Api-Key` | Auth profile, orders, tracking |
| Driver | `X-Driver-Api-Key` | `POST /api/v1/drivers/{driverId}/location` |

Order read and tracking enforce ownership (403 if the order belongs to another customer).

Set `SWIFTEATS_ALLOW_INSECURE_DEFAULTS=true` only for local Docker Compose. Shared or production hosts must use strong keys and `SWIFTEATS_ALLOW_INSECURE_DEFAULTS=false`.

## Build without Docker (API only)

From `services/`, build and run the gateway (proxies to other services if they are up):

```bash
cd services
mvn -pl backend-service -am package
java -jar backend-service/target/backend-service-*.jar
```

Or run the full microservice set locally (each in its own terminal) after starting Postgres, Redis, RabbitMQ, and Kafka.

Ensure infrastructure matches `application.yml` / environment variables in `.env.example`.

## Run tests

```bash
cd services/platform-lib
mvn test
```

Integration tests using Testcontainers are skipped automatically when Docker is unavailable.

Generate a coverage report:

```bash
cd services/platform-lib && mvn test
open target/site/jacoco/index.html   # macOS
```

See [COVERAGE.md](./COVERAGE.md). CI runs `platform-lib` tests on push/PR (`.github/workflows/ci.yml`).

## Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_HOST` | `localhost` | Database host |
| `POSTGRES_PORT` | `5432` | Database port |
| `POSTGRES_DB` | `swifteats` | Database name |
| `POSTGRES_USER` | `swifteats` | Database user |
| `POSTGRES_PASSWORD` | `swifteats` | Database password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka brokers |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `ADMIN_API_KEY` | *(required)* | Admin route authentication — no insecure default outside local `.env` |
| `SWIFTEATS_ALLOW_INSECURE_DEFAULTS` | `false` | Allow weak admin keys for local dev only |
| `SWIFTEATS_AUTH_ENABLED` | `true` | Must remain `true`; setting `false` fails startup |
| `GPS_RATE_LIMIT_PER_SECOND` | `10` | Per-driver GPS POST rate limit |
| `RESTAURANT_CACHE_ENABLED` | `true` | Redis menu/list cache |
| `PAYMENT_MESSAGING_ENABLED` | `true` | RabbitMQ payment worker |
| `PAYMENT_MOCK_FAILURE_RATE` | `0.05` | Mock gateway decline rate |
| `TRACKING_KAFKA_ENABLED` | `true` | Kafka GPS ingest path |
| `GPS_SIMULATOR_ENABLED` | `false` | In-process driver GPS simulator |
| `GPS_SIMULATOR_DRIVER_COUNT` | `50` (docker-compose) | Simulated drivers; max 50 at 5s interval ≈ 10 evt/s |
| `ANALYTICS_IMPORT_ON_STARTUP` | `false` | Auto-import A2 CSV on boot |

## Demo walkthrough

**Automated E2E check** (after `docker compose up --build`):

```bash
./scripts/e2e-smoke.sh
```

Load credentials from `.env` first:

```bash
set -a && source .env && set +a
```

### 1. Browse restaurants and menu

```bash
curl "http://localhost:8080/api/v1/restaurants?city=Pune"

curl "http://localhost:8080/api/v1/restaurants/22222222-2222-2222-2222-222222222201/menu"
```

### 2. Place an order

```bash
curl -X POST http://localhost:8080/api/v1/orders \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-order-001" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H "X-Customer-Api-Key: $CUSTOMER_API_TOKEN" \
  -d '{
    "restaurantId": "22222222-2222-2222-2222-222222222201",
    "deliveryAddress": "123 MG Road, Pune",
    "city": "Pune",
    "paymentMode": "UPI",
    "items": [{"menuItemId": "33333333-3333-3333-3333-333333333301", "quantity": 2}]
  }'
```

Save the returned `orderId`. Within ~1–2 seconds the outbox poller and payment worker transition the order to `CONFIRMED` (unless mock payment fails).

```bash
curl "http://localhost:8080/api/v1/orders/{orderId}" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H "X-Customer-Api-Key: $CUSTOMER_API_TOKEN"
```

### 3. Advance kitchen / delivery states (admin)

```bash
# Preparing
curl -X PATCH "http://localhost:8080/api/v1/admin/orders/{orderId}/state" \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"status":"PREPARING","changedBy":"kitchen"}'

# Out for delivery (assigns driver)
curl -X PATCH "http://localhost:8080/api/v1/admin/orders/{orderId}/state" \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{"status":"OUT_FOR_DELIVERY","changedBy":"kitchen"}'
```

### 4. Live driver tracking

With `GPS_SIMULATOR_ENABLED=true` in docker-compose, drivers emit GPS automatically.

```bash
# REST snapshot
curl "http://localhost:8080/api/v1/orders/{orderId}/tracking" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H "X-Customer-Api-Key: $CUSTOMER_API_TOKEN"

# SSE stream
curl -N "http://localhost:8080/api/v1/orders/{orderId}/tracking/stream" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H "X-Customer-Api-Key: $CUSTOMER_API_TOKEN"
```

Manual GPS post:

```bash
curl -X POST "http://localhost:8080/api/v1/drivers/$DRIVER_ID/location" \
  -H "Content-Type: application/json" \
  -H "X-Driver-Api-Key: $DRIVER_API_TOKEN" \
  -d '{"latitude":18.5204,"longitude":73.8567,"heading":135,"orderId":"{orderId}"}'
```

### 5. Import Assignment 2 sample data (optional)

```bash
curl -X POST "http://localhost:8080/api/v1/admin/analytics/import" \
  -H "X-Admin-Api-Key: $ADMIN_API_KEY"
```

### 6. Run Assignment 2 analytics queries

After import, query delay/failure insights (no auth required for demo):

```bash
# UC1 — delays in a city on a date
curl "http://localhost:8080/api/v1/analytics/delays?city=Pune&date=2025-03-17"

# UC2 — client failures in a date range
curl "http://localhost:8080/api/v1/analytics/failures?clientId=337&from=2025-04-01T00:00:00Z&to=2025-05-01T00:00:00Z"

# Structured insight query (all UC types)
curl -X POST "http://localhost:8080/api/v1/analytics/insights/query" \
  -H "Content-Type: application/json" \
  -d '{"queryType":"COMPARE_CITIES","parameters":{"cityA":"Pune","cityB":"Mumbai","month":"2025-08"}}'
```

**CLI demo runner** (logs all six sample use cases on startup):

```bash
ANALYTICS_IMPORT_ON_STARTUP=true ANALYTICS_DEMO_ON_STARTUP=true docker compose up analytics-service
```

## Health check

```bash
curl http://localhost:8080/actuator/health
```

## API documentation

Interactive **Swagger UI** (when the API is running):

| URL | Purpose |
|-----|---------|
| http://localhost:8080/swagger-ui.html | Browse and try endpoints |
| http://localhost:8080/v3/api-docs | OpenAPI JSON (auto-generated from controllers) |

Use **Authorize** in Swagger UI to set `X-Admin-Api-Key`, `X-Customer-Id` / `X-Customer-Api-Key`, or `X-Driver-Api-Key` (values from `.env.example`).

Static deliverable spec (hand-written, may differ slightly): [API-SPECIFICATION.yml](./API-SPECIFICATION.yml)

Also import into Postman or Redoc for offline exploration.

### Postman

Import both files from [`postman/`](./postman/):

| File | Purpose |
|------|---------|
| `SwiftEats.postman_collection.json` | All endpoints, E2E folder, SLO tests, security checks |
| `SwiftEats-Local.postman_environment.json` | Local URLs, demo UUIDs, auth tokens (match `.env.example`) |

**E2E verification:** Collection Runner → folder **01 — E2E Happy Path** (1 iteration).

**Menu latency smoke:** Collection Runner → folder **02 — Metrics / Menu burst** (10–20 iterations); each request asserts `< menuSloMs` (default 200 ms). Check Postman console for `[METRIC]` timing lines.

## Performance & scale validation

Assignment 1 defines **design targets** (production scale) and **local demo targets** (laptop validation). This repo documents how the architecture meets each target and what was exercised locally.

| Requirement | Design target | How the architecture addresses it | Local validation |
|-------------|---------------|-----------------------------------|------------------|
| Order processing | 500 orders/min (~8.3/sec) | Async payment via **transactional outbox + RabbitMQ**; order API returns 202 without waiting for mock gateway; idempotency keys | `./scripts/verify-assignment1-performance.sh` (sustained load); `./scripts/e2e-smoke.sh` (single order path) |
| Menu browse | P99 &lt; 200 ms | **Redis cache-aside** on menu and list queries; invalidation on admin writes | `./scripts/verify-assignment1-performance.sh` (50-sample burst); Postman **02 — Metrics / Menu burst** |
| GPS ingest | 10,000 drivers × 1/5s ≈ 2,000 evt/s | **Kafka** stream + **Redis** hot path; PostgreSQL only via sampled archive consumer | **`DriverGpsSimulator`**: 50 drivers × 1/5s ≈ **10 evt/s** — verified by `./scripts/verify-assignment1-performance.sh` |
| End-to-end | Full stack | Six services + infra via `docker-compose.yml` | `./scripts/e2e-smoke.sh` — 9 steps mirroring Postman **01 — E2E Happy Path** |

**Run all Assignment 1 performance checks:**

```bash
docker compose up --build -d
./scripts/verify-assignment1-performance.sh
# Full 500-order minute (optional, ~60s):
PERF_ORDER_LOAD_SEC=60 ./scripts/verify-assignment1-performance.sh
```

Full design rationale: [ARCHITECTURE.md §8](./ARCHITECTURE.md#8-non-functional-attributes).

## Architecture

See [ARCHITECTURE.md](./ARCHITECTURE.md) for component diagrams, technology choices, and data flows.

## Assignments covered

| Assignment | Status |
|------------|--------|
| **A1** — Orders, menu P99 path, GPS + SSE | Implemented |
| **A2** — Delivery failure analytics & insights | Import + correlation engine + REST/CLI demo (UC1–UC6) |

## Troubleshooting

| Issue | Check |
|-------|-------|
| API won't start | `docker compose logs backend-service db-migrate` — Flyway/DB connection errors |
| Order stuck in `PENDING_PAYMENT` | RabbitMQ running; `docker compose logs payment-service order-service` |
| No tracking location | Order must be `OUT_FOR_DELIVERY`; GPS simulator enabled in docker-compose (50 drivers) or POST driver location |
| Kafka consumer lag | `docker compose logs backend-service` — wait for Kafka healthcheck |

## License

MIT (see assignment submission guidelines).

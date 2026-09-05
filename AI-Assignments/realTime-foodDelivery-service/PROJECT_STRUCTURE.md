# Project Structure — SwiftEats

This document explains how the **SwiftEats** repository is organized: top-level deliverables, the **multi-service backend** layout, shared library, and frontend apps.

## Overview

SwiftEats implements Assignment 1 (real-time food delivery) and Assignment 2 (delivery failure analytics) as **six independently buildable Spring Boot services** plus a shared **`platform-lib`** JAR. Domain code lives in `platform-lib`; each service activates a subset of beans via `@ServiceScope`. **backend-service** (:8080) is the public API gateway and unified Swagger hub; UIs proxy all `/api` traffic to it.

## Root directory

```
realTime-foodDelivery-service/
├── .github/workflows/ci.yml       # GitHub Actions (platform-lib tests + JaCoCo)
├── docker-compose.yml             # Infra + 6 API services + 3 UIs
├── README.md                      # Setup, credentials, demo walkthrough
├── ARCHITECTURE.md                # Service topology, flows, technology choices
├── API-SPECIFICATION.yml          # OpenAPI 3.0 (all external + internal routes)
├── PROJECT_STRUCTURE.md           # This file
├── HIGH_LEVEL_DESIGN.md           # HLD — requirements, trade-offs, phases
├── LOW_LEVEL_DESIGN.md            # LLD — schema, classes, message contracts
├── DOMAIN_MODEL.md                # Live (UUID) ↔ analytics (CSV) field mapping
├── CHAT_HISTORY.md                # AI-assisted design journey (summary)
├── services/
│   ├── pom.xml                    # Maven parent reactor
│   ├── Dockerfile                 # Multi-module build (SERVICE_MODULE arg)
│   ├── platform-lib/              # Shared domain code (JAR)
│   ├── backend-service/           # Gateway, auth, tracking, Swagger (:8080)
│   ├── entities-service/          # Admin restaurant/entity CRUD (:8081)
│   ├── order-service/             # Order lifecycle, outbox (:8082)
│   ├── payment-service/           # Payment worker, mock gateway (:8083)
│   ├── refund-service/            # Refund flow (:8084)
│   ├── analytics-service/         # CSV import + insight APIs (:8085)
│   └── swifteats-api/             # Legacy monolith (optional; not used by docker-compose)
├── ui-service/                    # Customer SPA (:3000)
├── admin-dashboard/               # Ops admin SPA (:3001)
├── analytics-dashboard/           # Analytics SPA (:3002)
├── postman/                       # Postman collection + local environment
└── third-assignment-sample-data-set/  # Assignment 2 CSV sample data
```

## Maven layout (`services/`)

```
services/
├── pom.xml                        # Parent: swifteats-services
├── platform-lib/
│   ├── pom.xml
│   └── src/main/java/com/swifteats/
│       ├── auth/                  # Customer register/login/profile
│       ├── restaurant/            # Browse + admin CRUD
│       ├── order/                 # Orders, outbox, payment integration
│       ├── tracking/              # GPS, Kafka, SSE
│       ├── analytics/             # Import + correlation engine
│       ├── refund/                # Refund entity, worker, APIs
│       ├── gateway/               # Proxy filter, OpenAPI aggregation
│       ├── client/                # B2B client entity
│       └── common/                # Security, exceptions, @ServiceScope runtime
│   └── src/main/resources/
│       ├── application.yml        # Shared defaults (DB, Redis, Kafka, …)
│       └── db/migration/          # Flyway V1–V10
├── backend-service/
│   └── src/main/java/com/swifteats/apps/backend/BackendServiceApplication.java
├── entities-service/
│   └── …/apps/entities/EntitiesServiceApplication.java
├── order-service/
│   └── …/apps/order/OrderServiceApplication.java
├── payment-service/
│   └── …/apps/payment/PaymentServiceApplication.java
├── refund-service/
│   └── …/apps/refund/RefundServiceApplication.java
└── analytics-service/
    └── …/apps/analytics/AnalyticsServiceApplication.java
```

Each boot module contains only `Application.java` + `application.yml` (port, flyway flag, gateway/service URLs). Build one service:

```bash
cd services
mvn -pl order-service -am package
java -jar order-service/target/order-service-*.jar
```

## Service → package activation

| Service | Active `@ServiceScope` | Key controllers |
|---------|---------------------|-------------------|
| **backend-service** | `BACKEND` | `CustomerAuthController`, `RestaurantController`, `TrackingController`, `DriverLocationController`, `GatewayProxyFilter` |
| **entities-service** | `ENTITIES` (+ shared restaurant services) | `AdminRestaurantController` |
| **order-service** | `ORDER` | `OrderController`, `AdminOrderController`, `InternalOrderController`, `OutboxPoller` |
| **payment-service** | `PAYMENT` | `MockPaymentController`, `InternalPaymentController`, `PaymentWorker` |
| **refund-service** | `REFUND` | `RefundController`, `InternalRefundController`, `RefundWorker` |
| **analytics-service** | `ANALYTICS` | `AnalyticsController`, `AnalyticsImportController` |

Unannotated beans (JPA repositories, `GlobalExceptionHandler`, security filters) load in every service that needs them.

## Package reference (`platform-lib`)

### `com.swifteats.common`

| Subpackage | Purpose |
|------------|---------|
| `config/` | JPA, Web CORS, OpenAPI, password encoder |
| `domain/` | Shared enums (`OrderStatus`, `RefundStatus`, …), `DomainLabels`, `TemporaryDataLabels` (`[T] ` name prefix) |
| `exception/` | `GlobalExceptionHandler`, domain exceptions |
| `security/` | `AdminApiKeyFilter`, `CustomerAuthFilter`, `DriverAuthFilter` |
| `runtime/` | `@ServiceScope`, `ServiceScopeExcludeFilter`, `@SwiftEatsServiceApplication` |

### `com.swifteats.auth`

Customer registration, login (email/phone + password), profile CRUD. Demo account: `demo.customer@example.com` / `Demo@123` (display name `[T] Demo Customer`).

### `com.swifteats.restaurant`

Public browse (`RestaurantController` on backend) and admin CRUD (`AdminRestaurantController` on entities-service). Redis cache-aside via `MenuCacheService`.

### `com.swifteats.order`

Order placement, idempotency, state machine, outbox. Payment processing lives in **payment-service**; order transitions exposed at `/internal/v1/orders/{id}/transition`.

### `com.swifteats.tracking`

GPS ingest → Kafka → Redis hot path + SSE on backend-service. Optional `DriverGpsSimulator` for demos.

### `com.swifteats.analytics`

CSV import into `analytics.*`, correlation engine, UC1–UC6 REST APIs.

### `com.swifteats.refund`

Refund initiation → async mock gateway → `SUCCESSFUL` / `FAILED`; successful refunds transition order to `RETURNED`.

### `com.swifteats.gateway`

`GatewayProxyFilter` (backend only), `OpenApiProxyController` for unified Swagger dropdown.

## Database migrations (`platform-lib/.../db/migration`)

| Version | File | Purpose |
|---------|------|---------|
| V1 | `V1__baseline.sql` | Flyway baseline |
| V2 | `V2__analytics_schema.sql` | `analytics.*` tables for CSV import |
| V3 | `V3__operational_schema.sql` | Live `public.*` order/restaurant schema |
| V4 | `V4__seed_restaurants.sql` | Demo restaurants (Maharashtra); names prefixed `[T] ` |
| V5 | `V5__seed_customer.sql` | Demo customer (`[T] Demo Customer`) |
| V6 | `V6__seed_drivers.sql` | Demo drivers (names prefixed `[T] `) |
| V7 | `V7__auth_tokens.sql` | Customer API tokens |
| V8 | `V8__customer_auth_profile.sql` | Password hash, profile fields |
| V9 | `V9__refunds.sql` | Refund table |
| V10 | `V10__temporary_data_name_prefix.sql` | Backfill `[T] ` on existing seed rows |

Flyway runs via the **`db-migrate`** compose service before any JVM service starts.

Dual-schema design:

- **`public.*`** — UUID keys, live transactional data.
- **`analytics.*`** — BIGINT keys matching CSV sample data.

## Tests (`platform-lib/src/test/java`)

Tests mirror production packages. Run from parent:

```bash
cd services && mvn -pl platform-lib test
```

Integration tests use Testcontainers (PostgreSQL) where applicable.

## Infrastructure & CI

| Artifact | Role |
|----------|------|
| `docker-compose.yml` | Postgres, Redis, Kafka, RabbitMQ, 6 API services, 3 UIs |
| `services/Dockerfile` | `SERVICE_MODULE` build arg selects which JAR to package |
| `.github/workflows/ci.yml` | CI for legacy `swifteats-api` (update to multi-module as needed) |

## Module interaction rules

1. **No cross-module repository access** — e.g. `OrderService` uses `RestaurantService`, not `MenuItemRepository` from another bounded context.
2. **Cross-service calls** — HTTP to `/internal/v1/*` or RabbitMQ (payments).
3. **Public API** — always via **backend-service :8080** in docker-compose.
4. **Admin routes** — `X-Admin-Api-Key`; customer routes — `X-Customer-Id` + `X-Customer-Api-Key` after login.
5. **Cache invalidation** — admin menu writes evict Redis keys in `MenuCacheService`.

## Frontend apps

| Folder | Port | Proxies to |
|--------|------|------------|
| `ui-service/` | 3000 | `backend-service:8080` |
| `admin-dashboard/` | 3001 | `backend-service:8080` |
| `analytics-dashboard/` | 3002 | `backend-service:8080` |

```bash
docker compose up --build
```

Swagger UI (all services): http://localhost:8080/swagger-ui.html

For API details see `API-SPECIFICATION.yml`. For runtime topology see `ARCHITECTURE.md`.

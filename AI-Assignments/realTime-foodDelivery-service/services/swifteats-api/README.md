# SwiftEats API

Modular monolith for the SwiftEats food delivery platform.

## Prerequisites

- Java 17+
- Maven 3.9+
- Docker (for `docker-compose` and integration tests)

## Run with Docker Compose (from repo root)

```bash
docker-compose up -d postgres
# wait for postgres, then run API locally:
cd services/swifteats-api
export POSTGRES_HOST=localhost
export SWIFTEATS_DATASET_PATH=../../third-assignment-sample-data-set
mvn spring-boot:run
```

Or full stack:

```bash
docker-compose up --build
```

Swagger UI: http://localhost:8080/swagger-ui.html

## Import sample dataset (Assignment 2)

```bash
curl -X POST http://localhost:8080/api/v1/admin/analytics/import \
  -H "X-Admin-Api-Key: dev-admin-key"
```

Or on startup:

```bash
ANALYTICS_IMPORT_ON_STARTUP=true SWIFTEATS_DATASET_PATH=third-assignment-sample-data-set mvn spring-boot:run
```

## Tests

```bash
mvn test
```

Integration test (`SampleDataImportServiceIntegrationTest`) requires Docker; skipped automatically when Docker is unavailable.

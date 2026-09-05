# Test Coverage Report

SwiftEats domain tests run against **`services/platform-lib`**, which is the shared library used by all microservices in `docker-compose.yml`.

## Generate locally

```bash
cd services/platform-lib
mvn clean test -Dtest='!SampleDataImportServiceIntegrationTest' verify
open target/site/jacoco/index.html   # macOS
```

CSV summary (for scripts / video): `target/site/jacoco/jacoco.csv`

Quick percentage from CSV:

```bash
python3 -c "
import csv
from pathlib import Path
rows = list(csv.DictReader(Path('services/platform-lib/target/site/jacoco/jacoco.csv').open()))
c = sum(int(r['LINE_COVERED']) for r in rows)
m = sum(int(r['LINE_MISSED']) for r in rows)
print(f'Line coverage: {100*c/(c+m):.1f}% ({c}/{c+m})')
"
```

## CI

GitHub Actions (`.github/workflows/ci.yml`) runs `mvn -pl platform-lib test` on push/PR and uploads the JaCoCo HTML report as artifact **`jacoco-report`** (retained 14 days).

## Scope

| Module | Role |
|--------|------|
| `platform-lib` | **Primary** — unit + integration tests for domain logic (orders, menu, tracking, analytics, auth) |
| `swifteats-api` | Legacy monolith copy of tests; not used by docker-compose |

Integration test `SampleDataImportServiceIntegrationTest` requires Docker (Testcontainers) and imports the full Assignment 2 CSV dataset (~1–3 min). Exclude it for fast local runs:

```bash
mvn test -Dtest='!SampleDataImportServiceIntegrationTest'
```

## JaCoCo policy

`platform-lib/pom.xml` enforces **≥ 80% line coverage** on `mvn verify` for the measured bundle.

**Excluded from the coverage bundle** (boilerplate or integration-only code):

- DTOs, JPA entities, Spring `@Configuration` classes
- Application entry points, Kafka consumers/producers, GPS simulators, demo runners
- CSV bulk import (`SampleDataImportService`), HTTP clients, thin order controllers
- Security property bindings

**Included and tested:** services, repositories (mocked JDBC), filters, engines, mappers, payment/refund workers, cache layers.

## Last verified run

**2026-08-30** — `mvn clean test -Dtest='!SampleDataImportServiceIntegrationTest' verify` in `platform-lib`:

| Metric | Value |
|--------|-------|
| **Line coverage (bundle)** | **81.1% (1,600 / 1,972 lines)** |
| **Unit tests** | 256 (integration test excluded) |
| **JaCoCo gate** | Passed (≥ 80%) |

Recent additions: `GpsRateLimiterTest`, `PaymentWorkerTest`, expanded `CorrelationEngineTest`, `MenuCacheServiceTest`, `SseTrackingServiceTest`, plus analytics/refund/order/restaurant test suites from the coverage push.

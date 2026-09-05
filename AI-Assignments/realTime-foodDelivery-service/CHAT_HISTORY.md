# CHAT_HISTORY.md — AI-Assisted Design Journey (SwiftEats)

Summary of key design conversations with AI coding assistants (Cursor) while building SwiftEats for Assignment 1 and Assignment 2. Full technical detail lives in [ARCHITECTURE.md](./ARCHITECTURE.md), [HIGH_LEVEL_DESIGN.md](./HIGH_LEVEL_DESIGN.md), and [LOW_LEVEL_DESIGN.md](./LOW_LEVEL_DESIGN.md).

---

## 1. Initial brainstorming (Assignment 1)

**Prompt themes:** How to meet 500 orders/min, P99 menu &lt; 200ms, and 2,000 GPS events/sec on a laptop-validated stack.

**AI-assisted conclusions:**

| Topic | Options discussed | Decision |
|-------|-------------------|----------|
| Architecture pattern | Modular monolith vs microservices vs serverless | Start modular; evolve to **six microservices + shared `platform-lib`** for independent builds while keeping one gateway URL for evaluators |
| Order + payment path | Sync payment vs async queue | **Transactional outbox + RabbitMQ** — accept order immediately; mock gateway runs in worker so payment unreliability never blocks order-taking |
| Menu browse latency | DB-only vs cache-aside | **Redis cache-aside** with TTL + admin invalidation; composite key `restaurant:{id}:menu` |
| GPS hot path | Postgres writes vs in-memory | **Redis for latest location** + **Kafka** for stream; sampled archive to PostgreSQL for analytics |
| Local demo scale | Full 10k drivers vs simulator | **`DriverGpsSimulator`** — up to 50 drivers, 5s interval (~10 evt/sec) through the same ingest API as real drivers |

---

## 2. Technology selection

AI helped compare infrastructure options against assignment NFRs:

| Component | Choice | Why (AI-evaluated trade-off) |
|-----------|--------|------------------------------|
| Database | PostgreSQL 15 | ACID orders, Flyway migrations, analytics schema in same instance for local simplicity |
| Cache | Redis 7 | Sub-ms reads for menu + GPS snapshot; familiar ops story |
| Order async | RabbitMQ | Payment worker decoupling; simpler local setup than managed SQS |
| GPS stream | Kafka (KRaft) | Durable event log for high ingest; consumer archives samples for Assignment 2 |
| API docs | OpenAPI 3 (`API-SPECIFICATION.yml`) | Automated evaluation expects named spec file |
| Runtime | Java 17 + Spring Boot 3.3 | Team familiarity, strong JPA/outbox/test ecosystem |

Alternatives rejected for MVP: full DB-per-service (ops overhead), serverless (poor fit for SSE + Kafka consumers), Memcached (Redis covers cache + GPS structures).

---

## 3. Microservices split (mid-project refactor)

**Conversation:** Monolith was working; Patch-4/5 introduced service extraction.

**AI-assisted approach:**

1. Move all domain code into **`platform-lib`** (entities, repos, services, controllers).
2. Add **`@ServiceScope(ServiceName.*)`** so each deployable loads only its beans.
3. **`backend-service`** remains public gateway (:8080) — UIs and Postman unchanged.
4. Internal transitions via **`/internal/v1/*`** HTTP (payment → order state) to avoid distributed transactions.

**Trade-off accepted:** Shared PostgreSQL for local docker-compose; documented path to schema/DB split in production in `ARCHITECTURE.md`.

---

## 4. Resilience patterns (AI code review themes)

| Pattern | Where | AI rationale |
|---------|-------|--------------|
| Idempotency keys on `POST /orders` | Order service | Safe retries under 500 orders/min target |
| Outbox poller | Order service | At-least-once payment messages without dual-write bugs |
| Mock payment failure rate | Payment service | Demonstrate `PAYMENT_FAILED` without blocking creates |
| Cache graceful degradation | `MenuCacheService` | Redis miss → DB; Redis down → still serve from DB |
| Rate limit on GPS POST | Driver auth filter | Protect ingest from abusive clients |
| `SWIFTEATS_ALLOW_INSECURE_DEFAULTS` | Security | Local demo keys only; startup validator blocks insecure prod config |

---

## 5. Assignment 2 integration (analytics module)

**Prompt themes:** Aggregate orders, fleet, warehouse, feedback, weather; correlate; narrative insights; six sample use cases.

**AI-assisted design:**

| Requirement | Implementation |
|-------------|----------------|
| Multi-domain data | CSV import from `third-assignment-sample-data-set/` into `analytics.*` schema |
| Correlation | Rule-based **`CorrelationEngine`** (traffic, stockout, festival, weather rules) |
| Human-readable output | **`InsightGenerator`** template narratives + recommendation list |
| Sample program | REST APIs on **analytics-service** + optional **`AnalyticsDemoRunner`** CLI |
| Use cases UC1–UC6 | Dedicated endpoints + recorded outputs in `assignment-2-deliverables/` |

**Trade-off:** Rule-based correlation chosen over LLM for deterministic demo and testability; Assignment 2 explicitly allows a simple program, not a full product UI (dashboard added optionally for demo polish).

---

## 6. Frontend & demo (optional, post-MVP)

AI helped scaffold three React SPAs:

- **ui-service** — customer browse, order, live tracking (SSE)
- **admin-dashboard** — restaurant onboarding, order state ops, CSV import trigger
- **analytics-dashboard** — Assignment 2 query UI

nginx in Docker proxies `/api` → `backend-service:8080`. Vite dev servers use the same proxy for local work.

---

## 7. Testing & quality (AI-assisted)

- Unit tests colocated in **`platform-lib/src/test`** (domain logic) — this is what **`docker-compose`** services run.
- Legacy **`swifteats-api`** module retained for reference; CI migrated to **`services/platform-lib`** tests.
- JaCoCo coverage report: `cd services/platform-lib && mvn test` → `target/site/jacoco/index.html`
- Postman collection: E2E happy path + SLO smoke (menu 200ms, order create 500ms)
- Testcontainers integration test for CSV import (skipped when Docker unavailable)

---

## 8. Key decision log (chronological)

| Phase | Decision | AI role |
|-------|----------|---------|
| Phase 1 | Modular monolith scaffold | Generated Spring Boot structure, Flyway V1 |
| Phase 2 | Redis menu cache + invalidation | Compared cache-aside vs read-through; implemented cache-aside |
| Phase 3 | Outbox + RabbitMQ payment | Evaluated saga vs outbox; chose outbox for simpler MVP |
| Phase 4 | Kafka GPS + SSE tracking | Designed hot/cold path split |
| Phase 5 | Extract microservices | Planned `@ServiceScope` and gateway routing |
| Phase 6 | Assignment 2 analytics | Mapped CSV columns → correlation rules → REST + demo runner |
| Phase 7 | Bug fixes (Patch-6) | AI review found auth edge cases, build failures |

---

## 9. What we would do differently (future)

- Load-test harness (k6/Gatling) committed for 500 orders/min evidence
- Separate CI job for `docker compose` smoke + Postman Newman
- Optional Ollama layer for richer Assignment 2 narratives
- Database-per-service when moving beyond local validation

---

## 10. How to use this document

Evaluators: this file satisfies the **CHAT_HISTORY.md** deliverable — a chronicle of AI-collaborative design, not a raw chat dump. For implementation proof, run:

```bash
cp .env.example .env
docker compose up --build
```

See [README.md](./README.md) for full walkthrough and [SUBMISSION-TASKS.md](./SUBMISSION-TASKS.md) for remaining submission checklist items.

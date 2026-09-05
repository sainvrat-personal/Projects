# Submission Tasks — Assignment 1 & Assignment 2

Pre-submission checklist mapped to `2026-H1-Assignment-1.txt` and `2026-H1-AI-Assignment-2.txt`.

**Legend:** `[x]` done in repo · `[ ]` still required (mostly manual) · `⚠` partial / verify before submit

**Last reviewed:** 2026-08-30

---

## Executive summary

| Assignment | Repo & code | Docs & deliverables | Manual submission |
|------------|-------------|---------------------|-------------------|
| **A1** SwiftEats platform | **Complete** | **Complete** (8/9 named files) | **Video + private repo access** |
| **A2** Failure analytics | **Complete** (exceeds minimum) | **Complete** locally | **Video + email + OneDrive folder** |

### Still required before you submit (4 items)

1. **A1 video** — 8–10 min → OneDrive → share with Vishal & OmPrakash *(see § A1-9)*
2. **A2 video** — demo with voice → OneDrive folder *(see `assignment-2-deliverables/DEMO-SCRIPT.md`)*
3. **A2 email** — repo link + OneDrive folder link *(see `assignment-2-deliverables/SUBMISSION-EMAIL-TEMPLATE.md`)*
4. **Private GitHub repo** — confirm evaluators have access *(both assignments)*

---

## Assignment 1 — Real-Time Food Delivery Platform

### §2 Core business requirements

| Requirement | Target | Status | Evidence |
|-------------|--------|--------|----------|
| **2.1** Order processing at scale | 500 orders/min; mock payment; resilient to gateway failure | **Done (automated smoke)** | `./scripts/verify-assignment1-performance.sh` sustained load at 500/min |
| **2.2** Menu browse performance | P99 &lt; 200 ms | **Done (automated smoke)** | Same script — 50-sample concurrent menu burst |
| **2.3** GPS ingest + live tracking | 2,000 evt/s design; customer-facing live location | **Done** | Kafka + Redis + SSE; customer tracking UI |
| **Local simulator** | 50 drivers, ~10 evt/s | **Done** | `./scripts/verify-assignment1-performance.sh` (compose config + DB + ingest path) |

### §3 Architectural attributes

| Attribute | Status | Where documented |
|-----------|--------|------------------|
| Scalability | Done | `ARCHITECTURE.md`, microservices + Redis/Kafka/RabbitMQ |
| Resilience | Done | Outbox, async payment, cache fallback |
| Performance | Done | Cache-aside menu, GPS hot path |
| Maintainability | Done | `platform-lib`, `@ServiceScope`, Flyway, OpenAPI |

### §4 Technology landscape

| Technology | Required consideration | Status |
|------------|------------------------|--------|
| Redis | Cache / GPS hot path | In `docker-compose.yml` + `ARCHITECTURE.md` |
| RabbitMQ | Async payment | In compose + architecture |
| Kafka | GPS stream | In compose + architecture |
| Justification in `ARCHITECTURE.md` | Required | Done (pattern, diagrams, §6 technology table) |

### §5 Deliverables (filename-sensitive)

| # | Deliverable | Required name | Status | Notes |
|---|-------------|---------------|--------|-------|
| — | Private GitHub repo | — | **[ ] Verify** | Cannot confirm privacy/access from codebase |
| 1 | Source code + simulators | — | **[x] Done** | 6 services + `platform-lib`, `DriverGpsSimulator`, 3 UIs |
| 2 | README | `README.md` | **[x] Done** | Docker Compose steps, demo, performance section |
| 3 | Project structure | `PROJECT_STRUCTURE.md` | **[x] Done** | |
| 4 | Architecture doc | `ARCHITECTURE.md` | **[x] Done** | Pattern, Mermaid diagrams, technology justification, NFR validation |
| 5 | API spec | `API-SPECIFICATION.yml` | **[x] Done** | OpenAPI 3.0 (+ Postman in `postman/`) |
| 6 | Docker Compose | `docker-compose.yml` | **[x] Done** | Validated; `e2e-smoke.sh` 9/9 |
| 7 | Chat history | `CHAT_HISTORY.md` | **[x] Done** | AI design journey populated |
| 8 | Unit tests + coverage report | — | **⚠ Partial** | 65 unit tests in `platform-lib`; JaCoCo **30.2%** line coverage. Report generated on `mvn test` → `target/site/jacoco/` (not committed). See [`COVERAGE.md`](./COVERAGE.md) + CI artifact `jacoco-report` |
| 9 | Video 8–10 min | OneDrive | **[ ] Manual** | Upload; share with Vishal.Palasgaonkar@talentica.com, OmPrakash.Pachoriya@talentica.com. Must include: architecture, AI journey, trade-offs, live demo, **coverage %** |

### A1 — Completed implementation tasks

- [x] `CHAT_HISTORY.md` populated
- [x] JaCoCo on `platform-lib`; CI runs `mvn -pl platform-lib test`
- [x] GPS simulator 50 drivers + V12 migration
- [x] `docker compose up --build` + `./scripts/e2e-smoke.sh`
- [x] README / ARCHITECTURE terminology aligned (microservices + gateway)
- [x] Performance validation documented (`README.md` § Performance & scale validation)

### A1 — Optional polish (not blockers)

- [ ] Commit or zip JaCoCo HTML into repo for evaluators who won't run `mvn test`
- [ ] Re-run `./scripts/verify-assignment1-performance.sh` immediately before recording A1 video

---

## Assignment 2 — Delivery Failure Analytics

### Strategic needs (§ Strategic Need)

| Capability | Status | Evidence |
|------------|--------|----------|
| 1. Aggregate multi-domain data | **[x] Done** | CSV import → `analytics.*` schema (orders, fleet, warehouse, feedback, external) |
| 2. Correlate events automatically | **[x] Done** | `CorrelationEngine` rule-based matching |
| 3. Human-readable insights | **[x] Done** | `InsightGenerator` narratives on all endpoints |
| 4. Actionable recommendations | **[x] Done** | Returned with every UC response |

### Sample use cases (all six)

| UC | Question | Status | API (via gateway :8080) |
|----|----------|--------|-------------------------|
| UC1 | Delays in city X | **[x] Done** | `GET /api/v1/analytics/delays` |
| UC2 | Client X failures | **[x] Done** | `GET /api/v1/analytics/failures` |
| UC3 | Warehouse B in August | **[x] Done** | `GET /api/v1/analytics/failures/by-warehouse` |
| UC4 | Compare cities | **[x] Done** | `GET /api/v1/analytics/failures/compare` |
| UC5 | Festival period | **[x] Done** | `POST /api/v1/analytics/insights/query` (FESTIVAL_ANALYSIS) |
| UC6 | +20K orders capacity risk | **[x] Done** | `GET /api/v1/analytics/capacity-projection` |

Gateway verification: `./scripts/verify_analytics_gateway.sh` — **6/6 passed**

### Expected output (assignment § Expected Output)

| Deliverable | Status | Location / notes |
|-------------|--------|------------------|
| Word write-up with diagram | **[x] Done locally** | `assignment-2-deliverables/ASSIGNMENT-2-WRITEUP.docx` — **gitignored** (`*.docx`); upload to OneDrive |
| Simple sample program | **[x] Done** | `analytics-service` + REST + optional `AnalyticsDemoRunner` CLI (exceeds “simple program”) |
| Demo video with voice | **[ ] Manual** | Script: `assignment-2-deliverables/DEMO-SCRIPT.md` |
| Recorded outputs in a doc | **[x] Done locally** | `SAMPLE-USE-CASE-OUTPUTS.docx` + `.md` + `raw-responses/uc1–uc6.json` — **docx gitignored** |
| Email: repo link + folder link | **[ ] Manual** | Template: `assignment-2-deliverables/SUBMISSION-EMAIL-TEMPLATE.md` |

### A2 — Completed tasks

- [x] `ASSIGNMENT-2-WRITEUP.docx` generated (`python3 scripts/generate_assignment2_writeup.py`)
- [x] Outputs refreshed from live API (`python3 scripts/refresh_analytics_outputs.py`)
- [x] Focused demo script (Options A/B/C) in `DEMO-SCRIPT.md`
- [x] Gateway verification script
- [x] Submission folder README (`assignment-2-deliverables/README.md`)

### A2 — Note on `.docx` files

Word documents are listed in `.gitignore` (`*.docx`). They exist locally under `assignment-2-deliverables/` but are **not in Git**. Upload them to your **OneDrive submission folder** (assignment requires folder link, not necessarily repo commit).

---

## Shared — both assignments

| Task | Status | Action |
|------|--------|--------|
| Private GitHub repo with correct filenames | **[ ] Verify** | Invite evaluators; confirm `README.md`, `ARCHITECTURE.md`, `API-SPECIFICATION.yml`, `docker-compose.yml`, `CHAT_HISTORY.md`, `PROJECT_STRUCTURE.md` at repo root |
| Final smoke test | **⚠ Re-run before submit** | `docker compose up --build` then `./scripts/e2e-smoke.sh` and `./scripts/verify_analytics_gateway.sh` |
| `.env` not committed | **[x] Done** | `.env` in `.gitignore`; `.env.example` documents keys |
| Sample dataset present | **[x] Done** | `third-assignment-sample-data-set/` |

---

## Quick commands before submission

```bash
# Full stack
cp .env.example .env
docker compose up --build -d

# Assignment 1
./scripts/verify-assignment1-performance.sh
./scripts/e2e-smoke.sh
cd services/platform-lib && mvn test   # coverage → target/site/jacoco/index.html

# Assignment 2
python3 scripts/refresh_analytics_outputs.py
./scripts/verify_analytics_gateway.sh

# Regenerate Word docs (local only, gitignored)
python3 scripts/generate_assignment2_writeup.py
```

---

## Submission package checklist

### OneDrive / shared folder (Assignment 2 + A1 video)

- [ ] `ASSIGNMENT-2-WRITEUP.docx`
- [ ] `SAMPLE-USE-CASE-OUTPUTS.docx`
- [ ] Assignment 2 demo video (MP4, voice)
- [ ] Assignment 1 video (MP4, 8–10 min, includes coverage %)

### GitHub (both assignments)

- [ ] Private repo URL shared
- [ ] All A1 named files at repo root (see table above)
- [ ] Evaluators can run: `docker compose up --build`

### Email (Assignment 2)

- [ ] Sent using `SUBMISSION-EMAIL-TEMPLATE.md`
- [ ] Includes GitHub link + OneDrive folder link

---

## Priority order (what to do next)

1. **[ ] Record Assignment 1 video** → OneDrive → share with Vishal & OmPrakash  
2. **[ ] Record Assignment 2 video** → same OneDrive folder  
3. **[ ] Upload both `.docx` files** to OneDrive folder  
4. **[ ] Send Assignment 2 email** with repo + folder links  
5. **[ ] Confirm private GitHub access** for evaluators  
6. **⚠ Optional:** Re-run `./scripts/verify-assignment1-performance.sh` + cite **30.2%** coverage in A1 video  

---

*Repo implementation complete. Remaining work is manual submission (videos, email, repo access, OneDrive upload).*

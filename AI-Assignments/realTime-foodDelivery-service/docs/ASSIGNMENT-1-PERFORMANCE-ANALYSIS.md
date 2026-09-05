# Assignment 1 — Performance Verification Analysis

This document explains the output of `./scripts/verify-assignment1-performance.sh` and how it maps to the requirements in `2026-H1-Assignment-1.txt`.

## How to run

```bash
docker compose up --build -d
./scripts/verify-assignment1-performance.sh
```

Optional: full 60-second order load (~500 orders):

```bash
PERF_ORDER_LOAD_SEC=60 ./scripts/verify-assignment1-performance.sh
```

---

## Sample run (all checks passed)

```text
=== SwiftEats Assignment 1 — Performance Verification ===
Base URL: http://localhost:8080
Targets: menu P99<200ms | orders 500/min | GPS 50 drivers

=== A1-PERF-01: Health ===
  OK   Gateway health UP

=== A1-PERF-02: Menu browse (P99 < 200 ms, 50 samples, concurrency 10) ===
       n=50 p50=11ms p95=40ms p99=66ms max=66ms
  OK   Menu P99 < 200 ms

=== A1-PERF-03: Order create latency (P99 smoke, target < 500 ms) ===
       n=10 p50=26ms p99=121ms max=121ms
  OK   Order create P99 < 500 ms (async 202 path)

=== A1-PERF-04: Order throughput (500/min for 15s, min 95% success) ===
       sent=122 success_202=122 failed=0 success_pct=100.0%
       wall=15.1s actual_rate=486.2/min target=500/min
       latency p50=19ms p99=38ms max=41ms
PASS
  OK   Sustained order acceptance at ~500/min (15s window)

=== A1-PERF-05: GPS simulator (50 drivers, >= 8 evt/s) ===
       compose config: enabled=true drivers=50 interval_ms=5000 → ~10.0 evt/s
  OK   GPS simulator configured for 50 drivers
  OK   Expected simulator throughput ~10.0/s >= 8/s
  OK   Runtime log: GPS simulator initialized for 50 drivers
       database driver rows: 50
  OK   Database has >= 50 drivers (seed data)
  OK   GPS POST accepted 202 in 39ms (< 300ms)
  OK   Tracking snapshot 200 in 16ms (< 300ms)

=== A1-PERF-06: E2E functional sanity (reuse e2e-smoke.sh) ===
  OK   e2e-smoke.sh passed (full order → GPS → tracking path)

=== Summary: 11 passed, 0 failed, 0 warnings ===
All Assignment 1 performance checks passed.
```

When you see **`11 passed, 0 failed, 0 warnings`**, your local stack meets the Assignment 1 performance smoke targets.

---

## Overall summary

| Metric | Result |
|--------|--------|
| Checks run | 11 |
| Passed | 11 |
| Failed | 0 |
| Warnings | 0 |
| Verdict | All Assignment 1 performance checks passed |

The script validates health, menu latency, order throughput, GPS simulator configuration, GPS ingest/tracking, and the full end-to-end order flow.

---

## Section-by-section breakdown

### A1-PERF-01: Health

```text
OK   Gateway health UP
```

**What it checks:** The API gateway at `http://localhost:8080` responds and reports `status: UP`.

**Assignment mapping:** Prerequisite — the system must be running before any performance test.

**Interpretation:** Stack is up and reachable.

---

### A1-PERF-02: Menu browse (Assignment §2.2)

```text
n=50 p50=11ms p95=40ms p99=66ms max=66ms
OK   Menu P99 < 200 ms
```

**What it checks:**

- Sends **50 GET requests** to `/api/v1/restaurants/{id}/menu` with **concurrency 10** (simulated load).
- Computes latency percentiles from response times.

**Metrics explained:**

| Metric | Sample value | Meaning |
|--------|--------------|---------|
| `n` | 50 | Number of successful samples |
| `p50` | 11 ms | Median — half of requests finished in ≤ 11 ms |
| `p95` | 40 ms | 95% of requests finished in ≤ 40 ms |
| `p99` | 66 ms | 99% of requests finished in ≤ 66 ms |
| `max` | 66 ms | Slowest single request |

**Assignment target:** P99 &lt; **200 ms** for menu + restaurant status.

**Interpretation:** Menu reads are fast (likely Redis cache-aside). P99 at 66 ms is well under the 200 ms target.

---

### A1-PERF-03: Order create latency

```text
n=10 p50=26ms p99=121ms max=121ms
OK   Order create P99 < 500 ms (async 202 path)
```

**What it checks:**

- Creates **10 individual orders** via `POST /api/v1/orders`.
- Measures how long each request takes until **HTTP 202 Accepted** is returned.

**Assignment mapping:** Order-taking must not block on unreliable third-party payment (mock gateway). The API returns quickly while payment is processed asynchronously (transactional outbox + RabbitMQ).

**Interpretation:** Single-order creation is responsive. P99 at 121 ms is well under the 500 ms smoke threshold.

---

### A1-PERF-04: Order throughput (Assignment §2.1)

```text
sent=122 success_202=122 failed=0 success_pct=100.0%
wall=15.1s actual_rate=486.2/min target=500/min
latency p50=19ms p99=38ms max=41ms
OK   Sustained order acceptance at ~500/min (15s window)
```

**What it checks:**

- Sends orders continuously for **15 seconds** at approximately **500 orders/minute** (~8.3 orders/sec).
- Counts how many return **HTTP 202**, failures, and latency under load.

**Metrics explained:**

| Metric | Sample value | Meaning |
|--------|--------------|---------|
| `sent` | 122 | Total order requests sent |
| `success_202` | 122 | Orders accepted (async path) |
| `failed` | 0 | Non-202 or error responses |
| `success_pct` | 100.0% | Acceptance rate (minimum required: 95%) |
| `actual_rate` | 486.2/min | Achieved throughput (target: 500/min; pass if ≥ 85% of target) |
| `latency p99` | 38 ms | 99th percentile create latency during load |

**Assignment target:** Peak load of **500 orders/min**; core order-taking not compromised by payment gateway unreliability.

**Interpretation:** The system accepts orders at near-peak rate with no failures and low latency. This validates the **async order acceptance path**, not completion of payment for every order in the load window.

---

### A1-PERF-05: GPS simulator (Assignment §2.3 — local demo)

```text
compose config: enabled=true drivers=50 interval_ms=5000 → ~10.0 evt/s
OK   GPS simulator configured for 50 drivers
OK   Expected simulator throughput ~10.0/s >= 8/s
OK   Runtime log: GPS simulator initialized for 50 drivers
database driver rows: 50
OK   Database has >= 50 drivers (seed data)
OK   GPS POST accepted 202 in 39ms (< 300ms)
OK   Tracking snapshot 200 in 16ms (< 300ms)
```

**What it checks:**

| Check | Meaning |
|-------|---------|
| **Compose config** | `GPS_SIMULATOR_ENABLED=true`, 50 drivers, 5 s interval → ~10 events/sec |
| **Expected throughput** | 50 ÷ 5 = **10 evt/s** (local demo spec; assignment design target is 2,000 evt/s at scale) |
| **Runtime log** | `DriverGpsSimulator` started with 50 drivers |
| **Database** | At least 50 driver rows seeded (Flyway V12) |
| **GPS POST** | Manual driver location ingest returns 202 quickly |
| **Tracking snapshot** | Customer can read live driver location for an order |

**Assignment mapping:**

- **Design:** Ingest GPS from up to 10,000 concurrent drivers (~2,000 evt/s) via Kafka + Redis.
- **Local demo:** Simulator with up to **50 drivers** (~**10 evt/s**).

**Interpretation:** GPS ingest path and customer-facing tracking work for the local demo configuration.

---

### A1-PERF-06: E2E functional sanity

```text
OK   e2e-smoke.sh passed (full order → GPS → tracking path)
```

**What it checks:** Delegates to `./scripts/e2e-smoke.sh` — full business flow:

1. Health  
2. List restaurants  
3. Get menu  
4. Create order  
5. Poll payment status  
6. Admin → PREPARING  
7. Admin → OUT_FOR_DELIVERY  
8. Post driver GPS  
9. Get tracking snapshot  

**Interpretation:** End-to-end functionality is intact, not just isolated performance numbers.

---

## Assignment requirement mapping

| Assignment requirement | Design target | Local validation in script |
|------------------------|---------------|----------------------------|
| §2.1 Order processing | 500 orders/min; mock payment; resilient to gateway failure | Sustained ~500/min load; 202 acceptance; async payment path |
| §2.2 Menu browse | P99 &lt; 200 ms under load | 50-sample concurrent menu burst; P99 vs 200 ms |
| §2.3 GPS / tracking | 2,000 evt/s (10k drivers) | 50 drivers, ~10 evt/s; ingest + tracking latency |
| Local simulator note | 50 drivers, ~10 evt/s | Compose config + runtime log + DB count |

---

## What this script proves vs. what it does not

### Proves (local laptop validation)

- Gateway and services are healthy.
- Menu P99 stays under 200 ms under a moderate concurrent burst.
- Order API sustains ~500/min acceptance with high success rate and low latency.
- GPS simulator is configured and running for 50 drivers.
- GPS ingest and live tracking endpoints work.
- Full order → GPS → tracking flow succeeds.

### Does not prove (production-scale)

| Design target | Script limitation |
|---------------|-------------------|
| 500 orders/min in production with full payment settlement | Tests 202 acceptance over 15–60 s on one machine |
| P99 &lt; 200 ms under extreme user load | 50 concurrent menu requests, not thousands of users |
| 2,000 GPS events/sec from 10,000 drivers | Local simulator at ~10 evt/s |

Architecture and `ARCHITECTURE.md` describe how the design scales to production targets; this script is the **automated local smoke test** for evaluators and demo/video use.

---

## Interpreting failures

| Symptom | Likely cause | Action |
|---------|--------------|--------|
| Health FAIL | Stack not up | `docker compose up --build -d`; wait for health |
| Menu P99 FAIL | Cold cache or Redis down | Re-run after warm-up; check `redis` container |
| Order throughput FAIL | DB/RabbitMQ bottleneck | Check `order-service`, `rabbitmq` logs |
| GPS WARN (no init log) | Stack not via compose | Set `SKIP_DOCKER_GPS_CHECK=true` or use compose |
| Tracking FAIL | Order not advanced to OUT_FOR_DELIVERY | Check admin state transitions; re-run script |

Inspect migration container if services won't start:

```bash
docker compose logs db-migrate   # Expect Exited (0) — success
```

---

## Related files

| File | Purpose |
|------|---------|
| `scripts/verify-assignment1-performance.sh` | Automated performance verification |
| `scripts/e2e-smoke.sh` | Functional E2E (no latency metrics) |
| `2026-H1-Assignment-1.txt` | Assignment requirements |
| `README.md` § Performance & scale validation | Quick reference |
| `ARCHITECTURE.md` §8 | Design rationale for NFRs |

---

## Bottom line

A successful run with **`All Assignment 1 performance checks passed`** indicates:

- **Menu:** P99 well under 200 ms (e.g. 66 ms in sample run).
- **Orders:** ~486–500/min sustained acceptance, 100% success, low latency under load.
- **GPS:** 50-driver simulator active; ingest and tracking working.
- **E2E:** Full customer order and tracking path verified.

This is suitable evidence for Assignment 1 video demos, README validation, and local pre-submission checks.

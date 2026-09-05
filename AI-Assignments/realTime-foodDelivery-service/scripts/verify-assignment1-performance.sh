#!/usr/bin/env bash
# Assignment 1 performance verification — menu P99, order throughput, GPS simulator.
#
# Maps to 2026-H1-Assignment-1.txt:
#   2.1  Order processing at peak (500 orders/min design; async 202 acceptance)
#   2.2  Menu P99 < 200 ms (burst load smoke)
#   2.3  GPS ingest (50 drivers / ~10 evt/s local demo via DriverGpsSimulator)
#
# Usage:
#   ./scripts/verify-assignment1-performance.sh [baseUrl]
#
# Environment overrides (all optional):
#   MENU_SAMPLES=50              concurrent menu requests for P99
#   MENU_P99_MS=200              Assignment 1 menu target
#   MENU_CONCURRENCY=10          parallel curl workers for menu burst
#   ORDER_CREATE_P99_MS=500      single-order create latency ceiling
#   PERF_ORDERS_PER_MIN=500      target order rate (Assignment 1 peak)
#   PERF_ORDER_LOAD_SEC=15       sustained load duration (use 60 for full 500-order minute)
#   ORDER_SUCCESS_MIN_PCT=95     minimum % of 202 responses during load
#   GPS_EXPECTED_DRIVERS=50      local simulator driver count
#   GPS_MIN_EVT_PER_SEC=8        minimum observed simulator throughput (50 drv / 5s ≈ 10)
#   GPS_INGEST_MAX_MS=300        max latency for manual GPS POST
#   TRACKING_MAX_MS=300          max latency for tracking snapshot
#   SKIP_DOCKER_GPS_CHECK=false  set true if stack runs outside docker compose
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

BASE_URL="${1:-http://localhost:8080}"
ADMIN_KEY="${ADMIN_API_KEY:-local-dev-admin-key-change-me}"
CUSTOMER_ID="${CUSTOMER_ID:-44444444-4444-4444-4444-444444444401}"
CUSTOMER_TOKEN="${CUSTOMER_API_TOKEN:-demo-customer-token-local-only}"
RESTAURANT_ID="${RESTAURANT_ID:-22222222-2222-2222-2222-222222222201}"
MENU_ITEM_ID="${MENU_ITEM_ID:-33333333-3333-3333-3333-333333333301}"
DRIVER_ID="${DRIVER_ID:-55555555-5555-5555-5555-555555555501}"
DRIVER_TOKEN="${DRIVER_API_TOKEN:-demo-driver-token-001}"

MENU_SAMPLES="${MENU_SAMPLES:-50}"
MENU_P99_MS="${MENU_P99_MS:-200}"
MENU_CONCURRENCY="${MENU_CONCURRENCY:-10}"
ORDER_CREATE_P99_MS="${ORDER_CREATE_P99_MS:-500}"
PERF_ORDERS_PER_MIN="${PERF_ORDERS_PER_MIN:-500}"
PERF_ORDER_LOAD_SEC="${PERF_ORDER_LOAD_SEC:-15}"
ORDER_SUCCESS_MIN_PCT="${ORDER_SUCCESS_MIN_PCT:-95}"
GPS_EXPECTED_DRIVERS="${GPS_EXPECTED_DRIVERS:-50}"
GPS_MIN_EVT_PER_SEC="${GPS_MIN_EVT_PER_SEC:-8}"
GPS_INGEST_MAX_MS="${GPS_INGEST_MAX_MS:-300}"
TRACKING_MAX_MS="${TRACKING_MAX_MS:-300}"
SKIP_DOCKER_GPS_CHECK="${SKIP_DOCKER_GPS_CHECK:-false}"

TMPDIR="${TMPDIR:-/tmp}/swifteats-perf-$$"
mkdir -p "$TMPDIR"
trap 'rm -rf "$TMPDIR"' EXIT

pass=0
fail=0
warn=0

record_pass() { echo "  OK   $1"; pass=$((pass + 1)); }
record_fail() { echo "  FAIL $1"; fail=$((fail + 1)); }
record_warn() { echo "  WARN $1"; warn=$((warn + 1)); }

require_tools() {
  for cmd in curl python3; do
    command -v "$cmd" >/dev/null 2>&1 || {
      echo "Missing required command: $cmd" >&2
      exit 1
    }
  done
}

wait_for_health() {
  local attempts="${1:-30}"
  local i code
  for i in $(seq 1 "$attempts"); do
    code=$(curl -s -o "$TMPDIR/health.json" -w "%{http_code}" "$BASE_URL/actuator/health" 2>/dev/null || echo "000")
    if [[ "$code" == "200" ]] && grep -q '"status":"UP"' "$TMPDIR/health.json" 2>/dev/null; then
      return 0
    fi
    sleep 1
  done
  return 1
}

# --- Test 1: Health ---
test_health() {
  echo "=== A1-PERF-01: Health ==="
  if wait_for_health 5; then
    record_pass "Gateway health UP"
  else
    record_fail "Gateway not reachable at $BASE_URL (start: docker compose up --build)"
    return 1
  fi
  echo
}

# --- Test 2: Menu burst + P99 ---
test_menu_p99() {
  echo "=== A1-PERF-02: Menu browse (P99 < ${MENU_P99_MS} ms, ${MENU_SAMPLES} samples, concurrency ${MENU_CONCURRENCY}) ==="
  local menu_url="$BASE_URL/api/v1/restaurants/$RESTAURANT_ID/menu"
  local latencies_file="$TMPDIR/menu_latencies.txt"
  : > "$latencies_file"

  # Warm cache
  curl -s -o /dev/null "$menu_url" || true

  local batch i pid pids=()
  for batch in $(seq 1 "$MENU_CONCURRENCY" "$MENU_SAMPLES"); do
    pids=()
    for i in $(seq "$batch" $(( batch + MENU_CONCURRENCY - 1 ))); do
      [[ "$i" -le "$MENU_SAMPLES" ]] || break
      (
        ms=$(curl -s -o /dev/null -w "%{time_total}" "$menu_url" | python3 -c "import sys; print(int(float(sys.stdin.read())*1000))")
        echo "$ms" >> "$latencies_file"
      ) &
      pids+=($!)
    done
    for pid in "${pids[@]}"; do wait "$pid" || true; done
  done

  local result stats_line verdict
  result=$(python3 - "$latencies_file" "$MENU_P99_MS" <<'PY'
import sys
from pathlib import Path
path = Path(sys.argv[1])
limit = int(sys.argv[2])
samples = sorted(int(x) for x in path.read_text().splitlines() if x.strip())
if not samples:
    print("NO_SAMPLES\nFAIL")
    sys.exit(0)
n = len(samples)
def pct(p):
    idx = max(0, min(n - 1, int(round((p / 100.0) * n)) - 1))
    return samples[idx]
p99 = pct(99)
print(f"n={len(samples)} p50={pct(50)}ms p95={pct(95)}ms p99={p99}ms max={samples[-1]}ms")
print("PASS" if p99 < limit else "FAIL")
PY
)
  stats_line=$(echo "$result" | head -1)
  verdict=$(echo "$result" | tail -1)
  echo "       $stats_line"
  if [[ "$verdict" == "PASS" ]]; then
    record_pass "Menu P99 < ${MENU_P99_MS} ms"
  else
    record_fail "Menu P99 >= ${MENU_P99_MS} ms ($stats_line)"
  fi
  echo
}

# --- Test 3: Single order create latency ---
test_order_create_latency() {
  echo "=== A1-PERF-03: Order create latency (P99 smoke, target < ${ORDER_CREATE_P99_MS} ms) ==="
  local latencies_file="$TMPDIR/order_single_latencies.txt"
  : > "$latencies_file"
  local i idem ms code

  for i in $(seq 1 10); do
    idem="perf-single-${RANDOM}-${i}-$(date +%s%N)"
    code=$(curl -s -o "$TMPDIR/order.json" -w "%{http_code} %{time_total}" \
      -X POST "$BASE_URL/api/v1/orders" \
      -H "Content-Type: application/json" \
      -H "Idempotency-Key: $idem" \
      -H "X-Customer-Id: $CUSTOMER_ID" \
      -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
      -d "{\"restaurantId\":\"$RESTAURANT_ID\",\"deliveryAddress\":\"Perf Test $i, Pune\",\"city\":\"Pune\",\"state\":\"Maharashtra\",\"pincode\":\"411001\",\"paymentMode\":\"UPI\",\"items\":[{\"menuItemId\":\"$MENU_ITEM_ID\",\"quantity\":1}]}")
    ms=$(echo "$code" | awk '{print int($2*1000)}')
    http=$(echo "$code" | awk '{print $1}')
    if [[ "$http" == "202" ]]; then
      echo "$ms" >> "$latencies_file"
    else
      echo "       sample $i returned HTTP $http" >&2
    fi
  done

  local result stats_line verdict
  result=$(python3 - "$latencies_file" "$ORDER_CREATE_P99_MS" <<'PY'
import sys
from pathlib import Path
path = Path(sys.argv[1])
limit = int(sys.argv[2])
samples = sorted(int(x) for x in path.read_text().splitlines() if x.strip())
if not samples:
    print("no successful 202 responses\nFAIL")
    sys.exit(0)
def pct(arr, p):
    n = len(arr)
    idx = max(0, min(n - 1, int(round((p / 100.0) * n)) - 1))
    return arr[idx]
p99 = pct(samples, 99)
print(f"n={len(samples)} p50={pct(samples,50)}ms p99={p99}ms max={samples[-1]}ms")
print("PASS" if p99 < limit else "FAIL")
PY
)
  stats_line=$(echo "$result" | head -1)
  verdict=$(echo "$result" | tail -1)
  echo "       $stats_line"
  if [[ "$verdict" == "PASS" ]]; then
    record_pass "Order create P99 < ${ORDER_CREATE_P99_MS} ms (async 202 path)"
  else
    record_fail "Order create P99 >= ${ORDER_CREATE_P99_MS} ms ($stats_line)"
  fi
  echo
}

# --- Test 4: Sustained order throughput (500/min design target) ---
test_order_throughput() {
  echo "=== A1-PERF-04: Order throughput (${PERF_ORDERS_PER_MIN}/min for ${PERF_ORDER_LOAD_SEC}s, min ${ORDER_SUCCESS_MIN_PCT}% success) ==="
  export BASE_URL CUSTOMER_ID CUSTOMER_TOKEN RESTAURANT_ID MENU_ITEM_ID
  export PERF_ORDERS_PER_MIN PERF_ORDER_LOAD_SEC ORDER_SUCCESS_MIN_PCT ORDER_CREATE_P99_MS
  export TMPDIR

  python3 <<'PY'
import json
import os
import sys
import time
import urllib.error
import urllib.request
import uuid

base = os.environ["BASE_URL"].rstrip("/")
customer_id = os.environ["CUSTOMER_ID"]
customer_token = os.environ["CUSTOMER_TOKEN"]
restaurant_id = os.environ["RESTAURANT_ID"]
menu_item_id = os.environ["MENU_ITEM_ID"]
orders_per_min = int(os.environ["PERF_ORDERS_PER_MIN"])
duration_sec = float(os.environ["PERF_ORDER_LOAD_SEC"])
min_success_pct = float(os.environ["ORDER_SUCCESS_MIN_PCT"])
p99_limit = int(os.environ["ORDER_CREATE_P99_MS"])

rate = orders_per_min / 60.0
interval = 1.0 / rate if rate > 0 else 0.12
expected = int(rate * duration_sec)

latencies = []
success = 0
failed = 0
start = time.monotonic()
sent = 0

while time.monotonic() - start < duration_sec:
    t0 = time.monotonic()
    idem = f"perf-load-{uuid.uuid4()}"
    body = json.dumps({
        "restaurantId": restaurant_id,
        "deliveryAddress": f"Load Test {sent}, Pune",
        "city": "Pune",
        "state": "Maharashtra",
        "pincode": "411001",
        "paymentMode": "UPI",
        "items": [{"menuItemId": menu_item_id, "quantity": 1}],
    }).encode()
    req = urllib.request.Request(
        f"{base}/api/v1/orders",
        data=body,
        method="POST",
        headers={
            "Content-Type": "application/json",
            "Idempotency-Key": idem,
            "X-Customer-Id": customer_id,
            "X-Customer-Api-Key": customer_token,
        },
    )
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            elapsed_ms = int((time.monotonic() - t0) * 1000)
            if resp.status == 202:
                success += 1
                latencies.append(elapsed_ms)
            else:
                failed += 1
    except urllib.error.HTTPError as e:
        failed += 1
    except Exception:
        failed += 1
    sent += 1
    elapsed = time.monotonic() - t0
    sleep_for = interval - elapsed
    if sleep_for > 0:
        time.sleep(sleep_for)

wall = time.monotonic() - start
actual_rate = (success / wall) * 60 if wall > 0 else 0
success_pct = (success / sent * 100) if sent else 0

def pct(arr, p):
    if not arr:
        return 0
    arr = sorted(arr)
    n = len(arr)
    idx = max(0, min(n - 1, int(round((p / 100.0) * n)) - 1))
    return arr[idx]

p99 = pct(latencies, 99) if latencies else 9999
p50 = pct(latencies, 50) if latencies else 0

print(f"       sent={sent} success_202={success} failed={failed} success_pct={success_pct:.1f}%")
print(f"       wall={wall:.1f}s actual_rate={actual_rate:.1f}/min target={orders_per_min}/min")
if latencies:
    print(f"       latency p50={p50}ms p99={p99}ms max={max(latencies)}ms")

rate_ok = actual_rate >= (orders_per_min * 0.85)  # 85% of target on laptop
success_ok = success_pct >= min_success_pct
latency_ok = p99 < p99_limit if latencies else False

out = os.path.join(os.environ["TMPDIR"], "order_load_result.txt")
with open(out, "w") as f:
    f.write(f"{rate_ok and success_ok and latency_ok}\n")
    f.write(f"rate_ok={rate_ok} success_ok={success_ok} latency_ok={latency_ok}\n")

if rate_ok and success_ok and latency_ok:
    print("PASS")
else:
    print("FAIL")
    if not rate_ok:
        print(f"       rate below 85% of {orders_per_min}/min", file=sys.stderr)
    if not success_ok:
        print(f"       success rate below {min_success_pct}%", file=sys.stderr)
    if not latency_ok:
        print(f"       load P99 >= {p99_limit}ms", file=sys.stderr)
PY

  local load_ok details
  load_ok=$(head -1 "$TMPDIR/order_load_result.txt" 2>/dev/null || echo "False")
  details=$(tail -1 "$TMPDIR/order_load_result.txt" 2>/dev/null || echo "")
  if [[ "$load_ok" == "True" ]]; then
    record_pass "Sustained order acceptance at ~${PERF_ORDERS_PER_MIN}/min (${PERF_ORDER_LOAD_SEC}s window)"
  else
    record_fail "Order throughput below target ($details)"
  fi
  echo
}

# --- Test 5: GPS simulator (50 drivers / ~10 evt/s) ---
test_gps_simulator() {
  echo "=== A1-PERF-05: GPS simulator (${GPS_EXPECTED_DRIVERS} drivers, >= ${GPS_MIN_EVT_PER_SEC} evt/s) ==="

  local compose_file="$REPO_ROOT/docker-compose.yml"
  local sim_enabled sim_drivers sim_interval expected_rate driver_count

  sim_enabled=$(grep -E 'GPS_SIMULATOR_ENABLED:' "$compose_file" | tail -1 | awk '{print $2}' | tr -d '"')
  sim_drivers=$(grep -E 'GPS_SIMULATOR_DRIVER_COUNT:' "$compose_file" | tail -1 | awk '{print $2}' | tr -d '"')
  sim_interval=$(grep -E 'GPS_SIMULATOR_INTERVAL_MS:' "$compose_file" | tail -1 | awk '{print $2}' | tr -d '"')
  sim_enabled="${sim_enabled:-true}"
  sim_drivers="${sim_drivers:-$GPS_EXPECTED_DRIVERS}"
  sim_interval="${sim_interval:-5000}"

  expected_rate=$(python3 -c "print(round(int('$sim_drivers') / (int('$sim_interval') / 1000.0), 1))")
  echo "       compose config: enabled=$sim_enabled drivers=$sim_drivers interval_ms=$sim_interval → ~${expected_rate} evt/s"

  if [[ "$sim_enabled" == "true" ]] && [[ "$sim_drivers" -ge "$GPS_EXPECTED_DRIVERS" ]]; then
    record_pass "GPS simulator configured for ${sim_drivers} drivers"
  else
    record_fail "GPS simulator config below target (enabled=$sim_enabled drivers=$sim_drivers)"
  fi

  if python3 -c "import sys; sys.exit(0 if float('$expected_rate') >= float('$GPS_MIN_EVT_PER_SEC') else 1)"; then
    record_pass "Expected simulator throughput ~${expected_rate}/s >= ${GPS_MIN_EVT_PER_SEC}/s"
  else
    record_fail "Expected throughput ${expected_rate}/s below ${GPS_MIN_EVT_PER_SEC}/s"
  fi

  if [[ "$SKIP_DOCKER_GPS_CHECK" != "true" ]] && command -v docker >/dev/null 2>&1; then
    docker compose -f "$compose_file" --project-directory "$REPO_ROOT" logs backend-service >"$TMPDIR/backend.log" 2>&1 || true
    if grep -q "GPS simulator initialized for ${GPS_EXPECTED_DRIVERS} drivers" "$TMPDIR/backend.log"; then
      record_pass "Runtime log: GPS simulator initialized for ${GPS_EXPECTED_DRIVERS} drivers"
    elif grep -q "GPS simulator initialized for" "$TMPDIR/backend.log"; then
      local init_count
      init_count=$(grep "GPS simulator initialized for" "$TMPDIR/backend.log" | tail -1 | sed -E 's/.*initialized for ([0-9]+) drivers.*/\1/')
      record_warn "GPS simulator initialized for ${init_count} drivers (expected ${GPS_EXPECTED_DRIVERS})"
    else
      record_warn "Could not find GPS simulator init log — is the stack running via docker compose?"
    fi

    driver_count=$(docker compose -f "$compose_file" --project-directory "$REPO_ROOT" exec -T postgres \
      psql -U swifteats -d swifteats -t -A -c "SELECT count(*) FROM driver;" 2>/dev/null | tr -d '[:space:]' || echo "0")
    driver_count="${driver_count:-0}"
    echo "       database driver rows: ${driver_count}"
    if [[ "$driver_count" -ge "$GPS_EXPECTED_DRIVERS" ]]; then
      record_pass "Database has >= ${GPS_EXPECTED_DRIVERS} drivers (seed data)"
    else
      record_warn "Database driver count ${driver_count} < ${GPS_EXPECTED_DRIVERS}"
    fi
  else
    record_warn "Skipping docker runtime GPS checks (SKIP_DOCKER_GPS_CHECK=$SKIP_DOCKER_GPS_CHECK)"
  fi

  # Direct GPS POST latency (customer-facing ingest path)
  local idem="perf-gps-$(date +%s)"
  local order_resp
  order_resp=$(curl -s -o "$TMPDIR/gps_order.json" -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/orders" \
    -H "Content-Type: application/json" \
    -H "Idempotency-Key: $idem" \
    -H "X-Customer-Id: $CUSTOMER_ID" \
    -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
    -d "{\"restaurantId\":\"$RESTAURANT_ID\",\"deliveryAddress\":\"GPS Perf, Pune\",\"city\":\"Pune\",\"state\":\"Maharashtra\",\"pincode\":\"411001\",\"paymentMode\":\"UPI\",\"items\":[{\"menuItemId\":\"$MENU_ITEM_ID\",\"quantity\":1}]}")
  if [[ "$order_resp" != "202" ]]; then
    record_fail "Could not create order for GPS test (HTTP $order_resp)"
    echo
    return
  fi
  local order_id status
  order_id=$(python3 -c "import json; print(json.load(open('$TMPDIR/gps_order.json'))['orderId'])")

  status="PENDING_PAYMENT"
  for _ in $(seq 1 15); do
    curl -s -o "$TMPDIR/gps_order_status.json" \
      -H "X-Customer-Id: $CUSTOMER_ID" \
      -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
      "$BASE_URL/api/v1/orders/$order_id" >/dev/null || true
    status=$(python3 -c "import json; print(json.load(open('$TMPDIR/gps_order_status.json')).get('status',''))" 2>/dev/null || echo "")
    if [[ "$status" != "PENDING_PAYMENT" && -n "$status" ]]; then
      break
    fi
    sleep 1
  done

  curl -s -o /dev/null \
    -X PATCH "$BASE_URL/api/v1/admin/orders/$order_id/state" \
    -H "Content-Type: application/json" \
    -H "X-Admin-Api-Key: $ADMIN_KEY" \
    -d '{"status":"PREPARING","changedBy":"perf-gps"}'

  curl -s -o /dev/null \
    -X PATCH "$BASE_URL/api/v1/admin/orders/$order_id/state" \
    -H "Content-Type: application/json" \
    -H "X-Admin-Api-Key: $ADMIN_KEY" \
    -d '{"status":"OUT_FOR_DELIVERY","changedBy":"perf-gps"}'

  local gps_ms gps_code
  gps_ms=$(curl -s -o /dev/null -w "%{time_total}" \
    -X POST "$BASE_URL/api/v1/drivers/$DRIVER_ID/location" \
    -H "Content-Type: application/json" \
    -H "X-Driver-Api-Key: $DRIVER_TOKEN" \
    -d "{\"latitude\":18.5204,\"longitude\":73.8567,\"heading\":90,\"orderId\":\"$order_id\"}" \
    | python3 -c "import sys; print(int(float(sys.stdin.read())*1000))")
  gps_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -X POST "$BASE_URL/api/v1/drivers/$DRIVER_ID/location" \
    -H "Content-Type: application/json" \
    -H "X-Driver-Api-Key: $DRIVER_TOKEN" \
    -d "{\"latitude\":18.5205,\"longitude\":73.8568,\"heading\":91,\"orderId\":\"$order_id\"}")

  if [[ "$gps_code" == "202" ]] && [[ "$gps_ms" -lt "$GPS_INGEST_MAX_MS" ]]; then
    record_pass "GPS POST accepted 202 in ${gps_ms}ms (< ${GPS_INGEST_MAX_MS}ms)"
  else
    record_fail "GPS POST HTTP $gps_code latency ${gps_ms}ms (target < ${GPS_INGEST_MAX_MS}ms)"
  fi

  local track_ms track_code
  sleep 1
  track_ms=$(curl -s -o "$TMPDIR/tracking.json" -w "%{time_total}" \
    -H "X-Customer-Id: $CUSTOMER_ID" \
    -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
    "$BASE_URL/api/v1/orders/$order_id/tracking" \
    | python3 -c "import sys; print(int(float(sys.stdin.read())*1000))")
  track_code=$(curl -s -o /dev/null -w "%{http_code}" \
    -H "X-Customer-Id: $CUSTOMER_ID" \
    -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
    "$BASE_URL/api/v1/orders/$order_id/tracking")

  if [[ "$track_code" == "200" ]] && [[ "$track_ms" -lt "$TRACKING_MAX_MS" ]]; then
    record_pass "Tracking snapshot 200 in ${track_ms}ms (< ${TRACKING_MAX_MS}ms)"
  else
    record_fail "Tracking HTTP $track_code latency ${track_ms}ms"
  fi
  echo
}

# --- Test 6: Optional E2E functional sanity (reuse existing script) ---
test_e2e_sanity() {
  echo "=== A1-PERF-06: E2E functional sanity (reuse e2e-smoke.sh) ==="
  if [[ -x "$SCRIPT_DIR/e2e-smoke.sh" ]]; then
    if "$SCRIPT_DIR/e2e-smoke.sh" "$BASE_URL" >/dev/null 2>&1; then
      record_pass "e2e-smoke.sh passed (full order → GPS → tracking path)"
    else
      record_fail "e2e-smoke.sh failed — run manually for details: ./scripts/e2e-smoke.sh $BASE_URL"
    fi
  else
    record_warn "e2e-smoke.sh not executable; skipping"
  fi
  echo
}

main() {
  require_tools
  echo "=== SwiftEats Assignment 1 — Performance Verification ==="
  echo "Base URL: $BASE_URL"
  echo "Targets: menu P99<${MENU_P99_MS}ms | orders ${PERF_ORDERS_PER_MIN}/min | GPS ${GPS_EXPECTED_DRIVERS} drivers"
  echo

  test_health || exit 1
  test_menu_p99
  test_order_create_latency
  test_order_throughput
  test_gps_simulator
  test_e2e_sanity

  echo "=== Summary: $pass passed, $fail failed, $warn warnings ==="
  if [[ "$fail" -eq 0 ]]; then
    echo "All Assignment 1 performance checks passed."
    exit 0
  else
    echo "Some checks failed. Tune env vars or see README § Performance & scale validation."
    exit 1
  fi
}

main "$@"

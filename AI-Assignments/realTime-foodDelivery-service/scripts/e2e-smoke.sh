#!/usr/bin/env bash
# E2E smoke test — mirrors Postman folder "01 — E2E Happy Path".
# Usage: ./scripts/e2e-smoke.sh [baseUrl]
set -euo pipefail

BASE_URL="${1:-http://localhost:8080}"
ADMIN_KEY="${ADMIN_API_KEY:-local-dev-admin-key-change-me}"
CUSTOMER_ID="${CUSTOMER_ID:-44444444-4444-4444-4444-444444444401}"
CUSTOMER_TOKEN="${CUSTOMER_API_TOKEN:-demo-customer-token-local-only}"
RESTAURANT_ID="${RESTAURANT_ID:-22222222-2222-2222-2222-222222222201}"
MENU_ITEM_ID="${MENU_ITEM_ID:-33333333-3333-3333-3333-333333333301}"
DRIVER_ID="${DRIVER_ID:-55555555-5555-5555-5555-555555555501}"
DRIVER_TOKEN="${DRIVER_API_TOKEN:-demo-driver-token-001}"

pass=0
fail=0

check() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [[ "$actual" == "$expected" ]]; then
    echo "  OK   $name (HTTP $actual)"
    pass=$((pass + 1))
  else
    echo "  FAIL $name (expected HTTP $expected, got $actual)"
    fail=$((fail + 1))
  fi
}

echo "=== SwiftEats E2E smoke ==="
echo "Base URL: $BASE_URL"
echo

echo "01 — Health"
code=$(curl -s -o /tmp/swifteats-health.json -w "%{http_code}" "$BASE_URL/actuator/health")
check "health" "200" "$code"
grep -q '"status":"UP"' /tmp/swifteats-health.json || { echo "  FAIL health body not UP"; fail=$((fail + 1)); }

echo "02 — List restaurants"
code=$(curl -s -o /tmp/swifteats-restaurants.json -w "%{http_code}" \
  "$BASE_URL/api/v1/restaurants?city=Pune&page=0&size=20")
check "list restaurants" "200" "$code"

echo "03 — Get menu"
code=$(curl -s -o /tmp/swifteats-menu.json -w "%{http_code}" \
  "$BASE_URL/api/v1/restaurants/$RESTAURANT_ID/menu")
check "get menu" "200" "$code"

echo "04 — Create order"
IDEM_KEY="e2e-smoke-$(date +%s)"
code=$(curl -s -o /tmp/swifteats-order-create.json -w "%{http_code}" \
  -X POST "$BASE_URL/api/v1/orders" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $IDEM_KEY" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
  -d "{\"restaurantId\":\"$RESTAURANT_ID\",\"deliveryAddress\":\"123 MG Road, Pune\",\"city\":\"Pune\",\"state\":\"Maharashtra\",\"pincode\":\"411001\",\"paymentMode\":\"UPI\",\"items\":[{\"menuItemId\":\"$MENU_ITEM_ID\",\"quantity\":2}]}")
check "create order" "202" "$code"
ORDER_ID=$(python3 -c "import json; print(json.load(open('/tmp/swifteats-order-create.json'))['orderId'])" 2>/dev/null || true)
if [[ -z "${ORDER_ID:-}" ]]; then
  echo "  FAIL no orderId in response"
  fail=$((fail + 1))
else
  echo "       orderId=$ORDER_ID"
fi

echo "05 — Poll payment (up to 15s)"
status="PENDING_PAYMENT"
for _ in $(seq 1 15); do
  code=$(curl -s -o /tmp/swifteats-order.json -w "%{http_code}" \
    -H "X-Customer-Id: $CUSTOMER_ID" \
    -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
    "$BASE_URL/api/v1/orders/$ORDER_ID")
  status=$(python3 -c "import json; print(json.load(open('/tmp/swifteats-order.json')).get('status',''))" 2>/dev/null || echo "")
  if [[ "$status" != "PENDING_PAYMENT" && -n "$status" ]]; then
    break
  fi
  sleep 1
done
check "get order" "200" "$code"
if [[ "$status" == "PENDING_PAYMENT" ]]; then
  echo "  WARN payment still pending (status=$status)"
else
  echo "       status=$status"
fi

echo "06 — Admin PREPARING"
code=$(curl -s -o /tmp/swifteats-prep.json -w "%{http_code}" \
  -X PATCH "$BASE_URL/api/v1/admin/orders/$ORDER_ID/state" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $ADMIN_KEY" \
  -d '{"status":"PREPARING","changedBy":"e2e-smoke"}')
check "PREPARING" "200" "$code"

echo "07 — Admin OUT_FOR_DELIVERY"
code=$(curl -s -o /tmp/swifteats-ofd.json -w "%{http_code}" \
  -X PATCH "$BASE_URL/api/v1/admin/orders/$ORDER_ID/state" \
  -H "Content-Type: application/json" \
  -H "X-Admin-Api-Key: $ADMIN_KEY" \
  -d '{"status":"OUT_FOR_DELIVERY","changedBy":"e2e-smoke"}')
check "OUT_FOR_DELIVERY" "200" "$code"

echo "08 — Post driver GPS"
code=$(curl -s -o /tmp/swifteats-gps.json -w "%{http_code}" \
  -X POST "$BASE_URL/api/v1/drivers/$DRIVER_ID/location" \
  -H "Content-Type: application/json" \
  -H "X-Driver-Api-Key: $DRIVER_TOKEN" \
  -d "{\"latitude\":18.5204,\"longitude\":73.8567,\"heading\":135,\"orderId\":\"$ORDER_ID\"}")
check "GPS ingest" "202" "$code"

echo "09 — Tracking snapshot"
code=$(curl -s -o /tmp/swifteats-tracking.json -w "%{http_code}" \
  -H "X-Customer-Id: $CUSTOMER_ID" \
  -H "X-Customer-Api-Key: $CUSTOMER_TOKEN" \
  "$BASE_URL/api/v1/orders/$ORDER_ID/tracking")
check "tracking" "200" "$code"

echo
echo "=== Summary: $pass passed, $fail failed ==="
[[ "$fail" -eq 0 ]]

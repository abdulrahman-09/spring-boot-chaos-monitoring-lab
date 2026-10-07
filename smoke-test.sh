#!/usr/bin/env bash
# Quick end-to-end check of both services. Run after `docker compose up -d --build`.
# Usage: ./smoke-test.sh        (override with ORDER_URL / PAYMENT_URL if needed)

ORDER_URL="${ORDER_URL:-http://localhost:8080}"
PAYMENT_URL="${PAYMENT_URL:-http://localhost:8082}"
JSON='Content-Type: application/json'

passed=0
failed=0

check() { # name expected actual
  if [ "$2" = "$3" ]; then
    echo "PASS  $1"
    passed=$((passed + 1))
  else
    echo "FAIL  $1 (expected $2, got $3)"
    failed=$((failed + 1))
  fi
}

status() { curl -s -o /dev/null -w '%{http_code}' "$@"; }

echo "== payment-service (direct) =="
check "valid payment returns 200" 200 \
  "$(status -X POST "$PAYMENT_URL/payments" -H "$JSON" -d '{"orderId":"t-1","amount":10.5}')"
check "negative amount returns 400" 400 \
  "$(status -X POST "$PAYMENT_URL/payments" -H "$JSON" -d '{"orderId":"t-1","amount":-1}')"

echo "== order-service =="
body=$(curl -s -X POST "$ORDER_URL/orders" -H "$JSON" -d '{"customerId":"c-123","amount":49.90}')
order_id=$(echo "$body" | grep -o '"id":"[^"]*"' | head -1 | cut -d'"' -f4)
check "create order is PAID" true "$(echo "$body" | grep -q '"status":"PAID"' && echo true || echo false)"
check "get existing order returns 200" 200 "$(status "$ORDER_URL/orders/$order_id")"
check "get unknown order returns 404" 404 \
  "$(status "$ORDER_URL/orders/00000000-0000-0000-0000-000000000000")"
check "invalid order returns 400" 400 \
  "$(status -X POST "$ORDER_URL/orders" -H "$JSON" -d '{"customerId":"c-1","amount":-5}')"

echo "== failure injection =="
curl -s -X POST "$PAYMENT_URL/chaos/errors?rate=1" > /dev/null
check "payment errors -> order returns 502" 502 \
  "$(status -X POST "$ORDER_URL/orders" -H "$JSON" -d '{"customerId":"c-1","amount":5}')"
curl -s -X POST "$PAYMENT_URL/chaos/reset" > /dev/null

curl -s -X POST "$PAYMENT_URL/chaos/latency?ms=3000" > /dev/null
check "slow payment -> order returns 504" 504 \
  "$(status -X POST "$ORDER_URL/orders" -H "$JSON" -d '{"customerId":"c-1","amount":5}')"
check "GET still works while payment is slow" 200 "$(status "$ORDER_URL/orders/$order_id")"
curl -s -X POST "$PAYMENT_URL/chaos/reset" > /dev/null

echo
echo "passed=$passed failed=$failed"
[ "$failed" -eq 0 ]

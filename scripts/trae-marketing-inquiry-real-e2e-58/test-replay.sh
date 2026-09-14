#!/bin/bash
# TR58: replay test with changed remark (different payload -> 409)
# Sensitive values from DB env vars, output redacted
set -u
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== DIFFERENT PAYLOAD REPLAY (changed remark -> 409) ==="

REQ=$($M "$SCHEMA" -e "SELECT request_id FROM marketing_attribution_event WHERE business_no='INQ3' AND event_type='inquiry' LIMIT 1;")
ORIG_NAME=$($M "$SCHEMA" -e "SELECT customer_name FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_PHONE=$($M "$SCHEMA" -e "SELECT customer_phone FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_DATE=$($M "$SCHEMA" -e "SELECT preferred_date FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_PARTY=$($M "$SCHEMA" -e "SELECT guest_count FROM booking_inquiry WHERE id=3 LIMIT 1;")

BI_BEFORE=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM booking_inquiry;")
EVT_BEFORE=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM marketing_attribution_event;")
echo "  before: booking_inquiry=$BI_BEFORE, events=$EVT_BEFORE"

# Changed remark (different payload, same requestId)
RESP=$(curl -s -X POST http://127.0.0.1:18080/api/public/booking-inquiry \
  -H 'Content-Type: application/json' \
  -d "{\"customerName\":\"$ORIG_NAME\",\"phone\":\"$ORIG_PHONE\",\"expectedDate\":\"$ORIG_DATE\",\"partySize\":$ORIG_PARTY,\"sourceCode\":\"SRC-VISIBLE-001\",\"requestId\":\"$REQ\",\"remark\":\"DIFFERENT_REMARK\"}")

RESP_CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null)
echo "  response_code=$RESP_CODE (expected 409)"

BI_AFTER=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM booking_inquiry;")
EVT_AFTER=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM marketing_attribution_event;")
echo "  after: booking_inquiry=$BI_AFTER, events=$EVT_AFTER"
echo "  delta: booking_inquiry=$((BI_AFTER-BI_BEFORE)), events=$((EVT_AFTER-EVT_BEFORE))"

if [ "$BI_AFTER" -eq "$BI_BEFORE" ] && [ "$EVT_AFTER" -eq "$EVT_BEFORE" ] && [ "$RESP_CODE" = "409" ]; then
  echo "  [PASS] different-payload replay: 409, zero new rows"
else
  echo "  [FAIL] check above"
fi
echo "REPLAY_DONE"

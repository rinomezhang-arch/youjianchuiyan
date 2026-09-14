#!/bin/bash
# TR58-R2: same-payload replay test
# Uses env vars for sensitive values; only outputs redacted/desensitized data
set -u
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== SAME PAYLOAD REPLAY ==="

# Get the exact requestId from INQ3 first submission
REQ=$($M "$SCHEMA" -e "SELECT request_id FROM marketing_attribution_event WHERE business_no='INQ3' AND event_type='inquiry' LIMIT 1;")
if [ -z "$REQ" ]; then echo "  [FAIL] no INQ3 requestId found in DB"; exit 1; fi
echo "  requestId=[REDACTED]"

# Get original payload from DB (not logging values, just using them)
ORIG_NAME=$($M "$SCHEMA" -e "SELECT customer_name FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_PHONE=$($M "$SCHEMA" -e "SELECT customer_phone FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_DATE=$($M "$SCHEMA" -e "SELECT preferred_date FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_PARTY=$($M "$SCHEMA" -e "SELECT guest_count FROM booking_inquiry WHERE id=3 LIMIT 1;")
ORIG_REMARK=$($M "$SCHEMA" -e "SELECT remark FROM booking_inquiry WHERE id=3 LIMIT 1;")

# Get counts before
BI_BEFORE=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM booking_inquiry;")
EVT_BEFORE=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM marketing_attribution_event;")
echo "  before: booking_inquiry=$BI_BEFORE, events=$EVT_BEFORE"

# Replay with EXACT same payload from DB
RESP=$(curl -s -X POST http://127.0.0.1:18080/api/public/booking-inquiry \
  -H 'Content-Type: application/json' \
  -d "{\"customerName\":\"$ORIG_NAME\",\"phone\":\"$ORIG_PHONE\",\"expectedDate\":\"$ORIG_DATE\",\"partySize\":$ORIG_PARTY,\"sourceCode\":\"SRC-VISIBLE-001\",\"requestId\":\"$REQ\",\"remark\":\"$ORIG_REMARK\"}")

# Extract only code and inquiryNo (no sensitive data)
RESP_CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null)
RESP_INQ=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('data',{}).get('inquiryNo',''))" 2>/dev/null)
echo "  response_code=$RESP_CODE"
echo "  response_inquiryNo=$RESP_INQ"

# Get counts after
BI_AFTER=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM booking_inquiry;")
EVT_AFTER=$($M "$SCHEMA" -e "SELECT COUNT(*) FROM marketing_attribution_event;")
echo "  after: booking_inquiry=$BI_AFTER, events=$EVT_AFTER"
echo "  delta: booking_inquiry=$((BI_AFTER-BI_BEFORE)), events=$((EVT_AFTER-EVT_BEFORE))"

if [ "$BI_AFTER" -eq "$BI_BEFORE" ] && [ "$EVT_AFTER" -eq "$EVT_BEFORE" ] && [ "$RESP_CODE" = "200" ] && [ "$RESP_INQ" = "INQ3" ]; then
  echo "  [PASS] same-payload replay: 200, same inquiryNo, zero new rows"
else
  echo "  [FAIL] check above"
  exit 1
fi
echo "REPLAY_DONE"

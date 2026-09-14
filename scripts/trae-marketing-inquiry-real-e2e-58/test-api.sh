#!/bin/bash
# TR58: API connectivity test (desensitized, reads sensitive from DB)
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== API TEST ==="
ORIG_PHONE=$($M "$SCHEMA" -e "SELECT customer_phone FROM booking_inquiry WHERE id=3 LIMIT 1;")
RESP=$(curl -s -X POST http://127.0.0.1:18080/api/public/booking-inquiry/lookup \
  -H 'Content-Type: application/json' \
  -d "{\"inquiryNo\":\"INQ3\",\"phone\":\"$ORIG_PHONE\"}")
RESP_CODE=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin)['code'])" 2>/dev/null)
RESP_STATUS=$(echo "$RESP" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('status',''))" 2>/dev/null)
echo "  lookup_code=$RESP_CODE status=$RESP_STATUS"
echo "API_TEST_DONE"

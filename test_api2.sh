#!/bin/bash
H="-H 'X-Store-Id: 1' -H 'X-Staff-Id: 1' -H 'X-Device-Sn: test001' -H 'X-Client-Type: ipad'"

echo "=== 1. dish/list ==="
eval curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/api/ipad/dish/list $H
echo

echo "=== 2. staff/verify ==="
echo '{"card_number":"rino","password":"002323"}' > /tmp/sv.json
eval curl -s -X POST http://localhost:8080/api/ipad/staff/verify -H "'Content-Type: application/json'" $H -d @/tmp/sv.json
echo

echo "=== 3. order/customer ==="
echo '{"table_id":14,"customer_name":"test","phone":"13800138000","person_count":4,"note":"test"}' > /tmp/oc.json
eval curl -s -X POST http://localhost:8080/api/ipad/order/customer -H "'Content-Type: application/json'" $H -d @/tmp/oc.json
echo

echo "=== 4. send-kitchen ==="
echo '{"table_id":14,"order_note":"test note"}' > /tmp/sk.json
eval curl -s -X POST http://localhost:8080/api/ipad/order/send-kitchen -H "'Content-Type: application/json'" $H -d @/tmp/sk.json
echo

#!/bin/bash
echo "=== 1. dish/list ==="
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8080/api/ipad/dish/list \
  -H 'X-Store-Id: 1' -H 'X-Staff-Id: 1' -H 'X-Device-Sn: test001' -H 'X-Client-Type: ipad'

echo "=== 2. staff/verify ==="
cat > /tmp/staff_test.json << 'EOF'
{"card_number":"001","password":"123456"}
EOF
curl -s -X POST http://localhost:8080/api/ipad/staff/verify \
  -H 'Content-Type: application/json' \
  -H 'X-Store-Id: 1' -H 'X-Staff-Id: 1' -H 'X-Device-Sn: test001' -H 'X-Client-Type: ipad' \
  -d @/tmp/staff_test.json
echo

echo "=== 3. order/customer ==="
cat > /tmp/customer_test.json << 'EOF'
{"table_id":"A01","customer_name":"test","phone":"13800138000","person_count":4,"note":"test note"}
EOF
curl -s -X POST http://localhost:8080/api/ipad/order/customer \
  -H 'Content-Type: application/json' \
  -H 'X-Store-Id: 1' -H 'X-Staff-Id: 1' -H 'X-Device-Sn: test001' -H 'X-Client-Type: ipad' \
  -d @/tmp/customer_test.json
echo

echo "=== 4. send-kitchen ==="
cat > /tmp/kitchen_test.json << 'EOF'
{"table_id":"A01","order_note":"test order note","staff_id":1}
EOF
curl -s -X POST http://localhost:8080/api/ipad/order/send-kitchen \
  -H 'Content-Type: application/json' \
  -H 'X-Store-Id: 1' -H 'X-Staff-Id: 1' -H 'X-Device-Sn: test001' -H 'X-Client-Type: ipad' \
  -d @/tmp/kitchen_test.json
echo

echo "=== 5. Check staff_master table ==="
cat > /tmp/check_staff.py << 'PYEOF'
import pymysql
conn = pymysql.connect(host='localhost', user='root', password='youjian2024', database='banquet_management', charset='utf8mb4')
cur = conn.cursor()
cur.execute("SELECT staff_id, staff_name, card_number, password FROM staff_master LIMIT 5")
for row in cur.fetchall():
    print(row)
conn.close()
PYEOF
python3 /tmp/check_staff.py 2>/dev/null || echo "pymysql not available, trying mysql cli"
mysql -u root -pyoujian2024 banquet_management -e "SELECT staff_id, staff_name, card_number, password FROM staff_master LIMIT 5" 2>/dev/null

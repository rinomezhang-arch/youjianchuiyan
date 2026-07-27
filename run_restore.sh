#!/bin/bash
docker exec mysql-banquet mysql -uroot -pBanquet123! banquet << 'EOF'
UPDATE orders SET table_id = 14 WHERE booking_id = 'BK20260720001';
UPDATE orders SET table_id = 20 WHERE booking_id = 'BK20260720002';
UPDATE orders SET table_id = 22 WHERE booking_id = 'BK20260721001';
UPDATE orders SET table_id = 23 WHERE booking_id = 'BK20260721002';
UPDATE orders SET table_id = 24 WHERE booking_id = 'BK20260722001';
SELECT id, booking_id, table_id FROM orders ORDER BY id;
EOF
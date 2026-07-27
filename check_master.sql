SELECT booking_id, customer_name, guest_count, table_count, total_amount, booking_status, created_at
FROM booking_master 
WHERE store_id=1 
ORDER BY created_at DESC 
LIMIT 15;

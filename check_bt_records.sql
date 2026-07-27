-- Check booking_table records
SELECT bt.booking_id, bt.table_name, bt.table_number, bt.store_id
FROM booking_table bt 
WHERE bt.store_id=1 
ORDER BY bt.booking_id DESC 
LIMIT 20;

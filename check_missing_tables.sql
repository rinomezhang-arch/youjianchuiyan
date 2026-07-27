-- Check which bookings have NO table assignments
SELECT bm.booking_id, bm.customer_name, bm.guest_count, bm.table_count,
  (SELECT COUNT(*) FROM booking_table bt WHERE bt.booking_id=bm.booking_id AND bt.store_id=bm.store_id) as bt_count
FROM booking_master bm 
WHERE bm.store_id=1 
  AND (SELECT COUNT(*) FROM booking_table bt WHERE bt.booking_id=bm.booking_id AND bt.store_id=bm.store_id) = 0
ORDER BY bm.created_at DESC;

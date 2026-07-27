SELECT bm.booking_id, bm.customer_name, bm.guest_count, bm.table_count, bm.total_amount,
  (SELECT COUNT(*) FROM booking_table bt WHERE bt.booking_id=bm.booking_id AND bt.store_id=bm.store_id) as bt_count,
  (SELECT GROUP_CONCAT(bt.table_name SEPARATOR ', ') FROM booking_table bt WHERE bt.booking_id=bm.booking_id AND bt.store_id=bm.store_id) as bt_names,
  (SELECT COUNT(*) FROM booking_dish_detail bd WHERE bd.booking_id=bm.booking_id AND bd.store_id=bm.store_id) as dish_count,
  (SELECT GROUP_CONCAT(CONCAT(bd.dish_name, 'x', bd.dish_quantity) SEPARATOR ', ') FROM booking_dish_detail bd WHERE bd.booking_id=bm.booking_id AND bd.store_id=bm.store_id) as dish_names
FROM booking_master bm 
WHERE bm.store_id=1 
ORDER BY bm.created_at DESC 
LIMIT 15;

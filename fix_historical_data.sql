-- Fix historical data: fill in NULL dish_name from dish_master
UPDATE booking_dish_detail bd
JOIN dish_master dm ON bd.dish_id = dm.dish_id AND bd.store_id = dm.store_id
SET bd.dish_name = dm.dish_name
WHERE bd.dish_name IS NULL AND bd.store_id = 1;

-- Fix historical data: calculate subtotal where it's NULL
UPDATE booking_dish_detail
SET subtotal = unit_price * dish_quantity
WHERE subtotal IS NULL AND unit_price IS NOT NULL AND store_id = 1;

-- Fix historical data: recalculate total_amount for all bookings
UPDATE booking_master bm
SET total_amount = (
  SELECT COALESCE(SUM(subtotal), 0)
  FROM booking_dish_detail bd
  WHERE bd.booking_id = bm.booking_id AND bd.store_id = bm.store_id
)
WHERE bm.store_id = 1;

-- Show results
SELECT booking_id, customer_name, guest_count, table_count, total_amount, booking_status
FROM booking_master WHERE store_id = 1 ORDER BY created_at DESC LIMIT 15;

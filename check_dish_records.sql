-- Check booking_dish_detail records
SELECT bd.booking_id, bd.dish_name, bd.dish_quantity, bd.unit_price, bd.subtotal
FROM booking_dish_detail bd 
WHERE bd.store_id=1 
ORDER BY bd.booking_id DESC 
LIMIT 20;

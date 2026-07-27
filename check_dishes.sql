-- Check dish details for recent bookings
SELECT bd.booking_id, bd.dish_name, bd.dish_quantity, bd.unit_price, bd.subtotal, bd.store_id
FROM booking_dish_detail bd 
WHERE bd.store_id=1 
ORDER BY bd.booking_id DESC 
LIMIT 30;

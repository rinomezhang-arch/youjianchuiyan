-- 检查orders表
SELECT order_id, table_id, booking_id, total_price FROM orders ORDER BY order_id;

-- 检查booking_table关联
SELECT booking_id, table_id FROM booking_table ORDER BY booking_id;

-- 检查table_master表
SELECT table_id, table_name, table_status FROM table_master LIMIT 10;

-- 检查orders的table_id空值情况
SELECT COUNT(*) as null_count FROM orders WHERE table_id IS NULL;

-- 检查orders与booking的关联
SELECT o.order_id, o.table_id, o.booking_id, b.booking_name 
FROM orders o LEFT JOIN booking_master b ON o.booking_id = b.booking_id
ORDER BY o.order_id;
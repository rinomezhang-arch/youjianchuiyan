-- ============================================
-- orders.table_id 数据恢复脚本
-- 由于将varchar(64)转为int导致数据丢失，需要根据booking_table关联恢复
-- ============================================

-- 1. 查看当前orders数据
SELECT id, booking_id, table_id, total_price FROM orders ORDER BY id;

-- 2. 查看booking_table关联
SELECT booking_id, table_id FROM booking_table ORDER BY booking_id;

-- 3. 恢复有booking_table关联的订单
UPDATE orders o
JOIN booking_table bt ON o.booking_id = bt.booking_id
SET o.table_id = bt.table_id;

-- 4. 检查恢复结果
SELECT id, booking_id, table_id, total_price FROM orders ORDER BY id;

-- 5. 统计恢复数量
SELECT 
  SUM(CASE WHEN table_id IS NOT NULL THEN 1 ELSE 0 END) as restored_count,
  SUM(CASE WHEN table_id IS NULL THEN 1 ELSE 0 END) as still_null_count
FROM orders;
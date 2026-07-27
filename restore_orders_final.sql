-- ============================================
-- orders.table_id 数据恢复脚本（最终版）
-- 旧预订没有booking_table关联，需要手动分配桌台
-- ============================================

-- 1. 给旧预订分配桌台（基于table_count）
-- BK20260720001: table_count=2
UPDATE orders SET table_id = 14 WHERE booking_id = 'BK20260720001' AND table_id IS NULL;

-- BK20260720002: table_count=1
UPDATE orders SET table_id = 20 WHERE booking_id = 'BK20260720002' AND table_id IS NULL;

-- BK20260721001: table_count=1
UPDATE orders SET table_id = 22 WHERE booking_id = 'BK20260721001' AND table_id IS NULL;

-- BK20260721002: table_count=2
UPDATE orders SET table_id = 23 WHERE booking_id = 'BK20260721002' AND table_id IS NULL;

-- BK20260722001: table_count=1
UPDATE orders SET table_id = 24 WHERE booking_id = 'BK20260722001' AND table_id IS NULL;

-- 2. 检查恢复结果
SELECT id, booking_id, table_id, total_price FROM orders ORDER BY id;

-- 3. 统计恢复数量
SELECT 
  SUM(CASE WHEN table_id IS NOT NULL THEN 1 ELSE 0 END) as restored_count,
  SUM(CASE WHEN table_id IS NULL THEN 1 ELSE 0 END) as still_null_count
FROM orders;
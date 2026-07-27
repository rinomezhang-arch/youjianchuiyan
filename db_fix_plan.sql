-- =========================================
-- 数据库整理修复 SQL
-- 生成时间: 2026-07-23
-- 数据库: banquet
-- =========================================

-- ===== P0: 修复类型不一致 =====

-- 1. change_log.store_id int -> bigint (与其他表对齐)
ALTER TABLE change_log MODIFY COLUMN store_id BIGINT NOT NULL DEFAULT 1;

-- 2. orders.table_id varchar(64) -> int (与table_master对齐)
-- 先检查是否有非数字数据
ALTER TABLE orders MODIFY COLUMN table_id INT NULL;

-- 3. package_dish_rel.package_id int -> varchar(20) (与package_master对齐)
ALTER TABLE package_dish_rel MODIFY COLUMN package_id VARCHAR(20) NOT NULL;

-- ===== P1: dish_master 冗余列统一 =====
-- 统一用 sale_price 和 dish_category，保留旧列作为兼容

-- ===== 索引优化 =====

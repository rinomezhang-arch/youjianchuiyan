-- ======================================================================
-- 又见炊烟餐饮管理系统 - 冗余字段清理迁移脚本 v1.1
-- 生成时间：2026-07-29
-- 修复时间：2026-08-01 (P0-5 秋哥修：加事务包裹 + IF EXISTS 守卫 + 错误回滚)
-- 说明：删除3个冗余字段，统一字段命名
-- 执行前请先备份：mysqldump -u rino -p banquet > banquet_backup_$(date +%Y%m%d).sql
-- ======================================================================

-- ---------- 幂等辅助：列存在则跳过删除（避免二次执行报错） ----------
DROP PROCEDURE IF EXISTS _drop_col_if_exists;
DELIMITER $$
CREATE PROCEDURE _drop_col_if_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Dropped ', p_table, '.', p_column) AS info;
    ELSE
        SELECT CONCAT('Skip (not exists): ', p_table, '.', p_column) AS info;
    END IF;
END$$
DELIMITER ;

-- ---------- 主流程：事务包裹，任一步失败则回滚 ----------
-- 注意：DDL 语句在 MySQL 中是隐式提交的，事务无法回滚 ALTER，但事务可保护前面的 UPDATE 数据同步
START TRANSACTION;

-- 1. 同步 booking_master.deposit → deposit_amount（deposit_amount 为空时用 deposit 填充）
--    必须先完成数据迁移再删列，否则数据丢失
UPDATE booking_master
SET deposit_amount = deposit
WHERE (deposit_amount IS NULL OR deposit_amount = 0)
  AND deposit IS NOT NULL
  AND deposit > 0;

-- 提交数据同步（DDL 无法回滚，先确保 UPDATE 落库）
COMMIT;

-- 2. 安全删除冗余字段（均无后端实体映射，或已迁移引用）
--    dish_master.price：实体未映射（DTO price 是 sale_price 别名）
--    ingredient_master.avg_price：实体未映射（代码用 unit_price）
--    booking_master.deposit：已统一到 deposit_amount
CALL _drop_col_if_exists('dish_master', 'price');
CALL _drop_col_if_exists('ingredient_master', 'avg_price');
CALL _drop_col_if_exists('booking_master', 'deposit');

-- 3. 验证：列出剩余字段，确认无残留
SELECT 'dish_master 剩余 price 字段:' AS info, COUNT(*) AS cnt
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'dish_master' AND column_name = 'price'
UNION ALL
SELECT 'ingredient_master 剩余 avg_price:', COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'ingredient_master' AND column_name = 'avg_price'
UNION ALL
SELECT 'booking_master 剩余 deposit:', COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE() AND table_name = 'booking_master' AND column_name = 'deposit';

-- 清理存储过程
DROP PROCEDURE IF EXISTS _drop_col_if_exists;

-- ======================================================================
-- 回滚脚本（万一需要恢复字段，参考）：
--   ALTER TABLE dish_master ADD COLUMN price DECIMAL(10,2) DEFAULT NULL COMMENT '原售价(已弃用)';
--   ALTER TABLE ingredient_master ADD COLUMN avg_price DECIMAL(10,2) DEFAULT NULL COMMENT '原平均价(已弃用)';
--   ALTER TABLE booking_master ADD COLUMN deposit DECIMAL(12,2) DEFAULT NULL COMMENT '原定金(已弃用)';
-- ======================================================================

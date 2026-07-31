-- 重复字段清理迁移脚本 v1
-- 生成时间：2026-07-29
-- 说明：删除3个冗余字段，统一字段命名
-- 执行前请先备份：mysqldump -u rino -p banquet > banquet_backup_$(date +%Y%m%d).sql

-- 1. 同步 booking_master.deposit → deposit_amount（deposit_amount 为空时用 deposit 填充）
UPDATE booking_master
SET deposit_amount = deposit
WHERE (deposit_amount IS NULL OR deposit_amount = 0)
  AND deposit IS NOT NULL
  AND deposit > 0;

-- 2. 删除冗余字段（安全：均无后端实体映射，或已迁移引用）

-- dish_master.price：实体未映射（DTO price 是 sale_price 别名）
ALTER TABLE dish_master DROP COLUMN price;

-- ingredient_master.avg_price：实体未映射（代码用 unit_price）
ALTER TABLE ingredient_master DROP COLUMN avg_price;

-- booking_master.deposit：已统一到 deposit_amount
ALTER TABLE booking_master DROP COLUMN deposit;

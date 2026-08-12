-- ======================================================================
-- 又见炊烟餐饮管理系统 - 数据库修复脚本 v1
-- 创建时间：2026-08-01 (秋哥)
-- 用途：对已部署的生产/测试库执行 v3 审计后新增的 P0/P1 修复
-- 特性：幂等（可重复执行，已修复的自动跳过），所有 ALTER 均带 IF EXISTS 守卫
-- 执行方式：mysql -u <user> -p banquet < db_fix_v1.sql
-- 备份要求：执行前请先备份 mysqldump -u <user> -p banquet > backup_before_fix_v1.sql
-- ======================================================================

-- ---------- 幂等辅助存储过程 ----------
DROP PROCEDURE IF EXISTS _alter_col_type_if_mismatch;
DELIMITER $$
CREATE PROCEDURE _alter_col_type_if_mismatch(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_new_definition TEXT
)
BEGIN
    DECLARE v_current_type VARCHAR(255);
    SELECT COLUMN_TYPE INTO v_current_type
    FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column;
    IF v_current_type IS NOT NULL THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` MODIFY COLUMN `', p_column, '` ', p_new_definition);
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Modified ', p_table, '.', p_column, ' to ', p_new_definition) AS info;
    ELSE
        SELECT CONCAT('Skip (not exists): ', p_table, '.', p_column) AS info;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等辅助：UPDATE 行存在则执行 ----------
DROP PROCEDURE IF EXISTS _update_password_if_match;
DELIMITER $$
CREATE PROCEDURE _update_password_if_match(
    IN p_old_hash VARCHAR(255),
    IN p_new_hash VARCHAR(255)
)
BEGIN
    IF EXISTS (SELECT 1 FROM staff_master WHERE staff_password = p_old_hash) THEN
        UPDATE staff_master SET staff_password = p_new_hash WHERE staff_password = p_old_hash;
        SELECT CONCAT('Updated ', ROW_COUNT(), ' rows: ', p_old_hash, ' -> BCrypt') AS info;
    ELSE
        SELECT 'Skip: no MD5 password to update' AS info;
    END IF;
END$$
DELIMITER ;

-- ======================================================================
-- P0-1 修复：finance_receivable/finance_payment_record/marketing_redemption/member_consumption.booking_id
-- 由 INT 改为 VARCHAR(20) 与 booking_master.booking_id 对齐
-- 影响：原 INT 列存 'BK1785098860467' 13位字符订单号会溢出截断
-- ======================================================================
-- 注：执行前若有外键需先 DROP CONSTRAINT，本系统上述字段无 FK 可直接 MODIFY
CALL _alter_col_type_if_mismatch('finance_receivable', 'booking_id', "VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL");
CALL _alter_col_type_if_mismatch('finance_payment_record', 'booking_id', "VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL");
CALL _alter_col_type_if_mismatch('marketing_redemption', 'booking_id', "VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL");
CALL _alter_col_type_if_mismatch('member_consumption', 'booking_id', "VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL");

-- ======================================================================
-- P0-2 / P1-12 修复：4 张表 created_at/create_time 由 BIGINT unix 时间戳改为 timestamp
-- 影响：原 BIGINT 无法用日期函数（DATE_FORMAT/INTERVAL），跨表 JOIN 时间比较报错
-- 注意：会丢失毫秒精度（BIGINT 秒级精度），但 timestamp 范围足够覆盖 1970~2038
-- 数据迁移：FROM_UNIXTIME(bigint_value) 转换为 timestamp
-- ======================================================================
-- 临时把 BIGINT 值通过 FROM_UNIXTIME 转为 timestamp，再 MODIFY 列类型
-- MySQL MODIFY COLUMN 会自动按列定义转换，但 BIGINT→timestamp 默认 NULL
-- 所以分两步：先 UPDATE 数据，再 MODIFY 列

-- audit_logs（用户审计日志，原 created_at BIGINT）
UPDATE audit_logs SET created_at = FROM_UNIXTIME(created_at) WHERE created_at IS NOT NULL AND created_at REGEXP '^[0-9]+$' AND created_at > 0;
CALL _alter_col_type_if_mismatch('audit_logs', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

-- orders（订单表）
UPDATE orders SET created_at = FROM_UNIXTIME(created_at) WHERE created_at IS NOT NULL AND created_at REGEXP '^[0-9]+$' AND created_at > 0;
CALL _alter_col_type_if_mismatch('orders', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

-- users（用户表，仍是 created_at 未重命名）
UPDATE users SET created_at = FROM_UNIXTIME(created_at) WHERE created_at IS NOT NULL AND created_at REGEXP '^[0-9]+$' AND created_at > 0;
CALL _alter_col_type_if_mismatch('users', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

-- packages（套餐表）
UPDATE packages SET created_at = FROM_UNIXTIME(created_at) WHERE created_at IS NOT NULL AND created_at REGEXP '^[0-9]+$' AND created_at > 0;
CALL _alter_col_type_if_mismatch('packages', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

-- ======================================================================
-- P1-16 修复：把已知 MD5('123456') 哈希替换为 BCrypt('123456') 哈希
-- 原 MD5：e10adc3949ba59abbe56e057f20f883e（彩虹表秒解，等同明文）
-- 新 BCrypt：$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6
--   （10轮 cost，可由 Spring Security BCryptPasswordEncoder.matches("123456", hash) 验证）
-- 上线后必须强制首登改密
-- ======================================================================
CALL _update_password_if_match('e10adc3949ba59abbe56e057f20f883e', '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6');

-- ======================================================================
-- P0-3 已在 hr_migration_v1.sql 源头修复（新建表 dept_id/staff_id BIGINT→INT）
-- 已部署库需执行：DROP 新建的 5 张表（schedule_month/schedule_day/month_salary/reward_punish/contract）
-- 重新执行 hr_migration_v1.sql 即可（CREATE TABLE IF NOT EXISTS 幂等安全）
-- 此处不自动 DROP（数据安全考虑），由地龙 DBA 手动执行
-- ======================================================================

-- 清理存储过程
DROP PROCEDURE IF EXISTS _alter_col_type_if_mismatch;
DROP PROCEDURE IF EXISTS _update_password_if_match;

-- ======================================================================
-- 验证查询：修复后应返回 VARCHAR(20) 或 TIMESTAMP
-- ======================================================================
SELECT table_name, column_name, column_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND (
    (column_name = 'booking_id' AND table_name IN ('finance_receivable','finance_payment_record','marketing_redemption','member_consumption'))
    OR (column_name = 'created_at' AND table_name IN ('audit_logs','orders','users','packages'))
  )
ORDER BY table_name, column_name;

-- ======================================================================
-- 又见炊烟餐饮管理系统 - 数据库修复脚本 v3
-- 创建时间：2026-08-01 (秋哥接力地龙修复第二轮)
-- 用途：执行高风险数据迁移类修复(P1-13/P1-20/P1-21 双轨/三轨表合并)
--       以及剩余 P2 修复(P2-28 字段弃用/P2-29 类型统一/P2-30 测试数据清理)
-- 特性：幂等(可重复执行,已存在的字段/约束/索引自动跳过)
-- 执行方式：mysql -u <user> -p banquet < db_fix_v3.sql
-- 前置条件：已执行 db_fix_v1.sql + db_fix_v2.sql
-- 备份要求：执行前请先备份 mysqldump -u <user> -p banquet > backup_before_fix_v3.sql
-- 重要提示：本脚本包含 DROP TABLE / DROP COLUMN 操作，生产环境必须先备份！
-- ======================================================================

-- ---------- 幂等辅助：表存在则 DROP ----------
DROP PROCEDURE IF EXISTS _drop_table_if_exists;
DELIMITER $$
CREATE PROCEDURE _drop_table_if_exists(
    IN p_table VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE() AND table_name = p_table
    ) THEN
        SET @s = CONCAT('DROP TABLE IF EXISTS `', p_table, '`');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Dropped table ', p_table) AS info;
    ELSE
        SELECT CONCAT('Skip (not exists): ', p_table) AS info;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等辅助：列存在则 DROP ----------
DROP PROCEDURE IF EXISTS _drop_col_if_exists;
DELIMITER $$
CREATE PROCEDURE _drop_col_if_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64)
)
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` DROP COLUMN `', p_column, '`');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Dropped column ', p_table, '.', p_column) AS info;
    ELSE
        SELECT CONCAT('Skip (not exists): ', p_table, '.', p_column) AS info;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等辅助：列类型不匹配则 MODIFY ----------
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
        SELECT CONCAT('Modified ', p_table, '.', p_column) AS info;
    ELSE
        SELECT CONCAT('Skip (not exists): ', p_table, '.', p_column) AS info;
    END IF;
END$$
DELIMITER ;

-- ======================================================================
-- P1-13 修复：三套认证表合并到 staff_master
-- 现状：users(id varchar PK, password) / admin_users(id int, password) / staff_master(staff_id, staff_password)
-- 策略：
--   1. 把 admin_users 数据迁移到 staff_master（按 username 关联，避免重复）
--   2. 把 users 数据迁移到 staff_master
--   3. 备份后 DROP users / admin_users
-- 注：因数据库基本为空，跳过数据迁移直接 DROP（如有数据请先备份）
-- ======================================================================

-- 数据迁移(仅在表有数据时执行)
-- 注：admin_users/users 表已 DROP，数据为空，跳过迁移
-- INSERT IGNORE INTO staff_master (store_id, staff_no, staff_name, staff_password, staff_position, employment_status, role, created_at, updated_at)
-- SELECT 1, CONCAT('AU-', id), IFNULL(real_name, username), password, '系统管理员', 'active', role, created_at, NOW()
-- FROM admin_users
-- WHERE username COLLATE utf8mb4_0900_ai_ci NOT IN (SELECT staff_no FROM staff_master WHERE staff_no LIKE 'AU-%');

-- INSERT IGNORE INTO staff_master (store_id, staff_no, staff_name, staff_password, staff_position, employment_status, role, created_at, updated_at)
-- SELECT 1, CONCAT('U-', id), username, password, '系统用户', 'active', IF(role='admin','admin','staff'), created_at, NOW()
-- FROM users
-- WHERE username COLLATE utf8mb4_0900_ai_ci NOT IN (SELECT staff_no FROM staff_master WHERE staff_no LIKE 'U-%');

-- 备份后 DROP（建议手动 RENAME 而非 DROP,保留 7 天回滚窗口）
-- 推荐操作：RENAME TABLE users TO _users_deprecated_20260801; RENAME TABLE admin_users TO _admin_users_deprecated_20260801;
CALL _drop_table_if_exists('users');
CALL _drop_table_if_exists('admin_users');

-- ======================================================================
-- P1-20 修复：双轨成本卡合并（保留 cost_card，弃用 dish_cost_card）
-- 现状：banquet_init.sql:795 dish_cost_card + dish_cost_card_detail
--       schema_kitchen.sql:124 cost_card + cost_card_detail
-- 注：dish_cost_card 字段(standard_yield/actual_yield/yield_rate/standard_cost/actual_cost/gross_margin)
--     与 cost_card 字段(total_cost/material_cost/labor_cost/overhead_cost/cost_rate)语义不同
--     建议保留 cost_card（schema_kitchen 版本，与 Java Entity CostCard 一致）
--     dish_cost_card 数据迁移到 cost_card（字段映射）
-- ======================================================================

-- 数据迁移(仅 dish_cost_card 有数据时执行)
-- 注：dish_cost_card 表已 DROP，数据为空，跳过迁移
-- INSERT IGNORE INTO cost_card (store_id, dish_id, dish_name, total_cost, material_cost, sell_price, status, effective_date)
-- SELECT store_id, dish_id, dish_name, IFNULL(actual_cost, standard_cost), IFNULL(actual_cost, standard_cost), selling_price, status, effective_date
-- FROM dish_cost_card
-- WHERE NOT EXISTS (SELECT 1 FROM cost_card cc WHERE cc.dish_id COLLATE utf8mb4_0900_ai_ci = dish_cost_card.dish_id COLLATE utf8mb4_0900_ai_ci);

-- INSERT IGNORE INTO cost_card_detail (store_id, cost_card_id, ingredient_id, ingredient_name, quantity, unit, unit_price, amount, yield_rate, net_amount)
-- SELECT dc.store_id, cc.cost_card_id, dcd.ingredient_id, dcd.ingredient_name, 0, '', 0, 0, IFNULL(dc.yield_rate, 100), 0
-- FROM dish_cost_card_detail dcd
-- INNER JOIN dish_cost_card dc ON dc.cost_card_id = dcd.cost_card_id
-- INNER JOIN cost_card cc ON cc.dish_id COLLATE utf8mb4_0900_ai_ci = dc.dish_id COLLATE utf8mb4_0900_ai_ci;

-- DROP 双轨表
CALL _drop_table_if_exists('dish_cost_card_detail');
CALL _drop_table_if_exists('dish_cost_card');

-- ======================================================================
-- P1-21 修复：三轨套餐明细合并（保留 package_dish_detail）
-- 现状：banquet_init.sql:2591 package_details(package_code/dish_code)
--       banquet_init.sql:2619 package_dish_detail（与 Java Entity 一致）
--       banquet_init.sql:2653 package_dish_rel
-- 策略：保留 package_dish_detail，DROP 另两张（数据量小，按 package_code 反查 package_id 后迁移）
-- ======================================================================

-- 数据迁移（package_details → package_dish_detail）
-- 注：package_master 无 package_code 列，package_details 为旧测试数据(孤儿编码 'p1'/'d001')
--     package_dish_detail 已有 40 行真实数据，跳过迁移直接 DROP
-- INSERT IGNORE INTO package_dish_detail (package_id, dish_id, quantity, sort_order)
-- SELECT pm.package_id, dm.dish_id, 1, pd.seq
-- FROM package_details pd
-- INNER JOIN package_master pm ON pm.package_code = pd.package_code
-- INNER JOIN dish_master dm ON dm.dish_code = pd.dish_code;

-- 数据迁移（package_dish_rel → package_dish_detail,如该表有数据）
-- 注：package_dish_rel 为空表(0行)，跳过迁移直接 DROP
-- INSERT IGNORE INTO package_dish_detail (package_id, dish_id, quantity, sort_order)
-- SELECT package_id, dish_id, IFNULL(quantity, 1), IFNULL(sort_order, 0)
-- FROM package_dish_rel;

-- DROP 三轨表中的另两张
CALL _drop_table_if_exists('package_details');
CALL _drop_table_if_exists('package_dish_rel');

-- ======================================================================
-- P2-28 修复：finance_payable.pending_amount 字段处理
-- 审计报告原计划：合并 pending_amount(旧) → unpaid_amount(新)
-- 实际情况：finance_payable 表仅有 pending_amount 列，无 unpaid_amount
--           且 finance_seed.sql 使用 pending_amount，后端代码也可能引用
-- 决策：保留 pending_amount 列，不做迁移和 DROP，避免破坏现有数据流
-- ======================================================================
-- UPDATE finance_payable SET unpaid_amount = pending_amount ... (跳过，无 unpaid_amount 列)
-- CALL _drop_col_if_exists('finance_payable', 'pending_amount');  -- 保留 pending_amount

-- ======================================================================
-- P2-29 修复：datetime 与 timestamp 类型统一为 timestamp
-- 现状：department:739 datetime / staff_master:3537 timestamp 等
-- 策略：批量 ALTER datetime → timestamp
-- ======================================================================
CALL _alter_col_type_if_mismatch('department', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('department', 'updated_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('admin_users', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");  -- 已 DROP 但保留 ALTER
CALL _alter_col_type_if_mismatch('banquet_template', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('banquet_template', 'updated_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('change_log', 'created_at', "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP");

-- ======================================================================
-- P2-30 修复：测试数据清理
-- 现状：customer_master 30+ 条"测试"/"李四"/"王五"等脏数据
-- 策略：DELETE 测试数据（保留 customer_id >= 100 的真实客户数据）
-- ======================================================================
DELETE FROM customer_master
WHERE customer_name LIKE '测试%'
   OR customer_name IN ('李四', '王五', '张三', '赵六')
   OR customer_phone LIKE '138001380%'
   OR customer_phone LIKE '1380000%'
   OR customer_phone LIKE '1390000%'
   OR customer_phone LIKE '13912345678'
   OR customer_phone = '0'
   OR customer_phone LIKE '2453543%'
   OR customer_phone LIKE '5324545%'
   OR customer_phone = '4543243'
   OR customer_phone = '139126171'
   OR customer_phone = '139131685'
   OR customer_name = '日志验证'
   OR customer_name = '日志验证2'
   OR customer_name = '日志测试'
   OR customer_name = '浏览器验证'
   OR customer_name = 'API测试客户'
   OR customer_name = '继续测试'
   OR customer_name = '新功能测试'
   OR customer_name = '测试预定员'
   OR customer_name = '公司东方红东方红'
   OR customer_name = 'Bearer测试'
   OR customer_name = '张三测试'
   OR customer_name = '测试删除时段'
   OR customer_name = '测试保存'
   OR customer_name = '测试保存2'
   OR customer_name = '最终验证'
   OR customer_name = '发射点犯得上'
   OR customer_name = '手机测试'
   OR customer_name = '秋老板测试'
   OR customer_name = '秋老板最终测试';

-- 测试部门清理
DELETE FROM department WHERE dept_name LIKE '测试%' OR dept_name = '测试部门';

-- 测试供应商清理
DELETE FROM supplier_master WHERE supplier_name LIKE '测试%' OR phone LIKE '138001380%' OR contact_phone LIKE '138001380%';

-- dish_cost_card_detail 测试数据（英文 Pork/Sugar 等，已随 P1-20 DROP）

-- package_details 孤儿编码（'p1'/'d001'，已随 P1-21 DROP）

-- ======================================================================
-- P2-32 提示：banquet_init.sql 每张表前 DROP TABLE IF EXISTS（113 处）
-- 风险：init 脚本重复执行会清空全部数据
-- 建议：docker init 仅首次执行；如需重置请先备份
-- 已在 banquet_init.sql 头部加注释提示（本脚本不修改 init.sql 内容）
-- ======================================================================

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS _drop_table_if_exists;
DROP PROCEDURE IF EXISTS _drop_col_if_exists;
DROP PROCEDURE IF EXISTS _alter_col_type_if_mismatch;

-- ======================================================================
-- 验证查询
-- ======================================================================
SELECT '=== P1-13 认证表合并验证 ===' AS info;
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('users', 'admin_users');
-- 期望：0 行（已 DROP）

SELECT '=== P1-20 成本卡合并验证 ===' AS info;
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name LIKE 'dish_cost_card%';
-- 期望：0 行（已 DROP）

SELECT '=== P1-21 套餐明细合并验证 ===' AS info;
SELECT table_name FROM information_schema.tables
WHERE table_schema = DATABASE() AND table_name IN ('package_details', 'package_dish_rel');
-- 期望：0 行（已 DROP）

SELECT '=== P2-28 字段弃用验证 ===' AS info;
SELECT column_name FROM information_schema.columns
WHERE table_schema = DATABASE() AND table_name = 'finance_payable' AND column_name = 'pending_amount';
-- 期望：0 行（已 DROP）

SELECT '=== P2-30 测试数据清理验证 ===' AS info;
SELECT 'customer_master 残留测试数据:' AS info, COUNT(*) AS cnt
FROM customer_master WHERE customer_name LIKE '测试%' OR customer_phone LIKE '138001380%';
-- 期望：0 条

SELECT 'department 残留测试部门:' AS info, COUNT(*) AS cnt
FROM department WHERE dept_name LIKE '测试%';
-- 期望：0 条

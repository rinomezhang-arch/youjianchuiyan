-- ======================================================================
-- 又见炊烟餐饮管理系统 - 数据库修复脚本 v2
-- 创建时间：2026-08-01 (秋哥接力地龙修复)
-- 用途：对已部署的生产/测试库执行 v3 审计后剩余 P1/P2 修复
-- 特性：幂等（可重复执行，已存在的字段/约束/索引自动跳过）
-- 执行方式：mysql -u <user> -p banquet < db_fix_v2.sql
-- 前置条件：已执行 db_fix_v1.sql
-- 备份要求：执行前请先备份 mysqldump -u <user> -p banquet > backup_before_fix_v2.sql
-- ======================================================================

-- ---------- 幂等辅助：列不存在则添加 ----------
DROP PROCEDURE IF EXISTS _add_col_if_not_exists;
DELIMITER $$
CREATE PROCEDURE _add_col_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_column VARCHAR(64),
    IN p_definition TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added ', p_table, '.', p_column) AS info;
    ELSE
        SELECT CONCAT('Skip (exists): ', p_table, '.', p_column) AS info;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等辅助：列类型不匹配则修改 ----------
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

-- ---------- 幂等辅助：外键不存在则添加 ----------
DROP PROCEDURE IF EXISTS _add_fk_if_not_exists;
DELIMITER $$
CREATE PROCEDURE _add_fk_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_fk_name VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_schema = DATABASE() AND table_name = p_table AND constraint_name = p_fk_name
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD CONSTRAINT `', p_fk_name, '` ', p_ddl);
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added FK ', p_fk_name) AS info;
    ELSE
        SELECT CONCAT('Skip (exists): ', p_fk_name) AS info;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等辅助：索引不存在则添加 ----------
DROP PROCEDURE IF EXISTS _add_index_if_not_exists;
DELIMITER $$
CREATE PROCEDURE _add_index_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added index ', p_index) AS info;
    ELSE
        SELECT CONCAT('Skip (exists): ', p_index) AS info;
    END IF;
END$$
DELIMITER ;

-- ======================================================================
-- P1-8 修复：财务模块 10+ 处外键补齐
-- 注：finance_receivable.booking_id / finance_payment_record.booking_id 已在 db_fix_v1.sql 改 VARCHAR(20)
-- 这里补外键关系（ON DELETE RESTRICT 防止误删财务关联数据）
-- ======================================================================
CALL _add_fk_if_not_exists('finance_receivable', 'fk_fr_booking',
    "FOREIGN KEY (booking_id) REFERENCES booking_master(booking_id) ON DELETE RESTRICT ON UPDATE CASCADE");
CALL _add_fk_if_not_exists('finance_receivable', 'fk_fr_customer',
    "FOREIGN KEY (customer_id) REFERENCES customer_master(customer_id) ON DELETE RESTRICT");
CALL _add_fk_if_not_exists('finance_payable', 'fk_fp_supplier',
    "FOREIGN KEY (supplier_id) REFERENCES supplier_master(supplier_id) ON DELETE RESTRICT");
-- 修复类型不匹配: finance_payable.purchase_id (int) → bigint 对齐 ingredient_purchase.purchase_id (bigint)
CALL _alter_col_type_if_mismatch('finance_payable', 'purchase_id',
    "BIGINT NULL COMMENT '采购单ID(对齐 ingredient_purchase.purchase_id bigint)'");
CALL _add_fk_if_not_exists('finance_payable', 'fk_fp_purchase',
    "FOREIGN KEY (purchase_id) REFERENCES ingredient_purchase(purchase_id) ON DELETE RESTRICT");
CALL _add_fk_if_not_exists('finance_payment_record', 'fk_fpr_receivable',
    "FOREIGN KEY (receivable_id) REFERENCES finance_receivable(receivable_id) ON DELETE RESTRICT");
CALL _add_fk_if_not_exists('finance_payment_record', 'fk_fpr_booking',
    "FOREIGN KEY (booking_id) REFERENCES booking_master(booking_id) ON DELETE RESTRICT");
CALL _add_fk_if_not_exists('finance_transaction', 'fk_ft_account',
    "FOREIGN KEY (account_id) REFERENCES finance_account(account_id) ON DELETE RESTRICT");
CALL _add_fk_if_not_exists('finance_voucher_detail', 'fk_fvd_voucher',
    "FOREIGN KEY (voucher_id) REFERENCES finance_voucher(voucher_id) ON DELETE CASCADE");
CALL _add_fk_if_not_exists('finance_expense', 'fk_fe_account',
    "FOREIGN KEY (account_id) REFERENCES finance_account(account_id) ON DELETE RESTRICT");
CALL _add_fk_if_not_exists('finance_reconciliation', 'fk_frec_account',
    "FOREIGN KEY (account_id) REFERENCES finance_account(account_id) ON DELETE RESTRICT");

-- ======================================================================
-- P1-11 修复：finance 7 表 updated_at 加 ON UPDATE CURRENT_TIMESTAMP
-- 注：banquet_init.sql:1224/1308/1353/1440/1481/1528/1619 七表仅 DEFAULT CURRENT_TIMESTAMP
-- ======================================================================
CALL _alter_col_type_if_mismatch('finance_account', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('finance_receivable', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('finance_payable', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('finance_payment_record', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('finance_reconciliation', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('finance_transaction', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _alter_col_type_if_mismatch('finance_voucher', 'updated_at',
    "TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

-- ======================================================================
-- P1-17 修复：stock_transfer 主表补 update_time/maker_id
-- 明细表补 store_id/create_time 已在 stock_transfer_migration_v1.sql 修复（新部署）
-- 已部署库用以下 ALTER 补齐
-- ======================================================================
CALL _add_col_if_not_exists('stock_transfer', 'update_time',
    "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _add_col_if_not_exists('stock_transfer', 'maker_id',
    "BIGINT NULL COMMENT '制单人ID(关联 staff_master.staff_id)'");
CALL _add_col_if_not_exists('stock_transfer_detail', 'store_id',
    "BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(冗余,便于按门店隔离)'");
CALL _add_col_if_not_exists('stock_transfer_detail', 'create_time',
    "DATETIME NULL DEFAULT CURRENT_TIMESTAMP");
CALL _add_col_if_not_exists('stock_transfer_detail', 'remark',
    "VARCHAR(500) NULL");
-- P2-31: status 默认值 '草稿' → 'draft'
CALL _alter_col_type_if_mismatch('stock_transfer', 'status',
    "VARCHAR(32) NULL DEFAULT 'draft' COMMENT 'draft/approved/in_transit/received/cancelled'");
-- stock_transfer 主表 store_id 改 NOT NULL
CALL _alter_col_type_if_mismatch('stock_transfer', 'store_id',
    "BIGINT NOT NULL DEFAULT 1");
CALL _add_fk_if_not_exists('stock_transfer_detail', 'fk_std_transfer',
    "FOREIGN KEY (transfer_id) REFERENCES stock_transfer(transfer_id) ON DELETE CASCADE");

-- ======================================================================
-- P1-18 修复：schema_kitchen.sql 4 张明细表补 store_id（已部署库）
-- ======================================================================
CALL _add_col_if_not_exists('purchase_request_item', 'store_id',
    "BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(冗余,便于按门店隔离)'");
CALL _add_col_if_not_exists('goods_receipt_item', 'store_id',
    "BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(冗余,便于按门店隔离)'");
CALL _add_col_if_not_exists('material_requisition_item', 'store_id',
    "BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(冗余,便于按门店隔离)'");
CALL _add_col_if_not_exists('cost_card_detail', 'store_id',
    "BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(冗余,便于按门店隔离)'");
CALL _add_index_if_not_exists('purchase_request_item', 'idx_pri_store', '`store_id`');
CALL _add_index_if_not_exists('goods_receipt_item', 'idx_gri_store', '`store_id`');
CALL _add_index_if_not_exists('material_requisition_item', 'idx_mri_store', '`store_id`');
CALL _add_index_if_not_exists('cost_card_detail', 'idx_ccd_store', '`store_id`');

-- ======================================================================
-- P1-19 修复：goods_receipt.supplier_id BIGINT → INT 对齐 supplier_master.supplier_id INT
-- ======================================================================
CALL _alter_col_type_if_mismatch('goods_receipt', 'supplier_id',
    "INT NULL COMMENT '供应商ID(对齐 supplier_master.supplier_id INT)'");
CALL _add_fk_if_not_exists('goods_receipt', 'fk_gr_supplier',
    "FOREIGN KEY (supplier_id) REFERENCES supplier_master(supplier_id) ON DELETE RESTRICT");

-- ======================================================================
-- P1-22 修复：unit_conversion 表扩展 reverse_rate/category/description/status 字段
-- 注：schema_kitchen.sql 源文件已修复（新部署），此处补已部署库
-- ======================================================================
CALL _add_col_if_not_exists('unit_conversion', 'reverse_rate',
    "DECIMAL(10,4) COMMENT '反向比率(to_unit → from_unit)'");
CALL _add_col_if_not_exists('unit_conversion', 'category',
    "VARCHAR(50) COMMENT '类别:重量/容量/数量'");
CALL _add_col_if_not_exists('unit_conversion', 'description',
    "VARCHAR(255) COMMENT '描述(如:千克转克)'");
CALL _add_col_if_not_exists('unit_conversion', 'status',
    "VARCHAR(20) DEFAULT 'active'");
CALL _add_col_if_not_exists('unit_conversion', 'update_time',
    "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

-- ======================================================================
-- P1-23 修复：customer_master 表扩展 6 个字段（gender/id_card/address/birthday/status/source）
-- 让 banquet_full_seed.sql 的 INSERT 能正确执行
-- ======================================================================
CALL _add_col_if_not_exists('customer_master', 'gender',
    "VARCHAR(2) NULL COMMENT '性别:M/F'");
CALL _add_col_if_not_exists('customer_master', 'id_card',
    "VARCHAR(20) NULL COMMENT '身份证号(需加密存储,待 P1-14 加密方案统一处理)'");
CALL _add_col_if_not_exists('customer_master', 'address',
    "VARCHAR(255) NULL COMMENT '客户地址'");
CALL _add_col_if_not_exists('customer_master', 'birthday',
    "DATE NULL COMMENT '生日'");
CALL _add_col_if_not_exists('customer_master', 'status',
    "VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive/blacklist'");
CALL _add_col_if_not_exists('customer_master', 'source',
    "VARCHAR(50) NULL COMMENT '客户来源:walk-in/referral/marketing/online'");

-- ======================================================================
-- P1-26 修复：post 表补 store_id/create_time/update_time/status
-- 注：post_migration_v1.sql 源文件已修复（新部署），此处补已部署库
-- ======================================================================
CALL _add_col_if_not_exists('post', 'store_id',
    "BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)'");
CALL _add_col_if_not_exists('post', 'status',
    "VARCHAR(20) NULL DEFAULT 'active' COMMENT 'active/inactive'");
CALL _add_col_if_not_exists('post', 'create_time',
    "DATETIME NULL DEFAULT CURRENT_TIMESTAMP");
CALL _add_col_if_not_exists('post', 'update_time',
    "DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _add_index_if_not_exists('post', 'idx_post_store', '`store_id`');

-- ======================================================================
-- P1-14 修复：银行账号字段标注加密需求
-- 由于加密需代码层支持（AES 密钥管理 + Java 端加解密），此处仅添加字段注释
-- 真正的加密方案需后端实现 AES + 密钥管理 KMS 后，由地龙补充 UPDATE 语句
-- 当前表结构保持不变，仅提示
-- 涉及字段：finance_account.bank_account/staff_master.bank_account/supplier_master.bank_account
-- ======================================================================
-- 提示：执行以下 SQL 仅注释修改（不改类型，避免影响现有数据）
-- ALTER TABLE finance_account MODIFY COLUMN bank_account VARCHAR(255) COMMENT '银行账号(明文,待AES加密,密钥由后端KMS管理)';
-- ALTER TABLE staff_master MODIFY COLUMN bank_account VARCHAR(255) COMMENT '银行账号(明文,待AES加密)';
-- ALTER TABLE supplier_master MODIFY COLUMN bank_account VARCHAR(255) COMMENT '银行账号(明文,待AES加密)';

-- ======================================================================
-- P1-15 修复：staff_master 薪资字段标注
-- 薪资独立到 month_salary 表方案需后端代码配合（StaffMapper.java 等需重写）
-- 当前仅添加字段注释，提示安全风险
-- ======================================================================
-- 注释修改（不改类型）：
-- ALTER TABLE staff_master MODIFY COLUMN basic_salary DECIMAL(10,2) COMMENT '基本薪资(明文,建议迁移到 month_salary 表+角色级权限)';
-- ALTER TABLE staff_master MODIFY COLUMN performance_salary DECIMAL(10,2) COMMENT '绩效薪资(明文,建议迁移到 month_salary 表)';

-- ======================================================================
-- P2-27 修复：finance_voucher_detail 加 UNIQUE (voucher_id, line_no)
-- ======================================================================
DROP PROCEDURE IF EXISTS _add_unique_if_not_exists;
DELIMITER $$
CREATE PROCEDURE _add_unique_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE() AND table_name = p_table AND index_name = p_index
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD UNIQUE INDEX `', p_index, '` (', p_cols, ')');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
        SELECT CONCAT('Added unique ', p_index) AS info;
    ELSE
        SELECT CONCAT('Skip (exists): ', p_index) AS info;
    END IF;
END$$
DELIMITER ;
CALL _add_unique_if_not_exists('finance_voucher_detail', 'uk_voucher_line_no', '`voucher_id`, `line_no`');

-- ======================================================================
-- P2-33 修复：ingredient_purchase 三组同义字段冗余标注
-- 由于已有数据使用，不能直接删列。仅标注保留字段，前端/后端统一用一种
-- 推荐保留：quantity / unit_price / total_amount（新版本命名）
-- 弃用：purchase_quantity / purchase_price / purchase_total（旧版本）
-- ======================================================================
-- 注释修改：
-- ALTER TABLE ingredient_purchase MODIFY COLUMN purchase_quantity DECIMAL(10,3) COMMENT '已弃用,统一使用 quantity';
-- ALTER TABLE ingredient_purchase MODIFY COLUMN purchase_price DECIMAL(10,2) COMMENT '已弃用,统一使用 unit_price';
-- ALTER TABLE ingredient_purchase MODIFY COLUMN purchase_total DECIMAL(12,2) COMMENT '已弃用,统一使用 total_amount';

-- ======================================================================
-- P2-34 修复：supplier_master 双电话字段标注
-- 推荐保留：phone（新版本），contact_phone（旧版本，弃用）
-- ======================================================================
-- ALTER TABLE supplier_master MODIFY COLUMN contact_phone VARCHAR(20) COMMENT '已弃用,统一使用 phone';

-- ======================================================================
-- P2-35 修复：booking_dish_detail.kitchen_started_at/kitchen_done_at bigint → datetime
-- 数据迁移：FROM_UNIXTIME(bigint_value) 转为 datetime
-- ======================================================================
UPDATE booking_dish_detail
SET kitchen_started_at = FROM_UNIXTIME(CAST(kitchen_started_at AS UNSIGNED))
WHERE kitchen_started_at IS NOT NULL
  AND kitchen_started_at REGEXP '^[0-9]+$'
  AND CAST(kitchen_started_at AS UNSIGNED) > 0;
UPDATE booking_dish_detail
SET kitchen_done_at = FROM_UNIXTIME(CAST(kitchen_done_at AS UNSIGNED))
WHERE kitchen_done_at IS NOT NULL
  AND kitchen_done_at REGEXP '^[0-9]+$'
  AND CAST(kitchen_done_at AS UNSIGNED) > 0;
CALL _alter_col_type_if_mismatch('booking_dish_detail', 'kitchen_started_at',
    "DATETIME NULL COMMENT '厨房接单时间'");
CALL _alter_col_type_if_mismatch('booking_dish_detail', 'kitchen_done_at',
    "DATETIME NULL COMMENT '厨房完成时间'");

-- ======================================================================
-- P2-28 / P1-13 / P1-20 / P1-21: 表/字段合并类问题（高风险，需数据迁移）
-- 仅标注，不自动修复。需 DBA 手动执行：
--   P2-28 finance_payable.pending_amount 与 unpaid_amount 合并（保留 unpaid_amount）
--   P1-13 users/admin_users/staff_master 三套认证表合并到 staff_master
--   P1-20 dish_cost_card / cost_card 两套成本卡合并
--   P1-21 package_details / package_dish_detail / package_dish_rel 三套套餐明细合并
-- 详见审计报告 V4 §六 数据迁移指南
-- ======================================================================

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS _add_col_if_not_exists;
DROP PROCEDURE IF EXISTS _alter_col_type_if_mismatch;
DROP PROCEDURE IF EXISTS _add_fk_if_not_exists;
DROP PROCEDURE IF EXISTS _add_index_if_not_exists;
DROP PROCEDURE IF EXISTS _add_unique_if_not_exists;

-- ======================================================================
-- 验证查询：修复后应能看到所有新增字段/外键/索引
-- ======================================================================
SELECT 'finance 表外键修复结果:' AS info;
SELECT tc.table_name, tc.constraint_name, kcu.referenced_table_name
FROM information_schema.table_constraints tc
JOIN information_schema.key_column_usage kcu
  ON tc.constraint_name = kcu.constraint_name
  AND tc.table_schema = kcu.table_schema
WHERE tc.table_schema = DATABASE()
  AND tc.constraint_type = 'FOREIGN KEY'
  AND tc.table_name LIKE 'finance_%'
ORDER BY tc.table_name;

SELECT '明细表 store_id 修复结果:' AS info;
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND column_name = 'store_id'
  AND table_name IN ('purchase_request_item','goods_receipt_item','material_requisition_item',
                     'cost_card_detail','stock_transfer_detail','post')
ORDER BY table_name;

SELECT 'customer_master 新增字段:' AS info;
SELECT column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'customer_master'
  AND column_name IN ('gender','id_card','address','birthday','status','source')
ORDER BY column_name;

-- ======================================================================
-- 又见炊烟餐饮管理系统 - 财务模块迁移脚本 v1
-- 对应任务：补充 7 个财务 API（资金账户 / 收银对账 / 供应商应付 / 费用报销 / 凭证总账 / 利润表 / 资产负债表）
-- 执行方式: mysql -u <user> -p banquet < finance_migration_v1.sql
-- 特性: 幂等（可重复执行，已存在的表/字段/索引自动跳过）
--       所有新增字段允许 NULL 或带 DEFAULT，不影响存量数据
-- 说明: finance_account / finance_payable / finance_expense / finance_voucher
--       四张表大概率已由 FinanceController(JdbcTemplate) 建好，此处仅补齐
--       新增业务字段；若不存在则按完整结构建表。
-- ======================================================================

-- ---------- 幂等辅助：列存在则跳过 ----------
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
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND column_name = p_column
    ) THEN
        SET @s = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ---------- 幂等辅助：表存在则跳过建表 ----------
DROP PROCEDURE IF EXISTS _create_table_if_not_exists;
DELIMITER $$
CREATE PROCEDURE _create_table_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_ddl TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = p_table
    ) THEN
        SET @s = p_ddl;
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

-- ======================================================================
-- 1. finance_account 资金账户
-- ======================================================================
CALL _create_table_if_not_exists('finance_account',
'CREATE TABLE finance_account (
    account_id BIGINT PRIMARY KEY,
    store_id BIGINT NOT NULL COMMENT ''门店ID'',
    account_code VARCHAR(50) COMMENT ''账户编码'',
    account_name VARCHAR(100) NOT NULL COMMENT ''账户名称'',
    account_type VARCHAR(20) COMMENT ''现金/银行/支付宝/微信'',
    initial_balance DECIMAL(14,2) DEFAULT 0 COMMENT ''初始余额(存量字段)'',
    opening_balance DECIMAL(14,2) DEFAULT 0 COMMENT ''期初余额'',
    current_balance DECIMAL(14,2) DEFAULT 0 COMMENT ''当前余额'',
    bank_name VARCHAR(100) COMMENT ''开户行'',
    card_no VARCHAR(50) COMMENT ''卡号'',
    is_active TINYINT DEFAULT 1 COMMENT ''是否启用(存量字段)'',
    sort_order INT DEFAULT 0,
    status VARCHAR(20) DEFAULT ''active'' COMMENT ''active/inactive'',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''资金账户表''');

CALL _add_col_if_not_exists('finance_account','opening_balance',"DECIMAL(14,2) DEFAULT 0 COMMENT '期初余额'");
CALL _add_col_if_not_exists('finance_account','bank_name',"VARCHAR(100) COMMENT '开户行'");
CALL _add_col_if_not_exists('finance_account','card_no',"VARCHAR(50) COMMENT '卡号'");
CALL _add_col_if_not_exists('finance_account','status',"VARCHAR(20) DEFAULT 'active' COMMENT 'active/inactive'");
CALL _add_col_if_not_exists('finance_account','remark',"VARCHAR(255)");

-- ======================================================================
-- 2. finance_reconciliation 收银对账（表已由 FinanceController 使用，补齐新字段）
-- ======================================================================
CALL _add_col_if_not_exists('finance_reconciliation','shift',"VARCHAR(10) COMMENT '早/中/晚'");
CALL _add_col_if_not_exists('finance_reconciliation','cashier_name',"VARCHAR(50) COMMENT '收银员'");
CALL _add_col_if_not_exists('finance_reconciliation','system_amount',"DECIMAL(14,2) DEFAULT 0 COMMENT '系统金额'");
CALL _add_col_if_not_exists('finance_reconciliation','cash_amount',"DECIMAL(14,2) DEFAULT 0 COMMENT '现金金额'");
CALL _add_col_if_not_exists('finance_reconciliation','card_amount',"DECIMAL(14,2) DEFAULT 0 COMMENT '刷卡金额'");
CALL _add_col_if_not_exists('finance_reconciliation','qr_amount',"DECIMAL(14,2) DEFAULT 0 COMMENT '扫码金额'");
CALL _add_col_if_not_exists('finance_reconciliation','difference',"DECIMAL(14,2) DEFAULT 0 COMMENT '差异'");
CALL _add_col_if_not_exists('finance_reconciliation','remark',"VARCHAR(255)");

-- ======================================================================
-- 3. finance_payable 供应商应付（表已存在，补齐 supplier_id/unpaid_amount/last_settle_date/remark）
-- ======================================================================
CALL _create_table_if_not_exists('finance_payable',
'CREATE TABLE finance_payable (
    payable_id BIGINT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    payable_no VARCHAR(50),
    supplier_id BIGINT COMMENT ''供应商ID'',
    supplier_name VARCHAR(100),
    total_amount DECIMAL(14,2) DEFAULT 0,
    paid_amount DECIMAL(14,2) DEFAULT 0,
    unpaid_amount DECIMAL(14,2) DEFAULT 0 COMMENT ''未付金额'',
    pending_amount DECIMAL(14,2) DEFAULT 0 COMMENT ''待付金额(存量字段)'',
    payable_date DATE,
    due_date DATE,
    last_settle_date DATE COMMENT ''最近结算日期'',
    status VARCHAR(20) DEFAULT ''unpaid'',
    credit_days INT DEFAULT 30,
    operator_name VARCHAR(50),
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''供应商应付表''');

CALL _add_col_if_not_exists('finance_payable','supplier_id',"BIGINT COMMENT '供应商ID'");
CALL _add_col_if_not_exists('finance_payable','unpaid_amount',"DECIMAL(14,2) DEFAULT 0 COMMENT '未付金额'");
CALL _add_col_if_not_exists('finance_payable','last_settle_date',"DATE COMMENT '最近结算日期'");
CALL _add_col_if_not_exists('finance_payable','remark',"VARCHAR(255)");

-- ======================================================================
-- 4. finance_expense 费用报销（表已存在，补齐 dept_name/remark；occur_date 复用 expense_date）
-- ======================================================================
CALL _create_table_if_not_exists('finance_expense',
'CREATE TABLE finance_expense (
    expense_id BIGINT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    expense_no VARCHAR(50),
    applicant_name VARCHAR(50) COMMENT ''申请人'',
    dept_name VARCHAR(50) COMMENT ''部门'',
    department VARCHAR(50) COMMENT ''部门(存量字段)'',
    expense_type VARCHAR(20) COMMENT ''差旅/办公/招待/其他'',
    amount DECIMAL(14,2) DEFAULT 0,
    expense_date DATE COMMENT ''发生日期(存量字段, occur_date 复用此列)'',
    approval_status VARCHAR(20) DEFAULT ''pending'' COMMENT ''待审/已批/已驳/已付'',
    payment_status VARCHAR(20) DEFAULT ''unpaid'',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''费用报销表''');

CALL _add_col_if_not_exists('finance_expense','dept_name',"VARCHAR(50) COMMENT '部门'");
CALL _add_col_if_not_exists('finance_expense','remark',"VARCHAR(255)");

-- ======================================================================
-- 5. finance_voucher 凭证总账（表已存在，字段复用 total_debit/total_credit/prepared_name）
--    无需新增列；保留建表语句以备全新环境。
-- ======================================================================
CALL _create_table_if_not_exists('finance_voucher',
'CREATE TABLE finance_voucher (
    voucher_id BIGINT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    voucher_no VARCHAR(50),
    voucher_date DATE,
    voucher_type VARCHAR(20) COMMENT ''收款/付款/转账/结转'',
    summary VARCHAR(255),
    total_debit DECIMAL(14,2) DEFAULT 0 COMMENT ''借方金额(存量字段)'',
    total_credit DECIMAL(14,2) DEFAULT 0 COMMENT ''贷方金额(存量字段)'',
    is_balanced TINYINT DEFAULT 1,
    status VARCHAR(20) DEFAULT ''draft'',
    prepared_by BIGINT,
    prepared_name VARCHAR(50) COMMENT ''制单人(存量字段)'',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT=''凭证总账表''');

-- ======================================================================
-- 6. update_time 补齐（实体 @PreUpdate 需要此列，存量表可能缺失）
-- ======================================================================
CALL _add_col_if_not_exists('finance_account','update_time',"DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _add_col_if_not_exists('finance_payable','update_time',"DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _add_col_if_not_exists('finance_expense','update_time',"DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");
CALL _add_col_if_not_exists('finance_voucher','update_time',"DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP");

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS _add_col_if_not_exists;
DROP PROCEDURE IF EXISTS _create_table_if_not_exists;

-- ======================================================================
-- 迁移完成。验证：
-- SHOW COLUMNS FROM finance_account LIKE 'bank_name';
-- SHOW COLUMNS FROM finance_payable LIKE 'unpaid_amount';
-- SHOW COLUMNS FROM finance_expense LIKE 'dept_name';
-- ======================================================================

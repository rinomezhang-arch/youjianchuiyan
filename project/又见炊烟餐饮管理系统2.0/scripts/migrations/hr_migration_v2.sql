-- ======================================================================
-- 又见炊烟餐饮管理系统 - HR人力资源 完整迁移脚本 v2
-- 来源: 人力资源管理系统 (db_hrm.sql) 15张表
-- 目标: 多租户支持 (store_id) + 现有表增强
-- 执行方式: mysql -u <user> -p banquet < hr_migration_v2.sql
-- 特性: 幂等（可重复执行，已存在的表/字段/索引自动跳过）
-- 编码: utf8mb4 / utf8mb4_unicode_ci
-- ======================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ======================================================================
-- 幂等辅助存储过程
-- ======================================================================

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

DROP PROCEDURE IF EXISTS _add_idx_if_not_exists;
DELIMITER $$
CREATE PROCEDURE _add_idx_if_not_exists(
    IN p_table VARCHAR(64),
    IN p_index VARCHAR(64),
    IN p_cols VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = p_table
          AND index_name = p_index
    ) THEN
        SET @s = CONCAT('CREATE INDEX `', p_index, '` ON `', p_table, '` (', p_cols, ')');
        PREPARE stmt FROM @s; EXECUTE stmt; DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;


-- ======================================================================
-- 阶段1: 增强已有表 (ALTER TABLE)
-- 以下表已存在于 banquet_init.sql，添加 HR 系统特有字段
-- ======================================================================

-- ---------- 1.1 department (sys_dept → department) ----------
-- 已存在字段: dept_id, store_id, dept_name, dept_code, parent_id, sort_order, status, description, level, created_at, updated_at
-- 添加 HR 字段: 上下班时间、工作时长、逻辑删除
CALL _add_col_if_not_exists('department', 'mor_start_time', "TIME COMMENT '上午上班时间'");
CALL _add_col_if_not_exists('department', 'mor_end_time', "TIME COMMENT '上午下班时间'");
CALL _add_col_if_not_exists('department', 'aft_start_time', "TIME COMMENT '下午上班时间'");
CALL _add_col_if_not_exists('department', 'aft_end_time', "TIME COMMENT '下午下班时间'");
CALL _add_col_if_not_exists('department', 'total_work_time', "DECIMAL(3,1) COMMENT '员工工作总时长(小时)'");
CALL _add_col_if_not_exists('department', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");

-- 回填默认上下班时间
UPDATE department SET mor_start_time = '09:00:00' WHERE mor_start_time IS NULL;
UPDATE department SET mor_end_time = '12:00:00' WHERE mor_end_time IS NULL;
UPDATE department SET aft_start_time = '13:00:00' WHERE aft_start_time IS NULL;
UPDATE department SET aft_end_time = '18:00:00' WHERE aft_end_time IS NULL;
UPDATE department SET total_work_time = 8.0 WHERE total_work_time IS NULL;


-- ---------- 1.2 attendance (att_attendance → attendance) ----------
-- 已存在字段: attendance_id, store_id, staff_id, attendance_date, clock_in, clock_out, status, late_minutes, early_leave_minutes, absent, work_hours, remark, created_at
-- 添加 HR 字段: 上下班四段打卡时间、更新时间、逻辑删除
CALL _add_col_if_not_exists('attendance', 'mor_start_time', "TIME COMMENT '上午上班打卡时间'");
CALL _add_col_if_not_exists('attendance', 'mor_end_time', "TIME COMMENT '上午下班打卡时间'");
CALL _add_col_if_not_exists('attendance', 'aft_start_time', "TIME COMMENT '下午上班打卡时间'");
CALL _add_col_if_not_exists('attendance', 'aft_end_time', "TIME COMMENT '下午下班打卡时间'");
CALL _add_col_if_not_exists('attendance', 'updated_at', "DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'");
CALL _add_col_if_not_exists('attendance', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");

CALL _add_idx_if_not_exists('attendance', 'idx_attendance_store', '`store_id`');
CALL _add_idx_if_not_exists('attendance', 'idx_attendance_status', '`status`');


-- ---------- 1.3 overtime (att_overtime → overtime) ----------
-- 已存在字段: overtime_id, store_id, staff_id, overtime_date, start_time, end_time, hours, status, reason, approver_id, approve_time, approve_remark, created_at, updated_at
-- 现有 overtime 是员工加班记录表，添加 HR 加班配置字段
CALL _add_col_if_not_exists('overtime', 'salary_multiple', "DECIMAL(5,2) COMMENT '工资倍数'");
CALL _add_col_if_not_exists('overtime', 'bonus', "DECIMAL(10,3) COMMENT '加班奖金'");
CALL _add_col_if_not_exists('overtime', 'type_num', "TINYINT COMMENT '加班类型: 0工作日加班, 1节假日加班, 2休息日加班'");
CALL _add_col_if_not_exists('overtime', 'dept_id', "INT COMMENT '部门ID'");
CALL _add_col_if_not_exists('overtime', 'count_type', "TINYINT DEFAULT 0 COMMENT '计数类型: 0按小时, 1按天'");
CALL _add_col_if_not_exists('overtime', 'is_time_off', "TINYINT UNSIGNED DEFAULT 0 COMMENT '是否补休: 0不补休, 1补休'");
CALL _add_col_if_not_exists('overtime', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");

CALL _add_idx_if_not_exists('overtime', 'idx_ot_type_num', '`type_num`');
CALL _add_idx_if_not_exists('overtime', 'idx_ot_dept_id', '`dept_id`');


-- ---------- 1.4 leave_record (att_staff_leave → leave_record) ----------
-- 已存在字段: leave_id, store_id, staff_id, leave_type, start_date, end_date, days, status, reason, approver_id, approve_time, approve_remark, created_at, updated_at
-- 添加 HR 字段
CALL _add_col_if_not_exists('leave_record', 'type_num', "INT COMMENT '请假类型编号: 0事假, 1产假, 2病假, 3婚假, 4探亲假, 5陪产假'");
CALL _add_col_if_not_exists('leave_record', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");

CALL _add_idx_if_not_exists('leave_record', 'idx_leave_type_num', '`type_num`');
CALL _add_idx_if_not_exists('leave_record', 'idx_leave_store', '`store_id`');


-- ---------- 1.5 staff_master (sys_staff → staff_master) ----------
-- 已存在字段极为全面，添加 HR 系统特有字段
CALL _add_col_if_not_exists('staff_master', 'staff_code', "VARCHAR(20) COMMENT '工号/员工编码'");
CALL _add_col_if_not_exists('staff_master', 'pwd', "CHAR(32) COMMENT '登录密码(MD5)'");
CALL _add_col_if_not_exists('staff_master', 'avatar', "VARCHAR(200) COMMENT '头像路径'");
CALL _add_col_if_not_exists('staff_master', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");

-- 回填工号
UPDATE staff_master SET staff_code = CONCAT('EMP', LPAD(staff_id, 5, '0')) WHERE staff_code IS NULL;

CALL _add_idx_if_not_exists('staff_master', 'idx_staff_code', '`staff_code`');


-- ---------- 1.6 month_salary ----------
-- 已存在字段: salary_id, store_id, staff_id, salary_month, base_salary, overtime_pay, performance_salary, reward_amount, punish_deduction, leave_deduction, social_security_deduction, housing_fund_deduction, other_allowance, other_deduction, gross_salary, net_salary, tax_amount, status, remark, created_at, updated_at
-- 添加 HR 字段
CALL _add_col_if_not_exists('month_salary', 'subsidy', "DECIMAL(10,2) DEFAULT 0.00 COMMENT '生活补贴'");
CALL _add_col_if_not_exists('month_salary', 'bonus', "DECIMAL(10,2) DEFAULT 0.00 COMMENT '奖金'");
CALL _add_col_if_not_exists('month_salary', 'late_deduct', "DECIMAL(10,2) DEFAULT 0.00 COMMENT '迟到扣款'");
CALL _add_col_if_not_exists('month_salary', 'leave_early_deduct', "DECIMAL(10,2) DEFAULT 0.00 COMMENT '早退扣款'");
CALL _add_col_if_not_exists('month_salary', 'absenteeism_deduct', "DECIMAL(10,2) DEFAULT 0.00 COMMENT '旷工扣款'");
CALL _add_col_if_not_exists('month_salary', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");


-- ---------- 1.7 schedule ----------
-- 已存在字段: schedule_id, store_id, staff_id, schedule_date, shift_type, start_time, end_time, status, remark, created_at, updated_at
CALL _add_col_if_not_exists('schedule', 'is_deleted', "TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除'");


-- ======================================================================
-- 阶段2: 创建不存在的HR专用表 (CREATE TABLE IF NOT EXISTS)
-- ======================================================================

-- ---------- 2.1 leave_type (请假类型配置表, 对应 att_leave) ----------
CREATE TABLE IF NOT EXISTS `leave_type` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `dept_id` INT COMMENT '部门ID',
    `days` INT UNSIGNED COMMENT '休假天数',
    `type_num` TINYINT UNSIGNED COMMENT '休假类型: 0事假, 1产假, 2病假, 3婚假, 4探亲假, 5陪产假',
    `status` TINYINT UNSIGNED DEFAULT 1 COMMENT '0禁用, 1正常',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_type_num` (`type_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假类型配置表';


-- ---------- 2.2 staff_leave (员工请假表, 对应 att_staff_leave) ----------
CREATE TABLE IF NOT EXISTS `staff_leave` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `staff_id` INT COMMENT '员工ID',
    `days` INT COMMENT '请假天数',
    `type_num` INT COMMENT '请假类型: 0事假, 1产假, 2病假, 3婚假, 4探亲假, 5陪产假',
    `start_date` DATE COMMENT '请假开始日期',
    `end_date` DATE COMMENT '请假结束日期',
    `status` TINYINT UNSIGNED DEFAULT 0 COMMENT '0未审核, 1审核通过, 2驳回, 3撤销',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_staff_id` (`staff_id`),
    INDEX `idx_status` (`status`),
    INDEX `idx_start_date` (`start_date`),
    INDEX `idx_end_date` (`end_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工请假表';


-- ---------- 2.3 hr_menu (菜单表, 对应 per_menu) ----------
CREATE TABLE IF NOT EXISTS `hr_menu` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `code` VARCHAR(20) COMMENT '菜单编码',
    `name` VARCHAR(50) COMMENT '菜单名称',
    `icon` VARCHAR(50) COMMENT '菜单图标',
    `path` VARCHAR(200) COMMENT '菜单路径',
    `parent_id` INT UNSIGNED DEFAULT 0 COMMENT '父菜单ID, 0代表根菜单',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR菜单表';


-- ---------- 2.4 hr_role (角色表, 对应 per_role) ----------
CREATE TABLE IF NOT EXISTS `hr_role` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `code` VARCHAR(20) COMMENT '角色编码',
    `name` VARCHAR(50) COMMENT '角色名称',
    `remark` VARCHAR(200) COMMENT '角色备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR角色表';


-- ---------- 2.5 hr_role_menu (角色菜单关系表, 对应 per_role_menu) ----------
CREATE TABLE IF NOT EXISTS `hr_role_menu` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `role_id` INT UNSIGNED NOT NULL COMMENT '角色ID',
    `menu_id` INT UNSIGNED NOT NULL COMMENT '菜单ID',
    `status` TINYINT UNSIGNED DEFAULT 1 COMMENT '0禁用, 1正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_role_id` (`role_id`),
    INDEX `idx_menu_id` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR角色菜单关系表';


-- ---------- 2.6 hr_staff_role (员工角色关系表, 对应 per_staff_role) ----------
CREATE TABLE IF NOT EXISTS `hr_staff_role` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `staff_id` INT UNSIGNED COMMENT '员工ID',
    `role_id` INT UNSIGNED COMMENT '角色ID',
    `status` TINYINT UNSIGNED DEFAULT 1 COMMENT '0禁用, 1正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_staff_id` (`staff_id`),
    INDEX `idx_role_id` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR员工角色关系表';


-- ---------- 2.7 salary_deduct (扣款配置表, 对应 sal_salary_deduct) ----------
CREATE TABLE IF NOT EXISTS `salary_deduct` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `dept_id` INT COMMENT '部门ID',
    `type_num` TINYINT UNSIGNED COMMENT '扣款类型: 0迟到, 1早退, 2旷工, 3休假',
    `deduct` INT UNSIGNED DEFAULT 0 COMMENT '每次扣款金额',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_type_num` (`type_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工资扣除规则表';


-- ---------- 2.8 soc_city (参保城市表, 对应 soc_city) ----------
CREATE TABLE IF NOT EXISTS `soc_city` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `name` VARCHAR(50) COMMENT '参保城市名称',
    `average_salary` DECIMAL(10,3) UNSIGNED COMMENT '职工上年度平均月工资',
    `lower_salary` DECIMAL(10,3) COMMENT '职工上年度最低月工资',
    `soc_upper_limit` DECIMAL(10,3) UNSIGNED COMMENT '社保缴纳基数上限',
    `soc_lower_limit` DECIMAL(10,3) UNSIGNED COMMENT '社保缴纳基数下限',
    `hou_upper_limit` DECIMAL(10,3) COMMENT '公积金缴纳基数上限',
    `hou_lower_limit` DECIMAL(10,3) COMMENT '公积金缴纳基数下限',
    `per_pension_rate` DECIMAL(6,3) UNSIGNED COMMENT '个人养老保险缴费比例',
    `com_pension_rate` DECIMAL(6,3) UNSIGNED COMMENT '企业养老保险缴费比例',
    `per_medical_rate` DECIMAL(6,3) UNSIGNED COMMENT '个人医疗保险缴费比例',
    `com_medical_rate` DECIMAL(6,3) UNSIGNED COMMENT '企业医疗保险缴费比例',
    `per_unemployment_rate` DECIMAL(6,3) UNSIGNED COMMENT '个人失业保险缴费比例',
    `com_unemployment_rate` DECIMAL(6,3) UNSIGNED COMMENT '企业失业保险缴费比例',
    `com_maternity_rate` DECIMAL(6,3) UNSIGNED COMMENT '企业生育保险缴费比例',
    `com_injury_rate` DECIMAL(6,3) COMMENT '工伤保险企业缴纳比例',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参保城市表';


-- ---------- 2.9 soc_insurance (五险一金表, 对应 soc_insurance) ----------
CREATE TABLE IF NOT EXISTS `soc_insurance` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `city_id` INT COMMENT '城市ID',
    `staff_id` INT COMMENT '员工ID',
    `house_base` DECIMAL(10,3) COMMENT '公积金基数',
    `per_house_rate` DECIMAL(6,3) COMMENT '公积金个人缴纳比例',
    `per_house_pay` DECIMAL(10,3) COMMENT '公积金个人缴纳金额',
    `com_house_rate` DECIMAL(6,3) COMMENT '公积金企业缴纳比例',
    `com_house_pay` DECIMAL(10,3) COMMENT '公积金企业缴纳金额',
    `social_base` DECIMAL(10,3) COMMENT '社保基数',
    `com_social_pay` DECIMAL(10,3) COMMENT '社保企业缴纳金额',
    `per_social_pay` DECIMAL(10,3) COMMENT '社保个人缴纳金额',
    `com_injury_rate` DECIMAL(6,3) COMMENT '工伤保险企业缴纳比例',
    `social_remark` VARCHAR(200) COMMENT '社保备注',
    `house_remark` VARCHAR(200) COMMENT '公积金备注',
    `pay_month` VARCHAR(6) COMMENT '缴纳月份(YYYYMM)',
    `status` TINYINT UNSIGNED DEFAULT 0 COMMENT '0未支付, 1已支付, 2支付失败',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_city_id` (`city_id`),
    INDEX `idx_staff_id` (`staff_id`),
    INDEX `idx_pay_month` (`pay_month`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工五险一金表';


-- ---------- 2.10 hr_docs (文件表, 对应 sys_docs) ----------
CREATE TABLE IF NOT EXISTS `hr_docs` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `name` VARCHAR(200) COMMENT '文件名称',
    `type` VARCHAR(20) COMMENT '文件类型',
    `old_name` VARCHAR(200) COMMENT '文件原始名称',
    `md5` VARCHAR(200) COMMENT '文件MD5值',
    `size` BIGINT UNSIGNED COMMENT '文件大小(字节)',
    `staff_id` INT COMMENT '上传者ID',
    `remark` VARCHAR(200) COMMENT '文件备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_staff_id` (`staff_id`),
    INDEX `idx_md5` (`md5`),
    INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR文件表';


-- ---------- 2.11 attendance_record (考勤记录表, 对应 att_attendance) ----------
-- 独立于 attendance 表，用于 HR 专用考勤记录
CREATE TABLE IF NOT EXISTS `attendance_record` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `staff_id` INT COMMENT '员工ID',
    `mor_start_time` TIME COMMENT '上午上班时间',
    `mor_end_time` TIME COMMENT '上午下班时间',
    `aft_start_time` TIME COMMENT '下午上班时间',
    `aft_end_time` TIME COMMENT '下午下班时间',
    `attendance_date` DATE NOT NULL COMMENT '考勤日期',
    `status` TINYINT DEFAULT 0 COMMENT '0正常, 1迟到, 2早退, 3旷工, 4休假',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_staff_id` (`staff_id`),
    INDEX `idx_attendance_date` (`attendance_date`),
    INDEX `idx_staff_date` (`staff_id`, `attendance_date`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='HR考勤记录表';


-- ---------- 2.12 overtime_config (加班类型配置表, 对应 att_overtime) ----------
-- 独立于 overtime 表，后者是员工加班记录，此表是加班类型/规则配置
CREATE TABLE IF NOT EXISTS `overtime_config` (
    `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
    `salary_multiple` DECIMAL(5,2) COMMENT '工资倍数',
    `bonus` DECIMAL(10,3) COMMENT '加班奖金',
    `type_num` TINYINT COMMENT '加班类型: 0工作日加班, 1节假日加班, 2休息日加班',
    `dept_id` INT COMMENT '部门ID',
    `count_type` TINYINT DEFAULT 0 COMMENT '计数类型: 0按小时, 1按天',
    `is_time_off` TINYINT UNSIGNED DEFAULT 0 COMMENT '是否补休: 0不补休, 1补休',
    `status` TINYINT UNSIGNED DEFAULT 1 COMMENT '0禁用, 1正常',
    `remark` VARCHAR(200) COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除: 0未删除, 1已删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_type_num` (`type_num`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加班类型配置表';


-- ======================================================================
-- 阶段3: 默认数据初始化
-- ======================================================================

-- ---------- 3.1 请假类型默认数据 ----------
INSERT INTO `leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 3, 0, 1, '事假', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `leave_type` WHERE `store_id` = 1 AND `type_num` = 0);

INSERT INTO `leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 98, 1, 1, '产假', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `leave_type` WHERE `store_id` = 1 AND `type_num` = 1);

INSERT INTO `leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 5, 2, 1, '病假', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `leave_type` WHERE `store_id` = 1 AND `type_num` = 2);

INSERT INTO `leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 3, 3, 1, '婚假', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `leave_type` WHERE `store_id` = 1 AND `type_num` = 3);

INSERT INTO `leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 15, 4, 1, '探亲假', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `leave_type` WHERE `store_id` = 1 AND `type_num` = 4);

INSERT INTO `leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 7, 5, 1, '陪产假', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `leave_type` WHERE `store_id` = 1 AND `type_num` = 5);


-- ---------- 3.2 扣款规则默认数据 ----------
INSERT INTO `salary_deduct` (`store_id`, `dept_id`, `type_num`, `deduct`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 0, 20, '迟到扣款/次', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `salary_deduct` WHERE `store_id` = 1 AND `type_num` = 0);

INSERT INTO `salary_deduct` (`store_id`, `dept_id`, `type_num`, `deduct`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 1, 20, '早退扣款/次', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `salary_deduct` WHERE `store_id` = 1 AND `type_num` = 1);

INSERT INTO `salary_deduct` (`store_id`, `dept_id`, `type_num`, `deduct`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 2, 100, '旷工扣款/天', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `salary_deduct` WHERE `store_id` = 1 AND `type_num` = 2);

INSERT INTO `salary_deduct` (`store_id`, `dept_id`, `type_num`, `deduct`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, NULL, 3, 50, '休假扣款/天', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `salary_deduct` WHERE `store_id` = 1 AND `type_num` = 3);


-- ---------- 3.3 HR菜单默认数据 ----------
INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, 1, 'hr_dashboard', 'HR仪表盘', 'dashboard', '/hr/dashboard', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 1);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 2, 1, 'hr_staff', '员工管理', 'user', '/hr/staff', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 2);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 3, 1, 'hr_dept', '部门管理', 's-operation', '/hr/department', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 3);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 4, 1, 'hr_attendance', '考勤管理', 'edit', '/hr/attendance', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 4);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 5, 1, 'hr_leave', '请假审批', 'suitcase', '/hr/leave', 4, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 5);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 6, 1, 'hr_overtime', '加班管理', 'time', '/hr/overtime', 4, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 6);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 7, 1, 'hr_salary', '薪资管理', 'data-line', '/hr/salary', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 7);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 8, 1, 'hr_insurance', '五险一金', 's-order', '/hr/insurance', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 8);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 9, 1, 'hr_docs', '合同文件', 'document', '/hr/docs', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 9);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 10, 1, 'hr_permission', '权限管理', 's-cooperation', '/hr/permission', 0, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 10);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 11, 1, 'hr_role', '角色管理', 's-custom', '/hr/role', 10, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 11);

INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 12, 1, 'hr_menu_mgr', '菜单管理', 'collection', '/hr/menu', 10, NULL, NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_menu` WHERE `id` = 12);


-- ---------- 3.4 HR角色默认数据 ----------
INSERT INTO `hr_role` (`id`, `store_id`, `code`, `name`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, 1, 'admin', '超级管理员', '拥有所有权限', NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_role` WHERE `id` = 1);

INSERT INTO `hr_role` (`id`, `store_id`, `code`, `name`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 2, 1, 'manager', '店长', '门店管理者', NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_role` WHERE `id` = 2);

INSERT INTO `hr_role` (`id`, `store_id`, `code`, `name`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 3, 1, 'hr', '人事经理', '人力资源管理', NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_role` WHERE `id` = 3);

INSERT INTO `hr_role` (`id`, `store_id`, `code`, `name`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 4, 1, 'staff', '普通员工', '基础权限', NOW(), NULL, 0
WHERE NOT EXISTS (SELECT 1 FROM `hr_role` WHERE `id` = 4);


-- ---------- 3.5 角色菜单关系默认数据（管理员拥有所有菜单） ----------
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, 1, 1, 1, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 1);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 2, 1, 1, 2, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 2);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 3, 1, 1, 3, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 3);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 4, 1, 1, 4, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 4);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 5, 1, 1, 5, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 5);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 6, 1, 1, 6, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 6);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 7, 1, 1, 7, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 7);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 8, 1, 1, 8, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 8);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 9, 1, 1, 9, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 9);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 10, 1, 1, 10, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 10);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 11, 1, 1, 11, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 11);
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`)
SELECT 12, 1, 1, 12, 1, NOW(), NULL, 0 WHERE NOT EXISTS (SELECT 1 FROM `hr_role_menu` WHERE `id` = 12);


-- ---------- 3.6 加班类型配置默认数据 ----------
INSERT INTO `overtime_config` (`id`, `store_id`, `salary_multiple`, `bonus`, `type_num`, `dept_id`, `count_type`, `is_time_off`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, 1, 1.50, NULL, 0, NULL, 0, 0, 1, '工作日加班(1.5倍)', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `overtime_config` WHERE `id` = 1);

INSERT INTO `overtime_config` (`id`, `store_id`, `salary_multiple`, `bonus`, `type_num`, `dept_id`, `count_type`, `is_time_off`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 2, 1, 2.00, 200.000, 1, NULL, 1, 0, 1, '节假日加班(2倍+奖金)', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `overtime_config` WHERE `id` = 2);

INSERT INTO `overtime_config` (`id`, `store_id`, `salary_multiple`, `bonus`, `type_num`, `dept_id`, `count_type`, `is_time_off`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 3, 1, 2.00, NULL, 2, NULL, 0, 1, 1, '休息日加班(2倍+补休)', NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `overtime_config` WHERE `id` = 3);


-- ---------- 3.7 参保城市默认数据 ----------
INSERT INTO `soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, '成都', 10000.000, 3000.000, 30000.000, 6000.000, 30000.000, 3000.000, 0.080, 0.160, 0.020, 0.065, 0.004, 0.006, 0.008, 0.002, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `soc_city` WHERE `store_id` = 1 AND `name` = '成都');

INSERT INTO `soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, '重庆', 8000.000, 3000.000, 24000.000, 4800.000, 24000.000, 3000.000, 0.080, 0.160, 0.020, 0.085, 0.005, 0.005, 0.005, 0.005, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `soc_city` WHERE `store_id` = 1 AND `name` = '重庆');

INSERT INTO `soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, '北京', 12000.000, 4000.000, 36000.000, 7200.000, 36000.000, 4000.000, 0.080, 0.160, 0.020, 0.098, 0.005, 0.005, 0.008, 0.004, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `soc_city` WHERE `store_id` = 1 AND `name` = '北京');

INSERT INTO `soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, '上海', 15000.000, 10000.000, 45000.000, 9000.000, 45000.000, 10000.000, 0.080, 0.160, 0.020, 0.095, 0.005, 0.005, 0.010, 0.005, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `soc_city` WHERE `store_id` = 1 AND `name` = '上海');

INSERT INTO `soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `create_time`, `update_time`, `is_deleted`)
SELECT 1, '深圳', 13000.000, 10000.000, 39000.000, 7800.000, 39000.000, 10000.000, 0.080, 0.140, 0.020, 0.060, 0.005, 0.005, 0.005, 0.002, NULL, NOW(), NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM `soc_city` WHERE `store_id` = 1 AND `name` = '深圳');


-- ======================================================================
-- 清理辅助存储过程
-- ======================================================================
DROP PROCEDURE IF EXISTS _add_col_if_not_exists;
DROP PROCEDURE IF EXISTS _add_idx_if_not_exists;

SET FOREIGN_KEY_CHECKS = 1;

-- ======================================================================
-- 迁移完成。验证SQL:
--   1. 检查新增表: SHOW TABLES LIKE 'leave_type'; SHOW TABLES LIKE 'hr_%'; SHOW TABLES LIKE 'soc_%'; SHOW TABLES LIKE 'salary_deduct'; SHOW TABLES LIKE 'attendance_record'; SHOW TABLES LIKE 'overtime_config';
--   2. 检查新增字段: SHOW COLUMNS FROM department LIKE 'mor_start_time'; SHOW COLUMNS FROM attendance LIKE 'is_deleted'; SHOW COLUMNS FROM overtime LIKE 'type_num';
--   3. 检查默认数据: SELECT COUNT(*) FROM hr_menu; SELECT COUNT(*) FROM hr_role; SELECT COUNT(*) FROM soc_city;
-- ======================================================================
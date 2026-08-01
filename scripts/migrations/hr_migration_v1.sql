-- ======================================================================
-- 又见炊烟餐饮管理系统 - 人事模块数据库迁移脚本 v1
-- 对应规划手册: 开发详情/开发规划手册/5.txt
-- 执行方式: mysql -u <user> -p banquet < hr_migration_v1.sql
-- 特性: 幂等（可重复执行，已存在的字段/表/索引自动跳过）
--       所有新增字段允许 NULL 或带 DEFAULT，不影响存量数据
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

-- ---------- 幂等辅助：索引存在则跳过 ----------
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
-- 阶段1：扩展 4 张存量表（仅追加，不动存量字段）
-- ======================================================================

-- ---------- 1.1 staff_master ----------
CALL _add_col_if_not_exists('staff_master','staff_code',"VARCHAR(20) COMMENT '工号'");
CALL _add_col_if_not_exists('staff_master','gender',"TINYINT DEFAULT 0 COMMENT '0-未知 1-男 2-女'");
CALL _add_col_if_not_exists('staff_master','birth_date',"DATE COMMENT '出生日期'");
CALL _add_col_if_not_exists('staff_master','email',"VARCHAR(100) COMMENT '邮箱'");
CALL _add_col_if_not_exists('staff_master','wechat',"VARCHAR(50) COMMENT '微信号'");
CALL _add_col_if_not_exists('staff_master','native_place',"VARCHAR(100) COMMENT '籍贯'");
CALL _add_col_if_not_exists('staff_master','nation',"VARCHAR(20) DEFAULT '汉族' COMMENT '民族'");
CALL _add_col_if_not_exists('staff_master','marital_status',"TINYINT DEFAULT 0 COMMENT '0-未婚 1-已婚 2-离异 3-丧偶'");
CALL _add_col_if_not_exists('staff_master','education',"VARCHAR(20) COMMENT '学历'");
CALL _add_col_if_not_exists('staff_master','major',"VARCHAR(50) COMMENT '专业'");
CALL _add_col_if_not_exists('staff_master','graduate_school',"VARCHAR(100) COMMENT '毕业院校'");
CALL _add_col_if_not_exists('staff_master','graduate_date',"DATE COMMENT '毕业日期'");
CALL _add_col_if_not_exists('staff_master','probation_months',"INT DEFAULT 1 COMMENT '试用期月数'");
CALL _add_col_if_not_exists('staff_master','regular_date',"DATE COMMENT '转正日期'");
CALL _add_col_if_not_exists('staff_master','leader_id',"BIGINT COMMENT '直属上级ID(关联staff_master.staff_id)'");
CALL _add_col_if_not_exists('staff_master','employment_type',"TINYINT DEFAULT 1 COMMENT '1-全职 2-兼职 3-临时工 4-外包'");
CALL _add_col_if_not_exists('staff_master','avatar_url',"VARCHAR(500) COMMENT '头像URL(COS)'");
-- 注：id_card/hire_date/emergency_contact/emergency_phone/resign_date/resign_reason/remark 已存在，跳过

-- 回填工号
UPDATE staff_master SET staff_code = CONCAT('EMP', LPAD(staff_id, 5, '0')) WHERE staff_code IS NULL;

CALL _add_idx_if_not_exists('staff_master','idx_staff_code','`staff_code`');
CALL _add_idx_if_not_exists('staff_master','idx_hire_date','`hire_date`');
CALL _add_idx_if_not_exists('staff_master','idx_resign_date','`resign_date`');
CALL _add_idx_if_not_exists('staff_master','idx_leader_id','`leader_id`');
CALL _add_idx_if_not_exists('staff_master','idx_staff_dept_status','`dept_id`,`employment_status`');

-- ---------- 1.2 attendance_records ----------
CALL _add_col_if_not_exists('attendance','clock_in_location',"VARCHAR(255) COMMENT '上班打卡位置'");
CALL _add_col_if_not_exists('attendance','clock_out_location',"VARCHAR(255) COMMENT '下班打卡位置'");
CALL _add_col_if_not_exists('attendance','clock_in_type',"TINYINT DEFAULT 1 COMMENT '1-正常 2-迟到 3-缺卡'");
CALL _add_col_if_not_exists('attendance','clock_out_type',"TINYINT DEFAULT 1 COMMENT '1-正常 2-早退 3-缺卡'");
CALL _add_col_if_not_exists('attendance','is_manual',"TINYINT DEFAULT 0 COMMENT '0-自动 1-手动补卡'");
CALL _add_col_if_not_exists('attendance','manual_remark',"VARCHAR(255) COMMENT '补卡备注'");
CALL _add_col_if_not_exists('attendance','approver_id',"BIGINT COMMENT '补卡审批人ID'");
CALL _add_col_if_not_exists('attendance','approve_status',"TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回'");
CALL _add_col_if_not_exists('attendance','approve_time',"DATETIME COMMENT '审批时间'");
CALL _add_idx_if_not_exists('attendance','idx_attendance_date','`attendance_date`');
CALL _add_idx_if_not_exists('attendance','idx_attendance_staff_date','`staff_id`,`attendance_date`');
CALL _add_idx_if_not_exists('attendance','idx_approve_status','`approve_status`');

-- ---------- 1.3 leave_record ----------
CALL _add_col_if_not_exists('leave_record','leave_no',"VARCHAR(30) COMMENT '请假单号'");
CALL _add_col_if_not_exists('leave_record','attachment_path',"VARCHAR(500) COMMENT '附件COS路径'");
CALL _add_col_if_not_exists('leave_record','approver_1_id',"BIGINT COMMENT '店长审批人ID'");
CALL _add_col_if_not_exists('leave_record','approver_1_status',"TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回'");
CALL _add_col_if_not_exists('leave_record','approver_1_time',"DATETIME COMMENT '店长审批时间'");
CALL _add_col_if_not_exists('leave_record','approver_1_remark',"VARCHAR(255) COMMENT '店长审批备注'");
CALL _add_col_if_not_exists('leave_record','approver_2_id',"BIGINT COMMENT '总经理审批人ID'");
CALL _add_col_if_not_exists('leave_record','approver_2_status',"TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回'");
CALL _add_col_if_not_exists('leave_record','approver_2_time',"DATETIME COMMENT '总经理审批时间'");
CALL _add_col_if_not_exists('leave_record','approver_2_remark',"VARCHAR(255) COMMENT '总经理审批备注'");
CALL _add_col_if_not_exists('leave_record','final_status',"TINYINT DEFAULT 1 COMMENT '1-待审批 2-已通过 3-已驳回 4-已撤销'");
CALL _add_idx_if_not_exists('leave_record','idx_leave_no','`leave_no`');
CALL _add_idx_if_not_exists('leave_record','idx_final_status','`final_status`');
CALL _add_idx_if_not_exists('leave_record','idx_leave_staff_status','`staff_id`,`final_status`');

-- ---------- 1.4 overtime ----------
CALL _add_col_if_not_exists('overtime','overtime_no',"VARCHAR(30) COMMENT '加班单号'");
CALL _add_col_if_not_exists('overtime','approver_1_id',"BIGINT COMMENT '店长审批人ID'");
CALL _add_col_if_not_exists('overtime','approver_1_status',"TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回'");
CALL _add_col_if_not_exists('overtime','approver_1_time',"DATETIME COMMENT '店长审批时间'");
CALL _add_col_if_not_exists('overtime','approver_2_id',"BIGINT COMMENT '总经理审批人ID'");
CALL _add_col_if_not_exists('overtime','approver_2_status',"TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回'");
CALL _add_col_if_not_exists('overtime','approver_2_time',"DATETIME COMMENT '总经理审批时间'");
CALL _add_col_if_not_exists('overtime','final_status',"TINYINT DEFAULT 1 COMMENT '1-待审批 2-已通过 3-已驳回'");
CALL _add_idx_if_not_exists('overtime','idx_overtime_no','`overtime_no`');
CALL _add_idx_if_not_exists('overtime','idx_ot_final_status','`final_status`');
CALL _add_idx_if_not_exists('overtime','idx_overtime_staff_status','`staff_id`,`final_status`');

-- ======================================================================
-- 阶段2：新建 6 张独立人事业务表（无 hr_ 前缀）
-- ======================================================================

-- ---------- 2.1 排班 ----------
CREATE TABLE IF NOT EXISTS schedule_month (
    schedule_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL COMMENT '门店ID',
    dept_id INT NOT NULL COMMENT '部门ID',
    schedule_month VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
    status TINYINT DEFAULT 0 COMMENT '0-草稿 1-已发布 2-已确认',
    published_by INT COMMENT '发布人(staff_id)',
    published_time DATETIME COMMENT '发布时间',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dept_id) REFERENCES department(dept_id) ON DELETE RESTRICT,
    UNIQUE KEY uk_month_dept (schedule_month, dept_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度排班主表';

CREATE TABLE IF NOT EXISTS schedule_day (
    day_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    schedule_id BIGINT NOT NULL COMMENT '关联排班主表',
    staff_id INT NOT NULL COMMENT '关联员工',
    work_date DATE NOT NULL COMMENT '日期',
    shift_type TINYINT NOT NULL COMMENT '1-早班 2-中班 3-晚班 4-休息 5-请假 6-培训',
    shift_start TIME COMMENT '上班时间',
    shift_end TIME COMMENT '下班时间',
    break_minutes INT DEFAULT 60 COMMENT '休息时长(分钟)',
    is_holiday TINYINT DEFAULT 0 COMMENT '是否节假日',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (schedule_id) REFERENCES schedule_month(schedule_id) ON DELETE CASCADE,
    FOREIGN KEY (staff_id) REFERENCES staff_master(staff_id) ON DELETE RESTRICT,
    UNIQUE KEY uk_staff_date (staff_id, work_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日排班明细';

-- ---------- 2.2 薪资 ----------
CREATE TABLE IF NOT EXISTS salary_template (
    template_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    template_name VARCHAR(100) NOT NULL COMMENT '模板名称',
    post_name VARCHAR(50) NOT NULL COMMENT '岗位名称',
    base_salary DECIMAL(10,2) NOT NULL COMMENT '基本工资',
    overtime_rate DECIMAL(3,1) DEFAULT 1.5 COMMENT '加班倍率',
    meal_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '餐补',
    transport_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '交通补贴',
    housing_subsidy DECIMAL(10,2) DEFAULT 0 COMMENT '住房补贴',
    attendance_bonus DECIMAL(10,2) DEFAULT 0 COMMENT '全勤奖',
    social_security_employee DECIMAL(10,2) DEFAULT 0 COMMENT '社保个人承担',
    housing_fund_employee DECIMAL(10,2) DEFAULT 0 COMMENT '公积金个人承担',
    performance_ratio DECIMAL(5,2) DEFAULT 30 COMMENT '绩效占比(%)',
    is_active TINYINT DEFAULT 1,
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='薪资标准模板';

CREATE TABLE IF NOT EXISTS month_salary (
    salary_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    staff_id INT NOT NULL COMMENT '关联staff_master.staff_id',
    salary_month VARCHAR(7) NOT NULL COMMENT 'YYYY-MM',
    base_salary DECIMAL(10,2) NOT NULL COMMENT '基本工资',
    overtime_pay DECIMAL(10,2) DEFAULT 0 COMMENT '加班费',
    performance_salary DECIMAL(10,2) DEFAULT 0 COMMENT '绩效工资',
    reward_amount DECIMAL(10,2) DEFAULT 0 COMMENT '奖励金额',
    punish_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '处罚扣款',
    leave_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '请假扣款',
    social_security_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '社保扣款',
    housing_fund_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '公积金扣款',
    other_allowance DECIMAL(10,2) DEFAULT 0 COMMENT '其他补贴',
    other_deduction DECIMAL(10,2) DEFAULT 0 COMMENT '其他扣款',
    gross_salary DECIMAL(10,2) NOT NULL COMMENT '应发工资',
    net_salary DECIMAL(10,2) NOT NULL COMMENT '实发工资',
    tax_amount DECIMAL(10,2) DEFAULT 0 COMMENT '个税',
    status TINYINT DEFAULT 0 COMMENT '0-草稿 1-已核算 2-已审批 3-已发放',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff_master(staff_id) ON DELETE RESTRICT,
    UNIQUE KEY uk_staff_month (staff_id, salary_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='月度薪资主表';

-- ---------- 2.3 奖惩 ----------
CREATE TABLE IF NOT EXISTS reward_punish (
    rp_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    staff_id INT NOT NULL COMMENT '关联staff_master.staff_id',
    rp_no VARCHAR(30) NOT NULL COMMENT '单据编号',
    rp_type TINYINT NOT NULL COMMENT '1-奖励 2-处罚',
    rp_category VARCHAR(50) NOT NULL COMMENT '分类',
    amount DECIMAL(10,2) NOT NULL COMMENT '金额(奖励正,处罚负)',
    reason TEXT NOT NULL COMMENT '事由',
    evidence_path VARCHAR(500) COMMENT '证据附件COS路径',
    approver_1_id INT COMMENT '店长审批人',
    approver_1_status TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回',
    approver_1_time DATETIME,
    approver_1_remark VARCHAR(255),
    approver_2_id INT COMMENT '总经理审批人',
    approver_2_status TINYINT DEFAULT 1 COMMENT '1-待审 2-通过 3-驳回',
    approver_2_time DATETIME,
    approver_2_remark VARCHAR(255),
    final_status TINYINT DEFAULT 1 COMMENT '1-待审批 2-已生效 3-已驳回',
    is_synced_to_salary TINYINT DEFAULT 0 COMMENT '是否已同步薪资',
    sync_salary_id BIGINT COMMENT '关联month_salary.salary_id',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff_master(staff_id) ON DELETE RESTRICT,
    INDEX idx_rp_no (rp_no),
    INDEX idx_rp_final_status (final_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='奖惩记录表';

-- ---------- 2.4 合同 ----------
CREATE TABLE IF NOT EXISTS contract (
    contract_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL,
    staff_id INT NOT NULL COMMENT '关联staff_master.staff_id',
    contract_no VARCHAR(50) NOT NULL COMMENT '合同编号',
    contract_type TINYINT DEFAULT 1 COMMENT '1-固定期限 2-无固定期限 3-实习协议',
    sign_date DATE NOT NULL COMMENT '签订日期',
    start_date DATE NOT NULL COMMENT '合同开始日期',
    end_date DATE COMMENT '合同到期日期',
    file_path VARCHAR(500) COMMENT '合同扫描件COS路径',
    status TINYINT DEFAULT 1 COMMENT '1-有效 2-已到期 3-已解除',
    remark VARCHAR(255),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (staff_id) REFERENCES staff_master(staff_id) ON DELETE RESTRICT,
    INDEX idx_end_date (end_date),
    INDEX idx_contract_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='劳动合同表';

-- ======================================================================
-- 阶段3：数据回填（在职员工默认入职日期）
-- ======================================================================
UPDATE staff_master
SET hire_date = DATE(create_time)
WHERE hire_date IS NULL
  AND employment_status IN ('1', '2', '在职', '试用期');

-- 清理辅助存储过程
DROP PROCEDURE IF EXISTS _add_col_if_not_exists;
DROP PROCEDURE IF EXISTS _add_idx_if_not_exists;

-- ======================================================================
-- 迁移完成。验证：
-- SHOW COLUMNS FROM staff_master LIKE 'staff_code';
-- SHOW TABLES LIKE 'schedule_%';
-- SHOW TABLES LIKE 'salary_%';
-- SHOW TABLES LIKE 'reward_punish';
-- SHOW TABLES LIKE 'contract';
-- ======================================================================

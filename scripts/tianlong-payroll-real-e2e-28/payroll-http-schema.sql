-- ============================================================================
-- TL-RC-PAYROLL-HTTP-LINUX-28  R1 窄返工：工资 HTTP 闭环独立 schema 建表
--
-- 用途：由 run-http-e2e.sh 应用到「本任务新建的唯一 schema tlpay28_http_<时间戳>」。
-- 约束（违反即失败）：
--   * 仅 CREATE TABLE / INSERT，绝不 DELETE / DROP / TRUNCATE；
--   * 不写本文件不含任何密码 / JWT / 凭证明文（合成账号密码在运行期内存生成，经 BCrypt 后写入）；
--   * 只建本 schema 所需表，绝不动 13317 上任何已有 schema。
--
-- 表口径对齐：
--   * store_info / staff_master / month_salary 参考基线 fixture
--     payroll-metadata-fixture-20260907.sql + 生产 schema 列口径；
--   * month_salary 审批发放列 / payroll_payout_record 对齐 scripts/migrations/payroll_approval_payout_v1.sql 最终口径；
--   * staff_master 补齐登录（staff_password/staff_phone/staff_en_name/staff_position/permission_level）
--     与实时复核（role/employment_status/store_id）所需列。
-- ============================================================================

-- 门店（登录 getStoreName 与 /api/stores、工资跨店校验的外键父表）
CREATE TABLE store_info (
  store_id BIGINT NOT NULL PRIMARY KEY,
  store_code VARCHAR(50) NOT NULL,
  store_name VARCHAR(100) NOT NULL,
  store_short_name VARCHAR(50),
  store_type VARCHAR(20) DEFAULT 'normal',
  address VARCHAR(200),
  phone VARCHAR(20),
  status VARCHAR(20) DEFAULT 'open',
  sort_order INT DEFAULT 0,
  bank_account VARCHAR(255)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO store_info (store_id, store_code, store_name, store_short_name, store_type, address, phone, status, sort_order)
VALUES
  (1, 'NG001', '宁国店', '宁国', 'normal', '宁国店测试地址', '0563-0000001', 'open', 1),
  (2, 'XC001', '宣城店', '宣城', 'normal', '宣城店测试地址', '0563-0000002', 'open', 2);

-- 员工花名册（登录 + 实时复核 + 工资权限判定 + 工资明细回退的唯一父表）
CREATE TABLE staff_master (
  staff_id INT NOT NULL PRIMARY KEY,
  store_id BIGINT NOT NULL,
  staff_name VARCHAR(50),
  staff_account VARCHAR(50),
  staff_en_name VARCHAR(50),
  staff_phone VARCHAR(20),
  staff_password VARCHAR(100),
  role VARCHAR(30),
  department VARCHAR(30),
  staff_position VARCHAR(30),
  permission_level VARCHAR(30),
  can_manage_hr INT DEFAULT 0,
  can_view_all_stores INT DEFAULT 0,
  employment_status VARCHAR(20),
  basic_salary DECIMAL(10,2),
  monthly_salary DECIMAL(10,2),
  performance_salary DECIMAL(10,2),
  subsidy DECIMAL(10,2),
  bonus DECIMAL(10,2),
  social_insurance DECIMAL(10,2),
  housing_fund DECIMAL(10,2),
  bank_account VARCHAR(255),
  id_card VARCHAR(50),
  CONSTRAINT fk_staff_store FOREIGN KEY (store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 工资明细（保存 → 审批 → 发放记账 的唯一落库表）
CREATE TABLE month_salary (
  salary_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id BIGINT NOT NULL,
  staff_id INT NOT NULL,
  salary_month VARCHAR(7) NOT NULL,
  base_salary DECIMAL(10,2) NOT NULL,
  overtime_pay DECIMAL(10,2) DEFAULT 0,
  performance_salary DECIMAL(10,2) DEFAULT 0,
  reward_amount DECIMAL(10,2) DEFAULT 0,
  punish_deduction DECIMAL(10,2) DEFAULT 0,
  leave_deduction DECIMAL(10,2) DEFAULT 0,
  social_security_deduction DECIMAL(10,2) DEFAULT 0,
  housing_fund_deduction DECIMAL(10,2) DEFAULT 0,
  other_allowance DECIMAL(10,2) DEFAULT 0,
  other_deduction DECIMAL(10,2) DEFAULT 0,
  gross_salary DECIMAL(10,2) NOT NULL,
  net_salary DECIMAL(10,2) NOT NULL,
  tax_amount DECIMAL(10,2) DEFAULT 0,
  status INT DEFAULT 0,
  remark VARCHAR(255),
  post_salary_snapshot DECIMAL(10,2),
  attendance_pay_snapshot DECIMAL(10,2),
  approved_by VARCHAR(40),
  approved_at DATETIME,
  paid_by VARCHAR(40),
  paid_at DATETIME,
  payout_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_staff_month (staff_id, salary_month),
  KEY idx_staff_salary (staff_id),
  KEY idx_store_salary (store_id),
  KEY idx_month_salary_payout (payout_id),
  CONSTRAINT fk_salary_staff FOREIGN KEY (staff_id) REFERENCES staff_master(staff_id),
  CONSTRAINT fk_salary_store FOREIGN KEY (store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 发放记账台账（非银行支付凭证；与工资行通过 payout_id 互反查）
CREATE TABLE payroll_payout_record (
  payout_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL,
  store_id BIGINT NULL,
  headcount INT NOT NULL,
  total_net DECIMAL(15,2) NOT NULL,
  recorded_by VARCHAR(40) NOT NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note VARCHAR(200),
  UNIQUE KEY uk_payout_month_identity (payout_id, salary_month),
  KEY idx_payout_month (salary_month, store_id),
  CONSTRAINT fk_payout_store FOREIGN KEY (store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- month_salary -> payroll_payout_record 复合外键（对齐迁移最终口径）
ALTER TABLE month_salary
  ADD CONSTRAINT fk_salary_payout_month
  FOREIGN KEY (payout_id, salary_month) REFERENCES payroll_payout_record (payout_id, salary_month)
  ON DELETE RESTRICT;

-- getPayroll() 只读依赖的三张明细表（工资回读）
CREATE TABLE attendance_records (staff_id VARCHAR(30), month VARCHAR(7), total_present DECIMAL(10,2)) ENGINE=InnoDB;
CREATE TABLE overtime (staff_id INT, overtime_date DATE, hours DECIMAL(10,2)) ENGINE=InnoDB;
CREATE TABLE report_staff_kpi (staff_id INT, stat_month VARCHAR(7), reward_count INT) ENGINE=InnoDB;

-- 操作审计表（AuditLogAspect 写操作自动落库）
CREATE TABLE audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id VARCHAR(60),
  action VARCHAR(200),
  target VARCHAR(200),
  detail TEXT,
  store_id BIGINT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

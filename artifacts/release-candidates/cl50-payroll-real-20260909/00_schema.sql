-- CL50 隔离 schema。只新增，不 DROP / 不 DELETE / 不 reset。
-- 结构取自卡内指定 test 的 setup 与 payroll-metadata-fixture-20260907.sql，
-- 缺的按被测代码实际 SQL 补齐（卡内已授权）。
-- 全部为合成数据，与生产无关，也不代表任何真人。

CREATE TABLE IF NOT EXISTS store_info (
  store_id BIGINT NOT NULL PRIMARY KEY,
  store_name VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO store_info VALUES (1,'合成一店'),(2,'合成二店');

CREATE TABLE IF NOT EXISTS staff_master (
  staff_id INT NOT NULL PRIMARY KEY,
  store_id BIGINT NOT NULL,
  staff_name VARCHAR(50), staff_account VARCHAR(50), staff_en_name VARCHAR(50),
  staff_phone VARCHAR(30), staff_password VARCHAR(100), staff_position VARCHAR(50),
  role VARCHAR(30), employment_status VARCHAR(20), department VARCHAR(30),
  permission_level INT DEFAULT 0,
  can_view_all_stores INT DEFAULT 0, can_manage_hr INT DEFAULT 0,
  basic_salary DECIMAL(10,2), monthly_salary DECIMAL(10,2), performance_salary DECIMAL(10,2),
  subsidy DECIMAL(10,2), bonus DECIMAL(10,2), social_insurance DECIMAL(10,2), housing_fund DECIMAL(10,2),
  FOREIGN KEY (store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS month_salary (
  salary_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id BIGINT NOT NULL, staff_id INT NOT NULL, salary_month VARCHAR(7) NOT NULL,
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
  gross_salary DECIMAL(10,2) NOT NULL, net_salary DECIMAL(10,2) NOT NULL,
  tax_amount DECIMAL(10,2) DEFAULT 0, status INT DEFAULT 0, remark VARCHAR(255),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_staff_month (staff_id, salary_month),
  KEY idx_staff_salary (staff_id), KEY idx_store_salary (store_id),
  FOREIGN KEY (staff_id) REFERENCES staff_master(staff_id),
  FOREIGN KEY (store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS attendance_records (staff_id VARCHAR(30), month VARCHAR(7), total_present DECIMAL(10,2));
CREATE TABLE IF NOT EXISTS overtime (staff_id INT, overtime_date DATE, hours DECIMAL(10,2));
CREATE TABLE IF NOT EXISTS report_staff_kpi (staff_id INT, stat_month VARCHAR(7), reward_count INT);
CREATE TABLE IF NOT EXISTS audit_logs (
  id BIGINT AUTO_INCREMENT PRIMARY KEY, user_id VARCHAR(60), action VARCHAR(200),
  target VARCHAR(200), detail TEXT, store_id BIGINT,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

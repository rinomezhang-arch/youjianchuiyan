-- month_salary metadata verified by TL-OPS-PAYROLL-SCHEMA-01, 2026-09-07.
-- staff_master/store_info below are reference-key/service-column fixtures, not full production DDL.
CREATE TABLE store_info (store_id BIGINT NOT NULL PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO store_info VALUES(1),(2);
CREATE TABLE staff_master (
 staff_id INT NOT NULL PRIMARY KEY, store_id BIGINT NOT NULL,
 staff_name VARCHAR(50), staff_account VARCHAR(50), staff_en_name VARCHAR(50),
 FOREIGN KEY(store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE month_salary (
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
 UNIQUE KEY uk_staff_month(staff_id,salary_month),
 KEY idx_staff_salary(staff_id), KEY idx_store_salary(store_id),
 FOREIGN KEY(staff_id) REFERENCES staff_master(staff_id),
 FOREIGN KEY(store_id) REFERENCES store_info(store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

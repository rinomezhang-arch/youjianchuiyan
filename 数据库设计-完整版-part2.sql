-- 接 数据库设计-完整版.sql

-- ===== 10. 员工/HR (续) =====

CREATE TABLE IF NOT EXISTS payroll_record (
    payroll_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    pay_month VARCHAR(7),
    base_salary DECIMAL(10,2),
    overtime_pay DECIMAL(10,2),
    bonus DECIMAL(10,2),
    deduction DECIMAL(10,2),
    social_insurance DECIMAL(10,2),
    net_salary DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'draft',
    paid_at TIMESTAMP NULL,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staff_license (
    license_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    license_type VARCHAR(50),
    license_no VARCHAR(100),
    issue_date DATE,
    expiry_date DATE,
    file_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'valid',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staff_self_service (
    request_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    request_type VARCHAR(30),
    request_data JSON,
    status VARCHAR(20) DEFAULT 'pending',
    approved_by INT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 11. 财务 (6张表) =====

CREATE TABLE IF NOT EXISTS finance_daily_report (
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    report_date DATE NOT NULL,
    total_revenue DECIMAL(12,2),
    total_cost DECIMAL(12,2),
    gross_profit DECIMAL(12,2),
    order_count INT,
    avg_order_amount DECIMAL(10,2),
    payment_summary JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_date (store_id, report_date)
);

CREATE TABLE IF NOT EXISTS finance_expense (
    expense_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    expense_date DATE,
    expense_category VARCHAR(50),
    amount DECIMAL(10,2),
    description VARCHAR(200),
    receipt_url VARCHAR(500),
    operator_id INT,
    approved_by INT,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dish_cost_analysis (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    dish_id VARCHAR(20) NOT NULL,
    analysis_date DATE,
    ingredient_cost DECIMAL(10,2),
    labor_cost DECIMAL(10,2),
    overhead_cost DECIMAL(10,2),
    total_cost DECIMAL(10,2),
    sale_price DECIMAL(10,2),
    profit_margin DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS finance_invoice (
    invoice_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    order_id INT,
    invoice_no VARCHAR(50),
    invoice_type VARCHAR(20),
    amount DECIMAL(10,2),
    tax_amount DECIMAL(10,2),
    buyer_name VARCHAR(100),
    buyer_tax_no VARCHAR(50),
    status VARCHAR(20) DEFAULT 'issued',
    issued_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS finance_cash_flow (
    flow_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    flow_date DATE,
    flow_type VARCHAR(20),
    category VARCHAR(50),
    amount DECIMAL(12,2),
    direction VARCHAR(5),
    related_id INT,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS finance_receivable (
    receivable_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    customer_name VARCHAR(100),
    amount DECIMAL(10,2),
    due_date DATE,
    status VARCHAR(20) DEFAULT 'pending',
    related_order_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 12. 营销/会员 (6张表) =====

CREATE TABLE IF NOT EXISTS marketing_campaign (
    campaign_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    campaign_name VARCHAR(100),
    campaign_type VARCHAR(30),
    start_date DATE,
    end_date DATE,
    discount_type VARCHAR(20),
    discount_value DECIMAL(10,2),
    target_group VARCHAR(50),
    budget DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'draft',
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon_master (
    coupon_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    coupon_name VARCHAR(100),
    coupon_type VARCHAR(20),
    discount_value DECIMAL(10,2),
    min_order_amount DECIMAL(10,2) DEFAULT 0,
    valid_start DATE,
    valid_end DATE,
    total_count INT DEFAULT 0,
    used_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'active',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS coupon_record (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    coupon_id INT NOT NULL,
    customer_id INT,
    coupon_code VARCHAR(30) UNIQUE,
    status VARCHAR(20) DEFAULT 'unused',
    used_at TIMESTAMP NULL,
    used_order_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS marketing_message (
    message_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    customer_id INT,
    message_type VARCHAR(20),
    title VARCHAR(100),
    content TEXT,
    send_status VARCHAR(20) DEFAULT 'pending',
    sent_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS guest_analysis (
    analysis_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    analysis_date DATE,
    time_type VARCHAR(10),
    total_guests INT,
    new_guests INT,
    return_guests INT,
    avg_guest_per_table DECIMAL(4,1),
    peak_hour VARCHAR(10),
    source_distribution JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS table_utilization (
    util_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    util_date DATE,
    time_type VARCHAR(10),
    total_tables INT,
    used_tables INT,
    utilization_rate DECIMAL(5,2),
    avg_turnover_time INT,
    revenue_per_table DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 13. 行政/资产 (6张表) =====

CREATE TABLE IF NOT EXISTS asset_master (
    asset_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    asset_name VARCHAR(100),
    asset_category VARCHAR(50),
    asset_code VARCHAR(30),
    purchase_date DATE,
    purchase_price DECIMAL(10,2),
    current_value DECIMAL(10,2),
    location VARCHAR(100),
    status VARCHAR(20) DEFAULT 'in-use',
    responsible_person VARCHAR(50),
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS maintenance_record (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    asset_id INT,
    maintenance_type VARCHAR(30),
    description TEXT,
    reporter_id INT,
    assigned_to VARCHAR(50),
    priority VARCHAR(10) DEFAULT 'normal',
    status VARCHAR(20) DEFAULT 'pending',
    cost DECIMAL(10,2),
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS engineering_project (
    project_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    project_name VARCHAR(100),
    project_type VARCHAR(50),
    start_date DATE,
    end_date DATE,
    budget DECIMAL(12,2),
    actual_cost DECIMAL(12,2),
    status VARCHAR(20) DEFAULT 'planning',
    manager VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS decoration_record (
    deco_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    area_name VARCHAR(50),
    deco_type VARCHAR(30),
    start_date DATE,
    end_date DATE,
    budget DECIMAL(12,2),
    contractor VARCHAR(100),
    status VARCHAR(20) DEFAULT 'planning',
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS energy_record (
    energy_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    record_month VARCHAR(7),
    energy_type VARCHAR(20),
    usage_amount DECIMAL(12,2),
    unit_price DECIMAL(10,4),
    total_cost DECIMAL(10,2),
    meter_reading DECIMAL(12,2),
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS safety_check (
    check_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    check_date DATE,
    check_type VARCHAR(30),
    checker VARCHAR(50),
    result VARCHAR(20),
    issues TEXT,
    corrective_actions TEXT,
    next_check_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 14. 证照管理 (2张表) =====

CREATE TABLE IF NOT EXISTS store_license (
    license_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    license_type VARCHAR(50),
    license_no VARCHAR(100),
    issue_authority VARCHAR(100),
    issue_date DATE,
    expiry_date DATE,
    file_url VARCHAR(500),
    status VARCHAR(20) DEFAULT 'valid',
    reminder_days INT DEFAULT 30,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS license_renewal_log (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    license_id INT NOT NULL,
    old_expiry DATE,
    new_expiry DATE,
    operator_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 15. 数据分析/报表 (4张表) =====

CREATE TABLE IF NOT EXISTS report_template (
    template_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    template_name VARCHAR(100),
    report_type VARCHAR(50),
    config JSON,
    is_system TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS report_generated (
    report_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    template_id INT,
    report_name VARCHAR(100),
    report_data JSON,
    generated_by INT,
    file_url VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS data_screen_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    screen_name VARCHAR(100),
    layout_config JSON,
    data_sources JSON,
    refresh_interval INT DEFAULT 30,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS change_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    module VARCHAR(50),
    entity_type VARCHAR(50),
    entity_id VARCHAR(50),
    action VARCHAR(20),
    old_value JSON,
    new_value JSON,
    operator_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 16. AI助手 (2张表) =====

CREATE TABLE IF NOT EXISTS ai_conversation (
    conv_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    user_id INT,
    title VARCHAR(100),
    context JSON,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS ai_message (
    message_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conv_id INT NOT NULL,
    role VARCHAR(20),
    content TEXT,
    tool_calls JSON,
    tokens_used INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 索引 =====
CREATE INDEX idx_table_store ON table_master(store_id);
CREATE INDEX idx_table_area ON table_master(table_area);
CREATE INDEX idx_booking_date ON booking_master(booking_date);
CREATE INDEX idx_booking_status ON booking_master(booking_status);
CREATE INDEX idx_booking_customer ON booking_master(customer_id);
CREATE INDEX idx_order_store ON order_master(store_id);
CREATE INDEX idx_order_date ON order_master(created_at);
CREATE INDEX idx_order_status ON order_master(order_status);
CREATE INDEX idx_customer_phone ON customer_master(customer_phone);
CREATE INDEX idx_staff_dept ON staff_master(department);
CREATE INDEX idx_attendance_date ON attendance_record(attend_date);
CREATE INDEX idx_inventory_ingredient ON inventory_log(ingredient_id);
CREATE INDEX idx_finance_date ON finance_daily_report(report_date);

-- ===== 初始数据 =====
INSERT IGNORE INTO sys_store (store_id, store_name, store_code) VALUES (1, '宁国店', 'NG001');
INSERT IGNORE INTO sys_user (user_id, username, password_hash, real_name, store_id, role) VALUES (1, 'rino', '$2a$10$xxx', 'Rino', 1, 'admin');
INSERT IGNORE INTO sys_role (role_id, role_name, role_code, is_system) VALUES (1, '管理员', 'admin', 1), (2, '店长', 'manager', 1), (3, '员工', 'staff', 1);

-- 采购申请表
CREATE TABLE IF NOT EXISTS procurement_request (
    request_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    request_no VARCHAR(50) UNIQUE NOT NULL,
    department_id INT,
    department_name VARCHAR(50),
    requester_id INT,
    requester_name VARCHAR(50),
    request_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    total_amount DECIMAL(12,2),
    reason TEXT,
    urgency VARCHAR(20),
    expected_date DATE,
    approver_id INT,
    approver_name VARCHAR(50),
    approve_time DATETIME,
    approve_comment TEXT,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_request (store_id),
    INDEX idx_status_request (status),
    INDEX idx_dept_request (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 审批记录表
CREATE TABLE IF NOT EXISTS approval_log (
    log_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    business_id VARCHAR(50) NOT NULL,
    business_type VARCHAR(50) NOT NULL,
    current_status VARCHAR(20) NOT NULL,
    previous_status VARCHAR(20),
    approver_id INT,
    approver_name VARCHAR(50),
    action VARCHAR(20) NOT NULL,
    comment TEXT,
    approval_time DATETIME,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_business (business_id, business_type),
    INDEX idx_business_type (business_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 报销单表
CREATE TABLE IF NOT EXISTS reimbursement (
    reimbursement_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    reimbursement_no VARCHAR(50) UNIQUE NOT NULL,
    applicant_id INT,
    applicant_name VARCHAR(50),
    department_id INT,
    department_name VARCHAR(50),
    reimburse_date DATE NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    reimburse_type VARCHAR(50),
    receipt_count INT,
    receipt_file_path VARCHAR(255),
    purpose TEXT,
    approver_id INT,
    approver_name VARCHAR(50),
    approve_time DATETIME,
    approve_comment TEXT,
    finance_approver_id INT,
    finance_approver_name VARCHAR(50),
    finance_approve_time DATETIME,
    payment_date DATE,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_reimburse (store_id),
    INDEX idx_status_reimburse (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 领用单表
CREATE TABLE IF NOT EXISTS requisition_order (
    requisition_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    requisition_no VARCHAR(50) UNIQUE NOT NULL,
    department_id INT,
    department_name VARCHAR(50),
    requester_id INT,
    requester_name VARCHAR(50),
    requisition_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'pending',
    total_amount DECIMAL(12,2),
    reason TEXT,
    approver_id INT,
    approver_name VARCHAR(50),
    approve_time DATETIME,
    warehouse_keeper_id INT,
    warehouse_keeper_name VARCHAR(50),
    issue_time DATETIME,
    remark VARCHAR(500),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_requisition (store_id),
    INDEX idx_status_requisition (status),
    INDEX idx_dept_requisition (department_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 领用明细表
CREATE TABLE IF NOT EXISTS requisition_detail (
    detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    requisition_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    ingredient_id VARCHAR(50) NOT NULL,
    ingredient_name VARCHAR(100) NOT NULL,
    unit VARCHAR(20),
    request_quantity DECIMAL(10,3) NOT NULL,
    issue_quantity DECIMAL(10,3),
    unit_price DECIMAL(10,2),
    amount DECIMAL(12,2),
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_requisition_detail (requisition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 成本卡表
CREATE TABLE IF NOT EXISTS dish_cost_card (
    cost_card_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    dish_id VARCHAR(50) NOT NULL,
    dish_name VARCHAR(100) NOT NULL,
    dish_category VARCHAR(50),
    standard_yield DECIMAL(10,3),
    actual_yield DECIMAL(10,3),
    yield_rate DECIMAL(5,2),
    standard_cost DECIMAL(12,2),
    actual_cost DECIMAL(12,2),
    selling_price DECIMAL(12,2),
    gross_margin DECIMAL(5,2),
    status VARCHAR(20) DEFAULT 'active',
    effective_date DATETIME,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_costcard (store_id),
    INDEX idx_dish_costcard (dish_id),
    INDEX idx_status_costcard (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 成本卡明细表
CREATE TABLE IF NOT EXISTS dish_cost_card_detail (
    detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    cost_card_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    ingredient_id VARCHAR(50) NOT NULL,
    ingredient_name VARCHAR(100) NOT NULL,
    spec VARCHAR(100),
    unit VARCHAR(20),
    standard_quantity DECIMAL(10,3),
    actual_quantity DECIMAL(10,3),
    converted_quantity DECIMAL(10,3),
    unit_price DECIMAL(10,2),
    cost_amount DECIMAL(12,2),
    yield_rate DECIMAL(5,2),
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_costcard_detail (cost_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 出成率配置表
CREATE TABLE IF NOT EXISTS yield_rate_config (
    config_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    ingredient_id VARCHAR(50) NOT NULL,
    ingredient_name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    raw_unit VARCHAR(20) NOT NULL,
    processed_unit VARCHAR(20) NOT NULL,
    standard_yield_rate DECIMAL(5,2),
    min_yield_rate DECIMAL(5,2),
    max_yield_rate DECIMAL(5,2),
    loss_reason TEXT,
    status VARCHAR(20) DEFAULT 'active',
    effective_date DATETIME,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_yield (store_id),
    INDEX idx_ingredient_yield (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 单位换算配置表
CREATE TABLE IF NOT EXISTS unit_conversion (
    conversion_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id BIGINT NOT NULL DEFAULT 1,
    from_unit VARCHAR(20) NOT NULL,
    to_unit VARCHAR(20) NOT NULL,
    conversion_rate DECIMAL(15,6) NOT NULL,
    reverse_rate DECIMAL(15,6),
    category VARCHAR(50),
    description VARCHAR(200),
    status VARCHAR(20) DEFAULT 'active',
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_from_to_unit (from_unit, to_unit),
    INDEX idx_store_conversion (store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
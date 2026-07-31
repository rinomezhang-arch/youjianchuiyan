-- ========== 采购申请单 ==========
CREATE TABLE IF NOT EXISTS purchase_request (
    request_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    department_id VARCHAR(50),
    request_no VARCHAR(50) UNIQUE,
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_by VARCHAR(100),
    request_date DATE,
    expected_date DATE,
    total_amount DECIMAL(12,2),
    notes TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_id (store_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 采购申请明细 ==========
CREATE TABLE IF NOT EXISTS purchase_request_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    request_id BIGINT NOT NULL,
    ingredient_id VARCHAR(50),
    ingredient_name VARCHAR(200),
    category VARCHAR(50),
    quantity DECIMAL(10,3),
    unit VARCHAR(20),
    estimated_price DECIMAL(10,2),
    notes VARCHAR(500),
    INDEX idx_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 验收入库单 ==========
CREATE TABLE IF NOT EXISTS goods_receipt (
    receipt_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    request_id BIGINT,
    supplier_id BIGINT,
    receipt_no VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    received_by VARCHAR(100),
    inspected_by VARCHAR(100),
    receipt_date DATE,
    total_amount DECIMAL(12,2),
    qualified_amount DECIMAL(12,2),
    unqualified_amount DECIMAL(12,2),
    notes TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_id (store_id),
    INDEX idx_status (status),
    INDEX idx_request_id (request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 验收入库明细 ==========
CREATE TABLE IF NOT EXISTS goods_receipt_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    receipt_id BIGINT NOT NULL,
    ingredient_id VARCHAR(50),
    ingredient_name VARCHAR(200),
    ordered_qty DECIMAL(10,3),
    received_qty DECIMAL(10,3),
    qualified_qty DECIMAL(10,3),
    unit_price DECIMAL(10,2),
    amount DECIMAL(12,2),
    status VARCHAR(20),
    notes VARCHAR(500),
    INDEX idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 领料单 ==========
CREATE TABLE IF NOT EXISTS material_requisition (
    requisition_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    department_id VARCHAR(50),
    requisition_no VARCHAR(50),
    status VARCHAR(20) DEFAULT 'PENDING',
    requested_by VARCHAR(100),
    approved_by VARCHAR(100),
    requisition_date DATE,
    total_amount DECIMAL(12,2),
    notes TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_id (store_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 领料明细 ==========
CREATE TABLE IF NOT EXISTS material_requisition_item (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    requisition_id BIGINT NOT NULL,
    ingredient_id VARCHAR(50),
    ingredient_name VARCHAR(200),
    category VARCHAR(50),
    quantity DECIMAL(10,3),
    unit VARCHAR(20),
    unit_price DECIMAL(10,2),
    amount DECIMAL(12,2),
    notes VARCHAR(500),
    INDEX idx_requisition_id (requisition_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 初加工记录/出成率 ==========
CREATE TABLE IF NOT EXISTS preprocessing_record (
    record_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    ingredient_id VARCHAR(50),
    ingredient_name VARCHAR(200),
    raw_qty DECIMAL(10,3),
    processed_qty DECIMAL(10,3),
    yield_rate DECIMAL(5,2),
    unit VARCHAR(20),
    preprocessing_type VARCHAR(50),
    record_date DATE,
    operator VARCHAR(100),
    notes VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_store_id (store_id),
    INDEX idx_ingredient_id (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 成本卡 ==========
CREATE TABLE IF NOT EXISTS cost_card (
    cost_card_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    dish_id VARCHAR(50),
    dish_name VARCHAR(200),
    total_cost DECIMAL(12,2),
    material_cost DECIMAL(12,2),
    labor_cost DECIMAL(12,2),
    overhead_cost DECIMAL(12,2),
    cost_rate DECIMAL(5,2),
    sell_price DECIMAL(10,2),
    calculated_price DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'ACTIVE',
    effective_date DATE,
    notes TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_store_id (store_id),
    INDEX idx_dish_id (dish_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 成本卡明细 ==========
CREATE TABLE IF NOT EXISTS cost_card_detail (
    detail_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    cost_card_id BIGINT NOT NULL,
    ingredient_id VARCHAR(50),
    ingredient_name VARCHAR(200),
    category VARCHAR(50),
    quantity DECIMAL(10,3),
    unit VARCHAR(20),
    unit_price DECIMAL(10,2),
    amount DECIMAL(12,2),
    yield_rate DECIMAL(5,2),
    net_amount DECIMAL(12,2),
    INDEX idx_cost_card_id (cost_card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ========== 单位换算表 ==========
CREATE TABLE IF NOT EXISTS unit_conversion (
    conversion_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id BIGINT NOT NULL DEFAULT 1,
    from_unit VARCHAR(20),
    to_unit VARCHAR(20),
    conversion_rate DECIMAL(10,4),
    ingredient_id VARCHAR(50),
    notes VARCHAR(500),
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_store_id (store_id),
    INDEX idx_ingredient_id (ingredient_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- 椁愰ギ绠＄悊绯荤粺 瀹屾暣鏁版嵁搴撹璁?-- 鏁版嵁搴? banquet (MySQL 8.x)
-- 鐩爣: 90+ 寮犺〃锛岃鐩栧墠绔?7涓矾鐢遍〉闈㈢殑鍏ㄩ儴鏁版嵁闇€姹?-- 鍙傝€? 缇庡洟椁愰ギ/瀹㈠浜?浜岀淮鐏?澶╄储鍟嗛緳
-- ============================================================

-- ===== 1. 绯荤粺鍩虹 (8寮犺〃) =====

CREATE TABLE IF NOT EXISTS sys_store (
    store_id INT PRIMARY KEY AUTO_INCREMENT,
    store_name VARCHAR(100) NOT NULL,
    store_code VARCHAR(20),
    address VARCHAR(200),
    contact_phone VARCHAR(20),
    business_hours VARCHAR(50),
    timezone VARCHAR(50) DEFAULT 'Asia/Shanghai',
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_user (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(200) NOT NULL,
    real_name VARCHAR(50),
    phone VARCHAR(20),
    email VARCHAR(100),
    avatar_url VARCHAR(500),
    store_id INT,
    role VARCHAR(30) DEFAULT 'staff',
    is_active TINYINT DEFAULT 1,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (store_id) REFERENCES sys_store(store_id)
);

CREATE TABLE IF NOT EXISTS sys_role (
    role_id INT PRIMARY KEY AUTO_INCREMENT,
    role_name VARCHAR(50) NOT NULL,
    role_code VARCHAR(30) NOT NULL UNIQUE,
    description VARCHAR(200),
    is_system TINYINT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_permission (
    permission_id INT PRIMARY KEY AUTO_INCREMENT,
    permission_name VARCHAR(100) NOT NULL,
    permission_code VARCHAR(100) NOT NULL UNIQUE,
    module VARCHAR(50),
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS sys_role_permission (
    id INT PRIMARY KEY AUTO_INCREMENT,
    role_id INT NOT NULL,
    permission_id INT NOT NULL,
    FOREIGN KEY (role_id) REFERENCES sys_role(role_id),
    FOREIGN KEY (permission_id) REFERENCES sys_permission(permission_id)
);

CREATE TABLE IF NOT EXISTS sys_user_role (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    role_id INT NOT NULL,
    FOREIGN KEY (user_id) REFERENCES sys_user(user_id),
    FOREIGN KEY (role_id) REFERENCES sys_role(role_id)
);

CREATE TABLE IF NOT EXISTS sys_config (
    config_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    config_key VARCHAR(100) NOT NULL,
    config_value TEXT,
    config_type VARCHAR(20) DEFAULT 'string',
    description VARCHAR(200),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_store_key (store_id, config_key)
);

CREATE TABLE IF NOT EXISTS sys_operation_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    user_id INT,
    module VARCHAR(50),
    action VARCHAR(50),
    target_type VARCHAR(50),
    target_id VARCHAR(50),
    detail JSON,
    ip_address VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 2. 妗屽彴绠＄悊 (4寮犺〃) =====

CREATE TABLE IF NOT EXISTS table_master (
    table_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    table_number VARCHAR(10) NOT NULL,
    table_name VARCHAR(20),
    table_area VARCHAR(50),
    table_location VARCHAR(50),
    table_capacity INT DEFAULT 10,
    min_capacity INT DEFAULT 4,
    max_capacity INT DEFAULT 12,
    table_type VARCHAR(20) DEFAULT '澶у巺',
    table_status VARCHAR(20) DEFAULT 'available',
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    qr_code VARCHAR(200),
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS table_area (
    area_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    area_name VARCHAR(50) NOT NULL,
    area_code VARCHAR(20),
    floor_number INT DEFAULT 1,
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS table_type (
    type_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    type_name VARCHAR(50) NOT NULL,
    min_capacity INT DEFAULT 2,
    max_capacity INT DEFAULT 20,
    description VARCHAR(200),
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS table_status_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    table_id INT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    operator_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 3. 棰勮绠＄悊 (6寮犺〃) =====

CREATE TABLE IF NOT EXISTS booking_master (
    booking_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    booking_no VARCHAR(30) NOT NULL UNIQUE,
    booking_date DATE NOT NULL,
    booking_time TIME NOT NULL,
    time_type VARCHAR(10) DEFAULT 'dinner',
    customer_id INT,
    staff_id INT,
    guest_count INT DEFAULT 0,
    table_count INT DEFAULT 1,
    deposit DECIMAL(10,2) DEFAULT 0,
    total_amount DECIMAL(10,2) DEFAULT 0,
    booking_status VARCHAR(20) DEFAULT 'confirmed',
    banquet_name VARCHAR(100),
    occasion_type VARCHAR(20),
    special_request TEXT,
    source VARCHAR(20) DEFAULT 'phone',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_table (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    booking_id INT NOT NULL,
    table_id INT NOT NULL,
    table_number VARCHAR(10),
    guest_count INT DEFAULT 0,
    package_id INT,
    open_table_type VARCHAR(50),
    table_note TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking_master(booking_id),
    FOREIGN KEY (table_id) REFERENCES table_master(table_id)
);

CREATE TABLE IF NOT EXISTS booking_dish (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    booking_id INT NOT NULL,
    table_id INT,
    dish_id VARCHAR(20),
    dish_name VARCHAR(100),
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10,2),
    custom_name VARCHAR(100),
    dish_note TEXT,
    FOREIGN KEY (booking_id) REFERENCES booking_master(booking_id)
);

CREATE TABLE IF NOT EXISTS booking_status_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    booking_id INT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    operator_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_template (
    template_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    template_name VARCHAR(100) NOT NULL,
    occasion_type VARCHAR(20),
    default_guest_count INT DEFAULT 10,
    default_table_count INT DEFAULT 1,
    default_deposit DECIMAL(10,2) DEFAULT 0,
    default_dishes JSON,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS booking_waitlist (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    customer_name VARCHAR(50),
    customer_phone VARCHAR(20),
    guest_count INT,
    preferred_date DATE,
    preferred_time TIME,
    time_type VARCHAR(10),
    status VARCHAR(20) DEFAULT 'waiting',
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 4. 瀹㈡埛/浼氬憳 (6寮犺〃) =====

CREATE TABLE IF NOT EXISTS customer_master (
    customer_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    customer_name VARCHAR(50) NOT NULL,
    customer_phone VARCHAR(20),
    gender VARCHAR(5),
    birthday DATE,
    email VARCHAR(100),
    address VARCHAR(200),
    customer_preference TEXT,
    total_amount DECIMAL(12,2) DEFAULT 0,
    total_visits INT DEFAULT 0,
    member_level VARCHAR(10) DEFAULT 'v1',
    member_points INT DEFAULT 0,
    booking_count INT DEFAULT 0,
    last_booking_date DATE,
    source VARCHAR(20) DEFAULT 'walk-in',
    is_active TINYINT DEFAULT 1,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer_tag (
    tag_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    tag_name VARCHAR(50) NOT NULL,
    tag_color VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer_tag_relation (
    id INT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    tag_id INT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customer_master(customer_id),
    FOREIGN KEY (tag_id) REFERENCES customer_tag(tag_id)
);

CREATE TABLE IF NOT EXISTS member_level_config (
    level_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    level_code VARCHAR(10) NOT NULL,
    level_name VARCHAR(50) NOT NULL,
    min_points INT DEFAULT 0,
    discount_rate DECIMAL(3,2) DEFAULT 1.00,
    benefits TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS member_points_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id INT NOT NULL,
    points_change INT NOT NULL,
    points_balance INT NOT NULL,
    change_type VARCHAR(20),
    related_order_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS customer_feedback (
    feedback_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    customer_id INT,
    booking_id INT,
    rating INT,
    content TEXT,
    reply TEXT,
    reply_staff_id INT,
    status VARCHAR(20) DEFAULT 'pending',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 5. 鑿滃搧绠＄悊 (8寮犺〃) =====

CREATE TABLE IF NOT EXISTS dish_master (
    dish_id VARCHAR(20) NOT NULL,
    store_id INT NOT NULL DEFAULT 1,
    dish_name VARCHAR(100) NOT NULL,
    dish_category VARCHAR(50),
    spicy_level INT DEFAULT 0,
    main_ingredient_type VARCHAR(50),
    main_ingredient VARCHAR(100),
    english_name VARCHAR(200),
    cost_price DECIMAL(10,2) DEFAULT 0,
    sale_price DECIMAL(10,2) DEFAULT 0,
    cost_rate DECIMAL(5,2) DEFAULT 0,
    cooking_time INT DEFAULT 15,
    image_url VARCHAR(500),
    description TEXT,
    is_active TINYINT DEFAULT 1,
    is_soldout TINYINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (dish_id, store_id)
);

CREATE TABLE IF NOT EXISTS dish_category (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    category_name VARCHAR(50) NOT NULL,
    category_code VARCHAR(20),
    parent_id INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dish_occasion_names (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    dish_id VARCHAR(20) NOT NULL,
    occasion_type VARCHAR(20) NOT NULL,
    custom_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dish_recipe (
    recipe_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    dish_id VARCHAR(20) NOT NULL,
    ingredient_id VARCHAR(50) NOT NULL,
    ingredient_name VARCHAR(100),
    unit VARCHAR(20),
    unit_price DECIMAL(10,4) DEFAULT 0,
    quantity DECIMAL(10,3) DEFAULT 0,
    total_cost DECIMAL(10,2) DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dish_label (
    label_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    label_name VARCHAR(50) NOT NULL,
    label_color VARCHAR(20),
    label_icon VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dish_label_relation (
    id INT PRIMARY KEY AUTO_INCREMENT,
    dish_id VARCHAR(20) NOT NULL,
    store_id INT NOT NULL DEFAULT 1,
    label_id INT NOT NULL
);

CREATE TABLE IF NOT EXISTS dish_soldout_record (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    dish_id VARCHAR(20) NOT NULL,
    soldout_date DATE NOT NULL,
    time_type VARCHAR(10),
    reason VARCHAR(200),
    operator_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS dish_price_history (
    id INT PRIMARY KEY AUTO_INCREMENT,
    dish_id VARCHAR(20) NOT NULL,
    store_id INT NOT NULL DEFAULT 1,
    old_price DECIMAL(10,2),
    new_price DECIMAL(10,2),
    operator_id INT,
    reason VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 6. 濂楅绠＄悊 (4寮犺〃) =====

CREATE TABLE IF NOT EXISTS package_master (
    package_id VARCHAR(20) NOT NULL,
    store_id INT NOT NULL DEFAULT 1,
    package_name VARCHAR(100) NOT NULL,
    package_total_price DECIMAL(10,2) DEFAULT 0,
    package_cost_price DECIMAL(10,2) DEFAULT 0,
    cost_rate DECIMAL(5,2) DEFAULT 0,
    dish_count INT DEFAULT 0,
    suggest_guests INT DEFAULT 10,
    occasion_type VARCHAR(20),
    image_url VARCHAR(500),
    description TEXT,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (package_id, store_id)
);

CREATE TABLE IF NOT EXISTS package_dish_detail (
    detail_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    package_id VARCHAR(20) NOT NULL,
    dish_id VARCHAR(20) NOT NULL,
    dish_quantity INT DEFAULT 1,
    dish_order INT DEFAULT 0,
    custom_name VARCHAR(100),
    note TEXT
);

CREATE TABLE IF NOT EXISTS package_template (
    template_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    template_name VARCHAR(100),
    occasion_type VARCHAR(20),
    min_guests INT,
    max_guests INT,
    base_price DECIMAL(10,2),
    dishes JSON,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS festive_menu (
    menu_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    menu_name VARCHAR(100),
    festival_name VARCHAR(50),
    start_date DATE,
    end_date DATE,
    dishes JSON,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 7. 璁㈠崟绠＄悊 (8寮犺〃) =====

CREATE TABLE IF NOT EXISTS order_master (
    order_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    order_no VARCHAR(30) NOT NULL UNIQUE,
    booking_id INT,
    table_id INT,
    customer_id INT,
    staff_id INT,
    order_type VARCHAR(20) DEFAULT 'dine-in',
    order_status VARCHAR(20) DEFAULT 'pending',
    guest_count INT DEFAULT 0,
    total_amount DECIMAL(10,2) DEFAULT 0,
    discount_amount DECIMAL(10,2) DEFAULT 0,
    final_amount DECIMAL(10,2) DEFAULT 0,
    paid_amount DECIMAL(10,2) DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_item (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    dish_id VARCHAR(20),
    dish_name VARCHAR(100),
    quantity INT DEFAULT 1,
    unit_price DECIMAL(10,2),
    subtotal DECIMAL(10,2),
    item_status VARCHAR(20) DEFAULT 'pending',
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES order_master(order_id)
);

CREATE TABLE IF NOT EXISTS order_status_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20),
    operator_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS payment_record (
    payment_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    store_id INT NOT NULL DEFAULT 1,
    payment_method VARCHAR(20),
    amount DECIMAL(10,2),
    payment_status VARCHAR(20) DEFAULT 'success',
    transaction_no VARCHAR(100),
    operator_id INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS discount_record (
    discount_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    discount_type VARCHAR(20),
    discount_amount DECIMAL(10,2),
    discount_reason VARCHAR(200),
    approved_by INT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS shift_record (
    shift_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    shift_date DATE NOT NULL,
    shift_type VARCHAR(10),
    start_time TIME,
    end_time TIME,
    open_cash DECIMAL(10,2) DEFAULT 0,
    close_cash DECIMAL(10,2) DEFAULT 0,
    total_sales DECIMAL(10,2) DEFAULT 0,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS kitchen_ticket (
    ticket_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    item_id INT,
    dish_name VARCHAR(100),
    quantity INT,
    ticket_status VARCHAR(20) DEFAULT 'pending',
    kitchen_staff_id INT,
    printed_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS split_bill_record (
    split_id INT PRIMARY KEY AUTO_INCREMENT,
    order_id INT NOT NULL,
    split_type VARCHAR(20),
    split_count INT,
    split_detail JSON,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 8. 鍘熸枡/搴撳瓨 (10寮犺〃) =====

CREATE TABLE IF NOT EXISTS ingredient_master (
    ingredient_id VARCHAR(50) NOT NULL,
    store_id INT NOT NULL DEFAULT 1,
    ingredient_name VARCHAR(100) NOT NULL,
    ingredient_category VARCHAR(50),
    brand VARCHAR(100),
    purchase_unit VARCHAR(20),
    usage_unit VARCHAR(20),
    conversion_rate DECIMAL(10,3) DEFAULT 1,
    primary_supplier_id INT,
    current_stock DECIMAL(12,3) DEFAULT 0,
    warning_threshold DECIMAL(12,3) DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (ingredient_id, store_id)
);

CREATE TABLE IF NOT EXISTS ingredient_category (
    category_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    category_name VARCHAR(50) NOT NULL,
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_in (
    in_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    ingredient_id VARCHAR(50) NOT NULL,
    supplier_id INT,
    quantity DECIMAL(12,3) NOT NULL,
    unit_price DECIMAL(10,4),
    total_amount DECIMAL(10,2),
    in_date DATE,
    batch_no VARCHAR(50),
    expiry_date DATE,
    operator_id INT,
    purchase_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_out (
    out_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    ingredient_id VARCHAR(50) NOT NULL,
    quantity DECIMAL(12,3) NOT NULL,
    usage_unit_price DECIMAL(10,4),
    out_type VARCHAR(20),
    related_order_id INT,
    related_dish_id VARCHAR(20),
    operator_id INT,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_log (
    log_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    ingredient_id VARCHAR(50) NOT NULL,
    log_type VARCHAR(20) NOT NULL,
    log_quantity DECIMAL(12,3),
    stock_before DECIMAL(12,3),
    stock_after DECIMAL(12,3),
    related_id INT,
    operator_id INT,
    note TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS inventory_warning (
    warning_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    ingredient_id VARCHAR(50) NOT NULL,
    warning_type VARCHAR(20),
    current_stock DECIMAL(12,3),
    threshold DECIMAL(12,3),
    is_resolved TINYINT DEFAULT 0,
    resolved_by INT,
    resolved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock_take (
    take_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    take_date DATE NOT NULL,
    take_type VARCHAR(20) DEFAULT 'full',
    status VARCHAR(20) DEFAULT 'in-progress',
    operator_id INT,
    reviewer_id INT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS stock_take_detail (
    detail_id INT PRIMARY KEY AUTO_INCREMENT,
    take_id INT NOT NULL,
    ingredient_id VARCHAR(50) NOT NULL,
    system_stock DECIMAL(12,3),
    actual_stock DECIMAL(12,3),
    diff_quantity DECIMAL(12,3),
    diff_amount DECIMAL(10,2),
    remark VARCHAR(200),
    FOREIGN KEY (take_id) REFERENCES stock_take(take_id)
);

CREATE TABLE IF NOT EXISTS purchase_order (
    po_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    po_no VARCHAR(30) NOT NULL UNIQUE,
    supplier_id INT,
    order_date DATE,
    expected_date DATE,
    total_amount DECIMAL(10,2),
    status VARCHAR(20) DEFAULT 'draft',
    operator_id INT,
    auditor_id INT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS purchase_order_item (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    po_id INT NOT NULL,
    ingredient_id VARCHAR(50) NOT NULL,
    quantity DECIMAL(12,3),
    unit_price DECIMAL(10,4),
    received_quantity DECIMAL(12,3) DEFAULT 0,
    remark VARCHAR(200),
    FOREIGN KEY (po_id) REFERENCES purchase_order(po_id)
);

-- ===== 9. 渚涘簲鍟?(3寮犺〃) =====

CREATE TABLE IF NOT EXISTS supplier_master (
    supplier_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    supplier_name VARCHAR(100) NOT NULL,
    contact_person VARCHAR(50),
    contact_phone VARCHAR(20),
    address VARCHAR(200),
    bank_account VARCHAR(50),
    bank_name VARCHAR(100),
    wechat_account VARCHAR(50),
    alipay_account VARCHAR(50),
    main_products TEXT,
    payment_terms VARCHAR(50),
    rating INT DEFAULT 5,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier_reconciliation (
    recon_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    supplier_id INT NOT NULL,
    recon_month VARCHAR(7),
    system_amount DECIMAL(12,2),
    supplier_amount DECIMAL(12,2),
    diff_amount DECIMAL(12,2),
    status VARCHAR(20) DEFAULT 'pending',
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS supplier_evaluation (
    eval_id INT PRIMARY KEY AUTO_INCREMENT,
    supplier_id INT NOT NULL,
    eval_date DATE,
    quality_score INT,
    delivery_score INT,
    price_score INT,
    service_score INT,
    total_score INT,
    evaluator_id INT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- ===== 10. 鍛樺伐/HR (14寮犺〃) =====

CREATE TABLE IF NOT EXISTS staff_master (
    staff_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_no VARCHAR(20) UNIQUE,
    staff_name VARCHAR(50) NOT NULL,
    staff_account VARCHAR(50) UNIQUE,
    staff_password VARCHAR(200),
    gender VARCHAR(2),
    age INT,
    phone VARCHAR(20),
    id_card VARCHAR(20),
    department VARCHAR(50),
    position VARCHAR(50),
    role VARCHAR(20) DEFAULT 'staff',
    employment_status VARCHAR(10) DEFAULT 'active',
    employee_type VARCHAR(20) DEFAULT '鍏ㄨ亴',
    join_date DATE,
    leave_date DATE,
    base_salary DECIMAL(10,2),
    permission_level INT DEFAULT 1,
    avatar_url VARCHAR(500),
    emergency_contact VARCHAR(50),
    emergency_phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staff_department (
    dept_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    dept_name VARCHAR(50) NOT NULL,
    dept_code VARCHAR(20),
    manager_id INT,
    parent_id INT DEFAULT 0,
    sort_order INT DEFAULT 0,
    is_active TINYINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS attendance_record (
    record_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    attend_date DATE NOT NULL,
    clock_in TIME,
    clock_out TIME,
    work_hours DECIMAL(5,2),
    overtime_hours DECIMAL(5,2),
    attend_status VARCHAR(20) DEFAULT 'normal',
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_staff_date (staff_id, attend_date)
);

CREATE TABLE IF NOT EXISTS attendance_monthly (
    id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    month VARCHAR(7) NOT NULL,
    work_days INT DEFAULT 0,
    actual_days INT DEFAULT 0,
    late_count INT DEFAULT 0,
    early_leave_count INT DEFAULT 0,
    absent_count INT DEFAULT 0,
    leave_days DECIMAL(5,1) DEFAULT 0,
    overtime_hours DECIMAL(6,2) DEFAULT 0,
    public_holiday_days INT DEFAULT 0,
    carry_over DECIMAL(5,1) DEFAULT 0,
    final_balance DECIMAL(5,1) DEFAULT 0,
    employment VARCHAR(20),
    salary_status VARCHAR(20),
    summary_notes TEXT,
    created_by VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_staff_month (staff_id, month)
);

CREATE TABLE IF NOT EXISTS leave_record (
    leave_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    leave_type VARCHAR(20),
    start_date DATE,
    end_date DATE,
    leave_days DECIMAL(5,1),
    reason TEXT,
    approval_status VARCHAR(20) DEFAULT 'pending',
    approved_by INT,
    approved_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS schedule_record (
    schedule_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    schedule_date DATE NOT NULL,
    shift_type VARCHAR(20),
    start_time TIME,
    end_time TIME,
    is_off TINYINT DEFAULT 0,
    remark VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS training_record (
    training_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    training_name VARCHAR(100),
    training_type VARCHAR(50),
    trainer VARCHAR(50),
    training_date DATE,
    duration_hours DECIMAL(5,1),
    attendees JSON,
    content TEXT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS staff_performance (
    perf_id INT PRIMARY KEY AUTO_INCREMENT,
    store_id INT NOT NULL DEFAULT 1,
    staff_id INT NOT NULL,
    eval_month VARCHAR(7),
    sales_amount DECIMAL(12,2),
    customer_count INT,
    attendance_score INT,
    service_score INT,
    total_score INT,
    rank_in_dept INT,
    remark TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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
    status VARCHAR
-- 鎺?鏁版嵁搴撹璁?瀹屾暣鐗?sql

-- ===== 10. 鍛樺伐/HR (缁? =====

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

-- ===== 11. 璐㈠姟 (6寮犺〃) =====

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

-- ===== 12. 钀ラ攢/浼氬憳 (6寮犺〃) =====

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

-- ===== 13. 琛屾斂/璧勪骇 (6寮犺〃) =====

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

-- ===== 14. 璇佺収绠＄悊 (2寮犺〃) =====

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

-- ===== 15. 鏁版嵁鍒嗘瀽/鎶ヨ〃 (4寮犺〃) =====

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

-- ===== 16. AI鍔╂墜 (2寮犺〃) =====

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

-- ===== 绱㈠紩 =====
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

-- ===== 鍒濆鏁版嵁 =====
INSERT IGNORE INTO sys_store (store_id, store_name, store_code) VALUES (1, '瀹佸浗搴?, 'NG001');
INSERT IGNORE INTO sys_user (user_id, username, password_hash, real_name, store_id, role) VALUES (1, 'rino', '$2a$10$xxx', 'Rino', 1, 'admin');
INSERT IGNORE INTO sys_role (role_id, role_name, role_code, is_system) VALUES (1, '绠＄悊鍛?, 'admin', 1), (2, '搴楅暱', 'manager', 1), (3, '鍛樺伐', 'staff', 1);


-- ============================================================
-- 从审计报告生成的建表SQL (v5)
-- 共 85 张表
-- ============================================================

-- 系统管理员表
CREATE TABLE IF NOT EXISTS `admin_users` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `username` varchar(64) NOT NULL,
  `password` varchar(255) NOT NULL,
  `real_name` varchar(50),
  `role` varchar(30) NOT NULL DEFAULT 'staff',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统管理员表';

-- AI对话历史表
CREATE TABLE IF NOT EXISTS `ai_chat_history` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `staff_id` int,
  `role` varchar(30),
  `content` text NOT NULL,
  `image_url` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_created_at` (`created_at`),
  KEY `idx_staff_id` (`staff_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话历史表';

-- AI记忆表
CREATE TABLE IF NOT EXISTS `ai_memory` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` varchar(64),
  `content` text NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_store_id` (`store_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI记忆表';

-- 考勤明细表
CREATE TABLE IF NOT EXISTS `attendance` (
  `attendance_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  `attendance_date` date NOT NULL,
  `clock_in` datetime,
  `clock_out` datetime,
  `status` varchar(32) DEFAULT 'normal',
  `late_minutes` int DEFAULT 0,
  `early_leave_minutes` int DEFAULT 0,
  `absent` tinyint(1) DEFAULT 0,
  `work_hours` decimal(10,3) DEFAULT 0.000,
  `remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_attendance_date` (`attendance_date`),
  KEY `idx_staff_date` (`staff_id`, `attendance_date`),
  KEY `idx_store_att` (`store_id`, `staff_id`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤明细表';

-- 考勤月度汇总表
CREATE TABLE IF NOT EXISTS `attendance_records` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `record_id` varchar(50) NOT NULL,
  `staff_id` varchar(50),
  `staff_name` varchar(50),
  `department` varchar(50),
  `month` varchar(7) NOT NULL,
  `scope` varchar(10) NOT NULL DEFAULT 'full',
  `day_num` int NOT NULL,
  `am_type` varchar(20),
  `pm_type` varchar(20),
  `am_note` text,
  `pm_note` text,
  `day_note` text,
  `employment` varchar(20) DEFAULT '全勤在职',
  `salary_status` varchar(20) DEFAULT '未发放',
  `public_holiday` int DEFAULT 6,
  `carry_over` int DEFAULT 0,
  `summary_notes` text,
  `total_present` decimal(12,2) DEFAULT 0.00,
  `total_statutory` decimal(12,2) DEFAULT 0.00,
  `total_holiday` decimal(12,2) DEFAULT 0.00,
  `total_comp` decimal(12,2) DEFAULT 0.00,
  `total_travel` decimal(12,2) DEFAULT 0.00,
  `total_overtime` decimal(12,2) DEFAULT 0.00,
  `total_leave` decimal(12,2) DEFAULT 0.00,
  `total_late` decimal(12,2) DEFAULT 0.00,
  `total_early` decimal(12,2) DEFAULT 0.00,
  `total_absent` decimal(12,2) DEFAULT 0.00,
  `final_balance` decimal(12,2) DEFAULT 0.00,
  `recorded_days` int DEFAULT 0,
  `created_by` varchar(50) DEFAULT 'Rino',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_emp_id` (`staff_id`),
  KEY `idx_month` (`month`),
  KEY `idx_store_attr` (`store_id`, `staff_id`, `month`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `record_id` (`record_id`),
  UNIQUE KEY `uk_emp_month` (`staff_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤月度汇总表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `user_id` varchar(64),
  `action` varchar(128),
  `target` varchar(256),
  `detail` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- 宴会菜单模板表
CREATE TABLE IF NOT EXISTS `banquet_template` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `template_name` varchar(100) NOT NULL,
  `template_code` varchar(50) NOT NULL,
  `template_type` varchar(20) NOT NULL,
  `description` varchar(200),
  `base_price` decimal(12,2),
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宴会菜单模板表';

-- 宴会类型-模板关联表
CREATE TABLE IF NOT EXISTS `banquet_template_rel` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `banquet_type_id` int NOT NULL,
  `template_id` int NOT NULL,
  `is_default` tinyint DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_store_id` (`store_id`),
  KEY `template_id` (`template_id`),
  UNIQUE KEY `uk_banquet_template` (`banquet_type_id`, `template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宴会类型-模板关联表';

-- 宴会类型表
CREATE TABLE IF NOT EXISTS `banquet_type` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `type_name` varchar(50) NOT NULL,
  `type_code` varchar(50) NOT NULL,
  `description` varchar(200),
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宴会类型表';

-- 订菜明细表
CREATE TABLE IF NOT EXISTS `booking_dish_detail` (
  `dish_booking_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `table_booking_id` bigint,
  `booking_id` varchar(20),
  `dish_id` varchar(20) NOT NULL,
  `dish_name` varchar(100),
  `dish_quantity` int DEFAULT 1,
  `unit_price` decimal(12,2) DEFAULT 0.00,
  `subtotal` decimal(12,2) DEFAULT 0.00,
  `custom_name` varchar(100),
  `dish_note` text,
  `dish_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `kitchen_status` varchar(20) DEFAULT 'pending',
  `kitchen_station` varchar(50),
  `kitchen_note` text,
  `kitchen_started_at` bigint,
  `kitchen_done_at` bigint,
  KEY `idx_booking` (`booking_id`),
  KEY `idx_booking_dish_booking` (`booking_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_table_booking` (`table_booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订菜明细表';

-- 预订主档表
CREATE TABLE IF NOT EXISTS `booking_master` (
  `booking_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `booking_date` date NOT NULL,
  `booking_time` time NOT NULL,
  `customer_id` int,
  `customer_name` varchar(50),
  `customer_phone` varchar(20),
  `staff_id` int,
  `staff_name` varchar(20),
  `deposit` decimal(12,2) DEFAULT 0.00,
  `guest_count` int DEFAULT 0,
  `table_count` int DEFAULT 0,
  `spare_tables` int DEFAULT 0,
  `guest_per_table` int DEFAULT 10,
  `booking_status` varchar(20) NOT NULL DEFAULT 'pending',
  `banquet_name` varchar(100),
  `occasion_type` varchar(20),
  `special_request` text,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `final_amount` decimal(12,2) DEFAULT 0.00,
  `payment_status` varchar(20) DEFAULT 'unpaid',
  `remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `booking_no` varchar(30),
  `package_id` varchar(20),
  `booking_type` varchar(20) DEFAULT 'normal',
  `deposit_amount` decimal(12,2) DEFAULT 0.00,
  `package_name` varchar(100),
  `status` varchar(32) DEFAULT 'pending',
  PRIMARY KEY (`booking_id`, `store_id`),
  KEY `fk_bm_staff` (`staff_id`),
  KEY `idx_booking_customer` (`customer_id`),
  KEY `idx_booking_date_status` (`booking_date`, `booking_status`),
  KEY `idx_customer` (`customer_id`),
  KEY `idx_date` (`booking_date`),
  KEY `idx_phone` (`customer_phone`),
  KEY `idx_status` (`booking_status`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_booking` (`store_id`, `booking_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='预订主档表';

-- 订桌明细表
CREATE TABLE IF NOT EXISTS `booking_table` (
  `table_booking_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `booking_id` varchar(20) NOT NULL,
  `booking_date` date NOT NULL,
  `booking_time` time NOT NULL,
  `table_id` int NOT NULL,
  `table_number` varchar(10),
  `table_name` varchar(50),
  `guest_count` int DEFAULT 0,
  `package_id` varchar(20),
  `package_name` varchar(100),
  `open_table_type` varchar(50),
  `table_note` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_booking` (`booking_id`),
  KEY `idx_booking_table_booking` (`booking_id`),
  KEY `idx_booking_table_table` (`table_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_table` (`table_id`),
  UNIQUE KEY `uk_table_date_time` (`table_id`, `booking_date`, `booking_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订桌明细表';

-- 系统变更日志表
CREATE TABLE IF NOT EXISTS `change_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `operator_id` int,
  `operator_name` varchar(50),
  `operation_type` varchar(30) NOT NULL,
  `target_type` varchar(30) NOT NULL,
  `target_id` varchar(50),
  `summary` varchar(200) NOT NULL,
  `detail` text,
  `old_value` text,
  `new_value` text,
  `ip_address` varchar(128),
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_changelog_time` (`created_at`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_store_time` (`store_id`, `created_at`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_type` (`operation_type`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统变更日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `config` (
  `config_key` varchar(128) NOT NULL,
  `config_value` text,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`config_key`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 客户主档表
CREATE TABLE IF NOT EXISTS `customer_master` (
  `customer_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `customer_name` varchar(50) NOT NULL,
  `customer_phone` varchar(20) NOT NULL,
  `customer_preference` text,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `member_level` varchar(10) DEFAULT 'v1',
  `booking_count` int DEFAULT 0,
  `last_booking_date` date,
  `remark` text,
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_level` (`member_level`),
  KEY `idx_name` (`customer_name`),
  KEY `idx_phone` (`customer_phone`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_customer` (`store_id`, `customer_phone`),
  UNIQUE KEY `uk_store_name_phone` (`store_id`, `customer_name`, `customer_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户主档表';

-- 部门表
CREATE TABLE IF NOT EXISTS `department` (
  `dept_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dept_name` varchar(50) NOT NULL,
  `dept_code` varchar(20),
  `parent_id` int,
  `sort_order` int DEFAULT 0,
  `status` varchar(32) DEFAULT 'active',
  `description` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `level` int DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 菜品分类表
CREATE TABLE IF NOT EXISTS `dish_category` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `category_name` varchar(50) NOT NULL,
  `category_code` varchar(50) NOT NULL,
  `description` varchar(200),
  `sort_order` int DEFAULT 0,
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品分类表';

-- 菜品主档表
CREATE TABLE IF NOT EXISTS `dish_master` (
  `dish_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dish_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `category_id` varchar(64),
  `dish_category` varchar(50),
  `spicy_level` int DEFAULT 0,
  `main_ingredient_type` varchar(50),
  `main_ingredient` varchar(100),
  `english_name` varchar(200),
  `cost_price` decimal(12,2) DEFAULT 0.00,
  `sale_price` decimal(12,2) DEFAULT 0.00,
  `cost_rate` decimal(12,2) DEFAULT 0.00,
  `cooking_time` int DEFAULT 15,
  `servings` int DEFAULT 1,
  `birthday_name` varchar(100),
  `wedding_name` varchar(100),
  `house_move_name` varchar(100),
  `promotion_name` varchar(100),
  `reunion_name` varchar(100),
  `thanksgiving_name` varchar(100),
  `year_end_name` varchar(100),
  `baby_born_name` varchar(100),
  `is_active` tinyint DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `usage_type` varchar(20) DEFAULT 'unused',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `image_url` varchar(500),
  `festive_name` varchar(100),
  `cooking_method` varchar(50),
  `dish_code` varchar(64),
  `dish_name_en` varchar(100),
  `is_seasonal` int DEFAULT 0,
  `is_specialty` int DEFAULT 0,
  `main_ingredients` text,
  `taste` varchar(50),
  `unit` varchar(32) DEFAULT '?',
  `price` decimal(12,2) DEFAULT 0.00,
  `remark` text,
  PRIMARY KEY (`dish_id`, `store_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_category` (`dish_category`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_dish` (`store_id`, `dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品主档表';

-- 菜品场合别名表
CREATE TABLE IF NOT EXISTS `dish_occasion_names` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dish_id` varchar(20) NOT NULL,
  `occasion_type` varchar(20) NOT NULL,
  `custom_name` varchar(100) NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_dish` (`dish_id`),
  KEY `idx_occasion` (`occasion_type`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品场合别名表';

-- 菜品配方表
CREATE TABLE IF NOT EXISTS `dish_recipe` (
  `recipe_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dish_id` varchar(20) NOT NULL,
  `ingredient_id` varchar(50) NOT NULL,
  `ingredient_name` varchar(100),
  `unit` varchar(32),
  `unit_price` decimal(12,2) DEFAULT 0.00,
  `quantity` decimal(10,3) DEFAULT 0.000,
  `total_cost` decimal(12,2) DEFAULT 0.00,
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `wastage_rate` decimal(5,2) DEFAULT 0.00,
  `yield_rate` decimal(5,2) DEFAULT 0.00,
  `last_entry_date` date,
  `net_unit_price` decimal(12,2) DEFAULT 0.00,
  KEY `idx_dish` (`dish_id`),
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品配方表';

-- 菜品标签表
CREATE TABLE IF NOT EXISTS `dish_tag` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `tag_name` varchar(50) NOT NULL,
  `tag_code` varchar(50) NOT NULL,
  `tag_type` varchar(20) NOT NULL,
  `dish_category` varchar(50),
  `sort_order` int DEFAULT 0,
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `import_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `menu_date` date,
  UNIQUE KEY `tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品标签表';

-- 菜品标签关联表
CREATE TABLE IF NOT EXISTS `dish_tag_relation` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dish_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `tag_id` int NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `tag_id` (`tag_id`),
  UNIQUE KEY `uk_dish_tag` (`dish_id`, `store_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品标签关联表';

-- 菜品用途表
CREATE TABLE IF NOT EXISTS `dish_usage` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `usage_name` varchar(20) NOT NULL,
  `usage_code` varchar(20) NOT NULL,
  `description` varchar(200),
  `is_active` tinyint DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `usage_code` (`usage_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品用途表';

-- 菜品用途关联表
CREATE TABLE IF NOT EXISTS `dish_usage_relation` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dish_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `usage_id` int NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_dish_usage` (`dish_id`, `store_id`, `usage_id`),
  KEY `usage_id` (`usage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品用途关联表';

-- 员工生命周期表
CREATE TABLE IF NOT EXISTS `employee_lifecycle` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `staff_id` varchar(50),
  `staff_name` varchar(50),
  `event_type` varchar(20) NOT NULL,
  `event_date` date NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_emp_id` (`staff_id`),
  KEY `idx_event_date` (`event_date`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_store_life` (`store_id`, `staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工生命周期表';

-- 财务账户表
CREATE TABLE IF NOT EXISTS `finance_account` (
  `account_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `account_code` varchar(50) NOT NULL,
  `account_name` varchar(100) NOT NULL,
  `account_type` varchar(20) NOT NULL,
  `bank_name` varchar(100),
  `bank_account` varchar(50),
  `account_holder` varchar(50),
  `initial_balance` decimal(12,2) NOT NULL DEFAULT 0.00,
  `current_balance` decimal(12,2) NOT NULL DEFAULT 0.00,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_account_type` (`account_type`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_account_code` (`account_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务账户表';

-- 成本记录表
CREATE TABLE IF NOT EXISTS `finance_cost_record` (
  `cost_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `cost_date` date NOT NULL,
  `cost_type` varchar(50) NOT NULL,
  `cost_category` varchar(50),
  `amount` decimal(12,2) NOT NULL,
  `related_type` varchar(50),
  `related_id` bigint,
  `department_id` int,
  `department` varchar(50),
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本记录表';

-- 费用报销表
CREATE TABLE IF NOT EXISTS `finance_expense` (
  `expense_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `expense_no` varchar(50) NOT NULL,
  `expense_type` varchar(50),
  `expense_date` date NOT NULL,
  `applicant_id` int,
  `applicant_name` varchar(50),
  `department_id` int,
  `department` varchar(50),
  `amount` decimal(12,2) NOT NULL,
  `invoice_amount` decimal(12,2),
  `approval_status` varchar(20) NOT NULL DEFAULT 'pending',
  `approver_id` int,
  `approver_name` varchar(50),
  `approve_time` datetime,
  `approve_remark` varchar(500),
  `payment_status` varchar(20) DEFAULT 'unpaid',
  `payment_time` datetime,
  `account_id` bigint,
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_expense_no` (`expense_no`),
  KEY `idx_expense_type` (`expense_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用报销表';

-- 应付账款表
CREATE TABLE IF NOT EXISTS `finance_payable` (
  `payable_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `payable_no` varchar(50) NOT NULL,
  `supplier_id` int,
  `supplier_name` varchar(100),
  `purchase_id` int,
  `purchase_no` varchar(50),
  `total_amount` decimal(12,2) NOT NULL,
  `paid_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `pending_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `payable_date` date,
  `due_date` date,
  `status` varchar(20) NOT NULL DEFAULT 'unpaid',
  `credit_days` int,
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_due_date` (`due_date`),
  KEY `idx_payable_no` (`payable_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应付账款表';

-- 收款记录表
CREATE TABLE IF NOT EXISTS `finance_payment_record` (
  `payment_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `payment_no` varchar(50) NOT NULL,
  `payment_date` date NOT NULL,
  `receivable_id` bigint,
  `customer_id` int,
  `customer_name` varchar(100),
  `booking_id` int,
  `booking_no` varchar(50),
  `amount` decimal(12,2) NOT NULL,
  `payment_method` varchar(20),
  `account_id` bigint,
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款记录表';

-- 应收账款表
CREATE TABLE IF NOT EXISTS `finance_receivable` (
  `receivable_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `receivable_no` varchar(50) NOT NULL,
  `customer_id` int,
  `customer_name` varchar(100),
  `booking_id` int,
  `booking_no` varchar(50),
  `total_amount` decimal(12,2) NOT NULL,
  `received_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `pending_amount` decimal(12,2) NOT NULL DEFAULT 0.00,
  `receivable_date` date,
  `due_date` date,
  `status` varchar(20) NOT NULL DEFAULT 'unpaid',
  `credit_days` int,
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_receivable_no` (`receivable_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款表';

-- 对账记录表
CREATE TABLE IF NOT EXISTS `finance_reconciliation` (
  `recon_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `recon_no` varchar(50) NOT NULL,
  `recon_date` date NOT NULL,
  `account_id` bigint,
  `account_name` varchar(100),
  `book_balance` decimal(12,2),
  `bank_balance` decimal(12,2),
  `diff_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'pending',
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_account_id` (`account_id`),
  KEY `idx_recon_no` (`recon_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账记录表';

-- 结算记录表
CREATE TABLE IF NOT EXISTS `finance_settlement` (
  `settlement_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `settlement_no` varchar(50) NOT NULL,
  `settlement_date` date NOT NULL,
  `settlement_type` varchar(20) NOT NULL,
  `start_date` date,
  `end_date` date,
  `total_income` decimal(12,2) DEFAULT 0.00,
  `total_expense` decimal(12,2) DEFAULT 0.00,
  `total_profit` decimal(12,2) DEFAULT 0.00,
  `food_cost` decimal(12,2) DEFAULT 0.00,
  `labor_cost` decimal(12,2) DEFAULT 0.00,
  `rent_cost` decimal(12,2) DEFAULT 0.00,
  `utility_cost` decimal(12,2) DEFAULT 0.00,
  `other_cost` decimal(12,2) DEFAULT 0.00,
  `cost_rate` decimal(5,2),
  `status` varchar(20) DEFAULT 'draft',
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_settlement_date` (`settlement_date`),
  KEY `idx_settlement_no` (`settlement_no`),
  KEY `idx_settlement_type` (`settlement_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算记录表';

-- 收支流水表
CREATE TABLE IF NOT EXISTS `finance_transaction` (
  `trans_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `trans_no` varchar(50) NOT NULL,
  `trans_date` date NOT NULL,
  `trans_time` datetime NOT NULL,
  `trans_type` varchar(20) NOT NULL,
  `trans_category` varchar(50),
  `account_id` bigint,
  `related_type` varchar(50),
  `related_id` bigint,
  `related_no` varchar(50),
  `amount` decimal(12,2) NOT NULL,
  `balance_after` decimal(12,2),
  `payer_payee` varchar(100),
  `payment_method` varchar(20),
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_account_id` (`account_id`),
  KEY `idx_related` (`related_type`, `related_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_trans_date` (`trans_date`),
  KEY `idx_trans_no` (`trans_no`),
  KEY `idx_trans_type` (`trans_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支流水表';

-- 会计凭证表
CREATE TABLE IF NOT EXISTS `finance_voucher` (
  `voucher_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `voucher_no` varchar(50) NOT NULL,
  `voucher_date` date NOT NULL,
  `voucher_type` varchar(20) DEFAULT 'transfer',
  `summary` varchar(200),
  `total_debit` decimal(12,2) NOT NULL DEFAULT 0.00,
  `total_credit` decimal(12,2) NOT NULL DEFAULT 0.00,
  `is_balanced` tinyint DEFAULT 1,
  `status` varchar(20) DEFAULT 'draft',
  `prepared_by` int,
  `prepared_name` varchar(50),
  `audited_by` int,
  `audited_name` varchar(50),
  `audited_at` datetime,
  `posted_by` int,
  `posted_at` datetime,
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_voucher_date` (`voucher_date`),
  KEY `idx_voucher_no` (`voucher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计凭证表';

-- 会计凭证明细表
CREATE TABLE IF NOT EXISTS `finance_voucher_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `voucher_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `line_no` int NOT NULL,
  `subject_code` varchar(50) NOT NULL,
  `subject_name` varchar(100) NOT NULL,
  `summary` varchar(200),
  `debit_amount` decimal(12,2) DEFAULT 0.00,
  `credit_amount` decimal(12,2) DEFAULT 0.00,
  `related_type` varchar(50),
  `related_id` bigint,
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_store_id` (`store_id`),
  KEY `idx_subject_code` (`subject_code`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计凭证明细表';

-- 库存变动日志表
CREATE TABLE IF NOT EXISTS `ingredient_inventory_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` varchar(50) NOT NULL,
  `log_type` varchar(20) NOT NULL,
  `log_quantity` decimal(10,3) NOT NULL,
  `stock_after` decimal(10,3) DEFAULT 0.000,
  `log_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  `related_order_id` varchar(50),
  `operator_id` int,
  `note` text,
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_time` (`log_time`),
  KEY `idx_type` (`log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表';

-- 食材/原料主档表
CREATE TABLE IF NOT EXISTS `ingredient_master` (
  `ingredient_id` varchar(50) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_name` varchar(100) NOT NULL,
  `ingredient_category` varchar(50),
  `brand` varchar(100),
  `purchase_unit` varchar(20),
  `usage_unit` varchar(20),
  `conversion_rate` decimal(5,2) DEFAULT 1.00,
  `primary_supplier_id` int,
  `current_stock` decimal(10,3) DEFAULT 0.000,
  `warning_threshold` decimal(12,2) DEFAULT 0.00,
  `avg_price` decimal(12,2) DEFAULT 0.00,
  `yield_rate` decimal(5,2) DEFAULT 0.00,
  `last_entry_date` date,
  `is_active` tinyint DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`ingredient_id`, `store_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_category` (`ingredient_category`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_ing` (`store_id`, `ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食材/原料主档表';

-- 采购记录表
CREATE TABLE IF NOT EXISTS `ingredient_purchase` (
  `purchase_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` varchar(50) NOT NULL,
  `supplier_id` int,
  `purchase_date` date NOT NULL,
  `purchase_quantity` decimal(10,3) DEFAULT 0.000,
  `purchase_price` decimal(12,2) DEFAULT 0.00,
  `purchase_total` decimal(12,2) DEFAULT 0.00,
  `usage_quantity` decimal(10,3) DEFAULT 0.000,
  `usage_price` decimal(12,2) DEFAULT 0.00,
  `processing_note` text,
  `operator_id` int,
  `status` varchar(32) DEFAULT 'completed',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_date` (`purchase_date`),
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_purchase_date` (`purchase_date`),
  KEY `idx_store` (`store_id`),
  KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购记录表';

-- 厨房操作日志表
CREATE TABLE IF NOT EXISTS `kitchen_log` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `action` varchar(128) NOT NULL,
  `target_type` varchar(30) NOT NULL,
  `booking_id` varchar(20),
  `dish_id` varchar(20),
  `dish_name` varchar(100),
  `operator_id` int,
  `operator_name` varchar(50),
  `note` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_booking` (`booking_id`),
  KEY `idx_created` (`created_at`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='厨房操作日志表';

-- 请假记录表
CREATE TABLE IF NOT EXISTS `leave_record` (
  `leave_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  `leave_type` varchar(20) NOT NULL,
  `start_date` date NOT NULL,
  `end_date` date NOT NULL,
  `days` decimal(10,3) DEFAULT 0.000,
  `status` varchar(32) DEFAULT 'pending',
  `reason` varchar(500),
  `approver_id` int,
  `approve_time` datetime,
  `approve_remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_staff_status` (`staff_id`, `status`),
  KEY `idx_store_leave` (`store_id`, `staff_id`, `start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假记录表';

-- 营销活动表
CREATE TABLE IF NOT EXISTS `marketing_activity` (
  `activity_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `activity_code` varchar(50) NOT NULL,
  `activity_name` varchar(100) NOT NULL,
  `activity_type` varchar(50) NOT NULL,
  `start_date` date,
  `end_date` date,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `activity_rules` text,
  `activity_content` text,
  `target_customers` varchar(50),
  `budget_amount` decimal(12,2),
  `actual_cost` decimal(12,2) DEFAULT 0.00,
  `expected_income` decimal(12,2),
  `actual_income` decimal(12,2) DEFAULT 0.00,
  `participant_count` int DEFAULT 0,
  `operator_id` int,
  `operator_name` varchar(50),
  `description` varchar(500),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_activity_code` (`activity_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动表';

-- 优惠券表
CREATE TABLE IF NOT EXISTS `marketing_coupon` (
  `coupon_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `coupon_code` varchar(50) NOT NULL,
  `coupon_name` varchar(100) NOT NULL,
  `coupon_type` varchar(20) NOT NULL,
  `discount_value` decimal(10,2),
  `min_consume` decimal(12,2) DEFAULT 0.00,
  `total_count` int DEFAULT 0,
  `received_count` int DEFAULT 0,
  `used_count` int DEFAULT 0,
  `valid_days` int,
  `start_date` date,
  `end_date` date,
  `applicable_type` varchar(20) DEFAULT 'all',
  `applicable_ids` text,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `description` varchar(500),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_coupon_type` (`coupon_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- 优惠券领取使用记录表
CREATE TABLE IF NOT EXISTS `marketing_coupon_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `coupon_id` bigint NOT NULL,
  `coupon_code` varchar(50),
  `coupon_name` varchar(100),
  `member_id` bigint,
  `member_name` varchar(100),
  `phone` varchar(20),
  `receive_time` datetime,
  `use_time` datetime,
  `expire_date` date,
  `status` varchar(20) DEFAULT 'unused',
  `booking_id` int,
  `booking_no` varchar(50),
  `discount_amount` decimal(12,2),
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券领取使用记录表';

-- 优惠规则表
CREATE TABLE IF NOT EXISTS `marketing_discount_rule` (
  `rule_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `rule_name` varchar(100) NOT NULL,
  `rule_type` varchar(50) NOT NULL,
  `condition_amount` decimal(12,2),
  `condition_quantity` int,
  `discount_amount` decimal(12,2),
  `discount_rate` decimal(5,2),
  `gift_item_id` int,
  `gift_item_name` varchar(100),
  `applicable_type` varchar(20) DEFAULT 'all',
  `applicable_ids` text,
  `priority` int DEFAULT 0,
  `stackable` tinyint DEFAULT 0,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `start_date` date,
  `end_date` date,
  `description` varchar(500),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_is_active` (`is_active`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠规则表';

-- 抽奖活动表
CREATE TABLE IF NOT EXISTS `marketing_lottery` (
  `lottery_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `lottery_code` varchar(50) NOT NULL,
  `lottery_name` varchar(100) NOT NULL,
  `start_date` date,
  `end_date` date,
  `daily_limit` int DEFAULT 1,
  `total_limit` int,
  `cost_points` int DEFAULT 0,
  `cost_amount` decimal(12,2) DEFAULT 0.00,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `prizes` text,
  `probability_rules` text,
  `description` varchar(500),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_lottery_code` (`lottery_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖活动表';

-- 会员奖励规则表
CREATE TABLE IF NOT EXISTS `marketing_member_reward` (
  `reward_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `reward_name` varchar(100) NOT NULL,
  `reward_type` varchar(50) NOT NULL,
  `reward_balance` decimal(12,2) DEFAULT 0.00,
  `reward_points` int DEFAULT 0,
  `reward_coupon_id` bigint,
  `reward_coupon_count` int DEFAULT 0,
  `condition_value` decimal(12,2),
  `is_active` tinyint NOT NULL DEFAULT 1,
  `description` varchar(500),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_is_active` (`is_active`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员奖励规则表';

-- 优惠码表
CREATE TABLE IF NOT EXISTS `marketing_promo_code` (
  `code_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `promo_code` varchar(50) NOT NULL,
  `code_name` varchar(100),
  `code_type` varchar(20) NOT NULL,
  `discount_value` decimal(10,2),
  `min_consume` decimal(12,2) DEFAULT 0.00,
  `total_count` int DEFAULT 1,
  `used_count` int DEFAULT 0,
  `start_date` date,
  `end_date` date,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `description` varchar(500),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_promo_code` (`promo_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠码表';

-- 会员卡主档表
CREATE TABLE IF NOT EXISTS `member_card` (
  `member_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `card_no` varchar(50) NOT NULL,
  `member_name` varchar(100) NOT NULL,
  `gender` varchar(2),
  `phone` varchar(20) NOT NULL,
  `id_card` varchar(20),
  `birthday` date,
  `level_id` int,
  `level_name` varchar(50),
  `balance` decimal(12,2) NOT NULL DEFAULT 0.00,
  `total_points` int NOT NULL DEFAULT 0,
  `total_recharge` decimal(12,2) NOT NULL DEFAULT 0.00,
  `total_consume` decimal(12,2) NOT NULL DEFAULT 0.00,
  `consume_count` int NOT NULL DEFAULT 0,
  `last_consume_date` date,
  `register_date` date,
  `register_store_id` bigint,
  `referrer_id` bigint,
  `avatar_url` varchar(255),
  `email` varchar(100),
  `address` varchar(200),
  `status` varchar(20) NOT NULL DEFAULT 'active',
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_level_id` (`level_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_card_no` (`card_no`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员卡主档表';

-- 会员消费记录表
CREATE TABLE IF NOT EXISTS `member_consume_record` (
  `consume_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `consume_no` varchar(50) NOT NULL,
  `member_id` bigint NOT NULL,
  `card_no` varchar(50),
  `member_name` varchar(100),
  `consume_date` date NOT NULL,
  `booking_id` int,
  `booking_no` varchar(50),
  `consume_amount` decimal(12,2) NOT NULL,
  `discount_amount` decimal(12,2) DEFAULT 0.00,
  `actual_amount` decimal(12,2) NOT NULL,
  `balance_pay` decimal(12,2) DEFAULT 0.00,
  `cash_pay` decimal(12,2) DEFAULT 0.00,
  `other_pay` decimal(12,2) DEFAULT 0.00,
  `balance_before` decimal(12,2),
  `balance_after` decimal(12,2),
  `points_earned` int DEFAULT 0,
  `points_used` int DEFAULT 0,
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_consume_date` (`consume_date`),
  KEY `idx_consume_no` (`consume_no`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员消费记录表';

-- 会员等级表
CREATE TABLE IF NOT EXISTS `member_level` (
  `level_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `level_code` varchar(20) NOT NULL,
  `level_name` varchar(50) NOT NULL,
  `min_points` int DEFAULT 0,
  `min_recharge` decimal(12,2) DEFAULT 0.00,
  `discount_rate` decimal(5,2) DEFAULT 100.00,
  `point_rate` decimal(5,2) DEFAULT 1.00,
  `birthday_discount` decimal(5,2) DEFAULT 100.00,
  `benefits` text,
  `is_active` tinyint NOT NULL DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_level_code` (`level_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';

-- 积分变动日志表
CREATE TABLE IF NOT EXISTS `member_point_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `member_id` bigint NOT NULL,
  `card_no` varchar(50),
  `member_name` varchar(100),
  `change_type` varchar(20) NOT NULL,
  `change_points` int NOT NULL,
  `points_before` int,
  `points_after` int,
  `related_type` varchar(50),
  `related_id` bigint,
  `related_no` varchar(50),
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_change_type` (`change_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分变动日志表';

-- 积分规则表
CREATE TABLE IF NOT EXISTS `member_point_rule` (
  `rule_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `rule_name` varchar(100) NOT NULL,
  `rule_type` varchar(50) NOT NULL,
  `point_value` int,
  `amount_condition` decimal(12,2),
  `is_active` tinyint NOT NULL DEFAULT 1,
  `effective_date` date,
  `expiry_date` date,
  `description` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分规则表';

-- 储值充值记录表
CREATE TABLE IF NOT EXISTS `member_recharge_record` (
  `recharge_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `recharge_no` varchar(50) NOT NULL,
  `member_id` bigint NOT NULL,
  `card_no` varchar(50),
  `member_name` varchar(100),
  `recharge_date` date NOT NULL,
  `recharge_amount` decimal(12,2) NOT NULL,
  `gift_amount` decimal(12,2) DEFAULT 0.00,
  `total_amount` decimal(12,2) NOT NULL,
  `balance_before` decimal(12,2),
  `balance_after` decimal(12,2),
  `payment_method` varchar(20),
  `recharge_type` varchar(20) DEFAULT 'normal',
  `activity_id` bigint,
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_member_id` (`member_id`),
  KEY `idx_recharge_date` (`recharge_date`),
  KEY `idx_recharge_no` (`recharge_no`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='储值充值记录表';

-- 菜单分类表(零点排版)
CREATE TABLE IF NOT EXISTS `menu_category` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `category_name` varchar(50) NOT NULL,
  `category_code` varchar(50) NOT NULL,
  `description` varchar(200),
  `sort_order` int DEFAULT 0,
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单分类表(零点排版)';

-- 加班申请表
CREATE TABLE IF NOT EXISTS `overtime` (
  `overtime_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  `overtime_date` date NOT NULL,
  `start_time` datetime,
  `end_time` datetime,
  `hours` decimal(10,3) DEFAULT 0.000,
  `status` varchar(32) DEFAULT 'pending',
  `reason` varchar(500),
  `approver_id` int,
  `approve_time` datetime,
  `approve_remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_staff_date` (`staff_id`, `overtime_date`),
  KEY `idx_store_ot` (`store_id`, `staff_id`, `overtime_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加班申请表';

-- 套餐菜品明细表
CREATE TABLE IF NOT EXISTS `package_dish_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `package_id` varchar(20) NOT NULL,
  `dish_id` varchar(20) NOT NULL,
  `dish_quantity` int DEFAULT 1,
  `dish_order` int DEFAULT 0,
  `custom_name` varchar(100),
  `note` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_dish` (`dish_id`),
  KEY `idx_package` (`package_id`),
  KEY `idx_pkg_dish_pkg` (`package_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐菜品明细表';

-- 套餐主档表
CREATE TABLE IF NOT EXISTS `package_master` (
  `package_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `package_name` varchar(100) NOT NULL,
  `package_total_price` decimal(12,2) DEFAULT 0.00,
  `package_cost_price` decimal(12,2) DEFAULT 0.00,
  `cost_rate` decimal(12,2) DEFAULT 0.00,
  `dish_count` int DEFAULT 0,
  `suggest_guests` int DEFAULT 10,
  `occasion_type` varchar(20),
  `package_series` varchar(20),
  `is_active` tinyint DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`package_id`, `store_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_occasion` (`occasion_type`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_package` (`store_id`, `package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐主档表';

-- 采购订单主档表
CREATE TABLE IF NOT EXISTS `purchase_order` (
  `order_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `order_no` varchar(50) NOT NULL,
  `supplier_id` int,
  `supplier_name` varchar(100),
  `order_date` date NOT NULL,
  `expected_date` date,
  `total_quantity` decimal(10,2) DEFAULT 0.00,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `received_quantity` decimal(10,2) DEFAULT 0.00,
  `received_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  `order_type` varchar(20) DEFAULT 'normal',
  `purchaser_id` int,
  `purchaser_name` varchar(50),
  `approver_id` int,
  `approver_name` varchar(50),
  `approve_time` datetime,
  `warehouse_keeper_id` int,
  `warehouse_keeper_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_order_date` (`order_date`),
  UNIQUE KEY `idx_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单主档表';

-- 采购订单明细表
CREATE TABLE IF NOT EXISTS `purchase_order_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `order_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `line_no` int NOT NULL,
  `ingredient_id` int,
  `ingredient_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `spec` varchar(100),
  `unit` varchar(20),
  `quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `received_quantity` decimal(10,2) DEFAULT 0.00,
  `returned_quantity` decimal(10,2) DEFAULT 0.00,
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

-- 采购入库单主档表
CREATE TABLE IF NOT EXISTS `purchase_receipt` (
  `receipt_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `receipt_no` varchar(50) NOT NULL,
  `receipt_date` date NOT NULL,
  `order_id` bigint,
  `order_no` varchar(50),
  `supplier_id` int,
  `supplier_name` varchar(100),
  `total_quantity` decimal(10,2) DEFAULT 0.00,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'confirmed',
  `warehouse_keeper_id` int,
  `warehouse_keeper_name` varchar(50),
  `delivery_person` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_order_id` (`order_id`),
  KEY `idx_receipt_date` (`receipt_date`),
  UNIQUE KEY `idx_receipt_no` (`receipt_no`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库单主档表';

-- 采购入库明细表
CREATE TABLE IF NOT EXISTS `purchase_receipt_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `receipt_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `line_no` int NOT NULL,
  `order_detail_id` bigint,
  `ingredient_id` int,
  `ingredient_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `spec` varchar(100),
  `unit` varchar(20),
  `order_quantity` decimal(10,2),
  `actual_quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `quality_status` varchar(20) DEFAULT 'qualified',
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库明细表';

-- 采购退货单主档表
CREATE TABLE IF NOT EXISTS `purchase_return` (
  `return_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `return_no` varchar(50) NOT NULL,
  `return_date` date NOT NULL,
  `receipt_id` bigint,
  `receipt_no` varchar(50),
  `order_id` bigint,
  `supplier_id` int,
  `supplier_name` varchar(100),
  `total_quantity` decimal(10,2) DEFAULT 0.00,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `return_reason` varchar(200),
  `status` varchar(20) DEFAULT 'confirmed',
  `warehouse_keeper_id` int,
  `warehouse_keeper_name` varchar(50),
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_return_date` (`return_date`),
  UNIQUE KEY `idx_return_no` (`return_no`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货单主档表';

-- 采购退货明细表
CREATE TABLE IF NOT EXISTS `purchase_return_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `return_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `line_no` int NOT NULL,
  `receipt_detail_id` bigint,
  `ingredient_id` int,
  `ingredient_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `unit` varchar(20),
  `return_quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `return_reason` varchar(200),
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_return_id` (`return_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货明细表';

-- 日报表
CREATE TABLE IF NOT EXISTS `report_daily` (
  `report_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `report_date` date NOT NULL,
  `week_day` varchar(10),
  `is_holiday` tinyint DEFAULT 0,
  `weather` varchar(50),
  `total_booking_count` int DEFAULT 0,
  `total_guest_count` int DEFAULT 0,
  `total_table_count` int DEFAULT 0,
  `table_turnover_rate` decimal(5,2),
  `total_revenue` decimal(12,2) DEFAULT 0.00,
  `food_revenue` decimal(12,2) DEFAULT 0.00,
  `beverage_revenue` decimal(12,2) DEFAULT 0.00,
  `other_revenue` decimal(12,2) DEFAULT 0.00,
  `member_recharge` decimal(12,2) DEFAULT 0.00,
  `total_cost` decimal(12,2) DEFAULT 0.00,
  `food_cost` decimal(12,2) DEFAULT 0.00,
  `labor_cost` decimal(12,2) DEFAULT 0.00,
  `rent_cost` decimal(12,2) DEFAULT 0.00,
  `utility_cost` decimal(12,2) DEFAULT 0.00,
  `other_cost` decimal(12,2) DEFAULT 0.00,
  `gross_profit` decimal(12,2) DEFAULT 0.00,
  `gross_profit_rate` decimal(5,2),
  `net_profit` decimal(12,2) DEFAULT 0.00,
  `net_profit_rate` decimal(5,2),
  `food_cost_rate` decimal(5,2),
  `avg_consumption` decimal(10,2),
  `avg_table_spending` decimal(10,2),
  `new_member_count` int DEFAULT 0,
  `active_member_count` int DEFAULT 0,
  `member_consume_count` int DEFAULT 0,
  `status` varchar(20) DEFAULT 'draft',
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_report_date` (`report_date`),
  UNIQUE KEY `uk_store_date` (`store_id`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日报表';

-- 部门成本统计表
CREATE TABLE IF NOT EXISTS `report_department_cost` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `stat_date` date NOT NULL,
  `stat_type` varchar(20) NOT NULL,
  `department_id` int,
  `department` varchar(50) NOT NULL,
  `labor_cost` decimal(12,2) DEFAULT 0.00,
  `material_cost` decimal(12,2) DEFAULT 0.00,
  `other_cost` decimal(12,2) DEFAULT 0.00,
  `total_cost` decimal(12,2) DEFAULT 0.00,
  `output_value` decimal(12,2) DEFAULT 0.00,
  `cost_rate` decimal(5,2),
  `staff_count` int,
  `per_capita_cost` decimal(10,2),
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_department_id` (`department_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门成本统计表';

-- 菜品销售统计表
CREATE TABLE IF NOT EXISTS `report_dish_sales` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `stat_date` date NOT NULL,
  `stat_type` varchar(20) NOT NULL,
  `dish_id` int,
  `dish_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `spicy_level` varchar(20),
  `main_ingredient_type` varchar(50),
  `sale_quantity` decimal(10,2) DEFAULT 0.00,
  `sale_amount` decimal(12,2) DEFAULT 0.00,
  `cost_amount` decimal(12,2) DEFAULT 0.00,
  `gross_profit` decimal(12,2) DEFAULT 0.00,
  `gross_profit_rate` decimal(5,2),
  `refund_quantity` decimal(10,2) DEFAULT 0.00,
  `refund_amount` decimal(12,2) DEFAULT 0.00,
  `sale_rank` int,
  `amount_rank` int,
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_category` (`category`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品销售统计表';

-- 月报表
CREATE TABLE IF NOT EXISTS `report_monthly` (
  `report_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `report_month` varchar(7) NOT NULL,
  `report_year` int,
  `report_month_of_year` int,
  `total_booking_count` int DEFAULT 0,
  `total_guest_count` int DEFAULT 0,
  `total_table_count` int DEFAULT 0,
  `avg_daily_guest` decimal(10,2),
  `table_turnover_rate` decimal(5,2),
  `total_revenue` decimal(12,2) DEFAULT 0.00,
  `food_revenue` decimal(12,2) DEFAULT 0.00,
  `beverage_revenue` decimal(12,2) DEFAULT 0.00,
  `other_revenue` decimal(12,2) DEFAULT 0.00,
  `member_recharge` decimal(12,2) DEFAULT 0.00,
  `total_cost` decimal(12,2) DEFAULT 0.00,
  `food_cost` decimal(12,2) DEFAULT 0.00,
  `labor_cost` decimal(12,2) DEFAULT 0.00,
  `rent_cost` decimal(12,2) DEFAULT 0.00,
  `utility_cost` decimal(12,2) DEFAULT 0.00,
  `other_cost` decimal(12,2) DEFAULT 0.00,
  `gross_profit` decimal(12,2) DEFAULT 0.00,
  `gross_profit_rate` decimal(5,2),
  `net_profit` decimal(12,2) DEFAULT 0.00,
  `net_profit_rate` decimal(5,2),
  `food_cost_rate` decimal(5,2),
  `avg_consumption` decimal(10,2),
  `avg_table_spending` decimal(10,2),
  `new_member_count` int DEFAULT 0,
  `total_member_count` int DEFAULT 0,
  `active_member_count` int DEFAULT 0,
  `total_purchase_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'draft',
  `operator_id` int,
  `operator_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_report_month` (`report_month`),
  UNIQUE KEY `uk_store_month` (`store_id`, `report_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月报表';

-- 员工KPI统计表
CREATE TABLE IF NOT EXISTS `report_staff_kpi` (
  `id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `stat_month` varchar(7) NOT NULL,
  `staff_id` int,
  `staff_name` varchar(50) NOT NULL,
  `department` varchar(50),
  `position` varchar(50),
  `attendance_days` decimal(5,1) DEFAULT 0.0,
  `late_count` int DEFAULT 0,
  `early_leave_count` int DEFAULT 0,
  `absent_days` decimal(5,1) DEFAULT 0.0,
  `overtime_hours` decimal(6,1) DEFAULT 0.0,
  `leave_days` decimal(5,1) DEFAULT 0.0,
  `performance_score` decimal(5,2) DEFAULT 0.00,
  `performance_rank` int,
  `sale_amount` decimal(12,2) DEFAULT 0.00,
  `service_count` int DEFAULT 0,
  `customer_praise` int DEFAULT 0,
  `customer_complaint` int DEFAULT 0,
  `reward_count` int DEFAULT 0,
  `penalty_count` int DEFAULT 0,
  `kpi_score` decimal(5,2) DEFAULT 0.00,
  `kpi_grade` varchar(10),
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_department` (`department`),
  KEY `idx_staff_id` (`staff_id`),
  KEY `idx_stat_month` (`stat_month`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工KPI统计表';

-- 排班表
CREATE TABLE IF NOT EXISTS `schedule` (
  `schedule_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  `schedule_date` date NOT NULL,
  `shift_type` varchar(20),
  `start_time` datetime,
  `end_time` datetime,
  `status` varchar(32) DEFAULT 'normal',
  `remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_staff_date` (`staff_id`, `schedule_date`),
  KEY `idx_store_sched` (`store_id`, `staff_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班表';

-- 员工主档表
CREATE TABLE IF NOT EXISTS `staff_master` (
  `staff_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_name` varchar(20) NOT NULL,
  `staff_account` varchar(20),
  `staff_password` varchar(100),
  `staff_gender` varchar(2),
  `staff_age` int,
  `staff_phone` varchar(20),
  `staff_position` varchar(50),
  `department` varchar(50),
  `hire_date` date,
  `monthly_salary` decimal(12,2) DEFAULT 0.00,
  `id_card` varchar(20),
  `home_address` varchar(200),
  `emergency_contact` varchar(20),
  `emergency_phone` varchar(20),
  `employment_status` varchar(10) DEFAULT 'active',
  `resign_reason` text,
  `resign_date` date,
  `role` varchar(30),
  `remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `permission_level` int DEFAULT 0,
  `dept_id` int,
  `can_manage_kitchen` tinyint DEFAULT 0,
  `can_manage_sales` tinyint DEFAULT 0,
  `can_manage_finance` tinyint DEFAULT 0,
  `can_manage_hr` tinyint DEFAULT 0,
  `can_view_all_stores` tinyint DEFAULT 0,
  `can_edit_system` tinyint DEFAULT 0,
  `staff_no` varchar(20),
  `avatar_url` varchar(500),
  `nation` varchar(20),
  `birth_date` date,
  `native_place` varchar(100),
  `marital_status` varchar(10),
  `political_status` varchar(20),
  `education` varchar(20),
  `major` varchar(50),
  `graduate_school` varchar(100),
  `email` varchar(100),
  `wechat` varchar(50),
  `staff_rank` varchar(20),
  `employment_type` varchar(20),
  `hire_channel` varchar(30),
  `probation_months` decimal(3,1),
  `probation_start_date` date,
  `probation_end_date` date,
  `regular_date` date,
  `leader_id` int,
  `work_location` varchar(100),
  `basic_salary` decimal(12,2),
  `performance_salary` decimal(12,2),
  `subsidy` decimal(12,2),
  `bonus` decimal(12,2),
  `social_insurance` decimal(12,2),
  `housing_fund` decimal(12,2),
  `bank_name` varchar(50),
  `bank_account` varchar(30),
  `account_holder` varchar(20),
  `entry_age` int,
  `work_years` decimal(5,2),
  KEY `fk_staff_dept` (`dept_id`),
  KEY `idx_account` (`staff_account`),
  KEY `idx_phone` (`staff_phone`),
  KEY `idx_staff_dept_status` (`dept_id`, `employment_status`),
  KEY `idx_status` (`employment_status`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_staff` (`store_id`, `staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工主档表';

-- 报损单主档表
CREATE TABLE IF NOT EXISTS `stock_loss` (
  `loss_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `loss_no` varchar(50) NOT NULL,
  `loss_date` date NOT NULL,
  `loss_type` varchar(50),
  `total_quantity` decimal(10,2) DEFAULT 0.00,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'pending',
  `applicant_id` int,
  `applicant_name` varchar(50),
  `approver_id` int,
  `approver_name` varchar(50),
  `approve_time` datetime,
  `approve_remark` varchar(500),
  `warehouse_keeper_id` int,
  `warehouse_keeper_name` varchar(50),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_loss_date` (`loss_date`),
  UNIQUE KEY `idx_loss_no` (`loss_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报损单主档表';

-- 报损明细表
CREATE TABLE IF NOT EXISTS `stock_loss_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `loss_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `line_no` int NOT NULL,
  `ingredient_id` int,
  `ingredient_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `unit` varchar(20),
  `loss_quantity` decimal(10,2) NOT NULL,
  `unit_price` decimal(10,2) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `loss_reason` varchar(200),
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_loss_id` (`loss_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报损明细表';

-- 盘点单主档表
CREATE TABLE IF NOT EXISTS `stock_take` (
  `take_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `take_no` varchar(50) NOT NULL,
  `take_date` date NOT NULL,
  `take_type` varchar(20) NOT NULL DEFAULT 'full',
  `category_id` int,
  `warehouse_id` int,
  `total_items` int DEFAULT 0,
  `total_diff_items` int DEFAULT 0,
  `total_diff_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) NOT NULL DEFAULT 'draft',
  `operator_id` int,
  `operator_name` varchar(50),
  `supervisor_id` int,
  `supervisor_name` varchar(50),
  `finish_time` datetime,
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_take_date` (`take_date`),
  UNIQUE KEY `idx_take_no` (`take_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点单主档表';

-- 盘点明细表
CREATE TABLE IF NOT EXISTS `stock_take_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `take_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `line_no` int NOT NULL,
  `ingredient_id` int,
  `ingredient_name` varchar(100) NOT NULL,
  `category` varchar(50),
  `unit` varchar(20),
  `system_quantity` decimal(10,2) NOT NULL,
  `system_amount` decimal(12,2),
  `actual_quantity` decimal(10,2) NOT NULL,
  `actual_amount` decimal(12,2),
  `diff_quantity` decimal(10,2) DEFAULT 0.00,
  `diff_amount` decimal(12,2) DEFAULT 0.00,
  `diff_type` varchar(20),
  `unit_price` decimal(10,2),
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_diff_type` (`diff_type`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_take_id` (`take_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点明细表';

-- 库存调拨单表
CREATE TABLE IF NOT EXISTS `stock_transfer` (
  `transfer_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `transfer_no` varchar(50) NOT NULL,
  `transfer_date` date NOT NULL,
  `from_warehouse_id` int,
  `from_warehouse_name` varchar(50),
  `to_warehouse_id` int,
  `to_warehouse_name` varchar(50),
  `total_quantity` decimal(10,2) DEFAULT 0.00,
  `total_amount` decimal(12,2) DEFAULT 0.00,
  `status` varchar(20) DEFAULT 'pending',
  `out_time` datetime,
  `in_time` datetime,
  `operator_out_id` int,
  `operator_out_name` varchar(50),
  `operator_in_id` int,
  `operator_in_name` varchar(50),
  `transfer_reason` varchar(200),
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_transfer_date` (`transfer_date`),
  UNIQUE KEY `idx_transfer_no` (`transfer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存调拨单表';

-- 门店信息表
CREATE TABLE IF NOT EXISTS `store_info` (
  `store_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_code` varchar(50) NOT NULL,
  `store_name` varchar(100) NOT NULL,
  `store_short_name` varchar(50),
  `store_type` varchar(20) DEFAULT 'normal',
  `store_level` varchar(20),
  `address` varchar(200),
  `province` varchar(50),
  `city` varchar(50),
  `district` varchar(50),
  `phone` varchar(20),
  `contact_person` varchar(50),
  `business_hours` varchar(100),
  `table_count` int DEFAULT 0,
  `max_capacity` int DEFAULT 0,
  `business_area` decimal(8,2),
  `manager_id` int,
  `manager_name` varchar(50),
  `opening_date` date,
  `status` varchar(20) DEFAULT 'open',
  `tax_no` varchar(50),
  `bank_name` varchar(100),
  `bank_account` varchar(50),
  `logo_url` varchar(255),
  `store_image_url` varchar(255),
  `sort_order` int DEFAULT 0,
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_status` (`status`),
  UNIQUE KEY `uk_store_code` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店信息表';

-- 供应商主档表
CREATE TABLE IF NOT EXISTS `supplier_master` (
  `supplier_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `supplier_code` varchar(20),
  `supplier_name` varchar(100) NOT NULL,
  `contact_person` varchar(50),
  `contact_phone` varchar(20),
  `bank_account` varchar(50),
  `platform_account` varchar(100),
  `main_products` text,
  `wechat_account` varchar(50),
  `alipay_account` varchar(50),
  `taobao_account` varchar(50),
  `supplier_rating` int DEFAULT 5,
  `is_active` tinyint DEFAULT 1,
  `remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_active` (`is_active`),
  KEY `idx_name` (`supplier_name`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_supplier` (`store_id`, `supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商主档表';

-- 数据字典表
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `dict_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dict_code` varchar(100) NOT NULL,
  `dict_name` varchar(100) NOT NULL,
  `dict_type` varchar(20) NOT NULL DEFAULT 'list',
  `store_id` bigint NOT NULL DEFAULT 1,
  `description` varchar(200),
  `sort_order` int DEFAULT 0,
  `is_active` tinyint DEFAULT 1,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- 数据字典项表
CREATE TABLE IF NOT EXISTS `sys_dict_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `dict_id` bigint NOT NULL,
  `dict_code` varchar(100) NOT NULL,
  `item_value` varchar(100) NOT NULL,
  `item_label` varchar(100) NOT NULL,
  `parent_id` bigint,
  `store_id` bigint NOT NULL DEFAULT 1,
  `sort_order` int DEFAULT 0,
  `is_active` tinyint DEFAULT 1,
  `remark` varchar(200),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_dict_code` (`dict_code`, `store_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_dict_value` (`dict_id`, `item_value`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典项表';

-- 系统通知表
CREATE TABLE IF NOT EXISTS `sys_notification` (
  `notify_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `notify_type` varchar(50) NOT NULL,
  `notify_title` varchar(200) NOT NULL,
  `notify_content` text,
  `priority` varchar(20) DEFAULT 'normal',
  `sender_id` int,
  `sender_name` varchar(50),
  `send_time` datetime,
  `receiver_type` varchar(20) DEFAULT 'all',
  `receiver_ids` text,
  `related_type` varchar(50),
  `related_id` bigint,
  `is_read` tinyint DEFAULT 0,
  `status` varchar(20) DEFAULT 'published',
  `remark` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_created_at` (`created_at`),
  KEY `idx_notify_type` (`notify_type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `log_id` bigint NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `operator_id` int,
  `operator_name` varchar(50),
  `operator_account` varchar(50),
  `operation_type` varchar(50) NOT NULL,
  `operation_module` varchar(50),
  `operation_action` varchar(100),
  `request_method` varchar(10),
  `request_url` varchar(255),
  `request_params` text,
  `request_ip` varchar(50),
  `target_type` varchar(50),
  `target_id` varchar(100),
  `target_name` varchar(200),
  `old_value` text,
  `new_value` text,
  `diff_value` text,
  `status` varchar(20) DEFAULT 'success',
  `error_msg` text,
  `cost_time` int,
  `user_agent` varchar(500),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_created_at` (`created_at`),
  KEY `idx_operation_module` (`operation_module`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 桌台主档表
CREATE TABLE IF NOT EXISTS `table_master` (
  `table_id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `store_id` bigint NOT NULL DEFAULT 1,
  `table_number` varchar(10) NOT NULL,
  `table_name` varchar(50) NOT NULL,
  `table_location` varchar(50),
  `table_area` varchar(20),
  `table_capacity` int DEFAULT 10,
  `table_type` varchar(20),
  `table_status` varchar(20) NOT NULL DEFAULT 'available',
  `min_capacity` int DEFAULT 6,
  `max_capacity` int DEFAULT 12,
  `sort_order` int DEFAULT 0,
  `is_active` tinyint DEFAULT 1,
  `remark` text,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_area` (`table_area`),
  KEY `idx_number` (`table_number`),
  KEY `idx_status` (`table_status`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_table` (`store_id`, `table_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='桌台主档表';

-- 模板-分类关联表
CREATE TABLE IF NOT EXISTS `template_category_rel` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `template_id` int NOT NULL,
  `menu_category_id` int NOT NULL,
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  KEY `idx_store_id` (`store_id`),
  KEY `menu_category_id` (`menu_category_id`),
  UNIQUE KEY `uk_template_category` (`template_id`, `menu_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板-分类关联表';

-- 模板-菜品关联表
CREATE TABLE IF NOT EXISTS `template_dish_rel` (
  `id` int NOT NULL AUTO_INCREMENT PRIMARY KEY,
  `template_id` int NOT NULL,
  `dish_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `menu_category_id` int,
  `special_price` decimal(12,2),
  `sort_order` int DEFAULT 0,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  KEY `menu_category_id` (`menu_category_id`),
  UNIQUE KEY `uk_template_dish` (`template_id`, `dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板-菜品关联表';


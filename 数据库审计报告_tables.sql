-- ============================================================
-- 从审计报告生成的建表SQL
-- 共 85 张表
-- ============================================================

-- 系统管理员表
CREATE TABLE IF NOT EXISTS `admin_users` (
  `id` int NOT NULL,
  `username` varchar(64) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统管理员表';

-- AI对话历史表
CREATE TABLE IF NOT EXISTS `ai_chat_history` (
  `id` bigint NOT NULL,
  `staff_id` int,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_staff_id` (`staff_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话历史表';

-- AI记忆表
CREATE TABLE IF NOT EXISTS `ai_memory` (
  `id` bigint NOT NULL,
  `user_id` varchar(64),
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI记忆表';

-- 考勤明细表
CREATE TABLE IF NOT EXISTS `attendance` (
  `attendance_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  `attendance_date` date NOT NULL,
  PRIMARY KEY (`attendance_id`),
  KEY `idx_attendance_date` (`attendance_date`),
  KEY `idx_staff_date` (`staff_id`, `attendance_date`),
  KEY `idx_store_att` (`store_id`, `staff_id`, `attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤明细表';

-- 考勤月度汇总表
CREATE TABLE IF NOT EXISTS `attendance_records` (
  `id` int NOT NULL,
  `record_id` varchar(50) NOT NULL,
  `staff_id` varchar(50),
  `month` varchar(7) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`staff_id`),
  KEY `idx_month` (`month`),
  KEY `idx_store_attr` (`store_id`, `staff_id`, `month`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `record_id` (`record_id`),
  UNIQUE KEY `uk_emp_month` (`staff_id`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='考勤月度汇总表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS `audit_logs` (
  `id` int NOT NULL,
  `user_id` varchar(64),
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_audit_user` (`user_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志表';

-- 宴会菜单模板表
CREATE TABLE IF NOT EXISTS `banquet_template` (
  `id` int NOT NULL,
  `template_code` varchar(50) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宴会菜单模板表';

-- 宴会类型-模板关联表
CREATE TABLE IF NOT EXISTS `banquet_template_rel` (
  `id` int NOT NULL,
  `banquet_type_id` int NOT NULL,
  `template_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `template_id` (`template_id`),
  UNIQUE KEY `uk_banquet_template` (`banquet_type_id`, `template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宴会类型-模板关联表';

-- 宴会类型表
CREATE TABLE IF NOT EXISTS `banquet_type` (
  `id` int NOT NULL,
  `type_code` varchar(50) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `type_code` (`type_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宴会类型表';

-- 订菜明细表
CREATE TABLE IF NOT EXISTS `booking_dish_detail` (
  `dish_booking_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `table_booking_id` bigint,
  `booking_id` varchar(20),
  `dish_id` varchar(20) NOT NULL,
  PRIMARY KEY (`dish_booking_id`),
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
  `customer_id` int,
  `customer_phone` varchar(20),
  `staff_id` int,
  `booking_status` varchar(20) NOT NULL DEFAULT 'pending',
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
  `table_booking_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `booking_id` varchar(20) NOT NULL,
  `table_id` int NOT NULL,
  PRIMARY KEY (`table_booking_id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_booking_table_booking` (`booking_id`),
  KEY `idx_booking_table_table` (`table_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_table` (`table_id`),
  UNIQUE KEY `uk_table_date_time` (`table_id`, `booking_date`, `booking_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订桌明细表';

-- 系统变更日志表
CREATE TABLE IF NOT EXISTS `change_log` (
  `log_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `operator_id` int,
  `operation_type` varchar(30) NOT NULL,
  `target_type` varchar(30) NOT NULL,
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_changelog_time` (`created_at`),
  KEY `idx_operator` (`operator_id`),
  KEY `idx_store_time` (`store_id`, `created_at`),
  KEY `idx_target` (`target_type`, `target_id`),
  KEY `idx_type` (`operation_type`, `target_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统变更日志表';

-- 系统配置表
CREATE TABLE IF NOT EXISTS `config` (
  `config_key` varchar(128) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`config_key`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 客户主档表
CREATE TABLE IF NOT EXISTS `customer_master` (
  `customer_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `customer_name` varchar(50) NOT NULL,
  `customer_phone` varchar(20) NOT NULL,
  `member_level` varchar(10) DEFAULT 'v1',
  PRIMARY KEY (`customer_id`),
  KEY `idx_level` (`member_level`),
  KEY `idx_name` (`customer_name`),
  KEY `idx_phone` (`customer_phone`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_customer` (`store_id`, `customer_phone`),
  UNIQUE KEY `uk_store_name_phone` (`store_id`, `customer_name`, `customer_phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户主档表';

-- 部门表
CREATE TABLE IF NOT EXISTS `department` (
  `dept_id` int NOT NULL,
  PRIMARY KEY (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门表';

-- 菜品分类表
CREATE TABLE IF NOT EXISTS `dish_category` (
  `id` int NOT NULL,
  `category_code` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品分类表';

-- 菜品主档表
CREATE TABLE IF NOT EXISTS `dish_master` (
  `dish_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dish_category` varchar(50),
  `is_active` tinyint DEFAULT 1,
  PRIMARY KEY (`dish_id`, `store_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_category` (`dish_category`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_dish` (`store_id`, `dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品主档表';

-- 菜品场合别名表
CREATE TABLE IF NOT EXISTS `dish_occasion_names` (
  `id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dish_id` varchar(20) NOT NULL,
  `occasion_type` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_occasion` (`occasion_type`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品场合别名表';

-- 菜品配方表
CREATE TABLE IF NOT EXISTS `dish_recipe` (
  `recipe_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `dish_id` varchar(20) NOT NULL,
  `ingredient_id` varchar(50) NOT NULL,
  PRIMARY KEY (`recipe_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品配方表';

-- 菜品标签表
CREATE TABLE IF NOT EXISTS `dish_tag` (
  `id` int NOT NULL,
  `tag_code` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `tag_code` (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品标签表';

-- 菜品标签关联表
CREATE TABLE IF NOT EXISTS `dish_tag_relation` (
  `id` int NOT NULL,
  `dish_id` varchar(20) NOT NULL,
  `tag_id` int NOT NULL,
  PRIMARY KEY (`id`),
  KEY `tag_id` (`tag_id`),
  UNIQUE KEY `uk_dish_tag` (`dish_id`, `store_id`, `tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品标签关联表';

-- 菜品用途表
CREATE TABLE IF NOT EXISTS `dish_usage` (
  `id` int NOT NULL,
  `usage_code` varchar(20) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `usage_code` (`usage_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品用途表';

-- 菜品用途关联表
CREATE TABLE IF NOT EXISTS `dish_usage_relation` (
  `id` int NOT NULL,
  `dish_id` varchar(20) NOT NULL,
  `usage_id` int NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dish_usage` (`dish_id`, `store_id`, `usage_id`),
  KEY `usage_id` (`usage_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品用途关联表';

-- 员工生命周期表
CREATE TABLE IF NOT EXISTS `employee_lifecycle` (
  `id` int NOT NULL,
  `staff_id` varchar(50),
  `event_date` date NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_emp_id` (`staff_id`),
  KEY `idx_event_date` (`event_date`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_store_life` (`store_id`, `staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工生命周期表';

-- 财务账户表
CREATE TABLE IF NOT EXISTS `finance_account` (
  `account_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `account_code` varchar(50) NOT NULL,
  `account_type` varchar(20) NOT NULL,
  PRIMARY KEY (`account_id`),
  KEY `idx_account_type` (`account_type`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_account_code` (`account_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务账户表';

-- 成本记录表
CREATE TABLE IF NOT EXISTS `finance_cost_record` (
  `cost_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `cost_date` date NOT NULL,
  `cost_type` varchar(50) NOT NULL,
  PRIMARY KEY (`cost_id`),
  KEY `idx_cost_date` (`cost_date`),
  KEY `idx_cost_type` (`cost_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成本记录表';

-- 费用报销表
CREATE TABLE IF NOT EXISTS `finance_expense` (
  `expense_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `expense_no` varchar(50) NOT NULL,
  `expense_type` varchar(50),
  `applicant_id` int,
  `approval_status` varchar(20) NOT NULL DEFAULT 'pending',
  PRIMARY KEY (`expense_id`),
  KEY `idx_applicant_id` (`applicant_id`),
  KEY `idx_approval_status` (`approval_status`),
  KEY `idx_expense_no` (`expense_no`),
  KEY `idx_expense_type` (`expense_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用报销表';

-- 应付账款表
CREATE TABLE IF NOT EXISTS `finance_payable` (
  `payable_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `payable_no` varchar(50) NOT NULL,
  `supplier_id` int,
  `due_date` date,
  `status` varchar(20) NOT NULL DEFAULT 'unpaid',
  PRIMARY KEY (`payable_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_payable_no` (`payable_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应付账款表';

-- 收款记录表
CREATE TABLE IF NOT EXISTS `finance_payment_record` (
  `payment_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `payment_no` varchar(50) NOT NULL,
  `payment_date` date NOT NULL,
  `receivable_id` bigint,
  `customer_id` int,
  PRIMARY KEY (`payment_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_payment_date` (`payment_date`),
  KEY `idx_payment_no` (`payment_no`),
  KEY `idx_receivable_id` (`receivable_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收款记录表';

-- 应收账款表
CREATE TABLE IF NOT EXISTS `finance_receivable` (
  `receivable_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `receivable_no` varchar(50) NOT NULL,
  `customer_id` int,
  `due_date` date,
  `status` varchar(20) NOT NULL DEFAULT 'unpaid',
  PRIMARY KEY (`receivable_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_due_date` (`due_date`),
  KEY `idx_receivable_no` (`receivable_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收账款表';

-- 对账记录表
CREATE TABLE IF NOT EXISTS `finance_reconciliation` (
  `recon_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `recon_no` varchar(50) NOT NULL,
  `account_id` bigint,
  `status` varchar(20) DEFAULT 'pending',
  PRIMARY KEY (`recon_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_recon_no` (`recon_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对账记录表';

-- 结算记录表
CREATE TABLE IF NOT EXISTS `finance_settlement` (
  `settlement_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `settlement_no` varchar(50) NOT NULL,
  `settlement_date` date NOT NULL,
  `settlement_type` varchar(20) NOT NULL,
  PRIMARY KEY (`settlement_id`),
  KEY `idx_settlement_date` (`settlement_date`),
  KEY `idx_settlement_no` (`settlement_no`),
  KEY `idx_settlement_type` (`settlement_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='结算记录表';

-- 收支流水表
CREATE TABLE IF NOT EXISTS `finance_transaction` (
  `trans_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `trans_no` varchar(50) NOT NULL,
  `trans_date` date NOT NULL,
  `trans_type` varchar(20) NOT NULL,
  `account_id` bigint,
  `related_type` varchar(50),
  PRIMARY KEY (`trans_id`),
  KEY `idx_account_id` (`account_id`),
  KEY `idx_related` (`related_type`, `related_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_trans_date` (`trans_date`),
  KEY `idx_trans_no` (`trans_no`),
  KEY `idx_trans_type` (`trans_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收支流水表';

-- 会计凭证表
CREATE TABLE IF NOT EXISTS `finance_voucher` (
  `voucher_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `voucher_no` varchar(50) NOT NULL,
  `voucher_date` date NOT NULL,
  `status` varchar(20) DEFAULT 'draft',
  PRIMARY KEY (`voucher_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_voucher_date` (`voucher_date`),
  KEY `idx_voucher_no` (`voucher_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计凭证表';

-- 会计凭证明细表
CREATE TABLE IF NOT EXISTS `finance_voucher_detail` (
  `detail_id` bigint NOT NULL,
  `voucher_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `subject_code` varchar(50) NOT NULL,
  PRIMARY KEY (`detail_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_subject_code` (`subject_code`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会计凭证明细表';

-- 库存变动日志表
CREATE TABLE IF NOT EXISTS `ingredient_inventory_log` (
  `log_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` varchar(50) NOT NULL,
  `log_type` varchar(20) NOT NULL,
  `log_time` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_time` (`log_time`),
  KEY `idx_type` (`log_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存变动日志表';

-- 食材/原料主档表
CREATE TABLE IF NOT EXISTS `ingredient_master` (
  `ingredient_id` varchar(50) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_category` varchar(50),
  `is_active` tinyint DEFAULT 1,
  PRIMARY KEY (`ingredient_id`, `store_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_category` (`ingredient_category`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_ing` (`store_id`, `ingredient_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食材/原料主档表';

-- 采购记录表
CREATE TABLE IF NOT EXISTS `ingredient_purchase` (
  `purchase_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` varchar(50) NOT NULL,
  `supplier_id` int,
  `purchase_date` date NOT NULL,
  PRIMARY KEY (`purchase_id`),
  KEY `idx_date` (`purchase_date`),
  KEY `idx_ingredient` (`ingredient_id`),
  KEY `idx_purchase_date` (`purchase_date`),
  KEY `idx_store` (`store_id`),
  KEY `idx_supplier` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购记录表';

-- 厨房操作日志表
CREATE TABLE IF NOT EXISTS `kitchen_log` (
  `id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `booking_id` varchar(20),
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_booking` (`booking_id`),
  KEY `idx_created` (`created_at`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='厨房操作日志表';

-- 请假记录表
CREATE TABLE IF NOT EXISTS `leave_record` (
  `leave_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  PRIMARY KEY (`leave_id`),
  KEY `idx_staff_status` (`staff_id`, `status`),
  KEY `idx_store_leave` (`store_id`, `staff_id`, `start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假记录表';

-- 营销活动表
CREATE TABLE IF NOT EXISTS `marketing_activity` (
  `activity_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `activity_code` varchar(50) NOT NULL,
  `activity_type` varchar(50) NOT NULL,
  `is_active` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`activity_id`),
  KEY `idx_activity_type` (`activity_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_activity_code` (`activity_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='营销活动表';

-- 优惠券表
CREATE TABLE IF NOT EXISTS `marketing_coupon` (
  `coupon_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `coupon_code` varchar(50) NOT NULL,
  `coupon_type` varchar(20) NOT NULL,
  `is_active` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`coupon_id`),
  KEY `idx_coupon_type` (`coupon_type`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_coupon_code` (`coupon_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券表';

-- 优惠券领取使用记录表
CREATE TABLE IF NOT EXISTS `marketing_coupon_record` (
  `record_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `coupon_id` bigint NOT NULL,
  `member_id` bigint,
  `status` varchar(20) DEFAULT 'unused',
  PRIMARY KEY (`record_id`),
  KEY `idx_coupon_id` (`coupon_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券领取使用记录表';

-- 优惠规则表
CREATE TABLE IF NOT EXISTS `marketing_discount_rule` (
  `rule_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `rule_type` varchar(50) NOT NULL,
  `is_active` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`rule_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠规则表';

-- 抽奖活动表
CREATE TABLE IF NOT EXISTS `marketing_lottery` (
  `lottery_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `lottery_code` varchar(50) NOT NULL,
  `is_active` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`lottery_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_lottery_code` (`lottery_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='抽奖活动表';

-- 会员奖励规则表
CREATE TABLE IF NOT EXISTS `marketing_member_reward` (
  `reward_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `reward_type` varchar(50) NOT NULL,
  `is_active` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`reward_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_reward_type` (`reward_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员奖励规则表';

-- 优惠码表
CREATE TABLE IF NOT EXISTS `marketing_promo_code` (
  `code_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `promo_code` varchar(50) NOT NULL,
  `is_active` tinyint NOT NULL DEFAULT 1,
  PRIMARY KEY (`code_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_promo_code` (`promo_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠码表';

-- 会员卡主档表
CREATE TABLE IF NOT EXISTS `member_card` (
  `member_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `card_no` varchar(50) NOT NULL,
  `phone` varchar(20) NOT NULL,
  `level_id` int,
  `status` varchar(20) NOT NULL DEFAULT 'active',
  PRIMARY KEY (`member_id`),
  KEY `idx_level_id` (`level_id`),
  KEY `idx_phone` (`phone`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_card_no` (`card_no`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员卡主档表';

-- 会员消费记录表
CREATE TABLE IF NOT EXISTS `member_consume_record` (
  `consume_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `consume_no` varchar(50) NOT NULL,
  `member_id` bigint NOT NULL,
  `consume_date` date NOT NULL,
  `booking_id` int,
  PRIMARY KEY (`consume_id`),
  KEY `idx_booking_id` (`booking_id`),
  KEY `idx_consume_date` (`consume_date`),
  KEY `idx_consume_no` (`consume_no`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员消费记录表';

-- 会员等级表
CREATE TABLE IF NOT EXISTS `member_level` (
  `level_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `level_code` varchar(20) NOT NULL,
  PRIMARY KEY (`level_id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_level_code` (`level_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会员等级表';

-- 积分变动日志表
CREATE TABLE IF NOT EXISTS `member_point_log` (
  `log_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `member_id` bigint NOT NULL,
  `change_type` varchar(20) NOT NULL,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_change_type` (`change_type`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分变动日志表';

-- 积分规则表
CREATE TABLE IF NOT EXISTS `member_point_rule` (
  `rule_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `rule_type` varchar(50) NOT NULL,
  PRIMARY KEY (`rule_id`),
  KEY `idx_rule_type` (`rule_type`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分规则表';

-- 储值充值记录表
CREATE TABLE IF NOT EXISTS `member_recharge_record` (
  `recharge_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `recharge_no` varchar(50) NOT NULL,
  `member_id` bigint NOT NULL,
  `recharge_date` date NOT NULL,
  PRIMARY KEY (`recharge_id`),
  KEY `idx_member_id` (`member_id`),
  KEY `idx_recharge_date` (`recharge_date`),
  KEY `idx_recharge_no` (`recharge_no`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='储值充值记录表';

-- 菜单分类表(零点排版)
CREATE TABLE IF NOT EXISTS `menu_category` (
  `id` int NOT NULL,
  `category_code` varchar(50) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `category_code` (`category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜单分类表(零点排版)';

-- 加班申请表
CREATE TABLE IF NOT EXISTS `overtime` (
  `overtime_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  PRIMARY KEY (`overtime_id`),
  KEY `idx_staff_date` (`staff_id`, `overtime_date`),
  KEY `idx_store_ot` (`store_id`, `staff_id`, `overtime_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='加班申请表';

-- 套餐菜品明细表
CREATE TABLE IF NOT EXISTS `package_dish_detail` (
  `detail_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `package_id` varchar(20) NOT NULL,
  `dish_id` varchar(20) NOT NULL,
  PRIMARY KEY (`detail_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_package` (`package_id`),
  KEY `idx_pkg_dish_pkg` (`package_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐菜品明细表';

-- 套餐主档表
CREATE TABLE IF NOT EXISTS `package_master` (
  `package_id` varchar(20) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `occasion_type` varchar(20),
  `is_active` tinyint DEFAULT 1,
  PRIMARY KEY (`package_id`, `store_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_occasion` (`occasion_type`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_package` (`store_id`, `package_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='套餐主档表';

-- 采购订单主档表
CREATE TABLE IF NOT EXISTS `purchase_order` (
  `order_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `order_no` varchar(50) NOT NULL,
  `supplier_id` int,
  `order_date` date NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'pending',
  PRIMARY KEY (`order_id`),
  KEY `idx_order_date` (`order_date`),
  UNIQUE KEY `idx_order_no` (`order_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单主档表';

-- 采购订单明细表
CREATE TABLE IF NOT EXISTS `purchase_order_detail` (
  `detail_id` bigint NOT NULL,
  `order_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` int,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单明细表';

-- 采购入库单主档表
CREATE TABLE IF NOT EXISTS `purchase_receipt` (
  `receipt_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `receipt_no` varchar(50) NOT NULL,
  `receipt_date` date NOT NULL,
  `order_id` bigint,
  `supplier_id` int,
  PRIMARY KEY (`receipt_id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_receipt_date` (`receipt_date`),
  UNIQUE KEY `idx_receipt_no` (`receipt_no`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库单主档表';

-- 采购入库明细表
CREATE TABLE IF NOT EXISTS `purchase_receipt_detail` (
  `detail_id` bigint NOT NULL,
  `receipt_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` int,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购入库明细表';

-- 采购退货单主档表
CREATE TABLE IF NOT EXISTS `purchase_return` (
  `return_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `return_no` varchar(50) NOT NULL,
  `return_date` date NOT NULL,
  `receipt_id` bigint,
  `supplier_id` int,
  PRIMARY KEY (`return_id`),
  KEY `idx_receipt_id` (`receipt_id`),
  KEY `idx_return_date` (`return_date`),
  UNIQUE KEY `idx_return_no` (`return_no`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_supplier_id` (`supplier_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货单主档表';

-- 采购退货明细表
CREATE TABLE IF NOT EXISTS `purchase_return_detail` (
  `detail_id` bigint NOT NULL,
  `return_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` int,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_return_id` (`return_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购退货明细表';

-- 日报表
CREATE TABLE IF NOT EXISTS `report_daily` (
  `report_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `report_date` date NOT NULL,
  PRIMARY KEY (`report_id`),
  KEY `idx_report_date` (`report_date`),
  UNIQUE KEY `uk_store_date` (`store_id`, `report_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日报表';

-- 部门成本统计表
CREATE TABLE IF NOT EXISTS `report_department_cost` (
  `id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `stat_date` date NOT NULL,
  `department_id` int,
  PRIMARY KEY (`id`),
  KEY `idx_department_id` (`department_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='部门成本统计表';

-- 菜品销售统计表
CREATE TABLE IF NOT EXISTS `report_dish_sales` (
  `id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `stat_date` date NOT NULL,
  `dish_id` int,
  `category` varchar(50),
  PRIMARY KEY (`id`),
  KEY `idx_category` (`category`),
  KEY `idx_dish_id` (`dish_id`),
  KEY `idx_stat_date` (`stat_date`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品销售统计表';

-- 月报表
CREATE TABLE IF NOT EXISTS `report_monthly` (
  `report_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `report_month` varchar(7) NOT NULL,
  PRIMARY KEY (`report_id`),
  KEY `idx_report_month` (`report_month`),
  UNIQUE KEY `uk_store_month` (`store_id`, `report_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='月报表';

-- 员工KPI统计表
CREATE TABLE IF NOT EXISTS `report_staff_kpi` (
  `id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `stat_month` varchar(7) NOT NULL,
  `staff_id` int,
  `department` varchar(50),
  PRIMARY KEY (`id`),
  KEY `idx_department` (`department`),
  KEY `idx_staff_id` (`staff_id`),
  KEY `idx_stat_month` (`stat_month`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工KPI统计表';

-- 排班表
CREATE TABLE IF NOT EXISTS `schedule` (
  `schedule_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_id` int NOT NULL,
  PRIMARY KEY (`schedule_id`),
  KEY `idx_staff_date` (`staff_id`, `schedule_date`),
  KEY `idx_store_sched` (`store_id`, `staff_id`, `schedule_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排班表';

-- 员工主档表
CREATE TABLE IF NOT EXISTS `staff_master` (
  `staff_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `staff_account` varchar(20),
  `staff_phone` varchar(20),
  `employment_status` varchar(10) DEFAULT 'active',
  `dept_id` int,
  PRIMARY KEY (`staff_id`),
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
  `loss_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `loss_no` varchar(50) NOT NULL,
  `loss_date` date NOT NULL,
  `status` varchar(20) DEFAULT 'pending',
  PRIMARY KEY (`loss_id`),
  KEY `idx_loss_date` (`loss_date`),
  UNIQUE KEY `idx_loss_no` (`loss_no`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报损单主档表';

-- 报损明细表
CREATE TABLE IF NOT EXISTS `stock_loss_detail` (
  `detail_id` bigint NOT NULL,
  `loss_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` int,
  PRIMARY KEY (`detail_id`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_loss_id` (`loss_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报损明细表';

-- 盘点单主档表
CREATE TABLE IF NOT EXISTS `stock_take` (
  `take_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `take_no` varchar(50) NOT NULL,
  `take_date` date NOT NULL,
  `status` varchar(20) NOT NULL DEFAULT 'draft',
  PRIMARY KEY (`take_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_take_date` (`take_date`),
  UNIQUE KEY `idx_take_no` (`take_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点单主档表';

-- 盘点明细表
CREATE TABLE IF NOT EXISTS `stock_take_detail` (
  `detail_id` bigint NOT NULL,
  `take_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `ingredient_id` int,
  `diff_type` varchar(20),
  PRIMARY KEY (`detail_id`),
  KEY `idx_diff_type` (`diff_type`),
  KEY `idx_ingredient_id` (`ingredient_id`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_take_id` (`take_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='盘点明细表';

-- 库存调拨单表
CREATE TABLE IF NOT EXISTS `stock_transfer` (
  `transfer_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `transfer_no` varchar(50) NOT NULL,
  `transfer_date` date NOT NULL,
  `status` varchar(20) DEFAULT 'pending',
  PRIMARY KEY (`transfer_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`),
  KEY `idx_transfer_date` (`transfer_date`),
  UNIQUE KEY `idx_transfer_no` (`transfer_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存调拨单表';

-- 门店信息表
CREATE TABLE IF NOT EXISTS `store_info` (
  `store_id` bigint NOT NULL,
  `store_code` varchar(50) NOT NULL,
  `status` varchar(20) DEFAULT 'open',
  PRIMARY KEY (`store_id`),
  KEY `idx_status` (`status`),
  UNIQUE KEY `uk_store_code` (`store_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='门店信息表';

-- 供应商主档表
CREATE TABLE IF NOT EXISTS `supplier_master` (
  `supplier_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `supplier_name` varchar(100) NOT NULL,
  `is_active` tinyint DEFAULT 1,
  PRIMARY KEY (`supplier_id`),
  KEY `idx_active` (`is_active`),
  KEY `idx_name` (`supplier_name`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_supplier` (`store_id`, `supplier_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商主档表';

-- 数据字典表
CREATE TABLE IF NOT EXISTS `sys_dict` (
  `dict_id` bigint NOT NULL,
  `dict_code` varchar(100) NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`dict_id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_dict_code` (`dict_code`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典表';

-- 数据字典项表
CREATE TABLE IF NOT EXISTS `sys_dict_item` (
  `item_id` bigint NOT NULL,
  `dict_id` bigint NOT NULL,
  `dict_code` varchar(100) NOT NULL,
  `parent_id` bigint,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`item_id`),
  KEY `idx_dict_code` (`dict_code`, `store_id`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_store_id` (`store_id`),
  UNIQUE KEY `uk_dict_value` (`dict_id`, `item_value`, `store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据字典项表';

-- 系统通知表
CREATE TABLE IF NOT EXISTS `sys_notification` (
  `notify_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `notify_type` varchar(50) NOT NULL,
  `priority` varchar(20) DEFAULT 'normal',
  `sender_id` int,
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`notify_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_notify_type` (`notify_type`),
  KEY `idx_priority` (`priority`),
  KEY `idx_sender_id` (`sender_id`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
  `log_id` bigint NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `operator_id` int,
  `operation_type` varchar(50) NOT NULL,
  `operation_module` varchar(50),
  `status` varchar(20) DEFAULT 'success',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`log_id`),
  KEY `idx_created_at` (`created_at`),
  KEY `idx_operation_module` (`operation_module`),
  KEY `idx_operation_type` (`operation_type`),
  KEY `idx_operator_id` (`operator_id`),
  KEY `idx_status` (`status`),
  KEY `idx_store_id` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- 桌台主档表
CREATE TABLE IF NOT EXISTS `table_master` (
  `table_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  `table_number` varchar(10) NOT NULL,
  `table_area` varchar(20),
  `table_status` varchar(20) NOT NULL DEFAULT 'available',
  PRIMARY KEY (`table_id`),
  KEY `idx_area` (`table_area`),
  KEY `idx_number` (`table_number`),
  KEY `idx_status` (`table_status`),
  KEY `idx_store` (`store_id`),
  KEY `idx_store_table` (`store_id`, `table_number`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='桌台主档表';

-- 模板-分类关联表
CREATE TABLE IF NOT EXISTS `template_category_rel` (
  `id` int NOT NULL,
  `template_id` int NOT NULL,
  `menu_category_id` int NOT NULL,
  `store_id` bigint NOT NULL DEFAULT 1,
  PRIMARY KEY (`id`),
  KEY `idx_store_id` (`store_id`),
  KEY `menu_category_id` (`menu_category_id`),
  UNIQUE KEY `uk_template_category` (`template_id`, `menu_category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板-分类关联表';

-- 模板-菜品关联表
CREATE TABLE IF NOT EXISTS `template_dish_rel` (
  `id` int NOT NULL,
  `template_id` int NOT NULL,
  `menu_category_id` int,
  PRIMARY KEY (`id`),
  KEY `menu_category_id` (`menu_category_id`),
  UNIQUE KEY `uk_template_dish` (`template_id`, `dish_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='模板-菜品关联表';


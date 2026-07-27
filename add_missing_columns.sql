-- ============================================================
-- 补充缺失字段的ALTER TABLE语句
-- 共 15 张表
-- ============================================================

-- 系统管理员表 - 补充 1 个字段
ALTER TABLE `admin_users` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- AI对话历史表 - 补充 1 个字段
ALTER TABLE `ai_chat_history` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- AI记忆表 - 补充 1 个字段
ALTER TABLE `ai_memory` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 考勤月度汇总表 - 补充 3 个字段
ALTER TABLE `attendance_records` ADD COLUMN `staff_id` varchar(50);
ALTER TABLE `attendance_records` ADD COLUMN `staff_name` varchar(50);
ALTER TABLE `attendance_records` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 审计日志表 - 补充 1 个字段
ALTER TABLE `audit_logs` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 宴会菜单模板表 - 补充 1 个字段
ALTER TABLE `banquet_template` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 宴会类型-模板关联表 - 补充 1 个字段
ALTER TABLE `banquet_template_rel` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 宴会类型表 - 补充 1 个字段
ALTER TABLE `banquet_type` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 预订主档表 - 补充 6 个字段
ALTER TABLE `booking_master` ADD COLUMN `booking_no` varchar(30);
ALTER TABLE `booking_master` ADD COLUMN `package_id` varchar(20);
ALTER TABLE `booking_master` ADD COLUMN `booking_type` varchar(20) DEFAULT 'normal';
ALTER TABLE `booking_master` ADD COLUMN `deposit_amount` decimal(12,2) DEFAULT 0.00;
ALTER TABLE `booking_master` ADD COLUMN `package_name` varchar(100);
ALTER TABLE `booking_master` ADD COLUMN `status` varchar(32) DEFAULT 'pending';

-- 系统配置表 - 补充 1 个字段
ALTER TABLE `config` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 菜品主档表 - 补充 12 个字段
ALTER TABLE `dish_master` ADD COLUMN `category` varchar(50);
ALTER TABLE `dish_master` ADD COLUMN `category_id` varchar(64);
ALTER TABLE `dish_master` ADD COLUMN `cooking_method` varchar(50);
ALTER TABLE `dish_master` ADD COLUMN `dish_code` varchar(64);
ALTER TABLE `dish_master` ADD COLUMN `dish_name_en` varchar(100);
ALTER TABLE `dish_master` ADD COLUMN `is_seasonal` int DEFAULT 0;
ALTER TABLE `dish_master` ADD COLUMN `is_specialty` int DEFAULT 0;
ALTER TABLE `dish_master` ADD COLUMN `main_ingredients` text;
ALTER TABLE `dish_master` ADD COLUMN `taste` varchar(50);
ALTER TABLE `dish_master` ADD COLUMN `unit` varchar(32) DEFAULT '?';
ALTER TABLE `dish_master` ADD COLUMN `price` decimal(12,2) DEFAULT 0.00;
ALTER TABLE `dish_master` ADD COLUMN `remark` text;

-- 员工生命周期表 - 补充 3 个字段
ALTER TABLE `employee_lifecycle` ADD COLUMN `staff_id` varchar(50);
ALTER TABLE `employee_lifecycle` ADD COLUMN `staff_name` varchar(50);
ALTER TABLE `employee_lifecycle` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;

-- 食材/原料主档表 - 补充 1 个字段 (已存在，跳过)
-- ALTER TABLE `ingredient_master` ADD COLUMN `warning_threshold` decimal(12,2) DEFAULT 0.00;

-- 员工主档表 - 补充 32 个字段
ALTER TABLE `staff_master` ADD COLUMN `staff_no` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `avatar_url` varchar(500);
ALTER TABLE `staff_master` ADD COLUMN `nation` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `birth_date` date;
ALTER TABLE `staff_master` ADD COLUMN `native_place` varchar(100);
ALTER TABLE `staff_master` ADD COLUMN `marital_status` varchar(10);
ALTER TABLE `staff_master` ADD COLUMN `political_status` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `education` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `major` varchar(50);
ALTER TABLE `staff_master` ADD COLUMN `graduate_school` varchar(100);
ALTER TABLE `staff_master` ADD COLUMN `email` varchar(100);
ALTER TABLE `staff_master` ADD COLUMN `wechat` varchar(50);
ALTER TABLE `staff_master` ADD COLUMN `staff_rank` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `employment_type` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `hire_channel` varchar(30);
ALTER TABLE `staff_master` ADD COLUMN `probation_months` decimal(3,1);
ALTER TABLE `staff_master` ADD COLUMN `probation_start_date` date;
ALTER TABLE `staff_master` ADD COLUMN `probation_end_date` date;
ALTER TABLE `staff_master` ADD COLUMN `regular_date` date;
ALTER TABLE `staff_master` ADD COLUMN `leader_id` int;
ALTER TABLE `staff_master` ADD COLUMN `work_location` varchar(100);
ALTER TABLE `staff_master` ADD COLUMN `basic_salary` decimal(12,2);
ALTER TABLE `staff_master` ADD COLUMN `performance_salary` decimal(12,2);
ALTER TABLE `staff_master` ADD COLUMN `subsidy` decimal(12,2);
ALTER TABLE `staff_master` ADD COLUMN `bonus` decimal(12,2);
ALTER TABLE `staff_master` ADD COLUMN `social_insurance` decimal(12,2);
ALTER TABLE `staff_master` ADD COLUMN `housing_fund` decimal(12,2);
ALTER TABLE `staff_master` ADD COLUMN `bank_name` varchar(50);
ALTER TABLE `staff_master` ADD COLUMN `bank_account` varchar(30);
ALTER TABLE `staff_master` ADD COLUMN `account_holder` varchar(20);
ALTER TABLE `staff_master` ADD COLUMN `entry_age` int;
ALTER TABLE `staff_master` ADD COLUMN `work_years` decimal(5,2);

-- 模板-分类关联表 - 补充 1 个字段
ALTER TABLE `template_category_rel` ADD COLUMN `store_id` bigint NOT NULL DEFAULT 1;


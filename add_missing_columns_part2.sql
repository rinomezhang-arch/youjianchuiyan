-- ============================================================
-- 补充缺失字段的ALTER TABLE语句（剩余部分）
-- ============================================================

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

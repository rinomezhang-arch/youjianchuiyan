-- ======================================================================
-- 又见炊烟餐饮管理系统 - 五险一金模块迁移脚本
-- 来源：HR系统 soc_city + soc_insurance
-- 执行方式: mysql -u <user> -p banquet < hr_insurance.sql
-- 特性: 幂等（可重复执行）
-- 表前缀: hr_ 避免与现有 soc_city / soc_insurance 冲突
-- 新增: store_id 多租户字段
-- ======================================================================

-- ======================================================================
-- 1. 参保城市表 hr_soc_city
-- ======================================================================
CREATE TABLE IF NOT EXISTS `hr_soc_city` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT NOT NULL COMMENT '门店ID(多租户)',
  `name` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '参保城市',
  `average_salary` DECIMAL(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '职工上年度平均月工资',
  `lower_salary` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '职工上年度最低月工资',
  `soc_upper_limit` DECIMAL(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '职工社保缴纳基数上限',
  `soc_lower_limit` DECIMAL(10, 3) UNSIGNED NULL DEFAULT NULL COMMENT '职工社保缴纳基数下限',
  `hou_upper_limit` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '公积金缴纳基数上限',
  `hou_lower_limit` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '公积金缴纳基数下限',
  `per_pension_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '个人养老保险缴费比例',
  `com_pension_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业养老保险缴费比例',
  `per_medical_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '个人医疗保险缴费比例',
  `com_medical_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业医疗保险缴费比例',
  `per_unemployment_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '个人失业保险缴费比例',
  `com_unemployment_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业失业保险缴费比例',
  `com_maternity_rate` DECIMAL(6, 3) UNSIGNED NULL DEFAULT NULL COMMENT '企业生育保险缴费比例',
  `com_injury_rate` DECIMAL(6, 3) NULL DEFAULT NULL COMMENT '工伤保险企业缴纳比例',
  `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '参保城市表(HR)' ROW_FORMAT = DYNAMIC;

-- ======================================================================
-- 2. 员工五险一金表 hr_soc_insurance
-- ======================================================================
CREATE TABLE IF NOT EXISTS `hr_soc_insurance` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT NOT NULL COMMENT '门店ID(多租户)',
  `city_id` INT NULL DEFAULT NULL COMMENT '城市id',
  `staff_id` INT NULL DEFAULT NULL COMMENT '员工id',
  `house_base` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '公积金基数',
  `per_house_rate` DECIMAL(6, 3) NULL DEFAULT NULL COMMENT '公积金个人缴纳比例',
  `per_house_pay` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '公积金个人缴纳费用',
  `com_house_rate` DECIMAL(6, 3) NULL DEFAULT NULL COMMENT '公积金企业缴纳比例',
  `com_house_pay` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '公积金企业缴纳费用',
  `social_base` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '社保基数',
  `com_social_pay` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '社保企业缴纳费用',
  `per_social_pay` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '社保个人缴纳费用',
  `com_injury_rate` DECIMAL(6, 3) NULL DEFAULT NULL COMMENT '工伤保险企业缴纳比例',
  `social_remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社保备注',
  `house_remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公积金备注',
  `pay_month` VARCHAR(6) NULL DEFAULT NULL COMMENT '缴纳月份(YYYYMM)',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未支付，1已支付，2支付失败',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`),
  INDEX `idx_city_id` (`city_id`),
  INDEX `idx_staff_id` (`staff_id`),
  INDEX `idx_pay_month` (`pay_month`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工五险一金表(HR)' ROW_FORMAT = DYNAMIC;

-- ======================================================================
-- 3. 初始化种子数据（参考HR系统默认城市）
-- ======================================================================
INSERT INTO `hr_soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `created_at`, `updated_at`)
SELECT 1, '成都', 10000.000, 3000.000, 30000.000, 6000.000, 30000.000, 3000.000, 0.090, 0.160, 0.020, 0.090, 0.007, 0.006, 0.010, 0.002, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `hr_soc_city` WHERE `store_id` = 1 AND `name` = '成都');

INSERT INTO `hr_soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `created_at`, `updated_at`)
SELECT 1, '重庆', 8000.000, 3000.000, 24000.000, 4800.000, 24000.000, 3000.000, 0.100, 0.100, 0.020, 0.085, 0.005, 0.009, 0.028, 0.006, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `hr_soc_city` WHERE `store_id` = 1 AND `name` = '重庆');

INSERT INTO `hr_soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `created_at`, `updated_at`)
SELECT 1, '北京', 12000.000, 4000.000, 36000.000, 7200.000, 36000.000, 4000.000, 0.080, 0.120, 0.020, 0.090, 0.011, 0.005, 0.008, 0.004, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `hr_soc_city` WHERE `store_id` = 1 AND `name` = '北京');

INSERT INTO `hr_soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `created_at`, `updated_at`)
SELECT 1, '上海', 15000.000, 10000.000, 45000.000, 9000.000, 45000.000, 10000.000, 0.076, 0.022, 0.010, 0.020, 0.100, 0.120, 0.090, 0.005, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `hr_soc_city` WHERE `store_id` = 1 AND `name` = '上海');

INSERT INTO `hr_soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `created_at`, `updated_at`)
SELECT 1, '武汉', 5000.000, 3400.000, 15000.000, 3000.000, 15000.000, 3400.000, 0.100, 0.130, 0.100, 0.130, 0.140, 0.021, 0.025, 0.003, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `hr_soc_city` WHERE `store_id` = 1 AND `name` = '武汉');

INSERT INTO `hr_soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`, `created_at`, `updated_at`)
SELECT 1, '深圳', 13000.000, 10000.000, 39000.000, 7800.000, 39000.000, 10000.000, 0.050, 0.070, 0.030, 0.060, 0.015, 0.010, 0.010, 0.002, NULL, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM `hr_soc_city` WHERE `store_id` = 1 AND `name` = '深圳');
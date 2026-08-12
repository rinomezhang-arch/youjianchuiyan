-- ============================================================
-- 整合参考系统迁移脚本 v1
-- 来源：HR系统、餐厅点餐系统、采购管理系统
-- 日期：2026-08-10
-- ============================================================

-- 1. 新增请假类型配置表 (来源：HR系统 att_leave)
DROP TABLE IF EXISTS `leave_type`;
CREATE TABLE `leave_type` (
  `leave_type_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `type_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '请假类型名称',
  `type_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '类型编码',
  `is_paid` tinyint DEFAULT '1' COMMENT '是否带薪，0否1是',
  `max_days` int DEFAULT NULL COMMENT '年度最大天数',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`leave_type_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假类型配置表';

-- 插入默认请假类型
INSERT INTO `leave_type` (`store_id`, `type_name`, `type_code`, `is_paid`, `max_days`, `remark`) VALUES
(1, '年假', 'annual', 1, 15, '带薪年假'),
(1, '事假', 'personal', 0, 30, '无薪事假'),
(1, '病假', 'sick', 1, 90, '带薪病假'),
(1, '婚假', 'marriage', 1, 15, '婚假'),
(1, '产假', 'maternity', 1, 128, '产假'),
(1, '丧假', 'funeral', 1, 7, '丧假'),
(1, '调休', 'compensatory', 1, 30, '调休');

-- 2. 增强 overtime 表：添加加班工资倍数、奖金、补休字段 (来源：HR系统 att_overtime)
ALTER TABLE `overtime`
  ADD COLUMN `salary_multiple` decimal(4,2) DEFAULT '1.00' COMMENT '加班工资倍数' AFTER `hours`,
  ADD COLUMN `overtime_bonus` decimal(10,2) DEFAULT '0.00' COMMENT '加班奖金' AFTER `salary_multiple`,
  ADD COLUMN `make_up` tinyint DEFAULT '0' COMMENT '是否补休，0否1是' AFTER `overtime_bonus`;

-- 3. 新增扣款配置表 (来源：HR系统 sal_salary_deduct)
DROP TABLE IF EXISTS `salary_deduct`;
CREATE TABLE `salary_deduct` (
  `deduct_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dept_id` int DEFAULT NULL COMMENT '部门id',
  `deduct_type` tinyint NOT NULL DEFAULT '0' COMMENT '扣款类型，0迟到，1早退，2旷工，3休假',
  `deduct_amount` decimal(10,2) NOT NULL DEFAULT '0.00' COMMENT '每次扣款金额',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`deduct_id`),
  KEY `idx_store_dept` (`store_id`,`dept_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='工资扣款配置表';

-- 插入默认扣款配置
INSERT INTO `salary_deduct` (`store_id`, `dept_id`, `deduct_type`, `deduct_amount`, `remark`) VALUES
(1, NULL, 0, 100.00, '迟到扣款'),
(1, NULL, 1, 100.00, '早退扣款'),
(1, NULL, 2, 200.00, '旷工扣款'),
(1, NULL, 3, 0.00, '休假不扣款');

-- 4. 新增参保城市表 (来源：HR系统 soc_city)
DROP TABLE IF EXISTS `soc_city`;
CREATE TABLE `soc_city` (
  `city_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `city_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '参保城市',
  `average_salary` decimal(10,3) DEFAULT NULL COMMENT '职工上年度平均月工资',
  `lower_salary` decimal(10,3) DEFAULT NULL COMMENT '职工上年度最低月工资',
  `soc_upper_limit` decimal(10,3) DEFAULT NULL COMMENT '社保缴纳基数上限',
  `soc_lower_limit` decimal(10,3) DEFAULT NULL COMMENT '社保缴纳基数下限',
  `hou_upper_limit` decimal(10,3) DEFAULT NULL COMMENT '公积金缴纳基数上限',
  `hou_lower_limit` decimal(10,3) DEFAULT NULL COMMENT '公积金缴纳基数下限',
  `per_pension_rate` decimal(6,3) DEFAULT NULL COMMENT '个人养老保险缴费比例',
  `com_pension_rate` decimal(6,3) DEFAULT NULL COMMENT '企业养老保险缴费比例',
  `per_medical_rate` decimal(6,3) DEFAULT NULL COMMENT '个人医疗保险缴费比例',
  `com_medical_rate` decimal(6,3) DEFAULT NULL COMMENT '企业医疗保险缴费比例',
  `per_unemployment_rate` decimal(6,3) DEFAULT NULL COMMENT '个人失业保险缴费比例',
  `com_unemployment_rate` decimal(6,3) DEFAULT NULL COMMENT '企业失业保险缴费比例',
  `com_maternity_rate` decimal(6,3) DEFAULT NULL COMMENT '企业生育保险缴费比例',
  `com_injury_rate` decimal(6,3) DEFAULT '0.005' COMMENT '企业工伤保险缴费比例',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`city_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='参保城市配置表';

-- 插入默认参保城市数据
INSERT INTO `soc_city` (`store_id`, `city_name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`) VALUES
(1, '成都', 10000.000, 3000.000, 30000.000, 6000.000, 30000.000, 3000.000, 0.080, 0.160, 0.020, 0.090, 0.005, 0.006, 0.010, 0.005, NULL),
(1, '重庆', 8000.000, 3000.000, 24000.000, 4800.000, 24000.000, 3000.000, 0.080, 0.160, 0.020, 0.085, 0.005, 0.009, 0.028, 0.005, NULL),
(1, '北京', 12000.000, 4000.000, 36000.000, 7200.000, 36000.000, 4000.000, 0.080, 0.160, 0.020, 0.090, 0.005, 0.005, 0.008, 0.005, NULL),
(1, '上海', 15000.000, 10000.000, 45000.000, 9000.000, 45000.000, 10000.000, 0.080, 0.160, 0.020, 0.095, 0.005, 0.005, 0.010, 0.005, NULL),
(1, '深圳', 13000.000, 10000.000, 39000.000, 7800.000, 39000.000, 10000.000, 0.080, 0.140, 0.020, 0.060, 0.005, 0.007, 0.005, 0.005, NULL);

-- 5. 新增员工五险一金表 (来源：HR系统 soc_insurance)
DROP TABLE IF EXISTS `soc_insurance`;
CREATE TABLE `soc_insurance` (
  `insurance_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `city_id` int NOT NULL COMMENT '参保城市id',
  `staff_id` int NOT NULL COMMENT '员工id',
  `house_base` decimal(10,3) DEFAULT NULL COMMENT '公积金基数',
  `per_house_rate` decimal(6,3) DEFAULT NULL COMMENT '公积金个人缴纳比例',
  `per_house_pay` decimal(10,3) DEFAULT NULL COMMENT '公积金个人缴纳费用',
  `com_house_rate` decimal(6,3) DEFAULT NULL COMMENT '公积金企业缴纳比例',
  `com_house_pay` decimal(10,3) DEFAULT NULL COMMENT '公积金企业缴纳费用',
  `social_base` decimal(10,3) DEFAULT NULL COMMENT '社保基数',
  `com_social_pay` decimal(10,3) DEFAULT NULL COMMENT '社保企业缴纳费用',
  `per_social_pay` decimal(10,3) DEFAULT NULL COMMENT '社保个人缴纳费用',
  `com_injury_rate` decimal(6,3) DEFAULT '0.005' COMMENT '工伤保险企业缴纳比例',
  `social_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '社保备注',
  `house_remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '公积金备注',
  `pay_month` varchar(6) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '缴纳月份，格式YYYYMM',
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '0未支付，1已支付，2支付失败',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`insurance_id`),
  KEY `idx_store_staff` (`store_id`,`staff_id`),
  KEY `idx_city` (`city_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工五险一金表';

-- 6. 增强 department 表：添加上下班时间字段 (来源：HR系统 sys_dept)
ALTER TABLE `department`
  ADD COLUMN `mor_start_time` time DEFAULT NULL COMMENT '上午上班时间' AFTER `manager_id`,
  ADD COLUMN `mor_end_time` time DEFAULT NULL COMMENT '上午下班时间' AFTER `mor_start_time`,
  ADD COLUMN `aft_start_time` time DEFAULT NULL COMMENT '下午上班时间' AFTER `mor_end_time`,
  ADD COLUMN `aft_end_time` time DEFAULT NULL COMMENT '下午下班时间' AFTER `aft_start_time`,
  ADD COLUMN `total_work_hours` decimal(3,1) DEFAULT NULL COMMENT '每日工作总时长' AFTER `aft_end_time`;

-- 7. 新增菜品评论表 (来源：点餐系统 discusscaipinxinxi)
DROP TABLE IF EXISTS `dish_review`;
CREATE TABLE `dish_review` (
  `review_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dish_id` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '菜品id',
  `user_id` int DEFAULT NULL COMMENT '评论用户id',
  `user_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `rating` tinyint DEFAULT '5' COMMENT '评分1-5',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '评论内容',
  `reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '商家回复',
  `reply_time` datetime DEFAULT NULL COMMENT '回复时间',
  `is_show` tinyint DEFAULT '1' COMMENT '是否显示，0隐藏1显示',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`review_id`),
  KEY `idx_dish` (`dish_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='菜品评论表';

-- 8. 新增材料种类表 (来源：采购系统 cailiaozhonglei)
DROP TABLE IF EXISTS `ingredient_category`;
CREATE TABLE `ingredient_category` (
  `category_id` int NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `category_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '种类名称',
  `category_code` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '种类编码',
  `parent_id` int DEFAULT '0' COMMENT '父级种类id',
  `sort_order` int DEFAULT '0' COMMENT '排序',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`category_id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='材料种类表';

-- 插入默认材料种类
INSERT INTO `ingredient_category` (`store_id`, `category_name`, `category_code`, `parent_id`, `sort_order`, `remark`) VALUES
(1, '蔬菜', 'vegetable', 0, 1, '蔬菜类食材'),
(1, '肉类', 'meat', 0, 2, '肉类食材'),
(1, '水产', 'seafood', 0, 3, '水产类食材'),
(1, '调料', 'seasoning', 0, 4, '调料类'),
(1, '粮油', 'grain_oil', 0, 5, '粮油类'),
(1, '酒水', 'beverage', 0, 6, '酒水饮料'),
(1, '其他', 'other', 0, 99, '其他类');

-- 9. 为 ingredient_master 添加 category_id 外键关联
ALTER TABLE `ingredient_master`
  ADD COLUMN `category_id` int DEFAULT NULL COMMENT '材料种类id' AFTER `unit`;
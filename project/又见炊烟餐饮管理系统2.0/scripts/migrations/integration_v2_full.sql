-- ============================================================
-- 完整整合迁移脚本 v2 (Full)
-- 来源：HR系统(db_hrm)、餐厅点餐系统(springboot3258n)、采购管理系统(springbootwdw38)
-- 日期：2026-08-11
-- 说明：完整复刻三个参考系统的全部数据库表结构
-- ============================================================

-- ===========================================
-- 第一部分：HR系统表（已有部分 + 新增）
-- ===========================================

-- 1. 员工表 (已存在 hr_staff，确认字段对齐)
-- 如果 hr_staff 表不存在则创建
CREATE TABLE IF NOT EXISTS `hr_staff` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工id',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID',
  `code` varchar(20) DEFAULT '' COMMENT '员工编码',
  `name` varchar(20) NOT NULL DEFAULT '' COMMENT '员工姓名',
  `gender` tinyint UNSIGNED DEFAULT '0' COMMENT '性别，0男，1女，默认0',
  `pwd` char(32) DEFAULT NULL COMMENT '员工密码',
  `avatar` varchar(50) DEFAULT NULL COMMENT '员工头像',
  `birthday` date DEFAULT NULL COMMENT '员工生日',
  `phone` char(11) DEFAULT NULL COMMENT '员工电话',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `remark` varchar(200) DEFAULT NULL COMMENT '员工备注',
  `dept_id` int UNSIGNED DEFAULT NULL COMMENT '部门id',
  `status` tinyint UNSIGNED NOT NULL DEFAULT '1' COMMENT '员工状态，0异常，1正常',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0' COMMENT '逻辑删除，0未删除，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工表';

-- 2. 部门表 (已存在 hr_dept)
CREATE TABLE IF NOT EXISTS `hr_dept` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门id',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID',
  `code` varchar(20) DEFAULT NULL COMMENT '部门编码',
  `name` varchar(20) DEFAULT NULL COMMENT '部门名称',
  `mor_start_time` time DEFAULT NULL COMMENT '上午上班时间',
  `mor_end_time` time DEFAULT NULL COMMENT '上午下班时间',
  `aft_start_time` time DEFAULT NULL COMMENT '下午上班时间',
  `aft_end_time` time DEFAULT NULL COMMENT '下午下班时间',
  `total_work_time` decimal(3,1) DEFAULT NULL COMMENT '员工工作总时长',
  `remark` varchar(200) DEFAULT NULL COMMENT '部门备注',
  `parent_id` int UNSIGNED NOT NULL DEFAULT '0' COMMENT '父级部门id，0根部门',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0' COMMENT '逻辑删除，0未删除，1删除',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 3. 考勤表 (已存在 att_attendance)
CREATE TABLE IF NOT EXISTS `att_attendance` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_id` int DEFAULT NULL COMMENT '员工id',
  `mor_start_time` time DEFAULT NULL COMMENT '上午上班时间',
  `mor_end_time` time DEFAULT NULL COMMENT '上午下班时间',
  `aft_start_time` time DEFAULT NULL COMMENT '下午上班时间',
  `aft_end_time` time DEFAULT NULL COMMENT '下午下班时间',
  `attendance_date` date NOT NULL COMMENT '考勤日期',
  `status` tinyint DEFAULT NULL COMMENT '0正常，1迟到，2早退，3旷工，4休假',
  `remark` varchar(200) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_staff` (`store_id`,`staff_id`),
  KEY `idx_date` (`attendance_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工考勤表';

-- 4. 请假类型配置表 (新增)
DROP TABLE IF EXISTS `att_leave_type`;
CREATE TABLE `att_leave_type` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dept_id` int DEFAULT NULL COMMENT '部门id',
  `days` int UNSIGNED DEFAULT NULL COMMENT '休假天数',
  `type_num` tinyint UNSIGNED DEFAULT NULL COMMENT '休假类型，0年假，1事假，2病假，3婚假，4产假，5丧假，6调休',
  `status` tinyint UNSIGNED DEFAULT '1' COMMENT '0禁用，1正常，默认1',
  `remark` varchar(200) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='请假类型配置表';

-- 插入默认请假类型
INSERT INTO `att_leave_type` (`store_id`, `dept_id`, `days`, `type_num`, `status`, `remark`) VALUES
(1, 2, 40, 0, 1, '年假'),
(1, 2, 4, 1, 1, '事假'),
(1, 2, 10, 2, 1, '病假'),
(1, 2, 4, 3, 1, '婚假'),
(1, 2, 10, 4, 1, '产假'),
(1, 2, 2, 5, 1, '丧假'),
(1, 2, 10, 6, 1, '调休');

-- 5. 员工请假表 (新增)
DROP TABLE IF EXISTS `att_staff_leave`;
CREATE TABLE `att_staff_leave` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_id` int DEFAULT NULL COMMENT '员工id',
  `days` int DEFAULT NULL COMMENT '请假的天数',
  `type_num` int DEFAULT NULL COMMENT '请假类型id',
  `start_date` date DEFAULT NULL COMMENT '请假的开始日期',
  `status` tinyint UNSIGNED DEFAULT '0' COMMENT '0未审核，1审核通过，2驳回，3撤销',
  `remark` varchar(200) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_staff` (`store_id`,`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工请假表';

-- 6. 加班表 (已存在 overtime，增强字段)
-- 确保 overtime 表有 salary_multiple, overtime_bonus, make_up 字段
CREATE TABLE IF NOT EXISTS `att_overtime` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `salary_multiple` decimal(5,2) DEFAULT NULL COMMENT '工资倍数',
  `bonus` decimal(10,3) DEFAULT NULL COMMENT '加班奖金',
  `type_num` int DEFAULT NULL COMMENT '加班类型，0工作日，1周末，2节假日，3其他',
  `dept_id` int DEFAULT NULL COMMENT '部门id',
  `count_type` tinyint DEFAULT NULL COMMENT '0小时，1天，默认0',
  `remark` varchar(200) DEFAULT NULL,
  `is_time_off` tinyint UNSIGNED DEFAULT '0' COMMENT '0不补休，1补休，默认0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_dept` (`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='加班表';

-- 7. 工资表 (新增)
DROP TABLE IF EXISTS `sal_salary`;
CREATE TABLE `sal_salary` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_id` int UNSIGNED DEFAULT NULL COMMENT '员工id',
  `base_salary` decimal(10,3) DEFAULT NULL COMMENT '基础工资',
  `overtime_salary` decimal(10,3) DEFAULT NULL COMMENT '加班费',
  `subsidy` decimal(10,3) UNSIGNED DEFAULT NULL COMMENT '生活补贴',
  `bonus` decimal(10,3) UNSIGNED DEFAULT NULL COMMENT '奖金',
  `total_salary` decimal(10,3) UNSIGNED DEFAULT NULL COMMENT '总工资',
  `late_deduct` decimal(10,3) DEFAULT NULL COMMENT '迟到扣款',
  `leave_deduct` decimal(10,3) DEFAULT NULL COMMENT '休假扣款',
  `leave_early_deduct` decimal(10,3) DEFAULT NULL COMMENT '早退扣款',
  `absenteeism_deduct` decimal(10,3) DEFAULT NULL COMMENT '旷工扣款',
  `month` char(6) DEFAULT NULL COMMENT '月份，格式YYYYMM',
  `remark` varchar(200) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_staff` (`store_id`,`staff_id`),
  KEY `idx_month` (`month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工工资表';

-- 8. 工资扣款配置表 (新增)
DROP TABLE IF EXISTS `sal_salary_deduct`;
CREATE TABLE `sal_salary_deduct` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `dept_id` int UNSIGNED DEFAULT NULL COMMENT '部门id',
  `type_num` int UNSIGNED DEFAULT NULL COMMENT '扣款类型，0迟到，1早退，2旷工，3休假',
  `deduct` int UNSIGNED NOT NULL DEFAULT '0' COMMENT '每次扣款金额',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_dept` (`store_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资扣除表';

-- 插入默认扣款配置
INSERT INTO `sal_salary_deduct` (`store_id`, `dept_id`, `type_num`, `deduct`, `remark`) VALUES
(1, 2, 0, 100, '迟到扣款'),
(1, 2, 1, 50, '早退扣款'),
(1, 2, 2, 200, '旷工扣款'),
(1, 2, 3, 0, '休假不扣款');

-- 9. 参保城市表 (新增)
DROP TABLE IF EXISTS `soc_city`;
CREATE TABLE `soc_city` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `name` varchar(20) DEFAULT NULL COMMENT '参保城市',
  `average_salary` decimal(10,3) UNSIGNED DEFAULT NULL COMMENT '职工上年度平均月工资',
  `lower_salary` decimal(10,3) DEFAULT NULL COMMENT '职工上年度最低月工资',
  `soc_upper_limit` decimal(10,3) UNSIGNED DEFAULT NULL COMMENT '职工社保缴纳基数上限',
  `soc_lower_limit` decimal(10,3) UNSIGNED DEFAULT NULL COMMENT '职工社保缴纳基数下限',
  `hou_upper_limit` decimal(10,3) DEFAULT NULL COMMENT '公积金缴纳基数上限',
  `hou_lower_limit` decimal(10,3) DEFAULT NULL COMMENT '公积金缴纳基数下限',
  `per_pension_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '个人养老保险缴费比例',
  `com_pension_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '企业养老保险缴费比例',
  `per_medical_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '个人医疗保险缴费比例',
  `com_medical_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '企业医疗保险缴费比例',
  `per_unemployment_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '个人失业保险缴费比例',
  `com_unemployment_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '企业失业保险缴费比例',
  `com_maternity_rate` decimal(6,3) UNSIGNED DEFAULT NULL COMMENT '企业生育保险缴费比例',
  `com_injury_rate` decimal(6,3) DEFAULT '0.005' COMMENT '企业工伤保险缴费比例',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='参保城市表';

-- 插入默认参保城市数据
INSERT INTO `soc_city` (`store_id`, `name`, `average_salary`, `lower_salary`, `soc_upper_limit`, `soc_lower_limit`, `hou_upper_limit`, `hou_lower_limit`, `per_pension_rate`, `com_pension_rate`, `per_medical_rate`, `com_medical_rate`, `per_unemployment_rate`, `com_unemployment_rate`, `com_maternity_rate`, `com_injury_rate`, `remark`) VALUES
(1, '成都', 10000.000, 3000.000, 30000.000, 6000.000, 30000.000, 3000.000, 0.090, 0.160, 0.020, 0.090, 0.007, 0.006, 0.010, 0.005, NULL),
(1, '重庆', 8000.000, 3000.000, 24000.000, 4800.000, 24000.000, 3000.000, 0.100, 0.100, 0.020, 0.085, 0.005, 0.009, 0.028, 0.005, NULL),
(1, '北京', 12000.000, 4000.000, 36000.000, 7200.000, 36000.000, 4000.000, 0.080, 0.120, 0.020, 0.090, 0.011, 0.005, 0.008, 0.005, NULL),
(1, '上海', 15000.000, 10000.000, 45000.000, 9000.000, 45000.000, 10000.000, 0.076, 0.022, 0.010, 0.020, 0.100, 0.120, 0.090, 0.005, NULL),
(1, '深圳', 13000.000, 10000.000, 39000.000, 7800.000, 39000.000, 10000.000, 0.050, 0.070, 0.030, 0.060, 0.015, 0.010, 0.010, 0.005, NULL);

-- 10. 员工五险一金表 (新增)
DROP TABLE IF EXISTS `soc_insurance`;
CREATE TABLE `soc_insurance` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `city_id` int DEFAULT NULL COMMENT '城市id',
  `staff_id` int DEFAULT NULL COMMENT '员工id',
  `house_base` decimal(10,3) DEFAULT NULL COMMENT '公积金基数',
  `per_house_rate` decimal(6,3) DEFAULT NULL COMMENT '公积金个人缴纳比例',
  `per_house_pay` decimal(10,3) DEFAULT NULL COMMENT '公积金个人缴纳费用',
  `com_house_rate` decimal(6,3) DEFAULT NULL COMMENT '公积金企业缴纳比例',
  `com_house_pay` decimal(10,3) DEFAULT NULL COMMENT '公积金企业缴纳费用',
  `social_base` decimal(10,3) DEFAULT NULL COMMENT '社保基数',
  `com_social_pay` decimal(10,3) DEFAULT NULL COMMENT '社保企业缴纳费用',
  `per_social_pay` decimal(10,3) DEFAULT NULL COMMENT '社保个人缴纳费用',
  `com_injury_rate` decimal(6,3) DEFAULT NULL COMMENT '工伤保险企业缴纳比例',
  `social_remark` varchar(200) DEFAULT NULL COMMENT '社保备注',
  `house_remark` varchar(200) DEFAULT NULL COMMENT '公积金备注',
  `status` tinyint UNSIGNED NOT NULL DEFAULT '0' COMMENT '0未支付，1已支付，2支付失败',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_staff` (`store_id`,`staff_id`),
  KEY `idx_city` (`city_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工五险一金表';

-- 11. 菜单表 (新增)
DROP TABLE IF EXISTS `per_menu`;
CREATE TABLE `per_menu` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单id',
  `store_id` bigint NOT NULL DEFAULT '1',
  `code` varchar(20) DEFAULT NULL COMMENT '菜单编码',
  `name` varchar(20) DEFAULT NULL COMMENT '菜单名称',
  `icon` varchar(20) DEFAULT NULL,
  `path` varchar(100) DEFAULT NULL COMMENT '菜单路径',
  `parent_id` int UNSIGNED NOT NULL DEFAULT '0' COMMENT '父菜单id，0代表根菜单',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单表';

-- 插入默认菜单
INSERT INTO `per_menu` (`store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`) VALUES
(1, 'staff', '员工管理', 'user', '/staff', 5, NULL),
(1, 'docs', '文件管理', 'folder', '/docs', 5, NULL),
(1, 'role', '角色管理', 's-custom', '/role', 6, ''),
(1, 'menu', '菜单管理', 'collection', '/menu', 6, NULL),
(1, 'system', '系统管理', 's-management', '/system', 0, ''),
(1, 'permission', '权限管理', 's-cooperation', '/permission', 0, NULL),
(1, 'department', '部门管理', 's-operation', '/department', 5, NULL),
(1, 'attendance', '考勤管理', 'edit', '/attendance', 0, NULL),
(1, 'insurance', '五险一金', 's-data', '/insurance', 17, NULL),
(1, 'salary', '薪资管理', 'data-line', '/salary', 17, NULL),
(1, 'money', '财务管理', 's-finance', '/money', 0, NULL),
(1, 'city', '参保城市', 'coordinate', '/city', 17, NULL),
(1, 'leave', '请假审批', 'suitcase', '/leave', 8, NULL),
(1, 'performance', '考勤表现', 'reading', '/performance', 8, NULL);

-- 12. 角色表 (新增)
DROP TABLE IF EXISTS `per_role`;
CREATE TABLE `per_role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色id',
  `store_id` bigint NOT NULL DEFAULT '1',
  `code` varchar(20) DEFAULT NULL COMMENT '角色编码',
  `name` varchar(20) DEFAULT NULL COMMENT '角色名称',
  `remark` varchar(200) DEFAULT NULL COMMENT '角色备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工角色表';

-- 插入默认角色
INSERT INTO `per_role` (`store_id`, `code`, `name`, `remark`) VALUES
(1, 'admin', '管理员', ''),
(1, 'manager', '总经理', ''),
(1, 'hr', '人事经理', NULL),
(1, 'finance_minister', '薪酬总监', NULL);

-- 13. 角色菜单关系表 (新增)
DROP TABLE IF EXISTS `per_role_menu`;
CREATE TABLE `per_role_menu` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `role_id` int UNSIGNED NOT NULL COMMENT '角色id',
  `menu_id` int UNSIGNED NOT NULL COMMENT '菜单id',
  `status` tinyint UNSIGNED NOT NULL DEFAULT '1' COMMENT '0禁用，1正常，默认1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_role` (`store_id`,`role_id`),
  KEY `idx_menu` (`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色菜单关系表';

-- 14. 员工角色关系表 (新增)
DROP TABLE IF EXISTS `per_staff_role`;
CREATE TABLE `per_staff_role` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `staff_id` int UNSIGNED DEFAULT NULL COMMENT '员工id',
  `role_id` int UNSIGNED DEFAULT NULL COMMENT '角色id',
  `status` tinyint UNSIGNED NOT NULL DEFAULT '1' COMMENT '0禁用，1正常，默认1',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_store_staff` (`store_id`,`staff_id`),
  KEY `idx_role` (`role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工角色关系表';

-- 15. 文件表 (新增)
DROP TABLE IF EXISTS `sys_docs`;
CREATE TABLE `sys_docs` (
  `id` int UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` bigint NOT NULL DEFAULT '1',
  `name` varchar(200) DEFAULT NULL COMMENT '文件名称',
  `type` varchar(10) DEFAULT NULL COMMENT '文件类型',
  `old_name` varchar(200) DEFAULT NULL COMMENT '文件的原名称',
  `md5` varchar(200) DEFAULT NULL COMMENT '文件md5信息',
  `size` bigint UNSIGNED DEFAULT NULL COMMENT '文件大小KB',
  `staff_id` int DEFAULT NULL COMMENT '文件上传者id',
  `remark` varchar(200) DEFAULT NULL COMMENT '文件备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  `is_deleted` tinyint UNSIGNED NOT NULL DEFAULT '0' COMMENT '0未删除，1已删除',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_staff` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件表';

-- ===========================================
-- 第二部分：点餐系统表（新增）
-- ===========================================

-- 16. 菜品类型表 (新增)
DROP TABLE IF EXISTS `dish_type`;
CREATE TABLE `dish_type` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `type_name` varchar(200) NOT NULL COMMENT '菜品类型',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品类型';

-- 插入默认菜品类型
INSERT INTO `dish_type` (`store_id`, `type_name`) VALUES
(1, '凉菜'), (1, '热菜'), (1, '汤品'), (1, '主食'), (1, '饮品'), (1, '甜品'), (1, '海鲜'), (1, '特色菜');

-- 17. 餐桌信息表 (新增)
DROP TABLE IF EXISTS `table_info`;
CREATE TABLE `table_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_number` varchar(200) NOT NULL COMMENT '餐桌号码',
  `image` longtext COMMENT '图片',
  `capacity` int DEFAULT NULL COMMENT '可坐人数',
  `location` varchar(200) DEFAULT NULL COMMENT '餐桌位置',
  `status` varchar(200) DEFAULT NULL COMMENT '餐桌状态',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_table_number` (`table_number`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐桌信息';

-- 18. 餐桌使用记录表 (新增)
DROP TABLE IF EXISTS `table_usage`;
CREATE TABLE `table_usage` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_number` varchar(200) DEFAULT NULL COMMENT '餐桌号码',
  `table_location` varchar(200) DEFAULT NULL COMMENT '餐桌位置',
  `capacity` int DEFAULT NULL COMMENT '可坐人数',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  `username` varchar(200) DEFAULT NULL COMMENT '用户名',
  `name` varchar(200) DEFAULT NULL COMMENT '姓名',
  `phone` varchar(200) DEFAULT NULL COMMENT '手机',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐桌使用';

-- 19. 购物车表 (新增)
DROP TABLE IF EXISTS `dish_cart`;
CREATE TABLE `dish_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_name` varchar(200) DEFAULT 'dish_master' COMMENT '商品表名',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `good_id` bigint NOT NULL COMMENT '商品id',
  `good_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '图片',
  `buy_number` int NOT NULL COMMENT '购买数量',
  `price` float DEFAULT NULL COMMENT '单价',
  `discount_price` float DEFAULT NULL COMMENT '会员价',
  `good_type` varchar(200) DEFAULT NULL COMMENT '商品类型',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 20. 菜品评论表 (新增)
DROP TABLE IF EXISTS `dish_review`;
CREATE TABLE `dish_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `ref_id` bigint NOT NULL COMMENT '关联表id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `avatar_url` longtext COMMENT '头像',
  `nickname` varchar(200) DEFAULT NULL COMMENT '用户名',
  `content` longtext NOT NULL COMMENT '评论内容',
  `reply` longtext COMMENT '回复内容',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_ref` (`store_id`,`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜品评论表';

-- 21. 餐厅资讯表 (新增)
DROP TABLE IF EXISTS `restaurant_news`;
CREATE TABLE `restaurant_news` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `introduction` longtext COMMENT '简介',
  `picture` longtext NOT NULL COMMENT '图片',
  `content` longtext NOT NULL COMMENT '内容',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='餐厅资讯';

-- 22. 订单表 (新增)
DROP TABLE IF EXISTS `dish_order`;
CREATE TABLE `dish_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `order_id` varchar(200) NOT NULL COMMENT '订单编号',
  `table_name` varchar(200) DEFAULT 'dish_master' COMMENT '商品表名',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `good_id` bigint NOT NULL COMMENT '商品id',
  `good_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '商品图片',
  `buy_number` int NOT NULL COMMENT '购买数量',
  `price` float NOT NULL DEFAULT '0' COMMENT '价格',
  `discount_price` float DEFAULT '0' COMMENT '折扣价格',
  `total` float NOT NULL DEFAULT '0' COMMENT '总价格',
  `discount_total` float DEFAULT '0' COMMENT '折扣总价格',
  `type` int DEFAULT '1' COMMENT '支付类型',
  `status` varchar(200) DEFAULT NULL COMMENT '状态',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `tel` varchar(200) DEFAULT NULL COMMENT '电话',
  `consignee` varchar(200) DEFAULT NULL COMMENT '收货人',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `logistics` longtext COMMENT '物流',
  `good_type` varchar(200) DEFAULT NULL COMMENT '商品类型',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 23. 收藏表 (新增)
DROP TABLE IF EXISTS `dish_storeup`;
CREATE TABLE `dish_storeup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `ref_id` bigint DEFAULT NULL COMMENT '商品id',
  `table_name` varchar(200) DEFAULT NULL COMMENT '表名',
  `name` varchar(200) NOT NULL COMMENT '名称',
  `picture` longtext NOT NULL COMMENT '图片',
  `type` varchar(200) DEFAULT '1' COMMENT '类型(1:收藏,21:赞,22:踩,31:竞拍参与,41:关注)',
  `intel_type` varchar(200) DEFAULT NULL COMMENT '推荐类型',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 24. 用户地址表 (新增)
DROP TABLE IF EXISTS `user_address`;
CREATE TABLE `user_address` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `address` varchar(200) NOT NULL COMMENT '地址',
  `name` varchar(200) NOT NULL COMMENT '收货人',
  `phone` varchar(200) NOT NULL COMMENT '电话',
  `is_default` varchar(200) NOT NULL COMMENT '是否默认地址[是/否]',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='地址';

-- ===========================================
-- 第三部分：采购系统表（新增）
-- ===========================================

-- 25. 供应商表 (新增)
DROP TABLE IF EXISTS `supplier`;
CREATE TABLE `supplier` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `supplier_account` varchar(200) NOT NULL COMMENT '供应商账号',
  `password` varchar(200) NOT NULL COMMENT '密码',
  `supplier_name` varchar(200) NOT NULL COMMENT '供应商名称',
  `image` longtext COMMENT '图片',
  `contact_person` varchar(200) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(200) DEFAULT NULL COMMENT '联系电话',
  `supplier_address` varchar(200) DEFAULT NULL COMMENT '供应商地址',
  `balance` float DEFAULT '0' COMMENT '余额',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_supplier_account` (`supplier_account`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='供应商';

-- 插入默认供应商
INSERT INTO `supplier` (`store_id`, `supplier_account`, `password`, `supplier_name`, `contact_person`, `contact_phone`, `supplier_address`, `balance`) VALUES
(1, 'supplier01', '123456', '兴兴供应商', '张三', '13823855552', '北京路9号', 0),
(1, 'supplier02', '123456', '供应商名称2', '联系人2', '联系电话2', '供应商地址2', 200),
(1, 'supplier03', '123456', '供应商名称3', '联系人3', '联系电话3', '供应商地址3', 200);

-- 26. 材料种类表 (新增)
DROP TABLE IF EXISTS `material_category`;
CREATE TABLE `material_category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `category_name` varchar(200) DEFAULT NULL COMMENT '材料种类',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_category_name` (`category_name`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='材料种类';

-- 插入默认材料种类
INSERT INTO `material_category` (`store_id`, `category_name`) VALUES
(1, '蔬菜'), (1, '肉类'), (1, '水产'), (1, '调料'), (1, '粮油'), (1, '酒水'), (1, '其他');

-- 27. 材料信息表 (新增)
DROP TABLE IF EXISTS `material_info`;
CREATE TABLE `material_info` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `material_name` varchar(200) NOT NULL COMMENT '材料名称',
  `image` longtext COMMENT '图片',
  `category` varchar(200) NOT NULL COMMENT '材料种类',
  `specification` varchar(200) DEFAULT NULL COMMENT '材料规格',
  `detail` longtext COMMENT '材料详情',
  `supplier_account` varchar(200) DEFAULT NULL COMMENT '供应商账号',
  `supplier_name` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  `single_limit` int DEFAULT NULL COMMENT '单限',
  `stock` int DEFAULT NULL COMMENT '库存',
  `click_time` datetime DEFAULT NULL COMMENT '最近点击时间',
  `price` float NOT NULL COMMENT '价格',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='材料信息';

-- 28. 采购入库表 (新增)
DROP TABLE IF EXISTS `purchase_in`;
CREATE TABLE `purchase_in` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `material_name` varchar(200) DEFAULT NULL COMMENT '材料名称',
  `category` varchar(200) DEFAULT NULL COMMENT '材料种类',
  `specification` varchar(200) DEFAULT NULL COMMENT '材料规格',
  `stock` int NOT NULL COMMENT '库存',
  `in_time` datetime DEFAULT NULL COMMENT '入库时间',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `supplier_account` varchar(200) DEFAULT NULL COMMENT '供应商账号',
  `supplier_name` varchar(200) DEFAULT NULL COMMENT '供应商名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='采购入库';

-- 29. 材料评论表 (新增)
DROP TABLE IF EXISTS `material_review`;
CREATE TABLE `material_review` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `ref_id` bigint NOT NULL COMMENT '关联表id',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `avatar_url` longtext COMMENT '头像',
  `nickname` varchar(200) DEFAULT NULL COMMENT '用户名',
  `content` longtext NOT NULL COMMENT '评论内容',
  `reply` longtext COMMENT '回复内容',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_ref` (`store_id`,`ref_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='材料信息评论表';

-- 30. 采购购物车表 (新增)
DROP TABLE IF EXISTS `purchase_cart`;
CREATE TABLE `purchase_cart` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `table_name` varchar(200) DEFAULT 'material_info' COMMENT '商品表名',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `good_id` bigint NOT NULL COMMENT '商品id',
  `good_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '图片',
  `buy_number` int NOT NULL COMMENT '购买数量',
  `price` float DEFAULT NULL COMMENT '单价',
  `discount_price` float DEFAULT NULL COMMENT '会员价',
  `supplier_account` varchar(200) DEFAULT NULL COMMENT '商户名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='购物车表';

-- 31. 采购订单表 (新增)
DROP TABLE IF EXISTS `purchase_order`;
CREATE TABLE `purchase_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `order_id` varchar(200) NOT NULL COMMENT '订单编号',
  `table_name` varchar(200) DEFAULT 'material_info' COMMENT '商品表名',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `good_id` bigint NOT NULL COMMENT '商品id',
  `good_name` varchar(200) DEFAULT NULL COMMENT '商品名称',
  `picture` longtext COMMENT '商品图片',
  `buy_number` int NOT NULL COMMENT '购买数量',
  `price` float NOT NULL DEFAULT '0' COMMENT '价格',
  `discount_price` float DEFAULT '0' COMMENT '折扣价格',
  `total` float NOT NULL DEFAULT '0' COMMENT '总价格',
  `discount_total` float DEFAULT '0' COMMENT '折扣总价格',
  `type` int DEFAULT '1' COMMENT '支付类型',
  `status` varchar(200) DEFAULT NULL COMMENT '状态',
  `address` varchar(200) DEFAULT NULL COMMENT '地址',
  `tel` varchar(200) DEFAULT NULL COMMENT '电话',
  `consignee` varchar(200) DEFAULT NULL COMMENT '收货人',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `logistics` longtext COMMENT '物流',
  `supplier_account` varchar(200) DEFAULT NULL COMMENT '商户名称',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单';

-- 32. 采购收藏表 (新增)
DROP TABLE IF EXISTS `purchase_storeup`;
CREATE TABLE `purchase_storeup` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `user_id` bigint NOT NULL COMMENT '用户id',
  `ref_id` bigint DEFAULT NULL COMMENT '商品id',
  `table_name` varchar(200) DEFAULT NULL COMMENT '表名',
  `name` varchar(200) NOT NULL COMMENT '名称',
  `picture` longtext NOT NULL COMMENT '图片',
  `type` varchar(200) DEFAULT '1' COMMENT '类型(1:收藏,21:赞,22:踩,31:竞拍参与,41:关注)',
  `intel_type` varchar(200) DEFAULT NULL COMMENT '推荐类型',
  `remark` varchar(200) DEFAULT NULL COMMENT '备注',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_store_user` (`store_id`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';

-- 33. 系统配置表 (新增)
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键',
  `store_id` bigint NOT NULL DEFAULT '1',
  `name` varchar(100) NOT NULL COMMENT '配置参数名称',
  `value` varchar(100) DEFAULT NULL COMMENT '配置参数值',
  PRIMARY KEY (`id`),
  KEY `idx_store` (`store_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置';

INSERT INTO `sys_config` (`store_id`, `name`, `value`) VALUES
(1, 'picture1', 'upload/picture1.jpg'),
(1, 'picture2', 'upload/picture2.jpg'),
(1, 'picture3', 'upload/picture3.jpg');

-- ===========================================
-- 完成
-- ===========================================
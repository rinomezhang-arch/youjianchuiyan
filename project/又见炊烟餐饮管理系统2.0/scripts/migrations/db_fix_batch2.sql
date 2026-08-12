-- ================================================================
-- 数据库修复SQL - 第二批
-- 任务1: 扩容 booking_master.booking_id 到 varchar(50)
-- 任务2: 删除3张重复旧表 (dishes/packages/categories)
-- 任务3: 新建15张缺失表 (工程管理9 + 工具资产6)
-- 执行时间: 2026-08-02
-- ================================================================
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS=0;

-- ================================================================
-- 任务1: 扩容 booking_master.booking_id
-- ================================================================
-- 现状: booking_master.booking_id=varchar(20), 其他7表=varchar(50)
-- 方向: 统一为 varchar(50) (扩容不丢数据)
ALTER TABLE `booking_master` MODIFY COLUMN `booking_id` VARCHAR(50) NULL COMMENT '预订业务编号';

-- ================================================================
-- 任务2: 删除3张重复旧表
-- ================================================================
-- 已核实: 无外键引用, 0行数据, 是旧版表
-- 保留主表: dish_master / meal_package / dish_category

-- 2.1 dishes 表 (旧菜品表, 主表为 dish_master)
-- 注意: 之前已删除其 FULLTEXT 索引 idx_dishes_fulltext
DROP TABLE IF EXISTS `dishes`;

-- 2.2 packages 表 (旧套餐表, 主表为 meal_package)
DROP TABLE IF EXISTS `packages`;

-- 2.3 categories 表 (旧分类表, 主表为 dish_category)
DROP TABLE IF EXISTS `categories`;

-- ================================================================
-- 任务3: 新建15张缺失表
-- ================================================================

-- ============== 工程管理模块 (9张) ==============

-- 3.1 工程工单表
CREATE TABLE IF NOT EXISTS `engineering_work_order` (
  `work_order_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `work_order_no` VARCHAR(32) NOT NULL COMMENT '工单编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `work_type` VARCHAR(32) NOT NULL COMMENT '工单类型(维修/保养/安装/其他)',
  `title` VARCHAR(128) NOT NULL COMMENT '工单标题',
  `description` TEXT NULL COMMENT '工单描述',
  `assignee_id` BIGINT NULL COMMENT '指派人ID(关联staff_master.staff_id)',
  `priority` TINYINT NOT NULL DEFAULT 2 COMMENT '优先级(1低/2中/3高/4紧急)',
  `status` VARCHAR(16) NOT NULL DEFAULT '待处理' COMMENT '状态(待处理/处理中/已完成/已取消)',
  `scheduled_date` DATE NULL COMMENT '计划执行日期',
  `started_at` DATETIME NULL COMMENT '实际开始时间',
  `completed_at` DATETIME NULL COMMENT '实际完成时间',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`work_order_id`),
  UNIQUE KEY `uk_work_order_no` (`work_order_no`),
  KEY `idx_engineering_work_order_store` (`store_id`),
  KEY `idx_engineering_work_order_assignee` (`assignee_id`),
  KEY `idx_engineering_work_order_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程工单表';

-- 3.2 工程巡检记录表
CREATE TABLE IF NOT EXISTS `engineering_inspection` (
  `inspection_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_no` VARCHAR(32) NOT NULL COMMENT '巡检编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `inspector_id` BIGINT NULL COMMENT '巡检人ID(关联staff_master.staff_id)',
  `inspection_date` DATE NOT NULL COMMENT '巡检日期',
  `shift` ENUM('早班','中班','晚班','夜班') NOT NULL COMMENT '班次',
  `area` VARCHAR(64) NULL COMMENT '巡检区域',
  `result` ENUM('正常','异常','需跟进') NOT NULL DEFAULT '正常' COMMENT '巡检结果',
  `findings` TEXT NULL COMMENT '发现问题描述',
  `status` VARCHAR(16) NOT NULL DEFAULT '已完成' COMMENT '状态(已完成/待跟进/已关闭)',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inspection_id`),
  UNIQUE KEY `uk_inspection_no` (`inspection_no`),
  KEY `idx_engineering_inspection_store` (`store_id`),
  KEY `idx_engineering_inspection_inspector` (`inspector_id`),
  KEY `idx_engineering_inspection_date` (`inspection_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程巡检记录表';

-- 3.3 巡检照片表
CREATE TABLE IF NOT EXISTS `inspection_photos` (
  `photo_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inspection_id` BIGINT NOT NULL COMMENT '巡检记录ID(关联engineering_inspection.inspection_id)',
  `photo_url` VARCHAR(512) NOT NULL COMMENT '照片URL',
  `photo_type` ENUM('现场','问题','完成','其他') NOT NULL DEFAULT '现场' COMMENT '照片类型',
  `remark` VARCHAR(255) NULL COMMENT '照片说明',
  `upload_by` BIGINT NULL COMMENT '上传人ID(关联staff_master.staff_id)',
  `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  PRIMARY KEY (`photo_id`),
  KEY `idx_inspection_photos_inspection` (`inspection_id`),
  KEY `idx_inspection_photos_type` (`photo_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='巡检照片表';

-- 3.4 工程备件表
CREATE TABLE IF NOT EXISTS `engineering_spare_part` (
  `spare_part_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `part_no` VARCHAR(32) NOT NULL COMMENT '备件编号',
  `part_name` VARCHAR(128) NOT NULL COMMENT '备件名称',
  `category` VARCHAR(64) NULL COMMENT '备件分类',
  `spec` VARCHAR(128) NULL COMMENT '规格型号',
  `unit` VARCHAR(16) NOT NULL DEFAULT '个' COMMENT '单位',
  `stock_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '库存数量',
  `min_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '最低库存阈值',
  `max_qty` DECIMAL(12,2) NULL COMMENT '最高库存阈值',
  `unit_price` DECIMAL(10,2) NULL COMMENT '单价',
  `location` VARCHAR(64) NULL COMMENT '存放位置',
  `remark` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`spare_part_id`),
  UNIQUE KEY `uk_spare_part_no` (`part_no`, `store_id`),
  KEY `idx_engineering_spare_part_store` (`store_id`),
  KEY `idx_engineering_spare_part_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工程备件表';

-- 3.5 装修项目表
CREATE TABLE IF NOT EXISTS `decoration_project` (
  `project_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_no` VARCHAR(32) NOT NULL COMMENT '项目编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `project_name` VARCHAR(128) NOT NULL COMMENT '项目名称',
  `manager_id` BIGINT NULL COMMENT '项目负责人ID(关联staff_master.staff_id)',
  `contractor` VARCHAR(128) NULL COMMENT '施工方',
  `budget` DECIMAL(14,2) NULL COMMENT '预算金额',
  `actual_cost` DECIMAL(14,2) NULL COMMENT '实际成本',
  `progress` TINYINT NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
  `status` VARCHAR(16) NOT NULL DEFAULT '规划中' COMMENT '状态(规划中/进行中/已完成/已暂停/已取消)',
  `start_date` DATE NULL COMMENT '计划开始日期',
  `end_date` DATE NULL COMMENT '计划结束日期',
  `actual_start_date` DATE NULL COMMENT '实际开始日期',
  `actual_end_date` DATE NULL COMMENT '实际结束日期',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_decoration_project_no` (`project_no`),
  KEY `idx_decoration_project_store` (`store_id`),
  KEY `idx_decoration_project_manager` (`manager_id`),
  KEY `idx_decoration_project_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='装修项目表';

-- 3.6 楼层工程表
CREATE TABLE IF NOT EXISTS `floor_project` (
  `project_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `project_no` VARCHAR(32) NOT NULL COMMENT '项目编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `floor` VARCHAR(16) NOT NULL COMMENT '楼层',
  `project_name` VARCHAR(128) NOT NULL COMMENT '项目名称',
  `manager_id` BIGINT NULL COMMENT '项目负责人ID(关联staff_master.staff_id)',
  `progress` TINYINT NOT NULL DEFAULT 0 COMMENT '进度百分比(0-100)',
  `status` VARCHAR(16) NOT NULL DEFAULT '进行中' COMMENT '状态(规划中/进行中/已完成/已暂停)',
  `start_date` DATE NULL COMMENT '开始日期',
  `end_date` DATE NULL COMMENT '结束日期',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`project_id`),
  UNIQUE KEY `uk_floor_project_no` (`project_no`),
  KEY `idx_floor_project_store` (`store_id`),
  KEY `idx_floor_project_manager` (`manager_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='楼层工程表';

-- 3.7 安全隐患表
CREATE TABLE IF NOT EXISTS `safety_issue` (
  `issue_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `issue_no` VARCHAR(32) NOT NULL COMMENT '隐患编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `location` VARCHAR(128) NOT NULL COMMENT '隐患位置',
  `category` VARCHAR(64) NULL COMMENT '隐患类别(消防/电气/燃气/结构/其他)',
  `description` TEXT NOT NULL COMMENT '隐患描述',
  `reporter_id` BIGINT NULL COMMENT '报告人ID(关联staff_master.staff_id)',
  `handler_id` BIGINT NULL COMMENT '处理人ID(关联staff_master.staff_id)',
  `severity` TINYINT NOT NULL DEFAULT 2 COMMENT '严重程度(1低/2中/3高/4紧急)',
  `status` VARCHAR(16) NOT NULL DEFAULT '待处理' COMMENT '状态(待处理/处理中/已解决/已关闭)',
  `found_at` DATETIME NULL COMMENT '发现时间',
  `resolved_at` DATETIME NULL COMMENT '解决时间',
  `solution` TEXT NULL COMMENT '解决方案',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `uk_safety_issue_no` (`issue_no`),
  KEY `idx_safety_issue_store` (`store_id`),
  KEY `idx_safety_issue_reporter` (`reporter_id`),
  KEY `idx_safety_issue_handler` (`handler_id`),
  KEY `idx_safety_issue_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='安全隐患表';

-- 3.8 值班记录表
CREATE TABLE IF NOT EXISTS `duty_record` (
  `duty_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `duty_no` VARCHAR(32) NOT NULL COMMENT '值班编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `staff_id` BIGINT NOT NULL COMMENT '值班人ID(关联staff_master.staff_id)',
  `duty_date` DATE NOT NULL COMMENT '值班日期',
  `shift_type` ENUM('早班','中班','晚班','夜班','全天') NOT NULL COMMENT '班次类型',
  `check_in_time` DATETIME NULL COMMENT '签到时间',
  `check_out_time` DATETIME NULL COMMENT '签退时间',
  `status` VARCHAR(16) NOT NULL DEFAULT '待签到' COMMENT '状态(待签到/已签到/已签退/缺勤)',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`duty_id`),
  UNIQUE KEY `uk_duty_no` (`duty_no`),
  KEY `idx_duty_record_store` (`store_id`),
  KEY `idx_duty_record_staff` (`staff_id`),
  KEY `idx_duty_record_date` (`duty_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='值班记录表';

-- 3.9 通用附件表
CREATE TABLE IF NOT EXISTS `attachment` (
  `attachment_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `biz_type` VARCHAR(64) NOT NULL COMMENT '业务类型(如maintenance_request/inspection/safety_issue等)',
  `biz_id` BIGINT NOT NULL COMMENT '业务ID',
  `file_name` VARCHAR(255) NOT NULL COMMENT '文件名',
  `file_url` VARCHAR(512) NOT NULL COMMENT '文件URL',
  `file_type` VARCHAR(32) NULL COMMENT '文件MIME类型',
  `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  `upload_by` BIGINT NULL COMMENT '上传人ID(关联staff_master.staff_id)',
  `upload_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  PRIMARY KEY (`attachment_id`),
  KEY `idx_attachment_biz` (`biz_type`, `biz_id`),
  KEY `idx_attachment_upload_by` (`upload_by`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='通用附件表';


-- ============== 工具资产管理模块 (6张) ==============

-- 3.10 工具分类表
CREATE TABLE IF NOT EXISTS `tool_category` (
  `category_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `category_name` VARCHAR(64) NOT NULL COMMENT '分类名称(刀具/锅具/工具箱/服务用具)',
  `parent_id` BIGINT NULL COMMENT '父分类ID(自引用)',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_tool_category_name` (`category_name`),
  KEY `idx_tool_category_parent` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具分类表';

-- 3.11 工具台账表
CREATE TABLE IF NOT EXISTS `tool_master` (
  `tool_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tool_no` VARCHAR(32) NOT NULL COMMENT '工具编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `tool_name` VARCHAR(128) NOT NULL COMMENT '工具名称',
  `category_id` BIGINT NULL COMMENT '分类ID(关联tool_category.category_id)',
  `spec` VARCHAR(128) NULL COMMENT '规格型号',
  `brand` VARCHAR(64) NULL COMMENT '品牌',
  `unit` VARCHAR(16) NOT NULL DEFAULT '个' COMMENT '单位',
  `unit_price` DECIMAL(10,2) NULL COMMENT '单价',
  `total_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '总数量',
  `available_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '可用数量',
  `status` VARCHAR(16) NOT NULL DEFAULT '在用' COMMENT '状态(在用/闲置/报废/丢失)',
  `purchase_date` DATE NULL COMMENT '采购日期',
  `location` VARCHAR(64) NULL COMMENT '存放位置',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_id`),
  UNIQUE KEY `uk_tool_no` (`tool_no`, `store_id`),
  KEY `idx_tool_master_store` (`store_id`),
  KEY `idx_tool_master_category` (`category_id`),
  KEY `idx_tool_master_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具台账表';

-- 3.12 工具领用表
CREATE TABLE IF NOT EXISTS `tool_issue` (
  `issue_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `issue_no` VARCHAR(32) NOT NULL COMMENT '领用编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `staff_id` BIGINT NOT NULL COMMENT '领用人ID(关联staff_master.staff_id)',
  `tool_id` BIGINT NOT NULL COMMENT '工具ID(关联tool_master.tool_id)',
  `qty` DECIMAL(12,2) NOT NULL DEFAULT 1 COMMENT '领用数量',
  `issue_date` DATE NOT NULL COMMENT '领用日期',
  `expected_return_date` DATE NULL COMMENT '预计归还日期',
  `return_status` VARCHAR(16) NOT NULL DEFAULT '未归还' COMMENT '归还状态(未归还/部分归还/已归还)',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `uk_tool_issue_no` (`issue_no`),
  KEY `idx_tool_issue_store` (`store_id`),
  KEY `idx_tool_issue_staff` (`staff_id`),
  KEY `idx_tool_issue_tool` (`tool_id`),
  KEY `idx_tool_issue_status` (`return_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具领用表';

-- 3.13 工具归还表
CREATE TABLE IF NOT EXISTS `tool_return` (
  `return_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `return_no` VARCHAR(32) NOT NULL COMMENT '归还编号',
  `issue_id` BIGINT NOT NULL COMMENT '领用记录ID(关联tool_issue.issue_id)',
  `tool_id` BIGINT NOT NULL COMMENT '工具ID(关联tool_master.tool_id)',
  `staff_id` BIGINT NOT NULL COMMENT '归还人ID(关联staff_master.staff_id)',
  `qty` DECIMAL(12,2) NOT NULL DEFAULT 1 COMMENT '归还数量',
  `return_date` DATE NOT NULL COMMENT '归还日期',
  `condition` ENUM('完好','轻微损坏','严重损坏','丢失') NOT NULL DEFAULT '完好' COMMENT '归还状态',
  `damage_description` TEXT NULL COMMENT '损坏描述',
  `compensation_amount` DECIMAL(10,2) NULL COMMENT '赔偿金额',
  `receiver_id` BIGINT NULL COMMENT '接收人ID(关联staff_master.staff_id)',
  `remark` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`return_id`),
  UNIQUE KEY `uk_tool_return_no` (`return_no`),
  KEY `idx_tool_return_issue` (`issue_id`),
  KEY `idx_tool_return_tool` (`tool_id`),
  KEY `idx_tool_return_staff` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具归还表';

-- 3.14 工具损坏表
CREATE TABLE IF NOT EXISTS `tool_damage` (
  `damage_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `damage_no` VARCHAR(32) NOT NULL COMMENT '损坏编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `tool_id` BIGINT NOT NULL COMMENT '工具ID(关联tool_master.tool_id)',
  `staff_id` BIGINT NULL COMMENT '责任人ID(关联staff_master.staff_id)',
  `damage_date` DATE NOT NULL COMMENT '损坏日期',
  `damage_type` ENUM('轻微','严重','报废','丢失') NOT NULL DEFAULT '轻微' COMMENT '损坏类型',
  `reason` TEXT NOT NULL COMMENT '损坏原因',
  `compensation_amount` DECIMAL(10,2) NULL COMMENT '赔偿金额',
  `status` VARCHAR(16) NOT NULL DEFAULT '待处理' COMMENT '状态(待处理/已处理/已赔付)',
  `handler_id` BIGINT NULL COMMENT '处理人ID(关联staff_master.staff_id)',
  `handled_at` DATETIME NULL COMMENT '处理时间',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`damage_id`),
  UNIQUE KEY `uk_tool_damage_no` (`damage_no`),
  KEY `idx_tool_damage_store` (`store_id`),
  KEY `idx_tool_damage_tool` (`tool_id`),
  KEY `idx_tool_damage_staff` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具损坏表';

-- 3.15 工具盘点表
CREATE TABLE IF NOT EXISTS `tool_inventory` (
  `inventory_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `inventory_no` VARCHAR(32) NOT NULL COMMENT '盘点编号',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID',
  `inventory_date` DATE NOT NULL COMMENT '盘点日期',
  `staff_id` BIGINT NOT NULL COMMENT '盘点人ID(关联staff_master.staff_id)',
  `category_id` BIGINT NULL COMMENT '盘点分类ID(关联tool_category.category_id, NULL=全盘)',
  `total_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '账面数量',
  `actual_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '实际数量',
  `diff_qty` DECIMAL(12,2) NOT NULL DEFAULT 0 COMMENT '差异数量(实际-账面)',
  `status` VARCHAR(16) NOT NULL DEFAULT '待审核' COMMENT '状态(待审核/已审核/已调整)',
  `remark` TEXT NULL COMMENT '备注',
  `creator` VARCHAR(64) NULL COMMENT '创建人',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inventory_id`),
  UNIQUE KEY `uk_tool_inventory_no` (`inventory_no`),
  KEY `idx_tool_inventory_store` (`store_id`),
  KEY `idx_tool_inventory_staff` (`staff_id`),
  KEY `idx_tool_inventory_date` (`inventory_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具盘点表';

SET FOREIGN_KEY_CHECKS=1;
-- ================================================================
-- 任务1+2+3 完成
-- ================================================================

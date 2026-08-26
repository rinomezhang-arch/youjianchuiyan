-- ======================================================================
-- 工具/器具管理模块 建表迁移脚本 v1
-- 生成时间: 2026-08-27
-- 说明: 根据后端实体类(ToolCategory/ToolMaster/ToolIssue/ToolReturn/ToolDamage/ToolInventory)
--       补建生产库中完全缺失的 tool_* 系列表。ToolManagementController 已经挂在这些实体上，
--       之前从未建过表，一直是可被外部请求触达但必炸的死路径。
-- 规则: 遵循 create_missing_tables_v1.sql 的命名/字符集/引擎约定
-- 幂等: CREATE TABLE IF NOT EXISTS
-- ======================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- 1. tool_category (工具分类，支持自引用父分类)
CREATE TABLE IF NOT EXISTS `tool_category` (
  `category_id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `category_name` varchar(64) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT NULL COMMENT '父分类ID',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`category_id`),
  UNIQUE KEY `uk_tool_category_name` (`category_name`),
  KEY `idx_tool_category_parent` (`parent_id`),
  CONSTRAINT `fk_tool_category_parent` FOREIGN KEY (`parent_id`) REFERENCES `tool_category` (`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具分类表';

-- 2. tool_master (工具主数据/在库汇总)
CREATE TABLE IF NOT EXISTS `tool_master` (
  `tool_id` bigint NOT NULL AUTO_INCREMENT COMMENT '工具ID',
  `tool_no` varchar(32) NOT NULL COMMENT '工具编号',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `tool_name` varchar(128) NOT NULL COMMENT '工具名称',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `spec` varchar(128) DEFAULT NULL COMMENT '规格型号',
  `brand` varchar(64) DEFAULT NULL COMMENT '品牌',
  `unit` varchar(16) NOT NULL DEFAULT '个' COMMENT '单位',
  `unit_price` decimal(10,2) DEFAULT NULL COMMENT '单价',
  `total_qty` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总数量',
  `available_qty` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '可用数量',
  `status` varchar(16) NOT NULL DEFAULT '在用' COMMENT '状态(在用/闲置/报废/维修中)',
  `purchase_date` date DEFAULT NULL COMMENT '采购日期',
  `location` varchar(64) DEFAULT NULL COMMENT '存放位置',
  `remark` text COMMENT '备注',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`tool_id`),
  UNIQUE KEY `uk_tool_no` (`tool_no`),
  KEY `idx_tool_store` (`store_id`),
  KEY `idx_tool_category` (`category_id`),
  CONSTRAINT `fk_tool_master_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_master_category` FOREIGN KEY (`category_id`) REFERENCES `tool_category` (`category_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具主数据表';

-- 3. tool_issue (领用记录)
CREATE TABLE IF NOT EXISTS `tool_issue` (
  `issue_id` bigint NOT NULL AUTO_INCREMENT COMMENT '领用ID',
  `issue_no` varchar(32) NOT NULL COMMENT '领用单号',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `staff_id` int DEFAULT NULL COMMENT '领用员工ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `qty` decimal(12,2) NOT NULL DEFAULT '1.00' COMMENT '领用数量',
  `issue_date` date NOT NULL COMMENT '领用日期',
  `expected_return_date` date DEFAULT NULL COMMENT '预计归还日期',
  `return_status` varchar(16) NOT NULL DEFAULT '未归还' COMMENT '归还状态(未归还/已归还/部分归还)',
  `remark` text COMMENT '备注',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`issue_id`),
  UNIQUE KEY `uk_tool_issue_no` (`issue_no`),
  KEY `idx_tool_issue_store` (`store_id`),
  KEY `idx_tool_issue_tool` (`tool_id`),
  KEY `idx_tool_issue_staff` (`staff_id`),
  CONSTRAINT `fk_tool_issue_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_issue_tool` FOREIGN KEY (`tool_id`) REFERENCES `tool_master` (`tool_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_issue_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具领用记录表';

-- 4. tool_return (归还记录)
CREATE TABLE IF NOT EXISTS `tool_return` (
  `return_id` bigint NOT NULL AUTO_INCREMENT COMMENT '归还ID',
  `return_no` varchar(32) NOT NULL COMMENT '归还单号',
  `issue_id` bigint NOT NULL COMMENT '对应领用ID',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `staff_id` int DEFAULT NULL COMMENT '归还员工ID',
  `qty` decimal(12,2) NOT NULL DEFAULT '1.00' COMMENT '归还数量',
  `return_date` date NOT NULL COMMENT '归还日期',
  `condition` varchar(16) NOT NULL DEFAULT '完好' COMMENT '归还状态(完好/轻微损坏/严重损坏/丢失)',
  `damage_description` text COMMENT '损坏描述',
  `compensation_amount` decimal(10,2) DEFAULT NULL COMMENT '赔偿金额',
  `receiver_id` int DEFAULT NULL COMMENT '验收人ID',
  `remark` text COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`return_id`),
  UNIQUE KEY `uk_tool_return_no` (`return_no`),
  KEY `idx_tool_return_issue` (`issue_id`),
  KEY `idx_tool_return_tool` (`tool_id`),
  KEY `idx_tool_return_staff` (`staff_id`),
  KEY `idx_tool_return_receiver` (`receiver_id`),
  CONSTRAINT `fk_tool_return_issue` FOREIGN KEY (`issue_id`) REFERENCES `tool_issue` (`issue_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_return_tool` FOREIGN KEY (`tool_id`) REFERENCES `tool_master` (`tool_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_return_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_return_receiver` FOREIGN KEY (`receiver_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具归还记录表';

-- 5. tool_damage (损坏/丢失报告)
CREATE TABLE IF NOT EXISTS `tool_damage` (
  `damage_id` bigint NOT NULL AUTO_INCREMENT COMMENT '损坏ID',
  `damage_no` varchar(32) NOT NULL COMMENT '损坏单号',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `tool_id` bigint NOT NULL COMMENT '工具ID',
  `staff_id` int DEFAULT NULL COMMENT '责任人ID',
  `damage_date` date NOT NULL COMMENT '发生日期',
  `damage_type` varchar(16) NOT NULL DEFAULT '轻微' COMMENT '损坏类型(轻微/严重/报废/丢失)',
  `reason` text NOT NULL COMMENT '损坏原因',
  `compensation_amount` decimal(10,2) DEFAULT NULL COMMENT '赔偿金额',
  `status` varchar(16) NOT NULL DEFAULT '待处理' COMMENT '处理状态(待处理/已处理)',
  `handler_id` int DEFAULT NULL COMMENT '处理人ID',
  `handled_at` timestamp NULL DEFAULT NULL COMMENT '处理时间',
  `remark` text COMMENT '备注',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`damage_id`),
  UNIQUE KEY `uk_tool_damage_no` (`damage_no`),
  KEY `idx_tool_damage_store` (`store_id`),
  KEY `idx_tool_damage_tool` (`tool_id`),
  KEY `idx_tool_damage_staff` (`staff_id`),
  KEY `idx_tool_damage_handler` (`handler_id`),
  CONSTRAINT `fk_tool_damage_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_damage_tool` FOREIGN KEY (`tool_id`) REFERENCES `tool_master` (`tool_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_damage_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_damage_handler` FOREIGN KEY (`handler_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具损坏丢失记录表';

-- 6. tool_inventory (工具盘点)
CREATE TABLE IF NOT EXISTS `tool_inventory` (
  `inventory_id` bigint NOT NULL AUTO_INCREMENT COMMENT '盘点ID',
  `inventory_no` varchar(32) NOT NULL COMMENT '盘点单号',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `inventory_date` date NOT NULL COMMENT '盘点日期',
  `staff_id` int DEFAULT NULL COMMENT '盘点人ID',
  `category_id` bigint DEFAULT NULL COMMENT '盘点分类ID(为空表示全盘)',
  `total_qty` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '账面数量',
  `actual_qty` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '实盘数量',
  `diff_qty` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '差异数量',
  `status` varchar(16) NOT NULL DEFAULT '待审核' COMMENT '状态(待审核/已审核)',
  `remark` text COMMENT '备注',
  `creator` varchar(64) DEFAULT NULL COMMENT '创建人',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`inventory_id`),
  UNIQUE KEY `uk_tool_inventory_no` (`inventory_no`),
  KEY `idx_tool_inventory_store` (`store_id`),
  KEY `idx_tool_inventory_category` (`category_id`),
  KEY `idx_tool_inventory_staff` (`staff_id`),
  CONSTRAINT `fk_tool_inventory_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_tool_inventory_category` FOREIGN KEY (`category_id`) REFERENCES `tool_category` (`category_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_tool_inventory_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工具盘点表';

SET FOREIGN_KEY_CHECKS = 1;

-- ======================================================================
-- 迁移完成：新建 6 张表，13 个外键
-- ======================================================================

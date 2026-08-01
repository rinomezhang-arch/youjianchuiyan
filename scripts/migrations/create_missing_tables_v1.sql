-- ======================================================================
-- 补建缺失表迁移脚本 v1
-- 生成时间: 2026-08-01
-- 说明: 根据后端实体类(@Entity)补齐数据库中完全缺失的11张表
-- 规则: 遵循现有数据库命名系统,带外键/级联/字段注释/store_id隔离
-- 排序规则: utf8mb4_0900_ai_ci (与系统统一)
-- 引擎: InnoDB
-- 幂等: 使用 CREATE TABLE IF NOT EXISTS
-- ======================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- --------------------------------------------------------------------
-- 1. post (岗位表)
--    实体类: Post.java
--    外键: dept_id → department.dept_id (ON DELETE CASCADE)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `post` (
  `post_id` int NOT NULL AUTO_INCREMENT COMMENT '岗位ID',
  `dept_id` int DEFAULT NULL COMMENT '所属部门ID',
  `post_name` varchar(50) NOT NULL COMMENT '岗位名称',
  `post_code` varchar(50) DEFAULT NULL COMMENT '岗位编码',
  `headcount` int DEFAULT NULL COMMENT '编制人数',
  `on_duty_count` int DEFAULT NULL COMMENT '在岗人数',
  `sort_order` int DEFAULT NULL COMMENT '排序号',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`post_id`),
  KEY `idx_dept_post` (`dept_id`),
  CONSTRAINT `fk_post_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='岗位信息表';

-- --------------------------------------------------------------------
-- 2. contract (劳动合同表)
--    实体类: Contract.java
--    外键: staff_id → staff_master.staff_id (RESTRICT)
--          store_id → store_info.store_id (RESTRICT)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `contract` (
  `contract_id` bigint NOT NULL AUTO_INCREMENT COMMENT '合同ID',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `staff_id` int NOT NULL COMMENT '员工ID',
  `contract_no` varchar(50) NOT NULL COMMENT '合同编号',
  `contract_type` int DEFAULT '1' COMMENT '合同类型(1=固定期限 2=无固定期限 3=实习)',
  `sign_date` date NOT NULL COMMENT '签订日期',
  `start_date` date NOT NULL COMMENT '生效日期',
  `end_date` date DEFAULT NULL COMMENT '到期日期',
  `file_path` varchar(500) DEFAULT NULL COMMENT '合同文件路径',
  `status` int DEFAULT '1' COMMENT '状态(1=有效 2=到期 3=终止)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`contract_id`),
  UNIQUE KEY `uk_contract_no` (`contract_no`),
  KEY `idx_store_contract` (`store_id`),
  KEY `idx_staff_contract` (`staff_id`),
  CONSTRAINT `fk_contract_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_contract_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='劳动合同表';

-- --------------------------------------------------------------------
-- 3. salary_template (薪资标准模板)
--    实体类: SalaryTemplate.java
--    外键: store_id → store_info.store_id (RESTRICT)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `salary_template` (
  `template_id` bigint NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `template_name` varchar(100) NOT NULL COMMENT '模板名称',
  `post_name` varchar(50) NOT NULL COMMENT '适用岗位名称',
  `base_salary` decimal(10,2) NOT NULL COMMENT '基本工资',
  `overtime_rate` decimal(3,1) DEFAULT '1.5' COMMENT '加班倍率',
  `meal_subsidy` decimal(10,2) DEFAULT '0.00' COMMENT '餐补',
  `transport_subsidy` decimal(10,2) DEFAULT '0.00' COMMENT '交通补贴',
  `housing_subsidy` decimal(10,2) DEFAULT '0.00' COMMENT '住房补贴',
  `attendance_bonus` decimal(10,2) DEFAULT '0.00' COMMENT '全勤奖',
  `social_security_employee` decimal(10,2) DEFAULT '0.00' COMMENT '社保个人部分',
  `housing_fund_employee` decimal(10,2) DEFAULT '0.00' COMMENT '公积金个人部分',
  `performance_ratio` decimal(5,2) DEFAULT '30.00' COMMENT '绩效比例(%)',
  `is_active` int DEFAULT '1' COMMENT '是否启用(1=是 0=否)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`template_id`),
  KEY `idx_store_salary_tpl` (`store_id`),
  KEY `idx_post_name` (`post_name`),
  CONSTRAINT `fk_salary_tpl_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='薪资标准模板表';

-- --------------------------------------------------------------------
-- 4. month_salary (月度薪资主表)
--    实体类: MonthSalary.java
--    外键: staff_id → staff_master.staff_id (RESTRICT)
--          store_id → store_info.store_id (RESTRICT)
--    唯一约束: (staff_id, salary_month)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `month_salary` (
  `salary_id` bigint NOT NULL AUTO_INCREMENT COMMENT '薪资记录ID',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `staff_id` int NOT NULL COMMENT '员工ID',
  `salary_month` varchar(7) NOT NULL COMMENT '薪资月份(YYYY-MM)',
  `base_salary` decimal(10,2) NOT NULL COMMENT '基本工资',
  `overtime_pay` decimal(10,2) DEFAULT '0.00' COMMENT '加班费',
  `performance_salary` decimal(10,2) DEFAULT '0.00' COMMENT '绩效工资',
  `reward_amount` decimal(10,2) DEFAULT '0.00' COMMENT '奖金',
  `punish_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '罚款扣除',
  `leave_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '请假扣除',
  `social_security_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '社保扣除',
  `housing_fund_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '公积金扣除',
  `other_allowance` decimal(10,2) DEFAULT '0.00' COMMENT '其他补贴',
  `other_deduction` decimal(10,2) DEFAULT '0.00' COMMENT '其他扣除',
  `gross_salary` decimal(10,2) NOT NULL COMMENT '应发工资',
  `net_salary` decimal(10,2) NOT NULL COMMENT '实发工资',
  `tax_amount` decimal(10,2) DEFAULT '0.00' COMMENT '个税金额',
  `status` int DEFAULT '0' COMMENT '状态(0=草稿 1=已审核 2=已发放)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`salary_id`),
  UNIQUE KEY `uk_staff_month` (`staff_id`, `salary_month`),
  KEY `idx_store_salary` (`store_id`),
  KEY `idx_staff_salary` (`staff_id`),
  CONSTRAINT `fk_month_salary_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_month_salary_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度薪资主表';

-- --------------------------------------------------------------------
-- 5. reward_punish (奖惩记录表)
--    实体类: RewardPunish.java
--    外键: staff_id → staff_master.staff_id (RESTRICT)
--          store_id → store_info.store_id (RESTRICT)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `reward_punish` (
  `rp_id` bigint NOT NULL AUTO_INCREMENT COMMENT '奖惩ID',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `staff_id` int NOT NULL COMMENT '员工ID',
  `rp_no` varchar(30) NOT NULL COMMENT '奖惩编号',
  `rp_type` int NOT NULL COMMENT '类型(1=奖励 2=惩罚)',
  `rp_category` varchar(50) NOT NULL COMMENT '奖惩类别',
  `amount` decimal(10,2) NOT NULL COMMENT '金额',
  `reason` text NOT NULL COMMENT '奖惩原因',
  `evidence_path` varchar(500) DEFAULT NULL COMMENT '证据文件路径',
  `approver_1_id` int DEFAULT NULL COMMENT '审批人1ID',
  `approver_1_status` int DEFAULT '1' COMMENT '审批人1状态(1=待审 2=通过 3=驳回)',
  `approver_1_time` timestamp NULL DEFAULT NULL COMMENT '审批人1时间',
  `approver_1_remark` varchar(255) DEFAULT NULL COMMENT '审批人1备注',
  `approver_2_id` int DEFAULT NULL COMMENT '审批人2ID',
  `approver_2_status` int DEFAULT '1' COMMENT '审批人2状态(1=待审 2=通过 3=驳回)',
  `approver_2_time` timestamp NULL DEFAULT NULL COMMENT '审批人2时间',
  `approver_2_remark` varchar(255) DEFAULT NULL COMMENT '审批人2备注',
  `final_status` int DEFAULT '1' COMMENT '最终状态(1=待审 2=通过 3=驳回)',
  `is_synced_to_salary` int DEFAULT '0' COMMENT '是否已同步到薪资(1=是 0=否)',
  `sync_salary_id` bigint DEFAULT NULL COMMENT '同步的薪资记录ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`rp_id`),
  UNIQUE KEY `uk_rp_no` (`rp_no`),
  KEY `idx_store_rp` (`store_id`),
  KEY `idx_staff_rp` (`staff_id`),
  CONSTRAINT `fk_rp_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_rp_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='奖惩记录表';

-- --------------------------------------------------------------------
-- 6. schedule_month (月度排班主表)
--    实体类: ScheduleMonth.java
--    外键: store_id → store_info.store_id (RESTRICT)
--          dept_id → department.dept_id (RESTRICT)
--    唯一约束: (schedule_month, dept_id)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `schedule_month` (
  `schedule_id` bigint NOT NULL AUTO_INCREMENT COMMENT '排班ID',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `dept_id` int NOT NULL COMMENT '部门ID',
  `schedule_month` varchar(7) NOT NULL COMMENT '排班月份(YYYY-MM)',
  `status` int DEFAULT '0' COMMENT '状态(0=草稿 1=已发布)',
  `published_by` int DEFAULT NULL COMMENT '发布人ID',
  `published_time` timestamp NULL DEFAULT NULL COMMENT '发布时间',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`schedule_id`),
  UNIQUE KEY `uk_month_dept` (`schedule_month`, `dept_id`),
  KEY `idx_store_sched_month` (`store_id`),
  KEY `idx_dept_sched_month` (`dept_id`),
  CONSTRAINT `fk_sched_month_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_sched_month_dept` FOREIGN KEY (`dept_id`) REFERENCES `department` (`dept_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='月度排班主表';

-- --------------------------------------------------------------------
-- 7. schedule_day (每日排班明细)
--    实体类: ScheduleDay.java
--    外键: schedule_id → schedule_month.schedule_id (CASCADE)
--          staff_id → staff_master.staff_id (RESTRICT)
--    唯一约束: (staff_id, work_date)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `schedule_day` (
  `day_id` bigint NOT NULL AUTO_INCREMENT COMMENT '日排班ID',
  `schedule_id` bigint NOT NULL COMMENT '月度排班ID',
  `staff_id` int NOT NULL COMMENT '员工ID',
  `work_date` date NOT NULL COMMENT '工作日期',
  `shift_type` int NOT NULL COMMENT '班次类型(1=早班 2=中班 3=晚班 4=全天)',
  `shift_start` time DEFAULT NULL COMMENT '上班时间',
  `shift_end` time DEFAULT NULL COMMENT '下班时间',
  `break_minutes` int DEFAULT '60' COMMENT '休息时长(分钟)',
  `is_holiday` int DEFAULT '0' COMMENT '是否节假日(1=是 0=否)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`day_id`),
  UNIQUE KEY `uk_staff_date` (`staff_id`, `work_date`),
  KEY `idx_sched_id` (`schedule_id`),
  KEY `idx_staff_day` (`staff_id`),
  CONSTRAINT `fk_sched_day_month` FOREIGN KEY (`schedule_id`) REFERENCES `schedule_month` (`schedule_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_sched_day_staff` FOREIGN KEY (`staff_id`) REFERENCES `staff_master` (`staff_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='每日排班明细表';

-- --------------------------------------------------------------------
-- 8. stock_transfer_detail (库存调拨明细)
--    实体类: StockTransferDetail.java
--    外键: transfer_id → stock_transfer.transfer_id (CASCADE)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `stock_transfer_detail` (
  `detail_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `transfer_id` bigint NOT NULL COMMENT '调拨单ID',
  `ingredient_id` varchar(50) DEFAULT NULL COMMENT '食材ID',
  `quantity` decimal(12,2) DEFAULT NULL COMMENT '调拨数量',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  PRIMARY KEY (`detail_id`),
  KEY `idx_transfer_detail` (`transfer_id`),
  CONSTRAINT `fk_transfer_detail` FOREIGN KEY (`transfer_id`) REFERENCES `stock_transfer` (`transfer_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存调拨明细表';

-- --------------------------------------------------------------------
-- 9. inventory_summary (库存汇总表)
--    实体类: InventorySummary.java
--    外键: store_id → store_info.store_id (RESTRICT)
--          ingredient_id → ingredient_master.ingredient_id (CASCADE)
--    唯一约束: (store_id, ingredient_id)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `inventory_summary` (
  `summary_id` bigint NOT NULL AUTO_INCREMENT COMMENT '汇总ID',
  `store_id` bigint NOT NULL COMMENT '门店ID(多租户隔离)',
  `ingredient_id` varchar(50) NOT NULL COMMENT '食材ID',
  `total_quantity` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总库存数量',
  `total_cost` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '总成本金额',
  `avg_unit_price` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '平均单价',
  `last_in_time` timestamp NULL DEFAULT NULL COMMENT '最后入库时间',
  `last_out_time` timestamp NULL DEFAULT NULL COMMENT '最后出库时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`summary_id`),
  UNIQUE KEY `uk_store_ingredient` (`store_id`, `ingredient_id`),
  KEY `idx_store_inv_summary` (`store_id`),
  KEY `idx_ingredient_inv_summary` (`ingredient_id`),
  CONSTRAINT `fk_inv_summary_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_inv_summary_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient_master` (`ingredient_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存汇总表';

-- --------------------------------------------------------------------
-- 10. preprocessing_record (预处理记录表)
--     实体类: PreprocessingRecord.java
--     外键: store_id → store_info.store_id (RESTRICT)
--           ingredient_id → ingredient_master.ingredient_id (SET NULL)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `preprocessing_record` (
  `record_id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `store_id` bigint DEFAULT NULL COMMENT '门店ID(多租户隔离)',
  `ingredient_id` varchar(50) DEFAULT NULL COMMENT '食材ID',
  `ingredient_name` varchar(100) DEFAULT NULL COMMENT '食材名称',
  `raw_qty` decimal(10,3) DEFAULT NULL COMMENT '原料数量',
  `processed_qty` decimal(10,3) DEFAULT NULL COMMENT '加工后数量',
  `yield_rate` decimal(5,2) DEFAULT NULL COMMENT '出成率(%)',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `preprocessing_type` varchar(50) DEFAULT NULL COMMENT '预处理类型(清洗/切配/腌制等)',
  `record_date` date DEFAULT NULL COMMENT '记录日期',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `notes` text COMMENT '备注说明',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`record_id`),
  KEY `idx_store_preprocess` (`store_id`),
  KEY `idx_ingredient_preprocess` (`ingredient_id`),
  KEY `idx_date_preprocess` (`record_date`),
  CONSTRAINT `fk_preprocess_store` FOREIGN KEY (`store_id`) REFERENCES `store_info` (`store_id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_preprocess_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient_master` (`ingredient_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预处理记录表';

-- --------------------------------------------------------------------
-- 11. procurement_request_item (采购申请明细表)
--     实体类: PurchaseRequestItem.java (需修改@Table)
--     外键: request_id → procurement_request.request_id (CASCADE)
--           ingredient_id → ingredient_master.ingredient_id (SET NULL)
-- --------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `procurement_request_item` (
  `item_id` bigint NOT NULL AUTO_INCREMENT COMMENT '明细ID',
  `request_id` bigint NOT NULL COMMENT '采购申请ID',
  `ingredient_id` varchar(50) DEFAULT NULL COMMENT '食材ID',
  `ingredient_name` varchar(100) DEFAULT NULL COMMENT '食材名称',
  `category` varchar(50) DEFAULT NULL COMMENT '分类',
  `quantity` decimal(10,3) DEFAULT NULL COMMENT '申请数量',
  `unit` varchar(20) DEFAULT NULL COMMENT '单位',
  `estimated_price` decimal(10,2) DEFAULT NULL COMMENT '预估单价',
  `notes` text COMMENT '备注',
  PRIMARY KEY (`item_id`),
  KEY `idx_request_item` (`request_id`),
  KEY `idx_ingredient_req_item` (`ingredient_id`),
  CONSTRAINT `fk_req_item_request` FOREIGN KEY (`request_id`) REFERENCES `procurement_request` (`request_id`) ON DELETE CASCADE,
  CONSTRAINT `fk_req_item_ingredient` FOREIGN KEY (`ingredient_id`) REFERENCES `ingredient_master` (`ingredient_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='采购申请明细表';

SET FOREIGN_KEY_CHECKS = 1;

-- ======================================================================
-- 迁移完成
-- 新建表数: 11
-- 外键数: 22
-- 级联策略:
--   CASCADE:  schedule_day→schedule_month, stock_transfer_detail→stock_transfer,
--             procurement_request_item→procurement_request, post→department,
--             inventory_summary→ingredient_master
--   RESTRICT: contract→staff_master/store_info, month_salary→staff_master/store_info,
--             reward_punish→staff_master/store_info, schedule_month→store_info/department,
--             schedule_day→staff_master, inventory_summary→store_info,
--             salary_template→store_info, preprocessing_record→store_info
--   SET NULL: preprocessing_record→ingredient_master, procurement_request_item→ingredient_master
-- ======================================================================

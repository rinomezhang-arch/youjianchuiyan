-- ======================================================================
-- 又见炊烟餐饮管理系统 - 请假模块数据库迁移脚本
-- 对应参考系统: HR系统 att_leave + att_staff_leave
-- 执行方式: mysql -u <user> -p banquet < hr_leave.sql
-- 特性: 幂等（可重复执行，已存在的表自动跳过）
-- ======================================================================

-- ---------- 1. hr_leave_type (请假类型配置表，对应 att_leave) ----------
CREATE TABLE IF NOT EXISTS `hr_leave_type` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT NOT NULL COMMENT '门店ID',
  `dept_id` INT NULL DEFAULT NULL COMMENT '部门id',
  `days` INT UNSIGNED NULL DEFAULT NULL COMMENT '休假天数',
  `type_num` TINYINT UNSIGNED NULL DEFAULT NULL COMMENT '休假类型: 0-事假 1-产假 2-病假 3-婚假 4-探亲假 5-陪产假',
  `status` TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '0禁用，1正常，默认1',
  `remark` VARCHAR(200) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`),
  INDEX `idx_dept_id` (`dept_id`),
  INDEX `idx_type_num` (`type_num`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='请假类型配置表';

-- ---------- 2. hr_staff_leave (员工请假表，对应 att_staff_leave) ----------
CREATE TABLE IF NOT EXISTS `hr_staff_leave` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
  `store_id` BIGINT NOT NULL COMMENT '门店ID',
  `staff_id` INT NULL DEFAULT NULL COMMENT '员工id',
  `days` INT NULL DEFAULT NULL COMMENT '请假的天数',
  `type_num` INT NULL DEFAULT NULL COMMENT '请假类型id: 0-事假 1-产假 2-病假 3-婚假 4-探亲假 5-陪产假',
  `start_date` DATE NULL DEFAULT NULL COMMENT '请假的开始日期',
  `status` TINYINT UNSIGNED NULL DEFAULT 0 COMMENT '0未审核，1审核通过，2驳回，3撤销',
  `remark` VARCHAR(200) COMMENT '备注',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED DEFAULT 0 COMMENT '逻辑删除，0未删除，1删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`),
  INDEX `idx_staff_id` (`staff_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_start_date` (`start_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工请假表';
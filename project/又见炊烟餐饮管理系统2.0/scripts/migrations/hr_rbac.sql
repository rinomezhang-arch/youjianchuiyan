-- ============================================================
-- RBAC 权限管理模块迁移脚本
-- 来源：人力资源管理系统 per_menu / per_role / per_role_menu / per_staff_role
-- 目标：又见炊烟餐饮管理系统（多租户 store_id）
-- 日期：2026-08-11
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for hr_menu (菜单表)
-- 原表: per_menu
-- ----------------------------
DROP TABLE IF EXISTS `hr_menu`;
CREATE TABLE `hr_menu` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID，多租户隔离',
  `code` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单编码',
  `name` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单名称',
  `icon` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单图标',
  `path` VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '菜单路径',
  `parent_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父菜单ID，0代表根菜单',
  `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`) USING BTREE,
  INDEX `idx_parent_id` (`parent_id`) USING BTREE,
  INDEX `idx_code` (`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR菜单表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for hr_role (角色表)
-- 原表: per_role
-- ----------------------------
DROP TABLE IF EXISTS `hr_role`;
CREATE TABLE `hr_role` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID，多租户隔离',
  `code` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色编码',
  `name` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色名称',
  `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '角色备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`) USING BTREE,
  INDEX `idx_code` (`code`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR角色表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for hr_role_menu (角色菜单关系表)
-- 原表: per_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `hr_role_menu`;
CREATE TABLE `hr_role_menu` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID，多租户隔离',
  `role_id` INT UNSIGNED NOT NULL COMMENT '角色ID',
  `menu_id` INT UNSIGNED NOT NULL COMMENT '菜单ID',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用，1正常，默认1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`) USING BTREE,
  INDEX `idx_role_id` (`role_id`) USING BTREE,
  INDEX `idx_menu_id` (`menu_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR角色菜单关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for hr_staff_role (员工角色关系表)
-- 原表: per_staff_role
-- ----------------------------
DROP TABLE IF EXISTS `hr_staff_role`;
CREATE TABLE `hr_staff_role` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` BIGINT NOT NULL DEFAULT 1 COMMENT '门店ID，多租户隔离',
  `staff_id` INT UNSIGNED NULL DEFAULT NULL COMMENT '员工ID',
  `role_id` INT UNSIGNED NULL DEFAULT NULL COMMENT '角色ID',
  `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '0禁用，1正常，默认1',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_store_id` (`store_id`) USING BTREE,
  INDEX `idx_staff_id` (`staff_id`) USING BTREE,
  INDEX `idx_role_id` (`role_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR员工角色关系表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 初始化默认菜单数据
-- ----------------------------
INSERT INTO `hr_menu` (`id`, `store_id`, `code`, `name`, `icon`, `path`, `parent_id`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES
(1, 1, 'hr_dashboard', 'HR仪表盘', 'dashboard', '/hr/dashboard', 0, NULL, NOW(), NULL, 0),
(2, 1, 'hr_staff', '员工管理', 'user', '/hr/staff', 0, NULL, NOW(), NULL, 0),
(3, 1, 'hr_dept', '部门管理', 's-operation', '/hr/department', 0, NULL, NOW(), NULL, 0),
(4, 1, 'hr_attendance', '考勤管理', 'edit', '/hr/attendance', 0, NULL, NOW(), NULL, 0),
(5, 1, 'hr_leave', '请假审批', 'suitcase', '/hr/leave', 4, NULL, NOW(), NULL, 0),
(6, 1, 'hr_overtime', '加班管理', 'time', '/hr/overtime', 4, NULL, NOW(), NULL, 0),
(7, 1, 'hr_salary', '薪资管理', 'data-line', '/hr/salary', 0, NULL, NOW(), NULL, 0),
(8, 1, 'hr_system', '系统管理', 's-management', '/hr/system', 0, NULL, NOW(), NULL, 0),
(9, 1, 'hr_permission', '权限管理', 's-cooperation', '/hr/permission', 0, NULL, NOW(), NULL, 0),
(10, 1, 'hr_role', '角色管理', 's-custom', '/hr/role', 9, NULL, NOW(), NULL, 0),
(11, 1, 'hr_menu', '菜单管理', 'collection', '/hr/menu', 9, NULL, NOW(), NULL, 0);

-- ----------------------------
-- 初始化默认角色数据
-- ----------------------------
INSERT INTO `hr_role` (`id`, `store_id`, `code`, `name`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES
(1, 1, 'admin', '管理员', '超级管理员，拥有所有权限', NOW(), NULL, 0),
(2, 1, 'manager', '店长', '门店管理者', NOW(), NULL, 0),
(3, 1, 'hr', '人事经理', '人力资源管理', NOW(), NULL, 0);

-- ----------------------------
-- 初始化角色菜单关系（管理员拥有所有菜单）
-- ----------------------------
INSERT INTO `hr_role_menu` (`id`, `store_id`, `role_id`, `menu_id`, `status`, `create_time`, `update_time`, `is_deleted`) VALUES
(1, 1, 1, 1, 1, NOW(), NULL, 0),
(2, 1, 1, 2, 1, NOW(), NULL, 0),
(3, 1, 1, 3, 1, NOW(), NULL, 0),
(4, 1, 1, 4, 1, NOW(), NULL, 0),
(5, 1, 1, 5, 1, NOW(), NULL, 0),
(6, 1, 1, 6, 1, NOW(), NULL, 0),
(7, 1, 1, 7, 1, NOW(), NULL, 0),
(8, 1, 1, 8, 1, NOW(), NULL, 0),
(9, 1, 1, 9, 1, NOW(), NULL, 0),
(10, 1, 1, 10, 1, NOW(), NULL, 0),
(11, 1, 1, 11, 1, NOW(), NULL, 0);

SET FOREIGN_KEY_CHECKS = 1;
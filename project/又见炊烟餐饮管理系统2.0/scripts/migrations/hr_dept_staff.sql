-- ======================================================================
-- 又见炊烟餐饮管理系统 - HR部门/员工/文件模块迁移脚本
-- 来源: 人力资源管理系统 sys_dept / sys_staff / sys_docs 表
-- 增加 store_id 多租户支持
-- 执行方式: mysql -u <user> -p banquet < hr_dept_staff.sql
-- 特性: 幂等（可重复执行，表存在则跳过）
-- ======================================================================

-- ---------- 1. 部门表 hr_dept ----------
DROP PROCEDURE IF EXISTS _create_hr_dept;
DELIMITER $$
CREATE PROCEDURE _create_hr_dept()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'hr_dept'
    ) THEN
        CREATE TABLE `hr_dept` (
            `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '部门id',
            `store_id` BIGINT NOT NULL COMMENT '门店ID（多租户）',
            `code` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门编码',
            `name` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门名称',
            `mor_start_time` TIME NULL DEFAULT NULL COMMENT '上午上班时间',
            `mor_end_time` TIME NULL DEFAULT NULL COMMENT '上午下班时间',
            `aft_start_time` TIME NULL DEFAULT NULL COMMENT '下午上班时间',
            `aft_end_time` TIME NULL DEFAULT NULL COMMENT '下午下班时间',
            `total_work_time` DECIMAL(3, 1) NULL DEFAULT NULL COMMENT '员工工作总时长',
            `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '部门备注',
            `parent_id` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '父级部门id，0根部门',
            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1删除',
            PRIMARY KEY (`id`) USING BTREE,
            INDEX `idx_store_id` (`store_id`),
            INDEX `idx_parent_id` (`parent_id`),
            INDEX `idx_store_parent` (`store_id`, `parent_id`)
        ) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR部门表' ROW_FORMAT = DYNAMIC;
    END IF;
END$$
DELIMITER ;

CALL _create_hr_dept();
DROP PROCEDURE IF EXISTS _create_hr_dept;

-- ---------- 2. 员工表 hr_staff ----------
DROP PROCEDURE IF EXISTS _create_hr_staff;
DELIMITER $$
CREATE PROCEDURE _create_hr_staff()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'hr_staff'
    ) THEN
        CREATE TABLE `hr_staff` (
            `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '员工id',
            `store_id` BIGINT NOT NULL COMMENT '门店ID（多租户）',
            `code` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '' COMMENT '员工编码',
            `name` VARCHAR(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT '' COMMENT '员工姓名',
            `gender` TINYINT UNSIGNED NULL DEFAULT 0 COMMENT '性别，0男，1女，默认0',
            `pwd` CHAR(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工密码',
            `avatar` VARCHAR(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工头像',
            `birthday` DATE NULL DEFAULT NULL COMMENT '员工生日',
            `phone` CHAR(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工电话',
            `address` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '地址',
            `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '员工备注',
            `dept_id` INT UNSIGNED NULL DEFAULT NULL COMMENT '部门id',
            `status` TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '员工状态，0异常，1正常',
            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
            `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除，0未删除，1已删除',
            PRIMARY KEY (`id`) USING BTREE,
            INDEX `idx_store_id` (`store_id`),
            INDEX `idx_dept_id` (`dept_id`),
            INDEX `idx_store_dept` (`store_id`, `dept_id`),
            INDEX `idx_name` (`name`)
        ) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR员工表' ROW_FORMAT = DYNAMIC;
    END IF;
END$$
DELIMITER ;

CALL _create_hr_staff();
DROP PROCEDURE IF EXISTS _create_hr_staff;

-- ---------- 3. 文件表 hr_docs ----------
DROP PROCEDURE IF EXISTS _create_hr_docs;
DELIMITER $$
CREATE PROCEDURE _create_hr_docs()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'hr_docs'
    ) THEN
        CREATE TABLE `hr_docs` (
            `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
            `store_id` BIGINT NOT NULL COMMENT '门店ID（多租户）',
            `name` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件名称',
            `type` VARCHAR(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件类型',
            `old_name` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件的原名称',
            `md5` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件md5信息',
            `size` BIGINT UNSIGNED NULL DEFAULT NULL COMMENT '文件大小KB',
            `staff_id` INT NULL DEFAULT NULL COMMENT '文件上传者id',
            `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '文件备注',
            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
            `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
            `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0未删除，1已删除，默认为0',
            PRIMARY KEY (`id`) USING BTREE,
            INDEX `idx_store_id` (`store_id`),
            INDEX `idx_staff_id` (`staff_id`),
            INDEX `idx_md5` (`md5`)
        ) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR文件表' ROW_FORMAT = DYNAMIC;
    END IF;
END$$
DELIMITER ;

CALL _create_hr_docs();
DROP PROCEDURE IF EXISTS _create_hr_docs;

-- ======================================================================
-- 迁移完成。验证：
-- SHOW CREATE TABLE hr_dept;
-- SHOW CREATE TABLE hr_staff;
-- SHOW CREATE TABLE hr_docs;
-- ======================================================================
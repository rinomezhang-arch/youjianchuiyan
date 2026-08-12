-- ======================================================================
-- 又见炊烟餐饮管理系统 - 考勤模块迁移脚本
-- 来源: 人力资源管理系统 att_attendance 表
-- 增加 store_id 多租户支持
-- 执行方式: mysql -u <user> -p banquet < hr_attendance.sql
-- 特性: 幂等（可重复执行，表存在则跳过）
-- ======================================================================

-- ---------- 幂等辅助：表存在则跳过 ----------
DROP PROCEDURE IF EXISTS _create_att_attendance;
DELIMITER $$
CREATE PROCEDURE _create_att_attendance()
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.tables
        WHERE table_schema = DATABASE()
          AND table_name = 'att_attendance'
    ) THEN
        CREATE TABLE `att_attendance` (
            `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
            `store_id` BIGINT NOT NULL COMMENT '门店ID（多租户）',
            `staff_id` INT NULL DEFAULT NULL COMMENT '员工id',
            `mor_start_time` TIME NULL DEFAULT NULL COMMENT '上午上班时间',
            `mor_end_time` TIME NULL DEFAULT NULL COMMENT '上午下班时间',
            `aft_start_time` TIME NULL DEFAULT NULL COMMENT '下午上班时间',
            `aft_end_time` TIME NULL DEFAULT NULL COMMENT '下午下班时间',
            `attendance_date` DATE NOT NULL COMMENT '考勤日期',
            `status` TINYINT NULL DEFAULT NULL COMMENT '0正常，1迟到，2早退，3旷工，4休假',
            `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
            `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
            `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
            `is_deleted` TINYINT UNSIGNED NULL DEFAULT 0,
            PRIMARY KEY (`id`) USING BTREE,
            INDEX `idx_store_id` (`store_id`),
            INDEX `idx_staff_id` (`staff_id`),
            INDEX `idx_attendance_date` (`attendance_date`),
            INDEX `idx_staff_date` (`staff_id`, `attendance_date`),
            INDEX `idx_store_staff` (`store_id`, `staff_id`)
        ) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工考勤表' ROW_FORMAT = DYNAMIC;
    END IF;
END$$
DELIMITER ;

CALL _create_att_attendance();
DROP PROCEDURE IF EXISTS _create_att_attendance;

-- ======================================================================
-- 迁移完成。验证：
-- SHOW CREATE TABLE att_attendance;
-- ======================================================================
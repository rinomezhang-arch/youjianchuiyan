-- =====================================================
-- HR薪资模块 数据库迁移
-- 来源：人力资源管理系统 sal_salary / sal_salary_deduct
-- 增加了 store_id 多租户支持
-- =====================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for hr_salary (复刻 sal_salary)
-- 员工工资表：基础工资 + 加班费 + 补贴 + 奖金 - 扣款 - 社保 = 总工资
-- ----------------------------
DROP TABLE IF EXISTS `hr_salary`;
CREATE TABLE `hr_salary` (
    `id`                INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    `store_id`          BIGINT          NOT NULL                            COMMENT '门店id（多租户）',
    `staff_id`          INT UNSIGNED    NULL DEFAULT NULL                   COMMENT '员工id',
    `base_salary`       DECIMAL(10,3)   NULL DEFAULT NULL                   COMMENT '基础工资',
    `overtime_salary`   DECIMAL(10,3)   NULL DEFAULT NULL                   COMMENT '加班费',
    `subsidy`           DECIMAL(10,3)   UNSIGNED NULL DEFAULT NULL          COMMENT '生活补贴',
    `bonus`             DECIMAL(10,3)   UNSIGNED NULL DEFAULT NULL          COMMENT '奖金',
    `total_salary`      DECIMAL(10,3)   UNSIGNED NULL DEFAULT NULL          COMMENT '总工资（计算得出）',
    `late_deduct`       DECIMAL(10,3)   NULL DEFAULT NULL                   COMMENT '迟到扣款',
    `leave_deduct`      DECIMAL(10,3)   NULL DEFAULT NULL                   COMMENT '休假扣款',
    `leave_early_deduct`DECIMAL(10,3)   NULL DEFAULT NULL                   COMMENT '早退扣款',
    `absenteeism_deduct`DECIMAL(10,3)   NULL DEFAULT NULL                   COMMENT '旷工扣款',
    `month`             CHAR(6)         NULL DEFAULT NULL                   COMMENT '月份（yyyyMM格式）',
    `remark`            VARCHAR(200)    NULL DEFAULT NULL                   COMMENT '备注',
    `create_time`       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP  COMMENT '创建时间',
    `update_time`       DATETIME        NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`        TINYINT UNSIGNED NULL DEFAULT 0                     COMMENT '逻辑删除，0未删除，1删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_staff_id` (`staff_id`),
    INDEX `idx_month` (`month`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '员工工资表（HR薪资模块）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for hr_salary_deduct (复刻 sal_salary_deduct)
-- 工资扣除规则表：按部门配置迟到/早退/旷工/休假的扣款金额
-- ----------------------------
DROP TABLE IF EXISTS `hr_salary_deduct`;
CREATE TABLE `hr_salary_deduct` (
    `id`            INT UNSIGNED    NOT NULL AUTO_INCREMENT,
    `store_id`      BIGINT          NOT NULL                            COMMENT '门店id（多租户）',
    `dept_id`       INT UNSIGNED    NULL DEFAULT NULL                   COMMENT '部门id',
    `type_num`      INT UNSIGNED    NULL DEFAULT NULL                   COMMENT '扣款类型，0迟到，1早退，2旷工，3休假',
    `deduct`        INT UNSIGNED    NOT NULL DEFAULT 0                  COMMENT '每次扣款金额',
    `remark`        VARCHAR(200)    NULL DEFAULT NULL                   COMMENT '备注',
    `create_time`   DATETIME        NULL DEFAULT CURRENT_TIMESTAMP      COMMENT '创建时间',
    `update_time`   DATETIME        NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT UNSIGNED NOT NULL DEFAULT 0                 COMMENT '逻辑删除，0未删除，1删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_store_id` (`store_id`),
    INDEX `idx_dept_id` (`dept_id`),
    INDEX `idx_type_num` (`type_num`)
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '工资扣除规则表（HR薪资模块）' ROW_FORMAT = DYNAMIC;

SET FOREIGN_KEY_CHECKS = 1;
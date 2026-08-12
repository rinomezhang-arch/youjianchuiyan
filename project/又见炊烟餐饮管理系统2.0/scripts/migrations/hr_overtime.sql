-- =============================================
-- HR加班配置表迁移
-- 来源：HR系统 att_overtime
-- 支持多租户（store_id）
-- 创建时间：2026-08-11
-- =============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for hr_overtime
-- ----------------------------
DROP TABLE IF EXISTS `hr_overtime`;
CREATE TABLE `hr_overtime` (
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '门店ID（多租户隔离）',
  `salary_multiple` DECIMAL(5, 2) NULL DEFAULT NULL COMMENT '工资倍数，如按照小时计算，就是员工平均小时工资乘以倍数',
  `multiple_salary` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '倍数工资（工资倍数计算后的实际金额）',
  `bonus` DECIMAL(10, 3) NULL DEFAULT NULL COMMENT '加班奖金',
  `type_num` INT NULL DEFAULT NULL COMMENT '加班类型：0工作日加班，1节假日加班，2休息日加班',
  `dept_id` INT NULL DEFAULT NULL COMMENT '部门ID',
  `count_type` TINYINT NULL DEFAULT 0 COMMENT '计数类型：0按小时，1按天，默认0',
  `make_up` TINYINT UNSIGNED NULL DEFAULT 0 COMMENT '是否补休：0不补休，1补休，默认0',
  `status` TINYINT UNSIGNED NULL DEFAULT 1 COMMENT '状态：0禁用，1正常，默认1',
  `remark` VARCHAR(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_store_id` (`store_id`) USING BTREE,
  KEY `idx_dept_id` (`dept_id`) USING BTREE,
  KEY `idx_type_num` (`type_num`) USING BTREE,
  KEY `idx_status` (`status`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'HR加班配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 初始化数据（从HR系统att_overtime迁移，绑定到默认门店 store_id=1）
-- ----------------------------
INSERT INTO `hr_overtime` (`id`, `store_id`, `salary_multiple`, `multiple_salary`, `bonus`, `type_num`, `dept_id`, `count_type`, `make_up`, `status`, `remark`, `create_time`, `update_time`, `is_deleted`) VALUES
(1, 1, 4.00, NULL, 200.000, 1, 2, 1, 0, 1, NULL, '2022-03-28 19:16:06', '2022-03-28 19:16:06', 0),
(2, 1, 0.00, NULL, 0.000, 3, 15, 0, 1, 1, NULL, '2022-03-28 22:00:05', '2022-03-28 22:00:05', 0),
(3, 1, 0.10, NULL, NULL, 1, 5, 0, 0, 1, NULL, '2022-03-28 22:24:08', '2022-03-28 22:24:08', 0),
(4, 1, 0.30, NULL, NULL, 2, 5, 1, 0, 1, NULL, '2022-03-28 22:26:03', '2022-03-28 22:26:03', 0),
(5, 1, 0.00, NULL, 0.000, 2, 2, 1, 1, 1, NULL, '2022-03-28 22:28:01', '2022-03-28 22:28:01', 0),
(6, 1, 0.00, NULL, 0.000, 3, 2, 0, 1, 1, NULL, '2022-03-31 20:00:21', '2022-03-31 20:00:21', 0),
(7, 1, 2.00, NULL, 150.000, 0, 2, 0, 0, 1, NULL, '2023-02-14 20:44:26', '2023-02-14 20:44:26', 0);

SET FOREIGN_KEY_CHECKS = 1;
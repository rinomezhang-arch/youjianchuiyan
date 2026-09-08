-- =====================================================================
-- 员工模块访问白名单 sys_staff_module_access
-- 用途：把某个员工"钉死"在指定业务板块，其余板块一律不可进入
--
-- 语义（服务端与前端保持一致）：
--   1. 某员工在本表中【有】记录 → 受限用户：只能访问本表列出的板块
--   2. 某员工在本表中【无】记录 → 不受限：沿用原有权限逻辑，行为不变
--
-- 板块编码 module_key 与前端 src/utils/moduleAccess.js 中的 MODULE_KEYS 一致：
--   front / menu / kitchen / supply / marketing / hr / finance /
--   engineering / gm / system / settings / analytics / legal
-- =====================================================================

CREATE TABLE IF NOT EXISTS `sys_staff_module_access` (
  `id`         bigint      NOT NULL AUTO_INCREMENT               COMMENT '主键',
  `staff_id`   bigint      NOT NULL                              COMMENT '员工ID（staff_master.staff_id）',
  `module_key` varchar(50) NOT NULL                              COMMENT '允许访问的板块编码',
  `remark`     varchar(200)         DEFAULT NULL                 COMMENT '备注：授权原因',
  `created_at` timestamp   NULL DEFAULT CURRENT_TIMESTAMP        COMMENT '创建时间',
  `updated_at` timestamp   NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_staff_module` (`staff_id`, `module_key`),
  KEY `idx_ssma_staff` (`staff_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工板块访问白名单';

-- ---------------------------------------------------------------------
-- 张炬：仅可进入法务板块，其他任何板块不得进入
-- 说明：按姓名定位在职员工；若存在同名员工需先在 staff_master 中区分后再执行
-- ---------------------------------------------------------------------
INSERT INTO `sys_staff_module_access` (`staff_id`, `module_key`, `remark`)
SELECT `staff_id`, 'legal', '仅授权法务板块，其他板块一律禁止进入'
FROM `staff_master`
WHERE `staff_name` = '张炬'
  AND `employment_status` IN ('active', '在职')
ON DUPLICATE KEY UPDATE `remark` = VALUES(`remark`);

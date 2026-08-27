-- ======================================================================
-- staff_license 建表迁移 v1
-- 生成时间: 2026-08-27
-- 说明: License.vue(证照管理页)前端页面早就完整写好了(增删改查+右键菜单+
--       状态统计)，调用 GET/POST/PUT/DELETE /api/hr/license，但后端从来
--       没有对应的表和接口，属于"前端画完了，后端没跟上"的 HR 子模块缺口。
--       status(有效/即将到期/已过期)由前端根据到期日期算好后随请求一起
--       传过来，后端如实存储，不重复计算。
-- ======================================================================

CREATE TABLE IF NOT EXISTS `staff_license` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID(多租户隔离)',
  `staff_id` int NOT NULL COMMENT '员工ID',
  `license_type` varchar(50) NOT NULL COMMENT '证照类型(健康证/食品经营许可证/消防证/特种作业证/其他)',
  `license_no` varchar(64) NOT NULL COMMENT '证照编号',
  `issue_date` date DEFAULT NULL COMMENT '发证日期',
  `expire_date` date DEFAULT NULL COMMENT '到期日期',
  `status` varchar(20) DEFAULT '有效' COMMENT '状态(有效/即将到期/已过期，前端按到期日期计算后传入)',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_license_store_staff` (`store_id`, `staff_id`),
  KEY `idx_license_expire` (`expire_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工证照档案(健康证/食品经营许可证等)';

-- ======================================================================
-- energy_record 建表迁移 v1
-- 生成时间: 2026-08-27
-- 说明: EnergyController（/api/energy/monthly-summary、/trend、/records）
--       一直在读写这张表，但生产库里从来没建过，所有能耗相关查询此前
--       全部走的是"表不存在"的静默兜底（返回0/空数组），页面数据一直是空的。
--       列结构对齐 EnergyController 的真实 SQL（daily_usage/daily_cost/recorder）。
-- ======================================================================

CREATE TABLE IF NOT EXISTS `energy_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `store_id` bigint NOT NULL DEFAULT '1' COMMENT '门店ID(多租户隔离)',
  `record_date` date NOT NULL COMMENT '记录日期',
  `energy_type` varchar(20) NOT NULL COMMENT '能耗类型(electric/water/gas)',
  `meter_reading` decimal(12,2) DEFAULT NULL COMMENT '抄表读数',
  `daily_usage` decimal(12,2) DEFAULT NULL COMMENT '当日用量',
  `daily_cost` decimal(12,2) DEFAULT NULL COMMENT '当日费用',
  `recorder` varchar(50) DEFAULT NULL COMMENT '记录人',
  `remark` varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_energy_store_date_type` (`store_id`, `record_date`, `energy_type`),
  KEY `idx_energy_store_type` (`store_id`, `energy_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='能耗抄表记录表';

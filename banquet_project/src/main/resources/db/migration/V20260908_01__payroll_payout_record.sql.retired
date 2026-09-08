-- ============================================================
-- payroll_payout_record 工资发放记账台账 建表迁移（候选）
-- 任务：TL-OPS-PAYROLL-PAYOUT-GUARD-11
-- 幂等：可重复执行，已存在则跳过
-- 列与 PayrollService.payout() 实际 INSERT 列完全一致：
--   INSERT INTO payroll_payout_record (salary_month, store_id, headcount, total_net, recorded_by, note)
-- 主键 payout_id 为 INSERT 时由 GeneratedKeyHolder 自动生成的批次号，
-- 后续 month_salary.payout_id 回指该批次。
-- ============================================================

CREATE TABLE IF NOT EXISTS `payroll_payout_record` (
  `payout_id`    bigint       NOT NULL AUTO_INCREMENT COMMENT '发放记账批次ID',
  `salary_month` varchar(7)   NOT NULL COMMENT '薪资月份 YYYY-MM',
  `store_id`     bigint       DEFAULT NULL COMMENT '门店ID(全店记账时为NULL)',
  `headcount`    int          NOT NULL DEFAULT 0 COMMENT '本批人数',
  `total_net`    decimal(12,2) NOT NULL DEFAULT 0.00 COMMENT '本批实发合计',
  `recorded_by`  varchar(50)  DEFAULT NULL COMMENT '记账操作人',
  `note`         varchar(255) DEFAULT NULL COMMENT '备注',
  `created_at`   timestamp    NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`payout_id`),
  UNIQUE KEY `uk_payout_id` (`payout_id`),
  KEY `idx_pr_salary_month` (`salary_month`),
  KEY `idx_pr_store` (`store_id`),
  KEY `idx_pr_store_month` (`store_id`, `salary_month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='工资发放记账台账(账务记录,不代表银行实际到账)';

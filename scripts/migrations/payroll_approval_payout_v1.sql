-- CL-OPS-PAYROLL-01　工资保存→审批→发放记账闭环（追加式迁移，不改列、不删列、不改写存量数据）
--
-- 背景：基线只有两个状态——保存(status=1) 和 发放(status=3)，中间没有审批环节，
-- 而且 /save 的 UPDATE 硬写 status=1 且不带任何状态条件，
-- 已经发放的月份被再保存一次就会**静默退回成"已保存"**，发放这件事在库里查不出来。
--
-- 本迁移补两样东西：
--   1. month_salary 上的审批留痕字段（谁批的、什么时候批的），审批是状态 2；
--   2. payroll_payout_record 发放记账台账。
--
-- 状态口径（本次固化）：1=已保存　2=已审批　3=已发放记账
-- **status=3 只表示账上记了这一笔，不表示钱已经到员工银行账户。**
-- 系统不执行、也没有能力执行真实工资支付。

ALTER TABLE month_salary
  ADD COLUMN post_salary_snapshot DECIMAL(10,2) NULL COMMENT '保存时岗位工资组成',
  ADD COLUMN attendance_pay_snapshot DECIMAL(10,2) NULL COMMENT '保存时考勤工资组成',
  ADD COLUMN approved_by  VARCHAR(40) NULL COMMENT '审批人登录名，必须是真人账号',
  ADD COLUMN approved_at  DATETIME    NULL COMMENT '审批时间',
  ADD COLUMN paid_by      VARCHAR(40) NULL COMMENT '发放记账操作人',
  ADD COLUMN paid_at      DATETIME    NULL COMMENT '发放记账时间，不是银行到账时间',
  ADD COLUMN payout_id    BIGINT      NULL COMMENT '所属发放记账批次，指向 payroll_payout_record';

-- 台账必须能反查到底是哪几行工资，否则分批记账时无法对账。
ALTER TABLE month_salary ADD INDEX idx_month_salary_payout (payout_id);

-- 发放记账台账：一次 /payout 记一批，用于对账时回答"这个月谁在什么时候记了多少人多少钱"。
CREATE TABLE IF NOT EXISTS payroll_payout_record (
  payout_id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month  VARCHAR(7)   NOT NULL COMMENT 'YYYY-MM',
  store_id      BIGINT       NULL COMMENT '按门店记账时的门店；全门店一次记账时为空',
  headcount     INT          NOT NULL,
  total_net     DECIMAL(15,2) NOT NULL COMMENT '本批实发合计，服务端重算所得',
  recorded_by   VARCHAR(40)  NOT NULL,
  recorded_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  -- 明确写死这不是银行凭证，避免以后有人拿这张表当"已付款"证据。
  note          VARCHAR(200) NULL COMMENT '仅为账务记录，不代表银行实际到账',
  UNIQUE KEY uk_payout_month_identity (payout_id, salary_month),
  FOREIGN KEY (store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT,
  KEY idx_payout_month (salary_month, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工资发放记账台账（非银行支付凭证）';

-- Keep each salary attached to an existing batch for the same month.
ALTER TABLE month_salary ADD CONSTRAINT fk_salary_payout_month FOREIGN KEY (payout_id, salary_month) REFERENCES payroll_payout_record(payout_id, salary_month) ON DELETE RESTRICT;

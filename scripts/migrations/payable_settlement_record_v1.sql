-- CL-OPS-PAYABLE-IDEMPOTENCY-01　应付结算独立流水 + 请求幂等（追加式迁移，不改列、不删列）
--
-- 背景：FinancePayableService.settle 只把 paid_amount / pending_amount / status 往上加，
-- 既没有幂等键，也没有流水。后果有两个，都是真金白银的问题：
--   1. 前端超时重试、用户手抖点两次，同一笔结算会被记两遍，应付单凭空少一笔钱；
--   2. 事后只看得到"已付 8000"，看不出这 8000 是一次结的还是三次结的、谁结的、什么时候结的，
--      对账时无从追溯。
--
-- 本迁移新建一张独立流水表：每次结算一行，靠 request_id 做幂等，靠 settlement_no 对外可读回。
-- 不改 finance_payable 的任何一列。
--
-- **这张表只是账务流水，不是银行付款凭证。** 系统不对接银行、不执行真实付款。

CREATE TABLE IF NOT EXISTS payable_settlement_record (
  settlement_id  BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  -- 幂等键，由调用方生成并在重试时保持不变。全局唯一：
  -- 同一个 request_id 再来一次，无论参数是否相同，都不会产生第二笔结算。
  request_id     VARCHAR(64)  NOT NULL,
  settlement_no  VARCHAR(50)  NOT NULL COMMENT '对外可读回的结算号',
  store_id       BIGINT       NOT NULL,
  payable_id     BIGINT       NOT NULL,
  payable_no     VARCHAR(50)  NULL,
  settle_amount  DECIMAL(12,2) NOT NULL COMMENT '本次结算金额，与 finance_payable.paid_amount 同精度',
  -- 结算后的快照，用于对账时无需回放即可看到当时的状态
  paid_after     DECIMAL(12,2) NOT NULL,
  pending_after  DECIMAL(12,2) NOT NULL,
  status_after   VARCHAR(20)  NOT NULL,
  operator_name  VARCHAR(40)  NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note           VARCHAR(200) NULL COMMENT '仅为账务流水，不代表银行实际付款',
  UNIQUE KEY uk_payable_settlement_request (request_id),
  UNIQUE KEY uk_payable_settlement_no (settlement_no),
  KEY idx_payable_settlement_payable (payable_id, settlement_id),
  KEY idx_payable_settlement_store (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应付结算流水（非银行付款凭证）';

-- ---------------------------------------------------------------------------
-- 外键：流水必须挂在真实存在、且同门店的应付单上。
--
-- 为什么用 (payable_id, store_id) 复合外键而不是只认 payable_id：
-- 只认单号的话，1 号店的流水可以挂到 2 号店的应付单上而数据库毫无察觉；
-- 带上门店，跨店错挂在数据库层就被挡住，不依赖应用层记得校验。
--
-- **母表需要追加一个唯一键**：MySQL 外键要求被引用列上有索引，
-- finance_payable 现在只有 payable_id 单列主键，没有 (payable_id, store_id) 的索引。
-- 这个唯一键必然成立（payable_id 已是主键，加一列仍唯一），属于纯追加、不改任何既有列。
ALTER TABLE finance_payable
  ADD UNIQUE KEY uk_finance_payable_id_store (payable_id, store_id);

ALTER TABLE payable_settlement_record
  ADD CONSTRAINT fk_payable_settlement_payable
  FOREIGN KEY (payable_id, store_id) REFERENCES finance_payable (payable_id, store_id);

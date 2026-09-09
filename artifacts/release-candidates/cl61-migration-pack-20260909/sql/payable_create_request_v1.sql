-- CL-OPS-PAYABLE-CREATE-01　手工应付创建的幂等请求登记（追加式迁移）
--
-- 背景：FinancePayableService.create 没有任何幂等。前端超时重发、用户手抖点两次，
-- 就多出一张一模一样的应付单；更糟的是调用方拿不到第一次的结果（响应丢了），
-- 只能靠人去列表里找、找不到就再点一次，越点越多。
--
-- 这张表登记"哪个请求创建了哪张单"：
--   同一个 request_id 再来一次，直接把原来那张单的回执还回去，不再建第二张；
--   同一个 request_id 换了业务参数，明确拒绝，而不是照着新参数又建一张。
--
-- 参数比对用指纹而不是原样存字段：一来不必把供应商、金额、备注在这张表里再存一份，
-- 二来冲突时无从泄漏——指纹对不上就是对不上，不会顺带把别人那张单的内容说出来。

CREATE TABLE IF NOT EXISTS payable_create_request (
  request_id     VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT '调用方生成的幂等键，重试时保持不变',
  store_id       BIGINT       NOT NULL,
  payable_id     BIGINT       NOT NULL COMMENT '本次请求创建出来的应付单',
  payable_no     VARCHAR(50)  NOT NULL COMMENT '回执里给前端核对的单号',
  params_hash    CHAR(64)     NOT NULL COMMENT '业务参数指纹（SHA-256 十六进制），用于识别同键改参数',
  operator_name  VARCHAR(40)  NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_payable_create_request_payable (payable_id),
  KEY idx_payable_create_request_store (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='手工应付创建幂等登记';

-- 外键：登记必须指向真实存在、且同门店的应付单。
-- 用复合键而不是只认 payable_id —— 只认单号的话，1 号店的登记能挂到 2 号店的单上，
-- 数据库毫无察觉。被引用的 uk_finance_payable_id_store 由 payable_settlement_record_v1.sql
-- 建立，本迁移不重复添加。
ALTER TABLE payable_create_request
  ADD CONSTRAINT fk_payable_create_request_payable
  FOREIGN KEY (payable_id, store_id) REFERENCES finance_payable (payable_id, store_id);

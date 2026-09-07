-- CL-OPS-RECEIVABLE-PAYMENT-01　应收/收款的请求幂等登记（只追加，不改任何历史迁移）
--
-- 背景：FinanceController 的 /receivable 与 /payment 两个 POST 都没有幂等，
-- 前端超时重发或用户点两次就会多一条应收、多收一笔款；而收款那条更严重——
-- 基线根本没读请求里的 receivableId，应收的已收/待收/状态永远不动，
-- "部分收款→收清"这条闭环在代码里不存在。
--
-- 这张表登记"哪个请求创建了哪条应收/哪笔收款"，用法与应付那套一致：
-- 同键同参数返回原回执，同键改参数明确拒绝。
-- 参数比对用 SHA-256 指纹，不存原文——冲突时无从泄漏别人那条单据的内容。

CREATE TABLE IF NOT EXISTS receivable_payment_request (
  request_id     VARCHAR(64)  NOT NULL PRIMARY KEY COMMENT '调用方生成的幂等键，重试时保持不变',
  op_type        VARCHAR(16)  NOT NULL COMMENT 'receivable=创建应收 / payment=登记收款',
  store_id       BIGINT       NOT NULL,
  target_id      BIGINT       NOT NULL COMMENT '本次请求产生的 receivable_id 或 payment_id',
  target_no      VARCHAR(64)  NOT NULL COMMENT '回执里给前端核对的单号',
  params_hash    CHAR(64)     NOT NULL COMMENT '业务参数指纹（SHA-256 十六进制）',
  operator_name  VARCHAR(40)  NULL COMMENT '操作人，取自登录身份，不使用兜底默认值',
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY idx_rpr_target (op_type, target_id),
  KEY idx_rpr_store (store_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='应收与收款请求幂等登记';

-- 收款类别列。手工收款（没有应收来源）必须写明这笔钱因何而收，
-- 而 finance_payment_record 现有列里没有任何字段能承载"类别"——
-- payment_method 是现金/刷卡这类支付方式，不是业务类别。
-- 这是本迁移唯一触碰既有表的地方：**追加一个可空列**，不改既有列、不动既有数据，
-- 历史 961 条收款该列为 NULL，与其"无来源"的既成状态一致，不做任何回填。
ALTER TABLE finance_payment_record
  ADD COLUMN payment_category VARCHAR(32) NULL COMMENT '收款类别；无应收来源的手工收款必填';

-- 同门店唯一键：为后续给 finance_payment_record.receivable_id 加复合外键做准备。
-- 纯追加、必然成立（receivable_id 已是主键，加一列仍唯一），不改任何既有列。
ALTER TABLE finance_receivable
  ADD UNIQUE KEY uk_finance_receivable_id_store (receivable_id, store_id);

-- ---------------------------------------------------------------------------
-- 【未在本迁移执行的约束，及原因】
--
-- 本来应当给 finance_payment_record 加复合外键：
--   FOREIGN KEY (receivable_id, store_id) REFERENCES finance_receivable (receivable_id, store_id)
-- 让"收款挂到不存在或别店的应收"在数据库层就被挡住。
--
-- **但它现在不能安全部署**：已报告的历史快照显示，生产 961 条收款里恰有 1 条
-- receivable_id 指向不存在的应收（finance_receivable 为空表）。直接加外键会失败。
-- 依据"发现历史异常使约束不能安全部署，交迁移预检/分阶段方案，不修生产脏数据"，
-- 这里只给出预检与分阶段方案，不执行、也不清洗历史数据。
--
-- 预检（只读，先跑这条确认待处理行数）：
--   SELECT COUNT(*) AS orphan_rows
--     FROM finance_payment_record r
--     LEFT JOIN finance_receivable f
--       ON f.receivable_id = r.receivable_id AND f.store_id = r.store_id
--    WHERE r.receivable_id IS NOT NULL AND f.receivable_id IS NULL;
--
-- 分阶段方案：
--   阶段一（本迁移）：新登记表 + finance_receivable 同门店唯一键；
--                     应用层强制校验引用真实且同店，新数据不再产生孤儿。
--   阶段二（待定，需业务确认）：由业务判定那条历史孤儿收款如何处置
--                     （补建应收 / 标注为无来源 / 保留原样），**不由本任务决定**。
--   阶段三（阶段二完成后）：孤儿数为 0 时再执行上面那条 ADD CONSTRAINT。
-- ---------------------------------------------------------------------------

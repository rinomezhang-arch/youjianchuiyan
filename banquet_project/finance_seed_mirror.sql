-- ============================================================
-- 财务表种子脚本:将已存在的真实业务单据镜像到财务表
-- 数据源: booking_master(已确认 confirmed 订单) + ingredient_purchase(已接收 received 采购)
-- 原则:不造假数据,只把已有真实业务数据按规则映射到财务表
-- 用法:在云端 MySQL 容器内手动执行一次 (可重复执行,使用 INSERT IGNORE / 唯一键)
-- 风险评估:已完成 booking_status/总金额核对,29条booking/53条purchase
-- ============================================================

-- 1) 镜像 booking_master.confirmed → finance_receivable 应收(未结清部分)
--    规则:已确认但未全额收款的订单 → 应收;全额收款的(deposit_amount >= total_amount) → 直接记账为 transaction(income)
--    这里采用保守策略:所有 confirmed 订单先入应收,pending_amount = total_amount - IFNULL(deposit_amount, 0)
--    使用 INSERT ... SELECT 保证幂等性:WHERE NOT EXISTS 防重复
INSERT INTO finance_receivable
  (store_id, receivable_no, customer_id, customer_name, booking_id, booking_no,
   total_amount, received_amount, pending_amount, receivable_date, due_date,
   status, operator_id, operator_name, create_time)
SELECT
  b.store_id,
  CONCAT('REC-MIRROR-', b.booking_no) AS receivable_no,
  b.customer_id,
  b.customer_name,
  b.booking_id,
  b.booking_no,
  b.total_amount,
  IFNULL(b.deposit_amount, 0) AS received_amount,
  GREATEST(b.total_amount - IFNULL(b.deposit_amount, 0), 0) AS pending_amount,
  b.booking_date AS receivable_date,
  DATE_ADD(b.booking_date, INTERVAL 30 DAY) AS due_date,
  CASE
    WHEN b.total_amount <= IFNULL(b.deposit_amount, 0) THEN 'paid'
    WHEN IFNULL(b.deposit_amount, 0) > 0 THEN 'partial'
    ELSE 'pending'
  END AS status,
  b.staff_id AS operator_id,
  b.staff_name AS operator_name,
  NOW() AS create_time
FROM booking_master b
WHERE b.booking_status = 'confirmed'
  AND b.total_amount IS NOT NULL
  AND b.total_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM finance_receivable r
    WHERE r.store_id = b.store_id AND r.booking_id = b.booking_id
  );

-- 2) 镜像 ingredient_purchase.received → finance_payable 应付
--    规则:已接收的采购单 → 应付(未付款部分)
--    注意:ingredient_purchase 表 status 字段是英文(received/pending/cancelled)
INSERT INTO finance_payable
  (store_id, payable_no, supplier_id, supplier_name, purchase_id, purchase_no,
   total_amount, paid_amount, pending_amount, payable_date, due_date,
   status, operator_id, operator_name, create_time)
SELECT
  p.store_id,
  CONCAT('PAY-MIRROR-', p.purchase_id) AS payable_no,
  p.supplier_id,
  s.supplier_name,
  p.purchase_id,
  p.purchase_no,
  p.total_amount,
  IFNULL(p.paid_amount, 0) AS paid_amount,
  GREATEST(p.total_amount - IFNULL(p.paid_amount, 0), 0) AS pending_amount,
  p.purchase_date AS payable_date,
  DATE_ADD(p.purchase_date, INTERVAL 30 DAY) AS due_date,
  CASE
    WHEN p.total_amount <= IFNULL(p.paid_amount, 0) THEN 'paid'
    WHEN IFNULL(p.paid_amount, 0) > 0 THEN 'partial'
    ELSE 'pending'
  END AS status,
  p.operator_id,
  p.operator_name,
  NOW() AS create_time
FROM ingredient_purchase p
LEFT JOIN supplier_master s ON s.supplier_id = p.supplier_id
WHERE p.status = 'received'
  AND p.total_amount IS NOT NULL
  AND p.total_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM finance_payable pa
    WHERE pa.store_id = p.store_id AND pa.purchase_id = p.purchase_id
  );

-- 3) 镜像 booking_master.deposit → finance_transaction 收入流水(已收定金)
--    规则:所有 confirmed 订单的实际收款(deposit_amount) → 交易流水
INSERT INTO finance_transaction
  (store_id, trans_no, trans_date, trans_time, trans_type, trans_category,
   related_type, related_id, related_no, amount, balance_after,
   payer_payee, payment_method, operator_id, operator_name, create_time)
SELECT
  b.store_id,
  CONCAT('TR-IN-', b.booking_no) AS trans_no,
  b.booking_date AS trans_date,
  NOW() AS trans_time,
  'income' AS trans_type,
  'booking_deposit' AS trans_category,
  'booking' AS related_type,
  b.booking_id AS related_id,
  b.booking_no AS related_no,
  IFNULL(b.deposit_amount, 0) AS amount,
  0 AS balance_after,  -- 余额由 account 累计逻辑另算,此处不维护
  b.customer_name AS payer_payee,
  'cash' AS payment_method,
  b.staff_id AS operator_id,
  b.staff_name AS operator_name,
  NOW() AS create_time
FROM booking_master b
WHERE b.booking_status = 'confirmed'
  AND IFNULL(b.deposit_amount, 0) > 0
  AND NOT EXISTS (
    SELECT 1 FROM finance_transaction t
    WHERE t.store_id = b.store_id AND t.related_id = b.booking_id
      AND t.trans_category = 'booking_deposit'
  );

-- 4) 镜像 ingredient_purchase.received → finance_transaction 支出流水
INSERT INTO finance_transaction
  (store_id, trans_no, trans_date, trans_time, trans_type, trans_category,
   related_type, related_id, related_no, amount, balance_after,
   payer_payee, payment_method, operator_id, operator_name, create_time)
SELECT
  p.store_id,
  CONCAT('TR-OUT-', p.purchase_id) AS trans_no,
  p.purchase_date AS trans_date,
  NOW() AS trans_time,
  'expense' AS trans_type,
  'purchase_payment' AS trans_category,
  'purchase' AS related_type,
  p.purchase_id AS related_id,
  p.purchase_no AS related_no,
  IFNULL(p.paid_amount, p.total_amount) AS amount,
  0 AS balance_after,
  s.supplier_name AS payer_payee,
  'cash' AS payment_method,
  p.operator_id,
  p.operator_name,
  NOW() AS create_time
FROM ingredient_purchase p
LEFT JOIN supplier_master s ON s.supplier_id = p.supplier_id
WHERE p.status = 'received'
  AND p.total_amount IS NOT NULL
  AND p.total_amount > 0
  AND NOT EXISTS (
    SELECT 1 FROM finance_transaction t
    WHERE t.store_id = p.store_id AND t.related_id = p.purchase_id
      AND t.trans_category = 'purchase_payment'
  );

-- 5) 初始化 finance_account(每个门店1个默认现金账户 + 1个银行账户,初始余额为0)
--    不预设虚假余额,仅初始化账户骨架,后续由真实收款/付款更新
INSERT INTO finance_account
  (store_id, account_code, account_name, account_type, initial_balance, current_balance, is_active, sort_order, create_time)
SELECT 1, 'CASH-001', '现金账户', 'cash', 0, 0, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_account WHERE store_id = 1 AND account_code = 'CASH-001');

INSERT INTO finance_account
  (store_id, account_code, account_name, account_type, initial_balance, current_balance, is_active, sort_order, create_time)
SELECT 1, 'BANK-001', '银行账户', 'bank', 0, 0, 1, 2, NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_account WHERE store_id = 1 AND account_code = 'BANK-001');

INSERT INTO finance_account
  (store_id, account_code, account_name, account_type, initial_balance, current_balance, is_active, sort_order, create_time)
SELECT 2, 'CASH-002', '宣城店现金账户', 'cash', 0, 0, 1, 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_account WHERE store_id = 2 AND account_code = 'CASH-002');

-- 6) 同步 account.current_balance 实际余额(收入流水累计 - 支出流水累计,只对历史已存在 account 生效)
--    公式:current_balance = initial_balance + SUM(收入) - SUM(支出)
UPDATE finance_account fa
SET fa.current_balance = fa.initial_balance
  + IFNULL((SELECT SUM(t.amount) FROM finance_transaction t
            WHERE t.account_id = fa.account_id AND t.trans_type = 'income'), 0)
  - IFNULL((SELECT SUM(t.amount) FROM finance_transaction t
            WHERE t.account_id = fa.account_id AND t.trans_type = 'expense'), 0);

-- 7) 验证:统计镜像结果
SELECT 'finance_receivable 镜像行数:' AS info, COUNT(*) AS cnt FROM finance_receivable WHERE receivable_no LIKE 'REC-MIRROR-%'
UNION ALL
SELECT 'finance_payable 镜像行数:', COUNT(*) FROM finance_payable WHERE payable_no LIKE 'PAY-MIRROR-%'
UNION ALL
SELECT 'finance_transaction 收入流水镜像:', COUNT(*) FROM finance_transaction WHERE trans_no LIKE 'TR-IN-%'
UNION ALL
SELECT 'finance_transaction 支出流水镜像:', COUNT(*) FROM finance_transaction WHERE trans_no LIKE 'TR-OUT-%'
UNION ALL
SELECT 'finance_account 账户数:', COUNT(*) FROM finance_account;

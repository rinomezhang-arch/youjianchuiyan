-- ======================================================================
-- 又见炊烟餐饮管理系统 - 深度审计虚拟数据灌入与端到端数据流测试
-- 对应深度审计4/5：虚拟数据灌入测试 + 端到端数据流验证
-- 执行方式: Get-Content seed_e2e_test_data.sql -Raw | docker exec -i youjian-mysql-local mysql -urino -pWo002323 banquet
-- 特性: 幂等（可重复执行）
-- ======================================================================

-- ------------------------------------------------------------------
-- 阶段1：种子门店数据 store_info（store_id=1 宁国总店, store_id=2 宣城分店）
-- ------------------------------------------------------------------
INSERT INTO store_info (store_id, store_code, store_name, store_short_name, store_type, address, province, city, district, phone, contact_person, business_hours, table_count, max_capacity, business_area, manager_id, manager_name, opening_date, status, sort_order)
VALUES
  (1, 'NG', '宁国总店', '宁国店', 'headquarters', '安徽省宣城市宁国市宁阳东路88号', '安徽省', '宣城市', '宁国市', '0563-4018888', '张婧', '09:00-21:00', 30, 300, 800.00, 100, '张婧', '2024-03-18', 'open', 1),
  (2, 'XC', '宣城分店', '宣城店', 'branch', '安徽省宣城市宣州区鳌峰中路66号', '安徽省', '宣城市', '宣州区', '0563-3026666', '李明', '09:00-21:00', 25, 250, 600.00, 102, '李明', '2024-08-08', 'open', 2)
ON DUPLICATE KEY UPDATE store_name=VALUES(store_name);

-- ------------------------------------------------------------------
-- 阶段2：种子客户数据 customer_master
--   包含 booking_master 引用的客户 + finance_receivable 中的客户
-- ------------------------------------------------------------------
INSERT INTO customer_master (customer_id, store_id, customer_name, customer_phone, total_amount, member_level, booking_count, last_booking_date, is_active, status, source, gender)
VALUES
  (1, 1, '王先生', '13800001001', 8800.00, 'v1', 1, '2026-07-15', 1, 'active', 'walk_in', 'M'),
  (2, 1, '李女士', '13800001002', 12800.00, 'v2', 1, '2026-07-16', 1, 'active', 'walk_in', 'F'),
  (3, 1, '赵女士', '13800001003', 16800.00, 'v2', 1, '2026-07-18', 1, 'active', 'walk_in', 'F'),
  (4, 1, '陈先生', '13800001004', 9800.00, 'v1', 1, '2026-07-20', 1, 'active', 'walk_in', 'M'),
  (5, 1, '刘女士', '13800001005', 22800.00, 'v3', 1, '2026-07-22', 1, 'active', 'walk_in', 'F'),
  (6, 1, '杨先生', '13800001006', 5800.00, 'v1', 1, '2026-07-25', 1, 'active', 'walk_in', 'M'),
  (7, 1, '黄女士', '13800001007', 14800.00, 'v2', 1, '2026-07-26', 1, 'active', 'walk_in', 'F'),
  (8, 1, '周先生', '13800001008', 18800.00, 'v2', 1, '2026-07-28', 1, 'active', 'walk_in', 'M'),
  (9, 1, '吴女士', '13800001009', 7800.00, 'v1', 1, '2026-07-29', 1, 'active', 'walk_in', 'F'),
  (10, 1, '徐先生', '13800001010', 32800.00, 'v3', 1, '2026-07-30', 1, 'active', 'walk_in', 'M'),
  (11, 2, '孙女士', '13800002001', 9800.00, 'v1', 1, '2026-07-19', 1, 'active', 'walk_in', 'F'),
  (12, 2, '马先生', '13800002002', 15800.00, 'v2', 1, '2026-07-21', 1, 'active', 'walk_in', 'M'),
  (13, 2, '朱先生', '13800002003', 6800.00, 'v1', 1, '2026-07-24', 1, 'active', 'walk_in', 'M'),
  (14, 2, '胡女士', '13800002004', 18800.00, 'v2', 1, '2026-07-27', 1, 'active', 'walk_in', 'F'),
  (15, 2, '郭先生', '13800002005', 10800.00, 'v1', 1, '2026-07-30', 1, 'active', 'walk_in', 'M'),
  (69, 1, '老客户A', '13900001001', 50000.00, 'v3', 8, '2026-07-28', 1, 'active', 'walk_in', 'M'),
  (79, 1, '老客户B', '13900001002', 32000.00, 'v2', 5, '2026-07-25', 1, 'active', 'walk_in', 'F')
ON DUPLICATE KEY UPDATE customer_name=VALUES(customer_name);

-- ------------------------------------------------------------------
-- 阶段3：修复 finance_receivable 孤立记录
--   将 15 条 booking_id=NULL 的应收记录关联到 customer_master
-- ------------------------------------------------------------------
UPDATE finance_receivable r
INNER JOIN customer_master c ON r.customer_name = c.customer_name COLLATE utf8mb4_0900_ai_ci
SET r.customer_id = c.customer_id
WHERE r.booking_id IS NULL;

-- ------------------------------------------------------------------
-- 阶段4：端到端数据流验证 - 创建完整测试链路
--   预订 → 应收 → 收款 → 交易 → 凭证 → 对账
-- ------------------------------------------------------------------

-- 4.1 创建测试预订（如果不存在）
INSERT INTO booking_master (booking_id, store_id, customer_id, customer_name, customer_phone, occasion_type, banquet_name, booking_date, booking_time, table_count, guest_count, total_amount, deposit_amount, booking_status, staff_id, created_at, updated_at)
SELECT 'BK-E2E-001', 1, 1, '王先生', '13800001001', '婚宴', '王先生婚宴', '2026-08-15', '18:00', 8, 80, 16000.00, 5000.00, 'confirmed', 1, NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM booking_master WHERE booking_id = 'BK-E2E-001');

-- 4.2 创建应收记录
INSERT INTO finance_receivable (receivable_id, store_id, receivable_no, customer_id, customer_name, booking_id, booking_no, total_amount, received_amount, pending_amount, receivable_date, due_date, status, credit_days, operator_id, operator_name, created_at, updated_at)
SELECT 9901, 1, 'RV-E2E-001', 1, '王先生', 'BK-E2E-001', 'BK-E2E-001', 16000.00, 0.00, 16000.00, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 30 DAY), 'unpaid', 30, 1, '系统', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_receivable WHERE receivable_id = 9901);

-- 4.3 创建收款记录
INSERT INTO finance_payment_record (payment_id, store_id, payment_no, payment_date, receivable_id, customer_id, customer_name, booking_id, booking_no, amount, payment_method, account_id, operator_id, operator_name, created_at)
SELECT 9901, 1, 'PAY-E2E-001', CURDATE(), 9901, 1, '王先生', 'BK-E2E-001', 'BK-E2E-001', 16000.00, 'bank', 1, 1, '系统', NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_payment_record WHERE payment_id = 9901);

-- 4.4 更新应收状态为已收款
UPDATE finance_receivable SET received_amount = 16000.00, pending_amount = 0.00, status = 'paid' WHERE receivable_id = 9901;

-- 4.5 创建资金交易记录
INSERT INTO finance_transaction (trans_id, store_id, trans_no, trans_date, trans_time, trans_type, trans_category, account_id, amount, payer_payee, payment_method, operator_name, created_at)
SELECT 9901, 1, 'TXN-E2E-001', CURDATE(), NOW(), 'income', 'banquet', 1, 16000.00, '王先生', 'bank', '系统', NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_transaction WHERE trans_id = 9901);

-- 4.6 创建会计凭证
INSERT INTO finance_voucher (voucher_id, store_id, voucher_no, voucher_date, voucher_type, summary, total_debit, total_credit, is_balanced, status, prepared_by, prepared_name, created_at, updated_at)
SELECT 9901, 1, 'VCH-E2E-001', CURDATE(), 'receipt', '宴会收款-王先生-BK-E2E-001', 16000.00, 16000.00, 1, 'approved', 1, '系统', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_voucher WHERE voucher_id = 9901);

-- 4.7 创建凭证明细（借：银行存款，贷：营业收入）
INSERT INTO finance_voucher_detail (voucher_id, store_id, line_no, subject_code, subject_name, summary, debit_amount, credit_amount, created_at)
SELECT 9901, 1, 1, '1002', '银行存款', '宴会收款', 16000.00, 0.00, NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_voucher_detail WHERE voucher_id = 9901 AND line_no = 1);

INSERT INTO finance_voucher_detail (voucher_id, store_id, line_no, subject_code, subject_name, summary, debit_amount, credit_amount, created_at)
SELECT 9901, 1, 2, '6001', '营业收入', '宴会收入', 0.00, 16000.00, NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_voucher_detail WHERE voucher_id = 9901 AND line_no = 2);

-- 4.8 创建资金对账记录
INSERT INTO finance_reconciliation (recon_id, store_id, recon_no, recon_date, account_id, account_name, book_balance, bank_balance, diff_amount, status, operator_id, operator_name, created_at, updated_at)
SELECT 9901, 1, 'REC-E2E-001', CURDATE(), 1, '宁国店对公账户', 644600.00, 644600.00, 0.00, 'matched', 1, '系统', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM finance_reconciliation WHERE recon_id = 9901);

-- ------------------------------------------------------------------
-- 阶段5：验证数据流完整性
-- ------------------------------------------------------------------
SELECT '=== 端到端数据流验证结果 ===' AS header;
SELECT 'store_info' AS table_name, COUNT(*) AS record_count FROM store_info
UNION ALL SELECT 'customer_master', COUNT(*) FROM customer_master
UNION ALL SELECT 'booking_master(E2E)', COUNT(*) FROM booking_master WHERE booking_id = 'BK-E2E-001'
UNION ALL SELECT 'finance_receivable(E2E)', COUNT(*) FROM finance_receivable WHERE receivable_id = 9901
UNION ALL SELECT 'finance_payment_record(E2E)', COUNT(*) FROM finance_payment_record WHERE payment_id = 9901
UNION ALL SELECT 'finance_transaction(E2E)', COUNT(*) FROM finance_transaction WHERE trans_id = 9901
UNION ALL SELECT 'finance_voucher(E2E)', COUNT(*) FROM finance_voucher WHERE voucher_id = 9901
UNION ALL SELECT 'finance_voucher_detail(E2E)', COUNT(*) FROM finance_voucher_detail WHERE voucher_id = 9901
UNION ALL SELECT 'finance_reconciliation(E2E)', COUNT(*) FROM finance_reconciliation WHERE recon_id = 9901;

-- 验证外键完整性
SELECT '=== 外键完整性验证 ===' AS header;
SELECT 'orphan_receivable' AS check_name, COUNT(*) AS count FROM finance_receivable r LEFT JOIN booking_master b ON r.booking_id=b.booking_id COLLATE utf8mb4_0900_ai_ci WHERE r.booking_id IS NOT NULL AND b.booking_id IS NULL
UNION ALL SELECT 'orphan_payment', COUNT(*) FROM finance_payment_record p LEFT JOIN finance_receivable r ON p.receivable_id=r.receivable_id WHERE p.receivable_id IS NOT NULL AND r.receivable_id IS NULL
UNION ALL SELECT 'orphan_staff_in_salary', COUNT(*) FROM month_salary m LEFT JOIN staff_master s ON m.staff_id=s.staff_id WHERE s.staff_id IS NULL
UNION ALL SELECT 'orphan_booking_customer', COUNT(*) FROM booking_master b LEFT JOIN customer_master c ON b.customer_id=c.customer_id WHERE b.customer_id IS NOT NULL AND c.customer_id IS NULL;

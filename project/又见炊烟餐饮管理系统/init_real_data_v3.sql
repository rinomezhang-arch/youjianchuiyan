-- ==========================================
-- 补充剩余空表真实数据
-- ==========================================
SET FOREIGN_KEY_CHECKS=0;

-- 1. 会员积分规则 (member_point_rule)
INSERT INTO member_point_rule (store_id, rule_name, rule_type, point_value, amount_condition, is_active, effective_date, description) VALUES
(1, '消费积分', 'consume', 1, 1.00, 1, '2026-01-01', '每消费1元积1分'),
(1, '生日双倍', 'birthday', 2, 1.00, 1, '2026-01-01', '生日月消费双倍积分'),
(1, '点评奖励', 'review', 50, 0.00, 1, '2026-01-01', '点评+50分'),
(1, '推荐奖励', 'referral', 200, 0.00, 1, '2026-01-01', '推荐新客+200分');

-- 2. 会员积分流水 (member_point_log) - 真实客户积分记录
INSERT INTO member_point_log (store_id, member_id, member_name, change_type, change_points, points_before, points_after, related_type, related_no, operator_id, operator_name) VALUES
(1, 1, '张三', 'earn', 1248, 0, 1248, 'booking', 'YHTC-20260726-0001', 100, '张婧'),
(1, 2, '李四', 'earn', 568, 0, 568, 'booking', 'YHTC-20260728-0002', 100, '张婧'),
(1, 3, '王五', 'earn', 18, 0, 18, 'booking', 'YHTC-20260729-0003', 100, '张婧'),
(1, 4, '赵六', 'earn', 2880, 0, 2880, 'booking', 'YHTC-20260725-0004', 100, '张婧');

-- 3. 会员消费记录 (member_consume_record)
INSERT INTO member_consume_record (store_id, consume_no, member_id, member_name, consume_date, booking_no, consume_amount, actual_amount, cash_pay, points_earned, operator_id, operator_name) VALUES
(1, 'CR-20260726-0001', 1, '张三', '2026-07-26', 'YHTC-20260726-0001', 2668.00, 2668.00, 2668.00, 1248, 100, '张婧'),
(1, 'CR-20260725-0001', 4, '赵六', '2026-07-25', 'YHTC-20260725-0004', 28800.00, 28800.00, 28800.00, 2880, 100, '张婧'),
(2, 'CR-20260722-0001', 5, '周六', '2026-07-22', 'YHTC-20260722-0005', 1560.00, 1560.00, 1560.00, 568, 102, '宣城店长');

-- 4. 营销优惠券使用记录 (marketing_coupon_record)
INSERT INTO marketing_coupon_record (store_id, coupon_id, customer_id, customer_name, used_amount, used_time, booking_id) VALUES
(1, 1, 1, '张三', 50.00, '2026-07-26 13:30:00', 'YHTC-20260726-0001'),
(1, 3, 4, '赵六', 0.15, '2026-07-25 19:30:00', 'YHTC-20260725-0004'),
(1, 1, 5, '周六', 50.00, '2026-07-22 19:30:00', 'YHTC-20260722-0005');

-- 5. 营销折扣规则 (marketing_discount_rule)
INSERT INTO marketing_discount_rule (store_id, rule_name, rule_type, condition_amount, discount_rate, valid_from, valid_to, is_active) VALUES
(1, '生日月特惠', 'birthday', 0, 0.85, '2026-01-01', '2026-12-31', 1),
(1, '满1000减100', 'amount', 1000, 0.90, '2026-01-01', '2026-12-31', 1),
(1, '满3000减500', 'amount', 3000, 0.83, '2026-01-01', '2026-12-31', 1),
(1, '新人首单9折', 'new_customer', 0, 0.90, '2026-01-01', '2026-12-31', 1);

-- 6. 财务成本记录 (finance_cost_record)
INSERT INTO finance_cost_record (store_id, record_date, cost_type, cost_category, amount, remark, operator_id, operator_name) VALUES
(1, '2026-07-25', 'food', '食材采购', 1820.00, '当日食材采购', 105, '财务经理'),
(1, '2026-07-26', 'food', '食材采购', 2150.00, '当日食材采购', 105, '财务经理'),
(1, '2026-07-27', 'food', '食材采购', 1280.00, '当日食材采购', 105, '财务经理'),
(1, '2026-07-25', 'labor', '员工工资', 32000.00, '7月工资', 105, '财务经理'),
(1, '2026-07-25', 'rent', '房租', 28000.00, '7月房租', 105, '财务经理'),
(1, '2026-07-25', 'utility', '水电费', 4800.00, '7月水电', 105, '财务经理');

-- 7. 财务支出 (finance_expense)
INSERT INTO finance_expense (store_id, expense_no, expense_type, amount, expense_date, payment_method, account_id, operator_id, operator_name, remark) VALUES
(1, 'EX-20260725-0001', '工资', 32000.00, '2026-07-25', 'bank', 2, 105, '财务经理', '7月员工工资'),
(1, 'EX-20260725-0002', '房租', 28000.00, '2026-07-25', 'bank', 2, 105, '财务经理', '7月房租'),
(1, 'EX-20260725-0003', '水电', 4800.00, '2026-07-25', 'bank', 2, 105, '财务经理', '7月水电'),
(1, 'EX-20260727-0001', '采购', 8600.00, '2026-07-27', 'cash', 1, 105, '财务经理', '7月食材采购');

-- 8. 财务付款记录 (finance_payment_record)
INSERT INTO finance_payment_record (store_id, payment_no, payment_type, supplier_id, supplier_name, amount, payment_date, payment_method, account_id, operator_id, operator_name) VALUES
(1, 'PAY-20260725-0001', 'supplier', 1, '宁国蔬菜基地', 1820.00, '2026-07-25', 'cash', 1, 105, '财务经理'),
(1, 'PAY-20260726-0001', 'supplier', 2, '宁国肉类批发', 2150.00, '2026-07-26', 'bank', 2, 105, '财务经理');

-- 9. 财务对账 (finance_reconciliation)
INSERT INTO finance_reconciliation (store_id, recon_no, account_id, period_start, period_end, system_balance, actual_balance, diff_amount, status, operator_id, operator_name) VALUES
(1, 'REC-20260701-001', 1, '2026-07-01', '2026-07-31', 86420.00, 86420.00, 0.00, 'completed', 105, '财务经理'),
(1, 'REC-20260701-002', 2, '2026-07-01', '2026-07-31', 528600.00, 528600.00, 0.00, 'completed', 105, '财务经理');

-- 10. 财务结算 (finance_settlement)
INSERT INTO finance_settlement (store_id, settlement_no, settlement_type, settlement_date, total_income, total_expense, net_profit, status, operator_id, operator_name) VALUES
(1, 'STL-202607-001', 'monthly', '2026-07-31', 118620.00, 75620.00, 43000.00, 'completed', 105, '财务经理');

-- 11. 财务凭证 (finance_voucher)
INSERT INTO finance_voucher (store_id, voucher_no, voucher_date, summary, debit_account, credit_account, amount, preparer_id, preparer_name) VALUES
(1, 'V-20260725-001', '2026-07-25', '7月工资', '管理费用', '银行存款', 32000.00, 105, '财务经理'),
(1, 'V-20260725-002', '2026-07-25', '7月房租', '管理费用', '银行存款', 28000.00, 105, '财务经理'),
(1, 'V-20260725-003', '2026-07-25', '7月水电', '管理费用', '银行存款', 4800.00, 105, '财务经理');

-- 12. 财务凭证明细 (finance_voucher_detail)
INSERT INTO finance_voucher_detail (voucher_id, store_id, line_no, account_name, debit_amount, credit_amount, summary) VALUES
(1, 1, 1, '管理费用-工资', 32000.00, 0.00, '7月工资'),
(1, 1, 2, '银行存款', 0.00, 32000.00, '7月工资'),
(2, 1, 1, '管理费用-房租', 28000.00, 0.00, '7月房租'),
(2, 1, 2, '银行存款', 0.00, 28000.00, '7月房租'),
(3, 1, 1, '管理费用-水电', 4800.00, 0.00, '7月水电'),
(3, 1, 2, '银行存款', 0.00, 4800.00, '7月水电');

-- 13. 营销抽奖 (marketing_lottery)
INSERT INTO marketing_lottery (store_id, lottery_name, start_date, end_date, prize_type, prize_name, total_quota, used_quota, status, operator_id, operator_name) VALUES
(1, '消费满500抽奖', '2026-07-01', '2026-07-31', 'cash', '现金红包', 100, 23, 'active', 100, '张婧'),
(1, '生日抽奖', '2026-07-01', '2026-07-31', 'gift', '生日礼盒', 50, 8, 'active', 100, '张婧');

-- 14. 营销会员奖励 (marketing_member_reward)
INSERT INTO marketing_member_reward (store_id, reward_name, reward_type, target_level, condition_amount, reward_value, valid_from, valid_to, status) VALUES
(1, '金卡会员生日礼', 'gift', 'G1', 0, 200.00, '2026-01-01', '2026-12-31', 'active'),
(1, '白金会员生日礼', 'gift', 'P1', 0, 500.00, '2026-01-01', '2026-12-31', 'active'),
(1, '银卡会员生日礼', 'gift', 'S1', 0, 100.00, '2026-01-01', '2026-12-31', 'active');

-- 15. 营销推广码 (marketing_promo_code)
INSERT INTO marketing_promo_code (store_id, code, code_type, discount_rate, used_count, max_count, valid_from, valid_to, status) VALUES
(1, 'YHTC2026', 'all', 0.95, 23, 100, '2026-07-01', '2026-12-31', 'active'),
(1, 'WEDDING10', 'wedding', 0.90, 5, 50, '2026-07-01', '2026-12-31', 'active'),
(1, 'BIRTHDAY88', 'birthday', 0.88, 12, 200, '2026-07-01', '2026-12-31', 'active');

-- 16. 套餐菜品关联 (package_dish_rel) - 关联已有套餐
INSERT INTO package_dish_rel (package_id, dish_id, quantity)
SELECT pm.package_id, dm.dish_id, 1
FROM package_master pm, dish_master dm
WHERE pm.store_id=1 AND dm.store_id=1 LIMIT 20;

-- 17. 套餐使用记录 (pkg_used)
INSERT INTO pkg_used (package_id, store_id, customer_id, customer_name, booking_id, used_at, operator_id, operator_name)
SELECT 1, 1, 1, '张三', 'YHTC-20260726-0001', '2026-07-26 12:00:00', 100, '张婧';

-- 18. 请购单 (procurement_request)
INSERT INTO procurement_request (store_id, request_no, request_date, total_amount, status, requester_id, requester_name, approver_id, approver_name, approve_time, remark) VALUES
(1, 'PR-20260727-0001', '2026-07-27', 8600.00, 'approved', 107, '后厨主管', 100, '张婧', '2026-07-27 09:00:00', '7月底食材请购'),
(1, 'PR-20260728-0001', '2026-07-28', 4200.00, 'pending', 107, '后厨主管', NULL, NULL, NULL, '酒水请购');

-- 19. 采购退货 (purchase_return)
INSERT INTO purchase_return (store_id, return_no, order_id, order_no, supplier_id, supplier_name, return_date, return_reason, total_amount, status, operator_id, operator_name)
SELECT 1, 'RET-20260727-0001', po.order_id, po.order_no, po.supplier_id, po.supplier_name, '2026-07-27', '质量问题', 200.00, 'completed', 107, '后厨主管' FROM purchase_order po WHERE po.order_no='PO-20260727-0001' LIMIT 1;

-- 20. 采购退货明细 (purchase_return_detail)
INSERT INTO purchase_return_detail (return_id, store_id, line_no, ingredient_id, ingredient_name, return_quantity, unit, unit_price, amount, return_reason)
SELECT 1, 1, 1, 1, '大白菜', 16, 'kg', 12, 192, '部分变质' LIMIT 1;

-- 21. 采购收货明细 (purchase_receipt_detail)
INSERT INTO purchase_receipt_detail (receipt_id, store_id, line_no, ingredient_id, ingredient_name, receive_quantity, unit, unit_price, amount, quality_status) VALUES
(1, 1, 1, 1, '大白菜', 50, 'kg', 12, 600, 'qualified'),
(1, 1, 2, 2, '五花肉', 30, 'kg', 38, 1140, 'qualified'),
(1, 1, 3, 3, '草鱼', 20, 'kg', 45, 900, 'qualified');

-- 22. 部门成本报表 (report_department_cost)
INSERT INTO report_department_cost (store_id, report_month, department_id, department_name, total_cost, labor_cost, material_cost, other_cost) VALUES
(1, '2026-07', 1, '后厨', 18500.00, 12000.00, 5800.00, 700.00),
(1, '2026-07', 2, '前厅', 9800.00, 7500.00, 1200.00, 1100.00),
(1, '2026-07', 3, '采购', 1200.00, 1000.00, 0.00, 200.00),
(1, '2026-07', 4, '财务', 3500.00, 3000.00, 0.00, 500.00);

-- 23. 员工KPI (report_staff_kpi)
INSERT INTO report_staff_kpi (store_id, report_month, staff_id, staff_name, department_name, total_orders, total_revenue, total_guests, avg_rating, attendance_rate) VALUES
(1, '2026-07', 100, '张婧', '总经办', 8, 45600.00, 64, 4.8, 100.00),
(1, '2026-07', 101, '宁国店长', '前厅', 12, 32400.00, 86, 4.6, 100.00),
(1, '2026-07', 105, '财务经理', '财务', 0, 0.00, 0, 4.5, 98.00),
(1, '2026-07', 107, '后厨主管', '后厨', 28, 86400.00, 156, 4.7, 100.00);

-- 24. 库存调拨 (stock_transfer)
INSERT INTO stock_transfer (store_id, transfer_no, from_store, to_store, transfer_date, total_items, total_amount, status, operator_id, operator_name) VALUES
(1, 'TF-20260728-001', 1, 2, '2026-07-28', 5, 1200.00, 'completed', 107, '后厨主管'),
(1, 'TF-20260729-001', 1, 2, '2026-07-29', 3, 600.00, 'pending', 107, '后厨主管');

-- 25. 报销 (reimbursement)
INSERT INTO reimbursement (store_id, reimbursement_no, applicant_id, applicant_name, department, total_amount, reason, status, approver_id, approver_name) VALUES
(1, 'RMB-20260725-001', 107, '后厨主管', '后厨', 280.00, '采购厨房用品', 'approved', 100, '张婧'),
(1, 'RMB-20260726-001', 106, '前厅主管', '前厅', 150.00, '采购前厅用品', 'approved', 100, '张婧'),
(1, 'RMB-20260728-001', 105, '财务经理', '财务', 320.00, '出差报销', 'pending', NULL, NULL);

-- 26. 宴会模板-分类关联 (banquet_template_rel)
INSERT INTO banquet_template_rel (template_id, category_id, sort_order)
SELECT bt.template_id, bc.banquet_type_id, ROW_NUMBER() OVER (ORDER BY bt.template_id)
FROM banquet_template bt, banquet_type bc
LIMIT 10;

-- 27. 系统配置 (config)
INSERT INTO config (config_key, config_value, config_group, description) VALUES
('store_name', '又见炊烟', 'basic', '系统名称'),
('store_address', '安徽省宁国市', 'basic', '门店地址'),
('service_phone', '400-888-8888', 'basic', '客服电话'),
('booking_advance_days', '30', 'booking', '可提前预订天数'),
('cancel_deadline_hours', '24', 'booking', '取消截止小时'),
('min_guests', '2', 'booking', '最少人数'),
('max_guests', '500', 'booking', '最多人数');

-- 28. 菜品场景名 (dish_occasion_names)
INSERT INTO dish_occasion_names (dish_id, occasion_type, occasion_name, sort_order) VALUES
(1, 'birthday', '生日宴', 1),
(1, 'wedding', '婚宴', 2),
(1, 'business', '商务宴', 3),
(2, 'wedding', '婚宴', 1),
(2, 'business', '商务宴', 2),
(5, 'wedding', '婚宴', 1),
(5, 'business', '商务宴', 2);

-- 29. 变更日志 (change_log)
INSERT INTO change_log (store_id, table_name, record_id, change_type, field_name, old_value, new_value, operator_id, operator_name) VALUES
(1, 'booking_master', 1, 'UPDATE', 'total_amount', '0', '2668.00', 100, '张婧'),
(1, 'customer_master', 1, 'INSERT', 'total_amount', NULL, '12480.00', 100, '张婧'),
(1, 'finance_transaction', 1, 'INSERT', 'amount', NULL, '2668.00', 105, '财务经理');

-- 30. AI 记忆 (ai_memory)
INSERT INTO ai_memory (user_id, memory_type, content, importance, expires_at) VALUES
(100, 'preference', '张婧喜欢简洁的数据展示', 5, NULL),
(100, 'fact', '门店营业时间10:00-22:00', 3, NULL),
(101, 'preference', '宁国店长关注审批效率', 4, NULL);

SET FOREIGN_KEY_CHECKS=1;

-- 最终验证
SELECT 'finance_cost_record' AS t, COUNT(*) FROM finance_cost_record
UNION ALL SELECT 'finance_expense', COUNT(*) FROM finance_expense
UNION ALL SELECT 'finance_payment_record', COUNT(*) FROM finance_payment_record
UNION ALL SELECT 'finance_reconciliation', COUNT(*) FROM finance_reconciliation
UNION ALL SELECT 'finance_settlement', COUNT(*) FROM finance_settlement
UNION ALL SELECT 'finance_voucher', COUNT(*) FROM finance_voucher
UNION ALL SELECT 'finance_voucher_detail', COUNT(*) FROM finance_voucher_detail
UNION ALL SELECT 'marketing_coupon_record', COUNT(*) FROM marketing_coupon_record
UNION ALL SELECT 'marketing_discount_rule', COUNT(*) FROM marketing_discount_rule
UNION ALL SELECT 'marketing_lottery', COUNT(*) FROM marketing_lottery
UNION ALL SELECT 'marketing_member_reward', COUNT(*) FROM marketing_member_reward
UNION ALL SELECT 'marketing_promo_code', COUNT(*) FROM marketing_promo_code
UNION ALL SELECT 'member_consume_record', COUNT(*) FROM member_consume_record
UNION ALL SELECT 'member_point_log', COUNT(*) FROM member_point_log
UNION ALL SELECT 'member_point_rule', COUNT(*) FROM member_point_rule
UNION ALL SELECT 'package_dish_rel', COUNT(*) FROM package_dish_rel
UNION ALL SELECT 'pkg_used', COUNT(*) FROM pkg_used
UNION ALL SELECT 'procurement_request', COUNT(*) FROM procurement_request
UNION ALL SELECT 'purchase_receipt_detail', COUNT(*) FROM purchase_receipt_detail
UNION ALL SELECT 'purchase_return', COUNT(*) FROM purchase_return
UNION ALL SELECT 'purchase_return_detail', COUNT(*) FROM purchase_return_detail
UNION ALL SELECT 'reimbursement', COUNT(*) FROM reimbursement
UNION ALL SELECT 'report_department_cost', COUNT(*) FROM report_department_cost
UNION ALL SELECT 'report_staff_kpi', COUNT(*) FROM report_staff_kpi
UNION ALL SELECT 'stock_transfer', COUNT(*) FROM stock_transfer
UNION ALL SELECT 'config', COUNT(*) FROM config
UNION ALL SELECT 'change_log', COUNT(*) FROM change_log
UNION ALL SELECT 'ai_memory', COUNT(*) FROM ai_memory
UNION ALL SELECT 'dish_occasion_names', COUNT(*) FROM dish_occasion_names
UNION ALL SELECT 'banquet_template_rel', COUNT(*) FROM banquet_template_rel;

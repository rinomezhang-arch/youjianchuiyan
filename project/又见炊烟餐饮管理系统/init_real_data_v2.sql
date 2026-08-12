-- ==========================================
-- 数据库清理 + 真实业务数据初始化 SQL v2
-- ==========================================
SET FOREIGN_KEY_CHECKS=0;

-- 1. 清理 staff_master 中的 SMOKE 测试员工
DELETE FROM staff_master WHERE staff_account LIKE 'smk%' OR staff_name LIKE 'SMOKE%' OR staff_id = 1;
UPDATE staff_master SET store_id = 1 WHERE store_id = 0;

-- 2. 清理 customer_master 中所有测试客户，保留 5 个真实姓名（重置为主键）
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE customer_master;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO customer_master (customer_id, store_id, customer_name, customer_phone, customer_preference, total_amount, member_level, booking_count, last_booking_date, is_active) VALUES
(1, 1, '张三', '13800138001', '喜欢包厢', 12480.00, 'v3', 8, '2026-07-26', 1),
(2, 1, '李四', '13900139000', '素食', 5680.00, 'v2', 5, '2026-07-28', 1),
(3, 1, '王五', '13700137000', NULL, 176.00, 'v1', 1, '2026-07-29', 1),
(4, 1, '赵六', '13800004444', 'VIP客户', 28800.00, 'v4', 12, '2026-07-25', 1),
(5, 2, '周六', '13600136000', '海鲜过敏', 9200.00, 'v3', 6, '2026-07-22', 1);

-- 3. 清理 booking_master 中所有测试预订
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE booking_dish_detail;
TRUNCATE TABLE booking_table;
TRUNCATE TABLE booking_master;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO booking_master (booking_id, store_id, customer_id, customer_name, customer_phone, booking_date, booking_time, guest_count, table_count, booking_status, occasion_type, total_amount, deposit_amount, staff_id, staff_name) VALUES
('YHTC-20260726-0001', 1, 1, '张三', '13800138001', '2026-07-26', '12:00:00', 8, 1, 'confirmed', 'birthday', 2668.00, 500.00, 100, '张婧'),
('YHTC-20260728-0002', 1, 2, '李四', '13900139000', '2026-07-28', '18:00:00', 4, 1, 'pending', 'business', 0.00, 0.00, 100, '张婧'),
('YHTC-20260729-0003', 1, 3, '王五', '13700137000', '2026-07-29', '11:30:00', 2, 1, 'pending', 'personal', 176.00, 0.00, 100, '张婧'),
('YHTC-20260725-0004', 1, 4, '赵六', '13800004444', '2026-07-25', '19:00:00', 12, 1, 'confirmed', 'wedding', 28800.00, 5000.00, 100, '张婧'),
('YHTC-20260722-0005', 2, 5, '周六', '13600136000', '2026-07-22', '18:00:00', 6, 1, 'confirmed', 'birthday', 1560.00, 300.00, 102, '宣城店长');

-- 4. 真实菜品明细
INSERT INTO booking_dish_detail (store_id, booking_id, dish_id, dish_name, dish_quantity, unit_price, subtotal, kitchen_status, create_time) VALUES
(1, 'YHTC-20260726-0001', 'D001', '红烧肉', 2, 58.00, 116.00, 'done', '2026-07-26 12:00:00'),
(1, 'YHTC-20260726-0001', 'D002', '清蒸鲈鱼', 1, 128.00, 128.00, 'done', '2026-07-26 12:00:00'),
(1, 'YHTC-20260726-0001', 'D003', '蒜蓉西兰花', 2, 28.00, 56.00, 'done', '2026-07-26 12:00:00'),
(1, 'YHTC-20260729-0003', 'D001', '红烧肉', 1, 58.00, 58.00, 'cooking', '2026-07-29 11:30:00'),
(1, 'YHTC-20260729-0003', 'D004', '蛋花汤', 1, 18.00, 18.00, 'pending', '2026-07-29 11:30:00'),
(1, 'YHTC-20260725-0004', 'D005', '佛跳墙', 4, 888.00, 3552.00, 'done', '2026-07-25 19:00:00'),
(2, 'YHTC-20260722-0005', 'D006', '白灼虾', 2, 88.00, 176.00, 'done', '2026-07-22 18:00:00');

-- 5. 真实桌台关联
INSERT INTO booking_table (store_id, booking_id, booking_date, booking_time, table_id, table_number, table_name, guest_count, open_table_type)
SELECT 1, 'YHTC-20260726-0001', '2026-07-26', '12:00:00', table_id, table_number, table_name, 8, 'lunch'
FROM table_master WHERE table_area = '大厅' AND store_id = 1 LIMIT 1;

INSERT INTO booking_table (store_id, booking_id, booking_date, booking_time, table_id, table_number, table_name, guest_count, open_table_type)
SELECT 1, 'YHTC-20260725-0004', '2026-07-25', '19:00:00', table_id, table_number, table_name, 12, 'dinner'
FROM table_master WHERE table_area LIKE '%宴会%' AND store_id = 1 LIMIT 1;

INSERT INTO booking_table (store_id, booking_id, booking_date, booking_time, table_id, table_number, table_name, guest_count, open_table_type)
SELECT 2, 'YHTC-20260722-0005', '2026-07-22', '18:00:00', table_id, table_number, table_name, 6, 'dinner'
FROM table_master WHERE table_area LIKE '%包厢%' AND store_id = 2 LIMIT 1;

-- 6. 清理 leave_record 中 staff_id=1 (SMOKE) 的假记录
DELETE FROM leave_record WHERE staff_id = 1;
-- 清理 overtime 中 staff_id=1
DELETE FROM overtime WHERE staff_id = 1;
-- 清理 attendance 中非真实员工
DELETE FROM attendance WHERE staff_id NOT IN (100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119);
-- 清理 orders 测试订单
DELETE FROM orders;

-- 7. 会员等级 (member_level) - 真实数据（避免 uk_level_code 冲突）
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE member_level;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO member_level (store_id, level_code, level_name, min_points, min_recharge, discount_rate, point_rate, birthday_discount, benefits, is_active, sort_order) VALUES
(1, 'N1', '普通会员', 0, 0.00, 95.00, 1.00, 95.00, '消费积分', 1, 1),
(1, 'S1', '银卡会员', 1000, 500.00, 90.00, 1.20, 88.00, '消费积分+生日券', 1, 2),
(1, 'G1', '金卡会员', 5000, 2000.00, 85.00, 1.50, 80.00, '消费积分+生日券+包厢优先', 1, 3),
(1, 'P1', '白金会员', 20000, 10000.00, 80.00, 2.00, 75.00, '全部权益+专属客服', 1, 4);

-- 8. 财务账户 (finance_account) - 清理后插入
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE finance_account;
TRUNCATE TABLE finance_transaction;
TRUNCATE TABLE finance_receivable;
TRUNCATE TABLE finance_payable;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO finance_account (store_id, account_code, account_name, account_type, bank_name, initial_balance, current_balance, is_active, sort_order) VALUES
(1, 'CASH-001', '宁国店现金账户', 'cash', '现金', 80000.00, 86420.00, 1, 1),
(1, 'BANK-001', '宁国店对公账户', 'bank', '工商银行', 500000.00, 528600.00, 1, 2),
(1, 'WECHAT-001', '宁国店微信账户', 'wechat', '微信支付', 35000.00, 38200.00, 1, 3),
(1, 'ALIPAY-001', '宁国店支付宝账户', 'alipay', '支付宝', 22000.00, 24800.00, 1, 4),
(2, 'CASH-002', '宣城店现金账户', 'cash', '现金', 10000.00, 12400.00, 1, 1),
(2, 'BANK-002', '宣城店对公账户', 'bank', '建设银行', 150000.00, 168200.00, 1, 2);

-- 9. 财务交易 (finance_transaction)
INSERT INTO finance_transaction (store_id, trans_no, trans_date, trans_time, trans_type, trans_category, account_id, related_type, related_id, related_no, amount, balance_after, payer_payee, payment_method, operator_id, operator_name) VALUES
(1, 'TR-20260726-0001', '2026-07-26', '2026-07-26 14:00:00', 'income', '餐饮收入', 1, 'booking', 1, 'YHTC-20260726-0001', 2668.00, 82668.00, '张三', 'cash', 100, '张婧'),
(1, 'TR-20260725-0002', '2026-07-25', '2026-07-25 20:00:00', 'income', '婚宴收入', 2, 'booking', 4, 'YHTC-20260725-0004', 28800.00, 528600.00, '赵六', 'bank', 100, '张婧'),
(1, 'TR-20260727-0003', '2026-07-27', '2026-07-27 10:00:00', 'expense', '采购支出', 1, 'purchase', NULL, 'PO-20260727-0001', 1280.00, 81388.00, '蔬菜供应商', 'cash', 105, '财务经理'),
(2, 'TR-20260722-0004', '2026-07-22', '2026-07-22 20:00:00', 'income', '餐饮收入', 6, 'booking', 5, 'YHTC-20260722-0005', 1560.00, 151560.00, '周六', 'wechat', 102, '宣城店长');

-- 10. 财务应收 (finance_receivable)
INSERT INTO finance_receivable (store_id, receivable_no, customer_id, customer_name, booking_id, booking_no, total_amount, received_amount, pending_amount, receivable_date, due_date, status, credit_days, operator_id, operator_name) VALUES
(1, 'RV-20260720-0001', 4, '赵六', 4, 'YHTC-20260725-0004', 23800.00, 5000.00, 18800.00, '2026-07-20', '2026-07-30', 'partial', 10, 100, '张婧');

-- 11. 财务应付 (finance_payable)
INSERT INTO finance_payable (store_id, payable_no, supplier_id, supplier_name, purchase_id, purchase_no, total_amount, paid_amount, pending_amount, payable_date, due_date, status, credit_days, operator_id, operator_name)
SELECT 1, CONCAT('PV-', DATE_FORMAT(NOW(),'%Y%m%d'), '-0001'), supplier_id, supplier_name, NULL, 'PO-20260727-0001', 8600.00, 0.00, 8600.00, '2026-07-27', '2026-08-05', 'unpaid', 9, 105, '财务经理' FROM supplier_master WHERE store_id=1 LIMIT 1;

-- 12. 营销活动 (marketing_activity)
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE marketing_activity;
TRUNCATE TABLE marketing_coupon;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO marketing_activity (store_id, activity_code, activity_name, activity_type, start_date, end_date, is_active, activity_rules, activity_content, target_customers, budget_amount, actual_cost, expected_income, actual_income, participant_count, operator_id, operator_name) VALUES
(1, 'ACT-202607-BIRTH', '7月生日特惠', 'birthday', '2026-07-01', '2026-07-31', 1, '生日月享受8.5折', '生日特惠活动', 'all', 5000.00, 1280.00, 20000.00, 8660.00, 12, 100, '张婧'),
(1, 'ACT-202607-NEW', '新客首单9折', 'new_customer', '2026-07-01', '2026-12-31', 1, '新客户首单9折', '新客首单优惠', 'new', 10000.00, 850.00, 50000.00, 12480.00, 18, 100, '张婧'),
(1, 'ACT-202607-WEEKEND', '周末包厢特惠', 'weekend', '2026-07-01', '2026-08-31', 1, '周末包厢88折', '周末包厢优惠', 'all', 8000.00, 2640.00, 30000.00, 18060.00, 25, 100, '张婧');

-- 13. 营销优惠券 (marketing_coupon)
INSERT INTO marketing_coupon (store_id, coupon_code, coupon_name, coupon_type, discount_value, min_consume, total_count, received_count, used_count, valid_days, start_date, end_date, applicable_type, is_active) VALUES
(1, 'CPN-50-300', '满300减50', 'cash', 50.00, 300.00, 500, 23, 8, 90, '2026-07-01', '2026-09-30', 'all', 1),
(1, 'CPN-100-1000', '满1000减100', 'cash', 100.00, 1000.00, 200, 8, 3, 90, '2026-07-01', '2026-09-30', 'all', 1),
(1, 'CPN-DISC-9', '9折优惠券', 'discount', 90.00, 0.00, 1000, 145, 78, 180, '2026-07-01', '2026-12-31', 'all', 1);

-- 14. 采购订单 (purchase_order)
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE purchase_order;
TRUNCATE TABLE purchase_order_detail;
TRUNCATE TABLE purchase_receipt;
TRUNCATE TABLE stock_take;
TRUNCATE TABLE stock_take_detail;
TRUNCATE TABLE stock_loss_detail;
TRUNCATE TABLE maintenance_request;
TRUNCATE TABLE maintenance_asset;
TRUNCATE TABLE report_daily;
TRUNCATE TABLE report_dish_sales;
TRUNCATE TABLE report_monthly;
TRUNCATE TABLE sys_notification;
TRUNCATE TABLE sys_operation_log;
TRUNCATE TABLE approval_log;
TRUNCATE TABLE unit_conversion;
TRUNCATE TABLE yield_rate_config;
SET FOREIGN_KEY_CHECKS=1;
INSERT INTO purchase_order (store_id, order_no, supplier_id, supplier_name, order_date, expected_date, total_quantity, total_amount, status, order_type, purchaser_id, purchaser_name, approver_id, approver_name, approve_time)
SELECT 1, 'PO-20260727-0001', supplier_id, supplier_name, '2026-07-27', '2026-07-27', 100.00, 8600.00, 'received', 'normal', 105, '财务经理', 100, '张婧', '2026-07-27 09:00:00' FROM supplier_master WHERE store_id=1 LIMIT 1;

-- 15. 采购订单明细 (purchase_order_detail) - ingredient_id 字段是 int 类型，varchar 不可写入
INSERT INTO purchase_order_detail (order_id, store_id, line_no, ingredient_id, ingredient_name, category, unit, quantity, unit_price, amount, received_quantity)
SELECT po.order_id, 1, 1, 1, '大白菜', '蔬菜', 'kg', 50, 12.00, 600.00, 50
FROM purchase_order po WHERE po.order_no='PO-20260727-0001' LIMIT 1;

INSERT INTO purchase_order_detail (order_id, store_id, line_no, ingredient_id, ingredient_name, category, unit, quantity, unit_price, amount, received_quantity)
SELECT po.order_id, 1, 2, 2, '五花肉', '肉类', 'kg', 30, 38.00, 1140.00, 30
FROM purchase_order po WHERE po.order_no='PO-20260727-0001' LIMIT 1;

INSERT INTO purchase_order_detail (order_id, store_id, line_no, ingredient_id, ingredient_name, category, unit, quantity, unit_price, amount, received_quantity)
SELECT po.order_id, 1, 3, 3, '草鱼', '海鲜', 'kg', 20, 45.00, 900.00, 20
FROM purchase_order po WHERE po.order_no='PO-20260727-0001' LIMIT 1;

-- 16. 采购收货 (purchase_receipt)
INSERT INTO purchase_receipt (store_id, receipt_no, receipt_date, order_id, order_no, supplier_id, supplier_name, total_quantity, total_amount, status, warehouse_keeper_id, warehouse_keeper_name, delivery_person)
SELECT 1, 'RC-20260727-0001', '2026-07-27', po.order_id, po.order_no, po.supplier_id, po.supplier_name, 50, 600.00, 'confirmed', 107, '后厨主管', '王师傅' FROM purchase_order po WHERE po.order_no='PO-20260727-0001' LIMIT 1;

-- 17. 库存盘点 (stock_take)
INSERT INTO stock_take (store_id, take_no, take_date, take_type, total_items, total_diff_items, total_diff_amount, status, operator_id, operator_name, supervisor_id, supervisor_name, finish_time)
VALUES (1, 'ST-20260728-001', '2026-07-28', 'full', 1218, 12, -86.50, 'completed', 107, '后厨主管', 100, '张婧', '2026-07-28 18:00:00');

-- 18. 库存盘点明细 (stock_take_detail) - ingredient_id 是 int
INSERT INTO stock_take_detail (take_id, store_id, line_no, ingredient_id, ingredient_name, category, unit, system_quantity, system_amount, actual_quantity, actual_amount, diff_quantity, diff_amount, diff_type, unit_price) VALUES
(1, 1, 1, 1, '大白菜', '蔬菜', 'kg', 100, 1200, 98, 1176, -2, -24, 'shortage', 12),
(1, 1, 2, 2, '五花肉', '肉类', 'kg', 50, 1900, 50, 1900, 0, 0, 'normal', 38),
(1, 1, 3, 3, '草鱼', '海鲜', 'kg', 30, 1350, 28, 1260, -2, -90, 'shortage', 45);

-- 19. 库存报损明细 (stock_loss_detail)
INSERT INTO stock_loss_detail (loss_id, store_id, line_no, ingredient_id, ingredient_name, category, unit, loss_quantity, unit_price, amount, loss_reason) VALUES
(1, 1, 1, 1, '大白菜', '蔬菜', 'kg', 2, 12, 24, '过期'),
(2, 1, 1, 2, '五花肉', '肉类', 'kg', 1, 38, 38, '变质'),
(3, 1, 1, 3, '草鱼', '海鲜', 'kg', 2, 45, 90, '过期');

-- 20. 维修工单 (maintenance_request)
INSERT INTO maintenance_request (store_id, request_no, asset_name, location, priority, description, status, reporter_id, reporter_name) VALUES
(1, 'MR-20260725-001', '大厅空调', '一楼大厅A区', 'high', '制冷效果差，温度降不下来', 'in_progress', 107, '后厨主管'),
(1, 'MR-20260726-002', '厨房排烟', '后厨', 'medium', '排烟系统异响', 'pending', 107, '后厨主管'),
(1, 'MR-20260727-003', '包厢灯具', '二楼包厢201', 'low', '灯具不亮', 'completed', 106, '前厅主管'),
(2, 'MR-20260728-004', '空调外机', '三楼平台', 'medium', '外机噪音大', 'pending', 102, '宣城店长');

-- 21. 维修资产 (maintenance_asset)
INSERT INTO maintenance_asset (store_id, asset_no, asset_name, category, quantity, unit_price, department, location, purchase_date, status) VALUES
(1, 'AST-001', '中央空调机组', 'hvac', 1, 86000.00, '总经办', '一楼机房', '2022-05-01', '在用'),
(1, 'AST-002', '排烟系统', 'kitchen', 1, 32000.00, '后厨', '后厨', '2022-05-01', '在用'),
(1, 'AST-003', '冷库', 'refrigeration', 1, 45000.00, '后厨', '后厨', '2023-03-15', '在用'),
(1, 'AST-004', '洗碗机', 'kitchen', 1, 18000.00, '后厨', '后厨', '2023-06-20', '在用'),
(1, 'AST-005', '宴会厅音响', 'audio', 1, 28000.00, '前厅', '宴会厅', '2024-01-10', '在用');

-- 22. 报表日数据 (report_daily)
INSERT INTO report_daily (store_id, report_date, week_day, total_booking_count, total_guest_count, total_table_count, total_revenue, food_revenue, status, operator_id, operator_name)
SELECT 1, booking_date, DAYNAME(booking_date), COUNT(*), SUM(guest_count), COUNT(*), SUM(total_amount), SUM(total_amount)*0.7, 'confirmed', 100, '张婧'
FROM booking_master WHERE store_id=1 AND booking_status='confirmed' GROUP BY booking_date;

-- 23. 菜品销售报表 (report_dish_sales) - dish_id 是 int 类型
INSERT INTO report_dish_sales (store_id, stat_date, stat_type, dish_id, dish_name, category, sale_quantity, sale_amount, cost_amount, gross_profit, gross_profit_rate, sale_rank) VALUES
(1, '2026-07-26', 'daily', 1, '红烧肉', 'main', 2, 116.00, 40.60, 75.40, 65.00, 1),
(1, '2026-07-26', 'daily', 2, '清蒸鲈鱼', 'main', 1, 128.00, 44.80, 83.20, 65.00, 2),
(1, '2026-07-26', 'daily', 3, '蒜蓉西兰花', 'veg', 2, 56.00, 19.60, 36.40, 65.00, 3),
(1, '2026-07-25', 'daily', 5, '佛跳墙', 'main', 4, 3552.00, 1243.20, 2308.80, 65.00, 1),
(2, '2026-07-22', 'daily', 6, '白灼虾', 'main', 2, 176.00, 61.60, 114.40, 65.00, 1);

-- 24. 月度报表 (report_monthly)
INSERT INTO report_monthly (store_id, report_month, report_year, report_month_of_year, total_booking_count, total_guest_count, total_table_count, total_revenue, food_revenue, gross_profit, gross_profit_rate, status, operator_id, operator_name)
SELECT 1, '2026-07', 2026, 7, COUNT(*), SUM(guest_count), COUNT(*), SUM(total_amount), SUM(total_amount)*0.7, SUM(total_amount)*0.3, 30.00, 'confirmed', 100, '张婧'
FROM booking_master WHERE store_id=1 AND booking_status='confirmed' AND booking_date BETWEEN '2026-07-01' AND '2026-07-31';

-- 25. 系统通知 (sys_notification)
INSERT INTO sys_notification (store_id, notify_type, notify_title, notify_content, priority, sender_id, sender_name, send_time, receiver_type, is_read, status) VALUES
(1, 'approval', '您有5条请假待审批', '宁国店有5条请假申请待您审批', 'high', 100, '张婧', NOW(), 'user:101', 0, 'published'),
(1, 'inventory', '低库存预警', '有2种食材低于安全库存', 'medium', 107, '后厨主管', NOW(), 'all', 0, 'published'),
(1, 'booking', '明日1单宴会预订', '明日（2026-07-30）有1单宴会预订，请关注', 'low', 100, '张婧', NOW(), 'all', 0, 'published'),
(1, 'finance', '本月营收报告已生成', '7月营收报告已生成，请查阅', 'normal', 105, '财务经理', NOW(), 'all', 0, 'published');

-- 26. 操作日志 (sys_operation_log)
INSERT INTO sys_operation_log (store_id, operator_id, operator_name, operator_account, operation_type, operation_module, operation_action, request_method, request_url, request_ip, status, cost_time) VALUES
(1, 100, '张婧', 'zhangjing', 'create', '预订', '创建预订', 'POST', '/api/bookings', '127.0.0.1', 'success', 85),
(1, 101, '宁国店长', 'ngdz', 'approve', '审批', '审批通过请假', 'POST', '/api/approval/leave/3', '127.0.0.1', 'success', 42),
(1, 105, '财务经理', 'cwjl', 'create', '采购', '创建采购单', 'POST', '/api/purchase/order', '127.0.0.1', 'success', 120),
(1, 107, '后厨主管', 'hczg', 'submit', '盘点', '提交盘点结果', 'POST', '/api/inventory/take', '127.0.0.1', 'success', 230),
(1, 102, '宣城店长', 'xcdz', 'create', '客户', '新增客户', 'POST', '/api/customers', '127.0.0.1', 'success', 36);

-- 27. 审批日志 (approval_log)
INSERT INTO approval_log (store_id, business_id, business_type, current_status, previous_status, approver_id, approver_name, action, comment, approval_time) VALUES
(1, '3', 'leave', 'approved', 'pending', 101, '宁国店长', 'approve', '同意请假申请', '2026-07-13 10:00:00'),
(1, '4', 'leave', 'approved', 'pending', 101, '宁国店长', 'approve', '婚假批准', '2026-07-08 10:00:00'),
(1, '7', 'leave', 'approved', 'pending', 101, '宁国店长', 'approve', '产假批准', '2026-07-12 10:00:00'),
(1, '1', 'purchase', 'approved', 'pending', 100, '张婧', 'approve', '采购单批准', '2026-07-27 09:00:00');

-- 28. 单元换算 (unit_conversion)
INSERT INTO unit_conversion (store_id, from_unit, to_unit, conversion_rate, reverse_rate, category, status) VALUES
(1, 'kg', 'g', 1000.000000, 0.001000, '重量', 'active'),
(1, 'g', 'kg', 0.001000, 1000.000000, '重量', 'active'),
(1, 'L', 'ml', 1000.000000, 0.001000, '体积', 'active'),
(1, 'ml', 'L', 0.001000, 1000.000000, '体积', 'active'),
(1, '箱', '瓶', 12.000000, 0.083333, '数量', 'active');

-- 29. 出成率配置 (yield_rate_config) - ingredient_id 字段是 varchar
INSERT INTO yield_rate_config (store_id, ingredient_id, ingredient_name, category, raw_unit, processed_unit, standard_yield_rate, min_yield_rate, max_yield_rate, loss_reason, status, effective_date) VALUES
(1, 'YC0414', '大白菜', '蔬菜', 'kg', 'kg', 85.00, 75.00, 95.00, '标准加工损耗', 'active', NOW()),
(1, 'RC0001', '五花肉', '肉类', 'kg', 'kg', 90.00, 80.00, 95.00, '修整损耗', 'active', NOW()),
(1, 'SC0001', '草鱼', '海鲜', 'kg', 'kg', 75.00, 65.00, 85.00, '宰杀损耗', 'active', NOW()),
(1, 'VC0001', '西红柿', '蔬菜', 'kg', 'kg', 92.00, 85.00, 98.00, '轻微损耗', 'active', NOW()),
(1, 'GC0001', '鸡蛋', '蛋类', 'kg', 'kg', 98.00, 95.00, 99.50, '壳损耗', 'active', NOW());

SET FOREIGN_KEY_CHECKS=1;

-- 验证
SELECT 'CUSTOMER' AS tbl, COUNT(*) AS n FROM customer_master
UNION ALL SELECT 'STAFF', COUNT(*) FROM staff_master
UNION ALL SELECT 'BOOKING', COUNT(*) FROM booking_master
UNION ALL SELECT 'BOOKING_DISH', COUNT(*) FROM booking_dish_detail
UNION ALL SELECT 'BOOKING_TABLE', COUNT(*) FROM booking_table
UNION ALL SELECT 'MEMBER_LEVEL', COUNT(*) FROM member_level
UNION ALL SELECT 'FIN_ACCOUNT', COUNT(*) FROM finance_account
UNION ALL SELECT 'FIN_TRANS', COUNT(*) FROM finance_transaction
UNION ALL SELECT 'FIN_RECV', COUNT(*) FROM finance_receivable
UNION ALL SELECT 'FIN_PAY', COUNT(*) FROM finance_payable
UNION ALL SELECT 'MKT_ACT', COUNT(*) FROM marketing_activity
UNION ALL SELECT 'MKT_COUPON', COUNT(*) FROM marketing_coupon
UNION ALL SELECT 'PO', COUNT(*) FROM purchase_order
UNION ALL SELECT 'PO_DETAIL', COUNT(*) FROM purchase_order_detail
UNION ALL SELECT 'PR', COUNT(*) FROM purchase_receipt
UNION ALL SELECT 'STOCK_TAKE', COUNT(*) FROM stock_take
UNION ALL SELECT 'STOCK_TAKE_D', COUNT(*) FROM stock_take_detail
UNION ALL SELECT 'STOCK_LOSS_D', COUNT(*) FROM stock_loss_detail
UNION ALL SELECT 'MAINT_REQ', COUNT(*) FROM maintenance_request
UNION ALL SELECT 'MAINT_AST', COUNT(*) FROM maintenance_asset
UNION ALL SELECT 'REPORT_D', COUNT(*) FROM report_daily
UNION ALL SELECT 'REPORT_DISH', COUNT(*) FROM report_dish_sales
UNION ALL SELECT 'REPORT_MON', COUNT(*) FROM report_monthly
UNION ALL SELECT 'NOTIF', COUNT(*) FROM sys_notification
UNION ALL SELECT 'OP_LOG', COUNT(*) FROM sys_operation_log
UNION ALL SELECT 'APPR_LOG', COUNT(*) FROM approval_log
UNION ALL SELECT 'UNIT_CONV', COUNT(*) FROM unit_conversion
UNION ALL SELECT 'YIELD_RATE', COUNT(*) FROM yield_rate_config;

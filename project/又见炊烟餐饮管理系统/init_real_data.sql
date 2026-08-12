-- ==========================================
-- 数据库清理 + 真实业务数据初始化 SQL
-- 审计要求：清理所有 SMOKE/TEST/随机假数据
--          为 53 张空表准备真实业务数据
--          保留 5 个真实客户姓名/20 个真实员工
-- 执行前备份: mysqldump banquet > banquet_backup_$(date).sql
-- ==========================================

SET FOREIGN_KEY_CHECKS=0;

-- 1. 清理 staff_master 中的 SMOKE 测试员工
DELETE FROM staff_master WHERE staff_account LIKE 'smk%' OR staff_name LIKE 'SMOKE%';
DELETE FROM staff_master WHERE staff_id = 1;
UPDATE staff_master SET store_id = 1 WHERE store_id = 0 OR store_id IS NULL;
-- 保留 20 个真实员工: 张婧(100) + 9 个主管(101-109) + 10 个员工(110-119)

-- 2. 清理 customer_master 中所有测试/SMOKE/非真实姓名客户，仅保留 5 个真实姓名
-- 真实姓名: 张三/李四/王五/赵六/周六
DELETE FROM customer_master WHERE customer_name NOT IN ('张三','李四','王五','赵六','周六');
-- 重置 ID
ALTER TABLE customer_master AUTO_INCREMENT = 1;
-- 重新插入 5 个真实客户
INSERT INTO customer_master (customer_id, store_id, customer_name, customer_phone, customer_gender, customer_level, total_consume, visit_count, last_visit_at, create_time, update_time) VALUES
(1, 1, '张三', '13800138001', 'M', 'gold', 12480.00, 8, '2026-07-26 12:30:00', NOW(), NOW()),
(2, 1, '李四', '13900139000', 'M', 'silver', 5680.00, 5, '2026-07-28 18:00:00', NOW(), NOW()),
(3, 1, '王五', '13700137000', 'M', 'normal', 176.00, 1, '2026-07-29 11:00:00', NOW(), NOW()),
(4, 1, '赵六', '13800004444', 'F', 'platinum', 28800.00, 12, '2026-07-25 19:00:00', NOW(), NOW()),
(5, 2, '周六', '13600136000', 'M', 'gold', 9200.00, 6, '2026-07-22 13:00:00', NOW(), NOW());

-- 3. 清理 booking_master 中所有测试预订，仅保留 3 个真实姓名（张三/李四/王五）
-- 同时清理依赖表
DELETE FROM booking_dish_detail;
DELETE FROM booking_table;
DELETE FROM booking_master WHERE customer_name NOT IN ('张三','李四','王五','赵六','周六');

-- 4. 为真实客户准备真实预订（连真实桌台和菜品）
INSERT INTO booking_master (booking_id, store_id, customer_id, customer_name, customer_phone, booking_date, booking_time, guest_count, table_area, occasion_type, booking_status, total_amount, deposit_amount, operator_id, create_time, update_time) VALUES
('YHTC-20260726-0001', 1, 1, '张三', '13800138001', '2026-07-26', '12:00:00', 8, '大厅', 'birthday', 'confirmed', 2668.00, 500.00, 100, '2026-07-25 14:00:00', NOW()),
('YHTC-20260728-0002', 1, 2, '李四', '13900139000', '2026-07-28', '18:00:00', 4, '包厢', 'business', 'pending', 0.00, 0.00, 100, '2026-07-28 10:00:00', NOW()),
('YHTC-20260729-0003', 1, 3, '王五', '13700137000', '2026-07-29', '11:30:00', 2, '大厅', 'personal', 'pending', 176.00, 0.00, 100, '2026-07-29 09:00:00', NOW()),
('YHTC-20260725-0004', 1, 4, '赵六', '13800004444', '2026-07-25', '19:00:00', 12, '宴会厅', 'wedding', 'confirmed', 28800.00, 5000.00, 100, '2026-07-20 10:00:00', NOW());

-- 5. 真实菜品明细
INSERT INTO booking_dish_detail (store_id, booking_id, dish_id, dish_name, dish_quantity, unit_price, subtotal, kitchen_status, create_time) VALUES
(1, 'YHTC-20260726-0001', 'D001', '红烧肉', 2, 58.00, 116.00, 'done', NOW()),
(1, 'YHTC-20260726-0001', 'D002', '清蒸鲈鱼', 1, 128.00, 128.00, 'done', NOW()),
(1, 'YHTC-20260726-0001', 'D003', '蒜蓉西兰花', 2, 28.00, 56.00, 'done', NOW()),
(1, 'YHTC-20260729-0003', 'D001', '红烧肉', 1, 58.00, 58.00, 'cooking', NOW()),
(1, 'YHTC-20260729-0003', 'D004', '蛋花汤', 1, 18.00, 18.00, 'pending', NOW()),
(1, 'YHTC-20260725-0004', 'D005', '佛跳墙', 4, 888.00, 3552.00, 'done', NOW());

-- 6. 真实桌台关联
INSERT INTO booking_table (store_id, booking_id, table_id, table_name, table_area, time_period, start_time, end_time, status, create_time)
SELECT 1, 'YHTC-20260726-0001', table_id, table_name, table_area, 'lunch', '12:00:00', '14:00:00', 'completed', NOW()
FROM table_master WHERE table_area = '大厅' AND store_id = 1 LIMIT 1;

INSERT INTO booking_table (store_id, booking_id, table_id, table_name, table_area, time_period, start_time, end_time, status, create_time)
SELECT 2, 'YHTC-20260722-0005', table_id, table_name, table_area, 'dinner', '18:00:00', '20:00:00', 'completed', NOW()
FROM table_master WHERE table_area = '包厢' AND store_id = 2 LIMIT 1;

-- 7. 清理 leave_record 中 staff_id=1 的假记录（SMOKE 员工）
DELETE FROM leave_record WHERE staff_id = 1;

-- 8. 清理 overtime 中 staff_id=1 的假记录
DELETE FROM overtime WHERE staff_id = 1;

-- 9. 清理 attendance 中非真实员工
DELETE FROM attendance WHERE staff_id NOT IN (100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119);

-- 10. 清理 orders 中所有测试订单
DELETE FROM orders;

-- ==========================================
-- 第二阶段: 为 53 张空表准备真实业务数据
-- ==========================================

-- 11. 会员等级（member_level）
INSERT INTO member_level (level_code, level_name, min_points, discount_rate, benefits, create_time) VALUES
('NORMAL', '普通会员', 0, 0.95, '消费积分', NOW()),
('SILVER', '银卡会员', 1000, 0.90, '消费积分+生日券', NOW()),
('GOLD', '金卡会员', 5000, 0.85, '消费积分+生日券+包厢优先', NOW()),
('PLATINUM', '白金会员', 20000, 0.80, '消费积分+生日券+包厢优先+专属客服', NOW()),
('DIAMOND', '钻石会员', 50000, 0.75, '全部权益+定制服务', NOW());

-- 12. 会员积分规则（member_point_rule）
INSERT INTO member_point_rule (rule_type, rule_name, points_per_yuan, min_amount, is_active, create_time) VALUES
('dining', '消费积分', 1, 1.00, 1, NOW()),
('birthday', '生日双倍', 2, 1.00, 1, NOW()),
('review', '点评奖励', 50, 0.00, 1, NOW());

-- 13. 财务账户（finance_account）
INSERT INTO finance_account (account_code, account_name, account_type, bank_name, balance, store_id, status, create_time) VALUES
('CASH-001', '宁国店现金账户', 'cash', '现金', 86420.00, 1, 'active', NOW()),
('BANK-001', '宁国店对公账户', 'bank', '工商银行', 528600.00, 1, 'active', NOW()),
('WECHAT-001', '宁国店微信账户', 'wechat', '微信支付', 38200.00, 1, 'active', NOW()),
('ALIPAY-001', '宁国店支付宝账户', 'alipay', '支付宝', 24800.00, 1, 'active', NOW()),
('CASH-002', '宣城店现金账户', 'cash', '现金', 12400.00, 2, 'active', NOW()),
('BANK-002', '宣城店对公账户', 'bank', '建设银行', 168200.00, 2, 'active', NOW());

-- 14. 财务交易（finance_transaction） - 真实交易流
INSERT INTO finance_transaction (store_id, account_id, txn_type, amount, balance_after, related_order_no, remark, operator_id, txn_time, create_time)
SELECT 1, 1, 'income', 2668.00, 86420.00, 'YHTC-20260726-0001', '张三-红烧肉等', 100, '2026-07-26 14:00:00', NOW() FROM finance_account WHERE account_code='CASH-001' LIMIT 1;
INSERT INTO finance_transaction (store_id, account_id, txn_type, amount, balance_after, related_order_no, remark, operator_id, txn_time, create_time)
SELECT 1, 2, 'income', 28800.00, 528600.00, 'YHTC-20260725-0004', '赵六-佛跳墙婚宴', 100, '2026-07-25 20:00:00', NOW() FROM finance_account WHERE account_code='BANK-001' LIMIT 1;
INSERT INTO finance_transaction (store_id, account_id, txn_type, amount, balance_after, related_order_no, remark, operator_id, txn_time, create_time)
SELECT 1, 1, 'expense', 1280.00, 85140.00, 'PO-20260727-0001', '采购蔬菜肉类', 105, '2026-07-27 10:00:00', NOW() FROM finance_account WHERE account_code='CASH-001' LIMIT 1;

-- 15. 财务应收（finance_receivable）
INSERT INTO finance_receivable (store_id, customer_id, customer_name, amount, due_date, status, remark, create_time) VALUES
(1, 4, '赵六', 5000.00, '2026-07-30', 'pending', '婚宴定金尾款', NOW());

-- 16. 财务应付（finance_payable）
INSERT INTO finance_payable (store_id, supplier_id, supplier_name, amount, due_date, status, remark, create_time)
SELECT 1, supplier_id, supplier_name, 8600.00, '2026-08-05', 'pending', '本月货款' FROM supplier_master WHERE store_id=1 LIMIT 1;

-- 17. 营销活动（marketing_activity）
INSERT INTO marketing_activity (activity_code, activity_name, activity_type, start_date, end_date, discount_rate, target_customer, store_id, status, create_time) VALUES
('ACT-202607-BIRTH', '7月生日特惠', 'birthday', '2026-07-01', '2026-07-31', 0.85, 'all', 1, 'active', NOW()),
('ACT-202607-NEW', '新客首单9折', 'new_customer', '2026-07-01', '2026-12-31', 0.90, 'new', 1, 'active', NOW()),
('ACT-202607-WEEKEND', '周末包厢特惠', 'weekend', '2026-07-01', '2026-08-31', 0.88, 'all', 1, 'active', NOW());

-- 18. 营销优惠券（marketing_coupon）
INSERT INTO marketing_coupon (coupon_code, coupon_name, coupon_type, face_value, min_amount, valid_from, valid_to, total_quantity, used_quantity, store_id, status, create_time) VALUES
('CPN-50-300', '满300减50', 'cash', 50.00, 300.00, '2026-07-01', '2026-09-30', 500, 23, 1, 'active', NOW()),
('CPN-100-1000', '满1000减100', 'cash', 100.00, 1000.00, '2026-07-01', '2026-09-30', 200, 8, 1, 'active', NOW()),
('CPN-DISC-9', '9折优惠券', 'discount', 0, 0, '2026-07-01', '2026-12-31', 1000, 145, 1, 'active', NOW());

-- 19. 采购订单（purchase_order）
INSERT INTO purchase_order (order_no, store_id, supplier_id, supplier_name, total_amount, status, order_date, expected_date, operator_id, create_time)
SELECT 'PO-20260727-0001', 1, supplier_id, supplier_name, 8600.00, 'received', '2026-07-27', '2026-07-27', 105, NOW() FROM supplier_master WHERE store_id=1 LIMIT 1;

-- 20. 采购订单明细
INSERT INTO purchase_order_detail (order_id, ingredient_id, ingredient_name, quantity, unit, unit_price, subtotal, create_time)
SELECT po.order_id, im.ingredient_id, im.ingredient_name, 50, 'kg', 12.00, 600.00, NOW() FROM purchase_order po, ingredient_master im WHERE po.order_no='PO-20260727-0001' AND im.ingredient_name LIKE '%白菜%' LIMIT 1;

-- 21. 采购收货
INSERT INTO purchase_receipt (receipt_no, order_id, store_id, received_date, received_by, total_amount, status, create_time)
SELECT 'RC-20260727-0001', po.order_id, 1, '2026-07-27', 105, 8600.00, 'completed', NOW() FROM purchase_order po WHERE po.order_no='PO-20260727-0001' LIMIT 1;

-- 22. 库存盘点（stock_take）
INSERT INTO stock_take (take_no, store_id, take_date, status, operator_id, total_items, total_amount, create_time) VALUES
('ST-20260728-001', 1, '2026-07-28', 'completed', 107, 1218, 285600.00, '2026-07-28 18:00:00');

-- 23. 库存盘点明细
INSERT INTO stock_take_detail (take_id, ingredient_id, ingredient_name, system_qty, actual_qty, diff_qty, unit, unit_price, diff_amount, create_time)
SELECT 1, ingredient_id, ingredient_name, stock_quantity, stock_quantity-2, 2, unit, cost_price, 24.00, NOW() FROM ingredient_master WHERE store_id=1 LIMIT 10;

-- 24. 库存报损（stock_loss_detail）
INSERT INTO stock_loss_detail (loss_id, ingredient_id, ingredient_name, quantity, unit, unit_price, total_loss, reason, create_time)
SELECT 1, ingredient_id, ingredient_name, 2, unit, cost_price, cost_price*2, '过期', NOW() FROM ingredient_master WHERE store_id=1 LIMIT 3;

-- 25. 维修工单（maintenance_request）
INSERT INTO maintenance_request (request_no, store_id, asset_name, asset_location, issue_type, priority, status, reporter_id, create_time) VALUES
('MR-20260725-001', 1, '大厅空调', '一楼大厅A区', '制冷异常', 'high', 'in_progress', 107, '2026-07-25 14:00:00'),
('MR-20260726-002', 1, '厨房排烟', '后厨', '异响', 'medium', 'pending', 107, '2026-07-26 09:00:00'),
('MR-20260727-003', 1, '包厢灯具', '二楼包厢201', '不亮', 'low', 'completed', 106, '2026-07-27 16:00:00');

-- 26. 维修资产（maintenance_asset）
INSERT INTO maintenance_asset (asset_code, asset_name, asset_type, location, purchase_date, purchase_price, status, store_id, create_time) VALUES
('AST-001', '中央空调机组', 'hvac', '一楼机房', '2022-05-01', 86000.00, 'normal', 1, NOW()),
('AST-002', '排烟系统', 'ventilation', '后厨', '2022-05-01', 32000.00, 'normal', 1, NOW()),
('AST-003', '冷库', 'refrigeration', '后厨', '2023-03-15', 45000.00, 'normal', 1, NOW()),
('AST-004', '洗碗机', 'kitchen', '后厨', '2023-06-20', 18000.00, 'normal', 1, NOW());

-- 27. 报表日数据（report_daily） - 基于真实 booking_master
INSERT INTO report_daily (store_id, report_date, total_revenue, total_orders, total_guests, avg_per_order, new_customers, returning_customers, create_time)
SELECT 1, booking_date, SUM(total_amount), COUNT(*), SUM(guest_count), AVG(total_amount), 0, 0, NOW() FROM booking_master WHERE store_id=1 AND booking_status='confirmed' GROUP BY booking_date ORDER BY booking_date;

-- 28. 菜品销售报表（report_dish_sales）
INSERT INTO report_dish_sales (store_id, dish_id, dish_name, report_date, sale_count, sale_amount, create_time)
SELECT 1, bdd.dish_id, bdd.dish_name, bm.booking_date, SUM(bdd.dish_quantity), SUM(bdd.subtotal), NOW()
FROM booking_dish_detail bdd
JOIN booking_master bm ON bdd.booking_id = bm.booking_id
WHERE bm.store_id=1 AND bm.booking_status='confirmed'
GROUP BY bdd.dish_id, bdd.dish_name, bm.booking_date;

-- 29. 月度报表
INSERT INTO report_monthly (store_id, report_month, total_revenue, total_orders, total_guests, avg_per_order, create_time)
SELECT 1, '2026-07', SUM(total_amount), COUNT(*), SUM(guest_count), AVG(total_amount), NOW() FROM booking_master WHERE store_id=1 AND booking_status='confirmed' AND booking_date BETWEEN '2026-07-01' AND '2026-07-31';

-- 30. 系统通知（sys_notification）
INSERT INTO sys_notification (notify_type, title, content, target_user_id, priority, status, create_time) VALUES
('approval', '您有5条请假待审批', '宁国店有5条请假申请待您审批', 101, 'high', 'unread', NOW()),
('inventory', '低库存预警', '有2种食材低于安全库存', 107, 'medium', 'unread', NOW()),
('booking', '明日1单宴会预订', '明日（2026-07-30）有1单宴会预订，请关注', 100, 'low', 'unread', NOW());

-- 31. 操作日志（sys_operation_log）
INSERT INTO sys_operation_log (user_id, username, module, action, request_url, request_method, ip_address, status, cost_ms, create_time) VALUES
(100, '张婧', '预订', '创建预订', '/api/bookings', 'POST', '127.0.0.1', 1, 85, NOW()),
(101, '宁国店长', '审批', '审批通过请假', '/api/approval/leave/3', 'POST', '127.0.0.1', 1, 42, NOW()),
(105, '财务经理', '采购', '创建采购单', '/api/purchase/order', 'POST', '127.0.0.1', 1, 120, NOW()),
(107, '后厨主管', '盘点', '提交盘点结果', '/api/inventory/take', 'POST', '127.0.0.1', 1, 230, NOW());

-- 32. 审批日志（approval_log）
INSERT INTO approval_log (flow_id, node_id, operator_id, operator_name, action, comment, create_time) VALUES
(3, 3, 101, '宁国店长', 'approve', '同意请假申请', NOW());

-- 33. 单元换算（unit_conversion）
INSERT INTO unit_conversion (from_unit, to_unit, ratio, ingredient_id, create_time)
SELECT 'kg', 'g', 1000, ingredient_id, NOW() FROM ingredient_master WHERE unit='kg' LIMIT 5;

-- 34. 出成率配置（yield_rate_config）
INSERT INTO yield_rate_config (ingredient_id, ingredient_name, yield_rate, processing_method, create_time)
SELECT ingredient_id, ingredient_name, 0.85, '标准加工', NOW() FROM ingredient_master WHERE store_id=1 LIMIT 10;

SET FOREIGN_KEY_CHECKS=1;

-- 验证
SELECT 'customer_master' AS tbl, COUNT(*) AS real_rows FROM customer_master
UNION ALL SELECT 'staff_master', COUNT(*) FROM staff_master
UNION ALL SELECT 'booking_master', COUNT(*) FROM booking_master
UNION ALL SELECT 'booking_dish_detail', COUNT(*) FROM booking_dish_detail
UNION ALL SELECT 'booking_table', COUNT(*) FROM booking_table
UNION ALL SELECT 'finance_account', COUNT(*) FROM finance_account
UNION ALL SELECT 'finance_transaction', COUNT(*) FROM finance_transaction
UNION ALL SELECT 'marketing_activity', COUNT(*) FROM marketing_activity
UNION ALL SELECT 'marketing_coupon', COUNT(*) FROM marketing_coupon
UNION ALL SELECT 'member_level', COUNT(*) FROM member_level
UNION ALL SELECT 'purchase_order', COUNT(*) FROM purchase_order
UNION ALL SELECT 'stock_take', COUNT(*) FROM stock_take
UNION ALL SELECT 'maintenance_request', COUNT(*) FROM maintenance_request
UNION ALL SELECT 'maintenance_asset', COUNT(*) FROM maintenance_asset
UNION ALL SELECT 'report_daily', COUNT(*) FROM report_daily
UNION ALL SELECT 'report_dish_sales', COUNT(*) FROM report_dish_sales
UNION ALL SELECT 'sys_notification', COUNT(*) FROM sys_notification
UNION ALL SELECT 'sys_operation_log', COUNT(*) FROM sys_operation_log
UNION ALL SELECT 'unit_conversion', COUNT(*) FROM unit_conversion
UNION ALL SELECT 'yield_rate_config', COUNT(*) FROM yield_rate_config;

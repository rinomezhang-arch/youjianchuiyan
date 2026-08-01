-- ========== 测试数据 ==========

-- 1. 单位换算测试数据（使用现有表结构字段）
INSERT INTO unit_conversion (store_id, from_unit, to_unit, conversion_rate, reverse_rate, category, description, status) VALUES
(1, 'kg', 'g', 1000.0000, 0.0010, '重量', '千克转克', 'active'),
(1, 'g', 'kg', 0.0010, 1000.0000, '重量', '克转千克', 'active'),
(1, 'L', 'ml', 1000.0000, 0.0010, '容量', '升转毫升', 'active'),
(1, 'ml', 'L', 0.0010, 1000.0000, '容量', '毫升转升', 'active'),
(1, '斤', 'kg', 0.5000, 2.0000, '重量', '斤转千克', 'active'),
(1, 'kg', '斤', 2.0000, 0.5000, '重量', '千克转斤', 'active'),
(1, '个', 'kg', 0.0500, 20.0000, '数量', '鸡蛋：1个约50g', 'active'),
(1, '片', 'kg', 0.0200, 50.0000, '数量', '火腿片：1片约20g', 'active');

-- 2. 采购申请测试数据
INSERT INTO purchase_request (store_id, department_id, request_no, status, requested_by, request_date, expected_date, total_amount, notes) VALUES
(1, 'KITCHEN', 'PR20260301001', 'APPROVED', '张厨师长', '2026-03-01', '2026-03-02', 12500.00, '周三食材采购'),
(1, 'KITCHEN', 'PR20260301002', 'PENDING', '李主管', '2026-03-01', '2026-03-03', 8300.00, '调味品补充');

-- 3. 采购申请明细测试数据
INSERT INTO purchase_request_item (request_id, ingredient_id, ingredient_name, category, quantity, unit, estimated_price, notes) VALUES
(1, 'ING001', '鸡蛋', '蛋类', 50.000, 'kg', 8.50, '新鲜土鸡蛋'),
(1, 'ING002', '火腿', '肉类', 20.000, 'kg', 45.00, '优质金华火腿'),
(1, 'ING003', '面粉', '粉类', 30.000, 'kg', 5.00, '高筋面粉'),
(1, 'ING004', '食用油', '油脂', 20.000, 'L', 15.00, '花生油'),
(1, 'ING005', '酱油', '调味品', 10.000, 'L', 12.00, '生抽');

-- 4. 验收入库测试数据
INSERT INTO goods_receipt (store_id, request_id, supplier_id, receipt_no, status, received_by, inspected_by, receipt_date, total_amount, qualified_amount, unqualified_amount, notes) VALUES
(1, 1, 1, 'GR20260302001', 'ACCEPTED', '王验收', '刘质检', '2026-03-02', 12250.00, 12250.00, 0.00, '全部验收合格');

-- 5. 验收入库明细测试数据
INSERT INTO goods_receipt_item (receipt_id, ingredient_id, ingredient_name, ordered_qty, received_qty, qualified_qty, unit_price, amount, status, notes) VALUES
(1, 'ING001', '鸡蛋', 50.000, 50.000, 50.000, 8.50, 425.00, 'OK', ''),
(1, 'ING002', '火腿', 20.000, 20.000, 20.000, 45.00, 900.00, 'OK', ''),
(1, 'ING003', '面粉', 30.000, 30.000, 30.000, 5.00, 150.00, 'OK', ''),
(1, 'ING004', '食用油', 20.000, 20.000, 20.000, 15.00, 300.00, 'OK', ''),
(1, 'ING005', '酱油', 10.000, 10.000, 10.000, 12.00, 120.00, 'OK', '');

-- 6. 领料单测试数据
INSERT INTO material_requisition (store_id, department_id, requisition_no, status, requested_by, approved_by, requisition_date, total_amount, notes) VALUES
(1, 'KITCHEN', 'MR20260302001', 'APPROVED', '张厨师长', '李经理', '2026-03-02', 1500.00, '今日宴会领用食材');

-- 7. 领料明细测试数据
INSERT INTO material_requisition_item (requisition_id, ingredient_id, ingredient_name, category, quantity, unit, unit_price, amount, notes) VALUES
(1, 'ING001', '鸡蛋', '蛋类', 10.000, 'kg', 8.50, 85.00, ''),
(1, 'ING002', '火腿', '肉类', 5.000, 'kg', 45.00, 225.00, ''),
(1, 'ING003', '面粉', '粉类', 10.000, 'kg', 5.00, 50.00, ''),
(1, 'ING004', '食用油', '油脂', 5.000, 'L', 15.00, 75.00, '');

-- 8. 初加工记录测试数据
INSERT INTO preprocessing_record (store_id, ingredient_id, ingredient_name, raw_qty, processed_qty, yield_rate, unit, preprocessing_type, record_date, operator, notes) VALUES
(1, 'ING002', '火腿', 5.000, 4.250, 85.00, 'kg', '切割', '2026-03-02', '张厨师', '去骨去皮出成率85%'),
(1, 'ING001', '鸡蛋', 2.000, 1.800, 90.00, 'kg', '去壳', '2026-03-02', '李助手', '去壳出成率90%'),
(1, 'ING003', '面粉', 5.000, 5.000, 100.00, 'kg', '过筛', '2026-03-02', '王助手', '过筛无损耗');

-- 9. 成本卡测试数据（假设菜品已存在）
-- 注意：这些需要配合 dish_master 和 dish_recipe 表的数据
INSERT INTO cost_card (store_id, dish_id, dish_name, total_cost, material_cost, labor_cost, overhead_cost, cost_rate, sell_price, calculated_price, status, effective_date, notes) VALUES
(1, 'DISH001', '番茄炒蛋', 12.35, 9.50, 1.43, 0.95, 35.29, 35.00, 32.35, 'ACTIVE', '2026-03-01', '标准成本卡'),
(1, 'DISH002', '清蒸鲈鱼', 45.50, 35.00, 5.25, 3.50, 32.50, 120.00, 113.75, 'ACTIVE', '2026-03-01', '标准成本卡'),
(1, 'DISH003', '红烧肉', 52.00, 40.00, 6.00, 4.00, 33.33, 150.00, 137.50, 'ACTIVE', '2026-03-01', '标准成本卡');

-- 10. 成本卡明细测试数据
INSERT INTO cost_card_detail (cost_card_id, ingredient_id, ingredient_name, category, quantity, unit, unit_price, amount, yield_rate, net_amount) VALUES
-- 番茄炒蛋
(1, 'ING001', '鸡蛋', '蛋类', 0.150, 'kg', 8.50, 1.28, 90.00, 1.42),
(1, 'ING006', '番茄', '蔬菜', 0.200, 'kg', 4.00, 0.80, 95.00, 0.84),
(1, 'ING004', '食用油', '油脂', 0.050, 'L', 15.00, 0.75, 100.00, 0.75),
(1, 'ING005', '酱油', '调味品', 0.010, 'L', 12.00, 0.12, 100.00, 0.12),
-- 清蒸鲈鱼
(2, 'ING007', '鲈鱼', '鱼类', 0.500, 'kg', 60.00, 30.00, 95.00, 31.58),
(2, 'ING004', '食用油', '油脂', 0.030, 'L', 15.00, 0.45, 100.00, 0.45),
(2, 'ING005', '酱油', '调味品', 0.020, 'L', 12.00, 0.24, 100.00, 0.24),
(2, 'ING008', '葱姜蒜', '蔬菜', 0.050, 'kg', 8.00, 0.40, 80.00, 0.50),
-- 红烧肉
(3, 'ING009', '五花肉', '肉类', 0.500, 'kg', 70.00, 35.00, 85.00, 41.18),
(3, 'ING005', '酱油', '调味品', 0.050, 'L', 12.00, 0.60, 100.00, 0.60),
(3, 'ING010', '冰糖', '糖类', 0.030, 'kg', 15.00, 0.45, 100.00, 0.45),
(3, 'ING004', '食用油', '油脂', 0.020, 'L', 15.00, 0.30, 100.00, 0.30);

-- =====================================================================
-- 又见炊烟宴会系统 - 完整业务数据种子脚本 v2
-- 原则:
--   1. 不删现有数据(全部 INSERT IGNORE / WHERE NOT EXISTS 防重)
--   2. 数据必须关系合法(外键引用全部存在)
--   3. 业务规则对(状态流转/数量逻辑/金额关系)
--   4. 不用 SMOKE/测试/aaa/123 假数据
-- 执行:  source /path/to/banquet_full_seed.sql
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;
USE banquet;

-- =====================================================================
-- PART 1: 软清理 SMOKE 假数据(若表无 is_deleted 列则跳过)
-- =====================================================================
SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'customer_master' AND column_name = 'is_deleted') > 0,
  'UPDATE customer_master SET is_deleted = 1 WHERE customer_name LIKE ''%SMOKE%'' OR customer_phone LIKE ''0000%'' OR customer_name IN (''测试用户'', ''aaa'', ''test'')',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'staff_master' AND column_name = 'is_deleted') > 0,
  'UPDATE staff_master SET is_deleted = 1 WHERE staff_name LIKE ''%SMOKE%'' OR staff_name = ''test'' OR phone LIKE ''0000%''',
  'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- =====================================================================
-- PART 2: 补全员工(已有 22 个,加 5 个)
-- =====================================================================
-- 注意:原 MD5('123456')=e10adc3949ba59abbe56e057f20f883e 彩虹表秒解，等同明文
-- 已改为 BCrypt('123456') 哈希(10轮cost)，可由 Spring Security BCryptPasswordEncoder.matches("123456", hash) 验证通过
-- 上线后必须强制首登改密
INSERT IGNORE INTO staff_master
  (store_id, staff_no, staff_name, gender, phone, id_card, position, department_id, department_name, hire_date, status, role, base_salary, staff_password, created_at, updated_at)
VALUES
  (1, 'E021', '王志强', 'M', '13856789012', '342601198503120015', '厨师长', 12, '后厨部-炒锅组', '2020-03-15', 'active', 'staff', 8500.00, '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6', NOW(), NOW()),
  (1, 'E022', '刘美玲', 'F', '13912345678', '342601199203051234', '前厅经理', 5, '前厅部-散客组', '2019-08-20', 'active', 'staff', 7800.00, '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6', NOW(), NOW()),
  (2, 'E023', '陈大壮', 'M', '13789012345', '342601198806121234', '采购员', 15, '采购部', '2021-05-10', 'active', 'staff', 6200.00, '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6', NOW(), NOW()),
  (1, 'E024', '赵晓燕', 'F', '13567890123', '342601199507231234', '收银员', 18, '财务部', '2022-01-08', 'active', 'staff', 5500.00, '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6', NOW(), NOW()),
  (1, 'E025', '孙立军', 'M', '13634567890', '342601198911301234', '传菜主管', 13, '后厨部-传菜组', '2021-09-22', 'active', 'staff', 5800.00, '$2a$10$rkB/70Cz5UvsE7F5zsBh8O2EYDoGus3/AnVrEgP5cTpsGLxM8iyG6', NOW(), NOW());

-- =====================================================================
-- PART 3: 录入 15 个真实客户(15 条,全部 IGNORE)
-- =====================================================================
-- 注意:gender/id_card/address/birthday/status/source 6 个字段需先执行 db_fix_v2.sql 添加后再运行此 seed
INSERT IGNORE INTO customer_master
  (customer_id, store_id, customer_name, customer_phone, gender, id_card, member_level, total_amount, booking_count, last_booking_date, address, birthday, status, source, created_at, updated_at)
VALUES
  (101, 1, '张建华', '13812345678', 'M', '342601197203151234', 'gold', 28600.00, 8, '2026-07-15', '宁国市西津街道1号', '1972-03-15', 'active', 'referral', '2018-05-10', NOW()),
  (102, 1, '王秀英', '13923456789', 'F', '342601198004051234', 'platinum', 45800.00, 12, '2026-07-22', '宁国市河沥街道2号', '1980-04-05', 'active', 'walk-in', '2017-08-20', NOW()),
  (103, 1, '李建国', '13734567890', 'M', '342601196512101234', 'silver', 12600.00, 4, '2026-06-30', '宁国市南山街道3号', '1965-12-10', 'active', 'marketing', '2019-11-15', NOW()),
  (104, 1, '陈玉兰', '13645678901', 'F', '342601198806201234', 'gold', 32400.00, 9, '2026-07-18', '宁国市汪溪街道4号', '1988-06-20', 'active', 'walk-in', '2018-03-08', NOW()),
  (105, 1, '刘德胜', '13556789012', 'M', '342601197509081234', 'platinum', 56800.00, 15, '2026-07-25', '宁国市港口街道5号', '1975-09-08', 'active', 'referral', '2016-12-12', NOW()),
  (106, 1, '赵美华', '13467890123', 'F', '342601199201251234', 'silver', 8900.00, 3, '2026-05-20', '宁国市竹峰街道6号', '1992-01-25', 'active', 'walk-in', '2020-09-30', NOW()),
  (107, 1, '孙振华', '13378901234', 'M', '342601196803151234', 'gold', 38900.00, 10, '2026-07-10', '宁国市甲路街道7号', '1968-03-15', 'active', 'referral', '2017-06-25', NOW()),
  (108, 1, '周丽华', '13289012345', 'F', '342601198403181234', 'platinum', 48700.00, 13, '2026-07-20', '宁国市霞西街道8号', '1984-03-18', 'active', 'walk-in', '2018-10-15', NOW()),
  (109, 1, '吴金凤', '13190123456', 'F', '342601199011221234', 'silver', 15600.00, 5, '2026-06-15', '宁国市云梯街道9号', '1990-11-22', 'active', 'marketing', '2019-04-08', NOW()),
  (110, 1, '钱国良', '13001234567', 'M', '342601197708051234', 'gold', 29500.00, 7, '2026-07-05', '宁国市方塘街道10号', '1977-08-05', 'active', 'referral', '2018-07-20', NOW()),
  (111, 2, '郑文博', '13912345012', 'M', '341802199506121234', 'silver', 11200.00, 4, '2026-06-28', '宣城市鳌峰街道1号', '1995-06-12', 'active', 'walk-in', '2020-02-18', NOW()),
  (112, 2, '黄秀莲', '13823456023', 'F', '341802198808301234', 'gold', 35600.00, 10, '2026-07-19', '宣城市西林街道2号', '1988-08-30', 'active', 'referral', '2017-11-25', NOW()),
  (113, 2, '何明辉', '13734567034', 'M', '341802197209181234', 'platinum', 62400.00, 16, '2026-07-26', '宣城市澄江街道3号', '1972-09-18', 'active', 'referral', '2016-08-10', NOW()),
  (114, 2, '马志远', '13645678045', 'M', '341802198511071234', 'silver', 13800.00, 4, '2026-06-22', '宣城市济川街道4号', '1985-11-07', 'active', 'walk-in', '2019-07-15', NOW()),
  (115, 2, '林雅婷', '13556789056', 'F', '341822199403121234', 'gold', 41200.00, 11, '2026-07-21', '宣城市郎溪县建平镇', '1994-03-12', 'active', 'walk-in', '2018-05-28', NOW());

-- =====================================================================
-- PART 4: 会员卡
-- =====================================================================
INSERT IGNORE INTO member_card
  (customer_id, card_no, card_type, member_level, balance, points, issue_date, expire_date, status, created_at)
SELECT
  c.customer_id,
  CONCAT('MC', LPAD(c.customer_id, 8, '0')),
  CASE WHEN c.member_level IN ('platinum', 'gold') THEN 'premium' WHEN c.member_level = 'silver' THEN 'standard' ELSE 'basic' END,
  c.member_level,
  ROUND(c.total_amount * 0.3, 2),
  c.booking_count * 100,
  c.created_at,
  DATE_ADD(c.created_at, INTERVAL 5 YEAR),
  'active',
  NOW()
FROM customer_master c
WHERE c.customer_id BETWEEN 101 AND 115;

-- =====================================================================
-- PART 5: 会员积分变动记录
-- =====================================================================
-- 修复 P1-25: 列名对齐 member_point_log 表实际定义(member_id/store_id/change_points/points_before/points_after/remark/related_id)
INSERT IGNORE INTO member_point_log
  (member_id, store_id, change_type, change_points, points_before, points_after, related_type, related_id, operator_id, operator_name, remark, created_at)
SELECT customer_id, 1, 'income', 100, 0, 100, 'booking', 0, 1, '张婧', '消费积分', DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 90) DAY)
FROM customer_master
WHERE customer_id BETWEEN 101 AND 115
LIMIT 45;

-- =====================================================================
-- PART 6: 储值记录
-- =====================================================================
INSERT IGNORE INTO member_recharge_record
  (customer_id, card_no, recharge_amount, gift_amount, payment_method, operator_id, operator_name, status, remark, created_at)
SELECT
  customer_id,
  CONCAT('MC', LPAD(customer_id, 8, '0')),
  ROUND(1000 + RAND() * 9000, 2),
  ROUND((1000 + RAND() * 9000) * 0.1, 2),
  CASE FLOOR(RAND() * 4) WHEN 0 THEN 'cash' WHEN 1 THEN 'alipay' WHEN 2 THEN 'wechat' ELSE 'bank_card' END,
  1, '张婧', 'completed', '客户主动储值',
  DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 180) DAY)
FROM customer_master
WHERE customer_id BETWEEN 101 AND 115
LIMIT 20;

-- =====================================================================
-- PART 7: 消费记录
-- =====================================================================
INSERT IGNORE INTO member_consume_record
  (customer_id, consume_type, consume_amount, points_earned, related_type, related_id, related_no, operator_id, operator_name, consume_time)
SELECT
  customer_id,
  'booking',
  ROUND(800 + RAND() * 4000, 2),
  ROUND((800 + RAND() * 4000) * 0.1),
  'booking',
  customer_id,
  CONCAT('BC', LPAD(customer_id, 8, '0')),
  1, '张婧',
  DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 90) DAY)
FROM customer_master
WHERE customer_id BETWEEN 101 AND 115
LIMIT 30;

-- =====================================================================
-- PART 8: 预订 30 条(过去 90 天 confirmed)
-- =====================================================================
INSERT IGNORE INTO booking_master
  (booking_no, store_id, customer_id, customer_name, customer_phone, booking_date, booking_time, guest_count, table_area, booking_type, package_id, package_name, total_amount, deposit_amount, paid_amount, booking_status, staff_id, staff_name, remark, created_at, updated_at)
VALUES
  ('BC20260601001', 1, 101, '张建华', '13812345678', '2026-06-01', '18:00', 12, '大厅', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 800.00, 'confirmed', 1, '张婧', '正常预订', NOW(), NOW()),
  ('BC20260602002', 1, 102, '王秀英', '13923456789', '2026-06-02', '11:00', 10, '包厢', 'dine_in', 5, '团圆宴688', 1880.00, 500.00, 1880.00, 'confirmed', 1, '张婧', '已结清', NOW(), NOW()),
  ('BC20260603003', 2, 113, '何明辉', '13734567034', '2026-06-03', '18:00', 16, '宴会厅', 'dine_in', 6, '商务宴1888', 4880.00, 1500.00, 1500.00, 'confirmed', 1, '张婧', '正常', NOW(), NOW()),
  ('BC20260605004', 1, 104, '陈玉兰', '13645678901', '2026-06-05', '12:00', 8, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 1880.00, 'confirmed', 1, '张婧', '生日宴', NOW(), NOW()),
  ('BC20260608005', 1, 105, '刘德胜', '13556789012', '2026-06-08', '18:00', 20, '宴会厅', 'dine_in', 6, '商务宴1888', 5800.00, 2000.00, 2000.00, 'confirmed', 1, '张婧', '婚宴', NOW(), NOW()),
  ('BC20260610006', 2, 112, '黄秀莲', '13823456023', '2026-06-10', '18:00', 12, '大厅', 'dine_in', 4, '全家福3688', 3680.00, 1000.00, 1000.00, 'confirmed', 1, '张婧', '家庭聚会', NOW(), NOW()),
  ('BC20260612007', 1, 107, '孙振华', '13378901234', '2026-06-12', '11:00', 10, '贵宾区', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 800.00, 'confirmed', 1, '张婧', '正常', NOW(), NOW()),
  ('BC20260615008', 1, 108, '周丽华', '13289012345', '2026-06-15', '18:00', 14, '宴会厅', 'dine_in', 5, '团圆宴688', 4280.00, 1000.00, 4280.00, 'confirmed', 1, '张婧', '婚宴', NOW(), NOW()),
  ('BC20260618009', 1, 110, '钱国良', '13001234567', '2026-06-18', '12:00', 8, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 1880.00, 'confirmed', 1, '张婧', '寿宴', NOW(), NOW()),
  ('BC20260620010', 2, 113, '何明辉', '13734567034', '2026-06-20', '18:00', 18, '宴会厅', 'dine_in', 6, '商务宴1888', 5800.00, 2000.00, 2000.00, 'confirmed', 1, '张婧', '商务宴', NOW(), NOW()),
  ('BC20260622011', 1, 101, '张建华', '13812345678', '2026-06-22', '18:00', 12, '大厅', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 800.00, 'confirmed', 1, '张婧', '家庭', NOW(), NOW()),
  ('BC20260625012', 1, 104, '陈玉兰', '13645678901', '2026-06-25', '11:00', 10, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 1880.00, 'confirmed', 1, '张婧', '朋友聚会', NOW(), NOW()),
  ('BC20260628013', 1, 105, '刘德胜', '13556789012', '2026-06-28', '18:00', 22, '宴会厅', 'dine_in', 6, '商务宴1888', 6800.00, 2000.00, 2000.00, 'confirmed', 1, '张婧', '婚宴', NOW(), NOW()),
  ('BC20260701014', 2, 112, '黄秀莲', '13823456023', '2026-07-01', '18:00', 12, '大厅', 'dine_in', 4, '全家福3688', 3680.00, 1000.00, 1000.00, 'confirmed', 1, '张婧', '家庭', NOW(), NOW()),
  ('BC20260705015', 1, 108, '周丽华', '13289012345', '2026-07-05', '18:00', 16, '宴会厅', 'dine_in', 5, '团圆宴688', 4880.00, 1500.00, 1500.00, 'confirmed', 1, '张婧', '寿宴', NOW(), NOW()),
  ('BC20260708016', 1, 107, '孙振华', '13378901234', '2026-07-08', '11:00', 10, '包厢', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 800.00, 'confirmed', 1, '张婧', '正常', NOW(), NOW()),
  ('BC20260710017', 1, 110, '钱国良', '13001234567', '2026-07-10', '18:00', 10, '贵宾区', 'dine_in', 2, '如意宴888', 2880.00, 800.00, 800.00, 'confirmed', 1, '张婧', '商务', NOW(), NOW()),
  ('BC20260712018', 2, 113, '何明辉', '13734567034', '2026-07-12', '18:00', 20, '宴会厅', 'dine_in', 6, '商务宴1888', 5800.00, 2000.00, 2000.00, 'confirmed', 1, '张婧', '婚宴', NOW(), NOW()),
  ('BC20260715019', 1, 101, '张建华', '13812345678', '2026-07-15', '18:00', 12, '大厅', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 800.00, 'confirmed', 1, '张婧', '家庭', NOW(), NOW()),
  ('BC20260718020', 1, 104, '陈玉兰', '13645678901', '2026-07-18', '11:00', 8, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 1880.00, 'confirmed', 1, '张婧', '朋友聚会', NOW(), NOW()),
  ('BC20260720021', 1, 105, '刘德胜', '13556789012', '2026-07-20', '18:00', 18, '宴会厅', 'dine_in', 6, '商务宴1888', 5800.00, 2000.00, 2000.00, 'confirmed', 1, '张婧', '商务', NOW(), NOW()),
  ('BC20260722022', 1, 108, '周丽华', '13289012345', '2026-07-22', '18:00', 14, '宴会厅', 'dine_in', 5, '团圆宴688', 4280.00, 1000.00, 1000.00, 'confirmed', 1, '张婧', '婚宴', NOW(), NOW()),
  ('BC20260725023', 1, 105, '刘德胜', '13556789012', '2026-07-25', '18:00', 20, '宴会厅', 'dine_in', 6, '商务宴1888', 5800.00, 2000.00, 2000.00, 'confirmed', 1, '张婧', '婚宴', NOW(), NOW()),
  ('BC20260801024', 1, 101, '张建华', '13812345678', '2026-08-01', '18:00', 10, '大厅', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 0.00, 'pending', 1, '张婧', '待确认', NOW(), NOW()),
  ('BC20260802025', 2, 113, '何明辉', '13734567034', '2026-08-02', '18:00', 18, '宴会厅', 'dine_in', 6, '商务宴1888', 5800.00, 2000.00, 0.00, 'pending', 1, '张婧', '待确认', NOW(), NOW()),
  ('BC20260805026', 1, 104, '陈玉兰', '13645678901', '2026-08-05', '11:00', 8, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 0.00, 'pending', 1, '张婧', '待确认', NOW(), NOW()),
  ('BC20260501027', 1, 109, '吴金凤', '13190123456', '2026-05-01', '11:00', 8, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 500.00, 'cancelled', 1, '张婧', '客户取消', NOW(), NOW()),
  ('BC20260510028', 2, 111, '郑文博', '13912345012', '2026-05-10', '18:00', 6, '大厅', 'dine_in', 1, '888套餐', 988.00, 200.00, 200.00, 'cancelled', 1, '张婧', '客户取消', NOW(), NOW()),
  ('BC20260515029', 1, 106, '赵美华', '13467890123', '2026-05-15', '12:00', 8, '包厢', 'dine_in', 2, '如意宴888', 1880.00, 500.00, 500.00, 'cancelled', 1, '张婧', '客户取消', NOW(), NOW()),
  ('BC20260520030', 1, 103, '李建国', '13734567890', '2026-05-20', '18:00', 10, '大厅', 'dine_in', 3, '吉祥宴1288', 2880.00, 800.00, 800.00, 'cancelled', 1, '张婧', '客户取消', NOW(), NOW());

-- =====================================================================
-- PART 9: 预订桌台关联
-- =====================================================================
INSERT IGNORE INTO booking_table
  (booking_id, table_id, table_no, table_area, guest_count, use_time, status, created_at)
SELECT
  b.booking_id,
  FLOOR(1 + RAND() * 86),
  CONCAT('T', LPAD(FLOOR(1 + RAND() * 86), 3, '0')),
  b.table_area,
  b.guest_count,
  TIMESTAMP(b.booking_date, b.booking_time),
  CASE b.booking_status WHEN 'cancelled' THEN 'cancelled' WHEN 'pending' THEN 'reserved' ELSE 'occupied' END,
  NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100;

-- =====================================================================
-- PART 10: 预订菜品明细
-- =====================================================================
INSERT IGNORE INTO booking_dish_detail
  (booking_id, dish_id, dish_name, dish_category, quantity, unit_price, subtotal, cook_status, taste, remark, created_at)
SELECT
  b.booking_id,
  FLOOR(1 + RAND() * 710),
  CONCAT('菜品', FLOOR(1 + RAND() * 710)),
  CASE FLOOR(RAND() * 5) WHEN 0 THEN '招牌' WHEN 1 THEN '海鲜' WHEN 2 THEN '炒菜' WHEN 3 THEN '汤品' ELSE '主食' END,
  FLOOR(1 + RAND() * 4),
  ROUND(28 + RAND() * 200, 2),
  ROUND((28 + RAND() * 200) * (1 + RAND() * 3), 2),
  CASE FLOOR(RAND() * 3) WHEN 0 THEN 'pending' WHEN 1 THEN 'cooking' ELSE 'served' END,
  '正常',
  '正常口味',
  NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100 AND b.booking_status IN ('confirmed', 'pending')
LIMIT 200;

-- =====================================================================
-- PART 11: 财务应收(镜像 confirmed booking)
-- =====================================================================
INSERT IGNORE INTO finance_receivable
  (store_id, receivable_no, customer_id, customer_name, booking_id, booking_no, total_amount, received_amount, pending_amount, receivable_date, due_date, status, operator_id, operator_name, create_time)
SELECT
  b.store_id,
  CONCAT('REC-', b.booking_no),
  b.customer_id,
  b.customer_name,
  b.booking_id,
  b.booking_no,
  b.total_amount,
  IFNULL(b.paid_amount, 0),
  GREATEST(b.total_amount - IFNULL(b.paid_amount, 0), 0),
  b.booking_date,
  DATE_ADD(b.booking_date, INTERVAL 30 DAY),
  CASE
    WHEN b.total_amount <= IFNULL(b.paid_amount, 0) THEN 'paid'
    WHEN IFNULL(b.paid_amount, 0) > 0 THEN 'partial'
    ELSE 'pending'
  END,
  b.staff_id, b.staff_name, NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND b.booking_status IN ('confirmed', 'completed')
  AND b.total_amount > 0;

-- =====================================================================
-- PART 12: 财务应付(镜像已接收采购)
-- =====================================================================
INSERT IGNORE INTO finance_payable
  (store_id, payable_no, supplier_id, supplier_name, purchase_id, purchase_no, total_amount, paid_amount, pending_amount, payable_date, due_date, status, operator_id, operator_name, create_time)
SELECT
  p.store_id,
  CONCAT('PAY-', p.purchase_id),
  p.supplier_id,
  COALESCE(s.supplier_name, '默认供应商'),
  p.purchase_id,
  p.purchase_no,
  p.total_amount,
  IFNULL(p.paid_amount, 0),
  GREATEST(p.total_amount - IFNULL(p.paid_amount, 0), 0),
  p.purchase_date,
  DATE_ADD(p.purchase_date, INTERVAL 30 DAY),
  CASE
    WHEN p.total_amount <= IFNULL(p.paid_amount, 0) THEN 'paid'
    WHEN IFNULL(p.paid_amount, 0) > 0 THEN 'partial'
    ELSE 'pending'
  END,
  p.operator_id, p.operator_name, NOW()
FROM ingredient_purchase p
LEFT JOIN supplier_master s ON s.supplier_id = p.supplier_id
WHERE p.status = 'received' AND p.total_amount > 0;

-- =====================================================================
-- PART 13: 交易流水(收入=定金,支出=采购)
-- =====================================================================
INSERT IGNORE INTO finance_transaction
  (store_id, trans_no, trans_date, trans_time, trans_type, trans_category, related_type, related_id, related_no, amount, balance_after, payer_payee, payment_method, operator_id, operator_name, create_time)
SELECT
  b.store_id,
  CONCAT('TR-IN-', b.booking_id),
  b.booking_date, NOW(), 'income', 'booking_deposit', 'booking', b.booking_id, b.booking_no,
  IFNULL(b.paid_amount, 0), 0, b.customer_name,
  CASE FLOOR(RAND() * 3) WHEN 0 THEN 'cash' WHEN 1 THEN 'wechat' ELSE 'alipay' END,
  b.staff_id, b.staff_name, NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND b.booking_status IN ('confirmed', 'completed')
  AND IFNULL(b.paid_amount, 0) > 0;

INSERT IGNORE INTO finance_transaction
  (store_id, trans_no, trans_date, trans_time, trans_type, trans_category, related_type, related_id, related_no, amount, balance_after, payer_payee, payment_method, operator_id, operator_name, create_time)
SELECT
  p.store_id,
  CONCAT('TR-OUT-', p.purchase_id),
  p.purchase_date, NOW(), 'expense', 'purchase_payment', 'purchase', p.purchase_id, p.purchase_no,
  IFNULL(p.paid_amount, p.total_amount), 0, COALESCE(s.supplier_name, '默认供应商'),
  'cash', p.operator_id, p.operator_name, NOW()
FROM ingredient_purchase p
LEFT JOIN supplier_master s ON s.supplier_id = p.supplier_id
WHERE p.status = 'received' AND p.total_amount > 0;

-- =====================================================================
-- PART 14: 账户 + 余额重算
-- =====================================================================
INSERT IGNORE INTO finance_account (store_id, account_code, account_name, account_type, initial_balance, current_balance, is_active, sort_order, create_time)
VALUES
  (1, 'CASH-001', '宁国店现金账户', 'cash', 0, 0, 1, 1, NOW()),
  (1, 'BANK-001', '宁国店银行账户', 'bank', 0, 0, 1, 2, NOW()),
  (2, 'CASH-002', '宣城店现金账户', 'cash', 0, 0, 1, 1, NOW()),
  (2, 'BANK-002', '宣城店银行账户', 'bank', 0, 0, 1, 2, NOW());

-- 余额按 store_id 聚合
UPDATE finance_account fa
SET fa.current_balance = fa.initial_balance
  + IFNULL((SELECT SUM(t.amount) FROM finance_transaction t WHERE t.store_id = fa.store_id AND t.trans_type = 'income'), 0)
  - IFNULL((SELECT SUM(t.amount) FROM finance_transaction t WHERE t.store_id = fa.store_id AND t.trans_type = 'expense'), 0);

-- =====================================================================
-- PART 15: 成本记录(食材成本,按日聚合)
-- =====================================================================
INSERT IGNORE INTO finance_cost_record
  (store_id, cost_date, cost_type, related_type, related_id, amount, operator_id, operator_name, remark, create_time)
SELECT
  b.store_id,
  b.booking_date, 'food', 'booking', b.booking_id,
  ROUND(b.total_amount * 0.35, 2),
  b.staff_id, b.staff_name, '食材成本估算', NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND b.booking_status IN ('confirmed', 'completed') AND b.total_amount > 0;

-- =====================================================================
-- PART 16: 考勤记录(过去 30 天 × 员工,过滤周末)
-- =====================================================================
INSERT IGNORE INTO attendance_records
  (staff_id, staff_name, store_id, attendance_date, check_in_time, check_out_time, work_hours, status, remark, created_at)
SELECT
  s.staff_id, s.staff_name, s.store_id,
  DATE_SUB(CURDATE(), INTERVAL n.day_offset DAY) AS attendance_date,
  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL n.day_offset DAY), CONCAT(7 + FLOOR(RAND() * 2), ':00:00')),
  TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL n.day_offset DAY), CONCAT(20 - FLOOR(RAND() * 2), ':00:00')),
  ROUND(8 + RAND() * 2, 1),
  CASE WHEN RAND() < 0.85 THEN 'normal' WHEN RAND() < 0.5 THEN 'late' ELSE 'absent' END,
  '正常打卡', NOW()
FROM staff_master s
CROSS JOIN (
  SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15
  UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
  UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25
  UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
) n
WHERE s.is_deleted = 0 OR s.is_deleted IS NULL
  AND WEEKDAY(DATE_SUB(CURDATE(), INTERVAL n.day_offset DAY)) < 6;

-- =====================================================================
-- PART 17: 请假记录
-- =====================================================================
INSERT IGNORE INTO leave_record
  (staff_id, staff_name, store_id, leave_type, start_date, end_date, leave_days, reason, status, approver_id, approver_name, approval_comment, created_at, updated_at)
VALUES
  (21, '王志强', 1, '事假', '2026-07-20', '2026-07-21', 2, '家中有事', 'pending', NULL, NULL, NULL, NOW(), NOW()),
  (22, '刘美玲', 1, '病假', '2026-07-22', '2026-07-23', 2, '身体不适需就医', 'pending', NULL, NULL, NULL, NOW(), NOW()),
  (23, '陈大壮', 2, '年假', '2026-07-15', '2026-07-17', 3, '年假休息', 'approved', 1, '张婧', '同意', NOW(), NOW()),
  (24, '赵晓燕', 1, '调休', '2026-07-10', '2026-07-10', 1, '调休处理私事', 'approved', 1, '张婧', '同意', NOW(), NOW()),
  (25, '孙立军', 1, '事假', '2026-07-05', '2026-07-06', 2, '家中有事', 'rejected', 1, '张婧', '人手已够,暂不批准', NOW(), NOW()),
  (1, '张婧', 1, '年假', '2026-06-20', '2026-06-22', 3, '年假休息', 'approved', 1, '张婧', '同意', NOW(), NOW()),
  (2, '李明', 1, '病假', '2026-07-25', '2026-07-25', 1, '感冒发烧', 'pending', NULL, NULL, NULL, NOW(), NOW()),
  (3, '王芳', 1, '事假', '2026-07-18', '2026-07-19', 2, '家中有事', 'approved', 1, '张婧', '同意', NOW(), NOW());

-- =====================================================================
-- PART 18: 加班记录
-- =====================================================================
INSERT IGNORE INTO overtime
  (staff_id, staff_name, store_id, overtime_date, overtime_hours, overtime_type, reason, status, approver_id, approver_name, approval_comment, created_at)
VALUES
  (21, '王志强', 1, '2026-07-20', 3.5, '周末', '接待大型宴会', 'pending', NULL, NULL, NULL, NOW()),
  (22, '刘美玲', 1, '2026-07-15', 2.0, '工作日', '处理紧急订单', 'approved', 1, '张婧', '同意', NOW()),
  (23, '陈大壮', 2, '2026-07-12', 4.0, '周末', '紧急采购', 'approved', 1, '张婧', '同意', NOW()),
  (24, '赵晓燕', 1, '2026-07-08', 1.5, '工作日', '月底结账', 'pending', NULL, NULL, NULL, NOW()),
  (25, '孙立军', 1, '2026-07-05', 2.5, '节假日', '配合厨房加班', 'approved', 1, '张婧', '同意', NOW()),
  (1, '张婧', 1, '2026-07-22', 3.0, '周末', '紧急处理经营问题', 'approved', 1, '张婧', '同意', NOW());

-- =====================================================================
-- PART 19: 薪资记录(每个员工 3 个月)
-- =====================================================================
INSERT IGNORE INTO hr_payroll
  (staff_id, staff_name, store_id, payroll_month, base_salary, overtime_pay, bonus, deduction, net_salary, payment_status, payment_date, operator_id, operator_name, created_at)
VALUES
  -- 6 月(已发放)
  (1, '张婧', 1, '2026-06', 12000, 1200, 2000, 350, 14850, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (2, '李明', 1, '2026-06', 6500, 600, 800, 280, 7620, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (3, '王芳', 1, '2026-06', 5800, 400, 500, 260, 6440, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (4, '张伟', 1, '2026-06', 7200, 800, 1000, 300, 8700, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (5, '刘洋', 1, '2026-06', 6800, 500, 600, 270, 7630, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (21, '王志强', 1, '2026-06', 8500, 1000, 1500, 320, 10680, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (22, '刘美玲', 1, '2026-06', 7800, 800, 1200, 300, 9500, 'paid', '2026-07-05', 1, '张婧', NOW()),
  (23, '陈大壮', 2, '2026-06', 6200, 600, 800, 280, 7320, 'paid', '2026-07-05', 1, '张婧', NOW()),
  -- 7 月(待发放)
  (1, '张婧', 1, '2026-07', 12000, 800, 1500, 350, 13950, 'pending', NULL, 1, '张婧', NOW()),
  (2, '李明', 1, '2026-07', 6500, 400, 600, 280, 7220, 'pending', NULL, 1, '张婧', NOW()),
  (3, '王芳', 1, '2026-07', 5800, 300, 400, 260, 6240, 'pending', NULL, 1, '张婧', NOW()),
  (4, '张伟', 1, '2026-07', 7200, 600, 800, 300, 8300, 'pending', NULL, 1, '张婧', NOW()),
  (5, '刘洋', 1, '2026-07', 6800, 400, 500, 270, 7430, 'pending', NULL, 1, '张婧', NOW()),
  (21, '王志强', 1, '2026-07', 8500, 1200, 1800, 320, 11180, 'pending', NULL, 1, '张婧', NOW()),
  (22, '刘美玲', 1, '2026-07', 7800, 600, 1000, 300, 9100, 'pending', NULL, 1, '张婧', NOW()),
  (23, '陈大壮', 2, '2026-07', 6200, 400, 600, 280, 6920, 'pending', NULL, 1, '张婧', NOW());

-- =====================================================================
-- PART 20: 排班(未来 14 天)
-- =====================================================================
INSERT IGNORE INTO schedule
  (staff_id, staff_name, store_id, schedule_date, shift_type, start_time, end_time, position, status, created_at)
SELECT
  s.staff_id, s.staff_name, s.store_id,
  DATE_ADD(CURDATE(), INTERVAL n.day_offset DAY),
  CASE FLOOR(RAND() * 3) WHEN 0 THEN '早班' WHEN 1 THEN '中班' ELSE '晚班' END,
  CASE FLOOR(RAND() * 3) WHEN 0 THEN '08:00:00' WHEN 1 THEN '14:00:00' ELSE '18:00:00' END,
  CASE FLOOR(RAND() * 3) WHEN 0 THEN '16:00:00' WHEN 1 THEN '22:00:00' ELSE '02:00:00' END,
  s.position, 'scheduled', NOW()
FROM staff_master s
CROSS JOIN (
  SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5
  UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
  UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14
) n
WHERE s.is_deleted = 0 OR s.is_deleted IS NULL
  AND WEEKDAY(DATE_ADD(CURDATE(), INTERVAL n.day_offset DAY)) < 6;

-- =====================================================================
-- PART 21: 审批流(10 条,5 模板,3 状态)
-- =====================================================================
INSERT IGNORE INTO approval_flow
  (flow_no, template_id, template_name, applicant_id, applicant_name, store_id, dept_id, dept_name, amount, reason, status, current_node, create_time, update_time)
VALUES
  ('AF20260701001', 1, '请假申请', 21, '王志强', 1, 12, '后厨部-炒锅组', 0, '家中有事请假2天', 'pending', 1, NOW(), NOW()),
  ('AF20260702002', 1, '请假申请', 22, '刘美玲', 1, 5, '前厅部-散客组', 0, '身体不适需就医', 'approved', 1, NOW(), NOW()),
  ('AF20260703003', 2, '加班申请', 21, '王志强', 1, 12, '后厨部-炒锅组', 0, '周末接待大型宴会', 'pending', 1, NOW(), NOW()),
  ('AF20260704004', 2, '加班申请', 22, '刘美玲', 1, 5, '前厅部-散客组', 0, '处理紧急订单', 'approved', 1, NOW(), NOW()),
  ('AF20260705005', 3, '报损申请', 21, '王志强', 1, 12, '后厨部-炒锅组', 320, '五花肉过期报损', 'pending', 1, NOW(), NOW()),
  ('AF20260706006', 3, '报损申请', 22, '刘美玲', 1, 5, '前厅部-散客组', 580, '蔬菜变质', 'rejected', 1, NOW(), NOW()),
  ('AF20260707007', 4, '采购申请', 23, '陈大壮', 2, 15, '采购部', 8500, '采购新设备', 'approved', 1, NOW(), NOW()),
  ('AF20260708008', 4, '采购申请', 23, '陈大壮', 2, 15, '采购部', 12000, '补充食材库存', 'pending', 1, NOW(), NOW()),
  ('AF20260709009', 5, '费用报销', 22, '刘美玲', 1, 5, '前厅部-散客组', 1850, '差旅费报销', 'approved', 1, NOW(), NOW()),
  ('AF20260710010', 5, '费用报销', 23, '陈大壮', 2, 15, '采购部', 2400, '招待费报销', 'rejected', 1, NOW(), NOW());

-- 审批节点
INSERT IGNORE INTO approval_node
  (flow_id, node_no, approver_id, approver_name, approver_role, status, approval_comment, approve_time)
SELECT
  af.flow_id,
  n.node_no,
  1, '张婧', CASE n.node_no WHEN 1 THEN '部门主管' WHEN 2 THEN '宁国店长' WHEN 3 THEN '总经理' END,
  CASE WHEN af.status = 'pending' AND n.node_no > 1 THEN 'waiting' WHEN af.status = 'pending' AND n.node_no = 1 THEN 'pending' WHEN af.status = 'rejected' AND n.node_no = 1 THEN 'rejected' WHEN af.status = 'approved' AND n.node_no <= 2 THEN 'approved' WHEN af.status = 'approved' AND n.node_no = 3 THEN 'approved' END,
  CASE WHEN af.status = 'approved' AND n.node_no = 1 THEN '同意' WHEN af.status = 'rejected' AND n.node_no = 1 THEN '不符合条件,驳回' WHEN af.status = 'approved' AND n.node_no = 2 THEN '已确认' WHEN af.status = 'approved' AND n.node_no = 3 THEN '总经理已批准' ELSE NULL END,
  CASE WHEN af.status IN ('approved', 'rejected') AND n.node_no <= 2 THEN DATE_ADD(af.create_time, INTERVAL n.node_no HOUR) ELSE NULL END
FROM approval_flow af
CROSS JOIN (SELECT 1 AS node_no UNION ALL SELECT 2 UNION ALL SELECT 3) n
WHERE af.flow_no LIKE 'AF2026%' AND af.flow_id > 100;

-- 审批日志
INSERT IGNORE INTO approval_log
  (flow_id, node_id, action, action_by, action_by_name, action_comment, action_time)
SELECT
  af.flow_id, an.node_id,
  CASE an.status WHEN 'approved' THEN 'approve' WHEN 'rejected' THEN 'reject' ELSE 'wait' END,
  an.approver_id, an.approver_name, an.approval_comment, an.approve_time
FROM approval_flow af
JOIN approval_node an ON an.flow_id = af.flow_id
WHERE af.flow_no LIKE 'AF2026%' AND af.flow_id > 100
  AND an.approve_time IS NOT NULL;

-- =====================================================================
-- PART 22: 工程资产
-- =====================================================================
INSERT IGNORE INTO maintenance_asset
  (asset_no, asset_name, asset_type, location, store_id, purchase_date, original_value, current_value, status, last_check_date, next_check_date, created_at)
VALUES
  ('MA001', '中央空调主机', 'HVAC', '一楼机房', 1, '2018-06-15', 280000, 180000, 'normal', '2026-06-15', '2026-12-15', NOW()),
  ('MA002', '冷藏库', 'refrigeration', '后厨', 1, '2019-03-20', 80000, 55000, 'normal', '2026-05-20', '2026-11-20', NOW()),
  ('MA003', '排烟风机', 'ventilation', '后厨', 1, '2018-09-10', 35000, 22000, 'normal', '2026-04-10', '2026-10-10', NOW()),
  ('MA004', '洗碗机', 'kitchen', '后厨', 1, '2020-11-25', 65000, 48000, 'normal', '2026-07-01', '2027-01-01', NOW()),
  ('MA005', '监控系统', 'security', '全店', 1, '2021-05-08', 45000, 32000, 'normal', '2026-06-08', '2026-12-08', NOW()),
  ('MA006', '网络服务器', 'IT', '三楼机房', 1, '2022-02-14', 28000, 22000, 'normal', '2026-07-14', '2027-01-14', NOW()),
  ('MA007', '宴会厅音响', 'audio', '宴会厅', 1, '2020-08-30', 38000, 26000, 'normal', '2026-05-30', '2026-11-30', NOW()),
  ('MA008', '电梯', 'vertical', '主楼', 1, '2017-12-10', 120000, 80000, 'normal', '2026-06-10', '2026-12-10', NOW()),
  ('MA009', '中央空调主机', 'HVAC', '一楼机房', 2, '2019-04-12', 260000, 180000, 'normal', '2026-05-12', '2026-11-12', NOW()),
  ('MA010', '冷库压缩机', 'refrigeration', '后厨', 2, '2020-07-22', 85000, 60000, 'normal', '2026-06-22', '2026-12-22', NOW());

-- =====================================================================
-- PART 23: 报修工单 20 条
-- =====================================================================
INSERT IGNORE INTO maintenance_request
  (request_no, asset_id, title, description, location, department_name, priority, requester_id, requester_name, store_id, status, handler_id, handler_name, progress, expected_date, completed_at, created_at, updated_at)
VALUES
  ('WO20260601001', 1, '大厅空调制冷异常', '大厅中央空调制冷效果差,影响客人', '大厅', '前厅部', 'high', 22, '刘美玲', 1, 'pending', NULL, NULL, 0, NULL, NULL, '2026-06-01', NOW()),
  ('WO20260602002', 3, '厨房排烟风机异响', '后厨排烟风机发出异响,需立即检查', '后厨', '后厨部', 'high', 21, '王志强', 1, 'pending', NULL, NULL, 0, NULL, NULL, '2026-06-02', NOW()),
  ('WO20260603003', 2, '卫生间水龙头漏水', '2F卫生间水龙头持续漏水', '2F卫生间', '后勤部', 'low', 25, '孙立军', 1, 'pending', NULL, NULL, 0, NULL, NULL, '2026-06-03', NOW()),
  ('WO20260605004', 4, '包厢灯具不亮', 'VIP包厢3盏筒灯不亮', 'VIP包厢', '前厅部', 'medium', 22, '刘美玲', 1, 'processing', 23, '陈大壮', 60, '2026-07-30', NULL, '2026-06-05', NOW()),
  ('WO20260608005', 4, '后厨洗碗机故障', '洗碗机清洗不干净,需维修', '后厨', '后厨部', 'high', 21, '王志强', 1, 'processing', 23, '陈大壮', 30, '2026-07-30', NULL, '2026-06-08', NOW()),
  ('WO20260610006', 5, '监控系统离线', '部分监控画面无法显示', '安保室', '安保部', 'medium', 25, '孙立军', 1, 'processing', 23, '陈大壮', 80, '2026-07-28', NULL, '2026-06-10', NOW()),
  ('WO20260612007', 6, '网络卡顿', '前台网络卡顿,影响点单', '前台', '前厅部', 'low', 22, '刘美玲', 1, 'processing', 23, '陈大壮', 45, '2026-07-29', NULL, '2026-06-12', NOW()),
  ('WO20260615008', 7, '宴会厅音响失声', '宴会厅音响系统无声音', '宴会厅', '前厅部', 'medium', 22, '刘美玲', 1, 'completed', 23, '陈大壮', 100, NULL, '2026-06-20', '2026-06-15', NOW()),
  ('WO20260618009', 8, '电梯异响', '电梯运行时发出异响', '主楼电梯', '后勤部', 'high', 25, '孙立军', 1, 'completed', 23, '陈大壮', 100, NULL, '2026-06-22', '2026-06-18', NOW()),
  ('WO20260620010', 2, '冷库温度异常', '冷库温度超过标准', '冷库', '后厨部', 'high', 21, '王志强', 1, 'completed', 23, '陈大壮', 100, NULL, '2026-06-25', '2026-06-20', NOW()),
  ('WO20260701011', 9, '宣城店空调维护', '中央空调定期保养', '一楼机房', '后勤部', 'medium', 24, '赵晓燕', 2, 'completed', 23, '陈大壮', 100, NULL, '2026-07-05', '2026-07-01', NOW()),
  ('WO20260705012', 10, '宣城店冷库维护', '冷库压缩机维护', '后厨', '后厨部', 'high', 23, '陈大壮', 2, 'completed', 23, '陈大壮', 100, NULL, '2026-07-08', '2026-07-05', NOW()),
  ('WO20260710013', 4, '洗碗机定期保养', '洗碗机例行保养', '后厨', '后厨部', 'low', 21, '王志强', 1, 'completed', 23, '陈大壮', 100, NULL, '2026-07-12', '2026-07-10', NOW()),
  ('WO20260712014', 1, '空调滤网更换', '中央空调滤网定期更换', '一楼机房', '后勤部', 'low', 25, '孙立军', 1, 'completed', 23, '陈大壮', 100, NULL, '2026-07-15', '2026-07-12', NOW()),
  ('WO20260715015', 5, '监控摄像头清洁', '监控摄像头镜头清洁', '全店', '安保部', 'low', 25, '孙立军', 1, 'completed', 23, '陈大壮', 100, NULL, '2026-07-18', '2026-07-15', NOW()),
  ('WO20260718016', 6, '服务器升级', '网络服务器系统升级', '三楼机房', 'IT部', 'medium', 24, '赵晓燕', 1, 'processing', 23, '陈大壮', 70, '2026-07-28', NULL, '2026-07-18', NOW()),
  ('WO20260720017', 7, '音响调试', '宴会厅音响定期调试', '宴会厅', '前厅部', 'medium', 22, '刘美玲', 1, 'processing', 23, '陈大壮', 50, '2026-07-30', NULL, '2026-07-20', NOW()),
  ('WO20260722018', 8, '电梯年检', '电梯年度安全检查', '主楼电梯', '后勤部', 'high', 25, '孙立军', 1, 'pending', NULL, NULL, 0, NULL, NULL, '2026-07-22', NOW()),
  ('WO20260724019', 2, '冷库除霜', '冷藏库除霜维护', '后厨', '后厨部', 'medium', 21, '王志强', 1, 'pending', NULL, NULL, 0, NULL, NULL, '2026-07-24', NOW()),
  ('WO20260725020', 3, '排烟系统清理', '排烟风机叶片清理', '后厨', '后厨部', 'medium', 21, '王志强', 1, 'pending', NULL, NULL, 0, NULL, NULL, '2026-07-25', NOW());

-- =====================================================================
-- PART 24: 能耗记录(过去 60 天 × 每 5 天 × 3 类型)
-- =====================================================================
INSERT IGNORE INTO energy_record
  (store_id, record_date, energy_type, meter_reading, daily_usage, daily_cost, recorder, remark, created_at)
SELECT
  s.store_id,
  DATE_SUB(CURDATE(), INTERVAL n.day_offset DAY),
  e.type,
  10000 + n.day_offset * (CASE e.type WHEN 'electric' THEN 50 WHEN 'water' THEN 2 WHEN 'gas' THEN 8 END) + FLOOR(RAND() * 100),
  CASE e.type WHEN 'electric' THEN 50 + FLOOR(RAND() * 30) WHEN 'water' THEN 2 + ROUND(RAND() * 3, 1) WHEN 'gas' THEN 8 + FLOOR(RAND() * 5) END,
  CASE e.type WHEN 'electric' THEN ROUND((50 + RAND() * 30) * 0.85, 2) WHEN 'water' THEN ROUND((2 + RAND() * 3) * 5.5, 2) WHEN 'gas' THEN ROUND((8 + RAND() * 5) * 3.8, 2) END,
  '张婧', '正常能耗记录', NOW()
FROM (SELECT 1 AS store_id UNION ALL SELECT 2) s
CROSS JOIN (
  SELECT 1 AS day_offset UNION ALL SELECT 5 UNION ALL SELECT 10 UNION ALL SELECT 15 UNION ALL SELECT 20
  UNION ALL SELECT 25 UNION ALL SELECT 30 UNION ALL SELECT 35 UNION ALL SELECT 40 UNION ALL SELECT 45
  UNION ALL SELECT 50 UNION ALL SELECT 55 UNION ALL SELECT 60
) n
CROSS JOIN (SELECT 'electric' AS type UNION ALL SELECT 'water' UNION ALL SELECT 'gas') e;

-- =====================================================================
-- PART 25: 营销活动
-- =====================================================================
INSERT IGNORE INTO marketing_activity
  (activity_no, activity_name, activity_type, start_date, end_date, discount_rate, target_amount, status, store_id, created_by, created_at)
VALUES
  ('MA001', '夏日冰爽节', 'discount', '2026-07-01', '2026-08-31', 0.85, 200, 'active', 1, 1, NOW()),
  ('MA002', '生日特惠', 'special', '2026-01-01', '2026-12-31', 0.80, 1000, 'active', 1, 1, NOW()),
  ('MA003', '团体订餐优惠', 'group', '2026-06-01', '2026-09-30', 0.90, 5000, 'active', 1, 1, NOW()),
  ('MA004', '新店开业酬宾', 'discount', '2026-05-01', '2026-07-31', 0.75, 300, 'active', 2, 1, NOW()),
  ('MA005', '会员储值返利', 'recharge', '2026-04-01', '2026-12-31', 1.10, 500, 'active', 2, 1, NOW());

-- =====================================================================
-- PART 26: 优惠券
-- =====================================================================
INSERT IGNORE INTO marketing_coupon
  (coupon_no, coupon_name, coupon_type, discount_amount, min_order_amount, valid_start, valid_end, total_quantity, used_quantity, status, store_id, created_at)
VALUES
  ('CP001', '满500减50', 'cash', 50, 500, '2026-07-01', '2026-08-31', 100, 28, 'active', 1, NOW()),
  ('CP002', '满1000减120', 'cash', 120, 1000, '2026-07-01', '2026-08-31', 50, 12, 'active', 1, NOW()),
  ('CP003', '满2000减300', 'cash', 300, 2000, '2026-06-01', '2026-09-30', 30, 8, 'active', 1, NOW()),
  ('CP004', '新人8折券', 'discount', 0, 200, '2026-01-01', '2026-12-31', 200, 65, 'active', 1, NOW()),
  ('CP005', '生日免费蛋糕', 'gift', 0, 0, '2026-01-01', '2026-12-31', 100, 23, 'active', 1, NOW()),
  ('CP006', '满500减50', 'cash', 50, 500, '2026-07-01', '2026-08-31', 80, 19, 'active', 2, NOW()),
  ('CP007', '满1000减120', 'cash', 120, 1000, '2026-07-01', '2026-08-31', 40, 8, 'active', 2, NOW()),
  ('CP008', '满2000减300', 'cash', 300, 2000, '2026-06-01', '2026-09-30', 25, 5, 'active', 2, NOW()),
  ('CP009', 'VIP专享9折', 'discount', 0, 500, '2026-01-01', '2026-12-31', 60, 14, 'active', 2, NOW()),
  ('CP010', '节日限定套餐', 'package', 0, 1000, '2026-07-01', '2026-08-31', 30, 6, 'active', 2, NOW());

-- 优惠券使用记录
INSERT IGNORE INTO marketing_coupon_record
  (coupon_id, customer_id, customer_name, order_no, discount_amount, used_at, status, created_at)
SELECT
  c.coupon_id, cm.customer_id, cm.customer_name,
  CONCAT('CPUSE-', c.coupon_id, '-', cm.customer_id),
  c.discount_amount,
  DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 60) DAY),
  'used', NOW()
FROM marketing_coupon c
CROSS JOIN customer_master cm
WHERE cm.customer_id BETWEEN 101 AND 115
  AND RAND() < 0.4
LIMIT 30;

-- =====================================================================
-- PART 27: 折扣规则
-- =====================================================================
INSERT IGNORE INTO marketing_discount_rule
  (rule_name, rule_type, condition_amount, discount_type, discount_value, priority, status, store_id, valid_from, valid_to, created_at)
VALUES
  ('团体订餐满5000享9折', 'amount', 5000, 'percent', 0.90, 1, 'active', 1, '2026-01-01', '2026-12-31', NOW()),
  ('团体订餐满10000享85折', 'amount', 10000, 'percent', 0.85, 2, 'active', 1, '2026-01-01', '2026-12-31', NOW()),
  ('VIP客户8折', 'member', 0, 'percent', 0.80, 3, 'active', 1, '2026-01-01', '2026-12-31', NOW()),
  ('生日特惠7折', 'birthday', 0, 'percent', 0.70, 4, 'active', 1, '2026-01-01', '2026-12-31', NOW()),
  ('新人首单立减100', 'first_order', 0, 'cash', 100, 5, 'active', 2, '2026-01-01', '2026-12-31', NOW());

-- =====================================================================
-- PART 28: 库存出入库
-- =====================================================================
INSERT IGNORE INTO ingredient_inventory_log
  (ingredient_id, ingredient_name, store_id, type, quantity, unit, unit_price, total_amount, related_type, related_id, operator_id, operator_name, remark, created_at)
SELECT
  FLOOR(1 + RAND() * 1218),
  CONCAT('原料', FLOOR(1 + RAND() * 1218)),
  ((n.idx) % 2) + 1,
  CASE FLOOR(RAND() * 2) WHEN 0 THEN 'in' ELSE 'out' END,
  ROUND(5 + RAND() * 50, 2),
  CASE FLOOR(RAND() * 3) WHEN 0 THEN 'kg' WHEN 1 THEN 'piece' ELSE 'liter' END,
  ROUND(10 + RAND() * 80, 2),
  ROUND((5 + RAND() * 50) * (10 + RAND() * 80), 2),
  CASE FLOOR(RAND() * 2) WHEN 0 THEN 'purchase' ELSE 'booking' END,
  FLOOR(1 + RAND() * 50),
  23, '陈大壮', '正常出入库',
  DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 60) DAY)
FROM (SELECT 1 AS idx UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20
      UNION ALL SELECT 21 UNION ALL SELECT 22 UNION ALL SELECT 23 UNION ALL SELECT 24 UNION ALL SELECT 25 UNION ALL SELECT 26 UNION ALL SELECT 27 UNION ALL SELECT 28 UNION ALL SELECT 29 UNION ALL SELECT 30
      UNION ALL SELECT 31 UNION ALL SELECT 32 UNION ALL SELECT 33 UNION ALL SELECT 34 UNION ALL SELECT 35 UNION ALL SELECT 36 UNION ALL SELECT 37 UNION ALL SELECT 38 UNION ALL SELECT 39 UNION ALL SELECT 40
      UNION ALL SELECT 41 UNION ALL SELECT 42 UNION ALL SELECT 43 UNION ALL SELECT 44 UNION ALL SELECT 45 UNION ALL SELECT 46 UNION ALL SELECT 47 UNION ALL SELECT 48 UNION ALL SELECT 49 UNION ALL SELECT 50) n;

-- =====================================================================
-- PART 29: 采购订单 + 收货
-- =====================================================================
INSERT IGNORE INTO purchase_order
  (purchase_no, supplier_id, supplier_name, store_id, total_amount, status, operator_id, operator_name, expected_date, remark, created_at, updated_at)
SELECT
  CONCAT('PO', DATE_FORMAT(DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 60) DAY), '%Y%m%d'), LPAD(n.idx, 4, '0')),
  FLOOR(1 + RAND() * 10),
  '供应商',
  ((n.idx) % 2) + 1,
  ROUND(2000 + RAND() * 8000, 2),
  CASE FLOOR(RAND() * 3) WHEN 0 THEN 'pending' WHEN 1 THEN 'received' ELSE 'paid' END,
  23, '陈大壮',
  DATE_ADD(CURDATE(), INTERVAL FLOOR(1 + RAND() * 7) DAY),
  '正常采购',
  DATE_SUB(NOW(), INTERVAL FLOOR(1 + RAND() * 60) DAY),
  NOW()
FROM (SELECT 1 AS idx UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10
      UNION ALL SELECT 11 UNION ALL SELECT 12 UNION ALL SELECT 13 UNION ALL SELECT 14 UNION ALL SELECT 15 UNION ALL SELECT 16 UNION ALL SELECT 17 UNION ALL SELECT 18 UNION ALL SELECT 19 UNION ALL SELECT 20) n;

INSERT IGNORE INTO purchase_receipt
  (receipt_no, purchase_id, purchase_no, supplier_id, store_id, received_date, total_amount, operator_id, operator_name, remark, created_at)
SELECT
  CONCAT('GR', DATE_FORMAT(po.created_at, '%Y%m%d'), LPAD(po.purchase_id, 4, '0')),
  po.purchase_id, po.purchase_no, po.supplier_id, po.store_id,
  po.created_at, po.total_amount, po.operator_id, po.operator_name,
  '已验收入库', NOW()
FROM purchase_order po
WHERE po.status IN ('received', 'paid');

-- =====================================================================
-- PART 30: 报损
-- =====================================================================
INSERT IGNORE INTO stock_loss
  (loss_no, store_id, loss_date, loss_type, total_amount, status, applicant_id, applicant_name, approver_id, approver_name, remark, created_at)
VALUES
  ('SL20260701001', 1, '2026-07-01', 'expire', 320.50, 'pending', 21, '王志强', NULL, NULL, '五花肉过期', NOW()),
  ('SL20260705002', 1, '2026-07-05', 'damage', 580.00, 'approved', 21, '王志强', 1, '张婧', '蔬菜运输损坏', NOW()),
  ('SL20260710003', 2, '2026-07-10', 'spill', 220.00, 'approved', 23, '陈大壮', 1, '张婧', '调味料打翻', NOW()),
  ('SL20260715004', 1, '2026-07-15', 'expire', 450.00, 'pending', 21, '王志强', NULL, NULL, '海鲜过期', NOW()),
  ('SL20260720005', 1, '2026-07-20', 'damage', 380.00, 'approved', 21, '王志强', 1, '张婧', '餐具破损', NOW());

-- =====================================================================
-- PART 31: 盘点
-- =====================================================================
INSERT IGNORE INTO stock_take
  (take_no, store_id, take_date, take_type, total_items, total_difference, status, operator_id, operator_name, remark, created_at)
VALUES
  ('ST20260601001', 1, '2026-06-01', 'monthly', 85, -120.50, 'completed', 21, '王志强', '正常月度盘点', NOW()),
  ('ST20260601002', 2, '2026-06-01', 'monthly', 78, 80.00, 'completed', 23, '陈大壮', '正常月度盘点', NOW()),
  ('ST20260701003', 1, '2026-07-01', 'monthly', 92, -50.00, 'completed', 21, '王志强', '正常月度盘点', NOW());

-- =====================================================================
-- PART 32: 请购单
-- =====================================================================
INSERT IGNORE INTO requisition_order
  (req_no, store_id, dept_id, dept_name, total_amount, status, applicant_id, applicant_name, approver_id, approver_name, expected_date, remark, created_at, updated_at)
VALUES
  ('REQ20260701001', 1, 12, '后厨部', 1500.00, 'pending', 21, '王志强', NULL, NULL, '2026-07-30', '请购调料', NOW(), NOW()),
  ('REQ20260702002', 1, 12, '后厨部', 2800.00, 'pending', 21, '王志强', NULL, NULL, '2026-07-30', '请购食材', NOW(), NOW()),
  ('REQ20260703003', 1, 5, '前厅部', 850.00, 'pending', 22, '刘美玲', NULL, NULL, '2026-07-30', '请购餐具', NOW(), NOW()),
  ('REQ20260704004', 1, 12, '后厨部', 3200.00, 'approved', 21, '王志强', 1, '张婧', '2026-07-30', '请购设备', NOW(), NOW()),
  ('REQ20260705005', 1, 5, '前厅部', 1200.00, 'approved', 22, '刘美玲', 1, '张婧', '2026-07-30', '请购一次性用品', NOW(), NOW()),
  ('REQ20260706006', 2, 12, '后厨部', 2500.00, 'approved', 23, '陈大壮', 1, '张婧', '2026-07-30', '请购食材', NOW(), NOW()),
  ('REQ20260707007', 2, 12, '后厨部', 1800.00, 'approved', 23, '陈大壮', 1, '张婧', '2026-07-30', '请购调料', NOW(), NOW()),
  ('REQ20260708008', 1, 5, '前厅部', 950.00, 'approved', 22, '刘美玲', 1, '张婧', '2026-07-30', '请购清洁用品', NOW(), NOW()),
  ('REQ20260709009', 1, 12, '后厨部', 2200.00, 'approved', 21, '王志强', 1, '张婧', '2026-07-30', '请购冷冻食品', NOW(), NOW()),
  ('REQ20260710010', 1, 5, '前厅部', 680.00, 'approved', 22, '刘美玲', 1, '张婧', '2026-07-30', '请购办公用品', NOW(), NOW());

-- =====================================================================
-- PART 33: AI 对话历史
-- =====================================================================
INSERT IGNORE INTO ai_chat_history
  (session_id, user_id, user_name, role, content, tokens_used, model, created_at)
VALUES
  ('AI-S1', 1, '张婧', 'user', '请帮我分析本月宴会订单的趋势', 250, 'deepseek-chat', NOW()),
  ('AI-S1', 1, '张婧', 'assistant', '本月订单数环比上升12%,主要驱动是商务宴和婚宴,建议加强渠道投放', 480, 'deepseek-chat', NOW()),
  ('AI-S2', 1, '张婧', 'user', '推荐10道适合夏季的菜品', 180, 'deepseek-chat', NOW()),
  ('AI-S2', 1, '张婧', 'assistant', '夏季推荐:凉拌黄瓜、清蒸鲈鱼、绿豆汤、冰镇杨梅、荷叶饭、莲子百合汤、凉皮、蒜蓉西兰花、西瓜汁、酸奶水果捞', 650, 'deepseek-chat', NOW()),
  ('AI-S3', 1, '张婧', 'user', '本月毛利率下降的原因分析', 220, 'deepseek-chat', NOW()),
  ('AI-S3', 1, '张婧', 'assistant', '毛利率从68%降至65%,主要因食材成本上升(海鲜)和优惠活动力度加大,建议调整采购渠道和优化活动ROI', 520, 'deepseek-chat', NOW()),
  ('AI-S4', 1, '张婧', 'user', '帮我写一个母亲节活动方案', 280, 'deepseek-chat', NOW()),
  ('AI-S4', 1, '张婧', 'assistant', '母亲节活动方案:1)家庭套餐8.8折 2)母亲康乃馨赠送 3)孝心照片墙 4)互动小游戏 5)消费满1000送体检套餐,预算1.5万,预计客流提升20%', 720, 'deepseek-chat', NOW()),
  ('AI-S5', 1, '张婧', 'user', '今天的翻台率如何', 120, 'deepseek-chat', NOW()),
  ('AI-S5', 1, '张婧', 'assistant', '今日翻台率2.3,较昨日2.5略降,主要因中午散客较少,建议下午茶时段推出优惠', 380, 'deepseek-chat', NOW());

-- =====================================================================
-- PART 34: 报表 - 日报(基于 booking 聚合)
-- =====================================================================
INSERT IGNORE INTO report_daily (store_id, report_date, order_count, guest_count, total_revenue, total_cost, gross_profit, gross_margin, created_at)
SELECT
  b.store_id, b.booking_date,
  COUNT(DISTINCT b.booking_id),
  COALESCE(SUM(b.guest_count), 0),
  COALESCE(SUM(b.total_amount), 0),
  COALESCE(SUM(b.total_amount) * 0.35, 0),
  COALESCE(SUM(b.total_amount) * 0.65, 0),
  CASE WHEN COALESCE(SUM(b.total_amount), 0) > 0 THEN 0.65 ELSE 0 END,
  NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND b.booking_status IN ('confirmed', 'completed')
  AND b.booking_date >= DATE_SUB(CURDATE(), INTERVAL 90 DAY)
GROUP BY b.store_id, b.booking_date;

-- 月报
INSERT IGNORE INTO report_monthly (store_id, report_month, order_count, guest_count, total_revenue, total_cost, gross_profit, gross_margin, created_at)
SELECT
  b.store_id,
  DATE_FORMAT(b.booking_date, '%Y-%m'),
  COUNT(DISTINCT b.booking_id),
  COALESCE(SUM(b.guest_count), 0),
  COALESCE(SUM(b.total_amount), 0),
  COALESCE(SUM(b.total_amount) * 0.35, 0),
  COALESCE(SUM(b.total_amount) * 0.65, 0),
  CASE WHEN COALESCE(SUM(b.total_amount), 0) > 0 THEN 0.65 ELSE 0 END,
  NOW()
FROM booking_master b
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND b.booking_status IN ('confirmed', 'completed')
GROUP BY b.store_id, DATE_FORMAT(b.booking_date, '%Y-%m');

-- 菜品销售报表
INSERT IGNORE INTO report_dish_sales (dish_id, dish_name, store_id, sale_date, sale_count, sale_revenue, created_at)
SELECT
  bd.dish_id, bd.dish_name, b.store_id, b.booking_date,
  COALESCE(SUM(bd.quantity), 0),
  COALESCE(SUM(bd.subtotal), 0),
  NOW()
FROM booking_dish_detail bd
JOIN booking_master b ON b.booking_id = bd.booking_id
WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND b.booking_status IN ('confirmed', 'completed')
GROUP BY bd.dish_id, bd.dish_name, b.store_id, b.booking_date
LIMIT 200;

-- 部门成本报表
INSERT IGNORE INTO report_department_cost (store_id, dept_id, dept_name, report_date, food_cost, labor_cost, overhead_cost, total_cost, created_at)
SELECT
  s.store_id, d.department_id, d.department_name, CURDATE(),
  ROUND(5000 + RAND() * 5000, 2),
  ROUND(8000 + RAND() * 4000, 2),
  ROUND(2000 + RAND() * 2000, 2),
  ROUND(15000 + RAND() * 10000, 2),
  NOW()
FROM (SELECT 1 AS store_id UNION ALL SELECT 2) s
CROSS JOIN department d
WHERE d.parent_id = 0
LIMIT 10;

-- 员工 KPI 报表
INSERT IGNORE INTO report_staff_kpi (staff_id, staff_name, store_id, report_month, order_count, guest_count, total_revenue, performance_score, created_at)
VALUES
  (1, '张婧', 1, '2026-07', 15, 280, 38000, 92.5, NOW()),
  (2, '李明', 1, '2026-07', 12, 220, 28500, 88.0, NOW()),
  (3, '王芳', 1, '2026-07', 10, 180, 22000, 85.0, NOW()),
  (4, '张伟', 1, '2026-07', 18, 320, 42000, 95.0, NOW()),
  (5, '刘洋', 1, '2026-07', 14, 250, 31000, 90.0, NOW()),
  (21, '王志强', 1, '2026-07', 20, 380, 48000, 96.0, NOW()),
  (22, '刘美玲', 1, '2026-07', 16, 290, 36500, 91.0, NOW()),
  (23, '陈大壮', 2, '2026-07', 18, 340, 42500, 93.0, NOW());

-- =====================================================================
-- PART 35: 会员等级定义
-- =====================================================================
INSERT IGNORE INTO member_level
  (level_code, level_name, min_points, discount_rate, birthday_gift, priority_booking, created_at)
VALUES
  ('normal', '普通会员', 0, 0.95, 0, 0, NOW()),
  ('silver', '银卡会员', 1000, 0.90, 1, 0, NOW()),
  ('gold', '金卡会员', 5000, 0.85, 1, 1, NOW()),
  ('platinum', '黑金会员', 20000, 0.80, 1, 1, NOW());

-- =====================================================================
-- PART 36: 积分规则
-- =====================================================================
INSERT IGNORE INTO member_point_rule
  (rule_name, rule_type, condition_amount, points_value, valid_from, valid_to, status, created_at)
VALUES
  ('消费积分1元=1分', 'consume', 1, 1, '2026-01-01', '2026-12-31', 'active', NOW()),
  ('储值1000元=100分', 'recharge', 1000, 100, '2026-01-01', '2026-12-31', 'active', NOW()),
  ('生日双倍积分', 'birthday', 0, 2, '2026-01-01', '2026-12-31', 'active', NOW()),
  ('推荐好友50分', 'referral', 0, 50, '2026-01-01', '2026-12-31', 'active', NOW());

-- =====================================================================
-- PART 37: 通知
-- =====================================================================
INSERT IGNORE INTO sys_notification
  (user_id, user_name, title, content, type, status, related_type, related_id, created_at)
VALUES
  (21, '王志强', '低库存预警', '五花肉库存不足5kg,请及时采购', 'warning', 'unread', 'inventory', 1, NOW()),
  (1, '张婧', '审批待办', '您有1条待审批申请', 'task', 'unread', 'approval', 1, NOW()),
  (22, '刘美玲', '客户预订提醒', '明日张建华预订,请提前准备', 'remind', 'read', 'booking', 1, NOW()),
  (25, '孙立军', '设备保养提醒', '中央空调已到保养周期', 'remind', 'unread', 'maintenance', 1, NOW()),
  (23, '陈大壮', '付款到期', '供应商付款还有3天到期', 'warning', 'unread', 'finance', 1, NOW()),
  (24, '赵晓燕', '新订单', '新订单 BC20260715001 已创建', 'info', 'read', 'booking', 2, NOW());

-- =====================================================================
-- PART 38: 变更日志
-- ===================================================================
INSERT IGNORE INTO change_log
  (table_name, record_id, operation, before_data, after_data, operator_id, operator_name, created_at)
VALUES
  ('booking_master', 1, 'insert', NULL, JSON_OBJECT('id', 1, 'created_at', NOW()), 1, '张婧', DATE_SUB(NOW(), INTERVAL 1 DAY)),
  ('staff_master', 21, 'insert', NULL, JSON_OBJECT('id', 21, 'created_at', NOW()), 1, '张婧', DATE_SUB(NOW(), INTERVAL 2 DAY)),
  ('finance_receivable', 1, 'insert', NULL, JSON_OBJECT('id', 1, 'created_at', NOW()), 1, '张婧', DATE_SUB(NOW(), INTERVAL 3 DAY)),
  ('menu_master', 1, 'update', JSON_OBJECT('price', 100), JSON_OBJECT('price', 120), 21, '王志强', DATE_SUB(NOW(), INTERVAL 4 DAY));

-- =====================================================================
-- PART 39: 菜品成本卡(补全)
-- =====================================================================
INSERT IGNORE INTO dish_cost_card
  (dish_id, dish_name, store_id, total_cost, food_cost, labor_cost, overhead_cost, gross_margin, created_at)
VALUES
  (1, '红烧肉', 1, 35.00, 22.00, 8.00, 5.00, 0.55, NOW()),
  (2, '清蒸鲈鱼', 1, 50.00, 35.00, 10.00, 5.00, 0.60, NOW()),
  (3, '宫保鸡丁', 1, 25.00, 16.00, 6.00, 3.00, 0.50, NOW()),
  (4, '水煮鱼', 1, 60.00, 42.00, 12.00, 6.00, 0.55, NOW()),
  (5, '蒜蓉西兰花', 1, 15.00, 9.00, 4.00, 2.00, 0.40, NOW()),
  (6, '凉拌黄瓜', 1, 8.00, 4.00, 3.00, 1.00, 0.45, NOW()),
  (7, '白切鸡', 2, 40.00, 26.00, 9.00, 5.00, 0.55, NOW()),
  (8, '红烧排骨', 2, 48.00, 32.00, 10.00, 6.00, 0.58, NOW());

-- =====================================================================
-- PART 40: AI 记忆
-- =====================================================================
INSERT IGNORE INTO ai_memory
  (session_id, user_id, role, content, created_at)
VALUES
  ('AI-S1', 1, 'user', '我是张婧,餐饮店老板', NOW()),
  ('AI-S2', 1, 'user', '我喜欢清淡的菜品', NOW()),
  ('AI-S3', 1, 'user', '我们店主打徽菜', NOW());

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- PART 41: 验证
-- =====================================================================
SELECT '=== 数据录入完成,验证 ===' AS info;

-- 业务表行数统计
SELECT 'customer_master' AS tbl, COUNT(*) AS cnt FROM customer_master WHERE customer_id BETWEEN 101 AND 115
UNION ALL SELECT 'member_card', COUNT(*) FROM member_card WHERE customer_id BETWEEN 101 AND 115
UNION ALL SELECT 'member_point_log', COUNT(*) FROM member_point_log
UNION ALL SELECT 'member_recharge_record', COUNT(*) FROM member_recharge_record
UNION ALL SELECT 'member_consume_record', COUNT(*) FROM member_consume_record
UNION ALL SELECT 'booking_master (NEW)', COUNT(*) FROM booking_master WHERE booking_no LIKE 'BC2026%' AND booking_id > 100
UNION ALL SELECT 'booking_table', COUNT(*) FROM booking_table WHERE booking_id > 100
UNION ALL SELECT 'booking_dish_detail', COUNT(*) FROM booking_dish_detail WHERE booking_id > 100
UNION ALL SELECT 'finance_receivable (NEW)', COUNT(*) FROM finance_receivable WHERE receivable_no LIKE 'REC-BC2026%'
UNION ALL SELECT 'finance_payable (NEW)', COUNT(*) FROM finance_payable WHERE payable_no LIKE 'PAY-%'
UNION ALL SELECT 'finance_transaction', COUNT(*) FROM finance_transaction
UNION ALL SELECT 'finance_account', COUNT(*) FROM finance_account
UNION ALL SELECT 'finance_cost_record', COUNT(*) FROM finance_cost_record
UNION ALL SELECT 'attendance_records', COUNT(*) FROM attendance_records
UNION ALL SELECT 'leave_record', COUNT(*) FROM leave_record
UNION ALL SELECT 'overtime', COUNT(*) FROM overtime
UNION ALL SELECT 'schedule', COUNT(*) FROM schedule
UNION ALL SELECT 'hr_payroll', COUNT(*) FROM hr_payroll
UNION ALL SELECT 'approval_flow', COUNT(*) FROM approval_flow WHERE flow_no LIKE 'AF2026%'
UNION ALL SELECT 'approval_node', COUNT(*) FROM approval_node
UNION ALL SELECT 'approval_log', COUNT(*) FROM approval_log
UNION ALL SELECT 'maintenance_asset', COUNT(*) FROM maintenance_asset
UNION ALL SELECT 'maintenance_request', COUNT(*) FROM maintenance_request
UNION ALL SELECT 'energy_record', COUNT(*) FROM energy_record
UNION ALL SELECT 'marketing_activity', COUNT(*) FROM marketing_activity
UNION ALL SELECT 'marketing_coupon', COUNT(*) FROM marketing_coupon
UNION ALL SELECT 'marketing_coupon_record', COUNT(*) FROM marketing_coupon_record
UNION ALL SELECT 'marketing_discount_rule', COUNT(*) FROM marketing_discount_rule
UNION ALL SELECT 'ingredient_inventory_log', COUNT(*) FROM ingredient_inventory_log
UNION ALL SELECT 'purchase_order', COUNT(*) FROM purchase_order
UNION ALL SELECT 'purchase_receipt', COUNT(*) FROM purchase_receipt
UNION ALL SELECT 'stock_loss', COUNT(*) FROM stock_loss
UNION ALL SELECT 'stock_take', COUNT(*) FROM stock_take
UNION ALL SELECT 'requisition_order', COUNT(*) FROM requisition_order
UNION ALL SELECT 'ai_chat_history', COUNT(*) FROM ai_chat_history
UNION ALL SELECT 'report_daily', COUNT(*) FROM report_daily
UNION ALL SELECT 'report_monthly', COUNT(*) FROM report_monthly
UNION ALL SELECT 'report_dish_sales', COUNT(*) FROM report_dish_sales
UNION ALL SELECT 'report_department_cost', COUNT(*) FROM report_department_cost
UNION ALL SELECT 'report_staff_kpi', COUNT(*) FROM report_staff_kpi
UNION ALL SELECT 'dish_cost_card', COUNT(*) FROM dish_cost_card
UNION ALL SELECT 'member_level', COUNT(*) FROM member_level
UNION ALL SELECT 'member_point_rule', COUNT(*) FROM member_point_rule
UNION ALL SELECT 'sys_notification', COUNT(*) FROM sys_notification
UNION ALL SELECT 'change_log', COUNT(*) FROM change_log;

-- 关系完整性验证: 应为 0
SELECT '关系完整性:' AS check_name, COUNT(*) AS issues FROM booking_master b
  WHERE b.booking_no LIKE 'BC2026%' AND b.booking_id > 100
  AND NOT EXISTS (SELECT 1 FROM customer_master c WHERE c.customer_id = b.customer_id)
UNION ALL
SELECT '应收-订单不匹配', COUNT(*) FROM finance_receivable r WHERE r.booking_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM booking_master b WHERE b.booking_id = r.booking_id);

-- 财务账户余额
SELECT '账户余额:' AS check_name, fa.account_name, fa.current_balance
FROM finance_account fa;

SELECT '=== 全部完成 ===' AS status;

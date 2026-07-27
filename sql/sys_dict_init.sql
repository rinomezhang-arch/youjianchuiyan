-- ============================================================
-- 数据字典初始化数据
-- 基于 sys_dict / sys_dict_item 表（真实数据库表结构）
-- 数据库: banquet (MySQL 8.x)
-- ============================================================

-- 1. 宴席类型 occasion_type
INSERT INTO sys_dict (dict_code, dict_name, dict_type, store_id, description, sort_order, is_active)
VALUES ('occasion_type', '宴席类型', 'list', 1, '宴席/宴会类型字典', 1, 1);
SET @occasion_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, dict_code, item_value, item_label, store_id, sort_order, is_active) VALUES
(@occasion_id, 'occasion_type', 'a_la_carte',   '零点',   1, 1,  1),
(@occasion_id, 'occasion_type', 'wedding',      '婚宴',   1, 2,  1),
(@occasion_id, 'occasion_type', 'birthday',    '生日宴', 1, 3,  1),
(@occasion_id, 'occasion_type', 'engagement',   '订婚宴', 1, 4,  1),
(@occasion_id, 'occasion_type', 'baby_born',   '满月宴', 1, 5,  1),
(@occasion_id, 'occasion_type', 'graduation',  '谢师宴', 1, 6,  1),
(@occasion_id, 'occasion_type', 'help_wine',   '帮忙酒', 1, 7,  1),
(@occasion_id, 'occasion_type', 'house_move',  '乔迁宴', 1, 8,  1),
(@occasion_id, 'occasion_type', 'promotion',   '升迁宴', 1, 9,  1),
(@occasion_id, 'occasion_type', 'reunion',     '团圆宴', 1, 10, 1),
(@occasion_id, 'occasion_type', 'thanksgiving','答谢宴', 1, 11, 1),
(@occasion_id, 'occasion_type', 'year_end',    '尾牙宴', 1, 12, 1);

-- 2. 客户来源 source_type
INSERT INTO sys_dict (dict_code, dict_name, dict_type, store_id, description, sort_order, is_active)
VALUES ('source_type', '客户来源', 'list', 1, '客户来源渠道字典', 2, 1);
SET @source_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, dict_code, item_value, item_label, store_id, sort_order, is_active) VALUES
(@source_id, 'source_type', 'WALKIN',  '上门散客', 1, 1, 1),
(@source_id, 'source_type', 'OTA',     '线上客户', 1, 2, 1),
(@source_id, 'source_type', 'CORP',    '企业协议', 1, 3, 1),
(@source_id, 'source_type', 'TOUR',    '旅游团队', 1, 4, 1),
(@source_id, 'source_type', 'CONF',    '会议团队', 1, 5, 1),
(@source_id, 'source_type', 'DIRECT',  '自有客户', 1, 6, 1),
(@source_id, 'source_type', 'REF',     '朋友介绍', 1, 7, 1),
(@source_id, 'source_type', 'LOCAL',   '周边社区', 1, 8, 1),
(@source_id, 'source_type', 'PER',     '亲朋好友', 1, 9, 1);

-- 3. 预定类型 booking_type
INSERT INTO sys_dict (dict_code, dict_name, dict_type, store_id, description, sort_order, is_active)
VALUES ('booking_type', '预定类型', 'list', 1, '预订类型字典', 3, 1);
SET @booking_type_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, dict_code, item_value, item_label, store_id, sort_order, is_active) VALUES
(@booking_type_id, 'booking_type', 'direct',  '直接预定', 1, 1, 1),
(@booking_type_id, 'booking_type', 'pending', '客户待定', 1, 2, 1);

-- 4. 餐别时间 time_slot
INSERT INTO sys_dict (dict_code, dict_name, dict_type, store_id, description, sort_order, is_active)
VALUES ('time_slot', '餐别时间', 'list', 1, '餐别时间段字典', 4, 1);
SET @time_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, dict_code, item_value, item_label, store_id, sort_order, is_active) VALUES
(@time_id, 'time_slot', 'lunch_11:00',  '午餐 11:00', 1, 1, 1),
(@time_id, 'time_slot', 'lunch_11:30',  '午餐 11:30', 1, 2, 1),
(@time_id, 'time_slot', 'lunch_12:00',  '午餐 12:00', 1, 3, 1),
(@time_id, 'time_slot', 'dinner_17:00', '晚餐 17:00', 1, 4, 1),
(@time_id, 'time_slot', 'dinner_17:30', '晚餐 17:30', 1, 5, 1),
(@time_id, 'time_slot', 'dinner_18:00', '晚餐 18:00', 1, 6, 1),
(@time_id, 'time_slot', 'dinner_18:30', '晚餐 18:30', 1, 7, 1),
(@time_id, 'time_slot', 'dinner_19:00', '晚餐 19:00', 1, 8, 1);

-- 5. 预定状态 booking_status
INSERT INTO sys_dict (dict_code, dict_name, dict_type, store_id, description, sort_order, is_active)
VALUES ('booking_status', '预定状态', 'list', 1, '预订状态字典', 5, 1);
SET @status_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, dict_code, item_value, item_label, store_id, sort_order, is_active) VALUES
(@status_id, 'booking_status', 'pending',   '待确认', 1, 1, 1),
(@status_id, 'booking_status', 'confirmed', '已确认', 1, 2, 1),
(@status_id, 'booking_status', 'completed', '已完成', 1, 3, 1),
(@status_id, 'booking_status', 'cancelled', '已取消', 1, 4, 1);

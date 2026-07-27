-- ============================================================
-- 数据字典表设计
-- 数据库: banquet (MySQL 8.x)
-- 目的: 保证 occasion_type / source_type / booking_type / time_slot
--       等字段的数据有效性，支持后台维护
-- ============================================================

-- 字典类型表
CREATE TABLE IF NOT EXISTS sys_dict_type (
    dict_id INT PRIMARY KEY AUTO_INCREMENT,
    dict_code VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码，如 occasion_type',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称，如 宴席类型',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用 1=是 0=否',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典类型表';

-- 字典项表
CREATE TABLE IF NOT EXISTS sys_dict_item (
    item_id INT PRIMARY KEY AUTO_INCREMENT,
    dict_id INT NOT NULL COMMENT '关联字典类型ID',
    item_value VARCHAR(50) NOT NULL COMMENT '字典项值，如 wedding',
    item_label VARCHAR(100) NOT NULL COMMENT '字典项标签，如 婚宴',
    item_en VARCHAR(100) COMMENT '英文标签，如 Wedding',
    is_active TINYINT DEFAULT 1 COMMENT '是否启用',
    sort_order INT DEFAULT 0 COMMENT '排序',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_dict_value (dict_id, item_value),
    CONSTRAINT fk_dict_item_type FOREIGN KEY (dict_id) REFERENCES sys_dict_type(dict_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据字典项表';

-- ============================================================
-- 初始化数据
-- ============================================================

-- 1. 宴席类型 occasion_type
INSERT INTO sys_dict_type (dict_code, dict_name, sort_order) VALUES ('occasion_type', '宴席类型', 1);
SET @occasion_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, item_value, item_label, item_en, sort_order) VALUES
(@occasion_id, 'a_la_carte',   '零点',   'A la Carte',     1),
(@occasion_id, 'wedding',      '婚宴',   'Wedding',        2),
(@occasion_id, 'birthday',    '生日宴', 'Birthday',       3),
(@occasion_id, 'engagement',   '订婚宴', 'Engagement',     4),
(@occasion_id, 'baby_born',   '满月宴', 'Baby Born',      5),
(@occasion_id, 'graduation',  '谢师宴', 'Graduation',     6),
(@occasion_id, 'help_wine',   '帮忙酒', 'Help Wine',      7),
(@occasion_id, 'house_move',  '乔迁宴', 'House Move',     8),
(@occasion_id, 'promotion',   '升迁宴', 'Promotion',      9),
(@occasion_id, 'reunion',     '团圆宴', 'Reunion',        10),
(@occasion_id, 'thanksgiving','答谢宴', 'Thanksgiving',  11),
(@occasion_id, 'year_end',    '尾牙宴', 'Year End',      12);

-- 2. 客户来源 source_type
INSERT INTO sys_dict_type (dict_code, dict_name, sort_order) VALUES ('source_type', '客户来源', 2);
SET @source_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, item_value, item_label, item_en, sort_order) VALUES
(@source_id, 'WALKIN',  '上门散客',   'Walk-in',      1),
(@source_id, 'OTA',     '线上客户',   'OTA',           2),
(@source_id, 'CORP',    '企业协议',   'Corporate',     3),
(@source_id, 'TOUR',    '旅游团队',   'Tour Group',    4),
(@source_id, 'CONF',    '会议团队',   'Conference',   5),
(@source_id, 'DIRECT',  '自有客户',   'Direct',        6),
(@source_id, 'REF',     '朋友介绍',   'Referral',      7),
(@source_id, 'LOCAL',   '周边社区',   'Local',         8),
(@source_id, 'PER',     '亲朋好友',   'Personal',      9);

-- 3. 预定类型 booking_type
INSERT INTO sys_dict_type (dict_code, dict_name, sort_order) VALUES ('booking_type', '预定类型', 3);
SET @booking_type_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, item_value, item_label, item_en, sort_order) VALUES
(@booking_type_id, 'direct',  '直接预定', 'Direct',   1),
(@booking_type_id, 'pending', '客户待定', 'Pending',  2);

-- 4. 餐别时间 time_slot
INSERT INTO sys_dict_type (dict_code, dict_name, sort_order) VALUES ('time_slot', '餐别时间', 4);
SET @time_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, item_value, item_label, item_en, sort_order) VALUES
(@time_id, 'lunch_11:00',  '11:00', 'Lunch', 1),
(@time_id, 'lunch_11:30',  '11:30', 'Lunch', 2),
(@time_id, 'lunch_12:00',  '12:00', 'Lunch', 3),
(@time_id, 'dinner_17:00', '17:00', 'Dinner', 4),
(@time_id, 'dinner_17:30', '17:30', 'Dinner', 5),
(@time_id, 'dinner_18:00', '18:00', 'Dinner', 6),
(@time_id, 'dinner_18:30', '18:30', 'Dinner', 7),
(@time_id, 'dinner_19:00', '19:00', 'Dinner', 8);

-- 5. 预定状态 booking_status
INSERT INTO sys_dict_type (dict_code, dict_name, sort_order) VALUES ('booking_status', '预定状态', 5);
SET @status_id = LAST_INSERT_ID();
INSERT INTO sys_dict_item (dict_id, item_value, item_label, item_en, sort_order) VALUES
(@status_id, 'pending',   '待确认', 'Pending',   1),
(@status_id, 'confirmed', '已确认', 'Confirmed', 2),
(@status_id, 'completed', '已完成', 'Completed', 3),
(@status_id, 'cancelled', '已取消', 'Cancelled', 4);

-- =====================================================================
-- seed_org_position_level.sql
-- 又见炊烟餐饮管理系统V2.0 - 组织系统职位表与员工级别表建表+灌入
-- 生成日期：2026-08-03
-- 维护：地龙（DL-BOT）
-- 说明：
--   1. 新建 staff_level（员工级别表）— 定义级别体系（员工级→高管级）
--   2. 新建 position（职位表）— 定义具体职位，关联岗位post和级别staff_level
--   3. UPDATE staff_master.staff_rank 关联到级别名称
--   4. 不修改已有表 schema，仅新建表 + 数据灌入
--
-- 表关系：
--   department(部门) → 1:N → post(岗位) → 1:N → position(职位)
--                                                  ↓ N:1
--                                              staff_level(级别)
--                                                  ↓ 1:N
--                                              staff_master(员工) 通过 staff_rank 字段关联
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================================
-- 一、新建 staff_level 表（员工级别表）
--    定义餐饮企业的6级管理体系：
--    L1=员工级(基层) → L2=领班级 → L3=主管级 → L4=经理级 → L5=总监级 → L6=高管级
-- =====================================================================

DROP TABLE IF EXISTS `position`;
DROP TABLE IF EXISTS `staff_level`;

CREATE TABLE `staff_level` (
  `level_id` int NOT NULL AUTO_INCREMENT COMMENT '级别ID',
  `level_code` varchar(20) NOT NULL COMMENT '级别编码(STAFF/LEAD/SUPERVISOR/MANAGER/DIRECTOR/EXECUTIVE)',
  `level_name` varchar(30) NOT NULL COMMENT '级别名称(员工级/领班级/主管级/经理级/总监级/高管级)',
  `level_rank` int NOT NULL COMMENT '级别序号(1=最低,6=最高)',
  `base_salary_min` decimal(10,2) DEFAULT NULL COMMENT '该级别最低月薪参考',
  `base_salary_max` decimal(10,2) DEFAULT NULL COMMENT '该级别最高月薪参考',
  `description` varchar(200) DEFAULT NULL COMMENT '级别说明',
  `status` varchar(10) DEFAULT 'active' COMMENT '状态(active/inactive)',
  `sort_order` int DEFAULT 0 COMMENT '排序号',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`level_id`),
  UNIQUE KEY `uk_level_code` (`level_code`),
  KEY `idx_level_rank` (`level_rank`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工级别表(定义级别体系)';

-- =====================================================================
-- 二、新建 position 表（职位表）
--    定义具体职位，关联到 post(岗位) 和 staff_level(级别)
--    一个岗位可有多个职位（如"热菜厨师"岗位下有"初级热菜厨师"/"高级热菜厨师"职位）
-- =====================================================================

CREATE TABLE `position` (
  `position_id` int NOT NULL AUTO_INCREMENT COMMENT '职位ID',
  `store_id` bigint NOT NULL DEFAULT 1 COMMENT '门店ID(多租户隔离)',
  `post_id` int DEFAULT NULL COMMENT '关联岗位ID(→post.post_id)',
  `level_id` int DEFAULT NULL COMMENT '关联级别ID(→staff_level.level_id)',
  `position_name` varchar(50) NOT NULL COMMENT '职位名称',
  `position_code` varchar(50) DEFAULT NULL COMMENT '职位编码',
  `position_desc` varchar(200) DEFAULT NULL COMMENT '职位描述',
  `requirements` text DEFAULT NULL COMMENT '任职要求',
  `responsibilities` text DEFAULT NULL COMMENT '岗位职责',
  `base_salary` decimal(10,2) DEFAULT NULL COMMENT '基础薪资参考',
  `sort_order` int DEFAULT 0 COMMENT '排序号',
  `status` varchar(10) DEFAULT 'active' COMMENT '状态(active/inactive)',
  `created_at` timestamp DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`position_id`),
  KEY `idx_post` (`post_id`),
  KEY `idx_level` (`level_id`),
  KEY `idx_store` (`store_id`),
  CONSTRAINT `fk_position_post` FOREIGN KEY (`post_id`) REFERENCES `post` (`post_id`) ON DELETE SET NULL,
  CONSTRAINT `fk_position_level` FOREIGN KEY (`level_id`) REFERENCES `staff_level` (`level_id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='职位表(定义具体职位,关联岗位和级别)';

SET FOREIGN_KEY_CHECKS = 1;

-- =====================================================================
-- 三、灌入 staff_level 基础数据（6个级别）
-- =====================================================================

INSERT INTO staff_level (level_id, level_code, level_name, level_rank, base_salary_min, base_salary_max, description, status, sort_order) VALUES
(1, 'STAFF',      '员工级',  1, 3500.00,  6000.00, '基层员工：服务员、厨师、收银员、洗碗工等', 'active', 1),
(2, 'LEAD',       '领班级',  2, 5000.00,  8000.00, '一线领班：楼面领班、宴会领班、包厢主管等', 'active', 2),
(3, 'SUPERVISOR', '主管级',  3, 7000.00, 12000.00, '部门主管：前厅主管、后厨主管、人事主管等', 'active', 3),
(4, 'MANAGER',    '经理级',  4, 12000.00, 20000.00, '部门经理：店长、财务经理、销售总监等', 'active', 4),
(5, 'DIRECTOR',   '总监级',  5, 18000.00, 28000.00, '总监：行政总厨、运营总监等', 'active', 5),
(6, 'EXECUTIVE',  '高管级',  6, 25000.00, 60000.00, '高管：总经理、老板、董事长等', 'active', 6);

-- =====================================================================
-- 四、灌入 position 基础数据
--    按现有岗位(post)创建对应职位，关联到级别(staff_level)
--    关系映射：post.post_id → position.post_id, staff_level.level_id → position.level_id
-- =====================================================================

INSERT INTO position (position_id, store_id, post_id, level_id, position_name, position_code, position_desc, base_salary, sort_order, status) VALUES
-- === 高层管理部 (dept 1) ===
-- rino=系统管理员, 张婧=老板 → 高管级(L6)
(1,  1, NULL, 6, '总经理',       'GM',         '公司总经理，全面负责企业运营管理', 40000.00, 1, 'active'),
(2,  1, NULL, 6, '董事长',       'CHAIRMAN',   '企业创始人/董事长', 50000.00, 2, 'active'),
(3,  1, NULL, 5, '运营总监',     'OPS_DIR',    '运营管理总监', 25000.00, 3, 'active'),
(4,  1, NULL, 4, '门店总经理',   'STORE_GM',   '单门店总经理(店长)', 18000.00, 4, 'active'),

-- === 销售宴会部 (dept 2) ===
-- 宴会销售专员(post_id=1) → 员工级(L1)
(5,  1, 1, 1, '宴会销售专员',   'BQT_SALES',  '宴会订单销售与客户维护', 7000.00, 1, 'active'),
-- 宴会销售经理(post_id=2) → 经理级(L4)
(6,  1, 2, 4, '宴会销售经理',   'BQT_SALES_M','宴会销售团队管理', 14000.00, 2, 'active'),
-- 婚礼策划师(post_id=3) → 主管级(L3)
(7,  1, 3, 3, '婚礼策划师',     'WED_PLANNER', '婚礼方案策划与执行', 8000.00, 3, 'active'),
(8,  1, 4, 2, '婚礼顾问',       'WED_CONS',   '婚礼咨询与客户接待', 5500.00, 4, 'active'),
-- 宴会统筹专员(post_id=5) → 主管级(L3)
(9,  1, 5, 3, '宴会统筹主管',   'BQT_COORD',  '宴会现场统筹与执行', 11000.00, 5, 'active'),
(10, 1, 6, 4, '宴会执行经理',   'BQT_EXEC_M', '宴会执行团队管理', 14000.00, 6, 'active'),
-- 预定文员(post_id=7) → 员工级(L1)
(11, 1, 7, 1, '预定文员',       'BOOK_CLERK', '电话/网络预定接待', 5500.00, 7, 'active'),
(12, 1, 8, 1, '前台接待',       'FRONT_DESK', '前台客户接待', 5000.00, 8, 'active'),

-- === 前厅服务部 (dept 3) ===
(13, 1, 9,  4, '楼面经理',      'FLOOR_MGR',   '楼面全面管理', 12000.00, 1, 'active'),
(14, 1, 10, 2, '楼面领班',      'FLOOR_LEAD',  '楼面班次领班', 6000.00, 2, 'active'),
(15, 1, 11, 1, '包厢服务员',    'VIP_SERVER',  'VIP包厢服务', 4500.00, 3, 'active'),
(16, 1, 12, 2, '包厢主管',      'VIP_LEAD',    '包厢服务管理', 7000.00, 4, 'active'),
(17, 1, 13, 1, '宴会服务员',    'BQT_SERVER',  '宴会现场服务', 4500.00, 5, 'active'),
(18, 1, 14, 2, '宴会领班',      'BQT_LEAD_P',  '宴会服务领班', 6000.00, 6, 'active'),
(19, 1, 15, 1, '吧员',          'BARTENDER',   '吧台饮品制作', 4500.00, 7, 'active'),
(20, 1, 16, 2, '吧台主管',      'BAR_LEAD_P',  '吧台管理', 7000.00, 8, 'active'),
(21, 1, 17, 1, '迎宾',          'GREETER',     '迎宾接待', 4000.00, 9, 'active'),
(22, 1, 18, 1, '保洁员',        'CLEANER',     '清洁卫生', 3500.00, 10, 'active'),
(23, 1, 19, 2, '保洁主管',      'CLEAN_LEAD',  '保洁管理', 5000.00, 11, 'active'),
(24, 1, 20, 1, '安保员',        'SECURITY',    '安全保卫', 4000.00, 12, 'active'),
(25, 1, 21, 2, '安保主管',      'SEC_LEAD',    '安保管理', 5500.00, 13, 'active'),

-- === 后厨生产部 (dept 4) ===
(26, 1, 22, 1, '热菜厨师',      'HOT_CHEF',    '热菜烹饪制作', 9000.00, 1, 'active'),
(27, 1, 23, 3, '热菜主管',      'HOT_LEAD_P',  '热菜组管理', 12000.00, 2, 'active'),
(28, 1, 24, 1, '凉菜厨师',      'COLD_CHEF',   '凉菜制作', 8000.00, 3, 'active'),
(29, 1, 25, 2, '凉菜主管',      'COLD_LEAD_P', '凉菜组管理', 10000.00, 4, 'active'),
(30, 1, 26, 1, '面点师',        'PASTRY_CHEF', '面点制作', 7000.00, 5, 'active'),
(31, 1, 27, 2, '面点主管',      'PASTRY_LEAD', '面点组管理', 9000.00, 6, 'active'),
(32, 1, 28, 1, '库管员',        'KEEPER',      '仓库管理', 6500.00, 7, 'active'),
(33, 1, 29, 3, '库管主管',      'STORE_LEAD_P','库房管理', 10000.00, 8, 'active'),
(34, 1, 30, 1, '洗碗工',        'DISHWASHER',  '餐具清洗', 3500.00, 9, 'active'),
(35, 1, 31, 2, '洗碗主管',      'DISH_LEAD_P', '洗碗组管理', 4500.00, 10, 'active'),
(36, 1, 32, 1, '粗加工厨师',    'PREP_CHEF',   '食材粗加工', 4000.00, 11, 'active'),
(37, 1, 33, 2, '粗加工主管',    'PREP_LEAD_P', '粗加工管理', 5000.00, 12, 'active'),
(38, 1, 34, 1, '甜品师',        'DESSERT_CHEF','甜品制作', 6000.00, 13, 'active'),
(39, 1, 35, 2, '甜品主管',      'DESSERT_LEAD','甜品组管理', 7500.00, 14, 'active'),
-- 行政总厨/副厨 → 总监级/主管级
(40, 1, NULL, 5, '行政总厨',    'EXEC_CHEF',   '后厨全面技术管理', 22000.00, 15, 'active'),
(41, 1, NULL, 3, '后厨主管',    'BOH_SUPER',   '后厨日常管理', 12000.00, 16, 'active'),

-- === 财务采购人事部 (dept 5) ===
(42, 1, 36, 1, '财务专员',      'FIN_CLERK_P', '日常财务处理', 6000.00, 1, 'active'),
(43, 1, 37, 3, '财务经理',      'FIN_MGR',     '财务管理', 18000.00, 2, 'active'),
(44, 1, 38, 1, '收银专员',      'CASHIER_P',   '收银操作', 5500.00, 3, 'active'),
(45, 1, 39, 2, '收银主管',      'CASH_LEAD_P', '收银管理', 7000.00, 4, 'active'),
(46, 1, 40, 1, '采购专员',      'BUYER_P',     '采购执行', 6500.00, 5, 'active'),
(47, 1, 41, 3, '采购主管',      'BUY_LEAD_P',  '采购管理', 10000.00, 6, 'active'),
(48, 1, 42, 1, '人事专员',      'HR_CLERK_P',  '人事日常处理', 6000.00, 7, 'active'),
(49, 1, 43, 3, '人事主管',      'HR_LEAD_P',   '人事管理', 10000.00, 8, 'active'),
(50, 1, 44, 1, '出纳',          'CASH_OUT_P',  '现金出纳', 5500.00, 9, 'active'),
(51, 1, 45, 2, '出纳主管',      'PAY_LEAD_P',  '出纳管理', 7000.00, 10, 'active'),
(52, 1, 46, 1, '供应商管理员',  'SUP_ADM_P',   '供应商日常管理', 6000.00, 11, 'active'),
(53, 1, 47, 3, '供应商主管',    'SUP_LEAD_P',  '供应商管理', 10000.00, 12, 'active'),

-- === 宣城店专属职位 (store_id=2) ===
(54, 2, NULL, 4, '宣城店长',    'XC_STORE_GM', '宣城门店总经理', 18000.00, 1, 'active');

-- =====================================================================
-- 五、UPDATE staff_master.staff_rank 关联到级别名称
--    按员工当前职位匹配级别
-- =====================================================================

-- 高管级(L6)
UPDATE staff_master SET staff_rank='高管级' WHERE staff_id IN (1, 100);
-- 经理级(L4) — 店长、销售总监
UPDATE staff_master SET staff_rank='经理级' WHERE staff_id IN (101, 102, 103);
-- 总监级(L5) — 行政总厨
UPDATE staff_master SET staff_rank='总监级' WHERE staff_id = 104;
-- 经理级(L4) — 财务经理
UPDATE staff_master SET staff_rank='经理级' WHERE staff_id = 105;
-- 主管级(L3)
UPDATE staff_master SET staff_rank='主管级' WHERE staff_id IN (106, 107, 108, 109);
-- 员工级(L1)
UPDATE staff_master SET staff_rank='员工级' WHERE staff_id IN (110, 111, 112, 113, 114, 115, 116, 117, 118, 119);

-- =====================================================================
-- 灌入完成统计：
-- 1. staff_level: 6 条（员工级/领班级/主管级/经理级/总监级/高管级）
-- 2. position: 54 条（覆盖5大部门+1条宣城店专属）
-- 3. staff_master.staff_rank: 21 条 UPDATE（关联级别名称）
--
-- 表关系总览：
--   department(部门,30条) → 1:N → post(岗位,47条) → 1:N → position(职位,54条)
--                                                                ↓ N:1
--                                                          staff_level(级别,6条)
--                                                                ↓ 1:N (通过 staff_rank 文本关联)
--                                                          staff_master(员工,31条)
-- =====================================================================

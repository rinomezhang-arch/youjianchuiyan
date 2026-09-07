-- TR-OPS-COST-UI-01 E2E 种子数据（Trae, 2026-09-07）
-- 仅限本人隔离库 trae_cost_e2e_20260907（127.0.0.1:13317），全部为 TR验收_ 合成数据。
-- 禁止在生产或任何其他库执行；验收后随整个测试库一并废弃。
-- 可重复执行：主键冲突走 UPDATE，配方行先清后插。

USE trae_cost_e2e_20260907;

-- store_info.manager_id 外键引用 staff_master，一次性测试库内临时关闭以便重建空桩
SET FOREIGN_KEY_CHECKS = 0;

-- 1) staff_master：生产结构夹具此处是空桩（仅 staff_id 列），扩为可登录结构以走真实登录链路
DROP TABLE IF EXISTS staff_master;
CREATE TABLE staff_master (
  staff_id INT PRIMARY KEY,
  store_id INT NOT NULL DEFAULT 1,
  staff_name VARCHAR(50) NOT NULL,
  staff_account VARCHAR(50) NOT NULL,
  staff_phone VARCHAR(20) NULL,
  staff_password VARCHAR(100) NOT NULL,
  role VARCHAR(20) NOT NULL,
  department VARCHAR(50) NULL,
  staff_position VARCHAR(50) NULL,
  permission_level INT DEFAULT 1,
  employment_status VARCHAR(20) DEFAULT 'active'
);
INSERT INTO staff_master
  (staff_id, store_id, staff_name, staff_account, staff_phone, staff_password, role, department, staff_position, permission_level, employment_status)
VALUES
  (90001, 1, 'TR验收_测试员', 'trae_test', '13800000000', '002323', 'gm', '财务部', '测试岗', 10, 'active');

-- 2) 门店（/stores 与登录门店名使用）
INSERT INTO store_info (store_id, store_code, store_name, status, sort_order)
VALUES (1, 'TR-STORE-01', 'TR验收_测试门店', 'open', 1)
ON DUPLICATE KEY UPDATE store_name = VALUES(store_name), status = 'open';

-- 3) 原料（CostRecipe 原料下拉数据源 /api/ingredients）
-- unit_price 语义=采购单位价（如36元/千克）；净价=unit_price/(conversion_rate×yield_rate%)
INSERT INTO ingredient_master
  (ingredient_id, store_id, ingredient_name, ingredient_category, purchase_unit, usage_unit, conversion_rate, avg_price, unit_price, is_active, yield_rate)
VALUES
  ('TR-ING-001', 1, 'TR验收_五花肉', '肉类', '千克', '克', 1000.000, 36.0000, 36.00000000, 1, 90.00),
  ('TR-ING-002', 1, 'TR验收_土豆',   '蔬菜', '千克', '克', 1000.000, 3.0000,  3.00000000,  1, 85.00),
  ('TR-ING-003', 1, 'TR验收_生抽',   '调味品', '瓶', '毫升', 500.000, 8.0000,  8.00000000,  1, NULL)
ON DUPLICATE KEY UPDATE ingredient_name = VALUES(ingredient_name), unit_price = VALUES(unit_price), avg_price = VALUES(avg_price);

-- 4) 菜品（/api/cost/ranking 聚合数据源 dish_master）
INSERT INTO dish_master (dish_id, store_id, dish_name, category, sale_price, cost_price, unit, is_active)
VALUES
  ('TR-DISH-001', 1, 'TR验收_红烧肉',     '热菜', 58.00, 0.00, '份', 1),
  ('TR-DISH-002', 1, 'TR验收_酸辣土豆丝', '热菜', 18.00, 0.00, '份', 1),
  ('TR-DISH-003', 1, 'TR验收_清蒸鲈鱼',   '热菜', 88.00, 0.00, '份', 1)
ON DUPLICATE KEY UPDATE dish_name = VALUES(dish_name);

-- 5) 配方：DISH-001 两种原料、DISH-002 一种；DISH-003 故意无配方（验证 hasRecipe=false 展示）
DELETE FROM dish_recipe WHERE dish_id LIKE 'TR-%';
-- unit_price 预置为按净价公式算好的每使用单位价（recalc-all 会重写校准）：
-- 五花肉 36/(1000×0.90)=0.04/克；生抽 8/(500×1)=0.016/毫升；土豆 3/(1000×0.85)=0.00352941/克
INSERT INTO dish_recipe
  (store_id, dish_id, ingredient_id, ingredient_name, unit, unit_price, quantity, total_cost)
VALUES
  (1, 'TR-DISH-001', 'TR-ING-001', 'TR验收_五花肉', '克',   0.04000000, 500.000, 20.0000),
  (1, 'TR-DISH-001', 'TR-ING-003', 'TR验收_生抽',   '毫升', 0.01600000, 20.000, 0.3200),
  (1, 'TR-DISH-002', 'TR-ING-002', 'TR验收_土豆',   '克',   0.00352941, 300.000, 1.0588);

SET FOREIGN_KEY_CHECKS = 1;

-- 6) 回读断言
SELECT 'staff' AS what, COUNT(*) AS n FROM staff_master WHERE staff_account='trae_test'
UNION ALL SELECT 'stores', COUNT(*) FROM store_info WHERE store_code='TR-STORE-01'
UNION ALL SELECT 'ingredients', COUNT(*) FROM ingredient_master WHERE ingredient_id LIKE 'TR-%'
UNION ALL SELECT 'dishes', COUNT(*) FROM dish_master WHERE dish_id LIKE 'TR-%'
UNION ALL SELECT 'recipes', COUNT(*) FROM dish_recipe WHERE dish_id LIKE 'TR-%';

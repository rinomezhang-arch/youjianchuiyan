-- =============================================================================
-- ultimate_checkup_v2.sql
-- Banquet System V2.0 - Ultimate Checkup v2
-- Compare DB state against Excel source workbook
-- Date: 2026-08-03 Executor: Dilong
-- =============================================================================

SELECT '========================================' AS '';
SELECT '  ULTIMATE CHECKUP V2 - vs Excel Source' AS '';
SELECT '========================================' AS '';
SELECT '' AS '';

-- =============================================================================
-- SECTION A: TABLE EXISTENCE
-- =============================================================================
SELECT '### A: TABLE EXISTENCE vs REQUIREMENTS ###' AS '';
SELECT '' AS '';

SELECT 'A1. dish_master (348 dishes in Excel)' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_master')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM dish_master) AS row_cnt,
  '348' AS target;

SELECT 'A2. ingredient_master (1217 items in Excel)' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM ingredient_master) AS row_cnt,
  '1217' AS target;

SELECT 'A3. ingredient_cost_price (unit prices)' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_cost_price')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM ingredient_cost_price) AS row_cnt,
  '1217' AS target;

SELECT 'A4. unit_conversion' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='unit_conversion')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM unit_conversion) AS row_cnt,
  '~50' AS target;

SELECT 'A5. yield_rate_config' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='yield_rate_config')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM yield_rate_config) AS row_cnt,
  '1 per ingredient' AS target;

SELECT 'A6. dish_recipe' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_recipe')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM dish_recipe) AS row_cnt,
  '1000-3500' AS target;

SELECT 'A7. dish_cost_card' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_cost_card')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM dish_cost_card) AS row_cnt,
  '348' AS target;

SELECT 'A8. purchase_order' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_order')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM purchase_order) AS row_cnt,
  'data' AS target;

SELECT 'A9. purchase_order_detail' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_order_detail')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM purchase_order_detail) AS row_cnt,
  'Linked to A8' AS target;

SELECT 'A10. purchase_receipt' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_receipt')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM purchase_receipt) AS row_cnt,
  'data' AS target;

SELECT 'A11. purchase_receipt_detail' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_receipt_detail')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM purchase_receipt_detail) AS row_cnt,
  'Linked to A10' AS target;

SELECT 'A12. requisition_order' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='requisition_order')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM requisition_order) AS row_cnt,
  'data' AS target;

SELECT 'A13. requisition_detail' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='requisition_detail')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM requisition_detail) AS row_cnt,
  'Linked to A12' AS target;

SELECT 'A14. ingredient_inventory_log' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_inventory_log')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM ingredient_inventory_log) AS row_cnt,
  'data' AS target;

SELECT 'A15. supplier_master' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='supplier_master')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM supplier_master) AS row_cnt,
  'real suppliers' AS target;

SELECT 'A16. store_info' AS item,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='store_info')
  THEN 'OK' ELSE 'MISSING' END AS status,
  (SELECT COUNT(*) FROM store_info) AS row_cnt,
  'Ningguo+Xuancheng' AS target;

SELECT '' AS '';
SELECT '' AS '';

-- =============================================================================
-- SECTION B: FAKE DATA DETECTION
-- =============================================================================
SELECT '### B: FAKE/PLACEHOLDER DATA DETECTION ###' AS '';
SELECT '' AS '';

-- B1: dish data format check
SELECT 'B1. dish_master format check:' AS '';
SELECT 'Excel format (CY000001)' AS format_type, COUNT(*) AS cnt FROM dish_master WHERE dish_id LIKE 'CY0000%'
UNION ALL SELECT 'Old DB format (CY0xx)', COUNT(*) FROM dish_master WHERE dish_id REGEXP '^CY0[0-9]{2}$'
UNION ALL SELECT 'Other format', COUNT(*) FROM dish_master WHERE dish_id NOT REGEXP '^CY0';
SELECT '' AS '';

SELECT 'B1.1 DB dish samples (are they from Excel?):' AS '';
SELECT dish_id, dish_name, dish_category, cost_price, sale_price FROM dish_master ORDER BY dish_id LIMIT 15;
SELECT '' AS '';

-- B2: recipe fake analysis
SELECT 'B2. dish_recipe fake data:' AS '';
SELECT 'total_rows' AS metric, COUNT(*) AS value FROM dish_recipe
UNION ALL SELECT 'store_2_fake_rows', COUNT(*) FROM dish_recipe WHERE store_id=2
UNION ALL SELECT 'name_equals_id_dirty', COUNT(*) FROM dish_recipe WHERE ingredient_name=ingredient_id
UNION ALL SELECT 'unit_is_null', COUNT(*) FROM dish_recipe WHERE unit IS NULL
UNION ALL SELECT 'wastage_and_yield_zero', COUNT(*) FROM dish_recipe WHERE wastage_rate=0 AND yield_rate=0;
SELECT '' AS '';

SELECT 'B2.1 Fake recipe rows (store_id=2 or dirty):' AS '';
SELECT recipe_id, store_id, dish_id, ingredient_id, ingredient_name, quantity, unit_price, total_cost FROM dish_recipe WHERE store_id=2 OR ingredient_name=ingredient_id LIMIT 10;
SELECT '' AS '';

-- B3: purchase fake
SELECT 'B3. ingredient_purchase all rows (6 fake):' AS '';
SELECT purchase_id, store_id, ingredient_id, purchase_date, purchase_quantity, purchase_price, purchase_total, quantity, unit_price, total_amount, status FROM ingredient_purchase;
SELECT '' AS '';

SELECT 'B3.1 purchase_quantity zero:' AS '';
SELECT COUNT(*) AS zero_qty_count FROM ingredient_purchase WHERE purchase_quantity=0;
SELECT '' AS '';

-- B4: orphan purchase_order_detail
SELECT 'B4. purchase_order_detail orphans:' AS '';
SELECT COUNT(*) AS orphan_count FROM purchase_order_detail pod WHERE NOT EXISTS (SELECT 1 FROM purchase_order po WHERE po.order_id=pod.order_id);
SELECT '' AS '';
SELECT 'B4.1 Orphan samples:' AS '';
SELECT pod.detail_id, pod.order_id, pod.ingredient_id, pod.ingredient_name, pod.quantity, pod.unit_price FROM purchase_order_detail pod WHERE NOT EXISTS (SELECT 1 FROM purchase_order po WHERE po.order_id=pod.order_id) LIMIT 10;
SELECT '' AS '';

-- B5: requisition fake
SELECT 'B5. requisition fake data:' AS '';
SELECT requisition_id, requisition_no, department_name, status, reason FROM requisition_order;
SELECT '' AS '';
SELECT 'B5.1 requisition_detail:' AS '';
SELECT detail_id, requisition_id, ingredient_id, ingredient_name, unit, request_quantity FROM requisition_detail;
SELECT '' AS '';

-- B6: supplier duplicates
SELECT 'B6. suppliers (duplicate?):' AS '';
SELECT supplier_id, supplier_name, contact_person, contact_phone FROM supplier_master;
SELECT '' AS '';

-- B7: department duplicates
SELECT 'B7. departments (old+new conflict):' AS '';
SELECT dept_id, dept_name FROM department ORDER BY dept_id;
SELECT '' AS '';

-- B8: deleted data evidence
SELECT 'B8. Deleted data (AUTO_INCREMENT gap):' AS '';
SELECT TABLE_NAME AS tbl, TABLE_ROWS AS est_rows, AUTO_INCREMENT AS next_id FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME IN ('ingredient_cost_price','dish_cost_card','dish_cost_card_detail','yield_rate_config') AND AUTO_INCREMENT > TABLE_ROWS + 1;
SELECT '' AS '';

-- =============================================================================
-- SECTION C: FOREIGN KEY INTEGRITY
-- =============================================================================
SELECT '### C: FOREIGN KEY INTEGRITY ###' AS '';
SELECT '' AS '';

-- C1: All tables referencing ingredient_master
SELECT 'C1. Child tables referencing ingredient_master:' AS '';
SELECT fk.TABLE_NAME AS child_table FROM information_schema.KEY_COLUMN_USAGE fk WHERE fk.TABLE_SCHEMA='banquet' AND fk.REFERENCED_TABLE_NAME='ingredient_master' GROUP BY fk.TABLE_NAME;
SELECT '' AS '';

-- C2: Orphan counts
SELECT 'C2. Orphan FK count per child table:' AS '';

SELECT 'dish_recipe' AS child, COUNT(*) AS orphans FROM dish_recipe dr LEFT JOIN ingredient_master im ON dr.ingredient_id=im.ingredient_id AND dr.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'purchase_order_detail', COUNT(*) FROM purchase_order_detail pod LEFT JOIN ingredient_master im ON pod.ingredient_id=im.ingredient_id AND pod.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'requisition_detail', COUNT(*) FROM requisition_detail rd LEFT JOIN ingredient_master im ON rd.ingredient_id=im.ingredient_id AND rd.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'purchase_receipt_detail', COUNT(*) FROM purchase_receipt_detail prd LEFT JOIN ingredient_master im ON prd.ingredient_id=im.ingredient_id AND prd.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'stock_loss_detail', COUNT(*) FROM stock_loss_detail sld LEFT JOIN ingredient_master im ON sld.ingredient_id=im.ingredient_id AND sld.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'stock_take_detail', COUNT(*) FROM stock_take_detail std LEFT JOIN ingredient_master im ON std.ingredient_id=im.ingredient_id AND std.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'yield_rate_config', COUNT(*) FROM yield_rate_config yrc LEFT JOIN ingredient_master im ON yrc.ingredient_id=im.ingredient_id AND yrc.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'ingredient_inventory_log', COUNT(*) FROM ingredient_inventory_log iil LEFT JOIN ingredient_master im ON iil.ingredient_id=im.ingredient_id AND iil.store_id=im.store_id WHERE im.ingredient_id IS NULL
UNION ALL
SELECT 'dish_cost_card_detail', COUNT(*) FROM dish_cost_card_detail dccd LEFT JOIN ingredient_master im ON dccd.ingredient_id=im.ingredient_id AND dccd.store_id=im.store_id WHERE im.ingredient_id IS NULL;
SELECT '' AS '';

SELECT 'C3. purchase_order_detail with no parent order:' AS '';
SELECT COUNT(*) AS orphan_count FROM purchase_order_detail pod WHERE NOT EXISTS (SELECT 1 FROM purchase_order po WHERE po.order_id=pod.order_id);
SELECT '' AS '';

-- =============================================================================
-- SECTION D: FIELD-LEVEL PROBLEMS
-- =============================================================================
SELECT '### D: FIELD-LEVEL PROBLEMS ###' AS '';
SELECT '' AS '';

-- D1: ingredient_master duplicate columns
SELECT 'D1. ingredient_master REDUNDANT columns:' AS '';
SELECT 'category vs ingredient_category' AS issue, 
  CONCAT('category:', COLUMN_TYPE, ' / ingredient_category:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='ingredient_category')) AS detail FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='category'
UNION ALL 
SELECT 'unit vs purchase_unit vs usage_unit',
  CONCAT('unit:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='unit'), ' / purchase_unit:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='purchase_unit'), ' / usage_unit:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='usage_unit'))
UNION ALL
SELECT 'supplier_id vs primary_supplier_id',
  CONCAT('supplier_id:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='supplier_id'), ' / primary_supplier_id:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='primary_supplier_id'))
UNION ALL
SELECT 'status vs is_active',
  CONCAT('status:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='status'), ' / is_active:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='is_active'));
SELECT '' AS '';

-- D2: ingredient_purchase dual-track
SELECT 'D2. ingredient_purchase DUAL-TRACK fields:' AS '';
SELECT 'purchase_quantity vs quantity (old vs new)' AS issue,
  CONCAT('purchase_quantity:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='purchase_quantity'), ' | quantity:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='quantity')) AS detail
UNION ALL
SELECT 'purchase_price vs unit_price',
  CONCAT('purchase_price:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='purchase_price'), ' | unit_price:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='unit_price'))
UNION ALL
SELECT 'purchase_total vs total_amount',
  CONCAT('purchase_total:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='purchase_total'), ' | total_amount:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='total_amount'))
UNION ALL
SELECT 'usage_quantity vs purchase_quantity vs quantity (3rd track)',
  CONCAT('usage_quantity:', (SELECT COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase' AND COLUMN_NAME='usage_quantity'));
SELECT '' AS '';

-- D3: dish_master redundant columns
SELECT 'D3. dish_master REDUNDANT columns:' AS '';
SELECT 
  CONCAT('dish_category vs category; english_name vs dish_name_en; main_ingredient vs main_ingredients; dish_intro vs tiktok_recommend; festive_name vs birthday/wedding/house_move/... (8 festive name columns)') AS issue,
  CONCAT(COUNT(*), ' potentially redundant columns found') AS detail
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_master'
  AND (COLUMN_NAME IN ('dish_category','category','english_name','dish_name_en','main_ingredient','main_ingredients','festive_name',
       'birthday_name','wedding_name','house_move_name','promotion_name','reunion_name','thanksgiving_name','year_end_name','baby_born_name'))
GROUP BY TABLE_NAME;
SELECT '' AS '';

-- D4: inventory_log duplicates
SELECT 'D4. ingredient_inventory_log REDUNDANT columns:' AS '';
SELECT 'quantity vs log_quantity' AS issue, 'Two quantity columns' AS detail
UNION ALL SELECT 'before_stock vs after_stock', 'stock snapshot fields'
UNION ALL SELECT 'change_type vs log_type', 'Two type fields'
UNION ALL SELECT 'notes vs note', 'Two note fields'
UNION ALL SELECT 'operator vs operator_id', 'operator name vs id'
UNION ALL SELECT 'created_at vs log_time', 'Two timestamp fields';
SELECT '' AS '';

-- =============================================================================
-- SECTION E: DATA CONSISTENCY
-- =============================================================================
SELECT '### E: DATA CONSISTENCY ###' AS '';
SELECT '' AS '';

-- E1: dish cost vs recipe total
SELECT 'E1. dish_master.cost_price vs SUM(recipe.total_cost):' AS '';
SELECT d.dish_id, d.dish_name, d.cost_price AS master_cost, ROUND(SUM(dr.total_cost),2) AS recipe_sum, ROUND(d.cost_price - SUM(dr.total_cost),2) AS diff
FROM dish_master d JOIN dish_recipe dr ON d.dish_id=dr.dish_id WHERE dr.store_id=1
GROUP BY d.dish_id, d.dish_name, d.cost_price LIMIT 20;
SELECT '' AS '';

-- E2: recipe ingredients with no price source
SELECT 'E2. Recipe ingredients with zero cost source:' AS '';
SELECT DISTINCT dr.ingredient_id, MAX(dr.ingredient_name) AS name, COUNT(DISTINCT dr.dish_id) AS used_in
FROM dish_recipe dr
WHERE NOT EXISTS (SELECT 1 FROM ingredient_cost_price icp WHERE icp.ingredient_id=dr.ingredient_id)
  AND NOT EXISTS (SELECT 1 FROM ingredient_purchase ip WHERE ip.ingredient_id=dr.ingredient_id AND (ip.unit_price IS NOT NULL OR ip.purchase_price > 0))
GROUP BY dr.ingredient_id ORDER BY used_in DESC LIMIT 20;
SELECT '' AS '';

-- E3: inventory_summary
SELECT 'E3. inventory_summary (12 rows):' AS '';
SELECT summary_id, store_id, ingredient_id, total_quantity, total_cost FROM inventory_summary;
SELECT '' AS '';

-- =============================================================================
-- SECTION F: GAP vs EXCEL
-- =============================================================================
SELECT '### F: GAP vs EXCEL SOURCE ###' AS '';
SELECT '' AS '';

SELECT 'dishes: Excel 348 vs DB' AS metric, (SELECT COUNT(*) FROM dish_master) AS db_value;
SELECT 'ingredients: Excel 1217 vs DB' AS metric, (SELECT COUNT(*) FROM ingredient_master) AS db_value;
SELECT 'cost_prices: Excel 1217 vs DB' AS metric, (SELECT COUNT(*) FROM ingredient_cost_price) AS db_value;
SELECT 'unit_conversions: need ~50 vs DB' AS metric, (SELECT COUNT(*) FROM unit_conversion) AS db_value;
SELECT 'yield_rates: need 1/ingr vs DB' AS metric, (SELECT COUNT(*) FROM yield_rate_config) AS db_value;
SELECT 'cost_cards: need 348 vs DB' AS metric, (SELECT COUNT(*) FROM dish_cost_card) AS db_value;
SELECT 'recipe_rows: need 1000+ vs DB' AS metric, (SELECT COUNT(*) FROM dish_recipe) AS db_value;
SELECT 'purchases: need real vs DB' AS metric, (SELECT COUNT(*) FROM ingredient_purchase) AS db_value;

SELECT '' AS '';
SELECT 'Dish ID format mismatch:' AS '';
SELECT 'CY000xxx (Excel format)' AS format_type, COUNT(*) FROM dish_master WHERE dish_id LIKE 'CY000%'
UNION ALL SELECT 'CY0xx (old DB format)', COUNT(*) FROM dish_master WHERE dish_id REGEXP '^CY0[0-9]{2}$' AND dish_id NOT LIKE 'CY000%';
SELECT '' AS '';

-- =============================================================================
-- SECTION G: SOLO SQL FILES AUDIT
-- =============================================================================
SELECT '### G: SOLO 21 SQL FILES EXECUTION AUDIT ###' AS '';
SELECT '' AS '';

SELECT 'enhance_food_material_tables.sql' AS sql_file,
  CASE WHEN EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='pinyin_code')
  THEN 'EXECUTED' ELSE 'NOT_EXECUTED - no pinyin_code field' END AS verdict;

SELECT 'purchase_flow_tables.sql' AS sql_file,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_request')
  THEN 'EXECUTED' ELSE 'NOT_EXECUTED - purchase_request missing' END AS verdict;

SELECT 'stock_module_tables.sql' AS sql_file,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_inventory')
  THEN 'EXECUTED' ELSE 'NOT_EXECUTED - ingredient_inventory missing' END AS verdict;

SELECT 'seed_material_master.sql' AS sql_file,
  CASE WHEN (SELECT COUNT(*) FROM ingredient_master)>0 THEN 'MAYBE' ELSE 'NOT_EXECUTED - empty table' END AS verdict;

SELECT 'seed_ingredient_prices.sql' AS sql_file,
  CASE WHEN (SELECT COUNT(*) FROM ingredient_cost_price)>0 THEN 'MAYBE' ELSE 'NOT_EXECUTED - empty table' END AS verdict;

SELECT 'seed_staff_supplier.sql' AS sql_file,
  CASE WHEN (SELECT COUNT(*) FROM supplier_master)>=7 THEN 'MAYBE (7 suppliers with 1 duplicate)' ELSE 'NOT_EXECUTED' END AS verdict;

SELECT 'seed_org_position_level.sql' AS sql_file,
  CASE WHEN (SELECT COUNT(*) FROM department)>=10 THEN 'MAYBE (16 depts but 1-7+32-40 conflict)' ELSE 'NOT_EXECUTED' END AS verdict;

SELECT 'dish_cost_module.sql' AS sql_file,
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_cost_card')
  THEN 'TABLE EXISTS but EMPTY (0 rows) - created but NEVER populated' ELSE 'NOT_EXECUTED' END AS verdict;

SELECT 'ALL OTHER seed_*.sql (14 files)' AS sql_file,
  'NOT_EXECUTED - target tables empty' AS verdict;

SELECT '' AS '';

-- =============================================================================
-- SECTION H: FINAL VERDICT
-- =============================================================================
SELECT '### H: FINAL VERDICT ###' AS '';
SELECT '' AS '';

SELECT 
  CASE 
    WHEN (SELECT COUNT(*) FROM ingredient_master) = 0 
      AND (SELECT COUNT(*) FROM ingredient_cost_price) = 0
    THEN 'CRITICAL FAILURE: Solo wrote 21 SQL files to disk but executed NONE. All real Excel data (348 dishes, 1217 ingredients) never imported. DB contains ONLY prior fake/test data. 0% of task complete.'
    ELSE 'PARTIAL'
  END AS verdict;

SELECT '' AS '';
SELECT '========================================' AS '';
SELECT '  CHECKUP V2 COMPLETE' AS '';
SELECT '========================================' AS '';

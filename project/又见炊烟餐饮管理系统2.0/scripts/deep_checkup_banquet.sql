-- =============================================================================
-- deep_checkup_banquet.sql
-- Banquet Management System V2.0 - Full Chain Health Check
-- Date: 2026-08-03
-- Executor: Dilong
-- =============================================================================

SELECT '========================================================================' AS '';
SELECT CONCAT('Checkup Time: ', DATE_FORMAT(NOW(), '%Y-%m-%d %H:%i:%s')) AS '';
SELECT '' AS '';

-- =============================================================================
-- PART 1: Table Inventory - existence, columns, rows, comments
-- =============================================================================
SELECT '### PART 1: TABLE INVENTORY ###' AS '';
SELECT '' AS '';

SELECT 
  TABLE_NAME AS table_name,
  TABLE_COMMENT AS table_comment,
  (SELECT COUNT(*) FROM information_schema.COLUMNS c WHERE c.TABLE_SCHEMA='banquet' AND c.TABLE_NAME=t.TABLE_NAME) AS col_count,
  TABLE_ROWS AS est_rows
FROM information_schema.TABLES t
WHERE TABLE_SCHEMA='banquet' 
  AND TABLE_NAME IN (
    'dish_master','dish_category','dish_tag','dish_tag_relation','dish_usage','dish_usage_relation',
    'dish_recipe','dish_cost_card','dish_cost_card_detail',
    'ingredient_master','ingredient_purchase','ingredient_cost_price','yield_rate_config','unit_conversion',
    'purchase_order','purchase_order_detail','purchase_receipt','purchase_receipt_detail','purchase_return','purchase_return_detail',
    'requisition_order','requisition_detail',
    'ingredient_inventory_log','inventory_summary',
    'stock_loss','stock_loss_detail','stock_take','stock_take_detail','stock_transfer','stock_transfer_detail',
    'supplier_master','store_info','department','staff_master'
  )
ORDER BY TABLE_NAME;

SELECT '' AS '';

-- =============================================================================
-- PART 2: Row Count
-- =============================================================================
SELECT '### PART 2: ACTUAL ROW COUNTS ###' AS '';
SELECT '' AS '';

SELECT 'dish_master' AS tbl, COUNT(*) AS cnt FROM dish_master
UNION ALL SELECT 'dish_category', COUNT(*) FROM dish_category
UNION ALL SELECT 'dish_tag', COUNT(*) FROM dish_tag
UNION ALL SELECT 'dish_tag_relation', COUNT(*) FROM dish_tag_relation
UNION ALL SELECT 'dish_recipe', COUNT(*) FROM dish_recipe
UNION ALL SELECT 'dish_cost_card', COUNT(*) FROM dish_cost_card
UNION ALL SELECT 'dish_cost_card_detail', COUNT(*) FROM dish_cost_card_detail
UNION ALL SELECT 'ingredient_master', COUNT(*) FROM ingredient_master
UNION ALL SELECT 'ingredient_purchase', COUNT(*) FROM ingredient_purchase
UNION ALL SELECT 'ingredient_cost_price', COUNT(*) FROM ingredient_cost_price
UNION ALL SELECT 'yield_rate_config', COUNT(*) FROM yield_rate_config
UNION ALL SELECT 'unit_conversion', COUNT(*) FROM unit_conversion
UNION ALL SELECT 'purchase_order', COUNT(*) FROM purchase_order
UNION ALL SELECT 'purchase_order_detail', COUNT(*) FROM purchase_order_detail
UNION ALL SELECT 'purchase_receipt', COUNT(*) FROM purchase_receipt
UNION ALL SELECT 'purchase_receipt_detail', COUNT(*) FROM purchase_receipt_detail
UNION ALL SELECT 'purchase_return', COUNT(*) FROM purchase_return
UNION ALL SELECT 'purchase_return_detail', COUNT(*) FROM purchase_return_detail
UNION ALL SELECT 'requisition_order', COUNT(*) FROM requisition_order
UNION ALL SELECT 'requisition_detail', COUNT(*) FROM requisition_detail
UNION ALL SELECT 'ingredient_inventory_log', COUNT(*) FROM ingredient_inventory_log
UNION ALL SELECT 'inventory_summary', COUNT(*) FROM inventory_summary
UNION ALL SELECT 'stock_loss', COUNT(*) FROM stock_loss
UNION ALL SELECT 'stock_loss_detail', COUNT(*) FROM stock_loss_detail
UNION ALL SELECT 'stock_take', COUNT(*) FROM stock_take
UNION ALL SELECT 'stock_take_detail', COUNT(*) FROM stock_take_detail
UNION ALL SELECT 'stock_transfer', COUNT(*) FROM stock_transfer
UNION ALL SELECT 'supplier_master', COUNT(*) FROM supplier_master
UNION ALL SELECT 'store_info', COUNT(*) FROM store_info
UNION ALL SELECT 'department', COUNT(*) FROM department
UNION ALL SELECT 'staff_master', COUNT(*) FROM staff_master;

SELECT '' AS '';

-- =============================================================================
-- PART 3: Foreign Key Map
-- =============================================================================
SELECT '### PART 3: FOREIGN KEY RELATIONSHIPS ###' AS '';
SELECT '' AS '';

SELECT 
  fk.TABLE_NAME AS child_table,
  fk.COLUMN_NAME AS child_col,
  fk.REFERENCED_TABLE_NAME AS parent_table,
  fk.REFERENCED_COLUMN_NAME AS parent_col,
  fk.CONSTRAINT_NAME AS constraint_name
FROM information_schema.KEY_COLUMN_USAGE fk
WHERE fk.TABLE_SCHEMA='banquet' 
  AND fk.REFERENCED_TABLE_SCHEMA='banquet'
  AND fk.REFERENCED_TABLE_NAME IS NOT NULL
  AND fk.TABLE_NAME IN (
    'dish_master','dish_recipe','dish_cost_card','dish_cost_card_detail',
    'ingredient_master','ingredient_purchase','ingredient_cost_price','yield_rate_config',
    'purchase_order','purchase_order_detail','purchase_receipt','purchase_receipt_detail','purchase_return','purchase_return_detail',
    'requisition_order','requisition_detail',
    'ingredient_inventory_log','inventory_summary',
    'stock_loss','stock_loss_detail','stock_take','stock_take_detail','stock_transfer'
  )
ORDER BY fk.TABLE_NAME;

SELECT '' AS '';

-- =============================================================================
-- PART 4: End-to-end data flow breakage check
-- =============================================================================
SELECT '### PART 4: DATA FLOW BREAKAGE CHECK ###' AS '';
SELECT '' AS '';

-- 4.1 ingredient_master data
SELECT '4.1 ingredient_master row count:', COUNT(*) AS val FROM ingredient_master;
SELECT '4.1 dish_recipe unique ingredient_ids:', COUNT(DISTINCT ingredient_id) AS val FROM dish_recipe;
SELECT '4.1 ingredient_ids in recipe but NOT in master:', COUNT(DISTINCT dr.ingredient_id) AS missing
FROM dish_recipe dr LEFT JOIN ingredient_master im ON dr.ingredient_id=im.ingredient_id AND dr.store_id=im.store_id 
WHERE im.ingredient_id IS NULL;
SELECT '' AS '';

-- 4.2 Recipe ingredients vs master - full list
SELECT '4.2 Recipe ingredients vs master archive - per ingredient:' AS '';
SELECT 
  dr.ingredient_id AS recipe_ing,
  MAX(dr.ingredient_name) AS ing_name,
  COUNT(DISTINCT dr.dish_id) AS in_dishes,
  CASE WHEN im.ingredient_id IS NOT NULL THEN 'OK' ELSE 'MISSING' END AS archive_status
FROM dish_recipe dr
LEFT JOIN ingredient_master im ON dr.ingredient_id=im.ingredient_id AND dr.store_id=im.store_id
GROUP BY dr.ingredient_id, im.ingredient_id
ORDER BY im.ingredient_id IS NULL DESC, dr.ingredient_id;
SELECT '' AS '';

-- 4.3 Dish -> Recipe -> Cost Card chain
SELECT '4.3 Dish->Recipe->CostCard chain:' AS '';
SELECT 'dishes_with_recipes' AS check_item, COUNT(DISTINCT dish_id) AS val FROM dish_recipe
UNION ALL SELECT 'dishes_with_cost_cards', COUNT(DISTINCT dish_id) FROM dish_cost_card;
SELECT '' AS '';

-- 4.4 Purchase chain
SELECT '4.4 Purchase chain stats:' AS '';
SELECT 'purchase_orders' AS check_item, COUNT(*) AS val FROM purchase_order
UNION ALL SELECT 'purchase_order_details', COUNT(*) FROM purchase_order_detail
UNION ALL SELECT 'purchase_receipts', COUNT(*) FROM purchase_receipt
UNION ALL SELECT 'purchase_receipt_details', COUNT(*) FROM purchase_receipt_detail
UNION ALL SELECT 'purchase_returns', COUNT(*) FROM purchase_return;
SELECT '' AS '';

-- 4.5 Requisition chain
SELECT '4.5 Requisition chain:' AS '';
SELECT 'requisition_orders' AS check_item, COUNT(*) AS val FROM requisition_order
UNION ALL SELECT 'requisition_details', COUNT(*) FROM requisition_detail;
SELECT '' AS '';

-- 4.6 Inventory
SELECT '4.6 Inventory:' AS '';
SELECT 'inventory_log_rows' AS check_item, COUNT(*) AS val FROM ingredient_inventory_log
UNION ALL SELECT 'inventory_summary_rows', COUNT(*) FROM inventory_summary;
SELECT '' AS '';

-- 4.7 Loss/Take/Transfer
SELECT '4.7 Loss/Take/Transfer:' AS '';
SELECT 'stock_loss' AS check_item, COUNT(*) AS val FROM stock_loss
UNION ALL SELECT 'stock_take', COUNT(*) FROM stock_take
UNION ALL SELECT 'stock_transfer', COUNT(*) FROM stock_transfer;
SELECT '' AS '';

-- =============================================================================
-- PART 5: Data Quality
-- =============================================================================
SELECT '### PART 5: DATA QUALITY ###' AS '';
SELECT '' AS '';

-- 5.1 dish_recipe problems
SELECT '5.1 dish_recipe quality:' AS '';
SELECT 'total_rows' AS check_item, COUNT(*) AS val FROM dish_recipe
UNION ALL SELECT 'unit_is_null', COUNT(*) FROM dish_recipe WHERE unit IS NULL
UNION ALL SELECT 'name_equals_id(dirty)', COUNT(*) FROM dish_recipe WHERE ingredient_name = ingredient_id
UNION ALL SELECT 'wastage_yield_both_zero', COUNT(*) FROM dish_recipe WHERE wastage_rate=0 AND yield_rate=0
UNION ALL SELECT 'store_2_rows', COUNT(*) FROM dish_recipe WHERE store_id=2
UNION ALL SELECT 'store_1_rows', COUNT(*) FROM dish_recipe WHERE store_id=1;
SELECT '' AS '';

-- 5.2 Cost extremes
SELECT '5.2 dish_recipe cost extremes:' AS '';
SELECT 
  MIN(total_cost) AS min_cost,
  MAX(total_cost) AS max_cost,
  ROUND(AVG(total_cost),2) AS avg_cost,
  SUM(CASE WHEN total_cost < 0 THEN 1 ELSE 0 END) AS negative,
  SUM(CASE WHEN total_cost = 0 THEN 1 ELSE 0 END) AS zero_cost,
  SUM(CASE WHEN total_cost > 100 THEN 1 ELSE 0 END) AS over_100
FROM dish_recipe;
SELECT '' AS '';

-- 5.3 ingredient_purchase quality
SELECT '5.3 ingredient_purchase quality:' AS '';
SELECT 'total' AS check_item, COUNT(*) AS val FROM ingredient_purchase
UNION ALL SELECT 'qty_zero(invalid)', COUNT(*) FROM ingredient_purchase WHERE purchase_quantity=0
UNION ALL SELECT 'price_zero(missing)', COUNT(*) FROM ingredient_purchase WHERE purchase_price=0
UNION ALL SELECT 'store_1', COUNT(*) FROM ingredient_purchase WHERE store_id=1
UNION ALL SELECT 'store_2', COUNT(*) FROM ingredient_purchase WHERE store_id=2;
SELECT '' AS '';

-- 5.4 ingredient_purchase data sample
SELECT '5.4 ingredient_purchase sample (all rows):' AS '';
SELECT purchase_id, store_id, ingredient_id, purchase_date, purchase_quantity, purchase_price, purchase_total, quantity, unit_price, total_amount, status FROM ingredient_purchase ORDER BY purchase_id;
SELECT '' AS '';

-- 5.5 purchase_order_detail sample
SELECT '5.5 purchase_order_detail sample (first 10):' AS '';
SELECT detail_id, order_id, store_id, line_no, ingredient_id, ingredient_name, unit, quantity, unit_price, amount, received_quantity FROM purchase_order_detail ORDER BY detail_id LIMIT 10;
SELECT '' AS '';

-- 5.6 requisition detail sample
SELECT '5.6 requisition_detail sample:' AS '';
SELECT detail_id, requisition_id, store_id, line_no, ingredient_id, ingredient_name, unit, request_quantity, issue_quantity FROM requisition_detail ORDER BY detail_id;
SELECT '' AS '';

-- =============================================================================
-- PART 6: Cross-table consistency
-- =============================================================================
SELECT '### PART 6: CROSS-TABLE CONSISTENCY ###' AS '';
SELECT '' AS '';

-- 6.1 Missing ingredients
SELECT '6.1 Ingredients in recipe but missing from master:' AS '';
SELECT DISTINCT dr.ingredient_id, MAX(dr.ingredient_name) AS ing_name
FROM dish_recipe dr
LEFT JOIN ingredient_master im ON dr.ingredient_id=im.ingredient_id
WHERE im.ingredient_id IS NULL
GROUP BY dr.ingredient_id
ORDER BY dr.ingredient_id;
SELECT '' AS '';

-- 6.2 Missing ingredients from purchase_order_detail
SELECT '6.2 Ingredients in purchase_order_detail but missing from master:' AS '';
SELECT DISTINCT pod.ingredient_id, MAX(pod.ingredient_name) AS ing_name
FROM purchase_order_detail pod
LEFT JOIN ingredient_master im ON pod.ingredient_id=im.ingredient_id
WHERE im.ingredient_id IS NULL
GROUP BY pod.ingredient_id;
SELECT '' AS '';

-- 6.3 Missing from requisition_detail
SELECT '6.3 Ingredients in requisition_detail but missing from master:' AS '';
SELECT DISTINCT rd.ingredient_id, MAX(rd.ingredient_name) AS ing_name
FROM requisition_detail rd
LEFT JOIN ingredient_master im ON rd.ingredient_id=im.ingredient_id
WHERE im.ingredient_id IS NULL
GROUP BY rd.ingredient_id;
SELECT '' AS '';

-- 6.4 Empty tables check
SELECT '6.4 Empty tables (should have data):' AS '';
SELECT 'ingredient_cost_price' AS tbl, COUNT(*) AS cnt FROM ingredient_cost_price
UNION ALL SELECT 'yield_rate_config', COUNT(*) FROM yield_rate_config
UNION ALL SELECT 'unit_conversion', COUNT(*) FROM unit_conversion
UNION ALL SELECT 'dish_cost_card', COUNT(*) FROM dish_cost_card
UNION ALL SELECT 'dish_cost_card_detail', COUNT(*) FROM dish_cost_card_detail;
SELECT '' AS '';

-- =============================================================================
-- PART 7: Data Flow Summary
-- =============================================================================
SELECT '### PART 7: DATA FLOW BREAK SUMMARY ###' AS '';
SELECT '' AS '';

-- Level 1
SELECT 'L1: ingr_master(archive)' AS level,
  CASE WHEN COUNT(*)=0 THEN 'BROKEN(empty)' ELSE 'OK' END AS status
FROM ingredient_master;

-- Level 2
SELECT 'L2: recipe->master(FK)' AS level,
  CASE 
    WHEN (SELECT COUNT(DISTINCT dr.ingredient_id) FROM dish_recipe dr LEFT JOIN ingredient_master im ON dr.ingredient_id=im.ingredient_id WHERE im.ingredient_id IS NULL) > 0 
    THEN CONCAT('BROKEN(', (SELECT COUNT(DISTINCT dr.ingredient_id) FROM dish_recipe dr LEFT JOIN ingredient_master im ON dr.ingredient_id=im.ingredient_id WHERE im.ingredient_id IS NULL), ' orphan ingreds)')
    ELSE 'OK'
  END AS status;

-- Level 3
SELECT 'L3: cost_price(conversion)' AS level,
  CASE WHEN COUNT(*)=0 THEN 'BROKEN(empty)' ELSE CONCAT('OK(', COUNT(*), ' rows)') END AS status
FROM ingredient_cost_price;

-- Level 4
SELECT 'L4: yield_rate_config' AS level,
  CASE WHEN COUNT(*)=0 THEN 'BROKEN(empty)' ELSE CONCAT('OK(', COUNT(*), ' rows)') END AS status
FROM yield_rate_config;

-- Level 5
SELECT 'L5: unit_conversion' AS level,
  CASE WHEN COUNT(*)=0 THEN 'BROKEN(empty)' ELSE CONCAT('OK(', COUNT(*), ' rows)') END AS status
FROM unit_conversion;

-- Level 6
SELECT 'L6: purchase_data' AS level,
  CASE 
    WHEN (SELECT COUNT(*) FROM ingredient_purchase)=0 AND (SELECT COUNT(*) FROM purchase_order)=0
    THEN 'BROKEN(no data)'
    ELSE CONCAT('WARN(ingr_purchase:', (SELECT COUNT(*) FROM ingredient_purchase), ' purchase_order:', (SELECT COUNT(*) FROM purchase_order), ')')
  END AS status;

-- Level 7
SELECT 'L7: dish_cost_card' AS level,
  CASE WHEN COUNT(*)=0 THEN 'BROKEN(empty)' ELSE CONCAT('OK(', COUNT(*), ' cards)') END AS status
FROM dish_cost_card;

-- Level 8
SELECT 'L8: inventory_summary' AS level,
  CASE WHEN COUNT(*)=0 THEN 'WARN(empty)' ELSE CONCAT('OK(', COUNT(*), ' rows)') END AS status
FROM inventory_summary;

-- =============================================================================
-- PART 8: Solo's new SQL files vs DB reality
-- =============================================================================
SELECT '' AS '';
SELECT '### PART 8: SOLO SQL FILES vs DB REALITY ###' AS '';
SELECT '' AS '';

SELECT 'enhance_food_material_tables.sql (!ALTER 4 tables)' AS sql_file,
  CASE WHEN EXISTS(SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master' AND COLUMN_NAME='pinyin_code')
  THEN 'EXECUTED' ELSE 'NOT_EXECUTED' END AS db_status
UNION ALL SELECT 'purchase_flow_tables.sql (!Create 4 purchase tables)',
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_request')
  THEN 'EXECUTED' ELSE 'NOT_EXECUTED' END
UNION ALL SELECT 'stock_module_tables.sql (!Create 6 stock tables)',
  CASE WHEN EXISTS(SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_inventory')
  THEN 'EXECUTED' ELSE 'NOT_EXECUTED' END
UNION ALL SELECT 'seed_* series (seed data 7 files)',
  CASE WHEN (SELECT COUNT(*) FROM ingredient_master) > 0 THEN 'MAYBE_EXECUTED' ELSE 'NOT_EXECUTED' END;

SELECT '' AS '';
SELECT '========================================================================' AS '';
SELECT 'CHECKUP COMPLETE.' AS '';
SELECT '========================================================================' AS '';

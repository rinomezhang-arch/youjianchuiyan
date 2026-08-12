-- =============================================================================
-- ultimate_checkup_v3.sql
-- 又见炊烟餐饮管理系统2.0 - 终极体检V3
-- 新增：区分"查询列 vs 计算列"，验证计算列是否符合公式
-- 新增：检查solo是否直接抄了值到计算列（偷懒检测）
-- =============================================================================

SELECT '========================================' AS '';
SELECT '  ULTIMATE CHECKUP V3 -- DERIVED vs CALCULATED' AS '';
SELECT '========================================' AS '';
SELECT '' AS '';

-- =============================================================================
-- PART 0: 核心概念 —— 查询列 vs 计算列
-- =============================================================================
-- 
-- 查询列 (Lookup/Input): 从源数据直接复制，不需要计算
--   例: dish_name, ingredient_name, purchase_price(Excel原值), brand, spec
-- 
-- 计算列 (Calculated): 必须通过公式推导，直接从源数据复制=作弊
--   例: net_price_per_unit = purchase_price / conversion_rate / (yield_rate/100)
--   例: total_cost = quantity × unit_price × (1 + wastage_rate/100)
--   例: cost_card.standard_cost = SUM(dish_recipe.total_cost)
--   例: dish_master.cost_rate = cost_price / sale_price × 100
--   例: ingredient_cost_price.net_price_per_unit = purchase_price / conversion_rate / (yield_rate/100)
--
-- 铁律：计算列必须来源于父表数据运算，不能直接抄Excel！
-- =============================================================================

SELECT '' AS '';

-- =============================================================================
-- PART 1: 列出所有表的所有字段，标注 查询列 vs 计算列
-- =============================================================================
SELECT '### PART 1: COLUMN CLASSIFICATION (INPUT vs CALCULATED) ###' AS '';
SELECT '' AS '';

-- 1.1 dish_master
SELECT 'TABLE: dish_master (50 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('dish_id','store_id','dish_name','dish_category','spicy_level','main_ingredient_type','main_ingredient','english_name','sale_price','cooking_time','servings','usage_type','sort_order','is_active','image_url','dish_intro','tiktok_recommend','festive_name','menu_type','category','category_id','cooking_method','dish_code','dish_name_en','is_seasonal','is_specialty','main_ingredients','taste','unit','remark')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('cost_price','cost_rate')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('birthday_name','wedding_name','house_move_name','promotion_name','reunion_name','thanksgiving_name','year_end_name','baby_born_name','business_name','spring_name','school_name','victory_name','opening_name','comrade_name','teacher_name','adult_name')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_master'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.2 ingredient_master  
SELECT 'TABLE: ingredient_master (24 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('ingredient_id','store_id','ingredient_name','ingredient_category','brand','purchase_unit','spec','usage_unit','primary_supplier_id','warning_threshold','sort_order','is_active','min_stock','status')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('conversion_rate','yield_rate','current_stock','last_entry_date','unit_price')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('created_at','updated_at')
    THEN 'AUTO'
    WHEN COLUMN_NAME IN ('category','unit','supplier_id')
    THEN 'DUP(remove)'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_master'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.3 ingredient_cost_price
SELECT 'TABLE: ingredient_cost_price (12 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('ingredient_id','ingredient_name','purchase_unit','purchase_price')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('conversion_unit','conversion_rate','yield_rate','net_price_per_unit')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('cost_price_id','store_id','created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_cost_price'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.4 ingredient_purchase
SELECT 'TABLE: ingredient_purchase (21 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('store_id','ingredient_id','supplier_id','purchase_date','purchase_quantity','purchase_price','operator_id','status','approved_by','approved_at','notes')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('purchase_total','usage_quantity','usage_price')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('purchase_id','created_at','updated_at')
    THEN 'AUTO'
    WHEN COLUMN_NAME IN ('quantity','unit_price','total_amount','processing_note')
    THEN 'DUP(remove)'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='ingredient_purchase'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.5 yield_rate_config
SELECT 'TABLE: yield_rate_config (16 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('ingredient_id','ingredient_name','category','raw_unit','processed_unit','loss_reason','status','effective_date','created_by')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('standard_yield_rate','min_yield_rate','max_yield_rate')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('config_id','store_id','created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='yield_rate_config'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.6 unit_conversion
SELECT 'TABLE: unit_conversion (12 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('from_unit','to_unit','category','description','status','created_by')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('conversion_rate','reverse_rate')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('conversion_id','store_id','created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='unit_conversion'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.7 purchase_order
SELECT 'TABLE: purchase_order (23 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('store_id','order_no','supplier_id','supplier_name','order_date','expected_date','status','order_type','purchaser_id','purchaser_name','approver_id','approver_name','approve_time','warehouse_keeper_id','warehouse_keeper_name','remark')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('total_quantity','total_amount','received_quantity','received_amount')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('order_id','created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_order'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.8 purchase_order_detail
SELECT 'TABLE: purchase_order_detail (16 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('order_id','store_id','line_no','ingredient_id','ingredient_name','category','spec','unit','quantity','unit_price','remark')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('amount','received_quantity','returned_quantity')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('detail_id','created_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_order_detail'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.9 purchase_receipt
SELECT 'TABLE: purchase_receipt (17 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('store_id','receipt_no','receipt_date','order_id','order_no','supplier_id','supplier_name','status','warehouse_keeper_id','warehouse_keeper_name','delivery_person','remark')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('total_quantity','total_amount')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('receipt_id','created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_receipt'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.10 purchase_receipt_detail
SELECT 'TABLE: purchase_receipt_detail (17 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('receipt_id','store_id','line_no','order_detail_id','ingredient_id','ingredient_name','category','spec','unit','order_quantity','actual_quantity','unit_price','quality_status','remark')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('amount')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('detail_id','created_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='purchase_receipt_detail'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.11 dish_recipe
SELECT 'TABLE: dish_recipe (17 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('store_id','dish_id','ingredient_id','ingredient_name','unit','quantity','sort_order')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('unit_price','yield_rate','wastage_rate','net_unit_price')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('total_cost')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('recipe_id','created_at','updated_at','last_entry_date','notes')
    THEN 'AUTO/INFO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_recipe'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.12 dish_cost_card
SELECT 'TABLE: dish_cost_card (17 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('dish_id','dish_name','dish_category','status','effective_date','created_by')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('standard_yield','actual_yield','yield_rate','standard_cost','actual_cost','selling_price','gross_margin')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('cost_card_id','store_id','created_at','updated_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_cost_card'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- 1.13 dish_cost_card_detail
SELECT 'TABLE: dish_cost_card_detail (16 cols)' AS '';
SELECT 
  CASE 
    WHEN COLUMN_NAME IN ('cost_card_id','store_id','line_no','ingredient_id','ingredient_name','spec','unit','remark')
    THEN 'INPUT'
    WHEN COLUMN_NAME IN ('standard_quantity','actual_quantity','converted_quantity','unit_price','cost_amount','yield_rate')
    THEN 'CALC'
    WHEN COLUMN_NAME IN ('detail_id','created_at')
    THEN 'AUTO'
    ELSE 'UNKNOWN'
  END AS col_type,
  COLUMN_NAME,
  COLUMN_COMMENT
FROM information_schema.COLUMNS 
WHERE TABLE_SCHEMA='banquet' AND TABLE_NAME='dish_cost_card_detail'
ORDER BY ORDINAL_POSITION;

SELECT '' AS '';

-- =============================================================================
-- PART 2: 计算列公式验证
-- =============================================================================
SELECT '### PART 2: CALCULATED COLUMN FORMULA VALIDATION ###' AS '';
SELECT '' AS '';

-- 2.1 ingredient_cost_price: net_price_per_unit = purchase_price / conversion_rate / (yield_rate/100)
SELECT '2.1 ingredient_cost_price - net_price formula check:' AS '';
SELECT 
  COUNT(*) AS total_rows,
  SUM(CASE WHEN purchase_price > 0 AND conversion_rate > 0 AND yield_rate > 0 
    AND ABS(ROUND(net_price_per_unit,4) - ROUND(purchase_price / conversion_rate / (yield_rate/100), 4)) < 0.0001 
    THEN 1 ELSE 0 END) AS formula_OK,
  SUM(CASE WHEN purchase_price > 0 AND conversion_rate > 0 AND yield_rate > 0 
    AND ABS(ROUND(net_price_per_unit,4) - ROUND(purchase_price / conversion_rate / (yield_rate/100), 4)) >= 0.0001 
    THEN 1 ELSE 0 END) AS formula_WRONG,
  SUM(CASE WHEN purchase_price <= 0 OR conversion_rate <= 0 OR yield_rate <= 0 THEN 1 ELSE 0 END) AS invalid_input
FROM ingredient_cost_price;
SELECT '' AS '';

-- 2.2 抽查5条错误示例
SELECT '2.2 Wrong net_price samples:' AS '';
SELECT ingredient_name, ROUND(purchase_price,4) AS price, conversion_rate AS conv, yield_rate AS yield, 
  ROUND(net_price_per_unit,4) AS db_value,
  ROUND(purchase_price / conversion_rate / (yield_rate/100), 4) AS should_be
FROM ingredient_cost_price
WHERE purchase_price > 0 AND conversion_rate > 0 AND yield_rate > 0
  AND ABS(ROUND(net_price_per_unit,4) - ROUND(purchase_price / conversion_rate / (yield_rate/100), 4)) >= 0.0001
LIMIT 5;
SELECT '' AS '';

-- 2.3 purchase_order_detail: amount = quantity × unit_price
SELECT '2.3 purchase_order_detail - amount formula check:' AS '';
SELECT 
  COUNT(*) AS total,
  SUM(CASE WHEN ABS(ROUND(amount,2) - ROUND(quantity * unit_price, 2)) < 0.01 THEN 1 ELSE 0 END) AS OK,
  SUM(CASE WHEN ABS(ROUND(amount,2) - ROUND(quantity * unit_price, 2)) >= 0.01 THEN 1 ELSE 0 END) AS WRONG
FROM purchase_order_detail;
SELECT '' AS '';

-- 2.4 purchase_receipt_detail: amount = actual_quantity × unit_price
SELECT '2.4 purchase_receipt_detail - amount formula check:' AS '';
SELECT 
  COUNT(*) AS total,
  SUM(CASE WHEN ABS(ROUND(amount,2) - ROUND(actual_quantity * unit_price, 2)) < 0.01 THEN 1 ELSE 0 END) AS OK,
  SUM(CASE WHEN ABS(ROUND(amount,2) - ROUND(actual_quantity * unit_price, 2)) >= 0.01 THEN 1 ELSE 0 END) AS WRONG
FROM purchase_receipt_detail;
SELECT '' AS '';

-- 2.5 purchase_order: total_quantity = SUM(detail.quantity), total_amount = SUM(detail.amount)
SELECT '2.5 purchase_order - total vs detail SUM check:' AS '';
SELECT 
  po.order_id,
  po.total_quantity AS po_total_qty,
  ROUND(SUM(pod.quantity), 2) AS sum_detail_qty,
  po.total_amount AS po_total_amt,
  ROUND(SUM(pod.amount), 2) AS sum_detail_amt,
  CASE 
    WHEN ABS(po.total_quantity - ROUND(SUM(pod.quantity),2)) > 0.01 OR ABS(po.total_amount - ROUND(SUM(pod.amount),2)) > 0.01
    THEN 'MISMATCH' ELSE 'OK' 
  END AS verdict
FROM purchase_order po
JOIN purchase_order_detail pod ON po.order_id = pod.order_id
GROUP BY po.order_id, po.total_quantity, po.total_amount
ORDER BY po.order_id;
SELECT '' AS '';

-- 2.6 purchase_receipt: total = SUM(detail.amount)
SELECT '2.6 purchase_receipt - total vs detail SUM check:' AS '';
SELECT 
  pr.receipt_id,
  pr.total_quantity AS pr_total_qty,
  ROUND(SUM(prd.actual_quantity), 2) AS sum_detail_qty,
  pr.total_amount AS pr_total_amt,
  ROUND(SUM(prd.amount), 2) AS sum_detail_amt,
  CASE 
    WHEN ABS(pr.total_quantity - ROUND(SUM(prd.actual_quantity),2)) > 0.01 OR ABS(pr.total_amount - ROUND(SUM(prd.amount),2)) > 0.01
    THEN 'MISMATCH' ELSE 'OK' 
  END AS verdict
FROM purchase_receipt pr
JOIN purchase_receipt_detail prd ON pr.receipt_id = prd.receipt_id
GROUP BY pr.receipt_id, pr.total_quantity, pr.total_amount
ORDER BY pr.receipt_id;
SELECT '' AS '';

-- =============================================================================
-- PART 3: 偷懒检测 —— 计算列是否直接从Excel抄的
-- =============================================================================
SELECT '### PART 3: LAZY COPY DETECTION ###' AS '';
SELECT '' AS '';

-- 3.1 dish_master.cost_price is CALC (from cost_card.standard_cost), 
--     but cost_card is EMPTY, so cost_price must be copied from Excel = CHEATING
SELECT '3.1 dish_master.cost_price - COPIED FROM EXCEL? (cost_card is empty):' AS '';
SELECT 
  COUNT(*) AS dishes_with_cost,
  'ALL 348 cost_price values are DIRECT COPIES from Excel, not computed from cost cards. cost_card table is EMPTY.' AS verdict
FROM dish_master WHERE cost_price IS NOT NULL AND cost_price > 0;
SELECT '' AS '';

-- 3.2 dish_master.cost_rate should = cost_price / sale_price, check
SELECT '3.2 dish_master.cost_rate formula check:' AS '';
SELECT 
  COUNT(*) AS total,
  SUM(CASE WHEN sale_price > 0 AND ABS(ROUND(cost_rate,1) - ROUND(cost_price / sale_price * 100, 1)) < 0.5 THEN 1 ELSE 0 END) AS formula_OK,
  SUM(CASE WHEN sale_price > 0 AND ABS(ROUND(cost_rate,1) - ROUND(cost_price / sale_price * 100, 1)) >= 0.5 THEN 1 ELSE 0 END) AS formula_WRONG
FROM dish_master;
SELECT '' AS '';

-- 3.3 ingredient_cost_price.purchase_price - was it extracted from Excel or invented?
-- Excel oil price = 0.006848 yuan/g. DB has 0.01. Check if ANY purchase_price matches Excel.
SELECT '3.3 ingredient_cost_price - purchase_price vs Excel source check:' AS '';
SELECT 
  '龙虾(20头) Excel=0.03424 DB=' || ROUND((SELECT purchase_price FROM ingredient_cost_price WHERE ingredient_name='龙虾（20头）' LIMIT 1),5) AS check_item,
  '五花肉 Excel=0.0107 DB=' || ROUND((SELECT purchase_price FROM ingredient_cost_price WHERE ingredient_name='五花肉' LIMIT 1),5) AS check_item2,
  '八角 Excel=0.02561 DB=' || ROUND((SELECT purchase_price FROM ingredient_cost_price WHERE ingredient_name='八角' LIMIT 1),5) AS check_item3,
  '食用油 Excel=0.006848 DB=' || ROUND((SELECT purchase_price FROM ingredient_cost_price WHERE ingredient_name='食用油' LIMIT 1),5) AS check_item4,
  '啤酒 Excel=1.25/瓶 DB=' || ROUND((SELECT purchase_price FROM ingredient_cost_price WHERE ingredient_name='天目湖啤酒' LIMIT 1),5) AS check_item5;
SELECT '' AS '';
SELECT '3.3 VERDICT: purchase_price values do NOT match Excel source. They appear INVENTED/ROUNDED.' AS '';
SELECT '' AS '';

-- =============================================================================
-- PART 4: 数据流完整度
-- =============================================================================
SELECT '### PART 4: DATA FLOW COMPLETENESS ###' AS '';
SELECT '' AS '';

SELECT 'L1_supplier_master(INPUT)' AS layer, CONCAT(COUNT(*),' rows') AS status FROM supplier_master
UNION ALL
SELECT 'L2_ingredient_master(INPUT+CALC)', CONCAT(COUNT(*),' rows, supplier_id all NULL=', (SELECT COUNT(*) FROM ingredient_master WHERE primary_supplier_id IS NULL)) FROM ingredient_master
UNION ALL
SELECT 'L3_purchase_order(INPUT+CALC)', CONCAT(COUNT(*),' orders, FK broken=', (SELECT COUNT(*) FROM purchase_order po WHERE NOT EXISTS (SELECT 1 FROM supplier_master sm WHERE sm.supplier_id=po.supplier_id))) FROM purchase_order
UNION ALL
SELECT 'L4_purchase_receipt(INPUT+CALC)', CONCAT(COUNT(*),' receipts') FROM purchase_receipt
UNION ALL
SELECT 'L5_ingredient_cost_price(CALC)', CONCAT(COUNT(*),' rows, formula OK=', (SELECT SUM(CASE WHEN purchase_price>0 AND conversion_rate>0 AND yield_rate>0 AND ABS(ROUND(net_price_per_unit,4)-ROUND(purchase_price/conversion_rate/(yield_rate/100),4))<0.0001 THEN 1 ELSE 0 END) FROM ingredient_cost_price)) FROM ingredient_cost_price
UNION ALL
SELECT 'L6_unit_conversion(INPUT+CALC)', CONCAT(COUNT(*),' rows (EMPTY!)') FROM unit_conversion
UNION ALL
SELECT 'L7_yield_rate_config(INPUT+CALC)', CONCAT(COUNT(*),' rows, yield=100:', (SELECT COUNT(*) FROM yield_rate_config WHERE standard_yield_rate=100)) FROM yield_rate_config
UNION ALL
SELECT 'L8_dish_recipe(INPUT+CALC)', CONCAT(COUNT(*),' rows (EMPTY!)') FROM dish_recipe
UNION ALL
SELECT 'L9_dish_cost_card(CALC)', CONCAT(COUNT(*),' rows (EMPTY!)') FROM dish_cost_card
UNION ALL
SELECT 'L10_dish_master(INPUT+CALC)', CONCAT(COUNT(*),' dishes, cost copied from Excel') FROM dish_master;

SELECT '' AS '';
SELECT '### FINAL VERDICT ###' AS '';
SELECT '' AS '';
SELECT 'CALCULATED columns that are WRONG (formula mismatch): ' || 
  (SELECT COUNT(*) FROM ingredient_cost_price WHERE purchase_price>0 AND conversion_rate>0 AND yield_rate>0 
    AND ABS(ROUND(net_price_per_unit,4) - ROUND(purchase_price/conversion_rate/(yield_rate/100),4)) >= 0.0001) || 
  ' out of ' || (SELECT COUNT(*) FROM ingredient_cost_price WHERE purchase_price>0) AS net_price_issue;

SELECT 'CALCULATED columns that are COPIED (not computed):' AS issue,
  'dish_master.cost_price (348 rows) - cost_card is empty, values come from Excel directly' AS detail;

SELECT 'TABLES that should EXIST but are EMPTY:' AS issue,
  'unit_conversion(0), dish_recipe(0), dish_cost_card(0), dish_cost_card_detail(0)' AS detail;

SELECT 'TABLES with FK broken:' AS issue,
  'purchase_order(4/4 supplier_id=1 not in supplier_master), ingredient_master(1209 NULL supplier_id)' AS detail;

SELECT 'OVERALL: DATA IS NOT LEGAL' AS verdict,
  'Calculated columns fail formula check. Core tables empty. FK broken. Do not use as production data.' AS reason;

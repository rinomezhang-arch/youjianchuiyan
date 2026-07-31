-- 列名 + 类型
SELECT table_name, column_name, data_type
FROM information_schema.columns
WHERE table_schema='banquet'
  AND (table_name LIKE 'purchase_%_detail'
       OR table_name LIKE 'stock_%'
       OR table_name LIKE 'marketing_%'
       OR table_name LIKE 'member_%'
       OR table_name LIKE 'report_%')
ORDER BY table_name, ordinal_position;

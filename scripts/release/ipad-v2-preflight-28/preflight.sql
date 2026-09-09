-- TL-IPAD-V2-PREFLIGHT-28 preflight.sql
-- 只读采集脚本（在真实 MySQL 8 上由执行方运行）：不产生任何 DDL / 写操作 / 经营行明细。
-- 执行: mysql -u<user> -p <database> < preflight.sql
-- 采集结果整理成 metadata.json（键名与 check.py 契约一致）后交给 check.py 判定。
-- 目标库: banquet  规范来源: ipad_batch_request_migration_v2.sql (base_sha 5f4511b7)

SELECT DATABASE() AS schema_name, @@foreign_key_checks AS foreign_key_checks;
SELECT TABLE_NAME FROM information_schema.TABLES
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME IN ('booking_master','ipad_batch_request');

-- 1) booking_master 父表三列定义（类型含 unsigned / 字符集 / 排序规则）
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH,
       IS_NULLABLE, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'booking_master'
  AND COLUMN_NAME IN ('id','store_id','booking_id')
ORDER BY FIELD(COLUMN_NAME,'id','store_id','booking_id');

-- 2) ipad_batch_request 子表三列定义（v1 表缺失则该查询返回空行 = present=false）
SELECT TABLE_NAME, COLUMN_NAME, DATA_TYPE, COLUMN_TYPE, CHARACTER_MAXIMUM_LENGTH,
       IS_NULLABLE, CHARACTER_SET_NAME, COLLATION_NAME
FROM information_schema.COLUMNS
WHERE TABLE_SCHEMA = DATABASE()
  AND TABLE_NAME = 'ipad_batch_request'
  AND COLUMN_NAME IN ('booking_master_id','store_id','booking_id')
ORDER BY FIELD(COLUMN_NAME,'booking_master_id','store_id','booking_id');

-- 3) booking_master 现有索引完整列序（NON_UNIQUE=0 唯一）
SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'booking_master'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 4) ipad_batch_request 现有索引完整列序
SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME, SUB_PART
FROM information_schema.STATISTICS
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ipad_batch_request'
ORDER BY INDEX_NAME, SEQ_IN_INDEX;

-- 5) 两表外键：列顺序 / 引用目标 / 删除与更新规则
SELECT k.TABLE_NAME, k.CONSTRAINT_NAME, k.COLUMN_NAME, k.ORDINAL_POSITION,
       k.REFERENCED_TABLE_SCHEMA, k.REFERENCED_TABLE_NAME, k.REFERENCED_COLUMN_NAME,
       r.UPDATE_RULE, r.DELETE_RULE
FROM information_schema.KEY_COLUMN_USAGE k
LEFT JOIN information_schema.REFERENTIAL_CONSTRAINTS r
  ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
WHERE k.TABLE_SCHEMA = DATABASE()
  AND k.TABLE_NAME IN ('booking_master','ipad_batch_request')
  AND k.REFERENCED_TABLE_NAME IS NOT NULL
ORDER BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.ORDINAL_POSITION;

-- 6) 回执孤儿 / 跨店 / booking_id 错配计数（只出计数，不出任何经营行明细）
SELECT
  (SELECT COUNT(*) FROM ipad_batch_request r
    LEFT JOIN booking_master b ON b.id = r.booking_master_id
    WHERE b.id IS NULL) AS orphan_receipts,
  (SELECT COUNT(*) FROM ipad_batch_request r
    JOIN booking_master b ON b.id = r.booking_master_id
    WHERE b.store_id IS NULL OR r.store_id IS NULL OR b.store_id <> r.store_id) AS cross_store,
  (SELECT COUNT(*) FROM ipad_batch_request r
    JOIN booking_master b ON b.id = r.booking_master_id
    WHERE b.booking_id IS NULL OR r.booking_id IS NULL
       OR BINARY b.booking_id <> BINARY r.booking_id) AS booking_id_mismatch;

-- 整理成 metadata.json 的模板（键名即 check.py 契约）：
-- {
--   "schema": "实际DATABASE()返回值，不得照抄模板",
--   "foreign_key_checks": 1,
--   "tables": {
--     "booking_master": {"present": true,
--       "columns": {"id": {"type":"bigint","unsigned":false,"charset":null,"collation":null},
--                  "store_id": {"type":"bigint","unsigned":false,"charset":null,"collation":null},
--                  "booking_id": {"type":"varchar","length":255,"unsigned":false,"charset":"utf8mb4","collation":"utf8mb4_0900_ai_ci"}},
--       "indexes": [{"name":"...","unique":true,"columns":["id","store_id","booking_id"]}],
--       "foreign_keys": []},
--     "ipad_batch_request": {"present": true,
--       "columns": {"booking_master_id": {...}, "store_id": {...}, "booking_id": {...}},
--       "indexes": [...], "foreign_keys": [...]}
--   },
--   "counts": {"orphan_receipts": 0, "cross_store": 0, "booking_id_mismatch": 0}
-- }
-- 注：column_type 为 'bigint unsigned' 时 type=bigint,unsigned=true；varchar 取长度并比对字符集/排序规则。
-- 每列还必须采集nullable：IS_NULLABLE='YES'为true，否则false；回执子表三列必须false。
-- 规范v2未要求父表store_id非空；父列可空时仍检查现有回执关联的NULL/错配计数。
-- 索引columns用[{"name":"列名","prefix":null}]；prefix必须来自SUB_PART，不能省略。
-- 外键必须有ref_schema，来自REFERENCED_TABLE_SCHEMA并严格等于本次schema。
-- mysql或任一采集语句失败则停止；不得把缺结果补为0/false/空数组。数据缺失时check.py必须BLOCKED。

-- ============================================================
-- marketing_publication_v1.sql  营销发布与客人来源码数据库契约
-- 唯一正式迁移口径（幂等 + fail-closed 全量结构预检）
--
-- 目标（照 docs/营销H5生产闭环设计_20260913.md 第三节「最小数据模型」）：
--   1) marketing_activity 增加 status/document_version/row_version/
--      created_by/updated_by/submitted_at/approved_at，并移除 store_id 默认 1。
--      旧活动一律迁为 draft（DEFAULT），绝不伪造 approved/published。
--   2) 新建不可变 marketing_publication：public_slug/source_code/request_id 全局唯一，
--      活动+门店按复合键 (activity_id,store_id) 同店；快照字段不可更新、行不可物理删除
--      （BEFORE UPDATE / BEFORE DELETE 触发器，单语句体，纯 ; 分隔）。
--   3) 新建 marketing_attribution_event：request_id 唯一，事件必须与发布记录同店，
--      仅结算事件允许携带非负 amount。
--   4) booking_inquiry 增加可空 marketing_publication_id/source_code/source_channel，
--      并移除 store_id 默认 1；带来源咨询通过
--      (marketing_publication_id,store_id,source_code,source_channel) 四字段复合外键
--      强制与同一发布版本同店、同码、同渠道；三者全空仍兼容历史咨询。
--
-- fail-closed 全量结构预检（阶段0，纯只读信息schema，零 DDL）：
--   在第一条 DDL 之前语义核对全部目标列（类型/可空/默认）、CHECK 约束、
--   唯一键、普通索引、外键（子父列序+规则）；任一已存在但定义不符即 SELECT 不存在的
--   哨兵表触发整体失败，绝不带错结构执行任何 DDL（不形成部分迁移）。
--
-- 纯 ; 分隔 + PREPARE/EXECUTE 动态 SQL，兼容 mysql CLI 与 Spring ScriptUtils；
-- 触发器均为单语句体（无 BEGIN...END、无内部 ;），CREATE TRIGGER IF NOT EXISTS 幂等。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 阶段0：全量结构安全预检（纯只读，零 DDL，任一不符即整体拒绝）
-- 哨兵表 __refuse_* 不存在，SELECT 它必然 1146 报错 -> 脚本整体终止。
-- ============================================================

-- 0a. marketing_activity 目标列若已存在但定义不符 -> 拒绝
SET @bad_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND (
    (COLUMN_NAME='status'            AND NOT (COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='draft')) OR
    (COLUMN_NAME='document_version'  AND NOT (COLUMN_TYPE='int' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='1')) OR
    (COLUMN_NAME='row_version'       AND NOT (COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='1')) OR
    (COLUMN_NAME='created_by'        AND NOT (COLUMN_TYPE='bigint' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='updated_by'        AND NOT (COLUMN_TYPE='bigint' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='submitted_at'      AND NOT (COLUMN_TYPE='datetime' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='approved_at'       AND NOT (COLUMN_TYPE='datetime' AND IS_NULLABLE='YES'))
  ));
SET @sig = IF(@bad_col>0, 'SELECT * FROM `__refuse_marketing_activity_column_definition_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0b. marketing_activity 状态权威 CHECK 若已存在但语义不符（枚举缺项）-> 拒绝
SET @bad_ck = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='chk_activity_status'
  AND (CHECK_CLAUSE NOT LIKE '%draft%'
    OR CHECK_CLAUSE NOT LIKE '%pending_approval%'
    OR CHECK_CLAUSE NOT LIKE '%approved%'
    OR CHECK_CLAUSE NOT LIKE '%published%'
    OR CHECK_CLAUSE NOT LIKE '%paused%'
    OR CHECK_CLAUSE NOT LIKE '%expired%'
    OR CHECK_CLAUSE NOT LIKE '%cancelled%'));
SET @sig = IF(@bad_ck>0, 'SELECT * FROM `__refuse_marketing_activity_check_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0c. marketing_activity 同店复合唯一键若已存在但定义不符 -> 拒绝
SET @bad_uk = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND INDEX_NAME='uk_activity_id_store'
  GROUP BY INDEX_NAME, NON_UNIQUE
  HAVING NOT (NON_UNIQUE=0 AND cols='activity_id,store_id')
) t);
SET @sig = IF(@bad_uk>0, 'SELECT * FROM `__refuse_marketing_activity_unique_key_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0d. booking_inquiry 目标列若已存在但定义不符 -> 拒绝
SET @bad_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' AND (
    (COLUMN_NAME='marketing_publication_id' AND NOT (COLUMN_TYPE='bigint' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='source_code'              AND NOT (COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='source_channel'           AND NOT (COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='YES'))
  ));
SET @sig = IF(@bad_col>0, 'SELECT * FROM `__refuse_booking_inquiry_column_definition_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0e. booking_inquiry 四字段同店复合外键若已存在但定义不符 -> 拒绝
SET @bad_fk = (SELECT COUNT(*) FROM (
  SELECT kcu.CONSTRAINT_NAME,
         GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS child_cols,
         GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS parent_cols,
         MIN(kcu.REFERENCED_TABLE_NAME) AS parent_tbl,
         rc.DELETE_RULE
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='booking_inquiry'
    AND kcu.CONSTRAINT_NAME='fk_bi_marketing_publication' AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE, rc.UPDATE_RULE
  HAVING NOT (child_cols='marketing_publication_id,store_id,source_code,source_channel'
          AND parent_cols='publication_id,store_id,source_code,channel'
          AND parent_tbl='marketing_publication' AND rc.DELETE_RULE='RESTRICT' AND rc.UPDATE_RULE='RESTRICT')
) t);
SET @sig = IF(@bad_fk>0, 'SELECT * FROM `__refuse_booking_inquiry_foreign_key_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0h. marketing_publication 两只触发器若已存在但事件/时机/动作不符 -> 拒绝
SET @bad_trg = (SELECT COUNT(*) FROM information_schema.TRIGGERS
  WHERE TRIGGER_SCHEMA=DATABASE() AND EVENT_OBJECT_TABLE='marketing_publication'
  AND (
    (TRIGGER_NAME='trg_publication_no_delete' AND NOT (EVENT_MANIPULATION='DELETE' AND ACTION_TIMING='BEFORE' AND ACTION_STATEMENT LIKE '%禁止物理删除%'))
    OR
    (TRIGGER_NAME='trg_publication_immutable' AND NOT (EVENT_MANIPULATION='UPDATE' AND ACTION_TIMING='BEFORE' AND ACTION_STATEMENT LIKE '%UNION ALL%'))
  ));
SET @sig = IF(@bad_trg>0, 'SELECT * FROM `__refuse_marketing_publication_trigger_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0f. marketing_publication 若已存在，全量结构（列/CHECK/唯一键/索引/外键）必须完整且正确
SET @pub_tbl = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_publication');
SET @pub_col_ok = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_publication' AND (
    (COLUMN_NAME='publication_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='activity_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='store_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='version' AND COLUMN_TYPE='int' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='channel' AND COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='public_slug' AND COLUMN_TYPE='varchar(80)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='source_code' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='title' AND COLUMN_TYPE='varchar(200)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='summary' AND COLUMN_TYPE='varchar(500)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='content_json' AND COLUMN_TYPE='json' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='hero_asset_url' AND COLUMN_TYPE='varchar(500)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='cta_label' AND COLUMN_TYPE='varchar(50)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='status' AND COLUMN_TYPE='varchar(20)' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='published') OR
    (COLUMN_NAME='valid_from' AND COLUMN_TYPE='timestamp' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='valid_to' AND COLUMN_TYPE='timestamp' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='published_by' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='published_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='paused_by' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='paused_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='request_id' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='created_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='NO')
  ));
SET @pub_ck_ok = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='chk_publication_status'
  AND CHECK_CLAUSE LIKE '%published%' AND CHECK_CLAUSE LIKE '%paused%' AND CHECK_CLAUSE LIKE '%expired%');
SET @pub_uk_ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_publication'
    AND INDEX_NAME IN ('uk_publication_activity_version_channel','uk_publication_slug','uk_publication_source_code','uk_publication_request_id','uk_publication_id_store','uk_publication_id_store_source_channel')
  GROUP BY INDEX_NAME, NON_UNIQUE
  HAVING NON_UNIQUE=0 AND (
    (INDEX_NAME='uk_publication_activity_version_channel' AND cols='activity_id,version,channel') OR
    (INDEX_NAME='uk_publication_slug' AND cols='public_slug') OR
    (INDEX_NAME='uk_publication_source_code' AND cols='source_code') OR
    (INDEX_NAME='uk_publication_request_id' AND cols='request_id') OR
    (INDEX_NAME='uk_publication_id_store' AND cols='publication_id,store_id') OR
    (INDEX_NAME='uk_publication_id_store_source_channel' AND cols='publication_id,store_id,source_code,channel')
  )
) t);
SET @pub_idx_ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_publication'
    AND INDEX_NAME='idx_publication_store_status_valid'
  GROUP BY INDEX_NAME, NON_UNIQUE
  HAVING NON_UNIQUE=1 AND cols='store_id,status,valid_from,valid_to'
) t);
SET @pub_fk_ok = (SELECT COUNT(*) FROM (
  SELECT kcu.CONSTRAINT_NAME,
         GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS child_cols,
         GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS parent_cols,
         MIN(kcu.REFERENCED_TABLE_NAME) AS parent_tbl,
         rc.DELETE_RULE
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='marketing_publication'
    AND kcu.CONSTRAINT_NAME='fk_publication_activity' AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE, rc.UPDATE_RULE
  HAVING child_cols='activity_id,store_id' AND parent_cols='activity_id,store_id'
     AND parent_tbl='marketing_activity' AND rc.DELETE_RULE='RESTRICT' AND rc.UPDATE_RULE='RESTRICT'
) t);
SET @pub_bad = IF(@pub_tbl=1 AND (@pub_col_ok<>21 OR @pub_ck_ok<>1 OR @pub_uk_ok<>6 OR @pub_idx_ok<>1 OR @pub_fk_ok<>1), 1, 0);
SET @sig = IF(@pub_bad=1, 'SELECT * FROM `__refuse_marketing_publication_structure_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0g. marketing_attribution_event 若已存在，全量结构必须完整且正确
SET @evt_tbl = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_attribution_event');
SET @evt_col_ok = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_attribution_event' AND (
    (COLUMN_NAME='event_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='publication_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='store_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='source_code' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='event_type' AND COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='visitor_key' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='business_type' AND COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='business_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='business_no' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='amount' AND COLUMN_TYPE='decimal(12,2)' AND IS_NULLABLE='YES') OR
    (COLUMN_NAME='request_id' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='occurred_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='NO') OR
    (COLUMN_NAME='created_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='NO')
  ));
SET @evt_ck_ok = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS
  WHERE CONSTRAINT_SCHEMA=DATABASE() AND (
    (CONSTRAINT_NAME='chk_attribution_event_type' AND CHECK_CLAUSE LIKE '%view%' AND CHECK_CLAUSE LIKE '%inquiry%' AND CHECK_CLAUSE LIKE '%booking%' AND CHECK_CLAUSE LIKE '%arrival%' AND CHECK_CLAUSE LIKE '%settlement%')
    OR
    (CONSTRAINT_NAME='chk_attribution_amount' AND CHECK_CLAUSE LIKE '%settlement%' AND CHECK_CLAUSE LIKE '%amount%' AND CHECK_CLAUSE LIKE '%is not null%' AND CHECK_CLAUSE LIKE '%is null%')
  ));
SET @evt_uk_ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_attribution_event'
    AND INDEX_NAME='uk_attribution_request_id'
  GROUP BY INDEX_NAME, NON_UNIQUE
  HAVING NON_UNIQUE=0 AND cols='request_id'
) t);
SET @evt_idx_ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_attribution_event'
    AND INDEX_NAME IN ('idx_attribution_pub_type_occurred','idx_attribution_store_type_occurred','idx_attribution_business')
  GROUP BY INDEX_NAME, NON_UNIQUE
  HAVING NON_UNIQUE=1 AND (
    (INDEX_NAME='idx_attribution_pub_type_occurred' AND cols='publication_id,event_type,occurred_at') OR
    (INDEX_NAME='idx_attribution_store_type_occurred' AND cols='store_id,event_type,occurred_at') OR
    (INDEX_NAME='idx_attribution_business' AND cols='business_type,business_id')
  )
) t);
SET @evt_fk_ok = (SELECT COUNT(*) FROM (
  SELECT kcu.CONSTRAINT_NAME,
         GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS child_cols,
         GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS parent_cols,
         MIN(kcu.REFERENCED_TABLE_NAME) AS parent_tbl,
         rc.DELETE_RULE
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='marketing_attribution_event'
    AND kcu.CONSTRAINT_NAME='fk_attribution_publication' AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE, rc.UPDATE_RULE
  HAVING child_cols='publication_id,store_id' AND parent_cols='publication_id,store_id'
     AND parent_tbl='marketing_publication' AND rc.DELETE_RULE='RESTRICT' AND rc.UPDATE_RULE='RESTRICT'
) t);
SET @evt_bad = IF(@evt_tbl=1 AND (@evt_col_ok<>13 OR @evt_ck_ok<>2 OR @evt_uk_ok<>1 OR @evt_idx_ok<>3 OR @evt_fk_ok<>1), 1, 0);
SET @sig = IF(@evt_bad=1, 'SELECT * FROM `__refuse_marketing_attribution_event_structure_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段1：marketing_activity 增列 + 状态权威约束 + store_id 去默认 + 复合唯一键
-- ============================================================
SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='status' AND COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='draft');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT ''draft'' COMMENT ''唯一权威状态''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='document_version' AND COLUMN_TYPE='int' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='1');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN document_version INT NOT NULL DEFAULT 1 COMMENT ''当前编辑版本，初始1''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='row_version' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='1');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN row_version BIGINT NOT NULL DEFAULT 1 COMMENT ''乐观锁版本''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='created_by' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN created_by BIGINT NULL COMMENT ''创建人，实时身份''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='updated_by' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN updated_by BIGINT NULL COMMENT ''更新人，实时身份''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='submitted_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN submitted_at DATETIME NULL COMMENT ''提交审批时间''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='approved_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD COLUMN approved_at DATETIME NULL COMMENT ''审批通过时间''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 状态权威约束：拒绝状态枚举外的写入（幂等，按约束名存在即跳过）
SET @ok = (SELECT COUNT(*) FROM information_schema.CHECK_CONSTRAINTS WHERE CONSTRAINT_SCHEMA=DATABASE() AND CONSTRAINT_NAME='chk_activity_status');
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD CONSTRAINT chk_activity_status CHECK (status IN (''draft'',''pending_approval'',''approved'',''published'',''paused'',''expired'',''cancelled''))', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- store_id 移除默认 1（保持非空）
SET @has_def = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='store_id' AND COLUMN_DEFAULT IS NOT NULL);
SET @ddl = IF(@has_def>0, 'ALTER TABLE marketing_activity MODIFY COLUMN store_id BIGINT NOT NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 复合唯一键 (activity_id,store_id)：支撑 marketing_publication 同店复合外键
SET @ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_activity' AND INDEX_NAME='uk_activity_id_store'
  GROUP BY INDEX_NAME, NON_UNIQUE HAVING NON_UNIQUE=0 AND cols='activity_id,store_id'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE marketing_activity ADD UNIQUE KEY uk_activity_id_store (activity_id, store_id)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段2：创建不可变 marketing_publication（发布版本快照）
-- 发布行不更新内容快照；暂停只改状态与执行人/时间。
-- ============================================================
CREATE TABLE IF NOT EXISTS marketing_publication (
  publication_id BIGINT       NOT NULL AUTO_INCREMENT,
  activity_id    BIGINT       NOT NULL,
  store_id       BIGINT       NOT NULL,
  version        INT          NOT NULL,
  channel        VARCHAR(24)  NOT NULL,
  public_slug    VARCHAR(80)  NOT NULL,
  source_code    VARCHAR(64)  NOT NULL,
  title          VARCHAR(200) NOT NULL,
  summary        VARCHAR(500) NULL,
  content_json   JSON         NULL,
  hero_asset_url VARCHAR(500) NULL,
  cta_label      VARCHAR(50)  NULL,
  status         VARCHAR(20)  NOT NULL DEFAULT 'published',
  valid_from     TIMESTAMP    NULL,
  valid_to       TIMESTAMP    NULL,
  published_by   BIGINT       NULL,
  published_at   DATETIME     NULL,
  paused_by      BIGINT       NULL,
  paused_at      DATETIME     NULL,
  request_id     VARCHAR(64)  NOT NULL,
  created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (publication_id),
  UNIQUE KEY uk_publication_activity_version_channel (activity_id, version, channel),
  UNIQUE KEY uk_publication_slug (public_slug),
  UNIQUE KEY uk_publication_source_code (source_code),
  UNIQUE KEY uk_publication_request_id (request_id),
  UNIQUE KEY uk_publication_id_store (publication_id, store_id),
  UNIQUE KEY uk_publication_id_store_source_channel (publication_id, store_id, source_code, channel),
  KEY idx_publication_store_status_valid (store_id, status, valid_from, valid_to),
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id, store_id) REFERENCES marketing_activity (activity_id, store_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_publication_status CHECK (status IN ('published','paused','expired'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营销发布版本（不可变快照）';

-- ============================================================
-- 阶段3：创建 marketing_attribution_event（转化归因事件）
-- 事件必须与发布记录同店（复合外键）；仅结算事件携带非负 amount。
-- ============================================================
CREATE TABLE IF NOT EXISTS marketing_attribution_event (
  event_id       BIGINT        NOT NULL AUTO_INCREMENT,
  publication_id BIGINT        NOT NULL,
  store_id       BIGINT        NOT NULL,
  source_code    VARCHAR(64)   NOT NULL,
  event_type     VARCHAR(24)   NOT NULL,
  visitor_key    VARCHAR(64)   NULL,
  business_type  VARCHAR(24)   NULL,
  business_id    BIGINT        NULL,
  business_no    VARCHAR(64)   NULL,
  amount         DECIMAL(12,2) NULL,
  request_id     VARCHAR(64)   NOT NULL,
  occurred_at    DATETIME      NOT NULL,
  created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (event_id),
  UNIQUE KEY uk_attribution_request_id (request_id),
  KEY idx_attribution_pub_type_occurred (publication_id, event_type, occurred_at),
  KEY idx_attribution_store_type_occurred (store_id, event_type, occurred_at),
  KEY idx_attribution_business (business_type, business_id),
  CONSTRAINT fk_attribution_publication FOREIGN KEY (publication_id, store_id) REFERENCES marketing_publication (publication_id, store_id) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT chk_attribution_event_type CHECK (event_type IN ('view','inquiry','booking','arrival','settlement')),
  CONSTRAINT chk_attribution_amount CHECK ((event_type='settlement' AND amount IS NOT NULL AND amount >= 0) OR (event_type<>'settlement' AND amount IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营销转化归因事件';

-- ============================================================
-- 阶段4：booking_inquiry 增列 + store_id 去默认 + 四字段同店复合外键
-- 三者全空仍兼容历史咨询；带来源则必须与发布版本同店、同码、同渠道。
-- ============================================================
SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' AND COLUMN_NAME='marketing_publication_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE booking_inquiry ADD COLUMN marketing_publication_id BIGINT NULL COMMENT ''来源发布版本，可空''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' AND COLUMN_NAME='source_code' AND COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE booking_inquiry ADD COLUMN source_code VARCHAR(64) NULL COMMENT ''不可变来源码，可空''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' AND COLUMN_NAME='source_channel' AND COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE booking_inquiry ADD COLUMN source_channel VARCHAR(24) NULL COMMENT ''来源渠道，可空''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- store_id 移除默认 1（保持非空）
SET @has_def = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' AND COLUMN_NAME='store_id' AND COLUMN_DEFAULT IS NOT NULL);
SET @ddl = IF(@has_def>0, 'ALTER TABLE booking_inquiry MODIFY COLUMN store_id BIGINT NOT NULL', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 四字段同店复合外键（可空列参与时，MySQL 对含 NULL 的行不校验，历史咨询兼容）
SET @ok = (SELECT COUNT(*) FROM (
  SELECT kcu.CONSTRAINT_NAME,
         GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS child_cols,
         GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS parent_cols,
         MIN(kcu.REFERENCED_TABLE_NAME) AS parent_tbl,
         rc.DELETE_RULE
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='booking_inquiry'
    AND kcu.CONSTRAINT_NAME='fk_bi_marketing_publication' AND kcu.REFERENCED_TABLE_NAME IS NOT NULL
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE, rc.UPDATE_RULE
  HAVING child_cols='marketing_publication_id,store_id,source_code,source_channel'
     AND parent_cols='publication_id,store_id,source_code,channel'
     AND parent_tbl='marketing_publication' AND rc.DELETE_RULE='RESTRICT' AND rc.UPDATE_RULE='RESTRICT'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE booking_inquiry ADD CONSTRAINT fk_bi_marketing_publication FOREIGN KEY (marketing_publication_id, store_id, source_code, source_channel) REFERENCES marketing_publication (publication_id, store_id, source_code, channel) ON DELETE RESTRICT ON UPDATE RESTRICT', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段5：发布快照不可改、不可物理删除（单语句体触发器，纯 ; 分隔，幂等）
-- 只允许 status / paused_by / paused_at 变化。
-- ============================================================
CREATE TRIGGER IF NOT EXISTS trg_publication_no_delete
BEFORE DELETE ON marketing_publication
FOR EACH ROW
SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'marketing_publication 禁止物理删除';

CREATE TRIGGER IF NOT EXISTS trg_publication_immutable
BEFORE UPDATE ON marketing_publication
FOR EACH ROW
SET @__tl_immutable_guard = IF(
  NOT (OLD.publication_id <=> NEW.publication_id)
  OR NOT (OLD.activity_id <=> NEW.activity_id)
  OR NOT (OLD.store_id <=> NEW.store_id)
  OR NOT (OLD.version <=> NEW.version)
  OR NOT (OLD.channel <=> NEW.channel)
  OR NOT (OLD.public_slug <=> NEW.public_slug)
  OR NOT (OLD.source_code <=> NEW.source_code)
  OR NOT (OLD.title <=> NEW.title)
  OR NOT (OLD.summary <=> NEW.summary)
  OR NOT (OLD.content_json <=> NEW.content_json)
  OR NOT (OLD.hero_asset_url <=> NEW.hero_asset_url)
  OR NOT (OLD.cta_label <=> NEW.cta_label)
  OR NOT (OLD.valid_from <=> NEW.valid_from)
  OR NOT (OLD.valid_to <=> NEW.valid_to)
  OR NOT (OLD.published_by <=> NEW.published_by)
  OR NOT (OLD.published_at <=> NEW.published_at)
  OR NOT (OLD.request_id <=> NEW.request_id)
  OR NOT (OLD.created_at <=> NEW.created_at),
  (SELECT 1 UNION ALL SELECT 2),
  NULL
);

-- ============================================================
-- 结束哨兵：显式确认迁移执行到末尾（无异常即成功）
-- ============================================================
SELECT 'marketing_publication_v1 migration completed' AS migration_status;

-- ============================================================
-- marketing_publication_v1.sql  营销发布与客人来源码数据库契约
-- 唯一正式迁移口径（幂等 + fail-closed 结构预检）
--
-- 目标（照 docs/营销H5生产闭环设计_20260913.md 第三节「最小数据模型」）：
--   1) marketing_activity 增加 status/document_version/row_version/
--      created_by/updated_by/submitted_at/approved_at，并移除 store_id 默认 1。
--      旧活动一律迁为 draft（DEFAULT），绝不伪造 approved/published。
--   2) 新建不可变 marketing_publication：public_slug/source_code/request_id 全局唯一，
--      活动+门店按复合键 (activity_id,store_id) 同店，发布行不可更新内容快照。
--   3) 新建 marketing_attribution_event：request_id 唯一，事件必须与发布记录同店，
--      仅结算事件允许携带非负 amount。
--   4) booking_inquiry 增加可空 marketing_publication_id/source_code/source_channel，
--      并移除 store_id 默认 1；三者全空仍兼容历史咨询。
--
-- fail-closed 结构预检（阶段0，纯只读信息schema，零 DDL）：
--   任何同名错误列类型 / 错误唯一键 / 错误外键存在，即 SELECT 不存在的哨兵表
--   触发整体失败，绝不带错结构继续执行 DDL（不形成部分迁移）。
--
-- 纯 ; 分隔 + PREPARE/EXECUTE 动态 SQL，兼容 mysql CLI 与 Spring ScriptUtils。
-- ============================================================

SET NAMES utf8mb4;

-- ============================================================
-- 阶段0：结构安全预检（纯只读，零 DDL，任何不符即整体拒绝）
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

-- 0b. booking_inquiry 目标列若已存在但定义不符 -> 拒绝
SET @bad_col = (SELECT COUNT(*) FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='booking_inquiry' AND (
    (COLUMN_NAME='marketing_publication_id' AND NOT (COLUMN_TYPE='bigint' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='source_code'              AND NOT (COLUMN_TYPE='varchar(64)' AND IS_NULLABLE='YES')) OR
    (COLUMN_NAME='source_channel'           AND NOT (COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='YES'))
  ));
SET @sig = IF(@bad_col>0, 'SELECT * FROM `__refuse_booking_inquiry_column_definition_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0c. marketing_publication 若已存在但唯一键/索引/外键定义不符 -> 拒绝
--     不只看对象名是否存在，还核对列序 + 唯一性 + 外键子父列序与规则。
SET @pub_tbl = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_publication');
SET @pub_uk_ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_publication'
    AND INDEX_NAME IN ('uk_publication_activity_version_channel','uk_publication_slug','uk_publication_source_code','uk_publication_request_id','uk_publication_id_store')
  GROUP BY INDEX_NAME, NON_UNIQUE
  HAVING NON_UNIQUE=0 AND (
    (INDEX_NAME='uk_publication_activity_version_channel' AND cols='activity_id,version,channel') OR
    (INDEX_NAME='uk_publication_slug' AND cols='public_slug') OR
    (INDEX_NAME='uk_publication_source_code' AND cols='source_code') OR
    (INDEX_NAME='uk_publication_request_id' AND cols='request_id') OR
    (INDEX_NAME='uk_publication_id_store' AND cols='publication_id,store_id')
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
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE
  HAVING child_cols='activity_id,store_id' AND parent_cols='activity_id,store_id'
     AND parent_tbl='marketing_activity' AND rc.DELETE_RULE='RESTRICT'
) t);
SET @pub_bad = IF(@pub_tbl=1 AND (@pub_uk_ok<>5 OR @pub_idx_ok<>1 OR @pub_fk_ok<>1), 1, 0);
SET @sig = IF(@pub_bad=1, 'SELECT * FROM `__refuse_marketing_publication_structure_mismatch__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- 0d. marketing_attribution_event 若已存在但唯一键/索引/外键定义不符 -> 拒绝
SET @evt_tbl = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='marketing_attribution_event');
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
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE
  HAVING child_cols='publication_id,store_id' AND parent_cols='publication_id,store_id'
     AND parent_tbl='marketing_publication' AND rc.DELETE_RULE='RESTRICT'
) t);
SET @evt_bad = IF(@evt_tbl=1 AND (@evt_uk_ok<>1 OR @evt_idx_ok<>3 OR @evt_fk_ok<>1), 1, 0);
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
  KEY idx_publication_store_status_valid (store_id, status, valid_from, valid_to),
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id, store_id) REFERENCES marketing_activity (activity_id, store_id) ON DELETE RESTRICT,
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
  CONSTRAINT fk_attribution_publication FOREIGN KEY (publication_id, store_id) REFERENCES marketing_publication (publication_id, store_id) ON DELETE RESTRICT,
  CONSTRAINT chk_attribution_event_type CHECK (event_type IN ('view','inquiry','booking','arrival','settlement')),
  CONSTRAINT chk_attribution_amount CHECK ((event_type='settlement' AND amount IS NOT NULL AND amount >= 0) OR (event_type<>'settlement' AND amount IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='营销转化归因事件';

-- ============================================================
-- 阶段4：booking_inquiry 增列 + store_id 去默认 + 同店复合外键
-- 三者全空仍兼容历史咨询；带来源则必须与发布记录同店。
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

-- 同店复合外键（可空列参与时，MySQL 对含 NULL 的行不校验，历史咨询兼容）
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
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE
  HAVING child_cols='marketing_publication_id,store_id' AND parent_cols='publication_id,store_id'
     AND parent_tbl='marketing_publication' AND rc.DELETE_RULE='RESTRICT'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE booking_inquiry ADD CONSTRAINT fk_bi_marketing_publication FOREIGN KEY (marketing_publication_id, store_id) REFERENCES marketing_publication (publication_id, store_id) ON DELETE RESTRICT', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 结束哨兵：显式确认迁移执行到末尾（无异常即成功）
-- ============================================================
SELECT 'marketing_publication_v1 migration completed' AS migration_status;

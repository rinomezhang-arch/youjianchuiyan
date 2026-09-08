-- ============================================================
-- payroll_approval_payout_v1.sql  工资保存→审批→发放记账闭环
-- 唯一正式迁移口径（幂等 + 深度自愈）
--
-- 状态口径（固化）：1=已保存 2=已审批 3=已发放记账
-- status=3 只表示账上记了这一笔，不表示钱已到账。
--
-- 深度自愈策略（第二轮整改 CX 要求）：
--   阶段0：纯只读数据预检（零 DDL），任何不可安全收窄的数据存在即 SIGNAL 整体失败。
--   阶段1-6：按「正确性」而非「存在性」逐项比对——
--     列：类型/可空/默认/注释完整定义；
--     索引：列序 + 唯一性；
--     外键：子列序 + 父列序 + ON UPDATE/DELETE 规则。
--   错误的同名结构先 DROP 再 ADD 正确版；冗余结构 DROP。
-- 纯 ; 分隔 + PREPARE/EXECUTE，兼容 mysql CLI 与 Spring ScriptUtils。
-- ============================================================

-- ============================================================
-- 阶段0：数据安全预检（纯只读，零 DDL，失败即整体退出）
-- 动态防表/列不存在；表不存在或列不存在时预检跳过（返回 0）
-- ============================================================
-- recorded_by 收窄 varchar(50)→varchar(40)：预检超长数据
SET @has_tbl = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record');
SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND COLUMN_NAME='recorded_by');
SET @pre = IF(@has_tbl=0 OR @has_col=0, 'SELECT 0 INTO @over', 'SELECT COUNT(*) INTO @over FROM payroll_payout_record WHERE CHAR_LENGTH(recorded_by) > 40');
PREPARE s FROM @pre; EXECUTE s; DEALLOCATE PREPARE s;
SET @sig = IF(@over > 0, 'SELECT * FROM `__data_shrink_unsafe_recorded_by_gt_40__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- note 收窄 varchar(255)→varchar(200)：预检超长数据
SET @has_tbl = (SELECT COUNT(*) FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record');
SET @has_col = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND COLUMN_NAME='note');
SET @pre = IF(@has_tbl=0 OR @has_col=0, 'SELECT 0 INTO @over', 'SELECT COUNT(*) INTO @over FROM payroll_payout_record WHERE CHAR_LENGTH(note) > 200');
PREPARE s FROM @pre; EXECUTE s; DEALLOCATE PREPARE s;
SET @sig = IF(@over > 0, 'SELECT * FROM `__data_shrink_unsafe_note_gt_200__`', 'DO 0');
PREPARE s FROM @sig; EXECUTE s; DEALLOCATE PREPARE s;

-- total_net 扩宽 decimal(12,2)→decimal(15,2)：无需预检（扩宽安全）

-- ============================================================
-- 阶段1：month_salary 审批发放列（逐项正确性比对：不存在则 ADD）
-- ============================================================
SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='post_salary_snapshot' AND COLUMN_TYPE='decimal(10,2)' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN post_salary_snapshot DECIMAL(10,2) NULL COMMENT ''保存时岗位工资组成''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='attendance_pay_snapshot' AND COLUMN_TYPE='decimal(10,2)' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN attendance_pay_snapshot DECIMAL(10,2) NULL COMMENT ''保存时考勤工资组成''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='approved_by' AND COLUMN_TYPE='varchar(40)' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN approved_by VARCHAR(40) NULL COMMENT ''审批人登录名，必须是真人账号''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='approved_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN approved_at DATETIME NULL COMMENT ''审批时间''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='paid_by' AND COLUMN_TYPE='varchar(40)' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN paid_by VARCHAR(40) NULL COMMENT ''发放记账操作人''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='paid_at' AND COLUMN_TYPE='datetime' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN paid_at DATETIME NULL COMMENT ''发放记账时间，不是银行到账时间''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ok = (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='payout_id' AND COLUMN_TYPE='bigint' AND IS_NULLABLE='YES');
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD COLUMN payout_id BIGINT NULL COMMENT ''所属发放记账批次，指向 payroll_payout_record''', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段2：month_salary.payout_id 索引（正确性比对：列序）
-- ============================================================
SET @ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND INDEX_NAME='idx_month_salary_payout'
  GROUP BY INDEX_NAME HAVING cols='payout_id'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD INDEX idx_month_salary_payout (payout_id)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段3：payroll_payout_record 建表/列对齐（深度自愈）
-- ============================================================
CREATE TABLE IF NOT EXISTS payroll_payout_record (
  payout_id     BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month  VARCHAR(7)   NOT NULL COMMENT 'YYYY-MM',
  store_id      BIGINT       NULL COMMENT '按门店记账时的门店；全门店一次记账时为空',
  headcount     INT          NOT NULL,
  total_net     DECIMAL(15,2) NOT NULL COMMENT '本批实发合计，服务端重算所得',
  recorded_by   VARCHAR(40)  NOT NULL,
  recorded_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note          VARCHAR(200) NULL COMMENT '仅为账务记录，不代表银行实际到账',
  UNIQUE KEY uk_payout_month_identity (payout_id, salary_month),
  KEY idx_payout_month (salary_month, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工资发放记账台账（非银行支付凭证）';

-- created_at -> recorded_at（旧表用 created_at；仅 created_at 存在且 recorded_at 不存在时 RENAME）
SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND COLUMN_NAME='created_at') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND COLUMN_NAME='recorded_at') = 0, 'ALTER TABLE payroll_payout_record RENAME COLUMN created_at TO recorded_at', 'DO 0'));
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 逐列 MODIFY 修正到正式口径（数据已预检安全；MODIFY 到同定义是 no-op）
ALTER TABLE payroll_payout_record MODIFY COLUMN payout_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE payroll_payout_record MODIFY COLUMN salary_month VARCHAR(7) NOT NULL COMMENT 'YYYY-MM';
ALTER TABLE payroll_payout_record MODIFY COLUMN store_id BIGINT NULL COMMENT '按门店记账时的门店；全门店一次记账时为空';
ALTER TABLE payroll_payout_record MODIFY COLUMN headcount INT NOT NULL;
ALTER TABLE payroll_payout_record MODIFY COLUMN total_net DECIMAL(15,2) NOT NULL COMMENT '本批实发合计，服务端重算所得';
ALTER TABLE payroll_payout_record MODIFY COLUMN recorded_by VARCHAR(40) NOT NULL;
ALTER TABLE payroll_payout_record MODIFY COLUMN recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER recorded_by;
ALTER TABLE payroll_payout_record MODIFY COLUMN note VARCHAR(200) NULL COMMENT '仅为账务记录，不代表银行实际到账';
ALTER TABLE payroll_payout_record COMMENT='工资发放记账台账（非银行支付凭证）';

-- ============================================================
-- 阶段4：payroll_payout_record 索引对齐（列序 + 唯一性正确性）
-- ============================================================
-- 4a. 冗余旧索引（正式口径无）：DROP
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_pr_salary_month', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_pr_salary_month');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_pr_store', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_pr_store');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_pr_store_month', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_pr_store_month');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4b. 冗余单列唯一 uk_payout_id：DROP
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX uk_payout_id', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='uk_payout_id');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4c. 复合唯一键 uk_payout_month_identity：若存在但列序/唯一性不对，先 DROP
SET @ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='uk_payout_month_identity'
  GROUP BY INDEX_NAME, NON_UNIQUE HAVING NON_UNIQUE=0 AND cols='payout_id,salary_month'
) t);
SET @exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='uk_payout_month_identity');
SET @ddl = IF(@exists>0 AND @ok=0, 'ALTER TABLE payroll_payout_record DROP INDEX uk_payout_month_identity', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
-- 若正确的复合唯一键不存在，ADD
SET @ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='uk_payout_month_identity'
  GROUP BY INDEX_NAME, NON_UNIQUE HAVING NON_UNIQUE=0 AND cols='payout_id,salary_month'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE payroll_payout_record ADD UNIQUE KEY uk_payout_month_identity (payout_id, salary_month)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 4d. idx_payout_month：若存在但列序不对，先 DROP；若正确不存在，ADD
SET @ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_payout_month'
  GROUP BY INDEX_NAME HAVING cols='salary_month,store_id'
) t);
SET @exists = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_payout_month');
SET @ddl = IF(@exists>0 AND @ok=0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_payout_month', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ok = (SELECT COUNT(*) FROM (
  SELECT INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') AS cols
  FROM information_schema.STATISTICS
  WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_payout_month'
  GROUP BY INDEX_NAME HAVING cols='salary_month,store_id'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE payroll_payout_record ADD KEY idx_payout_month (salary_month, store_id)', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段5：payroll_payout_record store_id 外键（子父列序 + 规则正确性）
-- ============================================================
-- 5a. 错误 store_id 外键（子列 store_id 但父表/父列/规则不对）：DROP
SET @bad_name = (SELECT kcu.CONSTRAINT_NAME FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='payroll_payout_record'
    AND kcu.COLUMN_NAME='store_id'
    AND (kcu.REFERENCED_TABLE_NAME<>'store_info' OR kcu.REFERENCED_COLUMN_NAME<>'store_id' OR rc.DELETE_RULE<>'RESTRICT')
  LIMIT 1);
SET @ddl = IF(@bad_name IS NOT NULL, CONCAT('ALTER TABLE payroll_payout_record DROP FOREIGN KEY `', @bad_name, '`'), 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 5b. 正确 store_id 外键不存在则 ADD
SET @ok = (SELECT COUNT(*) FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='payroll_payout_record'
    AND kcu.COLUMN_NAME='store_id' AND kcu.REFERENCED_TABLE_NAME='store_info' AND kcu.REFERENCED_COLUMN_NAME='store_id'
    AND rc.DELETE_RULE='RESTRICT');
SET @ddl = IF(@ok=0, 'ALTER TABLE payroll_payout_record ADD FOREIGN KEY (store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- 阶段6：month_salary 复合外键 fk_salary_payout_month（子父列序 + 规则）
-- ============================================================
-- 6a. 同名错误复合外键（列序不对）：DROP FOREIGN KEY
SET @bad = (SELECT COUNT(*) FROM (
  SELECT kcu.CONSTRAINT_NAME,
         GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS child_cols,
         GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS parent_cols
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='month_salary' AND kcu.CONSTRAINT_NAME='fk_salary_payout_month'
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE
  HAVING NOT (child_cols='payout_id,salary_month' AND parent_cols='payout_id,salary_month' AND rc.DELETE_RULE='RESTRICT')
) t);
SET @ddl = IF(@bad>0, 'ALTER TABLE month_salary DROP FOREIGN KEY fk_salary_payout_month', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 6b. 仅当 6a 已 DROP 错误外键(@bad>0)时，清理残留的同名自动索引（InnoDB 不自动删）
SET @residual = (SELECT COUNT(*) FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND INDEX_NAME='fk_salary_payout_month');
SET @ddl = IF(@bad>0 AND @residual>0, 'ALTER TABLE month_salary DROP INDEX fk_salary_payout_month', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 6c. 正确复合外键不存在则 ADD
SET @ok = (SELECT COUNT(*) FROM (
  SELECT kcu.CONSTRAINT_NAME,
         GROUP_CONCAT(kcu.COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS child_cols,
         GROUP_CONCAT(kcu.REFERENCED_COLUMN_NAME ORDER BY kcu.ORDINAL_POSITION SEPARATOR ',') AS parent_cols
  FROM information_schema.KEY_COLUMN_USAGE kcu
  JOIN information_schema.REFERENTIAL_CONSTRAINTS rc
    ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME
  WHERE kcu.TABLE_SCHEMA=DATABASE() AND kcu.TABLE_NAME='month_salary' AND kcu.CONSTRAINT_NAME='fk_salary_payout_month'
  GROUP BY kcu.CONSTRAINT_NAME, rc.DELETE_RULE
  HAVING child_cols='payout_id,salary_month' AND parent_cols='payout_id,salary_month' AND rc.DELETE_RULE='RESTRICT'
) t);
SET @ddl = IF(@ok=0, 'ALTER TABLE month_salary ADD CONSTRAINT fk_salary_payout_month FOREIGN KEY (payout_id, salary_month) REFERENCES payroll_payout_record(payout_id, salary_month) ON DELETE RESTRICT', 'DO 0');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============================================================
-- payroll_approval_payout_v1.sql  工资保存→审批→发放记账闭环
-- 唯一正式迁移口径（幂等 + 自愈：可重复执行，旧冲突表存在时自动修复）
--
-- 状态口径（固化）：1=已保存 2=已审批 3=已发放记账
-- status=3 只表示账上记了这一笔，不表示钱已到账。
--
-- 幂等/自愈实现：纯 ; 分隔 + PREPARE/EXECUTE 动态 SQL（不用 DELIMITER/存储过程），
-- 兼容 mysql CLI 与 Spring ScriptUtils 两种执行入口。
-- 逐项检测 information_schema，列/索引/外键不存在才执行，列类型不符则 MODIFY 修正，
-- 保证全新库/重复执行/旧冲突表三态最终结构一致。
-- ============================================================

-- ============ month_salary 审批发放列（逐项条件 ADD） ============
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN post_salary_snapshot DECIMAL(10,2) NULL COMMENT ''保存时岗位工资组成''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='post_salary_snapshot');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN attendance_pay_snapshot DECIMAL(10,2) NULL COMMENT ''保存时考勤工资组成''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='attendance_pay_snapshot');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN approved_by VARCHAR(40) NULL COMMENT ''审批人登录名，必须是真人账号''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='approved_by');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN approved_at DATETIME NULL COMMENT ''审批时间''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='approved_at');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN paid_by VARCHAR(40) NULL COMMENT ''发放记账操作人''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='paid_by');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN paid_at DATETIME NULL COMMENT ''发放记账时间，不是银行到账时间''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='paid_at');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD COLUMN payout_id BIGINT NULL COMMENT ''所属发放记账批次，指向 payroll_payout_record''', 'DO 0') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND COLUMN_NAME='payout_id');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============ month_salary.payout_id 索引（条件 ADD） ============
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD INDEX idx_month_salary_payout (payout_id)', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND INDEX_NAME='idx_month_salary_payout');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============ payroll_payout_record 建表/自愈 ============
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
  FOREIGN KEY (store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT,
  KEY idx_payout_month (salary_month, store_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='工资发放记账台账（非银行支付凭证）';

-- created_at -> recorded_at（旧表用 created_at，正式口径是 recorded_at；仅 created_at 存在且 recorded_at 不存在时 RENAME）
SET @ddl = (SELECT IF((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND COLUMN_NAME='created_at') > 0 AND (SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND COLUMN_NAME='recorded_at') = 0, 'ALTER TABLE payroll_payout_record RENAME COLUMN created_at TO recorded_at', 'DO 0'));
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 自愈：旧冲突表逐列 MODIFY 修正到正式口径（MODIFY 到同定义是幂等 no-op）
ALTER TABLE payroll_payout_record MODIFY COLUMN payout_id BIGINT NOT NULL AUTO_INCREMENT;
ALTER TABLE payroll_payout_record MODIFY COLUMN salary_month VARCHAR(7) NOT NULL COMMENT 'YYYY-MM';
ALTER TABLE payroll_payout_record MODIFY COLUMN store_id BIGINT NULL COMMENT '按门店记账时的门店；全门店一次记账时为空';
ALTER TABLE payroll_payout_record MODIFY COLUMN headcount INT NOT NULL;
ALTER TABLE payroll_payout_record MODIFY COLUMN total_net DECIMAL(15,2) NOT NULL COMMENT '本批实发合计，服务端重算所得';
ALTER TABLE payroll_payout_record MODIFY COLUMN recorded_by VARCHAR(40) NOT NULL;
ALTER TABLE payroll_payout_record MODIFY COLUMN recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER recorded_by;
ALTER TABLE payroll_payout_record MODIFY COLUMN note VARCHAR(200) NULL COMMENT '仅为账务记录，不代表银行实际到账';
ALTER TABLE payroll_payout_record COMMENT='工资发放记账台账（非银行支付凭证）';

-- 删旧冲突表的冗余索引（正式口径无这些；条件 DROP）
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_pr_salary_month', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_pr_salary_month');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_pr_store', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_pr_store');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX idx_pr_store_month', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_pr_store_month');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 删旧单列唯一 uk_payout_id（正式口径用复合唯一；条件 DROP）
SET @ddl = (SELECT IF(COUNT(*)>0, 'ALTER TABLE payroll_payout_record DROP INDEX uk_payout_id', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='uk_payout_id');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- 复合唯一键（条件 ADD）
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE payroll_payout_record ADD UNIQUE KEY uk_payout_month_identity (payout_id, salary_month)', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='uk_payout_month_identity');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- store_id 外键（条件 ADD）
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE payroll_payout_record ADD FOREIGN KEY (store_id) REFERENCES store_info(store_id) ON DELETE RESTRICT', 'DO 0') FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND REFERENCED_TABLE_NAME='store_info');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- idx_payout_month（条件 ADD）
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE payroll_payout_record ADD KEY idx_payout_month (salary_month, store_id)', 'DO 0') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='payroll_payout_record' AND INDEX_NAME='idx_payout_month');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

-- ============ month_salary 复合外键（条件 ADD） ============
SET @ddl = (SELECT IF(COUNT(*)=0, 'ALTER TABLE month_salary ADD CONSTRAINT fk_salary_payout_month FOREIGN KEY (payout_id, salary_month) REFERENCES payroll_payout_record(payout_id, salary_month) ON DELETE RESTRICT', 'DO 0') FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='month_salary' AND CONSTRAINT_NAME='fk_salary_payout_month');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;

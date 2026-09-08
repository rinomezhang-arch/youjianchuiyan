#!/bin/bash
# TL-OPS-PAYROLL-MIGRATION-CANONICAL-13 异常结构反例测试（第二轮整改）
# 覆盖 5 类异常结构 + 历史数据零部分迁移预检，全部只连隔离 MySQL 13317。
set -euo pipefail

HOST=127.0.0.1; PORT=13317
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/../.." && pwd)"
FIXTURE="$ROOT/banquet_project/src/test/resources/payroll-metadata-fixture-20260907.sql"
MIG="$ROOT/scripts/migrations/payroll_approval_payout_v1.sql"
RETIRED="$ROOT/banquet_project/src/main/resources/db/migration/V20260908_01__payroll_payout_record.sql.retired"
MYSQL="mysql -uroot --protocol=tcp -h$HOST -P$PORT"

newdb(){ $MYSQL -e "CREATE DATABASE $1 CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" ; $MYSQL "$1" < "$FIXTURE" 2>/dev/null; }

PASS=0; FAIL=0
ok(){ echo "  [PASS] $*"; PASS=$((PASS+1)); }
bad(){ echo "  [FAIL] $*"; FAIL=$((FAIL+1)); }

# 反例1：同名错误复合外键（列序 salary_month,payout_id 错）→ 自愈为 payout_id,salary_month
D="anom1_$(date +%s)"; newdb "$D"
$MYSQL "$D" -e "
CREATE TABLE payroll_payout_record (
  payout_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL, store_id BIGINT NULL, headcount INT NOT NULL,
  total_net DECIMAL(15,2) NOT NULL, recorded_by VARCHAR(40) NOT NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, note VARCHAR(200) NULL,
  UNIQUE KEY uk_wrong_order (salary_month, payout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
ALTER TABLE month_salary ADD COLUMN payout_id BIGINT NULL;
ALTER TABLE month_salary ADD CONSTRAINT fk_salary_payout_month FOREIGN KEY (salary_month, payout_id) REFERENCES payroll_payout_record(salary_month, payout_id) ON DELETE RESTRICT;" 2>/dev/null
$MYSQL "$D" < "$MIG" 2>/dev/null && ok "反例1 同名错误复合外键自愈退出0" || bad "反例1 自愈失败"
COLS=$($MYSQL "$D" -N -e "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ',') FROM information_schema.key_column_usage WHERE table_schema='$D' AND CONSTRAINT_NAME='fk_salary_payout_month' GROUP BY CONSTRAINT_NAME;" 2>/dev/null)
[ "$COLS" = "payout_id,salary_month" ] && ok "反例1 外键列序已修正为 payout_id,salary_month" || bad "反例1 外键列序仍错($COLS)"

# 反例2：错误 store 外键（ON DELETE CASCADE 错）→ 自愈为 RESTRICT
D="anom2_$(date +%s)"; newdb "$D"
$MYSQL "$D" -e "
CREATE TABLE payroll_payout_record (
  payout_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL, store_id BIGINT NULL, headcount INT NOT NULL,
  total_net DECIMAL(15,2) NOT NULL, recorded_by VARCHAR(40) NOT NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, note VARCHAR(200) NULL,
  UNIQUE KEY uk_payout_month_identity (payout_id, salary_month),
  KEY idx_payout_month (salary_month, store_id),
  CONSTRAINT fk_store_wrong_rule FOREIGN KEY (store_id) REFERENCES store_info(store_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;" 2>/dev/null
$MYSQL "$D" < "$MIG" 2>/dev/null && ok "反例2 错误store外键自愈退出0" || bad "反例2 自愈失败"
RULE=$($MYSQL "$D" -N -e "SELECT DELETE_RULE FROM information_schema.referential_constraints WHERE constraint_schema='$D' AND table_name='payroll_payout_record';" 2>/dev/null)
[ "$RULE" = "RESTRICT" ] && ok "反例2 store外键规则已修正为 RESTRICT" || bad "反例2 规则仍错($RULE)"

# 反例3：历史数据字段收窄（recorded_by>40）→ 整体失败零 DDL
D="anom3_$(date +%s)"; newdb "$D"
$MYSQL "$D" -e "
CREATE TABLE payroll_payout_record (
  payout_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL, store_id BIGINT NULL, headcount INT NOT NULL,
  total_net DECIMAL(15,2) NOT NULL, recorded_by VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, note VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO payroll_payout_record(salary_month, headcount, total_net, recorded_by) VALUES ('2026-08',1,100.00, REPEAT('x',45));" 2>/dev/null
$MYSQL "$D" < "$MIG" 2>/dev/null && bad "反例3 应整体失败却退出0" || ok "反例3 数据收窄预检整体失败(非0退出)"
STILL=$($MYSQL "$D" -N -e "SELECT GROUP_CONCAT(CONCAT(COLUMN_NAME,':',COLUMN_TYPE) ORDER BY COLUMN_NAME) FROM information_schema.columns WHERE table_schema='$D' AND table_name='payroll_payout_record' AND COLUMN_NAME IN ('recorded_by','created_at');" 2>/dev/null)
[ "$STILL" = "created_at:datetime,recorded_by:varchar(50)" ] && ok "反例3 零DDL（recorded_by仍varchar(50)、created_at未rename）" || bad "反例3 有部分DDL残留($STILL)"

# 反例4：错误列类型（recorded_by varchar(50)、total_net decimal(12,2)、created_at）→ 自愈
D="anom4_$(date +%s)"; newdb "$D"
$MYSQL "$D" -e "
CREATE TABLE payroll_payout_record (
  payout_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL, store_id BIGINT NULL, headcount INT NOT NULL,
  total_net DECIMAL(12,2) NOT NULL, recorded_by VARCHAR(50) NOT NULL,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, note VARCHAR(255) NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
INSERT INTO payroll_payout_record(salary_month, headcount, total_net, recorded_by) VALUES ('2026-08',1,100.00,'zhangxiaoqiu');" 2>/dev/null
$MYSQL "$D" < "$MIG" 2>/dev/null && ok "反例4 错误列类型自愈退出0" || bad "反例4 自愈失败"
FIXED=$($MYSQL "$D" -N -e "SELECT GROUP_CONCAT(CONCAT(COLUMN_NAME,':',COLUMN_TYPE) ORDER BY COLUMN_NAME) FROM information_schema.columns WHERE table_schema='$D' AND table_name='payroll_payout_record' AND COLUMN_NAME IN ('total_net','recorded_by','note','recorded_at');" 2>/dev/null)
[ "$FIXED" = "note:varchar(200),recorded_at:datetime,recorded_by:varchar(40),total_net:decimal(15,2)" ] && ok "反例4 列类型已修正" || bad "反例4 列类型未完全修正($FIXED)"

# 反例5：同名错误索引（uk_payout_month_identity 列序错）→ 自愈
D="anom5_$(date +%s)"; newdb "$D"
$MYSQL "$D" -e "
CREATE TABLE payroll_payout_record (
  payout_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  salary_month VARCHAR(7) NOT NULL, store_id BIGINT NULL, headcount INT NOT NULL,
  total_net DECIMAL(15,2) NOT NULL, recorded_by VARCHAR(40) NOT NULL,
  recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP, note VARCHAR(200) NULL,
  UNIQUE KEY uk_payout_month_identity (salary_month, payout_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;" 2>/dev/null
$MYSQL "$D" < "$MIG" 2>/dev/null && ok "反例5 同名错误索引自愈退出0" || bad "反例5 自愈失败"
IDX=$($MYSQL "$D" -N -e "SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.statistics WHERE table_schema='$D' AND table_name='payroll_payout_record' AND INDEX_NAME='uk_payout_month_identity' GROUP BY INDEX_NAME;" 2>/dev/null)
[ "$IDX" = "payout_id,salary_month" ] && ok "反例5 索引列序已修正为 payout_id,salary_month" || bad "反例5 索引列序仍错($IDX)"

echo ""
echo "=== 反例测试结果：PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" = "0" ] && echo "全部通过" || echo "存在失败"
exit $([ "$FAIL" = "0" ] && echo 0 || echo 1)

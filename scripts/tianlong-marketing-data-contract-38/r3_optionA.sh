#!/bin/bash
# ============================================================
# R3 选项A实现 定向证据（不重跑 43 项整套）
#   1) 全文件 // 分隔迁移：mysql --delimiter=// 首次+重放；ScriptUtils separator=// 首次
#   2) 受控更新拒绝：负例(快照字段) 45000 自定义文案 + 正例(status/paused) 通过；两种执行器各验一次
#   3) 同名错误触发器前置失败 + 零部分迁移
#   4) 同名外键 ON UPDATE CASCADE 前置失败 + 零部分迁移
# 只连 127.0.0.1:13317；只建 TL38 前缀 schema；不 DROP/DELETE/TRUNCATE。
# ============================================================
set -u
HOST="127.0.0.1"; PORT="13317"
MYSQL="mysql -uroot --protocol=tcp -h${HOST} -P${PORT} --default-character-set=utf8mb4"
MIGRATION="scripts/migrations/marketing_publication_v1.sql"
BASE="scripts/tianlong-marketing-data-contract-38/base_schema.sql"
EVID="scripts/tianlong-marketing-data-contract-38"
TS=$(date +%Y%m%d_%H%M%S)
S_M="tl38_${TS}_r3_main"
S_SU="tl38_${TS}_r3_scriptutils"
S_TRG="tl38_${TS}_r3_badtrg"
S_UPD="tl38_${TS}_r3_badupdrule"
OUT="${EVID}/r3_optionA.txt"
: > "$OUT"
say(){ echo "$@" | tee -a "$OUT"; }
PASS=0; FAIL=0
chk(){ local id="$1" ok="$2" ev="$3"; if [ "$ok" = "PASS" ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi; say "[$id] $ok | $ev"; }

dump_struct() {
  local s="$1"
  $MYSQL -N "$s" -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${s}' ORDER BY TABLE_NAME;" 2>&1
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'<NULL>') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${s}' ORDER BY TABLE_NAME, ORDINAL_POSITION;" 2>&1
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='${s}' GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE ORDER BY TABLE_NAME, INDEX_NAME;" 2>&1
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, CONSTRAINT_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ','), REFERENCED_TABLE_NAME, GROUP_CONCAT(REFERENCED_COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ','), UPDATE_RULE, DELETE_RULE FROM information_schema.KEY_COLUMN_USAGE kcu JOIN information_schema.REFERENTIAL_CONSTRAINTS rc ON kcu.CONSTRAINT_SCHEMA=rc.CONSTRAINT_SCHEMA AND kcu.CONSTRAINT_NAME=rc.CONSTRAINT_NAME WHERE kcu.TABLE_SCHEMA='${s}' AND kcu.REFERENCED_TABLE_NAME IS NOT NULL GROUP BY TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME, UPDATE_RULE, DELETE_RULE ORDER BY TABLE_NAME, CONSTRAINT_NAME;" 2>&1
  $MYSQL -N "$s" -e "SELECT TRIGGER_NAME, EVENT_MANIPULATION, ACTION_TIMING FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='${s}' ORDER BY TRIGGER_NAME;" 2>&1
}

say "=== R3 选项A实现 定向证据 $(date '+%F %T') ==="
CP="$(cat ${EVID}/cp.txt 2>/dev/null)"

# ============================================================
# 1) 全文件 // 分隔迁移：mysql --delimiter=// 首次 + 重放
# ============================================================
say ""
say "== 1. 迁移 mysql --delimiter=// 首次+重放 =="
$MYSQL -e "CREATE DATABASE \`${S_M}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_M" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_M" -e "INSERT INTO store_info VALUES (1,'NG','宁国店'),(2,'XC','宣城店'); INSERT INTO marketing_activity (activity_id,store_id,activity_code,activity_name,activity_type) VALUES (1,1,'A1','X1','p'),(2,2,'A2','X2','p'); INSERT INTO booking_inquiry (id,store_id,customer_name,customer_phone,status) VALUES (1,1,'张三','13800000001','pending');" >/dev/null 2>&1
$MYSQL --delimiter=// "$S_M" < "$MIGRATION" > "${EVID}/r3_mig1.txt" 2>&1
MIG1=$?
$MYSQL --delimiter=// "$S_M" < "$MIGRATION" > "${EVID}/r3_mig2.txt" 2>&1
MIG2=$?
say "  首次迁移退出码=$MIG1 重放退出码=$MIG2"
if [ "$MIG1" -eq 0 ]; then chk "M1-mig-first" "PASS" "mysql --delimiter=// 首次迁移退出0"; else chk "M1-mig-first" "FAIL" "首次退出 $MIG1: $(cat ${EVID}/r3_mig1.txt)"; fi
if [ "$MIG2" -eq 0 ]; then chk "M2-mig-replay" "PASS" "mysql --delimiter=// 重放退出0"; else chk "M2-mig-replay" "FAIL" "重放退出 $MIG2: $(cat ${EVID}/r3_mig2.txt)"; fi

# ============================================================
# 2) ScriptUtils separator=// 首次迁移
# ============================================================
say ""
say "== 2. ScriptUtils separator=// 首次迁移 =="
MIGABS="/home/ubuntu/.openclaw/workspace/verify_marketing_38/scripts/migrations/marketing_publication_v1.sql"
$MYSQL -e "CREATE DATABASE \`${S_SU}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_SU" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_SU" -e "INSERT INTO store_info VALUES (1,'NG','宁国店'),(2,'XC','宣城店'); INSERT INTO marketing_activity (activity_id,store_id,activity_code,activity_name,activity_type) VALUES (1,1,'A1','X1','p'),(2,2,'A2','X2','p'); INSERT INTO booking_inquiry (id,store_id,customer_name,customer_phone,status) VALUES (1,1,'张三','13800000001','pending');" >/dev/null 2>&1
SU_MIG=$(java -cp "$CP:${EVID}" ScriptUtilsHarness "jdbc:mysql://127.0.0.1:13317/${S_SU}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8" root "" "$MIGABS" "//" 2>&1)
say "  ScriptUtils separator=// 跑迁移: $(printf '%s' "$SU_MIG" | grep -oE 'SCRIPT_(OK|FAIL)' || echo "$SU_MIG")"
if printf '%s' "$SU_MIG" | grep -q 'SCRIPT_OK'; then chk "M3-scriptutils-mig" "PASS" "ScriptUtils separator=// 可执行迁移"; else chk "M3-scriptutils-mig" "FAIL" "$SU_MIG"; fi

# ============================================================
# 3) 受控更新拒绝：负例 + 正例（两种执行器）
# ============================================================
say ""
say "== 3. 受控更新拒绝 负例(快照字段)/正例(status) =="
$MYSQL "$S_M" -e "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (1,1,1,'h5','slug-a','src-a','活动A','req-pub-a','published');" >/dev/null 2>&1
# mysql CLI 负例
NEG_CLI=$($MYSQL "$S_M" -e "UPDATE marketing_publication SET title='改标题' WHERE publication_id=1;" 2>&1)
say "  [mysql] 负例 title 更新: $(printf '%s' "$NEG_CLI" | grep -oE 'ERROR.*')"
if printf '%s' "$NEG_CLI" | grep -q '1644.*45000' || printf '%s' "$NEG_CLI" | grep -q '45000'; then
  if printf '%s' "$NEG_CLI" | grep -q '快照字段不可修改'; then chk "N1-neg-cli" "PASS" "mysql CLI 负例被拒 45000 + 自定义文案"; else chk "N1-neg-cli" "FAIL" "45000 但无自定义文案: $NEG_CLI"; fi
else chk "N1-neg-cli" "FAIL" "未按预期拒绝: $NEG_CLI"; fi
# mysql CLI 正例
POS_CLI=$($MYSQL "$S_M" -e "UPDATE marketing_publication SET status='paused', paused_by=1, paused_at=NOW() WHERE publication_id=1;" 2>&1)
POS_CLI_RC=$?
say "  [mysql] 正例 status+paused 更新退出码=$POS_CLI_RC"
if [ "$POS_CLI_RC" -eq 0 ]; then chk "N2-pos-cli" "PASS" "mysql CLI 正例 status/paused 更新通过"; else chk "N2-pos-cli" "FAIL" "正例失败: $POS_CLI"; fi
# 恢复 status 供 ScriptUtils 正例使用
$MYSQL "$S_M" -e "UPDATE marketing_publication SET status='published', paused_by=NULL, paused_at=NULL WHERE publication_id=1;" >/dev/null 2>&1

# ScriptUtils 负例（用 harness 跑 UPDATE 脚本）
printf "UPDATE marketing_publication SET title='改标题' WHERE publication_id=1;\n" > /tmp/r3_neg.sql
NEG_SU=$(java -cp "$CP:${EVID}" ScriptUtilsHarness "jdbc:mysql://127.0.0.1:13317/${S_M}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8" root "" /tmp/r3_neg.sql ";" 2>&1)
say "  [ScriptUtils] 负例 title 更新: $(printf '%s' "$NEG_SU" | grep -oE 'SCRIPT_(OK|FAIL)|快照字段不可修改|1644|45000' | tr '\n' ' ')"
if printf '%s' "$NEG_SU" | grep -q 'SCRIPT_FAIL' && printf '%s' "$NEG_SU" | grep -q '快照字段不可修改'; then chk "N3-neg-scriptutils" "PASS" "ScriptUtils 负例被拒且见自定义文案"; else chk "N3-neg-scriptutils" "FAIL" "$(printf '%s' "$NEG_SU" | head -3 | tr '\n' ' ')"; fi
# ScriptUtils 正例
printf "UPDATE marketing_publication SET status='paused', paused_by=1, paused_at=NOW() WHERE publication_id=1;\n" > /tmp/r3_pos.sql
POS_SU=$(java -cp "$CP:${EVID}" ScriptUtilsHarness "jdbc:mysql://127.0.0.1:13317/${S_M}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8" root "" /tmp/r3_pos.sql ";" 2>&1)
say "  [ScriptUtils] 正例 status 更新: $(printf '%s' "$POS_SU" | grep -oE 'SCRIPT_(OK|FAIL)')"
if printf '%s' "$POS_SU" | grep -q 'SCRIPT_OK'; then chk "N4-pos-scriptutils" "PASS" "ScriptUtils 正例 status 更新通过"; else chk "N4-pos-scriptutils" "FAIL" "$(printf '%s' "$POS_SU" | head -3 | tr '\n' ' ')"; fi

# ============================================================
# 4) 同名错误触发器前置失败 + 零部分迁移
# ============================================================
say ""
say "== 4. 同名错误触发器前置失败 =="
$MYSQL -e "CREATE DATABASE \`${S_TRG}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_TRG" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_TRG" <<'SQL' >/dev/null 2>&1
CREATE TABLE marketing_publication (publication_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TRIGGER trg_publication_no_delete AFTER INSERT ON marketing_publication FOR EACH ROW SET @x = 1;
SQL
dump_struct "$S_TRG" > "${EVID}/r3_badtrg_before.txt" 2>&1
$MYSQL --delimiter=// "$S_TRG" < "$MIGRATION" > "${EVID}/r3_badtrg_mig.txt" 2>&1
TRG_RC=$?
dump_struct "$S_TRG" > "${EVID}/r3_badtrg_after.txt" 2>&1
TRG_SENT=$(grep -oE '__refuse_[a-z_]+__' "${EVID}/r3_badtrg_mig.txt" | head -1)
if [ "$TRG_RC" -ne 0 ] && [ "$TRG_SENT" = "__refuse_marketing_publication_trigger_mismatch__" ]; then chk "T1-badtrg-refuse" "PASS" "错误触发器前置拒绝 rc=$TRG_RC sentinel=$TRG_SENT"; else chk "T1-badtrg-refuse" "FAIL" "rc=$TRG_RC sentinel=$TRG_SENT"; fi
if diff -q "${EVID}/r3_badtrg_before.txt" "${EVID}/r3_badtrg_after.txt" >/dev/null 2>&1; then chk "T2-badtrg-zeropartial" "PASS" "结构前后一致（零部分迁移）"; else chk "T2-badtrg-zeropartial" "FAIL" "结构变化"; fi

# ============================================================
# 5) 同名外键 ON UPDATE CASCADE 前置失败 + 零部分迁移
# ============================================================
say ""
say "== 5. 同名外键 ON UPDATE CASCADE 前置失败 =="
$MYSQL -e "CREATE DATABASE \`${S_UPD}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_UPD" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_UPD" <<'SQL' >/dev/null 2>&1
ALTER TABLE marketing_activity ADD UNIQUE KEY uk_activity_id_store (activity_id, store_id);
CREATE TABLE marketing_publication (
  publication_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL, store_id BIGINT NOT NULL, version INT NOT NULL, channel VARCHAR(24) NOT NULL,
  public_slug VARCHAR(80) NOT NULL, source_code VARCHAR(64) NOT NULL, title VARCHAR(200) NOT NULL,
  summary VARCHAR(500) NULL, content_json JSON NULL, hero_asset_url VARCHAR(500) NULL, cta_label VARCHAR(50) NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'published', valid_from TIMESTAMP NULL, valid_to TIMESTAMP NULL,
  published_by BIGINT NULL, published_at DATETIME NULL, paused_by BIGINT NULL, paused_at DATETIME NULL,
  request_id VARCHAR(64) NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_publication_activity_version_channel (activity_id, version, channel),
  UNIQUE KEY uk_publication_slug (public_slug),
  UNIQUE KEY uk_publication_source_code (source_code),
  UNIQUE KEY uk_publication_request_id (request_id),
  UNIQUE KEY uk_publication_id_store (publication_id, store_id),
  UNIQUE KEY uk_publication_id_store_source_channel (publication_id, store_id, source_code, channel),
  KEY idx_publication_store_status_valid (store_id, status, valid_from, valid_to),
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id, store_id) REFERENCES marketing_activity (activity_id, store_id) ON DELETE RESTRICT ON UPDATE CASCADE,
  CONSTRAINT chk_publication_status CHECK (status IN ('published','paused','expired'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
SQL
dump_struct "$S_UPD" > "${EVID}/r3_badupdrule_before.txt" 2>&1
$MYSQL --delimiter=// "$S_UPD" < "$MIGRATION" > "${EVID}/r3_badupdrule_mig.txt" 2>&1
UPD_RC=$?
dump_struct "$S_UPD" > "${EVID}/r3_badupdrule_after.txt" 2>&1
UPD_SENT=$(grep -oE '__refuse_[a-z_]+__' "${EVID}/r3_badupdrule_mig.txt" | head -1)
if [ "$UPD_RC" -ne 0 ] && [ "$UPD_SENT" = "__refuse_marketing_publication_structure_mismatch__" ]; then chk "U1-badupdrule-refuse" "PASS" "ON UPDATE CASCADE 前置拒绝 rc=$UPD_RC sentinel=$UPD_SENT"; else chk "U1-badupdrule-refuse" "FAIL" "rc=$UPD_RC sentinel=$UPD_SENT"; fi
if diff -q "${EVID}/r3_badupdrule_before.txt" "${EVID}/r3_badupdrule_after.txt" >/dev/null 2>&1; then chk "U2-badupdrule-zeropartial" "PASS" "结构前后一致（零部分迁移）"; else chk "U2-badupdrule-zeropartial" "FAIL" "结构变化"; fi

say ""
say "=== R3 汇总 ==="
say "PASS=$PASS FAIL=$FAIL"
say "schemas: $S_M $S_SU $S_TRG $S_UPD"
echo "DONE"

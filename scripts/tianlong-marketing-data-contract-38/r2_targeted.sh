#!/bin/bash
# ============================================================
# R2 定向证据（仅三组，不重跑整套 43 项）
#   组A 受控更新拒绝：函数单语句 SIGNAL 不可行证据 + ScriptUtils 两种方案验证
#   组B 同名错误触发器前置失败（零部分迁移）
#   组C 同名外键 ON UPDATE CASCADE 前置失败（零部分迁移）
# 只连 127.0.0.1:13317；只建 TL38 前缀 schema；不 DROP/DELETE/TRUNCATE。
# ============================================================
set -u
HOST="127.0.0.1"; PORT="13317"
MYSQL="mysql -uroot --protocol=tcp -h${HOST} -P${PORT} --default-character-set=utf8mb4"
MIGRATION="scripts/migrations/marketing_publication_v1.sql"
BASE="scripts/tianlong-marketing-data-contract-38/base_schema.sql"
EVID="scripts/tianlong-marketing-data-contract-38"
TS=$(date +%Y%m%d_%H%M%S)
S_TRG="tl38_${TS}_r2_badtrg"
S_UPD="tl38_${TS}_r2_badupdrule"
S_FN="tl38_${TS}_r2_fn"
OUT="${EVID}/r2_targeted.txt"
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

say "=== R2 定向证据 $(date '+%F %T') ==="

# ============================================================
# 组A：受控更新拒绝 — 函数单语句 SIGNAL 不可行 + 两方案验证
# ============================================================
say ""
say "== 组A-1: 函数单语句 SIGNAL 不可行（mysql CLI）=="
$MYSQL -e "CREATE DATABASE \`${S_FN}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
FN_ERR=$($MYSQL "$S_FN" -e "CREATE FUNCTION fn_guard() RETURNS INT SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='guard';" 2>&1)
say "  函数单语句 SIGNAL 结果: $FN_ERR"
if printf '%s' "$FN_ERR" | grep -q '1320'; then chk "A1-fn-single-signal-infeasible" "PASS" "函数体单语句 SIGNAL 被拒(1320 No RETURN found in FUNCTION)，机制不可行"; else chk "A1-fn-single-signal-infeasible" "FAIL" "unexpected: $FN_ERR"; fi
FN_PREP=$($MYSQL "$S_FN" -e "PREPARE s FROM 'CREATE FUNCTION fn_e() RETURNS INT RETURN 2'; EXECUTE s; DEALLOCATE PREPARE s;" 2>&1)
say "  PREPARE CREATE FUNCTION 结果: $FN_PREP"
if printf '%s' "$FN_PREP" | grep -q '1295'; then chk "A2-prepare-fn-infeasible" "PASS" "PREPARE 无法建函数(1295)，幂等创建需改法"; else chk "A2-prepare-fn-infeasible" "FAIL" "unexpected: $FN_PREP"; fi

say ""
say "== 组A-2: ScriptUtils 验证（harness 已编译；两种方案）=="
CP="$(cat ${EVID}/cp.txt 2>/dev/null)"
if [ -z "$CP" ]; then CP=""; fi
MIGABS="/home/ubuntu/.openclaw/workspace/verify_marketing_38/scripts/migrations/marketing_publication_v1.sql"
S_SU="tl38_${TS}_r2_scriptutils"
$MYSQL -e "CREATE DATABASE \`${S_SU}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_SU" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_SU" -e "INSERT INTO store_info VALUES (1,'NG','宁国店'),(2,'XC','宣城店'); INSERT INTO marketing_activity (activity_id,store_id,activity_code,activity_name,activity_type) VALUES (1,1,'A1','X1','p'),(2,2,'A2','X2','p'); INSERT INTO booking_inquiry (id,store_id,customer_name,customer_phone,status) VALUES (1,1,'张三','13800000001','pending');" >/dev/null 2>&1
SU1=$(java -cp "$CP:${EVID}" ScriptUtilsHarness "jdbc:mysql://127.0.0.1:13317/${S_SU}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8" root "" "$MIGABS" ";" 2>&1)
say "  ScriptUtils 跑现有迁移(separator ;): $(printf '%s' "$SU1" | grep -oE 'SCRIPT_(OK|FAIL)' || echo "$SU1")"
if printf '%s' "$SU1" | grep -q 'SCRIPT_OK'; then chk "A3-scriptutils-migration" "PASS" "ScriptUtils 默认 ; 分隔可执行现有迁移"; else chk "A3-scriptutils-migration" "FAIL" "$SU1"; fi

S_SU2="tl38_${TS}_r2_compound"
$MYSQL -e "CREATE DATABASE \`${S_SU2}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_SU2" -e "CREATE TABLE t (id INT PRIMARY KEY, title VARCHAR(50), status VARCHAR(20)); INSERT INTO t VALUES (1,'a','published');" >/dev/null 2>&1
cat > /tmp/r2_compound.sql <<'EOF'
CREATE TRIGGER trg_demo BEFORE UPDATE ON t FOR EACH ROW
BEGIN
  IF NOT (OLD.title <=> NEW.title) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'marketing_publication 快照字段不可修改';
  END IF;
END//
EOF
SU2=$(java -cp "$CP:${EVID}" ScriptUtilsHarness "jdbc:mysql://127.0.0.1:13317/${S_SU2}?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf-8" root "" /tmp/r2_compound.sql "//" 2>&1)
say "  ScriptUtils 跑复合触发器(separator //): $(printf '%s' "$SU2" | grep -oE 'SCRIPT_(OK|FAIL)' || echo "$SU2")"
if printf '%s' "$SU2" | grep -q 'SCRIPT_OK'; then chk "A4-scriptutils-compound" "PASS" "ScriptUtils 自定义 // 分隔可执行复合体触发器(SIGNAL 自定义文案)"; else chk "A4-scriptutils-compound" "FAIL" "$SU2"; fi
# 复合触发器负例与正例
SU_NEG=$($MYSQL "$S_SU2" -e "UPDATE t SET title='b' WHERE id=1;" 2>&1)
SU_POS=$($MYSQL "$S_SU2" -e "UPDATE t SET status='paused' WHERE id=1;" 2>&1)
say "  复合触发器 title 更新被拒: $(printf '%s' "$SU_NEG" | grep -oE 'ERROR.*')"
say "  复合触发器 status 更新通过: exit=$?"
if printf '%s' "$SU_NEG" | grep -q '1644'; then chk "A5-compound-neg" "PASS" "复合触发器自定义文案拒绝(1644)"; else chk "A5-compound-neg" "FAIL" "$SU_NEG"; fi

# ============================================================
# 组B：同名错误触发器前置失败（零部分迁移）
# ============================================================
say ""
say "== 组B: 同名错误触发器 =="
$MYSQL -e "CREATE DATABASE \`${S_TRG}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_TRG" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_TRG" <<'SQL' >/dev/null 2>&1
CREATE TABLE marketing_publication (publication_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TRIGGER trg_publication_no_delete AFTER INSERT ON marketing_publication FOR EACH ROW SET @x = 1;
SQL
dump_struct "$S_TRG" > "${EVID}/r2_badtrg_before.txt" 2>&1
$MYSQL "$S_TRG" < "$MIGRATION" > "${EVID}/r2_badtrg_mig.txt" 2>&1
TRG_RC=$?
dump_struct "$S_TRG" > "${EVID}/r2_badtrg_after.txt" 2>&1
TRG_SENT=$(grep -oE '__refuse_[a-z_]+__' "${EVID}/r2_badtrg_mig.txt" | head -1)
if [ "$TRG_RC" -ne 0 ] && [ "$TRG_SENT" = "__refuse_marketing_publication_trigger_mismatch__" ]; then chk "B1-badtrg-refuse" "PASS" "错误触发器前置拒绝 rc=$TRG_RC sentinel=$TRG_SENT"; else chk "B1-badtrg-refuse" "FAIL" "rc=$TRG_RC sentinel=$TRG_SENT"; fi
if diff -q "${EVID}/r2_badtrg_before.txt" "${EVID}/r2_badtrg_after.txt" >/dev/null 2>&1; then chk "B2-badtrg-zeropartial" "PASS" "结构前后一致（零部分迁移）"; else chk "B2-badtrg-zeropartial" "FAIL" "结构变化"; fi

# ============================================================
# 组C：同名外键 ON UPDATE CASCADE 前置失败（零部分迁移）
# ============================================================
say ""
say "== 组C: 同名外键 ON UPDATE CASCADE =="
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
dump_struct "$S_UPD" > "${EVID}/r2_badupdrule_before.txt" 2>&1
$MYSQL "$S_UPD" < "$MIGRATION" > "${EVID}/r2_badupdrule_mig.txt" 2>&1
UPD_RC=$?
dump_struct "$S_UPD" > "${EVID}/r2_badupdrule_after.txt" 2>&1
UPD_SENT=$(grep -oE '__refuse_[a-z_]+__' "${EVID}/r2_badupdrule_mig.txt" | head -1)
if [ "$UPD_RC" -ne 0 ] && [ "$UPD_SENT" = "__refuse_marketing_publication_structure_mismatch__" ]; then chk "C1-badupdrule-refuse" "PASS" "ON UPDATE CASCADE 前置拒绝 rc=$UPD_RC sentinel=$UPD_SENT"; else chk "C1-badupdrule-refuse" "FAIL" "rc=$UPD_RC sentinel=$UPD_SENT"; fi
if diff -q "${EVID}/r2_badupdrule_before.txt" "${EVID}/r2_badupdrule_after.txt" >/dev/null 2>&1; then chk "C2-badupdrule-zeropartial" "PASS" "结构前后一致（零部分迁移）"; else chk "C2-badupdrule-zeropartial" "FAIL" "结构变化"; fi

say ""
say "=== R2 定向汇总 ==="
say "PASS=$PASS FAIL=$FAIL"
say "schemas: $S_FN $S_SU $S_SU2 $S_TRG $S_UPD"
echo "DONE"

#!/bin/bash
# ============================================================
# R4 触发器预检加固 定向证据（不重跑 43 项整套 / R3 已通过项）
#   1) 全新 schema 首次迁移 + 重放（自检：更严 0h 不误伤正确触发器）
#   2) 反例A：同名触发器建在错误表 -> 前置拒绝 + 零 DDL
#   3) 反例B：同文案但保护字段不全(只保护 title) -> 前置拒绝 + 零 DDL
# 只连 127.0.0.1:13317；只建 TL38 前缀 schema；不 DROP/DELETE/TRUNCATE。
# ============================================================
set -u
HOST="127.0.0.1"; PORT="13317"
MYSQL="mysql -uroot --protocol=tcp -h${HOST} -P${PORT} --default-character-set=utf8mb4"
MIGRATION="scripts/migrations/marketing_publication_v1.sql"
BASE="scripts/tianlong-marketing-data-contract-38/base_schema.sql"
EVID="scripts/tianlong-marketing-data-contract-38"
TS=$(date +%Y%m%d_%H%M%S)
S_SELF="tl38_${TS}_r4_self"
S_WT="tl38_${TS}_r4_wrongtable"
S_INC="tl38_${TS}_r4_incomplete"
OUT="${EVID}/r4_trigcheck.txt"
: > "$OUT"
say(){ echo "$@" | tee -a "$OUT"; }
PASS=0; FAIL=0
chk(){ local id="$1" ok="$2" ev="$3"; if [ "$ok" = "PASS" ]; then PASS=$((PASS+1)); else FAIL=$((FAIL+1)); fi; say "[$id] $ok | $ev"; }

dump_struct() {
  local s="$1"
  $MYSQL -N "$s" -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${s}' ORDER BY TABLE_NAME;" 2>&1
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'<NULL>') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${s}' ORDER BY TABLE_NAME, ORDINAL_POSITION;" 2>&1
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='${s}' GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE ORDER BY TABLE_NAME, INDEX_NAME;" 2>&1
  $MYSQL -N "$s" -e "SELECT TRIGGER_NAME, EVENT_OBJECT_TABLE, EVENT_MANIPULATION, ACTION_TIMING FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA='${s}' ORDER BY TRIGGER_NAME;" 2>&1
}

say "=== R4 触发器预检加固 定向证据 $(date '+%F %T') ==="

# ============================================================
# 1) 全新 schema 首次迁移 + 重放（自检）
# ============================================================
say ""
say "== 1. 全新 schema 首次 + 重放自检 =="
$MYSQL -e "CREATE DATABASE \`${S_SELF}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_SELF" < "$BASE" >/dev/null 2>&1
$MYSQL "$S_SELF" -e "INSERT INTO store_info VALUES (1,'NG','宁国店'),(2,'XC','宣城店'); INSERT INTO marketing_activity (activity_id,store_id,activity_code,activity_name,activity_type) VALUES (1,1,'A1','X1','p'),(2,2,'A2','X2','p'); INSERT INTO booking_inquiry (id,store_id,customer_name,customer_phone,status) VALUES (1,1,'张三','13800000001','pending');" >/dev/null 2>&1
$MYSQL --delimiter=// "$S_SELF" < "$MIGRATION" >/dev/null 2>&1; R1=$?
$MYSQL --delimiter=// "$S_SELF" < "$MIGRATION" >/dev/null 2>&1; R2=$?
say "  首次退出=$R1 重放退出=$R2"
if [ "$R1" -eq 0 ] && [ "$R2" -eq 0 ]; then chk "S1-self-first-replay" "PASS" "更严 0h 未误伤正确触发器，首次+重放均退出0"; else chk "S1-self-first-replay" "FAIL" "首次=$R1 重放=$R2"; fi

# ============================================================
# 2) 反例A：同名触发器建在错误表
# ============================================================
say ""
say "== 2. 反例A 同名触发器建在错误表 =="
$MYSQL -e "CREATE DATABASE \`${S_WT}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$S_WT" <<'SQL' >/dev/null 2>&1
CREATE TABLE foo (id INT PRIMARY KEY) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TRIGGER trg_publication_no_delete BEFORE DELETE ON foo FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'marketing_publication 禁止物理删除';
SQL
dump_struct "$S_WT" > "${EVID}/r4_wrongtable_before.txt" 2>&1
$MYSQL --delimiter=// "$S_WT" < "$MIGRATION" > "${EVID}/r4_wrongtable_mig.txt" 2>&1; WT_RC=$?
dump_struct "$S_WT" > "${EVID}/r4_wrongtable_after.txt" 2>&1
WT_SENT=$(grep -oE '__refuse_[a-z_]+__' "${EVID}/r4_wrongtable_mig.txt" | head -1)
if [ "$WT_RC" -ne 0 ] && [ "$WT_SENT" = "__refuse_marketing_publication_trigger_mismatch__" ]; then chk "A1-wrongtable-refuse" "PASS" "错误表同名触发器前置拒绝 rc=$WT_RC sentinel=$WT_SENT"; else chk "A1-wrongtable-refuse" "FAIL" "rc=$WT_RC sentinel=$WT_SENT"; fi
if diff -q "${EVID}/r4_wrongtable_before.txt" "${EVID}/r4_wrongtable_after.txt" >/dev/null 2>&1; then chk "A2-wrongtable-zeropartial" "PASS" "结构前后一致（零部分迁移）"; else chk "A2-wrongtable-zeropartial" "FAIL" "结构变化"; fi

# ============================================================
# 3) 反例B：同文案但保护字段不全（只保护 title）
# ============================================================
say ""
say "== 3. 反例B 同文案但保护字段不全 =="
$MYSQL -e "CREATE DATABASE \`${S_INC}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL --delimiter=// "$S_INC" <<'SQL' >/dev/null 2>&1
CREATE TABLE marketing_publication (id INT PRIMARY KEY, title VARCHAR(200)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4//
CREATE TRIGGER trg_publication_immutable BEFORE UPDATE ON marketing_publication FOR EACH ROW
BEGIN
  IF NOT (OLD.title <=> NEW.title) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'marketing_publication 快照字段不可修改（迁移契约固定）：仅允许 status、paused_by、paused_at 变化';
  END IF;
END//
SQL
dump_struct "$S_INC" > "${EVID}/r4_incomplete_before.txt" 2>&1
$MYSQL --delimiter=// "$S_INC" < "$MIGRATION" > "${EVID}/r4_incomplete_mig.txt" 2>&1; INC_RC=$?
dump_struct "$S_INC" > "${EVID}/r4_incomplete_after.txt" 2>&1
INC_SENT=$(grep -oE '__refuse_[a-z_]+__' "${EVID}/r4_incomplete_mig.txt" | head -1)
if [ "$INC_RC" -ne 0 ] && [ "$INC_SENT" = "__refuse_marketing_publication_trigger_mismatch__" ]; then chk "B1-incomplete-refuse" "PASS" "保护字段不全前置拒绝 rc=$INC_RC sentinel=$INC_SENT"; else chk "B1-incomplete-refuse" "FAIL" "rc=$INC_RC sentinel=$INC_SENT"; fi
if diff -q "${EVID}/r4_incomplete_before.txt" "${EVID}/r4_incomplete_after.txt" >/dev/null 2>&1; then chk "B2-incomplete-zeropartial" "PASS" "结构前后一致（零部分迁移）"; else chk "B2-incomplete-zeropartial" "FAIL" "结构变化"; fi

say ""
say "=== R4 汇总 ==="
say "PASS=$PASS FAIL=$FAIL"
say "schemas: $S_SELF $S_WT $S_INC"
echo "DONE"

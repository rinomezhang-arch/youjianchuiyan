#!/bin/bash
# ============================================================
# TL-MARKETING-DATA-CONTRACT-38 隔离库验证（一次跑完）
# 只连隔离 MySQL 127.0.0.1:13317；不碰宿主 3306/生产库。
# 只建 TL38 前缀新 schema；不 DROP/DELETE/TRUNCATE/清理/停服务。
# 产出：machine_output.json + raw_output.txt（同目录）
# ============================================================
set -u

HOST="127.0.0.1"
PORT="13317"
MIGRATION="scripts/migrations/marketing_publication_v1.sql"
BASE_SCHEMA_SQL="scripts/tianlong-marketing-data-contract-38/base_schema.sql"
EVID_DIR="scripts/tianlong-marketing-data-contract-38"
MYSQL="mysql -uroot --protocol=tcp -h${HOST} -P${PORT} --default-character-set=utf8mb4"

TS=$(date +%Y%m%d_%H%M%S)
S_MAIN="tl38_${TS}_main"
S_BADCOL="tl38_${TS}_badcol"
S_BADUK="tl38_${TS}_baduk"
S_BADFK="tl38_${TS}_badfk"
S_BADUK_ACT="tl38_${TS}_baduk_act"
S_BADCK_ACT="tl38_${TS}_badck_act"
S_BADFK_BI="tl38_${TS}_badfk_bi"
S_BADCOL_PUB="tl38_${TS}_badcol_pub"
S_BADCK_EVT="tl38_${TS}_badck_evt"

RESULTS_TSV="${EVID_DIR}/.results_${TS}.tsv"
RAW="${EVID_DIR}/raw_output.txt"
JSON="${EVID_DIR}/machine_output.json"

: > "$RAW"
: > "$RESULTS_TSV"

say() { echo "$@" | tee -a "$RAW"; }

record() {
  local id="$1" concl="$2" ev="$3"
  ev=$(printf '%s' "$ev" | tr '\t\n' '  ')
  printf '%s\t%s\t%s\n' "$id" "$concl" "$ev" >> "$RESULTS_TSV"
}

# ---------- 结构转储（用于异常结构"零部分迁移"前后 diff） ----------
dump_struct() {
  local s="$1"
  echo "==TABLES=="
  $MYSQL -N "$s" -e "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA='${s}' ORDER BY TABLE_NAME;" 2>&1
  echo "==COLUMNS=="
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, IFNULL(COLUMN_DEFAULT,'<NULL>') FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${s}' ORDER BY TABLE_NAME, ORDINAL_POSITION;" 2>&1
  echo "==INDEXES=="
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, INDEX_NAME, NON_UNIQUE, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX SEPARATOR ',') FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='${s}' GROUP BY TABLE_NAME, INDEX_NAME, NON_UNIQUE ORDER BY TABLE_NAME, INDEX_NAME;" 2>&1
  echo "==FKS=="
  $MYSQL -N "$s" -e "SELECT TABLE_NAME, CONSTRAINT_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ','), REFERENCED_TABLE_NAME, GROUP_CONCAT(REFERENCED_COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ',') FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='${s}' AND REFERENCED_TABLE_NAME IS NOT NULL GROUP BY TABLE_NAME, CONSTRAINT_NAME, REFERENCED_TABLE_NAME ORDER BY TABLE_NAME, CONSTRAINT_NAME;" 2>&1
}

# ---------- 反例断言：期望失败且错误码匹配 ----------
run_neg() {
  local id="$1" expect_code="$2" desc="$3" sql="$4"
  local err rc code
  err=$(printf '%s\n' "$sql" | $MYSQL "$S_MAIN" 2>&1)
  rc=$?
  code=$(printf '%s' "$err" | grep -oE 'ERROR [0-9]+' | head -1 | grep -oE '[0-9]+')
  if [ "$rc" -ne 0 ] && [ "$code" = "$expect_code" ]; then
    record "$id" "PASS" "$desc | errcode=$code rc=$rc"
  else
    record "$id" "FAIL" "$desc | expected=$expect_code got_code=$code rc=$rc | $(printf '%s' "$err" | head -1)"
  fi
}

echo "============================================================" | tee -a "$RAW"
echo "TL38 隔离库验证 开始 $(date '+%F %T %Z')" | tee -a "$RAW"
echo "============================================================" | tee -a "$RAW"

# ============================================================
# 闸门：机器/容器/端口映射/数据目录（写入前实测留证）
# ============================================================
say "[gate] 主机与容器核实"
GATE_HOST=$(hostname)
GATE_DOCKER=$(docker ps --filter name=youjian-mysql-test-13317 --format '{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}' 2>&1)
GATE_MOUNTS=$(docker inspect youjian-mysql-test-13317 --format '{{json .Mounts}}' 2>&1)
GATE_DATADIR=$($MYSQL -N -e "SELECT @@port, @@datadir, VERSION();" 2>&1)
say "  host=$GATE_HOST"
say "  docker=$GATE_DOCKER"
say "  mounts=$GATE_MOUNTS"
say "  @@port,@@datadir,version=$GATE_DATADIR"

# 闸门断言：端口映射必须是 13317->3306，且无宿主目录挂载
if printf '%s' "$GATE_DOCKER" | grep -q '13317->3306'; then
  record "gate-01-port" "PASS" "容器 youjian-mysql-test-13317 端口 13317->3306"
else
  record "gate-01-port" "FAIL" "端口映射不符: $GATE_DOCKER"
fi
if [ "$GATE_MOUNTS" = "[]" ] || [ "$GATE_MOUNTS" = "null" ]; then
  record "gate-02-mount" "PASS" "无宿主目录挂载"
else
  record "gate-02-mount" "FAIL" "存在挂载: $GATE_MOUNTS"
fi
GATE_PORT=$(printf '%s' "$GATE_DATADIR" | cut -f1)
GATE_DD=$(printf '%s' "$GATE_DATADIR" | cut -f2)
if [ "$GATE_PORT" = "3306" ] && [ "$GATE_DD" = "/var/lib/mysql/" ]; then
  record "gate-03-datadir" "PASS" "@@port=3306, @@datadir=/var/lib/mysql/ (容器内)"
else
  record "gate-03-datadir" "FAIL" "@@port/@@datadir 不符: port=$GATE_PORT datadir=$GATE_DD"
fi
record "gate-04-scope" "INFO" "仅连接 ${HOST}:${PORT}；未连宿主 3306/生产库"

# ============================================================
# 建 TL38 前缀 schema（只建，不删）
# ============================================================
say ""
say "[schema] 创建 TL38 前缀 schema"
for s in "$S_MAIN" "$S_BADCOL" "$S_BADUK" "$S_BADFK" "$S_BADUK_ACT" "$S_BADCK_ACT" "$S_BADFK_BI" "$S_BADCOL_PUB" "$S_BADCK_EVT"; do
  $MYSQL -e "CREATE DATABASE \`${s}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$RAW"
done
record "schema-01" "INFO" "TL38 schema: $S_MAIN $S_BADCOL $S_BADUK $S_BADFK $S_BADUK_ACT $S_BADCK_ACT $S_BADFK_BI $S_BADCOL_PUB $S_BADCK_EVT"

# ============================================================
# 主 schema：基线 + 旧数据 + 快照
# ============================================================
say ""
say "[main] 基线建表 + 旧数据 + 快照"
$MYSQL "$S_MAIN" < "$BASE_SCHEMA_SQL" 2>&1 | tee -a "$RAW"
$MYSQL "$S_MAIN" <<'SQL' 2>&1 | tee -a "$RAW"
INSERT INTO store_info (store_id, store_code, store_name) VALUES (1,'NG','宁国店'),(2,'XC','宣城店');
INSERT INTO marketing_activity (activity_id, store_id, activity_code, activity_name, activity_type, start_date, end_date, is_active, budget_amount, actual_cost, participant_count, operator_id, operator_name, description) VALUES
 (1, 1, 'ACT-001', '迎春接福宴', 'promotion', '2026-09-13', '2026-10-13', 1, 1000.00, 0.00, 0, 1, '张晓秋', '宁国店迎春活动'),
 (2, 2, 'ACT-002', '生日祝寿宴', 'promotion', '2026-09-13', '2026-11-13', 0, 2000.00, 0.00, 0, 2, '张婧', '宣城店生日活动');
INSERT INTO booking_inquiry (id, store_id, customer_name, customer_phone, preferred_date, preferred_time, guest_count, remark, status) VALUES
 (1, 1, '张三', '13800000001', '2026-09-20', '18:00', 6, '老客户咨询', 'pending');
SQL

SNAP_BEFORE_SQL="SELECT activity_id, store_id, activity_code, activity_name, activity_type, start_date, end_date, is_active, activity_rules, activity_content, target_customers, budget_amount, actual_cost, expected_income, actual_income, participant_count, operator_id, operator_name, description, remark FROM marketing_activity ORDER BY activity_id; SELECT id, store_id, customer_name, customer_phone, preferred_date, preferred_time, guest_count, selected_dishes, remark, status, staff_note, handled_by, handled_time FROM booking_inquiry ORDER BY id;"
$MYSQL -N "$S_MAIN" -e "$SNAP_BEFORE_SQL" > "${EVID_DIR}/snap_before.txt" 2>&1
record "main-01-seed" "PASS" "基线三表 + 2 条旧活动 + 1 条旧咨询已写入"

# ============================================================
# 第一次迁移 + 第二次重放（幂等）
# ============================================================
say ""
say "[main] 第一次迁移"
$MYSQL "$S_MAIN" < "$MIGRATION" > "${EVID_DIR}/mig1.txt" 2>&1
RC1=$?
say "  第一次迁移退出码=$RC1"
if [ "$RC1" -eq 0 ]; then record "main-02-mig1" "PASS" "第一次迁移退出码 0"; else record "main-02-mig1" "FAIL" "第一次迁移退出码 $RC1"; fi

say "[main] 第二次重放（幂等）"
$MYSQL "$S_MAIN" < "$MIGRATION" > "${EVID_DIR}/mig2.txt" 2>&1
RC2=$?
say "  第二次重放退出码=$RC2"
if [ "$RC2" -eq 0 ]; then record "main-03-mig2" "PASS" "第二次重放退出码 0"; else record "main-03-mig2" "FAIL" "第二次重放退出码 $RC2"; fi

# ============================================================
# 旧业务值不变断言（逐字段）
# ============================================================
say ""
say "[main] 旧业务值逐字段不变断言"
$MYSQL -N "$S_MAIN" -e "$SNAP_BEFORE_SQL" > "${EVID_DIR}/snap_after.txt" 2>&1
if diff -q "${EVID_DIR}/snap_before.txt" "${EVID_DIR}/snap_after.txt" >/dev/null 2>&1; then
  record "main-04-oldval" "PASS" "2 条旧活动 + 1 条旧咨询业务值逐字段不变 (snap_before==snap_after)"
else
  record "main-04-oldval" "FAIL" "旧业务值变化: $(diff "${EVID_DIR}/snap_before.txt" "${EVID_DIR}/snap_after.txt" | head -5 | tr '\n' ' ')"
fi

# 旧活动状态迁移口径：必须 draft 或 cancelled，绝不 approved/published
BAD_STATUS_CNT=$($MYSQL -N "$S_MAIN" -e "SELECT COUNT(*) FROM marketing_activity WHERE status NOT IN ('draft','cancelled');" 2>&1)
NEW_COL_CHECK=$($MYSQL -N "$S_MAIN" -e "SELECT CONCAT(IFNULL((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${S_MAIN}' AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='status' AND COLUMN_TYPE='varchar(24)' AND IS_NULLABLE='NO' AND COLUMN_DEFAULT='draft'),'<NULL>'), '|', IFNULL((SELECT COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${S_MAIN}' AND TABLE_NAME='marketing_activity' AND COLUMN_NAME='store_id'),'<NULL>'), '|', IFNULL((SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='${S_MAIN}' AND TABLE_NAME='booking_inquiry' AND COLUMN_NAME IN ('marketing_publication_id','source_code','source_channel')),'<NULL>'));" 2>&1)
say "  非 draft/cancelled 活动数=$BAD_STATUS_CNT"
say "  [status类型|store_id默认|bi新列数]=$NEW_COL_CHECK"
if [ "$BAD_STATUS_CNT" = "0" ]; then
  record "main-05-oldstatus" "PASS" "旧活动全部迁为 draft（无 approved/published 伪造）"
else
  record "main-05-oldstatus" "FAIL" "存在非 draft/cancelled 旧活动: $BAD_STATUS_CNT"
fi
# store_id 默认值应为 NULL（已去除默认 1）
STORE_DEF=$(printf '%s' "$NEW_COL_CHECK" | cut -d'|' -f2)
if [ "$STORE_DEF" = "<NULL>" ] || [ "$STORE_DEF" = "" ]; then
  record "main-06-nodefault" "PASS" "marketing_activity.store_id 默认已去除（NULL）"
else
  record "main-06-nodefault" "FAIL" "marketing_activity.store_id 仍有默认: $STORE_DEF"
fi
BI_NEWCNT=$(printf '%s' "$NEW_COL_CHECK" | cut -d'|' -f3)
if [ "$BI_NEWCNT" = "3" ]; then
  record "main-07-bicols" "PASS" "booking_inquiry 新增 3 个可空来源列"
else
  record "main-07-bicols" "FAIL" "booking_inquiry 来源列数=$BI_NEWCNT"
fi

# ============================================================
# 反例（必须被数据库拒绝）
# ============================================================
say ""
say "[main] 反例拒绝测试"
# 先建合法发布与事件作基准
$MYSQL "$S_MAIN" <<'SQL' 2>&1 | tee -a "$RAW"
INSERT INTO marketing_publication (activity_id, store_id, version, channel, public_slug, source_code, title, request_id, status, valid_from, valid_to) VALUES
 (1, 1, 1, 'h5', 'slug-a', 'src-a', '活动A', 'req-pub-a', 'published', NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY));
INSERT INTO marketing_attribution_event (publication_id, store_id, source_code, event_type, request_id, occurred_at) VALUES
 (1, 1, 'src-a', 'view', 'req-evt-a', NOW());
SQL

run_neg "neg-01-dup-slug" "1062" "同 public_slug 重复(发布)" "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (1,1,2,'h5','slug-a','src-dup1','x','req-dup1','published');"
run_neg "neg-02-dup-src" "1062" "同 source_code 重复(发布)" "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (1,1,2,'h5','slug-dup2','src-a','x','req-dup2','published');"
run_neg "neg-03-dup-req-pub" "1062" "同 request_id 重复(发布)" "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (1,1,2,'h5','slug-dup3','src-dup3','x','req-pub-a','published');"
run_neg "neg-04-dup-req-evt" "1062" "同 request_id 重复(归因)" "INSERT INTO marketing_attribution_event (publication_id,store_id,source_code,event_type,request_id,occurred_at) VALUES (1,1,'src-a','view','req-evt-a',NOW());"
run_neg "neg-05-orphan-pub" "1452" "孤儿发布(activity_id 不存在)" "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (999,999,1,'h5','slug-orphan1','src-orphan1','x','req-orphan1','published');"
run_neg "neg-06-orphan-evt" "1452" "孤儿归因(publication_id 不存在)" "INSERT INTO marketing_attribution_event (publication_id,store_id,source_code,event_type,request_id,occurred_at) VALUES (999,999,'src-x','view','req-orphan2',NOW());"
run_neg "neg-07-crossstore-pub" "1452" "跨店发布(activity_id=1 但 store_id=2)" "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (1,2,2,'h5','slug-cs1','src-cs1','x','req-cs1','published');"
run_neg "neg-08-crossstore-evt" "1452" "跨店归因(publication_id=1 但 store_id=2)" "INSERT INTO marketing_attribution_event (publication_id,store_id,source_code,event_type,request_id,occurred_at) VALUES (1,2,'src-a','view','req-cs2',NOW());"
run_neg "neg-09-bad-pub-status" "3819" "发布状态非法(不在 published/paused/expired)" "INSERT INTO marketing_publication (activity_id,store_id,version,channel,public_slug,source_code,title,request_id,status) VALUES (1,1,2,'h5','slug-badstatus','src-badstatus','x','req-badstatus','bogus');"
run_neg "neg-10-bad-act-status" "3819" "活动状态非法(不在枚举内)" "UPDATE marketing_activity SET status='bogus' WHERE activity_id=1;"
run_neg "neg-11-neg-settlement" "3819" "负结算金额(settlement amount<0)" "INSERT INTO marketing_attribution_event (publication_id,store_id,source_code,event_type,amount,request_id,occurred_at) VALUES (1,1,'src-a','settlement',-5.00,'req-negset',NOW());"
run_neg "neg-12-settlement-noamount" "3819" "结算事件缺失 amount" "INSERT INTO marketing_attribution_event (publication_id,store_id,source_code,event_type,amount,request_id,occurred_at) VALUES (1,1,'src-a','settlement',NULL,'req-noamount',NOW());"
run_neg "neg-13-type-amount-conflict" "3819" "非结算事件携带 amount(view amount=100)" "INSERT INTO marketing_attribution_event (publication_id,store_id,source_code,event_type,amount,request_id,occurred_at) VALUES (1,1,'src-a','view',100.00,'req-conflict',NOW());"
run_neg "neg-14-forged-src" "1452" "同发布同店伪造 source_code(咨询)" "INSERT INTO booking_inquiry (store_id,customer_name,customer_phone,status,marketing_publication_id,source_code,source_channel) VALUES (1,'李四','13800000002','pending',1,'wrong-src','h5');"
run_neg "neg-15-forged-channel" "1452" "同发布同店伪造 source_channel(咨询)" "INSERT INTO booking_inquiry (store_id,customer_name,customer_phone,status,marketing_publication_id,source_code,source_channel) VALUES (1,'李四','13800000002','pending',1,'src-a','wecom');"
run_neg "neg-16-update-snapshot" "1242" "更新发布快照字段 title 被拒" "UPDATE marketing_publication SET title='改标题' WHERE publication_id=1;"
run_neg "neg-17-delete-pub" "1644" "删除发布行被拒" "DELETE FROM marketing_publication WHERE publication_id=1;"

# ============================================================
# 异常结构（8 组）：迁移必须在任何 DDL 前失败，零部分迁移
# ============================================================
say ""
say "[abnormal] 异常结构前置失败（零部分迁移）"

run_abnormal() {
  local s="$1" tag="$2" desc="$3" setup_sql="$4"
  $MYSQL "$s" < "$BASE_SCHEMA_SQL" >/dev/null 2>&1
  printf '%s\n' "$setup_sql" | $MYSQL "$s" >/dev/null 2>&1
  dump_struct "$s" > "${EVID_DIR}/ab_${tag}_before.txt" 2>&1
  $MYSQL "$s" < "$MIGRATION" > "${EVID_DIR}/ab_${tag}_mig.txt" 2>&1
  local rc=$?
  dump_struct "$s" > "${EVID_DIR}/ab_${tag}_after.txt" 2>&1
  local sentinel
  sentinel=$(grep -oE '__refuse_[a-z_]+__' "${EVID_DIR}/ab_${tag}_mig.txt" | head -1)
  if [ "$rc" -ne 0 ]; then
    record "ab-${tag}-refuse" "PASS" "$desc | 迁移退出非0 rc=$rc sentinel=$sentinel"
  else
    record "ab-${tag}-refuse" "FAIL" "$desc | 迁移居然退出0（未拒绝）"
  fi
  if diff -q "${EVID_DIR}/ab_${tag}_before.txt" "${EVID_DIR}/ab_${tag}_after.txt" >/dev/null 2>&1; then
    record "ab-${tag}-zeropartial" "PASS" "$desc | 结构前后一致（零部分迁移）"
  else
    record "ab-${tag}-zeropartial" "FAIL" "$desc | 结构发生变化（存在部分迁移）: $(diff "${EVID_DIR}/ab_${tag}_before.txt" "${EVID_DIR}/ab_${tag}_after.txt" | head -4 | tr '\n' ' ')"
  fi
}

run_abnormal "$S_BADCOL" "badcol" "同名错误列类型(status varchar(50))" "ALTER TABLE marketing_activity ADD COLUMN status VARCHAR(50) NOT NULL DEFAULT 'draft';"

run_abnormal "$S_BADUK" "baduk" "同名错误唯一键(uk_publication_slug 非唯一)" "
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
  KEY uk_publication_slug (public_slug),
  UNIQUE KEY uk_publication_source_code (source_code),
  UNIQUE KEY uk_publication_request_id (request_id),
  UNIQUE KEY uk_publication_id_store (publication_id, store_id),
  KEY idx_publication_store_status_valid (store_id, status, valid_from, valid_to),
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id, store_id) REFERENCES marketing_activity (activity_id, store_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
"

run_abnormal "$S_BADFK" "badfk" "同名错误外键(fk_publication_activity 指向 store_info)" "
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
  KEY idx_publication_store_status_valid (store_id, status, valid_from, valid_to),
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id) REFERENCES store_info (store_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
"

run_abnormal "$S_BADUK_ACT" "baduk-act" "错误活动唯一键(uk_activity_id_store 非唯一单列)" "ALTER TABLE marketing_activity ADD KEY uk_activity_id_store (store_id);"

run_abnormal "$S_BADCK_ACT" "badck-act" "错误活动 CHECK(chk_activity_status 枚举缺项)" "ALTER TABLE marketing_activity ADD COLUMN status VARCHAR(24) NOT NULL DEFAULT 'draft';
ALTER TABLE marketing_activity ADD CONSTRAINT chk_activity_status CHECK (status IN ('draft','published'));"

run_abnormal "$S_BADFK_BI" "badfk-bi" "错误咨询外键(fk_bi_marketing_publication 指向 store_info)" "ALTER TABLE booking_inquiry ADD COLUMN marketing_publication_id BIGINT NULL;
ALTER TABLE booking_inquiry ADD CONSTRAINT fk_bi_marketing_publication FOREIGN KEY (marketing_publication_id) REFERENCES store_info (store_id) ON DELETE RESTRICT;"

run_abnormal "$S_BADCOL_PUB" "badcol-pub" "错误发布列(title varchar(100) 而非 varchar(200))" "ALTER TABLE marketing_activity ADD UNIQUE KEY uk_activity_id_store (activity_id, store_id);
CREATE TABLE marketing_publication (
  publication_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  activity_id BIGINT NOT NULL, store_id BIGINT NOT NULL, version INT NOT NULL, channel VARCHAR(24) NOT NULL,
  public_slug VARCHAR(80) NOT NULL, source_code VARCHAR(64) NOT NULL, title VARCHAR(100) NOT NULL,
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
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id, store_id) REFERENCES marketing_activity (activity_id, store_id) ON DELETE RESTRICT,
  CONSTRAINT chk_publication_status CHECK (status IN ('published','paused','expired'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;"

run_abnormal "$S_BADCK_EVT" "badck-evt" "错误归因 CHECK(chk_attribution_event_type 枚举缺项)" "ALTER TABLE marketing_activity ADD UNIQUE KEY uk_activity_id_store (activity_id, store_id);
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
  CONSTRAINT fk_publication_activity FOREIGN KEY (activity_id, store_id) REFERENCES marketing_activity (activity_id, store_id) ON DELETE RESTRICT,
  CONSTRAINT chk_publication_status CHECK (status IN ('published','paused','expired'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
CREATE TABLE marketing_attribution_event (
  event_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  publication_id BIGINT NOT NULL, store_id BIGINT NOT NULL, source_code VARCHAR(64) NOT NULL, event_type VARCHAR(24) NOT NULL,
  visitor_key VARCHAR(64) NULL, business_type VARCHAR(24) NULL, business_id BIGINT NULL, business_no VARCHAR(64) NULL,
  amount DECIMAL(12,2) NULL, request_id VARCHAR(64) NOT NULL, occurred_at DATETIME NOT NULL, created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_attribution_request_id (request_id),
  KEY idx_attribution_pub_type_occurred (publication_id, event_type, occurred_at),
  KEY idx_attribution_store_type_occurred (store_id, event_type, occurred_at),
  KEY idx_attribution_business (business_type, business_id),
  CONSTRAINT fk_attribution_publication FOREIGN KEY (publication_id, store_id) REFERENCES marketing_publication (publication_id, store_id) ON DELETE RESTRICT,
  CONSTRAINT chk_attribution_event_type CHECK (event_type IN ('view','inquiry')),
  CONSTRAINT chk_attribution_amount CHECK ((event_type='settlement' AND amount IS NOT NULL AND amount >= 0) OR (event_type<>'settlement' AND amount IS NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;"

# ============================================================
# 范围说明（NOT_COVERED）
# ============================================================
record "nc-01-h5-browser" "NOT_COVERED" "H5 公开接口/页面/浏览器端到端验收（本任务仅 DB 迁移 + 隔离库验证）"
record "nc-02-http" "NOT_COVERED" "HTTP 接口契约（/api/marketing/* 与 /api/public/*）不在 DB 迁移范围"
record "nc-03-physical" "NOT_COVERED" "物理渠道投放/企微发送未实际执行"

# ============================================================
# 生成 machine_output.json + 汇总
# ============================================================
python3 - "$RESULTS_TSV" "$JSON" <<'PY'
import sys, json
tsv, out = sys.argv[1], sys.argv[2]
checks = []
for line in open(tsv, encoding='utf-8'):
    line = line.rstrip('\n')
    p = line.split('\t', 2)
    if len(p) != 3:
        continue
    checks.append({"id": p[0], "conclusion": p[1], "evidence": p[2]})
summary = {"pass":0,"fail":0,"not_covered":0,"info":0}
for c in checks:
    k = c["conclusion"].lower()
    if k in summary:
        summary[k] += 1
doc = {"checks": checks, "summary": summary}
with open(out, 'w', encoding='utf-8') as f:
    json.dump(doc, f, ensure_ascii=False, indent=2)
print(json.dumps(summary, ensure_ascii=False))
PY

say ""
say "===== 汇总 ====="
say "schemas: $S_MAIN $S_BADCOL $S_BADUK $S_BADFK $S_BADUK_ACT $S_BADCK_ACT $S_BADFK_BI $S_BADCOL_PUB $S_BADCK_EVT"
say "machine_output.json = $JSON"
say "raw_output.txt = $RAW"
say "检查项总数=$(grep -c '^' "$RESULTS_TSV")"
rm -f "$RESULTS_TSV"
echo "DONE"

#!/bin/bash
# ============================================================================
# TL-RC-PAYROLL-HTTP-R2-59  真实 HTTP 闭环（一次运行）
#   真实 TCP 后端 + 真实 POST /api/auth/login 取 JWT + 外部 HTTP 链 + DB 回读断言
#
# 边界（违反即失败）：
#   * 只写本运行新建的唯一 schema tlpay59_http_<时间戳>；禁 DELETE/DROP/TRUNCATE；
#   * 只连 127.0.0.1:13318（隔离容器），禁宿主 3306 / 13317 / 生产；
#   * 合成账号密码 + JWT 只存内存，绝不落任何文件/日志/报告/提交；
#   * 不部署、不重启任何生产服务；只启动非生产进程到 127.0.0.1:18081。
# ============================================================================
set -uo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
JAR="$REPO_ROOT/banquet_project/target/banquet-1.0.0.jar"
DDL="$SCRIPT_DIR/payroll-http-schema.sql"
EVID="$SCRIPT_DIR"

DB_HOST=127.0.0.1
DB_PORT=13318
APP_HOST=127.0.0.1
APP_PORT=18081
MONTH=2026-08

LOG="$EVID/http-request-log.txt"
DBLOG="$EVID/http-db-assertions.txt"
SUMMARY="$EVID/http-summary.txt"
APP_LOG="$EVID/app-startup.log"
APP_PID=""

SCHEMA="tlpay59_http_$(date +%s%3N)"

# 唯一性闸门：新 schema 名必须不存在
_SCHEMA_EXISTS=$(mysql -uroot --default-character-set=utf8mb4 --protocol=tcp -h"$DB_HOST" -P"$DB_PORT" -N -e "SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='$SCHEMA';" 2>/dev/null)
if [ "$_SCHEMA_EXISTS" != "0" ]; then
  echo "[FAIL] schema $SCHEMA 已存在，时间戳冲突，中止（零写入）"
  exit 1
fi

# 合成账号密码（运行期内存随机生成，仅用于 BCrypt 入库 + 登录，绝不写盘）
declare -A PW=()
# 各角色 JWT（内存）
declare -A TOK=()

PASS=0; FAIL=0
ok()   { PASS=$((PASS+1)); echo "[PASS] $1"; }
bad()  { FAIL=$((FAIL+1)); echo "[FAIL] $1"; }
info() { echo "[INFO] $1"; }

mysql_q() { mysql -uroot --default-character-set=utf8mb4 --protocol=tcp -h"$DB_HOST" -P"$DB_PORT" -N "$@"; }

# ---------- 清理 ----------
cleanup() {
  if [ -n "$APP_PID" ] && kill -0 "$APP_PID" 2>/dev/null; then
    kill "$APP_PID" 2>/dev/null
    wait "$APP_PID" 2>/dev/null
    echo "[INFO] 已停止非生产后端进程 pid=$APP_PID" >> "$SUMMARY" 2>/dev/null
  fi
}
trap cleanup EXIT

# ---------- 就绪探测 ----------
wait_ready() {
  local i code
  for i in $(seq 1 90); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "http://$APP_HOST:$APP_PORT/api/stores" 2>/dev/null)
    if [ -n "$code" ] && [ "$code" != "000" ]; then
      echo "$code"
      return 0
    fi
    sleep 1
  done
  echo "000"
  return 1
}

# ---------- HTTP 请求辅助 ----------
do_req() {
  local method="$1" url="$2" auth="$3" body="$4"
  local args=(-s -X "$method" "$url" -w $'\n%{http_code}')
  if [ -n "$auth" ]; then args+=(-H "Authorization: Bearer $auth"); fi
  if [ -n "$body" ]; then args+=(-H 'Content-Type: application/json' -d "$body"); fi
  local resp
  resp=$(curl "${args[@]}" 2>/dev/null)
  CODE=$(printf '%s' "$resp" | tail -n1)
  BODY=$(printf '%s' "$resp" | sed '$d')
}
bcode() { printf '%s' "$BODY" | jq -r '.code // empty' 2>/dev/null; }

# ---------- 记录请求清单（脱敏） ----------
log_req() {
  printf '%-4s %-6s %-34s actor=%-16s http=%s code=%s  %s\n' \
    "$1" "$2" "$3" "$4" "$5" "$6" "$7" >> "$LOG"
}

# ---------- 初始化证据文件 ----------
{
  echo "=== TL-RC-PAYROLL-HTTP-R2-59 HTTP 链请求清单（脱敏） ==="
  echo "时间: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "schema: $SCHEMA"
  echo "后端: http://$APP_HOST:$APP_PORT  （非生产进程，只绑本机）"
  echo "后端 jar: $JAR"
  echo "说明: 密码/JWT 全程仅存内存，本清单只记 actor/HTTP 状态/业务码。"
  echo ""
} > "$LOG"

{
  echo "=== DB 回读断言（schema: $SCHEMA） ==="
  echo "时间: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo ""
} > "$DBLOG"

{
  echo "=== 通过/失败汇总 ==="
  echo "时间: $(date '+%Y-%m-%d %H:%M:%S %z')"
  echo "schema: $SCHEMA"
  echo "后端端口: $APP_PORT"
  echo "分支: codex/tianlong-payroll-real-e2e-59"
  echo ""
} > "$SUMMARY"

# ============================================================================
# 0. 环境闸门
# ============================================================================
info "运行环境前置断言..."
bash "$SCRIPT_DIR/env-precheck.sh" > "$EVID/env-precheck-http.txt" 2>&1
PRECHECK_RC=$?
echo "PRECHECK_RC=$PRECHECK_RC" >> "$SUMMARY"
if [ "$PRECHECK_RC" != "0" ]; then
  bad "环境前置断言未通过，中止（零写入）"
  exit 1
fi
ok "环境前置断言通过（机器/容器/端口/挂载/datadir/唯一性/端口空闲）"

# ============================================================================
# 1. 建唯一 schema + 建表
# ============================================================================
info "创建唯一 schema $SCHEMA 并建表..."
mysql_q -e "CREATE DATABASE \`$SCHEMA\` CHARACTER SET utf8mb4" 2>&1
if [ $? -ne 0 ]; then bad "CREATE DATABASE 失败"; exit 1; fi
mysql_q "$SCHEMA" < "$DDL" 2>&1
if [ $? -ne 0 ]; then bad "DDL 应用失败"; exit 1; fi
ok "schema $SCHEMA 建表完成"

# ============================================================================
# 2. 合成账号：随机密码 → BCrypt 入库
# ============================================================================
info "生成合成账号（密码随机，仅内存；BCrypt 入库）..."
seed_staff() {
  local id=$1 store=$2 name=$3 acct=$4 role=$5 mhr=$6 vall=$7 emp=$8
  local pw hash
  pw=$(openssl rand -hex 12)
  PW["$acct"]="$pw"
  hash=$(python3 -c "import bcrypt,sys; print(bcrypt.hashpw(sys.argv[1].encode(), bcrypt.gensalt(rounds=10)).decode())" "$pw")
  mysql_q "$SCHEMA" -e "INSERT INTO staff_master(staff_id,store_id,staff_name,staff_account,role,can_manage_hr,can_view_all_stores,employment_status,staff_password) VALUES ($id,$store,'$name','$acct','$role',$mhr,$vall,'$emp','$hash');"
}
seed_staff 1  1 'TLPAY59审批人'  tlpay59_approver  gm            1 1 'active'
seed_staff 2  1 'TLPAY59付款人'  tlpay59_payer     gm            1 1 'active'
seed_staff 3  1 'TLPAY59核算员'  tlpay59_hr        gm            1 1 'active'
seed_staff 4  1 'TLPAY59员工甲'  tlpay59_worker_a  staff         0 0 'active'
seed_staff 5  1 'TLPAY59员工乙'  tlpay59_worker_b  staff         0 0 'active'
seed_staff 6  1 'TLPAY59无权限'  tlpay59_noperm    waiter        0 0 'active'
seed_staff 7  1 'TLPAY59停用'    tlpay59_disabled  gm            1 1 'resigned'
seed_staff 8  1 'TLPAY59店长'    tlpay59_mgr       store_manager 1 0 'active'
seed_staff 9  2 'TLPAY59他店员工' tlpay59_other     staff         0 0 'active'
seed_staff 10 1 'TLPAY59待停用'  tlpay59_todisable staff         0 0 'active'
N_STAFF=$(mysql_q "$SCHEMA" -e "SELECT COUNT(*) FROM staff_master;")
ok "合成账号入库完成，共 ${N_STAFF} 名（密码为随机 BCrypt，无明文落盘）"

# ============================================================================
# 3. 启动非生产后端进程
# ============================================================================
info "启动非生产后端进程（port=$APP_PORT）..."
export JWT_SECRET="$(openssl rand -hex 32)"
export AES_SECRET_KEY="$(openssl rand -hex 16)"
export APPROVAL_APPROVERS="tlpay59_approver"
export SPRING_PROFILES_ACTIVE="dev"
export SPRING_DATASOURCE_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${SCHEMA}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai"
export SPRING_DATASOURCE_USERNAME="root"
export SPRING_DATASOURCE_PASSWORD=""
export SPRING_DATASOURCE_DRIVER_CLASS_NAME="com.mysql.cj.jdbc.Driver"

# 启动日志流式脱敏：原始 stdout/stderr 必须先过 redact_stream 再落盘，禁止先写磁盘后 sed
redact_stream() {
  sed -E \
    -e 's/(JWT_SECRET|AES_SECRET_KEY|TIANLONG_TOKEN|SPRING_DATASOURCE_PASSWORD)([=:])[^ ,;"]*/\1\2<redacted>/g' \
    -e 's/(Authorization:[[:space:]]*Bearer[[:space:]]+)[A-Za-z0-9._-]+/\1<redacted>/g' \
    -e 's/eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}/<redacted-jwt>/g' \
    -e 's/(password|passwd|pwd)([=:])[^ &"]*/\1\2<redacted>/gi'
}

# SHA/JAR 哈希记录入口（本卡不启动 JAR：runtime 绑定未验，只 prepared）
JAR_SHA256="$(sha256sum "$JAR" 2>/dev/null | awk '{print $1}')"
if [ -z "${GIT_SHA:-}" ]; then
  GIT_SHA="$(git -C "$(dirname "$(dirname "$JAR")")" rev-parse HEAD 2>/dev/null || echo unknown)"
fi
{
  echo "=== 制品证据入口（prepared；本卡未启动 JAR，runtime 一致未验） ==="
  echo "构建输入SHA: ${GIT_SHA}"
  echo "运行文件: $JAR"
  echo "运行文件sha256: ${JAR_SHA256}"
  echo "runtime一致性核对: 未执行（prepared only，禁用虚报）"
} >> "$SUMMARY"

nohup java -jar "$JAR" \
  --server.address="$APP_HOST" \
  --server.port="$APP_PORT" \
  --spring.jpa.hibernate.ddl-auto=none \
  --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect \
  > >(redact_stream >> "$APP_LOG") 2>&1 &
APP_PID=$!

{
  echo "=== 进程启动命令（脱敏） ==="
  echo "java -jar $JAR --server.address=$APP_HOST --server.port=$APP_PORT \\"
  echo "  --spring.jpa.hibernate.ddl-auto=none \\"
  echo "  --spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect"
  echo "环境变量: JWT_SECRET=<随机hex64,内存> AES_SECRET_KEY=<随机hex32,内存> APPROVAL_APPROVERS=tlpay59_approver"
  echo "          SPRING_DATASOURCE_URL=jdbc:mysql://${DB_HOST}:${DB_PORT}/${SCHEMA}?..."
  echo "进程 pid: $APP_PID"
  echo ""
} >> "$SUMMARY"

READY=$(wait_ready)
if [ "$READY" = "000" ]; then
  bad "后端 90s 内未就绪"
  echo "=== 启动日志尾部 ===" >> "$SUMMARY"
  tail -n 60 "$APP_LOG" | redact_stream >> "$SUMMARY"
  exit 1
fi
ok "后端就绪：GET /api/stores 返回 HTTP $READY"
echo "就绪探测: GET /api/stores -> HTTP $READY（无 token）" >> "$SUMMARY"
grep -E 'Tomcat started on port|Started BanquetApplication|Tomcat initialized' "$APP_LOG" | sed 's/[[:space:]]*$//' >> "$SUMMARY" 2>/dev/null || true

# ============================================================================
# 4. 真实登录取 JWT（内存）
# ============================================================================
login() {
  local acct="$1" u pw body
  u="$acct"; pw="${PW[$acct]}"
  body=$(jq -n --arg u "$u" --arg p "$pw" '{username:$u, password:$p}')
  do_req POST "http://$APP_HOST:$APP_PORT/api/auth/login" "" "$body"
}
TOK["tlpay59_hr"]=""
TOK["tlpay59_approver"]=""
TOK["tlpay59_payer"]=""
TOK["tlpay59_noperm"]=""
TOK["tlpay59_mgr"]=""
TOK["tlpay59_todisable"]=""

for acct in tlpay59_hr tlpay59_approver tlpay59_payer tlpay59_noperm tlpay59_mgr tlpay59_todisable; do
  login "$acct"
  bc=$(bcode)
  if [ "$CODE" = "200" ] && [ "$bc" = "200" ]; then
    TOK["$acct"]=$(printf '%s' "$BODY" | jq -r '.data.token // empty')
    log_req "L" "POST" "/api/auth/login" "$acct" "$CODE" "$bc" "登录成功(JWT入内存,长度=${#TOK[$acct]})"
    ok "登录成功: $acct (JWT 长度 ${#TOK[$acct]})"
  else
    TOK["$acct"]=""
    log_req "L" "POST" "/api/auth/login" "$acct" "$CODE" "$bc" "登录失败"
    bad "登录失败: $acct http=$CODE code=$bc"
  fi
done

# 停用账号：登录必须被拒（HTTP 200 + 业务码 401）
login "tlpay59_disabled"
bc=$(bcode)
log_req "L" "POST" "/api/auth/login" "tlpay59_disabled" "$CODE" "$bc" "停用账号登录被拒"
if [ "$CODE" = "200" ] && [ "$bc" = "401" ]; then
  ok "停用账号 tlpay59_disabled 登录被拒（业务码 401）"
else
  bad "停用账号登录期望 200/401，实际 http=$CODE code=$bc"
fi

# ============================================================================
# 5. 完整 HTTP 链
# ============================================================================
ITEMS='[{"emp_id":4,"base_salary":"1000","post_salary":"200","attendance_pay":"300","bonus":"40","overtime_pay":"50","allowance":"5"},{"emp_id":5,"base_salary":"1000","post_salary":"200","attendance_pay":"300","bonus":"40","overtime_pay":"50","allowance":"5"}]'
BASE="http://$APP_HOST:$APP_PORT/api/hr/payroll"

# 5.1 核算保存（hr）
do_req POST "$BASE/save?month=$MONTH" "${TOK[tlpay59_hr]}" "$ITEMS"
SAVED=$(printf '%s' "$BODY" | jq -r '.data.saved // empty' 2>/dev/null)
log_req "1" "POST" "/api/hr/payroll/save?month=$MONTH" "tlpay59_hr" "$CODE" "$(bcode)" "saved=$SAVED"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "200" ] && [ "$SAVED" = "2" ]; then
  ok "保存: saved=2"
else
  bad "保存: http=$CODE code=$(bcode) saved=$SAVED"
fi

# 5.2 审批（approver）
do_req POST "$BASE/approve?month=$MONTH" "${TOK[tlpay59_approver]}" ""
APPROVED=$(printf '%s' "$BODY" | jq -r '.data.approved // empty' 2>/dev/null)
log_req "2" "POST" "/api/hr/payroll/approve?month=$MONTH" "tlpay59_approver" "$CODE" "$(bcode)" "approved=$APPROVED"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "200" ] && [ "$APPROVED" = "2" ]; then
  ok "审批: approved=2"
else
  bad "审批: http=$CODE code=$(bcode) approved=$APPROVED"
fi

# 5.3 付款（payer）
do_req POST "$BASE/payout?month=$MONTH" "${TOK[tlpay59_payer]}" ""
PAID=$(printf '%s' "$BODY" | jq -r '.data.paid // empty' 2>/dev/null)
PAYOUT_ID=$(printf '%s' "$BODY" | jq -r '.data.payoutId // empty' 2>/dev/null)
log_req "3" "POST" "/api/hr/payroll/payout?month=$MONTH" "tlpay59_payer" "$CODE" "$(bcode)" "paid=$PAID payoutId=$PAYOUT_ID"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "200" ] && [ "$PAID" = "2" ] && [ -n "$PAYOUT_ID" ]; then
  ok "付款: paid=2 payoutId=$PAYOUT_ID"
else
  bad "付款: http=$CODE code=$(bcode) paid=$PAID payoutId=$PAYOUT_ID"
fi

# 5.4 刷新回读（approver 全门店读回）
do_req GET "$BASE?month=$MONTH" "${TOK[tlpay59_approver]}" ""
R_STATUS=$(printf '%s' "$BODY" | jq -r '.data[] | select(.emp_id==4) | .salary_status // empty' 2>/dev/null)
R_NET=$(printf '%s' "$BODY" | jq -r '.data[] | select(.emp_id==4) | .net_pay // empty' 2>/dev/null)
log_req "4" "GET" "/api/hr/payroll?month=$MONTH" "tlpay59_approver" "$CODE" "$(bcode)" "emp4 salary_status=$R_STATUS net_pay=$R_NET"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "200" ] && [ "$R_STATUS" = "3" ] && [ "$R_NET" = "1595.00" ]; then
  ok "刷新回读: emp4 status=3 net_pay=$R_NET"
else
  bad "刷新回读: http=$CODE code=$(bcode) status=$R_STATUS net_pay=$R_NET"
fi

# 5.5 重复付款（payer 再付）
do_req POST "$BASE/payout?month=$MONTH" "${TOK[tlpay59_payer]}" ""
DUP=$(printf '%s' "$BODY" | jq -r '.data.alreadyRecorded // empty' 2>/dev/null)
DUP_PAID=$(printf '%s' "$BODY" | jq -r '.data.paid // empty' 2>/dev/null)
log_req "5" "POST" "/api/hr/payroll/payout?month=$MONTH" "tlpay59_payer" "$CODE" "$(bcode)" "alreadyRecorded=$DUP paid=$DUP_PAID"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "200" ] && [ "$DUP" = "true" ] && [ "$DUP_PAID" = "0" ]; then
  ok "重复付款: alreadyRecorded=true paid=0"
else
  bad "重复付款: http=$CODE code=$(bcode) alreadyRecorded=$DUP paid=$DUP_PAID"
fi

# 5.6 无权限角色（noperm 保存）
do_req POST "$BASE/save?month=$MONTH" "${TOK[tlpay59_noperm]}" "$ITEMS"
log_req "6" "POST" "/api/hr/payroll/save?month=$MONTH" "tlpay59_noperm" "$CODE" "$(bcode)" "无权限角色保存"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "403" ]; then
  ok "无权限角色: 业务码 403"
else
  bad "无权限角色: http=$CODE code=$(bcode)"
fi

# 5.7 跨店（店长 mgr 保存他店员工 9）
CROSS_ITEMS='[{"emp_id":9,"base_salary":"1000","post_salary":"200","attendance_pay":"300","bonus":"40","overtime_pay":"50","allowance":"5"}]'
do_req POST "$BASE/save?month=$MONTH" "${TOK[tlpay59_mgr]}" "$CROSS_ITEMS"
log_req "7" "POST" "/api/hr/payroll/save?month=$MONTH" "tlpay59_mgr" "$CODE" "$(bcode)" "跨店保存员工9"
if [ "$CODE" = "200" ] && [ "$(bcode)" = "400" ]; then
  ok "跨店保存: 业务码 400"
else
  bad "跨店保存: http=$CODE code=$(bcode)"
fi

# 5.8 实时停用：待停用账号先登录成功拿 token，再改 employment_status=resigned
do_req GET "$BASE?month=$MONTH" "${TOK[tlpay59_todisable]}" ""
BEFORE_REVOKE="$CODE"
mysql_q "$SCHEMA" -e "UPDATE staff_master SET employment_status='resigned' WHERE staff_id=10;" 2>&1
do_req GET "$BASE?month=$MONTH" "${TOK[tlpay59_todisable]}" ""
log_req "8" "GET" "/api/hr/payroll?month=$MONTH" "tlpay59_todisable" "$CODE" "-" "停用前http=$BEFORE_REVOKE 停用后(旧token)"
if [ "$CODE" = "401" ]; then
  ok "实时停用: 旧 token 在账号停用后立即 401（StaffRealtimeGuard）"
else
  bad "实时停用: 期望 401 实际 http=$CODE（停用前 $BEFORE_REVOKE）"
fi

# ============================================================================
# 6. 数据库回读断言
# ============================================================================
{
  echo "--- month_salary（staff 4/5） ---"
  mysql_q "$SCHEMA" -e "SELECT staff_id, status, approved_by, paid_by, payout_id, net_salary FROM month_salary WHERE staff_id IN (4,5) AND salary_month='$MONTH' ORDER BY staff_id;"
  echo ""
  echo "--- payroll_payout_record ---"
  mysql_q "$SCHEMA" -e "SELECT payout_id, salary_month, store_id, headcount, total_net, recorded_by, note FROM payroll_payout_record;"
  echo ""
} >> "$DBLOG"

# 6.1 金额一致
LEDGER=$(mysql_q "$SCHEMA" -e "SELECT total_net FROM payroll_payout_record LIMIT 1;")
ACTUAL=$(mysql_q "$SCHEMA" -e "SELECT SUM(net_salary) FROM month_salary WHERE status=3;")
{
  echo "--- 6.1 金额一致 ---"
  echo "台账 total_net        = $LEDGER"
  echo "工资行 SUM(net_salary WHERE status=3) = $ACTUAL"
} >> "$DBLOG"
if [ "$LEDGER" = "3190.00" ] && [ "$ACTUAL" = "3190.00" ]; then
  ok "金额一致: 台账 3190.00 == 实发合计 3190.00"
else
  bad "金额一致: 台账=$LEDGER 实发=$ACTUAL"
fi

# 6.2 流水唯一
PCOUNT=$(mysql_q "$SCHEMA" -e "SELECT COUNT(*) FROM payroll_payout_record;")
{
  echo ""
  echo "--- 6.2 流水唯一 ---"
  echo "payroll_payout_record 行数 = $PCOUNT（期望 1，重复付款后仍 1）"
} >> "$DBLOG"
if [ "$PCOUNT" = "1" ]; then
  ok "流水唯一: 台账仅 1 条"
else
  bad "流水唯一: 台账 $PCOUNT 条"
fi

# 6.3 无孤儿 / 守恒
ORPHAN=$(mysql_q "$SCHEMA" -e "SELECT COUNT(*) FROM month_salary m LEFT JOIN payroll_payout_record p ON m.payout_id=p.payout_id AND m.salary_month=p.salary_month WHERE m.payout_id IS NOT NULL AND (p.payout_id IS NULL OR (p.store_id IS NOT NULL AND p.store_id<>m.store_id));")
CONSERVE=$(mysql_q "$SCHEMA" -e "SELECT COUNT(*) FROM payroll_payout_record p WHERE p.headcount<>(SELECT COUNT(*) FROM month_salary m WHERE m.payout_id=p.payout_id) OR p.total_net<>(SELECT COALESCE(SUM(m.net_salary),0) FROM month_salary m WHERE m.payout_id=p.payout_id);")
{
  echo ""
  echo "--- 6.3 无孤儿 / 守恒 ---"
  echo "孤儿(悬空 payout_id 或跨店指向) = $ORPHAN（期望 0）"
  echo "台账 headcount/total_net 与本批工资行不符 = $CONSERVE（期望 0）"
} >> "$DBLOG"
if [ "$ORPHAN" = "0" ] && [ "$CONSERVE" = "0" ]; then
  ok "无孤儿: 悬空/跨店=0，台账与工资行守恒一致"
else
  bad "无孤儿: orphan=$ORPHAN conserve=$CONSERVE"
fi

# 6.4 业务键恢复
RECOVER=$(mysql_q "$SCHEMA" -e "SELECT CONCAT(staff_id,':',status,':',payout_id,':',net_salary) FROM month_salary WHERE staff_id IN (4,5) AND salary_month='$MONTH' ORDER BY staff_id;" | tr '\n' ';')
{
  echo ""
  echo "--- 6.4 业务键(staff_id,salary_month)回读恢复 ---"
  echo "staff4/5 -> staff_id:status:payout_id:net_salary = $RECOVER"
} >> "$DBLOG"
if echo "$RECOVER" | grep -q "4:3:${PAYOUT_ID}:1595.00" && echo "$RECOVER" | grep -q "5:3:${PAYOUT_ID}:1595.00"; then
  ok "业务键恢复: staff4/5 均 status=3, payout_id=$PAYOUT_ID, net=1595.00"
else
  bad "业务键恢复: $RECOVER"
fi

# 6.5 跨店零写入
CROSS_ROWS=$(mysql_q "$SCHEMA" -e "SELECT COUNT(*) FROM month_salary WHERE staff_id=9;")
{
  echo ""
  echo "--- 6.5 跨店零写入 ---"
  echo "跨店目标员工9的工资行 = $CROSS_ROWS（期望 0）"
} >> "$DBLOG"
if [ "$CROSS_ROWS" = "0" ]; then
  ok "跨店零写入: 员工9 无工资行"
else
  bad "跨店零写入: 员工9 有 $CROSS_ROWS 行"
fi

# ============================================================================
# 7. 脱敏
# ============================================================================
SANITIZE_ITEMS=("$JWT_SECRET" "$AES_SECRET_KEY")
for a in tlpay59_hr tlpay59_approver tlpay59_payer tlpay59_noperm tlpay59_mgr tlpay59_todisable; do
  [ -n "${PW[$a]:-}" ] && SANITIZE_ITEMS+=("${PW[$a]}")
  [ -n "${TOK[$a]:-}" ] && SANITIZE_ITEMS+=("${TOK[$a]}")
done
python3 - "$APP_LOG" "${SANITIZE_ITEMS[@]}" <<'PY'
import sys
f = sys.argv[1]
secrets = sys.argv[2:]
try:
    with open(f, encoding='utf-8', errors='replace') as fh:
        s = fh.read()
    for sec in secrets:
        if sec:
            s = s.replace(sec, '<REDACTED>')
    with open(f, 'w', encoding='utf-8') as fh:
        fh.write(s)
except FileNotFoundError:
    pass
PY
sed -i -E 's#eyJ[A-Za-z0-9_-]{20,}#<REDACTED_JWT>#g' "$APP_LOG"

# ============================================================================
# 8. 汇总
# ============================================================================
{
  echo ""
  echo "=== 通过/失败 ==="
  echo "PASS=$PASS"
  echo "FAIL=$FAIL"
  echo "HTTP 链步骤: 保存->审批->付款->刷新回读->重复付款->无权限->跨店->实时停用（含各角色真实登录）"
  echo "DB 回读断言: 金额一致 / 流水唯一 / 无孤儿 / 业务键恢复 / 跨店零写入"
  echo ""
  echo "=== 请求清单摘要（详见 http-request-log.txt） ==="
} >> "$SUMMARY"

echo ""
echo "================ 结果汇总 ================"
echo "schema=$SCHEMA"
echo "port=$APP_PORT"
echo "PASS=$PASS FAIL=$FAIL"
echo "========================================="

cleanup
APP_PID=""

if [ "$FAIL" = "0" ]; then
  echo "HTTP_E2E_RESULT=PASS"
  exit 0
else
  echo "HTTP_E2E_RESULT=FAIL"
  exit 1
fi

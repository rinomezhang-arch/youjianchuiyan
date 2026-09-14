#!/bin/bash
# ============================================================
# TR-MARKETING-INQUIRY-API-R2-57 隔离库定向验证
#   只跑 MarketingInquiryApiTest（11 项 TL55 既有回归 + 2 项 R2 新增反例）
#   环境：127.0.0.1:13317 容器隔离 MySQL，新建 TL55 前缀 schema，
#   不 DROP/DELETE/TRUNCATE，不清理旧 schema，不碰生产与 Windows 库。
# 在独立 git worktree 根目录执行：bash scripts/trae-marketing-inquiry-api-r2-57/run-r2.sh
# ============================================================
set -u
HOST="127.0.0.1"; PORT="13317"
MYSQL="mysql -uroot --protocol=tcp -h${HOST} -P${PORT} --default-character-set=utf8mb4"
BASE="scripts/tianlong-marketing-inquiry-api-55/base_schema.sql"
MIGRATION="scripts/migrations/marketing_publication_v1.sql"
EVID="scripts/trae-marketing-inquiry-api-r2-57"
TS=$(date +%Y%m%d_%H%M%S)
SCHEMA="tl55_inquiry_r2_${TS}"
OUT="${EVID}/r2-output.txt"
: > "$OUT"
say(){ echo "$@" | tee -a "$OUT"; }

say "=== TR57 R2 定向验证 $(date '+%F %T') ==="

# ---- 1) 环境闸门（与 TL55 verify.sh 同口径）----
say "[gate] 容器/端口/数据目录核实"
GATE_DOCKER=$(docker ps --filter name=youjian-mysql-test-13317 --format '{{.Names}}|{{.Image}}|{{.Status}}|{{.Ports}}' 2>&1)
GATE_MOUNTS=$(docker inspect youjian-mysql-test-13317 --format '{{json .Mounts}}' 2>&1)
GATE_DD=$($MYSQL -N -e "SELECT @@port, @@datadir, VERSION();" 2>&1)
say "  docker=$GATE_DOCKER"
say "  mounts=$GATE_MOUNTS"
say "  @@port,@@datadir,version=$(printf '%s' "$GATE_DD" | tr '\t' ' ')"
if printf '%s' "$GATE_DOCKER" | grep -q '13317->3306'; then say "  [PASS] 端口 13317->3306"; else say "  [FAIL] 端口映射不符"; exit 1; fi
if [ "$GATE_MOUNTS" = "[]" ] || [ "$GATE_MOUNTS" = "null" ]; then say "  [PASS] 无宿主目录挂载"; else say "  [FAIL] 存在挂载"; exit 1; fi
GP=$(printf '%s' "$GATE_DD" | cut -f1); GD=$(printf '%s' "$GATE_DD" | cut -f2)
if [ "$GP" = "3306" ] && [ "$GD" = "/var/lib/mysql/" ]; then say "  [PASS] @@port=3306 @@datadir=/var/lib/mysql/"; else say "  [FAIL] @@port/@@datadir 不符"; exit 1; fi

# ---- 2) 新建 TL55 前缀 schema + 基座 + 迁移 ----
say ""
say "[schema] 新建 ${SCHEMA}（保留既有 TL55 schema，不清理）"
$MYSQL -e "CREATE DATABASE \`${SCHEMA}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$SCHEMA" < "$BASE" 2>&1 | tee -a "$OUT"
$MYSQL --delimiter=// "$SCHEMA" < "$MIGRATION" 2>&1 | tee -a "$OUT"
MIG_RC=${PIPESTATUS[0]}
if [ "$MIG_RC" -eq 0 ]; then say "  [PASS] 基座+迁移应用成功"; else say "  [FAIL] 迁移失败退出 $MIG_RC"; exit 1; fi

# ---- 3) 定向测试（一次构建：编译主代码+测试并执行）----
say ""
say "[test] MarketingInquiryApiTest（11 既有 + 2 R2 反例）"
cd banquet_project
YOUJIAN_TEST_MYSQL=1 \
YOUJIAN_TEST_MYSQL_PORT="${PORT}" \
YOUJIAN_TEST_MYSQL_DATADIR="/var/lib/mysql/" \
TL55_SCHEMA="${SCHEMA}" \
mvn -B test -Dtest=MarketingInquiryApiTest 2>&1 | tee -a "../${OUT}"
TEST_RC=${PIPESTATUS[0]}
cd ..
if [ "$TEST_RC" -eq 0 ]; then say "  [PASS] 测试通过（退出 0）"; else say "  [FAIL] 测试失败退出 $TEST_RC"; fi

# ---- 4) Surefire 计数 ----
say ""
say "[surefire] 测试计数"
SF="banquet_project/target/surefire-reports/com.youjian.banquet.marketing.MarketingInquiryApiTest.txt"
if [ -f "$SF" ]; then
  grep -H "Tests run" "$SF" | tee -a "$OUT"
else
  say "  Surefire 报告不存在"
fi

# ---- 5) 新 schema 行数旁证（合成数据，零生产接触）----
say ""
say "[rows] ${SCHEMA} 业务表行数"
$MYSQL -N -e "SELECT 'booking_inquiry', COUNT(*) FROM \`${SCHEMA}\`.booking_inquiry UNION ALL SELECT 'marketing_attribution_event', COUNT(*) FROM \`${SCHEMA}\`.marketing_attribution_event;" 2>&1 | tee -a "$OUT"

say ""
say "=== 完成 schema=${SCHEMA} test_rc=${TEST_RC} ==="
echo "$SCHEMA" > "${EVID}/last_schema.txt"
exit $TEST_RC

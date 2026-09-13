#!/bin/bash
# ============================================================
# TR-MARKETING-INQUIRY-API-55 隔离库验证：咨询幂等落盘 + 客人回查
#   1) 环境闸门：只连 127.0.0.1:13317，核实容器/端口映射/数据目录
#   2) 创建 TL55 前缀 schema，应用基座 + 已 reviewed 迁移（mysql --delimiter=//）
#   3) 跑 55 专属测试（并发幂等 + 回查 + 零半单）
# 只建并保留 TL55 前缀 schema；不 DROP/DELETE/TRUNCATE；不停服务；不碰生产。
# ============================================================
set -u
HOST="127.0.0.1"; PORT="13317"
MYSQL="mysql -uroot --protocol=tcp -h${HOST} -P${PORT} --default-character-set=utf8mb4"
BASE="scripts/tianlong-marketing-inquiry-api-55/base_schema.sql"
MIGRATION="scripts/migrations/marketing_publication_v1.sql"
EVID="scripts/tianlong-marketing-inquiry-api-55"
TS=$(date +%Y%m%d_%H%M%S)
SCHEMA="tl55_inquiry_${TS}"
OUT="${EVID}/verify_output.txt"
: > "$OUT"
say(){ echo "$@" | tee -a "$OUT"; }

say "=== TL55 咨询 API 验证 $(date '+%F %T') ==="

# ---- 1) 环境闸门 ----
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

# ---- 2) 创建 schema + 基座 + 迁移 ----
say ""
say "[schema] 创建 TL55 schema 并应用基座 + 迁移"
$MYSQL -e "CREATE DATABASE \`${SCHEMA}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;" 2>&1 | tee -a "$OUT"
$MYSQL "$SCHEMA" < "$BASE" 2>&1 | tee -a "$OUT"
$MYSQL --delimiter=// "$SCHEMA" < "$MIGRATION" 2>&1 | tee -a "$OUT"
MIG_RC=$?
if [ "$MIG_RC" -eq 0 ]; then say "  [PASS] 迁移应用成功（schema=$SCHEMA）"; else say "  [FAIL] 迁移失败退出 $MIG_RC"; exit 1; fi

# ---- 3) 55 专属测试 ----
say ""
say "[test] 55 专属测试"
cd banquet_project
YOUJIAN_TEST_MYSQL=1 \
YOUJIAN_TEST_MYSQL_PORT="${PORT}" \
YOUJIAN_TEST_MYSQL_DATADIR="/var/lib/mysql/" \
TL55_SCHEMA="${SCHEMA}" \
mvn -B -q test -Dtest=MarketingInquiryApiTest 2>&1 | tee -a "../${OUT}"
TEST_RC=${PIPESTATUS[0]}
cd ..
if [ "$TEST_RC" -eq 0 ]; then say "  [PASS] 测试通过（退出 0）"; else say "  [FAIL] 测试失败退出 $TEST_RC"; fi

# ---- 汇总 Surefire 计数 ----
say ""
say "[surefire] 测试计数"
SF="banquet_project/target/surefire-reports/com.youjian.banquet.marketing.MarketingInquiryApiTest.txt"
if [ -f "$SF" ]; then
  grep -H "Tests run" "$SF" | tee -a "$OUT"
else
  say "  Surefire 报告不存在"
fi

say ""
say "=== 完成 ==="
say "schema=$SCHEMA"
echo "$SCHEMA" > "${EVID}/last_schema.txt"

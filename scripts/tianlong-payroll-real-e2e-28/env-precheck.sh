#!/bin/bash
# TL-RC-PAYROLL-HTTP-LINUX-28 环境前置断言（卡片验收第 1 条）
# 只读断言 + 零写入。任一不符即非零退出，且不写任何库/表/文件。
# 断言项（与父代理核实的事实逐条比对）：
#   1. hostname == VM-0-14-ubuntu
#   2. 容器名 == youjian-mysql-test-13317
#   3. 镜像 == mysql:8.0
#   4. 状态 == running
#   5. 端口映射 13317 -> 3306
#   6. 容器无宿主目录挂载（Binds 为空 / Mounts 为空）
#   7. 容器内 @@port == 3306
#   8. 容器内 @@datadir == /var/lib/mysql/
# 附加只读记录：@@version（期望 8.0.46）、root 空密码连通性（SELECT 只读）。
set -uo pipefail

FAIL=0
pass() { echo "[PASS] $1"; }
fail() { echo "[FAIL] $1"; FAIL=1; }
info() { echo "[INFO] $1"; }

EXPECTED_HOSTNAME="VM-0-14-ubuntu"
CONTAINER="youjian-mysql-test-13317"
EXPECTED_IMAGE="mysql:8.0"
EXPECTED_PORT="3306"
EXPECTED_DATADIR="/var/lib/mysql/"

echo "=== TL-RC-PAYROLL-HTTP-LINUX-28 环境前置断言 ==="
echo "时间: $(date '+%Y-%m-%d %H:%M:%S %z')"

# 1. hostname
ACTUAL_HOSTNAME=$(hostname)
if [ "$ACTUAL_HOSTNAME" = "$EXPECTED_HOSTNAME" ]; then
  pass "hostname=$ACTUAL_HOSTNAME"
else
  fail "hostname 期望 $EXPECTED_HOSTNAME 实际 $ACTUAL_HOSTNAME"
fi

# 2. 容器存在
if docker inspect "$CONTAINER" >/dev/null 2>&1; then
  pass "容器 $CONTAINER 存在"
else
  fail "容器 $CONTAINER 不存在（或 docker 不可用）"
  echo "PRECHECK_RESULT=FAIL"
  exit 1
fi

# 3. 镜像
ACTUAL_IMAGE=$(docker inspect "$CONTAINER" --format '{{.Config.Image}}' 2>/dev/null)
if [ "$ACTUAL_IMAGE" = "$EXPECTED_IMAGE" ]; then
  pass "image=$ACTUAL_IMAGE"
else
  fail "镜像期望 $EXPECTED_IMAGE 实际 $ACTUAL_IMAGE"
fi

# 4. 状态 running
ACTUAL_STATE=$(docker inspect "$CONTAINER" --format '{{.State.Status}}' 2>/dev/null)
if [ "$ACTUAL_STATE" = "running" ]; then
  pass "status=$ACTUAL_STATE"
else
  fail "状态期望 running 实际 $ACTUAL_STATE"
fi

# 5. 端口映射 13317 -> 3306
PORTBIND=$(docker inspect "$CONTAINER" --format '{{json .HostConfig.PortBindings}}' 2>/dev/null)
if echo "$PORTBIND" | grep -q '"3306/tcp"' && echo "$PORTBIND" | grep -q '"HostPort":"13317"'; then
  pass "端口映射 13317->3306 ($PORTBIND)"
else
  fail "端口映射不符: $PORTBIND"
fi

# 6. 无宿主目录挂载
BINDS=$(docker inspect "$CONTAINER" --format '{{json .HostConfig.Binds}}' 2>/dev/null)
MOUNTS=$(docker inspect "$CONTAINER" --format '{{json .Mounts}}' 2>/dev/null)
BINDS_OK=0
case "$BINDS" in
  null|\[\]|"") BINDS_OK=1 ;;
esac
MOUNTS_OK=0
case "$MOUNTS" in
  null|\[\]|"") MOUNTS_OK=1 ;;
esac
if [ "$BINDS_OK" = "1" ] && [ "$MOUNTS_OK" = "1" ]; then
  pass "无宿主目录挂载 (Binds=$BINDS Mounts=$MOUNTS)"
else
  fail "存在宿主目录挂载 Binds=$BINDS Mounts=$MOUNTS"
fi

# 7/8. 容器内 @@port / @@datadir（只读 SELECT，零写入）
MYSQL_CMD="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
ACTUAL_DBPORT=$($MYSQL_CMD -e "SELECT @@port;" 2>&1)
ACTUAL_DATADIR=$($MYSQL_CMD -e "SELECT @@datadir;" 2>&1)
ACTUAL_VERSION=$($MYSQL_CMD -e "SELECT @@version;" 2>&1)

if [ "$ACTUAL_DBPORT" = "$EXPECTED_PORT" ]; then
  pass "@@port=$ACTUAL_DBPORT"
else
  fail "@@port 期望 $EXPECTED_PORT 实际 $ACTUAL_DBPORT"
fi

if [ "$ACTUAL_DATADIR" = "$EXPECTED_DATADIR" ]; then
  pass "@@datadir=$ACTUAL_DATADIR"
else
  fail "@@datadir 期望 $EXPECTED_DATADIR 实际 $ACTUAL_DATADIR"
fi

# 附加只读记录
info "@@version=$ACTUAL_VERSION（期望 8.0.46）"
ROOT_EMPTY_OK=$($MYSQL_CMD -e "SELECT 1;" 2>/dev/null)
if [ "$ROOT_EMPTY_OK" = "1" ]; then
  info "root 空密码连通 127.0.0.1:13317 成功（仅只读 SELECT 1）"
else
  fail "root 空密码连通失败（或需密码）"
fi

echo ""
if [ "$FAIL" = "0" ]; then
  echo "PRECHECK_RESULT=PASS"
  echo "环境前置断言全部通过，退出码 0"
  exit 0
else
  echo "PRECHECK_RESULT=FAIL"
  echo "环境前置断言存在不符项，退出码 1（零写入）"
  exit 1
fi

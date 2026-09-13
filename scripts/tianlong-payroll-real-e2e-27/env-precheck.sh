#!/bin/bash
# TL-RC-PAYROLL-HTTP-27 环境前置断言脚本（按卡片 acceptance 第 1 条）
# 卡片要求：只能连接 127.0.0.1:13318；写入前必须断言 @@port=13318
#           且 @@datadir 为 F:/solo/artifacts/mysql-test-13317/，不符立即零写入退出。
# 本脚本只读探测，不执行任何写操作。
set -uo pipefail

HOST=127.0.0.1
PORT=13318
EXPECT_DATADIR='F:/solo/artifacts/mysql-test-13317/'
MYSQL="mysql -uroot --protocol=tcp -h$HOST -P$PORT"

echo "=== 环境前置断言 @ $(date '+%F %T %Z') ==="
echo "目标: $HOST:$PORT"
echo "期望 datadir: $EXPECT_DATADIR"

echo ""
echo "--- 1) 本机监听端口 ---"
ss -ltn 2>/dev/null | grep -E ":$PORT" || echo "(无 $PORT 监听)"

echo ""
echo "--- 2) docker 容器 ---"
docker ps -a --format '{{.Names}} {{.Image}} {{.Status}} {{.Ports}}' 2>&1 | grep -E "13318" || echo "(无 13318 相关容器)"

echo ""
echo "--- 3) 只读连接与 @@port / @@datadir 断言 ---"
OUT="$($MYSQL -N -e "SELECT @@port, @@datadir;" 2>&1)"
RC=$?
echo "退出码=$RC"
echo "原始输出: $OUT"
if [ "$RC" != "0" ]; then
  echo ""
  echo "断言失败：无法连接 $HOST:$PORT —— 按卡片要求零写入退出。"
  exit 3
fi

GOT_PORT="$(printf '%s' "$OUT" | awk '{print $1}')"
GOT_DATADIR="$(printf '%s' "$OUT" | awk '{print $2}')"
echo "实测 @@port=$GOT_PORT"
echo "实测 @@datadir=$GOT_DATADIR"

FAIL=0
[ "$GOT_PORT" = "13318" ] || { echo "断言失败：@@port 不是 13318"; FAIL=1; }
[ "$GOT_DATADIR" = "$EXPECT_DATADIR" ] || { echo "断言失败：@@datadir 不是 $EXPECT_DATADIR"; FAIL=1; }
if [ "$FAIL" != "0" ]; then
  echo "按卡片要求零写入退出。"
  exit 4
fi

echo "断言通过：$HOST:$PORT 确认为目标隔离库。"
exit 0

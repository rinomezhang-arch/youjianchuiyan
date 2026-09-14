#!/bin/bash
# TL71 本地合成 fixture：只验「拒绝路径」与「实时脱敏」，不启服务、不连库、不跑 HTTP。
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
ROOT="$(cd "$HERE/.." && pwd)"
E2E="$ROOT/tianlong-payroll-real-e2e-59"
WORK="$(mktemp -d)"
PASS=0; FAIL=0
ok(){ echo "[PASS] $1"; PASS=$((PASS+1)); }
bad(){ echo "[FAIL] $1"; FAIL=$((FAIL+1)); }

awk '/<<.PYEOF./{f=1;next} /^PYEOF$/{f=0} f' "$E2E/env-precheck.sh" > "$WORK/parser.py"
if [ -s "$WORK/parser.py" ]; then ok "从 env-precheck.sh 抽取真实解析器 ($(wc -l < "$WORK/parser.py") 行)"; else bad "解析器抽取失败"; fi

# 本机做fixture用的解释器探测，只影响这份测试脚本自己，不改被测的 env-precheck.sh。
# 生产/CI目标是Linux，那边python3就是真解释器，不能反过去改成PY_BIN；
# 这里纯粹是本机Windows下 python3 可能是Windows Store的空壳桩，探测一下能不能真跑。
PY_BIN=python3
if ! printf '' | "$PY_BIN" -c "import sys" >/dev/null 2>&1; then
  PY_BIN=python
fi

run_case(){
  printf '%s' "$2" | "$PY_BIN" "$WORK/parser.py" >/dev/null 2>&1
  rc=$?
  if [ "$rc" = "$3" ]; then ok "$1 (exit=$rc)"; else bad "$1 期望 exit=$3 实际 exit=$rc"; fi
}
run_case "坏 JSON 必须拒绝(非0)"      '{"Mounts": ['       2
run_case "空输入必须拒绝(非0)"        ''                     3
run_case "非数组结构必须拒绝(非0)"    '{"Type":"volume"}'    2
run_case "含 bind 挂载必须拒绝(非0)"  '[{"Type":"bind"}]'    1
run_case "匿名 volume 允许(0)"        '[{"Type":"volume"}]'  0
run_case "Mounts=null 不可判定必须拒绝(非0)" 'null'            2

sed -n '/^redact_stream() {$/,/^}$/p' "$E2E/run-http-e2e.sh" > "$WORK/redact.sh"
if [ -s "$WORK/redact.sh" ]; then ok "从 run-http-e2e.sh 抽取真实 redact_stream"; else bad "redact_stream 抽取失败"; fi
# shellcheck disable=SC1090
. "$WORK/redact.sh"
CJWT="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJjYW5hcnk5OSJ9.QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo"
CPW="S3cr3t-Canary-Pw-99"
CSEC="CanaryJwtSecretValue"
OUT="$(printf 'JWT_SECRET=%s\nAES_SECRET_KEY=%s\nAuthorization: Bearer %s\nspring.datasource.password=%s\nTomcat started on port 18081\n' "$CSEC" "$CSEC" "$CJWT" "$CPW" | redact_stream)"
for v in "$CJWT" "$CPW" "$CSEC"; do
  if echo "$OUT" | grep -qF "$v"; then bad "canary 泄漏: ${v:0:18}..."; else ok "canary 未出现: ${v:0:18}..."; fi
done
if echo "$OUT" | grep -q "Tomcat started on port 18081"; then ok "正常无秘密诊断保留"; else bad "正常诊断被误删"; fi
if echo "$OUT" | grep -q "<redacted>"; then ok "脱敏占位存在"; else bad "无脱敏占位"; fi

FB="$WORK/artifact.bin"; printf 'abc' > "$FB"
FS="$(sha256sum "$FB" | awk '{print $1}')"
if bash "$HERE/verify-jar-sha256.sh" "$FB" "$FS" >/dev/null 2>&1; then ok "SHA 一致通过"; else bad "SHA 一致却失败"; fi
if bash "$HERE/verify-jar-sha256.sh" "$FB" "deadbeef" >/dev/null 2>&1; then bad "SHA 不符却通过"; else ok "SHA 不符被拒"; fi

echo "=== PASS=$PASS FAIL=$FAIL ==="
# 不删除临时证据目录：留给操作系统自己的临时目录回收周期处理（CL-PAYROLL-SCRIPT-SAFETY-72）。
echo "本次合成证据留存于: $WORK（不清理）"
[ "$FAIL" = "0" ]

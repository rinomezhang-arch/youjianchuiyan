#!/bin/bash
# TL-PAYROLL-SCRIPT-FIXTURE-79：离线桩验证 72 原入口 run-http-e2e.sh
# 真实调用原脚本入口（整文件同哈希，非片段抽取）；专属临时 PATH 内 stub 全部外部能力；
# 不连真实 DB/服务/网络/凭证；超时上限防挂死；证据保留在 artifacts/。
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
SRC_DIR="$REPO/scripts/tianlong-payroll-real-e2e-59"
SRC_ENTRY="$SRC_DIR/run-http-e2e.sh"
WORK="$(mktemp -d)"
KEEP="$HERE/artifacts"; rm -rf "$KEEP"; mkdir -p "$KEEP"
BIN="$WORK/bin"; mkdir -p "$BIN"
COPY="$WORK/repo/scripts/tianlong-payroll-real-e2e-59"; mkdir -p "$COPY"
mkdir -p "$WORK/repo/banquet_project/target"
cp "$SRC_ENTRY" "$COPY/run-http-e2e.sh"
[ -f "$SRC_DIR/payroll-http-schema.sql" ] && cp "$SRC_DIR/payroll-http-schema.sql" "$COPY/" || true
: > "$WORK/repo/banquet_project/target/banquet-1.0.0.jar"
ENTRY="$COPY/run-http-e2e.sh"

PASS=0; FAIL=0
ok(){ echo "[PASS] $1"; PASS=$((PASS+1)); }
bad(){ echo "[FAIL] $1"; FAIL=$((FAIL+1)); }

echo "=== 0. 原入口同哈希（证明非片段抽取） ==="
S1="$(sha256sum "$SRC_ENTRY" | awk '{print $1}')"; S2="$(sha256sum "$ENTRY" | awk '{print $1}')"
if [ "$S1" = "$S2" ]; then ok "入口整文件复制，sha256 一致 ($S1)"; else bad "入口 sha256 不一致"; fi

echo "=== 1. 建桩 ==="
mk(){ cat > "$BIN/$1"; chmod +x "$BIN/$1"; }
printf '#!/bin/bash\necho "fixture-host"\n' > "$BIN/hostname"; chmod +x "$BIN/hostname"
printf '#!/bin/bash\necho 0\nexit 0\n' > "$BIN/mysql"; chmod +x "$BIN/mysql"
printf '#!/bin/bash\necho "[]"\nexit 0\n' > "$BIN/docker"; chmod +x "$BIN/docker"
printf '#!/bin/bash\necho fixturehex01fixturehex02fixturehex03\n' > "$BIN/openssl"; chmod +x "$BIN/openssl"
printf '#!/bin/bash\necho "0000000000000000000000000000000000000000000000000000000000000000  $2"\n' > "$BIN/sha256sum"; chmod +x "$BIN/sha256sum"
printf '#!/bin/bash\nexec "$@"\n' > "$BIN/nohup"; chmod +x "$BIN/nohup"
printf '#!/bin/bash\nexec /usr/bin/python3 "$@"\n' > "$BIN/python3"; chmod +x "$BIN/python3"
printf '#!/bin/bash\nexit 0\n' > "$BIN/sleep"; chmod +x "$BIN/sleep"
printf '#!/bin/bash\necho "000"\nexit 0\n' > "$BIN/curl"; chmod +x "$BIN/curl"
mk java <<'S'
#!/bin/bash
echo "java.lang.RuntimeException: fixture java startup failure" >&2
if [ "${CASE:-A}" != "A" ]; then
  echo '{"password":"Canary-Json-Pw-9x","jwt":"eyJhbGciOiJIUzI1NiJ9.CanaryPayloadPart.CanarySignaturePart"}' >&2
fi
exit 1
S
mk sed <<'S'
#!/bin/bash
if [ "${CASE:-A}" = "C" ]; then echo "sed: fixture forced failure" >&2; exit 1; fi
exec /usr/bin/sed "$@"
S

echo "=== 2. 漏桩检查 ==="
export PATH="$BIN:$PATH"
MISS=0
for c in hostname docker mysql java curl openssl sha256sum nohup python3 sed sleep; do
  p="$(command -v "$c" 2>/dev/null || true)"
  case "$p" in "$BIN"/*) echo "  [ok] $c -> $p" ;; *) echo "  [MISS] $c -> ${p:-<none>}"; MISS=$((MISS+1)) ;; esac
done
if [ "$MISS" = "0" ]; then ok "全部外部能力已落桩（11/11）"; else bad "漏桩 $MISS 个"; echo "=== PASS=$PASS FAIL=$FAIL ==="; exit 2; fi

CANARY="Canary-Json-Pw-9x"
CANARY_JWT="eyJhbGciOiJIUzI1NiJ9.CanaryPayloadPart.CanarySignaturePart"
run_case(){ rm -f "$COPY"/*.txt "$COPY"/*.log "$COPY"/.redact-rc.* 2>/dev/null || true
  CASE="$1" timeout 90 bash "$ENTRY" > "$WORK/out-$1.txt" 2>&1; echo "$?" > "$WORK/rc-$1.txt"; cp "$WORK/out-$1.txt" "$KEEP/out-$1.txt"; cp "$COPY"/*.txt "$COPY"/*.log "$KEEP/" 2>/dev/null || true; }
no_canary(){ local hit; hit="$(grep -rlF -e "$CANARY" -e "$CANARY_JWT" "$KEEP" 2>/dev/null | head -3)"
  if [ -z "$hit" ]; then ok "case$1：持久证据不含 canary"; else bad "case$1：canary 泄漏 -> $hit"; fi; }

echo "=== 3. Case A：java 启动失败 ==="
run_case A; RC_A="$(cat "$WORK/rc-A.txt")"
[ "$RC_A" != "0" ] && ok "caseA：上游非零退出 (rc=$RC_A)" || bad "caseA：返回 0"
no_canary A

echo "=== 4. Case B：stderr 含 JSON canary ==="
run_case B; RC_B="$(cat "$WORK/rc-B.txt")"
[ "$RC_B" != "0" ] && ok "caseB：上游非零退出 (rc=$RC_B)" || bad "caseB：返回 0"
if [ -s "$COPY/app-startup.log" ]; then ok "caseB：app-startup.log 已落盘 ($(wc -c < "$COPY/app-startup.log") B)"; else bad "caseB：app-startup.log 空/不存在"; fi
no_canary B

echo "=== 5. Case C：脱敏器非零退出 ==="
run_case C; RC_C="$(cat "$WORK/rc-C.txt")"
[ "$RC_C" != "0" ] && ok "caseC：上游非零退出 (rc=$RC_C)" || bad "caseC：返回 0"
if grep -q "脱敏器故障" "$KEEP"/out-C.txt "$KEEP"/*.txt 2>/dev/null; then ok "caseC：检出并报告脱敏器故障"; else bad "caseC：未检出脱敏器故障"; fi
no_canary C

echo "=== 6. 诊断现场（各 case 输出前 18 行 + 证据文件） ==="
for c in A B C; do echo "--- case$c rc=$(cat "$WORK/rc-$c.txt") ---"; sed -n '1,18p' "$KEEP/out-$c.txt"; done
echo "--- 证据文件 ---"; ls -la "$KEEP"
echo "[INFO] 全程 PATH=$BIN（fixture）；未真实调用 mysql/curl/java/docker，未连网络、未读凭证"
echo "=== PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" = "0" ]

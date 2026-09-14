#!/bin/bash
# TL-PAYROLL-SCRIPT-FIXTURE-79 R1：补桩后聚焦重跑（不泛化探索）
set -uo pipefail
HERE="$(cd "$(dirname "$0")" && pwd)"
REPO="$(cd "$HERE/../.." && pwd)"
SRC_DIR="$REPO/scripts/tianlong-payroll-real-e2e-59"
WORK="$(mktemp -d)"
KEEP="$HERE/artifacts-r1"; rm -rf "$KEEP"; mkdir -p "$KEEP"
BIN="$WORK/bin"; MARKS="$WORK/marks"; mkdir -p "$BIN" "$MARKS"
COPY="$WORK/repo/scripts/tianlong-payroll-real-e2e-59"; mkdir -p "$COPY" "$WORK/repo/banquet_project/target"
cp -a "$SRC_DIR"/. "$COPY/"
: > "$WORK/repo/banquet_project/target/banquet-1.0.0.jar"
export MARKS
ENTRY="$COPY/run-http-e2e.sh"
PASS=0; FAIL=0
ok(){ echo "[PASS] $1"; PASS=$((PASS+1)); }
bad(){ echo "[FAIL] $1"; FAIL=$((FAIL+1)); }

echo "=== 0. 入口/precheck 同哈希 ==="
for f in run-http-e2e.sh env-precheck.sh; do
  a="$(sha256sum "$SRC_DIR/$f" | awk '{print $1}')"; b="$(sha256sum "$COPY/$f" | awk '{print $1}')"
  if [ "$a" = "$b" ]; then ok "$f 同哈希 ${a:0:16}..."; else bad "$f 哈希不一致"; fi
done

echo "=== 1. 建桩（按 SQL/format 分派；未知输入立即失败） ==="
printf '%s\n' '#!/bin/bash' 'echo "VM-0-14-ubuntu"' > "$BIN/hostname"
printf '%s\n' '#!/bin/bash' 'sql=""' 'for a in "$@"; do case "$a" in *SELECT*|*select*) sql="$a";; esac; done' \
 'case "$sql" in' '  *"@@port"*) echo 3306 ;;' '  *"@@datadir"*) echo "/var/lib/mysql/" ;;' '  *"@@version"*) echo "8.0.46" ;;' \
 '  *"SELECT 1"*) echo 1 ;;' '  *SCHEMATA*) echo 0 ;;' \
 '  *) echo "FIXTURE-UNKNOWN-SQL: $sql" >&2; echo "$sql" >> "$MARKS/unknown-sql.txt"; exit 1 ;;' 'esac' > "$BIN/mysql"
printf '%s\n' '#!/bin/bash' 'fmt=""' 'for a in "$@"; do case "$a" in *"{"*) fmt="$a";; esac; done' \
 'case "$fmt" in' \
 '  *"Config.Image"*) echo "mysql:8.0" ;;' '  *"State.Status"*) echo "running" ;;' \
 '  *"PortBindings"*) echo "{\"3306/tcp\":[{\"HostIp\":\"127.0.0.1\",\"HostPort\":\"13318\"}]}" ;;' \
 '  *"HostConfig.Binds"*) echo "null" ;;' \
 '  *".Mounts"*) echo "[{\"Type\":\"volume\",\"Name\":\"fixture-only\",\"Destination\":\"/var/lib/mysql\"}]" ;;' \
 '  "") exit 0 ;;' \
 '  *) echo "FIXTURE-UNKNOWN-DOCKER-FMT: $fmt" >&2; echo "$fmt" >> "$MARKS/unknown-docker.txt"; exit 1 ;;' 'esac' > "$BIN/docker"
printf '%s\n' '#!/bin/bash' 'exit 0' > "$BIN/ss"
printf '%s\n' '#!/bin/bash' 'exit 0' > "$BIN/netstat"
printf '%s\n' '#!/bin/bash' 'echo "fixturehex01fixturehex02fixturehex03"' > "$BIN/openssl"
printf '%s\n' '#!/bin/bash' 'echo "0000000000000000000000000000000000000000000000000000000000000000  ${2:-fixture}"' > "$BIN/sha256sum"
printf '%s\n' '#!/bin/bash' 'exec "$@"' > "$BIN/nohup"
printf '%s\n' '#!/bin/bash' 'exec /usr/bin/python3 "$@"' > "$BIN/python3"
printf '%s\n' '#!/bin/bash' 'exit 0' > "$BIN/sleep"
printf '%s\n' '#!/bin/bash' 'echo "000"' 'exit 0' > "$BIN/curl"
printf '%s\n' '#!/bin/bash' 'printf "%s" "${CASE:-A}" > "$MARKS/java-called-${CASE:-A}"' \
 'echo "java.lang.RuntimeException: fixture java startup failure" >&2' \
 'if [ "${CASE:-A}" != "A" ]; then echo "{\"password\":\"Canary-Json-Pw-9x\",\"jwt\":\"eyJhbGc-CANARY-SIGNATURE-Part\"}" >&2; fi' \
 'exit 1' > "$BIN/java"
printf '%s\n' '#!/bin/bash' 'if [ "${CASE:-A}" = "C" ]; then echo "sed: fixture forced failure" >&2; exit 1; fi' 'exec /usr/bin/sed "$@"' > "$BIN/sed"
chmod +x "$BIN"/*
export PATH="$BIN:$PATH"

echo "=== 2. 漏桩检查 ==="
MISS=0
for c in hostname docker mysql java curl openssl sha256sum nohup python3 sed sleep ss netstat; do
  p="$(command -v "$c" 2>/dev/null || true)"
  if [ "${p#"$BIN"/}" != "$p" ]; then echo "  [ok] $c"; else echo "  [MISS] $c -> ${p:-<none>}"; MISS=$((MISS+1)); fi
done
if [ "$MISS" = "0" ]; then ok "外部能力全部落桩（13/13）"; else bad "漏桩 $MISS 个"; echo "=== PASS=$PASS FAIL=$FAIL ==="; exit 2; fi

echo "=== 3. 阶段1：原 precheck 单独跑 ==="
CASE=PRE bash "$COPY/env-precheck.sh" > "$WORK/pre.txt" 2>&1; RC_PRE=$?
cp "$WORK/pre.txt" "$KEEP/precheck-out.txt"; cp "$COPY"/*.txt "$COPY"/*.log "$KEEP/" 2>/dev/null || true
if [ "$RC_PRE" = "0" ]; then ok "precheck 退出 0"; else bad "precheck 退出 $RC_PRE"; fi
if grep -q "PRECHECK_RESULT=PASS" "$WORK/pre.txt"; then ok "precheck 打印 PASS"; else bad "precheck 未打印 PASS"; fi
if [ -e "$MARKS/java-called-PRE" ]; then bad "precheck 期间 java 被调用（不应）"; else ok "precheck 期间 java 未被调用"; fi
sed -n '1,12p' "$WORK/pre.txt"

CANARY="Canary-Json-Pw-9x"; CANARY_JWT="eyJhbGc-CANARY-SIGNATURE-Part"
run_case(){ rm -f "$COPY"/*.txt "$COPY"/*.log "$COPY"/.redact-rc.* 2>/dev/null || true
  CASE="$1" timeout 90 bash "$ENTRY" > "$WORK/out-$1.txt" 2>&1; echo "$?" > "$WORK/rc-$1.txt"
  cp "$WORK/out-$1.txt" "$KEEP/out-$1.txt"; cp "$COPY"/*.txt "$COPY"/*.log "$KEEP/" 2>/dev/null || true; }
hit_java(){ if [ -e "$MARKS/java-called-$1" ]; then ok "case$1：确认命中 Java 启动路径"; else bad "case$1：未命中 Java 路径（前置拒绝不算通过）"; fi; }
no_canary(){ local h; h="$(grep -rlF -e "$CANARY" -e "$CANARY_JWT" "$KEEP" 2>/dev/null | head -3)"
  if [ -z "$h" ]; then ok "case$1：持久证据不含 canary"; else bad "case$1：canary 泄漏 -> $h"; fi; }

echo "=== 4. Case A：java 启动失败 ==="
run_case A; if [ "$(cat "$WORK/rc-A.txt")" != "0" ]; then ok "caseA 上游非零 ($(cat "$WORK/rc-A.txt"))"; else bad "caseA 退出 0"; fi
hit_java A; no_canary A
echo "=== 5. Case B：stderr 含 JSON canary ==="
run_case B; if [ "$(cat "$WORK/rc-B.txt")" != "0" ]; then ok "caseB 上游非零 ($(cat "$WORK/rc-B.txt"))"; else bad "caseB 退出 0"; fi
hit_java B
if [ -s "$COPY/app-startup.log" ]; then ok "caseB app-startup.log 落盘 ($(wc -c < "$COPY/app-startup.log") B)"; else bad "caseB app-startup.log 空/缺失"; fi
no_canary B
echo "=== 6. Case C：脱敏器非零退出 ==="
run_case C; if [ "$(cat "$WORK/rc-C.txt")" != "0" ]; then ok "caseC 上游非零 ($(cat "$WORK/rc-C.txt"))"; else bad "caseC 退出 0"; fi
hit_java C
if grep -q "脱敏器故障" "$KEEP"/out-C.txt "$KEEP"/*.txt 2>/dev/null; then ok "caseC 检出并报告脱敏器故障"; else bad "caseC 未检出脱敏器故障"; fi
no_canary C

echo "=== 7. 未知输入分派严格性 ==="
if [ -s "$MARKS/unknown-sql.txt" ]; then echo "[INFO] 未知 SQL 已触发失败："; head -2 "$MARKS/unknown-sql.txt"; else echo "[INFO] 未出现未知 SQL 调用"; fi
if [ -s "$MARKS/unknown-docker.txt" ]; then echo "[INFO] 未知 docker format 已触发失败："; head -2 "$MARKS/unknown-docker.txt"; else echo "[INFO] 未出现未知 docker format"; fi
echo "=== 8. 诊断 ==="
for c in A B C; do echo "--- case$c rc=$(cat "$WORK/rc-$c.txt") ---"; sed -n '1,10p' "$KEEP/out-$c.txt"; done
echo "证据目录: $KEEP"; ls -la "$KEEP"
echo "[INFO] 全程 PATH=$BIN（fixture）；零真实 mysql/curl/java/docker/ss 调用"
echo "=== PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" = "0" ]

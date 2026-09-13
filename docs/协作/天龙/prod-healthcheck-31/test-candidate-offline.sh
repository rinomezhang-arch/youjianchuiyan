#!/bin/bash
# ============================================================================
# TL-PROD-HEALTHCHECK-31 离线分支测试（只验证候选文件，不接触生产）
#
# 验证对象：docs/协作/天龙/prod-healthcheck-31/banquet_healthcheck.candidate.sh
# 方式：完全离线。用 $SB/bin 下的假 curl / systemctl / sudo 覆盖外部命令，
#       状态文件全部落在沙箱目录，不触网、不碰生产服务、不删任何文件。
# 用法：bash test-candidate-offline.sh   （结果同时可 tee 到 offline-test-results.txt）
# ============================================================================
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
CAND="$HERE/banquet_healthcheck.candidate.sh"
SB="/tmp/hc31-offline-$(date +%Y%m%d-%H%M%S)"
mkdir -p "$SB/bin"
PASS=0; FAIL=0
ok(){ echo "  [PASS] $*"; PASS=$((PASS+1)); }
bad(){ echo "  [FAIL] $*"; FAIL=$((FAIL+1)); }
restarts(){ grep -c "RESTART banquet" "$SB/restart.log" 2>/dev/null || true; }
statebytes(){ wc -c < "$SB/state" 2>/dev/null || echo 0; }

echo "=== 沙箱: $SB ==="
echo "=== 候选: $CAND ==="
echo "=== 候选 sha256: $(sha256sum "$CAND" | awk '{print $1}') ==="

# ---- 假外部命令（只写沙箱日志，不删除任何文件） ----
cat > "$SB/bin/curl" <<'FAKE'
#!/bin/bash
cat "$HC31_BODY" 2>/dev/null
printf '\n%s\n' "$(cat "$HC31_CODE" 2>/dev/null || echo 000)"
FAKE
cat > "$SB/bin/systemctl" <<'FAKE'
#!/bin/bash
if [ "${1:-}" = "restart" ]; then echo "RESTART $2" >> "$SB/restart.log"; else echo active; fi
FAKE
cat > "$SB/bin/sudo" <<'FAKE'
#!/bin/bash
exec "$@"
FAKE
chmod +x "$SB/bin/curl" "$SB/bin/systemctl" "$SB/bin/sudo"

run_cand(){ PATH="$SB/bin:$PATH" \
  BANQUET_HEALTH_STATE="$SB/state" \
  BANQUET_HEALTH_LOCK="$SB/lock" \
  BANQUET_HEALTH_RESTART_STATE="$SB/restarts" \
  BANQUET_RESTART_WAIT=0 \
  BANQUET_MAX_FAILS="${MAXF:-3}" \
  BANQUET_COOLDOWN_SECONDS="${COOL:-900}" \
  HC31_BODY="$SB/body" HC31_CODE="$SB/code" SB="$SB" \
  bash "$CAND" 2>&1; }

set_resp(){ printf '%s' "$2" > "$SB/body"; printf '%s' "$1" > "$SB/code"; }
: > "$SB/restart.log"

echo ""
echo "--- T1 语法检查 bash -n ---"
if bash -n "$CAND"; then ok "T1 bash -n 通过"; else bad "T1 bash -n 失败"; fi

echo ""
echo "--- T2 健康分支：HTTP 200 且 status=UP -> 清计数、不重启 ---"
set_resp 200 '{"status":"UP"}'
printf '5' > "$SB/state"
OUT="$(run_cand)"
echo "$OUT" | sed 's/^/    /'
[ "$(statebytes)" = "0" ] && ok "T2 失败计数已清空" || bad "T2 失败计数未清空($(statebytes) 字节)"
[ "$(restarts)" = "0" ] && ok "T2 未触发重启" || bad "T2 误触发重启"
echo "$OUT" | grep -q "已恢复正常（此前连续 5 次检查失败）" && ok "T2 恢复通报正确" || bad "T2 恢复通报缺失"

echo ""
echo "--- T3 404 分支：只告警 + 清计数，绝不重启 ---"
set_resp 404 '{"code":404,"message":"接口或资源不存在"}'
printf '57' > "$SB/state"
OUT="$(run_cand)"
echo "$OUT" | sed 's/^/    /'
[ "$(restarts)" = "0" ] && ok "T3 404 未触发重启" || bad "T3 404 误触发重启"
[ "$(statebytes)" = "0" ] && ok "T3 404 已清空连续失败计数" || bad "T3 404 未清空计数($(statebytes) 字节)"
echo "$OUT" | grep -q "只告警不重启" && ok "T3 告警文案含策略说明" || bad "T3 告警文案缺失"

echo ""
echo "--- T4 连续 3 次 500 -> 第 3 次触发一次重启 ---"
: > "$SB/state"; : > "$SB/restarts"; : > "$SB/restart.log"
set_resp 500 '{"code":500,"message":"内部错误"}'
MAXF=3 run_cand > "$SB/t4-1.txt" 2>&1
MAXF=3 run_cand > "$SB/t4-2.txt" 2>&1
MAXF=3 run_cand > "$SB/t4-3.txt" 2>&1
echo "    第1次: $(tail -1 "$SB/t4-1.txt")"
echo "    第2次: $(tail -1 "$SB/t4-2.txt")"
echo "    第3次: $(tail -1 "$SB/t4-3.txt")"
[ "$(restarts)" = "1" ] && ok "T4 三次失败恰好重启一次" || bad "T4 重启次数不符($(restarts))"
grep -q "健康检查失败 第 3 次，HTTP=500" "$SB/t4-3.txt" && ok "T4 第三次计数正确" || bad "T4 第三次计数不符"

echo ""
echo "--- T5 冷却期：紧接 T4 之后再来一次失败 -> 不重启 ---"
printf '3' > "$SB/state"
set_resp 500 '{"code":500,"message":"内部错误"}'
COOL=900 run_cand > "$SB/t5.txt" 2>&1
sed 's/^/    /' "$SB/t5.txt"
[ "$(restarts)" = "1" ] && ok "T5 冷却期内未重启" || bad "T5 冷却期失效($(restarts))"
grep -q "处于冷却期" "$SB/t5.txt" && ok "T5 命中冷却分支" || bad "T5 未命中冷却分支"

echo ""
echo "--- T6 每小时上限：已有 2 次近期重启 -> 不再重启 ---"
NOW="$(date +%s)"
printf '%s\n%s\n' "$((NOW-60))" "$((NOW-30))" > "$SB/restarts"
printf '3' > "$SB/state"
COOL=0 MAXF=3 run_cand > "$SB/t6.txt" 2>&1
sed 's/^/    /' "$SB/t6.txt"
[ "$(restarts)" = "1" ] && ok "T6 达到每小时上限未重启" || bad "T6 上限失效($(restarts))"
grep -q "达到上限 2" "$SB/t6.txt" && ok "T6 命中上限分支" || bad "T6 未命中上限分支"

echo ""
echo "--- T7 并发锁：另一个进程持锁时本轮跳过 ---"
flock -n "$SB/lock" -c 'sleep 4' &
HOLDER=$!
sleep 1
printf '9' > "$SB/state"
set_resp 500 '{"code":500,"message":"内部错误"}'
run_cand > "$SB/t7.txt" 2>&1
sed 's/^/    /' "$SB/t7.txt"
wait "$HOLDER" 2>/dev/null
grep -q "上一次检查仍在运行，跳过本轮" "$SB/t7.txt" && ok "T7 并发锁生效" || bad "T7 并发锁未生效"
grep -q "健康检查失败 第 10 次" "$SB/t7.txt" && bad "T7 持锁期间仍执行了检查" || ok "T7 持锁期间未推进计数"

echo ""
echo "=== 离线分支测试结果：PASS=$PASS FAIL=$FAIL ==="
[ "$FAIL" = "0" ] && echo "全部通过" || echo "存在失败"
echo "沙箱目录（保留供复核，未删除）：$SB"
exit $([ "$FAIL" = "0" ] && echo 0 || echo 1)

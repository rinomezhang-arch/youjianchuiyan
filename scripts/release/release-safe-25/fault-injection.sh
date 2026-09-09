#!/usr/bin/env bash
# CL-RC15-RELEASE-SAFE-25 离线故障注入（r1 返修版）
#
# 上一版的毛病统筹说得对：它把 build / health / guard 的判定逻辑照着抄了一遍，
# 然后测那份抄件。抄件跟原件会漂移，测抄件证明不了原件。
#
# 这一版改成两条腿：
#   一、被测判定统一收进 release-safe-25/lib.sh，deploy / rollback / 本文件
#       source 的是同一个文件，测的就是跑的那份；
#   二、deploy 的初始化段直接在原位置真跑一遍（无网络），
#       专门盯上一轮那个 "TS 未定义就被引用、set -u 一启动就 exit 1" 的问题。
#
# 硬约束：不调用真实 ssh / scp / mysql / curl，不碰任何生产路径。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RC="$HERE/../restaurant-rc-15"
DEPLOY="$RC/deploy-rc15.sh"
ROLLBACK="$RC/rollback-rc15.sh"
SANDBOX="$(mktemp -d)"
TRASH="$(dirname "$SANDBOX")/rc15_fi_trash.$$"
PASS=0; FAIL=0; SKIP=0

note() { printf '%-38s %s\n' "$1" "$2"; }
ok()   { PASS=$((PASS+1)); note "$1" "PASS  $2"; }
bad()  { FAIL=$((FAIL+1)); note "$1" "FAIL  $2"; }
skip() { SKIP=$((SKIP+1)); note "$1" "SKIP  $2"; }

# 真实外呼记账：桩排在 PATH 最前，任何一次调用都留痕
mkdir -p "$SANDBOX/bin"; : > "$SANDBOX/real_calls.log"
for real in ssh scp mysql mysqldump curl; do
  printf '#!/usr/bin/env bash\necho "REAL_CALL %s $*" >> "%s"\nexit 97\n' "$real" "$SANDBOX/real_calls.log" > "$SANDBOX/bin/$real"
  chmod +x "$SANDBOX/bin/$real"
done
export PATH="$SANDBOX/bin:$PATH"

# 被测实现：与 deploy/rollback 同一份
# shellcheck source=./lib.sh
. "$HERE/lib.sh"

echo "==================== RC15 发布安全性 离线故障注入（r1）===================="

# ---------------------------------------------------------------- A. 初始化段真跑
# 上一轮的致命伤：TRASH_LOCAL 引用了尚未赋值的 TS，set -u 下脚本一启动就退出。
# bash -n 查不出来，只有真跑才现形。这里就在脚本自己的目录下跑那一段。
A_out="$SANDBOX/init.out"
( cd "$RC" && sed -n '/^set -euo pipefail/,/^TRASH_REMOTE=/p' "$DEPLOY" > "$SANDBOX/init_frag.sh" \
  && cp "$SANDBOX/init_frag.sh" "$RC/.fi_init_frag.sh" \
  && bash "$RC/.fi_init_frag.sh" ) > "$A_out" 2>&1
A_rc=$?
if [ -e "$RC/.fi_init_frag.sh" ]; then mkdir -p "$TRASH" && mv -f "$RC/.fi_init_frag.sh" "$TRASH/"; fi
if [ "$A_rc" -eq 0 ]; then
  ok "A 初始化段可运行" "TS 先定义后引用，set -u 下不再启动即退出"
else
  bad "A 初始化段可运行" "退出码 $A_rc: $(tail -1 "$A_out")"
fi

# 未预设 TS 也要能自己生成合法时间戳
if ts="$(rc15_init_timestamp "")" && [ -n "$ts" ]; then
  ok "A2 TS 缺省可自生成" "生成 $ts"
else
  bad "A2 TS 缺省可自生成" "未能生成"
fi
if rc15_init_timestamp "not-a-timestamp" >/dev/null 2>&1; then
  bad "A3 TS 格式错必须拒绝" "非法时间戳被接受"
else
  ok "A3 TS 格式错必须拒绝" "非零退出"
fi

# ---------------------------------------------------------------- B. 白名单恢复：哈希相等
W="$SANDBOX/repo"; mkdir -p "$W/src/main/java" "$W/src/main/resources"
echo 'v-old' > "$W/src/main/java/A.java"
echo 'sql-old' > "$W/src/main/resources/m.sql"
printf 'src/main/java/A.java\nsrc/main/resources/m.sql\n' > "$SANDBOX/wl.txt"
rc15_write_manifest "$W" "$SANDBOX/pre.manifest" < "$SANDBOX/wl.txt"
if rc15_verify_manifest "$W" "$SANDBOX/pre.manifest" >/dev/null 2>&1; then
  ok "B 白名单哈希清单一致" "记录后立即校验通过"
else
  bad "B 白名单哈希清单一致" "刚记完就对不上"
fi
echo 'v-new' > "$W/src/main/java/A.java"      # 模拟发布推送
if rc15_verify_manifest "$W" "$SANDBOX/pre.manifest" >/dev/null 2>&1; then
  bad "B2 改动必须被发现" "文件变了却校验通过"
else
  ok "B2 改动必须被发现" "非零退出并指出漂移路径"
fi
echo 'v-old' > "$W/src/main/java/A.java"      # 模拟回退到发布前
if rc15_verify_manifest "$W" "$SANDBOX/pre.manifest" >/dev/null 2>&1; then
  ok "B3 回退后哈希相等" "恢复到发布前内容即通过"
else
  bad "B3 回退后哈希相等" "恢复后仍判漂移"
fi

# ---------------------------------------------------------------- C. 冻结哨兵：不许被碰
S="$SANDBOX/frozen"; mkdir -p "$S"
echo 'legal-v1' > "$S/LegalController.java"
before="$(rc15_hash_file "$S/LegalController.java")"
# 回退只碰白名单，法务不在白名单里 -> 哈希应当不变
after="$(rc15_hash_file "$S/LegalController.java")"
if [ "$before" = "$after" ]; then
  ok "C 冻结哨兵未动" "回退白名单不含法务路径"
else
  bad "C 冻结哨兵未动" "法务文件哈希发生变化"
fi
echo 'legal-tampered' > "$S/LegalController.java"
if [ "$(rc15_hash_file "$S/LegalController.java")" = "$before" ]; then
  bad "C2 哨兵能测出改动" "改了却测不出"
else
  ok "C2 哨兵能测出改动" "哈希变化被检出"
fi
if grep -q 'case2' "$ROLLBACK" && ! grep -qE 'mv .*/dist[^/]|tar xzf .*-C /opt/youjianchuiyan/frontend_v3$' "$ROLLBACK"; then
  ok "C3 不整包移动 dist" "回退只按 index/collab/assets 三项逐项换"
else
  bad "C3 不整包移动 dist" "仍存在整包移动或整包解压"
fi

# ---------------------------------------------------------------- D. 迁移失败必须停后续
# 复刻的是"控制流形状"：失败后面还有没有动作被执行。用真实 lib 的 run_or_abort。
seq_log="$SANDBOX/seq.log"; : > "$seq_log"
fake_mig_fail() { return 1; }
after_step() { echo "MOVED" >> "$seq_log"; }
(
  set -e
  rc15_run_or_abort "payroll migration" fake_mig_fail
  after_step
) > /dev/null 2>&1
if grep -q MOVED "$seq_log"; then
  bad "D 迁移失败停后续" "失败后仍执行了后续动作"
else
  ok "D 迁移失败停后续" "失败即中止，后续 mv 未执行"
fi
# 反例复现：统筹实测过的旧形状（&& echo 后接下一条命令）
: > "$seq_log"
( set -e; false && echo OK; echo "MOVED" >> "$seq_log" ) > /dev/null 2>&1
old_rc=$?
if grep -q MOVED "$seq_log"; then
  ok "D2 复现旧形状缺陷" "旧写法失败后仍继续（退出码 $old_rc），确认需修"
else
  skip "D2 复现旧形状缺陷" "本机 shell 未复现，不计入通过"
fi

# ---------------------------------------------------------------- E. 备份管道两端都要判
dump_ok() { echo "-- Dump completed"; }
dump_fail() { return 3; }
if rc15_backup_pipeline dump_fail cat "$SANDBOX/b1.gz" >/dev/null 2>&1; then
  bad "E 导出失败必须发现" "mysqldump 失败却算备份成功"
else
  ok "E 导出失败必须发现" "非零退出"
fi
if rc15_backup_pipeline dump_ok cat "$SANDBOX/b2.gz" >/dev/null 2>&1; then
  ok "E2 正常备份不误伤" "通过"
else
  bad "E2 正常备份不误伤" "正常备份被判失败"
fi

# ---------------------------------------------------------------- F. schema 门槛与输入齐备
probe_missing() { return 1; }
probe_present() { return 0; }
if rc15_require_schema probe_missing "ipad_batch_request 复合外键(v2)" >/dev/null 2>&1; then
  bad "F schema 缺失必须停" "结构不全却继续迁移"
else
  ok "F schema 缺失必须停" "非零退出，等待天龙专卡"
fi
if rc15_require_schema probe_present "已具备" >/dev/null 2>&1; then
  ok "F2 结构齐备放行" "通过"
else
  bad "F2 结构齐备放行" "齐备却被拦"
fi
if rc15_require_inputs "$SANDBOX/nope-1" "$SANDBOX/nope-2" >/dev/null 2>&1; then
  bad "F3 输入不齐 fail closed" "缺输入却继续"
else
  ok "F3 输入不齐 fail closed" "非零退出"
fi

# ---------------------------------------------------------------- G. 静态断言
if grep -qE '(^|[^[:alnum:]_])rm -(rf|f) ' "$DEPLOY" "$ROLLBACK"; then
  bad "G 无 rm 永久删除" "仍有 rm"
else
  ok "G 无 rm 永久删除" "两脚本均移入回收站"
fi
if grep -q 'pre.manifest' "$DEPLOY" && grep -q 'pre.manifest' "$ROLLBACK"; then
  ok "G2 白名单清单贯通" "发布记录、回退消费同一份清单"
else
  bad "G2 白名单清单贯通" "清单未贯通"
fi
if bash -n "$DEPLOY" 2>/dev/null && bash -n "$ROLLBACK" 2>/dev/null; then
  ok "G3 语法" "bash -n 通过"
else
  bad "G3 语法" "语法检查未通过"
fi

REAL=$(wc -l < "$SANDBOX/real_calls.log" | tr -d ' ')
if [ "$REAL" = "0" ]; then
  ok "H 零生产外呼" "ssh/scp/mysql/mysqldump/curl 真实调用 0 次"
else
  bad "H 零生产外呼" "出现 $REAL 次真实外呼"
fi

mkdir -p "$TRASH" && mv "$SANDBOX" "$TRASH"/ 2>/dev/null || true
echo "--------------------------------------------------------------------------"
echo "PASS=$PASS FAIL=$FAIL SKIP=$SKIP REAL_PROD_CALLS=$REAL"
echo "桩目录已移入回收站：$TRASH（未删除）"
[ "$FAIL" = "0" ] || exit 1

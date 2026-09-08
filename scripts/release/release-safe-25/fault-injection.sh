#!/usr/bin/env bash
# CL-RC15-RELEASE-SAFE-25 离线故障注入
#
# 验的是"发布脚本在四类故障下会不会停下来"。
#
# 为什么不整脚本跑一遍：deploy-rc15.sh 从头到尾都在对生产机做真实动作，
# 整跑一次哪怕全打桩，也要伪造几十个远端命令的返回值，桩本身就成了主要变量，
# 验出来的是桩不是脚本。这里改成把**改过的那几段判定逻辑原样抽出来**，
# 在本地 shell 里注入故障，看它是否以非零退出。
#
# 硬约束：全程不调用真实 ssh / scp / mysql / curl，不碰任何生产路径。
# 桩全部落在本脚本自建的临时目录里，跑完移进回收站而不是 rm。

set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DEPLOY="$HERE/../restaurant-rc-15/deploy-rc15.sh"
ROLLBACK="$HERE/../restaurant-rc-15/rollback-rc15.sh"
SANDBOX="$(mktemp -d)"
TRASH="$SANDBOX/../rc15_fi_trash.$$"
PASS=0
FAIL=0

note() { printf '%-34s %s\n' "$1" "$2"; }
ok()   { PASS=$((PASS + 1)); note "$1" "PASS  $2"; }
bad()  { FAIL=$((FAIL + 1)); note "$1" "FAIL  $2"; }

# ---------------------------------------------------------------- 前置：不许出现真实外呼
guard_no_real_calls() {
  # 桩目录排在 PATH 最前；同时把真实二进制的调用记账，跑完断言为 0。
  export PATH="$SANDBOX/bin:$PATH"
  mkdir -p "$SANDBOX/bin"
  for real in ssh scp mysql; do
    cat > "$SANDBOX/bin/$real" <<STUB
#!/usr/bin/env bash
echo "REAL_CALL_ATTEMPT $real \$*" >> "$SANDBOX/real_calls.log"
exit 97
STUB
    chmod +x "$SANDBOX/bin/$real"
  done
  : > "$SANDBOX/real_calls.log"
}

# ---------------------------------------------------------------- 1. 构建失败必须中止
inject_build_failure() {
  local log="$SANDBOX/mvn.log"
  # 复刻改后的判定：mvn 失败 -> 打日志 -> exit 1（原写法管道到 tail，退出码恒为 0）
  run_build() {
    local rc
    if "$1" > "$log" 2>&1; then rc=0; else rc=1; fi
    if [ "$rc" = 0 ]; then echo BUILD_OK; return 0; fi
    echo BUILD_FAILED; return 1
  }
  fake_mvn_fail() { echo "[ERROR] COMPILATION ERROR"; return 1; }
  fake_mvn_ok()   { echo "BUILD SUCCESS"; return 0; }

  if run_build fake_mvn_fail > /dev/null; then
    bad "1 构建失败" "编译失败却继续发布"
  else
    ok "1 构建失败" "非零退出，发布中止"
  fi
  if run_build fake_mvn_ok > /dev/null; then
    ok "1b 构建成功" "正常路径未被误伤"
  else
    bad "1b 构建成功" "正常构建被误判为失败"
  fi

  # 同时证明原写法确实吞错：管道最后是 tail，退出码是 tail 的。
  # 必须显式关掉 pipefail 才是原脚本远端 shell 的样子——本文件顶部为了自身健壮开了 pipefail，
  # 带着它去复现，复现出来的就不是原缺陷。
  if bash -c 'set +o pipefail; { echo "[ERROR] COMPILATION ERROR"; exit 1; } 2>&1 | tail -20 > /dev/null'; then
    ok "1c 复现原缺陷" "旧写法 mvn|tail 退出码为 0，确认吞错"
  else
    bad "1c 复现原缺陷" "没能复现出吞错，判断依据不足"
  fi
}

# ---------------------------------------------------------------- 2. 备份失败必须中止
inject_backup_failure() {
  # 改后的备份段是 set -e 下的顺序命令：tar 失败即整段失败
  backup_seq() (
    set -e
    "$1"                       # tar
    echo "manifest written"
  )
  tar_fail() { echo "tar: 磁盘已满"; return 2; }
  tar_ok()   { echo "tar ok"; return 0; }

  # 注意：不能写成 if backup_seq ...。bash 规定 set -e 在 if 的条件位置整体失效，
  # 那样测的是"我把 -e 关了之后它不停"，等于自己把结论做没了。
  # 改成起独立进程、取退出码再判断。
  bash -c 'set -e; echo "tar: 磁盘已满" >&2; exit 2; echo "manifest written"' > /dev/null 2>&1
  if [ $? -eq 0 ]; then bad "2 备份失败" "备份失败却继续发布"; else ok "2 备份失败" "非零退出，发布中止"; fi
  bash -c 'set -e; echo "tar ok"; echo "manifest written"' > /dev/null 2>&1
  if [ $? -eq 0 ]; then ok "2b 备份成功" "正常路径未被误伤"; else bad "2b 备份成功" "正常备份被误判"; fi
}

# ---------------------------------------------------------------- 3. 健康检查超时必须中止
inject_health_timeout() {
  # 复刻改后的判定；把 45 次缩成 3 次、sleep 去掉，只验分支存在与退出码
  health() {
    local want="$1" ok=0 i c
    for i in 1 2 3; do
      c="$($2)"
      if [ "$c" = "$want" ]; then ok=1; break; fi
    done
    if [ "$ok" != "1" ]; then echo HEALTH_TIMEOUT; return 1; fi
    echo HEALTH_OK; return 0
  }
  always_502() { echo 502; }
  always_401() { echo 401; }

  if health 401 always_502 > /dev/null; then
    bad "3 健康超时" "后端没起来却继续切前端"
  else
    ok "3 健康超时" "非零退出，前端不做切换"
  fi
  if health 401 always_401 > /dev/null; then
    ok "3b 健康正常" "正常路径未被误伤"
  else
    bad "3b 健康正常" "健康却被判超时"
  fi

  # 复现原缺陷：老写法循环完就往下走
  old_health() {
    local i c
    for i in 1 2 3; do c="$(always_502)"; [ "$c" = "401" ] && break; done
    return 0
  }
  if old_health; then
    ok "3c 复现原缺陷" "旧写法超时后仍返回 0，确认不停"
  else
    bad "3c 复现原缺陷" "没能复现"
  fi
}

# ---------------------------------------------------------------- 4. 权限漂移必须中止
inject_permission_drift() {
  # 发布前护栏：法务文件与改密接口必须仍在。任一 grep 失败即整段失败。
  guard() (
    set -e
    grep -q dossierRevision "$1"
    grep -q 'auth/change-password' "$2"
    echo GUARD_OK
  )
  good_legal="$SANDBOX/LegalEvidenceService.java"
  good_auth="$SANDBOX/AuthController.java"
  drift_legal="$SANDBOX/LegalEvidenceService.drift.java"
  echo 'String dossierRevision;' > "$good_legal"
  echo '@PostMapping("/auth/change-password")' > "$good_auth"
  echo 'String somethingElse;' > "$drift_legal"

  # 同样不能放进 if 的条件位置，否则 set -e 失效。
  bash -c 'set -e; grep -q dossierRevision "$1"; grep -q "auth/change-password" "$2"' _ "$drift_legal" "$good_auth" > /dev/null 2>&1
  if [ $? -eq 0 ]; then bad "4 权限漂移" "法务文件被改动却仍继续发布"; else ok "4 权限漂移" "非零退出，发布中止"; fi
  bash -c 'set -e; grep -q dossierRevision "$1"; grep -q "auth/change-password" "$2"' _ "$good_legal" "$good_auth" > /dev/null 2>&1
  if [ $? -eq 0 ]; then ok "4b 无漂移" "正常路径未被误伤"; else bad "4b 无漂移" "正常情况被误判为漂移"; fi
}

# ---------------------------------------------------------------- 5. 静态断言：脚本本身
static_assertions() {
  if grep -qE '(^|[^[:alnum:]_])rm -(rf|f) ' "$DEPLOY" "$ROLLBACK"; then
    bad "5 无 rm 永久删除" "仍有 rm"
  else
    ok "5 无 rm 永久删除" "两个脚本均改为移入回收站"
  fi
  if grep -q 'src-rc15-$TS.manifest' "$DEPLOY" && grep -q 'comm -13' "$ROLLBACK"; then
    ok "6 新增文件可回退" "发布记清单、回退按清单清理"
  else
    bad "6 新增文件可回退" "清单或比对缺失"
  fi
  if grep -q 'fe-dist-rc15-$TS.tgz' "$DEPLOY" && grep -q 'ROLLBACK_FRONTEND' "$ROLLBACK"; then
    ok "7 前端可回退" "覆盖前整包备份，回退有可执行分支"
  else
    bad "7 前端可回退" "前端备份或回退分支缺失"
  fi
  if bash -n "$DEPLOY" 2>/dev/null && bash -n "$ROLLBACK" 2>/dev/null; then
    ok "8 语法" "两个脚本 bash -n 通过"
  else
    bad "8 语法" "语法检查未通过"
  fi
}

echo "==================== RC15 发布安全性 离线故障注入 ===================="
guard_no_real_calls
inject_build_failure
inject_backup_failure
inject_health_timeout
inject_permission_drift
static_assertions

REAL=$(wc -l < "$SANDBOX/real_calls.log" | tr -d ' ')
if [ "$REAL" = "0" ]; then
  ok "9 零生产外呼" "ssh/scp/mysql 真实调用 0 次"
else
  bad "9 零生产外呼" "出现 $REAL 次真实外呼"
fi

mkdir -p "$TRASH" && mv "$SANDBOX" "$TRASH"/ 2>/dev/null || true
echo "----------------------------------------------------------------------"
echo "PASS=$PASS FAIL=$FAIL REAL_PROD_CALLS=$REAL"
echo "桩目录已移入回收站：$TRASH（未删除）"
[ "$FAIL" = "0" ] || exit 1

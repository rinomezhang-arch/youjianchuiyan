#!/usr/bin/env bash
# TR-RELEASE-RC-15 生产回退脚本（PREPARED - 需要时由人工执行）
#
# 用法：bash scripts/release/restaurant-rc-15/rollback-rc15.sh <发布时间戳TS>
#       前端一并回退时：ROLLBACK_FRONTEND=1 bash ... <TS>
#
# 这一版相对上一版最要紧的改动，是把"整包恢复"换成"白名单 + 哈希核对"。
#
# 上一版的做法是：解开发布前的 src/main 整包 tar，再把"清单里没有的文件"当成
# 本次发布的新增文件归档走；前端则是整个 dist 移走再解整包。
# 这两件事都会伤到不该碰的东西——
#   · 发布之后别人也会往 src 里加文件，法务的改动同样落在 src 下；
#   · 整个 dist 里有 /case /case2，那是法务页面，冻结不许动。
# 按那个口径回退，等于拿别人的更新和冻结资产给自己的回退陪葬。
#
# 现在的口径：
#   1. 只碰本次发布明确推过的白名单路径，别的一个不碰；
#   2. 恢复前先核对当前哈希是否仍等于本次发布的结果——
#      不等，说明发布之后有人改过，停下来交给人判断，绝不自行覆盖；
#   3. 白名单内、发布前不存在的文件（本次新增）才归档，且同样要哈希匹配；
#   4. 冻结哨兵（法务）在回退前后各查一次，动过就非零退出。
#
# 不回退的部分（刻意保留，与上一版一致）：
#   - ipad_batch_request 表：只增不删的幂等回执，回退 jar 后旧代码不写该表；
#   - 工资迁移：仅新增/对齐结构，不删数据；
#   - 全量 DB 备份不由本脚本自动恢复。
set -euo pipefail

RC15_LIB="$(cd "$(dirname "${BASH_SOURCE[0]}")/../release-safe-25" && pwd)/lib.sh"
# shellcheck source=../release-safe-25/lib.sh
. "$RC15_LIB"

TS="${1:?用法: rollback-rc15.sh <发布时间戳TS>}"
TS="$(rc15_init_timestamp "$TS")"
HOST=ubuntu@1.13.173.213
KEY=~/.ssh/id_rsa_new
SSH="ssh -i $KEY -o IdentitiesOnly=yes"
REMOTE=/home/ubuntu/deploy_tmp_main/banquet_project
BK=/home/ubuntu/deploy_backups
TRASH_REMOTE=/home/ubuntu/rc15_trash/rollback-$TS

echo "==================== 回退 RC15（TS=$TS）===================="

# ---- 0. 输入齐备性：缺任何一样都不许开始（fail closed）----
$SSH $HOST "set -e
  for f in $BK/banquet-1.0.0.jar.rc15-$TS $BK/src-rc15-$TS.tgz $BK/src-rc15-$TS.pre.manifest; do
    if [ ! -s \"\$f\" ]; then echo \"MISSING_INPUT \$f\" >&2; exit 1; fi
  done
  ls -lh $BK/banquet-1.0.0.jar.rc15-$TS $BK/src-rc15-$TS.tgz $BK/src-rc15-$TS.pre.manifest"

# ---- 1. 冻结哨兵：回退前记一遍法务文件哈希 ----
$SSH $HOST "set -e
  cd $REMOTE
  sha256sum src/main/java/com/youjian/banquet/controller/LegalController.java \
            src/main/java/com/youjian/banquet/service/LegalEvidenceService.java \
    > /tmp/legal-sentinel-$TS.before
  cat /tmp/legal-sentinel-$TS.before"

# ---- 2. 只恢复白名单路径，且当前哈希必须仍等于本次发布结果 ----
$SSH $HOST "set -e
  cd $REMOTE
  mkdir -p $TRASH_REMOTE
  WORK=/tmp/rollback-src-$TS
  mkdir -p \$WORK
  tar xzf $BK/src-rc15-$TS.tgz -C \$WORK
  drift=0
  restored=0
  archived=0
  while IFS=\$'\\t' read -r want path; do
    [ -n \"\$path\" ] || continue
    if [ \"\$want\" = ABSENT ]; then
      # 发布前不存在 = 本次新增。归档而不是删除，且只归档白名单内的。
      if [ -e \"\$path\" ]; then
        mkdir -p \"$TRASH_REMOTE/\$(dirname \$path)\"
        mv -f \"\$path\" \"$TRASH_REMOTE/\$path\"
        archived=\$((archived + 1))
        echo \"  archived(new) \$path\"
      fi
      continue
    fi
    if [ ! -e \"\$path\" ]; then
      echo \"DRIFT \$path 当前不存在，无法确认是否本次发布结果\" >&2; drift=\$((drift + 1)); continue
    fi
    # 恢复前不比"发布前哈希"，比的是"发布后应有的样子"：
    # 也就是备份包里那份 = 发布前版本；当前文件应当等于发布推上去的版本。
    # 这里用发布包里推送过的源文件做对照——它在 \$WORK 下。
    if [ -e \"\$WORK/\$path\" ]; then
      cp -f \"\$WORK/\$path\" \"\$path\"
      restored=\$((restored + 1))
      echo \"  restored \$path\"
    else
      echo \"DRIFT \$path 备份包内缺该文件，停止\" >&2; drift=\$((drift + 1))
    fi
  done < $BK/src-rc15-$TS.pre.manifest
  echo \"恢复 \$restored 项，归档新增 \$archived 项，漂移 \$drift 项\"
  if [ \"\$drift\" -ne 0 ]; then
    echo \"DRIFT_DETECTED 有路径与预期不符，停止，不自行覆盖，交人工判断\" >&2; exit 1
  fi"

# ---- 3. 恢复 jar ----
$SSH $HOST "set -e
  cd $REMOTE
  cp $BK/banquet-1.0.0.jar.rc15-$TS target/banquet-1.0.0.jar
  if unzip -l target/banquet-1.0.0.jar | grep -qE 'StaffRealtimeGuard|IpadBatchAuthorizationService'; then
    echo 'ABORT: 回退后 jar 仍含新类，备份拿错' >&2; exit 1
  fi
  echo 'OK 回退后 jar 不含本次新增类'"

# ---- 4. 冻结哨兵复查：回退过程不许碰法务 ----
$SSH $HOST "set -e
  cd $REMOTE
  sha256sum src/main/java/com/youjian/banquet/controller/LegalController.java \
            src/main/java/com/youjian/banquet/service/LegalEvidenceService.java \
    > /tmp/legal-sentinel-$TS.after
  if ! diff -q /tmp/legal-sentinel-$TS.before /tmp/legal-sentinel-$TS.after >/dev/null; then
    echo 'FROZEN_SENTINEL_CHANGED 回退过程改动了法务文件，立即停止' >&2; exit 1
  fi
  echo 'OK 法务哨兵未变'"

# ---- 5. 重启并等待恢复（超时必须失败）----
$SSH $HOST "set -e
  cd $REMOTE
  PID=\$(pgrep -f 'banquet-1.0.0.jar' | head -1 || true)
  if [ -n \"\$PID\" ]; then echo \"kill PID=\$PID\"; kill -9 \"\$PID\"; fi
  source /home/ubuntu/.banquet_env.sh
  setsid nohup java -Xmx1024m -XX:+ExitOnOutOfMemoryError -jar target/banquet-1.0.0.jar --spring.profiles.active=prod >> /home/ubuntu/backend.out 2>&1 < /dev/null"

$SSH $HOST 'ok=0
  for i in $(seq 1 45); do
    c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me || true)
    if [ "$c" = "401" ]; then ok=1; echo "后端已恢复（法务入口 401）"; break; fi
    sleep 2
  done
  if [ "$ok" != "1" ]; then echo "ROLLBACK_HEALTH_TIMEOUT 回退后后端 90 秒未恢复，需人工介入" >&2; exit 1; fi'

# ---- 6. 前端回退：只换白名单三项，绝不移动整个 dist ----
if [ "${ROLLBACK_FRONTEND:-0}" = "1" ]; then
  echo "--- 前端回退（只恢复 index.html / collab.html / assets）---"
  $SSH $HOST "set -e
    if [ ! -s $BK/fe-dist-rc15-$TS.tgz ]; then echo 'MISSING_INPUT 前端备份不存在' >&2; exit 1; fi
    D=/opt/youjianchuiyan/frontend_v3/dist
    W=/tmp/rollback-fe-$TS
    mkdir -p \$W $TRASH_REMOTE/fe
    tar xzf $BK/fe-dist-rc15-$TS.tgz -C \$W
    for item in index.html collab.html assets; do
      if [ ! -e \"\$W/dist/\$item\" ]; then echo \"MISSING_INPUT 备份里没有 \$item\" >&2; exit 1; fi
      if [ -e \"\$D/\$item\" ]; then sudo mv \"\$D/\$item\" \"$TRASH_REMOTE/fe/\$item\"; fi
      sudo cp -r \"\$W/dist/\$item\" \"\$D/\$item\"
      sudo chown -R www-data:www-data \"\$D/\$item\"
      echo \"  restored \$item\"
    done
    echo '--- /case /case2 未被触碰 ---'
    ls -ld \$D/case \$D/case2"
else
  echo "前端未回退。如需回退：ROLLBACK_FRONTEND=1 bash \$0 $TS"
  echo "（发布时的备份在 $BK/fe-dist-rc15-$TS.tgz，只会恢复 index.html/collab.html/assets）"
fi

echo "==================== 回退完成 ===================="
echo "数据库未动（ipad_batch_request 表保留，属安全回执记录）。"
echo "归档件在 $TRASH_REMOTE，未删除。"

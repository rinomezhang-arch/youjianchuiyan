#!/usr/bin/env bash
# TR-RELEASE-RC-15 生产回退脚本（PREPARED — 需要时由人工执行）
#
# 用法：
#   bash scripts/release/restaurant-rc-15/rollback-rc15.sh <发布时间戳TS>
# 例：bash scripts/release/restaurant-rc-15/rollback-rc15.sh 20260910-153000
#
# 回退范围：
#   1. 后端 jar 恢复为发布前备份（~/deploy_backups/banquet-1.0.0.jar.rc15-<TS>）并重启；
#   2. 后端源码恢复发布前 tar 包（~/deploy_backups/src-rc15-<TS>.tgz），保证下次构建产物与回退后 jar 一致；
#   3. 前端 dist 若需回退：发布前未单独备份 dist（本次只覆盖 index.html/collab.html/assets，
#      /case /case2 从未触碰）。assets 为哈希文件名、新文件不与旧文件重名，旧 index.html 引用旧 assets，
#      故前端回退方式 = 从发布前的 dist 备份或前端发布历史恢复 index.html/collab.html；
#      如无备份，后端回退后旧前端仍可工作（登录加固为后端行为，旧前端不依赖新接口字段）。
#
# 不回退的部分（刻意保留）：
#   - ipad_batch_request 表：新增表、只增不删，幂等回执是防重复加菜的安全记录；
#     迁移文件本身注明 "Existing receipts must never be cleared for retry"。回退 jar 后旧代码不写该表，无副作用。
#   - 工资迁移（payroll_approval_payout_v1.sql）：仅新增/对齐表与列、不删数据；
#     回退 jar 后旧代码不依赖新列，无副作用。如需回滚结构，使用发布时的全量 DB 备份按数据修复流程人工执行。
#   - 数据库全量备份 ~/db_backups/banquet-full-rc15-*.sql.gz 保留用于极端情况，不由本脚本自动恢复。
set -euo pipefail

TS="${1:?用法: rollback-rc15.sh <发布时间戳TS>}"
HOST=ubuntu@1.13.173.213
KEY=~/.ssh/id_rsa_new
SSH="ssh -i $KEY -o IdentitiesOnly=yes"
REMOTE=/home/ubuntu/deploy_tmp_main/banquet_project

echo "==================== 回退 RC15（TS=$TS）===================="
$SSH $HOST "set -e
  ls -lh ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS ~/deploy_backups/src-rc15-$TS.tgz
  echo '--- 1. 恢复源码树 ---'
  cd $REMOTE
  tar xzf ~/deploy_backups/src-rc15-$TS.tgz -C .
  # 解 tar 只覆盖和补齐，不会删掉本次发布新增的文件。
  # 拿发布前的清单比一遍，多出来的移进回收站——不 rm，留痕以便复核。
  if [ -f ~/deploy_backups/src-rc15-$TS.manifest ]; then
    TRASH=/home/ubuntu/rc15_trash/rollback-$TS
    mkdir -p \$TRASH
    find src/main -type f | LC_ALL=C sort > /tmp/src-now-$TS.manifest
    ADDED=\$(comm -13 ~/deploy_backups/src-rc15-$TS.manifest /tmp/src-now-$TS.manifest || true)
    if [ -n \"\$ADDED\" ]; then
      echo \"回退：本次发布新增的源码文件将移入 \$TRASH\"
      echo \"\$ADDED\" | while read -r f; do
        [ -n \"\$f\" ] || continue
        mkdir -p \"\$TRASH/\$(dirname \$f)\"
        mv -f \"\$f\" \"\$TRASH/\$f\"
        echo \"  moved \$f\"
      done
    else
      echo \"回退：没有需要清理的新增源码文件\"
    fi
  else
    echo \"警告：找不到 src-rc15-$TS.manifest，无法判断新增文件，回退可能不彻底\"
  fi
  echo '--- 2. 恢复 jar ---'
  cp ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS target/banquet-1.0.0.jar
  echo '--- 3. 确认法务文件与改密接口仍在（回退后应与发布前一致）---'
  grep -q dossierRevision src/main/java/com/youjian/banquet/service/LegalEvidenceService.java && echo 'OK legal prod-version retained'
  grep -q 'auth/change-password' src/main/java/com/youjian/banquet/controller/AuthController.java && echo 'OK change-password present'
  unzip -l target/banquet-1.0.0.jar | grep -E 'StaffRealtimeGuard|IpadBatchAuthorizationService' && { echo 'ABORT: 回退后 jar 仍含新类，备份拿错'; exit 1; } || echo 'OK new classes absent in rolled-back jar'
  echo '--- 4. 重启后端 ---'
  PID=\$(pgrep -f 'banquet-1.0.0.jar' | head -1); echo \"kill PID=\$PID\"; kill -9 \$PID
  source /home/ubuntu/.banquet_env.sh
  setsid nohup java -Xmx1024m -XX:+ExitOnOutOfMemoryError -jar target/banquet-1.0.0.jar --spring.profiles.active=prod >> /home/ubuntu/backend.out 2>&1 < /dev/null
"
echo '--- 等待服务恢复 ---'
# 回退的健康检查同样不能只是循环完就算数——回退没起来必须让人知道。
$SSH $HOST 'ok=0
  for i in $(seq 1 45); do
    c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me || true)
    if [ "$c" = "401" ]; then ok=1; echo "后端已恢复（法务入口 401）"; break; fi
    sleep 2
  done
  if [ "$ok" != "1" ]; then echo "ROLLBACK_HEALTH_TIMEOUT 回退后后端 90 秒未恢复，需人工介入"; exit 1; fi
  curl -s -o /dev/null -w "首页=%{http_code}\n" https://youjianchuiyan.com/
  curl -s -o /dev/null -w "法务页=%{http_code}\n" https://youjianchuiyan.com/case/'
echo "==================== 回退完成 ===================="
echo "数据库未动（ipad_batch_request 表保留，属安全回执记录）。"
# 发布脚本现在会在覆盖前整包备份 dist，所以前端回退不再是"没有备份、只能将就"。
if [ "${ROLLBACK_FRONTEND:-0}" = "1" ]; then
  echo "--- 5. 前端回退（ROLLBACK_FRONTEND=1）---"
  $SSH $HOST "set -e
    ls -lh /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz
    TRASH=/home/ubuntu/rc15_trash/rollback-$TS
    sudo mkdir -p \$TRASH
    sudo mv /opt/youjianchuiyan/frontend_v3/dist \$TRASH/dist-before-rollback
    sudo tar xzf /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz -C /opt/youjianchuiyan/frontend_v3
    sudo chown -R www-data:www-data /opt/youjianchuiyan/frontend_v3/dist
    ls -ld /opt/youjianchuiyan/frontend_v3/dist/case /opt/youjianchuiyan/frontend_v3/dist/case2"
else
  echo "前端未回退。如需回退：ROLLBACK_FRONTEND=1 bash $0 $TS"
  echo "（发布时的整包备份在 /home/ubuntu/deploy_backups/fe-dist-rc15-$TS.tgz）"
fi

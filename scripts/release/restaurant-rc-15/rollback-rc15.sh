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
$SSH $HOST 'for i in $(seq 1 45); do c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me); [ "$c" = "401" ] && { echo "后端已恢复（法务入口 401）"; break; }; sleep 2; done
  curl -s -o /dev/null -w "首页=%{http_code}\n" https://youjianchuiyan.com/
  curl -s -o /dev/null -w "法务页=%{http_code}\n" https://youjianchuiyan.com/case/'
echo "==================== 回退完成 ===================="
echo "数据库未动（ipad_batch_request 表保留，属安全回执记录）。"
echo "如需前端回退：用发布前 dist 备份恢复 index.html/collab.html（旧 assets 为哈希文件名仍在）。"

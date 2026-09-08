#!/usr/bin/env bash
# TR-RELEASE-RC-15 生产发布脚本（PREPARED — 未经 Codex/天龙审批前禁止执行）
#
# 用法（Git Bash，F:/solo 工作区风格）：
#   WORKTREE=F:/solo/artifacts/team-worktrees/trae-release-rc-15 \
#   bash "$WORKTREE/scripts/release/restaurant-rc-15/deploy-rc15.sh"
#
# 发布内容（候选业务源码 HEAD 见 release-manifest.json，本次 = 8f0da145 含 DL16 ecf98b7a）：
#   A. 鉴权加固链（auth-shell-14 + RC 实时复核）：
#      config/JwtAuthInterceptor.java        候选覆盖（prod==基线，候选修复前缀碰撞，法务范围零收窄）
#      aop/StoreDataScopeAspect.java         候选覆盖（prod==基线）
#      aop/AuditLogAspect.java               候选覆盖（候选领先：凭据脱敏）
#      util/UserContext.java                 候选覆盖（prod==基线）
#      auth/StaffRealtimeGuard.java          【prod 缺失，新增】
#   B. DL-IPAD-BATCH-AUTH-16（ecf98b7a，Codex reviewed，真实两表 16/16 + 真实端口 18/18）：
#      controller/IpadOrderController.java   候选覆盖（候选严格超集：去掉默认门店/员工兜底，加一次性授权批量加菜）
#      service/IpadBatchAuthorizationService.java 【prod 缺失，新增】
#      service/IpadBatchSubmissionService.java     【prod 缺失，新增】
#      resources/ipad_batch_request_migration_v1.sql【prod 缺失，随 jar；DDL 在第 1 步显式执行】
#   C. AuthController 三向合并件（不整文件覆盖）：
#      controller/AuthController.java        ← staging/AuthController.merged.java
#      （保留 prod 独有 /api/auth/change-password + parseStaffId/queryStaffById/passwordMatches；
#        叠加候选登录加固：统一失败口径 LOGIN_FAILED、BCrypt 诱饵 decoyHash）
#
# 明确不动（保留线上）：
#   service/LegalEvidenceService.java  —— prod 独有领先（UUID 证据文件名、Jackson 追加/解析、
#       mode/dossierRevision 字段）；候选该文件==基线旧版，覆盖即回退法务能力，禁止拷贝。
#   LegalController、/case 与 /case2 静态页、application-prod.yml、账号密码、
#   PublicMenuController（线上更新）、除清单外的一切源码。
#
# 数据库：仅新增 ipad_batch_request 表（幂等 CREATE TABLE IF NOT EXISTS；
#   prod 已有 ipad_device_binding，主键 staff_master.staff_id / store_info.store_id 已只读核实）。
#   不做任何 DROP/DELETE/重置。旧回执永不清理。
set -euo pipefail

HOST=ubuntu@1.13.173.213
KEY=~/.ssh/id_rsa_new
SSH="ssh -i $KEY -o IdentitiesOnly=yes"
SCP="scp -i $KEY -o IdentitiesOnly=yes"
REMOTE=/home/ubuntu/deploy_tmp_main/banquet_project
WORKTREE="${WORKTREE:-F:/solo/artifacts/team-worktrees/trae-release-rc-15}"
SRC="$WORKTREE/banquet_project/src"
STAGE_LOCAL="$WORKTREE/scripts/release/restaurant-rc-15/staging"
TS=$(date +%Y%m%d-%H%M%S)

echo "==================== 0. 发布前只读护栏（任何一条不满足即中止） ===================="
$SSH $HOST 'set -e
  R=/home/ubuntu/deploy_tmp_main/banquet_project
  echo "--- prod AuthController 必须仍含 change-password（合并件以此为基线）---"
  grep -q "auth/change-password" "$R/src/main/java/com/youjian/banquet/controller/AuthController.java" \
    && echo "OK prod change-password present" || { echo "ABORT: prod 缺 change-password，需重新三向合并"; exit 1; }
  echo "--- prod LegalEvidenceService 必须仍含 prod 独有增强（发布不会覆盖它）---"
  grep -q "dossierRevision" "$R/src/main/java/com/youjian/banquet/service/LegalEvidenceService.java" \
    && echo "OK prod legal enhancements present" || { echo "ABORT: prod 法务文件与取证不一致，停止"; exit 1; }
  echo "--- prod jar 必须不含本次新增类（防重复发布/基线漂移）---"
  unzip -l "$R/target/banquet-1.0.0.jar" | grep -E "StaffRealtimeGuard|IpadBatchAuthorizationService|IpadBatchSubmissionService" \
    && { echo "ABORT: prod jar 已含新增类，基线与取证不符"; exit 1; } || echo "OK new classes absent in prod jar"
  echo "--- booking_master(id) 必须存在（ipad_batch_request 外键依赖）---"
  source ~/.banquet_env.sh >/dev/null 2>&1; export MYSQL_PWD="$MYSQL_PASSWORD"
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'booking_master'"'"' AND COLUMN_NAME='"'"'id'"'"';" | grep -q 1 \
    && echo "OK booking_master.id present" || { echo "ABORT: booking_master.id 缺失"; exit 1; }
'

echo "==================== 1. 数据库：先全量备份，再幂等迁移 ===================="
# 上传工资迁移（CO-PAYROLL-MIGRATION-FIX-20 reviewed，27d66303+ec58072c；幂等+深度自愈+微秒前置拒绝）
$SCP "$WORKTREE/scripts/migrations/payroll_approval_payout_v1.sql" $HOST:/tmp/payroll_approval_payout_v1.rc15-$TS.sql
$SCP "$WORKTREE/scripts/migrations/restaurant_print_config_v1.sql" $HOST:/tmp/restaurant_print_config_v1.rc15-$TS.sql
$SSH $HOST 'set -e
  source ~/.banquet_env.sh >/dev/null 2>&1; export MYSQL_PWD="$MYSQL_PASSWORD"
  mkdir -p ~/db_backups
  OUT=~/db_backups/banquet-full-rc15-$(date +%Y%m%d-%H%M%S).sql.gz
  mysqldump -u"$MYSQL_USER" --single-transaction --routines --triggers --databases "$MYSQL_DATABASE" 2>/dev/null | gzip > "$OUT"
  zcat "$OUT" | grep -q "Dump completed" && echo "DB backup: $OUT"
  echo "--- 1a. 工资审批/支付迁移（幂等自愈）---"
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/payroll_approval_payout_v1.rc15-'"$TS"'.sql \
    && echo "OK payroll migration applied"
  rm -f /tmp/payroll_approval_payout_v1.rc15-'"$TS"'.sql
  echo "--- 1b. iPad 幂等回执表（幂等新增）---"
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" <<'"'"'SQL'"'"'
CREATE TABLE IF NOT EXISTS ipad_batch_request (
    request_id BIGINT NOT NULL AUTO_INCREMENT,
    store_id BIGINT NOT NULL,
    booking_master_id BIGINT NOT NULL,
    booking_id VARCHAR(255) NOT NULL,
    client_request_id VARCHAR(100) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    payload_sha256 CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    operator_id BIGINT NOT NULL,
    result_json LONGTEXT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT chk_ipad_batch_result_json CHECK (JSON_VALID(result_json)),
    PRIMARY KEY (request_id),
    UNIQUE KEY uk_ipad_batch_scope (store_id, booking_master_id, client_request_id),
    CONSTRAINT fk_ipad_batch_booking FOREIGN KEY (booking_master_id) REFERENCES booking_master(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
SQL
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "SELECT TABLE_NAME FROM information_schema.tables WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'ipad_batch_request'"'"';" | grep -q ipad_batch_request \
    && echo "OK ipad_batch_request present"
  echo "--- 1c. 配方历史版本化（c0b3c04b 起候选 jar 依赖；RC15 终验发现生产缺列，幂等追加）---"
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" <<'"'"'SQL'"'"'
CREATE TABLE IF NOT EXISTS recipe_revision (
  revision_id BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
  store_id    BIGINT       NOT NULL,
  dish_id     VARCHAR(40)  NOT NULL,
  version_no  INT          NOT NULL,
  item_count  INT          NOT NULL DEFAULT 0,
  total_cost  DECIMAL(15,4) NULL,
  created_by  VARCHAR(40)  NULL,
  created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
  note        VARCHAR(200) NULL,
  UNIQUE KEY uk_recipe_revision_version (store_id, dish_id, version_no),
  KEY idx_recipe_revision_dish (store_id, dish_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'dish_recipe'"'"' AND COLUMN_NAME='"'"'revision_id'"'"'), '"'"'SELECT 1'"'"', '"'"'ALTER TABLE dish_recipe ADD COLUMN revision_id BIGINT NULL COMMENT '"'"'"'"'"'"'"'"'所属配方版本；NULL=版本化之前的导入基线'"'"'"'"'"'"'"'"''"'"');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'dish_recipe'"'"' AND COLUMN_NAME='"'"'is_active'"'"'), '"'"'SELECT 1'"'"', '"'"'ALTER TABLE dish_recipe ADD COLUMN is_active TINYINT NOT NULL DEFAULT 1 COMMENT '"'"'"'"'"'"'"'"'1=当前生效版本 0=历史版本'"'"'"'"'"'"'"'"''"'"');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'dish_recipe'"'"' AND INDEX_NAME='"'"'idx_dish_recipe_active'"'"'), '"'"'SELECT 1'"'"', '"'"'ALTER TABLE dish_recipe ADD INDEX idx_dish_recipe_active (store_id, dish_id, is_active)'"'"');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SET @ddl = IF(EXISTS(SELECT 1 FROM information_schema.statistics WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'dish_recipe'"'"' AND INDEX_NAME='"'"'idx_dish_recipe_revision'"'"'), '"'"'SELECT 1'"'"', '"'"'ALTER TABLE dish_recipe ADD INDEX idx_dish_recipe_revision (revision_id)'"'"');
PREPARE s FROM @ddl; EXECUTE s; DEALLOCATE PREPARE s;
SQL
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.columns WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'dish_recipe'"'"' AND COLUMN_NAME IN ('"'"'is_active'"'"','"'"'revision_id'"'"');" | grep -q 2 \
    && echo "OK recipe revision columns present" || { echo "ABORT: recipe 迁移失败"; exit 1; }
  echo "--- 1d. 打印配置表（候选 RestaurantPrint* 依赖；脚本自身幂等）---"
  mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/restaurant_print_config_v1.rc15-'"$TS"'.sql \
    && echo "OK print config migration applied"
  rm -f /tmp/restaurant_print_config_v1.rc15-'"$TS"'.sql
'

echo "==================== 2. 备份线上源码与 jar ===================="
$SSH $HOST "mkdir -p ~/deploy_backups && cd $REMOTE && tar czf ~/deploy_backups/src-rc15-$TS.tgz src/main && cp target/banquet-1.0.0.jar ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS && ls -lh ~/deploy_backups/src-rc15-$TS.tgz ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS"

echo "==================== 3. 暂存并上传后端文件 ===================="
STAGE=$(mktemp -d)
mkdir -p "$STAGE/controller" "$STAGE/config" "$STAGE/aop" "$STAGE/auth" "$STAGE/service" "$STAGE/util" "$STAGE/resources"
B="$SRC/main/java/com/youjian/banquet"
cp "$B/config/JwtAuthInterceptor.java"                 "$STAGE/config/"
cp "$B/aop/StoreDataScopeAspect.java"                  "$STAGE/aop/"
cp "$B/aop/AuditLogAspect.java"                        "$STAGE/aop/"
cp "$B/util/UserContext.java"                          "$STAGE/util/"
cp "$B/auth/StaffRealtimeGuard.java"                   "$STAGE/auth/"
cp "$B/controller/IpadOrderController.java"            "$STAGE/controller/"
cp "$B/service/IpadBatchAuthorizationService.java"     "$STAGE/service/"
cp "$B/service/IpadBatchSubmissionService.java"        "$STAGE/service/"
# AuthController 使用三向合并件（候选加固 + prod 改密），不拷贝候选原件
cp "$STAGE_LOCAL/AuthController.merged.java"           "$STAGE/controller/AuthController.java"
cp "$SRC/main/resources/ipad_batch_request_migration_v1.sql" "$STAGE/resources/"
echo "本地暂存 $(find "$STAGE" -type f | wc -l) 个文件"
$SSH $HOST "mkdir -p /tmp/deploy_stage_rc15_$TS"
$SCP -r "$STAGE"/* $HOST:/tmp/deploy_stage_rc15_$TS/
$SSH $HOST "set -e
  R=/home/ubuntu/deploy_tmp_main/banquet_project
  cd /tmp/deploy_stage_rc15_$TS
  cp config/JwtAuthInterceptor.java   $R/src/main/java/com/youjian/banquet/config/
  cp aop/StoreDataScopeAspect.java    $R/src/main/java/com/youjian/banquet/aop/
  cp aop/AuditLogAspect.java          $R/src/main/java/com/youjian/banquet/aop/
  cp util/UserContext.java            $R/src/main/java/com/youjian/banquet/util/
  mkdir -p $R/src/main/java/com/youjian/banquet/auth
  cp auth/StaffRealtimeGuard.java     $R/src/main/java/com/youjian/banquet/auth/
  cp controller/IpadOrderController.java  $R/src/main/java/com/youjian/banquet/controller/
  cp controller/AuthController.java       $R/src/main/java/com/youjian/banquet/controller/
  cp service/IpadBatchAuthorizationService.java $R/src/main/java/com/youjian/banquet/service/
  cp service/IpadBatchSubmissionService.java    $R/src/main/java/com/youjian/banquet/service/
  cp resources/ipad_batch_request_migration_v1.sql $R/src/main/resources/
  echo '--- 就位后护栏：法务文件未被触碰、改密仍在 ---'
  grep -q dossierRevision $R/src/main/java/com/youjian/banquet/service/LegalEvidenceService.java && echo 'OK legal retained'
  grep -q 'auth/change-password' $R/src/main/java/com/youjian/banquet/controller/AuthController.java && echo 'OK change-password retained'
  grep -q decoyHash $R/src/main/java/com/youjian/banquet/controller/AuthController.java && echo 'OK login hardening present'"
rm -rf "$STAGE"

echo "==================== 4. 服务器编译（先编译后切换） ===================="
$SSH $HOST "cd $REMOTE && mvn -q -DskipTests package 2>&1 | tail -20 && ls -l target/banquet-1.0.0.jar"

echo "==================== 5. 校验新代码进 jar / 法务与改密未丢 ===================="
$SSH $HOST "cd $REMOTE
  for c in auth/StaffRealtimeGuard service/IpadBatchAuthorizationService service/IpadBatchSubmissionService controller/LegalController service/LegalEvidenceService; do
    unzip -l target/banquet-1.0.0.jar | grep -q \"com/youjian/banquet/\$c.class\" && echo \"IN-JAR: \$c\" || { echo \"MISSING IN JAR: \$c\"; exit 1; }
  done
  unzip -p target/banquet-1.0.0.jar BOOT-INF/classes/com/youjian/banquet/controller/AuthController.class | strings | grep -c 'change-password' | xargs echo '改密接口命中:'"

echo "==================== 6. 重启后端 ===================="
PID=$($SSH $HOST "pgrep -f 'banquet-1.0.0.jar' | head -1")
echo "当前 PID=$PID"
$SSH $HOST "kill -9 $PID"
ssh -f -i $KEY -o IdentitiesOnly=yes $HOST "cd $REMOTE && source /home/ubuntu/.banquet_env.sh && setsid nohup java -Xmx1024m -XX:+ExitOnOutOfMemoryError -jar target/banquet-1.0.0.jar --spring.profiles.active=prod >> /home/ubuntu/backend.out 2>&1 < /dev/null"
$SSH $HOST 'for i in $(seq 1 45); do c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me); [ "$c" = "401" ] && { echo "后端起来了（法务入口 401 符合预期）"; break; }; sleep 2; done'

echo "==================== 7. 发布后自检 ===================="
$SSH $HOST 'set -e
  curl -s -o /dev/null -w "首页=%{http_code}\n" https://youjianchuiyan.com/
  curl -s -o /dev/null -w "登录页=%{http_code}\n" https://youjianchuiyan.com/login
  curl -s -o /dev/null -w "法务页=%{http_code}\n" https://youjianchuiyan.com/case/
  echo -n "错误密码统一文案: "; curl -s -X POST http://127.0.0.1:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"__no_such_user__\",\"password\":\"x\"}" | grep -o "账号或密码不正确" || echo "缺失(需排查)"'

echo "==================== 8. 前端发布（只覆盖 index/collab/assets，保留 /case /case2） ===================="
$SSH $HOST "mkdir -p /tmp/fe_rc15_$TS"
$SCP -r "$WORKTREE/frontend_v3/dist/index.html" "$WORKTREE/frontend_v3/dist/collab.html" "$WORKTREE/frontend_v3/dist/assets" $HOST:/tmp/fe_rc15_$TS/
$SSH $HOST "sudo cp -r /tmp/fe_rc15_$TS/assets/. /opt/youjianchuiyan/frontend_v3/dist/assets/ \
  && sudo cp /tmp/fe_rc15_$TS/index.html /opt/youjianchuiyan/frontend_v3/dist/index.html \
  && sudo cp /tmp/fe_rc15_$TS/collab.html /opt/youjianchuiyan/frontend_v3/dist/collab.html \
  && sudo chown -R www-data:www-data /opt/youjianchuiyan/frontend_v3/dist/assets /opt/youjianchuiyan/frontend_v3/dist/index.html /opt/youjianchuiyan/frontend_v3/dist/collab.html \
  && rm -rf /tmp/fe_rc15_$TS \
  && ls -ld /opt/youjianchuiyan/frontend_v3/dist/case /opt/youjianchuiyan/frontend_v3/dist/case2"

echo "==================== 完成 ===================="
echo "回滚：bash scripts/release/restaurant-rc-15/rollback-rc15.sh $TS"

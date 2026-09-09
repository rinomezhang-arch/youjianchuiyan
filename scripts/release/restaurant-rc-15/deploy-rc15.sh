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

# ---- 判定逻辑统一走 lib.sh，脚本与故障注入台调用同一份实现 ----
RC15_LIB="$(cd "$(dirname "${BASH_SOURCE[0]}")/../release-safe-25" && pwd)/lib.sh"
# shellcheck source=../release-safe-25/lib.sh
. "$RC15_LIB"

# ---- 时间戳必须先定义再被引用 ----
# 上一轮这里直接写了 TRASH_LOCAL=".../$TS"，而 TS 在下面才赋值，
# set -u 下脚本一启动就 exit 1。bash -n 只查语法查不出来，实跑才会暴露。
TS="$(rc15_init_timestamp "${RC15_TS:-}")"
export TS

# ---- 删除一律进回收站，不用 rm ----
# 发布出问题时，暂存件是唯一能看出"到底推了什么上去"的证据，删掉只能靠回忆。
TRASH_LOCAL="${TMPDIR:-/tmp}/rc15_trash/$TS"
TRASH_REMOTE="/home/ubuntu/rc15_trash/$TS"
RC15_TOOLS="$(cd "$(dirname "${BASH_SOURCE[0]}")/../release-safe-25" && pwd)"
REMOTE_TOOLS="/home/ubuntu/rc15_tools/$TS"

# 清单与回退算法在远端执行，先确认 python3 在；没有就停，不做静默降级。
$SSH $HOST "command -v python3 >/dev/null 2>&1 || { echo 'MISSING_PYTHON3 远端没有 python3，清单与回退算法无法运行，发布中止' >&2; exit 1; }"
$SSH $HOST "mkdir -p $REMOTE_TOOLS"
$SCP "$RC15_TOOLS/rc15_restore.py" $HOST:$REMOTE_TOOLS/
# 白名单逐文件写死，后端与前端各一份；前端的 assets 在发布时按实际推送文件展开
$SSH $HOST "cat > $REMOTE_TOOLS/backend.whitelist <<'"'"'WL'"'"'
src/main/java/com/youjian/banquet/config/JwtAuthInterceptor.java
src/main/java/com/youjian/banquet/aop/StoreDataScopeAspect.java
src/main/java/com/youjian/banquet/aop/AuditLogAspect.java
src/main/java/com/youjian/banquet/util/UserContext.java
src/main/java/com/youjian/banquet/auth/StaffRealtimeGuard.java
src/main/java/com/youjian/banquet/controller/IpadOrderController.java
src/main/java/com/youjian/banquet/controller/AuthController.java
src/main/java/com/youjian/banquet/service/IpadBatchAuthorizationService.java
src/main/java/com/youjian/banquet/service/IpadBatchSubmissionService.java
src/main/resources/ipad_batch_request_migration_v1.sql
WL"
trash_local() {
  mkdir -p "$TRASH_LOCAL"
  for p in "$@"; do
    if [ -e "$p" ]; then mv -f "$p" "$TRASH_LOCAL"/; fi
  done
  echo "已移入本地回收站 $TRASH_LOCAL: $*"
}

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

echo "==================== 0b. v2 结构门槛（缺规范结构即停）===================="
# r1 我只在 lib.sh 里定义了 rc15_require_schema 却没调用，等于没有门槛。
# 这里显式查一次：ipad_batch_request 的复合外键（TL27 指出 v2 缺失）。
# 查不到就停，不自行补半成品 SQL——完整迁移由天龙专卡交付。
$SSH $HOST 'set -e
  source ~/.banquet_env.sh >/dev/null 2>&1; export MYSQL_PWD="$MYSQL_PASSWORD"
  n=$(mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" -N -e "SELECT COUNT(*) FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='"'"'ipad_batch_request'"'"' AND CONSTRAINT_TYPE='"'"'FOREIGN KEY'"'"';" 2>/dev/null || echo 0)
  if [ "${n:-0}" -ge 1 ]; then
    echo "OK schema 前置满足：ipad_batch_request 已有外键约束 ($n)"
  else
    echo "SCHEMA_GATE_BLOCKED 缺 ipad_batch_request 复合外键(v2)，迁移不执行，等待天龙专卡" >&2
    exit 1
  fi'

echo "==================== 1. 数据库：先全量备份，再幂等迁移 ===================="
# 上传工资迁移（CO-PAYROLL-MIGRATION-FIX-20 reviewed，27d66303+ec58072c；幂等+深度自愈+微秒前置拒绝）
$SCP "$WORKTREE/scripts/migrations/payroll_approval_payout_v1.sql" $HOST:/tmp/payroll_approval_payout_v1.rc15-$TS.sql
$SCP "$WORKTREE/scripts/migrations/restaurant_print_config_v1.sql" $HOST:/tmp/restaurant_print_config_v1.rc15-$TS.sql
$SSH $HOST 'set -e
  source ~/.banquet_env.sh >/dev/null 2>&1; export MYSQL_PWD="$MYSQL_PASSWORD"
  mkdir -p ~/db_backups
  OUT=~/db_backups/banquet-full-rc15-$(date +%Y%m%d-%H%M%S).sql.gz
  # 原写法管道退出码是 gzip 的，mysqldump 失败只会得到一个空包，还被当成备份成功。
  set -o pipefail
  if ! mysqldump -u"$MYSQL_USER" --single-transaction --routines --triggers --databases "$MYSQL_DATABASE" | gzip > "$OUT"; then
    echo "ABORT 数据库备份失败（导出或压缩），发布中止" >&2; exit 1
  fi
  if [ ! -s "$OUT" ]; then echo "ABORT 备份文件为空: $OUT" >&2; exit 1; fi
  if ! zcat "$OUT" | grep -q "Dump completed"; then
    echo "ABORT 备份内容不完整（缺 Dump completed）: $OUT" >&2; exit 1
  fi
  echo "DB backup: $OUT"
  echo "--- 1a. 工资审批/支付迁移（幂等自愈）---"
  # 原写法 mysql ... && echo OK 把"成功"和"继续往下"绑在一起，
  # 读的人以为错误被处理了，实际后面的 mv 与后续迁移照样执行。改成显式 if/else。
  if mysql -u"$MYSQL_USER" "$MYSQL_DATABASE" < /tmp/payroll_approval_payout_v1.rc15-'"$TS"'.sql; then
    echo "OK payroll migration applied"
  else
    echo "ABORT payroll migration failed，后续迁移与文件移动全部停止" >&2
    exit 1
  fi
  mkdir -p '"$TRASH_REMOTE"' && mv -f /tmp/payroll_approval_payout_v1.rc15-'"$TS"'.sql '"$TRASH_REMOTE"'/
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
  mkdir -p '"$TRASH_REMOTE"' && mv -f /tmp/restaurant_print_config_v1.rc15-'"$TS"'.sql '"$TRASH_REMOTE"'/
'

echo "==================== 2. 备份线上源码与 jar ===================="
# 除了 tar，另存一份发布前的文件清单。原因：回退时解 tar 只会覆盖和补齐，
# 不会删除本次发布"新增"的源码文件（例如 auth/StaffRealtimeGuard.java）。
# 没有清单就无从判断哪些是新增的，回退后这些文件留在盘上，下次构建又被编进 jar，
# 回退等于没退干净。
$SSH $HOST "set -e
  mkdir -p ~/deploy_backups
  cd $REMOTE
  tar czf ~/deploy_backups/src-rc15-$TS.tgz src/main
  # 清单由共享实现生成（deploy 与 rollback 用同一份代码），不再在 shell 里手写哈希循环。
  python3 $REMOTE_TOOLS/rc15_restore.py record --root . \
    --whitelist $REMOTE_TOOLS/backend.whitelist \
    --out ~/deploy_backups/src-rc15-$TS.pre.manifest
  # 备份体：白名单里当前存在的文件逐个复制进备份树，回退时按哈希核对后才用
  while IFS=$'\t' read -r h p; do
    if [ "$h" != ABSENT ]; then
      mkdir -p ~/deploy_backups/src-rc15-$TS.files/"$(dirname "$p")"
      cp -f "$p" ~/deploy_backups/src-rc15-$TS.files/"$p"
    fi
  done < ~/deploy_backups/src-rc15-$TS.pre.manifest
  cp target/banquet-1.0.0.jar ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS
  ls -lh ~/deploy_backups/src-rc15-$TS.tgz ~/deploy_backups/src-rc15-$TS.manifest ~/deploy_backups/banquet-1.0.0.jar.rc15-$TS"

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
trash_local "$STAGE"

echo "==================== 3b. 记录发布后哈希并当场核对 ===================="
# 回退要判断的是"当前文件是否仍等于本次发布推上去的样子"，所以必须有 post 清单。
# r1 缺这一份，回退只能拿 pre 去 cp，等于把别人后来的改动一并覆盖。
$SSH $HOST "set -e
  cd $REMOTE
  python3 $REMOTE_TOOLS/rc15_restore.py record --root . \
    --whitelist $REMOTE_TOOLS/backend.whitelist \
    --out ~/deploy_backups/src-rc15-$TS.post.manifest
  python3 $REMOTE_TOOLS/rc15_restore.py verify --root . \
    --manifest ~/deploy_backups/src-rc15-$TS.post.manifest"

echo "==================== 4. 服务器编译（先编译后切换） ===================="
# 原写法是 mvn ... 2>&1 | tail -20。管道的退出码是最后一个命令 tail 的，永远是 0，
# 所以编译失败也照样往下走，直到后面以别的形式炸出来才被发现。
# 远端 shell 没开 pipefail，靠管道解决不了，这里改成不用管道：
# 输出落盘，用 mvn 自己的退出码判断，失败立刻中止发布。
$SSH $HOST "cd $REMOTE
  set -e
  if mvn -DskipTests package > /tmp/mvn-rc15-$TS.log 2>&1; then
    tail -20 /tmp/mvn-rc15-$TS.log
  else
    echo 'BUILD_FAILED 编译失败，发布中止。完整日志见 /tmp/mvn-rc15-$TS.log，末尾如下：'
    tail -60 /tmp/mvn-rc15-$TS.log
    exit 1
  fi
  ls -l target/banquet-1.0.0.jar"

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
# 原循环轮询 45 次后没有失败分支：一直起不来也只是循环自然结束，脚本继续做前端发布，
# 于是"后端挂着、前端已经换成新版"这种最难收拾的状态就出现了。改成超时即失败。
$SSH $HOST 'ok=0
  for i in $(seq 1 45); do
    c=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/api/legal/me || true)
    if [ "$c" = "401" ]; then ok=1; echo "后端起来了（法务入口 401 符合预期）"; break; fi
    sleep 2
  done
  if [ "$ok" != "1" ]; then echo "HEALTH_TIMEOUT 后端 90 秒内没起来，发布中止，前端不做切换"; exit 1; fi'

echo "==================== 7. 发布后自检 ===================="
$SSH $HOST 'set -e
  curl -s -o /dev/null -w "首页=%{http_code}\n" https://youjianchuiyan.com/
  curl -s -o /dev/null -w "登录页=%{http_code}\n" https://youjianchuiyan.com/login
  curl -s -o /dev/null -w "法务页=%{http_code}\n" https://youjianchuiyan.com/case/
  echo -n "错误密码统一文案: "; curl -s -X POST http://127.0.0.1:8080/api/auth/login -H "Content-Type: application/json" -d "{\"username\":\"__no_such_user__\",\"password\":\"x\"}" | grep -o "账号或密码不正确" || echo "缺失(需排查)"'

echo "==================== 8. 前端发布（只覆盖 index/collab/assets，保留 /case /case2） ===================="
$SSH $HOST "mkdir -p /tmp/fe_rc15_$TS"
$SCP -r "$WORKTREE/frontend_v3/dist/index.html" "$WORKTREE/frontend_v3/dist/collab.html" "$WORKTREE/frontend_v3/dist/assets" $HOST:/tmp/fe_rc15_$TS/
# 覆盖前先整包备份 dist。原脚本直接 sudo cp 覆盖，一旦新前端有问题，
# 回退脚本里只能写一句"如无备份，旧前端仍可工作"——那是把风险留给未来的人。
# 前端也走逐文件清单，不再只留一个整包 tar。
# 整包备份在回退时只能整包还原，会把别人后发的资源和法务共用资源一起盖掉；
# 逐文件清单才能只碰本次推送的那几个。白名单 = index.html + collab.html + 本次实际推送的每个 asset。
$SSH $HOST "set -e
  sudo mkdir -p /home/ubuntu/deploy_backups
  cd /tmp/fe_rc15_$TS
  { echo index.html; echo collab.html; find assets -type f | LC_ALL=C sort; } > $REMOTE_TOOLS/frontend.whitelist
  cd /opt/youjianchuiyan/frontend_v3/dist
  sudo python3 $REMOTE_TOOLS/rc15_restore.py record --root .     --whitelist $REMOTE_TOOLS/frontend.whitelist     --out /home/ubuntu/deploy_backups/fe-rc15-$TS.pre.manifest
  # 备份体：清单里当前存在的逐个复制，回退时按哈希核对后才用
  while IFS=\$'"'"'	'"'"' read -r h f; do
    if [ \"\$h\" != ABSENT ]; then
      sudo mkdir -p /home/ubuntu/deploy_backups/fe-rc15-$TS.files/\"\$(dirname \$f)\"
      sudo cp -f \"\$f\" /home/ubuntu/deploy_backups/fe-rc15-$TS.files/\"\$f\"
    fi
  done < /home/ubuntu/deploy_backups/fe-rc15-$TS.pre.manifest
  sudo chown -R ubuntu:ubuntu /home/ubuntu/deploy_backups/fe-rc15-$TS.files /home/ubuntu/deploy_backups/fe-rc15-$TS.pre.manifest
  wc -l < /home/ubuntu/deploy_backups/fe-rc15-$TS.pre.manifest | xargs echo '前端白名单条目:'"
$SSH $HOST "sudo cp -r /tmp/fe_rc15_$TS/assets/. /opt/youjianchuiyan/frontend_v3/dist/assets/ \
  && sudo cp /tmp/fe_rc15_$TS/index.html /opt/youjianchuiyan/frontend_v3/dist/index.html \
  && sudo cp /tmp/fe_rc15_$TS/collab.html /opt/youjianchuiyan/frontend_v3/dist/collab.html \
  && sudo chown -R www-data:www-data /opt/youjianchuiyan/frontend_v3/dist/assets /opt/youjianchuiyan/frontend_v3/dist/index.html /opt/youjianchuiyan/frontend_v3/dist/collab.html \
  && mkdir -p $TRASH_REMOTE && mv -f /tmp/fe_rc15_$TS $TRASH_REMOTE/ \
  && ls -ld /opt/youjianchuiyan/frontend_v3/dist/case /opt/youjianchuiyan/frontend_v3/dist/case2"

# 前端发布后同样记 post 并当场自校验——回退要判断的是"当前是否仍等于本次推上去的样子"
$SSH $HOST "set -e
  cd /opt/youjianchuiyan/frontend_v3/dist
  sudo python3 $REMOTE_TOOLS/rc15_restore.py record --root .     --whitelist $REMOTE_TOOLS/frontend.whitelist     --out /home/ubuntu/deploy_backups/fe-rc15-$TS.post.manifest
  sudo chown ubuntu:ubuntu /home/ubuntu/deploy_backups/fe-rc15-$TS.post.manifest
  python3 $REMOTE_TOOLS/rc15_restore.py verify --root .     --manifest /home/ubuntu/deploy_backups/fe-rc15-$TS.post.manifest"

echo "==================== 完成 ===================="
echo "回滚：bash scripts/release/restaurant-rc-15/rollback-rc15.sh $TS"

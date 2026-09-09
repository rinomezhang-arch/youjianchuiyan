# shellcheck shell=bash
# CL-RC15-RELEASE-SAFE-25 发布判定库
#
# 上一轮的故障注入台是把判定逻辑"照着抄了一遍"再测抄件。统筹指出得对：
# 抄件和原件会漂移，测抄件证明不了原件。所以把真正要判定的逻辑收进这个文件，
# deploy / rollback / 故障注入台三方 source 同一份实现，测的就是跑的那份。
#
# 本文件只做判断与登记，不发起任何网络调用，不动生产。

# ---------------------------------------------------------------------------
# 时间戳：必须先定义再被引用。
# 上一轮我把 TRASH_LOCAL="...$TS" 写在 TS 赋值之前，set -u 下脚本一启动就 exit 1。
# bash -n 只查语法查不出这个，统筹实跑才发现。这里给一个显式的初始化入口，
# 顺序固定：先定 TS，验证格式，再构造依赖 TS 的路径。
# ---------------------------------------------------------------------------
rc15_init_timestamp() {
  local ts="${1:-}"
  if [ -z "$ts" ]; then
    ts="$(date +%Y%m%d-%H%M%S)"
  fi
  case "$ts" in
    [0-9][0-9][0-9][0-9][0-9][0-9][0-9][0-9]-[0-9][0-9][0-9][0-9][0-9][0-9]) ;;
    *) echo "BAD_TIMESTAMP 时间戳格式必须是 YYYYmmdd-HHMMSS，收到: $ts" >&2; return 1 ;;
  esac
  printf '%s' "$ts"
}

# ---------------------------------------------------------------------------
# 白名单：本次发布实际推送的文件，逐条写死。
#
# 上一轮回退用的是"发布前后遍历整个 src/main，多出来的就算本次新增"。
# 那是错的，而且危险：发布之后别人也会往 src 里加文件，法务的改动同样在 src 下，
# 按那个口径回退会把别人的东西和冻结的法务一起归档走。
# 回退只能碰这张表里的路径，别的一律不碰。
# ---------------------------------------------------------------------------
rc15_backend_whitelist() {
  cat <<'PATHS'
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
PATHS
}

# 前端同理：只碰这三项，绝不整包移动 dist。/case /case2 不在表内。
rc15_frontend_whitelist() {
  cat <<'PATHS'
index.html
collab.html
assets
PATHS
}

# 冻结哨兵：这些路径任何时候都不该被本次发布改动，回退也不许碰。
rc15_frozen_sentinels() {
  cat <<'PATHS'
src/main/java/com/youjian/banquet/controller/LegalController.java
src/main/java/com/youjian/banquet/service/LegalEvidenceService.java
PATHS
}

# ---------------------------------------------------------------------------
# 哈希清单：记录与比对。
# 回退的前提是"这个文件自本次发布之后没被别人动过"，靠哈希判定，不靠假设。
# ---------------------------------------------------------------------------
rc15_hash_file() {
  local f="$1"
  if [ ! -e "$f" ]; then printf 'ABSENT'; return 0; fi
  if [ -d "$f" ]; then
    # 目录按内容逐文件哈希再汇总，避免只看目录本身看不出内容变化
    find "$f" -type f -print0 | LC_ALL=C sort -z \
      | xargs -0 -r sha256sum 2>/dev/null | sha256sum | cut -d' ' -f1
    return 0
  fi
  sha256sum "$f" 2>/dev/null | cut -d' ' -f1
}

# 把一批路径的哈希写成清单：每行 "<hash>\t<path>"
rc15_write_manifest() {
  local root="$1" out="$2" p
  : > "$out"
  while IFS= read -r p; do
    [ -n "$p" ] || continue
    printf '%s\t%s\n' "$(rc15_hash_file "$root/$p")" "$p" >> "$out"
  done
  [ -s "$out" ] || { echo "EMPTY_MANIFEST 清单为空，输入不齐，按 fail-closed 中止" >&2; return 1; }
}

# 校验：当前哈希必须与清单一致。任一漂移即非零退出，绝不自行覆盖。
rc15_verify_manifest() {
  local root="$1" manifest="$2" drift=0 want path now
  [ -s "$manifest" ] || { echo "MISSING_MANIFEST 找不到或为空: $manifest" >&2; return 1; }
  while IFS="$(printf '\t')" read -r want path; do
    [ -n "$path" ] || continue
    now="$(rc15_hash_file "$root/$path")"
    if [ "$now" != "$want" ]; then
      echo "DRIFT $path 期望=$want 实际=$now" >&2
      drift=$((drift + 1))
    fi
  done < "$manifest"
  if [ "$drift" -ne 0 ]; then
    echo "DRIFT_DETECTED 共 $drift 处与本次发布结果不一致，停止，不自行覆盖" >&2
    return 1
  fi
  return 0
}

# ---------------------------------------------------------------------------
# 迁移与命令的控制流：显式 if/else，不用 a && b || c。
# a && b || c 在 b 失败时会掉进 c，把"成功"打印成"失败分支"，反过来也一样；
# 更要紧的是它让人以为已经处理了错误，实际错误被吞掉、后续步骤照常执行。
# ---------------------------------------------------------------------------
rc15_run_or_abort() {
  local label="$1"; shift
  if "$@"; then
    echo "OK $label"
    return 0
  fi
  echo "ABORT $label 失败，后续步骤全部停止" >&2
  return 1
}

# 备份管道：mysqldump 与 gzip 任一失败都必须被发现。
# 原写法 mysqldump ... | gzip > OUT 的退出码是 gzip 的，导出失败只会得到一个空包。
rc15_backup_pipeline() {
  local dump_cmd="$1" gzip_cmd="$2" out="$3"
  set -o pipefail
  if ! "$dump_cmd" | "$gzip_cmd" > "$out"; then
    echo "BACKUP_PIPELINE_FAILED 导出或压缩失败: $out" >&2
    return 1
  fi
  if [ ! -s "$out" ]; then
    echo "BACKUP_EMPTY 备份文件为空: $out" >&2
    return 1
  fi
  return 0
}

# ---------------------------------------------------------------------------
# 迁移前置结构门槛（TL27 指出 v2 复合外键缺失）。
# 这里只做"缺规范结构就停"，不复制半成品 SQL——完整迁移与 schema 守卫由天龙专卡交付。
# ---------------------------------------------------------------------------
rc15_require_schema() {
  local probe_cmd="$1" what="$2"
  if "$probe_cmd"; then
    echo "OK schema 前置满足: $what"
    return 0
  fi
  echo "SCHEMA_GATE_BLOCKED 缺少必需结构: $what。迁移不执行，等待天龙专卡交付完整定义" >&2
  return 1
}

# 发布输入齐备性：任一缺失即 fail closed
rc15_require_inputs() {
  local missing=0 f
  for f in "$@"; do
    if [ ! -e "$f" ]; then echo "MISSING_INPUT $f" >&2; missing=$((missing + 1)); fi
  done
  if [ "$missing" -ne 0 ]; then
    echo "INPUTS_INCOMPLETE 缺 $missing 项发布输入，按 fail-closed 中止" >&2
    return 1
  fi
  return 0
}

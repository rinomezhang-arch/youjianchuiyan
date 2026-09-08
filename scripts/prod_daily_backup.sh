#!/usr/bin/env bash
# Production database backup. Credentials are sourced locally and never echoed.
set -Eeuo pipefail
umask 077

ENV_FILE="${BANQUET_ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
BACKUP_DIR="${BACKUP_DIR:-/home/ubuntu/db_backups}"
LOCAL_BACKUP_ROOT="${LOCAL_BACKUP_ROOT:-/home/ubuntu}"
COS_DIR="${COS_BACKUP_DIR:-/mnt/cos/ai公共工作空间/项目管理/餐饮管理系统/数据库备份}"
COS_MOUNT_ROOT="${COS_MOUNT_ROOT:-/mnt/cos}"
COS_REQUIRE_MOUNT="${COS_REQUIRE_MOUNT:-1}"
KEEP_DAYS="${KEEP_DAYS:-30}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQLDUMP_BIN="${MYSQLDUMP_BIN:-mysqldump}"
GZIP_BIN="${GZIP_BIN:-gzip}"
SHA256_BIN="${SHA256_BIN:-sha256sum}"
FLOCK_BIN="${FLOCK_BIN:-flock}"
MOUNTPOINT_BIN="${MOUNTPOINT_BIN:-mountpoint}"
REALPATH_BIN="${REALPATH_BIN:-realpath}"
CP_BIN="${CP_BIN:-cp}"
RUN_TS="${RUN_TS:-$(date +%Y%m%d-%H%M%S)}"

log() { printf '[%s] %s\n' "$(date -Is)" "$*"; }
fail() { log "ERROR: $2" >&2; exit "$1"; }
require_absolute_dir() {
  case "$1" in /*) ;; *) fail 10 "$2 must be an absolute path" ;; esac
}
reject_parent_segments() {
  case "/${1#/}/" in */../*) fail 15 "$2 contains a parent-directory segment" ;; esac
}
canonical_path() {
  "$REALPATH_BIN" -m -- "$1" 2>/dev/null || fail 15 "cannot canonicalize $2"
}
require_within() {
  local candidate="$1" root="$2" label="$3"
  case "$candidate" in "$root"/*) ;; *) fail 15 "$label escapes its allowed root" ;; esac
}
ensure_safe_descendant() {
  local root="$1" relative="$2" label="$3" current="$1" component candidate canonical
  local -a components
  IFS='/' read -r -a components <<<"$relative"
  for component in "${components[@]}"; do
    [[ -n "$component" && "$component" != . && "$component" != .. ]] || fail 16 "$label has an unsafe component"
    candidate="$current/$component"
    [[ ! -L "$candidate" ]] || fail 16 "$label contains a symbolic-link directory"
    if [[ ! -e "$candidate" ]]; then
      mkdir -- "$candidate" || fail 16 "cannot create $label"
    fi
    [[ -d "$candidate" && ! -L "$candidate" ]] || fail 16 "$label is not a real directory"
    canonical="$(canonical_path "$candidate" "$label")"
    require_within "$canonical" "$root" "$label"
    current="$canonical"
  done
}
require_absolute_dir "$BACKUP_DIR" BACKUP_DIR
require_absolute_dir "$LOCAL_BACKUP_ROOT" LOCAL_BACKUP_ROOT
require_absolute_dir "$COS_DIR" COS_BACKUP_DIR
require_absolute_dir "$COS_MOUNT_ROOT" COS_MOUNT_ROOT
reject_parent_segments "$BACKUP_DIR" BACKUP_DIR
reject_parent_segments "$LOCAL_BACKUP_ROOT" LOCAL_BACKUP_ROOT
reject_parent_segments "$COS_DIR" COS_BACKUP_DIR
reject_parent_segments "$COS_MOUNT_ROOT" COS_MOUNT_ROOT
[[ -d "$LOCAL_BACKUP_ROOT" && ! -L "$LOCAL_BACKUP_ROOT" ]] || fail 15 "LOCAL_BACKUP_ROOT must be a real directory"
[[ -d "$COS_MOUNT_ROOT" && ! -L "$COS_MOUNT_ROOT" ]] || fail 15 "COS_MOUNT_ROOT must be a real directory"
LOCAL_BACKUP_ROOT="$(canonical_path "$LOCAL_BACKUP_ROOT" LOCAL_BACKUP_ROOT)"
COS_MOUNT_ROOT="$(canonical_path "$COS_MOUNT_ROOT" COS_MOUNT_ROOT)"
BACKUP_DIR="$(canonical_path "$BACKUP_DIR" BACKUP_DIR)"
COS_DIR="$(canonical_path "$COS_DIR" COS_BACKUP_DIR)"
require_within "$BACKUP_DIR" "$LOCAL_BACKUP_ROOT" BACKUP_DIR
require_within "$COS_DIR" "$COS_MOUNT_ROOT" COS_BACKUP_DIR
[[ "$KEEP_DAYS" =~ ^[0-9]+$ ]] || fail 10 "KEEP_DAYS must be a non-negative integer"
[[ "$MYSQL_PORT" =~ ^[0-9]+$ ]] || fail 10 "MYSQL_PORT must be numeric"
[[ "$COS_REQUIRE_MOUNT" == 0 || "$COS_REQUIRE_MOUNT" == 1 ]] || fail 10 "COS_REQUIRE_MOUNT must be 0 or 1"
[[ -r "$ENV_FILE" ]] || fail 11 "credential environment file is not readable"

# shellcheck disable=SC1090
source "$ENV_FILE" >/dev/null 2>&1
: "${MYSQL_USER:?MYSQL_USER is required in the environment file}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required in the environment file}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE is required in the environment file}"
[[ "$MYSQL_DATABASE" =~ ^[A-Za-z0-9_]+$ ]] || fail 12 "MYSQL_DATABASE contains unsafe characters"
export MYSQL_PWD="$MYSQL_PASSWORD"

mkdir -p "$BACKUP_DIR"
ensure_safe_descendant "$BACKUP_DIR" .trash "local trash path"
LOCK_FILE="$BACKUP_DIR/.backup.lock"
exec 9>"$LOCK_FILE"
"$FLOCK_BIN" -n 9 || fail 13 "another backup run is active"

OUT="$BACKUP_DIR/banquet-full-$RUN_TS.sql.gz"
[[ ! -e "$OUT" ]] || fail 14 "backup output already exists for this timestamp"

move_to_trash() {
  local file="$1" reason="$2" root="$3"
  [[ -e "$file" ]] || return 0
  ensure_safe_descendant "$root" ".trash/$RUN_TS/$reason" "trash destination"
  local destination="$root/.trash/$RUN_TS/$reason"
  mv -- "$file" "$destination/"
  log "moved $(basename "$file") to timestamped trash ($reason)"
}

COPY_TMP=""
cleanup() {
  local status=$?
  trap - EXIT HUP INT TERM
  if [[ -n "$COPY_TMP" && -e "$COPY_TMP" ]]; then
    set +e
    move_to_trash "$COPY_TMP" copy-incomplete "$COS_DIR"
    set -e
  fi
  unset MYSQL_PWD MYSQL_PASSWORD
  exit "$status"
}
trap cleanup EXIT
trap 'exit 129' HUP
trap 'exit 130' INT
trap 'exit 143' TERM

archive_expired() {
  local root="$1"
  [[ -d "$root" ]] || return 0
  while IFS= read -r -d '' old_file; do
    move_to_trash "$old_file" expired "$root"
  done < <(find "$root" -maxdepth 1 -type f -name 'banquet-full-*.sql.gz' -mtime "+$KEEP_DAYS" -print0)
}

log "starting database backup"
set +e
"$MYSQLDUMP_BIN" \
  --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" \
  --single-transaction --routines --triggers --events --hex-blob \
  --default-character-set=utf8mb4 --set-gtid-purged=OFF \
  "$MYSQL_DATABASE" 2>/dev/null | "$GZIP_BIN" -c >"$OUT"
pipeline_status=("${PIPESTATUS[@]}")
dump_status=${pipeline_status[0]:-1}
gzip_status=${pipeline_status[1]:-1}
set -e

if (( dump_status != 0 || gzip_status != 0 )); then
  move_to_trash "$OUT" failed "$BACKUP_DIR"
  fail 20 "database dump pipeline failed"
fi
if ! "$GZIP_BIN" -t "$OUT"; then
  move_to_trash "$OUT" invalid "$BACKUP_DIR"
  fail 21 "backup integrity validation failed"
fi
last_nonempty=$("$GZIP_BIN" -cd "$OUT" | awk 'NF { line=$0 } END { print line }')
if [[ ! "$last_nonempty" =~ ^--[[:space:]]Dump[[:space:]]completed[[:space:]]on[[:space:]][0-9]{4}-[0-9]{2}-[0-9]{2}[[:space:]][0-9]{2}:[0-9]{2}:[0-9]{2}$ ]]; then
  move_to_trash "$OUT" invalid "$BACKUP_DIR"
  fail 21 "backup integrity validation failed"
fi

if [[ "$COS_REQUIRE_MOUNT" == 1 ]]; then
  "$MOUNTPOINT_BIN" -q "$COS_MOUNT_ROOT" || fail 22 "backup mount is unavailable; local backup retained"
fi
[[ -d "$(dirname "$COS_DIR")" ]] || fail 22 "backup copy parent is unavailable; local backup retained"
mkdir -p "$COS_DIR"
ensure_safe_descendant "$COS_DIR" .trash "copy trash path"
COPY="$COS_DIR/$(basename "$OUT")"
[[ ! -e "$COPY" ]] || fail 24 "backup copy already exists; local backup retained"
COPY_TMP="$COS_DIR/.$(basename "$OUT").partial.$RUN_TS.$$"
[[ ! -e "$COPY_TMP" ]] || fail 24 "backup temporary copy already exists; local backup retained"
"$CP_BIN" -- "$OUT" "$COPY_TMP" || fail 25 "backup copy failed; local backup retained"
local_hash=$("$SHA256_BIN" "$OUT" | awk '{print $1}')
copy_hash=$("$SHA256_BIN" "$COPY_TMP" | awk '{print $1}')
[[ "$local_hash" == "$copy_hash" ]] || fail 23 "backup copy hash mismatch; local backup retained"
mv -- "$COPY_TMP" "$COPY"
COPY_TMP=""

archive_expired "$BACKUP_DIR"
archive_expired "$COS_DIR"
log "backup completed and copy hash verified: $(basename "$OUT")"

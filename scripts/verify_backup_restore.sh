#!/usr/bin/env bash
# Restore a backup only into a dedicated loopback verification schema.
set -Eeuo pipefail
umask 077

[[ $# -eq 2 ]] || { echo "usage: $0 BACKUP.sql.gz restore_verify_SCHEMA" >&2; exit 2; }
BACKUP_FILE="$1"
TARGET_SCHEMA="$2"
ENV_FILE="${BANQUET_ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
MYSQL_HOST="${RESTORE_MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${RESTORE_MYSQL_PORT:-13317}"
RESTORE_PREFIX="restore_verify_"
RESTORE_BACKUP_ROOT="${RESTORE_BACKUP_ROOT:-/home/ubuntu/db_backups}"
GZIP_BIN="${GZIP_BIN:-gzip}"
REALPATH_BIN="${REALPATH_BIN:-realpath}"

case "$MYSQL_HOST" in 127.0.0.1|localhost) ;; *) echo "restore host must be loopback" >&2; exit 30 ;; esac
[[ "$MYSQL_PORT" =~ ^[0-9]+$ ]] || { echo "restore port must be numeric" >&2; exit 30; }
(( ${#MYSQL_PORT} <= 5 )) || { echo "restore port is out of range" >&2; exit 30; }
MYSQL_PORT=$((10#$MYSQL_PORT))
(( MYSQL_PORT >= 1 && MYSQL_PORT <= 65535 )) || { echo "restore port is out of range" >&2; exit 30; }
(( MYSQL_PORT != 3306 )) || { echo "restore verification refuses port 3306" >&2; exit 30; }
[[ "$TARGET_SCHEMA" =~ ^[A-Za-z0-9_]+$ && "$TARGET_SCHEMA" == "$RESTORE_PREFIX"* ]] || {
  echo "target schema must use the verification prefix" >&2; exit 31;
}
case "$BACKUP_FILE" in /*) ;; *) echo "backup path must be absolute" >&2; exit 32 ;; esac
case "$RESTORE_BACKUP_ROOT" in /*) ;; *) echo "backup root must be absolute" >&2; exit 32 ;; esac
case "/${BACKUP_FILE#/}/" in */../*) echo "backup path contains a parent-directory segment" >&2; exit 32 ;; esac
case "/${RESTORE_BACKUP_ROOT#/}/" in */../*) echo "backup root contains a parent-directory segment" >&2; exit 32 ;; esac
[[ -d "$RESTORE_BACKUP_ROOT" && ! -L "$RESTORE_BACKUP_ROOT" ]] || { echo "backup root must be a real directory" >&2; exit 32; }
[[ ! -L "$BACKUP_FILE" ]] || { echo "symbolic-link backup files are refused" >&2; exit 32; }
RESTORE_BACKUP_ROOT=$("$REALPATH_BIN" -m -- "$RESTORE_BACKUP_ROOT" 2>/dev/null) || { echo "cannot canonicalize backup root" >&2; exit 32; }
BACKUP_FILE=$("$REALPATH_BIN" -m -- "$BACKUP_FILE" 2>/dev/null) || { echo "cannot canonicalize backup path" >&2; exit 32; }
case "$BACKUP_FILE" in "$RESTORE_BACKUP_ROOT"/*) ;; *) echo "backup path escapes the allowed root" >&2; exit 32 ;; esac
[[ -r "$BACKUP_FILE" ]] || { echo "backup is not readable" >&2; exit 32; }
[[ -r "$ENV_FILE" ]] || { echo "credential environment file is not readable" >&2; exit 32; }

# shellcheck disable=SC1090
source "$ENV_FILE" >/dev/null 2>&1
: "${MYSQL_USER:?MYSQL_USER is required in the environment file}"
: "${MYSQL_PASSWORD:?MYSQL_PASSWORD is required in the environment file}"
export MYSQL_PWD="$MYSQL_PASSWORD"
trap 'unset MYSQL_PWD MYSQL_PASSWORD' EXIT

"$GZIP_BIN" -t "$BACKUP_FILE" || { echo "backup gzip validation failed" >&2; exit 33; }
last_nonempty=$("$GZIP_BIN" -cd "$BACKUP_FILE" | awk 'NF { line=$0 } END { print line }')
[[ "$last_nonempty" =~ ^--[[:space:]]Dump[[:space:]]completed[[:space:]]on[[:space:]][0-9]{4}-[0-9]{2}-[0-9]{2}[[:space:]][0-9]{2}:[0-9]{2}:[0-9]{2}$ ]] || {
  echo "backup completion marker is missing or not final" >&2; exit 33;
}
if ! "$GZIP_BIN" -cd "$BACKUP_FILE" | awk '
  BEGIN {
    ident="(`?[a-z_][a-z0-9_$]*`?)"
    qualified=ident "[[:space:]]*\\.[[:space:]]*" ident
  }
  {
    line=tolower($0)
    if (line ~ /^[[:space:]]*use[[:space:]]+[`a-z0-9_]+/) exit 1
    if (line ~ /(^|[[:space:];])(create|drop)[[:space:]]+(database|schema)([[:space:]]|$)/) exit 1
    if (line ~ ("create([^;]*[[:space:]])?(table|view|trigger|procedure|function|event)[[:space:]]+(if[[:space:]]+not[[:space:]]+exists[[:space:]]+)?" qualified)) exit 1
    if (line ~ ("(alter|drop|truncate)[[:space:]]+(table|view|trigger|event|procedure|function)[[:space:]]+(if[[:space:]]+exists[[:space:]]+)?" qualified)) exit 1
    if (line ~ ("rename[[:space:]]+table[[:space:]]+" qualified)) exit 1
    if (line ~ ("(insert|replace)[[:space:]]+into[[:space:]]+" qualified)) exit 1
    if (line ~ ("update[[:space:]]+" qualified)) exit 1
    if (line ~ ("delete[[:space:]]+from[[:space:]]+" qualified)) exit 1
    if (line ~ ("(from|join)[[:space:]]+" qualified)) exit 1
    if (line ~ ("lock[[:space:]]+tables[[:space:]]+" qualified)) exit 1
    if (line ~ ("references[[:space:]]+" qualified)) exit 1
    if (line ~ ("create([^;]*[[:space:]])?index[^;]*[[:space:]]on[[:space:]]+" qualified)) exit 1
    if (line ~ ("(analyze|check|optimize|repair)[[:space:]]+table[[:space:]]+" qualified)) exit 1
    if (line ~ ("(grant|revoke)[^;]*[[:space:]]on[[:space:]]+" qualified)) exit 1
    if (line ~ ("call[[:space:]]+" qualified)) exit 1
    if (line ~ ("load[[:space:]]+data[^;]*into[[:space:]]+table[[:space:]]+" qualified)) exit 1
  }
'; then
  echo "backup contains database-selection or cross-schema SQL" >&2
  exit 38
fi
expected_tables=$("$GZIP_BIN" -cd "$BACKUP_FILE" | grep -c '^CREATE TABLE ' || true)
(( expected_tables > 0 )) || { echo "backup contains no CREATE TABLE statements" >&2; exit 34; }

mysql_args=(--host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --batch --skip-column-names)
exists=$("$MYSQL_BIN" "${mysql_args[@]}" --execute="SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='$TARGET_SCHEMA'" 2>/dev/null)
[[ "$exists" == 0 ]] || { echo "verification schema already exists; choose a new name" >&2; exit 35; }
"$MYSQL_BIN" "${mysql_args[@]}" --execute="CREATE DATABASE \`$TARGET_SCHEMA\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci" >/dev/null
"$GZIP_BIN" -cd "$BACKUP_FILE" | "$MYSQL_BIN" "${mysql_args[@]}" "$TARGET_SCHEMA" >/dev/null
actual_tables=$("$MYSQL_BIN" "${mysql_args[@]}" --execute="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$TARGET_SCHEMA'" 2>/dev/null)
[[ "$actual_tables" =~ ^[0-9]+$ ]] || { echo "restore table count was not numeric" >&2; exit 36; }
(( actual_tables == expected_tables )) || {
  echo "restore table count mismatch: expected=$expected_tables actual=$actual_tables" >&2
  exit 37
}
printf 'RESTORE_VERIFY_OK schema=%s tables=%s retained=true\n' "$TARGET_SCHEMA" "$actual_tables"

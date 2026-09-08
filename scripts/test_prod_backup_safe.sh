#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

ROOT="$(mktemp -d /tmp/prod-backup-safe-test.XXXXXX)"
BIN="$ROOT/bin"
mkdir -p "$BIN" "$ROOT/local" "$ROOT/copy-parent/copy" "$ROOT/outside"
ENV_FILE="$ROOT/test.env"
cat >"$ENV_FILE" <<'EOF'
MYSQL_USER=test_user
MYSQL_PASSWORD=test_password_not_production
MYSQL_DATABASE=synthetic_backup_db
EOF

cat >"$BIN/fake-mysqldump" <<'EOF'
#!/usr/bin/env bash
case "${FAKE_DUMP_MODE:-ok}" in
  ok) printf '%s\n' 'CREATE TABLE `alpha` (`id` int);' 'INSERT INTO `alpha` VALUES (1);' '-- Dump completed on 2026-09-08 18:00:00' ;;
  no-marker) printf '%s\n' 'CREATE TABLE `alpha` (`id` int);' ;;
  trailing) printf '%s\n' 'CREATE TABLE `alpha` (`id` int);' '-- Dump completed on 2026-09-08 18:00:00' 'TRUNCATED TRAILING DATA' ;;
  cross-db) printf '%s\n' 'CREATE TABLE `alpha` (`id` int);' 'INSERT INTO `other_schema`.`alpha` VALUES (1);' '-- Dump completed on 2026-09-08 18:00:00' ;;
  fail) printf '%s\n' 'partial synthetic output'; exit 7 ;;
esac
EOF
chmod +x "$BIN/fake-mysqldump"

cat >"$BIN/fake-mysql" <<'EOF'
#!/usr/bin/env bash
args="$*"
if [[ "$args" == *"information_schema.schemata"* ]]; then printf '%s\n' "${FAKE_SCHEMA_EXISTS:-0}"; exit 0; fi
if [[ "$args" == *"CREATE DATABASE"* ]]; then exit 0; fi
if [[ "$args" == *"information_schema.tables"* ]]; then printf '%s\n' "${FAKE_RESTORE_TABLES:-1}"; exit 0; fi
cat >/dev/null
EOF
chmod +x "$BIN/fake-mysql"

cat >"$BIN/fake-flock" <<'EOF'
#!/usr/bin/env bash
exit 0
EOF
chmod +x "$BIN/fake-flock"

cat >"$BIN/fake-mountpoint" <<'EOF'
#!/usr/bin/env bash
exit 1
EOF
chmod +x "$BIN/fake-mountpoint"

cat >"$BIN/fake-cp-partial" <<'EOF'
#!/usr/bin/env bash
destination=""
for argument in "$@"; do destination="$argument"; done
printf 'partial-copy' >"$destination"
exit 9
EOF
chmod +x "$BIN/fake-cp-partial"

BACKUP_SCRIPT="$(cd "$(dirname "$0")" && pwd)/prod_daily_backup.sh"
RESTORE_SCRIPT="$(cd "$(dirname "$0")" && pwd)/verify_backup_restore.sh"
passes=0
check() { "$@"; passes=$((passes + 1)); }

run_backup() {
  BANQUET_ENV_FILE="$ENV_FILE" BACKUP_DIR="$ROOT/local" COS_BACKUP_DIR="$ROOT/copy-parent/copy" \
    LOCAL_BACKUP_ROOT="$ROOT" COS_MOUNT_ROOT="$ROOT/copy-parent" \
    MYSQLDUMP_BIN="$BIN/fake-mysqldump" RUN_TS="$1" FAKE_DUMP_MODE="${2:-ok}" KEEP_DAYS="${3:-30}" \
    FLOCK_BIN="$BIN/fake-flock" CP_BIN="${CP_BIN_OVERRIDE:-cp}" COS_REQUIRE_MOUNT=0 \
    bash "$BACKUP_SCRIPT"
}

run_restore() {
  BANQUET_ENV_FILE="$ENV_FILE" MYSQL_BIN="$BIN/fake-mysql" RESTORE_BACKUP_ROOT="$ROOT/local" \
    RESTORE_MYSQL_PORT="${RESTORE_PORT_OVERRIDE:-13317}" \
    bash "$RESTORE_SCRIPT" "$1" "$2"
}

run_backup 20260908-010101 ok
check test -s "$ROOT/local/banquet-full-20260908-010101.sql.gz"
check cmp -s "$ROOT/local/banquet-full-20260908-010101.sql.gz" "$ROOT/copy-parent/copy/banquet-full-20260908-010101.sql.gz"

set +e
run_backup 20260908-020202 no-marker >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 21
check test -s "$ROOT/local/.trash/20260908-020202/invalid/banquet-full-20260908-020202.sql.gz"

set +e
run_backup 20260908-030303 fail >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 20
check test -s "$ROOT/local/.trash/20260908-030303/failed/banquet-full-20260908-030303.sql.gz"

old="$ROOT/local/banquet-full-20000101-000000.sql.gz"
cp "$ROOT/local/banquet-full-20260908-010101.sql.gz" "$old"
touch -d '40 days ago' "$old"
run_backup 20260908-040404 ok 30 >/dev/null
check test ! -e "$old"
check test -s "$ROOT/local/.trash/20260908-040404/expired/banquet-full-20000101-000000.sql.gz"

run_restore "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_success >/dev/null
passes=$((passes + 1))

set +e
FAKE_RESTORE_TABLES=2 run_restore "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_mismatch >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 37

set +e
RESTORE_PORT_OVERRIDE=3306 run_restore "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_prod_port >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 30

set +e
RESTORE_PORT_OVERRIDE=03306 run_restore "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_zero_padded_port >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 30

set +e
RESTORE_SCHEMA_PREFIX=unsafe_ run_restore "$ROOT/local/banquet-full-20260908-040404.sql.gz" unsafe_schema >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 31

set +e
FAKE_SCHEMA_EXISTS=1 run_restore "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_existing >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 35

set +e
BANQUET_ENV_FILE="$ENV_FILE" BACKUP_DIR="$ROOT/local" COS_BACKUP_DIR="$ROOT/copy-parent/copy" \
  COS_MOUNT_ROOT="$ROOT/copy-parent" COS_REQUIRE_MOUNT=1 MOUNTPOINT_BIN="$BIN/fake-mountpoint" \
  LOCAL_BACKUP_ROOT="$ROOT" \
  MYSQLDUMP_BIN="$BIN/fake-mysqldump" FLOCK_BIN="$BIN/fake-flock" RUN_TS=20260908-050505 \
  bash "$BACKUP_SCRIPT" >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 22
check test -s "$ROOT/local/banquet-full-20260908-050505.sql.gz"

set +e
BANQUET_ENV_FILE="$ENV_FILE" MYSQL_BIN="$BIN/fake-mysql" RESTORE_MYSQL_HOST=192.0.2.10 RESTORE_MYSQL_PORT=13317 \
  bash "$RESTORE_SCRIPT" "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_remote >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 30

run_backup 20260908-060606 cross-db >/dev/null
set +e
run_restore "$ROOT/local/banquet-full-20260908-060606.sql.gz" restore_verify_cross_db >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 38

set +e
run_backup 20260908-070707 trailing >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 21
check test -s "$ROOT/local/.trash/20260908-070707/invalid/banquet-full-20260908-070707.sql.gz"

set +e
CP_BIN_OVERRIDE="$BIN/fake-cp-partial" run_backup 20260908-080808 ok >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 25
check test -s "$ROOT/local/banquet-full-20260908-080808.sql.gz"
check test ! -e "$ROOT/copy-parent/copy/banquet-full-20260908-080808.sql.gz"
partial_files=("$ROOT/copy-parent/copy/.trash/20260908-080808/copy-incomplete/".[!.]*)
check test "${#partial_files[@]}" -eq 1
check test -s "${partial_files[0]}"

cp "$ROOT/local/banquet-full-20260908-040404.sql.gz" "$ROOT/outside/outside.sql.gz"
set +e
RESTORE_BACKUP_ROOT="$ROOT/local" BANQUET_ENV_FILE="$ENV_FILE" MYSQL_BIN="$BIN/fake-mysql" RESTORE_MYSQL_PORT=13317 \
  bash "$RESTORE_SCRIPT" "$ROOT/local/../outside/outside.sql.gz" restore_verify_traversal >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 32

link_windows=$(cygpath -w "$ROOT/local/escape-link")
outside_windows=$(cygpath -w "$ROOT/outside")
powershell.exe -NoProfile -NonInteractive -Command "\$null = New-Item -ItemType Junction -Path '$link_windows' -Target '$outside_windows'"
set +e
run_restore "$ROOT/local/escape-link/outside.sql.gz" restore_verify_symlink_escape >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 32

set +e
BANQUET_ENV_FILE="$ENV_FILE" BACKUP_DIR="$ROOT/local/../escape" LOCAL_BACKUP_ROOT="$ROOT" \
  COS_BACKUP_DIR="$ROOT/copy-parent/copy" COS_MOUNT_ROOT="$ROOT/copy-parent" COS_REQUIRE_MOUNT=0 \
  MYSQLDUMP_BIN="$BIN/fake-mysqldump" FLOCK_BIN="$BIN/fake-flock" RUN_TS=20260908-090909 \
  bash "$BACKUP_SCRIPT" >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 15

set +e
BANQUET_ENV_FILE="$ENV_FILE" BACKUP_DIR="$ROOT/local/escape-link" LOCAL_BACKUP_ROOT="$ROOT/local" \
  COS_BACKUP_DIR="$ROOT/copy-parent/copy" COS_MOUNT_ROOT="$ROOT/copy-parent" COS_REQUIRE_MOUNT=0 \
  MYSQLDUMP_BIN="$BIN/fake-mysqldump" FLOCK_BIN="$BIN/fake-flock" RUN_TS=20260908-101010 \
  bash "$BACKUP_SCRIPT" >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 15

if grep -REn '(^|[[:space:]])(rm|unlink)([[:space:]]|$)|-delete([[:space:]]|$)' "$BACKUP_SCRIPT" "$RESTORE_SCRIPT"; then
  echo "forbidden physical deletion command found" >&2
  exit 90
fi
passes=$((passes + 1))

printf 'TESTS_PASS=%s TESTS_FAIL=0 ARTIFACT_ROOT=%s\n' "$passes" "$ROOT"

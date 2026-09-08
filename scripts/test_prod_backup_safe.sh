#!/usr/bin/env bash
set -Eeuo pipefail
umask 077

ROOT="$(mktemp -d /tmp/prod-backup-safe-test.XXXXXX)"
BIN="$ROOT/bin"
mkdir -p "$BIN" "$ROOT/local" "$ROOT/copy-parent/copy"
ENV_FILE="$ROOT/test.env"
cat >"$ENV_FILE" <<'EOF'
MYSQL_USER=test_user
MYSQL_PASSWORD=test_password_not_production
MYSQL_DATABASE=synthetic_backup_db
EOF

cat >"$BIN/fake-mysqldump" <<'EOF'
#!/usr/bin/env bash
case "${FAKE_DUMP_MODE:-ok}" in
  ok) printf '%s\n' 'CREATE TABLE `alpha` (`id` int);' 'INSERT INTO `alpha` VALUES (1);' '-- Dump completed on synthetic test' ;;
  no-marker) printf '%s\n' 'CREATE TABLE `alpha` (`id` int);' ;;
  fail) printf '%s\n' 'partial synthetic output'; exit 7 ;;
esac
EOF
chmod +x "$BIN/fake-mysqldump"

cat >"$BIN/fake-mysql" <<'EOF'
#!/usr/bin/env bash
args="$*"
if [[ "$args" == *"information_schema.schemata"* ]]; then printf '0\n'; exit 0; fi
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

BACKUP_SCRIPT="$(cd "$(dirname "$0")" && pwd)/prod_daily_backup.sh"
RESTORE_SCRIPT="$(cd "$(dirname "$0")" && pwd)/verify_backup_restore.sh"
passes=0
check() { "$@"; passes=$((passes + 1)); }

run_backup() {
  BANQUET_ENV_FILE="$ENV_FILE" BACKUP_DIR="$ROOT/local" COS_BACKUP_DIR="$ROOT/copy-parent/copy" \
    MYSQLDUMP_BIN="$BIN/fake-mysqldump" RUN_TS="$1" FAKE_DUMP_MODE="${2:-ok}" KEEP_DAYS="${3:-30}" \
    FLOCK_BIN="$BIN/fake-flock" COS_REQUIRE_MOUNT=0 \
    bash "$BACKUP_SCRIPT"
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

BANQUET_ENV_FILE="$ENV_FILE" MYSQL_BIN="$BIN/fake-mysql" RESTORE_MYSQL_PORT=13317 \
  bash "$RESTORE_SCRIPT" "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_success >/dev/null
passes=$((passes + 1))

set +e
BANQUET_ENV_FILE="$ENV_FILE" MYSQL_BIN="$BIN/fake-mysql" FAKE_RESTORE_TABLES=2 RESTORE_MYSQL_PORT=13317 \
  bash "$RESTORE_SCRIPT" "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_mismatch >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 37

set +e
BANQUET_ENV_FILE="$ENV_FILE" MYSQL_BIN="$BIN/fake-mysql" RESTORE_MYSQL_PORT=3306 \
  bash "$RESTORE_SCRIPT" "$ROOT/local/banquet-full-20260908-040404.sql.gz" restore_verify_prod_port >/dev/null 2>&1
status=$?
set -e
check test "$status" -eq 30

set +e
BANQUET_ENV_FILE="$ENV_FILE" BACKUP_DIR="$ROOT/local" COS_BACKUP_DIR="$ROOT/copy-parent/copy" \
  COS_MOUNT_ROOT="$ROOT/copy-parent" COS_REQUIRE_MOUNT=1 MOUNTPOINT_BIN="$BIN/fake-mountpoint" \
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

if grep -REn '(^|[[:space:]])(rm|unlink)([[:space:]]|$)|-delete([[:space:]]|$)' "$BACKUP_SCRIPT" "$RESTORE_SCRIPT"; then
  echo "forbidden physical deletion command found" >&2
  exit 90
fi
passes=$((passes + 1))

printf 'TESTS_PASS=%s TESTS_FAIL=0 ARTIFACT_ROOT=%s\n' "$passes" "$ROOT"

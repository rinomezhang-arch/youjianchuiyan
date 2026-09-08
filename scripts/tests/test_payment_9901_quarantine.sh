#!/usr/bin/env bash
set -Eeuo pipefail
set +x
umask 077

ROOT="$(mktemp -d /tmp/payment-9901-quarantine-test.XXXXXX)"
BIN="$ROOT/bin"
mkdir -p "$BIN"
cat >"$BIN/mysql-no-password" <<'SH'
#!/usr/bin/env bash
unset MYSQL_PWD
exec mysql "$@"
SH
chmod +x "$BIN/mysql-no-password"

SCRIPT_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
QUARANTINE="$SCRIPT_ROOT/production/quarantine_payment_9901.sh"
RESTORE="$SCRIPT_ROOT/production/restore_quarantined_payment_9901.sh"
MYSQL=(mysql --no-defaults --protocol=TCP --host=127.0.0.1 --port=13317 --user=root --batch --skip-column-names)
BASE="qp9901_$(date +%Y%m%d%H%M%S)_$$"
MAIN_SCHEMA="${BASE}_main"
DOWNSTREAM_SCHEMA="${BASE}_downstream"
FINGERPRINT_SCHEMA="${BASE}_fingerprint"
ROLLBACK_SCHEMA="${BASE}_rollback"
SCHEMA_DRIFT_SCHEMA="${BASE}_drift"
passes=0
failures=0

pass() { passes=$((passes + 1)); }
fail() { echo "TEST_FAIL $1" >&2; failures=$((failures + 1)); }
check_eq() { [[ "$1" == "$2" ]] && pass || fail "$3 expected=$2 actual=$1"; }
query() { "${MYSQL[@]}" --database="$1" --execute="$2"; }

setup_schema() {
  local schema="$1"
  "${MYSQL[@]}" <<SQL
CREATE DATABASE \`$schema\` CHARACTER SET utf8mb4;
USE \`$schema\`;
CREATE TABLE finance_receivable(receivable_id BIGINT PRIMARY KEY,store_id BIGINT NOT NULL);
CREATE TABLE booking_master(booking_id VARCHAR(50) PRIMARY KEY,store_id BIGINT NOT NULL);
CREATE TABLE customer_master(customer_id INT PRIMARY KEY,store_id BIGINT NOT NULL);
CREATE TABLE finance_payment_record(
 payment_id BIGINT PRIMARY KEY,store_id BIGINT NOT NULL,payment_no VARCHAR(50) NOT NULL,payment_date DATE NOT NULL,
 receivable_id BIGINT NULL,customer_id INT NULL,customer_name VARCHAR(100) NULL,booking_id VARCHAR(20) NULL,
 booking_no VARCHAR(50) NULL,amount DECIMAL(12,2) NOT NULL,payment_method VARCHAR(20) NULL,account_id BIGINT NULL,
 operator_id INT NULL,operator_name VARCHAR(50) NULL,remark VARCHAR(500) NULL,created_at TIMESTAMP NULL,
 CONSTRAINT fk_test_receivable FOREIGN KEY(receivable_id) REFERENCES finance_receivable(receivable_id),
 CONSTRAINT fk_test_booking FOREIGN KEY(booking_id) REFERENCES booking_master(booking_id));
CREATE TABLE finance_transaction(trans_id BIGINT PRIMARY KEY,related_id BIGINT NULL,related_no VARCHAR(50),amount DECIMAL(12,2));
CREATE TABLE finance_voucher(voucher_id BIGINT PRIMARY KEY);
CREATE TABLE finance_voucher_detail(detail_id BIGINT AUTO_INCREMENT PRIMARY KEY,voucher_id BIGINT,related_id BIGINT);
CREATE TABLE finance_reconciliation(recon_id BIGINT PRIMARY KEY);
CREATE TABLE booking_payment(id BIGINT AUTO_INCREMENT PRIMARY KEY,booking_id VARCHAR(50),transaction_id VARCHAR(64),out_trade_no VARCHAR(64));
CREATE TABLE ingredient_inventory_log(log_id BIGINT AUTO_INCREMENT PRIMARY KEY,source_id VARCHAR(50));
CREATE TABLE finance_cost_record(id BIGINT AUTO_INCREMENT PRIMARY KEY,related_id BIGINT,amount DECIMAL(12,2));
CREATE TABLE member_point_log(id BIGINT AUTO_INCREMENT PRIMARY KEY,related_id BIGINT,related_no VARCHAR(50),change_points INT);
CREATE TABLE sys_notification(id BIGINT AUTO_INCREMENT PRIMARY KEY,related_id BIGINT);
CREATE TABLE finance_settlement(id BIGINT AUTO_INCREMENT PRIMARY KEY,store_id BIGINT,start_date DATE,end_date DATE,total_income DECIMAL(12,2));
SET FOREIGN_KEY_CHECKS=0;
INSERT INTO finance_payment_record VALUES
 (9901,1,'PAY-E2E-001','2026-08-01',9901,1,'synthetic customer','BK-E2E-001','BK-E2E-001',16000.00,'bank',1,1,'synthetic operator',NULL,'2026-08-01 15:14:00');
SET FOREIGN_KEY_CHECKS=1;
SQL
  cat >"$ROOT/$schema.env" <<EOF
MYSQL_USER=root
MYSQL_PASSWORD=
MYSQL_DATABASE=$schema
MYSQL_HOST=127.0.0.1
MYSQL_PORT=13317
EOF
}

run_quarantine() {
  local schema="$1" run_ts="$2" apply="$3"
  BANQUET_ENV_FILE="$ROOT/$schema.env" MYSQL_BIN="$BIN/mysql-no-password" RUN_TS="$run_ts" \
    QUARANTINE_ACTOR=test-runner APPLY="$apply" bash "$QUARANTINE"
}
run_restore() {
  local schema="$1" table="$2" apply="$3" null_invalid="$4" run_ts="$5"
  BANQUET_ENV_FILE="$ROOT/$schema.env" MYSQL_BIN="$BIN/mysql-no-password" RUN_TS="$run_ts" \
    RESTORE_ACTOR=test-runner APPLY="$apply" RESTORE_NULL_INVALID="$null_invalid" bash "$RESTORE" "$table"
}

setup_schema "$MAIN_SCHEMA"
archive_main=finance_payment_record_trash_20260908190001
preview="$(run_quarantine "$MAIN_SCHEMA" 20260908190001 NO)"
check_eq "$?" 0 preview_exit
[[ "$preview" == *'PRECHECK_OK'* && "$preview" == *'NO_CHANGE'* ]] && pass || fail preview_output
check_eq "$(query "$MAIN_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 1 preview_keeps_source
check_eq "$(query "$MAIN_SCHEMA" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$MAIN_SCHEMA' AND table_name='$archive_main'")" 0 preview_creates_no_archive

apply_output="$(run_quarantine "$MAIN_SCHEMA" 20260908190001 YES)"
check_eq "$?" 0 apply_exit
[[ "$apply_output" == *'APPLY_OK'* && "$apply_output" == *'removed=1'* ]] && pass || fail apply_output
check_eq "$(query "$MAIN_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 0 apply_removes_source
check_eq "$(query "$MAIN_SCHEMA" "SELECT COUNT(*) FROM $archive_main WHERE source_pk=9901")" 1 archive_has_one_row
check_eq "$(query "$MAIN_SCHEMA" "SELECT COUNT(*) FROM $archive_main WHERE original_row_hash=SHA2(CAST(row_image AS CHAR),256) AND quarantined_by='test-runner' AND quarantine_reason<>''")" 1 archive_hash_and_metadata

set +e
strict_output="$(run_restore "$MAIN_SCHEMA" "$archive_main" NO NO 20260908190002 2>&1)"
strict_status=$?
set -e
check_eq "$strict_status" 53 strict_restore_refused
[[ "$strict_output" == *'invalid_parent_refs=3'* ]] && pass || fail strict_restore_reports_count
check_eq "$(query "$MAIN_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 0 strict_restore_no_write

null_preview="$(run_restore "$MAIN_SCHEMA" "$archive_main" NO YES 20260908190003)"
check_eq "$?" 0 null_preview_exit
[[ "$null_preview" == *'mode=null-invalid'* && "$null_preview" == *'NO_CHANGE'* ]] && pass || fail null_preview_output
check_eq "$(query "$MAIN_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 0 null_preview_no_write

restore_output="$(run_restore "$MAIN_SCHEMA" "$archive_main" YES YES 20260908190004)"
check_eq "$?" 0 null_restore_exit
[[ "$restore_output" == *'RESTORE_OK'* && "$restore_output" == *'archive_retained=1'* ]] && pass || fail null_restore_output
check_eq "$(query "$MAIN_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901 AND amount=16000.00')" 1 restored_amount
check_eq "$(query "$MAIN_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901 AND receivable_id IS NULL AND booking_id IS NULL AND customer_id IS NULL')" 1 invalid_refs_nulled
check_eq "$(query "$MAIN_SCHEMA" "SELECT COUNT(*) FROM $archive_main WHERE source_pk=9901 AND restored_at IS NOT NULL AND restore_mode LIKE 'null-invalid:%'")" 1 archive_retained_and_marked

set +e
run_quarantine "$MAIN_SCHEMA" 20260908190005 NO >/dev/null 2>&1
recheck_status=$?
set -e
check_eq "$recheck_status" 41 changed_fingerprint_refused

setup_schema "$DOWNSTREAM_SCHEMA"
query "$DOWNSTREAM_SCHEMA" "INSERT INTO finance_transaction(trans_id,related_id,related_no,amount) VALUES(1,9901,'PAY-E2E-001',16000.00)" >/dev/null
set +e
run_quarantine "$DOWNSTREAM_SCHEMA" 20260908190101 YES >/dev/null 2>&1
downstream_status=$?
set -e
check_eq "$downstream_status" 43 downstream_refused
check_eq "$(query "$DOWNSTREAM_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 1 downstream_failure_keeps_source
check_eq "$(query "$DOWNSTREAM_SCHEMA" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DOWNSTREAM_SCHEMA' AND table_name='finance_payment_record_trash_20260908190101'")" 0 downstream_failure_creates_no_archive

setup_schema "$FINGERPRINT_SCHEMA"
query "$FINGERPRINT_SCHEMA" 'SET FOREIGN_KEY_CHECKS=0; UPDATE finance_payment_record SET amount=15999.00 WHERE payment_id=9901; SET FOREIGN_KEY_CHECKS=1' >/dev/null
set +e
run_quarantine "$FINGERPRINT_SCHEMA" 20260908190201 YES >/dev/null 2>&1
fingerprint_status=$?
set -e
check_eq "$fingerprint_status" 41 fingerprint_refused
check_eq "$(query "$FINGERPRINT_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901 AND amount=15999.00')" 1 fingerprint_failure_keeps_source

setup_schema "$ROLLBACK_SCHEMA"
query "$ROLLBACK_SCHEMA" "CREATE TRIGGER reject_payment_delete BEFORE DELETE ON finance_payment_record FOR EACH ROW SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='synthetic delete failure'" >/dev/null
set +e
run_quarantine "$ROLLBACK_SCHEMA" 20260908190301 YES >/dev/null 2>&1
rollback_status=$?
set -e
[[ "$rollback_status" -ne 0 ]] && pass || fail transactional_failure_exit
check_eq "$(query "$ROLLBACK_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 1 transaction_rollback_keeps_source
check_eq "$(query "$ROLLBACK_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record_trash_20260908190301')" 0 transaction_rollback_removes_archive_insert
check_eq "$(query "$ROLLBACK_SCHEMA" "SELECT COUNT(*) FROM information_schema.routines WHERE routine_schema='$ROLLBACK_SCHEMA' AND routine_name='qp9901_20260908190301'")" 0 failed_procedure_cleaned

setup_schema "$SCHEMA_DRIFT_SCHEMA"
query "$SCHEMA_DRIFT_SCHEMA" 'ALTER TABLE finance_payment_record ADD COLUMN unexpected_column VARCHAR(10) NULL' >/dev/null
set +e
run_quarantine "$SCHEMA_DRIFT_SCHEMA" 20260908190401 YES >/dev/null 2>&1
drift_status=$?
set -e
check_eq "$drift_status" 39 schema_drift_refused
check_eq "$(query "$SCHEMA_DRIFT_SCHEMA" 'SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901')" 1 schema_drift_keeps_source
check_eq "$(query "$SCHEMA_DRIFT_SCHEMA" "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$SCHEMA_DRIFT_SCHEMA' AND table_name='finance_payment_record_trash_20260908190401'")" 0 schema_drift_creates_no_archive

if (( failures > 0 )); then
  printf 'TESTS_PASS=%s TESTS_FAIL=%s RETAINED_SCHEMAS=%s,%s,%s,%s,%s\n' "$passes" "$failures" "$MAIN_SCHEMA" "$DOWNSTREAM_SCHEMA" "$FINGERPRINT_SCHEMA" "$ROLLBACK_SCHEMA" "$SCHEMA_DRIFT_SCHEMA"
  exit 1
fi
printf 'TESTS_PASS=%s TESTS_FAIL=0 RETAINED_SCHEMAS=%s,%s,%s,%s,%s ARTIFACT_ROOT=%s\n' "$passes" "$MAIN_SCHEMA" "$DOWNSTREAM_SCHEMA" "$FINGERPRINT_SCHEMA" "$ROLLBACK_SCHEMA" "$SCHEMA_DRIFT_SCHEMA" "$ROOT"

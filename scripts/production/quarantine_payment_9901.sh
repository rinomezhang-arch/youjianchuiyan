#!/usr/bin/env bash
# Preview by default. APPLY=YES is required to quarantine the single known E2E orphan.
set -Eeuo pipefail
set +x
umask 077

ENV_FILE="${BANQUET_ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
APPLY="${APPLY:-NO}"
RUN_TS="${RUN_TS:-$(date +%Y%m%d%H%M%S)}"
ACTOR="${QUARANTINE_ACTOR:-approved-operator}"

[[ "$APPLY" == "NO" || "$APPLY" == "YES" ]] || { echo 'ERROR invalid APPLY value' >&2; exit 10; }
[[ "$RUN_TS" =~ ^[0-9]{14}$ ]] || { echo 'ERROR invalid RUN_TS' >&2; exit 10; }
[[ "$ACTOR" =~ ^[A-Za-z0-9._-]{1,64}$ ]] || { echo 'ERROR invalid operator label' >&2; exit 10; }
[[ -r "$ENV_FILE" ]] || { echo 'ERROR credential environment file is not readable' >&2; exit 11; }

# shellcheck disable=SC1090
source "$ENV_FILE" >/dev/null 2>&1
: "${MYSQL_USER:?MYSQL_USER is required}"
: "${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
[[ "${MYSQL_PASSWORD+x}" == x ]] || { echo 'ERROR MYSQL_PASSWORD is required' >&2; exit 11; }
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
[[ "$MYSQL_HOST" == "127.0.0.1" || "$MYSQL_HOST" == "localhost" ]] || { echo 'ERROR database host must be loopback' >&2; exit 12; }
[[ "$MYSQL_PORT" =~ ^[0-9]+$ ]] || { echo 'ERROR database port must be numeric' >&2; exit 12; }

export MYSQL_PWD="$MYSQL_PASSWORD"
trap 'unset MYSQL_PWD MYSQL_PASSWORD' EXIT
mysql_args=(--no-defaults --protocol=TCP --host="$MYSQL_HOST" --port="$MYSQL_PORT" --user="$MYSQL_USER" --database="$MYSQL_DATABASE" --batch --skip-column-names --default-character-set=utf8mb4)

read -r schema_match payment_count fingerprint_count receivable_parent booking_parent customer_parent downstream_count dashboard_delta <<<"$(
  "$MYSQL_BIN" "${mysql_args[@]}" --execute="
    SET SESSION TRANSACTION READ ONLY;
    START TRANSACTION READ ONLY;
    SELECT
      (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ',')='payment_id,store_id,payment_no,payment_date,receivable_id,customer_id,customer_name,booking_id,booking_no,amount,payment_method,account_id,operator_id,operator_name,remark,created_at'
         FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='finance_payment_record'),
      (SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901),
      (SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901 AND store_id=1
         AND BINARY payment_no='PAY-E2E-001' AND payment_date='2026-08-01'
         AND amount=16000.00 AND receivable_id=9901 AND BINARY booking_id='BK-E2E-001'),
      (SELECT COUNT(*) FROM finance_receivable WHERE receivable_id=9901),
      (SELECT COUNT(*) FROM booking_master WHERE BINARY booking_id='BK-E2E-001'),
      (SELECT COUNT(*) FROM customer_master c JOIN finance_payment_record p ON p.customer_id=c.customer_id WHERE p.payment_id=9901),
      (SELECT
         (SELECT COUNT(*) FROM finance_transaction WHERE trans_id=9901 OR related_id=9901 OR related_no IN ('PAY-E2E-001','BK-E2E-001'))
       + (SELECT COUNT(*) FROM finance_voucher WHERE voucher_id=9901)
       + (SELECT COUNT(*) FROM finance_voucher_detail WHERE voucher_id=9901 OR related_id=9901)
       + (SELECT COUNT(*) FROM finance_reconciliation WHERE recon_id=9901)
       + (SELECT COUNT(*) FROM booking_payment WHERE booking_id='BK-E2E-001' OR transaction_id='PAY-E2E-001' OR out_trade_no='PAY-E2E-001')
       + (SELECT COUNT(*) FROM ingredient_inventory_log WHERE source_id IN ('9901','PAY-E2E-001','BK-E2E-001'))
       + (SELECT COUNT(*) FROM finance_cost_record WHERE related_id=9901)
       + (SELECT COUNT(*) FROM member_point_log WHERE related_id=9901 OR related_no IN ('PAY-E2E-001','BK-E2E-001'))
       + (SELECT COUNT(*) FROM sys_notification WHERE related_id=9901)
       + (SELECT COUNT(*) FROM finance_settlement WHERE store_id=1 AND start_date<='2026-08-01' AND end_date>='2026-08-01')),
      (SELECT COALESCE(SUM(amount),0)-COALESCE(SUM(CASE WHEN payment_id<>9901 THEN amount ELSE 0 END),0)
         FROM finance_payment_record WHERE store_id=1 AND payment_date>='2026-08-01' AND payment_date<'2026-09-01');
    ROLLBACK;"
)"

[[ "$schema_match" == 1 ]] || { echo 'PRECHECK_FAIL payment schema changed; full-row capture not guaranteed' >&2; exit 39; }
[[ "$payment_count" == 1 ]] || { echo 'PRECHECK_FAIL payment row count is not one' >&2; exit 40; }
[[ "$fingerprint_count" == 1 ]] || { echo 'PRECHECK_FAIL business fingerprint changed' >&2; exit 41; }
[[ "$receivable_parent" == 0 && "$booking_parent" == 0 && "$customer_parent" == 0 ]] || { echo 'PRECHECK_FAIL parent state changed' >&2; exit 42; }
[[ "$downstream_count" == 0 ]] || { echo 'PRECHECK_FAIL downstream references exist' >&2; exit 43; }
[[ "$dashboard_delta" == "16000.00" || "$dashboard_delta" == "16000" ]] || { echo 'PRECHECK_FAIL dashboard delta changed' >&2; exit 44; }

archive_table="finance_payment_record_trash_$RUN_TS"
echo "PRECHECK_OK payment=9901 business_no=PAY-***-001 parents_missing=3 downstream=0 dashboard_delta=16000.00 mode=$APPLY"
if [[ "$APPLY" != "YES" ]]; then
  echo "NO_CHANGE archive_candidate=$archive_table"
  exit 0
fi

# MySQL permanent DDL implicitly commits. The empty archive table is therefore created
# before the data-move transaction; the insert, hash check and delete remain atomic.
"$MYSQL_BIN" "${mysql_args[@]}" --execute="
  CREATE TABLE \`$archive_table\` (
    quarantine_id BIGINT NOT NULL AUTO_INCREMENT,
    source_table VARCHAR(64) NOT NULL,
    source_pk BIGINT NOT NULL,
    business_fingerprint VARCHAR(128) NOT NULL,
    row_image JSON NOT NULL,
    original_row_hash CHAR(64) NOT NULL,
    quarantine_reason VARCHAR(255) NOT NULL,
    quarantined_by VARCHAR(64) NOT NULL,
    quarantined_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    restored_at TIMESTAMP(6) NULL,
    restore_mode VARCHAR(32) NULL,
    PRIMARY KEY (quarantine_id),
    UNIQUE KEY uk_source_pk (source_table, source_pk)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;"

procedure_name="qp9901_$RUN_TS"
set +e
"$MYSQL_BIN" "${mysql_args[@]}" <<SQL
DELIMITER \$\$
CREATE PROCEDURE \`$procedure_name\`()
BEGIN
  DECLARE locked_id BIGINT DEFAULT NULL;
  DECLARE inserted_rows INT DEFAULT 0;
  DECLARE deleted_rows INT DEFAULT 0;
  DECLARE archive_rows INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;
  IF (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ',')
        FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='finance_payment_record') <>
     'payment_id,store_id,payment_no,payment_date,receivable_id,customer_id,customer_name,booking_id,booking_no,amount,payment_method,account_id,operator_id,operator_name,remark,created_at'
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='payment schema changed'; END IF;
  SELECT payment_id INTO locked_id
    FROM finance_payment_record
   WHERE payment_id=9901 AND store_id=1 AND BINARY payment_no='PAY-E2E-001'
     AND payment_date='2026-08-01' AND amount=16000.00
     AND receivable_id=9901 AND BINARY booking_id='BK-E2E-001'
   FOR UPDATE;
  IF locked_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='fingerprint lock failed'; END IF;
  IF (SELECT COUNT(*) FROM finance_receivable WHERE receivable_id=9901) <> 0
     OR (SELECT COUNT(*) FROM booking_master WHERE BINARY booking_id='BK-E2E-001') <> 0
     OR (SELECT COUNT(*) FROM customer_master c JOIN finance_payment_record p ON p.customer_id=c.customer_id WHERE p.payment_id=9901) <> 0
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='parent state changed'; END IF;
  IF ((SELECT COUNT(*) FROM finance_transaction WHERE trans_id=9901 OR related_id=9901 OR related_no IN ('PAY-E2E-001','BK-E2E-001'))
      +(SELECT COUNT(*) FROM finance_voucher WHERE voucher_id=9901)
      +(SELECT COUNT(*) FROM finance_voucher_detail WHERE voucher_id=9901 OR related_id=9901)
      +(SELECT COUNT(*) FROM finance_reconciliation WHERE recon_id=9901)
      +(SELECT COUNT(*) FROM booking_payment WHERE booking_id='BK-E2E-001' OR transaction_id='PAY-E2E-001' OR out_trade_no='PAY-E2E-001')
      +(SELECT COUNT(*) FROM ingredient_inventory_log WHERE source_id IN ('9901','PAY-E2E-001','BK-E2E-001'))
      +(SELECT COUNT(*) FROM finance_cost_record WHERE related_id=9901)
      +(SELECT COUNT(*) FROM member_point_log WHERE related_id=9901 OR related_no IN ('PAY-E2E-001','BK-E2E-001'))
      +(SELECT COUNT(*) FROM sys_notification WHERE related_id=9901)
      +(SELECT COUNT(*) FROM finance_settlement WHERE store_id=1 AND start_date<='2026-08-01' AND end_date>='2026-08-01')) <> 0
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='downstream state changed'; END IF;
  IF (SELECT COALESCE(SUM(amount),0)-COALESCE(SUM(CASE WHEN payment_id<>9901 THEN amount ELSE 0 END),0)
        FROM finance_payment_record WHERE store_id=1 AND payment_date>='2026-08-01' AND payment_date<'2026-09-01') <> 16000.00
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='dashboard delta changed'; END IF;

  INSERT INTO \`$archive_table\`
    (source_table,source_pk,business_fingerprint,row_image,original_row_hash,quarantine_reason,quarantined_by)
  SELECT 'finance_payment_record',p.payment_id,'PAY-***-001',
    JSON_OBJECT('payment_id',p.payment_id,'store_id',p.store_id,'payment_no',p.payment_no,
      'payment_date',p.payment_date,'receivable_id',p.receivable_id,'customer_id',p.customer_id,
      'customer_name',p.customer_name,'booking_id',p.booking_id,'booking_no',p.booking_no,
      'amount',p.amount,'payment_method',p.payment_method,'account_id',p.account_id,
      'operator_id',p.operator_id,'operator_name',p.operator_name,'remark',p.remark,'created_at',p.created_at),
    SHA2(CAST(JSON_OBJECT('payment_id',p.payment_id,'store_id',p.store_id,'payment_no',p.payment_no,
      'payment_date',p.payment_date,'receivable_id',p.receivable_id,'customer_id',p.customer_id,
      'customer_name',p.customer_name,'booking_id',p.booking_id,'booking_no',p.booking_no,
      'amount',p.amount,'payment_method',p.payment_method,'account_id',p.account_id,
      'operator_id',p.operator_id,'operator_name',p.operator_name,'remark',p.remark,'created_at',p.created_at) AS CHAR),256),
    'confirmed E2E orphan; production owner decision required','$ACTOR'
  FROM finance_payment_record p WHERE p.payment_id=9901;
  SET inserted_rows=ROW_COUNT();
  IF inserted_rows <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='archive insert count was not one'; END IF;
  SELECT COUNT(*) INTO archive_rows FROM \`$archive_table\`
   WHERE source_table='finance_payment_record' AND source_pk=9901
     AND original_row_hash=SHA2(CAST(row_image AS CHAR),256);
  IF archive_rows <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='archive hash verification failed'; END IF;

  DELETE FROM finance_payment_record
   WHERE payment_id=9901 AND store_id=1 AND BINARY payment_no='PAY-E2E-001'
     AND payment_date='2026-08-01' AND amount=16000.00
     AND receivable_id=9901 AND BINARY booking_id='BK-E2E-001';
  SET deleted_rows=ROW_COUNT();
  IF deleted_rows <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='source delete count was not one'; END IF;
  IF (SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901) <> 0
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='source row still present'; END IF;
  COMMIT;
  SELECT 'APPLY_OK','payment=9901','business_no=PAY-***-001','archived=1','removed=1','dashboard_delta=-16000.00';
END\$\$
DELIMITER ;
CALL \`$procedure_name\`();
SQL
apply_status=$?
set -e
set +e
"$MYSQL_BIN" "${mysql_args[@]}" --execute="DROP PROCEDURE IF EXISTS \`$procedure_name\`;" >/dev/null
cleanup_status=$?
set -e
(( apply_status == 0 )) || { echo 'ERROR quarantine transaction rolled back' >&2; exit "$apply_status"; }
(( cleanup_status == 0 )) || { echo 'APPLY_SUCCEEDED_BUT_PROCEDURE_CLEANUP_UNCONFIRMED' >&2; exit 70; }

echo "QUARANTINE_COMPLETE archive_table=$archive_table"

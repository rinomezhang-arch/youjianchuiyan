#!/usr/bin/env bash
# Preview by default. Strict restore refuses orphan references; RESTORE_NULL_INVALID=YES explicitly nulls them.
set -Eeuo pipefail
set +x
umask 077

[[ $# -eq 1 ]] || { echo 'usage: restore_quarantined_payment_9901.sh finance_payment_record_trash_YYYYMMDDHHMMSS' >&2; exit 2; }
ARCHIVE_TABLE="$1"
ENV_FILE="${BANQUET_ENV_FILE:-/home/ubuntu/.banquet_env.sh}"
MYSQL_BIN="${MYSQL_BIN:-mysql}"
APPLY="${APPLY:-NO}"
NULL_INVALID="${RESTORE_NULL_INVALID:-NO}"
RUN_TS="${RUN_TS:-$(date +%Y%m%d%H%M%S)}"
ACTOR="${RESTORE_ACTOR:-approved-operator}"

[[ "$ARCHIVE_TABLE" =~ ^finance_payment_record_trash_[0-9]{14}$ ]] || { echo 'ERROR invalid archive table name' >&2; exit 10; }
[[ "$APPLY" == "NO" || "$APPLY" == "YES" ]] || { echo 'ERROR invalid APPLY value' >&2; exit 10; }
[[ "$NULL_INVALID" == "NO" || "$NULL_INVALID" == "YES" ]] || { echo 'ERROR invalid RESTORE_NULL_INVALID value' >&2; exit 10; }
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

read -r schema_match archive_count source_count hash_count receivable_parent booking_parent customer_parent <<<"$(
  "$MYSQL_BIN" "${mysql_args[@]}" --execute="
    SET SESSION TRANSACTION READ ONLY;
    START TRANSACTION READ ONLY;
    SELECT
      (SELECT GROUP_CONCAT(COLUMN_NAME ORDER BY ORDINAL_POSITION SEPARATOR ',')='payment_id,store_id,payment_no,payment_date,receivable_id,customer_id,customer_name,booking_id,booking_no,amount,payment_method,account_id,operator_id,operator_name,remark,created_at'
         FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='finance_payment_record'),
      (SELECT COUNT(*) FROM \`$ARCHIVE_TABLE\` WHERE source_table='finance_payment_record' AND source_pk=9901 AND restored_at IS NULL),
      (SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901),
      (SELECT COUNT(*) FROM \`$ARCHIVE_TABLE\` WHERE source_table='finance_payment_record' AND source_pk=9901
         AND restored_at IS NULL AND original_row_hash=SHA2(CAST(row_image AS CHAR),256)),
      (SELECT COUNT(*) FROM finance_receivable WHERE receivable_id=CAST(JSON_UNQUOTE((SELECT JSON_EXTRACT(row_image,'$.receivable_id') FROM \`$ARCHIVE_TABLE\` WHERE source_pk=9901 LIMIT 1)) AS UNSIGNED)),
      (SELECT COUNT(*) FROM booking_master WHERE BINARY booking_id=JSON_UNQUOTE((SELECT JSON_EXTRACT(row_image,'$.booking_id') FROM \`$ARCHIVE_TABLE\` WHERE source_pk=9901 LIMIT 1))),
      (SELECT COUNT(*) FROM customer_master WHERE customer_id=CAST(JSON_UNQUOTE((SELECT JSON_EXTRACT(row_image,'$.customer_id') FROM \`$ARCHIVE_TABLE\` WHERE source_pk=9901 LIMIT 1)) AS UNSIGNED));
    ROLLBACK;"
)"

[[ "$schema_match" == 1 ]] || { echo 'RESTORE_PRECHECK_FAIL payment schema changed' >&2; exit 49; }
[[ "$archive_count" == 1 ]] || { echo 'RESTORE_PRECHECK_FAIL archive row count is not one' >&2; exit 50; }
[[ "$source_count" == 0 ]] || { echo 'RESTORE_PRECHECK_FAIL source payment already exists' >&2; exit 51; }
[[ "$hash_count" == 1 ]] || { echo 'RESTORE_PRECHECK_FAIL archive hash mismatch' >&2; exit 52; }

missing_parents=$(( (receivable_parent == 0 ? 1 : 0) + (booking_parent == 0 ? 1 : 0) + (customer_parent == 0 ? 1 : 0) ))
if (( missing_parents > 0 )) && [[ "$NULL_INVALID" != "YES" ]]; then
  echo "RESTORE_PRECHECK_REFUSED invalid_parent_refs=$missing_parents use_explicit_null_mode=YES" >&2
  exit 53
fi
restore_mode="strict"
[[ "$NULL_INVALID" == "YES" ]] && restore_mode="null-invalid"
echo "RESTORE_PRECHECK_OK payment=9901 business_no=PAY-***-001 archive_hash=valid invalid_parent_refs=$missing_parents mode=$restore_mode apply=$APPLY"
if [[ "$APPLY" != "YES" ]]; then
  echo 'NO_CHANGE restore preview only'
  exit 0
fi

procedure_name="rp9901_$RUN_TS"
set +e
"$MYSQL_BIN" "${mysql_args[@]}" <<SQL
DELIMITER \$\$
CREATE PROCEDURE \`$procedure_name\`()
BEGIN
  DECLARE v_archive_id BIGINT DEFAULT NULL;
  DECLARE inserted_rows INT DEFAULT 0;
  DECLARE marked_rows INT DEFAULT 0;
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
  SELECT q.quarantine_id INTO v_archive_id FROM \`$ARCHIVE_TABLE\` q
   WHERE source_table='finance_payment_record' AND source_pk=9901 AND restored_at IS NULL
     AND original_row_hash=SHA2(CAST(row_image AS CHAR),256)
   FOR UPDATE;
  IF v_archive_id IS NULL THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='archive lock or hash validation failed'; END IF;
  IF (SELECT COUNT(*) FROM finance_payment_record WHERE payment_id=9901) <> 0
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='source payment already exists'; END IF;
  IF '$restore_mode' = 'strict' AND (
       (SELECT COUNT(*) FROM finance_receivable WHERE receivable_id=CAST(JSON_UNQUOTE((SELECT JSON_EXTRACT(row_image,'$.receivable_id') FROM \`$ARCHIVE_TABLE\` WHERE quarantine_id=v_archive_id)) AS UNSIGNED))=0
    OR (SELECT COUNT(*) FROM booking_master WHERE BINARY booking_id=JSON_UNQUOTE((SELECT JSON_EXTRACT(row_image,'$.booking_id') FROM \`$ARCHIVE_TABLE\` WHERE quarantine_id=v_archive_id)))=0
    OR (SELECT COUNT(*) FROM customer_master WHERE customer_id=CAST(JSON_UNQUOTE((SELECT JSON_EXTRACT(row_image,'$.customer_id') FROM \`$ARCHIVE_TABLE\` WHERE quarantine_id=v_archive_id)) AS UNSIGNED))=0)
  THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='strict restore refuses invalid parent references'; END IF;

  INSERT INTO finance_payment_record
    (payment_id,store_id,payment_no,payment_date,receivable_id,customer_id,customer_name,
     booking_id,booking_no,amount,payment_method,account_id,operator_id,operator_name,remark,created_at)
  SELECT
    CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.payment_id')) AS UNSIGNED),
    CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.store_id')) AS UNSIGNED),
    JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.payment_no')),
    CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.payment_date')) AS DATE),
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.receivable_id'))='NULL' THEN NULL WHEN '$restore_mode'='null-invalid' AND NOT EXISTS(SELECT 1 FROM finance_receivable r WHERE r.receivable_id=CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.receivable_id')) AS UNSIGNED)) THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.receivable_id')) AS UNSIGNED) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.customer_id'))='NULL' THEN NULL WHEN '$restore_mode'='null-invalid' AND NOT EXISTS(SELECT 1 FROM customer_master c WHERE c.customer_id=CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.customer_id')) AS UNSIGNED)) THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.customer_id')) AS UNSIGNED) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.customer_name'))='NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.customer_name')) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.booking_id'))='NULL' THEN NULL WHEN '$restore_mode'='null-invalid' AND NOT EXISTS(SELECT 1 FROM booking_master b WHERE BINARY b.booking_id=JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.booking_id'))) THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.booking_id')) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.booking_no'))='NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.booking_no')) END,
    CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.amount')) AS DECIMAL(12,2)),
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.payment_method'))='NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.payment_method')) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.account_id'))='NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.account_id')) AS UNSIGNED) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.operator_id'))='NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.operator_id')) AS UNSIGNED) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.operator_name'))='NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.operator_name')) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.remark'))='NULL' THEN NULL ELSE JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.remark')) END,
    CASE WHEN JSON_TYPE(JSON_EXTRACT(row_image,'$.created_at'))='NULL' THEN NULL ELSE CAST(JSON_UNQUOTE(JSON_EXTRACT(row_image,'$.created_at')) AS DATETIME) END
  FROM \`$ARCHIVE_TABLE\` WHERE quarantine_id=v_archive_id;
  SET inserted_rows=ROW_COUNT();
  IF inserted_rows <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='restore insert count was not one'; END IF;

  UPDATE \`$ARCHIVE_TABLE\` SET restored_at=CURRENT_TIMESTAMP(6),restore_mode='$restore_mode:$ACTOR'
   WHERE quarantine_id=v_archive_id AND restored_at IS NULL;
  SET marked_rows=ROW_COUNT();
  IF marked_rows <> 1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='archive restore marker count was not one'; END IF;
  COMMIT;
  SELECT 'RESTORE_OK','payment=9901','business_no=PAY-***-001','inserted=1','archive_retained=1','mode=$restore_mode';
END\$\$
DELIMITER ;
CALL \`$procedure_name\`();
SQL
restore_status=$?
set -e
set +e
"$MYSQL_BIN" "${mysql_args[@]}" --execute="DROP PROCEDURE IF EXISTS \`$procedure_name\`;" >/dev/null
cleanup_status=$?
set -e
(( restore_status == 0 )) || { echo 'ERROR restore transaction rolled back' >&2; exit "$restore_status"; }
(( cleanup_status == 0 )) || { echo 'RESTORE_SUCCEEDED_BUT_PROCEDURE_CLEANUP_UNCONFIRMED' >&2; exit 70; }

echo "RESTORE_COMPLETE archive_table=$ARCHIVE_TABLE mode=$restore_mode"

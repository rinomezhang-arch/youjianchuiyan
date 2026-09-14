#!/bin/bash
# TR58: DB read-only audit (desensitized, no sensitive values in output)
# Only outputs IDs, counts, and relationship assertions
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== DB AUDIT (DESENSITIZED) ==="
echo "=== booking_inquiry ==="
$M "$SCHEMA" -e "SELECT id, store_id, customer_name REGEXP '^[^0-9]+$' AS name_valid, customer_phone REGEXP '^1[3-9][0-9]{9}$' AS phone_valid, preferred_date, guest_count, remark IS NOT NULL AS has_remark, status, marketing_publication_id, source_code, source_channel FROM booking_inquiry\G"
echo "=== marketing_attribution_event ==="
$M "$SCHEMA" -e "SELECT event_id, publication_id, store_id, source_code, event_type, business_type, business_id, business_no, request_id IS NOT NULL AS has_request_id, occurred_at IS NOT NULL AS has_occurred_at FROM marketing_attribution_event\G"
echo "=== publication ==="
$M "$SCHEMA" -e "SELECT publication_id, store_id, source_code, channel, public_slug, status FROM marketing_publication\G"
echo "=== store ==="
$M "$SCHEMA" -e "SELECT store_id, store_code, store_name, status FROM store_info\G"
echo "=== RELATIONSHIP ASSERTIONS ==="
$M "$SCHEMA" -e "SELECT CASE WHEN bi.store_id=evt.store_id THEN 1 ELSE 0 END AS store_match, CASE WHEN bi.marketing_publication_id=evt.publication_id THEN 1 ELSE 0 END AS pub_match, CASE WHEN bi.source_code=evt.source_code THEN 1 ELSE 0 END AS source_match FROM booking_inquiry bi, marketing_attribution_event evt WHERE evt.business_id=bi.id AND evt.event_type='inquiry'\G"
echo "DB_AUDIT_DONE"

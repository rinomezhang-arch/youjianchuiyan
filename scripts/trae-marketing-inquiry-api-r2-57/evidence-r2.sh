#!/bin/bash
# TR57 R2 库级旁证：只读 SELECT，不做任何写/删操作
set -u
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 --default-character-set=utf8mb4 -N"
SCHEMA="$(cat "$HOME/tr57-work/scripts/trae-marketing-inquiry-api-r2-57/last_schema.txt")"
echo "SCHEMA=${SCHEMA}"
echo "=== [A] 全部 TL55 前缀 schema（旧 schema 保留核对，不清理）==="
$M -e "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tl55%' ORDER BY schema_name"
echo "=== [B] 本次 schema 两表总数 ==="
$M -e "SELECT 'booking_inquiry' AS t, COUNT(*) AS c FROM \`${SCHEMA}\`.booking_inquiry UNION ALL SELECT 'marketing_attribution_event', COUNT(*) FROM \`${SCHEMA}\`.marketing_attribution_event"
echo "=== [C] R2反例1 只改备注 requestId 的事件（期望：仅1行；business_type=booking_inquiry；business_no=INQ+id）==="
$M "${SCHEMA}" -e "SELECT request_id, business_type, business_no, COUNT(*) AS cnt FROM marketing_attribution_event WHERE request_id LIKE 'req-http-rmk-%' GROUP BY request_id, business_type, business_no"
echo "=== [D] R2反例1 合成号码13800000021 咨询（期望：仅1行，remark=原始备注，status 未变）==="
$M "${SCHEMA}" -e "SELECT id, customer_phone, remark, status, booking_id FROM booking_inquiry WHERE customer_phone='13800000021'"
echo "=== [E] 被篡改备注不得落库（期望 0）==="
$M "${SCHEMA}" -e "SELECT COUNT(*) FROM booking_inquiry WHERE remark='被篡改的备注'"
echo "=== [F] 该合成号码咨询行数（期望 1，篡改重放零新增）==="
$M "${SCHEMA}" -e "SELECT COUNT(*) FROM booking_inquiry WHERE customer_phone='13800000021'"
echo "=== EVIDENCE_DONE ==="

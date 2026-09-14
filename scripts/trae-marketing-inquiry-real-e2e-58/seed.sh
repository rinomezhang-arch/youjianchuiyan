#!/bin/bash
set -u
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 --default-character-set=utf8mb4 -N"
cd ~/tr58-work
SCHEMA=$(cat scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "Using SCHEMA=${SCHEMA}"

# Seed: store_info, marketing_activity, marketing_publication (visible)
$M "$SCHEMA" <<'SQL'
INSERT INTO store_info (store_id, store_code, store_name, address, phone, status) VALUES
  (1, 'TEST001', '测试门店', '测试地址', '13800000000', 'open')
  ON DUPLICATE KEY UPDATE store_name=VALUES(store_name);

INSERT INTO marketing_activity (activity_id, store_id, activity_code, activity_name, activity_type, start_date, end_date, is_active) VALUES
  (1, 1, 'ACT001', '测试营销活动', 'promotion', '2026-01-01', '2026-12-31', 1)
  ON DUPLICATE KEY UPDATE activity_name=VALUES(activity_name);

INSERT INTO marketing_publication (publication_id, activity_id, store_id, version, channel, public_slug, source_code, title, status, valid_from, valid_to, request_id, published_at) VALUES
  (1, 1, 1, 1, 'h5_share', 'test-pub-slug-001', 'SRC-VISIBLE-001', '测试营销活动发布', 'published', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 'seed-req-001', NOW())
  ON DUPLICATE KEY UPDATE status=VALUES(status);
SQL
echo "=== SEED DONE ==="

echo "=== VERIFY ==="
$M "$SCHEMA" -e "SELECT publication_id, public_slug, source_code, status, valid_from, valid_to FROM marketing_publication;"
$M "$SCHEMA" -e "SELECT activity_id, store_id, activity_name, is_active FROM marketing_activity;"
$M "$SCHEMA" -e "SELECT store_id, store_code, store_name, status FROM store_info;"
echo "SEED_READY"

#!/bin/bash
set -u
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 --default-character-set=utf8mb4 -N"
cd ~/tr58-work
TS=$(date +%Y%m%d_%H%M%S)
SCHEMA="tr58_e2e_${TS}"
mkdir -p scripts/trae-marketing-inquiry-real-e2e-58
echo "$SCHEMA" > scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt
echo "SCHEMA=${SCHEMA}"

# Gate check
echo "=== GATE ==="
docker ps --filter name=youjian-mysql-test-13317 --format '{{.Names}}|{{.Status}}|{{.Ports}}'
docker inspect youjian-mysql-test-13317 --format '{{json .Mounts}}'
DD=$($M -e "SELECT @@port, @@datadir, VERSION();")
echo "  @@port/datadir/version=$(printf '%s' "$DD" | tr '\t' ' ')"

# Create schema
$M -e "CREATE DATABASE \`$SCHEMA\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
echo "  schema created"

# Base schema
$M "$SCHEMA" < scripts/tianlong-marketing-inquiry-api-55/base_schema.sql
echo "  base_schema applied"

# Migration
$M --delimiter=// "$SCHEMA" < scripts/migrations/marketing_publication_v1.sql
echo "  migration applied"

# Seed: need a visible marketing publication for E2E
# Insert a store, marketing campaign, and a visible publication
$M "$SCHEMA" <<'SQL'
INSERT INTO store (id, name, address, phone, status) VALUES
  (1, '测试门店', '测试地址', '13800000000', 1)
  ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO marketing_campaign (id, store_id, name, status, start_date, end_date) VALUES
  (1, 1, '测试营销活动', 1, '2026-01-01', '2026-12-31')
  ON DUPLICATE KEY UPDATE name=VALUES(name);

INSERT INTO marketing_publication (id, store_id, campaign_id, public_slug, source_code, source_channel, request_id, status, version, valid_from, valid_to) VALUES
  (1, 1, 1, 'test-pub-slug-001', 'SRC-VISIBLE-001', 'h5_share', 'seed-req-001', 1, 1, '2026-01-01 00:00:00', '2026-12-31 23:59:59')
  ON DUPLICATE KEY UPDATE status=VALUES(status);
SQL
echo "  seed data inserted"

# Verify
echo "=== VERIFY ==="
$M "$SCHEMA" -e "SELECT id, public_slug, source_code, status FROM marketing_publication;"
$M "$SCHEMA" -e "SELECT id, name, status FROM store;"
$M "$SCHEMA" -e "SELECT COUNT(*) AS table_count FROM information_schema.tables WHERE table_schema='${SCHEMA}';"
echo "SCHEMA_READY=${SCHEMA}"

#!/bin/bash
# TR58: read-only verification (former cleanup.sh, now DELETE-free)
# Historical DELETE operations registered in reported.md
# This script only verifies row counts, does NOT delete
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== READ-ONLY VERIFY (no DELETE) ==="
$M "$SCHEMA" -e "SELECT COUNT(*) AS bi_count FROM booking_inquiry;"
$M "$SCHEMA" -e "SELECT COUNT(*) AS evt_count FROM marketing_attribution_event;"
echo "VERIFY_DONE (zero writes)"

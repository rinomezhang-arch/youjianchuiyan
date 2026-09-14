#!/bin/bash
# TR58: read-only count check (former clean2.sh, now DELETE-free)
# Historical DELETE operations registered in reported.md
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== READ-ONLY COUNT (no DELETE) ==="
$M "$SCHEMA" -e "SELECT COUNT(*) FROM booking_inquiry;"
$M "$SCHEMA" -e "SELECT COUNT(*) FROM marketing_attribution_event;"
echo "COUNT_DONE (zero writes)"

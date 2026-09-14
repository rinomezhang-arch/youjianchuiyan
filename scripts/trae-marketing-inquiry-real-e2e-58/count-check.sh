#!/bin/bash
# TR58: read-only count check (replaces former DELETE cleanup scripts)
# Historical DELETE operations are registered in reported.md
# This script only counts rows, does not modify data
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== READ-ONLY COUNT CHECK ==="
echo "SCHEMA=[REDACTED]"
$M "$SCHEMA" -e "SELECT COUNT(*) AS booking_inquiry_count FROM booking_inquiry;"
$M "$SCHEMA" -e "SELECT COUNT(*) AS attribution_event_count FROM marketing_attribution_event;"
echo "CHECK_DONE (no DELETE, no writes)"

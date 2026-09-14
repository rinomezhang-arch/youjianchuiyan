#!/bin/bash
fuser -k 18080/tcp 2>/dev/null
fuser -k 5174/tcp 2>/dev/null
echo "services stopped"
rm -f ~/tr58-work/frontend_v3/.vite.e2e.config.mjs
echo "temp config removed"
docker ps --filter name=youjian-mysql-test-13317 --format "{{.Names}}|{{.Status}}" | head -1
PROD=$(fuser 8080/tcp 2>/dev/null)
echo "production 8080 PID=$PROD (untouched)"
echo "CLEANUP_DONE"

#!/bin/bash
# Stop test backend (18080) only - NOT production 8080
PID=$(fuser 18080/tcp 2>/dev/null)
if [ -z "$PID" ]; then echo "BACKEND_NOT_RUNNING"; exit 1; fi
echo "Stopping test backend PID=$PID (port 18080 only)"
kill $PID
sleep 2
# Verify production 8080 still running
PROD=$(fuser 8080/tcp 2>/dev/null)
echo "  production 8080 PID=$PROD (should still be running)"
# Verify test backend stopped
TEST=$(fuser 18080/tcp 2>/dev/null)
if [ -n "$TEST" ]; then echo "  [WARN] 18080 still running PID=$TEST"; else echo "  [PASS] test backend stopped"; fi
echo "BACKEND_STOPPED"

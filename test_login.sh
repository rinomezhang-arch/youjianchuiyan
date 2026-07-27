#!/bin/bash
echo "=== 1. Check auth endpoints ==="
curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"rino","password":"002323"}'
echo " /api/auth/login"

curl -s -o /dev/null -w '%{http_code}' -X POST http://localhost:8080/auth/login -H 'Content-Type: application/json' -d '{"username":"rino","password":"002323"}'
echo " /auth/login"

echo "=== 2. Full login response ==="
curl -s -X POST http://localhost:8080/api/auth/login -H 'Content-Type: application/json' -d '{"username":"rino","password":"002323"}'
echo

echo "=== 3. Check AuthController ==="
grep -n 'login\|auth' /home/ubuntu/.openclaw/workspace/youjianchuiyan_restored/backend/src/main/java/com/youjian/banquet/controller/AuthController.java 2>/dev/null | head -20

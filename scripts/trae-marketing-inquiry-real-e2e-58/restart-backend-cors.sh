#!/bin/bash
SCHEMA=$(cat ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "Restarting backend with CORS for test-frontend-address:5174, SCHEMA=[REDACTED]"
PID=$(fuser 18080/tcp 2>/dev/null)
if [ -n "$PID" ]; then kill $PID; sleep 3; fi
cd ~/tr58-work/banquet_project
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:13317/${SCHEMA}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&allowMultiQueries=true&characterEncoding=utf-8"
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=""
export SERVER_PORT=18080
export SPRING_PROFILES_ACTIVE=dev
export TIANLONG_BASE_URL=http://127.0.0.1:1
export DASHSCOPE_BASE_URL=http://127.0.0.1:1
export DEEPSEEK_BASE_URL=http://127.0.0.1:1
export CORS_ALLOWED_ORIGINS="${CORS_TEST_ORIGIN:-http://localhost:5174},http://localhost:5174,http://127.0.0.1:5174"
nohup mvn -B spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/tr58-backend.log 2>&1 &
NEW_PID=$!
echo "NEW_PID=${NEW_PID}"
for i in $(seq 1 60); do
  sleep 2
  CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:18080/actuator/health 2>/dev/null)
  if echo "$CODE" | grep -q 200; then
    echo "  [PASS] backend up after ${i}x2s"
    exit 0
  fi
done
echo "  [FAIL] backend not up"
tail -20 /tmp/tr58-backend.log
exit 1

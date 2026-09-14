#!/bin/bash
set -u
cd ~/tr58-work
SCHEMA=$(cat scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== TR58 E2E 启动后端+前端 ==="
echo "SCHEMA=${SCHEMA}"

# Gate: verify schema, port, datadir
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
DD=$($M -e "SELECT @@port, @@datadir;")
echo "  gate: @@port/datadir=$(printf '%s' "$DD" | tr '\t' ' ')"
GP=$(printf '%s' "$DD" | cut -f1)
GD=$(printf '%s' "$DD" | cut -f2)
if [ "$GP" != "3306" ] || [ "$GD" != "/var/lib/mysql/" ]; then echo "  [FAIL] gate"; exit 1; fi
echo "  [PASS] gate"

# Check 8080 not in use by production
PROD=$(pgrep -f 'banquet.*--spring.profiles.active=prod' 2>/dev/null | head -1)
if [ -n "$PROD" ]; then echo "  [FAIL] production banquet running PID=$PROD"; exit 1; fi
echo "  [PASS] no production backend"

# Kill any existing test backend on 8080
fuser -k 8080/tcp 2>/dev/null; sleep 1
fuser -k 5173/tcp 2>/dev/null; sleep 1

# Start backend
cd ~/tr58-work/banquet_project
export MYSQL_HOST=127.0.0.1
export MYSQL_DATABASE="$SCHEMA"
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:13317/${SCHEMA}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&allowMultiQueries=true&characterEncoding=utf-8"
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=""
export SERVER_PORT=8080
export SPRING_PROFILES_ACTIVE=dev
# Disable external service calls
export TIANLONG_BASE_URL=http://127.0.0.1:1
export DASHSCOPE_BASE_URL=http://127.0.0.1:1
export DEEPSEEK_BASE_URL=http://127.0.0.1:1

echo "  starting backend on port ${SERVER_PORT}..."
nohup mvn -B spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/tr58-backend.log 2>&1 &
BACKEND_PID=$!
echo "  backend PID=${BACKEND_PID}"

# Wait for backend to start (up to 120s)
echo "  waiting for backend..."
for i in $(seq 1 60); do
  sleep 2
  if curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:8080/actuator/health 2>/dev/null | grep -q 200; then
    echo "  [PASS] backend up after ${i}x2s"
    break
  fi
  if [ $i -eq 60 ]; then
    echo "  [FAIL] backend not up in 120s"
    tail -30 /tmp/tr58-backend.log
    exit 1
  fi
done

# Start frontend
cd ~/tr58-work/frontend_v3
# Install deps if needed
if [ ! -d node_modules ]; then
  echo "  installing frontend deps..."
  npm install --silent 2>&1 | tail -5
fi
echo "  starting frontend on port 5173..."
nohup npx vite --host 0.0.0.0 --port 5173 > /tmp/tr58-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "  frontend PID=${FRONTEND_PID}"

# Wait for frontend
for i in $(seq 1 30); do
  sleep 2
  if curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:5173 2>/dev/null | grep -q 200; then
    echo "  [PASS] frontend up after ${i}x2s"
    break
  fi
  if [ $i -eq 30 ]; then
    echo "  [FAIL] frontend not up in 60s"
    tail -20 /tmp/tr58-frontend.log
    exit 1
  fi
done

echo "=== SERVICES READY ==="
echo "  backend: http://127.0.0.1:8080"
echo "  frontend: http://127.0.0.1:5173"
echo "  PID backend=${BACKEND_PID} frontend=${FRONTEND_PID}"
echo "TR58_READY"

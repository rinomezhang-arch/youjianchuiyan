#!/bin/bash
set -u
cd ~/tr58-work
SCHEMA=$(cat scripts/trae-marketing-inquiry-real-e2e-58/last_schema.txt)
echo "=== TR58 E2E 启动后端(18080)+前端(5174) ==="
echo "SCHEMA=${SCHEMA}"

# Gate
M="mysql -uroot --protocol=tcp -h127.0.0.1 -P13317 -N"
DD=$($M -e "SELECT @@port, @@datadir;")
echo "  gate: $(printf '%s' "$DD" | tr '\t' ' ')"

# Use non-production port 18080 to avoid clashing with production 8080
TEST_PORT=18080
FRONT_PORT=5174

# Kill any existing test services on these ports
fuser -k ${TEST_PORT}/tcp 2>/dev/null; sleep 1
fuser -k ${FRONT_PORT}/tcp 2>/dev/null; sleep 1

# Start backend on 18080
cd ~/tr58-work/banquet_project
export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:13317/${SCHEMA}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&allowMultiQueries=true&characterEncoding=utf-8"
export SPRING_DATASOURCE_USERNAME=root
export SPRING_DATASOURCE_PASSWORD=""
export SERVER_PORT=${TEST_PORT}
export SPRING_PROFILES_ACTIVE=dev
export TIANLONG_BASE_URL=http://127.0.0.1:1
export DASHSCOPE_BASE_URL=http://127.0.0.1:1
export DEEPSEEK_BASE_URL=http://127.0.0.1:1

echo "  starting backend on port ${TEST_PORT}..."
nohup mvn -B spring-boot:run -Dspring-boot.run.profiles=dev > /tmp/tr58-backend.log 2>&1 &
BACKEND_PID=$!
echo "  backend PID=${BACKEND_PID}"

# Wait for backend
for i in $(seq 1 90); do
  sleep 2
  CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:${TEST_PORT}/actuator/health 2>/dev/null)
  if echo "$CODE" | grep -q 200; then
    echo "  [PASS] backend up after ${i}x2s"
    break
  fi
  if [ $i -eq 90 ]; then
    echo "  [FAIL] backend not up in 180s"
    tail -50 /tmp/tr58-backend.log
    exit 1
  fi
done

# Start frontend on 5174 with proxy to 18080
# Write a wrapper vite config in the allowed scripts path
cat > ~/tr58-work/scripts/trae-marketing-inquiry-real-e2e-58/vite.e2e.config.js <<VEOF
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [
    vue(),
    AutoImport({ resolvers: [ElementPlusResolver()], imports: ['vue', 'vue-router', 'pinia'] }),
    Components({ resolvers: [ElementPlusResolver()], dirs: ['src/components'] })
  ],
  resolve: { alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) } },
  server: {
    host: '0.0.0.0',
    port: 5174,
    proxy: {
      '/api': { target: 'http://127.0.0.1:18080', changeOrigin: true }
    }
  }
})
VEOF

cd ~/tr58-work/frontend_v3
if [ ! -d node_modules ]; then
  echo "  installing frontend deps..."
  npm install --silent 2>&1 | tail -5
fi
echo "  starting frontend on port ${FRONT_PORT}..."
nohup npx vite --config ../scripts/trae-marketing-inquiry-real-e2e-58/vite.e2e.config.js --host 0.0.0.0 --port ${FRONT_PORT} > /tmp/tr58-frontend.log 2>&1 &
FRONTEND_PID=$!
echo "  frontend PID=${FRONTEND_PID}"

# Wait for frontend
for i in $(seq 1 30); do
  sleep 2
  CODE=$(curl -s -o /dev/null -w "%{http_code}" http://127.0.0.1:${FRONT_PORT} 2>/dev/null)
  if echo "$CODE" | grep -q 200; then
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
echo "  backend: http://127.0.0.1:${TEST_PORT}"
echo "  frontend: http://127.0.0.1:${FRONT_PORT}"
echo "  PID backend=${BACKEND_PID} frontend=${FRONTEND_PID}"
echo "TR58_READY"

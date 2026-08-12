<template>
  <div class="info-page">
    <div class="page-header">
      <h2 class="page-title">系统信息与运行状态</h2>
      <p class="page-subtitle">System Information & Status</p>
      <button class="refresh-btn" @click="refreshData">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <polyline points="23 4 23 10 17 10"/>
          <polyline points="1 20 1 14 7 14"/>
          <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
        </svg>
        刷新数据
      </button>
    </div>

    <div class="status-cards">
      <div class="status-card" :class="serverStatus.online ? 'online' : 'offline'">
        <div class="status-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <rect x="2" y="2" width="20" height="8" rx="2"/>
            <rect x="2" y="14" width="20" height="8" rx="2"/>
            <line x1="6" y1="6" x2="6.01" y2="6"/>
            <line x1="6" y1="18" x2="6.01" y2="18"/>
          </svg>
        </div>
        <div class="status-info">
          <div class="status-label">服务器状态</div>
          <div class="status-value">{{ serverStatus.online ? '运行中' : '离线' }}</div>
        </div>
        <div class="status-indicator" :class="serverStatus.online ? 'green' : 'red'"></div>
      </div>

      <div class="status-card" :class="serverStatus.dbOnline ? 'online' : 'offline'">
        <div class="status-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <ellipse cx="12" cy="5" rx="9" ry="3"/>
            <path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/>
            <path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>
          </svg>
        </div>
        <div class="status-info">
          <div class="status-label">数据库状态</div>
          <div class="status-value">{{ serverStatus.dbOnline ? '连接正常' : '连接失败' }}</div>
        </div>
        <div class="status-indicator" :class="serverStatus.dbOnline ? 'green' : 'red'"></div>
      </div>

      <div class="status-card version">
        <div class="status-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"/>
            <line x1="7" y1="7" x2="7.01" y2="7"/>
          </svg>
        </div>
        <div class="status-info">
          <div class="status-label">系统版本</div>
          <div class="status-value">v3.2.1</div>
        </div>
      </div>

      <div class="status-card uptime">
        <div class="status-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="status-info">
          <div class="status-label">运行时间</div>
          <div class="status-value">{{ serverStatus.uptime }}</div>
        </div>
      </div>
    </div>

    <div class="detail-section">
      <h3 class="section-title">服务器详情</h3>
      <div class="detail-grid">
        <div class="detail-item">
          <div class="detail-label">CPU使用率</div>
          <div class="detail-value">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: serverStatus.cpu + '%' }" :class="getProgressClass(serverStatus.cpu)"></div>
            </div>
            <span class="progress-text">{{ serverStatus.cpu }}%</span>
          </div>
        </div>

        <div class="detail-item">
          <div class="detail-label">内存使用率</div>
          <div class="detail-value">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: serverStatus.memory + '%' }" :class="getProgressClass(serverStatus.memory)"></div>
            </div>
            <span class="progress-text">{{ serverStatus.memory }}%</span>
          </div>
        </div>

        <div class="detail-item">
          <div class="detail-label">磁盘使用率</div>
          <div class="detail-value">
            <div class="progress-bar">
              <div class="progress-fill" :style="{ width: serverStatus.disk + '%' }" :class="getProgressClass(serverStatus.disk)"></div>
            </div>
            <span class="progress-text">{{ serverStatus.disk }}%</span>
          </div>
        </div>

        <div class="detail-item">
          <div class="detail-label">数据库连接数</div>
          <div class="detail-number">{{ serverStatus.dbConnections }} / 100</div>
        </div>

        <div class="detail-item">
          <div class="detail-label">QPS (查询/秒)</div>
          <div class="detail-number">{{ serverStatus.qps }}</div>
        </div>

        <div class="detail-item">
          <div class="detail-label">最后更新</div>
          <div class="detail-number">{{ serverStatus.lastUpdate }}</div>
        </div>
      </div>
    </div>

    <div class="info-table-section">
      <h3 class="section-title">系统环境</h3>
      <table class="info-table">
        <tbody>
          <tr v-for="item in envInfo" :key="item.label">
            <td class="info-label">{{ item.label }}</td>
            <td class="info-value">{{ item.value }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup>
import { reactive, onMounted } from 'vue'

const serverStatus = reactive({
  online: true,
  dbOnline: true,
  uptime: '3天 14小时 22分',
  cpu: 42,
  memory: 68,
  disk: 55,
  dbConnections: 23,
  qps: 156,
  lastUpdate: new Date().toLocaleTimeString('zh-CN')
})

const envInfo = [
  { label: '操作系统', value: 'Ubuntu 22.04 LTS' },
  { label: 'Node.js版本', value: 'v18.17.0' },
  { label: 'Java版本', value: 'OpenJDK 17.0.8' },
  { label: 'MySQL版本', value: '8.0.34' },
  { label: 'Redis版本', value: '7.0.12' },
  { label: 'Nginx版本', value: '1.24.0' },
  { label: '服务器IP', value: '100.70.171.0 (Tailscale)' },
  { label: '部署路径', value: '/opt/youjianchuiyan/' }
]

function refreshData() {
  serverStatus.cpu = Math.floor(Math.random() * 40 + 30)
  serverStatus.memory = Math.floor(Math.random() * 30 + 55)
  serverStatus.disk = Math.floor(Math.random() * 20 + 50)
  serverStatus.dbConnections = Math.floor(Math.random() * 40 + 15)
  serverStatus.qps = Math.floor(Math.random() * 100 + 120)
  serverStatus.lastUpdate = new Date().toLocaleTimeString('zh-CN')
}

function getProgressClass(value) {
  if (value >= 80) return 'danger'
  if (value >= 60) return 'warning'
  return 'normal'
}

onMounted(() => {
  refreshData()
})
</script>

<style scoped>
.info-page {
  max-width: 1200px;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 28px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.refresh-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.refresh-btn:hover {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.status-card {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  position: relative;
  overflow: hidden;
}

.status-card.online {
  border-color: rgba(74, 124, 89, 0.3);
  background: rgba(74, 124, 89, 0.04);
}

.status-card.offline {
  border-color: rgba(220, 38, 38, 0.3);
  background: rgba(220, 38, 38, 0.04);
}

.status-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: rgba(45, 74, 62, 0.08);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  flex-shrink: 0;
}

.status-icon svg {
  width: 22px;
  height: 22px;
}

.status-info {
  flex: 1;
}

.status-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
}

.status-value {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.status-indicator {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  position: absolute;
  top: 16px;
  right: 16px;
}

.status-indicator.green {
  background: #10b981;
  box-shadow: 0 0 8px rgba(16, 185, 129, 0.6);
  animation: pulse 2s infinite;
}

.status-indicator.red {
  background: #ef4444;
  box-shadow: 0 0 8px rgba(239, 68, 68, 0.6);
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

.detail-section,
.info-table-section {
  background: var(--color-bg-alt);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 20px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.detail-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.detail-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.detail-value {
  display: flex;
  align-items: center;
  gap: 12px;
}

.progress-bar {
  flex: 1;
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.progress-fill.normal {
  background: linear-gradient(90deg, #10b981, #34d399);
}

.progress-fill.warning {
  background: linear-gradient(90deg, #f59e0b, #fbbf24);
}

.progress-fill.danger {
  background: linear-gradient(90deg, #ef4444, #f87171);
}

.progress-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  min-width: 45px;
  text-align: right;
}

.detail-number {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.info-table {
  width: 100%;
  border-collapse: collapse;
}

.info-table tr {
  border-bottom: 1px solid var(--color-border-light);
}

.info-table tr:last-child {
  border-bottom: none;
}

.info-table td {
  padding: 12px 0;
}

.info-label {
  font-size: 13px;
  color: var(--color-text-secondary);
  width: 180px;
  font-weight: 500;
}

.info-value {
  font-size: 13px;
  color: var(--color-text);
  font-family: 'Courier New', monospace;
}

@media (max-width: 1024px) {
  .status-cards {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .detail-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .status-cards {
    grid-template-columns: 1fr;
  }
  
  .page-header {
    flex-direction: column;
    gap: 12px;
  }
  
  .refresh-btn {
    align-self: flex-start;
  }
}
</style>

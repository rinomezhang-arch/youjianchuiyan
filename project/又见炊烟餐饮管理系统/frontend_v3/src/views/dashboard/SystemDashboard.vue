<template>
  <div class="system-dashboard">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">系统信息与运行状态 · System Info</h1>
        <p class="page-desc">实时监控系统运行指标 · CPU/内存/磁盘/网络 · 服务健康状态</p>
      </div>
      <div class="header-right">
        <span class="last-update">数据刷新：{{ lastUpdate }}</span>
        <el-button type="primary" @click="refreshData">刷新数据</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-cpu">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="4" y="4" width="16" height="16" rx="2"/><rect x="9" y="9" width="6" height="6"/><line x1="9" y1="1" x2="9" y2="4"/><line x1="15" y1="1" x2="15" y2="4"/><line x1="9" y1="20" x2="9" y2="23"/><line x1="15" y1="20" x2="15" y2="23"/><line x1="20" y1="9" x2="23" y2="9"/><line x1="20" y1="14" x2="23" y2="14"/><line x1="1" y1="9" x2="4" y2="9"/><line x1="1" y1="14" x2="4" y2="14"/>
            </svg>
          </div>
          <span class="stat-label">CPU 使用率</span>
        </div>
        <div class="stat-value">{{ cpuUsage }}%</div>
        <div class="stat-meter"><div class="stat-fill" :style="{ width: cpuUsage + '%' }"></div></div>
        <div class="stat-sub">{{ cpuInfo }}</div>
      </div>
      <div class="stat-card stat-mem">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="2" y="6" width="20" height="12" rx="2"/><line x1="6" y1="12" x2="10" y2="12"/><line x1="14" y1="12" x2="18" y2="12"/>
            </svg>
          </div>
          <span class="stat-label">内存使用</span>
        </div>
        <div class="stat-value">{{ memUsage }}%</div>
        <div class="stat-meter"><div class="stat-fill mem" :style="{ width: memUsage + '%' }"></div></div>
        <div class="stat-sub">{{ memInfo }}</div>
      </div>
      <div class="stat-card stat-disk">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <ellipse cx="12" cy="5" rx="9" ry="3"/><path d="M21 12c0 1.66-4 3-9 3s-9-1.34-9-3"/><path d="M3 5v14c0 1.66 4 3 9 3s9-1.34 9-3V5"/>
            </svg>
          </div>
          <span class="stat-label">磁盘使用</span>
        </div>
        <div class="stat-value">{{ diskUsage }}%</div>
        <div class="stat-meter"><div class="stat-fill disk" :style="{ width: diskUsage + '%' }"></div></div>
        <div class="stat-sub">{{ diskInfo }}</div>
      </div>
      <div class="stat-card stat-net">
        <div class="stat-header">
          <div class="stat-icon">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="10"/><path d="M2 12h20"/><path d="M12 2a15.3 15.3 0 0 1 4 10 15.3 15.3 0 0 1-4 10 15.3 15.3 0 0 1-4-10 15.3 15.3 0 0 1 4-10z"/>
            </svg>
          </div>
          <span class="stat-label">网络连通</span>
        </div>
        <div class="stat-value net-status" :class="netOnline ? 'online' : 'offline'">{{ netOnline ? '在线' : '离线' }}</div>
        <div class="stat-sub">{{ netInfo }}</div>
      </div>
    </div>

    <!-- 服务运行状态 -->
    <div class="section">
      <h3 class="section-title">服务运行状态</h3>
      <div class="service-grid">
        <div class="service-card" v-for="svc in services" :key="svc.name">
          <div class="svc-indicator" :class="svc.status"></div>
          <div class="svc-info">
            <div class="svc-name">{{ svc.name }}</div>
            <div class="svc-detail">{{ svc.detail }}</div>
          </div>
          <el-tag :type="svc.status === 'running' ? 'success' : svc.status === 'stopped' ? 'danger' : 'info'" size="small" effect="plain">
            {{ svc.status === 'running' ? '运行中' : svc.status === 'stopped' ? '已停止' : '待检测' }}
          </el-tag>
        </div>
      </div>
    </div>

    <!-- 系统版本与配置信息 -->
    <div class="section">
      <h3 class="section-title">系统版本与配置信息</h3>
      <div class="info-grid">
        <div class="info-card">
          <div class="info-label">系统版本</div>
          <div class="info-value">v3.5.1</div>
        </div>
        <div class="info-card">
          <div class="info-label">前端框架</div>
          <div class="info-value">Vue 3 + Vite 5</div>
        </div>
        <div class="info-card">
          <div class="info-label">UI 组件库</div>
          <div class="info-value">Element Plus</div>
        </div>
        <div class="info-card">
          <div class="info-label">数据库</div>
          <div class="info-value">MySQL 8.4</div>
        </div>
        <div class="info-card">
          <div class="info-label">后端框架</div>
          <div class="info-value">Spring Boot 3</div>
        </div>
        <div class="info-card">
          <div class="info-label">部署平台</div>
          <div class="info-value">腾讯云 CVM</div>
        </div>
        <div class="info-card">
          <div class="info-label">Node 版本</div>
          <div class="info-value">v20 LTS</div>
        </div>
        <div class="info-card">
          <div class="info-label">仓库地址</div>
          <div class="info-value">本地 Windows 办公室001</div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'

const lastUpdate = ref(new Date().toLocaleString('zh-CN'))
const cpuUsage = ref('--')
const memUsage = ref('--')
const diskUsage = ref('--')
const cpuInfo = ref('等待接入监控')
const memInfo = ref('等待接入监控')
const diskInfo = ref('等待接入监控')
const netOnline = ref(true)
const netInfo = ref('Tailscale / 公网正常')

const services = ref([
  { name: 'MySQL 8.4', detail: 'localhost:3306 · banquet', status: 'running' },
  { name: 'Java 后端 API', detail: 'Spring Boot :8080', status: 'pending' },
  { name: 'Vite 开发服务器', detail: 'Vue 3 :5173', status: 'running' },
  { name: 'Nginx 前端服务', detail: 'youjianchuiyan.com', status: 'running' }
])

function refreshData() {
  lastUpdate.value = new Date().toLocaleString('zh-CN')
  // TODO: 接入实际监控数据源
}
</script>

<style scoped>
.system-dashboard { max-width: 1200px; margin: 0 auto; }

.page-header {
  display: flex; justify-content: space-between; align-items: flex-start;
  margin-bottom: 24px; flex-wrap: wrap; gap: 12px;
}
.header-left { flex: 1; }
.page-title { font-size: 22px; font-weight: 700; color: #1a1a1a; margin-bottom: 4px; }
.page-desc { font-size: 13px; color: #8a8a8a; }
.header-right { display: flex; align-items: center; gap: 10px; }
.last-update { font-size: 12px; color: #aaa; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 28px; }
.stat-card {
  background: #fff; border: 1px solid #e8e8e8; border-radius: 10px;
  padding: 20px; transition: all 0.2s;
}
.stat-card:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.06); }
.stat-header { display: flex; align-items: center; gap: 8px; margin-bottom: 10px; }
.stat-icon { width: 32px; height: 32px; border-radius: 8px; display: flex; align-items: center; justify-content: center; }
.stat-cpu .stat-icon { background: rgba(45,74,62,0.08); color: #2D4A3E; }
.stat-mem .stat-icon { background: rgba(74,124,89,0.08); color: #4A7C59; }
.stat-disk .stat-icon { background: rgba(196,163,90,0.08); color: #C4A35A; }
.stat-net .stat-icon { background: rgba(91,123,138,0.08); color: #5B7B8A; }
.stat-icon svg { width: 16px; height: 16px; }
.stat-label { font-size: 12px; font-weight: 500; color: #666; }
.stat-value { font-size: 28px; font-weight: 700; color: #1a1a1a; margin-bottom: 8px; }
.stat-value.net-status { font-size: 22px; }
.stat-value.net-status.online { color: #4A7C59; }
.stat-value.net-status.offline { color: #c0392b; }
.stat-meter { height: 4px; background: #f0f0f0; border-radius: 2px; margin-bottom: 6px; overflow: hidden; }
.stat-fill { height: 100%; background: linear-gradient(90deg, #2D4A3E, #4A7C59); border-radius: 2px; transition: width 0.5s; }
.stat-fill.mem { background: linear-gradient(90deg, #4A7C59, #6A9C79); }
.stat-fill.disk { background: linear-gradient(90deg, #C4A35A, #D4B36A); }
.stat-sub { font-size: 11px; color: #aaa; }

.section { margin-bottom: 28px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a1a1a; margin-bottom: 14px; }

.service-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.service-card {
  display: flex; align-items: center; gap: 12px;
  padding: 14px 16px; background: #fff; border: 1px solid #e8e8e8;
  border-radius: 8px; transition: all 0.2s;
}
.service-card:hover { box-shadow: 0 2px 8px rgba(0,0,0,0.04); }
.svc-indicator { width: 10px; height: 10px; border-radius: 50%; flex-shrink: 0; }
.svc-indicator.running { background: #4A7C59; box-shadow: 0 0 6px rgba(74,124,89,0.4); }
.svc-indicator.stopped { background: #c0392b; box-shadow: 0 0 6px rgba(192,57,43,0.4); }
.svc-indicator.pending { background: #ccc; }
.svc-info { flex: 1; min-width: 0; }
.svc-name { font-size: 13px; font-weight: 500; color: #333; }
.svc-detail { font-size: 11px; color: #999; margin-top: 2px; }

.info-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; }
.info-card {
  padding: 14px; background: #fff; border: 1px solid #e8e8e8;
  border-radius: 8px; display: flex; flex-direction: column; gap: 4px;
}
.info-label { font-size: 11px; color: #999; }
.info-value { font-size: 13px; font-weight: 500; color: #333; }

@media (max-width: 900px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
  .info-grid { grid-template-columns: repeat(2, 1fr); }
  .service-grid { grid-template-columns: 1fr; }
}
@media (max-width: 600px) {
  .stats-row, .info-grid { grid-template-columns: 1fr; }
}
</style>

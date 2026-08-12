<template>
  <div class="engineering-page">
    <div class="page-header">
      <h2 class="page-title">工程管理总览 · Engineering Overview</h2>
      <p class="page-subtitle">Decoration, Maintenance, Energy & Safety Management</p>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-content">
          <div class="stat-label">装修项目 · Decoration</div>
          <div class="stat-value">{{ stats.decorationProjects }}</div>
          <div class="stat-sub">进行中 {{ stats.decorationActive }} · 待审批 {{ stats.decorationPending }}</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-content">
          <div class="stat-label">设备维护 · Maintenance</div>
          <div class="stat-value">{{ stats.maintenanceTasks }}</div>
          <div class="stat-sub">待处理 {{ stats.maintenancePending }} · 本月完成 {{ stats.maintenanceDone }}</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#D4A853' }">
        <div class="stat-content">
          <div class="stat-label">本月能耗 · Energy</div>
          <div class="stat-value">{{ stats.energyUsage }}</div>
          <div class="stat-sub">电 {{ stats.energyElectric }}kWh · 水 {{ stats.energyWater }}t</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#C0392B' }">
        <div class="stat-content">
          <div class="stat-label">安全隐患 · Safety</div>
          <div class="stat-value">{{ stats.safetyIssues }}</div>
          <div class="stat-sub">待整改 {{ stats.safetyPending }} · 已整改 {{ stats.safetyResolved }}</div>
        </div>
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="quick-actions-card">
      <h3 class="section-title">快捷入口 · Quick Access</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('decoration')">
          <span class="action-text">装修管理 · Decoration</span>
        </div>
        <div class="action-card" @click="goTo('maintenance')">
          <span class="action-text">设备维护 · Maintenance</span>
        </div>
        <div class="action-card" @click="goTo('energy')">
          <span class="action-text">能耗管理 · Energy</span>
        </div>
        <div class="action-card" @click="goTo('safety')">
          <span class="action-text">安全管理 · Safety</span>
        </div>
      </div>
    </div>

    <!-- 装修项目列表 -->
    <div class="content-grid">
      <div class="content-card wide">
        <h3 class="section-title">装修项目 · Decoration Projects</h3>
        <div class="table-wrapper">
          <el-table :data="decorationProjects" border style="width: 100%">
            <el-table-column prop="name" label="项目名称" min-width="160" />
            <el-table-column prop="type" label="类型" width="100" />
            <el-table-column prop="manager" label="负责人" width="100" />
            <el-table-column label="预算" width="130">
              <template #default="scope">¥{{ (scope.row.budget || 0).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="进度" width="180">
              <template #default="scope">
                <div class="progress-bar">
                  <div class="progress-fill" :style="{ width: (scope.row.progress || 0) + '%' }"></div>
                  <span class="progress-text">{{ scope.row.progress || 0 }}%</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="scope">
                <span :class="['status-badge', scope.row.status]">{{ statusText(scope.row.status) }}</span>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <!-- 设备维护工单 -->
      <div class="content-card">
        <h3 class="section-title">维护工单 · Maintenance Orders</h3>
        <div class="order-list">
          <div v-for="o in maintenanceOrders" :key="o.id" class="order-item" :class="o.priority">
            <div class="order-info">
              <div class="order-title">{{ o.title }}</div>
              <div class="order-meta">{{ o.location }} · {{ o.time }}</div>
            </div>
            <span class="order-status">{{ orderStatusText(o.status) }}</span>
          </div>
        </div>
      </div>

      <!-- 能耗趋势 -->
      <div class="content-card">
        <h3 class="section-title">能耗趋势 · Energy Trend</h3>
        <div class="energy-chart">
          <div v-for="(item, i) in energyTrend" :key="i" class="energy-bar-group">
            <div class="energy-bars">
              <div class="energy-bar electric" :style="{ height: (item.electric / 500 * 100) + '%' }" :title="'电: ' + item.electric + 'kWh'"></div>
              <div class="energy-bar water" :style="{ height: (item.water / 100 * 100) + '%' }" :title="'水: ' + item.water + 't'"></div>
            </div>
            <span class="energy-label">{{ item.month }}</span>
          </div>
        </div>
        <div class="chart-legend">
          <span class="legend-item"><span class="legend-dot electric"></span> 用电 kWh</span>
          <span class="legend-item"><span class="legend-dot water"></span> 用水 t</span>
        </div>
      </div>

      <!-- 安全巡检 -->
      <div class="content-card">
        <h3 class="section-title">安全巡检 · Safety Inspection</h3>
        <div class="order-list">
          <div v-for="s in safetyIssues" :key="s.id" class="order-item" :class="s.severity">
            <div class="order-info">
              <div class="order-title">{{ s.title }}</div>
              <div class="order-meta">{{ s.location }} · {{ s.time }}</div>
            </div>
            <span class="order-status">{{ safetyStatusText(s.status) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const goTo = (path) => {
  router.push('/dashboard/' + path)
}

const stats = ref({
  decorationProjects: 0,
  decorationActive: 0,
  decorationPending: 0,
  maintenanceTasks: 0,
  maintenancePending: 0,
  maintenanceDone: 0,
  energyUsage: 0,
  energyElectric: 0,
  energyWater: 0,
  safetyIssues: 0,
  safetyPending: 0,
  safetyResolved: 0,
})

const decorationProjects = ref([])

const maintenanceOrders = ref([])

const energyTrend = ref([])

const safetyIssues = ref([])

const statusText = (s) => ({ active: '进行中', pending: '待审批', done: '已完成' }[s] || s)
const orderStatusText = (s) => ({ pending: '待处理', processing: '处理中', done: '已完成' }[s] || s)
const safetyStatusText = (s) => ({ pending: '待整改', resolved: '已整改' }[s] || s)
</script>

<style scoped>
.engineering-page {
  padding: 24px 32px;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0 0 4px 0;
}

.page-subtitle {
  font-size: 13px;
  color: #8a9a8e;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  gap: 16px;
  border: 1px solid #e8ece9;
}

.stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(45, 74, 62, 0.06);
  flex-shrink: 0;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: #8a9a8e;
  margin-bottom: 4px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: #a0b0a5;
  margin-top: 4px;
}

.quick-actions-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 24px;
  border: 1px solid #e8ece9;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a2f23;
  margin: 0 0 16px 0;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 20px 16px;
  border-radius: 8px;
  background: #f8f9f8;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
}

.action-card:hover {
  background: #fff;
  border-color: #2D4A3E;
  box-shadow: 0 2px 8px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
}

.action-icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.action-icon svg {
  width: 22px;
  height: 22px;
}

.action-text {
  font-size: 13px;
  color: #4a5c50;
  font-weight: 500;
}

.content-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.content-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  border: 1px solid #e8ece9;
}

.content-card.wide {
  grid-column: 1 / -1;
}

.table-wrapper {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th {
  text-align: left;
  padding: 10px 12px;
  font-weight: 600;
  color: #6a7a6e;
  border-bottom: 2px solid #e8ece9;
  font-size: 12px;
}

.data-table td {
  padding: 10px 12px;
  border-bottom: 1px solid #f0f2f0;
  color: #3a4a3e;
}

.progress-bar {
  display: flex;
  align-items: center;
  gap: 8px;
}

.progress-fill {
  height: 8px;
  background: linear-gradient(90deg, #2D4A3E, #4A7C59);
  border-radius: 4px;
  min-width: 20px;
  flex: 1;
}

.progress-text {
  font-size: 12px;
  color: #6a7a6e;
  min-width: 36px;
}

.status-badge {
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 11px;
  font-weight: 500;
}

.status-badge.active {
  background: rgba(45, 74, 62, 0.1);
  color: #2D4A3E;
}

.status-badge.pending {
  background: rgba(212, 168, 83, 0.12);
  color: #b8922e;
}

.status-badge.done {
  background: rgba(74, 124, 89, 0.1);
  color: #4A7C59;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  border-radius: 6px;
  background: #f8f9f8;
  border-left: 3px solid #e8ece9;
}

.order-item.high {
  border-left-color: #C0392B;
}

.order-item.medium {
  border-left-color: #D4A853;
}

.order-item.low {
  border-left-color: #4A7C59;
}

.order-info {
  flex: 1;
}

.order-title {
  font-size: 13px;
  font-weight: 500;
  color: #1a2f23;
}

.order-meta {
  font-size: 11px;
  color: #8a9a8e;
  margin-top: 2px;
}

.order-status {
  font-size: 11px;
  padding: 3px 10px;
  border-radius: 12px;
  background: rgba(45, 74, 62, 0.08);
  color: #4a5c50;
  white-space: nowrap;
}

.energy-chart {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  height: 160px;
  padding: 12px 0;
}

.energy-bar-group {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100%;
  justify-content: flex-end;
}

.energy-bars {
  display: flex;
  gap: 3px;
  align-items: flex-end;
  height: 120px;
  width: 100%;
  justify-content: center;
}

.energy-bar {
  width: 14px;
  border-radius: 3px 3px 0 0;
  min-height: 4px;
  transition: height 0.3s;
}

.energy-bar.electric {
  background: linear-gradient(180deg, #D4A853, #e8c97a);
}

.energy-bar.water {
  background: linear-gradient(180deg, #5B7B8A, #7a9baa);
}

.energy-label {
  font-size: 11px;
  color: #8a9a8e;
  margin-top: 6px;
}

.chart-legend {
  display: flex;
  gap: 16px;
  justify-content: center;
  margin-top: 12px;
}

.legend-item {
  font-size: 11px;
  color: #6a7a6e;
  display: flex;
  align-items: center;
  gap: 4px;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.legend-dot.electric {
  background: #D4A853;
}

.legend-dot.water {
  background: #5B7B8A;
}
</style>

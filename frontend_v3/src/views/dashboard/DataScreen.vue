<template>
  <div class="data-screen-page">
    <div class="page-header">
      <h2 class="page-title">经营数据大屏分析</h2>
      <p class="page-subtitle">Data Analytics Dashboard</p>
    </div>

    <div class="overview-row">
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">综合营收</span>
          <span class="card-trend" :class="overview.revenueTrend >= 0 ? 'up' : 'down'">{{ overview.revenueTrend >= 0 ? '+' : '' }}{{ overview.revenueTrend }}%</span>
        </div>
        <div class="card-value">{{ loading.overview ? '...' : '¥' + overview.revenue }}</div>
        <div class="card-sub">今日实时数据</div>
      </div>
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">客流量</span>
          <span class="card-trend" :class="overview.customerTrend >= 0 ? 'up' : 'down'">{{ overview.customerTrend >= 0 ? '+' : '' }}{{ overview.customerTrend }}%</span>
        </div>
        <div class="card-value">{{ loading.overview ? '...' : overview.customerCount }}</div>
        <div class="card-sub">今日进店人数</div>
      </div>
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">客单价</span>
          <span class="card-trend" :class="overview.avgPriceTrend >= 0 ? 'up' : 'down'">{{ overview.avgPriceTrend >= 0 ? '+' : '' }}{{ overview.avgPriceTrend }}%</span>
        </div>
        <div class="card-value">{{ loading.overview ? '...' : '¥' + overview.avgPrice }}</div>
        <div class="card-sub">平均消费金额</div>
      </div>
      <div class="overview-card">
        <div class="card-header">
          <span class="card-label">毛利率</span>
          <span class="card-trend" :class="overview.grossMarginTrend >= 0 ? 'up' : 'down'">{{ overview.grossMarginTrend >= 0 ? '+' : '' }}{{ overview.grossMarginTrend }}%</span>
        </div>
        <div class="card-value">{{ loading.overview ? '...' : overview.grossMargin + '%' }}</div>
        <div class="card-sub">成本控制优良</div>
      </div>
    </div>

    <div class="main-section">
      <div class="chart-card">
        <h3 class="card-title">营收趋势分析</h3>
        <div class="chart-placeholder">
          <div class="bar-chart">
            <div class="bar-group" v-for="(item, index) in revenueData" :key="index">
              <div class="bar" :style="{ height: item.percent + '%' }">
                <span class="bar-value">{{ item.value }}</span>
              </div>
              <span class="bar-label">{{ item.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="card-title">菜品热销排行</h3>
        <div class="ranking-list">
          <div class="ranking-item" v-for="(item, index) in hotDishes" :key="index">
            <div class="rank-badge" :class="'rank-' + (index + 1)">{{ index + 1 }}</div>
            <div class="dish-info">
              <div class="dish-name">{{ item.name }}</div>
              <div class="dish-meta">{{ item.sales }} 份 · ¥{{ item.revenue }}</div>
            </div>
            <div class="dish-percent">
              <div class="percent-bar">
                <div class="percent-fill" :style="{ width: item.percent + '%' }"></div>
              </div>
              <span class="percent-text">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="chart-card">
        <h3 class="card-title">客户分析</h3>
        <div class="customer-grid">
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">{{ loading.customer ? '...' : customerAnalysis.repurchaseRate + '%' }}</div>
              <div class="customer-label">复购率</div>
            </div>
          </div>
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">{{ loading.customer ? '...' : customerAnalysis.memberRatio + '%' }}</div>
              <div class="customer-label">会员消费占比</div>
            </div>
          </div>
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">{{ loading.customer ? '...' : customerAnalysis.newMembers }}</div>
              <div class="customer-label">今日新增会员</div>
            </div>
          </div>
          <div class="customer-item">
            <div class="customer-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
                <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                <circle cx="9" cy="7" r="4"/>
              </svg>
            </div>
            <div class="customer-content">
              <div class="customer-value">{{ loading.customer ? '...' : customerAnalysis.peakHour }}</div>
              <div class="customer-label">客流高峰时段</div>
            </div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="card-title">成本分析</h3>
        <div class="cost-section">
          <div class="cost-item" v-for="(item, index) in costAnalysis" :key="index">
            <div class="cost-header">
              <span class="cost-label">{{ item.label }}</span>
              <span class="cost-value">¥{{ item.value }}</span>
            </div>
            <div class="cost-bar">
              <div class="cost-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
            </div>
            <div class="cost-meta">占营收 {{ item.ratio }}%</div>
          </div>
        </div>
      </div>

      <div class="chart-card">
        <h3 class="card-title">异常预警</h3>
        <div class="alert-list">
          <div class="alert-item danger" v-for="(alert, index) in alerts" :key="index">
            <div class="alert-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
                <path d="M12 9v4"/>
                <path d="M12 17h.01"/>
                <circle cx="12" cy="12" r="10"/>
              </svg>
            </div>
            <div class="alert-content">
              <div class="alert-title">{{ alert.title }}</div>
              <div class="alert-meta">{{ alert.meta }}</div>
            </div>
            <button class="alert-action">处理</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'

// Loading states
const loading = ref({
  overview: false,
  revenue: false,
  dishes: false,
  customer: false,
  cost: false,
  alerts: false
})

// Reactive data refs
const overview = ref({
  revenue: 0,
  revenueTrend: 0,
  customerCount: 0,
  customerTrend: 0,
  avgPrice: 0,
  avgPriceTrend: 0,
  grossMargin: 0,
  grossMarginTrend: 0
})

const revenueData = ref([])
const hotDishes = ref([])
const customerAnalysis = ref({
  repurchaseRate: 0,
  memberRatio: 0,
  newMembers: 0,
  peakHour: '--:--'
})
const costAnalysis = ref([])
const alerts = ref([])

// Auto-refresh interval
let refreshInterval = null

// API functions
async function getScreenOverview() {
  loading.value.overview = true
  try {
    const { data } = await request.get('/dashboard/screen/overview')
    overview.value = {
      revenue: data.revenue || 0,
      revenueTrend: data.revenueTrend || 0,
      customerCount: data.customerCount || 0,
      customerTrend: data.customerTrend || 0,
      avgPrice: data.avgPrice || 0,
      avgPriceTrend: data.avgPriceTrend || 0,
      grossMargin: data.grossMargin || 0,
      grossMarginTrend: data.grossMarginTrend || 0
    }
  } catch (e) {
    console.error('获取概览数据失败:', e)
  } finally {
    loading.value.overview = false
  }
}

async function getRevenueTrend() {
  loading.value.revenue = true
  try {
    const { data } = await request.get('/dashboard/screen/revenue-trend')
    if (Array.isArray(data)) {
      const maxVal = Math.max(...data.map(d => d.value))
      revenueData.value = data.map(d => ({
        label: d.label,
        value: '¥' + d.value.toLocaleString(),
        percent: maxVal > 0 ? Math.round((d.value / maxVal) * 100) : 0
      }))
    }
  } catch (e) {
    console.error('获取营收趋势失败:', e)
  } finally {
    loading.value.revenue = false
  }
}

async function getHotDishes() {
  loading.value.dishes = true
  try {
    const { data } = await request.get('/dashboard/screen/hot-dishes')
    if (Array.isArray(data)) {
      const maxSales = data[0]?.sales || 1
      hotDishes.value = data.map(d => ({
        name: d.name,
        sales: d.sales,
        revenue: d.revenue?.toLocaleString() || '0',
        percent: Math.round((d.sales / maxSales) * 100)
      }))
    }
  } catch (e) {
    console.error('获取热销菜品失败:', e)
  } finally {
    loading.value.dishes = false
  }
}

async function getCustomerAnalysis() {
  loading.value.customer = true
  try {
    const { data } = await request.get('/dashboard/screen/customer-analysis')
    customerAnalysis.value = {
      repurchaseRate: data.repurchaseRate || 0,
      memberRatio: data.memberRatio || 0,
      newMembers: data.newMembers || 0,
      peakHour: data.peakHour || '--:--'
    }
  } catch (e) {
    console.error('获取客户分析失败:', e)
  } finally {
    loading.value.customer = false
  }
}

async function getCostAnalysis() {
  loading.value.cost = true
  try {
    const { data } = await request.get('/dashboard/screen/cost-analysis')
    if (Array.isArray(data)) {
      costAnalysis.value = data.map(item => ({
        label: item.label,
        value: item.value?.toLocaleString() || '0',
        percent: item.percent || 0,
        ratio: item.ratio || 0,
        color: item.color || '#4A7C59'
      }))
    }
  } catch (e) {
    console.error('获取成本分析失败:', e)
  } finally {
    loading.value.cost = false
  }
}

async function getAlerts() {
  loading.value.alerts = true
  try {
    const { data } = await request.get('/dashboard/screen/alerts')
    if (Array.isArray(data)) {
      alerts.value = data.map(a => ({
        title: a.title,
        meta: a.meta
      }))
    }
  } catch (e) {
    console.error('获取预警数据失败:', e)
  } finally {
    loading.value.alerts = false
  }
}

// Load all data
async function loadData() {
  await Promise.all([
    getScreenOverview(),
    getRevenueTrend(),
    getHotDishes(),
    getCustomerAnalysis(),
    getCostAnalysis(),
    getAlerts()
  ])
}

onMounted(() => {
  loadData()
  // Auto-refresh every 60 seconds
  refreshInterval = setInterval(loadData, 60000)
})

onUnmounted(() => {
  if (refreshInterval) {
    clearInterval(refreshInterval)
  }
})
</script>

<style scoped>
.data-screen-page {
  max-width: 1600px;
  margin: 0 auto;
}

.overview-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.overview-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
  text-align: center;
  position: relative;
  overflow: hidden;
}

.overview-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-accent));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-label {
  font-size: 13px;
  color: var(--color-text-muted);
  font-weight: 500;
}

.card-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 12px;
}

.card-trend.up {
  background: rgba(74, 124, 89, 0.08);
  color: #4A7C59;
}

.card-trend.down {
  background: rgba(194, 85, 85, 0.08);
  color: #C25555;
}

.card-value {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
  margin-bottom: 4px;
}

.card-sub {
  font-size: 12px;
  color: var(--color-text-muted);
}

.main-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
  margin-bottom: 24px;
}

.chart-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 24px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 20px;
}

.chart-placeholder {
  height: 200px;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.bar-chart {
  display: flex;
  align-items: flex-end;
  gap: 16px;
  height: 100%;
  width: 100%;
  padding-bottom: 8px;
}

.bar-group {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
}

.bar {
  width: 40px;
  background: linear-gradient(180deg, var(--color-primary) 0%, rgba(45, 74, 62, 0.3) 100%);
  border-radius: 4px 4px 0 0;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 4px;
  transition: height 0.5s ease;
}

.bar-value {
  font-size: 10px;
  font-weight: 600;
  color: #fff;
  opacity: 0;
  transition: opacity 0.3s;
}

.bar-group:hover .bar-value {
  opacity: 1;
}

.bar-label {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 8px;
}

.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.rank-badge {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.rank-1 {
  background: #C4A35A;
  color: #fff;
}

.rank-2 {
  background: #95A5A6;
  color: #fff;
}

.rank-3 {
  background: #B8835B;
  color: #fff;
}

.rank-4, .rank-5 {
  background: rgba(45, 74, 62, 0.08);
  color: var(--color-text-secondary);
}

.dish-info {
  flex: 1;
}

.dish-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.dish-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.dish-percent {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100px;
}

.percent-bar {
  flex: 1;
  height: 6px;
  background: var(--color-border);
  border-radius: 3px;
  overflow: hidden;
}

.percent-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.percent-text {
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.bottom-section {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.customer-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.customer-item {
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.customer-icon {
  width: 48px;
  height: 48px;
  opacity: 0.6;
  margin-bottom: 12px;
}

.customer-content {
  flex: 1;
}

.customer-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.customer-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

.cost-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cost-item {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cost-header {
  display: flex;
  justify-content: space-between;
}

.cost-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.cost-value {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.cost-bar {
  height: 8px;
  background: var(--color-border);
  border-radius: 4px;
  overflow: hidden;
}

.cost-fill {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s ease;
}

.cost-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.alert-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.alert-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: rgba(194, 85, 85, 0.06);
  border-radius: var(--radius-sm);
  border-left: 3px solid #C25555;
}

.alert-icon {
  width: 36px;
  height: 36px;
  opacity: 0.7;
  flex-shrink: 0;
}

.alert-content {
  flex: 1;
}

.alert-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 2px;
}

.alert-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.alert-action {
  padding: 6px 12px;
  font-size: 12px;
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.alert-action:hover {
  background: rgba(45, 74, 62, 0.1);
}

@media (max-width: 1200px) {
  .overview-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .main-section {
    grid-template-columns: 1fr;
  }
  .bottom-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .overview-row {
    grid-template-columns: 1fr;
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
  .customer-grid {
    grid-template-columns: 1fr;
  }
}
</style>

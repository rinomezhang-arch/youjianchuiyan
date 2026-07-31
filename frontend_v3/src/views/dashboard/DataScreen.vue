<template>
  <div class="data-screen-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">数据大屏 · Data Screen</h2>
        <p class="page-desc">实时经营数据 · 销售排行 · 桌台状态 · 订单监控</p>
      </div>
      <div class="header-right">
        <el-button :type="autoRefresh ? 'primary' : 'default'" @click="toggleRefresh">
          {{ autoRefresh ? '停止刷新' : '自动刷新' }}
        </el-button>
        <el-button @click="refreshAll">手动刷新</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">今日营收</div>
        <div class="stat-value">¥168,000</div>
        <div class="stat-sub">较昨日 +12.5%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">今日订单</div>
        <div class="stat-value">286</div>
        <div class="stat-sub">堂食 218 · 外带 68</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">平均客单</div>
        <div class="stat-value">¥587</div>
        <div class="stat-sub">较昨日 +4.2%</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">翻台率</div>
        <div class="stat-value">3.6</div>
        <div class="stat-sub">高于行业均值</div>
      </div>
    </div>

    <div class="screen-grid">
      <!-- 营收概览 -->
      <div class="screen-card section-revenue">
        <div class="card-title">营收概览</div>
        <div class="revenue-grid">
          <div class="revenue-item">
            <div class="revenue-label">今日营收</div>
            <div class="revenue-value">¥168,000</div>
          </div>
          <div class="revenue-item">
            <div class="revenue-label">本周营收</div>
            <div class="revenue-value">¥968,000</div>
          </div>
          <div class="revenue-item">
            <div class="revenue-label">本月营收</div>
            <div class="revenue-value">¥3,860,000</div>
          </div>
          <div class="revenue-item">
            <div class="revenue-label">毛利率</div>
            <div class="revenue-value">68.5%</div>
          </div>
        </div>
      </div>

      <!-- 销售排行 -->
      <div class="screen-card section-ranking">
        <div class="card-title">销售排行 · Top 5</div>
        <div class="ranking-list">
          <div class="ranking-item" v-for="(item, index) in topDishes" :key="index">
            <div class="rank-no" :class="rankClass(index)">{{ index + 1 }}</div>
            <div class="dish-info">
              <div class="dish-name">{{ item.name }}</div>
              <div class="dish-meta">{{ item.sales }} 份 · ¥{{ item.revenue }}</div>
            </div>
            <div class="dish-bar-wrap">
              <div class="dish-bar">
                <div class="dish-bar-fill" :style="{ width: item.percent + '%' }"></div>
              </div>
              <span class="dish-percent">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 桌台状态 -->
      <div class="screen-card section-tables">
        <div class="card-title">桌台状态</div>
        <div class="tables-grid">
          <div class="table-status-item">
            <div class="status-dot dot-using"></div>
            <div class="status-info">
              <div class="status-count">{{ tableStatus.using }}</div>
              <div class="status-label">使用中</div>
            </div>
          </div>
          <div class="table-status-item">
            <div class="status-dot dot-idle"></div>
            <div class="status-info">
              <div class="status-count">{{ tableStatus.idle }}</div>
              <div class="status-label">空闲</div>
            </div>
          </div>
          <div class="table-status-item">
            <div class="status-dot dot-reserved"></div>
            <div class="status-info">
              <div class="status-count">{{ tableStatus.reserved }}</div>
              <div class="status-label">已预订</div>
            </div>
          </div>
          <div class="table-status-item">
            <div class="status-dot dot-cleaning"></div>
            <div class="status-info">
              <div class="status-count">{{ tableStatus.cleaning }}</div>
              <div class="status-label">清理中</div>
            </div>
          </div>
        </div>
        <div class="tables-summary">
          总桌数：{{ tableStatus.total }} · 使用率 {{ tableUsageRate }}%
        </div>
      </div>

      <!-- 实时订单 -->
      <div class="screen-card section-orders">
        <div class="card-title">实时订单</div>
        <el-table :data="recentOrders" stripe size="small" v-loading="loading" max-height="320">
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="table" label="桌台" width="100" />
          <el-table-column prop="amount" label="金额" width="100" align="right">
            <template #default="{ row }">¥{{ row.amount }}</template>
          </el-table-column>
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="orderStatusType(row.status)" size="small" effect="plain">
                {{ row.status }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="time" label="下单时间" min-width="100" />
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const loading = ref(false)
const autoRefresh = ref(false)
let refreshTimer = null

const topDishes = ref([])
const tableStatus = ref({ total: 0, using: 0, idle: 0, reserved: 0, cleaning: 0 })
const recentOrders = ref([])

const tableUsageRate = computed(() => {
  const total = tableStatus.value.total || 0
  if (!total) return 0
  return ((tableStatus.value.using / total) * 100).toFixed(1)
})

function rankClass(index) {
  return ['rank-1', 'rank-2', 'rank-3', 'rank-4', 'rank-5'][index] || 'rank-4'
}

function orderStatusType(status) {
  return { '进行中': 'success', '待接单': 'warning', '已完成': 'info', '已取消': 'danger' }[status] || 'info'
}

function refreshAll() {
  loading.value = true
  // 预留接口接入位：拉取 topDishes / tableStatus / recentOrders
  setTimeout(() => { loading.value = false }, 300)
}

function toggleRefresh() {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) {
    refreshTimer = setInterval(refreshAll, 15000)
  } else if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
}

onMounted(() => { refreshAll() })
onBeforeUnmount(() => {
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})
</script>

<style scoped>
.data-screen-page {
  padding: 24px 32px;
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 12px;
}

.header-left {
  display: flex;
  flex-direction: column;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0 0 6px 0;
  letter-spacing: 0.5px;
}

.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin: 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 4px;
  height: 100%;
  background: linear-gradient(180deg, #2D4A3E 0%, #C4A35A 100%);
}

.stat-card:hover {
  box-shadow: 0 8px 24px rgba(45, 74, 62, 0.1);
  transform: translateY(-2px);
}

.stat-label {
  font-size: 13px;
  color: #95A5A6;
  margin-bottom: 8px;
  font-weight: 500;
  letter-spacing: 0.5px;
}

.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  letter-spacing: -0.3px;
}

.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 6px;
}

.screen-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.screen-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 24px;
}

.section-orders {
  grid-column: 1 / -1;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a2f23;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #E8E4DE;
  letter-spacing: 0.5px;
}

/* 营收概览 */
.revenue-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.revenue-item {
  background: #FAF8F5;
  border-radius: 8px;
  padding: 16px;
}

.revenue-label {
  font-size: 12px;
  color: #95A5A6;
  margin-bottom: 6px;
  font-weight: 500;
}

.revenue-value {
  font-size: 22px;
  font-weight: 700;
  color: #2D4A3E;
  line-height: 1.2;
}

/* 销售排行 */
.ranking-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.ranking-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #FAF8F5;
  border-radius: 8px;
}

.rank-no {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.rank-1 { background: #C4A35A; color: #FFFFFF; }
.rank-2 { background: #95A5A6; color: #FFFFFF; }
.rank-3 { background: #B8835B; color: #FFFFFF; }
.rank-4, .rank-5 { background: rgba(45, 74, 62, 0.08); color: #5D6D7E; }

.dish-info {
  flex: 1;
  min-width: 0;
}

.dish-name {
  font-size: 14px;
  font-weight: 600;
  color: #1a2f23;
  margin-bottom: 2px;
}

.dish-meta {
  font-size: 11px;
  color: #95A5A6;
}

.dish-bar-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 140px;
}

.dish-bar {
  flex: 1;
  height: 6px;
  background: #E8E4DE;
  border-radius: 3px;
  overflow: hidden;
}

.dish-bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #2D4A3E 0%, #C4A35A 100%);
  border-radius: 3px;
  transition: width 0.5s ease;
}

.dish-percent {
  font-size: 11px;
  font-weight: 600;
  color: #5D6D7E;
  min-width: 32px;
  text-align: right;
}

/* 桌台状态 */
.tables-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.table-status-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: #FAF8F5;
  border-radius: 8px;
}

.status-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  flex-shrink: 0;
}

.dot-using { background: #4A7C59; }
.dot-idle { background: #95A5A6; }
.dot-reserved { background: #C4A35A; }
.dot-cleaning { background: #5B7B8A; }

.status-info {
  display: flex;
  flex-direction: column;
}

.status-count {
  font-size: 20px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
}

.status-label {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 2px;
}

.tables-summary {
  font-size: 12px;
  color: #5D6D7E;
  padding-top: 12px;
  border-top: 1px solid #E8E4DE;
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .screen-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .revenue-grid,
  .tables-grid {
    grid-template-columns: 1fr;
  }
  .page-header {
    flex-direction: column;
    align-items: stretch;
  }
}
</style>

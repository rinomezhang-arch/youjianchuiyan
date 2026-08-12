<template>
  <div class="finance-page">
    <div class="page-header">
      <h2 class="page-title">财务数据总览</h2>
      <p class="page-subtitle">Finance & Data Analytics</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">今日总营收</div>
          <div class="stat-value">¥16,800</div>
          <div class="stat-sub">较昨日 +12%</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">线上营收</div>
          <div class="stat-value">¥8,200</div>
          <div class="stat-sub">占比 48.8%</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#C4A35A' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">线下营收</div>
          <div class="stat-value">¥8,600</div>
          <div class="stat-sub">占比 51.2%</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#5B7B8A' }">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">毛利</div>
          <div class="stat-value">¥11,500</div>
          <div class="stat-sub">毛利率 68.5%</div>
        </div>
      </div>
    </div>

    <div class="quick-actions-card">
      <h3 class="section-title">快捷功能</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('finance/cost-analysis')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <line x1="12" y1="1" x2="12" y2="23"/>
              <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
            </svg>
          </div>
          <span class="action-text">营收核算</span>
        </div>
        <div class="action-card" @click="goTo('finance')">
          <div class="action-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <rect x="3" y="3" width="18" height="18" rx="2"/>
              <path d="M9 9h6"/>
              <path d="M9 15h6"/>
            </svg>
          </div>
          <span class="action-text">对账管理</span>
        </div>
        <div class="action-card" @click="goTo('finance')">
          <div class="action-icon" style="background: rgba(196,163,90,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
              <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="9" cy="7" r="4"/>
            </svg>
          </div>
          <span class="action-text">工资核算</span>
        </div>
        <div class="action-card" @click="goTo('finance/cost-analysis')">
          <div class="action-icon" style="background: rgba(91,123,138,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
              <path d="M18 20V10"/>
              <path d="M12 20V4"/>
              <path d="M6 20v-6"/>
            </svg>
          </div>
          <span class="action-text">成本报表</span>
        </div>
        <div class="action-card" @click="goTo('finance')">
          <div class="action-icon" style="background: rgba(45,74,62,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/>
            </svg>
          </div>
          <span class="action-text">资金流水</span>
        </div>
        <div class="action-card" @click="goTo('finance')">
          <div class="action-icon" style="background: rgba(74,124,89,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
              <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10"/>
              <path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"/>
            </svg>
          </div>
          <span class="action-text">税务台账</span>
        </div>
        <div class="action-card" @click="goTo('finance')">
          <div class="action-icon" style="background: rgba(196,163,90,0.06)">
            <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
              <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
              <polyline points="22 4 12 14.01 9 11.01"/>
            </svg>
          </div>
          <span class="action-text">财务审核</span>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="chart-card">
        <h3 class="section-title">月度营收成本对比</h3>
        <div class="chart-placeholder">
          <div class="dual-chart">
            <svg viewBox="0 0 500 200" class="dual-svg">
              <defs>
                <linearGradient id="revenueGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" style="stop-color:#2D4A3E;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#2D4A3E;stop-opacity:0" />
                </linearGradient>
                <linearGradient id="costGradient" x1="0%" y1="0%" x2="0%" y2="100%">
                  <stop offset="0%" style="stop-color:#C4A35A;stop-opacity:0.3" />
                  <stop offset="100%" style="stop-color:#C4A35A;stop-opacity:0" />
                </linearGradient>
              </defs>
              <path :d="revenueAreaPath" fill="url(#revenueGradient)" />
              <path :d="revenueLinePath" fill="none" stroke="#2D4A3E" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              <circle v-for="(point, index) in revenuePoints" :key="'r-'+index" :cx="point.x" :cy="point.y" r="4" fill="#2D4A3E" />
              <path :d="costAreaPath" fill="url(#costGradient)" />
              <path :d="costLinePath" fill="none" stroke="#C4A35A" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              <circle v-for="(point, index) in costPoints" :key="'c-'+index" :cx="point.x" :cy="point.y" r="4" fill="#C4A35A" />
            </svg>
            <div class="chart-labels">
              <span v-for="month in months" :key="month">{{ month }}</span>
            </div>
            <div class="chart-legend">
              <div class="legend-item">
                <span class="legend-dot" style="background: #2D4A3E"></span>
                <span>营收</span>
              </div>
              <div class="legend-item">
                <span class="legend-dot" style="background: #C4A35A"></span>
                <span>成本</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-section">
        <div class="docs-card">
          <h3 class="section-title">待对账单据</h3>
          <div class="doc-tabs">
            <button class="tab-btn active">收银对账</button>
            <button class="tab-btn">供应商结款</button>
            <button class="tab-btn">宴会定金</button>
          </div>
          <div class="doc-list">
            <div class="doc-item" v-for="(doc, index) in pendingDocs" :key="index">
              <div class="doc-icon" :class="doc.type">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
                  <polyline points="14 2 14 8 20 8"/>
                </svg>
              </div>
              <div class="doc-content">
                <div class="doc-title">{{ doc.title }}</div>
                <div class="doc-meta">{{ doc.date }} · {{ doc.amount }}</div>
              </div>
              <div class="doc-status">{{ doc.status }}</div>
            </div>
          </div>
        </div>

        <div class="balance-card">
          <h3 class="section-title">资金状况</h3>
          <div class="balance-grid">
            <div class="balance-item">
              <div class="balance-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
                  <line x1="12" y1="1" x2="12" y2="23"/>
                  <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
                </svg>
              </div>
              <div class="balance-content">
                <div class="balance-value">¥128,600</div>
                <div class="balance-label">资金余额</div>
              </div>
            </div>
            <div class="balance-item">
              <div class="balance-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
                  <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
                </svg>
              </div>
              <div class="balance-content">
                <div class="balance-value">¥45,800</div>
                <div class="balance-label">应收账款</div>
              </div>
            </div>
            <div class="balance-item">
              <div class="balance-icon">
                <svg viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2">
                  <path d="M21 11.5a8.38 8.38 0 0 1-.9 3.8 8.5 8.5 0 0 1-7.6 4.7 8.38 8.38 0 0 1-3.8-.9L3 21l1.9-5.7a8.38 8.38 0 0 1-.9-3.8 8.5 8.5 0 0 1 4.7-7.6 8.38 8.38 0 0 1 3.8-.9h.5a8.48 8.48 0 0 1 8 8v.5z"/>
                </svg>
              </div>
              <div class="balance-content">
                <div class="balance-value">¥28,500</div>
                <div class="balance-label">应付账款</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

const months = ['1月', '2月', '3月', '4月', '5月', '6月']
const revenueData = [85, 92, 88, 98, 105, 112]
const costData = [42, 45, 44, 48, 52, 55]

const maxRevenue = Math.max(...revenueData)
const maxCost = Math.max(...costData)
const maxValue = Math.max(maxRevenue, maxCost)

const revenuePoints = computed(() => {
  return revenueData.map((value, index) => ({
    x: 60 + (index * 70),
    y: 170 - (value / maxValue) * 140
  }))
})

const costPoints = computed(() => {
  return costData.map((value, index) => ({
    x: 60 + (index * 70),
    y: 170 - (value / maxValue) * 140
  }))
})

const revenueLinePath = computed(() => {
  if (revenuePoints.value.length === 0) return ''
  return revenuePoints.value.map((point, index) => 
    `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`
  ).join(' ')
})

const costLinePath = computed(() => {
  if (costPoints.value.length === 0) return ''
  return costPoints.value.map((point, index) => 
    `${index === 0 ? 'M' : 'L'} ${point.x} ${point.y}`
  ).join(' ')
})

const revenueAreaPath = computed(() => {
  if (revenuePoints.value.length === 0) return ''
  const points = revenuePoints.value
  const startX = points[0].x
  const endX = points[points.length - 1].x
  return `${revenueLinePath.value} L ${endX} 170 L ${startX} 170 Z`
})

const costAreaPath = computed(() => {
  if (costPoints.value.length === 0) return ''
  const points = costPoints.value
  const startX = points[0].x
  const endX = points[points.length - 1].x
  return `${costLinePath.value} L ${endX} 170 L ${startX} 170 Z`
})

const pendingDocs = [
  { title: '收银对账 - 06-15', date: '2026-06-15', amount: '¥16,800', type: 'cashier', status: '待审核' },
  { title: '供应商结款 - 鑫源食品', date: '2026-06-15', amount: '¥8,500', type: 'supplier', status: '待支付' },
  { title: '宴会定金结算 - 牡丹厅', date: '2026-06-14', amount: '¥2,000', type: 'banquet', status: '待确认' },
  { title: '收银对账 - 06-14', date: '2026-06-14', amount: '¥14,200', type: 'cashier', status: '已完成' }
]

function goTo(path) {
  router.push(`/dashboard/${path}`)
}
</script>

<style scoped>
.finance-page {
  max-width: 1400px;
  margin: 0 auto;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: currentColor;
  opacity: 0.03;
  border-radius: 0 0 0 80px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  opacity: 0.7;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text);
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.quick-actions-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(7, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: var(--transition);
}

.action-card:hover {
  background: var(--color-bg-side);
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.action-icon svg {
  width: 22px;
  height: 22px;
}

.action-text {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
}

.bottom-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.chart-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.chart-placeholder {
  height: 220px;
}

.dual-chart {
  height: 100%;
  position: relative;
}

.dual-svg {
  width: 100%;
  height: 180px;
}

.chart-labels {
  display: flex;
  justify-content: space-between;
  padding: 0 30px;
  margin-top: 8px;
}

.chart-labels span {
  font-size: 11px;
  color: var(--color-text-muted);
}

.chart-legend {
  display: flex;
  justify-content: center;
  gap: 24px;
  margin-top: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--color-text-secondary);
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.right-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.docs-card, .balance-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px;
}

.doc-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border-light);
  padding-bottom: 8px;
}

.tab-btn {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-muted);
  background: transparent;
  border: none;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: var(--transition);
}

.tab-btn.active {
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
}

.doc-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.doc-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
}

.doc-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.doc-icon.cashier {
  background: rgba(45, 74, 62, 0.06);
  color: #2D4A3E;
}

.doc-icon.supplier {
  background: rgba(74, 124, 89, 0.06);
  color: #4A7C59;
}

.doc-icon.banquet {
  background: rgba(196, 163, 90, 0.06);
  color: #C4A35A;
}

.doc-icon svg {
  width: 18px;
  height: 18px;
}

.doc-content {
  flex: 1;
}

.doc-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text);
  margin-bottom: 2px;
}

.doc-meta {
  font-size: 11px;
  color: var(--color-text-muted);
}

.doc-status {
  font-size: 12px;
  font-weight: 500;
  color: #C25555;
  padding: 2px 8px;
  background: rgba(194, 85, 85, 0.08);
  border-radius: 4px;
}

.balance-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.balance-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-md);
}

.balance-icon {
  width: 40px;
  height: 40px;
  opacity: 0.6;
}

.balance-content {
  flex: 1;
}

.balance-value {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 2px;
}

.balance-label {
  font-size: 12px;
  color: var(--color-text-muted);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
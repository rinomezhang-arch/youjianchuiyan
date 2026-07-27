<template>
  <div class="tax-dashboard">
    <!-- 顶部标题 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">票据税务台账</h1>
        <p class="page-subtitle">Tax Management · 票据管理、税务申报、资金流水明细</p>
      </div>
      <div class="header-right">
        <div class="date-range">
          <span class="range-btn" :class="{ active: dateRange === 'month' }" @click="dateRange = 'month'">本月</span>
          <span class="range-btn" :class="{ active: dateRange === 'quarter' }" @click="dateRange = 'quarter'">本季度</span>
          <span class="range-btn" :class="{ active: dateRange === 'year' }" @click="dateRange = 'year'">本年</span>
        </div>
        <div class="store-selector">
          <select v-model="selectedStore" class="selector-dropdown">
            <option value="all">全部门店</option>
            <option value="ningguo">宁国店</option>
            <option value="xuancheng">宣城店</option>
            <option value="hangzhou">杭州店</option>
          </select>
        </div>
        <button class="add-btn" @click="openAddModal">新增票据</button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-section">
      <div class="stat-card invoice">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
            <polyline points="10 9 9 9 8 9"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.totalInvoice }}</div>
          <div class="stat-label">票据总数</div>
        </div>
        <div class="stat-trend up">+{{ stats.invoiceTrend }}%</div>
      </div>
      <div class="stat-card amount">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">¥{{ formatNumber(stats.totalAmount) }}</div>
          <div class="stat-label">票据总金额</div>
        </div>
        <div class="stat-trend up">+{{ stats.amountTrend }}%</div>
      </div>
      <div class="stat-card tax">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <circle cx="12" cy="12" r="6"/>
            <circle cx="12" cy="12" r="2"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">¥{{ formatNumber(stats.taxAmount) }}</div>
          <div class="stat-label">应交税额</div>
        </div>
        <div class="stat-trend" :class="stats.taxTrend > 0 ? 'down' : 'up'">
          {{ stats.taxTrend > 0 ? '+' : '' }}{{ stats.taxTrend }}%
        </div>
      </div>
      <div class="stat-card pending">
        <div class="stat-icon">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.pendingCount }}</div>
          <div class="stat-label">待处理</div>
        </div>
        <div class="stat-trend down">+{{ stats.pendingTrend }}%</div>
      </div>
    </div>

    <!-- 双栏：票据分类 + 税务申报进度 -->
    <div class="mid-section">
      <!-- 票据分类统计 -->
      <div class="invoice-category">
        <div class="section-header">
          <h2 class="section-title">票据分类统计</h2>
        </div>
        <div class="category-pie">
          <div class="pie-chart">
            <svg viewBox="0 0 200 200" class="pie-svg">
              <circle v-for="(item, i) in pieData" :key="i"
                cx="100" cy="100" r="80"
                :fill="item.color"
                :stroke="'#fff'"
                stroke-width="2"
                :transform="`rotate(${item.rotate} 100 100)`"
                :style="{ clipPath: item.clipPath }"
              />
            </svg>
            <div class="pie-center">
              <div class="pie-total">¥{{ formatNumber(stats.totalAmount) }}</div>
              <div class="pie-label">票据总额</div>
            </div>
          </div>
          <div class="category-legend">
            <div class="legend-item" v-for="item in pieData" :key="item.name">
              <span class="legend-color" :style="{ background: item.color }"></span>
              <span class="legend-text">{{ item.name }}</span>
              <span class="legend-value">¥{{ formatNumber(item.amount) }}</span>
              <span class="legend-percent">{{ item.percent }}%</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 税务申报进度 -->
      <div class="tax-progress">
        <div class="section-header">
          <h2 class="section-title">税务申报进度</h2>
        </div>
        <div class="progress-list">
          <div class="progress-item" v-for="item in taxProgress" :key="item.type">
            <div class="progress-header">
              <div class="progress-info">
                <div class="progress-name">{{ item.name }}</div>
                <div class="progress-desc">{{ item.desc }}</div>
              </div>
              <div class="progress-status" :class="item.status">
                {{ item.statusText }}
              </div>
            </div>
            <div class="progress-bar-wrap">
              <div class="progress-bar" :style="{ width: item.percent + '%', background: item.color }"></div>
            </div>
            <div class="progress-meta">
              <span>截止日期: {{ item.deadline }}</span>
              <span>已申报: ¥{{ formatNumber(item.amount) }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 底部：票据记录 + 资金流水 -->
    <div class="bottom-section">
      <!-- 票据记录列表 -->
      <div class="invoice-list">
        <div class="section-header">
          <h2 class="section-title">票据记录</h2>
          <div class="filter-tabs">
            <button :class="['filter-tab', { active: invoiceFilter === 'all' }]" @click="invoiceFilter = 'all'">全部</button>
            <button :class="['filter-tab', { active: invoiceFilter === 'income' }]" @click="invoiceFilter = 'income'">进项</button>
            <button :class="['filter-tab', { active: invoiceFilter === 'expense' }]" @click="invoiceFilter = 'expense'">销项</button>
          </div>
          <button class="export-btn">导出Excel</button>
        </div>
        <div class="list-table">
          <table>
            <thead>
              <tr>
                <th>票据类型</th>
                <th>票据编号</th>
                <th>金额</th>
                <th>税额</th>
                <th>开票日期</th>
                <th>开票单位</th>
                <th>门店</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in invoiceRecords" :key="item.id">
                <td><span class="type-tag" :class="item.type">{{ item.typeText }}</span></td>
                <td><span class="invoice-no">{{ item.no }}</span></td>
                <td class="amount">¥{{ formatNumber(item.amount) }}</td>
                <td class="tax">¥{{ formatNumber(item.tax) }}</td>
                <td>{{ item.date }}</td>
                <td>{{ item.company }}</td>
                <td>{{ item.store }}</td>
                <td><span class="status-tag" :class="item.status">{{ item.statusText }}</span></td>
                <td>
                  <button class="action-btn view" @click="viewDetail(item)">查看</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 资金流水 -->
      <div class="capital-flow">
        <div class="section-header">
          <h2 class="section-title">资金流水</h2>
          <div class="flow-tabs">
            <button :class="['flow-tab', { active: flowType === 'all' }]" @click="flowType = 'all'">全部</button>
            <button :class="['flow-tab', { active: flowType === 'income' }]" @click="flowType = 'income'">收入</button>
            <button :class="['flow-tab', { active: flowType === 'expense' }]" @click="flowType = 'expense'">支出</button>
          </div>
        </div>
        <div class="flow-summary">
          <div class="summary-item income">
            <div class="summary-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="19" x2="12" y2="5"/>
                <polyline points="5 12 12 5 19 12"/>
              </svg>
            </div>
            <div class="summary-info">
              <div class="summary-label">收入总额</div>
              <div class="summary-value">¥{{ formatNumber(flowStats.income) }}</div>
            </div>
          </div>
          <div class="summary-item expense">
            <div class="summary-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <polyline points="19 12 12 19 5 12"/>
              </svg>
            </div>
            <div class="summary-info">
              <div class="summary-label">支出总额</div>
              <div class="summary-value">¥{{ formatNumber(flowStats.expense) }}</div>
            </div>
          </div>
          <div class="summary-item balance">
            <div class="summary-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="3" x2="16" y2="13"/>
              </svg>
            </div>
            <div class="summary-info">
              <div class="summary-label">余额</div>
              <div class="summary-value">¥{{ formatNumber(flowStats.balance) }}</div>
            </div>
          </div>
        </div>
        <div class="flow-list">
          <div class="flow-item" v-for="item in capitalFlows" :key="item.id">
            <div class="flow-icon" :class="item.type">
              <svg v-if="item.type === 'income'" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="19" x2="12" y2="5"/>
                <polyline points="5 12 12 5 19 12"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"/>
                <polyline points="19 12 12 19 5 12"/>
              </svg>
            </div>
            <div class="flow-info">
              <div class="flow-name">{{ item.name }}</div>
              <div class="flow-desc">{{ item.store }} · {{ item.time }}</div>
            </div>
            <div class="flow-amount" :class="item.type">
              {{ item.type === 'income' ? '+' : '-' }}¥{{ formatNumber(item.amount) }}
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 发票开具统计 -->
    <div class="invoice-section">
      <div class="section-header">
        <h2 class="section-title">发票开具统计</h2>
      </div>
      <div class="invoice-grid">
        <div class="invoice-card" v-for="item in invoiceStats" :key="item.type">
          <div class="invoice-icon" :class="item.type">{{ item.icon }}</div>
          <div class="invoice-name">{{ item.name }}</div>
          <div class="invoice-num">{{ item.count }}张</div>
          <div class="invoice-amount">¥{{ formatNumber(item.amount) }}</div>
          <div class="invoice-trend" :class="item.trend > 0 ? 'up' : 'down'">
            {{ item.trend > 0 ? '+' : '' }}{{ item.trend }}%
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'

const dateRange = ref('month')
const selectedStore = ref('all')
const invoiceFilter = ref('all')
const flowType = ref('all')

function formatNumber(num) {
  return num.toLocaleString('zh-CN')
}

// 统计数据
const stats = ref({
  totalInvoice: 156,
  invoiceTrend: 8.5,
  totalAmount: 3860000,
  amountTrend: 12.3,
  taxAmount: 231600,
  taxTrend: 12.3,
  pendingCount: 8,
  pendingTrend: 0
})

// 票据分类饼图数据
const pieData = ref([
  { name: '餐饮服务', amount: 1800000, percent: 46.6, color: '#F56C6C', rotate: 0, clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' },
  { name: '食材采购', amount: 1200000, percent: 31.1, color: '#E6A23C', rotate: 168, clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' },
  { name: '水电燃气', amount: 460000, percent: 11.9, color: '#5B7B8A', rotate: 278, clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' },
  { name: '租金物业', amount: 250000, percent: 6.5, color: '#4A7C59', rotate: 320, clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' },
  { name: '其他', amount: 150000, percent: 3.9, color: '#8B9A8C', rotate: 345, clipPath: 'polygon(0 0, 100% 0, 100% 100%, 0 100%)' },
])

// 税务申报进度
const taxProgress = ref([
  { type: 'vat', name: '增值税', desc: '一般纳税人申报', deadline: '2026-07-15', amount: 156000, percent: 100, status: 'completed', statusText: '已完成', color: '#67C23A' },
  { type: 'income', name: '企业所得税', desc: '季度预缴申报', deadline: '2026-07-20', amount: 45000, percent: 80, status: 'processing', statusText: '申报中', color: '#E6A23C' },
  { type: 'personal', name: '个人所得税', desc: '员工个税申报', deadline: '2026-07-15', amount: 30600, percent: 100, status: 'completed', statusText: '已完成', color: '#67C23A' },
  { type: 'other', name: '城建税及附加', desc: '附加税费申报', deadline: '2026-07-15', amount: 0, percent: 0, status: 'pending', statusText: '待申报', color: '#F56C6C' },
])

// 票据记录
const invoiceRecords = ref([
  { id: 1, type: 'income', typeText: '进项', no: 'FP202607001', amount: 58000, tax: 7540, date: '2026-07-25', company: '宁国市鑫源食品有限公司', store: '宁国店', status: 'verified', statusText: '已验证' },
  { id: 2, type: 'expense', typeText: '销项', no: 'FP202607002', amount: 126000, tax: 16380, date: '2026-07-25', company: '宁国市机关事务管理局', store: '宁国店', status: 'issued', statusText: '已开具' },
  { id: 3, type: 'income', typeText: '进项', no: 'FP202607003', amount: 42000, tax: 5460, date: '2026-07-24', company: '宣城市新鲜蔬菜配送中心', store: '宣城店', status: 'verified', statusText: '已验证' },
  { id: 4, type: 'expense', typeText: '销项', no: 'FP202607004', amount: 89000, tax: 11570, date: '2026-07-24', company: '杭州某科技有限公司', store: '杭州店', status: 'issued', statusText: '已开具' },
  { id: 5, type: 'income', typeText: '进项', no: 'FP202607005', amount: 35000, tax: 4550, date: '2026-07-23', company: '宁国市液化气供应站', store: '宁国店', status: 'pending', statusText: '待验证' },
])

// 资金流水统计
const flowStats = ref({
  income: 2350000,
  expense: 1510000,
  balance: 840000
})

// 资金流水记录
const capitalFlows = ref([
  { id: 1, type: 'income', name: '宁国店营业款', store: '宁国店', time: '2026-07-27 18:30', amount: 86000 },
  { id: 2, type: 'income', name: '宣城店营业款', store: '宣城店', time: '2026-07-27 18:00', amount: 62000 },
  { id: 3, type: 'expense', name: '食材采购付款', store: '宁国店', time: '2026-07-27 14:00', amount: 45000 },
  { id: 4, type: 'income', name: '杭州店营业款', store: '杭州店', time: '2026-07-27 17:30', amount: 78000 },
  { id: 5, type: 'expense', name: '供应商货款', store: '宣城店', time: '2026-07-27 10:00', amount: 32000 },
  { id: 6, type: 'expense', name: '水电费', store: '宁国店', time: '2026-07-27 09:00', amount: 18000 },
])

// 发票开具统计
const invoiceStats = ref([
  { type: 'special', name: '增值税专票', count: 45, amount: 1280000, trend: 15.2, icon: '专' },
  { type: 'general', name: '增值税普票', count: 86, amount: 1860000, trend: 8.5, icon: '普' },
  { type: 'electronic', name: '电子发票', count: 25, amount: 720000, trend: 25.8, icon: '电' },
])

function openAddModal() {
  ElMessage.info('新增票据功能')
}

function viewDetail(item) {
  ElMessage.info(`查看票据 ${item.no} 详情`)
}
</script>

<style scoped>
.tax-dashboard {
  max-width: 1600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.page-subtitle {
  font-size: 14px;
  color: #7a8c84;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.date-range {
  display: flex;
  background: #f5f7f6;
  border-radius: 8px;
  padding: 4px;
}

.range-btn {
  padding: 8px 20px;
  font-size: 13px;
  color: #7a8c84;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s;
}

.range-btn.active {
  background: #fff;
  color: #2D4A3E;
  font-weight: 600;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.selector-dropdown {
  padding: 8px 16px;
  border: 1px solid #e8edea;
  border-radius: 8px;
  font-size: 14px;
  background: #fff;
}

.add-btn {
  padding: 8px 16px;
  font-size: 14px;
  color: #fff;
  background: #2D4A3E;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

/* 统计卡片 */
.stats-section {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
  display: flex;
  align-items: center;
  gap: 16px;
}

.stat-card.invoice {
  border-left: 4px solid #F56C6C;
}

.stat-card.amount {
  border-left: 4px solid #E6A23C;
}

.stat-card.tax {
  border-left: 4px solid #5B7B8A;
}

.stat-card.pending {
  border-left: 4px solid #4A7C59;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.stat-card.invoice .stat-icon {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.stat-card.amount .stat-icon {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.stat-card.tax .stat-icon {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.stat-card.pending .stat-icon {
  background: rgba(74,124,89,0.1);
  color: #4A7C59;
}

.stat-icon svg {
  width: 24px;
  height: 24px;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.stat-label {
  font-size: 13px;
  color: #999;
}

.stat-trend {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.stat-trend.up {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.stat-trend.down {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

/* 中间区域 */
.mid-section {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

/* 票据分类 */
.invoice-category {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.category-pie {
  display: flex;
  align-items: center;
  gap: 32px;
}

.pie-chart {
  position: relative;
  width: 200px;
  height: 200px;
}

.pie-svg {
  transform: rotate(-90deg);
}

.pie-center {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  border-radius: 50%;
  width: 100px;
  height: 100px;
  margin: auto;
}

.pie-total {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
}

.pie-label {
  font-size: 12px;
  color: #999;
}

.category-legend {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.legend-color {
  width: 16px;
  height: 16px;
  border-radius: 4px;
}

.legend-text {
  flex: 1;
  font-size: 14px;
  color: #2D4A3E;
}

.legend-value {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  min-width: 100px;
}

.legend-percent {
  font-size: 14px;
  font-weight: 600;
  color: #7a8c84;
}

/* 税务申报进度 */
.tax-progress {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.progress-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.progress-item {
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
}

.progress-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.progress-info {
  flex: 1;
}

.progress-name {
  font-size: 15px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.progress-desc {
  font-size: 13px;
  color: #999;
}

.progress-status {
  padding: 4px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.progress-status.completed {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.progress-status.processing {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.progress-status.pending {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.progress-bar-wrap {
  height: 8px;
  background: #e8edea;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 8px;
}

.progress-bar {
  height: 100%;
  border-radius: 4px;
  transition: width 0.5s;
}

.progress-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}

/* 底部区域 */
.bottom-section {
  display: grid;
  grid-template-columns: 1.3fr 1fr;
  gap: 24px;
  margin-bottom: 24px;
}

/* 票据记录 */
.invoice-list {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.filter-tabs {
  display: flex;
  gap: 8px;
}

.filter-tab {
  padding: 6px 14px;
  font-size: 13px;
  color: #7a8c84;
  background: #f5f7f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.filter-tab.active {
  background: #2D4A3E;
  color: #fff;
}

.export-btn {
  padding: 6px 14px;
  font-size: 13px;
  color: #2D4A3E;
  background: rgba(45,74,62,0.06);
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.list-table {
  overflow-x: auto;
}

.list-table table {
  width: 100%;
  border-collapse: collapse;
}

.list-table th,
.list-table td {
  padding: 12px;
  text-align: left;
  font-size: 14px;
}

.list-table th {
  background: #fafbfb;
  color: #7a8c84;
  font-weight: 600;
}

.list-table tbody tr {
  border-bottom: 1px solid #f0f2f1;
}

.list-table tbody tr:hover {
  background: #fafbfb;
}

.amount {
  font-weight: 700;
  color: #2D4A3E;
}

.tax {
  font-weight: 600;
  color: #5B7B8A;
}

.type-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.type-tag.income {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.type-tag.expense {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.invoice-no {
  font-family: monospace;
  font-size: 13px;
  color: #7a8c84;
}

.status-tag {
  padding: 4px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
}

.status-tag.verified {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.status-tag.issued {
  background: rgba(74,124,89,0.1);
  color: #4A7C59;
}

.status-tag.pending {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.action-btn {
  padding: 6px 12px;
  font-size: 12px;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.action-btn.view {
  background: #f5f7f6;
  color: #7a8c84;
}

/* 资金流水 */
.capital-flow {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.flow-tabs {
  display: flex;
  gap: 8px;
}

.flow-tab {
  padding: 6px 14px;
  font-size: 13px;
  color: #7a8c84;
  background: #f5f7f6;
  border: none;
  border-radius: 6px;
  cursor: pointer;
}

.flow-tab.active {
  background: #2D4A3E;
  color: #fff;
}

.flow-summary {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.summary-item {
  flex: 1;
  background: #fafbfb;
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
}

.summary-item.income {
  border-left: 4px solid #67C23A;
}

.summary-item.expense {
  border-left: 4px solid #F56C6C;
}

.summary-item.balance {
  border-left: 4px solid #5B7B8A;
}

.summary-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.summary-item.income .summary-icon {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.summary-item.expense .summary-icon {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.summary-item.balance .summary-icon {
  background: rgba(91,123,138,0.1);
  color: #5B7B8A;
}

.summary-icon svg {
  width: 20px;
  height: 20px;
}

.summary-info {
  flex: 1;
}

.summary-label {
  font-size: 12px;
  color: #999;
  margin-bottom: 4px;
}

.summary-value {
  font-size: 18px;
  font-weight: 700;
  color: #2D4A3E;
}

.flow-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.flow-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: #fafbfb;
  border-radius: 10px;
}

.flow-icon {
  width: 36px;
  height: 36px;
  border-radius: 9px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.flow-icon.income {
  background: rgba(103,194,58,0.1);
  color: #67C23A;
}

.flow-icon.expense {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.flow-icon svg {
  width: 18px;
  height: 18px;
}

.flow-info {
  flex: 1;
}

.flow-name {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
  margin-bottom: 2px;
}

.flow-desc {
  font-size: 12px;
  color: #999;
}

.flow-amount {
  font-size: 15px;
  font-weight: 700;
}

.flow-amount.income {
  color: #67C23A;
}

.flow-amount.expense {
  color: #F56C6C;
}

/* 发票开具统计 */
.invoice-section {
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  border: 1px solid #e8edea;
}

.invoice-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.invoice-card {
  background: #fafbfb;
  border-radius: 12px;
  padding: 20px;
  text-align: center;
}

.invoice-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  margin: 0 auto 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.invoice-icon.special {
  background: rgba(245,108,108,0.1);
  color: #F56C6C;
}

.invoice-icon.general {
  background: rgba(230,162,60,0.1);
  color: #E6A23C;
}

.invoice-icon.electronic {
  background: rgba(74,124,89,0.1);
  color: #4A7C59;
}

.invoice-name {
  font-size: 14px;
  color: #7a8c84;
  margin-bottom: 8px;
}

.invoice-num {
  font-size: 24px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 4px;
}

.invoice-amount {
  font-size: 14px;
  color: #7a8c84;
  margin-bottom: 8px;
}

.invoice-trend {
  font-size: 12px;
  font-weight: 600;
}

.invoice-trend.up {
  color: #67C23A;
}

.invoice-trend.down {
  color: #F56C6C;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stats-section {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 1200px) {
  .mid-section, .bottom-section {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .stats-section {
    grid-template-columns: 1fr;
  }
  .invoice-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
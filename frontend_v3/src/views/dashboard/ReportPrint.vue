<template>
  <div class="report-print-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">报表打印 · Report & Print</h2>
        <p class="page-subtitle">Generate, preview and print booking reports</p>
      </div>
    </div>

    <!-- 报表类型选择 -->
    <div class="report-type-grid">
      <div v-for="rt in reportTypes" :key="rt.id" class="report-type-card" :class="{ active: selectedType === rt.id }" @click="selectedType = rt.id">
        <div class="report-type-icon" :style="{ background: rt.color + '15' }">
          <svg viewBox="0 0 24 24" fill="none" :stroke="rt.color" stroke-width="2">
            <g v-if="rt.id === 'daily'">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/><polyline points="10 9 9 9 8 9"/>
            </g>
            <g v-else-if="rt.id === 'weekly'">
              <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"/><polyline points="3.27 6.96 12 12.01 20.73 6.96"/><line x1="12" y1="22.08" x2="12" y2="12"/>
            </g>
            <g v-else-if="rt.id === 'monthly'">
              <path d="M12 20V10"/><path d="M18 20V4"/><path d="M6 20v-4"/>
            </g>
            <g v-else>
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="17 8 12 3 7 8"/><line x1="12" y1="3" x2="12" y2="15"/>
            </g>
          </svg>
        </div>
        <div class="report-type-name">{{ rt.name }}</div>
        <div class="report-type-desc">{{ rt.desc }}</div>
      </div>
    </div>

    <!-- 报表参数设置 -->
    <div class="report-config-card">
      <h3 class="section-title">报表参数 · Report Parameters</h3>
      <div class="config-grid">
        <div class="config-item">
          <label>日期范围</label>
          <div class="date-range">
            <input type="date" v-model="config.dateFrom" />
            <span>至</span>
            <input type="date" v-model="config.dateTo" />
          </div>
        </div>
        <div class="config-item">
          <label>桌台区域</label>
          <select v-model="config.zone">
            <option value="">全部区域</option>
            <option value="hall">大厅散台</option>
            <option value="private">包厢</option>
            <option value="vip">VIP包房</option>
          </select>
        </div>
        <div class="config-item">
          <label>时段筛选</label>
          <select v-model="config.timeSlot">
            <option value="">全部时段</option>
            <option value="lunch">午餐</option>
            <option value="dinner">晚餐</option>
            <option value="late">夜宵</option>
          </select>
        </div>
        <div class="config-item">
          <label>预订状态</label>
          <select v-model="config.status">
            <option value="">全部状态</option>
            <option value="completed">已完成</option>
            <option value="cancelled">已取消</option>
            <option value="all">含进行中</option>
          </select>
        </div>
        <div class="config-item">
          <label>客人来源</label>
          <select v-model="config.source">
            <option value="">全部来源</option>
            <option value="walk-in">自来客</option>
            <option value="phone">电话</option>
            <option value="online">线上</option>
            <option value="member">会员</option>
          </select>
        </div>
        <div class="config-item">
          <label>接待员工</label>
          <select v-model="config.staff">
            <option value="">全部员工</option>
            <option v-for="s in staffList" :key="s.id" :value="s.id">{{ s.name }}</option>
          </select>
        </div>
        <div class="config-item">
          <label>消费金额范围</label>
          <div class="range-input">
            <input type="number" v-model.number="config.amountMin" placeholder="最低" />
            <span>至</span>
            <input type="number" v-model.number="config.amountMax" placeholder="最高" />
          </div>
        </div>
        <div class="config-item">
          <label>排序方式</label>
          <select v-model="config.sortBy">
            <option value="time">按时间</option>
            <option value="amount">按金额</option>
            <option value="pax">按人数</option>
            <option value="table">按桌台</option>
          </select>
        </div>
      </div>
      <div class="config-actions">
        <button class="btn-secondary" @click="resetConfig">重置参数</button>
        <button class="btn-primary" @click="generateReport">生成报表</button>
      </div>
    </div>

    <!-- 报表预览 -->
    <div v-if="reportGenerated" class="report-preview-card">
      <div class="preview-header">
        <div class="preview-title">
          <h3>{{ currentReportType.name }} · 报表预览</h3>
          <p>{{ config.dateFrom }} 至 {{ config.dateTo }}</p>
        </div>
        <div class="preview-actions">
          <button class="btn-sm" @click="printReport">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/>
            </svg>
            打印
          </button>
          <button class="btn-sm" @click="exportPDF">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
            </svg>
            导出PDF
          </button>
          <button class="btn-sm" @click="exportExcel">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            导出Excel
          </button>
        </div>
      </div>

      <!-- 报表摘要 -->
      <div class="report-summary">
        <div class="summary-item">
          <span class="summary-label">总预订数</span>
          <span class="summary-value">{{ reportSummary.totalBookings }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">总营收</span>
          <span class="summary-value money">¥{{ reportSummary.totalRevenue.toLocaleString() }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">平均消费</span>
          <span class="summary-value">¥{{ reportSummary.avgAmount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">总人数</span>
          <span class="summary-value">{{ reportSummary.totalPax }}人</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">取消数</span>
          <span class="summary-value cancel">{{ reportSummary.cancelled }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">完成率</span>
          <span class="summary-value">{{ reportSummary.completionRate }}%</span>
        </div>
      </div>

      <!-- 报表图表 -->
      <div class="report-charts">
        <div class="report-chart-item">
          <h4 class="chart-subtitle">每日预订量趋势</h4>
          <div class="mini-bar-chart">
            <div v-for="d in dailyBookingTrend" :key="d.date" class="mini-bar-group">
              <div class="mini-bar" :style="{ height: (d.count / maxDailyBooking * 100) + '%', background: d.cancelled > 0 ? '#C0392B' : '#2D4A3E' }" :title="`${d.date}: ${d.count}单${d.cancelled ? ' (取消' + d.cancelled + ')' : ''}`"></div>
              <span class="mini-bar-label">{{ d.date.slice(5) }}</span>
            </div>
          </div>
        </div>
        <div class="report-chart-item">
          <h4 class="chart-subtitle">营收构成</h4>
          <div class="revenue-breakdown">
            <div v-for="item in revenueBreakdown" :key="item.label" class="breakdown-row">
              <span class="breakdown-label">{{ item.label }}</span>
              <div class="breakdown-bar-track">
                <div class="breakdown-bar-fill" :style="{ width: item.percent + '%', background: item.color }"></div>
              </div>
              <span class="breakdown-value">¥{{ item.amount.toLocaleString() }} ({{ item.percent }}%)</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 报表明细表 -->
      <div class="report-table-section">
        <h4 class="chart-subtitle">预订明细 · Booking Details</h4>
        <table class="report-table">
          <thead>
            <tr>
              <th>日期</th>
              <th>时间</th>
              <th>桌台</th>
              <th>客人</th>
              <th>人数</th>
              <th>来源</th>
              <th>接待人</th>
              <th>消费金额</th>
              <th>状态</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="b in reportBookings" :key="b.id">
              <td>{{ b.date }}</td>
              <td>{{ b.time }}</td>
              <td>{{ b.tableName }}</td>
              <td>{{ b.guestName }}</td>
              <td>{{ b.pax }}人</td>
              <td>{{ b.source }}</td>
              <td>{{ b.staff }}</td>
              <td class="money-cell">¥{{ b.amount.toLocaleString() }}</td>
              <td><span :class="['status-badge', b.status]">{{ statusText(b.status) }}</span></td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="total-row">
              <td colspan="7">合计 · Total</td>
              <td class="money-cell total-amount">¥{{ reportSummary.totalRevenue.toLocaleString() }}</td>
              <td>{{ reportBookings.length }}单</td>
            </tr>
          </tfoot>
        </table>
      </div>

      <!-- 页脚 -->
      <div class="report-footer">
        <span>报表生成时间：{{ new Date().toLocaleString('zh-CN') }}</span>
        <span>宁国店 · 前台预定部</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'

const selectedType = ref('daily')
const reportGenerated = ref(false)

const reportTypes = [
  { id: 'daily', name: '日报表', desc: '每日预订、营收、客流汇总', color: '#2D4A3E' },
  { id: 'weekly', name: '周报表', desc: '本周趋势对比与周汇总', color: '#4A7C59' },
  { id: 'monthly', name: '月报表', desc: '月度经营分析与同比环比', color: '#D4A853' },
  { id: 'custom', name: '自定义报表', desc: '按需选择维度和指标', color: '#5B7B8A' },
]

const currentReportType = computed(() => reportTypes.find(r => r.id === selectedType.value) || reportTypes[0])

const staffList = [
  { id: 1, name: '王芳' },
  { id: 2, name: '李强' },
  { id: 3, name: '张敏' },
  { id: 4, name: '刘洋' },
  { id: 5, name: '陈静' },
]

const config = ref({
  dateFrom: '2026-07-01', dateTo: '2026-07-09',
  zone: '', timeSlot: '', status: '', source: '', staff: '',
  amountMin: null, amountMax: null, sortBy: 'time',
})

const reportSummary = ref({
  totalBookings: 386,
  totalRevenue: 258600,
  avgAmount: 670,
  totalPax: 1820,
  cancelled: 28,
  completionRate: 92.7,
})

const dailyBookingTrend = ref([
  { date: '2026-07-01', count: 38, cancelled: 3 },
  { date: '2026-07-02', count: 42, cancelled: 2 },
  { date: '2026-07-03', count: 45, cancelled: 4 },
  { date: '2026-07-04', count: 40, cancelled: 3 },
  { date: '2026-07-05', count: 48, cancelled: 2 },
  { date: '2026-07-06', count: 52, cancelled: 5 },
  { date: '2026-07-07', count: 55, cancelled: 3 },
  { date: '2026-07-08', count: 35, cancelled: 4 },
  { date: '2026-07-09', count: 31, cancelled: 2 },
])

const maxDailyBooking = computed(() => Math.max(...dailyBookingTrend.value.map(d => d.count)))

const revenueBreakdown = ref([
  { label: '大厅散台', amount: 85000, percent: 33, color: '#2D4A3E' },
  { label: '普通包厢', amount: 92000, percent: 36, color: '#4A7C59' },
  { label: 'VIP包房', amount: 58000, percent: 22, color: '#D4A853' },
  { label: '宴会厅', amount: 23600, percent: 9, color: '#5B7B8A' },
])

const reportBookings = ref([
  { id: 1, date: '2026-07-09', time: '12:30', tableName: '牡丹厅', guestName: '张先生', pax: 8, source: '电话', staff: '王芳', amount: 2800, status: 'completed' },
  { id: 2, date: '2026-07-09', time: '18:00', tableName: '3号桌', guestName: '李女士', pax: 4, source: '自来', staff: '李强', amount: 680, status: 'completed' },
  { id: 3, date: '2026-07-09', time: '19:30', tableName: 'VIP-1', guestName: '王总', pax: 12, source: '会员', staff: '王芳', amount: 8500, status: 'completed' },
  { id: 4, date: '2026-07-09', time: '11:00', tableName: '5号桌', guestName: '赵先生', pax: 6, source: '线上', staff: '张敏', amount: 1200, status: 'completed' },
  { id: 5, date: '2026-07-09', time: '20:00', tableName: '荷花厅', guestName: '刘女士', pax: 10, source: '电话', staff: '李强', amount: 3500, status: 'completed' },
  { id: 6, date: '2026-07-08', time: '12:00', tableName: '2号桌', guestName: '孙伟', pax: 4, source: '自来', staff: '陈静', amount: 580, status: 'completed' },
  { id: 7, date: '2026-07-08', time: '18:30', tableName: '菊花厅', guestName: '周婷', pax: 8, source: '电话', staff: '王芳', amount: 2200, status: 'cancelled' },
  { id: 8, date: '2026-07-08', time: '19:00', tableName: 'VIP-2', guestName: '吴总', pax: 14, source: '企业', staff: '王芳', amount: 6200, status: 'completed' },
  { id: 9, date: '2026-07-07', time: '12:30', tableName: '6号桌', guestName: '郑先生', pax: 6, source: '线上', staff: '张敏', amount: 980, status: 'completed' },
  { id: 10, date: '2026-07-07', time: '18:00', tableName: '牡丹厅', guestName: '冯女士', pax: 10, source: '会员', staff: '李强', amount: 3800, status: 'completed' },
])

const statusText = (s) => ({ completed: '已完成', cancelled: '已取消', pending: '待确认', confirmed: '已确认' }[s] || s)

const resetConfig = () => {
  config.value = {
    dateFrom: '2026-07-01', dateTo: '2026-07-09',
    zone: '', timeSlot: '', status: '', source: '', staff: '',
    amountMin: null, amountMax: null, sortBy: 'time',
  }
}

const generateReport = () => {
  reportGenerated.value = true
}

const printReport = () => {
  window.print()
}

const exportPDF = () => {
  console.log('Exporting PDF...')
}

const exportExcel = () => {
  console.log('Exporting Excel...')
}
</script>

<style scoped>
.report-print-page { padding: 24px 32px; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }

.report-type-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.report-type-card {
  background: #fff; border-radius: 8px; padding: 20px;
  border: 2px solid #e8ece9; cursor: pointer; transition: all 0.2s;
  display: flex; flex-direction: column; align-items: center; text-align: center; gap: 8px;
}
.report-type-card:hover { border-color: #2D4A3E; transform: translateY(-2px); box-shadow: 0 4px 12px rgba(45,74,62,0.1); }
.report-type-card.active { border-color: #2D4A3E; background: rgba(45,74,62,0.03); }
.report-type-icon {
  width: 48px; height: 48px; border-radius: 12px;
  display: flex; align-items: center; justify-content: center;
}
.report-type-icon svg { width: 24px; height: 24px; }
.report-type-name { font-size: 14px; font-weight: 600; color: #1a2f23; }
.report-type-desc { font-size: 11px; color: #8a9a8e; }

.report-config-card { background: #fff; border-radius: 8px; padding: 20px; border: 1px solid #e8ece9; margin-bottom: 20px; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0 0 16px 0; }
.config-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 16px; }
.config-item { display: flex; flex-direction: column; gap: 6px; }
.config-item label { font-size: 12px; color: #6a7a6e; font-weight: 500; }
.config-item input, .config-item select {
  padding: 7px 10px; border: 1px solid #d0d8d2; border-radius: 6px;
  font-size: 13px; color: #3a4a3e; outline: none;
}
.config-item input:focus, .config-item select:focus { border-color: #2D4A3E; }
.date-range, .range-input { display: flex; align-items: center; gap: 6px; }
.date-range span, .range-input span { font-size: 12px; color: #8a9a8e; }
.config-actions { display: flex; justify-content: flex-end; gap: 10px; }
.btn-primary {
  background: #2D4A3E; color: #fff; border: none; padding: 8px 20px;
  border-radius: 6px; font-size: 13px; cursor: pointer; font-weight: 500;
}
.btn-primary:hover { background: #3a5f50; }
.btn-secondary { padding: 8px 20px; border-radius: 6px; font-size: 13px; cursor: pointer; border: 1px solid #d0d8d2; background: #fff; color: #6a7a6e; }

.report-preview-card { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; padding: 24px; }
.preview-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.preview-title h3 { font-size: 18px; font-weight: 700; color: #1a2f23; margin: 0; }
.preview-title p { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }
.preview-actions { display: flex; gap: 8px; }
.btn-sm {
  padding: 6px 14px; border-radius: 6px; font-size: 12px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #3a4a3e;
  display: flex; align-items: center; gap: 5px;
}
.btn-sm:hover { background: #f0f4f1; border-color: #2D4A3E; }

.report-summary { display: grid; grid-template-columns: repeat(6, 1fr); gap: 12px; margin-bottom: 24px; padding: 16px; background: #f8f9f8; border-radius: 8px; }
.summary-item { display: flex; flex-direction: column; align-items: center; gap: 4px; }
.summary-label { font-size: 11px; color: #8a9a8e; }
.summary-value { font-size: 18px; font-weight: 700; color: #1a2f23; }
.summary-value.money { color: #D4A853; }
.summary-value.cancel { color: #C0392B; }

.report-charts { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 24px; }
.chart-subtitle { font-size: 14px; font-weight: 600; color: #1a2f23; margin: 0 0 12px 0; }
.mini-bar-chart { display: flex; gap: 6px; align-items: flex-end; height: 120px; }
.mini-bar-group { flex: 1; display: flex; flex-direction: column; align-items: center; height: 100%; justify-content: flex-end; }
.mini-bar { width: 100%; max-width: 30px; border-radius: 3px 3px 0 0; min-height: 2px; }
.mini-bar-label { font-size: 9px; color: #8a9a8e; margin-top: 4px; }

.revenue-breakdown { display: flex; flex-direction: column; gap: 10px; }
.breakdown-row { display: flex; align-items: center; gap: 10px; }
.breakdown-label { width: 70px; font-size: 12px; color: #3a4a3e; }
.breakdown-bar-track { flex: 1; height: 8px; background: #f0f2f0; border-radius: 4px; overflow: hidden; }
.breakdown-bar-fill { height: 100%; border-radius: 4px; }
.breakdown-value { width: 140px; font-size: 11px; color: #6a7a6e; text-align: right; }

.report-table-section { margin-bottom: 20px; }
.report-table { width: 100%; border-collapse: collapse; font-size: 13px; }
.report-table th { text-align: left; padding: 10px 12px; font-weight: 600; color: #6a7a6e; border-bottom: 2px solid #e8ece9; font-size: 12px; white-space: nowrap; }
.report-table td { padding: 10px 12px; border-bottom: 1px solid #f0f2f0; color: #3a4a3e; }
.money-cell { font-weight: 600; color: #D4A853; }
.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.status-badge.completed { background: rgba(74,124,89,0.1); color: #4A7C59; }
.status-badge.cancelled { background: rgba(192,57,43,0.08); color: #C0392B; }
.total-row { background: #f8f9f8; font-weight: 600; }
.total-amount { font-size: 15px; }

.report-footer { display: flex; justify-content: space-between; font-size: 11px; color: #8a9a8e; padding-top: 16px; border-top: 1px solid #e8ece9; }
</style>

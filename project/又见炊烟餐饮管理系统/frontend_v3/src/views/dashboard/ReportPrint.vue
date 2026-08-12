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
        <div class="report-type-name" :style="{ color: rt.color }">{{ rt.name }}</div>
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
            <el-date-picker v-model="config.dateFrom" type="date" value-format="YYYY-MM-DD" placeholder="开始日期" style="width:140px" />
            <span>至</span>
            <el-date-picker v-model="config.dateTo" type="date" value-format="YYYY-MM-DD" placeholder="结束日期" style="width:140px" />
          </div>
        </div>
        <div class="config-item">
          <label>桌台区域</label>
          <el-select v-model="config.zone" placeholder="全部区域" clearable style="width:100%">
            <el-option label="大厅散台" value="hall" />
            <el-option label="包厢" value="private" />
            <el-option label="VIP包房" value="vip" />
          </el-select>
        </div>
        <div class="config-item">
          <label>时段筛选</label>
          <el-select v-model="config.timeSlot" placeholder="全部时段" clearable style="width:100%">
            <el-option label="午餐" value="lunch" />
            <el-option label="晚餐" value="dinner" />
            <el-option label="夜宵" value="late" />
          </el-select>
        </div>
        <div class="config-item">
          <label>预订状态</label>
          <el-select v-model="config.status" placeholder="全部状态" clearable style="width:100%">
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
            <el-option label="含进行中" value="all" />
          </el-select>
        </div>
        <div class="config-item">
          <label>客人来源</label>
          <el-select v-model="config.source" placeholder="全部来源" clearable style="width:100%">
            <el-option label="自来客" value="walk-in" />
            <el-option label="电话" value="phone" />
            <el-option label="线上" value="online" />
            <el-option label="会员" value="member" />
          </el-select>
        </div>
        <div class="config-item">
          <label>接待员工</label>
          <el-select v-model="config.staff" placeholder="全部员工" clearable style="width:100%">
            <el-option v-for="s in staffList" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </div>
        <div class="config-item">
          <label>消费金额范围</label>
          <div class="range-input">
            <el-input-number v-model="config.amountMin" :min="0" controls-position="right" placeholder="最低" style="width:110px" />
            <span>至</span>
            <el-input-number v-model="config.amountMax" :min="0" controls-position="right" placeholder="最高" style="width:110px" />
          </div>
        </div>
        <div class="config-item">
          <label>排序方式</label>
          <el-select v-model="config.sortBy" placeholder="按时间" style="width:100%">
            <el-option label="按时间" value="time" />
            <el-option label="按金额" value="amount" />
            <el-option label="按人数" value="pax" />
            <el-option label="按桌台" value="table" />
          </el-select>
        </div>
      </div>
      <div class="config-actions">
        <el-button @click="resetConfig">重置参数</el-button>
        <el-button type="primary" @click="generateReport">生成报表</el-button>
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
          <el-button size="small" @click="printReport">打印</el-button>
          <el-button size="small" @click="exportPDF">导出PDF</el-button>
          <el-button size="small" @click="exportExcel">导出Excel</el-button>
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
          <div v-if="!dailyBookingTrend.length" class="chart-empty">暂无数据</div>
          <div v-else class="mini-bar-chart">
            <div v-for="d in dailyBookingTrend" :key="d.date" class="mini-bar-group">
              <div class="mini-bar" :style="{ height: (d.count / maxDailyBooking * 100) + '%', background: d.cancelled > 0 ? '#C0392B' : '#2D4A3E' }" :title="`${d.date}: ${d.count}单${d.cancelled ? ' (取消' + d.cancelled + ')' : ''}`"></div>
              <span class="mini-bar-label">{{ d.date.slice(5) }}</span>
            </div>
          </div>
        </div>
        <div class="report-chart-item">
          <h4 class="chart-subtitle">营收构成</h4>
          <div v-if="!revenueBreakdown.length" class="chart-empty">暂无数据</div>
          <div v-else class="revenue-breakdown">
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
        <el-table :data="reportBookings" border style="width: 100%">
          <el-table-column prop="date" label="日期" width="110" />
          <el-table-column prop="time" label="时间" width="80" />
          <el-table-column prop="tableName" label="桌台" width="100" />
          <el-table-column prop="guestName" label="客人" width="100" />
          <el-table-column label="人数" width="80">
            <template #default="scope">{{ scope.row.pax }}人</template>
          </el-table-column>
          <el-table-column prop="source" label="来源" width="80" />
          <el-table-column prop="staff" label="接待人" width="90" />
          <el-table-column label="消费金额" width="130">
            <template #default="scope">
              <span class="money-cell">¥{{ (scope.row.amount || 0).toLocaleString() }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="scope">
              <span :class="['status-badge', scope.row.status]">{{ statusText(scope.row.status) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <div class="report-total-row">
          <span class="total-label">合计 · Total</span>
          <span class="money-cell total-amount">¥{{ (reportSummary.totalRevenue || 0).toLocaleString() }}</span>
          <span class="total-count">{{ reportBookings.length }}单</span>
        </div>
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

const staffList = ref([])

const config = ref({
  dateFrom: '', dateTo: '',
  zone: '', timeSlot: '', status: '', source: '', staff: '',
  amountMin: null, amountMax: null, sortBy: 'time',
})

const reportSummary = ref({
  totalBookings: 0,
  totalRevenue: 0,
  avgAmount: 0,
  totalPax: 0,
  cancelled: 0,
  completionRate: 0,
})

const dailyBookingTrend = ref([])

const maxDailyBooking = computed(() => dailyBookingTrend.value.length ? Math.max(...dailyBookingTrend.value.map(d => d.count)) : 1)

const revenueBreakdown = ref([])

const reportBookings = ref([])

const statusText = (s) => ({ completed: '已完成', cancelled: '已取消', pending: '待确认', confirmed: '已确认' }[s] || s)

const resetConfig = () => {
  config.value = {
    dateFrom: '', dateTo: '',
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
.chart-empty { text-align: center; padding: 40px 0; color: #8a9a8e; font-size: 13px; }
.money-cell { font-weight: 600; color: #D4A853; }
.status-badge { padding: 3px 10px; border-radius: 12px; font-size: 11px; font-weight: 500; }
.status-badge.completed { background: rgba(74,124,89,0.1); color: #4A7C59; }
.status-badge.cancelled { background: rgba(192,57,43,0.08); color: #C0392B; }
.report-total-row { display: flex; align-items: center; gap: 16px; padding: 12px 16px; background: #f8f9f8; border-radius: 4px; margin-top: 8px; font-weight: 600; }
.report-total-row .total-label { flex: 1; font-size: 13px; color: #1a2f23; }
.report-total-row .total-amount { font-size: 15px; }
.report-total-row .total-count { font-size: 13px; color: #6a7a6e; }

.report-footer { display: flex; justify-content: space-between; font-size: 11px; color: #8a9a8e; padding-top: 16px; border-top: 1px solid #e8ece9; }
</style>

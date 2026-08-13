<template>
  <main class="reports-page">
    <header class="page-header">
      <div>
        <p class="eyebrow">MANAGEMENT INFORMATION SYSTEM</p>
        <h1>营运报表中心 · Operations Reports</h1>
        <p>统一呈现营收、客流、菜品、成本与员工绩效，数据均来自业务系统。</p>
      </div>
      <div class="header-actions no-print">
        <el-date-picker v-model="range" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
        <el-button type="primary" :loading="loading" @click="loadReport">生成报表</el-button>
        <el-button @click="exportCsv">导出 CSV</el-button>
        <el-button @click="printReport">打印 / PDF</el-button>
      </div>
    </header>

    <section class="report-index" aria-label="报表目录">
      <div><strong>7</strong><span>标准管理报表<br />Standard Reports</span></div>
      <button v-for="section in sections" :key="section.id" :class="{ active: activeSection === section.id }" @click="activeSection = section.id">
        {{ section.cn }}<small>{{ section.en }}</small>
      </button>
    </section>

    <section class="report-sheet" v-loading="loading">
      <div class="sheet-masthead">
        <div>
          <img :src="logoImage" alt="又见炊烟私房菜 Logo" />
          <div><strong>又见炊烟私房菜</strong><span>Youjian Private Kitchen</span></div>
        </div>
        <div class="report-meta"><span>营运管理报表 · Operations Management Report</span><span>{{ periodText }}</span></div>
      </div>

      <div class="kpi-grid">
        <article v-for="item in kpis" :key="item.label">
          <span>{{ item.label }}</span><strong>{{ item.value }}</strong><small>{{ item.en }}</small>
        </article>
      </div>

      <template v-if="activeSection === 'daily'">
        <ReportTable title="每日营运摘要" subtitle="Daily Operations Summary" :columns="dailyColumns" :rows="data.daily || []" />
      </template>
      <template v-else-if="activeSection === 'payment'">
        <ReportTable title="收款方式构成" subtitle="Payment Mix" :columns="paymentColumns" :rows="data.paymentMix || []" />
      </template>
      <template v-else-if="activeSection === 'dish'">
        <ReportTable title="菜品销售排行" subtitle="Dish Sales Ranking" :columns="dishColumns" :rows="data.dishRanking || []" />
      </template>
      <template v-else-if="activeSection === 'cost'">
        <ReportTable title="部门成本分析" subtitle="Department Cost Analysis" :columns="costColumns" :rows="data.departmentCost || []" />
      </template>
      <template v-else-if="activeSection === 'staff'">
        <ReportTable title="员工营运绩效" subtitle="Staff Operations Performance" :columns="staffColumns" :rows="data.staffKpi || []" />
      </template>
      <template v-else-if="activeSection === 'booking'">
        <ReportTable title="预订与接待明细" subtitle="Booking & Guest Detail" :columns="bookingColumns" :rows="data.bookingDetails || []" />
      </template>
      <template v-else>
        <div class="executive-grid">
          <article class="trend-panel">
            <div class="section-heading"><div><h2>营收趋势</h2><p>Revenue Trend</p></div></div>
            <div v-if="dailyMax" class="bars">
              <div v-for="row in data.daily || []" :key="row.reportDate" class="bar-item">
                <span>{{ shortDate(row.reportDate) }}</span><div><i :style="{ height: `${Math.max(4, number(row.revenue) / dailyMax * 100)}%` }"></i></div><b>{{ money(row.revenue) }}</b>
              </div>
            </div>
            <el-empty v-else description="暂无营运数据" />
          </article>
          <article class="mix-panel">
            <div class="section-heading"><div><h2>管理提示</h2><p>Management Notes</p></div></div>
            <ul>
              <li><span>完成率</span><strong>{{ percent(data.overview?.completionRate) }}</strong></li>
              <li><span>人均消费 · Average Check</span><strong>{{ money(data.overview?.averageCheck) }}</strong></li>
              <li><span>取消预订 · Cancelled</span><strong>{{ number(data.overview?.cancelledBookings) }}</strong></li>
              <li><span>统计口径</span><strong>已付款收入 / 非取消客流</strong></li>
            </ul>
          </article>
        </div>
        <ReportTable title="每日营运摘要" subtitle="Daily Operations Summary" :columns="dailyColumns" :rows="data.daily || []" />
      </template>

      <footer class="sheet-footer">
        <span>系统生成 · System Generated</span><span>{{ generatedAt }}</span><span>内部管理使用 · Internal Use</span>
      </footer>
    </section>
  </main>
</template>

<script setup>
import { computed, defineComponent, h, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getOperationsReport } from '@/api/reports'
import logoImage from '@/assets/images/logo.png'

const ReportTable = defineComponent({
  props: { title: String, subtitle: String, columns: Array, rows: Array },
  setup(props) {
    const display = (row, col) => col.format ? col.format(row[col.key]) : (row[col.key] ?? '—')
    return () => h('section', { class: 'table-block' }, [
      h('div', { class: 'section-heading' }, [h('div', [h('h2', props.title), h('p', props.subtitle)]), h('span', `${props.rows.length} records`)]),
      h('div', { class: 'table-scroll' }, [h('table', [
        h('thead', [h('tr', props.columns.map(col => h('th', col.label)))]),
        h('tbody', props.rows.length ? props.rows.map((row, index) => h('tr', { key: index }, props.columns.map(col => h('td', { class: col.align === 'right' ? 'numeric' : '' }, display(row, col))))) : [h('tr', [h('td', { colspan: props.columns.length, class: 'empty-cell' }, '暂无数据 · No Data')])])
      ])])
    ])
  }
})

const today = new Date()
const ago = new Date(today.getTime() - 29 * 86400000)
const iso = d => d.toISOString().slice(0, 10)
const range = ref([iso(ago), iso(today)])
const loading = ref(false)
const data = ref({})
const activeSection = ref('executive')
const sections = [
  { id: 'executive', cn: '管理摘要', en: 'Executive' }, { id: 'daily', cn: '每日营运', en: 'Daily' },
  { id: 'payment', cn: '收款构成', en: 'Payment' }, { id: 'dish', cn: '菜品销售', en: 'Dish Sales' },
  { id: 'cost', cn: '部门成本', en: 'Cost' }, { id: 'staff', cn: '员工绩效', en: 'Staff KPI' },
  { id: 'booking', cn: '预订明细', en: 'Bookings' }
]

const number = value => Number(value || 0)
const money = value => `¥${number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const percent = value => `${number(value).toFixed(1)}%`
const shortDate = value => String(value || '').slice(5)
const periodText = computed(() => `${range.value?.[0] || '—'} — ${range.value?.[1] || '—'}`)
const generatedAt = computed(() => data.value.generatedAt ? new Date(data.value.generatedAt).toLocaleString('zh-CN') : '—')
const dailyMax = computed(() => Math.max(0, ...(data.value.daily || []).map(row => number(row.revenue))))
const kpis = computed(() => [
  { label: '营业收入', en: 'Revenue', value: money(data.value.overview?.bookingRevenue) },
  { label: '预订总数', en: 'Bookings', value: number(data.value.overview?.totalBookings).toLocaleString() },
  { label: '接待客流', en: 'Guests Served', value: number(data.value.overview?.totalGuests).toLocaleString() },
  { label: '人均消费', en: 'Average Check', value: money(data.value.overview?.averageCheck) },
  { label: '完成率', en: 'Completion', value: percent(data.value.overview?.completionRate) }
])

const dailyColumns = [
  { key: 'reportDate', label: '日期 Date' }, { key: 'bookingCount', label: '预订 Bookings', align: 'right' },
  { key: 'guestCount', label: '客流 Guests', align: 'right' }, { key: 'revenue', label: '营收 Revenue', align: 'right', format: money },
  { key: 'deposit', label: '定金 Deposit', align: 'right', format: money }, { key: 'cancelled', label: '取消 Cancelled', align: 'right' }
]
const paymentColumns = [{ key: 'paymentMethod', label: '收款方式 Payment Method' }, { key: 'transactionCount', label: '笔数 Transactions', align: 'right' }, { key: 'amount', label: '金额 Amount', align: 'right', format: money }]
const dishColumns = [{ key: 'dishName', label: '菜品 Dish' }, { key: 'quantity', label: '销量 Quantity', align: 'right' }, { key: 'salesAmount', label: '销售额 Sales', align: 'right', format: money }]
const costColumns = [{ key: 'department', label: '成本类别 Cost Category' }, { key: 'itemCount', label: '笔数 Items', align: 'right' }, { key: 'totalCost', label: '成本 Cost', align: 'right', format: money }]
const staffColumns = [{ key: 'staffName', label: '员工 Staff' }, { key: 'bookingCount', label: '预订 Bookings', align: 'right' }, { key: 'servedGuests', label: '服务客流 Guests', align: 'right' }, { key: 'salesAmount', label: '销售额 Sales', align: 'right', format: money }]
const bookingColumns = [{ key: 'bookingNo', label: '预订号 Booking No.' }, { key: 'bookingDate', label: '日期 Date' }, { key: 'customerName', label: '客人 Guest' }, { key: 'guestCount', label: '人数 Pax', align: 'right' }, { key: 'staffName', label: '员工 Staff' }, { key: 'finalAmount', label: '金额 Amount', align: 'right', format: money }, { key: 'bookingStatus', label: '状态 Status' }]

async function loadReport() {
  if (!range.value?.[0] || !range.value?.[1]) return ElMessage.warning('请选择报表日期范围')
  loading.value = true
  try {
    const res = await getOperationsReport({ startDate: range.value[0], endDate: range.value[1] })
    data.value = res.data || {}
  } catch (error) {
    ElMessage.error(error.message || '报表加载失败')
  } finally { loading.value = false }
}

function exportCsv() {
  const rows = data.value.bookingDetails || []
  if (!rows.length) return ElMessage.warning('当前没有可导出的明细数据')
  const columns = bookingColumns
  const csv = [columns.map(c => c.label), ...rows.map(row => columns.map(c => String(row[c.key] ?? '')))]
    .map(row => row.map(cell => `"${cell.replaceAll('"', '""')}"`).join(',')).join('\n')
  const blob = new Blob([`\uFEFF${csv}`], { type: 'text/csv;charset=utf-8' })
  const link = document.createElement('a'); link.href = URL.createObjectURL(blob); link.download = `operations-${range.value[0]}-${range.value[1]}.csv`; link.click(); URL.revokeObjectURL(link.href)
}
function printReport() { window.print() }
onMounted(loadReport)
</script>

<style scoped>
.reports-page { padding: 24px 28px 48px; color: #23342d; background: #f4f6f4; min-height: 100%; font-family: "Noto Sans SC", sans-serif; }
.page-header { display: flex; justify-content: space-between; align-items: flex-end; gap: 20px; margin-bottom: 18px; }
.eyebrow { margin: 0 0 5px; color: #9a7b36; font-size: 11px; font-weight: 700; letter-spacing: 2px; }
.page-header h1 { margin: 0; font-size: 25px; letter-spacing: .3px; }.page-header p:not(.eyebrow) { margin: 6px 0 0; color: #718078; font-size: 13px; }
.header-actions { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; justify-content: flex-end; }
.report-index { display: flex; gap: 8px; padding: 10px; background: #fff; border: 1px solid #dde4df; margin-bottom: 16px; overflow-x: auto; }
.report-index > div { min-width: 150px; display: flex; gap: 10px; align-items: center; padding: 4px 12px; border-right: 1px solid #dde4df; }.report-index > div strong { color: #9a7b36; font-size: 27px; }.report-index > div span { font-size: 11px; line-height: 1.45; }
.report-index button { min-width: 108px; border: 0; background: transparent; color: #526159; padding: 8px 10px; cursor: pointer; font-weight: 600; }.report-index button small { display: block; margin-top: 3px; color: #95a098; font-weight: 400; }.report-index button.active { background: #315a4a; color: #fff; }.report-index button.active small { color: #dce8e2; }
.report-sheet { background: #fff; border: 1px solid #d7dfda; box-shadow: 0 12px 32px rgba(35,52,45,.07); padding: 26px; }
.sheet-masthead { display: flex; justify-content: space-between; align-items: center; border-bottom: 3px solid #315a4a; padding-bottom: 15px; }.sheet-masthead > div:first-child { display: flex; align-items: center; gap: 12px; }.sheet-masthead img { width: 54px; height: 54px; object-fit: contain; }.sheet-masthead strong { display: block; font-size: 18px; }.sheet-masthead span { display: block; color: #77847d; font-size: 11px; margin-top: 3px; }.report-meta { text-align: right; }
.kpi-grid { display: grid; grid-template-columns: repeat(5, 1fr); border: 1px solid #dfe5e1; border-top: 0; margin-bottom: 24px; }.kpi-grid article { padding: 17px; border-right: 1px solid #dfe5e1; }.kpi-grid article:last-child { border-right: 0; }.kpi-grid span,.kpi-grid small { display:block; color:#78857e; font-size:11px; }.kpi-grid strong { display:block; color:#234638; font-size:21px; margin:5px 0 2px; font-variant-numeric:tabular-nums; }
.executive-grid { display:grid; grid-template-columns: 1.55fr .75fr; gap:20px; margin-bottom:24px; }.trend-panel,.mix-panel { border:1px solid #dfe5e1; padding:18px; }.section-heading { display:flex; align-items:flex-end; justify-content:space-between; border-bottom:1px solid #dfe5e1; padding-bottom:9px; margin-bottom:13px; }.section-heading h2 { margin:0; font-size:15px; }.section-heading p,.section-heading span { margin:2px 0 0; color:#87928c; font-size:10px; text-transform:uppercase; letter-spacing:1px; }
.bars { height:180px; display:flex; align-items:flex-end; gap:5px; padding-top:10px; }.bar-item { flex:1; height:100%; min-width:18px; display:flex; flex-direction:column; align-items:center; }.bar-item > div { flex:1; width:100%; display:flex; align-items:flex-end; justify-content:center; border-bottom:1px solid #ccd6d0; }.bar-item i { width:60%; max-width:24px; background:#315a4a; display:block; }.bar-item span { font-size:9px; color:#78857e; margin-top:5px; }.bar-item b { display:none; }
.mix-panel ul { list-style:none; padding:0; margin:0; }.mix-panel li { display:flex; justify-content:space-between; gap:12px; padding:12px 0; border-bottom:1px solid #edf0ee; font-size:12px; }.mix-panel li strong { text-align:right; color:#315a4a; }
.table-block { margin-top:22px; }.table-scroll { overflow-x:auto; }table { width:100%; border-collapse:collapse; font-size:12px; }th { background:#eef3f0; color:#43544b; text-align:left; padding:10px; border:1px solid #d8e0db; white-space:nowrap; }td { padding:10px; border:1px solid #e1e6e3; color:#46564e; }.numeric { text-align:right; font-variant-numeric:tabular-nums; }.empty-cell { text-align:center; padding:30px; color:#8a958f; }
.sheet-footer { display:flex; justify-content:space-between; gap:15px; border-top:1px solid #d9e0dc; margin-top:24px; padding-top:12px; color:#7d8982; font-size:10px; }
@media (max-width: 1100px) { .page-header { align-items:flex-start; flex-direction:column; }.kpi-grid { grid-template-columns:repeat(2,1fr); }.executive-grid { grid-template-columns:1fr; } }
@media print { .reports-page { padding:0; background:#fff; }.no-print,.report-index { display:none !important; }.report-sheet { box-shadow:none; border:0; padding:0; }.kpi-grid { break-inside:avoid; }.table-block { break-inside:auto; }thead { display:table-header-group; }tr { break-inside:avoid; }.sheet-footer { position:running(footer); } }
</style>

<template>
  <div class="report-print-page">
    <header class="page-header">
      <h2 class="page-title">报表打印 · Report &amp; Print</h2>
      <p class="page-subtitle">按预订日期查询、预览和导出</p>
    </header>
    <div class="report-type-grid">
      <button v-for="type in REPORT_TYPES" :key="type.id" class="report-type-card"
        :class="{ active: config.type === type.id }" :aria-pressed="config.type === type.id" @click="config.type = type.id">
        <span class="report-type-name">{{ type.name }}</span>
        <span class="report-type-desc">{{ type.desc }}</span>
      </button>
    </div>
    <section class="report-config-card">
      <h3 class="section-title">报表参数</h3>
      <div class="config-item" v-if="config.type !== 'custom'">
        <label for="report-anchor">{{ config.type === 'daily' ? '查询日期' : '选择期间内任一天' }}</label>
        <input id="report-anchor" type="date" v-model="config.anchor" />
      </div>
      <div class="date-range" v-else>
        <div class="config-item"><label for="report-start">开始日期</label><input id="report-start" type="date" v-model="config.startDate" /></div>
        <span>至</span>
        <div class="config-item"><label for="report-end">结束日期</label><input id="report-end" type="date" v-model="config.endDate" /></div>
      </div>
      <p class="page-subtitle">{{ rangeLabel }}</p>
      <div class="config-actions">
        <button class="btn-secondary" @click="resetConfig">重置</button>
        <button class="btn-primary" :disabled="state.loading" @click="generateReport">{{ state.loading ? '查询中…' : '生成报表' }}</button>
      </div>
    </section>
    <p class="source-note">{{ SOURCE_NOTE }}{{ STATUS_NOTE }}</p>
    <p v-if="state.error" role="alert" class="error-note">{{ state.error }}</p>
    <p v-else-if="state.loading" role="status">正在查询所选期间…</p>
    <p v-else-if="state.report && !state.report.rows.length" role="status">选定期间暂无预订数据，无法打印或导出。</p>
    <p v-else-if="!state.report" class="page-subtitle">选择期间后点击“生成报表”。</p>
    <section v-if="usable" class="report-preview-card">
      <div class="preview-actions">
        <button class="btn-sm" :disabled="printing" @click="printReport">打印报表</button>
        <button class="btn-sm" :disabled="printing" @click="printReport">打印 / 保存 PDF</button>
        <button class="btn-sm" :disabled="exporting" @click="exportExcel">{{ exporting ? '导出中…' : '导出 Excel' }}</button>
      </div>
      <p class="page-subtitle">PDF：在浏览器打印窗口选择“另存为 PDF”。</p>
      <article class="report-region">
        <h3>{{ currentTitle }} · 预订金额报表</h3>
        <p>{{ state.report.startDate }} 至 {{ state.report.endDate }}</p>
        <p class="source-note">{{ SOURCE_NOTE }}{{ STATUS_NOTE }}</p>
        <div class="report-summary">
          <div><span>预订数</span><strong>{{ state.report.totalBookings }}</strong></div>
          <div><span>预订金额</span><strong>¥{{ state.report.totalRevenue.toFixed(2) }}</strong></div>
          <div><span>单均预订金额</span><strong>¥{{ average.toFixed(2) }}</strong></div>
          <div><span>预订人数</span><strong>{{ state.report.totalGuests }}</strong></div>
        </div>
        <div class="table-scroll">
          <table class="report-table">
            <thead><tr><th>预订日期</th><th>订单数</th><th>预订金额（元）</th><th>单均预订金额（元）</th></tr></thead>
            <tbody><tr v-for="row in state.report.rows" :key="row.date"><td>{{ row.date }}</td><td>{{ row.bookingCount }}</td><td>{{ row.revenue.toFixed(2) }}</td><td>{{ row.averagePerBooking.toFixed(2) }}</td></tr></tbody>
          </table>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardReport } from '@/api/booking'
import { localDate, money } from '@/utils/reportExport'
import { REPORT_TYPES, SOURCE_NOTE, STATUS_NOTE, resolveBookingRange, createBookingReportLoader,
  canUseBookingReport, bookingPrintHtml, bookingReportWorkbook } from '@/utils/bookingReportPrint'

const defaults = () => { const today = localDate(new Date()); return { type: 'daily', anchor: today, startDate: today, endDate: today } }
const config = reactive(defaults())
const state = reactive({ loading: false, report: null, error: '', key: '' })
const loader = createBookingReportLoader(state, getDashboardReport)
const exporting = ref(false)
const printing = ref(false)
let printFrame = null
const usable = computed(() => canUseBookingReport(state, config))
const currentTitle = computed(() => REPORT_TYPES.find(type => type.id === config.type)?.name || '报表')
const average = computed(() => state.report?.totalBookings ? money(state.report.totalRevenue / state.report.totalBookings) : 0)
const rangeLabel = computed(() => {
  try { const range = resolveBookingRange(config); return `查询范围：${range.startDate} 至 ${range.endDate}` }
  catch { return '请选择有效的日期范围' }
})
function clearPrint() { printFrame?.remove(); printFrame = null; printing.value = false }
watch(config, () => { loader.invalidate(); clearPrint() }, { deep: true, flush: 'sync' })
onBeforeUnmount(() => { loader.invalidate(); clearPrint() })
function resetConfig() { Object.assign(config, defaults()) }
function generateReport() { clearPrint(); return loader.load({ ...config }) }

function printReport() {
  if (!usable.value || printing.value) return
  const snapshot = state.report
  try {
    const html = bookingPrintHtml(snapshot, currentTitle.value)
    clearPrint()
    const frame = document.createElement('iframe')
    printFrame = frame
    printing.value = true
    frame.title = '预订报表打印区域'
    frame.style.cssText = 'position:fixed;left:-10000px;top:0;width:800px;height:600px;border:0'
    frame.onload = () => {
      if (printFrame !== frame || !usable.value || state.report !== snapshot) { frame.remove(); return }
      try {
        frame.contentWindow.onafterprint = () => { if (printFrame === frame) clearPrint() }
        frame.contentWindow.focus()
        frame.contentWindow.print()
        printing.value = false
      } catch (error) { clearPrint(); ElMessage.error(error.message || '无法打开打印窗口') }
    }
    frame.srcdoc = html
    document.body.appendChild(frame)
  } catch (error) { clearPrint(); ElMessage.error(error.message || '打印失败') }
}

async function exportExcel() {
  if (!usable.value || exporting.value) return
  const snapshot = state.report
  exporting.value = true
  try {
    const { buffer, filename } = await bookingReportWorkbook(snapshot)
    if (!usable.value || state.report !== snapshot) { ElMessage.warning('查询条件已变化，请重新生成报表'); return }
    const url = URL.createObjectURL(new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    const link = document.createElement('a')
    try { link.href = url; link.download = filename; document.body.appendChild(link); link.click() }
    finally { link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000) }
  } catch (error) { ElMessage.error(error.message || '导出失败') }
  finally { exporting.value = false }
}
</script>

<style scoped>
.report-print-page { padding:24px 32px; color:#1a2f23; }
.page-header { margin-bottom:20px; }
.page-title { font-size:22px; margin:0; }
.page-subtitle, .source-note { font-size:13px; color:#6a7a6e; line-height:1.7; }
.report-type-grid { display:grid; grid-template-columns:repeat(4,1fr); gap:12px; margin-bottom:20px; }
.report-type-card { background:#fff; border:1px solid #e8ece9; border-radius:8px; padding:20px 12px; cursor:pointer; display:flex; flex-direction:column; gap:8px; align-items:center; }
.report-type-card.active { border-color:#2d4a3e; background:#f2f6f3; }
.report-type-name { font-size:14px; font-weight:600; }
.report-type-desc { font-size:12px; color:#6a7a6e; }
.report-config-card, .report-preview-card { background:#fff; border:1px solid #e8ece9; border-radius:8px; padding:24px; margin-bottom:20px; }
.section-title { font-size:15px; margin:0 0 16px; }
.config-item { display:flex; flex-direction:column; gap:6px; align-items:flex-start; }
.config-item label { font-size:13px; color:#6a7a6e; }
.config-item input { padding:8px 10px; border:1px solid #d0d8d2; border-radius:6px; font:inherit; color:inherit; }
.date-range, .config-actions, .preview-actions { display:flex; gap:10px; align-items:center; flex-wrap:wrap; }
.config-actions, .preview-actions { justify-content:flex-end; }
.btn-primary, .btn-secondary, .btn-sm { padding:8px 16px; border:1px solid #d0d8d2; border-radius:6px; background:#fff; color:#3a4a3e; cursor:pointer; }
.btn-primary { background:#2d4a3e; border-color:#2d4a3e; color:#fff; }
button:disabled { opacity:.5; cursor:not-allowed; }
button:focus-visible { outline:2px solid #4a7c59; outline-offset:3px; }
.report-summary { display:grid; grid-template-columns:repeat(4,1fr); gap:16px; padding:16px; background:#f8f9f8; border-radius:8px; margin:20px 0; }
.report-summary div { display:flex; flex-direction:column; gap:8px; }
.report-summary span { color:#6a7a6e; font-size:12px; }
.report-summary strong { font-size:20px; }
.table-scroll { overflow-x:auto; }
.report-table { width:100%; border-collapse:collapse; font-size:13px; }
.report-table th, .report-table td { padding:12px; border-bottom:1px solid #e8ece9; text-align:right; white-space:nowrap; }
.report-table th:first-child, .report-table td:first-child { text-align:left; }
.error-note { color:#b42318; }
@media(max-width:700px) { .report-print-page { padding:16px; } .report-type-grid,.report-summary { grid-template-columns:repeat(2,1fr); } }
</style>

<template>
  <div class="page">
    <div class="page-header">
      
      <h2>数据报表 · Data Reports</h2>
      <p class="page-desc">营收报表 · 成本分析 · 客户统计</p>
    </div>
    <div class="summary-cards" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:16px;margin-bottom:20px">
      <div class="card" style="padding:20px">
        <div style="font-size:13px;color:var(--color-text-muted)">选定期间预订金额</div>
        <div style="font-size:28px;font-weight:600;color:var(--color-primary)">{{ state.report ? state.report.totalRevenue.toFixed(2) : '—' }}</div>
      </div>
      <div class="card" style="padding:20px">
        <div style="font-size:13px;color:var(--color-text-muted)">选定期间订单数</div>
        <div style="font-size:28px;font-weight:600">{{ state.report?.totalBookings ?? '—' }}</div>
      </div>
      <div class="card" style="padding:20px">
        <div style="font-size:13px;color:var(--color-text-muted)">选定期间接待人数</div>
        <div style="font-size:28px;font-weight:600;color:var(--color-info)">{{ state.report?.totalGuests ?? '—' }}</div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker v-model="reportRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
      </div>
      <div class="toolbar-right">
        <el-button :disabled="!canExport || exporting" :loading="exporting" @click="exportData">导出 Excel</el-button>
      </div>
    </div>

    <el-alert title="本页按预订金额统计，不代表实际收款；取消与退款尚未从金额中扣减。" type="info" :closable="false" class="report-basis" />
    <el-alert v-if="state.error" :title="state.error" type="error" :closable="false" />
    <el-table :data="state.report?.rows || []" stripe class="data-table" v-loading="state.loading">
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="bookingCount" label="订单数" width="80" />
      <el-table-column label="预订金额" width="120">
        <template #default="{ row }">¥{{ Number(row.revenue || 0).toFixed(2) }}</template>
      </el-table-column>
      <el-table-column label="单均金额" width="100">
        <template #default="{ row }">¥{{ row.averagePerBooking.toFixed(2) }}</template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch } from 'vue'
import { getDashboardReport } from '@/api/booking'
import { ElMessage } from 'element-plus'
import { createReportLoader, canExportReport, buildReportWorkbook } from '@/utils/reportExport'

const state = reactive({ loading: false, report: null, error: '' })
const exporting = ref(false)
const today = new Date()
const start = new Date(today)
start.setDate(start.getDate() - 30)
const reportRange = ref([start, today])
const fetchReport = createReportLoader(state, getDashboardReport)
const canExport = computed(() => canExportReport(state, reportRange.value))
// Synchronous invalidation prevents export between a picker change and the next render.
watch(reportRange, value => fetchReport(value), { immediate: true, deep: true, flush: 'sync' })

async function exportData() {
  if (exporting.value) return
  if (!canExport.value) { ElMessage.warning('请先加载所选日期范围内的有效数据'); return }
  const snapshot = state.report
  exporting.value = true
  try {
    const { buffer, filename } = await buildReportWorkbook(snapshot)
    if (!canExport.value || state.report !== snapshot) {
      ElMessage.warning('日期范围或数据已变化，请重新导出')
      return
    }
    const url = URL.createObjectURL(new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
    const link = document.createElement('a')
    try {
      link.href = url
      link.download = filename
      document.body.appendChild(link)
      link.click()
    } finally {
      link.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    }
  } catch (error) {
    ElMessage.error(error.message || '导出失败，请重试')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
.back-btn:hover { background:#fff; color:#1e293b; border-color:#94a3b8; }
.report-basis { margin-bottom:12px; }
@media (max-width: 600px) {
  .page-header { align-items:flex-start; flex-direction:column; gap:4px; }
  .toolbar, .toolbar-left, .toolbar-right { width:100%; min-width:0; box-sizing:border-box; }
  .toolbar { align-items:stretch; }
  .toolbar-left :deep(.el-date-editor) { width:100%; min-width:0; max-width:100%; box-sizing:border-box; }
  .toolbar-right { justify-content:flex-start; }
  .toolbar-right .el-button { max-width:100%; }
}
</style>



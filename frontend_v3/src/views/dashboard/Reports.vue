<template>
  <div class="page">
    <div class="page-header">
      
      <h2>数据报表 · Data Reports</h2>
      <p class="page-desc">营收报表 · 成本分析 · 客户统计</p>
    </div>
    <div class="summary-cards" style="display:grid;grid-template-columns:repeat(auto-fit,minmax(200px,1fr));gap:16px;margin-bottom:20px">
      <div class="card" style="padding:20px">
        <div style="font-size:13px;color:var(--color-text-muted)">本月营收</div>
        <div style="font-size:28px;font-weight:600;color:var(--color-primary)">{{ monthlyRevenue.toFixed(0) }}</div>
      </div>
      <div class="card" style="padding:20px">
        <div style="font-size:13px;color:var(--color-text-muted)">本月订单数</div>
        <div style="font-size:28px;font-weight:600">{{ monthlyOrders }}</div>
      </div>
      <div class="card" style="padding:20px">
        <div style="font-size:13px;color:var(--color-text-muted)">活跃客户</div>
        <div style="font-size:28px;font-weight:600;color:var(--color-info)">{{ activeCustomers }}</div>
      </div>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-date-picker v-model="reportRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" @change="fetchReport" />
      </div>
      <div class="toolbar-right">
        <el-button @click="exportData">导出 Excel</el-button>
      </div>
    </div>

    <el-table :data="reportList" stripe class="data-table" v-loading="loading">
      <el-table-column prop="date" label="日期" width="120" />
      <el-table-column prop="bookingCount" label="订单数" width="80" />
      <el-table-column prop="guestCount" label="接待人数" width="80" />
      <el-table-column prop="revenue" label="营收" width="100" />
      <el-table-column prop="deposit" label="定金" width="80" />
      <el-table-column prop="avgSpend" label="客单价" width="80" />
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getDashboardReport } from '@/api/booking'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const reportList = ref([])
const monthlyRevenue = ref(0); const monthlyOrders = ref(0); const activeCustomers = ref(0)
const reportRange = ref([new Date(Date.now() - 30*86400000), new Date()])

async function fetchReport() {
  loading.value = true
  try {
    const res = await getDashboardReport()
    if (res.code === 200) {
      const data = res.data || {}
      reportList.value = data.report || data.daily || []
      monthlyRevenue.value = data.monthlyRevenue || 0
      monthlyOrders.value = data.monthlyOrders || 0
      activeCustomers.value = data.activeCustomers || 0
    }
  } catch (e) { console.error(e); ElMessage.error('加载报表数据失败') } finally { loading.value = false }
}

function exportData() {
  ElMessage.success('导出功能开发中')
}

onMounted(fetchReport)
</script>

<style scoped>
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
.back-btn:hover { background:#fff; color:#1e293b; border-color:#94a3b8; }
</style>



<template>
  <div class="reports-page">
    <div class="page-header">
      <h2 class="page-title">数据报表 · Reports</h2>
      <p class="page-subtitle">报表管理 · 生成 · 审核 · 归档 · Report Management</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :style="{ color: s.color }">
        <div class="stat-content">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
        </div>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="toolbar-left">
        <el-select v-model="typeFilter" placeholder="报表类型 · Type" clearable style="width:180px">
          <el-option v-for="t in reportTypes" :key="t" :label="t" :value="t" />
        </el-select>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" />
      </div>
      <div class="toolbar-right">
        <el-button @click="exportData">导出 · Export</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="filteredReports" stripe v-loading="loading" empty-text="暂无报表 · No reports">
        <el-table-column prop="name" label="报表名 · Name" min-width="180" />
        <el-table-column prop="type" label="类型 · Type" min-width="120">
          <template #default="{ row }">
            <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="period" label="周期 · Period" min-width="140" />
        <el-table-column prop="generatedAt" label="生成时间 · Generated" min-width="160" />
        <el-table-column prop="status" label="状态 · Status" min-width="110" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="statusType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作 · Actions" min-width="160" align="center">
          <template #default="{ row }">
            <el-button link size="small" @click="viewReport(row)">查看</el-button>
            <el-button link size="small" type="primary" @click="auditReport(row)">审核</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const reportList = ref([])
const typeFilter = ref('')
const dateRange = ref([])
const reportTypes = ['日报', '周报', '月报', '季报', '年报', '专项报表']

const stats = computed(() => [
  { label: '报表总数 · Total', value: reportList.value.length, color: '#2D4A3E' },
  { label: '今日生成 · Today', value: reportList.value.filter(r => r.isToday).length, color: '#4A7C59' },
  { label: '待审核 · Pending', value: reportList.value.filter(r => r.status === '待审核').length, color: '#D4A853' },
  { label: '已归档 · Archived', value: reportList.value.filter(r => r.status === '已归档').length, color: '#5B7B8A' },
])

const filteredReports = computed(() => {
  let arr = reportList.value
  if (typeFilter.value) arr = arr.filter(r => r.type === typeFilter.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [s, e] = dateRange.value
    arr = arr.filter(r => r.generatedAt && r.generatedAt >= s && r.generatedAt <= e)
  }
  return arr
})

function statusType(s) {
  return ({ '待审核': 'warning', '已审核': 'success', '已归档': 'info' })[s] || ''
}

function viewReport(row) { ElMessage.info('查看报表 · View: ' + (row.name || '')) }
function auditReport(row) { ElMessage.info('审核报表 · Audit: ' + (row.name || '')) }
function exportData() { ElMessage.info('导出功能开发中 · Export coming soon') }
</script>

<style scoped>
.reports-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); margin: 4px 0 0; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 24px 32px; position: relative; overflow: hidden; }
.stat-card::after { content: ''; position: absolute; top: 0; right: 0; width: 80px; height: 80px; background: currentColor; opacity: 0.03; border-radius: 0 0 0 80px; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 6px; font-weight: 500; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--color-text); line-height: 1.2; }

.toolbar-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 16px 24px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.toolbar-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }

.table-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 16px; }

@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } .toolbar-card { flex-direction: column; align-items: stretch; } }
</style>

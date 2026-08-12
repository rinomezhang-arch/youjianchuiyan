<template>
  <div class="finance-page">
    <div class="page-header">
      <h2 class="page-title">财务管理 · Finance</h2>
      <p class="page-subtitle">本月营收 · 成本 · 毛利分析 · Monthly Revenue & Cost</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :style="{ color: s.color }">
        <div class="stat-content">
          <div class="stat-label">{{ s.label }}</div>
          <div class="stat-value">{{ s.value }}</div>
          <div class="stat-sub">{{ s.sub }}</div>
        </div>
      </div>
    </div>

    <div class="toolbar-card">
      <div class="toolbar-left">
        <span class="toolbar-label">月份 · Month</span>
        <el-date-picker v-model="month" type="month" placeholder="选择月份 · Select month" value-format="YYYY-MM" @change="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button @click="exportData">导出 · Export</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="tableData" stripe v-loading="loading" empty-text="暂无数据 · No data">
        <el-table-column prop="date" label="日期 · Date" min-width="120" />
        <el-table-column label="营收 · Revenue" min-width="120" align="right">
          <template #default="{ row }">¥{{ (row.revenue || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="成本 · Cost" min-width="120" align="right">
          <template #default="{ row }">¥{{ (row.cost || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="毛利 · Gross Profit" min-width="120" align="right">
          <template #default="{ row }">¥{{ (row.profit || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="毛利率 · Margin" min-width="120" align="center">
          <template #default="{ row }">
            <span :style="{ color: marginColor(row.margin) }">{{ (row.margin || 0).toFixed(1) }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="环比 · MoM" min-width="110" align="center">
          <template #default="{ row }">
            <span :class="(row.mom || 0) >= 0 ? 'trend-up' : 'trend-down'">{{ (row.mom || 0) >= 0 ? '+' : '' }}{{ (row.mom || 0).toFixed(1) }}%</span>
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
const month = ref('')
const tableData = ref([])

const stats = computed(() => {
  const revenue = tableData.value.reduce((s, r) => s + (r.revenue || 0), 0)
  const cost = tableData.value.reduce((s, r) => s + (r.cost || 0), 0)
  const profit = revenue - cost
  const margin = revenue > 0 ? (profit / revenue) * 100 : 0
  return [
    { label: '本月营收 · Revenue', value: '¥' + revenue.toFixed(2), sub: 'Monthly Revenue', color: '#2D4A3E' },
    { label: '本月成本 · Cost', value: '¥' + cost.toFixed(2), sub: 'Monthly Cost', color: '#C25555' },
    { label: '本月毛利 · Profit', value: '¥' + profit.toFixed(2), sub: 'Gross Profit', color: '#C4A35A' },
    { label: '毛利率 · Margin', value: margin.toFixed(1) + '%', sub: 'Gross Margin', color: '#4A7C59' },
  ]
})

function marginColor(m) {
  if (m >= 60) return '#4A7C59'
  if (m >= 40) return '#D4A853'
  return '#C25555'
}

async function fetchData() {
  // TODO: 接入真实接口，按月份查询 / fetch by month
  loading.value = false
}

function exportData() {
  ElMessage.info('导出功能开发中 · Export coming soon')
}
</script>

<style scoped>
.finance-page { max-width: 1400px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.page-title { font-size: 20px; font-weight: 700; color: var(--color-text); margin: 0; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); margin: 4px 0 0; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 24px 32px; position: relative; overflow: hidden; }
.stat-card::after { content: ''; position: absolute; top: 0; right: 0; width: 80px; height: 80px; background: currentColor; opacity: 0.03; border-radius: 0 0 0 80px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: var(--color-text-muted); margin-bottom: 6px; font-weight: 500; }
.stat-value { font-size: 26px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.stat-sub { font-size: 11px; color: var(--color-text-muted); margin-top: 6px; }

.toolbar-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 16px 24px; margin-bottom: 16px; display: flex; align-items: center; justify-content: space-between; gap: 16px; flex-wrap: wrap; }
.toolbar-left { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.toolbar-label { font-size: 13px; color: var(--color-text-secondary); }

.table-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 16px; }

.trend-up { color: #4A7C59; font-weight: 600; }
.trend-down { color: #C25555; font-weight: 600; }

@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } .toolbar-card { flex-direction: column; align-items: stretch; } }
</style>

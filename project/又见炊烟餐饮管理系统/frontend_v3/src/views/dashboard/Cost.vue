<template>
  <div class="cost-page">
    <div class="page-header">
      <h2 class="page-title">成本分析 · Cost Analysis</h2>
      <p class="page-subtitle">月度成本构成 · 食材 · 人工 · 其他 · Monthly Cost Breakdown</p>
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
        <span class="toolbar-label">日期范围 · Date Range</span>
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD" @change="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button @click="exportData">导出 · Export</el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="tableData" stripe v-loading="loading" empty-text="暂无数据 · No data">
        <el-table-column prop="month" label="月份 · Month" min-width="120" />
        <el-table-column label="总成本 · Total" min-width="130" align="right">
          <template #default="{ row }">¥{{ (row.total || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="食材 · Ingredient" min-width="130" align="right">
          <template #default="{ row }">¥{{ (row.ingredient || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="人工 · Labor" min-width="130" align="right">
          <template #default="{ row }">¥{{ (row.labor || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="其他 · Other" min-width="130" align="right">
          <template #default="{ row }">¥{{ (row.other || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column label="同比 · YoY" min-width="120" align="center">
          <template #default="{ row }">
            <span :class="(row.yoy || 0) >= 0 ? 'trend-up' : 'trend-down'">{{ (row.yoy || 0) >= 0 ? '+' : '' }}{{ (row.yoy || 0).toFixed(1) }}%</span>
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
const dateRange = ref([])
const tableData = ref([])

const stats = computed(() => {
  const total = tableData.value.reduce((s, r) => s + (r.total || 0), 0)
  const ingredient = tableData.value.reduce((s, r) => s + (r.ingredient || 0), 0)
  const labor = tableData.value.reduce((s, r) => s + (r.labor || 0), 0)
  const other = tableData.value.reduce((s, r) => s + (r.other || 0), 0)
  return [
    { label: '总成本 · Total', value: '¥' + total.toFixed(2), color: '#2D4A3E' },
    { label: '食材成本 · Ingredient', value: '¥' + ingredient.toFixed(2), color: '#C4A35A' },
    { label: '人工成本 · Labor', value: '¥' + labor.toFixed(2), color: '#4A7C59' },
    { label: '其他成本 · Other', value: '¥' + other.toFixed(2), color: '#5B7B8A' },
  ]
})

async function fetchData() {
  // TODO: 接入真实接口，按日期范围查询 / fetch by date range
  loading.value = false
}

function exportData() {
  ElMessage.info('导出功能开发中 · Export coming soon')
}
</script>

<style scoped>
.cost-page { max-width: 1400px; margin: 0 auto; }
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
.toolbar-label { font-size: 13px; color: var(--color-text-secondary); }

.table-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 10px; padding: 16px; }

.trend-up { color: #C25555; font-weight: 600; }
.trend-down { color: #4A7C59; font-weight: 600; }

@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } .toolbar-card { flex-direction: column; align-items: stretch; } }
</style>

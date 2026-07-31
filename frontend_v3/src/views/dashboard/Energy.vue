<template>
  <div class="energy-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">能耗管理 · Energy Management</h2>
        <p class="page-desc">水电气消耗统计 · 费用分析 · 同比环比趋势</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleExport">导出报表</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">本月用电 · Electricity</span>
        <span class="stat-value" style="color:#C4A35A">{{ stats.electric }} <span class="unit">kWh</span></span>
      </div>
      <div class="stat-card">
        <span class="stat-label">本月用水 · Water</span>
        <span class="stat-value" style="color:#5B7B8A">{{ stats.water }} <span class="unit">t</span></span>
      </div>
      <div class="stat-card">
        <span class="stat-label">本月燃气 · Gas</span>
        <span class="stat-value" style="color:#C0392B">{{ stats.gas }} <span class="unit">m³</span></span>
      </div>
      <div class="stat-card">
        <span class="stat-label">费用总额 · Total Cost</span>
        <span class="stat-value">¥{{ stats.cost.toLocaleString() }}</span>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 280px"
          />
          <el-button @click="resetFilter">重置</el-button>
        </div>
      </div>

      <el-table :data="filteredRecords" stripe style="width: 100%" show-summary :summary-method="getSummaries">
        <el-table-column prop="month" label="月份" width="120" />
        <el-table-column label="用电 (kWh)" width="140">
          <template #default="{ row }">{{ (row.electric || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="用水 (t)" width="130">
          <template #default="{ row }">{{ row.water || 0 }}</template>
        </el-table-column>
        <el-table-column label="燃气 (m³)" width="140">
          <template #default="{ row }">{{ row.gas || 0 }}</template>
        </el-table-column>
        <el-table-column label="费用 (元)" width="140">
          <template #default="{ row }">¥{{ (row.cost || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="同比" width="130">
          <template #default="{ row }">
            <el-tag :type="yoyType(row.yoy)" effect="plain">
              {{ row.yoy > 0 ? '+' : '' }}{{ row.yoy || 0 }}%
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const dateRange = ref([])

const records = ref([])

const stats = computed(() => {
  const list = records.value
  return {
    electric: list.reduce((s, r) => s + (r.electric || 0), 0),
    water: list.reduce((s, r) => s + (r.water || 0), 0),
    gas: list.reduce((s, r) => s + (r.gas || 0), 0),
    cost: list.reduce((s, r) => s + (r.cost || 0), 0),
  }
})

const filteredRecords = computed(() => {
  if (!dateRange.value || dateRange.value.length !== 2) return records.value
  const [start, end] = dateRange.value
  const s = new Date(start)
  const e = new Date(end)
  return records.value.filter(r => {
    const d = new Date(r.month + '-01')
    return d >= s && d <= e
  })
})

const yoyType = (v) => {
  if (v == null) return 'info'
  if (v > 0) return 'danger'
  if (v < 0) return 'success'
  return 'info'
}

const getSummaries = ({ columns, data }) => {
  return columns.map((col, idx) => {
    if (idx === 0) return '合计'
    const field = col.property
    const fields = ['electric', 'water', 'gas', 'cost']
    if (fields.includes(field)) {
      const total = data.reduce((s, r) => s + (r[field] || 0), 0)
      if (field === 'cost') return '¥' + total.toLocaleString()
      return total.toLocaleString()
    }
    return '-'
  })
}

const resetFilter = () => {
  dateRange.value = []
  ElMessage.info('筛选已重置')
}

const handleExport = () => {
  ElMessage.success('能耗报表导出任务已提交')
}
</script>

<style scoped>
.energy-page { padding: 24px 32px; }

.page-header {
  display: flex; align-items: flex-start; justify-content: space-between;
  margin-bottom: 24px; gap: 16px; flex-wrap: wrap;
}
.header-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-desc { font-size: 13px; color: #8a9a8e; margin: 0; }

.stats-row {
  display: grid; grid-template-columns: repeat(4, 1fr);
  gap: 16px; margin-bottom: 20px;
}
.stat-card {
  background: #fff; border: 1px solid #e8ece9; border-radius: 10px;
  padding: 18px 20px; display: flex; flex-direction: column; gap: 6px;
}
.stat-label { font-size: 12px; color: #8a9a8e; }
.stat-value { font-size: 26px; font-weight: 700; color: #2D4A3E; }
.stat-value .unit { font-size: 14px; font-weight: 400; }

.content-card {
  background: #fff; border: 1px solid #e8ece9; border-radius: 10px; padding: 20px;
}

.toolbar {
  display: flex; align-items: center; justify-content: space-between;
  margin-bottom: 16px; flex-wrap: wrap; gap: 12px;
}
.toolbar-left { display: flex; align-items: center; gap: 10px; flex-wrap: wrap; }

.text-muted { font-size: 12px; color: #a0b0a5; }
</style>

<template>
  <div class="production-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">出品管理 · Production</h2>
        <p class="page-desc">厨房出品跟踪 · 质量把控 · 出品时效</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="handleRefresh">刷新</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">待出品 · Pending</span>
        <span class="stat-value">{{ stats.pending }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">出品中 · Cooking</span>
        <span class="stat-value">{{ stats.cooking }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">已完成 · Done</span>
        <span class="stat-value">{{ stats.done }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">平均时长 · Avg Time</span>
        <span class="stat-value">{{ stats.avgTime }}min</span>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待出品" value="pending" />
            <el-option label="出品中" value="cooking" />
            <el-option label="已完成" value="done" />
          </el-select>
          <el-input v-model="searchKey" placeholder="搜索菜品/桌号" clearable style="width: 220px" />
        </div>
      </div>

      <el-table :data="filteredOrders" stripe style="width: 100%">
        <el-table-column prop="orderNo" label="单号" width="120" />
        <el-table-column prop="tableNo" label="桌号" width="80" />
        <el-table-column prop="dishName" label="菜品" min-width="160" />
        <el-table-column prop="qty" label="数量" width="70" />
        <el-table-column prop="orderTime" label="下单时间" width="120" />
        <el-table-column prop="cookName" label="负责厨师" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" size="small" type="primary" @click="startCook(row)">开始出品</el-button>
            <el-button v-else-if="row.status === 'cooking'" size="small" type="success" @click="finishCook(row)">完成</el-button>
            <span v-else class="text-muted">已完结</span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const filterStatus = ref('')
const searchKey = ref('')

const stats = ref({ pending: 0, cooking: 0, done: 0, avgTime: 0 })

const orders = ref([])

const filteredOrders = computed(() => {
  return orders.value.filter(o => {
    if (filterStatus.value && o.status !== filterStatus.value) return false
    if (searchKey.value) {
      const k = searchKey.value.toLowerCase()
      if (!o.dishName?.toLowerCase().includes(k) && !o.tableNo?.toLowerCase().includes(k)) return false
    }
    return true
  })
})

const statusType = (s) => ({ pending: 'warning', cooking: 'primary', done: 'success' }[s] || 'info')
const statusText = (s) => ({ pending: '待出品', cooking: '出品中', done: '已完成' }[s] || s)

const startCook = (row) => {
  row.status = 'cooking'
  ElMessage.success(`开始出品：${row.dishName}`)
}

const finishCook = (row) => {
  row.status = 'done'
  ElMessage.success(`出品完成：${row.dishName}`)
  updateStats()
}

const updateStats = () => {
  stats.value.pending = orders.value.filter(o => o.status === 'pending').length
  stats.value.cooking = orders.value.filter(o => o.status === 'cooking').length
  stats.value.done = orders.value.filter(o => o.status === 'done').length
}

const handleRefresh = () => {
  ElMessage.info('刷新出品列表')
}

onMounted(() => {
  updateStats()
})
</script>

<style scoped>
.production-page { padding: 24px 32px; }

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
.stat-value { font-size: 28px; font-weight: 700; color: #2D4A3E; }

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

<template>
  <div class="kitchen-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">厨房管理 · Kitchen</h2>
        <p class="page-desc">订单接单 · 制作进度 · 出餐管理 · Order Tracking</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="refresh">刷新 · Refresh</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">待接单 · Pending</div>
        <div class="stat-value stat-pending">{{ stats.pending }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">制作中 · Cooking</div>
        <div class="stat-value stat-cooking">{{ stats.cooking }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待出餐 · Ready</div>
        <div class="stat-value stat-ready">{{ stats.ready }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成 · Done</div>
        <div class="stat-value stat-done">{{ stats.done }}</div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 160px">
            <el-option label="全部 · All" value="" />
            <el-option label="待接单 · Pending" value="待接单" />
            <el-option label="制作中 · Cooking" value="制作中" />
            <el-option label="待出餐 · Ready" value="待出餐" />
            <el-option label="已完成 · Done" value="已完成" />
          </el-select>
          <el-input
            v-model="searchKeyword"
            placeholder="搜索订单号 / 桌号"
            clearable
            style="width: 240px"
          />
        </div>
        <div class="toolbar-right">
          <el-button text @click="clearFilters">清除 · Clear</el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="filteredList" stripe class="data-table" v-loading="loading">
        <el-table-column prop="orderNo" label="订单号 · Order" width="150" />
        <el-table-column prop="tableNo" label="桌号 · Table" width="110" />
        <el-table-column prop="dishName" label="菜品 · Dish" min-width="160" />
        <el-table-column prop="quantity" label="数量 · Qty" width="90" align="center" />
        <el-table-column prop="remark" label="备注 · Remark" min-width="150" show-overflow-tooltip />
        <el-table-column prop="orderTime" label="下单时间 · Time" width="160" />
        <el-table-column prop="status" label="状态 · Status" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small" effect="light">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="chef" label="厨师 · Chef" width="110" />
        <el-table-column label="操作 · Action" width="130" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === '待接单'"
              type="primary"
              size="small"
              @click="acceptOrder(row)"
            >接单</el-button>
            <el-button
              v-else-if="row.status === '制作中'"
              type="warning"
              size="small"
              @click="serveDish(row)"
            >出餐</el-button>
            <el-button
              v-else-if="row.status === '待出餐'"
              type="success"
              size="small"
              @click="completeOrder(row)"
            >完成</el-button>
            <span v-else class="done-text">—</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <span class="total-text">共 {{ filteredList.length }} 单</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const orders = ref([])
const filterStatus = ref('')
const searchKeyword = ref('')

const stats = computed(() => {
  const list = orders.value
  return {
    pending: list.filter(o => o.status === '待接单').length,
    cooking: list.filter(o => o.status === '制作中').length,
    ready: list.filter(o => o.status === '待出餐').length,
    done: list.filter(o => o.status === '已完成').length,
  }
})

const filteredList = computed(() => {
  let list = orders.value
  if (filterStatus.value) list = list.filter(o => o.status === filterStatus.value)
  if (searchKeyword.value) {
    const q = searchKeyword.value.toLowerCase()
    list = list.filter(o =>
      (o.orderNo || '').toLowerCase().includes(q) ||
      (o.tableNo || '').toLowerCase().includes(q)
    )
  }
  return list
})

function statusTag(s) {
  return { '待接单': 'danger', '制作中': 'warning', '待出餐': '', '已完成': 'success' }[s] || 'info'
}

function clearFilters() {
  filterStatus.value = ''
  searchKeyword.value = ''
}

function acceptOrder(row) {
  row.status = '制作中'
  ElMessage.success(`订单 ${row.orderNo} 已接单`)
}

function serveDish(row) {
  row.status = '待出餐'
  ElMessage.success(`订单 ${row.orderNo} 已出餐`)
}

function completeOrder(row) {
  row.status = '已完成'
  ElMessage.success(`订单 ${row.orderNo} 已完成`)
}

async function loadData() {
  loading.value = true
  try {
    // TODO: 接入后端 API
    // const res = await getKitchenOrders()
    // if (res.code === 200) orders.value = res.data || []
    orders.value = []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function refresh() {
  loadData()
  ElMessage.info('已刷新')
}

onMounted(() => { loadData() })
</script>

<style scoped>
.kitchen-page {
  padding: 24px 32px;
  max-width: 1600px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.header-left { display: flex; flex-direction: column; }
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
}
.page-desc {
  font-size: 13px;
  color: #6b7c72;
  margin: 4px 0 0;
}
.header-right { display: flex; gap: 8px; }

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #ffffff;
  border: 1px solid #ebe6dc;
  border-radius: 10px;
  padding: 18px 20px;
  border-top: 3px solid #2D4A3E;
}
.stat-label {
  font-size: 13px;
  color: #6b7c72;
  margin-bottom: 8px;
  font-weight: 500;
}
.stat-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}
.stat-pending { color: #C25555; }
.stat-cooking { color: #C4A35A; }
.stat-ready { color: #2D4A3E; }
.stat-done { color: #4A7C59; }

/* 内容卡片 */
.content-card {
  background: #ffffff;
  border: 1px solid #ebe6dc;
  border-radius: 10px;
  padding: 20px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.toolbar-right { display: flex; align-items: center; gap: 8px; }

/* 表格 */
.data-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}
.done-text {
  color: #b6b6b6;
  font-size: 14px;
}
.table-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.total-text {
  font-size: 13px;
  color: #6b7c72;
}

/* 主题色覆盖 */
:deep(.el-button--primary) {
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4e;
  --el-button-hover-border-color: #3a5e4e;
  --el-button-active-bg-color: #243d33;
  --el-button-active-border-color: #243d33;
}
:deep(.el-button--warning) {
  --el-button-bg-color: #C4A35A;
  --el-button-border-color: #C4A35A;
  --el-button-hover-bg-color: #d1b36c;
  --el-button-hover-border-color: #d1b36c;
}
:deep(.el-button--success) {
  --el-button-bg-color: #4A7C59;
  --el-button-border-color: #4A7C59;
  --el-button-hover-bg-color: #5a8d6a;
  --el-button-hover-border-color: #5a8d6a;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .kitchen-page { padding: 16px; }
}
</style>

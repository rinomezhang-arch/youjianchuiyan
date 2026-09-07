<template>
  <div class="page">
    <div class="page-header">
      <h2>盘点 · Stock Take</h2>
      <p class="page-desc">库存盘点 · 差异处理 · 盘点报告</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索原料" class="search-box" clearable />
      </div>
      <div class="toolbar-right">
        <el-button v-if="!stockTaking" type="success" @click="startStockTake">+ 开始盘点</el-button>
        <template v-else>
          <el-button type="primary" @click="submitStockTake" :loading="submitting">提交盘点</el-button>
          <el-button @click="cancelStockTake">取消</el-button>
        </template>
        <el-button @click="fetchHistory">历史盘点单</el-button>
        <el-button @click="exportData">导出</el-button>
      </div>
    </div>
    <el-table :data="filteredList" stripe v-loading="loading" max-height="calc(100vh - 320px)">
      <el-table-column prop="ingredientId" label="编码" width="100" />
      <el-table-column prop="ingredientName" label="原料" width="160" />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="systemQuantity" label="系统库存" width="100" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="实盘数量" width="120">
        <template #default="{ row }">
          <el-input-number
            v-if="stockTaking"
            v-model="row.actualQuantity"
            :min="0"
            :precision="3"
            size="small"
            controls-position="right"
            style="width:100%"
            @change="updateDiff(row)"
          />
          <span v-else>{{ row.actualQuantity != null ? row.actualQuantity : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="差异" width="100">
        <template #default="{ row }">
          <span v-if="row.diffQty != null" :style="{ color: row.diffQty > 0 ? '#389e0d' : row.diffQty < 0 ? '#dc2626' : '#666' }">
            {{ row.diffQty > 0 ? '+' : '' }}{{ row.diffQty }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="差异金额" width="110">
        <template #default="{ row }">
          <span v-if="row.diffAmount != null" :style="{ color: row.diffAmount > 0 ? '#389e0d' : row.diffAmount < 0 ? '#dc2626' : '#666' }">
            ¥{{ row.diffAmount > 0 ? '+' : '' }}{{ row.diffAmount.toFixed(2) }}
          </span>
          <span v-else>-</span>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="stockTaking" class="summary-bar">
      <span>盘点总项: <strong>{{ list.length }}</strong> 项</span>
      <span>差异项: <strong>{{ diffCount }}</strong> 项</span>
      <span>差异金额: <strong :style="{ color: totalDiffAmount >= 0 ? '#389e0d' : '#dc2626' }">¥{{ totalDiffAmount >= 0 ? '+' : '' }}{{ totalDiffAmount.toFixed(2) }}</strong></span>
    </div>

    <!-- 历史盘点单 -->
    <el-dialog v-model="showHistory" title="历史盘点单" width="min(700px, 94vw)">
      <el-table :data="historyList" v-loading="historyLoading" max-height="400">
        <el-table-column prop="takeNo" label="盘点单号" width="150" />
        <el-table-column prop="takeDate" label="盘点日期" width="110" />
        <el-table-column prop="totalItems" label="总项数" width="80" />
        <el-table-column prop="totalDiffItems" label="差异项" width="80" />
        <el-table-column label="差异金额" width="100">
          <template #default="{ row }">¥{{ (row.totalDiffAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="盘点人" width="90" />
        <el-table-column label="查看" width="80" fixed="right">
          <template #default="{ row }"><el-button link type="primary" @click="viewDetails(row)">明细</el-button></template>
        </el-table-column>
      </el-table>
    </el-dialog>
    <el-dialog v-model="showDetails" :title="detailTitle" width="min(860px, 94vw)">
      <el-table :data="detailList" v-loading="detailLoading" max-height="480">
        <el-table-column prop="ingredientName" label="原料" min-width="130" />
        <el-table-column prop="unit" label="单位" width="70" />
        <el-table-column prop="systemQuantity" label="账面数量" width="100" />
        <el-table-column prop="actualQuantity" label="实盘数量" width="100" />
        <el-table-column prop="diffQuantity" label="差异数量" width="100" />
        <el-table-column label="差异金额" width="110">
          <template #default="{ row }">¥{{ Number(row.diffAmount || 0).toFixed(2) }}</template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.storeId)

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const keyword = ref('')
const stockTaking = ref(false)
const showHistory = ref(false)
const historyLoading = ref(false)
const historyList = ref([])
const showDetails = ref(false)
const detailLoading = ref(false)
const detailList = ref([])
const detailTitle = ref('盘点明细')
let detailRequest = 0

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(i => (i.ingredientName || '').toLowerCase().includes(kw))
})

const diffCount = computed(() => list.value.filter(i => i.diffQty != null && i.diffQty !== 0).length)
const totalDiffAmount = computed(() => list.value.reduce((sum, i) => sum + (i.diffAmount || 0), 0))

// 盘点清单：真实原料 + 真实系统库存，之前这个接口根本不存在，盘点页面从未真正打开过要盘的原料
async function fetchData() {
  const storeId = currentStoreId.value
  loading.value = true
  try {
    const res = await request.get('/stock-takes/count-sheet', { params: { storeId } })
    if (storeId !== currentStoreId.value) return
    list.value = (res.data || []).map(i => ({ ...i, actualQuantity: null, diffQty: null, diffAmount: null }))
  } catch (e) {
    console.error('获取盘点清单失败', e)
    ElMessage.error('获取盘点清单失败')
  } finally {
    if (storeId === currentStoreId.value) loading.value = false
  }
}

function updateDiff(row) {
  if (row.actualQuantity == null) { row.diffQty = null; row.diffAmount = null; return }
  row.diffQty = Number((row.actualQuantity - (row.systemQuantity || 0)).toFixed(3))
  row.diffAmount = Number((row.diffQty * (row.unitPrice || 0)).toFixed(2))
}

function startStockTake() {
  stockTaking.value = true
  list.value.forEach(row => { row.actualQuantity = row.systemQuantity })
  ElMessage.info('已按系统库存预填，请核对并修改实际盘点数量')
}

async function submitStockTake() {
  if (submitting.value || !list.value.length) return
  const storeId = currentStoreId.value
  const unfilled = list.value.filter(i => i.actualQuantity == null)
  if (unfilled.length > 0) {
    ElMessage.warning(`还有 ${unfilled.length} 项没有填写实盘数量`)
    return
  }
  try {
    await ElMessageBox.confirm(
      `确认提交本次盘点？共 ${list.value.length} 项，差异 ${diffCount.value} 项，差异金额 ¥${totalDiffAmount.value.toFixed(2)}。`,
      '确认提交',
      { confirmButtonText: '确认提交', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }
  if (submitting.value || !stockTaking.value) return
  if (storeId !== currentStoreId.value) {
    ElMessage.warning('门店已切换，请重新核对盘点单')
    return
  }
  submitting.value = true
  try {
    const saved = await request.post('/stock-takes', {
      storeId,
      takeType: 'monthly',
      takeDate: localDate(),
      items: list.value.map(i => ({ ingredientId: i.ingredientId, actualQuantity: i.actualQuantity }))
    })
    ElMessage.success(`盘点已保存：${saved.data?.takeNo || '请在历史盘点单查看'}`)
    if (storeId !== currentStoreId.value) return
    stockTaking.value = false
    fetchData()
  } catch (e) {
    console.error('提交盘点失败', e)
    ElMessage.error(e.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function cancelStockTake() {
  stockTaking.value = false
  fetchData()
}

async function fetchHistory() {
  const storeId = currentStoreId.value
  showHistory.value = true
  historyLoading.value = true
  try {
    const res = await request.get('/stock-takes', { params: { storeId } })
    if (storeId !== currentStoreId.value) return
    historyList.value = res.data || []
  } catch (e) {
    console.error('获取历史盘点单失败', e)
  } finally {
    historyLoading.value = false
  }
}

function localDate() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')}`
}

async function viewDetails(row) {
  const version = ++detailRequest
  const storeId = currentStoreId.value
  detailTitle.value = `盘点明细 · ${row.takeNo}`
  detailList.value = []
  showDetails.value = true
  detailLoading.value = true
  try {
    const res = await request.get(`/stock-takes/${row.takeId}`)
    if (version !== detailRequest || storeId !== currentStoreId.value) return
    detailList.value = res.data?.details || []
  } catch {
    if (version === detailRequest) ElMessage.error('盘点明细读取失败，请重试')
  } finally {
    if (version === detailRequest) detailLoading.value = false
  }
}

function exportData() {
  if (list.value.length === 0) return
  const header = ['编码', '原料', '分类', '系统库存', '单位', '实盘数量', '差异', '差异金额']
  const rows = list.value.map(i => [
    i.ingredientId, i.ingredientName, i.category, i.systemQuantity, i.unit,
    i.actualQuantity ?? '', i.diffQty ?? '', i.diffAmount ?? ''
  ])
  const csv = [header, ...rows]
    .map(row => row.map(v => `"${String(v ?? '').replace(/"/g, '""')}"`).join(','))
    .join('\r\n')
  const blob = new Blob(['﻿' + csv], { type: 'text/csv;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `盘点表_${localDate()}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => { fetchData() })
watch(currentStoreId, () => {
  detailRequest++
  showDetails.value = false
  showHistory.value = false
  detailList.value = []
  stockTaking.value = false
  list.value = []
  historyList.value = []
  fetchData()
})
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; }
.search-box { width:200px; }
.summary-bar { margin-top:16px; padding:12px 16px; border:1px solid #e5e7eb; background:#fafafa; display:flex; gap:30px; font-size:14px; border-radius:4px; }
:deep(.el-table) { width:100%; }
@media (max-width: 640px) {
  .page-header, .toolbar, .toolbar-left, .toolbar-right, .summary-bar { flex-wrap:wrap; gap:10px; }
  .toolbar { align-items:flex-start; }
  .summary-bar { line-height:1.8; }
  .search-box { width:100%; }
}
</style>

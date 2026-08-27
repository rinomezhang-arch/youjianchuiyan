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
            :precision="2"
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
    <el-dialog v-model="showHistory" title="历史盘点单" width="700px">
      <el-table :data="historyList" v-loading="historyLoading" max-height="400">
        <el-table-column prop="takeNo" label="盘点单号" width="150" />
        <el-table-column prop="takeDate" label="盘点日期" width="110" />
        <el-table-column prop="totalItems" label="总项数" width="80" />
        <el-table-column prop="totalDiffItems" label="差异项" width="80" />
        <el-table-column label="差异金额" width="100">
          <template #default="{ row }">¥{{ (row.totalDiffAmount || 0).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="operatorName" label="盘点人" width="90" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)

const loading = ref(false)
const submitting = ref(false)
const list = ref([])
const keyword = ref('')
const stockTaking = ref(false)
const showHistory = ref(false)
const historyLoading = ref(false)
const historyList = ref([])

const filteredList = computed(() => {
  if (!keyword.value) return list.value
  const kw = keyword.value.toLowerCase()
  return list.value.filter(i => (i.ingredientName || '').toLowerCase().includes(kw))
})

const diffCount = computed(() => list.value.filter(i => i.diffQty != null && i.diffQty !== 0).length)
const totalDiffAmount = computed(() => list.value.reduce((sum, i) => sum + (i.diffAmount || 0), 0))

// 盘点清单：真实原料 + 真实系统库存，之前这个接口根本不存在，盘点页面从未真正打开过要盘的原料
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/stock-takes/count-sheet', { params: { storeId: currentStoreId.value } })
    list.value = (res.data || []).map(i => ({ ...i, actualQuantity: null, diffQty: null, diffAmount: null }))
  } catch (e) {
    console.error('获取盘点清单失败', e)
    ElMessage.error('获取盘点清单失败')
  } finally {
    loading.value = false
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
  submitting.value = true
  try {
    await request.post('/stock-takes', {
      storeId: currentStoreId.value,
      takeType: 'monthly',
      takeDate: new Date().toISOString().slice(0, 10),
      items: list.value.map(i => ({ ingredientId: i.ingredientId, actualQuantity: i.actualQuantity }))
    })
    ElMessage.success('盘点提交成功')
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
  showHistory.value = true
  historyLoading.value = true
  try {
    const res = await request.get('/stock-takes', { params: { storeId: currentStoreId.value } })
    historyList.value = res.data || []
  } catch (e) {
    console.error('获取历史盘点单失败', e)
  } finally {
    historyLoading.value = false
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
  a.download = `盘点表_${new Date().toISOString().slice(0, 10)}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

onMounted(() => { fetchData() })
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
</style>

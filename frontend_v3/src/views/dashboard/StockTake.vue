<template>
  <div class="page">
    <div class="page-header">
      <h2>盘点 · Stock Take</h2>
      <p class="page-desc">库存盘点 · 差异处理 · 盘点报告</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="warehouseId" placeholder="选择仓库" style="width:200px">
          <el-option v-for="w in warehouses" :key="w.warehouse_id" :label="w.warehouse_name" :value="w.warehouse_id" />
        </el-select>
        <el-input v-model="keyword" placeholder="搜索原料" class="search-box" clearable @keyup.enter="fetchData" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" @click="startStockTake">+ 开始盘点</el-button>
        <el-button type="warning" @click="viewReport">盘点报告</el-button>
        <el-button type="danger" @click="exportData">导出</el-button>
      </div>
    </div>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="material_id" label="编码" width="120" />
      <el-table-column prop="material_name" label="原料" width="180" />
      <el-table-column prop="warehouse_name" label="仓库" width="100" />
      <el-table-column prop="system_qty" label="系统库存" width="100" />
      <el-table-column prop="actual_qty" label="实际盘点" width="100">
        <template #default="{ row }">
          <el-input v-if="stockTaking" v-model.number="row.actual_qty" size="small" @change="updateActualQty(row)" />
          <span v-else>{{ row.actual_qty || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column label="差异" width="100">
        <template #default="{ row }">
          <span :style="{ color: row.diff_qty > 0 ? '#389e0d' : row.diff_qty < 0 ? '#dc2626' : '#666' }">
            {{ row.diff_qty > 0 ? '+' : '' }}{{ row.diff_qty }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="diff_amount" label="差异金额" width="100">
        <template #default="{ row }">
          <span :style="{ color: row.diff_amount > 0 ? '#389e0d' : row.diff_amount < 0 ? '#dc2626' : '#666' }">
            ¥{{ row.diff_amount > 0 ? '+' : '' }}{{ row.diff_amount }}
          </span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status===0" type="info" size="small">未盘点</el-tag>
          <el-tag v-else-if="row.status===1" type="warning" size="small">盘点中</el-tag>
          <el-tag v-else-if="row.status===2 && row.diff_qty !== 0" type="danger" size="small">有差异</el-tag>
          <el-tag v-else type="success" size="small">已完成</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="stockTaking || summary" style="margin-top:16px; padding:12px; border:1px solid #e5e7eb; background:#fafafa">
      <div style="display:flex; gap:30px; font-size:14px">
        <span>盘点总项: <strong>{{ summary?.totalCount || 0 }}</strong> 项</span>
        <span>已盘点: <strong>{{ summary?.countedCount || 0 }}</strong> 项</span>
        <span>差异项: <strong>{{ summary?.diffCount || 0 }}</strong> 项</span>
        <span>差异金额: <strong :style="{ color: summary?.diffAmount >= 0 ? '#389e0d' : '#dc2626' }">¥{{ summary?.diffAmount >= 0 ? '+' : '' }}{{ summary?.diffAmount }}</strong></span>
      </div>
      <div v-if="stockTaking" style="margin-top:12px">
        <el-button type="primary" @click="submitStockTake">提交盘点</el-button>
        <el-button @click="cancelStockTake">取消盘点</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false); const list = ref([]); const warehouseId = ref(''); const keyword = ref('')
const warehouses = ref([]); const summary = ref(null); const stockTaking = ref(false)

async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/api/stock-takes', { params: { warehouseId: warehouseId.value, keyword: keyword.value } })
    const d = res.data || res
    list.value = d?.list || d?.data || []
    summary.value = d?.summary || null
  } catch (e) {
    console.error('获取盘点数据失败', e)
    ElMessage.error('获取盘点数据失败')
  } finally { loading.value = false }
}

async function fetchWarehouses() {
  try {
    const res = await request.get('/api/inventory/stock-transfer/', { params: { type: 'warehouses' } })
    const d = res.data || res
    warehouses.value = d?.list || d?.data || []
  } catch (e) {
    console.error('获取仓库列表失败', e)
  }
}

function updateActualQty(row) {
  row.diff_qty = (row.actual_qty || 0) - (row.system_qty || 0)
  row.diff_amount = row.diff_qty * (row.avg_cost || 0)
  row.status = 1
}

function startStockTake() {
  stockTaking.value = true
  ElMessage.info('请在"实际盘点"列输入实际库存数量')
}

function submitStockTake() {
  ElMessageBox.confirm('确认提交盘点结果?', '提示', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      const res = await request.post('/api/stock-takes', {
        warehouseId: warehouseId.value,
        items: list.value.filter(i => i.status === 1)
      })
      const d = res.data || res
      if (d.code === 200) { ElMessage.success('盘点提交成功'); stockTaking.value = false; fetchData() }
      else ElMessage.error(d.message || '提交失败')
    } catch (e) { ElMessage.error('提交失败') }
  })
}

function cancelStockTake() {
  stockTaking.value = false
  fetchData()
}

function viewReport() {
  ElMessage.info('盘点报告功能开发中')
}

function exportData() {
  ElMessage.info('导出功能开发中')
}

onMounted(() => { fetchWarehouses(); fetchData() })
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:12px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:0; }
.toolbar { display:flex; justify-content:space-between; align-items:center; margin-bottom:12px; }
.toolbar-left, .toolbar-right { display:flex; gap:8px; align-items:center; }
.search-box { width:200px; }
:deep(.el-table) { width:100%; }
</style>

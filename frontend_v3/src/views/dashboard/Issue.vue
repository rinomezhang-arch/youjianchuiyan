<template>
  <div class="page">
    <div class="page-header">
      <h2>领用出库 · Issue</h2>
      <p class="page-desc">原料领用 · 出库登记 · 库存扣减</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索单号/原料" class="search-box" clearable @keyup.enter="fetchData" />
        <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期" />
        <el-button type="primary" @click="fetchData">查询</el-button>
      </div>
      <div class="toolbar-right">
        <el-button type="success" @click="showAddDialog = true">+ 领用</el-button>
        <el-button type="warning" @click="exportData">导出</el-button>
      </div>
    </div>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="bill_no" label="出库单号" width="140" />
      <el-table-column prop="material_name" label="原料" width="150" />
      <el-table-column prop="warehouse_name" label="仓库" width="100" />
      <el-table-column prop="quantity" label="数量" width="100" />
      <el-table-column prop="unit" label="单位" width="70" />
      <el-table-column prop="reason" label="领用原因" width="120" />
      <el-table-column prop="applicant" label="申请人" width="100" />
      <el-table-column prop="issue_date" label="日期" width="110">
        <template #default="{ row }">{{ formatDate(row.issue_date) }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.status===0" type="warning" size="small">待审核</el-tag>
          <el-tag v-else type="success" size="small">已出库</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button v-if="row.status===0" size="small" type="success" @click="audit(row)">审核</el-button>
          <el-button size="small" @click="view(row)">查看</el-button>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false); const list = ref([]); const keyword = ref(''); const dateRange = ref([])
const showAddDialog = ref(false)

function formatDate(date) { if (!date) return '-'; return String(date).slice(0, 10) }

async function fetchData() {
  loading.value = true
  try {
    const params = { keyword: keyword.value }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const { data } = await request({ url: '/api/inventory/issues', method: 'get', params })
    list.value = data || []
  } catch (e) {
    console.error('fetchData error:', e)
    ElMessage.error('加载领用出库数据失败')
  } finally {
    loading.value = false
  }
}

function view(row) {
  ElMessage.info(`查看出库单: ${row.bill_no}`)
}

function audit(row) {
  ElMessageBox.confirm('确认出库?', '提示', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(async () => {
    try {
      const { data } = await request({ url: `/api/inventory/issues/${row.id}/audit`, method: 'put' })
      ElMessage.success('出库成功')
      fetchData()
    } catch (e) {
      console.error('audit error:', e)
      ElMessage.error('出库失败')
    }
  }).catch(() => {})
}

function exportData() {
  try {
    const params = { keyword: keyword.value }
    if (dateRange.value && dateRange.value.length === 2) {
      params.startDate = dateRange.value[0]
      params.endDate = dateRange.value[1]
    }
    const query = new URLSearchParams(params).toString()
    const url = `/api/inventory/issues/export${query ? '?' + query : ''}`
    window.open(url, '_blank')
  } catch (e) {
    console.error('exportData error:', e)
    ElMessage.error('导出失败')
  }
}

onMounted(fetchData)
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

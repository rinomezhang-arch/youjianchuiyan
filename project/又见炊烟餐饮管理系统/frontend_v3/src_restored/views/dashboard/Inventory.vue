<template>
  <div class="page">
    <div class="page-header">
      <h2>库存管理 · Inventory Management</h2>
      <p class="page-desc">原料库存 · 出入库 · 盘点 · 预警</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索原料" class="search-box" clearable @keyup.enter="fetchData" />
      </div>
      <div class="toolbar-right">
        <el-button type="warning" plain @click="fetchWarnings">⚠ 查看预警</el-button>
      </div>
    </div>
    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="material_id" label="编码" width="100" />
      <el-table-column prop="material_name" label="原料名称" min-width="180" />
      <el-table-column prop="category_id" label="分类" width="100" />
      <el-table-column prop="current_qty" label="当前库存" width="100" />
      <el-table-column prop="unit" label="单位" width="60" />
      <el-table-column prop="safety_qty" label="预警阈值" width="100" />
      <el-table-column prop="avg_cost" label="平均成本" width="100" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag v-if="row.current_qty <= row.safety_qty" type="danger" size="small">低库存</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getInventory, getInventoryWarnings } from '@/api/booking'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false); const list = ref([]); const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getInventory({ keyword: keyword.value })
    if (res.code === 200) list.value = res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

async function fetchWarnings() {
  const res = await getInventoryWarnings()
  if (res.code === 200) {
    const warns = res.data || []
    if (warns.length === 0) ElMessage.info('暂无低库存预警')
    else ElMessageBox.alert(warns.map(w => `⚠ ${w.material_name}: 当前 ${w.current_qty} ${w.unit}，阈值 ${w.safety_qty}`).join('\n'), '低库存预警', { confirmButtonText: '知道了' })
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
:deep(.el-table) { width:100%; }
</style>

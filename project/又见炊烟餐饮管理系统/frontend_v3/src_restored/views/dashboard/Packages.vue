<template>
  <div class="page">
    <div class="page-header">
      
      <h2>套餐管理 · Package Management</h2>
      <p class="page-desc">套餐配置 · 菜品搭配 · 成本率</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-right">
        <el-button type="primary" @click="openAdd">+ 新建套餐</el-button>
      </div>
    </div>
    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="packageId" label="编号" width="120" />
      <el-table-column prop="packageName" label="套餐名称" width="150" />
      <el-table-column prop="dishCount" label="菜品数" width="60" />
      <el-table-column prop="suggestGuests" label="建议人数" width="80" />
      <el-table-column prop="packageTotalPrice" label="售价" width="80" />
      <el-table-column prop="packageCostPrice" label="成本" width="80" />
      <el-table-column prop="costRate" label="成本率" width="70">
        <template #default="{ row }">{{ (row.costRate || 0).toFixed(1) }}%</template>
      </el-table-column>
      <el-table-column prop="occasionType" label="适用场合" width="80" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="editRow(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination background layout="prev,pager,next" :total="total" :page-size="20" @current-change="fetchData" class="mt-4" />

    <el-dialog v-model="showDialog" :title="editing ? '编辑套餐' : '新建套餐'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="套餐名称" required><el-input v-model="form.packageName" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="售价"><el-input-number v-model="form.packageTotalPrice" :precision="2" :min="0" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="建议人数"><el-input-number v-model="form.suggestGuests" :min="1" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="适用场合"><el-input v-model="form.occasionType" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="savePackage">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getPackages, createPackage, updatePackage } from '@/api/package'
import { ElMessage } from 'element-plus'

const loading = ref(false); const list = ref([]); const total = ref(0)
const showDialog = ref(false); const editing = ref(false)
const form = ref({ packageName: '', packageTotalPrice: 0, suggestGuests: 10, occasionType: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getPackages()
    if (res.code === 200) list.value = res.data?.content || res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function openAdd() { editing.value = false; form.value = { packageName: '', packageTotalPrice: 0, suggestGuests: 10, occasionType: '' }; showDialog.value = true }
function editRow(row) { editing.value = true; form.value = { ...row }; showDialog.value = true }
async function savePackage() {
  const res = editing.value ? await updatePackage(form.value.packageId, form.value) : await createPackage(form.value)
  if (res.code === 200) { ElMessage.success('保存成功'); showDialog.value = false; fetchData() }
}
onMounted(fetchData)
</script>

<style scoped>
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
.back-btn:hover { background:#fff; color:#1e293b; border-color:#94a3b8; }
</style>



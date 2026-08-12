<template>
  <div class="page">
    <div class="page-header">
      
      <h2>供应商管理 · Supplier Management</h2>
      <p class="page-desc">供应商信息 · 联系方式 · 供货管理</p>
    </div>
    <div class="toolbar">
      <div class="toolbar-right">
        <el-button type="primary" @click="openAdd">+ 新增供应商</el-button>
      </div>
    </div>
    <el-table :data="list" stripe class="data-table" v-loading="loading">
      <el-table-column prop="supplierName" label="供应商名称" width="160" />
      <el-table-column prop="contactPerson" label="联系人" width="80" />
      <el-table-column prop="contactPhone" label="联系电话" width="130" />
      <el-table-column prop="mainProducts" label="主要供应品" min-width="150" />
      <el-table-column prop="wechatAccount" label="微信" width="120" />
      <el-table-column prop="alipayAccount" label="支付宝" width="120" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{ row }">
          <el-button text size="small" @click="editRow(row)">编辑</el-button>
          <el-popconfirm title="确定删除?" @confirm="deleteSupplier(row)">
            <template #reference>
              <el-button text size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editing ? '编辑供应商' : '新增供应商'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="供应商名称" required><el-input v-model="form.supplierName" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contactPerson" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.contactPhone" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="主要供应品"><el-input v-model="form.mainProducts" /></el-form-item>
        <el-row :gutter="16">
          <el-col :span="8"><el-form-item label="微信"><el-input v-model="form.wechatAccount" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="支付宝"><el-input v-model="form.alipayAccount" /></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="银行账号"><el-input v-model="form.bankAccount" /></el-form-item></el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSupplier">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getSuppliers, createSupplier, updateSupplier, deleteSupplier as delSupplier } from '@/api/booking'
import { ElMessage } from 'element-plus'

const loading = ref(false); const list = ref([])
const showDialog = ref(false); const editing = ref(false)
const form = ref({ supplierName: '', contactPerson: '', contactPhone: '', mainProducts: '', wechatAccount: '', alipayAccount: '', bankAccount: '' })

async function fetchData() {
  loading.value = true
  try {
    const res = await getSuppliers()
    if (res.code === 200) list.value = res.data?.content || res.data || []
  } catch (e) { console.error(e) } finally { loading.value = false }
}

function openAdd() { editing.value = false; form.value = { supplierName: '', contactPerson: '', contactPhone: '', mainProducts: '', wechatAccount: '', alipayAccount: '', bankAccount: '' }; showDialog.value = true }
function editRow(row) { editing.value = true; form.value = { ...row }; showDialog.value = true }
async function saveSupplier() {
  const res = editing.value ? await updateSupplier(form.value.supplierId, form.value) : await createSupplier(form.value)
  if (res.code === 200) { ElMessage.success('保存成功'); showDialog.value = false; fetchData() }
}
async function deleteSupplier(row) {
  const res = await delSupplier(row.supplierId)
  if (res.code === 200) { ElMessage.success('已删除'); fetchData() }
}
onMounted(fetchData)
</script>

<style scoped>
.page { width:100%; }
.page-header { display:flex; align-items:center; gap:12px; margin-bottom:16px; }
.page-header h2 { font-size:18px; font-weight:600; margin:0; }
.page-desc { font-size:13px; color:#64748b; margin:2px 0 0; }
.back-btn:hover { background:#fff; color:#1e293b; border-color:#94a3b8; }
:deep(.el-table) { width:100%; }
</style>



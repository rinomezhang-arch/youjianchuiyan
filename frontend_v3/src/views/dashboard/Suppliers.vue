<template>
  <div class="suppliers-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">供应商 · Suppliers</h2>
        <p class="page-desc">供应商信息 · 联系方式 · 供货管理</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">供应商总数</div>
        <div class="stat-value">{{ stats.total }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">合作中</div>
        <div class="stat-value">{{ stats.active }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已停用</div>
        <div class="stat-value">{{ stats.disabled }}</div>
      </div>
      <div class="stat-card stat-card--accent">
        <div class="stat-label">本月新增</div>
        <div class="stat-value">{{ stats.monthNew }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filters.status" placeholder="状态" style="width:140px" clearable>
            <el-option label="全部" value="" />
            <el-option label="合作中" value="active" />
            <el-option label="已停用" value="disabled" />
          </el-select>
          <el-select v-model="filters.category" placeholder="品类" style="width:160px" clearable>
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
          <el-input v-model="filters.keyword" placeholder="搜索名称/电话" style="width:220px" clearable />
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button @click="exportData">导出</el-button>
          <el-button type="primary" @click="openAdd">新增供应商</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无供应商记录">
        <el-table-column prop="supplierNo" label="编号" width="120" />
        <el-table-column prop="name" label="供应商名称" min-width="180" />
        <el-table-column prop="contact" label="联系人" width="100" />
        <el-table-column prop="phone" label="电话" width="140" />
        <el-table-column prop="category" label="品类" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.category }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="settlement" label="结算方式" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'active'" type="success" size="small">合作中</el-tag>
            <el-tag v-else type="danger" size="small">已停用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="editRow(row)">编辑</el-button>
            <el-button v-if="row.status === 'active'" size="small" type="warning" @click="toggleStatus(row)">停用</el-button>
            <el-button v-else size="small" type="success" @click="toggleStatus(row)">启用</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showDialog" :title="editing ? '编辑供应商' : '新增供应商'" width="640px" @close="resetForm">
      <el-form :model="form" label-width="100px">
        <el-form-item label="供应商名称" required>
          <el-input v-model="form.name" placeholder="请输入供应商名称" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="联系人" required>
              <el-input v-model="form.contact" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" required>
              <el-input v-model="form.phone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="地址">
          <el-input v-model="form.address" placeholder="请输入供应商地址" />
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="品类">
              <el-select v-model="form.category" filterable placeholder="请选择品类" style="width:100%">
                <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结算方式">
              <el-select v-model="form.settlement" placeholder="请选择结算方式" style="width:100%">
                <el-option label="月结" value="月结" />
                <el-option label="现结" value="现结" />
                <el-option label="周结" value="周结" />
                <el-option label="预付" value="预付" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSupplier">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const categoryOptions = ref(['蔬菜', '肉类', '水产', '粮油', '调料', '酒水', '干货', '禽蛋', '其他'])

const filters = reactive({
  status: '',
  category: '',
  keyword: ''
})

const stats = computed(() => ({
  total: list.value.length,
  active: list.value.filter(s => s.status === 'active').length,
  disabled: list.value.filter(s => s.status === 'disabled').length,
  monthNew: 0
}))

const filteredList = computed(() => {
  return list.value.filter(row => {
    if (filters.status && row.status !== filters.status) return false
    if (filters.category && row.category !== filters.category) return false
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!String(row.name || '').toLowerCase().includes(kw) &&
          !String(row.phone || '').toLowerCase().includes(kw)) return false
    }
    return true
  })
})

function resetFilters() {
  filters.status = ''
  filters.category = ''
  filters.keyword = ''
}

const showDialog = ref(false)
const editing = ref(false)
const form = ref(emptyForm())

function emptyForm() {
  return {
    supplierNo: '',
    name: '',
    contact: '',
    phone: '',
    address: '',
    category: '',
    settlement: '月结',
    remark: '',
    status: 'active'
  }
}

function openAdd() {
  editing.value = false
  form.value = emptyForm()
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  form.value = { ...row }
  showDialog.value = true
}

function resetForm() {
  form.value = emptyForm()
}

function saveSupplier() {
  if (!form.value.name) { ElMessage.warning('请输入供应商名称'); return }
  if (!form.value.contact) { ElMessage.warning('请输入联系人'); return }
  if (!form.value.phone) { ElMessage.warning('请输入联系电话'); return }
  ElMessage.success(editing.value ? '供应商已更新（待接入API）' : '供应商已新增（待接入API）')
  showDialog.value = false
}

function toggleStatus(row) {
  const action = row.status === 'active' ? '停用' : '启用'
  ElMessageBox.confirm(`确认${action}供应商 "${row.name}" ?`, '状态确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    row.status = row.status === 'active' ? 'disabled' : 'active'
    ElMessage.success(`已${action}`)
  }).catch(() => {})
}

function exportData() {
  ElMessage.info('导出功能待接入')
}
</script>

<style scoped>
.suppliers-page {
  padding: 24px 32px;
  background: #FAF8F5;
  min-height: 100%;
  color: #1a2f23;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.header-left { display: flex; flex-direction: column; gap: 4px; }
.page-title { font-size: 22px; font-weight: 700; color: #2D4A3E; margin: 0; }
.page-desc { font-size: 13px; color: #6b7c70; margin: 0; }
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  border: 1px solid #ece7df;
  border-left: 4px solid #2D4A3E;
}
.stat-card--accent { border-left-color: #C4A35A; }
.stat-label { font-size: 13px; color: #6b7c70; margin-bottom: 8px; }
.stat-value { font-size: 26px; font-weight: 700; color: #2D4A3E; }
.stat-card--accent .stat-value { color: #C4A35A; }
.content-card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  border: 1px solid #ece7df;
}
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
  flex-wrap: wrap;
}
.toolbar-left, .toolbar-right {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
}
:deep(.el-table) { width: 100%; }
:deep(.el-button--primary) {
  background: #2D4A3E;
  border-color: #2D4A3E;
}
:deep(.el-button--primary:hover) {
  background: #3a5e4f;
  border-color: #3a5e4f;
}
</style>

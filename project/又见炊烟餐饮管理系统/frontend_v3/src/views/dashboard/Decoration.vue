<template>
  <div class="decoration-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">装修管理 · Decoration Management</h2>
        <p class="page-desc">装修改造项目立项 · 进度跟踪 · 预算与审批管理</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="openAddDialog">新建项目</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">项目总数 · Total</span>
        <span class="stat-value">{{ stats.total }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">进行中 · Active</span>
        <span class="stat-value" style="color:#C4A35A">{{ stats.active }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">待审批 · Pending</span>
        <span class="stat-value" style="color:#C0392B">{{ stats.pending }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">已完成 · Done</span>
        <span class="stat-value" style="color:#4A7C59">{{ stats.done }}</span>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待审批" value="pending" />
            <el-option label="进行中" value="active" />
            <el-option label="已完成" value="done" />
          </el-select>
          <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 140px">
            <el-option label="装修" value="装修" />
            <el-option label="改造" value="改造" />
            <el-option label="维修" value="维修" />
          </el-select>
          <el-input v-model="searchKey" placeholder="搜索项目名称/负责人" clearable style="width: 240px" />
        </div>
      </div>

      <el-table :data="filteredProjects" stripe style="width: 100%">
        <el-table-column prop="name" label="项目名称" min-width="180" />
        <el-table-column prop="type" label="类型" width="90">
          <template #default="{ row }">
            <el-tag type="info" effect="plain">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="manager" label="负责人" width="100" />
        <el-table-column label="预算" width="120">
          <template #default="{ row }">¥{{ (row.budget || 0).toLocaleString() }}</template>
        </el-table-column>
        <el-table-column label="进度" width="180">
          <template #default="{ row }">
            <div class="progress-cell">
              <el-progress :percentage="row.progress || 0" :stroke-width="8" :color="'#2D4A3E'" />
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" link @click="deleteProject(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑项目' : '新建装修项目'" width="560px">
      <el-form :model="form" label-width="100px" label-position="right">
        <el-form-item label="项目名称">
          <el-input v-model="form.name" placeholder="例：大厅吊顶翻新" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" placeholder="请选择类型" style="width: 100%">
            <el-option label="装修" value="装修" />
            <el-option label="改造" value="改造" />
            <el-option label="维修" value="维修" />
          </el-select>
        </el-form-item>
        <el-form-item label="负责人">
          <el-input v-model="form.manager" placeholder="负责人姓名" />
        </el-form-item>
        <el-form-item label="预算（元）">
          <el-input v-model.number="form.budget" type="number" placeholder="0" />
        </el-form-item>
        <el-form-item label="开工日期">
          <el-date-picker v-model="form.startDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="预计完工">
          <el-date-picker v-model="form.endDate" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="项目描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="项目详细说明..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProject">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const filterStatus = ref('')
const filterType = ref('')
const searchKey = ref('')
const dialogVisible = ref(false)
const editingId = ref(null)

const projects = ref([])

const form = ref({ name: '', type: '装修', manager: '', budget: 0, startDate: '', endDate: '', description: '' })

const stats = computed(() => ({
  total: projects.value.length,
  active: projects.value.filter(p => p.status === 'active').length,
  pending: projects.value.filter(p => p.status === 'pending').length,
  done: projects.value.filter(p => p.status === 'done').length,
}))

const filteredProjects = computed(() => {
  return projects.value.filter(p => {
    if (filterStatus.value && p.status !== filterStatus.value) return false
    if (filterType.value && p.type !== filterType.value) return false
    if (searchKey.value) {
      const k = searchKey.value.toLowerCase()
      if (!p.name?.toLowerCase().includes(k) && !p.manager?.toLowerCase().includes(k)) return false
    }
    return true
  })
})

const statusType = (s) => ({ active: 'primary', pending: 'danger', done: 'success' }[s] || 'info')
const statusText = (s) => ({ active: '进行中', pending: '待审批', done: '已完成' }[s] || s)

const resetForm = () => {
  form.value = { name: '', type: '装修', manager: '', budget: 0, startDate: '', endDate: '', description: '' }
}

const openAddDialog = () => {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  editingId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

const saveProject = () => {
  if (!form.value.name) {
    ElMessage.warning('请输入项目名称')
    return
  }
  if (editingId.value) {
    const idx = projects.value.findIndex(p => p.id === editingId.value)
    if (idx >= 0) projects.value[idx] = { ...form.value, id: editingId.value }
    ElMessage.success('项目已更新')
  } else {
    projects.value.push({ ...form.value, id: Date.now(), progress: 0, status: 'pending' })
    ElMessage.success('项目已创建')
  }
  dialogVisible.value = false
  editingId.value = null
  resetForm()
}

const deleteProject = (row) => {
  ElMessageBox.confirm(`确定删除项目「${row.name}」吗？`, '提示', { type: 'warning' })
    .then(() => {
      projects.value = projects.value.filter(p => p.id !== row.id)
      ElMessage.success('项目已删除')
    })
    .catch(() => {})
}
</script>

<style scoped>
.decoration-page { padding: 24px 32px; }

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

.progress-cell { min-width: 140px; }

.text-muted { font-size: 12px; color: #a0b0a5; }
</style>

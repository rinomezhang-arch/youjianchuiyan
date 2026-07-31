<template>
  <div class="safety-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">安全管理 · Safety Management</h2>
        <p class="page-desc">安全隐患台账 · 整改跟踪 · 巡检与合规管理</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="openAddDialog">上报隐患</el-button>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <span class="stat-label">待整改 · Pending</span>
        <span class="stat-value" style="color:#C0392B">{{ stats.pending }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">已整改 · Resolved</span>
        <span class="stat-value" style="color:#4A7C59">{{ stats.resolved }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">巡检任务 · Inspections</span>
        <span class="stat-value">{{ stats.inspections }}</span>
      </div>
      <div class="stat-card">
        <span class="stat-label">完成率 · Rate</span>
        <span class="stat-value" style="color:#C4A35A">{{ stats.rate }}%</span>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width: 140px">
            <el-option label="待整改" value="pending" />
            <el-option label="整改中" value="processing" />
            <el-option label="已整改" value="resolved" />
          </el-select>
          <el-select v-model="filterSeverity" placeholder="全部严重程度" clearable style="width: 160px">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
          <el-input v-model="searchKey" placeholder="搜索问题/检查区域" clearable style="width: 240px" />
        </div>
      </div>

      <el-table :data="filteredIssues" stripe style="width: 100%">
        <el-table-column prop="date" label="日期" width="130" />
        <el-table-column prop="area" label="检查区域" width="140" />
        <el-table-column prop="type" label="问题类型" min-width="160" />
        <el-table-column label="严重程度" width="110">
          <template #default="{ row }">
            <el-tag :type="severityType(row.severity)" effect="plain">{{ severityText(row.severity) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="responsible" label="整改人" width="110">
          <template #default="{ row }">{{ row.responsible || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'resolved'" size="small" type="primary" link @click="advanceStatus(row)">推进</el-button>
            <el-button v-if="row.status === 'resolved'" size="small" type="info" link disabled>已闭环</el-button>
            <el-button size="small" type="danger" link @click="deleteIssue(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="上报安全隐患" width="560px">
      <el-form :model="form" label-width="100px" label-position="right">
        <el-form-item label="检查区域">
          <el-input v-model="form.area" placeholder="例：后门通道" />
        </el-form-item>
        <el-form-item label="问题类型">
          <el-input v-model="form.type" placeholder="例：消防通道堆放杂物" />
        </el-form-item>
        <el-form-item label="严重程度">
          <el-select v-model="form.severity" placeholder="请选择严重程度" style="width: 100%">
            <el-option label="高" value="high" />
            <el-option label="中" value="medium" />
            <el-option label="低" value="low" />
          </el-select>
        </el-form-item>
        <el-form-item label="发现日期">
          <el-date-picker v-model="form.date" type="date" placeholder="选择日期" style="width: 100%" />
        </el-form-item>
        <el-form-item label="整改人">
          <el-input v-model="form.responsible" placeholder="整改责任人" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="详细描述安全隐患..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveIssue">上报</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const filterStatus = ref('')
const filterSeverity = ref('')
const searchKey = ref('')
const dialogVisible = ref(false)

const issues = ref([])

const form = ref({ area: '', type: '', severity: 'medium', date: '', responsible: '', description: '' })

const stats = computed(() => {
  const total = issues.value.length
  const resolved = issues.value.filter(s => s.status === 'resolved').length
  const inspections = total
  const rate = total > 0 ? Math.round((resolved / total) * 100) : 0
  return {
    pending: issues.value.filter(s => s.status === 'pending').length,
    resolved,
    inspections,
    rate,
  }
})

const filteredIssues = computed(() => {
  return issues.value.filter(s => {
    if (filterStatus.value && s.status !== filterStatus.value) return false
    if (filterSeverity.value && s.severity !== filterSeverity.value) return false
    if (searchKey.value) {
      const k = searchKey.value.toLowerCase()
      if (!s.type?.toLowerCase().includes(k) && !s.area?.toLowerCase().includes(k)) return false
    }
    return true
  })
})

const severityType = (s) => ({ high: 'danger', medium: 'warning', low: 'success' }[s] || 'info')
const severityText = (s) => ({ high: '高', medium: '中', low: '低' }[s] || s)
const statusType = (s) => ({ pending: 'danger', processing: 'warning', resolved: 'success' }[s] || 'info')
const statusText = (s) => ({ pending: '待整改', processing: '整改中', resolved: '已整改' }[s] || s)

const todayStr = () => new Date().toISOString().slice(0, 10)

const openAddDialog = () => {
  form.value = { area: '', type: '', severity: 'medium', date: todayStr(), responsible: '', description: '' }
  dialogVisible.value = true
}

const saveIssue = () => {
  if (!form.value.type || !form.value.area) {
    ElMessage.warning('请填写检查区域和问题类型')
    return
  }
  issues.value.unshift({
    id: Date.now(),
    ...form.value,
    date: form.value.date || todayStr(),
    status: 'pending',
  })
  ElMessage.success('隐患已上报')
  dialogVisible.value = false
  form.value = { area: '', type: '', severity: 'medium', date: '', responsible: '', description: '' }
}

const advanceStatus = (row) => {
  if (row.status === 'pending') {
    row.status = 'processing'
    ElMessage.success('已开始整改')
  } else if (row.status === 'processing') {
    row.status = 'resolved'
    ElMessage.success('隐患已整改')
  }
}

const deleteIssue = (row) => {
  ElMessageBox.confirm(`确定删除隐患记录吗？`, '提示', { type: 'warning' })
    .then(() => {
      issues.value = issues.value.filter(s => s.id !== row.id)
      ElMessage.success('记录已删除')
    })
    .catch(() => {})
}
</script>

<style scoped>
.safety-page { padding: 24px 32px; }

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

.text-muted { font-size: 12px; color: #a0b0a5; }
</style>

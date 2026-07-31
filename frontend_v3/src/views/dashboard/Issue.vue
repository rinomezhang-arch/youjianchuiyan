<template>
  <div class="issue-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">领用出库 · Issue</h2>
        <p class="page-desc">原料领用 · 出库登记 · 审批管理</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">今日出库</div>
        <div class="stat-value">{{ stats.todayIssue }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">待审批</div>
        <div class="stat-value">{{ stats.pendingApproval }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">本月出库</div>
        <div class="stat-value">{{ stats.monthIssue }}</div>
      </div>
      <div class="stat-card stat-card--accent">
        <div class="stat-label">待领取</div>
        <div class="stat-value">{{ stats.waitingPickup }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filters.status" placeholder="状态" style="width:140px" clearable>
            <el-option label="全部" value="" />
            <el-option label="待审批" value="pending" />
            <el-option label="已审批" value="approved" />
            <el-option label="已出库" value="issued" />
          </el-select>
          <el-select v-model="filters.department" placeholder="领用部门" style="width:160px" clearable>
            <el-option v-for="d in departmentOptions" :key="d" :label="d" :value="d" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width:260px"
          />
          <el-input v-model="filters.keyword" placeholder="搜索单号/领用人" style="width:220px" clearable />
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button @click="exportData">导出</el-button>
          <el-button type="primary" @click="openAddDialog">新增领用单</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无领用出库记录">
        <el-table-column prop="issueNo" label="单号" width="160" />
        <el-table-column prop="department" label="领用部门" width="120" />
        <el-table-column prop="applicant" label="领用人" width="100" />
        <el-table-column prop="itemCount" label="品项数" width="90" align="center" />
        <el-table-column prop="totalQty" label="数量" width="90" align="center" />
        <el-table-column prop="issueTime" label="出库时间" width="160" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'pending'" type="warning" size="small">待审批</el-tag>
            <el-tag v-else-if="row.status === 'approved'" type="info" size="small">已审批</el-tag>
            <el-tag v-else-if="row.status === 'issued'" type="success" size="small">已出库</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 'pending'" size="small" type="success" @click="approveIssue(row)">审批</el-button>
            <el-button v-if="row.status === 'approved'" size="small" type="primary" @click="completeIssue(row)">完成出库</el-button>
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAddDialog" title="新增领用出库单" width="780px" @close="resetAddForm">
      <el-form :model="addForm" label-width="100px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="领用部门" required>
              <el-select v-model="addForm.department" filterable placeholder="请选择部门" style="width:100%">
                <el-option v-for="d in departmentOptions" :key="d" :label="d" :value="d" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="领用人" required>
              <el-input v-model="addForm.applicant" placeholder="请输入领用人" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="领用明细">
          <el-table :data="addForm.items" border size="small">
            <el-table-column label="品项" min-width="180">
              <template #default="{ row }">
                <el-select v-model="row.materialId" filterable size="small" placeholder="选择品项" style="width:100%">
                  <el-option v-for="m in materialOptions" :key="m.id" :label="m.name" :value="m.id" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="单位" width="80">
              <template #default="{ row }">{{ row.unit }}</template>
            </el-table-column>
            <el-table-column label="数量" width="150">
              <template #default="{ row }">
                <el-input-number v-model="row.quantity" :min="0" size="small" style="width:100%" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template #default="{ $index }">
                <el-button type="danger" text size="small" @click="removeAddItem($index)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="add-items-footer">
            <el-button size="small" @click="addAddItem">添加品项</el-button>
            <span class="add-total">合计数量: <b>{{ addTotalQty }}</b></span>
          </div>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])
const materialOptions = ref([])
const departmentOptions = ref(['后厨', '前厅', '吧台', '面点间', '凉菜间', '行政'])

const filters = reactive({
  status: '',
  department: '',
  dateRange: [],
  keyword: ''
})

const stats = computed(() => ({
  todayIssue: 0,
  pendingApproval: list.value.filter(r => r.status === 'pending').length,
  monthIssue: 0,
  waitingPickup: list.value.filter(r => r.status === 'approved').length
}))

const filteredList = computed(() => {
  return list.value.filter(row => {
    if (filters.status && row.status !== filters.status) return false
    if (filters.department && row.department !== filters.department) return false
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!String(row.issueNo || '').toLowerCase().includes(kw) &&
          !String(row.applicant || '').toLowerCase().includes(kw)) return false
    }
    if (filters.dateRange && filters.dateRange.length === 2) {
      const d = row.issueTime ? String(row.issueTime).slice(0, 10) : ''
      if (d < filters.dateRange[0] || d > filters.dateRange[1]) return false
    }
    return true
  })
})

function resetFilters() {
  filters.status = ''
  filters.department = ''
  filters.dateRange = []
  filters.keyword = ''
}

const showAddDialog = ref(false)
const addForm = ref({ department: '', applicant: '', items: [], remark: '' })

const addTotalQty = computed(() => {
  return addForm.value.items.reduce((sum, i) => sum + (i.quantity || 0), 0)
})

function openAddDialog() {
  resetAddForm()
  showAddDialog.value = true
}

function resetAddForm() {
  addForm.value = { department: '', applicant: '', items: [emptyItem()], remark: '' }
}

function emptyItem() {
  return { materialId: '', unit: '', quantity: 1 }
}

function addAddItem() {
  addForm.value.items.push(emptyItem())
}

function removeAddItem(index) {
  addForm.value.items.splice(index, 1)
}

function submitAdd() {
  if (!addForm.value.department) { ElMessage.warning('请选择领用部门'); return }
  if (!addForm.value.applicant) { ElMessage.warning('请输入领用人'); return }
  const valid = addForm.value.items.filter(i => i.materialId && i.quantity > 0)
  if (valid.length === 0) { ElMessage.warning('请添加有效的品项'); return }
  ElMessage.success('领用出库单已提交（待接入API）')
  showAddDialog.value = false
}

function approveIssue(row) {
  ElMessageBox.confirm(`确认审批通过单据 "${row.issueNo}" ?`, '审批确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    row.status = 'approved'
    ElMessage.success('审批通过')
  }).catch(() => {})
}

function completeIssue(row) {
  ElMessageBox.confirm(`确认完成出库单据 "${row.issueNo}" ?`, '出库确认', {
    confirmButtonText: '确认出库', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    row.status = 'issued'
    ElMessage.success('出库完成')
  }).catch(() => {})
}

function viewDetail(row) {
  ElMessage.info(`查看出库单: ${row.issueNo}`)
}

function exportData() {
  ElMessage.info('导出功能待接入')
}
</script>

<style scoped>
.issue-page {
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
.add-items-footer {
  margin-top: 8px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.add-total { font-size: 14px; color: #1a2f23; }
.add-total b { color: #C4A35A; font-size: 15px; }
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

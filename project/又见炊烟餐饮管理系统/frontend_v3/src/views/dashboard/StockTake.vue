<template>
  <div class="stocktake-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">盘点 · Stock Take</h2>
        <p class="page-desc">库存盘点 · 差异处理 · 盘点报告</p>
      </div>
    </div>

    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">待盘点</div>
        <div class="stat-value">{{ stats.pending }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">盘点中</div>
        <div class="stat-value">{{ stats.counting }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">已完成</div>
        <div class="stat-value">{{ stats.completed }}</div>
      </div>
      <div class="stat-card stat-card--accent">
        <div class="stat-label">差异项</div>
        <div class="stat-value">{{ stats.diffCount }}</div>
      </div>
    </div>

    <div class="content-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filters.status" placeholder="状态" style="width:140px" clearable>
            <el-option label="全部" value="" />
            <el-option label="待盘点" value="pending" />
            <el-option label="盘点中" value="counting" />
            <el-option label="已完成" value="completed" />
          </el-select>
          <el-date-picker
            v-model="filters.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width:260px"
          />
          <el-input v-model="filters.keyword" placeholder="搜索盘点单号/范围" style="width:220px" clearable />
          <el-button @click="resetFilters">重置</el-button>
        </div>
        <div class="toolbar-right">
          <el-button @click="exportData">导出</el-button>
          <el-button type="primary" @click="openAddDialog">新增盘点单</el-button>
        </div>
      </div>

      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无盘点记录">
        <el-table-column prop="stocktakeNo" label="盘点单号" width="160" />
        <el-table-column prop="scope" label="盘点范围" min-width="180" />
        <el-table-column prop="operator" label="盘点人" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="160" />
        <el-table-column prop="endTime" label="结束时间" width="160" />
        <el-table-column prop="diffCount" label="差异项数" width="100" align="center">
          <template #default="{ row }">
            <span :class="{ 'diff-num': row.diffCount > 0 }">{{ row.diffCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.status === 'pending'" type="warning" size="small">待盘点</el-tag>
            <el-tag v-else-if="row.status === 'counting'" type="info" size="small">盘点中</el-tag>
            <el-tag v-else-if="row.status === 'completed'" type="success" size="small">已完成</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'completed'" size="small" type="primary" @click="continueStocktake(row)">继续</el-button>
            <el-button size="small" @click="viewDetail(row)">查看</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="showAddDialog" title="新增盘点单" width="640px" @close="resetAddForm">
      <el-form :model="addForm" label-width="100px">
        <el-form-item label="盘点范围" required>
          <el-select v-model="addForm.scope" filterable placeholder="请选择盘点范围" style="width:100%">
            <el-option label="全店库存" value="全店库存" />
            <el-option label="主仓库" value="主仓库" />
            <el-option label="冷库" value="冷库" />
            <el-option label="后厨暂存" value="后厨暂存" />
            <el-option label="酒水库" value="酒水库" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="盘点人" required>
              <el-input v-model="addForm.operator" placeholder="请输入盘点人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="预计开始">
              <el-date-picker
                v-model="addForm.startTime"
                type="datetime"
                placeholder="选择开始时间"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="addForm.remark" type="textarea" :rows="2" placeholder="备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="submitAdd">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showDetailDialog" title="盘点明细 · 系统数量与实盘对比" width="900px">
      <div v-if="currentDetail" class="detail-meta">
        <span>单号: <b>{{ currentDetail.stocktakeNo }}</b></span>
        <span>范围: <b>{{ currentDetail.scope }}</b></span>
        <span>盘点人: <b>{{ currentDetail.operator }}</b></span>
        <span>差异项: <b :class="{ 'diff-num': currentDetail.diffCount > 0 }">{{ currentDetail.diffCount }}</b></span>
      </div>
      <el-table :data="detailItems" border size="small" empty-text="暂无盘点明细">
        <el-table-column prop="materialName" label="品项" min-width="180" />
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column prop="systemQty" label="系统数量" width="120" align="right" />
        <el-table-column prop="actualQty" label="实盘数量" width="120" align="right" />
        <el-table-column label="差异" width="120" align="right">
          <template #default="{ row }">
            <span :class="diffClass(row.diff)">{{ diffText(row.diff) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异金额" width="120" align="right">
          <template #default="{ row }">
            <span :class="diffClass(row.diffAmount)">¥{{ formatMoney(row.diffAmount) }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="showDetailDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const list = ref([])

const filters = reactive({
  status: '',
  dateRange: [],
  keyword: ''
})

const stats = computed(() => ({
  pending: list.value.filter(r => r.status === 'pending').length,
  counting: list.value.filter(r => r.status === 'counting').length,
  completed: list.value.filter(r => r.status === 'completed').length,
  diffCount: list.value.reduce((sum, r) => sum + (r.diffCount || 0), 0)
}))

const filteredList = computed(() => {
  return list.value.filter(row => {
    if (filters.status && row.status !== filters.status) return false
    if (filters.keyword) {
      const kw = filters.keyword.toLowerCase()
      if (!String(row.stocktakeNo || '').toLowerCase().includes(kw) &&
          !String(row.scope || '').toLowerCase().includes(kw)) return false
    }
    if (filters.dateRange && filters.dateRange.length === 2) {
      const d = row.startTime ? String(row.startTime).slice(0, 10) : ''
      if (d < filters.dateRange[0] || d > filters.dateRange[1]) return false
    }
    return true
  })
})

function resetFilters() {
  filters.status = ''
  filters.dateRange = []
  filters.keyword = ''
}

function formatMoney(n) {
  return Number(n || 0).toFixed(2)
}

function diffClass(v) {
  if (v > 0) return 'diff-pos'
  if (v < 0) return 'diff-neg'
  return 'diff-zero'
}

function diffText(v) {
  if (v > 0) return '+' + v
  return String(v)
}

const showAddDialog = ref(false)
const addForm = ref({ scope: '', operator: '', startTime: '', remark: '' })

function openAddDialog() {
  resetAddForm()
  showAddDialog.value = true
}

function resetAddForm() {
  addForm.value = { scope: '', operator: '', startTime: '', remark: '' }
}

function submitAdd() {
  if (!addForm.value.scope) { ElMessage.warning('请选择盘点范围'); return }
  if (!addForm.value.operator) { ElMessage.warning('请输入盘点人'); return }
  ElMessage.success('盘点单已创建（待接入API）')
  showAddDialog.value = false
}

function continueStocktake(row) {
  ElMessageBox.confirm(`确认开始/继续盘点单 "${row.stocktakeNo}" ?`, '盘点确认', {
    confirmButtonText: '确认', cancelButtonText: '取消', type: 'warning'
  }).then(() => {
    row.status = 'counting'
    ElMessage.success('已进入盘点状态')
  }).catch(() => {})
}

const showDetailDialog = ref(false)
const currentDetail = ref(null)
const detailItems = ref([])

function viewDetail(row) {
  currentDetail.value = { ...row }
  detailItems.value = []
  showDetailDialog.value = true
}

function exportData() {
  ElMessage.info('导出功能待接入')
}
</script>

<style scoped>
.stocktake-page {
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
.diff-num { color: #dc2626; font-weight: 700; }
.diff-pos { color: #389e0d; font-weight: 600; }
.diff-neg { color: #dc2626; font-weight: 600; }
.diff-zero { color: #6b7c70; }
.detail-meta {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  margin-bottom: 16px;
  padding: 12px 16px;
  background: #FAF8F5;
  border-radius: 10px;
  font-size: 13px;
  color: #1a2f23;
}
.detail-meta b { color: #2D4A3E; }
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

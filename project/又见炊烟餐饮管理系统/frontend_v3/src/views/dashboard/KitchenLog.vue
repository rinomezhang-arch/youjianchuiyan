<template>
  <div class="kitchen-log-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">后厨日志 · Kitchen Log</h2>
        <p class="page-desc">出品记录 · 设备故障 · 卫生检查 · 食材损耗 · 安全事故</p>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="openAddDialog">新增日志 · Add</el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-label">今日日志 · Today</div>
        <div class="stat-value stat-today">{{ stats.today }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">异常事件 · Abnormal</div>
        <div class="stat-value stat-abnormal">{{ stats.abnormal }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">设备故障 · Equipment</div>
        <div class="stat-value stat-equipment">{{ stats.equipment }}</div>
      </div>
      <div class="stat-card">
        <div class="stat-label">卫生检查 · Hygiene</div>
        <div class="stat-value stat-hygiene">{{ stats.hygiene }}</div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <!-- 工具栏 -->
      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterType" placeholder="全部类型" clearable style="width: 160px">
            <el-option label="全部 · All" value="" />
            <el-option label="出品记录 · Output" value="出品记录" />
            <el-option label="设备故障 · Equipment" value="设备故障" />
            <el-option label="卫生检查 · Hygiene" value="卫生检查" />
            <el-option label="食材损耗 · Wastage" value="食材损耗" />
            <el-option label="安全事故 · Safety" value="安全事故" />
          </el-select>
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 280px"
          />
          <el-input
            v-model="searchKeyword"
            placeholder="搜索事件描述 / 操作人"
            clearable
            style="width: 240px"
          />
        </div>
        <div class="toolbar-right">
          <el-button text @click="clearFilters">清除 · Clear</el-button>
        </div>
      </div>

      <!-- 数据表格 -->
      <el-table :data="filteredList" stripe class="data-table" v-loading="loading">
        <el-table-column prop="date" label="日期 · Date" width="130" />
        <el-table-column prop="time" label="时间 · Time" width="110" />
        <el-table-column prop="type" label="类型 · Type" width="130" align="center">
          <template #default="{ row }">
            <el-tag :type="typeTag(row.type)" size="small" effect="light">{{ row.type }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="事件描述 · Description" min-width="240" show-overflow-tooltip />
        <el-table-column prop="operator" label="操作人 · Operator" width="130" />
        <el-table-column prop="remark" label="备注 · Remark" min-width="180" show-overflow-tooltip />
      </el-table>

      <div class="table-footer">
        <span class="total-text">共 {{ filteredList.length }} 条</span>
      </div>
    </div>

    <!-- 新增日志对话框 -->
    <el-dialog v-model="showAddDialog" title="新增后厨日志 · Add Kitchen Log" width="560px" destroy-on-close>
      <el-form :model="logForm" label-width="100px">
        <el-form-item label="日期 · Date">
          <el-date-picker
            v-model="logForm.date"
            type="date"
            placeholder="选择日期"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="时间 · Time">
          <el-input v-model="logForm.time" placeholder="如 14:30" />
        </el-form-item>
        <el-form-item label="类型 · Type">
          <el-select v-model="logForm.type" placeholder="选择类型" style="width: 100%">
            <el-option label="出品记录 · Output" value="出品记录" />
            <el-option label="设备故障 · Equipment" value="设备故障" />
            <el-option label="卫生检查 · Hygiene" value="卫生检查" />
            <el-option label="食材损耗 · Wastage" value="食材损耗" />
            <el-option label="安全事故 · Safety" value="安全事故" />
          </el-select>
        </el-form-item>
        <el-form-item label="事件描述">
          <el-input
            v-model="logForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入事件描述"
          />
        </el-form-item>
        <el-form-item label="操作人">
          <el-input v-model="logForm.operator" placeholder="请输入操作人姓名" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="logForm.remark" type="textarea" :rows="2" placeholder="可选" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveLog">保存 · Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'

const loading = ref(false)
const logs = ref([])
const filterType = ref('')
const dateRange = ref([])
const searchKeyword = ref('')
const showAddDialog = ref(false)

const logForm = ref({
  date: '',
  time: '',
  type: '出品记录',
  description: '',
  operator: '',
  remark: '',
})

const stats = computed(() => {
  const list = logs.value
  const today = new Date().toISOString().slice(0, 10)
  return {
    today: list.filter(l => l.date === today).length,
    abnormal: list.filter(l => l.type === '安全事故' || l.type === '设备故障').length,
    equipment: list.filter(l => l.type === '设备故障').length,
    hygiene: list.filter(l => l.type === '卫生检查').length,
  }
})

const filteredList = computed(() => {
  let list = logs.value
  if (filterType.value) list = list.filter(l => l.type === filterType.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(l => l.date >= start && l.date <= end)
  }
  if (searchKeyword.value) {
    const q = searchKeyword.value.toLowerCase()
    list = list.filter(l =>
      (l.description || '').toLowerCase().includes(q) ||
      (l.operator || '').toLowerCase().includes(q)
    )
  }
  return list
})

function typeTag(t) {
  return {
    '出品记录': 'success',
    '设备故障': 'danger',
    '卫生检查': '',
    '食材损耗': 'warning',
    '安全事故': 'danger',
  }[t] || 'info'
}

function clearFilters() {
  filterType.value = ''
  dateRange.value = []
  searchKeyword.value = ''
}

function openAddDialog() {
  const now = new Date()
  logForm.value = {
    date: now.toISOString().slice(0, 10),
    time: now.toTimeString().slice(0, 5),
    type: '出品记录',
    description: '',
    operator: '',
    remark: '',
  }
  showAddDialog.value = true
}

function saveLog() {
  if (!logForm.value.description) {
    ElMessage.warning('请输入事件描述')
    return
  }
  logs.value.unshift({ ...logForm.value })
  ElMessage.success('日志已添加')
  showAddDialog.value = false
}

async function loadData() {
  loading.value = true
  try {
    // TODO: 接入后端 API
    // const res = await getKitchenLogs()
    // if (res.code === 200) logs.value = res.data || []
    logs.value = []
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => { loadData() })
</script>

<style scoped>
.kitchen-log-page {
  padding: 24px 32px;
  max-width: 1600px;
  margin: 0 auto;
}

/* 页面头部 */
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.header-left { display: flex; flex-direction: column; }
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
}
.page-desc {
  font-size: 13px;
  color: #6b7c72;
  margin: 4px 0 0;
}
.header-right { display: flex; gap: 8px; }

/* 统计卡片 */
.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #ffffff;
  border: 1px solid #ebe6dc;
  border-radius: 10px;
  padding: 18px 20px;
  border-top: 3px solid #2D4A3E;
}
.stat-label {
  font-size: 13px;
  color: #6b7c72;
  margin-bottom: 8px;
  font-weight: 500;
}
.stat-value {
  font-size: 30px;
  font-weight: 700;
  line-height: 1.1;
}
.stat-today { color: #2D4A3E; }
.stat-abnormal { color: #C25555; }
.stat-equipment { color: #C4A35A; }
.stat-hygiene { color: #4A7C59; }

/* 内容卡片 */
.content-card {
  background: #ffffff;
  border: 1px solid #ebe6dc;
  border-radius: 10px;
  padding: 20px;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 10px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}
.toolbar-right { display: flex; align-items: center; gap: 8px; }

/* 表格 */
.data-table {
  width: 100%;
  border-radius: 8px;
  overflow: hidden;
}
.table-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.total-text {
  font-size: 13px;
  color: #6b7c72;
}

/* 主题色覆盖 */
:deep(.el-button--primary) {
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4e;
  --el-button-hover-border-color: #3a5e4e;
  --el-button-active-bg-color: #243d33;
  --el-button-active-border-color: #243d33;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .kitchen-log-page { padding: 16px; }
}
</style>

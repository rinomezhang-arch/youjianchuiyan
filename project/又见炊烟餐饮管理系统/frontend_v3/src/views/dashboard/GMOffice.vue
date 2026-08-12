<template>
  <div class="gm-office-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">总经办 · GM Office</h1>
        <p class="page-desc">总经理决策中心 · 审批事项、决议公告与经营报告统一管理</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-pending">
        <div class="stat-label">待审批</div>
        <div class="stat-value">{{ stats.pending }}</div>
        <div class="stat-sub">Pending Approval</div>
      </div>
      <div class="stat-card stat-approved">
        <div class="stat-label">已审批</div>
        <div class="stat-value">{{ stats.approved }}</div>
        <div class="stat-sub">Approved</div>
      </div>
      <div class="stat-card stat-resolution">
        <div class="stat-label">今日决议</div>
        <div class="stat-value">{{ stats.todayResolution }}</div>
        <div class="stat-sub">Today's Resolutions</div>
      </div>
      <div class="stat-card stat-report">
        <div class="stat-label">本月报告</div>
        <div class="stat-value">{{ stats.monthReport }}</div>
        <div class="stat-sub">Monthly Reports</div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <el-tabs v-model="activeTab" class="page-tabs">
        <!-- 审批事项 -->
        <el-tab-pane label="审批事项" name="approval">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:140px">
                <el-option label="待审批" value="待审批" />
                <el-option label="已通过" value="已通过" />
                <el-option label="已驳回" value="已驳回" />
              </el-select>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width:260px"
              />
            </div>
            <el-button type="primary" @click="fetchData">刷新</el-button>
          </div>
          <el-table :data="filteredApprovals" stripe v-loading="loading" empty-text="暂无审批事项">
            <el-table-column prop="applicant" label="申请人" width="110" />
            <el-table-column prop="subject" label="事项" min-width="220" show-overflow-tooltip />
            <el-table-column prop="type" label="类型" width="120">
              <template #default="{ row }">
                <el-tag size="small" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" width="130" align="right">
              <template #default="{ row }">
                <span class="amount-text">¥{{ (row.amount || 0).toLocaleString() }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="submitTime" label="提交时间" width="170" />
            <el-table-column prop="status" label="状态" width="110" align="center">
              <template #default="{ row }">
                <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="170" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" @click="viewApproval(row)">详情</el-button>
                <el-button v-if="row.status === '待审批'" text size="small" type="success" @click="approveRow(row)">通过</el-button>
                <el-button v-if="row.status === '待审批'" text size="small" type="danger" @click="rejectRow(row)">驳回</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 决议公告 -->
        <el-tab-pane label="决议公告" name="resolution">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width:260px"
              />
            </div>
            <el-button type="primary" @click="fetchData">刷新</el-button>
          </div>
          <el-table :data="filteredResolutions" stripe v-loading="loading" empty-text="暂无决议公告">
            <el-table-column prop="title" label="标题" min-width="240" show-overflow-tooltip />
            <el-table-column prop="publisher" label="发布人" width="120" />
            <el-table-column prop="publishDate" label="发布日期" width="140" />
            <el-table-column prop="summary" label="内容摘要" min-width="320" show-overflow-tooltip />
            <el-table-column label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" @click="viewResolution(row)">查看</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <!-- 经营报告 -->
        <el-tab-pane label="经营报告" name="report">
          <div class="toolbar">
            <div class="toolbar-left">
              <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:140px">
                <el-option label="待审阅" value="待审阅" />
                <el-option label="已审阅" value="已审阅" />
                <el-option label="已归档" value="已归档" />
              </el-select>
              <el-date-picker
                v-model="dateRange"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                style="width:260px"
              />
            </div>
            <el-button type="primary" @click="fetchData">刷新</el-button>
          </div>
          <el-table :data="filteredReports" stripe v-loading="loading" empty-text="暂无经营报告">
            <el-table-column prop="reportType" label="报告类型" width="160">
              <template #default="{ row }">
                <el-tag size="small" effect="plain" type="info">{{ row.reportType }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="month" label="月份" width="120" />
            <el-table-column prop="author" label="编制人" width="120" />
            <el-table-column prop="submitDate" label="提交日期" width="140" />
            <el-table-column prop="status" label="状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="reportStatusTag(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button text size="small" @click="viewReport(row)">查看</el-button>
                <el-button v-if="row.status === '待审阅'" text size="small" type="primary" @click="markReportRead(row)">审阅</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDialog" :title="dialogTitle" width="560px">
      <el-form label-width="100px" label-position="right">
        <el-form-item v-for="field in dialogFields" :key="field.label" :label="field.label">
          <span class="dialog-text">{{ field.value }}</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">关闭</el-button>
        <el-button v-if="dialogAction === 'approve'" type="success" @click="confirmApprove">通过</el-button>
        <el-button v-if="dialogAction === 'approve'" type="danger" @click="confirmReject">驳回</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const activeTab = ref('approval')
const filterStatus = ref('')
const dateRange = ref(null)
const showDialog = ref(false)
const dialogTitle = ref('详情')
const dialogAction = ref('')
const currentRow = ref(null)

const approvalList = ref([])
const resolutionList = ref([])
const reportList = ref([])

const stats = ref({
  pending: 0,
  approved: 0,
  todayResolution: 0,
  monthReport: 0
})

const dialogFields = ref([])

const filteredApprovals = computed(() => {
  let list = approvalList.value
  if (filterStatus.value) list = list.filter(a => a.status === filterStatus.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(a => {
      const t = new Date(a.submitTime)
      return t >= new Date(start) && t <= new Date(end)
    })
  }
  return list
})

const filteredResolutions = computed(() => {
  let list = resolutionList.value
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(r => {
      const t = new Date(r.publishDate)
      return t >= new Date(start) && t <= new Date(end)
    })
  }
  return list
})

const filteredReports = computed(() => {
  let list = reportList.value
  if (filterStatus.value) list = list.filter(r => r.status === filterStatus.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(r => {
      const t = new Date(r.submitDate)
      return t >= new Date(start) && t <= new Date(end)
    })
  }
  return list
})

function statusTag(status) {
  return { '待审批': 'warning', '已通过': 'success', '已驳回': 'danger' }[status] || 'info'
}

function reportStatusTag(status) {
  return { '待审阅': 'warning', '已审阅': 'success', '已归档': 'info' }[status] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await fetch('/menu-api/gm/overview').then(r => r.json())
    if (res.code === 200) {
      approvalList.value = res.data?.approvals || []
      resolutionList.value = res.data?.resolutions || []
      reportList.value = res.data?.reports || []
      stats.value = res.data?.stats || stats.value
    }
  } catch {
    console.error('加载总经办数据失败')
    approvalList.value = []
    resolutionList.value = []
    reportList.value = []
  } finally {
    loading.value = false
  }
}

function viewApproval(row) {
  currentRow.value = row
  dialogTitle.value = '审批详情'
  dialogAction.value = row.status === '待审批' ? 'approve' : ''
  dialogFields.value = [
    { label: '申请人', value: row.applicant },
    { label: '事项', value: row.subject },
    { label: '类型', value: row.type },
    { label: '金额', value: `¥${(row.amount || 0).toLocaleString()}` },
    { label: '提交时间', value: row.submitTime },
    { label: '状态', value: row.status }
  ]
  showDialog.value = true
}

function viewResolution(row) {
  currentRow.value = row
  dialogTitle.value = '决议详情'
  dialogAction.value = ''
  dialogFields.value = [
    { label: '标题', value: row.title },
    { label: '发布人', value: row.publisher },
    { label: '发布日期', value: row.publishDate },
    { label: '内容摘要', value: row.summary }
  ]
  showDialog.value = true
}

function viewReport(row) {
  currentRow.value = row
  dialogTitle.value = '报告详情'
  dialogAction.value = ''
  dialogFields.value = [
    { label: '报告类型', value: row.reportType },
    { label: '月份', value: row.month },
    { label: '编制人', value: row.author },
    { label: '提交日期', value: row.submitDate },
    { label: '状态', value: row.status }
  ]
  showDialog.value = true
}

function approveRow(row) {
  ElMessageBox.confirm(`确定通过「${row.subject}」？`, '审批确认', { type: 'success' })
    .then(() => { row.status = '已通过'; ElMessage.success('已通过'); refreshStats() })
    .catch(() => {})
}

function rejectRow(row) {
  ElMessageBox.confirm(`确定驳回「${row.subject}」？`, '驳回确认', { type: 'warning' })
    .then(() => { row.status = '已驳回'; ElMessage.success('已驳回'); refreshStats() })
    .catch(() => {})
}

function confirmApprove() {
  if (currentRow.value) {
    currentRow.value.status = '已通过'
    ElMessage.success('已通过')
    refreshStats()
  }
  showDialog.value = false
}

function confirmReject() {
  if (currentRow.value) {
    currentRow.value.status = '已驳回'
    ElMessage.success('已驳回')
    refreshStats()
  }
  showDialog.value = false
}

function markReportRead(row) {
  row.status = '已审阅'
  ElMessage.success('已标记为已审阅')
}

function refreshStats() {
  stats.value.pending = approvalList.value.filter(a => a.status === '待审批').length
  stats.value.approved = approvalList.value.filter(a => a.status === '已通过').length
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.gm-office-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}
.header-left {
  display: flex;
  flex-direction: column;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a2f23;
  margin: 0;
  letter-spacing: 0.5px;
}
.page-desc {
  font-size: 13px;
  color: #5D6D7E;
  margin-top: 6px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}
.stat-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 22px;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}
.stat-card::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 4px;
  border-radius: 2px 0 0 2px;
}
.stat-card.stat-pending::before { background: #C4A35A; }
.stat-card.stat-approved::before { background: #4A7C59; }
.stat-card.stat-resolution::before { background: #2D4A3E; }
.stat-card.stat-report::before { background: #5B7B8A; }
.stat-card:hover {
  box-shadow: 0 4px 12px rgba(45, 74, 62, 0.08);
  transform: translateY(-2px);
}
.stat-label {
  font-size: 13px;
  color: #5D6D7E;
  font-weight: 500;
  letter-spacing: 0.5px;
}
.stat-value {
  font-size: 30px;
  font-weight: 700;
  color: #1a2f23;
  line-height: 1.2;
  margin-top: 8px;
}
.stat-sub {
  font-size: 12px;
  color: #95A5A6;
  margin-top: 4px;
}

.content-card {
  background: #FFFFFF;
  border: 1px solid #E8E4DE;
  border-radius: 10px;
  padding: 20px 24px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}
.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.amount-text {
  font-weight: 600;
  color: #1a2f23;
}

.dialog-text {
  color: #1a2f23;
  font-size: 14px;
}

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .toolbar { flex-direction: column; align-items: stretch; }
}
</style>

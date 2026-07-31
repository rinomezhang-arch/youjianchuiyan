<template>
  <div class="approval-page">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">审批中心 · Approval Center</h1>
        <p class="page-desc">统一处理各部门审批 · 待办、已办、我发起的事项一目了然</p>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-todo">
        <div class="stat-label">待我审批</div>
        <div class="stat-value">{{ stats.todo }}</div>
        <div class="stat-sub">Pending For Me</div>
      </div>
      <div class="stat-card stat-done">
        <div class="stat-label">我已审批</div>
        <div class="stat-value">{{ stats.done }}</div>
        <div class="stat-sub">Approved By Me</div>
      </div>
      <div class="stat-card stat-mine">
        <div class="stat-label">我发起的</div>
        <div class="stat-value">{{ stats.mine }}</div>
        <div class="stat-sub">Initiated By Me</div>
      </div>
      <div class="stat-card stat-cc">
        <div class="stat-label">抄送我的</div>
        <div class="stat-value">{{ stats.cc }}</div>
        <div class="stat-sub">CC To Me</div>
      </div>
    </div>

    <!-- 内容卡片 -->
    <div class="content-card">
      <el-tabs v-model="activeTab" class="page-tabs">
        <el-tab-pane label="待审批" name="todo" />
        <el-tab-pane label="已审批" name="done" />
        <el-tab-pane label="我发起的" name="mine" />
      </el-tabs>

      <div class="toolbar">
        <div class="toolbar-left">
          <el-select v-model="filterType" placeholder="全部类型" clearable style="width:150px">
            <el-option label="采购申请" value="采购申请" />
            <el-option label="费用报销" value="费用报销" />
            <el-option label="人事申请" value="人事申请" />
            <el-option label="合同审批" value="合同审批" />
            <el-option label="其他" value="其他" />
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

      <el-table :data="filteredList" stripe v-loading="loading" empty-text="暂无审批数据">
        <el-table-column prop="approvalNo" label="单号" width="170" />
        <el-table-column prop="title" label="标题" min-width="220" show-overflow-tooltip />
        <el-table-column prop="applicant" label="发起人" width="110" />
        <el-table-column prop="applyTime" label="发起时间" width="170" />
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
        <el-table-column prop="status" label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTag(row.status)" size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text size="small" @click="viewDetail(row)">详情</el-button>
            <template v-if="activeTab === 'todo' && row.status === '待审批'">
              <el-button text size="small" type="success" @click="openApprove(row)">通过</el-button>
              <el-button text size="small" type="danger" @click="openReject(row)">驳回</el-button>
            </template>
            <el-button v-if="activeTab === 'mine' && row.status === '待审批'" text size="small" type="warning" @click="withdraw(row)">撤回</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 详情弹窗 -->
    <el-dialog v-model="showDetail" title="审批详情" width="560px">
      <el-form label-width="100px" label-position="right">
        <el-form-item label="单号"><span class="dialog-text">{{ currentRow?.approvalNo }}</span></el-form-item>
        <el-form-item label="标题"><span class="dialog-text">{{ currentRow?.title }}</span></el-form-item>
        <el-form-item label="发起人"><span class="dialog-text">{{ currentRow?.applicant }}</span></el-form-item>
        <el-form-item label="发起时间"><span class="dialog-text">{{ currentRow?.applyTime }}</span></el-form-item>
        <el-form-item label="类型"><span class="dialog-text">{{ currentRow?.type }}</span></el-form-item>
        <el-form-item label="金额"><span class="dialog-text">¥{{ (currentRow?.amount || 0).toLocaleString() }}</span></el-form-item>
        <el-form-item label="状态"><span class="dialog-text">{{ currentRow?.status }}</span></el-form-item>
        <el-form-item label="备注"><span class="dialog-text">{{ currentRow?.remark || '-' }}</span></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDetail = false">关闭</el-button>
        <template v-if="currentRow?.status === '待审批' && activeTab === 'todo'">
          <el-button type="success" @click="confirmApprove">通过</el-button>
          <el-button type="danger" @click="confirmReject">驳回</el-button>
        </template>
      </template>
    </el-dialog>

    <!-- 审批操作弹窗 -->
    <el-dialog v-model="showAction" :title="actionTitle" width="480px">
      <el-form label-width="90px" label-position="right">
        <el-form-item label="单号"><span class="dialog-text">{{ currentRow?.approvalNo }}</span></el-form-item>
        <el-form-item label="标题"><span class="dialog-text">{{ currentRow?.title }}</span></el-form-item>
        <el-form-item :label="actionType === 'approve' ? '审批意见' : '驳回原因'">
          <el-input v-model="actionComment" type="textarea" :rows="4" :placeholder="actionType === 'approve' ? '请输入审批意见（可选）' : '请输入驳回原因'" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAction = false">取消</el-button>
        <el-button :type="actionType === 'approve' ? 'success' : 'danger'" @click="submitAction">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const activeTab = ref('todo')
const filterType = ref('')
const dateRange = ref(null)
const showDetail = ref(false)
const showAction = ref(false)
const actionType = ref('approve')
const actionComment = ref('')
const currentRow = ref(null)

const approvalList = ref([])

const stats = ref({ todo: 0, done: 0, mine: 0, cc: 0 })

const actionTitle = computed(() => actionType.value === 'approve' ? '通过审批' : '驳回审批')

const filteredList = computed(() => {
  let list = approvalList.value
  if (activeTab.value === 'todo') list = list.filter(a => a.bucket === 'todo' && a.status === '待审批')
  if (activeTab.value === 'done') list = list.filter(a => a.bucket === 'done')
  if (activeTab.value === 'mine') list = list.filter(a => a.bucket === 'mine')
  if (filterType.value) list = list.filter(a => a.type === filterType.value)
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    list = list.filter(a => {
      const t = new Date(a.applyTime)
      return t >= new Date(start) && t <= new Date(end)
    })
  }
  return list
})

function statusTag(status) {
  return { '待审批': 'warning', '已通过': 'success', '已驳回': 'danger', '已撤回': 'info' }[status] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const res = await fetch('/menu-api/approvals').then(r => r.json())
    if (res.code === 200) {
      approvalList.value = res.data?.list || res.data || []
    }
  } catch {
    console.error('加载审批数据失败')
    approvalList.value = []
  } finally {
    loading.value = false
  }
  refreshStats()
}

function refreshStats() {
  stats.value.todo = approvalList.value.filter(a => a.bucket === 'todo' && a.status === '待审批').length
  stats.value.done = approvalList.value.filter(a => a.bucket === 'done').length
  stats.value.mine = approvalList.value.filter(a => a.bucket === 'mine').length
  stats.value.cc = approvalList.value.filter(a => a.bucket === 'cc').length
}

function viewDetail(row) {
  currentRow.value = row
  showDetail.value = true
}

function openApprove(row) {
  currentRow.value = row
  actionType.value = 'approve'
  actionComment.value = ''
  showAction.value = true
}

function openReject(row) {
  currentRow.value = row
  actionType.value = 'reject'
  actionComment.value = ''
  showAction.value = true
}

function submitAction() {
  if (!currentRow.value) return
  if (actionType.value === 'approve') {
    currentRow.value.status = '已通过'
    currentRow.value.remark = actionComment.value || '审批通过'
    currentRow.value.bucket = 'done'
    ElMessage.success('审批通过')
  } else {
    if (!actionComment.value) {
      ElMessage.warning('请输入驳回原因')
      return
    }
    currentRow.value.status = '已驳回'
    currentRow.value.remark = actionComment.value
    currentRow.value.bucket = 'done'
    ElMessage.success('已驳回')
  }
  showAction.value = false
  refreshStats()
}

function confirmApprove() {
  showDetail.value = false
  openApprove(currentRow.value)
}

function confirmReject() {
  showDetail.value = false
  openReject(currentRow.value)
}

function withdraw(row) {
  ElMessageBox.confirm(`确定撤回「${row.title}」？`, '撤回确认', { type: 'warning' })
    .then(() => { row.status = '已撤回'; ElMessage.success('已撤回'); refreshStats() })
    .catch(() => {})
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.approval-page {
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
.stat-card.stat-todo::before { background: #C4A35A; }
.stat-card.stat-done::before { background: #4A7C59; }
.stat-card.stat-mine::before { background: #2D4A3E; }
.stat-card.stat-cc::before { background: #5B7B8A; }
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
.page-tabs {
  margin-bottom: 4px;
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

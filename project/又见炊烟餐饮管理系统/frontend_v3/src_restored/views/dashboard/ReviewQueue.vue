<template>
  <div class="review-queue-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">入职审核队列 · Onboarding Review</h1>
        <span class="page-desc">自助入职登记审核 · Approve / Reject</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="fetchSubmissions" :loading="loading">
          <el-icon style="margin-right:4px"><Refresh /></el-icon>
          刷新 · Refresh
        </el-button>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card pending">
        <div class="stat-num">{{ stats.pending }}</div>
        <div class="stat-label">待审核 · Pending</div>
      </div>
      <div class="stat-card approved">
        <div class="stat-num">{{ stats.approved }}</div>
        <div class="stat-label">已通过 · Approved</div>
      </div>
      <div class="stat-card rejected">
        <div class="stat-num">{{ stats.rejected }}</div>
        <div class="stat-label">已驳回 · Rejected</div>
      </div>
    </div>

    <!-- 标签页 -->
    <div class="tabs-wrapper">
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane name="pending">
          <template #label>
            <span class="tab-label">
              待审核 · Pending
              <el-badge :value="stats.pending" :hidden="stats.pending === 0" class="tab-badge" />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="approved">
          <template #label>
            <span class="tab-label">
              已通过 · Approved
              <el-badge :value="stats.approved" :hidden="stats.approved === 0" class="tab-badge approved-badge" />
            </span>
          </template>
        </el-tab-pane>
        <el-tab-pane name="rejected">
          <template #label>
            <span class="tab-label">
              已驳回 · Rejected
              <el-badge :value="stats.rejected" :hidden="stats.rejected === 0" class="tab-badge rejected-badge" />
            </span>
          </template>
        </el-tab-pane>
      </el-tabs>
    </div>

    <!-- 列表 -->
    <div class="list-wrapper" v-loading="loading">
      <template v-if="filteredList.length > 0">
        <div
          v-for="item in filteredList"
          :key="item.id"
          class="review-card"
        >
          <div class="card-header">
            <div class="card-meta">
              <span class="card-type" :class="item.submitType === 'new' ? 'type-new' : 'type-update'">
                {{ item.submitType === 'new' ? '新增' : '更新' }}
              </span>
              <span class="card-time">{{ formatTime(item.createdAt) }}</span>
            </div>
            <div class="card-status" v-if="item.status !== 'pending'">
              <el-tag :type="item.status === 'approved' ? 'success' : 'danger'" size="small">
                {{ item.status === 'approved' ? '已通过' : '已驳回' }}
              </el-tag>
            </div>
          </div>

          <div class="card-body">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">姓名</span>
                <span class="info-value">{{ item.name }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">手机号</span>
                <span class="info-value">{{ item.phone }}</span>
              </div>
              <div class="info-item" v-if="item.idCard">
                <span class="info-label">身份证号</span>
                <span class="info-value">{{ maskIdCard(item.idCard) }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">部门</span>
                <span class="info-value">{{ item.department }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">职位</span>
                <span class="info-value">{{ item.position }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">性别</span>
                <span class="info-value">{{ item.gender }}</span>
              </div>
              <div class="info-item info-full" v-if="item.address">
                <span class="info-label">家庭住址</span>
                <span class="info-value">{{ item.address }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">紧急联系人</span>
                <span class="info-value">{{ item.emergencyContact }}</span>
              </div>
              <div class="info-item" v-if="item.emergencyPhone">
                <span class="info-label">紧急联系电话</span>
                <span class="info-value">{{ item.emergencyPhone }}</span>
              </div>
              <div class="info-item info-full" v-if="item.remark">
                <span class="info-label">备注</span>
                <span class="info-value">{{ item.remark }}</span>
              </div>
            </div>

            <!-- 驳回原因 -->
            <div v-if="item.status === 'rejected' && item.rejectNote" class="reject-note">
              <span class="reject-label">驳回原因：</span>
              <span class="reject-text">{{ item.rejectNote }}</span>
            </div>
          </div>

          <!-- 操作按钮（仅待审核） -->
          <div class="card-actions" v-if="item.status === 'pending'">
            <el-button
              type="success"
              :loading="approvingId === item.id"
              @click="handleApprove(item)"
            >
              通过 · Approve
            </el-button>
            <el-button
              type="danger"
              plain
              @click="openRejectDialog(item)"
            >
              驳回 · Reject
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-else description="暂无数据 · No Data" />
    </div>

    <!-- 驳回弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="驳回原因 · Reject Reason"
      width="460px"
      :close-on-click-modal="false"
      destroy-on-close
    >
      <el-form :model="rejectForm" label-position="top">
        <el-form-item label="驳回原因 · Reason" required>
          <el-input
            v-model="rejectForm.note"
            type="textarea"
            :rows="4"
            placeholder="请填写驳回原因..."
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消 · Cancel</el-button>
        <el-button
          type="danger"
          :loading="rejecting"
          @click="handleReject"
          :disabled="!rejectForm.note.trim()"
        >
          确认驳回 · Confirm
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'

const activeTab = ref('pending')
const loading = ref(false)
const approvingId = ref(null)
const rejecting = ref(false)
const rejectDialogVisible = ref(false)
const rejectTarget = ref(null)
const submissions = ref([])

const rejectForm = reactive({
  note: ''
})

const stats = computed(() => {
  const pending = submissions.value.filter(s => s.status === 'pending').length
  const approved = submissions.value.filter(s => s.status === 'approved').length
  const rejected = submissions.value.filter(s => s.status === 'rejected').length
  return { pending, approved, rejected }
})

const filteredList = computed(() => {
  return submissions.value.filter(s => s.status === activeTab.value)
})

const formatTime = (val) => {
  if (!val) return '-'
  const d = new Date(val)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const maskIdCard = (val) => {
  if (!val || val.length < 8) return val
  return val.slice(0, 4) + '**********' + val.slice(-4)
}

const fetchSubmissions = async () => {
  loading.value = true
  try {
    const res = await fetch('/api/hr/self-service/submissions')
    if (!res.ok) throw new Error('获取审核列表失败')
    const data = await res.json()
    submissions.value = Array.isArray(data) ? data : (data.data || [])
  } catch (e) {
    ElMessage.error(e.message || '获取审核列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = () => {
  // tab 切换时无需额外请求
}

const handleApprove = async (item) => {
  try {
    await ElMessageBox.confirm(
      `确认通过「${item.name}」的入职申请吗？通过后将自动写入员工档案。`,
      '确认通过',
      {
        confirmButtonText: '确认通过 · Confirm',
        cancelButtonText: '取消 · Cancel',
        type: 'success'
      }
    )
  } catch {
    return
  }

  approvingId.value = item.id
  try {
    const res = await fetch(`/api/hr/self-service/approve/${item.id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || '审核操作失败')
    }
    ElMessage.success(`已通过 ${item.name} 的入职申请，已写入员工档案`)
    await fetchSubmissions()
  } catch (e) {
    ElMessage.error(e.message || '审核操作失败')
  } finally {
    approvingId.value = null
  }
}

const openRejectDialog = (item) => {
  rejectTarget.value = item
  rejectForm.note = ''
  rejectDialogVisible.value = true
}

const handleReject = async () => {
  if (!rejectForm.note.trim()) {
    ElMessage.warning('请填写驳回原因')
    return
  }

  rejecting.value = true
  try {
    const res = await fetch(`/api/hr/self-service/reject/${rejectTarget.value.id}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ note: rejectForm.note.trim() })
    })
    if (!res.ok) {
      const err = await res.json().catch(() => ({}))
      throw new Error(err.message || '驳回操作失败')
    }
    ElMessage.success(`已驳回 ${rejectTarget.value.name} 的申请`)
    rejectDialogVisible.value = false
    await fetchSubmissions()
  } catch (e) {
    ElMessage.error(e.message || '驳回操作失败')
  } finally {
    rejecting.value = false
  }
}

onMounted(() => {
  fetchSubmissions()
})
</script>

<style scoped>
.review-queue-page {
  padding: 20px 24px;
  min-height: 100vh;
  background: #f5f7f6;
}

/* 顶部 */
.page-topbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.topbar-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #7a8c84;
}

/* 统计 */
.stats-row {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  flex: 1;
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
  border-left: 4px solid #ccc;
}

.stat-card.pending {
  border-left-color: #E6A23C;
}

.stat-card.approved {
  border-left-color: #67C23A;
}

.stat-card.rejected {
  border-left-color: #F56C6C;
}

.stat-num {
  font-size: 32px;
  font-weight: 700;
  color: #2D4A3E;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #7a8c84;
  margin-top: 4px;
}

/* 标签页 */
.tabs-wrapper {
  background: #fff;
  border-radius: 12px;
  padding: 0 20px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
  margin-bottom: 16px;
}

.tabs-wrapper :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.tabs-wrapper :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background-color: #e8edea;
}

.tabs-wrapper :deep(.el-tabs__item) {
  font-size: 14px;
  color: #7a8c84;
  padding: 0 24px;
  height: 48px;
  line-height: 48px;
}

.tabs-wrapper :deep(.el-tabs__item.is-active) {
  color: #2D4A3E;
  font-weight: 600;
}

.tabs-wrapper :deep(.el-tabs__active-bar) {
  background-color: #2D4A3E;
}

.tab-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.tab-badge {
  margin-top: -2px;
}

.tab-badge :deep(.el-badge__content) {
  background-color: #E6A23C;
}

.tab-badge.approved-badge :deep(.el-badge__content) {
  background-color: #67C23A;
}

.tab-badge.rejected-badge :deep(.el-badge__content) {
  background-color: #F56C6C;
}

/* 列表 */
.list-wrapper {
  min-height: 200px;
}

.review-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px 24px;
  margin-bottom: 12px;
  box-shadow: 0 1px 8px rgba(0, 0, 0, 0.04);
  transition: box-shadow 0.2s;
}

.review-card:hover {
  box-shadow: 0 2px 16px rgba(0, 0, 0, 0.08);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #e8edea;
}

.card-meta {
  display: flex;
  align-items: center;
  gap: 12px;
}

.card-type {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.card-type.type-new {
  background: #e8f5e9;
  color: #2D4A3E;
}

.card-type.type-update {
  background: #e3f2fd;
  color: #1565C0;
}

.card-time {
  font-size: 13px;
  color: #999;
}

/* 信息网格 */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 24px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.info-item.info-full {
  grid-column: 1 / -1;
}

.info-label {
  font-size: 12px;
  color: #999;
}

.info-value {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

/* 驳回原因 */
.reject-note {
  margin-top: 14px;
  padding: 12px 16px;
  background: #fef0f0;
  border-radius: 8px;
  border-left: 3px solid #F56C6C;
}

.reject-label {
  font-size: 13px;
  color: #F56C6C;
  font-weight: 600;
}

.reject-text {
  font-size: 13px;
  color: #666;
}

/* 操作按钮 */
.card-actions {
  display: flex;
  gap: 12px;
  margin-top: 16px;
  padding-top: 14px;
  border-top: 1px solid #e8edea;
  justify-content: flex-end;
}

.card-actions .el-button--success {
  --el-button-bg-color: #2D4A3E;
  --el-button-border-color: #2D4A3E;
  --el-button-hover-bg-color: #3a5e4f;
  --el-button-hover-border-color: #3a5e4f;
}

/* 弹窗 */
:deep(.el-dialog__header) {
  border-bottom: 1px solid #e8edea;
  padding: 20px 24px;
}

:deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #2D4A3E;
}

:deep(.el-dialog__body) {
  padding: 24px;
}

:deep(.el-dialog__footer) {
  border-top: 1px solid #e8edea;
  padding: 16px 24px;
}

/* 空状态 */
:deep(.el-empty__description) {
  color: #999;
}
</style>

<template>
  <div class="approval-page">
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">审批中心</h1>
        <span class="page-desc">Approval Center · 统一处理所有部门审批</span>
      </div>
      <div class="topbar-actions">
        <el-select v-model="filterStatus" placeholder="全部状态" clearable style="width:140px" size="default">
          <el-option label="待审批" value="待审批" />
          <el-option label="已通过" value="已通过" />
          <el-option label="已驳回" value="已驳回" />
        </el-select>
        <el-select v-model="filterType" placeholder="全部类型" clearable style="width:140px" size="default">
          <el-option label="请假" value="leave" />
          <el-option label="加班" value="overtime" />
          <el-option label="采购" value="purchase" />
          <el-option label="报损" value="stock_loss" />
        </el-select>
        <el-button @click="refreshData">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/><path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/></svg>
          刷新
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card stat-pending">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#D4A843" stroke-width="2"><circle cx="12" cy="12" r="10"/><polyline points="12 6 12 12 16 14"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ pendingCount }}</span>
          <span class="stat-label">待处理</span>
        </div>
      </div>
      <div class="stat-card stat-approved">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ approvedCount }}</span>
          <span class="stat-label">已通过</span>
        </div>
      </div>
      <div class="stat-card stat-rejected">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#C25555" stroke-width="2"><circle cx="12" cy="12" r="10"/><line x1="15" y1="9" x2="9" y2="15"/><line x1="9" y1="9" x2="15" y2="15"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ rejectedCount }}</span>
          <span class="stat-label">已驳回</span>
        </div>
      </div>
      <div class="stat-card stat-total">
        <div class="stat-icon">
          <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
        </div>
        <div class="stat-info">
          <span class="stat-value">{{ approvalList.length }}</span>
          <span class="stat-label">本月审批</span>
        </div>
      </div>
    </div>

    <!-- 分类标签 -->
    <div class="tab-bar">
      <button
        v-for="tab in tabs"
        :key="tab.key"
        class="tab-btn"
        :class="{ active: activeTab === tab.key }"
        @click="activeTab = tab.key"
      >
        {{ tab.label }}
        <span class="tab-badge" v-if="tab.count > 0">{{ tab.count }}</span>
      </button>
    </div>

    <!-- 审批列表 -->
    <el-table :data="filteredList" stripe class="data-table" v-loading="loading">
      <el-table-column prop="flowNo" label="审批编号" width="220" />
      <el-table-column label="申请类型" width="120">
        <template #default="{ row }">{{ typeLabel(row.flowType) }}</template>
      </el-table-column>
      <el-table-column prop="businessNo" label="业务单号" width="140" />
      <el-table-column label="申请日期" width="160">
        <template #default="{ row }">{{ (row.createdTime || '').replace('T', ' ') }}</template>
      </el-table-column>
      <el-table-column prop="applicantName" label="申请人" width="100" />
      <el-table-column prop="status" label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTag(row.status)" size="small" effect="plain">{{ row.status }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="160">
        <template #default="{ row }">
          <el-button text size="small" @click="viewDetail(row)">详情</el-button>
          <template v-if="row.status === 'pending'">
            <el-button text size="small" type="success" @click="approveItem(row)">通过</el-button>
            <el-button text size="small" type="danger" @click="rejectItem(row)">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条</span>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const approvalList = ref([])
const historyList = ref([])
const filterStatus = ref('')
const filterType = ref('')
const activeTab = ref('all')

// 后端 ApprovalFlow.status 真实值是英文 pending/approved/rejected/cancelled，
// 不是中文，之前用中文比对，审批/驳回按钮从未出现过、状态统计也一直是 0
const STATUS_LABEL = { pending: '待审批', approved: '已通过', rejected: '已驳回', cancelled: '已取消' }
const TYPE_LABEL = { leave: '请假', overtime: '加班', purchase: '采购', stock_loss: '报损' }

function statusLabel(status) {
  return STATUS_LABEL[status] || status
}
function typeLabel(type) {
  return TYPE_LABEL[type] || type
}

// 合并"待审批"(实时) + "历史"(已审批/已驳回)，因为后端 /pending 只返回未处理的
const allFlows = computed(() => [...approvalList.value, ...historyList.value])

const tabs = computed(() => [
  { key: 'all', label: '全部', count: allFlows.value.length },
  { key: 'pending', label: '待审批', count: approvalList.value.length },
  { key: 'approved', label: '已通过', count: historyList.value.filter(a => a.status === 'approved').length },
  { key: 'rejected', label: '已驳回', count: historyList.value.filter(a => a.status === 'rejected').length },
])

const pendingCount = computed(() => approvalList.value.length)
const approvedCount = computed(() => historyList.value.filter(a => a.status === 'approved').length)
const rejectedCount = computed(() => historyList.value.filter(a => a.status === 'rejected').length)

const filteredList = computed(() => {
  let list = allFlows.value
  if (activeTab.value !== 'all') {
    const statusMap = { pending: 'pending', approved: 'approved', rejected: 'rejected' }
    list = list.filter(a => a.status === statusMap[activeTab.value])
  }
  if (filterStatus.value) list = list.filter(a => a.status === filterStatus.value)
  if (filterType.value) list = list.filter(a => a.flowType === filterType.value)
  return list
})

function statusTag(status) {
  return { pending: 'warning', approved: 'success', rejected: 'danger', cancelled: 'info' }[status] || 'info'
}

async function refreshData() {
  loading.value = true
  try {
    const [pendingRes, historyRes] = await Promise.all([
      request.get('/api/approval/pending'),
      request.get('/api/approval/history')
    ])
    approvalList.value = pendingRes.data || []
    historyList.value = historyRes.data || []
  } catch (e) {
    console.error('获取审批列表失败', e)
    ElMessage.error('获取审批列表失败')
    approvalList.value = []
    historyList.value = []
  } finally {
    loading.value = false
  }
}

function viewDetail(row) {
  ElMessage.info(`审批编号：${row.flowNo} · ${typeLabel(row.flowType)} · ${statusLabel(row.status)}`)
}

async function approveItem(row) {
  try {
    await ElMessageBox.confirm(`确定通过「${row.flowNo}」？`, '审批确认', { type: 'success' })
    // 后端路径是 /{flowId} 且只认数字ID，不是业务编号 flowNo
    await request.post(`/api/approval/${row.id}/approve`)
    ElMessage.success('已通过')
    refreshData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('审批操作失败', e)
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

async function rejectItem(row) {
  try {
    await ElMessageBox.confirm(`确定驳回「${row.flowNo}」？`, '驳回确认', { type: 'warning' })
    await request.post(`/api/approval/${row.id}/reject`)
    ElMessage.success('已驳回')
    refreshData()
  } catch (e) {
    if (e !== 'cancel') {
      console.error('驳回操作失败', e)
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

onMounted(() => { refreshData() })
</script>

<style scoped>
.approval-page { max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }
.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }
.topbar-actions { display: flex; gap: 10px; align-items: center; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 20px; }
.stat-card { background: var(--color-card); border: 1px solid var(--color-border); border-radius: 12px; padding: 18px 20px; display: flex; align-items: center; gap: 14px; }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; }
.stat-pending .stat-icon { background: rgba(212,168,67,0.1); }
.stat-approved .stat-icon { background: rgba(74,124,89,0.1); }
.stat-rejected .stat-icon { background: rgba(194,85,85,0.1); }
.stat-total .stat-icon { background: rgba(91,123,138,0.1); }
.stat-info { display: flex; flex-direction: column; gap: 2px; }
.stat-value { font-size: 24px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); }

.tab-bar { display: flex; gap: 8px; margin-bottom: 16px; }
.tab-btn { padding: 8px 18px; border: 1px solid var(--color-border); background: var(--color-card); border-radius: 8px; cursor: pointer; font-size: 13px; color: var(--color-text-secondary); transition: all 0.2s; display: flex; align-items: center; gap: 6px; }
.tab-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.tab-btn.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.tab-badge { font-size: 11px; padding: 1px 6px; border-radius: 10px; background: rgba(255,255,255,0.2); }
.tab-btn:not(.active) .tab-badge { background: var(--color-bg-alt); color: var(--color-text-muted); }

.data-table { border-radius: 2px; overflow: hidden; }
.table-footer { display: flex; align-items: center; justify-content: space-between; margin-top: 12px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }

@media (max-width: 1200px) { .stats-row { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .stats-row { grid-template-columns: 1fr; } .tab-bar { flex-wrap: wrap; } }
</style>

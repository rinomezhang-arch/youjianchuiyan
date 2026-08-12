<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">请假管理 · Leave Management</h1>
        <span class="page-desc">员工请假 · 审批流程 · 考勤记录 · Leave · Approval · Attendance</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="openAdd"><span>新增请假 · Add Leave</span></el-button>
      </div>
    </div>

    <!-- 统计 -->
    <div class="stats-row">
      <div class="stat-card" v-for="s in stats" :key="s.label" :class="s.cls">
        <div class="stat-num">{{ s.value }}</div>
        <div class="stat-label">{{ s.label }}</div>
      </div>
    </div>

    <!-- 搜索过滤 -->
    <div class="filter-bar">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        style="width: 280px"
        value-format="YYYY-MM-DD"
      />
      <el-select v-model="filterDept" placeholder="部门筛选 · Department" clearable style="width:160px">
        <el-option v-for="d in allDepts" :key="d.deptId" :label="d.deptName" :value="d.deptName" />
      </el-select>
      <el-select v-model="filterType" placeholder="请假类型 · Type" clearable style="width:140px">
        <el-option label="事假 · Personal" value="personal" />
        <el-option label="病假 · Sick" value="sick" />
        <el-option label="年假 · Annual" value="annual" />
        <el-option label="调休 · Compensatory" value="compensatory" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态 · Status" clearable style="width:120px">
        <el-option label="待审批 · Pending" value="pending" />
        <el-option label="已通过 · Approved" value="approved" />
        <el-option label="已拒绝 · Rejected" value="rejected" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      :default-sort="{ prop: 'startDate', order: 'descending' }"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="staffName" label="员工姓名 · Name" width="110" />
      <el-table-column prop="department" label="部门 · Dept" width="120" />
      <el-table-column prop="leaveType" label="请假类型 · Type" width="120">
        <template #default="{ row }">
          <el-tag :color="leaveTypeColor(row.leaveType)" size="small" effect="plain" style="color: white; border: none;">{{ leaveTypeLabel(row.leaveType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startDate" label="开始日期 · Start" width="120" sortable />
      <el-table-column prop="endDate" label="结束日期 · End" width="120" />
      <el-table-column prop="days" label="天数 · Days" width="80" align="center" />
      <el-table-column prop="status" label="状态 · Status" width="100" align="center">
        <template #default="{ row }">
          <el-tag :color="statusColor(row.status)" size="small" effect="plain" style="color: white; border: none;">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="reason" label="原因 · Reason" min-width="180" show-overflow-tooltip />
      <el-table-column prop="approver" label="审批人 · Approver" width="110" />
      <el-table-column prop="approveTime" label="审批时间 · Time" width="160" />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条记录 · 右键操作</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxApprove">审批 · Approve</div>
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑请假对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑请假 · Edit Leave' : '新增请假 · Add Leave'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="员工" prop="staffId">
          <el-select v-model="form.staffId" placeholder="选择员工" filterable style="width:100%">
            <el-option v-for="s in staffList" :key="s.staffId" :label="`${s.staffName} (${s.department})`" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="请假类型" prop="leaveType">
          <el-select v-model="form.leaveType" placeholder="选择类型" style="width:100%">
            <el-option label="事假 · Personal" value="personal" />
            <el-option label="病假 · Sick" value="sick" />
            <el-option label="年假 · Annual" value="annual" />
            <el-option label="调休 · Compensatory" value="compensatory" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" placeholder="选择开始日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" placeholder="选择结束日期" style="width:100%" value-format="YYYY-MM-DD" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="请假原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请输入请假原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveLeave" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 审批对话框 -->
    <el-dialog v-model="showApproveDialog" title="审批请假 · Approve Leave" width="500px">
      <el-form :model="approveForm" label-width="100px">
        <el-form-item label="申请人">
          <el-input :model-value="approveForm.staffName" disabled />
        </el-form-item>
        <el-form-item label="请假类型">
          <el-input :model-value="leaveTypeLabel(approveForm.leaveType)" disabled />
        </el-form-item>
        <el-form-item label="请假天数">
          <el-input :model-value="approveForm.days + ' 天'" disabled />
        </el-form-item>
        <el-form-item label="审批结果" prop="status">
          <el-select v-model="approveForm.status" style="width:100%">
            <el-option label="通过 · Approved" value="approved" />
            <el-option label="拒绝 · Rejected" value="rejected" />
          </el-select>
        </el-form-item>
        <el-form-item label="审批备注">
          <el-input v-model="approveForm.remark" type="textarea" :rows="3" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showApproveDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="submitApprove" :loading="approving">确认 · Confirm</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const saving = ref(false)
const approving = ref(false)
const showDialog = ref(false)
const showApproveDialog = ref(false)
const editing = ref(false)
const dateRange = ref([])
const filterDept = ref('')
const filterType = ref('')
const filterStatus = ref('')
const formRef = ref(null)

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0, row: null })
let ctxRow = null

// 数据列表
const list = ref([])
const allDepts = ref([])
const staffList = ref([])

// 获取请假数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/leave')
    list.value = (res.data || []).map(item => ({ ...item, id: item.id ?? item.leaveId }))
  } catch (e) {
    console.error('获取请假数据失败', e)
    ElMessage.error('获取请假数据失败')
  } finally {
    loading.value = false
  }
}

// 获取部门列表
async function fetchDepts() {
  try {
    const res = await request.get('/hr/departments')
    allDepts.value = res.data || []
  } catch (e) {
    console.error('获取部门列表失败', e)
  }
}

// 获取员工列表
async function fetchStaff() {
  try {
    const res = await request.get('/hr/staff')
    staffList.value = res.data || []
  } catch (e) {
    console.error('获取员工列表失败', e)
  }
}

// 表单
const form = ref({
  id: null,
  staffId: '',
  leaveType: '',
  startDate: '',
  endDate: '',
  reason: ''
})

const approveForm = ref({
  id: null,
  staffName: '',
  leaveType: '',
  days: 0,
  status: 'approved',
  remark: ''
})

const rules = {
  staffId: [{ required: true, message: '请选择员工' }],
  leaveType: [{ required: true, message: '请选择请假类型' }],
  startDate: [{ required: true, message: '请选择开始日期' }],
  endDate: [{ required: true, message: '请选择结束日期' }],
  reason: [{ required: true, message: '请输入请假原因' }]
}

// 统计
const stats = computed(() => {
  const total = list.value.length
  const pending = list.value.filter(l => l.status === 'pending').length
  const approved = list.value.filter(l => l.status === 'approved').length
  const rejected = list.value.filter(l => l.status === 'rejected').length
  return [
    { label: '总请假 · Total', value: total, cls: 'st-total' },
    { label: '待审批 · Pending', value: pending, cls: 'st-pending' },
    { label: '已通过 · Approved', value: approved, cls: 'st-approved' },
    { label: '已拒绝 · Rejected', value: rejected, cls: 'st-rejected' }
  ]
})

// 过滤
const filteredList = computed(() => {
  let l = list.value
  
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    l = l.filter(item => {
      return item.startDate >= start && item.endDate <= end
    })
  }
  
  if (filterDept.value) l = l.filter(item => item.department === filterDept.value)
  if (filterType.value) l = l.filter(item => item.leaveType === filterType.value)
  if (filterStatus.value) l = l.filter(item => item.status === filterStatus.value)
  
  return l
})

// 标签颜色
const leaveTypeColor = type => ({
  personal: '#E6A23C',
  sick: '#F56C6C',
  annual: '#67C23A',
  compensatory: '#409EFF'
}[type] || '#909399')

const leaveTypeLabel = type => ({
  personal: '事假',
  sick: '病假',
  annual: '年假',
  compensatory: '调休'
}[type] || type)

const statusColor = status => ({
  pending: '#E6A23C',
  approved: '#67C23A',
  rejected: '#F56C6C'
}[status] || '#909399')

const statusLabel = status => ({
  pending: '待审批',
  approved: '已通过',
  rejected: '已拒绝'
}[status] || status)

// 右键菜单
function onRowMenu(row, column, event) {
  event.preventDefault()
  ctxRow = row
  ctxMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 180),
    y: Math.min(event.clientY, window.innerHeight - 120)
  }
}

function closeMenu() {
  ctxMenu.value.visible = false
  ctxRow = null
}

function ctxApprove() {
  closeMenu()
  if (ctxRow.status !== 'pending') {
    ElMessage.warning('只能审批待审批的请假记录')
    return
  }
  approveForm.value = {
    id: ctxRow.id,
    staffName: ctxRow.staffName,
    leaveType: ctxRow.leaveType,
    days: ctxRow.days,
    status: 'approved',
    remark: ''
  }
  showApproveDialog.value = true
}

function ctxEdit() {
  closeMenu()
  editing.value = true
  form.value = { ...ctxRow }
  showDialog.value = true
}

function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.staffName}」的请假记录？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning'
  })
    .then(async () => {
      try {
        await request.delete(`/hr/leave/${ctxRow.id}`)
        ElMessage.success('已删除 · Deleted')
        await fetchData()
      } catch (e) {
        console.error('删除失败', e)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// 打开新增
function openAdd() {
  editing.value = false
  form.value = {
    id: null,
    staffId: '',
    leaveType: '',
    startDate: '',
    endDate: '',
    reason: ''
  }
  showDialog.value = true
}

// 保存请假
async function saveLeave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  saving.value = true
  try {
    const staff = staffList.value.find(s => s.staffId === form.value.staffId)
    const startDate = new Date(form.value.startDate)
    const endDate = new Date(form.value.endDate)
    const days = Math.ceil((endDate - startDate) / (1000 * 60 * 60 * 24)) + 1

    const leaveData = {
      staffId: form.value.staffId,
      staffName: staff?.staffName || '',
      department: staff?.department || '',
      leaveType: form.value.leaveType,
      startDate: form.value.startDate,
      endDate: form.value.endDate,
      days: days,
      reason: form.value.reason
    }

    if (editing.value) {
      await request.put(`/hr/leave/${form.value.id}`, leaveData)
      ElMessage.success('已更新 · Updated')
    } else {
      await request.post('/hr/leave', leaveData)
      ElMessage.success('已创建 · Created')
    }
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}

// 提交审批
async function submitApprove() {
  approving.value = true
  try {
    await request.put(`/hr/leave/${approveForm.value.id}/approve`, {
      status: approveForm.value.status,
      remark: approveForm.value.remark
    })
    ElMessage.success('审批完成 · Approved')
    showApproveDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('审批失败', e)
    ElMessage.error('审批失败')
  } finally {
    approving.value = false
  }
}

function clearFilters() {
  dateRange.value = []
  filterDept.value = ''
  filterType.value = ''
  filterStatus.value = ''
}

onMounted(() => {
  fetchData()
  fetchDepts()
  fetchStaff()
  document.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenu)
  closeMenu()
})
</script>

<style scoped>
.hr-page { max-width: 1600px; margin: 0 auto; padding-bottom: 40px; }

.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

.stats-row { display: flex; gap: 12px; margin-bottom: 16px; }
.stat-card { flex: 1; padding: 14px 18px; border-radius: 2px; background: var(--color-card); text-align: center; border: 1px solid var(--color-border); }
.st-total { border-color: var(--color-border); }
.st-pending { background: rgba(230,162,60,0.04); border-color: rgba(230,162,60,0.2); }
.st-approved { background: rgba(103,194,58,0.04); border-color: rgba(103,194,58,0.2); }
.st-rejected { background: rgba(245,108,108,0.04); border-color: rgba(245,108,108,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.st-pending .stat-num { color: #E6A23C; }
.st-approved .stat-num { color: #67C23A; }
.st-rejected .stat-num { color: #F56C6C; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }

.data-table { border-radius: 2px; overflow: hidden; }

.table-footer { margin-top: 10px; }
.total-text { font-size: 13px; color: var(--color-text-secondary); }

/* 右键菜单 */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.15);
  padding: 6px;
  min-width: 150px;
}
.ctx-item {
  padding: 8px 14px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 2px;
  color: var(--color-text-primary);
  transition: background 0.1s;
}
.ctx-item:hover {
  background: rgba(45,74,62,0.04);
}
.ctx-item.danger {
  color: #C25555;
}
.ctx-item.danger:hover {
  background: rgba(194,85,85,0.04);
}
.ctx-divider {
  height: 1px;
  background: var(--color-border);
  margin: 4px 8px;
}
</style>

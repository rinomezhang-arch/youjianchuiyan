<template>
  <div class="overtime-page">
    <div class="page-header">
      <div class="header-left">
        <h2 class="page-title">
          <el-icon><Clock /></el-icon>
          加班管理
        </h2>
        <span class="page-subtitle">加班申请录入、查询与审批流程管理</span>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="openFormDialog()">
          <el-icon><Plus /></el-icon>
          新增加班申请
        </el-button>
        <el-button @click="refreshList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>
    </div>

    <el-card class="search-card" shadow="never">
      <el-form :inline="true" :model="searchForm" label-width="90px" size="default">
        <el-form-item label="员工姓名">
          <el-input v-model="searchForm.staffName" placeholder="输入员工姓名筛选" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待审批" value="pending" />
            <el-option label="已通过" value="approved" />
            <el-option label="已拒绝" value="rejected" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="searchForm.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            style="width: 260px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchOvertime" :icon="Search">查询</el-button>
          <el-button @click="resetSearch" :icon="Close">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="stats-card" shadow="never">
      <el-row :gutter="16">
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-num stat-total">{{ stats.total }}</div>
            <div class="stat-label">总记录数</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-num stat-pending">{{ stats.pending }}</div>
            <div class="stat-label">待审批</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-num stat-approved">{{ stats.approved }}</div>
            <div class="stat-label">已通过</div>
          </div>
        </el-col>
        <el-col :span="6">
          <div class="stat-item">
            <div class="stat-num stat-hours">{{ totalHours.toFixed(1) }}h</div>
            <div class="stat-label">累计加班时长</div>
          </div>
        </el-col>
      </el-row>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table :data="filteredList" stripe border v-loading="loading" style="width: 100%">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="overtimeId" label="加班单号" width="110" align="center" />
        <el-table-column prop="staffName" label="员工姓名" width="120">
          <template #default="{ row }">
            <span>{{ row.staffName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="department" label="部门" width="120">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.department || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="overtimeDate" label="加班日期" width="120" align="center" />
        <el-table-column label="加班时段" align="center">
          <template #default="{ row }">
            <span>{{ row.startTime || '-' }} ~ {{ row.endTime || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="hours" label="时长(h)" width="90" align="center">
          <template #default="{ row }">
            <span class="hours-text">{{ row.hours || 0 }}</span>
          </template>
        </el-table-column>
        <el-table-column label="加班费" width="110" align="center">
          <template #default="{ row }">
            <span class="money-text">￥{{ Number(row.overtimeBonus || 0).toFixed(2) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              :type="statusTagType(row.status)"
              size="small"
              effect="light"
              round
            >
              {{ statusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="reason" label="加班事由" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="申请时间" width="170" align="center">
          <template #default="{ row }">
            <span>{{ formatDateTime(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openFormDialog(row)">
              <el-icon><Edit /></el-icon>编辑
            </el-button>
            <el-button
              v-if="row.status === 'pending'"
              type="success"
              link
              size="small"
              @click="openApproveDialog(row, 'approved')"
            >
              <el-icon><CircleCheck /></el-icon>审批
            </el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="empty-tip" v-if="!loading && filteredList.length === 0">
        <el-empty description="暂无加班记录，点击右上角按钮新增申请" />
      </div>
    </el-card>

    <el-dialog
      v-model="formDialogVisible"
      :title="formMode === 'edit' ? '编辑加班申请' : '新增加班申请'"
      width="620px"
      destroy-on-close
    >
      <el-form :model="formData" :rules="formRules" ref="formRef" label-width="110px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="员工" prop="staffId">
              <el-select
                v-model="formData.staffId"
                placeholder="请选择员工"
                filterable
                clearable
                style="width: 100%"
                @change="handleStaffChange"
              >
                <el-option
                  v-for="s in staffList"
                  :key="s.staffId"
                  :label="`${s.staffName}（${s.department || '未分配'}）`"
                  :value="s.staffId"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="加班日期" prop="overtimeDate">
              <el-date-picker
                v-model="formData.overtimeDate"
                type="date"
                value-format="YYYY-MM-DD"
                placeholder="选择加班日期"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="formData.startTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="开始时间"
                style="width: 100%"
                @change="calcHours"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="formData.endTime"
                format="HH:mm"
                value-format="HH:mm"
                placeholder="结束时间"
                style="width: 100%"
                @change="calcHours"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="时长(h)" prop="hours">
              <el-input-number
                v-model="formData.hours"
                :min="0"
                :step="0.5"
                :precision="1"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="薪资倍数">
              <el-select v-model="formData.salaryMultiple" placeholder="默认1.5倍" clearable style="width: 100%">
                <el-option label="平时 (1.5倍)" :value="1.5" />
                <el-option label="休息日 (2倍)" :value="2" />
                <el-option label="法定假日 (3倍)" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="加班费(￥)">
              <el-input-number
                v-model="formData.overtimeBonus"
                :min="0"
                :precision="2"
                :step="10"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="补休方式">
              <el-select v-model="formData.makeUp" placeholder="无补休" clearable style="width: 100%">
                <el-option label="不补休 / 发钱" value="none" />
                <el-option label="1小时补1小时" value="1to1" />
                <el-option label="1小时补1.5小时" value="1to1.5" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="申请状态">
          <el-radio-group v-model="formData.status">
            <el-radio label="pending">待审批</el-radio>
            <el-radio label="approved">直接通过</el-radio>
            <el-radio label="cancelled">已取消</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="加班事由" prop="reason">
          <el-input
            v-model="formData.reason"
            type="textarea"
            :rows="3"
            placeholder="请说明加班原因（如：大型宴会、紧急订单、月度盘点等）"
            maxlength="300"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="approveDialogVisible"
      title="加班审批"
      width="480px"
      destroy-on-close
    >
      <div class="approve-info">
        <el-descriptions :column="1" size="small" border>
          <el-descriptions-item label="员工姓名">
            {{ currentApprovalRow.staffName || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="加班日期">
            {{ currentApprovalRow.overtimeDate }}
          </el-descriptions-item>
          <el-descriptions-item label="时段 / 时长">
            {{ currentApprovalRow.startTime }} ~ {{ currentApprovalRow.endTime }}
            （共 {{ currentApprovalRow.hours || 0 }} 小时）
          </el-descriptions-item>
          <el-descriptions-item label="加班事由">
            {{ currentApprovalRow.reason || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="审批结果">
            <el-tag :type="approveForm.status === 'approved' ? 'success' : 'danger'" size="small" effect="dark">
              {{ approveForm.status === 'approved' ? '通过' : '拒绝' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <el-form :model="approveForm" style="margin-top: 16px" label-width="90px">
          <el-form-item label="审批意见">
            <el-input
              v-model="approveForm.remark"
              type="textarea"
              :rows="3"
              placeholder="请输入审批意见（可选）"
              maxlength="200"
              show-word-limit
            />
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="approveDialogVisible = false">取消</el-button>
        <el-button
          :type="approveForm.status === 'approved' ? 'success' : 'danger'"
          :loading="approving"
          @click="submitApprove"
        >
          <el-icon><Check /></el-icon>
          确认{{ approveForm.status === 'approved' ? '通过' : '拒绝' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Clock, Plus, Refresh, Search, Close, Edit, Delete, CircleCheck, Check
} from '@element-plus/icons-vue'
import {
  getOvertimeList,
  createOvertime,
  updateOvertime,
  deleteOvertime,
  approveOvertime
} from '@/api/hr'
import { getStaffList } from '@/api/hr'

const loading = ref(false)
const submitting = ref(false)
const approving = ref(false)
const list = ref([])
const staffList = ref([])

const searchForm = reactive({
  staffName: '',
  status: '',
  dateRange: []
})

const stats = reactive({ total: 0, pending: 0, approved: 0 })
const totalHours = computed(() => list.value.reduce((sum, r) => sum + Number(r.hours || 0), 0))

const filteredList = computed(() => {
  let arr = list.value
  if (searchForm.staffName.trim()) {
    const kw = searchForm.staffName.trim()
    arr = arr.filter(r => (r.staffName || '').includes(kw))
  }
  if (searchForm.status) {
    arr = arr.filter(r => r.status === searchForm.status)
  }
  if (searchForm.dateRange && searchForm.dateRange.length === 2) {
    const [sd, ed] = searchForm.dateRange
    arr = arr.filter(r => r.overtimeDate && r.overtimeDate >= sd && r.overtimeDate <= ed)
  }
  return arr
})

const formDialogVisible = ref(false)
const formMode = ref('add')
const formRef = ref(null)
const formData = reactive({
  overtimeId: null,
  staffId: null,
  overtimeDate: '',
  startTime: '',
  endTime: '',
  hours: 0,
  salaryMultiple: 1.5,
  overtimeBonus: 0,
  makeUp: 'none',
  status: 'pending',
  reason: ''
})

const formRules = {
  staffId: [{ required: true, message: '请选择员工', trigger: 'change' }],
  overtimeDate: [{ required: true, message: '请选择加班日期', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  hours: [
    { required: true, message: '请填写加班时长', trigger: 'blur' },
    { type: 'number', min: 0.5, message: '时长至少为 0.5 小时', trigger: 'blur' }
  ],
  reason: [{ required: true, message: '请输入加班事由', trigger: 'blur' }]
}

const approveDialogVisible = ref(false)
const currentApprovalRow = reactive({})
const approveForm = reactive({ status: 'approved', remark: '' })

function statusText(status) {
  return {
    pending: '待审批',
    approved: '已通过',
    rejected: '已拒绝',
    cancelled: '已取消'
  }[status] || status || '未知'
}

function statusTagType(status) {
  return {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger',
    cancelled: 'info'
  }[status] || 'info'
}

function formatDateTime(val) {
  if (!val) return '-'
  const d = new Date(val)
  if (isNaN(d)) return val
  const p = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}

function calcHours() {
  if (!formData.startTime || !formData.endTime) return
  const [sh, sm] = formData.startTime.split(':').map(Number)
  const [eh, em] = formData.endTime.split(':').map(Number)
  let diff = (eh * 60 + em) - (sh * 60 + sm)
  if (diff <= 0) diff += 24 * 60
  formData.hours = Number((diff / 60).toFixed(1))
}

function handleStaffChange(val) {
  const s = staffList.value.find(x => x.staffId === val)
  if (s) {
    formData.staffName = s.staffName
    formData.department = s.department
  }
}

async function loadList() {
  loading.value = true
  try {
    const res = await getOvertimeList({ storeId: 1 })
    if (res.code === 0 || res.code === 200) {
      list.value = (res.data || []).map(r => ({
        ...r,
        staffName: r.staffName || '',
        department: r.department || ''
      }))
      stats.total = list.value.length
      stats.pending = list.value.filter(r => r.status === 'pending').length
      stats.approved = list.value.filter(r => r.status === 'approved').length
    } else {
      ElMessage.error(res.message || '获取加班列表失败')
    }
  } catch (e) {
    ElMessage.error('获取加班列表失败')
  } finally {
    loading.value = false
  }
}

async function loadStaff() {
  try {
    const res = await getStaffList({ storeId: 1 })
    if (res.code === 0 || res.code === 200) {
      staffList.value = (res.data || []).map(s => ({
        staffId: s.staffId,
        staffName: s.staffName,
        department: s.department
      }))
    }
  } catch (e) {}
}

function searchOvertime() {
  loadList()
}

function resetSearch() {
  searchForm.staffName = ''
  searchForm.status = ''
  searchForm.dateRange = []
}

function refreshList() {
  loadList()
}

function openFormDialog(row) {
  if (row) {
    formMode.value = 'edit'
    Object.assign(formData, {
      overtimeId: row.overtimeId,
      staffId: row.staffId,
      staffName: row.staffName,
      department: row.department,
      overtimeDate: row.overtimeDate,
      startTime: row.startTime || '',
      endTime: row.endTime || '',
      hours: Number(row.hours || 0),
      salaryMultiple: row.salaryMultiple || 1.5,
      overtimeBonus: Number(row.overtimeBonus || 0),
      makeUp: row.makeUp || 'none',
      status: row.status || 'pending',
      reason: row.reason || ''
    })
  } else {
    formMode.value = 'add'
    Object.assign(formData, {
      overtimeId: null,
      staffId: null,
      staffName: '',
      department: '',
      overtimeDate: '',
      startTime: '',
      endTime: '',
      hours: 0,
      salaryMultiple: 1.5,
      overtimeBonus: 0,
      makeUp: 'none',
      status: 'pending',
      reason: ''
    })
  }
  formDialogVisible.value = true
}

async function submitForm() {
  if (!formRef.value) return
  await formRef.value.validate(async (ok) => {
    if (!ok) return
    submitting.value = true
    try {
      const payload = {
        staffId: formData.staffId,
        overtimeDate: formData.overtimeDate,
        startTime: formData.startTime,
        endTime: formData.endTime,
        hours: formData.hours,
        salaryMultiple: formData.salaryMultiple,
        overtimeBonus: formData.overtimeBonus,
        makeUp: formData.makeUp,
        status: formData.status,
        reason: formData.reason
      }
      const res = formMode.value === 'edit'
        ? await updateOvertime(formData.overtimeId, payload)
        : await createOvertime(payload)
      if (res.code === 0 || res.code === 200) {
        ElMessage.success(formMode.value === 'edit' ? '加班记录已更新' : '加班申请已提交')
        formDialogVisible.value = false
        loadList()
      } else {
        ElMessage.error(res.message || '保存失败')
      }
    } catch (e) {
      ElMessage.error('保存失败：' + (e.message || e))
    } finally {
      submitting.value = false
    }
  })
}

function openApproveDialog(row, status) {
  Object.assign(currentApprovalRow, row)
  approveForm.status = status
  approveForm.remark = ''
  approveDialogVisible.value = true
}

async function submitApprove() {
  approving.value = true
  try {
    const res = await approveOvertime(currentApprovalRow.overtimeId, {
      status: approveForm.status,
      remark: approveForm.remark
    })
    if (res.code === 0 || res.code === 200) {
      ElMessage.success(approveForm.status === 'approved' ? '已通过审批' : '已拒绝')
      approveDialogVisible.value = false
      loadList()
    } else {
      ElMessage.error(res.message || '审批失败')
    }
  } catch (e) {
    ElMessage.error('审批失败：' + (e.message || e))
  } finally {
    approving.value = false
  }
}

async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确认删除「${row.staffName || '该记录'}」的加班申请吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
    const res = await deleteOvertime(row.overtimeId)
    if (res.code === 0 || res.code === 200) {
      ElMessage.success('删除成功')
      loadList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e) {
    if (e !== 'cancel') ElMessage.error('删除失败')
  }
}

onMounted(() => {
  loadStaff()
  loadList()
})
</script>

<style scoped>
.overtime-page {
  padding: 16px;
  background: #f5f7fa;
  min-height: 100%;
}
.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.header-left {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.page-title {
  margin: 0;
  font-size: 22px;
  color: #303133;
  display: flex;
  align-items: center;
  gap: 8px;
}
.page-subtitle {
  font-size: 13px;
  color: #909399;
}
.header-actions {
  display: flex;
  gap: 8px;
}
.search-card, .stats-card, .table-card {
  margin-bottom: 16px;
  border-radius: 10px;
}
.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 12px 8px;
  background: linear-gradient(135deg, #fafbff 0%, #f1f5ff 100%);
  border-radius: 10px;
}
.stat-num {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}
.stat-total { color: #409eff; }
.stat-pending { color: #e6a23c; }
.stat-approved { color: #67c23a; }
.stat-hours { color: #7c3aed; font-size: 22px; }
.stat-label {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
}
.hours-text {
  font-weight: 600;
  color: #7c3aed;
}
.money-text {
  font-weight: 600;
  color: #d97706;
}
.empty-tip {
  padding: 24px 0;
}
.approve-info {
  padding: 4px 0;
}
</style>

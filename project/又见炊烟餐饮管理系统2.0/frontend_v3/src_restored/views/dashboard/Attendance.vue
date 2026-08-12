<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">考勤管理 · Attendance Management</h1>
        <span class="page-desc">考勤记录 · 迟到早退 · 异常统计 · Records · Late/Early · Statistics</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="openAdd"><span>新增考勤 · Add</span></el-button>
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
        range-separator="-"
        start-placeholder="开始日期 · Start"
        end-placeholder="结束日期 · End"
        value-format="YYYY-MM-DD"
        style="width:280px"
      />
      <el-select v-model="filterDept" placeholder="部门筛选 · Dept" clearable style="width:160px">
        <el-option v-for="d in allDepts" :key="d" :label="d" :value="d" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="状态筛选 · Status" clearable style="width:160px">
        <el-option label="正常 · Normal" value="normal" />
        <el-option label="迟到 · Late" value="late" />
        <el-option label="早退 · Early Leave" value="early_leave" />
        <el-option label="旷工 · Absent" value="absent" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      :default-sort="{ prop: 'date', order: 'descending' }"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="staffName" label="员工姓名 · Name" width="110" />
      <el-table-column prop="department" label="部门 · Dept" width="120" />
      <el-table-column prop="date" label="日期 · Date" width="120" sortable />
      <el-table-column prop="checkIn" label="签到时间 · Check In" width="120" />
      <el-table-column prop="checkOut" label="签退时间 · Check Out" width="120" />
      <el-table-column prop="status" label="状态 · Status" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="statusTagType(row.status)" size="small" effect="plain">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lateMinutes" label="迟到分钟 · Late(min)" width="120" align="center">
        <template #default="{ row }">
          <span :class="{ 'highlight-red': row.lateMinutes > 0 }">{{ row.lateMinutes || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="earlyMinutes" label="早退分钟 · Early(min)" width="130" align="center">
        <template #default="{ row }">
          <span :class="{ 'highlight-blue': row.earlyMinutes > 0 }">{{ row.earlyMinutes || '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remark" label="备注 · Remark" min-width="160" show-overflow-tooltip />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条记录 · 右键编辑</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑考勤 · Edit Attendance' : '新增考勤 · Add Attendance'" width="560px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="员工 · Staff" prop="staffName">
          <el-select v-model="form.staffName" placeholder="选择员工" filterable style="width:100%">
            <el-option v-for="name in staffNames" :key="name" :label="name" :value="name" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期 · Date" prop="date">
          <el-date-picker v-model="form.date" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态 · Status" prop="status">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="正常 · Normal" value="normal" />
            <el-option label="迟到 · Late" value="late" />
            <el-option label="早退 · Early Leave" value="early_leave" />
            <el-option label="旷工 · Absent" value="absent" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="签到时间 · In" prop="checkIn">
              <el-time-picker v-model="form.checkIn" placeholder="签到" format="HH:mm" value-format="HH:mm" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="签退时间 · Out" prop="checkOut">
              <el-time-picker v-model="form.checkOut" placeholder="签退" format="HH:mm" value-format="HH:mm" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注 · Remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="可选备注..." />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveRecord" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const showDialog = ref(false)
const editing = ref(false)
const editingId = ref(null)
const formRef = ref(null)
const dateRange = ref([])
const filterDept = ref('')
const filterStatus = ref('')

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0 })
let ctxRow = null

function onRowMenu(row, column, event) {
  event.preventDefault()
  ctxRow = row
  ctxMenu.value = {
    visible: true,
    x: Math.min(event.clientX, window.innerWidth - 180),
    y: Math.min(event.clientY, window.innerHeight - 120),
  }
}

function closeMenu() {
  ctxMenu.value.visible = false
  ctxRow = null
}

function ctxEdit() {
  closeMenu()
  editRow(ctxRow)
}

function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.staffName}」${ctxRow.date} 的考勤记录？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(async () => {
      try {
        await request.delete(`/hr/attendance/${ctxRow.id}`)
        ElMessage.success('已删除 · Deleted')
        await fetchData()
      } catch (e) {
        console.error('删除失败', e)
        ElMessage.error('删除失败')
      }
    })
    .catch(() => {})
}

// 员工列表
const staffNames = ref([])
const deptOptions = ['前厅部', '后厨部', '财务部', '人事部', '管理层']

// 部门列表
const allDepts = ref(deptOptions)

// 数据列表
const list = ref([])

// 获取考勤数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/attendance')
    list.value = res.data || []
  } catch (e) {
    console.error('获取考勤数据失败', e)
    ElMessage.error('获取考勤数据失败')
  } finally {
    loading.value = false
  }
}

// 获取员工列表
async function fetchStaff() {
  try {
    const res = await request.get('/hr/staff')
    staffNames.value = (res.data || []).map(s => s.staffName)
  } catch (e) {
    console.error('获取员工列表失败', e)
  }
}

// 根据员工名获取部门
function getDeptByStaff(name) {
  const found = list.value.find(r => r.staffName === name)
  return found ? found.department : ''
}

// 统计
const stats = computed(() => {
  const total = list.value.length
  const normal = list.value.filter(r => r.status === 'normal').length
  const late = list.value.filter(r => r.status === 'late').length
  const earlyLeave = list.value.filter(r => r.status === 'early_leave').length
  const absent = list.value.filter(r => r.status === 'absent').length
  return [
    { label: '总考勤 · Total', value: total, cls: 'st-total' },
    { label: '正常 · Normal', value: normal, cls: 'st-normal' },
    { label: '迟到 · Late', value: late, cls: 'st-late' },
    { label: '早退 · Early Leave', value: earlyLeave, cls: 'st-early' },
    { label: '旷工 · Absent', value: absent, cls: 'st-absent' },
  ]
})

const filteredList = computed(() => {
  let l = list.value
  if (dateRange.value && dateRange.value.length === 2) {
    const [start, end] = dateRange.value
    l = l.filter(r => r.date >= start && r.date <= end)
  }
  if (filterDept.value) l = l.filter(r => r.department === filterDept.value)
  if (filterStatus.value) l = l.filter(r => r.status === filterStatus.value)
  return l
})

const statusTagType = s => ({ normal: 'success', late: 'warning', early_leave: '', absent: 'danger' }[s] || 'info')
const statusLabel = s => ({ normal: '正常', late: '迟到', early_leave: '早退', absent: '旷工' }[s] || s)

// form
const form = ref({
  staffName: '',
  date: '',
  status: 'normal',
  checkIn: '',
  checkOut: '',
  remark: '',
})

const rules = {
  staffName: [{ required: true, message: '请选择员工' }],
  date: [{ required: true, message: '请选择日期' }],
  status: [{ required: true, message: '请选择状态' }],
}

function openAdd() {
  editing.value = false
  editingId.value = null
  form.value = { staffName: '', date: '', status: 'normal', checkIn: '', checkOut: '', remark: '' }
  showDialog.value = true
}

function editRow(row) {
  editing.value = true
  editingId.value = row.id
  form.value = {
    staffName: row.staffName,
    date: row.date,
    status: row.status,
    checkIn: row.checkIn,
    checkOut: row.checkOut,
    remark: row.remark,
  }
  showDialog.value = true
}

async function saveRecord() {
  await formRef.value?.validate().catch(() => false)
  const dept = getDeptByStaff(form.value.staffName)
  const lateMin = form.value.status === 'late' ? (parseInt(form.value.checkIn?.split(':')[0]) * 60 + parseInt(form.value.checkIn?.split(':')[1] || '0') - 9 * 60) : 0
  const earlyMin = form.value.status === 'early_leave' ? (18 * 60 - (parseInt(form.value.checkOut?.split(':')[0]) * 60 + parseInt(form.value.checkOut?.split(':')[1] || '0'))) : 0

  const data = {
    staffName: form.value.staffName,
    department: dept,
    date: form.value.date,
    checkIn: form.value.checkIn,
    checkOut: form.value.checkOut,
    status: form.value.status,
    lateMinutes: Math.max(lateMin, 0),
    earlyMinutes: Math.max(earlyMin, 0),
    remark: form.value.remark,
  }

  try {
    if (editing.value) {
      await request.put(`/hr/attendance/${editingId.value}`, data)
      ElMessage.success('已更新 · Updated')
    } else {
      await request.post('/hr/attendance', data)
      ElMessage.success('已创建 · Created')
    }
    showDialog.value = false
    await fetchData()
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  }
}

function clearFilters() {
  dateRange.value = []
  filterDept.value = ''
  filterStatus.value = ''
}

onMounted(() => {
  fetchData()
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
.st-normal { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.st-late { background: rgba(196,163,90,0.04); border-color: rgba(196,163,90,0.2); }
.st-early { background: rgba(91,123,138,0.04); border-color: rgba(91,123,138,0.2); }
.st-absent { background: rgba(194,85,85,0.04); border-color: rgba(194,85,85,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.st-late .stat-num { color: #C4A35A; }
.st-absent .stat-num { color: #C25555; }

.filter-bar { display: flex; gap: 10px; margin-bottom: 16px; align-items: center; flex-wrap: wrap; }

.data-table { border-radius: 2px; overflow: hidden; }
.highlight-red { color: #C25555; font-weight: 600; }
.highlight-blue { color: #5B7B8A; font-weight: 600; }

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

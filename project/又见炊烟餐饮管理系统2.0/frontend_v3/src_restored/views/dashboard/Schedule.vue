<template>
  <div class="hr-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">排班管理 · Schedule Management</h1>
        <span class="page-desc">员工排班 · 班次管理 · 考勤统计 · Staff Schedule · Shift · Attendance</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="showBatchDialog = true"><span>批量排班 · Batch</span></el-button>
        <el-button type="primary" @click="openAdd"><span>新增排班 · Add</span></el-button>
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
        v-model="filterDate"
        type="date"
        placeholder="选择日期 · Select Date"
        format="YYYY-MM-DD"
        value-format="YYYY-MM-DD"
        style="width:180px"
      />
      <el-select v-model="filterDept" placeholder="部门筛选 · Dept" clearable style="width:160px">
        <el-option v-for="d in allDepts" :key="d.deptId" :label="d.deptName" :value="d.deptName" />
      </el-select>
      <el-select v-model="filterShift" placeholder="班次筛选 · Shift" clearable style="width:140px">
        <el-option label="早班 · Morning" value="morning" />
        <el-option label="晚班 · Evening" value="evening" />
        <el-option label="全天 · Full" value="full" />
      </el-select>
      <el-button @click="clearFilters" text>清除 · Clear</el-button>
    </div>

    <!-- 表格 -->
    <el-table
      :data="filteredList"
      stripe
      class="data-table"
      v-loading="loading"
      @row-contextmenu="onRowMenu"
    >
      <el-table-column prop="staffName" label="员工姓名 · Staff" width="120" />
      <el-table-column prop="department" label="部门 · Dept" width="120" />
      <el-table-column prop="scheduleDate" label="日期 · Date" width="120" sortable />
      <el-table-column prop="shiftType" label="班次 · Shift" width="110">
        <template #default="{ row }">
          <el-tag :type="shiftTag(row.shiftType)" size="small" effect="plain">{{ shiftLabel(row.shiftType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间 · Start" width="110" />
      <el-table-column prop="endTime" label="结束时间 · End" width="110" />
      <el-table-column prop="remark" label="备注 · Remark" min-width="180" show-overflow-tooltip />
    </el-table>

    <div class="table-footer">
      <span class="total-text">共 {{ filteredList.length }} 条排班 · 右键编辑</span>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
    >
      <div class="ctx-item" @click="ctxEdit">编辑 · Edit</div>
      <div class="ctx-item" @click="ctxBatch">批量排班 · Batch</div>
      <div class="ctx-divider"></div>
      <div class="ctx-item danger" @click="ctxDelete">删除 · Delete</div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog v-model="showDialog" :title="editing ? '编辑排班 · Edit Schedule' : '新增排班 · Add Schedule'" width="600px" destroy-on-close>
      <el-form :model="form" label-width="100px" :rules="rules" ref="formRef">
        <el-form-item label="员工" prop="staffId">
          <el-select v-model="form.staffId" placeholder="选择员工" style="width:100%" filterable>
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName + ' (' + s.department + ')'" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="scheduleDate">
          <el-date-picker
            v-model="form.scheduleDate"
            type="date"
            placeholder="选择日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="班次" prop="shiftType">
          <el-select v-model="form.shiftType" placeholder="选择班次" style="width:100%">
            <el-option label="早班 · Morning" value="morning" />
            <el-option label="晚班 · Evening" value="evening" />
            <el-option label="全天 · Full" value="full" />
          </el-select>
        </el-form-item>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="开始时间" prop="startTime">
              <el-time-picker
                v-model="form.startTime"
                placeholder="选择时间"
                format="HH:mm"
                value-format="HH:mm"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间" prop="endTime">
              <el-time-picker
                v-model="form.endTime"
                placeholder="选择时间"
                format="HH:mm"
                value-format="HH:mm"
                style="width:100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input
            v-model="form.remark"
            type="textarea"
            :rows="3"
            placeholder="可选备注信息 · Optional remark"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveSchedule" :loading="saving">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 批量排班对话框 -->
    <el-dialog v-model="showBatchDialog" title="批量排班 · Batch Schedule" width="600px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="员工">
          <el-select v-model="batchForm.staffIds" multiple placeholder="选择多个员工" style="width:100%" filterable>
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName + ' (' + s.department + ')'" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="batchForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
            style="width:100%"
          />
        </el-form-item>
        <el-form-item label="班次">
          <el-select v-model="batchForm.shiftType" placeholder="选择班次" style="width:100%">
            <el-option label="早班 · Morning" value="morning" />
            <el-option label="晚班 · Evening" value="evening" />
            <el-option label="全天 · Full" value="full" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="batchForm.remark" type="textarea" :rows="2" placeholder="可选备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBatchDialog = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveBatch" :loading="batchSaving">批量保存 · Save All</el-button>
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
const batchSaving = ref(false)
const list = ref([])
const staffList = ref([])
const allDepts = ref([])
const showDialog = ref(false)
const showBatchDialog = ref(false)
const editing = ref(false)
const filterDate = ref(new Date().toISOString().split('T')[0])
const filterDept = ref('')
const filterShift = ref('')
const formRef = ref(null)

// 右键菜单
const ctxMenu = ref({ visible: false, x: 0, y: 0, row: null })
let ctxRow = null

// 获取排班数据
async function fetchData() {
  loading.value = true
  try {
    const res = await request.get('/hr/schedule')
    list.value = res.data || []
  } catch (e) {
    console.error('获取排班数据失败', e)
    ElMessage.error('获取排班数据失败')
  } finally {
    loading.value = false
  }
}

// 获取员工列表
async function fetchStaff() {
  try {
    const res = await request.get('/hr/staff')
    staffList.value = res.data || []
    allDepts.value = [...new Set(staffList.value.map(s => s.department))]
  } catch (e) {
    console.error('获取员工列表失败', e)
  }
}

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

function ctxBatch() {
  closeMenu()
  showBatchDialog.value = true
}

function ctxDelete() {
  closeMenu()
  ElMessageBox.confirm(`确定删除「${ctxRow.staffName}」在 ${ctxRow.scheduleDate} 的排班？此操作不可恢复。`, '删除确认 · Delete Confirm', {
    confirmButtonText: '确定删除 · Delete',
    cancelButtonText: '取消 · Cancel',
    type: 'warning',
  })
    .then(() => removeSchedule(ctxRow))
    .catch(() => {})
}

// ---------- form ----------
const form = ref({
  scheduleId: null,
  staffId: null,
  scheduleDate: '',
  shiftType: '',
  startTime: '',
  endTime: '',
  remark: '',
})

const batchForm = ref({
  staffIds: [],
  dateRange: [],
  shiftType: 'morning',
  remark: '',
})

const rules = {
  staffId: [{ required: true, message: '请选择员工' }],
  scheduleDate: [{ required: true, message: '请选择日期' }],
  shiftType: [{ required: true, message: '请选择班次' }],
  startTime: [{ required: true, message: '请选择开始时间' }],
  endTime: [{ required: true, message: '请选择结束时间' }],
}

const stats = computed(() => {
  const today = new Date().toISOString().split('T')[0]
  const todayCount = list.value.filter(s => s.scheduleDate === today).length
  const morningCount = list.value.filter(s => s.shiftType === 'morning').length
  const eveningCount = list.value.filter(s => s.shiftType === 'evening').length
  const fullCount = list.value.filter(s => s.shiftType === 'full').length
  return [
    { label: '今日排班 · Today', value: todayCount, cls: 'st-total' },
    { label: '早班 · Morning', value: morningCount, cls: 'st-morning' },
    { label: '晚班 · Evening', value: eveningCount, cls: 'st-evening' },
    { label: '全天 · Full', value: fullCount, cls: 'st-full' },
  ]
})

const filteredList = computed(() => {
  let l = list.value
  if (filterDate.value) l = l.filter(s => s.scheduleDate === filterDate.value)
  if (filterDept.value) l = l.filter(s => s.department === filterDept.value)
  if (filterShift.value) l = l.filter(s => s.shiftType === filterShift.value)
  return l
})

const shiftTag = t => ({ morning: 'warning', evening: '', full: 'success' }[t] || 'info')
const shiftLabel = t => ({ morning: '早班', evening: '晚班', full: '全天' }[t] || t)

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
.st-morning { background: rgba(196,163,90,0.04); border-color: rgba(196,163,90,0.2); }
.st-evening { background: rgba(91,123,138,0.04); border-color: rgba(91,123,138,0.2); }
.st-full { background: rgba(45,74,62,0.04); border-color: rgba(45,74,62,0.2); }
.stat-num { font-size: 26px; font-weight: 700; color: var(--color-text-primary); }
.stat-label { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }

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

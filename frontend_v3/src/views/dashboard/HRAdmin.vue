<template>
  <div class="hr-admin-page">
    <div class="page-header">
      <h2 class="page-title">人事行政总览 · HR Overview</h2>
      <p class="page-subtitle">Human Resources & Administration</p>
    </div>

    <div class="stats-row">
      <div class="stat-card" :style="{ color: '#2D4A3E' }">
        <div class="stat-content">
          <div class="stat-label">在岗人数 · On Duty</div>
          <div class="stat-value">{{ stats.activeCount }}</div>
          <div class="stat-sub">总计 {{ stats.total }} 人 · 前厅 {{ stats.foh }} · 后厨 {{ stats.boh }}</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#4A7C59' }">
        <div class="stat-content">
          <div class="stat-label">今日排班 · Schedule</div>
          <div class="stat-value">{{ todaySchedule.length }}</div>
          <div class="stat-sub">早班 {{ stats.morningShift }} · 晚班 {{ stats.eveningShift }}</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#D4A853' }">
        <div class="stat-content">
          <div class="stat-label">请假人数 · On Leave</div>
          <div class="stat-value">{{ todayLeave.length }}</div>
          <div class="stat-sub">事假 {{ stats.personalLeave }} · 病假 {{ stats.sickLeave }}</div>
        </div>
      </div>
      <div class="stat-card" :style="{ color: '#5B7B8A' }">
        <div class="stat-content">
          <div class="stat-label">新入职 · New Hires</div>
          <div class="stat-value">{{ stats.newThisMonth }}</div>
          <div class="stat-sub">本月累计 {{ stats.newThisMonth }} 人</div>
        </div>
      </div>
    </div>

    <div class="quick-actions-card">
      <h3 class="section-title">快捷入口 · Quick Access</h3>
      <div class="action-grid">
        <div class="action-card" @click="goTo('staff')">
          <span class="action-text">员工档案 · Staff</span>
        </div>
        <div class="action-card" @click="goTo('training')">
          <span class="action-text">培训管理 · Training</span>
        </div>
        <div class="action-card" @click="goTo('attendance')">
          <span class="action-text">考勤管理 · Attendance</span>
        </div>
        <div class="action-card" @click="goTo('schedule')">
          <span class="action-text">排班 · Schedule</span>
        </div>
        <div class="action-card" @click="goTo('leave')">
          <span class="action-text">请假 · Leave</span>
        </div>
        <div class="action-card" @click="goTo('license')">
          <span class="action-text">证照管理 · License</span>
        </div>
        <div class="action-card" @click="goTo('security')">
          <span class="action-text">安保保洁 · Security</span>
        </div>
        <div class="action-card" @click="goTo('assets')">
          <span class="action-text">行政资产 · Assets</span>
        </div>
      </div>
    </div>

    <div class="bottom-section">
      <div class="left-section">
        <div class="attendance-card">
          <h3 class="section-title">今日考勤异常 · Attendance Issues</h3>
          <div class="attendance-tabs">
            <button :class="['tab-btn', { active: attendanceTab === 'all' }]" @click="attendanceTab = 'all'">全部 · All</button>
            <button :class="['tab-btn', { active: attendanceTab === 'late' }]" @click="attendanceTab = 'late'">迟到 · Late</button>
            <button :class="['tab-btn', { active: attendanceTab === 'absent' }]" @click="attendanceTab = 'absent'">旷工 · Absent</button>
            <button :class="['tab-btn', { active: attendanceTab === 'early' }]" @click="attendanceTab = 'early'">早退 · Early</button>
          </div>
          <div class="attendance-list" v-loading="loading.attendance">
            <div class="attendance-item" v-for="(item, index) in filteredAttendanceIssues" :key="index">
              <div class="attendance-content">
                <div class="attendance-name">{{ item.name }}</div>
                <div class="attendance-meta">{{ item.department }} · {{ item.time }}</div>
              </div>
              <div class="attendance-status">{{ item.status }}</div>
            </div>
            <el-empty v-if="filteredAttendanceIssues.length === 0" description="暂无异常记录" />
          </div>
        </div>

        <div class="performance-card">
          <h3 class="section-title">部门人员分布 · Department Distribution</h3>
          <div class="performance-grid">
            <div class="performance-item" v-for="(item, index) in deptDistribution" :key="index">
              <div class="perf-header">
                <div class="perf-name">{{ item.name }}</div>
                <div class="perf-score">{{ item.count }}</div>
              </div>
              <div class="perf-bar">
                <div class="perf-fill" :style="{ width: item.percent + '%' }"></div>
              </div>
              <div class="perf-rank">{{ item.percent }}%</div>
            </div>
          </div>
        </div>
      </div>

      <div class="right-section">
        <div class="todo-card">
          <h3 class="section-title">今日请假 · Today Leave</h3>
          <div class="todo-list" v-loading="loading.leave">
            <div class="todo-item" v-for="(item, index) in todayLeave" :key="index">
              <div class="todo-priority" :class="item.leaveType === 'sick' ? 'high' : 'medium'"></div>
              <div class="todo-content">
                <div class="todo-title">{{ item.staffName }} · {{ leaveTypeLabel(item.leaveType) }}</div>
                <div class="todo-meta">{{ item.startDate }} ~ {{ item.endDate }} · {{ item.days }}天</div>
              </div>
              <span class="todo-status" :class="item.status">{{ leaveStatusLabel(item.status) }}</span>
            </div>
            <el-empty v-if="todayLeave.length === 0" description="今日无人请假" />
          </div>
        </div>

        <div class="schedule-card">
          <h3 class="section-title">今日排班 · Today Schedule</h3>
          <div class="schedule-tabs">
            <button :class="['tab-btn', { active: scheduleTab === 'morning' }]" @click="scheduleTab = 'morning'">早班</button>
            <button :class="['tab-btn', { active: scheduleTab === 'evening' }]" @click="scheduleTab = 'evening'">晚班</button>
            <button :class="['tab-btn', { active: scheduleTab === 'all' }]" @click="scheduleTab = 'all'">全部</button>
          </div>
          <div class="schedule-list" v-loading="loading.schedule">
            <div class="schedule-item" v-for="(item, index) in filteredSchedule" :key="index">
              <div class="schedule-info">
                <div class="schedule-name">{{ item.staffName }}</div>
                <div class="schedule-role">{{ item.department }}</div>
              </div>
              <div class="schedule-time">{{ formatScheduleTime(item) }}</div>
            </div>
            <el-empty v-if="filteredSchedule.length === 0" description="暂无排班" />
          </div>
        </div>
      </div>
    </div>

    <!-- 考勤登记弹窗 -->
    <el-dialog v-model="dialogs.attendance" title="考勤登记 · Attendance" width="500px">
      <el-form :model="attendanceForm" label-width="100px">
        <el-form-item label="员工 · Staff">
          <el-select v-model="attendanceForm.staffId" filterable style="width:100%">
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期 · Date">
          <el-date-picker v-model="attendanceForm.attendanceDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态 · Status">
          <el-select v-model="attendanceForm.status" style="width:100%">
            <el-option label="正常 · Normal" value="normal" />
            <el-option label="迟到 · Late" value="late" />
            <el-option label="早退 · Early" value="early_leave" />
            <el-option label="旷工 · Absent" value="absent" />
          </el-select>
        </el-form-item>
        <el-form-item label="签到 · Clock In">
          <el-time-picker v-model="attendanceForm.clockIn" value-format="HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="签退 · Clock Out">
          <el-time-picker v-model="attendanceForm.clockOut" value-format="HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注 · Remark">
          <el-input v-model="attendanceForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogs.attendance = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveAttendance" :loading="saving.attendance">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 请假登记弹窗 -->
    <el-dialog v-model="dialogs.leave" title="请假登记 · Leave" width="500px">
      <el-form :model="leaveForm" label-width="100px">
        <el-form-item label="员工 · Staff">
          <el-select v-model="leaveForm.staffId" filterable style="width:100%">
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="类型 · Type">
          <el-select v-model="leaveForm.leaveType" style="width:100%">
            <el-option label="事假 · Personal" value="personal" />
            <el-option label="病假 · Sick" value="sick" />
            <el-option label="年假 · Annual" value="annual" />
            <el-option label="调休 · Compensatory" value="compensatory" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始 · Start">
          <el-date-picker v-model="leaveForm.startDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束 · End">
          <el-date-picker v-model="leaveForm.endDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="原因 · Reason">
          <el-input v-model="leaveForm.reason" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogs.leave = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveLeave" :loading="saving.leave">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 排班登记弹窗 -->
    <el-dialog v-model="dialogs.schedule" title="排班登记 · Schedule" width="500px">
      <el-form :model="scheduleForm" label-width="100px">
        <el-form-item label="员工 · Staff">
          <el-select v-model="scheduleForm.staffId" filterable style="width:100%">
            <el-option v-for="s in staffList" :key="s.staffId" :label="s.staffName" :value="s.staffId" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期 · Date">
          <el-date-picker v-model="scheduleForm.scheduleDate" type="date" value-format="YYYY-MM-DD" style="width:100%" />
        </el-form-item>
        <el-form-item label="班次 · Shift">
          <el-select v-model="scheduleForm.shiftType" style="width:100%">
            <el-option label="早班 · Morning" value="morning" />
            <el-option label="晚班 · Evening" value="evening" />
            <el-option label="全天 · Full" value="full" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始 · Start">
          <el-time-picker v-model="scheduleForm.startTime" value-format="HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="结束 · End">
          <el-time-picker v-model="scheduleForm.endTime" value-format="HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="备注 · Remark">
          <el-input v-model="scheduleForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogs.schedule = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveSchedule" :loading="saving.schedule">保存 · Save</el-button>
      </template>
    </el-dialog>

    <!-- 右下角操作浮钮 -->
    <div class="fab-container">
      <el-button class="fab-btn" @click="openAttendanceDialog" title="考勤打卡">考勤</el-button>
      <el-button class="fab-btn" @click="openLeaveDialog" title="请假登记">请假</el-button>
      <el-button class="fab-btn" @click="openScheduleDialog" title="排班登记">排班</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  getStaffList,
  getAttendanceList,
  getLeaveList,
  getScheduleList,
  createAttendance,
  createLeave,
  createSchedule
} from '@/api/hr'

const router = useRouter()
const today = new Date().toISOString().split('T')[0]

const staffList = ref([])
const attendanceList = ref([])
const leaveList = ref([])
const scheduleList = ref([])
const attendanceTab = ref('all')
const scheduleTab = ref('all')

const loading = ref({
  staff: false,
  attendance: false,
  leave: false,
  schedule: false
})

const dialogs = ref({
  attendance: false,
  leave: false,
  schedule: false
})

const saving = ref({
  attendance: false,
  leave: false,
  schedule: false
})

const attendanceForm = ref({
  staffId: null,
  attendanceDate: today,
  status: 'normal',
  clockIn: '',
  clockOut: '',
  remark: ''
})

const leaveForm = ref({
  staffId: null,
  leaveType: 'personal',
  startDate: today,
  endDate: today,
  reason: ''
})

const scheduleForm = ref({
  staffId: null,
  scheduleDate: today,
  shiftType: 'morning',
  startTime: '09:00:00',
  endTime: '14:00:00',
  remark: ''
})

const stats = computed(() => {
  const total = staffList.value.length
  const activeCount = staffList.value.filter(s => s.employmentStatus === 'active').length
  const foh = staffList.value.filter(s => s.department === '前厅服务部').length
  const boh = staffList.value.filter(s => s.department === '后厨生产部').length
  const morningShift = todaySchedule.value.filter(s => s.shiftType === 'morning').length
  const eveningShift = todaySchedule.value.filter(s => s.shiftType === 'evening').length
  const personalLeave = todayLeave.value.filter(l => l.leaveType === 'personal').length
  const sickLeave = todayLeave.value.filter(l => l.leaveType === 'sick').length
  const newThisMonth = staffList.value.filter(s => {
    const d = s.createdAt ? s.createdAt.slice(0, 7) : ''
    return d === today.slice(0, 7)
  }).length
  return { total, activeCount, foh, boh, morningShift, eveningShift, personalLeave, sickLeave, newThisMonth }
})

const todayLeave = computed(() => {
  return leaveList.value.filter(l => l.startDate <= today && l.endDate >= today)
})

const todaySchedule = computed(() => {
  return scheduleList.value.filter(s => s.scheduleDate === today)
})

const filteredSchedule = computed(() => {
  if (scheduleTab.value === 'all') return todaySchedule.value
  return todaySchedule.value.filter(s => s.shiftType === scheduleTab.value)
})

const attendanceIssues = computed(() => {
  const issues = []
  attendanceList.value.forEach(a => {
    if (a.status === 'late') {
      issues.push({
        name: a.staffName,
        department: getStaffDept(a.staffId),
        time: a.clockIn || '-',
        status: `迟到 ${a.lateMinutes || 0} 分钟`,
        type: 'late'
      })
    } else if (a.status === 'early_leave') {
      issues.push({
        name: a.staffName,
        department: getStaffDept(a.staffId),
        time: a.clockOut || '-',
        status: `早退 ${a.earlyLeaveMinutes || 0} 分钟`,
        type: 'early'
      })
    } else if (a.status === 'absent' || a.absent) {
      issues.push({
        name: a.staffName,
        department: getStaffDept(a.staffId),
        time: '-',
        status: '旷工',
        type: 'absent'
      })
    }
  })
  return issues
})

const filteredAttendanceIssues = computed(() => {
  if (attendanceTab.value === 'all') return attendanceIssues.value
  return attendanceIssues.value.filter(i => i.type === attendanceTab.value)
})

const deptDistribution = computed(() => {
  const map = {}
  staffList.value.forEach(s => {
    const dept = s.department || '未分配'
    map[dept] = (map[dept] || 0) + 1
  })
  const total = staffList.value.length || 1
  return Object.entries(map).map(([name, count]) => ({
    name,
    count,
    percent: Math.round((count / total) * 100)
  })).sort((a, b) => b.count - a.count)
})

function getNameById(staffId) {
  const s = staffList.value.find(x => (x.staffId || x.staff_id) === staffId)
  return s ? (s.staffName || s.staff_name) : '-'
}

function getStaffDept(staffId) {
  const s = staffList.value.find(x => x.staffId === staffId)
  return s ? s.department : '-'
}

function leaveTypeLabel(type) {
  return { personal: '事假', sick: '病假', annual: '年假', compensatory: '调休' }[type] || type
}

function leaveStatusLabel(status) {
  return { pending: '待审批', approved: '已通过', rejected: '已拒绝' }[status] || status
}

function formatScheduleTime(item) {
  if (!item.startTime || !item.endTime) return '-'
  const s = item.startTime.slice(11, 16)
  const e = item.endTime.slice(11, 16)
  return `${s} - ${e}`
}

function goTo(path) {
  router.push(`/dashboard/${path}`)
}

async function loadData() {
  loading.value.staff = true
  loading.value.attendance = true
  loading.value.leave = true
  loading.value.schedule = true
  try {
    const [staffRes, attRes, leaveRes, schRes] = await Promise.all([
      getStaffList({ storeId: 1 }),
      getAttendanceList({ storeId: 1 }),
      getLeaveList({ storeId: 1 }),
      getScheduleList({ storeId: 1 })
    ])
    staffList.value = (staffRes.data || []).map(normalizeStaff)
    attendanceList.value = attRes.data || []
    leaveList.value = (leaveRes.data || []).map(l => ({
      staffId: l.staff_id ?? l.staffId,
      staffName: l.staff_name ?? l.staffName ?? getNameById(l.staff_id ?? l.staffId),
      leaveType: l.leave_type ?? l.leaveType,
      startDate: l.start_date ?? l.startDate,
      endDate: l.end_date ?? l.endDate,
      days: l.days ?? l.leave_days ?? (l.end_date && l.start_date ? Math.ceil((new Date(l.end_date || l.endDate) - new Date(l.start_date || l.startDate)) / 86400000) + 1 : 1),
      status: l.status ?? 'pending',
      reason: l.reason
    }))
    scheduleList.value = (schRes.data || []).map(s => ({
      staffId: s.staff_id ?? s.staffId,
      staffName: s.staff_name ?? s.staffName ?? getNameById(s.staff_id ?? s.staffId),
      department: s.dept_name ?? s.department ?? getStaffDept(s.staff_id ?? s.staffId),
      scheduleDate: s.schedule_date ?? s.scheduleDate,
      shiftType: s.shift_type ?? s.shiftType,
      startTime: s.start_time ?? s.startTime,
      endTime: s.end_time ?? s.endTime
    }))
  } catch (e) {
    console.error(e)
    ElMessage.error('加载人事数据失败')
  } finally {
    loading.value.staff = false
    loading.value.attendance = false
    loading.value.leave = false
    loading.value.schedule = false
  }
}

// 后端返回 staff_name 驼峰，前端用 staffName——统一映射
function normalizeStaff(s) {
  return {
    staffId: s.staff_id ?? s.staffId,
    staffName: s.staff_name ?? s.staffName,
    staffAccount: s.staff_account ?? s.staffAccount,
    department: s.dept_name ?? s.department ?? '-',
    deptId: s.dept_id ?? s.deptId,
    employmentStatus: s.employment_status ?? s.employmentStatus ?? 'active',
    staffPosition: s.staff_position ?? s.staffPosition,
    staffPhone: s.staff_phone ?? s.staffPhone,
    staffGender: s.staff_gender ?? s.staffGender,
    staffAge: s.staff_age ?? s.staffAge,
    hireDate: s.hire_date ?? s.hireDate,
    createdAt: s.created_at ?? s.createdAt
  }
}

function openAttendanceDialog() {
  attendanceForm.value = {
    staffId: null,
    attendanceDate: today,
    status: 'normal',
    clockIn: '',
    clockOut: '',
    remark: ''
  }
  dialogs.value.attendance = true
}

async function saveAttendance() {
  saving.value.attendance = true
  try {
    await createAttendance(attendanceForm.value)
    ElMessage.success('考勤登记成功')
    dialogs.value.attendance = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.attendance = false
  }
}

function openLeaveDialog() {
  leaveForm.value = {
    staffId: null,
    leaveType: 'personal',
    startDate: today,
    endDate: today,
    reason: ''
  }
  dialogs.value.leave = true
}

async function saveLeave() {
  saving.value.leave = true
  try {
    await createLeave(leaveForm.value)
    ElMessage.success('请假登记成功')
    dialogs.value.leave = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.leave = false
  }
}

function openScheduleDialog() {
  scheduleForm.value = {
    staffId: null,
    scheduleDate: today,
    shiftType: 'morning',
    startTime: '09:00:00',
    endTime: '14:00:00',
    remark: ''
  }
  dialogs.value.schedule = true
}

async function saveSchedule() {
  saving.value.schedule = true
  try {
    const payload = { ...scheduleForm.value }
    const date = payload.scheduleDate
    payload.startTime = `${date}T${payload.startTime}`
    payload.endTime = `${date}T${payload.endTime}`
    await createSchedule(payload)
    ElMessage.success('排班登记成功')
    dialogs.value.schedule = false
    loadData()
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value.schedule = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.hr-admin-page {
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.page-subtitle {
  font-size: 13px;
  color: var(--color-text-secondary);
  margin: 4px 0 0;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  position: relative;
  overflow: hidden;
}

.stat-card::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 80px;
  height: 80px;
  background: currentColor;
  opacity: 0.03;
  border-radius: 0 0 0 80px;
}

.stat-icon {
  width: 48px;
  height: 48px;
  opacity: 0.7;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 4px;
  font-weight: 500;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: var(--color-text-secondary);
  margin-top: 4px;
}

.quick-actions-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 20px;
  margin-bottom: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 16px;
}

.action-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 12px;
}

.action-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  background: var(--color-bg);
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s;
}

.action-card:hover {
  background: rgba(45, 74, 62, 0.04);
  transform: translateY(-2px);
}

.action-icon {
  width: 40px;
  height: 40px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 8px;
}

.action-icon svg {
  width: 22px;
  height: 22px;
}

.action-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  text-align: center;
}

.bottom-section {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.left-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.attendance-card, .performance-card, .todo-card, .schedule-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 20px;
}

.attendance-tabs, .schedule-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--color-border);
  padding-bottom: 8px;
}

.tab-btn {
  padding: 6px 16px;
  font-size: 13px;
  color: var(--color-text-secondary);
  background: transparent;
  border: none;
  border-radius: 2px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn.active {
  color: var(--color-primary);
  background: rgba(45, 74, 62, 0.06);
}

.attendance-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.attendance-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg);
  border-radius: 2px;
}

.attendance-icon {
  width: 36px;
  height: 36px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.attendance-icon.late {
  background: rgba(212, 168, 83, 0.08);
}

.attendance-icon.absent {
  background: rgba(194, 85, 85, 0.08);
}

.attendance-icon.early {
  background: rgba(149, 165, 166, 0.08);
}

.attendance-icon svg {
  width: 18px;
  height: 18px;
}

.attendance-content {
  flex: 1;
}

.attendance-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.attendance-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.attendance-status {
  font-size: 12px;
  font-weight: 500;
  color: #D4A853;
}

.performance-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.performance-item {
  background: var(--color-bg);
  border-radius: 2px;
  padding: 16px;
}

.perf-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 10px;
}

.perf-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.perf-score {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-primary);
}

.perf-bar {
  height: 6px;
  background: var(--color-border);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.perf-fill {
  height: 100%;
  background: var(--color-primary);
  border-radius: 2px;
  transition: width 0.5s ease;
}

.perf-rank {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.right-section {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.todo-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px;
  background: var(--color-bg);
  border-radius: 2px;
}

.todo-priority {
  width: 4px;
  height: 36px;
  border-radius: 2px;
}

.todo-priority.high {
  background: #C25555;
}

.todo-priority.medium {
  background: #D4A853;
}

.todo-priority.low {
  background: #4A7C59;
}

.todo-content {
  flex: 1;
}

.todo-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.todo-meta {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.todo-status {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 2px;
  font-weight: 500;
}

.todo-status.pending {
  background: rgba(212, 168, 83, 0.1);
  color: #D4A853;
}

.todo-status.approved {
  background: rgba(45, 74, 62, 0.1);
  color: #2D4A3E;
}

.todo-status.rejected {
  background: rgba(194, 85, 85, 0.1);
  color: #C25555;
}

.schedule-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.schedule-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;
  background: var(--color-bg);
  border-radius: 2px;
}

.schedule-info {
  flex: 1;
}

.schedule-name {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.schedule-role {
  font-size: 11px;
  color: var(--color-text-secondary);
}

.schedule-time {
  font-size: 12px;
  color: var(--color-text-secondary);
}

@media (max-width: 1200px) {
  .stats-row {
    grid-template-columns: repeat(2, 1fr);
  }
  .action-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  .bottom-section {
    grid-template-columns: 1fr;
  }
}

.fab-container {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  z-index: 999;
}

.fab-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: #2D4A3E;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
  transition: all 0.2s;
}

.fab-btn:hover {
  background: #3A5F4E;
  transform: scale(1.1);
  box-shadow: 0 6px 16px rgba(0,0,0,0.2);
}

@media (max-width: 768px) {
  .stats-row {
    grid-template-columns: 1fr;
  }
  .action-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .performance-grid {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="att-cal-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">考勤日历 · Attendance Calendar</h1>
        <span class="page-desc">月历录入 · 半日考勤 · 结余计算 · Monthly Entry · Half-Day · Balance</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="saveRecord" :loading="saving">
          <span>保存 · Save</span>
        </el-button>
      </div>
    </div>

    <!-- 控制栏 -->
    <div class="control-bar">
      <div class="control-group">
        <label class="control-label">员工 · Staff</label>
        <el-select v-model="selectedEmpId" placeholder="选择员工" filterable @change="onEmpChange" style="width:180px">
          <el-option v-for="e in staffList" :key="e.staff_id" :label="e.staff_name" :value="String(e.staff_id)" />
        </el-select>
      </div>
      <div class="control-group">
        <label class="control-label">月份 · Month</label>
        <el-select v-model="month" @change="onMonthChange" style="width:140px">
          <el-option v-for="m in monthOptions" :key="m" :label="m" :value="m" />
        </el-select>
      </div>
      <div class="control-group">
        <label class="control-label">公休天数 · PH</label>
        <el-input-number v-model="publicHoliday" :min="0" :max="31" size="small" style="width:100px" />
      </div>
      <div class="control-group">
        <label class="control-label">上月结余 · Carry</label>
        <el-input-number v-model="carryOver" :min="0" :max="31" size="small" style="width:100px" />
      </div>
      <div class="control-group">
        <label class="control-label">用工状态 · Emp</label>
        <el-select v-model="employment" style="width:130px">
          <el-option label="全勤在职" value="全勤在职" />
          <el-option label="入职" value="入职" />
          <el-option label="离职" value="离职" />
        </el-select>
      </div>
      <div class="control-group">
        <label class="control-label">薪资 · Salary</label>
        <el-select v-model="salaryStatus" style="width:110px">
          <el-option label="未发放" value="未发放" />
          <el-option label="已发放" value="已发放" />
        </el-select>
      </div>
    </div>

    <!-- 主体：日历 + 统计 -->
    <div class="cal-main">
      <!-- 日历 -->
      <div class="cal-grid-wrap">
        <div class="cal-weekdays">
          <div v-for="d in weekdays" :key="d" class="cal-weekday">{{ d }}</div>
        </div>
        <div class="cal-grid">
          <div
            v-for="(cell, idx) in monthGrid"
            :key="idx"
            :class="['cal-cell', { 'in-month': cell.inMonth, weekend: cell.weekend, today: cell.isToday }]"
          >
            <div class="cal-day-num">{{ cell.day }}</div>
            <div v-if="cell.inMonth" class="cal-halfs">
              <div
                :class="['cal-half', 'cal-am', halfClass(cell.day, 'am')]"
                @click.right.prevent="openMenu($event, cell.day, 'am')"
                @click="openMenu($event, cell.day, 'am')"
              >
                <span class="half-symbol" :style="{ color: halfColor(cell.day, 'am') }">{{ halfSymbol(cell.day, 'am') }}</span>
              </div>
              <div
                :class="['cal-half', 'cal-pm', halfClass(cell.day, 'pm')]"
                @click.right.prevent="openMenu($event, cell.day, 'pm')"
                @click="openMenu($event, cell.day, 'pm')"
              >
                <span class="half-symbol" :style="{ color: halfColor(cell.day, 'pm') }">{{ halfSymbol(cell.day, 'pm') }}</span>
              </div>
            </div>
            <!-- 入职/离职标记 -->
            <div v-if="cell.inMonth && joinDay === cell.day" class="cal-lifecycle join">入职</div>
            <div v-if="cell.inMonth && leaveDay === cell.day" class="cal-lifecycle leave">离职</div>
          </div>
        </div>
      </div>

      <!-- 右侧统计 -->
      <div class="cal-stats">
        <h3 class="stats-title">考勤统计 · Summary</h3>
        <div class="stats-list">
          <div v-for="t in typeList" :key="t.key" class="stats-row">
            <span class="stats-dot" :style="{ background: t.dotColor }"></span>
            <span class="stats-label">{{ t.label }}</span>
            <span class="stats-val">{{ halfCounts[t.key] || 0 }}</span>
            <span class="stats-unit">半天</span>
          </div>
        </div>
        <el-divider />
        <div class="stats-summary">
          <div class="sum-row">
            <span>已录入天数</span>
            <strong>{{ recordedDays }}</strong>
          </div>
          <div class="sum-row">
            <span>公休结余</span>
            <strong :class="{ 'highlight-green': finalBalance >= 0, 'highlight-red': finalBalance < 0 }">{{ finalBalance }}</strong>
          </div>
          <div class="sum-formula">
            = {{ carryOver }} + {{ publicHoliday }} - {{ halfCounts.holiday || 0 }} - {{ halfCounts.statutory || 0 }}
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="ctxMenu.visible"
      class="ctx-menu"
      :style="{ left: ctxMenu.x + 'px', top: ctxMenu.y + 'px' }"
      @click.stop
    >
      <div class="ctx-title">{{ ctxMenu.day }}日 {{ ctxMenu.scope === 'am' ? '上午' : '下午' }}</div>
      <div class="ctx-divider"></div>
      <div
        v-for="t in typeList"
        :key="t.key"
        :class="['ctx-item', { selected: ctxMenu.day && records[ctxMenu.day]?.[ctxMenu.scope]?.type === t.key }]"
        @click="setType(ctxMenu.day, ctxMenu.scope, t.key)"
      >
        <span class="ctx-dot" :style="{ background: t.dotColor }"></span>
        <span>{{ t.label }}</span>
        <span class="ctx-symbol" :style="{ color: t.symbolColor }">{{ t.symbol }}</span>
      </div>
      <div class="ctx-divider"></div>
      <div class="ctx-item" @click="setType(ctxMenu.day, 'full', ctxMenu.day && records[ctxMenu.day]?.am?.type || 'present')">
        <span>📋 全天同上午</span>
      </div>
      <div class="ctx-item" @click="openNoteDialog(ctxMenu.day, ctxMenu.scope)">
        <span>📝 设置备注</span>
      </div>
      <div class="ctx-divider"></div>
      <div class="ctx-item" @click="markLifecycle('join', ctxMenu.day)">
        <span>🏁 标记入职日</span>
      </div>
      <div class="ctx-item" @click="markLifecycle('leave', ctxMenu.day)">
        <span>🚪 标记离职日</span>
      </div>
    </div>

    <!-- 备注弹窗 -->
    <el-dialog v-model="noteDialog.visible" title="设置备注 · Note" width="400px">
      <el-input v-model="noteDialog.text" type="textarea" :rows="3" placeholder="输入备注..." />
      <template #footer>
        <el-button @click="noteDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveNote">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

// ===== 考勤类型定义 =====
const ATT_TYPES = {
  present: { key: 'present', label: '出勤', symbol: '✓', dotColor: '#2D4A3E', symbolColor: '#2D4A3E', bg: 'rgba(45,74,62,0.08)' },
  statutory: { key: 'statutory', label: '法定假日', symbol: '☆', dotColor: '#0D9488', symbolColor: '#0D9488', bg: 'rgba(13,148,136,0.08)' },
  holiday: { key: 'holiday', label: '公休', symbol: '◉', dotColor: '#94A3B8', symbolColor: '#64748B', bg: 'rgba(148,163,184,0.08)' },
  comp: { key: 'comp', label: '补休', symbol: '⊿', dotColor: '#3B82F6', symbolColor: '#2563EB', bg: 'rgba(59,130,246,0.08)' },
  travel: { key: 'travel', label: '出差', symbol: '◆', dotColor: '#06B6D4', symbolColor: '#0891B2', bg: 'rgba(6,182,212,0.08)' },
  overtime: { key: 'overtime', label: '加班', symbol: '✦', dotColor: '#6366F1', symbolColor: '#4F46E5', bg: 'rgba(99,102,241,0.08)' },
  leave: { key: 'leave', label: '请假', symbol: '✕', dotColor: '#C4A35A', symbolColor: '#B8942E', bg: 'rgba(196,163,90,0.08)' },
  late: { key: 'late', label: '迟到', symbol: '▲', dotColor: '#F97316', symbolColor: '#EA580C', bg: 'rgba(249,115,22,0.08)' },
  early: { key: 'early', label: '早退', symbol: '▼', dotColor: '#F43F5E', symbolColor: '#E11D48', bg: 'rgba(244,63,94,0.08)' },
  absent: { key: 'absent', label: '旷工', symbol: '✗', dotColor: '#DC2626', symbolColor: '#DC2626', bg: 'rgba(220,38,38,0.08)' },
}

const typeList = Object.values(ATT_TYPES)

// ===== 星期 =====
const weekdays = ['一', '二', '三', '四', '五', '六', '日']

// ===== 状态 =====
const staffList = ref([])
const selectedEmpId = ref('')
const selectedEmpName = ref('')
const selectedEmpDept = ref('')
const month = ref('')
const monthOptions = ref([])
const publicHoliday = ref(6)
const carryOver = ref(0)
const employment = ref('全勤在职')
const salaryStatus = ref('未发放')
const joinDay = ref(null)
const leaveDay = ref(null)
const records = ref({})
const saving = ref(false)

// 右键菜单
const ctxMenu = reactive({ visible: false, x: 0, y: 0, day: null, scope: 'am' })
const noteDialog = reactive({ visible: false, day: null, scope: 'am', text: '' })

// ===== 生成月份选项 =====
function buildMonthOptions() {
  const now = new Date()
  const opts = []
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    opts.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return opts
}

// ===== 月历网格 =====
const monthGrid = computed(() => {
  if (!month.value) return []
  const [y, m] = month.value.split('-').map(Number)
  const first = new Date(y, m - 1, 1)
  const daysInMonth = new Date(y, m, 0).getDate()
  const daysInPrev = new Date(y, m - 1, 0).getDate()
  // Monday-first
  let leading = (first.getDay() + 6) % 7
  const cells = []
  // 上月填充
  for (let i = leading - 1; i >= 0; i--) {
    cells.push({ day: daysInPrev - i, inMonth: false, weekend: false, isToday: false })
  }
  // 本月
  const now = new Date()
  for (let d = 1; d <= daysInMonth; d++) {
    const dow = new Date(y, m - 1, d).getDay()
    const isToday = y === now.getFullYear() && m - 1 === now.getMonth() && d === now.getDate()
    cells.push({ day: d, inMonth: true, weekend: dow === 0 || dow === 6, isToday })
  }
  // 下月填充
  let trailing = 1
  while (cells.length % 7 !== 0) {
    cells.push({ day: trailing++, inMonth: false, weekend: false, isToday: false })
  }
  return cells
})

// ===== 半格样式 =====
function halfClass(day, scope) {
  const rec = records.value[day]
  if (!rec) return ''
  const type = scope === 'am' ? rec.am_type : rec.pm_type
  return type ? `type-${type}` : ''
}

function halfColor(day, scope) {
  const rec = records.value[day]
  if (!rec) return '#ccc'
  const type = scope === 'am' ? rec.am_type : rec.pm_type
  return type ? (ATT_TYPES[type]?.symbolColor || '#ccc') : '#ccc'
}

function halfSymbol(day, scope) {
  const rec = records.value[day]
  if (!rec) return '·'
  const type = scope === 'am' ? rec.am_type : rec.pm_type
  return type ? (ATT_TYPES[type]?.symbol || '·') : '·'
}

// ===== 半日计数 =====
const halfCounts = computed(() => {
  const c = {}
  typeList.forEach(t => { c[t.key] = 0 })
  Object.values(records.value).forEach(r => {
    if (r.am_type) c[r.am_type] = (c[r.am_type] || 0) + 1
    if (r.pm_type) c[r.pm_type] = (c[r.pm_type] || 0) + 1
  })
  return c
})

const recordedDays = computed(() => {
  return Object.values(records.value).filter(r => r.am_type || r.pm_type).length
})

const finalBalance = computed(() => {
  if (employment.value !== '全勤在职') return carryOver.value
  return carryOver.value + publicHoliday.value - (halfCounts.value.holiday || 0) - (halfCounts.value.statutory || 0)
})

// ===== API =====
async function fetchStaff() {
  try {
    const res = await request.get('/hr/staff')
    staffList.value = res.data || []
  } catch (e) {
    console.error('获取员工列表失败', e)
  }
}

async function fetchRecord() {
  if (!selectedEmpId.value || !month.value) return
  try {
    const res = await request.get('/hr/attendance/record', {
      params: { empId: selectedEmpId.value, month: month.value }
    })
    const data = res.data || {}
    // 转换 records 数组为按 day_num 索引的对象
    const map = {}
    ;(data.records || []).forEach(r => {
      map[r.day_num] = {
        am_type: r.am_type || null,
        pm_type: r.pm_type || null,
        am_note: r.am_note || '',
        pm_note: r.pm_note || '',
        day_note: r.day_note || '',
      }
    })
    records.value = map
    publicHoliday.value = data.publicHoliday ?? 6
    carryOver.value = data.carryOver ?? 0
    employment.value = data.employment || '全勤在职'
    salaryStatus.value = data.salaryStatus || '未发放'
    joinDay.value = data.joinDay || null
    leaveDay.value = data.leaveDay || null
  } catch (e) {
    console.error('获取考勤记录失败', e)
    records.value = {}
  }
}

async function saveRecord() {
  if (!selectedEmpId.value) {
    ElMessage.warning('请先选择员工')
    return
  }
  saving.value = true
  try {
    const recs = Object.entries(records.value).map(([day, r]) => ({
      day_num: Number(day),
      am_type: r.am_type || null,
      pm_type: r.pm_type || null,
      am_note: r.am_note || '',
      pm_note: r.pm_note || '',
      day_note: r.day_note || '',
    }))
    await request.post('/hr/attendance/record', {
      empId: selectedEmpId.value,
      empName: selectedEmpName.value,
      department: selectedEmpDept.value,
      month: month.value,
      records: recs,
      publicHoliday: publicHoliday.value,
      carryOver: carryOver.value,
      employment: employment.value,
      salaryStatus: salaryStatus.value,
      joinDay: joinDay.value,
      leaveDay: leaveDay.value,
      finalBalance: finalBalance.value,
      recordedDays: recordedDays.value,
    })
    ElMessage.success('保存成功 · Saved')
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// ===== 交互 =====
function onEmpChange(val) {
  const emp = staffList.value.find(e => String(e.staff_id) === val)
  if (emp) {
    selectedEmpName.value = emp.staff_name
    selectedEmpDept.value = emp.department || ''
  }
  fetchRecord()
}

function onMonthChange() {
  fetchRecord()
}

function openMenu(e, day, scope) {
  ctxMenu.day = day
  ctxMenu.scope = scope
  ctxMenu.x = Math.min(e.clientX, window.innerWidth - 200)
  ctxMenu.y = Math.min(e.clientY, window.innerHeight - 400)
  ctxMenu.visible = true
}

function setType(day, scope, type) {
  if (!day) return
  if (scope === 'full') {
    if (!records.value[day]) records.value[day] = { am_type: null, pm_type: null, am_note: '', pm_note: '', day_note: '' }
    records.value[day].am_type = type
    records.value[day].pm_type = type
  } else if (scope === 'am') {
    if (!records.value[day]) records.value[day] = { am_type: null, pm_type: null, am_note: '', pm_note: '', day_note: '' }
    records.value[day].am_type = type
  } else {
    if (!records.value[day]) records.value[day] = { am_type: null, pm_type: null, am_note: '', pm_note: '', day_note: '' }
    records.value[day].pm_type = type
  }
  ctxMenu.visible = false
}

function openNoteDialog(day, scope) {
  ctxMenu.visible = false
  noteDialog.day = day
  noteDialog.scope = scope
  const rec = records.value[day]
  if (scope === 'am') noteDialog.text = rec?.am_note || ''
  else if (scope === 'pm') noteDialog.text = rec?.pm_note || ''
  else noteDialog.text = rec?.day_note || ''
  noteDialog.visible = true
}

function saveNote() {
  const { day, scope, text } = noteDialog
  if (!records.value[day]) records.value[day] = { am_type: null, pm_type: null, am_note: '', pm_note: '', day_note: '' }
  if (scope === 'am') records.value[day].am_note = text
  else if (scope === 'pm') records.value[day].pm_note = text
  else records.value[day].day_note = text
  noteDialog.visible = false
}

function markLifecycle(kind, day) {
  ctxMenu.visible = false
  if (kind === 'join') joinDay.value = day
  else leaveDay.value = day
}

function closeMenu() {
  ctxMenu.visible = false
}

// ===== 生命周期 =====
onMounted(() => {
  monthOptions.value = buildMonthOptions()
  month.value = monthOptions.value[0]
  fetchStaff()
  document.addEventListener('click', closeMenu)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMenu)
})
</script>

<style scoped>
.att-cal-page { max-width: 1600px; margin: 0 auto; padding-bottom: 40px; }

.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

.control-bar { display: flex; gap: 16px; margin-bottom: 20px; align-items: flex-end; flex-wrap: wrap; padding: 16px; background: var(--color-card); border-radius: 2px; border: 1px solid var(--color-border); }
.control-group { display: flex; flex-direction: column; gap: 4px; }
.control-label { font-size: 11px; color: var(--color-text-secondary); font-weight: 500; }

.cal-main { display: flex; gap: 20px; align-items: flex-start; }

.cal-grid-wrap { flex: 1; background: var(--color-card); border-radius: 2px; border: 1px solid var(--color-border); overflow: hidden; }

.cal-weekdays { display: grid; grid-template-columns: repeat(7, 1fr); background: rgba(45,74,62,0.06); border-bottom: 1px solid var(--color-border); }
.cal-weekday { text-align: center; padding: 10px 0; font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }

.cal-grid { display: grid; grid-template-columns: repeat(7, 1fr); }
.cal-cell { min-height: 80px; border-right: 1px solid var(--color-border); border-bottom: 1px solid var(--color-border); padding: 2px; position: relative; display: flex; flex-direction: column; }
.cal-cell:nth-child(7n) { border-right: none; }
.cal-cell.weekend { background: rgba(0,0,0,0.02); }
.cal-cell.today { box-shadow: inset 0 0 0 2px #2D4A3E; }
.cal-cell:not(.in-month) { opacity: 0.3; }

.cal-day-num { font-size: 12px; font-weight: 600; color: var(--color-text-secondary); padding: 2px 4px; }

.cal-halfs { flex: 1; display: flex; flex-direction: column; }
.cal-half { flex: 1; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background 0.15s; border-radius: 2px; margin: 1px 2px; }
.cal-half:hover { background: rgba(45,74,62,0.06); }

.half-symbol { font-size: 14px; font-weight: 700; }

/* 考勤类型背景 */
.type-present { background: rgba(45,74,62,0.08); }
.type-statutory { background: rgba(13,148,136,0.08); }
.type-holiday { background: rgba(148,163,184,0.08); }
.type-comp { background: rgba(59,130,246,0.08); }
.type-travel { background: rgba(6,182,212,0.08); }
.type-overtime { background: rgba(99,102,241,0.08); }
.type-leave { background: rgba(196,163,90,0.08); }
.type-late { background: rgba(249,115,22,0.08); }
.type-early { background: rgba(244,63,94,0.08); }
.type-absent { background: rgba(220,38,38,0.08); }

.cal-lifecycle { position: absolute; bottom: 2px; left: 50%; transform: translateX(-50%); font-size: 9px; padding: 1px 4px; border-radius: 2px; white-space: nowrap; }
.cal-lifecycle.join { background: rgba(45,74,62,0.1); color: #2D4A3E; }
.cal-lifecycle.leave { background: rgba(220,38,38,0.1); color: #DC2626; }

/* 统计面板 */
.cal-stats { width: 220px; flex-shrink: 0; background: var(--color-card); border-radius: 2px; border: 1px solid var(--color-border); padding: 16px; }
.stats-title { font-size: 14px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 12px; }
.stats-list { display: flex; flex-direction: column; gap: 6px; }
.stats-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.stats-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.stats-label { flex: 1; color: var(--color-text-secondary); }
.stats-val { font-weight: 600; color: var(--color-text-primary); min-width: 20px; text-align: right; }
.stats-unit { font-size: 11px; color: var(--color-text-muted); }

.stats-summary { margin-top: 4px; }
.sum-row { display: flex; justify-content: space-between; font-size: 14px; padding: 4px 0; }
.sum-row strong { font-size: 16px; }
.highlight-green { color: #2D4A3E; }
.highlight-red { color: #DC2626; }
.sum-formula { font-size: 11px; color: var(--color-text-muted); margin-top: 4px; }

/* 右键菜单 */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  padding: 4px;
  min-width: 180px;
}
.ctx-title { padding: 8px 12px; font-size: 12px; font-weight: 600; color: var(--color-text-secondary); }
.ctx-divider { height: 1px; background: var(--color-border); margin: 4px 8px; }
.ctx-item {
  padding: 7px 12px;
  font-size: 13px;
  cursor: pointer;
  border-radius: 2px;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
  transition: background 0.1s;
}
.ctx-item:hover { background: rgba(45,74,62,0.04); }
.ctx-item.selected { background: rgba(45,74,62,0.08); font-weight: 600; }
.ctx-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ctx-symbol { margin-left: auto; font-weight: 700; font-size: 14px; }
</style>

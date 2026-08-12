<template>
  <div class="att-cal-page">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">考勤日历 · Attendance Calendar</h1>
        <span class="page-desc">月历录入 · 半日考勤 · 公休结余 · Monthly Entry · Half-Day · Balance</span>
      </div>
      <div class="topbar-actions">
        <el-button type="primary" @click="saveRecord" :loading="saving">
          <span>保存 · Save</span>
        </el-button>
        <el-button @click="prevEmployee">
          <span>← 上一人</span>
        </el-button>
        <el-button @click="nextEmployee">
          <span>下一人 →</span>
        </el-button>
      </div>
    </div>

    <!-- 控制栏：员工搜索 / 月份 / 公休 / 结余 / 状态 -->
    <div class="control-bar">
      <!-- 员工选择（带部门分组） -->
      <div class="control-group emp-select-group">
        <label class="control-label">员工 · Staff</label>
        <div class="emp-select-wrap">
          <el-select
            v-model="selectedEmpId"
            filterable
            remote
            :remote-method="searchEmployee"
            :loading="empSearchLoading"
            placeholder="搜索姓名筛选…"
            @change="onEmpSelect"
            style="width:220px"
            clearable
          >
            <el-option
              v-for="e in filteredStaff"
              :key="String(e.staffId || e.staff_id || e.empNo)"
              :label="`${e.staffName || e.staff_name || e.name} (${e.department || e.dept || ''})`"
              :value="String(e.staffId || e.staff_id || e.empNo)"
            >
              <span class="emp-opt-name">{{ e.staffName || e.staff_name || e.name }}</span>
              <span class="emp-opt-dept">{{ e.department || e.dept }}</span>
            </el-option>
          </el-select>
          <span class="emp-name-display" v-if="selectedEmpName">{{ selectedEmpName }} · {{ selectedEmpDept }}</span>
        </div>
      </div>

      <!-- 部门快速筛选 -->
      <div class="control-group">
        <label class="control-label">部门 · Dept</label>
        <el-select v-model="filterDept" placeholder="全部" clearable style="width:140px" @change="filterByDept">
          <el-option v-for="d in deptOptions" :key="d" :label="d" :value="d" />
        </el-select>
      </div>

      <!-- 月份 -->
      <div class="control-group">
        <label class="control-label">月份 · Month</label>
        <div class="month-nav">
          <el-button size="small" @click="prevMonth">‹</el-button>
          <el-select v-model="month" @change="onMonthChange" style="width:120px">
            <el-option v-for="m in monthOptions" :key="m" :label="m" :value="m" />
          </el-select>
          <el-button size="small" @click="nextMonth">›</el-button>
        </div>
      </div>

      <!-- 公休 -->
      <div class="control-group">
        <label class="control-label">公休天数 · PH</label>
        <el-input-number v-model="publicHoliday" :min="0" :max="31" size="small" style="width:100px" />
      </div>

      <!-- 上月结余 -->
      <div class="control-group">
        <label class="control-label">上月结余 · Carry</label>
        <el-input-number v-model="carryOver" :min="-31" :max="31" size="small" style="width:100px" />
        <span v-if="prevBalance !== null" class="balance-hint" @click="carryOver = prevBalance">
          上月结存 {{ prevBalance }} 天 ↻
        </span>
      </div>

      <!-- 用工状态 -->
      <div class="control-group">
        <label class="control-label">用工 · Emp</label>
        <el-select v-model="employment" style="width:110px" @change="onEmploymentChange">
          <el-option label="全勤在职" value="全勤在职" />
          <el-option label="入职" value="入职" />
          <el-option label="离职" value="离职" />
        </el-select>
      </div>

      <!-- 薪资 -->
      <div class="control-group">
        <label class="control-label">薪资 · Salary</label>
        <el-select v-model="salaryStatus" style="width:100px">
          <el-option label="未发放" value="未发放" />
          <el-option label="已发放" value="已发放" />
        </el-select>
      </div>
    </div>

    <!-- 主体：日历 + 统计 -->
    <div class="cal-main">
      <!-- 日历网格 -->
      <div class="cal-grid-wrap">
        <div class="cal-weekdays">
          <div v-for="d in weekdays" :key="d" class="cal-weekday">{{ d }}</div>
        </div>
        <div class="cal-grid">
          <div
            v-for="(cell, idx) in monthGrid"
            :key="idx"
            :class="['cal-cell', {
              'in-month': cell.inMonth,
              'weekend': cell.weekend,
              'today': cell.isToday,
              'cal-join': cell.inMonth && joinDay === cell.day,
              'cal-leave': cell.inMonth && leaveDay === cell.day,
            }]"
          >
            <div class="cal-day-num">
              {{ cell.day }}
              <span v-if="cell.inMonth && joinDay === cell.day" class="lifecycle-tag join">入</span>
              <span v-if="cell.inMonth && leaveDay === cell.day" class="lifecycle-tag leave">离</span>
            </div>
            <div v-if="cell.inMonth" class="cal-halfs">
              <!-- 上午 -->
              <div
                :class="['cal-half', 'cal-am', halfCellClass(cell.day, 'am')]"
                @click="cycleType(cell.day, 'am')"
                @contextmenu.prevent="openCtxMenu($event, cell.day, 'am')"
              >
                <span class="half-symbol" :style="{ color: halfCellColor(cell.day, 'am') }">
                  {{ halfCellSymbol(cell.day, 'am') }}
                </span>
              </div>
              <!-- 下午 -->
              <div
                :class="['cal-half', 'cal-pm', halfCellClass(cell.day, 'pm')]"
                @click="cycleType(cell.day, 'pm')"
                @contextmenu.prevent="openCtxMenu($event, cell.day, 'pm')"
              >
                <span class="half-symbol" :style="{ color: halfCellColor(cell.day, 'pm') }">
                  {{ halfCellSymbol(cell.day, 'pm') }}
                </span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧统计面板 -->
      <div class="cal-stats">
        <h3 class="stats-title">考勤统计 · Summary</h3>
        <div class="stats-list">
          <div v-for="t in TYPE_LIST" :key="t.key" class="stats-row">
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
          <div class="sum-row balance-row">
            <span>公休结余</span>
            <strong :class="finalBalance >= 0 ? 'balance-pos' : 'balance-neg'">
              {{ finalBalance >= 0 ? '+' : '' }}{{ finalBalance }}
            </strong>
          </div>
          <div class="sum-formula">
            = {{ carryOver }} + {{ publicHoliday }} − 公休{{ halfCounts.holiday || 0 }}/2 − 法定{{ halfCounts.statutory || 0 }}/2
          </div>
        </div>

        <!-- 类型快捷键说明 -->
        <el-divider />
        <div class="shortcut-hint">
          <p class="hint-title">快捷键 · Shortcut</p>
          <p class="hint-text">单击半格：循环切换类型</p>
          <p class="hint-text">右键半格：弹出菜单选型</p>
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
        v-for="t in TYPE_LIST"
        :key="t.key"
        :class="['ctx-item', { selected: ctxDayType === t.key }]"
        @click="setType(ctxMenu.day, ctxMenu.scope, t.key)"
      >
        <span class="ctx-dot" :style="{ background: t.dotColor }"></span>
        <span>{{ t.label }}</span>
        <span class="ctx-symbol" :style="{ color: t.symbolColor }">{{ t.symbol }}</span>
      </div>
      <div class="ctx-divider"></div>
      <div class="ctx-item ctx-clear" @click="setType(ctxMenu.day, ctxMenu.scope, null)">
        <span>✕ 清除 · Clear</span>
      </div>
      <div class="ctx-item" @click="setFullDay(ctxMenu.day)">
        <span>📋 同上半天 · Copy AM→PM</span>
      </div>
      <div class="ctx-item" @click="openNoteDialog(ctxMenu.day, ctxMenu.scope)">
        <span>📝 备注 · Note</span>
      </div>
      <div class="ctx-divider"></div>
      <div class="ctx-item" @click="markJoinDay(ctxMenu.day)">
        <span>🏁 {{ joinDay === ctxMenu.day ? '取消' : '标记' }}入职日</span>
      </div>
      <div class="ctx-item" @click="markLeaveDay(ctxMenu.day)">
        <span>🚪 {{ leaveDay === ctxMenu.day ? '取消' : '标记' }}离职日</span>
      </div>
    </div>

    <!-- 备注弹窗 -->
    <el-dialog v-model="noteDialog.visible" title="设置备注 · Note" width="400px">
      <el-input v-model="noteDialog.text" type="textarea" :rows="3" placeholder="输入备注（如迟到原因、请假事由）…" />
      <template #footer>
        <el-button @click="noteDialog.visible = false">取消 · Cancel</el-button>
        <el-button type="primary" @click="saveNote">确定 · OK</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage } from 'element-plus'
import { loadAttendanceRecord, saveAttendanceRecord } from '@/api/hr.js'
import request from '@/utils/request'

// ==================== 考勤类型定义 ====================
const ATT_TYPES = {
  present:   { key:'present',   label:'出勤',     symbol:'✓', dotColor:'#10b981', symbolColor:'#059669', bg:'rgba(16,185,129,0.08)' },
  statutory: { key:'statutory', label:'法定假日', symbol:'★', dotColor:'#0d9488', symbolColor:'#0f766e', bg:'rgba(13,148,136,0.08)' },
  holiday:   { key:'holiday',   label:'公休',     symbol:'○', dotColor:'#94a3b8', symbolColor:'#64748b', bg:'rgba(148,163,184,0.08)' },
  comp:      { key:'comp',      label:'补休',     symbol:'⊙', dotColor:'#3b82f6', symbolColor:'#2563eb', bg:'rgba(59,130,246,0.08)' },
  travel:    { key:'travel',    label:'出差',     symbol:'◇', dotColor:'#06b6d4', symbolColor:'#0891b2', bg:'rgba(6,182,212,0.08)' },
  overtime:  { key:'overtime',  label:'加班',     symbol:'＋', dotColor:'#6366f1', symbolColor:'#4f46e5', bg:'rgba(99,102,241,0.08)' },
  leave:     { key:'leave',     label:'请假',     symbol:'／', dotColor:'#f59e0b', symbolColor:'#d97706', bg:'rgba(245,158,11,0.08)' },
  late:      { key:'late',      label:'迟到',     symbol:'△', dotColor:'#f97316', symbolColor:'#ea580c', bg:'rgba(249,115,22,0.08)' },
  early:     { key:'early',     label:'早退',     symbol:'▲', dotColor:'#f43f5e', symbolColor:'#e11d48', bg:'rgba(244,63,94,0.08)' },
  absent:    { key:'absent',    label:'旷工',     symbol:'✕', dotColor:'#ef4444', symbolColor:'#dc2626', bg:'rgba(239,68,68,0.08)' },
}
const TYPE_LIST = Object.values(ATT_TYPES)
const TYPE_CYCLE = ['present','leave','late','absent','holiday','overtime','comp','travel','early','statutory']

const weekdays = ['一', '二', '三', '四', '五', '六', '日']

// ==================== 状态 ====================
const staffList = ref([])
const filteredStaff = ref([])
const empSearchLoading = ref(false)
const selectedEmpId = ref('')
const selectedEmpName = ref('')
const selectedEmpDept = ref('')
const filterDept = ref('')
const deptOptions = ref([])
const month = ref('')
const monthOptions = ref([])
const publicHoliday = ref(6)
const carryOver = ref(0)
const prevBalance = ref(null)       // 上月结存（建议值）
const employment = ref('全勤在职')
const salaryStatus = ref('未发放')
const joinDay = ref(null)
const leaveDay = ref(null)
// 本地日记录：{ dayNum: { amType, pmType, amNote, pmNote, dayNote } }
const records = ref({})
const saving = ref(false)
const loading = ref(false)

const ctxMenu = reactive({ visible: false, x: 0, y: 0, day: null, scope: 'am' })
const noteDialog = reactive({ visible: false, day: null, scope: 'am', text: '' })

// 用于右键选中高亮
const ctxDayType = computed(() => {
  if (!ctxMenu.visible || ctxMenu.day == null) return null
  const rec = records.value[ctxMenu.day]
  if (!rec) return null
  return ctxMenu.scope === 'am' ? rec.amType : rec.pmType
})

// ==================== 月份生成 ====================
function buildMonthOptions() {
  const now = new Date()
  const opts = []
  // 从上一月开始（考勤次月结算）
  for (let i = 1; i <= 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    opts.push(`${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`)
  }
  return opts
}

function prevMonth() {
  const idx = monthOptions.value.indexOf(month.value)
  if (idx < monthOptions.value.length - 1) {
    month.value = monthOptions.value[idx + 1]
    onMonthChange()
  }
}

function nextMonth() {
  const idx = monthOptions.value.indexOf(month.value)
  if (idx > 0) {
    month.value = monthOptions.value[idx - 1]
    onMonthChange()
  }
}

// ==================== 月历网格 ====================
const monthGrid = computed(() => {
  if (!month.value) return []
  const [y, m] = month.value.split('-').map(Number)
  const first = new Date(y, m - 1, 1)
  const daysInMonth = new Date(y, m, 0).getDate()
  const daysInPrev = new Date(y, m - 1, 0).getDate()
  const leading = (first.getDay() + 6) % 7
  const cells = []
  for (let i = leading - 1; i >= 0; i--) {
    cells.push({ day: daysInPrev - i, inMonth: false, weekend: false, isToday: false })
  }
  const now = new Date()
  for (let d = 1; d <= daysInMonth; d++) {
    const dow = new Date(y, m - 1, d).getDay()
    cells.push({
      day: d,
      inMonth: true,
      weekend: dow === 0 || dow === 6,
      isToday: y === now.getFullYear() && m - 1 === now.getMonth() && d === now.getDate(),
    })
  }
  let trailing = 1
  while (cells.length % 7 !== 0) {
    cells.push({ day: trailing++, inMonth: false, weekend: false, isToday: false })
  }
  return cells
})

// ==================== 半格渲染 ====================
function halfCellClass(day, scope) {
  const rec = records.value[day]
  if (!rec) return ''
  const type = scope === 'am' ? rec.amType : rec.pmType
  return type ? `type-${type}` : ''
}

function halfCellColor(day, scope) {
  const rec = records.value[day]
  if (!rec) return '#ccc'
  const type = scope === 'am' ? rec.amType : rec.pmType
  return type ? (ATT_TYPES[type]?.symbolColor || '#ccc') : '#ccc'
}

function halfCellSymbol(day, scope) {
  const rec = records.value[day]
  if (!rec) return '·'
  const type = scope === 'am' ? rec.amType : rec.pmType
  return type ? (ATT_TYPES[type]?.symbol || '·') : '·'
}

// ==================== 统计计算 ====================
const halfCounts = computed(() => {
  const c = {}
  TYPE_LIST.forEach(t => { c[t.key] = 0 })
  Object.values(records.value).forEach(r => {
    if (r.amType) c[r.amType] = (c[r.amType] || 0) + 1
    if (r.pmType) c[r.pmType] = (c[r.pmType] || 0) + 1
  })
  return c
})

const recordedDays = computed(() => {
  return Object.values(records.value).filter(r => r.amType || r.pmType).length
})

const finalBalance = computed(() => {
  if (employment.value !== '全勤在职') return carryOver.value
  return carryOver.value + publicHoliday.value - (halfCounts.value.holiday || 0)/2 - (halfCounts.value.statutory || 0)/2
})

// ==================== 员工搜索 ====================
function searchEmployee(query) {
  empSearchLoading.value = true
  if (query) {
    const q = query.toLowerCase()
    filteredStaff.value = staffList.value.filter(e =>
      (e.staffName || e.staff_name || e.name || '').toLowerCase().includes(q) ||
      (e.department || e.dept || '').toLowerCase().includes(q)
    )
  } else {
    filteredStaff.value = [...staffList.value]
  }
  empSearchLoading.value = false
}

function filterByDept(dept) {
  if (dept) {
    filteredStaff.value = staffList.value.filter(e =>
      (e.department || e.dept) === dept
    )
  } else {
    filteredStaff.value = [...staffList.value]
  }
}

// 员工导航
function prevEmployee() {
  const idx = filteredStaff.value.findIndex(e => String(e.staffId || e.staff_id || e.empNo) === selectedEmpId.value)
  if (idx > 0) {
    const prev = filteredStaff.value[idx - 1]
    selectEmployee(prev)
  }
}

function nextEmployee() {
  const idx = filteredStaff.value.findIndex(e => String(e.staffId || e.staff_id || e.empNo) === selectedEmpId.value)
  if (idx < filteredStaff.value.length - 1) {
    const next = filteredStaff.value[idx + 1]
    selectEmployee(next)
  }
}

// ==================== 数据加载 ====================
async function fetchStaff() {
  try {
    const res = await request.get('/hr/staff')
    const list = res.data || []
    staffList.value = list
    filteredStaff.value = [...list]
    // 提取部门列表
    const depts = [...new Set(list.map(e => e.department || e.dept || '').filter(Boolean))]
    deptOptions.value = depts
    // 有员工时默认选中第一个
    if (filteredStaff.value.length > 0 && !selectedEmpId.value) {
      selectEmployee(filteredStaff.value[0])
    }
  } catch (e) {
    console.error('获取员工列表失败', e)
  }
}

async function fetchRecord() {
  if (!selectedEmpId.value || !month.value) return
  loading.value = true
  try {
    const res = await loadAttendanceRecord(selectedEmpId.value, month.value)
    const data = res.data
    if (data) {
      // 后端返回 days: [{ dayNum, amType, pmType, amNote, pmNote, dayNote }]
      const map = {}
      ;(data.days || []).forEach(d => {
        map[d.dayNum] = {
          amType: d.amType || null,
          pmType: d.pmType || null,
          amNote: d.amNote || '',
          pmNote: d.pmNote || '',
          dayNote: d.dayNote || '',
        }
      })
      records.value = map
      publicHoliday.value = data.publicHoliday ?? 6
      carryOver.value = data.carryOver ?? 0
      employment.value = data.employment || '全勤在职'
      salaryStatus.value = data.salaryStatus || '未发放'
      joinDay.value = data.joinDay ?? null
      leaveDay.value = data.leaveDay ?? null
      prevBalance.value = null
    }
  } catch (e) {
    if (e.response?.status === 404 || e.message?.includes('未找到')) {
      // 无记录：清零并尝试加载上月结存
      records.value = {}
      publicHoliday.value = 6
      carryOver.value = 0
      employment.value = '全勤在职'
      salaryStatus.value = '未发放'
      joinDay.value = null
      leaveDay.value = null
      // 尝试获取上月结存
      await loadPrevBalance()
    } else {
      console.error('获取考勤记录失败', e)
      records.value = {}
    }
  } finally {
    loading.value = false
  }
}

/** 获取上月结存作为本月建议值 */
async function loadPrevBalance() {
  if (!selectedEmpId.value || !month.value) return
  const [y, m] = month.value.split('-').map(Number)
  const prev = new Date(y, m - 2, 1)
  const prevMonth = `${prev.getFullYear()}-${String(prev.getMonth() + 1).padStart(2, '0')}`
  try {
    const res = await loadAttendanceRecord(selectedEmpId.value, prevMonth)
    const data = res.data
    if (data && data.finalBalance != null) {
      const bal = Math.round(data.finalBalance)
      prevBalance.value = bal
      carryOver.value = bal
    }
  } catch (_) {
    // 无上月记录也没关系
  }
}

function selectEmployee(emp) {
  selectedEmpId.value = String(emp.staffId || emp.staff_id || emp.empNo)
  selectedEmpName.value = emp.staffName || emp.staff_name || emp.name || ''
  selectedEmpDept.value = emp.department || emp.dept || ''
  prevBalance.value = null
  fetchRecord()
}

function onEmpSelect(val) {
  if (!val) {
    selectedEmpName.value = ''
    selectedEmpDept.value = ''
    records.value = {}
    return
  }
  const emp = staffList.value.find(e => String(e.staffId || e.staff_id || e.empNo) === val)
  if (emp) {
    selectedEmpName.value = emp.staffName || emp.staff_name || emp.name || ''
    selectedEmpDept.value = emp.department || emp.dept || ''
  }
  fetchRecord()
}

function onMonthChange() {
  if (selectedEmpId.value) fetchRecord()
}

function onEmploymentChange() {
  // 入职/离职时自动清 joinDay/leaveDay？不处理，让用户手动设置
}

// ==================== 类型交互 ====================
/** 单击循环切换类型 */
function cycleType(day, scope) {
  const rec = records.value[day] || { amType: null, pmType: null, amNote: '', pmNote: '', dayNote: '' }
  if (!records.value[day]) records.value[day] = rec
  const current = scope === 'am' ? rec.amType : rec.pmType
  let next = null
  if (current) {
    const idx = TYPE_CYCLE.indexOf(current)
    if (idx >= 0 && idx < TYPE_CYCLE.length - 1) {
      next = TYPE_CYCLE[idx + 1]
    }
  } else {
    next = TYPE_CYCLE[0] // 空白 → 出勤
  }
  if (scope === 'am') rec.amType = next
  else rec.pmType = next
}

function setType(day, scope, type) {
  if (day == null) return
  const rec = records.value[day] || { amType: null, pmType: null, amNote: '', pmNote: '', dayNote: '' }
  if (!records.value[day]) records.value[day] = rec
  if (scope === 'am') rec.amType = type
  else rec.pmType = type
  ctxMenu.visible = false
}

function setFullDay(day) {
  if (day == null) return
  const rec = records.value[day] || { amType: null, pmType: null, amNote: '', pmNote: '', dayNote: '' }
  if (!records.value[day]) records.value[day] = rec
  rec.pmType = rec.amType
  ctxMenu.visible = false
}

function openCtxMenu(e, day, scope) {
  ctxMenu.day = day
  ctxMenu.scope = scope
  ctxMenu.x = Math.min(e.clientX, window.innerWidth - 220)
  ctxMenu.y = Math.min(e.clientY, window.innerHeight - 460)
  ctxMenu.visible = true
}

function markJoinDay(day) {
  ctxMenu.visible = false
  if (joinDay.value === day) joinDay.value = null
  else joinDay.value = day
  // 标记入职时自动设置 employment
  if (joinDay.value !== null && employment.value === '全勤在职') {
    employment.value = '入职'
  }
}

function markLeaveDay(day) {
  ctxMenu.visible = false
  if (leaveDay.value === day) leaveDay.value = null
  else leaveDay.value = day
  if (leaveDay.value !== null && employment.value === '全勤在职') {
    employment.value = '离职'
  }
}

// 备注
function openNoteDialog(day, scope) {
  ctxMenu.visible = false
  noteDialog.day = day
  noteDialog.scope = scope
  const rec = records.value[day]
  if (!rec) { noteDialog.text = ''; noteDialog.visible = true; return }
  if (scope === 'am') noteDialog.text = rec.amNote || ''
  else noteDialog.text = rec.pmNote || ''
  noteDialog.visible = true
}

function saveNote() {
  const { day, scope, text } = noteDialog
  if (day == null) return
  const rec = records.value[day] || { amType: null, pmType: null, amNote: '', pmNote: '', dayNote: '' }
  if (!records.value[day]) records.value[day] = rec
  if (scope === 'am') rec.amNote = text
  else rec.pmNote = text
  noteDialog.visible = false
}

function closeMenu() { ctxMenu.visible = false }

// ==================== 保存 ====================
async function saveRecord() {
  if (!selectedEmpId.value) {
    ElMessage.warning('请先选择员工')
    return
  }
  saving.value = true
  try {
    // 将 records 转为后端期望的 days 格式 (camelCase)
    const days = Object.entries(records.value).map(([dayNum, r]) => ({
      dayNum: Number(dayNum),
      amType: r.amType || null,
      pmType: r.pmType || null,
      amNote: r.amNote || '',
      pmNote: r.pmNote || '',
      dayNote: r.dayNote || '',
    }))

    await saveAttendanceRecord({
      empId: selectedEmpId.value,
      empName: selectedEmpName.value,
      department: selectedEmpDept.value,
      month: month.value,
      days,
      employment: employment.value,
      salaryStatus: salaryStatus.value,
      publicHoliday: publicHoliday.value,
      carryOver: carryOver.value,
      summaryNotes: '',
      joinDay: joinDay.value,
      leaveDay: leaveDay.value,
      finalBalance: finalBalance.value,
      recordedDays: recordedDays.value,
    })
    ElMessage.success('保存成功 · Saved ✓')
  } catch (e) {
    console.error('保存失败', e)
    ElMessage.error('保存失败 · Save Failed')
  } finally {
    saving.value = false
  }
}

// ==================== 生命周期 ====================
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

/* 顶部栏 */
.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 2px; }

/* 控制栏 */
.control-bar { display: flex; gap: 16px; margin-bottom: 20px; align-items: flex-end; flex-wrap: wrap; padding: 16px; background: var(--color-card); border-radius: 2px; border: 1px solid var(--color-border); }
.control-group { display: flex; flex-direction: column; gap: 4px; }
.control-label { font-size: 11px; color: var(--color-text-secondary); font-weight: 500; text-transform: uppercase; letter-spacing: 0.05em; }

.emp-select-group { min-width: 260px; }
.emp-select-wrap { display: flex; flex-direction: column; gap: 2px; }
.emp-name-display { font-size: 11px; color: var(--color-text-muted); padding-left: 2px; }
.emp-opt-name { font-weight: 500; }
.emp-opt-dept { margin-left: 8px; font-size: 11px; color: #999; }

.month-nav { display: flex; align-items: center; gap: 4px; }

.balance-hint { font-size: 10px; color: #3b82f6; cursor: pointer; text-decoration: underline dotted; }
.balance-hint:hover { color: #1d4ed8; }

/* 日历主体 */
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
.cal-cell.cal-join { border-left: 3px solid #10b981; }
.cal-cell.cal-leave { border-right: 3px solid #ef4444; }

.cal-day-num { font-size: 12px; font-weight: 600; color: var(--color-text-secondary); padding: 2px 4px; display: flex; align-items: center; gap: 3px; }

.lifecycle-tag { font-size: 9px; padding: 0 3px; border-radius: 2px; line-height: 14px; }
.lifecycle-tag.join { background: #10b981; color: #fff; }
.lifecycle-tag.leave { background: #ef4444; color: #fff; }

.cal-halfs { flex: 1; display: flex; flex-direction: column; }
.cal-half { flex: 1; display: flex; align-items: center; justify-content: center; cursor: pointer; transition: background 0.15s; border-radius: 2px; margin: 1px 2px; user-select: none; }
.cal-half:hover { background: rgba(45,74,62,0.06); }
.half-symbol { font-size: 14px; font-weight: 700; pointer-events: none; }

/* 考勤类型背景色 */
.type-present { background: rgba(16,185,129,0.08); }
.type-statutory { background: rgba(13,148,136,0.08); }
.type-holiday { background: rgba(148,163,184,0.08); }
.type-comp { background: rgba(59,130,246,0.08); }
.type-travel { background: rgba(6,182,212,0.08); }
.type-overtime { background: rgba(99,102,241,0.08); }
.type-leave { background: rgba(245,158,11,0.08); }
.type-late { background: rgba(249,115,22,0.08); }
.type-early { background: rgba(244,63,94,0.08); }
.type-absent { background: rgba(239,68,68,0.08); }

/* 统计面板 */
.cal-stats { width: 230px; flex-shrink: 0; background: var(--color-card); border-radius: 2px; border: 1px solid var(--color-border); padding: 16px; position: sticky; top: 100px; }
.stats-title { font-size: 14px; font-weight: 700; color: var(--color-text-primary); margin-bottom: 12px; }
.stats-list { display: flex; flex-direction: column; gap: 5px; }
.stats-row { display: flex; align-items: center; gap: 8px; font-size: 13px; }
.stats-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.stats-label { flex: 1; color: var(--color-text-secondary); }
.stats-val { font-weight: 600; color: var(--color-text-primary); min-width: 22px; text-align: right; }
.stats-unit { font-size: 11px; color: var(--color-text-muted); }

.stats-summary { margin-top: 4px; }
.sum-row { display: flex; justify-content: space-between; font-size: 14px; padding: 4px 0; }
.sum-row strong { font-size: 16px; }
.balance-pos { color: #10b981; }
.balance-neg { color: #ef4444; }
.sum-formula { font-size: 10px; color: var(--color-text-muted); margin-top: 4px; line-height: 1.5; }

.shortcut-hint { margin-top: 4px; }
.hint-title { font-size: 11px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 4px; }
.hint-text { font-size: 10px; color: var(--color-text-muted); line-height: 1.6; }

/* 右键菜单 */
.ctx-menu {
  position: fixed;
  z-index: 9999;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.15);
  padding: 4px;
  min-width: 190px;
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
.ctx-item.ctx-clear { color: #999; font-style: italic; }
.ctx-dot { width: 8px; height: 8px; border-radius: 50%; flex-shrink: 0; }
.ctx-symbol { margin-left: auto; font-weight: 700; font-size: 14px; }

/* Element Plus 覆盖 */
:deep(.el-select-dropdown__item) { font-size: 13px; }
:deep(.el-input-number--small) { width: auto; }
:deep(.el-input-number--small .el-input-number__decrease),
:deep(.el-input-number--small .el-input-number__increase) { width: 22px; }
</style>

<template>
  <div class="payroll-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">工资管理 · Payroll</h2>
        <p class="page-subtitle">工资保存、真人审批与发放记账</p>
      </div>
      <div class="header-actions">
        <div class="month-selector">
          <el-select v-model="selectedMonth" placeholder="选择月份" size="default" :disabled="!!state.busy || state.dirty">
            <el-option
              v-for="m in availableMonths"
              :key="m.value"
              :label="m.label"
              :value="m.value"
            />
          </el-select>
        </div>
        <button
          v-if="!unlocked"
          class="btn-unlock" :disabled="!!state.busy"
          @click="showUnlockDialog = true"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
          解锁查看
        </button>
        <button
          v-else
          class="btn-lock" :disabled="!!state.busy"
          @click="handleLock"
        >
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 9 0v4"/>
            <circle cx="12" cy="16" r="1"/>
          </svg>
          锁定
        </button>
        <span v-if="unlocked" class="unlock-timer">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          {{ countdownText }}
        </span>
      </div>
    </div>

    <p class="workflow-note">{{ PAYROLL_NOTE }}审批仅由当前登录且获授权的真人操作，解锁查看不代表拥有审批权限。</p>
    <p v-if="state.error" role="alert" class="workflow-error">{{ state.error }}</p>
    <p v-if="state.notice" role="status" class="workflow-note">{{ state.notice }}</p>
    <p v-if="state.dirty" class="workflow-note">有未保存输入；合计和个税将在保存回读后更新。切换月份前请保存，或点击刷新并确认放弃修改。</p>
    <p v-if="!state.ready && payrollData.length" class="workflow-error">当前数据尚未确认，请刷新核对；原输入暂时保留，审批和记账已禁用。</p>
    <!-- 汇总卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(45,74,62,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="9" cy="7" r="4"/>
            <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
            <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">总人数 · Headcount</div>
          <div class="stat-value" style="color:#2D4A3E">{{ payrollData.length }}</div>
          <div class="stat-sub">{{ selectedMonth }} 发薪月</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(74,124,89,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
            <line x1="12" y1="1" x2="12" y2="23"/>
            <path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">应发合计 · Gross Total</div>
          <div class="stat-value" style="color:#4A7C59">
            {{ !unlocked ? '****' : state.dirty ? '保存后重算' : '¥' + formatMoney(totalGross) }}
          </div>
          <div class="stat-sub">人均 {{ !unlocked ? '****' : state.dirty ? '保存后重算' : '¥' + formatMoney(avgGross) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(212,168,83,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
            <rect x="2" y="3" width="20" height="18" rx="2"/>
            <path d="M6 8h12"/>
            <path d="M6 12h12"/>
            <path d="M6 16h8"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">实发合计 · Net Total</div>
          <div class="stat-value" style="color:#D4A853">
            {{ !unlocked ? '****' : state.dirty ? '保存后重算' : '¥' + formatMoney(totalNet) }}
          </div>
          <div class="stat-sub">人均 {{ !unlocked ? '****' : state.dirty ? '保存后重算' : '¥' + formatMoney(avgNet) }}</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(91,123,138,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#5B7B8A" stroke-width="2">
            <path d="M12 2L2 7l10 5 10-5-10-5z"/>
            <path d="M2 17l10 5 10-5"/>
            <path d="M2 12l10 5 10-5"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">扣款合计 · Deductions</div>
          <div class="stat-value" style="color:#5B7B8A">
            {{ !unlocked ? '****' : state.dirty ? '保存后重算' : '¥' + formatMoney(totalDeductions) }}
          </div>
          <div class="stat-sub">社保+个税+其他</div>
        </div>
      </div>
    </div>

    <!-- 工资表格 -->
    <div class="table-card">
      <div class="card-header">
        <h3 class="section-title">工资明细 · Payroll Details</h3>
        <div class="card-header-actions">
          <span v-if="payrollStatus" class="payroll-status-tag" :class="payrollStatus">
            {{ payrollStatus }}
          </span>
          <button v-if="unlocked" class="btn-export" :disabled="!permissions.save" @click="handleSavePayroll">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M19 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h11l5 5v11a2 2 0 0 1-2 2z"/>
              <polyline points="17 21 17 13 7 13 7 21"/>
              <polyline points="7 3 7 8 15 8"/>
            </svg>
            {{ saving ? '保存中...' : '核算保存' }}
          </button>
          <button v-if="unlocked" class="btn-export" :disabled="!permissions.approve" @click="handleApprovePayroll">{{ state.busy === 'approve' ? '审批中…' : '真人审批' }}</button>
          <button v-if="unlocked" class="btn-export" :disabled="!permissions.payout" @click="handlePayPayroll">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M20 6L9 17l-5-5"/>
            </svg>
            {{ paying ? '处理中...' : '发放记账' }}
          </button>
          <button class="btn-export" :disabled="!permissions.output || !filteredPayroll.length" @click="handleExport">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            导出筛选结果
          </button>
        </div>
      </div>
      <div class="payroll-filters">
        <el-input v-model="keyword" placeholder="工号 / 姓名筛选" clearable aria-label="工号或姓名筛选" />
        <el-select v-model="departmentFilter" placeholder="全部部门" clearable><el-option v-for="dept in departments" :key="dept" :label="dept" :value="dept" /></el-select>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable><el-option v-for="(label, value) in PAYROLL_STATUS" :key="value" :label="label" :value="value" /></el-select>
        <button class="btn-export" :disabled="!!state.busy" @click="fetchPayroll">刷新回读</button>
        <button class="btn-export" :disabled="!permissions.output || !filteredPayroll.length" @click="handlePrint">打印筛选结果</button>
      </div>
      <div v-if="unlocked" class="payroll-filters">
        <el-select v-model="batchField" aria-label="批量录入项目"><el-option v-for="[field,label] in PAYROLL_FIELDS" :key="field" :label="label" :value="field" /></el-select>
        <el-input v-model="batchValue" placeholder="非负金额，最多两位小数" aria-label="批量录入金额" :disabled="!permissions.edit" />
        <button class="btn-export" :disabled="!permissions.edit" @click="applyBatch">填入筛选内可编辑行</button>
        <span>支持 Tab / Enter 和表格粘贴；保存处理全部可编辑行，审批与记账处理当前权限范围内本月工资，不受筛选影响。</span>
      </div>
      <div class="table-wrapper">
        <el-table
          :data="filteredPayroll" v-loading="!!state.busy"
          border
          stripe
          size="default"
          style="width: 100%"
          :header-cell-style="{ background: '#f5f7f5', color: '#2D4A3E', fontWeight: 600, fontSize: '12px' }"
          :cell-style="{ fontSize: '13px', color: '#3a4a3e' }"
          show-summary
          :summary-method="getSummaries"
        >
          <el-table-column prop="emp_id" label="工号" width="100" fixed="left" align="center" />
          <el-table-column prop="emp_name" label="姓名" width="100" fixed="left" align="center" />
          <el-table-column prop="department" label="部门" width="110" align="center" />
          <el-table-column v-for="[field, label] in PAYROLL_FIELDS" :key="field" :prop="field" :label="label" width="140" align="right">
            <template #default="{ row }">
              <input v-if="unlocked && row.salary_status < 2" v-model="row[field]" class="payroll-cell-input" inputmode="decimal"
                :aria-label="`${row.emp_name} ${label}`" :disabled="!permissions.edit" @input="state.dirty = true"
                @keydown.enter.prevent="focusNextInput" @paste="pasteCells($event, row, field)" />
              <span v-else>{{ unlocked ? formatMoney(row[field]) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deduction_tax" label="个税代扣" width="120" align="right">
            <template #default="{row}">{{ !unlocked ? '****' : state.dirty ? '保存后重算' : formatMoney(row.deduction_tax) }}</template>
          </el-table-column>
          <el-table-column prop="salary_status" label="状态" width="130" align="center"><template #default="{row}">{{ PAYROLL_STATUS[row.salary_status] }}</template></el-table-column>
          <el-table-column prop="gross_pay" label="应发合计" width="130" align="right" fixed="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'gross': true }">{{ !unlocked ? '****' : state.dirty ? '保存后重算' : formatMoney(row.gross_pay) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="net_pay" label="实发合计" width="130" align="right" fixed="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'net': true }">{{ !unlocked ? '****' : state.dirty ? '保存后重算' : formatMoney(row.net_pay) }}</span>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>

    <!-- 解锁弹窗 -->
    <el-dialog
      v-model="showUnlockDialog"
      title="验证身份"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      class="unlock-dialog"
    >
      <div class="dialog-body">
        <div class="lock-icon-wrap">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2" width="48" height="48">
            <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
            <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
          </svg>
        </div>
        <p class="dialog-desc">请输入配置的验证码解锁查看；审批权限由登录身份决定</p>
        <el-input
          v-model="unlockCode"
          placeholder="请输入验证码"
          type="password"
          size="large"
          maxlength="20"
          show-password
          @keyup.enter="handleUnlock"
          :class="{ 'is-error': unlockError }"
        >
          <template #prefix>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="16" height="16">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              <circle cx="12" cy="16" r="1"/>
            </svg>
          </template>
        </el-input>
        <p v-if="unlockError" class="error-msg">{{ unlockError }}</p>
      </div>
      <template #footer>
        <el-button @click="showUnlockDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUnlock" :loading="unlocking" class="btn-confirm">
          确认解锁
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '@/utils/request'
import { PAYROLL_FIELDS, PAYROLL_STATUS, PAYROLL_NOTE, createPayrollActions, payrollPermissions,
  payrollStatusText, filterPayrollRows, payrollCsv, payrollPrintHtml } from '@/utils/payrollActions'

const now = new Date()
const selectedMonth = ref(`${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`)
const state = reactive({ month: selectedMonth.value, loadedMonth: '', rows: [], ready: false, dirty: false, busy: '', error: '', notice: '', unlocked: false })
const actions = createPayrollActions(state, request)
const payrollData = computed(() => state.rows)
const permissions = computed(() => payrollPermissions(state))
const payrollStatus = computed(() => payrollStatusText(state.rows))
const unlocked = computed(() => state.unlocked)
const saving = computed(() => state.busy === 'save')
const paying = computed(() => state.busy === 'payout')
const unlocking = computed(() => state.busy === 'unlock')
const keyword = ref(''), departmentFilter = ref(''), statusFilter = ref('')
const departments = computed(() => [...new Set(state.rows.map(row => row.department).filter(Boolean))])
const filteredPayroll = computed(() => filterPayrollRows(state.rows, keyword.value, departmentFilter.value, statusFilter.value))
const batchField = ref('bonus'), batchValue = ref('')
const showUnlockDialog = ref(false), unlockCode = ref(''), unlockError = ref(''), unlockToken = ref('')
const countdownSeconds = ref(0)
let countdownTimer = null, printFrame = null
let disposed = false
const availableMonths = computed(() => Array.from({ length: 24 }, (_, i) => {
  const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
  return { value: `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`, label: `${d.getFullYear()}年${d.getMonth() + 1}月` }
}))
const countdownText = computed(() => `${Math.floor(countdownSeconds.value / 60)}分${String(countdownSeconds.value % 60).padStart(2, '0')}秒后锁定`)
const formatMoney = value => Number.isFinite(Number(value)) ? Number(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) : '输入无效'
const sum = key => state.rows.reduce((total, row) => total + Number(row[key] || 0), 0)
const totalGross = computed(() => sum('gross_pay'))
const totalNet = computed(() => sum('net_pay'))
const totalDeductions = computed(() => sum('deduction_social') + sum('deduction_tax') + sum('deduction_other'))
const avgGross = computed(() => state.rows.length ? totalGross.value / state.rows.length : 0)
const avgNet = computed(() => state.rows.length ? totalNet.value / state.rows.length : 0)
function getSummaries({ columns, data }) {
  return columns.map((column, i) => {
    if (i === 0) return '筛选合计'
    if (i === 1) return `${data.length}人`
    if (!column.property || ['department', 'salary_status'].includes(column.property)) return ''
    if (!unlocked.value) return '****'
    if (state.dirty && ['gross_pay', 'net_pay', 'deduction_tax'].includes(column.property)) return '保存后重算'
    return formatMoney(data.reduce((total, row) => total + Number(row[column.property]), 0))
  })
}
async function fetchPayroll() {
  if (state.busy) return
  if (state.dirty) {
    state.busy = 'confirmRefresh'
    try { await ElMessageBox.confirm('刷新成功后将替换当前未保存输入，是否继续？', '刷新回读') }
    catch { return }
    finally { state.busy = '' }
  }
  await actions.refresh()
}
watch(selectedMonth, month => {
  actions.invalidate(); clearPrint(); state.month = month; state.rows = []; state.dirty = false; state.notice = ''
  actions.refresh()
}, { flush: 'sync' })
const confirmation = async (action, month) => {
  const label = { save: '核算保存', approve: '真人审批', payout: '发放记账' }[action]
  const detail = action === 'approve' ? '请由当前登录的授权真人核对后审批，不可代他人确认。'
    : action === 'payout' ? PAYROLL_NOTE : '保存所有可编辑行，合计和个税由服务端重算；已审批或已记账行不修改。'
  await ElMessageBox.confirm(`${month}：${detail}`, label, { confirmButtonText: label, cancelButtonText: '取消', type: 'warning' })
  return true
}
const handleSavePayroll = () => actions.run('save', confirmation)
const handleApprovePayroll = () => actions.run('approve', confirmation)
const handlePayPayroll = () => actions.run('payout', confirmation)
function applyBatch() {
  if (!permissions.value.edit) return
  if (!/^\d+(\.\d{1,2})?$/.test(batchValue.value.trim())) { ElMessage.warning('金额须为非负数字，最多两位小数'); return }
  const editable = filteredPayroll.value.filter(row => row.salary_status < 2)
  if (!editable.length) { ElMessage.info('筛选结果没有可编辑行'); return }
  editable.forEach(row => { row[batchField.value] = batchValue.value.trim() })
  state.dirty = true
}
function focusNextInput(event) {
  const inputs = [...event.target.closest('.table-wrapper').querySelectorAll('.payroll-cell-input:not(:disabled)')]
  inputs[inputs.indexOf(event.target) + 1]?.focus()
}
function pasteCells(event, row, field) {
  if (!permissions.value.edit) return
  const text = event.clipboardData?.getData('text') || ''
  if (!/[\t\r\n]/.test(text)) return
  event.preventDefault()
  const matrix = text.trimEnd().split(/\r?\n/).map(line => line.split('\t'))
  const startRow = filteredPayroll.value.indexOf(row), startColumn = PAYROLL_FIELDS.findIndex(([key]) => key === field)
  matrix.forEach((cells, ri) => {
    const target = filteredPayroll.value[startRow + ri]
    if (!target || target.salary_status >= 2) return
    cells.forEach((value, ci) => { const column = PAYROLL_FIELDS[startColumn + ci]; if (column) target[column[0]] = value })
  })
  state.dirty = true
}
function stopCountdown() { if (countdownTimer) clearInterval(countdownTimer); countdownTimer = null; countdownSeconds.value = 0 }
function localLock() { state.unlocked = false; unlockToken.value = ''; stopCountdown(); clearPrint() }
function startCountdown() {
  stopCountdown(); countdownSeconds.value = 30 * 60
  countdownTimer = setInterval(() => {
    countdownSeconds.value--
    if (countdownSeconds.value <= 0) { localLock(); ElMessage.info('工资视图已自动锁定') }
  }, 1000)
}
async function handleUnlock() {
  if (state.busy) return
  unlockError.value = ''
  if (!unlockCode.value.trim()) { unlockError.value = '请输入验证码'; return }
  state.busy = 'unlock'
  try {
    const res = await request.post('/hr/payroll/unlock', { code: unlockCode.value })
    if (disposed) return
    if (res.code !== 200 || typeof res.data?.token !== 'string' || !res.data.token) throw new Error(res.message || '解锁响应无效')
    unlockToken.value = res.data.token
    state.unlocked = true; showUnlockDialog.value = false; unlockCode.value = ''; startCountdown()
  } catch (error) { unlockError.value = error.message || '解锁失败，请核对验证码或联系管理员检查配置' }
  finally { state.busy = '' }
}
async function handleLock() {
  if (state.busy) return
  state.busy = 'lock'
  try {
    try { await ElMessageBox.confirm('锁定工资视图？未保存输入会保留。', '确认锁定') } catch { return }
    try { await request.post('/hr/payroll/lock', { token: unlockToken.value }) }
    catch (error) { state.error = `锁定请求失败：${error.message}；当前视图已遮蔽。` }
    localLock()
  } finally { state.busy = '' }
}
function handleExport() {
  if (!permissions.value.output || !filteredPayroll.value.length) return
  const url = URL.createObjectURL(new Blob([payrollCsv(filteredPayroll.value)], { type: 'text/csv;charset=utf-8;' }))
  const link = document.createElement('a')
  try { link.href = url; link.download = `工资表_${selectedMonth.value}.csv`; document.body.appendChild(link); link.click() }
  finally { link.remove(); setTimeout(() => URL.revokeObjectURL(url), 1000) }
}
function clearPrint() { printFrame?.remove(); printFrame = null }
function handlePrint() {
  if (!permissions.value.output || !filteredPayroll.value.length) return
  clearPrint()
  const snapshot = state.rows
  const frame = document.createElement('iframe'); printFrame = frame
  frame.title = '工资表打印区域'
  frame.style.cssText = 'position:fixed;left:-10000px;top:0;width:1000px;height:600px;border:0'
  frame.onload = () => {
    if (printFrame !== frame || snapshot !== state.rows || !permissions.value.output) { frame.remove(); return }
    try {
      frame.contentWindow.onafterprint = () => { if (printFrame === frame) clearPrint() }
      frame.contentWindow.focus(); frame.contentWindow.print()
    } catch (error) { clearPrint(); state.error = error.message || '无法打开打印窗口' }
  }
  frame.srcdoc = payrollPrintHtml(filteredPayroll.value, selectedMonth.value)
  document.body.appendChild(frame)
}
onMounted(() => actions.refresh())
onUnmounted(() => { disposed = true; actions.invalidate(); localLock() })
</script>

<style scoped>
.payroll-filters { display:flex; gap:10px; flex-wrap:wrap; align-items:center; padding:12px 20px; }
.payroll-filters :deep(.el-input), .payroll-filters :deep(.el-select) { width:180px; }
.payroll-filters span { font-size:12px; color:#6a7a6e; }
.payroll-cell-input { width:108px; padding:6px; box-sizing:border-box; border:1px solid #d0d8d2; border-radius:4px; text-align:right; }
.workflow-note { font-size:13px; line-height:1.7; color:#52685a; }
.workflow-error { font-size:13px; color:#b42318; }
.card-header-actions { flex-wrap:wrap; }

.payroll-page { padding: 24px 32px; }

/* ── 页头 ── */
.page-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; flex-wrap: wrap; gap: 12px; }
.page-title { font-size: 22px; font-weight: 700; color: #1a2f23; margin: 0; }
.page-subtitle { font-size: 13px; color: #8a9a8e; margin: 4px 0 0 0; }
.header-actions { display: flex; align-items: center; gap: 10px; }

/* ── 月份选择器 ── */
.month-selector :deep(.el-select) { width: 160px; }
.month-selector :deep(.el-input__wrapper) { border-color: #d0d8d2; box-shadow: none; }
.month-selector :deep(.el-input__wrapper:hover) { border-color: #2D4A3E; }
.month-selector :deep(.el-input__wrapper.is-focus) { border-color: #2D4A3E; box-shadow: 0 0 0 1px #2D4A3E inset; }

/* ── 解锁/锁定按钮 ── */
.btn-unlock {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: 1px solid #D4A853; background: linear-gradient(135deg, #D4A853, #C49A3C);
  color: #fff; font-weight: 500; transition: all 0.2s;
}
.btn-unlock:hover { background: linear-gradient(135deg, #C49A3C, #B38A2C); box-shadow: 0 2px 8px rgba(212,168,83,0.3); }

.btn-lock {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 18px; border-radius: 6px; font-size: 13px; cursor: pointer;
  border: 1px solid #2D4A3E; background: #2D4A3E;
  color: #fff; font-weight: 500; transition: all 0.2s;
}
.btn-lock:hover { background: #1a2f23; box-shadow: 0 2px 8px rgba(45,74,62,0.3); }

.unlock-timer {
  display: flex; align-items: center; gap: 4px;
  font-size: 12px; color: #D4A853; font-weight: 500;
  padding: 4px 10px; background: rgba(212,168,83,0.08); border-radius: 4px;
}

/* ── 统计卡片 ── */
.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-bottom: 20px; }
.stat-card { background: #fff; border-radius: 8px; padding: 18px 20px; border: 1px solid #e8ece9; display: flex; align-items: flex-start; gap: 14px; }
.stat-icon { width: 44px; height: 44px; border-radius: 10px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.stat-icon svg { width: 22px; height: 22px; }
.stat-content { flex: 1; }
.stat-label { font-size: 12px; color: #8a9a8e; margin-bottom: 4px; }
.stat-value { font-size: 26px; font-weight: 700; line-height: 1.2; }
.stat-sub { font-size: 11px; color: #a0b0a5; margin-top: 4px; }

/* ── 表格卡片 ── */
.table-card { background: #fff; border-radius: 8px; border: 1px solid #e8ece9; overflow: hidden; }
.card-header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; border-bottom: 1px solid #e8ece9; }
.section-title { font-size: 15px; font-weight: 600; color: #1a2f23; margin: 0; }
.btn-export {
  display: flex; align-items: center; gap: 4px;
  padding: 5px 14px; border-radius: 4px; font-size: 12px; cursor: pointer;
  border: 1px solid #d0d8d2; background: #fff; color: #3a4a3e;
  transition: all 0.2s;
}
.btn-export:hover { border-color: #2D4A3E; color: #2D4A3E; }
.btn-export:disabled { opacity: 0.5; cursor: not-allowed; }
.card-header-actions { display: flex; align-items: center; gap: 8px; }
.payroll-status-tag { font-size: 12px; padding: 3px 10px; border-radius: 10px; font-weight: 500; }
.payroll-status-tag.calculated { background: rgba(212,168,83,0.12); color: #B8860B; }
.payroll-status-tag.paid { background: rgba(74,124,89,0.12); color: #4A7C59; }

.table-wrapper { padding: 0; overflow-x: auto; }

/* ── 金额蒙版 ── */
.masked { color: #a0b0a5; font-family: monospace; letter-spacing: 2px; }
.deduction { color: #C0392B; }
.gross { color: #2D4A3E; font-weight: 600; }
.net { color: #4A7C59; font-weight: 700; }

/* ── Element Plus 表格样式覆盖 ── */
:deep(.el-table) { --el-table-border-color: #e8ece9; }
:deep(.el-table th.el-table__cell) { border-bottom: 2px solid #2D4A3E; }
:deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) { background: #f8faf8; }
:deep(.el-table .el-table__footer-wrapper .el-table__footer td.el-table__cell) {
  background: #f5f7f5; font-weight: 700; color: #2D4A3E; border-top: 2px solid #2D4A3E;
}
:deep(.el-table__body tr:hover > td.el-table__cell) { background: #eef5ef; }

/* ── 解锁弹窗 ── */
.unlock-dialog :deep(.el-dialog__header) { border-bottom: 1px solid #e8ece9; padding: 20px 24px; }
.unlock-dialog :deep(.el-dialog__title) { font-size: 16px; font-weight: 600; color: #1a2f23; }
.unlock-dialog :deep(.el-dialog__body) { padding: 28px 24px 20px; }
.unlock-dialog :deep(.el-dialog__footer) { border-top: 1px solid #e8ece9; padding: 14px 24px; }
.dialog-body { display: flex; flex-direction: column; align-items: center; gap: 16px; }
.lock-icon-wrap { width: 72px; height: 72px; border-radius: 50%; background: rgba(45,74,62,0.06); display: flex; align-items: center; justify-content: center; }
.dialog-desc { font-size: 14px; color: #6a7a6e; text-align: center; margin: 0; }
.dialog-body :deep(.el-input) { width: 280px; }
.dialog-body :deep(.el-input__wrapper) { border-color: #d0d8d2; box-shadow: none; }
.dialog-body :deep(.el-input__wrapper:hover) { border-color: #2D4A3E; }
.dialog-body :deep(.el-input__wrapper.is-focus) { border-color: #2D4A3E; box-shadow: 0 0 0 1px #2D4A3E inset; }
.dialog-body :deep(.is-error .el-input__wrapper) { border-color: #C0392B; box-shadow: 0 0 0 1px #C0392B inset; }
.error-msg { font-size: 12px; color: #C0392B; margin: 0; }
.btn-confirm { background: #2D4A3E; border-color: #2D4A3E; }
.btn-confirm:hover { background: #1a2f23; border-color: #1a2f23; }
</style>

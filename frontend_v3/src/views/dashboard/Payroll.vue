<template>
  <div class="payroll-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">工资管理 · Payroll</h2>
        <p class="page-subtitle">Salary management with encrypted protection</p>
      </div>
      <div class="header-actions">
        <div class="month-selector">
          <el-select v-model="selectedMonth" placeholder="选择月份" size="default" @change="fetchPayroll">
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
          class="btn-unlock"
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
          class="btn-lock"
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
            {{ unlocked ? '¥' + formatMoney(totalGross) : '****' }}
          </div>
          <div class="stat-sub">人均 {{ unlocked ? '¥' + formatMoney(avgGross) : '****' }}</div>
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
            {{ unlocked ? '¥' + formatMoney(totalNet) : '****' }}
          </div>
          <div class="stat-sub">人均 {{ unlocked ? '¥' + formatMoney(avgNet) : '****' }}</div>
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
            {{ unlocked ? '¥' + formatMoney(totalDeductions) : '****' }}
          </div>
          <div class="stat-sub">社保+个税+其他</div>
        </div>
      </div>
    </div>

    <!-- 工资表格 -->
    <div class="table-card">
      <div class="card-header">
        <h3 class="section-title">工资明细 · Payroll Details</h3>
        <button class="btn-export" @click="handleExport">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="14" height="14">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
            <polyline points="7 10 12 15 17 10"/>
            <line x1="12" y1="15" x2="12" y2="3"/>
          </svg>
          导出
        </button>
      </div>
      <div class="table-wrapper">
        <el-table
          :data="payrollData"
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
          <el-table-column prop="base_salary" label="基本工资" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked }">{{ unlocked ? formatMoney(row.base_salary) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="post_salary" label="岗位工资" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked }">{{ unlocked ? formatMoney(row.post_salary) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="attendance_pay" label="出勤绩效" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked }">{{ unlocked ? formatMoney(row.attendance_pay) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="overtime_pay" label="加班费" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked }">{{ unlocked ? formatMoney(row.overtime_pay) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="bonus" label="奖金" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked }">{{ unlocked ? formatMoney(row.bonus) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="allowance" label="补贴" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked }">{{ unlocked ? formatMoney(row.allowance) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deduction_social" label="社保代扣" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'deduction': true }">{{ unlocked ? formatMoney(row.deduction_social) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deduction_tax" label="个税代扣" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'deduction': true }">{{ unlocked ? formatMoney(row.deduction_tax) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deduction_other" label="其他扣款" width="120" align="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'deduction': true }">{{ unlocked ? formatMoney(row.deduction_other) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="gross_pay" label="应发合计" width="130" align="right" fixed="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'gross': true }">{{ unlocked ? formatMoney(row.gross_pay) : '****' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="net_pay" label="实发合计" width="130" align="right" fixed="right">
            <template #default="{ row }">
              <span :class="{ 'masked': !unlocked, 'net': true }">{{ unlocked ? formatMoney(row.net_pay) : '****' }}</span>
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
        <p class="dialog-desc">工资数据已加密保护，请输入验证码解锁查看</p>
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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// ── 状态 ──
const selectedMonth = ref('2026-07')
const unlocked = ref(false)
const showUnlockDialog = ref(false)
const unlockCode = ref('')
const unlockError = ref('')
const unlocking = ref(false)
const unlockToken = ref('')
const countdownSeconds = ref(0)
let countdownTimer = null

const payrollData = ref([])

// ── 可用月份 ──
const availableMonths = computed(() => {
  const months = []
  const now = new Date()
  for (let i = 0; i < 24; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1)
    const value = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}`
    const label = `${d.getFullYear()}年${d.getMonth() + 1}月`
    months.push({ value, label })
  }
  return months
})

// ── 倒计时 ──
const countdownText = computed(() => {
  if (countdownSeconds.value <= 0) return ''
  const m = Math.floor(countdownSeconds.value / 60)
  const s = countdownSeconds.value % 60
  return `${m}分${String(s).padStart(2, '0')}秒后自动锁定`
})

// ── 格式化 ──
const formatMoney = (val) => {
  if (val == null) return '0'
  return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// ── 汇总计算 ──
const totalGross = computed(() => payrollData.value.reduce((sum, r) => sum + (Number(r.gross_pay) || 0), 0))
const totalNet = computed(() => payrollData.value.reduce((sum, r) => sum + (Number(r.net_pay) || 0), 0))
const totalDeductions = computed(() => payrollData.value.reduce((sum, r) => sum + (Number(r.deduction_social) || 0) + (Number(r.deduction_tax) || 0) + (Number(r.deduction_other) || 0), 0))
const avgGross = computed(() => payrollData.value.length ? totalGross.value / payrollData.value.length : 0)
const avgNet = computed(() => payrollData.value.length ? totalNet.value / payrollData.value.length : 0)

// ── 表格合计行 ──
const getSummaries = (param) => {
  const { columns, data } = param
  const sums = []
  columns.forEach((col, index) => {
    if (index === 0) {
      sums[index] = '合计'
      return
    }
    if (index === 1) {
      sums[index] = `${data.length}人`
      return
    }
    if (index === 2) {
      sums[index] = ''
      return
    }
    const prop = col.property
    if (!prop) {
      sums[index] = ''
      return
    }
    const total = data.reduce((sum, row) => sum + (Number(row[prop]) || 0), 0)
    sums[index] = unlocked.value ? formatMoney(total) : '****'
  })
  return sums
}

// ── 数据获取 ──
const fetchPayroll = async () => {
  try {
    const res = await fetch(`/api/hr/payroll?month=${selectedMonth.value}`)
    if (res.ok) {
      const json = await res.json()
      payrollData.value = json.data || json || []
    } else {
      // 使用模拟数据
      payrollData.value = generateMockData()
    }
  } catch {
    payrollData.value = generateMockData()
  }
}

// ── 解锁 ──
const handleUnlock = async () => {
  unlockError.value = ''
  if (!unlockCode.value.trim()) {
    unlockError.value = '请输入验证码'
    return
  }
  unlocking.value = true
  try {
    const res = await fetch('/api/hr/payroll/unlock', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ code: unlockCode.value })
    })
    if (res.ok) {
      const json = await res.json()
      unlockToken.value = json.token || ''
      unlocked.value = true
      showUnlockDialog.value = false
      unlockCode.value = ''
      countdownSeconds.value = 30 * 60 // 30分钟
      startCountdown()
      ElMessage.success('已解锁，30分钟后将自动锁定')
    } else {
      const json = await res.json().catch(() => ({}))
      unlockError.value = json.message || '验证码错误'
    }
  } catch {
    // 离线模式：默认验证码 002323
    if (unlockCode.value === '002323') {
      unlockToken.value = 'local-' + Date.now()
      unlocked.value = true
      showUnlockDialog.value = false
      unlockCode.value = ''
      countdownSeconds.value = 30 * 60
      startCountdown()
      ElMessage.success('已解锁（离线模式），30分钟后将自动锁定')
    } else {
      unlockError.value = '验证码错误'
    }
  } finally {
    unlocking.value = false
  }
}

// ── 锁定 ──
const handleLock = async () => {
  try {
    await ElMessageBox.confirm('确定要锁定工资数据吗？', '确认锁定', {
      confirmButtonText: '锁定',
      cancelButtonText: '取消',
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await fetch('/api/hr/payroll/lock', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token: unlockToken.value })
    })
  } catch {
    // 离线模式忽略
  }
  unlocked.value = false
  unlockToken.value = ''
  stopCountdown()
  ElMessage.info('工资数据已锁定')
}

// ── 倒计时 ──
const startCountdown = () => {
  stopCountdown()
  countdownTimer = setInterval(() => {
    if (countdownSeconds.value > 0) {
      countdownSeconds.value--
    } else {
      unlocked.value = false
      unlockToken.value = ''
      stopCountdown()
      ElMessage.warning('解锁已过期，工资数据已自动锁定')
    }
  }, 1000)
}

const stopCountdown = () => {
  if (countdownTimer) {
    clearInterval(countdownTimer)
    countdownTimer = null
  }
  countdownSeconds.value = 0
}

// ── 导出 ──
const handleExport = () => {
  if (!unlocked.value) {
    ElMessage.warning('请先解锁后导出')
    return
  }
  const csv = [
    '工号,姓名,部门,基本工资,岗位工资,出勤绩效,加班费,奖金,补贴,社保代扣,个税代扣,其他扣款,应发合计,实发合计',
    ...payrollData.value.map(r => [
      r.emp_id, r.emp_name, r.department,
      r.base_salary, r.post_salary, r.attendance_pay,
      r.overtime_pay, r.bonus, r.allowance,
      r.deduction_social, r.deduction_tax, r.deduction_other,
      r.gross_pay, r.net_pay
    ].join(','))
  ].join('\n')
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `工资表_${selectedMonth.value}.csv`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

// ── 模拟数据 ──
const generateMockData = () => {
  const departments = ['前厅部', '后厨部', '行政部', '财务部', '工程部', '市场部']
  const names = [
    { id: '1001', name: '王芳', dept: '前厅部' },
    { id: '1002', name: '李强', dept: '前厅部' },
    { id: '1003', name: '张敏', dept: '前厅部' },
    { id: '1004', name: '刘洋', dept: '前厅部' },
    { id: '1005', name: '陈静', dept: '前厅部' },
    { id: '2001', name: '赵刚', dept: '后厨部' },
    { id: '2002', name: '孙伟', dept: '后厨部' },
    { id: '2003', name: '周杰', dept: '后厨部' },
    { id: '2004', name: '吴昊', dept: '后厨部' },
    { id: '2005', name: '郑凯', dept: '后厨部' },
    { id: '3001', name: '林丹', dept: '行政部' },
    { id: '3002', name: '黄蕾', dept: '行政部' },
    { id: '4001', name: '杨帆', dept: '财务部' },
    { id: '4002', name: '马丽', dept: '财务部' },
    { id: '5001', name: '朱峰', dept: '工程部' },
    { id: '5002', name: '胡涛', dept: '工程部' },
    { id: '6001', name: '何琳', dept: '市场部' },
    { id: '6002', name: '罗阳', dept: '市场部' },
  ]
  return names.map(n => {
    const base = 3500 + Math.floor(Math.random() * 3000)
    const post = 800 + Math.floor(Math.random() * 2500)
    const att = 500 + Math.floor(Math.random() * 1500)
    const ot = Math.floor(Math.random() * 1200)
    const bonus = Math.floor(Math.random() * 2000)
    const allow = 200 + Math.floor(Math.random() * 600)
    const social = Math.floor(base * 0.105)
    const taxBase = base + post + att + ot + bonus + allow - social - 5000
    const tax = Math.max(0, Math.floor(taxBase * (taxBase > 3000 ? 0.1 : 0.03)))
    const other = Math.floor(Math.random() * 200)
    const gross = base + post + att + ot + bonus + allow
    const net = gross - social - tax - other
    return {
      emp_id: n.id,
      emp_name: n.name,
      department: n.dept,
      base_salary: base,
      post_salary: post,
      attendance_pay: att,
      overtime_pay: ot,
      bonus,
      allowance: allow,
      deduction_social: social,
      deduction_tax: tax,
      deduction_other: other,
      gross_pay: gross,
      net_pay: net,
    }
  })
}

// ── 生命周期 ──
onMounted(() => {
  fetchPayroll()
})

onUnmounted(() => {
  stopCountdown()
})
</script>

<style scoped>
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

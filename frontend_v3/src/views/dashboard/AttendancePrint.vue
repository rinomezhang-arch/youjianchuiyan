<template>
  <div class="attendance-print">
    <!-- 筛选栏 -->
    <el-card shadow="never" class="filter-card no-print">
      <el-row :gutter="20" align="middle">
        <el-col :xs="24" :sm="6">
          <el-form-item label="月份" class="filter-item">
            <el-date-picker
              v-model="selectedMonth"
              type="month"
              placeholder="选择月份"
              format="YYYY-MM"
              value-format="YYYY-MM"
              @change="fetchData"
            />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="6">
          <el-form-item label="部门" class="filter-item">
            <el-select
              v-model="selectedDept"
              placeholder="全部部门"
              clearable
              @change="fetchData"
            >
              <el-option
                v-for="dept in departments"
                :key="dept"
                :label="dept"
                :value="dept"
              />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :sm="4">
          <el-button type="primary" @click="handlePrint" class="print-btn">
            <el-icon><Printer /></el-icon>
            打印报表
          </el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 报表区域 -->
    <el-card shadow="never" class="report-card" id="printArea">
      <!-- 报表标题 -->
      <div class="report-header">
        <h2 class="report-title">又见炊烟 · 考勤月报表</h2>
        <div class="report-meta">
          <span>月份：{{ selectedMonth }}</span>
          <span v-if="selectedDept">部门：{{ selectedDept }}</span>
          <span>打印日期：{{ printDate }}</span>
        </div>
      </div>

      <!-- 考勤表格 -->
      <div class="table-wrapper" v-if="attendanceData.length > 0">
        <table class="attendance-table">
          <thead>
            <tr>
              <th rowspan="2" class="col-fixed col-name">姓名</th>
              <th rowspan="2" class="col-fixed col-dept">部门</th>
              <th
                v-for="day in daysInMonth"
                :key="day"
                class="col-day"
                :class="{ 'col-weekend': isWeekend(day) }"
              >
                {{ day }}
              </th>
              <th colspan="5" class="col-summary-header">本月汇总</th>
            </tr>
            <tr>
              <template v-for="day in daysInMonth" :key="'dow-' + day">
                <th class="col-day dow" :class="{ 'col-weekend': isWeekend(day) }">
                  {{ getDayOfWeek(day) }}
                </th>
              </template>
              <th class="col-summary">出勤</th>
              <th class="col-summary">公休</th>
              <th class="col-summary">请假</th>
              <th class="col-summary">加班</th>
              <th class="col-summary">补休</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, idx) in attendanceData" :key="idx">
              <td class="col-name">{{ row.name }}</td>
              <td class="col-dept">{{ row.department }}</td>
              <td
                v-for="day in daysInMonth"
                :key="day"
                class="col-day"
                :class="{
                  'col-weekend': isWeekend(day),
                  'col-abnormal': isAbnormal(row.records[day - 1])
                }"
              >
                <span class="attendance-symbol" :style="{ color: getSymbolColor(row.records[day - 1]) }">
                  {{ getSymbol(row.records[day - 1]) }}
                </span>
              </td>
              <td class="col-summary">{{ row.summary.attendance }}</td>
              <td class="col-summary">{{ row.summary.rest }}</td>
              <td class="col-summary">{{ row.summary.leave }}</td>
              <td class="col-summary">{{ row.summary.overtime }}</td>
              <td class="col-summary">{{ row.summary.compLeave }}</td>
            </tr>
          </tbody>
          <tfoot>
            <tr class="footer-row">
              <td colspan="2" class="footer-label">部门合计</td>
              <td v-for="day in daysInMonth" :key="'ft-' + day" class="col-day"></td>
              <td class="col-summary footer-value">{{ totals.attendance }}</td>
              <td class="col-summary footer-value">{{ totals.rest }}</td>
              <td class="col-summary footer-value">{{ totals.leave }}</td>
              <td class="col-summary footer-value">{{ totals.overtime }}</td>
              <td class="col-summary footer-value">{{ totals.compLeave }}</td>
            </tr>
            <tr class="balance-row">
              <td colspan="2" class="footer-label">最终结余</td>
              <td v-for="day in daysInMonth" :key="'bal-' + day" class="col-day"></td>
              <td colspan="5" class="footer-value balance-value">
                {{ balance >= 0 ? '+' : '' }}{{ balance }} 天
                <span v-if="balance >= 0" class="balance-positive">（结余）</span>
                <span v-else class="balance-negative">（欠班）</span>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>

      <el-empty v-else description="暂无考勤数据" />

      <!-- 报表底部 -->
      <div class="report-footer">
        <div class="footer-item">
          <span>制表人：_______________</span>
        </div>
        <div class="footer-item">
          <span>审核人：_______________</span>
        </div>
        <div class="footer-item">
          <span>审批人：_______________</span>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { Printer } from '@element-plus/icons-vue'
import request from '@/utils/request'
import dayjs from 'dayjs'

const selectedMonth = ref(dayjs().format('YYYY-MM'))
const selectedDept = ref('')
const departments = ref([])
const attendanceData = ref([])

const printDate = computed(() => dayjs().format('YYYY-MM-DD'))

// 考勤符号映射
const SYMBOL_MAP = {
  present: '√',
  absent: '×',
  late: '△',
  early: '▽',
  leave: '假',
  rest: '休',
  overtime: '加',
  compLeave: '补',
  holiday: '节',
  business: '差'
}

const SYMBOL_COLORS = {
  present: '#2D4A3E',
  absent: '#F56C6C',
  late: '#E6A23C',
  early: '#E6A23C',
  leave: '#409EFF',
  rest: '#67C23A',
  overtime: '#2D4A3E',
  compLeave: '#909399',
  holiday: '#E6A23C',
  business: '#409EFF'
}

const daysInMonth = computed(() => {
  const date = dayjs(selectedMonth.value)
  return date.daysInMonth()
})

function isWeekend(day) {
  const date = dayjs(selectedMonth.value).date(day)
  const dow = date.day()
  return dow === 0 || dow === 6
}

function getDayOfWeek(day) {
  const date = dayjs(selectedMonth.value).date(day)
  const weekdays = ['日', '一', '二', '三', '四', '五', '六']
  return weekdays[date.day()]
}

function getSymbol(record) {
  if (!record) return ''
  return SYMBOL_MAP[record] || record
}

function getSymbolColor(record) {
  if (!record) return '#ccc'
  return SYMBOL_COLORS[record] || '#666'
}

function isAbnormal(record) {
  return record === 'late' || record === 'early' || record === 'absent'
}

// 汇总计算
const totals = computed(() => {
  const t = { attendance: 0, rest: 0, leave: 0, overtime: 0, compLeave: 0 }
  attendanceData.value.forEach(row => {
    t.attendance += row.summary.attendance || 0
    t.rest += row.summary.rest || 0
    t.leave += row.summary.leave || 0
    t.overtime += row.summary.overtime || 0
    t.compLeave += row.summary.compLeave || 0
  })
  return t
})

const balance = computed(() => {
  // 结余 = 出勤 + 加班 + 补休 - 公休 - 请假（半日计数）
  const t = totals.value
  return ((t.attendance + t.overtime + t.compLeave - t.rest - t.leave) / 2).toFixed(1)
})

async function fetchData() {
  try {
    const params = { month: selectedMonth.value }
    if (selectedDept.value) params.department = selectedDept.value

    const res = await request.get('/api/hr/attendance/summary', { params })
    const data = res.data || res

    attendanceData.value = (data.records || []).map(record => ({
      name: record.name || record.employeeName || '',
      department: record.department || '',
      records: record.records || record.dailyRecords || [],
      summary: {
        attendance: record.summary?.attendance || record.attendanceDays || 0,
        rest: record.summary?.rest || record.restDays || 0,
        leave: record.summary?.leave || record.leaveDays || 0,
        overtime: record.summary?.overtime || record.overtimeDays || 0,
        compLeave: record.summary?.compLeave || record.compLeaveDays || 0
      }
    }))

    departments.value = data.departments || []
  } catch (error) {
    console.error('获取考勤数据失败:', error)
    attendanceData.value = []
    departments.value = []
  }
}

function handlePrint() {
  // 打印前隐藏非打印元素
  const style = document.createElement('style')
  style.id = 'print-style'
  style.textContent = `
    @media print {
      .no-print { display: none !important; }
      .el-aside, .el-menu-vertical, .sidebar-container, .navbar, .tags-view, .app-breadcrumb {
        display: none !important;
      }
      .app-main, .main-container {
        margin-left: 0 !important;
        padding: 0 !important;
      }
      .attendance-print {
        padding: 0 !important;
      }
      .report-card {
        box-shadow: none !important;
        border: none !important;
      }
      @page {
        size: A3 landscape;
        margin: 10mm;
      }
    }
  `
  document.head.appendChild(style)
  window.print()
  setTimeout(() => {
    const el = document.getElementById('print-style')
    if (el) el.remove()
  }, 500)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.attendance-print {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
  border-radius: 12px;
  border: none;
}

.filter-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.filter-item {
  margin-bottom: 0;
}

.filter-item :deep(.el-form-item__label) {
  color: #2D4A3E;
  font-weight: 500;
}

.print-btn {
  background: #2D4A3E;
  border-color: #2D4A3E;
  width: 100%;
}

.print-btn:hover {
  background: #3D6A5A;
  border-color: #3D6A5A;
}

.report-card {
  border-radius: 12px;
  border: none;
}

.report-card :deep(.el-card__body) {
  padding: 24px;
}

.report-header {
  text-align: center;
  margin-bottom: 24px;
  padding-bottom: 16px;
  border-bottom: 2px solid #2D4A3E;
}

.report-title {
  font-size: 22px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0 0 12px 0;
  letter-spacing: 4px;
}

.report-meta {
  display: flex;
  justify-content: center;
  gap: 32px;
  font-size: 14px;
  color: #666;
}

.table-wrapper {
  overflow-x: auto;
  margin-bottom: 24px;
}

.attendance-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
  min-width: 1200px;
}

.attendance-table th,
.attendance-table td {
  border: 1px solid #e0e0e0;
  padding: 4px 2px;
  text-align: center;
  vertical-align: middle;
}

.attendance-table thead th {
  background: #2D4A3E;
  color: #fff;
  font-weight: 600;
  white-space: nowrap;
}

.attendance-table thead th.dow {
  font-size: 10px;
  font-weight: 400;
  background: #3D6A5A;
}

.col-fixed {
  min-width: 60px;
  position: sticky;
  left: 0;
  background: #2D4A3E !important;
  z-index: 2;
}

.col-name {
  min-width: 64px;
}

.col-dept {
  min-width: 64px;
}

.col-day {
  width: 28px;
  min-width: 28px;
  max-width: 28px;
  padding: 2px 0 !important;
}

.col-weekend {
  background: #f5f7fa !important;
}

.col-weekend.dow {
  background: #3D6A5A !important;
}

.col-summary-header {
  background: #3D6A5A !important;
}

.col-summary {
  min-width: 48px;
  font-weight: 500;
}

.attendance-symbol {
  font-size: 12px;
  font-weight: 600;
}

.col-abnormal {
  background: rgba(245, 108, 108, 0.08) !important;
}

.attendance-table tbody tr:hover {
  background: rgba(45, 74, 62, 0.04);
}

.attendance-table tbody td.col-name,
.attendance-table tbody td.col-dept {
  font-weight: 500;
  color: #333;
}

.footer-row td {
  background: #f5f7fa;
  font-weight: 600;
}

.footer-label {
  color: #2D4A3E;
  font-size: 13px;
}

.footer-value {
  color: #2D4A3E;
  font-weight: 700;
}

.balance-row td {
  background: rgba(45, 74, 62, 0.08);
}

.balance-value {
  font-size: 14px;
  color: #2D4A3E;
  font-weight: 700;
}

.balance-positive {
  color: #67C23A;
  font-size: 12px;
}

.balance-negative {
  color: #F56C6C;
  font-size: 12px;
}

.report-footer {
  display: flex;
  justify-content: space-around;
  margin-top: 40px;
  padding-top: 20px;
  border-top: 1px solid #e0e0e0;
}

.footer-item {
  font-size: 14px;
  color: #666;
}

@media print {
  .no-print {
    display: none !important;
  }

  .attendance-print {
    padding: 0 !important;
  }

  .report-card {
    box-shadow: none !important;
    border: none !important;
  }

  .report-card :deep(.el-card__body) {
    padding: 0 !important;
  }

  .attendance-table thead th {
    background: #2D4A3E !important;
    color: #fff !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .attendance-table thead th.dow {
    background: #3D6A5A !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .col-weekend {
    background: #f5f7fa !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .col-abnormal {
    background: rgba(245, 108, 108, 0.08) !important;
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}

@media (max-width: 768px) {
  .attendance-print {
    padding: 10px;
  }

  .report-card :deep(.el-card__body) {
    padding: 12px;
  }

  .report-title {
    font-size: 18px;
  }

  .report-meta {
    flex-direction: column;
    gap: 8px;
  }
}
</style>

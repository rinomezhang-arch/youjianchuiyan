<template>
  <div class="bookings-page">
    <div class="page-header">
      <div>
        <h2 class="page-title">预订管理 · Bookings</h2>
        <p class="page-subtitle">Booking management, date query, and customer records</p>
      </div>
      <div class="header-actions">
        <button class="btn-primary" @click="openBookingDialog(null)">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/>
          </svg>
          新建预订
        </button>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <div class="date-nav">
          <button class="tool-btn" @click="mvDay(-1)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          </button>
          <input type="date" class="date-input" :value="queryDate" @input="onDateChange" />
          <button class="tool-btn" @click="mvDay(1)">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="9 18 15 12 9 6"/></svg>
          </button>
          <button class="tool-btn-sm" @click="setToday">今天</button>
        </div>
        <div class="period-tabs">
          <button :class="['period-tab', {active: period==='all'}]" @click="period='all';fetchData()">全天</button>
          <button :class="['period-tab', {active: period==='lunch'}]" @click="period='lunch';fetchData()">午餐</button>
          <button :class="['period-tab', {active: period==='dinner'}]" @click="period='dinner';fetchData()">晚餐</button>
        </div>
      </div>
      <div class="toolbar-right">
        <div class="quick-range">
          <button :class="['range-btn', {active: quickRange==='today'}]" @click="qdate('today')">今天</button>
          <button :class="['range-btn', {active: quickRange==='week'}]" @click="qdate('week')">本周</button>
          <button :class="['range-btn', {active: quickRange==='month'}]" @click="qdate('month')">本月</button>
          <button :class="['range-btn', {active: quickRange==='year'}]" @click="qdate('year')">本年</button>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(45,74,62,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#2D4A3E" stroke-width="2">
            <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">总预订 · Total</div>
          <div class="stat-value" style="color:#2D4A3E">{{ total }}</div>
          <div class="stat-sub">单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(196,163,90,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/><polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">已确认 · Confirmed</div>
          <div class="stat-value" style="color:#C4A35A">{{ confirmedCount }}</div>
          <div class="stat-sub">单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(74,124,89,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
            <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">总人数 · Guests</div>
          <div class="stat-value" style="color:#4A7C59">{{ totalPeople }}</div>
          <div class="stat-sub">人</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(212,168,83,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#D4A853" stroke-width="2">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">午餐 · Lunch</div>
          <div class="stat-value" style="color:#D4A853">{{ lunchCount }}</div>
          <div class="stat-sub">单</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon" style="background:rgba(74,124,89,0.08)">
          <svg viewBox="0 0 24 24" fill="none" stroke="#4A7C59" stroke-width="2">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </div>
        <div class="stat-content">
          <div class="stat-label">晚餐 · Dinner</div>
          <div class="stat-value" style="color:#4A7C59">{{ dinnerCount }}</div>
          <div class="stat-sub">单</div>
        </div>
      </div>
    </div>

    <!-- 筛选栏 -->
    <div class="filter-row">
      <div class="filter-group">
        <label>搜索</label>
        <input class="filter-input" v-model="keyword" placeholder="客户姓名 / 电话" @keyup.enter="fetchData" />
      </div>
      <div class="filter-group">
        <label>状态</label>
        <select class="filter-select" v-model="statusFilter" @change="fetchData">
          <option value="">全部状态</option>
          <option value="confirmed">已确认</option>
          <option value="pending">待确认</option>
          <option value="cancelled">已取消</option>
          <option value="completed">已完成</option>
        </select>
      </div>
      <div class="filter-actions">
        <button class="btn-secondary" @click="resetFilter">重置</button>
        <button class="btn-primary" @click="fetchData">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/><path d="m21 21-4.35-4.35"/>
          </svg>
          查询
        </button>
      </div>
    </div>

    <!-- 列表 -->
    <div v-if="loading" class="loading-state">
      <div class="loading-spinner"></div>
      <span>加载中...</span>
    </div>

    <div v-else-if="total === 0" class="empty-state">
      <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#C4A35A" stroke-width="1.5">
        <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
      </svg>
      <p>暂无预订记录</p>
    </div>

    <div v-else class="table-wrapper">
      <el-table :data="list" border class="booking-table" @row-dblclick="openBookingDialog">
        <el-table-column prop="bookingId" label="预订编号" width="120">
          <template #default="scope">
            <span class="booking-id">{{ scope.row.bookingId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="日期/时段" width="150">
          <template #default="scope">
            <div>{{ scope.row.bookingDate }}</div>
            <span :class="['time-tag', scope.row.timeLabel]">{{ scope.row.timeLabel }}</span>
          </template>
        </el-table-column>
        <el-table-column label="桌台区域" width="120">
          <template #default="scope">
            <div class="table-names">{{ scope.row.tableNames || '-' }}</div>
            <div class="area-text">{{ scope.row.tableArea || '' }}</div>
          </template>
        </el-table-column>
        <el-table-column prop="customerName" label="客户姓名" width="110">
          <template #default="scope">
            <div>{{ scope.row.customerName }}</div>
            <div class="phone-text">{{ scope.row.customerPhone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="人数/桌数" width="120">
          <template #default="scope">
            {{ scope.row.guestCount || '?' }}人 / {{ scope.row.tableCount || '?' }}桌
            <span v-if="scope.row.spareTables > 0" class="spare-text">(备{{ scope.row.spareTables }})</span>
          </template>
        </el-table-column>
        <el-table-column prop="occasionType" label="类型" width="90">
          <template #default="scope">
            {{ scope.row.occasionType || '零点' }}
          </template>
        </el-table-column>
        <el-table-column label="菜品/金额" width="140">
          <template #default="scope">
            <div>{{ scope.row.dishCount || 0 }}道 / ¥{{ scope.row.totalAmount || 0 }}</div>
            <div v-if="scope.row.dishNames" class="dish-preview">{{ scope.row.dishNames.slice(0,20) }}{{ scope.row.dishNames.length > 20 ? '...' : '' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="预定员" width="100">
          <template #default="scope">
            <div>{{ scope.row.staffName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="140">
          <template #default="scope">
            {{ scope.row.createdAt ? scope.row.createdAt.replace('T', ' ').substring(0, 16) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="bookingStatus" label="状态" width="80">
          <template #default="scope">
            <span :class="['status-badge', scope.row.bookingStatus]">{{ getStatusText(scope.row.bookingStatus) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="140" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="scope">
            <button class="row-btn" @click="openBookingDialog(scope.row)">编辑</button>
            <button class="row-btn row-btn-print" @click="openPrintPreview(scope.row)">打印</button>
            <button class="row-btn row-btn-cancel" v-if="scope.row.bookingStatus === 'confirmed'" @click="cancelBooking(scope.row)">取消</button>
            <button class="row-btn row-btn-copy" @click="copyBooking(scope.row)">复制</button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          :current-page="page"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- 预订弹窗 -->
    <BookingDialog :model-value="bookingDialogVisible" :booking="bookingDialogRow" @update:model-value="onBookingDialogClose" @saved="onBookingDialogClose" @print="onDialogPrint" />

    <!-- 打印预览 -->
    <PrintPreview :visible="printPreviewVisible" :type="printPreviewType" :data="printPreviewData" @close="printPreviewVisible = false" />

    <!-- 复制弹窗 -->
    <div v-if="showCopyModal" class="modal-overlay" @click.self="showCopyModal=false">
      <div class="modal-box">
        <div class="modal-title">预订信息 · Booking Info</div>
        <div class="copy-content">{{ copyText }}</div>
        <div class="modal-actions">
          <button class="btn-secondary" @click="showCopyModal=false">关闭</button>
          <button class="btn-primary" @click="copyToClipboard">复制到剪贴板</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'
import {
  listBookings,
  createBooking as createBookingApi,
  updateBooking as updateBookingApi,
  cancelBooking as cancelBookingApi,
  getBookingStats
} from '@/api/booking'
import BookingDialog from '@/components/BookingDialog.vue'
import PrintPreview from '@/components/PrintPreview.vue'
import { ElMessage, ElMessageBox } from 'element-plus'

// ── API 函数封装（与 booking.js 保持一致）──
function getBookings(params) {
  return request({ url: '/bookings', method: 'get', params })
}
function createBooking(data) {
  return request({ url: '/bookings', method: 'post', data })
}
function updateBooking(id, data) {
  return request({ url: `/bookings/${id}`, method: 'put', data })
}
function deleteBooking(id) {
  return request({ url: `/bookings/${id}`, method: 'delete' })
}
// getBookingStats 已从 @/api/booking 导入，此处也可用本地封装：
// function getBookingStatsLocal(params) {
//   return request({ url: '/bookings/stats', method: 'get', params })
// }

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)
const queryDate = ref(new Date().toISOString().slice(0, 10))
const period = ref('all')
const statusFilter = ref('')
const keyword = ref('')
const quickRange = ref('today')
const showCopyModal = ref(false)
const copyText = ref('')

const bookingDialogVisible = ref(false)
const bookingDialogRow = ref(null)

const printPreviewVisible = ref(false)
const printPreviewType = ref('confirmation')
const printPreviewData = ref({})

function openBookingDialog(row) {
  bookingDialogRow.value = row
  bookingDialogVisible.value = true
}

function onBookingDialogClose() {
  bookingDialogVisible.value = false
  bookingDialogRow.value = null
  fetchData()
}

const confirmedCount = ref(0)
const totalPeople = ref(0)
const lunchCount = ref(0)
const dinnerCount = ref(0)
const statsError = ref('')

async function fetchStats() {
  try {
    const res = await getBookingStats({ date: queryDate.value })
    if (res.code === 200 && res.data) {
      const s = res.data
      confirmedCount.value = s.confirmedCount ?? s.confirmed ?? 0
      totalPeople.value = s.totalPeople ?? s.guests ?? 0
      lunchCount.value = s.lunchCount ?? s.lunch ?? 0
      dinnerCount.value = s.dinnerCount ?? s.dinner ?? 0
    } else {
      computeStatsLocal()
    }
  } catch (e) {
    statsError.value = '统计加载失败'
    computeStatsLocal()
  }
}
function computeStatsLocal() {
  confirmedCount.value = list.value.filter(b => b.bookingStatus === 'confirmed').length
  totalPeople.value = list.value.reduce((s, b) => s + (b.guestCount || 0), 0)
  lunchCount.value = list.value.filter(b => b.timeLabel === '午餐').length
  dinnerCount.value = list.value.filter(b => b.timeLabel === '晚餐').length
}

function onDateChange(e) { queryDate.value = e.target.value; page.value = 1; fetchData() }
function mvDay(n) { const d = new Date(queryDate.value); d.setDate(d.getDate() + n); queryDate.value = d.toISOString().slice(0, 10); page.value = 1; fetchData() }
function setToday() { queryDate.value = new Date().toISOString().slice(0, 10); page.value = 1; fetchData() }

function getMonday(d) { const nd = new Date(d); const day = nd.getDay(); nd.setDate(nd.getDate() - day + (day === 0 ? -6 : 1)); nd.setHours(0, 0, 0, 0); return nd }
function qdate(r) {
  const n = new Date()
  if (r === 'week') { const d = getMonday(n); d.setDate(d.getDate() + 6); queryDate.value = d.toISOString().slice(0, 10) }
  else if (r === 'month') { const d = new Date(n.getFullYear(), n.getMonth() + 1, 0); queryDate.value = d.toISOString().slice(0, 10) }
  else if (r === 'year') { const d = new Date(n.getFullYear(), 11, 31); queryDate.value = d.toISOString().slice(0, 10) }
  else queryDate.value = n.toISOString().slice(0, 10)
  quickRange.value = r
  page.value = 1; fetchData()
}

function getStatusText(status) {
  switch (status) {
    case 'confirmed': return '已确认'
    case 'pending': return '待确认'
    case 'cancelled': return '已取消'
    case 'completed': return '已完成'
    default: return status || '-'
  }
}

function resetFilter() {
  keyword.value = ''
  statusFilter.value = ''
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const timeStr = period.value === 'all' ? '' : (period.value === 'lunch' ? '午餐' : '晚餐')
    const params = {
      date: queryDate.value,
      time: timeStr,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    }
    const res = await listBookings(params)
    if (res.code === 200) {
      list.value = res.data?.rows || []
      total.value = res.data?.total || 0
      // 加载统计数据（优先 API，失败则本地计算）
      await fetchStats()
    } else {
      ElMessage.error(res.message || '加载预订列表失败')
    }
  } catch (e) {
    console.error('加载预订列表失败:', e)
    ElMessage.error('加载失败，请检查网络连接')
  } finally {
    loading.value = false
  }
}

function handlePageChange(val) {
  page.value = val
  fetchData()
}

async function cancelBooking(row) {
  try {
    await ElMessageBox.confirm(
      `确认取消预订 ${row.bookingId}？\n客户：${row.customerName || row.guestName || '未知'}\n日期：${row.bookingDate}\n此操作不可撤销。`,
      '取消预订',
      { confirmButtonText: '确认取消', cancelButtonText: '返回', type: 'warning' }
    )
    const res = await cancelBookingApi(row.bookingId)
    if (res.code === 200) {
      ElMessage.success('已取消')
      fetchData()
    }
  } catch (err) {
    if (err !== 'cancel' && err !== 'close') {
      ElMessage.error('取消失败')
    }
  }
}

function copyBooking(row) {
  copyText.value = `预订编号: ${row.bookingId}
客户: ${row.customerName}
电话: ${row.customerPhone}
日期: ${row.bookingDate} ${row.timeLabel}
人数: ${row.guestCount}人 / ${row.tableCount}桌${row.spareTables > 0 ? ' 备' + row.spareTables + '桌' : ''}
桌台: ${row.tableNames || '-'}
类型: ${row.occasionType || '零点'}
菜品: ${row.dishCount}道 / ¥${row.totalAmount || 0}
状态: ${getStatusText(row.bookingStatus)}
备注: ${row.remark || '-'}`
  showCopyModal.value = true
}

function copyToClipboard() {
  navigator.clipboard.writeText(copyText.value)
  ElMessage.success('已复制')
  showCopyModal.value = false
}

function formatPhone(phone) {
  if (!phone) return '-'
  const str = String(phone)
  if (str.length === 11) return str.slice(0, 3) + '****' + str.slice(7)
  return str
}

function preparePrintData(row) {
  let bookingTables = []
  if (row.tableNames) {
    const names = typeof row.tableNames === 'string' ? row.tableNames.split(',') : row.tableNames
    bookingTables = names.map(name => ({ table_name: name.trim() }))
  } else if (row.booking_tables) {
    bookingTables = row.booking_tables
  }
  return {
    booking_id: row.bookingId || row.booking_id || '-',
    customer_name: row.customerName || row.customer_name || '-',
    customer_phone: formatPhone(row.customerPhone || row.customer_phone),
    booking_date: row.bookingDate || row.booking_date || '-',
    booking_time: row.time_slot || row.booking_time || row.bookingTime || '',
    occasion_type: row.banquet_type || row.occasion_type || row.occasionType || 'a_la_carte',
    table_count: row.tableCount || row.table_count || 1,
    spare_tables: row.spareTables || row.spare_tables || 0,
    guest_per_table: row.guest_per_table || row.guestPerTable || 10,
    deposit: row.deposit || '',
    remark: row.remark || '',
    booking_tables: bookingTables,
    booking_status: row.bookingStatus || row.booking_status || row.status || '-'
  }
}

function openPrintPreview(row, type = 'confirmation') {
  const name = row.customerName || row.customer_name
  if (!name) { ElMessage.warning('客户姓名为空，无法打印'); return }
  const date = row.bookingDate || row.booking_date
  if (!date) { ElMessage.warning('日期为空，无法打印'); return }
  const tables = row.tableNames || row.booking_tables || (row.tableName ? [row.tableName] : [])
  let hasTable = false
  if (typeof tables === 'string' && tables.trim()) hasTable = true
  else if (Array.isArray(tables) && tables.length > 0) hasTable = true
  if (!hasTable && type === 'table_sign') { ElMessage.warning('桌台为空，无法打印桌签'); return }
  printPreviewData.value = preparePrintData(row)
  printPreviewType.value = type
  printPreviewVisible.value = true
}

function onDialogPrint(payload) {
  if (!payload || !payload.data) return
  printPreviewData.value = payload.data
  printPreviewType.value = payload.type || 'confirmation'
  printPreviewVisible.value = true
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.bookings-page {
  padding: 24px;
  min-height: calc(100vh - 108px);
  background: #FAF8F5;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
  margin: 0;
  font-family: var(--font-family, 'Microsoft YaHei', sans-serif);
}

.page-subtitle {
  font-size: 12px;
  color: #8a9a8e;
  margin: 4px 0 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.btn-primary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 16px;
  background: linear-gradient(135deg, #C4A35A, #D4B36A);
  color: #fff;
  border: none;
  border-radius: 2px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(196, 163, 90, 0.3);
}
.btn-primary:hover {
  background: linear-gradient(135deg, #D4B36A, #E8D5A0);
  box-shadow: 0 3px 10px rgba(196, 163, 90, 0.4);
}

.btn-secondary {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  padding: 0 16px;
  background: #fff;
  color: #2D4A3E;
  border: 1px solid #C4A35A;
  border-radius: 2px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-secondary:hover {
  background: rgba(196, 163, 90, 0.08);
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 2px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  gap: 12px;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.date-nav {
  display: flex;
  align-items: center;
  gap: 4px;
}

.tool-btn {
  width: 32px;
  height: 32px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  background: #fff;
  color: #2D4A3E;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}
.tool-btn:hover {
  background: rgba(45, 74, 62, 0.06);
  border-color: #C4A35A;
}

.tool-btn-sm {
  height: 32px;
  padding: 0 12px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  background: #fff;
  color: #2D4A3E;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.tool-btn-sm:hover {
  background: rgba(45, 74, 62, 0.06);
  border-color: #C4A35A;
}

.date-input {
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  background: #fff;
  font-size: 13px;
  color: #2D4A3E;
  outline: none;
  cursor: pointer;
  width: 140px;
}
.date-input:focus {
  border-color: #C4A35A;
  box-shadow: 0 0 0 2px rgba(196, 163, 90, 0.15);
}

.period-tabs {
  display: flex;
  gap: 2px;
  background: #f0f2f0;
  border-radius: 2px;
  padding: 2px;
}

.period-tab {
  height: 28px;
  padding: 0 14px;
  border: none;
  border-radius: 2px;
  background: transparent;
  color: #5a6e62;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.period-tab:hover {
  background: rgba(255, 255, 255, 0.6);
}
.period-tab.active {
  background: #2D4A3E;
  color: #fff;
  font-weight: 600;
}

.toolbar-right {
  display: flex;
  align-items: center;
}

.quick-range {
  display: flex;
  gap: 2px;
}

.range-btn {
  height: 28px;
  padding: 0 12px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  background: #fff;
  color: #5a6e62;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}
.range-btn:hover {
  border-color: #C4A35A;
  color: #C4A35A;
}
.range-btn.active {
  background: rgba(196, 163, 90, 0.1);
  border-color: #C4A35A;
  color: #C4A35A;
  font-weight: 600;
}

/* 统计卡片 */
.stats-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 160px;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 2px;
  transition: all 0.2s;
}
.stat-card:hover {
  border-color: #C4A35A;
  box-shadow: 0 2px 8px rgba(196, 163, 90, 0.12);
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 2px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.stat-icon svg {
  width: 22px;
  height: 22px;
}

.stat-content {
  flex: 1;
}

.stat-label {
  font-size: 11px;
  color: #8a9a8e;
  margin-bottom: 2px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-sub {
  font-size: 11px;
  color: #8a9a8e;
}

/* 筛选栏 */
.filter-row {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  padding: 12px 16px;
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 2px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.filter-group {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.filter-group label {
  font-size: 11px;
  color: #8a9a8e;
  font-weight: 500;
}

.filter-input {
  width: 220px;
  height: 32px;
  padding: 0 10px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  font-size: 13px;
  color: #2D4A3E;
  outline: none;
  transition: border-color 0.2s;
}
.filter-input:focus {
  border-color: #C4A35A;
  box-shadow: 0 0 0 2px rgba(196, 163, 90, 0.15);
}

.filter-select {
  height: 32px;
  padding: 0 8px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  font-size: 13px;
  color: #2D4A3E;
  outline: none;
  background: #fff;
  cursor: pointer;
}
.filter-select:focus {
  border-color: #C4A35A;
}

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

/* 表格 */
.table-wrapper {
  background: #fff;
  border: 1px solid #e8ece9;
  border-radius: 2px;
  overflow: hidden;
}

.booking-table {
  width: 100%;
}

.booking-table :deep(.el-table__header th) {
  background: #f5f7f5;
  color: #2D4A3E;
  font-weight: 600;
  font-size: 12px;
  border-color: #e8ece9;
}

.booking-table :deep(.el-table__body td) {
  font-size: 13px;
  color: #3a4a3e;
  border-color: #e8ece9;
}

.booking-table :deep(.el-table__row:hover td) {
  background: rgba(196, 163, 90, 0.04);
}

.booking-id {
  font-family: 'Courier New', monospace;
  color: #C4A35A;
  font-weight: 600;
  font-size: 12px;
}

.time-tag {
  font-size: 10px;
  padding: 1px 6px;
  border-radius: 2px;
  display: inline-block;
  margin-top: 2px;
  font-weight: 500;
}
.time-tag.午餐 {
  background: rgba(212, 168, 83, 0.12);
  color: #B8942E;
}
.time-tag.晚餐 {
  background: rgba(74, 124, 89, 0.12);
  color: #3A6B4A;
}

.phone-text {
  font-size: 11px;
  color: #a0aea5;
}

.spare-text {
  color: #C0392B;
  font-size: 11px;
  margin-left: 4px;
}

.table-names {
  font-weight: 600;
  color: #2D4A3E;
}

.area-text {
  font-size: 11px;
  color: #a0aea5;
}

.dish-preview {
  font-size: 11px;
  color: #a0aea5;
  max-width: 140px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 2px;
  font-weight: 500;
  display: inline-block;
}
.status-badge.confirmed {
  background: rgba(45, 74, 62, 0.1);
  color: #2D4A3E;
}
.status-badge.pending {
  background: rgba(196, 163, 90, 0.12);
  color: #B8942E;
}
.status-badge.cancelled {
  background: rgba(192, 57, 43, 0.1);
  color: #C0392B;
}
.status-badge.completed {
  background: rgba(74, 124, 89, 0.1);
  color: #4A7C59;
}

.row-btn {
  height: 24px;
  padding: 0 8px;
  border: 1px solid #e0e4e1;
  border-radius: 2px;
  background: #fff;
  color: #2D4A3E;
  font-size: 11px;
  cursor: pointer;
  margin-right: 4px;
  transition: all 0.2s;
}
.row-btn:hover {
  border-color: #C4A35A;
  color: #C4A35A;
  background: rgba(196, 163, 90, 0.06);
}
.row-btn-print {
  color: #5B7B8A;
  border-color: #5B7B8A;
}
.row-btn-print:hover {
  background: rgba(91, 123, 138, 0.08);
  color: #5B7B8A;
}
.row-btn-cancel {
  color: #C0392B;
  border-color: #C0392B;
}
.row-btn-cancel:hover {
  background: rgba(192, 57, 43, 0.08);
}
.row-btn-copy {
  color: #4A7C59;
  border-color: #4A7C59;
}
.row-btn-copy:hover {
  background: rgba(74, 124, 89, 0.08);
}

.pagination-row {
  display: flex;
  justify-content: center;
  padding: 16px;
}

/* 加载/空状态 */
.loading-state {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: #8a9a8e;
  font-size: 14px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #e8ece9;
  border-top-color: #C4A35A;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px 0;
  color: #8a9a8e;
}
.empty-state p {
  font-size: 14px;
  margin: 0;
}

/* 弹窗 */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 100;
}

.modal-box {
  background: #fff;
  border-radius: 2px;
  padding: 24px;
  width: 50vw;
  max-width: 600px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
  max-height: 80vh;
  overflow-y: auto;
}

.modal-title {
  font-size: 16px;
  font-weight: 700;
  color: #2D4A3E;
  margin-bottom: 16px;
}

.copy-content {
  white-space: pre-wrap;
  padding: 12px;
  background: #f5f7f5;
  border: 1px solid #e8ece9;
  border-radius: 2px;
  font-size: 13px;
  color: #3a4a3e;
  max-height: 300px;
  overflow-y: auto;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 16px;
}
</style>

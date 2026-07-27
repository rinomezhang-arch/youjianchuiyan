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
        <button class="btn-secondary" @click="printAllBookings">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="6 9 6 2 18 2 18 9"/><path d="M6 18H4a2 2 0 0 1-2-2v-5a2 2 0 0 1 2-2h16a2 2 0 0 1 2 2v5a2 2 0 0 1-2 2h-2"/><rect x="6" y="14" width="12" height="8"/>
          </svg>
          打印预定报告
        </button>
      </div>
    </div>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="toolbar-left">
        <!-- 日期范围选择 -->
        <div class="date-range-group">
          <input type="date" class="date-input" v-model="startDate" @change="onDateRangeChange" />
          <span class="date-sep">至</span>
          <input type="date" class="date-input" v-model="endDate" @change="onDateRangeChange" />
        </div>
        <!-- 快捷范围 -->
        <div class="quick-range">
          <button :class="['range-btn', {active: quickRange==='today'}]" @click="qdate('today')">今天</button>
          <button :class="['range-btn', {active: quickRange==='week'}]" @click="qdate('week')">本周</button>
          <button :class="['range-btn', {active: quickRange==='month'}]" @click="qdate('month')">本月</button>
          <button :class="['range-btn', {active: quickRange==='year'}]" @click="qdate('year')">本年</button>
        </div>
        <!-- 时段 -->
        <div class="period-tabs">
          <button :class="['period-tab', {active: period==='all'}]" @click="setPeriod('all')">全天</button>
          <button :class="['period-tab', {active: period==='lunch'}]" @click="setPeriod('lunch')">午餐</button>
          <button :class="['period-tab', {active: period==='dinner'}]" @click="setPeriod('dinner')">晚餐</button>
        </div>
      </div>
      <div class="toolbar-right">
        <!-- 时间滑动条 -->
        <div class="time-slider">
          <span class="slider-label-sm">时段</span>
          <input type="range" min="0" max="24" v-model.number="hourFilter" class="slider" @input="onSliderChange" />
          <span class="slider-val">{{ hourFilter === 0 ? '全天' : (hourFilter === 24 ? '全天' : hourFilter + ':00') }}</span>
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
      <div class="filter-group" style="flex:1; min-width:200px">
        <label>搜索 · Search</label>
        <input class="filter-input" v-model="keyword" placeholder="输入客户姓名 / 电话 / 编号即时搜索" @input="onSearchInput" style="width:100%" />
      </div>
      <div class="status-btns">
        <button :class="['status-btn', {active: statusFilter===''}]" @click="setStatus('')">全部</button>
        <button :class="['status-btn st-confirmed', {active: statusFilter==='confirmed'}]" @click="setStatus('confirmed')">已确认</button>
        <button :class="['status-btn st-pending', {active: statusFilter==='pending'}]" @click="setStatus('pending')">待确认</button>
        <button :class="['status-btn st-completed', {active: statusFilter==='completed'}]" @click="setStatus('completed')">已完成</button>
        <button :class="['status-btn st-cancelled', {active: statusFilter==='cancelled'}]" @click="setStatus('cancelled')">已取消</button>
      </div>
      <button class="btn-secondary" @click="resetFilter">重置</button>
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
      <el-table :data="list" border class="booking-table" @row-dblclick="(row) => openBookingDialog(row)">
        <el-table-column prop="bookingId" label="预订编号" width="120">
          <template #default="scope">
            <span class="booking-id" @click="openBookingDialog(scope.row)" style="cursor:pointer; text-decoration: underline; color: #C4A35A">{{ scope.row.bookingId }}</span>
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
        <el-table-column label="菜品/金额" width="160">
          <template #default="scope">
            <div v-if="scope.row.dishCount > 0" class="dish-amount-clickable" @click.stop="openDishDetail(scope.row)">
              <span class="dish-count-text">{{ scope.row.dishCount }}道</span>
              <span class="dish-amount-text">¥{{ scope.row.totalAmount || 0 }}</span>
            </div>
            <div v-else class="no-dish-link" @click.stop="goOrderDish(scope.row)">未点菜</div>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="scope">
            <button class="row-btn" @click="openEditDialog(scope.row)">编辑</button>
            <button class="row-btn row-btn-print" @click="openPrintPreview(scope.row)">打印</button>
            <button class="row-btn row-btn-cancel" v-if="scope.row.bookingStatus === 'confirmed'" @click="confirmCancel(scope.row)">取消</button>
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

    <!-- 打印预览 -->
    <PrintPreview :visible="printPreviewVisible" :type="printPreviewType" :data="printPreviewData" @close="printPreviewVisible = false" />

    <!-- 菜单详情弹窗 -->
    <div v-if="dishDetailVisible" class="modal-overlay" @click.self="dishDetailVisible=false">
      <div class="dish-detail-modal">
        <div class="dish-detail-header">
          <div>
            <h3>菜单详情 · Menu Detail</h3>
            <p>{{ dishDetailData.bookingId }} · {{ dishDetailData.customerName }} · {{ dishDetailData.bookingDate }}</p>
          </div>
          <button class="dish-detail-close" @click="dishDetailVisible=false">x</button>
        </div>
        <div class="dish-detail-body">
          <table class="dish-detail-table">
            <thead>
              <tr>
                <th style="width:40px">序号</th>
                <th>菜品名称</th>
                <th style="width:60px">数量</th>
                <th style="width:80px">单价</th>
                <th style="width:90px">小计</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(d, i) in dishDetailData.dishes" :key="i">
                <td>{{ i + 1 }}</td>
                <td>{{ d.dishName || d.dish_name || '-' }}</td>
                <td>{{ d.dishQuantity || d.dish_quantity || 1 }}</td>
                <td>¥{{ d.unitPrice || d.unit_price || 0 }}</td>
                <td>¥{{ d.subtotal || 0 }}</td>
              </tr>
              <tr v-if="dishDetailData.dishes.length === 0">
                <td colspan="5" style="text-align:center; color:#999; padding:24px">暂无菜品数据</td>
              </tr>
            </tbody>
            <tfoot>
              <tr>
                <td colspan="4" style="text-align:right; font-weight:600">合计 · Total</td>
                <td style="font-weight:600; color:#C4A35A; font-size:14px">¥{{ dishDetailData.totalAmount || 0 }}</td>
              </tr>
            </tfoot>
          </table>
        </div>
        <div class="dish-detail-footer">
          <button class="btn-secondary" @click="dishDetailVisible=false">关闭</button>
          <button class="btn-primary" @click="printDishDetail">预览打印</button>
        </div>
      </div>
    </div>

  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { listBookings, cancelBooking as cancelBookingApi } from '@/api/booking'
import PrintPreview from '@/components/PrintPreview.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

// 日期范围
const todayStr = new Date().toISOString().slice(0, 10)
const startDate = ref(todayStr)
const endDate = ref(todayStr)
const quickRange = ref('today')

const period = ref('all')
const statusFilter = ref('')
const keyword = ref('')
const hourFilter = ref(0)



const printPreviewVisible = ref(false)
const printPreviewType = ref('confirmation')
const printPreviewData = ref({})

// 菜单详情弹窗
const dishDetailVisible = ref(false)
const dishDetailData = ref({ dishes: [], totalAmount: 0, bookingId: '', customerName: '', bookingDate: '' })

// 防抖搜索
let searchTimer = null
function onSearchInput() {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1
    fetchData()
  }, 350)
}

function openBookingDialog(row) {
  if (row) {
    // 编辑现有预订，带上预订ID
    router.push({ path: '/dashboard/table-board', query: { bookingId: row.bookingId } })
  } else {
    // 新建预订，直接跳转桌台看板
    router.push('/dashboard/table-board')
  }
}

function openEditDialog(row) {
  router.push({ path: '/dashboard/table-board', query: { bookingId: row.bookingId, edit: 'true' } })
}

function confirmCancel(row) {
  if (!confirm(`确定要取消预订 ${row.bookingId} 吗？\n客户：${row.customerName}\n取消后该时段的房间将释放回可售池。`)) return
  cancelBooking(row)
}

function goOrderDish(row) {
  router.push({ path: '/dashboard/table-board', query: { bookingId: row.bookingId, edit: 'true', dish: 'true' } })
}

const confirmedCount = computed(() => list.value.filter(b => b.bookingStatus === 'confirmed').length)
const totalPeople = computed(() => list.value.reduce((s, b) => s + (b.guestCount || 0), 0))
const lunchCount = computed(() => list.value.filter(b => b.timeLabel === '午餐').length)
const dinnerCount = computed(() => list.value.filter(b => b.timeLabel === '晚餐').length)

function onDateRangeChange() {
  quickRange.value = ''
  page.value = 1
  fetchData()
}

function setPeriod(p) {
  period.value = p
  page.value = 1
  fetchData()
}

function setStatus(s) {
  statusFilter.value = s
  page.value = 1
  fetchData()
}

function onSliderChange() {
  page.value = 1
  fetchData()
}

function getMonday(d) { const nd = new Date(d); const day = nd.getDay(); nd.setDate(nd.getDate() - day + (day === 0 ? -6 : 1)); nd.setHours(0, 0, 0, 0); return nd }
function qdate(r) {
  const n = new Date()
  if (r === 'today') {
    startDate.value = todayStr
    endDate.value = todayStr
  } else if (r === 'week') {
    const mon = getMonday(n)
    const sun = new Date(mon)
    sun.setDate(sun.getDate() + 6)
    startDate.value = mon.toISOString().slice(0, 10)
    endDate.value = sun.toISOString().slice(0, 10)
  } else if (r === 'month') {
    startDate.value = new Date(n.getFullYear(), n.getMonth(), 1).toISOString().slice(0, 10)
    endDate.value = new Date(n.getFullYear(), n.getMonth() + 1, 0).toISOString().slice(0, 10)
  } else if (r === 'year') {
    startDate.value = new Date(n.getFullYear(), 0, 1).toISOString().slice(0, 10)
    endDate.value = new Date(n.getFullYear(), 11, 31).toISOString().slice(0, 10)
  }
  quickRange.value = r
  page.value = 1
  fetchData()
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
  hourFilter.value = 0
  period.value = 'all'
  quickRange.value = 'today'
  startDate.value = todayStr
  endDate.value = todayStr
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const timeStr = period.value === 'all' ? '' : (period.value === 'lunch' ? '午餐' : '晚餐')
    const params = {
      startDate: startDate.value,
      endDate: endDate.value,
      time: timeStr,
      keyword: keyword.value || undefined,
      status: statusFilter.value || undefined,
      page: page.value,
      pageSize: pageSize.value
    }
    const res = await listBookings(params)
    if (res.code === 200) {
      const rawRows = res.data?.rows || []
      let mapped = rawRows.map(row => {
        const bt = row.booking_time || row.bookingTime || ''
        const hour = parseInt((bt || '').split(':')[0], 10) || 0
        const timeLabel = hour < 15 ? '午餐' : '晚餐'
        return {
          ...row,
          bookingId: row.booking_id || row.bookingId,
          storeId: row.store_id || row.storeId,
          bookingDate: row.booking_date || row.bookingDate,
          bookingTime: bt,
          customerId: row.customer_id || row.customerId,
          customerName: row.customer_name || row.customerName,
          customerPhone: row.customer_phone || row.customerPhone,
          staffId: row.staff_id || row.staffId,
          staffName: row.staff_name || row.staffName,
          deposit: row.deposit,
          guestCount: row.guest_count || row.guestCount,
          tableCount: row.table_count || row.tableCount,
          spareTables: row.spare_tables || row.spareTables,
          guestPerTable: row.guest_per_table || row.guestPerTable,
          bookingStatus: row.booking_status || row.bookingStatus,
          banquetName: row.banquet_name || row.banquetName,
          occasionType: row.occasion_type || row.occasionType,
          specialRequest: row.special_request || row.specialRequest,
          totalAmount: row.total_amount || row.totalAmount,
          finalAmount: row.final_amount || row.finalAmount,
          paymentStatus: row.payment_status || row.paymentStatus,
          createdAt: row.created_at || row.createdAt,
          updatedAt: row.updated_at || row.updatedAt,
          remark: row.remark || '',
          tableNames: row.bt_names || row.tableNames,
          tableArea: '',
          timeLabel,
          dishCount: row.dish_count || row.dishCount || 0,
          dishNames: row.dish_names || row.dishNames || ''
        }
      })
      // 时间滑动条过滤
      if (hourFilter.value > 0 && hourFilter.value < 24) {
        mapped = mapped.filter(r => {
          const h = parseInt((r.bookingTime || '').split(':')[0], 10) || 0
          return h >= hourFilter.value
        })
      }
      list.value = mapped
      total.value = hourFilter.value > 0 && hourFilter.value < 24 ? mapped.length : (res.data?.total || 0)
    }
  } catch (e) { console.error(e); ElMessage.error('加载失败') }
  finally { loading.value = false }
}

function handlePageChange(val) {
  page.value = val
  fetchData()
}

async function cancelBooking(row) {
  try {
    const res = await cancelBookingApi(row.bookingId)
    if (res.code === 200) {
      ElMessage.success('已取消')
      fetchData()
    }
  } catch { ElMessage.error('取消失败') }
}

function printAllBookings() {
  if (list.value.length === 0) {
    ElMessage.warning('当前无预订数据，无法打印')
    return
  }
  const storeName = localStorage.getItem('storeName') || '全部门店'
  const statusText = statusFilter.value ? getStatusText(statusFilter.value) : '全部'
  const periodText = period.value === 'all' ? '全天' : (period.value === 'lunch' ? '午餐' : '晚餐')
  const keywordText = keyword.value || '无'

  const rows = list.value.map((r, i) => `<tr>
    <td style="text-align:center">${i+1}</td>
    <td>${r.bookingId || ''}</td>
    <td style="text-align:center">${r.bookingDate || ''}</td>
    <td style="text-align:center">${r.timeLabel || ''}</td>
    <td>${r.tableNames || '-'}</td>
    <td>${r.customerName || ''}</td>
    <td style="text-align:center">${r.customerPhone || ''}</td>
    <td style="text-align:center">${r.guestCount || 0}人/${r.tableCount || 0}桌</td>
    <td>${r.occasionType || '零点'}</td>
    <td style="text-align:center">${r.dishCount || 0}道</td>
    <td style="text-align:right">¥${r.totalAmount || 0}</td>
    <td style="text-align:center">${r.staffName || '-'}</td>
    <td style="text-align:center">${getStatusText(r.bookingStatus)}</td>
  </tr>`).join('')

  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>预订报告 - ${storeName}</title>
  <style>
    * { margin:0; padding:0; box-sizing:border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; padding: 30px 40px; color: #333; }
    .report-header { display:flex; align-items:center; border-bottom: 3px solid #2D4A3E; padding-bottom: 15px; margin-bottom: 20px; }
    .report-logo { width:50px; height:50px; background:#2D4A3E; color:#C4A35A; border-radius:6px; display:flex; align-items:center; justify-content:center; font-size:20px; font-weight:700; margin-right:15px; }
    .report-title h1 { font-size: 22px; color: #2D4A3E; }
    .report-title p { font-size: 13px; color: #888; margin-top: 3px; }
    .report-meta { margin-left:auto; text-align:right; font-size:12px; color:#888; }
    .query-conditions { background:#faf9f6; border:1px solid #e8e6e0; border-radius:4px; padding:12px 16px; margin-bottom:20px; font-size:13px; }
    .query-conditions-title { font-weight:600; color:#2D4A3E; margin-bottom:8px; }
    .query-row { display:flex; gap:30px; flex-wrap:wrap; }
    .query-item { color:#555; }
    .query-item span { color:#2D4A3E; font-weight:600; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #2D4A3E; color: #fff; padding: 8px 6px; font-size: 12px; font-weight: 500; text-align: left; white-space:nowrap; }
    td { padding: 7px 6px; border-bottom: 1px solid #eee; font-size: 12px; }
    tr:nth-child(even) td { background: #faf9f6; }
    .report-footer { margin-top: 20px; display:flex; justify-content:space-between; font-size: 12px; color: #999; }
    .report-total { margin-top: 15px; text-align: right; font-size: 14px; font-weight: 700; color: #C4A35A; }
  </style>
  </head><body>
    <div class="report-header">
      <div class="report-logo">炊</div>
      <div class="report-title">
        <h1>又见炊烟私房菜 - 预订报告</h1>
        <p>Booking Report · ${storeName}</p>
      </div>
      <div class="report-meta">
        <div>打印时间：${new Date().toLocaleString('zh-CN')}</div>
        <div>共 ${list.value.length} 条记录</div>
      </div>
    </div>
    <div class="query-conditions">
      <div class="query-conditions-title">查询条件 · Query Conditions</div>
      <div class="query-row">
        <div class="query-item">门店：<span>${storeName}</span></div>
        <div class="query-item">日期范围：<span>${startDate.value} 至 ${endDate.value}</span></div>
        <div class="query-item">时段：<span>${periodText}</span></div>
        <div class="query-item">状态：<span>${statusText}</span></div>
        <div class="query-item">搜索关键词：<span>${keywordText}</span></div>
      </div>
    </div>
    <table>
      <thead><tr>
        <th>序号</th><th>预订编号</th><th>日期</th><th>时段</th><th>桌台</th>
        <th>客户姓名</th><th>电话</th><th>人数/桌数</th><th>宴席类型</th>
        <th>菜品数</th><th>金额</th><th>预定员</th><th>状态</th>
      </tr></thead>
      <tbody>${rows}</tbody>
    </table>
    <div class="report-total">合计金额：¥${list.value.reduce((s, r) => s + (r.totalAmount || 0), 0)}</div>
    <div class="report-footer">
      <span>又见炊烟私房菜 · 宴会预订管理系统</span>
      <span>第 1 页 / 共 1 页</span>
    </div>
  </body></html>`

  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(iframe)
  iframe.contentWindow.document.open()
  iframe.contentWindow.document.write(html)
  iframe.contentWindow.document.close()
  iframe.contentWindow.focus()
  setTimeout(() => {
    iframe.contentWindow.print()
    setTimeout(() => document.body.removeChild(iframe), 1000)
  }, 300)
}

async function openDishDetail(row) {
  dishDetailData.value = {
    dishes: [],
    totalAmount: row.totalAmount || 0,
    bookingId: row.bookingId,
    customerName: row.customerName,
    bookingDate: row.bookingDate,
    bookingTime: row.bookingTime
  }
  dishDetailVisible.value = true
  try {
    const storeId = Number(localStorage.getItem('storeId')) || 1
    const res = await fetch(`/api/bookings/${row.bookingId}?storeId=${storeId}`)
    const json = await res.json()
    if (json.code === 200 && json.data) {
      const dishes = json.data.dishes || []
      dishDetailData.value = {
        ...dishDetailData.value,
        dishes,
        totalAmount: json.data.booking?.total_amount || json.data.booking?.totalAmount || row.totalAmount || 0
      }
    }
  } catch (e) {
    console.error('获取菜品详情失败', e)
  }
}

function printDishDetail() {
  const d = dishDetailData.value
  const rows = d.dishes.map((dish, i) => {
    const name = dish.dishName || dish.dish_name || '-'
    const qty = dish.dishQuantity || dish.dish_quantity || 1
    const price = dish.unitPrice || dish.unit_price || 0
    const sub = dish.subtotal || 0
    return `<tr><td style="text-align:center">${i+1}</td><td>${name}</td><td style="text-align:center">${qty}</td><td style="text-align:right">¥${price}</td><td style="text-align:right">¥${sub}</td></tr>`
  }).join('')
  const html = `<!DOCTYPE html><html><head><meta charset="utf-8"><title>菜单详情 - ${d.bookingId}</title>
  <style>
    * { margin:0; padding:0; box-sizing:border-box; }
    body { font-family: "Microsoft YaHei", sans-serif; padding: 30px; color: #333; }
    .header { text-align:center; margin-bottom: 20px; padding-bottom: 15px; border-bottom: 2px solid #C4A35A; }
    .header h1 { font-size: 20px; color: #2D4A3E; }
    .header p { font-size: 13px; color: #888; margin-top: 5px; }
    .info { display:flex; justify-content:space-between; margin-bottom: 15px; font-size: 13px; color: #555; }
    table { width: 100%; border-collapse: collapse; }
    th { background: #2D4A3E; color: #fff; padding: 8px 10px; font-size: 13px; font-weight: 500; }
    td { padding: 8px 10px; border-bottom: 1px solid #eee; font-size: 13px; }
    tr:nth-child(even) td { background: #faf9f6; }
    .total { margin-top: 15px; text-align: right; font-size: 16px; font-weight: 700; color: #C4A35A; }
    .footer { margin-top: 30px; text-align: center; font-size: 12px; color: #999; }
  </style>
  </head><body>
    <div class="header">
      <h1>又见炊烟私房菜 - 菜单详情</h1>
      <p>Menu Detail · ${d.bookingId}</p>
    </div>
    <div class="info">
      <span>客户：${d.customerName || '-'}</span>
      <span>日期：${d.bookingDate || '-'}</span>
      <span>时间：${d.bookingTime || '-'}</span>
    </div>
    <table>
      <thead><tr><th style="width:40px">序号</th><th>菜品名称</th><th style="width:60px">数量</th><th style="width:80px">单价</th><th style="width:90px">小计</th></tr></thead>
      <tbody>${rows || '<tr><td colspan="5" style="text-align:center;color:#999;padding:20px">暂无菜品</td></tr>'}</tbody>
    </table>
    <div class="total">合计：¥${d.totalAmount || 0}</div>
    <div class="footer">打印时间：${new Date().toLocaleString('zh-CN')}</div>
  </body></html>`
  const iframe = document.createElement('iframe')
  iframe.style.cssText = 'position:fixed;right:0;bottom:0;width:0;height:0;border:0'
  document.body.appendChild(iframe)
  iframe.contentWindow.document.open()
  iframe.contentWindow.document.write(html)
  iframe.contentWindow.document.close()
  iframe.contentWindow.focus()
  setTimeout(() => {
    iframe.contentWindow.print()
    setTimeout(() => document.body.removeChild(iframe), 1000)
  }, 300)
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

/* 日期范围选择 */
.date-range-group {
  display: flex;
  align-items: center;
  gap: 4px;
}
.date-sep {
  font-size: 12px;
  color: #8a9a8e;
  padding: 0 2px;
}

/* 时间滑动条 */
.time-slider {
  display: flex;
  align-items: center;
  gap: 8px;
}
.slider-label-sm {
  font-size: 11px;
  color: #8a9a8e;
  white-space: nowrap;
}
.slider {
  -webkit-appearance: none;
  appearance: none;
  width: 120px;
  height: 4px;
  border-radius: 2px;
  background: #e0e4e1;
  outline: none;
}
.slider::-webkit-slider-thumb {
  -webkit-appearance: none;
  appearance: none;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #C4A35A;
  cursor: pointer;
  border: 2px solid #fff;
  box-shadow: 0 1px 4px rgba(196,163,90,0.4);
}
.slider::-moz-range-thumb {
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #C4A35A;
  cursor: pointer;
  border: 2px solid #fff;
}
.slider-val {
  font-size: 12px;
  color: #2D4A3E;
  font-weight: 600;
  min-width: 42px;
  text-align: center;
}

/* 状态按钮组 */
.status-btns {
  display: flex;
  gap: 2px;
  background: #f0f2f0;
  border-radius: 2px;
  padding: 2px;
}
.status-btn {
  height: 28px;
  padding: 0 12px;
  border: none;
  border-radius: 2px;
  background: transparent;
  color: #5a6e62;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.status-btn:hover {
  background: rgba(255,255,255,0.6);
}
.status-btn.active {
  background: #2D4A3E;
  color: #fff;
  font-weight: 600;
}
.status-btn.active.st-confirmed { background: #C4A35A; }
.status-btn.active.st-pending { background: #D4A853; }
.status-btn.active.st-completed { background: #4A7C59; }
.status-btn.active.st-cancelled { background: #b04a3a; }

/* 菜品金额可点击 */
.dish-amount-clickable {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  transition: opacity 0.2s;
}
.dish-amount-clickable:hover {
  opacity: 0.7;
}
.dish-count-text {
  font-size: 12px;
  color: #8a9a8e;
}
.dish-amount-text {
  font-size: 13px;
  font-weight: 600;
  color: #C4A35A;
  text-decoration: underline;
}
.no-dish-link {
  font-size: 12px;
  color: #b04a3a;
  cursor: pointer;
  text-decoration: underline;
  padding: 2px 6px;
  border-radius: 2px;
  transition: background 0.2s;
  display: inline-block;
}
.no-dish-link:hover {
  background: rgba(176,74,58,0.08);
}

/* 菜单详情弹窗 */
.dish-detail-modal {
  background: #fff;
  border-radius: 8px;
  width: 50vw;
  max-width: 640px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 32px rgba(0,0,0,0.12);
}
.dish-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
}
.dish-detail-header h3 {
  font-size: 16px;
  color: #2D4A3E;
  margin: 0;
}
.dish-detail-header p {
  font-size: 12px;
  color: #888;
  margin: 4px 0 0 0;
}
.dish-detail-close {
  border: none;
  background: none;
  font-size: 18px;
  color: #999;
  cursor: pointer;
  padding: 0 4px;
}
.dish-detail-close:hover { color: #333; }
.dish-detail-body {
  padding: 16px 20px;
  overflow-y: auto;
  flex: 1;
}
.dish-detail-table {
  width: 100%;
  border-collapse: collapse;
}
.dish-detail-table th {
  background: #2D4A3E;
  color: #fff;
  padding: 8px 10px;
  font-size: 12px;
  font-weight: 500;
  text-align: left;
}
.dish-detail-table td {
  padding: 8px 10px;
  border-bottom: 1px solid #eee;
  font-size: 13px;
}
.dish-detail-table tr:nth-child(even) td {
  background: #faf9f6;
}
.dish-detail-table tfoot td {
  border-top: 2px solid #C4A35A;
  border-bottom: none;
  padding-top: 12px;
}
.dish-detail-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  border-top: 1px solid #eee;
}
</style>

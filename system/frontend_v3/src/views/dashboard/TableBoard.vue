<template>
  <div class="tboard">
    <section class="reference-hero">
      <div>
        <p class="eyebrow">RESTAURANT RESERVATION SYSTEM</p>
        <h1>又见炊烟 · 餐饮预订管理</h1>
        <p class="hero-copy">桌台、预订、客户与经营数据集中管理</p>
      </div>
      <div class="hero-actions">
        <button class="hero-btn" @click="goToBookings">预订管理</button>
        <button class="hero-btn primary" @click="openExportPanel">导出数据</button>
      </div>
    </section>

    <nav class="page-tabs" aria-label="桌台看板功能">
      <button :class="{ active: activeView === 'board' }" @click="activeView = 'board'">桌台预订</button>
      <button :class="{ active: activeView === 'analysis' }" @click="openAnalysis">经营分析</button>
    </nav>

    <template v-if="activeView === 'board'">
    <section class="glass-panel board-panel">
    <!-- 日期导航 -->
    <div class="date-nav">
      <input type="date" class="date-input" :value="fmtDate(curDate)" @change="onDateChange" />
      <button class="nav-btn" @click="mvDay(-1)">‹</button>
      <button class="today-btn" @click="setToday">今天 · Today</button>
      <button class="nav-btn" @click="mvDay(1)">›</button>
      <span class="nav-sep"></span>
      <button :class="['quick-btn', { active: activeQuick === 'week' }]" @click="activeQuick = 'week'; mvDay(7)">下周 · Next Week</button>
      <button :class="['quick-btn', { active: activeQuick === 'month' }]" @click="activeQuick = 'month'; mvDay(30)">下月 · Next Month</button>
      <span class="nav-sep"></span>
      <button :class="['period-btn', { active: timeType === 'all' }]" @click="timeType = 'all'; loadData()">全天 · All</button>
      <button :class="['period-btn', 'lunch', { active: timeType === 'lunch' }]" @click="timeType = 'lunch'; loadData()">{{ t('booking.lunch') }} · {{ t('booking.lunchEn') }}</button>
      <button :class="['period-btn', 'dinner', { active: timeType === 'dinner' }]" @click="timeType = 'dinner'; loadData()">{{ t('booking.dinner') }} · {{ t('booking.dinnerEn') }}</button>
      <span class="nav-sep"></span>
      <button :class="['edit-btn', { editing: edit }]" @click="toggleEdit">{{ edit ? `${t('common.save')} · ${t('common.saveEn')}` : `${t('tableBoard.actions.edit')} · ${t('tableBoard.actions.editEn')}` }}</button>
      <button class="edit-btn nav-btn-bookings" @click="goToBookings">预订管理 · Bookings</button>
    </div>

    <div class="summary-grid">
      <article class="summary-card"><span>桌台总数</span><strong>{{ list.length }}</strong><small>TABLES</small></article>
      <article class="summary-card available"><span>空闲桌台</span><strong>{{ freeN }}</strong><small>AVAILABLE</small></article>
      <article class="summary-card reserved"><span>已预订</span><strong>{{ bookedN }}</strong><small>RESERVED</small></article>
      <article class="summary-card guests"><span>预计客流</span><strong>{{ totalGuests }}</strong><small>GUESTS</small></article>
    </div>

    <div class="filter-heading">
      <div><strong>{{ fmtDate(curDate) }}</strong><span>{{ dateLabel }}</span></div>
      <div class="legend"><span><i class="dot free"></i>空闲</span><span><i class="dot booked"></i>已预订</span></div>
    </div>

    <!-- 区域筛选 -->
    <div class="area-row">
      <button v-for="a in areaList" :key="a" :class="['area-btn', { active: area === a }]" @click="area = a">{{ a }}</button>
    </div>

    <!-- 状态筛选 -->
    <div class="status-row" v-if="!edit">
      <button :class="['status-btn', { active: statusFilter === 'all' }]" @click="statusFilter = 'all'">{{ t('common.status') }} · {{ t('common.statusEn') }} <span class="sb-badge">{{ list.length }}</span></button>
      <button :class="['status-btn', { active: statusFilter === 'free' }]" @click="statusFilter = 'free'">{{ t('tableBoard.status.available') }} · {{ t('tableBoard.status.availableEn') }} <span class="sb-badge">{{ freeN }}</span></button>
      <button :class="['status-btn', { active: statusFilter === 'booked' }]" @click="statusFilter = 'booked'">{{ t('tableBoard.status.reserved') }} · {{ t('tableBoard.status.reservedEn') }} <span class="sb-badge">{{ bookedN }}</span></button>
    </div>

    <!-- 编辑栏 -->
    <div v-if="edit" class="edit-toolbar">
      <button class="add-btn" @click="showAddModal = true">+ {{ t('tableBoard.actions.add') }} · {{ t('tableBoard.actions.addEn') }}</button>
      <span class="edit-hint">拖拽调整顺序 | 点击×{{ t('tableBoard.actions.delete') }} · {{ t('tableBoard.actions.deleteEn') }}</span>
    </div>

    <!-- 底部浮动操作工具栏 -->
    <transition name="toolbar-slide">
      <div v-show="showActionToolbar" class="floating-action-bar" :style="toolbarStyle">
        <div class="fab-summary">{{ actionSummary }}</div>
        <div class="fab-btns">
          <button class="act-btn act-copy" :disabled="!canCopy" @click="copyBookingToSelected">复制 · Copy</button>
          <button class="act-btn act-swap" :disabled="!canSwap" @click="startSwapMode">互换 · Swap</button>
          <button class="act-btn act-delete" :disabled="!canDelete" @click="quickDeleteBooking">删除 · Delete</button>
          <button class="act-btn act-print" :disabled="!hasBooked" @click="printBooking">打印 · Print</button>
          <button class="act-btn act-cancel" @click="selClear">取消 · Cancel</button>
        </div>
      </div>
    </transition>

    <!-- 桌台网格 -->
    <div class="tgrid" :class="{ 'tgrid-with-fab': showActionToolbar }" id="tableGrid">
      <template v-for="(t, i) in displayList" :key="t.table_id">
        <div
          :class="cardClass(t)"
          :data-table-id="t.table_id"
          @click="!edit && toggleSel(t, $event)"
          @dblclick="!edit && openBooking(t)"
          @contextmenu.prevent="!edit && onContextMenu($event, t)"
          @dragstart="onDragStart($event, i)"
          @dragend="onDragEnd"
          @dragover="onDragOver($event, i)"
          @dragleave="onDragLeave"
          @drop="onDrop($event, i)"
          :draggable="edit"
        >
          <div class="table-name">{{ t.table_number }}</div>
          <!-- 全天模式：合并午晚数据，精简显示上下午 -->
          <template v-if="timeType === 'all' && t.booking">
            <div v-if="t.booking.booking_id2" class="tbinfo-both">
              <div class="table-summary lunch">
                <span class="ts-tag">午</span>
                <span class="ts-name">{{ t.booking.customer_name }}</span>
                <span class="ts-count" v-if="t.booking.guest_count">· {{ t.booking.guest_count }}人</span>
              </div>
              <div class="table-summary dinner">
                <span class="ts-tag">晚</span>
                <span class="ts-name">{{ t.booking.customer_name2 }}</span>
                <span class="ts-count" v-if="t.booking.guest_count2">· {{ t.booking.guest_count2 }}人</span>
              </div>
            </div>
            <div v-else class="tbinfo-both">
              <template v-if="isLunchTime(t.booking.booking_time)">
                <div class="table-summary lunch">
                  <span class="ts-tag">午</span>
                  <span class="ts-name">{{ t.booking.customer_name }}</span>
                  <span class="ts-count" v-if="t.booking.guest_count">· {{ t.booking.guest_count }}人</span>
                </div>
                <div class="table-summary empty">
                  <span class="ts-tag">晚</span>
                  <span class="ts-name">空闲</span>
                </div>
              </template>
              <template v-else>
                <div class="table-summary empty">
                  <span class="ts-tag">午</span>
                  <span class="ts-name">空闲</span>
                </div>
                <div class="table-summary dinner">
                  <span class="ts-tag">晚</span>
                  <span class="ts-name">{{ t.booking.customer_name }}</span>
                  <span class="ts-count" v-if="t.booking.guest_count">· {{ t.booking.guest_count }}人</span>
                </div>
              </template>
            </div>
          </template>
          <!-- 单时段模式：详细显示 -->
          <div v-else-if="t.booking" class="tbinfo">
            <div class="table-guest">
              {{ t.booking.customer_name }}
              <span v-if="t.booking.visit_count >= 5" class="vip-badge" title="第{{ t.booking.visit_count }}次到店">👑VIP</span>
              <span v-else-if="t.booking.visit_count >= 2" class="regular-badge" title="第{{ t.booking.visit_count }}次到店">⭐熟客</span>
            </div>
            <div v-if="t.booking.customer_phone" class="table-phone">{{ t.booking.customer_phone }}</div>
            <div v-if="t.booking.guest_count" class="table-booked-people">预订 {{ t.booking.guest_count }} 人</div>
            <div v-if="t.booking.dishes_count > 0" class="table-dish-count">🍽 {{ t.booking.dishes_count }} 道菜</div>
            <div v-if="t.booking.booking_time" class="table-booking-time">{{ t.booking.booking_time.slice(0, 5) }}</div>
            <div v-if="t.booking.banquet_name" class="banquet-type-badge">{{ t.booking.banquet_name }}</div>
          </div>
          <div v-else class="table-capacity">可容纳 {{ t.table_capacity }} 人</div>
          <span class="status-dot"></span>
          <div v-if="edit" class="drag-handle">⠿</div>
          <div v-if="edit" class="delete-btn" @click.stop="deleteTableItem(t)">×</div>
        </div>
      </template>
      <div v-if="displayList.length === 0" class="empty-state">
        <strong>当前筛选下暂无桌台</strong>
        <span>可切换日期、时段或区域查看</span>
      </div>
    </div>
    </section>
    </template>

    <section v-else class="analysis-page">
      <div class="analysis-toolbar glass-panel">
        <div class="analysis-dates">
          <label>开始日期<input v-model="analysisStart" type="date" /></label>
          <label>结束日期<input v-model="analysisEnd" type="date" /></label>
        </div>
        <div class="analysis-actions">
          <button class="soft-btn" @click="setAnalysisRange(0)">今日</button>
          <button class="soft-btn" @click="setAnalysisRange(6)">近 7 天</button>
          <button class="soft-btn" @click="setAnalysisRange(29)">近 30 天</button>
          <button class="primary-btn" @click="loadAnalysis">刷新分析</button>
        </div>
      </div>
      <div class="analysis-kpis">
        <article><span>总预订数</span><strong>{{ analysisBookings.length }}</strong></article>
        <article><span>总人次</span><strong>{{ analysisGuests }}</strong></article>
        <article><span>桌均人数</span><strong>{{ averageGuests }}</strong></article>
        <article><span>热门区域</span><strong class="textual">{{ hotArea }}</strong></article>
      </div>
      <div class="analysis-grid">
        <article class="glass-panel ranking-card">
          <div class="section-title"><div><small>AREA INSIGHTS</small><h2>区域预订分布</h2></div></div>
          <div v-if="areaRanking.length" class="bar-list">
            <div v-for="item in areaRanking" :key="item.name" class="bar-item">
              <div><span>{{ item.name }}</span><strong>{{ item.count }} 单</strong></div>
              <div class="bar-track"><i :style="{ width: item.percent + '%' }"></i></div>
            </div>
          </div>
          <div v-else class="analysis-empty">所选日期暂无预订数据</div>
        </article>
        <article class="glass-panel ranking-card">
          <div class="section-title"><div><small>TABLE RANKING</small><h2>热门桌台排行</h2></div></div>
          <ol v-if="tableRanking.length" class="rank-list">
            <li v-for="(item, index) in tableRanking" :key="item.name"><b>{{ index + 1 }}</b><span>{{ item.name }}</span><strong>{{ item.count }} 单</strong></li>
          </ol>
          <div v-else class="analysis-empty">暂无桌台排行</div>
        </article>
      </div>
      <article class="glass-panel detail-card">
        <div class="section-title"><div><small>BOOKING DETAILS</small><h2>预订明细</h2></div><button class="primary-btn" @click="openExportPanel">导出数据</button></div>
        <div class="detail-table-wrap">
          <table class="detail-table"><thead><tr><th>日期</th><th>桌台</th><th>区域</th><th>客户</th><th>电话</th><th>人数</th><th>状态</th></tr></thead>
            <tbody><tr v-for="booking in analysisBookings" :key="booking.booking_id || booking.bookingId"><td>{{ booking.booking_date || booking.bookingDate }}</td><td>{{ booking.table_number || booking.tableNumber || '-' }}</td><td>{{ booking.table_area || booking.tableArea || '-' }}</td><td>{{ booking.customer_name || booking.customerName || '-' }}</td><td>{{ booking.customer_phone || booking.customerPhone || '-' }}</td><td>{{ booking.guest_count || booking.guestCount || 0 }}</td><td>{{ booking.booking_status || booking.status || '-' }}</td></tr></tbody>
          </table>
        </div>
      </article>
    </section>


    <!-- 添加桌台弹窗 -->
    <div v-if="showAddModal" class="modal-overlay" @click.self="showAddModal = false">
      <div class="modal-box">
        <div class="modal-title">{{ t('tableBoard.actions.add') }} · {{ t('tableBoard.actions.addEn') }}</div>
        <div class="modal-field"><label>{{ t('common.name') }} · {{ t('common.nameEn') }}</label><input v-model="newTableName" placeholder="例：201春华" /></div>
        <div class="modal-field"><label>{{ t('common.people') }} · {{ t('common.peopleEn') }}</label><input v-model="newTablePeople" type="number" placeholder="例：10" /></div>
        <div class="modal-field"><label>{{ t('tableBoard.area.private') }} · {{ t('tableBoard.area.privateEn') }}</label>
          <select v-model="newTableArea">
            <option v-for="z in ZONES" :key="z" :value="z">{{ z }}</option>
          </select>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="showAddModal = false">{{ t('common.cancel') }} · {{ t('common.cancelEn') }}</button>
          <button class="btn-ok" @click="confirmAddTable">{{ t('common.confirm') }} · {{ t('common.confirmEn') }}</button>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div v-if="ctxMenuVisible" class="right-menu" :style="{ left: ctxMenuPos.x + 'px', top: ctxMenuPos.y + 'px' }">
      <div class="menu-item" @click="ctxNewBooking">✏️ 预订录入 · New Booking</div>
      <div class="menu-item" v-if="!ctxMenuTable?.booking" @click="ctxQuickOccupy">🔴 快速占用 · Occupy</div>
      <div class="menu-item" @click="ctxEditBooking">📝 编辑预订 · Edit</div>
      <div class="menu-item danger" @click="ctxDeleteBooking">🗑️ 删除预订 · Delete</div>
      <div class="menu-divider"></div>
      <div class="menu-item" @click="ctxCustomerLookup">👤 客户查询 · Customer</div>
      <div class="menu-item" @click="openMenuPicker(ctxMenuTable)">🍽️ 点菜 · Menu</div>
      <div class="menu-divider"></div>
      <div class="menu-item" @click="selClear">✕ 取消选择 · Cancel</div>
    </div>

    <!-- 预订弹窗 -->
    <BookingDialog ref="bkDialogRef" v-model="bkVis" :tableId="selTable?.table_id" :tableNumber="selTable?.table_number" :tableName="selTable?.table_name" :date="fmtDate(curDate)" :booking="initialBooking" :tableIds="selIds" :tableNames="selectedTables.map(t => t.table_number)" @saved="onSaved" @date-change="onDialogDateChange" @period-change="onDialogPeriodChange" @print="onPrint" />

    <!-- 菜单选择弹窗 -->
    <MenuPicker v-model="showMenuPicker" :tableId="menuPickerTableId" :initialDishes="menuPickerInitialDishes" @confirm="onMenuConfirmed" />

    <!-- 客户分析弹窗 -->
    <CustomerAnalysis v-model="showCustomerAnalysis" :initialPhone="customerAnalysisPhone" />

    <!-- 打印预览弹窗 -->
    <PrintPreview v-if="printPreviewVisible" :visible="printPreviewVisible" :type="printPreviewType" :data="printPreviewData" @close="printPreviewVisible = false" />

    <div v-if="exportVisible" class="modal-overlay" @click.self="exportVisible = false">
      <div class="modal-box export-box">
        <div class="modal-title-row"><div><small>EXPORT</small><h3>导出预订数据</h3></div><button @click="exportVisible = false">×</button></div>
        <div class="modal-field"><label>开始日期</label><input v-model="exportStart" type="date" /></div>
        <div class="modal-field"><label>结束日期</label><input v-model="exportEnd" type="date" /></div>
        <div class="modal-field"><label>时段</label><select v-model="exportPeriod"><option value="all">全部时段</option><option value="morning">午餐</option><option value="afternoon">晚餐</option></select></div>
        <div class="export-preview"><span>预计导出</span><strong>{{ exportRows.length }}</strong><span>条预订记录</span></div>
        <div class="modal-actions"><button class="btn-cancel" @click="exportVisible = false">取消</button><button class="btn-ok" :disabled="exportRows.length === 0" @click="downloadCsv">下载 CSV</button></div>
      </div>
    </div>

    <!-- 全天模式时段选择弹窗 -->
    <div v-if="showPeriodModal" class="period-modal-overlay" @click.self="showPeriodModal = false">
      <div class="period-modal-box">
        <div class="period-modal-header">
          <div class="period-modal-title">请选择时段</div>
          <div class="period-modal-subtitle">Select Time Period</div>
        </div>
        <div class="period-modal-body">
          <div class="period-card" @click="selectPeriod('lunch')">
            <div class="period-icon lunch-icon">☀</div>
            <div class="period-name">午餐 · Lunch</div>
            <div class="period-time">11:00 - 14:30</div>
          </div>
          <div class="period-card" @click="selectPeriod('dinner')">
            <div class="period-icon dinner-icon">🌙</div>
            <div class="period-name">晚餐 · Dinner</div>
            <div class="period-time">17:00 - 21:30</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { getTableBoard, getTableStatus, deleteTable, reorderTables, createTable, cancelBooking, copyBooking, swapBooking, createBooking, getBookings } from '@/api/booking'
import BookingDialog from '@/components/BookingDialog.vue'
import MenuPicker from './MenuPicker.vue'
import CustomerAnalysis from './CustomerAnalysis.vue'
import PrintPreview from '@/components/PrintPreview.vue'

const { t } = useI18n()
const router = useRouter()
const route = useRoute()

const curDate = ref(new Date())
const activeQuick = ref(null)
const timeType = ref('all')
const area = ref('全部')
const statusFilter = ref('all')
const list = ref([])
const selIds = ref([])
const selPosition = ref({ x: 0, y: 0 })
const edit = ref(false)
const bkVis = ref(false)
const bkDialogRef = ref(null)
const selTable = ref(null)
const initialBooking = ref(null)
const showAddModal = ref(false)
const newTableName = ref('')
const newTablePeople = ref('')
const newTableArea = ref('一楼包厢')

// 菜单选择器
const showMenuPicker = ref(false)

// 全天模式时段选择弹窗
const showPeriodModal = ref(false)
const menuPickerTableId = ref(null)
const menuPickerInitialDishes = ref([])

// 客户分析
const showCustomerAnalysis = ref(false)
const customerAnalysisPhone = ref('')

// 打印预览
const printPreviewVisible = ref(false)
const printPreviewType = ref('confirmation')
const printPreviewData = ref({})

const dragSrcIdx = ref(null)
const dragTargetIdx = ref(null)
const dragInsertPos = ref('after')
const swapMode = ref(null) // 调换模式：发起方桌台ID或null
const ctxMenuVisible = ref(false)
const ctxMenuPos = ref({ x: 0, y: 0 })
const ctxMenuTable = ref(null) // 右键选中的桌台
const activeView = ref('board')
const analysisStart = ref(fmtDate(new Date()))
const analysisEnd = ref(fmtDate(new Date()))
const analysisBookings = ref([])
const exportVisible = ref(false)
const exportStart = ref(fmtDate(new Date()))
const exportEnd = ref(fmtDate(new Date()))
const exportPeriod = ref('all')
const exportRows = ref([])

const totalGuests = computed(() => list.value.reduce((sum, table) => {
  const first = Number(table.booking?.guest_count || 0)
  const second = Number(table.booking?.guest_count2 || 0)
  return sum + first + second
}, 0))
const dateLabel = computed(() => curDate.value.toLocaleDateString('zh-CN', { weekday: 'long' }))
const analysisGuests = computed(() => analysisBookings.value.reduce((sum, booking) => sum + Number(booking.guest_count || booking.guestCount || 0), 0))
const averageGuests = computed(() => analysisBookings.value.length ? (analysisGuests.value / analysisBookings.value.length).toFixed(1) : '0.0')
const areaRanking = computed(() => buildRanking('table_area', 'tableArea'))
const tableRanking = computed(() => buildRanking('table_number', 'tableNumber').slice(0, 6))
const hotArea = computed(() => areaRanking.value[0]?.name || '-')

const ZONES = ['一楼包厢', '一楼扶摇厅', '二楼1号服务厅', '一楼散客大厅', '二楼2号服务厅', '三楼宴会厅', '四楼宴会厅', '排队或加桌', '一楼外摆']

function fmtDate(d) {
  if (!d) return ''
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}
// 判断预订时间是否为午餐时段（< 15:00）
function isLunchTime(t) {
  if (!t) return true
  const hour = parseInt(String(t).split(':')[0] || '12')
  return hour < 15
}
function onDateChange(e) {
  curDate.value = new Date(e.target.value + 'T00:00:00')
  loadData()
}
function mvDay(n) {
  activeQuick.value = null
  curDate.value = new Date(curDate.value.getTime() + n * 864e5)
  loadData()
}
function setToday() {
  activeQuick.value = null
  curDate.value = new Date()
  loadData()
}

function normalizeBookings(response) {
  const payload = response?.data ?? response ?? []
  if (Array.isArray(payload)) return payload
  return payload.content || payload.records || payload.list || []
}

function buildRanking(primaryKey, secondaryKey) {
  const counts = new Map()
  for (const booking of analysisBookings.value) {
    const name = booking[primaryKey] || booking[secondaryKey] || '未分类'
    counts.set(name, (counts.get(name) || 0) + 1)
  }
  const max = Math.max(1, ...counts.values())
  return [...counts.entries()]
    .map(([name, count]) => ({ name, count, percent: Math.round((count / max) * 100) }))
    .sort((a, b) => b.count - a.count)
}

async function loadBookingRange(startDate, endDate, period = 'all') {
  const response = await getBookings({ storeId: 1, startDate, endDate, period, page: 1, pageSize: 1000 })
  return normalizeBookings(response).filter(booking => {
    const date = String(booking.booking_date || booking.bookingDate || '').slice(0, 10)
    if (date && (date < startDate || date > endDate)) return false
    if (period === 'all') return true
    const time = booking.booking_time || booking.bookingTime || ''
    return period === 'morning' ? isLunchTime(time) : !isLunchTime(time)
  })
}

async function openAnalysis() {
  activeView.value = 'analysis'
  await loadAnalysis()
}

async function loadAnalysis() {
  try {
    analysisBookings.value = await loadBookingRange(analysisStart.value, analysisEnd.value)
  } catch (error) {
    analysisBookings.value = []
    ElMessage.error('经营分析加载失败')
  }
}

function setAnalysisRange(days) {
  const end = new Date()
  const start = new Date(end.getTime() - days * 864e5)
  analysisStart.value = fmtDate(start)
  analysisEnd.value = fmtDate(end)
  loadAnalysis()
}

async function openExportPanel() {
  exportStart.value = activeView.value === 'analysis' ? analysisStart.value : fmtDate(curDate.value)
  exportEnd.value = activeView.value === 'analysis' ? analysisEnd.value : fmtDate(curDate.value)
  exportVisible.value = true
  try {
    exportRows.value = await loadBookingRange(exportStart.value, exportEnd.value, exportPeriod.value)
  } catch {
    exportRows.value = []
    ElMessage.error('导出预览加载失败')
  }
}

function escapeCsv(value) {
  return `"${String(value ?? '').replaceAll('"', '""')}"`
}

function downloadCsv() {
  const header = ['日期', '时段', '桌台', '区域', '客户', '电话', '人数', '状态', '备注']
  const rows = exportRows.value.map(booking => [
    booking.booking_date || booking.bookingDate,
    booking.booking_time || booking.bookingTime,
    booking.table_number || booking.tableNumber,
    booking.table_area || booking.tableArea,
    booking.customer_name || booking.customerName,
    booking.customer_phone || booking.customerPhone,
    booking.guest_count || booking.guestCount || 0,
    booking.booking_status || booking.status,
    booking.notes || booking.remark
  ])
  const csv = '\uFEFF' + [header, ...rows].map(row => row.map(escapeCsv).join(',')).join('\n')
  const link = document.createElement('a')
  link.href = URL.createObjectURL(new Blob([csv], { type: 'text/csv;charset=utf-8' }))
  link.download = `预订数据_${exportStart.value}_${exportEnd.value}.csv`
  link.click()
  URL.revokeObjectURL(link.href)
  exportVisible.value = false
  ElMessage.success('导出完成')
}

function onDialogDateChange(newDate) {
  if (!newDate || !bkDialogRef.value) return
  const oldDate = fmtDate(curDate.value)
  const period = bkDialogRef.value.getCurrentPeriod()
  loadTablesForValidation(newDate, period).then((newList) => {
    const hasConflict = bkDialogRef.value.updateSelectedTables(newList)
    if (hasConflict) {
      bkDialogRef.value.revertDate(oldDate)
    } else {
      curDate.value = new Date(newDate + 'T00:00:00')
      list.value = newList
    }
  }).catch(() => {
    bkDialogRef.value.revertDate(oldDate)
  })
}

function onDialogPeriodChange(newPeriod, oldPeriod, currentDate) {
  if (!bkDialogRef.value) return
  const date = currentDate || fmtDate(curDate.value)
  loadTablesForValidation(date, newPeriod).then((newList) => {
    const hasConflict = bkDialogRef.value.updateSelectedTables(newList)
    if (hasConflict) {
      bkDialogRef.value.revertPeriod(oldPeriod)
    } else {
      timeType.value = newPeriod
      list.value = newList
    }
  }).catch(() => {
    bkDialogRef.value.revertPeriod(oldPeriod)
  })
}

async function loadTablesForValidation(date, timeType) {
  const res = await getTableStatus({ date, timeType })
  if (res.code === 200) {
    return (res.data || []).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
  }
  throw new Error('加载桌台数据失败')
}

function onSaved(bookingData) {
  const date = bookingData?.booking_date || bookingData?.bookingDate
  if (date) {
    const dateStr = typeof date === 'string' && date.includes('T')
      ? date.split('T')[0]
      : date
    // 先加载新日期的数据，让新日期卡片亮起
    loadData(dateStr).then(() => {
      // 切换日期显示
      curDate.value = new Date(dateStr + 'T00:00:00')
    })
  }
  const timeSlot = bookingData?.time_slot || bookingData?.booking_time || bookingData?.bookingTime
  if (timeSlot) {
    const hour = parseInt(String(timeSlot).split(':')[0] || '18')
    timeType.value = hour < 15 ? 'lunch' : 'dinner'
  }
}

// 打印预览回调
function onPrint({ type, data }) {
  printPreviewType.value = type
  printPreviewData.value = data
  printPreviewVisible.value = true
}

// 打开菜单选择器
function openMenuPicker(t) {
  menuPickerTableId.value = t.table_id
  menuPickerInitialDishes.value = t.booking?.dishes || []
  showMenuPicker.value = true
}

// 菜单选择确认回调
function onMenuConfirmed({ dishes, totalPrice }) {
  ElMessage.success(`已选择 ${dishes.length} 道菜，总计 ¥${totalPrice}`)
  showMenuPicker.value = false
}

const areaList = computed(() => ['全部', ...ZONES.filter(a => list.value.some(t => t.table_area === a))])

// 跳转到预订管理界面
function goToBookings() {
  router.push('/dashboard/bookings')
}

// 全天模式选择时段
function selectPeriod(period) {
  showPeriodModal.value = false
  timeType.value = period
  loadData()
  // 如果有选中的桌台，打开预订弹窗
  if (selTable.value) {
    setTimeout(() => {
      initialBooking.value = selTable.value?.booking || null
      bkVis.value = true
    }, 300)
  }
}

// 工具栏定位：固定底部居中
const toolbarStyle = computed(() => {
  const toolbarWidth = 500
  const left = Math.max(16, (window.innerWidth - toolbarWidth) / 2)
  return {
    position: 'fixed',
    left: left + 'px',
    bottom: '16px',
    top: 'auto',
    right: 'auto',
    transform: 'none'
  }
})

// 浮动工具栏相关计算属性
const selectedTables = computed(() => list.value.filter(t => selIds.value.includes(t.table_id)))
const bookedList = computed(() => selectedTables.value.filter(t => t.booking && t.booking.booking_status !== 'cancelled'))
const emptyList = computed(() => selectedTables.value.filter(t => !t.booking || t.booking.booking_status === 'cancelled'))
const hasBooked = computed(() => bookedList.value.length > 0)
const hasEmpty = computed(() => emptyList.value.length > 0)
const canCopy = computed(() => bookedList.value.length === 1 && emptyList.value.length >= 1)
const canSwap = computed(() => selIds.value.length === 2 && hasBooked.value)
const canDelete = computed(() => hasBooked.value)

// 判断选中预订的类型



const showActionToolbar = computed(() => !edit.value && selIds.value.length >= 1 && timeType.value !== 'all')

const actionSummary = computed(() => {
  const total = selIds.value.length
  const parts = []
  if (bookedList.value.length > 0) parts.push(`${bookedList.value.length}个已预订`)
  if (emptyList.value.length > 0) parts.push(`${emptyList.value.length}个空闲`)
  return `已选 ${total} 个桌台（${parts.join('，')}）`
})

const displayList = computed(() => {
  return list.value.filter(t => {
    if (area.value !== '全部' && t.table_area !== area.value) return false
    if (statusFilter.value === 'free' && t.booking) return false
    if (statusFilter.value === 'booked' && !t.booking) return false
    return true
  })
})

const freeN = computed(() => list.value.filter(t => !t.booking).length)
const bookedN = computed(() => list.value.filter(t => t.booking && t.booking.booking_status !== 'cancelled').length)

function tcls(t) {
  if (!t.booking) return 'free'
  return 'booked'
}

function cardClass(t) {
  const c = ['table-item', 'status-' + tcls(t)]
  if (t.booking && t.booking.time_type) {
    c.push('time-' + t.booking.time_type)
  }
  if (selIds.value.includes(t.table_id)) c.push('selected')
  if (edit.value) c.push('editing')
  if (dragSrcIdx.value !== null && displayList.value[dragSrcIdx.value]?.table_id === t.table_id) c.push('dragging')
  if (dragTargetIdx.value !== null && dragInsertPos.value === 'before' && displayList.value[dragTargetIdx.value]?.table_id === t.table_id) c.push('insert-before')
  if (dragTargetIdx.value !== null && dragInsertPos.value === 'after' && displayList.value[dragTargetIdx.value]?.table_id === t.table_id) c.push('insert-after')
  return c.join(' ')
}

function toggleSel(t, e) {
  if (edit.value) return
  // 记录点击位置
  selPosition.value = { x: e.clientX, y: e.clientY }
  // 调换模式：点击目标桌台执行调换
  if (swapMode.value !== null) {
    if (t.table_id === swapMode.value) return
    performSwap(swapMode.value, t.table_id)
    return
  }
  // 全天模式下点击卡片弹出时段选择弹窗
  if (timeType.value === 'all') {
    selIds.value = [t.table_id]
    selTable.value = t
    showPeriodModal.value = true
    return
  }
  // Ctrl/Cmd + 点击 = 多选
  if (e.ctrlKey || e.metaKey) {
    const idx = selIds.value.indexOf(t.table_id)
    if (idx >= 0) {
      selIds.value.splice(idx, 1)
    } else {
      selIds.value.push(t.table_id)
    }
    return
  }
  // 普通单击 = 单选切换
  const idx = selIds.value.indexOf(t.table_id)
  if (idx >= 0) {
    selIds.value.splice(idx, 1)
  } else {
    selIds.value = [t.table_id]
  }
}

function openBooking(t) {
  if (edit.value) return
  // 关闭时段选择弹窗（双击时优先）
  showPeriodModal.value = false
  // 全天模式下双击桌台
  if (timeType.value === 'all' && t.booking) {
    // 有两个订单（上午+下午），不可编辑，以只读模式打开
    if (t.booking.booking_id2) {
      selIds.value = [t.table_id]
      selTable.value = t
      initialBooking.value = { ...t.booking, _noEdit: true }
      bkVis.value = true
      return
    }
    // 只有一个订单，推断时段并切换（午餐/晚餐按钮自动亮起），直接打开编辑界面
    const bt = t.booking.booking_time || t.booking.time_slot || ''
    const hour = parseInt(String(bt).split(':')[0] || '18')
    const inferred = hour < 15 ? 'lunch' : 'dinner'
    timeType.value = inferred
    selIds.value = [t.table_id]
    selTable.value = t
    // 延迟等待数据加载后打开预订弹窗
    setTimeout(() => {
      initialBooking.value = { ...t?.booking, _editMode: true } || null
      bkVis.value = true
    }, 300)
    return
  }
  selIds.value = [t.table_id]
  selTable.value = t
  initialBooking.value = t?.booking ? { ...t.booking, _editMode: true } : (t?.bk ? { ...t.bk, _editMode: true } : null)
  bkVis.value = true
}

function selClear() {
  selIds.value = []
  swapMode.value = null
  ctxMenuVisible.value = false
  ctxMenuTable.value = null
}

// 右键菜单
function onContextMenu(e, t) {
  if (edit.value) return
  // 右键点击的桌台若未选中，则选中它
  if (!selIds.value.includes(t.table_id)) {
    selIds.value = [t.table_id]
  }
  ctxMenuTable.value = t
  // 计算菜单位置（防止超出视口）
  const menuW = 220
  const menuH = 200
  let x = e.clientX
  let y = e.clientY
  if (x + menuW > window.innerWidth) x = window.innerWidth - menuW - 10
  if (y + menuH > window.innerHeight) y = window.innerHeight - menuH - 10
  ctxMenuPos.value = { x, y }
  ctxMenuVisible.value = true
}

function hideCtxMenu() {
  ctxMenuVisible.value = false
}

// 右键菜单 - 预订录入（打开预订弹窗）
function ctxNewBooking() {
  if (!ctxMenuTable.value) return
  // 全天模式下双击桌台推断时段
  const t = ctxMenuTable.value
  if (timeType.value === 'all' && t.booking) {
    const bt = t.booking.booking_time || t.booking.time_slot || ''
    const hour = parseInt(String(bt).split(':')[0] || '18')
    const inferred = hour < 15 ? 'lunch' : 'dinner'
    if (timeType.value !== inferred) {
      timeType.value = inferred
      loadData()
    }
  }
  selTable.value = t
  initialBooking.value = t?.booking || null
  bkVis.value = true
  ctxMenuVisible.value = false
}

// 右键菜单 - 快速占用（空闲桌台直接标记为占用，无需填写预订信息）
function ctxQuickOccupy() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  // 直接打开预订录入，快速填写
  selTable.value = t
  initialBooking.value = null
  bkVis.value = true
  ctxMenuVisible.value = false
}

// 右键菜单 - 编辑预订（如果没有预订则打开录入窗体）
function ctxEditBooking() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  if (timeType.value === 'all' && t.booking) {
    const bt = t.booking.booking_time || t.booking.time_slot || ''
    const hour = parseInt(String(bt).split(':')[0] || '18')
    const inferred = hour < 15 ? 'lunch' : 'dinner'
    if (timeType.value !== inferred) {
      timeType.value = inferred
      loadData()
    }
  }
  selTable.value = t
  initialBooking.value = t?.booking ? { ...t.booking, _editMode: true } : null
  bkVis.value = true
  ctxMenuVisible.value = false
}

// 右键菜单 - 删除预订
function ctxDeleteBooking() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  if (!t.booking) {
    ElMessage.warning('该桌台没有预订')
    ctxMenuVisible.value = false
    return
  }
  if (timeType.value === 'all') {
    ElMessage.warning('全天模式下无法删除，请先切换到午餐或晚餐时段')
    ctxMenuVisible.value = false
    return
  }
  ctxMenuVisible.value = false
  ElMessageBox.confirm(
    `删除「${t.table_number}」的预订？\n\n客户：${t.booking.customer_name || '-'}`,
    '删除预订 · Delete Booking',
    { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error', confirmButtonClass: 'el-button--danger' }
  ).then(async () => {
    try {
      if (t.booking?.booking_id) {
        await cancelBooking(t.booking.booking_id)
      }
      ElMessage.success('已删除')
      loadData()
    } catch {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

function toggleEdit() {
  edit.value = !edit.value
  selClear()
}

// 右键菜单 - 客户查询
function ctxCustomerLookup() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  const phone = t.booking?.customer_phone || ''
  customerAnalysisPhone.value = phone
  showCustomerAnalysis.value = true
  ctxMenuVisible.value = false
}

// 复制预订：将已预订桌台复制到选中的空闲桌台（用后端API）
async function copyBookingToSelected() {
  if (!hasBooked.value || !hasEmpty.value) {
    ElMessage.warning('需要同时选中已预订和空闲桌台')
    return
  }
  const source = bookedList.value[0]
  const targets = emptyList.value
  const targetNames = targets.map(t => t.table_number).join('、')
  const bk = source.booking
  try {
    await ElMessageBox.confirm(
      `将「${source.table_number}」的预订复制到以下桌台？\n\n${targetNames}\n\n客户：${bk.customer_name || '-'}\n电话：${bk.customer_phone || '-'}`,
      '复制预订 · Copy Booking',
      { confirmButtonText: '确定复制', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }
  try {
    const targetTableIds = targets.map(t => t.table_id)
    await copyBooking({
      sourceBookingId: bk.booking_id,
      date: fmtDate(curDate.value),
      time: bk.booking_time || '12:00',
      targetTableIds,
      storeId: 1
    })
    ElMessage.success(`已复制到 ${targets.length} 个桌台`)
    selClear()
    loadData()
  } catch (e) {
    ElMessage.error('复制失败：' + (e?.message || '未知错误'))
  }
}

// 进入调换模式
function startSwapMode() {
  if (selIds.value.length !== 2) {
    ElMessage.warning('请选择恰好2个桌台进行调换')
    return
  }
  if (!hasBooked.value) {
    ElMessage.warning('至少需要1个桌台有预订')
    return
  }
  const [t1, t2] = selectedTables.value
  performSwap(t1.table_id, t2.table_id)
}

// 执行调换（用后端API交换两个桌台的预订数据，支持一对一、一对空）
async function performSwap(srcId, dstId) {
  const src = list.value.find(t => t.table_id === srcId)
  const dst = list.value.find(t => t.table_id === dstId)
  if (!src || !dst) return
  const srcBooked = src.booking && src.booking.booking_status !== 'cancelled'
  const dstBooked = dst.booking && dst.booking.booking_status !== 'cancelled'
  let msg = ''
  if (srcBooked && dstBooked) {
    msg = `将「${src.table_number}」和「${dst.table_number}」的预订互换？`
  } else if (srcBooked && !dstBooked) {
    msg = `将「${src.table_number}」的预订转移到「${dst.table_number}」？`
  } else if (!srcBooked && dstBooked) {
    msg = `将「${dst.table_number}」的预订转移到「${src.table_number}」？`
  } else {
    ElMessage.warning('至少需要1个桌台有预订')
    return
  }
  try {
    await ElMessageBox.confirm(msg, '调换台号 · Swap Table',
      { confirmButtonText: '确定调换', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    swapMode.value = null
    return
  }
  try {
    await swapBooking({
      fromTableId: srcId,
      toTableId: dstId,
      date: fmtDate(curDate.value),
      period: timeType.value === 'lunch' ? 'morning' : 'afternoon',
      storeId: 1
    })
    ElMessage.success('调换成功')
    swapMode.value = null
    selClear()
    loadData()
  } catch (e) {
    ElMessage.error('调换失败：' + (e?.message || '未知错误'))
    swapMode.value = null
  }
}

// 快速删除预订
async function quickDeleteBooking() {
  if (!hasBooked.value) {
    ElMessage.warning('请选择有预订的桌台')
    return
  }
  const names = bookedList.value.map(t => t.table_number).join('、')
  try {
    await ElMessageBox.confirm(
      `删除以下桌台的预订？\n\n${names}\n\n共 ${bookedList.value.length} 个预订`,
      '删除预订 · Delete Booking',
      { confirmButtonText: '删除', cancelButtonText: '取消', type: 'error', confirmButtonClass: 'el-button--danger' }
    )
  } catch { return }
  try {
    for (const t of bookedList.value) {
      if (t.booking?.booking_id) {
        await cancelBooking(t.booking.booking_id)
      }
    }
    ElMessage.success(`已删除 ${bookedList.value.length} 个预订`)
    selClear()
    loadData()
  } catch (e) {
    ElMessage.error('删除失败：' + (e?.message || '未知错误'))
  }
}

function confirmAddTable() {
  if (!newTableName.value.trim()) { ElMessage.warning('请输入桌台编号'); return }
  if (!newTablePeople.value || parseInt(newTablePeople.value) <= 0) { ElMessage.warning('请输入有效人数'); return }
  if (list.value.some(t => t.table_number === newTableName.value.trim())) { ElMessage.warning('桌台名称已存在'); return }
  createTable({
    table_number: newTableName.value.trim(),
    table_capacity: parseInt(newTablePeople.value),
    table_area: newTableArea.value
  }).then(() => {
    ElMessage.success('已添加')
    showAddModal.value = false
    newTableName.value = ''
    newTablePeople.value = ''
    loadData()
  }).catch(() => ElMessage.error('添加失败'))
}

// P2-2 打印预订通知单
function printBooking() {
  let items = bookedList.value
  // 全天模式下bookedList可能为空，回退到ctxMenuTable作为打印目标
  if (!items.length && ctxMenuTable.value && ctxMenuTable.value.booking) {
    items = [ctxMenuTable.value]
  }
  if (!items.length) return
  const win = window.open('', '_blank', 'width=700,height=600')
  if (!win) { ElMessage.warning('请允许弹窗'); return }
  let html = '<html><head><meta charset="utf-8"><style>body{font-family:SimSun,serif;padding:20px}' +
    'h2{text-align:center}h4{text-align:center;color:#666}table{width:100%%;border-collapse:collapse;margin-top:20px}' +
    'th,td{border:1px solid #333;padding:8px;font-size:14px}th{background:#f5f5f5}.foot{margin-top:30px;text-align:right}</style></head><body>' +
    '<h2>又见炊烟 · 订台通知单</h2><h4>' + fmtDate(curDate.value) + '</h4>' +
    '<table><tr><th>台号</th><th>客户</th><th>电话</th><th>人数</th><th>时间</th><th>菜品</th></tr>'
  items.forEach(t => {
    const b = t.booking
    html += '<tr><td>' + t.table_number + '</td><td>' + (b.customer_name || '-') + '</td>' +
      '<td>' + (b.customer_phone || '-') + '</td><td>' + (b.guest_count || '-') + '</td>' +
      '<td>' + (b.booking_time || '').slice(0, 5) + '</td><td>' + (b.dishes_count || 0) + '道</td></tr>'
  })
  html += '</table><p class="foot">打印时间: ' + new Date().toLocaleString() + '</p></body></html>'
  win.document.write(html)
  win.document.close()
  setTimeout(() => win.print(), 500)
}

async function deleteTableItem(t) {
  // 全天模式下禁用删除有预订的桌台（参考宁国店预定系统：需明确时段才能操作预订）
  if (t.booking && timeType.value === 'all') {
    ElMessage.warning('全天模式下无法删除带预订的桌台，请先切换到午餐或晚餐时段')
    return
  }
  const hasBooking = t.booking !== null
  try {
    if (hasBooking) {
      await ElMessageBox.confirm(`桌台「${t.table_number}」当前有预订，删除它将同时删除预订。确定删除吗？`, '确认', {
        confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning'
      })
    } else {
      await ElMessageBox.confirm(`删除「${t.table_number}」？`, '确认', {
        confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
      })
    }
  } catch { return }
  try {
    if (t.table_id > 0) await deleteTable(t.table_id)
    list.value = list.value.filter(x => x.table_id !== t.table_id)
    ElMessage.success(`已删除「${t.table_number}」`)
  } catch { ElMessage.error('删除失败') }
}

function onDragStart(e, i) {
  if (!edit.value) { e.preventDefault(); return }
  dragSrcIdx.value = i
  e.dataTransfer.effectAllowed = 'move'
  e.dataTransfer.setData('text/plain', String(i))
}

function onDragOver(e, i) {
  if (!edit.value) return
  e.preventDefault()
  e.dataTransfer.dropEffect = 'move'
  if (dragSrcIdx.value === null || dragSrcIdx.value === i) return
  const el = e.target.closest('.table-item')
  if (!el) return
  const rect = el.getBoundingClientRect()
  const leftHalf = (e.clientX - rect.left) < rect.width / 2
  dragTargetIdx.value = i
  dragInsertPos.value = leftHalf ? 'before' : 'after'
}

function onDragLeave() {
  dragTargetIdx.value = null
}

function onDrop(e, i) {
  e.preventDefault()
  if (!edit.value || dragSrcIdx.value === null) return
  const dl = displayList.value
  const srcItem = dl[dragSrcIdx.value]
  if (!srcItem) { resetDrag(); return }
  const srcGlobalIdx = list.value.findIndex(t => t.table_id === srcItem.table_id)
  list.value.splice(srcGlobalIdx, 1)
  let targetGlobalIdx = list.value.findIndex(t => t.table_id === dl[i]?.table_id)
  if (dragInsertPos.value === 'after') targetGlobalIdx++
  list.value.splice(targetGlobalIdx, 0, srcItem)
  const order = list.value.map((t, idx) => ({ table_id: t.table_id, sort_order: idx }))
  reorderTables(order).then(() => ElMessage.success('桌台顺序已更新')).catch(() => {})
  resetDrag()
}

function onDragEnd() {
  resetDrag()
}

function resetDrag() {
  dragSrcIdx.value = null
  dragTargetIdx.value = null
  dragInsertPos.value = 'after'
}

const boardPeriod = computed(() => {
  if (timeType.value === 'lunch') return 'morning'
  if (timeType.value === 'dinner') return 'afternoon'
  return 'all'
})

async function loadData(customDate) {
  try {
    const dateToLoad = customDate || fmtDate(curDate.value)
    const res = await getTableBoard({ storeId: 1, date: dateToLoad, period: boardPeriod.value })
    if (res.code === 200) {
      const raw = res.data || []
      // 转换board格式为TableBoard用的list格式
      list.value = raw.map(t => ({
        table_id: t.table_id,
        store_id: t.store_id,
        table_number: t.table_number,
        table_name: t.table_name,
        table_area: t.table_area,
        table_capacity: t.table_capacity,
        table_type: t.table_type,
        table_status: t.booking_id ? 'occupied' : 'available',
        sort_order: t.sort_order,
        is_active: t.is_active,
        // 预订信息
        booking: t.booking_id ? {
          booking_id: t.booking_id,
          booking_date: t.booking_date,
          booking_time: t.booking_time,
          customer_name: t.customer_name,
          customer_phone: t.customer_phone,
          booking_status: t.booking_status,
          banquet_name: t.banquet_name,
          occasion_type: t.occasion_type,
          guest_count: t.bm_guest_count,
          dishes_count: t.dishes_count,
          visit_count: t.visit_count || 0,
          // 全天模式双预订
          booking_id2: t.booking_id2,
          customer_name2: t.customer_name2,
          dishes_count2: t.dishes_count2,
          booking_time2: t.booking_time2,
          guest_count2: t.guest_count2 || t.bt_guest_count2
        } : null
      })).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
    }
  } catch (e) { console.error('load', e) }
}

watch([exportStart, exportEnd, exportPeriod], async () => {
  if (!exportVisible.value || !exportStart.value || !exportEnd.value) return
  try {
    exportRows.value = await loadBookingRange(exportStart.value, exportEnd.value, exportPeriod.value)
  } catch {
    exportRows.value = []
  }
})

onMounted(() => {
  loadData()
  // ESC键取消桌台选择（参考宁国店预定系统）
  window.addEventListener('keydown', onKeydown)
  // 点击空白处关闭右键菜单
  window.addEventListener('click', hideCtxMenu)
  
  // 处理路由参数：从预订管理页面跳转过来时，自动打开预订弹窗
  const bookingId = route.query.bookingId
  const isEdit = route.query.edit === 'true'
  const isDish = route.query.dish === 'true'
  if (bookingId) {
    // 延迟一下确保数据加载完成
    setTimeout(() => {
      initialBooking.value = { booking_id: bookingId, _editMode: isEdit, _goDishTab: isDish }
      bkVis.value = true
    }, 500)
  }
  // 处理快速开台：从工作台跳转过来时，自动选择第一个空闲桌台
  if (route.query.quickOpen === 'true') {
    setTimeout(() => {
      const freeTables = list.value.filter(t => !t.booking || t.booking.booking_status === 'cancelled')
      if (freeTables.length > 0) {
        selTable.value = freeTables[0]
        initialBooking.value = null
        bkVis.value = true
      }
    }, 800)
  }
})
onUnmounted(() => {
  window.removeEventListener('keydown', onKeydown)
  window.removeEventListener('click', hideCtxMenu)
})

function onKeydown(e) {
  if (e.key === 'Escape') {
    if (ctxMenuVisible.value) {
      ctxMenuVisible.value = false
      return
    }
    if (selIds.value.length > 0 && !edit.value && !bkVis.value) {
      selClear()
    }
  }
}
</script>

<style scoped>
.tboard {
  --ref-ink: #1e293b;
  --ref-muted: #64748b;
  --ref-primary: #4f46e5;
  --ref-accent: #7c3aed;
  --ref-surface: rgba(255, 255, 255, 0.72);
  padding: 28px;
  min-height: 100%;
  color: var(--ref-ink);
  background:
    linear-gradient(140deg, rgba(238, 242, 255, 0.92), rgba(248, 250, 252, 0.96) 48%, rgba(245, 243, 255, 0.9));
}
.reference-hero { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; padding: 12px 4px 24px; }
.reference-hero .eyebrow { margin: 0 0 8px; color: var(--ref-primary); font-size: 11px; font-weight: 800; letter-spacing: .18em; }
.reference-hero h1 { margin: 0; font-size: clamp(26px, 3vw, 42px); line-height: 1.15; letter-spacing: -.03em; }
.hero-copy { margin: 10px 0 0; color: var(--ref-muted); font-size: 14px; }
.hero-actions { display: flex; gap: 10px; }
.hero-btn, .soft-btn, .primary-btn { min-height: 38px; padding: 0 18px; border: 1px solid rgba(99,102,241,.18); border-radius: 12px; background: rgba(255,255,255,.68); color: var(--ref-ink); font-weight: 700; cursor: pointer; transition: .2s ease; }
.hero-btn:hover, .soft-btn:hover { transform: translateY(-1px); border-color: var(--ref-primary); color: var(--ref-primary); }
.hero-btn.primary, .primary-btn { border: 0; color: #fff; background: linear-gradient(135deg, var(--ref-primary), var(--ref-accent)); box-shadow: 0 12px 28px rgba(79,70,229,.2); }
.page-tabs { display: flex; gap: 6px; width: fit-content; padding: 5px; margin-bottom: 18px; border: 1px solid rgba(255,255,255,.8); border-radius: 14px; background: rgba(255,255,255,.52); backdrop-filter: blur(20px); }
.page-tabs button { padding: 9px 22px; border: 0; border-radius: 10px; background: transparent; color: var(--ref-muted); font-weight: 700; cursor: pointer; }
.page-tabs button.active { color: #fff; background: linear-gradient(135deg, var(--ref-primary), var(--ref-accent)); }
.glass-panel { border: 1px solid rgba(255,255,255,.9); border-radius: 22px; background: var(--ref-surface); box-shadow: 0 18px 50px rgba(71,85,105,.1); backdrop-filter: blur(24px); }
.board-panel { padding: 18px; }
.summary-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; margin: 4px 0 18px; }
.summary-card { display: grid; grid-template-columns: 1fr auto; align-items: end; gap: 4px 12px; padding: 15px 16px; border: 1px solid rgba(148,163,184,.16); border-radius: 16px; background: rgba(255,255,255,.6); }
.summary-card span { color: var(--ref-muted); font-size: 13px; }
.summary-card strong { grid-row: span 2; font-size: 28px; color: var(--ref-ink); }
.summary-card small { color: #94a3b8; font-size: 9px; letter-spacing: .12em; }
.summary-card.available strong { color: #059669; }.summary-card.reserved strong { color: #7c3aed; }.summary-card.guests strong { color: #d97706; }
.filter-heading { display: flex; align-items: center; justify-content: space-between; gap: 16px; margin: 4px 0 12px; }
.filter-heading > div:first-child { display: flex; align-items: baseline; gap: 10px; }.filter-heading strong { font-size: 17px; }.filter-heading span { color: var(--ref-muted); font-size: 12px; }
.legend { display: flex; gap: 14px; }.legend span { display: inline-flex; align-items: center; gap: 6px; }.dot { width: 8px; height: 8px; border-radius: 50%; }.dot.free { background: #10b981; }.dot.booked { background: #7c3aed; }
.empty-state { grid-column: 1 / -1; display: flex; flex-direction: column; align-items: center; gap: 6px; padding: 60px 20px; color: var(--ref-muted); }.empty-state strong { color: var(--ref-ink); }
.analysis-page { display: flex; flex-direction: column; gap: 16px; }.analysis-toolbar { display: flex; align-items: end; justify-content: space-between; gap: 16px; padding: 16px; }.analysis-dates,.analysis-actions { display: flex; gap: 10px; align-items: end; flex-wrap: wrap; }.analysis-dates label { display: flex; flex-direction: column; gap: 5px; color: var(--ref-muted); font-size: 12px; }.analysis-dates input { height: 38px; padding: 0 12px; border: 1px solid rgba(148,163,184,.25); border-radius: 10px; background: rgba(255,255,255,.7); }
.analysis-kpis { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 12px; }.analysis-kpis article { padding: 20px; border-radius: 18px; color: #fff; background: linear-gradient(135deg, #4f46e5, #7c3aed); box-shadow: 0 16px 32px rgba(79,70,229,.18); }.analysis-kpis article:nth-child(2) { background: linear-gradient(135deg,#059669,#10b981); }.analysis-kpis article:nth-child(3) { background: linear-gradient(135deg,#d97706,#f59e0b); }.analysis-kpis article:nth-child(4) { background: linear-gradient(135deg,#334155,#475569); }.analysis-kpis span { display:block;font-size:12px;opacity:.75; }.analysis-kpis strong { display:block;margin-top:8px;font-size:30px; }.analysis-kpis strong.textual { font-size:18px; }
.analysis-grid { display: grid; grid-template-columns: 1.4fr 1fr; gap: 16px; }.ranking-card,.detail-card { padding: 20px; }.section-title { display:flex;align-items:center;justify-content:space-between;gap:12px;margin-bottom:18px; }.section-title small { color:var(--ref-primary);font-weight:800;letter-spacing:.13em; }.section-title h2 { margin:4px 0 0;font-size:18px; }
.bar-list { display:flex;flex-direction:column;gap:16px; }.bar-item > div:first-child { display:flex;justify-content:space-between;font-size:13px; }.bar-track { height:8px;margin-top:7px;border-radius:99px;background:#e2e8f0;overflow:hidden; }.bar-track i { display:block;height:100%;border-radius:inherit;background:linear-gradient(90deg,var(--ref-primary),var(--ref-accent)); }.rank-list { display:flex;flex-direction:column;gap:10px;padding:0;margin:0;list-style:none; }.rank-list li { display:grid;grid-template-columns:28px 1fr auto;align-items:center;gap:10px;padding:10px;border-radius:12px;background:rgba(248,250,252,.82); }.rank-list b { display:grid;place-items:center;width:24px;height:24px;border-radius:8px;background:#ede9fe;color:#6d28d9; }.analysis-empty { display:grid;place-items:center;min-height:180px;color:var(--ref-muted); }
.detail-table-wrap { overflow:auto;max-height:360px; }.detail-table { width:100%;border-collapse:collapse;font-size:13px; }.detail-table th,.detail-table td { padding:11px 12px;border-bottom:1px solid rgba(148,163,184,.16);text-align:left;white-space:nowrap; }.detail-table th { position:sticky;top:0;background:#f8fafc;color:var(--ref-muted); }.export-box { width:440px;border-radius:20px; }.modal-title-row { display:flex;align-items:center;justify-content:space-between;margin-bottom:18px; }.modal-title-row h3 { margin:3px 0 0; }.modal-title-row small { color:var(--ref-primary);font-weight:800;letter-spacing:.15em; }.modal-title-row button { border:0;background:transparent;font-size:24px;cursor:pointer; }.export-preview { display:flex;align-items:baseline;justify-content:center;gap:8px;padding:16px;border-radius:14px;background:#eef2ff;color:var(--ref-muted); }.export-preview strong { color:var(--ref-primary);font-size:26px; }
@media (max-width: 800px) { .tboard { padding:16px; }.reference-hero,.analysis-toolbar { align-items:stretch;flex-direction:column; }.hero-actions { width:100%; }.hero-btn { flex:1; }.summary-grid,.analysis-kpis { grid-template-columns:repeat(2,1fr); }.analysis-grid { grid-template-columns:1fr; }.date-nav { padding:12px; }.board-panel { padding:12px; } }
@media (max-width: 520px) { .summary-grid,.analysis-kpis { grid-template-columns:1fr 1fr; }.reference-hero h1 { font-size:26px; }.page-tabs { width:100%; }.page-tabs button { flex:1; }.filter-heading { align-items:flex-start;flex-direction:column; } }

.date-nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 8px 0 16px;
  background: transparent;
  border: 0;
  margin-bottom: 2px;
  flex-wrap: wrap;
  box-shadow: none;
}
.nav-sep { width: 1px; height: 24px; background: var(--color-border); margin: 0 6px; }
.date-input {
  height: 34px;
  padding: 6px 10px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-bg);
  font-size: 14px;
  color: var(--color-text-primary);
  outline: none;
  cursor: pointer;
  width: 154px;
}
.date-input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 2px rgba(45, 74, 62, 0.1); }
.nav-btn {
  width: 34px;
  height: 34px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 18px;
  line-height: 1;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.nav-btn:hover {
  background: var(--color-primary-light);
  border-color: var(--color-primary);
  color: var(--color-primary);
  transform: translateY(-1px);
}
.today-btn {
  height: 34px;
  padding: 0 16px;
  border: 1px solid var(--color-primary);
  border-radius: 2px;
  background: var(--color-primary);
  color: var(--color-bg);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.today-btn:hover { background: var(--color-primary-dark); }
.quick-btn {
  height: 34px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.quick-btn:hover { background: var(--color-primary-light); color: var(--color-primary); border-color: var(--color-primary); }
.quick-btn.active { background: var(--color-primary); color: var(--color-bg); border-color: var(--color-primary); }
.period-btn {
  height: 34px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.period-btn:hover { background: var(--color-primary-light); color: var(--color-primary); }
.period-btn.active { background: var(--color-primary); color: var(--color-bg); border-color: var(--color-primary); }
.period-btn.lunch.active { background: var(--color-accent); border-color: var(--color-accent); color: var(--color-text-primary); }
.period-btn.dinner.active { background: var(--color-primary); border-color: var(--color-primary); }
.edit-btn {
  height: 34px;
  padding: 0 16px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-bg);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
}
.edit-btn:hover { border-color: var(--color-text-secondary); }
.edit-btn.editing { background: #C0392B; color: #fff; border-color: #C0392B; }
.edit-btn.nav-btn-bookings {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.edit-btn.nav-btn-bookings:hover {
  background: var(--color-primary-dark);
  border-color: var(--color-primary-dark);
}

.area-row { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 14px; }
.area-btn {
  padding: 7px 18px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.area-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.area-btn.active { background: var(--color-primary); color: var(--color-bg); border-color: var(--color-primary); }

.status-row { display: flex; gap: 0; margin-bottom: 18px; }
.status-btn {
  padding: 7px 22px;
  border: 1px solid var(--color-border);
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  outline: none;
  transition: all 0.2s;
}
.status-btn:first-child { border-radius: 2px 0 0 2px; }
.status-btn:last-child { border-radius: 0 2px 2px 0; }
.status-btn + .status-btn { border-left: none; }
.status-btn.active { background: var(--color-primary); color: var(--color-bg); border-color: var(--color-primary); }
.sb-badge {
  display: inline-block;
  min-width: 22px;
  height: 18px;
  line-height: 18px;
  padding: 0 6px;
  border-radius: 9px;
  background: rgba(0, 0, 0, 0.08);
  font-size: 11px;
  margin-left: 5px;
  text-align: center;
}
.status-btn.active .sb-badge { background: rgba(255, 255, 255, 0.25); color: var(--color-bg); }

.edit-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 10px 16px;
  background: rgba(196, 163, 90, 0.08);
  border-radius: 2px;
  border: 1px solid rgba(196, 163, 90, 0.3);
}
.add-btn { height: 34px; padding: 0 18px; background: var(--color-primary); color: #fff; border: none; border-radius: 2px; font-size: 13px; cursor: pointer; transition: all 0.2s; }
.add-btn:hover { background: var(--color-primary-dark); }
.edit-hint { font-size: 12px; color: var(--color-text-secondary); }

.tgrid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin-bottom: 24px;
}
@media (min-width: 640px) { .tgrid { grid-template-columns: repeat(4, 1fr); } }
@media (min-width: 768px) { .tgrid { grid-template-columns: repeat(6, 1fr); } }
@media (min-width: 1024px) { .tgrid { grid-template-columns: repeat(8, 1fr); } }
@media (min-width: 1280px) { .tgrid { grid-template-columns: repeat(9, 1fr); } }
.tgrid-with-fab {
  padding-bottom: 72px;
}

.table-item {
  background: rgba(255,255,255,.78);
  border: 1px solid rgba(148,163,184,.2);
  border-radius: 18px;
  padding: 14px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  aspect-ratio: 1;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  position: relative;
}
.table-item:hover {
  border-color: var(--color-primary);
  box-shadow: 0 4px 12px rgba(45, 74, 62, 0.1);
}

.table-item.selected {
  border: 3px solid #7c3aed;
  box-shadow: 0 0 0 4px rgba(124, 58, 237, 0.25), 0 4px 12px rgba(124, 58, 237, 0.2);
  transform: none;
  background: rgba(124, 58, 237, 0.08) !important;
}
.table-item.selected::after {
  content: '\2713';
  position: absolute;
  top: 6px;
  right: 8px;
  width: 22px;
  height: 22px;
  background: #7c3aed;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.table-item.status-free { border-color: rgba(45, 74, 62, 0.2); }
.table-item.status-booked {
  background: #fef9c3 !important;
  border: 1px solid #eab308 !important;
}

.table-item.editing {
  cursor: grab;
  border: 2px dashed var(--color-primary);
  background: linear-gradient(135deg, rgba(45, 74, 62, 0.05) 0%, rgba(255, 255, 255, 0.9) 100%);
  transform: scale(0.97);
  border-radius: 24px;
}
.table-item.editing:active { cursor: grabbing; }

.table-item.dragging { opacity: 0.4; transform: scale(0.92); }
.table-item.insert-before {
  border-left: 5px solid var(--color-accent) !important;
  transform: translateX(4px);
  background: rgba(196, 163, 90, 0.08) !important;
}
.table-item.insert-after {
  border-right: 5px solid var(--color-accent) !important;
  transform: translateX(-4px);
  background: rgba(196, 163, 90, 0.08) !important;
}

.table-item .table-name { font-weight: 700; font-size: 16px; color: var(--color-text-primary); }
.table-item .table-capacity { font-size: 11px; color: #94a3b8; margin-top: 2px; }

.table-guest { font-size: 13px; font-weight: 500; margin-top: 4px; background: rgba(234, 179, 8, 0.15); padding: 2px 12px; border-radius: 30px; display: inline-block; }
.vip-badge { font-size: 10px; margin-left: 6px; background: linear-gradient(135deg, #F5B041, #E67E22); color: #fff; padding: 2px 6px; border-radius: 2px; vertical-align: middle; }
.regular-badge { font-size: 10px; margin-left: 6px; background: rgba(196, 163, 90, 0.3); color: var(--color-primary); padding: 2px 6px; border-radius: 2px; vertical-align: middle; font-weight: 500; }
.table-phone { font-size: 11px; color: #64748b; margin-top: 2px; }
.table-booked-people { font-size: 12px; font-weight: 600; color: #854d0e; margin-top: 2px; }
.table-dish-count { font-size: 12px; color: var(--color-accent); margin-top: 2px; font-weight: 500; }
.table-booking-time { font-size: 11px; color: var(--color-text-secondary); margin-top: 2px; }
.banquet-type-badge { display: inline-block; font-size: 10px; padding: 2px 8px; border-radius: 2px; background: var(--color-primary); color: #fff; margin-top: 4px; letter-spacing: 1px; }
/* 全天模式：上下午两行容器 */
.tbinfo-both {
  display: flex;
  flex-direction: column;
  gap: 4px;
  margin-top: 4px;
  width: 100%;
  align-items: stretch;
}
/* 单行上午/下午摘要（严格参考单页文件 .table-summary） */
.table-summary {
  font-size: 13px;
  font-weight: 600;
  color: #4f46e5;
  margin-top: 4px;
  display: flex;
  gap: 6px;
  justify-content: center;
  align-items: center;
  background: rgba(79, 70, 229, 0.08);
  padding: 4px 12px;
  border-radius: 30px;
}
.table-summary .ts-tag { font-size: 11px; font-weight: 700; opacity: 0.7; }
.table-summary .ts-name { min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.table-summary .ts-count { font-weight: 500; opacity: 0.85; font-size: 12px; }
.table-summary.empty { color: #94a3b8; }

.status-dot { width: 10px; height: 10px; border-radius: 50%; display: inline-block; margin-top: 6px; }
.status-free .status-dot { background: #22c55e; }
.status-booked .status-dot { background: #7c3aed; }
.table-item.selected .status-dot { display: none; }

.drag-handle { position: absolute; top: 15px; left: 15px; font-size: 27px; color: var(--color-text-secondary); cursor: grab; }
.drag-handle:active { cursor: grabbing; }
.delete-btn {
  position: absolute;
  top: -10px;
  right: -10px;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #C0392B;
  color: #fff;
  border: 2px solid #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(192, 57, 43, 0.3);
  transition: 0.2s;
}
.delete-btn:hover { transform: scale(1.15); }

.modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.35); backdrop-filter: blur(6px); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.modal-box {
  background: var(--color-card);
  border-radius: 2px;
  padding: 28px 24px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 40px 80px -20px rgba(0, 0, 0, 0.25);
  border: 1px solid var(--color-border);
  animation: tbModalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes tbModalPop {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.modal-title { font-size: 17px; font-weight: 600; margin-bottom: 20px; color: var(--color-text-primary); letter-spacing: 1px; }
.modal-field { margin-bottom: 14px; }
.modal-field label { display: block; font-size: 13px; color: var(--color-text-secondary); margin-bottom: 6px; font-weight: 500; }
.modal-field input, .modal-field select { width: 100%; padding: 9px 12px; border: 1px solid var(--color-border); border-radius: 2px; font-size: 14px; outline: none; background: var(--color-bg); color: var(--color-text-primary); box-sizing: border-box; }
.modal-field input:focus, .modal-field select:focus { border-color: var(--color-primary); box-shadow: 0 0 0 2px rgba(45, 74, 62, 0.1); }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-cancel {
  padding: 8px 20px;
  border: 1px solid var(--color-border);
  border-radius: 2px;
  background: var(--color-bg);
  cursor: pointer;
  font-size: 13px;
  color: var(--color-text-secondary);
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.btn-cancel:hover {
  border-color: var(--color-text-secondary);
  transform: translateY(-1px);
}
.btn-ok {
  padding: 8px 20px;
  border: none;
  border-radius: 2px;
  background: var(--color-primary);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 6px 14px rgba(45, 74, 62, 0.15);
}
.btn-ok:hover {
  background: var(--color-primary-dark);
  transform: scale(1.03) translateY(-2px);
  box-shadow: 0 12px 24px rgba(45, 74, 62, 0.2);
}
.btn-ok:active {
  transform: scale(0.97);
}

/* 操作按钮通用样式 */
.act-btn {
  padding: 9px 22px;
  border-radius: 6px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
  letter-spacing: 0.5px;
  color: #fff;
}
.act-btn:disabled { cursor: not-allowed; opacity: 0.45; }
.act-copy {
  background: #c4a35a;
  box-shadow: 0 2px 8px rgba(196, 163, 90, 0.3);
}
.act-copy:hover:not(:disabled) {
  background: #d4b76a;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(196, 163, 90, 0.4);
}
.act-swap {
  background: #2d4a3e;
  box-shadow: 0 2px 8px rgba(45, 74, 62, 0.3);
}
.act-swap:hover:not(:disabled) {
  background: #3d6a56;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(45, 74, 62, 0.4);
}
.act-delete {
  background: #dc2626;
  box-shadow: 0 2px 8px rgba(220, 38, 38, 0.3);
}
.act-delete:hover:not(:disabled) {
  background: #ef4444;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(220, 38, 38, 0.4);
}
.act-cancel {
  background: #888;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}
.act-cancel:hover {
  background: #666;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
}
.act-print {
  background: #8B5CF6;
  box-shadow: 0 2px 8px rgba(139, 92, 246, 0.3);
}
.act-print:hover:not(:disabled) {
  background: #7C3AED;
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(124, 58, 237, 0.4);
}

/* 浮动操作工具栏 */
.floating-action-bar {
  position: fixed;
  z-index: 9999;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 12px 20px;
  background: #fff;
  border: 2px solid #c4a35a;
  border-radius: 8px;
  box-shadow: 0 4px 20px rgba(0,0,0,0.2);
  min-width: 400px;
  margin-bottom: 0;
}
.fab-summary {
  font-size: 13px;
  font-weight: 600;
  color: #2d4a3e;
  letter-spacing: 0.3px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-shrink: 1;
  min-width: 0;
}
.fab-btns {
  display: flex;
  gap: 8px;
  flex-shrink: 0;
}
.fab-btns .act-btn {
  padding: 8px 16px;
  font-size: 13px;
  letter-spacing: 0.3px;
}

.toolbar-slide-enter-active,
.toolbar-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}
.toolbar-slide-enter-from,
.toolbar-slide-leave-to {
  opacity: 0;
  transform: translateY(100%);
}

/* 右键菜单 */
.right-menu {
  position: fixed;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(20px);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  box-shadow: 0 20px 50px -12px rgba(0, 0, 0, 0.18), 0 4px 12px rgba(0, 0, 0, 0.06);
  padding: 8px;
  min-width: 220px;
  z-index: 10001;
  animation: ctxMenuFade 0.15s ease-out;
}
@keyframes ctxMenuFade {
  from { opacity: 0; transform: scale(0.96) translateY(-4px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}
.menu-item {
  padding: 10px 16px;
  border-radius: 2px;
  cursor: pointer;
  transition: background 0.15s;
  font-size: 13px;
  color: var(--color-text-primary);
}
.menu-item:hover {
  background: var(--color-primary-light);
}
.menu-item.danger {
  color: #dc2626;
}
.menu-item.danger:hover {
  background: #fef2f2;
}
.menu-divider {
  height: 1px;
  background: var(--color-border);
  margin: 6px 0;
}

/* 全天模式时段选择弹窗 */
.period-modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: periodOverlayFade 0.3s ease-out;
}
@keyframes periodOverlayFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
.period-modal-box {
  background: var(--color-card);
  border-radius: 16px;
  padding: 0;
  width: 420px;
  max-width: 85vw;
  box-shadow: 0 40px 80px -20px rgba(0, 0, 0, 0.3), 0 0 0 1px var(--color-border);
  animation: periodModalPop 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
}
@keyframes periodModalPop {
  from {
    opacity: 0;
    transform: scale(0.85) translateY(30px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.period-modal-header {
  padding: 24px 28px 16px;
  text-align: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
}
.period-modal-title {
  font-size: 19px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 2px;
}
.period-modal-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
  margin-top: 4px;
  letter-spacing: 1px;
}
.period-modal-body {
  display: flex;
  gap: 16px;
  padding: 24px 28px;
}
.period-card {
  flex: 1;
  padding: 20px 16px;
  border-radius: 12px;
  background: var(--color-bg);
  border: 2px solid var(--color-border);
  cursor: pointer;
  text-align: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.period-card:hover {
  border-color: var(--color-primary);
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(45, 74, 62, 0.1);
}
.period-card:active {
  transform: translateY(-2px);
}
.period-icon {
  font-size: 36px;
  margin-bottom: 12px;
}
.period-icon.lunch-icon {
  color: #f59e0b;
}
.period-icon.dinner-icon {
  color: #3b82f6;
}
.period-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}
.period-time {
  font-size: 12px;
  color: var(--color-text-secondary);
}
</style>

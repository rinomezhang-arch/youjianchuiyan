<template>
  <div class="tboard">
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
          <div class="table-name">{{ t.table_name || t.table_number }}</div>
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
    </div>


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
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { getTableBoard, getTableStatus, deleteTable, reorderTables, createTable, cancelBooking, copyBooking, swapBooking, createBooking } from '@/api/booking'
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
// 宽松比较工具：避免 table_id 类型(number/string)不一致导致 includes/indexOf 误判
// 构建标记：__YJC_V2_FIX_20260823_A8C3F1__  确保包含_inSel/_idxOfSel/selClear修复
function _inSel(id) {
  if (id == null) return false
  const sid = String(id)
  return selIds.value.some(x => String(x) === sid)
}
function _idxOfSel(id) {
  if (id == null) return -1
  const sid = String(id)
  return selIds.value.findIndex(x => String(x) === sid)
}
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

// 统一字段名（兼容Jackson JPA camelCase及Board snake_case）
function _v(t, k1, k2, dflt) {
  const v = t[k1] != null ? t[k1] : (t[k2] != null ? t[k2] : dflt)
  return v == null ? dflt : v
}
// 将board接口/JPA实体统一转换为TableBoard组件用的标准格式
function normalizeBoardRow(t) {
  return {
    table_id: _v(t, 'table_id', 'tableId'),
    store_id: _v(t, 'store_id', 'storeId'),
    table_number: _v(t, 'table_number', 'tableNumber'),
    table_name: _v(t, 'table_name', 'tableName'),
    table_area: _v(t, 'table_area', 'tableArea'),
    table_capacity: _v(t, 'table_capacity', 'tableCapacity'),
    table_type: _v(t, 'table_type', 'tableType'),
    table_status: t.booking_id || t.bookingId ? 'occupied' : 'available',
    sort_order: _v(t, 'sort_order', 'sortOrder', 0),
    is_active: _v(t, 'is_active', 'isActive', 1),
    booking: (t.booking_id || t.bookingId) ? {
      booking_id: _v(t, 'booking_id', 'bookingId'),
      booking_date: _v(t, 'booking_date', 'bookingDate'),
      booking_time: _v(t, 'booking_time', 'bookingTime'),
      customer_name: _v(t, 'customer_name', 'customerName'),
      customer_phone: _v(t, 'customer_phone', 'customerPhone'),
      booking_status: _v(t, 'booking_status', 'bookingStatus'),
      banquet_name: _v(t, 'banquet_name', 'banquetName'),
      occasion_type: _v(t, 'occasion_type', 'occasionType'),
      guest_count: _v(t, 'bm_guest_count', 'bmGuestCount', _v(t, 'guest_count', 'guestCount')),
      dishes_count: _v(t, 'dishes_count', 'dishesCount', 0),
      visit_count: _v(t, 'visit_count', 'visitCount', 0),
      booking_id2: _v(t, 'booking_id2', 'bookingId2'),
      customer_name2: _v(t, 'customer_name2', 'customerName2'),
      dishes_count2: _v(t, 'dishes_count2', 'dishesCount2', 0),
      booking_time2: _v(t, 'booking_time2', 'bookingTime2'),
      guest_count2: _v(t, 'guest_count2', 'guestCount2', _v(t, 'bt_guest_count2', 'btGuestCount2'))
    } : null
  }
}
function normalizeBoardList(raw) {
  return (raw || []).map(normalizeBoardRow).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
}

async function loadTablesForValidation(date, tt) {
  // 改用board接口：支持date+period过滤、返回真实预订冲突数据、并与loadData保持字段一致
  // period映射：lunch→morning, dinner→afternoon，其余→all（与boardPeriod保持一致）
  const period = tt === 'lunch' ? 'morning' : (tt === 'dinner' ? 'afternoon' : 'all')
  const res = await getTableBoard({ storeId: 1, date, period })
  if (res.code === 200) {
    return normalizeBoardList(res.data)
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
function onMenuConfirmed({ dishes, totalPrice, tableId }) {
  console.log('菜单已选择:', { dishes, totalPrice, tableId })
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
const selectedTables = computed(() => list.value.filter(t => _inSel(t.table_id)))
const bookedList = computed(() => selectedTables.value.filter(t => t.booking && t.booking.booking_status !== 'cancelled'))
const emptyList = computed(() => selectedTables.value.filter(t => !t.booking || t.booking.booking_status === 'cancelled'))
const hasBooked = computed(() => bookedList.value.length > 0)
const hasEmpty = computed(() => emptyList.value.length > 0)
const canCopy = computed(() => bookedList.value.length === 1 && emptyList.value.length >= 1)
const canSwap = computed(() => selIds.value.length === 2 && hasBooked.value)
const canDelete = computed(() => hasBooked.value)

// 判断选中预订的类型



const showActionToolbar = computed(() => {
  const result = !edit.value && selIds.value.length >= 1 && timeType.value !== 'all'
  console.log('showActionToolbar:', result, 'edit:', edit.value, 'selIds.length:', selIds.value.length, 'timeType:', timeType.value)
  return result
})

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
  if (_inSel(t.table_id)) c.push('selected')
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
    if (String(t.table_id) === String(swapMode.value)) return
    performSwap(swapMode.value, t.table_id)
    return
  }

  const isMulti = e.ctrlKey || e.metaKey
  const i = _idxOfSel(t.table_id)

  // 全天模式：单击只选中，不弹时段选择弹窗；双击 openBooking 里再处理时段
  // 普通时段：与全天模式一致，单击=选中切换，Ctrl=多选
  if (isMulti) {
    if (i >= 0) selIds.value.splice(i, 1)
    else selIds.value.push(t.table_id)
  } else {
    if (i >= 0) {
      // 已选中 → 取消（单选语义）
      selIds.value.splice(i, 1)
    } else {
      selIds.value = [t.table_id]
    }
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
  if (!_inSel(t.table_id)) {
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
  console.log('quickDeleteBooking called', {
    hasBooked: hasBooked.value,
    bookedList: bookedList.value,
    selIds: selIds.value
  })
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
      list.value = normalizeBoardList(res.data)
    }
  } catch (e) { console.error('load', e) }
}

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
  padding: 24px;
  min-height: 100%;
  background: linear-gradient(145deg, #e9f2f9 0%, #d6e4f0 100%);
}

.date-nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 14px 20px;
  background: rgba(255, 255, 255, 0.65);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 24px;
  margin-bottom: 16px;
  flex-wrap: wrap;
  box-shadow: 0 20px 40px -12px rgba(0, 0, 0, 0.08), 0 4px 18px rgba(0, 0, 0, 0.02);
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
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 40px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  color: #475569;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}
.area-btn:hover { border-color: rgba(148, 163, 184, 0.5); transform: translateY(-1px); }
.area-btn.active { background: var(--color-primary); color: #fff; border-color: var(--color-primary); }

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
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  padding: 16px 10px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.02);
  position: relative;
}
.table-item:hover {
  transform: translateY(-4px) scale(1.02);
  border-color: rgba(148, 163, 184, 0.5);
  box-shadow: 0 20px 30px -12px rgba(0, 0, 0, 0.12);
}

.table-item.selected {
  border: 3px solid #7c3aed;
  box-shadow: 0 0 0 6px rgba(124, 58, 237, 0.25), 0 12px 32px -8px rgba(124, 58, 237, 0.3);
  transform: translateY(-2px);
  background: rgba(124, 58, 237, 0.05) !important;
}

/* 互换发起方脉冲动画 */
.table-item.swap-initiator {
  border: 3px solid #f59e0b !important;
  box-shadow: 0 0 0 6px rgba(245, 158, 11, 0.35), 0 0 20px rgba(245, 158, 11, 0.25) !important;
  animation: swapPulse 1.2s ease-in-out infinite;
  transform: translateY(-3px);
}
@keyframes swapPulse {
  0%, 100% { box-shadow: 0 0 0 6px rgba(245, 158, 11, 0.35), 0 0 20px rgba(245, 158, 11, 0.25); }
  50% { box-shadow: 0 0 0 10px rgba(245, 158, 11, 0.2), 0 0 30px rgba(245, 158, 11, 0.4); }
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

.table-item.status-free {
  border: 1px solid rgba(226, 232, 240, 0.9);
  background: #ffffff !important;
}
.table-item.status-booked {
  background: #fef9c3 !important;
  border: 1px solid #eab308 !important;
}

.table-item.editing {
  cursor: grab;
  border: 2px solid rgba(129, 199, 132, 0.5);
  background: linear-gradient(135deg, rgba(200, 230, 201, 0.25) 0%, rgba(255, 255, 255, 0.7) 100%);
  box-shadow: 0 8px 24px rgba(129, 199, 132, 0.15), 0 2px 8px rgba(0, 0, 0, 0.04);
  transform: scale(0.98);
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
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 24px;
  padding: 28px 24px;
  width: 420px;
  max-width: 90vw;
  box-shadow: 0 40px 80px -20px rgba(0, 0, 0, 0.25);
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
.modal-field input, .modal-field select { width: 100%; padding: 12px 16px; border: 1px solid rgba(226, 232, 240, 0.6); border-radius: 16px; font-size: 14px; outline: none; background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(4px); color: var(--color-text-primary); box-sizing: border-box; transition: 0.25s ease; }
.modal-field input:focus, .modal-field select:focus { border-color: var(--color-primary); box-shadow: 0 0 0 2px rgba(45, 74, 62, 0.1); }
.modal-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 20px; }
.btn-cancel {
  padding: 10px 20px;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 40px;
  background: rgba(255, 255, 255, 0.6);
  backdrop-filter: blur(8px);
  cursor: pointer;
  font-size: 13px;
  font-weight: 500;
  color: #334155;
  transition: all 0.2s ease;
}
.btn-cancel:hover {
  border-color: var(--color-text-secondary);
  transform: translateY(-1px);
}
.btn-ok {
  padding: 10px 24px;
  border: none;
  border-radius: 40px;
  background: var(--color-primary);
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  box-shadow: 0 6px 14px rgba(0, 0, 0, 0.08);
}
.btn-ok:hover {
  background: var(--color-primary-dark);
  transform: scale(1.03) translateY(-2px);
  box-shadow: 0 12px 24px rgba(45, 74, 62, 0.2);
}
.btn-ok:active {
  transform: scale(0.97);
}

/* 操作按钮 — 复刻单页配色 */
.act-btn {
  padding: 8px 16px;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}
.act-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.act-copy {
  background: #7c3aed;
  color: #fff;
  border-color: #7c3aed;
}
.act-copy:hover:not(:disabled) { background: #6d28d9; }
.act-swap {
  background: #f59e0b;
  color: #fff;
  border-color: #f59e0b;
}
.act-swap:hover:not(:disabled) { background: #d97706; }
.act-delete {
  background: #fff;
  color: #dc2626;
  border-color: #fecaca;
}
.act-delete:hover:not(:disabled) { background: #fef2f2; }
.act-cancel {
  background: #fff;
  color: #64748b;
  border-color: #e2e8f0;
}
.act-cancel:hover { background: #f8fafc; }
.act-print {
  background: #8B5CF6;
  color: #fff;
  border-color: #8B5CF6;
}
.act-print:hover:not(:disabled) { background: #7C3AED; }

/* 浮动操作工具栏 — 复刻单页毛玻璃风格 */
.floating-action-bar {
  position: sticky;
  bottom: 16px;
  z-index: 100;
  display: flex;
  flex-direction: row;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid #e2e8f0;
  border-radius: 20px;
  box-shadow: 0 -4px 24px rgba(0, 0, 0, 0.08);
  flex-wrap: wrap;
  margin-bottom: 16px;
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
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 16px;
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
  border-radius: 12px;
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

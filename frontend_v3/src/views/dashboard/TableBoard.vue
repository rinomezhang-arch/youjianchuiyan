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
          role="button"
          tabindex="0"
          :aria-label="t.table_number + (t.booking ? ' 已预订' : ' 空闲')"
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
      <div class="menu-item" @click="ctxEditBooking">📝 编辑预订 · Edit</div>
      <div class="menu-item danger" @click="ctxDeleteBooking">🗑️ 删除预订 · Delete</div>
      <div class="menu-divider"></div>
      <div class="menu-item" @click="ctxCustomerLookup">👤 客户查询 · Customer</div>
      <div class="menu-item" @click="goToIpadMenu">🍽️ 去点菜 · Go to Menu</div>
      <div class="menu-divider"></div>
      <div class="menu-item" @click="selClear">✕ 取消选择 · Cancel</div>
    </div>

    <!-- 预订弹窗 -->
    <BookingDialog ref="bkDialogRef" v-model="bkVis" :tableId="selTable?.table_id" :tableNumber="selTable?.table_number" :tableName="selTable?.table_name" :date="fmtDate(curDate)" :booking="initialBooking" :tableIds="selIds" :tableNames="selectedTables.map(t => t.table_number)" :tableAreas="selectedTables.map(t => t.table_area)" :tableCapacities="selectedTables.map(t => t.table_capacity)" @saved="onSaved" @date-change="onDialogDateChange" @period-change="onDialogPeriodChange" @print="onPrint" />

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
          <!-- 午餐卡片 -->
          <div class="period-card period-lunch" :class="{ 'is-booked': lunchBooking }" @click="selectPeriodAndOpen('lunch')">
            <div class="pc-top">
              <div class="pc-icon">☀</div>
              <div class="pc-name">午餐</div>
              <div class="pc-time">11:00 - 14:30</div>
            </div>
            <div class="pc-divider"></div>
            <div class="pc-content">
              <template v-if="lunchBooking">
                <div class="pc-customer">{{ lunchBooking.customer_name }}</div>
                <div class="pc-phone" v-if="lunchBooking.customer_phone">{{ lunchBooking.customer_phone }}</div>
                <div class="pc-banquet" v-if="lunchBooking.banquet_name || lunchBooking.occasion_type">
                  {{ lunchBooking.banquet_name || lunchBooking.occasion_type }}
                </div>
                <div class="pc-count" v-if="lunchBooking.guest_count">{{ lunchBooking.guest_count }} 人</div>
              </template>
              <template v-else>
                <div class="pc-empty-icon">📋</div>
                <div class="pc-empty-text">暂无预定</div>
              </template>
            </div>
            <div class="pc-bottom">
              <div class="pc-btn" :class="lunchBooking ? 'btn-view' : 'btn-book'">
                {{ lunchBooking ? '查看更改' : '立即预定' }}
              </div>
            </div>
          </div>

          <!-- 晚餐卡片 -->
          <div class="period-card period-dinner" :class="{ 'is-booked': dinnerBooking }" @click="selectPeriodAndOpen('dinner')">
            <div class="pc-top">
              <div class="pc-icon">🌙</div>
              <div class="pc-name">晚餐</div>
              <div class="pc-time">17:00 - 21:30</div>
            </div>
            <div class="pc-divider"></div>
            <div class="pc-content">
              <template v-if="dinnerBooking">
                <div class="pc-customer">{{ dinnerBooking.customer_name }}</div>
                <div class="pc-phone" v-if="dinnerBooking.customer_phone">{{ dinnerBooking.customer_phone }}</div>
                <div class="pc-banquet" v-if="dinnerBooking.banquet_name || dinnerBooking.occasion_type">
                  {{ dinnerBooking.banquet_name || dinnerBooking.occasion_type }}
                </div>
                <div class="pc-count" v-if="dinnerBooking.guest_count">{{ dinnerBooking.guest_count }} 人</div>
              </template>
              <template v-else>
                <div class="pc-empty-icon">📋</div>
                <div class="pc-empty-text">暂无预定</div>
              </template>
            </div>
            <div class="pc-bottom">
              <div class="pc-btn" :class="dinnerBooking ? 'btn-view' : 'btn-book'">
                {{ dinnerBooking ? '查看更改' : '立即预定' }}
              </div>
            </div>
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

// 全天模式时段选择弹窗
const showPeriodModal = ref(false)

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

// 本地降级存储：用于后端API不可用时的前端数据持久化
const STORAGE_KEY = 'tboard_local_bookings'
const localBookings = ref({})

function loadLocalBookings() {
  try {
    const saved = localStorage.getItem(STORAGE_KEY)
    if (saved) {
      const raw = JSON.parse(saved)
      let migrated = false
      const now = new Date()
      const defaultCreatedAt = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
      
      for (const key of Object.keys(raw)) {
        const b = raw[key]
        // 数据迁移：补充缺失的必要字段
        if (!b.created_at) {
          b.created_at = b._updatedAt || defaultCreatedAt
          migrated = true
        }
        if (!b.booking_id && !b.order_no) {
          b.booking_id = 'BK' + Date.now() + '_' + Math.random().toString(36).substr(2, 6)
          migrated = true
        }
        if (!b.staff_name && !b.created_by) {
          b.staff_name = '系统管理员'
          migrated = true
        }
        if (!b.booking_status) {
          b.booking_status = 'confirmed'
          migrated = true
        }
        raw[key] = b
      }
      
      if (migrated) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(raw))
        console.info('本地预订数据已迁移，补充缺失字段')
      }
      localBookings.value = raw
    }
  } catch (e) {
    console.warn('加载本地预订数据失败:', e)
    localBookings.value = {}
  }
}

function saveLocalBookings() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(localBookings.value))
  } catch (e) {
    console.warn('保存本地预订数据失败:', e)
  }
}

function getBookingKey(tableId, date, period) {
  return `${tableId}_${date}_${period}`
}

function getPeriodFromTime(time) {
  if (!time) return 'afternoon'
  const hour = parseInt(String(time).split(':')[0] || '18')
  return hour < 15 ? 'morning' : 'afternoon'
}

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

async function loadTablesForValidation(date, timeType) {
  const res = await getTableStatus({ date, timeType })
  if (res.code === 200) {
    return (res.data || []).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
  }
  throw new Error('加载桌台数据失败')
}

function onSaved(bookingData) {
  const date = bookingData?.booking_date || bookingData?.bookingDate
  // 重新加载本地存储的预订数据（前端降级场景）
  loadLocalBookings()
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

// 跳转到iPad点菜页面
function goToIpadMenu() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  ctxMenuVisible.value = false
  router.push({ 
    path: '/dashboard/ipad', 
    query: { tableId: t.table_id, tableNumber: t.table_number } 
  })
}

const areaList = computed(() => ['全部', ...ZONES.filter(a => list.value.some(t => t.table_area === a))])

// 判断是否为包房/宴会厅区域
function isBanquetArea(areaName) {
  if (!areaName || areaName === '全部') return false
  const banquetKeywords = ['包厢', '包房', '宴会厅', '扶摇厅', '厅']
  return banquetKeywords.some(kw => areaName.includes(kw))
}

// 跳转到预订管理界面
function goToBookings() {
  router.push('/dashboard/bookings')
}

// 全天模式选择时段（仅切换，不打开弹窗）
function selectPeriod(period) {
  showPeriodModal.value = false
  timeType.value = period
  loadData()
}

// 全天模式：选择时段并直接打开预订弹窗
function selectPeriodAndOpen(period) {
  showPeriodModal.value = false
  timeType.value = period
  const t = selTable.value
  if (!t) return

  // 构造该时段的预订数据
  let bookingData = null
  if (t.booking) {
    const hasBoth = !!t.booking.booking_id2
    const isLunch = isLunchTime(t.booking.booking_time)

    if (hasBoth) {
      // 两个时段都有预订
      if (period === 'lunch') {
        bookingData = {
          ...t.booking,
          booking_id: t.booking.booking_id,
          customer_name: t.booking.customer_name,
          booking_time: t.booking.booking_time,
          guest_count: t.booking.guest_count,
          dishes_count: t.booking.dishes_count
        }
      } else {
        bookingData = {
          ...t.booking,
          booking_id: t.booking.booking_id2,
          customer_name: t.booking.customer_name2,
          booking_time: t.booking.booking_time2,
          guest_count: t.booking.guest_count2,
          dishes_count: t.booking.dishes_count2
        }
      }
    } else {
      // 只有一个时段有预订
      const bookingPeriod = isLunch ? 'lunch' : 'dinner'
      if (bookingPeriod === period) {
        bookingData = t.booking
      }
    }
  }

  selIds.value = [t.table_id]
  initialBooking.value = bookingData
  bkVis.value = true
}

// 计算当前选中桌台的午餐/晚餐预订信息
const lunchBooking = computed(() => {
  const t = selTable.value
  if (!t || !t.booking) return null
  const hasBoth = !!t.booking.booking_id2
  if (hasBoth) {
    return {
      customer_name: t.booking.customer_name,
      customer_phone: t.booking.customer_phone,
      guest_count: t.booking.guest_count,
      booking_time: t.booking.booking_time,
      banquet_name: t.booking.banquet_name,
      occasion_type: t.booking.occasion_type
    }
  }
  if (isLunchTime(t.booking.booking_time)) {
    return {
      customer_name: t.booking.customer_name,
      customer_phone: t.booking.customer_phone,
      guest_count: t.booking.guest_count,
      booking_time: t.booking.booking_time,
      banquet_name: t.booking.banquet_name,
      occasion_type: t.booking.occasion_type
    }
  }
  return null
})

const dinnerBooking = computed(() => {
  const t = selTable.value
  if (!t || !t.booking) return null
  const hasBoth = !!t.booking.booking_id2
  if (hasBoth) {
    return {
      customer_name: t.booking.customer_name2,
      customer_phone: t.booking.customer_phone2,
      guest_count: t.booking.guest_count2,
      booking_time: t.booking.booking_time2,
      banquet_name: t.booking.banquet_name2,
      occasion_type: t.booking.occasion_type2
    }
  }
  if (!isLunchTime(t.booking.booking_time)) {
    return {
      customer_name: t.booking.customer_name,
      customer_phone: t.booking.customer_phone,
      guest_count: t.booking.guest_count,
      booking_time: t.booking.booking_time,
      banquet_name: t.booking.banquet_name,
      occasion_type: t.booking.occasion_type
    }
  }
  return null
})

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




const showActionToolbar = computed(() => {
  // 至少需要选中2个桌台才显示浮动操作条；弹窗打开或编辑模式时隐藏
  return !edit.value && !bkVis.value && selIds.value.length >= 2
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
  const isSelected = selIds.value.includes(t.table_id)
  if (isSelected) c.push('selected')
  // 首选台号（第一个选中）显示红色高亮
  if (isSelected && String(selIds.value[0]) === String(t.table_id)) {
    c.push('primary-selected')
  }
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
  // Ctrl/Cmd + 点击 = 多选（不跳转）
  if (e.ctrlKey || e.metaKey) {
    const idx = selIds.value.indexOf(t.table_id)
    if (idx >= 0) {
      selIds.value.splice(idx, 1)
    } else {
      selIds.value.push(t.table_id)
    }
    return
  }
  // 普通单击：只选中桌台（不跳转）
  const idx = selIds.value.indexOf(t.table_id)
  if (idx >= 0) {
    selIds.value.splice(idx, 1)
  } else {
    selIds.value = [t.table_id]
    selTable.value = t
  }
}

async function openBooking(t) {
  if (edit.value) return
  showPeriodModal.value = false
  // 所有模式下双击都弹时段选择窗体
  // 顶部"全天/午餐/晚餐"仅作预览切换，不影响操作流程
  selTable.value = t
  selIds.value = [t.table_id]
  if (timeType.value !== 'all') {
    // 非全天模式下，先切换到全天模式并加载数据，确保两个时段数据都完整
    timeType.value = 'all'
    await loadData()
  }
  showPeriodModal.value = true
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
  // 确保 selIds 包含当前桌台
  if (!selIds.value.includes(t.table_id)) {
    selIds.value = [...selIds.value, t.table_id]
  }
  selTable.value = t
  initialBooking.value = t?.booking || null
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
  // 确保 selIds 包含当前桌台
  if (!selIds.value.includes(t.table_id)) {
    selIds.value = [...selIds.value, t.table_id]
  }
  selTable.value = t
  initialBooking.value = t?.booking || null
  bkVis.value = true
  ctxMenuVisible.value = false
}

// 右键菜单 - 删除预订：前端降级实现
function ctxDeleteBooking() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  if (!t.booking) {
    ElMessage.warning('该桌台没有预订')
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
      const date = fmtDate(curDate.value)
      const currentPeriod = boardPeriod.value
      if (t.booking?.booking_id) {
        const period = currentPeriod === 'all' ? getPeriodFromTime(t.booking.booking_time) : currentPeriod
        const key = getBookingKey(t.table_id, date, period)
        if (localBookings.value[key]) {
          delete localBookings.value[key]
          saveLocalBookings()
        }
        // 全天模式下的双预订
        if (t.booking.booking_id2) {
          const key2 = getBookingKey(t.table_id, date, getPeriodFromTime(t.booking.booking_time2))
          if (localBookings.value[key2]) {
            delete localBookings.value[key2]
            saveLocalBookings()
          }
        }
      }
      ElMessage.success('已删除 · Deleted')
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

// 复制预订：前端降级实现（后端API不可用时使用）
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
    const date = fmtDate(curDate.value)
    const period = boardPeriod.value === 'all' ? getPeriodFromTime(bk.booking_time) : boardPeriod.value
    
    // 保存源预订到本地存储（保留原始）
    const sourceBookingKey = getBookingKey(source.table_id, date, period)
    localBookings.value[sourceBookingKey] = {
      ...bk,
      table_id: source.table_id,
      table_number: source.table_number,
      table_area: source.table_area || bk.table_area || '',
      table_capacity: source.table_capacity || bk.table_capacity || null,
      date,
      period,
      _local: true
    }
    
    // 将预订复制到目标桌台
    const newBookings = []
    for (const target of targets) {
      const targetKey = getBookingKey(target.table_id, date, period)
      const newBookingId = 'LOCAL_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
      localBookings.value[targetKey] = {
        ...bk,
        booking_id: newBookingId,
        table_id: target.table_id,
        table_number: target.table_number,
        table_area: target.table_area || '',
        table_capacity: target.table_capacity || null,
        booking_time: bk.booking_time || '18:00:00',
        date,
        period,
        _local: true,
        _sourceCopy: source.table_number
      }
      newBookings.push(target.table_number)
    }
    
    saveLocalBookings()
    ElMessage.success(`已复制到 ${newBookings.length} 个桌台：${newBookings.join('、')}`)
    selClear()
    loadData()
  } catch (e) {
    console.warn('复制失败:', e)
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

// 执行调换：前端降级实现（后端API不可用时使用）
async function performSwap(srcId, dstId) {
  const src = list.value.find(t => String(t.table_id) === String(srcId))
  const dst = list.value.find(t => String(t.table_id) === String(dstId))
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
    const date = fmtDate(curDate.value)
    const currentPeriod = boardPeriod.value
    const srcPeriod = currentPeriod === 'all' ? getPeriodFromTime(src.booking?.booking_time) : currentPeriod
    const dstPeriod = currentPeriod === 'all' ? getPeriodFromTime(dst.booking?.booking_time) : currentPeriod
    
    // 获取两个桌台的当前预订数据
    const srcKey = getBookingKey(srcId, date, srcPeriod)
    const dstKey = getBookingKey(dstId, date, dstPeriod)
    
    // 合并预订数据时保留桌台元信息
    const srcData = src.booking ? {
      ...src.booking,
      table_id: srcId,
      table_number: src.table_number,
      table_area: src.table_area || src.booking.table_area || '',
      table_capacity: src.table_capacity || src.booking.table_capacity || null,
      _local: true
    } : null
    const dstData = dst.booking ? {
      ...dst.booking,
      table_id: dstId,
      table_number: dst.table_number,
      table_area: dst.table_area || dst.booking.table_area || '',
      table_capacity: dst.table_capacity || dst.booking.table_capacity || null,
      _local: true
    } : null
    
    if (srcBooked && dstBooked) {
      // 两个都有预订：互换
      localBookings.value[srcKey] = { ...dstData, table_id: srcId, table_number: src.table_number, table_area: src.table_area || dstData.table_area, table_capacity: src.table_capacity || dstData.table_capacity }
      localBookings.value[dstKey] = { ...srcData, table_id: dstId, table_number: dst.table_number, table_area: dst.table_area || srcData.table_area, table_capacity: dst.table_capacity || srcData.table_capacity }
      // 清理旧键
      if (srcPeriod !== dstPeriod) {
        delete localBookings.value[getBookingKey(srcId, date, dstPeriod)]
        delete localBookings.value[getBookingKey(dstId, date, srcPeriod)]
      }
    } else if (srcBooked && !dstBooked) {
      // 源有预订，目标空闲：转移
      delete localBookings.value[srcKey]
      localBookings.value[dstKey] = { ...srcData, table_id: dstId, table_number: dst.table_number, table_area: dst.table_area || srcData.table_area, table_capacity: dst.table_capacity || srcData.table_capacity }
    } else if (!srcBooked && dstBooked) {
      // 源空闲，目标有预订：转移
      delete localBookings.value[dstKey]
      localBookings.value[srcKey] = { ...dstData, table_id: srcId, table_number: src.table_number, table_area: src.table_area || dstData.table_area, table_capacity: src.table_capacity || dstData.table_capacity }
    }
    
    saveLocalBookings()
    ElMessage.success('调换成功 · Swap completed')
    swapMode.value = null
    selClear()
    loadData()
  } catch (e) {
    console.warn('调换失败，尝试前端降级:', e)
    ElMessage.error('调换失败：' + (e?.message || '未知错误'))
    swapMode.value = null
  }
}

// 快速删除预订：前端降级实现（后端API不可用时使用）
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
    const date = fmtDate(curDate.value)
    const currentPeriod = boardPeriod.value
    // 从本地存储中删除预订
    for (const t of bookedList.value) {
      if (t.booking?.booking_id) {
        const period = currentPeriod === 'all' ? getPeriodFromTime(t.booking.booking_time) : currentPeriod
        const key = getBookingKey(t.table_id, date, period)
        // 删除本地存储的预订
        if (localBookings.value[key]) {
          delete localBookings.value[key]
        }
        // 全天模式下检查双预订
        if (t.booking.booking_id2) {
          const key2 = getBookingKey(t.table_id, date, getPeriodFromTime(t.booking.booking_time2))
          if (localBookings.value[key2]) {
            delete localBookings.value[key2]
          }
        }
      }
    }
    saveLocalBookings()
    ElMessage.success(`已删除 ${bookedList.value.length} 个预订 · Deleted`)
    selClear()
    loadData()
  } catch (e) {
    console.warn('删除失败:', e)
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

// P2-2 打印预订通知单：修复字段对齐，支持全天模式
function printBooking() {
  let items = bookedList.value
  // 全天模式下bookedList可能为空，回退到所有选中的有预订的桌台
  if (!items.length && selIds.value.length > 0) {
    items = selectedTables.value.filter(t => t.booking)
  }
  // 右键菜单打印回退
  if (!items.length && ctxMenuTable.value && ctxMenuTable.value.booking) {
    items = [ctxMenuTable.value]
  }
  if (!items.length) {
    ElMessage.warning('没有可打印的预订')
    return
  }
  
  const win = window.open('', '_blank', 'width=700,height=600')
  if (!win) { ElMessage.warning('请允许弹窗'); return }
  
  const dateStr = fmtDate(curDate.value)
  const periodLabel = timeType.value === 'all' ? '全天' : (timeType.value === 'lunch' ? '午餐' : '晚餐')
  
  let html = '<html><head><meta charset="utf-8"><style>body{font-family:SimSun,serif;padding:20px}' +
    'h2{text-align:center;margin-bottom:5px}h4{text-align:center;color:#666;margin-top:0}' +
    'table{width:100%;border-collapse:collapse;margin-top:20px}' +
    'th,td{border:1px solid #333;padding:8px;font-size:14px;text-align:center}' +
    'th{background:#f5f5f5}.foot{margin-top:30px;text-align:right;font-size:12px;color:#666}' +
    '.section{margin-top:15px;font-weight:bold;color:#333}</style></head><body>' +
    '<h2>又见炊烟 · 订台通知单</h2>' +
    '<h4>日期：' + dateStr + ' | 时段：' + periodLabel + ' | 共 ' + items.length + ' 桌</h4>'
  
  // 按预订时段分组显示
  const lunchItems = []
  const dinnerItems = []
  const otherItems = []
  
  items.forEach(t => {
    const b = t.booking
    if (!b) return
    const bookingTime = b.booking_time || ''
    const hour = parseInt(bookingTime.split(':')[0] || '18')
    if (hour < 15) {
      lunchItems.push(t)
    } else {
      dinnerItems.push(t)
    }
  })
  
  // 输出午餐时段
  if (lunchItems.length > 0) {
    html += '<div class="section">午餐时段 (' + lunchItems.length + '桌)</div>'
    html += '<table><tr><th>台号</th><th>客户</th><th>电话</th><th>人数</th><th>时间</th><th>菜品数</th><th>状态</th></tr>'
    lunchItems.forEach(t => {
      const b = t.booking
      html += '<tr>' +
        '<td>' + (t.table_number || '-') + '</td>' +
        '<td>' + (b.customer_name || '-') + '</td>' +
        '<td>' + (b.customer_phone || '-') + '</td>' +
        '<td>' + (b.guest_count || 0) + '</td>' +
        '<td>' + (b.booking_time || '').slice(0, 5) + '</td>' +
        '<td>' + (b.dishes_count || 0) + '</td>' +
        '<td>' + (b.booking_status === 'cancelled' ? '已取消' : '已确认') + '</td>' +
        '</tr>'
    })
    html += '</table>'
  }
  
  // 输出晚餐时段
  if (dinnerItems.length > 0) {
    html += '<div class="section">晚餐时段 (' + dinnerItems.length + '桌)</div>'
    html += '<table><tr><th>台号</th><th>客户</th><th>电话</th><th>人数</th><th>时间</th><th>菜品数</th><th>状态</th></tr>'
    dinnerItems.forEach(t => {
      const b = t.booking
      html += '<tr>' +
        '<td>' + (t.table_number || '-') + '</td>' +
        '<td>' + (b.customer_name || '-') + '</td>' +
        '<td>' + (b.customer_phone || '-') + '</td>' +
        '<td>' + (b.guest_count || 0) + '</td>' +
        '<td>' + (b.booking_time || '').slice(0, 5) + '</td>' +
        '<td>' + (b.dishes_count || 0) + '</td>' +
        '<td>' + (b.booking_status === 'cancelled' ? '已取消' : '已确认') + '</td>' +
        '</tr>'
    })
    html += '</table>'
  }
  
  html += '<p class="foot">打印时间: ' + new Date().toLocaleString() + '</p></body></html>'
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
    // 先加载本地存储
    loadLocalBookings()
    
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
      
      // 合并本地存储的预订数据（前端降级）
      mergeLocalBookings(dateToLoad)
    } else {
      // API返回非200，使用本地降级
      mergeLocalBookings(dateToLoad)
    }
  } catch (e) { 
    console.error('load', e)
    // API失败时，仍然从本地存储加载数据作为降级方案
    const dateToLoad = customDate || fmtDate(curDate.value)
    loadLocalBookings()
    // 如果列表为空，尝试从本地存储恢复
    if (list.value.length === 0) {
      // 保持空列表，等待用户操作
      console.warn('API加载失败，使用本地降级模式')
    }
    // 始终合并本地预订
    mergeLocalBookings(dateToLoad)
  }
}

// 合并本地存储的预订数据到列表中
function mergeLocalBookings(date) {
  const period = boardPeriod.value
  const allKeys = Object.keys(localBookings.value)

  for (const key of allKeys) {
    const booking = localBookings.value[key]
    if (!booking) continue
    // 字段命名兼容：date / booking_date 均支持
    const bookingDate = booking.date || booking.booking_date || ''
    if (bookingDate !== date) continue

    // 检查是否匹配当前时段
    if (period !== 'all' && booking.period !== period) continue

    const tableIdx = list.value.findIndex(t => String(t.table_id) === String(booking.table_id))
    if (tableIdx >= 0) {
      const table = list.value[tableIdx]
      // 如果桌台当前没有预订，或者本地预订有更高优先级
      if (!table.booking || booking._local) {
        // 合并预订数据，包含桌台区域和容量
        const existing = table.booking || {}
        table.booking = {
          ...existing,
          ...booking,
          booking_id: booking.booking_id,
          order_no: booking.booking_id || booking.order_no,
          created_at: booking.created_at || booking._updatedAt || '',
          staff_name: booking.staff_name || booking.created_by || '',
          customer_name: booking.customer_name,
          customer_phone: booking.customer_phone,
          booking_time: booking.booking_time,
          booking_status: booking.booking_status || 'confirmed',
          guest_count: booking.guest_count || existing.guest_count || 0,
          dishes_count: booking.dishes_count || existing.dishes_count || 0,
          table_area: booking.table_area || table.table_area || '',
          table_capacity: booking.table_capacity || table.table_capacity || null,
          _local: true
        }
        list.value[tableIdx] = { ...table }
      }
    }
  }
}

onMounted(() => {
  // 初始化本地存储
  loadLocalBookings()
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
  background: var(--color-bg);
}

.date-nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  gap: 8px;
  padding: 14px 20px;
  background: var(--color-card);
  border-radius: 2px;
  border: 1px solid var(--color-border);
  margin-bottom: 16px;
  flex-wrap: wrap;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03);
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
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 12px 8px;
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
/* 首选台号：中国红高亮 */
.table-item.primary-selected {
  border: 3px solid #C0392B !important;
  box-shadow: 0 0 0 4px rgba(192, 57, 43, 0.3), 0 4px 16px rgba(192, 57, 43, 0.25) !important;
  background: rgba(192, 57, 43, 0.12) !important;
}
.table-item.primary-selected::before {
  content: '★ 首选';
  position: absolute;
  top: 6px;
  left: 8px;
  background: #C0392B;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  padding: 2px 6px;
  border-radius: 3px;
  z-index: 10;
  letter-spacing: 0.5px;
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
  background: rgba(0, 0, 0, 0.45);
  backdrop-filter: blur(6px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: periodOverlayFade 0.25s ease-out;
}
@keyframes periodOverlayFade {
  from { opacity: 0; }
  to { opacity: 1; }
}
.period-modal-box {
  background: var(--color-card);
  border-radius: 18px;
  padding: 0;
  width: 520px;
  max-width: 90vw;
  box-shadow: 0 30px 60px -15px rgba(0, 0, 0, 0.25), 0 0 0 1px var(--color-border);
  animation: periodModalPop 0.35s cubic-bezier(0.34, 1.56, 0.64, 1);
  overflow: hidden;
}
@keyframes periodModalPop {
  from {
    opacity: 0;
    transform: scale(0.9) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.period-modal-header {
  padding: 22px 32px 18px;
  text-align: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-accent) 100%);
}
.period-modal-title {
  font-size: 20px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 3px;
}
.period-modal-subtitle {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
  margin-top: 6px;
  letter-spacing: 2px;
}
.period-modal-body {
  display: flex;
  gap: 24px;
  padding: 32px 28px 36px;
  justify-content: center;
}

/* 时段卡片：正方形设计 */
.period-card {
  width: 200px;
  height: 300px;
  border-radius: 14px;
  cursor: pointer;
  text-align: center;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 2px solid #d1d5db;
  background: #fafafa;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}
.period-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 16px 32px rgba(0, 0, 0, 0.1);
  border-color: #9ca3af;
}
.period-card:active {
  transform: translateY(-2px);
}

/* 卡片顶部时段区 */
.pc-top {
  padding: 18px 16px 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
  transition: all 0.3s ease;
}
.period-lunch .pc-top {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
}
.period-dinner .pc-top {
  background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
}
.pc-icon {
  font-size: 32px;
  line-height: 1;
  filter: grayscale(100%) opacity(0.4);
  transition: all 0.3s ease;
}
.pc-name {
  font-size: 17px;
  font-weight: 700;
  color: #6b7280;
  letter-spacing: 2px;
  transition: all 0.3s ease;
}
.pc-time {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 500;
  transition: all 0.3s ease;
}

/* 有预订时：顶部高亮 + 图标发光 */
.period-card.is-booked.period-lunch .pc-top {
  background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
}
.period-card.is-booked.period-dinner .pc-top {
  background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
}
.period-card.is-booked .pc-icon {
  filter: none;
  animation: iconGlow 2s ease-in-out infinite alternate;
}
.period-card.is-booked.period-lunch .pc-icon {
  text-shadow: 0 0 20px rgba(251, 191, 36, 0.8), 0 0 40px rgba(251, 191, 36, 0.4);
}
.period-card.is-booked.period-dinner .pc-icon {
  text-shadow: 0 0 20px rgba(96, 165, 250, 0.8), 0 0 40px rgba(96, 165, 250, 0.4);
}
@keyframes iconGlow {
  from {
    filter: drop-shadow(0 0 4px rgba(251, 191, 36, 0.5));
  }
  to {
    filter: drop-shadow(0 0 12px rgba(251, 191, 36, 0.9));
  }
}
.period-card.is-booked.period-dinner .pc-icon {
  animation: iconGlowBlue 2s ease-in-out infinite alternate;
}
@keyframes iconGlowBlue {
  from {
    filter: drop-shadow(0 0 4px rgba(96, 165, 250, 0.5));
  }
  to {
    filter: drop-shadow(0 0 12px rgba(96, 165, 250, 0.9));
  }
}
.period-card.is-booked .pc-name {
  color: #1f2937;
}
.period-card.is-booked .pc-time {
  color: #6b7280;
}

/* 分隔线 */
.pc-divider {
  height: 1px;
  margin: 0 24px;
  background: rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

/* 卡片内容区 */
.pc-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  gap: 6px;
  min-height: 0;
}
.pc-customer {
  font-size: 17px;
  font-weight: 700;
  color: var(--color-primary);
  transition: all 0.3s ease;
}
.period-card.period-lunch.is-booked .pc-customer {
  color: #d97706;
}
.period-card.period-dinner.is-booked .pc-customer {
  color: #2563eb;
}
.pc-phone {
  font-size: 13px;
  color: #6b7280;
  font-family: 'SF Mono', Menlo, monospace;
}
.pc-banquet {
  font-size: 11px;
  color: #92400e;
  background: #fef3c7;
  padding: 3px 10px;
  border-radius: 12px;
  font-weight: 500;
}
.pc-count {
  font-size: 13px;
  color: #4b5563;
  font-weight: 500;
}
.pc-empty-icon {
  font-size: 28px;
  opacity: 0.25;
  margin-bottom: 6px;
}
.pc-empty-text {
  font-size: 14px;
  color: #9ca3af;
  font-weight: 500;
}

/* 卡片底部按钮区 */
.pc-bottom {
  padding: 0 20px 18px;
  flex-shrink: 0;
}
.pc-btn {
  display: block;
  padding: 10px 0;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  transition: all 0.2s;
  letter-spacing: 1px;
}
.period-lunch .pc-btn.btn-view {
  background: #f59e0b;
  color: #fff;
  box-shadow: 0 2px 8px rgba(245, 158, 11, 0.3);
}
.period-dinner .pc-btn.btn-view {
  background: #3b82f6;
  color: #fff;
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.3);
}
.period-lunch .pc-btn.btn-book {
  background: #fff;
  color: #f59e0b;
  border: 1.5px solid #f59e0b;
}
.period-dinner .pc-btn.btn-book {
  background: #fff;
  color: #3b82f6;
  border: 1.5px solid #3b82f6;
}
.period-card:hover .pc-btn {
  transform: scale(1.02);
}

/* 有预定状态的卡片边框 */
.period-card.is-booked.period-lunch {
  border-color: #f59e0b;
  box-shadow: 0 4px 20px rgba(245, 158, 11, 0.15), inset 0 0 0 1px rgba(245, 158, 11, 0.1);
}
.period-card.is-booked.period-dinner {
  border-color: #3b82f6;
  box-shadow: 0 4px 20px rgba(59, 130, 246, 0.15), inset 0 0 0 1px rgba(59, 130, 246, 0.1);
}
</style>
<template>
  <div class="tboard">
    <!-- 日期导航 -->
    <div class="date-nav">
      <input type="date" class="date-input" :value="fmtDate(curDate)" @change="onDateChange" />
      <button class="nav-btn" @click="mvDay(-1)">‹</button>
      <button class="today-btn" @click="setToday">{{ t('common.date') }} · {{ t('common.dateEn') }}</button>
      <button class="nav-btn" @click="mvDay(1)">›</button>
      <span class="nav-sep"></span>
      <button :class="['period-btn', { active: timeType === 'all' }]" @click="timeType = 'all'; loadData()">全天 · All</button>
      <button :class="['period-btn', 'lunch', { active: timeType === 'lunch' }]" @click="timeType = 'lunch'; loadData()">{{ t('booking.lunch') }} · {{ t('booking.lunchEn') }}</button>
      <button :class="['period-btn', 'dinner', { active: timeType === 'dinner' }]" @click="timeType = 'dinner'; loadData()">{{ t('booking.dinner') }} · {{ t('booking.dinnerEn') }}</button>
      <span class="nav-sep"></span>
      <button :class="['edit-btn', { editing: edit }]" @click="toggleEdit">{{ edit ? `${t('common.save')} · ${t('common.saveEn')}` : `${t('tableBoard.actions.edit')} · ${t('tableBoard.actions.editEn')}` }}</button>
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

    <!-- 桌台网格 -->
    <div class="tgrid" id="tableGrid">
      <template v-for="(t, i) in displayList" :key="t.table_id">
        <div
          :class="cardClass(t)"
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
          <div v-if="t.booking" class="tbinfo">
            <div class="table-guest">{{ t.booking.customer_name }}</div>
            <div v-if="t.booking.customer_phone" class="table-phone">{{ t.booking.customer_phone }}</div>
            <div v-if="t.booking.guest_count" class="table-booked-people">预订 {{ t.booking.guest_count }} 人</div>
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

    <!-- 浮动操作工具栏 -->
    <transition name="toolbar-fade">
      <div v-show="showActionToolbar" class="action-toolbar">
        <span class="action-summary">{{ actionSummary }}</span>
        <button class="action-btn action-copy" :disabled="!canCopy" @click="copyBookingToSelected">复制预订 · Copy</button>
        <button class="action-btn action-swap" :disabled="!canSwap" @click="startSwapMode">互换预订 · Swap</button>
        <button class="action-btn action-delete" :disabled="!canDelete" @click="quickDeleteBooking">删除预订 · Delete</button>
        <button class="action-btn action-cancel" @click="selClear">取消 · Cancel</button>
      </div>
    </transition>

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
      <div class="menu-item" @click="selClear">✕ 取消选择 · Cancel</div>
    </div>

    <!-- 预订弹窗 -->
    <BookingDialog ref="bkDialogRef" v-model="bkVis" :tableId="selTable?.table_id" :tableNumber="selTable?.table_number" :tableName="selTable?.table_name" :date="fmtDate(curDate)" :booking="initialBooking" @saved="onSaved" @date-change="onDialogDateChange" @period-change="onDialogPeriodChange" />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTableStatus, deleteTable, reorderTables, createTable, cancelBooking, swapTableBooking, createBooking } from '@/api/booking'
import BookingDialog from '@/components/BookingDialog.vue'

const { t } = useI18n()

const curDate = ref(new Date())
const timeType = ref('all')
const area = ref('全部')
const statusFilter = ref('all')
const list = ref([])
const selIds = ref([])
const edit = ref(false)
const bkVis = ref(false)
const bkDialogRef = ref(null)
const selTable = ref(null)
const initialBooking = ref(null)
const showAddModal = ref(false)
const newTableName = ref('')
const newTablePeople = ref('')
const newTableArea = ref('一楼包厢')

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
function onDateChange(e) {
  curDate.value = new Date(e.target.value + 'T00:00:00')
  loadData()
}
function mvDay(n) {
  curDate.value = new Date(curDate.value.getTime() + n * 864e5)
  loadData()
}
function setToday() {
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
  if (date) {
    const dateStr = typeof date === 'string' && date.includes('T')
      ? date.split('T')[0]
      : date
    curDate.value = new Date(dateStr + 'T00:00:00')
  }
  const timeSlot = bookingData?.time_slot || bookingData?.booking_time || bookingData?.bookingTime
  if (timeSlot) {
    const hour = parseInt(String(timeSlot).split(':')[0] || '18')
    timeType.value = hour < 15 ? 'lunch' : 'dinner'
  }
  loadData()
}

const areaList = computed(() => ['全部', ...ZONES.filter(a => list.value.some(t => t.table_area === a))])

// 浮动工具栏相关计算属性
const selectedTables = computed(() => list.value.filter(t => selIds.value.includes(t.table_id)))
const bookedList = computed(() => selectedTables.value.filter(t => t.booking && t.booking.booking_status !== 'cancelled'))
const emptyList = computed(() => selectedTables.value.filter(t => !t.booking || t.booking.booking_status === 'cancelled'))
const hasBooked = computed(() => bookedList.value.length > 0)
const hasEmpty = computed(() => emptyList.value.length > 0)
const canCopy = computed(() => timeType.value !== 'all' && hasBooked.value && hasEmpty.value)
const canSwap = computed(() => timeType.value !== 'all' && bookedList.value.length === 2)
const canDelete = computed(() => timeType.value !== 'all' && hasBooked.value)
const showActionToolbar = computed(() => !edit.value && selIds.value.length > 0)

const actionSummary = computed(() => {
  if (timeType.value === 'all') {
    return `已选 ${selIds.value.length} 个桌台（全天模式下请切换到午餐或晚餐时段操作预订）`
  }
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
  if (selIds.value.includes(t.table_id)) c.push('selected')
  if (edit.value) c.push('editing')
  if (dragSrcIdx.value !== null && displayList.value[dragSrcIdx.value]?.table_id === t.table_id) c.push('dragging')
  if (dragTargetIdx.value !== null && dragInsertPos.value === 'before' && displayList.value[dragTargetIdx.value]?.table_id === t.table_id) c.push('insert-before')
  if (dragTargetIdx.value !== null && dragInsertPos.value === 'after' && displayList.value[dragTargetIdx.value]?.table_id === t.table_id) c.push('insert-after')
  return c.join(' ')
}

function toggleSel(t, e) {
  if (edit.value) return
  // 调换模式：点击目标桌台执行调换
  if (swapMode.value !== null) {
    if (t.table_id === swapMode.value) return
    performSwap(swapMode.value, t.table_id)
    return
  }
  const isCtrl = e && (e.ctrlKey || e.metaKey)
  if (isCtrl) {
    const idx = selIds.value.indexOf(t.table_id)
    if (idx >= 0) selIds.value.splice(idx, 1)
    else selIds.value.push(t.table_id)
  } else {
    selIds.value = [t.table_id]
  }
}

function openBooking(t) {
  if (edit.value) return
  // 全天模式下双击桌台，根据预订状态推断时段（参考宁国店预定系统）
  if (timeType.value === 'all' && t.booking) {
    const bt = t.booking.booking_time || t.booking.time_slot || ''
    const hour = parseInt(String(bt).split(':')[0] || '18')
    const inferred = hour < 15 ? 'lunch' : 'dinner'
    if (timeType.value !== inferred) {
      timeType.value = inferred
      loadData()
    }
  }
  selIds.value = [t.table_id]
  selTable.value = t
  initialBooking.value = t?.booking || t?.bk || null
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

// 右键菜单 - 编辑预订
function ctxEditBooking() {
  if (!ctxMenuTable.value) return
  const t = ctxMenuTable.value
  if (!t.booking) {
    ElMessage.warning('该桌台没有预订')
    ctxMenuVisible.value = false
    return
  }
  if (timeType.value === 'all') {
    const bt = t.booking.booking_time || t.booking.time_slot || ''
    const hour = parseInt(String(bt).split(':')[0] || '18')
    const inferred = hour < 15 ? 'lunch' : 'dinner'
    if (timeType.value !== inferred) {
      timeType.value = inferred
      loadData()
    }
  }
  selTable.value = t
  initialBooking.value = t.booking
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

// 复制预订：将已预订桌台的信息复制到选中的空闲桌台
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
      `将「${source.table_number}」的预订复制到以下桌台？\n\n${targetNames}\n\n客户：${bk.customer_name || '-'}\n电话：${bk.customer_phone || '-'}\n人数：${bk.guest_count || 0}人`,
      '复制预订 · Copy Booking',
      { confirmButtonText: '确定复制', cancelButtonText: '取消', type: 'info' }
    )
  } catch { return }
  try {
    const dateStr = fmtDate(curDate.value)
    let timeSlot = bk.booking_time || bk.time_slot || '12:00'
    if (!timeSlot.includes(':')) timeSlot = '12:00'
    for (const t of targets) {
      await createBooking({
        table_ids: [t.table_id],
        table_names: [t.table_number || ''],
        booking_date: dateStr,
        booking_time: timeSlot,
        customer_name: bk.customer_name || '',
        customer_phone: bk.customer_phone || '',
        guest_count: bk.guest_count || 0,
        table_count: 1,
        spare_tables: 0,
        occasion_type: bk.occasion_type || bk.banquet_type || '',
        deposit: bk.deposit || 0,
        remark: bk.remark || '',
        booking_status: 'confirmed',
        staff_name: bk.staff_name || ''
      })
    }
    ElMessage.success(`已复制到 ${targets.length} 个桌台`)
    selClear()
    loadData()
  } catch (e) {
    ElMessage.error('复制失败：' + (e?.message || '未知错误'))
  }
}

// 进入调换模式
function startSwapMode() {
  if (bookedList.value.length !== 2) {
    ElMessage.warning('请选择恰好2个已预订的桌台进行调换')
    return
  }
  const [t1, t2] = bookedList.value
  performSwap(t1.table_id, t2.table_id)
}

// 执行调换（交换两个已预订桌台的预订数据）
async function performSwap(srcId, dstId) {
  const src = list.value.find(t => t.table_id === srcId)
  const dst = list.value.find(t => t.table_id === dstId)
  if (!src || !dst) return
  try {
    await ElMessageBox.confirm(
      `将「${src.table_number}」和「${dst.table_number}」的预订互换？`,
      '调换台号 · Swap Table',
      { confirmButtonText: '确定调换', cancelButtonText: '取消', type: 'warning' }
    )
  } catch {
    swapMode.value = null
    return
  }
  try {
    const dateStr = fmtDate(curDate.value)
    const srcBk = src.booking
    const dstBk = dst.booking

    // 1. 取消两个桌台的预订
    await Promise.all([
      cancelBooking(srcBk.booking_id),
      cancelBooking(dstBk.booking_id)
    ])

    // 2. 在对方桌台创建新预订
    await Promise.all([
      createBooking({
        table_ids: [dstId],
        table_names: [dst.table_number || ''],
        booking_date: dateStr,
        booking_time: srcBk.booking_time || '12:00',
        customer_name: srcBk.customer_name || '',
        customer_phone: srcBk.customer_phone || '',
        guest_count: srcBk.guest_count || 0,
        table_count: srcBk.table_count || 1,
        spare_tables: srcBk.spare_tables || 0,
        occasion_type: srcBk.occasion_type || srcBk.banquet_type || '',
        deposit: srcBk.deposit || 0,
        remark: srcBk.remark || '',
        booking_status: 'confirmed',
        staff_name: srcBk.staff_name || ''
      }),
      createBooking({
        table_ids: [srcId],
        table_names: [src.table_number || ''],
        booking_date: dateStr,
        booking_time: dstBk.booking_time || '12:00',
        customer_name: dstBk.customer_name || '',
        customer_phone: dstBk.customer_phone || '',
        guest_count: dstBk.guest_count || 0,
        table_count: dstBk.table_count || 1,
        spare_tables: dstBk.spare_tables || 0,
        occasion_type: dstBk.occasion_type || dstBk.banquet_type || '',
        deposit: dstBk.deposit || 0,
        remark: dstBk.remark || '',
        booking_status: 'confirmed',
        staff_name: dstBk.staff_name || ''
      })
    ])

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

async function loadData() {
  try {
    const res = await getTableStatus({ date: fmtDate(curDate.value), timeType: timeType.value })
    if (res.code === 200) list.value = (res.data || []).sort((a, b) => (a.sort_order || 0) - (b.sort_order || 0))
  } catch (e) { console.error('load', e) }
}

onMounted(() => {
  loadData()
  // ESC键取消桌台选择（参考宁国店预定系统）
  window.addEventListener('keydown', onKeydown)
  // 点击空白处关闭右键菜单
  window.addEventListener('click', hideCtxMenu)
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
  grid-template-columns: repeat(auto-fill, minmax(165px, 1fr));
  gap: 14px;
}

.table-item {
  position: relative;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 16px 12px;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
  min-height: 125px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-shadow: 0 2px 6px rgba(0,0,0,0.03);
}
.table-item:hover {
  transform: translateY(-4px) scale(1.02);
  border-color: var(--color-primary);
  box-shadow: 0 20px 30px -12px rgba(45, 74, 62, 0.18);
}

.table-item.selected {
  border: 2px solid var(--color-accent) !important;
  box-shadow: 0 0 0 4px rgba(196, 163, 90, 0.2), 0 12px 32px -8px rgba(196, 163, 90, 0.25) !important;
  transform: translateY(-2px);
  background: rgba(196, 163, 90, 0.08) !important;
  animation: tbSwapPulse 1.2s ease-in-out infinite;
}
@keyframes tbSwapPulse {
  0%, 100% { box-shadow: 0 0 0 4px rgba(196, 163, 90, 0.2), 0 12px 32px -8px rgba(196, 163, 90, 0.2); }
  50% { box-shadow: 0 0 0 6px rgba(196, 163, 90, 0.35), 0 16px 40px -8px rgba(196, 163, 90, 0.3); }
}
.table-item.selected::after {
  content: '\2713';
  position: absolute;
  top: 8px;
  right: 10px;
  width: 24px;
  height: 24px;
  background: var(--color-accent);
  color: #fff;
  border-radius: 50%;
  font-size: 13px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.table-item.status-free { border-color: rgba(45, 74, 62, 0.2); }

.table-item.status-booked {
  background: linear-gradient(135deg, #FDF6E3 0%, #FAF0D7 100%) !important;
  border: 1px solid var(--color-accent) !important;
}

.table-item.editing {
  cursor: grab;
  border: 2px dashed var(--color-primary);
  background: linear-gradient(135deg, rgba(45, 74, 62, 0.05) 0%, rgba(255, 255, 255, 0.9) 100%);
  transform: scale(0.97);
  border-radius: 2px;
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

.table-item .table-name { font-weight: 700; font-size: 17px; color: var(--color-text-primary); letter-spacing: 1px; }
.table-item .table-capacity { font-size: 12px; color: var(--color-text-secondary); margin-top: 6px; }

.table-guest { font-weight: 600; color: var(--color-accent); font-size: 13px; margin-top: 6px; background: rgba(196, 163, 90, 0.12); padding: 4px 14px; border-radius: 2px; display: inline-block; }
.table-phone { font-size: 12px; color: var(--color-text-secondary); margin-top: 4px; }
.table-booked-people { font-size: 12px; color: var(--color-text-secondary); margin-top: 2px; }
.table-booking-time { font-size: 11px; color: var(--color-text-secondary); margin-top: 2px; }
.banquet-type-badge { display: inline-block; font-size: 10px; padding: 2px 8px; border-radius: 2px; background: var(--color-primary); color: #fff; margin-top: 4px; letter-spacing: 1px; }

.status-dot { position: absolute; top: 10px; right: 10px; width: 10px; height: 10px; border-radius: 50%; }
.status-free .status-dot { background: var(--color-primary); box-shadow: 0 0 6px rgba(45, 74, 62, 0.4); }
.status-booked .status-dot { background: var(--color-accent); box-shadow: 0 0 6px rgba(196, 163, 90, 0.5); }
.table-item.selected .status-dot { display: none; }

.drag-handle { position: absolute; top: 10px; left: 10px; font-size: 18px; color: var(--color-text-secondary); cursor: grab; }
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

/* 浮动操作工具栏 */
.action-toolbar {
  position: fixed;
  bottom: 30px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(16px);
  border: 1px solid var(--color-border);
  border-radius: 2px;
  padding: 14px 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.06);
  z-index: 500;
  justify-content: center;
  max-width: 90vw;
}
.toolbar-fade-enter-active,
.toolbar-fade-leave-active {
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.toolbar-fade-enter-from,
.toolbar-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(20px);
}
.action-summary {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-accent);
  margin-right: 8px;
  white-space: nowrap;
}
.action-btn {
  padding: 8px 16px;
  border-radius: 2px;
  font-size: 13px;
  font-weight: 600;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  white-space: nowrap;
}
.action-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.action-copy {
  background: var(--color-accent);
  color: #fff;
  border-color: var(--color-accent);
}
.action-copy:hover:not(:disabled) {
  transform: scale(1.03) translateY(-2px);
  box-shadow: 0 8px 20px rgba(196, 163, 90, 0.3);
}
.action-swap {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.action-swap:hover:not(:disabled) {
  transform: scale(1.03) translateY(-2px);
  box-shadow: 0 8px 20px rgba(45, 74, 62, 0.3);
}
.action-delete {
  background: #fff;
  color: #dc2626;
  border-color: #fecaca;
}
.action-delete:hover:not(:disabled) {
  background: #fef2f2;
  transform: scale(1.03) translateY(-2px);
}
.action-cancel {
  background: var(--color-bg);
  color: var(--color-text-secondary);
  border-color: var(--color-border);
}
.action-cancel:hover {
  transform: translateY(-1px);
  border-color: var(--color-text-secondary);
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
  z-index: 2000;
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
</style>

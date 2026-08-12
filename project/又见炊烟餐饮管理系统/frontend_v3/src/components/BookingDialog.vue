<template>
  <el-dialog v-model="visible" width="900px" class="bk-dlg" :close-on-click-modal="false" :show-close="false" @opened="onOpened">
    <template #header>
      <div class="bk-header">
        <div class="bk-header-left">
          <div class="bk-header-icon">
            <el-icon><KnifeFork /></el-icon>
          </div>
          <div>
            <h2 class="bk-header-title">{{ readonly ? `${t('booking.detail')}` : `${t('booking.title')}` }}</h2>
            <p class="bk-header-sub">{{ readonly ? t('booking.detailEn') : t('booking.titleEn') }} · {{ form.booking_id }}</p>
          </div>
        </div>
        <div class="bk-header-right">
          <span class="bk-status-badge" v-if="form.booking_status">
            <el-icon><Check /></el-icon>
            {{ statusLabel(form.booking_status) }} · {{ statusLabelEn(form.booking_status) }}
          </span>
          <button v-if="readonly && isEdit && !noEdit" class="bk-header-edit-btn" @click="enterEditMode">
            <el-icon><Edit /></el-icon>
            {{ t('booking.edit') }}
          </button>
          <button class="bk-close-btn" @click="visible = false" aria-label="关闭">
            <el-icon><Close /></el-icon>
          </button>
        </div>
      </div>
    </template>

    <!-- 标签页导航 -->
    <nav class="bk-tabs">
      <button
        v-for="tb in tabs"
        :key="tb.key"
        :class="['bk-tab', { active: activeTab === tb.key }]"
        @click="activeTab = tb.key"
      >
        {{ tb.label }}
        <span class="bk-tab-en">{{ tb.en }}</span>
      </button>
    </nav>

    <!-- 主体内容 -->
    <div class="bk-body" @dblclick="handleBodyDblclick">
      <!-- ============ Tab 1: 预订时间 ============ -->
      <div v-show="activeTab === 'basic'" class="bk-basic">
        <!-- 顶部信息条：单号 + 创建时间 -->
        <div class="bk-info-bar bk-info-top">
          <span class="bk-info-item">
            <span class="bk-info-label">单号 · No.</span>
            <span class="bk-info-val">{{ form.booking_id }}</span>
          </span>
          <span class="bk-info-sep"></span>
          <span class="bk-info-item">
            <span class="bk-info-label">创建时间 · Created</span>
            <span class="bk-info-val">{{ form.created_at }}</span>
          </span>
        </div>

        <!-- 三行三列表单区 -->
        <div class="bk-form-grid">
          <!-- 行1：餐别 | 日期 | 时间 -->
          <div class="bk-field">
            <label class="bk-label">餐别 <span class="bk-label-en">· Meal</span></label>
            <div class="bk-meal-switch">
              <button
                :class="['bk-meal-btn', { active: mealPeriod === 'lunch' }]"
                @click="setMeal('lunch')"
                :disabled="readonly"
              >午餐 · Lunch</button>
              <button
                :class="['bk-meal-btn', { active: mealPeriod === 'dinner' }]"
                @click="setMeal('dinner')"
                :disabled="readonly"
              >晚餐 · Dinner</button>
            </div>
          </div>
          <div class="bk-field">
            <label class="bk-label">日期 <span class="bk-label-en">· Date</span></label>
            <el-date-picker v-model="form.booking_date" type="date" value-format="YYYY-MM-DD" :placeholder="`${t('booking.date')} · ${t('booking.dateEn')}`" :disabled-date="disabledPast" :disabled="readonly" class="bk-input" />
          </div>
          <div class="bk-field">
            <label class="bk-label">时间 <span class="bk-label-en">· Time</span></label>
            <el-select v-model="form.booking_time" :placeholder="`${t('booking.period')} · ${t('booking.periodEn')}`" :disabled="readonly" class="bk-input">
              <el-option v-for="item in currentTimeOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </div>

          <!-- 行2：客户姓名 | 手机号 | 宴席类型 -->
          <div class="bk-field">
            <label class="bk-label">客户姓名 <span class="bk-label-en">· Customer Name</span> <span class="bk-req">*</span></label>
            <el-autocomplete v-model="form.customer_name" :fetch-suggestions="queryCustomers" :placeholder="`${t('booking.customer.name')} · ${t('booking.customer.nameEn')}`" @select="onCustomerSelect" :disabled="readonly" class="bk-input" />
          </div>
          <div class="bk-field">
            <label class="bk-label">手机号 <span class="bk-label-en">· Phone</span> <span class="bk-req">*</span></label>
            <el-input v-model="form.customer_phone" :placeholder="`${t('booking.customer.phone')} · ${t('booking.customer.phoneEn')}`" :disabled="readonly" maxlength="11" class="bk-input" @blur="onPhoneBlur" />
          </div>
          <div class="bk-field">
            <label class="bk-label">宴席类型 <span class="bk-label-en">· Occasion Type</span></label>
            <el-select v-model="form.occasion_type" :placeholder="`${t('booking.occasion.type')} · ${t('booking.occasion.typeEn')}`" :disabled="readonly" class="bk-input">
              <el-option v-for="o in occasionOptions" :key="o.value" :label="o.label" :value="o.value" />
            </el-select>
          </div>

          <!-- 行3：定金 | 菜品总额 | 统筹负责人 -->
          <div class="bk-field">
            <label class="bk-label">定金 <span class="bk-label-en">· Deposit</span></label>
            <el-input v-model="form.deposit" :placeholder="`${t('booking.payment.deposit')} · ${t('booking.payment.depositEn')}`" type="number" :disabled="readonly" class="bk-input" />
          </div>
          <div class="bk-field">
            <label class="bk-label">菜品总额 <span class="bk-label-en">· Total Amount</span></label>
            <el-input :model-value="`¥${dishTotal}`" disabled class="bk-input" />
          </div>
          <div class="bk-field">
            <label class="bk-label">统筹负责人 <span class="bk-label-en">· Coordinator</span></label>
            <el-input v-model="form.staff_name" :placeholder="`${t('booking.coordinator')} · ${t('booking.coordinatorEn')}`" :disabled="readonly" class="bk-input" />
          </div>
        </div>

        <!-- 桌台配置块 -->
        <div class="bk-block">
          <div class="bk-block-title-row">
            <div class="bk-block-title">
              <el-icon><User /></el-icon>
              <h3>桌台配置 <span class="bk-title-en">· Seating</span></h3>
            </div>
            <div class="bk-stats-inline">
              <div class="bk-stat-mini">
                <span class="bk-stat-mini-label">桌数</span>
                <span class="bk-stat-mini-val">{{ form.table_count }}</span>
              </div>
              <span class="bk-stat-divider">+</span>
              <div class="bk-stat-mini">
                <span class="bk-stat-mini-label">备桌</span>
                <span class="bk-stat-mini-val">{{ form.spare_tables }}</span>
              </div>
              <span class="bk-stat-divider">×</span>
              <div class="bk-stat-mini">
                <span class="bk-stat-mini-label">人/桌</span>
                <span class="bk-stat-mini-val">{{ form.guest_per_table }}</span>
              </div>
              <span class="bk-stat-divider">=</span>
              <div class="bk-stat-mini bk-stat-mini-highlight">
                <span class="bk-stat-mini-label">合计</span>
                <span class="bk-stat-mini-val">{{ totalGuests }}</span>
              </div>
            </div>
          </div>
          <div class="bk-stat-inputs bk-stat-inputs-row">
            <div class="bk-field bk-field-inline">
              <label class="bk-label bk-label-inline">桌数</label>
              <el-input-number v-model="form.table_count" :min="1" :max="200" :disabled="readonly" class="bk-input" />
            </div>
            <div class="bk-field bk-field-inline">
              <label class="bk-label bk-label-inline">备桌</label>
              <el-input-number v-model="form.spare_tables" :min="0" :max="50" :disabled="readonly" class="bk-input" />
            </div>
            <div class="bk-field bk-field-inline">
              <label class="bk-label bk-label-inline">人/桌</label>
              <el-input-number v-model="form.guest_per_table" :min="1" :max="20" :disabled="readonly" class="bk-input" />
            </div>
          </div>
        </div>

        <!-- 已选桌台块 -->
        <div class="bk-block">
          <div class="bk-block-title-row">
            <div class="bk-block-title">
              <el-icon><Grid /></el-icon>
              <h3>已选桌台 <span class="bk-title-en">· Selected Tables</span></h3>
            </div>
            <button class="bk-order-btn" @click="openDishDialog" :disabled="readonly">
              <el-icon><KnifeFork /></el-icon>
              {{ hasDishes ? `${t('booking.tables.ordered')} · ${t('booking.tables.orderedEn')}` : `${t('booking.tables.order')} · ${t('booking.tables.orderEn')}` }}
            </button>
          </div>
          <div class="bk-chips">
            <span v-for="(tb, idx) in selectedTables" :key="tb.table_id" :class="['bk-chip', { 'bk-chip-primary': idx === 0, 'bk-chip-secondary': idx > 0 }]">{{ tb.table_name || tb.table_number }}</span>
            <span v-if="selectedTables.length === 0" class="bk-empty">{{ t('booking.tables.noSelection') }} · {{ t('booking.tables.noSelectionEn') }}</span>
          </div>
        </div>

        <!-- 备注 -->
        <div class="bk-block">
          <div class="bk-field">
            <label class="bk-label">备注 <span class="bk-label-en">· Remark</span></label>
            <el-input v-model="form.remark" type="textarea" :rows="2" :placeholder="`${t('booking.remark')} · ${t('booking.remarkEn')}`" :disabled="readonly" class="bk-input" />
          </div>
        </div>
      </div>

      <!-- ============ Tab 2: 客户历史 ============ -->
      <div v-show="activeTab === 'history'" class="bk-tab-body">
        <div v-if="customerHistory.length > 0" class="bk-list">
          <div v-for="h in customerHistory" :key="h.booking_id" class="bk-list-item">
            <span class="bk-list-date">{{ h.booking_date }}</span>
            <span :class="['bk-list-status', h.booking_status]">{{ statusLabel(h.booking_status) }}</span>
            <span>{{ h.banquet_name || '宴席' }}</span>
            <span>{{ h.guest_count }}人</span>
            <span class="bk-list-amount">¥{{ h.total_amount }}</span>
          </div>
        </div>
        <el-empty v-else :description="`${t('booking.customer.history')} · ${t('booking.customer.historyEn')}`" />
      </div>

      <!-- ============ Tab 3: 菜单 ============ -->
      <div v-show="activeTab === 'menu'" class="bk-tab-body">
        <div v-if="dishOrderItems.length > 0" class="bk-list">
          <div v-for="d in dishOrderItems" :key="d.dishId" class="bk-list-item">
            <span>{{ d.dishName }}</span>
            <span>×{{ d.qty }}</span>
            <span class="bk-list-amount">¥{{ d.price * d.qty }}</span>
          </div>
          <div class="bk-list-total">{{ t('menu.total') }} · {{ t('menu.totalEn') }}：¥{{ dishTotal }}</div>
        </div>
        <el-empty :description="`${t('menu.noDishes')} · ${t('menu.noDishesEn')}`" />
      </div>

      <!-- ============ Tab 4: 变更记录 ============ -->
      <div v-show="activeTab === 'logs'" class="bk-tab-body" v-if="isEdit">
        <div v-if="changeLogs.length > 0" class="bk-list">
          <div v-for="log in changeLogs" :key="log.logId" class="bk-list-item bk-log">
            <div class="bk-log-head">
              <span class="bk-log-time">{{ formatLogTime(log.createdAt) }}</span>
              <span class="bk-log-op">{{ log.operatorName }}</span>
            </div>
            <div class="bk-log-detail">{{ log.detail || log.summary }}</div>
          </div>
        </div>
        <el-empty :description="`${t('logs.noRecords')} · ${t('logs.noRecordsEn')}`" />
      </div>
    </div>

    <template #footer>
      <div class="bk-footer">
        <button class="bk-btn bk-btn-default" @click="doCancel" :disabled="loading">
          {{ readonly ? `${t('booking.close')} · ${t('booking.closeEn')}` : `${t('booking.cancel')} · ${t('booking.cancelEn')}` }}
        </button>
        
                <!-- 一键通知文案 -->
        <div v-if="readonly && isEdit && form.booking_status === 'confirmed'" class="bk-footer-notify">
          <button class="bk-btn bk-btn-notify" @click="copyNotification">
            <el-icon><DocumentCopy /></el-icon>
            复制通知文案 · Copy Notice
          </button>
        </div>

        <!-- 打印按钮组 - 根据预订状态显示 -->
        <div v-if="readonly && isEdit" class="bk-footer-print">
          <!-- 已确认：显示所有打印选项 -->
          <template v-if="form.booking_status === 'confirmed'">
            <button class="bk-btn bk-btn-print" @click="emitPrint('confirmation')">
              <el-icon><Printer /></el-icon>
              打印确认单
            </button>
            <button class="bk-btn bk-btn-print" @click="emitPrint('table_sign')">
              <el-icon><Printer /></el-icon>
              打印桌签
            </button>
          </template>
          <!-- 已取消：只显示打印取消单 -->
          <template v-else-if="form.booking_status === 'cancelled'">
            <button class="bk-btn bk-btn-print" @click="emitPrint('cancellation')">
              <el-icon><Printer /></el-icon>
              打印取消单
            </button>
          </template>
          <!-- 待确认：不显示打印按钮 -->
        </div>
        
        <button v-if="!readonly" class="bk-btn bk-btn-primary" @click="doSave" :disabled="loading">
          <el-icon v-if="!loading"><Check /></el-icon>
          {{ loading ? '保存中...' : `${t('booking.submit')} · ${t('booking.submitEn')}` }}
        </button>
      </div>
    </template>

    <DishOrderDialog v-if="dishVisible" v-model="dishVisible" :date="form.booking_date" :period="form.booking_time" :table-name="currentTableName" :booking-id="currentBookingId" @confirmed="onDishConfirmed" />
  </el-dialog>
</template>

<script setup>
import { ref, watch, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import 'element-plus/es/components/message/style/css'
import 'element-plus/es/components/message-box/style/css'
import { KnifeFork, Check, Close, User, Grid, Edit, Printer, DocumentCopy } from '@element-plus/icons-vue'
import { useUserStore } from '@/store/user'
import DishOrderDialog from './DishOrderDialog.vue'
import { searchCustomers } from '../api/customer'
import { getBookingDetail } from '../api/booking'
import { getTableOrders } from '../utils/menuStore'

const props = defineProps({
  modelValue: Boolean,
  date: String,
  tableId: Number,
  tableNumber: String,
  tableName: String,
  booking: Object,
  tableIds: Array,
  tableNames: Array
})
const emit = defineEmits(['update:modelValue', 'saved', 'date-change', 'period-change', 'print'])

const { t } = useI18n()

const tabs = [
  { key: 'basic', label: '预订时间', en: 'Booking Time' },
  { key: 'history', label: '客户历史', en: 'Customer History' },
  { key: 'menu', label: '菜单', en: 'Menu' },
  { key: 'logs', label: '变更记录', en: 'Changes' }
]

const timeOptions = {
  lunch: [
    { label: '11:00', value: '11:00:00' },
    { label: '11:30', value: '11:30:00' },
    { label: '12:00', value: '12:00:00' }
  ],
  dinner: [
    { label: '17:00', value: '17:00:00' },
    { label: '17:30', value: '17:30:00' },
    { label: '18:00', value: '18:00:00' },
    { label: '18:30', value: '18:30:00' },
    { label: '19:00', value: '19:00:00' }
  ]
}
const mealPeriod = ref('dinner')
const currentTimeOptions = computed(() => timeOptions[mealPeriod.value])

function setMeal(m) {
  const oldMeal = mealPeriod.value
  mealPeriod.value = m
  const options = timeOptions[m]
  if (options && options.length > 0) {
    form.value.booking_time = options[0].value
  }
  if (visible.value && !readonly.value && selectedTables.value.length > 0) {
    emit('period-change', m, oldMeal, form.value.booking_date)
  }
}

const occasionOptions = [
  { label: '零点', value: 'a_la_carte' }, { label: '婚宴', value: 'wedding' },
  { label: '生日宴', value: 'birthday' }, { label: '订婚宴', value: 'engagement' },
  { label: '满月宴', value: 'baby_born' }, { label: '谢师宴', value: 'graduation' },
  { label: '帮忙酒', value: 'help_wine' }, { label: '乔迁宴', value: 'house_move' },
  { label: '升迁宴', value: 'promotion' }, { label: '团圆宴', value: 'reunion' },
  { label: '答谢宴', value: 'thanksgiving' }, { label: '尾牙宴', value: 'year_end' }
]
const statusOptions = [
  { label: '待确认', value: 'pending' }, { label: '已确认', value: 'confirmed' },
  { label: '已完成', value: 'completed' }, { label: '已取消', value: 'cancelled' }
]

const visible = ref(props.modelValue)
const activeTab = ref('basic')
const loading = ref(false)
const customerHistory = ref([])
const dishVisible = ref(false)
const currentTableName = ref('')
const currentBookingId = ref('')
const readonly = ref(false)
const noEdit = ref(false)

const form = ref(emptyForm())
const isEdit = computed(() => !!props.booking)

function emptyForm() {
  const userName = localStorage.getItem('userName') || localStorage.getItem('username') || localStorage.getItem('user_name') || localStorage.getItem('nickName') || localStorage.getItem('displayName') || ''
  return {
    booking_id: '', created_at: '', staff_name: userName,
    booking_date: '', booking_time: '', customer_name: '', customer_phone: '',
    occasion_type: 'banquet', guest_per_table: 10, table_count: 1, spare_tables: 0,
    deposit: '', booking_status: 'confirmed', remark: ''
  }
}

const selectedTables = ref([])
const dishOrderItems = ref([])
const changeLogs = ref([])

const totalGuests = computed(() =>
  (form.value.table_count + form.value.spare_tables) * form.value.guest_per_table
)
const hasDishes = computed(() => dishOrderItems.value.length > 0)
const dishTotal = computed(() =>
  dishOrderItems.value.reduce((sum, d) => sum + d.price * d.qty, 0)
)

function disabledPast(date) {
  const d = new Date(date)
  d.setHours(0, 0, 0, 0)
  return d.getTime() < Date.now() - 864e5
}

function onCustomerSelect(item) {
  form.value.customer_name = item.value
  form.value.customer_phone = item.phone || ''
  loadCustomerHistory(item.id)
}

async function queryCustomers(qs, cb) {
  if (!qs || qs.length < 1) return cb([])
  try {
    const res = await searchCustomers({ q: qs })
    const data = res.data?.list || res.data?.rows || res.data || []
    const rows = Array.isArray(data) ? data : []
    cb(rows.map(c => ({
      value: c.customerName || c.customer_name || c.name,
      phone: c.customerPhone || c.customer_phone || c.phone,
      id: c.customerId || c.customer_id || c.id
    })))
  } catch { cb([]) }
}

// 手机号失焦 → 查询该号码历史预订 + 回头次数
async function onPhoneBlur() {
  const phone = form.value.customer_phone?.trim()
  if (!phone || phone.length < 11) return
  if (!/^1[3-9]\d{9}$/.test(phone)) return
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/customers?phone=' + encodeURIComponent(phone), {
      credentials: 'include',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const json = await res.json()
    const customers = json.data?.rows || json.data || []
    if (customers.length > 0) {
      const c = customers[0]
      // 自动填入姓名
      if (!form.value.customer_name) {
        form.value.customer_name = c.customerName || c.customer_name || c.name || ''
      }
      // 加载历史预订
      const custId = c.customerId || c.customer_id || c.id
      if (custId) {
        await loadCustomerHistory(custId)
      }
      // 提示回头次数
      const visitCount = c.totalVisits || c.visit_count || c.total_visits || customerHistory.value.length
      if (visitCount >= 5) {
        ElMessage.success('VIP客户！历史到店 ' + visitCount + ' 次')
      } else if (visitCount >= 2) {
        ElMessage.info('回头客，历史到店 ' + visitCount + ' 次')
      }
      // 自动切换到历史 Tab
      if (customerHistory.value.length > 0) {
        activeTab.value = 'history'
      }
    }
  } catch (e) {
    // 静默失败
  }
}


async function loadCustomerHistory(customerId) {
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/bookings?customer_id=' + encodeURIComponent(customerId), {
      credentials: 'include',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const json = await res.json()
    customerHistory.value = json.data?.rows || json.data || []
  } catch { customerHistory.value = [] }
}

function statusLabel(s) {
  const m = { pending: '待确认', confirmed: '已确认', completed: '已完成', cancelled: '已取消' }
  return m[s] || s
}
function statusLabelEn(s) {
  const m = { pending: 'Pending', confirmed: 'Confirmed', completed: 'Completed', cancelled: 'Cancelled' }
  return m[s] || s
}

function loadDishOrders() {
  const date = form.value.booking_date
  const period = form.value.booking_time
  const tableName = selectedTables.value.map(t => t.table_name || t.table_number).join('、')
  dishOrderItems.value = []
  if (date && period && tableName) {
    const tableOrders = getTableOrders(date, period, tableName)
    dishOrderItems.value = tableOrders.map(o => ({
      dishId: o.dishCode,
      dishName: o.dishCode,
      qty: o.qty,
      price: 0,
      remark: o.remark || ''
    }))
  }
}

function openDishDialog() {
  currentTableName.value = selectedTables.value.map(t => t.table_name || t.table_number).join('、')
  currentBookingId.value = form.value.booking_id || props.booking?.id || props.booking?.bookingId || props.booking?.booking_id || ''
  dishVisible.value = true
  activeTab.value = 'menu'
}

function onDishConfirmed() {
  dishVisible.value = false
  loadDishOrders()
}

// 保存预订后，自动将本地点菜数据提交到后端
async function submitLocalDishesToBackend(bookingId) {
  try {
    const tableNames = selectedTables.value.map(t => t.table_name || t.table_number).filter(Boolean)
    const allDishes = []
    // 收集所有桌台的点菜数据
    for (const name of tableNames) {
      const orders = getTableOrders(form.value.booking_date, form.value.booking_time, name)
      if (orders && orders.length > 0) {
        allDishes.push(...orders)
      }
    }
    if (allDishes.length === 0) return

    const token = localStorage.getItem('token') || ''
    const dishes = allDishes.map(o => ({
      dish_id: o.dishCode,
      dish_quantity: o.qty || 1,
      store_id: 1
    }))
    const res = await fetch(`/api/bookings/${bookingId}/dishes/batch`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify(dishes)
    })
    const data = await res.json()
    if (data.code === 200) {
      console.log('点菜数据已自动提交后端:', dishes.length, '道菜')
    }
  } catch (e) {
    console.error('自动提交点菜数据失败:', e)
  }
}

function autoRemark() {
  const tables = form.value.table_count
  const spare = form.value.spare_tables
  const total = totalGuests.value
  form.value.remark = tables + '桌备' + spare + '桌，共' + total + '人'
}

async function doSave() {
  if (!validateBooking()) return
  loading.value = true
  const firstTable = selectedTables.value[0]
  
  const body = {
    customer_name: form.value.customer_name.trim(),
    customer_phone: form.value.customer_phone.trim(),
    booking_date: fmtDateForApi(form.value.booking_date),
    booking_time: form.value.booking_time,
    guest_count: totalGuests.value,
    table_count: form.value.table_count,
    spare_tables: form.value.spare_tables,
    occasion_type: form.value.occasion_type,
    deposit: form.value.deposit ? parseFloat(form.value.deposit) : null,
    remark: form.value.remark || '',
    booking_status: 'confirmed',
    table_ids: selectedTables.value.map(t => t.table_id).filter(Boolean),
    table_names: selectedTables.value.map(t => t.table_name || t.table_number || '')
  }

  console.log('提交预订数据:', body)

  try {
    const token = localStorage.getItem('token') || ''
    const editId = props.booking?.id || props.booking?.bookingId || props.booking?.booking_id
    const url = editId ? '/api/bookings/' + editId : '/api/bookings'
    const method = editId ? 'PUT' : 'POST'

    console.log('请求URL:', url, '方法:', method)

    const res = await fetch(url, {
      method,
      credentials: 'include',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': 'Bearer ' + token
      },
      body: JSON.stringify(body)
    })

    const data = await res.json()
    console.log('服务器响应:', data)

    if (data.code === 200) {
      const bookingId = data.data?.bookingId || data.data?.order_no || data.data?.id || data.data?.booking_id || ''
      // 保存成功后，自动提交本地点菜数据到后端
      if (bookingId) {
        await submitLocalDishesToBackend(bookingId)
      }
      ElMessage.success(bookingId ? '保存成功！单号：' + bookingId : '保存成功')
      visible.value = false
      dishOrderItems.value = []
      emit('saved', data.data)
    } else {
      let errMsg = data.message || '服务器返回错误'
      if (res.status === 401 || res.status === 403) {
        errMsg = '登录已过期，请重新登录'
      } else if (res.status === 500) {
        errMsg = '服务器内部错误'
      }
      ElMessageBox.alert(errMsg, '保存失败 · Save Failed', {
        confirmButtonText: '我知道了 · OK',
        appendToBody: true,
        customClass: 'bk-message-box'
      })
    }
  } catch (e) {
    console.error('Save error:', e)
    ElMessageBox.alert(e.message || '网络异常，请检查网络连接', '保存失败 · Save Failed', {
      confirmButtonText: '我知道了 · OK',
      appendToBody: true,
      customClass: 'bk-message-box'
    })
  } finally {
    loading.value = false
  }
}

function doCancel() { visible.value = false }
function enterEditMode() { if (!noEdit.value) readonly.value = false }
function handleBodyDblclick() {
  if (readonly.value && isEdit.value && !noEdit.value) {
    readonly.value = false
  }
}

function emitPrint(type) {
  const bookingTables = selectedTables.value.map(t => ({
    table_name: t.table_name || t.table_number || '-',
    table_number: t.table_number || t.table_name || '-'
  }))

  let phone = form.value.customer_phone || '-'
  if (phone.length === 11) phone = phone.slice(0, 3) + '****' + phone.slice(7)

  const printData = {
    booking_id: form.value.booking_id || '-',
    booking_date: form.value.booking_date || '-',
    booking_time: form.value.booking_time || '',
    customer_name: form.value.customer_name || '-',
    customer_phone: phone,
    occasion_type: form.value.occasion_type || 'a_la_carte',
    table_count: form.value.table_count || 1,
    spare_tables: form.value.spare_tables || 0,
    guest_per_table: form.value.guest_per_table || 10,
    deposit: form.value.deposit || '',
    remark: form.value.remark || '',
    booking_tables: bookingTables,
    booking_status: form.value.booking_status || 'confirmed'
  }
  emit('print', { type, data: printData })
}

function updateSelectedTables(tables) {
  if (!tables || !Array.isArray(tables)) {
    selectedTables.value = []
    return false
  }
  if (selectedTables.value.length === 0) return false
  const selectedIds = selectedTables.value.map(t => t.table_id)
  const newSelected = []
  const bookedNames = []
  selectedIds.forEach(id => {
    const found = tables.find(t => t.table_id === id)
    if (!found) return
    if (found.booking) {
      bookedNames.push(found.table_name || found.table_number || '未知桌台')
    } else {
      newSelected.push(found)
    }
  })
  if (bookedNames.length > 0) {
    const names = bookedNames.join('、')
    ElMessageBox.alert(
      `「${names}」在选择的日期/时间已有预订，请选择其他桌台。\nThe selected table(s) are already booked for the chosen date/time. Please choose another.`,
      '桌台已被预订 · Table Already Booked',
      { confirmButtonText: '我知道了 · OK', customClass: 'bk-message-box' }
    )
    return true
  } else {
    selectedTables.value = newSelected
    return false
  }
}

function revertDate(oldDate) {
  const stopWatch = watch(() => form.value.booking_date, () => {
    stopWatch()
  })
  form.value.booking_date = oldDate
}

function revertPeriod(oldPeriod) {
  const stopWatch = watch(() => mealPeriod.value, () => {
    stopWatch()
  })
  mealPeriod.value = oldPeriod
  const options = timeOptions[oldPeriod]
  if (options && options.length > 0) {
    form.value.booking_time = options[0].value
  }
}

function getCurrentPeriod() {
  return mealPeriod.value
}

function validateBooking() {
  const errors = []
  
  if (!form.value.booking_date) {
    errors.push('日期未选择')
  }
  if (!form.value.booking_time) {
    errors.push('时段未选择')
  }
  if (!form.value.customer_name || !form.value.customer_name.trim()) {
    errors.push('客户姓名为空')
  }
  if (!form.value.customer_phone || !form.value.customer_phone.trim()) {
    errors.push('手机号为空')
  } else if (!/^1[3-9]\d{9}$/.test(form.value.customer_phone.trim())) {
    errors.push('手机号格式不正确（应为11位数字，以1开头）')
  }
  if (selectedTables.value.length === 0) {
    errors.push('未选择桌台')
  }
  if (!form.value.table_count || form.value.table_count < 1) {
    errors.push('桌数不能小于1')
  }
  if (!form.value.guest_per_table || form.value.guest_per_table < 1) {
    errors.push('每桌人数不能小于1')
  }
  if (!form.value.occasion_type) {
    errors.push('宴席类型未选择')
  }
  if (form.value.deposit && isNaN(parseFloat(form.value.deposit))) {
    errors.push('定金不是有效数字')
  }
  
  if (errors.length > 0) {
    const reason = errors.map((e, i) => `${i + 1}. ${e}`).join('\n')
    ElMessageBox.alert(reason, '无法保存 · Unable to Save', {
      confirmButtonText: '我知道了 · OK',
      appendToBody: true,
      customClass: 'bk-message-box'
    })
    return false
  }
  return true
}

function generateBookingId() {
  const now = new Date()
  const date = now.getFullYear().toString() +
    String(now.getMonth() + 1).padStart(2, '0') +
    String(now.getDate()).padStart(2, '0')
  const time = String(now.getHours()).padStart(2, '0') +
    String(now.getMinutes()).padStart(2, '0') +
    String(now.getSeconds()).padStart(2, '0')
  const random = String(Math.floor(Math.random() * 10000)).padStart(4, '0')
  return `BK${date}${time}${random}`
}

function getCurrentDateTime() {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(now.getDate()).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`
}

watch(() => props.modelValue, async (val) => {
  visible.value = val
  if (val) {
    const b = props.booking
    const bookingId = b?.id || b?.bookingId || b?.booking_id || b?.order_no || ''
    noEdit.value = !!b?._noEdit
    readonly.value = (!!bookingId && !b?._editMode) || noEdit.value
    if (b?._goDishTab) {
      activeTab.value = 'menu'
    }
    if (bookingId) {
      try {
        const data = await getBookingDetail(bookingId)
        if (data && data.code === 200 && data.data) {
          const raw = data.data
          // API返回 { booking: {...}, tables: [...], dishes: [...] }，兼容直接返回booking对象
          const d = raw.booking || raw
          let rawTime = d.time_slot || d.booking_time || d.bookingTime || ''
          if (rawTime && rawTime.length === 5) rawTime = rawTime + ':00'
          if (!rawTime && d.bookingTime) rawTime = d.bookingTime
          const hour = parseInt(rawTime.split(':')[0] || '18')
          mealPeriod.value = hour < 15 ? 'lunch' : 'dinner'

          const occType = d.banquet_type || d.occasion_type || d.occasionType || 'wedding'
          const validOccasions = occasionOptions.map(o => o.value)
          const finalOccasion = validOccasions.includes(occType) ? occType : 'wedding'

          form.value = {
            booking_id: d.order_no || d.id || d.booking_id || d.bookingId || bookingId,
            created_at: d.created_at || d.createdAt || '',
            staff_name: d.created_by || d.staffName || d.staff_name || '',
            booking_date: d.booking_date || d.bookingDate || '',
            booking_time: rawTime,
            customer_name: d.customer_name || d.customerName || '',
            customer_phone: d.customer_phone || d.customerPhone || '',
            occasion_type: finalOccasion,
            guest_per_table: d.guest_per_table || d.guestPerTable || 10,
            table_count: d.table_count || d.tableCount || 1,
            spare_tables: d.spare_tables || d.spareTables || 0,
            deposit: d.deposit || '',
            booking_status: d.status || d.booking_status || d.bookingStatus || 'confirmed',
            remark: d.remarks || d.remark || ''
          }
          const tables = raw.tables || raw.booking_tables || raw.bookingTables || []
          if (tables.length > 0) {
            selectedTables.value = tables.map(bt => ({
              table_id: bt.table_id || bt.tableId,
              table_number: bt.table_number || bt.tableNumber || bt.table_name || bt.tableName || '',
              table_name: bt.table_name || bt.tableName || bt.table_number || bt.tableNumber || ''
            }))
          } else if (d.table_id || d.tableId) {
            selectedTables.value = [{
              table_id: d.table_id || d.tableId,
              table_number: d.table_name || d.tableName || '',
              table_name: d.table_name || d.tableName || ''
            }]
          } else { selectedTables.value = [] }
        } else {
          // API返回非200或无数据，回退使用看板上的预订信息填充
          fillFormFromBooking(b)
        }
      } catch (e) {
        console.error('拉取预订详情失败:', e)
        fillFormFromBooking(b)
      }
    } else {
      fillFormFromBooking(b)
    }
    activeTab.value = 'basic'
    customerHistory.value = []
    loadDishOrders()
  }
})

function fillFormFromBooking(b) {
  if (b) {
    let rawTime = b.time_slot || b.booking_time || b.bookingTime || '18:00:00'
    if (rawTime && rawTime.length === 5) rawTime = rawTime + ':00'
    const hour = parseInt(rawTime.split(':')[0] || '18')
    mealPeriod.value = hour < 15 ? 'lunch' : 'dinner'
    
    const occType = b.banquet_type || b.occasion_type || b.occasionType || 'wedding'
    const validOccasions = occasionOptions.map(o => o.value)
    const finalOccasion = validOccasions.includes(occType) ? occType : 'wedding'
    
    form.value = {
      booking_id: b.order_no || b.id || b.booking_id || b.bookingId || '',
      created_at: b.created_at || b.createdAt || '',
      staff_name: b.created_by || b.staffName || b.staff_name || '',
      booking_date: b.booking_date || b.bookingDate || '',
      booking_time: rawTime,
      customer_name: b.customer_name || b.customerName || '',
      customer_phone: b.customer_phone || b.customerPhone || '',
      occasion_type: finalOccasion,
      guest_per_table: b.guest_per_table || b.guestPerTable || 10,
      table_count: b.table_count || b.tableCount || 1,
      spare_tables: b.spare_tables || b.spareTables || 0,
      deposit: b.deposit || '',
      booking_status: b.status || b.booking_status || b.bookingStatus || 'confirmed',
      remark: b.remarks || b.remark || ''
    }
    const tables = b.booking_tables || b.bookingTables || []
    if (tables.length > 0) {
      selectedTables.value = tables.map(bt => ({
        table_id: bt.table_id || bt.tableId,
        table_number: bt.table_number || bt.tableNumber || bt.table_name || bt.tableName || '',
        table_name: bt.table_name || bt.tableName || bt.table_number || bt.tableNumber || ''
      }))
    } else if (b.table_id || b.tableId) {
      selectedTables.value = [{
        table_id: b.table_id || b.tableId,
        table_number: b.table_name || b.tableName || '',
        table_name: b.table_name || b.tableName || ''
      }]
    } else if (b.tableNames) {
      const names = typeof b.tableNames === 'string' ? b.tableNames.split(',') : b.tableNames
      selectedTables.value = names.map((n, i) => ({
        table_id: (Array.isArray(b.tableIds) ? b.tableIds[i] : null) || null,
        table_number: n.trim(),
        table_name: n.trim()
      }))
    } else { selectedTables.value = [] }
  } else {
    const userStore = useUserStore()
    const currentUserName = userStore.userInfo?.staffName || userStore.userInfo?.name || ''
    mealPeriod.value = 'dinner'
    form.value = {
      booking_id: generateBookingId(),
      created_at: getCurrentDateTime(),
      booking_date: props.date || new Date().toISOString().split('T')[0],
      booking_time: '18:00:00',
      staff_name: currentUserName,
      customer_name: '',
      customer_phone: '',
      occasion_type: 'wedding',
      guest_per_table: 10,
      table_count: 1,
      spare_tables: 0,
      deposit: '',
      booking_status: 'confirmed',
      remark: ''
    }
    if (props.tableIds && props.tableIds.length > 0) {
      // 多选桌台模式
      selectedTables.value = props.tableIds.map((id, i) => ({
        table_id: id,
        table_number: props.tableNames?.[i] || '',
        table_name: props.tableNames?.[i] || ''
      }))
      form.value.table_count = props.tableIds.length
    } else if (props.tableId) {
      selectedTables.value = [{
        table_id: props.tableId,
        table_number: props.tableNumber || props.tableName || '',
        table_name: props.tableName || props.tableNumber || ''
      }]
    } else { selectedTables.value = [] }
  }
}

watch(visible, (v) => emit('update:modelValue', v))

function fmtDateForApi(d) {
  if (!d) return ''
  if (typeof d === 'string' && /^\d{4}-\d{2}-\d{2}/.test(d)) {
    return d.slice(0, 10)
  }
  const date = new Date(d)
  if (isNaN(date.getTime())) return ''
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

watch(() => form.value.booking_date, (newDate, oldDate) => {
  if (!newDate || newDate === oldDate) return
  if (!visible.value || readonly.value) return
  emit('date-change', fmtDateForApi(newDate))
})
watch(() => [form.value.table_count, form.value.spare_tables, form.value.guest_per_table], autoRemark)

watch(() => [props.tableId, props.tableNumber, props.tableName, props.tableIds, props.tableNames], ([id, num, name, ids, names]) => {
  if (!visible.value) return
  if (ids && ids.length > 0) {
    // 多选桌台模式
    selectedTables.value = ids.map((tableId, i) => ({
      table_id: tableId,
      table_number: names?.[i] || '',
      table_name: names?.[i] || ''
    }))
    form.value.table_count = ids.length
  } else if (id) {
    selectedTables.value = [{
      table_id: id,
      table_number: num || name || '',
      table_name: name || num || ''
    }]
  }
})

function onOpened() {
  if (!form.value.booking_date) form.value.booking_date = new Date().toISOString().split('T')[0]
  if (!form.value.booking_time) form.value.booking_time = '18:00:00'
  if (isEdit.value && form.value.booking_id) loadChangeLogs(form.value.booking_id)
}

async function loadChangeLogs(bookingId) {
  try {
    const token = localStorage.getItem('token') || ''
    const res = await fetch('/api/bookings/' + bookingId + '/logs', {
      credentials: 'include',
      headers: { 'Authorization': 'Bearer ' + token }
    })
    const json = await res.json()
    changeLogs.value = json.data?.rows || json.data || []
  } catch { changeLogs.value = [] }
}

// 一键通知文案生成 - 复制到剪贴板
function copyNotification() {
  const date = form.value.booking_date || ''
  const time = form.value.booking_time || ''
  const period = time ? (parseInt(time.split(':')[0]) < 15 ? '午餐' : '晚餐') : ''
  const tables = selectedTables.value.map(t => t.table_number || t.table_name).join('、')
  
  const month = date.split('-')[1]?.replace(/^0/, '') || ''
  const day = date.split('-')[2]?.replace(/^0/, '') || ''
  const dateDisplay = month + '月' + day + '日'
  
  const text = `🎊 亲爱的朋友们，我将于${dateDisplay}${period}在又见炊烟私房菜（宁国店）备好佳肴，恭候光临！


 ▪️ ${tables}

 📱 15905638866（微信同步）

 📞 0563-4626666

 📍 宁国市青龙西路1号（青龙西路与凤形路交汇处，金山维也纳晨堡原成峰幼儿园）


 🗺️ 高德：https://surl.amap.com/krNbdnbp2rD

 🗺️ 百度：https://j.map.baidu.com/88/O97c


 ━━━━━━━━━━━━━

 🏮 又见炊烟 · 宴会预定

 ━━━━━━━━━━━━━

 🏠 2个宴会厅 | 2~20桌灵活接待

 🏠2～22位包厢｜各种台型

 💍 订婚酒 · 🤝 帮忙酒

 🎂 十周岁宴 · 🍼 满月酒

 🏡 乔迁酒 · 🎉 开业酒

 📚 升学宴 · 🎓 谢师宴

 🙏 答谢宴 · 📋 订货会

 👥 年会 · 团建 · 旅行团


 🍶 订喜酒即赠古井贡酒，喜上加喜！

 ☎欢迎致电咨询。

 365天营业，随时恭候 ❤️`
  
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('通知文案已复制到剪贴板！')
  }).catch(() => {
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('通知文案已复制！')
  })
}

function formatLogTime(t) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

defineExpose({ updateSelectedTables, revertDate, revertPeriod, getCurrentPeriod })
</script>

<style>
.bk-dlg .el-dialog {
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 40px 80px -20px rgba(45, 74, 62, 0.25);
  animation: bkModalPop 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
}
@keyframes bkModalPop {
  from {
    opacity: 0;
    transform: scale(0.92) translateY(20px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}
.bk-dlg .el-overlay {
  backdrop-filter: blur(6px);
  background: rgba(0, 0, 0, 0.35);
}
.bk-dlg .el-dialog__header {
  padding: 0;
  margin: 0;
}
.bk-dlg .el-dialog__headerbtn { display: none; }
.bk-dlg .el-dialog__body {
  padding: 0;
  background: oklch(1 0.004 95);
}
.bk-dlg .el-dialog__footer {
  padding: 0;
  background: transparent;
}

/* ============ 头部 ============ */
.bk-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: oklch(0.38 0.055 160);
  padding: 14px 18px;
  color: oklch(0.98 0.01 95);
}
.bk-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-header-icon {
  width: 34px;
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  background: oklch(0.98 0.01 95 / 0.1);
  box-shadow: 0 0 0 1px oklch(0.98 0.01 95 / 0.15) inset;
  font-size: 16px;
}
.bk-header-title {
  font-size: 16px;
  font-weight: 600;
  line-height: 1.2;
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}
.bk-header-sub {
  font-size: 11px;
  color: oklch(0.98 0.01 95 / 0.7);
  margin: 2px 0 0;
}
.bk-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
.bk-status-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  background: oklch(0.75 0.11 75);
  color: oklch(0.28 0.03 70);
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 11px;
  font-weight: 600;
}
.bk-close-btn {
  background: transparent;
  border: none;
  color: oklch(0.98 0.01 95 / 0.8);
  cursor: pointer;
  padding: 3px;
  border-radius: 0.375rem;
  font-size: 16px;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}
.bk-close-btn:hover {
  background: oklch(0.98 0.01 95 / 0.1);
  color: oklch(0.98 0.01 95);
}
.bk-header-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.25);
  color: #fff;
  padding: 6px 14px;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.bk-header-edit-btn:hover {
  background: rgba(255, 255, 255, 0.25);
  border-color: rgba(255, 255, 255, 0.4);
}

/* ============ 标签页 ============ */
.bk-tabs {
  display: flex;
  gap: 2px;
  border-bottom: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.4);
  padding: 0 12px;
}
.bk-tab {
  position: relative;
  padding: 9px 10px;
  background: transparent;
  border: none;
  cursor: pointer;
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.52 0.02 150);
  transition: color 0.2s;
}
.bk-tab:hover { color: oklch(0.24 0.02 160); }
.bk-tab.active { color: oklch(0.38 0.055 160); }
.bk-tab-en {
  margin-left: 3px;
  font-size: 11px;
  color: oklch(0.52 0.02 150);
}
.bk-tab.active .bk-tab-en { color: oklch(0.52 0.02 150); }
.bk-tab.active::after {
  content: '';
  position: absolute;
  left: 8px;
  right: 8px;
  bottom: 0;
  height: 2px;
  border-radius: 9999px;
  background: oklch(0.38 0.055 160);
}

/* ============ 主体 ============ */
.bk-body {
  max-height: 65vh;
  overflow-y: auto;
  padding: 16px 18px;
}
.bk-blocks {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.bk-block {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.bk-block-title {
  display: flex;
  align-items: center;
  gap: 6px;
  color: oklch(0.38 0.055 160);
}
.bk-block-title h3 {
  font-size: 12px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  margin: 0;
}
.bk-title-en {
  font-weight: 400;
  color: oklch(0.52 0.02 150);
  font-size: 11px;
}
.bk-block-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.bk-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.bk-grid-4 {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 8px;
}

/* ============ 字段 ============ */
.bk-field {
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.bk-label {
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
}
.bk-label-en {
  font-size: 11px;
  font-weight: 400;
  color: oklch(0.52 0.02 150);
}
.bk-req {
  color: oklch(0.75 0.11 75);
  margin-left: 2px;
}
.bk-input {
  width: 100%;
}
.bk-input .el-input__wrapper,
.bk-input .el-select .el-input__wrapper,
.bk-field .el-input__wrapper,
.bk-field .el-select .el-input__wrapper,
.bk-field .el-autocomplete .el-input__wrapper {
  border-radius: 0.375rem !important;
  box-shadow: 0 0 0 1px oklch(0.9 0.012 120) inset !important;
  background: oklch(1 0.004 95) !important;
}
.bk-input .el-input__wrapper:hover,
.bk-field .el-input__wrapper:hover,
.bk-field .el-select .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px oklch(0.75 0.11 75) inset !important;
}
.bk-input .el-input__wrapper.is-focus,
.bk-field .el-input__wrapper.is-focus,
.bk-field .el-select .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px oklch(0.38 0.055 160) inset !important;
}
.bk-field .el-input__inner,
.bk-field .el-select .el-input__inner {
  font-size: 12px;
  height: 32px;
  color: oklch(0.24 0.02 160);
}
.bk-field .el-textarea__inner {
  border-radius: 0.375rem;
  font-size: 12px;
  border-color: oklch(0.9 0.012 120);
  color: oklch(0.24 0.02 160);
  background: oklch(1 0.004 95);
  line-height: 1.5;
  min-height: 60px;
}

/* ============ 餐别切换 ============ */
.bk-meal-switch {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 4px;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.5);
  border-radius: 0.5rem;
  padding: 4px;
}
.bk-meal-btn {
  padding: 8px 12px;
  border: none;
  background: transparent;
  border-radius: 0.375rem;
  font-size: 14px;
  font-weight: 500;
  color: oklch(0.52 0.02 150);
  cursor: pointer;
  transition: all 0.2s;
}
.bk-meal-btn:hover { color: oklch(0.24 0.02 160); }
.bk-meal-btn.active {
  background: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.bk-meal-btn:disabled { cursor: not-allowed; opacity: 0.6; }

/* ============ 统计卡片 ============ */
.bk-stat {
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  border-radius: 0.5rem;
  padding: 10px 14px;
  text-align: center;
}
.bk-stat-label {
  font-size: 11px;
  font-weight: 500;
  letter-spacing: 0.05em;
  color: oklch(0.52 0.02 150);
  margin: 0;
  text-transform: uppercase;
}
.bk-stat-value {
  font-size: 20px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  margin: 4px 0 0;
  font-family: 'Noto Serif SC', serif;
}
.bk-stat-highlight {
  border-color: oklch(0.38 0.055 160 / 0.2);
  background: oklch(0.38 0.055 160 / 0.05);
}
.bk-stat-highlight .bk-stat-label { color: oklch(0.38 0.055 160 / 0.7); }
.bk-stat-highlight .bk-stat-value { color: oklch(0.38 0.055 160); }

.bk-stat-inputs {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

/* ============ 已选桌台 ============ */
.bk-order-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  border-radius: 0.5rem;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
  cursor: pointer;
  transition: background 0.2s;
}
.bk-order-btn:hover { background: oklch(0.955 0.012 120); }
.bk-order-btn:disabled { cursor: not-allowed; opacity: 0.6; }
.bk-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.3);
  border-radius: 0.75rem;
  padding: 12px;
}
.bk-chip {
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  border-radius: 0.375rem;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.bk-chip-primary {
  background: linear-gradient(135deg, #ef4444, #dc2626);
  border-color: #dc2626;
  color: #fff;
  font-weight: 600;
}
.bk-chip-secondary {
  background: linear-gradient(135deg, #fbbf24, #f59e0b);
  border-color: #f59e0b;
  color: #78350f;
  font-weight: 600;
}
.bk-empty {
  color: oklch(0.52 0.02 150);
  font-size: 13px;
}

/* ============ 信息条 ============ */
.bk-info-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  background: oklch(0.955 0.012 120 / 0.4);
  border-radius: 0.5rem;
  padding: 12px 16px;
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.bk-info-val {
  font-weight: 500;
  color: oklch(0.24 0.02 160);
}

/* ============ 列表 ============ */
.bk-tab-body { padding: 24px; }
.bk-list { max-height: 400px; overflow-y: auto; }
.bk-list-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 10px 12px;
  font-size: 13px;
  color: oklch(0.24 0.02 160);
  border-bottom: 1px dashed oklch(0.9 0.012 120);
}
.bk-list-item:hover { background: oklch(0.955 0.012 120); }
.bk-list-date { color: oklch(0.34 0.04 160); min-width: 90px; }
.bk-list-status { font-size: 11px; padding: 2px 10px; border-radius: 9999px; font-weight: 500; }
.bk-list-status.pending { background: oklch(0.955 0.012 120); color: oklch(0.34 0.04 160); }
.bk-list-status.confirmed { background: oklch(0.955 0.012 120); color: oklch(0.38 0.055 160); }
.bk-list-status.completed { background: oklch(0.955 0.012 120); color: oklch(0.38 0.055 160); }
.bk-list-status.cancelled { background: oklch(0.955 0.012 120); color: oklch(0.577 0.19 27.325); }
.bk-list-amount { margin-left: auto; color: oklch(0.75 0.11 75); font-weight: 600; }
.bk-list-total { text-align: right; padding: 12px; font-weight: 600; color: oklch(0.75 0.11 75); font-size: 14px; border-top: 1px solid oklch(0.9 0.012 120); }
.bk-log { flex-direction: column; align-items: flex-start; gap: 6px; padding: 12px; border-left: 3px solid oklch(0.75 0.11 75); }
.bk-log-head { display: flex; gap: 14px; font-size: 12px; color: oklch(0.52 0.02 150); }
.bk-log-op { color: oklch(0.24 0.02 160); font-weight: 500; }
.bk-log-detail { font-size: 13px; color: oklch(0.34 0.04 160); line-height: 1.5; }

/* ============ 底部 ============ */
.bk-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  border-top: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.3);
  padding: 16px 24px;
}
.bk-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 0.5rem;
  padding: 10px 20px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid transparent;
}
.bk-btn:disabled { cursor: not-allowed; opacity: 0.6; }
.bk-btn-default {
  border-color: oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.24 0.02 160);
}
.bk-btn-default:hover {
  background: oklch(0.955 0.012 120);
  transform: translateY(-1px);
}
.bk-btn-primary {
  background: oklch(0.38 0.055 160);
  border-color: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  font-weight: 600;
  box-shadow: 0 6px 14px rgba(45, 74, 62, 0.15);
}
.bk-btn-primary:hover {
  background: oklch(0.34 0.04 160);
  border-color: oklch(0.34 0.04 160);
  transform: scale(1.03) translateY(-2px);
  box-shadow: 0 12px 24px rgba(45, 74, 62, 0.2);
}
.bk-btn-primary:active {
  transform: scale(0.97);
}

/* ============ 横版三行三列布局 ============ */
.bk-basic {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

/* 顶部信息条 */
.bk-info-top {
  margin: 0 !important;
  border-radius: 0.5rem;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.4);
  padding: 10px 16px;
}
.bk-info-top .bk-info-item {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bk-info-top .bk-info-label {
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.bk-info-top .bk-info-val {
  font-size: 12px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
}
.bk-info-top .bk-info-sep {
  width: 1px;
  height: 14px;
  background: oklch(0.9 0.012 120);
}

/* 三行三列表单网格 */
.bk-form-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 16px;
}

/* 块间距压缩 */
.bk-basic .bk-block {
  padding: 10px 14px;
  margin: 0;
}
.bk-basic .bk-block-title {
  margin-bottom: 6px;
}
.bk-basic .bk-block-title h3 {
  font-size: 13px;
}

/* 桌台配置：标题+统计一行 */
.bk-block-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}
.bk-stats-inline {
  display: flex;
  align-items: center;
  gap: 6px;
}
.bk-stat-mini {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 2px 8px;
  border-radius: 0.375rem;
  background: oklch(0.955 0.012 120 / 0.5);
}
.bk-stat-mini-label {
  font-size: 11px;
  color: oklch(0.52 0.02 150);
}
.bk-stat-mini-val {
  font-size: 14px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  font-family: 'Noto Serif SC', serif;
}
.bk-stat-mini-highlight {
  background: oklch(0.38 0.055 160 / 0.1);
}
.bk-stat-mini-highlight .bk-stat-mini-label { color: oklch(0.38 0.055 160 / 0.7); }
.bk-stat-mini-highlight .bk-stat-mini-val { color: oklch(0.38 0.055 160); }
.bk-stat-divider {
  font-size: 12px;
  color: oklch(0.52 0.02 150);
  font-weight: 600;
}

/* 数量输入框一行 */
.bk-stat-inputs-row {
  margin-top: 8px;
  gap: 12px;
}
.bk-field-inline {
  display: flex;
  align-items: center;
  gap: 8px;
}
.bk-label-inline {
  font-size: 12px;
  color: oklch(0.34 0.04 160);
  white-space: nowrap;
  width: auto;
  margin-bottom: 0;
}

/* 标签文字缩小 */
.bk-label {
  font-size: 12px;
}

/* 输入框高度缩小 */
.bk-input .el-input__wrapper,
.bk-input .el-select__wrapper,
.bk-input .el-date-editor.el-input__wrapper {
  padding: 0 12px !important;
  min-height: 34px !important;
}
.bk-input .el-input__inner {
  height: 34px;
  font-size: 13px;
}
.bk-input .el-input-number {
  width: 100%;
}
.bk-input .el-input-number .el-input__wrapper {
  height: 34px;
  min-height: 34px;
}
.bk-input textarea.el-input__inner {
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.5;
}

/* 底部body padding */
.bk-body {
  padding: 14px 18px;
}

/* ============ 打印按钮组 ============ */
.bk-footer-print {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}
.bk-btn-print {
  background: oklch(0.55 0.12 250);
  border-color: oklch(0.55 0.12 250);
  color: oklch(0.98 0.01 95);
  font-weight: 500;
  padding: 8px 16px;
  font-size: 13px;
}
.bk-btn-print:hover {
  background: oklch(0.50 0.14 250);
  border-color: oklch(0.50 0.14 250);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(59, 130, 246, 0.15);
}
.bk-btn-print:active {
  transform: scale(0.97);
}

/* 通知文案按钮 */
.bk-footer-notify {
  display: inline-flex;
  align-items: center;
}
.bk-btn-notify {
  background: oklch(0.62 0.18 120);
  border-color: oklch(0.62 0.18 120);
  color: oklch(0.98 0.01 95);
  font-weight: 500;
  padding: 8px 16px;
  font-size: 13px;
}
.bk-btn-notify:hover {
  background: oklch(0.58 0.20 120);
  border-color: oklch(0.58 0.20 120);
  transform: translateY(-1px);
  box-shadow: 0 4px 10px rgba(34, 197, 94, 0.15);
}
.bk-btn-notify:active {
  transform: scale(0.97);
}
</style>

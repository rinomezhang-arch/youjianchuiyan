<template>
  <div class="guest-order">
    <!-- 左侧分类栏 -->
    <aside class="category-sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">{{ tableNames || '订单加菜' }}</span>
        <span class="sidebar-sub">加菜 · Guest Order</span>
      </div>
      <div class="category-list">
        <button
          v-for="cat in categories"
          :key="cat.category_id || cat"
          :class="['cat-item', { active: activeCat === (cat.category_id || cat) }]"
          @click="selectCategory(cat)"
        >
          <span class="cat-name">{{ cat.dish_category || cat }}</span>
          <span class="cat-count" v-if="cat.count !== undefined">{{ cat.count }}</span>
        </button>
      </div>
      <div class="sidebar-footer">
        <div class="room-total" v-if="orderDetail.total_amount != null">
          <span>已点菜金额（不含优惠与付款）</span>
          <span class="room-total-price">¥{{ formatMoney(orderDetail.total_amount) }}</span>
        </div>
      </div>
    </aside>

    <!-- 右侧主区域 -->
    <div class="content-area">
      <!-- 顶部信息栏 -->
      <div class="content-header">
        <div class="header-left">
          <h2 class="content-title">{{ currentCatName }}</h2>
          <span class="content-sub">{{ currentCatNameEn }}</span>
        </div>
        <div class="header-right">
          <div class="dish-count-info" v-if="orderedDishes.length">
            <span class="info-dot"></span>
            已点 {{ orderedDishes.length }} 道菜
          </div>
          <div class="search-box">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
            <input v-model="searchKeyword" :disabled="menuLoading" placeholder="搜索菜品 · Search" @input="onSearch" />
          </div>
        </div>
      </div>

      <!-- 菜品网格 -->
      <div class="order-status" role="status">
        <span>{{ notice || (orderReady ? '订单已核对，可选择加菜' : '请服务员授权查看订单') }}</span>
        <span v-if="journalError">{{ journalError }}</span>
        <button :disabled="authLoading || orderLoading" @click="openView">{{ orderLoading ? '读取中...' : '授权查看订单' }}</button>
        <button v-if="viewToken && !orderReady" :disabled="authLoading || orderLoading" @click="loadOrderDetail()">重新读取订单</button>
        <button v-if="journal?.state === 'pending'" :disabled="!orderReady || orderLoading || authLoading || !!journalError" @click="openSubmit">核对原批次</button>
      </div>
      <div v-if="orderReady && orderedDishes.length" class="order-status ordered-lines">
        <span v-for="line in orderedDishes" :key="line.dish_booking_id">{{ line.dish_name }} × {{ line.dish_quantity }} · ¥{{ formatMoney(line.subtotal) }}</span>
      </div>
      <div class="dish-grid" v-loading="menuLoading || searchLoading" :aria-busy="menuLoading || searchLoading">
        <div
          v-for="dish in displayDishes"
          :key="dish.dish_id || dish.id"
          class="dish-card"
          @click="showDishDetail(dish)"
        >
          <div class="dish-img" :class="['cat-color-' + catColorIndex(dish.dish_category)]">
            <img v-if="dish.cover_img || dish.image_url" :src="dish.cover_img || dish.image_url" :alt="dish.dish_name" @error="onImgError" />
            <div v-else class="dish-img-fallback">
              <span class="fallback-name">{{ dish.dish_name }}</span>
              <span class="fallback-cat">{{ dish.dish_category }}</span>
            </div>
          </div>
          <div class="dish-info">
            <div class="dish-name">{{ dish.dish_name }}</div>
            <div class="dish-tags">
              <span v-if="dish.spicy_level >= 1" class="tag spicy">{{ '🌶'.repeat(dish.spicy_level) }}</span>
            </div>
            <div class="dish-price">¥{{ formatMoney(dish.sale_price) }}</div>
          </div>
          <button class="quick-add" :disabled="!canEditCart" @click.stop="addToCart(dish)">+</button>
        </div>
      </div>
    </div>

    <!-- 右下角购物车浮动按钮 -->
    <div class="cart-fab" @click="showCart = true">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
      <span v-if="cartCount > 0" class="cart-badge">{{ cartCount }}</span>
      <span v-if="cartCount > 0" class="cart-total">¥{{ cartTotal }}</span>
    </div>

    <!-- 购物车侧滑面板 -->
    <Transition name="cart-slide">
      <div v-if="showCart" class="cart-panel">
        <div class="cart-header">
          <h3>已点加菜 · Added</h3>
          <button class="cart-close" @click="showCart = false">×</button>
        </div>
        <p v-if="journal || journalError || submitted" class="cart-recovery" role="status">{{ notice }} {{ journalError }}</p>
        <div class="cart-body" v-if="cart.length">
          <div v-for="item in cart" :key="item.dish_id" class="cart-item">
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.dish_name }}</div>
              <div class="cart-item-price">¥{{ formatMoney(item.sale_price ?? item.unit_price) }}</div>
            </div>
            <div class="cart-item-qty">
              <button :disabled="!canEditCart" @click="changeQty(item, -1)">−</button>
              <span>{{ item.qty }}</span>
              <button :disabled="!canEditCart || item.qty >= 99" @click="changeQty(item, 1)">+</button>
            </div>
          </div>
        </div>
        <div v-else class="cart-empty">
          <p>还没有加菜</p>
          <p class="cart-empty-en">No dishes added yet</p>
        </div>
        <div class="cart-footer" v-if="cart.length">
          <div class="cart-summary">
            <span>加菜合计 · Subtotal</span>
            <span class="cart-summary-price">¥{{ cartTotal }}</span>
          </div>
          <div class="cart-actions">
            <button class="btn-clear" :disabled="!canEditCart" @click="cart = []">清空</button>
            <button class="btn-submit" :disabled="!orderReady || orderLoading || authLoading || !!journalError || journal?.state === 'committed' || (!journal && !orderWritable)" @click="openSubmit">{{ journal ? '核对原批次' : '服务员授权提交' }}</button>
          </div>
        </div>
      </div>
    </Transition>
    <Transition name="fade">
      <div v-if="showCart" class="cart-overlay" @click="showCart = false"></div>
    </Transition>

    <!-- 菜品详情弹窗（点击菜品卡片查看） -->
    <Transition name="modal">
      <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
        <div class="detail-modal">
          <div class="detail-img" :class="['cat-color-' + catColorIndex(detailDish?.dish_category)]">
            <img v-if="detailDish?.cover_img || detailDish?.image_url" :src="detailDish.cover_img || detailDish.image_url" @error="onImgError" />
            <div v-else class="detail-img-fallback">
              <span class="fallback-name">{{ detailDish?.dish_name }}</span>
              <span class="fallback-cat">{{ detailDish?.dish_category }}</span>
            </div>
          </div>
          <div class="detail-info">
            <h3>{{ detailDish?.dish_name }}</h3>
            <p class="detail-category">{{ detailDish?.dish_category }}</p>
            <div class="detail-tags">
              <span v-if="detailDish?.spicy_level" class="tag spicy">{{ '🌶'.repeat(detailDish.spicy_level) }}</span>
            </div>
            <p v-if="detailDish?.main_ingredients || detailDish?.main_ingredient" class="detail-ingredients">
              食材：{{ detailDish?.main_ingredients || detailDish?.main_ingredient }}
            </p>
            <div class="detail-price">¥{{ formatMoney(detailDish?.sale_price) }}</div>
            <div class="detail-qty">
              <button :disabled="!canEditCart" @click="detailQty = Math.max(1, detailQty - 1)">−</button>
              <span>{{ detailQty }}</span>
              <button :disabled="!canEditCart || detailQty >= 99" @click="detailQty++">+</button>
            </div>
            <button class="detail-add-btn" :disabled="!canEditCart" @click="addFromDetail">加入加菜 · Add</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 服务员授权弹窗 -->
    <Transition name="modal">
      <div v-if="showAuth" class="modal-overlay auth-overlay" @click.self="closeAuth">
        <div class="auth-box">
          <div class="auth-header">
            <h3>{{ authMode === 'view' ? '授权查看订单' : journal ? '授权核对原批次' : '授权加菜' }}</h3>
            <button class="auth-close" :disabled="authLoading" @click="closeAuth">×</button>
          </div>
          <p class="auth-hint">{{ authMode === 'view' ? '请服务员输入账号密码，查看当前订单' : journal ? '仅核对并重放原批次，不另建加菜请求' : '请服务员确认本次加菜' }}</p>
          <div class="auth-form">
            <div class="form-item">
              <label>服务员账号 · Staff ID</label>
              <input v-model="authForm.username" placeholder="请输入服务员账号" @keyup.enter="$refs.pwd.focus()" />
            </div>
            <div class="form-item">
              <label>密码 · Password</label>
              <input ref="pwd" v-model="authForm.password" type="password" placeholder="请输入密码"
                     @keyup.enter="handleAuth" />
            </div>
          </div>
          <div v-if="authError" class="auth-error">{{ authError }}</div>
          <div class="auth-actions">
            <button class="btn-cancel" :disabled="authLoading" @click="closeAuth">取消</button>
            <button class="btn-confirm" :disabled="authLoading" @click="handleAuth">
              {{ authLoading ? '处理中...' : authMode === 'view' ? '授权查看' : journal ? '授权核对' : '授权并提交' }}
            </button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 成功提示 -->
    <Transition name="fade">
      <div v-if="successMsg" class="success-toast">{{ successMsg }}</div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ipadDishList, ipadDishSearch, ipadOrderDetail, ipadAuthVerify, ipadOrderAddDishes, ipadOrderViewAuthorize } from '@/api/ipad'
import { useIpadStore } from '@/store/ipad'
import { createJournal, readJournal, saveJournal, clearJournal, assertReceipt, receiptRowsMatch, batchFailureMessage } from '@/utils/guestOrderJournal'
import { ElMessage } from 'element-plus'

const route = useRoute()
const ipad = useIpadStore()
const bookingId = computed(() => String(route.params.bookingId || ''))
const scope = () => ({ store_id: String(ipad.storeId), device_sn: ipad.deviceSn, booking_id: bookingId.value })
let scopeGeneration = 0
let viewTimer
const viewToken = ref('')
let viewExpires = 0
const orderReady = ref(false)
const orderLoading = ref(false)
const notice = ref('')
const journal = ref(null)
const journalError = ref('')
const submitted = ref(false)
const receiptNeedsReview = ref(false)
const authMode = ref('view')
const orderWritable = computed(() => orderReady.value && orderDetail.value.payment_status !== 'paid'
  && !['completed', 'cancelled'].includes(orderDetail.value.booking_status))
const canEditCart = computed(() => orderWritable.value && !journal.value && !journalError.value && !authLoading.value && !submitted.value)

// 订单详情
const orderDetail = ref({})
const orderedDishes = ref([])
const tableNames = computed(() => (orderDetail.value.tables || []).map(table => table.table_name || `桌台 ${table.table_id}`).join('、'))

// 菜品
const categories = ref([])
const activeCat = ref('all')
const allDishes = ref([])
const dishes = ref([])
const searchKeyword = ref('')
const menuLoading = ref(true)
const searchLoading = ref(false)
let searchGeneration = 0

const currentCatName = computed(() => {
  if (activeCat.value === 'all') return '全部菜品'
  const cat = categories.value.find(c => (c.category_id || c.dish_category || c) === activeCat.value)
  return cat?.dish_category || cat || '全部菜品'
})

const currentCatNameEn = computed(() => {
  if (activeCat.value === 'all') return 'All Dishes'
  const cat = categories.value.find(c => (c.category_id || c.dish_category || c) === activeCat.value)
  return cat?.dish_category_en || cat?.english_name || 'Current'
})

const displayDishes = computed(() => {
  if (searchKeyword.value) {
    return dishes.value.filter(d => d.dish_name?.includes(searchKeyword.value))
  }
  return dishes.value
})

// 购物车
const cart = ref([])
const showCart = ref(false)
const cartCount = computed(() => cart.value.reduce((s, i) => s + i.qty, 0))
// Convert decimal prices to integer cents before arithmetic; never multiply binary floats.
function moneyCents(value) {
  const match = String(value ?? '').trim().match(/^([+-]?)(\d+)(?:\.(\d+))?$/)
  if (!match) return null
  const fraction = match[3] || ''
  const cents = BigInt(match[2]) * 100n + BigInt((fraction + '00').slice(0, 2))
    + (Number(fraction[2] || 0) >= 5 ? 1n : 0n)
  return match[1] === '-' ? -cents : cents
}

function formatCents(cents) {
  if (cents === null) return '—'
  const absolute = cents < 0n ? -cents : cents
  return `${cents < 0n ? '-' : ''}${absolute / 100n}.${String(absolute % 100n).padStart(2, '0')}`
}

function formatMoney(value) {
  return formatCents(moneyCents(value))
}

const cartTotal = computed(() => {
  let total = 0n
  for (const item of cart.value) {
    const cents = moneyCents(item.sale_price ?? item.unit_price)
    if (cents === null || !Number.isSafeInteger(item.qty)) return '—'
    total += cents * BigInt(item.qty)
  }
  return formatCents(total)
})

// 菜品详情
const showDetail = ref(false)
const detailDish = ref(null)
const detailQty = ref(1)

// 授权
const showAuth = ref(false)
const authForm = ref({ username: '', password: '' })
const authError = ref('')
const authLoading = ref(false)

// 成功提示
const successMsg = ref('')

function selectCategory(cat) {
  searchGeneration++
  searchLoading.value = false
  activeCat.value = cat.category_id || cat.dish_category || cat
  loadDishes()
}

// 按分类名hash生成颜色索引（0-17），每个分类一种渐变色
const CAT_COLOR_COUNT = 18
function catColorIndex(catName) {
  if (!catName) return 0
  let hash = 0
  for (let i = 0; i < catName.length; i++) {
    hash = ((hash << 5) - hash) + catName.charCodeAt(i)
    hash |= 0
  }
  return Math.abs(hash) % CAT_COLOR_COUNT
}

// 图片加载失败时隐藏img，显示fallback
function onImgError(e) {
  e.target.style.display = 'none'
}

function showDishDetail(dish) {
  detailDish.value = dish
  detailQty.value = 1
  showDetail.value = true
}

function addFromDetail() {
  if (!canEditCart.value) return
  addToCart(detailDish.value, detailQty.value)
  showDetail.value = false
  ElMessage.success(`已加入 ${detailQty.value} 份`)
}

function addToCart(dish, qty = 1) {
  if (!canEditCart.value) return
  const existing = cart.value.find(i => i.dish_id === (dish.dish_id || dish.id))
  if (existing) {
    if (existing.qty + qty > 99) { ElMessage.warning('每道菜限99份'); return }
    existing.qty += qty
  } else {
    cart.value.push({
      dish_id: dish.dish_id || dish.id,
      dish_name: dish.dish_name,
      sale_price: dish.sale_price,
      unit_price: dish.sale_price,
      qty: qty
    })
  }
}

function changeQty(item, delta) {
  if (!canEditCart.value || item.qty + delta > 99) return
  item.qty += delta
  if (item.qty <= 0) {
    cart.value = cart.value.filter(i => i !== item)
  }
}

async function onSearch() {
  const generation = ++searchGeneration
  const keyword = searchKeyword.value
  if (!keyword) { searchLoading.value = false; loadDishes(); return }
  searchLoading.value = true
  try {
    const res = await ipadDishSearch(keyword)
    if (generation !== searchGeneration) return
    if (res.code === 200) dishes.value = res.data || []
  } catch {
    if (generation !== searchGeneration) return
    dishes.value = allDishes.value.filter(d => d.dish_name?.includes(keyword))
  } finally {
    if (generation === searchGeneration) searchLoading.value = false
  }
}

function current(generation) { return generation === scopeGeneration }
function errorMessage(error, fallback) { return error?.response?.data?.message || error?.message || fallback }
function expireView() {
  clearTimeout(viewTimer)
  viewToken.value = ''; viewExpires = 0; orderReady.value = false
  orderDetail.value = {}; orderedDishes.value = []
}
function closeAuth() { if (!authLoading.value) { showAuth.value = false; authForm.value.password = '' } }
function openView() {
  if (authLoading.value || orderLoading.value) return
  authMode.value = 'view'; authError.value = ''; authForm.value.password = ''; showAuth.value = true
}
async function openSubmit() {
  if (!orderReady.value || orderLoading.value || authLoading.value || journalError.value || submitted.value
    || journal.value?.state === 'committed' || (!journal.value && (!orderWritable.value || !cart.value.length))) return
  if (journal.value?.state === 'pending') {
    const generation = scopeGeneration
    await loadOrderDetail(generation)
    if (!current(generation) || !orderReady.value || !journal.value || receiptNeedsReview.value || !orderWritable.value) return
  }
  authMode.value = 'add'; authError.value = ''; authForm.value.password = ''; showAuth.value = true
}
function restoreCart() {
  if (journal.value?.state !== 'pending') return
  cart.value = journal.value.dishes.map(row => {
    const dish = allDishes.value.find(d => String(d.dish_id || d.id) === row.dish_id)
    return { dish_id: row.dish_id, dish_name: dish?.dish_name || `菜品 ${row.dish_id}`, sale_price: dish?.sale_price ?? null, qty: row.dish_quantity }
  })
}
function initializeScope() {
  scopeGeneration++; searchGeneration++; searchLoading.value = false
  expireView(); orderLoading.value = false; authLoading.value = false
  cart.value = []; journal.value = null; journalError.value = ''; submitted.value = false; receiptNeedsReview.value = false
  showCart.value = false; showDetail.value = false; successMsg.value = ''
  authForm.value = { username: '', password: '' }; authError.value = ''
  notice.value = '请服务员授权查看订单'
  try {
    journal.value = readJournal(localStorage, scope())
    if (journal.value) {
      submitted.value = journal.value.state === 'committed'
      notice.value = submitted.value ? '该批加菜已提交，请授权重新读取订单' : '已恢复待核对批次，请先授权查看订单，再重新授权核对原批次'
      restoreCart()
    }
  } catch (error) { journalError.value = errorMessage(error, '无法读取本机记录，已锁定提交') }
  authMode.value = 'view'; showAuth.value = true
  allDishes.value = []; dishes.value = []; categories.value = []; searchKeyword.value = ''; activeCat.value = 'all'
  refreshMenu(scopeGeneration)
}

async function refreshMenu(generation) {
  await loadAllDishes(generation)
  if (!current(generation)) return
  await loadCategories(); await loadDishes(); restoreCart()
}

async function handleAuth() {
  if (authLoading.value) return
  if (!authForm.value.username || !authForm.value.password) { authError.value = '请输入账号和密码'; return }
  const generation = scopeGeneration, target = scope(), mode = authMode.value
  if (mode === 'add' && (!orderWritable.value || orderLoading.value || journalError.value || submitted.value || receiptNeedsReview.value)) return
  authLoading.value = true; authError.value = ''
  const credentials = { username: authForm.value.username, password: authForm.value.password, booking_id: target.booking_id }
  try {
    if (mode === 'view') {
      expireView()
      const res = await ipadOrderViewAuthorize(credentials, target)
      if (!current(generation)) return
      const data = res.data
      if (res.code !== 200) throw new Error(res.message || '查看授权失败')
      if (data?.booking_id !== target.booking_id || data.purpose !== 'ipad:order-view'
        || typeof data.order_view_token !== 'string' || !/^[A-Za-z0-9_-]{43}$/.test(data.order_view_token)
        || !Number.isInteger(data.expires_in) || data.expires_in <= 0 || data.expires_in > 1800) throw new Error('查看授权回执不匹配')
      expireView(); viewToken.value = data.order_view_token; viewExpires = Date.now() + data.expires_in * 1000
      viewTimer = setTimeout(() => { if (current(generation)) { expireView(); notice.value = '查看授权已过期，请服务员重新授权' } }, data.expires_in * 1000)
      showAuth.value = false
      await loadOrderDetail(generation)
    } else {
      // Persist the immutable logical submission before either authorization or delivery.
      if (!journal.value) {
        const existing = readJournal(localStorage, target)
        if (existing) { journal.value = existing; restoreCart(); throw new Error('发现待核对批次，请重新查看后核对') }
        const entry = createJournal(target, cart.value.map(row => ({ dish_id: String(row.dish_id), dish_quantity: row.qty })))
        try { saveJournal(localStorage, entry) } catch { journalError.value = '无法保存加菜记录，未发送请求'; throw new Error(journalError.value) }
        journal.value = entry
      }
      const entry = readJournal(localStorage, target)
      if (!entry || entry.client_request_id !== journal.value.client_request_id || entry.state !== 'pending') throw new Error('本机加菜记录已变化，请刷新后核对')
      const res = await ipadAuthVerify(credentials, target)
      if (!current(generation)) return
      const data = res.data
      if (res.code !== 200) throw new Error(res.message || '加菜授权失败')
      if (data?.booking_id !== target.booking_id || data.purpose !== 'ipad:batch-add' || typeof data.authorization_token !== 'string' || !data.authorization_token) throw new Error('加菜授权回执不匹配')
      if (!orderWritable.value) throw new Error('订单不可加菜，请先重新查看订单')
      showAuth.value = false
      await submitAddDishes(entry, data.authorization_token, generation)
    }
  } catch (error) {
    if (current(generation)) { authError.value = errorMessage(error, '授权失败，请重试'); notice.value = authError.value }
  } finally {
    credentials.password = ''
    if (current(generation)) { authLoading.value = false; authForm.value.password = '' }
  }
}

async function submitAddDishes(entry, token, generation) {
  notice.value = '正在核对本批加菜，请勿重复操作'
  try {
    const res = await ipadOrderAddDishes({ booking_id: entry.scope.booking_id, client_request_id: entry.client_request_id,
      authorization_token: token, dishes: entry.dishes }, entry.scope)
    if (!current(generation)) return
    if (res.code !== 200) { notice.value = batchFailureMessage(res, entry); return }
    const receipt = assertReceipt(res.data, entry)
    acceptCommitted(entry, receipt)
    await loadOrderDetail(generation)
  } catch (error) {
    if (current(generation)) notice.value = batchFailureMessage(error?.response?.data, entry)
  }
}

function acceptCommitted(entry, receipt) {
  const saved = readJournal(localStorage, entry.scope)
  if (!saved || saved.client_request_id !== entry.client_request_id || JSON.stringify(saved.dishes) !== JSON.stringify(entry.dishes)) {
    journalError.value = '本机批次记录已变化，请保留记录并联系服务员核对'
    throw new Error(journalError.value)
  }
  submitted.value = true; receiptNeedsReview.value = false; cart.value = []; showCart.value = false
  journal.value = { ...entry, state: 'committed' }
  try {
    saveJournal(localStorage, journal.value)
    clearJournal(localStorage, journal.value); journal.value = null
  } catch { journalError.value = '该批已提交，本机记录未清理；请保留页面并联系管理员核对' }
  notice.value = `该批已提交，${receipt.added_dishes}道菜，菜金额¥${formatMoney(receipt.added_amount)}；正在重新读取订单`
}

async function loadOrderDetail(generation = scopeGeneration) {
  if (orderLoading.value || !current(generation)) return
  if (!viewToken.value || Date.now() >= viewExpires) { expireView(); notice.value = submitted.value ? '该批已提交，请重新授权读取订单' : '请重新授权查看订单'; return }
  const target = scope(), token = viewToken.value, entry = journal.value?.state === 'pending' ? journal.value : null
  orderLoading.value = true; orderReady.value = false
  orderDetail.value = {}; orderedDishes.value = []
  try {
    const res = await ipadOrderDetail(target.booking_id, token, target, entry?.client_request_id)
    if (!current(generation) || token !== viewToken.value || Date.now() >= viewExpires) return
    if (res.code !== 200) { if ([401,403].includes(res.code)) expireView(); throw new Error(res.message || '订单读取失败') }
    const data = res.data, money = value => typeof value === 'string' && /^\d+\.\d{2}$/.test(value)
    if (data?.booking_id !== target.booking_id || String(data.store_id) !== target.store_id || data.read_only !== true
      || data.amount_basis !== 'active_dish_subtotal' || typeof data.booking_status !== 'string'
      || !Array.isArray(data.tables) || !Array.isArray(data.dishes) || !money(data.total_amount)
      || data.tables.some(row => !row || row.table_booking_id == null)
      || data.dishes.some(row => !row || row.dish_booking_id == null || typeof row.dish_name !== 'string'
        || !Number.isInteger(row.dish_quantity) || !money(row.unit_price) || !money(row.subtotal))) throw new Error('订单数据未能核对，请重新读取')
    orderDetail.value = data; orderedDishes.value = data.dishes; orderReady.value = true
    if (entry) {
      if (!Object.prototype.hasOwnProperty.call(data, 'submission')) { orderReady.value = false; throw new Error('收据查询暂不可用，原批次已保留') }
      if (data.submission !== null) {
        const receipt = assertReceipt(data.submission, entry)
        if (receiptRowsMatch(receipt, entry, data.dishes)) acceptCommitted(entry, receipt)
        else {
          receiptNeedsReview.value = true
          notice.value = '该提交编号已有收据，原菜品快照暂无法核对；记录已保留，请联系服务员核对'
          return
        }
      } else receiptNeedsReview.value = false
    }
    if (journal.value?.state === 'committed') {
      try { clearJournal(localStorage, journal.value); journal.value = null; journalError.value = '' } catch { journalError.value = '该批已提交，本机记录未清理；请联系管理员核对' }
    }
    notice.value = submitted.value ? '该批已提交，订单已重新读取' : journal.value ? orderWritable.value ? '原批次尚未查到收据，仍待核对；可再次核对或重新授权重放原批次' : '订单已结束，原批次仍待核对；仅可重新读取收据' : orderWritable.value ? '订单已核对，可选择加菜' : '订单已结束，仅可查看'
    submitted.value = false
  } catch (error) {
    if (current(generation)) {
      orderReady.value = false
      if ([401,403].includes(error?.response?.status)) expireView()
      notice.value = submitted.value ? '该批已提交，订单暂未读回；请仅重新读取订单' : errorMessage(error, '订单读取失败，请重试')
    }
  } finally { if (current(generation)) orderLoading.value = false }
}

async function loadDishes() {
  // 直接前端过滤 allDishes，避免后端 category_id 中文不识别
  if (activeCat.value === 'all') {
    dishes.value = allDishes.value
  } else {
    dishes.value = allDishes.value.filter(d => d.dish_category === activeCat.value)
  }
}

async function loadAllDishes(generation = scopeGeneration) {
  menuLoading.value = true
  try {
    const res = await ipadDishList()
    if (!current(generation)) return
    if (res.code === 200) {
      allDishes.value = res.data || []
    }
  } catch (e) {
    console.warn('All dish list API failed:', e.message)
  } finally {
    if (current(generation)) menuLoading.value = false
  }
}

async function loadCategories() {
  // 直接从 dish_master.dish_category 字段提取分类，避免 menu_category 表字符集问题
  const cats = [...new Set(allDishes.value.map(d => d.dish_category).filter(Boolean))]
  const catList = [{ category_id: 'all', dish_category: '全部', dish_category_en: 'All', count: allDishes.value.length }]
  cats.forEach(c => {
    const cnt = allDishes.value.filter(d => d.dish_category === c).length
    catList.push({ category_id: c, dish_category: c, dish_category_en: '', count: cnt })
  })
  categories.value = catList
}

watch(() => [bookingId.value, ipad.storeId, ipad.deviceSn], initializeScope, { flush: 'sync' })
onBeforeUnmount(() => { scopeGeneration++; searchGeneration++; expireView() })

onMounted(async () => {
  initializeScope()
})
</script>

<style scoped>
.cart-recovery { margin: 0; padding: 12px 20px; flex-shrink: 0; font-size: 13px; line-height: 1.6; overflow-wrap: anywhere; }
.order-status { display: flex; flex-wrap: wrap; align-items: center; gap: 8px; padding: 12px 20px; flex-shrink: 0; font-size: 13px; overflow-wrap: anywhere; }
.order-status button { padding: 8px 12px; border: 1px solid var(--color-border); border-radius: 6px; background: var(--color-card); cursor: pointer; }
.ordered-lines { max-height: 130px; overflow-y: auto; }
button:disabled { opacity: .5; cursor: not-allowed; }
@media (max-width: 600px) {
  .order-status { padding: 8px; gap: 6px; }
  .order-status button { width: 100%; }
  .guest-order .auth-box { width: calc(100vw - 24px); max-width: calc(100vw - 24px); max-height: calc(100dvh - 24px); overflow-y: auto; }
}
.guest-order {
  width: 100%; height: 100%;
  display: flex; background: var(--color-bg);
  position: relative;
}

/* ===== 左侧分类栏 ===== */
.category-sidebar {
  width: 160px; flex-shrink: 0;
  background: linear-gradient(180deg, #2D4A3E 0%, #1D3A2E 100%);
  display: flex; flex-direction: column;
  border-right: 2px solid rgba(196, 163, 90, 0.2);
}
.sidebar-header {
  padding: 20px 16px 16px; text-align: center;
  border-bottom: 1px solid rgba(196, 163, 90, 0.15);
}
.sidebar-title { display: block; font-size: 18px; font-weight: 700; color: #C4A35A; letter-spacing: 2px; font-family: var(--font-family); }
.sidebar-sub { display: block; font-size: 11px; color: rgba(196, 163, 90, 0.6); letter-spacing: 1px; margin-top: 4px; }

.category-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.cat-item {
  display: flex; align-items: center; justify-content: space-between;
  width: 100%; padding: 14px 18px; border: none; background: transparent;
  color: rgba(250, 248, 245, 0.7); font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all 0.2s; text-align: left;
  font-family: var(--font-family); letter-spacing: 1px;
  border-left: 3px solid transparent;
}
.cat-item:hover { background: rgba(196, 163, 90, 0.1); color: #FAF8F5; }
.cat-item.active {
  background: linear-gradient(90deg, rgba(196, 163, 90, 0.25) 0%, rgba(196, 163, 90, 0.05) 100%);
  color: #FFE8A8; font-weight: 700;
  border-left-color: #C4A35A;
}
.cat-count { font-size: 11px; opacity: 0.5; }

.sidebar-footer {
  padding: 14px 16px;
  border-top: 1px solid rgba(196, 163, 90, 0.15);
}
.room-total {
  padding: 10px 12px;
  background: rgba(0,0,0,0.2);
  border-radius: 8px;
  display: flex; flex-direction: column; gap: 2px;
}
.room-total span:first-child { font-size: 11px; color: rgba(250,248,245,0.5); letter-spacing: 1px; }
.room-total-price { font-size: 18px; font-weight: 700; color: #FFD78A; }

/* ===== 右侧主区域 ===== */
.content-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.content-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 24px; background: var(--color-card);
  border-bottom: 1px solid var(--color-border); flex-shrink: 0;
}
.header-left { display: flex; align-items: baseline; gap: 8px; }
.content-title { font-size: 20px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; display: inline; margin-right: 8px; }
.content-sub { font-size: 12px; color: var(--color-text-muted); letter-spacing: 1px; }
.header-right { display: flex; align-items: center; gap: 16px; }
.dish-count-info {
  display: flex; align-items: center; gap: 6px;
  font-size: 13px; color: var(--color-text-secondary);
}
.info-dot {
  width: 8px; height: 8px; border-radius: 50%;
  background: #27ae60; box-shadow: 0 0 0 3px rgba(39,174,96,0.15);
}
.search-box {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px; border: 1px solid var(--color-border);
  border-radius: var(--radius-md); background: var(--color-bg-alt);
}
.search-box svg { color: var(--color-text-muted); flex-shrink: 0; }
.search-box input { border: none; background: transparent; font-size: 13px; color: var(--color-text); outline: none; width: 160px; }

/* ===== 菜品网格 ===== */
.dish-grid {
  flex: 1; overflow-y: auto;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  grid-auto-rows: min-content;
  gap: 16px; padding: 20px 24px;
  align-content: start;
}

.dish-card {
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); overflow: hidden;
  cursor: pointer; transition: all 0.25s; position: relative;
}
.dish-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); border-color: var(--color-primary); }

.dish-img {
  width: 100%; aspect-ratio: 4/3; background: var(--color-bg-alt);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
  position: relative;
}
.dish-img img { width: 100%; height: 100%; object-fit: cover; }
.dish-img-fallback {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  width: 100%; height: 100%; padding: 8px; text-align: center;
}
.fallback-name {
  font-size: 15px; font-weight: 700; color: rgba(255,255,255,0.95);
  text-shadow: 0 1px 3px rgba(0,0,0,0.3); line-height: 1.3;
  font-family: var(--font-family); letter-spacing: 1px;
}
.fallback-cat {
  font-size: 10px; color: rgba(255,255,255,0.6); margin-top: 4px; letter-spacing: 1px;
}

/* 18种分类渐变色（徽派雅致配色，避免大红大紫） */
.cat-color-0 { background: linear-gradient(135deg, #2D4A3E 0%, #1D3A2E 100%); }
.cat-color-1 { background: linear-gradient(135deg, #3E5C4A 0%, #2D4A3E 100%); }
.cat-color-2 { background: linear-gradient(135deg, #5C7A6B 0%, #3E5C4A 100%); }
.cat-color-3 { background: linear-gradient(135deg, #7A8B6B 0%, #5C7A5C 100%); }
.cat-color-4 { background: linear-gradient(135deg, #8B7A5C 0%, #6B5C3E 100%); }
.cat-color-5 { background: linear-gradient(135deg, #A4833A 0%, #8B7A3E 100%); }
.cat-color-6 { background: linear-gradient(135deg, #C4A35A 0%, #A4833A 100%); }
.cat-color-7 { background: linear-gradient(135deg, #4A5C6B 0%, #2D3E4A 100%); }
.cat-color-8 { background: linear-gradient(135deg, #5C6B7A 0%, #3E4A5C 100%); }
.cat-color-9 { background: linear-gradient(135deg, #6B5C7A 0%, #4A3E5C 100%); }
.cat-color-10 { background: linear-gradient(135deg, #3E4A5C 0%, #2D3A4A 100%); }
.cat-color-11 { background: linear-gradient(135deg, #4A6B5C 0%, #2D4A3E 100%); }
.cat-color-12 { background: linear-gradient(135deg, #5C6B3E 0%, #3E4A2D 100%); }
.cat-color-13 { background: linear-gradient(135deg, #7A8B3E 0%, #5C6B2D 100%); }
.cat-color-14 { background: linear-gradient(135deg, #8B6B3E 0%, #6B5C2D 100%); }
.cat-color-15 { background: linear-gradient(135deg, #6B3E3E 0%, #4A2D2D 100%); }
.cat-color-16 { background: linear-gradient(135deg, #3E6B6B 0%, #2D4A4A 100%); }
.cat-color-17 { background: linear-gradient(135deg, #5C4A3E 0%, #3E2D2D 100%); }

.dish-info { padding: 10px 12px; }
.dish-name { font-size: 14px; font-weight: 700; color: var(--color-text); letter-spacing: 0.5px; margin-bottom: 4px; }
.dish-tags { display: flex; gap: 4px; margin-bottom: 4px; flex-wrap: wrap; }
.tag { font-size: 10px; padding: 1px 6px; border-radius: 3px; font-weight: 500; }
.tag.spicy { background: #FDECEC; color: #C25555; }
.dish-price { font-size: 16px; font-weight: 700; color: var(--color-accent-dark); }

/* 快速加菜 */
.quick-add {
  position: absolute; bottom: 10px; right: 10px;
  width: 32px; height: 32px; border-radius: 50%;
  background: var(--color-primary); color: white;
  border: none; font-size: 20px; font-weight: 700;
  cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
  box-shadow: 0 2px 8px rgba(45, 74, 62, 0.3);
  opacity: 0; transform: scale(0.8);
}
.dish-card:hover .quick-add { opacity: 1; transform: scale(1); }
.quick-add:hover { background: var(--color-primary-dark); transform: scale(1.1) !important; }

/* ===== 购物车浮动按钮 ===== */
.cart-fab {
  position: fixed; bottom: 24px; right: 24px;
  min-width: 56px; height: 56px; border-radius: 28px;
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white; display: flex; align-items: center; justify-content: center; gap: 8px;
  cursor: pointer; box-shadow: 0 4px 16px rgba(45, 74, 62, 0.35);
  transition: all 0.25s; z-index: 100;
  padding: 0 16px;
}
.cart-fab:hover { transform: translateY(-3px); box-shadow: 0 8px 24px rgba(45, 74, 62, 0.4); }
.cart-badge {
  position: absolute; top: -4px; right: -4px;
  min-width: 22px; height: 22px; border-radius: 11px;
  background: var(--color-danger); color: white;
  font-size: 12px; font-weight: 700;
  display: flex; align-items: center; justify-content: center;
  padding: 0 4px;
}
.cart-total { font-size: 14px; font-weight: 700; }

/* ===== 购物车侧滑面板 ===== */
.cart-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); z-index: 199; }
.cart-panel {
  position: fixed; top: 0; right: 0; bottom: 0;
  width: 360px; max-width: 90vw;
  background: var(--color-card); z-index: 200;
  display: flex; flex-direction: column;
  box-shadow: -4px 0 24px rgba(0,0,0,0.1);
}
.cart-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 18px 20px; border-bottom: 1px solid var(--color-border);
}
.cart-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; }
.cart-close { width: 32px; height: 32px; border: none; background: var(--color-bg-alt); border-radius: 50%; font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.cart-close:hover { background: var(--color-danger); color: white; }
.cart-body { flex: 1; overflow-y: auto; padding: 12px 20px; }
.cart-item {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 0; border-bottom: 1px solid var(--color-border-light);
}
.cart-item-name { font-size: 14px; font-weight: 600; color: var(--color-text); }
.cart-item-price { font-size: 13px; color: var(--color-accent-dark); font-weight: 600; }
.cart-item-qty { display: flex; align-items: center; gap: 10px; }
.cart-item-qty button { width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 16px; cursor: pointer; display: flex; align-items: center; justify-content: center; }
.cart-item-qty button:hover { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.cart-item-qty span { font-size: 16px; font-weight: 700; min-width: 24px; text-align: center; }
.cart-empty { flex: 1; display: flex; flex-direction: column; align-items: center; justify-content: center; color: var(--color-text-muted); }
.cart-empty p { font-size: 15px; margin-bottom: 4px; }
.cart-empty-en { font-size: 12px; }
.cart-footer { padding: 16px 20px; border-top: 1px solid var(--color-border); }
.cart-summary { display: flex; justify-content: space-between; margin-bottom: 12px; }
.cart-summary-price { font-size: 22px; font-weight: 700; color: var(--color-accent-dark); }
.cart-actions { display: flex; gap: 10px; }
.btn-clear { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-submit { flex: 2; padding: 12px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; letter-spacing: 1px; }
.btn-submit:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45, 74, 62, 0.3); }

/* ===== 菜品详情弹窗 ===== */
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.detail-modal {
  background: var(--color-card); border-radius: var(--radius-xl);
  width: 520px; max-width: 90vw; overflow: hidden;
  box-shadow: var(--shadow-xl);
}
.detail-img { width: 100%; height: 200px; background: var(--color-bg-alt); display: flex; align-items: center; justify-content: center; overflow: hidden; }
.detail-img img { width: 100%; height: 100%; object-fit: cover; }
.detail-img-placeholder { font-size: 48px; font-weight: 700; color: var(--color-border); font-family: var(--font-family); }
.detail-info { padding: 24px; }
.detail-info h3 { font-size: 22px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; margin-bottom: 4px; }
.detail-category { font-size: 13px; color: var(--color-text-muted); margin-bottom: 10px; }
.detail-tags { display: flex; gap: 6px; margin-bottom: 12px; }
.detail-ingredients { font-size: 13px; color: var(--color-text-secondary); margin-bottom: 16px; line-height: 1.6; }
.detail-price { font-size: 28px; font-weight: 700; color: var(--color-accent-dark); margin-bottom: 16px; }
.detail-qty { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.detail-qty button { width: 40px; height: 40px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.detail-qty button:hover { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.detail-qty span { font-size: 24px; font-weight: 700; min-width: 40px; text-align: center; }
.detail-add-btn {
  width: 100%; padding: 14px; border: none; border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white; font-size: 16px; font-weight: 700; cursor: pointer;
  letter-spacing: 2px; transition: all 0.25s;
}
.detail-add-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }

/* ===== 服务员授权弹窗 ===== */
.auth-overlay { z-index: 500; }
.auth-box {
  background: var(--color-card);
  border-radius: 16px;
  width: 50vw; max-width: 440px;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  overflow: hidden;
}
.auth-header {
  padding: 16px 24px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(135deg, #2D4A3E 0%, #1D3A2E 100%);
}
.auth-header h3 { font-size: 18px; font-weight: 700; color: #fff; letter-spacing: 1px; }
.auth-close { background: none; border: none; color: #fff; font-size: 24px; cursor: pointer; padding: 0 4px; }
.auth-hint { font-size: 13px; color: var(--color-text-secondary); padding: 16px 24px 8px; }
.auth-form { padding: 0 24px 12px; display: flex; flex-direction: column; gap: 14px; }
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-item label { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
.form-item input {
  height: 32px; padding: 0 12px;
  border: 1px solid var(--color-border); border-radius: 6px;
  font-size: 13px; font-family: var(--font-family);
  background: var(--color-card); color: var(--color-text); outline: none;
}
.form-item input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 2px rgba(45,74,62,0.1); }
.auth-error { color: var(--color-danger); font-size: 13px; padding: 0 24px; }
.auth-actions { display: flex; gap: 12px; padding: 16px 24px 24px; }
.btn-cancel { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-confirm { flex: 2; padding: 12px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; letter-spacing: 1px; }
.btn-confirm:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }
.btn-confirm:disabled { opacity: 0.5; transform: none; box-shadow: none; }

/* 成功提示 */
.success-toast {
  position: fixed; top: 20px; left: 50%; transform: translateX(-50%);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white; padding: 12px 24px;
  border-radius: 10px; font-size: 14px; font-weight: 600; z-index: 1000;
  box-shadow: 0 4px 16px rgba(45, 74, 62, 0.35);
  letter-spacing: 1px;
}

/* 动画 */
.modal-enter-active, .modal-leave-active { transition: opacity 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .detail-modal, .modal-enter-from .auth-box { transform: scale(0.95); }
.cart-slide-enter-active, .cart-slide-leave-active { transition: transform 0.3s ease; }
.cart-slide-enter-from, .cart-slide-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

<template>
  <div class="guest-order">
    <!-- 左侧分类栏 -->
    <aside class="category-sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">{{ orderDetail.table_name || '加载中...' }}</span>
        <span class="sidebar-sub" v-if="orderDetail.customer_name">
          {{ orderDetail.customer_name }} · {{ orderDetail.guest_count }}人
        </span>
        <span class="sidebar-sub" v-else>加菜 · Guest Order</span>
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
        <div class="room-total" v-if="orderDetail.total_amount">
          <span>已点金额</span>
          <span class="room-total-price">¥{{ orderDetail.total_amount }}</span>
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
            <input v-model="searchKeyword" placeholder="搜索菜品 · Search" @input="onSearch" />
          </div>
        </div>
      </div>

      <!-- 菜品网格 -->
      <div class="dish-grid">
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
            <div class="dish-price">¥{{ Number(dish.sale_price).toFixed(0) }}</div>
          </div>
          <button class="quick-add" @click.stop="addToCart(dish)">+</button>
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
        <div class="cart-body" v-if="cart.length">
          <div v-for="item in cart" :key="item.dish_id" class="cart-item">
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.dish_name }}</div>
              <div class="cart-item-price">¥{{ Number(item.sale_price || item.unit_price).toFixed(0) }}</div>
            </div>
            <div class="cart-item-qty">
              <button @click="changeQty(item, -1)">−</button>
              <span>{{ item.qty }}</span>
              <button @click="changeQty(item, 1)">+</button>
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
            <span class="cart-summary-price">¥{{ Number(cartTotal).toFixed(2) }}</span>
          </div>
          <div class="cart-actions">
            <button class="btn-clear" @click="cart = []">清空</button>
            <button class="btn-submit" @click="showAuth = true">服务员授权提交</button>
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
            <div class="detail-price">¥{{ Number(detailDish?.sale_price || 0).toFixed(0) }}</div>
            <div class="detail-qty">
              <button @click="detailQty = Math.max(1, detailQty - 1)">−</button>
              <span>{{ detailQty }}</span>
              <button @click="detailQty++">+</button>
            </div>
            <button class="detail-add-btn" @click="addFromDetail">加入加菜 · Add</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 服务员授权弹窗 -->
    <Transition name="modal">
      <div v-if="showAuth" class="modal-overlay auth-overlay" @click.self="showAuth = false">
        <div class="auth-box">
          <div class="auth-header">
            <h3>服务员授权 · Staff Authorization</h3>
            <button class="auth-close" @click="showAuth = false">×</button>
          </div>
          <p class="auth-hint">客人加菜需服务员确认，请输入账号密码</p>
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
            <button class="btn-cancel" @click="showAuth = false">取消</button>
            <button class="btn-confirm" :disabled="authLoading" @click="handleAuth">
              {{ authLoading ? '验证中...' : '授权并提交' }}
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
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ipadDishList, ipadDishSearch, ipadOrderDetail, ipadAuthVerify, ipadOrderAddDishes } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const route = useRoute()
const bookingId = route.params.bookingId

// 订单详情
const orderDetail = ref({})
const orderedDishes = ref([])

// 菜品
const categories = ref([])
const activeCat = ref('all')
const allDishes = ref([])
const dishes = ref([])
const searchKeyword = ref('')

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
const cartTotal = computed(() => cart.value.reduce((s, i) => s + Number(i.sale_price || i.unit_price) * i.qty, 0).toFixed(0))

// 菜品详情
const showDetail = ref(false)
const detailDish = ref(null)
const detailQty = ref(1)

// 授权
const showAuth = ref(false)
const authForm = ref({ username: '', password: '' })
const authError = ref('')
const authLoading = ref(false)
const verifiedStaff = ref(null)

// 成功提示
const successMsg = ref('')

function selectCategory(cat) {
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
  addToCart(detailDish.value, detailQty.value)
  showDetail.value = false
  ElMessage.success(`已加入 ${detailQty.value} 份`)
}

function addToCart(dish, qty = 1) {
  const existing = cart.value.find(i => i.dish_id === (dish.dish_id || dish.id))
  if (existing) {
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
  item.qty += delta
  if (item.qty <= 0) {
    cart.value = cart.value.filter(i => i !== item)
  }
}

async function onSearch() {
  if (!searchKeyword.value) { loadDishes(); return }
  try {
    const res = await ipadDishSearch(searchKeyword.value)
    if (res.code === 200) dishes.value = res.data || []
  } catch {
    dishes.value = allDishes.value.filter(d => d.dish_name?.includes(searchKeyword.value))
  }
}

async function handleAuth() {
  if (!authForm.value.username || !authForm.value.password) {
    authError.value = '请输入账号和密码'
    return
  }
  authLoading.value = true
  authError.value = ''
  try {
    const res = await ipadAuthVerify({
      username: authForm.value.username,
      password: authForm.value.password
    })
    if (res.code === 200) {
      verifiedStaff.value = res.data
      showAuth.value = false
      await submitAddDishes()
    } else {
      authError.value = res.msg || '授权失败'
    }
  } catch (e) {
    authError.value = e.response?.data?.msg || '网络错误'
  } finally {
    authLoading.value = false
  }
}

async function submitAddDishes() {
  try {
    const res = await ipadOrderAddDishes({
      booking_id: bookingId,
      staff_id: verifiedStaff.value.staff_id,
      dishes: cart.value.map(i => ({ dish_id: i.dish_id, dish_quantity: i.qty }))
    })
    if (res.code === 200) {
      successMsg.value = `加菜成功！新增${res.data.added_dishes}道，合计¥${res.data.added_amount}`
      cart.value = []
      showCart.value = false
      authForm.value = { username: '', password: '' }
      setTimeout(() => { successMsg.value = '' }, 3000)
      await loadOrderDetail()
    } else {
      successMsg.value = '加菜失败：' + (res.msg || '')
      setTimeout(() => { successMsg.value = '' }, 3000)
    }
  } catch (e) {
    successMsg.value = '加菜失败：网络错误'
    setTimeout(() => { successMsg.value = '' }, 3000)
  }
}

async function loadOrderDetail() {
  try {
    const res = await ipadOrderDetail(bookingId)
    if (res.code === 200) {
      orderDetail.value = res.data
      orderedDishes.value = res.data.dishes || []
    }
  } catch (e) {
    console.error('加载订单详情失败', e)
  }
}

async function loadDishes() {
  // 直接前端过滤 allDishes，避免后端 category_id 中文不识别
  if (activeCat.value === 'all') {
    dishes.value = allDishes.value
  } else {
    dishes.value = allDishes.value.filter(d => d.dish_category === activeCat.value)
  }
}

async function loadAllDishes() {
  try {
    const res = await ipadDishList()
    if (res.code === 200) {
      allDishes.value = res.data || []
    }
  } catch (e) {
    console.warn('All dish list API failed:', e.message)
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

onMounted(async () => {
  await loadAllDishes()
  await loadCategories()
  await loadDishes()
  await loadOrderDetail()
})
</script>

<style scoped>
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

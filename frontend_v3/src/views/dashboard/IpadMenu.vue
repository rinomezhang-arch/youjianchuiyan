<template>
  <div class="ipad-menu-page">
    <!-- 顶部信息栏 -->
    <header class="top-bar">
      <div class="top-left">
        <div class="brand-logo">
          <span class="brand-icon">炊</span>
        </div>
        <div class="brand-info">
          <div class="brand-name">又见炊烟私房菜</div>
          <div class="brand-name-en">Youjian Kitchen · Private Cuisine</div>
        </div>
      </div>
      <div class="top-right">
        <div class="table-badge">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 7V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v2"/></svg>
          <span>A06</span>
        </div>
        <button class="lang-btn" @click="toggleLang">{{ lang === 'zh' ? 'EN/中' : '中/EN' }}</button>
        <button class="back-btn" @click="$router.push('/dashboard/menu')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          返回
        </button>
      </div>
    </header>

    <!-- 标签栏 -->
    <div class="tab-bar">
      <button
        :class="['tab-item', { active: activeTab === 'alacarte' }]"
        @click="activeTab = 'alacarte'; loadDishes()"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 8h1a4 4 0 0 1 0 8h-1"/><path d="M2 8h16v9a4 4 0 0 1-4 4H6a4 4 0 0 1-4-4V8z"/><line x1="6" y1="1" x2="6" y2="4"/><line x1="10" y1="1" x2="10" y2="4"/><line x1="14" y1="1" x2="14" y2="4"/></svg>
        零点 · À la Carte
      </button>
      <button
        :class="['tab-item', { active: activeTab === 'banquet' }]"
        @click="activeTab = 'banquet'; loadDishes()"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>
        宴会 · Banquet
      </button>
      <button
        :class="['tab-item', { active: activeTab === 'all' }]"
        @click="activeTab = 'all'; loadDishes()"
      >
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/></svg>
        全部 · All
      </button>
    </div>

    <!-- 主体区域 -->
    <div class="main-body">
      <!-- 左侧分类栏 -->
      <aside class="category-sidebar">
        <div class="sidebar-scroll">
          <button
            v-for="cat in categories"
            :key="cat.category_id"
            :class="['cat-item', { active: activeCat === cat.category_id }]"
            @click="selectCategory(cat)"
          >
            <span class="cat-icon">{{ cat.icon || '🍽' }}</span>
            <span class="cat-name">{{ cat.dish_category }}</span>
            <span class="cat-count">({{ cat.count || 0 }})</span>
          </button>
        </div>
      </aside>

      <!-- 右侧内容区 -->
      <div class="content-area">
        <!-- 搜索栏 -->
        <div class="search-bar">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchKeyword" placeholder="搜索菜品 / Search dishes..." @input="onSearch" />
        </div>

        <!-- 菜品网格 -->
        <div class="dish-grid">
          <div
            v-for="dish in displayDishes"
            :key="dish.dish_id || dish.id"
            :class="['dish-card', { 'sold-out': dish.is_sold_out }]"
            @click="!dish.is_sold_out && showDishDetail(dish)"
          >
            <div class="dish-img-wrap">
              <img v-if="dish.cover_img || dish.image_url" :src="dish.cover_img || dish.image_url" :alt="dish.dish_name" />
              <div v-else class="dish-img-placeholder">{{ dish.dish_name?.charAt(0) }}</div>
              <!-- 标签角标 -->
              <div v-if="dish.dish_tag === '招牌' || dish.tag === '招牌'" class="badge badge-signature">招牌</div>
              <div v-else-if="dish.dish_tag === '热门' || dish.dish_tag === '推荐' || dish.tag === '热门'" class="badge badge-hot">热门</div>
              <div v-if="dish.is_sold_out" class="badge badge-soldout">沽清</div>
              <!-- 已点数量 -->
              <div v-if="getCartQty(dish.dish_id || dish.id) > 0" class="badge badge-qty">
                {{ getCartQty(dish.dish_id || dish.id) }}
              </div>
            </div>
            <div class="dish-info">
              <div class="dish-name-cn">{{ dish.dish_name }}</div>
              <div class="dish-name-en">{{ dish.dish_name_en || dish.dish_name }}</div>
              <div class="dish-bottom">
                <span class="dish-price">¥{{ Number(dish.sale_price || 0).toFixed(0) }}</span>
                <button
                  v-if="!dish.is_sold_out"
                  class="add-btn"
                  @click.stop="quickAddDish(dish)"
                >+</button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 浮动购物车 -->
    <div class="cart-fab" @click="showCart = true">
      <div class="cart-fab-icon">
        <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><line x1="3" y1="6" x2="21" y2="6"/><path d="M16 10a4 4 0 0 1-8 0"/></svg>
        <span v-if="ipad.cartCount > 0" class="cart-fab-badge">{{ ipad.cartCount }}</span>
      </div>
      <div class="cart-fab-info">
        <span class="cart-fab-label">查看点菜单</span>
        <span class="cart-fab-detail">{{ ipad.cartCount }}道菜 · ¥{{ ipad.cartTotal.toFixed(0) }}</span>
      </div>
    </div>

    <!-- 购物车侧滑面板 -->
    <Transition name="cart-slide">
      <div v-if="showCart" class="cart-panel">
        <div class="cart-panel-header">
          <h3>已点菜品 · Ordered</h3>
          <button class="cart-close" @click="showCart = false">×</button>
        </div>
        <div class="cart-panel-body" v-if="ipad.cartItems.length">
          <div v-for="item in ipad.cartItems" :key="item.dish_id" class="cart-item">
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.dish_name }}</div>
              <div class="cart-item-price">¥{{ Number(item.sale_price || item.unit_price || 0).toFixed(0) }}</div>
            </div>
            <div class="cart-item-qty">
              <button @click="ipad.updateCartQty(item.dish_id, item.dish_quantity - 1)">−</button>
              <span>{{ item.dish_quantity }}</span>
              <button @click="ipad.updateCartQty(item.dish_id, item.dish_quantity + 1)">+</button>
            </div>
          </div>
        </div>
        <div v-else class="cart-empty">
          <p>还没有点菜</p>
          <p class="cart-empty-en">No dishes ordered yet</p>
        </div>
        <div class="cart-panel-footer" v-if="ipad.cartItems.length">
          <div class="cart-summary">
            <span>合计 · Total</span>
            <span class="cart-summary-price">¥{{ ipad.cartTotal.toFixed(2) }}</span>
          </div>
          <div class="cart-actions">
            <button class="btn-clear" @click="ipad.clearCart()">清空</button>
            <button class="btn-submit" @click="submitToKitchen">提交后厨 · Submit</button>
          </div>
        </div>
      </div>
    </Transition>
    <Transition name="fade">
      <div v-if="showCart" class="cart-overlay" @click="showCart = false"></div>
    </Transition>

    <!-- 菜品详情弹窗 -->
    <Transition name="modal">
      <div v-if="showDetail" class="modal-overlay" @click.self="showDetail = false">
        <div class="detail-modal">
          <button class="detail-close" @click="showDetail = false">×</button>
          <div class="detail-img">
            <img v-if="detailDish?.cover_img || detailDish?.image_url" :src="detailDish.cover_img || detailDish.image_url" />
            <div v-else class="detail-img-placeholder">{{ detailDish?.dish_name?.charAt(0) }}</div>
          </div>
          <div class="detail-info">
            <h3>{{ detailDish?.dish_name }}</h3>
            <p class="detail-en">{{ detailDish?.dish_name_en || detailDish?.dish_name }}</p>
            <p class="detail-category">{{ detailDish?.dish_category }}</p>
            <div class="detail-tags">
              <span v-if="detailDish?.spicy_level" class="tag spicy">{{ '🌶'.repeat(detailDish.spicy_level) }}</span>
              <span v-if="detailDish?.cooking_method" class="tag">{{ detailDish.cooking_method }}</span>
              <span v-if="detailDish?.taste" class="tag">{{ detailDish.taste }}</span>
              <span v-if="detailDish?.main_ingredients" class="tag ingredient">食材：{{ detailDish.main_ingredients }}</span>
            </div>
            <div class="detail-price-row">
              <span class="detail-price">¥{{ Number(detailDish?.sale_price || 0).toFixed(0) }}</span>
              <div class="detail-qty">
                <button @click="detailQty = Math.max(1, detailQty - 1)">−</button>
                <span>{{ detailQty }}</span>
                <button @click="detailQty++">+</button>
              </div>
            </div>
            <div class="detail-remark">
              <input v-model="detailRemark" placeholder="口味备注 · Taste note (可选)" />
            </div>
            <button class="detail-add-btn" @click="addFromDetail">加入购物车 · Add to Cart</button>
            <!-- 搜索链接 -->
            <div class="detail-links">
              <a :href="`https://www.douyin.com/search/${encodeURIComponent(detailDish?.dish_name || '')}`" target="_blank" class="search-link">
                抖音搜索做法
              </a>
              <a :href="`https://www.baidu.com/s?wd=${encodeURIComponent(detailDish?.dish_name + ' 做法')}`" target="_blank" class="search-link">
                百度搜索做法
              </a>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadDishCategory, ipadDishList, ipadDishSearch, ipadOrderAdd, ipadOrderSendKitchen } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const router = useRouter()
const ipad = useIpadStore()

const lang = ref('zh')
const activeTab = ref('alacarte')
const categories = ref([])
const activeCat = ref('all')
const dishes = ref([])
const allDishes = ref([])
const searchKeyword = ref('')
const showCart = ref(false)
const showDetail = ref(false)
const detailDish = ref(null)
const detailQty = ref(1)
const detailRemark = ref('')

const categoryIcons = {
  '全部': '📋', '冷菜': '🥒', '热菜': '🍳', '海鲜': '🦐',
  '肉类': '🥩', '禽类': '', '汤菜': '🍜', '蔬菜': '🥬',
  '主食': '🍚', '点心': '🥟', '酒水': '', '茶饮': '🍵',
  '套餐': '🎁', '凉菜': '🥒', '汤类': '🍜', '饮品': ''
}

const currentCatName = computed(() => {
  if (activeCat.value === 'all') return '全部菜品'
  const cat = categories.value.find(c => c.category_id === activeCat.value)
  return cat?.dish_category || '全部菜品'
})

const displayDishes = computed(() => {
  if (searchKeyword.value) return dishes.value.filter(d => d.dish_name?.includes(searchKeyword.value))
  return dishes.value
})

function toggleLang() {
  lang.value = lang.value === 'zh' ? 'en' : 'zh'
}

function selectCategory(cat) {
  activeCat.value = cat.category_id
  loadDishes()
}

async function loadDishes() {
  try {
    const params = activeCat.value === 'all' ? {} : { category_id: activeCat.value }
    const res = await ipadDishList(params)
    if (res.code === 200) {
      dishes.value = res.data || []
    }
  } catch (e) {
    console.warn('Dish list API failed:', e.message)
    dishes.value = mockDishes(activeCat.value)
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

function getCartQty(dishId) {
  const item = ipad.cartItems.find(i => i.dish_id === dishId)
  return item ? item.dish_quantity : 0
}

function showDishDetail(dish) {
  detailDish.value = dish
  detailQty.value = 1
  detailRemark.value = ''
  showDetail.value = true
}

function quickAddDish(dish) {
  ipad.addToCart({
    dish_id: dish.dish_id || dish.id,
    dish_name: dish.dish_name,
    sale_price: dish.sale_price,
    unit_price: dish.sale_price,
    dish_quantity: 1
  })
  submitAddDish(dish.dish_id || dish.id, 1)
  ElMessage.success(`已加 ${dish.dish_name}`)
}

function addFromDetail() {
  ipad.addToCart({
    dish_id: detailDish.value.dish_id || detailDish.value.id,
    dish_name: detailDish.value.dish_name,
    sale_price: detailDish.value.sale_price,
    unit_price: detailDish.value.sale_price,
    dish_quantity: detailQty.value
  })
  submitAddDish(detailDish.value.dish_id || detailDish.value.id, detailQty.value, detailRemark.value)
  showDetail.value = false
  ElMessage.success(`已加入 ${detailQty.value} 份`)
}

async function submitAddDish(dishId, qty, note) {
  if (!ipad.currentBooking?.booking_id) return
  try {
    await ipadOrderAdd({
      booking_id: ipad.currentBooking.booking_id,
      dish_id: dishId,
      dish_quantity: qty,
      dish_note: note || undefined
    })
  } catch (e) {
    console.warn('Add dish API failed (mock mode):', e.message)
  }
}

async function submitToKitchen() {
  if (!ipad.currentBooking?.booking_id) {
    ElMessage.warning('请先开台')
    return
  }
  try {
    const res = await ipadOrderSendKitchen(ipad.currentBooking.booking_id)
    if (res.code === 200) {
      ElMessage.success('已提交后厨')
      showCart.value = false
    } else {
      ElMessage.error(res.msg || '提交失败')
    }
  } catch (e) {
    console.warn('Send kitchen API failed:', e.message)
    ElMessage.warning('演示模式：已模拟提交后厨')
    showCart.value = false
  }
}

// 模拟数据（后端未就绪时降级）
function mockDishes(catId) {
  const all = [
    { dish_id: 'D001', dish_name: '蒜泥白肉', dish_name_en: 'Garlic Pork with Soy Sauce', dish_category: '冷菜', sale_price: 48, spicy_level: 0, cooking_method: '凉拌', taste: '蒜香', is_sold_out: 0, dish_tag: '招牌' },
    { dish_id: 'D002', dish_name: '清蒸石斑鱼', dish_name_en: 'Steamed Grouper', dish_category: '热菜', sale_price: 128, spicy_level: 0, cooking_method: '清蒸', taste: '鲜美', is_sold_out: 0, dish_tag: '热门' },
    { dish_id: 'D003', dish_name: '凉拌木耳', dish_name_en: 'Wood Ear Mushroom Salad', dish_category: '冷菜', sale_price: 28, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D004', dish_name: '红烧肉', dish_name_en: 'Braised Pork Belly in Brown Sauce', dish_category: '热菜', sale_price: 68, spicy_level: 0, cooking_method: '红烧', taste: '咸甜', is_sold_out: 0, dish_tag: '招牌' },
    { dish_id: 'D005', dish_name: '口水鸡', dish_name_en: 'Mouth-Watering Chicken', dish_category: '热菜', sale_price: 42, spicy_level: 2, cooking_method: '凉拌', taste: '麻辣', is_sold_out: 0, dish_tag: '热门' },
    { dish_id: 'D006', dish_name: '酸辣汤', dish_name_en: 'Hot and Sour Soup', dish_category: '汤菜', sale_price: 38, spicy_level: 2, is_sold_out: 0, dish_tag: '汤汤' },
    { dish_id: 'D007', dish_name: '清蒸鲈鱼', dish_name_en: 'Steamed Sea Bass', dish_category: '海鲜', sale_price: 88, spicy_level: 0, cooking_method: '清蒸', taste: '鲜美', is_sold_out: 0, dish_tag: '推荐' },
    { dish_id: 'D008', dish_name: '蒜蓉粉丝蒸扇贝', dish_name_en: 'Steamed Scallops with Garlic', dish_category: '海鲜', sale_price: 128, spicy_level: 0, is_sold_out: 0, dish_tag: '热门' },
    { dish_id: 'D009', dish_name: '凉拌黄瓜', dish_name_en: 'Cucumber Salad', dish_category: '冷菜', sale_price: 18, spicy_level: 1, is_sold_out: 0 },
    { dish_id: 'D010', dish_name: '老醋花生', dish_name_en: 'Peanuts in Vinegar', dish_category: '冷菜', sale_price: 22, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D011', dish_name: '皮蛋豆腐', dish_name_en: 'Century Egg with Tofu', dish_category: '冷菜', sale_price: 28, spicy_level: 1, is_sold_out: 0 },
    { dish_id: 'D012', dish_name: '番茄蛋汤', dish_name_en: 'Tomato Egg Soup', dish_category: '汤菜', sale_price: 28, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D013', dish_name: '蛋炒饭', dish_name_en: 'Egg Fried Rice', dish_category: '主食', sale_price: 22, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D014', dish_name: '手工水饺', dish_name_en: 'Handmade Dumplings', dish_category: '主食', sale_price: 32, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D015', dish_name: '酸梅汤', dish_name_en: 'Plum Drink', dish_category: '茶饮', sale_price: 15, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D016', dish_name: '鲜榨橙汁', dish_name_en: 'Fresh Orange Juice', dish_category: '酒水', sale_price: 25, spicy_level: 0, is_sold_out: 1 },
    { dish_id: 'D017', dish_name: '迎春接福宴', dish_name_en: 'Spring Banquet', dish_category: '套餐', sale_price: 988, spicy_level: 0, is_sold_out: 0, dish_tag: '套餐' },
    { dish_id: 'D018', dish_name: '阖家团圆宴', dish_name_en: 'Family Reunion Banquet', dish_category: '套餐', sale_price: 1288, spicy_level: 0, is_sold_out: 0, dish_tag: '套餐' },
    { dish_id: 'D019', dish_name: '水煮牛肉', dish_name_en: 'Boiled Beef in Chili Oil', dish_category: '热菜', sale_price: 78, spicy_level: 3, is_sold_out: 0, dish_tag: '热门' },
    { dish_id: 'D020', dish_name: '干锅花菜', dish_name_en: 'Dry Pot Cauliflower', dish_category: '热菜', sale_price: 48, spicy_level: 2, is_sold_out: 0 },
    { dish_id: 'D021', dish_name: '铁板牛柳', dish_name_en: 'Sizzling Beef Tenderloin', dish_category: '热菜', sale_price: 98, spicy_level: 1, is_sold_out: 0, dish_tag: '招牌' },
    { dish_id: 'D022', dish_name: '白灼虾', dish_name_en: 'Blanched Shrimp', dish_category: '海鲜', sale_price: 168, spicy_level: 0, is_sold_out: 0, dish_tag: '推荐' },
    { dish_id: 'D023', dish_name: '糖醋排骨', dish_name_en: 'Sweet & Sour Ribs', dish_category: '热菜', sale_price: 58, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D024', dish_name: '宫保鸡丁', dish_name_en: 'Kung Pao Chicken', dish_category: '禽类', sale_price: 48, spicy_level: 2, is_sold_out: 0 },
    { dish_id: 'D025', dish_name: '北京烤鸭', dish_name_en: 'Peking Duck', dish_category: '禽类', sale_price: 198, spicy_level: 0, is_sold_out: 0, dish_tag: '招牌' },
    { dish_id: 'D026', dish_name: '清炒时蔬', dish_name_en: 'Stir-fried Seasonal Vegetables', dish_category: '蔬菜', sale_price: 32, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D027', dish_name: '小笼包', dish_name_en: 'Soup Dumplings', dish_category: '点心', sale_price: 38, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D028', dish_name: '桂花糕', dish_name_en: 'Osmanthus Cake', dish_category: '点心', sale_price: 28, spicy_level: 0, is_sold_out: 0 },
  ]
  if (catId === 'all') return all
  const catMap = { 'hot': '热菜', 'cold': '冷菜', 'seafood': '海鲜', 'meat': '肉类', 'poultry': '禽类', 'soup': '汤菜', 'vegetable': '蔬菜', 'staple': '主食', 'dimsum': '点心', 'alcohol': '酒水', 'tea': '茶饮', 'package': '套餐' }
  const catName = catMap[catId] || Object.values(catMap).find(v => v === catId)
  return all.filter(d => d.dish_category === catName || d.dish_category === catId)
}

onMounted(async () => {
  try {
    const res = await ipadDishCategory()
    if (res.code === 200 && res.data?.length) {
      categories.value = [
        { category_id: 'all', dish_category: '全部', count: res.data.reduce((s, c) => s + (c.count || 0), 0), icon: '' },
        ...res.data.map(c => ({ ...c, icon: categoryIcons[c.dish_category] || '🍽' }))
      ]
    } else throw new Error('empty')
  } catch {
    categories.value = [
      { category_id: 'all', dish_category: '全部', count: 34, icon: '📋' },
      { category_id: 'cold', dish_category: '冷菜', count: 6, icon: '🥒' },
      { category_id: 'hot', dish_category: '热菜', count: 10, icon: '🍳' },
      { category_id: 'seafood', dish_category: '海鲜', count: 6, icon: '🦐' },
      { category_id: 'meat', dish_category: '肉类', count: 3, icon: '' },
      { category_id: 'poultry', dish_category: '禽类', count: 3, icon: '🍗' },
      { category_id: 'soup', dish_category: '汤菜', count: 4, icon: '🍜' },
      { category_id: 'vegetable', dish_category: '蔬菜', count: 4, icon: '🥬' },
      { category_id: 'staple', dish_category: '主食', count: 3, icon: '🍚' },
      { category_id: 'dimsum', dish_category: '点心', count: 2, icon: '🥟' },
      { category_id: 'alcohol', dish_category: '酒水', count: 2, icon: '🍷' },
      { category_id: 'tea', dish_category: '茶饮', count: 2, icon: '' },
    ]
  }
  loadDishes()
  try {
    const allRes = await ipadDishList({})
    if (allRes.code === 200) allDishes.value = allRes.data || []
  } catch {
    allDishes.value = mockDishes('all')
  }
})
</script>

<style scoped>
/* ===== 页面整体 ===== */
.ipad-menu-page {
  width: 100%;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #F5F0E8;
  font-family: 'Microsoft YaHei', 'PingFang SC', sans-serif;
  overflow: hidden;
}

/* ===== 顶部信息栏 ===== */
.top-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 24px;
  background: linear-gradient(135deg, #1a3a2a 0%, #2d5a3d 50%, #1a3a2a 100%);
  color: #fff;
  flex-shrink: 0;
}

.top-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brand-logo {
  width: 44px;
  height: 44px;
  background: linear-gradient(135deg, #C4A35A, #D4B86A);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
  color: #1a3a2a;
  flex-shrink: 0;
}

.brand-info {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 18px;
  font-weight: 700;
  letter-spacing: 2px;
  color: #F5F0E8;
}

.brand-name-en {
  font-size: 11px;
  color: rgba(245, 240, 232, 0.7);
  letter-spacing: 1px;
}

.top-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.table-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  background: rgba(196, 163, 90, 0.2);
  border: 1px solid rgba(196, 163, 90, 0.4);
  border-radius: 20px;
  font-size: 14px;
  font-weight: 600;
  color: #C4A35A;
}

.lang-btn {
  padding: 6px 14px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.lang-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  background: transparent;
  border: 1px solid rgba(255, 255, 255, 0.3);
  border-radius: 20px;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.1);
}

/* ===== 标签栏 ===== */
.tab-bar {
  display: flex;
  gap: 0;
  padding: 0 24px;
  background: linear-gradient(135deg, #8B2020, #A03030);
  flex-shrink: 0;
}

.tab-item {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 20px;
  background: transparent;
  border: none;
  color: rgba(255, 255, 255, 0.7);
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.25s;
  letter-spacing: 1px;
  border-bottom: 3px solid transparent;
}

.tab-item:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.08);
}

.tab-item.active {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border-bottom-color: #C4A35A;
  font-weight: 700;
}

/* ===== 主体区域 ===== */
.main-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* ===== 左侧分类栏 ===== */
.category-sidebar {
  width: 160px;
  background: linear-gradient(180deg, #1a3a2a 0%, #2d5a3d 40%, #1a3a2a 100%);
  flex-shrink: 0;
  overflow: hidden;
  position: relative;
}

.category-sidebar::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: url("data:image/svg+xml,%3Csvg width='60' height='60' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='n'%3E%3CfeTurbulence baseFrequency='0.65' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='60' height='60' filter='url(%23n)' opacity='0.03'/%3E%3C/svg%3E");
  pointer-events: none;
}

.sidebar-scroll {
  height: 100%;
  overflow-y: auto;
  padding: 8px 0;
  position: relative;
  z-index: 1;
}

.sidebar-scroll::-webkit-scrollbar {
  width: 3px;
}

.sidebar-scroll::-webkit-scrollbar-thumb {
  background: rgba(196, 163, 90, 0.3);
  border-radius: 3px;
}

.cat-item {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
  padding: 14px 16px;
  background: transparent;
  border: none;
  color: rgba(245, 240, 232, 0.7);
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  border-left: 3px solid transparent;
}

.cat-item:hover {
  background: rgba(255, 255, 255, 0.06);
  color: #F5F0E8;
}

.cat-item.active {
  background: rgba(196, 163, 90, 0.15);
  color: #C4A35A;
  border-left-color: #C4A35A;
  font-weight: 600;
}

.cat-icon {
  font-size: 16px;
  width: 24px;
  text-align: center;
  flex-shrink: 0;
}

.cat-name {
  flex: 1;
  white-space: nowrap;
}

.cat-count {
  font-size: 12px;
  opacity: 0.6;
}

/* ===== 右侧内容区 ===== */
.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: #F5F0E8;
}

/* 搜索栏 */
.search-bar {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: #fff;
  border-bottom: 1px solid #E8E0D4;
  flex-shrink: 0;
}

.search-bar svg {
  color: #999;
  flex-shrink: 0;
}

.search-bar input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 15px;
  color: #333;
  background: transparent;
}

.search-bar input::placeholder {
  color: #bbb;
}

/* 菜品网格 */
.dish-grid {
  flex: 1;
  overflow-y: auto;
  padding: 16px 20px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  align-content: start;
}

.dish-grid::-webkit-scrollbar {
  width: 6px;
}

.dish-grid::-webkit-scrollbar-thumb {
  background: #D4C8B8;
  border-radius: 3px;
}

/* 菜品卡片 */
.dish-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.25s;
  cursor: pointer;
}

.dish-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.1);
}

.dish-card.sold-out {
  opacity: 0.5;
}

.dish-img-wrap {
  position: relative;
  height: 140px;
  background: linear-gradient(135deg, #f0ebe3, #e8e0d4);
  overflow: hidden;
}

.dish-img-wrap img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.dish-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 700;
  color: #C4B8A8;
  background: linear-gradient(135deg, #f0ebe3, #e8e0d4);
}

/* 标签角标 */
.badge {
  position: absolute;
  padding: 3px 10px;
  font-size: 11px;
  font-weight: 600;
  border-radius: 3px;
  color: #fff;
}

.badge-signature {
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #C4A35A, #D4B86A);
  transform: rotate(-5deg);
}

.badge-hot {
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #C25555, #D46666);
  transform: rotate(-5deg);
}

.badge-soldout {
  top: 8px;
  right: 8px;
  background: rgba(0, 0, 0, 0.6);
}

.badge-qty {
  top: 8px;
  right: 8px;
  background: #2D4A3E;
  border-radius: 50%;
  width: 26px;
  height: 26px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  font-size: 12px;
  font-weight: 700;
}

/* 菜品信息 */
.dish-info {
  padding: 10px 12px;
}

.dish-name-cn {
  font-size: 15px;
  font-weight: 700;
  color: #2c2c2c;
  margin-bottom: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-name-en {
  font-size: 11px;
  color: #999;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dish-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.dish-price {
  font-size: 18px;
  font-weight: 700;
  color: #C25555;
}

.add-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: linear-gradient(135deg, #C4A35A, #D4B86A);
  color: #fff;
  font-size: 20px;
  font-weight: 700;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
  box-shadow: 0 2px 6px rgba(196, 163, 90, 0.3);
}

.add-btn:hover {
  transform: scale(1.1);
  box-shadow: 0 4px 12px rgba(196, 163, 90, 0.4);
}

/* ===== 浮动购物车 ===== */
.cart-fab {
  position: fixed;
  bottom: 24px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 20px;
  background: linear-gradient(135deg, #2D4A3E, #3d6a52);
  border-radius: 28px;
  box-shadow: 0 4px 20px rgba(45, 74, 62, 0.4);
  cursor: pointer;
  z-index: 100;
  transition: all 0.25s;
}

.cart-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(45, 74, 62, 0.5);
}

.cart-fab-icon {
  position: relative;
  color: #C4A35A;
}

.cart-fab-badge {
  position: absolute;
  top: -8px;
  right: -10px;
  min-width: 20px;
  height: 20px;
  background: #C25555;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
}

.cart-fab-info {
  display: flex;
  flex-direction: column;
}

.cart-fab-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.cart-fab-detail {
  font-size: 14px;
  font-weight: 700;
  color: #C4A35A;
}

/* ===== 购物车侧滑面板 ===== */
.cart-panel {
  position: fixed;
  top: 0;
  right: 0;
  width: 420px;
  height: 100vh;
  background: #fff;
  box-shadow: -4px 0 24px rgba(0, 0, 0, 0.15);
  z-index: 200;
  display: flex;
  flex-direction: column;
}

.cart-panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #eee;
  flex-shrink: 0;
}

.cart-panel-header h3 {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin: 0;
}

.cart-close {
  width: 32px;
  height: 32px;
  border: none;
  background: #f5f5f5;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.cart-close:hover {
  background: #eee;
}

.cart-panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px 20px;
}

.cart-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 0;
  border-bottom: 1px solid #f5f5f5;
}

.cart-item-info {
  flex: 1;
}

.cart-item-name {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 2px;
}

.cart-item-price {
  font-size: 13px;
  color: #C25555;
  font-weight: 600;
}

.cart-item-qty {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cart-item-qty button {
  width: 28px;
  height: 28px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 6px;
  font-size: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.cart-item-qty button:hover {
  border-color: #C4A35A;
  color: #C4A35A;
}

.cart-item-qty span {
  font-size: 15px;
  font-weight: 600;
  min-width: 24px;
  text-align: center;
}

.cart-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #ccc;
}

.cart-empty p {
  font-size: 16px;
  margin: 4px 0;
}

.cart-empty-en {
  font-size: 13px !important;
  color: #ddd !important;
}

.cart-panel-footer {
  padding: 16px 20px;
  border-top: 1px solid #eee;
  flex-shrink: 0;
}

.cart-summary {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  font-size: 15px;
  color: #666;
}

.cart-summary-price {
  font-size: 22px;
  font-weight: 700;
  color: #C25555;
}

.cart-actions {
  display: flex;
  gap: 12px;
}

.btn-clear {
  flex: 1;
  padding: 12px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-clear:hover {
  border-color: #C25555;
  color: #C25555;
}

.btn-submit {
  flex: 2;
  padding: 12px;
  border: none;
  background: linear-gradient(135deg, #2D4A3E, #3d6a52);
  color: #fff;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-submit:hover {
  box-shadow: 0 4px 14px rgba(45, 74, 62, 0.3);
}

.cart-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.3);
  z-index: 199;
}

/* ===== 菜品详情弹窗 ===== */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0, 0, 0, 0.5);
  z-index: 300;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-modal {
  width: 50vw;
  max-width: 700px;
  background: #fff;
  border-radius: 16px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.2);
}

.detail-close {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  border-radius: 50%;
  font-size: 18px;
  cursor: pointer;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-img {
  width: 100%;
  height: 260px;
  background: linear-gradient(135deg, #f0ebe3, #e8e0d4);
  overflow: hidden;
}

.detail-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-img-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 72px;
  font-weight: 700;
  color: #C4B8A8;
}

.detail-info {
  padding: 24px;
}

.detail-info h3 {
  font-size: 24px;
  font-weight: 700;
  color: #2c2c2c;
  margin: 0 0 4px;
}

.detail-en {
  font-size: 13px;
  color: #999;
  margin: 0 0 8px;
}

.detail-category {
  font-size: 13px;
  color: #888;
  margin: 0 0 12px;
}

.detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.tag {
  padding: 4px 12px;
  border-radius: 14px;
  font-size: 12px;
  font-weight: 500;
  background: #E8F5E9;
  color: #2D4A3E;
}

.tag.spicy {
  background: #FDECEC;
  color: #C25555;
}

.tag.ingredient {
  background: #FFF8E1;
  color: #B8860B;
}

.detail-price-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.detail-price {
  font-size: 32px;
  font-weight: 700;
  color: #C25555;
}

.detail-qty {
  display: flex;
  align-items: center;
  gap: 12px;
}

.detail-qty button {
  width: 36px;
  height: 36px;
  border: 1px solid #ddd;
  background: #fff;
  border-radius: 8px;
  font-size: 18px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.15s;
}

.detail-qty button:hover {
  border-color: #C4A35A;
  color: #C4A35A;
}

.detail-qty span {
  font-size: 18px;
  font-weight: 700;
  min-width: 32px;
  text-align: center;
}

.detail-remark {
  margin-bottom: 16px;
}

.detail-remark input {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  outline: none;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.detail-remark input:focus {
  border-color: #C4A35A;
}

.detail-add-btn {
  width: 100%;
  padding: 14px;
  border: none;
  background: linear-gradient(135deg, #2D4A3E, #3d6a52);
  color: #fff;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s;
  letter-spacing: 1px;
}

.detail-add-btn:hover {
  box-shadow: 0 4px 16px rgba(45, 74, 62, 0.3);
}

.detail-links {
  display: flex;
  gap: 12px;
  margin-top: 12px;
  justify-content: center;
}

.search-link {
  padding: 6px 16px;
  border: 1px solid #e0e0e0;
  border-radius: 16px;
  font-size: 12px;
  color: #888;
  text-decoration: none;
  transition: all 0.2s;
}

.search-link:hover {
  border-color: #C4A35A;
  color: #C4A35A;
}

/* ===== 动画 ===== */
.cart-slide-enter-active,
.cart-slide-leave-active {
  transition: transform 0.3s ease;
}

.cart-slide-enter-from,
.cart-slide-leave-to {
  transform: translateX(100%);
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.modal-enter-active,
.modal-leave-active {
  transition: all 0.3s ease;
}

.modal-enter-from {
  opacity: 0;
  transform: scale(0.9);
}

.modal-leave-to {
  opacity: 0;
  transform: scale(0.95);
}
</style>

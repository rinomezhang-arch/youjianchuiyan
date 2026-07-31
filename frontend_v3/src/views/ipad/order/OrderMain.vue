<template>
  <div class="order-main">
    <!-- 左侧分类栏 -->
    <aside class="category-sidebar">
      <div class="sidebar-header">
        <span class="sidebar-title">菜品分类</span>
        <span class="sidebar-sub">Categories</span>
      </div>
      <div class="category-list">
        <button
          v-for="cat in categories"
          :key="cat.category_id"
          :class="['cat-item', { active: activeCat === cat.category_id }]"
          @click="selectCategory(cat)"
        >
          <span class="cat-name">{{ cat.dish_category }}</span>
          <span class="cat-count">{{ cat.count || 0 }}</span>
        </button>
      </div>
      <div class="sidebar-footer">
        <button class="back-btn" @click="router.push('/ipad/home')">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="15 18 9 12 15 6"/></svg>
          返回桌台
        </button>
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
          :class="['dish-card', { 'sold-out': dish.is_sold_out }]"
          @click="!dish.is_sold_out && showDishDetail(dish)"
        >
          <div class="dish-img">
            <img v-if="dish.cover_img || dish.image_url" :src="dish.cover_img || dish.image_url" :alt="dish.dish_name" />
            <div v-else class="dish-img-placeholder">{{ dish.dish_name?.charAt(0) }}</div>
            <div v-if="dish.is_sold_out" class="sold-out-badge">沽清</div>
          </div>
          <div class="dish-info">
            <div class="dish-name">{{ dish.dish_name }}</div>
            <div class="dish-tags">
              <span v-if="dish.spicy_level >= 1" class="tag spicy">{{ '🌶'.repeat(dish.spicy_level) }}</span>
              <span v-if="dish.dish_tag || dish.tag" class="tag recommend">{{ dish.dish_tag || dish.tag }}</span>
            </div>
            <div class="dish-price">¥{{ Number(dish.sale_price).toFixed(0) }}</div>
          </div>
          <!-- 快速加菜按钮 -->
          <button v-if="!dish.is_sold_out" class="quick-add" @click.stop="quickAddDish(dish)">+</button>
        </div>
      </div>
    </div>

    <!-- 右下角购物车浮动按钮 -->
    <div class="cart-fab" @click="showCart = true">
      <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>
      <span v-if="ipad.cartCount > 0" class="cart-badge">{{ ipad.cartCount }}</span>
      <span v-if="ipad.cartCount > 0" class="cart-total">¥{{ ipad.cartTotal.toFixed(0) }}</span>
    </div>

    <!-- 购物车侧滑面板 -->
    <Transition name="cart-slide">
      <div v-if="showCart" class="cart-panel">
        <div class="cart-header">
          <h3>已点菜品 · Ordered</h3>
          <button class="cart-close" @click="showCart = false">×</button>
        </div>
        <div class="cart-body" v-if="ipad.cartItems.length">
          <div v-for="item in ipad.cartItems" :key="item.dish_id" class="cart-item">
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.dish_name }}</div>
              <div class="cart-item-price">¥{{ Number(item.sale_price || item.unit_price).toFixed(0) }}</div>
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
        <div class="cart-footer" v-if="ipad.cartItems.length">
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
          <div class="detail-img">
            <img v-if="detailDish?.cover_img || detailDish?.image_url" :src="detailDish.cover_img || detailDish.image_url" />
            <div v-else class="detail-img-placeholder">{{ detailDish?.dish_name?.charAt(0) }}</div>
          </div>
          <div class="detail-info">
            <h3>{{ detailDish?.dish_name }}</h3>
            <p class="detail-category">{{ detailDish?.dish_category }}</p>
            <div class="detail-tags">
              <span v-if="detailDish?.spicy_level" class="tag spicy">{{ '🌶'.repeat(detailDish.spicy_level) }}</span>
              <span v-if="detailDish?.cooking_method" class="tag">{{ detailDish.cooking_method }}</span>
              <span v-if="detailDish?.taste" class="tag">{{ detailDish.taste }}</span>
            </div>
            <p v-if="detailDish?.main_ingredients" class="detail-ingredients">食材：{{ detailDish.main_ingredients }}</p>
            <div class="detail-price">¥{{ Number(detailDish?.sale_price || 0).toFixed(0) }}</div>
            <div class="detail-qty">
              <button @click="detailQty = Math.max(1, detailQty - 1)">−</button>
              <span>{{ detailQty }}</span>
              <button @click="detailQty++">+</button>
            </div>
            <div class="detail-remark">
              <input v-model="detailRemark" placeholder="口味备注 · Taste note (可选)" />
            </div>
            <button class="detail-add-btn" @click="addFromDetail">加入购物车 · Add to Cart</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadDishCategory, ipadDishList, ipadDishSearch, ipadOrderAdd, ipadOrderSendKitchen } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const ipad = useIpadStore()

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

const currentCatName = computed(() => {
  if (activeCat.value === 'all') return '全部菜品'
  const cat = categories.value.find(c => c.category_id === activeCat.value)
  return cat?.dish_category || '全部菜品'
})

const currentCatNameEn = computed(() => {
  if (activeCat.value === 'all') return 'All Dishes'
  const cat = categories.value.find(c => c.category_id === activeCat.value)
  return cat?.dish_category_en || 'All Dishes'
})

const displayDishes = computed(() => {
  if (searchKeyword.value) return dishes.value.filter(d => d.dish_name?.includes(searchKeyword.value))
  return dishes.value
})

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
  // 同时提交后端
  submitAddDish(dish.dish_id || dish.id, 1)
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
    { dish_id: 'D001', dish_name: '红烧肉', dish_category: '热菜', sale_price: 68, spicy_level: 0, cooking_method: '红烧', taste: '咸甜', is_sold_out: 0, dish_tag: '招牌' },
    { dish_id: 'D002', dish_name: '清蒸鲈鱼', dish_category: '热菜', sale_price: 88, spicy_level: 0, cooking_method: '清蒸', taste: '鲜美', is_sold_out: 0, dish_tag: '推荐' },
    { dish_id: 'D003', dish_name: '蒜蓉粉丝蒸扇贝', dish_category: '热菜', sale_price: 128, spicy_level: 0, is_sold_out: 0, dish_tag: '热门' },
    { dish_id: 'D004', dish_name: '凉拌黄瓜', dish_category: '凉菜', sale_price: 18, spicy_level: 1, is_sold_out: 0 },
    { dish_id: 'D005', dish_name: '老醋花生', dish_category: '凉菜', sale_price: 22, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D006', dish_name: '皮蛋豆腐', dish_category: '凉菜', sale_price: 28, spicy_level: 1, is_sold_out: 0 },
    { dish_id: 'D007', dish_name: '酸辣汤', dish_category: '汤类', sale_price: 38, spicy_level: 2, is_sold_out: 0 },
    { dish_id: 'D008', dish_name: '番茄蛋汤', dish_category: '汤类', sale_price: 28, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D009', dish_name: '蛋炒饭', dish_category: '主食', sale_price: 22, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D010', dish_name: '手工水饺', dish_category: '主食', sale_price: 32, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D011', dish_name: '酸梅汤', dish_category: '饮品', sale_price: 15, spicy_level: 0, is_sold_out: 0 },
    { dish_id: 'D012', dish_name: '鲜榨橙汁', dish_category: '饮品', sale_price: 25, spicy_level: 0, is_sold_out: 1 },
    { dish_id: 'D013', dish_name: '迎春接福宴', dish_category: '套餐', sale_price: 988, spicy_level: 0, is_sold_out: 0, dish_tag: '套餐' },
    { dish_id: 'D014', dish_name: '阖家团圆宴', dish_category: '套餐', sale_price: 1288, spicy_level: 0, is_sold_out: 0, dish_tag: '套餐' },
    { dish_id: 'D015', dish_name: '水煮牛肉', dish_category: '热菜', sale_price: 78, spicy_level: 3, is_sold_out: 0, dish_tag: '热门' },
    { dish_id: 'D016', dish_name: '干锅花菜', dish_category: '热菜', sale_price: 48, spicy_level: 2, is_sold_out: 0 },
    { dish_id: 'D017', dish_name: '铁板牛柳', dish_category: '热菜', sale_price: 98, spicy_level: 1, is_sold_out: 0, dish_tag: '招牌' },
    { dish_id: 'D018', dish_name: '白灼虾', dish_category: '热菜', sale_price: 168, spicy_level: 0, is_sold_out: 0, dish_tag: '推荐' },
  ]
  if (catId === 'all') return all
  const catMap = { 'hot': '热菜', 'cold': '凉菜', 'soup': '汤类', 'staple': '主食', 'drink': '饮品', 'package': '套餐' }
  const catName = catMap[catId] || Object.values(catMap).find(v => v === catId)
  return all.filter(d => d.dish_category === catName || d.dish_category === catId)
}

onMounted(async () => {
  try {
    const res = await ipadDishCategory()
    if (res.code === 200 && res.data?.length) {
      categories.value = [{ category_id: 'all', dish_category: '全部', dish_category_en: 'All' }, ...res.data]
    } else throw new Error('empty')
  } catch {
    categories.value = [
      { category_id: 'all', dish_category: '全部', dish_category_en: 'All' },
      { category_id: 'cold', dish_category: '凉菜', dish_category_en: 'Cold' },
      { category_id: 'hot', dish_category: '热菜', dish_category_en: 'Hot' },
      { category_id: 'soup', dish_category: '汤类', dish_category_en: 'Soup' },
      { category_id: 'staple', dish_category: '主食', dish_category_en: 'Staple' },
      { category_id: 'drink', dish_category: '饮品', dish_category_en: 'Drinks' },
      { category_id: 'package', dish_category: '套餐', dish_category_en: 'Package' },
    ]
  }
  activeCat.value = 'all'
  loadDishes()
})
</script>

<style scoped>
.order-main {
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
.sidebar-title { display: block; font-size: 16px; font-weight: 700; color: #FAF8F5; letter-spacing: 2px; font-family: var(--font-family); }
.sidebar-sub { display: block; font-size: 11px; color: rgba(196, 163, 90, 0.6); letter-spacing: 1px; margin-top: 2px; }

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

.sidebar-footer { padding: 12px 16px; border-top: 1px solid rgba(196, 163, 90, 0.15); }
.back-btn {
  display: flex; align-items: center; gap: 8px; width: 100%;
  padding: 10px; border: 1px solid rgba(196, 163, 90, 0.2);
  border-radius: var(--radius-md); background: transparent;
  color: rgba(250, 248, 245, 0.6); font-size: 13px; cursor: pointer;
  transition: all 0.2s; font-family: var(--font-family);
}
.back-btn:hover { border-color: rgba(196, 163, 90, 0.5); color: #FAF8F5; }

/* ===== 右侧主区域 ===== */
.content-area { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

.content-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 24px; background: var(--color-card);
  border-bottom: 1px solid var(--color-border); flex-shrink: 0;
}
.content-title { font-size: 20px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; display: inline; margin-right: 8px; }
.content-sub { font-size: 12px; color: var(--color-text-muted); letter-spacing: 1px; }
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
  gap: 16px; padding: 20px 24px;
  align-content: start;
}

.dish-card {
  background: var(--color-card); border: 1px solid var(--color-border);
  border-radius: var(--radius-lg); overflow: hidden;
  cursor: pointer; transition: all 0.25s; position: relative;
}
.dish-card:hover { transform: translateY(-3px); box-shadow: var(--shadow-lg); border-color: var(--color-primary); }
.dish-card.sold-out { opacity: 0.5; cursor: not-allowed; }
.dish-card.sold-out:hover { transform: none; box-shadow: none; }

.dish-img {
  width: 100%; aspect-ratio: 4/3; background: var(--color-bg-alt);
  display: flex; align-items: center; justify-content: center; overflow: hidden;
  position: relative;
}
.dish-img img { width: 100%; height: 100%; object-fit: cover; }
.dish-img-placeholder { font-size: 32px; font-weight: 700; color: var(--color-border); font-family: var(--font-family); }
.sold-out-badge {
  position: absolute; top: 8px; right: 8px;
  padding: 2px 8px; background: var(--color-danger); color: white;
  border-radius: 4px; font-size: 11px; font-weight: 600;
}

.dish-info { padding: 10px 12px; }
.dish-name { font-size: 14px; font-weight: 700; color: var(--color-text); letter-spacing: 0.5px; margin-bottom: 4px; }
.dish-tags { display: flex; gap: 4px; margin-bottom: 4px; flex-wrap: wrap; }
.tag { font-size: 10px; padding: 1px 6px; border-radius: 3px; font-weight: 500; }
.tag.spicy { background: #FDECEC; color: #C25555; }
.tag.recommend { background: #FEF6E8; color: #A4833A; }
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
.detail-qty { display: flex; align-items: center; gap: 16px; margin-bottom: 16px; }
.detail-qty button { width: 40px; height: 40px; border-radius: 50%; border: 1px solid var(--color-border); background: var(--color-card); font-size: 20px; cursor: pointer; display: flex; align-items: center; justify-content: center; transition: all 0.2s; }
.detail-qty button:hover { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.detail-qty span { font-size: 24px; font-weight: 700; min-width: 40px; text-align: center; }
.detail-remark { margin-bottom: 20px; }
.detail-remark input { width: 100%; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; outline: none; }
.detail-remark input:focus { border-color: var(--color-primary); box-shadow: 0 0 0 2px rgba(45,74,62,0.1); }
.detail-add-btn {
  width: 100%; padding: 14px; border: none; border-radius: var(--radius-md);
  background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light));
  color: white; font-size: 16px; font-weight: 700; cursor: pointer;
  letter-spacing: 2px; transition: all 0.25s;
}
.detail-add-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }

/* 动画 */
.modal-enter-active, .modal-leave-active { transition: opacity 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
.modal-enter-from .detail-modal { transform: scale(0.95); }
.cart-slide-enter-active, .cart-slide-leave-active { transition: transform 0.3s ease; }
.cart-slide-enter-from, .cart-slide-leave-to { transform: translateX(100%); }
.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }</style>

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

    <!-- 提交订单弹窗：选桌台 + 填写信息 -->
    <Transition name="modal">
      <div v-if="showSubmitModal" class="modal-overlay submit-overlay" @click.self="showSubmitModal = false">
        <div class="submit-modal">
          <div class="submit-header">
            <h3>确认下单 · Confirm Order</h3>
            <button class="submit-close" @click="showSubmitModal = false">×</button>
          </div>
          <div class="submit-body">
            <!-- 已点菜品摘要 -->
            <div class="submit-section">
              <div class="section-label">已点菜品 · Dishes ({{ ipad.cartCount }}件)</div>
              <div class="dish-summary">
                <div v-for="item in ipad.cartItems" :key="item.dish_id" class="summary-item">
                  <span class="summary-name">{{ item.dish_name }}</span>
                  <span class="summary-qty">×{{ item.dish_quantity }}</span>
                  <span class="summary-price">¥{{ (Number(item.sale_price || item.unit_price) * item.dish_quantity).toFixed(0) }}</span>
                </div>
                <div class="summary-total">
                  <span>合计</span>
                  <span class="total-price">¥{{ ipad.cartTotal.toFixed(2) }}</span>
                </div>
              </div>
            </div>

            <!-- 选择桌台 -->
            <div class="submit-section">
              <div class="section-label">选择桌台 · Select Table</div>
              <div class="table-select-grid">
                <div
                  v-for="t in availableTables"
                  :key="t.table_id"
                  :class="['table-select-card', { selected: submitForm.table_id === t.table_id }]"
                  @click="selectTable(t)"
                >
                  <div class="tsc-name">{{ t.table_name || t.table_number }}</div>
                  <div class="tsc-area">{{ t.table_area }}</div>
                  <div class="tsc-cap">{{ t.table_capacity }}人</div>
                </div>
                <div v-if="availableTables.length === 0" class="no-tables">暂无空闲桌台</div>
              </div>
            </div>

            <!-- 填写信息 -->
            <div class="submit-section">
              <div class="section-label">用餐信息 · Guest Info</div>
              <div class="form-grid">
                <div class="form-item">
                  <label>用餐人数 · Guests</label>
                  <div class="qty-control">
                    <button @click="submitForm.guest_count = Math.max(1, submitForm.guest_count - 1)">−</button>
                    <span>{{ submitForm.guest_count }}</span>
                    <button @click="submitForm.guest_count++">+</button>
                  </div>
                </div>
                <div class="form-item">
                  <label>客人姓名 · Name</label>
                  <input v-model="submitForm.customer_name" placeholder="请输入客人姓名" />
                </div>
                <div class="form-item">
                  <label>联系电话 · Phone</label>
                  <input v-model="submitForm.customer_phone" placeholder="请输入手机号" maxlength="11" />
                </div>
                <div class="form-item">
                  <label>宴会类别 · Type</label>
                  <select v-model="submitForm.booking_type">
                    <option value="normal">零点 · Normal</option>
                    <option value="banquet">宴会 · Banquet</option>
                    <option value="business">商务宴 · Business</option>
                  </select>
                </div>
                <div class="form-item full-width">
                  <label>备注 · Remark</label>
                  <input v-model="submitForm.remark" placeholder="特殊要求（可选）" />
                </div>
              </div>
            </div>
          </div>
          <div class="submit-actions">
            <button class="btn-cancel" @click="showSubmitModal = false">取消 · Cancel</button>
            <button class="btn-confirm" :disabled="!submitForm.table_id || submitting" @click="confirmSubmitOrder">
              {{ submitting ? '提交中...' : '确认下单 · Confirm' }}
            </button>
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
import { ipadDishCategory, ipadDishList, ipadDishSearch, ipadTableAll, ipadOrderSubmit } from '@/api/ipad'
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

// 提交订单弹窗状态
const showSubmitModal = ref(false)
const availableTables = ref([])
const submitting = ref(false)
const submitForm = ref({
  table_id: null,
  guest_count: 4,
  customer_name: '',
  customer_phone: '',
  booking_type: 'normal',
  remark: ''
})

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
  } catch (error) {
    dishes.value = []
    ElMessage.error(error.response?.data?.message || '菜品加载失败，请检查网络后重试')
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
}

function addFromDetail() {
  ipad.addToCart({
    dish_id: detailDish.value.dish_id || detailDish.value.id,
    dish_name: detailDish.value.dish_name,
    sale_price: detailDish.value.sale_price,
    unit_price: detailDish.value.sale_price,
    dish_quantity: detailQty.value,
    dish_note: detailRemark.value || undefined
  })
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
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '加菜失败，请重试')
    throw error
  }
}

async function submitToKitchen() {
  if (ipad.cartItems.length === 0) {
    ElMessage.warning('请先点菜')
    return
  }
  // 加载空闲桌台列表
  try {
    const res = await ipadTableAll()
    if (res.code === 200) {
      availableTables.value = (res.data || []).filter(t =>
        t.table_status === 'idle' || t.table_status === 'available'
      )
    }
  } catch (error) {
    availableTables.value = []
    ElMessage.error(error.response?.data?.message || '桌台加载失败，暂不能下单')
    return
  }
  // 重置表单
  submitForm.value = {
    table_id: null,
    guest_count: 4,
    customer_name: '',
    booking_type: 'normal',
    remark: ''
  }
  showSubmitModal.value = true
  showCart.value = false
}

function selectTable(t) {
  submitForm.value.table_id = t.table_id
}

async function confirmSubmitOrder() {
  if (!submitForm.value.table_id) {
    ElMessage.warning('请选择桌台')
    return
  }
  if (ipad.cartItems.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  submitting.value = true
  try {
    const dishes = ipad.cartItems.map(item => ({
      dish_id: item.dish_id,
      dish_quantity: item.dish_quantity,
      dish_note: item.dish_note || undefined
    }))
    const res = await ipadOrderSubmit({
      table_id: submitForm.value.table_id,
      guest_count: submitForm.value.guest_count,
      customer_name: submitForm.value.customer_name || '散客',
      customer_phone: submitForm.value.customer_phone || undefined,
      booking_type: submitForm.value.booking_type,
      remark: submitForm.value.remark,
      dishes
    })
    if (res.code === 200) {
      ElMessage.success(`下单成功！订单号 ${res.data.booking_id}，已提交后厨`)
      showSubmitModal.value = false
      ipad.clearCart()
      // 跳转到桌台主页
      router.push('/ipad/home')
    } else {
      ElMessage.error(res.msg || '下单失败')
    }
  } catch (error) {
    ElMessage.error(error.response?.data?.message || '下单失败，请确认桌台状态后重试')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await ipadDishCategory()
    if (res.code === 200 && res.data?.length) {
      categories.value = [{ category_id: 'all', dish_category: '全部', dish_category_en: 'All' }, ...res.data]
    } else throw new Error('empty')
  } catch (error) {
    categories.value = [{ category_id: 'all', dish_category: '全部', dish_category_en: 'All' }]
    ElMessage.error(error.response?.data?.message || '菜品分类加载失败')
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
.fade-enter-from, .fade-leave-to { opacity: 0; }

/* ===== 提交订单弹窗 ===== */
.submit-overlay { z-index: 2000; }
.submit-modal {
  background: var(--color-card);
  border-radius: 16px;
  width: 50vw; max-width: 720px; max-height: 90vh;
  display: flex; flex-direction: column;
  box-shadow: 0 20px 60px rgba(0,0,0,0.2);
  overflow: hidden;
}
.submit-header {
  padding: 16px 24px;
  display: flex; align-items: center; justify-content: space-between;
  border-bottom: 1px solid var(--color-border);
  background: linear-gradient(135deg, #2D4A3E 0%, #1D3A2E 100%);
}
.submit-header h3 { font-size: 18px; font-weight: 700; color: #fff; letter-spacing: 1px; }
.submit-close { background: none; border: none; color: #fff; font-size: 24px; cursor: pointer; padding: 0 4px; }
.submit-body { padding: 20px 24px; overflow-y: auto; flex: 1; }
.submit-section { margin-bottom: 20px; }
.section-label {
  font-size: 13px; font-weight: 600; color: var(--color-text-secondary);
  margin-bottom: 10px; padding-left: 8px;
  border-left: 3px solid var(--color-primary);
}

/* 菜品摘要 */
.dish-summary {
  background: rgba(45, 74, 62, 0.03);
  border-radius: 8px; padding: 12px;
  max-height: 160px; overflow-y: auto;
}
.summary-item {
  display: flex; align-items: center; gap: 12px;
  padding: 6px 0; border-bottom: 1px dashed rgba(0,0,0,0.05);
  font-size: 13px;
}
.summary-name { flex: 1; color: var(--color-text); }
.summary-qty { color: var(--color-text-secondary); min-width: 40px; text-align: center; }
.summary-price { color: var(--color-primary); font-weight: 600; min-width: 60px; text-align: right; }
.summary-total {
  display: flex; justify-content: space-between;
  padding-top: 8px; margin-top: 4px;
  border-top: 2px solid var(--color-primary);
  font-size: 15px; font-weight: 700;
}
.total-price { color: #c0392b; }

/* 桌台选择网格 */
.table-select-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 8px;
  max-height: 200px; overflow-y: auto;
  padding: 4px;
}
.table-select-card {
  border: 2px solid var(--color-border);
  border-radius: 8px;
  padding: 8px 6px;
  text-align: center;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--color-card);
}
.table-select-card:hover { border-color: var(--color-primary); background: rgba(45,74,62,0.04); }
.table-select-card.selected {
  border-color: #C4A35A;
  background: rgba(196, 163, 90, 0.1);
  box-shadow: 0 0 0 2px rgba(196, 163, 90, 0.3);
}
.tsc-name { font-size: 14px; font-weight: 700; color: var(--color-text); }
.tsc-area { font-size: 11px; color: var(--color-text-secondary); margin-top: 2px; }
.tsc-cap { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.no-tables { grid-column: 1 / -1; text-align: center; padding: 20px; color: var(--color-text-muted); font-size: 13px; }

/* 表单 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}
.form-item { display: flex; flex-direction: column; gap: 6px; }
.form-item.full-width { grid-column: 1 / -1; }
.form-item label { font-size: 13px; font-weight: 600; color: var(--color-text-secondary); }
.form-item input, .form-item select {
  height: 32px; padding: 0 12px;
  border: 1px solid var(--color-border); border-radius: 6px;
  font-size: 13px; font-family: var(--font-family);
  background: var(--color-card); color: var(--color-text);
}
.form-item input:focus, .form-item select:focus {
  outline: none; border-color: var(--color-primary);
}
.qty-control { display: flex; align-items: center; gap: 12px; }
.qty-control button {
  width: 32px; height: 32px; border-radius: 50%;
  border: 1px solid var(--color-border); background: var(--color-card);
  font-size: 18px; cursor: pointer; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center;
}
.qty-control button:hover { background: var(--color-primary); color: white; border-color: var(--color-primary); }

/* 操作按钮 */
.submit-actions {
  padding: 16px 24px;
  display: flex; justify-content: flex-end; gap: 12px;
  border-top: 1px solid var(--color-border);
}
.submit-actions .btn-cancel {
  padding: 8px 20px; border-radius: 6px;
  border: 1px solid var(--color-border); background: var(--color-card);
  color: var(--color-text-secondary); font-size: 14px; cursor: pointer;
  transition: all 0.2s;
}
.submit-actions .btn-cancel:hover { border-color: var(--color-text); color: var(--color-text); }
.submit-actions .btn-confirm {
  padding: 8px 24px; border-radius: 6px;
  border: none; background: linear-gradient(135deg, #2D4A3E, #1D3A2E);
  color: #fff; font-size: 14px; font-weight: 600; cursor: pointer;
  transition: all 0.2s;
}
.submit-actions .btn-confirm:hover:not(:disabled) { transform: translateY(-1px); box-shadow: 0 4px 12px rgba(45,74,62,0.3); }
.submit-actions .btn-confirm:disabled { opacity: 0.5; cursor: not-allowed; }
</style>

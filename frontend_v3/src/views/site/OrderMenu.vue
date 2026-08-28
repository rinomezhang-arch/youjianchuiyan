<template>
  <div class="order-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', en: 'Home', to: '/' }, { label: '门店选择', en: 'Restaurants', to: '/stores' }, { label: store.storeName, to: `/stores/${store.storeId}` }, { label: '我要点菜' }]" />

    <div v-if="loading" class="loading-full">菜单加载中...</div>

    <div v-else class="order-layout">
      <!-- 左侧分类 -->
      <aside class="cat-sidebar">
        <div class="cat-sidebar-head">
          <p class="csh-cn">{{ store.storeName }}</p>
          <p class="csh-en">Select Dishes</p>
        </div>
        <button
          v-for="c in categoryList"
          :key="c.name"
          class="cat-btn"
          :class="{ active: activeCat === c.name }"
          @click="activeCat = c.name"
        >
          <span>{{ c.name }}</span>
          <span class="cat-btn-count">{{ c.count }}</span>
        </button>
      </aside>

      <!-- 中间菜品区：拖拽或点加号加入购物篮，加入后从这里消失 -->
      <main
        class="dish-area"
        @dragover.prevent
        @drop="onDropToGrid"
      >
        <div class="dish-area-head">
          <h2>{{ activeCat }}</h2>
          <input v-model="searchKeyword" class="search-input" placeholder="搜索菜品 · Search dishes" />
        </div>
        <div v-if="visibleDishes.length === 0" class="dish-empty">这个分类的菜都已加入购物篮啦</div>
        <div v-else class="dish-grid">
          <div
            v-for="d in visibleDishes"
            :key="d.dishId"
            class="dish-tile"
            draggable="true"
            @dragstart="onDragStart($event, d)"
          >
            <div class="dish-tile-body">
              <h4>{{ d.dishName }}</h4>
              <span class="dish-tile-cat">{{ d.dishCategory }}</span>
              <span class="dish-tile-price">¥{{ formatPrice(d.salePrice) }}</span>
            </div>
            <button class="dish-tile-add" @click="addToCart(d)" title="加入购物篮">+</button>
          </div>
        </div>
      </main>

      <!-- 右侧购物篮：拖入加菜，数量/备注/删除都在这里 -->
      <aside
        class="cart-sidebar"
        :class="{ 'drop-hover': dragOverCart }"
        @dragover.prevent="dragOverCart = true"
        @dragleave="dragOverCart = false"
        @drop="onDropToCart"
      >
        <div class="cart-head">
          <p class="cart-head-cn">已选菜品 · Cart</p>
          <p class="cart-head-en">{{ cart.length }} 道菜 · ¥{{ cartTotal }}</p>
        </div>

        <div class="cart-list">
          <div v-if="cart.length === 0" class="cart-empty">
            <p>还没有选菜</p>
            <p class="cart-empty-en">拖拽菜品到这里，或点击"+"加入</p>
          </div>
          <div
            v-for="item in cart"
            :key="item.dishId"
            class="cart-item"
            draggable="true"
            @dragstart="onCartDragStart($event, item)"
          >
            <div class="cart-item-top">
              <span class="cart-item-name">{{ item.dishName }}</span>
              <i class="cart-item-del" @click="removeFromCart(item.dishId)">✕</i>
            </div>
            <div class="cart-item-mid">
              <span class="cart-item-price">¥{{ formatPrice(item.salePrice) }}</span>
              <div class="cart-item-qty">
                <button @click="changeQty(item, -1)">−</button>
                <span>{{ item.qty }}</span>
                <button @click="changeQty(item, 1)">+</button>
              </div>
            </div>
            <input v-model="item.note" class="cart-item-note" placeholder="备注：如微辣、不要香菜" />
          </div>
        </div>

        <div class="cart-footer">
          <div class="cart-total-row">
            <span>合计 Subtotal</span>
            <span class="cart-total-price">¥{{ cartTotal }}</span>
          </div>

          <form class="contact-form" @submit.prevent="submitOrder">
            <input v-model="form.customerName" placeholder="您的姓名 *" required />
            <input v-model="form.customerPhone" placeholder="手机号 *" required />
            <div class="contact-row">
              <input v-model="form.preferredDate" type="date" />
              <input v-model.number="form.guestCount" type="number" min="1" placeholder="人数" />
            </div>
            <button class="btn-gold submit-btn" type="submit" :disabled="submitting">
              {{ submitting ? '提交中...' : (submitted ? '已提交 ✓' : '提交预定申请') }}
            </button>
          </form>
        </div>
      </aside>
    </div>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import request from '@/utils/request'

const route = useRoute()
const store = ref({ storeId: route.params.storeId, storeName: '门店' })
const allDishes = ref([])
const loading = ref(true)
const activeCat = ref('全部')
const searchKeyword = ref('')
const cart = ref([])
const dragOverCart = ref(false)
const submitting = ref(false)
const submitted = ref(false)

const form = reactive({
  customerName: '',
  customerPhone: '',
  preferredDate: '',
  guestCount: null
})

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

// 已经在购物篮里的菜，从中间展示区里消失——"此消彼长"
const cartIds = computed(() => new Set(cart.value.map(i => i.dishId)))
const remainingDishes = computed(() => allDishes.value.filter(d => !cartIds.value.has(d.dishId)))

const categoryList = computed(() => {
  const cats = [...new Set(allDishes.value.map(d => d.dishCategory).filter(Boolean))]
  const list = [{ name: '全部', count: remainingDishes.value.length }]
  cats.forEach(c => {
    list.push({ name: c, count: remainingDishes.value.filter(d => d.dishCategory === c).length })
  })
  return list
})

const visibleDishes = computed(() => {
  let list = activeCat.value === '全部' ? remainingDishes.value : remainingDishes.value.filter(d => d.dishCategory === activeCat.value)
  if (searchKeyword.value.trim()) {
    const kw = searchKeyword.value.trim()
    list = list.filter(d => d.dishName.includes(kw))
  }
  return list
})

const cartTotal = computed(() => cart.value.reduce((s, i) => s + Number(i.salePrice || 0) * i.qty, 0).toFixed(0))

function addToCart(d) {
  if (cartIds.value.has(d.dishId)) return
  cart.value.push({ dishId: d.dishId, dishName: d.dishName, salePrice: d.salePrice, qty: 1, note: '' })
}
function removeFromCart(dishId) {
  cart.value = cart.value.filter(i => i.dishId !== dishId)
}
function changeQty(item, delta) {
  item.qty = Math.max(1, item.qty + delta)
}

// 拖拽：中间菜品卡片 → 拖进购物篮 = 加菜；购物篮里的菜 → 拖回中间区域 = 移除
function onDragStart(e, dish) {
  e.dataTransfer.setData('text/dish-id', dish.dishId)
  e.dataTransfer.effectAllowed = 'copy'
}
function onCartDragStart(e, item) {
  e.dataTransfer.setData('text/cart-dish-id', item.dishId)
  e.dataTransfer.effectAllowed = 'move'
}
function onDropToCart(e) {
  dragOverCart.value = false
  const dishId = e.dataTransfer.getData('text/dish-id')
  if (dishId) {
    const d = allDishes.value.find(x => x.dishId === dishId)
    if (d) addToCart(d)
  }
}
function onDropToGrid(e) {
  const dishId = e.dataTransfer.getData('text/cart-dish-id')
  if (dishId) removeFromCart(dishId)
}

async function loadStore() {
  const res = await request.get('/api/public/stores')
  const list = (res.data || []).map(s => ({
    storeId: s.store_id ?? s.storeId,
    storeName: s.store_name ?? s.storeName
  }))
  const found = list.find(s => String(s.storeId) === String(route.params.storeId))
  if (found) store.value = found
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await request.get('/api/public/menu/full', { params: { storeId: route.params.storeId } })
    allDishes.value = (res.data || []).map(d => ({
      dishId: d.dish_id ?? d.dishId,
      dishName: d.dish_name ?? d.dishName,
      dishCategory: d.dish_category ?? d.dishCategory,
      salePrice: d.sale_price ?? d.salePrice
    }))
  } catch (e) {
    allDishes.value = []
  } finally {
    loading.value = false
  }
}

async function submitOrder() {
  submitting.value = true
  try {
    await request.post('/api/public/booking-inquiry', {
      storeId: store.value.storeId,
      customerName: form.customerName,
      customerPhone: form.customerPhone,
      preferredDate: form.preferredDate || undefined,
      guestCount: form.guestCount,
      remark: cart.value.map(i => i.note).filter(Boolean).join('；'),
      selectedDishes: cart.value.map(i => ({ dishName: i.dishName, salePrice: i.salePrice, qty: i.qty, note: i.note }))
    })
    submitted.value = true
  } catch (e) {
    alert(e?.message || '提交失败，请稍后重试或直接致电门店')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadStore()
  await loadDishes()
})
</script>

<style scoped>
.order-page {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #7A7A72;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif;
  color: var(--ink);
  background: var(--ivory);
  min-height: 100vh;
}
.loading-full { padding: 200px 0; text-align: center; color: var(--muted); }

.order-layout {
  display: grid;
  grid-template-columns: 200px 1fr 340px;
  min-height: 70vh;
  padding-top: 88px;
}

/* 左侧分类 */
.cat-sidebar { background: var(--forest); padding: 24px 0; }
.cat-sidebar-head { padding: 0 20px 20px; border-bottom: 1px solid rgba(255,255,255,0.1); margin-bottom: 12px; }
.csh-cn { color: #fff; font-size: 15px; font-weight: 700; margin: 0 0 4px; }
.csh-en { color: rgba(255,255,255,0.5); font-size: 10px; letter-spacing: 1px; margin: 0; }
.cat-btn {
  display: flex; justify-content: space-between; align-items: center; width: 100%;
  background: none; border: none; border-left: 3px solid transparent;
  color: rgba(255,255,255,0.75); padding: 12px 20px; font-size: 13.5px; cursor: pointer; text-align: left;
}
.cat-btn:hover { background: rgba(255,255,255,0.06); }
.cat-btn.active { background: rgba(184,147,90,0.18); border-left-color: var(--gold); color: #fff; font-weight: 700; }
.cat-btn-count { font-size: 11px; opacity: 0.6; }

/* 中间菜品区 */
.dish-area { padding: 24px 28px; overflow-y: auto; max-height: calc(100vh - 88px); }
.dish-area-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.dish-area-head h2 { font-size: 20px; color: var(--forest); margin: 0; }
.search-input { border: 1px solid #DDD3B8; border-radius: 4px; padding: 8px 14px; font-size: 13px; width: 220px; }
.dish-empty { text-align: center; color: var(--muted); padding: 60px 0; }
.dish-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(200px, 1fr)); gap: 14px; }
.dish-tile {
  background: #fff; border: 1px solid #EDE7D9; border-radius: 6px; padding: 14px;
  display: flex; justify-content: space-between; align-items: flex-start; cursor: grab; position: relative;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.dish-tile:hover { box-shadow: 0 4px 16px rgba(0,0,0,0.06); border-color: var(--gold); }
.dish-tile:active { cursor: grabbing; }
.dish-tile-body { display: flex; flex-direction: column; gap: 4px; }
.dish-tile-body h4 { font-size: 14px; color: var(--forest); margin: 0; }
.dish-tile-cat { font-size: 11px; color: var(--muted); }
.dish-tile-price { font-size: 14px; font-weight: 700; color: var(--forest); margin-top: 2px; }
.dish-tile-add {
  width: 28px; height: 28px; border-radius: 50%; border: 1px solid var(--forest); background: #fff;
  color: var(--forest); font-size: 16px; cursor: pointer; flex-shrink: 0; display: flex; align-items: center; justify-content: center;
}
.dish-tile-add:hover { background: var(--forest); color: #fff; }

/* 右侧购物篮 */
.cart-sidebar { background: #fff; border-left: 1px solid #EDE7D9; display: flex; flex-direction: column; max-height: calc(100vh - 88px); transition: background 0.15s; }
.cart-sidebar.drop-hover { background: rgba(184,147,90,0.08); }
.cart-head { padding: 20px 20px 14px; border-bottom: 1px solid #EDE7D9; }
.cart-head-cn { font-size: 15px; font-weight: 700; color: var(--forest); margin: 0 0 4px; }
.cart-head-en { font-size: 12px; color: var(--muted); margin: 0; }
.cart-list { flex: 1; overflow-y: auto; padding: 12px 20px; min-height: 120px; }
.cart-empty { text-align: center; color: var(--muted); padding: 40px 0; }
.cart-empty-en { font-size: 11px; margin-top: 4px; }
.cart-item { border-bottom: 1px solid #F0EBDE; padding: 12px 0; cursor: grab; }
.cart-item:active { cursor: grabbing; }
.cart-item-top { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.cart-item-name { font-size: 13.5px; font-weight: 600; color: var(--ink); }
.cart-item-del { cursor: pointer; color: var(--muted); font-style: normal; padding: 2px 6px; }
.cart-item-del:hover { color: #C0392B; }
.cart-item-mid { display: flex; justify-content: space-between; align-items: center; margin-bottom: 6px; }
.cart-item-price { font-size: 13px; color: var(--forest); font-weight: 700; }
.cart-item-qty { display: flex; align-items: center; gap: 8px; }
.cart-item-qty button { width: 22px; height: 22px; border-radius: 50%; border: 1px solid #DDD3B8; background: #fff; font-size: 13px; cursor: pointer; }
.cart-item-qty span { font-size: 13px; font-weight: 700; min-width: 16px; text-align: center; }
.cart-item-note { width: 100%; border: 1px solid #EDE7D9; border-radius: 3px; padding: 5px 8px; font-size: 12px; font-family: inherit; }

.cart-footer { padding: 16px 20px 24px; border-top: 1px solid #EDE7D9; }
.cart-total-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.cart-total-price { font-size: 20px; font-weight: 700; color: var(--forest); }
.contact-form { display: flex; flex-direction: column; gap: 10px; }
.contact-form input {
  border: 1px solid #DDD3B8; border-radius: 3px; padding: 9px 12px; font-size: 13px; font-family: inherit;
}
.contact-row { display: flex; gap: 10px; }
.contact-row input { flex: 1; }
.btn-gold {
  background: var(--gold); border: 1px solid var(--gold); color: #fff;
  padding: 11px 0; border-radius: 3px; font-size: 13.5px; cursor: pointer; margin-top: 4px;
}
.btn-gold:hover { background: #A17E48; }
.btn-gold:disabled { opacity: 0.6; cursor: not-allowed; }

@media (max-width: 960px) {
  .order-layout { grid-template-columns: 1fr; }
  .cat-sidebar { display: flex; overflow-x: auto; padding: 12px; }
  .cat-sidebar-head { display: none; }
  .cat-btn { flex-shrink: 0; width: auto; border-left: none; border-bottom: 3px solid transparent; }
  .cat-btn.active { border-left: none; border-bottom-color: var(--gold); }
  .dish-area, .cart-sidebar { max-height: none; }
  .cart-sidebar { border-left: none; border-top: 1px solid #EDE7D9; }
}
</style>

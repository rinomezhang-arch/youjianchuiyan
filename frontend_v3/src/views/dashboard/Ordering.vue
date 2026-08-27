<template>
  <div class="ordering-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">点菜 · Ordering</h2>
        <p class="page-subtitle">桌台选择 · 菜品浏览 · 下单结算</p>
      </div>
      <div class="page-header-right">
        <el-select v-model="currentTableId" placeholder="选择桌台" class="table-select" @change="onTableChange">
          <el-option
            v-for="t in tableList"
            :key="t.table_id"
            :label="`${t.table_name || t.table_number}${t.booking_id ? ' · 用餐中' : ''}`"
            :value="t.table_id"
          />
        </el-select>
        <el-button type="warning" @click="refreshAll">刷新数据</el-button>
      </div>
    </div>

    <div class="ordering-main">
      <div class="left-panel">
        <div class="category-bar">
          <div
            class="category-item"
            :class="{ active: selectedCategory === '' }"
            @click="selectedCategory = ''"
          >
            <span class="cat-name">全部</span>
          </div>
          <div
            v-for="cat in categories"
            :key="cat"
            class="category-item"
            :class="{ active: selectedCategory === cat }"
            @click="selectedCategory = cat"
          >
            <span class="cat-name">{{ cat }}</span>
          </div>
        </div>
      </div>

      <div class="center-panel">
        <div class="dish-search-bar">
          <el-input v-model="searchKeyword" placeholder="搜索菜品名称/口味..." clearable class="search-input" />
        </div>
        <div class="dish-grid">
          <div
            v-for="d in filteredDishes"
            :key="d.dishId"
            class="dish-card"
            :class="{ soldout: d.status !== 'active' }"
            @click="addToCart(d)"
          >
            <div class="dish-image">
              <img v-if="d.imageUrl" :src="d.imageUrl" :alt="d.dishName" />
              <div v-else class="dish-image-ph">{{ (d.dishName || '菜').charAt(0) }}</div>
              <div v-if="getCartQty(d.dishId) > 0" class="dish-badge">{{ getCartQty(d.dishId) }}</div>
            </div>
            <div class="dish-info">
              <div class="dish-name">{{ d.dishName }}</div>
              <div class="dish-tags">
                <span v-if="d.tags" class="tag">{{ d.tags }}</span>
                <span v-if="d.status !== 'active'" class="tag tag-time">已下架</span>
              </div>
              <div class="dish-bottom">
                <span class="dish-price">¥{{ (d.salePrice || 0).toFixed(2) }}</span>
                <el-button size="small" type="primary" round :disabled="d.status !== 'active'" @click.stop="addToCart(d)">+ 加菜</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="filteredDishes.length === 0 && !loading" description="暂无菜品" />
      </div>

      <div class="right-panel">
        <div class="cart-header">
          <span class="cart-title">本次加菜</span>
          <span class="cart-count">{{ cartTotalQty }} 份</span>
          <el-button text size="small" type="danger" @click="clearCart" v-if="cart.length">清空</el-button>
        </div>
        <div class="cart-list" v-loading="cartLoading">
          <div v-if="existingDishes.length" class="existing-dishes">
            <div class="existing-title">已下单（{{ existingDishes.length }} 项）</div>
            <div v-for="item in existingDishes" :key="item.dishBookingId" class="cart-item existing">
              <div class="cart-item-info">
                <div class="cart-item-name">{{ item.dishName }}</div>
                <div class="cart-item-price">¥{{ (item.unitPrice || 0).toFixed(2) }} × {{ item.dishQuantity }}</div>
              </div>
            </div>
          </div>
          <div v-for="item in cart" :key="item.dishId" class="cart-item">
            <div class="cart-item-img">
              <img v-if="item.imageUrl" :src="item.imageUrl" />
              <span v-else>{{ (item.dishName || '菜').charAt(0) }}</span>
            </div>
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.dishName }}</div>
              <div class="cart-item-price">¥{{ (item.unitPrice || 0).toFixed(2) }}</div>
            </div>
            <div class="cart-item-ctrl">
              <el-button size="small" circle @click="decQty(item)">-</el-button>
              <span class="qty-num">{{ item.qty }}</span>
              <el-button size="small" type="primary" circle @click="incQty(item)">+</el-button>
            </div>
          </div>
          <el-empty v-if="cart.length === 0 && existingDishes.length === 0" description="请选择桌台并点选菜品" />
        </div>
        <div class="cart-footer">
          <div class="cart-summary">
            <div class="summary-row">
              <span>本次加菜合计：</span>
              <span class="summary-total">¥{{ cartTotalAmount.toFixed(2) }}</span>
            </div>
          </div>
          <div class="cart-actions">
            <el-button class="action-btn primary" size="large" type="primary" :disabled="cart.length === 0 || !currentTableId" @click="submitOrder">
              提交下单
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showOpenTableDialog" title="开台登记" width="420px">
      <el-form :model="openTableForm" label-width="90px">
        <el-form-item label="客户姓名" required>
          <el-input v-model="openTableForm.customerName" placeholder="散客可填“散客”" />
        </el-form-item>
        <el-form-item label="联系电话" required>
          <el-input v-model="openTableForm.customerPhone" placeholder="11位手机号" />
        </el-form-item>
        <el-form-item label="用餐人数" required>
          <el-input-number v-model="openTableForm.guestCount" :min="1" :max="99" class="full-width" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOpenTableDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmOpenTableAndSubmit">开台并下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { getDishes, getCategories } from '@/api/dish'
import { getTableBoard } from '@/api/booking'
import request from '@/utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const currentStoreId = computed(() => userStore.currentStore?.storeId || userStore.stores?.[0]?.storeId || 1)

const loading = ref(false)
const cartLoading = ref(false)
const dishList = ref([])
const categories = ref([])
const tableList = ref([])
const selectedCategory = ref('')
const searchKeyword = ref('')
const currentTableId = ref(null)
const currentBookingId = ref(null)
const cart = ref([])
const existingDishes = ref([])
const showOpenTableDialog = ref(false)
const openTableForm = ref({ customerName: '', customerPhone: '', guestCount: 2 })

const currentTable = computed(() => tableList.value.find(t => t.table_id === currentTableId.value))

const filteredDishes = computed(() => {
  let result = dishList.value
  if (selectedCategory.value) {
    result = result.filter(d => d.category === selectedCategory.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(d =>
      (d.dishName || '').toLowerCase().includes(kw) ||
      (d.tags || '').toLowerCase().includes(kw)
    )
  }
  return result
})

const cartTotalQty = computed(() => cart.value.reduce((s, i) => s + (i.qty || 0), 0))
const cartTotalAmount = computed(() => cart.value.reduce((s, i) => s + (i.unitPrice || 0) * (i.qty || 0), 0))

function getCartQty(dishId) {
  const item = cart.value.find(c => c.dishId === dishId)
  return item ? item.qty : 0
}

async function fetchCategories() {
  try {
    const res = await getCategories({ storeId: currentStoreId.value })
    categories.value = res.data || []
  } catch (e) {
    console.error('加载分类失败', e)
    categories.value = []
  }
}

async function fetchTables() {
  try {
    const today = new Date().toISOString().slice(0, 10)
    const res = await getTableBoard({ storeId: currentStoreId.value, date: today })
    tableList.value = res.data || []
    if (tableList.value.length > 0 && !currentTableId.value) {
      currentTableId.value = tableList.value[0].table_id
      onTableChange()
    }
  } catch (e) {
    console.error('加载桌台失败', e)
    tableList.value = []
  }
}

async function fetchDishes() {
  loading.value = true
  try {
    const res = await getDishes({ storeId: currentStoreId.value })
    dishList.value = res.data || []
  } catch (e) {
    console.error('加载菜品失败', e)
    ElMessage.error('加载菜品失败')
  } finally {
    loading.value = false
  }
}

async function fetchExistingDishes() {
  existingDishes.value = []
  if (!currentBookingId.value) return
  cartLoading.value = true
  try {
    const res = await request.get(`/bookings/${currentBookingId.value}/dishes`, { params: { storeId: currentStoreId.value } })
    existingDishes.value = res.data || []
  } catch (e) {
    console.error('加载已下单菜品失败', e)
  } finally {
    cartLoading.value = false
  }
}

function addToCart(dish) {
  if (dish.status !== 'active') {
    ElMessage.warning('该菜品已下架')
    return
  }
  const existing = cart.value.find(c => c.dishId === dish.dishId)
  if (existing) {
    existing.qty += 1
  } else {
    cart.value.push({
      dishId: dish.dishId,
      dishName: dish.dishName,
      imageUrl: dish.imageUrl,
      unitPrice: dish.salePrice || 0,
      qty: 1
    })
  }
  ElMessage.success(`已添加 ${dish.dishName}`)
}

function incQty(item) {
  item.qty += 1
}

function decQty(item) {
  if (item.qty <= 1) {
    cart.value = cart.value.filter(c => c.dishId !== item.dishId)
  } else {
    item.qty -= 1
  }
}

async function clearCart() {
  try {
    await ElMessageBox.confirm('确定清空本次加菜？', '确认', { type: 'warning' })
    cart.value = []
  } catch (e) {
    /* cancel */
  }
}

async function submitOrder() {
  if (cart.value.length === 0) {
    ElMessage.warning('请先选择菜品')
    return
  }
  if (!currentTableId.value) {
    ElMessage.warning('请先选择桌台')
    return
  }
  if (!currentBookingId.value) {
    // 该桌台今天还没有开台记录，需要先开台才能下单
    openTableForm.value = { customerName: '', customerPhone: '', guestCount: 2 }
    showOpenTableDialog.value = true
    return
  }
  await submitDishesToBooking(currentBookingId.value)
}

async function confirmOpenTableAndSubmit() {
  const f = openTableForm.value
  if (!f.customerName.trim()) { ElMessage.warning('请填写客户姓名'); return }
  if (!/^1[3-9]\d{9}$/.test(f.customerPhone)) { ElMessage.warning('请填写正确的11位手机号'); return }
  try {
    const today = new Date().toISOString().slice(0, 10)
    const bookingRes = await request.post('/bookings', {
      customerName: f.customerName,
      customerPhone: f.customerPhone,
      guestCount: f.guestCount,
      bookingDate: today,
      bookingType: 'normal',
      storeId: currentStoreId.value
    })
    const bookingId = bookingRes.data?.bookingId
    if (!bookingId) throw new Error('开台失败：未返回预订号')
    await request.post(`/bookings/${bookingId}/tables`, {
      tableId: currentTableId.value,
      guestCount: f.guestCount,
      storeId: currentStoreId.value
    })
    currentBookingId.value = bookingId
    showOpenTableDialog.value = false
    await submitDishesToBooking(bookingId)
    fetchTables()
  } catch (e) {
    console.error('开台失败', e)
    ElMessage.error(e.response?.data?.message || e.message || '开台失败')
  }
}

async function submitDishesToBooking(bookingId) {
  try {
    for (const item of cart.value) {
      await request.post(`/bookings/${bookingId}/dishes`, {
        dishId: item.dishId,
        dishName: item.dishName,
        dishQuantity: item.qty,
        unitPrice: item.unitPrice,
        subtotal: item.unitPrice * item.qty
      })
    }
    cart.value = []
    ElMessage.success('下单成功')
    await fetchExistingDishes()
  } catch (e) {
    console.error('提交订单失败', e)
    ElMessage.error(e.response?.data?.message || '提交失败')
  }
}

function onTableChange() {
  currentBookingId.value = currentTable.value?.booking_id || null
  cart.value = []
  fetchExistingDishes()
}

function refreshAll() {
  fetchCategories()
  fetchTables()
  fetchDishes()
  if (currentBookingId.value) fetchExistingDishes()
}

onMounted(refreshAll)
</script>

<style scoped>
.ordering-page { max-width: 1600px; margin: 0 auto; height: calc(100vh - 140px); display: flex; flex-direction: column; }
.page-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text); margin-bottom: 4px; }
.page-subtitle { font-size: 13px; color: var(--color-text-muted); }
.page-header-right { display: flex; gap: 10px; align-items: center; }
.table-select { width: 180px; }

.ordering-main { flex: 1; display: grid; grid-template-columns: 200px 1fr 380px; gap: 16px; min-height: 0; }

.left-panel { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); padding: 12px; overflow-y: auto; }
.category-bar { display: flex; flex-direction: column; gap: 6px; }
.category-item { padding: 12px 14px; border-radius: 8px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 8px; font-size: 14px; color: var(--color-text); }
.category-item:hover { background: var(--color-bg-alt); }
.category-item.active { background: linear-gradient(135deg, #1a3a2a, #2D4A3E); color: #FFD78A; font-weight: 600; }
.cat-name { flex: 1; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }

.center-panel { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); display: flex; flex-direction: column; min-height: 0; }
.dish-search-bar { padding: 12px 16px; border-bottom: 1px solid var(--color-border); }
.search-input { width: 100%; }
.dish-grid { flex: 1; padding: 16px; overflow-y: auto; display: grid; grid-template-columns: repeat(auto-fill, minmax(210px, 1fr)); gap: 14px; align-content: start; }
.dish-card { background: var(--color-bg); border: 1px solid var(--color-border); border-radius: 10px; overflow: hidden; cursor: pointer; transition: all 0.2s; display: flex; flex-direction: column; }
.dish-card:hover { box-shadow: 0 4px 14px rgba(0,0,0,0.08); transform: translateY(-2px); }
.dish-image { position: relative; width: 100%; height: 130px; background: var(--color-bg-alt); }
.dish-image img { width: 100%; height: 100%; object-fit: cover; }
.dish-image-ph { width: 100%; height: 100%; display: flex; align-items: center; justify-content: center; font-size: 36px; font-weight: 600; color: var(--color-text-muted); background: linear-gradient(135deg, #f5f0e8, #e8e0d0); }
.dish-badge { position: absolute; top: 8px; right: 8px; background: #C4A35A; color: #fff; font-weight: 600; font-size: 12px; border-radius: 50%; width: 24px; height: 24px; display: flex; align-items: center; justify-content: center; }
.dish-info { padding: 10px 12px; display: flex; flex-direction: column; gap: 6px; }
.dish-name { font-size: 15px; font-weight: 600; color: var(--color-text); }
.dish-tags { display: flex; gap: 6px; flex-wrap: wrap; }
.tag { font-size: 11px; padding: 2px 6px; border-radius: 4px; background: #f5f0e8; color: #8B2020; }
.tag-time { background: #e8f0ea; color: #1a3a2a; }
.dish-bottom { display: flex; justify-content: space-between; align-items: center; margin-top: 4px; }
.dish-price { color: #8B2020; font-weight: 700; font-size: 16px; }

.right-panel { background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-lg); display: flex; flex-direction: column; min-height: 0; }
.cart-header { padding: 14px 16px; border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 10px; }
.cart-title { font-size: 16px; font-weight: 700; color: var(--color-text); flex: 1; }
.cart-count { font-size: 13px; color: var(--color-text-muted); }
.cart-list { flex: 1; overflow-y: auto; padding: 8px 0; }
.existing-dishes { padding: 0 16px 8px; border-bottom: 1px dashed var(--color-border); margin-bottom: 8px; }
.existing-title { font-size: 12px; color: var(--color-text-muted); margin-bottom: 6px; }
.cart-item.existing { padding: 6px 0; opacity: 0.75; }
.cart-item { display: flex; align-items: center; gap: 10px; padding: 10px 16px; border-bottom: 1px solid var(--color-border); }
.cart-item-img { width: 44px; height: 44px; border-radius: 6px; background: var(--color-bg-alt); display: flex; align-items: center; justify-content: center; font-weight: 600; color: var(--color-text-muted); flex-shrink: 0; overflow: hidden; }
.cart-item-img img { width: 100%; height: 100%; object-fit: cover; }
.cart-item-info { flex: 1; min-width: 0; }
.cart-item-name { font-size: 13px; color: var(--color-text); font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.cart-item-price { font-size: 12px; color: #8B2020; font-weight: 600; }
.cart-item-ctrl { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.qty-num { min-width: 22px; text-align: center; font-weight: 600; font-size: 14px; }

.cart-footer { border-top: 1px solid var(--color-border); padding: 14px 16px; }
.cart-summary { margin-bottom: 12px; }
.summary-row { display: flex; justify-content: space-between; align-items: baseline; }
.summary-total { color: #8B2020; font-weight: 700; font-size: 22px; }
.cart-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.action-btn { width: 100%; height: 42px; }
.action-btn.primary { height: 42px; }
.amount-total { color: #8B2020; font-weight: 700; font-size: 18px; }
.full-width { width: 100%; }
</style>

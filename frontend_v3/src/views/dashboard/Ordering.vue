<template>
  <div class="ordering-page">
    <div class="page-header">
      <div class="page-header-left">
        <h2 class="page-title">点菜 · Ordering</h2>
        <p class="page-subtitle">桌台选择 · 菜品浏览 · 下单结算</p>
      </div>
      <div class="page-header-right">
        <el-select v-model="currentTable" placeholder="选择桌台" class="table-select" @change="onTableChange">
          <el-option v-for="t in tableList" :key="t.id" :label="`桌台 ${t.id}`" :value="t.id" />
        </el-select>
        <el-button type="warning" @click="refreshAll">刷新数据</el-button>
      </div>
    </div>

    <div class="ordering-main">
      <div class="left-panel">
        <div class="category-bar">
          <div
            v-for="cat in categories"
            :key="cat.id || cat.caipinleixing"
            class="category-item"
            :class="{ active: selectedCategory === (cat.caipinleixing || cat.name) }"
            @click="selectedCategory = cat.caipinleixing || cat.name"
          >
            <span class="cat-name">{{ cat.caipinleixing || cat.name }}</span>
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
            :key="d.id"
            class="dish-card"
            :class="{ soldout: d.status === 'soldout' }"
            @click="addToCart(d)"
          >
            <div class="dish-image">
              <img v-if="d.tupian" :src="d.tupian" :alt="d.caipinmingcheng" />
              <div v-else class="dish-image-ph">{{ (d.caipinmingcheng || '菜').charAt(0) }}</div>
              <div v-if="getCartQty(d.id) > 0" class="dish-badge">{{ getCartQty(d.id) }}</div>
            </div>
            <div class="dish-info">
              <div class="dish-name">{{ d.caipinmingcheng }}</div>
              <div class="dish-tags">
                <span v-if="d.kouwei" class="tag">{{ d.kouwei }}</span>
                <span v-if="d.yujishijian" class="tag tag-time">{{ d.yujishijian }}</span>
              </div>
              <div class="dish-bottom">
                <span class="dish-price">¥{{ (d.price || 0).toFixed(2) }}</span>
                <el-button size="small" type="primary" round @click.stop="addToCart(d)">+ 加菜</el-button>
              </div>
            </div>
          </div>
        </div>
        <el-empty v-if="filteredDishes.length === 0 && !loading" description="暂无菜品" />
      </div>

      <div class="right-panel">
        <div class="cart-header">
          <span class="cart-title">购物车</span>
          <span class="cart-count">{{ cartTotalQty }} 份</span>
          <el-button text size="small" type="danger" @click="clearCart" v-if="cart.length">清空</el-button>
        </div>
        <div class="cart-list" v-loading="cartLoading">
          <div v-for="item in cart" :key="item.goodid" class="cart-item">
            <div class="cart-item-img">
              <img v-if="item.picture" :src="item.picture" />
              <span v-else>{{ (item.goodname || '菜').charAt(0) }}</span>
            </div>
            <div class="cart-item-info">
              <div class="cart-item-name">{{ item.goodname }}</div>
              <div class="cart-item-price">¥{{ (item.price || 0).toFixed(2) }}</div>
            </div>
            <div class="cart-item-ctrl">
              <el-button size="small" circle @click="decQty(item)">-</el-button>
              <span class="qty-num">{{ item.buynumber }}</span>
              <el-button size="small" type="primary" circle @click="incQty(item)">+</el-button>
            </div>
          </div>
          <el-empty v-if="cart.length === 0" description="购物车为空，请选择菜品" />
        </div>
        <div class="cart-footer">
          <div class="cart-summary">
            <div class="summary-row">
              <span>合计：</span>
              <span class="summary-total">¥{{ cartTotalAmount.toFixed(2) }}</span>
            </div>
          </div>
          <div class="cart-actions">
            <el-button class="action-btn" size="large" @click="saveCart">保存购物车</el-button>
            <el-button class="action-btn primary" size="large" type="primary" :disabled="cart.length === 0" @click="submitOrder">
              下单结算
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <el-dialog v-model="showOrderDialog" title="确认下单" width="50vw">
      <el-form :model="orderForm" label-width="90px">
        <el-form-item label="订单号">
          <el-input v-model="orderForm.orderid" disabled />
        </el-form-item>
        <el-form-item label="桌台">
          <span>桌台 {{ currentTable || '-' }}</span>
        </el-form-item>
        <el-form-item label="商品总数">
          <span>{{ cartTotalQty }} 份</span>
        </el-form-item>
        <el-form-item label="订单金额">
          <span class="amount-total">¥{{ cartTotalAmount.toFixed(2) }}</span>
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="orderForm.status" class="full-width">
            <el-option label="待支付" value="待支付" />
            <el-option label="已支付" value="已支付" />
            <el-option label="已发货" value="已发货" />
            <el-option label="已完成" value="已完成" />
          </el-select>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="orderForm.tel" placeholder="选填" />
        </el-form-item>
        <el-form-item label="收货地址">
          <el-input v-model="orderForm.address" placeholder="选填" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showOrderDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmOrder">确认提交订单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import {
  btDishPage,
  btDishTypeList,
  btCartPage,
  btCartSave,
  btCartUpdate,
  btCartDelete,
  btOrderSave,
  btTableInfoList
} from '@/api/dish'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const cartLoading = ref(false)
const dishList = ref([])
const categories = ref([])
const tableList = ref([])
const selectedCategory = ref('')
const searchKeyword = ref('')
const currentTable = ref(null)
const currentUser = ref(Number(localStorage.getItem('userId') || 1))
const cart = ref([])
const showOrderDialog = ref(false)
const orderForm = ref({
  orderid: '',
  status: '待支付',
  tel: '',
  address: '',
  consignee: '',
  remark: ''
})

const filteredDishes = computed(() => {
  let result = dishList.value
  if (selectedCategory.value) {
    result = result.filter(d => d.caipinleixing === selectedCategory.value)
  }
  if (searchKeyword.value) {
    const kw = searchKeyword.value.toLowerCase()
    result = result.filter(d =>
      (d.caipinmingcheng || '').toLowerCase().includes(kw) ||
      (d.kouwei || '').toLowerCase().includes(kw) ||
      (d.caipinjieshao || '').toLowerCase().includes(kw)
    )
  }
  return result
})

const cartTotalQty = computed(() => cart.value.reduce((s, i) => s + (i.buynumber || 0), 0))

const cartTotalAmount = computed(() => cart.value.reduce((s, i) => s + (i.price || 0) * (i.buynumber || 0), 0))

function getCartQty(goodid) {
  const item = cart.value.find(c => c.goodid === goodid)
  return item ? item.buynumber : 0
}

async function fetchCategories() {
  try {
    const res = await btDishTypeList()
    categories.value = res.data || res || []
    if (categories.value.length > 0) {
      selectedCategory.value = categories.value[0].caipinleixing
    }
  } catch (e) {
    console.warn('加载分类失败', e)
    categories.value = [
      { caipinleixing: '凉菜' },
      { caipinleixing: '热菜' },
      { caipinleixing: '汤羹' },
      { caipinleixing: '主食' }
    ]
    selectedCategory.value = categories.value[0].caipinleixing
  }
}

async function fetchTables() {
  try {
    const res = await btTableInfoList()
    tableList.value = res.data || res || []
  } catch (e) {
    console.warn('加载桌台失败', e)
    tableList.value = [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }, { id: 5 }, { id: 6 }]
  }
  if (tableList.value.length > 0) {
    currentTable.value = tableList.value[0].id
  }
}

async function fetchDishes() {
  loading.value = true
  try {
    const res = await btDishPage({ page: 1, limit: 500 })
    const data = res.data || {}
    dishList.value = data.list || []
  } catch (e) {
    console.error('加载菜品失败', e)
    ElMessage.error('加载菜品失败')
  } finally {
    loading.value = false
  }
}

async function fetchCart() {
  cartLoading.value = true
  try {
    const res = await btCartPage({ page: 1, limit: 200, userid: currentUser.value })
    const data = res.data || {}
    cart.value = (data.list || []).map(x => ({
      ...x,
      goodid: x.goodid,
      goodname: x.goodname,
      picture: x.picture,
      buynumber: x.buynumber || 1,
      price: x.price || 0
    }))
  } catch (e) {
    console.warn('加载购物车失败', e)
    cart.value = []
  } finally {
    cartLoading.value = false
  }
}

function addToCart(dish) {
  const existing = cart.value.find(c => c.goodid === dish.id)
  if (existing) {
    existing.buynumber += 1
    updateCartItem(existing)
  } else {
    const newItem = {
      tablename: 'caipinxinxi',
      userid: currentUser.value,
      goodid: dish.id,
      goodname: dish.caipinmingcheng,
      picture: dish.tupian,
      buynumber: 1,
      price: dish.price,
      discountprice: dish.price,
      goodtype: dish.caipinleixing,
      storeId: Number(localStorage.getItem('currentStoreId') || localStorage.getItem('storeId') || 1)
    }
    cart.value.push(newItem)
    saveCartItem(newItem)
  }
  ElMessage.success(`已添加 ${dish.caipinmingcheng}`)
}

function incQty(item) {
  item.buynumber += 1
  updateCartItem(item)
}

function decQty(item) {
  if (item.buynumber <= 1) {
    removeFromCart(item)
  } else {
    item.buynumber -= 1
    updateCartItem(item)
  }
}

async function saveCartItem(item) {
  try {
    await btCartSave(item)
  } catch (e) {
    console.warn('保存购物车项失败', e)
  }
}

async function updateCartItem(item) {
  try {
    if (item.id) {
      await btCartUpdate(item)
    } else {
      await btCartSave(item)
    }
  } catch (e) {
    console.warn('更新购物车项失败', e)
  }
}

async function removeFromCart(item) {
  try {
    if (item.id) {
      await btCartDelete([item.id])
    }
    cart.value = cart.value.filter(c => c.goodid !== item.goodid)
  } catch (e) {
    console.warn('删除购物车项失败', e)
  }
}

async function saveCart() {
  try {
    for (const item of cart.value) {
      if (item.id) {
        await btCartUpdate(item)
      } else {
        await btCartSave(item)
      }
    }
    ElMessage.success('购物车已保存')
  } catch (e) {
    console.error('保存购物车失败', e)
    ElMessage.error('保存失败')
  }
}

async function clearCart() {
  try {
    await ElMessageBox.confirm('确定清空购物车？', '确认', { type: 'warning' })
    const ids = cart.value.map(x => x.id).filter(Boolean)
    if (ids.length > 0) {
      await btCartDelete(ids)
    }
    cart.value = []
    ElMessage.success('已清空')
  } catch (e) {
    if (!e.message?.includes('cancel') && e !== 'cancel') {
      cart.value = []
    }
  }
}

function submitOrder() {
  if (cart.value.length === 0) {
    ElMessage.warning('购物车为空')
    return
  }
  orderForm.value = {
    orderid: 'OD' + Date.now(),
    status: '待支付',
    tel: '',
    address: '',
    consignee: '',
    remark: ''
  }
  showOrderDialog.value = true
}

async function confirmOrder() {
  try {
    const storeId = Number(localStorage.getItem('currentStoreId') || localStorage.getItem('storeId') || 1)
    for (const item of cart.value) {
      const order = {
        orderid: orderForm.value.orderid,
        tablename: 'caipinxinxi',
        userid: currentUser.value,
        goodid: item.goodid,
        goodname: item.goodname,
        picture: item.picture,
        buynumber: item.buynumber,
        price: item.price,
        discountprice: item.discountprice || item.price,
        total: (item.price || 0) * (item.buynumber || 0),
        discounttotal: ((item.discountprice || item.price) || 0) * (item.buynumber || 0),
        type: 1,
        status: orderForm.value.status,
        address: orderForm.value.address,
        tel: orderForm.value.tel,
        consignee: orderForm.value.consignee,
        remark: orderForm.value.remark,
        logistics: '',
        goodtype: item.goodtype,
        storeId
      }
      await btOrderSave(order)
    }
    const ids = cart.value.map(x => x.id).filter(Boolean)
    if (ids.length > 0) {
      await btCartDelete(ids)
    }
    cart.value = []
    showOrderDialog.value = false
    ElMessage.success('订单提交成功')
  } catch (e) {
    console.error('提交订单失败', e)
    ElMessage.error('提交失败：' + (e.message || '未知错误'))
  }
}

function onTableChange() {
  fetchCart()
}

function refreshAll() {
  fetchCategories()
  fetchTables()
  fetchDishes()
  fetchCart()
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

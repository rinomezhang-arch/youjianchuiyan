<template>
  <el-dialog :model-value="visible" width="1000px" class="od-dlg" :close-on-click-modal="false" :show-close="false" @update:model-value="$emit('update:visible', $event)" @opened="loadAll">
    <template #header>
      <div class="od-header">
        <div class="od-header-left">
          <div class="od-header-icon">
            <el-icon><KnifeFork /></el-icon>
          </div>
          <div>
            <h2 class="od-header-title">点菜</h2>
            <p class="od-header-sub">Order Dishes · {{ tableName }}</p>
          </div>
        </div>
        <button class="od-close-btn" @click="$emit('update:visible', false)" aria-label="关闭">
          <el-icon><Close /></el-icon>
        </button>
      </div>
    </template>

    <!-- Meta bar -->
    <div class="od-meta">
      <span>{{ date }} {{ periodTime }}</span>
      <span class="od-sep">|</span>
      <span>{{ periodLabel }} · {{ periodLabelEn }}</span>
      <span class="od-sep">|</span>
      <span>{{ tableName }} · Table {{ tableName }}</span>
    </div>

    <!-- Toolbar -->
    <div class="od-toolbar">
      <div class="od-source-tabs">
        <button :class="['od-source-tab', { active: sourceMode === 'a_la_carte' }]" @click="sourceMode = 'a_la_carte'">
          <el-icon><KnifeFork /></el-icon>
          零点菜肴 · À la carte
        </button>
        <button :class="['od-source-tab', { active: sourceMode === 'package' }]" @click="sourceMode = 'package'">
          <el-icon><Menu /></el-icon>
          宴会菜肴 · Banquet Packages
        </button>
      </div>
      <div v-if="sourceMode === 'a_la_carte'" class="od-search-wrap">
        <el-input v-model="search" :placeholder="`${t('menu.search')} · ${t('menu.searchEn')}`" class="od-search" clearable>
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div v-else class="od-pkg-info">
        <span class="od-pkg-count">共 {{ packages.length }} 套套餐 · {{ packages.length }} packages</span>
      </div>
    </div>

    <!-- Main -->
    <div class="od-main">
      <!-- Left: categories + dishes / packages -->
      <div class="od-left">
        <!-- 零点菜肴：分类 + 菜品 -->
        <template v-if="sourceMode === 'a_la_carte'">
          <!-- Category tabs -->
          <div class="od-cats">
            <button
              :class="['od-cat-btn', { active: catFilter === 'all' }]"
              @click="catFilter = 'all'"
            >全部 · All</button>
            <button
              v-for="c in cats"
              :key="c.categoryId || c.id || c.key"
              :class="['od-cat-btn', { active: catFilter === (c.categoryId || c.id || c.key) }]"
              :title="c.categoryNameEn || c.nameEn || c.en"
              @click="catFilter = c.categoryId || c.id || c.key"
            >{{ c.categoryName || c.name || c.label }}</button>
          </div>

          <!-- Dish grid -->
          <div class="od-dishes">
            <div v-if="filteredDishes.length === 0" class="od-empty">
              <el-icon class="od-empty-icon"><KnifeFork /></el-icon>
              <p class="od-empty-text">{{ loading ? '加载中...' : '暂无菜品 · No dishes found' }}</p>
            </div>
            <div v-else class="od-dish-grid">
              <div
                v-for="d in filteredDishes"
                :key="d.dishId || d.id"
                class="od-dish-card"
              >
                <div class="od-dish-info">
                  <div class="od-dish-name-row">
                    <h4 class="od-dish-name">{{ d.dishName || d.name }}</h4>
                    <span v-if="d.tag || d.isSignature || d.isPopular" :class="['od-tag', getTagClass(d)]">
                      <el-icon v-if="d.tag === '招牌' || d.isSignature"><Medal /></el-icon>
                      <el-icon v-else-if="d.tag === '热销' || d.isPopular"><HotWater /></el-icon>
                      <el-icon v-else><Star /></el-icon>
                      {{ d.tag || (d.isSignature ? '招牌' : d.isPopular ? '热销' : '新品') }}
                    </span>
                  </div>
                  <p class="od-dish-en">{{ d.dishNameEn || d.nameEn || d.en || '' }}</p>
                  <p class="od-dish-price">¥{{ d.salePrice || d.price }}</p>
                </div>

                <div v-if="getQty(d) > 0" class="od-qty-row">
                  <button class="od-qty-btn od-qty-minus" @click="changeQty(d, -1)" aria-label="减少">
                    <el-icon><Minus /></el-icon>
                  </button>
                  <span class="od-qty-num">{{ getQty(d) }}</span>
                  <button class="od-qty-btn od-qty-plus" @click="changeQty(d, 1)" aria-label="增加">
                    <el-icon><Plus /></el-icon>
                  </button>
                </div>
                <button v-else class="od-add-btn" @click="changeQty(d, 1)" aria-label="添加">
                  <el-icon><Plus /></el-icon>
                </button>
              </div>
            </div>
          </div>
        </template>

        <!-- 宴会菜肴：套餐列表 -->
        <template v-else>
          <div class="od-pkg-list">
            <div v-if="packages.length === 0" class="od-empty">
              <el-icon class="od-empty-icon"><Menu /></el-icon>
              <p class="od-empty-text">{{ pkgLoading ? '加载中...' : '暂无套餐 · No packages found' }}</p>
            </div>
            <div v-else class="od-pkg-grid">
              <div
                v-for="pkg in packages"
                :key="pkg.packageId || pkg.id"
                class="od-pkg-card"
              >
                <div class="od-pkg-header">
                  <div class="od-pkg-title-row">
                    <h4 class="od-pkg-name">{{ pkg.packageName || pkg.name }}</h4>
                    <span class="od-pkg-tag">{{ pkg.packageType || pkg.type || '标准套餐' }}</span>
                  </div>
                  <p class="od-pkg-en">{{ pkg.packageNameEn || pkg.nameEn || '' }}</p>
                  <div class="od-pkg-meta">
                    <span class="od-pkg-dishes">{{ pkg.dishCount || (pkg.dishes?.length) || 0 }} 道菜</span>
                    <span class="od-pkg-serve">适合 {{ pkg.suitablePeople || '10' }} 人</span>
                  </div>
                  <p class="od-pkg-price">
                    ¥{{ pkg.price || pkg.salePrice || 0 }}
                    <span class="od-pkg-unit">/桌</span>
                  </p>
                </div>
                <div class="od-pkg-dishes-preview">
                  <p class="od-pkg-dishes-title">菜品包含 · Included Dishes</p>
                  <div class="od-pkg-dishes-list">
                    <span v-for="(dish, i) in getPkgDishes(pkg).slice(0, 6)" :key="i" class="od-pkg-dish-chip">
                      {{ dish.dishName || dish.name || dish }}
                    </span>
                    <span v-if="getPkgDishes(pkg).length > 6" class="od-pkg-dish-more">
                      +{{ getPkgDishes(pkg).length - 6 }} 更多
                    </span>
                  </div>
                </div>
                <div class="od-pkg-footer">
                  <button class="od-pkg-add-btn" @click="addPackage(pkg)">
                    <el-icon><Plus /></el-icon>
                    加入菜单 · Add to Order
                  </button>
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <!-- Right: cart -->
      <aside class="od-right">
        <div class="od-cart-header">
          <div class="od-cart-title">
            <el-icon><ShoppingCart /></el-icon>
            <h3>已选菜品 <span class="od-cart-sub">· Cart ({{ totalCount }})</span></h3>
          </div>
          <span class="od-cart-total">共 {{ totalCount }} 道 · ¥{{ totalPrice }}</span>
        </div>

        <div class="od-cart-body">
          <div v-if="orderItems.length === 0" class="od-empty">
            <el-icon class="od-empty-icon"><ShoppingCart /></el-icon>
            <p class="od-empty-text">
              点击左侧菜品添加<br/>
              <span class="od-empty-sub">Add dishes from the left</span>
            </p>
          </div>
          <ul v-else class="od-cart-list">
            <li v-for="item in orderItems" :key="item.dishCode || item.dishId" class="od-cart-item">
              <div class="od-cart-info">
                <p class="od-cart-name">{{ getDishName(item.dishCode || item.dishId) }}</p>
                <p class="od-cart-price">¥{{ getDishPrice(item.dishCode || item.dishId) }} × {{ item.qty }}</p>
              </div>
              <div class="od-cart-qty">
                <button class="od-cart-qty-btn od-qty-minus" @click="changeQtyByCode(item.dishCode || item.dishId, -1)" aria-label="减少">
                  <el-icon><Minus /></el-icon>
                </button>
                <span class="od-cart-qty-num">{{ item.qty }}</span>
                <button class="od-cart-qty-btn od-qty-plus" @click="changeQtyByCode(item.dishCode || item.dishId, 1)" aria-label="增加">
                  <el-icon><Plus /></el-icon>
                </button>
              </div>
            </li>
          </ul>
        </div>

        <!-- Cart footer -->
        <div class="od-cart-footer">
          <div class="od-total-row">
            <span class="od-total-label">合计 · Total</span>
            <span class="od-total-price">¥{{ totalPrice }}</span>
          </div>
          <div class="od-footer-btns">
            <button class="od-btn od-btn-clear" @click="clearAll" :disabled="orderItems.length === 0">
              <el-icon><Delete /></el-icon>
              清空
            </button>
            <button class="od-btn od-btn-cancel" @click="$emit('update:visible', false)">
              取消 · Cancel
            </button>
            <button class="od-btn od-btn-confirm" @click="doConfirm">
              <el-icon><Check /></el-icon>
              确认 · Confirm
            </button>
          </div>
        </div>
      </aside>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  KnifeFork, Close, Search, Plus, Minus, Delete,
  ShoppingCart, Star, HotWater, Medal, Check, Menu
} from '@element-plus/icons-vue'
import { getDishes as apiGetDishes, getCategories as apiGetCategories } from '@/api/dish'
import { getPackages as apiGetPackages } from '@/api/package'
import {
  getOrders, saveOrders, getOrderKey,
  addDishToOrder, removeDishFromOrder, updateDishQty,
  clearTableOrders
} from '@/utils/menuStore'

const { t } = useI18n()

const props = defineProps({
  visible: Boolean,
  date: String,
  period: String,
  tableName: String,
})

const emit = defineEmits(['update:visible', 'confirmed'])

const search = ref('')
const catFilter = ref('all')
const loading = ref(false)
const pkgLoading = ref(false)
const allDishes = ref([])
const orderItems = ref([])
const cats = ref([])
const packages = ref([])
const sourceMode = ref('a_la_carte')

const periodLabel = computed(() => {
  const p = props.period?.toLowerCase() || ''
  if (p.includes('lunch') || p.includes('午')) return '午餐'
  return '晚餐'
})
const periodLabelEn = computed(() => {
  const p = props.period?.toLowerCase() || ''
  if (p.includes('lunch') || p.includes('午')) return 'Lunch'
  return 'Dinner'
})
const periodTime = computed(() => {
  const p = props.period || ''
  if (!p || p.includes('dinner') || p.includes('晚')) return '17:55'
  return '11:30'
})

const filteredDishes = computed(() => {
  let arr = allDishes.value
  const q = search.value.trim().toLowerCase()
  if (q) arr = arr.filter(d => {
    const name = (d.dishName || d.name || '').toLowerCase()
    const en = (d.dishNameEn || d.nameEn || d.en || '').toLowerCase()
    const id = (d.dishId || d.id || '').toLowerCase()
    return name.includes(q) || en.includes(q) || id.includes(q)
  })
  if (catFilter.value !== 'all') {
    arr = arr.filter(d => (d.dishCategoryId || d.categoryId || d.dishCategory || d.category) === catFilter.value)
  }
  return arr
})

const totalCount = computed(() => orderItems.value.reduce((s, i) => s + (i.qty || 1), 0))
const totalPrice = computed(() => {
  return orderItems.value.reduce((s, i) => {
    const d = allDishes.value.find(x => (x.dishId || x.id) === (i.dishCode || i.dishId))
    return s + (d ? (d.salePrice || d.price || 0) * (i.qty || 1) : 0)
  }, 0)
})

function getTagClass(d) {
  if (d.tag === '招牌' || d.isSignature) return 'od-tag-signature'
  if (d.tag === '热销' || d.isPopular) return 'od-tag-hot'
  return 'od-tag-new'
}

function getDish(code) {
  return allDishes.value.find(x => (x.dishId || x.id) === code)
}
function getDishName(code) {
  const d = getDish(code)
  return d ? (d.dishName || d.name) : code
}
function getDishPrice(code) {
  const d = getDish(code)
  return d ? (d.salePrice || d.price || 0) : 0
}

function getQty(d) {
  const code = d.dishId || d.id
  const item = orderItems.value.find(i => (i.dishCode || i.dishId) === code)
  return item ? item.qty : 0
}

function changeQty(d, delta) {
  const code = d.dishId || d.id
  if (delta > 0) {
    addDishToOrder(props.date, props.period, props.tableName, code)
  } else {
    updateDishQty(props.date, props.period, props.tableName, code, delta)
  }
  refreshOrders()
}

function changeQtyByCode(code, delta) {
  if (delta > 0) {
    addDishToOrder(props.date, props.period, props.tableName, code)
  } else {
    updateDishQty(props.date, props.period, props.tableName, code, delta)
  }
  refreshOrders()
}

function getPkgDishes(pkg) {
  if (pkg.dishes && Array.isArray(pkg.dishes)) return pkg.dishes
  if (pkg.packageDishes && Array.isArray(pkg.packageDishes)) return pkg.packageDishes
  if (pkg.items && Array.isArray(pkg.items)) return pkg.items
  return []
}

function addPackage(pkg) {
  const dishes = getPkgDishes(pkg)
  let count = 0
  dishes.forEach(dish => {
    const code = dish.dishId || dish.id || dish.dishCode
    if (code) {
      addDishToOrder(props.date, props.period, props.tableName, code)
      count++
    }
  })
  refreshOrders()
  ElMessage.success(`已加入 ${count} 道菜品`)
}

function clearAll() {
  ElMessageBox.confirm('确定清空当前桌台所有菜品？', '警告', { type: 'warning' }).then(() => {
    clearTableOrders(props.date, props.period, props.tableName)
    refreshOrders()
    ElMessage.success('已清空')
  }).catch(() => {})
}

function doConfirm() {
  if (orderItems.value.length === 0) {
    ElMessage.warning('请先添加菜品')
    return
  }
  ElMessage.success('菜单已确认')
  emit('confirmed')
  emit('update:visible', false)
}

function refreshOrders() {
  const orders = getOrders()
  const key = getOrderKey(props.date, props.period, props.tableName)
  orderItems.value = orders[key] || []
}

async function loadAll() {
  loading.value = true
  pkgLoading.value = true
  refreshOrders()
  try {
    const dishRes = await apiGetDishes({ usageType: 'a_la_carte' })
    if (dishRes.code === 200) {
      allDishes.value = dishRes.data || []
    }
    const catRes = await apiGetCategories()
    if (catRes.code === 200) {
      cats.value = catRes.data || []
    }
    try {
      const pkgRes = await apiGetPackages()
      if (pkgRes.code === 200) {
        packages.value = pkgRes.data || pkgRes.packages || []
      }
    } catch (pe) {
      console.warn('加载套餐失败', pe)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('加载菜品失败')
  } finally {
    loading.value = false
    pkgLoading.value = false
  }
}
</script>

<style>
.od-dlg .el-dialog {
  border-radius: 1rem;
  overflow: hidden;
  box-shadow: 0 25px 50px -12px rgba(45, 74, 62, 0.15);
  max-height: 85vh;
}
.od-dlg .el-dialog__header { padding: 0; margin: 0; }
.od-dlg .el-dialog__headerbtn { display: none; }
.od-dlg .el-dialog__body { padding: 0; background: oklch(1 0.004 95); }

/* ============ 头部 ============ */
.od-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  background: oklch(0.38 0.055 160);
  padding: 16px 24px;
  color: oklch(0.98 0.01 95);
}
.od-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.od-header-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 0.5rem;
  background: oklch(0.98 0.01 95 / 0.1);
  box-shadow: 0 0 0 1px oklch(0.98 0.01 95 / 0.15) inset;
  font-size: 20px;
}
.od-header-title {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.2;
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}
.od-header-sub {
  font-size: 12px;
  color: oklch(0.98 0.01 95 / 0.7);
  margin: 2px 0 0;
}
.od-close-btn {
  background: transparent;
  border: none;
  color: oklch(0.98 0.01 95 / 0.8);
  cursor: pointer;
  padding: 4px;
  border-radius: 0.375rem;
  font-size: 20px;
  display: flex;
  align-items: center;
  transition: background 0.2s;
}
.od-close-btn:hover {
  background: oklch(0.98 0.01 95 / 0.1);
  color: oklch(0.98 0.01 95);
}

/* ============ Meta bar ============ */
.od-meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 0 16px;
  border-bottom: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.4);
  padding: 10px 24px;
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.od-sep { color: oklch(0.9 0.012 120); }

/* ============ Toolbar ============ */
.od-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  border-bottom: 1px solid oklch(0.9 0.012 120);
  padding: 12px 24px;
}
.od-source-tabs {
  display: inline-flex;
  border-radius: 0.5rem;
  background: oklch(0.955 0.012 120 / 0.5);
  padding: 2px;
  gap: 2px;
}
.od-source-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 0.375rem;
  padding: 6px 14px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  background: transparent;
  color: oklch(0.52 0.02 150);
}
.od-source-tab:hover { color: oklch(0.24 0.02 160); }
.od-source-tab.active {
  background: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.od-search-wrap {
  flex: 1;
  min-width: 200px;
}
.od-pkg-info {
  flex: 1;
  display: flex;
  justify-content: flex-end;
}
.od-pkg-count {
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.od-search .el-input__wrapper {
  border-radius: 0.5rem !important;
  box-shadow: 0 0 0 1px oklch(0.9 0.012 120) inset !important;
  background: oklch(1 0.004 95) !important;
  padding: 0 12px 0 36px !important;
}
.od-search .el-input__wrapper:hover {
  box-shadow: 0 0 0 1px oklch(0.75 0.11 75) inset !important;
}
.od-search .el-input__wrapper.is-focus {
  box-shadow: 0 0 0 2px oklch(0.38 0.055 160 / 0.4), 0 0 0 2px oklch(0.38 0.055 160 / 0.15) inset !important;
}
.od-search .el-input__inner {
  height: 36px;
  font-size: 13px;
  color: oklch(0.24 0.02 160);
}
.od-search .el-input__prefix {
  color: oklch(0.52 0.02 150);
  font-size: 16px;
  left: 12px;
}

/* ============ Main ============ */
.od-main {
  display: flex;
  min-height: 0;
  height: 500px;
}

/* ============ Left ============ */
.od-left {
  display: flex;
  min-width: 0;
  flex: 1;
  flex-direction: column;
}
.od-cats {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  border-bottom: 1px solid oklch(0.9 0.012 120);
  padding: 12px 24px;
}
.od-cat-btn {
  border-radius: 0.5rem;
  padding: 6px 12px;
  font-size: 12px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.52 0.02 150);
}
.od-cat-btn:hover { color: oklch(0.24 0.02 160); }
.od-cat-btn.active {
  background: oklch(0.38 0.055 160);
  border-color: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}

.od-dishes {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.od-dish-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
@media (min-width: 768px) {
  .od-dish-grid { grid-template-columns: 1fr 1fr; }
}

.od-dish-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border-radius: 0.75rem;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  padding: 14px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  transition: all 0.2s;
}
.od-dish-card:hover {
  border-color: oklch(0.38 0.055 160 / 0.3);
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
}
.od-dish-info { min-width: 0; flex: 1; }
.od-dish-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.od-dish-name {
  font-size: 14px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
  margin: 0;
  truncate: true;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.od-tag {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  border-radius: 2px;
  padding: 2px 6px;
  font-size: 10px;
  font-weight: 600;
  flex-shrink: 0;
}
.od-tag-hot {
  background: oklch(0.577 0.19 27.325 / 0.1);
  color: oklch(0.577 0.19 27.325);
}
.od-tag-signature {
  background: oklch(0.75 0.11 75 / 0.2);
  color: oklch(0.28 0.03 70);
}
.od-tag-new {
  background: oklch(0.38 0.055 160 / 0.1);
  color: oklch(0.38 0.055 160);
}
.od-dish-en {
  font-size: 11px;
  color: oklch(0.52 0.02 150);
  margin: 2px 0 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.od-dish-price {
  font-size: 14px;
  font-weight: 600;
  color: oklch(0.38 0.055 160);
  margin: 4px 0 0;
  font-family: 'Noto Serif SC', serif;
}

/* 数量按钮 */
.od-qty-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;
}
.od-qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 0.375rem;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.52 0.02 150);
}
.od-qty-btn:hover {
  border-color: oklch(0.38 0.055 160 / 0.4);
  color: oklch(0.38 0.055 160);
}
.od-qty-btn.od-qty-plus {
  background: oklch(0.38 0.055 160);
  border-color: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
}
.od-qty-btn.od-qty-plus:hover {
  background: oklch(0.34 0.04 160);
  border-color: oklch(0.34 0.04 160);
  color: oklch(0.98 0.01 95);
}
.od-qty-num {
  width: 20px;
  text-align: center;
  font-size: 13px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
}
.od-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 0.5rem;
  background: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  cursor: pointer;
  font-size: 16px;
  border: none;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
  transition: background 0.2s;
  flex-shrink: 0;
}
.od-add-btn:hover {
  background: oklch(0.34 0.04 160);
}

/* ============ Right ============ */
.od-right {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border-left: 1px solid oklch(0.9 0.012 120);
  background: oklch(0.955 0.012 120 / 0.2);
}
.od-cart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid oklch(0.9 0.012 120);
  padding: 12px 16px;
}
.od-cart-title {
  display: flex;
  align-items: center;
  gap: 8px;
  color: oklch(0.38 0.055 160);
}
.od-cart-title h3 {
  font-size: 14px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  margin: 0;
}
.od-cart-sub {
  font-weight: 400;
  color: oklch(0.52 0.02 150);
  font-size: 12px;
}
.od-cart-total {
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.38 0.055 160);
}

.od-cart-body {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}
.od-cart-list {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.od-cart-item {
  display: flex;
  align-items: center;
  gap: 8px;
  border-radius: 0.5rem;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  padding: 10px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.od-cart-info {
  min-width: 0;
  flex: 1;
}
.od-cart-name {
  font-size: 13px;
  font-weight: 500;
  color: oklch(0.24 0.02 160);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.od-cart-price {
  font-size: 11px;
  color: oklch(0.52 0.02 150);
  margin: 2px 0 0;
}
.od-cart-qty {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}
.od-cart-qty-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 0.375rem;
  cursor: pointer;
  font-size: 12px;
  transition: all 0.2s;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.52 0.02 150);
}
.od-cart-qty-btn:hover {
  border-color: oklch(0.38 0.055 160 / 0.4);
  color: oklch(0.38 0.055 160);
}
.od-cart-qty-btn.od-qty-plus {
  background: oklch(0.38 0.055 160);
  border-color: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
}
.od-cart-qty-btn.od-qty-plus:hover {
  background: oklch(0.34 0.04 160);
  color: oklch(0.98 0.01 95);
}
.od-cart-qty-num {
  width: 16px;
  text-align: center;
  font-size: 12px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
}

/* ============ Empty ============ */
.od-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  height: 100%;
  color: oklch(0.52 0.02 150);
  padding: 40px 20px;
}
.od-empty-icon {
  font-size: 40px;
  opacity: 0.3;
}
.od-empty-text {
  font-size: 13px;
  text-align: center;
  margin: 0;
  line-height: 1.6;
}
.od-empty-sub {
  font-size: 11px;
}

/* ============ Cart footer ============ */
.od-cart-footer {
  border-top: 1px solid oklch(0.9 0.012 120);
  padding: 12px 16px;
}
.od-total-row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 12px;
}
.od-total-label {
  font-size: 13px;
  color: oklch(0.52 0.02 150);
}
.od-total-price {
  font-size: 20px;
  font-weight: 600;
  color: oklch(0.38 0.055 160);
  font-family: 'Noto Serif SC', serif;
}
.od-footer-btns {
  display: flex;
  align-items: center;
  gap: 8px;
}
.od-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border-radius: 0.5rem;
  padding: 10px 12px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid transparent;
  flex-shrink: 0;
}
.od-btn:disabled { cursor: not-allowed; opacity: 0.5; }
.od-btn-clear {
  border-color: oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.52 0.02 150);
}
.od-btn-clear:hover:not(:disabled) {
  color: oklch(0.577 0.19 27.325);
}
.od-btn-cancel {
  border-color: oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  color: oklch(0.24 0.02 160);
}
.od-btn-cancel:hover {
  background: oklch(0.955 0.012 120);
}
.od-btn-confirm {
  flex: 1;
  justify-content: center;
  background: oklch(0.38 0.055 160);
  border-color: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  font-weight: 600;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.od-btn-confirm:hover {
  background: oklch(0.34 0.04 160);
  border-color: oklch(0.34 0.04 160);
}

/* ============ Package List ============ */
.od-pkg-list {
  min-height: 0;
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
}
.od-pkg-grid {
  display: grid;
  grid-template-columns: 1fr;
  gap: 12px;
}
.od-pkg-card {
  border-radius: 0.75rem;
  border: 1px solid oklch(0.9 0.012 120);
  background: oklch(1 0.004 95);
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
  transition: all 0.2s;
  overflow: hidden;
}
.od-pkg-card:hover {
  border-color: oklch(0.38 0.055 160 / 0.3);
  box-shadow: 0 4px 6px -1px rgba(0,0,0,0.05);
}
.od-pkg-header {
  padding: 14px 16px;
  border-bottom: 1px dashed oklch(0.9 0.012 120);
}
.od-pkg-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.od-pkg-name {
  font-size: 15px;
  font-weight: 600;
  color: oklch(0.24 0.02 160);
  margin: 0;
  font-family: 'Noto Serif SC', serif;
}
.od-pkg-tag {
  flex-shrink: 0;
  border-radius: 4px;
  padding: 2px 8px;
  font-size: 11px;
  font-weight: 500;
  background: oklch(0.75 0.11 75 / 0.15);
  color: oklch(0.28 0.03 70);
}
.od-pkg-en {
  font-size: 11px;
  color: oklch(0.52 0.02 150);
  margin: 2px 0 0;
}
.od-pkg-meta {
  display: flex;
  gap: 16px;
  margin-top: 8px;
}
.od-pkg-meta span {
  font-size: 12px;
  color: oklch(0.52 0.02 150);
}
.od-pkg-price {
  font-size: 18px;
  font-weight: 600;
  color: oklch(0.38 0.055 160);
  margin: 8px 0 0;
  font-family: 'Noto Serif SC', serif;
}
.od-pkg-unit {
  font-size: 12px;
  font-weight: 400;
  color: oklch(0.52 0.02 150);
  margin-left: 2px;
}
.od-pkg-dishes-preview {
  padding: 10px 16px;
}
.od-pkg-dishes-title {
  font-size: 12px;
  font-weight: 500;
  color: oklch(0.34 0.04 160);
  margin: 0 0 8px;
}
.od-pkg-dishes-list {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.od-pkg-dish-chip {
  border-radius: 4px;
  background: oklch(0.955 0.012 120 / 0.5);
  padding: 3px 8px;
  font-size: 11px;
  color: oklch(0.34 0.04 160);
}
.od-pkg-dish-more {
  border-radius: 4px;
  background: oklch(0.38 0.055 160 / 0.1);
  padding: 3px 8px;
  font-size: 11px;
  color: oklch(0.38 0.055 160);
  font-weight: 500;
}
.od-pkg-footer {
  padding: 10px 16px 14px;
}
.od-pkg-add-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  width: 100%;
  border-radius: 0.5rem;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid oklch(0.38 0.055 160 / 0.3);
  background: oklch(0.38 0.055 160 / 0.05);
  color: oklch(0.38 0.055 160);
}
.od-pkg-add-btn:hover {
  background: oklch(0.38 0.055 160);
  color: oklch(0.98 0.01 95);
  border-color: oklch(0.38 0.055 160);
}
</style>

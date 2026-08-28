<template>
  <view class="menu-page">
    <!-- 搜索 -->
    <view class="search-wrap">
      <view class="search-box">
        <text class="s-icon">🔍</text>
        <input
          class="s-input"
          v-model="keyword"
          confirm-type="search"
          placeholder="搜索菜品/口味/食材"
          @confirm="onSearch"
        />
        <text v-if="keyword" class="s-clear" @tap="clearKw">✕</text>
      </view>
      <text class="s-btn" @tap="onSearch">搜索</text>
    </view>

    <view class="menu-body">
      <!-- 左栏：分类 -->
      <scroll-view class="cat-col" scroll-y :scroll-into-view="'cat-' + currentCatId">
        <view
          v-for="c in categories"
          :key="c.id"
          :id="'cat-' + c.id"
          :class="['cat-item', {active: currentCatId === c.id}]"
          @tap="selectCat(c.id)"
        >
          <text class="cat-name ellipsis-1">{{ c.name }}</text>
          <view v-if="currentCatId === c.id" class="cat-active-bar"></view>
        </view>
      </scroll-view>

      <!-- 右栏：菜品列表 -->
      <scroll-view class="dish-col" scroll-y @scroll="onDishScroll" :scroll-into-view="scrollToDishId">
        <view v-if="searchMode" class="search-tip">
          <text>为您找到 <text class="gold-text">{{ filteredDishes.length }}</text> 道相关菜品</text>
        </view>

        <view v-for="c in displayCategories" :key="'sec-' + c.id" :id="'dish-cat-' + c.id" class="dish-cat-sec">
          <view class="dish-cat-title">
            <text>{{ c.name }}</text>
            <text class="dish-cat-count">{{ (dishByCat[c.id]||[]).length }}道</text>
          </view>

          <view v-if="!(dishByCat[c.id]||[]).length" class="cat-empty">该分类暂无菜品</view>

          <view
            v-for="d in (dishByCat[c.id]||[])"
            :key="d.id"
            class="dish-card"
            @tap="openDetail(d)"
          >
            <image class="dish-img" :src="d.image || d.imageUrl || placeholder" mode="aspectFill" />
            <view class="dish-info">
              <text class="dish-name ellipsis-1">{{ d.name }}</text>
              <text class="dish-desc ellipsis-2">{{ d.desc || d.description || '匠心烹制，地道风味' }}</text>
              <view class="tags" v-if="d.tags && d.tags.length">
                <text class="tag" v-for="t in d.tags.slice(0,2)" :key="t">{{ t }}</text>
              </view>
              <view class="dish-bottom">
                <text class="dish-price">¥<text class="p-num">{{ d.price || '0.00' }}</text></text>
                <view class="stepper" @tap.stop>
                  <text class="s-minus" v-if="countOf(d.id)" @tap.stop="change(d, -1)">−</text>
                  <text class="s-count" v-if="countOf(d.id)">{{ countOf(d.id) }}</text>
                  <text class="s-plus" @tap.stop="change(d, +1)">＋</text>
                </view>
              </view>
            </view>
          </view>
        </view>

        <view style="height: 240rpx"></view>
      </scroll-view>
    </view>

    <!-- 底部购物车 -->
    <view class="cart-bar" v-if="!cartStore.isEmpty">
      <view class="cart-icon-wrap" @tap="onPreviewCart">
        <text class="cart-icon">🛒</text>
        <text class="cart-badge">{{ cartStore.totalCount }}</text>
      </view>
      <view class="cart-info">
        <text class="cart-total">合计 ¥{{ (cartStore.totalFen/100).toFixed(2) }}</text>
        <text class="cart-hint">已选 {{ cartStore.totalCount }} 道</text>
      </view>
      <view class="cart-btn" @tap="onCheckout">去下单</view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { useCartStore } from '@/store/cart'
import { fetchCategories, fetchDishes, searchDishes } from '@/api/dish'
import { IMG_PLACEHOLDER } from '@/config/env'

const appStore  = useAppStore()
const cartStore = useCartStore()
const placeholder = IMG_PLACEHOLDER

onMounted(() => cartStore.hydrate())

const keyword = ref('')
const categories = ref([])
const dishes = ref([])
const currentCatId = ref(0)
const scrollToDishId = ref('')
const searchMode = ref(false)

/* 接口加载 */
async function loadAll() {
  cartStore.hydrate()
  try {
    let cats = await fetchCategories()
    if (!Array.isArray(cats)) cats = cats?.records || cats?.list || []
    categories.value = cats && cats.length ? cats : defaultCategories()
    currentCatId.value = categories.value[0]?.id || 0

    let list = await fetchDishes({ categoryId: null, size: 500 })
    if (!Array.isArray(list)) list = list?.records || list?.list || []
    dishes.value = list || []
  } catch (e) {
    categories.value = defaultCategories()
    dishes.value = []
  }
}
function defaultCategories() {
  return [
    { id: 1, name: '招牌推荐' }, { id: 2, name: '经典徽菜' },
    { id: 3, name: '农家小炒' }, { id: 4, name: '汤羹炖品' },
    { id: 5, name: '田园时蔬' }, { id: 6, name: '主食点心' }, { id: 7, name: '酒水饮料' }
  ]
}

/* 分类 ↔ 菜品 分组 */
const dishByCat = computed(() => {
  const base = searchMode.value ? filteredDishes.value : dishes.value
  const map = {}
  categories.value.forEach(c => { map[c.id] = [] })
  base.forEach(d => {
    const cid = d.categoryId ?? d.dishCategoryId ?? 0
    if (map[cid]) map[cid].push(d)
    else {
      if (!map[0]) map[0] = []
      map[0].push(d)
    }
  })
  return map
})
const displayCategories = computed(() => {
  if (!searchMode.value) return categories.value
  // 搜索模式：只展示有命中的分类
  return categories.value.filter(c => (dishByCat.value[c.id] || []).length)
})

/* 搜索 */
const filteredDishes = computed(() => {
  const kw = String(keyword.value || '').trim()
  if (!kw) return dishes.value
  const low = kw.toLowerCase()
  return dishes.value.filter(d => [d.name, d.desc, d.description, d.tags?.join?.('')].join(' ').toLowerCase().includes(low))
})

async function onSearch() {
  const kw = String(keyword.value || '').trim()
  searchMode.value = !!kw
  if (!kw) return
  try {
    const r = await searchDishes(kw)
    if (Array.isArray(r) && r.length) dishes.value = r
  } catch {}
}
function clearKw() { keyword.value = ''; searchMode.value = false }

/* 分类交互 */
function selectCat(id) {
  currentCatId.value = id
  scrollToDishId.value = ''
  nextTick(() => { scrollToDishId.value = 'dish-cat-' + id })
}
let scrollTimer = null
function onDishScroll(e) {
  if (searchMode.value) return
  clearTimeout(scrollTimer)
  scrollTimer = setTimeout(() => {
    const top = e.detail.scrollTop
    // 简单策略：从下往上判断第一个顶部大于等于当前的分类
    for (let i = categories.value.length - 1; i >= 0; i--) {
      const id = categories.value[i].id
      const el = document?.getElementById?.(`dish-cat-${id}`)   // H5 only
      if (el && el.offsetTop - 20 <= top) {
        currentCatId.value = id
        return
      }
    }
    currentCatId.value = categories.value[0]?.id || 0
  }, 80)
}

/* 购物车加减 */
function countOf(id) { return cartStore.items.find(i => i.id === id)?.count || 0 }
function change(d, delta) {
  cartStore.add(d, delta)
  if (delta > 0) {
    uni.vibrateShort && uni.vibrateShort({ type: 'light' }).catch(() => {})
  }
}

/* 详情/预览购物车/下单 */
function openDetail(d) {
  uni.navigateTo({ url: `/pages/dishDetail/dishDetail?id=${d.id}` })
}
function onPreviewCart() {
  uni.showModal({
    title: '已选菜品',
    content: cartStore.items.map(i => `${i.name} ×${i.count}`).join('，') || '还未加购',
    showCancel: false
  })
}
function onCheckout() {
  if (!appStore.token) {
    uni.showModal({
      title: '请先登录',
      content: '登录后可提交订单，保存您的消费记录',
      confirmText: '去登录',
      success: (r) => r.confirm && uni.navigateTo({ url: '/pages/login/login' })
    })
    return
  }
  // 购物车空提示
  if (cart.isEmpty) {
    return uni.showToast({ title: '购物车是空的，先加点菜～', icon: 'none' })
  }
  uni.navigateTo({ url: '/pages/checkout/checkout' })
}

onMounted(loadAll)
onPullDownRefresh(async () => { await loadAll(); uni.stopPullDownRefresh() })
watch(() => appStore.currentStoreId, loadAll)
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.menu-page { min-height: 100vh; background: $brand-bg; display: flex; flex-direction: column; }

/* 搜索 */
.search-wrap {
  padding: $spacing-sm $spacing-md; display:flex; gap: $spacing-sm; align-items:center;
  background: #fff; border-bottom: 1rpx solid #F4F0E6; position: sticky; top: 0; z-index: 10;
}
.search-box {
  flex: 1; height: 72rpx; background: #F7F3EA; border-radius: 999rpx;
  display: flex; align-items: center; padding: 0 20rpx; gap: 12rpx;
}
.s-icon { font-size: $font-md; color: $brand-gold-dark; }
.s-input { flex: 1; font-size: $font-sm; height: 72rpx; }
.s-clear { color: #bbb; padding: 0 10rpx; font-size: $font-sm; }
.s-btn { color: $brand-gold-dark; font-size: $font-sm; padding: 0 10rpx; font-weight: 600; }

/* 主体两栏 */
.menu-body { flex: 1; display: flex; min-height: 0; }

.cat-col {
  width: 180rpx; background: #fff; flex-shrink: 0;
  border-right: 1rpx solid #F4F0E6;
}
.cat-item {
  position: relative; padding: 28rpx 16rpx; text-align:center;
  color: $text-2; font-size: $font-sm; transition: .2s;
}
.cat-item.active { color: $brand-gold-dark; font-weight: 700; background: #FFFBF0; }
.cat-active-bar { position: absolute; left: 0; top: 50%; transform: translateY(-50%); width: 6rpx; height: 40rpx; background: $brand-gold; border-radius: 0 4rpx 4rpx 0; }

.dish-col { flex: 1; padding: $spacing-sm; box-sizing: border-box; }
.search-tip { font-size: $font-xs; color: $text-3; margin-bottom: $spacing-sm; }
.dish-cat-sec { margin-bottom: $spacing-md; }
.dish-cat-title {
  display:flex; align-items: baseline; gap: 10rpx; padding: 6rpx 0 12rpx;
  font-size: $font-md; color: $brand-ink; font-weight: 700;
}
.dish-cat-count { font-size: $font-xs; color: $text-3; font-weight: 400; }
.cat-empty { color: $text-4; padding: $spacing-md 0; font-size: $font-xs; text-align: center; }

.dish-card {
  display: flex; gap: $spacing-sm; padding: $spacing-sm; background:#fff;
  border-radius: $radius-md; margin-bottom: $spacing-sm; box-shadow: $shadow-card;
}
.dish-img { width: 160rpx; height: 160rpx; border-radius: $radius-sm; flex-shrink: 0; background:#eee; }
.dish-info { flex: 1; display: flex; flex-direction: column; }
.dish-name { font-size: $font-md; color: $text-1; font-weight: 600; }
.dish-desc { font-size: $font-xs; color: $text-3; margin-top: 4rpx; }
.tags { margin-top: 6rpx; display:flex; gap: 8rpx; flex-wrap: wrap; }
.tag { background: #FFF3D6; color: $brand-gold-dark; font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 4rpx; }
.dish-bottom { margin-top: auto; display:flex; align-items: center; justify-content: space-between; }
.dish-price { color: $brand-gold-dark; font-weight: 700; }
.p-num { font-size: $font-lg; margin-left: 2rpx; }
.stepper { display:flex; align-items:center; gap: 14rpx; }
.s-plus {
  width: 44rpx; height: 44rpx; line-height: 40rpx; text-align:center;
  background: linear-gradient(135deg, #E6D4A8, #C9A86C); color:#fff; border-radius: 50%;
  font-size: 32rpx; box-shadow: $shadow-gold;
}
.s-minus {
  width: 40rpx; height: 40rpx; line-height: 36rpx; text-align:center;
  border: 1rpx solid $brand-gold; color: $brand-gold-dark; border-radius: 50%; font-size: 28rpx;
}
.s-count { min-width: 28rpx; text-align:center; color: $text-1; font-weight: 600; font-size: $font-sm; }

/* 底部购物车 */
.cart-bar {
  position: fixed; left: $spacing-md; right: $spacing-md; bottom: calc(env(safe-area-inset-bottom) + 20rpx);
  height: 108rpx; background: #fff; border-radius: $radius-pill;
  display: flex; align-items: center; padding: 0 12rpx 0 140rpx; box-shadow: 0 8rpx 36rpx rgba(0,0,0,0.15);
}
.cart-icon-wrap {
  position: absolute; left: 28rpx; top: -24rpx;
  width: 112rpx; height: 112rpx; border-radius: 50%;
  background: linear-gradient(135deg, #F9E6B8, #C9A86C);
  box-shadow: $shadow-gold; display:flex; align-items:center; justify-content:center;
}
.cart-icon { font-size: 54rpx; }
.cart-badge {
  position: absolute; right: -6rpx; top: -6rpx; min-width: 36rpx; height: 36rpx; padding: 0 8rpx;
  background: #E53935; color:#fff; border-radius: 999rpx; font-size: 22rpx; line-height: 36rpx; text-align:center;
}
.cart-info { flex: 1; display:flex; flex-direction: column; padding-left: 14rpx; }
.cart-total { color: $brand-gold-dark; font-weight: 800; font-size: $font-md; }
.cart-hint  { font-size: $font-xs; color: $text-3; }
.cart-btn {
  height: 80rpx; padding: 0 44rpx; line-height: 80rpx; color: #fff;
  background: linear-gradient(135deg, #1A1A1A, #3C3C3C); border-radius: $radius-pill; font-weight: 600;
}
</style>

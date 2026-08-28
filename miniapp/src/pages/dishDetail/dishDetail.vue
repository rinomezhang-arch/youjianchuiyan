<template>
  <view class="dd-page" v-if="dish">
    <image class="banner" :src="dish.image || dish.imageUrl || placeholder" mode="aspectFill" />

    <view class="info-card">
      <text class="d-name">{{ dish.name }}</text>
      <text class="d-desc">{{ dish.desc || dish.description || '又见炊烟匠心烹制，地道徽菜风味。' }}</text>

      <view class="tags" v-if="tags.length">
        <text class="tag" v-for="(t, i) in tags" :key="i">{{ t }}</text>
      </view>

      <view class="bottom-row">
        <view class="price-row">
          <text class="unit">¥</text>
          <text class="price">{{ dish.price || '0.00' }}</text>
          <text class="unit-suffix" v-if="dish.unit">/{{ dish.unit }}</text>
        </view>
        <view class="stepper">
          <text class="s-minus" v-if="count" @tap="change(-1)">−</text>
          <text class="s-count" v-if="count">{{ count }}</text>
          <text class="s-plus" @tap="change(+1)">＋</text>
        </view>
      </view>
    </view>

    <view class="card" v-if="dish.ingredients || dish.tips">
      <view class="c-title"><text class="bar"></text><text>菜品信息</text></view>
      <view class="kv" v-if="dish.ingredients">
        <text class="k">主要食材</text>
        <text class="v">{{ dish.ingredients }}</text>
      </view>
      <view class="kv" v-if="dish.spicyLevel != null">
        <text class="k">辣度</text>
        <text class="v">{{ '🌶'.repeat(Math.max(0, Number(dish.spicyLevel))) || '不辣' }}</text>
      </view>
      <view class="kv" v-if="dish.allergens">
        <text class="k">过敏原</text>
        <text class="v">{{ dish.allergens }}</text>
      </view>
      <view class="kv" v-if="dish.tips">
        <text class="k">温馨提示</text>
        <text class="v">{{ dish.tips }}</text>
      </view>
    </view>

    <view class="card" v-if="recommendList.length">
      <view class="c-title"><text class="bar"></text><text>本店推荐</text></view>
      <scroll-view scroll-x class="rec-scroll">
        <view class="rec-card" v-for="d in recommendList" :key="d.id" @tap="switchDish(d)">
          <image class="rec-img" :src="d.image || d.imageUrl || placeholder" mode="aspectFill" />
          <text class="rec-name ellipsis-1">{{ d.name }}</text>
          <text class="rec-price">¥{{ d.price }}</text>
        </view>
      </scroll-view>
    </view>

    <view style="height: 180rpx"></view>

    <view class="bottom-bar">
      <view class="b-left" @tap="goMenu">
        <text class="bl-icon">🍲</text>
        <text class="bl-text">返回菜单</text>
      </view>
      <view class="b-btn" @tap="goBookFromDish">搭配预订</view>
    </view>
  </view>
  <view v-else class="loading">加载中…</view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useCartStore } from '@/store/cart'
import { fetchDishDetail, fetchFeaturedDishes } from '@/api/dish'
import { IMG_PLACEHOLDER } from '@/config/env'

const placeholder = IMG_PLACEHOLDER
const cartStore = useCartStore()
onMounted(() => cartStore.hydrate())

const dish = ref(null)
const recommendList = ref([])
const tags = computed(() => {
  const d = dish.value || {}
  const arr = Array.isArray(d.tags) ? [...d.tags] : []
  if (d.hot) arr.unshift('🔥 招牌')
  if (d.isSignature) arr.unshift('⭐ 主厨推荐')
  if (d.status === 2) arr.push('今日售罄')
  return [...new Set(arr)].slice(0, 4)
})

const count = computed(() => cartStore.items.find(i => i.id === dish.value?.id)?.count || 0)

function change(delta) { cartStore.add(dish.value, delta) }

async function load(id) {
  try {
    const r = await fetchDishDetail(id)
    if (r) dish.value = r
  } catch {}
  if (!dish.value) {
    dish.value = {
      id, name: '又见炊烟·徽菜一道', price: '88.00', unit: '例',
      desc: '传统工艺，地道风味，又见炊烟出品。',
      image: placeholder, ingredients: '本地食材 + 秘制配方',
      spicyLevel: 0, tips: '推荐 2-4 人分享'
    }
  }
}

async function loadRec(excludeId) {
  try {
    let r = await fetchFeaturedDishes(1, 10)
    if (!Array.isArray(r)) r = r?.records || r?.list || []
    recommendList.value = (r || []).filter(x => x.id !== excludeId).slice(0, 8)
  } catch { recommendList.value = [] }
}

function switchDish(d) {
  dish.value = { ...d }
  loadRec(d.id)
  uni.pageScrollTo({ scrollTop: 0, duration: 200 })
}
function goMenu() { uni.switchTab({ url: '/pages/menu/menu' }) }
function goBookFromDish() { uni.switchTab({ url: '/pages/book/book' }) }

onMounted(() => {
  const pages = getCurrentPages()
  const pg = pages[pages.length - 1]
  const id = Number(pg?.options?.id || 0) || 1001
  load(id); loadRec(id)
})
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.dd-page { background: $brand-bg; min-height: 100vh; }
.banner { width: 100%; height: 520rpx; display:block; background: #eee; }

.info-card {
  background: #fff; margin: -40rpx $spacing-md $spacing-md; position: relative;
  border-radius: $radius-lg; padding: $spacing-lg; box-shadow: $shadow-card;
}
.d-name { font-size: 44rpx; font-weight: 800; color: $text-1; display:block; }
.d-desc { font-size: $font-sm; color: $text-3; margin-top: 10rpx; line-height: 1.6; display:block; }

.tags { display:flex; gap: 10rpx; flex-wrap: wrap; margin-top: 14rpx; }
.tag {
  padding: 4rpx 14rpx; background: #FFF3D6; color: $brand-gold-dark;
  font-size: 22rpx; border-radius: 6rpx;
}

.bottom-row {
  display: flex; justify-content: space-between; align-items: center;
  margin-top: $spacing-md; padding-top: $spacing-sm; border-top: 1rpx dashed #F4F0E6;
}
.price-row { display:flex; align-items: baseline; gap: 4rpx; }
.unit { color: $brand-gold-dark; font-size: $font-lg; font-weight: 700; }
.price { color: $brand-gold-dark; font-size: 60rpx; font-weight: 900; line-height: 1; }
.unit-suffix { color: $text-3; font-size: $font-xs; }
.stepper { display:flex; align-items:center; gap: 20rpx; }
.s-plus { width: 64rpx; height: 64rpx; line-height: 60rpx; text-align:center; border-radius: 50%; background: linear-gradient(135deg,#E6D4A8,#C9A86C); color:#fff; font-size: 40rpx; box-shadow: $shadow-gold; }
.s-minus { width: 56rpx; height: 56rpx; line-height: 52rpx; text-align:center; border-radius: 50%; border: 1rpx solid $brand-gold; color: $brand-gold-dark; font-size: 32rpx; }
.s-count { min-width: 40rpx; text-align: center; font-size: $font-lg; font-weight: 700; }

.card { background: #fff; border-radius: $radius-lg; padding: $spacing-md; margin: 0 $spacing-md $spacing-md; box-shadow: $shadow-card; }
.c-title { display:flex; align-items: center; gap: 12rpx; margin-bottom: $spacing-sm; }
.c-title .bar { width: 8rpx; height: 28rpx; background: linear-gradient(180deg,#E6D4A8,#C9A86C); border-radius: 4rpx; }
.c-title text:last-child { font-size: $font-md; font-weight: 700; }
.kv { display:flex; padding: 10rpx 0; border-bottom: 1rpx dashed #F7F3E7; }
.kv:last-child { border-bottom: 0; }
.k { width: 140rpx; color: $text-3; font-size: $font-sm; flex-shrink: 0; }
.v { flex: 1; color: $text-1; font-size: $font-sm; line-height: 1.6; }

.rec-scroll { white-space: nowrap; }
.rec-card {
  display: inline-block; width: 200rpx; background: #fff; border-radius: $radius-md; overflow: hidden;
  box-shadow: $shadow-card; margin-right: $spacing-sm; white-space: normal;
}
.rec-img { width: 200rpx; height: 200rpx; background:#eee; display:block; }
.rec-name { display:block; padding: 10rpx 12rpx 0; font-size: $font-xs; font-weight: 600; color: $text-1; }
.rec-price { display:block; padding: 0 12rpx 12rpx; color: $brand-gold-dark; font-weight: 800; font-size: $font-sm; }

.loading { text-align: center; color: $text-3; padding: 200rpx 0; }

.bottom-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: $spacing-sm $spacing-md calc(env(safe-area-inset-bottom) + 16rpx);
  background: #fff; box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.06);
  display:flex; align-items:center; gap: $spacing-md;
}
.b-left { display:flex; flex-direction: column; align-items: center; padding: 0 10rpx; color: $brand-gold-dark; }
.bl-icon { font-size: 36rpx; }
.bl-text { font-size: 20rpx; margin-top: 2rpx; }
.b-btn {
  flex: 1; height: 92rpx; line-height: 92rpx; text-align:center; color:#fff; letter-spacing: 2rpx;
  background: linear-gradient(135deg, #E6D4A8, #C9A86C); border-radius: $radius-pill;
  box-shadow: $shadow-gold; font-weight: 700;
}
</style>

<template>
  <view class="coupon-page">
    <scroll-view scroll-x class="tabs" :show-scrollbar="false">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{active: active===t.key}"
        @tap="switchTab(t.key)">
        {{ t.label }}
      </view>
    </scroll-view>

    <!-- 可用 Tab：展示可用券领取中心 -->
    <view v-if="active === 'available'" class="section">
      <view class="section-title">🎁 领券中心 · 先到先得</view>
      <view v-for="c in available" :key="c.id" class="coupon">
        <view class="left">
          <view class="v">¥{{ (c.valueFen/100).toFixed(0) }}</view>
          <view class="min">满{{ (c.minFen/100).toFixed(0) }}可用</view>
          <view class="shop">{{ c.storeName || '全门店通用' }}</view>
        </view>
        <view class="mid">
          <view class="name">{{ c.name }}</view>
          <view class="range muted">{{ c.validDateRange || '领券后30天内有效' }}</view>
          <view class="scopes muted">{{ c.scopeText || '通用菜品（香烟/酒水/套餐除外）' }}</view>
          <view class="stock" v-if="c.stock != null">剩余 {{ c.stock }} / {{ c.total }}</view>
        </view>
        <view class="right">
          <button
            class="btn"
            :class="{ disabled: c.received || c.stock === 0 }"
            size="mini"
            @tap="receive(c)">
            {{ c.received ? '已领' : '立即领取' }}
          </button>
        </view>
      </view>
      <view v-if="availableLoaded && !available.length" class="empty">
        <image class="icon" src="https://img.icons8.com/ios/200/C9A86C/gift.png" mode="aspectFit" />
        <view class="text">暂时没有可领的券，关注门店活动第一时间发券哦～</view>
      </view>
    </view>

    <!-- 我的 Tab -->
    <view v-else class="section">
      <view v-for="c in myList" :key="c.id" class="coupon my"
            :class="{ invalid: c.status !== 0, ['use-'+mode]: !!mode }"
            @tap="backToCheckout(c)">
        <view class="left" :class="{ gray: c.status !== 0 }">
          <view class="v">¥{{ (c.valueFen/100).toFixed(0) }}</view>
          <view class="min">满{{ (c.minFen/100).toFixed(0) }}可用</view>
          <view class="shop">{{ c.storeName || '全门店通用' }}</view>
        </view>
        <view class="mid">
          <view class="name">{{ c.name }}</view>
          <view class="range muted">{{ c.validDateRange }}</view>
          <view class="scopes muted">{{ c.scopeText || '通用菜品' }}</view>
          <view class="status-flag" v-if="c.status===1">✅ 已使用</view>
          <view class="status-flag exp" v-else-if="c.status===2">⌛ 已过期</view>
        </view>
        <view class="right" v-if="c.status===0 && mode==='select'">
          <button class="btn" size="mini" @tap.stop="backToCheckout(c)">使用</button>
        </view>
        <view class="right" v-else-if="c.status===0">
          <button class="btn go" size="mini" @tap.stop="goMenu">去使用</button>
        </view>
      </view>
      <view v-if="myLoaded && !myList.length" class="empty">
        <image class="icon" src="https://img.icons8.com/ios/200/C9A86C/receipt.png" mode="aspectFit" />
        <view class="text">你还没有{{ activeLabel }}的优惠券～</view>
      </view>
    </view>

    <view class="tips">
      💡 说明：优惠券不可叠加使用，取餐/外卖/堂食订单可在下单页自动选最优券
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import couponApi from '@/api/coupon'
import { toast } from '@/api/wx'

const tabs = [
  { key: 'available', label: '🎁 领券中心' },
  { key: 0,           label: '🟡 未使用' },
  { key: 1,           label: '✅ 已使用' },
  { key: 2,           label: '⌛ 已过期' }
]
const active = ref('available')
const available = ref([])
const availableLoaded = ref(false)
const myList = ref([])
const myLoaded = ref(false)
const mode = ref('')        // 'select' = 下单页跳转过来选券
const activeLabel = computed(() => {
  const t = tabs.find(x=>x.key===active.value)
  return t?.label?.replace(/^\S*\s*/, '') || ''
})

onLoad(q => {
  if (q.mode === 'select') mode.value = 'select'
  if (q.back === '1') { /* 预留：eventChannel 回传 */ }
})

onShow(() => {
  if (active.value === 'available') loadAvailable()
  else loadMy()
})
function switchTab(k) {
  active.value = k
  if (k === 'available') loadAvailable()
  else loadMy()
}

async function loadAvailable() {
  const r = await couponApi.fetchAvailableCoupons()
  available.value = Array.isArray(r?.data) ? r.data : []
  availableLoaded.value = true
}
async function loadMy() {
  const r = await couponApi.fetchMyCoupons(active.value === 'all' ? '' : active.value)
  myList.value = Array.isArray(r?.data?.list || r?.data) ? (r.data.list || r.data) : []
  myLoaded.value = true
}
async function receive(c) {
  if (c.received || c.stock === 0) return
  try {
    await couponApi.receiveCoupon(c.id)
    c.received = true
    if (c.stock != null) c.stock--
    uni.showToast({ title: '领取成功', icon: 'success' })
  } catch (e) { toast(e?.message || '领取失败') }
}
function goMenu(){ uni.switchTab({ url: '/pages/menu/menu' }) }
function backToCheckout(c) {
  if (mode.value !== 'select') return
  if (c.status !== 0) return
  // 通过 eventChannel 回传给 checkout 页
  const pages = getCurrentPages()
  const prev  = pages[pages.length - 2]
  if (prev && prev.$vm && prev.$vm.onCouponSelected) {
    prev.$vm.onCouponSelected(c)
    uni.navigateBack()
  } else {
    // fallback: 存到全局 storage 让 checkout 读取
    uni.setStorageSync('selected_coupon', c)
    uni.navigateBack()
  }
}
</script>

<style lang="scss">
@import '@/uni.scss';
.coupon-page{ background:$page-bg; min-height:100vh; padding-bottom:80rpx}
.tabs{white-space:nowrap; background:#fff; padding:16rpx 16rpx 0;
  .tab{display:inline-block; padding:14rpx 30rpx 18rpx; font-size:28rpx; color:$text-muted; position:relative;
    &.active{color:$ink-black; font-weight:600;
      &:after{content:''; position:absolute; left:50%; transform:translateX(-50%); bottom:2rpx; width:36rpx; height:6rpx; background:$brand-gradient; border-radius:3rpx}
    }
  }
}
.section{padding:20rpx}
.section-title{font-size:30rpx; font-weight:600; color:$ink-black; padding:10rpx 10rpx 20rpx}

.coupon{@include row-start; background:linear-gradient(135deg,#fffef7 0%,#fff 100%);
  border:2rpx solid #f0e6d0; border-radius:20rpx; padding:0; margin-bottom:20rpx; overflow:hidden;
  box-shadow: 0 6rpx 20rpx rgba(201,168,108,.10);
  &.my.invalid .left{background:repeating-linear-gradient(45deg,#f3f3f3,#f3f3f3 6rpx,#fafafa 6rpx,#fafafa 12rpx)}
  &.my.invalid .name,.my.invalid .mid{filter:grayscale(1); opacity:.7}
  .left{flex:0 0 200rpx; background:$brand-gradient; color:#fff; padding:24rpx 20rpx; position:relative;
    &.gray{background:#d5d5d5}
    &:after,&:before{content:''; position:absolute; right:-12rpx; width:24rpx; height:24rpx; border-radius:50%; background:$page-bg}
    &:before{top:-12rpx}
    &:after{bottom:-12rpx}
    .v{font-size:56rpx; font-weight:800}
    .min{font-size:22rpx; opacity:.9; margin-top:4rpx}
    .shop{font-size:20rpx; opacity:.75; margin-top:16rpx}
  }
  .mid{flex:1; padding:20rpx;
    .name{font-size:30rpx; color:$ink-black; font-weight:600}
    .range{font-size:22rpx; margin-top:8rpx}
    .scopes{font-size:22rpx; margin-top:4rpx}
    .stock{font-size:22rpx; color:#ff5e52; margin-top:10rpx}
    .status-flag{display:inline-block; margin-top:10rpx; padding:4rpx 14rpx; background:#eee; font-size:22rpx; border-radius:6rpx; color:$text-muted;
      &.exp{background:#fef1e6; color:#ff8a3c}
    }
  }
  .right{padding:20rpx 16rpx; @include column; justify-content:center;
    .btn{height:60rpx; line-height:60rpx; padding:0 20rpx; border-radius:30rpx; font-size:24rpx; @include gold-btn-sm;
      &.disabled{background:#dcdcdc; color:#fff}
      &.go{background:#fff; border:2rpx solid $brand-gold; color:$brand-gold}
    }
  }
}
.empty{@include column; align-items:center; padding:120rpx 0; color:$text-muted;
  .icon{width:180rpx; height:180rpx; opacity:.55}
  .text{font-size:28rpx; margin:30rpx 0; text-align:center; padding:0 40rpx}
}
.tips{padding:20rpx 40rpx; color:$text-muted; font-size:22rpx; line-height:1.7}
</style>

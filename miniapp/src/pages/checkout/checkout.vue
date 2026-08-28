<template>
  <view class="checkout-page">
    <!-- 顶部：下单方式切换 -->
    <view class="order-type-tabs">
      <view
        v-for="t in types"
        :key="t.key"
        class="tab"
        :class="{ active: orderType === t.key }"
        @tap="orderType = t.key">
        {{ t.label }}
      </view>
    </view>

    <!-- 1) 堂食：选桌号 -->
    <view v-if="orderType === 'EAT_IN'" class="card">
      <view class="card-title">🍽️ 堂食信息</view>
      <view class="row">
        <text class="label">桌号</text>
        <input v-model="tableNo" class="input" placeholder="请输入桌号（扫桌码可自动带入）" />
      </view>
      <view class="row">
        <text class="label">用餐人数</text>
        <stepper v-model:value="people" :min="1" :max="30" />
      </view>
    </view>

    <!-- 2) 自取 -->
    <view v-else-if="orderType === 'TAKEAWAY'" class="card">
      <view class="card-title">🥡 自取信息</view>
      <view class="row">
        <text class="label">联系人</text>
        <input v-model="contact.name" class="input" placeholder="您的称呼" />
      </view>
      <view class="row">
        <text class="label">手机号</text>
        <input v-model="contact.phone" type="number" maxlength="11" class="input" placeholder="请输入手机号" />
        <button
          v-if="isMp"
          class="mini-btn gold-outline"
          open-type="getPhoneNumber"
          @getphonenumber="onGetPhone">一键授权</button>
      </view>
      <view class="row">
        <text class="label">期望取餐时间</text>
        <picker mode="time" :value="expectTime" @change="e => expectTime = e.detail.value">
          <view class="input picker">{{ expectTime || '尽快出餐' }}</view>
        </picker>
      </view>
    </view>

    <!-- 3) 外卖 -->
    <view v-else class="card">
      <view class="card-title" @tap="chooseAddress">
        🏠 收货地址 <text class="more">选择 / 新增 ›</text>
      </view>
      <view v-if="address" class="addr-block">
        <view class="addr-top">
          <text class="name">{{ address.name }} {{ address.phone }}</text>
          <text class="tag default" v-if="address.isDefault">默认</text>
        </view>
        <view class="addr-detail">{{ address.detail }}</view>
      </view>
      <view v-else class="addr-empty" @tap="chooseAddress">
        ＋ 点击添加收货地址
      </view>
      <view class="row" style="margin-top:24rpx">
        <text class="label">期望送达</text>
        <picker mode="time" :value="expectTime" @change="e => expectTime = e.detail.value">
          <view class="input picker">{{ expectTime || '尽快送达' }}</view>
        </picker>
      </view>
    </view>

    <!-- 优惠券 -->
    <view class="card" @tap="openCouponSheet = true">
      <view class="row">
        <text class="label">优惠券</text>
        <view class="coupon-right">
          <text v-if="selectedCoupon" class="gold-text">
            已选 满¥{{ (selectedCoupon.minFen/100).toFixed(0) }}减¥{{ (selectedCoupon.valueFen/100).toFixed(0) }}
          </text>
          <text v-else-if="usefulCoupons.length">共 {{ usefulCoupons.length }} 张可用</text>
          <text v-else class="muted">暂无可用券</text>
          <text class="more">›</text>
        </view>
      </view>
    </view>

    <!-- 菜品清单 -->
    <view class="card">
      <view class="card-title">🍱 菜品清单</view>
      <view v-for="it in cart.items" :key="it.id" class="dish-row">
        <image v-if="it.image" class="thumb" :src="it.image" mode="aspectFill" />
        <view class="meta">
          <view class="name">{{ it.name }}</view>
          <view class="sub muted">× {{ it.count }}</view>
        </view>
        <view class="price">¥ {{ (it.priceFen * it.count / 100).toFixed(2) }}</view>
      </view>
    </view>

    <!-- 备注 -->
    <view class="card">
      <view class="card-title">📝 订单备注</view>
      <textarea v-model="remark" class="remark" placeholder="例：少油少辣 / 不要香菜 / 需要儿童椅" />
      <view class="tags">
        <text
          v-for="t in quickTags"
          :key="t"
          class="chip"
          :class="{ active: remark.includes(t) }"
          @tap="toggleTag(t)">{{ t }}</text>
      </view>
    </view>

    <!-- 金额汇总 -->
    <view class="card sum">
      <view class="row"><text class="label">菜品小计</text><text>¥ {{ (cart.totalFen / 100).toFixed(2) }}</text></view>
      <view v-if="packFeeFen" class="row"><text class="label">打包费</text><text>¥ {{ (packFeeFen / 100).toFixed(2) }}</text></view>
      <view v-if="deliveryFeeFen" class="row"><text class="label">配送费</text><text>¥ {{ (deliveryFeeFen / 100).toFixed(2) }}</text></view>
      <view v-if="selectedCoupon" class="row"><text class="label">优惠券抵扣</text><text class="red">
        - ¥ {{ (discountFen / 100).toFixed(2) }}</text></view>
      <view class="row total">
        <text class="label">合计</text>
        <text class="price">¥ {{ (payableFen / 100).toFixed(2) }}</text>
      </view>
    </view>

    <view class="footer-gap"></view>
    <!-- 底部结算条 -->
    <view class="footer">
      <view class="total-wrap">
        <text class="muted">合计</text>
        <text class="big-price">¥ {{ (payableFen / 100).toFixed(2) }}</text>
      </view>
      <button class="gold-btn big" :disabled="submitting || !canSubmit" @tap="doSubmit">
        {{ submitting ? '提交中…' : submitBtnText }}
      </button>
    </view>

    <!-- 优惠券选择 sheet -->
    <view v-if="openCouponSheet" class="mask" @tap="openCouponSheet = false">
      <view class="sheet" @tap.stop>
        <view class="sheet-title">
          <text>选择优惠券</text>
          <text class="muted close" @tap="openCouponSheet = false">×</text>
        </view>
        <scroll-view scroll-y class="sheet-body">
          <view
            v-for="c in usefulCoupons"
            :key="c.id"
            class="coupon-item"
            :class="{ active: selectedCouponId === c.id, disabled: !c.useful }"
            @tap="c.useful && selectCoupon(c.id)">
            <view class="coupon-left">
              <text class="v">¥{{ (c.valueFen/100).toFixed(0) }}</text>
              <text class="min">满{{ (c.minFen/100).toFixed(0) }}可用</text>
            </view>
            <view class="coupon-right-info">
              <view class="c-name">{{ c.name }}</view>
              <view class="c-range muted">{{ c.validDateRange }}</view>
              <view class="c-scopes muted">{{ c.scopeText }}</view>
            </view>
            <view class="tick" v-if="selectedCouponId === c.id">✓</view>
          </view>
          <view v-if="!usefulCoupons.length" class="empty muted">暂无可用优惠券</view>
          <view v-if="selectedCouponId" class="no-use" @tap="selectedCouponId = null">
            不使用优惠券
          </view>
        </scroll-view>
      </view>
    </view>

    <!-- 地址选择跳转：实际用 uni.navigateTo 到 /pages/address/address -->
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import Stepper from '@/components/Stepper.vue'
import { useAppStore } from '@/store/app'
import { useCartStore as _useCart } from '@/store/cart'
import orderApi, { submitOrder } from '@/api/order'
import couponApi from '@/api/coupon'
import { fetchPhoneNumber } from '@/api/wx'

// --- stores
const appStore = useAppStore()
const cart = _useCart()
onMounted(() => { cart.hydrate() })

// --- 基础
const isMp = (() => {
  // #ifdef MP-WEIXIN
  return true
  // #endif
  return false
})()
const types = [
  { key: 'EAT_IN',   label: '🍽️ 堂食' },
  { key: 'TAKEAWAY', label: '🥡 自取' },
  { key: 'DELIVERY', label: '🛵 外卖' }
]
const orderType = ref('EAT_IN')
const submitBtnText = computed(() => {
  if (orderType.value === 'EAT_IN')   return '提交订单·微信支付'
  if (orderType.value === 'TAKEAWAY') return '提交自取·微信支付'
  return '提交外卖·微信支付'
})

// --- 堂食
const tableNo = ref('')
const people  = ref(2)

// --- 自取
const contact = ref({ name: appStore.user?.nickname || '', phone: appStore.user?.phone || '' })
const expectTime = ref('')
function onGetPhone(e) {
  fetchPhoneNumber(e.detail.code).then(p => contact.value.phone = p.phoneNumber)
}

// --- 外卖地址
const address = ref(null)
function chooseAddress() {
  uni.navigateTo({
    url: '/pages/address/address?select=1',
    success: () => { /* 由 address 页面通过 eventChannel 回传 */ },
    events: {
      choose: (addr) => { address.value = addr }
    }
  })
}

// --- 优惠券
const openCouponSheet = ref(false)
const selectedCouponId = ref(null)
const usefulCoupons = ref([])
const selectedCoupon = computed(() =>
  usefulCoupons.value.find(c => c.id === selectedCouponId.value) || null
)

async function loadCoupons() {
  const r = await couponApi.fetchUsefulCoupons(cart.totalFen, appStore.currentStoreId)
  const all = Array.isArray(r?.data) ? r.data : (Array.isArray(r) ? r : [])
  usefulCoupons.value = all.map(c => ({
    ...c,
    useful: cart.totalFen >= (c.minFen || 0)
  }))
  // 默认选最大那张
  if (!selectedCouponId.value && usefulCoupons.value.some(c => c.useful)) {
    const best = usefulCoupons.value
      .filter(c => c.useful)
      .sort((a,b) => b.valueFen - a.valueFen)[0]
    selectedCouponId.value = best?.id || null
  }
}
onMounted(loadCoupons)
function selectCoupon(id) { selectedCouponId.value = id; openCouponSheet.value = false }

// --- 金额
const packFeeFen = computed(() => {
  if (orderType.value === 'EAT_IN') return 0
  return Math.min(cart.items.reduce((s,i)=>s+i.count,0) * 100, 600) // 每份1元，封顶6元
})
const deliveryFeeFen = computed(() => {
  if (orderType.value !== 'DELIVERY') return 0
  return cart.totalFen >= 3000 ? 0 : 500 // 满30免配送
})
const discountFen = computed(() => selectedCoupon.value ? selectedCoupon.value.valueFen : 0)
const payableFen = computed(() =>
  Math.max(0, cart.totalFen + packFeeFen.value + deliveryFeeFen.value - discountFen.value)
)

// --- 备注
const remark = ref('')
const quickTags = ['少辣', '微辣', '不要香菜', '少油', '不要葱', '多打包米饭', '不要味精', '忌口蒜']
function toggleTag(t) {
  const arr = remark.value.split('、').filter(Boolean)
  const idx = arr.indexOf(t)
  if (idx >= 0) arr.splice(idx, 1); else arr.push(t)
  remark.value = arr.join('、')
}

// --- 提交
const submitting = ref(false)
const canSubmit = computed(() => {
  if (cart.isEmpty) return false
  if (orderType.value === 'EAT_IN')   return !!tableNo.value && people.value >= 1
  if (orderType.value === 'TAKEAWAY') return !!contact.value.name && /^1\d{10}$/.test(contact.value.phone)
  if (orderType.value === 'DELIVERY') return !!address.value
  return false
})

async function doSubmit() {
  if (!canSubmit.value) {
    uni.showToast({ title: '请补全订单信息', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    const payload = {
      orderType: orderType.value,
      tableNo:   orderType.value === 'EAT_IN' ? tableNo.value : undefined,
      people:    orderType.value === 'EAT_IN' ? people.value : undefined,
      contact:   orderType.value === 'TAKEAWAY' ? contact.value : { name: address.value?.name, phone: address.value?.phone },
      addressId: orderType.value === 'DELIVERY' ? address.value?.id : undefined,
      address:   orderType.value === 'DELIVERY' ? address.value?.detail : undefined,
      expectTime: expectTime.value,
      remark:    remark.value,
      couponId:  selectedCoupon.value?.id,
      couponDiscountFen: discountFen.value,
      packFeeFen: packFeeFen.value,
      deliveryFeeFen: deliveryFeeFen.value,
      payableFen: payableFen.value
    }
    const res = await submitOrder(payload)
    const order = res?.data?.order || res?.order || res?.data || {}
    const orderId = order.id || res?.data?.id
    // 未支付/已支付走微信支付
    if (order && order.status === 0 && order.payInfo) {
      await import('@/api/wx').then(m => m.wxPay(order.payInfo))
    } else if (payableFen.value > 0) {
      const payApi = await import('@/api/pay.js')
      const payRes = await payApi.createPayment({
        orderType: 'ORDER',
        bizId:     orderId,
        amountFen: payableFen.value
      })
      const wx = await import('@/api/wx.js')
      await wx.wxPay(payRes.data)
    }
    cart.clear()
    uni.redirectTo({ url: `/pages/orderDetail/orderDetail?id=${orderId}` })
  } catch (e) {
    // 若支付取消，仍跳订单详情（状态=待支付）
    if (String(e?.errMsg || '').includes('cancel')) {
      uni.redirectTo({ url: `/pages/orderList/orderList` })
    }
  } finally {
    submitting.value = false
  }
}
</script>

<style lang="scss">
@import '@/uni.scss';
.checkout-page{padding:20rpx; padding-bottom: 140rpx; background:$page-bg; min-height:100vh;}
.order-type-tabs{ @include row-center; background:#fff; border-radius:$card-radius; padding:10rpx; margin-bottom:20rpx;
  .tab{flex:1; text-align:center; padding:16rpx 0; border-radius:8rpx; color:$text-muted; font-size:28rpx;
    &.active{ background:$brand-gradient; color:#fff; font-weight:600; }}
}
.card{ @include card; margin-bottom:20rpx;}
.card-title{font-size:30rpx; font-weight:600; color:$ink-black; margin-bottom:20rpx; .more{float:right; color:$brand-gold; font-size:24rpx; font-weight:400}}
.row{@include row-between; padding:14rpx 0; border-top:1rpx dashed #eee; &:first-child{border-top:0}}
.label{color:$ink-black; font-size:28rpx; min-width:160rpx}
.input{flex:1; padding:10rpx 16rpx; background:#fafafa; border-radius:8rpx; font-size:28rpx;
  &.picker{color:$ink-black}
}
.mini-btn{font-size:22rpx; padding:0 16rpx; line-height:52rpx; height:52rpx; margin-left:16rpx; }
.gold-outline{border:2rpx solid $brand-gold; color:$brand-gold; background:#fff;}
.addr-block{background:#fafaf5; border-radius:12rpx; padding:20rpx;
  .addr-top{@include row-start; .name{font-size:28rpx; color:$ink-black; font-weight:600; margin-right:12rpx}}
  .tag.default{background:$brand-gold; color:#fff; font-size:20rpx; padding:2rpx 12rpx; border-radius:20rpx}
  .addr-detail{color:$text-muted; font-size:26rpx; margin-top:8rpx}
}
.addr-empty{padding:40rpx; text-align:center; border:2rpx dashed $brand-gold; color:$brand-gold; border-radius:12rpx}
.dish-row{@include row-start; padding:14rpx 0; border-top:1rpx dashed #eee; &:first-child{border-top:0};
  .thumb{width:100rpx; height:100rpx; border-radius:12rpx; margin-right:16rpx; background:#eee}
  .meta{flex:1; .name{font-size:28rpx; color:$ink-black} .sub{font-size:24rpx; margin-top:6rpx}}
  .price{color:$brand-gold; font-weight:600; font-size:28rpx}
}
.remark{width:100%; min-height:160rpx; background:#fafafa; border-radius:12rpx; padding:20rpx; font-size:28rpx; box-sizing:border-box}
.tags{@include row-wrap; margin-top:16rpx;
  .chip{@include chip; }
}
.sum{ .row.total{margin-top:10rpx; padding-top:16rpx; border-top:2rpx solid #eee}
  .price{color:$brand-gold; font-size:36rpx; font-weight:700}
}
.footer{@include footer-bar; }
.total-wrap{flex:1; @include row-start;
  .muted{margin-right:12rpx}
  .big-price{color:$brand-gold; font-size:40rpx; font-weight:700}
}
.footer-gap{height: 40rpx}
/* coupon sheet */
.mask{position:fixed; inset:0; background:rgba(0,0,0,.5); z-index:99; @include column; justify-content:flex-end}
.sheet{background:#fff; border-top-left-radius:24rpx; border-top-right-radius:24rpx; max-height:72vh; @include column;
  .sheet-title{@include row-between; padding:28rpx 32rpx; font-size:32rpx; font-weight:600; color:$ink-black;
    .close{font-size:40rpx; color:#bbb; padding:0 20rpx}
  }
  .sheet-body{padding:0 24rpx 48rpx; flex:1}
}
.coupon-item{@include row-start; background:linear-gradient(135deg,#fffef7 0%,#fff 100%); border:2rpx solid #f0e6d0;
  border-radius:16rpx; padding:20rpx; margin-bottom:16rpx; position:relative;
  &.active{border-color:$brand-gold; box-shadow: 0 6rpx 20rpx rgba(201,168,108,.22)}
  &.disabled{opacity:.45}
  .coupon-left{min-width:170rpx; padding-right:20rpx; border-right:2rpx dashed #f0e6d0;
    text-align:center; @include column;
    .v{font-size:52rpx; font-weight:700; color:$brand-gold}
    .min{font-size:22rpx; color:$text-muted; margin-top:4rpx}
  }
  .coupon-right-info{flex:1; padding-left:20rpx; @include column;
    .c-name{font-size:28rpx; color:$ink-black; font-weight:600}
    .c-range{font-size:22rpx; margin-top:8rpx}
    .c-scopes{font-size:22rpx; margin-top:4rpx}
  }
  .tick{position:absolute; top:20rpx; right:20rpx; width:40rpx; height:40rpx; border-radius:50%; background:$brand-gold; color:#fff; text-align:center; line-height:40rpx; font-size:24rpx}
}
.no-use{text-align:center; padding:20rpx; color:$brand-deep-gold; font-size:26rpx; margin-top:8rpx}
.empty{text-align:center; padding:60rpx 0; color:#bbb}
.gold-text{color:$brand-gold; font-weight:600; font-size:26rpx}
.coupon-right{@include row-center; .more{margin-left:16rpx; color:#ccc}}
</style>

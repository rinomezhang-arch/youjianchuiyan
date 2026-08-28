<template>
  <view class="me-page">
    <!-- 顶部个人卡 -->
    <view class="hero">
      <view class="hero-bg"></view>
      <view class="hero-inner">
        <view class="avatar" @tap="onUserTap">
          <image v-if="appStore.userAvatar" :src="appStore.userAvatar" mode="aspectFill" />
          <text v-else class="avatar-text">{{ avatarText }}</text>
        </view>
        <view class="hero-info">
          <text class="h-name">{{ appStore.userName }}</text>
          <text class="h-sub">
            <block v-if="appStore.isLogin">{{ appStore.currentStore?.name || '又见炊烟' }} 会员</block>
            <block v-else>点击登录，查看消费记录与会员福利</block>
          </text>
        </view>
        <text v-if="!appStore.isLogin" class="login-btn" @tap="toLogin">登录 ›</text>
      </view>

      <view class="stat-row">
        <view class="stat" @tap="openList('booking')">
          <text class="num">{{ myStats.booking }}</text>
          <text class="lbl">我的预订</text>
        </view>
        <view class="stat" @tap="openList('fav')">
          <text class="num">{{ myStats.fav }}</text>
          <text class="lbl">我的收藏</text>
        </view>
        <view class="stat" @tap="openList('coupon')">
          <text class="num">{{ myStats.coupon }}</text>
          <text class="lbl">优惠券</text>
        </view>
        <view class="stat" @tap="openList('order')">
          <text class="num">{{ myStats.order }}</text>
          <text class="lbl">历史订单</text>
        </view>
      </view>
    </view>

    <!-- 门店切换 -->
    <view class="card" v-if="appStore.stores.length" @tap="onPickStore">
      <text class="card-icon">📍</text>
      <view class="card-mid">
        <text class="card-title">当前门店 · {{ appStore.currentStore?.name || '又见炊烟' }}</text>
        <text class="card-desc">{{ appStore.currentStore?.address || '点击切换门店' }}</text>
      </view>
      <text class="card-arrow">›</text>
    </view>

    <!-- 快捷入口 -->
    <view class="card-group">
      <view class="list-card" v-for="(row, idx) in listGroups" :key="idx">
        <view
          v-for="item in row"
          :key="item.key"
          class="list-item"
          @tap="onItemTap(item.key)"
        >
          <text class="li-icon" :style="{color: item.color}">{{ item.icon }}</text>
          <text class="li-label">{{ item.label }}</text>
          <text class="li-arrow">›</text>
        </view>
      </view>
    </view>

    <!-- 客服 -->
    <view class="card" @tap="contact">
      <text class="card-icon">💬</text>
      <view class="card-mid">
        <text class="card-title">联系客服</text>
        <text class="card-desc">预订/消费有疑问，一键呼叫门店</text>
      </view>
      <text class="card-arrow">›</text>
    </view>

    <view class="brand-foot">又见炊烟 · 匠心徽菜 家的味道</view>
    <view style="height: 60rpx"></view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { fetchStores } from '@/api/store'
import { fetchMyBookings } from '@/api/booking'

const appStore = useAppStore()

const avatarText = computed(() => (appStore.userName || '游').slice(0, 1).toUpperCase())
const myStats = reactive({ booking: 0, fav: 0, coupon: 0, order: 0 })

const listGroups = [
  [
    { key: 'booking', icon: '📅',  label: '我的预订', color: '#C9A86C' },
    { key: 'order',   icon: '🧾',  label: '我的订单', color: '#E53935' },
    { key: 'menu',    icon: '🍲',  label: '浏览菜单', color: '#8FBC74' },
    { key: 'pay',     icon: '💰',  label: '待支付',   color: '#E5823A' }
  ],
  [
    { key: 'coupon',  icon: '🎟',  label: '优惠券',   color: '#FF8F66' },
    { key: 'vip',     icon: '🎖',  label: '会员码',   color: '#9D7D48' },
    { key: 'qr',      icon: '📲',  label: '我的桌码', color: '#3B82F6' },
    { key: 'fav',     icon: '⭐',  label: '我的收藏', color: '#F6B84E' }
  ],
  [
    { key: 'addr',    icon: '📍',  label: '收货地址', color: '#10B981' },
    { key: 'fb',      icon: '💬',  label: '意见反馈', color: '#8B5CF6' },
    { key: 'share',   icon: '📤',  label: '分享邀请', color: '#4A90E2' },
    { key: 'set',     icon: '⚙️',  label: '设置',     color: '#6B7280' }
  ]
]

async function loadData() {
  // 门店
  try {
    const s = await fetchStores()
    if (Array.isArray(s) && s.length) {
      appStore.setStores(s)
      if (!appStore.currentStore) appStore.setCurrentStoreId(s[0].id)
    }
  } catch {}

  // 预订数量统计
  if (appStore.isLogin) {
    try {
      const r = await fetchMyBookings({ size: 500 })
      const arr = Array.isArray(r) ? r : (r?.records || r?.list || [])
      myStats.booking = arr.length
    } catch {}
  } else {
    myStats.booking = 0
  }
}

function onPickStore() {
  const list = appStore.stores || []
  if (!list.length) return
  uni.showActionSheet({
    itemList: list.map(s => `${s.name} · ${s.address || '又见炊烟'}`),
    success: (r) => {
      const store = list[r.tapIndex]
      if (store) appStore.setCurrentStoreId(store.id)
    }
  })
}

function onUserTap() {
  if (!appStore.isLogin) toLogin()
}
function toLogin() { uni.navigateTo({ url: '/pages/login/login' }) }

function openList(k) {
  const notLoginCheck = () => {
    if (!appStore.isLogin) {
      uni.showModal({
        title: '请先登录', content: '登录后查看我的记录',
        confirmText: '去登录',
        success: (r) => r.confirm && uni.navigateTo({ url: '/pages/login/login' })
      })
      return true
    }
    return false
  }
  if (k === 'booking') { if (notLoginCheck()) return; uni.navigateTo({ url: '/pages/bookingList/bookingList' }); return }
  if (k === 'menu')    { uni.switchTab({ url: '/pages/menu/menu' }); return }
  if (k === 'order')   { if (notLoginCheck()) return; uni.navigateTo({ url: '/pages/orderList/orderList' }); return }
  if (k === 'coupon')  { uni.navigateTo({ url: '/pages/coupon/coupon' }); return }
  if (k === 'fav')     { uni.navigateTo({ url: '/pages/menu/menu?tab=fav' }); return }
}

function onItemTap(k) {
  if (['booking','menu','order','fav','coupon'].includes(k)) return openList(k)
  if (k === 'set')     { uni.navigateTo({ url: '/pages/setting/setting' }); return }
  if (k === 'addr')    { uni.navigateTo({ url: '/pages/address/address' }); return }
  if (k === 'qr')      { uni.navigateTo({ url: '/pages/qrcode/qrcode?type=member' }); return }
  if (k === 'fb')      { uni.navigateTo({ url: '/pages/feedback/feedback' }); return }
  if (k === 'pay')     { uni.navigateTo({ url: '/pages/orderList/orderList?status=0' }); return }
  if (k === 'vip')     { uni.navigateTo({ url: '/pages/qrcode/qrcode?type=member' }); return }
  if (k === 'share') {
    // #ifdef MP-WEIXIN
    uni.showShareMenu && uni.showShareMenu({ withShareTicket: true, menus: ['shareAppMessage','shareTimeline'] })
    uni.showToast({ title: '点击右上角… → 分享给朋友', icon: 'none' })
    // #endif
    // #ifndef MP-WEIXIN
    uni.navigateTo({ url: '/pages/qrcode/qrcode?type=invite' })
    // #endif
    return
  }
}

function contact() {
  const phone = appStore.currentStore?.phone
  if (!phone) return uni.showToast({ title: '门店电话暂未设置', icon: 'none' })
  uni.makePhoneCall({ phoneNumber: String(phone).replace(/[^\d-]/g,'') }).catch(() => {})
}

onMounted(loadData)
onPullDownRefresh(async () => { await loadData(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.me-page { padding: $spacing-md $spacing-md 0; background: $brand-bg; min-height: 100vh; box-sizing: border-box; }

/* 顶部个人卡 */
.hero { position: relative; border-radius: $radius-lg; overflow: hidden; margin-bottom: $spacing-md; }
.hero-bg {
  position: absolute; inset: 0;
  background:
    radial-gradient(120% 80% at 100% -10%, #FFE199 0%, transparent 60%),
    linear-gradient(135deg, #2E2414 0%, #564428 45%, #C9A86C 100%);
}
.hero-inner { position: relative; padding: $spacing-lg $spacing-md $spacing-md; display:flex; align-items:center; gap: $spacing-md; }
.avatar {
  width: 120rpx; height: 120rpx; border-radius: 50%; background: rgba(255,255,255,0.15);
  border: 2rpx solid rgba(255,255,255,0.35); overflow: hidden;
  display:flex; align-items:center; justify-content:center;
}
.avatar image { width: 100%; height: 100%; }
.avatar-text { color:#FFD98C; font-size: 50rpx; font-weight: 800; }
.hero-info { flex: 1; color: #fff; }
.h-name { font-size: $font-lg; font-weight: 700; display:block; }
.h-sub { font-size: $font-xs; color: rgba(255,255,255,0.78); margin-top: 6rpx; display:block; }
.login-btn {
  background: rgba(255,255,255,0.2); border: 1rpx solid rgba(255,255,255,0.35);
  padding: 10rpx 24rpx; color:#fff; border-radius: $radius-pill; font-size: $font-xs;
}

.stat-row {
  position: relative; padding: $spacing-md;
  display: grid; grid-template-columns: repeat(4, 1fr);
  border-top: 1rpx solid rgba(255,255,255,0.15);
}
.stat { display:flex; flex-direction: column; align-items:center; color:#fff; }
.stat .num { font-size: 40rpx; font-weight: 800; }
.stat .lbl { font-size: 22rpx; color: rgba(255,255,255,0.75); margin-top: 6rpx; }

/* 卡片 */
.card {
  display:flex; align-items: center; gap: $spacing-sm;
  background: #fff; border-radius: $radius-lg; padding: $spacing-md; margin-bottom: $spacing-md;
  box-shadow: $shadow-card;
}
.card-icon { font-size: 44rpx; width: 60rpx; text-align: center; }
.card-mid { flex: 1; display:flex; flex-direction: column; }
.card-title { font-size: $font-sm; color: $text-1; font-weight: 600; }
.card-desc { font-size: $font-xs; color: $text-3; margin-top: 4rpx; }
.card-arrow { color: #ccc; font-size: 36rpx; padding: 0 6rpx; }

.list-card {
  background: #fff; border-radius: $radius-lg; box-shadow: $shadow-card;
  margin-bottom: $spacing-md; overflow: hidden;
}
.list-item {
  display: flex; align-items: center; padding: $spacing-md;
  border-bottom: 1rpx solid #F4F0E6;
}
.list-item:last-child { border-bottom: 0; }
.li-icon { width: 60rpx; text-align: center; font-size: 36rpx; }
.li-label { flex: 1; font-size: $font-sm; color: $text-1; }
.li-arrow { color: #ccc; font-size: 32rpx; }

.brand-foot {
  text-align: center; color: $text-4; font-size: 22rpx; padding: 30rpx 0 10rpx; letter-spacing: 2rpx;
}
</style>

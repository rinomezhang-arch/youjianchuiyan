<template>
  <view class="bl-page">
    <!-- 顶部状态筛选 -->
    <view class="tabs">
      <text
        v-for="t in tabs"
        :key="t.value"
        :class="['tab', {active: curStatus === t.value}]"
        @tap="curStatus = t.value"
      >{{ t.label }}<text v-if="t.count != null" class="cnt">({{ t.count }})</text></text>
    </view>

    <!-- 列表 -->
    <view v-if="list.length" class="bl-list">
      <view class="bl-card" v-for="b in list" :key="b.id">
        <view class="bl-head">
          <text class="bl-store">{{ b.storeName || (appStore.currentStore?.name) || '又见炊烟' }}</text>
          <text :class="['bl-status', statusClass(b.status)]">{{ statusText(b.status) }}</text>
        </view>

        <view class="bl-main" @tap="openDetail(b)">
          <view class="row">
            <text class="k">日期</text>
            <text class="v gold-text">{{ b.bookingDate }}  {{ b.bookingTime }}</text>
          </view>
          <view class="row">
            <text class="k">人数/包厢</text>
            <text class="v">{{ b.guestCount }} 位 · {{ roomText(b.roomType) }}</text>
          </view>
          <view class="row">
            <text class="k">联系人</text>
            <text class="v">{{ b.customerName }} · {{ maskPhone(b.customerPhone) }}</text>
          </view>
          <view class="row" v-if="b.packageName">
            <text class="k">宴会套餐</text>
            <text class="v">{{ b.packageName }}</text>
          </view>
          <view class="row" v-if="b.remark">
            <text class="k">备注</text>
            <text class="v">{{ b.remark }}</text>
          </view>
        </view>

        <view class="bl-footer">
          <text class="f-id">预订号：YJCY{{ String(b.id).padStart(6,'0') }}</text>
          <view class="f-actions">
            <view
              v-if="canCancel(b.status)"
              class="btn-outline sm"
              @tap="onCancel(b)"
            >取消预订</view>
            <view
              v-if="canPay(b.status)"
              class="btn-gold sm"
              @tap="onPay(b)"
            >支付定金</view>
            <view
              class="btn-outline sm"
              @tap="callStore(b)"
            >联系门店</view>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-box">
      <text class="e-icon">📅</text>
      <text class="e-text">{{ curStatus === 'ALL' ? '还没有预订记录' : '暂无此状态的预订' }}</text>
      <view class="e-btn" @tap="goBook">去预订一个</view>
    </view>

    <view style="height: 80rpx"></view>
  </view>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { onPullDownRefresh, onLoad } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { fetchMyBookings, cancelBooking } from '@/api/booking'
import { createPayOrder } from '@/api/pay'
import { wechatPay, requestSubscribe } from '@/api/wx'

const appStore = useAppStore()

const tabs = reactive([
  { label: '全部', value: 'ALL' },
  { label: '待确认', value: 0 },
  { label: '已确认', value: 1 },
  { label: '已完成', value: 2 },
  { label: '已取消', value: -1 }
])
const curStatus = ref('ALL')

const rawList = ref([])
async function load() {
  try {
    let r = await fetchMyBookings({ size: 500 })
    if (!Array.isArray(r)) r = r?.records || r?.list || []
    rawList.value = r || []
    // 更新 tab 数字
    tabs[1].count = rawList.value.filter(x => Number(x.status) === 0).length
    tabs[2].count = rawList.value.filter(x => Number(x.status) === 1).length
    tabs[3].count = rawList.value.filter(x => Number(x.status) === 2).length
    tabs[4].count = rawList.value.filter(x => Number(x.status) === -1).length
  } catch (e) { rawList.value = [] }
}

const list = computed(() => {
  if (curStatus.value === 'ALL') return rawList.value
  return rawList.value.filter(x => Number(x.status) === Number(curStatus.value))
})

function statusText(s) {
  const n = Number(s)
  if (n === 0)   return '待门店确认'
  if (n === 1)   return '已确认'
  if (n === 2)   return '已用餐'
  if (n === 3)   return '已完成'
  if (n === -1)  return '已取消'
  return '未知状态'
}
function statusClass(s) {
  const n = Number(s)
  if (n === 0)   return 'pending'
  if (n === 1)   return 'confirmed'
  if (n === 2 || n === 3) return 'done'
  if (n === -1)  return 'canceled'
  return 'pending'
}
function roomText(r) {
  if (!r) return '大厅散台'
  return { SMALL: '小包厢', MID: '中包厢', BIG: '大包厢', HALL: '宴会厅' }[r] || r
}
function maskPhone(p) {
  const s = String(p || '')
  return s.length >= 11 ? s.slice(0,3) + '****' + s.slice(-4) : s
}

function canCancel(s) { const n = Number(s); return n === 0 || n === 1 }
function canPay(s)   { const n = Number(s); return n === 1 }   // 已确认需支付定金

function openDetail(b) {
  uni.navigateTo({ url: `/pages/bookingDetail/bookingDetail?id=${b.id}` })
}
async function onCancel(b) {
  const r = await uni.showModal({
    title: '确认取消预订？',
    content: '如行程变动，建议提前 2 小时告知门店',
    confirmText: '确认取消',
    confirmColor: '#E53935'
  })
  if (!r.confirm) return
  try {
    await cancelBooking(b.id, '用户在小程序端取消')
    uni.showToast({ title: '已取消', icon: 'success' })
    load()
  } catch {}
}
async function onPay(b) {
  try {
    uni.showLoading({ title: '发起支付…', mask: true })
    const sign = await createPayOrder({
      orderType: 'booking',
      bizId: b.id,
      amountFen: Math.round(Number(b.deposit || 20000))   // 定金默认200元=20000分
    })
    uni.hideLoading()
    await wechatPay(sign)
    uni.showToast({ title: '支付成功', icon: 'success' })
    try { await requestSubscribe('PAY_SUCCESS') } catch {}
    load()
  } catch (e) {
    uni.hideLoading()
    uni.showModal({
      title: '支付未完成',
      content: e?.message || '支付失败或被取消，可稍后重试',
      showCancel: false
    })
  }
}
function callStore(b) {
  const phone = (b && b.storePhone) || appStore.currentStore?.phone
  if (!phone) return uni.showToast({ title: '门店电话暂未设置', icon: 'none' })
  uni.makePhoneCall({ phoneNumber: String(phone).replace(/[^\d-]/g,'') }).catch(() => {})
}
function goBook() { uni.switchTab({ url: '/pages/book/book' }) }

onMounted(async () => {
  // 若未登录，先引导登录
  if (!appStore.isLogin) {
    const r = await uni.showModal({
      title: '请先登录',
      content: '登录后查看我的预订记录',
      confirmText: '去登录'
    })
    if (r.confirm) { uni.navigateTo({ url: '/pages/login/login' }); return }
  }
  await load()

  // 支持 ?newId=xxx 参数，高亮刚新建的预订
  const pages = getCurrentPages()
  const pg = pages[pages.length - 1]
  const id = Number(pg?.options?.newId || 0)
  if (id) {
    setTimeout(() => {
      uni.showModal({
        title: '预订提交成功',
        content: `预订号 YJCY${String(id).padStart(6,'0')}\n门店将在营业时间内电话确认。`,
        showCancel: false
      })
    }, 300)
  }
})
onPullDownRefresh(async () => { await load(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.bl-page { padding: $spacing-md; background: $brand-bg; min-height: 100vh; box-sizing: border-box; }

.tabs {
  display:flex; overflow-x: auto; white-space: nowrap;
  background:#fff; border-radius: $radius-md; padding: $spacing-xs; margin-bottom: $spacing-md;
  box-shadow: $shadow-card; gap: 8rpx;
}
.tab {
  padding: 14rpx 24rpx; border-radius: $radius-pill; font-size: $font-xs; color: $text-2; flex-shrink: 0;
}
.tab.active { background: linear-gradient(135deg, #FFF3D6, #FFE3A8); color: $brand-gold-dark; font-weight: 700; }
.cnt { margin-left: 6rpx; font-weight: 400; opacity: 0.9; }

.bl-list { display:flex; flex-direction: column; gap: $spacing-md; }
.bl-card { background:#fff; border-radius: $radius-lg; box-shadow: $shadow-card; overflow: hidden; }
.bl-head {
  display:flex; justify-content: space-between; align-items: center;
  padding: $spacing-sm $spacing-md; border-bottom: 1rpx solid #F4F0E6;
}
.bl-store { font-weight: 700; color: $text-1; font-size: $font-md; }
.bl-status {
  font-size: $font-xs; padding: 4rpx 16rpx; border-radius: $radius-pill; font-weight: 600;
}
.bl-status.pending   { background: #FFF5DE; color: $brand-gold-dark; }
.bl-status.confirmed { background: #E2F6D7; color: #3C8C1A; }
.bl-status.done      { background: #EEEEEE; color: $text-3; }
.bl-status.canceled  { background: #FFEAEA; color: #E53935; }

.bl-main { padding: $spacing-sm $spacing-md; }
.row { display:flex; padding: 6rpx 0; font-size: $font-sm; }
.row .k { width: 180rpx; color: $text-3; flex-shrink: 0; }
.row .v { flex: 1; color: $text-1; }

.bl-footer {
  padding: $spacing-sm $spacing-md; display:flex; justify-content: space-between; align-items: center;
  border-top: 1rpx solid #F4F0E6; background: #FFFBF0;
}
.f-id { font-size: 22rpx; color: $text-4; }
.f-actions { display:flex; gap: 12rpx; }
.btn-outline.sm, .btn-gold.sm { height: 60rpx; line-height: 58rpx; padding: 0 24rpx; font-size: $font-xs; border-radius: $radius-pill; }
.btn-outline.sm { border: 1rpx solid #ddd; color: $text-2; background:#fff; }
.btn-gold.sm { background: linear-gradient(135deg,#E6D4A8,#C9A86C); color:#fff; box-shadow: 0 4rpx 12rpx rgba(201,168,108,0.3); }

.empty-box { padding: 180rpx 0; text-align: center; }
.e-icon { font-size: 100rpx; display:block; margin-bottom: 20rpx; opacity: 0.6; }
.e-text { color: $text-3; font-size: $font-sm; display:block; margin-bottom: $spacing-md; }
.e-btn {
  display: inline-block; padding: 16rpx 48rpx; color:#fff; font-weight: 700;
  background: linear-gradient(135deg,#E6D4A8,#C9A86C); border-radius: $radius-pill;
}
</style>

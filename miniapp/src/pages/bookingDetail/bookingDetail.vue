<template>
  <view class="booking-detail">
    <!-- 顶部状态卡片 -->
    <view class="status-card" :class="'s-'+detail.status">
      <view class="status-text">{{ statusText }}</view>
      <view class="sub">{{ subText }}</view>
      <view class="id muted">预订号 #{{ detail.orderNo || detail.id }}</view>
    </view>

    <!-- 宴席/桌信息 -->
    <view class="card">
      <view class="row big">
        <text class="ico">📍</text>
        <view class="flex1">
          <view class="name">{{ detail.storeName || appStore.currentStore?.name || '又见炊烟' }}</view>
          <view class="muted" v-if="detail.address">{{ detail.address }}</view>
        </view>
        <text class="call" @tap="doCall">致电</text>
      </view>
      <view class="divider"></view>
      <view class="row"><text class="label">预订日期</text><text class="value">{{ detail.date }}</text></view>
      <view class="row"><text class="label">用餐时段</text><text class="value">{{ detail.period || detail.timeSlot || detail.time }}</text></view>
      <view class="row"><text class="label">用餐人数</text><text class="value">{{ detail.people || detail.guests }} 人</text></view>
      <view class="row" v-if="detail.roomName || detail.tableNo">
        <text class="label">{{ detail.roomName ? '包厢' : '桌号' }}</text>
        <text class="value gold">{{ detail.roomName || detail.tableNo }}</text>
      </view>
      <view class="row" v-if="detail.feeType">
        <text class="label">套餐/标准</text>
        <text class="value">{{ detail.feeType }} · ¥{{ (detail.feePerPerson/100).toFixed(0) }}/人</text>
      </view>
      <view class="row" v-if="detail.remark || detail.notes">
        <text class="label">备注</text>
        <text class="value right">{{ detail.remark || detail.notes }}</text>
      </view>
      <view class="row" v-if="detail.depositFen">
        <text class="label">已付定金</text>
        <text class="value paid">¥{{ (detail.depositFen/100).toFixed(2) }}</text>
      </view>
    </view>

    <!-- 联系人 -->
    <view class="card">
      <view class="card-title">📞 联系人信息</view>
      <view class="row"><text class="label">姓名</text><text class="value">{{ detail.contactName || detail.name }}</text></view>
      <view class="row"><text class="label">手机</text>
        <text class="value link" @tap="doCall(detail.contactPhone || detail.phone)">
          {{ detail.contactPhone || detail.phone }}
        </text>
      </view>
      <view class="row" v-if="detail.gender"><text class="label">称谓</text>
        <text class="value">{{ detail.gender === 1 ? '先生' : '女士' }}</text>
      </view>
    </view>

    <!-- 取消原因 / 商家留言 -->
    <view class="card" v-if="detail.status === 5 && detail.cancelReason">
      <view class="card-title">⚠️ 取消原因</view>
      <view class="reason-block">{{ detail.cancelReason }}</view>
    </view>
    <view class="card" v-if="detail.merchantNote">
      <view class="card-title">📝 商家提醒</view>
      <view class="note-block">{{ detail.merchantNote }}</view>
    </view>

    <view class="gap"></view>

    <!-- 底部操作条 -->
    <view class="footer">
      <button class="btn ghost" v-if="detail.status===1||detail.status===2" @tap="doShare">分享预订</button>
      <button class="btn ghost" v-if="canModify" @tap="doModify">修改预订</button>
      <button class="btn ghost" v-if="detail.status===1||detail.status===2" @tap="doAgain">再订一次</button>
      <button class="btn gold"  v-if="detail.status===1 && !detail.depositFen" @tap="doPayDeposit">支付定金</button>
      <button class="btn danger" v-if="canCancel" @tap="doCancel">取消预订</button>
      <button class="btn gold" v-else-if="detail.status===4||detail.status===3" @tap="doRateAndBack">查看评价</button>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import bookingApi from '@/api/booking'
import { wxPay, toast } from '@/api/wx'
import payApi from '@/api/pay'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
const id = ref(null)
const detail = ref({})

onLoad(q => { id.value = q.id; load() })

async function load() {
  try {
    const r = await bookingApi.fetchBookingDetail(id.value)
    detail.value = r?.data?.booking || r?.data || r || {}
  } catch(e) {
    uni.showToast({ title: '预订详情加载失败', icon:'none' })
  }
}

const STATUS = {
  0: '待确认', 1: '已确认·待到店', 2: '已入席', 3: '用餐中',
  4: '已完成', 5: '已取消', 6: '已拒绝'
}
const statusText = computed(() => STATUS[detail.value.status] || '处理中')
const subText = computed(() => {
  switch (detail.value.status) {
    case 0: return '商家将在10分钟内确认您的预订'
    case 1: return '我们已为您保留桌位，请准时到店'
    case 2: return '顾客已入座，正在享用佳肴'
    case 3: return '用餐中，服务铃已就绪'
    case 4: return '用餐完成，欢迎下次光临'
    case 5: return '预订已取消'
    case 6: return '很抱歉，该时段商家无法接待'
    default: return ''
  }
})

const canCancel = computed(() => [0, 1, 6].includes(detail.value.status))
const canModify = computed(() => [0, 1].includes(detail.value.status))

/* 操作 */
function doCall(p){
  uni.makePhoneCall({
    phoneNumber: String(p || detail.value.storePhone || appStore.currentStore?.phone || '4000000000').replace(/[^\d-]/g,'')
  }).catch(()=>{})
}
function doAgain(){
  uni.redirectTo({
    url: `/pages/book/book?cloneId=${detail.value.id}`
  })
}
function doModify(){
  uni.redirectTo({
    url: `/pages/book/book?editId=${detail.value.id}`
  })
}
async function doCancel(){
  const r = await uni.showModal({ title:'确认取消预订？', content:'取消后桌位/包厢将释放给其他顾客' })
  if (!r.confirm) return
  await bookingApi.cancelBooking(detail.value.id, '顾客取消')
  toast('已取消')
  setTimeout(load, 400)
}
async function doPayDeposit(){
  // 若后端返回了 depositPayInfo 直接跳支付；否则用默认定金接口
  const deposit = detail.value.depositNeedFen || Math.min(20000, (detail.value.totalFeeFen || 10000) * 0.2)
  try {
    const r = await payApi.createPayment({ orderType:'BOOKING_DEPOSIT', bizId: detail.value.id, amountFen: deposit })
    await wxPay(r?.data || r)
    toast('支付成功')
    setTimeout(load, 400)
  } catch(e) {
    if (!String(e?.errMsg || '').includes('cancel')) toast('支付失败')
  }
}
function doShare(){
  uni.showShareMenu && uni.showShareMenu({ withShareTicket:true, menus:['shareAppMessage','shareTimeline'] })
  toast('请点击右上角分享')
}
function doRateAndBack(){ uni.navigateBack() }

// 小程序分享（用户点"分享"会自动带参数，同时也便于订位商家追踪）
// #ifdef MP-WEIXIN
import { defineExpose } from 'vue'
function onShareAppMessage() {
  return {
    title: `又见炊烟 ${detail.value.date} ${detail.value.people}人位 预订确认`,
    path:  `/pages/bookingDetail/bookingDetail?id=${detail.value.id}`
  }
}
defineExpose({ onShareAppMessage })
// #endif
</script>

<style lang="scss">
@import '@/uni.scss';
.booking-detail{background:$page-bg; min-height:100vh; padding-bottom:180rpx}
.status-card{background:$brand-gradient; color:#fff; padding:50rpx 32rpx;
  .status-text{font-size:44rpx; font-weight:700}
  .sub{margin-top:10rpx; font-size:26rpx; opacity:.92}
  .id{margin-top:24rpx; font-size:22rpx; opacity:.8; color:#fff}
  &.s-0{background:linear-gradient(135deg,#f0c36d,#b88646)}
  &.s-4,&.s-3{background:linear-gradient(135deg,#C9A86C,#8F7030)}
  &.s-5{background:linear-gradient(135deg,#bbb,#777)}
  &.s-6{background:linear-gradient(135deg,#E57373,#b23a3a)}
}
.card{@include card; margin:20rpx;}
.card-title{font-size:30rpx; font-weight:600; color:$ink-black; margin-bottom:18rpx}
.row{@include row-between; padding:14rpx 0; border-top:2rpx dashed #eee; &:first-child{border-top:0}
  &.big{align-items:flex-start}
  .label{font-size:28rpx; color:$text-muted; min-width:180rpx}
  .value{font-size:28rpx; color:$ink-black; &.right{text-align:right; max-width:60%; word-break:break-all}
    &.gold{color:$brand-gold; font-weight:600}
    &.paid{color:#22a06b; font-weight:600}
    &.link{color:$brand-gold; text-decoration:underline}
  }
  .ico{font-size:38rpx; margin-right:16rpx}
  .flex1{flex:1; .name{font-size:30rpx; font-weight:600; color:$ink-black} .muted{margin-top:6rpx}}
  .call{color:$brand-gold; text-decoration:underline; font-size:26rpx}
}
.divider{height:2rpx; background:#f2eee1; margin:16rpx 0}
.reason-block,.note-block{padding:20rpx; background:#fffaf0; border:2rpx dashed #e6cf9e; border-radius:12rpx; color:#6b4f1b; font-size:26rpx; line-height:1.6}
.reason-block{background:#ffeaea; border-color:#e6b3b3; color:#8c2b2b}
.gap{height:40rpx}
.footer{@include footer-bar; flex-wrap:wrap; gap:12rpx;
  .btn{flex:1; min-width:160rpx; height:76rpx; line-height:76rpx; border-radius:38rpx; font-size:26rpx; margin-left:0}
  .btn.ghost{background:#f7f3eb; color:$brand-deep-gold}
  .btn.gold{@include gold-btn}
  .btn.danger{background:#ffe8e4; color:#e25246}
}
</style>

<template>
  <view class="order-detail-page">
    <!-- 顶部状态卡片 -->
    <view class="status-card">
      <view class="status-text">{{ statusText(order.status) }}</view>
      <view class="sub">{{ subText(order.status) }}</view>
      <view class="progress">
        <view
          v-for="(s, i) in timeline"
          :key="i"
          class="node"
          :class="{ done: order.status >= s.key, active: order.status === s.key }">
          <view class="dot"></view>
          <text class="label">{{ s.label }}</text>
        </view>
      </view>
    </view>

    <!-- 下单类型信息 -->
    <view class="card">
      <view v-if="order.orderType === 'EAT_IN'" class="row">
        <text class="icon">🍽️</text>
        <view class="flex1">
          <view class="big">堂食 · 桌号 {{ order.tableNo || '--' }}</view>
          <view class="muted">{{ order.people }} 人就餐</view>
        </view>
      </view>
      <view v-else-if="order.orderType === 'TAKEAWAY'" class="row">
        <text class="icon">🥡</text>
        <view class="flex1">
          <view class="big">门店自取</view>
          <view class="muted">{{ order.contact?.name || '' }} · {{ order.contact?.phone || '' }}</view>
          <view class="muted" v-if="order.expectTime">取餐时间：{{ order.expectTime }}</view>
        </view>
      </view>
      <view v-else class="row">
        <text class="icon">🛵</text>
        <view class="flex1">
          <view class="big">{{ order.address }}</view>
          <view class="muted">{{ order.contact?.name || '' }} · {{ order.contact?.phone || '' }}</view>
          <view class="muted" v-if="order.expectTime">期望送达：{{ order.expectTime }}</view>
        </view>
      </view>
      <view v-if="order.storeName" class="store muted">📍 {{ order.storeName }}
        <text class="call" @tap="callStore">致电门店</text>
      </view>
    </view>

    <!-- 菜品清单 -->
    <view class="card">
      <view class="card-title">🍱 菜品明细</view>
      <view v-for="it in order.items || []" :key="it.dishId || it.id" class="dish">
        <image v-if="it.image" class="thumb" :src="it.image" mode="aspectFill" />
        <view class="meta">
          <text class="name">{{ it.name }}</text>
          <text class="remark muted" v-if="it.remark">{{ it.remark }}</text>
        </view>
        <view class="col">
          <text class="price">¥{{ (it.priceFen/100).toFixed(2) }}</text>
          <text class="qty">×{{ it.count }}</text>
        </view>
      </view>
      <view class="amounts">
        <view class="a"><text>菜品小计</text><text>¥{{ (order.totalFen/100).toFixed(2) }}</text></view>
        <view class="a" v-if="order.packFeeFen"><text>打包费</text><text>¥{{ (order.packFeeFen/100).toFixed(2) }}</text></view>
        <view class="a" v-if="order.deliveryFeeFen"><text>配送费</text><text>¥{{ (order.deliveryFeeFen/100).toFixed(2) }}</text></view>
        <view class="a" v-if="order.couponDiscountFen"><text>优惠券</text><text class="red">-¥{{ (order.couponDiscountFen/100).toFixed(2) }}</text></view>
        <view class="a total"><text>实付金额</text><text class="price big">¥{{ (order.payableFen/100).toFixed(2) }}</text></view>
      </view>
    </view>

    <!-- 订单信息 -->
    <view class="card">
      <view class="card-title">📄 订单信息</view>
      <view class="info"><text>订单编号</text><text class="copy" @tap="copy(order.orderNo || order.id)">{{ order.orderNo || order.id }} 复制</text></view>
      <view class="info"><text>下单时间</text><text>{{ order.createdAt }}</text></view>
      <view class="info" v-if="order.paidAt"><text>支付时间</text><text>{{ order.paidAt }}</text></view>
      <view class="info" v-if="order.finishedAt"><text>完成时间</text><text>{{ order.finishedAt }}</text></view>
      <view class="info" v-if="order.remark"><text>备注</text><text class="r">{{ order.remark }}</text></view>
    </view>

    <!-- 评价区 -->
    <view v-if="order.status === 4 && !order.commentId" class="card" @tap="goWriteComment">
      <view class="comment-entry">
        <view>
          <view class="big">📝 留下评价吧～</view>
          <view class="muted">您的建议帮助我们做得更好</view>
        </view>
        <text class="more">去评价 ›</text>
      </view>
    </view>

    <view class="gap"></view>
    <!-- 底部操作 -->
    <view class="footer">
      <button class="btn ghost" v-if="order.status === 0" @tap="doCancel">取消订单</button>
      <button class="btn ghost" v-else-if="order.status === 4" @tap="doAgain">再来一单</button>
      <button class="btn gold" v-if="order.status === 0" @tap="doPay">立即支付</button>
      <button class="btn gold" v-else-if="order.status >= 1 && order.status <= 3" @tap="callStore">联系门店</button>
      <button class="btn gold" v-else-if="order.status === 4 && order.commentId" @tap="goBackList">返回订单</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import orderApi from '@/api/order'
import payApi from '@/api/pay'
import { wxPay, toast } from '@/api/wx'
import { useAppStore } from '@/store/app'
import { useCartStore } from '@/store/cart'

const appStore = useAppStore()
const cart = useCartStore()

const id = ref(null)
const order = ref({ items: [] })
const timeline = [
  { key: 0, label: '下单' },
  { key: 1, label: '支付成功' },
  { key: 2, label: '制作中' },
  { key: 4, label: '已完成' }
]

onLoad(q => { id.value = q.id; load() })

async function load() {
  const r = await orderApi.fetchOrderDetail(id.value)
  order.value = r?.data || r?.order || r || {}
}
function statusText(s) {
  return ({ 0:'等待支付', 1:'等待上菜', 2:'正在制作', 3:'待取餐/配送', 4:'订单已完成', 5:'已取消', 6:'退款中' })[s] || '处理中'
}
function subText(s) {
  if (s === 0) return '请尽快完成支付，超时订单将自动取消'
  if (s === 1) return '厨房正在备菜，请耐心等候'
  if (s === 2) return '大厨正在精心烹制中，马上就好'
  if (s === 3) return '骑手/打包即将出发'
  if (s === 4) return '用餐愉快，期待您的宝贵评价'
  if (s === 5) return '订单已取消，有需要欢迎再次光临'
  return '我们正在处理'
}
function callStore() {
  uni.makePhoneCall({ phoneNumber: appStore.currentStore?.phone || (order.value.storePhone) || '4000000000' })
}
function copy(v) { uni.setClipboardData({ data: String(v) }) }
function goWriteComment(){ uni.navigateTo({ url: `/pages/comment/comment?orderId=${order.value.id}` }) }
function goBackList(){ uni.navigateBack() }
async function doCancel(){
  const r = await uni.showModal({ title:'确认取消？', content:'取消后优惠将被释放' })
  if (!r.confirm) return
  await orderApi.cancelOrder(order.value.id)
  toast('已取消')
  load()
}
async function doPay(){
  try {
    const res = await payApi.createPayment({
      orderType: 'ORDER', bizId: order.value.id, amountFen: order.value.payableFen || order.value.totalFen
    })
    await wxPay(res.data || res)
    toast('支付成功')
    setTimeout(load, 300)
  } catch(e){
    if (!String(e?.errMsg || '').includes('cancel')) toast('支付失败')
  }
}
async function doAgain() {
  const items = (order.value.items||[]).map(i => ({
    id: i.dishId||i.id, name: i.name, image: i.image, priceFen: i.priceFen, count: i.count
  }))
  cart.replaceItems(items, order.value.storeId)
  toast('已加入购物车')
  setTimeout(()=> uni.switchTab({ url: '/pages/menu/menu' }), 500)
}
</script>

<style lang="scss">
@import '@/uni.scss';
.order-detail-page{ background:$page-bg; min-height:100vh; padding-bottom:160rpx}
.status-card{background:$brand-gradient; color:#fff; padding:40rpx 32rpx 30rpx;
  .status-text{font-size:44rpx; font-weight:700}
  .sub{opacity:.9; font-size:26rpx; margin-top:10rpx}
  .progress{@include row-between; margin-top:40rpx;
    .node{@include column; align-items:center; flex:1; position:relative;
      .dot{width:18rpx; height:18rpx; border-radius:50%; background:rgba(255,255,255,.3); margin-bottom:10rpx}
      .label{font-size:22rpx; opacity:.75}
      &.done .dot{background:#fff; }
      &.active { .label{opacity:1; font-weight:600; font-size:24rpx} .dot{transform:scale(1.35)} }
    }
    .node + .node:before{
      content:''; position:absolute; left:-50%; top:10rpx; width:100%; height:4rpx; background:rgba(255,255,255,.25)
    }
  }
}
.card{@include card; margin:20rpx; }
.card-title{font-size:30rpx; font-weight:600; color:$ink-black; margin-bottom:20rpx}
.row{@include row-start;}
.flex1{flex:1; margin-left:16rpx}
.icon{font-size:36rpx}
.big{font-size:28rpx; color:$ink-black; font-weight:600; margin-bottom:6rpx}
.muted{color:$text-muted; font-size:24rpx}
.store{margin-top:20rpx; padding-top:16rpx; border-top:2rpx dashed #eee; @include row-between;
  .call{color:$brand-gold; text-decoration:underline}
}
.dish{@include row-start; padding:12rpx 0; border-top:2rpx dashed #eee; &:first-child{border-top:0}
  .thumb{width:88rpx; height:88rpx; border-radius:10rpx; background:#eee; margin-right:14rpx}
  .meta{flex:1; .name{font-size:28rpx; color:$ink-black; display:block} .remark{font-size:22rpx; margin-top:4rpx} }
  .col{@include column; align-items:flex-end; .price{font-size:26rpx} .qty{color:$text-muted; font-size:22rpx}}
}
.amounts{margin-top:20rpx; padding-top:16rpx; border-top:2rpx solid #eee;
  .a{@include row-between; padding:6rpx 0; color:$text-muted; font-size:26rpx;
    &.total{padding-top:12rpx; border-top:2rpx dashed #eee; margin-top:6rpx; color:$ink-black;
      .price.big{color:$brand-gold; font-size:34rpx; font-weight:700}
    }
    .red{color:#ff5e52}
  }
}
.info{@include row-between; padding:10rpx 0; color:$text-muted; font-size:26rpx;
  .copy{color:$brand-gold}
  .r{color:$ink-black; text-align:right; max-width:60%;}
}
.comment-entry{@include row-between; .big{font-size:28rpx; font-weight:600; color:$ink-black; margin-bottom:6rpx} .more{color:$brand-gold}}
.gap{height:40rpx}
.footer{@include footer-bar;
  .btn{flex:1; height:80rpx; line-height:80rpx; margin-right:16rpx; border-radius:40rpx; font-size:28rpx}
  .btn:last-child{margin-right:0}
  .btn.ghost{background:#f7f3eb; color:$brand-deep-gold}
  .btn.gold{@include gold-btn}
}
.red{color:#ff5e52}
</style>

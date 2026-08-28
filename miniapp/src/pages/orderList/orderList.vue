<template>
  <view class="order-list-page">
    <scroll-view scroll-x class="tabs" :show-scrollbar="false">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{active: active === t.key}"
        @tap="switchTab(t.key)">
        {{ t.label }}
        <text v-if="t.count" class="badge">{{ t.count }}</text>
      </view>
    </scroll-view>

    <scroll-view scroll-y class="list" @scrolltolower="loadMore">
      <view v-for="o in list" :key="o.id" class="card order-card" @tap="goDetail(o.id)">
        <view class="top">
          <view class="order-no">
            <text class="tag" v-if="o.orderType === 'EAT_IN'">堂食</text>
            <text class="tag takeaway" v-else-if="o.orderType === 'TAKEAWAY'">自取</text>
            <text class="tag delivery" v-else>外卖</text>
            <text class="no">订单号 {{ o.orderNo || o.id }}</text>
          </view>
          <text :class="'status s-'+o.status">{{ statusText(o.status) }}</text>
        </view>

        <view class="items">
          <view v-for="it in (o.items||[]).slice(0,3)" :key="it.dishId || it.id" class="dish">
            <image v-if="it.image" class="thumb" :src="it.image" mode="aspectFill" />
            <view class="meta">
              <text class="name">{{ it.name }}</text>
              <text class="qty">×{{ it.count }}</text>
            </view>
            <text class="price">¥{{ (it.priceFen * it.count / 100).toFixed(2) }}</text>
          </view>
          <view v-if="(o.items||[]).length > 3" class="more muted">共 {{ o.items.length }} 件菜品 ›</view>
        </view>

        <view class="bottom">
          <view class="date muted">{{ o.createdAt }}</view>
          <view class="sum">
            <text class="total">合计：</text>
            <text class="price">¥{{ ((o.payableFen || o.totalFen)/100).toFixed(2) }}</text>
          </view>
        </view>

        <view class="actions">
          <button class="btn ghost" size="mini" v-if="o.status===0" @tap.stop="doCancel(o)">取消订单</button>
          <button class="btn ghost" size="mini" v-if="o.status===4" @tap.stop="doAgain(o)">再来一单</button>
          <button class="btn ghost" size="mini" v-if="o.status===4 && !o.commentId" @tap.stop="goComment(o)">去评价</button>
          <button class="btn ghost" size="mini" v-if="o.status<=2" @tap.stop="doCall">联系门店</button>
          <button class="btn gold" size="mini" v-if="o.status===0" @tap.stop="doPay(o)">立即支付</button>
          <button class="btn gold" size="mini" v-else-if="o.status===2" @tap.stop="goDetail(o.id)">查看进度</button>
        </view>
      </view>

      <view v-if="!loading && !list.length" class="empty">
        <image class="icon" src="https://img.icons8.com/ios/200/C9A86C/shopping-list.png" mode="aspectFit" />
        <view class="text">{{ active==='all' ? '暂无订单，快去挑几道美味吧～' : '没有这个状态的订单哦' }}</view>
        <button class="gold-btn" @tap="goMenu">去点餐</button>
      </view>

      <view v-if="loading" class="loading muted">加载中…</view>
      <view v-if="!hasMore && list.length" class="no-more muted">—— 已经到底了 ——</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import orderApi from '@/api/order'
import payApi from '@/api/pay'
import { wxPay, toast } from '@/api/wx'
import { useAppStore } from '@/store/app'
import { useCartStore } from '@/store/cart'

const appStore  = useAppStore()
const cartStore = useCartStore()

const tabs = [
  { key: 'all',  label: '全部',     count: 0 },
  { key: 0,      label: '待支付',   count: 0 },
  { key: 1,      label: '待上菜',   count: 0 },
  { key: 2,      label: '制作中',   count: 0 },
  { key: 4,      label: '已完成',   count: 0 },
  { key: 5,      label: '已取消',   count: 0 }
]
const active = ref('all')
const list = ref([])
const page = ref(1)
const size = 10
const hasMore = ref(true)
const loading = ref(false)

function statusText(s) {
  return ({0:'待支付', 1:'待上菜', 2:'制作中', 3:'待取餐', 4:'已完成', 5:'已取消', 6:'退款中'})[s] || '处理中'
}
function switchTab(k) { active.value = k; page.value = 1; hasMore.value = true; list.value = []; load() }

async function load() {
  loading.value = true
  try {
    const params = { page: page.value, size }
    if (active.value !== 'all') params.status = active.value
    const storeId = appStore.currentStoreId
    if (storeId) params.storeId = storeId
    const r = await orderApi.fetchMyOrders(params)
    const rows = Array.isArray(r?.data?.list) ? r.data.list : (Array.isArray(r?.data) ? r.data : [])
    list.value = page.value === 1 ? rows : [...list.value, ...rows]
    hasMore.value = rows.length >= size
    // 小计数
    if (page.value === 1) {
      const all = Array.isArray(r?.data?.allRows) ? r.data.allRows : list.value
      tabs[1].count = all.filter(x => x.status === 0).length
      tabs[2].count = all.filter(x => x.status === 1).length
      tabs[3].count = all.filter(x => x.status === 2).length
    }
  } finally { loading.value = false }
}
function loadMore() { if (hasMore.value && !loading.value) { page.value++; load() } }

onShow(load)

function goDetail(id){ uni.navigateTo({ url: `/pages/orderDetail/orderDetail?id=${id}` }) }
function goMenu(){   uni.switchTab({ url: '/pages/menu/menu' }) }
function goComment(o){ uni.navigateTo({ url: `/pages/comment/comment?orderId=${o.id}` }) }

async function doCancel(o) {
  const r = await uni.showModal({ title: '确认取消订单？', content: '优惠名额会被释放哦' })
  if (!r.confirm) return
  await orderApi.cancelOrder(o.id)
  toast('已取消')
  load()
}

async function doPay(o) {
  try {
    const res = await payApi.createPayment({ orderType: 'ORDER', bizId: o.id, amountFen: (o.payableFen || o.totalFen) })
    await wxPay(res.data || res)
    toast('支付成功')
    load()
  } catch (e) {
    if (!String(e?.errMsg || '').includes('cancel')) toast('支付失败')
  }
}

async function doAgain(o) {
  // 把原订单菜品装回购物车，然后跳菜单
  const items = (o.items || []).map(i => ({
    id:    i.dishId || i.id,
    name:  i.name,
    image: i.image,
    priceFen: i.priceFen,
    count: i.count
  }))
  cartStore.replaceItems(items, o.storeId)
  uni.showToast({ title: '已加入购物车', icon: 'success' })
  setTimeout(() => uni.switchTab({ url: '/pages/menu/menu' }), 600)
}
function doCall(){
  uni.makePhoneCall({ phoneNumber: appStore.currentStore?.phone || '4000000000' })
}
</script>

<style lang="scss">
@import '@/uni.scss';
.order-list-page{ background:$page-bg; min-height:100vh; display:flex; flex-direction:column}
.tabs{white-space:nowrap; background:#fff; padding:16rpx 16rpx 0;
  .tab{display:inline-block; padding:14rpx 30rpx 18rpx; font-size:28rpx; color:$text-muted; position:relative;
    &.active{color:$ink-black; font-weight:600;
      &:after{content:''; position:absolute; left:50%; transform:translateX(-50%); bottom:2rpx; width:36rpx; height:6rpx; background:$brand-gradient; border-radius:3rpx}
    }
    .badge{background:#ff5e52; color:#fff; font-size:20rpx; border-radius:16rpx; padding:2rpx 10rpx; margin-left:6rpx}
  }
}
.list{flex:1; padding:20rpx; box-sizing:border-box}
.card.order-card{margin-bottom:20rpx;}
.order-card .top{@include row-between; padding-bottom:16rpx; border-bottom:2rpx dashed #eee}
.order-card .order-no{@include row-center;
  .tag{background:rgba(201,168,108,0.12); color:$brand-deep-gold; font-size:22rpx; padding:2rpx 14rpx; border-radius:8rpx; margin-right:12rpx;}
  .tag.takeaway{background:rgba(255,94,82,0.10); color:#ff5e52}
  .tag.delivery{background:rgba(59,130,246,0.12); color:#3b82f6}
  .no{font-size:24rpx; color:$text-muted}
}
.status{font-size:26rpx; font-weight:600;
  &.s-0{color:#ff5e52} &.s-4{color:$brand-gold} &.s-5{color:$text-muted}
  &.s-1,&.s-2,&.s-3{color:$brand-deep-gold}
}
.items{padding:16rpx 0;}
.dish{@include row-start; padding:10rpx 0;
  .thumb{width:80rpx; height:80rpx; border-radius:10rpx; background:#eee; margin-right:14rpx}
  .meta{flex:1; .name{font-size:26rpx; color:$ink-black; display:block} .qty{font-size:22rpx; color:$text-muted}}
  .price{font-size:26rpx; color:$ink-black}
}
.more{font-size:24rpx; padding:10rpx 0 0 94rpx}
.bottom{@include row-between; padding-top:14rpx; border-top:2rpx dashed #eee}
.sum{.total{color:$text-muted; font-size:24rpx} .price{color:$brand-gold; font-size:30rpx; font-weight:700}}
.actions{@include row-end; padding-top:16rpx;
  .btn{font-size:24rpx; padding:0 22rpx; line-height:52rpx; height:52rpx; border-radius:28rpx; margin-left:14rpx}
  .btn.ghost{background:#f7f3eb; color:$brand-deep-gold}
  .btn.gold{@include gold-btn-sm;}
}
.empty{@include column; align-items:center; padding:140rpx 0; color:$text-muted;
  .icon{width:200rpx; height:200rpx; opacity:.6}
  .text{font-size:28rpx; margin:30rpx 0 40rpx}
}
.loading,.no-more{text-align:center; padding:30rpx 0; font-size:24rpx}
</style>

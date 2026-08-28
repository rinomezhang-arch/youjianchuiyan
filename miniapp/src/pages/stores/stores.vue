<template>
  <view class="stores-page">
    <!-- 顶部搜索 -->
    <view class="search">
      <text class="ico">🔍</text>
      <input class="input" v-model="keyword" placeholder="搜索门店名、地址"/>
      <text v-if="keyword" class="clear" @tap="keyword=''">×</text>
    </view>

    <!-- 快捷 tabs -->
    <scroll-view scroll-x class="tabs" :show-scrollbar="false">
      <view
        v-for="t in tabs"
        :key="t.key"
        class="tab"
        :class="{active: tab === t.key}"
        @tap="tab=t.key">{{ t.label }}</view>
    </scroll-view>

    <!-- 列表 -->
    <scroll-view scroll-y class="list" @scrolltolower="loadMore">
      <view
        v-for="s in listFiltered"
        :key="s.id"
        class="store-card"
        :class="{active: s.id === appStore.currentStoreId}"
        @tap="pick(s)">
        <image class="cover"
               :src="s.image || s.imageUrl || placeholder"
               mode="aspectFill"/>
        <view class="info">
          <view class="row1">
            <text class="name">{{ s.name }}</text>
            <view v-if="s.id === appStore.currentStoreId" class="badge current">当前</view>
            <view v-else-if="s.status === 1" class="badge open">营业中</view>
            <view v-else class="badge close">休息</view>
          </view>
          <view class="tags">
            <text v-if="s.tags && s.tags.length" class="tag" v-for="t in s.tags.slice(0,3)" :key="t">{{ t }}</text>
            <text v-if="s.distance != null" class="tag dist">{{ s.distance }}</text>
          </view>
          <view class="addr muted">📍 {{ s.address || '地址见门店详情' }}</view>
          <view class="hours muted">🕐 {{ s.businessHours || '10:00 - 21:30' }}</view>
          <view class="actions">
            <view class="act" @tap.stop="doMap(s)">🧭 导航</view>
            <view class="act" @tap.stop="doCall(s)">📞 电话</view>
            <view v-if="selectMode" class="act go" @tap.stop="pick(s)">✓ 选择这家</view>
          </view>
        </view>
      </view>

      <view v-if="!loading && listFiltered.length === 0" class="empty">
        <image class="icon" src="https://img.icons8.com/ios/200/C9A86C/shop.png" mode="aspectFit" />
        <view class="text">没有匹配的门店，换个关键词试试～</view>
      </view>
      <view v-if="loading" class="loading muted">加载中…</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { fetchStores } from '@/api/store'
import { useAppStore } from '@/store/app'
import { toast } from '@/api/wx'
import { IMG_PLACEHOLDER } from '@/config/env'

const appStore = useAppStore()
const placeholder = IMG_PLACEHOLDER

const tabs = [
  { key:'all',   label:'全部' },
  { key:'near',  label:'🗺️ 附近' },
  { key:'open',  label:'🕙 营业中' },
  { key:'hasPkg',label:'🎎 有婚宴包厢' }
]
const tab = ref('all')
const keyword = ref('')
const selectMode = ref(false)

const list = ref([])
const loading = ref(false)

onLoad(q => { if (q.select === '1') selectMode.value = true })
onMounted(load)

async function load() {
  loading.value = true
  try {
    const r = await fetchStores()
    const arr = Array.isArray(r?.data) ? r.data : (Array.isArray(r?.records||r?.list) ? (r.records||r.list) : [])
    list.value = arr.map(s => ({
      ...s,
      status: s.status ?? 1,  // 默认为营业
      distance: s.distance != null ? (typeof s.distance === 'number' ? (s.distance < 1 ? Math.round(s.distance*1000)+'m' : s.distance.toFixed(1)+'km') : s.distance) : '定位中…'
    }))
  } catch(e) {
    // 兜底 mock
    list.value = [
      { id: 1, name:'又见炊烟 · 宁国总店', address:'宣城市宁国市宁城北路 128 号', businessHours:'10:00 - 21:30',
        status:1, tags:['徽菜名店','喜宴包厢','可停车'], distance:'0.8 km', image: placeholder },
      { id: 2, name:'又见炊烟 · 宣城万达店', address:'宣城市宣州区鳌峰中路 1 号', businessHours:'10:00 - 22:00',
        status:1, tags:['外卖配送','扫码点餐'], distance:'12.6 km', image: placeholder },
      { id: 3, name:'又见炊烟 · 绩溪店', address:'宣城市绩溪县华阳镇望徽路 8 号', businessHours:'09:30 - 21:00',
        status:0, tags:['徽菜源头店','农家土菜'], distance:'58 km', image: placeholder }
    ]
  } finally { loading.value = false }
}

const listFiltered = computed(() => {
  let l = [...list.value]
  if (keyword.value.trim()) {
    const k = keyword.value.trim()
    l = l.filter(s => (s.name||'').includes(k) || (s.address||'').includes(k) || (s.tags||[]).some(t => t.includes(k)))
  }
  if (tab.value === 'open')    l = l.filter(s => s.status === 1)
  if (tab.value === 'hasPkg')  l = l.filter(s => (s.tags||[]).some(t => /宴|包厢|包/.test(t)))
  if (tab.value === 'near')    l.sort((a,b) => parseFloat(a.distance||'9999') - parseFloat(b.distance||'9999'))
  return l
})

/* 操作 */
function pick(s){
  appStore.setCurrentStoreId(s.id)
  toast(`已切换：${s.name}`)
  setTimeout(() => uni.navigateBack(), 400)
}
function doCall(s){ uni.makePhoneCall({ phoneNumber:String(s.phone||s.tel||'4000000000').replace(/[^\d-]/g,'') }).catch(()=>{}) }
function doMap(s){
  if (s.latitude && s.longitude) {
    uni.openLocation({
      latitude: s.latitude, longitude: s.longitude, name: s.name, address: s.address || ''
    })
  } else {
    // 兜底：调 H5 地图 / 搜索关键词
    toast('暂无坐标，已复制地址')
    uni.setClipboardData({ data: s.address || s.name })
  }
}
function loadMore(){ /* 预留分页 */ }
</script>

<style lang="scss">
@import '@/uni.scss';
.stores-page{background:$page-bg; min-height:100vh; display:flex; flex-direction:column}
.search{@include row-center; margin:20rpx; padding:0 24rpx; height:76rpx; background:#fff; border-radius:40rpx; box-shadow:$shadow-card;
  .ico{font-size:28rpx; margin-right:12rpx}
  .input{flex:1; height:76rpx; font-size:28rpx}
  .clear{padding:0 10rpx; color:#bbb; font-size:36rpx}
}
.tabs{white-space:nowrap; padding:10rpx 20rpx 20rpx;
  .tab{display:inline-block; padding:14rpx 28rpx; background:#fff; border-radius:30rpx; font-size:26rpx; color:$text-muted; margin-right:16rpx;
    &.active{background:$brand-gradient; color:#fff; font-weight:600; box-shadow:$shadow-gold}
  }
}
.list{flex:1; padding:0 20rpx 20rpx;}
.store-card{display:flex; background:#fff; border-radius:20rpx; overflow:hidden; margin-bottom:20rpx; box-shadow:$shadow-card; border:4rpx solid transparent;
  &.active{border-color:$brand-gold}
  .cover{width:220rpx; height:220rpx; background:#eee; flex-shrink:0}
  .info{flex:1; padding:20rpx; min-width:0;}
  .row1{@include row-start; align-items:center; flex-wrap:wrap;
    .name{font-size:30rpx; font-weight:700; color:$ink-black; margin-right:14rpx}
    .badge{padding:4rpx 12rpx; border-radius:8rpx; font-size:20rpx; font-weight:600;
      &.open{background:#d8f3d9; color:#1f8f2c}
      &.close{background:#eee; color:#888}
      &.current{background:rgba(201,168,108,0.15); color:$brand-deep-gold; margin-right:6rpx}
    }
  }
  .tags{@include row-wrap; margin-top:8rpx;
    .tag{background:#faf3e2; color:$brand-deep-gold; padding:2rpx 12rpx; border-radius:8rpx; font-size:20rpx; margin-right:8rpx; margin-top:4rpx;
      &.dist{background:#f1f7ff; color:#2c6ad5}
    }
  }
  .addr{margin-top:10rpx; font-size:24rpx; word-break:break-all}
  .hours{margin-top:4rpx; font-size:22rpx}
  .actions{@include row-start; margin-top:16rpx; gap:14rpx;
    .act{padding:10rpx 18rpx; background:#faf3e2; color:$brand-deep-gold; border-radius:20rpx; font-size:22rpx;
      &.go{background:$brand-gold; color:#fff}
    }
  }
}
.empty{@include column; align-items:center; padding:140rpx 40rpx; color:$text-muted;
  .icon{width:180rpx; height:180rpx; opacity:.55}
  .text{font-size:28rpx; margin-top:30rpx; text-align:center}
}
.loading{text-align:center; padding:30rpx 0; font-size:24rpx}
</style>

<template>
  <view class="search-page">
    <view class="search-bar">
      <view class="search-box">
        <text class="ico">🔍</text>
        <input
          class="input"
          focus
          v-model="keyword"
          confirm-type="search"
          placeholder="搜索菜品、套餐、门店、标签…"
          @confirm="doSearch(keyword, 'input')"
          @input="onInput"
        />
        <text v-if="keyword" class="clear" @tap="onClear">×</text>
      </view>
      <text class="cancel" @tap="goBack">取消</text>
    </view>

    <!-- 搜索建议（实时） -->
    <view v-if="showSuggest && suggestions.length" class="section">
      <view class="sec-title">搜索建议</view>
      <view class="suggests">
        <view
          v-for="s in suggestions"
          :key="s.text"
          class="s-item"
          @tap="doSearch(s.text, 'suggest')">
          <text class="s-ico">🔎</text>
          <text class="s-text"><rich-text :nodes="s.highlight"></rich-text></text>
        </view>
      </view>
    </view>

    <!-- 结果视图 -->
    <scroll-view
      v-else-if="searched"
      scroll-y
      class="results"
      @scrolltolower="loadMore">
      <view class="sec-title">
        共找到 {{ resultCount }} 条结果
        <view class="filters">
          <text
            v-for="f in filters"
            :key="f.key"
            class="filter"
            :class="{active: sort===f.key}"
            @tap="sort=f.key">{{ f.label }}</text>
        </view>
      </view>

      <!-- 菜品 Tab -->
      <view v-if="sortedDishes.length" class="group">
        <view class="group-title">🍲 菜品（{{ dishes.length }}）</view>
        <view v-for="d in sortedDishes" :key="d.id" class="dish-card" @tap="goDish(d.id)">
          <image class="cover" :src="d.image || d.imageUrl || placeholder" mode="aspectFill"/>
          <view class="info">
            <view class="name">{{ d.name }}</view>
            <view class="desc muted">{{ d.desc || d.subtitle || '' }}</view>
            <view class="tags" v-if="d.tags?.length">
              <text class="tag" v-for="t in d.tags.slice(0,3)" :key="t">{{ t }}</text>
            </view>
            <view class="bottom">
              <text class="price">¥{{ (d.priceFen/100).toFixed(2) || d.price || '—' }}</text>
              <text v-if="d.sold" class="sold muted">月售 {{ d.sold }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 套餐 -->
      <view v-if="packages.length" class="group">
        <view class="group-title">🎎 套餐（{{ packages.length }}）</view>
        <view v-for="p in packages" :key="p.id" class="pkg-card" @tap="goPkg(p.id)">
          <image class="cover" :src="p.image || placeholder" mode="aspectFill"/>
          <view class="info">
            <view class="name">{{ p.name }}</view>
            <view class="desc muted">{{ p.guests }}位 · {{ p.dishesCount || 'N' }}道</view>
            <view class="bottom">
              <text class="price">¥{{ p.price }}</text>
              <text class="origin muted" v-if="p.originPrice">原价 ¥{{ p.originPrice }}</text>
            </view>
          </view>
        </view>
      </view>

      <!-- 门店 -->
      <view v-if="stores.length" class="group">
        <view class="group-title">🏘️ 门店（{{ stores.length }}）</view>
        <view v-for="s in stores" :key="s.id" class="store" @tap="goStore(s)">
          <text class="s-name">{{ s.name }}</text>
          <text class="s-addr muted">{{ s.address }}</text>
          <text class="s-dist">{{ s.distance }}</text>
        </view>
      </view>

      <view v-if="!sortedDishes.length && !packages.length && !stores.length" class="empty">
        <image class="icon" src="https://img.icons8.com/ios/200/C9A86C/search.png" mode="aspectFit" />
        <view class="text">未找到「{{ keyword }}」相关结果</view>
        <view class="hint muted">试试下面的热门关键词👇</view>
      </view>

      <view v-if="loading" class="loading muted">加载中…</view>
      <view style="height:60rpx"></view>
    </scroll-view>

    <!-- 默认视图：热门 + 历史 -->
    <view v-else class="defaults">
      <view class="section" v-if="history.length">
        <view class="sec-title">
          最近搜索
          <text class="clear-all" @tap="clearHistory">清空 🗑️</text>
        </view>
        <view class="chips">
          <text v-for="h in history" :key="h" class="chip" @tap="doSearch(h,'history')">{{ h }}</text>
        </view>
      </view>
      <view class="section">
        <view class="sec-title">🔥 热门搜索</view>
        <view class="chips">
          <text
            v-for="(h, i) in hots"
            :key="h"
            class="chip hot"
            :class="'rank-'+(i+1)"
            @tap="doSearch(h,'hot')">{{ h }}</text>
        </view>
      </view>
      <view class="section">
        <view class="sec-title">💡 猜你喜欢</view>
        <scroll-view scroll-x class="recs" :show-scrollbar="false">
          <view class="rec" v-for="d in recs" :key="d.id" @tap="goDish(d.id)">
            <image class="rec-img" :src="d.image || placeholder" mode="aspectFill"/>
            <view class="rec-name">{{ d.name }}</view>
            <view class="rec-price">¥{{ d.price }}</view>
          </view>
        </scroll-view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { IMG_PLACEHOLDER } from '@/config/env'
import dishApi from '@/api/dish'
import packageApi from '@/api/package'
import storeApi from '@/api/store'
import { useAppStore } from '@/store/app'

const appStore = useAppStore()
const placeholder = IMG_PLACEHOLDER
const HISTORY_KEY = 'yjcy_search_history'

const keyword = ref('')
const history = ref([])
const hots = [
  '臭鳜鱼','农家土鸡汤','婚宴套餐','包厢','外卖','徽菜','生日宴','毛豆腐','葛粉圆子','一品锅'
]
const recs = ref([])
const filters = [
  { key:'default', label:'综合' },
  { key:'priceUp', label:'价格↑' },
  { key:'priceDown', label:'价格↓' },
  { key:'sold', label:'销量' }
]
const sort = ref('default')

// 结果
const searched = ref(false)
const loading = ref(false)
const dishes = ref([])
const packages = ref([])
const stores = ref([])
const resultCount = computed(() => dishes.value.length + packages.value.length + stores.value.length)
const sortedDishes = computed(() => {
  const arr = [...dishes.value]
  if (sort.value === 'priceUp')   arr.sort((a,b) => (a.priceFen||parseFloat(a.price)*100||0) - (b.priceFen||parseFloat(b.price)*100||0))
  if (sort.value === 'priceDown') arr.sort((a,b) => (b.priceFen||parseFloat(b.price)*100||0) - (a.priceFen||parseFloat(a.price)*100||0))
  if (sort.value === 'sold')      arr.sort((a,b) => (b.sold||0) - (a.sold||0))
  return arr
})

// 建议
const suggestions = computed(() => {
  if (!keyword.value.trim()) return []
  const kw = keyword.value.trim()
  const pool = [
    ...hots.map(h => ({ type:'hot', text:h })),
    ...history.value.map(h => ({ type:'hist', text:h })),
    ...dishes.value.map(d => ({ type:'dish', text: d.name })),
  ]
  const seen = new Set()
  return pool
    .filter(p => p.text.toLowerCase().includes(kw.toLowerCase()))
    .filter(p => seen.has(p.text) ? false : (seen.add(p.text), true))
    .slice(0, 8)
    .map(p => {
      // 高亮：替换第一个匹配为金色粗体
      const i = p.text.toLowerCase().indexOf(kw.toLowerCase())
      const hl = i >= 0
        ? p.text.slice(0,i) + `<span style="color:#C9A86C;font-weight:600">${p.text.slice(i,i+kw.length)}</span>` + p.text.slice(i+kw.length)
        : p.text
      return { ...p, highlight: hl }
    })
})
const showSuggest = computed(() => keyword.value.trim().length > 0 && !searched.value)

/* 初始化 */
onMounted(() => {
  history.value = uni.getStorageSync(HISTORY_KEY) || []
  loadRecs()
})
async function loadRecs(){
  const r = await dishApi.fetchFeaturedDishes(appStore.currentStoreId, 8).catch(()=>[])
  recs.value = (Array.isArray(r)?r:(r?.records||r?.list||[])).slice(0,8)
  if (!recs.value.length) recs.value = [
    { id: 1, name: '徽州臭鳜鱼', price:'128.00', image: placeholder },
    { id: 2, name: '农家土鸡汤', price:'88.00',  image: placeholder },
    { id: 3, name: '毛豆腐锅仔', price:'58.00',  image: placeholder },
    { id: 4, name: '黄山一品锅', price:'168.00', image: placeholder }
  ]
}

/* 交互 */
function onInput(){ searched.value = false }
function onClear(){ keyword.value = ''; searched.value = false }
function goBack(){ uni.navigateBack() }

function pushHistory(k){
  const key = k.trim()
  if (!key) return
  const next = [key, ...history.value.filter(h => h !== key)].slice(0, 10)
  history.value = next
  uni.setStorageSync(HISTORY_KEY, next)
}
function clearHistory(){
  history.value = []
  uni.removeStorageSync(HISTORY_KEY)
}

async function doSearch(k, src){
  const kw = (k||'').trim()
  if (!kw) return
  keyword.value = kw
  pushHistory(kw)
  searched.value = true
  loading.value = true
  try {
    const storeId = appStore.currentStoreId
    const [rd, rp, rs] = await Promise.all([
      dishApi.searchDishes({ keyword: kw, storeId }).catch(() => ({ data: [] })),
      packageApi.fetchPackages({ keyword: kw, storeId }).catch(() => ({ data: [] })),
      storeApi.fetchStores({ keyword: kw }).catch(() => ({ data: [] }))
    ])
    dishes.value    = Array.isArray(rd?.data) ? rd.data : (Array.isArray(rd) ? rd : [])
    packages.value  = Array.isArray(rp?.data) ? rp.data : (Array.isArray(rp) ? rp : [])
    stores.value    = Array.isArray(rs?.data) ? rs.data : (Array.isArray(rs) ? rs : [])
  } finally { loading.value = false }
}
function loadMore(){}

/* 跳转 */
function goDish(id){ uni.navigateTo({ url: `/pages/dishDetail/dishDetail?id=${id}` }) }
function goPkg(id){  uni.navigateTo({ url: `/pages/packageDetail/packageDetail?id=${id}` }) }
function goStore(s){ appStore.setCurrentStoreId(s.id); uni.navigateTo({ url: '/pages/stores/stores?select=1' }) }
</script>

<style lang="scss">
@import '@/uni.scss';
.search-page{background:$page-bg; min-height:100vh; display:flex; flex-direction:column}
.search-bar{@include row-center; background:#fff; padding:20rpx 24rpx; gap:16rpx;
  box-shadow: 0 6rpx 20rpx rgba(0,0,0,.05);
  .search-box{@include row-center; flex:1; height:72rpx; background:#f7f3ea; border-radius:36rpx; padding:0 22rpx;
    .ico{margin-right:12rpx; color:$brand-gold; font-size:28rpx}
    .input{flex:1; height:72rpx; font-size:28rpx}
    .clear{color:#bbb; font-size:34rpx; padding:0 10rpx}
  }
  .cancel{color:$brand-deep-gold; font-size:28rpx; font-weight:600}
}
.section{padding:24rpx 32rpx 8rpx}
.sec-title{@include row-between; font-size:28rpx; font-weight:700; color:$ink-black; margin-bottom:20rpx;
  .clear-all{font-size:24rpx; color:$text-muted; font-weight:400}
}
.chips{@include row-wrap; gap:16rpx;
  .chip{@include chip; }
  .chip.hot.rank-1{background:linear-gradient(135deg,#ff5a4d,#ff8a3c); color:#fff; border-color:transparent}
  .chip.hot.rank-2{background:linear-gradient(135deg,#ff8a3c,#ffbb3c); color:#fff; border-color:transparent}
  .chip.hot.rank-3{background:linear-gradient(135deg,#ffbb3c,#e6cf6a); color:#fff; border-color:transparent}
}
.suggests{background:#fff; border-radius:14rpx; overflow:hidden; box-shadow:$shadow-card;
  .s-item{@include row-center; padding:22rpx 24rpx; border-top:2rpx solid #f6f1e1; &:first-child{border-top:0}
    .s-ico{font-size:24rpx; color:#bbb; margin-right:14rpx}
    .s-text{font-size:28rpx; color:$ink-black}
  }
}

.results{flex:1; padding:0 20rpx}
.filters{@include row-wrap; gap:10rpx;
  .filter{padding:6rpx 18rpx; border-radius:20rpx; font-size:22rpx; color:$text-muted; background:#f5efe0; font-weight:400;
    &.active{background:$brand-gold; color:#fff}
  }
}
.group{margin:24rpx 0;
  .group-title{font-size:28rpx; font-weight:700; color:$ink-black; margin-bottom:16rpx}
}
.dish-card,.pkg-card{@include row-start; background:#fff; border-radius:18rpx; padding:16rpx; margin-bottom:14rpx; box-shadow:$shadow-card;
  .cover{width:180rpx; height:180rpx; border-radius:14rpx; margin-right:16rpx; background:#eee; flex-shrink:0}
  .info{flex:1; min-width:0; display:flex; flex-direction:column;
    .name{font-size:28rpx; font-weight:600; color:$ink-black; overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
    .desc{margin-top:6rpx; font-size:22rpx; overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
    .tags{@include row-wrap; margin-top:8rpx;
      .tag{background:#fff1cc; color:#986c1b; font-size:20rpx; padding:2rpx 10rpx; border-radius:6rpx; margin-right:6rpx}
    }
    .bottom{@include row-center; margin-top:auto;
      .price{color:$brand-gold; font-size:30rpx; font-weight:700; margin-right:12rpx}
      .origin{text-decoration:line-through}
      .sold{margin-left:12rpx}
    }
  }
}
.store{background:#fff; border-radius:14rpx; padding:20rpx 24rpx; margin-bottom:12rpx; @include row-between; box-shadow:$shadow-card;
  .s-name{font-size:28rpx; font-weight:600; color:$ink-black; max-width:320rpx; overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
  .s-addr{flex:1; padding:0 20rpx; font-size:22rpx; overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
  .s-dist{color:#2c6ad5; font-size:22rpx}
}
.recs{white-space:nowrap; padding-bottom:16rpx;
  .rec{display:inline-block; width:220rpx; margin-right:20rpx; vertical-align:top;
    .rec-img{width:220rpx; height:180rpx; border-radius:14rpx; background:#eee}
    .rec-name{font-size:26rpx; color:$ink-black; margin-top:10rpx; padding:0 4rpx;
      overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
    .rec-price{color:$brand-gold; font-weight:700; font-size:26rpx; padding:4rpx}
  }
}
.empty{@include column; align-items:center; padding:100rpx 0; color:$text-muted;
  .icon{width:160rpx; height:160rpx; opacity:.55}
  .text{font-size:28rpx; margin:20rpx 0 10rpx}
  .hint{font-size:24rpx}
}
.loading{text-align:center; padding:30rpx 0; font-size:24rpx}
</style>

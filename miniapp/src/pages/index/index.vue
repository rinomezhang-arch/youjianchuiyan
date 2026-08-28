<template>
  <view class="home">
    <!-- 顶部：门店切换 + 搜索 -->
    <view class="topbar">
      <view class="store-switch" @tap="onPickStore">
        <text class="store-name">{{ currentStore?.name || '选择门店' }}</text>
        <text class="store-sub">{{ currentStore?.address ? currentStore.address.slice(0,12) + '…' : '又见炊烟' }}</text>
        <text class="arrow">▾</text>
      </view>
      <view class="search" @tap="onSearch">
        <text class="icon">🔍</text>
        <text class="ph">搜索招牌菜、农家小炒…</text>
      </view>
    </view>

    <!-- 轮播：活动/包厢图 -->
    <swiper class="banner" circular autoplay :interval="4000" indicator-dots indicator-color="rgba(201,168,108,0.4)" indicator-active-color="#C9A86C">
      <swiper-item v-for="(b, i) in banners" :key="i" @tap="onBannerTap(b)">
        <image class="banner-img" :src="b.image" mode="aspectFill" />
        <view class="banner-caption" v-if="b.title"><text>{{ b.title }}</text></view>
      </swiper-item>
    </swiper>

    <!-- 门店公告 -->
    <view class="notice-bar" v-if="notice" @tap="onNoticeTap(notice)">
      <text class="n-ico">📢</text>
      <text class="n-text">{{ notice.title || notice.content }}</text>
      <text class="n-more">›</text>
    </view>

    <!-- 活动弹窗（后端 /popup 下发后首次显示） -->
    <view v-if="popup && showPopup" class="popup-mask" @tap.self="showPopup = false">
      <view class="popup-card" v-if="popup.image">
        <image class="p-img" :src="popup.image" mode="widthFix" @tap="onPopupTap"/>
        <view class="p-close" @tap="showPopup = false">×</view>
      </view>
    </view>

    <!-- 功能入口：预订 / 宴会套餐 / 我的预订 / 扫码点餐 -->
    <view class="entries">
      <view class="entry" @tap="go('/pages/book/book')">
        <view class="entry-icon" style="background:linear-gradient(135deg,#FFF3D6,#FFD9A2)">📅</view>
        <text class="entry-text">在线预订</text>
      </view>
      <view class="entry" @tap="go('/pages/packages/packages')">
        <view class="entry-icon" style="background:linear-gradient(135deg,#FFE0E0,#FFB4B4)">🎎</view>
        <text class="entry-text">宴会套餐</text>
      </view>
      <view class="entry" @tap="go('/pages/bookingList/bookingList')">
        <view class="entry-icon" style="background:linear-gradient(135deg,#DCEEFF,#9EC9FF)">📋</view>
        <text class="entry-text">我的预订</text>
      </view>
      <view class="entry" @tap="go('/pages/menu/menu')">
        <view class="entry-icon" style="background:linear-gradient(135deg,#E2F6D7,#B0E497)">🍲</view>
        <text class="entry-text">扫码点餐</text>
      </view>
    </view>

    <view class="gold-line"></view>

    <!-- 招牌菜 -->
    <view class="section">
      <view class="sec-head">
        <view class="sec-title">
          <text class="sec-bar"></text>
          <text class="sec-text">招牌推荐</text>
        </view>
        <text class="sec-more" @tap="go('/pages/menu/menu')">查看全部 ›</text>
      </view>

      <view class="feature-grid" v-if="featured.length">
        <view
          class="f-card"
          v-for="d in featured"
          :key="d.id"
          @tap="goDish(d.id)"
        >
          <image class="f-img" :src="d.image || d.imageUrl || placeholder" mode="aspectFill" />
          <view class="f-info">
            <text class="f-name ellipsis-1">{{ d.name }}</text>
            <text class="f-desc ellipsis-1">{{ d.desc || d.subtitle || '又见炊烟 匠心烹制' }}</text>
            <view class="f-bottom">
              <text class="f-price">¥{{ d.price || '0.00' }}</text>
              <text class="f-btn">立即预订</text>
            </view>
          </view>
        </view>
      </view>
      <view v-else class="empty"><text>暂无招牌菜，敬请期待</text></view>
    </view>

    <view class="gold-line"></view>

    <!-- 门店介绍 -->
    <view class="section" v-if="currentStore">
      <view class="sec-head">
        <view class="sec-title"><text class="sec-bar"></text><text class="sec-text">{{ currentStore.name }}</text></view>
      </view>
      <view class="store-card">
        <view class="store-row"><text class="s-label">地址</text><text class="s-value">{{ currentStore.address || '—' }}</text></view>
        <view class="store-row"><text class="s-label">电话</text>
          <text class="s-value link" v-if="currentStore.phone" @tap.stop="onCall(currentStore.phone)">{{ currentStore.phone }}</text>
          <text class="s-value" v-else>—</text>
        </view>
        <view class="store-row"><text class="s-label">营业</text><text class="s-value">{{ currentStore.businessHours || '10:00 - 21:30' }}</text></view>
        <view class="store-row"><text class="s-label">特色</text><text class="s-value">徽菜 / 农家土菜 / 喜宴包厢</text></view>
        <view class="store-actions">
          <view class="btn-outline" @tap="onCall(currentStore.phone)">📞 拨打</view>
          <view class="btn-gold" @tap="go('/pages/book/book')">马上预订</view>
        </view>
      </view>
    </view>

    <view style="height: 140rpx"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { fetchStores, fetchStoreDetail } from '@/api/store'
import { fetchFeaturedDishes } from '@/api/dish'
import activityApi from '@/api/activity'
import couponApi from '@/api/coupon'
import { IMG_PLACEHOLDER } from '@/config/env'

const appStore = useAppStore()
const currentStore = computed(() => appStore.currentStore)
const placeholder = IMG_PLACEHOLDER

/* 轮播/Banner：先给默认兜底，后端接口通了自动替换 */
const defaultBanners = [
  { image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20restaurant%20dining%20hall%20warm%20gold%20huizhou%20style&image_size=landscape_16_9',
    title: '又见炊烟 · 宁国店 重装开业', link: '/pages/packages/packages', type: 'PAGE' },
  { image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=anhui%20huizhou%20dish%20braised%20pork%20potato%20top%20view&image_size=landscape_16_9',
    title: '招牌徽州臭鳜鱼 堂食立减38元', link: '/pages/coupon/coupon', type: 'PAGE' },
  { image: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20wedding%20banquet%20table%20decoration%20red%20gold&image_size=landscape_16_9',
    title: '预订婚宴套餐 送婚房布置 + 司仪', link: '/pages/packageDetail/packageDetail?id=1', type: 'PAGE' }
]
const banners = ref([...defaultBanners])

/* 门店公告 + 活动弹窗 */
const notice = ref(null)
const popup = ref(null)
const showPopup = ref(false)

/* 异步拉 Banner / 公告 / 弹窗，失败静默兜底 */
async function loadMarketing() {
  const storeId = appStore.currentStoreId
  try {
    const r = await activityApi.fetchBanners(storeId)
    const list = Array.isArray(r?.data) ? r.data : []
    if (list.length) banners.value = list.map(b => ({ ...b, image: b.image || b.imgUrl, title: b.title || b.name }))
  } catch(e){ /* 兜底默认 */ }

  try {
    const r = await activityApi.fetchNotice(storeId)
    if (r?.data?.id || r?.data?.title || r?.data?.content) notice.value = r.data
    else if (typeof r === 'string' && r) notice.value = { title: r }
  } catch(e){}

  try {
    const r = await activityApi.fetchPopup(storeId)
    const p = r?.data
    if (p?.id && p?.image) {
      // 每个 popup.id 本次会话只显示一次
      const key = `yjcy_popup_shown_${p.id}`
      if (!uni.getStorageSync(key)) {
        popup.value = p
        showPopup.value = true
        uni.setStorageSync(key, 1)
      }
    }
  } catch(e){}
}

const featured = ref([])

async function loadFeatured() {
  try {
    const list = await fetchFeaturedDishes(appStore.currentStoreId, 6)
    featured.value = Array.isArray(list) ? list : (list?.records || list?.list || [])
    if (!featured.value.length) featured.value = fallbackFeatured()
  } catch (e) {
    featured.value = fallbackFeatured()
  }
}

function fallbackFeatured() {
  const ph = placeholder
  return [
    { id: 1001, name: '徽州臭鳜鱼', price: '128.00', desc: '经典徽菜 闻着臭吃着香', image: ph },
    { id: 1002, name: '农家土鸡汤',   price: '88.00',  desc: '三黄鸡慢炖4小时',     image: ph },
    { id: 1003, name: '毛豆腐锅仔',   price: '58.00',  desc: '外酥里嫩 秘制蘸酱',     image: ph },
    { id: 1004, name: '腊肉笋衣煲',   price: '68.00',  desc: '黄山笋衣 农家腊肉',     image: ph },
    { id: 1005, name: '黄山一品锅',   price: '168.00', desc: '六样叠层 团圆好味道',   image: ph },
    { id: 1006, name: '葛粉圆子',     price: '38.00',  desc: 'Q弹软糯 山珍内馅',     image: ph },
  ]
}

function go(url) { uni.navigateTo({ url }).catch(() => uni.switchTab({ url })) }
function goDish(id) { uni.navigateTo({ url: `/pages/dishDetail/dishDetail?id=${id}` }) }
function onSearch() {
  uni.navigateTo({ url: '/pages/search/search' })
}
function onPickStore() {
  uni.navigateTo({
    url: '/pages/stores/stores?select=1',
    fail: () => {
      const list = appStore.stores || []
      if (!list.length) { uni.showToast({ title: '暂无门店', icon: 'none' }); return }
      uni.showActionSheet({
        itemList: list.map(s => `${s.name} · ${s.address || '又见炊烟'}`),
        success: (r) => {
          const store = list[r.tapIndex]
          if (store) { appStore.setCurrentStoreId(store.id); loadFeatured(); loadMarketing() }
        }
      })
    }
  })
}
function onCall(phone) {
  if (!phone) return
  uni.makePhoneCall({ phoneNumber: String(phone).replace(/[^\d-]/g,'') }).catch(() => {})
}

/* Banner / 公告 / 弹窗 的点击跳转 */
function smartGo(target) {
  if (!target) return
  if (/^https?:\/\//.test(target)) return uni.navigateTo({ url: `/pages/webview/webview?url=${encodeURIComponent(target)}` }).catch(() => {} )
  go(target)
}
function onBannerTap(b) {
  // 后端约定 b.target 或 b.link / b.url 三种都兼容
  smartGo(b.target || b.link || b.url)
}
function onNoticeTap(n) { smartGo(n.link || n.url || n.target) }
async function onPopupTap() {
  const p = popup.value
  showPopup.value = false
  if (!p) return
  // 两种常见类型：发券/页面跳转
  if (p.type === 'COUPON' && p.couponId) {
    try { await couponApi.receiveCoupon(p.couponId); uni.showToast({ title:'领取成功', icon:'success' }) }
    catch(e){ uni.showToast({ title: e?.message || '已领过或已抢完', icon:'none' }) }
  } else if (p.type === 'RECEIVE_COUPONS' || p.key === 'receive_coupons') {
    uni.navigateTo({ url: '/pages/coupon/coupon' })
  } else {
    smartGo(p.target || p.link || p.url)
  }
}

// 启动 + 每次回到前台都拉营销
onMounted(() => { loadFeatured(); loadMarketing() })
onShow(loadMarketing)
watch(() => appStore.currentStoreId, () => { loadFeatured(); loadMarketing() })
onPullDownRefresh(async () => {
  await Promise.all([loadFeatured(), loadMarketing()])
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.home { padding: $spacing-md; background: $brand-bg; min-height: 100vh; box-sizing: border-box; }

/* 顶部 */
.topbar { display: flex; align-items: center; gap: $spacing-md; }
.store-switch {
  flex: 0 0 auto; padding: 10rpx 20rpx; background:#fff; border-radius: $radius-md;
  box-shadow: $shadow-card; max-width: 300rpx;
}
.store-name { font-size: $font-md; font-weight: 600; color: $brand-ink; display: block; }
.store-sub  { font-size: $font-xs; color: $text-3; display: block; }
.arrow      { color: $brand-gold; margin-left: 6rpx; }
.search {
  flex: 1; background: #fff; height: 76rpx; border-radius: 999rpx; padding: 0 24rpx;
  display: flex; align-items: center; gap: 12rpx; box-shadow: $shadow-card;
  color: $text-3; font-size: $font-sm;
}
.search .icon { font-size: $font-md; }

/* 轮播 */
.banner { height: 320rpx; margin-top: $spacing-md; border-radius: $radius-lg; overflow: hidden; }
.banner-img { width: 100%; height: 320rpx; }
.banner-caption {
  position: absolute; left: 0; right: 0; bottom: 0; padding: 16rpx 24rpx;
  background: linear-gradient(transparent, rgba(0,0,0,0.55)); color:#fff; font-size: $font-md;
}

/* 功能入口 */
.entries {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: $spacing-sm;
  margin-top: $spacing-lg; padding: $spacing-md; background:#fff; border-radius: $radius-lg; box-shadow: $shadow-card;
}
.entry { display:flex; flex-direction:column; align-items:center; gap: 10rpx; }
.entry-icon { width: 88rpx; height: 88rpx; border-radius: 50%; display:flex; align-items:center; justify-content:center; font-size: 40rpx; }
.entry-text { font-size: $font-xs; color: $text-2; }

/* 版块标题 */
.section { margin-top: $spacing-lg; }
.sec-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: $spacing-md; }
.sec-title { display:flex; align-items:center; gap: 12rpx; }
.sec-bar { width: 8rpx; height: 32rpx; background: linear-gradient(180deg, #E6D4A8, #C9A86C); border-radius: 4rpx; }
.sec-text { font-size: $font-lg; font-weight: 600; color: $brand-ink; }
.sec-more { font-size: $font-sm; color: $brand-gold-dark; }

/* 招牌菜 网格 */
.feature-grid { display: grid; grid-template-columns: 1fr 1fr; gap: $spacing-md; }
.f-card { background:#fff; border-radius: $radius-lg; overflow: hidden; box-shadow: $shadow-card; }
.f-img  { width: 100%; height: 220rpx; background: #eee; }
.f-info { padding: $spacing-sm; }
.f-name { font-size: $font-md; color: $text-1; font-weight: 600; }
.f-desc { font-size: $font-xs; color: $text-3; margin: 6rpx 0 10rpx; display:block; }
.f-bottom { display:flex; align-items:center; justify-content: space-between; }
.f-price { color: $brand-gold-dark; font-weight: 700; font-size: $font-md; }
.f-btn { background: linear-gradient(135deg, #E6D4A8, #C9A86C); color:#fff; font-size: $font-xs; padding: 6rpx 16rpx; border-radius: 999rpx; }
.empty { text-align: center; color: $text-3; padding: $spacing-xl 0; }

/* 门店卡片 */
.store-card { background:#fff; border-radius: $radius-lg; padding: $spacing-md; box-shadow: $shadow-card; }
.store-row { display: flex; padding: 10rpx 0; border-bottom: 1rpx solid #F4F0E6; }
.store-row:last-of-type { border-bottom: 0; }
.s-label { width: 100rpx; color: $text-3; font-size: $font-sm; flex-shrink: 0; }
.s-value { flex: 1; color: $text-1; font-size: $font-sm; }
.s-value.link { color: $brand-gold-dark; }
.store-actions { display:flex; gap: $spacing-sm; margin-top: $spacing-md; }
.btn-outline {
  flex: 1; text-align: center; padding: 18rpx 0; border: 1rpx solid $brand-gold; color: $brand-gold-dark;
  border-radius: $radius-pill; font-size: $font-sm;
}
.btn-gold {
  flex: 1.5; text-align: center; padding: 18rpx 0;
  background: linear-gradient(135deg, #E6D4A8, #C9A86C); color:#fff;
  border-radius: $radius-pill; font-size: $font-sm; box-shadow: $shadow-gold;
}

/* 金色分隔线 */
.gold-line{height:2rpx; background:linear-gradient(90deg, transparent, #C9A86C 20%, #E6D4A8 50%, #C9A86C 80%, transparent); margin:40rpx 40rpx 0; opacity:.5}

/* 门店公告条 */
.notice-bar{margin-top:20rpx; padding:16rpx 24rpx; background:#fffaf0; border:2rpx dashed #e6cf9e; border-radius:14rpx;
  display:flex; align-items:center;
  .n-ico{margin-right:12rpx; font-size:30rpx}
  .n-text{flex:1; color:#7a5a20; font-size:26rpx; line-height:1.5;
    overflow:hidden; text-overflow:ellipsis; white-space:nowrap}
  .n-more{color:#c2a060; font-size:32rpx; padding:0 8rpx}
}

/* 活动弹窗 */
.popup-mask{position:fixed; inset:0; background:rgba(0,0,0,.6); z-index:999; @include column; align-items:center; justify-content:center; padding:0 60rpx}
.popup-card{position:relative; width:100%; max-width:640rpx; border-radius:24rpx; overflow:hidden; background:transparent;
  .p-img{width:100%; display:block; border-radius:24rpx; box-shadow:0 30rpx 80rpx rgba(0,0,0,.3)}
  .p-close{position:absolute; left:50%; transform:translateX(-50%); bottom:-80rpx; width:60rpx; height:60rpx; line-height:56rpx; text-align:center; border-radius:50%; background:rgba(255,255,255,.18); color:#fff; border:2rpx solid #fff8; font-size:40rpx}
}
</style>

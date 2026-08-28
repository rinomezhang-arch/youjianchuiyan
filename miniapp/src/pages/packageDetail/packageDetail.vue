<template>
  <view class="pd-page" v-if="pkg">
    <image class="banner" :src="pkg.coverImage || pkg.image || placeholder" mode="aspectFill" />

    <view class="info-card">
      <view v-if="pkg.tag" class="tag">{{ pkg.tag }}</view>
      <text class="pkg-name">{{ pkg.name }}</text>
      <text class="pkg-serve">{{ pkg.serves || '10人/桌适用' }}</text>

      <view class="price-row">
        <text class="unit">¥</text>
        <text class="price">{{ pkg.price }}</text>
        <text v-if="pkg.oldPrice" class="old">¥{{ pkg.oldPrice }}</text>
        <text class="discount" v-if="pkg.oldPrice">省¥{{ Number(pkg.oldPrice) - Number(pkg.price) }}</text>
      </view>

      <view class="tips" v-if="pkg.bonus">
        <text class="t-item" v-for="(b, i) in pkg.bonus" :key="i">🎁 {{ b }}</text>
      </view>
    </view>

    <view class="card" v-if="menuText || (pkg.dishes && pkg.dishes.length)">
      <view class="c-title"><text class="bar"></text><text>套餐菜单</text></view>
      <view class="menu-tbl">
        <view class="tbl-sec" v-for="sec in menuSections" :key="sec.sec">
          <view class="tbl-sec-head">{{ sec.sec }}</view>
          <view class="tbl-row" v-for="d in sec.items" :key="d.name">
            <text class="d-name">{{ d.name }}</text>
            <text class="d-note" v-if="d.unit">{{ d.unit }}</text>
          </view>
        </view>
      </view>
    </view>

    <view class="card">
      <view class="c-title"><text class="bar"></text><text>预订须知</text></view>
      <view class="notice">
        <text>· 预订成功后，婚宴/商务宴需 3 个工作日内到店付定金 30%</text>
        <text>· 菜单支持 3 道菜同价位调换，详细联系门店</text>
        <text>· 包场、投影仪、舞台音响等增值服务可致电咨询</text>
        <text>· 如取消预订，定金扣除已制作物料后原路退回</text>
      </view>
    </view>

    <view style="height: 180rpx"></view>

    <view class="bottom-bar">
      <view class="b-left" @tap="onCall">
        <text class="bl-icon">📞</text>
        <text class="bl-text">门店咨询</text>
      </view>
      <view class="b-btn" @tap="bookNow">立即预订此套餐</view>
    </view>
  </view>

  <view v-else class="loading">加载中…</view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useAppStore } from '@/store/app'
import { fetchPackageDetail } from '@/api/package'
import { IMG_PLACEHOLDER } from '@/config/env'

const appStore = useAppStore()
const placeholder = IMG_PLACEHOLDER
const pkg = ref(null)

/* 把 flat dishes 分组（冷盘/头菜/主菜/汤/主食/点心/果盘） */
const menuText = ref('')
const SEC_ORDER = ['冷盘','前菜','头菜','主菜','汤羹','蒸炖','炒烧','主食','点心','甜品','饮品','果盘']
const menuSections = computed(() => {
  const list = Array.isArray(pkg.value?.dishes) ? pkg.value.dishes
    : ((menuText.value || pkg.value?.menu || pkg.value?.dishList || '')
        .toString().split(/[\n;；]/).map(s => s.trim()).filter(Boolean))

  // 如果是对象数组 [{name,section,unit}]，直接归section
  const secMap = {}
  const push = (sec, d) => {
    const s = sec || '其他'
    if (!secMap[s]) secMap[s] = []
    secMap[s].push(d)
  }

  list.forEach(d => {
    if (typeof d === 'string') {
      // 解析 "冷盘：八福喜临门" 或 "八福喜临门"
      const m = d.match(/^([\u4e00-\u9fa5A-Za-z]{2,4})[：:]\s*(.+)$/)
      if (m) push(m[1], { name: m[2] })
      else    push('其他',   { name: d })
    } else {
      push(d.section || d.category || '其他', d)
    }
  })

  // 排序
  const keys = Object.keys(secMap).sort((a, b) => {
    const ai = SEC_ORDER.indexOf(a), bi = SEC_ORDER.indexOf(b)
    if (ai === -1 && bi === -1) return a.localeCompare(b)
    if (ai === -1) return 1
    if (bi === -1) return -1
    return ai - bi
  })
  return keys.map(sec => ({ sec, items: secMap[sec] }))
})

async function load(id) {
  try {
    const r = await fetchPackageDetail(id)
    if (r) { pkg.value = r; return }
  } catch {}
  // 兜底：从 packages 页面保存的临时列表里找
  const cached = uni.getStorageSync('yjcy_pkg_list_' + id)
  if (cached) { try { pkg.value = JSON.parse(cached) } catch {} }
  if (!pkg.value) {
    pkg.value = { id, name: '又见炊烟·定制套餐', price: '2688', serves: '10人/桌', dishes: ['定制菜单 可致电门店'] }
  }
}

function onCall() {
  const phone = appStore.currentStore?.phone
  if (!phone) return uni.showToast({ title: '门店电话暂未设置', icon: 'none' })
  uni.makePhoneCall({ phoneNumber: String(phone).replace(/[^\d-]/g,'') }).catch(() => {})
}
function bookNow(p = pkg.value) {
  if (!p) return
  uni.setStorageSync('yjcy_booking_pkg', JSON.stringify({ packageId: p.id, name: p.name, price: p.price }))
  uni.switchTab({ url: '/pages/book/book' })
}

onMounted(() => {
  const pages = getCurrentPages()
  const pg = pages[pages.length - 1]
  const id = Number(pg?.options?.id || 0)
  if (id) load(id)
  else pkg.value = { id: 0, name: '套餐详情', price: '—', dishes: [] }
})
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.pd-page { padding-bottom: 0; background: $brand-bg; min-height: 100vh; }

.banner { width: 100%; height: 480rpx; background: #eee; display: block; }

.info-card {
  background: #fff; margin: -40rpx $spacing-md $spacing-md; position: relative;
  border-radius: $radius-lg; padding: $spacing-lg; box-shadow: $shadow-card;
}
.tag { display: inline-block; padding: 4rpx 18rpx; background: #FFEAEA; color: #E53935; border-radius: $radius-pill; font-size: $font-xs; margin-bottom: 10rpx; }
.pkg-name { display:block; font-size: 42rpx; font-weight: 800; color: $text-1; }
.pkg-serve { display:block; color: $text-3; font-size: $font-xs; margin-top: 6rpx; }

.price-row { margin: 18rpx 0; display:flex; align-items: baseline; gap: 10rpx; flex-wrap: wrap; }
.unit { color: #E53935; font-size: $font-lg; font-weight: 700; }
.price { color: #E53935; font-size: 64rpx; font-weight: 900; line-height: 1; }
.old { color: $text-4; text-decoration: line-through; }
.discount { padding: 4rpx 16rpx; background: linear-gradient(135deg,#FFF3D6,#FFE3A8); color: $brand-gold-dark; border-radius: $radius-pill; font-size: $font-xs; font-weight: 700; }

.tips { display:flex; flex-direction: column; gap: 6rpx; margin-top: 10rpx; }
.t-item { font-size: $font-xs; color: $brand-gold-dark; }

.card { background: #fff; border-radius: $radius-lg; padding: $spacing-md; margin: 0 $spacing-md $spacing-md; box-shadow: $shadow-card; }
.c-title { display:flex; align-items: center; gap: 12rpx; margin-bottom: $spacing-sm; }
.c-title .bar { width: 8rpx; height: 28rpx; background: linear-gradient(180deg,#E6D4A8,#C9A86C); border-radius: 4rpx; }
.c-title text:last-child { font-size: $font-md; font-weight: 700; color: $text-1; }

.menu-tbl { border: 1rpx solid #F4F0E6; border-radius: $radius-md; overflow: hidden; }
.tbl-sec-head { background: #FFFBF0; color: $brand-gold-dark; padding: 10rpx 20rpx; font-size: $font-sm; font-weight: 700; border-bottom: 1rpx solid #F4F0E6; }
.tbl-row { display: flex; justify-content: space-between; padding: 12rpx 20rpx; font-size: $font-sm; color: $text-2; border-bottom: 1rpx dashed #F7F3E7; }
.tbl-row:last-child { border-bottom: 0; }
.d-note { color: $text-3; font-size: $font-xs; }

.notice { color: $text-3; font-size: $font-xs; line-height: 2; }

.loading { text-align: center; color: $text-3; padding: 200rpx 0; }

.bottom-bar {
  position: fixed; left: 0; right: 0; bottom: 0;
  padding: $spacing-sm $spacing-md calc(env(safe-area-inset-bottom) + 16rpx);
  background: #fff; box-shadow: 0 -4rpx 20rpx rgba(0,0,0,0.06);
  display: flex; align-items: center; gap: $spacing-md;
}
.b-left { display:flex; align-items:center; gap: 8rpx; color: $brand-gold-dark; padding: 0 20rpx; }
.bl-icon { font-size: 36rpx; }
.bl-text { font-size: $font-sm; font-weight: 600; }
.b-btn {
  flex: 1; height: 92rpx; line-height: 92rpx; text-align: center; color:#fff; font-weight: 800; letter-spacing: 2rpx;
  background: linear-gradient(135deg, #E53935, #FF6246); border-radius: $radius-pill; box-shadow: 0 6rpx 18rpx rgba(229,57,53,0.3);
}
</style>

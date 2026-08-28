<template>
  <view class="pkg-page">
    <!-- 顶部横幅 + 类型筛选 -->
    <view class="header-card">
      <view class="title">又见炊烟 · 宴会套餐</view>
      <view class="subtitle">家庭团聚 · 喜寿宴席 · 商务宴请 · 上门定制</view>
      <view class="filters">
        <text
          v-for="t in types"
          :key="t.value"
          :class="['filter-chip', {active: selectedType === t.value}]"
          @tap="onPickType(t.value)"
        >{{ t.label }}</text>
      </view>
    </view>

    <!-- 套餐列表 -->
    <view v-if="list.length" class="pkg-list">
      <view class="pkg-card" v-for="p in list" :key="p.id" @tap="openDetail(p)">
        <view class="pkg-media">
          <image class="pkg-img" :src="p.coverImage || p.image || p.imageUrl || placeholder" mode="aspectFill" />
          <view v-if="p.tag" class="pkg-tag">{{ p.tag }}</view>
          <view v-if="p.hot" class="pkg-hot">🔥 热销</view>
        </view>
        <view class="pkg-body">
          <view class="pkg-top">
            <text class="pkg-name ellipsis-1">{{ p.name }}</text>
            <text class="pkg-serve">{{ p.serves || '10-12 位适用' }}</text>
          </view>
          <text class="pkg-desc ellipsis-2">{{ p.description || p.desc || '精美冷盘 + 特色头菜 + 汤羹 + 主菜 + 主食点心，精心搭配 16-22 道' }}</text>
          <view class="pkg-items-tags">
            <text v-for="(d, i) in displayDishes(p)" :key="i" class="d-tag">{{ d }}</text>
          </view>
          <view class="pkg-bottom">
            <view class="price-row">
              <text class="p-unit">¥</text>
              <text class="p-price">{{ p.price || '1688' }}</text>
              <text class="p-old" v-if="p.oldPrice">¥{{ p.oldPrice }}</text>
            </view>
            <view class="pkg-actions">
              <text class="book-btn sm" @tap.stop="callStore">咨询</text>
              <text class="book-btn" @tap.stop="bookNow(p)">立即预订</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <view v-else class="empty-box">
      <text class="e-icon">🎎</text>
      <text class="e-text">套餐加载中… 请稍后</text>
    </view>

    <view style="height: 80rpx"></view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import { useAppStore } from '@/store/app'
import { fetchPackages } from '@/api/package'
import { IMG_PLACEHOLDER } from '@/config/env'

const appStore = useAppStore()
const placeholder = IMG_PLACEHOLDER

const types = [
  { label: '全部', value: '' },
  { label: '婚宴喜宴', value: 'WEDDING' },
  { label: '生日寿宴', value: 'BIRTHDAY' },
  { label: '家庭团圆', value: 'FAMILY' },
  { label: '商务宴请', value: 'COMPANY' },
  { label: '谢师升学', value: 'GRAD' }
]
const selectedType = ref('')

const rawList = ref([])
function onPickType(v) { selectedType.value = v }
const list = computed(() => {
  const base = rawList.value
  if (!selectedType.value) return base
  return base.filter(p => (p.type || '').toUpperCase() === selectedType.value)
})

function displayDishes(p) {
  const items = Array.isArray(p.dishes) ? p.dishes.map(d => typeof d === 'string' ? d : d.name)
                : (p.dishList || p.menu || '').toString().split(/[，,、]/).map(s => s.trim()).filter(Boolean)
  return items.slice(0, 5)
}

async function load() {
  try {
    let r = await fetchPackages({ type: '' })
    if (!Array.isArray(r)) r = r?.records || r?.list || []
    rawList.value = r && r.length ? r : fallbackPkgs()
  } catch {
    rawList.value = fallbackPkgs()
  }
}

function fallbackPkgs() {
  return [
    {
      id: 9001, name: '百年好合婚宴套餐', type: 'WEDDING', serves: '10人/桌',
      tag: '新人专享', hot: true, price: '2288', oldPrice: '2688',
      coverImage: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20wedding%20banquet%20red%20gold%20table%2016%20dishes&image_size=landscape_16_9',
      desc: '冷盘8道+头盘1道+主菜10道+汤羹2道+主食点心+果盘，菜单可定制',
      dishes: ['八福喜临门','潮式卤拼','金汤佛跳墙','清蒸石斑鱼','避风塘大虾','黑椒牛柳粒','金玉满堂','百年好合甜汤']
    },
    {
      id: 9002, name: '福寿安康寿宴套餐', type: 'BIRTHDAY', serves: '10人/桌',
      tag: '寿桃赠礼', price: '1888', oldPrice: '2188',
      coverImage: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20birthday%20longevity%20banquet%20peach%20shoutao&image_size=landscape_16_9',
      desc: '寓意福寿绵长的经典徽菜组合，赠送寿桃面点一份',
      dishes: ['卤味拼盘','老火鸡汤','红烧大鲍翅','椒盐大虾','黑松露牛肉','清蒸海石斑','团圆大盆菜','寿桃贺寿面']
    },
    {
      id: 9003, name: '阖家团圆家宴套餐', type: 'FAMILY', serves: '6-8人',
      tag: '每日限量', price: '988', oldPrice: '1188',
      coverImage: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20family%20dinner%20huizhou%20dishes%20top%20view&image_size=landscape_16_9',
      desc: '合家欢6人小份菜，地道徽菜 + 农家小炒，老人小孩都爱吃',
      dishes: ['农家土鸡汤','徽州臭鳜鱼','毛豆腐锅仔','腊味笋衣','问政山笋','时蔬两道','葛粉圆子','酒酿圆子']
    },
    {
      id: 9004, name: '和鸣商务宴套餐', type: 'COMPANY', serves: '10-12人',
      tag: '私密包厢', hot: true, price: '2688', oldPrice: '3088',
      coverImage: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20business%20dining%20vip%20room%20premium%20dishes&image_size=landscape_16_9',
      desc: 'VIP包厢 + 精致摆盘 + 专属服务员，商务接待首选',
      dishes: ['刺身拼盘','红烧大鲍鱼','燕液炖血燕','清蒸老鼠斑','蒜香波士顿龙虾','雪花和牛粒','黑松露捞饭','精美果盘']
    },
    {
      id: 9005, name: '金榜题名升学宴', type: 'GRAD', serves: '10人/桌',
      tag: '满10桌送', price: '1588', oldPrice: '1888',
      coverImage: 'https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=chinese%20graduation%20banquet%20red%20honor%20table&image_size=landscape_16_9',
      desc: '赠主桌鲜花布置 + 状元糕礼盒，满10桌额外赠投影仪一套使用',
      dishes: ['节节高升虾','鱼跃龙门煲','金榜题名蹄','鹏程万里翅','学业有成羹','连中三元丸','谢师感恩面','锦绣前程果']
    }
  ]
}

function openDetail(p) {
  uni.navigateTo({ url: `/pages/packageDetail/packageDetail?id=${p.id}` })
}
function bookNow(p) {
  // 直接跳预订页，把套餐信息塞进去（用 storage 传递较稳妥）
  uni.setStorageSync('yjcy_booking_pkg', JSON.stringify({ packageId: p.id, name: p.name, price: p.price }))
  uni.switchTab({ url: '/pages/book/book' })
}
function callStore() {
  const phone = appStore.currentStore?.phone || '暂无'
  if (phone === '暂无') return uni.showToast({ title: '门店电话暂未设置', icon: 'none' })
  uni.makePhoneCall({ phoneNumber: String(phone).replace(/[^\d-]/g,'') }).catch(() => {})
}

onMounted(load)
onPullDownRefresh(async () => { await load(); uni.stopPullDownRefresh() })
</script>

<style lang="scss" scoped>
@import '@/uni.scss';
.pkg-page { padding: $spacing-md; background: $brand-bg; min-height: 100vh; box-sizing: border-box; }

.header-card {
  background: linear-gradient(135deg, #FFF6DB, #FFDCA1 60%, #C9A86C);
  border-radius: $radius-lg; padding: $spacing-lg; color: #3C2C00; box-shadow: $shadow-gold;
  margin-bottom: $spacing-md;
}
.title    { font-size: 42rpx; font-weight: 800; letter-spacing: 2rpx; }
.subtitle { font-size: $font-sm; color: #5A4714; margin-top: 8rpx; }
.filters  { margin-top: $spacing-md; display:flex; flex-wrap: wrap; gap: 12rpx; }
.filter-chip {
  padding: 10rpx 24rpx; background: rgba(255,255,255,0.7); color: #5A4714;
  border-radius: $radius-pill; font-size: $font-xs;
}
.filter-chip.active { background: #1A1A1A; color: #FFDCA1; font-weight: 700; }

/* 套餐卡片 */
.pkg-list { display:flex; flex-direction: column; gap: $spacing-md; }
.pkg-card {
  background: #fff; border-radius: $radius-lg; overflow: hidden; box-shadow: $shadow-card;
}
.pkg-media { position: relative; }
.pkg-img { width: 100%; height: 320rpx; background:#eee; }
.pkg-tag {
  position: absolute; left: 20rpx; top: 20rpx; padding: 6rpx 18rpx;
  background: rgba(230, 57, 53, 0.95); color:#fff; font-size: $font-xs; border-radius: $radius-pill;
}
.pkg-hot {
  position: absolute; right: 20rpx; top: 20rpx; padding: 6rpx 18rpx;
  background: rgba(201, 168, 108, 0.95); color:#fff; font-size: $font-xs; border-radius: $radius-pill;
}
.pkg-body { padding: $spacing-md; }
.pkg-top { display:flex; align-items: baseline; justify-content: space-between; }
.pkg-name { font-size: $font-lg; font-weight: 800; color: $text-1; }
.pkg-serve { font-size: $font-xs; color: $text-3; }
.pkg-desc { display:block; font-size: $font-sm; color: $text-2; margin: 10rpx 0 14rpx; line-height: 1.6; }
.pkg-items-tags { display:flex; flex-wrap: wrap; gap: 10rpx; margin-bottom: 14rpx; }
.d-tag {
  padding: 4rpx 14rpx; background: #FFFBF0; border: 1rpx solid #F1E5C2;
  color: $brand-gold-dark; font-size: 22rpx; border-radius: 6rpx;
}
.pkg-bottom { display:flex; align-items: center; justify-content: space-between; margin-top: 8rpx; padding-top: 14rpx; border-top: 1rpx dashed #F4F0E6; }
.price-row { display:flex; align-items: baseline; gap: 6rpx; }
.p-unit { color: #E53935; font-size: $font-md; font-weight: 700; }
.p-price { color: #E53935; font-size: 52rpx; font-weight: 900; line-height: 1; }
.p-old  { color: $text-4; text-decoration: line-through; margin-left: 10rpx; font-size: $font-xs; }
.pkg-actions { display:flex; gap: 12rpx; }
.book-btn {
  padding: 16rpx 32rpx; color:#fff; font-size: $font-sm; font-weight: 700;
  background: linear-gradient(135deg, #E6D4A8, #C9A86C); border-radius: $radius-pill;
  box-shadow: $shadow-gold;
}
.book-btn.sm {
  background: #fff; color: $brand-gold-dark; border: 1rpx solid $brand-gold; box-shadow: none;
}

.empty-box { padding: 120rpx 0; text-align:center; color: $text-3; }
.e-icon { font-size: 100rpx; display:block; margin-bottom: 20rpx; }
</style>

<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', en: 'Home', to: '/' }, { label: '门店选择', en: 'Restaurants', to: '/stores' }, { label: store.storeName || '门店详情' }]" />

    <section v-if="store.storeId" class="store-hero">
      <p class="page-eyebrow">{{ store.storeName }}</p>
      <h1 class="page-title">{{ store.storeName }}</h1>
      <div class="store-meta-row">
        <span>📍 {{ store.address }}</span>
        <span>🕐 {{ store.businessHours }}</span>
        <span>📞 {{ store.phone }}</span>
      </div>
      <div class="store-hero-actions">
        <a class="btn-gold" :href="'tel:' + store.phone">致电预定</a>
        <button class="btn-outline-dark" @click="openMap">查看地图</button>
      </div>
    </section>

    <section class="page-body" v-if="store.storeId">
      <!-- 该店菜单 -->
      <div class="block">
        <h2 class="block-title">菜单介绍</h2>
        <div v-if="dishesLoading" class="loading">菜单加载中...</div>
        <div v-else class="dish-grid">
          <div v-for="(d, i) in dishes" :key="i" class="dish-card">
            <h4>{{ d.dishName }}</h4>
            <div class="dish-meta">
              <span>{{ d.dishCategory || '精选' }}</span>
              <span class="price">¥{{ formatPrice(d.salePrice) }}</span>
            </div>
          </div>
        </div>
        <div class="block-more"><a @click="$router.push('/menu')">查看完整菜单 →</a></div>
      </div>

      <!-- 环境 -->
      <div class="block">
        <h2 class="block-title">包厢与环境</h2>
        <div class="env-grid">
          <img v-for="(p, i) in envPhotos" :key="i" :src="p" alt="门店环境" />
        </div>
      </div>

      <!-- 套餐与优惠 -->
      <div class="block">
        <h2 class="block-title">套餐与优惠</h2>
        <div v-if="pkgLoading" class="loading">套餐加载中...</div>
        <div v-else-if="packages.length === 0" class="loading">套餐信息完善中</div>
        <div v-else class="pkg-grid">
          <div v-for="p in packages" :key="p.packageId" class="pkg-card">
            <h4>{{ p.packageName }}</h4>
            <p class="pkg-price">¥{{ formatPrice(p.price) }} <span v-if="p.originalPrice" class="pkg-original">¥{{ formatPrice(p.originalPrice) }}</span></p>
            <p class="pkg-meta">{{ p.minGuests }}-{{ p.maxGuests }}人 · {{ p.dishCount }}道菜</p>
          </div>
        </div>
        <div class="block-more"><a @click="$router.push('/packages')">查看全部宴会套餐 →</a></div>
      </div>

      <!-- 预定留资 -->
      <div class="block booking-block">
        <h2 class="block-title">落实预定</h2>
        <p class="block-sub">填写以下信息，我们会尽快与您电话确认</p>
        <form class="booking-form" @submit.prevent="submitInquiry">
          <div class="form-row">
            <input v-model="form.customerName" placeholder="您的姓名 *" required />
            <input v-model="form.customerPhone" placeholder="手机号 *" required />
          </div>
          <div class="form-row">
            <input v-model="form.preferredDate" type="date" />
            <input v-model="form.preferredTime" placeholder="期望时段，如 午市/晚市" />
            <input v-model.number="form.guestCount" type="number" min="1" placeholder="用餐人数" />
          </div>
          <textarea v-model="form.remark" placeholder="备注：包厢需求、宴席类型、忌口等"></textarea>
          <button class="btn-gold submit-btn" type="submit" :disabled="submitting">
            {{ submitting ? '提交中...' : (submitted ? '已提交，我们会尽快联系您' : '提交预定申请') }}
          </button>
        </form>
      </div>
    </section>

    <div v-else class="loading page-loading">门店信息加载中...</div>

    <!-- 地图大窗口 -->
    <div v-if="mapOpen" class="map-modal-mask" @click.self="mapOpen = false">
      <div class="map-modal">
        <button class="map-modal-close" @click="mapOpen = false">✕</button>
        <h3 class="map-modal-title">{{ store.storeName }}</h3>
        <p class="map-modal-address">📍 {{ store.address }}</p>
        <div class="map-modal-links">
          <a class="btn-gold" :href="amapUrl" target="_blank" rel="noopener">在高德地图中打开</a>
          <a class="btn-outline-dark" :href="tencentMapUrl" target="_blank" rel="noopener">在腾讯地图中打开</a>
        </div>
        <div class="map-modal-qr">
          <canvas ref="mapQrCanvas"></canvas>
          <span class="map-modal-qr-label">手机扫码，直接在地图App中导航</span>
        </div>
      </div>
    </div>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import QRCode from 'qrcode'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import request from '@/utils/request'

const route = useRoute()
const store = ref({})
const dishes = ref([])
const dishesLoading = ref(true)
const packages = ref([])
const pkgLoading = ref(true)
const mapOpen = ref(false)
const mapQrCanvas = ref(null)
const submitting = ref(false)
const submitted = ref(false)

// 两店共用的真实环境实拍（用户确认过：不区分门店）
const envPhotos = [
  '/site-photos/private-room-chandelier.jpg',
  '/site-photos/private-room-cityview.jpg',
  '/site-photos/rooftop-terrace-dusk.jpg',
  '/site-photos/terrace-dining-real.jpg'
]

const form = reactive({
  customerName: '',
  customerPhone: '',
  preferredDate: '',
  preferredTime: '',
  guestCount: null,
  remark: ''
})

const amapUrl = computed(() => `https://uri.amap.com/search?keyword=${encodeURIComponent(store.value.address || '')}`)
const tencentMapUrl = computed(() => `https://apis.map.qq.com/uri/v1/search?keyword=${encodeURIComponent(store.value.address || '')}&referer=YJCY`)

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

async function openMap() {
  mapOpen.value = true
  await nextTick()
  if (mapQrCanvas.value) QRCode.toCanvas(mapQrCanvas.value, amapUrl.value, { width: 160, margin: 1 })
}

async function loadStore() {
  const res = await request.get('/api/public/stores')
  const list = (res.data || []).map(s => ({
    storeId: s.store_id ?? s.storeId,
    storeName: s.store_name ?? s.storeName,
    address: s.address,
    phone: s.phone,
    businessHours: s.business_hours ?? s.businessHours
  }))
  store.value = list.find(s => String(s.storeId) === String(route.params.storeId)) || list[0] || {}
}

async function loadDishes() {
  dishesLoading.value = true
  try {
    const res = await request.get('/api/public/menu/preview', { params: { storeId: store.value.storeId, limit: 6 } })
    dishes.value = (res.data || []).map(d => ({
      dishName: d.dish_name ?? d.dishName,
      dishCategory: d.dish_category ?? d.dishCategory,
      salePrice: d.sale_price ?? d.salePrice
    }))
  } catch (e) {
    dishes.value = []
  } finally {
    dishesLoading.value = false
  }
}

async function loadPackages() {
  pkgLoading.value = true
  try {
    const res = await request.get('/api/public/packages', { params: { storeId: store.value.storeId } })
    packages.value = (res.data || []).slice(0, 4).map(p => ({
      packageId: p.package_id ?? p.packageId,
      packageName: p.package_name ?? p.packageName,
      price: p.price,
      originalPrice: p.original_price ?? p.originalPrice,
      minGuests: p.min_guests ?? p.minGuests,
      maxGuests: p.max_guests ?? p.maxGuests,
      dishCount: p.dish_count ?? p.dishCount
    }))
  } catch (e) {
    packages.value = []
  } finally {
    pkgLoading.value = false
  }
}

async function submitInquiry() {
  submitting.value = true
  try {
    await request.post('/api/public/booking-inquiry', {
      storeId: store.value.storeId,
      customerName: form.customerName,
      customerPhone: form.customerPhone,
      preferredDate: form.preferredDate || undefined,
      preferredTime: form.preferredTime,
      guestCount: form.guestCount,
      remark: form.remark
    })
    submitted.value = true
  } catch (e) {
    alert(e?.message || '提交失败，请稍后重试或直接致电门店')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadStore()
  loadDishes()
  loadPackages()
})
</script>

<style scoped>
.site-page {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #7A7A72;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif;
  color: var(--ink);
  background: var(--ivory);
  min-height: 100vh;
}
.store-hero { max-width: 1200px; margin: 0 auto; padding: 40px 32px 24px; text-align: center; }
.page-eyebrow { font-size: 13px; letter-spacing: 3px; color: var(--gold); margin: 0 0 10px; font-weight: 600; }
.page-title { font-size: 32px; font-weight: 700; color: var(--forest); margin: 0 0 16px; }
.store-meta-row { display: flex; justify-content: center; gap: 24px; font-size: 14px; color: var(--muted); margin-bottom: 24px; flex-wrap: wrap; }
.store-hero-actions { display: flex; justify-content: center; gap: 12px; }

.btn-gold {
  background: var(--gold); border: 1px solid var(--gold); color: #fff;
  padding: 10px 24px; border-radius: 2px; font-size: 13px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s; display: inline-block; text-decoration: none; text-align: center;
}
.btn-gold:hover { background: #A17E48; }
.btn-gold:disabled { opacity: 0.6; cursor: not-allowed; }
.btn-outline-dark {
  background: transparent; border: 1px solid var(--forest); color: var(--forest);
  padding: 10px 24px; border-radius: 2px; font-size: 13px; letter-spacing: 1px; cursor: pointer;
}
.btn-outline-dark:hover { background: var(--forest); color: #fff; }

.page-body { max-width: 1000px; margin: 0 auto; padding: 20px 32px 100px; }
.page-loading { padding: 120px 0; }
.loading { text-align: center; color: var(--muted); padding: 30px 0; }
.block { background: #fff; border-radius: 6px; padding: 32px; margin-bottom: 28px; box-shadow: 0 2px 16px rgba(0,0,0,0.04); }
.block-title { font-size: 20px; font-weight: 700; color: var(--forest); margin: 0 0 20px; }
.block-sub { font-size: 13px; color: var(--muted); margin: -12px 0 24px; }
.block-more { margin-top: 16px; text-align: right; }
.block-more a { font-size: 13px; color: var(--forest); font-weight: 600; cursor: pointer; }

.dish-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.dish-card { border: 1px solid #EDE7D9; border-radius: 4px; padding: 16px; }
.dish-card h4 { font-size: 15px; color: var(--forest); margin: 0 0 8px; }
.dish-meta { display: flex; justify-content: space-between; font-size: 13px; color: var(--muted); }
.dish-meta .price { color: var(--forest); font-weight: 700; }

.env-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; }
.env-grid img { width: 100%; height: 140px; object-fit: cover; border-radius: 4px; }

.pkg-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.pkg-card { border: 1px solid #EDE7D9; border-radius: 4px; padding: 18px; }
.pkg-card h4 { font-size: 15px; color: var(--forest); margin: 0 0 8px; }
.pkg-price { font-size: 17px; font-weight: 700; color: var(--forest); margin: 0 0 6px; }
.pkg-original { font-size: 12px; color: var(--muted); text-decoration: line-through; font-weight: 400; margin-left: 6px; }
.pkg-meta { font-size: 12px; color: var(--muted); margin: 0; }

.booking-form { display: flex; flex-direction: column; gap: 14px; }
.form-row { display: flex; gap: 14px; }
.form-row input, .booking-form textarea {
  flex: 1; border: 1px solid #DDD3B8; border-radius: 3px; padding: 11px 14px; font-size: 14px;
  font-family: inherit; color: var(--ink);
}
.booking-form textarea { min-height: 80px; resize: vertical; }
.submit-btn { align-self: flex-start; padding: 12px 32px; }

.map-modal-mask { position: fixed; inset: 0; z-index: 200; background: rgba(20,32,26,0.6); display: flex; align-items: center; justify-content: center; padding: 24px; }
.map-modal { background: #fff; border-radius: 6px; padding: 40px; width: 100%; max-width: 560px; position: relative; }
.map-modal-close { position: absolute; top: 16px; right: 16px; background: none; border: none; font-size: 18px; color: var(--muted); cursor: pointer; }
.map-modal-title { font-size: 22px; font-weight: 700; color: var(--forest); margin: 0 0 12px; }
.map-modal-address { font-size: 15px; color: #4A4A44; margin: 0 0 28px; }
.map-modal-links { display: flex; gap: 12px; margin-bottom: 28px; }
.map-modal-links a { flex: 1; text-align: center; }
.map-modal-qr { display: flex; flex-direction: column; align-items: center; gap: 10px; padding-top: 24px; border-top: 1px solid #EDE7D9; }
.map-modal-qr-label { font-size: 13px; color: var(--muted); }

@media (max-width: 960px) {
  .dish-grid, .env-grid, .pkg-grid { grid-template-columns: repeat(2, 1fr); }
  .form-row { flex-direction: column; }
}
</style>

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
      </div>
    </section>

    <section class="page-body" v-if="store.storeId">
      <!-- 十大特色菜肴：只做橱窗展示，真正点菜/加购物篮在独立的点菜页完成 -->
      <div class="block">
        <div class="menu-teaser-head">
          <div>
            <h2 class="block-title">十大特色菜肴</h2>
            <p class="block-sub">Top 10 Signature Dishes</p>
          </div>
          <button class="btn-gold order-cta" @click="$router.push(`/stores/${store.storeId}/order`)">我要点菜 · Order Now</button>
        </div>
        <div v-if="dishesLoading" class="loading">菜单加载中...</div>
        <div v-else-if="topDishes.length === 0" class="loading">菜单信息完善中，敬请期待</div>
        <div v-else class="dish-grid">
          <div v-for="(d, i) in topDishes" :key="i" class="dish-card">
            <h4>{{ d.dishName }}</h4>
            <div class="dish-meta">
              <span>{{ d.dishCategory || '精选' }}</span>
              <span class="price">¥{{ formatPrice(d.salePrice) }}</span>
            </div>
          </div>
        </div>
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

      <!-- 预定留资：想仔细点菜请走上面"我要点菜"，这里是不选菜直接留资的快捷通道 -->
      <div class="block booking-block">
        <h2 class="block-title">落实预定</h2>
        <p class="block-sub">还没想好吃什么也没关系，先留资，我们电话与您确认菜单</p>

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

      <!-- 查预订：客人自己核对行程用。放在地图之前，因为"我订了没"比"怎么来"更常被问到 -->
      <div class="block">
        <h2 class="block-title">查询我的预订</h2>
        <p class="lookup-hint">
          需要同时填写<strong>预订时留的手机号</strong>与<strong>预订单号</strong>，两者都对才能查到。
        </p>
        <form class="lookup-form" @submit.prevent="doLookup">
          <label class="lookup-field">
            <span>预订手机号</span>
            <input v-model="lookupForm.phone" type="tel" inputmode="numeric" maxlength="11"
                   placeholder="11 位手机号" autocomplete="off" />
          </label>
          <label class="lookup-field">
            <span>预订单号</span>
            <input v-model="lookupForm.bookingId" type="text" maxlength="20"
                   placeholder="以 BK 开头的单号" autocomplete="off" />
          </label>
          <button class="lookup-submit" type="submit" :disabled="lookupBusy">
            {{ lookupBusy ? '查询中…' : '查询' }}
          </button>
        </form>

        <p v-if="lookupMessage" class="lookup-message" role="status">{{ lookupMessage }}</p>

        <div v-if="lookupBooking" class="lookup-result">
          <div class="lookup-row"><span>预订单号</span><b>{{ lookupBooking.booking_id }}</b></div>
          <div class="lookup-row"><span>到店日期</span><b>{{ lookupBooking.booking_date || '--' }}</b></div>
          <div class="lookup-row"><span>到店时间</span><b>{{ lookupBooking.booking_time || '--' }}</b></div>
          <div class="lookup-row"><span>用餐人数</span><b>{{ lookupBooking.guest_count ?? '--' }}</b></div>
          <div class="lookup-row"><span>桌台数</span><b>{{ lookupBooking.table_count ?? '--' }}</b></div>
          <div class="lookup-row"><span>状态</span><b>{{ bookingStatusText(lookupBooking.booking_status) }}</b></div>
        </div>
      </div>

      <!-- 地图放最后，符合"先看内容，最后看怎么来"的浏览习惯 -->
      <div class="block">
        <h2 class="block-title">位置地图</h2>
        <StoreMap :stores="[store]" />
      </div>
    </section>

    <div v-else class="loading page-loading">门店信息加载中...</div>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import StoreMap from '@/components/site/StoreMap.vue'
import request from '@/utils/request'
import { lookupBooking as requestLookup, validateLookupInput, readLookupResult, bookingStatusText }
  from '@/api/publicBooking'

const route = useRoute()
const store = ref({})
const topDishes = ref([])
const dishesLoading = ref(true)
const packages = ref([])
const pkgLoading = ref(true)
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

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
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
    const res = await request.get('/api/public/menu/preview', { params: { storeId: store.value.storeId, limit: 10 } })
    topDishes.value = (res.data || []).map(d => ({
      dishName: d.dish_name ?? d.dishName,
      dishCategory: d.dish_category ?? d.dishCategory,
      salePrice: d.sale_price ?? d.salePrice
    }))
  } catch (e) {
    topDishes.value = []
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

// ==================== 查询我的预订 ====================
// 口径与后端严格一致：查不到的各种原因**共用同一句提示**，界面不做任何区分。
// 后端刻意把「查无 / 手机号不符 / 别人的单 / 别店的单」抹平成同一种空结果，
// 就是为了不让人拿一个单号去试不同手机号反推机主；前端要是分开提示，这道防线就白设了。
const lookupForm = reactive({ phone: '', bookingId: '' })
const lookupBusy = ref(false)
const lookupBooking = ref(null)
const lookupMessage = ref('')

async function doLookup() {
  if (lookupBusy.value) return
  lookupBooking.value = null
  const checked = validateLookupInput(lookupForm.phone, lookupForm.bookingId)
  if (!checked.ok) { lookupMessage.value = checked.message; return }

  lookupBusy.value = true
  lookupMessage.value = ''
  try {
    const result = readLookupResult(await requestLookup(checked.payload))
    if (result.state === 'found') {
      lookupBooking.value = result.booking
    } else {
      lookupMessage.value = result.message
    }
  } catch (e) {
    // 网络层失败也不透露细节，与"查不到"同一句，避免从错误差异反推
    lookupMessage.value = readLookupResult(null).message
  } finally {
    lookupBusy.value = false
  }
}

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

.menu-teaser-head { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; gap: 16px; flex-wrap: wrap; }
.menu-teaser-head .block-title { margin-bottom: 4px; }
.menu-teaser-head .block-sub { margin: 0; font-size: 11px; letter-spacing: 1px; color: var(--muted); }
.order-cta { flex-shrink: 0; white-space: nowrap; }

.dish-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.dish-card { border: 1px solid #EDE7D9; border-radius: 4px; padding: 16px; }
.dish-card h4 { font-size: 15px; color: var(--forest); margin: 0 0 8px; }
.dish-meta { display: flex; justify-content: space-between; font-size: 13px; color: var(--muted); }
.dish-meta .price { color: var(--forest); font-weight: 700; }
.submitted-summary p { font-size: 13px; color: var(--forest); margin: 0 0 8px; font-weight: 600; }
.submitted-summary ul { margin: 0; padding-left: 20px; }
.submitted-summary li { font-size: 12.5px; color: var(--muted); line-height: 1.8; }

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

@media (max-width: 960px) {
  .dish-grid, .env-grid, .pkg-grid { grid-template-columns: repeat(2, 1fr); }
  .form-row { flex-direction: column; }
}

/* 查预订：两栏在桌面并排，手机上自动堆叠；输入框用 min-width:0 防止 flex 子项撑破容器 */
.lookup-hint { color: #666; font-size: 14px; line-height: 1.7; margin-bottom: 12px; }
.lookup-form { display: flex; flex-wrap: wrap; gap: 12px; align-items: flex-end; }
.lookup-field { display: flex; flex-direction: column; gap: 6px; flex: 1 1 200px; min-width: 0; }
.lookup-field span { font-size: 13px; color: #555; }
.lookup-field input { width: 100%; box-sizing: border-box; padding: 10px 12px;
  border: 1px solid #ddd; border-radius: 8px; font-size: 15px; }
.lookup-submit { padding: 10px 24px; border: none; border-radius: 8px; background: #2D4A3E;
  color: #fff; font-size: 15px; cursor: pointer; }
.lookup-submit:disabled { opacity: .6; cursor: not-allowed; }
.lookup-message { margin-top: 14px; color: #b3261e; line-height: 1.7; }
.lookup-result { margin-top: 16px; border: 1px solid #e5e5e5; border-radius: 10px; overflow: hidden; }
.lookup-row { display: flex; justify-content: space-between; gap: 16px; padding: 10px 14px;
  border-bottom: 1px solid #f0f0f0; font-size: 14px; }
.lookup-row:last-child { border-bottom: none; }
.lookup-row span { color: #777; }
.lookup-row b { color: #222; word-break: break-all; text-align: right; }
@media (max-width: 600px) {
  .lookup-field { flex: 1 1 100%; }
  .lookup-submit { width: 100%; }
}
</style>

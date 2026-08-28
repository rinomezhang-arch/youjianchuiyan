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
      <!-- 地图：直接嵌入实景地图，不用再让客人扫码跳转 -->
      <div class="block">
        <h2 class="block-title">位置地图</h2>
        <StoreMap :stores="[store]" />
      </div>

      <!-- 该店完整菜单：真正可以点选，选好的菜直接带进下面的预定表单一起提交 -->
      <div class="block">
        <h2 class="block-title">菜单点选</h2>
        <p class="block-sub">点击"+ 选"加入预定单，选好后拉到下方提交预定申请（共 {{ allDishes.length }} 道菜）</p>
        <div v-if="dishesLoading" class="loading">菜单加载中...</div>
        <div v-else-if="allDishes.length === 0" class="loading">菜单信息完善中，敬请期待</div>
        <template v-else>
          <div class="cat-tabs">
            <button :class="{ active: activeCat === '全部' }" @click="activeCat = '全部'">全部</button>
            <button v-for="c in categories" :key="c" :class="{ active: activeCat === c }" @click="activeCat = c">{{ c }}</button>
          </div>
          <div class="full-dish-grid">
            <div v-for="d in filteredDishes" :key="d.dishId" class="fdish-card" :class="{ on: isSelected(d.dishId) }">
              <div class="fdish-info">
                <h4>{{ d.dishName }}</h4>
                <span class="fdish-cat">{{ d.dishCategory }}</span>
              </div>
              <div class="fdish-action">
                <span class="fdish-price">¥{{ formatPrice(d.salePrice) }}</span>
                <button class="fdish-btn" @click="toggleDish(d)">{{ isSelected(d.dishId) ? '已选 ✓' : '+ 选' }}</button>
              </div>
            </div>
          </div>
        </template>
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

        <div class="selected-bar">
          <p class="selected-label">已选菜品（{{ selectedDishes.length }}）</p>
          <div v-if="selectedDishes.length === 0" class="selected-empty">还没有选菜，也可以先留资，我们电话与您确认菜单</div>
          <div v-else class="selected-chips">
            <span v-for="d in selectedDishes" :key="d.dishId" class="selected-chip">
              {{ d.dishName }} · ¥{{ formatPrice(d.salePrice) }}
              <i @click="toggleDish(d)">✕</i>
            </span>
          </div>
        </div>

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

        <div v-if="submitted" class="submitted-summary">
          <p>✓ 已收到您的预定申请{{ selectedDishes.length ? '，含以下 ' + selectedDishes.length + ' 道已选菜品：' : '，我们会尽快电话与您确认。' }}</p>
          <ul v-if="selectedDishes.length">
            <li v-for="d in selectedDishes" :key="d.dishId">{{ d.dishName }} · ¥{{ formatPrice(d.salePrice) }}</li>
          </ul>
        </div>
      </div>
    </section>

    <div v-else class="loading page-loading">门店信息加载中...</div>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import StoreMap from '@/components/site/StoreMap.vue'
import request from '@/utils/request'

const route = useRoute()
const store = ref({})
const allDishes = ref([])
const dishesLoading = ref(true)
const activeCat = ref('全部')
const selectedDishes = ref([])
const packages = ref([])
const pkgLoading = ref(true)
const submitting = ref(false)
const submitted = ref(false)

const categories = computed(() => {
  const set = new Set(allDishes.value.map(d => d.dishCategory).filter(Boolean))
  return Array.from(set)
})
const filteredDishes = computed(() => {
  if (activeCat.value === '全部') return allDishes.value
  return allDishes.value.filter(d => d.dishCategory === activeCat.value)
})
function isSelected(dishId) {
  return selectedDishes.value.some(d => d.dishId === dishId)
}
function toggleDish(d) {
  const i = selectedDishes.value.findIndex(x => x.dishId === d.dishId)
  if (i >= 0) selectedDishes.value.splice(i, 1)
  else selectedDishes.value.push(d)
}

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
    const res = await request.get('/api/public/menu/full', { params: { storeId: store.value.storeId } })
    allDishes.value = (res.data || []).map(d => ({
      dishId: d.dish_id ?? d.dishId,
      dishName: d.dish_name ?? d.dishName,
      dishCategory: d.dish_category ?? d.dishCategory,
      salePrice: d.sale_price ?? d.salePrice
    }))
  } catch (e) {
    allDishes.value = []
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
      remark: form.remark,
      selectedDishes: selectedDishes.value.map(d => ({ dishName: d.dishName, salePrice: d.salePrice }))
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

.cat-tabs { display: flex; gap: 8px; flex-wrap: wrap; margin-bottom: 20px; }
.cat-tabs button {
  background: #fff; border: 1px solid #DDD3B8; color: var(--muted);
  padding: 7px 16px; border-radius: 999px; font-size: 12.5px; cursor: pointer; transition: all 0.15s;
}
.cat-tabs button.active { background: var(--forest); border-color: var(--forest); color: #fff; }

.full-dish-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; max-height: 560px; overflow-y: auto; padding-right: 4px; }
.fdish-card {
  border: 1px solid #EDE7D9; border-radius: 4px; padding: 14px; display: flex; flex-direction: column; gap: 10px;
  transition: border-color 0.15s, background 0.15s;
}
.fdish-card.on { border-color: var(--gold); background: rgba(184,147,90,0.06); }
.fdish-info h4 { font-size: 14px; color: var(--forest); margin: 0 0 4px; }
.fdish-cat { font-size: 11px; color: var(--muted); }
.fdish-action { display: flex; justify-content: space-between; align-items: center; }
.fdish-price { font-size: 14px; font-weight: 700; color: var(--forest); }
.fdish-btn {
  background: #fff; border: 1px solid var(--forest); color: var(--forest);
  padding: 5px 12px; border-radius: 3px; font-size: 12px; cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.fdish-card.on .fdish-btn { background: var(--gold); border-color: var(--gold); color: #fff; }
.fdish-btn:hover { background: var(--forest); color: #fff; }

.selected-bar { background: var(--ivory); border-radius: 6px; padding: 18px 20px; margin-bottom: 20px; }
.selected-label { font-size: 13px; font-weight: 700; color: var(--forest); margin: 0 0 10px; }
.selected-empty { font-size: 12.5px; color: var(--muted); margin: 0; }
.selected-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.selected-chip {
  display: inline-flex; align-items: center; gap: 8px; background: #fff; border: 1px solid #DDD3B8;
  border-radius: 999px; padding: 6px 8px 6px 14px; font-size: 12.5px; color: var(--ink);
}
.selected-chip i { cursor: pointer; color: var(--muted); font-style: normal; width: 18px; height: 18px; display: flex; align-items: center; justify-content: center; border-radius: 50%; }
.selected-chip i:hover { background: #EDE7D9; color: var(--ink); }

.submitted-summary { margin-top: 20px; padding: 16px 20px; background: rgba(184,147,90,0.08); border-radius: 6px; }
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
  .full-dish-grid, .env-grid, .pkg-grid { grid-template-columns: repeat(2, 1fr); }
  .form-row { flex-direction: column; }
}
</style>

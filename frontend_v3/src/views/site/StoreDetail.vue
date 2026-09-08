<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', to: '/' }, { label: '门店', to: '/stores' }, { label: store.storeName || '门店详情' }]" />

    <header v-if="store.storeId" class="site-hero store-hero">
      <p class="site-eyebrow">Restaurant</p>
      <h1 class="site-title">{{ store.storeName }}</h1>
      <dl class="store-meta-row">
        <div class="meta-item">
          <dt><SiteIcon name="pin" /></dt>
          <dd>{{ store.address }}</dd>
        </div>
        <div class="meta-item">
          <dt><SiteIcon name="clock" /></dt>
          <dd>{{ store.businessHours }}</dd>
        </div>
        <div class="meta-item">
          <dt><SiteIcon name="phone" /></dt>
          <dd><a :href="'tel:' + store.phone" class="meta-tel">{{ store.phone }}</a></dd>
        </div>
      </dl>
      <div class="store-hero-actions">
        <button class="site-btn site-btn--primary" @click="$router.push(`/stores/${store.storeId}/order`)">
          <SiteIcon name="basket" :size="15" />开始点菜
        </button>
        <a class="site-btn site-btn--ghost" :href="'tel:' + store.phone">致电门店</a>
      </div>
    </header>

    <section class="page-body" v-if="store.storeId">
      <!-- 十大特色菜肴：只做橱窗展示，真正点菜/加购物篮在独立的点菜页完成 -->
      <div class="block">
        <div class="menu-teaser-head">
          <h2 class="block-title">十大特色菜肴</h2>
          <button class="block-cta" @click="$router.push(`/stores/${store.storeId}/order`)">
            看完整菜单并点菜<SiteIcon name="arrow-right" :size="15" class="cta-arrow" />
          </button>
        </div>
        <p v-if="dishesLoading" class="loading">正在取菜单…</p>
        <p v-else-if="topDishes.length === 0" class="loading">这家店的菜单正在整理，可先致电门店问问当日时令。</p>
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
        <p v-if="pkgLoading" class="loading">正在取套餐…</p>
        <p v-else-if="packages.length === 0" class="loading">套餐正在整理中。</p>
        <div v-else class="pkg-grid">
          <div v-for="p in packages" :key="p.packageId" class="pkg-card">
            <h4>{{ p.packageName }}</h4>
            <p class="pkg-price">¥{{ formatPrice(p.price) }} <span v-if="p.originalPrice" class="pkg-original">¥{{ formatPrice(p.originalPrice) }}</span></p>
            <p class="pkg-meta">{{ p.minGuests }}-{{ p.maxGuests }}人 · {{ p.dishCount }}道菜</p>
          </div>
        </div>
        <div class="block-more">
          <a @click="$router.push('/packages')">查看全部宴会套餐<SiteIcon name="arrow-right" :size="15" /></a>
        </div>
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
          <button class="site-btn site-btn--primary submit-btn" type="submit" :disabled="submitting">
            {{ submitting ? '提交中...' : (submitted ? '已提交，我们会尽快联系您' : '提交预定申请') }}
          </button>
        </form>
      </div>

      <!-- 地图放最后，符合"先看内容，最后看怎么来"的浏览习惯 -->
      <div class="block">
        <h2 class="block-title">位置地图</h2>
        <StoreMap :stores="[store]" />
      </div>
    </section>

    <p v-else class="loading page-loading">正在取门店信息…</p>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import SiteIcon from '@/components/site/SiteIcon.vue'
import StoreMap from '@/components/site/StoreMap.vue'
import request from '@/utils/request'

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
</script>

<style scoped>
/*
  门店页是"决定来不来"的一页，所以先把三条硬信息（地址、营业时间、电话）
  从一行 emoji 前缀改成一组带图标的定义列表：图标单色、跟着文字颜色走，
  三条各自成行，扫一眼就知道在哪、几点开、打哪个号。

  原来页面上有两个金色实底按钮（"致电预定"和"我要点菜 · Order Now"），
  一个在首屏一个在菜单块，颜色一样、分量一样，客人不知道该按哪个。
  现在首屏给一对主次分明的按钮，菜单块那个降成带箭头的文字入口。
*/

.store-hero { padding-bottom: var(--site-s6); }

.store-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--site-s3) var(--site-s7);
  margin: var(--site-s5) 0 0;
}
.meta-item { display: flex; align-items: flex-start; gap: 9px; margin: 0; }
.meta-item dt { margin: 0; color: var(--site-ink-3); line-height: 1.6; }
.meta-item dd {
  margin: 0;
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-normal);
  color: var(--site-ink-2);
}
/* 全局 a 带下划线，电话号码顶着一条线看着像误点的链接。
   去掉下划线，hover 时才用黄铜色的下划线表示"这个能点"。 */
.meta-tel {
  color: inherit;
  text-decoration: none;
  border-bottom: 1px solid transparent;
  transition: color var(--site-dur) var(--site-ease), border-color var(--site-dur) var(--site-ease);
}
.meta-tel:hover { color: var(--site-pine); border-bottom-color: var(--site-brass); }

.store-hero-actions { display: flex; gap: var(--site-s3); flex-wrap: wrap; margin-top: var(--site-s6); }
.store-hero-actions .site-btn { text-decoration: none; }

.page-body {
  max-width: var(--site-max);
  margin: 0 auto;
  padding: 0 var(--site-gutter) var(--site-s9);
}

/* ---------- 内容块：用一条上边线分段，不用卡片包起来 ---------- */
.block { padding: var(--site-s8) 0; border-top: 1px solid var(--site-line); }
.block:first-child { border-top: none; padding-top: var(--site-s6); }

.block-title {
  font-family: var(--site-serif);
  font-size: var(--site-fs-h3);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--site-pine);
  margin: 0;
}
.block-sub {
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
  margin: var(--site-s3) 0 0;
  max-width: 40em;
}

.menu-teaser-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--site-s5);
  flex-wrap: wrap;
  margin-bottom: var(--site-s5);
}
.block-cta {
  display: inline-flex; align-items: center; gap: 7px;
  background: none; border: none; padding: 0;
  font-family: inherit; font-size: var(--site-fs-small);
  letter-spacing: 0.04em; color: var(--site-pine); cursor: pointer;
}
.cta-arrow { transition: transform var(--site-dur) var(--site-ease); }
.block-cta:hover .cta-arrow { transform: translateX(4px); }

/* ---------- 招牌菜：清单式，不做成一堆小卡片 ---------- */
.dish-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 0 var(--site-s7);
}
.dish-card {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--site-s4);
  padding: var(--site-s4) 0;
  border-bottom: 1px solid var(--site-line);
}
.dish-card h4 {
  font-size: var(--site-fs-body);
  font-weight: 500;
  color: var(--site-ink);
  margin: 0;
  letter-spacing: 0.02em;
}
.dish-meta { display: flex; align-items: baseline; gap: var(--site-s3); flex-shrink: 0; }
.dish-meta > span:first-child {
  font-size: var(--site-fs-micro);
  letter-spacing: 0.1em;
  color: var(--site-brass);
}
.price { font-family: var(--site-serif); font-size: var(--site-fs-lead); color: var(--site-pine); }

/* ---------- 环境 ---------- */
.env-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--site-s3);
  margin-top: var(--site-s5);
}
.env-grid img {
  width: 100%;
  aspect-ratio: 3 / 4;
  object-fit: cover;
  border-radius: var(--site-radius);
  display: block;
}

/* ---------- 套餐 ---------- */
.pkg-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--site-s4);
  margin-top: var(--site-s5);
}
.pkg-card {
  background: var(--site-surface);
  border: 1px solid var(--site-line);
  border-radius: var(--site-radius);
  padding: var(--site-s5);
}
.pkg-card h4 {
  font-family: var(--site-serif);
  font-size: var(--site-fs-lead);
  font-weight: 600;
  color: var(--site-pine);
  margin: 0 0 var(--site-s3);
  letter-spacing: 0.03em;
}
.pkg-price { font-family: var(--site-serif); font-size: 22px; color: var(--site-pine); margin: 0; }
.pkg-original {
  font-family: var(--site-sans);
  font-size: var(--site-fs-small);
  color: var(--site-ink-3);
  text-decoration: line-through;
  text-decoration-color: var(--site-line-strong);
  margin-left: 6px;
}
.pkg-meta { font-size: var(--site-fs-caption); color: var(--site-ink-3); margin: 6px 0 0; letter-spacing: 0.04em; }

.block-more { margin-top: var(--site-s5); }
.block-more a {
  display: inline-flex; align-items: center; gap: 8px;
  font-size: var(--site-fs-small);
  color: var(--site-pine);
  cursor: pointer;
  padding-bottom: 5px;
  border-bottom: 1px solid var(--site-line-strong);
  transition: border-color var(--site-dur) var(--site-ease);
}
.block-more a:hover { border-bottom-color: var(--site-brass); }
.block-more a :deep(.site-icon) { transition: transform var(--site-dur) var(--site-ease); }
.block-more a:hover :deep(.site-icon) { transform: translateX(4px); }

/* ---------- 留资表单 ---------- */
.booking-form { margin-top: var(--site-s5); max-width: 680px; }
.form-row { display: flex; gap: var(--site-s3); margin-bottom: var(--site-s3); flex-wrap: wrap; }
.form-row > * { flex: 1 1 180px; }
.booking-form input,
.booking-form textarea {
  width: 100%;
  font-family: inherit;
  font-size: var(--site-fs-body);
  color: var(--site-ink);
  background: var(--site-surface);
  border: 1px solid var(--site-line-strong);
  border-radius: var(--site-radius);
  padding: 13px 14px;
  transition: border-color var(--site-dur) var(--site-ease);
}
.booking-form input::placeholder,
.booking-form textarea::placeholder { color: var(--site-ink-3); }
.booking-form input:focus,
.booking-form textarea:focus { outline: none; border-color: var(--site-pine); }
.booking-form textarea { min-height: 110px; resize: vertical; line-height: var(--site-lh-normal); }
.submit-btn { margin-top: var(--site-s4); padding: 14px 32px; font-size: var(--site-fs-body); }
.submit-btn:disabled { opacity: 0.55; cursor: default; }

.submitted-summary {
  margin-top: var(--site-s4);
  padding: var(--site-s4) var(--site-s5);
  background: var(--site-surface-2);
  border-left: 2px solid var(--site-brass);
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
}

.loading {
  color: var(--site-ink-3);
  font-size: var(--site-fs-body);
  line-height: var(--site-lh-loose);
  padding: var(--site-s6) 0;
  margin: 0;
}
.page-loading { text-align: center; padding: var(--site-s9) var(--site-gutter); }

@media (max-width: 960px) {
  .dish-grid { grid-template-columns: 1fr; gap: 0; }
  .pkg-grid { grid-template-columns: 1fr; }
  .env-grid { grid-template-columns: repeat(2, 1fr); }
  .block { padding: var(--site-s7) 0; }
  .store-meta-row { gap: var(--site-s3); flex-direction: column; }
}
</style>

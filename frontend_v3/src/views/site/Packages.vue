<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', en: 'Home', to: '/' }, { label: '宴会套餐', en: 'Banquets & Celebrations' }]" />

    <section class="page-hero">
      <p class="page-eyebrow">Celebrations & Banquets</p>
      <h1 class="page-title">宴会套餐 · 婚宴与庆典</h1>
      <p class="page-desc">为婚宴、寿宴、升学、商务与满月等场合量身而备，十道菜起，现点现做</p>
    </section>

    <section class="page-body">
      <div class="cat-tabs">
        <button :class="{ active: activeCat === 'all' }" @click="activeCat = 'all'">全部套餐</button>
        <button :class="{ active: activeCat === 'CELEBRATION' }" @click="activeCat = 'CELEBRATION'">婚宴与庆典</button>
        <button :class="{ active: activeCat === 'BUSINESS' }" @click="activeCat = 'BUSINESS'">商务宴请</button>
        <button :class="{ active: activeCat === 'GRADUATION' }" @click="activeCat = 'GRADUATION'">升学宴</button>
      </div>

      <div v-if="loading" class="loading">套餐加载中...</div>
      <div v-else-if="filteredPackages.length === 0" class="loading">套餐信息完善中，敬请期待</div>
      <div v-else class="pkg-grid">
        <div v-for="p in filteredPackages" :key="p.packageId" class="pkg-card">
          <div class="pkg-image placeholder-block">
            <span class="placeholder-label">{{ occasionLabel(p.occasionType) }}</span>
          </div>
          <div class="pkg-body">
            <h3>{{ p.packageName }}</h3>
            <p class="pkg-desc">{{ p.description }}</p>
            <div class="pkg-price-row">
              <span class="pkg-price">¥{{ formatPrice(p.price) }}</span>
              <span v-if="p.originalPrice" class="pkg-original">¥{{ formatPrice(p.originalPrice) }}</span>
            </div>
            <p class="pkg-meta">{{ p.minGuests }}-{{ p.maxGuests }}人 · {{ p.dishCount }}道菜</p>
            <button class="btn-gold" @click="$router.push('/stores')">了解更多 · 预定</button>
          </div>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import request from '@/utils/request'

const packages = ref([])
const loading = ref(true)
const activeCat = ref('all')

const CELEBRATION_TYPES = ['WEDDING', 'BIRTHDAY_ELDER', 'BABY_MOON']

const occasionLabels = {
  WEDDING: '婚宴',
  BIRTHDAY_ELDER: '寿宴',
  GRADUATION: '升学宴',
  BUSINESS: '商务宴请',
  BABY_MOON: '满月宴'
}
function occasionLabel(t) {
  return occasionLabels[t] || '宴会套餐'
}

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

const filteredPackages = computed(() => {
  if (activeCat.value === 'all') return packages.value
  if (activeCat.value === 'CELEBRATION') return packages.value.filter(p => CELEBRATION_TYPES.includes(p.occasionType))
  return packages.value.filter(p => p.occasionType === activeCat.value)
})

onMounted(async () => {
  loading.value = true
  try {
    const res = await request.get('/api/public/packages', { params: { storeId: 1 } })
    packages.value = (res.data || []).map(p => ({
      packageId: p.package_id ?? p.packageId,
      packageName: p.package_name ?? p.packageName,
      price: p.price,
      originalPrice: p.original_price ?? p.originalPrice,
      occasionType: p.occasion_type ?? p.occasionType,
      minGuests: p.min_guests ?? p.minGuests,
      maxGuests: p.max_guests ?? p.maxGuests,
      dishCount: p.dish_count ?? p.dishCount,
      description: p.description
    }))
  } catch (e) {
    packages.value = []
  } finally {
    loading.value = false
  }
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
.page-hero { max-width: 1200px; margin: 0 auto; padding: 40px 32px 8px; text-align: center; }
.page-eyebrow { font-size: 13px; letter-spacing: 3px; color: var(--gold); margin: 0 0 10px; font-weight: 600; }
.page-title { font-size: 32px; font-weight: 700; color: var(--forest); margin: 0 0 12px; }
.page-desc { font-size: 14px; color: var(--muted); margin: 0; }

.page-body { max-width: 1200px; margin: 0 auto; padding: 40px 32px 100px; }
.cat-tabs { display: flex; justify-content: center; gap: 12px; margin-bottom: 40px; flex-wrap: wrap; }
.cat-tabs button {
  background: #fff; border: 1px solid #DDD3B8; color: var(--muted);
  padding: 10px 24px; border-radius: 2px; font-size: 14px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s;
}
.cat-tabs button.active { background: var(--forest); border-color: var(--forest); color: #fff; }

.loading { text-align: center; color: var(--muted); padding: 60px 0; }
.pkg-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 28px; }
.pkg-card { background: #fff; border-radius: 6px; overflow: hidden; box-shadow: 0 4px 20px rgba(0,0,0,0.06); }
.placeholder-block {
  background: linear-gradient(135deg, #EDE7D9 0%, #E3DBC8 50%, #D9CFB5 100%);
  display: flex; align-items: center; justify-content: center;
}
.placeholder-label { color: #9C8F6E; font-size: 14px; letter-spacing: 2px; }
.pkg-image { height: 180px; }
.pkg-body { padding: 24px; }
.pkg-body h3 { font-size: 18px; font-weight: 700; color: var(--forest); margin: 0 0 8px; }
.pkg-desc { font-size: 13px; color: var(--muted); line-height: 1.6; margin: 0 0 16px; min-height: 20px; }
.pkg-price-row { display: flex; align-items: baseline; gap: 8px; margin-bottom: 6px; }
.pkg-price { font-size: 22px; font-weight: 700; color: var(--forest); }
.pkg-original { font-size: 13px; color: var(--muted); text-decoration: line-through; }
.pkg-meta { font-size: 12px; color: var(--muted); margin: 0 0 18px; }
.btn-gold {
  width: 100%; background: var(--gold); border: 1px solid var(--gold); color: #fff;
  padding: 10px 0; border-radius: 2px; font-size: 13px; letter-spacing: 1px; cursor: pointer;
}
.btn-gold:hover { background: #A17E48; }

@media (max-width: 960px) {
  .pkg-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

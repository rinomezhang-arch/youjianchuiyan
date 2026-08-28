<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', en: 'Home', to: '/' }, { label: '臻选菜品', en: 'Signature Dishes' }]" />

    <section class="page-hero">
      <p class="page-eyebrow">Signature Dishes</p>
      <h1 class="page-title">臻选菜品</h1>
      <p class="page-desc">现点现做 · 时令为先 · 徽风粤味，不做预制菜</p>
    </section>

    <section class="page-body">
      <div class="store-tabs-row">
        <div class="store-tabs">
          <button v-for="s in stores" :key="s.storeId" :class="{ active: activeStoreId === s.storeId }" @click="switchStore(s.storeId)">
            {{ s.storeName }}
          </button>
        </div>
        <button class="btn-gold order-cta" @click="$router.push(`/stores/${activeStoreId}/order`)">我要点菜 · Order Now</button>
      </div>

      <div v-if="loading" class="loading">菜单加载中...</div>
      <div v-else-if="dishes.length === 0" class="loading">菜单信息完善中，敬请期待</div>
      <div v-else class="dish-grid">
        <div v-for="(d, i) in dishes" :key="i" class="dish-card">
          <div class="dish-image placeholder-block">
            <span class="placeholder-label">菜品实拍待补</span>
          </div>
          <div class="dish-info">
            <h4 class="dish-name">{{ d.dishName }}</h4>
            <p v-if="d.dishNameEn" class="dish-name-en">{{ d.dishNameEn }}</p>
            <p v-if="d.dishIntro" class="dish-intro">{{ d.dishIntro }}</p>
            <div class="dish-meta">
              <span class="dish-category">{{ d.dishCategory || '精选' }}</span>
              <span class="dish-price">¥{{ formatPrice(d.salePrice) }}</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import request from '@/utils/request'

const stores = ref([])
const activeStoreId = ref(1)
const dishes = ref([])
const loading = ref(true)

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

async function loadStores() {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = (res.data || []).map(s => ({
      storeId: s.store_id ?? s.storeId,
      storeName: s.store_name ?? s.storeName
    }))
    if (stores.value.length) activeStoreId.value = stores.value[0].storeId
  } catch (e) {
    stores.value = []
  }
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await request.get('/api/public/menu/preview', { params: { storeId: activeStoreId.value, limit: 12 } })
    dishes.value = (res.data || []).map(d => ({
      dishName: d.dish_name ?? d.dishName,
      dishNameEn: d.dish_name_en ?? d.dishNameEn,
      dishCategory: d.dish_category ?? d.dishCategory,
      dishIntro: d.dish_intro ?? d.dishIntro,
      salePrice: d.sale_price ?? d.salePrice
    }))
  } catch (e) {
    dishes.value = []
  } finally {
    loading.value = false
  }
}

function switchStore(id) {
  activeStoreId.value = id
  loadDishes()
}

onMounted(async () => {
  await loadStores()
  loadDishes()
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
.page-title { font-size: 34px; font-weight: 700; color: var(--forest); margin: 0 0 12px; }
.page-desc { font-size: 14px; color: var(--muted); margin: 0; }

.page-body { max-width: 1200px; margin: 0 auto; padding: 40px 32px 100px; }
.store-tabs-row {
  display: flex; justify-content: center; align-items: center; gap: 24px;
  margin-bottom: 40px; flex-wrap: wrap;
}
.store-tabs { display: flex; gap: 12px; }
.store-tabs button {
  background: #fff; border: 1px solid #DDD3B8; color: var(--muted);
  padding: 10px 28px; border-radius: 2px; font-size: 14px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s;
}
.store-tabs button.active { background: var(--forest); border-color: var(--forest); color: #fff; }
.btn-gold {
  background: var(--gold); border: 1px solid var(--gold); color: #fff;
  padding: 10px 24px; border-radius: 2px; font-size: 13.5px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s; white-space: nowrap;
}
.btn-gold:hover { background: #A17E48; }

.loading { text-align: center; color: var(--muted); padding: 60px 0; }
.dish-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.dish-card { background: #fff; border-radius: 4px; overflow: hidden; box-shadow: 0 2px 16px rgba(0,0,0,0.05); }
.placeholder-block {
  background: linear-gradient(135deg, #EDE7D9 0%, #E3DBC8 50%, #D9CFB5 100%);
  display: flex; align-items: center; justify-content: center;
}
.placeholder-label { color: #9C8F6E; font-size: 12px; }
.dish-image { height: 180px; }
.dish-info { padding: 20px; }
.dish-name { font-size: 17px; font-weight: 700; color: var(--forest); margin: 0 0 2px; }
.dish-name-en { font-size: 11px; color: var(--muted); margin: 0 0 8px; letter-spacing: 0.5px; }
.dish-intro { font-size: 12px; color: var(--muted); line-height: 1.6; margin: 0 0 12px; }
.dish-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.dish-category { font-size: 12px; color: var(--gold); background: rgba(184,147,90,0.1); padding: 3px 10px; border-radius: 2px; }
.dish-price { font-size: 17px; font-weight: 700; color: var(--forest); }

@media (max-width: 960px) {
  .dish-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

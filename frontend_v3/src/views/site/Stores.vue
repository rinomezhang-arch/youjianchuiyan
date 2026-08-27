<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', en: 'Home', to: '/' }, { label: '门店选择', en: 'Our Restaurants' }]" />

    <section class="page-hero">
      <p class="page-eyebrow">Our Restaurants</p>
      <h1 class="page-title">门店选择</h1>
      <p class="page-desc">选择门店，查看该店菜单、环境与套餐，直接预定</p>
    </section>

    <section class="page-body">
      <div v-if="loading" class="loading">门店信息加载中...</div>
      <div v-else class="store-grid">
        <div v-for="s in stores" :key="s.storeId" class="store-card" @click="$router.push(`/stores/${s.storeId}`)">
          <div class="store-image placeholder-block">
            <span class="placeholder-label">{{ s.storeName }} 门店实景待补</span>
          </div>
          <div class="store-info">
            <h3 class="store-name">{{ s.storeName }}</h3>
            <p class="store-detail"><span class="store-icon">📍</span>{{ s.address }}</p>
            <p class="store-detail"><span class="store-icon">🕐</span>{{ s.businessHours }}</p>
            <span class="store-link">进入详情页 →</span>
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
const loading = ref(true)

onMounted(async () => {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = (res.data || []).map(s => ({
      storeId: s.store_id ?? s.storeId,
      storeName: s.store_name ?? s.storeName,
      address: s.address,
      phone: s.phone,
      businessHours: s.business_hours ?? s.businessHours
    }))
  } catch (e) {
    stores.value = []
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
.page-title { font-size: 34px; font-weight: 700; color: var(--forest); margin: 0 0 12px; }
.page-desc { font-size: 14px; color: var(--muted); margin: 0; }

.page-body { max-width: 1200px; margin: 0 auto; padding: 40px 32px 100px; }
.loading { text-align: center; color: var(--muted); padding: 60px 0; }
.store-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 32px; }
.store-card { background: #fff; border: 1px solid #EDE7D9; border-radius: 4px; overflow: hidden; cursor: pointer; transition: box-shadow 0.2s; }
.store-card:hover { box-shadow: 0 12px 32px rgba(0,0,0,0.1); }
.placeholder-block {
  background: linear-gradient(135deg, #EDE7D9 0%, #E3DBC8 50%, #D9CFB5 100%);
  display: flex; align-items: center; justify-content: center;
}
.placeholder-label { color: #9C8F6E; font-size: 13px; }
.store-image { height: 220px; }
.store-info { padding: 28px; }
.store-name { font-size: 22px; font-weight: 700; color: var(--forest); margin: 0 0 16px; }
.store-detail { font-size: 14px; color: #4A4A44; margin: 0 0 10px; display: flex; gap: 8px; align-items: flex-start; }
.store-icon { flex-shrink: 0; }
.store-link { display: inline-block; margin-top: 12px; font-size: 13px; color: var(--forest); font-weight: 600; }

@media (max-width: 960px) {
  .store-grid { grid-template-columns: 1fr; }
}
</style>

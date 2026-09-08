<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', to: '/' }, { label: '臻选菜品' }]" />

    <header class="site-hero">
      <p class="site-eyebrow">Signature Dishes</p>
      <h1 class="site-title">臻选菜品</h1>
      <p class="site-lede">现点现做，时令为先，徽风粤味。不做预制菜。</p>
      <hr class="site-rule" />
    </header>

    <main class="page-body">
      <div class="toolbar">
        <div class="store-tabs" role="tablist">
          <button
            v-for="s in stores"
            :key="s.storeId"
            role="tab"
            :aria-selected="activeStoreId === s.storeId"
            :class="{ active: activeStoreId === s.storeId }"
            @click="switchStore(s.storeId)"
          >
            {{ s.storeName }}
          </button>
        </div>
        <button class="site-btn site-btn--primary" @click="$router.push(`/stores/${activeStoreId}/order`)">
          <SiteIcon name="basket" :size="15" />
          我要点菜
        </button>
      </div>

      <div v-if="loading" class="dish-grid">
        <div v-for="n in 6" :key="n" class="dish-card is-skeleton">
          <div class="dish-image sk-block"></div>
          <div class="dish-info">
            <div class="sk-line sk-line--title"></div>
            <div class="sk-line sk-line--short"></div>
          </div>
        </div>
      </div>

      <p v-else-if="dishes.length === 0" class="empty">这家店的菜单正在整理，先看看另一家，或直接致电门店询问当日时令。</p>

      <div v-else class="dish-grid">
        <article v-for="(d, i) in dishes" :key="i" class="dish-card site-card">
          <div class="dish-image site-placeholder">
            <span class="dish-mark">{{ firstChar(d.dishName) }}</span>
          </div>
          <div class="dish-info">
            <h2 class="dish-name">{{ d.dishName }}</h2>
            <p v-if="d.dishNameEn" class="dish-name-en">{{ d.dishNameEn }}</p>
            <p v-if="d.dishIntro" class="dish-intro">{{ d.dishIntro }}</p>
            <div class="dish-meta">
              <span class="dish-category">{{ d.dishCategory || '精选' }}</span>
              <span class="dish-price"><i>¥</i>{{ formatPrice(d.salePrice) }}</span>
            </div>
          </div>
        </article>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'
import SiteIcon from '@/components/site/SiteIcon.vue'
import request from '@/utils/request'

const stores = ref([])
const activeStoreId = ref(1)
const dishes = ref([])
const loading = ref(true)

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

/** 没有实拍图时，用菜名的头一个字做版位标记，比"实拍待补"体面得多。 */
function firstChar(name) {
  return typeof name === 'string' && name.length ? name.trim().charAt(0) : '菜'
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
/*
  这一版的取舍：

  · 门店切换从"两个胶囊按钮"改成下划线式分段。胶囊 + 选中填充深色，
    在一屏里会跟主按钮抢注意力；下划线只用一条 1.5px 的线表示"你在这一栏"，安静得多。
  · "我要点菜"从金色实底改成墨绿实底。金色铺满一个按钮就是塑料感的来源，
    金色在本站只用来画线。
  · 菜品卡去掉四周的柔和投影。那种 0 2px 16px 的浅影会让每张卡都像浮在一层塑料膜上，
    换成 1px 发丝边，hover 时才轻轻抬起来。
  · 价格用衬线体，并把 ¥ 缩小。中文页面里数字用衬线立刻显得讲究，
    货币符号跟数字同样大反而抢戏。
*/

.page-body {
  max-width: var(--site-max);
  margin: 0 auto;
  padding: var(--site-s6) var(--site-gutter) var(--site-s9);
}

/* ---------- 工具条 ---------- */
.toolbar {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--site-s5);
  flex-wrap: wrap;
  margin-bottom: var(--site-s7);
  padding-bottom: var(--site-s4);
  border-bottom: 1px solid var(--site-line);
}
.store-tabs { display: flex; gap: var(--site-s6); }
.store-tabs button {
  position: relative;
  background: none;
  border: none;
  padding: 0 0 var(--site-s3);
  margin-bottom: -17px; /* 让下划线压在工具条那条分隔线上 */
  font-family: inherit;
  font-size: var(--site-fs-lead);
  letter-spacing: 0.04em;
  color: var(--site-ink-3);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.store-tabs button:hover { color: var(--site-ink); }
.store-tabs button.active { color: var(--site-pine); }
.store-tabs button.active::after {
  content: '';
  position: absolute;
  left: 0; right: 0; bottom: 0;
  height: 1.5px;
  background: var(--site-pine);
}

/* ---------- 菜品网格 ---------- */
.dish-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--site-s5);
}
.dish-card { overflow: hidden; }
.dish-image { height: 190px; }
.dish-mark {
  font-family: var(--site-serif);
  font-size: 52px;
  color: rgba(30, 58, 47, 0.13);
  user-select: none;
}

.dish-info { padding: var(--site-s5); }
.dish-name {
  font-family: var(--site-serif);
  font-size: var(--site-fs-lead);
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--site-pine);
  margin: 0;
}
.dish-name-en {
  font-size: var(--site-fs-micro);
  color: var(--site-ink-3);
  letter-spacing: 0.06em;
  margin: 5px 0 0;
}
.dish-intro {
  font-size: var(--site-fs-small);
  color: var(--site-ink-2);
  line-height: var(--site-lh-loose);
  margin: var(--site-s3) 0 0;
  /* 简介长短不一，统一压成两行，卡片高度才齐 */
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.dish-meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: var(--site-s3);
  margin-top: var(--site-s4);
  padding-top: var(--site-s3);
  border-top: 1px solid var(--site-line);
}
.dish-category {
  font-size: var(--site-fs-micro);
  letter-spacing: 0.12em;
  color: var(--site-brass);
}
.dish-price {
  font-family: var(--site-serif);
  font-size: 20px;
  color: var(--site-pine);
  letter-spacing: 0.02em;
}
.dish-price i { font-style: normal; font-size: 13px; margin-right: 1px; opacity: 0.65; }

.empty {
  text-align: center;
  color: var(--site-ink-3);
  font-size: var(--site-fs-body);
  line-height: var(--site-lh-loose);
  padding: var(--site-s9) 0;
  margin: 0;
}

/* ---------- 骨架屏 ---------- */
.is-skeleton { pointer-events: none; }
.sk-block, .sk-line { background: var(--site-surface-2); position: relative; overflow: hidden; }
.sk-block { height: 190px; }
.sk-line { height: 12px; border-radius: 2px; }
.sk-line--title { height: 18px; width: 55%; margin-bottom: 14px; }
.sk-line--short { width: 72%; }
.sk-block::after, .sk-line::after {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.7), transparent);
  transform: translateX(-100%);
  animation: sk-sweep 1.4s infinite;
}
@keyframes sk-sweep { to { transform: translateX(100%); } }
@media (prefers-reduced-motion: reduce) {
  .sk-block::after, .sk-line::after { animation: none; }
}

@media (max-width: 960px) {
  .dish-grid { grid-template-columns: repeat(2, 1fr); gap: var(--site-s4); }
  .dish-image, .sk-block { height: 148px; }
  .dish-info { padding: var(--site-s4); }
  .toolbar { align-items: stretch; flex-direction: column; }
  .store-tabs { margin-bottom: 0; }
  .store-tabs button { margin-bottom: 0; }
}
</style>

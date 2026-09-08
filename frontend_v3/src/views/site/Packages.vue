<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', to: '/' }, { label: '宴会套餐' }]" />

    <header class="site-hero">
      <p class="site-eyebrow">Celebrations &amp; Banquets</p>
      <h1 class="site-title">宴会套餐</h1>
      <p class="site-lede">婚宴、寿宴、升学、商务与满月，各按场合备菜。十道起，现点现做。</p>
      <hr class="site-rule" />
    </header>

    <section class="page-body">
      <div class="cat-tabs">
        <button :class="{ active: activeCat === 'all' }" @click="activeCat = 'all'">全部套餐</button>
        <button :class="{ active: activeCat === 'CELEBRATION' }" @click="activeCat = 'CELEBRATION'">婚宴与庆典</button>
        <button :class="{ active: activeCat === 'BUSINESS' }" @click="activeCat = 'BUSINESS'">商务宴请</button>
        <button :class="{ active: activeCat === 'GRADUATION' }" @click="activeCat = 'GRADUATION'">升学宴</button>
      </div>

      <div v-if="loading" class="pkg-grid">
        <div v-for="n in 3" :key="n" class="pkg-card is-skeleton">
          <div class="pkg-image sk-block"></div>
          <div class="pkg-body">
            <div class="sk-line sk-line--title"></div>
            <div class="sk-line"></div>
            <div class="sk-line sk-line--short"></div>
          </div>
        </div>
      </div>
      <p v-else-if="filteredPackages.length === 0" class="empty">这一类套餐正在整理，先看看别的分类，或直接致电门店定制。</p>
      <div v-else class="pkg-grid">
        <div v-for="p in filteredPackages" :key="p.packageId" class="pkg-card">
          <div class="pkg-image site-placeholder">
            <span class="pkg-occasion">{{ occasionLabel(p.occasionType) }}</span>
          </div>
          <div class="pkg-body">
            <h3>{{ p.packageName }}</h3>
            <p class="pkg-desc">{{ p.description }}</p>
            <div class="pkg-price-row">
              <span class="pkg-price">¥{{ formatPrice(p.price) }}</span>
              <span v-if="p.originalPrice" class="pkg-original">¥{{ formatPrice(p.originalPrice) }}</span>
            </div>
            <p class="pkg-meta">{{ p.minGuests }}-{{ p.maxGuests }}人 · {{ p.dishCount }}道菜</p>
            <button class="pkg-cta" @click="$router.push('/stores')">
              选门店预定
              <SiteIcon name="arrow-right" :size="15" class="cta-arrow" />
            </button>
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
import SiteIcon from '@/components/site/SiteIcon.vue'
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
/*
  分类切换原先是四个胶囊按钮，选中填成深绿。四个实心块并排，
  比下面真正要看的套餐卡还抢眼。改成下划线式：只用一条线说明"你在这一栏"。
  卡片上的"了解更多 · 预定"也换掉了——中间那个间隔点是装饰性的，
  按钮上的字越少越清楚，直接写"选门店预定"，客人知道下一步会发生什么。
*/

.page-body {
  max-width: var(--site-max);
  margin: 0 auto;
  padding: var(--site-s6) var(--site-gutter) var(--site-s9);
}

.cat-tabs {
  display: flex;
  gap: var(--site-s6);
  flex-wrap: wrap;
  margin-bottom: var(--site-s7);
  padding-bottom: var(--site-s4);
  border-bottom: 1px solid var(--site-line);
}
.cat-tabs button {
  position: relative;
  background: none; border: none; padding: 0 0 var(--site-s3);
  margin-bottom: -17px;
  font-family: inherit;
  font-size: var(--site-fs-body);
  letter-spacing: 0.04em;
  color: var(--site-ink-3);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.cat-tabs button:hover { color: var(--site-ink); }
.cat-tabs button.active { color: var(--site-pine); }
.cat-tabs button.active::after {
  content: '';
  position: absolute; left: 0; right: 0; bottom: 0;
  height: 1.5px; background: var(--site-pine);
}

.pkg-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--site-s5);
}
.pkg-card {
  background: var(--site-surface);
  border: 1px solid var(--site-line);
  border-radius: var(--site-radius);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  transition: border-color var(--site-dur) var(--site-ease),
              box-shadow var(--site-dur) var(--site-ease),
              transform var(--site-dur) var(--site-ease);
}
.pkg-card:hover {
  border-color: var(--site-line-strong);
  box-shadow: var(--site-lift);
  transform: translateY(-2px);
}

.pkg-image { height: 190px; }
.pkg-occasion {
  font-family: var(--site-serif);
  font-size: 26px;
  letter-spacing: 0.24em;
  color: rgba(30, 58, 47, 0.16);
  user-select: none;
  padding-left: 0.24em;
}

.pkg-body { padding: var(--site-s5); display: flex; flex-direction: column; flex: 1; }
.pkg-body h3 {
  font-family: var(--site-serif);
  font-size: var(--site-fs-lead);
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--site-pine);
  margin: 0;
}
.pkg-desc {
  font-size: var(--site-fs-small);
  color: var(--site-ink-2);
  line-height: var(--site-lh-loose);
  margin: var(--site-s3) 0 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.pkg-price-row {
  display: flex; align-items: baseline; gap: 10px;
  margin-top: var(--site-s4);
  padding-top: var(--site-s3);
  border-top: 1px solid var(--site-line);
}
.pkg-price {
  font-family: var(--site-serif);
  font-size: 24px;
  color: var(--site-pine);
  letter-spacing: 0.02em;
}
/* 划线价：只需要"原来更贵"这一个信息，不必用红色喊出来 */
.pkg-original {
  font-size: var(--site-fs-small);
  color: var(--site-ink-3);
  text-decoration: line-through;
  text-decoration-color: var(--site-line-strong);
}
.pkg-meta {
  font-size: var(--site-fs-caption);
  color: var(--site-ink-3);
  letter-spacing: 0.04em;
  margin: 6px 0 0;
}
/*
  六张卡片各放一个深绿实心按钮，一屏就是六个色块，比要看的菜名还抢眼。
  目录页的每一项都是平等的，不该有六个"主行动"。改成带箭头的文字入口，
  和门店卡的"进入门店"是同一套语汇。margin-top:auto 把它压到卡片底部，
  简介长短不一时按钮也能对齐成一条线。
*/
.pkg-cta {
  margin-top: auto;
  padding: var(--site-s4) 0 0;
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 7px;
  background: none;
  border: none;
  font-family: inherit;
  font-size: var(--site-fs-small);
  letter-spacing: 0.04em;
  color: var(--site-pine);
  cursor: pointer;
}
.cta-arrow { transition: transform var(--site-dur) var(--site-ease); }
.pkg-card:hover .cta-arrow { transform: translateX(4px); }

.empty {
  text-align: center;
  color: var(--site-ink-3);
  font-size: var(--site-fs-body);
  line-height: var(--site-lh-loose);
  padding: var(--site-s9) 0;
  margin: 0;
}

.is-skeleton { pointer-events: none; }
.sk-block, .sk-line { background: var(--site-surface-2); position: relative; overflow: hidden; }
.sk-block { height: 190px; }
.sk-line { height: 12px; border-radius: 2px; margin-bottom: 12px; }
.sk-line--title { height: 18px; width: 55%; margin-bottom: 16px; }
.sk-line--short { width: 68%; }
.sk-block::after, .sk-line::after {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.7), transparent);
  transform: translateX(-100%);
  animation: sk-sweep 1.4s infinite;
}
@keyframes sk-sweep { to { transform: translateX(100%); } }
@media (prefers-reduced-motion: reduce) { .sk-block::after, .sk-line::after { animation: none; } }

@media (max-width: 960px) {
  .pkg-grid { grid-template-columns: 1fr; }
  .pkg-image, .sk-block { height: 170px; }
  .cat-tabs { gap: var(--site-s5); }
  .cat-tabs button { font-size: var(--site-fs-small); }
}
</style>

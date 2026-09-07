<template>
  <div class="m-home">
    <div class="hero">
      <video
        v-if="heroReady"
        ref="videoRef"
        class="hero-video"
        :src="heroClips[heroIdx]"
        autoplay
        muted
        playsinline
        @ended="nextClip"
      ></video>
      <div class="hero-scrim"></div>
      <div class="hero-content">
        <div class="hero-brand">
          <img src="/logo.png" class="hero-logo" alt="logo" />
          <div>
            <div class="hero-title">又见炊烟私房菜</div>
            <div class="hero-sub">YOUJIANCHUIYAN · PRIVATE KITCHEN</div>
          </div>
        </div>
        <p class="hero-line">私房手艺 · 本地时令食材</p>
        <button class="hero-cta" @click="$router.push('/m/book')">立即预定 · Book a Table</button>
      </div>
    </div>

    <section class="m-section">
      <div class="m-section-head">
        <span class="m-section-title">臻选招牌</span>
        <span class="m-section-more" @click="$router.push('/m/menu')">查看全部 ›</span>
      </div>
      <div class="dish-scroll">
        <div v-for="d in dishes" :key="d.dish_name" class="dish-card">
          <div class="dish-photo" :style="photoStyle(d)">
            <span v-if="!d._photo" class="dish-photo-fallback">{{ d.dish_name }}</span>
          </div>
          <div class="dish-name">{{ d.dish_name }}</div>
          <div class="dish-price">¥{{ formatPrice(d.sale_price) }}</div>
        </div>
        <div v-if="!dishes.length" class="dish-loading">菜品加载中…</div>
      </div>
    </section>

    <section class="m-section">
      <div class="m-section-head">
        <span class="m-section-title">门店与地址</span>
      </div>
      <div v-for="s in stores" :key="s.store_id" class="store-card">
        <div class="store-info">
          <div class="store-name">{{ s.store_name }}</div>
          <div class="store-addr">{{ s.address }}</div>
          <div class="store-hours">营业时间 {{ s.business_hours }}</div>
        </div>
        <a class="store-call" :href="`tel:${s.phone}`" @click.stop>拨打</a>
      </div>
    </section>

    <section class="m-section banquet-card" @click="$router.push('/m/packages')">
      <img src="/site-photos/banquet-hall-grand.jpg" class="banquet-bg" alt="banquet" />
      <div class="banquet-overlay">
        <div class="banquet-title">宴会套餐 · 婚宴与庆典</div>
        <div class="banquet-sub">Banquets & Celebrations ›</div>
      </div>
    </section>

    <div class="m-footer-note">又见炊烟私房菜 · 二十余年手艺传承</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import request from '@/utils/request'

const heroClips = ['/site-videos/exterior-1.mp4', '/site-videos/exterior-2.mp4']
const heroIdx = ref(0)
const heroReady = ref(true)
const videoRef = ref(null)

function nextClip() {
  heroIdx.value = (heroIdx.value + 1) % heroClips.length
}

const dishes = ref([])
const stores = ref([])

const REAL_PHOTOS = {
  '剁椒鱼头': '/dish-photos/duojiao-yutou.jpg',
  '土锅黑鱼': '/dish-photos/tuguo-heiyu.jpg',
  '老豆腐蒸腊肉': '/dish-photos/laodoufu-larou.jpg'
}

function photoStyle(d) {
  const url = REAL_PHOTOS[d.dish_name]
  d._photo = !!url
  if (url) return { backgroundImage: `url(${url})` }
  return {}
}

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

async function loadDishes() {
  try {
    const res = await request.get('/api/public/menu/preview', { params: { storeId: 1, limit: 10 } })
    dishes.value = res.data || []
  } catch (e) {
    dishes.value = []
  }
}

async function loadStores() {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = res.data || []
  } catch (e) {
    stores.value = []
  }
}

onMounted(() => {
  loadDishes()
  loadStores()
})
</script>

<style scoped>
.m-home {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #8A8478;
}

.hero {
  position: relative;
  height: 46vh;
  min-height: 300px;
  overflow: hidden;
  background: var(--forest);
}
.hero-video {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.hero-scrim {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(8,15,11,0.55) 0%, rgba(8,15,11,0.25) 45%, rgba(8,15,11,0.75) 100%);
}
.hero-content {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 16px 18px 22px;
  color: #fff;
}
.hero-brand { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.hero-logo { width: 34px; height: 34px; object-fit: contain; filter: drop-shadow(0 1px 4px rgba(0,0,0,0.4)); }
.hero-title { font-size: 18px; font-weight: 700; letter-spacing: 1px; }
.hero-sub { font-size: 8.5px; letter-spacing: 1.5px; color: rgba(255,255,255,0.75); margin-top: 2px; }
.hero-line { font-size: 12.5px; color: rgba(255,255,255,0.9); margin: 0 0 12px; }
.hero-cta {
  align-self: flex-start;
  background: var(--gold);
  color: #fff;
  border: none;
  padding: 11px 20px;
  border-radius: 24px;
  font-size: 13.5px;
  font-weight: 600;
  letter-spacing: 0.5px;
}

.m-section { padding: 18px 16px 4px; }
.m-section-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.m-section-title { font-size: 15.5px; font-weight: 700; color: var(--forest); }
.m-section-more { font-size: 11.5px; color: var(--muted); }

.dish-scroll { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 4px; -webkit-overflow-scrolling: touch; }
.dish-scroll::-webkit-scrollbar { display: none; }
.dish-card { flex: 0 0 auto; width: 118px; }
.dish-photo {
  width: 118px; height: 96px; border-radius: 10px;
  background: linear-gradient(135deg, #EDE7D9 0%, #DDD1B0 100%);
  background-size: cover; background-position: center;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 6px;
}
.dish-photo-fallback { font-size: 11px; color: #9C8F6E; text-align: center; padding: 0 8px; }
.dish-name { font-size: 12.5px; font-weight: 600; color: var(--ink); line-height: 1.3; }
.dish-price { font-size: 12px; color: var(--gold); font-weight: 700; margin-top: 2px; }
.dish-loading { font-size: 12px; color: var(--muted); padding: 20px 0; }

.store-card {
  display: flex; align-items: center; justify-content: space-between;
  background: #fff; border-radius: 10px; padding: 12px 14px; margin-bottom: 10px;
  box-shadow: 0 1px 6px rgba(0,0,0,0.05);
}
.store-name { font-size: 14px; font-weight: 700; color: var(--forest); }
.store-addr { font-size: 11.5px; color: var(--muted); margin-top: 3px; }
.store-hours { font-size: 11px; color: var(--muted); margin-top: 2px; }
.store-call {
  flex-shrink: 0; background: var(--forest); color: #fff; font-size: 12px;
  padding: 7px 14px; border-radius: 16px; text-decoration: none; margin-left: 10px;
}

.banquet-card {
  position: relative; height: 120px; border-radius: 12px; overflow: hidden;
  margin: 18px 16px; padding: 0 !important;
}
.banquet-bg { width: 100%; height: 100%; object-fit: cover; }
.banquet-overlay {
  position: absolute; inset: 0; background: linear-gradient(90deg, rgba(8,15,11,0.75), rgba(8,15,11,0.15));
  display: flex; flex-direction: column; justify-content: center; padding: 0 18px; color: #fff;
}
.banquet-title { font-size: 15px; font-weight: 700; }
.banquet-sub { font-size: 11px; color: rgba(255,255,255,0.8); margin-top: 4px; }

.m-footer-note { text-align: center; font-size: 10.5px; color: var(--muted); padding: 14px 0 26px; }
</style>

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
        <button class="hero-cta" @click="$router.push('/m/book')">立即预定</button>
      </div>
    </div>

    <section class="m-section">
      <div class="m-section-head">
        <span class="m-section-title">臻选招牌</span>
        <span class="m-section-more" @click="$router.push('/m/menu')">
          查看全部<SiteIcon name="chevron-right" :size="13" />
        </span>
      </div>
      <div class="dish-scroll">
        <div v-for="d in dishes" :key="d.dish_name" class="dish-card">
          <div class="dish-photo" :style="photoStyle(d)">
            <span v-if="!d._photo" class="dish-photo-fallback">{{ (d.dish_name || '菜').charAt(0) }}</span>
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
        <div class="banquet-title">宴会套餐</div>
        <div class="banquet-sub">婚宴 · 寿宴 · 商务 · 满月<SiteIcon name="chevron-right" :size="13" /></div>
      </div>
    </section>

    <div class="m-footer-note">又见炊烟私房菜 · 二十余年手艺传承</div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import SiteIcon from '@/components/site/SiteIcon.vue'
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
/* 品牌名用衬线，字重回到 600——18px 的伪粗体在手机上边缘发毛。 */
.hero-title {
  font-family: var(--site-serif);
  font-size: 19px; font-weight: 600; letter-spacing: 0.1em; line-height: 1.2;
}
.hero-sub { font-size: 9px; letter-spacing: 0.18em; color: rgba(255,255,255,0.7); margin-top: 3px; }
.hero-line { font-size: var(--site-fs-small); color: rgba(255,255,255,0.88); margin: 0 0 14px; letter-spacing: 0.06em; }
/*
  这颗按钮原来是金色实底 + 24px 全圆角。圆角胶囊加金色，是"塑料感"最典型的一组：
  颜色发亮、形状发软，跟页面上别的直角块面也对不上。
  改成白底墨绿字、2px 圆角——照片上最醒目的其实是白色，不是金色。
*/
.hero-cta {
  align-self: flex-start;
  background: #fff;
  color: var(--site-pine);
  border: none;
  padding: 12px 24px;
  border-radius: var(--site-radius);
  font-family: inherit;
  font-size: var(--site-fs-body);
  letter-spacing: 0.06em;
}

.m-section { padding: 18px 16px 4px; }
.m-section-head { display: flex; justify-content: space-between; align-items: baseline; margin-bottom: 10px; }
.m-section-title {
  font-family: var(--site-serif);
  font-size: var(--site-fs-lead); font-weight: 600;
  letter-spacing: 0.04em; color: var(--site-pine);
}
.m-section-more {
  display: inline-flex; align-items: center; gap: 3px;
  font-size: var(--site-fs-caption); color: var(--site-ink-3);
}

.dish-scroll { display: flex; gap: 12px; overflow-x: auto; padding-bottom: 4px; -webkit-overflow-scrolling: touch; }
.dish-scroll::-webkit-scrollbar { display: none; }
.dish-card { flex: 0 0 auto; width: 118px; }
/* 没有实拍图时，原来是米色渐变块里印一遍菜名——菜名下面本来就有一遍，重复了。
   改成宣纸底纹加菜名首字，跟桌面站一致。圆角从 10px 收到 3px，与全站对齐。 */
.dish-photo {
  width: 118px; height: 96px; border-radius: var(--site-radius-lg);
  background-color: var(--site-surface-2);
  background-image: repeating-linear-gradient(45deg,
    rgba(30,58,47,0.03) 0, rgba(30,58,47,0.03) 1px, transparent 1px, transparent 9px);
  background-size: cover; background-position: center;
  display: flex; align-items: center; justify-content: center;
  margin-bottom: 7px;
}
.dish-photo-fallback {
  font-family: var(--site-serif);
  font-size: 30px; color: rgba(30,58,47,0.14); user-select: none;
}
.dish-name { font-size: var(--site-fs-small); color: var(--site-ink); line-height: 1.35; }
.dish-price { font-family: var(--site-serif); font-size: var(--site-fs-body); color: var(--site-pine); margin-top: 3px; }
.dish-loading { font-size: 12px; color: var(--muted); padding: 20px 0; }

/* 卡片从"白底 + 10px 圆角 + 一层浅影"改成"白底 + 3px 圆角 + 1px 发丝边"。
   手机上一屏叠三四张带影的圆角卡，整页会显得软塌塌的。 */
.store-card {
  display: flex; align-items: center; justify-content: space-between;
  background: var(--site-surface);
  border: 1px solid var(--site-line);
  border-radius: var(--site-radius-lg);
  padding: 14px; margin-bottom: 10px;
}
.store-name { font-size: var(--site-fs-body); color: var(--site-pine); letter-spacing: 0.02em; }
.store-addr { font-size: var(--site-fs-caption); color: var(--site-ink-2); margin-top: 4px; line-height: var(--site-lh-normal); }
.store-hours { font-size: var(--site-fs-caption); color: var(--site-ink-3); margin-top: 3px; }
/* 拨打键原来是全圆角绿胶囊。手机上真正会被按的就是它，所以保留实底，
   但换成直角，和站点其余按钮同一套形状。 */
.store-call {
  flex-shrink: 0;
  background: var(--site-pine); color: #fff;
  font-size: var(--site-fs-small); letter-spacing: 0.06em;
  padding: 10px 16px; border-radius: var(--site-radius);
  text-decoration: none; margin-left: 12px;
}

.banquet-card {
  position: relative; height: 132px; border-radius: var(--site-radius-lg); overflow: hidden;
  margin: 18px 16px; padding: 0 !important;
}
.banquet-bg { width: 100%; height: 100%; object-fit: cover; }
.banquet-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(90deg, rgba(8,15,11,0.78) 0%, rgba(8,15,11,0.4) 60%, rgba(8,15,11,0.15) 100%);
  display: flex; flex-direction: column; justify-content: center; padding: 0 18px; color: #fff;
}
.banquet-title { font-family: var(--site-serif); font-size: var(--site-fs-h3); font-weight: 600; letter-spacing: 0.06em; }
.banquet-sub {
  display: inline-flex; align-items: center; gap: 4px;
  font-size: var(--site-fs-caption); color: rgba(255,255,255,0.82);
  margin-top: 6px; letter-spacing: 0.04em;
}

.m-footer-note { text-align: center; font-size: 10.5px; color: var(--muted); padding: 14px 0 26px; }
</style>

<template>
  <div class="home">
    <!-- 顶部导航 -->
    <header class="nav" :class="{ scrolled: isScrolled }">
      <div class="nav-inner">
        <div class="brand" @click="scrollTo('top')">
          <span class="brand-cn">又见炊烟</span>
          <span class="brand-en">YOUJIANCHUIYAN</span>
        </div>
        <nav class="nav-links">
          <a @click="scrollTo('story')">品牌故事</a>
          <a @click="scrollTo('dishes')">臻选菜品</a>
          <a @click="scrollTo('route')">皖南川藏线</a>
          <a @click="scrollTo('stores')">门店选择</a>
        </nav>
        <div class="nav-actions">
          <button class="btn-ghost" @click="goLogin">登录 · Login</button>
          <button class="btn-gold" @click="scrollTo('stores')">立即预定</button>
        </div>
      </div>
    </header>

    <!-- Hero -->
    <section id="top" class="hero">
      <div class="hero-media placeholder-block">
        <span class="placeholder-label">首图 · 门店实景 / 宣传视频（待补）</span>
      </div>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <p class="hero-eyebrow">皖南水墨山水间</p>
        <h1 class="hero-title">又见炊烟，又见你！</h1>
        <p class="hero-subtitle">A Private Kitchen Amid the Ink-Wash Landscapes of Southern Anhui</p>
        <p class="hero-desc">处处山清水秀，徽风皖韵，现点现做，不做预制菜。宁国、宣城两店，坐落皖南川藏线两端。</p>
        <div class="hero-actions">
          <button class="btn-gold large" @click="scrollTo('stores')">预定席位 · Reserve</button>
          <button class="btn-outline large" @click="scrollTo('dishes')">查看菜品 · Menu</button>
        </div>
      </div>
    </section>

    <!-- 品牌故事 -->
    <section id="story" class="section story">
      <div class="section-inner two-col">
        <div class="col-text">
          <p class="section-eyebrow">Our Story</p>
          <h2 class="section-title">二十余年灶火，只为一味乡愁</h2>
          <p class="section-body">
            又见炊烟私房菜创立于皖南，以徽菜为根、私房手艺为魂，二十余年只做一件事——
            现点现做，绝不使用预制菜。从食材甄选、初加工到出品上桌，每一道工序都亲手完成，
            只为端上桌的那一刻，客人能尝到"锅气"与"人情味"。
          </p>
          <p class="section-body">
            我们相信，一顿好的宴席不只是味道，更是一段值得慢下来的时光——
            这也是"又见炊烟"这个名字的由来：暮色四合，炊烟升起，家的味道正在等你。
          </p>
        </div>
        <div class="col-media placeholder-block tall">
          <span class="placeholder-label">门店环境实拍（待补）</span>
        </div>
      </div>
    </section>

    <!-- 门店环境 -->
    <section class="section environment">
      <div class="section-inner">
        <p class="section-eyebrow center">Ambiance</p>
        <h2 class="section-title center">门店环境</h2>
        <p class="section-subtitle center">包厢雅致，大堂开阔，山水相伴，宜宴宜聚</p>
        <div class="gallery-grid">
          <div v-for="n in 6" :key="n" class="placeholder-block gallery-item">
            <span class="placeholder-label">环境实景 {{ n }}</span>
          </div>
        </div>
      </div>
    </section>

    <!-- 皖南川藏线 -->
    <section id="route" class="section route">
      <div class="section-inner two-col reverse">
        <div class="col-media placeholder-block tall">
          <span class="placeholder-label">皖南川藏线风光（待补）</span>
        </div>
        <div class="col-text">
          <p class="section-eyebrow">Scenic Route · 江南天路</p>
          <h2 class="section-title">皖南川藏线，两端皆有归处</h2>
          <p class="section-body">
            皖南川藏线素有"江南天路 · 皖南318"之称——既有桂林山水之秀美，云南石林之奇绝，
            也有几分318川藏线的蜿蜒险峻，一路串起青龙湾、储家滩、方塘喀斯特石林与落羽杉红杉林，
            是近年备受自驾与骑行爱好者青睐的皖南风景廊道。又见炊烟的两家门店，恰好分处这条线路的两端——
          </p>
          <ul class="route-list">
            <li>
              <strong>宁国店 · 皖南川藏线东入口</strong>
              <span>紧邻"天然氧吧"青龙湾，是许多旅人进入这条风景线的第一站，落座即歇脚，饱餐再启程。</span>
            </li>
            <li>
              <strong>宣城店</strong>
              <span>城区门店，交通便利，适合宴请、聚会与商务接待，亦是行程另一端的落脚之选。</span>
            </li>
          </ul>
          <p class="section-body">
            无论从东入口出发，还是从宣城折返，一路水墨山水之后，总有一处"又见炊烟"，
            以一桌热菜，款待归途——又见炊烟，又见你。
          </p>
        </div>
      </div>
    </section>

    <!-- 臻选菜品 -->
    <section id="dishes" class="section dishes">
      <div class="section-inner">
        <p class="section-eyebrow center">Signature Menu</p>
        <h2 class="section-title center">臻选菜品</h2>
        <p class="section-subtitle center">现点现做 · 时令为先 · 徽风粤味</p>

        <div v-if="dishesLoading" class="dishes-loading">菜单加载中...</div>
        <div v-else-if="dishes.length === 0" class="dishes-loading">菜单信息完善中，敬请期待</div>
        <div v-else class="dish-grid">
          <div v-for="(d, i) in dishes" :key="i" class="dish-card">
            <div class="placeholder-block dish-image">
              <span class="placeholder-label">菜品实拍</span>
            </div>
            <div class="dish-info">
              <h4 class="dish-name">{{ d.dishName }}</h4>
              <p v-if="d.dishNameEn" class="dish-name-en">{{ d.dishNameEn }}</p>
              <div class="dish-meta">
                <span class="dish-category">{{ d.dishCategory || '精选' }}</span>
                <span class="dish-price">¥{{ formatPrice(d.salePrice) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 门店选择 -->
    <section id="stores" class="section stores">
      <div class="section-inner">
        <p class="section-eyebrow center">Reservations</p>
        <h2 class="section-title center">门店选择 · 立即预定</h2>
        <p class="section-subtitle center">选择门店，致电预定，我们恭候光临</p>

        <div v-if="storesLoading" class="dishes-loading">门店信息加载中...</div>
        <div v-else class="store-grid">
          <div v-for="s in stores" :key="s.storeId" class="store-card">
            <div class="placeholder-block store-image">
              <span class="placeholder-label">{{ s.storeName }} 门店实景</span>
            </div>
            <div class="store-info">
              <h3 class="store-name">{{ s.storeName }}</h3>
              <p class="store-detail"><span class="store-icon">📍</span>{{ s.address }}</p>
              <p class="store-detail"><span class="store-icon">🕐</span>{{ s.businessHours }}</p>
              <p class="store-detail"><span class="store-icon">📞</span>{{ s.phone }}</p>
              <a class="btn-gold store-cta" :href="'tel:' + s.phone">致电预定 · {{ s.phone }}</a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 加入我们 -->
    <section class="join">
      <div class="join-inner">
        <div>
          <p class="section-eyebrow light">Join Our Team</p>
          <h2 class="join-title">加入又见炊烟</h2>
          <p class="join-desc">我们始终在寻找热爱美食与服务的伙伴，一起把"家的味道"端给更多人</p>
        </div>
        <button class="btn-gold large" @click="goJoin">查看在招岗位 · Apply Now</button>
      </div>
    </section>

    <!-- Footer -->
    <footer class="footer">
      <div class="footer-inner">
        <div class="footer-col">
          <div class="brand">
            <span class="brand-cn">又见炊烟</span>
          </div>
          <p class="footer-tagline">徽风皖韵 · 私房宴席 · 二十余年</p>
        </div>
        <div class="footer-col">
          <h4>门店</h4>
          <p v-for="s in stores" :key="s.storeId">{{ s.storeName }} · {{ s.phone }}</p>
        </div>
        <div class="footer-col">
          <h4>快速入口</h4>
          <p><a @click="goLogin">员工登录</a></p>
          <p><a @click="goJoin">加入我们</a></p>
          <p><a @click="scrollTo('stores')">门店预定</a></p>
        </div>
      </div>
      <div class="footer-bottom">
        <p>© {{ year }} 又见炊烟私房菜 · Youjianchuiyan Private Kitchen. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const isScrolled = ref(false)
const dishes = ref([])
const dishesLoading = ref(true)
const stores = ref([])
const storesLoading = ref(true)
const year = new Date().getFullYear()

function onScroll() {
  isScrolled.value = window.scrollY > 40
}

function scrollTo(id) {
  if (id === 'top') {
    window.scrollTo({ top: 0, behavior: 'smooth' })
    return
  }
  const el = document.getElementById(id)
  if (el) el.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function goLogin() {
  router.push('/login')
}

function goJoin() {
  router.push('/self-service')
}

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

async function loadDishes() {
  dishesLoading.value = true
  try {
    const res = await request.get('/api/public/menu/preview', { params: { storeId: 1, limit: 8 } })
    dishes.value = (res.data || []).map(d => ({
      dishName: d.dish_name ?? d.dishName,
      dishNameEn: d.dish_name_en ?? d.dishNameEn,
      dishCategory: d.dish_category ?? d.dishCategory,
      salePrice: d.sale_price ?? d.salePrice
    }))
  } catch (e) {
    dishes.value = []
  } finally {
    dishesLoading.value = false
  }
}

async function loadStores() {
  storesLoading.value = true
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
    storesLoading.value = false
  }
}

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  loadDishes()
  loadStores()
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
:root {}

.home {
  --forest: #1F3A2E;
  --forest-light: #2D4A3E;
  --gold: #B8935A;
  --gold-light: #D4B483;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #7A7A72;
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Segoe UI", sans-serif;
  color: var(--ink);
  background: var(--ivory);
  overflow-x: hidden;
}

/* ===== 通用占位块 ===== */
.placeholder-block {
  background: linear-gradient(135deg, #EDE7D9 0%, #E3DBC8 50%, #D9CFB5 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  border: 1px dashed #C9BFA0;
}
.placeholder-label {
  color: #9C8F6E;
  font-size: 13px;
  letter-spacing: 1px;
  padding: 8px 16px;
  background: rgba(255,255,255,0.5);
  border-radius: 4px;
}

/* ===== 导航 ===== */
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  transition: all 0.3s ease;
  background: transparent;
}
.nav.scrolled {
  background: rgba(31, 58, 46, 0.96);
  box-shadow: 0 2px 20px rgba(0,0,0,0.15);
}
.nav-inner {
  max-width: 1280px;
  margin: 0 auto;
  padding: 20px 32px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand { cursor: pointer; display: flex; flex-direction: column; line-height: 1.2; }
.brand-cn { font-size: 20px; font-weight: 700; color: #fff; letter-spacing: 2px; }
.brand-en { font-size: 9px; color: var(--gold-light); letter-spacing: 2px; }
.nav-links { display: flex; gap: 36px; }
.nav-links a {
  color: #fff; font-size: 14px; letter-spacing: 1px; cursor: pointer;
  opacity: 0.88; transition: opacity 0.2s;
}
.nav-links a:hover { opacity: 1; color: var(--gold-light); }
.nav-actions { display: flex; gap: 12px; align-items: center; }

.btn-ghost {
  background: transparent; border: 1px solid rgba(255,255,255,0.5); color: #fff;
  padding: 9px 18px; border-radius: 2px; font-size: 13px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s;
}
.btn-ghost:hover { border-color: var(--gold-light); color: var(--gold-light); }
.btn-gold {
  background: var(--gold); border: 1px solid var(--gold); color: #fff;
  padding: 9px 22px; border-radius: 2px; font-size: 13px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s; display: inline-block; text-decoration: none; text-align: center;
}
.btn-gold:hover { background: #A17E48; border-color: #A17E48; }
.btn-outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.7); color: #fff;
  padding: 9px 22px; border-radius: 2px; font-size: 13px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s;
}
.btn-outline:hover { background: rgba(255,255,255,0.12); }
.large { padding: 14px 32px; font-size: 14px; }

/* ===== Hero ===== */
.hero {
  position: relative;
  height: 92vh;
  min-height: 600px;
  display: flex;
  align-items: flex-end;
  overflow: hidden;
}
.hero-media { position: absolute; inset: 0; }
.hero-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(20,32,26,0.15) 0%, rgba(20,32,26,0.55) 65%, rgba(20,32,26,0.85) 100%);
}
.hero-content {
  position: relative;
  z-index: 2;
  max-width: 1280px;
  margin: 0 auto;
  padding: 0 32px 96px;
  width: 100%;
  color: #fff;
}
.hero-eyebrow { font-size: 14px; letter-spacing: 4px; color: var(--gold-light); margin: 0 0 16px; }
.hero-title { font-size: 56px; font-weight: 700; margin: 0 0 12px; letter-spacing: 4px; }
.hero-subtitle { font-size: 16px; color: rgba(255,255,255,0.85); margin: 0 0 20px; letter-spacing: 1px; }
.hero-desc { font-size: 15px; color: rgba(255,255,255,0.75); margin: 0 0 36px; max-width: 560px; line-height: 1.8; }
.hero-actions { display: flex; gap: 16px; }

/* ===== Section 通用 ===== */
.section { padding: 100px 32px; }
.section-inner { max-width: 1200px; margin: 0 auto; }
.section-eyebrow { font-size: 13px; letter-spacing: 3px; color: var(--gold); margin: 0 0 12px; font-weight: 600; }
.section-eyebrow.center { text-align: center; }
.section-eyebrow.light { color: var(--gold-light); }
.section-title { font-size: 32px; font-weight: 700; color: var(--forest); margin: 0 0 20px; letter-spacing: 1px; }
.section-title.center { text-align: center; }
.section-subtitle { font-size: 15px; color: var(--muted); margin: -8px 0 48px; }
.section-subtitle.center { text-align: center; }
.section-body { font-size: 15px; line-height: 2; color: #4A4A44; margin: 0 0 20px; }

.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 64px;
  align-items: center;
}
.two-col.reverse { direction: rtl; }
.two-col.reverse > * { direction: ltr; }
.col-media.tall { height: 420px; border-radius: 4px; overflow: hidden; }

.story { background: #fff; }

/* ===== 环境画廊 ===== */
.environment { background: var(--ivory); }
.gallery-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}
.gallery-item { height: 260px; border-radius: 4px; }

/* ===== 皖南川藏线 ===== */
.route { background: #fff; }
.route-list { list-style: none; padding: 0; margin: 24px 0; }
.route-list li {
  display: flex; flex-direction: column; gap: 4px;
  padding: 16px 0 16px 20px;
  border-left: 2px solid var(--gold);
  margin-bottom: 16px;
}
.route-list li strong { color: var(--forest); font-size: 16px; }
.route-list li span { color: var(--muted); font-size: 14px; line-height: 1.7; }

/* ===== 菜品 ===== */
.dishes { background: var(--ivory); }
.dishes-loading { text-align: center; color: var(--muted); padding: 40px 0; }
.dish-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
}
.dish-card { background: #fff; border-radius: 4px; overflow: hidden; box-shadow: 0 2px 16px rgba(0,0,0,0.05); }
.dish-image { height: 160px; }
.dish-info { padding: 18px; }
.dish-name { font-size: 16px; font-weight: 700; color: var(--forest); margin: 0 0 2px; }
.dish-name-en { font-size: 11px; color: var(--muted); margin: 0 0 12px; letter-spacing: 0.5px; }
.dish-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 12px; }
.dish-category { font-size: 12px; color: var(--gold); background: rgba(184,147,90,0.1); padding: 3px 10px; border-radius: 2px; }
.dish-price { font-size: 17px; font-weight: 700; color: var(--forest); }

/* ===== 门店选择 ===== */
.stores { background: #fff; }
.store-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 32px; }
.store-card { border: 1px solid #EDE7D9; border-radius: 4px; overflow: hidden; }
.store-image { height: 220px; }
.store-info { padding: 28px; }
.store-name { font-size: 22px; font-weight: 700; color: var(--forest); margin: 0 0 16px; }
.store-detail { font-size: 14px; color: #4A4A44; margin: 0 0 10px; display: flex; gap: 8px; align-items: flex-start; }
.store-icon { flex-shrink: 0; }
.store-cta { margin-top: 16px; width: 100%; }

/* ===== 加入我们 ===== */
.join { background: var(--forest); padding: 72px 32px; }
.join-inner {
  max-width: 1200px; margin: 0 auto;
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 24px;
}
.join-title { font-size: 28px; font-weight: 700; color: #fff; margin: 0 0 8px; }
.join-desc { font-size: 14px; color: rgba(255,255,255,0.7); margin: 0; }

/* ===== Footer ===== */
.footer { background: #16241C; padding: 56px 32px 24px; }
.footer-inner {
  max-width: 1200px; margin: 0 auto;
  display: grid; grid-template-columns: 2fr 1fr 1fr; gap: 48px;
  padding-bottom: 32px; border-bottom: 1px solid rgba(255,255,255,0.1);
}
.footer-tagline { color: rgba(255,255,255,0.5); font-size: 13px; margin-top: 10px; }
.footer-col h4 { color: #fff; font-size: 14px; margin: 0 0 16px; letter-spacing: 1px; }
.footer-col p { color: rgba(255,255,255,0.6); font-size: 13px; margin: 0 0 10px; }
.footer-col a { cursor: pointer; }
.footer-col a:hover { color: var(--gold-light); }
.footer-bottom { max-width: 1200px; margin: 0 auto; padding-top: 20px; text-align: center; }
.footer-bottom p { color: rgba(255,255,255,0.35); font-size: 12px; margin: 0; }

/* ===== 响应式 ===== */
@media (max-width: 960px) {
  .nav-links { display: none; }
  .hero-title { font-size: 38px; }
  .two-col, .two-col.reverse { grid-template-columns: 1fr; direction: ltr; }
  .gallery-grid { grid-template-columns: repeat(2, 1fr); }
  .dish-grid { grid-template-columns: repeat(2, 1fr); }
  .store-grid { grid-template-columns: 1fr; }
  .footer-inner { grid-template-columns: 1fr; gap: 28px; }
  .join-inner { flex-direction: column; align-items: flex-start; }
}
</style>

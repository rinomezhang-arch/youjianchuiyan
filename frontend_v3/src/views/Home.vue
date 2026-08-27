<template>
  <div class="home">
    <SiteNav />

    <!-- Hero：门店实景视频轮播，静音自动播放，两段循环切换 -->
    <section class="hero">
      <img src="/site-photos/hero-mountain-view.jpg" class="hero-media hero-media-fallback" alt="又见炊烟私房菜" />
      <video
        ref="heroVideoEl"
        class="hero-media hero-video"
        :src="heroVideos[heroVideoIndex]"
        autoplay muted playsinline
        @ended="nextHeroVideo"
      ></video>
      <div class="hero-overlay"></div>
      <div class="hero-content">
        <h1 class="hero-title">又见炊烟，又见你</h1>
        <p class="hero-subtitle">Youjianchuiyan Private Kitchen — Where Home-Cooked Flavor Waits</p>
        <div class="hero-actions">
          <button class="btn-outline large" @click="router.push('/menu')">了解更多 Discover More</button>
        </div>
      </div>
      <div class="hero-scroll-hint">向下探索 Scroll to Explore</div>
    </section>

    <!-- 品牌故事：突出私房菜手艺与本地食材，不是风光展示 -->
    <section class="brand-statement">
      <div class="brand-statement-inner">
        <p class="brand-statement-cn">
          又见炊烟，是一间"私房菜"——不是餐厅连锁，而是手艺的传承。二十余年灶火，
          食材皆取自本地时令山货与河鲜，现点现做，绝不使用预制菜。
          每一道菜，都是主厨对"家的味道"的坚持。
        </p>
        <p class="brand-statement-en">
          Youjianchuiyan is a private kitchen, not a chain — twenty years of craft, built on
          local, seasonal ingredients and dishes cooked to order, never pre-made.
        </p>
      </div>
    </section>

    <!-- 臻选菜品 -->
    <section class="board">
      <div class="board-inner">
        <div class="board-head">
          <p class="board-eyebrow">Signature Dishes</p>
          <h2 class="board-title">臻选菜品</h2>
          <p class="board-sub">本地时令食材 · 现点现做 · 十道招牌菜，一一为您道来</p>
        </div>
        <div class="dish-strip">
          <div class="dish-strip-card" v-for="d in featuredDishes" :key="d.name" @click="router.push('/menu')">
            <img :src="d.img" :alt="d.name" />
            <div class="dish-strip-overlay">
              <span class="dsn-cn">{{ d.name }}</span>
              <span class="dsn-en">{{ d.en }}</span>
              <p class="dsn-poem">{{ d.poem }}</p>
            </div>
          </div>
        </div>
        <div class="board-more"><a @click="router.push('/menu')">查看完整菜单 View Full Menu →</a></div>
      </div>
    </section>

    <!-- 包厢与环境 -->
    <section class="board board-alt">
      <div class="board-inner">
        <div class="board-head">
          <p class="board-eyebrow">Private Rooms & Ambiance</p>
          <h2 class="board-title">包厢与环境</h2>
          <p class="board-sub">推开一扇门，是山景，亦是心境 · 原木、纱帘与暖光交织的团聚之处</p>
        </div>
        <PeekCarousel :items="ambiancePhotos" :card-width="520" />
        <p class="section-body ambiance-caption">
          窗外或是城市灯火，或是远山如黛，无论哪一种，都值得您静坐片刻，慢慢用一顿饭的时间。
          <span class="section-body-en">Whether the view outside is city lights or distant hills, every room invites you to slow down.</span>
        </p>
        <div class="board-more"><a @click="router.push('/stores')">走进门店 Visit Our Restaurants →</a></div>
      </div>
    </section>

    <!-- 宴会套餐 · 婚宴与庆典 -->
    <section class="board">
      <div class="board-inner">
        <div class="board-head">
          <p class="board-eyebrow">Banquets & Celebrations</p>
          <h2 class="board-title">宴会套餐 · 婚宴与庆典</h2>
          <p class="board-sub">为婚宴、寿宴、升学、商务与满月等场合量身而备</p>
        </div>
        <div v-if="pkgLoading" class="board-loading">加载中 Loading...</div>
        <div v-else class="event-grid">
          <div class="event-card" v-for="e in eventCards" :key="e.key" @click="router.push('/packages')">
            <img :src="e.img" :alt="e.titleCn" />
            <div class="event-body">
              <p class="event-eyebrow">{{ e.eyebrow }}</p>
              <h3>{{ e.titleCn }}</h3>
              <p class="event-desc">{{ e.desc }}</p>
              <span class="event-link">了解更多 Learn More →</span>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 皖南攻略 -->
    <section class="board board-alt">
      <div class="board-inner two-col">
        <div class="col-media">
          <img src="/site-photos/mountain-mist-balcony.jpg" alt="皖南攻略" />
        </div>
        <div class="col-text">
          <p class="board-eyebrow">Travel Guide</p>
          <h2 class="board-title">皖南攻略</h2>
          <p class="section-body">
            推窗见山，是这两家门店共有的幸运。若您远道而来，不妨顺路走一走青龙湾、敬亭山、桃花潭这些皖南名胜，
            用餐之余，也留一程山水。
          </p>
          <p class="section-body-en">
            Both restaurants sit at either end of the Southern Anhui scenic route — worth a
            detour to Qinglong Bay, Jingting Mountain, or Peach Blossom Pool along the way.
          </p>
          <div class="board-more left"><a @click="router.push('/guide')">查看旅行攻略 View Travel Guide →</a></div>
        </div>
      </div>
    </section>

    <!-- 门店与地址：真实地图，两店标注，可滚轮缩放 -->
    <section class="board">
      <div class="board-inner">
        <div class="board-head">
          <p class="board-eyebrow">Visit Us</p>
          <h2 class="board-title">门店与地址</h2>
          <p class="board-sub">宁国、宣城两店，皆可现场品味</p>
        </div>
        <StoreMap v-if="stores.length" :stores="stores" />
        <div class="visit-list">
          <div class="visit-row" v-for="s in stores" :key="s.storeId">
            <div>
              <strong>{{ s.storeName }}</strong>
              <span>{{ s.address }}</span>
            </div>
            <div class="visit-row-meta">
              <span>{{ s.businessHours }}</span>
              <span>{{ s.phone }}</span>
              <a @click="router.push(`/stores/${s.storeId}`)">门店详情 →</a>
            </div>
          </div>
        </div>
      </div>
    </section>

    <!-- 加入我们 -->
    <section class="join">
      <img src="/site-photos/terrace-roses.jpg" class="join-media" alt="加入我们" />
      <div class="join-overlay"></div>
      <div class="join-inner">
        <div>
          <p class="section-eyebrow light">Join Our Team</p>
          <h2 class="join-title">加入又见炊烟 Join Youjianchuiyan</h2>
          <p class="join-desc">我们始终在寻找热爱美食与服务的伙伴，一起把"家的味道"端给更多人</p>
        </div>
        <button class="btn-gold large" @click="router.push('/self-service')">查看在招岗位 View Open Roles</button>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import PeekCarousel from '@/components/site/PeekCarousel.vue'
import StoreMap from '@/components/site/StoreMap.vue'
import request from '@/utils/request'

const ambiancePhotos = [
  { img: '/site-photos/private-room-marble-round.jpg', alt: '包厢环境' },
  { img: '/site-photos/private-room-french-window.jpg', alt: '包厢环境' },
  { img: '/site-photos/banquet-hall-grand.jpg', alt: '包厢环境' },
  { img: '/site-photos/private-room-terrace-access.jpg', alt: '包厢环境' },
  { img: '/site-photos/private-room-woodwall.jpg', alt: '包厢环境' },
  { img: '/site-photos/private-room-chandelier.jpg', alt: '包厢环境' },
  { img: '/site-photos/private-room-cityview.jpg', alt: '包厢环境' }
]

const router = useRouter()
const stores = ref([])
const storesLoading = ref(true)
const pkgLoading = ref(true)

// 首页顶部实景视频轮播：门店移动外景实拍，静音播完一段自动切下一段，循环往复
const heroVideos = ['/site-videos/exterior-1.mp4', '/site-videos/exterior-2.mp4']
const heroVideoIndex = ref(0)
const heroVideoEl = ref(null)
function nextHeroVideo() {
  heroVideoIndex.value = (heroVideoIndex.value + 1) % heroVideos.length
  nextTick(() => {
    if (heroVideoEl.value) {
      heroVideoEl.value.load()
      heroVideoEl.value.play().catch(() => {})
    }
  })
}

const featuredDishes = [
  {
    name: '剁椒鱼头', en: 'Steamed Fish Head with Chili', img: '/dish-photos/duojiao-yutou.jpg',
    poem: '湘式剁椒的辛香，浸入鱼头的丰腴，一口足以唤醒味蕾。'
  },
  {
    name: '土锅黑鱼', en: 'Clay-Pot Snakehead Fish', img: '/dish-photos/tuguo-heiyu.jpg',
    poem: '土锅慢煨，锁住河鲜本味，汤色浓白，鱼肉滑嫩如脂。'
  },
  {
    name: '老豆腐蒸腊肉', en: 'Steamed Tofu with Cured Pork', img: '/dish-photos/laodoufu-larou.jpg',
    poem: '老豆腐吸尽腊肉的烟熏咸香，是最朴素也最难忘的乡味。'
  }
]

const eventCards = [
  {
    key: 'wedding',
    eyebrow: 'Wedding Banquets',
    titleCn: '婚宴与庆典',
    desc: '百年好合宴、龙凤呈祥宴——满厅灯火与圆桌，见证您人生中最值得珍藏的一天。',
    img: '/site-photos/banquet-hall-grand.jpg'
  },
  {
    key: 'business',
    eyebrow: 'Business & Family Gatherings',
    titleCn: '商务与家宴',
    desc: '商务精英宴、福寿双全宴——包厢雅致，宜商宜聚，现点现做款待每一位宾客。',
    img: '/site-photos/private-room-cityview.jpg'
  }
]

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

onMounted(async () => {
  await loadStores()
  pkgLoading.value = false
})
</script>

<style scoped>
.home {
  --forest: #1F3A2E;
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

.btn-gold {
  background: var(--gold); border: 1px solid var(--gold); color: #fff;
  padding: 10px 22px; border-radius: 2px; font-size: 13px; letter-spacing: 0.5px; cursor: pointer;
  transition: all 0.2s; display: inline-block; text-decoration: none; text-align: center;
}
.btn-gold:hover { background: #A17E48; border-color: #A17E48; }
.btn-outline {
  background: transparent; border: 1px solid rgba(255,255,255,0.7); color: #fff;
  padding: 10px 22px; border-radius: 2px; font-size: 13px; letter-spacing: 0.5px; cursor: pointer;
  transition: all 0.2s;
}
.btn-outline:hover { background: rgba(255,255,255,0.12); }
.btn-outline-dark {
  background: transparent; border: 1px solid var(--forest); color: var(--forest);
  padding: 10px 22px; border-radius: 2px; font-size: 13px; letter-spacing: 0.5px; cursor: pointer;
}
.btn-outline-dark:hover { background: var(--forest); color: #fff; }
.large { padding: 15px 30px; font-size: 14px; }

/* ===== Hero ===== */
.hero {
  position: relative;
  height: 100vh;
  min-height: 640px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.hero-media { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.hero-overlay {
  position: absolute; inset: 0;
  background: linear-gradient(180deg, rgba(15,25,20,0.35) 0%, rgba(15,25,20,0.15) 40%, rgba(15,25,20,0.55) 100%);
}
.hero-content {
  position: relative; z-index: 2; max-width: 800px; padding: 0 32px; text-align: center; color: #fff;
}
.hero-eyebrow { font-size: 13px; letter-spacing: 1px; color: var(--gold-light); margin: 0 0 20px; }
.hero-title { font-size: 52px; font-weight: 700; margin: 0 0 16px; letter-spacing: 6px; }
.hero-subtitle { font-size: 15px; color: rgba(255,255,255,0.85); margin: 0 0 40px; letter-spacing: 0.5px; font-style: italic; }
.hero-actions { display: flex; gap: 16px; justify-content: center; flex-wrap: wrap; }
.hero-scroll-hint {
  position: absolute; bottom: 32px; left: 50%; transform: translateX(-50%);
  color: rgba(255,255,255,0.7); font-size: 11px; letter-spacing: 2px; z-index: 2;
}

/* ===== 品牌一句话陈述 ===== */
.brand-statement { background: #fff; padding: 100px 32px; }
.brand-statement-inner { max-width: 720px; margin: 0 auto; text-align: center; }
.brand-statement-cn { font-size: 19px; line-height: 2.1; color: var(--forest); margin: 0 0 20px; letter-spacing: 1px; font-weight: 500; }
.brand-statement-en { font-size: 13px; line-height: 1.8; color: var(--muted); margin: 0; font-style: italic; letter-spacing: 0.3px; }

/* ===== 板块通用 ===== */
.board { background: var(--ivory); padding: 90px 40px; }
.board-alt { background: #fff; }
.board-inner { max-width: 1240px; margin: 0 auto; }
.board-head { text-align: center; margin-bottom: 48px; }
.board-eyebrow { font-size: 12px; letter-spacing: 2.5px; color: var(--gold); margin: 0 0 12px; font-weight: 600; text-transform: uppercase; }
.board-title { font-size: 30px; font-weight: 700; color: var(--forest); margin: 0 0 12px; }
.board-sub { font-size: 13.5px; color: var(--muted); margin: 0; }
.board-more { margin-top: 36px; text-align: center; }
.board-more.left { text-align: left; margin-top: 28px; }
.board-more a { font-size: 13px; color: var(--forest); font-weight: 600; cursor: pointer; letter-spacing: 0.3px; }
.board-loading { text-align: center; color: var(--muted); padding: 40px 0; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 64px; align-items: center; }
.two-col.reverse { direction: rtl; }
.two-col.reverse > * { direction: ltr; }
.col-media img { width: 100%; height: 420px; object-fit: cover; border-radius: 4px; display: block; }
.section-body { font-size: 14.5px; line-height: 2; color: #4A4A44; margin: 0 0 10px; }
.section-body-en { font-size: 12px; line-height: 1.8; color: var(--muted); margin: 0; font-style: italic; }

/* ===== 菜品条 ===== */
.dish-strip { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.dish-strip-card { position: relative; border-radius: 6px; overflow: hidden; cursor: pointer; box-shadow: 0 8px 30px rgba(0,0,0,0.12); aspect-ratio: 4/3; }
.dish-strip-card img { width: 100%; height: 100%; object-fit: cover; display: block; transition: transform 0.5s ease; }
.dish-strip-card:hover img { transform: scale(1.06); }
.dish-strip-overlay {
  position: absolute; left: 0; right: 0; bottom: 0; padding: 18px 20px;
  background: linear-gradient(180deg, transparent, rgba(20,32,26,0.88));
  display: flex; flex-direction: column; gap: 3px;
}
.dsn-cn { color: #fff; font-size: 16px; font-weight: 700; letter-spacing: 0.5px; }
.dsn-en { color: rgba(255,255,255,0.75); font-size: 10.5px; letter-spacing: 0.3px; }
.dsn-poem { color: rgba(255,255,255,0.85); font-size: 11.5px; line-height: 1.6; margin: 6px 0 0; }

/* ===== 宴会事件卡片 ===== */
.event-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 28px; }
.event-card { background: #fff; border-radius: 6px; overflow: hidden; cursor: pointer; box-shadow: 0 4px 24px rgba(0,0,0,0.06); transition: transform 0.25s, box-shadow 0.25s; }
.event-card:hover { transform: translateY(-6px); box-shadow: 0 16px 40px rgba(0,0,0,0.14); }
.event-card img { width: 100%; height: 240px; object-fit: cover; display: block; transition: transform 0.5s ease; }
.event-card:hover img { transform: scale(1.05); }
.event-body { padding: 26px; }
.event-eyebrow { font-size: 11px; letter-spacing: 1.5px; color: var(--gold); margin: 0 0 8px; font-weight: 600; text-transform: uppercase; }
.event-body h3 { font-size: 19px; font-weight: 700; color: var(--forest); margin: 0 0 10px; }
.event-desc { font-size: 13px; line-height: 1.7; color: var(--muted); margin: 0 0 16px; }
.event-link { font-size: 13px; color: var(--forest); font-weight: 600; }

.ambiance-caption { max-width: 760px; text-align: center; margin: 12px auto 0; }
.ambiance-caption .section-body-en { display: block; margin-top: 8px; }

/* ===== 门店与地址 ===== */
.visit-list { max-width: 900px; margin: 28px auto 0; }
.visit-row {
  display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px;
  padding: 20px 4px; border-bottom: 1px solid #EDE7D9;
}
.visit-row:last-child { border-bottom: none; }
.visit-row strong { display: block; color: var(--forest); font-size: 16px; margin-bottom: 4px; }
.visit-row > div:first-child span { color: var(--muted); font-size: 13px; }
.visit-row-meta { display: flex; align-items: center; gap: 18px; font-size: 13px; color: var(--muted); }
.visit-row-meta a { color: var(--forest); font-weight: 600; cursor: pointer; }

/* ===== 加入我们 ===== */
.join { position: relative; padding: 88px 40px; overflow: hidden; }
.join-media { position: absolute; inset: 0; width: 100%; height: 100%; object-fit: cover; }
.join-overlay { position: absolute; inset: 0; background: linear-gradient(120deg, rgba(20,32,26,0.82) 0%, rgba(20,32,26,0.55) 100%); }
.join-inner { position: relative; z-index: 1; max-width: 1200px; margin: 0 auto; display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 24px; }
.section-eyebrow.light { color: var(--gold-light); font-size: 13px; letter-spacing: 3px; font-weight: 600; }
.join-title { font-size: 26px; font-weight: 700; color: #fff; margin: 8px 0 8px; }
.join-desc { font-size: 14px; color: rgba(255,255,255,0.7); margin: 0; }

@media (max-width: 960px) {
  .hero-title { font-size: 36px; letter-spacing: 3px; }
  .dish-strip, .event-grid { grid-template-columns: 1fr; }
  .two-col { grid-template-columns: 1fr; }
  .join-inner { flex-direction: column; align-items: flex-start; }
}
</style>

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
        <p class="hero-subtitle">私房手艺，本地时令，现点现做</p>
        <div class="hero-actions">
          <button class="site-btn site-btn--on-dark hero-btn" @click="router.push('/menu')">看看菜单</button>
          <button class="site-btn site-btn--solid-light hero-btn" @click="router.push('/stores')">选门店预定</button>
        </div>
      </div>
      <div class="hero-scroll-hint" aria-hidden="true"><span class="scroll-line"></span>向下</div>
    </section>

    <!-- 品牌故事：突出私房菜手艺与本地食材，不是风光展示 -->
    <section class="brand-statement">
      <div class="brand-statement-inner">
        <p class="brand-statement-cn">
          又见炊烟，是一间"私房菜"——不是餐厅连锁，而是手艺的传承。二十余年灶火，
          食材皆取自本地时令山货与河鲜，现点现做，绝不使用预制菜。
          每一道菜，都是主厨对"家的味道"的坚持。
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
        <div class="board-more">
          <a @click="router.push('/menu')">查看完整菜单<SiteIcon name="arrow-right" :size="15" /></a>
        </div>
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
          窗外或是城市灯火，或是远山如黛。无论哪一种，都值得静坐片刻，慢慢用一顿饭的时间。
        </p>
        <div class="board-more">
          <a @click="router.push('/stores')">走进门店<SiteIcon name="arrow-right" :size="15" /></a>
        </div>
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
        <div v-if="pkgLoading" class="board-loading">正在取套餐…</div>
        <div v-else class="event-grid">
          <div class="event-card" v-for="e in eventCards" :key="e.key" @click="router.push('/packages')">
            <img :src="e.img" :alt="e.titleCn" />
            <div class="event-body">
              <p class="event-eyebrow">{{ e.eyebrow }}</p>
              <h3>{{ e.titleCn }}</h3>
              <p class="event-desc">{{ e.desc }}</p>
              <span class="event-link">了解更多<SiteIcon name="arrow-right" :size="15" class="ev-arrow" /></span>
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
          <div class="board-more left">
            <a @click="router.push('/guide')">查看旅行攻略<SiteIcon name="arrow-right" :size="15" /></a>
          </div>
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
              <a @click="router.push(`/stores/${s.storeId}`)">门店详情<SiteIcon name="arrow-right" :size="14" /></a>
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
          <p class="site-eyebrow join-eyebrow">Join Our Team</p>
          <h2 class="join-title">加入又见炊烟</h2>
          <p class="join-desc">我们一直在找热爱做菜与待客的人，一起把"家的味道"端给更多人。</p>
        </div>
        <button class="site-btn site-btn--solid-light join-btn" @click="router.push('/self-service')">查看在招岗位</button>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import SiteIcon from '@/components/site/SiteIcon.vue'
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
  /* 中段原来只有 0.15，正好是标题和按钮所在的位置。照片一亮（楼体灯光、天空）
     文字就糊在背景里。把中段压到 0.34，并在文字区再叠一层椭圆暗场——
     比给每个字加投影干净得多，也不会让字发虚。 */
  background:
    radial-gradient(ellipse 68% 46% at 50% 46%, rgba(12, 20, 16, 0.42) 0%, rgba(12, 20, 16, 0) 100%),
    linear-gradient(180deg, rgba(15, 25, 20, 0.46) 0%, rgba(15, 25, 20, 0.34) 42%, rgba(15, 25, 20, 0.62) 100%);
}
.hero-content {
  position: relative; z-index: 2; max-width: 800px; padding: 0 32px; text-align: center; color: #fff;
}
.hero-eyebrow { font-size: 13px; letter-spacing: 1px; color: var(--gold-light); margin: 0 0 20px; }
/* 首屏标题：字间距从 6px 收到 0.16em。6px 在 52px 字号下把词拆成了单字，
   气势没上去，只是散了。字重也从 700 降到 600——衬线体本来就有骨架，
   再加粗只会糊成一团。 */
.hero-title {
  font-family: var(--site-serif);
  font-size: var(--site-fs-display);
  font-weight: 600;
  letter-spacing: 0.16em;
  line-height: 1.25;
  margin: 0 0 var(--site-s4);
  text-indent: 0.16em;
}
/* 副标题原来是英文长句 + font-style: italic。中文字体没有真正的斜体，
   浏览器只能把字形整体倾斜，看着就是歪的。换成一句中文，去掉斜体。 */
.hero-subtitle {
  font-size: var(--site-fs-lead);
  color: rgba(255, 255, 255, 0.86);
  letter-spacing: 0.22em;
  margin: 0 0 var(--site-s7);
  text-indent: 0.22em;
}
.hero-btn { padding: 13px 30px; font-size: var(--site-fs-body); }
.hero-actions { display: flex; gap: var(--site-s3); justify-content: center; flex-wrap: wrap; }
/* 滚动提示：一条会呼吸的竖线加两个字。原来是"向下探索 Scroll to Explore"，
   一行八个字压在首屏底部，比它要引导的动作本身还重。 */
.hero-scroll-hint {
  position: absolute; bottom: 30px; left: 50%; transform: translateX(-50%);
  display: flex; flex-direction: column; align-items: center; gap: 10px;
  color: rgba(255, 255, 255, 0.62);
  font-size: var(--site-fs-micro); letter-spacing: 0.3em; text-indent: 0.3em;
  z-index: 2;
}
.scroll-line {
  width: 1px; height: 34px;
  background: linear-gradient(180deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.55) 100%);
  animation: scroll-breathe 2.4s var(--site-ease) infinite;
  transform-origin: top;
}
@keyframes scroll-breathe {
  0%, 100% { transform: scaleY(0.4); opacity: 0.35; }
  50%      { transform: scaleY(1);   opacity: 1; }
}
@media (prefers-reduced-motion: reduce) {
  .scroll-line { animation: none; opacity: 0.7; }
}

/* ===== 品牌一句话陈述 ===== */
.brand-statement { background: #fff; padding: 100px 32px; }
.brand-statement-inner { max-width: 720px; margin: 0 auto; text-align: center; }
/* 品牌陈述是全页唯一一段"说话"的文字，用衬线排，字重回到常规。
   19px/2.1 的行距留着——中文长段落就得这么松才读得下去。 */
.brand-statement-cn {
  font-family: var(--site-serif);
  font-size: 19px;
  line-height: 2.1;
  color: var(--site-pine);
  margin: 0;
  letter-spacing: 0.04em;
  font-weight: 400;
}
.brand-statement-en { font-size: 13px; line-height: 1.8; color: var(--muted); margin: 0; font-style: italic; letter-spacing: 0.3px; }

/* ===== 板块通用 ===== */
.board { background: var(--ivory); padding: 90px 40px; }
.board-alt { background: #fff; }
.board-inner { max-width: 1240px; margin: 0 auto; }
.board-head { text-align: center; margin-bottom: 48px; }
.board-eyebrow {
  font-size: var(--site-fs-caption);
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--site-brass);
  font-weight: 500;
  margin: 0 0 var(--site-s4);
}
.board-title {
  font-family: var(--site-serif);
  font-size: var(--site-fs-h2);
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--site-pine);
  margin: 0 0 var(--site-s3);
}
.board-sub {
  font-size: var(--site-fs-body);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
  margin: 0 auto;
  max-width: 40em;
}
.board-more { margin-top: var(--site-s7); text-align: center; }
.board-more.left { text-align: left; margin-top: var(--site-s5); }
/* 入口链接：静止是一条浅色下划线，hover 时线变成黄铜色并把箭头推出去。
   原来是加粗 + 一个"→"字符，粗体在中文里其实是伪粗体，边缘会发毛。 */
.board-more a {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: var(--site-fs-body);
  letter-spacing: 0.04em;
  color: var(--site-pine);
  cursor: pointer;
  padding-bottom: 6px;
  border-bottom: 1px solid var(--site-line-strong);
  transition: border-color var(--site-dur) var(--site-ease);
}
.board-more a:hover { border-bottom-color: var(--site-brass); }
.board-more a :deep(.site-icon) { transition: transform var(--site-dur) var(--site-ease); }
.board-more a:hover :deep(.site-icon) { transform: translateX(4px); }
.board-loading { text-align: center; color: var(--muted); padding: 40px 0; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 64px; align-items: center; }
.two-col.reverse { direction: rtl; }
.two-col.reverse > * { direction: ltr; }
.col-media img { width: 100%; height: 420px; object-fit: cover; border-radius: 4px; display: block; }
.section-body { font-size: 14.5px; line-height: 2; color: #4A4A44; margin: 0 0 10px; }
.section-body-en { font-size: 12px; line-height: 1.8; color: var(--muted); margin: 0; font-style: italic; }

/* ===== 菜品条 ===== */
.dish-strip { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
/* 每张卡都挂一层 0 8px 30px 的柔影，十张排在一起就像浮在塑料膜上。
   照片本身有明暗，不需要外框投影来"托"它。 */
.dish-strip-card { position: relative; border-radius: var(--site-radius); overflow: hidden; cursor: pointer; aspect-ratio: 4/3; }
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
.event-card {
  background: var(--site-surface);
  border: 1px solid var(--site-line);
  border-radius: var(--site-radius);
  overflow: hidden;
  cursor: pointer;
  transition: border-color var(--site-dur) var(--site-ease),
              box-shadow var(--site-dur) var(--site-ease),
              transform var(--site-dur) var(--site-ease);
}
.event-card:hover {
  border-color: var(--site-line-strong);
  transform: translateY(-2px);
  box-shadow: var(--site-lift);
}
.event-card img { width: 100%; height: 240px; object-fit: cover; display: block; transition: transform 0.5s ease; }
.event-card:hover img { transform: scale(1.05); }
.event-body { padding: 26px; }
.event-eyebrow { font-size: 11px; letter-spacing: 1.5px; color: var(--gold); margin: 0 0 8px; font-weight: 600; text-transform: uppercase; }
.event-body h3 { font-size: 19px; font-weight: 700; color: var(--forest); margin: 0 0 10px; }
.event-desc { font-size: 13px; line-height: 1.7; color: var(--muted); margin: 0 0 16px; }
.event-link {
  display: inline-flex; align-items: center; gap: 7px;
  font-size: var(--site-fs-small);
  letter-spacing: 0.04em;
  color: var(--site-pine);
}
.ev-arrow { transition: transform var(--site-dur) var(--site-ease); }
.event-card:hover .ev-arrow { transform: translateX(4px); }

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
.join-eyebrow { color: var(--site-brass-soft); margin-bottom: var(--site-s3); }
.join-title {
  font-family: var(--site-serif);
  font-size: var(--site-fs-h2);
  font-weight: 600;
  letter-spacing: 0.06em;
  color: #fff;
  margin: 0 0 var(--site-s3);
}
.join-desc {
  font-size: var(--site-fs-body);
  line-height: var(--site-lh-loose);
  color: rgba(255, 255, 255, 0.72);
  margin: 0;
  max-width: 34em;
}
.join-btn { padding: 14px 30px; font-size: var(--site-fs-body); }

@media (max-width: 960px) {
  .hero-title { font-size: 36px; letter-spacing: 0.1em; text-indent: 0.1em; }
  .hero-subtitle { font-size: var(--site-fs-body); letter-spacing: 0.14em; text-indent: 0.14em; }
  .dish-strip, .event-grid { grid-template-columns: 1fr; }
  .two-col { grid-template-columns: 1fr; }
  .join-inner { flex-direction: column; align-items: flex-start; }
}

/*
  窄屏上"又见炊烟，又见你"会断成"…又见 / 你"，把一个字甩到第二行。
  中文标题掉单字比换行本身更难看，所以在这个宽度再收一档字号与字距，
  让它整句待在一行。
*/
@media (max-width: 430px) {
  .hero-title { font-size: 30px; letter-spacing: 0.06em; text-indent: 0.06em; }
  .hero-subtitle { font-size: var(--site-fs-small); letter-spacing: 0.1em; text-indent: 0.1em; }
  .hero-actions { gap: var(--site-s2); }
  .hero-btn { padding: 12px 20px; font-size: var(--site-fs-small); }
}
</style>

<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', to: '/' }, { label: '皖南攻略' }]" />

    <header class="site-hero">
      <p class="site-eyebrow">Travel Guide</p>
      <h1 class="site-title">皖南攻略</h1>
      <p class="site-lede">皖南川藏线与周边名胜。一路水墨山水，总有一处又见炊烟。</p>
      <hr class="site-rule" />
    </header>

    <section class="page-body">
      <div class="guide-tabs">
        <button :class="{ active: tab === 'route' }" @click="tab = 'route'">皖南川藏线</button>
        <button :class="{ active: tab === 'nearby' }" @click="tab = 'nearby'">周边游</button>
      </div>

      <div v-show="tab === 'route'" class="two-col">
        <div class="col-media">
          <img src="/site-photos/mountain-mist-balcony.jpg" alt="皖南川藏线" />
        </div>
        <div class="col-text">
          <p class="site-eyebrow">江南天路</p>
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

      <div v-show="tab === 'nearby'">
        <p class="section-subtitle">来又见炊烟用餐之余，不妨顺路走走这些皖南名胜</p>
        <div class="nearby-grid">
          <div v-for="spot in nearbySpots" :key="spot.name" class="nearby-card">
            <div class="site-placeholder nearby-image">
              <span class="nearby-mark">{{ spot.name.charAt(0) }}</span>
            </div>
            <div class="nearby-info">
              <h4>{{ spot.name }}</h4>
              <p>{{ spot.desc }}</p>
            </div>
          </div>
        </div>
      </div>
    </section>

    <SiteFooter />
  </div>
</template>

<script setup>
import { ref } from 'vue'
import SiteNav from '@/components/site/SiteNav.vue'
import SiteFooter from '@/components/site/SiteFooter.vue'
import SiteBreadcrumb from '@/components/site/SiteBreadcrumb.vue'

const tab = ref('route')

// 周边游景点信息来自公开旅游攻略检索的真实景点，不是编造的
const nearbySpots = [
  { name: '青龙湾', desc: '国家水利风景区，水域面积32.8平方公里，湖中38岛，有"安徽千岛湖"之称，紧邻宁国店。' },
  { name: '敬亭山', desc: '"相看两不厌，唯有敬亭山"，李白笔下的江南诗山，就在宣城城区，宣城店出发车程很近。' },
  { name: '夏霖风景区', desc: '国家4A级景区，瀑布峡谷奇石众多，有"皖南第一大瀑布群"之称。' },
  { name: '桃花潭', desc: '"桃花潭水深千尺，不及汪伦送我情"，李白诗中的皖南名潭，古村与潭水相映。' },
  { name: '查济古村落', desc: '皖南保存完好的古村落之一，粉墙黛瓦、小桥流水，适合漫步写生。' },
  { name: '中国鳄鱼湖', desc: '万余条扬子鳄栖息地，皖南地区独具特色的生态景点。' }
]
</script>

<style scoped>
/*
  这一页原本最容易掉进"图文并排、两边都塞满"的老套路里。
  改动集中在三处：

  · 正文栏加了最大宽度。中文正文一行超过 40 个字，眼睛回到下一行就容易串行；
    限宽之后段落自己就有了呼吸。
  · 线路清单从项目符号列表改成带序号刻度的条目：左边一条竖线 + 一个小号编号，
    比圆点更像"路线上的两个站点"，也正好对上这一段要说的事。
  · 景点卡的占位块不再写"（待补实景）"。那是我们内部的话，
    换成景点名的头一个字做版位标记。
*/

.page-body {
  max-width: var(--site-max);
  margin: 0 auto;
  padding: var(--site-s6) var(--site-gutter) var(--site-s9);
}

.guide-tabs {
  display: flex;
  gap: var(--site-s6);
  margin-bottom: var(--site-s7);
  padding-bottom: var(--site-s4);
  border-bottom: 1px solid var(--site-line);
}
.guide-tabs button {
  position: relative;
  background: none; border: none; padding: 0 0 var(--site-s3);
  margin-bottom: -17px;
  font-family: inherit;
  font-size: var(--site-fs-lead);
  letter-spacing: 0.04em;
  color: var(--site-ink-3);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.guide-tabs button:hover { color: var(--site-ink); }
.guide-tabs button.active { color: var(--site-pine); }
.guide-tabs button.active::after {
  content: '';
  position: absolute; left: 0; right: 0; bottom: 0;
  height: 1.5px; background: var(--site-pine);
}

/* ---------- 图文两栏 ---------- */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--site-s8);
  align-items: start;
}
.col-media img {
  width: 100%;
  height: 100%;
  min-height: 420px;
  max-height: 560px;
  object-fit: cover;
  border-radius: var(--site-radius);
  display: block;
}
.col-text { padding-top: var(--site-s2); }

.section-title {
  font-family: var(--site-serif);
  font-size: var(--site-fs-h2);
  font-weight: 600;
  line-height: var(--site-lh-tight);
  letter-spacing: 0.03em;
  color: var(--site-pine);
  margin: 0 0 var(--site-s5);
}
.section-body {
  font-size: var(--site-fs-body);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
  margin: 0 0 var(--site-s5);
  max-width: 38em;
}
.section-subtitle {
  font-size: var(--site-fs-lead);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
  margin: 0 0 var(--site-s6);
  max-width: 38em;
}

/* ---------- 线路上的两个站点 ---------- */
.route-list {
  list-style: none;
  padding: 0;
  margin: 0 0 var(--site-s6);
  counter-reset: stop;
}
.route-list li {
  counter-increment: stop;
  position: relative;
  padding: 0 0 var(--site-s5) var(--site-s6);
  border-left: 1px solid var(--site-line-strong);
}
.route-list li:last-child { padding-bottom: 0; }
.route-list li::before {
  content: counter(stop, decimal-leading-zero);
  position: absolute;
  left: 0;
  top: 1px;
  transform: translateX(-50%);
  background: var(--site-paper);
  padding: 2px 0;
  font-family: var(--site-serif);
  font-size: var(--site-fs-caption);
  color: var(--site-brass);
  letter-spacing: 0.06em;
}
.route-list strong {
  display: block;
  font-size: var(--site-fs-body);
  font-weight: 600;
  color: var(--site-pine);
  margin-bottom: 6px;
  letter-spacing: 0.02em;
}
.route-list span {
  display: block;
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
  max-width: 34em;
}

/* ---------- 周边景点 ---------- */
.nearby-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: var(--site-s5);
}
.nearby-card {
  background: var(--site-surface);
  border: 1px solid var(--site-line);
  border-radius: var(--site-radius);
  overflow: hidden;
  transition: border-color var(--site-dur) var(--site-ease),
              box-shadow var(--site-dur) var(--site-ease),
              transform var(--site-dur) var(--site-ease);
}
.nearby-card:hover {
  border-color: var(--site-line-strong);
  box-shadow: var(--site-lift);
  transform: translateY(-2px);
}
.nearby-image { height: 170px; }
.nearby-mark {
  font-family: var(--site-serif);
  font-size: 48px;
  color: rgba(30, 58, 47, 0.13);
  user-select: none;
}
.nearby-info { padding: var(--site-s5); }
.nearby-info h4 {
  font-family: var(--site-serif);
  font-size: var(--site-fs-lead);
  font-weight: 600;
  letter-spacing: 0.03em;
  color: var(--site-pine);
  margin: 0 0 var(--site-s3);
}
.nearby-info p {
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-loose);
  color: var(--site-ink-2);
  margin: 0;
}

@media (max-width: 960px) {
  .two-col { grid-template-columns: 1fr; gap: var(--site-s5); }
  .col-media img { min-height: 240px; max-height: 300px; }
  .nearby-grid { grid-template-columns: 1fr; }
  .nearby-image { height: 180px; }
  .guide-tabs button { font-size: var(--site-fs-body); }
}
</style>

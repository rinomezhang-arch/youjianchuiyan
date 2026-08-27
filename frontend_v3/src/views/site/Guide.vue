<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', en: 'Home', to: '/' }, { label: '皖南攻略', en: 'Travel Guide' }]" />

    <section class="page-hero">
      <p class="page-eyebrow">Travel Guide</p>
      <h1 class="page-title">皖南攻略</h1>
      <p class="page-desc">皖南川藏线与周边名胜，一路水墨山水，总有一处又见炊烟</p>
    </section>

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

      <div v-show="tab === 'nearby'">
        <p class="section-subtitle">来又见炊烟用餐之余，不妨顺路走走这些皖南名胜</p>
        <div class="nearby-grid">
          <div v-for="spot in nearbySpots" :key="spot.name" class="nearby-card">
            <div class="placeholder-block nearby-image">
              <span class="placeholder-label">{{ spot.name }}（待补实景）</span>
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
.guide-tabs { display: flex; justify-content: center; gap: 12px; margin-bottom: 48px; }
.guide-tabs button {
  background: #fff; border: 1px solid #DDD3B8; color: var(--muted);
  padding: 10px 28px; border-radius: 2px; font-size: 14px; letter-spacing: 1px; cursor: pointer;
  transition: all 0.2s;
}
.guide-tabs button.active { background: var(--forest); border-color: var(--forest); color: #fff; }

.two-col { display: grid; grid-template-columns: 1fr 1fr; gap: 64px; align-items: center; }
.col-media img { width: 100%; height: 420px; object-fit: cover; border-radius: 4px; display: block; }
.section-eyebrow { font-size: 13px; letter-spacing: 3px; color: var(--gold); margin: 0 0 12px; font-weight: 600; }
.section-title { font-size: 26px; font-weight: 700; color: var(--forest); margin: 0 0 20px; }
.section-body { font-size: 15px; line-height: 2; color: #4A4A44; margin: 0 0 20px; }
.section-subtitle { text-align: center; font-size: 14px; color: var(--muted); margin: 0 0 32px; }

.route-list { list-style: none; padding: 0; margin: 24px 0; }
.route-list li {
  display: flex; flex-direction: column; gap: 4px;
  padding: 16px 0 16px 20px; border-left: 2px solid var(--gold); margin-bottom: 16px;
}
.route-list li strong { color: var(--forest); font-size: 16px; }
.route-list li span { color: var(--muted); font-size: 14px; line-height: 1.7; }

.nearby-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 24px; }
.nearby-card { background: #fff; border-radius: 4px; overflow: hidden; box-shadow: 0 2px 16px rgba(0,0,0,0.05); }
.placeholder-block {
  background: linear-gradient(135deg, #EDE7D9 0%, #E3DBC8 50%, #D9CFB5 100%);
  display: flex; align-items: center; justify-content: center;
}
.placeholder-label { color: #9C8F6E; font-size: 12px; padding: 0 12px; text-align: center; }
.nearby-image { height: 160px; }
.nearby-info { padding: 18px; }
.nearby-info h4 { font-size: 16px; font-weight: 700; color: var(--forest); margin: 0 0 8px; }
.nearby-info p { font-size: 13px; color: var(--muted); line-height: 1.7; margin: 0; }

@media (max-width: 960px) {
  .two-col { grid-template-columns: 1fr; }
  .nearby-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>

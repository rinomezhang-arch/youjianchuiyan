<template>
  <div class="site-page">
    <SiteNav solid />
    <SiteBreadcrumb :items="[{ label: '首页', to: '/' }, { label: '门店' }]" />

    <header class="site-hero">
      <p class="site-eyebrow">Our Restaurants</p>
      <h1 class="site-title">门店</h1>
      <p class="site-lede">两家店，同一套手艺。选一家进去看菜单、看环境、看套餐，也可以直接预定。</p>
      <hr class="site-rule" />
    </header>

    <main class="page-body">
      <div v-if="loading" class="store-grid">
        <div v-for="n in 2" :key="n" class="store-card is-skeleton">
          <div class="store-image sk-block"></div>
          <div class="store-info">
            <div class="sk-line sk-line--title"></div>
            <div class="sk-line"></div>
            <div class="sk-line sk-line--short"></div>
          </div>
        </div>
      </div>

      <p v-else-if="!stores.length" class="empty">门店信息暂时取不到，请稍后再看，或直接致电门店。</p>

      <div v-else class="store-grid">
        <article
          v-for="s in stores"
          :key="s.storeId"
          class="store-card site-card"
          tabindex="0"
          @click="$router.push(`/stores/${s.storeId}`)"
          @keyup.enter="$router.push(`/stores/${s.storeId}`)"
        >
          <div class="store-image site-placeholder">
            <span class="site-placeholder__mark">又见炊烟</span>
          </div>
          <div class="store-info">
            <h2 class="store-name">{{ s.storeName }}</h2>
            <p class="store-detail">
              <SiteIcon name="pin" />
              <span>{{ s.address || '地址请致电门店确认' }}</span>
            </p>
            <p class="store-detail">
              <SiteIcon name="clock" />
              <span>{{ s.businessHours || '营业时间请致电门店确认' }}</span>
            </p>
            <span class="store-enter">
              进入门店
              <SiteIcon name="arrow-right" :size="15" class="enter-arrow" />
            </span>
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
/*
  改动的三处，都是冲着"塑料感"去的：

  1) 标题区从居中改成左对齐。居中 + 全大写英文小标题 + 粗体大标题，
     是套模板的写法，谁用都一样。左对齐 + 拉开字号跨度 + 一条 40px 的黄铜细线，
     版面立刻安静下来，也更像有人认真排过。

  2) 图片位不再是米色渐变块。渐变是"塑料感"最直接的来源——
     真实的照片没有那种均匀的斜向过渡。换成宣纸底加极淡的斜纹，
     里面放品牌字，看着像预留的版位，而不是没做完的补丁。
     顺便把"门店实景待补"删掉：那是给我们自己看的话，不该端给客人。

  3) 加载态从一行"加载中..."换成骨架屏。前者会让版面在数据到达时整个跳一下。
*/

.page-body {
  max-width: var(--site-max);
  margin: 0 auto;
  padding: var(--site-s7) var(--site-gutter) var(--site-s9);
}

.store-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: var(--site-s6);
}

.store-card { overflow: hidden; cursor: pointer; }
.store-card:focus-visible { outline: 2px solid var(--site-pine); outline-offset: 3px; }

.store-image { height: 260px; }

.store-info { padding: var(--site-s6) var(--site-s6) var(--site-s5); }

.store-name {
  font-family: var(--site-serif);
  font-size: var(--site-fs-h3);
  font-weight: 600;
  letter-spacing: 0.04em;
  color: var(--site-pine);
  margin: 0 0 var(--site-s4);
}

.store-detail {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-normal);
  color: var(--site-ink-2);
  margin: 0 0 10px;
}
.store-detail :deep(.site-icon) { margin-top: 3px; color: var(--site-ink-3); }

.store-enter {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  margin-top: var(--site-s4);
  padding-top: var(--site-s4);
  border-top: 1px solid var(--site-line);
  width: 100%;
  font-size: var(--site-fs-small);
  letter-spacing: 0.04em;
  color: var(--site-pine);
}
.enter-arrow { transition: transform var(--site-dur) var(--site-ease); }
.store-card:hover .enter-arrow { transform: translateX(4px); }

.empty {
  text-align: center;
  color: var(--site-ink-3);
  font-size: var(--site-fs-body);
  padding: var(--site-s9) 0;
  margin: 0;
}

/* ---------- 骨架屏 ---------- */
.is-skeleton { pointer-events: none; }
.sk-block, .sk-line {
  background: var(--site-surface-2);
  position: relative;
  overflow: hidden;
}
.sk-block { height: 260px; }
.sk-line { height: 12px; border-radius: 2px; margin-bottom: 12px; }
.sk-line--title { height: 20px; width: 45%; margin-bottom: 20px; }
.sk-line--short { width: 62%; }
.sk-block::after, .sk-line::after {
  content: '';
  position: absolute; inset: 0;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.7), transparent);
  transform: translateX(-100%);
  animation: sk-sweep 1.4s infinite;
}
@keyframes sk-sweep { to { transform: translateX(100%); } }

/* 用户开了"减少动态效果"就不要再扫了 */
@media (prefers-reduced-motion: reduce) {
  .sk-block::after, .sk-line::after { animation: none; }
}

@media (max-width: 960px) {
  .store-grid { grid-template-columns: 1fr; gap: var(--site-s5); }
  .store-image, .sk-block { height: 200px; }
  .store-info { padding: var(--site-s5) var(--site-s5) var(--site-s4); }
}
</style>

<template>
  <footer class="footer">
    <div class="footer-inner">
      <div class="footer-col footer-brand-col">
        <div class="brand">
          <img src="/logo.png" alt="又见炊烟私房菜" class="brand-mark" />
          <div class="brand-text">
            <span class="brand-cn">又见炊烟</span>
            <span class="brand-en">私房菜 · Private Kitchen</span>
          </div>
        </div>
        <p class="footer-tagline">私房手艺，本地时令食材，二十余年。</p>
      </div>

      <div class="footer-col">
        <h4 class="col-title">门店</h4>
        <div v-for="s in stores" :key="s.storeId" class="footer-store">
          <a class="footer-store-name" @click="router.push(`/stores/${s.storeId}`)">{{ s.storeName }}</a>
          <p class="footer-store-line">
            <SiteIcon name="pin" :size="14" />
            <span>{{ s.address }}</span>
          </p>
          <p class="footer-store-line" v-if="s.phone">
            <SiteIcon name="phone" :size="14" />
            <span>{{ s.phone }}</span>
          </p>
        </div>
      </div>

      <div class="footer-col">
        <h4 class="col-title">快速入口</h4>
        <a class="footer-link" @click="router.push('/menu')">臻选菜品</a>
        <a class="footer-link" @click="router.push('/packages')">宴会套餐</a>
        <a class="footer-link" @click="router.push('/guide')">皖南攻略</a>
        <a class="footer-link" @click="router.push('/self-service')">加入我们</a>
        <a class="footer-link footer-link--quiet" @click="router.push('/login')">员工登录</a>
      </div>
    </div>

    <div class="footer-bottom">
      <p>© {{ year }} 又见炊烟私房菜</p>
    </div>
  </footer>
  <EllaChatWidget />
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import EllaChatWidget from '@/components/site/EllaChatWidget.vue'
import SiteIcon from '@/components/site/SiteIcon.vue'
import request from '@/utils/request'

const router = useRouter()
const stores = ref([])
const year = new Date().getFullYear()

onMounted(async () => {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = (res.data || []).map(s => ({
      storeId: s.store_id ?? s.storeId,
      storeName: s.store_name ?? s.storeName,
      address: s.address,
      phone: s.phone
    }))
  } catch (e) {
    stores.value = []
  }
})
</script>

<style scoped>
/*
  页脚原先每个栏目标题、每条链接下面都挂一行小号英文，
  一屏底部塞进去二十多个英文碎片，谁也不看，只是把版面弄脏。
  这一版只留中文，英文收进品牌标记那一处。
  地址和电话前面的 emoji 换成描边图标，颜色跟着文字走。
*/

.footer {
  background: var(--site-pine-ink);
  padding: var(--site-s9) var(--site-gutter) var(--site-s5);
  font-family: var(--site-sans);
}
.footer-inner {
  max-width: var(--site-max);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 1.6fr 1fr 0.8fr;
  gap: var(--site-s8);
  padding-bottom: var(--site-s7);
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}

.brand { display: flex; align-items: center; gap: 14px; margin-bottom: var(--site-s4); }
.brand-mark { width: 40px; height: 40px; object-fit: contain; flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-cn {
  font-family: var(--site-serif);
  font-size: 19px; font-weight: 600; color: #fff;
  letter-spacing: 0.14em; line-height: 1;
}
.brand-en {
  font-size: var(--site-fs-micro); color: rgba(255, 255, 255, 0.5);
  letter-spacing: 0.14em; line-height: 1;
}
.footer-tagline {
  color: rgba(255, 255, 255, 0.55);
  font-size: var(--site-fs-small);
  line-height: var(--site-lh-loose);
  margin: 0;
  max-width: 24em;
}

.col-title {
  color: #fff;
  font-size: var(--site-fs-caption);
  font-weight: 500;
  letter-spacing: 0.18em;
  text-transform: uppercase;
  margin: 0 0 var(--site-s5);
  padding-bottom: var(--site-s3);
  border-bottom: 1px solid rgba(255, 255, 255, 0.09);
}

.footer-store { margin-bottom: var(--site-s5); }
.footer-store:last-child { margin-bottom: 0; }
.footer-store-name {
  display: inline-block;
  color: #fff;
  font-size: var(--site-fs-body);
  margin-bottom: var(--site-s2);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.footer-store-name:hover { color: var(--site-brass-soft); }
.footer-store-line {
  display: flex; align-items: flex-start; gap: 8px;
  font-size: var(--site-fs-small);
  color: rgba(255, 255, 255, 0.5);
  line-height: var(--site-lh-normal);
  margin: 0 0 4px;
}
.footer-store-line :deep(.site-icon) { margin-top: 3px; opacity: 0.75; }

.footer-link {
  display: block;
  color: rgba(255, 255, 255, 0.68);
  font-size: var(--site-fs-small);
  margin-bottom: var(--site-s4);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.footer-link:hover { color: var(--site-brass-soft); }
.footer-link--quiet { color: rgba(255, 255, 255, 0.38); }

.footer-bottom {
  max-width: var(--site-max);
  margin: 0 auto;
  padding-top: var(--site-s5);
}
.footer-bottom p {
  color: rgba(255, 255, 255, 0.32);
  font-size: var(--site-fs-caption);
  letter-spacing: 0.04em;
  margin: 0;
}

@media (max-width: 960px) {
  .footer { padding-top: var(--site-s7); }
  .footer-inner { grid-template-columns: 1fr; gap: var(--site-s6); }
}
</style>

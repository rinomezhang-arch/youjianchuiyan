<template>
  <div class="m-me">
    <div class="me-hero">
      <img src="/logo.png" class="me-logo" alt="logo" />
      <div class="me-brand-cn">又见炊烟私房菜</div>
      <div class="me-brand-en">YOUJIANCHUIYAN · PRIVATE KITCHEN</div>
      <div class="me-tagline">私房手艺 · 本地时令食材 · 二十余年</div>
    </div>

    <div class="me-section">
      <div class="me-section-title">门店信息</div>
      <div v-for="s in stores" :key="s.store_id" class="store-block">
        <div class="store-block-name">{{ s.store_name }}</div>
        <div class="store-block-row"><SiteIcon name="pin" :size="14" />{{ s.address }}</div>
        <div class="store-block-row"><SiteIcon name="clock" :size="14" />{{ s.business_hours }}</div>
        <div class="store-block-actions">
          <a class="store-action" :href="`tel:${s.phone}`">拨打电话 {{ s.phone }}</a>
        </div>
      </div>
    </div>

    <div class="me-section">
      <div class="me-section-title">常用功能</div>
      <div class="menu-list">
        <div class="menu-item" @click="$router.push('/m/menu')">
          <span>臻选菜品</span><SiteIcon name="chevron-right" :size="15" class="menu-arrow" />
        </div>
        <div class="menu-item" @click="$router.push('/m/packages')">
          <span>宴会套餐</span><SiteIcon name="chevron-right" :size="15" class="menu-arrow" />
        </div>
        <div class="menu-item" @click="$router.push('/m/book')">
          <span>立即预定</span><SiteIcon name="chevron-right" :size="15" class="menu-arrow" />
        </div>
        <div class="menu-item" @click="openEla">
          <span>在线咨询</span><SiteIcon name="chevron-right" :size="15" class="menu-arrow" />
        </div>
        <div class="menu-item" @click="goDesktop">
          <span>查看电脑版官网</span><SiteIcon name="chevron-right" :size="15" class="menu-arrow" />
        </div>
      </div>
    </div>

    <div class="me-footer">© {{ year }} 又见炊烟私房菜</div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import request from '@/utils/request'

const stores = ref([])
const year = new Date().getFullYear()

async function loadStores() {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = res.data || []
  } catch (e) {
    stores.value = []
  }
}

function openEla() {
  const btn = document.querySelector('.ella-trigger')
  if (btn) btn.dispatchEvent(new MouseEvent('click', { bubbles: true }))
}

function goDesktop() {
  window.location.href = '/'
}

onMounted(loadStores)
</script>

<style scoped>
.m-me {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #8A8478;
  min-height: 100%;
  background: var(--ivory);
}

.me-hero {
  background: linear-gradient(135deg, var(--forest), #2C4E3D);
  padding: 34px 20px 26px;
  text-align: center;
  color: #fff;
}
.me-logo { width: 56px; height: 56px; object-fit: contain; margin-bottom: 10px; }
.me-brand-cn { font-family: var(--site-serif); font-size: 19px; font-weight: 600; letter-spacing: 0.1em; }
.me-brand-en { font-size: 9px; letter-spacing: 2px; color: #D4B483; margin-top: 4px; }
.me-tagline { font-size: 11.5px; color: rgba(255,255,255,0.75); margin-top: 10px; }

.me-section { padding: 18px 16px 4px; }
.me-section-title {
  font-size: var(--site-fs-caption); letter-spacing: 0.16em; text-transform: uppercase;
  color: var(--site-brass); font-weight: 500; margin-bottom: 12px;
}

.store-block { background: #fff; border-radius: 10px; padding: 14px; margin-bottom: 10px; box-shadow: 0 1px 6px rgba(0,0,0,0.05); }
.store-block-name { font-family: var(--site-serif); font-size: var(--site-fs-lead); font-weight: 600; letter-spacing: 0.03em; color: var(--site-pine); margin-bottom: 8px; }
.store-block-row { display: flex; align-items: flex-start; gap: 7px; }
.store-block-row { font-size: var(--site-fs-small); color: var(--site-ink-2); margin-bottom: 5px; line-height: var(--site-lh-normal); }
.store-block-row :deep(.site-icon) { margin-top: 3px; color: var(--site-ink-3); }
.store-block-actions { margin-top: 8px; }
.store-action { display: inline-block; font-size: var(--site-fs-small); color: var(--site-pine); text-decoration: none; }

.menu-list { background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.05); }
.menu-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px; font-size: 13.5px; color: var(--ink);
  border-bottom: 1px solid #F0EAD9;
}
.menu-item:last-child { border-bottom: none; }
.menu-arrow { color: var(--muted); }

.me-footer { text-align: center; font-size: 10.5px; color: var(--muted); padding: 24px 0 30px; }
</style>

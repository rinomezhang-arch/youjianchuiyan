<template>
  <div class="m-shell">
    <div class="m-content">
      <router-view />
    </div>

    <nav class="m-tabbar">
      <button
        v-for="t in tabs"
        :key="t.path"
        class="m-tab"
        :class="{ active: isActive(t.path), raised: t.raised }"
        @click="go(t.path)"
      >
        <span class="m-tab-icon" v-html="t.icon"></span>
        <span class="m-tab-label">{{ t.label }}</span>
      </button>
    </nav>

    <EllaChatWidget :bottom-offset="78" />
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import EllaChatWidget from '@/components/site/EllaChatWidget.vue'

const router = useRouter()
const route = useRoute()

const tabs = [
  { path: '/m', label: '首页', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M3 11l9-8 9 8"/><path d="M5 10v10h14V10"/></svg>' },
  { path: '/m/menu', label: '菜单', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><path d="M4 6h16M4 12h16M4 18h10"/></svg>' },
  { path: '/m/book', label: '预定', icon: '<svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="10"/><path d="M12 7v10M7 12h10"/></svg>', raised: true },
  { path: '/m/packages', label: '套餐', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><rect x="3" y="7" width="18" height="13" rx="1"/><path d="M3 11h18M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>' },
  { path: '/m/me', label: '我的', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.5-7 8-7s8 3 8 7"/></svg>' }
]

function isActive(path) {
  if (path === '/m') return route.path === '/m'
  return route.path.startsWith(path)
}
function go(path) {
  if (route.path !== path) router.push(path)
}
</script>

<style scoped>
.m-shell {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #8A8478;
  min-height: 100vh;
  min-height: 100dvh;
  background: var(--ivory);
  font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
  color: var(--ink);
}

.m-content {
  padding-bottom: 74px;
  min-height: 100vh;
  min-height: 100dvh;
}

.m-tabbar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 62px;
  background: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(31, 58, 46, 0.08);
  display: flex;
  align-items: center;
  z-index: 900;
  padding-bottom: env(safe-area-inset-bottom, 0);
}

.m-tab {
  flex: 1;
  height: 100%;
  border: none;
  background: none;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: var(--muted);
  cursor: pointer;
}
.m-tab-icon { display: flex; }
.m-tab-label { font-size: 10.5px; letter-spacing: 0.5px; }
.m-tab.active { color: var(--forest); }
.m-tab.active .m-tab-label { font-weight: 600; }

.m-tab.raised {
  position: relative;
}
.m-tab.raised .m-tab-icon {
  width: 46px;
  height: 46px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--forest), #2C4E3D);
  color: #fff;
  align-items: center;
  justify-content: center;
  margin-top: -22px;
  box-shadow: 0 6px 16px rgba(31, 58, 46, 0.35);
  border: 3px solid var(--ivory);
}
.m-tab.raised.active .m-tab-icon {
  background: linear-gradient(135deg, var(--gold), #A17E48);
}
.m-tab.raised .m-tab-label { margin-top: 1px; }
</style>

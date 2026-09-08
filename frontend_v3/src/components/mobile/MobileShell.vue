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
  // 中间这颗原来是个加号。加号的意思是"新增一条"，可这里点进去是选日期订位，
  // 用日历更准，也不会跟点菜页的加菜按钮混。
  { path: '/m/book', label: '预定', icon: '<svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"><rect x="3.5" y="5" width="17" height="16" rx="1.5"/><path d="M3.5 10h17M8 3v4M16 3v4"/></svg>', raised: true },
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
/*
  这层壳原先自己重写了一遍 --forest/--gold/--ivory，跟站点其余地方三份不同的绿。
  现在统一吃 site-tokens。

  另外两处改动都是冲着"塑料感"去的：
  · 中间那颗凸起按钮原来是 135deg 的绿色渐变，选中时又换成金色渐变。
    一个 46px 的小圆上做渐变，只会看出一层塑料光泽，没有立体感。改成平色。
  · 阴影从 0 6px 16px / 0.35 收到很淡的一层。它需要的是"浮在栏上面"这一点点提示，
    不是一坨黑影。
*/

.m-shell {
  /* 旧变量名先留着，指向新令牌。/m 下还有几页在用 var(--forest) 这类写法，
     一次性删掉会让那几页的颜色直接失效。等它们逐页换完再摘掉这四行。 */
  --forest: var(--site-pine);
  --gold: var(--site-brass);
  --ivory: var(--site-paper);
  --muted: var(--site-ink-3);

  min-height: 100vh;
  min-height: 100dvh;
  background: var(--site-paper);
  font-family: var(--site-sans);
  color: var(--site-ink);
  -webkit-font-smoothing: antialiased;
}

.m-content {
  padding-bottom: 74px;
  min-height: 100vh;
  min-height: 100dvh;
}

.m-tabbar {
  position: fixed;
  left: 0; right: 0; bottom: 0;
  height: 62px;
  background: rgba(255, 255, 255, 0.94);
  backdrop-filter: blur(14px);
  border-top: 1px solid var(--site-line);
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
  gap: 3px;
  color: var(--site-ink-3);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.m-tab-icon { display: flex; }
.m-tab-label { font-size: 10.5px; letter-spacing: 0.06em; }
.m-tab.active { color: var(--site-pine); }
/*
  选中态原来把标签加粗。中文伪粗体会让字宽变化，切换标签时整条栏轻微抖一下。
  改成只换颜色，字宽不变。
*/

.m-tab.raised { position: relative; }
.m-tab.raised .m-tab-icon {
  width: 46px; height: 46px;
  border-radius: 50%;
  background: var(--site-pine);
  color: #fff;
  align-items: center;
  justify-content: center;
  margin-top: -22px;
  box-shadow: 0 4px 12px -4px rgba(12, 20, 16, 0.4);
  border: 3px solid var(--site-paper);
  transition: background-color var(--site-dur) var(--site-ease);
}
.m-tab.raised.active .m-tab-icon { background: var(--site-pine-2); }
.m-tab.raised .m-tab-label { margin-top: 1px; }
</style>

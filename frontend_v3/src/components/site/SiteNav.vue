<template>
  <header class="nav" :class="{ scrolled: isScrolled || solid }">
    <div class="nav-inner">
      <div class="brand" @click="router.push('/')">
        <img src="/logo.png" alt="又见炊烟私房菜" class="brand-mark" />
        <div class="brand-text">
          <span class="brand-cn">又见炊烟</span>
          <span class="brand-en">私房菜 · Private Kitchen</span>
        </div>
      </div>

      <nav class="nav-links">
        <a @click="router.push('/menu')">臻选菜品</a>

        <div class="nav-dropdown" @mouseenter="openStoreMenu" @mouseleave="scheduleCloseStoreMenu">
          <a class="has-caret">
            门店
            <SiteIcon name="chevron-down" :size="14" class="caret" :class="{ open: storeMenuOpen }" />
          </a>
          <transition name="drop">
            <div class="dropdown-panel" v-show="storeMenuOpen">
              <a v-for="s in stores" :key="s.storeId" class="dropdown-item" @click="goStore(s)">
                <span class="dropdown-item-name">{{ s.storeName }}</span>
                <span class="dropdown-item-hint">{{ s.address || '菜单 · 环境 · 套餐 · 预定' }}</span>
              </a>
              <a class="dropdown-all" @click="router.push('/stores')">
                全部门店<SiteIcon name="arrow-right" :size="14" />
              </a>
            </div>
          </transition>
        </div>

        <a @click="router.push('/packages')">宴会套餐</a>
        <a @click="router.push('/guide')">皖南攻略</a>
      </nav>

      <div class="nav-actions">
        <button class="site-btn site-btn--on-dark nav-btn" @click="router.push('/login')">登录</button>
        <button class="site-btn nav-btn nav-btn--book" @click="router.push('/stores')">立即预定</button>
      </div>

      <button class="nav-burger" :class="{ open: mobileOpen }" @click="mobileOpen = !mobileOpen" aria-label="菜单">
        <span></span><span></span><span></span>
      </button>
    </div>

    <!-- 移动端菜单：桌面导航在小屏幕上直接隐藏，这里补一份可展开的入口，否则手机访客够不到任何子页面 -->
    <transition name="sheet">
      <div v-if="mobileOpen" class="mobile-panel">
        <a class="mp-item" @click="goMobile('/menu')">臻选菜品</a>
        <div class="mobile-stores">
          <p class="mobile-stores-label">门店</p>
          <a v-for="s in stores" :key="s.storeId" class="mobile-store-item" @click="goMobile(`/stores/${s.storeId}`)">
            {{ s.storeName }}
          </a>
          <a class="mobile-store-item mobile-store-all" @click="goMobile('/stores')">
            全部门店<SiteIcon name="arrow-right" :size="14" />
          </a>
        </div>
        <a class="mp-item" @click="goMobile('/packages')">宴会套餐</a>
        <a class="mp-item" @click="goMobile('/guide')">皖南攻略</a>
        <a class="mp-item mp-quiet" @click="goMobile('/login')">登录</a>
      </div>
    </transition>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import SiteIcon from '@/components/site/SiteIcon.vue'
import request from '@/utils/request'

// solid: 子页面没有透明大图 hero，导航需要一开始就是深色实底，不能等滚动才变色
defineProps({ solid: { type: Boolean, default: false } })

const router = useRouter()
const isScrolled = ref(false)
const storeMenuOpen = ref(false)
const mobileOpen = ref(false)

function goMobile(path) {
  mobileOpen.value = false
  router.push(path)
}
const stores = ref([])

function onScroll() {
  isScrolled.value = window.scrollY > 40
}

function goStore(s) {
  storeMenuOpen.value = false
  router.push(`/stores/${s.storeId}`)
}

// 下拉面板是绝对定位，跟触发链接之间有个视觉间隙——鼠标斜着移过去时很容易
// 中途离开两者的实际几何范围，mouseleave 立刻触发把菜单关掉，根本来不及点。
// 改成延迟关闭：mouseleave 先排个队，如果这个延迟窗口内又 mouseenter（哪怕是进了
// 下拉面板本身）就取消关闭。
let closeTimer = null
function openStoreMenu() {
  if (closeTimer) { clearTimeout(closeTimer); closeTimer = null }
  storeMenuOpen.value = true
}
function scheduleCloseStoreMenu() {
  closeTimer = setTimeout(() => { storeMenuOpen.value = false }, 250)
}

async function loadStores() {
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
  }
}

onMounted(() => {
  window.addEventListener('scroll', onScroll)
  loadStores()
})
onUnmounted(() => {
  window.removeEventListener('scroll', onScroll)
})
</script>

<style scoped>
/*
  这一版把三样"塑料味"的东西去掉了：
  1) 每个链接下面那行小号英文。中英并排看着是"国际化"，实际是把每个元素都变成两行噪点，
     视线没有落点。中文站就让中文当主角，英文只留在品牌标记上。
  2) 满屏 text-shadow。给字加投影是为了压住背景，可背景本来就有一层暗渐变兜底，
     再叠投影只会让字发虚发脏，像贴纸。
  3) 大面积金色按钮。金色一铺开就土；这里主按钮改成白底墨绿字，金色只留一条 1px 的线。
*/

.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  transition: background-color var(--site-dur) var(--site-ease),
              padding var(--site-dur) var(--site-ease),
              border-color var(--site-dur) var(--site-ease);
  /* 不用纯透明：首图亮部（天空/玻璃反光）会让白色文字读不清，任何情况下都保留一层暗渐变兜底 */
  background: linear-gradient(180deg, rgba(10, 18, 14, 0.62) 0%, rgba(10, 18, 14, 0.28) 55%, rgba(10, 18, 14, 0) 100%);
  border-bottom: 1px solid transparent;
  padding-bottom: 40px;
}
.nav.scrolled {
  background: var(--site-pine-ink);
  border-bottom-color: rgba(255, 255, 255, 0.09);
  padding-bottom: 0;
}

.nav-inner {
  max-width: var(--site-max-wide);
  margin: 0 auto;
  padding: 20px 48px;
  display: flex;
  align-items: center;
  gap: 48px;
}

/* ---------- 品牌 ---------- */
.brand { cursor: pointer; display: flex; align-items: center; gap: 14px; flex-shrink: 0; }
.brand-mark { width: 44px; height: 44px; object-fit: contain; flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; gap: 3px; }
.brand-cn {
  font-family: var(--site-serif);
  font-size: 21px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 0.14em;
  line-height: 1;
}
.brand-en {
  font-size: var(--site-fs-micro);
  color: rgba(255, 255, 255, 0.58);
  letter-spacing: 0.14em;
  line-height: 1;
}

/* ---------- 主导航 ---------- */
.nav-links { display: flex; gap: 40px; align-items: center; margin-left: auto; }
.nav-links > a,
.nav-dropdown > a {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 14.5px;
  letter-spacing: 0.06em;
  color: rgba(255, 255, 255, 0.88);
  cursor: pointer;
  padding: 6px 0;
  transition: color var(--site-dur) var(--site-ease);
}
.nav-links > a:hover,
.nav-dropdown > a:hover { color: #fff; }

/* hover 时从左侧长出一条黄铜细线——金色只在这个尺度上出现 */
.nav-links > a::after,
.nav-dropdown > a::after {
  content: '';
  position: absolute;
  left: 0; right: 100%; bottom: 0;
  height: 1px;
  background: var(--site-brass-soft);
  transition: right var(--site-dur) var(--site-ease);
}
.nav-links > a:hover::after,
.nav-dropdown:hover > a::after { right: 0; }

.caret { transition: transform var(--site-dur) var(--site-ease); opacity: 0.7; }
.caret.open { transform: rotate(180deg); }

.nav-dropdown { position: relative; }
.dropdown-panel {
  position: absolute;
  top: calc(100% + 14px);
  left: 50%;
  transform: translateX(-50%);
  background: var(--site-surface);
  border: 1px solid var(--site-line);
  border-radius: var(--site-radius);
  box-shadow: var(--site-lift-strong);
  min-width: 296px;
  padding: var(--site-s2);
}
/* 面板与触发链接之间的空隙用一块透明区补上，鼠标斜着移过去不会掉出去 */
.dropdown-panel::before {
  content: '';
  position: absolute;
  left: 0; right: 0; top: -14px; height: 14px;
}
.dropdown-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 13px 16px;
  border-radius: var(--site-radius);
  cursor: pointer;
  transition: background-color var(--site-dur) var(--site-ease);
}
.dropdown-item:hover { background: var(--site-surface-2); }
.dropdown-item-name { font-size: 14px; font-weight: 600; color: var(--site-pine); }
.dropdown-item-hint {
  font-size: var(--site-fs-caption);
  color: var(--site-ink-3);
  line-height: var(--site-lh-normal);
  /* 地址可能很长，一行放不下就截断，别把面板撑成一堵墙 */
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.dropdown-all {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: var(--site-s1);
  padding: 12px 16px 10px;
  border-top: 1px solid var(--site-line);
  font-size: var(--site-fs-small);
  color: var(--site-brass);
  cursor: pointer;
  transition: color var(--site-dur) var(--site-ease);
}
.dropdown-all:hover { color: var(--site-pine); }

/* ---------- 右侧动作 ---------- */
.nav-actions { display: flex; gap: 10px; align-items: center; flex-shrink: 0; }
.nav-btn { padding: 10px 20px; font-size: var(--site-fs-small); }
.nav-btn--book {
  background: #fff;
  color: var(--site-pine);
  border-color: #fff;
}
.nav-btn--book:hover { background: var(--site-brass-soft); border-color: var(--site-brass-soft); color: var(--site-pine-ink); }

/* ---------- 汉堡：点开时变成叉，省一个图标也少一次误解 ---------- */
.nav-burger {
  display: none;
  flex-direction: column;
  justify-content: center;
  gap: 5px;
  width: 32px; height: 32px;
  background: none; border: none; cursor: pointer; padding: 0;
  margin-left: auto;
}
.nav-burger span {
  display: block; width: 22px; height: 1.5px; background: #fff;
  transition: transform var(--site-dur) var(--site-ease), opacity var(--site-dur) var(--site-ease);
}
.nav-burger.open span:nth-child(1) { transform: translateY(6.5px) rotate(45deg); }
.nav-burger.open span:nth-child(2) { opacity: 0; }
.nav-burger.open span:nth-child(3) { transform: translateY(-6.5px) rotate(-45deg); }

/* ---------- 移动端面板 ---------- */
.mobile-panel {
  background: var(--site-pine-ink);
  padding: var(--site-s2) var(--site-gutter) var(--site-s5);
  display: flex; flex-direction: column;
}
.mp-item {
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  letter-spacing: 0.04em;
  padding: 15px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
}
.mp-quiet { color: rgba(255, 255, 255, 0.6); border-bottom: none; }
.mobile-stores { border-bottom: 1px solid rgba(255, 255, 255, 0.08); padding: 14px 0 8px; }
.mobile-stores-label {
  color: rgba(255, 255, 255, 0.45);
  font-size: var(--site-fs-caption);
  letter-spacing: 0.14em;
  text-transform: uppercase;
  margin: 0 0 var(--site-s2);
}
.mobile-store-item {
  display: flex; align-items: center; gap: 6px;
  color: var(--site-brass-soft);
  font-size: 14.5px;
  padding: 10px 0 10px 14px;
  cursor: pointer;
}
.mobile-store-all { color: rgba(255, 255, 255, 0.7); }

/* ---------- 过渡 ---------- */
.drop-enter-active, .drop-leave-active { transition: opacity 140ms var(--site-ease), transform 140ms var(--site-ease); }
.drop-enter-from, .drop-leave-to { opacity: 0; transform: translateX(-50%) translateY(-6px); }
.sheet-enter-active, .sheet-leave-active { transition: opacity 160ms var(--site-ease); }
.sheet-enter-from, .sheet-leave-to { opacity: 0; }

@media (max-width: 960px) {
  .nav-links, .nav-actions { display: none; }
  .nav-burger { display: flex; }
  .nav-inner { padding: 14px var(--site-gutter); gap: 12px; }
  /* 小屏保持首屏大图透明叠加，只把兜底暗渐变收短一点；
     打开菜单时下面那块面板自己是实底，不需要整条导航变色 */
  .nav { padding-bottom: 16px; }
  .nav.scrolled { padding-bottom: 0; }
  .brand-mark { width: 34px; height: 34px; }
  .brand-cn { font-size: 17px; letter-spacing: 0.1em; }
  .brand-en { font-size: 9.5px; }
}
</style>

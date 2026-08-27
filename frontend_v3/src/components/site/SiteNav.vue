<template>
  <header class="nav" :class="{ scrolled: isScrolled || solid }">
    <div class="nav-inner">
      <div class="brand" @click="router.push('/')">
        <img src="/logo.png" alt="又见炊烟私房菜 Logo" class="brand-mark" />
        <div class="brand-text">
          <span class="brand-cn">又见炊烟私房菜</span>
          <span class="brand-en">YOUJIANCHUIYAN · PRIVATE KITCHEN</span>
        </div>
      </div>
      <nav class="nav-links">
        <a @click="router.push('/menu')">
          <span class="nl-cn">臻选菜品</span>
          <span class="nl-en">Signature Dishes</span>
        </a>

        <div class="nav-dropdown" @mouseenter="storeMenuOpen = true" @mouseleave="storeMenuOpen = false">
          <a>
            <span class="nl-cn">门店选择 <i class="caret">▾</i></span>
            <span class="nl-en">Our Restaurants</span>
          </a>
          <div class="dropdown-panel" v-show="storeMenuOpen">
            <a v-for="s in stores" :key="s.storeId" class="dropdown-item" @click="goStore(s)">
              <span class="dropdown-item-name">{{ s.storeName }}</span>
              <span class="dropdown-item-hint">菜单 · 环境 · 套餐 · 预定 · Menu · Ambiance · Reserve</span>
            </a>
            <a class="dropdown-item dropdown-all" @click="router.push('/stores')">查看全部门店 · All Restaurants →</a>
          </div>
        </div>

        <a @click="router.push('/packages')">
          <span class="nl-cn">宴会套餐</span>
          <span class="nl-en">Banquets & Celebrations</span>
        </a>
        <a @click="router.push('/guide')">
          <span class="nl-cn">皖南攻略</span>
          <span class="nl-en">Travel Guide</span>
        </a>
      </nav>
      <div class="nav-actions">
        <button class="btn-ghost" @click="router.push('/login')">登录 Login</button>
        <button class="btn-gold" @click="router.push('/stores')">立即预定 Reserve</button>
      </div>
      <button class="nav-burger" @click="mobileOpen = !mobileOpen" aria-label="菜单 Menu">
        <span></span><span></span><span></span>
      </button>
    </div>

    <!-- 移动端菜单：桌面导航在小屏幕上直接隐藏，这里补一份可展开的入口，否则手机访客够不到任何子页面 -->
    <div v-if="mobileOpen" class="mobile-panel">
      <a @click="goMobile('/menu')">臻选菜品 <span class="mp-en">Signature Dishes</span></a>
      <div class="mobile-stores">
        <p class="mobile-stores-label">门店选择 Our Restaurants</p>
        <a v-for="s in stores" :key="s.storeId" @click="goMobile(`/stores/${s.storeId}`)" class="mobile-store-item">{{ s.storeName }}</a>
        <a @click="goMobile('/stores')" class="mobile-store-item mobile-store-all">查看全部门店 · All Restaurants →</a>
      </div>
      <a @click="goMobile('/packages')">宴会套餐 <span class="mp-en">Banquets & Celebrations</span></a>
      <a @click="goMobile('/guide')">皖南攻略 <span class="mp-en">Travel Guide</span></a>
      <a @click="goMobile('/login')">登录 Login</a>
    </div>
  </header>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
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
.nav {
  position: fixed;
  top: 0; left: 0; right: 0;
  z-index: 100;
  transition: all 0.3s ease;
  /* 不用纯透明：首图亮部（天空/玻璃反光）会让白色文字读不清，任何情况下都保留一层暗渐变兜底 */
  background: linear-gradient(180deg, rgba(8,15,11,0.65) 0%, rgba(8,15,11,0.35) 60%, rgba(8,15,11,0) 100%);
  padding-bottom: 44px;
}
.nav.scrolled {
  background: rgba(22, 36, 28, 0.97);
  box-shadow: 0 2px 20px rgba(0,0,0,0.15);
  padding-bottom: 0;
}
.nav-inner {
  max-width: 1440px;
  margin: 0 auto;
  padding: 24px 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 56px;
}
.brand { cursor: pointer; display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.brand-mark { width: 56px; height: 56px; object-fit: contain; filter: drop-shadow(0 1px 4px rgba(0,0,0,0.4)); flex-shrink: 0; }
.brand-text { display: flex; flex-direction: column; line-height: 1.3; }
.brand-cn { font-size: 23px; font-weight: 700; color: #fff; letter-spacing: 1.5px; text-shadow: 0 1px 6px rgba(0,0,0,0.5); }
.brand-en { font-size: 10.5px; color: #D4B483; letter-spacing: 2.5px; text-shadow: 0 1px 6px rgba(0,0,0,0.5); margin-top: 4px; }

.nav-links { display: flex; gap: 44px; align-items: center; margin-left: auto; }
.nav-links a {
  display: flex; flex-direction: column; align-items: center; gap: 3px;
  color: #fff; cursor: pointer; opacity: 0.94; transition: opacity 0.2s;
}
.nav-links a:hover { opacity: 1; }
.nav-links a:hover .nl-cn { color: #D4B483; }
.nl-cn { font-size: 14.5px; letter-spacing: 1px; text-shadow: 0 1px 6px rgba(0,0,0,0.5); }
.nl-en { font-size: 9px; letter-spacing: 1.2px; color: rgba(255,255,255,0.62); text-shadow: 0 1px 6px rgba(0,0,0,0.5); text-transform: uppercase; }
.nav-actions { display: flex; gap: 14px; align-items: center; flex-shrink: 0; }

.nav-dropdown { position: relative; padding: 6px 0; }
.caret { font-size: 10px; font-style: normal; }
.dropdown-panel {
  position: absolute; top: 100%; left: 50%; transform: translateX(-50%);
  background: #fff; border-radius: 4px; box-shadow: 0 12px 40px rgba(0,0,0,0.18);
  min-width: 280px; padding: 10px; margin-top: 10px;
}
.dropdown-item {
  display: flex; flex-direction: column; gap: 3px;
  padding: 14px 18px; border-radius: 3px; color: #2A2A28 !important;
  opacity: 1 !important; text-align: left;
}
.dropdown-item:hover { background: #FAF7F0; }
.dropdown-item-name { font-size: 14px; font-weight: 700; color: #1F3A2E; }
.dropdown-item-hint { font-size: 10.5px; color: #7A7A72; letter-spacing: 0.3px; }
.dropdown-all { color: #B8935A !important; font-size: 12.5px; text-align: center; margin-top: 4px; border-top: 1px solid #EDE7D9; padding-top: 12px; }

.btn-ghost {
  background: rgba(0,0,0,0.15); border: 1px solid rgba(255,255,255,0.6); color: #fff;
  padding: 10px 20px; border-radius: 2px; font-size: 13px; letter-spacing: 0.5px; cursor: pointer;
  transition: all 0.2s; text-shadow: 0 1px 4px rgba(0,0,0,0.4); white-space: nowrap;
}
.btn-ghost:hover { border-color: #D4B483; color: #D4B483; }
.btn-gold {
  background: #B8935A; border: 1px solid #B8935A; color: #fff;
  padding: 10px 24px; border-radius: 2px; font-size: 13px; letter-spacing: 0.5px; cursor: pointer;
  transition: all 0.2s; display: inline-block; text-decoration: none; text-align: center; white-space: nowrap;
}
.btn-gold:hover { background: #A17E48; border-color: #A17E48; }

.nav-burger {
  display: none;
  flex-direction: column; justify-content: center; gap: 5px;
  width: 32px; height: 32px; background: none; border: none; cursor: pointer; padding: 0;
}
.nav-burger span { display: block; width: 100%; height: 2px; background: #fff; }

.mobile-panel {
  background: rgba(22, 36, 28, 0.98);
  padding: 8px 32px 24px;
  display: flex; flex-direction: column;
}
.mobile-panel > a {
  color: #fff; font-size: 15px; padding: 14px 0; border-bottom: 1px solid rgba(255,255,255,0.1);
  cursor: pointer; display: flex; align-items: baseline; gap: 10px;
}
.mp-en { font-size: 10px; color: rgba(255,255,255,0.5); letter-spacing: 0.5px; }
.mobile-stores { border-bottom: 1px solid rgba(255,255,255,0.1); padding: 10px 0; }
.mobile-stores-label { color: rgba(255,255,255,0.5); font-size: 12px; margin: 4px 0 6px; }
.mobile-store-item { display: block; color: #D4B483; font-size: 14px; padding: 8px 0 8px 12px; cursor: pointer; }
.mobile-store-all { color: rgba(255,255,255,0.7); }

@media (max-width: 960px) {
  .nav-links, .nav-actions { display: none; }
  .nav-burger { display: flex; }
  .nav-inner { padding: 16px 20px; gap: 12px; }
  .brand-mark { width: 36px; height: 36px; }
  .brand-cn { font-size: 15px; letter-spacing: 0.5px; }
  .brand-en { font-size: 7.5px; letter-spacing: 1px; }
}

@media (max-width: 400px) {
  .brand-en { display: none; }
}
</style>

<template>
  <div class="ipad-store">
    <div class="bg-layer">
      <div class="bg-blob b1"></div>
      <div class="bg-blob b2"></div>
    </div>
    <div class="container">
      <div class="hero">
        <div class="logo-mark">
          <span class="logo-char">炊</span>
        </div>
        <h1 class="brand-cn">又见炊烟私房菜</h1>
        <p class="brand-en">Youjian Chuiyan Private Kitchen</p>
        <div class="divider-line"></div>
        <p class="tagline">人间烟火味 · 最抚凡人心</p>
      </div>
      <div class="section-label">
        <span class="label-cn">选择门店</span>
        <span class="label-en">Select a Store</span>
      </div>
      <div class="store-grid">
        <div
          v-for="store in stores"
          :key="store.id"
          :class="['store-card', { active: store.status === 'open', disabled: store.status !== 'open' }]"
          @click="handleSelect(store)"
        >
          <div class="card-inner">
            <div class="store-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
            </div>
            <div class="store-name">{{ store.name }}</div>
            <div class="store-name-en">{{ store.nameEn }}</div>
            <div class="store-meta">
              <span>{{ store.tables }} Tables</span>
              <span class="dot"></span>
              <span>{{ store.capacity }} Seats</span>
            </div>
            <div class="store-status" :class="store.status">
              <span class="status-dot"></span>
              {{ store.status === 'open' ? '营业中' : '未开业' }}
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useIpadStore } from '@/store/ipad'

const router = useRouter()
const ipad = useIpadStore()

const stores = ref([
  { id: 1, name: '宁国店', nameEn: 'Ningguo', status: 'open', tables: 84, capacity: 500 },
  { id: 2, name: '宣城店', nameEn: 'Xuancheng', status: 'open', tables: 16, capacity: 120 }
])

function handleSelect(store) {
  if (store.status !== 'open') return
  ipad.selectStore(store)
  router.push('/ipad/login')
}
</script>

<style scoped>
.ipad-store {
  width: 100%;
  height: 100%;
  background: linear-gradient(160deg, #FAF8F5 0%, #F0EBE5 50%, #E8E4DE 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
}
.bg-layer { position: absolute; inset: 0; pointer-events: none; }
.bg-blob { position: absolute; border-radius: 50%; filter: blur(100px); opacity: 0.3; }
.b1 { width: 600px; height: 600px; background: rgba(45, 74, 62, 0.08); top: -200px; right: -100px; }
.b2 { width: 500px; height: 500px; background: rgba(196, 163, 90, 0.06); bottom: -200px; left: -100px; }

.container { position: relative; z-index: 1; display: flex; flex-direction: column; align-items: center; padding: 32px; }
.hero { text-align: center; margin-bottom: 40px; }
.logo-mark { width: 72px; height: 72px; border-radius: 18px; background: linear-gradient(135deg, #2D4A3E, #4A7C59); display: flex; align-items: center; justify-content: center; margin: 0 auto 16px; box-shadow: 0 8px 24px rgba(45, 74, 62, 0.25); }
.logo-char { color: #FAF8F5; font-size: 32px; font-weight: 700; font-family: var(--font-family); }
.brand-cn { font-size: 36px; font-weight: 700; color: var(--color-text); letter-spacing: 6px; margin-bottom: 4px; }
.brand-en { font-size: 13px; color: var(--color-text-muted); letter-spacing: 3px; }
.divider-line { width: 40px; height: 2px; background: var(--color-accent); margin: 16px auto; border-radius: 1px; }
.tagline { font-size: 14px; color: var(--color-text-secondary); letter-spacing: 4px; }

.section-label { display: flex; align-items: center; gap: 12px; margin-bottom: 28px; }
.label-cn { font-size: 16px; font-weight: 600; color: var(--color-text); letter-spacing: 2px; }
.label-en { font-size: 12px; color: var(--color-text-muted); letter-spacing: 1px; }

.store-grid { display: flex; gap: 24px; }
.store-card { width: 280px; background: var(--color-card); border: 1px solid var(--color-border); border-radius: var(--radius-xl); cursor: pointer; transition: all 0.3s; position: relative; overflow: hidden; }
.store-card.active:hover { transform: translateY(-4px); box-shadow: var(--shadow-lg); border-color: var(--color-primary); }
.store-card.disabled { opacity: 0.4; cursor: not-allowed; }
.card-inner { padding: 32px 24px; text-align: center; display: flex; flex-direction: column; align-items: center; }
.store-icon { width: 48px; height: 48px; border-radius: 12px; background: rgba(45, 74, 62, 0.06); display: flex; align-items: center; justify-content: center; color: var(--color-primary); margin-bottom: 14px; }
.store-icon svg { width: 24px; height: 24px; }
.store-name { font-size: 20px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; margin-bottom: 2px; }
.store-name-en { font-size: 11px; color: var(--color-text-muted); letter-spacing: 1px; margin-bottom: 12px; }
.store-meta { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--color-text-secondary); padding: 10px 0; border-top: 1px solid var(--color-border-light); width: 100%; justify-content: center; }
.dot { width: 3px; height: 3px; border-radius: 50%; background: var(--color-border); }
.store-status { display: flex; align-items: center; gap: 6px; margin-top: 10px; font-size: 12px; font-weight: 600; }
.store-status.open { color: var(--color-success); }
.store-status.closed { color: var(--color-text-muted); }
.status-dot { width: 6px; height: 6px; border-radius: 50%; background: currentColor; }
</style>

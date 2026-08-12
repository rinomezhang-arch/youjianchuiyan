<template>
  <div class="store-select">
    <div class="bg-layer">
      <div class="bg-blob b1"></div>
      <div class="bg-blob b2"></div>
      <div class="bg-dots"></div>
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
        <p class="tagline-en">The taste of home, in every dish</p>
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
          @click="handleSelectStore(store)"
        >
          <div class="card-ribbon" v-if="store.status === 'open'">OPEN</div>
          <div class="card-inner">
            <div class="store-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round">
                <path d="M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/>
                <polyline points="9 22 9 12 15 12 15 22"/>
              </svg>
            </div>
            <div class="store-name">{{ store.name }}</div>
            <div class="store-name-en">{{ store.nameEn || 'Ningguo' }}</div>
            <div class="store-addr">{{ store.address }}</div>
            <div class="store-meta">
              <span class="meta-item">{{ store.tables }} Tables</span>
              <span class="meta-dot"></span>
              <span class="meta-item">{{ store.capacity }} Seats</span>
            </div>
            <div class="store-status-badge" :class="store.status">
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getStoreList } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const stores = ref([
  { id: 1, name: '宁国店', nameEn: 'Ningguo', address: '宁国市青龙西路1号', status: 'open', tables: 13, capacity: 150 },
  { id: 2, name: '宣城店', nameEn: 'Xuancheng', address: '宣城市状元南路88号', status: 'open', tables: 15, capacity: 180 },
])

async function handleSelectStore(store) {
  if (store.status !== 'open') {
    ElMessage.warning('该门店暂未开业 / Not open yet')
    return
  }
  userStore.selectStore(store)
  if (userStore.token) {
    router.push('/dashboard')
  } else {
    router.push('/login')
  }
}

onMounted(async () => {
  try {
    const res = await getStoreList()
    if (res.code === 200 && res.data?.length) {
      stores.value = res.data
    }
  } catch (e) {
    console.log('Using default store data')
  }
})
</script>

<style scoped>
.store-select {
  min-height: 100vh;
  background: linear-gradient(160deg, #F8FAFC 0%, #EFF3F8 40%, #E8EEF4 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

/* 背景装饰 */
.bg-layer {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bg-blob {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.4;
}

.b1 {
  width: 500px;
  height: 500px;
  background: rgba(79, 70, 229, 0.06);
  top: -150px;
  right: -150px;
}

.b2 {
  width: 400px;
  height: 400px;
  background: rgba(99, 102, 241, 0.04);
  bottom: -120px;
  left: -100px;
}

.bg-dots {
  position: absolute;
  inset: 0;
  background-image: radial-gradient(rgba(79,70,229,0.06) 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: 0.5;
}

/* 主容器 */
.container {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 900px;
  padding: 40px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

/* 品牌区 */
.hero {
  text-align: center;
  margin-bottom: 48px;
}

.logo-mark {
  width: 64px;
  height: 64px;
  border-radius: 16px;
  background: linear-gradient(135deg, #4F46E5, #7C3AED);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
  box-shadow: 0 8px 24px rgba(79,70,229,0.25);
}

.logo-char {
  color: #fff;
  font-size: 28px;
  font-weight: 700;
}

.brand-cn {
  font-size: 32px;
  font-weight: 700;
  color: var(--color-text);
  letter-spacing: 4px;
  margin-bottom: 6px;
}

.brand-en {
  font-size: 13px;
  color: var(--color-text-muted);
  letter-spacing: 2px;
  text-transform: uppercase;
  font-weight: 400;
}

.divider-line {
  width: 40px;
  height: 2px;
  background: linear-gradient(90deg, transparent, var(--color-primary), transparent);
  margin: 16px auto;
  border-radius: 1px;
}

.tagline {
  font-size: 14px;
  color: var(--color-text-secondary);
  letter-spacing: 3px;
  margin-bottom: 4px;
}

.tagline-en {
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 1px;
  font-style: italic;
}

/* 门店选择标签 */
.section-label {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 24px;
}

.label-cn {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  letter-spacing: 2px;
}

.label-en {
  font-size: 12px;
  color: var(--color-text-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
}

/* 门店卡片 */
.store-grid {
  display: flex;
  justify-content: center;
  gap: 20px;
  flex-wrap: wrap;
  width: 100%;
}

.store-grid .store-card {
  width: 280px;
  max-width: 100%;
}

.store-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  cursor: pointer;
  transition: all 0.35s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  overflow: hidden;
}

.store-card.active {
  border-color: rgba(79,70,229,0.2);
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
}

.store-card.active:hover {
  transform: translateY(-6px);
  box-shadow: 0 16px 40px rgba(79,70,229,0.12);
  border-color: var(--color-primary);
}

.store-card.disabled {
  opacity: 0.45;
  cursor: not-allowed;
  filter: grayscale(0.5);
}

.card-ribbon {
  position: absolute;
  top: 12px;
  right: -28px;
  background: linear-gradient(135deg, #4F46E5, #6366F1);
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 1px;
  padding: 4px 32px;
  transform: rotate(45deg);
  box-shadow: 0 2px 8px rgba(79,70,229,0.3);
}

.card-inner {
  padding: 36px 24px 28px;
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.store-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(79,70,229,0.06);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--color-primary);
  margin-bottom: 16px;
  transition: var(--transition);
}

.store-card.active:hover .store-icon {
  background: rgba(79,70,229,0.12);
  transform: scale(1.05);
}

.store-icon svg {
  width: 24px;
  height: 24px;
}

.store-name {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 2px;
  letter-spacing: 2px;
}

.store-name-en {
  font-size: 11px;
  color: var(--color-text-muted);
  letter-spacing: 1px;
  text-transform: uppercase;
  margin-bottom: 12px;
}

.store-addr {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-bottom: 14px;
  line-height: 1.4;
}

.store-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 0;
  border-top: 1px solid var(--color-border-light);
  width: 100%;
  justify-content: center;
}

.meta-item {
  font-size: 12px;
  color: var(--color-text-secondary);
  font-weight: 500;
}

.meta-dot {
  width: 3px;
  height: 3px;
  border-radius: 50%;
  background: var(--color-border);
}

.store-status-badge {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 12px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 1px;
}

.store-status-badge.open {
  color: #10B981;
}

.store-status-badge.closed {
  color: var(--color-text-muted);
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

@media (max-width: 768px) {
  .store-grid {
    grid-template-columns: 1fr;
    max-width: 360px;
  }
  .brand-cn {
    font-size: 26px;
  }
}
</style>

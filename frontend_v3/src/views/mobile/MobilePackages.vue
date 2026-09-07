<template>
  <div class="m-pkg">
    <div class="m-topbar">
      <div class="store-switch">
        <button v-for="s in stores" :key="s.store_id" :class="{ active: storeId === s.store_id }" @click="switchStore(s.store_id)">
          {{ s.store_short_name || s.store_name }}
        </button>
      </div>
    </div>

    <div v-if="loading" class="m-loading">加载中…</div>
    <div v-else-if="!packages.length" class="m-loading">这家门店暂时还没有套餐信息，直接找 Ella 咨询也可以</div>
    <div v-else class="pkg-list">
      <div v-for="p in packages" :key="p.package_id" class="pkg-card" @click="openDetail(p)">
        <div class="pkg-photo">
          <span class="pkg-occasion">{{ occasionLabel(p.occasion_type) }}</span>
        </div>
        <div class="pkg-body">
          <div class="pkg-name">{{ p.package_name }}</div>
          <div class="pkg-meta">{{ p.min_guests }}-{{ p.max_guests }}人 · {{ p.dish_count }}道菜</div>
          <div class="pkg-price-row">
            <span class="pkg-price">¥{{ formatPrice(p.price) }}</span>
            <span v-if="p.original_price && p.original_price > p.price" class="pkg-original">¥{{ formatPrice(p.original_price) }}</span>
          </div>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="detail" class="detail-mask" @click.self="detail = null">
        <div class="detail-card">
          <div class="detail-body">
            <div class="detail-name">{{ detail.package_name }}</div>
            <div class="detail-price">¥{{ formatPrice(detail.price) }}</div>
            <div class="detail-meta">{{ detail.min_guests }}-{{ detail.max_guests }}人 · {{ detail.dish_count }}道菜 · {{ occasionLabel(detail.occasion_type) }}</div>
            <p v-if="detail.description" class="detail-desc">{{ detail.description }}</p>
            <button class="detail-book" @click="goBook(detail)">预定这个套餐 · Reserve</button>
          </div>
          <button class="detail-close" @click="detail = null">×</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const stores = ref([])
const storeId = ref(1)
const packages = ref([])
const loading = ref(true)
const detail = ref(null)

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

const occasionLabels = {
  WEDDING: '婚宴',
  BIRTHDAY_ELDER: '寿宴',
  GRADUATION: '升学宴',
  BUSINESS: '商务宴请',
  BABY_MOON: '满月宴'
}
function occasionLabel(t) {
  return occasionLabels[t] || '宴会'
}

async function loadStores() {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = res.data || []
    if (stores.value.length) storeId.value = stores.value[0].store_id
  } catch (e) {
    stores.value = []
  }
}

async function loadPackages() {
  loading.value = true
  try {
    const res = await request.get('/api/public/packages', { params: { storeId: storeId.value } })
    packages.value = res.data || []
  } catch (e) {
    packages.value = []
  } finally {
    loading.value = false
  }
}

function switchStore(id) {
  storeId.value = id
  loadPackages()
}

function openDetail(p) {
  detail.value = p
}

function goBook(p) {
  router.push({ path: '/m/book', query: { storeId: storeId.value, dish: p.package_name } })
}

onMounted(async () => {
  await loadStores()
  loadPackages()
})
</script>

<style scoped>
.m-pkg {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #8A8478;
  min-height: 100%;
  background: var(--ivory);
}

.m-topbar { position: sticky; top: 0; z-index: 20; background: var(--ivory); padding: 14px 14px 10px; }
.store-switch { display: flex; gap: 8px; }
.store-switch button {
  flex: 1; background: #fff; border: 1px solid #E3DBC8; color: var(--muted);
  padding: 8px 0; border-radius: 8px; font-size: 12.5px;
}
.store-switch button.active { background: var(--forest); border-color: var(--forest); color: #fff; font-weight: 600; }

.m-loading { text-align: center; color: var(--muted); font-size: 13px; padding: 40px 24px; line-height: 1.6; }

.pkg-list { padding: 0 14px 24px; display: flex; flex-direction: column; gap: 12px; }
.pkg-card { background: #fff; border-radius: 12px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.05); display: flex; }
.pkg-photo {
  width: 100px; flex-shrink: 0; background: linear-gradient(135deg, var(--forest), #2C4E3D);
  display: flex; align-items: center; justify-content: center; padding: 8px;
}
.pkg-occasion { color: #D4B483; font-size: 12px; font-weight: 600; text-align: center; }
.pkg-body { padding: 12px 14px; flex: 1; }
.pkg-name { font-size: 14.5px; font-weight: 700; color: var(--forest); }
.pkg-meta { font-size: 11.5px; color: var(--muted); margin: 5px 0; }
.pkg-price-row { display: flex; align-items: baseline; gap: 8px; }
.pkg-price { font-size: 16px; font-weight: 700; color: var(--gold); }
.pkg-original { font-size: 11.5px; color: var(--muted); text-decoration: line-through; }

.detail-mask { position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000; display: flex; align-items: flex-end; }
.detail-card { position: relative; width: 100%; background: #fff; border-radius: 16px 16px 0 0; max-height: 80vh; overflow-y: auto; }
.detail-body { padding: 26px 20px 28px; }
.detail-name { font-size: 19px; font-weight: 700; color: var(--forest); }
.detail-price { font-size: 20px; font-weight: 700; color: var(--gold); margin: 8px 0 4px; }
.detail-meta { font-size: 12.5px; color: var(--muted); }
.detail-desc { font-size: 13px; color: var(--ink); line-height: 1.7; margin: 14px 0 20px; }
.detail-book { width: 100%; background: var(--forest); color: #fff; border: none; padding: 13px 0; border-radius: 24px; font-size: 14px; font-weight: 600; }
.detail-close { position: absolute; top: 10px; right: 10px; width: 30px; height: 30px; border-radius: 50%; background: rgba(0,0,0,0.4); color: #fff; border: none; font-size: 16px; }

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

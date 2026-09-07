<template>
  <div class="m-menu">
    <div class="m-topbar">
      <div class="store-switch">
        <button v-for="s in stores" :key="s.store_id" :class="{ active: storeId === s.store_id }" @click="switchStore(s.store_id)">
          {{ s.store_short_name || s.store_name }}
        </button>
      </div>
      <div class="search-box">
        <input v-model="keyword" placeholder="搜菜名…" />
      </div>
    </div>

    <div class="cat-chips">
      <button :class="{ active: activeCat === '' }" @click="activeCat = ''">全部</button>
      <button v-for="c in categories" :key="c" :class="{ active: activeCat === c }" @click="activeCat = c">{{ c }}</button>
    </div>

    <div v-if="loading" class="m-loading">加载中…</div>
    <div v-else-if="!filteredDishes.length" class="m-loading">没有找到相关菜品</div>
    <div v-else class="dish-grid">
      <div v-for="d in filteredDishes" :key="d.dish_id" class="dish-item" @click="openDetail(d)">
        <div class="dish-photo" :style="photoStyle(d)">
          <span v-if="!REAL_PHOTOS[d.dish_name]" class="dish-fallback">{{ d.dish_name }}</span>
        </div>
        <div class="dish-item-info">
          <div class="dish-item-name">{{ d.dish_name }}</div>
          <div class="dish-item-cat">{{ d.dish_category }}</div>
          <div class="dish-item-price">¥{{ formatPrice(d.sale_price) }}</div>
        </div>
      </div>
    </div>

    <Transition name="fade">
      <div v-if="detail" class="detail-mask" @click.self="detail = null">
        <div class="detail-card">
          <div class="detail-photo" :style="photoStyle(detail)">
            <span v-if="!REAL_PHOTOS[detail.dish_name]" class="dish-fallback">{{ detail.dish_name }}</span>
          </div>
          <div class="detail-body">
            <div class="detail-name">{{ detail.dish_name }}</div>
            <div v-if="detail.dish_name_en" class="detail-name-en">{{ detail.dish_name_en }}</div>
            <div class="detail-price">¥{{ formatPrice(detail.sale_price) }}</div>
            <p v-if="detail.dish_intro" class="detail-intro">{{ detail.dish_intro }}</p>
            <button class="detail-book" @click="goBookWithDish(detail)">加入预定 · Reserve with this</button>
          </div>
          <button class="detail-close" @click="detail = null">×</button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import request from '@/utils/request'

const router = useRouter()
const stores = ref([])
const storeId = ref(1)
const dishes = ref([])
const keyword = ref('')
const activeCat = ref('')
const loading = ref(true)
const detail = ref(null)

const REAL_PHOTOS = {
  '剁椒鱼头': '/dish-photos/duojiao-yutou.jpg',
  '土锅黑鱼': '/dish-photos/tuguo-heiyu.jpg',
  '老豆腐蒸腊肉': '/dish-photos/laodoufu-larou.jpg'
}

function photoStyle(d) {
  const url = REAL_PHOTOS[d.dish_name]
  return url ? { backgroundImage: `url(${url})` } : {}
}

function formatPrice(v) {
  const n = Number(v)
  return Number.isFinite(n) ? n.toFixed(0) : v
}

const categories = computed(() => {
  const set = new Set(dishes.value.map(d => d.dish_category).filter(Boolean))
  return [...set]
})

const filteredDishes = computed(() => {
  return dishes.value.filter(d => {
    if (activeCat.value && d.dish_category !== activeCat.value) return false
    if (keyword.value && !d.dish_name.includes(keyword.value.trim())) return false
    return true
  })
})

async function loadStores() {
  try {
    const res = await request.get('/api/public/stores')
    stores.value = res.data || []
    if (stores.value.length) storeId.value = stores.value[0].store_id
  } catch (e) {
    stores.value = []
  }
}

async function loadDishes() {
  loading.value = true
  try {
    const res = await request.get('/api/public/menu/full', { params: { storeId: storeId.value } })
    dishes.value = res.data || []
  } catch (e) {
    dishes.value = []
  } finally {
    loading.value = false
  }
}

function switchStore(id) {
  storeId.value = id
  activeCat.value = ''
  loadDishes()
}

function openDetail(d) {
  detail.value = d
}

function goBookWithDish(d) {
  router.push({ path: '/m/book', query: { storeId: storeId.value, dish: d.dish_name } })
}

onMounted(async () => {
  await loadStores()
  loadDishes()
})
</script>

<style scoped>
.m-menu {
  --forest: #1F3A2E;
  --gold: #B8935A;
  --ivory: #FAF7F0;
  --ink: #2A2A28;
  --muted: #8A8478;
  min-height: 100%;
  background: var(--ivory);
}

.m-topbar { position: sticky; top: 0; z-index: 20; background: var(--ivory); padding: 12px 14px 8px; }
.store-switch { display: flex; gap: 8px; margin-bottom: 8px; }
.store-switch button {
  flex: 1; background: #fff; border: 1px solid #E3DBC8; color: var(--muted);
  padding: 8px 0; border-radius: 8px; font-size: 12.5px;
}
.store-switch button.active { background: var(--forest); border-color: var(--forest); color: #fff; font-weight: 600; }

.search-box input {
  width: 100%; box-sizing: border-box; border: 1px solid #E3DBC8; border-radius: 8px;
  padding: 9px 12px; font-size: 13px; background: #fff; outline: none;
}
.search-box input:focus { border-color: var(--gold); }

.cat-chips {
  display: flex; gap: 8px; overflow-x: auto; padding: 2px 14px 12px;
  -webkit-overflow-scrolling: touch;
}
.cat-chips::-webkit-scrollbar { display: none; }
.cat-chips button {
  flex-shrink: 0; background: #fff; border: 1px solid #E3DBC8; color: var(--muted);
  padding: 6px 13px; border-radius: 14px; font-size: 12px;
}
.cat-chips button.active { background: var(--gold); border-color: var(--gold); color: #fff; }

.m-loading { text-align: center; color: var(--muted); font-size: 13px; padding: 40px 0; }

.dish-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; padding: 0 14px 20px; }
.dish-item { background: #fff; border-radius: 10px; overflow: hidden; box-shadow: 0 1px 6px rgba(0,0,0,0.05); }
.dish-photo {
  height: 96px; background: linear-gradient(135deg, #EDE7D9 0%, #DDD1B0 100%);
  background-size: cover; background-position: center;
  display: flex; align-items: center; justify-content: center;
}
.dish-fallback { font-size: 11px; color: #9C8F6E; text-align: center; padding: 0 10px; }
.dish-item-info { padding: 9px 10px; }
.dish-item-name { font-size: 13px; font-weight: 600; color: var(--ink); line-height: 1.3; }
.dish-item-cat { font-size: 10.5px; color: var(--muted); margin: 3px 0; }
.dish-item-price { font-size: 13px; font-weight: 700; color: var(--gold); }

.detail-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.5); z-index: 1000;
  display: flex; align-items: flex-end;
}
.detail-card {
  position: relative; width: 100%; background: #fff; border-radius: 16px 16px 0 0;
  max-height: 80vh; overflow-y: auto;
}
.detail-photo {
  height: 200px; background: linear-gradient(135deg, #EDE7D9 0%, #DDD1B0 100%);
  background-size: cover; background-position: center;
  display: flex; align-items: center; justify-content: center;
  border-radius: 16px 16px 0 0;
}
.detail-photo .dish-fallback { font-size: 14px; }
.detail-body { padding: 18px 20px 28px; }
.detail-name { font-size: 19px; font-weight: 700; color: var(--forest); }
.detail-name-en { font-size: 11px; color: var(--muted); margin-top: 3px; }
.detail-price { font-size: 18px; font-weight: 700; color: var(--gold); margin: 8px 0; }
.detail-intro { font-size: 13px; color: var(--ink); line-height: 1.7; margin: 8px 0 18px; }
.detail-book {
  width: 100%; background: var(--forest); color: #fff; border: none;
  padding: 13px 0; border-radius: 24px; font-size: 14px; font-weight: 600;
}
.detail-close {
  position: absolute; top: 10px; right: 10px; width: 30px; height: 30px; border-radius: 50%;
  background: rgba(0,0,0,0.4); color: #fff; border: none; font-size: 16px;
}

.fade-enter-active, .fade-leave-active { transition: opacity 0.2s; }
.fade-enter-from, .fade-leave-to { opacity: 0; }
</style>

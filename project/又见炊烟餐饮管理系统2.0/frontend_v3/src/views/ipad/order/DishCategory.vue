<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.push('/ipad/home')">← 返回桌台</button>
      <h1 class="page-title">菜品分类 · Categories</h1>
    </div>
    <div class="cat-grid">
      <div v-for="cat in categories" :key="cat.category_id" class="cat-card" @click="goToDishes(cat)">
        <div class="cat-name">{{ cat.dish_category }}</div>
        <div class="cat-count">{{ cat.count || 0 }} 道</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ipadDishCategory } from '@/api/ipad'

const router = useRouter()
const categories = ref([])

function goToDishes(cat) {
  router.push({ path: '/ipad/dishes', query: { cat: cat.category_id } })
}

onMounted(async () => {
  try {
    const res = await ipadDishCategory()
    if (res.code === 200) categories.value = res.data || []
  } catch {
    categories.value = [
      { category_id: 'cold', dish_category: '凉菜', count: 12 },
      { category_id: 'hot', dish_category: '热菜', count: 38 },
      { category_id: 'soup', dish_category: '汤类', count: 8 },
      { category_id: 'staple', dish_category: '主食', count: 10 },
      { category_id: 'drink', dish_category: '饮品', count: 6 },
      { category_id: 'package', dish_category: '套餐', count: 5 },
    ]
  }
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.cat-grid { flex: 1; overflow-y: auto; padding: 24px; display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 16px; align-content: start; }
.cat-card {
  padding: 32px 20px; background: var(--color-card);
  border: 1px solid var(--color-border); border-radius: var(--radius-lg);
  text-align: center; cursor: pointer; transition: all 0.2s;
}
.cat-card:hover { border-color: var(--color-primary); box-shadow: var(--shadow-md); transform: translateY(-2px); }
.cat-name { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; margin-bottom: 4px; }
.cat-count { font-size: 13px; color: var(--color-text-muted); }
</style>

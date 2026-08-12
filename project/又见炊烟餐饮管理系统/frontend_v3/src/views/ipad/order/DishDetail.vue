<template>
  <div class="ipad-page">
    <div class="page-top">
      <button class="back-link" @click="router.back()">← 返回</button>
      <h1 class="page-title">菜品详情 · Dish Detail</h1>
    </div>
    <div class="detail-content" v-if="dish">
      <div class="detail-img">
        <img v-if="dish.cover_img || dish.image_url" :src="dish.cover_img || dish.image_url" />
        <div v-else class="img-placeholder">{{ dish.dish_name?.charAt(0) }}</div>
      </div>
      <div class="detail-info">
        <h2>{{ dish.dish_name }}</h2>
        <p class="dish-cat">{{ dish.dish_category }}</p>
        <div class="dish-tags">
          <span v-if="dish.spicy_level" class="tag spicy">{{ '🌶'.repeat(dish.spicy_level) }}</span>
          <span v-if="dish.cooking_method" class="tag">{{ dish.cooking_method }}</span>
          <span v-if="dish.taste" class="tag">{{ dish.taste }}</span>
        </div>
        <p v-if="dish.main_ingredients" class="ingredients">食材：{{ dish.main_ingredients }}</p>
        <div class="price">¥{{ Number(dish.sale_price).toFixed(0) }}</div>
        <button class="add-btn" @click="addDish">加入购物车 · Add</button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useIpadStore } from '@/store/ipad'
import { ipadDishDetail } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const ipad = useIpadStore()
const dish = ref(null)

function addDish() {
  ipad.addToCart({ dish_id: dish.value.dish_id || dish.value.id, dish_name: dish.value.dish_name, sale_price: dish.value.sale_price, unit_price: dish.value.sale_price, dish_quantity: 1 })
  ElMessage.success('已加入购物车')
  router.back()
}

onMounted(async () => {
  const dishId = route.params.dishId
  try {
    const res = await ipadDishDetail(dishId)
    if (res.code === 200) dish.value = res.data
  } catch {
    dish.value = { dish_id: dishId, dish_name: '红烧肉', dish_category: '热菜', sale_price: 68, spicy_level: 0, cooking_method: '红烧', taste: '咸甜', main_ingredients: '五花肉、冰糖、酱油' }
  }
})
</script>

<style scoped>
.ipad-page { width: 100%; height: 100%; display: flex; flex-direction: column; background: var(--color-bg); }
.page-top { padding: 16px 24px; background: var(--color-card); border-bottom: 1px solid var(--color-border); display: flex; align-items: center; gap: 16px; flex-shrink: 0; }
.back-link { border: none; background: none; color: var(--color-text-muted); font-size: 13px; cursor: pointer; }
.page-title { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 2px; }
.detail-content { flex: 1; overflow-y: auto; padding: 24px; display: flex; gap: 24px; }
.detail-img { width: 400px; height: 300px; background: var(--color-bg-alt); border-radius: var(--radius-lg); overflow: hidden; flex-shrink: 0; display: flex; align-items: center; justify-content: center; }
.detail-img img { width: 100%; height: 100%; object-fit: cover; }
.img-placeholder { font-size: 64px; font-weight: 700; color: var(--color-border); font-family: var(--font-family); }
.detail-info { flex: 1; }
.detail-info h2 { font-size: 28px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; margin-bottom: 8px; }
.dish-cat { font-size: 14px; color: var(--color-text-muted); margin-bottom: 12px; }
.dish-tags { display: flex; gap: 8px; margin-bottom: 16px; }
.tag { padding: 4px 10px; border-radius: 4px; font-size: 12px; font-weight: 500; background: var(--color-tag-green); color: var(--color-success); }
.tag.spicy { background: #FDECEC; color: #C25555; }
.ingredients { font-size: 14px; color: var(--color-text-secondary); line-height: 1.8; margin-bottom: 20px; }
.price { font-size: 36px; font-weight: 700; color: var(--color-accent-dark); margin-bottom: 24px; }
.add-btn { padding: 14px 32px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 2px; }
.add-btn:hover { transform: translateY(-1px); box-shadow: 0 4px 14px rgba(45,74,62,0.3); }
</style>

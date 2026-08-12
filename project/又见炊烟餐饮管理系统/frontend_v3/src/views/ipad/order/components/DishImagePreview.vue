<template>
  <Transition name="modal">
    <div v-if="visible" class="preview-overlay" @click="emit('close')">
      <div class="preview-container">
        <img v-if="dish?.cover_img || dish?.image_url" :src="dish.cover_img || dish.image_url" :alt="dish.dish_name" @click.stop />
        <div v-else class="placeholder">{{ dish?.dish_name?.charAt(0) }}</div>
        <div class="preview-info">
          <h2>{{ dish?.dish_name }}</h2>
          <p v-if="dish?.dish_category">{{ dish.dish_category }}</p>
          <p class="price">¥{{ Number(dish?.sale_price || 0).toFixed(0) }}</p>
        </div>
        <button class="close-overlay" @click="emit('close')">✕</button>
      </div>
    </div>
  </Transition>
</template>

<script setup>
defineProps({ visible: Boolean, dish: Object })
const emit = defineEmits(['close'])
</script>

<style scoped>
.preview-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.85); display: flex; align-items: center; justify-content: center; z-index: 1100; }
.preview-container { position: relative; max-width: 80vw; max-height: 80vh; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.preview-container img { max-width: 100%; max-height: 60vh; border-radius: var(--radius-lg); object-fit: contain; }
.placeholder { width: 300px; height: 300px; background: var(--color-bg-alt); border-radius: var(--radius-lg); display: flex; align-items: center; justify-content: center; font-size: 80px; font-weight: 700; color: var(--color-border); }
.preview-info { text-align: center; color: white; }
.preview-info h2 { font-size: 24px; font-weight: 700; }
.preview-info p { font-size: 14px; opacity: 0.7; margin-top: 4px; }
.preview-info .price { font-size: 28px; font-weight: 700; color: var(--color-accent); margin-top: 8px; }
.close-overlay { position: absolute; top: -40px; right: 0; background: none; border: none; color: white; font-size: 28px; cursor: pointer; }

.modal-enter-active, .modal-leave-active { transition: all 0.3s; }
.modal-enter-from { opacity: 0; transform: scale(0.8); }
.modal-leave-to { opacity: 0; transform: scale(0.8); }
</style>

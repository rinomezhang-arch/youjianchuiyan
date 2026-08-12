<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="num-modal">
        <div class="modal-header">
          <h3>{{ dish?.dish_name }}</h3>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <div class="price-tag">¥{{ Number(dish?.sale_price || dish?.unit_price || 0).toFixed(0) }}</div>
          <div class="qty-control">
            <button class="qty-btn" @click="qty = Math.max(1, qty - 1)">−</button>
            <span class="qty-num">{{ qty }}</span>
            <button class="qty-btn" @click="qty = Math.min(99, qty + 1)">+</button>
          </div>
          <div class="remark-input">
            <input v-model="remark" placeholder="口味备注（可选）" />
          </div>
          <div v-if="qty" class="subtotal">小计：¥{{ (Number(dish?.sale_price || dish?.unit_price || 0) * qty).toFixed(0) }}</div>
          <button class="confirm-btn" @click="handleConfirm">确认加入 · Add</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean, dish: Object })
const emit = defineEmits(['close', 'confirm'])

const qty = ref(1)
const remark = ref('')

watch(() => props.visible, (v) => { if (v) { qty.value = 1; remark.value = '' } })

function handleConfirm() {
  emit('confirm', { dish: props.dish, qty: qty.value, remark: remark.value })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.num-modal { width: 360px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 18px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.modal-body { padding: 24px; display: flex; flex-direction: column; align-items: center; gap: 16px; }
.price-tag { font-size: 36px; font-weight: 700; color: var(--color-accent-dark); }
.qty-control { display: flex; align-items: center; gap: 20px; }
.qty-btn { width: 44px; height: 44px; border-radius: 50%; border: 2px solid var(--color-border); background: var(--color-card); font-size: 22px; cursor: pointer; display: flex; align-items: center; justify-content: center; color: var(--color-text); }
.qty-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.qty-num { font-size: 28px; font-weight: 700; min-width: 48px; text-align: center; }
.remark-input { width: 100%; }
.remark-input input { width: 100%; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; }
.remark-input input:focus { border-color: var(--color-primary); outline: none; }
.subtotal { font-size: 15px; color: var(--color-text-secondary); }
.confirm-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; letter-spacing: 1px; }
.confirm-btn:hover { transform: translateY(-1px); }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>

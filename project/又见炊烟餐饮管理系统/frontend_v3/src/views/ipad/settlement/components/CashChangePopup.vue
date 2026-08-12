<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="cash-modal">
        <div class="modal-header">
          <h3>现金找零 · Cash Change</h3>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <div class="pay-amount">应付：¥{{ amount.toFixed(2) }}</div>
          <div class="form-group">
            <label>实收金额 · Received</label>
            <input v-model.number="received" type="number" placeholder="输入实收金额" @input="calcChange" />
          </div>
          <div v-if="change > 0" class="change-result">
            <span>找零</span>
            <span class="change-val">¥{{ change.toFixed(2) }}</span>
          </div>
          <div class="quick-amounts">
            <button v-for="a in quickAmounts" :key="a" class="quick-btn" @click="received = a; calcChange()">{{ a }}元</button>
          </div>
          <button class="confirm-btn" @click="handleConfirm" :disabled="received < amount">确认收款 · Confirm</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({ visible: Boolean, amount: { type: Number, default: 0 } })
const emit = defineEmits(['close', 'confirm'])

const received = ref(0)
const change = computed(() => Math.max(0, received.value - props.amount))

const quickAmounts = computed(() => {
  const base = Math.ceil(props.amount / 100) * 100
  return [base, base + 100, base + 200, base + 500]
})

watch(() => props.visible, (v) => { if (v) received.value = 0 })

function calcChange() {}
function handleConfirm() {
  emit('confirm', { received: received.value, change: change.value })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.cash-modal { width: 380px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.modal-body { padding: 20px; }
.pay-amount { text-align: center; font-size: 24px; font-weight: 700; color: var(--color-accent-dark); margin-bottom: 16px; }
.form-group { margin-bottom: 12px; }
.form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 6px; }
.form-group input { width: 100%; padding: 12px 16px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 20px; font-weight: 700; text-align: center; }
.form-group input:focus { border-color: var(--color-primary); outline: none; }
.change-result { display: flex; justify-content: space-between; align-items: center; padding: 12px 0; margin-bottom: 12px; border-top: 1px solid var(--color-border-light); font-size: 16px; }
.change-val { font-size: 28px; font-weight: 700; color: var(--color-success); }
.quick-amounts { display: flex; gap: 8px; margin-bottom: 20px; }
.quick-btn { flex: 1; padding: 8px; border: 1px solid var(--color-border); border-radius: var(--radius-sm); background: var(--color-bg); font-size: 14px; cursor: pointer; }
.quick-btn:hover { border-color: var(--color-primary); color: var(--color-primary); }
.confirm-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.confirm-btn:not(:disabled):hover { transform: translateY(-1px); }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>

<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="confirm-modal">
        <div class="modal-body">
          <div class="confirm-icon">💳</div>
          <h3>确认支付？</h3>
          <p class="amount">¥{{ amount.toFixed(2) }}</p>
          <p class="method">支付方式：{{ methodName }}</p>
          <div class="actions">
            <button class="btn-cancel" @click="emit('close')">取消</button>
            <button class="btn-confirm" @click="emit('confirm')">确认支付</button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({ visible: Boolean, amount: Number, method: String })
const emit = defineEmits(['close', 'confirm'])

const methodName = computed(() => {
  const map = { wechat: '微信支付', alipay: '支付宝', cash: '现金', card: '银行卡' }
  return map[props.method] || props.method || '—'
})
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.confirm-modal { width: 340px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-body { padding: 32px 24px; text-align: center; }
.confirm-icon { font-size: 48px; margin-bottom: 12px; }
h3 { font-size: 18px; font-weight: 700; color: var(--color-text); margin-bottom: 8px; }
.amount { font-size: 32px; font-weight: 700; color: var(--color-accent-dark); margin-bottom: 4px; }
.method { font-size: 14px; color: var(--color-text-muted); margin-bottom: 24px; }
.actions { display: flex; gap: 12px; }
.btn-cancel { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-confirm { flex: 1; padding: 12px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; }
.btn-confirm:hover { transform: translateY(-1px); }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>

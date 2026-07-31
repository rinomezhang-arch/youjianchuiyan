<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="invoice-modal">
        <div class="modal-header">
          <h3>开具发票 · Invoice</h3>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <div class="form-group">
            <label>发票抬头 · Title</label>
            <input v-model="title" placeholder="公司名称或个人" />
          </div>
          <div class="form-group">
            <label>税号 · Tax No</label>
            <input v-model="taxNo" placeholder="统一社会信用代码" />
          </div>
          <div class="form-group">
            <label>发票类型 · Type</label>
            <div class="radio-group">
              <button :class="['radio-btn', { active: type === 'personal' }]" @click="type = 'personal'">个人</button>
              <button :class="['radio-btn', { active: type === 'company' }]" @click="type = 'company'">企业</button>
            </div>
          </div>
          <div class="amount-label">开票金额：¥{{ amount.toFixed(2) }}</div>
          <button class="confirm-btn" @click="handleConfirm" :disabled="!title">确认开票 · Submit</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean, amount: { type: Number, default: 0 }, bookingId: String })
const emit = defineEmits(['close', 'confirm'])

const title = ref('')
const taxNo = ref('')
const type = ref('personal')

watch(() => props.visible, (v) => { if (v) { title.value = ''; taxNo.value = ''; type.value = 'personal' } })

function handleConfirm() {
  emit('confirm', { booking_id: props.bookingId, invoice_title: title.value, tax_no: taxNo.value, type: type.value })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.invoice-modal { width: 400px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.modal-body { padding: 20px; }
.form-group { margin-bottom: 16px; }
.form-group label { display: block; font-size: 13px; font-weight: 600; color: var(--color-text-secondary); margin-bottom: 6px; }
.form-group input { width: 100%; padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; }
.form-group input:focus { border-color: var(--color-primary); outline: none; }
.radio-group { display: flex; gap: 8px; }
.radio-btn { padding: 8px 20px; border: 1px solid var(--color-border); border-radius: 20px; background: var(--color-card); font-size: 14px; cursor: pointer; }
.radio-btn.active { background: var(--color-primary); color: white; border-color: var(--color-primary); }
.amount-label { font-size: 16px; font-weight: 700; color: var(--color-accent-dark); margin-bottom: 20px; text-align: center; }
.confirm-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.confirm-btn:not(:disabled):hover { transform: translateY(-1px); }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>

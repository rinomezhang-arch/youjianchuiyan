<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="recharge-modal">
        <div class="modal-header">
          <h3>会员充值 · Recharge</h3>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <div v-if="member" class="member-info">
            <span class="member-name">{{ member.customer_name }}</span>
            <span class="member-balance">余额：¥{{ member.member_card?.balance?.toFixed(2) || '0.00' }}</span>
          </div>
          <div class="amount-grid">
            <button v-for="a in presetAmounts" :key="a" :class="['amt-btn', { active: selectedAmount === a }]"
              @click="selectedAmount = a">¥{{ a }}</button>
            <div class="custom-amt">
              <input v-model.number="customAmount" type="number" placeholder="自定义金额" @focus="selectedAmount = 0" />
            </div>
          </div>
          <button class="confirm-btn" @click="handleConfirm" :disabled="!finalAmount">确认充值 · Submit</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch } from 'vue'

const props = defineProps({ visible: Boolean, member: Object })
const emit = defineEmits(['close', 'confirm'])

const presetAmounts = [200, 500, 1000, 2000, 5000]
const selectedAmount = ref(0)
const customAmount = ref(null)

const finalAmount = computed(() => selectedAmount.value || customAmount.value || 0)

watch(() => props.visible, (v) => { if (v) { selectedAmount.value = 0; customAmount.value = null } })

function handleConfirm() {
  emit('confirm', { customer_id: props.member?.customer_id, recharge_money: finalAmount.value, pay_type: 'wechat' })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.recharge-modal { width: 380px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.modal-body { padding: 20px; }
.member-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; padding: 12px; background: var(--color-bg); border-radius: var(--radius-sm); }
.member-name { font-size: 16px; font-weight: 700; }
.member-balance { font-size: 14px; color: var(--color-accent-dark); font-weight: 600; }
.amount-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; margin-bottom: 20px; }
.amt-btn { padding: 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 16px; font-weight: 600; cursor: pointer; }
.amt-btn.active { border-color: var(--color-primary); background: rgba(45,74,62,0.06); color: var(--color-primary); }
.custom-amt { grid-column: span 3; }
.custom-amt input { width: 100%; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 15px; text-align: center; }
.custom-amt input:focus { border-color: var(--color-primary); outline: none; }
.confirm-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>

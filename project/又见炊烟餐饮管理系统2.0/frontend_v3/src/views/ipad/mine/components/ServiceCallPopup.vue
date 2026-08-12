<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="emit('close')">
      <div class="call-modal">
        <div class="modal-header">
          <h3>呼叫服务 · Service Call</h3>
          <button class="close-btn" @click="emit('close')">✕</button>
        </div>
        <div class="modal-body">
          <div class="service-grid">
            <button v-for="s in services" :key="s.type"
              :class="['service-btn', { active: selected === s.type }]"
              @click="selected = s.type">
              <span class="service-icon">{{ s.icon }}</span>
              <span class="service-name">{{ s.name }}</span>
            </button>
          </div>
          <button class="confirm-btn" @click="handleConfirm" :disabled="!selected">
            呼叫 · Call Service
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'

const props = defineProps({ visible: Boolean, tableId: [Number, String] })
const emit = defineEmits(['close', 'confirm'])

const selected = ref('')
const services = [
  { type: 'water', name: '加水/茶', icon: '🫖' },
  { type: 'bill', name: '结账', icon: '💳' },
  { type: 'clean', name: '清理桌面', icon: '🧹' },
  { type: 'other', name: '其他服务', icon: '🔔' },
]

watch(() => props.visible, (v) => { if (v) selected.value = '' })

function handleConfirm() {
  emit('confirm', { table_id: props.tableId, service_type: selected.value })
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: center; justify-content: center; z-index: 1000; }
.call-modal { width: 380px; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.modal-body { padding: 20px; }
.service-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 20px; }
.service-btn { display: flex; flex-direction: column; align-items: center; gap: 8px; padding: 20px 12px; border: 2px solid var(--color-border); border-radius: var(--radius-lg); background: var(--color-card); cursor: pointer; }
.service-btn.active { border-color: var(--color-primary); background: rgba(45,74,62,0.04); }
.service-icon { font-size: 32px; }
.service-name { font-size: 14px; font-weight: 600; color: var(--color-text); }
.confirm-btn { width: 100%; padding: 14px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 16px; font-weight: 700; cursor: pointer; }
.confirm-btn:disabled { opacity: 0.5; cursor: not-allowed; }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from { opacity: 0; transform: scale(0.95); }
.modal-leave-to { opacity: 0; transform: scale(0.95); }
</style>

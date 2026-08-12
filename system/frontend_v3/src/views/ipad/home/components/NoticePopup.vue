<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="notice-modal">
        <div class="modal-header">
          <h3>系统通知 · Notices</h3>
          <button class="close-btn" @click="$emit('close')">✕</button>
        </div>
        <div class="notice-list">
          <div v-for="n in notices" :key="n.id" class="notice-item">
            <div class="notice-time">{{ n.create_time }}</div>
            <div class="notice-content">{{ n.content }}</div>
          </div>
          <div v-if="!notices.length" class="empty">暂无通知</div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ipadPrintConfig } from '@/api/ipad'
// 注：实际接口应为 /sys/notice/list，此处用通用的系统通知调用

const props = defineProps({ visible: Boolean })
const emit = defineEmits(['close'])
const notices = ref([])

watch(() => props.visible, async (v) => {
  if (!v) return
  try {
    // 实际应调 ipadSysNoticeList，当前降级
    notices.value = [
      { id: 1, content: '后厨通知：红烧肉沽清，请替换', create_time: '17:00' },
      { id: 2, content: 'VIP客户张先生到店，请安排包间', create_time: '16:30' },
    ]
  } catch {
    notices.value = []
  }
})
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.3); display: flex; align-items: flex-start; justify-content: flex-end; padding: 80px 24px 0 0; z-index: 1000; }
.notice-modal { width: 380px; max-height: 60vh; background: var(--color-card); border-radius: var(--radius-lg); box-shadow: var(--shadow-xl); display: flex; flex-direction: column; overflow: hidden; }
.modal-header { padding: 16px 20px; display: flex; justify-content: space-between; align-items: center; border-bottom: 1px solid var(--color-border); }
.modal-header h3 { font-size: 16px; font-weight: 700; }
.close-btn { background: none; border: none; font-size: 20px; cursor: pointer; color: var(--color-text-muted); }
.notice-list { flex: 1; overflow-y: auto; padding: 12px 20px; }
.notice-item { padding: 12px 0; border-bottom: 1px solid var(--color-border-light); }
.notice-time { font-size: 12px; color: var(--color-text-muted); margin-bottom: 4px; }
.notice-content { font-size: 14px; color: var(--color-text); line-height: 1.6; }
.empty { text-align: center; padding: 24px; color: var(--color-text-muted); font-size: 14px; }

.modal-enter-active, .modal-leave-active { transition: all 0.25s; }
.modal-enter-from, .modal-leave-to { opacity: 0; transform: translateX(20px); }
</style>

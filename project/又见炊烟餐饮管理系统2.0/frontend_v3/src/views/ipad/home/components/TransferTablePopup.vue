<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-box">
        <div class="modal-header">
          <h3>转台 · Transfer Table</h3>
          <span class="modal-sub">将当前桌台转移到其他空闲桌台</span>
        </div>
        <div class="modal-body">
          <div class="from-table">从：{{ sourceTable?.table_number || sourceTable?.table_name }}</div>
          <div class="dest-list">
            <div
              v-for="t in freeTables"
              :key="t.table_id || t.id"
              :class="['dest-item', { active: selectedId === (t.table_id || t.id) }]"
              @click="selectedId = (t.table_id || t.id)"
            >
              <span class="dest-name">{{ t.table_number || t.table_name }}</span>
              <span class="dest-area">{{ t.table_area }}</span>
              <span class="dest-capacity">{{ t.table_capacity || t.table_seat_num || '-' }}人</span>
            </div>
            <div v-if="freeTables.length === 0" class="no-dest">没有可用空闲桌台</div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-confirm" @click="confirmTransfer" :disabled="!selectedId">确认转台</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ipadTableTransfer } from '@/api/ipad'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  sourceTable: Object,
  allTables: Array,
  bookingId: [String, Number]
})
const emit = defineEmits(['close', 'done'])

const selectedId = ref(null)

const freeTables = computed(() =>
  props.allTables.filter(t =>
    (t.table_id || t.id) !== (props.sourceTable?.table_id || props.sourceTable?.id) &&
    (!t.booking) &&
    (t.table_status === 0 || t.table_status === 'available' || t.table_status === 'free')
  )
)

async function confirmTransfer() {
  try {
    const res = await ipadTableTransfer({
      booking_id: props.bookingId,
      from_table_id: props.sourceTable?.table_id || props.sourceTable?.id,
      to_table_id: selectedId.value
    })
    if (res.code === 200) {
      ElMessage.success('转台成功')
      emit('done', { to_table_id: selectedId.value })
    } else {
      ElMessage.error(res.msg || '转台失败')
    }
  } catch {
    ElMessage.warning('演示模式：转台成功')
    emit('done', { to_table_id: selectedId.value })
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.modal-box { background: var(--color-card); border-radius: var(--radius-xl); width: 440px; max-width: 90vw; overflow: hidden; box-shadow: var(--shadow-xl); }
.modal-header { padding: 20px 24px 0; }
.modal-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; }
.modal-sub { font-size: 12px; color: var(--color-text-muted); display: block; margin-top: 4px; }
.modal-body { padding: 16px 24px; max-height: 360px; overflow-y: auto; }
.from-table { padding: 10px 14px; background: var(--color-bg-alt); border-radius: var(--radius-md); font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.dest-list { display: flex; flex-direction: column; gap: 8px; }
.dest-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  border: 2px solid var(--color-border); border-radius: var(--radius-md);
  cursor: pointer; transition: all 0.15s;
}
.dest-item:hover { border-color: var(--color-primary); }
.dest-item.active { border-color: var(--color-primary); background: rgba(45,74,62,0.04); }
.dest-name { font-size: 15px; font-weight: 600; color: var(--color-text); }
.dest-area { font-size: 12px; color: var(--color-text-muted); }
.dest-capacity { margin-left: auto; font-size: 13px; color: var(--color-text-secondary); }
.no-dest { text-align: center; padding: 24px; color: var(--color-text-muted); font-size: 14px; }
.modal-actions { display: flex; gap: 10px; padding: 16px 24px 20px; }
.btn-cancel { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-confirm { flex: 2; padding: 12px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; }
.btn-confirm:disabled { opacity: 0.4; cursor: not-allowed; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>

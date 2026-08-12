<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-box">
        <div class="modal-header">
          <h3>合台 · Merge Tables</h3>
          <span class="modal-sub">将其他桌台的菜品合并到当前桌台</span>
        </div>
        <div class="modal-body">
          <div class="target-table">目标桌台：{{ targetTable?.table_number || targetTable?.table_name }}</div>
          <div class="merge-list">
            <div
              v-for="t in mergeableTables"
              :key="t.table_id || t.id"
              :class="['merge-item', { active: selectedId === (t.table_id || t.id) }]"
              @click="selectedId = (t.table_id || t.id)"
            >
              <span class="merge-name">{{ t.table_number || t.table_name }}</span>
              <span class="merge-guests" v-if="t.booking">{{ t.booking.guest_count }}人</span>
              <span class="merge-area">{{ t.table_area }}</span>
            </div>
            <div v-if="mergeableTables.length === 0" class="no-data">没有可合并的桌台</div>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-confirm" @click="confirmMerge" :disabled="!selectedId">确认合并</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  visible: Boolean,
  targetTable: Object,
  allTables: Array
})
const emit = defineEmits(['close', 'done'])

const selectedId = ref(null)

const mergeableTables = computed(() =>
  props.allTables.filter(t =>
    (t.table_id || t.id) !== (props.targetTable?.table_id || props.targetTable?.id) &&
    t.booking
  )
)

async function confirmMerge() {
  try {
    // Merge API would go here
    ElMessage.success('合台成功')
    emit('done', { merge_table_id: selectedId.value })
  } catch {
    ElMessage.warning('演示模式：合台成功')
    emit('done', { merge_table_id: selectedId.value })
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
.target-table { padding: 10px 14px; background: var(--color-bg-alt); border-radius: var(--radius-md); font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.merge-list { display: flex; flex-direction: column; gap: 8px; }
.merge-item {
  display: flex; align-items: center; gap: 12px; padding: 12px 14px;
  border: 2px solid var(--color-border); border-radius: var(--radius-md);
  cursor: pointer; transition: all 0.15s;
}
.merge-item:hover { border-color: var(--color-primary); }
.merge-item.active { border-color: var(--color-primary); background: rgba(45,74,62,0.04); }
.merge-name { font-size: 15px; font-weight: 600; color: var(--color-text); }
.merge-guests { font-size: 13px; color: var(--color-text-secondary); margin-left: auto; }
.merge-area { font-size: 12px; color: var(--color-text-muted); }
.no-data { text-align: center; padding: 24px; color: var(--color-text-muted); font-size: 14px; }
.modal-actions { display: flex; gap: 10px; padding: 16px 24px 20px; }
.btn-cancel { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-confirm { flex: 2; padding: 12px; border: none; border-radius: var(--radius-md); background: linear-gradient(135deg, var(--color-primary), var(--color-primary-light)); color: white; font-size: 14px; font-weight: 700; cursor: pointer; }
.btn-confirm:disabled { opacity: 0.4; cursor: not-allowed; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>

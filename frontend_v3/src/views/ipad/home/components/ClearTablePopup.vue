<template>
  <Transition name="modal">
    <div v-if="visible" class="modal-overlay" @click.self="$emit('close')">
      <div class="modal-box">
        <div class="modal-header">
          <h3>清台 · Clear Table</h3>
          <span class="modal-sub">确认 {{ table?.table_number || table?.table_name }} 无客，释放桌台</span>
        </div>
        <div class="modal-body">
          <div class="clear-warn">确认该桌台客人已离场？</div>
          <div v-if="hasUnpaidOrder" class="clear-tip">⚠ 有未结账订单，请先结算</div>
          <div class="form-row">
            <label>清台原因</label>
            <select v-model="reason" class="reason-select">
              <option value="finished">正常结束</option>
              <option value="cancel">客人取消</option>
              <option value="empty">空桌</option>
            </select>
          </div>
        </div>
        <div class="modal-actions">
          <button class="btn-cancel" @click="$emit('close')">取消</button>
          <button class="btn-danger" @click="confirmClear" :disabled="hasUnpaidOrder">确认清台</button>
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
  table: Object,
  bookingId: [String, Number],
  hasUnpaidOrder: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'done'])

const reason = ref('finished')

async function confirmClear() {
  try {
    // Clear table API
    ElMessage.success('清台成功')
    emit('done', { table_id: props.table?.table_id || props.table?.id, reason: reason.value })
  } catch (error) {
    ElMessage.error(error?.message || '清台失败')
  }
}
</script>

<style scoped>
.modal-overlay { position: fixed; inset: 0; background: rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; z-index: 300; }
.modal-box { background: var(--color-card); border-radius: var(--radius-xl); width: 400px; max-width: 90vw; overflow: hidden; box-shadow: var(--shadow-xl); }
.modal-header { padding: 20px 24px 0; }
.modal-header h3 { font-size: 18px; font-weight: 700; color: var(--color-text); letter-spacing: 1px; }
.modal-sub { font-size: 12px; color: var(--color-text-muted); display: block; margin-top: 4px; }
.modal-body { padding: 16px 24px; }
.clear-warn { font-size: 14px; color: var(--color-text-secondary); margin-bottom: 12px; }
.clear-tip { padding: 10px 14px; background: #FFF3E0; border-radius: var(--radius-md); font-size: 13px; color: #E65100; margin-bottom: 12px; }
.form-row { display: flex; flex-direction: column; gap: 6px; }
.form-row label { font-size: 13px; color: var(--color-text-muted); }
.reason-select { padding: 10px 14px; border: 1px solid var(--color-border); border-radius: var(--radius-md); font-size: 14px; outline: none; background: var(--color-card); }
.reason-select:focus { border-color: var(--color-primary); }
.modal-actions { display: flex; gap: 10px; padding: 16px 24px 20px; }
.btn-cancel { flex: 1; padding: 12px; border: 1px solid var(--color-border); border-radius: var(--radius-md); background: var(--color-card); font-size: 14px; cursor: pointer; }
.btn-danger { flex: 2; padding: 12px; border: none; border-radius: var(--radius-md); background: var(--color-danger); color: white; font-size: 14px; font-weight: 700; cursor: pointer; }
.btn-danger:disabled { opacity: 0.4; cursor: not-allowed; }
.modal-enter-active, .modal-leave-active { transition: opacity 0.2s; }
.modal-enter-from, .modal-leave-to { opacity: 0; }
</style>

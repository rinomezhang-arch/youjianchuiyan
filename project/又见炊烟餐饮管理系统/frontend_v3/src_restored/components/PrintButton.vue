<template>
  <el-dropdown trigger="click" @command="handleCommand" popper-class="print-dropdown">
    <button class="print-btn">
      <el-icon><Printer /></el-icon>
      <span>打印 · Print</span>
    </button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item command="confirmation">
          <div class="print-option">
            <el-icon><Document /></el-icon>
            <div class="print-option-text">
              <span class="print-option-label">预订确认单</span>
              <span class="print-option-en">Booking Confirmation</span>
            </div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item command="table_sign">
          <div class="print-option">
            <el-icon><Grid /></el-icon>
            <div class="print-option-text">
              <span class="print-option-label">桌签</span>
              <span class="print-option-en">Table Sign</span>
            </div>
          </div>
        </el-dropdown-item>
        <el-dropdown-item command="cancellation">
          <div class="print-option">
            <el-icon><Delete /></el-icon>
            <div class="print-option-text">
              <span class="print-option-label">取消单</span>
              <span class="print-option-en">Cancellation Notice</span>
            </div>
          </div>
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>

  <PrintPreview
    :visible="previewVisible"
    :type="printType"
    :data="bookingData"
    @close="previewVisible = false"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { Printer, Document, Grid, Delete } from '@element-plus/icons-vue'
import PrintPreview from './PrintPreview.vue'

interface BookingData {
  booking_id?: string
  booking_date?: string
  booking_time?: string
  customer_name?: string
  customer_phone?: string
  occasion_type?: string
  table_count?: number
  spare_tables?: number
  guest_per_table?: number
  deposit?: string | number
  remark?: string
  booking_status?: string
  created_at?: string
  staff_name?: string
  booking_tables?: Array<{
    table_id?: number
    table_name?: string
    table_number?: string
  }>
}

const props = defineProps<{
  bookingData: BookingData
}>()

const previewVisible = ref(false)
const printType = ref<'confirmation' | 'table_sign' | 'cancellation'>('confirmation')

function handleCommand(command: string) {
  printType.value = command as 'confirmation' | 'table_sign' | 'cancellation'
  previewVisible.value = true
}
</script>

<style scoped>
.print-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #C4A35A;
  background: #FAF8F5;
  color: #2D4A3E;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.print-btn:hover {
  background: #2D4A3E;
  color: #FAF8F5;
  border-color: #2D4A3E;
}

.print-btn:active {
  transform: scale(0.97);
}

.print-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 4px 0;
}

.print-option-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.print-option-label {
  font-size: 13px;
  font-weight: 500;
  color: #2D4A3E;
}

.print-option-en {
  font-size: 11px;
  color: #888;
}
</style>

<style>
.print-dropdown .el-dropdown-menu {
  border-radius: 8px;
  border: 1px solid #e8e4df;
  box-shadow: 0 4px 16px rgba(45, 74, 62, 0.12);
  padding: 6px;
}

.print-dropdown .el-dropdown-menu__item {
  border-radius: 6px;
  padding: 8px 12px;
  transition: background 0.15s ease;
}

.print-dropdown .el-dropdown-menu__item:hover {
  background: #FAF8F5;
}

.print-dropdown .el-dropdown-menu__item .el-icon {
  color: #2D4A3E;
}
</style>

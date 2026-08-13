<template>
  <el-dialog
    :model-value="visible"
    :title="dialogTitle"
    width="700px"
    class="print-preview-dlg"
    :close-on-click-modal="false"
    :show-close="false"
    @update:model-value="handleDialogUpdate"
    @close="emit('close')"
  >
    <!-- 操作栏：打印时隐藏 -->
    <div class="print-toolbar no-print">
      <button class="print-action-btn" @click="handlePrint">
        <el-icon><Printer /></el-icon>
        <span>打印 · Print</span>
      </button>
      <button class="print-action-btn print-action-btn--cancel" @click="emit('close')">
        <el-icon><Close /></el-icon>
        <span>关闭 · Close</span>
      </button>
    </div>

    <!-- 打印内容区域 -->
    <div class="print-content" ref="printContentRef">

      <!-- ============ 预订确认单 (A5) ============ -->
      <div v-if="type === 'confirmation'" class="print-template print-a5">
        <div class="print-header">
          <div class="print-logo-line">
            <span class="print-logo-text">预订确认单</span>
          </div>
          <p class="print-subtitle">Booking Confirmation</p>
          <div class="print-divider"></div>
        </div>

        <div class="print-body">
          <div class="print-info-row">
            <span class="print-info-label">单号 · No.</span>
            <span class="print-info-value">{{ data?.booking_id || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">日期 · Date</span>
            <span class="print-info-value">{{ data?.booking_date || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">时间 · Time</span>
            <span class="print-info-value">{{ formatTime(data?.booking_time) }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">客户 · Customer</span>
            <span class="print-info-value">{{ data?.customer_name || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">电话 · Phone</span>
            <span class="print-info-value">{{ data?.customer_phone || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">宴席 · Occasion</span>
            <span class="print-info-value">{{ occasionLabel(data?.occasion_type) }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">桌数 · Tables</span>
            <span class="print-info-value">{{ data?.table_count || 0 }} 桌 + {{ data?.spare_tables || 0 }} 备桌</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">人数 · Guests</span>
            <span class="print-info-value">{{ totalGuests }} 人</span>
          </div>
          <div class="print-info-row" v-if="data?.deposit">
            <span class="print-info-label">定金 · Deposit</span>
            <span class="print-info-value">¥{{ data.deposit }}</span>
          </div>

          <div class="print-tables-section" v-if="data?.booking_tables && data.booking_tables.length > 0">
            <div class="print-section-title">桌台 · Tables</div>
            <div class="print-table-names">
              {{ data.booking_tables.map(t => t.table_name || t.table_number || '-').join('、') }}
            </div>
          </div>

          <div class="print-remark-section" v-if="data?.remark">
            <div class="print-section-title">备注 · Remark</div>
            <p class="print-remark-text">{{ data.remark }}</p>
          </div>
        </div>

        <div class="print-footer">
          <div class="print-divider"></div>
          <p class="print-footer-text">感谢您的预订 · Thank you for your booking</p>
          <p class="print-footer-time">打印时间 · Printed: {{ currentDateTime }}</p>
        </div>
      </div>

      <!-- ============ 桌签 (A6 竖版) ============ -->
      <div v-if="type === 'table_sign'" class="print-template print-a6-portrait">
        <div class="ts-container">
          <div class="ts-top-deco"></div>

          <div class="ts-main">
            <p class="ts-label-cn">已预订</p>
            <p class="ts-label-en">RESERVED</p>

            <div class="ts-divider"></div>

            <div class="ts-info-block">
              <div class="ts-info-item">
                <span class="ts-info-label">桌台 · Table</span>
                <span class="ts-info-value ts-info-value--large">
                  {{ primaryTableName }}
                </span>
              </div>
              <div class="ts-info-item">
                <span class="ts-info-label">客户 · Customer</span>
                <span class="ts-info-value">{{ data?.customer_name || '-' }}</span>
              </div>
              <div class="ts-info-item">
                <span class="ts-info-label">日期 · Date</span>
                <span class="ts-info-value">{{ data?.booking_date || '-' }}</span>
              </div>
              <div class="ts-info-item">
                <span class="ts-info-label">时间 · Time</span>
                <span class="ts-info-value">{{ formatTime(data?.booking_time) }}</span>
              </div>
              <div class="ts-info-item">
                <span class="ts-info-label">人数 · Guests</span>
                <span class="ts-info-value">{{ totalGuests }} 人</span>
              </div>
            </div>
          </div>

          <div class="ts-bottom-deco"></div>
        </div>
      </div>

      <!-- ============ 宴会通知 / 厨房单 / 定金收据 (A5) ============ -->
      <div v-if="['banquet_notice', 'kitchen_order', 'deposit_receipt'].includes(type)" class="print-template print-a5">
        <div class="print-header">
          <div class="print-logo-line"><span class="print-logo-text">{{ documentHeading.cn }}</span></div>
          <p class="print-subtitle">{{ documentHeading.en }}</p>
          <div class="print-divider"></div>
        </div>
        <div class="print-body">
          <div class="print-info-row"><span class="print-info-label">预订号 · Booking No.</span><span class="print-info-value">{{ data?.booking_id || '-' }}</span></div>
          <div class="print-info-row"><span class="print-info-label">日期时间 · Date & Time</span><span class="print-info-value">{{ data?.booking_date || '-' }} {{ formatTime(data?.booking_time) }}</span></div>
          <div class="print-info-row"><span class="print-info-label">客人 · Guest</span><span class="print-info-value">{{ data?.customer_name || '-' }} · {{ data?.customer_phone || '-' }}</span></div>
          <div class="print-info-row"><span class="print-info-label">宴会主题 · Occasion</span><span class="print-info-value">{{ occasionLabel(data?.occasion_type) }}</span></div>
          <div class="print-info-row"><span class="print-info-label">桌数 / 人数 · Tables / Pax</span><span class="print-info-value">{{ data?.table_count || 0 }} + {{ data?.spare_tables || 0 }} / {{ totalGuests }}</span></div>
          <div class="print-info-row"><span class="print-info-label">桌台 · Venue</span><span class="print-info-value">{{ primaryTableName }}</span></div>
          <div v-if="type === 'deposit_receipt'" class="cancel-notice-box">
            <p class="cancel-notice-cn">实收定金 ¥{{ data?.deposit || 0 }}</p><p class="cancel-notice-en">Deposit Received</p>
          </div>
          <div class="print-tables-section"><div class="print-section-title">特别要求 · Special Requirements</div><div class="print-table-names">{{ data?.remark || '无 · None' }}</div></div>
        </div>
        <div class="print-footer"><div class="print-divider"></div><p class="print-footer-text">经办人 · Prepared by: {{ data?.staff_name || '-' }}　打印时间 · Printed: {{ currentDateTime }}</p></div>
      </div>

      <!-- ============ 取消单 (A5) ============ -->
      <div v-if="type === 'cancellation'" class="print-template print-a5">
        <div class="print-header">
          <div class="print-logo-line">
            <span class="print-logo-text print-logo-text--cancel">预订取消单</span>
          </div>
          <p class="print-subtitle">Cancellation Notice</p>
          <div class="print-divider"></div>
        </div>

        <div class="print-body">
          <div class="cancel-notice-box">
            <p class="cancel-notice-cn">此预订已取消</p>
            <p class="cancel-notice-en">This booking has been cancelled</p>
          </div>

          <div class="print-info-row">
            <span class="print-info-label">单号 · No.</span>
            <span class="print-info-value">{{ data?.booking_id || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">日期 · Date</span>
            <span class="print-info-value">{{ data?.booking_date || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">时间 · Time</span>
            <span class="print-info-value">{{ formatTime(data?.booking_time) }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">客户 · Customer</span>
            <span class="print-info-value">{{ data?.customer_name || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">电话 · Phone</span>
            <span class="print-info-value">{{ data?.customer_phone || '-' }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">宴席 · Occasion</span>
            <span class="print-info-value">{{ occasionLabel(data?.occasion_type) }}</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">桌数 · Tables</span>
            <span class="print-info-value">{{ data?.table_count || 0 }} 桌 + {{ data?.spare_tables || 0 }} 备桌</span>
          </div>
          <div class="print-info-row">
            <span class="print-info-label">人数 · Guests</span>
            <span class="print-info-value">{{ totalGuests }} 人</span>
          </div>
          <div class="print-info-row" v-if="data?.deposit">
            <span class="print-info-label">定金 · Deposit</span>
            <span class="print-info-value">¥{{ data.deposit }}</span>
          </div>

          <div class="print-tables-section" v-if="data?.booking_tables && data.booking_tables.length > 0">
            <div class="print-section-title">桌台 · Tables</div>
            <div class="print-table-names">
              {{ data.booking_tables.map(t => t.table_name || t.table_number || '-').join('、') }}
            </div>
          </div>
        </div>

        <div class="print-footer">
          <div class="print-divider"></div>
          <p class="print-footer-text">取消时间 · Cancelled at: {{ currentDateTime }}</p>
        </div>
      </div>

    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Printer, Close } from '@element-plus/icons-vue'

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
  visible: boolean
  type: 'confirmation' | 'table_sign' | 'banquet_notice' | 'kitchen_order' | 'deposit_receipt' | 'cancellation'
  data: BookingData
}>()

const emit = defineEmits<{
  (e: 'close'): void
}>()

const printContentRef = ref<HTMLElement | null>(null)

const dialogTitle = computed(() => {
  const map: Record<string, string> = {
    confirmation: '预订确认单 · Booking Confirmation',
    table_sign: '桌签 · Table Sign',
    banquet_notice: '宴会通知单 · Banquet Event Order',
    kitchen_order: '厨房制作单 · Kitchen Production Order',
    deposit_receipt: '定金收据 · Deposit Receipt',
    cancellation: '取消单 · Cancellation Notice'
  }
  return map[props.type] || ''
})

const documentHeading = computed(() => ({
  banquet_notice: { cn: '宴会通知单', en: 'Banquet Event Order' },
  kitchen_order: { cn: '厨房制作单', en: 'Kitchen Production Order' },
  deposit_receipt: { cn: '定金收据', en: 'Deposit Receipt' }
}[props.type] || { cn: '业务单据', en: 'Operations Document' }))

const totalGuests = computed(() => {
  const tables = props.data?.table_count || 0
  const spare = props.data?.spare_tables || 0
  const perTable = props.data?.guest_per_table || 10
  return (tables + spare) * perTable
})

const primaryTableName = computed(() => {
  const tables = props.data?.booking_tables
  if (tables && tables.length > 0) {
    return tables.map(t => t.table_name || t.table_number || '-').join('、')
  }
  return '-'
})

const currentDateTime = computed(() => {
  const now = new Date()
  const y = now.getFullYear()
  const m = String(now.getMonth() + 1).padStart(2, '0')
  const d = String(now.getDate()).padStart(2, '0')
  const h = String(now.getHours()).padStart(2, '0')
  const min = String(now.getMinutes()).padStart(2, '0')
  return `${y}-${m}-${d} ${h}:${min}`
})

const occasionMap: Record<string, { cn: string; en: string }> = {
  a_la_carte: { cn: '零点', en: 'À la carte' },
  wedding: { cn: '婚宴', en: 'Wedding' },
  birthday: { cn: '生日宴', en: 'Birthday' },
  engagement: { cn: '订婚宴', en: 'Engagement' },
  baby_born: { cn: '满月宴', en: 'Baby Shower' },
  graduation: { cn: '谢师宴', en: 'Graduation' },
  help_wine: { cn: '帮忙酒', en: 'Helper\'s Wine' },
  house_move: { cn: '乔迁宴', en: 'Housewarming' },
  promotion: { cn: '升迁宴', en: 'Promotion' },
  reunion: { cn: '团圆宴', en: 'Reunion' },
  thanksgiving: { cn: '答谢宴', en: 'Thanksgiving' },
  year_end: { cn: '尾牙宴', en: 'Year-end Party' }
}

function occasionLabel(type?: string): string {
  if (!type) return '-'
  const item = occasionMap[type]
  return item ? `${item.cn} · ${item.en}` : type
}

function formatTime(time?: string): string {
  if (!time) return '-'
  return time.substring(0, 5)
}

function handlePrint() {
  window.print()
}

function handleDialogUpdate(val: boolean) {
  if (!val) {
    emit('close')
  }
}
</script>

<style scoped>
/* ============ 通用打印模板样式 ============ */
.print-template {
  background: #fff;
  color: #2D4A3E;
  font-family: 'Noto Serif SC', 'Georgia', serif;
}

.print-header {
  text-align: center;
  padding: 20px 0 12px;
}

.print-logo-line {
  display: flex;
  align-items: center;
  justify-content: center;
}

.print-logo-text {
  font-size: 22px;
  font-weight: 700;
  color: #2D4A3E;
  letter-spacing: 0.08em;
}

.print-logo-text--cancel {
  color: #8B5A3E;
}

.print-subtitle {
  font-size: 12px;
  color: #C4A35A;
  letter-spacing: 0.15em;
  text-transform: uppercase;
  margin: 4px 0 0;
  font-family: 'Georgia', serif;
}

.print-divider {
  width: 60px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #C4A35A, transparent);
  margin: 12px auto;
}

.print-body {
  padding: 0 8px;
}

.print-info-row {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  padding: 6px 0;
  border-bottom: 1px dashed #e8e4df;
}

.print-info-row:last-child {
  border-bottom: none;
}

.print-info-label {
  font-size: 12px;
  color: #888;
  flex-shrink: 0;
}

.print-info-value {
  font-size: 13px;
  font-weight: 600;
  color: #2D4A3E;
  text-align: right;
}

.print-section-title {
  font-size: 12px;
  font-weight: 600;
  color: #C4A35A;
  margin: 12px 0 6px;
  letter-spacing: 0.05em;
}

.print-table-names {
  font-size: 13px;
  color: #2D4A3E;
  font-weight: 500;
  padding: 6px 10px;
  background: #FAF8F5;
  border-radius: 4px;
  border: 1px solid #e8e4df;
}

.print-remark-section {
  margin-top: 8px;
}

.print-remark-text {
  font-size: 12px;
  color: #555;
  line-height: 1.6;
  margin: 0;
  padding: 6px 10px;
  background: #FAF8F5;
  border-radius: 4px;
  border: 1px solid #e8e4df;
}

.print-footer {
  text-align: center;
  padding: 12px 0 20px;
}

.print-footer-text {
  font-size: 12px;
  color: #C4A35A;
  margin: 8px 0 4px;
  letter-spacing: 0.05em;
}

.print-footer-time {
  font-size: 10px;
  color: #aaa;
  margin: 0;
}

/* ============ A5 纸张 ============ */
.print-a5 {
  width: 148mm;
  min-height: 210mm;
  margin: 0 auto;
  padding: 12mm 14mm;
  box-sizing: border-box;
}

/* ============ A6 竖版桌签 ============ */
.print-a6-portrait {
  width: 105mm;
  min-height: 148mm;
  margin: 0 auto;
  padding: 0;
  box-sizing: border-box;
  border: 2px solid #2D4A3E;
  border-radius: 6px;
  overflow: hidden;
}

.ts-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 148mm;
  padding: 10mm 8mm;
  text-align: center;
}

.ts-top-deco {
  width: 40px;
  height: 3px;
  background: #C4A35A;
  border-radius: 2px;
  margin-bottom: 16px;
}

.ts-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}

.ts-label-cn {
  font-size: 28px;
  font-weight: 700;
  color: #2D4A3E;
  letter-spacing: 0.15em;
  margin: 0;
}

.ts-label-en {
  font-size: 14px;
  color: #C4A35A;
  letter-spacing: 0.25em;
  text-transform: uppercase;
  margin: 4px 0 0;
  font-family: 'Georgia', serif;
}

.ts-divider {
  width: 30px;
  height: 1px;
  background: #C4A35A;
  margin: 16px auto;
}

.ts-info-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.ts-info-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.ts-info-label {
  font-size: 10px;
  color: #888;
  letter-spacing: 0.05em;
}

.ts-info-value {
  font-size: 14px;
  font-weight: 600;
  color: #2D4A3E;
}

.ts-info-value--large {
  font-size: 20px;
  font-weight: 700;
  color: #2D4A3E;
}

.ts-bottom-deco {
  width: 40px;
  height: 3px;
  background: #C4A35A;
  border-radius: 2px;
  margin-top: 16px;
}

/* ============ 取消单特殊样式 ============ */
.cancel-notice-box {
  text-align: center;
  padding: 16px;
  margin-bottom: 16px;
  border: 2px dashed #C4A35A;
  border-radius: 6px;
  background: #FAF8F5;
}

.cancel-notice-cn {
  font-size: 18px;
  font-weight: 700;
  color: #8B5A3E;
  margin: 0 0 4px;
}

.cancel-notice-en {
  font-size: 12px;
  color: #C4A35A;
  margin: 0;
  letter-spacing: 0.1em;
}
</style>

<style>
/* ============ 弹窗样式 ============ */
.print-preview-dlg .el-dialog {
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(45, 74, 62, 0.2);
}

.print-preview-dlg .el-dialog__header {
  background: #2D4A3E;
  padding: 14px 20px;
  margin: 0;
}

.print-preview-dlg .el-dialog__title {
  color: #FAF8F5;
  font-size: 15px;
  font-weight: 600;
  font-family: 'Noto Serif SC', serif;
}

.print-preview-dlg .el-dialog__body {
  padding: 0;
  background: #f0ede8;
}

.print-preview-dlg .el-dialog__footer {
  padding: 0;
}

/* ============ 工具栏 ============ */
.print-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  padding: 12px 20px;
  background: #FAF8F5;
  border-bottom: 1px solid #e8e4df;
}

.print-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 7px 16px;
  border: none;
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  background: #2D4A3E;
  color: #FAF8F5;
}

.print-action-btn:hover {
  background: #1e3529;
  transform: translateY(-1px);
}

.print-action-btn--cancel {
  background: transparent;
  color: #888;
  border: 1px solid #ddd;
}

.print-action-btn--cancel:hover {
  background: #f5f5f5;
  color: #555;
  transform: none;
}

/* ============ 打印内容滚动区 ============ */
.print-content {
  padding: 20px;
  overflow-y: auto;
  max-height: 70vh;
  display: flex;
  justify-content: center;
  background: #f0ede8;
}

/* ============ @media print 打印样式 ============ */
@media print {
  /* 隐藏非打印内容 */
  .no-print,
  .print-toolbar,
  .el-dialog__header,
  .el-dialog__footer,
  .print-action-btn {
    display: none !important;
  }

  /* 弹窗全屏铺满 */
  .print-preview-dlg .el-dialog {
    box-shadow: none;
    border-radius: 0;
    width: 100% !important;
    max-width: 100% !important;
  }

  .print-preview-dlg .el-overlay {
    background: transparent !important;
    position: static !important;
  }

  .el-overlay {
    background: transparent !important;
  }

  /* 打印内容区域 */
  .print-content {
    padding: 0;
    max-height: none;
    overflow: visible;
    background: transparent;
  }

  .print-template {
    box-shadow: none;
    margin: 0;
    padding: 10mm;
  }

  /* A5 纸张 */
  .print-a5 {
    width: 148mm;
    min-height: auto;
    page-break-after: avoid;
  }

  /* A6 桌签 */
  .print-a6-portrait {
    width: 105mm;
    min-height: 148mm;
    border: 2px solid #2D4A3E;
    page-break-after: avoid;
  }

  /* 确保打印时显示正确颜色 */
  body {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }

  .print-template * {
    -webkit-print-color-adjust: exact;
    print-color-adjust: exact;
  }
}
</style>

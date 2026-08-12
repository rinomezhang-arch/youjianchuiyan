<template>
  <el-dialog
    v-model="visible"
    title="导出数据 · Export Data"
    width="700px"
    :close-on-click-modal="false"
  >
    <div class="export-panel">
      <!-- 日期范围选择 -->
      <div class="section">
        <h3>日期范围 · Date Range</h3>
        <div class="date-range">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            format="YYYY-MM-DD"
            value-format="YYYY-MM-DD"
          />
        </div>
        <div class="quick-buttons">
          <el-button size="small" @click="setQuickDate('today')">今天</el-button>
          <el-button size="small" @click="setQuickDate('week')">本周</el-button>
          <el-button size="small" @click="setQuickDate('month')">本月</el-button>
          <el-button size="small" @click="setQuickDate('last30')">近30天</el-button>
          <el-button size="small" @click="setQuickDate('last90')">近90天</el-button>
        </div>
      </div>

      <!-- 时段选择 -->
      <div class="section">
        <h3>时段选择 · Time Period</h3>
        <el-radio-group v-model="timePeriod">
          <el-radio-button label="all">全部</el-radio-button>
          <el-radio-button label="lunch">午餐</el-radio-button>
          <el-radio-button label="dinner">晚餐</el-radio-button>
        </el-radio-group>
      </div>

      <!-- 导出选项 -->
      <div class="section">
        <h3>导出选项 · Export Options</h3>
        <el-checkbox v-model="includeCustomerInfo">包含客户信息</el-checkbox>
        <el-checkbox v-model="includeTableInfo">包含桌台信息</el-checkbox>
        <el-checkbox v-model="includeBanquetType">包含宴会类型</el-checkbox>
      </div>

      <!-- 预览数据 -->
      <div class="section">
        <h3>数据预览 · Data Preview</h3>
        <div class="preview-info">
          符合条件的预订记录: <strong>{{ previewCount }}</strong> 条
        </div>
      </div>

      <!-- 操作按钮 -->
      <div class="actions">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="exportToExcel" :loading="exporting">
          导出 Excel
        </el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import ExcelJS from 'exceljs'
import { listBookings } from '@/api/booking'

const props = defineProps({
  modelValue: Boolean
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 日期范围
const dateRange = ref([])

// 时段选择
const timePeriod = ref('all')

// 导出选项
const includeCustomerInfo = ref(true)
const includeTableInfo = ref(true)
const includeBanquetType = ref(true)

// 导出数据
const exportData = ref([])
const exporting = ref(false)

// 预览数量
const previewCount = computed(() => exportData.value.length)

// 设置快捷日期
const setQuickDate = (type) => {
  const today = new Date()
  const endDate = new Date(today)
  let startDate = new Date(today)

  switch (type) {
    case 'today':
      // 今天
      break
    case 'week':
      // 本周（周一到今天）
      const dayOfWeek = today.getDay()
      const diff = dayOfWeek === 0 ? 6 : dayOfWeek - 1
      startDate = new Date(today)
      startDate.setDate(today.getDate() - diff)
      break
    case 'month':
      // 本月（1号到今天）
      startDate = new Date(today.getFullYear(), today.getMonth(), 1)
      break
    case 'last30':
      // 近30天
      startDate.setDate(today.getDate() - 29)
      break
    case 'last90':
      // 近90天
      startDate.setDate(today.getDate() - 89)
      break
  }

  dateRange.value = [
    formatDate(startDate),
    formatDate(endDate)
  ]
}

// 格式化日期
const formatDate = (date) => {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

// 加载数据
const loadData = async () => {
  if (!dateRange.value || dateRange.value.length !== 2) {
    exportData.value = []
    return
  }

  try {
    const params = {
      startDate: dateRange.value[0],
      endDate: dateRange.value[1]
    }

    if (timePeriod.value !== 'all') {
      params.timePeriod = timePeriod.value
    }

    const response = await listBookings(params)
    exportData.value = response.data || []
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
    exportData.value = []
  }
}

// 监听日期和时段变化
watch([dateRange, timePeriod], loadData, { immediate: true })

// 导出到 Excel
const exportToExcel = async () => {
  if (exportData.value.length === 0) {
    ElMessage.warning('没有可导出的数据')
    return
  }

  exporting.value = true

  try {
    // 准备导出数据
    const data = exportData.value.map(booking => {
      const row = {
        '预订日期': booking.booking_date,
        '时段': booking.time_period === 'lunch' ? '午餐' : '晚餐',
        '预订时间': booking.booking_time
      }

      if (includeCustomerInfo.value) {
        row['客户姓名'] = booking.customer_name || ''
        row['客户电话'] = booking.customer_phone || ''
        row['人数'] = booking.guest_count || ''
      }

      if (includeTableInfo.value) {
        row['桌台号'] = booking.table_number || ''
        row['区域'] = booking.table_area || ''
      }

      if (includeBanquetType.value) {
        row['宴会类型'] = booking.banquet_type || ''
      }

      row['备注'] = booking.remark || ''
      row['状态'] = booking.status === 'confirmed' ? '已确认' : '已取消'
      row['创建时间'] = booking.created_at || ''

      return row
    })

    const workbook = new ExcelJS.Workbook()
    const worksheet = workbook.addWorksheet('预订数据')
    const headers = Object.keys(data[0])

    worksheet.columns = headers.map(key => ({
      header: key,
      key,
      width: Math.max(key.length * 2, 15)
    }))
    worksheet.addRows(data)
    worksheet.getRow(1).font = { bold: true }
    worksheet.views = [{ state: 'frozen', ySplit: 1 }]

    const fileName = `预订数据_${dateRange.value[0]}_${dateRange.value[1]}.xlsx`
    const buffer = await workbook.xlsx.writeBuffer()
    const blob = new Blob([buffer], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const downloadUrl = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = downloadUrl
    link.download = fileName
    link.click()
    URL.revokeObjectURL(downloadUrl)

    ElMessage.success('导出成功')
    visible.value = false
  } catch (error) {
    console.error('导出失败:', error)
    ElMessage.error('导出失败')
  } finally {
    exporting.value = false
  }
}
</script>

<style scoped>
.export-panel {
  padding: 0;
}

.section {
  margin-bottom: 24px;
}

.section h3 {
  margin: 0 0 12px 0;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.date-range {
  margin-bottom: 12px;
}

.quick-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preview-info {
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
  font-size: 14px;
  color: #606266;
}

.preview-info strong {
  color: #409eff;
  font-size: 18px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid #e4e7ed;
}
</style>

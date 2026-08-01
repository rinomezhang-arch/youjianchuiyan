<template>
  <el-dialog
    v-model="visible"
    title="菜单展示 · Menu Display"
    width="90%"
    top="5vh"
    :close-on-click-modal="false"
  >
    <div class="menu-display">
      <!-- 工具栏 -->
      <div class="toolbar">
        <el-button @click="handlePrint">🖨️ 打印 · Print</el-button>
        <el-button @click="copyToClipboard">📋 复制文本 · Copy</el-button>
        <el-button @click="exportAsHtml">📄 导出HTML · Export</el-button>
      </div>

      <!-- 菜单内容 -->
      <div class="menu-content" ref="menuContentRef">
        <div class="menu-header">
          <h1>{{ restaurantName }}</h1>
          <div class="menu-info">
            <span>日期 · Date: {{ displayDate }}</span>
            <span>时段 · Period: {{ periodText }}</span>
            <span v-if="tableName">桌号 · Table: {{ tableName }}</span>
            <span v-if="customerName">客户 · Customer: {{ customerName }}</span>
          </div>
        </div>

        <!-- 菜品列表 -->
        <div v-if="dishes.length > 0" class="dishes-section">
          <div v-for="(catDishes, catName) in groupedDishes" :key="catName" class="category-block">
            <h2 class="category-title">
              {{ catName }}
              <span class="category-en">{{ getCategoryEn(catName) }}</span>
            </h2>
            <div class="dish-list">
              <div v-for="dish in catDishes" :key="dish.id" class="dish-item">
                <span class="dish-name">{{ dish.name }}</span>
                <span class="dish-qty">×{{ dish.qty }}</span>
                <span class="dish-price">¥{{ dish.price * dish.qty }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 套餐信息 -->
        <div v-if="packageName" class="package-section">
          <h2>套餐 · Package</h2>
          <div class="package-info">
            <div class="package-name">{{ packageName }}</div>
            <div class="package-price">¥{{ packagePrice }}</div>
          </div>
        </div>

        <!-- 汇总 -->
        <div class="summary-section">
          <div class="summary-row">
            <span>菜品总数 · Total Dishes</span>
            <span>{{ totalDishes }} 道</span>
          </div>
          <div class="summary-row total">
            <span>总计 · Total</span>
            <span>¥{{ totalPrice }}</span>
          </div>
        </div>

        <!-- 备注 -->
        <div v-if="remark" class="remark-section">
          <h3>备注 · Remark</h3>
          <p>{{ remark }}</p>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

const props = defineProps({
  modelValue: Boolean,
  dishes: { type: Array, default: () => [] },
  packageName: { type: String, default: '' },
  packagePrice: { type: Number, default: 0 },
  tableName: { type: String, default: '' },
  customerName: { type: String, default: '' },
  bookingDate: { type: String, default: '' },
  timePeriod: { type: String, default: 'dinner' },
  remark: { type: String, default: '' },
  restaurantName: { type: String, default: '又见炊烟私房菜' }
})

const emit = defineEmits(['update:modelValue'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const menuContentRef = ref(null)

// 计算属性
const displayDate = computed(() => {
  if (!props.bookingDate) return new Date().toLocaleDateString('zh-CN')
  return props.bookingDate
})

const periodText = computed(() => {
  return props.timePeriod === 'lunch' ? '午餐 · Lunch' : '晚餐 · Dinner'
})

const groupedDishes = computed(() => {
  const groups = {}
  props.dishes.forEach(dish => {
    const cat = dish.category || '其他'
    if (!groups[cat]) groups[cat] = []
    groups[cat].push(dish)
  })
  return groups
})

const totalDishes = computed(() => {
  return props.dishes.reduce((sum, d) => sum + (d.qty || 1), 0)
})

const totalPrice = computed(() => {
  return props.dishes.reduce((sum, d) => sum + (d.price * (d.qty || 1)), 0) + (props.packagePrice || 0)
})

// 分类英文翻译
const categoryEnMap = {
  '凉菜': 'Cold Dishes',
  '热菜': 'Hot Dishes',
  '汤类': 'Soups',
  '主食': 'Staples',
  '甜点': 'Desserts',
  '酒水': 'Beverages',
  '其他': 'Others'
}

const getCategoryEn = (catName) => {
  return categoryEnMap[catName] || catName
}

// 打印
const handlePrint = () => {
  if (!menuContentRef.value) return
  
  const printWindow = window.open('', '_blank')
  const content = menuContentRef.value.innerHTML
  
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="UTF-8">
      <title>菜单 - ${props.restaurantName}</title>
      <style>
        body { font-family: 'Noto Serif SC', serif; padding: 20px; }
        .menu-header { text-align: center; margin-bottom: 30px; }
        .menu-header h1 { font-size: 28px; margin-bottom: 10px; }
        .menu-info { display: flex; justify-content: center; gap: 20px; font-size: 14px; color: #666; }
        .category-block { margin-bottom: 25px; }
        .category-title { font-size: 20px; border-bottom: 2px solid #333; padding-bottom: 5px; margin-bottom: 15px; }
        .category-en { font-size: 14px; color: #999; margin-left: 10px; }
        .dish-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #ddd; }
        .dish-name { flex: 1; }
        .dish-qty { width: 60px; text-align: center; }
        .dish-price { width: 80px; text-align: right; font-weight: bold; }
        .summary-section { margin-top: 30px; padding-top: 20px; border-top: 2px solid #333; }
        .summary-row { display: flex; justify-content: space-between; padding: 8px 0; }
        .summary-row.total { font-size: 20px; font-weight: bold; color: #d32f2f; }
        .remark-section { margin-top: 20px; padding: 15px; background: #f5f5f5; border-radius: 8px; }
        @media print { body { padding: 0; } }
      </style>
    </head>
    <body>${content}</body>
    </html>
  `)
  printWindow.document.close()
  printWindow.focus()
  setTimeout(() => {
    printWindow.print()
    printWindow.close()
  }, 250)
}

// 复制文本
const copyToClipboard = () => {
  const lines = []
  lines.push(`${props.restaurantName}`)
  lines.push(`日期: ${displayDate.value} | 时段: ${periodText.value}`)
  if (props.tableName) lines.push(`桌号: ${props.tableName}`)
  if (props.customerName) lines.push(`客户: ${props.customerName}`)
  lines.push('')
  lines.push('--- 菜单 ---')
  lines.push('')
  
  for (const [catName, catDishes] of Object.entries(groupedDishes.value)) {
    lines.push(`【${catName}】`)
    catDishes.forEach(dish => {
      lines.push(`  ${dish.name} ×${dish.qty || 1}  ¥${dish.price * (dish.qty || 1)}`)
    })
    lines.push('')
  }
  
  if (props.packageName) {
    lines.push(`套餐: ${props.packageName}  ¥${props.packagePrice}`)
    lines.push('')
  }
  
  lines.push(`菜品总数: ${totalDishes.value} 道`)
  lines.push(`总计: ¥${totalPrice.value}`)
  
  if (props.remark) {
    lines.push('')
    lines.push(`备注: ${props.remark}`)
  }
  
  const text = lines.join('\n')
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制到剪贴板')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 导出HTML
const exportAsHtml = () => {
  if (!menuContentRef.value) return
  
  const content = menuContentRef.value.innerHTML
  const html = `<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>菜单 - ${props.restaurantName}</title>
  <style>
    body { font-family: 'Noto Serif SC', serif; padding: 20px; max-width: 800px; margin: 0 auto; }
    .menu-header { text-align: center; margin-bottom: 30px; }
    .menu-header h1 { font-size: 28px; margin-bottom: 10px; }
    .menu-info { display: flex; justify-content: center; gap: 20px; font-size: 14px; color: #666; flex-wrap: wrap; }
    .category-block { margin-bottom: 25px; }
    .category-title { font-size: 20px; border-bottom: 2px solid #333; padding-bottom: 5px; margin-bottom: 15px; }
    .category-en { font-size: 14px; color: #999; margin-left: 10px; }
    .dish-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px dashed #ddd; }
    .dish-name { flex: 1; }
    .dish-qty { width: 60px; text-align: center; }
    .dish-price { width: 80px; text-align: right; font-weight: bold; }
    .summary-section { margin-top: 30px; padding-top: 20px; border-top: 2px solid #333; }
    .summary-row { display: flex; justify-content: space-between; padding: 8px 0; }
    .summary-row.total { font-size: 20px; font-weight: bold; color: #d32f2f; }
    .remark-section { margin-top: 20px; padding: 15px; background: #f5f5f5; border-radius: 8px; }
  </style>
</head>
<body>${content}</body>
</html>`
  
  const blob = new Blob([html], { type: 'text/html' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `菜单_${props.bookingDate || 'today'}.html`
  a.click()
  URL.revokeObjectURL(url)
  
  ElMessage.success('已导出HTML文件')
}
</script>

<style scoped>
.menu-display {
  max-height: 80vh;
  overflow-y: auto;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.menu-content {
  font-family: 'Noto Serif SC', serif;
  padding: 20px;
  background: #fafafa;
  border-radius: 8px;
}

.menu-header {
  text-align: center;
  margin-bottom: 30px;
}

.menu-header h1 {
  font-size: 28px;
  margin-bottom: 10px;
  color: #333;
}

.menu-info {
  display: flex;
  justify-content: center;
  gap: 20px;
  font-size: 14px;
  color: #666;
  flex-wrap: wrap;
}

.category-block {
  margin-bottom: 25px;
}

.category-title {
  font-size: 20px;
  border-bottom: 2px solid #333;
  padding-bottom: 5px;
  margin-bottom: 15px;
}

.category-en {
  font-size: 14px;
  color: #999;
  margin-left: 10px;
  font-weight: normal;
}

.dish-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px dashed #ddd;
}

.dish-name {
  flex: 1;
}

.dish-qty {
  width: 60px;
  text-align: center;
  color: #666;
}

.dish-price {
  width: 80px;
  text-align: right;
  font-weight: bold;
  color: #d32f2f;
}

.package-section {
  margin-top: 25px;
  padding: 15px;
  background: #fff3e0;
  border-radius: 8px;
}

.package-section h2 {
  margin: 0 0 10px 0;
  font-size: 18px;
}

.package-info {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.package-name {
  font-size: 16px;
  font-weight: bold;
}

.package-price {
  font-size: 18px;
  font-weight: bold;
  color: #d32f2f;
}

.summary-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 2px solid #333;
}

.summary-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
}

.summary-row.total {
  font-size: 20px;
  font-weight: bold;
  color: #d32f2f;
}

.remark-section {
  margin-top: 20px;
  padding: 15px;
  background: #f5f5f5;
  border-radius: 8px;
}

.remark-section h3 {
  margin: 0 0 10px 0;
  font-size: 16px;
}

.remark-section p {
  margin: 0;
  color: #666;
}
</style>

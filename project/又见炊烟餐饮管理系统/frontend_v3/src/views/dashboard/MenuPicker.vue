<template>
  <el-dialog
    v-model="visible"
    title="选择菜品 · Menu Picker"
    width="90%"
    :close-on-click-modal="false"
    @close="handleClose"
  >
    <div class="menu-picker">
      <!-- 左侧分类 -->
      <div class="category-panel">
        <div class="category-header">
          <h3>菜品分类 · Categories</h3>
        </div>
        <div class="category-list">
          <div
            v-for="cat in categories"
            :key="cat.id"
            :class="['category-item', { active: selectedCategory === cat.id }]"
            @click="selectCategory(cat.id)"
          >
            <span class="cat-name">{{ cat.name }}</span>
            <span class="cat-count">{{ getCategoryCount(cat.id) }}</span>
          </div>
        </div>
      </div>

      <!-- 右侧菜品列表 -->
      <div class="dish-panel">
        <div class="dish-header">
          <el-input
            v-model="searchQuery"
            placeholder="搜索菜品 · Search dishes..."
            clearable
            style="width: 300px"
          />
          <div class="selected-count">
            已选 {{ selectedDishes.length }} 道菜
          </div>
        </div>

        <div class="dish-grid">
          <div
            v-for="dish in filteredDishes"
            :key="dish.id"
            :class="['dish-card', { selected: isSelected(dish.id) }]"
            @click="toggleDish(dish)"
          >
            <div class="dish-image">
              <el-image
                v-if="dish.image"
                :src="dish.image"
                fit="cover"
                style="width: 100%; height: 100%"
              />
              <div v-else class="dish-placeholder">🍽️</div>
            </div>
            <div class="dish-info">
              <div class="dish-name">{{ dish.name }}</div>
              <div class="dish-price">¥{{ dish.price }}</div>
              <div v-if="isSelected(dish.id)" class="dish-qty">
                <el-button size="small" @click.stop="decreaseQty(dish)">-</el-button>
                <span>{{ getDishQty(dish.id) }}</span>
                <el-button size="small" @click.stop="increaseQty(dish)">+</el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部已选菜品 -->
      <div class="selected-panel">
        <div class="selected-header">
          <h3>已选菜品 · Selected Dishes</h3>
          <el-button type="danger" size="small" @click="clearAll">清空 · Clear All</el-button>
        </div>
        <div class="selected-list">
          <div v-for="item in selectedDishes" :key="item.id" class="selected-item">
            <span class="item-name">{{ item.name }}</span>
            <span class="item-qty">x{{ item.qty }}</span>
            <span class="item-price">¥{{ item.price * item.qty }}</span>
            <el-button size="small" @click="removeDish(item)">删除</el-button>
          </div>
          <div v-if="selectedDishes.length === 0" class="empty-text">
            未选择菜品 · No dishes selected
          </div>
        </div>
        <div class="selected-footer">
          <div class="total-price">
            总计 · Total: <strong>¥{{ totalPrice }}</strong>
          </div>
          <div class="action-buttons">
            <el-button @click="handleClose">取消 · Cancel</el-button>
            <el-button type="primary" @click="confirmSelection">确认选择 · Confirm</el-button>
          </div>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { getDishesWithRecipe } from '@/api/booking'

const props = defineProps({
  modelValue: Boolean,
  initialDishes: { type: Array, default: () => [] },
  tableId: { type: [String, Number], default: null }
})

const emit = defineEmits(['update:modelValue', 'confirm'])

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

// 数据
const categories = ref([])
const allDishes = ref([])
const selectedCategory = ref(null)
const searchQuery = ref('')
const selectedDishes = ref([])

// 加载数据
const loadData = async () => {
  try {
    const res = await getDishesWithRecipe()
    if (res.data) {
      // 解析分类
      const catMap = new Map()
      res.data.forEach(dish => {
        const catId = dish.category || 'other'
        const catName = dish.categoryName || '其他'
        if (!catMap.has(catId)) {
          catMap.set(catId, { id: catId, name: catName })
        }
      })
      categories.value = Array.from(catMap.values())
      if (categories.value.length > 0) {
        selectedCategory.value = categories.value[0].id
      }

      // 解析菜品
      allDishes.value = res.data.map(dish => ({
        id: dish.dishId || dish.id,
        name: dish.dishName || dish.name,
        price: dish.salePrice || dish.price || 0,
        category: dish.category || 'other',
        image: dish.image || null
      }))
    }
  } catch (error) {
    console.error('加载菜品失败:', error)
  }
}

// 初始化选中菜品
watch(() => props.initialDishes, (val) => {
  if (val && val.length > 0) {
    selectedDishes.value = val.map(d => ({
      id: d.id || d.dishId,
      name: d.name || d.dishName,
      price: d.price || d.salePrice || 0,
      qty: d.qty || 1
    }))
  }
}, { immediate: true })

// 计算属性
const filteredDishes = computed(() => {
  let dishes = allDishes.value
  if (selectedCategory.value) {
    dishes = dishes.filter(d => d.category === selectedCategory.value)
  }
  if (searchQuery.value) {
    const q = searchQuery.value.toLowerCase()
    dishes = dishes.filter(d => d.name.toLowerCase().includes(q))
  }
  return dishes
})

const totalPrice = computed(() => {
  return selectedDishes.value.reduce((sum, item) => sum + item.price * item.qty, 0)
})

// 方法
const selectCategory = (catId) => {
  selectedCategory.value = catId
}

const getCategoryCount = (catId) => {
  return allDishes.value.filter(d => d.category === catId).length
}

const isSelected = (dishId) => {
  return selectedDishes.value.some(d => d.id === dishId)
}

const getDishQty = (dishId) => {
  const item = selectedDishes.value.find(d => d.id === dishId)
  return item ? item.qty : 0
}

const toggleDish = (dish) => {
  if (isSelected(dish.id)) {
    // 已选中，增加数量
    increaseQty(dish)
  } else {
    // 未选中，添加
    selectedDishes.value.push({
      id: dish.id,
      name: dish.name,
      price: dish.price,
      qty: 1
    })
  }
}

const increaseQty = (dish) => {
  const item = selectedDishes.value.find(d => d.id === dish.id)
  if (item) {
    item.qty++
  }
}

const decreaseQty = (dish) => {
  const idx = selectedDishes.value.findIndex(d => d.id === dish.id)
  if (idx >= 0) {
    if (selectedDishes.value[idx].qty > 1) {
      selectedDishes.value[idx].qty--
    } else {
      selectedDishes.value.splice(idx, 1)
    }
  }
}

const removeDish = (item) => {
  const idx = selectedDishes.value.findIndex(d => d.id === item.id)
  if (idx >= 0) {
    selectedDishes.value.splice(idx, 1)
  }
}

const clearAll = () => {
  selectedDishes.value = []
}

const confirmSelection = () => {
  emit('confirm', {
    dishes: selectedDishes.value,
    totalPrice: totalPrice.value,
    tableId: props.tableId
  })
  visible.value = false
}

const handleClose = () => {
  visible.value = false
}

// 加载数据
loadData()
</script>

<style scoped>
.menu-picker {
  display: flex;
  flex-direction: column;
  height: 70vh;
}

.category-panel {
  width: 200px;
  border-right: 1px solid #e4e7ed;
  display: flex;
  flex-direction: column;
}

.category-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
}

.category-header h3 {
  margin: 0;
  font-size: 16px;
}

.category-list {
  flex: 1;
  overflow-y: auto;
}

.category-item {
  padding: 12px 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background 0.2s;
}

.category-item:hover {
  background: #f5f7fa;
}

.category-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.cat-name {
  font-size: 14px;
}

.cat-count {
  font-size: 12px;
  color: #909399;
}

.dish-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.dish-header {
  padding: 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-count {
  font-size: 14px;
  color: #606266;
}

.dish-grid {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
  gap: 16px;
}

.dish-card {
  border: 2px solid #e4e7ed;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}

.dish-card:hover {
  border-color: #409eff;
  transform: translateY(-2px);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.dish-card.selected {
  border-color: #67c23a;
  background: #f0f9ff;
}

.dish-image {
  width: 100%;
  height: 120px;
  background: #f5f7fa;
  display: flex;
  align-items: center;
  justify-content: center;
}

.dish-placeholder {
  font-size: 48px;
}

.dish-info {
  padding: 12px;
}

.dish-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}

.dish-price {
  font-size: 16px;
  color: #f56c6c;
  font-weight: 600;
}

.dish-qty {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  padding: 4px;
  background: #fff;
  border-radius: 4px;
}

.selected-panel {
  border-top: 2px solid #e4e7ed;
  max-height: 200px;
  display: flex;
  flex-direction: column;
}

.selected-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.selected-header h3 {
  margin: 0;
  font-size: 14px;
}

.selected-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.selected-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.item-name {
  flex: 1;
}

.item-qty {
  color: #909399;
}

.item-price {
  color: #f56c6c;
  font-weight: 600;
}

.empty-text {
  text-align: center;
  color: #909399;
  padding: 20px;
}

.selected-footer {
  padding: 12px 16px;
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.total-price {
  font-size: 16px;
}

.total-price strong {
  color: #f56c6c;
  font-size: 20px;
}

.action-buttons {
  display: flex;
  gap: 8px;
}
</style>

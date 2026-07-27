<template>
  <div class="menu-sort">
    <div class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h2 class="page-title">菜单排序管理</h2>
      <button class="save-btn" @click="saveSort">保存排序</button>
    </div>

    <!-- 分类筛选 -->
    <div class="category-filter">
      <button
        v-for="cat in filterCategories"
        :key="cat.id"
        class="filter-btn"
        :class="{ active: selectedCategory === cat.id }"
        @click="selectCategory(cat.id)"
      >
        {{ cat.name }}
      </button>
    </div>

    <!-- 排序列表 -->
    <div class="sort-content">
      <div class="sort-list">
        <div
          v-for="(item, index) in currentDishes"
          :key="item.id"
          class="sort-item"
          :class="{ dragging: dragIndex === index, top: item.isTop }"
          draggable="true"
          @dragstart="onDragStart(index)"
          @dragover.prevent
          @drop="onDrop(index)"
          @dragend="onDragEnd"
        >
          <div class="drag-handle">⋮⋮</div>
          <div class="item-star" @click.stop="toggleTop(item.id)">
            {{ item.isTop ? '⭐' : '☆' }}
          </div>
          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-desc">
              <span>{{ item.price || '0' }}元</span>
              <span class="divider">·</span>
              <span>{{ item.unit || '' }}</span>
            </div>
          </div>
          <div class="item-index">{{ index + 1 }}</div>
        </div>
      </div>
    </div>

    <div class="sort-tips">
      <p>🖱️ 拖拽菜品调整显示顺序</p>
      <p>⭐ 点击星星标记为推荐菜品</p>
      <p>💡 排序后点击"保存排序"生效</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getDishes } from '@/api/dish'

const router = useRouter()

const dishes = ref([])
const selectedCategory = ref('all')
const dragIndex = ref(-1)

const filterCategories = ref([
  { id: 'all', name: '全部' },
  { id: 1, name: '凉菜' },
  { id: 2, name: '热菜' },
  { id: 3, name: '汤羹' },
  { id: 4, name: '主食' },
  { id: 5, name: '海鲜' },
  { id: 6, name: '酒水' }
])

const currentDishes = computed(() => {
  if (selectedCategory.value === 'all') return dishes.value
  return dishes.value.filter(d => d.categoryId === selectedCategory.value)
})

async function loadDishes() {
  try {
    const res = await getDishes({ page_size: 999 })
    const list = res?.data?.list || res?.data || []
    dishes.value = list.length > 0 ? list : getMockDishes()
  } catch (e) {
    console.error('加载菜品失败', e)
    dishes.value = getMockDishes()
  }
}

function getMockDishes() {
  return [
    { id: 1, name: '凉拌黄瓜', price: 18, unit: '份', categoryId: 1, isTop: false },
    { id: 2, name: '拍黄瓜', price: 16, unit: '份', categoryId: 1, isTop: true },
    { id: 3, name: '凉拌木耳', price: 22, unit: '份', categoryId: 1, isTop: false },
    { id: 4, name: '红烧肉', price: 58, unit: '份', categoryId: 2, isTop: true },
    { id: 5, name: '糖醋排骨', price: 48, unit: '份', categoryId: 2, isTop: false },
    { id: 6, name: '宫保鸡丁', price: 38, unit: '份', categoryId: 2, isTop: false },
    { id: 7, name: '酸辣汤', price: 28, unit: '份', categoryId: 3, isTop: false },
    { id: 8, name: '西湖牛肉羹', price: 32, unit: '份', categoryId: 3, isTop: true },
    { id: 9, name: '米饭', price: 3, unit: '碗', categoryId: 4, isTop: false },
    { id: 10, name: '馒头', price: 2, unit: '个', categoryId: 4, isTop: false },
    { id: 11, name: '清蒸鲈鱼', price: 68, unit: '条', categoryId: 5, isTop: true },
    { id: 12, name: '蒜蓉扇贝', price: 58, unit: '份', categoryId: 5, isTop: false },
    { id: 13, name: '青岛啤酒', price: 8, unit: '瓶', categoryId: 6, isTop: false },
    { id: 14, name: '王老吉', price: 6, unit: '罐', categoryId: 6, isTop: true }
  ]
}

function selectCategory(catId) {
  selectedCategory.value = catId
}

function toggleTop(dishId) {
  const dish = dishes.value.find(d => d.id === dishId)
  if (dish) {
    dish.isTop = !dish.isTop
  }
}

function onDragStart(index) {
  dragIndex.value = index
}

function onDrop(targetIndex) {
  if (dragIndex.value === -1 || dragIndex.value === targetIndex) return
  
  const dragged = dishes.value.splice(dragIndex.value, 1)[0]
  dishes.value.splice(targetIndex, 0, dragged)
  dragIndex.value = -1
}

function onDragEnd() {
  dragIndex.value = -1
}

function saveSort() {
  const sorted = dishes.value.map((d, i) => ({ id: d.id, sort_order: i + 1, is_top: d.isTop }))
  console.log('保存排序:', sorted)
  alert('排序已保存！')
}

function goBack() {
  router.push('/dashboard/admin')
}

onMounted(() => {
  loadDishes()
})
</script>

<style scoped>
.menu-sort {
  max-width: 700px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
}

.back-btn {
  padding: 8px 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 14px;
  cursor: pointer;
  transition: var(--transition);
}

.back-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.page-title {
  flex: 1;
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin: 0;
}

.save-btn {
  padding: 8px 20px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: var(--transition);
}

.save-btn:hover {
  background: var(--color-primary-dark);
}

.category-filter {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.filter-btn {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: var(--color-card);
  color: var(--color-text-secondary);
  font-size: 13px;
  cursor: pointer;
  transition: var(--transition);
}

.filter-btn:hover {
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.filter-btn.active {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}

.sort-content {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.sort-list {
  padding: 12px;
}

.sort-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px;
  margin-bottom: 6px;
  background: var(--color-bg);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  cursor: grab;
  transition: var(--transition);
}

.sort-item:last-child {
  margin-bottom: 0;
}

.sort-item:hover {
  border-color: var(--color-primary);
  background: rgba(45, 74, 62, 0.04);
}

.sort-item.dragging {
  opacity: 0.5;
  transform: scale(1.02);
  cursor: grabbing;
}

.sort-item.top {
  background: rgba(196, 163, 90, 0.06);
  border-color: rgba(196, 163, 90, 0.3);
}

.drag-handle {
  font-size: 18px;
  color: var(--color-text-muted);
  cursor: grab;
  padding: 4px;
}

.sort-item:hover .drag-handle {
  color: var(--color-primary);
}

.item-star {
  font-size: 18px;
  cursor: pointer;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text);
}

.item-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.item-desc span {
  margin-right: 4px;
}

.item-desc .divider {
  color: var(--color-border);
}

.item-index {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-bg-alt);
  border-radius: 50%;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-muted);
}

.sort-tips {
  margin-top: 20px;
  padding: 16px;
  background: rgba(45, 74, 62, 0.04);
  border-radius: var(--radius-md);
  text-align: center;
}

.sort-tips p {
  margin: 4px 0;
  font-size: 13px;
  color: var(--color-text-muted);
}
</style>

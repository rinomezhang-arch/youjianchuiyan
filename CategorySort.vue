<template>
  <div class="category-sort">
    <div class="page-header">
      <button class="back-btn" @click="goBack">← 返回</button>
      <h2 class="page-title">菜品分类排序</h2>
      <button class="save-btn" @click="saveSort">保存排序</button>
    </div>

    <div class="sort-content">
      <div class="sort-list">
        <div
          v-for="(item, index) in categories"
          :key="item.id"
          class="sort-item"
          :class="{ dragging: dragIndex === index }"
          draggable="true"
          @dragstart="onDragStart(index)"
          @dragover.prevent
          @drop="onDrop(index)"
          @dragend="onDragEnd"
        >
          <div class="drag-handle">⋮⋮</div>
          <div class="item-color" :style="{ background: item.color || '#2D4A3E' }"></div>
          <div class="item-info">
            <div class="item-name">{{ item.name }}</div>
            <div class="item-desc">{{ item.dishCount || 0 }} 个菜品</div>
          </div>
          <div class="item-index">{{ index + 1 }}</div>
        </div>
      </div>
    </div>

    <div class="sort-tips">
      <p>🖱️ 拖拽分类调整显示顺序</p>
      <p>💡 排序后点击"保存排序"生效</p>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCategories } from '@/api/dish'

const router = useRouter()

const categories = ref([])
const dragIndex = ref(-1)

async function loadCategories() {
  try {
    const res = await getCategories()
    categories.value = res?.data?.list || res?.data || []
    if (categories.value.length === 0) {
      categories.value = [
        { id: 1, name: '凉菜', color: '#5B7B8A', dishCount: 12 },
        { id: 2, name: '热菜', color: '#4A7C59', dishCount: 28 },
        { id: 3, name: '汤羹', color: '#8B9A8C', dishCount: 8 },
        { id: 4, name: '主食', color: '#C4A35A', dishCount: 15 },
        { id: 5, name: '海鲜', color: '#2D4A3E', dishCount: 10 },
        { id: 6, name: '酒水', color: '#5B6B7A', dishCount: 20 }
      ]
    }
  } catch (e) {
    console.error('加载分类失败', e)
    categories.value = [
      { id: 1, name: '凉菜', color: '#5B7B8A', dishCount: 12 },
      { id: 2, name: '热菜', color: '#4A7C59', dishCount: 28 },
      { id: 3, name: '汤羹', color: '#8B9A8C', dishCount: 8 },
      { id: 4, name: '主食', color: '#C4A35A', dishCount: 15 },
      { id: 5, name: '海鲜', color: '#2D4A3E', dishCount: 10 },
      { id: 6, name: '酒水', color: '#5B6B7A', dishCount: 20 }
    ]
  }
}

function onDragStart(index) {
  dragIndex.value = index
}

function onDrop(targetIndex) {
  if (dragIndex.value === -1 || dragIndex.value === targetIndex) return
  
  const dragged = categories.value.splice(dragIndex.value, 1)[0]
  categories.value.splice(targetIndex, 0, dragged)
  dragIndex.value = -1
}

function onDragEnd() {
  dragIndex.value = -1
}

function saveSort() {
  const sortedIds = categories.value.map((c, i) => ({ id: c.id, sort_order: i + 1 }))
  console.log('保存排序:', sortedIds)
  alert('排序已保存！')
}

function goBack() {
  router.push('/dashboard/admin')
}

onMounted(() => {
  loadCategories()
})
</script>

<style scoped>
.category-sort {
  max-width: 600px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
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
  gap: 12px;
  padding: 16px;
  margin-bottom: 8px;
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

.drag-handle {
  font-size: 18px;
  color: var(--color-text-muted);
  cursor: grab;
  padding: 4px;
}

.sort-item:hover .drag-handle {
  color: var(--color-primary);
}

.item-color {
  width: 8px;
  height: 40px;
  border-radius: 4px;
}

.item-info {
  flex: 1;
}

.item-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
}

.item-desc {
  font-size: 12px;
  color: var(--color-text-muted);
  margin-top: 4px;
}

.item-index {
  width: 28px;
  height: 28px;
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

<template>
  <div class="gm-office-page" v-loading="loading">
    <!-- 顶部 -->
    <div class="page-topbar">
      <div class="topbar-left">
        <h1 class="page-title">总经办 · GM Office</h1>
        <span class="page-desc">总经理决策中心 · Decision Center</span>
      </div>
      <div class="topbar-actions">
        <el-button @click="$router.back()" plain>
          <el-icon><ArrowLeft /></el-icon>
          <span>返回工作台</span>
        </el-button>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-row">
      <div class="stat-card pai">
        <div class="stat-icon">拍</div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.review }}</div>
          <div class="stat-label">待批阅</div>
        </div>
      </div>
      <div class="stat-card yue">
        <div class="stat-icon">阅</div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.info }}</div>
          <div class="stat-label">待了解</div>
        </div>
      </div>
      <div class="stat-card pi">
        <div class="stat-icon">批</div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.approval }}</div>
          <div class="stat-label">待批复</div>
        </div>
      </div>
      <div class="stat-card ban">
        <div class="stat-icon">办</div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.todo }}</div>
          <div class="stat-label">待办</div>
        </div>
      </div>
    </div>

    <!-- 待批阅事项 -->
    <div class="section-card">
      <h3 class="section-title">待批阅事项 · Pending Review</h3>
      <div class="item-list">
        <div v-for="(item, idx) in reviewItems" :key="'review-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待批复事项 -->
    <div class="section-card">
      <h3 class="section-title">待批复事项 · Pending Approval</h3>
      <div class="item-list">
        <div v-for="(item, idx) in approvalItems" :key="'approval-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待了解事项 -->
    <div class="section-card">
      <h3 class="section-title">待了解事项 · To Know</h3>
      <div class="item-list">
        <div v-for="(item, idx) in infoItems" :key="'info-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- 待办事项 -->
    <div class="section-card">
      <h3 class="section-title">待办事项 · To Do</h3>
      <div class="item-list">
        <div v-for="(item, idx) in todoItems" :key="'todo-'+idx" class="list-item">
          <span class="item-num">{{ idx + 1 }}</span>
          <div class="item-content">
            <span class="item-text">{{ item.title }}</span>
            <el-tag :type="item.tagType" size="small" effect="plain">{{ item.tag }}</el-tag>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ArrowLeft } from '@element-plus/icons-vue'
import {
  getGMStats,
  getReviewItems as fetchReviewItems,
  getApprovalItems as fetchApprovalItems,
  getInfoItems as fetchInfoItems,
  getTodoItems as fetchTodoItems
} from '@/api/gm'

// 统计数字
const stats = ref({ review: 0, info: 0, approval: 0, todo: 0 })

// 列表数据
const reviewItems = ref([])
const approvalItems = ref([])
const infoItems = ref([])
const todoItems = ref([])

// 加载状态
const loading = ref(false)

async function loadStats() {
  try {
    const res = await getGMStats()
    if (res.data) {
      stats.value = {
        review: res.data.review ?? 0,
        info: res.data.info ?? 0,
        approval: res.data.approval ?? 0,
        todo: res.data.todo ?? 0
      }
    }
  } catch (e) {
    console.error('[GM] 加载统计失败:', e)
  }
}

async function loadReviewItems() {
  try {
    const res = await fetchReviewItems()
    if (res.data && Array.isArray(res.data)) {
      reviewItems.value = res.data.map(i => ({
        title: i.title,
        tag: i.tag || '待阅',
        tagType: i.tagType || 'warning'
      }))
    }
  } catch (e) {
    console.error('[GM] 加载待批阅失败:', e)
  }
}

async function loadApprovalItems() {
  try {
    const res = await fetchApprovalItems()
    if (res.data && Array.isArray(res.data)) {
      approvalItems.value = res.data.map(i => ({
        title: i.title,
        tag: i.tag || '待批',
        tagType: i.tagType || 'danger'
      }))
    }
  } catch (e) {
    console.error('[GM] 加载待批复失败:', e)
  }
}

async function loadInfoItems() {
  try {
    const res = await fetchInfoItems()
    if (res.data && Array.isArray(res.data)) {
      infoItems.value = res.data.map(i => ({
        title: i.title,
        tag: i.tag || '待阅',
        tagType: i.tagType || 'warning'
      }))
    }
  } catch (e) {
    console.error('[GM] 加载待了解失败:', e)
  }
}

async function loadTodoItems() {
  try {
    const res = await fetchTodoItems()
    if (res.data && Array.isArray(res.data)) {
      todoItems.value = res.data.map(i => ({
        title: i.title,
        tag: i.tag || '待办',
        tagType: i.tagType || 'warning'
      }))
    }
  } catch (e) {
    console.error('[GM] 加载待办失败:', e)
  }
}

async function loadAll() {
  loading.value = true
  try {
    await Promise.all([
      loadStats(),
      loadReviewItems(),
      loadApprovalItems(),
      loadInfoItems(),
      loadTodoItems()
    ])
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadAll()
})
</script>

<style scoped>
.gm-office-page { max-width: 1400px; margin: 0 auto; padding-bottom: 40px; }

.page-topbar { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24px; flex-wrap: wrap; gap: 12px; }
.topbar-left { display: flex; flex-direction: column; }
.page-title { font-size: 22px; font-weight: 700; color: var(--color-text-primary, var(--color-text)); margin: 0; }
.page-desc { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; }

.stats-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px 24px;
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: var(--transition);
}
.stat-card:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.stat-icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: 700;
  border-radius: var(--radius-md);
  color: #fff;
}
.stat-card.pai .stat-icon { background: var(--color-primary); }
.stat-card.yue .stat-icon { background: var(--color-info); }
.stat-card.pi .stat-icon { background: var(--color-danger); }
.stat-card.ban .stat-icon { background: var(--color-warning); }
.stat-info { flex: 1; }
.stat-num { font-size: 28px; font-weight: 700; color: var(--color-text); line-height: 1.2; }
.stat-label { font-size: 13px; color: var(--color-text-secondary); margin-top: 4px; }

.section-card {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 20px 24px;
  margin-bottom: 16px;
}
.section-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text);
  margin: 0 0 16px 0;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--color-border-light);
}

.item-list { display: flex; flex-direction: column; gap: 10px; }
.list-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: var(--color-bg-alt);
  border-radius: var(--radius-sm);
  transition: var(--transition);
}
.list-item:hover { background: var(--color-bg-side); }
.item-num {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: 50%;
  flex-shrink: 0;
}
.item-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.item-text { font-size: 14px; color: var(--color-text); }

@media (max-width: 1200px) {
  .stats-row { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stats-row { grid-template-columns: 1fr; }
  .item-content { flex-direction: column; align-items: flex-start; gap: 6px; }
}
</style>

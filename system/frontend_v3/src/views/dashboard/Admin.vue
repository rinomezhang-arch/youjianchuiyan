<template>
  <div class="admin">
    <div class="page-header">
      <h2 class="page-title">后台管理</h2>
      <p class="page-subtitle">{{ storeName }} · 可视化管理</p>
    </div>

    <div class="admin-content">
      <!-- 桌台管理 -->
      <div class="admin-panel" @click="goTo('table-layout')">
        <div class="panel-header">
          <div class="panel-icon">🪑</div>
          <h3 class="panel-title">桌台布局管理</h3>
        </div>
        <div class="panel-body">
          <div class="panel-desc">拖拽调整桌台位置、大小和分区</div>
          <div class="panel-features">
            <span>🖱️ 拖拽移动</span>
            <span>📐 调整大小</span>
            <span>🏷️ 分组管理</span>
          </div>
        </div>
        <div class="panel-arrow">→</div>
      </div>

      <!-- 菜品分类管理 -->
      <div class="admin-panel" @click="goTo('category-sort')">
        <div class="panel-header">
          <div class="panel-icon">📁</div>
          <h3 class="panel-title">菜品分类排序</h3>
        </div>
        <div class="panel-body">
          <div class="panel-desc">拖拽调整分类显示顺序</div>
          <div class="panel-features">
            <span>🔀 拖拽排序</span>
            <span>➕ 添加分类</span>
            <span>🎨 颜色标记</span>
          </div>
        </div>
        <div class="panel-arrow">→</div>
      </div>

      <!-- 菜单排序管理 -->
      <div class="admin-panel" @click="goTo('menu-sort')">
        <div class="panel-header">
          <div class="panel-icon">🍽️</div>
          <h3 class="panel-title">菜单排序管理</h3>
        </div>
        <div class="panel-body">
          <div class="panel-desc">拖拽调整菜单中菜品的显示顺序</div>
          <div class="panel-features">
            <span>🔀 拖拽排序</span>
            <span>📋 按分类管理</span>
            <span>⭐ 置顶推荐</span>
          </div>
        </div>
        <div class="panel-arrow">→</div>
      </div>

      <!-- 套餐组合管理 -->
      <div class="admin-panel" @click="goTo('packages')">
        <div class="panel-header">
          <div class="panel-icon">🎁</div>
          <h3 class="panel-title">套餐组合管理</h3>
        </div>
        <div class="panel-body">
          <div class="panel-desc">拖拽菜品组合成套餐</div>
          <div class="panel-features">
            <span>🖱️ 拖拽组合</span>
            <span>💰 定价设置</span>
            <span>🎨 套餐封面</span>
          </div>
        </div>
        <div class="panel-arrow">→</div>
      </div>

      <!-- 员工排班管理 -->
      <div class="admin-panel" @click="goTo('schedule')">
        <div class="panel-header">
          <div class="panel-icon">📅</div>
          <h3 class="panel-title">员工排班管理</h3>
        </div>
        <div class="panel-body">
          <div class="panel-desc">可视化排班表，拖拽调整班次</div>
          <div class="panel-features">
            <span>🗓️ 日历视图</span>
            <span>🔄 拖拽调班</span>
            <span>📊 统计查看</span>
          </div>
        </div>
        <div class="panel-arrow">→</div>
      </div>

      <!-- 系统设置 -->
      <div class="admin-panel" @click="goTo('settings')">
        <div class="panel-header">
          <div class="panel-icon">⚙️</div>
          <h3 class="panel-title">系统设置</h3>
        </div>
        <div class="panel-body">
          <div class="panel-desc">门店信息、营业时间、打印设置等</div>
          <div class="panel-features">
            <span>🏪 门店信息</span>
            <span>⏰ 营业时间</span>
            <span>🖨️ 打印配置</span>
          </div>
        </div>
        <div class="panel-arrow">→</div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const storeName = computed(() => userStore.storeName || '宁国店')

function goTo(path) {
  router.push(`/dashboard/${path}`)
}
</script>

<style scoped>
.admin {
  max-width: 1200px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 24px;
}

.page-title {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text);
  margin-bottom: 4px;
}

.page-subtitle {
  font-size: 14px;
  color: var(--color-text-muted);
}

.admin-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.admin-panel {
  background: var(--color-card);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  cursor: pointer;
  transition: var(--transition);
  position: relative;
}

.admin-panel:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-primary);
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 20px;
  background: linear-gradient(135deg, rgba(45, 74, 62, 0.04) 0%, transparent 100%);
  border-bottom: 1px solid var(--color-border);
}

.panel-icon {
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  background: var(--color-bg);
  border-radius: var(--radius-md);
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text);
}

.panel-body {
  padding: 16px 20px;
}

.panel-desc {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin-bottom: 12px;
  line-height: 1.5;
}

.panel-features {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.panel-features span {
  font-size: 12px;
  color: var(--color-text-muted);
  padding: 4px 10px;
  background: var(--color-bg);
  border-radius: 12px;
}

.panel-arrow {
  position: absolute;
  right: 20px;
  bottom: 20px;
  font-size: 16px;
  color: var(--color-text-muted);
  transition: var(--transition);
}

.admin-panel:hover .panel-arrow {
  color: var(--color-primary);
  transform: translateX(4px);
}

@media (max-width: 768px) {
  .admin-content {
    grid-template-columns: 1fr;
  }
}
</style>

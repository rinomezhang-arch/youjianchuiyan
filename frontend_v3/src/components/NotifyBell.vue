<template>
  <div class="notify-bell">
    <!-- 铃铛图标 -->
    <div class="bell-icon" @click="togglePanel" :class="{ active: unreadCount > 0 }">
      <el-icon :size="20">
        <Bell />
      </el-icon>
      <span v-if="unreadCount > 0" class="badge">{{ unreadCount > 99 ? '99+' : unreadCount }}</span>
    </div>

    <!-- 实时 Toast 弹出 -->
    <Transition name="toast">
      <div v-if="toastVisible" class="notify-toast" :class="toastPriority">
        <div class="toast-header">
          <span class="toast-type">{{ toastData?.title }}</span>
          <el-icon class="close" @click="toastVisible = false"><Close /></el-icon>
        </div>
        <div class="toast-body">{{ toastData?.content }}</div>
        <div class="toast-time">{{ toastData?.receivedAt }}</div>
      </div>
    </Transition>

    <!-- 通知列表面板 -->
    <Transition name="dropdown">
      <div v-if="panelOpen" class="notify-panel">
        <div class="panel-header">
          <span>实时通知</span>
          <div class="panel-actions">
            <el-button v-if="unreadCount > 0" size="small" text @click="markAllRead">全部已读</el-button>
            <el-button v-if="notifications.length > 0" size="small" text @click="clearAll">清空</el-button>
          </div>
        </div>
        <div class="panel-body">
          <div v-if="notifications.length === 0" class="empty">
            <el-icon :size="32" color="#ccc"><Bell /></el-icon>
            <p>暂无通知</p>
          </div>
          <div
            v-for="(n, i) in notifications"
            :key="i"
            class="notify-item"
            :class="{ unread: !n.read, urgent: n.priority === 'urgent', high: n.priority === 'high' }"
            @click="markOneRead(i)"
          >
            <div class="item-indicator"></div>
            <div class="item-content">
              <div class="item-title">{{ n.title }}</div>
              <div class="item-body">{{ n.content }}</div>
              <div class="item-meta">
                <span class="item-type">{{ formatType(n.notifyType) }}</span>
                <span class="item-time">{{ n.receivedAt }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- 连接状态指示 -->
    <span v-if="!connected" class="ws-offline" title="通知连接已断开，正在重连...">
      <el-icon :size="12"><Warning /></el-icon>
    </span>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useNotify } from '@/composables/useNotify'
import { Bell, Close, Warning } from '@element-plus/icons-vue'

const { connected, notifications, unreadCount, onMessage, markAllRead, markOneRead, clearAll } = useNotify()

const panelOpen = ref(false)
const toastVisible = ref(false)
const toastData = ref(null)
const toastPriority = ref('normal')
let toastTimer = null

function togglePanel() {
  panelOpen.value = !panelOpen.value
  if (panelOpen.value) {
    markAllRead()
  }
}

function showToast(data) {
  toastData.value = data
  toastPriority.value = data.priority || 'normal'
  toastVisible.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => {
    toastVisible.value = false
  }, 4000)
}

function formatType(type) {
  const map = {
    'order.created': '🍽 新订单',
    'dish.served': '✅ 出品完成',
    'booking.created': '📅 预订创建',
    'booking.confirmed': '✅ 预订确认',
    'booking.cancelled': '❌ 预订取消',
    'inventory.low_stock': '⚠️ 库存预警',
    'purchase.received': '📦 采购入库',
    'reimbursement.approved': '💰 报销审批',
    'table.occupancy_warn': '🔔 桌台提醒'
  }
  return map[type] || type
}

let unsubscribe = null

onMounted(() => {
  unsubscribe = onMessage((data) => {
    showToast(data)
    // 可选：播放提示音
    // playBeep()
  })
})

onUnmounted(() => {
  if (unsubscribe) unsubscribe()
  if (toastTimer) clearTimeout(toastTimer)
})
</script>

<style scoped>
.notify-bell {
  position: relative;
  display: flex;
  align-items: center;
}

.bell-icon {
  cursor: pointer;
  padding: 8px 10px;
  border-radius: 8px;
  transition: background 0.2s;
  color: var(--color-text-regular);
  display: flex;
  align-items: center;
  position: relative;
}

.bell-icon:hover {
  background: var(--color-fill);
}

.bell-icon.active {
  color: var(--el-color-primary);
}

.badge {
  position: absolute;
  top: 2px;
  right: 2px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  background: var(--el-color-danger);
  color: white;
  font-size: 10px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
}

.ws-offline {
  margin-left: 6px;
  color: var(--el-color-warning);
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

/* Toast 通知 */
.notify-toast {
  position: fixed;
  top: 70px;
  right: 24px;
  width: 340px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 9999;
  border-left: 4px solid var(--el-color-primary);
}

.notify-toast.urgent {
  border-left-color: var(--el-color-danger);
  background: linear-gradient(180deg, #fff5f5 0%, #fff 40px);
}

.notify-toast.high {
  border-left-color: var(--el-color-warning);
  background: linear-gradient(180deg, #fff9ed 0%, #fff 40px);
}

.notify-toast.normal {
  border-left-color: var(--el-color-primary);
}

.toast-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px 6px;
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary);
}

.toast-header .close {
  cursor: pointer;
  color: var(--color-text-placeholder);
  transition: color 0.2s;
}

.toast-header .close:hover {
  color: var(--color-text-primary);
}

.toast-body {
  padding: 0 14px 6px;
  font-size: 13px;
  color: var(--color-text-regular);
  line-height: 1.5;
}

.toast-time {
  padding: 4px 14px 10px;
  font-size: 11px;
  color: var(--color-text-placeholder);
}

/* 下拉面板 */
.notify-panel {
  position: absolute;
  top: 42px;
  right: 0;
  width: 360px;
  max-height: 480px;
  background: white;
  border-radius: 12px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  overflow: hidden;
  z-index: 9998;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  border-bottom: 1px solid var(--color-border-lighter);
  font-weight: 600;
  font-size: 14px;
  color: var(--color-text-primary);
}

.panel-actions {
  display: flex;
  gap: 4px;
}

.panel-body {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
}

.empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: var(--color-text-placeholder);
  gap: 8px;
}

.empty p {
  font-size: 13px;
  margin: 0;
}

.notify-item {
  display: flex;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
  border-bottom: 1px solid var(--color-border-lighter);
}

.notify-item:last-child {
  border-bottom: none;
}

.notify-item:hover {
  background: var(--color-fill);
}

.notify-item.unread {
  background: rgba(64, 158, 255, 0.04);
}

.notify-item.unread .item-indicator::before {
  content: '';
  display: block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--el-color-primary);
}

.notify-item.high .item-indicator::before {
  background: var(--el-color-warning);
}

.notify-item.urgent .item-indicator::before {
  background: var(--el-color-danger);
}

.item-indicator {
  flex-shrink: 0;
  width: 16px;
  display: flex;
  justify-content: center;
  padding-top: 4px;
}

.item-indicator::before {
  content: '';
  display: block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: transparent;
}

.item-content {
  flex: 1;
  min-width: 0;
}

.item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 2px;
}

.item-body {
  font-size: 12px;
  color: var(--color-text-regular);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.item-meta {
  display: flex;
  gap: 12px;
  margin-top: 6px;
  font-size: 11px;
  color: var(--color-text-placeholder);
}

/* 动画 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.3s ease;
}

.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.2s ease;
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}
</style>

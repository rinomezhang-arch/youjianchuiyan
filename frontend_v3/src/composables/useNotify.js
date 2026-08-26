import { ref, onUnmounted } from 'vue'
import { useUserStore } from '@/store/user'

// 单例 WebSocket 客户端
let ws = null
let reconnectTimer = null
let reconnectDelay = 3000 // 3 秒重连
const listeners = new Set()
const connected = ref(false)
const notifications = ref([]) // 最近的通知列表
const unreadCount = ref(0)

/**
 * 通知 WebSocket 客户端 — 自动连接、自动重连、多监听器分发。
 */
export function useNotify() {
  const userStore = useUserStore()

  function connect() {
    if (ws && ws.readyState === WebSocket.OPEN) return

    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
    const host = window.location.host
    const storeId = userStore.currentStoreId || 1
    const staffId = userStore.staffId || ''
    const url = `${protocol}//${host}/ws/notify?storeId=${storeId}&staffId=${staffId}`

    try {
      ws = new WebSocket(url)

      ws.onopen = () => {
        connected.value = true
        reconnectDelay = 3000
        console.log('[Notify] WebSocket 已连接', url)
      }

      ws.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (data.type === 'pong') return
          // 通知事件
          notifications.value.unshift({
            ...data,
            receivedAt: new Date().toLocaleTimeString('zh-CN')
          })
          if (notifications.value.length > 50) {
            notifications.value = notifications.value.slice(0, 50)
          }
          unreadCount.value++
          // 分发到所有监听器
          listeners.forEach(fn => {
            try { fn(data) } catch (e) { console.error(e) }
          })
        } catch (e) {
          // 非 JSON 消息忽略
        }
      }

      ws.onclose = () => {
        connected.value = false
        console.log('[Notify] WebSocket 已断开，3秒后重连...')
        scheduleReconnect()
      }

      ws.onerror = (err) => {
        console.error('[Notify] WebSocket 错误:', err)
      }
    } catch (e) {
      console.error('[Notify] WebSocket 创建失败:', e)
      scheduleReconnect()
    }
  }

  function scheduleReconnect() {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    reconnectTimer = setTimeout(() => {
      reconnectDelay = Math.min(reconnectDelay * 2, 30000) // 指数退避，最大 30s
      connect()
    }, reconnectDelay)
  }

  function disconnect() {
    if (reconnectTimer) clearTimeout(reconnectTimer)
    if (ws) {
      ws.close()
      ws = null
    }
    connected.value = false
  }

  function onMessage(fn) {
    listeners.add(fn)
    return () => listeners.delete(fn)
  }

  function markAllRead() {
    unreadCount.value = 0
  }

  function markOneRead(index) {
    if (notifications.value[index]) {
      notifications.value[index].read = true
      unreadCount.value = Math.max(0, unreadCount.value - 1)
    }
  }

  function clearAll() {
    notifications.value = []
    unreadCount.value = 0
  }

  // 自动连接（首次调用时）
  if (!ws) connect()

  onUnmounted(() => {
    // 不在此处 disconnect，因为是单例
    // 页面组件卸载不应该断开全局连接
  })

  return {
    connected,
    notifications,
    unreadCount,
    onMessage,
    markAllRead,
    markOneRead,
    clearAll,
    connect,
    disconnect
  }
}

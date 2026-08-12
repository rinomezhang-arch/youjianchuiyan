import request from '@/utils/request'

/**
 * 总经办 - GM Office API
 */

// 获取统计数字（待批阅、待了解、待批复、待办数量）
export function getGMStats() {
  return request({ url: '/gm/stats', method: 'get' })
}

// 获取待批阅事项
export function getReviewItems() {
  return request({ url: '/gm/review', method: 'get' })
}

// 获取待批复事项
export function getApprovalItems() {
  return request({ url: '/gm/approval', method: 'get' })
}

// 获取待了解事项
export function getInfoItems() {
  return request({ url: '/gm/info', method: 'get' })
}

// 获取待办事项
export function getTodoItems() {
  return request({ url: '/gm/todo', method: 'get' })
}

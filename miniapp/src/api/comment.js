/**
 * 评价 / 反馈 Comments
 * 后端契约参考（CommentController）：
 *   GET  /comments/order/{orderId}        查询某订单已写评价（用于判重）
 *   POST /comments                        提交评价
 *     { orderId, rating(1-5), tags[], images[], text, anonymous }
 *   GET  /comments/dish/{dishId}          某菜的评价列表
 *   POST /feedback                        意见反馈（给门店/总部）
 *     { type: '菜品/服务/环境/建议/其他', content, contact, images[] }
 */
import http from '@/utils/request'

export function fetchDishComments(dishId, params = {}) {
  return http.get(`/comments/dish/${dishId}`, { page: 1, size: 20, ...params }, { skipToken: true })
}

export function fetchOrderComment(orderId) {
  return http.get(`/comments/order/${orderId}`, {}, { silent: true })
}

export function submitComment(payload) {
  return http.post('/comments', payload)
}

export function submitFeedback(payload) {
  return http.post('/feedback', payload, {})
}

export default { fetchDishComments, fetchOrderComment, submitComment, submitFeedback }

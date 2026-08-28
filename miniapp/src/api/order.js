/**
 * 点餐订单 Orders（堂食扫码点餐 / 自取 / 外卖 的订单统一接口）
 * 与 Bookings（预订）是两个不同概念：
 *   Booking = 预约桌位/包厢（不一定点菜）
 *   Order   = 实际菜品下单（有SKU/数量/金额）
 *
 * 后端契约参考（建议在 OrderController 中实现）：
 *   GET    /orders                我的订单列表（筛选：status=0待支付 1已支付待上菜 2制作中 3待取 4已完成 5已取消）
 *   GET    /orders/{id}           订单详情
 *   POST   /orders                下单（见下方 payload 结构）
 *   PUT    /orders/{id}/cancel    取消订单（限待支付状态）
 *   POST   /orders/{id}/again     再来一单（把原订单的菜品再塞进购物车）
 */
import http from '@/utils/request'
import { useAppStore } from '@/store/app'
import { useCartStore } from '@/store/cart'

/**
 * 订单列表
 * @param {Object} params  { page, size, status: number|undefined, keyword }
 */
export function fetchMyOrders(params = {}) {
  return http.get('/orders', { page: 1, size: 50, ...params })
}

export function fetchOrderDetail(id) {
  return http.get(`/orders/${id}`)
}

/**
 * 根据购物车构建下单 payload 并提交
 * 后端 payload 约定：
 * {
 *   storeId,
 *   orderType: 'EAT_IN' | 'TAKEAWAY' | 'DELIVERY',   // 堂食/自取/外卖
 *   tableNo?: string,                                   // 扫码点餐桌号
 *   addressId?: number,                                 // 外卖地址
 *   items: [{ dishId, count, priceFen, remark }],       // 明细
 *   remark: string,                                     // 订单级备注
 *   couponId?: number,                                  // 核销的优惠券
 *   expectTime?: string,                                // 期望送达/取餐时间
 *   contact?: { name, phone }                           // 联系信息（自取/外卖）
 * }
 */
export function submitOrder(extra = {}) {
  const appStore = useAppStore()
  const cart = useCartStore()
  cart.hydrate()
  if (cart.isEmpty) return Promise.reject(new Error('购物车为空'))

  const payload = {
    storeId:   appStore.currentStoreId,
    openId:    appStore.openId || undefined,
    orderType: extra.orderType || 'EAT_IN',
    items:     cart.items.map(i => ({
      dishId:  i.id,
      name:    i.name,
      count:   i.count,
      priceFen: i.priceFen,
      image:    i.image
    })),
    totalFen:  cart.totalFen,
    remark:    extra.remark || '',
    ...extra
  }
  return http.post('/orders', payload)
}

export function cancelOrder(id, reason = '用户取消') {
  return http.put(`/orders/${id}/cancel`, { reason })
}

export function orderAgain(id) {
  return http.post(`/orders/${id}/again`, {})
}

export default { fetchMyOrders, fetchOrderDetail, submitOrder, cancelOrder, orderAgain }

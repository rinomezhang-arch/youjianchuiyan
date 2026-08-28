/**
 * 优惠券 Coupons
 * 后端契约参考（CouponController）：
 *   GET  /coupons/available              活动可领取券列表
 *   GET  /coupons/mine                   我的券（status：0未使用、1已使用、2过期/失效）
 *   POST /coupons/{id}/receive           领券（传入 id）
 *   POST /coupons/verify-by-code         口令领券（例如朋友圈分享的口令码）
 *   GET  /coupons/useful-for-order       下单页可用优惠券（根据订单金额/商品池筛选）
 */
import http from '@/utils/request'

export function fetchAvailableCoupons() {
  return http.get('/coupons/available', {}, { skipToken: false })
}

export function fetchMyCoupons(status = '') {
  return http.get('/coupons/mine', status !== '' ? { status } : {})
}

export function receiveCoupon(id) {
  return http.post(`/coupons/${id}/receive`, {})
}

export function receiveCouponByCode(code) {
  return http.post('/coupons/verify-by-code', { code })
}

/**
 * 给下单页用：按订单金额筛选可用券
 * @param {number} orderFen 订单总金额（分）
 */
export function fetchUsefulCoupons(orderFen, storeId) {
  return http.get('/coupons/useful-for-order', { orderFen, storeId })
}

export default {
  fetchAvailableCoupons, fetchMyCoupons,
  receiveCoupon, receiveCouponByCode,
  fetchUsefulCoupons
}

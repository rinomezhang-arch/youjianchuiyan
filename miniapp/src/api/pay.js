/**
 * 支付相关后端接口
 *  前端支付流程：
 *   1) POST /pay/create  后端用 JSAPI 下单，返回 { paySign, nonceStr, package, ... }
 *   2) wx.js wechatPay() 调起微信小程序原生支付弹窗
 *   3) 后端通过 notifyUrl 接收微信回调 → 更新订单状态
 *   4) 前端轮询或跳"订单详情"页展示最终状态
 */
import http from '@/utils/request'

/**
 * 发起支付
 * @param {Object} data
 * @param {string} data.orderType  booking(预订定金) | dish(堂食/外卖菜品) | package(套餐)
 * @param {number} data.bizId      业务主键（bookingId/dishOrderId/packageOrderId）
 * @param {number} data.amountFen  金额（分），若后端能从 bizId 反查可以不传
 * @param {string} data.attach     透传字段（可选）
 */
export function createPayOrder(data) {
  return http.post('/pay/create', data)
}

/** 查询支付状态（轮询用） */
export function queryPayStatus(orderNo) {
  return http.get('/pay/status', { orderNo })
}

export default { createPayOrder, queryPayStatus }

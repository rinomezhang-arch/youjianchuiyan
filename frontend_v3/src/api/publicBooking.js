import request from '@/utils/request'

/**
 * 客人自助查预订：**手机号与预订单号必须同时给**。
 *
 * 后端 POST /api/public/booking-lookup 把「查无 / 手机号不符 / 别人的单 / 别店的单 /
 * 缺参数 / 格式非法」统一成同一种空结果，连"订单不存在"都不说，
 * 为的是不让人拿一个单号去试不同手机号反推机主。
 *
 * **前端必须保持同一口径。** 如果界面按不同情况分开提示，
 * 后端堵住的枚举口子就在前端重新开了。所以下面只有两种结局：查到，或者一句统一的话。
 */
export function lookupBooking(payload) {
  return request({ url: '/public/booking-lookup', method: 'post', data: payload })
}

/** 员工确认：把咨询转成正式预订。日期、时间、桌台三者必填，系统不替员工选桌。 */
export function convertInquiry(inquiryId, payload) {
  return request({ url: `/booking-inquiries/${inquiryId}/convert`, method: 'post', data: payload })
}

/** 咨询列表（员工端）。 */
export function listInquiries(params) {
  return request({ url: '/booking-inquiries', method: 'get', params })
}

// 纯契约逻辑在 publicBookingContract.js，那边没有任何 import，可被 node 直接单测。
export {
  LOOKUP_NOT_FOUND, LOOKUP_FIELDS,
  validateLookupInput, readLookupResult, bookingStatusText,
  validateConvertInput, readConvertResult
} from './publicBookingContract.js'

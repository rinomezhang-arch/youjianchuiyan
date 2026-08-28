/**
 * 预订 Booking
 * 后端契约参考：BookingsController
 *   GET    /bookings          我的预订列表（需要登录）
 *   GET    /bookings/{id}     详情
 *   POST   /bookings          新建预订（提交表单）
 *   PUT    /bookings/{id}     修改/取消
 *   GET    /bookings/stats    统计（可选）
 *
 * 预订 payload 字段建议（对齐你前端的 BookingCreateDTO）：
 *   storeId, bookingDate(YYYY-MM-DD), bookingTime(HH:mm),
 *   guestCount, roomType(null/包厢), customerName, customerPhone,
 *   remark, deposit(定金分，可选), openId（可选，后端兜底）
 */
import http from '@/utils/request'
import { useAppStore } from '@/store/app'

export function fetchMyBookings(params = {}) {
  return http.get('/bookings', { page: 1, size: 50, ...params })
}

export function fetchBookingDetail(id) {
  return http.get(`/bookings/${id}`)
}

export function createBooking(payload) {
  const appStore = useAppStore()
  // 自动补 openId 和 storeId（若调用方没传）
  const body = {
    storeId:   appStore.currentStoreId,
    openId:    appStore.openId || undefined,
    ...payload
  }
  return http.post('/bookings', body)
}

export function updateBooking(id, payload) {
  return http.put(`/bookings/${id}`, payload)
}

export function cancelBooking(id, reason = '用户取消') {
  return http.put(`/bookings/${id}`, { status: -1, cancelReason: reason })
}

export default { fetchMyBookings, fetchBookingDetail, createBooking, updateBooking, cancelBooking }

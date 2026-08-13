import axios from 'axios'
import { useIpadStore } from '@/store/ipad'

/**
 * iPad 点餐子系统 API 封装
 * 统一注入 4 组请求头，字段名 snake_case 不转换
 */
const ipadRequest = axios.create({
  baseURL: '/api/ipad',
  timeout: 60000,
  headers: { 'Content-Type': 'application/json' }
})

// 请求拦截器：自动注入 4 组 Header
ipadRequest.interceptors.request.use(config => {
  const ipad = useIpadStore()
  config.headers['X-Store-Id'] = ipad.storeId
  config.headers['X-Staff-Id'] = ipad.staffId || 0
  config.headers['X-Device-Sn'] = ipad.deviceSn
  config.headers['X-Client-Type'] = 'ipad'
  return config
})

// 响应拦截器
ipadRequest.interceptors.response.use(
  res => res.data,
  err => {
    if (err.response?.status === 401) {
      // iPad 端跳回登录
      window.location.href = '/ipad/login'
    }
    return Promise.reject(err)
  }
)

// ========== 模块1：登录与设备认证 ==========
export const ipadLogin = (username, password) => ipadRequest.post('/login', { username, password })
export const ipadStoreList = () => ipadRequest.get('/store/list')
export const ipadDeviceBind = (data) => ipadRequest.post('/device/bind', data)
export const ipadPrintConfig = () => ipadRequest.get('/config/print')

// ========== 模块2：桌台与预定 ==========
export const ipadTableAll = (area) => ipadRequest.get('/table/all', { params: area ? { area } : {} })
export const ipadTableFilter = (status) => ipadRequest.get('/table/filter', { params: { status } })
export const ipadTableOpen = (data) => ipadRequest.post('/table/open', data)
export const ipadTableTransfer = (data) => ipadRequest.post('/table/transfer', data)
export const ipadBookingToday = (date) => ipadRequest.get('/booking/today', { params: date ? { date } : {} })
export const ipadWaitList = () => ipadRequest.get('/wait/list')

// ========== 模块3：点餐核心 ==========
export const ipadDishCategory = () => ipadRequest.get('/dish/category')
export const ipadDishList = (params) => ipadRequest.get('/dish/list', { params })
export const ipadDishDetail = (dishId) => ipadRequest.get(`/dish/detail/${dishId}`)
export const ipadDishSearch = (keyword) => ipadRequest.get('/dish/search', { params: { keyword } })
export const ipadPackageList = () => ipadRequest.get('/package/list')
export const ipadTemplateList = (banquetTypeId) => ipadRequest.get('/template/list', { params: { banquet_type_id: banquetTypeId } })
export const ipadOrderAdd = (data) => ipadRequest.post('/order/dish/add', data)
export const ipadOrderEdit = (data) => ipadRequest.put('/order/dish/edit', data)
export const ipadOrderRemove = (dishBookingId) => ipadRequest.delete('/order/dish/remove', { data: { dish_booking_id: dishBookingId } })
export const ipadOrderRefund = (data) => ipadRequest.post('/order/dish/refund', data)
export const ipadOrderSendKitchen = (bookingId) => ipadRequest.post('/order/send-kitchen', { booking_id: bookingId })
export const ipadOrderSubmit = (data) => ipadRequest.post('/order/submit', data)
export const ipadOrderUrgent = (dishBookingId) => ipadRequest.post('/order/urgent', { dish_booking_id: dishBookingId })

// ========== 客人自助点菜：服务员授权 + 加菜 ==========
export const ipadAuthVerify = (data) => ipadRequest.post('/auth/verify', data)
export const ipadOrderAddDishes = (data) => ipadRequest.post('/order/add-dishes', data)
export const ipadOrderDetail = (bookingId) => ipadRequest.get('/order/detail', { params: { booking_id: bookingId } })

// ========== 模块4：结算财务 ==========
export const ipadBillDetail = (bookingId) => ipadRequest.get(`/settlement/bill/${bookingId}`)
export const ipadCouponAvailable = (params) => ipadRequest.get('/coupon/available', { params })
export const ipadSettlementDiscount = (data) => ipadRequest.post('/settlement/discount', data)
export const ipadSettlementPay = (data, idempotencyKey) => ipadRequest.post('/settlement/pay', data, {
  headers: { 'Idempotency-Key': idempotencyKey }
})
export const ipadSettlementHistory = (params) => ipadRequest.get('/settlement/history', { params })
export const ipadSettlementInvoice = (data) => ipadRequest.post('/settlement/invoice', data)
export const ipadSettlementDeposit = (data) => ipadRequest.post('/settlement/deposit', data)

// ========== 模块5：会员与客户 ==========
export const ipadMemberSearch = (phone) => ipadRequest.get('/member/search', { params: { phone } })
export const ipadMemberRecharge = (data) => ipadRequest.post('/member/recharge', data)
export const ipadMemberPoint = (customerId) => ipadRequest.get('/member/point', { params: { customer_id: customerId } })
export const ipadCustomerCreate = (data) => ipadRequest.post('/customer/create', data)

// ========== 模块6：辅助功能 ==========
export const ipadStockCheck = (dishId) => ipadRequest.get('/stock/check', { params: { dish_id: dishId } })
export const ipadServiceCall = (data) => ipadRequest.post('/service/call', data)
export const ipadAiChat = (data) => ipadRequest.post('/ai/chat', data)

export default ipadRequest

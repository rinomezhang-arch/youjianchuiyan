import request from '@/utils/request'

export function getCustomers(params) {
  return request({ url: '/customers', method: 'get', params })
}
export function createCustomer(data) {
  return request({ url: '/customers', method: 'post', data })
}
export function updateCustomer(id, data) {
  return request({ url: `/customers/${id}`, method: 'put', data })
}
export function searchCustomers(params) {
  return request({ url: '/customers/search', method: 'get', params })
}
export function getCustomerHistory(customerId) {
  return request({ url: `/customers/${customerId}/history`, method: 'get' })
}

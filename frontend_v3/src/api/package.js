import request from '@/utils/request'

export function getPackages(params) {
  return request({ url: '/packages', method: 'get', params })
}
export function getPackageDetail(id) {
  return request({ url: `/packages/${id}`, method: 'get' })
}
export function createPackage(data) {
  return request({ url: '/packages', method: 'post', data })
}
export function updatePackage(id, data) {
  return request({ url: `/packages/${id}`, method: 'put', data })
}

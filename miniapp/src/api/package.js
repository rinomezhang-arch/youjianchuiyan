/**
 * 宴会套餐 Packages
 * 后端契约参考：PackagesController
 *   GET /packages     列表（filter: type=wedding/birthday/company 等）
 *   GET /packages/{id} 详情
 */
import http from '@/utils/request'

export function fetchPackages(params = {}) {
  return http.get('/packages', { status: 1, size: 100, ...params }, { skipToken: true })
}

export function fetchPackageDetail(id) {
  return http.get(`/packages/${id}`, {}, { skipToken: true })
}

export default { fetchPackages, fetchPackageDetail }

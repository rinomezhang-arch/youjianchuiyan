/**
 * 门店 Store
 * 后端契约参考：src/main/java/.../controller/StoresController
 *   GET /stores          返回门店列表
 *   GET /stores/{id}     返回单门店详情（营业时段/地址/电话等）
 */
import http from '@/utils/request'

export function fetchStores(params = {}) {
  return http.get('/stores', params, { skipStoreId: true, skipToken: true })
}

export function fetchStoreDetail(id) {
  return http.get(`/stores/${id}`, {}, { skipToken: true })
}

export default { fetchStores, fetchStoreDetail }

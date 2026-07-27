import request from '@/utils/request'

/**
 * 数据字典 API
 * 所有下拉选项数据均从数据库字典表获取，保证数据有效性
 */

// 查询所有字典类型
export function getDictTypes() {
  return request.get('/dict/types')
}

// 按字典编码查询字典项
export function getDictItems(dictCode) {
  return request.get(`/dict/items/${dictCode}`)
}

// 批量查询多个字典类型的字典项
// codes: 'occasion_type,source_type,booking_type,time_slot,booking_status'
// 返回: { occasion_type: [...], source_type: [...], ... }
export function getDictBatch(codes) {
  return request.get('/dict/batch', { params: { codes } })
}

// 新增字典项
export function addDictItem(data) {
  return request.post('/dict/items', data)
}

// 更新字典项
export function updateDictItem(itemId, data) {
  return request.put(`/dict/items/${itemId}`, data)
}

// 删除字典项（软删除）
export function deleteDictItem(itemId) {
  return request.delete(`/dict/items/${itemId}`)
}

// ===== 员工列表（预定员下拉） =====

// 获取员工列表，支持关键词搜索（姓名/工号/手机号）
export function getStaffList(keyword) {
  const params = {}
  if (keyword) params.keyword = keyword
  return request.get('/dict/staff', { params })
}

// ===== 客户模糊搜索 =====

// 客户模糊搜索（姓名/手机号），用于客户姓名、代订人、介绍人下拉
export function searchCustomersDict(keyword) {
  const params = {}
  if (keyword) params.keyword = keyword
  return request.get('/dict/customers', { params })
}

import request from '@/utils/request'

// ===== 菜品 =====
export function getDishes(params) {
  return request.get('/dishes', { params })
}

export function createDish(data) {
  return request.post('/dishes', data)
}

export function updateDish(id, data) {
  return request.put(`/dishes/${id}`, data)
}

export function deleteDish(id) {
  return request.delete(`/dishes/${id}`)
}

export function getCategories() {
  return request.get('/dishes/categories')
}

export function searchDishes(q, usageType) {
  return request.get('/dishes/search', { params: { q, usageType } })
}

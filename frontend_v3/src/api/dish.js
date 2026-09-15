import request from '@/utils/request'

// ===== 菜品：标准 /dishes 接口 =====
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

// ===== 点餐系统 Bt*：/api/bt/* 接口（联通 Caipinxinxi / Caipinleixing 等源路径）=====
export function btDishPage(params) {
  return request.get('/bt/dish-info/page', { params })
}

export function btDishList(params) {
  return request.get('/caipinxinxi/list', { params })
}

export function btDishInfo(id) {
  return request.get(`/caipinxinxi/info/${id}`)
}

export function btDishSave(data) {
  return request.post('/caipinxinxi/save', data)
}

export function btDishUpdate(data) {
  return request.post('/caipinxinxi/update', data)
}

export function btDishDelete(ids) {
  return request.post('/caipinxinxi/delete', ids)
}

export function btDishTypePage(params) {
  return request.get('/caipinleixing/page', { params })
}

export function btDishTypeList() {
  return request.get('/caipinleixing/lists')
}

export function btDishTypeSave(data) {
  return request.post('/caipinleixing/save', data)
}

export function btDishTypeUpdate(data) {
  return request.post('/caipinleixing/update', data)
}

export function btDishTypeDelete(ids) {
  return request.post('/caipinleixing/delete', ids)
}

export function btCartPage(params) {
  return request.get('/cart/page', { params })
}

export function btCartListByUser(userid) {
  return request.get('/cart/lists', { params: { userid } })
}

export function btCartSave(data) {
  return request.post('/cart/save', data)
}

export function btCartUpdate(data) {
  return request.post('/cart/update', data)
}

export function btCartDelete(ids) {
  return request.post('/cart/delete', ids)
}

export function btOrderPage(params) {
  return request.get('/orders/page', { params })
}

export function btOrderListByUser(userid) {
  return request.get('/orders/lists', { params: { userid } })
}

export function btOrderSave(data) {
  return request.post('/orders/save', data)
}

export function btOrderUpdate(data) {
  return request.post('/orders/update', data)
}

export function btOrderDelete(ids) {
  return request.post('/orders/delete', ids)
}

export function btOrderStatValue(xColumn, yColumn) {
  return request.get(`/orders/value/${xColumn}/${yColumn}`)
}

export function btOrderGroup(columnName) {
  return request.get(`/orders/group/${columnName}`)
}

export function btTableInfoPage(params) {
  return request.get('/canzhuoxinxi/page', { params })
}

export function btTableInfoList() {
  return request.get('/canzhuoxinxi/lists')
}

export function btTableInfoSave(data) {
  return request.post('/canzhuoxinxi/save', data)
}

export function btTableInfoUpdate(data) {
  return request.post('/canzhuoxinxi/update', data)
}

export function btTableInfoDelete(ids) {
  return request.post('/canzhuoxinxi/delete', ids)
}

export function btTableUsagePage(params) {
  return request.get('/canzhuoshiyong/page', { params })
}

export function btTableUsageSave(data) {
  return request.post('/canzhuoshiyong/save', data)
}

export function btTableUsageUpdate(data) {
  return request.post('/canzhuoshiyong/update', data)
}

export function btTableUsageDelete(ids) {
  return request.post('/canzhuoshiyong/delete', ids)
}

import request from '@/utils/request'

export function getTableStatus(params) {
  return request({ url: '/tables', method: 'get', params })
}
export function createTable(data) {
  return request({ url: '/tables', method: 'post', data })
}
export function updateTable(id, data) {
  return request({ url: `/tables/${id}`, method: 'put', data })
}
export function deleteTable(id) {
  return request({ url: `/tables/${id}`, method: 'delete' })
}
export function reorderTables(data) {
  return request({ url: '/tables/reorder', method: 'post', data })
}
export function swapTableBooking(data) {
  return request({ url: '/tables/swap-booking', method: 'post', data })
}

export function getBookings(params) {
  return request({ url: '/bookings', method: 'get', params })
}
export function listBookings(params) {
  return request({ url: '/bookings/list', method: 'get', params })
}
export function createBooking(data) {
  return request({ url: '/bookings', method: 'post', data })
}
export function updateBooking(id, data) {
  return request({ url: `/bookings/${id}`, method: 'put', data })
}
export function cancelBooking(id) {
  return request({ url: `/bookings/${id}`, method: 'delete' })
}
export function getTableBoard(params) {
  return request({ url: '/tables/board', method: 'get', params })
}
export function copyBooking(data) {
  return request({ url: '/bookings/copy', method: 'post', data })
}
export function swapBooking(data) {
  return request({ url: '/bookings/swap', method: 'post', data })
}
export function getBookingDetail(id) {
  return request({ url: `/bookings/${id}`, method: 'get' })
}
export function getBookingStats(params) {
  return request({ url: '/bookings/stats', method: 'get', params })
}

export function getTodayOverview(params) {
  return request({ url: '/dashboard/today', method: 'get', params })
}
export function getDashboardReport() {
  return request({ url: '/dashboard/report', method: 'get' })
}

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
export function getCustomerHistory(id) {
  return request({ url: `/customers/${id}/history`, method: 'get' })
}

export function getDishes(params) {
  return request({ url: '/dishes', method: 'get', params })
}
export function createDish(data) {
  return request({ url: '/dishes', method: 'post', data })
}
export function updateDish(id, data) {
  return request({ url: `/dishes/${id}`, method: 'put', data })
}

export function getRecipe(dishId) {
  return request({ url: `/recipes/${dishId}`, method: 'get' })
}
export function saveRecipe(dishId, items) {
  return request({ url: `/recipes/${dishId}`, method: 'post', data: items })    
}
export function getDishesWithRecipe() {
  return request({ url: '/recipes/dishes-with-recipe', method: 'get' })
}

export function getPackages(params) {
  return request({ url: '/packages', method: 'get', params })
}
export function createPackage(data) {
  return request({ url: '/packages', method: 'post', data })
}
export function updatePackage(id, data) {
  return request({ url: `/packages/${id}`, method: 'put', data })
}

export function getStaffList(params) {
  return request({ url: '/menu-api/staff', method: 'get', params })
}
export function createStaff(data) {
  return request({ url: '/staff', method: 'post', data })
}
export function updateStaff(id, data) {
  return request({ url: `/staff/${id}`, method: 'put', data })
}
export function deleteStaff(id) {
  return request({ url: `/staff/${id}`, method: 'delete' })
}

export function getSuppliers(params) {
  return request({ url: '/menu-api/suppliers', method: 'get', params })
}
export function createSupplier(data) {
  return request({ url: '/menu-api/suppliers', method: 'post', data })
}
export function updateSupplier(id, data) {
  return request({ url: `/menu-api/suppliers/${id}`, method: 'put', data })
}
export function deleteSupplier(id) {
  return request({ url: `/menu-api/suppliers/${id}`, method: 'delete' })
}

export function getIngredients(params) {
  return request({ url: '/menu-api/ingredients', method: 'get', params })
}
export function createIngredient(data) {
  return request({ url: '/menu-api/ingredients', method: 'post', data })
}

export function uploadImage(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request({ url: '/upload/image', method: 'post', data: formData, headers: { 'Content-Type': 'multipart/form-data' } })
}
export function deleteImage(fileName) {
  return request({ url: '/upload/image', method: 'delete', data: { fileName } })
}

export function recalcAllDishes() {
  return request({ url: '/recipes/recalc-all', method: 'post' })
}

export function inventoryIn(data) {
  return request({ url: '/menu-api/inventory/in', method: 'post', data })
}
export function inventoryOut(data) {
  return request({ url: '/menu-api/inventory/out', method: 'post', data })
}
export function getInventory(params) {
  return request({ url: '/menu-api/inventory/list', method: 'get', params })
}
export function getInventoryLogs(params) {
  return request({ url: '/menu-api/inventory/logs', method: 'get', params })
}
export function getInventoryWarnings() {
  return request({ url: '/menu-api/inventory/warnings', method: 'get' })
}

export function getPurchaseRecords(params) {
  return request({ url: '/menu-api/purchases', method: 'get', params })
}
export function getPurchaseRecord(id) {
  return request({ url: `/menu-api/purchases/${id}`, method: 'get' })
}
export function createPurchase(data) {
  return request({ url: '/menu-api/purchases', method: 'post', data })
}
export function updatePurchase(id, data) {
  return request({ url: `/menu-api/purchases/${id}`, method: 'put', data })
}
export function deletePurchase(id) {
  return request({ url: `/menu-api/purchases/${id}`, method: 'delete' })
}
export function auditPurchase(id) {
  return request({ url: `/menu-api/purchases/${id}/audit`, method: 'post' })
}

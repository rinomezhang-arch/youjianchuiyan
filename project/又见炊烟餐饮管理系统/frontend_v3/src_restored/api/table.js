import axios from './http'

export function getTables(params) {
  return axios.get('/api/tables', { params })
}

export function addTable(data) {
  return axios.post('/api/tables', data)
}

export function updateTable(id, data) {
  return axios.put(`/api/tables/${id}`, data)
}

export function deleteTable(id) {
  return axios.delete(`/api/tables/${id}`)
}

export function reorderTables(orderList) {
  return axios.post('/api/tables/reorder', orderList)
}

export function swapTableBooking(data) {
  return axios.post('/api/tables/swap-booking', data)
}

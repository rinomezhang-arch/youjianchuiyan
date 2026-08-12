import request from '@/utils/request'

export function getStaffList(params) {
  return request({ url: '/hr/staff', method: 'get', params })
}
export function createStaff(data) {
  return request({ url: '/hr/staff', method: 'post', data })
}
export function updateStaff(id, data) {
  return request({ url: `/hr/staff/${id}`, method: 'put', data })
}
export function deleteStaff(id) {
  return request({ url: `/hr/staff/${id}`, method: 'delete' })
}

export function getDepartments(params) {
  return request({ url: '/hr/departments', method: 'get', params })
}

export function getAttendanceList(params) {
  return request({ url: '/hr/attendance', method: 'get', params })
}
export function createAttendance(data) {
  return request({ url: '/hr/attendance', method: 'post', data })
}

export function getLeaveList(params) {
  return request({ url: '/hr/leave', method: 'get', params })
}
export function createLeave(data) {
  return request({ url: '/hr/leave', method: 'post', data })
}

export function getScheduleList(params) {
  return request({ url: '/hr/schedule', method: 'get', params })
}
export function createSchedule(data) {
  return request({ url: '/hr/schedule', method: 'post', data })
}

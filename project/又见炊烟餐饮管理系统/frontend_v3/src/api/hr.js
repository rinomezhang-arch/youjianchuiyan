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

// ===== 数据字典 =====
export function getDictItems(dictCode) {
  return request({ url: '/hr/dict/items', method: 'get', params: { dictCode } })
}
export function getAllDicts(storeId = 1) {
  return request({ url: '/hr/dict/all', method: 'get', params: { storeId } })
}

// ===== 员工入职 =====
export function onboardStaff(data) {
  return request({ url: '/hr/staff/onboard', method: 'post', data })
}

// ===== 考勤月记录（录入/标记）=====
/** 加载某员工某月考勤记录 */
export function loadAttendanceRecord(empId, month) {
  return request({ url: '/hr/attendance/record', method: 'get', params: { empId, month } })
}

/** 保存/更新某员工某月考勤记录 */
export function saveAttendanceRecord(data) {
  return request({ url: '/hr/attendance/record', method: 'post', data })
}

/** 获取某月全员考勤汇总 */
export function getAttendanceSummary(month) {
  return request({ url: '/hr/attendance/summary', method: 'get', params: { month } })
}

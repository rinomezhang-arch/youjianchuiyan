import request from '@/utils/request'

// ===== 员工 =====
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
export function getStaffStats() {
  return request({ url: '/hr/staff/stats', method: 'get' })
}

// ===== 部门 =====
export function getDepartments(params) {
  return request({ url: '/hr/departments', method: 'get', params })
}
export function createDepartment(data) {
  return request({ url: '/hr/departments', method: 'post', data })
}
export function updateDepartment(id, data) {
  return request({ url: `/hr/departments/${id}`, method: 'put', data })
}
export function deleteDepartment(id) {
  return request({ url: `/hr/departments/${id}`, method: 'delete' })
}

// ===== 考勤（列表型 CRUD）=====
export function getAttendanceList(params) {
  return request({ url: '/hr/attendance', method: 'get', params })
}
export function createAttendance(data) {
  return request({ url: '/hr/attendance', method: 'post', data })
}
export function updateAttendance(id, data) {
  return request({ url: `/hr/attendance/${id}`, method: 'put', data })
}
export function deleteAttendance(id) {
  return request({ url: `/hr/attendance/${id}`, method: 'delete' })
}

// ===== 考勤月记录（录入/标记日历）=====
export function loadAttendanceRecord(empId, month) {
  return request({ url: '/hr/attendance/record', method: 'get', params: { empId, month } })
}
export function saveAttendanceRecord(data) {
  return request({ url: '/hr/attendance/record', method: 'post', data })
}
export function getAttendanceSummary(month) {
  return request({ url: '/hr/attendance/summary', method: 'get', params: { month } })
}

// ===== 请假 =====
export function getLeaveList(params) {
  return request({ url: '/hr/leave', method: 'get', params })
}
export function createLeave(data) {
  return request({ url: '/hr/leave', method: 'post', data })
}
export function updateLeave(id, data) {
  return request({ url: `/hr/leave/${id}`, method: 'put', data })
}
export function deleteLeave(id) {
  return request({ url: `/hr/leave/${id}`, method: 'delete' })
}
export function approveLeave(id, data) {
  return request({ url: `/hr/leave/${id}/approve`, method: 'put', data })
}

// ===== 排班 =====
export function getScheduleList(params) {
  return request({ url: '/hr/schedule', method: 'get', params })
}
export function createSchedule(data) {
  return request({ url: '/hr/schedule', method: 'post', data })
}
export function updateSchedule(id, data) {
  return request({ url: `/hr/schedule/${id}`, method: 'put', data })
}
export function deleteSchedule(id) {
  return request({ url: `/hr/schedule/${id}`, method: 'delete' })
}

// ===== 加班 =====
export function getOvertimeList(params) {
  return request({ url: '/hr/overtime', method: 'get', params })
}
export function searchOvertime(params) {
  return request({ url: '/hr/overtime/search', method: 'get', params })
}
export function createOvertime(data) {
  return request({ url: '/hr/overtime', method: 'post', data })
}
export function updateOvertime(id, data) {
  return request({ url: `/hr/overtime/${id}`, method: 'put', data })
}
export function deleteOvertime(id) {
  return request({ url: `/hr/overtime/${id}`, method: 'delete' })
}
export function approveOvertime(id, data) {
  return request({ url: `/hr/overtime/${id}/approve`, method: 'put', data })
}

// ===== 工资 =====
export function getSalaryList(params) {
  return request({ url: '/hr/salary', method: 'get', params })
}
export function createSalary(data) {
  return request({ url: '/hr/salary', method: 'post', data })
}
export function updateSalary(id, data) {
  return request({ url: `/hr/salary/${id}`, method: 'put', data })
}
export function deleteSalary(id) {
  return request({ url: `/hr/salary/${id}`, method: 'delete' })
}

// ===== 工资表（Payroll + 解锁）=====
export function getPayroll(params) {
  return request({ url: '/hr/payroll', method: 'get', params })
}
export function unlockPayroll(data) {
  return request({ url: '/hr/payroll/unlock', method: 'post', data })
}
export function lockPayroll(data) {
  return request({ url: '/hr/payroll/lock', method: 'post', data })
}

// ===== 工资扣款配置 =====
export function getSalaryDeductList(params) {
  return request({ url: '/hr/salary-deduct', method: 'get', params })
}
export function createSalaryDeduct(data) {
  return request({ url: '/hr/salary-deduct', method: 'post', data })
}
export function updateSalaryDeduct(id, data) {
  return request({ url: `/hr/salary-deduct/${id}`, method: 'put', data })
}
export function deleteSalaryDeduct(id) {
  return request({ url: `/hr/salary-deduct/${id}`, method: 'delete' })
}

// ===== 社保 =====
export function getInsuranceList(params) {
  return request({ url: '/hr/insurance', method: 'get', params })
}
export function getInsuranceByStaffId(staffId) {
  return request({ url: `/hr/insurance/staff/${staffId}`, method: 'get' })
}
export function createInsurance(data) {
  return request({ url: '/hr/insurance', method: 'post', data })
}
export function updateInsurance(id, data) {
  return request({ url: `/hr/insurance/${id}`, method: 'put', data })
}
export function deleteInsurance(id) {
  return request({ url: `/hr/insurance/${id}`, method: 'delete' })
}

// ===== 生命周期 =====
export function getLifecycle(params) {
  return request({ url: '/hr/lifecycle', method: 'get', params })
}

// ===== 总览统计 =====
export function getHrOverviewStats(params) {
  return request({ url: '/hr/stats/overview', method: 'get', params })
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


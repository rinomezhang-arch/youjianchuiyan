import request from '@/utils/request'

export function getOperationsReport(params) {
  return request.get('/report/operations', { params })
}

export function getReportOverview(params) {
  return request.get('/report/overview', { params })
}

export function getProfitReport(params) {
  return request.get('/finance/profit-report', { params })
}

export function getBalanceReport(params) {
  return request.get('/finance/balance-report', { params })
}

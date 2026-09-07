import { dateRange, localDate, normalizeReport, money } from './reportExport.js'

export const SOURCE_NOTE = '按预订日期汇总预订金额，非银行实收。'
// 临时口径说明；经营报表仍需接入权威金额与退款数据链，不能以改名替代完成。
export const STATUS_NOTE = '按全部预订状态汇总，取消及退款未扣减；不用于实收对账。'
export const REPORT_TYPES = [
  { id: 'daily', name: '日报表', desc: '所选日期' },
  { id: 'weekly', name: '周报表', desc: '所选日期所在周 · 周一至周日' },
  { id: 'monthly', name: '月报表', desc: '所选日期所在自然月' },
  { id: 'custom', name: '自定义报表', desc: '指定起止日期' }
]

export function resolveBookingRange(config) {
  if (!REPORT_TYPES.some(type => type.id === config.type)) throw new Error('报表类型无效')
  if (config.type === 'custom') return dateRange([config.startDate, config.endDate])
  const anchor = localDate(config.anchor)
  const [year, month, day] = anchor.split('-').map(Number)
  const start = new Date(year, month - 1, day)
  const end = new Date(start)
  if (config.type === 'weekly') {
    start.setDate(start.getDate() - (start.getDay() + 6) % 7)
    end.setTime(start.getTime())
    end.setDate(end.getDate() + 6)
  } else if (config.type === 'monthly') {
    start.setDate(1)
    end.setMonth(end.getMonth() + 1, 0)
  }
  return dateRange([start, end])
}

function selectionKey(config) {
  return JSON.stringify([config.type, resolveBookingRange(config)])
}

export function createBookingReportLoader(state, request) {
  let sequence = 0
  function invalidate() {
    sequence++
    Object.assign(state, { report: null, loading: false, error: '', key: '' })
  }
  async function load(config) {
    invalidate()
    const current = sequence
    let range, key
    try { range = resolveBookingRange(config); key = selectionKey(config) }
    catch (error) { state.error = error.message; return }
    state.loading = true
    try {
      // All UI periods resolve to explicit dates; the existing API supports custom.
      const response = await request({ period: 'custom', ...range })
      if (sequence !== current) return
      state.report = normalizeReport(response.data, range)
      state.key = key
    } catch (error) {
      if (sequence === current) state.error = error.message || '加载报表失败，请重试'
    } finally {
      if (sequence === current) state.loading = false
    }
  }
  return { load, invalidate }
}

export function canUseBookingReport(state, config) {
  try {
    return !state.loading && !state.error && !!state.report?.rows.length && state.key === selectionKey(config)
  } catch { return false }
}

function checkedReport(report) {
  if (!report?.rows?.length) throw new Error('选定期间无数据，无法打印或导出')
  return normalizeReport({ ...report, dailyTrend: report.rows.map(row => ({ date: row.date, count: row.bookingCount, revenue: row.revenue })) },
    dateRange([report.startDate, report.endDate]))
}

export function escapeHtml(value) {
  return String(value).replace(/[&<>"']/g, char => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[char])
}

export function bookingPrintHtml(report, title) {
  const data = checkedReport(report)
  const rows = data.rows.map(row => `<tr><td>${escapeHtml(row.date)}</td><td>${row.bookingCount}</td><td>${row.revenue.toFixed(2)}</td><td>${row.averagePerBooking.toFixed(2)}</td></tr>`).join('')
  const average = data.totalBookings ? money(data.totalRevenue / data.totalBookings) : 0
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><title>${escapeHtml(title)}</title>
<style>@page{size:A4;margin:16mm}body{font:14px sans-serif;color:#1a2f23}h1{font-size:22px}p{line-height:1.6}table{border-collapse:collapse;width:100%}th,td{border-bottom:1px solid #ccd5ce;padding:9px;text-align:right}th:first-child,td:first-child{text-align:left}thead{display:table-header-group}tr{break-inside:avoid}.note{font-size:12px;color:#555}</style></head><body>
<main><h1>${escapeHtml(title)} · 预订金额报表</h1><p>${escapeHtml(data.startDate)} 至 ${escapeHtml(data.endDate)}</p>
<p class="note">${escapeHtml(SOURCE_NOTE)}<br>${escapeHtml(STATUS_NOTE)}</p>
<p>预订数：${data.totalBookings} · 预订人数：${data.totalGuests} · 预订金额：¥${data.totalRevenue.toFixed(2)} · 单均预订金额：¥${average.toFixed(2)}</p>
<table><thead><tr><th>预订日期</th><th>订单数</th><th>预订金额（元）</th><th>单均预订金额（元）</th></tr></thead><tbody>${rows}</tbody></table></main></body></html>`
}

export async function bookingReportWorkbook(report, ExcelJS) {
  const data = checkedReport(report)
  ExcelJS ||= (await import('exceljs')).default
  const workbook = new ExcelJS.Workbook()
  const sheet = workbook.addWorksheet('预订金额报表')
  sheet.addRow(['预订金额报表'])
  sheet.addRow(['选定期间', data.startDate, '至', data.endDate])
  sheet.addRow([SOURCE_NOTE])
  sheet.addRow([STATUS_NOTE])
  sheet.mergeCells('A3:D3'); sheet.mergeCells('A4:D4')
  sheet.getRow(3).alignment = { wrapText: true }; sheet.getRow(4).alignment = { wrapText: true }
  sheet.getRow(4).height = 32
  sheet.addRow(['预订日期', '订单数', '预订金额（元）', '单均预订金额（元）'])
  data.rows.forEach(row => sheet.addRow([row.date, row.bookingCount, row.revenue, row.averagePerBooking]))
  sheet.addRow(['选定期间合计', data.totalBookings, data.totalRevenue, data.totalBookings ? money(data.totalRevenue / data.totalBookings) : 0])
  for (let row = 6; row <= sheet.rowCount; row++) {
    sheet.getCell(row, 2).numFmt = '0'
    sheet.getCell(row, 3).numFmt = '0.00'; sheet.getCell(row, 4).numFmt = '0.00'
  }
  sheet.addRow(['预订人数', data.totalGuests])
  sheet.columns.forEach(column => { column.width = 25 })
  sheet.getRow(5).font = { bold: true }
  sheet.views = [{ state: 'frozen', ySplit: 5 }]
  return { buffer: await workbook.xlsx.writeBuffer(), filename: `预订金额报表_${data.startDate}_${data.endDate}.xlsx` }
}

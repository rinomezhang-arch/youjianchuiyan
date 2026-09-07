export function localDate(value) {
  if (value instanceof Date && Number.isFinite(value.getTime())) {
    return `${value.getFullYear()}-${String(value.getMonth() + 1).padStart(2, '0')}-${String(value.getDate()).padStart(2, '0')}`
  }
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value)) throw new Error('请选择有效日期')
  const [y, m, d] = value.split('-').map(Number)
  const date = new Date(y, m - 1, d)
  if (date.getFullYear() !== y || date.getMonth() !== m - 1 || date.getDate() !== d) throw new Error('日期无效')
  return value
}

export function dateRange(value) {
  if (!Array.isArray(value) || value.length !== 2) throw new Error('请选择日期范围')
  const [startDate, endDate] = value.map(localDate)
  if (startDate > endDate) throw new Error('开始日期不能晚于结束日期')
  return { startDate, endDate }
}

function numeric(value, integer = false) {
  if (!['string', 'number'].includes(typeof value) || String(value).trim() === '') throw new Error('报表数值无效')
  const n = Number(value)
  if (!Number.isFinite(n) || (integer && (!Number.isSafeInteger(n) || n < 0))) throw new Error('报表数值无效')
  if (!Number.isSafeInteger(Math.round(n * 100))) throw new Error('报表金额超出精确范围')
  return n
}

export function money(value) {
  const n = numeric(value)
  return Math.sign(n) * Math.round((Math.abs(n) + Number.EPSILON) * 100) / 100
}

export function normalizeReport(data, range) {
  if (!data || data.startDate !== range.startDate || data.endDate !== range.endDate || !Array.isArray(data.dailyTrend)) {
    throw new Error('报表返回范围或格式不一致，请重新加载')
  }
  const dates = new Set()
  const rows = data.dailyTrend.map(day => {
    const date = localDate(day.date)
    if (date < range.startDate || date > range.endDate || dates.has(date)) throw new Error('报表日期越界或重复')
    dates.add(date)
    const bookingCount = numeric(day.count, true)
    const revenue = money(day.revenue)
    return { date, bookingCount, revenue, averagePerBooking: bookingCount ? money(revenue / bookingCount) : 0 }
  }).sort((a, b) => a.date.localeCompare(b.date))
  return { ...range, rows, totalRevenue: money(data.totalRevenue), totalBookings: numeric(data.totalBookings, true), totalGuests: numeric(data.totalGuests, true) }
}

// The same request gate is used by the page and the Node race tests.
export function createReportLoader(state, request) {
  let version = 0
  return async function load(value) {
    const current = ++version
    state.loading = false
    state.report = null
    state.error = ''
    let range
    try { range = dateRange(value) } catch (error) { state.error = error.message; return }
    state.loading = true
    try {
      const response = await request({ period: 'custom', ...range })
      if (current === version) state.report = normalizeReport(response.data, range)
    } catch (error) {
      if (current === version) state.error = error.message || '加载报表数据失败'
    } finally {
      if (current === version) state.loading = false
    }
  }
}

export function canExportReport(state, value) {
  try {
    const range = dateRange(value)
    return !state.loading && !state.error && !!state.report?.rows.length &&
      state.report.startDate === range.startDate && state.report.endDate === range.endDate
  } catch { return false }
}

export async function buildReportWorkbook(report, ExcelJS) {
  if (!report?.rows?.length) throw new Error('选定期间暂无数据，无法导出')
  const range = dateRange([report.startDate, report.endDate])
  // Revalidate at the export boundary. Only typed dates/numbers enter cells;
  // untrusted strings or Excel formula objects cannot become formulas.
  const checked = normalizeReport({ ...report, dailyTrend: report.rows.map(row => ({ date: row.date, count: row.bookingCount, revenue: row.revenue })) }, range)
  ExcelJS ||= (await import('exceljs')).default
  const workbook = new ExcelJS.Workbook()
  const sheet = workbook.addWorksheet('营收报表')
  sheet.addRow(['选定期间', range.startDate, '至', range.endDate])
  sheet.addRow(['日期', '订单数', '营收（元）', '单均金额（元）'])
  checked.rows.forEach(row => sheet.addRow([row.date, row.bookingCount, row.revenue, row.averagePerBooking]))
  sheet.columns.forEach((column, index) => { column.width = index === 0 ? 18 : 22 })
  sheet.getRow(2).font = { bold: true }
  for (let row = 3; row <= sheet.rowCount; row++) {
    sheet.getCell(row, 2).numFmt = '0'
    sheet.getCell(row, 3).numFmt = '0.00'
    sheet.getCell(row, 4).numFmt = '0.00'
  }
  sheet.views = [{ state: 'frozen', ySplit: 2 }]
  return { buffer: await workbook.xlsx.writeBuffer(), filename: `营收报表_${range.startDate}_${range.endDate}.xlsx` }
}

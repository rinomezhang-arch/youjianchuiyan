import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { normalizeReport } from '../frontend_v3/src/utils/reportExport.js'
import { SOURCE_NOTE, STATUS_NOTE, resolveBookingRange, createBookingReportLoader, canUseBookingReport,
  escapeHtml, bookingPrintHtml, bookingReportWorkbook } from '../frontend_v3/src/utils/bookingReportPrint.js'

let requireDependency
for (const path of ['../frontend_v3/package.json', '../../../../frontend_v3/package.json']) {
  const candidate = createRequire(new URL(path, import.meta.url))
  try { candidate.resolve('exceljs'); requireDependency = candidate; break } catch {}
}
if (!requireDependency) throw new Error('请使用已有前端 ExcelJS 依赖环境运行；不自动安装')
const ExcelJS = requireDependency('exceljs')
const config = { type: 'custom', startDate: '2026-09-01', endDate: '2026-09-07' }
const range = resolveBookingRange(config)
const dto = (r = range) => ({ ...r, totalBookings: 5, totalGuests: 12, totalRevenue: '99.98',
  dailyTrend: [{ date: r.startDate, count: 3, revenue: '100.01' }, { date: r.endDate, count: 2, revenue: '-0.03' }] })

test('local daily/weekly/monthly/custom ranges: Sunday, year boundary, leap month and invalid input', () => {
  assert.deepEqual(resolveBookingRange({ type: 'daily', anchor: new Date(2026, 8, 7, 0, 5) }), { startDate: '2026-09-07', endDate: '2026-09-07' })
  assert.deepEqual(resolveBookingRange({ type: 'weekly', anchor: '2026-09-06' }), { startDate: '2026-08-31', endDate: '2026-09-06' })
  assert.deepEqual(resolveBookingRange({ type: 'weekly', anchor: '2026-01-01' }), { startDate: '2025-12-29', endDate: '2026-01-04' })
  assert.deepEqual(resolveBookingRange({ type: 'monthly', anchor: '2024-02-29' }), { startDate: '2024-02-01', endDate: '2024-02-29' })
  assert.deepEqual(resolveBookingRange({ type: 'monthly', anchor: '2026-12-31' }), { startDate: '2026-12-01', endDate: '2026-12-31' })
  assert.deepEqual(range, { startDate: config.startDate, endDate: config.endDate })
  for (const invalid of [{ type: 'daily', anchor: '' }, { type: 'weekly', anchor: '2026-02-30' },
    { ...config, startDate: '2026-09-08' }, { ...config, endDate: '' }, { type: 'unknown', anchor: '2026-09-07' }]) {
    assert.throws(() => resolveBookingRange(invalid))
  }
})

test('all UI periods send only supported custom/startDate/endDate params to existing report API', async () => {
  for (const choice of [config, ...['daily', 'weekly', 'monthly'].map(type => ({ type, anchor: '2026-09-07' }))]) {
    const state = {}
    const expected = resolveBookingRange(choice)
    const loader = createBookingReportLoader(state, async params => {
      assert.deepEqual(params, { period: 'custom', ...expected })
      return { data: { ...expected, totalBookings: 3, totalGuests: 8, totalRevenue: '100.01', dailyTrend: [{ date: expected.startDate, count: 3, revenue: '100.01' }] } }
    })
    await loader.load({ ...choice, staff: 'ignored', zone: 'ignored', amountMin: 999 })
    assert.equal(canUseBookingReport(state, choice), true)
    assert.equal(state.report.totalRevenue, 100.01)
  }
  const source = readFileSync(new URL('../frontend_v3/src/api/booking.js', import.meta.url), 'utf8')
  assert.match(source, /function getDashboardReport\(params\)\s*{\s*return request\(\{ url: '\/dashboard\/report', method: 'get', params \}\)/)
})

test('real XLSX round trip: dates, numeric amounts, two decimals, truthful labels and no formulas', async () => {
  assert.equal(STATUS_NOTE, '按全部预订状态汇总，取消及退款未扣减；不用于实收对账。')
  const { buffer, filename } = await bookingReportWorkbook(normalizeReport(dto(), range), ExcelJS)
  const workbook = new ExcelJS.Workbook(); await workbook.xlsx.load(buffer)
  assert.equal(filename, '预订金额报表_2026-09-01_2026-09-07.xlsx')
  const sheet = workbook.getWorksheet('预订金额报表')
  assert.deepEqual(sheet.getRow(2).values.slice(1), ['选定期间', '2026-09-01', '至', '2026-09-07'])
  assert.equal(sheet.getCell('A3').value, SOURCE_NOTE)
  assert.equal(sheet.getCell('A4').value, STATUS_NOTE)
  assert.deepEqual(sheet.getRow(5).values.slice(1), ['预订日期', '订单数', '预订金额（元）', '单均预订金额（元）'])
  assert.deepEqual(sheet.getRow(6).values.slice(1), ['2026-09-01', 3, 100.01, 33.34])
  assert.deepEqual(sheet.getRow(7).values.slice(1), ['2026-09-07', 2, -0.03, -0.02])
  assert.deepEqual(sheet.getRow(8).values.slice(1), ['选定期间合计', 5, 99.98, 20])
  assert.equal(sheet.getCell('B9').value, 12)
  for (const address of ['C6', 'D6', 'C7', 'D7', 'C8', 'D8']) {
    assert.equal(sheet.getCell(address).type, ExcelJS.ValueType.Number)
    assert.equal(sheet.getCell(address).numFmt, '0.00')
  }
  sheet.eachRow(row => row.eachCell(cell => assert.notEqual(cell.type, ExcelJS.ValueType.Formula)))
})

test('print HTML contains report only, escaped title, truthful notes, validated rows; injected cells rejected', async () => {
  const report = normalizeReport(dto(), range)
  const html = bookingPrintHtml(report, '<img src=x onerror="alert(1)"> & \'title\'')
  assert.match(html, /&lt;img src=x onerror=&quot;alert\(1\)&quot;&gt; &amp; &#39;title&#39;/)
  assert.equal(escapeHtml('<>&"\''), '&lt;&gt;&amp;&quot;&#39;')
  assert.ok(html.includes(SOURCE_NOTE)); assert.ok(html.includes(STATUS_NOTE))
  assert.doesNotMatch(html, /待后端修复/)
  assert.ok(html.includes('100.01')); assert.ok(html.includes('33.34'))
  assert.doesNotMatch(html, /<script|<img|<button|<input|<nav/i)
  assert.match(html, /<main>/)
  for (const attack of ['=1+1', '@SUM(1,2)', '<img src=x onerror=alert(1)>', { formula: '1+1' }]) {
    for (const field of ['date', 'bookingCount', 'revenue']) {
      const invalid = structuredClone(report); invalid.rows[0][field] = attack
      assert.throws(() => bookingPrintHtml(invalid, '日报表'))
      await assert.rejects(bookingReportWorkbook(invalid, ExcelJS))
    }
  }
})

test('empty/missing/mismatched results cannot be printed/exported; failures clear previous data', async () => {
  const state = {}
  let response = dto()
  const loader = createBookingReportLoader(state, async () => ({ data: response }))
  await loader.load(config); assert.equal(canUseBookingReport(state, config), true)
  for (const bad of [undefined, { ...dto(), endDate: '2026-09-08' }, { ...dto(), dailyTrend: null },
    { ...dto(), dailyTrend: [{ date: '2026-09-08', count: 1, revenue: 10 }] }]) {
    response = bad; await loader.load(config)
    assert.equal(canUseBookingReport(state, config), false); assert.equal(state.report, null)
    assert.ok(state.error)
  }
  response = { ...range, dailyTrend: [], totalBookings: 0, totalRevenue: 0, totalGuests: 0 }
  await loader.load(config)
  assert.equal(state.error, ''); assert.equal(canUseBookingReport(state, config), false)
  assert.throws(() => bookingPrintHtml(state.report, '报表'), /无数据/)
  await assert.rejects(bookingReportWorkbook(state.report, ExcelJS), /无数据/)
  await loader.load({ ...config, endDate: '' })
  assert.equal(state.report, null); assert.equal(state.loading, false)
})

test('stale success/failure and explicit invalidation never restore old output', async () => {
  const state = {}, pending = []
  const loader = createBookingReportLoader(state, params => new Promise((resolve, reject) => pending.push({ params, resolve, reject })))
  const first = loader.load(config)
  const second = loader.load(config)
  assert.equal(canUseBookingReport(state, config), false)
  pending[0].resolve({ data: dto() }); await first
  assert.equal(state.report, null); assert.equal(state.loading, true)
  pending[1].resolve({ data: dto() }); await second
  assert.equal(canUseBookingReport(state, config), true)
  assert.equal(canUseBookingReport(state, { type: 'weekly', anchor: '2026-09-07' }), false)
  const third = loader.load(config)
  loader.invalidate()
  pending[2].resolve({ data: dto() }); await third
  assert.equal(state.report, null); assert.equal(state.loading, false)
  const fourth = loader.load(config), fifth = loader.load(config)
  pending[4].resolve({ data: dto() }); await fifth
  pending[3].reject(new Error('obsolete')); await fourth
  assert.equal(canUseBookingReport(state, config), true)
  const sixth = loader.load(config)
  pending[5].reject(new Error('current failure')); await sixth
  assert.equal(state.report, null); assert.equal(state.error, 'current failure')
})

test('SFC script/template/style compile; query watcher invalidates synchronously; iframe prints report only', () => {
  const { parse, compileScript, compileTemplate, compileStyle } = requireDependency('@vue/compiler-sfc')
  const filename = new URL('../frontend_v3/src/views/dashboard/ReportPrint.vue', import.meta.url)
  const source = readFileSync(filename, 'utf8')
  const { descriptor, errors } = parse(source)
  assert.deepEqual(errors, [])
  const script = compileScript(descriptor, { id: 'booking-report-print' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: filename.pathname, id: 'booking-report-print', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.deepEqual(compileStyle({ source: descriptor.styles[0].content, filename: filename.pathname, id: 'data-v-booking-report-print', scoped: true }).errors, [])
  assert.match(source, /flush: 'sync'/)
  assert.match(source, /frame\.contentWindow\.print\(\)/)
  assert.match(source, /frame\.srcdoc = html/)
  assert.match(source, /state\.report !== snapshot/)
  assert.doesNotMatch(source, /window\.print\(|v-html|staffList|reportBookings|revenueBreakdown|toISOString/)
  assert.match(source, /另存为 PDF/)
})

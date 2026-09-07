import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import { test } from 'node:test'
import { localDate, dateRange, normalizeReport, createReportLoader, canExportReport, buildReportWorkbook } from '../frontend_v3/src/utils/reportExport.js'

// Reuse installed dependencies, including the parent's shared installation.
let requireDependency
for (const path of ['../frontend_v3/package.json', '../../../../frontend_v3/package.json']) {
  const candidate = createRequire(new URL(path, import.meta.url))
  try { candidate.resolve('exceljs'); requireDependency = candidate; break } catch {}
}
if (!requireDependency) throw new Error('现有 ExcelJS 不可用；请使用已有前端依赖环境运行')
const ExcelJS = requireDependency('exceljs')
const range = { startDate: '2026-09-01', endDate: '2026-09-07' }
const selection = Object.values(range)
const dto = (r = range) => ({ ...r, totalRevenue: '100.01', totalBookings: 3, totalGuests: 8,
  dailyTrend: [{ date: r.startDate, count: 3, revenue: '100.01' }] })

test('XLSX actual round trip: selected range, date, numeric cells, cents and per-order average', async () => {
  const report = normalizeReport(dto(), range)
  const { buffer, filename } = await buildReportWorkbook(report, ExcelJS)
  const read = new ExcelJS.Workbook()
  await read.xlsx.load(buffer)
  const sheet = read.getWorksheet('营收报表')
  assert.equal(filename, '营收报表_2026-09-01_2026-09-07.xlsx')
  assert.deepEqual(sheet.getRow(1).values.slice(1), ['选定期间', ...[range.startDate, '至', range.endDate]])
  assert.deepEqual(sheet.getRow(2).values.slice(1), ['日期', '订单数', '营收（元）', '单均金额（元）'])
  assert.deepEqual(sheet.getRow(3).values.slice(1), ['2026-09-01', 3, 100.01, 33.34])
  for (const address of ['B3', 'C3', 'D3']) assert.equal(sheet.getCell(address).type, ExcelJS.ValueType.Number)
  assert.equal(sheet.getCell('C3').numFmt, '0.00')
  assert.equal(sheet.getCell('D3').numFmt, '0.00')
  sheet.eachRow(row => row.eachCell(cell => assert.notEqual(cell.type, ExcelJS.ValueType.Formula)))
})

test('local calendar dates, invalid and cleared ranges', () => {
  assert.equal(localDate(new Date(2026, 8, 7, 0, 5)), '2026-09-07')
  assert.equal(localDate(new Date(2026, 8, 7, 23, 59)), '2026-09-07')
  for (const value of [null, [], ['2026-09-07', '2026-09-01'], ['2026-02-30', '2026-03-01'], [new Date(NaN), new Date()]]) {
    assert.throws(() => dateRange(value))
  }
})

test('formula injection, invalid numbers, mismatched ranges and empty data are rejected', async () => {
  for (const attack of ['=1+1', '+SUM(1,2)', '-1+2', '@SUM(1,2)', { formula: '1+1' }]) {
    for (const field of ['date', 'count', 'revenue']) {
      const source = dto()
      source.dailyTrend[0][field] = attack
      assert.throws(() => normalizeReport(source, range))
    }
  }
  for (const value of [null, '', Infinity, NaN, {}, 1e20]) {
    const source = dto(); source.dailyTrend[0].revenue = value
    assert.throws(() => normalizeReport(source, range))
  }
  assert.throws(() => normalizeReport({ ...dto(), startDate: '2026-08-01' }, range))
  assert.throws(() => normalizeReport({ ...dto(), dailyTrend: [{ date: '2026-09-08', count: 1, revenue: 1 }] }, range))
  const empty = normalizeReport({ ...dto(), dailyTrend: [], totalRevenue: 0, totalBookings: 0, totalGuests: 0 }, range)
  assert.equal(canExportReport({ report: empty }, selection), false)
  await assert.rejects(buildReportWorkbook(empty, ExcelJS), /暂无数据/)
  const tampered = normalizeReport(dto(), range)
  tampered.rows[0].revenue = { formula: '1+1' }
  await assert.rejects(buildReportWorkbook(tampered, ExcelJS))
})

test('zero orders and negative fractional revenue round trip as numbers', async () => {
  const source = dto()
  source.dailyTrend = [{ date: range.startDate, count: 0, revenue: 0 }, { date: range.endDate, count: 2, revenue: '-0.03' }]
  const { buffer } = await buildReportWorkbook(normalizeReport(source, range), ExcelJS)
  const read = new ExcelJS.Workbook(); await read.xlsx.load(buffer)
  assert.equal(read.worksheets[0].getCell('D3').value, 0)
  assert.equal(read.worksheets[0].getCell('C4').value, -0.03)
  assert.equal(read.worksheets[0].getCell('D4').value, -0.02)
})

test('request gate: loading, old success/failure, clear, current failure and recovery', async () => {
  const pending = []
  const state = { report: null, loading: false, error: '' }
  const load = createReportLoader(state, params => new Promise((resolve, reject) => pending.push({ params, resolve, reject })))
  const first = load(selection)
  assert.equal(canExportReport(state, selection), false)
  const otherRange = { startDate: '2026-08-01', endDate: '2026-08-31' }
  const second = load(Object.values(otherRange))
  assert.deepEqual(pending[1].params, { period: 'custom', ...otherRange })
  pending[0].resolve({ data: dto() }); await first
  assert.equal(state.report, null); assert.equal(state.loading, true)
  pending[1].resolve({ data: dto(otherRange) }); await second
  assert.equal(canExportReport(state, Object.values(otherRange)), true)
  assert.equal(canExportReport(state, selection), false)
  const third = load(selection)
  await load(null)
  pending[2].resolve({ data: dto() }); await third
  assert.equal(state.report, null); assert.equal(state.loading, false)
  const fourth = load(selection)
  const fifth = load(selection)
  pending[4].resolve({ data: dto() }); await fifth
  pending[3].reject(new Error('old failure')); await fourth
  assert.equal(canExportReport(state, selection), true)
  const sixth = load(selection)
  pending[5].reject(new Error('current failure')); await sixth
  assert.equal(state.report, null); assert.equal(canExportReport(state, selection), false)
  const seventh = load(selection)
  pending[6].resolve({ data: dto() }); await seventh
  assert.equal(canExportReport(state, selection), true)
})

test('Reports.vue SFC parses and compiles (not browser E2E)', () => {
  const { parse, compileScript, compileTemplate } = requireDependency('@vue/compiler-sfc')
  const filename = new URL('../frontend_v3/src/views/dashboard/Reports.vue', import.meta.url)
  const { descriptor, errors } = parse(readFileSync(filename, 'utf8'))
  assert.deepEqual(errors, [])
  compileScript(descriptor, { id: 'reports-test' })
  assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: filename.pathname, id: 'reports-test' }).errors, [])
})

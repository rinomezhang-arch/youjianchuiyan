import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { parse, compileScript, compileTemplate } from '@vue/compiler-sfc'
import { stockTakePrintHtml, printStockTake } from '../src/utils/stockTakePrint.js'

const sample = () => ({
  stockTake: { takeId: 41, storeId: 2, takeNo: 'ST-TEST', takeDate: '2026-09-07', totalItems: 2,
    totalDiffItems: 1, totalDiffAmount: '-1.25', status: 'completed', operatorName: '<img src=x onerror=alert(1)>', remark: 'A&B' },
  details: [
    { lineNo: 1, takeId: 41, storeId: 2, ingredientId: 'I1', ingredientName: '米<script>x</script>', unit: 'kg', systemQuantity: 2, actualQuantity: 1.875, diffQuantity: -0.125, diffAmount: -1.25 },
    { lineNo: 2, takeId: 41, storeId: 2, ingredientId: 'I2', ingredientName: '油', unit: 'kg', systemQuantity: 3, actualQuantity: 3, diffQuantity: 0, diffAmount: 0 }
  ]
})
let passed = 0
function check(label, run) { run(); passed++; console.log(`PASS ${label}`) }
check('saved snapshot, precision, escape and unchanged inputs', () => {
  const data = sample(), before = structuredClone(data), html = stockTakePrintHtml(data, 41, 2)
  assert.match(html, /1\.875/); assert.match(html, /-0\.125/); assert.match(html, /-1\.25/)
  assert.match(html, /&lt;script&gt;/); assert.doesNotMatch(html, /<script>|<img/)
  assert.match(html, /A&amp;B/); assert.match(html, /table-header-group/); assert.match(html, /盘点人签字/)
  assert.deepEqual(data, before)
})
for (const [label, mutate] of [
  ['wrong master', data => data.stockTake.takeId = 42],
  ['wrong store', data => data.stockTake.storeId = 3],
  ['wrong detail parent', data => data.details[0].takeId = 42],
  ['wrong detail store', data => data.details[0].storeId = 3],
  ['missing saved line number', data => data.details[0].lineNo = null],
  ['duplicate saved line number', data => data.details[0].lineNo = 2],
  ['missing rows', data => data.details = []],
  ['truncated rows', data => data.details.pop()],
  ['invalid amount', data => data.details[0].diffAmount = 'bad'],
  ['nonfinite amount', data => data.stockTake.totalDiffAmount = Infinity]
]) check(label, () => { const data = sample(); mutate(data); assert.throws(() => stockTakePrintHtml(data, 41, 2)) })
check('shuffled rows print in saved sequence without changing source', () => {
  const data = sample(); data.details.reverse()
  const before = structuredClone(data), html = stockTakePrintHtml(data, 41, 2)
  assert.ok(html.indexOf('<td>I1</td>') < html.indexOf('<td>I2</td>'))
  assert.deepEqual(data, before)
})
check('missing historical value stays missing', () => {
  const data = sample(); data.details[0].diffAmount = null
  assert.match(stockTakePrintHtml(data, 41, 2), /<td>—<\/td>/)
})
check('iframe uses loaded snapshot and releases on afterprint', () => {
  let after, prints = 0, removed = false, appended = false
  const frame = { style: {}, remove() { removed = true }, contentWindow: {
    addEventListener(event, callback) { assert.equal(event, 'afterprint'); after = callback },
    focus() {}, print() { prints++ }
  } }
  printStockTake('<html>snapshot</html>', { createElement: () => frame, body: { appendChild() { appended = true } } })
  assert.equal(prints, 0); assert.equal(appended, true); assert.equal(frame.srcdoc, '<html>snapshot</html>')
  frame.onload(); assert.equal(prints, 1); after(); assert.equal(removed, true)
})
check('browser printing failure reports and removes frame', () => {
  let errors = 0, removed = false
  const frame = { style: {}, remove() { removed = true }, contentWindow: {
    addEventListener() {}, focus() {}, print() { throw new Error('unavailable') }
  } }
  printStockTake('html', { createElement: () => frame, body: { appendChild() {} } }, () => errors++)
  frame.onload(); assert.equal(errors, 1); assert.equal(removed, true)
})
check('actual SFC compiles with button in detail dialog', () => {
  const source = readFileSync(new URL('../src/views/dashboard/StockTake.vue', import.meta.url), 'utf8')
  const { descriptor, errors } = parse(source); assert.equal(errors.length, 0)
  const script = compileScript(descriptor, { id: 'stocktake-print' })
  const template = compileTemplate({ source: descriptor.template.content, filename: 'StockTake.vue', id: 'stocktake-print', compilerOptions: { bindingMetadata: script.bindings } })
  assert.equal(template.errors.length, 0)
  assert.match(source, /v-model="showDetails"[\s\S]*?#footer[\s\S]*?printDetails/)
})
console.log(`${passed} checks passed (unit/SFC only; no physical printer assertion)`)

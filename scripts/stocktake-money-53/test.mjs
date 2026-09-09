import assert from 'node:assert/strict'
import { readFile, writeFile } from 'node:fs/promises'
import { resolve, dirname } from 'node:path'
import { fileURLToPath, pathToFileURL } from 'node:url'
import { createRequire } from 'node:module'
import { createHash } from 'node:crypto'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '../..')
const vuePath = resolve(root, 'frontend_v3/src/views/dashboard/StockTake.vue')
const utilPath = resolve(root, 'frontend_v3/src/utils/stockTakeMoney.js')
const money = await import(pathToFileURL(utilPath))
const { calculateStockTakeLine: line, sumStockTakeCents: sum, formatStockTakeMoney: format } = money
// Use existing installed Vue only; no package installation or Vite build.
const vueRequire = createRequire(resolve(process.env.CO53_VUE_ROOT || resolve(root, '../../..'), 'frontend_v3/package.json'))
const { ref, computed, reactive } = vueRequire('vue')
const { parse, compileScript, compileTemplate } = vueRequire('@vue/compiler-sfc')
const source = await readFile(vuePath, 'utf8')
const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*$/gm, '')
const results = []
async function test(name, fn) {
  try { await fn(); results.push({ name, passed: true }) }
  catch (error) { results.push({ name, passed: false, error: error.stack }) }
}
const row = (actualQuantity = 11, systemQuantity = 10, unitPrice = 0.015) => ({ ingredientId: 'SYN-CO53', ingredientName: 'Synthetic', actualQuantity, systemQuantity, unitPrice })
function page() {
  const messages = [], confirms = [], posts = [], blobs = []
  const user = reactive({ storeId: 1 })
  const hooks = { confirm: async () => {}, post: async () => ({ data: { takeNo: 'SYN-CO53' } }) }
  const request = {
    get: async () => ({ data: [] }),
    post: async (...args) => { posts.push(args); return hooks.post(...args) }
  }
  const ElMessage = Object.fromEntries(['warning', 'info', 'error', 'success'].map(kind => [kind, text => messages.push({ kind, text })]))
  const ElMessageBox = { confirm: async text => { confirms.push(text); return hooks.confirm(text) } }
  const context = { ref, computed, onMounted: () => {}, watch: () => {}, ElMessage, ElMessageBox, request, useUserStore: () => user,
    stockTakePrintHtml: () => '', printStockTake: () => {}, ...money, Blob,
    URL: { createObjectURL(blob) { blobs.push(blob); return 'blob:synthetic' }, revokeObjectURL() {} },
    document: { createElement() { return { click() {} } } }
  }
  const code = script + '\nreturn {list,stockTaking,submitting,loading,totalPreview,diffCount,updateDiff,startStockTake,submitStockTake,exportData,displayMoney};'
  const actual = new Function(...Object.keys(context), code)(...Object.values(context))
  return { ...actual, messages, confirms, posts, blobs, user, hooks }
}
function fill(p, rows) {
  p.list.value = rows
  p.stockTaking.value = true
  p.list.value.forEach(p.updateDiff)
}
const original = [
  { actual: 11, price: 0.015, expected: '0.02' },
  { actual: 9, price: 0.015, expected: '-0.02' },
  { actual: 11, price: 0.335, expected: '0.34' }
]
for (const [index, example] of original.entries()) {
  await test(`pure DL49 original ${index + 1}`, () => assert.equal(line(row(example.actual, 10, example.price)).diffAmount, example.expected))
  await test(`actual Vue DL49 original ${index + 1}`, () => {
    const p = page(); fill(p, [row(example.actual, 10, example.price)])
    assert.equal(p.list.value[0].diffAmount, example.expected)
    assert.equal(p.displayMoney(p.list.value[0].diffAmount), example.expected)
    assert.equal(p.totalPreview.value.text, example.expected)
  })
}
await test('three-digit quantity and eight-digit price', () => {
  const r = line(row('8.001000', '10.000', '1.12345678'))
  assert.equal(r.actualQuantity, '8.001'); assert.equal(r.diffQty, '-1.999'); assert.equal(r.diffAmount, '-2.25')
})
for (const price of [1e-8, '1e-8', '0.00000001']) {
  await test(`scientific JSON price ${String(price)}`, () => assert.equal(line(row('500000.000', 0, price)).diffAmount, '0.01'))
}
await test('scientific quantity is preserved at three places', () => assert.equal(line(row('8.001e0', '1e1', '1.5e-2')).diffQty, '-1.999'))
for (const [actual, expected] of [[1, '0.01'], [0, '-0.01']]) {
  await test(`positive negative exact half cent ${expected}`, () => assert.equal(line(row(actual, 1 - actual, '0.005')).diffAmount, expected))
}
await test('round lines first then sum differs from round aggregate', () => {
  const a = line(row(11, 10, '.015')), b = line(row(11, 10, '.015'))
  assert.equal(sum([a.diffCents, b.diffCents]).text, '0.04')
  assert.equal(sum([-a.diffCents, -b.diffCents]).text, '-0.04')
})
await test('exact integer cents avoid floating total drift', () => {
  assert.equal(sum([10n, 20n]).text, '0.30')
  assert.equal(sum([999999999999n, -999999999998n]).text, '0.01')
})
await test('zero quantity/price/difference does not become empty or negative zero', () => {
  const r = line(row('0.000', 0, 0)); assert.equal(r.diffAmount, '0.00'); assert.equal(r.diffQty, '0.000')
  assert.equal(line(row(0, 1, '0.00000001')).diffAmount, '0.00')
})
for (const value of [null, undefined, '', '   ']) {
  await test(`empty actual remains empty ${String(value)}`, async () => {
    const input = { ...row(), actualQuantity: value }
    assert.equal(line(input), null)
    const p = page(); fill(p, [input])
    assert.equal(p.list.value[0].actualQuantity, value); assert.equal(p.list.value[0].diffAmount, null)
    assert.equal(p.totalPreview.value.text, null)
    await p.submitStockTake(); assert.equal(p.posts.length, 0); assert.equal(p.confirms.length, 0)
    assert.ok(p.messages.some(m => m.text.includes('没有填写')))
  })
}
const invalid = [
  ['four decimal quantity', row('8.0001')], ['quantity overflow', row('1000000000')],
  ['negative actual', row('-0.001')], ['unknown system', row(1, null)], ['unknown price', row(1, 0, undefined)],
  ['empty system', row(1, '')], ['empty price', row(1, 0, '')], ['negative price', row(1, 0, '-1')],
  ['nine decimal price', row(1, 0, '.000000001')], ['price overflow', row(1, 0, '100000000')],
  ['NaN', row(NaN)], ['Infinity', row(Infinity)], ['boolean', row(true)], ['object', row({})],
  ['unknown text', row('unknown')], ['commas', row('1,000')], ['huge exponent', row('1e999')],
  ['difference storage overflow', row('999999999.999', '-999999999.999', 0)],
  ['unsafe numeric price', row(1, 0, 99999999.99999999)],
  ['monetary product overflow', row('999999999.999', 0, '99999999.99999999')],
  ['system amount overflow even zero difference', row('999999999.999', '999999999.999', '99999999.99999999')]
]
// Keep explicit undefined price: the row() default otherwise supplies the default price.
invalid.find(x => x[0] === 'unknown price')[1].unitPrice = undefined
for (const [name, input] of invalid) {
  await test(`pure + actual Vue reject ${name}`, async () => {
    assert.throws(() => line(input))
    const p = page(); fill(p, [input])
    assert.ok(p.list.value[0].moneyError); assert.equal(p.list.value[0].diffAmount, null)
    assert.ok(p.totalPreview.value.error)
    await p.submitStockTake(); p.exportData()
    assert.equal(p.posts.length, 0); assert.equal(p.confirms.length, 0); assert.equal(p.blobs.length, 0)
    assert.ok(p.messages.some(m => m.kind === 'warning'))
  })
}
await test('large exact string price stays precise without Number conversion', () => {
  assert.equal(line(row('1.001', '1', '99999999.99999999')).diffAmount, '100000.00')
})
await test('money formatter maintains two decimals across number/string/scientific history values', () => {
  for (const [input, text] of [[0, '0.00'], ['0.02', '0.02'], [-.02, '-0.02'], ['1e-2', '0.01'], ['9999999999.99', '9999999999.99']]) {
    assert.equal(format(input), text); assert.equal(page().displayMoney(input), text)
  }
  for (const input of [null, undefined, '', 'unknown', '1.001', '10000000000', 9007199254740992]) {
    assert.throws(() => format(input)); assert.equal(page().displayMoney(input), '金额无效')
  }
})
await test('total overflow visibly blocks confirmation and CSV', async () => {
  const p = page(); fill(p, [row('999999999.999', 0, 10), row('999999999.999', 0, 10)])
  assert.ok(p.totalPreview.value.error.includes('合计'))
  await p.submitStockTake(); p.exportData(); assert.equal(p.posts.length, 0); assert.equal(p.blobs.length, 0)
})
await test('start prefill refreshes stale amount and exact zero diff count', () => {
  const p = page(); p.list.value = [{ ...row(11), diffAmount: '0.02', diffSign: 1 }]
  p.startStockTake(); assert.equal(p.list.value[0].diffAmount, '0.00'); assert.equal(p.diffCount.value, 0)
})
await test('actual Vue display confirmation CSV and payload agree', async () => {
  const p = page(); fill(p, [row(11), { ...row(11), ingredientId: 'SYN-CO53-B' }])
  assert.equal(p.totalPreview.value.text, '0.04')
  p.exportData(); const csv = await p.blobs[0].text()
  assert.equal(csv.split('\r\n').slice(1).length, 2)
  assert.ok(csv.split('\r\n').slice(1).every(r => r.endsWith('"1.000","0.02"')))
  await p.submitStockTake()
  assert.ok(p.confirms[0].includes('差异 2 项，差异金额 ¥0.04'))
  assert.equal(p.posts.length, 1); assert.equal(p.posts[0][0], '/stock-takes')
  assert.deepEqual(p.posts[0][1].items, [{ ingredientId: 'SYN-CO53', actualQuantity: '11.000' }, { ingredientId: 'SYN-CO53-B', actualQuantity: '11.000' }])
  assert.doesNotThrow(() => JSON.stringify(p.posts[0][1]))
})
await test('CSV blank actual is not converted to zero', async () => {
  const p = page(); fill(p, [row('')]); p.exportData()
  assert.ok((await p.blobs[0].text()).split('\r\n')[1].endsWith('"","",""'))
})
await test('negative display confirmation and CSV use the same half-up cents', async () => {
  const p = page(); fill(p, [row(9), { ...row(9), ingredientId: 'SYN-CO53-B' }])
  assert.equal(p.totalPreview.value.text, '-0.04')
  assert.equal(p.displayMoney(p.list.value[0].diffAmount), '-0.02')
  p.exportData()
  assert.ok((await p.blobs[0].text()).split('\r\n').slice(1).every(r => r.endsWith('"-1.000","-0.02"')))
  await p.submitStockTake(); assert.ok(p.confirms[0].includes('¥-0.04')); assert.equal(p.posts.length, 1)
})
await test('actual Vue scientific price and precise quantity preserve display and payload', async () => {
  const p = page(); fill(p, [row('500000.000', 0, 1e-8)])
  assert.equal(p.totalPreview.value.text, '0.01'); p.exportData()
  assert.ok((await p.blobs[0].text()).split('\r\n')[1].endsWith('"500000.000","0.01"'))
  await p.submitStockTake(); assert.equal(p.posts[0][1].items[0].actualQuantity, '500000.000')
})
await test('confirmation detects edits and requires another confirmation', async () => {
  const p = page(); fill(p, [row(11)])
  p.hooks.confirm = async () => { p.list.value[0].actualQuantity = '12.000' }
  await p.submitStockTake(); assert.equal(p.posts.length, 0)
  assert.ok(p.messages.some(m => m.text.includes('数据已变化')))
})
await test('confirmation detects store switch', async () => {
  const p = page(); fill(p, [row(11)]); p.hooks.confirm = async () => { p.user.storeId = 2 }
  await p.submitStockTake(); assert.equal(p.posts.length, 0)
  assert.ok(p.messages.some(m => m.text.includes('门店已切换')))
})
await test('invalid-to-valid editing clears stale error', () => {
  const p = page(); fill(p, [row('8.0001')]); p.list.value[0].actualQuantity = '8.001'; p.updateDiff(p.list.value[0])
  assert.equal(p.list.value[0].moneyError, ''); assert.equal(p.list.value[0].diffAmount, '-0.03')
})
await test('actual SFC script and template compile without Vite', () => {
  const { descriptor, errors } = parse(source, { filename: vuePath }); assert.deepEqual(errors, [])
  const compiled = compileScript(descriptor, { id: 'co53' })
  const template = compileTemplate({ source: descriptor.template.content, filename: vuePath, id: 'co53', compilerOptions: { bindingMetadata: compiled.bindings } })
  assert.deepEqual(template.errors, [])
  assert.ok(source.includes('@input="updateDiff(row)"'))
  assert.ok(!source.includes('.toFixed('))
  assert.ok(!source.includes('<el-input-number'))
})
const report = {
  task: 'CO53', time: new Date().toISOString(), node: process.version,
  passed: results.filter(r => r.passed).length, failed: results.filter(r => !r.passed).length,
  originalDL49: { cases: 3, passed: results.filter(r => r.name.startsWith('actual Vue DL49') && r.passed).length },
  sources: Object.fromEntries(await Promise.all([vuePath, utilPath].map(async path => [path.slice(root.length + 1).replaceAll('\\', '/'), createHash('sha256').update(await readFile(path)).digest('hex')]))),
  coverage: 'Pure functions + actual unmodified script-setup functions with real Vue ref/computed, stubbed I/O; SFC compilation only',
  notRun: ['Vite full build', 'browser', 'HTTP/DB', 'Java'], results
}
const output = resolve(root, 'docs/协作/Codex/CO53', `test-result-${Date.now()}.json`)
await writeFile(output, JSON.stringify(report, null, 2) + '\n', { flag: 'wx' })
console.log(JSON.stringify({ passed: report.passed, failed: report.failed, originalDL49: report.originalDL49, output }))
for (const result of results.filter(r => !r.passed)) console.error(result.name + '\n' + result.error)
process.exitCode = report.failed ? 1 : 0

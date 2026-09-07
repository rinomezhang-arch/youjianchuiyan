import assert from 'node:assert/strict'
import { test } from 'node:test'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'
import * as payroll from '../frontend_v3/src/utils/payrollActions.js'
const { PAYROLL_FIELDS, createPayrollActions, payrollPermissions, payrollSavePayload, payrollStatusText, filterPayrollRows, payrollCsv, payrollPrintHtml } = payroll
let dependency
for (const path of ['../frontend_v3/package.json', '../../../../frontend_v3/package.json']) {
  const candidate = createRequire(new URL(path, import.meta.url))
  try { candidate.resolve('@vue/compiler-sfc'); dependency = candidate; break } catch {}
}
if (!dependency) throw new Error('已有 Vue 编译依赖不可用；不自动安装')
const row = (status = 0, id = 1) => ({ emp_id: id, emp_name: `Synthetic ${id}`, department: '测试部门',
  ...Object.fromEntries(PAYROLL_FIELDS.map(([key]) => [key, '10.00'])), deduction_tax: '0.00', gross_pay: '60.00', net_pay: '40.00', salary_status: status })
const fresh = () => ({ rows: [], month: '2026-09', loadedMonth: '', ready: false, dirty: false, busy: '', error: '', notice: '', unlocked: true })
const ok = data => ({ code: 200, data })
const loaded = (...rows) => ({ ...fresh(), rows, loadedMonth: '2026-09', ready: true })

test('save -> GET -> human approval -> GET -> payout -> GET; exact params and no forged identity/totals', async () => {
  const state = fresh(), calls = []
  let persisted = row()
  const request = {
    get: async (path, options) => { calls.push(['GET', path, options]); return ok([{ ...persisted }]) },
    post: async (path, payload, options) => {
      calls.push(['POST', path, payload, options])
      assert.deepEqual(options, { params: { month: '2026-09' } })
      if (path.endsWith('/save')) {
        assert.deepEqual(Object.keys(payload[0]).sort(), ['emp_id', ...PAYROLL_FIELDS.map(([key]) => key)].sort())
        assert.equal(payload[0].bonus, '12.34')
        persisted = { ...persisted, ...payload[0], gross_pay: '62.34', net_pay: '42.34', salary_status: 1 }
        return ok({ month: '2026-09', saved: 1, totalGross: 62.34, totalNet: 42.34, ignoredClientTotals: [] })
      }
      assert.equal(payload, null)
      if (path.endsWith('/approve')) { persisted.salary_status = 2; return ok({ month: '2026-09', approved: 1, approvedBy: 'Server identity' }) }
      assert.equal(path, '/hr/payroll/payout'); persisted.salary_status = 3
      return ok({ month: '2026-09', paid: 1, payoutId: 12, recordedBy: 'Server identity', totalNet: 42.34 })
    }
  }
  const actions = createPayrollActions(state, request)
  await actions.refresh()
  state.rows[0].bonus = '12.34'; state.dirty = true
  assert.equal(payrollPermissions(state).approve, false)
  for (const action of ['save', 'approve', 'payout']) {
    assert.equal(await actions.run(action, async () => true), true)
    assert.equal(state.busy, ''); assert.equal(state.ready, true)
  }
  assert.equal(state.rows[0].salary_status, 3)
  assert.equal(state.rows[0].net_pay, '42.34')
  assert.deepEqual(calls.map(call => call.slice(0, 2)), [['GET', '/hr/payroll'], ['POST', '/hr/payroll/save'], ['GET', '/hr/payroll'], ['POST', '/hr/payroll/approve'], ['GET', '/hr/payroll'], ['POST', '/hr/payroll/payout'], ['GET', '/hr/payroll']])
  assert.equal(await actions.run('payout', async () => true), false)
  assert.equal(calls.length, 7)
})

test('state gates: saved/approved/recorded/mixed/locked/dirty/stale; locked rows excluded from save', () => {
  assert.equal(payrollPermissions(loaded(row(0))).approve, false)
  assert.equal(payrollPermissions(loaded(row(1))).approve, true)
  assert.equal(payrollPermissions(loaded(row(1))).payout, false)
  assert.equal(payrollPermissions(loaded(row(2))).payout, true)
  assert.equal(payrollPermissions(loaded(row(3))).save, false)
  assert.equal(payrollPermissions(loaded(row(3))).payout, false)
  assert.equal(payrollPermissions(loaded(row(1), row(2, 2))).payout, false)
  assert.equal(payrollPermissions(loaded(row(2), row(3, 2))).payout, true)
  assert.equal(payrollPermissions({ ...loaded(row(1)), dirty: true }).approve, false)
  assert.equal(payrollPermissions({ ...loaded(row(1)), unlocked: false }).save, false)
  assert.equal(payrollPermissions({ ...loaded(row(1)), month: '2026-08' }).save, false)
  assert.deepEqual(payrollSavePayload([row(0), row(2, 2), row(3, 3)]).map(r => r.emp_id), [1])
  assert.match(payrollStatusText([row(1), row(2, 2), row(3, 3)]), /已保存 1人.*已审批 1人.*已发放记账 1人/)
})

test('confirmation is single-flight; cancellation or view locking sends no POST', async () => {
  const state = loaded(row(1)); let release, posts = 0
  const actions = createPayrollActions(state, { post: async () => { posts++ } })
  const first = actions.run('approve', () => new Promise(resolve => { release = resolve }))
  assert.equal(state.busy, 'approve')
  assert.equal(await actions.run('approve', async () => true), false)
  assert.equal(await actions.run('save', async () => true), false)
  assert.equal(await actions.refresh(), false)
  state.unlocked = false; release(true); await first
  assert.equal(posts, 0); assert.equal(state.busy, '')
  state.unlocked = true
  await actions.run('approve', async () => { throw new Error('cancel') })
  assert.equal(posts, 0); assert.equal(state.ready, true)
})

test('server permission rejection leaves no fabricated approval, keeps rows and blocks repeat until read', async () => {
  const state = loaded(row(1)), before = structuredClone(state.rows)
  const actions = createPayrollActions(state, { post: async () => ({ code: 403, message: '无审批权限' }) })
  assert.equal(await actions.run('approve', async () => true), false)
  assert.deepEqual(state.rows, before); assert.equal(state.ready, false)
  assert.match(state.error, /无审批权限/)
  assert.equal(payrollPermissions(state).payout, false)
  assert.equal(await actions.run('approve', async () => true), false)
})

test('validation and server/network save failures preserve entered values; refresh failure does not wipe them', async () => {
  const state = loaded(row()); state.rows[0].bonus = 'abc'; state.dirty = true
  let posts = 0
  const actions = createPayrollActions(state, { post: async () => { posts++; throw new Error('网络中断') }, get: async () => { throw new Error('回读失败') } })
  await actions.run('save', async () => true)
  assert.equal(posts, 0); assert.equal(state.rows[0].bonus, 'abc')
  state.rows[0].bonus = '12.34'
  await actions.run('save', async () => true)
  assert.equal(posts, 1); assert.equal(state.rows[0].bonus, '12.34'); assert.equal(state.dirty, true)
  assert.equal(state.ready, false)
  await actions.refresh()
  assert.equal(state.rows[0].bonus, '12.34'); assert.equal(state.dirty, true)
  for (const value of ['', '-1', '1.001', 'Infinity', '1e3', '=1+1']) assert.throws(() => payrollSavePayload([{ ...row(), bonus: value }]))
})

test('successful POST with failed/mismatched readback is not reported as completed; input survives', async () => {
  for (const response of [new Error('offline'), ok([row(0)]), ok(null)]) {
    const state = loaded(row()); state.rows[0].bonus = '25'; state.dirty = true
    const actions = createPayrollActions(state, {
      post: async () => ok({ month: '2026-09', saved: 1 }),
      get: async () => { if (response instanceof Error) throw response; return response }
    })
    assert.equal(await actions.run('save', async () => true), false)
    assert.match(state.error, /回读未确认/); assert.equal(state.notice, '')
    assert.equal(state.rows[0].bonus, '25'); assert.equal(state.rows[0].salary_status, 0)
    assert.equal(state.ready, false)
  }
})

test('stale GET / action responses and wrong-month receipts do not overwrite current state', async () => {
  const state = fresh(); let release
  const actions = createPayrollActions(state, { get: () => new Promise(resolve => { release = resolve }) })
  const fetching = actions.refresh()
  actions.invalidate(); state.month = '2026-08'
  release(ok([row(1)])); await fetching
  assert.deepEqual(state.rows, []); assert.equal(state.ready, false)
  Object.assign(state, loaded(row(1)))
  const wrong = createPayrollActions(state, { post: async () => ok({ month: '2026-08', approved: 1 }) })
  await wrong.run('approve', async () => true)
  assert.equal(state.rows[0].salary_status, 1); assert.equal(state.ready, false)
})

test('alreadyRecorded receipt reads status 3 without claiming new payout', async () => {
  const state = loaded(row(2))
  const actions = createPayrollActions(state, { post: async () => ok({ month: '2026-09', paid: 0, alreadyRecorded: true, previouslyRecorded: 1 }), get: async () => ok([row(3)]) })
  assert.equal(await actions.run('payout', async () => true), true)
  assert.match(state.notice, /未新增记账/)
  assert.equal(payrollPermissions(state).payout, false)
})

test('partial save count is rejected even if existing saved rows could mask missing writes', async () => {
  const state = loaded(row(1), row(1, 2)); state.dirty = true
  const actions = createPayrollActions(state, { post: async () => ok({ month: '2026-09', saved: 1 }) })
  assert.equal(await actions.run('save', async () => true), false)
  assert.equal(state.ready, false); assert.equal(state.dirty, true)
  assert.match(state.error, /人数不一致/)
})

test('filter, CSV escaping and report-only printing retain real rows and bookkeeping disclaimer', () => {
  const rows = [row(1), { ...row(2, 2), emp_name: '=HYPERLINK("x")', department: '<img onerror=alert(1)>' }]
  assert.deepEqual(filterPayrollRows(rows, 'synthetic', '测试部门', '1'), [rows[0]])
  const csv = payrollCsv(rows)
  assert.ok(csv.includes('"\'=HYPERLINK(""x"")"'))
  assert.ok(csv.includes(payroll.PAYROLL_NOTE))
  const html = payrollPrintHtml(rows, '2026-09')
  assert.match(html, /&lt;img onerror=alert\(1\)&gt;/)
  assert.doesNotMatch(html, /<script|<img|<input|<button/)
  assert.ok(html.includes(payroll.PAYROLL_NOTE))
})

const filename = new URL('../frontend_v3/src/views/dashboard/Payroll.vue', import.meta.url)
const source = readFileSync(filename, 'utf8')
const compiler = dependency('@vue/compiler-sfc')
const { descriptor, errors } = compiler.parse(source)
const compiled = compiler.compileScript(descriptor, { id: 'payroll-test' })
test('Payroll SFC compiles with editing/filtering/printing and no manual approver fields', () => {
  assert.deepEqual(errors, [])
  assert.deepEqual(compiler.compileTemplate({ source: descriptor.template.content, filename: filename.pathname, id: 'payroll-test', compilerOptions: { bindingMetadata: compiled.bindings } }).errors, [])
  assert.deepEqual(compiler.compileStyle({ source: descriptor.styles[0].content, filename: filename.pathname, id: 'data-v-payroll-test', scoped: true }).errors, [])
  assert.match(source, /@paste="pasteCells/)
  assert.match(source, /@keydown.enter.prevent="focusNextInput/)
  assert.match(source, /:data="filteredPayroll"/)
  assert.doesNotMatch(source, /approvedBy\s*:|staffId\s*:|operator\s*:|salary_status\s*=\s*[123]/)
})

function componentHarness(request) {
  const vue = dependency('vue')
  let tick
  const sandbox = { ...vue, ...payroll, request, ElMessage: { info() {}, warning() {} }, ElMessageBox: { confirm: async () => true },
    onMounted() {}, onUnmounted() {}, setInterval(fn) { tick = fn; return 1 }, clearInterval() {},
    setTimeout, clearTimeout }
  vm.createContext(sandbox)
  vm.runInContext(compiled.content.replace(/import[\s\S]*?from\s*['"][^'"]+['"];?/g, '').replace('export default', 'const component =') + '\nglobalThis.component = component', sandbox)
  const instance = sandbox.component.setup({}, { expose() {} })
  return { instance, tick: () => tick() }
}
test('component unlock reads data.token, timer starts at 1800; missing config rejection does not unlock', async () => {
  let release, calls = 0
  const { instance: ui, tick } = componentHarness({ post: () => { calls++; return new Promise(resolve => { release = resolve }) } })
  ui.unlockCode.value = 'synthetic-test-input'
  const pending = ui.handleUnlock()
  await ui.handleUnlock(); assert.equal(calls, 1)
  release(ok({ token: 'synthetic-response-token' })); await pending
  assert.equal(ui.unlocked.value, true); assert.equal(ui.countdownSeconds.value, 1800)
  tick(); assert.equal(ui.countdownSeconds.value, 1799)
  ui.localLock(); assert.equal(ui.unlocked.value, false)
  const { instance: denied } = componentHarness({ post: async () => { throw new Error('验证码错误') } })
  denied.unlockCode.value = 'synthetic-input'
  await denied.handleUnlock()
  assert.equal(denied.unlocked.value, false); assert.equal(denied.unlockError.value, '验证码错误')
  assert.equal(denied.unlockCode.value, 'synthetic-input')
})

test('component quick input batches filtered editable rows, never approved or recorded rows', () => {
  const { instance: ui } = componentHarness({})
  Object.assign(ui.state, loaded(row(0), row(2, 2), row(3, 3)))
  ui.batchField.value = 'bonus'; ui.batchValue.value = '15.25'
  ui.applyBatch()
  assert.equal(ui.state.rows[0].bonus, '15.25'); assert.equal(ui.state.rows[1].bonus, '10.00')
  assert.equal(ui.state.rows[2].bonus, '10.00'); assert.equal(ui.state.dirty, true)
  ui.pasteCells({ clipboardData: { getData: () => '20\t30\n99\t99' }, preventDefault() {} }, ui.state.rows[0], 'bonus')
  assert.equal(ui.state.rows[0].bonus, '20'); assert.equal(ui.state.rows[0].allowance, '30')
  assert.equal(ui.state.rows[1].bonus, '10.00')
})

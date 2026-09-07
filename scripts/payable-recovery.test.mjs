import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
import { createSettlementAttempt, moneyText } from '../frontend_v3/src/utils/payableSettlement.js'
import { createPayableCreation } from '../frontend_v3/src/utils/payableCreation.js'
const memory = () => { const map = new Map(); return { getItem: k => map.get(k) ?? null, setItem: (k, v) => map.set(k, v), removeItem: k => map.delete(k) } }
const ok = data => ({ code: 200, data })
const rejected = status => Object.assign(new Error('明确拒绝'), { response: { status, data: { code: status, message: '明确拒绝' } } })
const auth = ok({ user: { staffId: 8001 }, storeId: 1 })
const bill = { payableId: 10, payableNo: 'SYN10', storeId: 1, totalAmount: '10.00', pendingAmount: '10.00', supplierName: 'SYN supplier' }
const draft = { supplierName: 'SYN supplier', totalAmount: '5', remark: 'SYN input' }
const scope = '8001:1', creationKey = `payable-create-pending:${scope}`, settlementKey = `payable-pending:${scope}`
function creator(storage, send) { return createPayableCreation({ storage, scope, storeId: 1, send, uuid: () => 'SYN-CREATE-1' }) }

test('creation stores stable number and full normalized payload BEFORE send; unknown/restored never resends', async () => {
  const storage = memory(); let sent, calls = 0
  const first = creator(storage, async payload => {
    calls++; sent = payload
    assert.deepEqual(JSON.parse(storage.getItem(creationKey)).payload, payload)
    throw new Error('timeout after commit')
  })
  await assert.rejects(first.submit(draft), /timeout/)
  assert.deepEqual(sent, { storeId: 1, payableNo: 'PYSYNCREATE1', supplierName: 'SYN supplier', totalAmount: '5.00', remark: 'SYN input' })
  const original = storage.getItem(creationKey)
  const restored = creator(storage, async () => { calls++; throw rejected(403) })
  await assert.rejects(restored.submit({ ...draft, totalAmount: '6' }), /不可重复发送/)
  assert.equal(calls, 1); assert.equal(storage.getItem(creationKey), original)
  assert.equal(restored.reconcile([]), 'missing'); assert.equal(storage.getItem(creationKey), original)
})

test('only unique same number/store/supplier/amount list confirms; duplicates/mismatch/missing preserve raw entry', async () => {
  const storage = memory(), action = creator(storage, async () => ok({ payableId: 11 }))
  await action.submit(draft)
  const raw = storage.getItem(creationKey), payload = action.pending().payload
  const match = { ...payload, payableId: 11 }
  for (const [rows, expected] of [[[], 'missing'], [[match, { ...match, payableId: 12 }], 'ambiguous'],
    [[{ ...match, supplierName: 'other' }], 'mismatch'], [[{ ...match, totalAmount: '5.01' }], 'mismatch'], [[{ ...match, storeId: 2 }], 'mismatch']]) {
    assert.equal(action.reconcile(rows), expected); assert.equal(storage.getItem(creationKey), raw)
  }
  assert.equal(action.reconcile([{ ...match, totalAmount: 5 }]), 'confirmed')
  assert.equal(action.pending(), null)
})

test('only fresh exact HTTP400/403 code rejects allow correction; 409/500/mismatched code/ordinary errors remain unknown', async () => {
  for (const status of [400, 403]) {
    const storage = memory(); let calls = 0
    const action = creator(storage, async () => { if (++calls === 1) throw rejected(status); return ok({ payableId: 10 }) })
    await assert.rejects(action.submit(draft)); assert.equal(action.pending(), null)
    await action.submit({ ...draft, totalAmount: '4' }); assert.equal(calls, 2)
    assert.equal(action.pending().payload.totalAmount, '4.00')
  }
  for (const error of [rejected(409), rejected(500), Object.assign(new Error('mismatch'), { response: { status: 400, data: { code: 500 } } }), new Error('code lost')]) {
    const storage = memory(), action = creator(storage, async () => { throw error })
    await assert.rejects(action.submit(draft)); assert.ok(action.pending())
  }
})

test('storage write failure prevents POST; concurrent click and query cannot clear in-flight creation', async () => {
  let calls = 0
  const unavailable = creator({ getItem: () => null, setItem: () => { throw Error('quota') } }, async () => { calls++ })
  await assert.rejects(unavailable.submit(draft), /quota/); assert.equal(calls, 0)
  const storage = memory(); let finish
  const action = creator(storage, () => new Promise(resolve => { calls++; finish = resolve }))
  const active = action.submit(draft)
  await assert.rejects(action.submit(draft), /正在提交/)
  assert.equal(action.reconcile([{ ...action.pending().payload, payableId: 10 }]), 'waiting')
  finish(ok({ payableId: 10 })); await active
  assert.ok(action.pending()); assert.equal(calls, 1)
})

let dependency
for (const path of ['../frontend_v3/package.json', '../../../../frontend_v3/package.json']) {
  const candidate = createRequire(new URL(path, import.meta.url))
  try { candidate.resolve('@vue/compiler-sfc'); dependency = candidate; break } catch {}
}
if (!dependency) throw Error('需要已有 Vue 依赖，不自动安装')
const compiler = dependency('@vue/compiler-sfc'), vue = dependency('vue')
const filename = new URL('../frontend_v3/src/components/PayableLedger.vue', import.meta.url)
const source = readFileSync(filename, 'utf8'), parsed = compiler.parse(source)
const script = compiler.compileScript(parsed.descriptor, { id: 'payable-recovery' })
function harness(request, storage = memory()) {
  let mounted, unmounted
  const box = { ref: vue.ref, reactive: vue.reactive, computed: vue.computed, watch: vue.watch,
    onMounted: fn => { mounted = fn }, onUnmounted: fn => { unmounted = fn }, ElMessage: { success() {} },
    request, sessionStorage: storage, createSettlementAttempt, createPayableCreation }
  vm.createContext(box)
  vm.runInContext(script.content.replace(/import[\s\S]*?from\s*['"][^'"]+['"];?/g, '').replace('export default', 'const component =') + '\nglobalThis.component = component', box)
  return { ui: box.component.setup({}, { expose() {} }), mount: () => mounted(), unmount: () => unmounted(), storage }
}
const baseGet = async path => path === '/auth/me' ? auth : ok([bill])

test('VM creation timeout persists across remount, corrected inputs cannot repost, exact list recovery unlocks', async () => {
  const storage = memory(); let calls = 0, sent, listed = []
  const request = { get: async path => path === '/auth/me' ? auth : ok(listed), post: async (_, payload) => { calls++; sent = payload; throw Error('timeout after commit') } }
  const first = harness(request, storage); await first.mount()
  Object.assign(first.ui.draft, draft); await first.ui.create()
  assert.ok(first.ui.creationPending.value); assert.equal(first.ui.canStartWrite.value, false)
  first.unmount()
  const restored = harness(request, storage); await restored.mount()
  assert.equal(restored.ui.draft.totalAmount, '5.00')
  Object.assign(restored.ui.draft, { totalAmount: '6' }); await restored.ui.create()
  assert.equal(calls, 1)
  await restored.ui.load(); assert.equal(restored.ui.canStartWrite.value, false)
  listed = [{ ...sent, payableId: 20 }]
  await restored.ui.load()
  assert.equal(restored.ui.creationPending.value, null); assert.equal(restored.ui.canStartWrite.value, true)
  assert.equal(calls, 1)
})

test('VM first definitive creation rejection preserves draft and permits corrected submit without remount', async () => {
  let calls = 0, created
  const app = harness({ get: async path => path === '/auth/me' ? auth : ok(created ? [created] : []), post: async (_, payload) => {
    calls++; if (calls === 1) throw rejected(400)
    created = { ...payload, payableId: 22 }; return ok({ payableId: 22 })
  } })
  await app.mount(); Object.assign(app.ui.draft, draft); await app.ui.create()
  assert.equal(app.ui.draft.totalAmount, '5'); assert.equal(app.ui.canStartWrite.value, true)
  app.ui.draft.totalAmount = '4'; await app.ui.create()
  assert.equal(calls, 2); assert.equal(app.ui.creationPending.value, null)
})

test('VM corrupted creation OR settlement journals preserve raw entries, permit list/history, forbid all writes', async () => {
  for (const key of [creationKey, settlementKey]) {
    const storage = memory(); storage.setItem(key, '{broken')
    let reads = 0, writes = 0
    const app = harness({ get: async path => {
      if (path === '/auth/me') return auth
      reads++; return ok(path.endsWith('/settlements') ? [] : [bill])
    }, post: async () => { writes++ } }, storage)
    await app.mount()
    assert.equal(app.ui.rows.value.length, 1); assert.equal(app.ui.ready.value, true)
    assert.match(app.ui.recoveryError.value, /损坏.*原条目已保留/)
    await app.ui.history(app.ui.rows.value[0]); assert.equal(reads, 2)
    app.ui.openSettlement(app.ui.rows.value[0]); app.ui.selected.value = app.ui.rows.value[0]
    await app.ui.settle(); Object.assign(app.ui.draft, draft); await app.ui.create()
    assert.equal(writes, 0); assert.equal(storage.getItem(key), '{broken')
  }
})

test('VM scope switch invalidates rows/readiness/history immediately; late list/history responses ignored', async () => {
  let finish, signal; const started = new Promise(resolve => { signal = resolve })
  const app = harness({ get: async path => path === '/auth/me' ? auth : new Promise(resolve => { finish = resolve; signal() }) })
  const mounting = app.mount(); await started
  app.ui.storeId.value = 2
  assert.equal(app.ui.ready.value, false); assert.equal(app.ui.rows.value.length, 0)
  finish(ok([bill])); await mounting
  assert.equal(app.ui.rows.value.length, 0); assert.equal(app.ui.ready.value, false)
  let finishHistory
  const other = harness({ get: async path => path.endsWith('/settlements') ? new Promise(resolve => { finishHistory = resolve }) : baseGet(path) })
  await other.mount()
  const history = other.ui.history(other.ui.rows.value[0])
  other.ui.storeId.value = 2
  assert.equal(other.ui.showHistory.value, false); assert.equal(other.ui.historyLoading.value, false)
  finishHistory(ok([{ settlementNo: 'OLD' }])); await history
  assert.equal(other.ui.records.value.length, 0)
})

test('VM restores independent account/store journals and never writes using old rows or unauthorized query', async () => {
  const storage = memory()
  const saved = creator(storage, async () => { throw Error('unknown') }); await assert.rejects(saved.submit(draft))
  let writes = 0
  const app = harness({ get: baseGet, post: async () => { writes++ } }, storage); await app.mount()
  assert.ok(app.ui.creationPending.value)
  const oldRow = app.ui.rows.value[0]
  app.ui.storeId.value = 2
  assert.equal(app.ui.creationPending.value, null)
  app.ui.openSettlement(oldRow); await app.ui.settle(); await app.ui.create()
  await app.ui.load() // mock returns store 1: must fail the scope check
  assert.equal(app.ui.ready.value, false); assert.match(app.ui.error.value, /门店/)
  assert.equal(writes, 0)
  app.ui.storeId.value = 1; assert.ok(app.ui.creationPending.value)
  app.ui.staffId.value = 8002; assert.equal(app.ui.creationPending.value, null)
  app.ui.staffId.value = 8001; assert.ok(app.ui.creationPending.value)
  const denied = harness({ get: async path => path === '/auth/me' ? auth : Promise.reject(rejected(403)), post: async () => { writes++ } })
  await denied.mount(); Object.assign(denied.ui.draft, draft); await denied.ui.create()
  assert.equal(writes, 0); assert.equal(denied.ui.ready.value, false)
})

test('VM parent settlement fix: fresh exact rejection is correctable; unknown/remounted403 retains original key', async () => {
  let calls = 0
  const app = harness({ get: baseGet, post: async (_, payload) => {
    calls++; if (calls === 1) throw rejected(400)
    return ok({ payableId: payload.payableId, settlementNo: 'SYNSETTLE' })
  } })
  await app.mount(); app.ui.openSettlement(app.ui.rows.value[0]); app.ui.settleAmount.value = '11'; await app.ui.settle()
  assert.equal(app.ui.pending.value, null); assert.equal(app.ui.canSubmitSettlement.value, true)
  app.ui.settleAmount.value = '10'; await app.ui.settle(); assert.equal(calls, 2)
  const storage = memory()
  const unknown = harness({ get: baseGet, post: async () => { throw Error('timeout') } }, storage)
  await unknown.mount(); unknown.ui.openSettlement(unknown.ui.rows.value[0]); await unknown.ui.settle()
  const raw = storage.getItem(settlementKey)
  const restored = harness({ get: baseGet, post: async () => { throw rejected(403) } }, storage)
  await restored.mount(); restored.ui.resume(); await restored.ui.settle()
  assert.equal(storage.getItem(settlementKey), raw); assert.ok(restored.ui.pending.value)
})

test('VM no action while create in-flight; old scope POST completion cannot replace current rows', async () => {
  let finish, calls = 0
  const app = harness({ get: baseGet, post: () => { calls++; return new Promise(resolve => { finish = resolve }) } })
  await app.mount(); Object.assign(app.ui.draft, draft)
  const active = app.ui.create()
  await app.ui.create(); app.ui.openSettlement(app.ui.rows.value[0]); await app.ui.settle()
  assert.equal(calls, 1)
  app.ui.storeId.value = 2
  finish(ok({ payableId: 22 })); await active
  assert.equal(app.ui.rows.value.length, 0); assert.equal(app.ui.ready.value, false)
  assert.ok(app.storage.getItem(creationKey))
})

test('PayableLedger script/template/style compile; recovery and per-scope gates are bound', () => {
  assert.deepEqual(parsed.errors, [])
  assert.deepEqual(compiler.compileTemplate({ source: parsed.descriptor.template.content, filename: filename.pathname, id: 'payable-recovery', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.deepEqual(compiler.compileStyle({ source: parsed.descriptor.styles[0].content, filename: filename.pathname, id: 'data-v-payable-recovery', scoped: true }).errors, [])
  assert.match(source, /:disabled="!canStartWrite"/)
  assert.match(source, /:disabled="!canSubmitSettlement"/)
})

import assert from 'node:assert/strict'
import { test } from 'node:test'
import { createPayableCreation } from '../frontend_v3/src/utils/payableCreation.js'
import { harness, auth, ok, draft, memory } from './payable-recovery.test.mjs'
const scope = '8001:1', key = `payable-create-pending:${scope}`
const receipt = (p, extra = {}) => ({ code: 200, data: { requestId: p.requestId, payableId: 81, payableNo: p.payableNo, storeId: p.storeId, totalAmount: p.totalAmount, replayed: false, ...extra } })
const denied = status => Object.assign(Error('denied'), { response: { status, data: { code: status } } })
const action = (storage, send, extra = {}) => createPayableCreation({ storage, scope, storeId: 1, send, uuid: () => 'synthetic-v2-1', ...extra })
async function unknown(storage) {
  const a = action(storage, async () => { throw Error('timeout') })
  await assert.rejects(a.submit(draft)); return a.pending()
}
test('v2 remount explicitly restores exact persisted request despite edited draft, and verifies replay receipt', async () => {
  const storage = memory(), saved = await unknown(storage)
  assert.equal(saved.version, 2); assert.equal(saved.scope, scope)
  let calls = 0
  const a = action(storage, async p => { calls++; assert.deepEqual(p, saved.payload); return receipt(p, { replayed: true }) })
  await assert.rejects(a.submit({ ...draft, totalAmount: '9' }))
  assert.equal(calls, 0)
  assert.equal((await a.resume()).replayed, true); assert.equal(calls, 1)
  assert.equal(a.pending().receipt.payableId, 81)
  assert.equal(a.reconcile([{ ...saved.payload, payableId: 82 }]), 'mismatch')
  assert.equal(a.reconcile([{ ...saved.payload, payableId: 81 }]), 'confirmed')
})
test('restored HTTP400/403/409 and fresh409 preserve original journal unchanged', async () => {
  for (const status of [400, 403, 409]) {
    const storage = memory(); await unknown(storage); const raw = storage.getItem(key)
    await assert.rejects(action(storage, async () => { throw denied(status) }).resume())
    assert.equal(storage.getItem(key), raw)
  }
  const storage = memory(), a = action(storage, async () => { throw denied(409) })
  await assert.rejects(a.submit(draft)); assert.equal(a.pending().version, 2)
})
test('v1 never resends, still supports unique list reconciliation', async () => {
  const storage = memory(), saved = await unknown(storage)
  delete saved.payload.requestId
  storage.setItem(key, JSON.stringify({ version: 1, payload: saved.payload }))
  let calls = 0; const a = action(storage, async () => { calls++ })
  await assert.rejects(a.resume(), /旧版/); assert.equal(calls, 0)
  assert.equal(a.reconcile([{ ...saved.payload, payableId: 81 }]), 'confirmed')
})
test('copied cross-account/cross-store v2 and added payload fields fail closed before POST', async () => {
  for (const mutate of [s => { s.scope = '8002:1' }, s => { s.payload.storeId = 2 }, s => { s.payload.staffId = 99 }, s => { s.payload.requestId = '' }, s => { s.payload.requestId = 'x'.repeat(65) }]) {
    const storage = memory(), saved = await unknown(storage); mutate(saved)
    const raw = JSON.stringify(saved); storage.setItem(key, raw); let calls = 0
    await assert.rejects(action(storage, async () => { calls++ }).resume(), /损坏/)
    assert.equal(calls, 0); assert.equal(storage.getItem(key), raw)
  }
})
test('every missing/mismatched receipt field preserves journal; replay must keep acknowledged ID', async () => {
  for (const extra of [{ requestId: undefined }, { requestId: 'other' }, { payableId: 0 }, { payableId: '81' }, { payableNo: 'other' }, { storeId: 2 }, { storeId: '1' }, { totalAmount: '5.01' }, { totalAmount: null }, { replayed: undefined }, { replayed: 'true' }]) {
    const storage = memory(), a = action(storage, async p => receipt(p, extra))
    await assert.rejects(a.submit(draft), /回执/); assert.ok(a.pending()); assert.equal(a.pending().receipt, undefined)
  }
  const storage = memory(); let id = 81
  const a = action(storage, async p => receipt(p, { payableId: id }))
  await a.submit(draft); const raw = storage.getItem(key); id = 82
  await assert.rejects(a.resume(), /回执/); assert.equal(storage.getItem(key), raw)
})
test('cross-instance concurrent resume is blocked and reconciliation waits for the active response', async () => {
  const storage = memory(); await unknown(storage); let finish, calls = 0
  const send = p => new Promise(resolve => { calls++; finish = () => resolve(receipt(p)) })
  const a = action(storage, send), b = action(storage, send), flight = a.resume()
  await assert.rejects(b.resume(), /正在提交/)
  assert.equal(b.reconcile([{ ...b.pending().payload, payableId: 81 }]), 'waiting')
  finish(); await flight; assert.equal(calls, 1)
})
test('storage throws or silently drops writes: fresh and restored POST blocked', async () => {
  let calls = 0
  await assert.rejects(action({ getItem: () => null, setItem() {} }, async () => { calls++ }).submit(draft), /可靠保存/)
  const storage = memory(); await unknown(storage)
  storage.setItem = () => { throw Error('quota') }
  await assert.rejects(action(storage, async () => { calls++ }).resume(), /quota/)
  assert.equal(calls, 0)
})
test('VM explicit restore ignores edited form, uses original request and clears only after list match', async () => {
  const storage = memory(), saved = await unknown(storage); let listed = [], calls = 0
  const app = harness({ get: async path => path === '/auth/me' ? auth : ok(listed), post: async (_, p) => {
    calls++; assert.deepEqual(p, saved.payload); listed = [{ ...p, payableId: 81 }]; return receipt(p, { replayed: true })
  } }, storage)
  await app.mount(); assert.equal(app.ui.canResumeCreation.value, true)
  app.ui.draft.totalAmount = '999'; await app.ui.resumeCreation()
  assert.equal(calls, 1); assert.equal(app.ui.creationPending.value, null); assert.equal(app.ui.canStartWrite.value, true)
})
test('VM unauthorized/cross-scope/v1 recovery cannot POST; restored rejection stays locked', async () => {
  const storage = memory(); await unknown(storage); let calls = 0
  const app = harness({ get: async path => path === '/auth/me' ? auth : ok([]), post: async () => { calls++; throw denied(403) } }, storage)
  await app.mount(); const raw = storage.getItem(key)
  await app.ui.resumeCreation(); assert.equal(calls, 1); assert.equal(storage.getItem(key), raw)
  assert.equal(app.ui.canStartWrite.value, false)
  app.ui.storeId.value = 2; await app.ui.resumeCreation(); assert.equal(calls, 1)
  const deniedApp = harness({ get: async path => { if (path === '/auth/me') return auth; throw denied(403) }, post: async () => { calls++ } }, storage)
  await deniedApp.mount(); await deniedApp.ui.resumeCreation(); assert.equal(calls, 1)
  const saved = JSON.parse(raw); delete saved.payload.requestId; storage.setItem(key, JSON.stringify({ version: 1, payload: saved.payload }))
  const old = harness({ get: async path => path === '/auth/me' ? auth : ok([]), post: async () => { calls++ } }, storage)
  await old.mount(); assert.equal(old.ui.canResumeCreation.value, false); await old.ui.resumeCreation(); assert.equal(calls, 1)
})

test('VM concurrent restore sends once; late old-scope receipt cannot overwrite current UI', async () => {
  const storage = memory(); await unknown(storage); let finish, calls = 0
  const app = harness({ get: async path => path === '/auth/me' ? auth : ok([]), post: (_, p) => {
    calls++; return new Promise(resolve => { finish = () => resolve(receipt(p, { replayed: true })) })
  } }, storage)
  await app.mount(); const flight = app.ui.resumeCreation()
  await app.ui.resumeCreation(); assert.equal(calls, 1)
  app.ui.storeId.value = 2; finish(); await flight
  assert.equal(app.ui.rows.value.length, 0); assert.equal(app.ui.ready.value, false)
  assert.equal(app.ui.creationPending.value, null)
  assert.equal(JSON.parse(storage.getItem(key)).receipt.payableId, 81)
})

test('VM GM store zero stays unselected: no business request until explicit store choice', async () => {
  let reads = 0, writes = 0, listed = []
  const app = harness({ get: async (path, config) => {
    if (path === '/auth/me') return ok({ user: { staffId: 8001 }, storeId: 0 })
    reads++; assert.equal(config.params.storeId, 2); return ok(listed)
  }, post: async (_, p) => { writes++; assert.equal(p.storeId, 2); listed = [{ ...p, payableId: 81 }]; return receipt(p) } })
  await app.mount(); assert.equal(app.ui.storeId.value, null); assert.equal(reads, 0)
  Object.assign(app.ui.draft, draft); await app.ui.create(); await app.ui.resumeCreation()
  assert.equal(writes, 0); assert.equal(app.ui.canStartWrite.value, false)
  app.ui.storeId.value = 2; await app.ui.load(); assert.equal(app.ui.canStartWrite.value, true)
  Object.assign(app.ui.draft, draft); await app.ui.create(); assert.equal(writes, 1)
  const staff = harness({ get: async path => path === '/auth/me' ? auth : ok([]) })
  await staff.mount(); assert.equal(staff.ui.storeId.value, 1)
})

import { test } from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { moneyText, createSettlementAttempt } from '../frontend_v3/src/utils/payableSettlement.js'
const memory = () => { const m = new Map(); return { getItem: k => m.get(k), setItem: (k,v) => m.set(k,v), removeItem: k => m.delete(k) } }
const success = body => ({ code: 200, data: { payableId: body.payableId, settlementNo: 'SYNTHETIC-SETTLE-1', replayed: false } })
test('decimal normalization rejects fractions, exponents, negative and overflow without rounding', () => {
  assert.equal(moneyText('0001.2'), '1.20'); assert.equal(moneyText('9999999999.99'), '9999999999.99')
  for (const value of ['0', '-1', '1.005', '1e3', '', 'NaN', '10000000000', null]) assert.throws(() => moneyText(value))
})
test('timeout followed by remount replays exact request and preserves parameters', async () => {
  const storage = memory(), sent = []
  const first = createSettlementAttempt({ storage, scope: 'synthetic:1', uuid: () => 'same-key', send: async body => { sent.push(body); throw Error('timeout after commit') } })
  await assert.rejects(first.submit(10, '12.34'), /timeout/)
  const second = createSettlementAttempt({ storage, scope: 'synthetic:1', uuid: () => { throw Error('must not create key') }, send: async body => { sent.push(body); return success(body) } })
  await assert.rejects(second.submit(10, '12.35'), /尚未确认/)
  await assert.rejects(second.submit(11, '12.34'), /尚未确认/)
  await second.submit(10, '12.34')
  assert.deepEqual(sent[0], sent[1]); assert.equal(second.pending(), null)
})
test('double click sends exactly once until first response resolves', async () => {
  let finish, calls = 0
  const attempt = createSettlementAttempt({ storage: memory(), scope: 'a:1', uuid: () => 'key', send: body => { calls++; return new Promise(resolve => { finish = () => resolve(success(body)) }) } })
  const active = attempt.submit(1, '1')
  await assert.rejects(attempt.submit(1, '1'), /正在提交/)
  finish(); await active; assert.equal(calls, 1)
})
test('non-success or mismatched response never discards retry record', async () => {
  for (const result of [{code:400}, {code:200,data:{}}, {code:200,data:{payableId:2,settlementNo:'wrong'}}]) {
    const attempt = createSettlementAttempt({ storage: memory(), scope: 'a:1', uuid: () => 'key', send: async () => result })
    await assert.rejects(attempt.submit(1, '1'), /响应不完整/)
    assert.equal(attempt.pending().requestId, 'key')
  }
})
test('storage failure prevents network; account and store scopes do not share pending request', async () => {
  let calls = 0
  const storage = memory()
  const first = createSettlementAttempt({ storage, scope:'a:1', uuid: () => 'key', send: async () => { throw Error('offline') } })
  await assert.rejects(first.submit(1,'1'))
  assert.equal(createSettlementAttempt({ storage, scope:'b:1', send:async()=>{} }).pending(), null)
  assert.equal(createSettlementAttempt({ storage, scope:'a:2', send:async()=>{} }).pending(), null)
  const broken = createSettlementAttempt({ scope:'x:1', storage: { getItem:()=>null, setItem:()=>{throw Error('quota')} }, uuid:()=> 'key', send:async()=> { calls++ } })
  await assert.rejects(broken.submit(1,'1'), /quota/); assert.equal(calls,0)
})
test('Vue component and its parent parse and compile', () => {
  let compiler
  for (const path of ['../frontend_v3/package.json', '../../../../frontend_v3/package.json']) {
    try { compiler = createRequire(new URL(path, import.meta.url))('@vue/compiler-sfc'); break } catch {}
  }
  assert.ok(compiler, 'existing Vue compiler required')
  for (const file of ['components/PayableLedger.vue','views/dashboard/Finance.vue']) {
    const source = readFileSync(new URL('../frontend_v3/src/' + file, import.meta.url), 'utf8')
    const parsed = compiler.parse(source); assert.deepEqual(parsed.errors, [])
    const script = compiler.compileScript(parsed.descriptor, { id: file })
    const template = compiler.compileTemplate({ source: parsed.descriptor.template.content, filename: file, id: file, compilerOptions: { bindingMetadata:script.bindings } })
    assert.deepEqual(template.errors, [])
  }
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { createReceivableLedger, pendingText, explainError, isUnknownOutcome, STATUS_TEXT }
  from '../../src/utils/receivableLedger/index.js'
import { createSyntheticApi, memoryStorage, brokenStorage, sequentialUuid } from './syntheticApi.mjs'

function ledger(overrides = {}) {
  const api = overrides.api ?? createSyntheticApi()
  const storage = overrides.storage ?? memoryStorage()
  return {
    api,
    storage,
    ops: createReceivableLedger({
      storage, staffId: overrides.staffId ?? 'staff-1', storeId: overrides.storeId ?? 1,
      api, uuid: overrides.uuid ?? sequentialUuid()
    })
  }
}

// ==================== 正常闭环：新建 -> 详情 -> 部分收款 -> 收清 -> 刷新 ====================

test('新建应收到收清的完整闭环，余额与状态全部以服务端为准', async () => {
  const { api, ops } = ledger()

  const created = await ops.receivable.submit({ totalAmount: '100.00', customerName: '张三', remark: '婚宴尾款' })
  assert.equal(created.replayed, false)
  assert.equal(created.snapshot.status, 'unpaid')
  assert.equal(created.snapshot.pending_amount, '100.00')

  // 列表能查到，核对后恢复记录才清
  const list = await ops.listReceivables()
  assert.equal(ops.receivable.reconcile(list.data), 'confirmed')
  assert.equal(ops.receivable.pending(), null)

  const detail = await ops.receivableDetail(created.receivableId)
  assert.deepEqual(detail.data.payments, [])

  // 部分收款
  const part = await ops.payment.submit({ receivableId: created.receivableId, amount: '30.00' })
  assert.equal(part.snapshot.amount, '30.00')
  const afterPart = await ops.receivableDetail(created.receivableId)
  assert.equal(afterPart.data.received_amount, '30.00')
  assert.equal(afterPart.data.pending_amount, '70.00')
  assert.equal(afterPart.data.status, 'partial')
  assert.equal(STATUS_TEXT[afterPart.data.status], '部分收款')
  assert.equal(afterPart.data.payments.length, 1)

  assert.equal(ops.payment.reconcile((await ops.listPayments()).data), 'confirmed')

  // 收清
  await ops.payment.submit({ receivableId: created.receivableId, amount: '70.00' })
  const afterAll = await ops.receivableDetail(created.receivableId)
  assert.equal(afterAll.data.status, 'paid')
  assert.equal(afterAll.data.pending_amount, '0.00')
  assert.equal(pendingText(afterAll.data), '0.00')
  assert.equal(api.calls.recordPayment, 2)
})

// ==================== 丢响应：只能恢复，绝不新建 ====================

test('响应丢失后用原请求号恢复，服务端只记一笔', async () => {
  const { api, ops } = ledger()
  const created = await ops.receivable.submit({ totalAmount: '50.00' })
  ops.receivable.reconcile((await ops.listReceivables()).data)

  api.dropNextResponse()
  await assert.rejects(() => ops.payment.submit({ receivableId: created.receivableId, amount: '20.00' }))

  const saved = ops.payment.pending()
  assert.ok(saved, '结果未知时恢复记录必须保留')
  assert.equal(saved.receipt, undefined, '没拿到回执就不该存回执')
  const requestId = saved.payload.requestId

  // 结果未知 -> 只准恢复
  await assert.rejects(() => ops.payment.submit({ receivableId: created.receivableId, amount: '20.00' }),
    /尚未确认/)

  const resumed = await ops.payment.resume()
  assert.equal(resumed.replayed, true, '恢复必须命中服务端幂等，而不是新记一笔')
  assert.equal(resumed.requestId, requestId)
  assert.equal(api.calls.recordPayment, 2, '两次调用，但只应产生一笔流水')

  const payments = (await ops.listPayments()).data
  assert.equal(payments.length, 1)
  assert.equal(payments[0].amount, '20.00')
  const detail = await ops.receivableDetail(created.receivableId)
  assert.equal(detail.data.received_amount, '20.00', '不能重复扣一次')
})

test('未知结果判定：网络失败与 5xx 都算未知，400/403 才是明确拒绝', () => {
  assert.equal(isUnknownOutcome(new Error('socket hang up')), true)
  assert.equal(isUnknownOutcome({ response: { status: 500 } }), true)
  assert.equal(isUnknownOutcome({ response: { status: 409 } }), true)
  assert.equal(isUnknownOutcome({ response: { status: 400 } }), false)
  assert.equal(isUnknownOutcome({ response: { status: 403 } }), false)
})

test('首次请求被 400 明确拒绝时清恢复记录，恢复时的 400 则必须保留', async () => {
  const { api, ops } = ledger()
  api.failNextWith(400, '应收金额必填')
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '10.00' }), /应收金额必填/)
  assert.equal(ops.receivable.pending(), null, '服务端明确拒绝=没记账，日志可清')

  // 制造一条"结果未知"的待恢复记录：500 时服务端没落库，但客户端无从得知
  api.failNextWith(500, '服务端处理失败')
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '11.00' }))
  assert.ok(ops.receivable.pending(), '500 属于结果未知，必须留恢复记录')

  api.failNextWith(400, '临时校验失败')
  await assert.rejects(() => ops.receivable.resume(), /临时校验失败/)
  assert.ok(ops.receivable.pending(), '恢复时的 400 说明不了当初那次有没有落库，必须留着')
})

// ==================== 改参数不得复用旧键 ====================

test('有未确认请求时不许换参数重发；恢复只重放原 payload', async () => {
  const { api, ops } = ledger()
  api.dropNextResponse()
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '80.00', remark: '原备注' }))

  const saved = ops.receivable.pending()
  assert.equal(saved.payload.totalAmount, '80.00')
  assert.equal(saved.payload.remark, '原备注')

  await assert.rejects(() => ops.receivable.submit({ totalAmount: '90.00', remark: '改过的备注' }),
    /尚未确认/)

  // resume 不接受任何参数，只能原样重放
  const resumed = await ops.receivable.resume()
  assert.equal(resumed.snapshot.total_amount, '80.00')
  assert.equal(resumed.replayed, true)
  assert.equal((await ops.listReceivables()).data.length, 1)
})

test('同一个请求号配不同参数，服务端 409，恢复记录不清', async () => {
  const { api, storage, ops } = ledger()
  // 先让服务端真的落库但客户端拿不到回执：此时盘上有 payload、没有 receipt
  api.dropNextResponse()
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '60.00' }))
  const key = ops.receivable.key
  const saved = JSON.parse(storage.getItem(key))
  assert.equal(saved.receipt, undefined)

  // 篡改盘上的金额，模拟"同键改参"，恢复必须被服务端按冲突拒绝
  saved.payload.totalAmount = '61.00'
  storage.setItem(key, JSON.stringify(saved))
  await assert.rejects(() => ops.receivable.resume(), err => err.response?.status === 409)
  assert.ok(storage.getItem(key), '409 不能清日志：原单已存在，必须人工核对')
  assert.equal((await ops.listReceivables()).data.length, 1, '冲突不得再记一笔')
  assert.equal(api.calls.createReceivable, 2)
})

test('盘上 payload 与已存回执自相矛盾时直接锁死，不拿去重发', async () => {
  const { storage, ops } = ledger()
  await ops.receivable.submit({ totalAmount: '60.00' })
  const saved = JSON.parse(storage.getItem(ops.receivable.key))
  assert.ok(saved.receipt, '成功后应留着回执直到与列表核对上')
  saved.payload.totalAmount = '61.00'          // 回执说 60，payload 说 61
  storage.setItem(ops.receivable.key, JSON.stringify(saved))
  assert.throws(() => ops.receivable.pending(), /恢复记录已损坏/)
  await assert.rejects(() => ops.receivable.resume(), /恢复记录已损坏/)
})

// ==================== 作用域隔离 ====================

test('换门店或换人，恢复记录互不串用', async () => {
  const api = createSyntheticApi({ stores: [1, 2] })
  const storage = memoryStorage()
  const uuid = sequentialUuid()
  const a = createReceivableLedger({ storage, staffId: 'staff-1', storeId: 1, api, uuid })
  const b = createReceivableLedger({ storage, staffId: 'staff-1', storeId: 2, api, uuid })
  const c = createReceivableLedger({ storage, staffId: 'staff-9', storeId: 1, api, uuid })

  assert.notEqual(a.receivable.key, b.receivable.key)
  assert.notEqual(a.receivable.key, c.receivable.key)

  api.dropNextResponse()
  await assert.rejects(() => a.receivable.submit({ totalAmount: '10.00' }))
  assert.ok(a.receivable.pending())
  assert.equal(b.receivable.pending(), null, '别的门店不该看见这条待恢复')
  assert.equal(c.receivable.pending(), null, '别的操作人不该看见这条待恢复')

  // 换店后可以正常新建，不会被别店的未确认请求挡住
  const other = await b.receivable.submit({ totalAmount: '20.00' })
  assert.equal(other.snapshot.store_id, 2)
})

test('无登录身份直接拒绝构造，不发任何请求', () => {
  const api = createSyntheticApi()
  assert.throws(() => createReceivableLedger({ storage: memoryStorage(), staffId: '', storeId: 1, api }),
    /未取得登录身份/)
  assert.throws(() => createReceivableLedger({ storage: memoryStorage(), staffId: 'staff-1', storeId: 0, api }),
    /有效门店/)
})

test('越权访问别店：服务端 403，前端如实转述且不泄露对方数据', async () => {
  const api = createSyntheticApi({ stores: [1] })
  const storage = memoryStorage()
  const ops = createReceivableLedger({ storage, staffId: 'staff-1', storeId: 2, api, uuid: sequentialUuid() })
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '10.00' }), err => {
    assert.equal(err.response.status, 403)
    assert.equal(explainError(err), '无权操作其他门店的应收与收款数据')
    return true
  })
  assert.equal(ops.receivable.pending(), null, '403 是明确拒绝，日志可清')
})

// ==================== 手工收款规则 ====================

test('无应收来源的手工收款必须写类别与业务说明，前端先拦一道', async () => {
  const { api, ops } = ledger()
  await assert.rejects(() => ops.payment.submit({ amount: '25.00' }), /收款类别/)
  await assert.rejects(() => ops.payment.submit({ amount: '25.00', category: '零星收款' }), /业务说明/)
  await assert.rejects(
    () => ops.payment.submit({ amount: '25.00', category: 'x'.repeat(33), remark: '说明' }), /不能超过 32 字/)
  assert.equal(api.calls.recordPayment, 0, '前端拦下的不该打到服务端')

  const done = await ops.payment.submit({ amount: '25.00', category: '零星收款', remark: '客人现场补款' })
  assert.equal(done.snapshot.receivable_id, null)
  assert.equal(done.snapshot.payment_category, '零星收款')
})

test('挂了应收的收款不强制类别与说明', async () => {
  const { ops } = ledger()
  const created = await ops.receivable.submit({ totalAmount: '40.00' })
  ops.receivable.reconcile((await ops.listReceivables()).data)
  const paid = await ops.payment.submit({ receivableId: created.receivableId, amount: '40.00' })
  assert.equal(paid.snapshot.payment_category, null)
})

test('超额收款被服务端拒绝，前端不伪造负余额', async () => {
  const { ops } = ledger()
  const created = await ops.receivable.submit({ totalAmount: '40.00' })
  ops.receivable.reconcile((await ops.listReceivables()).data)
  await ops.payment.submit({ receivableId: created.receivableId, amount: '30.00' })
  ops.payment.reconcile((await ops.listPayments()).data)

  await assert.rejects(() => ops.payment.submit({ receivableId: created.receivableId, amount: '20.00' }),
    err => err.response.status === 400 && /超过该应收单的待收金额/.test(err.response.data.message))

  const detail = await ops.receivableDetail(created.receivableId)
  assert.equal(detail.data.pending_amount, '10.00', '被拒的那笔不得影响余额')
  assert.equal(detail.data.status, 'partial')
})

test('引用不存在或跨店时服务端 400，前端不伪造客户或账户', async () => {
  const api = createSyntheticApi({ stores: [1, 2], customers: { 7: 2 }, accounts: { 5: 2 } })
  const storage = memoryStorage()
  const ops = createReceivableLedger({ storage, staffId: 'staff-1', storeId: 1, api, uuid: sequentialUuid() })
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '10.00', customerId: 7 }), /客户不存在/)
  await assert.rejects(
    () => ops.payment.submit({ amount: '10.00', accountId: 5, category: '杂项', remark: '说明' }),
    /收款账户不存在/)
})

// ==================== 存储不可用 ====================

test('恢复记录写不进去就不许发请求', async () => {
  const api = createSyntheticApi()
  const ops = createReceivableLedger({ storage: brokenStorage(), staffId: 'staff-1', storeId: 1, api,
    uuid: sequentialUuid() })
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '10.00' }), /未能可靠保存/)
  assert.equal(api.calls.createReceivable, 0, '没有恢复凭据就绝不能把请求发出去')
})

test('恢复记录被外部改坏时锁死写入，只允许人工核对后丢弃', async () => {
  const { storage, ops } = ledger()
  storage.setItem(ops.receivable.key, '{不是合法 JSON')
  assert.throws(() => ops.receivable.pending(), /恢复记录已损坏/)
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '10.00' }), /恢复记录已损坏/)
  ops.receivable.discardAfterManualCheck()
  assert.equal(ops.receivable.pending(), null)
})

// ==================== 核对结果 ====================

test('列表核对的四种结果', async () => {
  const { ops } = ledger()
  assert.equal(ops.receivable.reconcile([]), 'none')
  const created = await ops.receivable.submit({ totalAmount: '10.00' })
  assert.equal(ops.receivable.reconcile([]), 'missing', '列表里没有=还没核上，别清日志')

  const rows = (await ops.listReceivables()).data
  assert.equal(ops.receivable.reconcile([...rows, { ...rows[0] }]), 'ambiguous')
  assert.equal(ops.receivable.reconcile([{ ...rows[0], store_id: 99 }]), 'mismatch')
  assert.equal(ops.receivable.reconcile(rows), 'confirmed')
  assert.equal(ops.receivable.pending(), null)
  assert.ok(created.receivableId > 0)
})

test('列表中主键相同但金额或应收归属错误时不得清除恢复记录', async () => {
  const { ops } = ledger()
  const created = await ops.receivable.submit({ totalAmount: '10.00' })
  const receivables = (await ops.listReceivables()).data
  assert.equal(ops.receivable.reconcile([{ ...receivables[0], total_amount: '99.00' }]), 'mismatch')
  assert.ok(ops.receivable.pending(), '应收金额关联不符时必须保留恢复记录')
  assert.equal(ops.receivable.reconcile(receivables), 'confirmed')

  await ops.payment.submit({ receivableId: created.receivableId, amount: '4.00' })
  const payments = (await ops.listPayments()).data
  assert.equal(ops.payment.reconcile([{ ...payments[0], receivable_id: created.receivableId + 1 }]), 'mismatch')
  assert.ok(ops.payment.pending(), '收款挂错应收时必须保留恢复记录')
  assert.equal(ops.payment.reconcile(payments), 'confirmed')
})

test('回执与请求对不上时当作结果未知，保留恢复记录', async () => {
  const api = createSyntheticApi()
  const storage = memoryStorage()
  const broken = {
    ...api,
    createReceivable: async body => {
      const real = await api.createReceivable(body)
      return { ...real, data: { ...real.data, no: 'RVDEADBEEFDEADBEEF' } }   // 单号被改
    }
  }
  const ops = createReceivableLedger({ storage, staffId: 'staff-1', storeId: 1, api: broken,
    uuid: sequentialUuid() })
  await assert.rejects(() => ops.receivable.submit({ totalAmount: '10.00' }), /回执不完整或与本次请求不符/)
  assert.ok(ops.receivable.pending(), '回执可疑时必须保留恢复记录')
})

// ==================== 提示文案 ====================

test('错误提示不泄露请求体与内部细节', () => {
  const cases = [
    [{ response: { status: 400, data: { message: '应收金额必填' } } }, '应收金额必填'],
    [{ response: { status: 401 } }, '登录已失效，请重新登录后再操作'],
    [{ response: { status: 500 } }, '服务端处理失败，本次未记账']
  ]
  for (const [error, expected] of cases) assert.equal(explainError(error), expected)
  const text = explainError(new Error('Request failed'))
  assert.ok(!/token|Bearer|requestId/i.test(text))
})

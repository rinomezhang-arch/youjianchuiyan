// 合成后端：照 b664eaa1 的 ReceivablePaymentService / FinanceController 的规则写。
// 这不是真实联调——它只保证前端在"服务端按契约行事"时行为正确。
// 真实联调由父统筹后续安排，本轮明确不声称。

function httpError(status, message) {
  const error = new Error(message)
  error.response = { status, data: { code: status, message } }
  return error
}

const CONFLICT_MESSAGE = '该请求号此前已用于另一笔不同参数的操作，本次未记账'

function money(text) {
  const [whole, fraction = '00'] = String(text).split('.')
  return BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'))
}

function moneyText(cents) {
  return `${cents / 100n}.${String(cents % 100n).padStart(2, '0')}`
}

// 与服务端同构的无歧义指纹：类型:字节长度:内容;
// 用 TextEncoder 而不是 Buffer，好让这份合成后端在 node 与浏览器里都能跑。
const encoder = new TextEncoder()
function fingerprint(parts) {
  return parts.map(p => {
    if (p === null || p === undefined) return 'N;'
    const tag = typeof p === 'number' ? 'N' : typeof p === 'boolean' ? 'B' : 'S'
    const value = String(p)
    return `${tag}:${encoder.encode(value).length}:${value};`
  }).join('')
}

/**
 * @param stores        本身份可操作的门店集合
 * @param identity      null 表示未登录，触发 403
 * @param customers/bookings/accounts  已存在的引用，形如 {id: storeId}
 */
export function createSyntheticApi({
  stores = [1], identity = 'staff-1', customers = {}, bookings = {}, accounts = {}, today = '2026-09-08'
} = {}) {
  const receivables = new Map()
  const payments = new Map()
  const registry = new Map()          // requestId -> {opType, storeId, targetId, targetNo, hash}
  let nextReceivableId = 1
  let nextPaymentId = 1
  const calls = { createReceivable: 0, recordPayment: 0 }
  let failNext = null                 // 注入一次性故障，模拟丢响应

  function requireStore(storeId) {
    if (!identity) throw httpError(403, '未登录或身份无效，无法访问应收与收款数据')
    if (!Number.isSafeInteger(storeId) || storeId <= 0) throw httpError(400, '请选择有效门店')
    if (!stores.includes(storeId)) throw httpError(403, '无权操作其他门店的应收与收款数据')
    return storeId
  }

  function requireRequestId(requestId) {
    const key = typeof requestId === 'string' ? requestId.trim() : ''
    if (!key) throw httpError(400, '缺少 requestId：必须带幂等键，且重试时保持同一个值')
    if (key.length > 64) throw httpError(400, 'requestId 不能超过 64 字符')
    return key
  }

  function requireAmount(raw, label, strictlyPositive) {
    if (raw === null || raw === undefined || raw === '') throw httpError(400, `${label}必填`)
    if (!/^\d{1,10}(\.\d{1,2})?$/.test(String(raw))) throw httpError(400, `${label}最多两位小数`)
    const cents = money(raw)
    if (strictlyPositive && cents <= 0n) throw httpError(400, `${label}必须大于 0`)
    return cents
  }

  function validateRef(map, id, label) {
    if (id === null || id === undefined) return
    if (map[id] === undefined) throw httpError(400, `${label}不存在或不属于当前门店`)
  }

  function validateRefScoped(map, id, storeId, label) {
    if (id === null || id === undefined) return
    if (map[id] !== storeId) throw httpError(400, `${label}不存在或不属于当前门店`)
  }

  function receivableSnapshot(id) {
    const r = receivables.get(id)
    return {
      receivable_id: r.receivable_id, store_id: r.store_id, receivable_no: r.receivable_no,
      total_amount: r.total_amount, received_amount: r.received_amount,
      pending_amount: r.pending_amount, status: r.status
    }
  }

  function paymentSnapshot(id) {
    const p = payments.get(id)
    return {
      payment_id: p.payment_id, store_id: p.store_id, payment_no: p.payment_no,
      receivable_id: p.receivable_id, amount: p.amount, payment_method: p.payment_method,
      payment_category: p.payment_category, payment_date: p.payment_date
    }
  }

  function replay(record, key, hash, expectedOp) {
    if (record.opType !== expectedOp || record.hash !== hash) throw httpError(409, CONFLICT_MESSAGE)
    return ok({
      requestId: key,
      [expectedOp === 'receivable' ? 'receivableId' : 'paymentId']: record.targetId,
      no: record.targetNo,
      replayed: true,
      snapshot: expectedOp === 'receivable' ? receivableSnapshot(record.targetId) : paymentSnapshot(record.targetId),
      message: '该请求此前已处理，返回原记录回执，未重复记账'
    })
  }

  function ok(data) {
    return { code: 200, message: 'success', data }
  }

  function maybeFail() {
    if (!failNext) return
    const f = failNext
    failNext = null
    if (f.afterCommit) return f       // 由调用点在提交后再抛，模拟"记了但响应丢了"
    throw f.error
  }

  return {
    calls,
    /** 让下一次写入"提交成功但响应丢失"，用于验证恢复而不是重发新键 */
    dropNextResponse() { failNext = { afterCommit: true, error: httpError(undefined, 'socket hang up') } },
    /** 让下一次写入在服务端就失败 */
    failNextWith(status, message) { failNext = { error: httpError(status, message) } },
    snapshotOf: receivableSnapshot,

    async createReceivable(body) {
      calls.createReceivable += 1
      const key = requireRequestId(body.requestId)
      const storeId = requireStore(body.storeId)
      const total = requireAmount(body.totalAmount, '应收金额', false)
      validateRefScoped(customers, body.customerId, storeId, '客户')
      validateRefScoped(bookings, body.bookingId, storeId, '预订单')

      const hash = fingerprint(['receivable', storeId, body.receivableNo ?? null, body.customerId ?? null,
        body.customerName ?? null, body.bookingId ?? null, body.bookingNo ?? null,
        moneyText(total), body.receivableDate ?? null, body.dueDate ?? null,
        body.creditDays ?? null, body.remark ?? null])

      const prior = registry.get(key)
      if (prior) return replay(prior, key, hash, 'receivable')

      const receivableDate = body.receivableDate ?? today
      const creditDays = body.creditDays ?? 30
      const dueDate = body.dueDate ?? addDays(receivableDate, creditDays)
      if (dueDate < receivableDate) throw httpError(400, '到期日不能早于应收日期')

      const failure = maybeFail()
      const id = nextReceivableId++
      const no = body.receivableNo ?? `RV${String(id).padStart(16, '0')}`
      receivables.set(id, {
        receivable_id: id, store_id: storeId, receivable_no: no,
        customer_id: body.customerId ?? null, customer_name: body.customerName ?? null,
        booking_id: body.bookingId ?? null, booking_no: body.bookingNo ?? null,
        total_amount: moneyText(total), received_amount: '0.00', pending_amount: moneyText(total),
        receivable_date: receivableDate, due_date: dueDate, status: 'unpaid',
        credit_days: creditDays, remark: body.remark ?? null
      })
      registry.set(key, { opType: 'receivable', storeId, targetId: id, targetNo: no, hash })
      if (failure) throw failure.error          // 已落库，但调用方拿不到回执
      return ok({ requestId: key, receivableId: id, no, replayed: false,
        snapshot: receivableSnapshot(id), message: '已登记。这是账务记录，不代表银行实际到账' })
    },

    async recordPayment(body) {
      calls.recordPayment += 1
      const key = requireRequestId(body.requestId)
      const storeId = requireStore(body.storeId)
      const amount = requireAmount(body.amount, '收款金额', true)

      let receivable = null
      if (body.receivableId !== null && body.receivableId !== undefined) {
        receivable = receivables.get(body.receivableId)
        if (!receivable) throw httpError(400, `应收单不存在：${body.receivableId}`)
        if (receivable.store_id !== storeId) throw httpError(403, '该应收单不属于当前门店，无权登记收款')
      }
      validateRefScoped(customers, body.customerId, storeId, '客户')
      validateRefScoped(bookings, body.bookingId, storeId, '预订单')
      validateRefScoped(accounts, body.accountId, storeId, '收款账户')

      if (receivable === null) {
        if (!body.category) throw httpError(400, '无应收来源的手工收款必须填写收款类别 category')
        if (!body.remark) throw httpError(400, '无应收来源的手工收款必须填写业务说明 remark')
        if (String(body.category).trim().length > 32) throw httpError(400, '收款类别不能超过 32 字')
      }

      const hash = fingerprint(['payment', storeId, body.paymentNo ?? null, body.receivableId ?? null,
        body.customerId ?? null, body.customerName ?? null, body.bookingId ?? null, body.bookingNo ?? null,
        moneyText(amount), body.paymentDate ?? null, body.paymentMethod ?? null,
        body.accountId ?? null, body.category ?? null, body.remark ?? null])

      const prior = registry.get(key)
      if (prior) return replay(prior, key, hash, 'payment')

      let newReceived = null, newPending = null, newStatus = null
      if (receivable) {
        const total = money(receivable.total_amount)
        const received = money(receivable.received_amount)
        newReceived = received + amount
        if (newReceived > total) {
          throw httpError(400, `收款金额超过该应收单的待收金额，本次未登记。待收 ${moneyText(total - received)}，本次 ${moneyText(amount)}`)
        }
        newPending = total - newReceived
        newStatus = newPending === 0n ? 'paid' : (newReceived > 0n ? 'partial' : 'unpaid')
      }

      const failure = maybeFail()
      const id = nextPaymentId++
      const no = body.paymentNo ?? `PAY${String(id).padStart(16, '0')}`
      payments.set(id, {
        payment_id: id, store_id: storeId, payment_no: no,
        payment_date: body.paymentDate ?? today, receivable_id: body.receivableId ?? null,
        customer_id: body.customerId ?? null, customer_name: body.customerName ?? null,
        booking_id: body.bookingId ?? null, booking_no: body.bookingNo ?? null,
        amount: moneyText(amount), payment_method: body.paymentMethod ?? null,
        account_id: body.accountId ?? null, payment_category: body.category ?? null,
        remark: body.remark ?? null
      })
      if (receivable) {
        receivable.received_amount = moneyText(newReceived)
        receivable.pending_amount = moneyText(newPending)
        receivable.status = newStatus
      }
      registry.set(key, { opType: 'payment', storeId, targetId: id, targetNo: no, hash })
      if (failure) throw failure.error
      return ok({ requestId: key, paymentId: id, no, replayed: false,
        snapshot: paymentSnapshot(id), message: '已登记。这是账务记录，不代表银行实际到账' })
    },

    async listReceivables(storeId) {
      const sid = requireStore(storeId)
      return ok([...receivables.values()].filter(r => r.store_id === sid))
    },

    async receivableDetail(id, storeId) {
      const sid = requireStore(storeId)
      const r = receivables.get(id)
      if (!r || r.store_id !== sid) throw httpError(400, '应收单不存在或不属于当前门店')
      return ok({ ...r, payments: [...payments.values()].filter(p => p.receivable_id === id && p.store_id === sid) })
    },

    async listPayments(storeId) {
      const sid = requireStore(storeId)
      return ok([...payments.values()].filter(p => p.store_id === sid))
    }
  }
}

function addDays(date, days) {
  const d = new Date(`${date}T00:00:00Z`)
  d.setUTCDate(d.getUTCDate() + days)
  return d.toISOString().slice(0, 10)
}

/** 只实现 get/set/remove 的内存 storage；另加一个"写不进去"的坏 storage 用于测试 */
export function memoryStorage() {
  const map = new Map()
  return {
    getItem: k => (map.has(k) ? map.get(k) : null),
    setItem: (k, v) => map.set(k, String(v)),
    removeItem: k => map.delete(k),
    _map: map
  }
}

export function brokenStorage() {
  return { getItem: () => null, setItem: () => {}, removeItem: () => {} }
}

/** 确定性请求号：必须含足够十六进制位，单号才推导得出来 */
export function sequentialUuid(prefix = '') {
  let n = 0
  return () => `${prefix}${(++n).toString(16).padStart(32 - prefix.length, '0')}`
}

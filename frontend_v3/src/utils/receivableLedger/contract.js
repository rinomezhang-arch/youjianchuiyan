// 请求体与回执的冻结契约，严格照 b664eaa1 的 FinanceController 六方法与 ReceivablePaymentService 写。
// 这里的校验只是**尽早拦住明显错的输入**，服务端仍然是唯一权威：
// 余额、状态、单号、默认日期一律以服务端返回为准，前端不自行结算、不补默认值。

import { amountText } from './money.js'

export const REQUEST_ID_PATTERN = /^(?:[0-9a-fA-F]{32}|[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})$/
const DATE_PATTERN = /^\d{4}-\d{2}-\d{2}$/

// 单号前缀与位数对齐服务端自动生成的格式（RV+16 / PAY+16），避免两种来源的单号长得不一样。
const RECEIVABLE_NO_PATTERN = /^RV[0-9A-F]{32}$/
const PAYMENT_NO_PATTERN = /^PAY[0-9A-F]{32}$/

export const RECEIVABLE_PAYLOAD_KEYS = [
  'bookingId', 'bookingNo', 'creditDays', 'customerId', 'customerName', 'dueDate',
  'receivableDate', 'receivableNo', 'remark', 'requestId', 'storeId', 'totalAmount'
]

export const PAYMENT_PAYLOAD_KEYS = [
  'accountId', 'amount', 'bookingId', 'bookingNo', 'category', 'customerId', 'customerName',
  'paymentDate', 'paymentMethod', 'paymentNo', 'receivableId', 'remark', 'requestId', 'storeId'
]

/** 服务端未传就用当天/30 天，且默认值是在指纹之后才补的——所以空就老实传 null，别替它填。 */
function optionalDate(value, label) {
  const text = String(value ?? '').trim()
  if (!text) return null
  if (!DATE_PATTERN.test(text)) throw new Error(`${label}格式应为 yyyy-MM-dd`)
  return text
}

function optionalText(value) {
  const text = String(value ?? '').trim()
  return text === '' ? null : text
}

function optionalId(value, label) {
  if (value === null || value === undefined || value === '') return null
  const n = Number(value)
  if (!Number.isSafeInteger(n) || n <= 0) throw new Error(`${label}不正确`)
  return n
}

function requireStoreId(storeId) {
  const n = Number(storeId)
  if (!Number.isSafeInteger(n) || n <= 0) throw new Error('请先选择有效门店')
  return n
}

function requireRequestId(requestId) {
  if (typeof requestId !== 'string' || !REQUEST_ID_PATTERN.test(requestId)) {
    throw new Error('请求号无效，本次未发送')
  }
  return requestId
}

/** 单号由 requestId 推导：同一个键重发得到同一个单号，列表核对才认得出是不是同一张。 */
function documentNo(prefix, requestId) {
  const hex = requestId.replace(/[^0-9a-fA-F]/g, '').toUpperCase()
  if (hex.length !== 32) throw new Error('请求号无法推导单号，本次未发送')
  return `${prefix}${hex}`
}

export function buildReceivablePayload(draft, { storeId, requestId }) {
  const payload = {
    requestId: requireRequestId(requestId),
    storeId: requireStoreId(storeId),
    receivableNo: documentNo('RV', requestId),
    customerId: optionalId(draft.customerId, '客户'),
    customerName: optionalText(draft.customerName),
    bookingId: optionalText(draft.bookingId),   // 业务字符串，不是数字主键
    bookingNo: optionalText(draft.bookingNo),
    totalAmount: amountText(draft.totalAmount, { allowZero: true, label: '应收金额' }),
    receivableDate: optionalDate(draft.receivableDate, '应收日期'),
    dueDate: optionalDate(draft.dueDate, '到期日'),
    creditDays: draft.creditDays === null || draft.creditDays === undefined || draft.creditDays === ''
      ? null
      : creditDays(draft.creditDays),
    remark: optionalText(draft.remark)
  }
  if (!RECEIVABLE_NO_PATTERN.test(payload.receivableNo)) throw new Error('应收单号推导失败，本次未发送')
  // 两个日期都给了才校验先后；只给一个时到期日由服务端按账期推，前端无从判断
  if (payload.receivableDate && payload.dueDate && payload.dueDate < payload.receivableDate) {
    throw new Error('到期日不能早于应收日期')
  }
  assertKeys(payload, RECEIVABLE_PAYLOAD_KEYS)
  return payload
}

function creditDays(value) {
  const n = Number(value)
  if (!Number.isSafeInteger(n) || n < 0) throw new Error('账期天数不正确')
  return n
}

export function buildPaymentPayload(draft, { storeId, requestId }) {
  const receivableId = optionalId(draft.receivableId, '应收单')
  const category = optionalText(draft.category)
  const remark = optionalText(draft.remark)

  // 无来源手工收款：历史上确实有 960 条无引用，所以不强制必须挂应收；
  // 但旧数据无来源不是新数据可以无来源的依据——类别和业务说明都必须写，否则事后无从追溯。
  if (receivableId === null) {
    if (!category) throw new Error('无应收来源的手工收款必须填写收款类别，用于说明这笔钱因何而收')
    if (!remark) throw new Error('无应收来源的手工收款必须填写业务说明，仅有类别不足以追溯')
    if (category.length > 32) throw new Error('收款类别不能超过 32 字')
  }

  const payload = {
    requestId: requireRequestId(requestId),
    storeId: requireStoreId(storeId),
    paymentNo: documentNo('PAY', requestId),
    paymentDate: optionalDate(draft.paymentDate, '收款日期'),
    receivableId,
    customerId: optionalId(draft.customerId, '客户'),
    customerName: optionalText(draft.customerName),
    bookingId: optionalText(draft.bookingId),
    bookingNo: optionalText(draft.bookingNo),
    amount: amountText(draft.amount, { allowZero: false, label: '收款金额' }),
    paymentMethod: optionalText(draft.paymentMethod),
    accountId: optionalId(draft.accountId, '收款账户'),
    category,
    remark
  }
  if (!PAYMENT_NO_PATTERN.test(payload.paymentNo)) throw new Error('收款单号推导失败，本次未发送')
  assertKeys(payload, PAYMENT_PAYLOAD_KEYS)
  return payload
}

function assertKeys(payload, expected) {
  if (Object.keys(payload).sort().join(',') !== expected.join(',')) {
    throw new Error('请求体字段与冻结契约不符，本次未发送')
  }
}

/**
 * 回执校验。回执对不上就当**结果未知**处理：保留恢复记录、不清账，
 * 让调用方走"用原 key 恢复"，而不是当成功放过去。
 */
export function validateReceivableReceipt(data, payload) {
  return (
    isPlainObject(data) &&
    data.requestId === payload.requestId &&
    Number.isSafeInteger(data.receivableId) && data.receivableId > 0 &&
    data.no === payload.receivableNo &&
    typeof data.replayed === 'boolean' &&
    isPlainObject(data.snapshot) &&
    Number(data.snapshot.receivable_id) === data.receivableId &&
    Number(data.snapshot.store_id) === payload.storeId &&
    data.snapshot.receivable_no === data.no &&
    safeAmount(data.snapshot.total_amount) === payload.totalAmount
  )
}

export function validatePaymentReceipt(data, payload) {
  return (
    isPlainObject(data) &&
    data.requestId === payload.requestId &&
    Number.isSafeInteger(data.paymentId) && data.paymentId > 0 &&
    data.no === payload.paymentNo &&
    typeof data.replayed === 'boolean' &&
    isPlainObject(data.snapshot) &&
    Number(data.snapshot.payment_id) === data.paymentId &&
    Number(data.snapshot.store_id) === payload.storeId &&
    data.snapshot.payment_no === data.no &&
    safeAmount(data.snapshot.amount) === payload.amount &&
    (payload.receivableId === null
      ? data.snapshot.receivable_id === null || data.snapshot.receivable_id === undefined
      : Number(data.snapshot.receivable_id) === payload.receivableId)
  )
}

/**
 * 列表核对也必须回到同一张业务单，不能只凭一个碰巧相同的主键清掉恢复记录。
 * 回执负责 requestId 与主键，列表负责业务单号、金额和应收归属，三层缺一不可。
 */
export function validateReceivableListRow(row, payload) {
  return (
    isPlainObject(row) &&
    row.receivable_no === payload.receivableNo &&
    safeAmount(row.total_amount) === payload.totalAmount &&
    nullableIdEquals(row.customer_id, payload.customerId) &&
    nullableTextEquals(row.booking_id, payload.bookingId) &&
    nullableTextEquals(row.booking_no, payload.bookingNo)
  )
}

export function validatePaymentListRow(row, payload) {
  return (
    isPlainObject(row) &&
    row.payment_no === payload.paymentNo &&
    safeAmount(row.amount) === payload.amount &&
    nullableIdEquals(row.customer_id, payload.customerId) &&
    nullableTextEquals(row.booking_id, payload.bookingId) &&
    nullableTextEquals(row.booking_no, payload.bookingNo) &&
    nullableIdEquals(row.account_id, payload.accountId) &&
    (payload.receivableId === null
      ? row.receivable_id === null || row.receivable_id === undefined
      : Number(row.receivable_id) === payload.receivableId)
  )
}

function nullableIdEquals(actual, expected) {
  if (expected === null) return actual === null || actual === undefined
  return Number(actual) === expected
}

function nullableTextEquals(actual, expected) {
  const normalizedActual = actual === null || actual === undefined || actual === '' ? null : String(actual)
  return normalizedActual === expected
}

function safeAmount(raw) {
  try {
    return amountText(raw, { allowZero: true })
  } catch {
    return null
  }
}

function isPlainObject(v) {
  return Boolean(v) && typeof v === 'object' && !Array.isArray(v)
}

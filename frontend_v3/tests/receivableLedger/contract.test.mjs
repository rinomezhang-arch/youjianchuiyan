import test from 'node:test'
import assert from 'node:assert/strict'
import { amountText, centsOf, centsText, displayAmount } from '../../src/utils/receivableLedger/money.js'
import {
  buildReceivablePayload, buildPaymentPayload,
  validateReceivableReceipt, validatePaymentReceipt,
  validateReceivableListRow, validatePaymentListRow,
  RECEIVABLE_PAYLOAD_KEYS, PAYMENT_PAYLOAD_KEYS
} from '../../src/utils/receivableLedger/contract.js'
import { pendingText } from '../../src/utils/receivableLedger/index.js'

const REQ = 'a1b2c3d4e5f60718293a4b5c6d7e8f90'

test('金额规范化：整数与小数都补到两位', () => {
  assert.equal(amountText('7'), '7.00')
  assert.equal(amountText('7.5'), '7.50')
  assert.equal(amountText(' 7.50 '), '7.50')
  assert.equal(amountText(0, { allowZero: true }), '0.00')
})

test('金额拒绝：负数、超两位小数、非数字、以及默认不许为零', () => {
  for (const bad of ['-1', '1.234', 'abc', '', null, undefined, '1e3', '1,000']) {
    assert.throws(() => amountText(bad), /金额/)
  }
  assert.throws(() => amountText('0'), /须大于零/)
})

test('金额走分不走浮点：0.1+0.2 不会漂', () => {
  assert.equal(centsText(centsOf(amountText('0.1')) + centsOf(amountText('0.2'))), '0.30')
  assert.equal(centsText(centsOf('9999999999.99') + centsOf('0.01')), '10000000000.00')
})

test('未知金额显示 --，不显示 0：把未知说成零会让人以为真没钱', () => {
  assert.equal(displayAmount(null), '--')
  assert.equal(displayAmount(''), '--')
  assert.equal(displayAmount('abc'), '--')
  assert.equal(displayAmount('12'), '12.00')
})

test('待收优先用服务端 pending_amount，缺失才用总额减已收', () => {
  assert.equal(pendingText({ pending_amount: '30.00', total_amount: '100.00', received_amount: '70.00' }), '30.00')
  assert.equal(pendingText({ total_amount: '100.00', received_amount: '70.00' }), '30.00')
  assert.equal(pendingText({}), '--')
})

test('应收请求体字段集与冻结契约完全一致', () => {
  const payload = buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: REQ })
  assert.deepEqual(Object.keys(payload).sort(), RECEIVABLE_PAYLOAD_KEYS)
  assert.equal(payload.receivableNo, 'RVA1B2C3D4E5F60718293A4B5C6D7E8F90')
  assert.equal(payload.totalAmount, '10.00')
})

test('空日期与空账期一律传 null，不替服务端填当天', () => {
  const payload = buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: REQ })
  assert.equal(payload.receivableDate, null)
  assert.equal(payload.dueDate, null)
  assert.equal(payload.creditDays, null)
  assert.equal(payload.remark, null)
})

test('两个日期都给了才校验先后', () => {
  assert.throws(
    () => buildReceivablePayload({ totalAmount: '10', receivableDate: '2026-09-10', dueDate: '2026-09-01' },
      { storeId: 1, requestId: REQ }), /到期日不能早于应收日期/)
  assert.doesNotThrow(
    () => buildReceivablePayload({ totalAmount: '10', dueDate: '2026-09-01' }, { storeId: 1, requestId: REQ }))
})

test('日期格式与门店、请求号的硬校验', () => {
  assert.throws(() => buildReceivablePayload({ totalAmount: '10', receivableDate: '2026/09/10' },
    { storeId: 1, requestId: REQ }), /yyyy-MM-dd/)
  assert.throws(() => buildReceivablePayload({ totalAmount: '10' }, { storeId: 0, requestId: REQ }), /有效门店/)
  assert.throws(() => buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: 'x'.repeat(65) }),
    /请求号无效/)
  assert.throws(() => buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: 'has space' }),
    /请求号无效/)
})

test('bookingId 是业务字符串不是数字主键，原样透传', () => {
  const payload = buildReceivablePayload({ totalAmount: '10', bookingId: 'BK-2026-0001' },
    { storeId: 1, requestId: REQ })
  assert.equal(payload.bookingId, 'BK-2026-0001')
})

test('收款请求体字段集一致，金额必须大于零', () => {
  const payload = buildPaymentPayload({ amount: '10', receivableId: 3 }, { storeId: 1, requestId: REQ })
  assert.deepEqual(Object.keys(payload).sort(), PAYMENT_PAYLOAD_KEYS)
  assert.equal(payload.paymentNo, 'PAYA1B2C3D4E5F60718293A4B5C6D7E8F90')
  assert.equal(payload.receivableId, 3)
  assert.throws(() => buildPaymentPayload({ amount: '0', receivableId: 3 }, { storeId: 1, requestId: REQ }),
    /须大于零/)
})

test('手工收款（无 receivableId）必须带类别与说明，类别不超过 32 字', () => {
  assert.throws(() => buildPaymentPayload({ amount: '10' }, { storeId: 1, requestId: REQ }), /收款类别/)
  assert.throws(() => buildPaymentPayload({ amount: '10', category: '零星' }, { storeId: 1, requestId: REQ }),
    /业务说明/)
  assert.throws(() => buildPaymentPayload({ amount: '10', category: 'x'.repeat(33), remark: '说明' },
    { storeId: 1, requestId: REQ }), /32 字/)
  const ok = buildPaymentPayload({ amount: '10', category: '零星收款', remark: '客人现场补款' },
    { storeId: 1, requestId: REQ })
  assert.equal(ok.receivableId, null)
})

test('同一个请求号推导出的单号是稳定的', () => {
  const a = buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: REQ })
  const b = buildReceivablePayload({ totalAmount: '99' }, { storeId: 1, requestId: REQ })
  assert.equal(a.receivableNo, b.receivableNo)
})

test('不同请求号即使前16位相同也必须生成不同业务单号', () => {
  const a = buildReceivablePayload({ totalAmount: '10' },
    { storeId: 1, requestId: '1111111111111111aaaaaaaaaaaaaaaa' })
  const b = buildReceivablePayload({ totalAmount: '10' },
    { storeId: 1, requestId: '1111111111111111bbbbbbbbbbbbbbbb' })
  assert.notEqual(a.receivableNo, b.receivableNo)
})

test('回执校验：任一项对不上都判不通过', () => {
  const payload = buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: REQ })
  const good = {
    requestId: REQ, receivableId: 5, no: payload.receivableNo, replayed: false,
    snapshot: { receivable_id: 5, receivable_no: payload.receivableNo, store_id: 1, total_amount: '10.00',
      customer_id: null, booking_id: null, booking_no: null }
  }
  assert.equal(validateReceivableReceipt(good, payload), true)
  assert.equal(validateReceivableReceipt({ ...good, requestId: 'other' }, payload), false)
  assert.equal(validateReceivableReceipt({ ...good, no: 'RV0000000000000001' }, payload), false)
  assert.equal(validateReceivableReceipt({ ...good, receivableId: 0 }, payload), false)
  assert.equal(validateReceivableReceipt({ ...good, replayed: 'yes' }, payload), false)
  assert.equal(validateReceivableReceipt({ ...good, snapshot: { ...good.snapshot, store_id: 2 } }, payload), false)
  assert.equal(validateReceivableReceipt(
    { ...good, snapshot: { ...good.snapshot, receivable_no: 'RV0000000000000001' } }, payload), false)
  assert.equal(validateReceivableReceipt({ ...good, snapshot: { ...good.snapshot, total_amount: '11.00' } },
    payload), false)
  assert.equal(validateReceivableReceipt(null, payload), false)
})

test('收款回执必须与是否挂应收一致', () => {
  const linked = buildPaymentPayload({ amount: '10', receivableId: 3 }, { storeId: 1, requestId: REQ })
  const base = { requestId: REQ, paymentId: 8, no: linked.paymentNo, replayed: false,
    snapshot: { payment_id: 8, payment_no: linked.paymentNo, store_id: 1, amount: '10.00', receivable_id: 3,
      customer_id: null, booking_id: null, booking_no: null, account_id: null } }
  assert.equal(validatePaymentReceipt(base, linked), true)
  assert.equal(validatePaymentReceipt({ ...base, snapshot: { ...base.snapshot, receivable_id: 4 } }, linked), false)
  assert.equal(validatePaymentReceipt(
    { ...base, snapshot: { ...base.snapshot, payment_no: 'PAY0000000000000001' } }, linked), false)

  const manual = buildPaymentPayload({ amount: '10', category: '零星', remark: '说明' },
    { storeId: 1, requestId: REQ })
  const manualReceipt = { requestId: REQ, paymentId: 9, no: manual.paymentNo, replayed: false,
    snapshot: { payment_id: 9, payment_no: manual.paymentNo, store_id: 1, amount: '10.00', receivable_id: null,
      customer_id: null, booking_id: null, booking_no: null, account_id: null } }
  assert.equal(validatePaymentReceipt(manualReceipt, manual), true)
  assert.equal(validatePaymentReceipt(
    { ...manualReceipt, snapshot: { ...manualReceipt.snapshot, receivable_id: 3 } }, manual), false)
})

test('列表核对必须同时匹配业务单号、金额和应收归属', () => {
  const receivable = buildReceivablePayload({ totalAmount: '10' }, { storeId: 1, requestId: REQ })
  const receivableRow = { receivable_no: receivable.receivableNo, total_amount: '10.00',
    customer_id: null, booking_id: null, booking_no: null }
  assert.equal(validateReceivableListRow(receivableRow, receivable), true)
  assert.equal(validateReceivableListRow({ ...receivableRow, receivable_no: 'RV0000000000000001' }, receivable), false)
  assert.equal(validateReceivableListRow({ ...receivableRow, total_amount: '11.00' }, receivable), false)

  const payment = buildPaymentPayload({ amount: '4', receivableId: 3 }, { storeId: 1, requestId: REQ })
  const paymentRow = { payment_no: payment.paymentNo, amount: '4.00', receivable_id: 3,
    customer_id: null, booking_id: null, booking_no: null, account_id: null }
  assert.equal(validatePaymentListRow(paymentRow, payment), true)
  assert.equal(validatePaymentListRow({ ...paymentRow, payment_no: 'PAY0000000000000001' }, payment), false)
  assert.equal(validatePaymentListRow({ ...paymentRow, amount: '5.00' }, payment), false)
  assert.equal(validatePaymentListRow({ ...paymentRow, receivable_id: 4 }, payment), false)
})

test('请求体里不含任何凭证字段', () => {
  const payload = buildReceivablePayload({ totalAmount: '10', remark: '备注' }, { storeId: 1, requestId: REQ })
  const text = JSON.stringify(payload).toLowerCase()
  for (const forbidden of ['token', 'password', 'secret', 'authorization']) {
    assert.ok(!text.includes(forbidden), `请求体不应含 ${forbidden}`)
  }
})

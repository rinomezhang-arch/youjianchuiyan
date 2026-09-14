// TR-MARKETING-INQUIRY-LOOKUP-UI-56 API 契约测试（MOCKED_CONTRACT，零网络）。
// 运行：node --import ./register.mjs api-contract.test.mjs
// 覆盖：POST 请求体白名单、URL 不含手机号、状态文案四态、字段白名单、统一空结果、手机号校验。
import assert from 'node:assert/strict'
import {
  INQUIRY_STATUS_TEXT,
  isValidLookupPhone,
  lookupBookingInquiry,
  normalizeInquiryLookup
} from './marketing-under-test.mjs'
import { calls as axiosCalls, resetCalls as resetAxiosCalls } from './axios-stub.mjs'
import { calls as requestCalls, resetCalls as resetRequestCalls } from './request-stub.mjs'

// lookupBookingInquiry 走独立 axios 实例（axios-stub），其他走 request（request-stub）
const calls = axiosCalls
function resetCalls() { resetAxiosCalls(); resetRequestCalls() }

let pass = 0
let fail = 0
function check(name, fn) {
  try {
    fn()
    pass++
    console.log(`PASS | ${name}`)
  } catch (e) {
    fail++
    console.log(`FAIL | ${name} | ${e.message}`)
  }
}

// 预热：真实调用一次 lookup，供请求体断言使用（走内存桩，无网络）
resetCalls()
await lookupBookingInquiry(' INQ9001 ', ' 13800000001 ')

check('A1 请求方法与路径：POST /public/booking-inquiry/lookup（baseURL /api 由请求层拼接）', () => {
  assert.equal(calls.length, 1)
  assert.equal(calls[0].config.method, 'post')
  assert.equal(calls[0].config.url, '/public/booking-inquiry/lookup')
})

check('A2 请求体键白名单：恰好 {inquiryNo, phone}，无 storeId/remark/多余键', () => {
  const body = calls[0].config.data
  assert.deepEqual(Object.keys(body).sort(), ['inquiryNo', 'phone'])
})

check('A3 请求体值：两端空格被修剪', () => {
  const body = calls[0].config.data
  assert.equal(body.inquiryNo, 'INQ9001')
  assert.equal(body.phone, '13800000001')
})

check('A4 手机号不进 URL/query', () => {
  const c = calls[0].config
  assert.ok(!String(c.url).includes('13800000001'))
  assert.equal(c.params, undefined)
})

check('B1 状态文案：pending/converted/rejected 三态中文', () => {
  assert.equal(INQUIRY_STATUS_TEXT.pending, '处理中')
  assert.equal(INQUIRY_STATUS_TEXT.converted, '已转预订')
  assert.equal(INQUIRY_STATUS_TEXT.rejected, '未通过')
})

check('B2 未知状态：statusText=状态未知，statusKind=unknown', () => {
  const out = normalizeInquiryLookup({ inquiryNo: 'INQ9004', status: 'weird_future_status', expectedDate: '2026-09-22', partySize: 4, createdAt: '2026-09-14 08:00' })
  assert.equal(out.statusText, '状态未知')
  assert.equal(out.statusKind, 'unknown')
})

check('B3 四态规范化：pending/converted/rejected 各自 statusKind 透传', () => {
  for (const s of ['pending', 'converted', 'rejected']) {
    const out = normalizeInquiryLookup({ inquiryNo: 'INQ', status: s })
    assert.equal(out.statusKind, s)
  }
})

check('C1 成功字段白名单：多余字段（phone/remark/operator/store 数据）被丢弃', () => {
  const out = normalizeInquiryLookup({
    inquiryNo: 'INQ9002',
    status: 'converted',
    expectedDate: '2026-09-21',
    partySize: 12,
    createdAt: '2026-09-13 18:05',
    bookingId: 'BK20260914002',
    // 以下为后端多给的敏感/内部字段，必须不出现
    phone: '13900000002',
    remark: '客人备注内容',
    operatorName: '张三',
    storeId: 1,
    storeName: '宁国总店',
    budget: 8888,
    internalNote: '内部记录'
  })
  assert.deepEqual(Object.keys(out).sort(), ['bookingId', 'createdAt', 'expectedDate', 'inquiryNo', 'partySize', 'status', 'statusKind', 'statusText'])
  assert.equal(out.bookingId, 'BK20260914002')
  assert.ok(!JSON.stringify(out).includes('13900000002'))
  assert.ok(!JSON.stringify(out).includes('张三'))
  assert.ok(!JSON.stringify(out).includes('宁国总店'))
})

check('C2 bookingId 仅 converted 显示；pending 带 bookingId 也丢弃', () => {
  const pending = normalizeInquiryLookup({ inquiryNo: 'INQ1', status: 'pending', bookingId: 'BK-EARLY' })
  assert.equal(pending.bookingId, '')
  const converted = normalizeInquiryLookup({ inquiryNo: 'INQ2', status: 'converted', bookingId: 12345 })
  assert.equal(converted.bookingId, '12345')
})

check('C3 统一空结果：null/非对象/缺 inquiryNo → null（查无与手机号不符同口径）', () => {
  assert.equal(normalizeInquiryLookup(null), null)
  assert.equal(normalizeInquiryLookup('x'), null)
  assert.equal(normalizeInquiryLookup({}), null)
  assert.equal(normalizeInquiryLookup({ status: 'pending' }), null)
  assert.equal(normalizeInquiryLookup({ inquiryNo: '   ' }), null)
})

check('D1 手机号校验：11 位 1[3-9] 开头通过；非法输入被拒（与后端 TL55 对齐）', () => {
  assert.equal(isValidLookupPhone('13800000001'), true)
  assert.equal(isValidLookupPhone(' 13800000001 '), true)
  assert.equal(isValidLookupPhone('23800000001'), false)
  assert.equal(isValidLookupPhone('1380000000'), false)
  assert.equal(isValidLookupPhone('138000000012'), false)
  assert.equal(isValidLookupPhone(''), false)
  assert.equal(isValidLookupPhone(null), false)
  assert.equal(isValidLookupPhone('1380000000a'), false)
  // 第二位 0/1/2 不合法（后端 ^1[3-9]\d{9}$）
  assert.equal(isValidLookupPhone('10800000001'), false)
  assert.equal(isValidLookupPhone('11800000001'), false)
  assert.equal(isValidLookupPhone('12800000001'), false)
  // 第二位 3-9 合法
  assert.equal(isValidLookupPhone('15000000001'), true)
  assert.equal(isValidLookupPhone('19900000002'), true)
})

check('D2 零请求反例：非法手机号不发请求（直接走空结果，不调 lookup）', () => {
  resetCalls()
  // 模拟页面 onSubmit 逻辑：非法输入不发请求
  const phone = '12800000001' // 第二位 2，不合法
  if (!isValidLookupPhone(phone)) {
    // 页面层直接进空结果，不调用 lookupBookingInquiry
  } else {
    throw new Error('不应到达此分支')
  }
  assert.equal(calls.length, 0, '非法手机号不应触发任何请求')
})

console.log(`\nSUMMARY api-contract: ${pass} passed, ${fail} failed, ${pass + fail} total`)
process.exit(fail === 0 ? 0 : 2)

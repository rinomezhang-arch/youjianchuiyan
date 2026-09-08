import test from 'node:test'
import assert from 'node:assert/strict'
import {
  LOOKUP_NOT_FOUND, LOOKUP_FIELDS,
  validateLookupInput, readLookupResult, bookingStatusText,
  validateConvertInput, readConvertResult
} from '../../src/api/publicBookingContract.js'

// ==================== 查预订：本地校验 ====================

test('两个都要填：缺哪个说哪个，好让用户知道怎么改', () => {
  assert.equal(validateLookupInput('', '').message, '请填写预订手机号与预订单号')
  assert.equal(validateLookupInput('', 'BK1').message, '请填写预订手机号')
  assert.equal(validateLookupInput('13700001111', '').message, '请填写预订单号')
})

test('手机号格式在本地就拦，省一次无谓请求', () => {
  for (const bad of ['12345', '23700001111', '1370000111a', '137000011112']) {
    assert.equal(validateLookupInput(bad, 'BK1').ok, false, bad)
  }
  const ok = validateLookupInput(' 13700001111 ', ' BK123 ')
  assert.equal(ok.ok, true)
  assert.deepEqual(ok.payload, { phone: '13700001111', bookingId: 'BK123' })
})

// ==================== 查预订：结果折叠 ====================

test('所有查不到的情况折成同一种状态与同一句话', () => {
  const misses = [
    null,
    undefined,
    { code: 200, data: null },
    { code: 200, data: undefined },
    { code: 400, message: '订单不存在' },
    { code: 403, message: '无权访问' },
    { code: 500, message: '服务端错误' }
  ]
  const seen = new Set()
  for (const response of misses) {
    const result = readLookupResult(response)
    assert.equal(result.state, 'notFound', JSON.stringify(response))
    seen.add(result.message)
  }
  assert.equal(seen.size, 1, '不同失败原因给出了可区分的文案：' + [...seen])
  assert.equal([...seen][0], LOOKUP_NOT_FOUND)
})

test('后端 message 里的细节不得泄漏到界面', () => {
  const result = readLookupResult({ code: 400, message: '该订单属于门店2的客户13900001111' })
  assert.equal(result.message, LOOKUP_NOT_FOUND)
  assert.ok(!result.message.includes('13900001111'))
  assert.ok(!result.message.includes('门店2'))
})

test('查到时只取白名单字段，后端多给的一律丢弃', () => {
  const result = readLookupResult({
    code: 200,
    data: {
      booking_id: 'BK1', booking_date: '2026-09-10', booking_time: '18:00',
      guest_count: 8, table_count: 2, booking_status: 'confirmed',
      customer_phone: '13700001111', remark: '内部备注', total_amount: '8888.88'
    }
  })
  assert.equal(result.state, 'found')
  assert.deepEqual(Object.keys(result.booking).sort(), [...LOOKUP_FIELDS].sort())
  const dumped = JSON.stringify(result.booking)
  for (const leak of ['13700001111', '内部备注', '8888.88']) {
    assert.ok(!dumped.includes(leak), '泄漏了 ' + leak)
  }
})

test('后端缺字段时补 null，不炸也不臆造', () => {
  const result = readLookupResult({ code: 200, data: { booking_id: 'BK1' } })
  assert.equal(result.state, 'found')
  assert.equal(result.booking.booking_id, 'BK1')
  assert.equal(result.booking.guest_count, null)
})

test('状态文案：认识的翻译，不认识的原样显示，空的给 --', () => {
  assert.equal(bookingStatusText('confirmed'), '已确认')
  assert.equal(bookingStatusText('cancelled'), '已取消')
  assert.equal(bookingStatusText('some_new_status'), 'some_new_status')
  assert.equal(bookingStatusText(null), '--')
})

// ==================== 转单：三者必填 ====================

test('日期、时间、桌台三者缺一不可，且绝不自动选桌', () => {
  assert.equal(validateConvertInput({}).message, '请选择预订日期')
  assert.equal(validateConvertInput({ bookingDate: '2026-09-10' }).message, '请选择到店时间')
  const noTable = validateConvertInput({ bookingDate: '2026-09-10', bookingTime: '18:00', tableIds: [] })
  assert.equal(noTable.ok, false)
  assert.ok(noTable.message.includes('不会自动分配'), noTable.message)
})

test('日期与时间格式在本地就校验', () => {
  assert.equal(validateConvertInput({ bookingDate: '2026/09/10', bookingTime: '18:00', tableIds: [1] }).ok, false)
  assert.equal(validateConvertInput({ bookingDate: '2026-09-10', bookingTime: '6pm', tableIds: [1] }).ok, false)
})

test('桌台号去重、排序、拒绝非法值', () => {
  const ok = validateConvertInput({ bookingDate: '2026-09-10', bookingTime: '18:00', tableIds: ['12', 11, '11'] })
  assert.deepEqual(ok.payload.tableIds, [11, 12])
  for (const bad of [['a'], [0], [-1], [1.5]]) {
    assert.equal(validateConvertInput({ bookingDate: '2026-09-10', bookingTime: '18:00', tableIds: bad }).ok,
      false, JSON.stringify(bad))
  }
})

// ==================== 转单：结果如实显示 ====================

test('replayed 必须如实说明是此前已转过，不能伪装成新成功', () => {
  const fresh = readConvertResult({ code: 200, data: { bookingId: 'BK9', replayed: false } })
  assert.equal(fresh.state, 'converted')
  assert.ok(fresh.message.includes('BK9'))

  const again = readConvertResult({ code: 200, data: { bookingId: 'BK9', replayed: true } })
  assert.equal(again.state, 'replayed')
  assert.equal(again.bookingId, 'BK9', '重复确认必须还是同一个单号')
  assert.ok(again.message.includes('此前已转'), again.message)
  assert.ok(again.message.includes('未重复建单'), again.message)
  assert.notEqual(again.state, fresh.state, '两者状态必须可区分，界面才能给出不同措辞')
})

test('失败与缺单号都判为 failed，不谎报成功', () => {
  assert.equal(readConvertResult({ code: 400, message: '桌台已被占用' }).state, 'failed')
  assert.equal(readConvertResult({ code: 400, message: '桌台已被占用' }).message, '桌台已被占用')
  assert.equal(readConvertResult({ code: 200, data: {} }).state, 'failed')
  assert.equal(readConvertResult(null).state, 'failed')
})

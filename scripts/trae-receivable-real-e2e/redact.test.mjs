// TR-OPS-RECEIVABLE-REAL-E2E-03 R3 定向断言：只验证 redactSecrets 输出脱敏，纯函数、零网络。
// 运行：node scripts/trae-receivable-real-e2e/redact.test.mjs
// 不启动服务、不连数据库、不重跑 E2E。
import assert from 'node:assert/strict'
import { redactSecrets } from './api-probe-03.mjs'

// 零网络守护：测试期间任何 fetch 调用都直接失败（导入探针本身受运行门控保护，不会触发 main）。
let networkCalls = 0
const originalFetch = globalThis.fetch
globalThis.fetch = async (...args) => {
  networkCalls += 1
  throw new Error('redact.test.mjs 禁止网络请求: ' + String(args[0]))
}

try {
  const input = {
    username: 'rino',
    password: 'PLAINTEXT-PASSWORD',
    nested: {
      token: 'PLAINTEXT-JWT',
      authorization: 'Bearer PLAINTEXT-BEARER',
      jwt: 'PLAINTEXT-JWT-CLAIM',
      secret: 'PLAINTEXT-SECRET',
      passwd: 'PLAINTEXT-PASSWD',
      // 普通业务字段必须保持原值，包括与弱口令形似的业务单号子串
      storeId: 1,
      totalAmount: '2000.00',
      receivableNo: 'RVABCDEF0123456789',
      businessId: 1788852951448,
      list: [{ paymentNo: 'PAY-E2E03-A1', amount: '800.00', secret: 'PLAINTEXT-IN-ARRAY' }]
    }
  }
  const snapshotBefore = JSON.stringify(input)
  const out = redactSecrets(input)

  // 1. 嵌套敏感键（含数组内）全部脱敏
  for (const path of [
    ['password'],
    ['nested', 'token'],
    ['nested', 'authorization'],
    ['nested', 'jwt'],
    ['nested', 'secret'],
    ['nested', 'passwd'],
    ['nested', 'list', 0, 'secret']
  ]) {
    let cur = out
    for (const k of path) cur = cur[k]
    assert.equal(cur, '[REDACTED]', '敏感键未脱敏: ' + path.join('.'))
  }

  // 2. 普通业务字段原值保持
  assert.equal(out.username, 'rino')
  assert.equal(out.nested.storeId, 1)
  assert.equal(out.nested.totalAmount, '2000.00')
  assert.equal(out.nested.receivableNo, 'RVABCDEF0123456789')
  assert.equal(out.nested.businessId, 1788852951448)
  assert.equal(out.nested.list[0].paymentNo, 'PAY-E2E03-A1')
  assert.equal(out.nested.list[0].amount, '800.00')

  // 3. 脱敏生成副本，原始入参不被修改
  assert.equal(JSON.stringify(input), snapshotBefore, '不得修改原始入参')
  assert.equal(input.password, 'PLAINTEXT-PASSWORD')
  assert.equal(input.nested.token, 'PLAINTEXT-JWT')

  // 4. 数组与标量
  assert.deepEqual(redactSecrets(['x', { token: 't' }, 1200, null]), ['x', { token: '[REDACTED]' }, 1200, null])
  assert.equal(redactSecrets('RVABCDEF0123456789'), 'RVABCDEF0123456789')
  assert.equal(redactSecrets(1200), 1200)

  // 5. 输出中不得出现任何明文密标
  const flat = JSON.stringify(out)
  for (const leak of ['PLAINTEXT-PASSWORD', 'PLAINTEXT-JWT', 'PLAINTEXT-BEARER', 'PLAINTEXT-SECRET', 'PLAINTEXT-PASSWD']) {
    assert.ok(!flat.includes(leak), '脱敏输出仍含明文: ' + leak)
  }

  assert.equal(networkCalls, 0, '测试过程发生了网络请求')
  console.log('PASS | redact.test: 嵌套敏感键全部[REDACTED]、业务字段原值、入参不变、零网络')
  process.exit(0)
} finally {
  globalThis.fetch = originalFetch
}

// TR-OPS-RECEIVABLE-REAL-E2E-03 真实后端 API 级证据探针（不替代浏览器 E2E，仅提供精确请求/回执/ID 证据）
// 用法：先在环境中提供必填登录口令 E2E_LOGIN_PASSWORD（无默认值、不回显、不入日志），再执行：
//   PowerShell: $env:E2E_LOGIN_PASSWORD='<隔离库测试账号口令>'; node scripts/trae-receivable-real-e2e/api-probe-03.mjs
// 前置：隔离 MySQL youjian-mysql-e2e(3307/banquet_e2e) + 集成分支后端 8080，真实 /api/auth/login。
const BASE = process.env.E2E_BASE || 'http://127.0.0.1:8080'
const LOGIN_USERNAME = process.env.E2E_USERNAME || 'rino'
// 登录口令必填环境变量：无默认值；缺失即中止；任何输出（控制台/证据 JSON）均只出现 [REDACTED]。
const LOGIN_PASSWORD = process.env.E2E_LOGIN_PASSWORD
if (!LOGIN_PASSWORD) {
  console.error('ABORT: 缺少必填环境变量 E2E_LOGIN_PASSWORD（隔离库测试账号登录口令）。探针不内置默认口令，请在环境中提供后重试。')
  process.exit(2)
}
const REDACT_KEYS = new Set(['password', 'passwd', 'token', 'authorization', 'jwt', 'secret'])
function redactSecrets(value) {
  if (Array.isArray(value)) return value.map(redactSecrets)
  if (value && typeof value === 'object') {
    const out = {}
    for (const [k, v] of Object.entries(value)) {
      out[k] = REDACT_KEYS.has(String(k).toLowerCase()) ? '[REDACTED]' : redactSecrets(v)
    }
    return out
  }
  return value
}
const results = []
function check(name, ok, detail) {
  results.push({ name, ok: !!ok, detail: detail ?? '' })
  console.log(`${ok ? 'PASS' : 'FAIL'} | ${name}${detail ? ' | ' + detail : ''}`)
}
let token, storeId
const log = []
async function call(method, path, body, note) {
  const headers = { 'Content-Type': 'application/json; charset=utf-8' }
  if (token) headers.Authorization = 'Bearer ' + token
  const startedAt = new Date().toISOString()
  let res, json, text
  try {
    res = await fetch(BASE + path, { method, headers, body: body ? JSON.stringify(body) : undefined })
    text = await res.text()
    try { json = JSON.parse(text) } catch { json = { raw: text } }
  } catch (e) {
    json = { networkError: String(e) }
    res = { status: 0 }
  }
  const entry = { at: startedAt, note, method, path, request: body ?? null, httpStatus: res.status, response: json }
  log.push(entry)
  console.log(`\n[${note}] ${method} ${path} -> HTTP ${res.status}`)
  if (body) console.log('  REQ ', JSON.stringify(body))
  console.log('  RESP', JSON.stringify(json))
  return { httpStatus: res.status, json }
}
const rid = (p) => `${p}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
const today = '2026-09-14'

try {
  // 0. 真实登录 JWT
  const login = await call('POST', '/api/auth/login', { username: LOGIN_USERNAME, password: LOGIN_PASSWORD }, '真实登录')
  token = login.json?.data?.token
  storeId = login.json?.data?.storeId
  check('登录获取JWT', !!token && storeId === 1, `storeId=${storeId}`)
  if (!token) throw new Error('no token, abort')

  // 1. 创建应收（中文名验证 UTF-8 持久化）
  const rReqId1 = rid('rv-a')
  const createA = await call('POST', '/api/finance/receivable', {
    requestId: rReqId1, storeId: 1, receivableNo: 'RV-E2E03-A', customerName: 'E2E03中文客户·张',
    totalAmount: '2000.00', receivableDate: today, remark: 'TR03真实链A'
  }, '创建应收A 2000')
  const A = createA.json?.data
  check('创建应收: code200+主键+requestId回显+replayed=false',
    createA.json?.code === 200 && A?.receivableId > 0 && A?.requestId === rReqId1 && A?.replayed === false,
    `receivableId=${A?.receivableId} no=${A?.no}`)
  check('创建应收: 回执含服务端快照(未收2000/unpaid)',
    A?.snapshot && Number(A.snapshot.total_amount) === 2000 && Number(A.snapshot.pending_amount) === 2000 &&
    Number(A.snapshot.received_amount) === 0 && A.snapshot.status === 'unpaid',
    JSON.stringify(A?.snapshot))

  // 2. 列表 & id 详情
  const list = await call('GET', '/api/finance/receivable?storeId=1', null, '应收列表')
  const inList = (list.json?.data || []).some(r => r.receivable_id === A.receivableId)
  check('列表回读包含新应收', list.json?.code === 200 && Array.isArray(list.json.data) && inList)
  const detail = await call('GET', `/api/finance/receivable?storeId=1&id=${A.receivableId}`, null, '按id查详情')
  check('详情支持id过滤且含payments数组',
    detail.json?.data?.receivable_id === A.receivableId && Array.isArray(detail.json.data.payments),
    `payments=${detail.json?.data?.payments?.length}`)

  // 3. 部分收款 800
  const pReqId1 = rid('pay-a1')
  const pay1 = await call('POST', '/api/finance/payment', {
    requestId: pReqId1, storeId: 1, paymentNo: 'PAY-E2E03-A1', paymentDate: today,
    receivableId: A.receivableId, amount: '800.00', paymentMethod: 'cash'
  }, '部分收款800')
  const P1 = pay1.json?.data
  check('部分收款: 主键+requestId', pay1.json?.code === 200 && P1?.paymentId > 0 && P1?.requestId === pReqId1)
  check('部分收款: 应收联动为 partial/已收800/待收1200',
    P1?.snapshot?.receivable?.status === 'partial' &&
    Number(P1.snapshot.receivable.received_amount) === 800 &&
    Number(P1.snapshot.receivable.pending_amount) === 1200,
    JSON.stringify(P1?.snapshot?.receivable))

  // 4. 未知结果恢复：同 requestId + 同参数重放
  const replay = await call('POST', '/api/finance/payment', {
    requestId: pReqId1, storeId: 1, paymentNo: 'PAY-E2E03-A1', paymentDate: today,
    receivableId: A.receivableId, amount: '800.00', paymentMethod: 'cash'
  }, '同requestId重放')
  check('重放: replayed=true 且返回原paymentId，不新增',
    replay.json?.data?.replayed === true && replay.json?.data?.paymentId === P1.paymentId,
    JSON.stringify(replay.json?.data))

  // 4b. 同 requestId 换参数 → 409 冲突
  const conflict = await call('POST', '/api/finance/payment', {
    requestId: pReqId1, storeId: 1, paymentNo: 'PAY-E2E03-A1', paymentDate: today,
    receivableId: A.receivableId, amount: '801.00', paymentMethod: 'cash'
  }, '同requestId改金额')
  check('同requestId换参数: 409拒绝', conflict.httpStatus === 409, `http=${conflict.httpStatus}`)

  // 5. 收清 1200
  const pReqId2 = rid('pay-a2')
  const pay2 = await call('POST', '/api/finance/payment', {
    requestId: pReqId2, storeId: 1, paymentNo: 'PAY-E2E03-A2', paymentDate: today,
    receivableId: A.receivableId, amount: '1200.00', paymentMethod: null
  }, '收清1200(paymentMethod留空)')
  const P2 = pay2.json?.data
  check('收清: status=paid/已收2000/待收0',
    P2?.snapshot?.receivable?.status === 'paid' &&
    Number(P2.snapshot.receivable.received_amount) === 2000 &&
    Number(P2.snapshot.receivable.pending_amount) === 0,
    JSON.stringify(P2?.snapshot?.receivable))

  // 6. 超额支付拒绝
  const rReqIdD = rid('rv-d')
  const createD = await call('POST', '/api/finance/receivable', {
    requestId: rReqIdD, storeId: 1, receivableNo: 'RV-E2E03-D', totalAmount: '100.00', receivableDate: today
  }, '创建应收D 100')
  const over = await call('POST', '/api/finance/payment', {
    requestId: rid('pay-d'), storeId: 1, paymentNo: 'PAY-E2E03-D1', paymentDate: today,
    receivableId: createD.json?.data?.receivableId, amount: '150.00', paymentMethod: 'cash'
  }, '超额支付150')
  check('超额支付: 非2xx拒绝(本次不记账)', over.httpStatus >= 400 && over.json?.code !== 200,
    `http=${over.httpStatus} msg=${over.json?.message || over.json?.error || ''}`)

  // 7. 资金账户：创建→收款联动余额→停用→停用户新收款拒绝
  const acc = await call('POST', '/api/finance/accounts', {
    accountName: 'E2E03停用测试户', accountType: 'cash', openingBalance: '1000.00'
  }, '创建资金账户(初始1000)')
  const accountId = acc.json?.data?.accountId
  check('账户创建返回accountId', accountId > 0, `accountId=${accountId}`)

  const rReqIdB = rid('rv-b')
  const createB = await call('POST', '/api/finance/receivable', {
    requestId: rReqIdB, storeId: 1, receivableNo: 'RV-E2E03-B', totalAmount: '500.00', receivableDate: today
  }, '创建应收B 500')
  const payAcc = await call('POST', '/api/finance/payment', {
    requestId: rid('pay-b'), storeId: 1, paymentNo: 'PAY-E2E03-B1', paymentDate: today,
    receivableId: createB.json?.data?.receivableId, amount: '200.00', paymentMethod: 'cash', accountId
  }, '启用账户收款200')
  check('启用账户收款: 200 且成功', payAcc.json?.code === 200 && payAcc.json?.data?.paymentId > 0)

  const disable = await call('PUT', `/api/finance/accounts/${accountId}`, { status: 'inactive' }, '停用账户')
  check('停用账户: status=inactive', disable.json?.data?.status === 'inactive' || disable.json?.data?.isActive === false,
    JSON.stringify(disable.json?.data))

  const rReqIdC = rid('rv-c')
  const createC = await call('POST', '/api/finance/receivable', {
    requestId: rReqIdC, storeId: 1, receivableNo: 'RV-E2E03-C', totalAmount: '300.00', receivableDate: today
  }, '创建应收C 300(停用户场景)')
  const payDisabled = await call('POST', '/api/finance/payment', {
    requestId: rid('pay-c'), storeId: 1, paymentNo: 'PAY-E2E03-C1', paymentDate: today,
    receivableId: createC.json?.data?.receivableId, amount: '100.00', paymentMethod: 'cash', accountId
  }, '停用账户新收款100')
  check('停用账户新收款: 非2xx拒绝', payDisabled.httpStatus >= 400 && payDisabled.json?.code !== 200,
    `http=${payDisabled.httpStatus} msg=${payDisabled.json?.message || payDisabled.json?.error || ''}`)
  const cDetail = await call('GET', `/api/finance/receivable?storeId=1&id=${createC.json?.data?.receivableId}`, null, 'C回读(应仍unpaid)')
  check('被拒后应收C仍 unpaid/待收300，且名下0流水',
    cDetail.json?.data?.status === 'unpaid' && Number(cDetail.json.data.pending_amount) === 300 &&
    (cDetail.json.data.payments || []).length === 0,
    JSON.stringify({ status: cDetail.json?.data?.status, payments: cDetail.json?.data?.payments?.length }))

  // 8. 刷新回读：A 仍为 paid（持久化）
  const aAfter = await call('GET', `/api/finance/receivable?storeId=1&id=${A.receivableId}`, null, '刷新回读A')
  check('刷新回读: A仍paid且两笔流水持久',
    aAfter.json?.data?.status === 'paid' && (aAfter.json.data.payments || []).length === 2,
    `payments=${aAfter.json?.data?.payments?.length}`)
} catch (e) {
  console.log('PROBE_ABORTED', String(e))
}

const pass = results.filter(r => r.ok).length
const fail = results.length - pass
console.log(`\nSUMMARY api-probe-03: ${pass} passed, ${fail} failed, ${results.length} total`)
console.log('EVIDENCE_JSON ' + JSON.stringify({ pass, fail, total: results.length, log }))
process.exit(fail === 0 ? 0 : 2)

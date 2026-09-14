import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'

const require = createRequire(new URL('../package.json', import.meta.url))

const SFC_URL = new URL('../src/views/ipad/settlement/PaySelect.vue', import.meta.url)
const API_URL = new URL('../src/api/ipad.js', import.meta.url)
const sfc = () => readFileSync(SFC_URL, 'utf8')
const apiSrc = () => readFileSync(API_URL, 'utf8')

function compiled() {
  const { parse, compileScript, compileTemplate } = require('@vue/compiler-sfc')
  const source = sfc()
  const parsed = parse(source)
  assert.deepEqual(parsed.errors, [])
  const script = compileScript(parsed.descriptor, { id: 'payselect' })
  const tpl = compileTemplate({
    source: parsed.descriptor.template.content,
    filename: 'PaySelect.vue',
    id: 'payselect',
    compilerOptions: { bindingMetadata: script.bindings },
  })
  assert.deepEqual(tpl.errors, [])
  return { source, script }
}

test('SFC 与模板编译零错误', () => {
  compiled()
})

test('api：账户列表为无参 GET，支付带 Idempotency-Key 且列表不带门店参数', () => {
  const src = apiSrc()
  assert.match(src, /export const ipadSettlementAccounts = \(\) => ipadRequest\.get\('\/settlement\/accounts'\)/)
  assert.match(src, /ipadRequest\.post\('\/settlement\/pay', data, \{\s*headers: \{ 'Idempotency-Key': idempotencyKey \}/)
  // 列表函数签名不接受 storeId/params——门店只能由后端按设备认证判定
  const listLine = src.split('\n').find(l => l.includes('ipadSettlementAccounts'))
  assert.ok(listLine && !/params|store/.test(listLine))
})

test('模板：账户未就绪时确认按钮禁用，渲染只用白名单字段', () => {
  const { source } = compiled()
  assert.match(source, /:disabled="paying \|\| !canPay"/)
  assert.match(source, /v-for="a in accounts"/)
  assert.match(source, /a\.account_name/)
  assert.match(source, /a\.account_type/)
  // 白名单之外的字段不得出现在 SFC 任何位置
  for (const banned of ['bank_account', 'account_no', 'opening_bank', 'balance', 'real_name']) {
    assert.ok(!source.includes(banned), `非白名单字段泄漏: ${banned}`)
  }
  assert.ok(!source.includes('account_id: 1'), '不得硬编码账户')
})

// ---------- 组件逻辑 vm 夹具 ----------
const memory = () => { const m = new Map(); return { getItem: k => m.get(k) ?? null, setItem: (k, v) => m.set(k, v), removeItem: k => m.delete(k) } }

function fixture(overrides = {}) {
  const vue = require('vue')
  const source = sfc().split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm, '')
  const route = vue.reactive({ params: { bookingId: 'SYN-BK-001' } })
  const pushed = []
  const ipad = vue.reactive({ storeId: 1, cartTotal: 0, clearCart: () => { pushed.push('clearCart') } })
  const storage = memory()
  storage.setItem('ipad_discount', JSON.stringify({ final_amount: 100 }))
  const calls = []
  const accountsCalls = []
  const warnings = []
  const errors = []
  const successes = []
  let accountsResp = overrides.accountsResp ?? { code: 200, data: [
    { account_id: 11, account_name: '合成微信账户', account_type: 'wechat', bank_account: '6222000000', balance: '9999.00' },
    { account_id: 12, account_name: '合成现金账户', account_type: 'cash' },
  ] }
  let payResp = overrides.payResp ?? { code: 200, data: { transaction_no: 'POS1' } }
  const sandbox = {
    ...vue,
    onMounted: fn => { sandbox.__mount = fn },
    onBeforeUnmount: fn => { sandbox.__dispose = fn },
    useRouter: () => ({ push: p => pushed.push(p), back: () => {} }),
    useRoute: () => route,
    useIpadStore: () => ipad,
    ElMessage: { success: m => successes.push(m), error: m => errors.push(m), warning: m => warnings.push(m) },
    crypto: globalThis.crypto,
    sessionStorage: storage,
    console,
    ipadSettlementAccounts: async () => { accountsCalls.push(1); return (typeof accountsResp === 'function' ? accountsResp() : accountsResp) },
    ipadSettlementPay: async (data, key) => { calls.push({ data, key }); return (typeof payResp === 'function' ? payResp() : payResp) },
    ipadBillDetail: async () => ({ code: 200, data: { final_amount: 100 } }),
  }
  const bindings = Object.keys(compiled().script.bindings)
  vm.runInNewContext(source + '\nthis.instance={' + bindings.join(',') + ',selectAccount:(id)=>{selectedAccountId.value=id}}', sandbox)
  const { parse } = require('@vue/compiler-sfc')
  const { compile } = require('@vue/compiler-dom')
  const render = new Function('Vue', compile(parse(sfc()).descriptor.template.content, { mode: 'function', prefixIdentifiers: true }).code)(vue)
  return {
    async mount() { await sandbox.__mount(); await new Promise(r => setTimeout(r, 0)) },
    i: sandbox.instance,
    calls, accountsCalls, warnings, errors, successes, pushed, ipad, route,
    render: () => render(vue.proxyRefs(sandbox.instance), []),
    setAccountsResp: r => { accountsResp = r },
    setPayResp: r => { payResp = r },
    dispose: () => sandbox.__dispose?.(),
    wait: () => new Promise(r => setTimeout(r, 5)),
  }
}

test('账户只保留白名单三字段，未选账户不能支付', async () => {
  const f = fixture(); await f.mount()
  assert.equal(f.i.accountsState.value, 'ready')
  assert.equal(f.i.accounts.value.length, 2)
  assert.deepEqual(Object.keys(f.i.accounts.value[0]).sort(), ['account_id', 'account_name', 'account_type'])
  assert.equal(f.i.canPay.value, false)
})

test('现金/微信/支付宝/银行卡/混合支付均带所选 account_id', async () => {
  const f = fixture(); await f.mount()
  for (const type of ['wechat', 'alipay', 'cash', 'card']) {
    f.i.selectMethod({ type })
    f.i.selectAccount(11)
    if (type === 'cash') f.i.cashReceived.value = 100
    assert.equal(f.i.canPay.value, true)
    await f.i.confirmPay()
    const c = f.calls[f.calls.length - 1]
    assert.equal(c.data.account_id, 11)
    assert.equal(c.data.pay_type, type)
  }
  // 混合支付
  f.i.splitPay.value = true
  f.i.splitMethods.value = [{ type: 'wechat', amount: 60 }, { type: 'cash', amount: 40 }]
  f.i.selectAccount(12)
  assert.equal(f.i.canPay.value, true)
  await f.i.confirmPay()
  const c = f.calls[f.calls.length - 1]
  assert.equal(c.data.pay_type, 'split')
  assert.equal(c.data.account_id, 12)
})

test('空列表：禁止支付并给可行动提示，不发起支付', async () => {
  const f = fixture({ accountsResp: { code: 200, data: [] } }); await f.mount()
  assert.equal(f.i.accountsState.value, 'empty')
  assert.equal(f.i.canPay.value, false)
  await f.i.confirmPay()
  assert.equal(f.calls.length, 0)
  assert.ok(f.warnings.some(w => /收款账户/.test(w)))
})

test('加载失败：禁止支付；重新加载成功后恢复', async () => {
  let attempt = 0
  const f = fixture({ accountsResp: () => {
    attempt += 1
    if (attempt === 1) throw { response: { data: { message: '网络断开' } } }
    return { code: 200, data: [{ account_id: 21, account_name: '恢复账户', account_type: 'card' }] }
  } })
  await f.mount()
  assert.equal(f.i.accountsState.value, 'error')
  assert.match(f.i.accountError.value, /网络断开/)
  assert.equal(f.i.canPay.value, false)
  await f.i.confirmPay()
  assert.equal(f.calls.length, 0)
  await f.i.loadAccounts(); await f.wait()
  assert.equal(f.i.accountsState.value, 'ready')
  assert.equal(f.i.accounts.value[0].account_id, 21)
})

test('切店：旧店账户与选择清空，按新店重新加载', async () => {
  const f = fixture(); await f.mount()
  f.i.selectAccount(11)
  assert.equal(f.i.selectedAccountId.value, 11)
  f.setAccountsResp({ code: 200, data: [{ account_id: 31, account_name: '二店账户', account_type: 'alipay' }] })
  f.ipad.storeId = 2
  await f.wait(); await f.wait()
  assert.equal(f.i.accountsState.value, 'ready')
  assert.equal(f.i.selectedAccountId.value, null)
  assert.deepEqual(f.i.accounts.value.map(a => a.account_id), [31])
})

function deferred() {
  let resolve, reject
  const promise = new Promise((ok, fail) => { resolve = ok; reject = fail })
  return { promise, resolve, reject }
}

function nodes(vnode) {
  if (!vnode || typeof vnode !== 'object') return []
  const children = Array.isArray(vnode.children) ? vnode.children : vnode.children?.default?.() || []
  return [vnode, ...children.flatMap(nodes)]
}
function byClass(f, name) {
  return nodes(f.render()).find(n => String(n.props?.class || '').split(' ').includes(name))
}
const payOutcomes = {
  success: d => d.resolve({ code: 200, data: {} }),
  business: d => d.resolve({ code: 500, message: '旧支付收款账户已停用' }),
  network: d => d.reject({ response: { data: { message: '旧支付收款账户网络错误' } } }),
}

for (const [outcome, settle] of Object.entries(payOutcomes)) {
  for (const change of ['store', 'booking', 'A-B-A', 'unmount']) {
    test(`R2 deferred ${outcome} after ${change} has no stale side effects`, async () => {
      const old = deferred()
      const f = fixture({ payResp: () => old.promise }); await f.mount(); f.i.selectAccount(11)
      const payment = f.i.confirmPay()
      if (change === 'unmount') f.dispose()
      else if (change === 'booking') f.route.params.bookingId = 'SYN-BK-002'
      else {
        f.setAccountsResp(storeTwoAccounts()); f.ipad.storeId = 2
        if (change === 'A-B-A') f.ipad.storeId = 1
      }
      await f.wait(); f.i.selectAccount(change === 'unmount' ? 11 : 31)
      const before = {
        account: f.i.selectedAccountId.value,
        accounts: JSON.stringify(f.i.accounts.value),
        key: f.i.paymentKey.value,
        loads: f.accountsCalls.length,
      }
      settle(old); await payment; await f.wait()
      assert.deepEqual(f.pushed, [], '旧响应不得清购物车或跳转')
      assert.deepEqual(f.errors, [], '旧错误不得展示')
      assert.deepEqual(f.successes, [], '旧成功不得提示')
      assert.equal(f.i.showChangeModal.value, false)
      assert.equal(f.i.selectedAccountId.value, before.account)
      assert.equal(JSON.stringify(f.i.accounts.value), before.accounts)
      assert.equal(f.i.paymentKey.value, before.key)
      assert.equal(f.accountsCalls.length, before.loads, '旧错误不得重拉账户')
    })
  }
  test(`R2 old ${outcome} finally cannot unlock a new payment`, async () => {
    const old = deferred(), current = deferred()
    const f = fixture({ payResp: () => old.promise }); await f.mount(); f.i.selectAccount(11)
    const oldPayment = f.i.confirmPay()
    f.setAccountsResp(storeTwoAccounts()); f.ipad.storeId = 2; await f.wait()
    f.i.selectAccount(31); f.setPayResp(() => current.promise)
    const newPayment = f.i.confirmPay()
    const sent = f.calls.length
    settle(old); await oldPayment
    const paying = f.i.paying.value
    current.resolve({ code: 200, data: {} }); await newPayment
    assert.equal(sent, 2, '新上下文可以发起支付')
    assert.equal(paying, true, '旧 finally 不能清新请求 paying')
    assert.deepEqual(f.errors, [])
    assert.deepEqual(f.pushed, ['clearCart', '/ipad/home'])
  })
}

test('R2 cash receipt modal uses submitted 100/200 snapshot after inputs change to 300', async () => {
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise }); await f.mount()
  f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
  f.i.cashReceived.value = 200; f.i.calcChange()
  const payment = f.i.confirmPay()
  assert.equal(f.calls[0].data.pay_amount, 200)
  // 模拟输入/外部响应式写入；显示结果仍必须来自提交快照。
  f.i.cashReceived.value = 300; f.i.payAmount.value = 50; f.i.calcChange()
  f.i.selected.value = 'wechat'
  pending.resolve({ code: 200, data: {} }); await payment
  assert.equal(f.i.showChangeModal.value, true, '支付方式也必须冻结')
  assert.equal(byClass(f, 'change-price').children, '¥100.00')
  assert.deepEqual(f.pushed, [])
})

test('R2 split pay payload is a deeply frozen independent snapshot', async () => {
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise }); await f.mount(); f.i.selectAccount(11)
  f.i.splitPay.value = true
  f.i.splitMethods.value = [{ type: 'wechat', amount: 60 }, { type: 'cash', amount: 40 }]
  const payment = f.i.confirmPay()
  const payload = f.calls[0].data
  f.i.splitMethods.value[0].amount = 300
  f.i.splitMethods.value.push({ type: 'card', amount: 20 })
  pending.resolve({ code: 200, data: {} }); await payment
  assert.deepEqual(JSON.parse(JSON.stringify(payload.pay_details)), [{ type: 'wechat', amount: 60 }, { type: 'cash', amount: 40 }])
  assert.ok(Object.isFrozen(payload) && Object.isFrozen(payload.pay_details) && Object.isFrozen(payload.pay_details[0]))
})

test('R2 request and confirmation modal lock every mutable payment control and duplicate clicks', async () => {
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise }); await f.mount()
  f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
  f.i.cashReceived.value = 200; f.i.calcChange()
  const payment = f.i.confirmPay()
  const assertLocked = () => {
    const controls = nodes(f.render()).filter(n => ['button', 'input', 'select'].includes(n.type) && n.props?.class !== 'change-done')
    assert.ok(controls.length >= 8)
    for (const control of controls) assert.equal(control.props?.disabled, true, `未锁定 ${control.props?.class}`)
    assert.equal(byClass(f, 'split-pay-toggle').props.disabled, true)
    // 检查条件显示的挂账、混合及重载控件，随后恢复现金状态。
    f.i.selected.value = 'credit'; f.i.splitPay.value = true
    f.i.splitMethods.value = [{ type: 'wechat', amount: 60 }, { type: 'cash', amount: 40 }]
    f.i.accountsState.value = 'error'
    for (const name of ['credit-input', 'split-select', 'split-amount', 'split-remove', 'add-split', 'account-retry']) {
      assert.equal(byClass(f, name).props.disabled, true, `未锁定 ${name}`)
    }
    f.i.selected.value = 'cash'; f.i.splitPay.value = false; f.i.accountsState.value = 'ready'
  }
  // 保证红测失败也会释放 deferred。
  let lockError
  try { assertLocked() } catch (e) { lockError = e }
  pending.resolve({ code: 200, data: {} }); await payment
  if (lockError) throw lockError
  assertLocked()
  await f.i.confirmPay(); await f.i.confirmPay()
  assert.equal(f.calls.length, 1, '确认弹窗期间不能再次支付')
  f.i.completePay(); f.i.completePay()
  assert.deepEqual(f.pushed, ['clearCart', '/ipad/home'])
  assert.equal(f.successes.length, 1)
})

test('R2 confirmation closed by context change cannot complete another booking', async () => {
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise }); await f.mount()
  f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
  f.i.cashReceived.value = 200; f.i.calcChange()
  const payment = f.i.confirmPay(); pending.resolve({ code: 200, data: {} }); await payment
  f.route.params.bookingId = 'SYN-BK-002'
  f.i.completePay()
  assert.equal(f.i.showChangeModal.value, false)
  assert.deepEqual(f.pushed, [])
})

for (const failure of ['network', 'service']) {
  test(`R2 unknown ${failure} result retries original received 200 and change 100 after attempted 300`, async () => {
    const first = deferred(), retry = deferred()
    const f = fixture({ payResp: () => first.promise }); await f.mount()
    f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
    f.i.cashReceived.value = 200; f.i.calcChange()
    const firstPayment = f.i.confirmPay()
    if (failure === 'network') first.reject(new Error('response lost'))
    else first.resolve({ code: 503, message: '服务暂不可用，结果未知' })
    await firstPayment
    // 保留真实断言，但先释放所有 deferred 再报告失败。
    const inputDisabled = byClass(f, 'cash-input').props.disabled
    const retryEnabled = !byClass(f, 'btn-confirm').props.disabled
    const pendingText = byClass(f, 'payment-pending')?.children
    f.i.cashReceived.value = 300; f.i.calcChange()
    f.i.selectMethod({ type: 'card' })
    f.setPayResp(() => retry.promise)
    const retryPayment = f.i.confirmPay()
    retry.resolve({ code: 200, data: {} }); await retryPayment
    assert.equal(f.calls.length, 2)
    assert.equal(f.calls[1].key, f.calls[0].key)
    assert.deepEqual(f.calls[1].data, f.calls[0].data, '同 key 必须原样重放首份载荷')
    assert.equal(f.calls[1].data.pay_amount, 200)
    assert.equal(inputDisabled, true)
    assert.equal(retryEnabled, true)
    assert.match(pendingText, /待确认/)
    assert.equal(byClass(f, 'change-price').children, '¥100.00')
    f.i.completePay()
    assert.equal(f.calls.length, 2)
    assert.deepEqual(f.pushed, ['clearCart', '/ipad/home'])
  })
}

test('R2 explicit invalid account rejection releases editing and permits a new snapshot with the same key', async () => {
  const f = fixture({ payResp: { code: 500, message: '收款账户不存在、不属于当前门店或已停用' } })
  await f.mount(); f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
  f.i.cashReceived.value = 200; f.i.calcChange()
  await f.i.confirmPay(); await f.wait()
  assert.equal(byClass(f, 'cash-input').props.disabled, false)
  f.i.selectAccount(11); f.i.cashReceived.value = 300; f.i.calcChange()
  f.setPayResp({ code: 200, data: {} }); await f.i.confirmPay()
  assert.equal(f.calls[1].data.account_id, 11)
  assert.equal(f.calls[1].data.pay_amount, 300)
  assert.equal(f.calls[0].key, f.calls[1].key)
  assert.equal(byClass(f, 'change-price').children, '¥200.00')
})

test('R2 unknown cash payment retains its key and payload across A-B-A within the component', async () => {
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise }); await f.mount()
  f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
  f.i.cashReceived.value = 200; f.i.calcChange()
  const payment = f.i.confirmPay(); pending.reject(new Error('response lost')); await payment
  f.ipad.storeId = 2; await f.wait()
  f.ipad.storeId = 1; await f.wait()
  f.i.selectAccount(12); f.i.cashReceived.value = 300; f.i.calcChange()
  f.setPayResp({ code: 200, data: {} }); await f.i.confirmPay()
  assert.equal(f.calls[1].key, f.calls[0].key)
  assert.deepEqual(f.calls[1].data, f.calls[0].data)
  assert.equal(byClass(f, 'change-price').children, '¥100.00')
})
const storeTwoAccounts = () => ({ code: 200, data: [{ account_id: 31, account_name: '二店账户', account_type: 'alipay' }] })

test('旧店成功响应晚到不能覆盖新店账户和选择', async () => {
  const old = deferred()
  const f = fixture({ accountsResp: () => old.promise }); await f.mount()
  f.setAccountsResp(storeTwoAccounts())
  f.ipad.storeId = 2
  await f.wait()
  f.i.selectAccount(31)
  old.resolve({ code: 200, data: [{ account_id: 11, account_name: '旧店', account_type: 'cash' }] })
  await f.wait()
  assert.deepEqual(f.i.accounts.value.map(a => a.account_id), [31])
  assert.equal(f.i.selectedAccountId.value, 31)
  assert.equal(f.i.accountsState.value, 'ready')
  await f.i.confirmPay()
  assert.equal(f.calls[0].data.account_id, 31)
})

test('旧店失败响应晚到不能清空新店账户或显示旧错误', async () => {
  const old = deferred()
  const f = fixture({ accountsResp: () => old.promise }); await f.mount()
  f.setAccountsResp(storeTwoAccounts())
  f.ipad.storeId = 2
  await f.wait()
  f.i.selectAccount(31)
  old.reject(new Error('旧店过期错误'))
  await f.wait()
  assert.deepEqual(f.i.accounts.value.map(a => a.account_id), [31])
  assert.equal(f.i.accountsState.value, 'ready')
  assert.equal(f.i.accountError.value, '')
  assert.equal(f.i.selectedAccountId.value, 31)
})

test('切店同一时刻即禁用旧账户支付，不能等下一次渲染', async () => {
  const f = fixture(); await f.mount(); f.i.selectAccount(11)
  const next = deferred(); f.setAccountsResp(() => next.promise)
  f.ipad.storeId = 2
  await f.i.confirmPay()
  assert.equal(f.calls.length, 0)
  assert.equal(f.i.selectedAccountId.value, null)
  next.resolve(storeTwoAccounts()); await f.wait()
})

test('卸载后未完成的账户请求不得回写组件状态', async () => {
  const pending = deferred()
  const f = fixture({ accountsResp: () => pending.promise }); await f.mount()
  f.dispose()
  pending.resolve(storeTwoAccounts()); await f.wait()
  assert.equal(f.i.accounts.value.length, 0)
  assert.equal(f.i.accountsState.value, 'loading')
})

test('幂等：业务失败与网络失败重试同一键且保留账户；成功后才换新键', async () => {
  // 业务失败 code=500 一次，随后成功
  let payAttempt = 0
  const f = fixture({ payResp: () => { payAttempt += 1; return payAttempt === 1 ? { code: 500, msg: '忙线' } : { code: 200, data: {} } } })
  await f.mount()
  f.i.selectAccount(12)
  const keyBefore = f.i.paymentKey.value
  await f.i.confirmPay()
  assert.equal(f.calls.length, 1)
  assert.equal(f.i.paymentKey.value, keyBefore, '失败后幂等键不变')
  assert.equal(f.i.selectedAccountId.value, 12, '失败后保留账户选择')
  await f.i.confirmPay()
  assert.equal(f.calls.length, 2)
  assert.equal(f.calls[0].key, f.calls[1].key, '两次请求同一幂等键')
  assert.notEqual(f.i.paymentKey.value, keyBefore, '成功后换新幂等键')
  assert.equal(f.i.selectedAccountId.value, null, '成功后清空账户选择')

  // 网络异常同样保留键
  let netAttempt = 0
  const g = fixture({ payResp: () => { netAttempt += 1; if (netAttempt === 1) { const e = new Error('network'); e.response = { data: { message: '网关超时' } }; throw e } return { code: 200, data: {} } } })
  await g.mount(); g.i.selectAccount(11)
  const gk = g.i.paymentKey.value
  await g.i.confirmPay()
  assert.equal(g.i.paymentKey.value, gk)
  await g.i.confirmPay()
  assert.equal(g.calls[0].key, g.calls[1].key)
  assert.notEqual(g.i.paymentKey.value, gk)
})

test('账户被停用：支付被拒后自动重拉账户列表并清空选择，幂等键保留', async () => {
  let payAttempt = 0
  const f = fixture({ payResp: () => {
    payAttempt += 1
    if (payAttempt === 1) {
      // 首次拒绝后，后端列表只剩另一个账户（模拟 11 刚被停用）
      f.setAccountsResp({ code: 200, data: [{ account_id: 12, account_name: '合成现金账户', account_type: 'cash' }] })
      return { code: 500, message: '收款账户不存在、不属于当前门店或已停用' }
    }
    return { code: 200, data: {} }
  } })
  await f.mount()
  assert.equal(f.accountsCalls.length, 1)
  f.i.selectAccount(11)
  const key = f.i.paymentKey.value
  await f.i.confirmPay()
  await f.wait()
  assert.equal(f.accountsCalls.length, 2, '收到账户类拒绝后必须重拉列表')
  assert.deepEqual(f.i.accounts.value.map(a => a.account_id), [12])
  assert.equal(f.i.selectedAccountId.value, null)
  assert.equal(f.i.paymentKey.value, key, '业务失败幂等键保留')
})

test('重复点击在支付中被 disabled 阻断（paying 期间不能二次发起）', async () => {
  let release
  const f = fixture({ payResp: () => new Promise(resolve => { release = resolve }) })
  await f.mount(); f.i.selectAccount(11)
  const p = f.i.confirmPay()
  assert.equal(f.i.paying.value, true)
  // 模板按钮禁用条件：paying || !canPay
  assert.equal(f.i.paying.value || !f.i.canPay.value, true)
  // 函数级重入守卫：支付中再点一次不会产生第二个请求
  await f.i.confirmPay()
  release({ code: 200, data: {} })
  await p
  assert.equal(f.calls.length, 1)
})

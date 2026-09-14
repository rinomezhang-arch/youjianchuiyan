import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import { resolve } from 'node:path'
import vm from 'node:vm'
import { execFileSync } from 'node:child_process'

// Executes the actual candidate component logic with the existing deferred fixture.
// No server, database, credentials or real payment is used.
const target = resolve(process.argv[2] || '../codex-ipad-account-recovery-60')
const frontend = resolve(target, 'frontend_v3')
const require = createRequire(resolve(frontend, 'package.json'))
const component = readFileSync(resolve(frontend, 'src/views/ipad/settlement/PaySelect.vue'), 'utf8')
// Keep the original reproduction fixture constant while the target component changes.
const testSource = execFileSync('git', ['show', 'e226289f0bc0e77fc6fbbfd74cddc159d2cdb532:frontend_v3/scripts/ipad-account-select.test.mjs'], { cwd: target, encoding: 'utf8' })
const displayRef = component.match(/class="change-price"[^\n]*?\{\{\s*([\w.]+)\.toFixed/)?.[1]
if (!displayRef) throw Error('Unable to identify the actual change modal binding')
const fixtureStart = testSource.indexOf('const memory =')
const fixtureEnd = testSource.indexOf('\ntest(', fixtureStart)
if (fixtureStart < 0 || fixtureEnd < 0) throw Error('Fixture boundary missing')
let fixtureCode = testSource.slice(fixtureStart, fixtureEnd)
fixtureCode = fixtureCode.replace('creditAccount,paymentKey', `creditAccount,calcChange,showChangeModal,${displayRef.split('.')[0]},paymentKey`)
const fixture = new Function('require', 'vm', 'sfc', fixtureCode + '\nreturn fixture')(require, vm, () => component)
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r }); return { promise, resolve } }
const results = []

{
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise })
  await f.mount(); f.i.selectAccount(11)
  const payment = f.i.confirmPay()
  f.setAccountsResp({ code: 200, data: [{ account_id: 31, account_name: 'SYN-STORE2', account_type: 'card' }] })
  f.ipad.storeId = 2; await f.wait(); f.i.selectAccount(31)
  pending.resolve({ code: 200, data: {} }); await payment
  results.push({ scenario: 'old-payment-success-after-store-switch', expected: { selected: 31, sideEffects: [] }, actual: { selected: f.i.selectedAccountId.value, sideEffects: f.pushed }, pass: f.i.selectedAccountId.value === 31 && f.pushed.length === 0 })
}
{
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise })
  await f.mount(); f.i.selectAccount(11)
  const payment = f.i.confirmPay(); f.dispose()
  pending.resolve({ code: 200, data: {} }); await payment
  results.push({ scenario: 'old-payment-success-after-unmount', expected: { sideEffects: [] }, actual: { sideEffects: f.pushed }, pass: f.pushed.length === 0 })
}
{
  const pending = deferred()
  const f = fixture({ payResp: () => pending.promise })
  await f.mount(); f.i.selectMethod({ type: 'cash' }); f.i.selectAccount(12)
  f.i.cashReceived.value = 200; f.i.calcChange()
  const payment = f.i.confirmPay()
  const submitted = f.calls[0].data.pay_amount
  f.i.cashReceived.value = 300; f.i.calcChange()
  pending.resolve({ code: 200, data: {} }); await payment
  const [displayRoot, ...displayPath] = displayRef.split('.')
  const shown = displayPath.reduce((value, key) => value[key], f.i[displayRoot].value)
  results.push({ scenario: 'cash-change-follows-submitted-snapshot', expected: { due: 100, received: submitted, change: 100 }, actual: { change: shown, modal: f.i.showChangeModal.value }, pass: shown === 100 && f.i.showChangeModal.value })
}
console.log(JSON.stringify({ scope: 'actual Vue script with deferred fake transport; not real HTTP or DB', results, pass: results.filter(r => r.pass).length, fail: results.filter(r => !r.pass).length }, null, 2))
process.exitCode = results.every(r => r.pass) ? 0 : 1

import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, mkdir, writeFile } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { fileURLToPath, pathToFileURL } from 'node:url'
import path from 'node:path'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const require = createRequire(path.resolve(root, 'frontend_v3/package.json'))
const helperSource = await readFile(path.join(root, 'frontend_v3/src/utils/restaurantPrint.js'), 'utf8')
const { printScope, printPayload, createRestaurantPrintActions } = await import('data:text/javascript;base64,' + Buffer.from(helperSource).toString('base64'))
const source = await readFile(path.join(root, 'frontend_v3/src/views/dashboard/PrintConfig.vue'), 'utf8')
const compiler = require('@vue/compiler-sfc')
const ok = data => ({ code: 200, data })
const printer = (id = 1, storeId = 2) => ({ id, storeId, name: '前台', type: 'network', paperWidth: '80', copies: 1, address: '', archive: false, connectionStatus: 'unverified' })
const rule = () => ({ id: 2, storeId: 2, name: '小票', printerId: 1, documentType: 'receipt', configuredEnabled: true, effectiveEnabled: false, archive: false })
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b }); return { promise, resolve, reject } }
function fixture() {
  const state = { sid: 2, printers: [], rules: [], ready: false, busy: '', error: '', notice: '', expected: null, uncertain: false }
  const db = { printers: [printer()], rules: [rule()] }, calls = []
  const request = {
    async get(url, options) { calls.push(['GET', url, options]); return ok(structuredClone(db[url.split('/').at(-1)])) },
    async post(url, data) { calls.push(['POST', url, structuredClone(data)]); const rows = db[url.split('/').at(-1)]; let row = rows.find(r => r.id === data.id); if (row) Object.assign(row, data); else { row = { ...data, id: 10, archive: false, connectionStatus: 'unverified' }; rows.push(row) } return ok(structuredClone(row)) }
  }
  return { state, db, calls, request, actions: createRestaurantPrintActions(state, request) }
}
test('SFC script/template/style compile and no fake device/test controls', () => {
  const { descriptor, errors } = compiler.parse(source); assert.deepEqual(errors, [])
  const script = compiler.compileScript(descriptor, { id: 'print-config' })
  assert.deepEqual(compiler.compileTemplate({ source: descriptor.template.content, filename: 'PrintConfig.vue', id: 'print-config', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.deepEqual(compiler.compileStyle({ source: descriptor.styles[0].content, id: 'print-config', scoped: true }).errors, [])
  assert.doesNotMatch(source, /testPrint|192\.168|Date\.now|localStorage\.setItem|online:/)
})
test('authenticated store scope; GM never defaults to 1', () => {
  assert.deepEqual(printScope({ user: { staffId: 8, role: 'gm' }, storeId: 0 }, null), { gm: true, sid: null })
  assert.equal(printScope({ user: { staffId: 8 }, storeId: 2 }, 9).sid, 2)
  assert.equal(printScope({ user: { staffId: 8, role: 'gm' }, storeId: 0 }, 9).sid, 9)
  assert.throws(() => printScope({}, 1))
})
test('whitelist payload strips server-only fields and validates enums/bools', () => {
  assert.deepEqual(Object.keys(printPayload('printers', printer(), 2)).sort(), ['storeId', 'id', 'name', 'type', 'paperWidth', 'copies', 'address'].sort())
  assert.deepEqual(printPayload('rules', rule(), 2, true), { id: 2, storeId: 2, archive: true })
  assert.throws(() => printPayload('rules', { ...rule(), configuredEnabled: 'true' }, 2))
  assert.throws(() => printPayload('printers', { ...printer(), copies: 6 }, 2))
  assert.throws(() => printPayload('printers', { ...printer(), paperWidth: 80 }, 2))
})
test('GET explicit scope, POST then two GETs confirm persisted data', async () => {
  const f = fixture(); assert.equal(await f.actions.load(), true)
  assert.equal(f.calls[0][2].params.storeId, 2)
  assert.equal(await f.actions.save('printers', { ...printer(), name: '修改' }), true)
  assert.equal(f.state.printers[0].name, '修改')
  assert.deepEqual(f.calls.slice(-3).map(c => c[0]), ['POST', 'GET', 'GET'])
})
test('definitive 400/403/409 retains draft and allows correction', async () => {
  for (const status of [400, 403, 409]) {
    const f = fixture(); await f.actions.load(); const draft = { ...printer(), name: '保留' }
    const post = f.request.post
    f.request.post = async () => { throw { response: { status, data: { code: status, message: '明确拒绝' } } } }
    assert.equal(await f.actions.save('printers', draft), false); assert.equal(draft.name, '保留'); assert.equal(f.state.ready, true)
    f.request.post = post; assert.equal(await f.actions.save('printers', draft), true)
  }
})
test('unknown response locks further writes while allowing reads', async () => {
  const f = fixture(); await f.actions.load(); let posts = 0
  f.request.post = async () => { posts++; throw new Error('网络未知') }
  assert.equal(await f.actions.save('printers', printer()), false); assert.equal(f.state.uncertain, true)
  await f.actions.load(); assert.equal(f.state.ready, true)
  assert.equal(await f.actions.save('printers', printer()), false); assert.equal(posts, 1)
})
test('save readback failure recovers known id without a second POST', async () => {
  const f = fixture(); await f.actions.load(); const get = f.request.get; f.request.get = async () => { throw new Error('回读失败') }
  assert.equal(await f.actions.save('printers', { ...printer(), name: '回读' }), false); assert.ok(f.state.expected)
  f.request.get = get; assert.equal(await f.actions.load(), true); assert.equal(f.state.expected, null)
  assert.equal(f.calls.filter(c => c[0] === 'POST').length, 1)
})
test('confirmation and POST are mutually exclusive, cancel does not write', async () => {
  const f = fixture(); await f.actions.load(); const gate = deferred()
  const first = f.actions.save('printers', printer(), true, () => gate.promise)
  assert.equal(await f.actions.save('printers', printer()), false)
  gate.resolve(false); assert.equal(await first, false); assert.equal(f.calls.filter(c => c[0] === 'POST').length, 0)
})
test('archive remains in history and cannot be edited; rule references restricted', async () => {
  const f = fixture(); await f.actions.load()
  assert.equal(await f.actions.save('printers', printer(), true), true); assert.equal(f.state.printers[0].archive, true)
  assert.equal(await f.actions.save('printers', f.state.printers[0]), false)
  assert.equal(await f.actions.save('rules', rule()), false)
})
test('scope change invalidates pending GET and prevents old rows from returning', async () => {
  const f = fixture(); const gate = deferred(); f.request.get = () => gate.promise
  const old = f.actions.load(); f.actions.invalidate(3); gate.resolve(ok([printer()])); await old
  assert.equal(f.state.sid, 3); assert.deepEqual(f.state.printers, []); assert.equal(f.state.ready, false)
})
test('scope change during confirmation sends no POST', async () => {
  const f = fixture(); await f.actions.load(); const gate = deferred()
  const old = f.actions.save('printers', printer(), true, () => gate.promise)
  f.actions.invalidate(3); gate.resolve(true); assert.equal(await old, false)
  assert.equal(f.calls.filter(c => c[0] === 'POST').length, 0)
})
test('mismatched response scope and unready writes rejected', async () => {
  const f = fixture(); assert.equal(await f.actions.save('printers', printer()), false)
  f.db.printers[0].storeId = 99; assert.equal(await f.actions.load(), false); assert.equal(f.state.ready, false)
  assert.equal(f.calls.filter(c => c[0] === 'POST').length, 0)
})

// Opt-in real Chromium component fixture. Synthetic API only; no JWT/device/business E2E.
if (process.env.PRINT_CONFIG_BROWSER === '1') test('isolated Chromium fixture: desktop/390, save/readback, correction, archive, GM and race', async () => {
  const out = path.resolve(root, '../../../docs/协作/Codex/print-config-ui/browser-fixture')
  await mkdir(out, { recursive: true })
  await writeFile(path.join(out, 'index.html'), '<html><meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1"><div id="app"></div><script type="module" src="/main.js"></script></html>')
  await writeFile(path.join(out, 'main.js'), `import {createApp} from 'vue'; import ElementPlus from 'element-plus'; import 'element-plus/dist/index.css'; import Component from '@component'; createApp(Component).use(ElementPlus).mount('#app');`)
  await writeFile(path.join(out, 'store.js'), `import {reactive} from 'vue'; const store=reactive({storeId:0,token:'fixture'}); window.fixtureStore=store; export const useUserStore=()=>store;`)
  await writeFile(path.join(out, 'request.js'), `async function call(method,url,data){const res=await fetch('/api'+url+(method==='GET'&&data?.params?'?'+new URLSearchParams(data.params):''),{method,headers:{'Content-Type':'application/json'},body:method==='POST'?JSON.stringify(data):undefined});const body=await res.json();if(!res.ok){const e=new Error(body.message);e.response={status:res.status,data:body};throw e}return body}export default {get:(url,p)=>call('GET',url,p),post:(url,p)=>call('POST',url,p)};`)
  const { createServer } = await import(pathToFileURL(path.join(path.dirname(require.resolve('vite')), 'dist/node/index.js')).href)
  const vuePlugin = require('@vitejs/plugin-vue').default
  const deps = path.resolve(root, '../../../frontend_v3/node_modules')
  const server = await createServer({ configFile: false, root: out, plugins: [vuePlugin()], resolve: { alias: [
    { find: '@component', replacement: path.join(root, 'frontend_v3/src/views/dashboard/PrintConfig.vue') },
    { find: '@/utils/restaurantPrint', replacement: path.join(root, 'frontend_v3/src/utils/restaurantPrint.js') },
    { find: '@/utils/request', replacement: path.join(out, 'request.js') }, { find: '@/store/user', replacement: path.join(out, 'store.js') },
    { find: /^vue$/, replacement: path.join(deps, 'vue/dist/vue.runtime.esm-bundler.js') },
    { find: 'element-plus', replacement: path.join(deps, 'element-plus') }
  ] }, server: { host: '127.0.0.1', port: 0, fs: { allow: [root, out, deps] } }, cacheDir: path.join(out, '.vite') })
  const { chromium } = require('C:/Users/rinom/AppData/Local/npm-cache/_npx/e41f203b7505f1fb/node_modules/playwright-core')
  let browser; const results = []
  try {
    await server.listen(); const origin = server.resolvedUrls.local[0]
    browser = await chromium.launch({ headless: true, executablePath: 'C:/Users/rinom/AppData/Local/ms-playwright/chromium-1228/chrome-win64/chrome.exe' })
    for (const width of [1440, 390]) {
      const context = await browser.newContext({ viewport: { width, height: 900 } }); const page = await context.newPage()
      const errors = []; page.on('pageerror', e => errors.push(e.message))
      const db = { printers: [], rules: [] }; let posts = 0, rejectNext = false, delay = false
      await context.route('**/*', async route => {
        const url = new URL(route.request().url())
        if (url.origin !== new URL(origin).origin) return route.abort()
        if (!url.pathname.startsWith('/api/')) return route.continue()
        if (url.pathname === '/api/auth/me') return route.fulfill({ json: ok({ user: { staffId: 7, role: 'gm' }, storeId: 0 }) })
        const kind = url.pathname.split('/').at(-1)
        if (!['printers', 'rules'].includes(kind)) throw new Error('Unexpected fixture endpoint')
        if (route.request().method() === 'GET') return route.fulfill({ json: ok(db[kind].filter(row => row.storeId === Number(url.searchParams.get('storeId')))) })
        posts++; const payload = route.request().postDataJSON()
        assert.equal(payload.storeId, 2); assert.equal(payload.connectionStatus, undefined); assert.equal(payload.effectiveEnabled, undefined)
        if (rejectNext) { rejectNext = false; return route.fulfill({ status: 409, json: { code: 409, message: '合成冲突，请纠正名称' } }) }
        if (delay) await new Promise(resolve => setTimeout(resolve, 250))
        let row = db[kind].find(r => r.id === payload.id)
        if (row) Object.assign(row, payload); else { row = { ...payload, id: 10 + posts, archive: false, connectionStatus: 'unverified', effectiveEnabled: false }; db[kind].push(row) }
        return route.fulfill({ json: ok(row) })
      })
      await page.goto(origin); await page.getByText('与当前门店选择联动').waitFor()
      assert.equal(await page.getByRole('button', { name: '添加打印机', exact: true }).isDisabled(), true)
      await page.locator('#print-store').fill('2'); await page.locator('#print-store').press('Tab')
      await page.getByRole('button', { name: '添加打印机', exact: true }).click()
      await page.getByRole('dialog').getByLabel('名称', { exact: true }).fill('合成前台')
      rejectNext = true; await page.getByRole('button', { name: '保存并回读' }).click()
      await page.getByRole('dialog').getByText('合成冲突，请纠正名称').waitFor()
      assert.equal(await page.getByRole('dialog').getByLabel('名称', { exact: true }).inputValue(), '合成前台')
      delay = true; await page.getByRole('button', { name: '保存并回读' }).click()
      assert.equal(await page.getByRole('button', { name: '保存并回读' }).isDisabled(), true)
      await page.getByRole('dialog').waitFor({ state: 'hidden' }); assert.equal(posts, 2)
      await page.getByText('连接未验证', { exact: true }).waitFor()
      await page.getByRole('button', { name: '添加规则', exact: true }).click()
      const dialog = page.getByRole('dialog'); await dialog.getByLabel('名称', { exact: true }).fill('合成小票')
      await dialog.locator('.el-select').first().click(); await page.getByRole('option', { name: '合成前台', exact: true }).click()
      await page.getByRole('button', { name: '保存并回读' }).click(); await dialog.waitFor({ state: 'hidden' })
      await page.getByText('尚未生效', { exact: true }).waitFor()
      await page.screenshot({ path: path.join(out, `print-config-${width}.png`), fullPage: true, animations: 'disabled' })
      const geometry = await page.evaluate(() => ({ viewport: innerWidth, scroll: document.documentElement.scrollWidth, buttons: [...document.querySelectorAll('.print-config-page button')].filter(b => b.textContent.includes('添加')).map(b => ({ text: b.textContent, right: b.getBoundingClientRect().right })) }))
      assert.ok(geometry.scroll <= width); assert.ok(geometry.buttons.every(b => b.right <= width))
      await page.getByRole('button', { name: '编辑打印机', exact: true }).click()
      await page.screenshot({ path: path.join(out, `print-dialog-${width}.png`), fullPage: true, animations: 'disabled' })
      const bounds = await page.getByRole('dialog').boundingBox(); assert.ok(bounds.x >= 0 && bounds.x + bounds.width <= width)
      await page.getByRole('dialog').getByRole('button', { name: '关闭', exact: true }).click()
      await page.getByRole('button', { name: '归档规则', exact: true }).click(); await page.getByRole('button', { name: '归档', exact: true }).click()
      await page.getByText('已归档并回读，历史记录保留。').waitFor()
      await page.getByText('显示归档历史', { exact: true }).click(); await page.getByText('已归档', { exact: true }).waitFor()
      assert.equal(await page.getByRole('button', { name: '编辑规则', exact: true }).isDisabled(), true)
      await page.evaluate(() => { window.fixtureStore.storeId = 3 })
      await page.getByText('暂无打印机配置', { exact: true }).waitFor(); assert.equal(await page.getByText('合成前台', { exact: true }).count(), 0)
      const beforeDenied = posts
      await context.route('**/api/auth/me*', route => route.fulfill({ status: 403, json: { code: 403, message: '合成未授权' } }))
      await page.reload(); await page.getByText('合成未授权', { exact: true }).waitFor()
      assert.equal(await page.getByRole('button', { name: '添加打印机', exact: true }).isDisabled(), true)
      assert.equal(posts, beforeDenied)
      assert.deepEqual(errors, []); results.push({ width, geometry, posts, unauthorizedNoWrite: true, passed: true }); await context.close()
    }
    await writeFile(path.join(out, 'results.json'), JSON.stringify({ type: 'real Chromium / synthetic API component fixture; not JWT or device E2E', results }, null, 2))
  } finally { await browser?.close(); await server.close() }
})

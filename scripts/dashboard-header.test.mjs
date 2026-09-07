import test from 'node:test'
import assert from 'node:assert/strict'
import { readFile, writeFile, mkdir, readdir, stat } from 'node:fs/promises'
import { createRequire } from 'node:module'
import { fileURLToPath } from 'node:url'
import path from 'node:path'
import { createServer } from 'node:http'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const solo = path.resolve(root, '../../..')
const require = createRequire(path.join(root, 'frontend_v3/package.json'))
const identitySource = await readFile(path.join(root, 'frontend_v3/src/utils/dashboardIdentity.js'), 'utf8')
const { dashboardIdentity, dashboardStoreLabel } = await import('data:text/javascript;base64,' + Buffer.from(identitySource).toString('base64'))
const source = await readFile(path.join(root, 'frontend_v3/src/views/Dashboard.vue'), 'utf8')
test('auth/me nested Chinese identity and position', () => {
  assert.deepEqual(dashboardIdentity({ user: { staffName: '张小秋', position: '值班经理', role: 'store_manager' }, storeId: 1 }), { staffName: '张小秋', staffPosition: '值班经理' })
})
test('selected store label uses menu mapping, not identity affiliation', () => {
  assert.equal(dashboardStoreLabel(2, '2', '总部'), '宣城店')
  assert.equal(dashboardStoreLabel(1, '1', '总部'), '宁国店')
  assert.equal(dashboardStoreLabel(1, '0', ''), '请选择门店')
  assert.equal(dashboardStoreLabel(9, '9', '总部'), '门店 9')
})
test('legacy login top-level userInfo and role fallback', () => {
  assert.deepEqual(dashboardIdentity({ staffName: '李明', staffPosition: '前厅主管', role: 'staff' }), { staffName: '李明', staffPosition: '前厅主管' })
  assert.equal(dashboardIdentity({ staffName: '李明', role: '店长' }).staffPosition, '店长')
})
test('nested identity does not mix stale outer person; invalid values stay honest', () => {
  assert.deepEqual(dashboardIdentity({ staffName: '旧用户', role: 'gm', user: { staffName: ' 新用户 ' } }), { staffName: '新用户', staffPosition: '未提供岗位' })
  assert.deepEqual(dashboardIdentity(null), { staffName: '未提供姓名', staffPosition: '未提供岗位' })
  assert.equal(dashboardIdentity({ staffName: { html: 'bad' } }).staffName, '未提供姓名')
})
test('display projection does not mutate data or expose authorization properties', () => {
  const original = Object.freeze({ staffName: '张三', role: 'gm', staffId: 99, storeId: 0 })
  const result = dashboardIdentity(original)
  assert.deepEqual(Object.keys(result), ['staffName', 'staffPosition'])
  assert.equal(original.role, 'gm'); assert.equal(result.storeId, undefined)
})
test('Dashboard SFC script/template/style compile, projection only used for header display', () => {
  const compiler = require('@vue/compiler-sfc'); const { descriptor, errors } = compiler.parse(source)
  assert.deepEqual(errors, [])
  const script = compiler.compileScript(descriptor, { id: 'header' })
  assert.deepEqual(compiler.compileTemplate({ source: descriptor.template.content, filename: 'Dashboard.vue', id: 'header', compilerOptions: { bindingMetadata: script.bindings } }).errors, [])
  assert.deepEqual(compiler.compileStyle({ source: descriptor.styles[0].content, id: 'header', scoped: true }).errors, [])
  assert.equal((source.match(/userInfo\./g) || []).length, 4)
  assert.match(source, /computed\(\(\) => dashboardIdentity\(userStore.userInfo\)\)/)
})
if (process.env.HEADER_BROWSER === '1') test('full build Chromium390/1440: geometry, Chinese identity, store/user menus, Reports/PrintConfig input', async () => {
  const out = path.join(solo, 'artifacts/header-current'); const build = path.join(out, 'build-store-label')
  await mkdir(out, { recursive: true })
  const evidence = path.join(out, 'store-label-evidence'); await mkdir(evidence, { recursive: true })
  // Reuse existing full-shell report/config scenarios without modifying their business sources.
  const child = await promisify(execFile)(process.execPath, [path.join(solo, 'scripts/report-browser-check.mjs'), '--root', out, '--build', build, '--suite', 'current', '--commit', 'header-working-tree-snapshot'], { maxBuffer: 1024 * 1024 })
  await writeFile(path.join(evidence, 'browser-regression.log'), child.stdout + child.stderr)
  const latest = (await readdir(out)).filter(n => n.startsWith('run-')).sort().at(-1)
  const report = JSON.parse(await readFile(path.join(out, latest, 'results.json'), 'utf8'))
  assert.deepEqual(report.pageErrors, [])
  assert.ok(report.results.every(r => r.status === 'pass'), JSON.stringify(report.results.filter(r => r.status !== 'pass')))
  const overlap = (a, b) => Math.min(a.right, b.right) - Math.max(a.left, b.left) > 1 && Math.min(a.bottom, b.bottom) - Math.max(a.top, b.top) > 1
  for (const r of report.results.filter(r => r.header)) {
    assert.equal(r.header.length, 3)
    for (const h of r.header) assert.ok(h.left >= 0 && h.right <= r.viewport && h.top >= 0, JSON.stringify(h))
    for (let i = 0; i < 3; i++) for (let j = i + 1; j < 3; j++) assert.equal(overlap(r.header[i], r.header[j]), false, r.name)
    assert.match(r.header[2].text, /合成测试用户/); assert.match(r.header[2].text, /合成岗位/)
  }
  const server = createServer(async (req, res) => {
    try {
      let file = path.resolve(build, '.' + new URL(req.url, 'http://localhost').pathname)
      if (!file.startsWith(build + path.sep)) file = path.join(build, 'index.html')
      try { if ((await stat(file)).isDirectory()) file = path.join(build, 'index.html') } catch { file = path.join(build, 'index.html') }
      res.setHeader('Content-Type', ({ '.js': 'text/javascript', '.css': 'text/css', '.html': 'text/html', '.png': 'image/png', '.svg': 'image/svg+xml' })[path.extname(file)] || 'application/octet-stream')
      res.end(await readFile(file))
    } catch { res.writeHead(500); res.end('fixture error') }
  })
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve)); assert.notEqual(server.address().port, 5174)
  const base = `http://127.0.0.1:${server.address().port}`
  const { chromium } = require('C:/Users/rinom/AppData/Local/npm-cache/_npx/e41f203b7505f1fb/node_modules/playwright-core')
  let browser; const checks = []
  try {
    browser = await chromium.launch({ headless: true })
    for (const width of [390, 1440]) {
      const context = await browser.newContext({ viewport: { width, height: 1000 }, serviceWorkers: 'block' }); const page = await context.newPage()
      await context.addInitScript(() => { if (!localStorage.getItem('token')) { localStorage.setItem('token', 'synthetic'); localStorage.setItem('storeId', '1') } })
      await context.route('**/*', async route => {
        const u = new URL(route.request().url()); if (u.origin !== base) return route.abort()
        if (!u.pathname.startsWith('/api/')) return route.continue()
        const data = u.pathname === '/api/auth/me' ? { user: { staffId: 71, staffName: '张小秋', role: 'gm', position: '值班经理' }, storeId: 0, storeName: '合成总部' } : []
        if (route.request().method() !== 'GET') throw new Error('Unexpected write in header menu checks')
        await route.fulfill({ json: { code: 200, data } })
      })
      await page.goto(base + '/dashboard/print-config', { waitUntil: 'networkidle' })
      await page.locator('.header .user-name').getByText('张小秋', { exact: true }).waitFor()
      await page.locator('.header .user-role').getByText('值班经理', { exact: true }).waitFor()
      await page.locator('.header .store-badge').click()
      await Promise.all([page.waitForNavigation({ waitUntil: 'networkidle' }), page.getByRole('menuitem', { name: '宣城店', exact: true }).click()])
            await page.screenshot({ path: path.join(evidence, `menu-selection-${width}.png`), fullPage: true, animations: 'disabled' })
      await writeFile(path.join(evidence, `menu-selection-${width}.json`), JSON.stringify(await page.evaluate(() => ({ badge: document.querySelector('.header .store-badge').textContent, selected: localStorage.getItem('storeId'), toasts: [...document.querySelectorAll('.el-message')].map(e => e.textContent) }))))
      assert.equal(await page.evaluate(() => localStorage.getItem('storeId')), '2')
      await page.waitForFunction(() => document.querySelector('.header .store-badge').textContent.trim() === '宣城店')
      await page.waitForFunction(() => document.querySelector('#print-store')?.value === '2')
      await page.locator('.header .avatar').click()
      await page.getByRole('menuitem').filter({ hasText: '退出' }).waitFor()
      await page.keyboard.press('Escape')
      const geometry = await page.locator('.header').evaluate(el => {
        const fields = [...el.querySelectorAll('.header-left,.header-center,.header-right,.store-badge,.user-name,.user-role,.avatar,.chat-btn,.refresh-btn')]
        return fields.map(e => { const r = e.getBoundingClientRect(); const hit = document.elementFromPoint(r.left + r.width / 2, r.top + r.height / 2); return { class: e.className, text: e.textContent.trim(), left: r.left, right: r.right, top: r.top, bottom: r.bottom, visibleAtCenter: !!hit && (e === hit || e.contains(hit)) } })
      })
      assert.ok(geometry.every(g => g.left >= 0 && g.right <= width && g.top >= 0 && g.visibleAtCenter), JSON.stringify(geometry))
      await page.screenshot({ path: path.join(evidence, `header-${width}.png`), fullPage: true, animations: 'disabled' })
      checks.push({ width, geometry, persistedStoreId: '2', requestedStore: '宣城店', displayedStore: await page.locator('.header .store-badge').innerText(), identity: '张小秋 / 值班经理', menuOpened: true })
      await context.close()
    }
    await writeFile(path.join(evidence, 'header-results.json'), JSON.stringify({ type: 'full-build Chromium / synthetic API, not JWT business E2E', base, regression: latest, checks }, null, 2))
  } finally { await browser?.close(); await new Promise(resolve => server.close(resolve)) }
})

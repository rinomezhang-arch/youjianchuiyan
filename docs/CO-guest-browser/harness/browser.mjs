import {createRequire} from 'node:module'
import {writeFile} from 'node:fs/promises'
import path from 'node:path'
import {fileURLToPath} from 'node:url'
import assert from 'node:assert/strict'
const dir=path.dirname(fileURLToPath(import.meta.url)),evidence=path.dirname(dir),workspace=path.resolve(evidence,'../..')
const frontend=path.resolve(process.env.GUEST_FRONTEND_DEPS_DIR || path.join(workspace,'frontend_v3'))
const deps=path.join(frontend,'node_modules'),require=createRequire(path.join(frontend,'package.json'))
let createServer,vuePlugin,chromium
try {
 ({createServer}=require('vite'));vuePlugin=require('@vitejs/plugin-vue')
 const playwright=process.env.GUEST_PLAYWRIGHT_MODULE
 ;({chromium}=playwright ? require(path.resolve(playwright)) : require('playwright'))
} catch {throw new Error('Browser dependencies missing: install frontend dependencies in frontend_v3 or set GUEST_FRONTEND_DEPS_DIR; install playwright there or set GUEST_PLAYWRIGHT_MODULE to its module path.')}
if(!process.env.GUEST_TEST_PORT || !process.env.GUEST_TEST_PASSWORD)throw new Error('Run through opt-in GuestBrowserE2eTest; server/credential environment missing.')
let server,browser
const result={checks:[],receipts:[],fixtureClosure:true}
try {
 server=await createServer({configFile:false,root:dir,cacheDir:path.join(dir,'.vite'),plugins:[vuePlugin.default()],resolve:{alias:{'@':path.join(workspace,'frontend_v3/src'),'vue':path.join(deps,'vue/dist/vue.runtime.esm-bundler.js'),'element-plus':path.join(deps,'element-plus'),'axios':path.join(deps,'axios'),'pinia':path.join(deps,'pinia'),'vue-router':path.join(deps,'vue-router')}},server:{host:'127.0.0.1',port:0,fs:{allow:[workspace,frontend]},proxy:{'/api':`http://127.0.0.1:${process.env.GUEST_TEST_PORT}`,'/__fixture':`http://127.0.0.1:${process.env.GUEST_TEST_PORT}`}}})
 await server.listen();const origin=`http://127.0.0.1:${server.httpServer.address().port}`
 browser=await chromium.launch({headless:true,...(process.env.GUEST_CHROMIUM_EXECUTABLE ? {executablePath:process.env.GUEST_CHROMIUM_EXECUTABLE} : {})});const context=await browser.newContext(),page=await context.newPage();page.setDefaultTimeout(18000)
 let writes=0;page.on('request',r=>{if(r.method()==='POST'&&r.url().includes('/order/add-dishes'))writes++})
 async function auth(label){await page.getByPlaceholder('请输入服务员账号').fill('SYN11');await page.getByPlaceholder('请输入密码').fill(process.env.GUEST_TEST_PASSWORD);await page.getByRole('button',{name:label,exact:true}).click()}
 async function view(){if(!await page.locator('.auth-box').isVisible())await page.getByRole('button',{name:'授权查看订单',exact:true}).click();const detail=page.waitForResponse(r=>r.url().includes('/order/detail?'));await auth('授权查看');const r=await detail;assert.equal(r.status(),200);return (await r.json()).data}
 async function add(){await page.locator('.dish-card').filter({hasText:'Synthetic Decimal'}).locator('.quick-add').click();await page.locator('.cart-fab').click();await page.getByRole('button',{name:'服务员授权提交',exact:true}).click()}
 await page.goto(origin+'/guest/SYN-BOOK');await page.locator('.dish-card').filter({hasText:'Synthetic Decimal'}).waitFor();const initial=await view();assert.equal(initial.total_amount,'50.98');result.checks.push('real view password + device scope + nonempty order')
 await add();const first=page.waitForResponse(r=>r.url().includes('/order/add-dishes'));await auth('授权并提交');const receipt=(await (await first).json()).data;assert.equal(receipt.status,'committed');assert.equal(receipt.added_amount,12.34);result.receipts.push(receipt)
 await page.getByText('该批已提交，订单已重新读取',{exact:true}).waitFor();result.checks.push('actual Vue decimal menu -> password batch grant -> real receipt -> detail')
 await page.reload();await view();await add();const before=writes
 await page.evaluate(()=>{window.savedSetItem=Storage.prototype.setItem;Storage.prototype.setItem=function(k,v){if(k.startsWith('guest-order:submission:'))throw new Error('synthetic storage failure');return window.savedSetItem.call(this,k,v)}})
 await auth('授权并提交');await page.locator('.auth-error').filter({hasText:'无法保存'}).waitFor();assert.equal(writes,before);result.checks.push('journal storage failure blocks batch POST')
 await page.evaluate(()=>{Storage.prototype.setItem=window.savedSetItem});await page.reload();await view();
 let dropped
 await page.route('**/api/ipad/order/add-dishes',async route=>{const response=await route.fetch();const body=await response.json();assert.equal(body.code,200);dropped=body.data;await route.abort('failed')})
 await add();await auth('授权并提交');await page.waitForFunction(()=>document.body.textContent.includes('未确认'))
 assert.ok(dropped);result.receipts.push(dropped)
 const pending=await page.evaluate(()=>{const k=Object.keys(localStorage).find(k=>k.startsWith('guest-order:submission:'));return k?JSON.parse(localStorage.getItem(k)):null});assert.equal(pending.client_request_id,dropped.client_request_id)
 await page.unroute('**/api/ipad/order/add-dishes')
 const closed=await page.evaluate(async secret=>{const r=await fetch('/__fixture/close',{method:'POST',headers:{'X-Fixture-Key':secret}});return r.json()},process.env.GUEST_TEST_PASSWORD);assert.equal(closed.fixture_closed,true)
 const atRecovery=writes;await page.reload();const recovered=await view();assert.equal(recovered.payment_status,'paid');assert.equal(recovered.booking_status,'confirmed');assert.deepEqual(recovered.submission,dropped)
 await page.getByText('该批已提交，订单已重新读取',{exact:true}).waitFor();assert.equal(writes,atRecovery);assert.equal(await page.locator('.quick-add:not([disabled])').count(),0)
 assert.equal(await page.evaluate(()=>Object.keys(localStorage).filter(k=>k.startsWith('guest-order:submission:')).length),0)
 result.checks.push('server commit then response lost -> fixture closes -> reload new view authorization -> GET same key receipt, no POST')
 const denied=await page.evaluate(async()=>{const headers={'X-Client-Type':'ipad','X-Device-Sn':'SYN-A','X-Store-Id':'1','X-Staff-Id':'0'};const a=await fetch('/api/ipad/order/detail?booking_id=SYN-BOOK',{headers});const b=await fetch('/api/ipad/order/detail?booking_id=SYN-BOOK',{headers:{...headers,'X-Store-Id':'2'}});return [a.status,b.status]});assert.deepEqual(denied,[403,403]);result.checks.push('no view token and cross-store header HTTP403')
 assert.equal(writes,2);result.batchPostCount=writes
 await page.screenshot({path:path.join(evidence,'guest-recovered.png'),fullPage:true});await context.close()
 await writeFile(path.join(evidence,'browser-result.json'),JSON.stringify(result,null,2))
} catch(e) {await writeFile(path.join(evidence,'partial-result.json'),JSON.stringify(result,null,2));console.error(String(e?.message||'browser failed').replaceAll(process.env.GUEST_TEST_PASSWORD||'__NO_SECRET__','[redacted]'));process.exitCode=1}
finally {if(browser)await browser.close();if(server)await server.close()}

import {createRequire} from 'node:module'
import {writeFile} from 'node:fs/promises'
import path from 'node:path'
import {fileURLToPath} from 'node:url'
import assert from 'node:assert/strict'
const dir=path.dirname(fileURLToPath(import.meta.url)),evidence=path.dirname(dir),workspace=path.resolve(evidence,'../../../..')
const frontend=process.env.PAYABLE_TEST_FRONTEND || path.join(workspace,'frontend_v3')
const require=createRequire(path.join(frontend,'package.json'))
const {createServer}=require('vite'),vuePlugin=require('@vitejs/plugin-vue')
const {chromium}=require(process.env.PLAYWRIGHT_MODULE || 'playwright-core')
const deps=path.join(frontend,'node_modules')
let server,browser
const result={base:process.env.PAYABLE_TEST_BASE || 'current-worktree',checks:[],receipts:[]}
try{
 server=await createServer({configFile:false,root:dir,cacheDir:path.join(dir,'.vite'),plugins:[vuePlugin.default()],
 resolve:{alias:{'@':path.join(workspace,'frontend_v3/src'),'vue':path.join(deps,'vue/dist/vue.runtime.esm-bundler.js'),'element-plus':path.join(deps,'element-plus'),'axios':path.join(deps,'axios')}},
 server:{host:'127.0.0.1',port:0,fs:{allow:[workspace,frontend]},proxy:{'/api':`http://127.0.0.1:${process.env.PAYABLE_TEST_PORT}`}}})
 await server.listen();const origin=`http://127.0.0.1:${server.httpServer.address().port}`
 browser=await chromium.launch({headless:true})
 const context=await browser.newContext(),page=await context.newPage();page.setDefaultTimeout(20000)
 async function login(account){await page.goto(origin);await page.getByLabel('合成账号').fill(account);await page.getByLabel('密码',{exact:true}).fill(process.env.PAYABLE_TEST_PASSWORD);await page.getByRole('button',{name:'登录',exact:true}).click();await page.getByRole('heading',{name:'供应商应付与结算'}).waitFor()}
 async function create(supplier,amount){await page.getByRole('button',{name:'手工新增应付',exact:true}).click();const dialog=page.getByRole('dialog');await dialog.locator('input').nth(0).fill(supplier);await dialog.locator('input').nth(1).fill(amount);await dialog.locator('input').nth(2).fill('SYN isolated browser');await dialog.getByRole('button',{name:'保存应付',exact:true}).click()}
 const receiptOf=d=>({requestId:d.requestId,payableId:d.payableId,payableNo:d.payableNo,storeId:d.storeId,totalAmount:d.totalAmount,replayed:d.replayed})
 await login('SYN-BROWSER-STAFF');const first=page.waitForResponse(r=>r.url().includes('/api/finance/payables')&&r.request().method()==='POST');await create('SYN normal','12.50');const firstData=(await (await first).json()).data;result.receipts.push(receiptOf(firstData));await page.getByRole('cell',{name:'SYN normal',exact:true}).waitFor();result.checks.push('actual Vue create -> real HTTP -> list')
 let dropped
 await page.route('**/api/finance/payables',async route=>{if(route.request().method()==='POST'&&!dropped){const response=await route.fetch();const body=await response.json();assert.equal(response.status(),200);assert.equal(body.code,200);dropped=body.data;await route.abort('failed')}else await route.continue()})
 await create('SYN dropped','23.75');await page.getByText('新增应付', {exact:false}).first().waitFor();
 await page.waitForFunction(()=>!!sessionStorage.getItem('payable-create-pending:8001:1')&&document.body.textContent.includes('结果待核对'))
 await page.unroute('**/api/finance/payables');assert.ok(dropped?.payableId)
 const pending=await page.evaluate(()=>JSON.parse(sessionStorage.getItem('payable-create-pending:8001:1')))
 assert.equal(pending.payload.requestId,dropped.requestId);result.receipts.push(receiptOf(dropped))
 // Refresh while first real list is delayed. Select the real paid filter so the unpaid
 // original is not visible; then exercise explicit replay rather than auto list reconciliation.
 let release,held=false;const gate=new Promise(r=>release=r)
 await page.route('**/api/finance/payables?*',async route=>{if(!held){held=true;await gate}await route.continue()})
 await page.reload({waitUntil:'domcontentloaded'});await page.locator('.query-bar .el-select').click();await page.getByRole('option',{name:'已结清',exact:true}).click();release()
 await page.getByRole('button',{name:'查询',exact:true}).click();await page.getByRole('button',{name:'恢复原请求（原金额、原供应商）',exact:true}).waitFor();
 await page.waitForFunction(()=>{const b=[...document.querySelectorAll('button')].find(b=>b.textContent.includes('恢复原请求'));return b&&!b.disabled})
 await page.unroute('**/api/finance/payables?*')
 const conflict=await page.evaluate(async p=>{const r=await fetch('/api/finance/payables',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+localStorage.getItem('token')},body:JSON.stringify({...p,totalAmount:'23.76'})});return {status:r.status,body:await r.json()}},pending.payload)
 assert.equal(conflict.status,409);assert.equal(conflict.body.code,409);result.checks.push('real same-key changed-amount HTTP409')
 assert.equal((await page.evaluate(()=>JSON.parse(sessionStorage.getItem('payable-create-pending:8001:1')))).payload.requestId,pending.payload.requestId)
 const replayResponse=page.waitForResponse(r=>r.url().includes('/api/finance/payables')&&r.request().method()==='POST')
 await page.getByRole('button',{name:'恢复原请求（原金额、原供应商）',exact:true}).click();const replay=await replayResponse
 assert.deepEqual(replay.request().postDataJSON(),pending.payload);const data=(await replay.json()).data
 assert.equal(data.replayed,true);assert.equal(data.payableId,dropped.payableId);result.receipts.push(receiptOf(data));result.restoredId=data.payableId;result.restoredRequestId=data.requestId
 await page.locator('.query-bar .el-select').click();await page.getByRole('option',{name:'未付',exact:true}).click();await page.getByRole('button',{name:'查询',exact:true}).click()
 await page.getByRole('cell',{name:'SYN dropped',exact:true}).waitFor();await page.waitForFunction(()=>!sessionStorage.getItem('payable-create-pending:8001:1'))
 result.checks.push('real committed response discarded -> refresh -> same payload replay same ID -> unique list clears journal')
 await page.screenshot({path:path.join(evidence,'staff-ledger.png'),fullPage:true})
 const foreign=await page.evaluate(async()=>{const r=await fetch('/api/finance/payables',{method:'POST',headers:{'Content-Type':'application/json',Authorization:'Bearer '+localStorage.getItem('token')},body:JSON.stringify({requestId:crypto.randomUUID(),storeId:2,supplierName:'SYN forbidden',totalAmount:'9.00'})});return {status:r.status,code:(await r.json()).code}})
 assert.equal(foreign.status,403);assert.equal(foreign.code,403);result.checks.push('actual authenticated store1 cannot create store2')
 const unauth=await page.evaluate(async()=>{const r=await fetch('/api/finance/payables',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({requestId:crypto.randomUUID(),storeId:1,totalAmount:'9.00'})});return r.status});assert.equal(unauth,401)
 await context.close()
 const gmContext=await browser.newContext();const gm=await gmContext.newPage();let gmWrites=0;gm.on('request',r=>{if(r.method()==='POST'&&r.url().includes('/api/finance/payables'))gmWrites++})
 await gm.goto(origin);await gm.getByLabel('合成账号').fill('SYN-BROWSER-GM');await gm.getByLabel('密码',{exact:true}).fill(process.env.PAYABLE_TEST_PASSWORD);await gm.getByRole('button',{name:'登录',exact:true}).click();await gm.getByRole('heading',{name:'供应商应付与结算'}).waitFor()
 await gm.getByText('请确认登录身份并选择有效门店',{exact:true}).waitFor();assert.equal(await gm.getByRole('button',{name:'手工新增应付',exact:true}).isDisabled(),true);assert.equal(gmWrites,0)
 assert.equal(await gm.locator('.query-bar input').first().inputValue(),'');result.checks.push('real GM login store0: unselected and zero write requests')
 await gm.screenshot({path:path.join(evidence,'gm-unselected.png'),fullPage:true});await gmContext.close()
 await writeFile(path.join(evidence,'browser-result.json'),JSON.stringify(result,null,2));console.log('BROWSER_CHECKS='+result.checks.length)
}catch(e){console.error('BROWSER_FAILURE '+String(e.message).replaceAll(process.env.PAYABLE_TEST_PASSWORD||'__none__','[redacted]'));process.exitCode=1}
finally{if(browser)await browser.close();if(server)await server.close()}

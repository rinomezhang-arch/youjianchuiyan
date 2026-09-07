import { test } from 'node:test'
import assert from 'node:assert/strict'
import { createRequire } from 'node:module'
import { readFileSync } from 'node:fs'
import vm from 'node:vm'
const require = createRequire('F:/solo/frontend_v3/package.json')
const compiler=require('@vue/compiler-sfc'),vue=require('vue')
const filename=new URL('../../../../frontend_v3/src/views/ipad/order/GuestOrder.vue',import.meta.url)
const parsed=compiler.parse(readFileSync(filename,'utf8')),script=compiler.compileScript(parsed.descriptor,{id:'ipad-auth2'})
function app(verify,submit) {
 const box={ref:vue.ref,computed:vue.computed,onMounted(){},useRoute:()=>({params:{bookingId:'SYN-BOOK'}}),
 ipadAuthVerify:verify,ipadOrderAddDishes:submit,ipadOrderDetail:async()=>({code:200,data:{dishes:[]}}),
 ipadDishList:async()=>({code:200,data:[]}),ipadDishSearch:async()=>({code:200,data:[]}),ElMessage:{success(){}},setTimeout(){},console}
 vm.createContext(box)
 vm.runInContext(script.content.replace(/import[\s\S]*?from\s*['"][^'"]+['"];?/g,'').replace('export default','const component =')+'\nglobalThis.component=component',box)
 const ui=box.component.setup({},{expose(){}})
 ui.authForm.value={username:'SYN11',password:'synthetic-password'};ui.cart.value=[{dish_id:'SYN-DISH',qty:2}]
 return ui
}
const grant={code:200,data:{authorization_token:'synthetic-opaque',booking_id:'SYN-BOOK',purpose:'ipad:batch-add',expires_in:120}}
test('verify binds order; batch sends capability not staff; completion clears password and capability',async()=>{
 let count=0
 const ui=app(async p=>{assert.equal(p.booking_id,'SYN-BOOK');return grant},async p=>{count++;assert.equal(p.authorization_token,'synthetic-opaque');assert.equal('staff_id' in p,false);return {code:200,data:{added_dishes:1,added_amount:25}}})
 await ui.handleAuth();assert.equal(count,1);assert.equal(ui.verifiedStaff.value,null);assert.equal(ui.authForm.value.password,'');assert.equal(ui.cart.value.length,0)
})
test('malformed/wrong order/wrong purpose verify receipt never submits',async()=>{
 for(const data of [{},{...grant.data,booking_id:'OTHER'},{...grant.data,purpose:'pc-login'}]) {
 let sends=0;const ui=app(async()=>({code:200,data}),async()=>{sends++});await ui.handleAuth();assert.equal(sends,0);assert.equal(ui.authForm.value.password,'')
 }
})
test('double click shares one authorization flow; timeout retains cart and warns to reconcile',async()=>{
 let finish,calls=0;const ui=app(()=>{calls++;return new Promise(r=>{finish=r})},async()=>{throw Error('timeout')})
 const first=ui.handleAuth();await ui.handleAuth();assert.equal(calls,1);finish(grant);await first
 assert.equal(ui.cart.value.length,1);assert.equal(ui.verifiedStaff.value,null);assert.match(ui.successMsg.value,/先核对订单/)
})
test('guest SFC script/template/style compiles',()=>{
 assert.deepEqual(parsed.errors,[])
 assert.deepEqual(compiler.compileTemplate({source:parsed.descriptor.template.content,filename:filename.pathname,id:'ipad-auth2',compilerOptions:{bindingMetadata:script.bindings}}).errors,[])
 assert.deepEqual(compiler.compileStyle({source:parsed.descriptor.styles[0].content,filename:filename.pathname,id:'ipad-auth2',scoped:true}).errors,[])
})

import test from 'node:test'
import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import vm from 'node:vm'
import { journalKey, createJournal, readJournal, saveJournal, clearJournal, assertReceipt, receiptRowsMatch, batchFailureMessage } from '../src/utils/guestOrderJournal.js'
const scope = {store_id:'1',device_sn:'fixture-device',booking_id:'fixture-booking'}
const entry = () => createJournal(scope,[{dish_id:'D1',dish_quantity:2}],'fixture-request-0001')
const memory = () => {const map=new Map();return {getItem:k=>map.get(k)??null,setItem:(k,v)=>map.set(k,v),removeItem:k=>map.delete(k)}}
test('journal persists exact original quantities, stable key; no token or password',()=>{
 const store=memory(),value=entry();saveJournal(store,value);assert.deepEqual(readJournal(store,scope),value)
 assert.deepEqual(Object.keys(value).sort(),['client_request_id','dishes','scope','state','version'])
 assert.deepEqual(value.dishes,[{dish_id:'D1',dish_quantity:2}]);assert.ok(!JSON.stringify(value).includes('token'))
})
test('scope isolates store/device/booking',()=>{
 for(const changed of [{store_id:'2'},{device_sn:'other'},{booking_id:'other'}])assert.notEqual(journalKey(scope),journalKey({...scope,...changed}))
})
test('invalid quantities and duplicate dishes rejected',()=>{
 for(const q of [0,100,1.5,NaN])assert.throws(()=>createJournal(scope,[{dish_id:'D1',dish_quantity:q}],'fixture-request-0001'))
 assert.throws(()=>createJournal(scope,[{dish_id:'D1',dish_quantity:1},{dish_id:'D1',dish_quantity:1}],'fixture-request-0001'))
})
test('corrupted record retained',()=>{
 const store=memory();store.setItem(journalKey(scope),'broken');assert.throws(()=>readJournal(store,scope),/损坏/);assert.equal(store.getItem(journalKey(scope)),'broken')
})
test('secret/unknown fields not admitted',()=>{
 const store=memory();store.setItem(journalKey(scope),JSON.stringify({...entry(),authorization_token:'do-not-store'}));assert.throws(()=>readJournal(store,scope))
})
test('write and readback failures fail closed',()=>{
 assert.throws(()=>saveJournal({getItem:()=>null,setItem:()=>{throw Error('quota')}},entry()))
 assert.throws(()=>saveJournal({getItem:()=>null,setItem:()=>{}},entry()),/未发送/)
})
test('existing key and payload cannot be replaced',()=>{
 const store=memory();saveJournal(store,entry());assert.throws(()=>saveJournal(store,{...entry(),client_request_id:'fixture-request-0002'}))
 assert.throws(()=>saveJournal(store,{...entry(),dishes:[{dish_id:'D1',dish_quantity:3}]}))
})
const receipt = () => ({client_request_id:'fixture-request-0001',booking_id:scope.booking_id,status:'committed',added_dishes:1,added_quantity:2,added_amount:40.98,dish_booking_ids:[10]})
test('only exact committed key/booking receipt accepted',()=>{
 assert.equal(assertReceipt(receipt(),entry()).status,'committed')
 for(const wrong of [{client_request_id:'other'},{booking_id:'other'},{status:'pending'},{added_dishes:2},{added_quantity:3},{added_amount:null},{added_amount:'40.98'},{dish_booking_ids:[]},{dish_booking_ids:['10']}])assert.throws(()=>assertReceipt({...receipt(),...wrong},entry()))
})
test('Maxwell v1 key bound, canonical IDs and 409 reasons remain distinct',()=>{
 assert.throws(()=>createJournal(scope,[{dish_id:'D1',dish_quantity:1}],'a'.repeat(101)))
 assert.equal(createJournal(scope,[{dish_id:' D1 ',dish_quantity:1}],'a'.repeat(100)).dishes[0].dish_id,'D1')
 const messages = ['request_conflict','booking_closed','dish_unavailable','dish_price_invalid'].map(error_code=>batchFailureMessage({code:409,data:{...receipt(),error_code}},entry()))
 assert.equal(new Set(messages).size,4)
 assert.match(batchFailureMessage({code:409,data:{...receipt(),booking_id:'wrong',error_code:'booking_closed'}},entry()),/未确认/)
})
test('committed marker survives reload then clears only matching key',()=>{
 const store=memory();saveJournal(store,entry());saveJournal(store,{...entry(),state:'committed'});assert.equal(readJournal(store,scope).state,'committed')
 assert.throws(()=>clearJournal(store,{...entry(),client_request_id:'other'}));clearJournal(store,entry());assert.equal(readJournal(store,scope),null)
})
test('SFC and request contract: view token header, no URL or persistence',()=>{
 const require=createRequire(new URL('../package.json',import.meta.url)),{parse,compileScript,compileTemplate}=require('@vue/compiler-sfc')
 const source=readFileSync(new URL('../src/views/ipad/order/GuestOrder.vue',import.meta.url),'utf8')
 const parsed=parse(source);assert.deepEqual(parsed.errors,[]);const script=compileScript(parsed.descriptor,{id:'guest'})
 assert.deepEqual(compileTemplate({source:parsed.descriptor.template.content,filename:'GuestOrder.vue',id:'guest',compilerOptions:{bindingMetadata:script.bindings}}).errors,[])
 const api=readFileSync(new URL('../src/api/ipad.js',import.meta.url),'utf8')
 assert.match(api,/headers: \{ 'X-Order-View-Token': viewToken \}/);assert.match(api,/params: \{ booking_id: bookingId,/)
 assert.match(api,/requestId \? \{ client_request_id: requestId \} : \{\}/)
 assert.ok(!source.includes('customer_name'));assert.ok(!source.includes('sessionStorage.setItem'));assert.ok(!source.includes('console.error'))
})
test('API interceptor rejects stale store and keeps guest 401 on recovery page',async()=>{
 const api=readFileSync(new URL('../src/api/ipad.js',import.meta.url),'utf8')
 let requestHook,responseError
 const ipad={storeId:1,staffId:88,deviceSn:'fixture-device'}
 const client={interceptors:{request:{use:fn=>requestHook=fn},response:{use:(_ok,fail)=>responseError=fail}}}
 const sandbox={axios:{create:()=>client},useIpadStore:()=>ipad,window:{location:{href:'/ipad/guest-order/fixture-booking'}}}
 vm.runInNewContext(api.replace(/^import .*$/gm,'').replace(/export const /g,'const ').replace('export default ipadRequest',''),sandbox)
 await assert.rejects(requestHook({headers:{},guestOrderScope:{...scope,store_id:'2'}}),/切换/)
 const config=requestHook({headers:{},guestOrderRecovery:true,guestOrderScope:scope})
 assert.equal(config.headers['X-Staff-Id'],0)
 await assert.rejects(responseError({response:{status:401},config:{guestOrderRecovery:true}}))
 assert.equal(sandbox.window.location.href,'/ipad/guest-order/fixture-booking')
})

function componentFixture(overrides = {}) {
 const require=createRequire(new URL('../package.json',import.meta.url)),vue=require('vue')
 const source=readFileSync(new URL('../src/views/ipad/order/GuestOrder.vue',import.meta.url),'utf8').split('<script setup>')[1].split('</script>')[0].replace(/^import .*$/gm,'')
 const route=vue.reactive({params:{bookingId:scope.booking_id}}),ipad=vue.reactive({storeId:1,deviceSn:scope.device_sn}),storage=memory(),calls=[]
 const detail={booking_id:scope.booking_id,store_id:1,booking_status:'confirmed',read_only:true,tables:[],dishes:[],total_amount:'0.00',amount_basis:'active_dish_subtotal'}
 const sandbox={...vue,onMounted:()=>{},onBeforeUnmount:()=>{},useRoute:()=>route,useIpadStore:()=>ipad,localStorage:storage,console,
  setTimeout:()=>1,clearTimeout:()=>{},ElMessage:{success:()=>{},warning:()=>{}},
  createJournal,readJournal,saveJournal,clearJournal,assertReceipt,receiptRowsMatch,batchFailureMessage,
  ipadDishList:async()=>({code:200,data:[{dish_id:'D1',dish_name:'合成鱼',sale_price:'20.49'}]}),
  ipadDishSearch:async()=>({code:200,data:[]}),
  ipadOrderViewAuthorize:async()=>({code:200,data:{order_view_token:'V'.repeat(43),expires_in:1800,booking_id:scope.booking_id,purpose:'ipad:order-view'}}),
  ipadOrderDetail:async(_id,_token,_scope,requestId)=>({code:200,data:{...detail,...(requestId?{submission:null}:{})}}),
  ipadAuthVerify:async()=>({code:200,data:{authorization_token:'fixture-token',booking_id:scope.booking_id,purpose:'ipad:batch-add'}}),
  ipadOrderAddDishes:async body=>{calls.push(body);return {code:200,data:{...receipt(),client_request_id:body.client_request_id}}},...overrides}
 vm.runInNewContext(source+'\nthis.instance={initializeScope,handleAuth,openSubmit,authForm,authMode,orderReady,canEditCart,cart,journal,notice,orderDetail}',sandbox)
 sandbox.instance.initializeScope()
 return {...sandbox,storage,calls,route}
}
const credentials = instance => { instance.authForm.value={username:'synthetic',password:'synthetic'} }
test('final component: read_only view still allows independently authorized add',async()=>{
 const fixture=componentFixture(),i=fixture.instance;credentials(i);await i.handleAuth()
 assert.equal(i.orderReady.value,true);assert.equal(i.canEditCart.value,true)
 i.cart.value=[{dish_id:'D1',sale_price:'20.49',qty:2}];i.openSubmit();credentials(i);await i.handleAuth()
 assert.equal(fixture.calls.length,1);assert.equal(i.cart.value.length,0);assert.equal(i.journal.value,null)
 assert.equal(fixture.storage.getItem(journalKey(scope)),null)
})
test('final component: keyed HTTP409 preserves original journal with exact reason',async()=>{
 const fixture=componentFixture({ipadOrderAddDishes:async body=>{throw {response:{status:409,data:{code:409,data:{error_code:'request_conflict',booking_id:body.booking_id,client_request_id:body.client_request_id}}}}}})
 const i=fixture.instance;credentials(i);await i.handleAuth();i.cart.value=[{dish_id:'D1',sale_price:'20.49',qty:2}];i.openSubmit();credentials(i);await i.handleAuth()
 assert.match(i.notice.value,/数量冲突/);assert.equal(i.journal.value.state,'pending');assert.equal(i.cart.value.length,1);assert.equal(i.canEditCart.value,false)
})
test('final component: wrong receipt cannot clear persisted original request',async()=>{
 const fixture=componentFixture({ipadOrderAddDishes:async()=>({code:200,data:{...receipt(),booking_id:'OTHER'}})})
 const i=fixture.instance;credentials(i);await i.handleAuth();i.cart.value=[{dish_id:'D1',sale_price:'20.49',qty:2}];i.openSubmit();credentials(i);await i.handleAuth()
 assert.equal(i.journal.value.state,'pending');assert.ok(fixture.storage.getItem(journalKey(scope)));assert.equal(i.cart.value.length,1)
})
test('final component: late view grant after booking switch is ignored',async()=>{
 let release;const fixture=componentFixture({ipadOrderViewAuthorize:()=>new Promise(resolve=>release=resolve)}),i=fixture.instance
 credentials(i);const pending=i.handleAuth();fixture.route.params.bookingId='OTHER'
 release({code:200,data:{order_view_token:'V'.repeat(43),expires_in:1800,booking_id:scope.booking_id,purpose:'ipad:order-view'}});await pending
 assert.equal(i.orderReady.value,false);assert.equal(i.authMode.value,'view')
})
test('final component: failed view renewal clears earlier read permission',async()=>{
 let attempt=0;const fixture=componentFixture({ipadOrderViewAuthorize:async()=>++attempt===1?{code:200,data:{order_view_token:'V'.repeat(43),expires_in:1800,booking_id:scope.booking_id,purpose:'ipad:order-view'}}:{code:403,message:'授权失败'}})
 const i=fixture.instance;credentials(i);await i.handleAuth();assert.equal(i.orderReady.value,true)
 credentials(i);await i.handleAuth();assert.equal(i.orderReady.value,false);assert.equal(i.canEditCart.value,false)
 assert.equal(Object.keys(i.orderDetail.value).length,0)
})

const readData = (submission, status = 'completed', rows = true) => ({booking_id:scope.booking_id,store_id:1,booking_status:status,read_only:true,tables:[],
 dishes:rows?[{dish_booking_id:10,dish_id:'D1',dish_name:'合成鱼',dish_quantity:2,unit_price:'20.49',subtotal:'40.98',kitchen_status:'pending'}]:[],
 total_amount:rows?'40.98':'0.00',amount_basis:'active_dish_subtotal',submission})
async function restoredFixture(data) {
 const reads=[]
 const fixture=componentFixture({ipadOrderDetail:async(id,_token,_scope,requestId)=>{reads.push({id,requestId});return {code:200,data}}})
 saveJournal(fixture.storage,entry());fixture.instance.initializeScope();credentials(fixture.instance);await fixture.instance.handleAuth()
 return {...fixture,reads}
}
test('closed order: exact read submission recovers pending batch without POST',async()=>{
 const fixture=await restoredFixture(readData(receipt())),i=fixture.instance
 assert.equal(i.journal.value,null);assert.equal(fixture.storage.getItem(journalKey(scope)),null);assert.equal(i.cart.value.length,0)
 assert.equal(fixture.calls.length,0);assert.equal(i.canEditCart.value,false);assert.equal(fixture.reads[0].requestId,entry().client_request_id)
 assert.match(i.notice.value,/已提交/)
})
test('closed order: null read submission remains unknown, repeated check never POSTs',async()=>{
 const fixture=await restoredFixture(readData(null)),i=fixture.instance
 assert.equal(i.journal.value.state,'pending');await i.openSubmit()
 assert.equal(fixture.reads.length,2);assert.equal(fixture.calls.length,0);assert.equal(i.authMode.value,'view');assert.match(i.notice.value,/仅可重新读取/)
})
test('normal order: null submission first checked again, then independent auth may replay same key',async()=>{
 const fixture=await restoredFixture(readData(null,'confirmed')),i=fixture.instance
 assert.equal(fixture.calls.length,0);await i.openSubmit();assert.equal(fixture.reads.length,2);assert.equal(i.authMode.value,'add')
 credentials(i);await i.handleAuth();assert.equal(fixture.calls.length,1);assert.equal(fixture.calls[0].client_request_id,entry().client_request_id)
})
test('paid order with active booking status only rereads a pending batch',async()=>{
 const fixture=await restoredFixture({...readData(null,'confirmed'),payment_status:'paid'}),i=fixture.instance
 assert.equal(i.canEditCart.value,false);assert.equal(i.journal.value.state,'pending')
 await i.openSubmit();assert.equal(fixture.reads.length,2);assert.equal(fixture.calls.length,0)
 assert.equal(i.authMode.value,'view');assert.match(i.notice.value,/仅可重新读取/)
})
test('read receipt mismatching journal key/booking stays blocked',async()=>{
 for(const wrong of [{client_request_id:'other'},{booking_id:'OTHER'}]){
  const fixture=await restoredFixture(readData({...receipt(),...wrong}));assert.equal(fixture.instance.journal.value.state,'pending')
  assert.equal(fixture.instance.orderReady.value,false);assert.equal(fixture.calls.length,0)
 }
})
test('read receipt without matching original row IDs/dish/qty retains review record',async()=>{
 const data=readData(receipt());data.dishes[0].dish_id='OTHER'
 const fixture=await restoredFixture(data),i=fixture.instance
 assert.match(i.notice.value,/快照暂无法核对/);assert.equal(i.journal.value.state,'pending');await i.openSubmit();assert.equal(fixture.calls.length,0)
 assert.equal(receiptRowsMatch(receipt(),entry(),[]),false)
})
test('read submission absent is not null or proof of no submission',async()=>{
 const data=readData(null);delete data.submission
 const fixture=await restoredFixture(data);assert.equal(fixture.instance.orderReady.value,false);assert.equal(fixture.instance.journal.value.state,'pending')
 assert.match(fixture.instance.notice.value,/查询暂不可用/)
})

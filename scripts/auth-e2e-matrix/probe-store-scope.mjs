#!/usr/bin/env node
/** DL-AUTH-E2E-MATRIX-15 深探：门店归属/越店（只读） */
const BASE = 'http://127.0.0.1:18080';
const PASS = process.env.MX_PASS;
if (!PASS) throw new Error('缺少 MX_PASS：密码必须运行时注入');
async function login(u){const r=await fetch(BASE+'/api/auth/login',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({username:u,password:PASS})});const j=await r.json();return j?.data?.token;}
async function get(p,t){const r=await fetch(BASE+p,{headers:{Authorization:'Bearer '+t}});let j=null;try{j=await r.json()}catch{};return {status:r.status,code:j?.code,rows:(j?.data||[]).map(x=>({id:x.account_id,store:x.store_id,name:x.account_name}))};}
const t1=await login('mx_mgr1'), t2=await login('mx_mgr2'), tg=await login('mx_gm');
console.log('mgr1(store1) ?storeId=1 ->', JSON.stringify(await get('/api/finance/account?storeId=1',t1)));
console.log('mgr1(store1) ?storeId=2 ->', JSON.stringify(await get('/api/finance/account?storeId=2',t1)));
console.log('mgr1(store1) 无storeId  ->', JSON.stringify(await get('/api/finance/account',t1)));
console.log('mgr2(store2) ?storeId=1 ->', JSON.stringify(await get('/api/finance/account?storeId=1',t2)));
console.log('mgr2(store2) 无storeId  ->', JSON.stringify(await get('/api/finance/account',t2)));
console.log('gm  ?storeId=1 ->', JSON.stringify(await get('/api/finance/account?storeId=1',tg)));
console.log('gm  ?storeId=2 ->', JSON.stringify(await get('/api/finance/account?storeId=2',tg)));
console.log('gm  无storeId  ->', JSON.stringify(await get('/api/finance/account',tg)));

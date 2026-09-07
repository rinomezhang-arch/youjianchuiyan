const fs = require('node:fs');
const vm = require('node:vm');
const assert = require('node:assert/strict');
const path = require('node:path');
const file = path.join(__dirname, '../frontend_v3/src/views/dashboard/StockTake.vue');
const source = fs.readFileSync(file, 'utf8').match(/<script setup>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*$/gm, '');

function setup() {
  const confirmations = [];
  const posts = [];
  const user = { storeId: 1 };
  let finishPost;
  const context = vm.createContext({
    ref: value => ({ value }), computed: fn => ({ get value() { return fn(); } }),
    onMounted() {}, watch() {}, useUserStore: () => user,
    ElMessage: { info() {}, success() {}, error() {}, warning() {} },
    ElMessageBox: { confirm: () => new Promise(resolve => confirmations.push(resolve)) },
    request: {
      get: async () => ({ data: [] }),
      post: (url, data) => {
        posts.push({ url, data });
        return new Promise(resolve => { finishPost = () => resolve({ data: { takeNo: 'SYN-COUNT' } }); });
      }
    }, console
  });
  vm.runInContext(source + '\n globalThis.api = {submitStockTake,list,stockTaking};', context);
  context.api.list.value = [{ ingredientId: 'SYN-COUNT', actualQuantity: 2, diffQty: 0, diffAmount: 0 }];
  context.api.stockTaking.value = true;
  return { api: context.api, user, posts, confirmations, finish: () => finishPost() };
}

(async () => {
  const a = setup();
  const first = a.api.submitStockTake();
  const second = a.api.submitStockTake();
  assert.equal(a.confirmations.length, 2);
  a.confirmations[0]();
  await new Promise(setImmediate);
  assert.equal(a.posts.length, 1);
  a.confirmations[1]();
  await new Promise(setImmediate);
  assert.equal(a.posts.length, 1, 'Concurrent confirmation must not create a second document');
  a.finish();
  await Promise.all([first, second]);

  const b = setup();
  const switched = b.api.submitStockTake();
  b.user.storeId = 2;
  b.confirmations[0]();
  await switched;
  assert.equal(b.posts.length, 0, 'Changing store while confirming must cancel submission');

  const c = setup();
  const cancelled = c.api.submitStockTake();
  c.api.stockTaking.value = false;
  c.confirmations[0]();
  await cancelled;
  assert.equal(c.posts.length, 0, 'Cancelling the count must invalidate its open confirmation');
  console.log('PASS: concurrent confirmation, store switch, cancelled count (component script, mocked transport; not browser E2E)');
})().catch(e => { console.error(e); process.exitCode = 1; });

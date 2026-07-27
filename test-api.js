const BASE_URL = 'http://youjianchuiyan.com/api';

async function login() {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: 'rino', password: '002323' })
  });
  const data = await res.json();
  return data.data?.token || data.token;
}

async function testApi(name, method, path, body = null, token) {
  const url = `${BASE_URL}${path}`;
  const headers = {
    'Content-Type': 'application/json',
    'X-Token': token,
    'X-Store-Id': '1'
  };
  
  const options = { method, headers };
  if (body) options.body = JSON.stringify(body);
  
  const startTime = Date.now();
  try {
    const res = await fetch(url, options);
    const duration = Date.now() - startTime;
    let data;
    try {
      data = await res.json();
    } catch {
      data = null;
    }
    
    let status = '❓';
    let statusCode = res.status;
    if (res.status >= 200 && res.status < 300) {
      status = '✅';
    } else if (res.status === 500) {
      status = '❌';
    } else if (res.status >= 400 && res.status < 500) {
      status = '⚠️';
    }
    
    const errorMsg = data?.message || data?.error || (data && typeof data === 'string' ? data : '') || '';
    
    console.log(`${status} ${statusCode} | ${method.padEnd(6)} ${path.padEnd(60)} | ${duration}ms${errorMsg ? ' | ' + errorMsg.substring(0, 80) : ''}`);
    
    return { name, method, path, statusCode, status, duration, errorMsg, data };
  } catch (err) {
    const duration = Date.now() - startTime;
    console.log(`💥 ERR | ${method.padEnd(6)} ${path.padEnd(60)} | ${duration}ms | ${err.message}`);
    return { name, method, path, statusCode: 0, status: '💥', duration, errorMsg: err.message, data: null };
  }
}

async function main() {
  console.log('🔐 登录中...');
  const token = await login();
  if (!token) {
    console.error('❌ 登录失败，无法获取token');
    process.exit(1);
  }
  console.log('✅ 登录成功\n');
  
  const results = [];
  
  console.log('━'.repeat(100));
  console.log('📊 Dashboard模块');
  console.log('━'.repeat(100));
  results.push(await testApi('今日数据', 'GET', '/dashboard/today?storeId=1', null, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('📦 Package模块');
  console.log('━'.repeat(100));
  results.push(await testApi('套餐列表', 'GET', '/packages?storeId=1', null, token));
  results.push(await testApi('套餐详情', 'GET', '/packages/1?storeId=1', null, token));
  results.push(await testApi('创建套餐', 'POST', '/packages', { storeId: '1', packageId: 'TEST001', packageName: '测试套餐', price: 99 }, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('🏭 Supplier模块');
  console.log('━'.repeat(100));
  results.push(await testApi('供应商列表', 'GET', '/menu-api/suppliers?storeId=1', null, token));
  results.push(await testApi('供应商详情', 'GET', '/menu-api/suppliers/1?storeId=1', null, token));
  results.push(await testApi('创建供应商', 'POST', '/menu-api/suppliers', { supplierId: 'S001', storeId: '1', supplierName: '测试供应商', contactPerson: '张三', phone: '13800138000' }, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('🥬 Ingredient模块');
  console.log('━'.repeat(100));
  results.push(await testApi('食材列表', 'GET', '/menu-api/ingredients?storeId=1', null, token));
  results.push(await testApi('食材详情', 'GET', '/menu-api/ingredients/1?storeId=1', null, token));
  results.push(await testApi('低库存食材', 'GET', '/menu-api/ingredients/low-stock?storeId=1', null, token));
  results.push(await testApi('创建食材', 'POST', '/menu-api/ingredients', { ingredientId: 'I001', storeId: '1', ingredientName: '测试食材', unit: '斤', currentStock: 100, unitPrice: 10 }, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('📦 Inventory模块');
  console.log('━'.repeat(100));
  results.push(await testApi('库存日志', 'GET', '/menu-api/inventory/logs?storeId=1', null, token));
  results.push(await testApi('库存告警', 'GET', '/menu-api/inventory/alerts?storeId=1', null, token));
  results.push(await testApi('入库操作', 'POST', '/menu-api/inventory/in', { storeId: '1', ingredientId: 'I001', quantity: 10, note: '测试入库' }, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('🍳 Recipe模块');
  console.log('━'.repeat(100));
  results.push(await testApi('菜品配方', 'GET', '/recipes/CY000001?storeId=1', null, token));
  results.push(await testApi('有配方的菜品', 'GET', '/recipes/dishes-with-recipe?storeId=1', null, token));
  results.push(await testApi('创建配方', 'POST', '/recipes', { dishId: 'CY000001', storeId: '1', ingredientId: 'I001', quantity: 0.5, unit: '斤' }, token));
  results.push(await testApi('重新计算所有成本', 'POST', '/recipes/recalc-all?storeId=1', null, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('🛒 Purchase模块');
  console.log('━'.repeat(100));
  results.push(await testApi('采购列表', 'GET', '/menu-api/purchases?storeId=1', null, token));
  results.push(await testApi('采购详情', 'GET', '/menu-api/purchases/1?storeId=1', null, token));
  results.push(await testApi('待处理采购', 'GET', '/menu-api/purchases/status/pending?storeId=1', null, token));
  results.push(await testApi('日期范围采购', 'GET', '/menu-api/purchases/range?storeId=1&startDate=2026-07-01&endDate=2026-07-31', null, token));
  results.push(await testApi('创建采购', 'POST', '/menu-api/purchases', { storeId: '1', ingredientId: 'I001', supplierId: 'S001', quantity: 50, unitPrice: 8, purchaseDate: '2026-07-27' }, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('📅 Booking模块');
  console.log('━'.repeat(100));
  results.push(await testApi('创建预订', 'POST', '/bookings', { storeId: '1', customerName: '测试客户', phone: '13900139000', bookingDate: '2026-07-28', bookingTime: '18:00', guestCount: 4, tableId: 1 }, token));
  
  console.log('\n' + '━'.repeat(100));
  console.log('👤 Customer模块');
  console.log('━'.repeat(100));
  results.push(await testApi('创建客户', 'POST', '/customers', { customerId: 'C001', storeId: '1', customerName: '测试客户2', phone: '13700137000' }, token));
  
  console.log('\n\n' + '═'.repeat(100));
  console.log('📋 测试结果汇总');
  console.log('═'.repeat(100));
  
  const total = results.length;
  const okCount = results.filter(r => r.statusCode >= 200 && r.statusCode < 300).length;
  const err500 = results.filter(r => r.statusCode === 500).length;
  const err400 = results.filter(r => r.statusCode >= 400 && r.statusCode < 500).length;
  const networkErr = results.filter(r => r.statusCode === 0).length;
  
  console.log(`\n📊 总计: ${total} 个接口`);
  console.log(`✅ 200 成功: ${okCount} 个 (${((okCount/total)*100).toFixed(1)}%)`);
  console.log(`❌ 500 错误: ${err500} 个`);
  console.log(`⚠️  4xx 错误: ${err400} 个`);
  console.log(`💥 网络错误: ${networkErr} 个`);
  
  if (err500 > 0) {
    console.log('\n' + '─'.repeat(100));
    console.log('❌ 500错误详情:');
    console.log('─'.repeat(100));
    results.filter(r => r.statusCode === 500).forEach(r => {
      console.log(`  ${r.method} ${r.path}`);
      if (r.errorMsg) console.log(`     错误: ${r.errorMsg.substring(0, 200)}`);
    });
  }
  
  if (err400 > 0) {
    console.log('\n' + '─'.repeat(100));
    console.log('⚠️  4xx错误详情:');
    console.log('─'.repeat(100));
    results.filter(r => r.statusCode >= 400 && r.statusCode < 500).forEach(r => {
      console.log(`  ${r.statusCode} | ${r.method} ${r.path}`);
      if (r.errorMsg) console.log(`     错误: ${r.errorMsg.substring(0, 200)}`);
    });
  }
  
  console.log('\n' + '═'.repeat(100));
  console.log(`修复进度: 原28个500错误 → 现${err500}个500错误`);
  console.log(`已修复: ${28 - err500} 个，剩余: ${err500} 个`);
  console.log('═'.repeat(100));
}

main().catch(console.error);

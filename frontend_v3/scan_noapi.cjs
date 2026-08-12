const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, 'src/views/dashboard');
const files = fs.readdirSync(dir).filter(f => f.endsWith('.vue'));
let noApi = [];
let hasApi = [];
for (const f of files) {
  const fp = path.join(dir, f);
  const stat = fs.statSync(fp);
  if (stat.size < 500) continue; // skip tiny files
  const content = fs.readFileSync(fp, 'utf8');
  const scriptPart = content.split('<script')[1] || '';
  const hasRequest = /request\.(get|post|put|delete|patch)/.test(scriptPart);
  const hasLocalStorage = /localStorage/.test(scriptPart);
  if (hasRequest) {
    hasApi.push({ name: f, size: stat.size });
  } else {
    noApi.push({ name: f, size: stat.size, hasLocalStorage });
  }
}
console.log(`\n=== 已有API调用: ${hasApi.length} 个 ===`);
hasApi.forEach(f => console.log(`  ✓ ${f.name} (${f.size})`));
console.log(`\n=== 无API调用（>500字节）: ${noApi.length} 个 ===`);
noApi.forEach(f => console.log(`  ✗ ${f.name} (${f.size})${f.hasLocalStorage ? ' [localStorage]' : ''}`));

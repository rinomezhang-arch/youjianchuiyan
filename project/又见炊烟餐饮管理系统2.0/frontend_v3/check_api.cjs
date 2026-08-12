const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, 'src/views/dashboard');
const files = ['Home','Bookings','Kitchen','Staff','Engineering','Safety','Marketing','GMOffice','AuditLog','Cost','Issue','Suppliers','Inventory','Reports','PermManager','Admin','CustomerAnalysis','HRAdmin'];
for (const f of files) {
  const fp = path.join(dir, f + '.vue');
  if (!fs.existsSync(fp)) { console.log(f + '.vue: NOT FOUND'); continue; }
  const c = fs.readFileSync(fp, 'utf8');
  const script = c.split('<script')[1] || '';
  const hasRequest = /request\.(get|post|put|delete|patch)/.test(script);
  const hasApiImport = /from ['"]@\/api\//.test(script);
  const hasFetch = /fetch\s*\(/.test(script);
  console.log(`${f.padEnd(20)} req=${hasRequest} api=${hasApiImport} fetch=${hasFetch}`);
}

const fs = require('fs');
const path = require('path');
const dir = path.join(__dirname, 'src/views/dashboard');
const files = ['Home','Bookings','Kitchen','Staff','Engineering','Safety','Marketing','GMOffice','AuditLog','Cost','Issue','HRAdmin','Customers','Inventory','Reports','PermManager','Admin'];
for (const f of files) {
  const fp = path.join(dir, f + '.vue');
  if (!fs.existsSync(fp)) { console.log(f + '.vue: NOT FOUND'); continue; }
  const c = fs.readFileSync(fp, 'utf8');
  const script = c.split('<script')[1] || '';
  const hasReq = /request\.(get|post|put|delete|patch)/.test(script);
  const hasApiImport = /from ['"]@\/api\//.test(script);
  const hasReqImport = /import request from/.test(script);
  console.log(`${f.padEnd(16)} req=${hasReq} apiImport=${hasApiImport} reqImport=${hasReqImport}`);
}

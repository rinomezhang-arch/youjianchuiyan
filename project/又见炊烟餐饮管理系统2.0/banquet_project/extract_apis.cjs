const fs = require('fs');
const path = require('path');

const ctrlDir = path.join(__dirname, 'src/main/java/com/youjian/banquet/controller');
const entityDir = path.join(__dirname, 'src/main/java/com/youjian/banquet/entity');
const repoDir = path.join(__dirname, 'src/main/java/com/youjian/banquet/repository');

// 1. Extract all Controller API paths
const controllers = fs.readdirSync(ctrlDir).filter(f => f.endsWith('.java'));
const apiMap = [];

for (const file of controllers) {
  const c = fs.readFileSync(path.join(ctrlDir, file), 'utf8');
  const ctrlName = file.replace('.java', '');
  
  // Find class-level @RequestMapping
  const classMapping = c.match(/@RequestMapping\(["']([^"']+)["']\)/);
  const basePath = classMapping ? classMapping[1] : '';
  
  // Find all method-level mappings
  const methodMappings = [...c.matchAll(/@(GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)(?:\(["']([^"']*)["']\))?/g)];
  
  for (const m of methodMappings) {
    const method = m[1].replace('Mapping', '').toUpperCase();
    const subPath = m[2] || '';
    const fullPath = basePath + (subPath.startsWith('/') ? subPath : '/' + subPath);
    apiMap.push({ controller: ctrlName, method, path: fullPath.replace(/\/+/g, '/') });
  }
}

// 2. Extract all Entity classes and their @Table names
const entities = fs.readdirSync(entityDir).filter(f => f.endsWith('.java'));
const entityMap = [];

for (const file of entities) {
  const c = fs.readFileSync(path.join(entityDir, file), 'utf8');
  const entityName = file.replace('.java', '');
  const tableMatch = c.match(/@Table\(name\s*=\s*"([^"]+)"/);
  const tableName = tableMatch ? tableMatch[1] : entityName.toLowerCase();
  entityMap.push({ entity: entityName, table: tableName });
}

// 3. Extract all Repository interfaces
const repos = fs.readdirSync(repoDir).filter(f => f.endsWith('.java'));

// Output
console.log(`\n========== 后端API端点 (${apiMap.length}个) ==========`);
apiMap.sort((a, b) => a.path.localeCompare(b.path));
for (const api of apiMap) {
  console.log(`  ${api.method.padEnd(7)} ${api.path.padEnd(55)} [${api.controller}]`);
}

console.log(`\n========== 数据库Entity→表 (${entityMap.length}个) ==========`);
entityMap.sort((a, b) => a.table.localeCompare(b.table));
for (const e of entityMap) {
  console.log(`  ${e.entity.padEnd(35)} → ${e.table}`);
}

console.log(`\n========== Repository (${repos.length}个) ==========`);
repos.forEach(f => console.log(`  ${f.replace('.java', '')}`));

// 4. Check: which controllers have NO entity/repository backing?
console.log(`\n========== 对齐检查 ==========`);
const entityNames = new Set(entityMap.map(e => e.entity.toLowerCase()));
const repoNames = new Set(repos.map(r => r.replace('Repository', '').toLowerCase()));

for (const ctrl of controllers) {
  const name = ctrl.replace('Controller', '').toLowerCase();
  const hasEntity = entityNames.has(name) || entityNames.has(name.replace('finance', ''));
  const hasRepo = repoNames.has(name) || repoNames.has(name + 'repository');
  
  // Some controllers map to multiple entities, skip strict check
  if (!hasEntity && !hasRepo) {
    console.log(`  ⚠️  ${ctrl} - 未找到同名Entity/Repository`);
  }
}

// Check orphan entities (entity without controller)
for (const e of entityMap) {
  const name = e.entity.toLowerCase();
  const hasCtrl = controllers.some(c => c.toLowerCase().includes(name));
  if (!hasCtrl) {
    console.log(`  📦 ${e.entity} (表:${e.table}) - 无对应Controller`);
  }
}

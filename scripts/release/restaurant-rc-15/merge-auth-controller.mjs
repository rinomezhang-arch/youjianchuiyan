#!/usr/bin/env node
/**
 * RC15 发布暂存三向合并：AuthController
 *
 * 背景：生产 AuthController 独有 /api/auth/change-password（自服务改密+管理员重置，
 *   含 parseStaffId/queryStaffById/passwordMatches 三个助手），候选 AuthController 独有
 *   登录加固（统一口径 LOGIN_FAILED、BCrypt 诱饵 decoyHash、staff_en_name 四选一登录）。
 *   整文件覆盖会让生产丢掉改密功能（回退），不合并又带不上登录加固。
 *
 * 本脚本只做文本级移植并自检双方特征都在：
 *   - 从生产文件抽取 changePassword 及其 3 个助手方法（按标记边界）
 *   - 插入候选文件的 logout 与 getStores 之间
 *   - 断言合并件同时含 decoyHash（候选加固）与 /auth/change-password（生产功能）
 * 不修改候选源码树；输出到 staging/AuthController.merged.java，随发布脚本在服务器
 * mvn package 阶段做真正编译验证（先编译后切换，编译失败不影响生产）。
 *
 * 用法：node merge-auth-controller.mjs <candidate AuthController.java> <prod AuthController.java> <out>
 */
import { readFile, writeFile, mkdir } from 'node:fs/promises';
import { dirname } from 'node:path';

const [candPath, prodPath, outPath] = process.argv.slice(2);
if (!candPath || !prodPath || !outPath) {
  console.error('usage: node merge-auth-controller.mjs <candidate> <prod> <out>');
  process.exit(2);
}
const cand = (await readFile(candPath, 'utf8')).replace(/\r\n/g, '\n');
const prod = (await readFile(prodPath, 'utf8')).replace(/\r\n/g, '\n');

// 生产独有块：从改密方法的 javadoc 注释起点，到 passwordMatches 方法结束（getStores 注解之前）。
const startMarker = '    /**\n     * 改密码接口。';
const endMarker = '    @GetMapping("/stores")';
const s = prod.indexOf(startMarker);
const e = prod.indexOf(endMarker);
if (s < 0 || e < 0 || e <= s) {
  console.error('MERGE_FAIL: 无法在生产文件中定位 change-password 块边界');
  process.exit(1);
}
const prodBlock = prod.slice(s, e).trimEnd() + '\n\n';

// 插入点：候选文件中 getStores 注解之前（即 logout 之后）。
const insertAt = cand.indexOf(endMarker);
if (insertAt < 0) {
  console.error('MERGE_FAIL: 候选文件缺少 getStores 插入锚点');
  process.exit(1);
}
const merged = cand.slice(0, insertAt) + prodBlock + cand.slice(insertAt);

// 双向特征自检
const checks = [
  ['候选登录加固-统一失败口径', merged.includes('private static final String LOGIN_FAILED')],
  ['候选登录加固-BCrypt诱饵', merged.includes('private String decoyHash()')],
  ['生产功能-改密接口', merged.includes('@PostMapping("/auth/change-password")')],
  ['生产助手-parseStaffId', merged.includes('private Long parseStaffId(')],
  ['生产助手-queryStaffById', merged.includes('private Map<String, Object> queryStaffById(')],
  ['生产助手-passwordMatches', merged.includes('private boolean passwordMatches(')],
  ['登录方法唯一', (merged.match(/public Result<Map<String, Object>> login\(/g) || []).length === 1],
  ['getStores 唯一', (merged.match(/@GetMapping\("\/stores"\)/g) || []).length === 1],
  ['change-password 唯一', (merged.match(/@PostMapping\("\/auth\/change-password"\)/g) || []).length === 1],
];
let bad = 0;
for (const [name, ok] of checks) {
  console.log(`${ok ? 'OK  ' : 'BAD '}${name}`);
  if (!ok) bad++;
}
if (bad) { console.error('MERGE_FAIL: 特征自检未全部通过'); process.exit(1); }

await mkdir(dirname(outPath), { recursive: true });
await writeFile(outPath, merged, 'utf8');
console.log(`MERGE_OK -> ${outPath} (${merged.length} chars)`);

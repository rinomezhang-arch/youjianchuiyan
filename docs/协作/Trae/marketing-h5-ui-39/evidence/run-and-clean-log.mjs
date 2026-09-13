// TR-MARKETING-H5-UI-39 R1 评审项5配套：运行命令并把输出写成"任务板可安全轮询"的日志。
// 处理规则：合并 stdout/stderr → 去 ANSI 颜色 → 对勾符号替换为 [PASS]/[FAIL]（GBK 不可编码
// 的 U+2713 曾导致默认 GBK 轮询失败）→ 去行尾空白 → 去文件末尾多余空行 → UTF-8 无 BOM 落盘。
// 用法：node run-and-clean-log.mjs <输出日志> <工作目录> <命令...>
import { execSync } from 'node:child_process'
import { writeFileSync } from 'node:fs'

const [, , outFile, cwd] = process.argv
const cmd = process.argv.slice(4).join(' ')
if (!outFile || !cwd || !cmd) {
  console.error('usage: node run-and-clean-log.mjs <out-log> <cwd> <command...>')
  process.exit(64)
}

let code = 0
let buf
try {
  buf = execSync(cmd, { cwd, encoding: 'buffer', env: { ...process.env, CI: 'true' }, maxBuffer: 128 * 1024 * 1024 })
} catch (e) {
  code = e.status ?? 1
  buf = Buffer.concat([Buffer.from(e.stdout || Buffer.alloc(0)), Buffer.from(String(e.stderr || ''))])
}

let text = buf.toString('utf8')
text = text
  // eslint-disable-next-line no-control-regex
  .replace(/\u001b\[[0-9;]*m/g, '') // ANSI 颜色码
  .replace(/\u2713/g, '[PASS]') // ✓（GBK 不可编码，曾导致任务板 GBK 轮询失败）
  .replace(/[\u2717\u2718]/g, '[FAIL]') // ✗ ✘
  .replace(/[ \t]+$/gm, '') // 行尾空白
  .replace(/\n{3,}$/g, '\n') // 末尾多余空行
  .replace(/\s+$/, '\n') // 末尾只留一个换行

writeFileSync(outFile, text, 'utf8')
const fails = (text.match(/\[FAIL\]/g) || []).length
console.log(`log written: ${outFile} | lines=${text.split('\n').length} | FAIL-markers=${fails} | exit=${code}`)
process.exit(code || (fails ? 2 : 0))

// Lightweight SFC syntax self-check (NOT a vite build): parse + compile template/script
// using the project's own @vue/compiler-sfc. Allowed by TR52 ("no full vite build").
import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import { createRequire } from 'node:module'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..', '..')
// This worktree ships no node_modules (deps are frozen by TR52). Resolve the
// compiler from the local worktree first, then the canonical F:/solo checkout (read-only use).
const candidates = [join(root, 'frontend_v3', 'package.json'), 'F:/solo/frontend_v3/package.json']
let compiler = null
for (const pkg of candidates) {
  try { compiler = createRequire(pkg)('@vue/compiler-sfc'); break } catch { /* try next */ }
}
if (!compiler) { console.log('SFC_CHECK_SKIPPED: @vue/compiler-sfc unavailable (no deps installed, install forbidden by TR52)'); process.exit(0) }
const { parse, compileTemplate, compileScript } = compiler
const file = join(root, 'frontend_v3', 'src', 'views', 'dashboard', 'StockTake.vue')
const source = readFileSync(file, 'utf8')

const { descriptor, errors } = parse(source, { filename: file })
if (errors.length) {
  console.log('SFC_PARSE_ERRORS=' + errors.length)
  for (const e of errors) console.log(String(e))
  process.exit(1)
}

const id = 'tr52check'
const script = compileScript(descriptor, { id })
const tpl = compileTemplate({
  source: descriptor.template.content,
  filename: file,
  id,
  compilerOptions: { bindingMetadata: script.bindings }
})
if (tpl.errors.length) {
  console.log('TEMPLATE_ERRORS=' + tpl.errors.length)
  for (const e of tpl.errors) console.log(String(e))
  process.exit(1)
}
console.log('SFC_OK template+script compiled (no vite build)')
console.log('usesClassifyFixed=' + source.includes('classifyFixed'))
console.log('usesQtyScale=' + source.includes('QTY_SCALE'))
console.log('blocksSubmit=' + source.includes("=== 'invalid'"))
console.log('hasInlineTip=' + source.includes('qty-invalid-tip'))

/**
 * TR-STOCKTAKE-DECIMAL-52 · Node counterexample test.
 * Verifies BigInt fixed-point rounding matches backend HALF_UP contract.
 * No browser/API/DB — pure function test of stockTakeMoney.js.
 *
 * Run: node scripts/stocktake-money-52/test-rounding.mjs
 */
import assert from 'node:assert'
import { execSync } from 'node:child_process'
import { fileURLToPath } from 'node:url'
import { dirname, join } from 'node:path'
import { parseFixed, roundHalfUp, computeRowDiff, sumCents } from '../../frontend_v3/src/utils/stockTakeMoney.js'

let pass = 0, fail = 0
function test(name, fn) { try { fn(); pass++; console.log('PASS  ' + name) } catch (e) { fail++; console.log('FAIL  ' + name + '  :: ' + e.message) } }
function eq(actual, expected, label) { assert.strictEqual(actual, expected, `${label}: expected ${expected}, got ${actual}`) }

// ── 1. Original 3 proof cases (DL-49) ──
test('proof#1: 1×0.015 → 0.02 (was 0.01)', () => {
  const r = computeRowDiff(11, 10, 0.015)
  assert(r.diffAmount === 0.02, `expected 0.02, got ${r.diffAmount}`)
})
test('proof#2: -1×0.015 → -0.02 (was -0.01)', () => {
  const r = computeRowDiff(9, 10, 0.015)
  assert(r.diffAmount === -0.02, `expected -0.02, got ${r.diffAmount}`)
})
test('proof#3: 1×0.335 → 0.34 (already correct)', () => {
  const r = computeRowDiff(11, 10, 0.335)
  assert(r.diffAmount === 0.34, `expected 0.34, got ${r.diffAmount}`)
})

// ── 2. Three-digit quantity precision ──
test('3-digit qty: 8.123−10.001 = −1.878', () => {
  const r = computeRowDiff('8.123', '10.001', 0.05706667)
  assert(r.diffQty === -1.878, `diffQty expected -1.878, got ${r.diffQty}`)
})
test('3-digit qty amount: −1.878×0.05706667 → −0.11', () => {
  const r = computeRowDiff('8.123', '10.001', 0.05706667)
  assert(r.diffAmount === -0.11, `expected -0.11, got ${r.diffAmount}`)
})

// ── 3. Eight-digit unit price ──
test('8-digit price: parseFixed(0.05706667, 8) = 5706667n', () => {
  eq(parseFixed(0.05706667, 8), 5706667n)
})
test('8-digit price amount: 2×0.05706667 → 0.11', () => {
  const r = computeRowDiff(12, 10, 0.05706667)
  assert(r.diffAmount === 0.11, `expected 0.11, got ${r.diffAmount}`)
})

// ── 4. Scientific notation 1e-8 ──
test('1e-8 parsed at scale 8 = 1n', () => {
  eq(parseFixed(1e-8, 8), 1n)
})
test('1e-8 not zero: 100000000×1e-8 → 1.00', () => {
  // diffQty = 100000000 (actual=100000001, system=1)
  const r = computeRowDiff(100000001, 1, 1e-8)
  assert(r.diffAmount === 1, `expected 1.00, got ${r.diffAmount}`)
})
test('1e-8 small: 1×1e-8 → 0.00 (too small for cents)', () => {
  const r = computeRowDiff(11, 10, 1e-8)
  assert(r.diffAmount === 0, `expected 0.00, got ${r.diffAmount}`)
})

// ── 5. Positive & negative half rounding ──
test('half+: 0.005 → 0.01', () => {
  const r = computeRowDiff(1.005, 1, 1)  // diffQty=0.005 × price=1 → 0.005
  assert(r.diffAmount === 0.01, `expected 0.01, got ${r.diffAmount}`)
})
test('half−: −0.005 → −0.01 (away from zero)', () => {
  const r = computeRowDiff(0.995, 1, 1)  // diffQty=−0.005 × price=1 → −0.005
  assert(r.diffAmount === -0.01, `expected -0.01, got ${r.diffAmount}`)
})
test('half+: 0.025 → 0.03', () => {
  const r = computeRowDiff(1.025, 1, 1)
  assert(r.diffAmount === 0.03, `expected 0.03, got ${r.diffAmount}`)
})
test('half−: −0.025 → −0.03 (away from zero)', () => {
  const r = computeRowDiff(0.975, 1, 1)
  assert(r.diffAmount === -0.03, `expected -0.03, got ${r.diffAmount}`)
})

// ── 6. Row total: accumulate integer cents, not floats ──
test('3 rows × 0.335 → total 1.02 (not 0.99 or 1.01)', () => {
  const rows = [0, 1, 2].map(() => {
    const r = computeRowDiff(11, 10, 0.335)  // each: diffAmount=0.34, cents=34n
    return { _diffAmountCents: r.diffAmountCents }
  })
  const total = sumCents(rows)  // 34n × 3 = 102n → 1.02
  assert(total === 1.02, `expected 1.02, got ${total}`)
})
test('mixed rows: 0.02 + (−0.02) + 0.02 → 0.02', () => {
  const rows = [
    { _diffAmountCents: computeRowDiff(11, 10, 0.015).diffAmountCents },  // 2n
    { _diffAmountCents: computeRowDiff(9, 10, 0.015).diffAmountCents },   // -2n
    { _diffAmountCents: computeRowDiff(11, 10, 0.015).diffAmountCents },  // 2n
  ]
  const total = sumCents(rows)
  assert(total === 0.02, `expected 0.02, got ${total}`)
})

// ── 7. Edge cases: null, 0, invalid ──
test('null actual → all null (not 0)', () => {
  const r = computeRowDiff(null, 10, 0.015)
  assert(r.diffQty === null && r.diffAmount === null && r.diffAmountCents === null)
})
test('empty string actual → all null', () => {
  const r = computeRowDiff('', 10, 0.015)
  assert(r.diffQty === null && r.diffAmount === null && r.diffAmountCents === null)
})
test('zero diff → diffQty=0, amount=0.00', () => {
  const r = computeRowDiff(10, 10, 0.015)
  assert(r.diffQty === 0 && r.diffAmount === 0, `qty=${r.diffQty} amt=${r.diffAmount}`)
})
test('sumCents with all-null rows → 0', () => {
  const total = sumCents([{ _diffAmountCents: null }, { _diffAmountCents: null }])
  assert(total === 0, `expected 0, got ${total}`)
})

// ── 8. roundHalfUp direct ──
test('roundHalfUp(15, 3→2) = 2n (0.015→0.02)', () => {
  eq(roundHalfUp(15n, 3, 2), 2n)
})
test('roundHalfUp(-15, 3→2) = -2n (−0.015→−0.02)', () => {
  eq(roundHalfUp(-15n, 3, 2), -2n)
})

// ── Result ──
console.log(`\n==== TR52 ROUNDING TEST: ${pass} passed, ${fail} failed ====`)

// ── Record source SHA ──
const _dirname = dirname(fileURLToPath(import.meta.url))
const root = join(_dirname, '..', '..')
const sha = execSync('git rev-parse HEAD', { cwd: root, encoding: 'utf-8' }).trim()
const stockTakeSha = execSync(`git hash-object "${join('frontend_v3', 'src', 'utils', 'stockTakeMoney.js')}"`, { cwd: root, encoding: 'utf-8' }).trim()
const vueSha = execSync(`git hash-object "${join('frontend_v3', 'src', 'views', 'dashboard', 'StockTake.vue')}"`, { cwd: root, encoding: 'utf-8' }).trim()
console.log(`commit_sha: ${sha}`)
console.log(`stockTakeMoney.js sha: ${stockTakeSha}`)
console.log(`StockTake.vue sha: ${vueSha}`)

process.exit(fail === 0 ? 0 : 1)

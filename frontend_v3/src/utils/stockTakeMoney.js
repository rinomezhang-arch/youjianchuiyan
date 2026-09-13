/**
 * stockTakeMoney.js — BigInt fixed-point decimal arithmetic for stock-take preview.
 *
 * Aligns frontend preview rounding with backend StockTakeController HALF_UP contract:
 *   - Quantity: 3 decimal places (scale 10^3)
 *   - Unit price: 8 decimal places (scale 10^8)
 *   - Per-row diff amount = diffQty(3) × unitPrice(8) → HALF_UP to cents(scale 2)
 *   - Total = Σ per-row integer cents (accumulate at cent scale, never float)
 *
 * No floating-point multiplication is used for the amount calculation.
 * No external dependencies.
 */

const QTY_SCALE = 3
const PRICE_SCALE = 8
const AMOUNT_SCALE = 2
export { QTY_SCALE, PRICE_SCALE, AMOUNT_SCALE }
const QTY_FACTOR = 10n ** BigInt(QTY_SCALE)        // 1000n
const PRICE_FACTOR = 10n ** BigInt(PRICE_SCALE)     // 100000000n
const AMOUNT_FACTOR = 10n ** BigInt(AMOUNT_SCALE)   // 100n
const COMBINED_SCALE = QTY_SCALE + PRICE_SCALE     // 11

/**
 * Parse a value (number, string, null) to a BigInt at the given decimal scale.
 * Handles scientific notation (e.g. 1e-8), negative numbers, and 0.
 * Returns null for null/undefined/empty/invalid — never masquerades as 0.
 */
export function parseFixed(value, scale) {
  if (value === null || value === undefined || value === '') return null
  const s = String(value).trim()
  if (s === '' || s === '-' || s === '+') return null

  let negative = false
  let str = s
  if (str[0] === '-') { negative = true; str = str.slice(1) }
  else if (str[0] === '+') { str = str.slice(1) }
  if (str === '' || str === '.') return null

  const factor = 10n ** BigInt(scale)

  // Scientific notation: 1e-8, 1.5e-3, etc.
  const eIdx = str.search(/[eE]/)
  if (eIdx !== -1) {
    const mantissa = str.slice(0, eIdx)
    const expStr = str.slice(eIdx + 1)
    const exp = parseInt(expStr, 10)
    if (isNaN(exp)) return null
    const [intP = '', fracP = ''] = mantissa.split('.')
    if (!/^\d*$/.test(intP) || !/^\d*$/.test(fracP) || (intP === '' && fracP === '')) return null
    const digits = (intP + fracP) || '0'
    // value = digits × 10^(exp - fracP.length)
    // at scale `scale`: digits × 10^(exp - fracP.length + scale)
    const power = BigInt(exp - fracP.length + scale)
    const bigDigits = BigInt(digits)
    let result
    if (power >= 0n) {
      result = bigDigits * (10n ** power)
    } else {
      // Value is smaller than scale precision — truncate (matches DB column precision)
      result = bigDigits / (10n ** -power)
    }
    return negative ? -result : result
  }

  // Regular decimal
  const [intP = '', fracP = ''] = str.split('.')
  if (!/^\d*$/.test(intP) || !/^\d*$/.test(fracP) || (intP === '' && fracP === '')) return null

  let frac = fracP
  while (frac.length < scale) frac += '0'
  if (frac.length > scale) frac = frac.slice(0, scale)

  const bigInt = BigInt(intP || '0') * factor + BigInt(frac || '0')
  return negative ? -bigInt : bigInt
}

/**
 * Round a BigInt value from one scale to another using HALF_UP (away from zero at half).
 * Matches Java BigDecimal.setScale(n, RoundingMode.HALF_UP).
 */
export function roundHalfUp(value, fromScale, toScale) {
  if (fromScale === toScale) return value
  if (fromScale < toScale) return value * (10n ** BigInt(toScale - fromScale))
  const divisor = 10n ** BigInt(fromScale - toScale)
  const half = divisor / 2n
  if (value >= 0n) {
    return (value + half) / divisor
  }
  return -((-value + half) / divisor)
}

/**
 * Strict decimal inspection shared by validation.
 * Unlike parseFixed (which truncates over-scale fraction digits), this keeps the
 * exact digit structure so the UI can reject out-of-contract precision instead
 * of silently dropping it.
 * Returns { kind: 'empty' } for null/undefined/blank/sign-only input,
 * { kind: 'invalid' } for NaN/Infinity/malformed strings,
 * { kind: 'ok', intDigits, fracDigits } with trailing-zero fraction trimmed.
 */
function inspectDecimal(value) {
  if (value === null || value === undefined) return { kind: 'empty' }
  const raw = String(value).trim()
  if (raw === '' || raw === '-' || raw === '+') return { kind: 'empty' }

  let str = raw
  if (str[0] === '-' || str[0] === '+') str = str.slice(1)
  if (!str || str === '.') return { kind: 'invalid' }

  let exp = 0
  const eIdx = str.search(/[eE]/)
  if (eIdx !== -1) {
    const expStr = str.slice(eIdx + 1)
    if (!/^[+-]?\d+$/.test(expStr)) return { kind: 'invalid' }
    exp = parseInt(expStr, 10)
    str = str.slice(0, eIdx)
  }

  const parts = str.split('.')
  if (parts.length > 2) return { kind: 'invalid' }
  const intP = parts[0] ?? ''
  const fracP = parts[1] ?? ''
  if (!/^\d*$/.test(intP) || !/^\d*$/.test(fracP) || (intP === '' && fracP === '')) {
    return { kind: 'invalid' }
  }

  // Rebase the decimal point after applying the exponent: value = digits x 10^(exp - fracLen).
  const digits = (intP + fracP).replace(/^0+/, '') || '0'
  const k = fracP.length - exp
  let intDigits
  let fracDigits
  if (k <= 0) {
    intDigits = digits + '0'.repeat(-k)
    fracDigits = ''
  } else if (k < digits.length) {
    intDigits = digits.slice(0, digits.length - k)
    fracDigits = digits.slice(digits.length - k)
  } else {
    intDigits = '0'
    fracDigits = '0'.repeat(k - digits.length) + digits
  }
  intDigits = intDigits.replace(/^0+/, '') || '0'
  fracDigits = fracDigits.replace(/0+$/, '')
  return { kind: 'ok', intDigits, fracDigits }
}

/**
 * Classify a user/JSON numeric input at the contract scale.
 *  - 'empty': not filled (null/undefined/blank)
 *  - 'invalid': malformed (NaN/Infinity/'abc'/'1.2.3') or more fraction digits than scale
 *  - 'ok': exactly representable at `scale` (1e-8 at scale 8 is ok; 5e-9 is invalid)
 * Unknown/invalid is NEVER reported as 0.
 */
export function classifyFixed(value, scale) {
  const r = inspectDecimal(value)
  if (r.kind !== 'ok') return r.kind
  return r.fracDigits.length > scale ? 'invalid' : 'ok'
}

/**
 * Compute per-row diff using BigInt fixed-point arithmetic.
 * Input: actualQuantity, systemQuantity (≤3 decimals), unitPrice (≤8 decimals).
 * Returns { diffQty, diffAmount, diffAmountCents, invalid }:
 *  - unfilled actual  -> all null, invalid=false
 *  - malformed/over-precision actual -> all null, invalid=true (UI must block submit)
 *  - valid -> diffQty Number(3dp), diffAmount Number(2dp), diffAmountCents BigInt
 * diffQty: Number (3-decimal, safe for display/comparison within inventory range)
 * diffAmount: Number (2-decimal cents, safe for display/comparison)
 * diffAmountCents: BigInt (integer cents for accumulation)
 */
export function computeRowDiff(actualQuantity, systemQuantity, unitPrice) {
  const kind = classifyFixed(actualQuantity, QTY_SCALE)
  const blank = { diffQty: null, diffAmount: null, diffAmountCents: null }
  if (kind === 'empty') return { ...blank, invalid: false }
  if (kind === 'invalid') return { ...blank, invalid: true }

  const actual = parseFixed(actualQuantity, QTY_SCALE)
  const system = parseFixed(systemQuantity, QTY_SCALE) ?? 0n
  const price = parseFixed(unitPrice, PRICE_SCALE) ?? 0n

  const diffQtyBig = actual - system                           // scale 3
  const amountRaw = diffQtyBig * price                          // scale 11 (3+8)
  const cents = roundHalfUp(amountRaw, COMBINED_SCALE, AMOUNT_SCALE)  // scale 2 (integer cents)

  return {
    diffQty: Number(diffQtyBig) / Number(QTY_FACTOR),
    diffAmount: Number(cents) / Number(AMOUNT_FACTOR),
    diffAmountCents: cents,
    invalid: false
  }
}

/**
 * Sum per-row integer cents (BigInt) into a total Number with 2 decimals.
 * Accumulates at cent scale — no float addition drift.
 */
export function sumCents(rows) {
  let total = 0n
  for (const row of rows) {
    if (row && row._diffAmountCents != null) total += row._diffAmountCents
  }
  return Number(total) / Number(AMOUNT_FACTOR)
}

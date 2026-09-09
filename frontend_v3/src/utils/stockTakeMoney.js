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
 * Compute per-row diff using BigInt fixed-point arithmetic.
 * Input: actualQuantity, systemQuantity (≤3 decimals), unitPrice (≤8 decimals).
 * Returns { diffQty, diffAmount, diffAmountCents } all null when actualQuantity is null/empty.
 * diffQty: Number (3-decimal, safe for display/comparison within inventory range)
 * diffAmount: Number (2-decimal cents, safe for display/comparison)
 * diffAmountCents: BigInt (integer cents for accumulation)
 */
export function computeRowDiff(actualQuantity, systemQuantity, unitPrice) {
  const actual = parseFixed(actualQuantity, QTY_SCALE)
  if (actual === null) return { diffQty: null, diffAmount: null, diffAmountCents: null }

  const system = parseFixed(systemQuantity, QTY_SCALE) ?? 0n
  const price = parseFixed(unitPrice, PRICE_SCALE) ?? 0n

  const diffQtyBig = actual - system                           // scale 3
  const amountRaw = diffQtyBig * price                          // scale 11 (3+8)
  const cents = roundHalfUp(amountRaw, COMBINED_SCALE, AMOUNT_SCALE)  // scale 2 (integer cents)

  return {
    diffQty: Number(diffQtyBig) / Number(QTY_FACTOR),
    diffAmount: Number(cents) / Number(AMOUNT_FACTOR),
    diffAmountCents: cents
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

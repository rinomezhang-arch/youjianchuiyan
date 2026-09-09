// Match the stock-take DECIMAL contract without passing money through Number.
const MAX_QUANTITY = 999999999999n // DECIMAL(12,3)
const MAX_PRICE = 9999999999999999n // DECIMAL(16,8)
const MAX_CENTS = 999999999999n // DECIMAL(12,2)
const MAX_SAFE_UNITS = 9007199254740991n
const abs = value => value < 0n ? -value : value
const sign = value => value > 0n ? 1 : value < 0n ? -1 : 0

export function isEmptyStockTakeQuantity(value) {
  return value == null || (typeof value === 'string' && value.trim() === '')
}

function fixedUnits(value, scale, label, maximum) {
  if (!['number', 'string'].includes(typeof value) ||
      (typeof value === 'number' && !Number.isFinite(value))) {
    throw new Error(`${label}必须是有效数字`)
  }
  const text = String(value).trim()
  if (!text || text.length > 128) throw new Error(`${label}必须是有效数字`)
  const match = /^([+-]?)(?:(\d+)(?:\.(\d*))?|\.(\d+))(?:[eE]([+-]?\d{1,3}))?$/.exec(text)
  if (!match) throw new Error(`${label}必须是有效数字`)
  const fraction = match[3] ?? match[4] ?? ''
  const significant = ((match[2] || '0') + fraction).replace(/^0+/, '').replace(/0+$/, '')
  if (typeof value === 'number' && significant.length > 15) {
    throw new Error(`${label}超出数字安全精度，请使用精确十进制字符串`)
  }
  // Number is used only for this bounded integer exponent, never money arithmetic.
  const exponent = Number(match[5] || 0)
  if (Math.abs(exponent) > 30) throw new Error(`${label}超出可支持的精度或范围`)
  let units = BigInt((match[2] || '0') + fraction)
  const shift = scale + exponent - fraction.length
  if (shift < 0) {
    const divisor = 10n ** BigInt(-shift)
    if (units % divisor !== 0n) throw new Error(`${label}最多${scale}位小数`)
    units /= divisor
  } else {
    units *= 10n ** BigInt(shift)
  }
  if (typeof value === 'number' && units > MAX_SAFE_UNITS) {
    throw new Error(`${label}超出数字安全精度，请使用精确十进制字符串`)
  }
  if (units > maximum) throw new Error(`${label}超出可支持范围`)
  return match[1] === '-' ? -units : units
}

function fixedText(units, scale) {
  const digits = abs(units).toString().padStart(scale + 1, '0')
  return `${units < 0n ? '-' : ''}${digits.slice(0, -scale)}.${digits.slice(-scale)}`
}

// Quantity thousandths * price hundred-millionths -> cents, HALF_UP away from zero.
function amountCents(quantity, price) {
  const product = quantity * price
  const cents = (abs(product) + 500000000n) / 1000000000n
  if (cents > MAX_CENTS) throw new Error('金额超出可支持范围（最多9999999999.99）')
  return product < 0n ? -cents : cents
}

export function calculateStockTakeLine({ actualQuantity, systemQuantity, unitPrice }) {
  if (isEmptyStockTakeQuantity(actualQuantity)) return null
  const actual = fixedUnits(actualQuantity, 3, '实盘数量', MAX_QUANTITY)
  const system = fixedUnits(systemQuantity, 3, '系统库存', MAX_QUANTITY)
  const price = fixedUnits(unitPrice, 8, '单价', MAX_PRICE)
  if (actual < 0n) throw new Error('实盘数量不能为负数')
  if (price < 0n) throw new Error('单价不能为负数')
  const difference = actual - system
  if (abs(difference) > MAX_QUANTITY) throw new Error('差异数量超出可支持范围')
  // All three amounts are persisted by the backend; reject overflow before submission.
  amountCents(system, price)
  amountCents(actual, price)
  const cents = amountCents(difference, price)
  return {
    actualQuantity: fixedText(actual, 3),
    diffQty: fixedText(difference, 3),
    diffSign: sign(difference),
    diffCents: cents,
    diffAmount: fixedText(cents, 2),
    amountSign: sign(cents)
  }
}

export function sumStockTakeCents(values) {
  let cents = 0n
  for (const value of values) {
    if (typeof value !== 'bigint' || abs(value) > MAX_CENTS) throw new Error('明细金额无效，请先核对盘点数据')
    cents += value
  }
  if (abs(cents) > MAX_CENTS) throw new Error('合计金额超出可支持范围（最多9999999999.99）')
  return { cents, text: fixedText(cents, 2), sign: sign(cents) }
}

// Two-decimal display contract for preview, stored history and stored details.
// Missing or over-precision amounts are unknown, never silently converted to zero.
export function formatStockTakeMoney(value) {
  return fixedText(fixedUnits(value, 2, '金额', MAX_CENTS), 2)
}

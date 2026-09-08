// 金额全程走字符串与 BigInt 的分，绝不落到 Number。
// 0.1 + 0.2 那种二进制误差记到账上就是真实的钱差，前端算一次就够错一次；
// 这里只做"规范化成两位小数的字符串"，真正的余额与状态一律以服务端为准。

/**
 * 把用户输入规范成 `整数.两位` 的字符串。
 * @param value 原始输入
 * @param allowZero 应收单总额允许为 0（服务端 strictlyPositive=false）；收款金额必须大于 0
 * @param label 出错提示里的字段名
 */
export function amountText(value, { allowZero = false, label = '金额' } = {}) {
  const text = String(value ?? '').trim()
  // 位数上限对齐 DECIMAL(12,2)：整数部分最多 10 位
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(text)) {
    throw new Error(`${label}须为不带符号的数字，最多两位小数`)
  }
  const [whole, fraction = ''] = text.split('.')
  const cents = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'))
  if (cents === 0n && !allowZero) throw new Error(`${label}须大于零`)
  return centsText(cents)
}

/** 已规范化的金额字符串转分；给"待收 = 总额 − 已收"这类**展示用**推导使用。 */
export function centsOf(text) {
  if (typeof text !== 'string' || !/^\d{1,10}\.\d{2}$/.test(text)) {
    throw new Error('金额格式不正确')
  }
  const [whole, fraction] = text.split('.')
  return BigInt(whole) * 100n + BigInt(fraction)
}

export function centsText(cents) {
  const negative = cents < 0n
  const abs = negative ? -cents : cents
  return `${negative ? '-' : ''}${abs / 100n}.${String(abs % 100n).padStart(2, '0')}`
}

/**
 * 后端 `SELECT *` 直出的金额可能是数字、字符串或 null，统一成展示文本。
 * 拿不准就返回 `--`，**不要猜 0**：把未知显示成 0 会让人以为这笔真的没钱。
 */
export function displayAmount(raw) {
  if (raw === null || raw === undefined || raw === '') return '--'
  try {
    return amountText(raw, { allowZero: true })
  } catch {
    return '--'
  }
}

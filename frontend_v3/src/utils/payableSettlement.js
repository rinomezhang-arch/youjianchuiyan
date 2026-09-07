export function moneyText(value) {
  const text = String(value ?? '').trim()
  if (!/^\d{1,10}(\.\d{1,2})?$/.test(text)) throw new Error('金额须为正数，最多两位小数')
  const [whole, fraction = ''] = text.split('.')
  const cents = BigInt(whole) * 100n + BigInt(fraction.padEnd(2, '0'))
  if (cents <= 0n) throw new Error('金额须大于零')
  return `${cents / 100n}.${String(cents % 100n).padStart(2, '0')}`
}

// Persist before sending. An uncertain response must never generate a new payment key.
export function createSettlementAttempt({ storage, scope, send, uuid = () => globalThis.crypto.randomUUID() }) {
  if (!scope) throw new Error('请先确认当前账户和门店')
  const key = `payable-pending:${scope}`
  let busy = false
  const pending = () => {
    const raw = storage.getItem(key)
    if (!raw) return null
    const value = JSON.parse(raw)
    if (!Number.isSafeInteger(value.payableId) || value.payableId <= 0 ||
      !value.requestId || typeof value.requestId !== 'string' || value.requestId.length > 64 ||
      moneyText(value.settleAmount) !== value.settleAmount) throw new Error('待确认结算记录损坏，请先核对流水')
    return value
  }
  return { pending, async submit(payableId, amount) {
    if (busy) throw new Error('结算正在提交，请稍候')
    if (!Number.isSafeInteger(payableId) || payableId <= 0) throw new Error('应付单号无效')
    const settleAmount = moneyText(amount)
    let body = pending()
    if (body && (body.payableId !== payableId || body.settleAmount !== settleAmount)) {
      throw new Error('上次结算结果尚未确认，请先用原金额重试并核对流水')
    }
    if (!body) {
      body = { payableId, settleAmount, requestId: uuid() }
      storage.setItem(key, JSON.stringify(body))
    }
    busy = true
    try {
      const result = await send({ ...body })
      if (result?.code !== 200 || !result.data?.settlementNo || result.data.payableId !== payableId) {
        throw new Error('结算响应不完整，请沿用原请求重试并核对流水')
      }
      storage.removeItem(key)
      return result.data
    } finally { busy = false }
  } }
}

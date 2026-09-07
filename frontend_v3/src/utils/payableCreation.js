import { moneyText } from './payableSettlement.js'

// This is an uncertainty journal, NOT server-side creation idempotency.
// Once sent/restored, never POST it again. Only an exact unique list match confirms it.
export function createPayableCreation({ storage, scope, storeId, send, uuid = () => globalThis.crypto.randomUUID() }) {
  if (!scope || !Number.isSafeInteger(storeId) || storeId <= 0) throw new Error('请确认账户和门店')
  const key = `payable-create-pending:${scope}`
  let busy = false
  function pending() {
    const raw = storage.getItem(key)
    if (!raw) return null
    try {
      const saved = JSON.parse(raw), p = saved?.payload
      if (saved.version !== 1 || !p || p.storeId !== storeId ||
          typeof p.payableNo !== 'string' || !/^PY[A-Za-z0-9]{1,48}$/.test(p.payableNo) ||
          typeof p.supplierName !== 'string' || !p.supplierName.trim() || p.supplierName !== p.supplierName.trim() || p.supplierName.length > 100 ||
          typeof p.remark !== 'string' || p.remark.length > 200 || moneyText(p.totalAmount) !== p.totalAmount) throw new Error()
      return saved
    } catch { throw new Error('新增应付恢复记录损坏，原条目已保留；可查询列表和流水，写入已锁定，请人工核对') }
  }
  async function submit(draft) {
    if (busy) throw new Error('新增应付正在提交')
    if (pending()) throw new Error('原新增结果尚未确认，请查询原单，不可重复发送')
    const supplierName = String(draft.supplierName ?? '').trim()
    const remark = String(draft.remark ?? '')
    if (!supplierName || supplierName.length > 100 || remark.length > 200) throw new Error('供应商或备注格式不正确')
    const totalAmount = moneyText(draft.totalAmount)
    const payableNo = `PY${uuid().replace(/-/g, '')}`
    if (!/^PY[A-Za-z0-9]{1,48}$/.test(payableNo)) throw new Error('无法生成有效应付单号')
    const payload = { storeId, payableNo, supplierName, totalAmount, remark }
    const raw = JSON.stringify({ version: 1, payload })
    // Write before the network call; storage failures must prevent submission.
    storage.setItem(key, raw)
    busy = true
    try {
      const response = await send({ ...payload })
      if (response?.code !== 200 || !Number.isSafeInteger(response.data?.payableId) || response.data.payableId <= 0) {
        throw new Error('新增回执不完整，请查询原单核对，不要重复提交')
      }
      return response.data
    } catch (error) {
      const status = error?.response?.status
      // submit only starts a fresh attempt. Restored/unknown attempts never reach send.
      if ([400, 403].includes(status) && error.response.data?.code === status && storage.getItem(key) === raw) storage.removeItem(key)
      throw error
    } finally { busy = false }
  }
  function reconcile(rows) {
    const saved = pending()
    if (!saved) return 'none'
    if (busy) return 'waiting'
    const raw = storage.getItem(key), p = saved.payload
    if (!Array.isArray(rows)) throw new Error('应付列表格式不正确')
    const matches = rows.filter(row => row.payableNo === p.payableNo)
    if (!matches.length) return 'missing'
    if (matches.length !== 1) return 'ambiguous'
    const row = matches[0]
    let sameAmount = false
    try { sameAmount = moneyText(row.totalAmount) === p.totalAmount } catch {}
    if (Number(row.storeId) !== storeId || row.supplierName !== p.supplierName || !sameAmount ||
        !Number.isSafeInteger(Number(row.payableId)) || Number(row.payableId) <= 0) return 'mismatch'
    if (storage.getItem(key) !== raw) throw new Error('恢复记录已变化，请重新核对')
    storage.removeItem(key)
    return 'confirmed'
  }
  return { pending, submit, reconcile }
}

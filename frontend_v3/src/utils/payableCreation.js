import { moneyText } from './payableSettlement.js'

// Same-realm instances share an in-flight gate. Server idempotency remains the
// authority across tabs/processes; sessionStorage is a recovery journal, not a lock.
const flights = new WeakMap()
export function createPayableCreation({ storage, scope, storeId, send, uuid = () => globalThis.crypto.randomUUID() }) {
  if (!scope || !Number.isSafeInteger(storeId) || storeId <= 0) throw new Error('请确认账户和门店')
  const key = `payable-create-pending:${scope}`
  if (!flights.has(storage)) flights.set(storage, new Set())
  const active = flights.get(storage)
  function validReceipt(d, p) {
    return d && d.requestId === p.requestId && d.payableNo === p.payableNo && d.storeId === storeId &&
      Number.isSafeInteger(d.payableId) && d.payableId > 0 && typeof d.replayed === 'boolean' && moneyText(d.totalAmount) === p.totalAmount
  }
  function pending() {
    const raw = storage.getItem(key)
    if (!raw) return null
    try {
      const saved = JSON.parse(raw), p = saved?.payload
      if (![1, 2].includes(saved.version) || !p || p.storeId !== storeId ||
          typeof p.payableNo !== 'string' || !/^PY[A-Za-z0-9]{1,48}$/.test(p.payableNo) ||
          typeof p.supplierName !== 'string' || !p.supplierName.trim() || p.supplierName !== p.supplierName.trim() || p.supplierName.length > 100 ||
          typeof p.remark !== 'string' || p.remark.length > 200 || moneyText(p.totalAmount) !== p.totalAmount) throw new Error()
      if (saved.version === 2 && (saved.scope !== scope || typeof p.requestId !== 'string' ||
          !/^[A-Za-z0-9_-]{1,64}$/.test(p.requestId) ||
          Object.keys(p).sort().join(',') !== 'payableNo,remark,requestId,storeId,supplierName,totalAmount' ||
          (saved.receipt !== undefined && !validReceipt(saved.receipt, p)))) throw new Error()
      return saved
    } catch { throw new Error('新增应付恢复记录损坏，原条目已保留；可查询列表和流水，写入已锁定，请人工核对') }
  }
  function persist(raw) {
    storage.setItem(key, raw)
    if (storage.getItem(key) !== raw) throw new Error('恢复记录未可靠保存，已阻止发送')
  }
  async function transmit(saved, fresh) {
    if (active.has(key)) throw new Error('新增应付正在提交')
    const raw = JSON.stringify(saved)
    // Includes retries: unavailable storage must prevent POST, even for a saved request.
    persist(raw)
    active.add(key)
    try {
      const response = await send({ ...saved.payload })
      let valid = false
      try { valid = response?.code === 200 && validReceipt(response.data, saved.payload) &&
        (!saved.receipt || saved.receipt.payableId === response.data.payableId) } catch {}
      if (!valid) throw new Error('新增回执不完整或与原请求不符，请核对原单；恢复记录已保留')
      if (storage.getItem(key) !== raw) throw new Error('恢复记录已变化，请重新核对')
      // Keep the validated ID until list reconciliation; retries must return the same ID.
      const { requestId, payableId, payableNo, storeId: receiptStore, totalAmount, replayed } = response.data
      persist(JSON.stringify({ ...saved, receipt: { requestId, payableId, payableNo, storeId: receiptStore, totalAmount, replayed } }))
      return response.data
    } catch (error) {
      const status = error?.response?.status
      if (fresh && [400, 403].includes(status) && error.response.data?.code === status && storage.getItem(key) === raw) storage.removeItem(key)
      throw error
    } finally { active.delete(key) }
  }
  async function submit(draft) {
    if (active.has(key)) throw new Error('新增应付正在提交')
    if (pending()) throw new Error('原新增结果尚未确认，请恢复原请求或查询原单，不可重复发送')
    const supplierName = String(draft.supplierName ?? '').trim(), remark = String(draft.remark ?? '')
    if (!supplierName || supplierName.length > 100 || remark.length > 200) throw new Error('供应商或备注格式不正确')
    const totalAmount = moneyText(draft.totalAmount), requestId = uuid()
    if (typeof requestId !== 'string' || !/^[A-Za-z0-9_-]{1,64}$/.test(requestId)) throw new Error('无法生成有效请求号')
    const payableNo = `PY${requestId.replace(/[-_]/g, '')}`
    if (!/^PY[A-Za-z0-9]{1,48}$/.test(payableNo)) throw new Error('无法生成有效应付单号')
    return transmit({ version: 2, scope, payload: { requestId, storeId, payableNo, supplierName, totalAmount, remark } }, true)
  }
  async function resume() {
    if (active.has(key)) throw new Error('新增应付正在提交')
    const saved = pending()
    if (!saved) throw new Error('没有待恢复的新增应付')
    if (saved.version !== 2) throw new Error('旧版记录只能查询列表核对，不可重发')
    return transmit(saved, false)
  }
  function reconcile(rows) {
    const saved = pending()
    if (!saved) return 'none'
    if (active.has(key)) return 'waiting'
    const raw = storage.getItem(key), p = saved.payload
    if (!Array.isArray(rows)) throw new Error('应付列表格式不正确')
    const matches = rows.filter(row => row.payableNo === p.payableNo)
    if (!matches.length) return 'missing'
    if (matches.length !== 1) return 'ambiguous'
    const row = matches[0]
    let sameAmount = false
    try { sameAmount = moneyText(row.totalAmount) === p.totalAmount } catch {}
    if (Number(row.storeId) !== storeId || row.supplierName !== p.supplierName || !sameAmount ||
        !Number.isSafeInteger(Number(row.payableId)) || Number(row.payableId) <= 0 ||
        (saved.receipt && saved.receipt.payableId !== Number(row.payableId))) return 'mismatch'
    if (storage.getItem(key) !== raw) throw new Error('恢复记录已变化，请重新核对')
    storage.removeItem(key)
    return 'confirmed'
  }
  return { pending, submit, resume, reconcile }
}

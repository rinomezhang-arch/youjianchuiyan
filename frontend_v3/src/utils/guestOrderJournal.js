// Contains only submission identifiers and quantities, never credentials or view tokens.
export function journalKey(scope) {
  if (!scope || !/^[1-9]\d*$/.test(String(scope.store_id)) || !scope.device_sn || !scope.booking_id) {
    throw new Error('设备、门店或订单无效，不能提交')
  }
  return 'guest-order:submission:v1:' + [scope.store_id, scope.device_sn, scope.booking_id].map(v => encodeURIComponent(String(v))).join(':')
}

function normalizeDishes(dishes) {
  if (!Array.isArray(dishes) || !dishes.length) throw new Error('请选择加菜')
  const ids = new Set()
  return dishes.map(row => {
    const id = typeof row?.dish_id === 'string' ? row.dish_id.trim().normalize('NFC') : ''
    if (!id || ids.has(id)
      || !Number.isInteger(row.dish_quantity) || row.dish_quantity < 1 || row.dish_quantity > 99) {
      throw new Error('菜品或数量无效，每道菜限1至99份')
    }
    ids.add(id)
    return { dish_id: id, dish_quantity: row.dish_quantity }
  })
}

export function createJournal(scope, dishes, requestId = globalThis.crypto.randomUUID()) {
  journalKey(scope)
  if (typeof requestId !== 'string' || !/^[A-Za-z0-9_-]{16,100}$/.test(requestId)) throw new Error('提交编号无效')
  return { version: 1, scope: { store_id: String(scope.store_id), device_sn: String(scope.device_sn), booking_id: String(scope.booking_id) },
    client_request_id: requestId, dishes: normalizeDishes(dishes), state: 'pending' }
}

export function readJournal(storage, scope) {
  const raw = storage.getItem(journalKey(scope))
  if (raw === null) return null
  try {
    const value = JSON.parse(raw)
    const clean = createJournal(value.scope, value.dishes, value.client_request_id)
    if (value.version !== 1 || journalKey(clean.scope) !== journalKey(scope)
      || JSON.stringify(value.dishes) !== JSON.stringify(clean.dishes)
      || !['pending', 'committed'].includes(value.state)) throw new Error()
    // Unknown properties are rejected rather than carrying credentials or damaged data forward.
    const allowed = ['version', 'scope', 'client_request_id', 'dishes', 'state']
    if (Object.keys(value).some(key => !allowed.includes(key))
      || Object.keys(value.scope).some(key => !['store_id', 'device_sn', 'booking_id'].includes(key))
      || value.dishes.some(row => Object.keys(row).some(key => !['dish_id', 'dish_quantity'].includes(key)))) throw new Error()
    return { ...clean, state: value.state }
  } catch { throw new Error('本机加菜记录损坏，已锁定提交；请联系管理员核对，原记录已保留') }
}

export function saveJournal(storage, entry) {
  const key = journalKey(entry.scope), raw = JSON.stringify(entry)
  const current = readJournal(storage, entry.scope)
  if (current && (current.client_request_id !== entry.client_request_id || JSON.stringify(current.dishes) !== JSON.stringify(entry.dishes))) {
    throw new Error('本机记录已变化，请刷新后核对')
  }
  storage.setItem(key, raw)
  if (storage.getItem(key) !== raw) throw new Error('无法保存加菜记录，未发送请求')
}

export function clearJournal(storage, entry) {
  const current = readJournal(storage, entry.scope)
  if (current && current.client_request_id !== entry.client_request_id) throw new Error('本机记录已变化，请刷新后核对')
  storage.removeItem(journalKey(entry.scope))
  if (storage.getItem(journalKey(entry.scope)) !== null) throw new Error('记录清理失败，请刷新后核对')
}

export function assertReceipt(data, entry) {
  if (!data || data.status !== 'committed' || data.client_request_id !== entry.client_request_id
    || data.booking_id !== entry.scope.booking_id || data.added_dishes !== entry.dishes.length
    || typeof data.added_amount !== 'number' || !Number.isFinite(data.added_amount) || data.added_amount < 0
    || !Array.isArray(data.dish_booking_ids) || data.dish_booking_ids.length !== data.added_dishes
    || data.dish_booking_ids.some(id => !Number.isSafeInteger(id) || id <= 0)
    || !Number.isInteger(data.added_quantity) || data.added_quantity !== entry.dishes.reduce((sum, row) => sum + row.dish_quantity, 0)
    || new Set(data.dish_booking_ids.map(String)).size !== data.dish_booking_ids.length) {
    throw new Error('提交回执未能核对，原记录已保留，请重新读取核对')
  }
  return data
}

// A read receipt has no payload hash: match its exact row IDs and original dish/qty,
// never infer commitment from an order total or a count of existing rows.
export function receiptRowsMatch(receipt, entry, rows) {
  if (!Array.isArray(rows)) return false
  const actual = []
  for (const id of receipt.dish_booking_ids) {
    const matches = rows.filter(row => String(row.dish_booking_id) === String(id))
    if (matches.length !== 1 || typeof matches[0].dish_id !== 'string') return false
    actual.push({ dish_id: matches[0].dish_id.trim().normalize('NFC'), dish_quantity: matches[0].dish_quantity })
  }
  const sorted = list => [...list].sort((a,b) => a.dish_id < b.dish_id ? -1 : a.dish_id > b.dish_id ? 1 : 0)
  return JSON.stringify(sorted(actual)) === JSON.stringify(sorted(entry.dishes))
}

export function batchFailureMessage(body, entry) {
  const data = body?.data
  if (body?.code === 409 && data?.client_request_id === entry.client_request_id && data.booking_id === entry.scope.booking_id) {
    const messages = {
      request_conflict: '提交编号与菜品数量冲突，原记录已保留，请联系管理员核对',
      booking_closed: '订单已结束，原批次仍待核对，请联系管理员处理',
      dish_unavailable: '本批菜品已下架，未新增菜品；原记录已保留，请服务员核对',
      dish_price_invalid: '本批菜品价格无效，未新增菜品；原记录已保留，请服务员核对'
    }
    if (messages[data.error_code]) return messages[data.error_code]
  }
  return '本批结果未确认，已保留原菜品与数量；请核对原批次'
}

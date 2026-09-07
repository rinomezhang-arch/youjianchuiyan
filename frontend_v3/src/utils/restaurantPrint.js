export const PRINT_TYPES = { network: '网络', usb: 'USB', bluetooth: '蓝牙', browser: '浏览器' }
export const DOCUMENT_TYPES = { receipt: '前台小票', kitchen: '厨房单', refund: '退款单', daily_report: '日报' }
export const PRINT_NOTE = '这里只保存打印配置，不连接设备或发送打印任务。配置启用不代表已实际生效。'
const positive = value => Number.isSafeInteger(Number(value)) && Number(value) > 0 && /^\d+$/.test(String(value))
export function printScope(auth, selected) {
  if (!positive(auth?.user?.staffId)) throw new Error('无法确认登录身份')
  const gm = auth.storeId === 0 || auth.storeId === '0' || ['gm', 'super_admin', 'admin'].includes(auth.user.role)
  const sid = gm ? (positive(selected) ? Number(selected) : null) : (positive(auth.storeId) ? Number(auth.storeId) : null)
  return { gm, sid }
}
function name(value) {
  if (typeof value !== 'string' || !value.trim() || value.trim().length > 80) throw new Error('名称须为1至80个字符')
  return value.trim()
}
export function printPayload(kind, draft, sid, archive = false) {
  if (!positive(sid)) throw new Error('请选择有效门店')
  if (!['printers', 'rules'].includes(kind)) throw new Error('配置类型无效')
  const data = { storeId: Number(sid) }
  if (draft.id !== undefined) {
    if (!positive(draft.id)) throw new Error('配置编号无效')
    data.id = Number(draft.id)
  }
  if (archive) {
    if (!data.id) throw new Error('只有已保存记录可以归档')
    return { ...data, archive: true }
  }
  data.name = name(draft.name)
  if (kind === 'printers') {
    if (!Object.hasOwn(PRINT_TYPES, draft.type) || !['58', '80', 'A4'].includes(draft.paperWidth)) throw new Error('打印类型或纸张规格无效')
    if (!Number.isInteger(draft.copies) || draft.copies < 1 || draft.copies > 5) throw new Error('份数须为1至5的整数')
    if (typeof draft.address !== 'string' || draft.address.trim().length > 255) throw new Error('设备地址最多255个字符')
    Object.assign(data, { type: draft.type, paperWidth: draft.paperWidth, copies: draft.copies, address: draft.address.trim() })
  } else {
    if (!positive(draft.printerId) || !Object.hasOwn(DOCUMENT_TYPES, draft.documentType) || typeof draft.configuredEnabled !== 'boolean') throw new Error('请选择打印机和有效分类，启用值须为布尔值')
    Object.assign(data, { printerId: Number(draft.printerId), documentType: draft.documentType, configuredEnabled: draft.configuredEnabled })
  }
  return data
}
function responseData(response) {
  if (response?.code !== 200) throw new Error(response?.message || '打印配置请求失败')
  return response.data
}
function validatedRows(response, sid) {
  const rows = responseData(response)
  if (!Array.isArray(rows) || rows.some(row => !positive(row.id) || Number(row.storeId) !== sid || typeof row.archive !== 'boolean')) throw new Error('返回配置范围或格式不一致')
  if (new Set(rows.map(row => String(row.id))).size !== rows.length) throw new Error('返回配置编号重复')
  return rows
}
export function createRestaurantPrintActions(state, request) {
  let version = 0
  function invalidate(sid) {
    version++; Object.assign(state, { sid, printers: [], rules: [], ready: false, busy: '', error: '', notice: '', expected: null, uncertain: false })
  }
  async function read(sid) {
    const responses = await Promise.all([request.get('/restaurant-print/printers', { params: { storeId: sid } }), request.get('/restaurant-print/rules', { params: { storeId: sid } })])
    return { printers: validatedRows(responses[0], sid), rules: validatedRows(responses[1], sid) }
  }
  function accept(data) {
    Object.assign(state, data)
    if (state.expected) {
      const { kind, id, payload } = state.expected
      const row = data[kind].find(item => Number(item.id) === id)
      if (!row || Object.entries(payload).some(([key, value]) => row[key] !== value)) throw new Error('保存回读未匹配，请核对配置后刷新；不要重复新增')
      state.expected = null
    }
    state.ready = true
  }
  async function load() {
    if (state.busy) return false
    if (!positive(state.sid)) { state.error = '请选择有效门店后查询'; return false }
    const current = ++version, sid = Number(state.sid)
    state.busy = 'load'; state.ready = false; state.error = ''
    try {
      const data = await read(sid)
      if (current !== version || state.sid !== sid) return false
      accept(data); return true
    } catch (error) { if (current === version) state.error = error.response?.data?.message || error.message; return false }
    finally { if (current === version) state.busy = '' }
  }
  async function save(kind, draft, archive = false, confirm = async () => true) {
    if (state.busy || !state.ready || state.uncertain || state.expected) return false
    let payload
    try {
      if (draft.archive) throw new Error('已归档记录不可修改或恢复')
      payload = printPayload(kind, draft, state.sid, archive)
      if (payload.id && !state[kind].some(row => Number(row.id) === payload.id && !row.archive)) throw new Error('请先刷新当前门店记录')
      if (kind === 'rules' && !archive && !state.printers.some(p => Number(p.id) === payload.printerId && !p.archive)) throw new Error('规则须选择当前门店未归档打印机')
    } catch (error) { state.error = error.message; return false }
    const current = ++version, sid = state.sid
    state.busy = 'save'; state.error = ''; state.notice = ''
    let sent = false
    try {
      try { if (!await confirm()) return false } catch { return false }
      if (version !== current || sid !== state.sid) return false
      sent = true
      const data = responseData(await request.post(`/restaurant-print/${kind}`, payload))
      if (version !== current) return false
      if (!positive(data?.id) || Number(data.storeId) !== sid || (payload.id && Number(data.id) !== payload.id)) throw new Error('保存回执不完整，请先核对记录')
      state.expected = { kind, id: Number(data.id), payload }
      state.ready = false
      const readback = await read(sid)
      if (version !== current || sid !== state.sid) return false
      accept(readback)
      state.notice = archive ? '已归档并回读，历史记录保留。' : '配置已保存并回读；设备连接与实际打印尚未验证。'
      return true
    } catch (error) {
      if (version === current) {
        const status = error.response?.status
        const definitive = [400, 403, 409].includes(status) && error.response.data?.code === status
        if (sent && !definitive) { state.ready = false; if (!state.expected) state.uncertain = true }
        state.error = `${error.response?.data?.message || error.message || '保存失败'}${sent && !definitive ? '；结果未确认，输入已保留，请查询核对，不要重复新增。' : ''}`
      }
      return false
    } finally { if (version === current) state.busy = '' }
  }
  return { load, save, invalidate }
}

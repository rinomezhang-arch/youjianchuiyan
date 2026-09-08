// 幂等提交与恢复。要害只有一条：
// **请求号先落盘，再发请求。** 落不了盘就不许发——否则响应一丢，这笔钱到底记没记就永远说不清。
//
// 与应付那套（payableCreation.js / payableSettlement.js）同一套骨架，但不复用它们的文件：
// 应收与收款的回执结构、单号规则、必填规则都不一样，硬套只会把两边都拧变形。

const JOURNAL_VERSION = 1

// 同一个 storage 上的多个实例共享一把"正在飞"的锁。跨标签页/跨进程靠服务端幂等兜底，
// storage 是恢复日志，不是锁——别把它当锁用。
const flights = new WeakMap()

/**
 * @param storage       sessionStorage 之类；只需 getItem/setItem/removeItem
 * @param scope         身份+门店的作用域串，换人或切店必须换 scope
 * @param storeId       当前门店
 * @param op            'receivable' | 'payment'，两条流水各自独立恢复
 * @param send          (payload) => Promise<{code,data}>，由调用方注入，便于用合成 API 测试
 * @param buildPayload  (draft, {storeId, requestId}) => payload
 * @param validate      (data, payload) => boolean，回执校验
 * @param idField       回执里的主键字段名：receivableId / paymentId
 * @param noField       列表行里的单号字段名：receivable_no / payment_no
 * @param validateRow   (row, payload) => boolean，列表行与业务请求的关联校验
 * @param uuid          请求号生成器
 */
export function createIdempotentOperation({
  storage, scope, storeId, op, send, buildPayload, validate, validateRow, idField, noField,
  uuid = () => globalThis.crypto.randomUUID()
}) {
  if (!scope || typeof scope !== 'string') throw new Error('请先确认当前身份与门店')
  if (!Number.isSafeInteger(storeId) || storeId <= 0) throw new Error('请先选择有效门店')
  if (typeof send !== 'function') throw new Error('缺少发送通道')

  const key = `receivable-ledger:${op}:${scope}`
  if (!flights.has(storage)) flights.set(storage, new Set())
  const active = flights.get(storage)

  function pending() {
    const raw = storage.getItem(key)
    if (!raw) return null
    let saved
    try {
      saved = JSON.parse(raw)
    } catch {
      throw new Error(damaged())
    }
    // 作用域或门店对不上：这条不属于当前身份，绝不拿来重发
    if (saved?.version !== JOURNAL_VERSION || saved.op !== op || saved.scope !== scope ||
        !saved.payload || saved.payload.storeId !== storeId) {
      throw new Error(damaged())
    }
    // 存进去的 payload 必须还能原样通过契约校验，改过就不认
    try {
      const rebuilt = buildPayload(saved.payload, { storeId, requestId: saved.payload.requestId })
      if (JSON.stringify(sorted(rebuilt)) !== JSON.stringify(sorted(saved.payload))) throw new Error()
    } catch {
      throw new Error(damaged())
    }
    if (saved.receipt !== undefined && !validate(saved.receipt, saved.payload)) throw new Error(damaged())
    return saved
  }

  function damaged() {
    return op === 'receivable'
      ? '应收创建的恢复记录已损坏，原条目保留；请到列表按单号核对后再决定，写入已锁定'
      : '收款登记的恢复记录已损坏，原条目保留；请到流水按单号核对后再决定，写入已锁定'
  }

  function persist(raw) {
    storage.setItem(key, raw)
    // 存储不可用（隐私模式、配额满）时静默失败最危险：请求发出去了却没有恢复凭据
    if (storage.getItem(key) !== raw) throw new Error('恢复记录未能可靠保存，已阻止本次发送')
  }

  async function transmit(saved, fresh) {
    if (active.has(key)) throw new Error('上一笔正在提交，请稍候')
    const raw = JSON.stringify(saved)
    persist(raw)                      // 重发也要先确认盘上还在，存储失效时连恢复都不许发
    active.add(key)
    try {
      const response = await send({ ...saved.payload })
      const data = response?.data
      const ok = response?.code === 200 && validate(data, saved.payload) &&
        (!saved.receipt || saved.receipt[idField] === data[idField])
      if (!ok) throw new Error('回执不完整或与本次请求不符，恢复记录已保留，请按单号核对后再处理')
      if (storage.getItem(key) !== raw) throw new Error('恢复记录在提交过程中被改动，请重新核对')
      // 留着主键直到与列表核对上：重发必须拿回同一个 id，拿回别的就是出事了
      persist(JSON.stringify({ ...saved, receipt: pickReceipt(data) }))
      return data
    } catch (error) {
      // 首次请求被 400/403 明确拒绝 = 服务端根本没记账，日志可以清。
      // 重发时的同样状态码**说明不了**当初那次超时的请求有没有落库，必须留着。
      // 409 一律留：可能是同键改参（原单已存在），也可能是锁竞争（结果未知），两种都不能清。
      const status = error?.response?.status
      if (fresh && [400, 403].includes(status) && error.response?.data?.code === status &&
          storage.getItem(key) === raw) {
        storage.removeItem(key)
      }
      throw error
    } finally {
      active.delete(key)
    }
  }

  function pickReceipt(data) {
    return {
      requestId: data.requestId,
      [idField]: data[idField],
      no: data.no,
      replayed: data.replayed,
      snapshot: data.snapshot
    }
  }

  return {
    key,
    pending,

    /** 新提交。有未确认的旧请求时一律拒绝——先把上一笔的结果弄清楚，别叠着发。 */
    async submit(draft) {
      if (active.has(key)) throw new Error('上一笔正在提交，请稍候')
      if (pending()) throw new Error('上一次的结果尚未确认，请先恢复原请求或按单号核对，不可重复发送')
      const requestId = uuid()
      const payload = buildPayload(draft, { storeId, requestId })
      return transmit({ version: JOURNAL_VERSION, op, scope, payload }, true)
    },

    /**
     * 恢复。**不接受任何参数**：改了参数就是另一笔业务，必须走新的请求号，
     * 沿用旧键会被服务端判为同键改参直接 409。
     */
    async resume() {
      if (active.has(key)) throw new Error('上一笔正在提交，请稍候')
      const saved = pending()
      if (!saved) throw new Error('没有待恢复的请求')
      return transmit(saved, false)
    },

    /**
     * 与列表核对。按单号找：单号由请求号推导，同一个键无论发几次都是同一个单号。
     * 只有确实核对上了才清恢复记录。
     */
    reconcile(rows) {
      const saved = pending()
      if (!saved) return 'none'
      if (active.has(key)) return 'waiting'
      if (!Array.isArray(rows)) throw new Error('列表数据格式不正确')
      const raw = storage.getItem(key)
      const expectedNo = saved.payload[op === 'receivable' ? 'receivableNo' : 'paymentNo']
      const matches = rows.filter(row => row?.[noField] === expectedNo)
      if (!matches.length) return 'missing'
      if (matches.length > 1) return 'ambiguous'
      const row = matches[0]
      const id = Number(row[idField === 'receivableId' ? 'receivable_id' : 'payment_id'])
      if (Number(row.store_id) !== storeId || !Number.isSafeInteger(id) || id <= 0) return 'mismatch'
      if (saved.receipt && saved.receipt[idField] !== id) return 'mismatch'
      if (typeof validateRow === 'function' && !validateRow(row, saved.payload)) return 'mismatch'
      if (storage.getItem(key) !== raw) throw new Error('恢复记录在核对过程中被改动，请重新核对')
      storage.removeItem(key)
      return 'confirmed'
    },

    /** 恢复记录损坏时的唯一出路：人工核对后手动丢弃。不在任何自动路径里调用。 */
    discardAfterManualCheck() {
      storage.removeItem(key)
    }
  }
}

function sorted(obj) {
  return Object.fromEntries(Object.keys(obj).sort().map(k => [k, obj[k]]))
}

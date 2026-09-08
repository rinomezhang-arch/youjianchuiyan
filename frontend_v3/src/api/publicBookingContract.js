// 公开预订相关的**纯契约逻辑**：无任何 import，可以直接被 node 单测。
//
// 单独成文件不是洁癖：这些规则（统一空结果、字段白名单、三者必填、replayed 如实显示）
// 是这轮的要害，必须能脱离 Vue 与 axios 独立验证。
// 一旦它们只能跟着组件跑，验证成本就会高到没人验。
/** 查不到时对客人说的话。**所有失败原因共用这一句**，不区分。 */
export const LOOKUP_NOT_FOUND =
  '没有查到对应的预订。请确认手机号与预订单号都填写正确，两者需与预订时一致。'

/** 后端返回的字段白名单；界面只认这六个，多的不显示，少的按"--"处理。 */
export const LOOKUP_FIELDS = [
  'booking_id', 'booking_date', 'booking_time',
  'guest_count', 'table_count', 'booking_status'
]

const PHONE = /^1[3-9]\d{9}$/

/**
 * 提交前的本地校验。
 *
 * 注意：这里**只判"两个都填了、手机号像手机号"**，不替后端判断存在性。
 * 而且校验不通过时给的仍是与"查不到"完全一样的措辞吗？——不是。
 * 本地校验是在告诉用户"你还没填完"，这跟"查不到"是两回事，用户需要知道怎么改。
 * 真正必须统一的是**后端返回之后**的各种失败，那些一律用 LOOKUP_NOT_FOUND。
 */
export function validateLookupInput(phone, bookingId) {
  const p = String(phone ?? '').trim()
  const b = String(bookingId ?? '').trim()
  if (!p && !b) return { ok: false, message: '请填写预订手机号与预订单号' }
  if (!p) return { ok: false, message: '请填写预订手机号' }
  if (!b) return { ok: false, message: '请填写预订单号' }
  if (!PHONE.test(p)) return { ok: false, message: '手机号格式不正确，请填写 11 位手机号' }
  return { ok: true, payload: { phone: p, bookingId: b } }
}

/**
 * 把后端响应折成界面状态。
 *
 * 后端对所有查不到的情况返回 `data: null`，这里也只产出一种 `notFound`，
 * **不去解析 message 做任何细分**——那等于把后端刻意抹平的差异又找回来。
 */
export function readLookupResult(response) {
  const data = response && response.code === 200 ? response.data : null
  if (!data || typeof data !== 'object') return { state: 'notFound', message: LOOKUP_NOT_FOUND }
  const view = {}
  for (const key of LOOKUP_FIELDS) view[key] = data[key] ?? null
  return { state: 'found', booking: view }
}

const STATUS_TEXT = {
  confirmed: '已确认',
  pending: '待确认',
  completed: '已完成',
  cancelled: '已取消'
}

export function bookingStatusText(status) {
  return STATUS_TEXT[status] || status || '--'
}

/** 员工转单的提交前校验：三者必填，**不替员工选桌**。 */
export function validateConvertInput(draft) {
  const date = String(draft?.bookingDate ?? '').trim()
  const time = String(draft?.bookingTime ?? '').trim()
  const tables = Array.isArray(draft?.tableIds) ? draft.tableIds : []
  if (!date) return { ok: false, message: '请选择预订日期' }
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date)) return { ok: false, message: '预订日期格式应为 yyyy-MM-dd' }
  if (!time) return { ok: false, message: '请选择到店时间' }
  if (!/^\d{2}:\d{2}$/.test(time)) return { ok: false, message: '到店时间格式应为 HH:mm' }
  if (!tables.length) return { ok: false, message: '请为该咨询指定桌台，系统不会自动分配' }
  const ids = []
  for (const raw of tables) {
    const n = Number(raw)
    if (!Number.isSafeInteger(n) || n <= 0) return { ok: false, message: '桌台编号不正确' }
    if (!ids.includes(n)) ids.push(n)
  }
  // 排序让 payload 确定：同样一组桌台无论员工按什么顺序输入，发出去的都一样。
  // 后端也按 table_id 升序加锁（避免交叉持锁死锁），两边同序也少一层心智负担。
  ids.sort((a, b) => a - b)
  return { ok: true, payload: { bookingDate: date, bookingTime: time, tableIds: ids } }
}

/**
 * 转单结果。
 *
 * `replayed: true` 表示这条咨询此前已经转过，后端把原单还了回来。
 * **界面必须如实说明**，不能伪装成一次新成功——员工会据此以为自己刚建了一张单，
 * 转头又去建第二张。
 */
export function readConvertResult(response) {
  if (!response || response.code !== 200 || !response.data) {
    return { state: 'failed', message: (response && response.message) || '转换失败，请重试' }
  }
  const { bookingId, replayed } = response.data
  if (!bookingId) return { state: 'failed', message: '后端未返回预订单号，请核对后重试' }
  return replayed
    ? { state: 'replayed', bookingId, message: `该咨询此前已转为正式预订，单号 ${bookingId}，未重复建单` }
    : { state: 'converted', bookingId, message: `已转为正式预订，单号 ${bookingId}` }
}

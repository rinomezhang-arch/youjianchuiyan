// 营销发布与客人 H5 前端契约层（TR-MARKETING-H5-UI-39）。
//
// ⚠️ MOCKED_CONTRACT：以下 /marketing/** 与 /public/marketing/** 路径严格按
// docs/营销H5生产闭环设计_20260913.md 第四节在前端先行落地，后端尚未接通。
// 组件测试（tests/marketing）通过 axios adapter 替换网络层，浏览器证据通过
// Playwright 路由级 fulfill 提供合同响应——均为 MOCKED_CONTRACT，不是真实
// HTTP/数据库闭环。真实联调以后端任务为准。
//
// 纪律（对应任务 forbidden）：
// - 不提供物理删除封装（设计第四节：DELETE /activities/{id} 已改为状态取消）。
// - 门店不默认 1：创建草稿的 storeId 必须由调用方显式传入。
// - 公开咨询只提交 sourceCode + 表单字段 + 幂等 requestId，绝不携带 storeId。
// - 成功只认后端业务成功响应（code===200 由 utils/request 拦截器统一判定）。
import request from '@/utils/request'

/* ============================ 常量与纯契约逻辑（可直接 node 单测） ============================ */

export const MARKETING_CHANNELS = Object.freeze(['h5'])

// 工作台四栏：草稿 / 待审批 / 已发布 / 已结束
export const WORKBENCH_COLUMNS = Object.freeze([
  { key: 'draft', title: '草稿', statuses: ['draft', 'changes_requested'] },
  { key: 'approval', title: '待审批', statuses: ['pending_approval', 'approved'] },
  { key: 'published', title: '已发布', statuses: ['published'] },
  { key: 'ended', title: '已结束', statuses: ['paused', 'expired', 'cancelled'] }
])

export const STATUS_TEXT = Object.freeze({
  draft: '草稿',
  changes_requested: '驳回待改',
  pending_approval: '待审批',
  approved: '已批准待发布',
  published: '已发布',
  paused: '已暂停',
  expired: '已过期',
  cancelled: '已取消'
})

export const CHANNEL_TEXT = Object.freeze({ h5: 'H5', wecom: '企微' })

/** 活动归入工作台哪一栏；未知状态归草稿栏但不伪装状态名。 */
export function columnOfStatus(status) {
  for (const col of WORKBENCH_COLUMNS) {
    if (col.statuses.includes(status)) return col.key
  }
  return 'draft'
}

/** 后端行（snake/camel 都可能）规范化为前端视图模型。 */
export function normalizeActivity(raw) {
  if (!raw || typeof raw !== 'object') return null
  const pub = raw.latestPublication ?? raw.latest_publication ?? null
  return {
    id: raw.id,
    activityName: raw.activityName ?? raw.activity_name ?? '',
    activityCode: raw.activityCode ?? raw.activity_code ?? '',
    storeId: raw.storeId ?? raw.store_id ?? null,
    storeName: raw.storeName ?? raw.store_name ?? '',
    status: raw.status || 'draft',
    documentVersion: raw.documentVersion ?? raw.document_version ?? 1,
    rowVersion: raw.rowVersion ?? raw.row_version ?? 0,
    validFrom: raw.validFrom ?? raw.valid_from ?? '',
    validTo: raw.validTo ?? raw.valid_to ?? '',
    channel: raw.channel ?? pub?.channel ?? '',
    publicSlug: pub?.publicSlug ?? pub?.public_slug ?? raw.publicSlug ?? raw.public_slug ?? '',
    sourceCode: pub?.sourceCode ?? pub?.source_code ?? raw.sourceCode ?? raw.source_code ?? '',
    latestPublication: pub
      ? {
          publicationId: pub.publicationId ?? pub.publication_id,
          version: pub.version,
          channel: pub.channel,
          publicSlug: pub.publicSlug ?? pub.public_slug,
          sourceCode: pub.sourceCode ?? pub.source_code,
          status: pub.status,
          validFrom: pub.validFrom ?? pub.valid_from,
          validTo: pub.validTo ?? pub.valid_to
        }
      : null
  }
}

/**
 * 公开快照在指定时刻是否可见。唯一权威：publication.status==='published' 且
 * 当前时间落在 [validFrom, validTo]。草稿/未审批/暂停/过期全部不可见。
 */
export function isPubliclyVisible(snapshot, now = new Date()) {
  if (!snapshot || snapshot.status !== 'published') return false
  const t = now.getTime()
  const from = snapshot.validFrom ? new Date(snapshot.validFrom).getTime() : -Infinity
  const to = snapshot.validTo ? new Date(snapshot.validTo).getTime() : Infinity
  return t >= from && t <= to
}

/**
 * 把 GET /public/marketing/a/{slug} 的合同响应读成 H5 六态之一：
 * success / paused / expired / not_found / bad_response。
 * network_error 在调用处由 axios 异常判定（拿不到任何响应）。
 *
 * 合同约定（MOCKED_CONTRACT，待后端确认）：
 * - 可公开：{ code:200, data:{ status:'published', validFrom, validTo, ... } }
 * - 暂停/过期：{ code:200, data:{ status:'paused'|'expired' } }，仅返回状态原因，
 *   不含预算/操作人等内部字段；
 * - 草稿/未审批/跨店/不存在：统一 404 空结果，前端不得区分试探。
 */
export function readPublicActivity(payload, now = new Date()) {
  const data = payload && (payload.data ?? payload)
  if (!data || typeof data !== 'object') return { state: 'not_found' }
  if (data.status === 'paused') return { state: 'paused', data }
  if (data.status === 'expired') return { state: 'expired', data }
  if (isPubliclyVisible(data, now)) return { state: 'success', data }
  if (data.status === 'published') return { state: 'expired', data }
  return { state: 'not_found' }
}

/**
 * 构造咨询提交体：只含 sourceCode、客人填写的表单字段与幂等 requestId。
 * 显式 delete storeId——客户端门店不被信任，服务端凭 sourceCode 反查发布得门店。
 */
export function buildInquiryPayload(form, sourceCode, requestId) {
  const payload = {
    sourceCode,
    requestId,
    customerName: (form.customerName || '').trim(),
    phone: (form.phone || '').trim(),
    expectedDate: form.expectedDate || '',
    partySize: form.partySize ? Number(form.partySize) : null,
    remark: (form.remark || '').trim()
  }
  delete payload.storeId
  delete payload.store_id
  return payload
}

/** 咨询表单校验；返回字段错误映射，空对象表示可提交。 */
export function validateInquiryForm(form) {
  const errors = {}
  if (!form.customerName || !String(form.customerName).trim()) errors.customerName = '请填写称呼'
  const phone = String(form.phone || '').trim()
  if (!/^1\d{10}$/.test(phone)) errors.phone = '请填写 11 位手机号'
  if (!form.expectedDate) errors.expectedDate = '请选择期望日期'
  const n = Number(form.partySize)
  if (!form.partySize || !Number.isInteger(n) || n < 1 || n > 99) errors.partySize = '人数为 1-99 的整数'
  return errors
}

/** 后台活动表单校验：门店必选且不得默认 1 由调用方保证不预填。 */
export function validateActivityForm(form) {
  const errors = {}
  if (!form.storeId) errors.storeId = '必须选择门店'
  if (!form.activityName || !String(form.activityName).trim()) errors.activityName = '请填写活动名称'
  if (!form.activityCode || !String(form.activityCode).trim()) errors.activityCode = '请填写活动编码'
  if (!form.validFrom || !form.validTo) {
    errors.validRange = '请选择完整有效期'
  } else if (new Date(form.validTo) < new Date(form.validFrom)) {
    errors.validRange = '结束日期不能早于开始日期'
  }
  if (!form.publicTitle || !String(form.publicTitle).trim()) errors.publicTitle = '请填写客人看到的标题'
  return errors
}

/** 幂等键生成：浏览器原生 crypto 的全局唯一标识；提交失败重试沿用同一个。 */
export function newRequestId() {
  if (globalThis.crypto?.randomUUID) return globalThis.crypto.randomUUID()
  return 'req-' + Date.now().toString(36) + '-' + Math.random().toString(36).slice(2, 10)
}

/**
 * 真实门店照片（只允许引用构建产物中已存在的 /site-photos/ 真实资产，
 * 不接受外部 URL，也不生成图片）。发布内容 heroAssetUrl 优先，其次按门店兜底。
 */
const REAL_STORE_PHOTOS = Object.freeze([
  '/site-photos/storefront-entrance.jpg',
  '/site-photos/storefront-dusk.jpg',
  '/site-photos/terrace-dining-real.jpg'
])

export function resolveHeroAsset(snapshot) {
  const hero = snapshot && (snapshot.heroAssetUrl ?? snapshot.hero_asset_url)
  if (typeof hero === 'string' && /^\/site-photos\/[a-z0-9-]+\.(jpg|jpeg|png|webp)$/i.test(hero)) {
    return hero
  }
  // 两家既有门店分别落两张真实门店照片；其余用真实露台餐区照片。
  const id = Number(snapshot?.storeId ?? snapshot?.store_id)
  if (id === 2) return REAL_STORE_PHOTOS[1]
  if (id >= 3) return REAL_STORE_PHOTOS[2]
  return REAL_STORE_PHOTOS[0]
}

/* ============================ 内部接口（工作台，需登录态） ============================ */

/**
 * 工作台列表：门店范围由服务端按实时身份决定，前端不传 storeId。
 * 显式给 null 是为了阻止 utils/request 的 GET 拦截器兜底注入 storeId=1。
 */
export function listMarketingActivities() {
  return request({ url: '/marketing/activities', method: 'get', params: { storeId: null } })
}

/** 既有门店列表（组织管理同款接口），供跨店总经理显式选择门店。 */
export function listStoresForMarketing() {
  return request.get('/api/stores')
}

/** 创建草稿。门店范围用户可不带 storeId（身份取店）；跨店总经理必须显式传合法 storeId。 */
export function createMarketingActivity(payload) {
  return request({ url: '/marketing/activities', method: 'post', data: payload })
}

/** 仅草稿可改；必须带 expectedRowVersion 乐观锁；门店/编码不可改（后端兜底）。 */
export function updateMarketingActivity(id, payload) {
  return request({ url: `/marketing/activities/${id}`, method: 'put', data: payload })
}

/** 提交既有审批流：business_type=marketing_activity + documentVersion。 */
export function submitMarketingActivity(id, payload) {
  return request({ url: `/marketing/activities/${id}/submit`, method: 'post', data: payload })
}

/**
 * 发布：只接受已批准的同一版本。必须带 channel、公开内容快照与 requestId。
 * 前端只在 code===200 时认定成功。
 */
export function publishMarketingActivity(id, payload) {
  return request({ url: `/marketing/activities/${id}/publish`, method: 'post', data: payload })
}

/** 暂停公开展示；重复请求由后端返回原结果（幂等）。 */
export function pauseMarketingPublication(publicationId) {
  return request({ url: `/marketing/publications/${publicationId}/pause`, method: 'post' })
}

/** 取消=状态取消，不物理删除；已有发布记录时由后端拒绝。 */
export function cancelMarketingActivity(id) {
  return request({ url: `/marketing/activities/${id}/cancel`, method: 'post' })
}

/** 转化数据（MOCKED_CONTRACT 合同入口，归因报表由后端任务实现）。 */
export function getMarketingAttribution(publicationId) {
  return request({ url: `/marketing/publications/${publicationId}/attribution`, method: 'get' })
}

/* ============================ 公开接口（客人 H5，免登录） ============================ */

/** 单个公开快照；草稿/未审批/暂停/过期/跨店由后端按第四节统一处理。 */
export function getPublicMarketingActivity(publicSlug) {
  // 公开读取只按 slug，绝不携带门店参数。显式 storeId=null 一箭双雕：
  // 1) utils/request 的 GET 拦截器只在 storeId===undefined 时兜底注入默认门店，null 会被原样跳过；
  // 2) axios 默认序列化不输出 null 参数——真实请求 URL 不含 ?storeId=...（R1 评审项1）。
  return request({
    url: `/public/marketing/a/${encodeURIComponent(publicSlug)}`,
    method: 'get',
    params: { storeId: null }
  })
}

/** 脱敏浏览事件（view），requestId 幂等；埋点失败不阻断浏览。 */
export function recordMarketingEvent(payload) {
  return request({ url: '/public/marketing/events', method: 'post', data: payload })
}

/**
 * 客人咨询：允许附 sourceCode（既有公开咨询接口，设计第四节）。
 * 调用方使用 buildInquiryPayload 构造——不含 storeId。
 */
export function submitPublicInquiry(payload) {
  return request({ url: '/public/booking-inquiry', method: 'post', data: payload })
}

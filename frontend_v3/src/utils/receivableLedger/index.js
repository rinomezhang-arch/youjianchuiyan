// 应收与手工收款的前端闭环入口。组件只跟这里打交道，不直接拼请求体。

import { createIdempotentOperation } from './pending.js'
import {
  buildReceivablePayload, buildPaymentPayload,
  validateReceivableReceipt, validatePaymentReceipt,
  validateReceivableListRow, validatePaymentListRow
} from './contract.js'
import { centsOf, centsText, displayAmount } from './money.js'

export { displayAmount } from './money.js'

export const STATUS_TEXT = {
  unpaid: '未收款',
  partial: '部分收款',
  paid: '已收清'
}

/**
 * 作用域串：身份或门店一变，恢复记录就必须隔离，
 * 否则换个人登录会把别人的未确认请求当成自己的重发出去。
 */
export function ledgerScope({ staffId, storeId }) {
  const id = String(staffId ?? '').trim()
  const store = Number(storeId)
  if (!id) throw new Error('未取得登录身份，无法进行应收与收款操作')
  if (!Number.isSafeInteger(store) || store <= 0) throw new Error('请先选择有效门店')
  return `${id}@${store}`
}

/**
 * @param api  { createReceivable, recordPayment, listReceivables, receivableDetail, listPayments }
 *             全部由调用方注入：组件注入真实 request.js，测试注入合成 API。
 */
export function createReceivableLedger({ storage, staffId, storeId, api, uuid }) {
  const store = Number(storeId)
  const scope = ledgerScope({ staffId, storeId: store })

  const receivable = createIdempotentOperation({
    storage, scope, storeId: store, op: 'receivable',
    send: payload => api.createReceivable(payload),
    buildPayload: buildReceivablePayload,
    validate: validateReceivableReceipt,
    validateRow: validateReceivableListRow,
    idField: 'receivableId', noField: 'receivable_no', uuid
  })

  const payment = createIdempotentOperation({
    storage, scope, storeId: store, op: 'payment',
    send: payload => api.recordPayment(payload),
    buildPayload: buildPaymentPayload,
    validate: validatePaymentReceipt,
    validateRow: validatePaymentListRow,
    idField: 'paymentId', noField: 'payment_no', uuid
  })

  return {
    scope,
    storeId: store,
    receivable,
    payment,
    listReceivables: () => api.listReceivables(store),
    receivableDetail: id => api.receivableDetail(id, store),
    listPayments: () => api.listPayments(store)
  }
}

/**
 * 待收金额只用于**显示**。服务端已经把 pending_amount 算好了，这里优先用它；
 * 只有它缺失时才用 总额−已收 顶一下，并且绝不据此判断"能不能收清"——那是服务端的事。
 */
export function pendingText(row) {
  const server = displayAmount(row?.pending_amount)
  if (server !== '--') return server
  try {
    return centsText(centsOf(displayAmount(row?.total_amount)) - centsOf(displayAmount(row?.received_amount)))
  } catch {
    return '--'
  }
}

/**
 * 把后端错误转成给人看的一句话。
 * **不打印任何请求体、凭证或内部细节**；状态码语义直接照六方法的契约来。
 */
export function explainError(error) {
  const status = error?.response?.status
  const message = error?.response?.data?.message
  if (status === 400) return message || '填写内容不符合要求，本次未记账'
  if (status === 403) return message || '当前身份无权操作该门店的应收与收款'
  if (status === 409) return message || '该请求与已有记录冲突，本次未记账，请按单号核对'
  if (status === 401) return '登录已失效，请重新登录后再操作'
  if (status === 500) return '服务端处理失败，本次未记账'
  return error?.message || '网络异常，本次结果未知，请用原请求恢复或按单号核对'
}

/**
 * 结果是否未知。未知就必须"先恢复、不新建"——
 * 网络超时、5xx、以及回执对不上，都属于"这笔可能已经记了"。
 */
export function isUnknownOutcome(error) {
  const status = error?.response?.status
  if (status === undefined) return true          // 网络层失败，压根不知道服务端收没收到
  return ![400, 403].includes(status)            // 400/403 是明确拒绝；409/500 都当未知
}

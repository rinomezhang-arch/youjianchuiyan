import request from '@/utils/request'
import { ElMessage } from 'element-plus'

/**
 * 订单小票真实打印链（TR-RECEIPT-REAL-24）。
 * 替换 printBill 的虚假成功提示：点击 → 同步开预览窗（防弹窗拦截）→
 * request 拉服务端只读快照（Result JSON，显式该单 storeId）→
 * 全部业务文本用 textContent 安全渲染（绝不 innerHTML 插入业务内容）→
 * 窗口内提供「打印 / 另存 PDF」按钮调用 window.print。
 * 任何失败路径都不允许出现“已打印成功”。
 */

// 注意：不能加 noopener —— noopener 会让 window.open 返回 null，
// 调用方将无法向新窗口写入骨架/小票（窗口停在 about:blank）。
// 新窗口内容全部由本模块以 createElement+textContent 写入，业务数据无 innerHTML 注入面。
const WINDOW_FEATURES = ''

/**
 * 点击处理入口。必须在用户手势的同步调用栈内执行：先开窗再发请求，
 * 否则异步弹窗会被浏览器拦截。
 * @param {{billNo?:string, storeId?:number|string}} bill 列表行（含真实 storeId）
 */
export function openReceiptPreview(bill) {
  if (!bill || !bill.billNo) {
    ElMessage.error('账单信息不完整，无法打印小票')
    return
  }
  if (bill.storeId === undefined || bill.storeId === null || bill.storeId === '') {
    ElMessage.error('该账单缺少门店信息，无法获取小票')
    return
  }

  const win = window.open('', '_blank', WINDOW_FEATURES)
  if (!win) {
    ElMessage.error('预览窗口被浏览器拦截，请允许本站弹出窗口后重试')
    return
  }

  writeSkeleton(win, { state: 'loading' })
  void loadAndRender(win, bill)
}

async function loadAndRender(win, bill) {
  try {
    const res = await request.get(`/bills/${encodeURIComponent(bill.billNo)}/receipt`, {
      params: { storeId: bill.storeId }
    })
    const receipt = res && res.data
    if (!receipt || !receipt.orderNo) {
      const empty = new Error('小票数据为空')
      empty.local = true
      throw empty
    }
    if (win.closed) return
    renderReceiptDocument(win.document, receipt)
    ElMessage.info('小票预览已打开，请在预览窗口中打印或另存为 PDF')
  } catch (error) {
    if (win.closed) return
    const message = errorMessageOf(error)
    try {
      writeSkeleton(win, { state: 'error', message })
    } catch (e) {
      // 窗口已被用户关闭等情况：静默，主页面已有错误提示
    }
    // request 拦截器对 HTTP 错误与 code!==200 均已弹 ElMessage.error，
    // 这里仅对“请求成功但数据为空”这类本地错误补一次提示，避免重复弹窗。
    if (error && error.local) {
      ElMessage.error(message)
    }
  }
}

function errorMessageOf(error) {
  return (error && error.message) ? error.message : '小票加载失败，请重试'
}

/* ==================== 安全 DOM 渲染（业务文本一律 textContent） ==================== */

/**
 * 把小票快照渲染进给定 document。纯 DOM API：createElement + textContent，
 * 任何业务字符串都不经过 innerHTML，菜名里夹带 <script>/<img onerror> 只会原样显示。
 * 导出供针对性安全测试调用。
 */
export function renderReceiptDocument(doc, receipt) {
  doc.open()
  doc.write(buildStaticSkeleton())
  doc.close()

  const root = doc.getElementById('receipt-root')
  root.replaceChildren(buildReceiptNode(doc, receipt))
}

function buildReceiptNode(doc, r) {
  const receiptEl = doc.createElement('div')
  receiptEl.className = 'receipt'

  const head = doc.createElement('div')
  head.className = 'r-head'
  const shop = doc.createElement('h1')
  shop.textContent = r.storeName || '门店'
  const title = doc.createElement('p')
  title.className = 'r-title'
  title.textContent = '订单小票'
  head.append(shop, title)
  receiptEl.appendChild(head)

  const meta = doc.createElement('div')
  meta.className = 'r-meta'
  appendMetaRow(doc, meta, '订单号', r.orderNo)
  appendMetaRow(doc, meta, '日期', r.bookingDate)
  if (r.guestCount !== undefined && r.guestCount !== null) {
    appendMetaRow(doc, meta, '人数', `${r.guestCount} 人`)
  }
  receiptEl.appendChild(meta)

  const divider = doc.createElement('div')
  divider.className = 'r-divider'
  divider.textContent = '--------------------------------'
  receiptEl.appendChild(divider)

  const table = doc.createElement('table')
  table.className = 'r-items'
  const thead = doc.createElement('thead')
  const headRow = doc.createElement('tr')
  ;['菜品', '数量', '单价', '小计'].forEach((text) => {
    const th = doc.createElement('th')
    th.textContent = text
    headRow.appendChild(th)
  })
  thead.appendChild(headRow)
  table.appendChild(thead)

  const tbody = doc.createElement('tbody')
  const dishes = Array.isArray(r.dishes) ? r.dishes : []
  dishes.forEach((d) => {
    const tr = doc.createElement('tr')
    ;[d.dishName, formatQty(d.quantity), `¥${money(d.unitPrice)}`, `¥${money(d.subtotal)}`].forEach((text) => {
      const td = doc.createElement('td')
      td.textContent = text
      tr.appendChild(td)
    })
    tbody.appendChild(tr)
  })
  table.appendChild(tbody)
  receiptEl.appendChild(table)

  const totals = doc.createElement('div')
  totals.className = 'r-totals'
  appendTotalRow(doc, totals, '账面合计', `¥${money(r.totalAmount)}`, false)
  appendTotalRow(doc, totals, '应付金额', `¥${money(r.finalAmount)}`, true)
  appendTotalRow(doc, totals, '支付状态', statusLabel(r.status), false)
  receiptEl.appendChild(totals)

  const note = doc.createElement('p')
  note.className = 'r-note'
  note.textContent = '应付金额为服务端实时快照，不代表实收；本小票仅供核对。'
  receiptEl.appendChild(note)

  const actions = doc.createElement('div')
  actions.className = 'r-actions'
  const printBtn = doc.createElement('button')
  printBtn.className = 'r-btn r-btn-primary'
  printBtn.textContent = '打印 / 另存为 PDF'
  printBtn.addEventListener('click', () => doc.defaultView.print())
  const closeBtn = doc.createElement('button')
  closeBtn.className = 'r-btn'
  closeBtn.textContent = '关闭'
  closeBtn.addEventListener('click', () => doc.defaultView.close())
  actions.append(printBtn, closeBtn)
  receiptEl.appendChild(actions)

  return receiptEl
}

function appendMetaRow(doc, container, label, value) {
  if (value === undefined || value === null || value === '') return
  const row = doc.createElement('div')
  row.className = 'r-meta-row'
  const lab = doc.createElement('span')
  lab.className = 'r-meta-label'
  lab.textContent = label
  const val = doc.createElement('span')
  val.textContent = String(value)
  row.append(lab, val)
  container.appendChild(row)
}

function appendTotalRow(doc, container, label, value, strong) {
  const row = doc.createElement('div')
  row.className = strong ? 'r-total-row r-total-strong' : 'r-total-row'
  const lab = doc.createElement('span')
  lab.textContent = label
  const val = doc.createElement('span')
  val.textContent = value
  row.append(lab, val)
  container.appendChild(row)
}

function statusLabel(status) {
  if (status === 'settled') return '已结'
  if (status === 'unsettled') return '未结'
  if (status === 'refunded') return '退款'
  return status || '-'
}

function money(value) {
  const n = Number(value)
  return Number.isFinite(n) ? n.toFixed(2) : '0.00'
}

function formatQty(value) {
  const n = Number(value)
  return Number.isFinite(n) ? (Number.isInteger(n) ? String(n) : n.toFixed(2)) : String(value ?? '')
}

/* ==================== 静态骨架（无业务数据，可直接 write） ==================== */

function writeSkeleton(win, opts) {
  const doc = win.document
  doc.open()
  doc.write(buildStaticSkeleton())
  doc.close()
  const root = doc.getElementById('receipt-root')
  if (opts.state === 'loading') {
    const p = doc.createElement('p')
    p.className = 'r-state'
    p.textContent = '小票加载中…'
    root.replaceChildren(p)
  } else if (opts.state === 'error') {
    const box = doc.createElement('div')
    box.className = 'r-state-box'
    const p = doc.createElement('p')
    p.className = 'r-state r-state-error'
    p.textContent = `小票加载失败：${opts.message || '请关闭窗口后重试'}`
    const btn = doc.createElement('button')
    btn.className = 'r-btn'
    btn.textContent = '关闭'
    btn.addEventListener('click', () => win.close())
    box.append(p, btn)
    root.replaceChildren(box)
  }
}

function buildStaticSkeleton() {
  return `<!doctype html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>小票预览</title>
<style>
  * { box-sizing: border-box; }
  html, body { margin: 0; padding: 0; background: #ececec; font-family: "Microsoft YaHei", "PingFang SC", system-ui, sans-serif; color: #111; }
  #receipt-root { display: flex; justify-content: center; padding: 24px 12px; }
  .receipt { width: 80mm; min-height: 120mm; background: #fff; padding: 6mm 5mm; font-size: 12px; line-height: 1.55; }
  .r-head { text-align: center; margin-bottom: 8px; }
  .r-head h1 { font-size: 17px; margin: 0 0 2px; }
  .r-title { margin: 0; color: #444; font-size: 13px; }
  .r-meta { margin: 6px 0; }
  .r-meta-row { display: flex; justify-content: space-between; gap: 8px; }
  .r-meta-label { color: #666; }
  .r-divider { letter-spacing: -1px; color: #999; margin: 6px 0; }
  .r-items { width: 100%; border-collapse: collapse; margin: 6px 0; }
  .r-items th { text-align: left; font-weight: 600; padding: 2px 0; border-bottom: 1px solid #ddd; }
  .r-items th:nth-child(2), .r-items td:nth-child(2) { text-align: center; width: 12mm; }
  .r-items th:nth-child(3), .r-items td:nth-child(3),
  .r-items th:nth-child(4), .r-items td:nth-child(4) { text-align: right; width: 20mm; }
  .r-items td { padding: 3px 0; vertical-align: top; word-break: break-all; }
  .r-totals { margin-top: 8px; border-top: 1px dashed #999; padding-top: 6px; }
  .r-total-row { display: flex; justify-content: space-between; padding: 2px 0; }
  .r-total-strong { font-size: 16px; font-weight: 700; margin-top: 4px; }
  .r-note { color: #777; font-size: 10px; margin: 8px 0 0; }
  .r-actions { display: flex; gap: 8px; margin-top: 14px; }
  .r-btn { flex: 1; padding: 9px 8px; font-size: 13px; border: 1px solid #bbb; border-radius: 6px; background: #fff; cursor: pointer; }
  .r-btn-primary { background: #2D4A3E; border-color: #2D4A3E; color: #fff; font-weight: 600; }
  .r-state { text-align: center; color: #555; padding: 40px 0; }
  .r-state-box { text-align: center; padding: 30px 10px; }
  .r-state-error { color: #b3261e; }
  @page { size: 80mm auto; margin: 0; }
  @media print {
    html, body { background: #fff; }
    #receipt-root { padding: 0; }
    .receipt { width: 80mm; padding: 4mm; box-shadow: none; }
    .r-actions, .r-state-box .r-btn { display: none; }
  }
</style>
</head>
<body><div id="receipt-root"></div></body>
</html>`
}

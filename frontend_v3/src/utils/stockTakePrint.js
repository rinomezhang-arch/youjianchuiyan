const escapeHtml = value => String(value ?? '—').replace(/[&<>"']/g,
  c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c])

function numberCell(value, places) {
  if (value === null || value === undefined || value === '') return '—'
  if (!['string', 'number'].includes(typeof value) || !String(value).trim() || !Number.isFinite(Number(value))) {
    throw new Error('盘点单数值异常，请核对原单')
  }
  return Number(value).toFixed(places)
}

export function stockTakePrintHtml(payload, expectedTakeId, expectedStoreId) {
  const master = payload?.stockTake
  const rows = payload?.details
  const sameId = (left, right) => left != null && right != null && String(left) === String(right)
  if (!master || !sameId(master.takeId, expectedTakeId) || !sameId(master.storeId, expectedStoreId) ||
      !Array.isArray(rows) || rows.length === 0 || !Number.isInteger(master.totalItems) || master.totalItems !== rows.length ||
      rows.some(row => !row || !sameId(row.takeId, master.takeId) || !sameId(row.storeId, master.storeId))) {
    throw new Error('盘点单关联或明细数量异常，请核对原单后打印')
  }
  if (rows.some(row => !Number.isInteger(row.lineNo) || row.lineNo < 1) || new Set(rows.map(row => row.lineNo)).size !== rows.length) {
    throw new Error('盘点明细序号缺失或重复，请核对原单')
  }
  const columns = ['序号', '原料编码', '原料', '单位', '账面数量', '实盘数量', '差异数量', '差异金额']
  const body = [...rows].sort((a, b) => a.lineNo - b.lineNo).map(row => [row.lineNo, row.ingredientId, row.ingredientName, row.unit,
    numberCell(row.systemQuantity, 3), numberCell(row.actualQuantity, 3),
    numberCell(row.diffQuantity, 3), numberCell(row.diffAmount, 2)])
    .map(cells => `<tr>${cells.map(cell => `<td>${escapeHtml(cell)}</td>`).join('')}</tr>`).join('')
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><title>盘点单 ${escapeHtml(master.takeNo)}</title>
<style>@page{size:A4 portrait;margin:12mm}body{font:12px/1.5 sans-serif;color:#111}h1{font-size:22px}table{width:100%;border-collapse:collapse}th,td{border:1px solid #aaa;padding:5px;overflow-wrap:anywhere}thead{display:table-header-group}tr{break-inside:avoid}td:nth-child(n+5){text-align:right}.meta{display:flex;flex-wrap:wrap;gap:8px 24px}.sign{margin-top:28px;display:flex;justify-content:space-between;break-inside:avoid}.note{color:#555;font-size:11px}</style></head>
<body><h1>库存盘点单</h1><div class="meta"><span>单号：${escapeHtml(master.takeNo)}</span><span>日期：${escapeHtml(master.takeDate)}</span><span>门店编号：${escapeHtml(master.storeId)}</span><span>盘点人：${escapeHtml(master.operatorName)}</span><span>状态：${escapeHtml(({ completed: '已完成', draft: '草稿' })[master.status] || master.status)}</span></div>
<p>原单项数：${escapeHtml(master.totalItems)}　差异项数：${escapeHtml(master.totalDiffItems)}　原单差异金额：${escapeHtml(numberCell(master.totalDiffAmount, 2))}</p>
<table><thead><tr>${columns.map(label => `<th>${label}</th>`).join('')}</tr></thead><tbody>${body}</tbody></table>
<p class="note">按已保存盘点单快照打印；“—”表示原单未记录。此单不代表库存调整或审批已完成。</p>
<p>备注：${escapeHtml(master.remark)}</p><div class="sign"><span>盘点人签字：____________</span><span>复核人签字：____________</span><span>日期：____________</span></div></body></html>`
}

export function printStockTake(html, documentRef = document, onError = () => {}) {
  const frame = documentRef.createElement('iframe')
  frame.title = '盘点单打印'
  frame.style.cssText = 'position:fixed;width:0;height:0;border:0;bottom:0;left:0'
  let cleanupTimer
  const cleanup = () => { clearTimeout(cleanupTimer); frame.remove() }
  frame.onload = () => {
    if (!frame.contentWindow) { cleanup(); onError(); return }
    frame.contentWindow.addEventListener('afterprint', cleanup, { once: true })
    cleanupTimer = setTimeout(cleanup, 300000)
    try { frame.contentWindow.focus(); frame.contentWindow.print() } catch { cleanup(); onError() }
  }
  frame.srcdoc = html
  documentRef.body.appendChild(frame)
}

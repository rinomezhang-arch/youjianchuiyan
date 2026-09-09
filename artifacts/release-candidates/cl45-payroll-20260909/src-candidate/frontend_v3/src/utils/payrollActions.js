export const PAYROLL_FIELDS = [
  ['base_salary', '基本工资'], ['post_salary', '岗位工资'], ['attendance_pay', '出勤绩效'],
  ['overtime_pay', '加班费'], ['bonus', '奖金'], ['allowance', '补贴'],
  ['deduction_social', '社保代扣'], ['deduction_other', '其他扣款']
]
export const PAYROLL_STATUS = { 0: '未保存', 1: '已保存', 2: '已审批', 3: '已发放记账' }
export const PAYROLL_NOTE = '发放记账仅记录工资台账，不执行银行付款，不代表员工银行到账。'
const moneyFields = [...PAYROLL_FIELDS.map(([key]) => key), 'deduction_tax', 'gross_pay', 'net_pay']

function checkMonth(month) {
  if (!/^\d{4}-(0[1-9]|1[0-2])$/.test(month)) throw new Error('月份格式应为 YYYY-MM')
  return month
}
function dataOf(response) {
  if (!response || response.code !== 200) throw new Error(response?.message || '工资请求失败')
  return response.data
}
function rowsOf(response) {
  const data = dataOf(response)
  if (!Array.isArray(data)) throw new Error('工资回读格式无效')
  const seen = new Set()
  return data.map(row => {
    if (!Number.isSafeInteger(Number(row.emp_id)) || Number(row.emp_id) <= 0 || seen.has(String(row.emp_id))) throw new Error('工资员工标识无效或重复')
    seen.add(String(row.emp_id))
    if (![0, 1, 2, 3].includes(Number(row.salary_status)) || row.salary_status == null) throw new Error('工资状态无效')
    for (const key of moneyFields) {
      if (row[key] == null || String(row[key]).trim() === '' || !Number.isFinite(Number(row[key]))) throw new Error('工资金额回读无效')
    }
    return { ...row, salary_status: Number(row.salary_status) }
  })
}
export function payrollPermissions(state) {
  const usable = state.unlocked && state.ready && !state.busy && state.loadedMonth === state.month && state.rows.length > 0
  const statuses = state.rows.map(row => Number(row.salary_status))
  return {
    edit: !!usable && statuses.some(s => s < 2),
    save: !!usable && statuses.some(s => s < 2),
    approve: !!usable && !state.dirty && !statuses.includes(0) && statuses.includes(1),
    payout: !!usable && !state.dirty && statuses.every(s => s >= 2) && statuses.includes(2),
    output: !!usable && !state.dirty
  }
}
export function payrollStatusText(rows) {
  if (!rows.length) return '暂无工资记录'
  const statuses = [...new Set(rows.map(row => Number(row.salary_status)))]
  return statuses.length === 1 ? PAYROLL_STATUS[statuses[0]] : statuses.map(s => `${PAYROLL_STATUS[s]} ${rows.filter(row => Number(row.salary_status) === s).length}人`).join(' / ')
}
export function payrollSavePayload(rows) {
  return rows.filter(row => Number(row.salary_status) < 2).map(row => {
    const item = { emp_id: row.emp_id }
    for (const [key, label] of PAYROLL_FIELDS) {
      const value = String(row[key] ?? '').trim()
      if (!/^\d+(\.\d{1,2})?$/.test(value) || !Number.isSafeInteger(Math.round(Number(value) * 100))) {
        throw new Error(`员工 ${row.emp_id} 的${label}须为非负金额，最多两位小数`)
      }
      item[key] = value
    }
    return item
  })
}

export function createPayrollActions(state, request) {
  let generation = 0
  async function read(month) {
    return rowsOf(await request.get('/hr/payroll', { params: { month } }))
  }
  function apply(rows, month) {
    state.rows = rows; state.loadedMonth = month; state.ready = true; state.dirty = false
  }
  async function refresh() {
    if (state.busy) return false
    const version = ++generation
    const month = state.month
    state.busy = 'refresh'; state.error = ''; state.ready = false
    try {
      checkMonth(month)
      const rows = await read(month)
      if (version !== generation || month !== state.month) return false
      apply(rows, month)
      return true
    } catch (error) {
      if (version === generation) state.error = error.message || '刷新失败，原输入已保留'
      return false
    } finally { if (version === generation) state.busy = '' }
  }
  async function run(action, confirm) {
    if (!['save', 'approve', 'payout'].includes(action) || !payrollPermissions(state)[action]) return false
    const version = ++generation
    const month = state.month
    const targetIds = state.rows.filter(row => action === 'save' ? row.salary_status < 2 : row.salary_status === (action === 'approve' ? 1 : 2)).map(row => String(row.emp_id))
    state.busy = action; state.error = ''; state.notice = ''
    let posted = false
    let acknowledged = false
    try {
      checkMonth(month)
      const payload = action === 'save' ? payrollSavePayload(state.rows) : null
      // Lock the UI before the confirmation dialog, including duplicate clicks.
      try { if (!await confirm(action, month)) return false } catch { return false }
      if (version !== generation || month !== state.month || !state.unlocked) return false
      posted = true
      state.ready = false
      const data = dataOf(await request.post(`/hr/payroll/${action}`, payload, { params: { month } }))
      if (version !== generation) return false
      const countKey = { save: 'saved', approve: 'approved', payout: 'paid' }[action]
      const count = data?.[countKey]
      if (data?.month !== month || !Number.isInteger(count) || count < 0 ||
          (count === 0 && !(action === 'payout' && data.alreadyRecorded === true))) throw new Error('操作回执无效，请刷新核对，勿重复提交')
      if (action === 'save' && count !== targetIds.length) throw new Error('保存回执人数与提交人数不一致，请刷新核对')
      acknowledged = true
      const rows = await read(month)
      if (version !== generation || month !== state.month) return false
      const minimum = { save: 1, approve: 2, payout: 3 }[action]
      if (targetIds.some(id => !rows.some(row => String(row.emp_id) === id && row.salary_status >= minimum))) {
        throw new Error('回读状态与操作回执不一致，请刷新核对')
      }
      apply(rows, month)
      state.notice = action === 'save' ? `已保存 ${count} 人并回读，个税与合计以服务端结果为准。`
        : action === 'approve' ? `已审批 ${count} 人并回读。`
          : data.alreadyRecorded ? '此前已完成发放记账，本次未新增记账。' : `已发放记账 ${count} 人并回读。`
      return true
    } catch (error) {
      if (version === generation) {
        state.error = `${acknowledged ? '操作已返回成功，但回读未确认；请刷新核对，不要重复提交。' : ''}${error.message || '操作失败'}${posted ? ' 原输入已保留，请先刷新核对。' : ''}`
      }
      return false
    } finally { if (version === generation) state.busy = '' }
  }
  function invalidate() { generation++; state.ready = false; state.busy = '' }
  return { refresh, run, invalidate }
}

export function filterPayrollRows(rows, keyword = '', department = '', status = '') {
  const q = keyword.trim().toLowerCase()
  return rows.filter(row => (!q || `${row.emp_id} ${row.emp_name}`.toLowerCase().includes(q)) &&
    (!department || row.department === department) && (status === '' || String(row.salary_status) === String(status)))
}
const columns = [['emp_id', '工号'], ['emp_name', '姓名'], ['department', '部门'], ...PAYROLL_FIELDS,
  ['deduction_tax', '个税代扣'], ['gross_pay', '应发合计'], ['net_pay', '实发合计'], ['salary_status', '状态']]
const cellValue = (row, key) => key === 'salary_status' ? PAYROLL_STATUS[row[key]] : moneyFields.includes(key) ? Number(row[key]).toFixed(2) : row[key] ?? ''
function escapeHtml(value) { return String(value).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' })[c]) }
export function payrollCsv(rows) {
  const cell = value => {
    const text = String(value)
    return `"${(/^[\s]*[=+\-@]/.test(text) ? "'" + text : text).replace(/"/g, '""')}"`
  }
  return '\uFEFF' + [[PAYROLL_NOTE], columns.map(([, label]) => label), ...rows.map(row => columns.map(([key]) => cellValue(row, key)))].map(row => row.map(cell).join(',')).join('\r\n')
}
export function payrollPrintHtml(rows, month) {
  checkMonth(month)
  return `<!doctype html><html lang="zh-CN"><head><meta charset="utf-8"><title>工资表 ${month}</title><style>@page{size:A4 landscape;margin:10mm}body{font:11px sans-serif}table{width:100%;border-collapse:collapse}td,th{padding:5px;border:1px solid #ccc}thead{display:table-header-group}tr{break-inside:avoid}</style></head><body><h1>${month} 工资表</h1><p>${PAYROLL_NOTE}</p><p>当前筛选 ${rows.length} 人</p><table><thead><tr>${columns.map(([, label]) => `<th>${label}</th>`).join('')}</tr></thead><tbody>${rows.map(row => `<tr>${columns.map(([key]) => `<td>${escapeHtml(cellValue(row, key))}</td>`).join('')}</tr>`).join('')}</tbody></table></body></html>`
}

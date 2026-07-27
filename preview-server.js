/**
 * 预览用 Mock 服务器
 * 提供登录、员工列表、考勤记录等模拟 API 端点
 * 启动: node preview-server.js
 */
const http = require('http')

const PORT = 3000
const TOKEN = 'preview_token_rino_2026'

// 模拟员工列表
const MOCK_STAFF = [
  { id: 1, staffId: 'QT-01', staffName: '周韵', department: '前厅', staffPosition: '店长', employmentStatus: 'active' },
  { id: 2, staffId: 'QT-02', staffName: '李文', department: '前厅', staffPosition: '服务员', employmentStatus: 'active' },
  { id: 3, staffId: 'QT-03', staffName: '王芳', department: '前厅', staffPosition: '收银', employmentStatus: 'active' },
  { id: 4, staffId: 'CF-01', staffName: '张大厨', department: '厨房', staffPosition: '厨师长', employmentStatus: 'active' },
  { id: 5, staffId: 'CF-02', staffName: '刘明', department: '厨房', staffPosition: '厨师', employmentStatus: 'active' },
  { id: 6, staffId: 'CF-03', staffName: '陈华', department: '厨房', staffPosition: '配菜', employmentStatus: 'active' },
  { id: 7, staffId: 'GL-01', staffName: '赵总', department: '管理', staffPosition: '总经理', employmentStatus: 'active' },
  { id: 8, staffId: 'CG-01', staffName: '孙采购', department: '采购', staffPosition: '采购员', employmentStatus: 'active' },
  { id: 9, staffId: 'LS-01', staffName: '临时工A', department: '前厅', staffPosition: '临时工', employmentStatus: 'active', employeeType: '临时工' },
  { id: 10, staffId: 'LS-02', staffName: '临时工B', department: '厨房', staffPosition: '临时工', employmentStatus: 'active', employeeType: '临时工' },
]

// 模拟桌台数据
const NOW = new Date()
const TODAY = `${NOW.getFullYear()}-${String(NOW.getMonth()+1).padStart(2,'0')}-${String(NOW.getDate()).padStart(2,'0')}`
const ZONES = ['一楼包厢','一楼扶摇厅','二楼1号服务厅','一楼散客大厅','二楼2号服务厅','三楼宴会厅','四楼宴会厅','排队或加桌','一楼外摆']

// 按分区生成 66 张桌台 — 使用中文包房名/桌号
const GUEST_NAMES = ['张先生','李总','王女士','赵老师','刘经理','陈董','周律师','吴教授','郑总','孙老板','钱医师','何女士','唐局长','曹经理','沈老师','彭先生','徐总','方主任','林医生','郭女士']
const BANQUET_NAMES = ['家宴','商务宴请','朋友聚会','生日宴','婚宴','寿宴','同学会','商务接待','答谢宴','满月酒']
const ROOM_NAMES_BY_ZONE = [
  ['101和风','102玉雨','103翠微','201紫霞','202星河','203烟云','204听溪','205远山','206梨花','207秋月','208苏月','209新月','210子月','211仙霞','212敬亭','213春归'],
  ['扶摇1','扶摇2','扶摇3','扶摇4','扶摇5','扶摇6','扶摇7','扶摇8','扶摇9'],
  ['厅1','厅2','厅3','厅4','厅5','厅6','厅7','厅8','厅9','厅10','厅11','厅12','大厅'],
  ['阳1','阳2','阳3','阳4'],
  ['三楼宴会厅1','三楼宴会厅2','三楼宴会厅3','三楼宴会厅5','三楼宴会厅6','三楼宴会厅8','三楼宴会厅9','三楼宴会厅10','三楼宴会厅11','三楼宴会厅12','三楼宴会厅15','三楼宴会厅16','三楼宴会厅18','三楼宴会厅19','三楼宴会厅20'],
  ['办公室'],
  ['外卖','排队1','排队2','排队3','排队4','排队5'],
  ['一楼外摆1'],
]
let _tid = 0
function genTables() {
  const t = []
  ZONES.forEach((zone, zi) => {
    const names = ROOM_NAMES_BY_ZONE[zi] || []
    const count = names.length
    for (let i = 0; i < count; i++) {
      _tid++
      const numStr = names[i]
      // 约 30% 有预订
      const hasBooking = _tid % 3 === 1 || _tid % 7 === 0
      t.push({
        table_id: _tid,
        table_number: numStr,
        table_area: zone,
        ticket_code: '',
        table_capacity: [2,4,4,6,6,8,8,10,12][i % 9],
        sort_order: _tid,
        status: hasBooking ? 'booked' : 'free',
        booking: hasBooking ? {
          booking_id: 10000 + _tid,
          customer_name: GUEST_NAMES[_tid % GUEST_NAMES.length],
          customer_phone: `1${String(30 + _tid % 50).padStart(2,'0')}****${String(_tid * 7 % 10000).padStart(4,'0')}`,
          guest_count: [2,3,4,5,6,7,8,10,12][i % 9],
          booking_date: TODAY,
          booking_time: `${_tid % 2 === 0 ? '12' : '18'}:${String(_tid * 3 % 60).padStart(2,'0')}`,
          time_type: _tid % 2 === 0 ? 'lunch' : 'dinner',
          banquet_name: BANQUET_NAMES[_tid % BANQUET_NAMES.length],
          occasion_type: BANQUET_NAMES[_tid % BANQUET_NAMES.length],
          booking_status: 'confirmed',
          deposit: _tid % 4 === 0 ? Math.round(_tid * 100) : 0,
          remark: _tid % 5 === 0 ? '需要提前布置' : '',
          staff_name: 'Rino',
          menu_items: null,
          table_count: 1,
          spare_tables: 0,
        } : null
      })
    }
  })
  return t
}
const MOCK_TABLES = genTables()

// 模拟考勤记录（内存存储）
const attendanceDB = {}

function jsonResponse(res, code, message, data) {
  res.writeHead(200, { 'Content-Type': 'application/json; charset=utf-8', 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*', 'Access-Control-Allow-Methods': '*' })
  res.end(JSON.stringify({ code, message, data }))
}

function parseBody(req) {
  return new Promise((resolve) => {
    let body = ''
    req.on('data', chunk => body += chunk)
    req.on('end', () => {
      try { resolve(JSON.parse(body)) } catch { resolve({}) }
    })
  })
}

const server = http.createServer(async (req, res) => {
  // CORS preflight
  if (req.method === 'OPTIONS') {
    res.writeHead(204, { 'Access-Control-Allow-Origin': '*', 'Access-Control-Allow-Headers': '*', 'Access-Control-Allow-Methods': '*', 'Access-Control-Max-Age': '86400' })
    return res.end()
  }

  const url = new URL(req.url, `http://localhost:${PORT}`)
  const path = url.pathname

  // ===== Auth =====
  if (path === '/api/auth/login' && req.method === 'POST') {
    const body = await parseBody(req)
    if (body.username === 'rino' && body.password === '002323') {
      return jsonResponse(res, 200, 'success', {
        token: TOKEN,
        user: { id: 1, username: 'rino', name: 'Rino', storeId: 1, storeName: '宁国店', role: 'admin' },
        storeId: 1,
        storeName: '宁国店',
      })
    }
    return jsonResponse(res, 401, '账号或密码错误', null)
  }

  // GET /api/auth/me
  if (path === '/api/auth/me' && req.method === 'GET') {
    return jsonResponse(res, 200, 'success', { id: 1, username: 'rino', name: 'Rino', storeId: 1, storeName: '宁国店' })
  }

  // POST /api/auth/logout
  if (path === '/api/auth/logout') {
    return jsonResponse(res, 200, 'success', null)
  }

  // ===== HR Staff =====
  if (path === '/api/hr/staff' && req.method === 'GET') {
    return jsonResponse(res, 200, 'success', MOCK_STAFF)
  }

  if (path === '/api/hr/departments' && req.method === 'GET') {
    const depts = [...new Set(MOCK_STAFF.map(s => s.department))]
    return jsonResponse(res, 200, 'success', depts.map(d => ({ deptId: d, deptName: d })))
  }

  // ===== Attendance Record =====
  if (path === '/api/hr/attendance/record' && req.method === 'GET') {
    const empId = url.searchParams.get('empId')
    const month = url.searchParams.get('month')
    const key = `${empId}_${month}`
    const existing = attendanceDB[key]
    if (!existing) {
      return jsonResponse(res, 404, `未找到该员工${month}的考勤记录`, null)
    }
    return jsonResponse(res, 200, 'success', existing)
  }

  if (path === '/api/hr/attendance/record' && req.method === 'POST') {
    const body = await parseBody(req)
    const key = `${body.empId}_${body.month}`
    attendanceDB[key] = {
      empId: body.empId,
      empName: body.empName,
      department: body.department,
      month: body.month,
      days: body.days || [],
      employment: body.employment || '全勤在职',
      salaryStatus: body.salaryStatus || '未发放',
      publicHoliday: body.publicHoliday ?? 6,
      carryOver: body.carryOver ?? 0,
      summaryNotes: body.summaryNotes || '',
      finalBalance: body.finalBalance ?? 0,
      recordedDays: body.recordedDays ?? 0,
      joinDay: body.joinDay ?? null,
      leaveDay: body.leaveDay ?? null,
      createdBy: body.createdBy || 'Rino',
    }
    return jsonResponse(res, 200, 'success', null)
  }

  if (path === '/api/hr/attendance/summary' && req.method === 'GET') {
    const month = url.searchParams.get('month')
    // aggregate from DB
    const summaries = Object.entries(attendanceDB)
      .filter(([k]) => k.endsWith(`_${month}`))
      .map(([k, v]) => v)
    return jsonResponse(res, 200, 'success', summaries)
  }

  // ===== 桌台管理 =====
  if (path === '/api/tables' && req.method === 'GET') {
    const timeType = url.searchParams.get('timeType')
    let tables = MOCK_TABLES
    if (timeType && timeType !== 'all') {
      tables = tables.map(t => {
        if (!t.booking) return t
        if (t.booking.time_type === timeType) return t
        return { ...t, booking: null, status: 'free' }
      })
    }
    return jsonResponse(res, 200, 'success', tables)
  }
  if (path === '/api/tables' && req.method === 'POST') {
    return jsonResponse(res, 200, 'success', { id: Date.now() })
  }
  if (path.startsWith('/api/tables/') && req.method === 'PUT') {
    return jsonResponse(res, 200, 'success', null)
  }
  if (path.startsWith('/api/tables/') && req.method === 'DELETE') {
    return jsonResponse(res, 200, 'success', null)
  }
  if (path === '/api/tables/reorder' && req.method === 'POST') {
    return jsonResponse(res, 200, 'success', null)
  }
  if (path === '/api/tables/swap-booking' && req.method === 'POST') {
    return jsonResponse(res, 200, 'success', null)
  }
  if (path === '/api/bookings' && req.method === 'POST') {
    return jsonResponse(res, 200, 'success', { booking_id: Date.now() })
  }
  if (path.startsWith('/api/bookings/') && req.method === 'PUT') {
    return jsonResponse(res, 200, 'success', null)
  }
  if (path.startsWith('/api/bookings/') && req.method === 'DELETE') {
    return jsonResponse(res, 200, 'success', null)
  }

  // ===== Fallback: 菜单、预订等返回空数据 =====
  console.log(`[Preview] ${req.method} ${path}`)
  return jsonResponse(res, 200, 'success', [])
})

server.listen(PORT, () => {
  console.log(`\n  🍳 餐饮管理系统 · 预览服务器已启动`)
  console.log(`  📡 后端 API: http://localhost:${PORT}`)
  console.log(`  🔐 账号: rino / 密码: 002323`)
  console.log(`  ⏳ 前端启动后访问 http://localhost:5173\n`)
})

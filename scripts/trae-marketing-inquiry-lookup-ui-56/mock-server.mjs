// TR-MARKETING-INQUIRY-LOOKUP-UI-56 合同 mock 服务器（MOCKED_CONTRACT）。
// 职责：静态服务 frontend_v3/dist（SPA fallback 支持 /h5/inquiry/:inquiryNo 深链接直开/刷新），
// 并按 56 任务卡契约提供 POST /api/public/booking-inquiry/lookup 假后端。
// 后端 TL55 未 reviewed——本服务仅用于浏览器证据，不冒充真实闭环。
// 日志纪律：只记 scenario 与 inquiryNo，绝不输出手机号。
import http from 'node:http'
import { readFileSync, existsSync, statSync } from 'node:fs'
import { join, extname } from 'node:path'

const MIME = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.jpg': 'image/jpeg',
  '.png': 'image/png',
  '.webp': 'image/webp',
  '.svg': 'image/svg+xml',
  '.ico': 'image/x-icon',
  '.woff2': 'font/woff2'
}

// 场景表：inquiryNo → { phone, data }。data 故意携带内部/敏感多余字段，
// 用于证明前端白名单层会丢弃它们；响应日志不落手机号。
const SCENARIOS = {
  INQ9001: {
    phone: '13800000001',
    data: { inquiryNo: 'INQ9001', status: 'pending', expectedDate: '2026-09-20', partySize: 8, createdAt: '2026-09-14 10:20', remark: '内部备注勿展示', operatorName: '内部员工', storeName: '宁国总店' }
  },
  INQ9002: {
    phone: '13900000002',
    data: { inquiryNo: 'INQ9002', status: 'converted', expectedDate: '2026-09-21', partySize: 12, createdAt: '2026-09-13 18:05', bookingId: 'BK20260914002', operatorName: '内部员工' }
  },
  INQ9003: {
    phone: '13700000003',
    data: { inquiryNo: 'INQ9003', status: 'rejected', expectedDate: '2026-09-19', partySize: 6, createdAt: '2026-09-14 09:11', remark: '内部备注勿展示' }
  },
  INQ9004: {
    phone: '13600000004',
    data: { inquiryNo: 'INQ9004', status: 'future_unknown_state', expectedDate: '2026-09-22', partySize: 4, createdAt: '2026-09-14 08:00' }
  }
}
const DELAY_MS = 300

export function startMockServer(distDir, port = 0) {
  const server = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost')
    if (req.method === 'POST' && url.pathname === '/api/public/booking-inquiry/lookup') {
      let raw = ''
      req.on('data', (c) => { raw += c })
      req.on('end', () => {
        let body = {}
        try { body = JSON.parse(raw || '{}') } catch { body = {} }
        const inquiryNo = String(body.inquiryNo ?? '')
        const phone = String(body.phone ?? '')
        const extraKeys = Object.keys(body).filter((k) => k !== 'inquiryNo' && k !== 'phone')
        const send = (status, payload) => {
          setTimeout(() => {
            res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8' })
            res.end(JSON.stringify(payload))
          }, DELAY_MS)
        }
        console.log(`[mock] lookup scenario=${scenOf(inquiryNo)} inquiryNo=${inquiryNo} bodyKeys=[${['inquiryNo', 'phone', ...extraKeys].join(',')}] phoneMatch=${phoneMatched(inquiryNo, phone)}`)
        if (inquiryNo.startsWith('INQERR')) return send(500, { code: 500, message: 'internal mock error' })
        // HTTP 200 + 业务 code=500（后端 Result.error(500) 返回 HTTP 200）
        if (inquiryNo.startsWith('INQBIZ')) return send(200, { code: 500, message: '系统内部错误，请稍后重试' })
        const sc = SCENARIOS[inquiryNo]
        if (!sc || phone !== sc.phone) return send(200, { code: 200, message: 'ok', data: null })
        send(200, { code: 200, message: 'ok', data: sc.data })
      })
      return
    }
    // 静态文件 + SPA fallback
    let filePath = join(distDir, decodeURIComponent(url.pathname))
    if (!existsSync(filePath) || statSync(filePath).isDirectory()) {
      filePath = join(distDir, 'index.html') // 深链接/刷新由 index.html 接管，vue-router 接路由
    }
    try {
      const buf = readFileSync(filePath)
      res.writeHead(200, { 'Content-Type': MIME[extname(filePath).toLowerCase()] || 'application/octet-stream' })
      res.end(buf)
    } catch {
      res.writeHead(404)
      res.end('not found')
    }
  })
  return new Promise((resolve) => server.listen(port, '127.0.0.1', () => resolve(server)))
}

function scenOf(inquiryNo) {
  if (inquiryNo.startsWith('INQERR')) return 'system_error'
  return SCENARIOS[inquiryNo] ? SCENARIOS[inquiryNo].data.status : 'not_found'
}

function phoneMatched(inquiryNo, phone) {
  const sc = SCENARIOS[inquiryNo]
  return !!sc && phone === sc.phone
}

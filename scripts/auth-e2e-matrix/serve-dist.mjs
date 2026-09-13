#!/usr/bin/env node
/**
 * DL-AUTH-E2E-MATRIX-15 候选 dist 静态服务（SPA 回退到 index.html）
 * python -m http.server 不支持 SPA 回退，/login 会 404，导致浏览器矩阵定位不到登录框。
 * 本服务对非文件路径统一回退 index.html，并显式服务 /case/**（法务入口）。
 */
import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = process.env.MX_DIST || path.resolve(fileURLToPath(new URL('../../frontend_v3/dist', import.meta.url)));
const PORT = Number(process.env.MX_FRONT_PORT || 5183);
const HOST = process.env.MX_FRONT_HOST || '127.0.0.1';
// 候选 dist 以同源方式调用 /api/**，必须反代到真实后端；否则 SPA 回退会把 HTML 当 JSON 返回。
const API_TARGET = new URL(process.env.MX_API_TARGET || 'http://127.0.0.1:18080');

const MIME = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.mjs': 'text/javascript; charset=utf-8', '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8', '.png': 'image/png', '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg', '.svg': 'image/svg+xml', '.ico': 'image/x-icon',
  '.woff': 'font/woff', '.woff2': 'font/woff2', '.ttf': 'font/ttf', '.map': 'application/json',
  '.txt': 'text/plain; charset=utf-8', '.mp4': 'video/mp4',
};

function send(res, file, allowIndexFallback) {
  const ext = path.extname(file).toLowerCase();
  const type = MIME[ext] || 'application/octet-stream';
  const stream = fs.createReadStream(file);
  stream.on('open', () => { res.writeHead(200, { 'Content-Type': type, 'Cache-Control': 'no-store' }); stream.pipe(res); });
  stream.on('error', () => {
    if (allowIndexFallback) return send(res, path.join(ROOT, 'index.html'), false);
    res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' }); res.end('404');
  });
}

function proxyApi(req, res) {
  const targetPath = req.url || '/';
  const headers = { ...req.headers, host: API_TARGET.host };
  // 由上游重新计算长度；避免 content-length 与实际体不一致导致上游等待挂起
  delete headers['content-length'];
  const upstream = http.request({
    host: API_TARGET.hostname, port: API_TARGET.port || 80,
    method: req.method, path: targetPath,
    headers,
  }, up => {
    const out = { ...up.headers };
    delete out['content-encoding'];
    delete out['transfer-encoding'];
    res.writeHead(up.statusCode || 502, out);
    up.pipe(res);
  });
  upstream.on('error', e => {
    if (!res.headersSent) res.writeHead(502, { 'Content-Type': 'application/json;charset=UTF-8' });
    res.end(JSON.stringify({ code: 502, message: 'upstream unavailable: ' + e.message }));
  });
  req.on('error', () => upstream.destroy());
  req.pipe(upstream);
}

const server = http.createServer((req, res) => {
  if ((req.url || '').startsWith('/api/')) return proxyApi(req, res);
  const urlPath = decodeURIComponent((req.url || '/').split('?')[0]);
  const candidate = path.join(ROOT, path.normalize(urlPath).replace(/^(\.\.[/\\])+/, ''));
  if (fs.existsSync(candidate) && fs.statSync(candidate).isFile()) return send(res, candidate, false);
  if (fs.existsSync(candidate) && fs.statSync(candidate).isDirectory()) {
    const idx = path.join(candidate, 'index.html');
    if (fs.existsSync(idx)) return send(res, idx, false);
  }
  // SPA 回退
  return send(res, path.join(ROOT, 'index.html'), false);
});

server.listen(PORT, HOST, () => console.log(`dist server on http://${HOST}:${PORT} root=${ROOT}`));

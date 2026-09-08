// TR-RECEIPT-REAL-24 browser test server: serves the candidate's BUILT frontend_v3/dist
// and proxies /api -> the isolated candidate backend at 127.0.0.1:18083.
// Same origin as the page, so the browser performs no CORS check; the Origin header is
// rewritten to the production domain to mimic nginx same-origin proxying (backend CORS
// whitelist would 403 the test origin). SPA history fallback -> index.html.
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { join, normalize, extname } from 'node:path';

const DIST = join(process.cwd(), 'frontend_v3', 'dist');
const PORT = Number(process.env.TR24_WEB_PORT || 5184);
const API_HOST = '127.0.0.1';
const API_PORT = Number(process.env.TR24_API_PORT || 18083);

const TYPES = {
  '.html': 'text/html; charset=utf-8', '.js': 'text/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8', '.json': 'application/json; charset=utf-8',
  '.png': 'image/png', '.jpg': 'image/jpeg', '.jpeg': 'image/jpeg', '.gif': 'image/gif',
  '.svg': 'image/svg+xml', '.ico': 'image/x-icon', '.woff': 'font/woff', '.woff2': 'font/woff2',
  '.ttf': 'font/ttf', '.mp4': 'video/mp4', '.webp': 'image/webp', '.map': 'application/json',
};

const server = http.createServer(async (req, res) => {
  const urlPath = decodeURIComponent(req.url.split('?')[0]);

  if (urlPath.startsWith('/api/') || urlPath === '/api') {
    const headers = { ...req.headers, host: `127.0.0.1:${API_PORT}`, origin: 'https://youjianchuiyan.com' };
    delete headers.referer;
    const up = http.request(
      { host: API_HOST, port: API_PORT, method: req.method, path: req.url, headers },
      (upRes) => {
        res.writeHead(upRes.statusCode || 502, upRes.headers);
        upRes.pipe(res);
      }
    );
    up.on('error', () => { if (!res.headersSent) res.writeHead(502); res.end(); });
    req.pipe(up);
    return;
  }

  let rel = urlPath === '/' ? 'index.html' : urlPath.slice(1);
  let file = normalize(join(DIST, rel));
  if (!file.startsWith(DIST)) { res.writeHead(403); return res.end(); }
  const candidates = extname(file)
    ? [file, join(DIST, 'index.html')]
    : [join(file, 'index.html'), join(DIST, 'index.html')];
  for (const candidate of candidates) {
    try {
      const body = await readFile(candidate);
      res.writeHead(200, { 'Content-Type': TYPES[extname(candidate)] || 'application/octet-stream' });
      return res.end(body);
    } catch { /* try next candidate */ }
  }
  res.writeHead(404); res.end();
});
server.listen(PORT, '127.0.0.1', () => console.log(`TR24_WEB http://127.0.0.1:${PORT} -> dist + /api proxy to ${API_HOST}:${API_PORT}`));

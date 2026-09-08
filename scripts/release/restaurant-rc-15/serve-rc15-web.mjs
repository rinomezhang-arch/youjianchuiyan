// RC15 browser test server: serves the candidate's BUILT frontend_v3/dist
// and proxies /api -> the isolated candidate backend at 127.0.0.1:18080.
// Purpose: browser regression runs against the real production-bundle + real backend,
// not a dev-server-only behavior. SPA history fallback -> index.html; public/* (incl. /case/) served as files.
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { join, normalize, extname } from 'node:path';

const DIST = join(process.cwd(), 'frontend_v3', 'dist');
const PORT = Number(process.env.RC15_WEB_PORT || 5183);
const API_HOST = '127.0.0.1';
const API_PORT = Number(process.env.RC15_API_PORT || 18080);

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
    const headers = { ...req.headers, host: `127.0.0.1:${API_PORT}` };
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
  // Candidate order: explicit file -> directory index (e.g. /case/ -> case/index.html) -> SPA fallback.
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
server.listen(PORT, '127.0.0.1', () => console.log(`RC15_WEB http://127.0.0.1:${PORT} -> dist + /api proxy to ${API_HOST}:${API_PORT}`));

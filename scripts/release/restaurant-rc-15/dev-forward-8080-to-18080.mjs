// Minimal local TCP forwarder: localhost:8080 -> 127.0.0.1:18080
// Why: frontend_v3/vite.config.js proxies /api to http://localhost:8080;
// the RC15 candidate backend runs on 18080 (isolated). Non-privileged port, no admin needed.
import net from 'node:net';
const LISTEN = 8080, TARGET_HOST = '127.0.0.1', TARGET_PORT = 18080;
const server = net.createServer((client) => {
  const upstream = net.connect(TARGET_PORT, TARGET_HOST, () => {
    client.pipe(upstream);
    upstream.pipe(client);
  });
  const drop = () => { client.destroy(); upstream.destroy(); };
  client.on('error', drop);
  upstream.on('error', drop);
});
server.listen(LISTEN, '127.0.0.1', () => console.log(`FORWARD 127.0.0.1:${LISTEN} -> ${TARGET_HOST}:${TARGET_PORT}`));

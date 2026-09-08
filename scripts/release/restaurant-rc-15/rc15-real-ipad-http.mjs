// RC15 real-port iPad batch idempotency chain driver (ASCII output).
// Against the running candidate backend at 127.0.0.1:18080 + isolated MySQL 13317/banquet_rc15.
// Flow mirrors DL-IPAD-BATCH-AUTH-16 but over real HTTP with the full Spring stack:
//   header/device gating -> one-time authorization -> batch submit -> replay idempotency
//   -> changed-payload 409 -> used-token 403 -> two-table readback (dish rows, receipts, operator).
// Readback uses the local mysql client (MEM-DILONG-005), not docker.
import { execFileSync } from 'node:child_process';
import { writeFileSync, readFileSync } from 'node:fs';
import http from 'node:http';

const BASE = { host: '127.0.0.1', port: 18080 };
const MYSQL = 'C:\\Program Files\\MySQL\\MySQL Server 8.4\\bin\\mysql.exe';
const SEED = process.argv[2] || 'scripts/release/restaurant-rc-15/seed-ipad-idem.sql';
const DEVICE = 'RC15IDEMDEV';
const BOOKING = 'RC15-IDEM-BK';
const DISH = 'RC15-IDEM-DISH';
const STAFF_ID = 405;
const results = [];

function mysql(query) {
  return execFileSync(MYSQL, ['--no-defaults', '--protocol=tcp', '--host=127.0.0.1', '--port=13317',
    '--user=root', '--default-character-set=utf8mb4', '-N', '-B', 'banquet_rc15', '-e', query],
    { encoding: 'utf8' }).trim();
}
function reseed() {
  execFileSync(MYSQL, ['--no-defaults', '--protocol=tcp', '--host=127.0.0.1', '--port=13317',
    '--user=root', '--default-character-set=utf8mb4', 'banquet_rc15'],
    { input: readFileSync(SEED, 'utf8'), encoding: 'utf8' });
}

function httpCall(method, path, headers, body) {
  return new Promise((resolve, reject) => {
    const payload = body ? JSON.stringify(body) : null;
    const req = http.request({ ...BASE, method, path,
      headers: { 'Content-Type': 'application/json', ...(payload ? { 'Content-Length': Buffer.byteLength(payload) } : {}), ...headers } },
      (res) => {
        let raw = ''; res.on('data', c => raw += c);
        res.on('end', () => { let json = null; try { json = raw ? JSON.parse(raw) : null; } catch {} resolve({ status: res.statusCode, raw, json }); });
      });
    req.on('error', reject);
    if (payload) req.write(payload);
    req.end();
  });
}
const ipadHeaders = (over = {}) => ({ 'X-Client-Type': 'ipad', 'X-Device-Sn': DEVICE, 'X-Store-Id': '1', 'X-Staff-Id': '0', ...over });

let pass = 0, fail = 0;
function check(name, ok, extra = '') {
  results.push(`${ok ? 'PASS' : 'FAIL'}  ${name}${extra ? ' :: ' + extra : ''}`);
  if (ok) pass++; else fail++;
}
async function authorize(bookingId = BOOKING, username = 'syn_ipad', password = 'synpass123', dev = DEVICE, store = '1') {
  return httpCall('POST', '/api/ipad/auth/verify', ipadHeaders({ 'X-Device-Sn': dev, 'X-Store-Id': store }),
    { username, password, booking_id: bookingId });
}
function batch(token, crid, dishes) {
  return httpCall('POST', '/api/ipad/order/add-dishes', ipadHeaders(),
    { client_request_id: crid, booking_id: BOOKING, dishes, authorization_token: token });
}

async function main() {
  reseed(); // deterministic starting point

  // --- header / device gating (real interceptor + real binding table)
  let r = await httpCall('POST', '/api/ipad/auth/verify', { 'X-Device-Sn': DEVICE, 'X-Store-Id': '1', 'X-Staff-Id': '0' },
    { username: 'syn_ipad', password: 'synpass123', booking_id: BOOKING });
  check('gate: missing X-Client-Type -> 403', r.status === 403, `status=${r.status}`);

  r = await httpCall('POST', '/api/ipad/auth/verify', ipadHeaders({ 'X-Device-Sn': 'GHOST-DEV' }),
    { username: 'syn_ipad', password: 'synpass123', booking_id: BOOKING });
  check('gate: unbound device -> 401', r.status === 401 && /401/.test(r.raw), `status=${r.status}`);

  r = await httpCall('POST', '/api/ipad/auth/verify', ipadHeaders({ 'X-Store-Id': '2' }),
    { username: 'syn_ipad', password: 'synpass123', booking_id: BOOKING });
  check('gate: device/store mismatch -> 403', r.status === 403, `status=${r.status}`);

  // --- authorization (real staff + booking checks)
  r = await authorize(BOOKING, 'syn_ipad', 'wrong-password');
  check('auth: wrong password -> 401', r.status === 200 && r.json?.code === 401, `code=${r.json?.code}`);

  r = await authorize(BOOKING, 'syn_nobody', 'synpass123');
  check('auth: unknown account -> 401', r.status === 200 && r.json?.code === 401, `code=${r.json?.code}`);

  r = await authorize('NOPE-BK');
  check('auth: foreign/unknown booking -> 403', r.status === 200 && r.json?.code === 403, `code=${r.json?.code}`);

  r = await authorize();
  const token = r.json?.data?.authorization_token;
  check('auth: valid verify -> 200 + one-time token', r.status === 200 && r.json?.code === 200 && !!token,
    `code=${r.json?.code} hasToken=${!!token} expires=${r.json?.data?.expires_in}`);

  // --- batch submit gating
  r = await httpCall('POST', '/api/ipad/order/add-dishes', ipadHeaders(),
    { client_request_id: 'RC15IDEM-GATE-0001', booking_id: BOOKING, dishes: [{ dish_id: DISH, dish_quantity: 1 }] });
  check('batch: missing token -> 403', r.status === 200 && r.json?.code === 403, `code=${r.json?.code}`);

  r = await httpCall('POST', '/api/ipad/order/add-dishes', ipadHeaders(),
    { staff_id: STAFF_ID, client_request_id: 'RC15IDEM-GATE-0002', booking_id: BOOKING,
      dishes: [{ dish_id: DISH, dish_quantity: 1 }], authorization_token: 'garbage' });
  check('batch: self-reported staff_id rejected -> 400', r.status === 200 && r.json?.code === 400, `code=${r.json?.code}`);

  // --- first real commit
  const CR1 = 'RC15IDEM-0000-0001';
  const CR_NEW = 'RC15IDEM-0000-0099';
  r = await batch(token, CR1, [{ dish_id: DISH, dish_quantity: 2 }]);
  const firstData = r.json?.data;
  check('batch: first submit committed', r.status === 200 && r.json?.code === 200 && firstData?.status === 'committed'
    && firstData?.added_quantity === 2 && Math.abs(Number(firstData?.added_amount) - 25.00) < 1e-9
    && Array.isArray(firstData?.dish_booking_ids) && firstData.dish_booking_ids.length === 1,
    `code=${r.json?.code} qty=${firstData?.added_quantity} amt=${firstData?.added_amount}`);

  // --- replay: same client_request_id + same payload, NEW authorization token
  r = await authorize();
  const token2 = r.json?.data?.authorization_token;
  r = await batch(token2, CR1, [{ dish_id: DISH, dish_quantity: 2 }]);
  check('replay: same id+payload -> 200 and identical receipt data', r.status === 200 && r.json?.code === 200
    && JSON.stringify(r.json?.data) === JSON.stringify(firstData),
    `dataEqual=${JSON.stringify(r.json?.data) === JSON.stringify(firstData)}`);

  // --- two-table readback: still 1 dish row, 1 receipt, operator is the real authorized staff
  const dishRows = Number(mysql(`SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='${BOOKING}' AND store_id=1`));
  const receiptRows = Number(mysql(`SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id='${CR1}'`));
  const operator = mysql(`SELECT operator_id FROM ipad_batch_request WHERE client_request_id='${CR1}'`);
  check('readback: exactly 1 dish detail row (qty 2, not 2 rows)', dishRows === 1, `dishRows=${dishRows}`);
  check('readback: exactly 1 receipt row', receiptRows === 1, `receiptRows=${receiptRows}`);
  check('readback: receipt operator_id = authorized staff', operator === String(STAFF_ID), `operator=${operator}`);

  // --- same client_request_id, changed payload -> 409, zero new rows
  r = await authorize();
  r = await batch(r.json?.data?.authorization_token, CR1, [{ dish_id: DISH, dish_quantity: 3 }]);
  check('conflict: same id different payload -> 409', r.status === 200 && r.json?.code === 409, `code=${r.json?.code} msg=${r.json?.message || ''}`.slice(0, 80));
  const dishRows2 = Number(mysql(`SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='${BOOKING}' AND store_id=1`));
  const receiptRows2 = Number(mysql(`SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id='${CR1}'`));
  check('conflict: two tables unchanged', dishRows2 === 1 && receiptRows2 === 1, `dishRows=${dishRows2} receiptRows=${receiptRows2}`);

  // --- replayed one-time token (first token already consumed) with a NEW client_request_id -> 403, zero new rows
  r = await batch(token, CR_NEW, [{ dish_id: DISH, dish_quantity: 1 }]);
  check('token replay: consumed token rejected -> 403', r.status === 200 && r.json?.code === 403, `code=${r.json?.code}`);
  const newReceipts = Number(mysql(`SELECT COUNT(*) FROM ipad_batch_request WHERE client_request_id='${CR_NEW}'`));
  const totalReceipts = Number(mysql(`SELECT COUNT(*) FROM ipad_batch_request WHERE booking_id='${BOOKING}' AND store_id=1`));
  const dishRows3 = Number(mysql(`SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id='${BOOKING}' AND store_id=1`));
  check('token replay: rejected request wrote zero rows', newReceipts === 0 && totalReceipts === 1 && dishRows3 === 1,
    `newReceipts=${newReceipts} totalReceipts=${totalReceipts} dishRows=${dishRows3}`);

  const line = `RC15_IPAD_HTTP_RESULT pass=${pass} fail=${fail}`;
  const out = `${results.join('\n')}\n${line}\n`;
  writeFileSync('scripts/release/restaurant-rc-15/ipad-http-result.txt', out, 'utf8');
  console.log(out);
  process.exit(fail ? 1 : 0);
}
main().catch(e => { console.error('DRIVER_ERROR', e); process.exit(2); });

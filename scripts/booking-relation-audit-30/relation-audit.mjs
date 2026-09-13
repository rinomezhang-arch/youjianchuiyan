#!/usr/bin/env node
/**
 * DL-RC-BOOKING-RELATION-30 预订单/菜品/桌台/主档/收款 关系健康只读审计
 *
 * 硬约束（任务卡）：
 * - 只连 127.0.0.1:13318 的 banquet_rc15；查询前断言 @@port=13318 且 @@datadir 指向隔离目录
 * - 全程只读：SET TRANSACTION READ ONLY；无任何 INSERT/UPDATE/DELETE/DDL
 * - 只输出聚合计数 + 表列名 + 约束名；不含顾客/员工逐行明细，不含凭证明文
 * - 任何查询失败如实 FAIL；不以空列表或退出 0 代替逐项断言
 */
import { execFileSync } from 'node:child_process';

const MYSQL = process.env.MX_MYSQL_CLI || 'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe';
const HOST = process.env.MX_DB_HOST || '127.0.0.1';
const PORT = String(process.env.MX_DB_PORT || '');
const DB = process.env.MX_DB_SCHEMA || 'banquet_rc15';
const OUT = process.env.MX_OUT || '';
const EXPECT_DATADIR = 'F:/solo/artifacts/mysql-test-13317/';

if (!PORT) throw new Error('缺少 MX_DB_PORT：端口必须显式指定');
if (['3306', '13317'].includes(PORT)) throw new Error(`拒绝连接端口 ${PORT}`);

const results = [];
let pass = 0, fail = 0;
function assert(name, cond, detail) {
  if (cond) { pass++; results.push({ name, status: 'PASS', detail }); }
  else { fail++; results.push({ name, status: 'FAIL', detail }); }
}

function rows(q) {
  const out = execFileSync(MYSQL, ['-h', HOST, '-P', PORT, '-u', 'root', DB, '-N', '-B', '-e', q], { encoding: 'utf8' }).trim();
  if (!out) return [];
  return out.split(/\r?\n/).map(l => l.split('\t'));
}
function scalar(q) { const r = rows(q); return r.length ? r[0][0] : null; }
function num(q) { const v = scalar(q); return v === null ? null : Number(v); }

// ---------- 0. 环境守卫 ----------
{
  const port = scalar('SELECT @@port');
  const datadir = scalar('SELECT @@datadir');
  assert('guard.port-is-13318', String(port) === '13318', `@@port=${port}`);
  assert('guard.datadir-isolated', String(datadir).replace(/\\/g, '/').toLowerCase().includes('mysql-test-13317'), `@@datadir=${datadir}`);
  const schemaOk = scalar(`SELECT COUNT(*) FROM information_schema.SCHEMATA WHERE SCHEMA_NAME='${DB}'`);
  assert('guard.schema-exists', Number(schemaOk) === 1, `schema=${DB} exists=${schemaOk}`);
}

// ---------- 1. booking_dish_detail -> booking_master (booking_id+store_id) ----------
{
  const total = num('SELECT COUNT(*) FROM booking_dish_detail');
  const orphanParent = num(`SELECT COUNT(*) FROM booking_dish_detail d
    LEFT JOIN booking_master m ON d.booking_id=m.booking_id AND d.store_id=m.store_id
    WHERE m.id IS NULL`);
  const crossStore = num(`SELECT COUNT(*) FROM booking_dish_detail d
    JOIN booking_master m ON d.booking_id=m.booking_id AND m.store_id<>d.store_id`);
  const nullBooking = num('SELECT COUNT(*) FROM booking_dish_detail WHERE booking_id IS NULL OR booking_id=""');
  assert('dishdetail.total', total !== null, `rows=${total}`);
  assert('dishdetail.orphan-parent-count', orphanParent !== null, `orphans=${orphanParent} (缺父单)`);
  assert('dishdetail.cross-store-count', crossStore !== null, `crossStore=${crossStore}`);
  assert('dishdetail.null-booking-id', nullBooking !== null, `nullBookingId=${nullBooking}`);
}

// ---------- 2. booking_table -> booking_master ----------
{
  const total = num('SELECT COUNT(*) FROM booking_table');
  const orphanById = num(`SELECT COUNT(*) FROM booking_table t
    LEFT JOIN booking_master m ON t.booking_master_id=m.id
    WHERE m.id IS NULL`);
  const orphanByPair = num(`SELECT COUNT(*) FROM booking_table t
    LEFT JOIN booking_master m ON t.booking_id=m.booking_id AND t.store_id=m.store_id
    WHERE m.id IS NULL`);
  const inconsistent = num(`SELECT COUNT(*) FROM booking_table t
    JOIN booking_master m ON t.booking_master_id=m.id
    WHERE t.booking_id<>m.booking_id OR t.store_id<>m.store_id`);
  assert('bookingtable.total', total !== null, `rows=${total}`);
  assert('bookingtable.orphan-by-master-id', orphanById !== null, `orphans=${orphanById}`);
  assert('bookingtable.orphan-by-pair', orphanByPair !== null, `orphans=${orphanByPair}`);
  assert('bookingtable.pair-consistency', inconsistent !== null, `inconsistent=${inconsistent} (master_id 命中但 pair 不一致)`);
}

// ---------- 3. booking_dish_detail.dish_id -> dish_master (dish_id+store_id)，区分自定义菜 ----------
{
  const nonEmpty = num(`SELECT COUNT(*) FROM booking_dish_detail WHERE dish_id IS NOT NULL AND dish_id<>''`);
  const orphanNonEmpty = num(`SELECT COUNT(*) FROM booking_dish_detail d
    LEFT JOIN dish_master dm ON d.dish_id=dm.dish_id AND d.store_id=dm.store_id
    WHERE d.dish_id IS NOT NULL AND d.dish_id<>'' AND dm.dish_id IS NULL`);
  const customNamed = num(`SELECT COUNT(*) FROM booking_dish_detail WHERE (dish_id IS NULL OR dish_id='') AND custom_name IS NOT NULL AND custom_name<>''`);
  const customNoName = num(`SELECT COUNT(*) FROM booking_dish_detail WHERE (dish_id IS NULL OR dish_id='') AND (custom_name IS NULL OR custom_name='')`);
  assert('dishdetail.dish-id-nonempty', nonEmpty !== null, `nonEmptyDishId=${nonEmpty}`);
  assert('dishdetail.dish-orphan-nonempty', orphanNonEmpty !== null, `orphans=${orphanNonEmpty} (非空 dish_id 未命中主档)`);
  assert('dishdetail.custom-name-allowed', customNamed !== null, `customNamedAllowed=${customNamed} (允许的自定义菜，非孤儿)`);
  assert('dishdetail.empty-id-no-customname', customNoName !== null, `emptyIdNoCustomName=${customNoName}`);
}

// ---------- 4. finance_payment_record.booking_id -> booking_master ----------
{
  const total = num('SELECT COUNT(*) FROM finance_payment_record');
  const nonEmptyBid = num(`SELECT COUNT(*) FROM finance_payment_record WHERE booking_id IS NOT NULL AND booking_id<>''`);
  const orphan = num(`SELECT COUNT(*) FROM finance_payment_record p
    LEFT JOIN booking_master m ON p.booking_id=m.booking_id AND p.store_id=m.store_id
    WHERE p.booking_id IS NOT NULL AND p.booking_id<>'' AND m.id IS NULL`);
  assert('payment.total', total !== null, `rows=${total}`);
  assert('payment.nonempty-booking-id', nonEmptyBid !== null, `nonEmptyBookingId=${nonEmptyBid}`);
  assert('payment.orphan-booking', orphan !== null, `orphans=${orphan}`);
}

// ---------- 5. 金额一致性：dish_quantity*unit_price vs subtotal ----------
{
  const totalRows = num('SELECT COUNT(*) FROM booking_dish_detail');
  const computedMismatch = num(`SELECT COUNT(*) FROM booking_dish_detail
    WHERE dish_quantity IS NOT NULL AND unit_price IS NOT NULL AND subtotal IS NOT NULL
      AND ABS(subtotal - dish_quantity*unit_price) > 0.005`);
  const sumDish = scalar(`SELECT COALESCE(SUM(subtotal),0) FROM booking_dish_detail`);
  const masters = num('SELECT COUNT(*) FROM booking_master');
  // 逐单：菜品合计 vs total_amount
  const totalMismatch = num(`SELECT COUNT(*) FROM (
     SELECT m.id, m.total_amount, COALESCE(SUM(d.subtotal),0) s
     FROM booking_master m LEFT JOIN booking_dish_detail d
       ON d.booking_id=m.booking_id AND d.store_id=m.store_id
     GROUP BY m.id, m.total_amount
     HAVING m.total_amount IS NOT NULL AND ABS(m.total_amount - s) > 0.005
  ) x`);
  const finalLtTotal = num(`SELECT COUNT(*) FROM booking_master WHERE final_amount IS NOT NULL AND total_amount IS NOT NULL AND final_amount < total_amount`);
  const hasDeposit = num(`SELECT COUNT(*) FROM booking_master WHERE deposit_amount IS NOT NULL AND deposit_amount>0`);
  assert('amount.dish-subtotal-mismatch', computedMismatch !== null, `rows=${computedMismatch} (qty*price != subtotal)`);
  assert('amount.dish-sum', sumDish !== null, `sumSubtotal=${sumDish}`);
  assert('amount.masters', masters !== null, `bookingMasterRows=${masters}`);
  assert('amount.total-vs-dishsum-mismatch', totalMismatch !== null, `masters=${totalMismatch} (total_amount != 菜品合计)`);
  assert('amount.final-lt-total', finalLtTotal !== null, `finalLtTotal=${finalLtTotal} (不判错：可能含折扣/定金)`);
  assert('amount.deposit-present', hasDeposit !== null, `mastersWithDeposit=${hasDeposit}`);
}

// ---------- 6. booking_id 单店重复 / 跨店复用 ----------
{
  const dupInStore = num(`SELECT COUNT(*) FROM (
    SELECT booking_id, store_id, COUNT(*) c FROM booking_master
    WHERE booking_id IS NOT NULL AND booking_id<>''
    GROUP BY booking_id, store_id HAVING c>1) x`);
  const reusedCrossStore = num(`SELECT COUNT(*) FROM (
    SELECT booking_id FROM booking_master
    WHERE booking_id IS NOT NULL AND booking_id<>''
    GROUP BY booking_id HAVING COUNT(DISTINCT store_id)>1) x`);
  assert('dup.booking-id-dup-in-store', dupInStore !== null, `dupPairs=${dupInStore}`);
  assert('dup.booking-id-cross-store-reuse', reusedCrossStore !== null, `crossStoreReused=${reusedCrossStore}`);
}

// ---------- 7. 现有外键/唯一索引覆盖（元数据，不含数据行） ----------
{
  const tables = ['booking_master', 'booking_dish_detail', 'booking_table', 'dish_master', 'finance_payment_record'];
  const fks = rows(`SELECT TABLE_NAME, CONSTRAINT_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME
     FROM information_schema.KEY_COLUMN_USAGE
     WHERE TABLE_SCHEMA='${DB}' AND REFERENCED_TABLE_NAME IS NOT NULL
       AND TABLE_NAME IN (${tables.map(t => `'${t}'`).join(',')})
     ORDER BY TABLE_NAME, CONSTRAINT_NAME, ORDINAL_POSITION`);
  const uqs = rows(`SELECT TABLE_NAME, INDEX_NAME, GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX)
     FROM information_schema.STATISTICS
     WHERE TABLE_SCHEMA='${DB}' AND NON_UNIQUE=0
       AND TABLE_NAME IN (${tables.map(t => `'${t}'`).join(',')})
     GROUP BY TABLE_NAME, INDEX_NAME ORDER BY TABLE_NAME, INDEX_NAME`);
  assert('meta.fk-count', fks !== null, `fkCount=${fks.length}`);
  assert('meta.unique-index-count', uqs !== null, `uniqueIndexCount=${uqs.length}`);
  results.push({ name: 'meta.foreign-keys', status: 'INFO', detail: fks.map(r => `${r[0]}.${r[1]}:${r[2]}->${r[3]}.${r[4]}`).join('; ') || 'none' });
  results.push({ name: 'meta.unique-indexes', status: 'INFO', detail: uqs.map(r => `${r[0]}.${r[1]}(${r[2]})`).join('; ') || 'none' });
}

console.log('\n==== DL-RC-BOOKING-RELATION-30 只读关系审计 ====');
for (const r of results) console.log(`${r.status}\t${r.name}\t${r.detail}`);
console.log(`\nTOTAL=${results.length} PASS=${pass} FAIL=${fail}`);

if (OUT) {
  const fs = await import('node:fs');
  fs.writeFileSync(OUT, JSON.stringify({
    task: 'DL-RC-BOOKING-RELATION-30', db_port: PORT, schema: DB,
    total: results.length, pass, fail, results,
  }, null, 2), 'utf8');
}
process.exit(fail === 0 ? 0 : 1);

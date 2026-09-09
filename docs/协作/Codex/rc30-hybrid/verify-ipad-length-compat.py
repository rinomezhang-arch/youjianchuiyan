import subprocess, json, hashlib
from pathlib import Path
from datetime import datetime, timezone
MYSQL = r'C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe'
ROOT = Path(r'F:/solo/artifacts/team-worktrees/codex-rc15-integrated-20260909')
OUT = Path(__file__).parent
DB = 'co_rc33_20260909_0936'
args = [MYSQL, '--no-defaults', '--protocol=tcp', '--host=127.0.0.1', '--port=13317', '--user=root', '--batch', '--raw', '--skip-column-names', '--default-character-set=utf8mb4']
checks = []
def sql(q, database=True, expected=None):
    p = subprocess.run(args + ([DB] if database else []) + ['-e', q], capture_output=True, encoding='utf-8')
    if expected:
        if p.returncode == 0 or expected not in p.stderr:
            raise RuntimeError('Expected rejection ' + expected + ', got exit ' + str(p.returncode))
        return expected
    if p.returncode:
        raise RuntimeError(p.stderr[:500])
    return p.stdout.strip()
result = {'at': datetime.now(timezone.utc).isoformat(), 'schema': DB, 'productionWrites': 0, 'checks': checks, 'sqlHashes': {}}
try:
    if sql("SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='" + DB + "'", False) != '0':
        raise RuntimeError('Named test schema already exists; no reset allowed')
    sql('CREATE DATABASE ' + DB + ' CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci', False)
    sql('CREATE TABLE booking_master(id BIGINT NOT NULL PRIMARY KEY,store_id BIGINT NOT NULL,booking_id VARCHAR(50) NULL,UNIQUE KEY uk_booking_id(booking_id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci')
    sql("INSERT INTO booking_master(id,store_id,booking_id) VALUES(33001,33,'CO33-TEST-001')")
    for name in ['ipad_batch_request_migration_v1.sql', 'ipad_batch_request_migration_v2.sql']:
        data = (ROOT / 'banquet_project/src/main/resources' / name).read_bytes()
        result['sqlHashes'][name] = hashlib.sha256(data).hexdigest()
        sql(data.decode('utf-8-sig'))
    cols = sql("SELECT table_name,character_maximum_length FROM information_schema.columns WHERE table_schema=DATABASE() AND column_name='booking_id' ORDER BY table_name")
    assert cols.splitlines() == ['booking_master\t50', 'ipad_batch_request\t255'], cols
    fk = sql("SELECT column_name,referenced_column_name FROM information_schema.key_column_usage WHERE constraint_schema=DATABASE() AND table_name='ipad_batch_request' AND constraint_name='fk_ipad_batch_booking_scope' ORDER BY ordinal_position")
    assert fk.splitlines() == ['booking_master_id\tid','store_id\tstore_id','booking_id\tbooking_id'], fk
    checks.append({'name':'Canonical v1 and v2 on parent50 child255', 'pass':True, 'lengths':cols, 'fk':fk})
    def insert(store, booking):
        return "INSERT INTO ipad_batch_request(store_id,booking_master_id,booking_id,client_request_id,payload_sha256,operator_id,result_json) VALUES(%d,33001,'%s','CO33-ONLY','%s',33001,'{}')" % (store, booking, 'a'*64)
    before=sql('SELECT (SELECT COUNT(*) FROM booking_master),(SELECT COUNT(*) FROM ipad_batch_request)')
    valid=sql('START TRANSACTION;'+insert(33,'CO33-TEST-001')+';SELECT COUNT(*) FROM ipad_batch_request;ROLLBACK;')
    assert valid == '1'
    checks.append({'name':'Valid scoped receipt inserted and rolled back', 'pass':True})
    for name, store, booking in [('wrong store',34,'CO33-TEST-001'),('wrong booking',33,'CO33-NONEXISTENT')]:
        error=sql('START TRANSACTION;'+insert(store,booking)+';ROLLBACK;', expected='ERROR 1452')
        checks.append({'name':name,'pass':True,'error':error})
    after=sql('SELECT (SELECT COUNT(*) FROM booking_master),(SELECT COUNT(*) FROM ipad_batch_request)')
    assert before == after == '1\t0'
    checks.append({'name':'Retained parent and zero test receipts', 'pass':True,'before':before,'after':after})
    result.update(status='PASS',passed=len(checks),failed=0)
except Exception as e:
    result.update(status='FAIL',error=str(e),passed=len(checks),failed=1)
finally:
    with (OUT / 'result.json').open('x',encoding='utf-8') as f:
        json.dump(result,f,ensure_ascii=True,indent=2)
print(json.dumps({k:result[k] for k in ['status','passed','failed','productionWrites']}))
raise SystemExit(0 if result['status']=='PASS' else 1)

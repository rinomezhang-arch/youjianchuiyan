"""Isolated MySQL rehearsal of the verified stock-take column subset, not full production DDL."""
from pathlib import Path
import json
import subprocess
import uuid

mysql = Path('C:/Program Files/MySQL/MySQL Server 8.4/bin/mysql.exe')
schema = 'stock_precision_' + uuid.uuid4().hex
migration = (Path(__file__).parent / 'migrations/stock_take_precision_v1.sql').read_text(encoding='utf-8')

def run(sql):
    result = subprocess.run([str(mysql), '--no-defaults', '--host=127.0.0.1', '--port=13317',
                             '--user=root', '--batch', '--skip-column-names', '--default-character-set=utf8mb4'],
                            input=sql, capture_output=True, text=True, encoding='utf-8', timeout=30)
    if result.returncode:
        raise RuntimeError(result.stderr)
    return result.stdout.strip()

run(f'CREATE DATABASE {schema}; USE {schema}; ' + '''
CREATE TABLE stock_take_detail (
 detail_id BIGINT PRIMARY KEY,
 system_quantity DECIMAL(10,2) NOT NULL,
 actual_quantity DECIMAL(10,2) NOT NULL,
 diff_quantity DECIMAL(10,2) NULL,
 unit_price DECIMAL(10,2) NULL
) ENGINE=InnoDB;
INSERT INTO stock_take_detail VALUES(1,10.01,8.12,-1.89,0.05);
''')
for _ in range(2):
    run(f'USE {schema};\n' + migration)
old = run(f'USE {schema}; SELECT * FROM stock_take_detail WHERE detail_id=1;')
assert old == '1\t10.010\t8.120\t-1.890\t0.05000000', old
run(f'USE {schema}; INSERT INTO stock_take_detail VALUES(2,10.001,8.123,-1.878,0.05706667);')
new = run(f'USE {schema}; SELECT * FROM stock_take_detail WHERE detail_id=2;')
assert new == '2\t10.001\t8.123\t-1.878\t0.05706667', new
columns = run(f"SELECT COLUMN_NAME,COLUMN_TYPE FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='{schema}' AND TABLE_NAME='stock_take_detail' ORDER BY ORDINAL_POSITION;")
assert columns.count('decimal(12,3)') == 3 and 'decimal(15,8)' in columns, columns
key = run(f"SELECT COLUMN_NAME FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='{schema}' AND TABLE_NAME='stock_take_detail' AND INDEX_NAME='PRIMARY';")
assert key == 'detail_id', key
print(json.dumps({'result':'PASS','synthetic_schema':schema,'old_row':old,'new_row':new,
                  'migration_runs':2,'primary_key_preserved':True,
                  'scope':'verified column subset only; no full production FK/index or load/lock rehearsal'},ensure_ascii=False))

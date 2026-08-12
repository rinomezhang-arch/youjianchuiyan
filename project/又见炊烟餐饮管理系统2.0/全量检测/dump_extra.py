# -*- coding: utf-8 -*-
import pymysql
conn = pymysql.connect(host='localhost', port=3306, user='rino', password='Wo002323',
                       database='banquet', charset='utf8mb4',
                       cursorclass=pymysql.cursors.DictCursor)
for t in ['reimbursement', 'stock_take', 'stock_take_detail', 'stock_loss']:
    print(f'\n=== {t} ===')
    with conn.cursor() as cur:
        cur.execute(f"SHOW FULL COLUMNS FROM `{t}`")
        for r in cur.fetchall():
            field = r.get('Field') or list(r.values())[0]
            typ = r.get('Type') or list(r.values())[1]
            null = r.get('Null') or list(r.values())[2]
            key = r.get('Key') or list(r.values())[3] or ''
            default = r.get('Default')
            comment = r.get('Comment') or list(r.values())[8] or ''
            null_marker = 'NULL' if null == 'YES' else 'NOT NULL'
            default_str = f" DEFAULT '{default}'" if default is not None else ''
            print(f"  {field:32s} {typ:25s} {null_marker:8s} {default_str:30s} {key:5s} {comment}")
conn.close()

import os
import json

base = r'F:\solo\project'
d = os.listdir(base)[0]
exam = os.path.join(base, d)

fp = None
for sub in os.listdir(exam):
    sub_path = os.path.join(exam, sub)
    if not os.path.isdir(sub_path):
        continue
    candidate = os.path.join(sub_path, 'audit_data.json')
    if os.path.exists(candidate):
        fp = candidate
        break

data = json.load(open(fp, 'r', encoding='utf-8'))
print('Summary:', data['summary'])
print()
for item in data['all_scan_items']:
    level = item['level']
    if level in ('FATAL', 'ERROR'):
        actual = str(item['actual'])
        if len(actual) > 250:
            actual = actual[:250] + '...'
        print(f'[{level}] {item["scan_id"]}: {item["title"]}')
        print(f'  Actual: {actual}')
        print(f'  Detail: {item["detail"]}')
        if item.get('fix_sql') and item['fix_sql'].strip():
            print(f'  FixSQL: {item["fix_sql"][:200]}')
        print()

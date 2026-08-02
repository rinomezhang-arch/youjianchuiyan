# -*- coding: utf-8 -*-
import re
import json
import sys

html_path = r'f:\solo\project\又见炊烟餐饮管理系统2.0\全量检测\checkup_v4_super\system_checkup_v4_latest.html'
html = open(html_path, encoding='utf-8').read()

# 多种可能的变量声明模式
patterns = [
    r'const\s+data\s*=\s*(\[.*?\]);',
    r'var\s+data\s*=\s*(\[.*?\]);',
    r'let\s+data\s*=\s*(\[.*?\]);',
    r'window\.__DATA__\s*=\s*(\[.*?\]);',
]
data = None
for p in patterns:
    m = re.search(p, html, re.DOTALL)
    if m:
        try:
            data = json.loads(m.group(1))
            print(f'[OK] matched pattern: {p[:30]}')
            break
        except Exception as e:
            print(f'[WARN] pattern {p[:30]} matched but JSON parse failed: {e}')

if data is None:
    # 降级：直接找 [{ 开头的 JSON 数组
    m = re.search(r'(\[\s*\{.*?\}\s*\])', html, re.DOTALL)
    if m:
        try:
            data = json.loads(m.group(1))
            print('[OK] fallback pattern matched')
        except Exception as e:
            print(f'[ERR] fallback JSON parse failed: {e}')

if not data:
    print('[ERR] no data found')
    sys.exit(1)

print(f'\nTotal items: {len(data)}')
print('=' * 80)

# 统计 level
from collections import Counter
levels = Counter(i.get('level', 'UNKNOWN') for i in data)
print('Level distribution:')
for k, v in sorted(levels.items()):
    print(f'  {k}: {v}')
print('=' * 80)

# 输出 FATAL 项
print('\n### FATAL items ###')
fatal = [i for i in data if i.get('level') == 'FATAL']
print(f'Count: {len(fatal)}')
for i, item in enumerate(fatal, 1):
    print(f'\n[FATAL-{i}]')
    print(f'  category: {item.get("category", "")}')
    print(f'  title:    {item.get("title", "")}')
    print(f'  table:    {item.get("table", "")}')
    print(f'  column:   {item.get("column", "")}')
    msg = item.get('message', '') or item.get('msg', '') or item.get('detail', '')
    if msg:
        print(f'  message:  {msg[:500]}')
    suggest = item.get('suggestion', '') or item.get('suggest', '') or item.get('fix', '')
    if suggest:
        print(f'  suggest:  {suggest[:300]}')

# 输出 ERROR 项
print('\n\n### ERROR items ###')
errors = [i for i in data if i.get('level') == 'ERROR']
print(f'Count: {len(errors)}')
for i, item in enumerate(errors, 1):
    print(f'\n[ERROR-{i}]')
    print(f'  category: {item.get("category", "")}')
    print(f'  title:    {item.get("title", "")}')
    print(f'  table:    {item.get("table", "")}')
    print(f'  column:   {item.get("column", "")}')
    msg = item.get('message', '') or item.get('msg', '') or item.get('detail', '')
    if msg:
        print(f'  message:  {msg[:500]}')
    suggest = item.get('suggestion', '') or item.get('suggest', '') or item.get('fix', '')
    if suggest:
        print(f'  suggest:  {suggest[:300]}')

print('\n' + '=' * 80)
print('Done.')

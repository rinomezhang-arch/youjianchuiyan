"""
生成完整数据字典 Markdown 文件
从数据库直接读取所有表和字段信息
"""
import pymysql
import os
from collections import defaultdict

# 连接数据库
conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='rino',
    password='Wo002323',
    database='banquet',
    charset='utf8mb4',
    cursorclass=pymysql.cursors.DictCursor
)
cur = conn.cursor()

# 获取所有表
cur.execute("""
    SELECT TABLE_NAME, TABLE_COMMENT, TABLE_ROWS
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA='banquet'
    ORDER BY TABLE_NAME
""")
tables = cur.fetchall()

# 获取所有列
cur.execute("""
    SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE,
           COLUMN_KEY, COLUMN_DEFAULT, COLUMN_COMMENT, EXTRA
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA='banquet'
    ORDER BY TABLE_NAME, ORDINAL_POSITION
""")
columns = cur.fetchall()

# 按表分组
cols_by_table = defaultdict(list)
for c in columns:
    cols_by_table[c['TABLE_NAME']].append(c)

# 输出Markdown
lines = []
lines.append('# 又见炊烟餐饮管理系统 2.0 - 完整数据字典')
lines.append('')
lines.append('> 自动生成日期：2026-08-02')
lines.append('> 数据库：banquet（MySQL 8.0）')
lines.append(f'> 总表数：{len(tables)} 张')
lines.append(f'> 总字段数：{len(columns)} 个')
lines.append('')
lines.append('---')
lines.append('')

# 目录
lines.append('## 目录')
lines.append('')
for i, t in enumerate(tables, 1):
    name = t['TABLE_NAME']
    comment = t['TABLE_COMMENT'] or '(无注释)'
    lines.append(f'{i}. [{name}](#{name}) - {comment}')
lines.append('')
lines.append('---')
lines.append('')

# 每张表详情
for t in tables:
    name = t['TABLE_NAME']
    comment = t['TABLE_COMMENT'] or '(无注释)'
    rows = t['TABLE_ROWS'] or 0
    cols = cols_by_table.get(name, [])

    lines.append(f'## {name}')
    lines.append('')
    lines.append(f'**注释**：{comment}  ')
    lines.append(f'**预估行数**：{rows}  ')
    lines.append(f'**字段数**：{len(cols)}')
    lines.append('')

    if cols:
        lines.append('| # | 字段名 | 类型 | 可空 | 键 | 默认值 | 额外 | 注释 |')
        lines.append('|---|--------|------|------|-----|--------|------|------|')
        for i, c in enumerate(cols, 1):
            nullable = 'Y' if c['IS_NULLABLE'] == 'YES' else 'N'
            key = c['COLUMN_KEY'] or ''
            default = str(c['COLUMN_DEFAULT']) if c['COLUMN_DEFAULT'] is not None else '-'
            extra = c['EXTRA'] or ''
            col_comment = c['COLUMN_COMMENT'] or ''
            col_name = c['COLUMN_NAME']
            col_type = c['COLUMN_TYPE']
            lines.append(f'| {i} | `{col_name}` | {col_type} | {nullable} | {key} | {default} | {extra} | {col_comment} |')
        lines.append('')

    lines.append('---')
    lines.append('')

content = '\n'.join(lines)

output_path = r'F:\solo\project\又见炊烟餐饮管理系统2.0\又见炊烟餐饮管理系统 2.0 全套开发交付文档集\02_数据库设计\完整数据字典.md'
with open(output_path, 'w', encoding='utf-8') as f:
    f.write(content)

print(f'DONE: {len(tables)} tables, {len(columns)} columns, {len(content)} bytes')
print(f'Output: {output_path}')

conn.close()

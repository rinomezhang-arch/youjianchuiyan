#!/usr/bin/env python3
"""从 deep_alignment_scan.json 生成字段级精确定位报告"""
import json, os

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
json_path = os.path.join(SCRIPT_DIR, "deep_alignment_scan.json")

with open(json_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

lines = []
lines.append("# 数据库-后端深度对齐报告（字段级精确定位）")
lines.append(f"> 扫描时间：{data['scan_time']}")
lines.append(f"> Entity：62个 | 数据库表：115张 | 问题总数：{data['total']}")
lines.append("")
lines.append("## 统计概览")
lines.append("| 级别 | 数量 | 含义 |")
lines.append("|------|------|------|")
lines.append(f"| 🔴 FATAL | {data['fatal']} | 启动即报错 |")
lines.append(f"| 🟠 ERROR | {data['error']} | 运行时数据错误 |")
lines.append(f"| 🟡 WARNING | {data['warning']} | 风险但可用 |")
lines.append("")

# 按Entity分组
from collections import defaultdict
by_entity = defaultdict(list)
for r in data['results']:
    by_entity[r['entity']].append(r)

# 按问题严重度排序：先FATAL多的Entity
def sort_key(item):
    ents = item[1]
    f = sum(1 for e in ents if e['level'] == 'FATAL')
    e = sum(1 for e in ents if e['level'] == 'ERROR')
    w = sum(1 for e in ents if e['level'] == 'WARNING')
    return (-f, -e, -w)

sorted_entities = sorted(by_entity.items(), key=sort_key)

lines.append("---")
lines.append("")
lines.append("## 逐Entity逐字段问题清单")
lines.append("")

for entity_name, items in sorted_entities:
    tbl = items[0]['table']
    fatal = [r for r in items if r['level'] == 'FATAL']
    error = [r for r in items if r['level'] == 'ERROR']
    warning = [r for r in items if r['level'] == 'WARNING']
    
    lines.append(f"### {entity_name} → 表 `{tbl}`")
    lines.append(f"")
    lines.append(f"FATAL:{len(fatal)} | ERROR:{len(error)} | WARNING:{len(warning)}")
    lines.append("")
    lines.append("| 级别 | 字段 | 问题描述 | 期望 | 实际 |")
    lines.append("|------|------|----------|------|------|")
    
    for r in items:
        level = r['level']
        icon = {'FATAL': '🔴', 'ERROR': '🟠', 'WARNING': '🟡'}.get(level, '')
        
        col_str = f"`{r['table']}`.`{r['col']}`" if r.get('col') else "-"
        issue = r['issue']
        
        # 解析issue提取期望/实际
        expect = "-"
        actual = "-"
        
        if '数据库缺少表' in issue:
            expect = f"`{r['table']}` 表应存在"
            actual = "数据库中不存在此表"
        elif '数据库缺少列' in issue:
            expect = f"`{r.get('col','')}` 列应存在"
            actual = "数据库中无此列"
        elif '类型不匹配' in issue:
            import re
            m = re.search(r'Java=(\S+)\s+DB=(\S+)', issue)
            if m:
                expect = f"Java: `{m.group(1)}`"
                actual = f"DB: `{m.group(2)}`"
        elif 'nullable不一致' in issue:
            m = re.search(r'Entity=(YES|NO)\s+DB=(YES|NO)', issue)
            if m:
                expect = f"Entity: nullable={m.group(1)}"
                actual = f"DB: nullable={m.group(2)}"
        elif 'Entity缺少字段' in issue:
            expect = "Entity应声明此字段"
            actual = "Entity中未声明"
        elif '主键用int' in issue:
            expect = "应使用 `Long` / `bigint`"
            actual = "使用了 `Integer` / `int`"
        elif '主键用varchar' in issue:
            expect = "应使用 `bigint` 自增主键"
            actual = "使用了 `varchar` 字符串主键"
        elif '外键类型不匹配' in issue:
            m = re.search(r'\((.+)\)\s*->\s*\S+\((.+)\)', issue)
            if m:
                expect = f"FK类型: `{m.group(1)}`"
                actual = f"PK类型: `{m.group(2)}`"
        elif 'nullable' in issue and '不一致' in issue:
            m = re.search(r'Entity=(YES|NO)\s+DB=(YES|NO)', issue)
            if m:
                expect = f"Entity要求nullable={m.group(1)}"
                actual = f"DB实际nullable={m.group(2)}"
        
        lines.append(f"| {icon} {level} | {col_str} | {issue} | {expect} | {actual} |")
    
    lines.append("")

# 汇总
lines.append("---")
lines.append("")
lines.append("## FATAL 问题汇总（启动阻塞）")
lines.append("")
for entity_name, items in sorted_entities:
    fatal = [r for r in items if r['level'] == 'FATAL']
    if not fatal: continue
    lines.append(f"### {entity_name} → `{items[0]['table']}`")
    for r in fatal:
        lines.append(f"- **{r['issue']}**")
        lines.append(f"  - {r['detail']}")
    lines.append("")

lines.append("---")
lines.append("")
lines.append("## ERROR 问题汇总（运行时故障）")
lines.append("")
for entity_name, items in sorted_entities:
    error = [r for r in items if r['level'] == 'ERROR']
    if not error: continue
    lines.append(f"### {entity_name} → `{items[0]['table']}`")
    for r in error:
        lines.append(f"- **{r['issue']}**")
        lines.append(f"  - {r['detail']}")
    lines.append("")

out_path = os.path.join(os.path.dirname(SCRIPT_DIR), "数据库深度对齐报告-字段级.md")
with open(out_path, 'w', encoding='utf-8') as f:
    f.write('\n'.join(lines))

print(f"报告已生成: {out_path}")
print(f"总共 {data['total']} 条问题，覆盖 {len(by_entity)} 个Entity")

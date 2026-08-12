#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生成SQL文件并通过docker exec执行"""
import subprocess
import os

SQL_FILE = r"F:\solo\project\又见炊烟餐饮管理系统2.0\scripts\update_dish_image_url.sql"

# 读取dish目录中所有CY开头的jpg文件
dish_dir = r"F:\solo\project\又见炊烟餐饮管理系统2.0\frontend_v3\public\dish"
files = [f for f in os.listdir(dish_dir) if f.startswith('CY') and f.endswith('.jpg')]
files.sort()

print(f"找到 {len(files)} 个菜品图片文件")

# 生成SQL
with open(SQL_FILE, 'w', encoding='utf-8') as f:
    f.write("USE banquet;\n\n")
    for fname in files:
        dish_id = fname.replace('.jpg', '')
        f.write(f"UPDATE dish_master SET image_url = '/dish/{fname}' WHERE dish_id = '{dish_id}' AND store_id = 1;\n")

print(f"SQL文件已生成: {SQL_FILE}")

# 通过docker exec执行SQL文件
cmd = [
    "docker", "exec", "-i", "youjian-mysql-local",
    "mysql", "-urino", "-pWo002323", "banquet"
]

with open(SQL_FILE, 'r', encoding='utf-8') as sql_file:
    result = subprocess.run(cmd, stdin=sql_file, capture_output=True, text=True, timeout=60)

if result.returncode == 0:
    print(f"[OK] 数据库更新成功，{len(files)} 条记录已更新")
else:
    print(f"[FAIL] {result.stderr.strip()}")

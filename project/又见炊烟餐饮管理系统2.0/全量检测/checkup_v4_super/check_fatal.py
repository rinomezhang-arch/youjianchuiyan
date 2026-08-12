#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
核实 FATAL 误判：检查 dish_tag_relation / dish_usage_relation 表结构
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

import pymysql
from config import get_db_config

config = get_db_config()
conn = pymysql.connect(
    host=config.host, port=config.port, user=config.user,
    password=config.password, database=config.database,
    charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor
)

try:
    # 1. 查 dish_tag_relation 表结构
    print("=" * 60)
    print("dish_tag_relation 表结构")
    print("=" * 60)
    with conn.cursor() as cur:
        cur.execute("SHOW FULL COLUMNS FROM dish_tag_relation")
        for row in cur.fetchall():
            print(f"  {row['Field']:20s} {row['Type']:20s} {row['Key']:5s} {row['Null']:3s} {row['Comment'] or ''}")
    
    # 2. 查 dish_usage_relation 表结构
    print("\n" + "=" * 60)
    print("dish_usage_relation 表结构")
    print("=" * 60)
    with conn.cursor() as cur:
        cur.execute("SHOW FULL COLUMNS FROM dish_usage_relation")
        for row in cur.fetchall():
            print(f"  {row['Field']:20s} {row['Type']:20s} {row['Key']:5s} {row['Null']:3s} {row['Comment'] or ''}")
    
    # 3. 查 dish_master 主键
    print("\n" + "=" * 60)
    print("dish_master 主键")
    print("=" * 60)
    with conn.cursor() as cur:
        cur.execute("SHOW FULL COLUMNS FROM dish_master WHERE `Key` = 'PRI'")
        for row in cur.fetchall():
            print(f"  {row['Field']:20s} {row['Type']:20s}")
    
    # 4. 判断是否有 dish_id 列
    print("\n" + "=" * 60)
    print("结论")
    print("=" * 60)
    with conn.cursor() as cur:
        cur.execute("SHOW COLUMNS FROM dish_tag_relation LIKE 'dish_id'")
        has_dish_id = cur.fetchone()
        if has_dish_id:
            print("  ✅ dish_tag_relation 有 dish_id 列")
            print(f"     类型: {has_dish_id['Type']}")
        else:
            print("  ❌ dish_tag_relation 无 dish_id 列")
            print("     → FATAL 是误判，store_id 不是指向 dish_master 的外键")
        
        cur.execute("SHOW COLUMNS FROM dish_usage_relation LIKE 'dish_id'")
        has_dish_id2 = cur.fetchone()
        if has_dish_id2:
            print("  ✅ dish_usage_relation 有 dish_id 列")
            print(f"     类型: {has_dish_id2['Type']}")
        else:
            print("  ❌ dish_usage_relation 无 dish_id 列")
            print("     → FATAL 是误判，store_id 不是指向 dish_master 的外键")

finally:
    conn.close()

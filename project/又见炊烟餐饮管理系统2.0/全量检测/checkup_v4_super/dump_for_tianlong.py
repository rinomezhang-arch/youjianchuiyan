import pymysql
import json
from datetime import datetime

# 连接数据库
conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='rino',
    password='***',
    database='banquet',
    charset='utf8mb4',
    cursorclass=pymysql.cursors.DictCursor
)

cursor = conn.cursor()

print("=" * 80)
print("一、业务规则配置数据")
print("=" * 80)

# 1. config 表
print("\n### 1. config 表完整数据")
cursor.execute("SELECT * FROM config")
configs = cursor.fetchall()
print(f"共 {len(configs)} 条配置：")
for c in configs:
    print(json.dumps(c, ensure_ascii=False, indent=2, default=str))

# 2. 宴会模板相关表
print("\n### 2. 宴会模板、套餐绑定计价规则")
tables = ['banquet_template', 'banquet_template_dish', 'banquet_template_rel', 
          'template_category_rel', 'template_dish_rel', 'package_master', 'package_dish']
for table in tables:
    try:
        cursor.execute(f"SELECT COUNT(*) as cnt FROM {table}")
        cnt = cursor.fetchone()['cnt']
        print(f"\n{table}: {cnt} 条记录")
        if cnt > 0:
            cursor.execute(f"SELECT * FROM {table} LIMIT 5")
            rows = cursor.fetchall()
            for r in rows:
                print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
    except Exception as e:
        print(f"{table}: 表不存在或查询失败 - {e}")

print("\n" + "=" * 80)
print("二、状态字典与枚举")
print("=" * 80)

# 3. 状态字典表
dict_tables = ['sys_dict', 'sys_dict_data', 'sys_dict_type', 'sys_dict_item']
for table in dict_tables:
    try:
        cursor.execute(f"SELECT COUNT(*) as cnt FROM {table}")
        cnt = cursor.fetchone()['cnt']
        print(f"\n{table}: {cnt} 条记录")
        if cnt > 0:
            cursor.execute(f"SELECT * FROM {table}")
            rows = cursor.fetchall()
            for r in rows:
                print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
    except Exception as e:
        print(f"{table}: 表不存在或查询失败 - {e}")

# 4. 桌台状态
print("\n### 桌台状态示例")
try:
    cursor.execute("SELECT table_id, table_number, status, table_type FROM table_master LIMIT 10")
    rows = cursor.fetchall()
    for r in rows:
        print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
except Exception as e:
    print(f"查询失败: {e}")

# 5. 预订状态
print("\n### 预订状态示例")
try:
    cursor.execute("SELECT booking_id, status, booking_time FROM booking_master LIMIT 10")
    rows = cursor.fetchall()
    for r in rows:
        print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
except Exception as e:
    print(f"查询失败: {e}")

# 6. 订单状态
print("\n### 订单状态示例")
try:
    cursor.execute("SELECT order_id, status, total_amount FROM order_master LIMIT 10")
    rows = cursor.fetchall()
    for r in rows:
        print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
except Exception as e:
    print(f"查询失败: {e}")

print("\n" + "=" * 80)
print("三、WebSocket/推送相关表")
print("=" * 80)

# 7. 通知表
try:
    cursor.execute("SELECT COUNT(*) as cnt FROM sys_notification")
    cnt = cursor.fetchone()['cnt']
    print(f"\nsys_notification: {cnt} 条记录")
    if cnt > 0:
        cursor.execute("SELECT * FROM sys_notification LIMIT 5")
        rows = cursor.fetchall()
        for r in rows:
            print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
except Exception as e:
    print(f"sys_notification: 表不存在或查询失败 - {e}")

# 8. 日志表
log_tables = ['audit_log', 'change_log', 'kitchen_log']
for table in log_tables:
    try:
        cursor.execute(f"SELECT COUNT(*) as cnt FROM {table}")
        cnt = cursor.fetchone()['cnt']
        print(f"\n{table}: {cnt} 条记录")
        if cnt > 0:
            cursor.execute(f"SELECT * FROM {table} LIMIT 3")
            rows = cursor.fetchall()
            for r in rows:
                print(json.dumps(r, ensure_ascii=False, indent=2, default=str))
    except Exception as e:
        print(f"{table}: 表不存在或查询失败 - {e}")

print("\n" + "=" * 80)
print("四、核心业务表结构")
print("=" * 80)

# 9. 核心表结构
core_tables = ['booking_master', 'table_master', 'order_master', 'order_detail', 
               'dish_master', 'deposit_master', 'member_master']
for table in core_tables:
    try:
        cursor.execute(f"SHOW CREATE TABLE {table}")
        result = cursor.fetchone()
        print(f"\n### {table}")
        print(result['Create Table'])
    except Exception as e:
        print(f"{table}: 表不存在或查询失败 - {e}")

conn.close()
print("\n" + "=" * 80)
print("数据导出完成")
print("=" * 80)

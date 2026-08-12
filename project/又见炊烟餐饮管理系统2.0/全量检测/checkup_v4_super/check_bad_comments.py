import pymysql

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='rino',
    password='Wo002323',
    database='banquet',
    charset='utf8mb4'
)

cursor = conn.cursor(pymysql.cursors.DictCursor)

# 查找所有包含 ?? 的表注释
cursor.execute("""
    SELECT TABLE_NAME, TABLE_COMMENT
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'banquet' AND TABLE_COMMENT LIKE '%?%'
    ORDER BY TABLE_NAME
""")

results = cursor.fetchall()
print(f'Found {len(results)} tables with ?? in comment:')
for r in results:
    print(f"  {r['TABLE_NAME']}: {r['TABLE_COMMENT']}")

conn.close()

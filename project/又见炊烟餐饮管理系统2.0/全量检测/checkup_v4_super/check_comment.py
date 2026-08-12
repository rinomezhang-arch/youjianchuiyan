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

# 查看 sys_role_permission 表的注释
cursor.execute("""
    SELECT TABLE_NAME, TABLE_COMMENT, TABLE_COLLATION
    FROM information_schema.TABLES
    WHERE TABLE_SCHEMA = 'banquet' AND TABLE_NAME = 'sys_role_permission'
""")

result = cursor.fetchone()
print('Table:', result['TABLE_NAME'])
print('Comment:', result['TABLE_COMMENT'])
print('Comment type:', type(result['TABLE_COMMENT']))
print('Comment repr:', repr(result['TABLE_COMMENT']))
print('Collation:', result['TABLE_COLLATION'])

# 尝试不同的编码方式
comment = result['TABLE_COMMENT']
if isinstance(comment, str):
    try:
        # 尝试 latin-1 -> utf-8
        fixed = comment.encode('latin-1').decode('utf-8')
        print('Fixed (latin-1->utf-8):', fixed)
    except:
        print('latin-1->utf-8 failed')
    
    try:
        # 尝试 gbk -> utf-8
        fixed = comment.encode('gbk').decode('utf-8')
        print('Fixed (gbk->utf-8):', fixed)
    except:
        print('gbk->utf-8 failed')

conn.close()

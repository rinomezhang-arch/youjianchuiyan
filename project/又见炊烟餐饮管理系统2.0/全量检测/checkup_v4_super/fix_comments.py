import pymysql

conn = pymysql.connect(
    host='localhost',
    port=3306,
    user='rino',
    password='Wo002323',
    database='banquet',
    charset='utf8mb4'
)

cursor = conn.cursor()

# 根据表名推断正确的注释
fixes = {
    'ai_chat_history': 'AI聊天历史',
    'ai_memory': 'AI记忆',
    'banquet_template_rel': '宴会-模板关联',
    'dish_category': '菜品分类/类别',
    'report_staff_kpi': '员工KPI报表',
    'sys_role_permission': '系统-角色权限',
    'sys_user_role': '系统-用户角色',
    'template_category_rel': '模板-分类关联',
    'template_dish_rel': '模板-菜品关联',
}

# 生成并执行修复SQL
for table, comment in fixes.items():
    sql = f"ALTER TABLE {table} COMMENT = '{comment}'"
    print(f'Executing: {sql}')
    cursor.execute(sql)
    print(f'  [OK] Fixed')

conn.commit()
print(f'\n[OK] All {len(fixes)} table comments fixed')

conn.close()

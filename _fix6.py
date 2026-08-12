# -*- coding: utf-8 -*-
import os

# 动态构造路径，避免中文编码问题
base = r'F:\solo\project'
project_dir = os.listdir(base)[0]  # 又见炊烟餐饮管理系统
health_path = os.path.join(base, project_dir, '体检', 'health_check.py')
print('path:', health_path)
print('exists:', os.path.exists(health_path))

with open(health_path, 'r', encoding='utf-8') as f:
    content = f.read()

# 更新代码路径到新项目位置
old_front = r'FRONT_CODE_PATH = r"F:\solo\frontend_v3"'
new_front = r'FRONT_CODE_PATH = r"F:\solo\project' + '\\' + project_dir + r'\frontend_v3"'
content = content.replace(old_front, new_front)

old_back = r'BACK_CODE_PATH = r"F:\solo\banquet_project\src\main\java\com\youjian\banquet"'
new_back = r'BACK_CODE_PATH = r"F:\solo\project' + '\\' + project_dir + r'\banquet_project\src\main\java\com\youjian\banquet"'
content = content.replace(old_back, new_back)

with open(health_path, 'w', encoding='utf-8') as f:
    f.write(content)

# 验证
with open(health_path, 'r', encoding='utf-8') as f:
    lines = f.readlines()
print('L41:', lines[40].rstrip())
print('L42:', lines[41].rstrip())
print('done')

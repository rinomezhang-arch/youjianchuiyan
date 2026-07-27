# 又见炊烟私房菜 · 餐饮管理系统 — 文件地址清单

## 生产环境（腾讯云 1.13.173.213）

| 文件 | 路径 | 说明 |
|------|------|------|
| 前端dist | /home/ubuntu/dist/ | Vue打包产物 |
| 后端JAR | /home/ubuntu/banquet-api.jar | Spring Boot |
| Nginx配置 | /etc/nginx/conf.d/youjianchuiyan.com.conf | 网站 |
| Nginx日志 | /var/log/nginx/ | 访问+错误日志 |
| Java源码 | /home/ubuntu/java-backend-src/ | 本地副本 |

## COS对象存储（/mnt/cos/）

| 目录 | 说明 |
|------|------|
| /mnt/cos/天龙/ | 天龙灵魂备份(MEMORY/SOUL/AGENTS等) |
| /mnt/cos/地龙/ | 地龙核心文件 |
| /mnt/cos/SOLO/ | SOLO文件 |
| /mnt/cos/天地双龙工作空间/ | 三龙共享工作区 |
| ├── java-backend/ | 后端代码交付区(20个子目录) |
| ├── 项目管理/餐饮管理系统/ | 📁 项目管理文档 |
| ├── 单页系统分析.md | 旧系统分析 |
| ├── 功能差距清单.md | 缺口清单 |
| ├── 公共对话.md | 三龙对话记录 |
| ├── 任务看板.md | 任务跟踪 |
| ├── message-log.txt | 通信日志 |
| ├── dilong-outbox.txt | 地龙留言板 |
| ├── tianlong-outbox.txt | 天龙留言板 |
| └── 复活必读.txt | 复活协议 |
| /mnt/cos/餐饮管理系统ai助手/ | 餐饮管理系统旧文件 |
| └── 单页餐饮管理系统.html | 7793行旧系统 |
| /mnt/cos/openclaw/ | 天龙记忆备份 |

## Windows本地（秋老板）

| 文件 | 路径 |
|------|------|
| OpenClaw配置 | C:\Users\rinom\.openclaw\openclaw.json |
| 模型配置 | C:\Users\rinom\.openclaw\agents\main\agent\models.json |
| 环境变量 | C:\Users\rinom\.openclaw\.env |
| 守护脚本 | C:\Users\rinom\.openclaw\gateway.vbs |
| 前端源码 | F:\trae\（待确认） |

## 数据库（MySQL 8.0 banquet）

| 表 | 说明 | 行数 |
|------|------|------|
| dish_master | 菜品 | 588 |
| table_master | 桌台 | 68 |
| booking_master | 预订主表 | - |
| booking_table | 预订-桌台关联 | - |
| booking_dish | 预订-菜品关联 | 新建 |
| customer_master | 客户 | 34 |
| package_master | 套餐 | 4 |
| staff_master | 员工 | 23 |
| store_master | 门店 | 2 |
| ingredient_master | 食材 | 1210 |

---
🦞 整理于 2026-07-22

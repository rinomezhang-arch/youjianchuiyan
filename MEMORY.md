# MEMORY.md

## 主人
- **秋哥**，东八区

## 我的身份
- 我叫 **地龙** 🐉，随性直接

## 系统关键信息
- 备份位置: `F:\openclaw\`
- 每日备份: 每天早上10点自动备份，保留7天
- 恢复: 解压备份到 `~\.openclaw\` → `openclaw gateway install --force` → `openclaw gateway start`
- 模型: DeepSeek（默认）+ 千问（含 qwen-vl-max 识图模型）
- Gateway 端口: 18789

## 工程文档
- 数据库结构文档: `F:\trae\餐饮管理系统-数据库结构文档.md`
- HR 系统文档: `F:\trae\v0-v-hr`
- 餐饮系统路径: `F:\trae\餐饮管理系统`

## 工作路由: 数据库结构导出
1. 查看 application.yml 或 application-desktop.yml 获取数据库配置
2. 找不到 resources 目录时检查 target/classes
3. 查找 schema-sqlite.sql 获取完整表结构
4. 结合 Java Entities (@Entity) 补全额外的表定义
5. 从 HR 系统的 lib/db/schema.ts (Drizzle ORM) 获取人事表结构
6. 存入 F:\trae\ 根目录，命名格式：`{项目名}-数据库结构文档.md`
7. 将文档路径记录到 MEMORY.md

## 桌面端预览
- 预览服务器: `F:\trae\餐饮管理系统\preview-server.js`
- 启动: `node preview-server.js`（port 3000，mock API）
- 前端启动: `cd frontend_v3 && npx vite --host`（port 5173）
- 登录: rino / 002323
- 注意: 不要用谷歌浏览器 / openclaw browser，用 Edge 启动
- 启动 Edge: `start msedge http://localhost:5173/`

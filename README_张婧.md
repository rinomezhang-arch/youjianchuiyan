# 又见炊烟餐饮管理系统 — 开发指南

> 天龙整理 | 2026-07-31

## 项目结构

```
youjianchuiyan/
├── banquet_project/          # 后端 Spring Boot 3.2.5 + Java 17 + Maven
│   └── src/main/java/com/youjian/banquet/
│       ├── controller/       # 28个Controller，168个API端点
│       ├── entity/           # 数据实体
│       ├── repository/       # JPA仓库
│       ├── service/          # 业务逻辑
│       ├── aop/              # 切面（审计日志、门店数据隔离）
│       └── config/           # 配置类
├── frontend_v3/              # 前端 Vue 3 + Vite + Element Plus
│   └── src/
│       ├── api/              # API调用（118个函数）
│       ├── views/            # 页面组件（82个.vue）
│       ├── router/           # 路由（83条）
│       └── utils/            # 工具函数
├── youjian-docker/           # Docker部署配置
├── SQL脚本                    # 数据库初始化脚本
├── 审查报告                   # 系统审计报告
└── 开发详情/                  # 技术文档
```

## 环境要求

### 后端
- JDK 17+
- Maven 3.8+
- MySQL 8.0

### 前端
- Node.js 18+
- npm 9+

## 快速启动

### 1. 数据库
```sql
-- 创建数据库
CREATE DATABASE banquet DEFAULT CHARACTER SET utf8mb4;
-- 导入表结构
source banquet_project/sql/banquet_init.sql;
-- 导入基础数据
source banquet_project/sql/init_data.sql;
```

### 2. 配置文件
编辑 `banquet_project/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banquet
    username: root
    password: 你的密码
jwt:
  secret: 你的JWT密钥（至少32字符）
```

### 3. 启动后端
```bash
cd banquet_project
mvn spring-boot:run
# 后端运行在 http://localhost:8080
```

### 4. 启动前端
```bash
cd frontend_v3
npm install
npm run dev
# 前端运行在 http://localhost:5173
# API自动代理到 localhost:8080
```

## 前后端通信

- 前端所有API走 `/api/*` 路径，Vite dev server代理到后端8080
- 食材供应链模块走 `/menu-api/*` 路径（部分Controller双路径支持）
- JWT Token存储在localStorage，请求头 `Authorization: Bearer <token>`

## 多门店架构

- store_id 数据隔离：宁国(store_id=1)、宣城(store_id=2)
- 门店切换：前端Header有门店选择器
- 超管(permission_level=100)可跨店查看

## 已知待修复

详见 `SYSTEM_AUDIT_REPORT_V3.md`
- P0: 4处密码硬编码(jwt secret默认值)、5张表无store_id隔离
- S: 财务+盘点流程断裂、~25个死接口

## 分工

- 天龙(🦞): 项目统筹、代码审查、整合
- 地龙(🐉): 后端 — Python脚本、SQL、业务逻辑
- Trae-BOT: 前端 — Vue组件、CSS、路由

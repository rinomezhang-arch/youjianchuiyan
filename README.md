# 又见炊烟餐饮管理系统 2.0

> 双门店餐饮一体化管理平台 | MySQL 8.0 + Spring Boot 3.2.5 + Vue 3
> 最后更新：2026-08-01

---

## 一、项目结构

```
又见炊烟餐饮管理系统2.0/
│
├── banquet_project/          # Java 后端（Spring Boot 3.2.5 + JPA + MySQL）
├── frontend_v3/               # Vue 3 前端（Element Plus + Vite）
├── chat-server/               # WebSocket 聊天服务
├── youjian-docker/            # Docker 部署（MySQL + 后端 + 前端）
│   └── mysql/
│       ├── init/              # 数据库初始化脚本（建表 + RBAC）
│       └── backup_strategy.sh # 自动备份脚本
│
├── scripts/                   # 运维脚本库
│   ├── migrations/            # 数据库迁移脚本（10个）
│   ├── seed/                  # 种子数据脚本（5个）
│   └── backup/                # 备份脚本
│
├── 体检/                       # 审计报告（7份）
├── 开发详情/                   # 开发规划文档
├── .github/                   # GitHub Actions 工作流
└── README.md                  # 本文件
```

---

## 二、技术栈

| 层 | 技术 | 版本 |
|----|------|------|
| 前端 | Vue 3 + Element Plus + Vite | 3.x |
| 后端 | Spring Boot + JPA Hibernate | 3.2.5 |
| 数据库 | MySQL | 8.0 |
| 字符集 | utf8mb4 / utf8mb4_0900_ai_ci | 全库统一 |
| 部署 | Docker + Docker Compose | - |
| AI | 天龙 OpenClaw Gateway | deepseek-chat |

---

## 三、数据库概览

| 指标 | 数量 |
|------|------|
| 总表数 | 113 |
| 总字段数 | 1816 |
| 外键数 | 40 |
| 索引数 | 110 |
| JPA 实体类 | 62 |
| JPA Repository | 55 |

### 门店规划

| store_id | 门店 | 角色 |
|----------|------|------|
| 0 | 全局 | 超级总经理（GM，全门店） |
| 1 | 宁国总店 | 总店员工（HQ_STAFF，仅本店） |
| 2 | 宣城分店 | 分店店长（STORE_MANAGER）/ 服务员（WAITER） |

---

## 四、快速启动

### 4.1 环境变量（必须配置）

```bash
# 数据库
export MYSQL_HOST=mysql
export MYSQL_DATABASE=banquet
export MYSQL_USER=rino
export MYSQL_PASSWORD=你的数据库密码

# 安全密钥（生产环境必须，无默认值）
export JWT_SECRET="至少32字节的JWT签名密钥"
export AES_SECRET_KEY="32字节AES-256加密密钥"

# 腾讯云 COS（文件存储）
export COS_SECRET_ID="你的COS密钥ID"
export COS_SECRET_KEY="你的COS密钥"
export COS_BUCKET="你的COS桶名"

# AI 服务
export TIANLONG_TOKEN="天龙网关Token"
```

### 4.2 Docker 部署

```bash
cd youjian-docker
docker-compose up -d
```

### 4.3 本地开发

```bash
# 后端
cd banquet_project
mvn spring-boot:run

# 前端
cd frontend_v3
npm install
npm run dev
```

### 4.4 访问地址

| 服务 | 地址 |
|------|------|
| 前端 | http://localhost:5173 |
| 后端 API | http://localhost:8080/api |
| 健康检查 | http://localhost:8080/actuator/health |
| API 文档 | http://localhost:8080/swagger-ui.html |
| API JSON | http://localhost:8080/api-docs |

---

## 五、脚本使用说明

### 5.1 数据库初始化（首次部署）

```bash
# 1. 建库
docker exec youjian-mysql-local mysql -urino -p密码 -e \
  "CREATE DATABASE IF NOT EXISTS banquet CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"

# 2. 建表（Docker初始化脚本）
docker exec -i youjian-mysql-local mysql -urino -p密码 banquet \
  < youjian-docker/mysql/init/banquet_init.sql

# 3. RBAC权限初始化
docker exec -i youjian-mysql-local mysql -urino -p密码 banquet \
  < youjian-docker/mysql/init/rbac_init.sql

# 4. 种子数据
docker exec -i youjian-mysql-local mysql -urino -p密码 banquet \
  < scripts/seed/init_real_data_v4.sql
```

### 5.2 数据库迁移脚本（按顺序执行）

| 顺序 | 脚本 | 说明 | 路径 |
|------|------|------|------|
| 1 | db_fix_v1.sql | P0 致命问题修复（booking_id类型/时间字段统一） | scripts/migrations/ |
| 2 | db_fix_v2.sql | P1 严重问题修复（外键补齐/字段冗余标注） | scripts/migrations/ |
| 3 | db_fix_v3.sql | P2 警告问题修复（冗余表删除/测试数据清理） | scripts/migrations/ |
| 4 | fix_collation_v1.sql | 排序规则统一（119表→utf8mb4_0900_ai_ci） | scripts/migrations/ |
| 5 | finance_migration_v1.sql | 财务表迁移 | scripts/migrations/ |
| 6 | hr_migration_v1.sql | HR表迁移（month_salary表创建） | scripts/migrations/ |
| 7 | salary_migration_v1.sql | 薪资数据迁移（staff_master→month_salary） | scripts/migrations/ |
| 8 | dedup_migration_v1.sql | 去重迁移 | scripts/migrations/ |
| 9 | post_migration_v1.sql | 后期迁移 | scripts/migrations/ |
| 10 | stock_transfer_migration_v1.sql | 库存调拨迁移 | scripts/migrations/ |

**执行方式**：
```bash
docker exec -i youjian-mysql-local mysql -urino -p密码 banquet < scripts/migrations/db_fix_v1.sql
```

### 5.3 种子数据脚本

| 脚本 | 说明 | 路径 |
|------|------|------|
| init_real_data_v4.sql | 初始化真实数据（门店/员工/菜品/客户） | scripts/seed/ |
| banquet_full_seed.sql | 全量种子数据 | scripts/seed/ |
| seed_kitchen_test_data.sql | 厨房模块测试数据 | scripts/seed/ |
| schema_kitchen.sql | 厨房表结构 | scripts/seed/ |
| seed_e2e_test_data.sql | 端到端测试数据 | scripts/seed/ |

### 5.4 数据备份

```bash
# 手动备份
docker exec youjian-mysql-local mysqldump -urino -p密码 \
  --single-transaction --routines --triggers --events banquet | gzip > backup.sql.gz

# 自动备份（建议crontab每日02:00执行）
# 0 2 * * * /path/to/scripts/backup/backup_strategy.sh
bash scripts/backup/backup_strategy.sh

# 恢复
gunzip < backup.sql.gz | docker exec -i youjian-mysql-local mysql -urino -p密码 banquet
```

---

## 六、安全设计

### 6.1 数据加密

| 数据类型 | 加密 | 脱敏 | 字段 |
|----------|------|------|------|
| 银行账号 | AES-256-GCM | 6222****7890 | FinanceAccount/StaffMaster/SupplierMaster/StoreInfo.bank_account |
| 身份证号 | AES-256-GCM | 342**********1234 | StaffMaster.id_card |

- 加密方式：JPA AttributeConverter 自动加解密
- 密钥注入：环境变量 AES_SECRET_KEY（无硬编码默认值）

### 6.2 API 安全

| 安全措施 | 实现 |
|----------|------|
| JWT 认证 | JwtAuthInterceptor 拦截 /api/** |
| 门店数据隔离 | StoreDataScopeAspect + UserContext（105表 store_id 隔离） |
| API 限流 | 登录 5次/分钟，普通 60次/分钟 |
| 审计日志 | AuditLogAspect 拦截所有 POST/PUT/DELETE |
| SQL 注入防护 | 全部参数化查询（?占位符） |

---

## 七、RBAC 权限体系

### 角色 → 权限

| 角色 | 权限数 | 数据范围 | 可操作 |
|------|--------|---------|--------|
| GM（超级总经理） | 20 | all（全门店） | 全部功能 |
| HQ_STAFF（总店员工） | 11 | store（仅本店） | 预订/菜品/客户/桌位/库存/财务查看 |
| STORE_MANAGER（分店店长） | 15 | store（仅本店） | HQ权限 + 员工管理 + 菜品管理 + 库存管理 |
| WAITER（分店服务员） | 5 | store（仅本店） | 预订 + 客户 + 桌位 |

### 权限表结构

```
sys_role (4角色)
  ├── sys_user_role → staff_master (4映射)
  └── sys_role_permission → sys_permission (20权限点)
sys_menu (15菜单) → permission_code → sys_permission
```

---

## 八、审计报告

位于 `体检/` 文件夹：

| 报告 | 内容 |
|------|------|
| SYSTEM_AUDIT_REPORT_V4.md | 初始系统审计（36项数据库问题修复） |
| DEEP_AUDIT_REPORT_V7.md | 深度审计（字段注释/外键/实体一致性/E2E） |
| DEAD_CORNER_AUDIT_V8.md | 死角审计（排序规则/重复表/类型不一致） |
| FINAL_COMPLETION_V9.md | 全量待处理事项完成（P0/P1/P2） |
| ISOLATION_RBAC_CASCADE_V10.md | 分店隔离·RBAC·级联关系审计 |
| HIDDEN_RISKS_RESOLVED_V11.md | 15项系统隐患消除 |
| DATABASE_DESIGN_MANUAL.md | 数据库规划设计使用说明书（10章+附录） |

---

## 九、级联关系

| 策略 | 数量 | 适用场景 |
|------|------|---------|
| CASCADE | 12 | 明细表（删主表自动清明细） |
| RESTRICT | 18 | 业务关键表（有引用禁止删除） |
| NO ACTION | 10 | 类似 RESTRICT |
| SET NULL | 1 | 非关键关联 |

---

## 十、编译与构建

### 后端编译

```bash
cd banquet_project
mvn clean compile -q    # 编译
mvn clean package -DskipTests  # 打包
```

### 前端构建

```bash
cd frontend_v3
npm install
npm run build            # 生产构建
npm run dev              # 开发模式
```

---

## 十一、新增业务表检查清单

- [ ] 表名 snake_case，含 store_id 字段
- [ ] 字符集 utf8mb4，排序规则 utf8mb4_0900_ai_ci
- [ ] 包含 created_at + updated_at 审计字段
- [ ] 创建 JPA 实体类（@Table + @Id + @Column + @PrePersist/@PreUpdate）
- [ ] 创建 Repository 接口
- [ ] 敏感字段加 @Convert + @JsonSerialize
- [ ] Controller 层使用 resolveQueryStoreId() 门店隔离
- [ ] 写操作方法加 @Transactional
- [ ] 外键关系明确（CASCADE/RESTRICT）
- [ ] 关键查询字段添加索引

---

## 十二、项目目录详细说明

### banquet_project/ — Java 后端

```
banquet_project/
├── src/main/java/com/youjian/banquet/
│   ├── aop/              # AOP切面（审计日志 + 门店数据隔离）
│   ├── config/           # 配置类（加密/限流/CORS/JWT）
│   ├── controller/       # 控制器（48个）
│   ├── dto/              # 数据传输对象
│   ├── entity/           # JPA实体类（62个）
│   ├── exception/        # 全局异常处理
│   ├── repository/       # JPA Repository（55个）
│   ├── service/          # 业务服务层（27个）
│   ├── util/             # 工具类（AES/脱敏/用户上下文）
│   └── BanquetApplication.java  # 启动类
├── src/main/resources/
│   ├── application.yml          # 配置文件（含dev/docker/prod profiles）
│   ├── application-prod.yml    # 生产环境配置
│   ├── logback-spring.xml      # 日志配置
│   └── *.sql                   # 迁移脚本（已复制到 scripts/migrations/）
└── pom.xml                      # Maven 配置
```

### frontend_v3/ — Vue 3 前端

```
frontend_v3/
├── src/
│   ├── api/              # API 请求层
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── i18n/             # 国际化
│   ├── router/           # 路由
│   ├── store/            # Vuex 状态管理
│   ├── utils/            # 工具函数
│   └── views/            # 页面视图
├── index.html
└── package.json
```

### youjian-docker/ — Docker 部署

```
youjian-docker/
├── mysql/
│   ├── init/             # 初始化脚本
│   │   ├── banquet_init.sql   # 建表脚本
│   │   └── rbac_init.sql      # RBAC权限初始化
│   └── backup_strategy.sh     # 自动备份脚本
├── docker-compose.yml    # Docker Compose 配置
└── Dockerfile            # 后端镜像
```

### scripts/ — 运维脚本库

```
scripts/
├── migrations/           # 数据库迁移脚本（10个SQL）
├── seed/                # 种子数据脚本（5个SQL）
└── backup/              # 备份脚本（1个Shell）
```

---

## 十三、环境配置

### 环境变量清单

| 变量名 | 用途 | 必填 | 默认值 |
|--------|------|------|--------|
| MYSQL_HOST | 数据库主机 | 否 | mysql |
| MYSQL_DATABASE | 数据库名 | 否 | banquet |
| MYSQL_USER | 数据库用户 | 否 | rino |
| MYSQL_PASSWORD | 数据库密码 | ✅ | - |
| JWT_SECRET | JWT签名密钥（≥32字节） | ✅ | - |
| AES_SECRET_KEY | AES加密密钥（32字节） | ✅ | - |
| COS_SECRET_ID | 腾讯云COS密钥ID | 按需 | - |
| COS_SECRET_KEY | 腾讯云COS密钥 | 按需 | - |
| COS_BUCKET | COS桶名 | 按需 | - |
| TIANLONG_TOKEN | 天龙AI网关Token | 按需 | - |

### 配置文件策略

| 环境 | 配置文件 | 密钥策略 |
|------|---------|---------|
| 本地开发 | application.yml（默认profile） | 硬编码默认值（方便开发） |
| Docker | application.yml（docker profile） | 强制环境变量（无默认值） |
| 生产 | application-prod.yml | 强制环境变量（无默认值） |

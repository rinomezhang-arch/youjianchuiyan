# Docker 容器部署脚本与操作指南

> 适用：`youjian-docker/docker-compose.yml`  
> 维护：地龙（DL-BOT）  
> 更新：2026-08-02

---

## 1. 文件清单

```
youjian-docker/
├── docker-compose.yml          # 容器编排
├── .env                        # 环境变量（被 .gitignore 忽略）
├── mysql/
│   └── init/
│       ├── banquet_init.sql    # 业务表 DDL
│       └── rbac_init.sql       # RBAC 表 DDL
├── backend/
│   └── Dockerfile              # 后端镜像构建
└── frontend/
    └── Dockerfile              # 前端镜像构建（如需）
```

## 2. 容器编排

`docker-compose.yml` 定义 4 个服务：

| 服务 | 容器名 | 镜像 | 端口 | 依赖 |
|------|--------|------|------|------|
| mysql | youjian-mysql | mysql:8.0 | 3306 | - |
| rabbitmq | youjian-rabbitmq | rabbitmq:3.13-management-alpine | 5672/15672 | - |
| backend | youjian-backend | 自构建（OpenJDK 17） | 8080 | mysql healthy + rabbitmq started |
| frontend | youjian-frontend | nginx:alpine | 80 | backend |

## 3. 环境变量配置

### 3.1 编辑 `.env`
```bash
cd youjian-docker
cp .env.example .env  # 如有 example
vim .env
```

`.env` 必填项：
```ini
# MySQL
MYSQL_DATABASE=banquet
MYSQL_USER=banquet_user
MYSQL_PASSWORD=<你的强密码>
MYSQL_ROOT_PASSWORD=<你的强密码>

# JWT
JWT_SECRET=<你的JWT密钥>

# AES 加密
AES_SECRET_KEY=<你的AES密钥>

# RabbitMQ
RABBITMQ_USER=banquet_mq
RABBITMQ_PASSWORD=<你的强密码>
RABBITMQ_VHOST=/
NOTIFY_ENABLED=true

# 腾讯云 COS（如需）
COS_SECRET_ID=
COS_SECRET_KEY=
COS_BUCKET=
COS_BASE_URL=

# 天龙 token（如需）
TIANLONG_TOKEN=
```

### 3.2 生成强密码
```bash
openssl rand -base64 32   # 用于 MYSQL_PASSWORD
openssl rand -base64 48   # 用于 JWT_SECRET
openssl rand -hex 32      # 用于 AES_SECRET_KEY
```

## 4. 部署步骤

### 4.1 首次部署
```bash
cd youjian-docker

# 1. 配置 .env（见上）

# 2. 拉取/构建镜像
docker compose build

# 3. 启动所有服务
docker compose up -d

# 4. 查看启动状态
docker compose ps
docker compose logs -f backend  # 看后端日志
```

### 4.2 验证
```bash
# MySQL 健康
docker exec youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD -e "SELECT 1;"

# RabbitMQ 健康
docker exec youjian-rabbitmq rabbitmqctl status

# 后端健康
curl http://localhost:8080/actuator/health
# 期望：{"status":"UP"}

# 后端 Swagger
curl http://localhost:8080/swagger-ui.html

# 前端
curl http://localhost/
```

### 4.3 数据库初始化
首次启动会自动执行 `mysql/init/*.sql`。如需重新初始化：
```bash
docker compose down -v  # 删除数据卷
docker compose up -d
```

## 5. 增量 SQL 迁移

### 5.1 业务规则配置
```bash
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet \
  < scripts/migrations/business_config_migration_v1.sql
```

### 5.2 字典补全
```bash
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet \
  < scripts/migrations/dict_extend_migration_v1.sql
```

### 5.3 table_status 修复
```bash
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet \
  < scripts/migrations/table_status_fix_v1.sql
```

### 5.4 审批流程初始化
```bash
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet \
  < "又见炊烟餐饮管理系统 2.0 全套开发交付文档集/05_权限审批流/各类审批流程初始化SQL.sql"
```

## 6. 常用操作

### 6.1 启停
```bash
docker compose start     # 启动
docker compose stop      # 停止（保留容器）
docker compose down      # 停止并删除容器（保留数据卷）
docker compose down -v   # 停止并删除容器+数据卷（⚠️ 全部数据丢失）
```

### 6.2 日志
```bash
docker compose logs -f                   # 全部
docker compose logs -f backend           # 仅后端
docker compose logs --tail=200 backend   # 最后 200 行
```

### 6.3 进入容器
```bash
docker exec -it youjian-backend sh
docker exec -it youjian-mysql mysql -uroot -p banquet
docker exec -it youjian-rabbitmq rabbitmqctl list_queues
```

### 6.4 重启单个服务
```bash
docker compose restart backend
docker compose restart rabbitmq
```

### 6.5 重新构建
```bash
# 修改后端代码后
docker compose build backend
docker compose up -d backend

# 修改前端代码后（如使用 Docker 部署前端）
docker compose build frontend
docker compose up -d frontend
```

## 7. 升级流程

### 7.1 后端代码升级
```bash
# 1. 拉取最新代码
git pull

# 2. 重新构建后端镜像
docker compose build backend

# 3. 滚动重启（停老容器，起新容器）
docker compose up -d backend

# 4. 执行增量 SQL（如有）
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet < scripts/migrations/xxx.sql

# 5. 验证
curl http://localhost:8080/actuator/health
```

### 7.2 数据库升级
**升级前必须备份**：
```bash
# 备份
docker exec youjian-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD banquet > backup_$(date +%Y%m%d).sql

# 执行迁移
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet < xxx_migration.sql

# 验证
docker exec youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet -e "SELECT ..."
```

回滚：
```bash
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet < backup_20260802.sql
```

## 8. 监控

### 8.1 健康检查
- MySQL：`mysqladmin ping`
- RabbitMQ：`rabbitmqctl status`
- 后端：`/actuator/health`

### 8.2 RabbitMQ 管理台
- URL：http://localhost:15672
- 账号：`.env` 中的 `RABBITMQ_USER` / `RABBITMQ_PASSWORD`
- 查看：
  - Queues → `youjian.notify.queue` 消息堆积
  - Exchanges → `youjian.notify.exchange` 流量
  - Connections / Channels

### 8.3 资源监控
```bash
docker stats
# CONTAINER / CPU / MEM / NET / IO
```

## 9. 备份与恢复

### 9.1 数据库备份
```bash
# 手动备份
docker exec youjian-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD --single-transaction banquet > backup.sql

# 定时备份（cron）
0 3 * * * docker exec youjian-mysql mysqldump -uroot -p$MYSQL_ROOT_PASSWORD --single-transaction banquet | gzip > /backup/banquet_$(date +\%Y\%m\%d).sql.gz
```

### 9.2 COS 上传备份
项目提供 `scripts/backup/cos_upload.py`：
```bash
python scripts/backup/cos_upload.py --file backup.sql
```
凭据自动从 `~/.cos.conf` 读取。

### 9.3 恢复
```bash
gunzip < backup_20260802.sql.gz | docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet
```

## 10. 故障排查

### 10.1 后端启动失败
```bash
docker compose logs backend | grep -i error
# 常见：数据库连接失败（host=mysql 不解析）→ 检查 docker 网络
# 常见：JWT_SECRET 未设置 → 检查 .env
```

### 10.2 MySQL 连接拒绝
```bash
docker exec youjian-mysql mysql -uroot -p -e "SELECT 1"
# 失败 → 检查密码 / 端口 / 容器健康
docker compose logs mysql
```

### 10.3 RabbitMQ 连接失败
```bash
docker exec youjian-rabbitmq rabbitmqctl status
# 检查 .env 中 RABBITMQ_USER/PASSWORD 与 RABBITMQ_DEFAULT_USER/PASS 一致
```

### 10.4 前端 502
```bash
docker compose logs frontend
# 检查 nginx.conf 中 proxy_pass 后端地址
docker compose restart backend
```

## 11. 安全加固

1. **不要将 .env 提交到 Git**（已 .gitignore）
2. **生产环境禁用 Swagger**：`springdoc.swagger-ui.enabled=false`
3. **限制端口暴露**：MySQL/RabbitMQ 不对公网开放，仅 `ports` 内部网络
4. **强密码**：所有密码 ≥ 16 位随机字符串
5. **定期备份**：数据库每日备份到 COS
6. **日志轮转**：`logging.file.name` + logback 轮转配置
7. **HTTPS**：Nginx 配置 SSL 证书

## 12. 上线 Checklist

详见同目录 `上线CheckList.md`（如未提供，请补充）。

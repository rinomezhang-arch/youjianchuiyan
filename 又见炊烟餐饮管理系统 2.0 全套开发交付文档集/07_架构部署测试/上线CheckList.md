# 上线 CheckList

> 适用：又见炊烟餐饮管理系统 2.0 生产部署前  
> 维护：地龙（DL-BOT）  
> 更新：2026-08-02

---

## 1. 代码与构建

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | 主分支代码已合并 | |
| ☐ | `mvn clean package -DskipTests` BUILD SUCCESS | |
| ☐ | `npm run build` 成功 | |
| ☐ | 单元测试通过 | |
| ☐ | 集成测试通过 | |
| ☐ | 无 `System.out.println` 残留 | |
| ☐ | 无硬编码密码 / Token | |
| ☐ | 无 TODO 标记（除已知的 NotifyConsumer WebSocket/短信 TODO） | |

## 2. 配置与环境变量

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | `.env` 已配置所有必填变量 | |
| ☐ | `MYSQL_PASSWORD` ≥ 16 位强密码 | |
| ☐ | `JWT_SECRET` ≥ 48 位随机字符串 | |
| ☐ | `AES_SECRET_KEY` ≥ 32 位 hex | |
| ☐ | `RABBITMQ_PASSWORD` 已修改（非默认 guest） | |
| ☐ | `SPRING_PROFILES_ACTIVE=prod` | |
| ☐ | `NOTIFY_ENABLED=true` | |
| ☐ | 生产 Swagger 关闭（`springdoc.swagger-ui.enabled=false`） | |

## 3. 数据库

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | `banquet_init.sql` 已执行 | |
| ☐ | `rbac_init.sql` 已执行 | |
| ☐ | `business_config_migration_v1.sql` 已执行 | |
| ☐ | `dict_extend_migration_v1.sql` 已执行 | |
| ☐ | `table_status_fix_v1.sql` 已执行（验证 table_status 无 'available'） | |
| ☐ | 各类审批流程初始化 SQL 已执行 | |
| ☐ | 数据库字符集 `utf8mb4` | |
| ☐ | 所有业务表有 `store_id` 字段 | |
| ☐ | 测试数据已清理 | |
| ☐ | 备份策略已配置（每日 mysqldump → COS） | |

## 4. 容器与服务

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | `docker compose build` 成功 | |
| ☐ | `docker compose up -d` 全部容器 Up | |
| ☐ | MySQL 健康检查通过 | |
| ☐ | RabbitMQ 健康检查通过 | |
| ☐ | 后端 `/actuator/health` UP | |
| ☐ | 前端 200 OK | |
| ☐ | 容器间网络通信正常（`youjian-backend` → `mysql` → `rabbitmq`） | |
| ☐ | 数据卷已挂载（mysql-data / app-logs / rabbitmq-data） | |

## 5. 鉴权与权限

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | 登录功能正常（用户名/密码中文不乱码） | |
| ☐ | JWT token 24h 后过期 | |
| ☐ | 店长无法跨店查询（403） | |
| ☐ | GM 可跨店查询 | |
| ☐ | RBAC 4 角色权限点正确 | |
| ☐ | 审批流 5 类模板已配置 | |

## 6. 核心业务流程

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | 创建预订 → 占用桌台 → 状态变 reserved | |
| ☐ | 取消预订 → 桌台释放 → 状态变 idle | |
| ☐ | iPad 点菜 → 后厨接单 → 出菜 → 状态变 served | |
| ☐ | 套餐保存 → 价格联动计算正确 | |
| ☐ | 客户 CRUD + 历史 | |
| ☐ | 采购入库 → 库存增加 + 应付生成 | |
| ☐ | 报销审批 → 状态流转 pending→approved→paid | |
| ☐ | 库存低于阈值 → 触发预警通知 | |

## 7. 异步通知

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | RabbitMQ 管理台可访问（http://server:15672） | |
| ☐ | `youjian.notify.queue` 队列已创建 | |
| ☐ | 创建预订 → sys_notification 落库 | |
| ☐ | MQ 消息可消费（无堆积） | |
| ☐ | MQ 发送失败时不影响主业务（同步落库兜底） | |

## 8. 安全

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | MySQL/RabbitMQ 端口不暴露公网 | |
| ☐ | 仅 80/443 对外 | |
| ☐ | HTTPS 证书已配置 | |
| ☐ | CORS 白名单限制（非 `*`） | |
| ☐ | 限流配置启用 | |
| ☐ | 敏感字段加密（身份证/银行卡 AES） | |
| ☐ | 审计日志正常记录 | |
| ☐ | 错误日志不泄漏堆栈到前端 | |

## 9. 性能

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | HikariCP 连接池大小合理（10~20） | |
| ☐ | JPA `show-sql=false`（生产） | |
| ☐ | 日志级别 INFO（生产） | |
| ☐ | 前端 gzip 压缩启用 | |
| ☐ | 静态资源 CDN 缓存 | |
| ☐ | 数据库索引覆盖常用查询 | |

## 10. 监控与运维

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | 日志路径 `/app/logs/` 持久化到卷 | |
| ☐ | 日志轮转配置（避免磁盘满） | |
| ☐ | 备份脚本定时执行（每日 3:00） | |
| ☐ | 备份文件上传 COS | |
| ☐ | 监控告警（CPU/MEM/磁盘） | |
| ☐ | 应急联系人 + 回滚方案文档 | |

## 11. 灾备

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | 数据库每日备份 | |
| ☐ | 备份保留 ≥ 30 天 | |
| ☐ | 恢复演练已执行（验证备份可用） | |
| ☐ | 配置文件备份（.env / application*.yml） | |
| ☐ | 镜像版本号已固定（非 `latest`） | |

## 12. 上线后验证

| 项 | 检查内容 | 是/否 |
|---|---------|------|
| ☐ | 4 个角色登录验证（GM/HQ_STAFF/STORE_MANAGER/WAITER） | |
| ☐ | 双门店（宁国 store_id=1 / 宣城 store_id=2）数据隔离验证 | |
| ☐ | 关键业务流程端到端跑通（预订→点菜→出品→结账） | |
| ☐ | 日志无 ERROR 级别（持续 1 小时观察） | |
| ☐ | MQ 队列无堆积 | |
| ☐ | 数据库慢查询日志无异常 | |

## 13. 回滚预案

### 13.1 触发条件
- 核心业务流程不可用（登录失败 / 预订无法创建）
- 数据损坏
- 安全漏洞被利用

### 13.2 回滚步骤
```bash
# 1. 停止应用
docker compose stop backend

# 2. 代码回滚到上一版本
git checkout <previous-tag>

# 3. 重新构建镜像
docker compose build backend

# 4. 数据库回滚（如需要）
docker exec -i youjian-mysql mysql -uroot -p$MYSQL_ROOT_PASSWORD banquet < backup_YYYYMMDD.sql

# 5. 启动
docker compose up -d backend

# 6. 验证
curl http://localhost:8080/actuator/health
```

### 13.3 联系人
- 应急负责人：（待补充）
- 数据库 DBA：（待补充）
- 运维：（待补充）

## 14. 签字确认

| 角色 | 姓名 | 签字日期 |
|------|------|---------|
| 开发负责人 | | |
| 测试负责人 | | |
| 运维负责人 | | |
| 产品负责人 | | |
| 总经理（甲方） | | |

---

**重要提醒**：
1. 上线前必须完成所有 ☐ 项
2. 任一关键项未通过不得上线
3. 上线后 1 小时内密切监控
4. 保留回滚能力至少 24 小时

# dev / test / prod 三套 yml 配置说明

> 适用：`banquet_project/src/main/resources/application*.yml`  
> 维护：地龙（DL-BOT）  
> 更新：2026-08-02

---

## 1. 配置文件结构

```
banquet_project/src/main/resources/
├── application.yml              # 主配置（默认环境）
└── application-prod.yml         # 生产环境（覆盖默认）
```

激活方式：
- 默认：`spring.profiles.active` 未设置，使用 `application.yml`
- 生产：`SPRING_PROFILES_ACTIVE=prod` 环境变量

## 2. 环境对比

| 项 | 默认（本地） | prod（Docker） |
|---|------------|---------------|
| 数据库 host | `localhost` | `mysql`（容器名） |
| 数据库端口 | 3306 | 3306 |
| 数据库名 | `banquet` | `banquet` |
| 数据库用户 | `root` | `${MYSQL_USER}` |
| 数据库密码 | `123456` | `${MYSQL_PASSWORD}` |
| 服务端口 | 8080 | 8080 |
| 前端端口 | 5173 | 80（Nginx） |
| RabbitMQ host | `localhost` | `rabbitmq`（容器名） |
| RabbitMQ 启用 | `NOTIFY_ENABLED=false` | `NOTIFY_ENABLED=true` |
| 日志级别 | DEBUG | INFO |
| Swagger | enabled=true | enabled=true |

## 3. 默认环境 application.yml

```yaml
server:
  port: 8080
  servlet:
    encoding:
      charset: UTF-8
      force: true

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/banquet?useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true
    username: root
    password: 123456
    driver-class-name: com.mysql.cj.jdbc.Driver
    hikari:
      maximum-pool-size: 10
      connection-timeout: 30000

  jpa:
    hibernate:
      ddl-auto: none  # 不自动建表，由 SQL 脚本管理
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
        format_sql: true

  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}
    listener:
      simple:
        concurrency: 1
        max-concurrency: 4
        prefetch: 10
        retry:
          enabled: true
          initial-interval: 1000ms
          max-attempts: 3
          multiplier: 2.0
        default-requeue-rejected: false

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true

app:
  notify:
    enabled: ${NOTIFY_ENABLED:false}  # 默认禁用，无 MQ 也能启动
    queue: youjian.notify.queue
    exchange: youjian.notify.exchange
    routing-key: notify.event

logging:
  level:
    root: INFO
    com.youjian.banquet: DEBUG
  file:
    name: logs/app.log
```

## 4. prod 环境 application-prod.yml

```yaml
spring:
  datasource:
    url: jdbc:mysql://mysql:3306/${MYSQL_DATABASE}?useSSL=false&serverTimezone=Asia/Shanghai&allowMultiQueries=true
    username: ${MYSQL_USER}
    password: ${MYSQL_PASSWORD}

  rabbitmq:
    host: ${RABBITMQ_HOST:rabbitmq}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USER:guest}
    password: ${RABBITMQ_PASSWORD:guest}
    virtual-host: ${RABBITMQ_VHOST:/}
    listener:
      simple:
        concurrency: 1
        max-concurrency: 8
        prefetch: 20
        retry:
          enabled: true
          initial-interval: 1000ms
          max-attempts: 3
          multiplier: 2.0
        default-requeue-rejected: false

app:
  notify:
    enabled: ${NOTIFY_ENABLED:true}  # 生产启用 MQ
    queue: youjian.notify.queue
    exchange: youjian.notify.exchange
    routing-key: notify.event

aes:
  secret-key: ${AES_SECRET_KEY}

jwt:
  secret: ${JWT_SECRET}

logging:
  level:
    root: INFO
    com.youjian.banquet: INFO
```

## 5. 环境变量清单

### 5.1 必填（生产）
| 变量 | 说明 | 示例 |
|------|------|------|
| `MYSQL_DATABASE` | 数据库名 | `banquet` |
| `MYSQL_USER` | 数据库用户 | `banquet_user` |
| `MYSQL_PASSWORD` | 数据库密码 | `****` |
| `JWT_SECRET` | JWT 签名密钥 | `****` |
| `AES_SECRET_KEY` | AES 加密密钥 | `****` |

### 5.2 可选（生产）
| 变量 | 默认 | 说明 |
|------|------|------|
| `RABBITMQ_HOST` | `rabbitmq` | MQ 主机 |
| `RABBITMQ_PORT` | `5672` | MQ 端口 |
| `RABBITMQ_USER` | `guest` | MQ 用户 |
| `RABBITMQ_PASSWORD` | `guest` | MQ 密码 |
| `RABBITMQ_VHOST` | `/` | MQ vhost |
| `NOTIFY_ENABLED` | `true` | 通知模块开关 |

## 6. 切换 profile

### 6.1 命令行
```bash
java -jar banquet.jar --spring.profiles.active=prod
```

### 6.2 环境变量（推荐 Docker）
```bash
export SPRING_PROFILES_ACTIVE=prod
java -jar banquet.jar
```

### 6.3 Docker Compose
```yaml
environment:
  SPRING_PROFILES_ACTIVE: prod
```

## 7. 开发环境启动

### 7.1 本地后端
```bash
cd banquet_project
mvn spring-boot:run
# 默认 profile，连接 localhost:3306
```

### 7.2 本地前端
```bash
cd frontend_v3
npm install
npm run dev
# 端口 5173，绑定 0.0.0.0
```

### 7.3 数据库
```bash
docker run -d --name mysql -p 3306:3306 \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=banquet \
  mysql:8.0
# 导入 youjian-docker/mysql/init/*.sql
```

## 8. 生产环境部署

详见同目录 `Docker容器部署脚本与操作指南.md`。

## 9. 日志

| 文件 | 路径 | 内容 |
|------|------|------|
| `app.log` | `logs/app.log` | 主日志 |
| `auth.log` | `logs/auth.log` | 登录认证 |
| `error.log` | `logs/error.log` | 错误 |

Docker 环境映射到 `app-logs` volume。

## 10. 验证

启动后访问：
- 后端健康：http://localhost:8080/actuator/health
- Swagger：http://localhost:8080/swagger-ui.html
- 前端：http://localhost:5173
- RabbitMQ 管理：http://localhost:15672（guest/guest）

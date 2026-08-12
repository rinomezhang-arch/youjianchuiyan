# 构建阶段：Maven打包项目
FROM maven:3.9-eclipse-temurin-17 AS builder

WORKDIR /app

# 分层复制，缓存Maven依赖，加快重复构建
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src ./src

# 打包跳过单元测试
RUN mvn clean package -DskipTests

# 运行阶段：轻量JRE镜像，减小镜像体积
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# 从构建阶段复制打包好的jar包
COPY --from=builder /app/target/*.jar app.jar

# JVM内存优化参数 + 激活 prod profile（所有敏感配置通过环境变量注入）
ENTRYPOINT ["java", "-Xms512m", "-Xmx1024m", "-Dspring.profiles.active=prod", "-jar", "app.jar"]

EXPOSE 8080

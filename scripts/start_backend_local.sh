#!/bin/bash
# 本地起后端：prod 配置文件 + 本地 MySQL，用于开发机上的真实预览/联调。
# jwt.secret / aes.secret-key 只在 application-prod.yml 里定义（无默认值，故意不硬编码），
# 本地跑 prod профиль 时必须通过环境变量注入——密钥存在 scripts/.env.local（已 gitignore，不进仓库）。
set -e
cd "$(dirname "$0")/.."

if [ ! -f scripts/.env.local ]; then
  echo "scripts/.env.local 不存在，先生成一份本地专用密钥..."
  {
    echo "JWT_SECRET=local-dev-jwt-secret-$(openssl rand -hex 24)"
    echo "AES_SECRET_KEY=$(openssl rand -hex 16)"
  } > scripts/.env.local
fi
set -a
source scripts/.env.local
set +a

export SPRING_DATASOURCE_URL="jdbc:mysql://127.0.0.1:3306/banquet?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
export SPRING_DATASOURCE_USERNAME=rino
export SPRING_DATASOURCE_PASSWORD=Wo002323

cd banquet_project
mvn -q -o spring-boot:run -Dspring-boot.run.profiles=prod

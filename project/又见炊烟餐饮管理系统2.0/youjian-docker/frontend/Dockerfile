# 构建阶段：Node打包Vue静态资源
FROM node:20-alpine AS builder

WORKDIR /app

# 缓存npm依赖（构建需要devDependencies中的vite）
COPY package*.json ./
RUN npm ci

COPY . .

# 打包生产环境静态文件
RUN npm run build

# 运行阶段：Nginx托管静态资源
FROM nginx:alpine

WORKDIR /usr/share/nginx/html

# 复制打包好的dist静态文件
COPY --from=builder /app/dist .

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]

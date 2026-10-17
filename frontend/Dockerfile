# ========== 阶段1：Node 构建前端静态资源 ==========
FROM node:20-alpine AS builder
WORKDIR /build

# 先复制 package 文件，利用缓存层加速 npm install
COPY frontend/package.json frontend/package-lock.json* ./

# 安装依赖
RUN npm config set registry https://registry.npmmirror.com && npm install

# 复制源码
COPY frontend/ ./

# 只构建不跑类型检查（类型检查在开发阶段完成，生产构建用 build-only 更稳定）
RUN npm run build-only

# ========== 阶段2：Nginx 运行时（托管静态资源 + 反向代理 + HTTPS） ==========
FROM nginx:stable-alpine
WORKDIR /usr/share/nginx/html

# 复制构建产物
COPY --from=builder /build/dist /usr/share/nginx/html

# 复制自定义 nginx 配置（含 /api 反代与 HTTPS）
COPY frontend/nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80 443

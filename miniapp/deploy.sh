#!/usr/bin/env bash
# =========================================================================
# 又见炊烟 小程序 + H5 → 腾讯云"天龙"服务器 一键部署脚本
# 用法：
#   1) 填下面的服务器信息 + 任选一种【登录方式】（3选1）
#   2) bash deploy.sh           # 部署 H5 + 源码备份到服务器
#   3) bash deploy.sh dry-run   # 只打印命令不真的执行（验证）
# =========================================================================

set -e
cd "$(dirname "$0")"
DRY=0 ; [ "$1" = "dry-run" ] && DRY=1
run() {
  if [ $DRY -eq 1 ]; then echo "  [dry] $*"; else echo "  ⇨ $*"; eval "$*"; fi
}

# ===================== ① 服务器信息 =====================
# 腾讯云"天龙"：私网 100.70.215.11 ，公网 1.13.173.213
DEPLOY_HOST=""                        # 填公网 IP 或域名，例：1.13.173.213 或 youjianchuiyan.com
DEPLOY_PORT="22"                      # SSH 端口（如果改过就改，如 2222 / 60022）
DEPLOY_USER="root"                    # SSH 用户：一般 root 或 ubuntu 或 centos

# H5 部署路径（你 Nginx 的 /m 目录）
DEPLOY_H5_DIR="/usr/local/openresty/nginx/html/m"
# 源码备份目录
DEPLOY_SRC_DIR="/opt/youjianchuiyan/miniapp-src"

# ===================== ② 登录方式（3 选 1，填一个就行） =====================
# —— 方式 A：本地有私钥文件（推荐）。例：/c/Users/你/.ssh/id_rsa 或 /Users/你/.ssh/id_rsa
SSH_KEY_FILE=""

# —— 方式 B：私钥文件不存在，但你知道私钥内容（复制下面 PEM 一行）
#   从本地 C:\Users\你\.ssh\id_rsa 打开，全部复制，到 https://www.base64encode.org/ 转 base64，粘这里
SSH_KEY_BASE64=""

# —— 方式 C：密码登录（不推荐，但最快）
SSH_PASSWORD=""

# ===================== 下面不用改 =====================

banner() {
  echo ""
  echo "============================================="
  echo "  又见炊烟 · Uni-App → 天龙服务器 一键部署"
  echo "  主机: $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_PORT"
  echo "  H5  : $DEPLOY_H5_DIR"
  echo "  源码: $DEPLOY_SRC_DIR"
  echo "============================================="
  echo ""
}

# --- 准备连接参数 ---
prepare_ssh() {
  SSH_OPTS="-o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null -o ConnectTimeout=8 -p $DEPLOY_PORT"
  export SSHPASS="$SSH_PASSWORD"

  if [ -n "$SSH_KEY_FILE" ] && [ -f "$SSH_KEY_FILE" ]; then
    echo "🔑 使用方式 A：私钥文件 $SSH_KEY_FILE"
    SSH_CMD="ssh $SSH_OPTS -i \"$SSH_KEY_FILE\" $DEPLOY_USER@$DEPLOY_HOST"
    SCP_CMD="scp $SSH_OPTS -i \"$SSH_KEY_FILE\" -r"
    RSYNC_CMD="rsync -avz -e \"ssh $SSH_OPTS -i $SSH_KEY_FILE\""
  elif [ -n "$SSH_KEY_BASE64" ]; then
    echo "🔐 使用方式 B：base64 私钥"
    TMP_KEY="/tmp/youjianchuiyan_deploy_key_$$"
    echo "$SSH_KEY_BASE64" | base64 -d > "$TMP_KEY"
    chmod 600 "$TMP_KEY"
    SSH_CMD="ssh $SSH_OPTS -i \"$TMP_KEY\" $DEPLOY_USER@$DEPLOY_HOST"
    SCP_CMD="scp $SSH_OPTS -i \"$TMP_KEY\" -r"
    RSYNC_CMD="rsync -avz -e \"ssh $SSH_OPTS -i $TMP_KEY\""
    trap "rm -f $TMP_KEY" EXIT
  elif [ -n "$SSH_PASSWORD" ]; then
    echo "🔒 使用方式 C：密码登录"
    which sshpass >/dev/null 2>&1 || (echo "需要 sshpass: apt-get -y install sshpass"; exit 1)
    SSH_CMD="sshpass -e ssh $SSH_OPTS $DEPLOY_USER@$DEPLOY_HOST"
    SCP_CMD="sshpass -e scp $SSH_OPTS -r"
    RSYNC_CMD="sshpass -e rsync -avz -e \"ssh $SSH_OPTS\""
  else
    echo "❌ 3 种登录方式一个都没填。请编辑 deploy.sh 顶部的 ② 登录方式（3选1）"
    exit 1
  fi
}

check() {
  [ -z "$DEPLOY_HOST" ] && { echo "❌ 请编辑 deploy.sh 顶部：① 填 DEPLOY_HOST"; exit 1; }
  banner
  prepare_ssh
  echo ""
  echo "🔗 测试能否连 SSH..."
  if [ $DRY -eq 1 ]; then
    echo "  [dry] $SSH_CMD 'echo SSH_OK'"
    return
  fi
  eval "$SSH_CMD 'echo SSH_OK'" 2>&1 || {
    echo ""
    echo "❌ SSH 连接失败。可能原因："
    echo "   1) 腾讯云安全组/防火墙没放行沙盒出口 IP 124.174.1.45"
    echo "   2) 端口不对（现在填的 $DEPLOY_PORT，是不是 2222/60022/8022？）"
    echo "   3) 密码/私钥不匹配 / 没做过密钥免密"
    echo "   4) 可以先在你本地 001 电脑跑同样命令验证，能通的话把登录方式原样搬过来"
    exit 1
  }
}

build_h5() {
  echo ""
  echo "🛠  第一步：编译 H5 产物"
  if [ -f "pnpm-lock.yaml" ] || grep -q "pnpm" package.json 2>/dev/null; then
    run "which pnpm >/dev/null 2>&1 || npm i -g pnpm"
    run "pnpm i --no-frozen-lockfile"
    run "pnpm build:h5"
  else
    run "npm i --legacy-peer-deps"
    run "npm run build:h5"
  fi
  [ -d unpackage/dist/build/h5 ] || { echo "❌ 编译失败，unpackage/dist/build/h5 不存在"; exit 1; }
  echo "   ✅ H5 产物已生成：$(du -sh unpackage/dist/build/h5 | cut -f1)"
}

deploy_h5() {
  echo ""
  echo "📤 第二步：部署 H5 → $DEPLOY_H5_DIR"
  run "eval \"$SSH_CMD \\\"mkdir -p $DEPLOY_H5_DIR && ls -la $DEPLOY_H5_DIR/index.html 2>/dev/null | head -1\\\"\""
  # 先备份旧版
  run "eval \"$SSH_CMD \\\"cp -a $DEPLOY_H5_DIR ${DEPLOY_H5_DIR}_backup_\\\\\\\\$(date +%Y%m%d_%H%M%S) 2>/dev/null || true\\\"\""
  # 清空旧 + 传新
  run "eval \"$SSH_CMD \\\"rm -rf $DEPLOY_H5_DIR/*\\\"\""
  run "eval \"$RSYNC_CMD unpackage/dist/build/h5/ $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_H5_DIR/\""
  echo "   ✅ 已部署，正在访问测试..."
  run "curl -s --max-time 5 -o /dev/null -w '   访问 https://youjianchuiyan.com/m/ 状态码：%{http_code}\\n' https://youjianchuiyan.com/m/"
}

backup_src() {
  echo ""
  echo "📦 第三步：源码备份 → $DEPLOY_SRC_DIR"
  SRC_TAR="/tmp/youjianchuiyan-miniapp-src-$(date +%Y%m%d).tar.gz"
  run "tar czf $SRC_TAR --exclude=.git --exclude=node_modules --exclude=unpackage --exclude=youjianchuiyan-miniapp.zip ."
  run "eval \"$SSH_CMD \\\"mkdir -p $DEPLOY_SRC_DIR\\\"\""
  run "eval \"$SCP_CMD $SRC_TAR $DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_SRC_DIR/\""
  run "eval \"$SSH_CMD \\\"ls -lh $DEPLOY_SRC_DIR\\\"\""
  [ $DRY -eq 0 ] && rm -f "$SRC_TAR"
  echo "   ✅ 源码备份完成"
}

nginx_reload() {
  echo ""
  echo "🔄 第四步：重载 Nginx / 清缓存"
  run "eval \"$SSH_CMD \\\"which openresty 2>/dev/null && openresty -t && openresty -s reload || (nginx -t 2>/dev/null && nginx -s reload 2>/dev/null || echo 'Nginx 未重载，请手动')\\\"\""
}

summary() {
  echo ""
  echo "============================================="
  echo "🎉 部署完成"
  echo " H5 线上： https://youjianchuiyan.com/m/"
  echo " 源码备份：$DEPLOY_USER@$DEPLOY_HOST:$DEPLOY_SRC_DIR/"
  echo " 旧 H5 备份位置：${DEPLOY_H5_DIR}_backup_*（出问题 cp 回去即可回滚）"
  echo "============================================="
}

check
build_h5
deploy_h5
backup_src
nginx_reload
summary

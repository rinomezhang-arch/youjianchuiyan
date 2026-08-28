#!/usr/bin/env bash
# =========================================================================
# 又见炊烟小程序 H5 → 腾讯云天龙/Nginx 服务器 一键自部署脚本
# 用法（在你天龙服务器 SSH 里执行，就这一条）：
#   bash <(curl -sSL https://raw.githubusercontent.com/rinomezhang-arch/youjianchuiyan-miniapp/miniapp-uniapp/deploy-h5-on-server.sh)
#
# 作用：
#   1) 自动检测并安装 Node 20 + pnpm（如缺）
#   2) git clone 最新 miniapp 源码到 /opt/youjianchuiyan/miniapp/
#   3) pnpm i + pnpm build:h5 编译
#   4) 备份你现在的旧版 /m → /usr/local/openresty/nginx/html/m_backup_时间戳/
#   5) rsync 新 H5 覆盖 /m → 重载 openresty / nginx
#   6) curl 自检 https://youjianchuiyan.com/m/ HTTP 状态
#   7) 失败 1 键回滚：./rollback-h5.sh（脚本自动写到当前目录）
# =========================================================================
set -euo pipefail
cd /root

RED="\e[31m"; GRN="\e[32m"; YLW="\e[33m"; BLU="\e[34m"; NRM="\e[0m"
log()  { echo -e "${BLU}[部署]${NRM} $*"; }
ok()   { echo -e "${GRN}[OK]${NRM}   $*"; }
warn() { echo -e "${YLW}[!]${NRM}    $*"; }
die()  { echo -e "${RED}[ERR]${NRM} $*"; exit 1; }

DEPLOY_H5_DIR="/usr/local/openresty/nginx/html/m"
SRC_DIR="/opt/youjianchuiyan/miniapp"
BRANCH="miniapp-uniapp"
# 如果以后要改仓库名，改这里就行：
REPO_URL="https://github.com/rinomezhang-arch/youjianchuiyan-miniapp.git"
ROLLBACK_SH="/root/rollback-h5.sh"

# ---------- 0. 系统检查 ----------
log "========== 又见炊烟 H5 自部署启动 =========="
[ "$(id -u)" -ne 0 ] && warn "当前不是 root，如需装 Node/pnpm 会失败；建议 sudo -i 后再跑"

# ---------- 1. Node + pnpm ----------
log "① 检查 Node/pnpm"
need_install=0
if ! command -v node >/dev/null 2>&1; then need_install=1; warn "Node 未安装"; fi
if ! command -v pnpm >/dev/null 2>&1; then need_install=1; warn "pnpm 未安装"; fi
NODE_V=$(node -v 2>/dev/null || echo v0)
NODE_MAJOR=$(echo "$NODE_V" | sed -E 's/v([0-9]+).*/\1/')
if [ "$NODE_MAJOR" -lt 18 ] 2>/dev/null; then need_install=1; warn "Node 版本 $NODE_V < 18，需要升级"; fi
if [ "$need_install" -eq 1 ]; then
  log "自动安装 Node 20 + pnpm（用 nodesource 官方源，约 1 分钟）..."
  if command -v apt-get >/dev/null 2>&1; then
    apt-get update -qq
    apt-get install -y -qq curl ca-certificates gnupg lsb-release rsync git
    curl -fsSL https://deb.nodesource.com/setup_20.x | bash -
    apt-get install -y -qq nodejs
  elif command -v yum >/dev/null 2>&1; then
    curl -fsSL https://rpm.nodesource.com/setup_20.x | bash -
    yum install -y -q nodejs git rsync
  else
    die "不认识的包管理器（不是 apt/yum），请手动装 Node ≥ 18 后再跑"
  fi
  npm i -g pnpm --no-audit --silent || corepack enable && corepack prepare pnpm@latest --activate
fi
ok "Node $(node -v)  /  pnpm $(pnpm -v) 就绪"

# ---------- 2. 拉源码 ----------
log "② 拉仓库 $REPO_URL 分支 $BRANCH"
if [ -d "$SRC_DIR/.git" ]; then
  cd "$SRC_DIR" && git fetch --depth=1 origin "$BRANCH" && git reset --hard "origin/$BRANCH"
else
  mkdir -p "$(dirname "$SRC_DIR")"
  [ -d "$SRC_DIR" ] && rm -rf "$SRC_DIR"
  git clone --depth=1 -b "$BRANCH" "$REPO_URL" "$SRC_DIR"
fi
cd "$SRC_DIR"
ok "源码就绪：$(git rev-parse --short HEAD)"

# ---------- 3. 编译 ----------
log "③ 装依赖 + 编译 H5（首次 2-4 分钟，后续增量快）"
pnpm install --no-frozen-lockfile --prefer-offline 2>&1 | tail -3
pnpm build:h5 2>&1 | tail -5
H5="$SRC_DIR/unpackage/dist/build/h5"
[ -f "$H5/index.html" ] || die "编译失败：$H5/index.html 不存在。上面 pnpm build 日志里有报错"
ok "H5 编译完成：$(du -sh $H5 | cut -f1)"

# ---------- 4. 备份旧版 ----------
log "④ 备份当前 /m"
[ -d "$DEPLOY_H5_DIR" ] || die "$DEPLOY_H5_DIR 不存在！请检查 Nginx 路径是否正确"
BACKUP_DIR="${DEPLOY_H5_DIR}_backup_$(date +%Y%m%d_%H%M%S)"
cp -a "$DEPLOY_H5_DIR" "$BACKUP_DIR"
cat > "$ROLLBACK_SH" <<EOF
#!/usr/bin/env bash
# 一键回滚：如果刚部署坏了，执行 bash $ROLLBACK_SH
set -e
echo "正在回滚到 $BACKUP_DIR"
rm -rf "$DEPLOY_H5_DIR"/*
rsync -a "$BACKUP_DIR/" "$DEPLOY_H5_DIR/"
(openresty -t && openresty -s reload) 2>/dev/null || (nginx -t && nginx -s reload) 2>/dev/null || true
echo "✅ 已回滚，访问 https://youjianchuiyan.com/m/ 验证"
EOF
chmod +x "$ROLLBACK_SH"
ok "旧版已备份 → $BACKUP_DIR  （回滚命令：bash $ROLLBACK_SH）"

# ---------- 5. 部署 ----------
log "⑤ 部署新 H5 → $DEPLOY_H5_DIR 并重载 Nginx"
rm -rf "$DEPLOY_H5_DIR"/*
rsync -a "$H5/" "$DEPLOY_H5_DIR/"
NICE=0
if command -v openresty >/dev/null 2>&1; then
  openresty -t 2>&1 | tail -1
  openresty -s reload && NICE=1
fi
if [ "$NICE" -eq 0 ] && command -v nginx >/dev/null 2>&1; then
  nginx -t 2>&1 | tail -1
  nginx -s reload && NICE=1
fi
[ "$NICE" -eq 1 ] || warn "Nginx/OpenResty 未重载，请手动执行"
ok "部署完成"

# ---------- 6. 自检 ----------
log "⑥ 访问自检：https://youjianchuiyan.com/m/"
sleep 1
HTTP=$(curl -k -s --max-time 8 -o /dev/null -w '%{http_code}' "https://youjianchuiyan.com/m/" || echo 000)
if [ "$HTTP" = "200" ]; then
  ok "✅ 全部完成！HTTP 200"
else
  warn "自检返回 HTTP $HTTP（可能 HTTPS SNI/缓存问题），浏览器手动访问 https://youjianchuiyan.com/m/ 验证"
fi

echo ""
echo -e "${GRN}=====================================${NRM}"
echo -e "${GRN} 部署日志摘要${NRM}"
echo "  · 源码位置：$SRC_DIR"
echo "  · 版本提交：$(cd $SRC_DIR && git rev-parse --short HEAD)"
echo "  · H5 线上：https://youjianchuiyan.com/m/"
echo "  · 回滚方案：bash $ROLLBACK_SH   （出问题 1 秒还原）"
echo -e "${GRN}=====================================${NRM}"

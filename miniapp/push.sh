#!/usr/bin/env bash
# ------------------------------------------------------------------
# 又见炊烟 小程序 → GitHub 推送脚本
# 运行前：把下面前 4 行变量改成你自己的，然后 `bash push.sh`
# ------------------------------------------------------------------

# ======== 必须填 1：GitHub 个人访问令牌（PAT）=======
# 生成位置：https://github.com/settings/tokens → Generate new token(classic)
# 权限至少勾：repo   (如果是推到组织仓库，勾 workflow 和 admin:org 不用)
export GITHUB_TOKEN=""

# ======== 必须填 2：目标仓库地址（3 选 1，去掉注释的 #） ========
# 选项 A：独立仓库（推荐，干净）
# REPO_URL="https://${GITHUB_TOKEN}@github.com/rinomezhang-arch/youjianchuiyan-miniapp.git"
# 选项 B：和前端后台一起（合并到你现有主仓库的 miniapp 子目录）
# REPO_URL="https://${GITHUB_TOKEN}@github.com/rinomezhang-arch/youjianchuiyan.git"
REPO_URL=""

# ======== 可选：分支名（默认已经是 miniapp-uniapp） ========
BRANCH="miniapp-uniapp"

# ======== 可选：如果选 B（合并到现有子目录），填这里的子路径 ========
SUB_DIR="miniapp"     # 会把本项目放在 rinomezhang-arch/youjianchuiyan/miniapp/ 下面

# ====================================================================

set -e
cd "$(dirname "$0")"

if [ -z "$GITHUB_TOKEN" ]; then
  echo "❌ 请先编辑 push.sh 第 10 行，填入 GITHUB_TOKEN (https://github.com/settings/tokens 生成)"
  exit 1
fi
if [ -z "$REPO_URL" ]; then
  echo "❌ 请先编辑 push.sh 第 15~18 行，选一个 REPO_URL 并去掉注释"
  exit 1
fi

echo "=================== 又见炊烟小程序推送脚本 ==================="
echo "本地分支：$BRANCH"
echo "目标仓库：$(echo "$REPO_URL" | sed 's|://.*@|://***@|g')"
echo ""

# 如果 repo_url 是主仓库（youjianchuiyan.git 不是 miniapp 结尾），就走 sub-tree 合并
if [[ "$REPO_URL" != *"miniapp.git" ]]; then
  echo "🔀 检测到你要推到主仓库一起 → 使用 git subtree 放到 '$SUB_DIR/' 目录"
  echo ""
  # 1) 克隆目标主仓库到临时目录
  TMP=/tmp/ycy_push_main
  rm -rf "$TMP"
  echo "⇣ 克隆主仓库..."
  git clone --depth=1 --branch "$BRANCH" "$REPO_URL" "$TMP" 2>/dev/null \
    || git clone --depth=1 "$REPO_URL" "$TMP"
  # 2) 拷贝本项目所有文件到 miniapp/ 下，排除 .git
  echo "⇶ 拷贝小程序代码到 '$SUB_DIR/' 子目录..."
  mkdir -p "$TMP/$SUB_DIR"
  # 用 tar 保证 .gitignore 选中的文件/空目录/符号链接完整传递
  tar cf - --exclude=.git . | (cd "$TMP/$SUB_DIR" && tar xf -)
  # 3) commit + push
  cd "$TMP"
  git add -A
  CHANGED=$(git status -s | wc -l)
  if [ "$CHANGED" -eq 0 ]; then
    echo "✅ 无变更"
  else
    git -c commit.gpgsign=false commit -m "feat(miniapp): 又见炊烟 Uni-App 小程序 Vue3 24页 首提交

分支：$BRANCH
时间：$(date '+%Y-%m-%d %H:%M:%S')
位置：$SUB_DIR/
包含：24 pages + 14 api 模块 + 微信原生能力（登录/支付/二维码/订阅消息）"
    echo "⇡ 推送到 $BRANCH ..."
    git push origin "HEAD:$BRANCH"
  fi
  echo ""
  echo "🎉 推送完成 ✅ 访问：$(echo "$REPO_URL" | sed 's|https://.*@|https://github.com/|g' | sed 's|\.git$||g')/tree/$BRANCH/$SUB_DIR"
  echo ""
  rm -rf "$TMP"
else
  echo "🎯 独立仓库推送模式"
  echo ""
  git remote remove origin 2>/dev/null || true
  git remote add origin "$REPO_URL"
  # 目标仓库可能已经有别的分支，强制推送当前分支（只推 miniapp-uniapp，不覆盖默认分支）
  echo "⇡ 推送分支 $BRANCH ..."
  git push -u origin "$BRANCH"
  echo ""
  echo "🎉 推送完成 ✅ 访问：$(echo "$REPO_URL" | sed 's|https://.*@|https://github.com/|g' | sed 's|\.git$||g')/tree/$BRANCH"
fi

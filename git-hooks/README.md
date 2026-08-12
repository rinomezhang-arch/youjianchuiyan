# Git Hooks 模板

此目录包含三个强制 Git 钩子模板，供天龙/地龙/Trae 使用。

## 安装方法

将文件复制到仓库的 `.git/hooks/` 目录并赋予执行权限：

```bash
cp git-hooks/* .git/hooks/ && chmod +x .git/hooks/pre-commit .git/hooks/prepare-commit-msg .git/hooks/pre-push
```

## 三个钩子

| 钩子 | 作用 |
|------|------|
| `pre-commit` | 提交前检查：禁止批量删除、敏感信息、越界修改 |
| `prepare-commit-msg` | 提交信息检查：强制【工具名】前缀格式 |
| `pre-push` | 推送前检查：禁止AI直接push main分支 |

## 工具身份配置

```bash
# 天龙
git config user.name "TL-BOT"
git config user.email "tl@project.local"

# 地龙
git config user.name "DL-BOT"
git config user.email "dl@project.local"

# Trae
git config user.name "TRAE-BOT"
git config user.email "trae@project.local"
```

详见仓库根目录 `CONTRIBUTING.md`

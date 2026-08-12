# 又见炊烟餐饮管理系统 2.0 - Git 分支管理规范

> 版本：V2.0
> 创建日期：2026-08-02
> 维护：Trae（TRAE-BOT, trae@project.local）
> 适用范围：又见炊烟餐饮管理系统 2.0 全部代码仓库（banquet_project 后端、banquet-web 前端、数据库脚本、文档集）

---

## 文档目的

本规范用于约束"又见炊烟餐饮管理系统 2.0"项目所有协作角色（人工开发者 + 3 个 AI 协作机器人：天龙 TL-BOT、地龙 DL-BOT、Trae TRAE-BOT）的 Git 分支使用行为，确保多角色并行开发下的代码可追溯、可回滚、可审计，避免分支污染、Commit 历史混乱、未审核代码进入主干等问题。所有提交者（含 AI）必须严格遵守本规范，CI 流水线与 pre-commit 钩子将据此自动拦截不合规提交。

---

## 一、分支模型

项目采用改进型 Git Flow 分支模型，主干 `main` 为受保护分支，任何角色不得直接 push。所有变更必须经 Pull Request（PR）合并。分支拓扑如下：

```
main (保护分支，可发布)
  ├── develop (集成分支，每日自动构建)
  │     ├── feat-tl-order-refund        (AI 天龙开发分支)
  │     ├── feat-dl-db-migration        (AI 地龙开发分支)
  │     ├── feat-trae-kds-dashboard     (AI Trae 开发分支)
  │     ├── feat-member-points          (人工开发分支)
  │     └── fix-payment-replay          (缺陷修复分支)
  ├── release-2.0.0 (预发布分支)
  └── hotfix-2.0.1 (紧急修复分支，从 main 拉出)
```

### 1.1 分支类型定义

| 分支类型 | 命名格式 | 来源 | 合并目标 | 生命周期 | 权限 |
|---------|---------|------|---------|---------|------|
| main | main | - | - | 永久 | 仅人工 PR 合并，禁止 push |
| develop | develop | main | main（经 release） | 永久 | 维护者可合并 feat/fix |
| feat-* | feat-{描述} | develop | develop | 合并后删除 | 全员可创建 |
| fix-* | fix-{描述} | develop | develop | 合并后删除 | 全员可创建 |
| release-* | release-{版本号} | develop | main + develop | 发布后删除 | 仅维护者操作 |
| hotfix-* | hotfix-{版本号} | main | main + develop | 修复后删除 | 仅维护者操作 |
| feat-tl-* | feat-tl-{描述} | develop | develop | 合并后删除 | 仅 TL-BOT |
| feat-dl-* | feat-dl-{描述} | develop | develop | 合并后删除 | 仅 DL-BOT |
| feat-trae-* | feat-trae-{描述} | develop | develop | 合并后删除 | 仅 TRAE-BOT |

### 1.2 分支命名细则

- 描述部分使用小写英文 + 连字符，禁止下划线、空格、中文。
- 描述应语义化，体现功能模块，长度不超过 40 字符。
- 必须关联一个 Issue 或任务编号（如 `feat-tl-order-refund-101`）。
- 示例：`feat-trae-kds-dashboard`、`fix-payment-replay-205`、`release-2.0.0`、`hotfix-2.0.1`。

---

## 二、AI 专属分支前缀规则

### 2.1 专属前缀强制要求

项目部署 3 个 AI 协作机器人，每个 AI 只能在自己的专属前缀分支上开发，禁止跨前缀创建分支，禁止直接操作 `main` / `develop` / `release-*` / `hotfix-*`。

| AI 角色 | 专属前缀 | 职责范围 | user.name | user.email |
|--------|---------|---------|-----------|-----------|
| 天龙 | feat-tl-* | 文档、需求规格、PRD 补充 | TL-BOT | tl@project.local |
| 地龙 | feat-dl-* | 后端 Python/SQL、数据库脚本 | DL-BOT | dl@project.local |
| Trae | feat-trae-* | Vue 前端、TypeScript、前端工程化 | TRAE-BOT | trae@project.local |
| 人工 | feat-* / fix-* | 全栈、架构、审核、紧急修复 | rinomezhang-arch | rinomezhang@gmail.com |

### 2.2 禁止行为清单

```text
// 禁止行为（CI 拦截 + 钩子拦截）
- AI 直接 git push origin main
- AI 直接 git push origin develop
- AI 在 feat-dl-* 分支提交前端 .vue 文件
- AI 在 feat-trae-* 分支提交 .sql 数据库脚本
- AI 创建无前缀的临时分支
- AI 强制推送（git push --force）到任何分支
- AI 越权修改 .github/workflows 或 hooks 配置
```

### 2.3 AI 分支创建示例

```bash
# Trae 创建前端 KDS 看板分支
git checkout develop
git pull origin develop
git checkout -b feat-trae-kds-dashboard-101
# ...开发完成后...
git push -u origin feat-trae-kds-dashboard-101
# 在 GitHub/Gitea 发起 PR：feat-trae-kds-dashboard-101 -> develop
```

---

## 三、PR 审核流程

### 3.1 PR 提交者必填字段

| 字段 | 说明 | 示例 |
|------|------|------|
| 标题 | 遵循 Commit 注释标准格式 | `【TRAE-BOT】kds-dashboard-feat：新增后厨看板实时订单卡片组件` |
| 关联 Issue | 必填，格式 `Closes #101` | `Closes #101` |
| 变更类型 | feat / fix / refactor / docs / chore 等 | feat |
| 影响范围 | 列出受影响的模块、表、接口 | KDS 模块、`/api/kds/orders` 接口 |
| 变更原因 | 业务背景与动机 | 宴会场景下后厨需实时看到新订单 |
| 测试说明 | 自测用例与结果 | 已通过 10 个 KDS 场景用例 |
| 回滚方案 | 出问题如何回退 | 删除组件 + 还原路由配置 |
| 数据库变更 | 是否含 DDL/DML，关联 Flyway 版本 | 无 / V202608021500 |
| 多租户影响 | 是否影响 store_id 隔离 | 不影响，组件层无租户逻辑 |

### 3.2 Reviewer 必填字段

| 字段 | 说明 |
|------|------|
| 代码质量评分 | 1-5 分 |
| 安全审查结论 | 通过 / 需修改 / 拒绝（参考《安全规范_餐饮行业专属.md》） |
| 多租户隔离校验 | 通过 / 不通过 |
| 性能影响评估 | 无影响 / 轻微 / 显著（需附压测数据） |
| 审核意见 | 具体修改建议或通过理由 |
| 审核人签名 | git config user.name |

### 3.3 CI 检查清单（PR 必过）

```yaml
# .github/workflows/pr-check.yml 触发条件
on:
  pull_request:
    branches: [ develop, main ]

CI 检查项（全部通过才允许合并）:
  1. 代码编译（mvn clean package / npm run build）
  2. 单元测试通过率 >= 80%，新增代码覆盖率 >= 70%
  3. Checkstyle / ESLint 静态检查 0 error
  4. SonarQube 严重漏洞数 = 0
  5. Commit 注释格式校验（正则匹配 【工具名】模块-操作：内容）
  6. 分支命名前缀校验（AI 分支与 user.email 一致性）
  7. 文件越权校验（feat-trae-* 分支不得修改 .sql / .py）
  8. Flyway 迁移脚本校验（如有 DDL，必须 V 前缀 + 不可变校验）
  9. 敏感信息扫描（不得包含手机号、密码、密钥明文）
  10. 多租户校验（新增业务表 SQL 必须含 store_id 字段）
```

### 3.4 人工审核标准

- AI 提交的 PR 必须由人工（rinomezhang-arch）审核通过后方可合并。
- 人工提交的 PR 可由另一名人工或指定 AI 协助审核，但合并需人工确认。
- 涉及支付、退款、会员余额、押金的 PR 必须双人审核（至少 1 名人工 + 1 名维护者）。
- 涉及 DDL 变更的 PR 必须额外经过数据库评审（参考《数据字典与 DDL 同步规范.md》）。
- 审核超时 48 小时未处理的 PR，自动通知企业微信群。

---

## 四、Commit 提交注释标准（重点）

### 4.1 格式规范

所有 Commit 必须符合以下格式，无标识或格式错误由 pre-commit 钩子直接拦截，CI 二次校验失败则 PR 无法合并。

```text
【工具名】模块-操作：内容
```

- **工具名**：固定取值 `TL-BOT` / `DL-BOT` / `TRAE-BOT` / `rinomezhang-arch`，与 git config user.name 一致。
- **模块**：英文小写，使用连字符分隔，如 `order` / `kds-dashboard` / `member-points` / `db-migration`。
- **操作**：操作类型前缀（见 4.2）。
- **内容**：中文描述，简洁说明本次提交做了什么（不是为什么），不超过 50 字。
- 标点：模块与操作之间用连字符 `-`，操作与内容之间用中文冒号 `：`。

### 4.2 类型前缀定义

| 前缀 | 含义 | 适用场景 |
|------|------|---------|
| feat | 新功能 | 新增业务功能、新接口、新页面 |
| fix | 缺陷修复 | 修复 Bug、异常、错误逻辑 |
| refactor | 重构 | 不改变外部行为的代码重构 |
| docs | 文档 | 文档新增、修订（PRD、规范、字典） |
| test | 测试 | 新增或修改测试用例 |
| chore | 构建/依赖 | 构建、依赖、配置文件变更 |
| style | 格式 | 代码格式化、空白、命名调整（不改逻辑） |
| perf | 性能 | 性能优化、索引调整、缓存改造 |
| ci | CI 配置 | 流水线、钩子、部署脚本变更 |

### 4.3 各类型示例（每类至少 2 个）

```text
# feat 新功能
【TRAE-BOT】kds-dashboard-feat：新增后厨看板实时订单卡片组件
【DL-BOT】order-refund-feat：新增押金退款三级审批接口

# fix 缺陷修复
【DL-BOT】payment-replay-fix：修复支付回调验签时间戳校验偏移问题
【TRAE-BOT】member-points-fix：修复会员积分变更未触发双因子校验问题

# refactor 重构
【DL-BOT】order-service-refactor：重构订单状态机为策略模式
【TRAE-BOT】table-layout-refactor：重构桌台布局组件抽取公共 DragWrapper

# docs 文档
【TL-BOT】git-branch-spec-docs：新增 Git 分支管理规范文档
【TL-BOT】security-spec-docs：补全餐饮行业专属安全规范脱敏章节

# test 测试
【DL-BOT】refund-approval-test：新增押金退款三级审批单元测试
【TRAE-BOT】kds-dashboard-test：新增 KDS 看板断线重连测试用例

# chore 构建/依赖
【rinomezhang-arch】deps-chore：升级 spring-boot 至 3.2.5
【TRAE-BOT】vite-config-chore：调整 Vite 5 构建分包策略

# style 格式
【DL-BOT】order-mapper-style：统一 OrderMapper 命名风格
【TRAE-BOT】api-js-style：统一 api.js 模块导出缩进为 2 空格

# perf 性能
【DL-BOT】order-index-perf：为 order 表 store_id+status 新增联合索引
【TRAE-BOT】menu-list-perf：菜单列表引入虚拟滚动优化大数据量渲染

# ci CI 配置
【rinomezhang-arch】pr-check-ci：新增 PR 分支前缀与文件越权校验流水线
【DL-BOT】sonar-ci：集成 SonarQube 静态扫描至 CI 流水线
```

### 4.4 中文+英文混合规则

- **模块名**：必须英文，便于检索与跨语言协作。
- **操作前缀**：必须英文（feat/fix/refactor 等为业界通用语义）。
- **内容描述**：必须中文，确保业务语义清晰、可审计。
- **禁止**：纯英文 Commit、纯中文 Commit、中英混杂的模块名。

```text
// 正确
【TRAE-BOT】kds-dashboard-feat：新增后厨看板实时订单卡片组件

// 错误（拦截）
新增后厨看板组件                        // 无工具名标识
【TRAE-BOT】feat：新增后厨看板           // 缺模块名
【TRAE-BOT】后厨看板-feat：新增组件       // 模块名非英文
【TRAE-BOT】kds-dashboard-feat: new card // 内容非中文
【TRAE-BOT】kds_dashboard-feat：新增组件  // 模块名含下划线
```

### 4.5 多行 Commit Body 规则

复杂变更需追加 Body，Body 每行不超过 72 字符，使用中文描述动机与影响。

```text
【DL-BOT】order-refund-feat：新增押金退款三级审批接口

本次新增押金退款接口，支持店长→财务→老板三级审批流，
金额阈值分别为 ≤500/500-5000/≥5000。退款接口集成 Redis 分布式锁
防止并发退款，并记录操作人 IP、设备指纹、地理位置至 audit_log。
关联 Flyway 迁移 V202608021500。
```

---

## 五、提交身份划分表

每个角色必须配置固定的 git 身份，CI 通过 user.email 校验分支前缀与提交者一致性，不一致直接拦截。

| 角色 | user.name | user.email | 专属分支前缀 | 职责 |
|------|-----------|-----------|-------------|------|
| 天龙 | TL-BOT | tl@project.local | feat-tl-* | 文档、需求规格、PRD |
| 地龙 | DL-BOT | dl@project.local | feat-dl-* | 后端 Python/SQL、数据库脚本 |
| Trae | TRAE-BOT | trae@project.local | feat-trae-* | Vue 前端、TypeScript |
| 人工 | rinomezhang-arch | rinomezhang@gmail.com | feat-* / fix-* / release-* / hotfix-* | 全栈、架构、审核、紧急修复 |

### 5.1 身份配置命令

```bash
# Trae 配置示例
git config user.name "TRAE-BOT"
git config user.email "trae@project.local"

# 人工配置示例
git config user.name "rinomezhang-arch"
git config user.email "rinomezhang@gmail.com"
```

### 5.2 一致性校验规则

```text
CI 校验逻辑：
  if commit.author.email == "trae@project.local":
      分支必须以 feat-trae- 开头，否则拒绝
  if commit.author.email == "dl@project.local":
      分支必须以 feat-dl- 开头，否则拒绝
  if commit.author.email == "tl@project.local":
      分支必须以 feat-tl- 开头，否则拒绝
  Commit 注释【工具名】必须与 user.name 一致
```

---

## 六、分支生命周期

### 6.1 生命周期阶段

| 阶段 | 操作 | 责任人 | 产物 |
|------|------|--------|------|
| 创建 | 从 develop 拉出 feat-* 分支 | 提交者 | 新分支 + Issue 关联 |
| 开发 | 本地提交，定期 rebase develop | 提交者 | Commit 序列 |
| 自测 | 本地跑通单元测试与场景用例 | 提交者 | 自测报告 |
| 推送 | push 到远程并发起 PR | 提交者 | PR |
| 审核 | Reviewer 审核 + CI 检查 | Reviewer + CI | 审核意见 |
| 合并 | Squash merge 到 develop | 维护者 | 合并 Commit |
| 删除 | 合并后删除远程与本地分支 | 维护者 | 分支清理 |
| 归档 | PR 与 Issue 关闭归档 | 维护者 | 历史记录 |

### 6.2 合并策略

- **feat-* → develop**：使用 Squash merge，保留单一整洁 Commit。
- **develop → release-***：使用普通 merge，保留集成历史。
- **release-* → main**：使用 merge commit，便于追溯发布点。
- **hotfix-* → main + develop**：双向 merge，确保修复同步到集成分支。

```bash
# Squash merge 示例
git checkout develop
git merge --squash feat-trae-kds-dashboard-101
git commit -m "【TRAE-BOT】kds-dashboard-feat：新增后厨看板实时订单卡片组件"
git push origin develop
git branch -d feat-trae-kds-dashboard-101
git push origin --delete feat-trae-kds-dashboard-101
```

---

## 七、冲突解决规范

### 7.1 冲突预防

- 每日开发前执行 `git fetch origin && git rebase origin/develop` 保持分支最新。
- 避免多角色同时修改同一文件，PR 前先在群里同步。
- 大型重构必须先发 Issue 占位，24 小时内无异议再动手。

### 7.2 冲突解决流程

| 步骤 | 操作 | 责任人 |
|------|------|--------|
| 1 | rebase 时遇到冲突，git status 查看冲突文件 | 提交者 |
| 2 | 仅解决自己分支引入的冲突，不得擅自修改他人代码 | 提交者 |
| 3 | 涉及他人代码语义的冲突，必须 @ 对应作者协商 | 提交者 + 原作者 |
| 4 | 解决后运行完整测试套件确认无破坏 | 提交者 |
| 5 | 继续 rebase，直至无冲突 | 提交者 |
| 6 | 强制 push 到自己分支（仅限 feat-* 个人分支） | 提交者 |

```bash
# rebase 冲突解决
git fetch origin
git rebase origin/develop
# 解决冲突后
git add <冲突文件>
git rebase --continue
# 个人分支可强制 push（main/develop 禁止）
git push origin feat-trae-kds-dashboard-101 --force-with-lease
```

### 7.3 冲突解决禁忌

- 禁止使用 `git merge` 把 develop 合入 feat 分支（产生脏 merge commit）。
- 禁止 `git rebase` 已 push 到远程且他人正在使用的分支。
- 禁止 `git checkout --theirs/--ours` 批量覆盖冲突（必须人工审查）。

---

## 八、标签管理

### 8.1 语义化版本规范

项目遵循 Semantic Versioning 2.0.0：`MAJOR.MINOR.PATCH`。

| 版本位 | 含义 | 触发条件 |
|--------|------|---------|
| MAJOR | 主版本 | 不兼容的架构变更（如 1.x → 2.0） |
| MINOR | 次版本 | 向下兼容的功能新增 |
| PATCH | 修订号 | 向下兼容的缺陷修复 |
| 预发布 | -rc.N / -beta.N / -alpha.N | 发布前测试版本 |

### 8.2 标签命名与创建

```bash
# 正式发布
git tag -a v2.0.0 -m "【rinomezhang-arch】release-tag：发布 2.0.0 正式版"
git push origin v2.0.0

# 预发布
git tag -a v2.0.0-rc.1 -m "【rinomezhang-arch】release-tag：2.0.0 第一个候选版本"
git push origin v2.0.0-rc.1

# 紧急修复
git tag -a v2.0.1 -m "【rinomezhang-arch】hotfix-tag：修复支付回调验签问题"
git push origin v2.0.1
```

### 8.3 标签管理规则

- 标签只能由人工（rinomezhang-arch）创建，AI 不得打标签。
- 正式标签必须打在 main 分支的合并 Commit 上。
- 预发布标签打在 release-* 分支上。
- 标签一旦发布不可删除、不可重命名（历史可追溯）。
- 每个标签必须对应一份 Release Notes，包含 DB 版本号、Flyway 版本、新增功能、修复列表、已知问题。

---

## 九、钩子与自动化

### 9.1 pre-commit 钩子检查项

```bash
# .git/hooks/pre-commit 或 husky 配置
检查项:
  1. Commit 注释格式正则匹配：^【(TL-BOT|DL-BOT|TRAE-BOT|rinomezhang-arch)】[a-z-]+-(feat|fix|refactor|docs|test|chore|style|perf|ci)：.+
  2. user.name 与【工具名】一致性校验
  3. 文件越权校验（feat-trae-* 不得提交 .sql/.py/.java）
  4. 敏感信息扫描（手机号正则、密码关键字、AK/SK）
  5. 大文件检查（> 10MB 需走 LFS）
  6. 行尾符统一（CRLF → LF）
  7. .java 文件不得包含 System.out.println（强制用日志框架）
```

### 9.2 CI 流水线触发条件

| 事件 | 触发流水线 | 必过检查 |
|------|-----------|---------|
| push 到 feat-* | build-test | 编译 + 单测 + 静态检查 |
| PR 到 develop | pr-check | 全部 10 项 CI 检查 |
| PR 到 main | release-check | pr-check + 安全扫描 + 人工双签 |
| 合并到 main | release-build | 构建镜像 + 打标签 + 部署预发 |
| 定时（每日 02:00） | nightly | 全量回归 + SonarQube + 依赖漏洞扫描 |

### 9.3 Commit-msg 钩子示例

```bash
#!/bin/bash
# commit-msg 钩子：校验 Commit 注释格式
msg=$(cat "$1")
pattern='^【(TL-BOT|DL-BOT|TRAE-BOT|rinomezhang-arch)】[a-z][a-z0-9-]*-(feat|fix|refactor|docs|test|chore|style|perf|ci)：.+'
if [[ ! "$msg" =~ $pattern ]]; then
  echo "错误：Commit 注释不符合规范"
  echo "正确格式：【工具名】模块-操作：内容"
  echo "示例：【TRAE-BOT】kds-dashboard-feat：新增后厨看板实时订单卡片组件"
  exit 1
fi
```

---

## 十、紧急回滚预案

### 10.1 回滚场景分级

| 级别 | 场景 | 回滚方式 | 时效要求 |
|------|------|---------|---------|
| P0 | 支付/退款资金错误 | 标签回退 + 数据修复 | 30 分钟内 |
| P1 | 核心功能不可用 | revert 合并 Commit | 1 小时内 |
| P2 | 非核心功能异常 | 下一版本修复 | 24 小时内 |
| P3 | 体验问题 | 紧急 hotfix 分支 | 48 小时内 |

### 10.2 回滚操作流程

```bash
# 方式一：revert 合并 Commit（推荐，保留历史）
git log --oneline -10  # 找到合并 Commit
git revert -m 1 <merge-commit-sha>
git push origin main

# 方式二：标签回退（P0 场景，快速恢复）
git checkout main
git reset --hard v2.0.0   # 回退到上一个稳定标签
git push origin main --force-with-lease  # 仅 P0 场景允许强制 push main，需人工审批
```

### 10.3 回滚后必做事项

1. 立即在企业微信群通报回滚原因、影响范围、当前状态。
2. 24 小时内提交事故复盘报告（根因、时间线、改进措施）。
3. 数据库变更回滚必须配套执行 U{版本号} 回滚脚本，并记录至 CHANGELOG.md。
4. 回滚产生的 Commit 必须遵循 Commit 注释标准，前缀用 `fix`。
5. 修复完成后必须重新走完整 PR 流程，不得直接 push 修复代码。

### 10.4 回滚禁忌

- 禁止 AI 角色执行任何回滚操作（必须人工）。
- 禁止在未备份数据库的情况下回滚含 DDL 的版本。
- 禁止使用 `git push --force` 替代 `--force-with-lease`。
- 禁止回滚后不更新 Release Notes 与 CHANGELOG。

---

## 附则

- 本规范由 Trae（TRAE-BOT）维护，修订需提 PR 经人工审核。
- 本规范与《安全规范_餐饮行业专属.md》《数据字典与 DDL 同步规范.md》《断线重连与消息可靠性规范.md》配套使用。
- 规范冲突时以本文件为准，但安全相关条款以《安全规范_餐饮行业专属.md》为准。
- 本规范自 2026-08-02 起生效，适用于又见炊烟餐饮管理系统 2.0 全部仓库。

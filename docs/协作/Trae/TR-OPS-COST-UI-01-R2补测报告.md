# TR-OPS-COST-UI-01 R2 补测报告（Trae，2026-09-07）

响应 Codex 20260907T131905Z changes_requested 四条要求。本轮**全部闭环，15/15 PASS**。

## 测试版本声明（Codex 要求第 4 条）

- 被测代码：本工作树 `artifacts/team-worktrees/trae`，基线 **5bd75305**（+Trae 文档提交 03bd7bf/37358a9），分支 codex/trae-cost-ui。
- 整合版 **c8590de**（含 utils/dishCostPreview.js 及 CostRecipe.vue 预览修复）**未在本任务测试范围**，本报告结果不代表整合版已通过浏览器验收。
- 上轮关于"onIngredientPick 预览单价混用"的观察基于 5bd75305，整合版已修，本任务不重写、不覆盖。

## 方法变更（为什么换 Playwright）

上轮手工 browser-use 三轮均因 60 步预算耗尽 + 会话僵尸浮层导致失败提示未捕获；本轮改为**确定性自动化**：
- 新增 `scripts/trae-cost-e2e/`（独立 npm 包，.gitignore 排除 node_modules，不污染 frontend_v3 依赖）
- `trae-cost-e2e.mjs`：Playwright + 系统 Edge 通道（channel=msedge，无需下载浏览器），原生 `viewport:{width:390,height:844}`，全新 browser context（无 localStorage，等同干净会话）
- 自动监听全部 /api/ 请求、可见性断言（.el-message--error/--success:visible）、自动截图
- 复跑：`cd scripts/trae-cost-e2e && npm i playwright && node trae-cost-e2e.mjs`（前置：8080 后端 + 5174 前端 + 13317 隔离库种子就绪）

## Codex 四条要求逐条响应

**① 干净会话 + 已纠正原料价格复验成功保存与回读，不重跑破坏性种子 → 满足**
- 全新 context 登录；未重跑种子 DELETE 段（库保持上轮修正后状态，仅脚本复跑前用 SQL 把 TR-DISH-003 数量恢复 500 测试基线）。
- 列表实测价格（修正采购单位语义后）：红烧肉 ¥20.32/率35.0%、清蒸鲈鱼 ¥20.00/率22.7%、酸辣土豆丝 ¥1.06/率5.9%。
- 成功链路：保存 toast 可见「配方保存成功」→ 重开回读 rows=1、五花肉 qty=300.000、行文本「TR验收_五花肉 ¥0.04 ¥12.00」（300克×0.04净价=12.00，小计正确）。

**② 可设置 viewport 的浏览器实际 390px → 满足（不再 blocked）**
- 手工 browser-use 工具确认无 resize/set_viewport 能力（具体 blocked 证据：window.resizeTo(390,844) 仅 outerWidth 变 390，innerWidth 仍 615；已留证）。
- Playwright 原生 viewport 解决：断言 `innerWidth=390` PASS；登录页/列表/失败弹窗/成功回读/增删行全部在 390×844 下执行并截图（pw-01~pw-05）。
- 390px 增删行：红烧肉弹窗初始 2 行 → 添加原料 3 行 → 删除 3 行恢复 2 行 → 取消关闭，PASS。

**③ 失败保存实际可见提示 + 弹窗保留 + 可取消 + 数据库旧版本 → 满足，证据如下**
- 操作：TR-DISH-003 清蒸鲈鱼（基线 1 行五花肉 500 克）→ 添加 TR验收_生抽 99999999 毫升 → 保存。
- **可见错误 toast 原文**：`保存配方失败: could not execute statement [Data truncation: Out of range value for column 'quantity' at row 1] [insert into di...`（.el-message--error:visible 断言 PASS）。
- 弹窗保留：保存后 dialogs=1、rows=2（原行+失败行）；失败输入保留：qty=99999999.000 仍在。
- 可取消：点「取消」后 visible overlays=0。
- **数据库断言（业务 ID 前后对比）**：
  - 失败前：TR-DISH-003 = TR-ING-01 五花肉 500.000 克
  - 失败后页面回读（重开弹窗）：rows=1、qty=500.000（旧版本保留，回滚在 UI 可见）
  - DB 终态：TR-DISH-003 无 TR-ING-003 生抽行（COUNT=0，失败数据未落库）；五花肉行未被删除
  - 随后成功保存 300 克落库：TR-DISH-003 五花肉 300.000、total_cost=12.0000、dish_master.cost_price=12.00、cost_rate=13.64（12/88）
- 截图：f:\solo\screenshots\trae-cost-pw-03-fail-toast390.png（失败提示）、pw-04-success-readback390.png（回读）。

**④ 报告标明测试版本 → 见上"测试版本声明"。**

## 15 项断言结果（全绿）

```
PASS login redirect            → /dashboard/home
PASS viewport innerWidth=390   → innerWidth=390
PASS old entry renders CostRecipe → 成本配方 · Cost Recipe @/dashboard/finance/dish-cost
PASS list non-empty TR dishes  → 三菜齐全，价格为修正后净价
PASS fail-test dialog open     → 配方编辑 - TR验收_清蒸鲈鱼 rows=1
PASS fail-test input prepared  → qty=99999999
PASS fail toast VISIBLE        → 保存配方失败: ...Data truncation: Out of range...
PASS dialog stays open         → dialogs=1 rows=2
PASS failed input retained     → 99999999.000
PASS cancel closes dialog      → visible overlays=0
PASS readback after rollback   → rows=1 qty=500.000（旧版本保留）
PASS success toast VISIBLE     → 配方保存成功
PASS save success readback 300 → rows=1 qty=300.000 小计¥12.00
PASS 390px add/remove row      → 2→3→2
PASS ZERO /dish-cost/* requests → 18 个 API 调用，/dish-cost/ 命中 0
```

## 旧证据保留说明

- 上轮 8 张截图（trae-cost-01~08b）保留在 f:\solo\screenshots\，其中 558px 视口与手工会话结果被本轮 Playwright 390px 确定性证据取代；旧证据不删除，作为过程记录。
- 控制台观察到的 401（登录前 /api/ai/models 竞态）与 404（静态资源）不影响业务断言，记录在此。

## 交付物

- 脚本：`scripts/trae-cost-e2e/trae-cost-e2e.mjs`（可复跑，15 断言）
- 依赖：`scripts/trae-cost-e2e/package.json`（playwright 独立依赖，node_modules 不入库）
- 截图：f:\solo\screenshots\trae-cost-pw-01~05.png（390px）
- 环境：后端 8080 / 前端 5174 / 隔离库 trae_cost_e2e_20260907@13317，均未改动业务源码

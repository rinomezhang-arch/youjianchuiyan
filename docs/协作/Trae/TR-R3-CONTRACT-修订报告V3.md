# TR-R3-CONTRACT 修订报告 V3（Trae · 响应 Codex changes_requested 事件3）

- 任务 ID：TR-R3-CONTRACT
- 基线：637b08b（不变）；分支 codex/trae-cost-ui 新提交：**6f5adf20**（已推送 origin）
- 响应对象：COS 任务板 events/TR-R3-CONTRACT/00000003.json（Codex，20260907T110804Z，changes_requested）
- 说明：本轮 reported 事件通道被另一 trae 身份的 V2 报告占用（status=reported 仅能由 Codex 推进），本报告以分支+文档+登记簿+人工转达为通道，未绕过任务板校验。

## 逐条响应 Codex 指令

| # | 指令 | 响应 |
|---|---|---|
| 1 | dishCostPreview.js 只读核对带入工作树，不另造 | 已核对：worktree 未跟踪副本 MD5=7245802015445CA14BCC4588AB11FA27 与主干一致；候选基线 59c3634 已含该文件，整合时以基线版为准。注：V2 报告 §0 的"已存在"核对发生在 19:15 之后，其所检文件很可能是我 19:15 放置的临时副本，属同 actor 双实例交叉污染（详见开发记录） |
| 2 | DishCost 可达→统一到 CostRecipe 入口，历史兼容，禁删旧文件 | **已执行**：Dashboard.vue:277 菜单「菜品成本」→ /dashboard/finance/dish-cost 确认可达；DishCost.vue 回退基线后改为组件内重定向（onMounted 首行 router.replace({name:'CostRecipe'}) 并 return，原逻辑保留不删不执行）；幽灵接口族 /dish-cost/* 九个端点零发出；router.replace 在组件内完成，**未动共享路由**（满足指令5） |
| 3 | ranking 字段对齐 | CostController.getRanking 返回 dishId/dishName/category/salePrice/costPrice/costRate(ROUND 1) 与 CostRecipe.vue 读取字段逐一命中（V2 §3.2 结论一致，无分歧） |
| 4 | 独立目录小补丁 + 实际页面请求/非空示例/窄屏验证 | 补丁已提交（见下）；实际页面请求验证见文末状态 |
| 5 | 报任务ID/提交/文件/测试结果，等验收；不动共享路由原文件 | 本报告即回报；共享路由未动（git diff 可证：变更仅 CostRecipe.vue、DishCost.vue、docs/协作/Trae/*） |

## 当前分支补丁构成（6f5adf20，基于 74731502 ← c42541d3）

1. **CostRecipe.vue（保留，新增证据支撑 C1）**
   - C1：saveRecipe 的 `POST /recipes/{dishId}` 显式 `params.storeId=currentStoreId`。**决定性证据**：frontend_v3/src/utils/request.js 第 43 行 `if (config.method === 'get')` —— storeId 自动注入仅限 GET，POST 裸奔 → RecipeController saveRecipe `defaultValue="1"` → 总经理在门店2保存配方写进门店1。**V2 报告只核对了端点存在性，未核对 POST 的 storeId 契约，此缺口为 V1/V2 均未覆盖的真实缺陷**。
   - C2：配方单位自由文本 → 原料档案单位下拉（usageUnit/purchaseUnit 白名单，DishCostCalculator.java:30 保存时拒绝其余单位）；onIngredientPick 默认单位对齐 usageUnit||purchaseUnit（与主工作区未提交修复同文，三方合并无冲突）。
2. **DishCost.vue（按指令2 重做）**
   - 回退 c42541d3 的整页重连方案（该方案完整保留于分支历史 c42541d3，若 Codex 改选「补后端+重连」路线可直接复用，勿丢弃）。
   - 现状 = 组件内重定向 shim：用户点「菜品成本」→ 提示「菜品成本管理已并入成本配方页面」→ 落到 CostRecipe。零虚构端点、零后端改动、历史 URL 兼容。
3. 构建证据：`npm run build` 通过（1m05s，含修订后 DishCost 分包）。

## 指令4「实际页面请求/非空示例/窄屏」状态

- 本地 8080（后端）/3306（MySQL）/5173 均未运行；起完整栈需 MySQL 初始化+Maven 构建，且与主工作区运行环境存在资源竞争。
- 已完成：三处真源交叉核验的静态契约证据 + 构建级验证；**端到端页面请求验证如实登记为未执行**，不标通过。
- 明确区分：本补丁不包含任何模拟接口——DishCost shim 不发任何请求；CostRecipe 全部调用真实端点。前端 previewRecipeLine 仅展示预览，落库金额以后端 BigDecimal 重算为准。

## 待 Codex 裁决

1. 验收 6f5adf20（CostRecipe C1/C2 + DishCost 重定向 shim）。
2. c42541d3 重连方案作为方向B备选的取舍。
3. 双 trae 实例撞车的流程处理（建议：任务板 event 增加实例标识，或统筹为同 actor 多实例分配子任务）。

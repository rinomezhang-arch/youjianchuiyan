# TR-R3-CONTRACT 契约比对报告（Trae · 修正版 V2）

- 任务 ID：TR-R3-CONTRACT
- BASE_SHA：637b08b8257552a141a659407838dbc6b9260379
- 分支 / 工作树：codex/trae-cost-ui @ artifacts/team-worktrees/trae
- 交付物：route/API/field comparison + small frontend candidate diff + 实际页面请求验证
- 日期：2026-09-07
- **本版修正 V1 报告的一处误判**（见 §0）

---

## §0 对 V1 报告的修正声明（重要）

V1 报告第 §4 称"`CostRecipe.vue` 引用了缺失的工具文件 `@/utils/dishCostPreview`"。**此判断错误，予以撤回。**

真实情况（已核实）：
- `frontend_v3/src/utils/dishCostPreview.js` 在我的 worktree 完整 checkout 中**已存在**，长度 1510 字节。
- 与共享主干 `F:\solo\frontend_v3\src\utils\dishCostPreview.js` 的 **MD5 完全一致**（`7245802015445CA14BCC4588AB11FA27`）。
- 我 V1 只看了输入包 `assets/inputs-r3.zip`（它只含 2 个 vue 文件、不含 utils），没有回到 worktree 完整前端目录再核实，就下了"缺失"结论。这是**核查不彻底**，已纠正。

Codex 反馈确认：`dishCostPreview.js` 已存在于共享工作区和候选基线 `59c36343`，输入包缺文件属交付包遗漏，无需另造。

---

## 一、结论（V2 定稿）

两个菜肴成本入口，**两类确认的不一致**：

1. **入口二 `DishCost.vue` 是一整篇"有前端无后端"的孤儿页面** —— 它有真实路由入口 `finance/dish-cost`，但调用的 `/dish-cost/*` 全部 9 个端点在后端源码中**完全不存在**（见 §5）。
2. **入口一 `CostRecipe.vue` 与后端契约字段对齐，无 mismatch** —— 此前担心的 `dishCostPreview.js` 缺失、ranking 字段不对齐，均已核实**不存在问题**。

---

## 二、输入范围与核实方法

本轮三个真源交叉核实：
- 输入包 `assets/inputs-r3.zip`（SHA256 与任务 JSON 一致）
- 我的 worktree 完整 checkout `artifacts/team-worktrees/trae`（基线 637b08b）
- 共享主干 `F:\solo`（只读）

后端成本/菜品相关端点全貌（共享主干 `controller/` 全量扫描）：

| Controller | 前缀 | 谁在用 |
|---|---|---|
| RecipeController | `/api/recipes/*` | CostRecipe.vue ✅ |
| CostController | `/api/cost/*`（含 `/ranking`）| CostRecipe.vue ✅ |
| IngredientController | `/api/ingredients` | CostRecipe.vue ✅ |
| KitchenSupplyController | `cost-cards/*` | 无前端调用 |
| DishController | `/api/dishes` | 非成本页 |
| **任何 Controller** | **`/dish-cost/*`** | **不存在** ❌ |

---

## 三、入口一：CostRecipe.vue（前端 → 后端契约，全部对齐 ✅）

### 3.1 路由/API 比对

| 前端调用 | HTTP | 后端接口 | 结论 |
|---|---|---|---|
| `GET /ingredients?storeId=` | GET | `IngredientController.getAllIngredients` | ✅ 存在，storeId required 已传 |
| `GET /cost/ranking?size=500` | GET | `CostController.getRanking` | ✅ 存在 |
| `GET /recipes/dishes-with-recipe` | GET | `RecipeController.dishesWithRecipe` | ✅ 存在 |
| `GET /recipes/{dishId}` | GET | `RecipeController.getRecipe` | ✅ 存在 |
| `POST /recipes/{dishId}` | POST | `RecipeController.saveRecipe` | ✅ 存在 |
| `POST /recipes/recalc-all` | POST | `RecipeController.recalcAll` | ✅ 存在 |

### 3.2 `GET /cost/ranking` 字段对齐（Codex 指令 3）

后端 `CostController.getRanking` 返回（`Result<Map>`，分页用 `content` 包裹）：
```
dishId, dishName, category, salePrice, costPrice,
costRate(ROUND 1 位), marginRate, profit, unit
```

前端 `CostRecipe.vue` 读取：
```js
const rankingData = rankingRes.data || rankingRes
list.value = (rankingData.content || []).map(d => ({
  dishId: d.dishId, dishName: d.dishName, categoryName: d.category,
  salePrice: d.salePrice, costPrice: d.costPrice, costRate: Number(d.costRate)
}))
```

字段名 `dishId/dishName/category/salePrice/costPrice/costRate` **完全命中**，`content` 分页结构也对。**无 mismatch。**

> 注意：后端 `ranking` 的 `costRate` 是 `ROUND(...,1)` 已是数值；前端 `Number(d.costRate)` 再包一层无害。

### 3.3 `dishCostPreview.js`（Codex 指令 1）

- 前端 `CostRecipe.vue` `import { previewRecipeLine } from '@/utils/dishCostPreview'`。
- 文件存在，公式与后端 `DishCostCalculator.calculateLine` 语义一致（`net = price*100/(conversion*yield)`，出成率上限 999.99，`toFixed(8)`/`toFixed(4)`）。
- **无需改动。** 前端仅做展示预览，后端落库用 BigDecimal 重算覆盖，符合"preserve price precision + explicit cost errors"验收点。

---

## 四、（V1 第 §4 已撤回，此处空）

---

## 五、关键问题：DishCost.vue 是孤儿页面（Codex 指令 2）

### 5.1 路由入口确实可达

`frontend_v3/src/router/index.js` 第 76 行：
```js
{ path: 'finance/dish-cost', name: 'FinanceDishCost',
  component: () => import('@/views/dashboard/DishCost.vue'),
  meta: { requiresAuth: true, title: '菜品成本管理' } }
```

`CostRecipe.vue` 第 143 行也有对 DishCost 的引用。**页面是真实挂载、真实可达的**，不是死代码。

### 5.2 但它调用的后端端点全部不存在

`DishCost.vue` 调用的 9 个接口：

| 前端调用 | HTTP | 后端是否存在 |
|---|---|---|
| `GET /dish-cost/dishes` | GET | ❌ 不存在 |
| `POST /dish-cost/dishes` | POST | ❌ |
| `PUT /dish-cost/dishes/{id}` | PUT | ❌ |
| `GET /dish-cost/recipe/{id}` | GET | ❌ |
| `PUT /dish-cost/recipe/{id}` | PUT | ❌ |
| `GET /dish-cost/ingredients?limit=` | GET | ❌ |
| `POST /dish-cost/ingredients` | POST | ❌ |
| `POST /dish-cost/upload` | POST | ❌ |
| `DELETE /dish-cost/image/{name}` | DELETE | ❌ |

核实证据：在输入包、worktree、共享主干三处，分别用 `dish-cost`、`dishCost`、`DishCost`、`@RequestMapping` 全量且宽松地检索 `banquet_project/src` 全部 `.java`，唯一命中是 `DishCostCalculator`（共享服务）、`RecipeController`（调它）、`KitchenSupplyService`（调它）。**没有任何 Controller 用 `@RequestMapping`/`@GetMapping` 等声明 `/dish-cost` 前缀或其子路径。**

### 5.3 附带精度问题（若后端未来补齐时需注意）

`DishCost.vue` 的 `calcRow` 与共享公式 `DishCostCalculator` 口径不一致：
- 前端：`netUnitPrice = unitPrice / (1 - wastageRate)`，用 JS `Math.round`，**不读 conversion_rate、不除 yield_rate**。
- 后端：`netPrice = price*100/(conversionRate*yieldRate)`。

属"若补后端则前端须同步改"项，本文登记，不在本次改动（后端缺失是前置阻断）。

---

## 六、单位/出成率展示口径（两入口对比）

| 字段 | CostRecipe.vue | DishCost.vue | 后端字段/约束 |
|---|---|---|---|
| 单位 | 自由输入（克/斤/个）| 采购单位 purchaseUnit | IngredientMaster.purchaseUnit/usageUnit |
| 出成率 | 0.01–999.99（precision 2）| 0–100（precision 1）| DishRecipe.yieldRate scale2；DishCostCalculator 上限 999.99 |
| 成本率 | toFixed(1)% | toFixed(1)% | DishMaster.costRate scale2 |

不一致点：DishCost.vue 出成率 `:max="100"`，后端允许 >100（吸水原料）。**若 DishCost.vue 处置结果是要保留，需改上限。**

---

## 七、候选修复建议（小 diff，待 Codex 定夺）

Codex 指令 2 说：DishCost 可达则应"统一到已存在 CostRecipe 入口，保存历史兼容导航，禁止删除旧文件"。

我评估的**现实约束**：DishCost.vue 调用的 `/dish-cost/*` 后端完全不存在，所以它不是"换个入口就能通"，而是**后端根本没实现**。这不是我一个前端小 diff 能补齐的（任务 acceptance 明确 `No invented endpoint`——我不能虚构后端端点）。

因此给 Codex 两个方向，均属"后端决策"，非我单方面落盘：

- **方向 A（推荐）**：`finance/dish-cost` 路由重定向到 CostRecipe 入口（`finance/cost-recipe` 或直接 `redirect`），保留 `DishCost.vue` 文件不删除，历史导航 `finance/dish-cost` 兼容跳转。这样用户点"菜品成本管理"落到真正可用的 CostRecipe 页。
- **方向 B**：补齐 `/dish-cost/*` 后端（属于后端工作量，需 Codex 或 Claude 承接，超 Trae 前端边界）。

Trae 可执行的**最小前端补丁**（若采纳方向 A）：在 `router/index.js` 把 `finance/dish-cost` 的 component 从 `DishCost.vue` 改为 `CostRecipe.vue`（或加 redirect），文件保留。**但这改的是共享路由 `router/index.js`**，而任务 acceptance 明确 `no shared router edit`——所以此改动需 Codex 明确授权，我暂不动手。

---

## 八、测试证据（Codex 指令 4：验证实际页面请求）

本轮交付为**契约比对 + 补丁建议**，非后端改动。测试手段与证据：

1. **路由入口核实**：`router/index.js` 第 76 行确含 `finance/dish-cost` → DishCost.vue，`meta.requiresAuth`。
2. **后端端点存在性核实**：三处全量源码检索 `dish-cost` 前缀，零命中（§5.2 证据）。
3. **rankging 字段核对**：`CostController.getRanking` SQL 返回字段与前端 `map` 字段逐一比对，全部命中（§3.2）。
4. **dishCostPreview 一致性核对**：worktree 与主干文件 MD5 相同（§0 证据）。

未执行的（需真实运行环境 + Codex 判定方向后）：
- 未跑 `npm build`（前端无实际改动，构建无意义）。
- 未做窄屏手机截图（需先确定 DishCost.vue 处置方向，且需要登录态；本机无生产 JWT，不伪造）。

明确区分：本报告是**真实源码检索证据**，非模拟接口。前端展示不替代后端金额重算（dishCostPreview 注释已声明 preview-only）。

---

## 九、未覆盖项 / 风险 / 下一步

1. **DishCost.vue 处置方向**：需 Codex 决定方向 A（路由重定向）还是方向 B（补后端）。这是卡住我的唯一前置项。
2. **共享路由改动授权**：方向 A 需改 `router/index.js`（acceptance 禁止 trae 动共享路由），须 Codex 书面授权或由 Codex 自己改。
3. **`cost-cards/*` 未使用**：后端有 `KitchenSupplyController` 的 cost-cards 接口无前端调用，属潜在冗余，仅登记。
4. 前端浮点精度：已用"前端 preview-only、后端 BigDecimal 落库"规避，无需改。

**下一步依赖**：等 Codex 就 DishCost.vue 处置方向给出指令（changes_requested 阶段）。

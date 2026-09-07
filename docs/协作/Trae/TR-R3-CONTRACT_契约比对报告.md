# TR-R3-CONTRACT 契约比对报告（Trae）

- 任务 ID：TR-R3-CONTRACT
- BASE_SHA：637b08b8257552a141a659407838dbc6b9260379
- 分支 / 工作树：codex/trae-cost-ui @ artifacts/team-worktrees/trae
- 交付物：route/API/field comparison + small frontend candidate diff
- 日期：2026-09-07

---

## 一、结论先行

两个菜肴成本入口（`CostRecipe.vue` 与 `DishCost.vue`）存在**三类确认的不一致**：

1. **入口二 `DishCost.vue` 是一整篇"有前端无后端"的孤儿页面**——它调用的 `/dish-cost/*` 全部端点在后端代码中不存在。
2. **入口一 `CostRecipe.vue` 引用了一个输入包缺失的前端工具文件** `@/utils/dishCostPreview`（`previewRecipeLine`）。
3. **两入口对"最近一次有效入库价 / 净料单价 / 出成率"的计算口径不统一**：后端已是共享 `DishCostCalculator`（BigDecimal + 单位换算 + 出成率），前端 `DishCost.vue` 仍是 JS 浮点 `Math.round` 私算，且不读 `conversion_rate`。

下面逐项给出证据。

---

## 二、输入包范围声明

`assets/inputs-r3.zip`（SHA256 与任务 JSON 一致）只包含以下后端文件：

```
controller/RecipeController.java          ← 唯一控制器
entity/DishRecipe / DishMaster / IngredientMaster
repository/DishRecipe / DishMaster / IngredientMaster
service/DishCostCalculator / KitchenSupplyService
```

**输入包内没有 `/dish-cost` 控制器，也没有 `IngredientController`。** 但我在自己的 worktree（完整 checkout）里，以及共享主干（只读）里都做了交叉核实：`/dish-cost` 控制器在整工程任何一个分支都没有。`/api/ingredients` 端点真实存在（`IngredientController`）。

---

## 三、入口一：CostRecipe.vue（前缀 `/recipes/*`，后端存在）

### 3.1 路由与 API 比对

| 前端调用 | HTTP | 后端实现 | 结论 |
|---|---|---|---|
| `GET /ingredients?storeId=` | GET | `IngredientController.getAllIngredients`（`/api/ingredients`）| ✅ 存在；但 `storeId` 是 required `@RequestParam String`，前端 `params:{storeId: currentStoreId}` 传了，OK |
| `GET /cost/ranking?size=500` | GET | 需 CostController（未在输入包，待确认，见 §5 未覆盖项） | 🟡 未在输入包核对 |
| `GET /recipes/dishes-with-recipe` | GET | `RecipeController.dishesWithRecipe` | ✅ 存在 |
| `GET /recipes/{dishId}` | GET | `RecipeController.getRecipe` | ✅ 存在 |
| `POST /recipes/{dishId}` | POST | `RecipeController.saveRecipe` | ✅ 存在 |
| `POST /recipes/recalc-all` | POST | `RecipeController.recalcAll` | ✅ 存在 |

### 3.2 字段契约比对（保存配方请求体）

前端 `saveRecipe` 提交：
```js
{ ingredientId, quantity, unit, yieldRate, wastageRate }
```

后端 `convertMapToRecipe` 读取：`ingredientId`(或 ingredient_id)、`quantity`、`unit`(或 usageUnit/usage_unit)、`yieldRate`(或 yield_rate)、`wastageRate`(或 wastage_rate)。

✅ **字段名与后备名完全兼容**，camelCase 直接命中。

### 3.3 精度

- 前端 `linePreview`（依赖缺失，见 §4）显示 `netUnitPrice.toFixed(4)`、`totalCost.toFixed(2)`。
- 后端 `DishRecipe.unitPrice`/`netUnitPrice` 是 `scale=8`，`totalCost` 是 `scale=4`。
- 前端只用 `toFixed` 做**展示**，不做持久化算术，精度未丢失。但 `CostRecipe.vue` 的 `calculatedCost` 用 `Number(...toFixed(2))` 聚合，属于**展示层取整**，与后端 `setScale(2, HALF_UP)` 一致，可接受。

---

## 四、关键问题 1：`CostRecipe.vue` 引用了缺失工具文件

`CostRecipe.vue` 第 4 行：
```js
import { previewRecipeLine } from '@/utils/dishCostPreview'
```

输入包 `frontend_v3/src/views/dashboard/` 只有两个 vue 文件，**没有 `utils/dishCostPreview.js`**。我在自己的 worktree 完整 checkout 里确认：`frontend_v3/src/utils/` 只有 `fallback.js / menuStore.js / request.js`，**没有 `dishCostPreview.js`**。

影响：`CostRecipe.vue` 的 `linePreview` 会在运行时 `previewRecipeLine is not a function`，配方编辑弹窗的净料单价/小计/成本率全部崩。

这属于"确认的不一致"，是前端候选修复对象（见 §7 候选 diff）。

---

## 五、关键问题 2：`DishCost.vue` 整套后端缺失（孤儿页面）

`DishCost.vue` 调用的全部端点：

| 前端调用 | HTTP | 后端是否存在 |
|---|---|---|
| `GET /dish-cost/dishes` | GET | ❌ 不存在 |
| `POST /dish-cost/dishes` | POST | ❌ 不存在 |
| `PUT /dish-cost/dishes/{id}` | PUT | ❌ 不存在 |
| `GET /dish-cost/recipe/{id}` | GET | ❌ 不存在 |
| `PUT /dish-cost/recipe/{id}` | PUT | ❌ 不存在 |
| `GET /dish-cost/ingredients?limit=` | GET | ❌ 不存在 |
| `POST /dish-cost/ingredients` | POST | ❌ 不存在 |
| `POST /dish-cost/upload` | POST | ❌ 不存在 |
| `DELETE /dish-cost/image/{name}` | DELETE | ❌ 不存在 |

证据：在 `inputs-r3`、`artifacts/team-worktrees/trae`（完整 checkout）、`F:\solo` 共享主干三处分别全量搜索 `dish-cost`，结果只有 `RecipeController`（注释）、`DishCostCalculator`、`KitchenSupplyService` 命中，**没有任何 Controller 用 `@RequestMapping` 声明 `/dish-cost` 前缀**。

### 5.1 附加精度问题（`DishCost.vue` 私算 vs 共享公式）

即使后端存在，`DishCost.vue` 的 `calcRow` 也违反了共享成本公式：

```js
function calcRow(row) {
  const r = 1 - (row.wastageRate || 0) / 100
  row.netUnitPrice = r > 0 ? (row.unitPrice || 0) / r : 0
  row.totalCost = Math.round((row.quantity || 0) * row.netUnitPrice * 100) / 100
}
```

对比后端 `DishCostCalculator.calculateLine`：
- 后端：`netPrice = price * 100 / (conversionRate * yieldRate)`，**必须查 conversion_rate（采购单位→使用单位换算）**
- 前端：`netUnitPrice = unitPrice / (1 - wastageRate)`，**完全不读 conversion_rate，也不读 yield_rate 做除数**，且用 JS `Math.round` 浮点（有精度误差），`unitPrice.toFixed(4)` 展示精度也低于后端 scale=8。

二者算出的"净料单价"和"共计"**数值口径不一致**。这是任务验收点「preserve price precision and explicit cost errors」直接命中的问题。

---

## 六、单位展示口径

| 字段 | CostRecipe.vue | DishCost.vue | 后端字段 |
|---|---|---|---|
| 单位 | 自由输入 `克/斤/个`（el-input） | 采购单位 purchaseUnit | IngredientMaster.purchaseUnit / usageUnit |
| 出成率 | 输入 0.01–999.99 | 输入 0–100（precision 1）| DishRecipe.yieldRate（scale 2），DishCostCalculator 上限 999.99 |
| 成本率 | `costRate.toFixed(1)%` | `costRate.toFixed(1)%` | DishMaster.costRate（scale 2）|

**不一致点**：出成率上限，前端 `DishCost.vue` 用 `:max="100"`，后端允许 >100（`DishCostCalculator` 注释明确"吸水原料实测出成率可>100%"）。`DishCost.vue` 的 `:max=100` 会**阻止合法的高出成率录入**。

---

## 七、前端候选修复 diff（仅针对"确认的不一致"，最小改动）

> 按任务约束：只改 `CostRecipe.vue` / `DishCost.vue` / restaurant-only API adapters；不改共享路由、user store、法务；保留价格精度与明确成本报错；未确认项只登记不改。

### 修复 1（高置信度）：补 `dishCostPreview.js`，或内联 `previewRecipeLine`

`CostRecipe.vue` 崩在缺失导入。最小修复有二选一：

- **方案 A**：在 `frontend_v3/src/utils/dishCostPreview.js` 新增 `previewRecipeLine`，其净料单价公式必须**对齐后端 `DishCostCalculator.calculateLine`**（读 conversion_rate、yield_rate，BigDecimal 语义）。这属于新增文件，不属于"改共享文件"。
- **方案 B**：在 `CostRecipe.vue` 内内联等价函数，删掉 import。

候选实现（方案 A，作为 diff 交付，不直接落盘共享区）：

```js
// frontend_v3/src/utils/dishCostPreview.js
// 与后端 com.youjian.banquet.service.DishCostCalculator.calculateLine 对齐
export function previewRecipeLine(row, ingredient) {
  if (!ingredient) return { error: '请选择原料' }
  const qty = Number(row.quantity)
  if (!(qty > 0)) return { error: '用量必须大于0' }
  const price = Number(ingredient.unitPrice ?? ingredient.avgPrice)
  if (!(price >= 0)) return { error: '原料缺少有效采购单价' }

  const purchaseUnit = (ingredient.purchaseUnit || ingredient.unit || '').trim()
  const usageUnit = (ingredient.usageUnit || purchaseUnit).trim()
  const recipeUnit = (row.unit || usageUnit).trim()
  if (!purchaseUnit || !recipeUnit) return { error: '缺采购单位或使用单位' }

  let conversion = 1
  if (recipeUnit === purchaseUnit) {
    conversion = 1
  } else if (recipeUnit === usageUnit) {
    conversion = Number(ingredient.conversionRate)
    if (!(conversion > 0)) return { error: '采购/配方单位不同，需有效换算率' }
  } else {
    return { error: '配方单位与原料档案不一致' }
  }

  let yieldRate = Number(row.yieldRate)
  if (!(yieldRate > 0)) yieldRate = Number(ingredient.yieldRate)
  if (!(yieldRate > 0) || yieldRate > 999.99) return { error: '出成率无效' }

  // 与后端一致：netPrice = price * 100 / (conversion * yield)
  const net = (price * 100) / (conversion * yieldRate)
  const netUnitPrice = roundTo(net, 8)
  const totalCost = roundTo(net * qty, 4)
  return { netUnitPrice, totalCost }
}

function roundTo(v, scale) {
  const m = Math.pow(10, scale)
  return Math.round(v * m) / m
}
```

> 注意：JS `Number` 仍有浮点误差，无法 100% 复刻后端 BigDecimal。这里**明确标注**：前端预览只用于展示，最终落库以后端 `dish_recipe` 的 BigDecimal 字段为准（保存时后端会用 `DishCostCalculator` 重算并覆盖）。这与验收点「preserve price precision」一致——前端不做持久化算术决策。

### 修复 2（登记，不改）：`DishCost.vue` 后端缺失

`/dish-cost/*` 无后端是**后端缺失问题，超出 Trae 前端字段修复范围**，也不属于本次"小 diff"能解决。按任务边界：**登记为未覆盖项 + 风险，交 Codex 决策**（见 §8）。Trae 不做"虚构端点"补丁。

### 修复 3（候选，低风险）：`DishCost.vue` 出成率上限

`<el-input-number v-model="row.yieldRate" ... :max="100">` 改为 `:max="999.99"`，与后端 `DishCostCalculator` 上限一致。属 `DishCost.vue` 允许路径。但既然该页面后端整体缺失（§5），此 diff 暂不单独交付，等 Codex 对 DishCost.vue 的处置结论后一并处理。

---

## 八、未覆盖项 / 风险 / 下一步

1. **`GET /cost/ranking` 未核对**：`CostRecipe.vue` 依赖，但 `CostController` 不在输入包。需 Codex 确认该端点是否存在及字段（dishId/dishName/category/salePrice/costPrice/costRate）。
2. **`DishCost.vue` 后端缺失**是最严重问题，需 Codex 决策：是补齐 `/dish-cost` 后端、还是废弃该入口统一到 `/recipes/*`。
3. **前端浮点精度**：JS `Number` 无法完全复刻 BigDecimal，已用"前端只展示、后端落库重算"策略规避，需 Codex 确认此口径可接受。
4. **`dish_cost_preview` 缺文件**已定位，修复 1 待 Codex 确认方案 A/B 后落盘。

## 九、测试证据

本任务为**只读契约比对 + 候选 diff**，未改动任何共享文件，未跑 mvn 构建（后端无改动）。上述"后端缺失"结论基于三处全量 `Select-String` 搜索证据，非推断。前端 `dishCostPreview.js` 缺失结论基于 worktree 完整 checkout 的 `Get-ChildItem` 证据。

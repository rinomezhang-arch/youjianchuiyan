# TR-R3-CONTRACT 路由/API/字段比对表（Trae）

基线：codex/trae-cost-ui @ 637b08b8257552a141a659407838dbc6b9260379
依据：任务输入包 inputs-r3.zip（SHA256 已核对一致）+ 工作树同基线后端源码。
结论：**确认不一致 13 处**（DishCost.vue 10、CostRecipe.vue 2、基线级 1）。未确认的问题只登记不改动。

## 一、两个成本入口概况

| 项 | CostRecipe.vue（/cost-recipe 成本配方） | DishCost.vue（/finance/dish-cost 菜品成本管理） |
|---|---|---|
| 数据来源 | 真实接口：/cost/ranking、/recipes/*、/ingredients | **幽灵接口族 /dish-cost/**（后端不存在） |
| 成本公式 | utils/dishCostPreview.js（与 DishCostCalculator 同式） | 本地 calcRow（用 wastageRate，忽略 yieldRate，且无换算率/精度口径） |
| 店铺隔离 | 仅 GET /ingredients 带 storeId，POST 漏带 | 全部不带（GET 靠拦截器兜底，POST 全漏） |

## 二、DishCost.vue 确认不一致（严重→轻）

| # | 前端调用/写法 | 后端真实契约（证据） | 影响 |
|---|---|---|---|
| D1 | GET /dish-cost/dishes | 不存在。真实 GET /api/cost/ranking（CostController:143，content 内 dishId/dishName/**category**/salePrice/costPrice/costRate/marginRate/profit/unit） | 列表整页 404；字段名 category≠dishCategory |
| D2 | GET /dish-cost/recipe/{dishId} | 不存在。真实 GET /api/recipes/{dishId}?storeId（RecipeController:37）返回 DishRecipe：ingredientName/unit/unitPrice/quantity/wastageRate/yieldRate/netUnitPrice/totalCost/lastEntryDate 全齐 | 配方回显 404 |
| D3 | PUT /dish-cost/recipe/{dishId}（保存/清空删除） | 不存在。真实 POST /api/recipes/{dishId}?storeId（GM 专用，403 文案明确） | 保存 404 |
| D4 | POST /dish-cost/ingredients（snake_case：ingredient_name/avg_price…） | 不存在。真实 POST /api/ingredients（IngredientDTO camelCase，**storeId 必填** Long.parseLong） | 建原料 404 |
| D5 | GET /dish-cost/ingredients?limit=2000（取 avgPrice/minPrice） | 不存在。真实 GET /api/ingredients?storeId（必传）返回 IngredientDTO：unitPrice/purchaseUnit/usageUnit/conversionRate/yieldRate；**无 avgPrice/minPrice/lastEntryDate** | 原料下拉 404 |
| D6 | POST /dish-cost/upload | 不存在。真实 POST /api/upload/image（UploadController:24，multipart "file" → Result.data.url） | 传图 404 |
| D7 | DELETE /dish-cost/image/{f} | **无真实删除端点**（不虚构）。仅前端清空引用 | 登记不改 |
| D8 | 新建/编辑菜肴 POST/PUT /dish-cost/dishes（snake_case） | 不存在。真实 POST /api/dishes、PUT /api/dishes/{dishId}?storeId（DishDTO camelCase；createDish **storeId 必填、status:"active" 才可见**；updateDish null-safe） | 建菜 404 |
| D9 | 售价输入 :precision="0" | dish_master.sale_price DECIMAL(10,2)（DishMaster.java:57） | 38.5 被截成 38 |
| D10 | quantity 自由 el-input 无小数限制；yieldRate max=100；calcRow=unitPrice/(1−wastage/100) | DishCostCalculator:17 用量>0 且 ≤3 位小数；出成率可>100%（水发）、≤999.99，公式 netPrice=price×100/(conversion×yield) scale8，行成本 scale4 | 4 位小数被后端明确拒绝；>100% 出成率录不进；本地预览与后端口径脱节 |

## 三、CostRecipe.vue 确认不一致

| # | 前端写法 | 后端真实契约 | 影响 |
|---|---|---|---|
| C1 | POST /recipes/{dishId} 未传 storeId | RecipeController:51 defaultValue="1"；request.js 只对 GET 自动注入 storeId | 总经理在门店2保存配方会写进门店1（多租户串店） |
| C2 | 配方单位是自由文本 el-input（占位"克/斤/个"） | DishCostCalculator:30 配方单位必须等于原料 purchaseUnit 或 usageUnit，否则明确报错 | 自由文本几乎必错，保存即 500 |

## 四、依赖与基线说明（勘误）

| # | 说明 | 证据 |
|---|---|---|
| B1（勘误） | ~~基线构建断~~ 结论撤回：基线 CostRecipe.vue **不**引用 dishCostPreview.js，可独立构建。是任务输入快照内的 CostRecipe.vue（=F:\solo 未提交修复版）引用了它，而快照未包含该文件。该修复文件真实存在于 F:\solo\frontend_v3\src\utils\dishCostPreview.js，与 DishCostCalculator 公式逐行同构。**本任务候选补丁中 DishCost.vue 同样引用该 util，属共享前置依赖，须随 CO-R3-BASE/整合先落基线**（此依赖文件不在我 allowed_paths，未提交，仅作未跟踪 scratch 供构建验证） | 基线文件无该 import；inputs-r3.zip 文件清单无该文件；F:\solo Test-Path=True |
| B2 | 未提交修复（F:\solo 主工作区）已改 CostRecipe.vue 的预览公式/校验/出成率列（20+/29-），与本补丁改动区域部分相邻；整合时如遇冲突，以「双方改动均保留」为准：补丁改动=单位下拉（C2）、onIngredientPick 单位对齐（与修复版同文，应无冲突）、saveRecipe 显式 storeId（C1，追加第三参数） | git diff --no-index 基线 vs 快照 |

## 五、登记不改动（超出"确认不一致"最小修复范围）

1. CostRecipe/DishCost 保存配方后走 POST /recipes/recalc-all（重算全部菜品）；存在单菜品真实端点 POST /api/kitchen-supply/cost-cards/calculate——DishCost 候选补丁采用单菜端点，CostRecipe 维持现状待统筹定夺。
2. IngredientDTO 无 lastEntryDate/updatedAt → DishCost 原料下拉"录入日期"恒为 "-"。
3. DishCost 弹窗宽 1100px，不符合 50vw 移动端偏好；属 UI 改造，另行任务。
4. /cost/ranking SQL 异常时 catch 返回空 content（静默空列表）——后端文件，不在 allowed_paths。
5. 菜品删除成本卡后 dish_master.cost_price/cost_rate 不回退（保留历史值）——业务口径待统筹确认。

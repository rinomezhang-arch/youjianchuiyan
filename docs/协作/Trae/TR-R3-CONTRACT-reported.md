# TR-R3-CONTRACT reported 报告（Trae）

- 提交号：c42541d3（分支 codex/trae-cost-ui，已推送 origin：637b08b..c42541d3）
- 基线：637b08b8257552a141a659407838dbc6b9260379（任务指定 base，未夹带未提交修复）
- 变更登记：docs/变更登记簿.md CHANGE-20260907-TR01（意图+结果）

## 改动文件（共 2 个代码文件 + 3 个文档）

1. frontend_v3/src/views/dashboard/CostRecipe.vue
   - C1 修复：saveRecipe 的 POST /recipes/{dishId} 显式追加 params.storeId=currentStoreId（request.js 仅对 GET 自动注入；修复总经理在门店2保存配方写进门店1 的串店缺陷，RecipeController:51 defaultValue="1" 证据）。
   - C2 修复：配方"单位"由自由文本改为原料档案单位下拉（usageUnit/purchaseUnit ∪ 当前行值）；onIngredientPick 默认单位对齐 usageUnit||purchaseUnit（与主工作区未提交修复同文，三方合并应无冲突）。
2. frontend_v3/src/views/dashboard/DishCost.vue（幽灵接口族全量重连，未虚构任何端点）
   - 列表：GET /dish-cost/dishes → GET /cost/ranking?size=500（CostController:143），字段 category→dishCategory 映射。
   - 回显：GET /dish-cost/recipe/{id} → GET /recipes/{id}?storeId（并行 GET /dishes/{id}?storeId 补 DishDTO 全字段，供 PUT 不丢字段）。
   - 保存：PUT /dish-cost/recipe/{id} → POST /recipes/{id}?storeId（GM 专用 403 文案保留）；保存后 POST /kitchen-supply/cost-cards/calculate 单菜重算（替代 recalc-all 全量重算）。
   - 删除：PUT 空数组 → POST /recipes/{id}?storeId + []。
   - 建菜/改菜：POST/PUT /dish-cost/dishes（snake_case）→ POST /dishes（含 storeId + status:'active'，createDish 硬性要求）/ PUT /dishes/{id}?storeId（null-safe 部分更新已核实 DishService:73-91）。
   - 建原料：POST /dish-cost/ingredients（snake_case）→ POST /ingredients（IngredientDTO camelCase，storeId 必填，status:'active'）。
   - 原料下拉：GET /dish-cost/ingredients → GET /ingredients?storeId；avgPrice/minPrice→unitPrice（DTO 无 min/lastEntryDate，"录入日期"列随之移除，"最低价"改"入库价"）。
   - 传图：POST /dish-cost/upload → POST /upload/image（UploadController:24）；删除图片：无真实端点，改为仅清引用并提示（不虚构 DELETE）。
   - 精度：售价 :precision 0→2（dish_master.sale_price DECIMAL(10,2)）；用量 el-input→el-input-number :precision="3" :min="0"（DishCostCalculator ≤3 位小数）；出成率 :max 100→999.99 :precision 2（水发>100% 合法）；本地净料单价/小计改用 previewRecipeLine（与 DishCostCalculator 同式：price×100/(conversion×yield)，净料8位、行成本4位）；成本率 null/0 显示"未核算"；保存前逐行显式校验（_error 直出，后端明确报错经拦截器原样弹出，不再叠加"保存失败"）。
3. docs/协作/Trae/：started 计划、对比表（13 处确认不一致+勘误+不改动清单）、契约比对报告（先前被中断运行的存档，一并入库）。

## 测试证据

- 输入包核验：inputs-r3.zip SHA256=12c674ea05a1dc5a401f69b0e2a1507df625eea6caf1999e3d0679d0e8332977，与任务 JSON 一致。
- 构建：npm run build 通过（1m17s，DishCost-DUO29d94.js 22.68kB 正常产出；node_modules 为指向主工作区的 Junction，未提交）。
- 端点存在性：所有新调用端点均逐一在后端源码定位（CostController/RecipeController/DishController/IngredientController/KitchenSupplyController/UploadController），无虚构。
- 并发核查：提交前后 git status 干净，无他人并发改动被覆盖。

## 未完成项 / 需 Codex 决策（按严重度）

1. **手机交互验证未执行**：本地 8080/3306/5173 均未运行，起完整栈超出本任务边界；发布类验证需统筹批准。已用构建+静态约束核查替代，未标通过。
2. **DishCost.vue 重连方案与先前中断运行报告的口径差异**：先前报告倾向"后端缺失仅登记不改"；本补丁按任务"提供精确接口修复候选"选择了重连真实端点。两方案冲突时请 Codex 裁决；若裁决不采纳重连，revert c42541d3 中 DishCost.vue 部分即可。
3. **共享前置依赖**：DishCost.vue import @/utils/dishCostPreview（主工作区未提交修复文件，不在我 allowed_paths，未随补丁提交）。整合顺序要求：先落 dishCostPreview.js 进基线，再合本补丁，否则构建断。
4. CostRecipe 保存后仍走 /recipes/recalc-all（全量重算）；是否切换单菜 cost-cards/calculate 待统筹定夺。
5. /cost/ranking 对 SQL 异常静默返回空 content（后端 catch），属后端文件，超出 allowed_paths，仅登记。
6. DishCost 弹窗宽 1100px 不符 50vw 移动端偏好；另 numeric 控件 controls-position="right" 与工作区约定（-左+右）不符——均属 UI 任务，本补丁未动。

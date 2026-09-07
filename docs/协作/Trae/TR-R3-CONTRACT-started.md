# TR-R3-CONTRACT started 计划（Trae）

- 任务：核对两个菜肴成本入口（CostRecipe.vue / DishCost.vue）并提供精确接口修复候选。
- 领取回执：CLAIM_VERIFIED=TR-R3-CONTRACT（2026-09-07，COS 任务板原子领取）。
- 输入核验：assets/inputs-r3.zip SHA256=12c674ea05a1dc5a401f69b0e2a1507df625eea6caf1999e3d0679d0e8332977，与任务 JSON 一致；已解压至 artifacts/team-worktrees/trae/.inputs-r3。
- 基线：分支 codex/trae-cost-ui @ 637b08b8257552a141a659407838dbc6b9260379，工作树干净。

## 步骤

1. 读输入包内 CostRecipe.vue、DishCost.vue 与后端契约（RecipeController、DishCostCalculator、实体/仓库），列出两条入口各自调用的路由、HTTP 方法、字段名、精度处理、单位展示。
2. 逐项比对：路径是否为真实存在的后端端点（不虚构端点）、请求/响应字段名、价格/成本精度（BigDecimal 序列化、小数位）、单位（克/份/%）展示口径。
3. 仅对"确认的不一致"在 CostRecipe.vue / DishCost.vue / restaurant-only API adapters 内做最小候选修复；保留价格精度与明确成本报错，不改共享路由与鉴权 store。
4. 手机交互验证：npm build + 本地预览，窄屏检查两个入口的可用性（输入高度、底部抽屉、无图标、单位可见），截图存 f:\solo\screenshots\。
5. 证据：比对表、diff、测试命令与结果写 docs/协作/Trae/，变更登记簿追加结果事件，任务板 reported 回报。

## 边界

不改协作台、共享鉴权、部署脚本、法务；不物理删除；不写生产；未确认的问题只登记不改动。

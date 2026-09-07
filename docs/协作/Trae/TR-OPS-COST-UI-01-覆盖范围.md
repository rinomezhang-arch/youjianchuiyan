# TR-OPS-COST-UI-01 覆盖范围与证据（Trae，2026-09-07）

任务：成本页面浏览器验证与手机操作证据（owner=trae，base=5bd75305）
结论先行：**真实环境 E2E 已执行（非 mock）**。核心验收通过；2 项未完全闭环（UI 失败 toast、精确 390px），如实登记原因与待办。

## 一、环境（全隔离，非 mock）

| 项 | 值 |
|---|---|
| 前端 | 工作树 `frontend_v3`，Vite dev，http://localhost:5174（日志 `docs/协作/Trae/vite-5174.log`） |
| 后端 | 工作树 `banquet_project`（base 5bd75305 打包 `banquet-1.0.0.jar`），prod profile，8080 |
| 数据库 | 隔离 MySQL 127.0.0.1:13317 / 库 `trae_cost_e2e_20260907`（root 空密码；未动 Start-Service、未用生产凭证、未碰其他协作方的库） |
| 数据 | `scripts/trae-cost-e2e-seed.sql` 全部 `TR验收_` 合成数据：1 员工（trae_test/002323，gm，store 1）、1 门店、3 原料、3 菜品、配方 |
| 后端日志 | `banquet_project/logs/trae-e2e-8080.log`（Started 54.8s，无 ERROR） |

种子数据单位语义（依 RecipeController.recalcAll 净价公式校准）：
`unit_price=采购单位价`，净价=unit_price/(conversion_rate×yield_rate%)。
例：五花肉 36 元/千克 ÷ (1000×90%) = 0.04 元/克。

## 二、浏览器级验证（browser-use 三轮 + 截图 8 张）

截图已转存 `f:\solo\screenshots\`：trae-cost-01-login / 02-old-entry-redirect / 03-list-nonempty / 04-recipe-dialog / 06-mobile-save-success / 07-mobile-recipe-readback / 08-mobile-save-fail / 08b-save-fail-toast（.png）。

| # | 断言 | 结果 | 证据 |
|---|---|---|---|
| B1 | 登录（trae_test/002323）→ 跳转主界面 | PASS | 01 截图 + 跳转 /dashboard/home |
| B2 | 旧菜品成本入口（/dashboard/finance/dish-cost）打开后渲染「成本配方 · Cost Recipe」 | PASS | 02/03 截图 + pathname 断言 |
| B3 | **网络请求零 `/dish-cost/*`**（页面数据全部来自 /api/cost/ranking、/api/recipes/*、/api/ingredients、/api/auth/me、/api/ai/models） | PASS | browser_network_requests 全量清单，计数=0（两轮复查） |
| B4 | 列表非空：TR验收_红烧肉 ¥58/有配方、酸辣土豆丝 ¥18/有配方、清蒸鲈鱼 ¥88/无配方 | PASS | 03 截图 |
| B5 | 配方弹窗：红烧肉 2 行（五花肉 500 克、生抽 20 毫升），单位列为 el-select 下拉（非自由文本） | PASS | 04 截图 |
| B6 | 添加原料：选「五花肉」→ 单位自动带出「克」；保存 → 成功链路（状态 无配方→有配方、无配方统计 1→0、弹窗自动关闭） | PASS | 06 截图 |
| B7 | 成功回读：重开清蒸鲈鱼弹窗 → 1 行五花肉/200/克 | PASS | 07 截图 |

说明：入口真实路径为 `/dashboard/finance/dish-cost`（仪表盘子路由）；顶层 `/finance/dish-cost` 本就不存在（404），菜单入口走的是子路由。

## 三、接口级+数据库级验证（可复跑脚本，12/12 PASS）

脚本：`scripts/trae-cost-verify.ps1`（幂等可重跑；只许指向隔离库）。
最近一次输出关键行：

```
[PASS] login | code=200 storeId=1 user=TR验收_测试员 role=gm
[PASS] ingredients TR non-empty | count=3
[PASS] cost/ranking TR dishes | total=3 trRows=3
[PASS] recipe detail TR-DISH-001 | items=2 五花肉/500.000/克
[PASS] save success | code=200
[PASS] recalc costPrice refreshed | 清蒸鲈鱼 costPrice=20.00 costRate=22.7
[PASS] save failure explicit message | http=200 code=500 msg=保存配方失败: ...Data truncation: Out of range value for column 'quantity'...
[PASS] rollback keeps prior recipe | before=[c,1] after=[c,1]
=== RESULT: PASS=12 FAIL=0 ===
```

要点：quantity=99999999 超 decimal(10,3) 上限 → 后端事务回滚（@Transactional），库中既有配方保留；信封 code=500 且 message 以「保存配方失败」开头（显式文案）。

## 四、未完全闭环项（如实登记，不标通过）

1. **UI 保存失败 toast 未能捕获**（验收项「保存失败提示」的浏览器侧证据缺失）
   - 已证：探针（XHR 包装）记录到 POST /api/recipes/TR-DISH-003 返回 HTTP 200 + code=500 + 显式 message；axios 拦截器（src/utils/request.js L87 `ElMessage.error(res.message)`）在代码上应弹错。
   - 矛盾：8 秒内 10 次采样 DOM 中 `.el-message` 恒为 0；第三轮甚至出现「取消」按钮（静态 `@click="showRecipeDialog=false"`）无响应——说明该浏览器会话存在死浮层/僵尸 DOM，交互层不可信，无法把「无 toast」归因到应用代码。
   - 待办：干净浏览器会话复测失败 toast；若仍无提示，则为真实前端缺陷，报 Codex 排查（请求层与回滚已有接口级证据兜底）。
2. **精确 390px 视口未达成**：工具 `window.open` 尺寸被钳制，实测 innerWidth=558。558px 下 B6/B7 全流程通过（截图 06/07）。精确 390px 待浏览器工具支持视口设置后补测。
3. 截图 06/07 拍摄于旧种子值（单价 ¥0.04）；种子现已修正为采购单位语义（原料档 36 元/千克，净价 0.04 元/克），当前库与脚本一致，页面展示逻辑不变。

## 五、移交 Codex 的观察（均不在本任务 allowed_paths，未改动）

1. **后端存在全局兜底**：任意未知 `/api/*` 路径（如 /api/definitely-not-real）返回 HTTP 200 + `{code:200,data:[]}`。这正是旧 DishCost.vue「静默空列表」的土壤；建议改为 404 信封或至少非 200 code。兜底处理器未在 controller/ 与 config/ 源码中定位到（rg 全查无 dish-cost、无 {**} 通配），建议 Codex 用启动日志/HandlerMapping 全量排查定位。
2. **CostRecipe.vue onIngredientPick 单价语义混用**：选原料时把 `ingredient_master.unit_price`（采购单位价，如 36 元/千克）直接写入行 `unitPrice` 并与使用单位数量相乘展示小计（保存前会显示 500×36=18000 元的虚高小计）；保存触发 recalc-all 后由后端净价公式校正。建议前端改为预览净价或标注「采购单位价」。

## 六、复现步骤

1. `mysql -h 127.0.0.1 -P 13317 -u root -e "source F:/solo/artifacts/team-worktrees/trae/scripts/trae-cost-e2e-seed.sql"`
2. 后端：`cd artifacts/team-worktrees/trae/banquet_project`，env：`SPRING_PROFILES_ACTIVE=prod`、`SPRING_DATASOURCE_URL=jdbc:mysql://127.0.0.1:13317/trae_cost_e2e_20260907?...`、`SPRING_DATASOURCE_USERNAME=root`、`SPRING_DATASOURCE_PASSWORD=`、`JWT_SECRET=<32字节>`、`AES_SECRET_KEY=<32字节>`、`APP_NOTIFY_ENABLED=false`，`java -jar target/banquet-1.0.0.jar`
3. 前端：`cd artifacts/team-worktrees/trae/frontend_v3 && npm run dev -- --port 5174 --strictPort`
4. 断言脚本：`powershell -ExecutionPolicy Bypass -File artifacts/team-worktrees/trae/scripts/trae-cost-verify.ps1`
5. 浏览器：登录 trae_test/002323 → 菜单「菜品成本」→ 断言 B2–B7。

# TR-R3-CONTRACT 第三轮报告（Trae · 响应 Codex 事件5）

- 响应：events/TR-R3-CONTRACT/00000005.json（Codex 20260907T113736Z，"确定执行方案，立即继续"）
- started 回执：已即时提交（EVENT_VERIFIED=started）
- 分支 codex/trae-cost-ui 新提交：**5bd75305**（已推送 origin：01862cdc..5bd75305）

## 实施内容（按授权边界）

| 文件 | 改动 | 边界核对 |
|---|---|---|
| frontend_v3/src/router/index.js | 仅第 62 行：`finance/dish-cost` 路由 component `DishCost.vue`→`CostRecipe.vue` | **Codex 明确授权的本任务例外**；path=`finance/dish-cost`、name=`FinanceDishCost`、meta 全部保留；其余 60+ 条路由零改动（`diff --cached --stat`：router 仅 2 行=1改） |
| frontend_v3/src/views/dashboard/DishCost.vue | 恢复基线原文（撤销 6f5adf20 的组件内重定向 shim，路由方案下已冗余） | 文件保留不删；不再被任何路由挂载；`/dish-cost/*` 幽灵接口族从可达路径上彻底消失 |
| frontend_v3/src/views/dashboard/CostRecipe.vue | 保存成功提示兼容 Claude 候选 bb3d4018 的新响应形态 | 防御性写法，新旧两种 data 形态通吃（见下） |

## 指令核对：Claude 候选 bb3d4018 兼容性（只读，未假称已发布）

- `POST /recipes/{dishId}` 成功 data：旧版为字符串 `"配方保存成功"`；bb3d4018 版本化后为对象 `{revisionId, versionNo, itemCount, totalCost, message:"配方保存成功，已存为第 N 版"}`。
- CostRecipe.vue 保存调用本就不消费响应体 → 兼容；现已升级为 `ElMessage.success((res.data && typeof res.data==='object' && res.data.message) || '配方保存成功')`——旧版回退默认文案，新版显示版本化提示，**两种后端形态均可运行**。
- `GET /recipes/{dishId}`：bb3d4018 的 `activeRecipe` 仍返回 `List<DishRecipe>`（RecipeRevisionService.java，`findByDishIdAndStoreIdAndIsActiveOrderBySortOrderAsc`）→ 前端 `res.data?.length` 解析不受影响。
- 附加核对：`POST /recipes/recalc-all` 无 storeId 参数属设计——后端 `dishRepo.findAll()` 遍历所有门店各自重算（GM 专用 403 守卫），无跨店缺口。
- 新增追溯端点 `/recipes/{dishId}/revisions`、`/recipes/revisions/{revisionId}/items`：CostRecipe 未接入（超本任务范围，登记给后续 UI 任务）。

## 验证证据

- `npm run build` 通过（18.81s 增量构建；动态 import 的 CostRecipe.vue 路径解析成功由构建成功背书）。
- 路由核对：`FinanceDishCost` 名称保留 → Dashboard 菜单（Dashboard.vue:277 按 path 跳转）与任何按 name 的导航均不受影响；同一组件现挂载于 `/dashboard/cost-recipe` 与 `/dashboard/finance/dish-cost` 两路径。
- 工作树并发披露：docs/协作/Trae/TR-R3-CONTRACT_契约比对报告.md 存在另一实例的未提交修改，本人未触碰、未提交、未回退（其归属由统筹裁定）。

## 未执行项（如实登记，不标通过）

1. **真实 E2E（登录→菜单→页面请求→非空数据→窄屏截图）仍未执行**：本地 MySQL84 服务启动需管理员提权（Start-Service 被拒），Docker 守护进程未运行，youjian-docker 缺 compose 文件。本轮已交付：构建级验证 + 全部调用的静态契约证据（端点/字段/响应形态逐一在后端源码定位）。与 mock 的区分：本补丁零 mock、零虚构端点；被挂载页面（CostRecipe）全部调用真实后端端点。
2. recalc-all 全量重算的性能与单菜化（cost-cards/calculate）仍待统筹另立任务。

## 请 Codex 验收

以 5bd75305 为准（含 6f5adf20/01862cd 历史轨迹）。若验收通过，主工作区路由集成（同一行 component 替换）由 Codex 执行。

# TR-OPS-COST-MOBILE-02 验收报告（Trae，2026-09-07）

## 结论
390px 手机视口与 1280px 桌面视口双跑 **52/52 PASS**。旧截图 trae-cost-pw-03 的弹窗左侧裁切（标题/原料/用量列在屏外）已修复：所有关键控件 boundingRect 均在视口内，宽表格改为弹窗/列表内**明确局部横向滚动**，页面与弹窗不再整体偏出屏幕。

- 工作树：F:\solo\artifacts\team-worktrees\trae-mobile-cost（独立新建，旧 trae 工作树保留）
- 分支：codex/trae-mobile-cost；基线 a94cc043e702e25578348cc287afb1ca92afc1bd
- 被测前端：整合版 a94cc043 + 本任务 CostRecipe.vue 移动适配；dishCostPreview.js 与 /api 契约**未改**
- 被测服务：整合版后端独立打包起 **8081**（dev profile + 环境变量注入隔离库 13317、JWT/AES 测试密钥），vite dev **5175**；未冒用旧 5174/8080（5bd 基线服务）
- 复用说明：node_modules 目录联接复用旧工作树已装依赖（两树 package.json 零 diff），Playwright 复用旧树已装包，**零新装依赖**

## 改动文件（仅允许路径）
1. `frontend_v3/src/views/dashboard/CostRecipe.vue`
   - 模板：el-dialog 增加专属 class `cost-recipe-dialog`（1 行，逻辑零改动）
   - scoped 样式新增 ≤768px 媒体查询：页头纵向、搜索/按钮全宽、统计卡 2 列、`.cost-table-wrapper` 改 overflow-x:auto（原 overflow:hidden 是裁切根因）、触控目标 min-height 36-40px、配方信息换行
   - 新增第二个**全局 style 块**（el-dialog teleport 到 body，scoped 后代选择器不跨 teleport）：全部规则限定在 `.cost-recipe-dialog` 专属前缀下，弹窗宽 calc(100vw-16px) !important 覆盖 inline 800px、header/body/footer padding 收紧、标题换行、footer 按钮 flex 等分 min-height 40px、弹窗内 el-table 局部横滚
   - 桌面（>768px）样式与逻辑完全不变
2. `scripts/trae-cost-mobile/mobile-acceptance.mjs`：双视口 Playwright 验收脚本（可复跑）
3. `docs/协作/Trae/cost-mobile/`：本报告与 started 记录

## 验收断言（52 项全绿，摘证）
- **390px boundingRect 全在视口**：弹窗 [8,382]、标题 [48,302]、关闭 X [334,382]、原料下拉 [60,216]、用量输入 [255,305]、添加原料按钮 [48,342]、取消 [38,190]、保存 [200,352]；页头/搜索/统计卡/列表编辑按钮同视口内
- **桌面 1280 不退化**：弹窗 [240,1040] 居中 800px，全部控件在视口
- 流程（两视口均跑）：打开列表（3 道 TR菜非空）→ 打开弹窗（标题「配方编辑 - TR验收_清蒸鲈鱼」）→ 增删行 1→2→1 → 失败保存（生抽 99999999）toast 可见「保存配方失败: ...Data truncation: Out of range value for column 'quantity'」、弹窗保留、输入 99999999.000 保留 → 取消关闭 → 重开回读旧版本 500.000（回滚可见）→ 改 300 保存 toast「配方已保存，成本已重新核算」→ 重开回读 300.000
- **DB 断言**（隔离库 trae_cost_e2e_20260907）：失败生抽行 is_active=1 计数 0（未落库）；生效行 1 条五花肉 300.000/12.0000；历史行 is_active=0 保留 2 条（整合版版本化设计）；recipe_revision 2 个版本（version_no 1、2，created_by=trae_test，total_cost=12.0000）
- 网络：零 /dish-cost/ 调用（14 个 /api/ 调用均为真实接口）
- 局部横滚证据：390px 弹窗表格 scrollWidth=294=clientWidth（首屏可见原料/用量列，单位/出成率/净料单价/小计/操作在右向横滚区）；列表表格同理

## 截图（f:\solo\screenshots\）
- trae-mobile-m-01-list.png / m-02-dialog.png / m-03-fail.png / m-04-readback.png（390px）
- trae-mobile-d-01-list.png / d-02-dialog.png / d-03-fail.png / d-04-readback.png（1280px）

## 超范围壳层证据（未改，按要求只报）
1. **侧边栏 390px 自动收为 60px 图标条**：`aside.sidebar` 实测 w=60/left=0，`.main-content` w=330/left=60（桌面 w=210）。这是 Dashboard 既有响应式行为，内容区可用；侧边栏竖排文字（工作台/桌台看板/菜品成本…）在 60px 下逐字排列，属壳层表现，未改 Dashboard.vue。
2. **顶部 header 在 390px 挤压换行**：门店切换「TR验收_烟私房菜」、铃铛、User 头像与标题文字换行重叠（见 m-01 截图顶部），节点在 Dashboard 壳层，非本页范围。
3. 列表「菜品名称」列 min-width 180 在 330px 内容区需横滚看全名（表格局部横滚，验收明示允许）。

## 测试库适配（隔离库 DDL/数据，非生产、未重跑破坏性种子）
整合版 a94cc043 实体比 0906 schema 新，隔离库按需对齐：
- dish_recipe 补 `is_active` tinyint、`revision_id` bigint（对应官方迁移 scripts/migrations/recipe_revision_v1.sql）
- 新建 `recipe_revision` 表（同上迁移 DDL）
- 修正 TR 种子数据：配方行 yield_rate 由 0/NULL 修为档案值（五花肉90/土豆85/生抽100）、生抽档案 yield_rate=100；配方行 unit 字面 `?`（HEX 3F，早期种子字符集丢失）修为 克/毫升（HEX E5858B / E6AFB2E58D87）——不修正则整合版 dishCostPreview 严格单位/出成率校验会拦截保存（「配方单位与原料档案不一致」「请填写有效出成率」），属测试数据问题而非本页缺陷

## 未覆盖/说明
- 未做真机触摸/手势测试（无真机环境）；Playwright 视口与触控目标尺寸断言替代
- vite.config.js proxy 仍指 8080（禁改），脚本用 Playwright route.fetch 服务端转发 /api/ 到 8081 并挂白名单 Origin，浏览器侧全程同源 5175
- 401 console 为登录前 /api/ai/models 竞态，与旧版一致，无业务影响

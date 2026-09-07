# TR-OPS-COST-MOBILE-02 reported（Trae，2026-09-07）

## 结论
完成，**390px + 1280px 双视口 Playwright 52/52 PASS**。旧截图弹窗左侧裁切（标题/原料/用量在屏外）已修复：关键控件 boundingRect 全部在视口内，宽表格改为局部横向滚动，页面与弹窗不整体偏出屏幕；桌面视觉零退化。

## 坐标
- 任务：TR-OPS-COST-MOBILE-02（started → reported）
- 工作树：F:\solo\artifacts\team-worktrees\trae-mobile-cost（独立新建，旧 trae 树保留）
- 分支：codex/trae-mobile-cost（已推送，新分支）
- 基线：a94cc043e702e25578348cc287afb1ca92afc1bd
- 交付提交：**0a295102**【Trae】TR-OPS-COST-MOBILE-02：CostRecipe手机布局修复…

## 改动文件（严格在 allowed_paths）
1. frontend_v3/src/views/dashboard/CostRecipe.vue
   - 模板仅 1 行：el-dialog 加专属 class="cost-recipe-dialog"，业务逻辑/契约零改动
   - scoped 新增 ≤768px 媒体查询：页头纵向、搜索框与「重新核算全部」全宽、统计卡 4→2 列、.cost-table-wrapper 由 overflow:hidden 改 overflow-x:auto（列表裁切根因）、触控目标 min-height 36-40px、配方信息换行
   - 新增全局 style 块（dialog teleport 到 body，scoped 后代选择器不跨 teleport）：规则全部限定 .cost-recipe-dialog 前缀——弹窗 width calc(100vw-16px) !important 覆盖 inline 800px、标题换行、footer 按钮 flex 等分 40px、弹窗内表格局部横滚；>768px 桌面完全不受影响
2. scripts/trae-cost-mobile/mobile-acceptance.mjs：双视口可复跑验收脚本
3. docs/协作/Trae/cost-mobile/：started 记录 + 验收报告
- 未改：Dashboard.vue/global CSS/router/request.js/dishCostPreview.js/其他 AI 页面/法务/后端/生产/配置；零新装依赖（node_modules 目录联接复用、Playwright 复用旧树包，两树 package.json 零 diff）

## 服务（未冒用旧 5174/8080）
整合版后端独立 mvn 打包起 **8081**（dev profile + 环境变量注入 13317 隔离库、JWT/AES 测试密钥）；整合版 vite 起 **5175**；脚本经 Playwright route.fetch 服务端转发 /api/→8081（同源、挂白名单 Origin），全程真实后端 E2E 非 mock。

## 验收证据
- boundingRect（390px）：弹窗 [8,382]、标题 [48,302]、关闭 X [334,382]、原料下拉 [60,216]、用量输入 [255,305]、添加原料 [48,342]、取消 [38,190]、保存 [200,352]——全部 left≥0/right≤390；桌面 1280 弹窗 [240,1040] 不退化
- 流程两视口均过：列表非空 → 弹窗标题正确 → 增删行 1→2→1 → 失败保存 toast 可见「保存配方失败: …Data truncation: Out of range value for column 'quantity'」+ 弹窗保留 + 输入 99999999.000 保留 + 取消关闭 → 重开回读 500.000（旧版本保留）→ 改 300 保存 toast「配方已保存，成本已重新核算」→ 重开回读 300.000
- DB：失败生抽行 is_active=1 计数 0；生效行 300克/12.00；历史行版本化保留 2 条；recipe_revision version_no 1、2（created_by=trae_test）；零 /dish-cost/ 调用
- 截图：f:\solo\screenshots\trae-mobile-m-01~04.png（390px）、trae-mobile-d-01~04.png（1280px）
- 复跑：node scripts/trae-cost-mobile/mobile-acceptance.mjs（前置 8081+5175+13317 种子库）

## 超范围壳层证据（只报未改）
1. aside.sidebar 在 390px 自动收为 60px 图标条（w=60/left=0），.main-content w=330/left=60——Dashboard 既有响应式行为，内容区可用；竖排单字菜单属壳层表现
2. 顶部 header（门店切换/铃铛/User/系统名）390px 挤压换行，节点在 Dashboard 壳层
3. 列表菜名列 min-width 180 在 330px 内容区需横滚看全名（表格局部横滚，验收明示允许）

## 测试库适配（隔离库，未重跑破坏性种子、未碰生产）
对齐整合版实体：dish_recipe 补 is_active/revision_id、建 recipe_revision 表（按官方 migrations/recipe_revision_v1.sql）；修正 TR 种子 yield_rate（0/NULL→90/85/100）与 unit（字面?=HEX3F 系早期种子字符集丢失→克/毫升，HEX 验证 E5858B/E6AFB2E58D87）。不修正时整合版 dishCostPreview 严格校验会拦截保存（「配方单位与原料档案不一致」），属测试数据问题非本页缺陷。

## 未覆盖
无真机触摸/手势（无真机），以视口+触控尺寸断言替代；401 console 为登录前 /api/ai/models 竞态，无业务影响。

等待 Codex 审核。

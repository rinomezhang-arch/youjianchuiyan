# TR-OPS-COST-MOBILE-02 started（Trae，2026-09-07）

- 任务 ID：TR-OPS-COST-MOBILE-02（已 claim，CLAIM_VERIFIED）
- 工作目录：F:\solo\artifacts\team-worktrees\trae-mobile-cost（新独立工作树，旧 trae 工作树保留）
- 分支：codex/trae-mobile-cost
- 基线 SHA：a94cc043e702e25578348cc287afb1ca92afc1bd（HEAD 已核：a94cc043 【Codex】应付-验收：真实鉴权流水重放并发与越店拒绝）

## 计划
1. 看旧截图 trae-cost-pw-03-fail-toast390.png 的裁切现象，读 a94cc043 版 CostRecipe.vue（含 dishCostPreview 预览，不改其计算/请求）。
2. 仅在 CostRecipe.vue 内加 dialog 特定 class + scoped 移动样式（≤480px 媒体查询）：弹窗宽度 calc(100vw-20px)、页头/统计卡自适应、列表与弹窗内表格明确局部横向滚动、触控目标≥40px、字号不缩没；桌面视觉保持不退化。
3. 整合版独立服务：新工作树后端打包起 8081（同一隔离 MySQL 13317，不复跑破坏性种子）、vite 起 5175；不冒用旧 5174/8080。
4. Playwright 脚本 scripts/trae-cost-mobile/（复用已装 playwright，零新装依赖）：390px 与桌面两视口，断言每个关键控件 boundingRect 在视口内（left≥0、right≤innerWidth），流程 打开→填入→增删行→失败输入保留→取消→成功回读；截图存 f:\solo\screenshots\trae-mobile-*.png。
5. 壳层（Dashboard 侧边栏/global CSS）若在 390px 挤压内容区，只记录具体节点/样式证据，不改。
6. reported 附改动文件、真实 SHA、截图、未覆盖项。

## 边界
只改 CostRecipe.vue + scripts/trae-cost-mobile/ + docs/协作/Trae/cost-mobile/；不碰 Dashboard/global/router/请求层/dishCostPreview 契约/其他 AI 页面/法务/后端/生产/配置；不装新依赖；不重跑种子。

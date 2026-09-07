# TR-OPS-COST-MOBILE-02 started（R3 续接，Trae，2026-09-07）

- 任务：TR-OPS-COST-MOBILE-02（changes_requested 第二轮，event 20260907T150523Z）；不 claim 新任务，继续原树。
- 工作树：F:\solo\artifacts\team-worktrees\trae-mobile-cost，分支 codex/trae-mobile-cost（当前 HEAD 17cd2add）。
- 依据：Codex changes_requested + 父独立复核 artifacts/cost-mobile-review/复核记录.md。

## 本轮计划
1. CostRecipe.vue 仅在专属 .cost-recipe-dialog ≤768px 媒体查询内补：原料下拉/数量/单位/出成率输入控件实际触控高度≥40px（覆写 size=small 的 24px），保留局部横滚；header/body padding 等被 global.css !important 压住的声明用专属选择器 + !important 落实。不改全局 CSS/Dashboard/算法/契约。
2. 新写严格脚本 scripts/trae-cost-mobile/strict-mobile-check.mjs（旧 mobile-acceptance.mjs 留存不删）：
   - 凭据与地址全部环境变量注入（COST_E2E_BASE_URL / COST_E2E_API_TARGET / COST_E2E_USERNAME / COST_E2E_PASSWORD），缺项列名后安全退出；不打印凭据/token/Authorization/认证响应；报告不写固定凭据。
   - 断言：每控件 x/y/right/bottom 双轴在视口 + elementFromPoint 中心遮挡命中；真实 scrollLeft/scrollTop 变化断言 + 横滚后点击删除 + 长配方(13行)纵滚后底部「添加原料」命中；失败分支精确（toast 须含 "Out of range" 后端分支，非任意错误/校验 warning）；输入保留精确 99999999.000；取消重开精确 500.000；成功重开精确 300.000；采集项(壳层几何)记 INFO 不计 PASS；零 /dish-cost/。
   - 视口 390×844 与 1440×1000（对齐父复核）。
3. DB 主张：mysql 原始输出落证据文件（仅 TR验收_ 合成数据，无凭据），报告逐项标注 DB-已验证/未验证。
4. 纠正原报告"52 完整通过"说法（新 R3 报告中显式更正，旧报告留存）。
5. 新提交 + reported 等待父审。

## 边界
不改全局 CSS/Dashboard/router/request/dishCostPreview/其他页面/法务/后端/生产/配置；不重跑破坏性种子；不装依赖；不清理 git 历史。

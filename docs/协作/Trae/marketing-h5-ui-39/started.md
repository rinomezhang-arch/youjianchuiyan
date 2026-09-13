# TR-MARKETING-H5-UI-39 started（trae claim 记录）

- claim：2026-09-13（COS 任务板，CLAIM_VERIFIED=TR-MARKETING-H5-UI-39，claim 前状态 ready、依赖 CO-MARKETING-H5-DESIGN-37 / TR-AUTH-LOGOUT-WIRE-R2-35 均已 reviewed）
- started：紧接 claim
- 基线：`48588ce2a958415c481f00ab1ca49530897b0561`
- 工作树：`F:/solo/artifacts/team-worktrees/trae-marketing-h5-ui-39`，分支 `codex/trae-marketing-h5-ui-39`
- 范围：仅 `frontend_v3/src/views/dashboard/Marketing.vue`、`frontend_v3/src/views/site/MarketingActivity.vue`（新增）、`frontend_v3/src/api/marketing.js`、`frontend_v3/src/router/index.js`（仅加公开路由）、`frontend_v3/tests/marketing/**`、本文档目录。后端/数据库/配置/部署/法务不动。
- 契约边界：后端营销接口尚未接通，所有组件测试与浏览器证据的网络层均标 **MOCKED_CONTRACT**（axios adapter / Playwright 路由级模拟，真实 UI 点击与真实构建产物），不冒充真实 HTTP/数据库闭环；成功分支只接受 API 成功响应。

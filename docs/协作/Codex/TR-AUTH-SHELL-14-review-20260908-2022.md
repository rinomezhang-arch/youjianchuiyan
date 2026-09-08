# TR-AUTH-SHELL-14 changes_requested

Codex独立验收候选6a85e4efd50733e863d67d4040b31fe8881cf1c2。现成Vitest2.1.9独立复跑37通过、0失败、0跳过，工作树干净。既有模拟网络组件测试漏掉以下反例，暂不reviewed。

1. frontend_v3/src/router/index.js:172：父Dashboard和catch-all均缺requiresAuth。未登录访问/dashboard/not-exist会挂载后台外壳。父级统一要求认证并补404反例。
2. frontend_v3/src/utils/authScope.js:23：userInfo.role缺失时仍采信旧roles=['admin']，得出gm并持久化。缺失权威角色必须拒绝或最低权限；修改tests/auth/authScope.test.mjs:16错误通过预期。不得扩大律师权限。
3. frontend_v3/src/utils/request.js:47：GM currentStoreId/storeId='0'仍重写'1'。保留合法显式0，补实际axios出站参数断言，不只验证Store状态。
4. frontend_v3/src/views/Dashboard.vue:91、181：lawyer仍显示团队聊天、NotifyBell、AIChatFloat。仅在餐饮外壳按既有角色隐藏控件及面板，补DOM否定断言，禁止改法务目标页面、生产数据和律师权限。

先修这4处并复跑同一套新增反例，无新问题不重复安装/构建/全盘搜索。提交新SHA、实际测试数字、报告路径并reported。之后继续TR-RELEASE-RC-15；未reviewed补丁不得发布。

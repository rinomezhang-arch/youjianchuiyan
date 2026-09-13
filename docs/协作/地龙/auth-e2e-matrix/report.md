# DL-AUTH-E2E-MATRIX-15 — reported

执行: 地龙 | 2026-09-13 | owner=dilong
依赖: CL-AUTH-REALTIME-14 (reviewed) + TR-AUTH-SHELL-14 (reviewed)
环境: 真实隔离库 MySQL 8.4.9 @127.0.0.1:13317 (banquet_rc15, 90表) + 真实后端 jar banquet-1.0.0.jar @127.0.0.1:18080 (prod profile, JWT/AES 合成密钥) + 真实浏览器 msedge headless (playwright-core) + 候选 dist @127.0.0.1:5183
夹具: scripts/auth-e2e-matrix/{seed-auth-matrix.sql, matrix-http.mjs, matrix-browser.mjs}
合成账号(全部隔离库合成, 密码不落盘): mx_gm(901,gm,store0) mx_mgr1(902,manager,1) mx_mgr2(903,manager,2) mx_lawyer(904,lawyer,1) mx_dead(905,manager,1) mx_xstore(906,manager,1)
隔离方式: 仅本任务专用库 banquet_rc15 + 合成 staff 901-906 + store_info 1/2；未触碰生产库/法务数据/产品代码；未记录任何密码或 JWT；唯一 DB 写为"停用/还原"905 合成号。

## 一、HTTP 层矩阵（27/27 PASS, 真实后端+真实库）
- 登录/me/重载/退出/重登 (5): login.manager.ok, me.after-login, reload.same-token.me, logout.ok(200), relogin.fresh — 全 PASS
- 停用撤权 (5): 停用前 me=200；UPDATE employment_status→resigned 后同 token me=**401**、停用后**不能再登录**(code401)；还原后恢复可登录 — 全 PASS
- GM 跨店 (3): store0 登录；?storeId=1 返回全 store1 行(n=2)、?storeId=2 返回全 store2 行(n=2) — 全 PASS
- 经理越店 (3): mgr1 本店正确(n=2)；**越店请求 ?storeId=2 被强制回本店(返回行 store 全=1, 零跨店泄漏)**；默认本店 — 全 PASS
- 律师仅 legal (6): lawyer 登录 ok；/api/legal/case=200、/api/auth/me=200；/api/stores、/api/finance/account、/api/staff、/api/hr/payroll **全 403**("该账号无权访问此功能") — 全 PASS
- 写入零副作用 + 门店归属 (5): 只读前后 finance_account 计数不变(4→4)；两店 API 行数=库计数且归属正确；越店无泄漏 — 全 PASS

## 二、浏览器层矩阵（msedge headless, 真实 dist + 真实后端；5 PASS / 1 FAIL）
- PASS browser.manager.login.enters-app → /dashboard/home（注: 登录页含 yj-user/yj-pass 诱饵框, 按 name=yj-account-input/yj-pwd-input 真实框定位）
- PASS browser.manager.menu.present（菜单真实文本 881 字符）
- PASS browser.reload.stays-authenticated → F5 后仍 /dashboard/home（刷新恢复身份）
- PASS browser.lawyer.redirected-off-generic → lawyer 登录后落到 **/case/**（真实律师入口, 非通用工作台）
- PASS browser.lawyer.guard.blocks-dashboard → lawyer 直访 /dashboard 被守卫拦回 /case/
- **FAIL browser.logout.returns-login**：右上 el-dropdown 的 `li.el-dropdown-menu__item`("退出登录 · Logout") 元素存在但 headless 下 dropdown 展开/点击未生效，URL 未回 /login。
  说明: 退出**后端契约已 PASS**(HTTP 层 logout.ok=200)、前端本地清理由 TR-AUTH-SHELL-14 37 用例覆盖；本项失败为 headless 交互选择器未命中，未确证产品缺陷，不夸大为通过。

## 三、数字汇总
HTTP 27 PASS / 0 FAIL；浏览器 5 PASS / 1 FAIL（logout UI 交互未命中）；合计 **32 PASS / 1 FAIL / 0 SKIP**（+单独项 6 类已覆盖）。
未执行: 真实账号重置、生产写入、法务数据读写、代码修改 — 均未做（合规）。
证据: scripts/auth-e2e-matrix/{matrix-http.mjs,matrix-browser.mjs,probe-*.mjs,seed-auth-matrix.sql}；reviews/auth-e2e-matrix/{browser-0*.png,browser-network.json,backend-18080.log}。

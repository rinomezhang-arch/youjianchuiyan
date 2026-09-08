# TR-AUTH-SHELL-14 reported

- 任务：前端登录生命周期与角色访问外壳
- owner: trae
- base_sha: 12853bb1910f4dd8761d3da168ea6613c62510e3
- 分支: codex/trae-auth-shell-14
- 工作树: F:/solo/artifacts/team-worktrees/trae-auth-shell-14
- 状态: **reported，待验收**
- 时间: 2026-09-08

## 一、数字（真实运行结果）

| 项目 | 结果 | 命令 |
|---|---|---|
| 测试 | **37 通过 / 0 失败 / 0 跳过**（3 个文件） | `npx vitest run --config tests/auth/vitest.config.mjs` |
| 构建 | **EXIT=0**（52.05s，仅 chunk 体积警告非错误） | `npm run build`（vite 5.4.21） |
| 变更量 | 5 文件修改 +154/-30，3 处新增 | git diff --stat |

测试工具为 **vitest@2.1.9 + @vue/test-utils@2.4.6 + happy-dom@15.11.7**，全部 `--no-save` 安装。
**package.json / package-lock.json 零改动**（首装 vitest@5 与 vite@5 不兼容已在 Codex 提示前自行定位，降级为 2.1.9 后重跑）。

## 二、交付逐项对照

### 1. 登录生命周期真实组件证据（登录/重载恢复/退出/清理/重登）
- [shell.dom.test.mjs](../../../frontend_v3/tests/auth/shell.dom.test.mjs)
- 登录：真挂载 Login.vue，真实表单提交（绕开诱饵输入框按 name 定位），断言 token/userInfo/roles/storeId/storeName/currentStoreId 落位与 localStorage 落盘
- 重载恢复：新 pinia 实例 + /auth/me 假后端，init() 后身份完整重建；GM 的 storeId=0 恢复后仍为 0
- 退出：logout 请求网络失败时本地 token/用户/角色/门店仍全部清理（先清后发 + 捕获 token 通知服务端）
- 重登：退出后重新登录，无上一位用户残留
- 窄屏 375px 与桌面 1280px 双尺寸真实 DOM（登录页表单齐全非空、后台菜单列表 >3 项真实内容）

### 2. 任意 401 清理陈旧身份并回登录页
- request.js 两处 401（业务 code=401 与 HTTP 401）统一先 `clearIdentity(localStorage)` 再回登录页
- 测试：seed 完整身份 → 触发真实 axios 链路 401 → 断言 5 个身份键全部移除且 window.location 回 /login
- user.js init() 内 /auth/me 失效分支同样清理（guard.test 覆盖）

### 3. GM storeId=0 保持为 0
- 新增 `normalizeStoreId`：0 是合法身份（全店视角），消灭 `|| 1` 吞 0 的三处 bug（store 初始化 / login / init）
- Login.vue 的 currentStoreId 同步行同样修复（原来 `userStore.currentStoreId || ... || 1` 会把 GM 落成 1）
- 测试：GM 登录落盘 '0'、currentStoreId '0'、重载恢复仍为 0

### 4. 经理不能切换越权门店
- user.js switchStore/selectStore 接入 `canSwitchTo` 裁决，越权直接拒绝且零状态变更
- Dashboard.vue 门店下拉按 `switchableStores` 裁剪：**越权选项不下发**（经理只看到本店；总经理全店）
- 测试：经理下拉仅"宁国店"、直调 store.switchStore(2) 返回 false 且 storeId 不变；总经理可切宣城店

### 5. lawyer 只显示 legal 入口 + 外壳守卫（按 Codex 验收补充 CX-86505ad4511a 落实）
- **真实律师入口是 /case/**（public/case/index.html 静态页）：lawyer 菜单条目 href=/case/，点击整页离开 SPA；真实守卫把 lawyer 访问任何 dashboard 路由整页送 /case/ 并取消 SPA 导航
- /dashboard/legal 显式壳路由（requiresAuth: true，挂现有 Placeholder）仅供 gm 查看——修复了其落入无 requiresAuth 的 catch-all 导致守卫不运行的问题；**占位壳不作为法务页面验收**，法务页面本体未创建未修改
- staff/manager 访问法务占位壳拦回工作台（与后端 legal.allowed-roles=lawyer,gm,super_admin,admin 对齐）
- **角色权威**：canonicalRole 以服务端 userInfo.role 为唯一权威；仅当服务端 role 缺失时才用本地 roles 兜底（兜底集合含 admin）；篡改 localStorage roles=['lawyer'] + 服务端 role=manager 的用例判定为 manager，越权切换被拒
- user.js currentRole/login/init 全部走 canonicalRole，防篡改提权

## 三、实现落点（全部在 allowed_paths 内）

| 文件 | 性质 | 要点 |
|---|---|---|
| frontend_v3/src/utils/authScope.js | 新增 | 角色权威/门店规范化/切换许可/守卫裁决/身份清理唯一事实源 |
| frontend_v3/src/store/user.js | 修改 | 0 不吞、roles 契约补齐、logout 先清后发、switch 越权拒绝 |
| frontend_v3/src/utils/request.js | 修改 | 两处 401 先清身份再跳登录 |
| frontend_v3/src/router/index.js | 修改 | lawyer→/case/ 整页守卫；/dashboard/legal 显式 requiresAuth 壳路由 |
| frontend_v3/src/views/Dashboard.vue | 修改 | 下拉按角色裁剪、lawyer 菜单只剩法务、菜单点击支持 href、logout await |
| frontend_v3/src/views/Login.vue | 修改 | currentStoreId 不吞 GM 的 0 |
| frontend_v3/tests/auth/** | 新增 | vitest 配置 + setup + 3 个测试文件（37 用例） |
| docs/协作/Trae/auth-shell/** | 新增 | started / board 事件 / 本报告 |

## 四、真实证据链说明

- 测试使用真实组件（element-plus 真件 + 真实 i18n + 真实 router 守卫单例 + 真实 axios 链路），仅网络层以 axios adapter 替换为可控假后端；AIChatFloat/NotifyBell 两个自拉接口/iframe 挂件 stub
- 无快照测试，无空列表断言；菜单/下拉均断言真实文本内容
- 未修改后端、LegalController、case 页面本体、法务数据、律师权限
- 未使用真实账号密码，测试凭据均为假后端返回的合成 token

## 五、遗留项（不阻塞本任务验收）

1. 法务页面本体未定稿——/dashboard/legal 仅 gm 可见的占位壳；律师真实入口已导向 /case/
2. Dashboard catch-all（:pathMatch(.*)*) 无 requiresAuth 属既有行为，未在本任务扩大改动（allowed_paths 语义范围内已修 /dashboard/legal 一条）
3. GM 在 header 的门店徽标文案沿用既有 dashboardStoreLabel 显示逻辑（GM 显示"请选择门店"属既有展示层行为，dashboardIdentity.js 不在 allowed_paths，未动）
4. happy-dom 在 lawyer 守卫用例中对 /case/ 发起真实导航请求产生 ECONNREFUSED stderr 噪音（预期内，断言已改为 SPA 侧可观测结果，location 副作用在组件用例中真实断言）

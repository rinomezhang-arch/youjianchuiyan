# TR-AUTH-LOGOUT-WIRE-R2-35 reported（trae）

## 结论
- 状态：**reported**。真实浏览器复验后认定：**基线 `426947e6` 上不存在「点退出无确认框」应用缺陷**；a900174f 矩阵唯一失败源于探针断言只查 Element Plus 旧弹层 `.el-message-box`，而桌面外壳在 3a6e9a17 改版后使用自定义确认弹层（`.modal-overlay`，Teleport 到 body）。真实点击链实测全部成立，故**业务源码零改动**，按「最小根因」纪律不编造补丁；交付最小 DOM 回归测试把该契约锁死，避免过期选择器再次误判。
- 分支 `codex/trae-auth-logout-r2-35`（工作树 `artifacts/team-worktrees/trae-auth-logout-r2-35`），基线父提交 `426947e6`；补丁提交 **`2670042b`**（仅 1 个代码文件：`frontend_v3/tests/auth/logout-wire.dom.test.mjs` + 本文档证据），最终 HEAD 为本报告提交。
- allowed_paths 零越界；未碰法务/后端/数据库结构/配置/部署；未跑大套件。

## 复现与定位（证据先行，不猜改）
- 用 a900174f **原始探针 verbatim**（`probe-logout-click.original.mjs`，SHA256 `e4e4161f7a9fa174…` 与事故记录 E4E4161F7A9FA174 一致）对基线重建 dist 复跑：avatar=1、菜单项可见、真实点击执行、token/URL 不变、零 `/api` 出站、`message-box.found=false` —— 复现了 a900174f 的表面现象。
- 但 DOM 级探针（`probe-logout-dom2.mjs`，每 120ms 采样）显示：点击退出项后**自定义确认弹层真实出现**——`.modal-overlay` 1280×720 `display:flex`、opacity 0.008→1 淡入，`.modal-dialog` 400×274 `display:block`；`.el-dropdown-menu` 同步收起（w/h=0）。
- 时间线核实：自定义弹层在 013afcf1（地龙测试的候选 dist）与 426947e6（本卡基线）中均存在；3a6e9a17 同时是两者祖先。地龙矩阵预期「点退出项即登出」，与 R2 验收「必须先出确认框」本身冲突；探针选择器又只认旧 MessageBox，双重原因导致假失败。

## R2 验收逐条（全部真实 DOM 点击，无 handleCommand/confirmLogout 直调）
| 验收 | 实测 |
|---|---|
| 真实头像下拉点退出必须出现确认框 | `.modal-dialog` 淡入出现（trace 采样 + 截图 `final-modal.png`） |
| 取消后会话保持 | `CANCEL path: url=/dashboard/home, token=true, overlay=false`，零 logout 出站 |
| 确认后调用现有 userStore.logout，清 token/身份并跳 /login | `POST /api/auth/logout 200`；localStorage 5 键 → 0；`url=/login` |
| 服务端 logout 失败也必须完成本地退出 | 强制 500：token/roles/currentStoreId 全 null，仍 `/login`（store 先本地清理后通知，既有 401 测试同覆盖） |
| 退出后后退/直达受保护路由 | 浏览器后退 `/login`、直达 `/dashboard/home` 被拦回 `/login`，头像菜单 0、无 token、登录输入框在 |
| 不以直调方法冒充真实点击 | 浏览器链全程 Playwright real click；DOM 测试仅对渲染元素派发 `click()` |

## 最小 DOM 回归
- 新文件 [logout-wire.dom.test.mjs](file:///F:/solo/artifacts/team-worktrees/trae-auth-logout-r2-35/frontend_v3/tests/auth/logout-wire.dom.test.mjs)，4 项：①点退出项只弹框、未发 logout、会话保持；②取消关框且登录态/路由不变；③确认发 post logout、清全部身份键、Pinia 登出、进 /login；④logout 网络错误仍本地退出并进 /login。
- 真实挂载 Dashboard.vue（Element Plus/i18n/pinia 全真件，仅 AIChatFloat/NotifyBell stub，沿用 SHELL-14 设施），通过头像触发器与 teleport 菜单项的真实 DOM click 走 `@command` 公开事件链，不断言组件内部方法。
- 测试结果：**tests/auth 4 文件 48/48 通过，0 失败 0 跳过**（新增 4 项；原 44 项无回归）。happy-dom 不做布局（rect 恒 0），弹层可见性用计算样式 + 标题文案断言。
- 前端构建：`npm run build` 通过（依赖以目录联接复用同基线 426947e6 树 node_modules，两树 package.json 零改动、零安装）；最终 dist 经上述四探针复验。

## 环境与边界
- 固定制品：a900174f 同一 jar SHA256 `3b5c428da3c9191aa48eb2f77447316a3919f4b362e2707e0b3ffe2b40bad16f`，进程数据源实测 `13318/banquet_rc15`（Win32_Process 命令行断言，无 13317）；前端 5185（a900174f serve-dist verbatim 反代 18084）；CORS 仅以运行时参数放行 5185，未改配置。
- 合成账号 mx_r35trae913_2（manager/store1，ids 958934–958939，经 reviewed seed-auth-matrix.mjs INSERT/ON DUP 装载），结束后 6 行 `employment_status=resigned` 留痕，无物理删除；密码全程环境变量运行时注入，证据无密码/JWT 明文（原始探针输出中的 JWT 头部前缀已脱敏）。
- 本轮临时后端（18084）与静态服务（5185）已停止；未新建/删除 schema，13317 零连接。

## 证据（docs/协作/Trae/auth-logout-wire-r2-35/evidence/，SHA256 前16）
- 原探针：`e4e4161f7a9fa174`（2733B）；基线复现输出：`probe-baseline-output.txt bf9cc9c8…`（JWT 已脱敏）
- 定位/路径探针：`probe-logout-dom2.mjs a433fa82…`、`probe-logout-confirm.mjs 23055f0b…`、`probe-logout-guard.mjs 46af6ec4…`、`diag-login.mjs ad623b60…`（CORS 403 环境定位留痕）
- 截图：`final-modal.png 1093af4e…`（确认弹层）、`baseline-after-item-click.png b84483b0…`、`final-confirm-login.png 51e93378…`（200 后登录页）、`final-server-fail-login.png 51e93378…`（500 仍登录页）、`final-guard.png 51e93378…`、`after-logout-guard.png 51e93378…`、`diag-login.png f9bd350d…`
- `evidence-manifest.json` 汇总环境/账号处置/哈希。

## 给验收方的处置建议
- 若裁决要求「必须有应用代码补丁」形态，本卡无可改的真实缺陷；建议改为关闭 R2（基线已满足）或将本卡并入矩阵探针修复任务（把 `.el-message-box` 断言更新为识别自定义 `.modal-overlay`，该文件属地龙 scripts 范围，不在本卡 allowed_paths，未擅动）。

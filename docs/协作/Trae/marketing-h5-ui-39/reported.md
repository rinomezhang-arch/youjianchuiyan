# TR-MARKETING-H5-UI-39 reported — 营销后台工作台 + 客人活动 H5（前端候选）

- 执行者：trae（solo）｜统筹：Codex（CX-TR39-2151）
- 基线：`48588ce2a958415c481f00ab1ca49530897b0561`
- 分支：`codex/trae-marketing-h5-ui-39`
- 实现提交：`fafb65f0f747ac90e1f650dff84e5c6f9c0713a0`（本报告在其后的 docs 提交）
- 结论：**PASS（前端候选范围）**。任务卡前端验收点全部有自动化或真实浏览器证据；凡后端未通项均标 **MOCKED_CONTRACT / NOT_COVERED**，无一处冒充真实闭环。

## 1. 交付（严格落在 6 个 allowed_paths）

| 文件 | 说明 |
| --- | --- |
| `frontend_v3/src/api/marketing.js` | 重写。严格按 `docs/营销H5生产闭环设计_20260913.md` 第四节路径封装内部/公开接口；契约纯函数：四栏归列、六态判定 `readPublicActivity`、`isPubliclyVisible`、`buildInquiryPayload`（显式剔除 storeId）、表单校验、幂等键、真实照片白名单 `resolveHeroAsset`。无物理删除封装。文件头 MOCKED_CONTRACT 声明。 |
| `frontend_v3/src/views/dashboard/Marketing.vue` | 重写。顶部仅 3 主动作；四栏（草稿/待审批/已发布/已结束）显示门店/状态/版本/渠道/有效期；行操作按状态机；**无删除入口**（取消=状态）；三步侧滑窗（基本信息→客人页面→审批与发布），门店 `storeId:null` 不预选 1；保存失败留当前步、保留输入、字段级错误；发布需先打开真实 H5 预览、成功只认响应中 publicationId/publicSlug、requestId 一次意图一个失败重试沿用、失败红字不显示成功不关抽屉；转化数据为合同入口，未接通显式空态不造数字。 |
| `frontend_v3/src/views/site/MarketingActivity.vue` | 新增。公开路由页六态：加载骨架/未找到/暂停/过期/网络错误（可重试）/成功。成功首屏：门店、标题、金色有效期、真实门店照片、唯一主按钮「咨询档期」（同名按钮两处通向同一表单）。无渐变/玻璃拟态/自动轮播/假统计。咨询提交 sourceCode+客人字段+稳定 requestId，不含 storeId；无返回编号不当成功；失败留输入并复用 requestId；成功页显示咨询编号+查询入口。 |
| `frontend_v3/src/router/index.js` | 仅加公开路由 `/h5/activity/:publicSlug`（顶层，不挂 requiresAuth）。 |
| `frontend_v3/tests/marketing/**` | 新增 vitest 配置（root/alias 与 tests/auth 同款，复用目录联接的零安装依赖）+ setup + helpers（axios adapter 合同假后端）+ 3 个测试文件，全部在文件头/用例名标注 MOCKED_CONTRACT。 |
| `docs/协作/Trae/marketing-h5-ui-39/**` | started.md、本报告、evidence/（脚本、8 张 390px 截图、请求体 JSON、运行日志、结果 JSON）。 |

未触碰：任何后端/数据库/迁移/配置/部署/依赖文件（node_modules 复用既有 junction，package.json/lock 零改动）；未跑全量测试（仅 `tests/marketing/**`）。

## 2. 任务卡验收点结论矩阵

| # | 验收点 | 结论 | 证据 |
| --- | --- | --- | --- |
| 1 | 顶部仅 3 主动作（新建活动/待我审批/查看已发布H5） | PASS | workbench 用例 1：header 按钮文本精确等于三项 |
| 2 | 四栏显示门店/状态/版本/渠道/有效期 | PASS | workbench 用例 1 断言「欢乐巷店/已发布/版本v2/渠道H5/2026-09-01」 |
| 3 | 物理删除入口消失 | PASS | 界面无「删除」；出站无 DELETE；marketing.js 无删除封装；取消为 POST .../cancel |
| 4 | 三步侧滑窗；门店必选不默认 1 | PASS | 空表单校验报 `storeId`（contract 用例）；新建时 `storeId=null`，用例真实/缝选门店 2 后创建载荷 storeId=2（非兜底 1） |
| 5 | 保存失败留当前步、保留输入 | PASS | workbench 用例 2：首次 POST 网络失败→step 不前进、第二步 v-show 隐藏、名称输入仍在、红字「保存失败」；恢复后同表单继续进入第二步 |
| 6 | 发布成功只认 API 成功响应 | PASS | workbench 用例 3：未预览按钮禁用；首次网络失败→「发布失败」红字、无成功提示、抽屉仍开；缺 publicationId/publicSlug 的 200 响应代码侧也主动 throw（见 publish()） |
| 7 | 公开路由 `/h5/activity/:publicSlug` | PASS | router；Playwright 直接访问 dist 路由（SPA fallback）渲染 |
| 8 | 390px 首屏：门店/标题/有效期/真实门店图/单一主按钮 | PASS | 浏览器证据 01：naturalWidth=1920 真实加载 `/site-photos/storefront-entrance.jpg`；可见主按钮恰好两个同名「咨询档期」 |
| 9 | 禁渐变/玻璃拟态/自动轮播/假统计/新 UI 框架 | PASS | 纯色 CSS，无新依赖；转化入口未接通为空态文案 |
| 10 | 六态 | PASS | H5 用例 8 项 + 截图 01–06：加载骨架、404/草稿→未找到、暂停、过期、断网→网络错误并可重试恢复、成功 |
| 11 | 不可公开状态不显示咨询按钮 | PASS | 暂停/过期/未找到用例 `.mk-cta` 计数 0，且无 view 埋点 |
| 12 | 无横向滚动、按钮单行 | PASS | 成功页/表单/成功后三页 `scrollWidth=390`；CTA `white-space:nowrap` 且 scrollWidth≤clientWidth |
| 13 | 咨询只提交 sourceCode+表单字段+稳定 requestId（不信任 storeId） | PASS | `inquiry-request-bodies.json`：两次请求体逐字段核对，无 storeId/store_id，requestId 相同 |
| 14 | 成功页显示返回咨询编号+查询入口 | PASS | 截图 08：YJ-20260913-3901 + 查询入口 /stores/1；无编号则 throw 防假成功（H5 用例 + 代码） |
| 15 | API 路径严格用设计文档第四节 | PASS | 内部 `/api/marketing/activities[...]`、submit/publish、publications/{id}/pause、cancel；公开 `/api/public/marketing/activities|a/{slug}|events`、`/api/public/booking-inquiry`（marketing.js 路径表） |
| 16 | 草稿不可公开 | PASS | draft slug 由合同后端回 404 → 统一「活动不存在或已下架」（截图 04）；published 但过期/未开始同样不可见（contract 边界用例） |
| 17 | 暂停/过期不可咨询 | PASS | 同 #11 |
| 18 | 发布失败不显示成功 | PASS | 同 #6；断言编辑器文本含「发布失败」不含「发布成功」、drawerOpen=true |
| 19 | 咨询重复 requestId 复用 | PASS | 组件用例 + 浏览器证据：abort 后二次提交 requestId 相同（fe226e47…） |
| 20 | 390px 无横向滚动 | PASS | 同 #12 |
| 21 | 报告 PASS/FAIL/NOT_COVERED/INFO | PASS | 本文件 |
| 22 | 构建通过 | PASS | `npm run build` ✓ built in 34.15s；含 `MarketingActivity-*.js 10.25 kB`、`Marketing-*.js 20.73 kB`；dist/site-photos 15 张真实资产 |
| 23 | 精确 SHA、推送核对 | PASS | 见 §6 |

## 3. 自动化测试（MOCKED_CONTRACT，axios adapter 合同假后端）

`npx vitest run --config tests/marketing/vitest.config.mjs` → **Test Files 3 passed，Tests 21 passed**（日志 evidence/vitest-run.log）：

- `contract.test.mjs` ×10：四栏归列、可见性边界（未开始/过期/非 published）、`readPublicActivity` 六态、提交体剔除 storeId、手机号/人数校验、门店必选且 0/空非法、日期倒置、snake_case 规范化、照片白名单拒绝外链。
- `workbench.dom.test.mjs` ×3：四栏渲染+无删除+三动作；保存失败留步保输入与恢复；发布预览闸门/失败不冒充成功/requestId 幂等/成功才关抽屉。
- `h5-activity.dom.test.mjs` ×8：草稿→未找到、HTTP 404 同口径、暂停、过期、断网重试、成功首屏+提交成功（请求体无 storeId）、失败留输入+同 requestId、空提交与手机号就地校验。

浏览器证据（Playwright + msedge，390×844 @2x，vite preview dist，page.route 合同 fulfill）：**25/25 PASS**（evidence/browser-evidence-run.log、browser-evidence-result.json）。

## 4. MOCKED_CONTRACT 边界（重要）

以下接口在前端按合同先行落地，**本会话内没有任何真实后端/数据库参与**：营销活动 CRUD、submit、publish、pause、cancel、attribution（内部）；公开快照、view 事件、booking-inquiry（公开）。组件测试经 axios adapter、浏览器证据经 Playwright 路由 fulfill 提供合同 JSON。上线前必须以后端真实实现重新联调并回归本套测试（adapter 切换为真实端点即可复用断言）。

## 5. NOT_COVERED / INFO

**NOT_COVERED（依赖后端或超出本任务 allowed_paths）**
- 物理渠道投放（物料二维码、桌贴等）：无前端面，NOT_COVERED。
- 企微发送/分享链路：合同常量保留 `wecom` 文案但本期渠道仅 `h5`（MARKETING_CHANNELS=['h5']），NOT_COVERED。
- 真实后端联调、鉴权与门店数据权限（跨店总经理可见范围）、数据库回读、乐观锁冲突真实响应、归因数字：NOT_COVERED；转化数据入口在未接通时只显示空态。
- 审批人实时身份权限、驳回意见回显链路：前端提交 business_type=marketing_activity，真实批复流 NOT_COVERED。
- 发布前「草稿内容级预览」：previewH5 打开的是真实公开地址（发布前按设计显示不可公开），不做假内容预览；如需草稿预览需后端预览接口，NOT_COVERED。
- el-date-picker daterange 真人键入：happy-dom 无弹层排版，组件测试经 vm.dateRange 测试缝注入；390px 真人操作留待真机回归（日期值在真实链路被 watch 同步并进入保存载荷）。
- 390px 以外断点的后台工作台像素级走查：CSS 已写 1200/768 响应式断点，未截图取证，NOT_COVERED。

**INFO（不影响验收，交接备查）**
- 全局 GET 拦截器（utils/request，不在 allowed_paths）会对缺省 storeId 的 GET 兜底注入 `?storeId=1`；工作台列表已用 `params:{storeId:null}` 显式阻止。公开 slug 详情 GET 仍会被注入该参数，后端对公开接口必须忽略它；**咨询 POST 请求体已在构造层显式 delete storeId，浏览器请求体可证**。
- 网络失败时除表单单行红域外，全局拦截器还会弹 3s ElMessage（项目既有约定，改动拦截器超出 allowed_paths）；成功页截图已等其消失，见脚本。
- 测试环境 node_modules 为目录联接复用既有 `--no-save` 安装（vitest 2.1.9/happy-dom/@vue/test-utils/Element Plus 2.14.2），package.json 与 lock 零改动。

## 6. 证据与 SHA

- 构建：`npm run build` 成功（34.15s），MarketingActivity 独立 chunk 10.25 kB / 后台 Marketing chunk 20.73 kB；真实照片 15 张进入 dist/site-photos。
- 证据目录 `docs/协作/Trae/marketing-h5-ui-39/evidence/`（sha256 前 16 位）：
  - 01-live-firstscreen-390.png `5181309a683ae88a`｜02-paused `53f2400f95663c1b`｜03-expired `19261a42c0c68d91`｜04-notfound-draft404 `caf9e623bae9b310`
  - 05-network-error `a6844fcbb2647927`｜06-retry-success `071ad2bb92c1d02d`｜07-inquiry-form-filled `a97c9d8dce8ddd7c`｜08-inquiry-success `a52ff67aa81f6ff3`
  - inquiry-request-bodies.json `1b2d230805afd318`（无 storeId、同 requestId）｜browser-evidence-result.json `f4b9974424525543`｜browser-evidence-run.log `6187f4f5bd4ee654`｜vitest-run.log `7ddbdb27396e510b`｜collect-browser-evidence.mjs `885b649aac969ae8`
- 实现 SHA：`fafb65f0f747ac90e1f650dff84e5c6f9c0713a0`；推送后远端核对结果见任务板 event 记录（ls-remote 输出）。
- 合成数据声明：证据中手机号 13800000001、称呼「王女士」均为合成数据；无 JWT/真实顾客资料落盘。

## 7. 建议下一步（交后端任务）

1. 按设计第四节实现活动 CRUD/审批/发布/暂停/取消与公开快照，错误码与 404 口径与本前端 `readPublicActivity` 对齐。
2. booking-inquiry 凭 sourceCode 反查门店并做 requestId 服务端幂等，返回 inquiryNo/lookupUrl。
3. 接通后将本套 adapter/route 合同切到真实端点回归；归因接口返回 views/inquiries/bookings/arrivals 后空态自动转数据。

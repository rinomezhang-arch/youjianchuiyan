# TR-MARKETING-H5-UI-39 reported — 营销后台工作台 + 客人活动 H5（前端候选）

- 执行者：trae（solo）｜统筹：Codex（CX-TR39-2151）
- 基线：`48588ce2a958415c481f00ab1ca49530897b0561`
- 分支：`codex/trae-marketing-h5-ui-39`
- 实现：R0 `fafb65f0f747ac90e1f650dff84e5c6f9c0713a0`；R1 五项定向修复 `d9cb3c0c3c9d78381a05872d1dc73e139fa594aa`（本报告为其后的 docs 提交）
- 结论：**PASS（前端候选范围）**。R1 评审（`TR-MARKETING-H5-UI-39-review-r1-20260913.md`）五项必须修正全部闭合并有对应定向证据；任务卡前端验收点全部有自动化或真实浏览器证据；凡后端未通项均标 **MOCKED_CONTRACT / NOT_COVERED**，无一处冒充真实闭环。本报告全文仅使用 GBK 可编码字符（R1 评审项 5）。

## 0. R1 五项修复对照（评审 -> 实现 -> 证据）

| # | 评审要求 | 修复实现 | 定向证据 |
| --- | --- | --- | --- |
| 1 | 公开 H5 GET 显式禁止拦截器补 storeId=1，只按 slug 读取，并更新网络证据 | `marketing.js` 的 `getPublicMarketingActivity` 显式 `params:{storeId:null}`：拦截器仅在 `storeId===undefined` 时兜底，`null` 原样跳过；axios 默认序列化不输出 null，真实 URL 无 `?storeId`（不改拦截器文件本身，仍在 allowed_paths 内） | 组件用例「公开快照 GET 只按 slug」（断言 params.storeId 为 null 且非 '1'/1）；浏览器证据第 27 行「公开 GET URL 不含 storeId (R1-1)」：6 个快照请求 URL 全部无 `storeId` |
| 2 | 390x844 首屏只允许一个可见主 CTA；第二入口不得与首屏同框 | `MarketingActivity.vue` 主按钮常驻；底部同名第二入口仅当主按钮完全离开视口（`getBoundingClientRect().bottom<0 或 top>window.innerHeight`，滚动监听）才渲染 | 浏览器证据第 3 行「首屏可见主按钮恰好一个」、第 4 行「首屏不渲染底部第二入口」、第 15 行「滚出首屏后底部第二入口出现（不与首屏 CTA 同框）」；组件用例同步断言 |
| 3 | 成功页必须同时使用后端返回的 inquiryNo 与 lookupUrl；缺任一字段不得显示成功，不得自行拼门店地址 | 提交成功处理改为双门槛：`inquiryNo` 与 `lookupUrl` 任一缺失即 throw，落网络错误态重试（requestId 复用）；查询入口 href 一律取响应 `lookupUrl`，删除按 storeId 拼接 `/stores/{id}` 的逻辑 | 组件用例「成功只认后端编号+查询入口（R1-3）」；浏览器证据第 19-20 行「成功只认后端编号 编号=YJ-20260913-3901」「查询入口来自 API lookupUrl href=/h5/inquiry-lookup?no=YJ-20260913-3901」 |
| 4 | previewH5 用当前草稿数据渲染同一套 H5 模板，用户真实看见内容后才解锁发布；证据继续标 MOCKED_CONTRACT | 工作台 `togglePreview()` 重写：预览卡片按当前表单草稿（门店/标题/有效期/真实门店图/主按钮文案）本地渲染，预览可见才 `previewConfirmed=true`；缺客人可见标题则拒绝解锁；不再打开公开地址（发布前该地址本就 404，无意义） | workbench 用例 3 更新：未预览禁发布 -> 打开预览（草稿数据渲染断言）-> 解锁发布；断言预览卡含草稿标题与门店名 |
| 5 | 清理证据日志格式（git diff --check 报行尾空白/末尾空行）；新报告只用 GBK 可编码字符 | 新增 `evidence/run-and-clean-log.mjs` 统一 runner：去 ANSI 色码、对勾/叉号替换为 [PASS]/[FAIL]、仅对勾不误伤乘号、去行尾空白与末尾多余空行、UTF-8 无 BOM 落盘；vitest 与浏览器证据均经该 runner 重出；本报告全文 GBK 安全 | `vitest-run.log`、`browser-evidence-run.log` 均为清洗后文件；`git diff --check` 无输出；报告全文经 GBK 严格编码校验通过（无对勾/叉号等非 GBK 字符） |

## 1. 交付（严格落在 6 个 allowed_paths）

| 文件 | 说明 |
| --- | --- |
| `frontend_v3/src/api/marketing.js` | 重写（R0）+ R1 修 1：公开读取只按 slug。契约纯函数：四栏归列、六态判定 `readPublicActivity`、`isPubliclyVisible`、`buildInquiryPayload`（显式剔除 storeId）、表单校验、幂等键、真实照片白名单 `resolveHeroAsset`。无物理删除封装。文件头 MOCKED_CONTRACT 声明。 |
| `frontend_v3/src/views/dashboard/Marketing.vue` | 重写（R0）+ R1 修 4：发布预览改为当前草稿数据本地渲染 H5 模板卡片，看见内容才解锁发布。顶部仅 3 主动作；四栏（草稿/待审批/已发布/已结束）显示门店/状态/版本/渠道/有效期；行操作按状态机；**无删除入口**（取消=状态）；三步侧滑窗（基本信息->客人页面->审批与发布），门店 `storeId:null` 不预选 1；保存失败留当前步、保留输入、字段级错误；发布成功只认响应中 publicationId/publicSlug、requestId 一次意图一个失败重试沿用、失败红字不显示成功不关抽屉；转化数据为合同入口，未接通显式空态不造数字。 |
| `frontend_v3/src/views/site/MarketingActivity.vue` | 新增（R0）+ R1 修 2/3：首屏单一主 CTA（底部第二入口滚出视口才出现）；成功双门槛（inquiryNo+lookupUrl 同缺不可，查询入口只取 API lookupUrl）。公开路由页六态：加载骨架/未找到/暂停/过期/网络错误（可重试）/成功。成功首屏：门店、标题、金色有效期、真实门店照片。无渐变/玻璃拟态/自动轮播/假统计。咨询提交 sourceCode+客人字段+稳定 requestId，不含 storeId；失败留输入并复用 requestId。 |
| `frontend_v3/src/router/index.js` | 仅加公开路由 `/h5/activity/:publicSlug`（顶层，不挂 requiresAuth）。 |
| `frontend_v3/tests/marketing/**` | R1 更新 3 个测试文件：新增/修订定向用例覆盖五项修复行为（公开 GET 无 storeId、首屏单 CTA、成功双门槛、草稿预览闸门）；vitest 配置（root/alias 与 tests/auth 同款，复用目录联接的零安装依赖）+ setup + helpers（axios adapter 合同假后端），全部标注 MOCKED_CONTRACT。 |
| `docs/协作/Trae/marketing-h5-ui-39/**` | started.md、本报告（R1 修订版）、evidence/（脚本含新增 run-and-clean-log.mjs、8 张 390px 截图、请求体 JSON、运行日志、结果 JSON）。 |

未触碰：任何后端/数据库/迁移/配置/部署/依赖文件（node_modules 复用既有 junction，package.json/lock 零改动）；未跑全量测试（仅 `tests/marketing/**`）；utils/request.js 拦截器未改（用显式参数绕开，R1 评审项 1 不要求改拦截器）。

## 2. 任务卡验收点结论矩阵

| # | 验收点 | 结论 | 证据 |
| --- | --- | --- | --- |
| 1 | 顶部仅 3 主动作（新建活动/待我审批/查看已发布H5） | PASS | workbench 用例 1：header 按钮文本精确等于三项 |
| 2 | 四栏显示门店/状态/版本/渠道/有效期 | PASS | workbench 用例 1 断言「欢乐巷店/已发布/版本v2/渠道H5/2026-09-01」 |
| 3 | 物理删除入口消失 | PASS | 界面无「删除」；出站无 DELETE；marketing.js 无删除封装；取消为 POST .../cancel |
| 4 | 三步侧滑窗；门店必选不默认 1 | PASS | 空表单校验报 `storeId`（contract 用例）；新建时 `storeId=null`，用例真实/缝选门店 2 后创建载荷 storeId=2（非兜底 1） |
| 5 | 保存失败留当前步、保留输入 | PASS | workbench 用例 2：首次 POST 网络失败->step 不前进、第二步 v-show 隐藏、名称输入仍在、红字「保存失败」；恢复后同表单继续进入第二步 |
| 6 | 发布成功只认 API 成功响应 | PASS | workbench 用例 3：未预览按钮禁用；首次网络失败->「发布失败」红字、无成功提示、抽屉仍开；缺 publicationId/publicSlug 的 200 响应代码侧也主动 throw（见 publish()） |
| 7 | 公开路由 `/h5/activity/:publicSlug` | PASS | router；Playwright 直接访问 dist 路由（SPA fallback）渲染 |
| 8 | 390px 首屏：门店/标题/有效期/真实门店图/**单一主按钮** | PASS | 浏览器证据 01：naturalWidth=1920 真实加载 `/site-photos/storefront-entrance.jpg`；可见主按钮**恰好一个**「咨询档期」，底部第二入口首屏不渲染（R1-2 修复后） |
| 9 | 禁渐变/玻璃拟态/自动轮播/假统计/新 UI 框架 | PASS | 纯色 CSS，无新依赖；转化入口未接通为空态文案 |
| 10 | 六态 | PASS | H5 用例 10 项 + 截图 01-06：加载骨架、404/草稿->未找到、暂停、过期、断网->网络错误并可重试恢复、成功 |
| 11 | 不可公开状态不显示咨询按钮 | PASS | 暂停/过期/未找到用例 `.mk-cta` 计数 0，且无 view 埋点 |
| 12 | 无横向滚动、按钮单行 | PASS | 成功页/表单/成功后三页 `scrollWidth=390`（证据第 5/16/21 行）；CTA `white-space:nowrap` 且 scrollWidth<=clientWidth |
| 13 | 咨询只提交 sourceCode+表单字段+稳定 requestId（不信任 storeId） | PASS | `inquiry-request-bodies.json`：两次请求体逐字段核对，无 storeId/store_id，requestId 相同（8b999243-3ec5-44cc-92b5-8d236c989564） |
| 14 | 成功页显示返回咨询编号+查询入口（均来自 API） | PASS | 截图 08：YJ-20260913-3901 + 查询入口 href=/h5/inquiry-lookup?no=YJ-20260913-3901（R1-3 修复后取自 API lookupUrl）；无编号或无 lookupUrl 一律 throw 防假成功（H5 用例 + 代码） |
| 15 | API 路径严格用设计文档第四节 | PASS | 内部 `/api/marketing/activities[...]`、submit/publish、publications/{id}/pause、cancel；公开 `/api/public/marketing/activities|a/{slug}|events`、`/api/public/booking-inquiry`（marketing.js 路径表） |
| 16 | 草稿不可公开 | PASS | draft slug 由合同后端回 404 -> 统一「活动不存在或已下架」（截图 04）；published 但过期/未开始同样不可见（contract 边界用例） |
| 17 | 暂停/过期不可咨询 | PASS | 同 #11 |
| 18 | 发布失败不显示成功 | PASS | 同 #6；断言编辑器文本含「发布失败」不含「发布成功」、drawerOpen=true |
| 19 | 咨询重复 requestId 复用 | PASS | 组件用例 + 浏览器证据第 25 行：abort 后二次提交 requestId 相同 |
| 20 | 390px 无横向滚动 | PASS | 同 #12 |
| 21 | 报告 PASS/FAIL/NOT_COVERED/INFO | PASS | 本文件 |
| 22 | 构建通过 | PASS | `npm run build` 成功，dist 产物 2026-09-14 00:10（修复后重建）；含 `MarketingActivity-*.js 10.25 kB`、`Marketing-*.js 24.0 kB`；浏览器证据 29/29 即基于该 dist + preview 采集 |
| 23 | 精确 SHA、推送核对 | PASS | 见 §6 |

## 3. 自动化测试（MOCKED_CONTRACT，axios adapter 合同假后端）

`npx vitest run --config tests/marketing/vitest.config.mjs` -> **Test Files 3 passed，Tests 23 passed**（日志 evidence/vitest-run.log，经 run-and-clean-log.mjs 清洗）：

- `contract.test.mjs` ×10：四栏归列、可见性边界（未开始/过期/非 published）、`readPublicActivity` 六态、提交体剔除 storeId、手机号/人数校验、门店必选且 0/空非法、日期倒置、snake_case 规范化、照片白名单拒绝外链。
- `h5-activity.dom.test.mjs` ×10：草稿->未找到、HTTP 404 同口径、暂停、过期、断网重试、成功首屏+提交成功（请求体无 storeId）、失败留输入+同 requestId、空提交与手机号就地校验；**R1 新增：公开快照 GET 显式 storeId=null 断言（R1-1）、成功双门槛（缺 lookupUrl 不当成功，R1-3）、首屏单 CTA 滚动显隐（R1-2）**。
- `workbench.dom.test.mjs` ×3：四栏渲染+无删除+三动作；保存失败留步保输入与恢复；发布预览闸门（**R1 修订：草稿数据预览可见才解锁**）/失败不冒充成功/requestId 幂等/成功才关抽屉。

浏览器证据（Playwright + msedge，390×844 @2x，vite preview 读 dist 真实构建产物，page.route 合同 fulfill）：**29/29 PASS**（evidence/browser-evidence-run.log、browser-evidence-result.json）。R1 定向四项：公开 GET URL 无 storeId（6 请求全验）、首屏可见主按钮恰好一个且第二入口不渲染、滚出首屏后第二入口出现（不与首屏同框）、查询入口来自 API lookupUrl。

## 4. MOCKED_CONTRACT 边界（重要）

以下接口在前端按合同先行落地，**本会话内没有任何真实后端/数据库参与**：营销活动 CRUD、submit、publish、pause、cancel、attribution（内部）；公开快照、view 事件、booking-inquiry（公开）。组件测试经 axios adapter、浏览器证据经 Playwright 路由 fulfill 提供合同 JSON。上线前必须以后端真实实现重新联调并回归本套测试（adapter 切换为真实端点即可复用断言）。

## 5. NOT_COVERED / INFO

**NOT_COVERED（依赖后端或超出本任务 allowed_paths）**
- 物理渠道投放（物料二维码、桌贴等）：无前端面，NOT_COVERED。
- 企微发送/分享链路：合同常量保留 `wecom` 文案但本期渠道仅 `h5`（MARKETING_CHANNELS=['h5']），NOT_COVERED。
- 真实后端联调、鉴权与门店数据权限（跨店总经理可见范围）、数据库回读、乐观锁冲突真实响应、归因数字：NOT_COVERED；转化数据入口在未接通时只显示空态。
- 审批人实时身份权限、驳回意见回显链路：前端提交 business_type=marketing_activity，真实批复流 NOT_COVERED。
- 草稿预览的后端预览接口（分享给他人看的 draft 预览）：前端已按草稿数据本地渲染（R1-4），如需后端生成可分享预览链接另立任务，NOT_COVERED。
- el-date-picker daterange 真人键入：happy-dom 无弹层排版，组件测试经 vm.dateRange 测试缝注入；390px 真人操作留待真机回归（日期值在真实链路被 watch 同步并进入保存载荷）。
- 390px 以外断点的后台工作台像素级走查：CSS 已写 1200/768 响应式断点，未截图取证，NOT_COVERED。

**INFO（不影响验收，交接备查）**
- 全局 GET 拦截器（utils/request，不在 allowed_paths）只对 `storeId===undefined` 的 GET 兜底注入；公开快照与工作台列表均已显式 `params:{storeId:null}` 阻止，浏览器网络证据证明真实 URL 无 `?storeId`（R1-1 闭合）。**咨询 POST 请求体已在构造层显式 delete storeId，浏览器请求体可证**。
- 网络失败时除表单单行红域外，全局拦截器还会弹 3s ElMessage（项目既有约定，改动拦截器超出 allowed_paths）；成功页截图已等其消失，见脚本。
- 测试环境 node_modules 为目录联接复用既有 `--no-save` 安装（vitest 2.1.9/happy-dom/@vue/test-utils/Element Plus 2.14.2），package.json 与 lock 零改动。
- run-and-clean-log.mjs 仅替换对勾字符为 [PASS]，叉号类单独映射，不误伤文案中的乘号（首轮曾误替换已修正）。

## 6. 证据与 SHA

- 构建：`npm run build` 成功（修复后重建，dist 2026-09-14 00:10），MarketingActivity 独立 chunk 约 10.25 kB / 后台 Marketing chunk 约 24.0 kB；真实照片进入 dist/site-photos。
- 证据目录 `docs/协作/Trae/marketing-h5-ui-39/evidence/`（sha256 前 16 位）：
  - 01-live-firstscreen-390.png `0577e4e08329d83e`｜02-paused `53f2400f95663c1b`｜03-expired `19261a42c0c68d91`｜04-notfound-draft404 `caf9e623bae9b310`
  - 05-network-error `a6844fcbb2647927`｜06-retry-success `4b4f9a4b1fdaa9fb`｜07-inquiry-form-filled `8cb0cb646dc0d554`｜08-inquiry-success `33c3fc88dc6f0e37`
  - inquiry-request-bodies.json `7fee7ddf7ba8b630`（无 storeId、同 requestId）｜browser-evidence-result.json `f8eaf3b9bf0d20de`｜browser-evidence-run.log `e0782d253aedb8cc`｜vitest-run.log `7f7c0d7e948b5e87`｜collect-browser-evidence.mjs `97053b5741b91531`｜run-and-clean-log.mjs `7df0db1e54fdb0e2`
- SHA：R0 实现 `fafb65f0f747ac90e1f650dff84e5c6f9c0713a0`（评审对象 20c0a74c）；R1 修复 `d9cb3c0c3c9d78381a05872d1dc73e139fa594aa`；推送后远端核对结果见任务板 event 记录（ls-remote 输出）。
- 合成数据声明：证据中手机号 13800000001、称呼「王女士」均为合成数据；无 JWT/真实顾客资料落盘。

## 7. 建议下一步（交后端任务）

1. 按设计第四节实现活动 CRUD/审批/发布/暂停/取消与公开快照，错误码与 404 口径与本前端 `readPublicActivity` 对齐。
2. booking-inquiry 凭 sourceCode 反查门店并做 requestId 服务端幂等，返回 inquiryNo/lookupUrl（**前端成功页已按 lookupUrl 直连渲染，后端务必返回完整可访问路径**）。
3. 接通后将本套 adapter/route 合同切到真实端点回归；归因接口返回 views/inquiries/bookings/arrivals 后空态自动转数据。

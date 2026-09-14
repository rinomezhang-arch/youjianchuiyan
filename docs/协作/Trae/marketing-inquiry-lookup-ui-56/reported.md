# TR-MARKETING-INQUIRY-LOOKUP-UI-56 reported — 营销咨询号+手机号自助回查 H5（前端候选）

- 执行者：trae（solo）｜统筹：Codex
- base_sha：`114f66340b24a1adebfcc39d0e8d158616fb1f9c`
- 分支：`codex/trae-marketing-lookup-ui-56`（工作树 `f:\solo\artifacts\team-worktrees\trae-marketing-lookup-ui-56`）
- 结论：**PASS（前端候选范围）**。【注意】 **MOCKED_CONTRACT**：后端 TL55（TR-MARKETING-INQUIRY-API-55）R1 为 CHANGES_REQUESTED 未 reviewed，本任务全部接口/浏览器证据来自任务卡契约 mock，不冒充真实闭环；TL55 通过后另做真实联调。

## 1. 交付（严格落在 5 个 allowed_paths）

| 文件 | 说明 |
| --- | --- |
| `frontend_v3/src/api/marketing.js` | 追加回查契约层：`lookupBookingInquiry`（POST `/public/booking-inquiry/lookup`，请求体仅 `{inquiryNo, phone}` 两键）、`normalizeInquiryLookup`（白名单规范化：inquiryNo/status/statusKind/statusText/expectedDate/partySize/submitTime，converted 才有 bookingId；多余字段一律丢弃）、`INQUIRY_STATUS_TEXT`（pending=处理中/converted=已转预订/rejected=未通过，未知=状态未知）、`isValidLookupPhone`（与咨询提交同规则）。文件头 MOCKED_CONTRACT 声明。 |
| `frontend_v3/src/router/index.js` | 仅追加顶层公开路由 `/h5/inquiry/:inquiryNo`（name=PublicMarketingInquiryLookup，免登录，不挂 requiresAuth，history 模式深链接可刷新/直开）。 |
| `frontend_v3/src/views/site/MarketingInquiryLookup.vue` | 新增客人回查 H5：路由 inquiryNo 页面可见并可复制；客人只输手机号；四态界面（查询中骨架/统一空结果/系统错误可重试/成功白名单卡片）；重复点击只发一次；沿用深绿 #2d4a3e、柔白 #f5f2ea、克制金 #c4a35a 视觉。 |
| `scripts/trae-marketing-inquiry-lookup-ui-56/**` | 56 专属测试与证据采集：`api-contract.test.mjs`（11 项）、`sfc-parse.test.mjs`（11 项）、`mock-server.mjs`（合同假后端+dist 静态服务+SPA fallback）、`browser-evidence.mjs` + `run-browser-evidence.ps1`（21 项浏览器证据）、loader/register/request-stub（node 加载钩子替换 '@/utils/request'，零安装零网络）。node_modules 为目录联接复用既有依赖，package.json/lock 零改动。 |
| `docs/协作/Trae/marketing-inquiry-lookup-ui-56/**` | started.md、本报告、evidence/（8 张 390×844@2x 截图、结果 JSON、请求体 JSON、运行日志）。 |

未触碰：任何后端/迁移/依赖/全局样式/配置/部署/法务；未改 MarketingActivity.vue 与其他业务页面。

## 2. 任务卡验收点矩阵

| # | 验收点 | 结论 | 证据 |
| --- | --- | --- | --- |
| 1 | 路由 inquiryNo 页面可见并参与查询；客人只输手机号；无登录/门店/内部 ID | PASS | 浏览器 01/03：直开与刷新后 `inquiry-no`=链接参数；页面唯一输入为手机号；顶层公开路由 |
| 2 | POST 请求体 `{inquiryNo, phone}`；手机号不进 URL/query/存储/控制台 | PASS | API A1-A4；浏览器 17-20；证据文本 0 手机号明文（掩码 138****0001 形态） |
| 3 | 成功只展示白名单（inquiryNo/状态/期望日期/人数/提交时间/converted 的 bookingId） | PASS | API C1/C2（mock 故意多给 remark/operator/store 字段被丢弃）；浏览器 07 |
| 4 | 四态中文；查无/手机号不符/非法输入同一不泄露结果；系统故障独立错误态且保留输入 | PASS | API B1-B3/C3；浏览器 06/09/10/11/12/13/14；查无与手机号不符文案逐字相同（浏览器 12） |
| 5 | 刷新与深链接直开可用；五态界面稳定；重复点击只发一次 | PASS | 浏览器 02/03/05/15/16（双击新增请求数=1） |
| 6 | 390px 无横向滚动；咨询号可复制；按钮输入可触达；视觉沿用 | PASS | 浏览器 04（clipboard=INQ9001）、21（5 个状态页 scrollWidth≤390）；纯色 CSS 无新框架 |
| 7 | API/页面测试覆盖九类场景；证据无手机号/密钥 | PASS | api-contract 11/11 + sfc-parse 11/11 + 浏览器 21/21；证据 JSON/日志扫描 0 明文 |
| 8 | 只跑 56 专属测试、SFC 解析和一次生产构建；mock 证据标 MOCKED_CONTRACT | PASS | 仅上述三个运行；`npm run build` 通过 1m49s（`MarketingInquiryLookup-*.js 4.8kB/css 4.1kB`）；全部证据标注 MOCKED_CONTRACT |

## 3. 精确数字

- API 契约测试：**11 passed / 0 failed**（`node --import ./register.mjs api-contract.test.mjs`）
- SFC 解析与隐私纪律测试：**11 passed / 0 failed**（`node sfc-parse.test.mjs`）
- 浏览器证据：**21 passed / 0 failed**（playwright@npx 缓存 + msedge headless + 390×844@2x + mock-server）
- 生产构建：**1 次通过**（唯一一次 `npm run build`，vite 5）
- 手机号明文扫描（证据 JSON/日志）：**0 命中**

## 4. MOCKED_CONTRACT 边界

- `/api/public/booking-inquiry/lookup` 由 `mock-server.mjs` 按任务卡契约提供：场景表 INQ9001-9004（pending/converted/rejected/未知）、INQERR*（500 系统错误）、其余+号码不匹配→`data:null` 统一空；响应故意携带 remark/operatorName/storeName 以证明前端白名单强制。
- 后端真实语义（稳定咨询号 INQ{id}、手机号双因子、幂等与 5xx 语义）以 TL55 reviewed 后为准；本页契约层 `normalizeInquiryLookup` 已按白名单防御多余字段，真实联调时 UI 不需改（除非 TL55 复核改变字段名）。
- 浏览器证据中手机号为测试占位号（138****0001 等掩码形态入证据），非任何真实客人数据。

## 5. 提交与上板

- 实现与证据提交：见分支 `codex/trae-marketing-lookup-ui-56`（推送后以远端为准）。
- COS 板：claim（CLAIM_VERIFIED）→ started（EVENT_VERIFIED）→ 本 reported。

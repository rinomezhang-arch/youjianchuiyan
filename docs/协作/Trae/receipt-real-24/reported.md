# TR-RECEIPT-REAL-24 reported（Trae）— 账单实际打印与保存 PDF 闭环

## 结论

- **真实链全绿：浏览器真实点击 → 真实后端快照（Result JSON）→ 安全 textContent 预览 → 来自预览页的 PDF**。
- 驱动结果：**pass=26 fail=0 skip=1**（唯一 skip：物理打印机出纸——环境无实体打印机/网络打印服务，链路止于浏览器 window.print 预览，PDF 已由预览页另存）。
- 后端小票针对性测试：**BillReceiptTest 12/12 通过**（surefire）。
- 只读核对：订单/明细行数前后不变（2/2 → 2/2），6 项孤儿/跨店计数全 0；未 DROP/DELETE/重灌，共享 13317 MySQL 未重启。

- 卡片：docs/协作/Codex/TR-RECEIPT-REAL-24.json；base **7c782b86ca618175af887e835c79b58bd6ec653c**
- 工作树：F:/solo/artifacts/team-worktrees/trae-receipt-real-24（分支 codex/trae-receipt-real-24）
- 代码提交 SHA：**494c3d625776aa4f5aec7abe29f380552618c3cb**（报告紧随其后单独提交）
- schema：co_print23_20260909_022305（复用 CO-RC15-PRINT-23 保留库）；订单 COPRINT23-BK-001（2×35 + 1×30 = 100.00）
- 后端 18083（-Xmx512m）、静态/代理 5184；Maven -Xmx256m；单一浏览器上下文（Playwright 1.61.1 + msedge headless）

## 变更文件（全部在卡片允许路径内）

| 文件 | 说明 |
| --- | --- |
| banquet_project/src/main/java/com/youjian/banquet/controller/BillController.java | 新增 `GET /api/bills/{bookingId}/receipt?storeId`（Result JSON）；ReceiptAccessException → HTTP 状态码 + Result.error；列表 SQL 完整版退化核心版以兼容精简 schema；每行补真实 storeId；明细排序改用两端通用的 dish_booking_id |
| banquet_project/src/main/java/com/youjian/banquet/service/BillReceiptService.java | 新增。JdbcTemplate 只读快照（订单/门店/日期/人数/明细/账面总额/应付 finalAmount/支付与订单状态）；金额 BigDecimal；finalAmount 标注「应付快照，不代表实收」 |
| banquet_project/src/test/java/com/youjian/banquet/controller/BillReceiptTest.java | 新增。standalone MockMvc + mock JdbcTemplate + UserContext，12 用例 |
| frontend_v3/src/views/dashboard/BillManage.vue | printBill 删除虚假成功提示，改为 openReceiptPreview(bill)；行内「打印」与详情弹窗「打印账单」两处按钮均走真实链 |
| frontend_v3/src/utils/billReceipt.js | 新增。用户手势同步栈内先开窗（防弹窗拦截）→ request.get 显式 storeId 取快照 → 全部业务文本 createElement+textContent 渲染（无 innerHTML 业务数据）→ 80mm 样式 + 「打印 / 另存为 PDF」按钮调 window.print；拦截/失败/关闭均无成功提示 |
| scripts/release/receipt-real-24/** | run-receipt-real-24.ps1（可重放，纯 ASCII、Start-Process 取退出码、运行时发现中文目录）、seed-tr24-accounts.sql（幂等 TR24 合成账号，仅本 schema）、serve-tr24-web.mjs（dist+/api 同源代理）、tr24-receipt-browser.mjs（真实点击驱动） |
| docs/协作/Trae/receipt-real-24/** | started.md、本报告、evidence/（result.json、network-redacted.json、db-assertions.json、tr24-receipt.pdf、tr24-receipt-preview.png、各进程日志） |

## 身份与门店收口（服务端，不采信客户端金额/身份）

- 无 JWT → **401**；GM 缺省/0/all/非正整数 storeId → **400**；普通员工显式跨店或传 all → **403**；同店未知订单 → **404**。
- 身份只从 UserContext 解析；订单按 booking_id + store_id 共同查询。未复制旧 scope 失效放宽行为；未动全局 request 拦截器/鉴权/网关。

## 验收对照

1. **真实点击**：以 coprint23_manager 真实登录 → /dashboard/bill-manage → 点击行内「打印」，context 收到新页面，网络面板出现 `GET /api/bills/COPRINT23-BK-001/receipt?storeId=1` **200**；无 setContent 假渲染。✅
2. **金额一致**：预览渲染订单号、COPRINT23合成门店、COPRINT23红烧肉×2、COPRINT23时蔬×1、¥100.00；DB 回读明细 70.00+30.00=100.00，与 total/final 一致。✅
3. **HTTP 矩阵**（4 个真实登录账号取 JWT，不伪造 token）：401×1、GM 400×4、404×1、403×2、200×3 全部符合。✅
4. **XSS**：route 把菜名替换为 `<img src=x onerror=...><script>...` 载荷，预览页 img=0、script=0、载荷原文以文本显示、window.__xss/__xss2 均 undefined。✅
5. **失败路径无成功提示**：弹窗被拦截（window.open→null）提示「预览窗口被浏览器拦截」且 success toast=0；后端注入 code=500 时预览窗显示「小票加载失败」、主页单条错误 toast、success toast=0（修复了拦截器与业务层重复弹两次错误的问题）；两处打印按钮均验证。✅
6. **PDF 来自业务预览**：tr24-receipt.pdf 与 tr24-receipt-preview.png 均在预览弹窗页生成；流程前后订单/明细行数 2/2 不变，6 项孤儿/跨店计数全 0。✅
7. **范围克制**：仅跑 BillReceiptTest 与最终候选构建；未重跑工资 13/iPad 16/打印配置 15；未重装依赖；启动前检查 13317/18083/5184/虚拟内存；未重启共享 MySQL。✅
8. **失败先存证再针对性修复**：本轮三处真实缺陷均按「首个错误 + 最小反例 → 明确修改 → 仅复试该项」处理：
   - 预览窗停留 about:blank：`window.open(..., 'noopener=yes')` 使返回值为 null，无法写入窗口 → 移除 noopener（窗口内容全为我方 textContent 写入，无注入面）；
   - 登录选择器 strict 冲突：登录页存在离屏诱饵输入框 yj-user/yj-pass → 改用真实字段 yj-account-input/yj-pwd-input；
   - 详情弹窗 el-dialog 头部 X 与底部「关闭」同名导致关闭点击被严格模式吞掉、遮罩挡住后续点击 → 精确点击 .el-dialog__footer 关闭按钮 + Escape 兜底 + 等待 detached。

## 未验证 / 剩余项

- **物理打印机出纸未验证**（环境无实体打印机/网络打印服务）：window.print 的系统打印对话框在 headless 下不交互；PDF 已证明预览页可打印性。接入实体打印机后需补一次出纸验收。
- TR24 合成账号（tr24_gm/tr24_staff1/tr24_staff2，staff_id 923101-923103，密码复用 123456 BCrypt）仅写入 co_print23 保留 schema，幂等种子，可随时复跑。

## 复放方式

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File `
  scripts\release\receipt-real-24\run-receipt-real-24.ps1 `
  -PlaywrightModule "C:\Users\rinom\AppData\Local\npm-cache\_npx\e41f203b7505f1fb\node_modules\playwright"
```

脚本自动：幂等种子 → BillReceiptTest → 打包 → 18083 后端 → 5184 代理 → Playwright 驱动；结束自动停自己起的 java/node（共享 MySQL 不动）。本次权威复跑输出 **TR24_RUNNER_OK**，驱动 **TR24_RECEIPT_RESULT pass=26 fail=0 skip=1**。

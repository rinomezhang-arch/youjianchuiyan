# TR-RECEIPT-REAL-24 reported r2（Trae）— changes_requested 四缺口返修

返修单：docs/协作/Codex/TR24-review-r1-20260909.md；受审 HEAD 5e5fef40（r0：pass=26 fail=0 skip=1，独立 BillReceiptTest 12/12 保留不倒改）。
继续同树 F:/solo/artifacts/team-worktrees/trae-receipt-real-24（分支 codex/trae-receipt-real-24，base 7c782b86）。

## 结论

- **真实链全绿：驱动 pass=32 fail=0 skip=1**（唯一 skip 仍为物理打印机出纸；插桩证据只声明 window.print 被业务按钮调用，不谎称原生对话框/设备成功）。
- 后端定向测试 **BillReceiptTest 14/14 通过**（r1 12 例 + 新增无绑定 null、列表异常不降级 2 例；原 12 例断言增强桌台聚合）。
- 权威复跑走**默认只读路径**（预检无任何写库）：输出 TR24_RUNNER_OK，唯一 run 目录 `docs/协作/Trae/receipt-real-24/evidence/runs/20260909-092824-bca3/`；r0 证据（evidence/ 根目录 result.json/pdf/png 等 16 个文件）原样保留未动；两次中途失败的 run 目录同样保留可查。
- 只读核对：订单/明细行数前后 2/2 不变，6 项孤儿/跨店计数全 0；未 DROP/DELETE/重灌，共享 13317 MySQL 未重启，未连生产/法务。

- 代码提交 SHA：**52f5df62**（桌台贯通/去降级/真实点击/seed 与运行器收紧，8 文件 +402/-54）
- 构建 manifest 提交：**023c3d55**（build-manifest.json 绑定 sourceHead=52f5df62、distHash=b2853bd6…、distFileCount=336、builtAt=2026-09-09T01:07:12Z）
- 驱动断言修正与本报告随后续提交一并给出（见提交日志）。

## 缺口 1：桌台贯通 DB → JSON → 预览 → PDF

- BillReceiptService.receipt() 新增 `SELECT table_name FROM booking_table WHERE booking_id=? AND store_id=? ORDER BY table_booking_id`：去空白/去重后多桌以「、」**稳定聚合**为 tableName（另出 tableNames 数组），无绑定才为 null，不伪造。
- 前端 billReceipt.js meta 段新增 `appendMetaRow(doc, meta, '桌台', r.tableName)`（appendMetaRow 本就跳过空值，无绑定不渲染该行）。
- 证据（run 20260909-092824-bca3）：
  - JSON：`tableName="TR24-01号桌、TR24-02号桌、TR29-A、TR29-B"`，tableNames 同序（TR24 两桌排在前序）。注：共享隔离库中 TR29 卡按只增原则加入了同单 TR29-A/B 夹具，本卡不断言、不删除他卡数据，仅按 TR24 标识断言本卡两桌。
  - DB：booking_table 同单 TR24 绑定 2 行（924001 TR24-01号桌 12:00:00 / 924002 TR24-02号桌 12:30:00）。
  - 预览/PDF：业务预览渲染聚合桌台字符串（行内「打印」与详情弹窗「打印账单」两入口均断言），PDF 与截图来自同一预览 popup。

## 缺口 2：移除生产 catch-all SQL 降级

- BillController.listBills 删除 `try{fullSql}catch(Exception){coreSql 降级}` 整段，只保留完整 SQL；查询异常由外层 catch 如实返回 `Result.error(500, "查询账单列表失败…")`，不再产出桌台/支付方式/经手人/时间为 null 且排序变化的“成功”账单。
- 新增单测 `listBillsQueryFailureDoesNotDegrade`：jdbc.queryForList 抛异常时断言 code=500、data=null、响应体不含订单号。
- 缺表/缺列改由隔离夹具补齐（见缺口 4）。**返修过程中该降级曾掩盖一个真实环境问题**：夹具初版 finance_transaction 沿用标准库 utf8mb4_unicode_ci，与隔离库默认 utf8mb4_0900_ai_ci 跨表比较触发 MySQL 1267（fullSql 的 ft.related_no=b.booking_id）；r1 靠 catch 降级“成功”掩盖。已在夹具侧修正（表 CONVERT 为 0900_ai_ci，夹具脚本同步改为不强制 unicode_ci），**生产 SQL 一字未动**（生产整库统一 unicode_ci，行为不变）。

## 缺口 3：真实点击打印按钮 + 插桩证据 + 80mm PDF

- 驱动在预览 popup 内先插桩 `window.print` 计数，再**实际点击业务按钮「打印 / 另存为 PDF」**，waitForFunction 等到计数 ≥1：`printCalls=1`。证据措辞严格限定为“业务按钮调用了 window.print 的插桩计数”，native 打印对话框/实体出纸不声称成功。
- 80mm 三重证据：① 预览内联样式原文 `@page { size: 80mm auto; margin: 0; }`；② PDF 以 width=80mm 由同一 popup 生成；③ 解析 PDF `/MediaBox`：**widthPt=227.04**（80mm=226.77pt，容差内）。
- 物理出纸继续 SKIP（环境无实体打印机/网络打印服务）。

## 缺口 4：seed/运行器收紧

- seed-tr24-accounts.sql：**删除硬 DELETE**，改为纯 INSERT（主键冲突即报错拒绝，不覆盖）；默认复放不再执行种子。
- 新增 init-tr24-fixtures.sql（仅新增、可重复执行）：信息架构守卫的 ADD COLUMN（booking_master 补 booking_time/staff_name/updated_at）、CREATE TABLE IF NOT EXISTS（table_master/booking_table/finance_transaction，定义摘自 banquet_full_schema.sql；booking_table 含标准 FK）、NOT EXISTS 守卫的 TR24 桌台夹具（table_master 924001/924002，booking_table 924001/924002 绑定 COPRINT23-BK-001）。
- 运行器：默认**只读预检**（3 个 TR24 账号存在、标准表/列存在且定义不符即停止不覆盖、TR24 绑定≥2）；仅显式 `-InitSeed` 才执行只增初始化（账号已存在则跳过种子 INSERT，冲突不覆盖）。
- **构建绑定**：build-manifest.json 记录 sourceHead（52f5df62）+ dist 全量文件哈希（distHash=b2853bd6…，336 文件）；运行器启动前重算 dist 哈希比对，并 `git diff --quiet <sourceHead> -- frontend_v3 banquet_project`，不匹配直接失败。本轮两闸门均 OK。
- **唯一 run 目录**：每次复放写入 evidence/runs/yyyyMMdd-HHmmss-<rand>/，结果/日志/PDF/截图全部落该目录，旧证据不覆盖（本轮根 evidence 目录 r0 文件未动）。
- Order 参数统一：驱动内 HTTP 矩阵/DB 断言全部改用 TR24_ORDER 变量，消除硬编码 'COPRINT23-BK-001' 与伪可配置并存。

## 隔离库 DDL/DML 清单（先列后执行，已在报告保留）

DDL（co_print23_20260909_022305，只增不改旧数据）：
1. `ALTER booking_master ADD COLUMN booking_time time / staff_name varchar(20) / updated_at timestamp`（信息架构守卫，缺则加）；
2. `CREATE TABLE IF NOT EXISTS table_master / booking_table / finance_transaction`（标准定义；booking_table 含 fk_bt_booking_master→booking_master(id)、fk_bt_table→table_master(table_id)）；
3. 运行中发现 booking_table 由 TR29 卡先建（FK 名为 fk_booking_master/fk_bt_table），列定义兼容本卡查询，预检通过未重建；
4. finance_transaction 建表后排序规则与库默认冲突，执行 `ALTER TABLE finance_transaction CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci`（空表、本卡夹具表，无旧数据影响）。

DML（全部 NOT EXISTS / 标识守卫，未触碰任何非 TR24 行）：
- table_master +2：924001 TR24-T01/TR24-01号桌、924002 TR24-T02/TR24-02号桌；
- booking_table +2：924001/924002 绑定 COPRINT23-BK-001（store 1，12:00/12:30）；
- TR24 账号 923101/923102/923103 为 r0 既有，本轮默认复放只读校验，未重种。

## 产物（run 20260909-092824-bca3，sha256）

- tr24-receipt.pdf `6aa10deb28a30d2068c013153309137fe999c55017b6d77f60c77df8760a2678`（MediaBox 227.04pt≈80mm）
- tr24-receipt-preview.png `cea2f356b89a8048bce8ede032ac901b96297ce9c4a042cba66f2717f83261d9`
- result.json `8d9a1570e658cbbe4f87216e5f3568c876f426edc9a247d2cccbdcd9f46f75a3`（pass=32 fail=0 skip=1）
- db-assertions.json `007cccdc24595fd5809ddbe910cb5a4e21d2157dad8b6d9de46055fc47d3ebff`
- network-redacted.json `9f6f7bf35877daec7a946d635789514f47577b45da008af800fe5ce39f6f772f`

## 范围克制

- 未重跑工资/iPad/旧打印配置；CL25 发布脚本未触碰；未动全局鉴权/拦截器/网关；后端 18083 -Xmx512m、Maven -Xmx256m、单浏览器上下文（Playwright 1.61.1 + msedge headless）。

## 复放方式

```powershell
# 默认只读复放（预检只读校验账号/夹具、manifest 绑定校验、唯一 run 目录）：
powershell -NoProfile -ExecutionPolicy Bypass -File `
  scripts\release\receipt-real-24\run-receipt-real-24.ps1 `
  -PlaywrightModule "C:\Users\rinom\AppData\Local\npm-cache\_npx\e41f203b7505f1fb\node_modules\playwright"
# 仅首次/缺夹具时显式初始化（只增、冲突不覆盖）：追加 -InitSeed
```

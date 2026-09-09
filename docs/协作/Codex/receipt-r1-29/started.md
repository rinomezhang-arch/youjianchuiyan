# CO-RECEIPT-R1-29 开始记录

执行人：Codex；时间：2026-09-09 08:42:43 +08:00。
工作树：F:/solo/artifacts/team-worktrees/codex-receipt-r1-29；分支 codex/receipt-r1-29；基线 5e5fef40fe0962745d46bfaff8d6edc1f4aef982，开始时干净。
用户独占授权，原 Trae 树不动，不派助手，不写总登记簿。

准备修改：BillController.java 去除 SQL 降级；BillReceiptService.java 读取同订单同店桌台；BillReceiptTest.java 补桌台、SQL 双键与禁止降级测试；billReceipt.js 显示桌台及 80mm 样式；scripts/release/receipt-real-24/ 修正真实点击 print、纸宽、默认只读夹具检查、唯一证据与 HEAD/源码/dist 绑定；本人目录记录结果、证据。BillManage.vue 仅必要时修改。
验证限制：一次 BillReceiptTest（MAVEN_OPTS=-Xmx256m）、一次最终前端构建、一次修正后真实驱动；后端只启动一份 -Xmx512m，先确认 18083/5184 空闲。依赖复用，不安装。
依赖联接意图：仅在新树 frontend_v3 创建指向原树现有 node_modules 的联接，构建缓存/配置临时文件放新树，不改原树源码。需要后端产物时在新树 package -DskipTests，不重跑测试。
DDL 意图：先只读检查已有 co_print23 隔离 schema 和新树项目标准 DDL；只追加列表必需的原项目缺列/缺表以及 TR29 桌台合成关联，不 DROP、DELETE、重灌、更新或覆盖旧行。不访问其他库、生产/法务或凭证。具体 DDL 执行前追加列/表、来源与影响范围。
证据追加至唯一运行目录，不覆盖原证据。15 分钟内给代码 SHA 和剩余。

## 08:46 DDL/DML 意图（执行前）
来源 banquet_project/banquet_full_schema.sql:443、448、463、491-516、1277-1304、3591-3610。现场已确认这些列/表不存在。
- booking_master 仅 ADD booking_time TIME NULL、staff_name VARCHAR(20) NULL、updated_at TIMESTAMP NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP；不改已有列与已有业务值。
- CREATE 项目原定义 table_master、booking_table（保留对主单和桌台外键）、finance_transaction（空表，不造流水）。不执行原 dump 的 DROP 或数据段。
- 仅 INSERT 新 TR29-A/TR29-B 桌台与它们对已保留 COPRINT23-BK-001 的两条关联；显式 table_id/table_booking_id 929001、929002，绑定主单的真实 id、store_id/date。先验证 ID 和标识无冲突，冲突拒绝。未新增/更新/删除主单、明细、账号、收款记录。
- 执行前保存原主单/明细既有字段的散列与行数；执行后复核不变。新 updated_at 的默认值属于新列，不当作历史真实结算时间。
- DDL 非事务性，任一步失败停止并保留真实执行日志，不自动回退或重灌；所有 SQL 保存 scripts/release/receipt-real-24/prepare-tr29-fixture.sql。

## 阶段结果
- DDL 成功追加 3 张原标准表、3 列，新增 2 个桌台和 2 条合法关联；既有主单/明细字段 SHA-256 前后相同，详见 ddl-result.json。没有重跑种库脚本。
- 唯一一次 BillReceiptTest：15 tests，0 failures，0 errors，0 skipped，4.713 秒；命令退出 0。新增断言覆盖双键参数、多桌/无桌语义和列表超时不降级。
- 唯一一次前端构建已启动；使用新树 dist（此前不存在）且 emptyOutDir=false，避免清理已有文件。接下来 skipTests 打包已测后端、提交代码、绑定产物，再运行一次真实驱动。

## 构建结果
- 最终前端构建仅一次，退出 0，30.49 秒；仅既有大 chunk 提示。后端离线 package -DskipTests 一次，退出 0，不重复测试。
- 本阶段准备提交 B1-B4 源码、DDL 留档与定向测试摘要；受审产物绑定和一次真实链结果在随后追加证据提交。

## SQL 预检异常与追加意图
完整账单查询 EXPLAIN 失败：MySQL 1267，finance_transaction.related_no 标准 utf8mb4_unicode_ci 与保留 booking_master.booking_id 的 utf8mb4_0900_ai_ci 比较冲突。驱动尚未运行，服务尚未启动，manifest 未创建。
不改既有列/表及业务数据；仅在 BillController.java 的流水 related_no=booking_id 关联右侧显式 COLLATE utf8mb4_unicode_ci，与原流水字段定义一致。此改动为去除降级后恢复标准完整查询所需，后续真实链验证列表可用。一次 targeted test 已完成于此调整前，不重跑；前端未变不重建；需再次 skipTests package 编译这一行并更新来源提交。

## 真实链结果与收尾意图
源码提交 ba1a9b94a438cb2034f3b6b4dbd58b495289b0dc；唯一真实链结束 2026-09-09 08:53:30 +08:00，34 PASS / 0 FAIL / 1 SKIP，退出 0。
唯一证据：evidence/20260909-085149683-8887f654。多桌 TR29-A、TR29-B 贯通 DB/JSON/DOM；实际按钮委托原生 print calls=1、returned=1、beforeprint=1、afterprint=0。PDF 一页宽 80.094663mm；不宣称原生对话框/实体出纸成功。
业务字段和桌台关系散列前后相同。仅停止任务自建 PID，结束时 18083/5184 无监听。
收尾统计命令首次 PowerShell 解析失败，未产生文件或数据库变更；改用 Python 后读取成功。
接下来新增 reported.md，提交本轮 manifest、证据、日志与结果。无源代码再改、无追加测试/构建/驱动、不写总登记簿/任务板，不发消息或派工。

最终收尾：来源绑定复核通过，本人代码未再变；只读发现原 Trae 树另有未提交补丁，已在 reported.md 如实记录，不读补丁、不合并或覆盖，不判定来源时间。仅追加本条记录并提交。

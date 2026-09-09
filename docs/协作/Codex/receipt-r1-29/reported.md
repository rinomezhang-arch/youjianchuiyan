# CO-RECEIPT-R1-29 实现交付

状态：reported，交统筹验收；未修改外部任务板。
工作树 F:/solo/artifacts/team-worktrees/codex-receipt-r1-29，分支 codex/receipt-r1-29。
基线 5e5fef40fe0962745d46bfaff8d6edc1f4aef982。
源码提交：d0804f591c99879de8bf2638f5d952cfe814e2f4、ba1a9b94a438cb2034f3b6b4dbd58b495289b0dc；最终受测/绑定源码为后者，随后提交仅追加本人记录和证据。

已完成 B1-B4；无剩余实现阻断。验收范围为真实本地 HTTP/数据库/浏览器预览、headless 原生 print 调用和 PDF 导出，不包含原生打印对话框或实体出纸。

| 修改路径（均相对于上述工作树） | 结果 |
| --- | --- |
| banquet_project/src/main/java/com/youjian/banquet/service/BillReceiptService.java | 桌台按订单+门店过滤，依 table_booking_id 稳定聚合；名称缺失取桌号/桌台ID；无绑定才返回 null |
| banquet_project/src/main/java/com/youjian/banquet/controller/BillController.java | 移除 catch-all SQL 降级；流水单号关联显式统一排序规则，解决标准隔离表的 MySQL 1267 |
| banquet_project/src/test/java/com/youjian/banquet/controller/BillReceiptTest.java | 增加多桌/无桌、三类查询双键参数、一次超时查询不能降级成功的断言 |
| frontend_v3/src/utils/billReceipt.js | 安全 DOM 展示桌台；有效 @page 80mm x 200mm |
| scripts/release/receipt-real-24/tr24-receipt-browser.mjs | 实际点击业务打印按钮并委托原生 print；检查 PDF MediaBox；桌台、来源绑定正/反例、业务字段散列；固定保留样本；排他写证据与失败记录 |
| scripts/release/receipt-real-24/run-receipt-real-24.ps1 | 默认不种库/不构建/不重测；启动前检查端口、只读账号夹具、HEAD/source/dist/jar 绑定；每次唯一目录，仅停止自建进程、恢复进程环境变量 |
| scripts/release/receipt-real-24/seed-tr24-accounts.sql | 保留旧文件名，内容改为 SELECT-only 夹具检查，无账号删除或覆盖 |
| scripts/release/receipt-real-24/receipt-artifacts.mjs | 新增可复核的源码提交/源码文件/336个dist文件/JAR散列绑定；旧manifest不可覆盖，不匹配拒绝 |
| scripts/release/receipt-real-24/prepare-tr29-fixture.sql | 本轮已执行的显式一次追加 SQL，非默认复放入口，保留原项目表定义和外键 |
| docs/协作/Codex/receipt-r1-29/** | started、DDL前后结果、测试/构建日志、复放说明、manifest、真实链证据及本结果 |

BillManage.vue 和静态服务 serve-tr24-web.mjs 无需修改。新树 frontend_v3/node_modules 联接复用既有依赖，未安装依赖；原 Trae 树源码、配置和原证据未修改。

**实际数字**

- BillReceiptTest 仅执行一次：15 tests / 0 failures / 0 errors / 0 skipped，4.713s。这次在 d0804f59 对应后端逻辑上执行；随后单行 COLLATE 调整未重跑单测，已由 skipTests 重新编译及最终真实列表请求覆盖。
- 前端最终构建仅一次：退出 0，30.49s；有既有大 chunk 提示，未扩大优化。没有第二次前端构建。
- 后端 package -DskipTests 共两次：均退出 0。第二次仅因 SQL 排序规则实际错误修正后需要更新 JAR，不是无改动重试；均未再次执行测试。
- 修正后的真实驱动仅一次：34 PASS / 0 FAIL / 1 SKIP，driverExit=0。包含既有故障注入/XSS检查及来源绑定负例，不把所有断言都称为纯真实业务 E2E。
- 实际点击预览按钮：calls=1、returned=1、beforeprint=1、afterprint=0；包装器调用原生方法。未据此认定原生打印对话框或物理完成。
- PDF 一页，MediaBox 80.094663mm（80mm目标，0.3mm容差）；同一预览有 TR29-A、TR29-B，多桌文字在截图中人工核对。PDF SHA-256：7c7152cc2dabee748aa89483b8c735c6785e0d6116fe600060d7c35efc6b5725。
- DB：只在已保留 co_print23 隔离库追加原标准 3 表、3 列、2 桌台+2关联；原主单/明细字段散列未变。真实链前后业务字段/桌台关系散列相同，6项孤儿/跨店计数均0。不覆盖旧账号、不重灌、不 DROP/DELETE。
- 进程结束：runner 仅停止本任务自建 Java/Node；结束后 18083/5184 无监听。

**来源及唯一证据**

唯一运行目录：docs/协作/Codex/receipt-r1-29/evidence/20260909-085149683-8887f654。
运行时间：2026-09-09 08:51:49 至 08:53:30 +08:00。
sourceHead/runtimeHead：ba1a9b94a438cb2034f3b6b4dbd58b495289b0dc。

- build/manifest.json：source SHA-256 1f10e1ba98240ee44431b42548c0a77132304f42d60aa12a09c41b852c198c4c。
- dist SHA-256：0defd93e8b6459c1aa47bbff3af557afffa5f4f5da000054342ad5a90d4159c4，336 文件。
- JAR SHA-256：d99943420714e4827bb93521939567362b36f0e65272d41d84ffa9c9b4e62253。
- evidence 下 result.json、print-evidence.json、db-assertions.json、network-redacted.json、run.json 和 PDF/PNG 提供逐项实际值；binding-negative-control.json 留存拒绝旧产物声明的反例。
- surefire.txt、mvn-test.log、frontend-build.log、mvn-package*.log 提供唯一测试/构建的真实日志。DDL预检第一次1267失败、改表达式后成功，见 started.md 与 list-query-explain.txt。

**未做及范围限制**

- 物理出纸 SKIP；原生打印对话框、打印取消流程、真实设备驱动/切纸、超长订单跨页未验收。
- 登录后的其他仪表盘请求仍产生精简隔离库缺 customer_master / banquet_name 的背景错误，本轮未扩建无关模块；34 PASS 仅覆盖本任务检查，不能宣称全业务环境无错。
- 未改生产、法务、别人的树、权限、总登记簿或外部任务板；未推送/发布；未再派助手。
- 下一步由统筹取本分支/提交验收并决定集成；用户已指定总登记簿与后续派工由统筹处理。

## 最终只读核对（2026-09-09 08:55:34 +08:00）
证据提交 7ed9ce1309d2d9a920194204399a6395efd4f3cf 后，source/dist/jar 绑定复核通过；相对 ba1a9b94 的代码/脚本差异为空，本人工作树干净。源码测试与运行产物仍对应 ba1a9b94，不把证据提交当成重新构建。
原 Trae 树 HEAD 仍 5e5fef40，但收尾只读 git status 看到其中已有 6 个修改文件和未跟踪 init-tr24-fixtures.sql。本次所有实现命令均限定新树；没有读取、合并或覆盖该补丁，不推断其形成时间或当前是否仍在执行。此状态仅交统筹知悉，不做额外派工。

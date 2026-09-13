# TR-RECEIPT-REAL-24 r4 返工上报（trae）

## 结论
- 状态：**reported（候选，未发布）**。r4 唯一阻断（MySQL 实例身份无法证明隔离）已按最小返工修复并复跑一次。
- 返工前工作树 HEAD：`36a50289`（干净）；r4 证据/运行器/本报告提交：`34ee8a76`；最终工作树 HEAD 为紧随其后的 r4 绑定提交（build-manifest.json 写入 r4EvidenceCommit/run/结果），完整 SHA 以任务板 r4 reported 事件为准（沿用 r2/r3 的双提交绑定惯例）。
- 代码/前端/后端**零改动**：本轮只改 TR24 运行器家族两文件，均在允许路径内：
  - `scripts/release/receipt-real-24/run-receipt-real-24.ps1`：新增 `-MysqlPort` 参数（本轮固定 13318）；所有 mysql 调用、种子调用、JDBC URL、驱动环境变量全部走该参数；**硬拒 13317**；任何 schema 访问之前先执行只读身份闸门 `SELECT @@port,@@datadir`，归一化（连续反斜杠折叠、去尾斜杠、小写）后必须严格等于 `13318` 与 `f:/solo/artifacts/mysql-test-13317`，不符立即退出；结束行打印 `mysql=` 端口。另把内存前置阈值按本卡实际串行堆上限（mvn 256m、JVM 512m 不并发）从 2.0GB 调整为 1.0GB 并打印实测值（TR37 已在 ~1.3GB 空闲下验证可构建/启动）。
  - `scripts/release/receipt-real-24/tr24-receipt-browser.mjs`：只读 DB 断言端口改由 `TR24_MYSQL_PORT` 注入（默认 13318），result.json/db-assertions.json 记录 `round:'r4'` 与 `mysqlPort`，便于散列级溯源。

## 闸门与只读事实
- 首次执行闸门即捕获到归一化缺陷（mysql --batch 把反斜杠双写），当场退出，**未访问任何 schema、未启动后端、未写库**（失败尝试 run 目录 `20260913-174107-204f` 仅含日志，驱动在第一条只读基线前即 ReferenceError 退出；修复后再跑）。
- 权威运行输出：`TR24 r4 identity gate OK: @@port=13318 @@datadir=f:/solo/artifacts/mysql-test-13317/`；`Preflight read-only OK: 3 TR24 accounts, standard tables/columns present (no writes)`（未用 -InitSeed，零写入）。
- 本轮全程未连接 13317（运行器对 13317 硬编码拒绝；无任何旁路连接）。

## 唯一权威运行
- run 目录：`docs/协作/Trae/receipt-real-24/evidence/runs/20260913-174415-e4d6/`（唯一，旧证据原样保留未覆盖未删除）。
- 真实链（Playwright + Edge headless，真实登录后点击账单行实际「打印」按钮，非 setContent）：**pass=32 fail=0 skip=1**；skip 为物理打印机出纸（环境无实体打印机/网络打印服务，止于 window.print 业务预览，PDF 可另存）。
- 后端定向测试：`BillReceiptTest` **Tests run: 14, Failures: 0, Errors: 0, Skipped: 0**（surefire 报告原件在 mvn-test 日志与 target/surefire-reports）。
- 候选 JAR 由同一绑定源码重新 package：`banquet_project/target/banquet-1.0.0.jar`，SHA256 `84dd20faf19d4acd4eda9624866429800faf8484f332eb5c1e3ba82cc9181505`。
- manifest 闸门：`sourceHead=52f5df62`、`distHash=b2853bd67c82`（builtAt 2026-09-13T07:20:00Z），`git diff 52f5df62 -- frontend_v3 banquet_project` 为空，前端 dist 重建自完整源码 52f5df62，未使用 013afcf1 旧制品。
- 预览 PDF（来自业务预览页 DOM）：SHA256 `d391b5232b3e124d2f7c732168da5b871e86c9b8f6620e4cca17d45af2bbd2a5`；预览截图：SHA256 `10e7f0ed4e549ac4c43d467b489c8fccfccb6fe0bd91d25c0568a00416c9895a`。
- DB 只读回读（13318 / co_print23_20260909_022305）：COPRINT23-BK-001 两行菜 2×35+1×30=100.00，booking finalAmount/paymentStatus=100.00/paid，TR24-01/02 两桌；流程前后 booking/dish 行数 2/2 不变，6 项孤儿/跨店计数全 0。
- 网络证据 `network-redacted.json` 登录凭据已脱敏；证据不含密码/token 明文。

## 边界
- 未重跑工资、iPad、RC 大矩阵；未部署生产；未触碰法务；未新建/删除任何 schema 或共享数据。
- 物理出纸维持 skip=1，如实未验证。

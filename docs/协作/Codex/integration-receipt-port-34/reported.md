# CO-INTEGRATION-RECEIPT-PORT-34 完成报告

- 实现提交：`4bb8c872ddb38f6f8c2794bf35712516b2e71d0f`
- 运行：`20260913-184846539-f65b8a13`
- 数据库：仅 `127.0.0.1:13318/co_print23_20260909_022305`

## 修复与闸门

- 保留统筹分支现有的 CO-RECEIPT-R1-29 小票实现，没有用 Trae 的旧分支覆盖。
- runner 参数、MySQL CLI、Spring datasource 和浏览器数据库回读统一使用 `MysqlPort=13318`。
- runner 和浏览器脚本只允许 13318，并在连接前拒绝遗留的 13317 连接字面量。
- Java 启动后读取实际进程命令行，确认 datasource 为 `127.0.0.1:13318/co_print23_20260909_022305` 后才继续。

## 验证

- PowerShell 语法、Node 语法、禁止端点扫描和隔离库身份断言通过。
- 前端生产构建通过。
- `BillReceiptTest`：15 tests、0 failures、0 errors、0 skipped。
- 当前源码、前端 dist 和后端 JAR 已由新 manifest 绑定到实现提交。
- 真实浏览器、真实登录、真实 HTTP、小票 PDF、数据库关系与只读回读：34 PASS、0 FAIL、1 SKIP。
- SKIP 仅为没有实体打印机，不能宣称纸张实际出纸；headless 原生打印调用和 80mm PDF 已验证。
- 订单 100.00 元、两行菜品、多桌聚合一致；前后业务数据散列相同；六类孤儿或跨店计数为 0。
- 运行后 18083 和 5184 均无监听；证据中 JWT、Bearer 和密码值扫描为 0。

## 非阻断观察

隔离 schema 没有 `customer_master`，页面后台客户列表请求在后端日志中留下表不存在异常。小票链及其网络断言全部通过，故不退回本任务；该缺口应由后续隔离环境完整性任务补齐。

未连接 13317/3306，未建 schema，未删除数据，未部署，法务未触碰。证据哈希见 `evidence-manifest.json`。

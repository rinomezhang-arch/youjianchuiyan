# CO-RECEIPT-PORT-R5-32 执行报告

状态：完成，待技术验收。

固定基线：`a3b17f19e62816cd58ecd7a5d2e11f7aad1059e6`。独立工作树：`F:/solo/artifacts/team-worktrees/codex-receipt-port-r5-32`。

## 修复

- `run-receipt-real-24.ps1` 的 seed、fixtures 与 Spring Boot datasource 三处连接全部改用 `$MysqlPort`。
- 新增静态闸门：脚本内出现硬编码 `--port=13317` 或 `jdbc:mysql://127.0.0.1:13317` 时，在任何数据库访问前退出。
- 新增进程闸门：Java 启动后读取实际命令行，必须包含 `jdbc:mysql://127.0.0.1:13318/co_print23_20260909_022305`，否则停止执行。

## 权威运行

运行目录：`docs/协作/Codex/receipt-port-r5-32/evidence/20260913-182435-4419/`。

顺序输出已确认：实例闸门通过，`@@port=13318` 且 datadir 为隔离目录；只读夹具预检通过；构建清单通过；Java 进程 datasource 闸门通过；末尾 `TR24_RUNNER_OK mysql=13318`。

真实浏览器和数据库结果：32 PASS、0 FAIL、1 SKIP。SKIP 仅为没有实体打印机出纸；浏览器 `window.print`、80mm PDF、业务预览和保存 PDF 已通过。订单、两行菜品、两张桌台、100.00 元合计一致，前后行数不变，6 类孤儿或跨店计数为 0。

PDF SHA256：`ACD02DC5C9C9C42259794ABA0C52F336ECF79C6262EF86AAFC49D3799DF21044`。截图 SHA256：`CEA2F356B89A8048BCE8EDE032AC901B96297CE9C4A042CBA66F2717F83261D9`。完整清单见 `evidence-manifest.json`。

运行结束后 18083 和 5184 均无监听。证据扫描 JWT 形态 0、Bearer 明文 0、密码值 0。未建 schema、未 seed、未删除数据、未连接 13317/3306、未修改业务源码、未部署，法务未触碰。

生成器沿用的 `result.json` 内部仍显示 `round=r4`，原文件保持不改；本次 R5 的任务、端口、run 路径和原文件哈希由外层 `evidence-manifest.json` 固定，避免改写原始证据。

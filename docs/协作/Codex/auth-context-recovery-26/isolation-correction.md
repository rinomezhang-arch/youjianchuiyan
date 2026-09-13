# CO-AUTH-CONTEXT-RECOVERY-26 隔离环境纠正

- 记录时间：2026-09-13 16:4x +08:00
- 原 64 项报告所用 13317 在运行时指向 `D:/MySQL/Data`，隔离结论撤回，原数字不得作为最终验收证据。
- 本轮只修改新增的真实 iPad 审计测试：要求显式 `YOUJIAN_TEST_MYSQL_PORT` 与 `YOUJIAN_TEST_MYSQL_DATADIR`，连接后先只读校验 `@@port`、`@@datadir`，不符立即拒绝，校验通过后才创建带随机标识的合成 schema。
- 本轮固定使用 Codex 已恢复的 127.0.0.1:13318 与 `F:/solo/artifacts/mysql-test-13317/`。不连接或停止 13317，不改 Windows 服务/配置，不碰生产与法务。

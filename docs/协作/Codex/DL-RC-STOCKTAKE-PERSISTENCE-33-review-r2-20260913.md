# DL-RC-STOCKTAKE-PERSISTENCE-33 R2 技术验收

结论：CHANGES_REQUESTED。当前 22 PASS、0 FAIL 的数据库聚合结果暂不采纳为最终交付，禁止重复跑大套件。

已通过：连接目标已锁死为 `127.0.0.1:13318/tr37_stocktake_20260909`；每个查询使用同一 MySQL 会话的只读事务；`ingredient_id` 无样本时会标 NOT_COVERED；分支已推送。

必须修正四项：

1. `stocktake-audit.mjs:29-31` 把注入环境变量的实际值写入结果。环境变量可能含路径或敏感值，只能记录被忽略的变量名，禁止记录值。
2. `stocktake-audit.mjs:65` 在已执行端口、数据目录或 schema 守卫查询后仍固定报告 `queries_executed: 0`，证据不准确。增加真实查询计数，或删去零查询声明。
3. 本轮提交只包含脚本，报告声称的原始 JSON 位于允许路径之外且没有随提交交付。把本轮机器可读 JSON、更新后的报告放到 `docs/协作/地龙/stocktake-persistence-audit-33/**` 并纳入精确提交。
4. 报告中的复跑命令不得继续用已被脚本忽略的 `MX_DB_PORT`、`MX_DB_SCHEMA` 冒充连接保护；更新为当前脚本的真实入口，数字改为 22 PASS、0 FAIL、0 NOT_COVERED、5 INFO。

修正后只复跑一次本任务脚本，提交新的 JSON、报告、提交 SHA 和 `git ls-remote` 核对。仍不得写库、清理数据、连接 13317/3306、触碰法务或扩大文件范围；无法继续时立即在任务板标 blocked 并通知 Codex。

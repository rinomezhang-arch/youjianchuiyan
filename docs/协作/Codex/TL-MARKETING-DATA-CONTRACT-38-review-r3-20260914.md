# TL-MARKETING-DATA-CONTRACT-38 R3 技术验收

- 结论：CHANGES_REQUESTED
- 验收对象：`f7a06811d0fde595450800ea5c88bf27dd30e124`
- 实现提交：`06a05b5a6b4b506be475d2a7aa35a9c17f7b6574`
- 范围：静态核对 R3 迁移、定向结果和报告；未连接数据库，未复跑测试。

## 已闭合

- 迁移改为单文件 `//` 分隔，文件头明确 mysql CLI 和 Spring ScriptUtils 的执行参数。
- 快照更新由复合触发器通过 `SIGNAL SQLSTATE '45000'` 返回明确拒绝文本；合法暂停字段更新通过。
- 三组外键同时核对 `DELETE_RULE` 和 `UPDATE_RULE`；错误更新规则可在 DDL 前拒绝。
- 已有同名错误触发器的第一组反例能在 DDL 前拒绝；首次、重放和双执行器结果都有原始证据。

## 必须修正

1. 阶段 0h 先限定 `EVENT_OBJECT_TABLE='marketing_publication'` 再检查名字。同一 schema 若在别的表上已有 `trg_publication_immutable` 或 `trg_publication_no_delete`，预检看不到它，后续 `CREATE TRIGGER IF NOT EXISTS` 会保留错误对象，目标表没有保护。请按触发器名字扫描整个 schema，把目标表名作为期望条件一起核对；补一组“同名触发器在错误表”零 DDL 反例。
2. 0h 对更新触发器只检查 `SIGNAL SQLSTATE` 和文案。一个只在 `status` 变化时 SIGNAL、却允许 `title` 更新的同名触发器仍会通过。请比较规范化后的完整 `ACTION_STATEMENT`，或逐项核对全部 18 个不可变字段的 OLD/NEW 引用和允许字段边界；补一组“同文案但保护字段不全”反例。
3. `scripts/tianlong-marketing-data-contract-38/r3_optionA.txt:17` 有行尾空白，`git diff --check` 非零。只清理该证据格式，不重跑测试。

## 复核要求

- 不重复 43 项整套或 R3 已通过项，只补上述两组触发器反例并清理一处空白。
- 新报告给出修复提交、两个反例结果和 `git diff --check` 结果，正文仅使用 GBK 可编码字符。
- 不清理既有 TL38 schema，不连接生产，不触碰法务。

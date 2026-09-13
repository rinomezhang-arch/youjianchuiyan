# TL-MARKETING-DATA-CONTRACT-38 R2 技术验收

- 结论：CHANGES_REQUESTED
- 验收对象：`ef76edc808c70405fc7242cc9287576276dd9c10`
- 修订提交：`9ced6cc39f60bab04f146a3a7dbf84a58d413268`
- 范围：静态核对迁移、定向证据和报告；未复跑整套验证，未连接数据库。

## 必须修正

1. `scripts/migrations/marketing_publication_v1.sql:404-427` 用 `(SELECT 1 UNION ALL SELECT 2)` 制造错误 1242 来阻止快照更新。该错误不是受控业务拒绝，文案不说明违规字段，也把执行器行为当成数据库契约。请改为确定的、由迁移定义的拒绝机制，返回明确错误；同时用 mysql CLI 和 Spring ScriptUtils 各做一次定向验证。
2. 阶段 0 没有核对已有 `trg_publication_no_delete` 和 `trg_publication_immutable` 的事件、时机及动作定义；后面的 `CREATE TRIGGER IF NOT EXISTS` 会静默保留同名错误或空触发器。请在第一条 DDL 前 fail-closed 核对两只触发器，并新增“同名错误触发器”零 DDL 反例。
3. 三处外键预检只比较 `DELETE_RULE`，没有比较 `UPDATE_RULE`；现有同名 `ON UPDATE CASCADE` 仍可能通过预检。请明确目标更新规则并同时核对 `UPDATE_RULE`，新增一个错误更新规则反例。

## 已确认闭合

- 咨询来源已经由四字段复合外键约束到同一发布、门店、来源码和渠道。
- 列、CHECK、唯一键、普通索引及主要外键的 DDL 前检查比 R1 完整。
- 删除发布行由明确 `SIGNAL` 拒绝；17 组写入反例和 8 组异常结构证据均有原始记录。
- 报告已删除隔离库认证状态等无关敏感表述。

## 复核要求

- 不重复现有 43 项整套验证；只补受控更新拒绝、错误触发器、错误 `UPDATE_RULE` 三组定向证据。
- 新报告必须列出修改文件、定向测试数字、错误码和错误文本，且正文仅使用 GBK 可编码字符。
- 不连接生产，不清理已有 TL38 schema，不触碰法务。

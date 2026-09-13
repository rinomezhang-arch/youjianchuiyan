# TL-MARKETING-DATA-CONTRACT-38 R1 技术验收

结论：CHANGES_REQUESTED。29 PASS、0 FAIL 的隔离执行结果暂不采纳为生产迁移候选，禁止重跑现有整套验证，先修合同缺口。

已通过：写入前环境闸门、TL38 独立 schema、首次和重复执行、旧行保留、13 组非法写入反例、三组异常结构零部分迁移、提交和远端核对均有证据；未碰生产、现有 schema、Java、Vue、配置、部署或法务。

必须修正四项：

1. `marketing_publication_v1.sql:289-293` 的咨询外键只有 `(marketing_publication_id, store_id)`，没有约束 `source_code` 和 `source_channel` 必须属于同一发布版本；报告却写成四字段关系已满足。给父表增加对应复合唯一键，并让咨询四字段复合外键完整关联，补“同 publication 和 store、伪造 source_code 或 channel”拒绝反例。
2. 阶段 0 只检查部分目标结构。现有 `marketing_activity` 同名错误 CHECK 或 `uk_activity_id_store`、`booking_inquiry` 同名错误外键，以及 publication/attribution 的错误列或 CHECK，仍可能在前几条 ALTER 已提交后才失败。MySQL DDL 会自动提交，必须在第一条 DDL 前语义核对全部目标列、默认值、CHECK、唯一键、普通索引和外键；任一不符立即拒绝。至少补错误活动唯一键、错误活动 CHECK、错误咨询外键、错误发布列和错误归因 CHECK 五类零部分迁移反例。
3. `marketing_publication` 被称为不可变快照，但当前表仍允许修改标题、内容、来源码、版本和有效期，也允许在没有子行时删除。迁移必须用可由 Spring ScriptUtils 执行的数据库约束或触发器禁止发布快照字段更新和物理删除，只允许发布状态及暂停审计字段按规则变化；补更新快照和删除发布行均被拒绝的反例。
4. 报告写出了隔离库的认证状态。认证方式也属于凭据事实，删除该表述；只保留“运行时取得且未持久化、未输出凭证明文”。同步修正报告中“四字段关系已满足”的过度结论。

修正后只运行新增和受影响反例，再执行一次完整验证；旧失败证据保留。若无法在当前迁移形式安全实现，立即在任务板标 blocked，写清已完成、原始证据、是否零生产写入和两个可执行选项，再通知 Codex。

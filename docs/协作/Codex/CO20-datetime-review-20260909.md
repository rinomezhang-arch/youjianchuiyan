# CO20 日期精度补充验收

候选27d66303ce2929237d675eeb9d913fbe3d03fbca，25项工资用例已由统筹独立复跑通过，0失败/错误/跳过。此前三项修复保留，不重做实现。

结论changes_requested，仅修新增MODIFY日期列造成的数据损失：阶段0接受date/datetime/timestamp，却没检查小数秒。统筹用完整metadata fixture、新co_payroll20_dt_1788889646932库和完整迁移复现：approved_at/paid_at为DATETIME(6)，初值10:20:30.123456与10:20:30.654321；迁移退出0后变成10:20:30.000000与10:20:31.000000。与首条DDL前防数据损失的要求不符。

精确修复：在任何DDL前检查两个既有时间列是否有非零MICROSECOND；有精度损失则明确拒绝，保留原定义和原值。不要在中途ALTER失败或完成后才发现。补纯合成异常，断言退出非零、尚未添加其他列、两个原值/定义保持。无需改业务Java。

再跑相关异常与25项。统筹已明确批准旧PayrollMysqlIntegrationTest本轮自行新建且保留的payroll_it_时间戳schema命名例外：CREATE不带IF NOT EXISTS，撞名即失败；只运行一个测试进程、不得访问或删除既有他人库，不必另开参数化任务。

证据：artifacts/coordination-r3/reviews/payroll20-datetime-repro.json及payroll20-27d66303.log。无生产或法务写入。

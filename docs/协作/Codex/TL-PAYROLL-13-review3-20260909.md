# TL-OPS-PAYROLL-MIGRATION-CANONICAL-13 changes_requested

候选9dc186c2b11d111dd2206405ecdb5e4253dad8aa。本轮独立静态复核后，Codex再次核对SQL相应段落，仍需以下三处定点修复；不合并、不部署。无需全盘重查。

1. scripts/migrations/payroll_approval_payout_v1.sql:43-69：month_salary列存在但类型不符时@ok=0，后续仍执行ADD同名列。最小反例为已有approved_by VARCHAR(50)，触发Duplicate column；应区分缺列/已有错误定义，安全修改并在任何DDL前验证转换和历史数据可保留。
2. 同文件:74-81：同名idx_month_salary_payout存在但列为salary_month时，直接ADD报Duplicate key name。应先识别已有索引及语义，安全修复；异常用例要验证最终列序与唯一性。
3. 同文件:150-165：idx_payout_month只核对列序，未核对NON_UNIQUE；同列序的错误UNIQUE索引会被误判合格。修复完整索引语义。

当前anomaly_test.sh仅测试payout表错误类型和其中一个唯一索引，未覆盖上述month_salary错误列及索引反例。分别补3类异常，按实际断言数报告，不改旧证据掩盖失败。保留此前三态、历史recorded_by越界零DDL拒绝、复合FK列序及DELETE RESTRICT、25项工资测试。

只在已确认归属自己的隔离库测试；修复后提交精确新SHA、真实通过/失败/错误/跳过数字和证据路径，reported给Codex，Trae再按新候选独立复核。当前静态拒绝是为避免已知错误浪费整套运行；未声称做过本轮SQL/生产实跑。

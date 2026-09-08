# CL-OPS-INQUIRY-CONVERT-07 · started

领取人：Claude（claude-code/solo）。`CLAIM_VERIFIED`。统筹：Codex。
工作树 `F:/solo/artifacts/team-worktrees/claude-ipad-account-flow`，
分支 `codex/claude-ipad-account-flow`，起始 HEAD `159b7465`（与 `base_sha` 一致）。
板上无其他执行端事件。

**本次先 claim、立刻 started、再实现**——前两轮我都是先实现后补 started，被统筹提醒两次，已改。

## 业务口径（照任务 notes，不自行发挥）

客人先提交咨询 → **员工明确桌台和时间后**确认成正式预订。
**系统不得自动猜桌台**：`table_ids` 必须由员工显式给出，缺了就拒绝。

## 计划

1. **先核现有表字段与仓储方法再动手**——不预设 `BookingMaster`/`BookingTable` 的列和
   `BookingRepository` 的现有方法，先读实体与仓储，避免臆造字段。
2. 新增员工确认接口，要求 `booking_time` 与 `table_ids`，门店取**咨询自身的 `store_id`**，
   不接受调用方覆盖。
3. 写入前拒绝四类：跨店桌台、已被占用的桌台、过去日期、已拒绝的咨询。
4. 一个事务内建 `booking_master` + `booking_table`，并把咨询标记已处理、
   **保存 `booking_id` 关联**；任何一步失败全部回滚，**不删除咨询**。
5. 幂等：同一咨询重复确认返回**同一个 `booking_id`**；并发确认只生成一张正式预订
   （靠唯一约束兜底，不靠先查后写）。
6. 客人用现有公开手机号查询能看到该正式预订。
7. 候选迁移只新增**必要的可空关联列与唯一约束**，兼容历史数据，
   **只在隔离库验证，严禁生产执行**。

## 验证

`YOUJIAN_TEST_MYSQL=1 mvn -o -Dtest='BookingInquiry*Test' test`，真实隔离 MySQL，schema 保留。
覆盖：成功转换、四类拒绝、失败全回滚、重复确认幂等、并发只生成一张、跨店、公开手机号回读。

## 明确不做

不自动占用未明确选择的桌台；不删除历史咨询；不碰前端、法务、部署、配置；
**不在生产执行任何迁移**。

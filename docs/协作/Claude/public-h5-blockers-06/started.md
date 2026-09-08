# CL-OPS-PUBLIC-H5-BLOCKERS-06 · started

领取人：Claude（claude-code/solo）。`CLAIM_VERIFIED`。统筹：Codex。
工作树 `F:/solo/artifacts/team-worktrees/claude-ipad-account-flow`，分支 `codex/claude-ipad-account-flow`，
起始 HEAD `aab6d59a`（与 `base_sha` 一致），领取时工作区 0 处改动，板上无其他执行端事件。

## 两个阻断，均已取证确认

**一、`PublicStoreController` 查了不存在的列。**
它 `SELECT ... latitude, longitude ...`，但 `latitude`/`longitude`
**在 `StoreInfo` 实体上没有**（实体 29 个列逐个比对确认），
**在 `scripts/migrations/` 与 `src/main/resources` 全量 schema 里也搜不到**。
真实库上这句直接 `Unknown column`，H5 首页门店列表整个打不开。

**二、`BookingInquiryController` 非法日期静默落库。**
原代码是 `try { inquiry.setPreferredDate(LocalDate.parse(dateStr)); } catch (Exception ignored) {}`——
格式非法被吞掉，`preferredDate` 落成 null，**记录照样入库**。
客人以为约了某天，店里拿到一条没有日期的咨询，而且没人知道他本来想约哪天。
过去的日期更是**压根没校验**。

## 修法

1. 门店查询只留确实存在的字段：`store_id, store_name, store_short_name, address, phone, business_hours`，
   保持原 `ORDER BY sort_order, store_id` 稳定排序，空门店返回空列表。
   **不臆造经纬度**——地图定位要用得先有列，属独立任务。
2. 日期在**任何写入之前**判完：格式非法 → 400「期望日期格式不正确，应为 yyyy-MM-dd」；
   早于今天 → 400「期望日期不能早于今天，请重新选择」。
   **当天放行**——客人当天想订位是正常需求，不能一刀切成"必须明天以后"。
   未填仍然允许（咨询可以不指定日期）。

## 验证

`YOUJIAN_TEST_MYSQL=1 mvn -o -Dtest='Public*Test' test`，真实隔离 MySQL，schema 保留。
覆盖：门店有值 / 空门店 / 非当前状态不可见 / 非法日期 / 过去日期 / 当天 / 未来 /
错误手机 / 正常提交，且非法路径**零落库**。

## 明确不做

不扩展"咨询转正式预订"（forbidden，另立任务）；不改生产 schema；不加迁移；不碰前端。

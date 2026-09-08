# CL-OPS-PUBLIC-H5-BLOCKERS-06 · reported

分支 `codex/claude-ipad-account-flow`，基线 `aab6d59a`（与任务一致），沿用原独立工作树。
只动允许路径。未碰前端、法务、生产、迁移、部署、配置；**未扩展咨询转正式预订**。

## 阻断一：门店列表查了库里不存在的列

`PublicStoreController` 原 SQL 里有 `latitude, longitude`。这两列：

- **`StoreInfo` 实体没有**（29 个列逐个比对确认）
- **`scripts/migrations/` 与 `src/main/resources` 全量 schema 里也搜不到**

真实库上这句必然 `Unknown column`，**H5 首页门店列表整个打不开**。

改法：只查确实存在的 `store_id, store_name, store_short_name, address, phone, business_hours`，
保留原 `ORDER BY sort_order, store_id`。**不臆造经纬度**——地图定位要用得先有列，属独立任务。

## 阻断二：非法日期静默落库

原代码：

```java
try { inquiry.setPreferredDate(LocalDate.parse(dateStr)); } catch (Exception ignored) {}
```

格式非法被吞掉，`preferredDate` 落成 null，**记录照样入库**。
客人以为约了某天，店里拿到一条没有日期的咨询，**而且没人知道他本来想约哪天**。
过去日期更是压根没校验。

改法：日期在**任何写入之前**判完。格式非法 → 400「期望日期格式不正确，应为 yyyy-MM-dd」；
早于今天 → 400「期望日期不能早于今天，请重新选择」。
**当天放行**——客人当天想订位是正常需求，不能一刀切成"必须明天以后"。
未填仍允许：咨询本来就可以不指定日期。

## 测试结果

命令（地龙复跑照抄即可）：

```
cd F:/solo/artifacts/team-worktrees/claude-ipad-account-flow/banquet_project
YOUJIAN_TEST_MYSQL=1 mvn -o -Dtest='PublicH5BlockersMysqlTest' test
```

**`Tests run: 9, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS，4.08 s。**

真实隔离 MySQL `127.0.0.1:13317`，schema 保留（`public_h5_<uuid>`，日志打
`PUBLIC_H5_EVIDENCE ... retained=true`）。**门店那条必须在真实库跑**——
它的病就是"查了库里没有的列"，用替身根本发现不了。

| Order | 用例 | 覆盖 |
|---|---|---|
| 1 | `emptyStoreListIsNotAnError` | 空库返回空列表，不是错误 |
| 2 | `storeListRunsOnRealSchema` | 真实 schema 上不再 Unknown column；只 `open` 可见（筹备店不可见）；**故意让 `sort_order` 与 `store_id` 相反**证明排序真按 `sort_order`；字段集合严格相等且不含经纬度；连查两次逐字相同 |
| 3 | `latitudeColumnTrulyAbsent` | 直接查 `information_schema` 断言这两列**确实不存在**，并断言查它必然抛错——把"原实现必然失败"钉死，防止日后有人以为是环境问题 |
| 4 | `malformedDateRejectedWithoutWrite` | 5 种非法格式（含 `2026-13-45`、`2026/03/01`、`20260301`）全拒且**零落库** |
| 5 | `pastDateRejectedWithoutWrite` | 昨天 → 拒绝且零落库 |
| 6 | `todayAndFutureAccepted` | 当天与 30 天后均可提交，且**回查库确认落库日期与提交一致** |
| 7 | `missingDateStillAccepted` | 不填日期可提交，落库为 NULL |
| 8 | `badPhoneRejectedWithoutWrite` | 4 种错误手机号全拒且零落库（既有行为，回归保护） |
| 9 | `bothInvalidStillNoWrite` | 手机号与日期都非法时同样零落库 |

**中途失败一次，如实记录**：首轮 9 项中 2 项报
`No EntityManager with actual transaction available` ——我漏给仓储代理挂事务拦截器。
**是我测试装配的问题，不是代码缺陷**（拒绝类用例根本走不到 `save`，所以都过了）。补上后 9/9 通过。

## 未解决：咨询转正式预订的缺口

**本任务明确不扩展**（forbidden），但缺口必须写清楚，否则容易被当成已闭环：

1. `booking_inquiry` 只是**咨询意向表**，`status` 落 `pending`。
   目前**没有任何代码路径**把一条 `pending` 咨询转成 `booking_master` 正式预订单。
   `/api/booking-inquiries/{id}/handle` 只改 `status` 与 `handledTime`，**不建预订单、不占台位**。
2. 因此 H5 客人提交"预订"后，实际只进了咨询队列，**没有任何排期或台位占用**，
   全靠员工人工在后台另开预订单。这条链路的真实闭环需要另立任务。
3. 相关口径未定：转正式预订时如何选台位、是否需要押金、
   同一手机号重复咨询是否合并、咨询被拒后如何通知客人。这些都要业务定，我不臆造。

## 其余未解决 / 明确不做

1. **经纬度字段不存在**。H5 若要地图定位，需要先加列（迁移，本任务 forbidden）+ 数据录入，属独立任务。
2. 未改生产 schema，未加迁移。
3. 未做浏览器端到端；是真实控制器 + 真实 MySQL，但不是页面操作。地龙的 E2E 复跑仍需另跑。
4. `status = 'open'` 这个取值我沿用原实现未动。**但要提醒**：本项目其他测试夹具里门店状态用的是
   `'active'`。若生产库里门店状态实际是 `'active'`，这个接口会返回空列表——
   这属于**数据口径问题不是代码问题**，建议统筹安排一次生产只读盘点确认 `store_info.status` 的实际取值。
   我没有改判断条件，因为凭本地夹具去改生产判断是拿本机现象描述生产。

## GBK 自查

本目录报告全文逐字符检查，不可 GBK 编码字符 **0** 个。

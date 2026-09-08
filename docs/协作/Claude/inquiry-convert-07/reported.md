# CL-OPS-INQUIRY-CONVERT-07 · reported

分支 `codex/claude-ipad-account-flow`，基线 `159b7465`（与任务一致），沿用原独立工作树。
未碰前端、法务、部署、配置；**未在生产执行任何迁移**；未删除任何历史咨询；
**未自动占用任何未明确选择的桌台**。

## 接口

```
POST /api/booking-inquiries/{id}/convert
{ "bookingDate": "2026-09-09", "bookingTime": "18:30", "tableIds": [11, 12], "remark": "可选" }
```

成功返回 `{ bookingId, inquiryId, replayed: false, tableIds }`；重复确认返回同一 `bookingId` 且 `replayed: true`。

**三个字段全部必填。** 业务口径照任务 notes：客人先咨询，**员工明确桌台和时间后**才确认成正式预订。
**系统绝不替员工猜桌台**——猜错的后果是客人到店没位子，比报个错严重得多。

**门店只取咨询自身的 `store_id`**，不接受请求体覆盖。咨询属于哪家店是既成事实，
允许覆盖就等于允许把 A 店的咨询转成 B 店的预订（用例 9 专门验了这个）。

## 并发与幂等怎么做到的

**咨询行的排他锁是本事务第一条数据库语句**，拿到锁后才判 `booking_id` 是否已回填。

普通查询会先建立一致性读快照，后到的事务就会带着过期快照进来，看不到先到者刚回填的
`booking_id`，于是建出第二张单。所以顺序不能反——这条在应收那几轮实测踩过。

两个员工同时点确认：后者阻塞到前者提交，然后看到已回填的 `booking_id`，
走幂等分支返回同一张单。`booking_id` 上的唯一索引是第二道防线。

桌台按 `table_id` 排序后逐个加锁，固定加锁顺序，避免两个请求交叉持锁死锁。

## 候选迁移

`banquet_project/src/main/resources/db/migration/inquiry_booking_link_v1.sql`
**候选，只在隔离库验证，严禁生产执行。**

只做两件事，都兼容历史：新增**可空**列 `booking_id`（历史咨询本来就没转过，留 NULL，不回填）；
该列加唯一索引（让"同一张正式预订被两条咨询认领"在数据库层不可能发生；MySQL 唯一索引允许多个 NULL）。

**没有加指向 `booking_master` 的外键**：`booking_master.booking_id` 的历史完整性未经取证，
贸然加外键会让迁移在生产直接失败——跟收款那条外键是同一个教训。需要强约束请另立任务，先做只读盘点。

## 测试结果

命令（地龙复跑照抄）：

```
cd F:/solo/artifacts/team-worktrees/claude-ipad-account-flow/banquet_project
YOUJIAN_TEST_MYSQL=1 mvn -o -Dtest='BookingInquiryConvertMysqlTest,PublicH5BlockersMysqlTest' test
```

**`Tests run: 18, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS。**
本任务 9 项（5.70 s）+ 06 的 9 项回归全绿（1.01 s）——我改的是同一个控制器，所以把 06 一起跑了。

真实隔离 MySQL `127.0.0.1:13317`，schema 保留（`inq_conv_<uuid>`，日志打
`INQ_CONVERT_EVIDENCE ... retained=true`），真实事务代理。

| Order | 用例 | 覆盖 |
|---|---|---|
| 1 | `candidateMigrationRunsOnLegacyData` | **迁移真跑**：先把表退回没有 `booking_id` 的历史形态、塞两条历史咨询，再执行迁移文件原文，断言列与唯一索引都建出来、历史行完好且为 NULL |
| 2 | `successfulConversion` | 一个事务内建单与占台；咨询标记 `converted` 并回填 `booking_id`；手机号从咨询带过来 |
| 3 | `fourRejections` | 缺桌台 / 空桌台数组 / 跨店桌台 / 过去日期 / 缺时间 / 已占桌台，**六种全拒且零副作用**（不建单、不占台、咨询不被改动） |
| 4 | `rejectedInquiryCannotConvert` | 已拒绝的咨询不能转 |
| 5 | `repeatConfirmIsIdempotent` | 重复确认返回同一 `bookingId`，且**故意换了时间和桌台**也不建第二张 |
| 6 | `concurrentConfirmCreatesOnlyOne` | 两线程同时确认，只生成一张正式预订 |
| 7 | `failureRollsBackEverything` | 台位插入失败时预订单一起回滚，咨询原样、**未被删除** |
| 8 | `customerPhoneFindsTheBooking` | 按手机号恰好查到自己那一张；无关手机号查不到任何单 |
| 9 | `storeIdComesFromInquiryOnly` | 请求体塞 `storeId` 不起作用，门店仍取咨询自身的 |

**中途失败三次，全部是我测试装配的问题，不是代码缺陷，如实记录：**

1. 硬编码 Hibernate 生成的唯一索引名 → `Can't DROP`。改成运行时查 `information_schema` 取真实索引名。
2. 用 `SELECT LAST_INSERT_ID()` 取主键 → 后续全部 404。
   **`DriverManagerDataSource` 每次调用都开新连接**，`LAST_INSERT_ID()` 自然是 0。改成按手机号回查。
3. 回滚用例原本靠 `ALTER ... MODIFY booking_id VARCHAR(1)` 制造失败 → ALTER 自己先失败，
   因为已有行的 `booking_id` 就超长。改成临时把表改名，插入直接报表不存在。

## 需要统筹处理的两件

### 一、`allowed_paths` 里的 `BookingRepository.java` 不存在

任务允许路径列了
`banquet_project/src/main/java/com/youjian/banquet/repository/BookingRepository.java`，
但**该文件在基线上不存在**——实际的是 `BookingMasterRepository.java`（不在允许路径内）。

我没有创建 `BookingRepository`，也没有改 `BookingMasterRepository`：
改用控制器里既有的 `JdbcTemplate` 完成全部读写，既不越界也不新增无用文件。
请统筹确认这是不是笔误，以及后续任务是否需要修正路径清单。

### 二、**没有"客人用手机号查预订"的公开接口**

任务验收提到"客人用现有公开手机号查询能看到该正式预订"，但基线上**只有两个 `/api/public/**` 端点**：
提交咨询与提交退款申请，**没有任何按手机号查预订的公开接口**。

所以用例 8 我是在**数据层**验的：`booking_master.customer_phone` 确实从咨询带了过来，
按手机号能恰好查到那一张、无关号码查不到。**这不等于"客人能查到"**——
要真让客人查得到，得新增一个公开查询接口（含防枚举保护），不在本任务允许路径内，需另立任务。
我不把数据层的验证说成端到端。

## 其余未解决 / 明确不做

1. 转换后**不自动改桌台状态**为占用：`table_master.table_status` 的流转由现有开台流程负责，
   本任务只建 `booking_table` 关联。若业务要求确认即锁台，属新口径。
2. 押金、套餐、菜品明细均未从咨询带入正式预订——咨询里的 `selected_dishes` 只是意向 JSON，
   转成 `booking_dish_detail` 需要定价与库存口径，任务未要求，我不臆造。
3. 同一手机号重复咨询是否合并、咨询被拒后如何通知客人——口径未定，未实现。
4. 未做浏览器端到端。
5. 迁移**未在生产执行**，只在隔离库验证过。

## GBK 自查

本目录报告全文逐字符检查，不可 GBK 编码字符 **0** 个。

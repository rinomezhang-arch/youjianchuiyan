# CL-OPS-INQUIRY-CONVERT-AUTH-09 · reported

分支 `codex/claude-ipad-account-flow`，基线 `608299c5`（与任务一致）。
只改 `BookingInquiryController` 与对应隔离 MySQL 测试。未碰前端、法务、生产、部署、核心配置。

## 修的是什么

`POST /api/booking-inquiries/{id}/convert` 原来只校验「桌台属于**咨询**所在门店」，
**从不校验调用者自身的门店与角色**。这个漏洞是我上一轮复现的，本轮我来修。

新增 `denyIfNotAuthorized(inquiryStoreId)`：未登录 401；解析不出门店 403；
非总经理且门店不符 403。口径对齐 `ReceivablePaymentService.requireStore`。

## 位置是刻意的，三条约束只有一种排法成立

```
锁咨询行 FOR UPDATE          <- 仍是本事务第一条数据库语句（自查行号 173）
  -> denyIfNotAuthorized      <- 186
  -> 重放判定 / 新建           <- 191
```

1. **必须在锁之后**：要判「调用者门店 == 咨询门店」，就得先拿到咨询的 `store_id`，
   而它来自被锁的那一行。
2. **必须在重放返回之前**——这是统筹点名的那条。原来的重放分支不做任何门店校验就把
   `bookingId` 还回去：**越权者即使转不了单，也能白拿一个真实单号**，
   而单号加手机号就能查到客人的行程。所以未授权的重放同样必须挡住，不能只挡新建。
3. **锁仍是第一条语句**，并发只生成一单的保证没被动过：普通查询会先建立一致性读快照，
   后到的事务带着过期快照进来就会建出第二张单。

## 提示统一，不当探测器

「咨询不存在」与「不是你门店的」用**同一句**「咨询不存在或不属于当前门店」。
分开说等于把接口变成探测器：换 id 试，从差异就能数出别店有多少条咨询、哪些 id 是真的。
与收款账户那边同一个道理。

## 测试结果

```
cd F:/solo/artifacts/team-worktrees/claude-ipad-account-flow/banquet_project
YOUJIAN_TEST_MYSQL=1 mvn -o -Dtest='BookingInquiryConvertMysqlTest,PublicH5BlockersMysqlTest,BookingInquiryPublicLookupHttpMysqlTest' test
```

**`Tests run: 31, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS。**

- `BookingInquiryConvertMysqlTest` **14**（既有 9 + 本轮新增 5），35.74 s
- `BookingInquiryPublicLookupHttpMysqlTest` 8（回归），5.03 s
- `PublicH5BlockersMysqlTest` 9（回归），2.51 s

真实隔离 MySQL `127.0.0.1:13317`，每次运行新建 schema 并保留。

| Order | 用例 | 覆盖 |
|---|---|---|
| 10 | `foreignStoreStaffCannotConvert` | 别店员工转本店咨询 → **403**，且 `booking_master`/`booking_table`/咨询行三处快照完全不变 |
| 11 | `foreignStaffReplayLeaksNothing` | 本店先正常转出一单，别店员工再拿同一条咨询重放 → **403、`data` 为 null、提示里不含真实单号** |
| 12 | `notFoundAndForeignShareOneMessage` | 别店的与不存在的，文案一致，不告诉对方是哪一种 |
| 13 | `anonymousRejected` | 未登录 → 401 且零副作用 |
| 14 | `concurrencyStillOneAfterAuth` | **加鉴权之后并发仍只生成一单** |

## 中途失败两次，都是旧用例把漏洞行为写进了断言，不是代码缺陷

1. `concurrentConfirmCreatesOnlyOne`（既有）线程池里没设身份。
   `UserContext` 是 **ThreadLocal**，子线程拿不到主线程的身份，加鉴权后直接 401，一单都没建，
   断言 4 变 3。修法是在池内线程补身份并在 finally 清理——**改的是测试装配，不是放宽代码**。
2. `storeIdComesFromInquiryOnly`（既有）用一店身份去转二店咨询。
   **新规则下这本来就该被拒**。该用例要验的是「门店取自咨询而不是请求体」，
   所以换成二店合法身份继续验那个断言，断言本身一个字没改。

这两处恰好说明鉴权真的生效了：不带身份、跨门店，原先都能过。

## 未解决 / 明确不做

1. **总经理跨店按既有口径放行**（`UserContext.isGeneralManager()`）。
   与应收那套一致，但**应收还额外要求总经理显式指定门店**；这里门店取自咨询自身，
   无从显式指定。若业务要求总经理也逐店确认，属新口径，未擅自加。
2. 未收紧 `GET /api/booking-inquiries` 的 `can_manage_hr=1` 判定，
   也未把 convert 对齐成同一条件——**两者要求什么权限属业务口径**，
   我只补了「必须是本店」这条明确缺失的，不自行统一。建议统筹确认
   转单是否也应要求 `can_manage_hr=1`。
3. 未改前端。前端本来就只让员工操作自己门店的咨询列表，但**前端限制不是安全边界**，
   真正的防线在本轮补的服务端校验。
4. 未做浏览器端到端；是真实身份上下文 + 真实事务 + 真实 MySQL，但不是页面操作。

## GBK 自查

本目录报告全文逐字符检查，不可 GBK 编码字符 **0** 个。

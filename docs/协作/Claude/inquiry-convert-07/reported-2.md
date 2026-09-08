# CL-OPS-INQUIRY-CONVERT-07 · 第二次 reported（回 changes_requested）

**统筹的退回理由成立，我认。** 第一轮「客人公开回读」只做了数据库查询，没有公开接口。
我当时在报告里如实标注了"这不等于客人能查到""不把数据层验证说成端到端"——
**但如实标注不等于交付**，该做的还是要做。本轮补上。

工作树、分支不变，`BookingInquiryController` 本就在允许路径内，**未扩范围**。
旧提交与旧报告保留不动。

## 新增接口

```
POST /api/public/booking-lookup
{ "phone": "13700001111", "bookingId": "BK..." }
```

**必须同时给对手机号和单号**，缺一不可。

### 防枚举是这个接口的第一要务，不是附带考虑

三条设计，每条都对应一种真实攻击：

1. **绝不允许仅凭手机号查询。** 只给手机号就能列出某人名下所有预订——
   那不是查询接口，是客户信息接口。用例 6 专门验：只给手机号时响应里连自己的单号都不出现。
2. **查无 / 手机号不符 / 别人的单 / 别店的单 / 缺参数 / 手机号格式非法，全部返回同一种空结果。**
   分开提示等于把接口变成校验器：拿一个 bookingId 去试不同手机号，
   从错误差异就能反推出机主是谁。所以这里**连"订单不存在"都不说**。
3. **用 POST + 请求体，不用 GET 查询串。** 手机号是个人信息，
   不该出现在 URL、访问日志和浏览器历史里。

### 字段白名单（在 SQL 里就定死）

只返回 `booking_id`、`booking_date`、`booking_time`、`guest_count`、`table_count`、`booking_status`。

**不返回客人自己的电话**——他知道，回显只会让这串号码在日志里多留一份。
不返回备注、金额、内部单号、操作人。

## 测试结果

命令（地龙复跑照抄）：

```
cd F:/solo/artifacts/team-worktrees/claude-ipad-account-flow/banquet_project
YOUJIAN_TEST_MYSQL=1 mvn -o -Dtest='BookingInquiryPublicLookupHttpMysqlTest,BookingInquiryConvertMysqlTest,PublicH5BlockersMysqlTest' test
```

**`Tests run: 26, Failures: 0, Errors: 0, Skipped: 0`，BUILD SUCCESS。一次通过，无中途失败。**

- `BookingInquiryPublicLookupHttpMysqlTest` **8 项（本轮新增，HTTP 级）**，2.07 s
- `BookingInquiryConvertMysqlTest` 9 项回归，9.23 s
- `PublicH5BlockersMysqlTest` 9 项回归，1.37 s

**HTTP 级**：MockMvc 真实分发 + 真实隔离 MySQL（schema 保留 `pub_lookup_<uuid>`）。
响应体按 UTF-8 读原始字节——MockMvc 默认用 ISO-8859-1 解码，中文断言会全部落空，这坑之前踩过。

| Order | 用例 | 覆盖 |
|---|---|---|
| 1 | `correctCombinationReturnsWhitelistedFields` | 正确组合查得到；键集合**严格等于**六个白名单字段；再对整个响应体全文搜索手机号、备注、金额、姓名，确认一个都没带出来 |
| 2 | `wrongPhoneReturnsEmpty` | 手机号错 → 空 |
| 3 | `wrongBookingIdReturnsEmpty` | 单号不存在 → 空 |
| 4 | `othersBookingReturnsEmpty` | 拿别人的单号配自己手机号 → 空；**反过来也验了一遍** |
| 5 | `crossStoreReturnsEmpty` | 别店的单 → 空 |
| 6 | `phoneOnlyCannotEnumerate` | 只给手机号 → 空，且响应里**不出现该手机号名下的单号** |
| 7 | `otherIncompleteInputsReturnEmpty` | 只给单号 / 都不给 / 手机号格式非法 → 全空 |
| 8 | `allMissResponsesAreByteIdentical` | **7 种失败原因的响应体逐字节相同、HTTP 状态也相同**——外部无从区分是单号错、手机号错还是别人的单 |

第 8 项是这套测试里最关键的一条：前面七种失败各自返回空只是"看起来一样"，
只有断言**去重后只剩一种响应**，才真正堵死了"用错误差异反推"的路。

## 未解决 / 明确不做

1. **没有限流。** 这个接口需要同时猜中手机号与单号，枚举成本已经很高，
   但生产上仍建议在网关层加频率限制。属部署配置，本任务 forbidden。
2. 不提供按手机号列出全部预订的能力——这是刻意的，不是遗漏。
3. 不返回金额与备注：客人核对行程用不到，属不必要暴露。
4. 未做浏览器端到端；是真实 HTTP 分发 + 真实 MySQL，但不是页面操作。
5. 第一轮报告里的其余未解决项（不自动改台位状态、押金套餐菜品未带入、
   重复咨询合并与拒绝通知口径未定、迁移未在生产执行）**均未变化**，仍然有效。

## GBK 自查

本目录报告全文逐字符检查，不可 GBK 编码字符 **0** 个。

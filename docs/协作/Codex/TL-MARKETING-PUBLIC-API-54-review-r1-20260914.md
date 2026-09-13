# TL-MARKETING-PUBLIC-API-54 R1 技术验收

结论：CHANGES_REQUESTED。

固定验收头：`91fdc2f5aa3ab2dc01db0b1bcbc6eec792feab95`；实现提交：`f1b182080e0a89d9636cb0cc6165b55f67580836`。允许路径和隔离证据范围合格，8 项定向测试全部通过，但还有两处真实契约缺口：

1. `PublicMarketingController.getActivity` 对不可公开记录返回 `Result.error(404, ...)`，实际 HTTP 状态仍是 200。当前前端响应拦截器会把业务非 200 转成没有 `response.status` 的普通 Error，`MarketingActivity.load` 因此显示“网络错误”，不能进入“活动不存在或已下架”。请只在该公开控制器让不可公开详情返回真实 HTTP 404，并新增 HTTP 状态断言；六类状态的响应体继续同码同文案。
2. `PublicMarketingService.recordViewEvent` 捕获并发唯一键冲突后只查询 `event_id` 就直接返回成功，没有再次核对 publication、store、source、eventType 和 visitorKey。并发的同 requestId 冲突载荷可能被误当成幂等成功。请复用现有完整载荷比较，对 DuplicateKeyException 回查结果同样判一致才返回原 eventId，否则 409；新增一条并发或直接触发重复键的冲突载荷反例，数据库仍只一行。

只改上述两个 Java 文件和定向测试，复跑营销 54 专属测试；不重跑迁移大套件、不扩内部状态机、咨询、前端、配置、生产或法务。

# CL-IPAD-PAYMENT-R2-REVIEW-64 短报告

**结论：PASS（带一处证据数字更正，一处非阻断观察）。**
CL61 两个缺口都是真修了，机制合理；后端 account_id/store/booking 关联查过没问题。
只读，`git show` 固定 SHA `6cf5e6d1`，未改代码、未建库；唯一一次误跑
`node --test` 因缺 `node_modules` 直接失败退出，没有产出任何结果，
不构成"复跑测试"，特此说明不隐瞒。

## CL61 缺口一（切店/卸载在飞支付回写）：已修，机制正确

新增 `contextGeneration` + `activePayment` + `isCurrentPayment()`，直接照搬
`loadAccounts()` 的过期守卫模式用到 `confirmPay()`/`completePay()` 上（PaySelect.vue
177-206 行新逻辑）。`watch` 从只盯 `storeId` 扩到 `[storeId, bookingId]`，覆盖了
A-B-A（同店内切换预订）场景，不只是切店。`await` 之后成功/失败两个分支都先判
`isCurrentPayment(request)` 不通过就直接 `return`，不再执行 `completePay()`。

更进一步：新增 `pendingByContext` Map，记录"已提交但结果未知"（响应还没回来时
上下文就变了）的支付快照，切回原门店/订单时会把这个快照原样恢复，UI 提示
"本笔支付结果待确认，请重试确认"，重试时复用**同一个** `payData`/`paymentKey`，
不允许改金额重发新 key。这个设计的安全性完全压在后端幂等键去重是否可靠上，
所以我去查了后端。

## 后端 account_id/store/booking 关联：查了，是对的

`IpadCheckoutController.pay()`（151-206行）：`storeId` 来自 `requiredStore(request)`
（服务端按设备认证上下文判定，不接受客户端参数）；幂等键去重查两次——
拿锁前查一次、`FOR UPDATE` 拿到 `booking_master` 行锁后再查一次（176-179行），
防并发重复；`requireUsableAccount(accountId, storeId)`（224-231行）校验账户属于
当前门店且启用；`finance_transaction` 一条 INSERT 同时带 `store_id`、`account_id`、
`related_id`（booking 主键）、`operator_id`（197-199行），三者绑定在同一事务里。
后端单测 `IpadCheckoutAccountIntegrationTest.java` 确认 **6 个 `@Test`**，名字对得上
（`financeInsertCarriesAccountIdAndRelatedIdTogether`、`crossStoreAccountRejectsBeforeAnyWrite`、
`disabledAccountRejectsBeforeAnyWrite` 等），和我读源码得出的结论一致。

**非阻断观察**：`repeatedPayment()`（重放命中幂等键时的返回，241-246行）只回
`{booking_id, amount}`，缺新支付成功时才有的 `transaction_no`/`change_amount`
（196-201行那条）。当前前端不读这两个字段（找零显示用的是客户端自己的快照，
下面会说），所以现在不是活 bug，但两条路径返回形状不一致，将来谁要是读
`res.data.transaction_no` 会在重放路径上拿到 undefined。建议后续统一，不算本次阻断项。

## CL61 缺口二（现金找零快照）：已修，验证方式对

`confirmPay()` 提交前用 `freezePayment(JSON.parse(JSON.stringify({...})))` 把
`payData`、`amount`、`received`、`change` 一起深拷贝冻结进 `pendingPayment`
（313-330行），找零弹窗改读 `confirmedPayment.change`（模板129行），不再读实时
`changeAmount`。现金输入框、支付方式按钮等全部控件现在都挂了
`:disabled="paymentLocked"`（`paymentLocked = paying || showChangeModal || pendingPayment`），
提交后到确认完成前整段时间用户改不了任何输入。这比我在 CL61 建议的"只挡输入框"
更彻底——连支付方式、混合支付明细都锁了，是对的方向。

## 需要更正的一处证据数字

任务备注写的是"组件38/38"，我在固定 SHA `6cf5e6d1` 的
`frontend_v3/scripts/ipad-account-select.test.mjs` 里数了一遍，**实际是 21 个
顶层 `test()`**（原有 15 项 CL61 基线 + 新增 6 项 R2 专项：现金快照抗改动、
混合支付载荷冻结、控件全锁+防重复点击、上下文切换中断确认、账户失效后可重选、
A-B-A 未知结果保留原键）。CO60 那份 r2 演进日志（`ipad-account-recovery-60/docs/
.../r2/final-green.log`）确实记着 "tests 38 pass 38"，但**那份日志对应的测试文件
内容和最终合入 `6cf5e6d1` 的不是同一份**——连 CO60 自己工作树当前 HEAD 的测试文件
也只有 21 个，不是 38。新增的 6 项内容本身是对的，覆盖点也切中要害，只是引用的
"38"这个数字对不上实际交付物，登记时应该更正成 21，不然后面的人核对会对不上。

独立复现 `docs/协作/Codex/ipad-payment-findings-63/after-verified.json` 确认
**3/3**，且自己标注了"deferred fake transport; not real HTTP or DB"，没有虚报成
真实链路，这条属实。

## 未覆盖，如实列出

- 真实 HTTP/数据库端到端：未执行——不是我漏做，是这次范围本来就不含（我也没有起
  服务、建库）。上面提到的独立复现同样明确自称非真实链路。
- 刷新页面后 `pendingPayment`（未知结果待确认状态）是否能恢复：`pendingByContext`
  是组件内存里的 `Map`，没有持久化（不进 `sessionStorage`/后端），**刷新或重进页面
  这份"待确认"状态会丢**。如果丢的时候后端那笔其实已经成功入账，用户下次进来会
  当作全新单据重新收一次款——但幂等键会变（`crypto.randomUUID()` 重新生成），
  所以这不是"重复扣款"风险，而是"看起来收款失败了但其实已经收了，需要人工去对账"
  的风险。这条刷新场景任务本身写了"明确刷新恢复未覆盖"，我确认了这个缺口真实存在，
  不是文字游戏。

## 边界

只读，未改 PaySelect.vue/ipad.js/测试文件/IpadCheckoutController.java 任何一行，
未起服务、未建库、未碰法务或生产、未改动 CO60/CO63 的任务事件。

# CL-IPAD-ACCOUNT-REVIEW-61 独立复核

**结论：CHANGES_REQUESTED。** 账户选择/切店清空/幂等重试三块做得扎实，且报告本身
（`docs/协作/Codex/ipad-account-recovery-60/reported.md`）没有虚报——它明确写了
"当前候选真实设备/浏览器及后端复验：未执行"，也没把旧 TR06 的 30 项浏览器测试
冒充新提交端到端通过。这条我逐字核对了，报告是诚实的。

但任务点名要查的两件事——"旧请求成功/失败不得回写"和"现金找零"——各查到一处真实缺口，
且**两处恰好都不在 15/15 的覆盖范围内**。逐条给出文件行和最小修复建议。

## 范围与核实方式

只读 `git diff ccf24b31..e226289f` 涉及的三个文件（`frontend_v3/src/api/ipad.js`、
`frontend_v3/src/views/ipad/settlement/PaySelect.vue`、
`frontend_v3/scripts/ipad-account-select.test.mjs`），未改动任何代码，未复跑测试，
未扫全仓。对 `before-tests.txt`／`after-tests.txt`／`build.txt` 逐份核对：
修复前 11 PASS/4 FAIL、修复后 **15 PASS/0 FAIL**、构建 `built OK, 54.19s` 均属实，
不是转述。

## 缺口一：`confirmPay()` 的响应处理没有切店/卸载的过期请求守卫（阻断级）

**文件**：`frontend_v3/src/views/ipad/settlement/PaySelect.vue:244-309`

`loadAccounts()`（第 175-205 行）对"过期响应"有完整守卫：每次调用记录
`request = ++accountsRequest` 与 `requestStore = ipad.storeId`，`await` 之后先判
`isCurrent()`（序号未变且门店未变）才写回状态；`onBeforeUnmount` 也会递增
`accountsRequest` 使在飞请求失效（第 336 行）。这套机制测试里验证得很仔细
（"旧店成功响应晚到不能覆盖新店账户和选择""旧店失败响应晚到不能清空新店账户或显示旧错误"
"卸载后未完成的账户请求不得回写组件状态"三项，第 179/195/220 行起）。

但**支付提交 `confirmPay()`（第 244-299 行）完全没有同类守卫**。场景：

1. 操作员在一店选中账户，点确认支付，`confirmPay()` 同步把 `booking_id`/`account_id`
   锁进 `payData`（第 259-275 行），随后 `await ipadSettlementPay(...)`（第 277 行）。
2. 请求还没返回时设备切店（`ipad.storeId` 变化，`flush:'sync'` 的 watch 第 335 行立即
   触发 `loadAccounts()`，同步清空 `selectedAccountId`/`accounts`）。
3. 一店那笔支付的响应**这时候才姗姗来迟**——无论成功还是失败，`confirmPay()` 里
   `await` 之后的代码**不检查门店是否已经变了，直接原样执行**：
   - 成功走 `completePay()`（第 301-309 行）：把 `selectedAccountId` 清 null
     （操作员刚给二店选的账户被顺手清掉）、`ipad.clearCart()`（二店正在进行的购物车
     被一起清空）、`router.push('/ipad/home')`（强制把界面从二店正在进行的交易上
     拽回首页）。
   - 失败则弹一条属于一店那笔的错误提示，盖在二店正在进行的界面上，操作员很容易
     误以为是当前这笔支付出了问题。
4. 实际打到后端的钱没有算错（`payData` 在 `await` 前就已锁定，第 259-275 行不受影响），
   风险不是"多扣钱"，而是**这次客户端 UI 状态被一笔不相干的旧支付回执污染**——
   清空购物车、清空账户选择、强制跳转，正是任务描述里"旧请求成功、失败都不能回写"
   要防的那类事，只是这次发生在支付提交而不是账户加载上。

**15 项测试没有覆盖这个场景。** "旧店成功/失败响应晚到"两个测试（第 179、195 行）测的
都是 `loadAccounts()` 的过期响应，触发方式是直接切店后立刻断言账户列表状态，**没有一个
测试是"先发起支付、支付还没返回时切店、再让支付响应到达"**。测试夹具（第 60-99 行）
里 `payResp` 完全支持用 `deferred()` 做这种测试——账户加载那两个测试就是这么写的
——但同款写法没有套用到支付提交上。所以报告里"旧请求成功、失败都不能回写新门店账户/
错误/选择"这句话，**只对账户加载成立，对支付提交不成立**，而支付提交这条链路
恰恰更要紧。

**最小修复建议**：在 `confirmPay()` 里 `await ipadSettlementPay(...)` 之前也记一份
`const requestStore = ipad.storeId`，`await` 之后先判 `if (requestStore !== ipad.storeId) return`
（或等价的组件已卸载/请求已过期判断）再执行 `completePay()` 与错误提示分支，
和 `loadAccounts()` 用同一套模式。至少要做到：过期响应不得清空当前门店的购物车、
账户选择，也不得强制跳转或弹出错误提示。

## 缺口二：现金找零金额没有在支付提交时锁定快照（非阻断，但属于现金收付一致性问题）

**文件**：`frontend_v3/src/views/ipad/settlement/PaySelect.vue:67、279-283、129`

现金输入框（第 67 行）在支付提交期间（`paying.value === true`）**没有禁用**，
只有确认按钮本身被 `disabled="paying || !canPay"`（第 118 行）挡住。而找零弹窗
（第 129 行 `{{ changeAmount.toFixed(2) }}`）显示的是**实时**的 `changeAmount` ref，
不是提交那一刻的快照。

链路：操作员输入实收现金→点确认支付→`payData.pay_amount = cashReceived.value` 在
`await` 之前锁定（第 272 行，这部分是对的，实际打给后端的金额不受影响）→**但在
`await` 等待响应期间，现金输入框还能继续编辑**→如果这时候操作员又改了实收金额
（比如客户临时又多给了一张钞票，或者操作员手滑改错了重改），`calcChange()`
（第 240-242 行）会重算 `changeAmount`→支付成功后找零弹窗（第 279-283 行触发，
第 124-133 行渲染）显示的是**这个后来被改过的找零数**，未必等于实际记账的那笔金额
对应的找零。这是柜台现场真实会发生的操作序列，一旦发生，操作员照屏幕上的数字
找零就可能找错钱。

15 项测试里没有一处触碰 `cashReceived`／`changeAmount`／`showChangeModal`
（唯一涉及现金的测试只是确认 `account_id` 被带上，不涉及找零金额一致性），
这块是彻底空白，不是"基线已有问题延续"——找零弹窗和 `account_id` 这套逻辑
都是这次新接的线，此前没有这条完整链路。

**最小修复建议**：在构建 `payData` 的同时把 `changeAmount.value` 快照进一个新 ref
（例如 `confirmedChange`），找零弹窗改读这个快照而不是实时 `changeAmount`；
再给现金输入框加 `:disabled="paying"` 做双重保险，从根上不让金额在等待响应期间被改。

## 未越界说明

- 未修改 `PaySelect.vue`／`ipad.js`／测试文件本身，只读复核。
- 未重跑任何测试、未起构建、未连数据库或后端，全部结论基于源码逻辑推演加既有日志核对。
- 未评价后端幂等键处理逻辑（后端不在允许路径内，且报告与测试均为纯前端 mock）；
  重试时若现金金额被改变导致同一幂等键携带不同金额，属于前后端契约问题，
  这里只记为观察，不下结论，交后端一并核实。
- 未把旧 TR06 30 项浏览器测试当作本次端到端通过证据，报告本身也没有这样声称。

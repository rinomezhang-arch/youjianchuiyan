# CL50 驱动器运行输出（抄录件，非原始重定向）

**先说清楚这份文件是什么、不是什么。**

运行当时我把 driver 的 stdout 直接管道给了 `tail -70` 看，**没有另存一份原始重定向日志**。
所以库里不存在一个"原始 stdout 文件"——统筹问的那个路径，我没有。这是我的疏漏，不遮掩。

下面是我实际看到的输出**逐字抄录**。因为过了 `tail -70`，
开头约 10 行（第 0 段"起点：隔离库现状"的计数、以及 A 段前几条）**不在抄录范围内**，
这里如实标出缺口，不凭记忆补写。

无法重跑补齐：driver 的 A 段断言"起点 month_salary 必须为 0"，
而隔离库现在已是终态（month_salary 1 行 status=3），重跑必然在 A 段失败。
要拿完整原始日志，需要另起一个新库重跑——那是另一轮的事，不在本卡内自行发起。

## 可核对的替代证据（这些是实打实存在的）

| 想核对什么 | 看哪里 |
| --- | --- |
| 每一条断言的原文与判定条件 | `drive_real_payroll.py`（51 项断言全部在源码里逐字可读） |
| 终态数据是否与报告一致 | 隔离库 `co_pay50_20260909`，直接查 `month_salary` 与 `payroll_payout_record` |
| 建库与种子做了什么 | `00_schema.sql`（`01_synthetic.sql` 含 bcrypt 哈希，留本机未提交） |
| 运行时口径 | `application.yml` |
| JAR 身份 | `co51-converged-candidate.jar` 实算 sha256 |

## 抄录（第 B 段起，之前的部分缺失）

```
PASS  B 审批人真实登录拿到 token | JWT 三段
PASS  B 门店员工真实登录拿到 token
PASS  B 总经理真实登录拿到 token
PASS  B 错误密码被拒 | code=401

=== C. 无权身份（can_manage_hr=0）必须被拒且零写入 ===
PASS  C 无权身份 save 被拒 | code=403
PASS  C 无权身份 save 后 month_salary 仍为 0

=== D. 跨店：一店审批人不能把二店员工混进同一批 ===
PASS  D 跨店混合批被拒 | code=400 msg=员工 3 不属于当前门店，无权保存其工资，整批未保存
PASS  D 跨店被拒后 month_salary 仍为 0（整批回滚）

=== E. 保存 → 状态 1 ===
PASS  E save 成功 | code=200 msg=success
PASS  E save 回执带 month | 回执 month=2026-08
PASS  E save 回执 saved=1 | saved=1
PASS  E 落库实发 = 1595.00（1000+200+300+40+50+5） | 库内 net_salary=1595.00
PASS  E 落库状态 = 1 已保存 | status=1

=== F. 列表：门店范围与已知数字 ===
PASS  F 一店审批人看不到二店员工(emp_id=3) | 看到 [1, 2, 4, 5]
PASS  F 列表含 emp_id=2
PASS  F 列表 net_pay=1595 | net_pay=1595.0
PASS  F 列表 attendance_pay=300 | attendance_pay=300.0
PASS  F 总经理全门店范围能看到二店员工 | 看到 [1, 2, 3, 4, 5]

=== G. 未审批就发放记账：必须拒绝且零写入 ===
PASS  G 未审批 payout 被拒 | code=400 msg=本月还有 1 条工资未审批，不能发放记账。请先完成审批
PASS  G 未审批 payout 后台账仍为 0（零写入）
PASS  G 未审批 payout 后状态仍为 1 | status=1

=== H. 不在批复白名单的人不能审批 ===
PASS  H 非批复人 approve 被拒 | code=400 msg=当前仅张婧、张晓秋可以审批工资，请转交他们处理
PASS  H 非批复人被拒后状态仍为 1 | status=1

=== I. 审批 1 → 2 ===
PASS  I approve 成功 | code=200 msg=success
PASS  I approve 回执带 month | month=2026-08
PASS  I approve 回执 approved=1 | approved=1
PASS  I 落库状态 = 2 已审批 | status=2
PASS  I 落库审批人 = 真实登录账号 synthetic_approver

=== J. 发放记账 2 → 3 + 台账 ===
PASS  J payout 成功 | code=200 msg=success
PASS  J payout 回执带 month | month=2026-08
PASS  J payout 回执 paid=1 | paid=1
PASS  J 回执明说只是记账不是银行到账 | message=已完成本批发放记账。这是账务记录，不代表银行已到账，请以银行流水为准
PASS  J 落库状态 = 3 已发放记账 | status=3
PASS  J 台账恰好 1 条 | 实际 1 条
PASS  J 台账 salary_month 与请求月一致 | 台账 month=2026-08
PASS  J 台账人数 = 库内该批人数 | headcount=1
PASS  J 台账合计 = 库内该批实发合计 | total_net=1595.00
PASS  J 台账记账人 = 真实登录账号 | recorded_by=synthetic_approver
PASS  J 无孤儿：批次号指向的台账都存在且门店一致

=== K. 重复发放记账：不得产生第二笔 ===
PASS  K 重复 payout 明确回 alreadyRecorded | data={"month": "2026-08", "paid": 0, "alreadyRecorded": true, "previouslyRecorded": 1, "message": "本月这 1 条工资此前已完成发放记账，本次未产生新的记账"}
PASS  K 重复 payout 后 month_salary 逐行未变
PASS  K 重复 payout 后台账逐行未变

=== L. 已发放记账后再保存：不得倒退状态、不得改金额 ===
PASS  L 再保存后状态仍为 3（未被退回已保存） | 状态=3
PASS  L 再保存后实发仍为 1595.00（金额未被改写） | net=1595.00

=== M. 审计留痕 ===
PASS  M 全链留下审计记录（>=4 条） | audit_logs=14

================ 汇总 ================
PASSED=51 FAILED=0
```

## L 段的完整回执（单独补跑一次拿到的原文）

L 段在上面只打了判定结果，没打服务端原话。运行结束后我又单独调了一次同样的请求
（幂等：已发放的月份再保存本来就该被拒，不会改变任何数据），拿到完整回执：

```
{"code": 400, "message": "以下员工本月工资已进入审批或发放阶段，不能直接保存覆盖：2(已发放记账)。当前没有撤回审批的接口，需要改动请联系张婧或张晓秋处理，整批未保存", "data": null}
库内：[['3', '1595.00', '1000.00']]   -- status / net_salary / base_salary
```

那次补跑之后 `audit_logs` 从 14 增至 16，所以报告里终态写的是 16 条，
与上面抄录里 M 段的 14 条相差 2，是这一次补跑造成的，不是两处数字对不上。

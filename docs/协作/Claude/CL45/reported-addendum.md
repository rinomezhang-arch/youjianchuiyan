# CL45 补充：DL39 四条生产线索的自证结果

收到 codex/current 转来的 DL39 partial report 线索后补做。
全部结论重新对着 CL45 卡内指定的生产快照
artifacts/coordination-r3/reviews/prod-java-current-20260909-0941 亲自查过，
不引用 DL39 的结论，只用它给的方向。仍未启动 Java，运行时一律 NOT_RUN。

DL39 的四条**全部成立**，且候选四条全部修掉了。

## 1. 生产缺 /approve —— 成立

生产 PayrollController 的全部映射只有五个：
@GetMapping（类路径）、/save、/pay、/unlock、/lock。**没有 /approve**。
前端 run('approve') 发 POST /hr/payroll/approve，生产上必然 404。
候选新增 @PostMapping("/approve")，1 已保存 → 2 已审批。

## 2. 生产 /pay 与前端 /payout 不符 —— 成立

生产：@PostMapping(value={"/pay"})，只有这一个拼写。
前端：POST /hr/payroll/payout。生产上必然 404。
候选：@PostMapping({"/pay", "/payout"})，两个拼写都收，注释写明保留 /pay 是为前端兼容。

## 3. 生产 1→3 跳过审批 2 —— 成立

生产 payPayroll 只有一条 SQL：
  UPDATE month_salary SET status = 3, updated_at = NOW() WHERE salary_month = ? AND status = 1
从"已保存"直接跳到"已发放记账"，状态 2 在生产上根本不存在（与第 1 条互为因果）。
候选 PayrollService.payout 先查本月是否仍有 status=1 未审批，有则整批拒绝，
只把 status=2 的记录改成 3，并写 payout_id 台账。

## 4. month 回执缺 —— 成立

生产 save 回执只有 {saved:n}，pay 回执只有 {paid:n}，都不回 month。
调用方无法从回执确认服务端处理的是哪个月。
候选回执带 month：save 回 {month,saved,totalGross,totalNet,ignoredClientTotals}；
approve 回 {month,approved,approvedBy}；payout 回 {month,payoutId,paid,totalNet,recordedBy,message}。

## 另有第五条，DL39 没提，我查生产源码时撞到的

生产 save 的 UPDATE 硬写 status=1 且**不带任何状态条件**：
  ... status=1, updated_at=NOW() WHERE salary_id=?
已经发放记账(status=3)的月份，只要再点一次保存，就被静默退回"已保存"，
发放这件事在库里消失，且没有任何提示。
候选把这条 UPDATE 收成 WHERE salary_id=? AND status < ?，越过审批线的记录不再被覆盖。

## 关于明文解锁码

生产 PayrollController 里 /unlock 是与一个六位明文常量直接比较，
位置：prod-java-current-20260909-0941 快照内 PayrollController.java 的 unlock 方法，
本报告一律以 [REDACTED] 引用，不复述该值。
候选已改为读配置项 unlockCode，未配置一律拒绝，注释里说明这是上线清单
"无硬编码密码/Token"那一条的反例。

我自己做的两件收口：
- 我生成的 PayrollController.prod-vs-candidate.diff 里原样带出了该明文（1 处），已改为 [REDACTED-6DIGIT]。
- 我为归档复制的 src-prod/ 副本是又一份明文拷贝，已移入 artifacts/trash/cl45-prod-copy-20260909/（**移入垃圾桶，未删除**）。
  生产侧的 hash 记录改以原快照为准，路径见上，不再另存副本。
提醒一句：该明文在库内已有的其他位置我一处未动——包含两份生产快照、.case_audit 与法务 worktree（冻结）。
是否清理、怎么清理，不是我这张卡能决定的，交秋哥/统筹。

## 仍然不可得 / 未跑

- 生产快照里**没有 PayrollService.java**，只有 PayrollController.java，Service 层生产 vs 候选 diff 不可得。
  另说明：生产的这些逻辑本来就写在 Controller 里，候选才抽出 Service，所以缺的这份文件在生产上很可能并不存在。
  但"很可能"不是证据，我不写成结论。
- 所有运行时行为：**NOT_RUN**。未启动 Java，512m 运行时 CO42 独占。
- 真实工资付款、任何生产写入：未执行。

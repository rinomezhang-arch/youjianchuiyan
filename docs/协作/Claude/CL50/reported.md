# CL-CONVERGED-PAYROLL-REAL-50 reported

真 HTTP、真登录、真 MySQL 跑完。**51 项断言全过，0 失败，0 错误，0 跳过。**
没有任何银行付款，没有任何生产读写。

## 运行事实（可复核）

- JAR：`artifacts/release-candidates/co51-converged-20260909/co51-converged-candidate.jar`
  本地实算 sha256 = `c7365d08a4ca0efb338e75b1d9668bdddcfb52cb20c1d3bee5056d8f3f07a59e`，与统筹给的一致。只用这一个 JAR，未重编、未用旧包。
- 进程：我拉起的 java PID **32684**，`-Xmx512m`，端口 **18095**。
  停止前先读了该 PID 的命令行确认是我自己的进程再停；停完 18095 已释放。**只停了这一个 PID。**
- 配置：`--spring.config.location=file:.../cl50-payroll-real-20260909/`，独立 application.yml **完全替代**打包内配置。
  `ddl-auto: none`、`app.notify.enabled: false`、`legal.enabled: false`、
  `approval.approvers: synthetic_approver`（不借用任何真人姓名）。
  JWT_SECRET / AES_SECRET_KEY / CUSTOMER_JWT_SECRET 本轮随机生成，**只经环境变量注入**，
  配置文件与本报告都不含密钥明文。
- 库：`127.0.0.1:13317` 的 **co_pay50_20260909**，本轮新建。
  只 CREATE / INSERT，**没有任何 DROP、DELETE、TRUNCATE、reset**。合成数据全部保留。
  结构 = 卡内 test 的 setup + `payroll-metadata-fixture-20260907.sql` + `payroll_approval_payout_v1.sql`，
  被测代码实际 SQL 需要而 fixture 没有的列按卡内授权补齐。
- 身份：5 个合成账号，密码是本轮随机生成的 bcrypt，明文只存在于本机运行期，不入库外、不写报告。
  **每个 token 都是打 `/api/auth/login` 换来的真 JWT，没有一处现签。**

## 51 项断言明细

**A 无 JWT（4 项 + 2 项零写入）**
`POST /save`、`/approve`、`/payout` 与 `GET /api/hr/payroll` 全部 **401**；
之后 `month_salary` 计数 0、`audit_logs` 计数 0 —— 挡在任何写入之前。

**B 真实登录（4 项）**
审批人、门店员工、总经理三个身份都真登成功，token 均为三段 JWT。
错误密码 **401**。

**C 无权身份（2 项）**
`can_manage_hr=0` 的合成员工调 save，**403**，`month_salary` 仍为 0。

**D 跨店整批回滚（2 项）**
一店审批人把二店员工混进同一批：**400 "员工 3 不属于当前门店，无权保存其工资，整批未保存"**，
`month_salary` 仍为 0 —— 整批回滚，没有半条落库。

**E 保存 → 状态 1（5 项）**
save 成功；回执 `month=2026-08`、`saved=1`；
库内 `net_salary=1595.00`（1000+200+300+40+50+5，与卡内 test 已知数字一致）；`status=1`。

**F 列表与门店范围（5 项）**
一店审批人看到 `[1,2,4,5]`，**看不到二店的 3**；
`net_pay=1595.0`、`attendance_pay=300.0`，与已知数字一致；
总经理（全门店范围）看到 `[1,2,3,4,5]`。

**G 未审批就发放（3 项）**
**400 "本月还有 1 条工资未审批，不能发放记账。请先完成审批"**；
台账计数仍为 0（零写入）；状态仍为 1。

**H 非批复人审批（2 项）**
门店员工调 approve：**400** 被拒；状态仍为 1。

**I 审批 1→2（5 项）**
成功；回执 `month=2026-08`、`approved=1`；库内 `status=2`；
`approved_by=synthetic_approver` —— 落的是真实登录账号，不是请求里传的什么值。

**J 发放记账 2→3 + 台账（10 项）**
成功；回执 `month`、`paid=1`，且 message 明写"这是账务记录，不代表银行已到账，请以银行流水为准"；
库内 `status=3`；台账**恰好 1 条**，`salary_month=2026-08`、`headcount=1`、`total_net=1595.00`、
`recorded_by=synthetic_approver`；
人数与合计都是**回头对着 `month_salary` 实查算出来再比**的，不是拿回执自证；
孤儿检查（批次号指向不存在的台账、或门店不一致）计数 **0**。

**K 重复发放（3 项）**
再打一次 payout：回 `alreadyRecorded=true`、`paid=0`、`previouslyRecorded=1`；
`month_salary` 与 `payroll_payout_record` **逐行快照比对完全未变** —— 没有第二笔。

**L 已发放后再保存（2 项）**
拿 9999 的金额去覆盖已发放的月份：
**400 "以下员工本月工资已进入审批或发放阶段，不能直接保存覆盖：2(已发放记账)。当前没有撤回审批的接口，需要改动请联系张婧或张晓秋处理，整批未保存"**；
库内状态仍为 **3**、实发仍为 **1595.00**、base_salary 仍为 1000.00 —— 没倒退，也没改钱。
这一条正是 CL45 里查到的生产缺陷（生产 save 硬写 status=1 且无状态条件，
已发放月份再保存会被静默退回已保存）在候选上的修复验证：候选是**显式报错**，不是静默忽略。

**M 审计留痕（1 项）** `audit_logs` 16 条。

终态：`month_salary` 1 行（salary_id=1, staff 2, 2026-08, net 1595.00, status 3,
approved_by/paid_by=synthetic_approver, payout_id=1）；
`payroll_payout_record` 1 行（payout_id=1, headcount 1, total_net 1595.00）。

## /lock 空实现：对照前端后，判断是**不构成缺口**

卡里让我先对照前端本地遮罩再下结论，对完了：

`Payroll.vue` 里 `unlocked` 只控制**显示**——未解锁时金额一律渲染成 `****`，
解锁后显示真数字。弹窗自己的文案也写着"请输入配置的验证码解锁查看；审批权限由登录身份决定"。
也就是说 lock/unlock 是**客户端展示遮罩**，服务端本来就没有"锁"这个状态要维护，
`/lock` 返回 success 而不做事，与前端的用法是自洽的。

但有一件事必须说清楚，而且我这轮拿到了直接证据：
**这个遮罩不是数据保护。** 我全程一次都没调过 unlock，
`GET /api/hr/payroll` 照样返回了真实的 1595 和 300。
真正拦人的是 `checkPayrollAccess()` 里的 `can_manage_hr`——C 项里 403 那条就是它拦的。
所以解锁码不能被当成工资数据的访问控制来看待。这不是本轮引入的问题，是既有设计，
我只是把它说明白，不改。

## 一处措辞与配置脱钩（低危，不影响放行）

H 项被拒时后端回的是「当前仅张婧、张晓秋可以审批工资，请转交他们处理」，
L 项被拒时回的是「需要改动请联系张婧或张晓秋处理」。
但本轮 `approval.approvers` 配的是 `synthetic_approver` —— 白名单**行为是对的**
（非白名单的人确实被拒了），只是**提示文案是写死的**，不跟着配置走。
生产上这两个名字恰好就是真实批复人，所以现在不会误导；
只有将来改了配置而没改文案时才会对不上。要不要改交统筹，我没动。

## 一处交代：合成身份 SQL 不入库

`01_synthetic.sql` 里带着 5 个合成账号的 bcrypt 口令哈希。
密码是本轮随机生成、用完即弃的，哈希也是单向的，但按"不泄露凭据"的口径，
这个文件**留在本机不提交**。归档里提交的是建表 DDL、驱动器和本报告，
要复现只需换一份随机口令重新生成同结构的插入即可。

## 边界

未做：任何实际银行发薪、生产 DML/DDL、部署或重启生产、法务任何文件、
他人源码改动、删除、全库探索。
本轮所有数据都是合成的，与真人无关；`status=3` 只表示账上记了这一笔，不代表钱已到员工账户。

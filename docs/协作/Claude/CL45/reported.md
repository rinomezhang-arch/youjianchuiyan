# CL-RC-PAYROLL-ALIGN-45 reported

从地龙 DL39 接手。全部结论来自实际阅读卡内指定 4 文件，未虚构任何 API。
未启动 Java（512m 运行时 CO42 独占），所有运行时行为一律 NOT_RUN。

## 六项对照：前端期望 vs 候选后端实际

候选后端类级路径 @RequestMapping("/api/hr/payroll")；前端 request 基址已含 /api。
前端状态定义 PAYROLL_STATUS = {0 未保存, 1 已保存, 2 已审批, 3 已发放记账}。

| 动作 | 前端实际请求 | 候选后端实际映射 | 必要字段 | 对齐 |
| --- | --- | --- | --- | --- |
| 列表 | GET /hr/payroll?month= | @GetMapping（类路径本身） | month | 对齐 |
| save | POST /hr/payroll/save?month= + 数组体 | @PostMapping("/save") | month（@RequestParam）+ List 体（required=false）；每项 emp_id 与 6 个金额字段 | 对齐 |
| approve | POST /hr/payroll/approve?month=（体为 null） | @PostMapping("/approve") | 仅 month | 对齐 |
| payout | POST /hr/payroll/payout?month=（体为 null） | @PostMapping({"/pay","/payout"}) | 仅 month；后端两种拼写都收 | 对齐 |
| unlock | POST /hr/payroll/unlock，体 {code}（走 handleUnlock，不走 run） | @PostMapping("/unlock") | 体内 code；后端与配置项 unlockCode 比对，未配置一律拒绝 | 对齐 |
| lock | POST /hr/payroll/lock?month=（体为 null，走 run） | @PostMapping("/lock") | @RequestBody(required=false) | 路由对齐，但**后端是空实现**：方法体只有 return Result.success()，不做任何状态变更 |

### 需要留意的一处

lock 在后端没有行为。前端按"锁定成功"处理，实际后端什么也没做。
这不是路由不匹配，接口调得通、返回 200，所以静态看接口清单发现不了。
是否应当有行为、由谁补，交统筹判断——我不在本卡自行实现。

### 一处我查证后否掉的假警报

前端 run(action) 对非 save 动作一律传 payload=null，我一度怀疑 unlock 会因此拿不到 code 而必然 401。
实查 Payroll.vue 第 365 行 handleUnlock()：unlock 走的是独立函数，
request.post('/hr/payroll/unlock', { code: unlockCode.value })，body 里带 code。
所以 unlock 是对齐的，我最初的怀疑不成立，没有把它写成缺陷。

### save 的字段与前置条件

前端 payrollSavePayload 只提交 salary_status < 2 的行，逐字段校验非负、最多两位小数、
放大 100 倍后仍是安全整数，任一不合直接抛错不发请求。
PAYROLL_FIELDS 六项不含 month_salary，与"month_salary 为空只影响展示、不影响发薪业务"一致。
前端常量 PAYROLL_NOTE 明确写着：发放记账仅记录工资台账，不执行银行付款，不代表员工银行到账。

## 源码 hash 与归档

归档目录 artifacts/release-candidates/cl45-payroll-20260909。

候选（codex-rc15-integrated-20260909）：
  Payroll.vue              e256660267e582de5bf916a9dc09bdf0ca4d700e13fc4ad5820fb0c53c47154c
  payrollActions.js        d617e9e48777f14233eafc8a2dd7b9bfa83fb681b67ca6dfbf54c1f9f7f879fe
  PayrollController.java   0b57034c8812a86567803cd7b2c5a5fc4e4a47f412c706cfa96963b9166c0e44
  PayrollService.java      0a95d19dd9cfeb20c431f1ea409164c98ceef9ef6dbf122a00dc177a6a6b64e6
生产快照（prod-java-current-20260909-0941）：
  PayrollController.java   1dd143c1da2668a02494961168a54ff1ef513889f55e0227f97c44a79a266353

生产 vs 候选 PayrollController 差异 +340 / -372，全文差异存 PayrollController.prod-vs-candidate.diff。
源码原样复制进 src-candidate/ 与 src-prod/，未做任何改写。

## NOT_RUN / 不可得，明确列出

- 运行时行为（save/approve/payout/unlock/lock 的实际状态流转与落库）：**NOT_RUN**。
  未启动 Java，512m 运行时由 CO42 独占。
- 生产快照里**没有 PayrollService.java**，只有 PayrollController.java。
  因此 Service 层的生产 vs 候选 diff **不可得**，不是我没做，是快照里没有这个文件。
  若需要该层比对，请补快照。
- 真实工资付款、任何 DDL/DML、生产写入：一律未执行，也不打算执行。

## 边界

未改权限与配置；未改前端与法务；未重建他人目录；未越出卡内 4 文件范围；
未重复 DL39 的采集工作。

# CL-RC30-FINANCE-INTEGRATE-34 追加：余额缺口的实际修复责任归属

我是这张卡的 owner，所以由我来写这份交接。**但要先把归属说清楚：
changes_requested 里那个收款余额缺口，不是我修的。**

## 归属

- 缺口本身：CO42 真实资金 20 项里那 1 项失败——新账户初始 0、收 100 之后余额仍为 0。
- 实际修复：**Codex 内部的 Godel，在独立卡 CO47 完成**，随后由统筹并入统一候选 CO51。
- 我在这中间做了什么：**什么都没做**。统筹明确让我不要与 CO47 重复，我就停在原地，
  CL34 的原始源码与两个构建 JAR 一字未动。
- 所以这一条**不能记在我头上**。我在本卡内该认的，只有下面"我自己那部分"。

## 我自己那部分（这些确实是我做的，仍然成立）

- 从 `codex-rc15-integrated-20260909`（b6df6469）取出 12 文件最小清单；
  jar sha256 `b3a8fea18d67699e98b9ba4764a4ab28d2db79ab8f82872c6fff5f3542403c17`。
- `scripts/release/finance-integrate-34/money_chain_check.py`，12/12 通过。
- 过程中我自己抓到并改掉的两处"假通过"：回放断言在 0 行上也能过；
  结算"可达"其实打到了错的路由、什么都没结算。这两处是我的失误，当时已改并写进报告。

## CO47 的证据：我自己核过的部分

不是转述统筹的说法，下面每一条我都打开文件看了：

- 目录存在：`artifacts/release-candidates/co47-account-balance-20260909`，42 个文件。
- `http-attempt-01/real-result.json`：**checks 共 17 项，17 项全 PASS，0 失败**。
  逐项名称覆盖真实登录、应收创建、收 40 / 收 60 两段的回执与落库、账户余额落库、
  重放回执与三表未变、停用账户拒绝且三表未变、跨店账户拒绝且三表未变，
  以及 `co42_original_failure_untouched`。
  第一项 `real_login` 的 `jwt_source` 写的是 `actual login response`——**是真登录换的 token，不是现签**。
  该结果文件里 token 与手机号都是 `[REDACTED]`，没有明文凭据。
- `evidence/`：`ReceivablePaymentService.patch`、`compile-test-summary.json`、
  `maven-run.json`、`maven-targeted.log`、`original-ReceivablePaymentService.java.txt`、
  `protected-inputs-after.json` 均在。
- `report.md` 自述：`mvn -o -q -Dtest=ReceivableAccountBalanceMysqlTest test` 退出 0，
  JUnit **9 通过 0 失败 0 错误 0 跳过**，真实 MySQL 13317 / `co_fin42_20260909`；
  并写明 `deploy_ready=false`，最终整合发布仍由统筹决定。
- CO47 自己那个候选 jar 的 sha256 是 `c420267127f12c439c8687958b6c70389e6e0165cf8587cf6bf35d731dcb38c5`，
  与统一候选 CO51 的 `c7365d08…f07a59e` 是两个不同的包，别混。

修复口径我也读了 patch 的说明：在原 `recordPayment` 事务内、写流水与推进应收之后，
对指定 accountId 做原子 `current_balance = current_balance + amount`；
UPDATE 同时约束 account_id、store_id、is_active=1 与 current_balance 非空，
影响行数不等于 1 就抛异常整笔回滚；NULL 明确拒绝而不是 COALESCE 成 0 或拿初始余额顶替。
这个口径是对的——拿初始余额替代当前余额正是原缺口那一类错法。

## 我核不动的一处，如实写出来

统筹给的源码提交 **`98287ac7` 在我这边的库里解析不出来**
（`git cat-file` / `git rev-parse` 均报 unknown revision，也不在我能看到的任何 worktree 里）。
大概率它在 Codex 侧尚未共享到这个检出的分支上。
所以"源码提交"这一环**我没有独立验证**，只验证了上面那些落盘的证据文件。
需要坐实的话，请把该提交推到这个检出能看到的 ref 上。

## 边界

未重跑 CO47 的任何测试，未起任何进程，未改 CL34 的原始源码与 JAR，
未碰 CO47 目录里的任何文件（只读）。本追加件只做责任归属与证据交接，不主张任何新的验收结论。

---

## 更正：`98287ac7…` 不是 Git 提交，是文件哈希

上面"我核不动的一处"写错了方向，据此更正。

统筹说明后我实算核对：
`98287ac71352fc5b13d9eb96832af452954564b535c58b579e1289542e6b46c6`
是 **`ReceivablePaymentService.java` 这个文件的 SHA256**，不是提交号。
我本地对 `co47-account-balance-20260909/src/main/java/com/youjian/banquet/service/ReceivablePaymentService.java`
实算结果与之逐字一致。**所以它是可验证的，我之前用 `git cat-file` 去解析是找错了工具。**

实际集成提交是 **`ba0695ba`**，在候选树
`artifacts/team-worktrees/codex-rc15-integrated-20260909`，
提交标题「【Codex】餐饮-修复：会话实时撤权与收款账户余额原子入账」，本地可解析、已核。
该候选树当前 HEAD 为 `013afcf1`。

至此这一环不再有未验证项。归属结论不变：修复由 CO47 完成，不是我做的。

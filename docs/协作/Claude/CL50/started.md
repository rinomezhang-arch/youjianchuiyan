# CL-CONVERGED-PAYROLL-REAL-50 started

已 claim（CLAIM_VERIFIED）。统一 JAR 哈希本地实算与统筹给的一致：
c7365d08a4ca0efb338e75b1d9668bdddcfb52cb20c1d3bee5056d8f3f07a59e

按卡执行：只新建 schema co_pay50_20260909（不存在才建），不 DROP/DELETE/reset 任何东西，
只新增合成数据并保留；18095/512m/ddl-none/notify false/legal false，独立配置完全替代打包配置，
JWT/AES 随机且只走 env；真实登录取 token，不现签 JWT；finally 只停我自己拉起的 PID。

不做：生产 DML/DDL、部署、重启生产、任何实际银行发薪、法务、他人源码、删除、全库探索。

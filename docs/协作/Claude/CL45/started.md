# CL-RC-PAYROLL-ALIGN-45 started（Claude）

从地龙 DL39 接手。基线 71cd01d6。
只读卡内指定 4 文件：候选前端 Payroll.vue / payrollActions.js，
生产快照与候选的 PayrollController.java / PayrollService.java。

产出：列表/save/approve/payout/unlock/lock 六项的前端期望与候选后端实际对照
（精确 URL、方法、必要字段、状态机），全部来自实际阅读，不虚构 API。
候选已有修复直接取最小源码进 cl45-payroll-20260909 并记 hash/diff，不重新实现。

不启动 Java（512m 运行时 CO42 独占），凡未实跑一律写 NOT_RUN。
不改权限与配置、不做任何 DDL/DML、不触发真实工资付款、不改前端与法务、不重建他人目录。

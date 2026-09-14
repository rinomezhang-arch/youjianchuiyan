# CL-PAYROLL-SCRIPT-SAFETY-72 reported

分支 codex/claude-payroll-script-safety-72，本地/远端 HEAD 一致：
`9c63d1b11e8ca360082bcc64f46eabe12dd31c6b`（git ls-remote 实核）。

## 消除的 rm/真删

- `env-precheck.sh:93` 原 `rm -f "$MOUNT_PARSER"` → 改为不删除，留给系统临时目录回收。
- `evidence-guards-71/fixture-reject-test.sh` 结尾原 `rm -rf "$WORK"` → 同样改为不删除。
- 已 grep 确认范围内三个脚本（env-precheck.sh/run-http-e2e.sh/fixture-reject-test.sh）
  不再含任何 `rm -f`/`rm -rf`。

## 环境适配（未动被测脚本本身）

`fixture-reject-test.sh` 硬编码 `python3`，本机 Windows 下 `python3` 是 WindowsApps 的
Store 空壳桩（不是真解释器，跑出 exit=49 这种反常码）。只在这份测试脚本里加了探测式
选择 `python3`/`python`，**未改 `env-precheck.sh` 里的 `python3` 调用本身**——生产/CI
目标是 Linux，那边 `python3` 是真解释器，不在本卡改动范围。

## 聚焦 fixture 结果

复用 TL71 已有 `fixture-reject-test.sh`（从真实脚本抽取 parser.py/redact_stream，
不是另写镜像逻辑）。首次跑 15 项 6 败，全部同一根因（python3 桩），定向重跑 1 次后
**15/15 全过，0 失败**：坏挂载 JSON / 空输入 / 非数组 / bind 挂载 / 匿名 volume /
null 结构六类拒绝路径；JWT / 密码 / 密钥三类 canary 脱敏不泄漏且正常诊断保留；
JAR SHA256 一致性校验通过/拒绝两条路径。

## 未覆盖（卡内已声明边界，如实列出不虚报）

- 真实启动失败与脱敏器自身故障：需要真实调用 `run-http-e2e.sh` 入口并 stub
  外部 java/docker/db，本卡未执行，仅覆盖到脱敏函数本身的行为。
- 59 独占环境缺口沿用未执行，本卡不解决。

## 边界

未启动真实服务/容器/数据库，未连库未跑真实HTTP，未碰法务/生产/网关/启动配置，
未复跑全套测试或全仓扫描，未建子代理。

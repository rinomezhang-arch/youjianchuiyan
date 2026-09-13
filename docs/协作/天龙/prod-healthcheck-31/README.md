# TL-PROD-HEALTHCHECK-31 生产健康检查误重启：只读根因与补丁候选（天龙）

- 对应 Codex：CX-8d3c2f2d5182
- 固定基线：1c9fbfd8f54100a2b25d9cd67630afd4f54875d1（远端分支 codex/integration-20260913 头）
- 范围：只读诊断；本目录只放文档与补丁候选，**未修改/未应用任何生产脚本、cron、systemd、服务、配置**
- 采集时间：2026-09-13 18:20-18:25 CST

## 一、结论（一句话）

生产后端**实际是健康的**（`/actuator/health` 返回 200 `{"status":"UP"}`），但健康检查脚本探测的 URL 是 `/api/actuator/health`（**404**），自 2026-09-13 13:40 起被判为"连续失败"，于是**每 5 分钟被 `systemctl restart` 重启一次**，已连续 57 次。属**检查器配置错误导致的误判误重启**，不是宕机。

## 二、只读对象与哈希（未读取任何配置值与凭证）

| 对象 | 路径 | 指纹 |
|---|---|---|
| 运行 JAR | /home/ubuntu/deploy_tmp_main/banquet_project/target/banquet-1.0.0.jar | sha256 2a9e033582963d62d1c6c227635b488303f63d7f1bb896d8271cc580d3544785；size 73092901；mtime 2026-09-13 13:37:32 |
| 服务单元 | /etc/systemd/system/banquet.service | sha256 0655a863398358a4d9e6d81babd18e9182e8adcd00c0fce694445e46540e0ed3 |
| 健康检查脚本 | /home/ubuntu/banquet_healthcheck.sh | sha256 d0df91f676679bbb9215ba76da0f26c2852bde67d113f98b63599ddfc0ca4688；56 行 |
| cron 条目 | ubuntu 用户 crontab 第 9 行 | `*/5 * * * * /home/ubuntu/banquet_healthcheck.sh >> /home/ubuntu/healthcheck.log 2>&1` |
| 失败计数状态 | /home/ubuntu/.banquet_health_fail | 仅计数，未读取业务数据 |

- 进程身份经 /proc 核对：MainPID 3011412，cwd=/home/ubuntu/deploy_tmp_main/banquet_project，fd 4/5 指向上述同一 JAR。
- 单元关键行为（值未展开）：ExecStart 经 `bash -lc 'source <环境文件> && exec java -Xmx1024m -XX:+ExitOnOutOfMemoryError -jar target/banquet-1.0.0.jar --spring.profiles.active=prod'`；Restart=always；RestartSec=10；SuccessExitStatus=143；TimeoutStopSec=40；stdout/stderr 追加 /home/ubuntu/backend.out。
- 说明：单元内 Environment/EnvironmentFile 行的**值已掩码（[REDACTED]）**，未输出配置值；环境文件内容未读取。

## 三、端点探测（只读 GET，20 秒内完成）

| URL | HTTP | Content-Type | 关键内容 |
|---|---|---|---|
| http://127.0.0.1:8080/api/actuator/health（脚本当前使用） | **404** | application/json;charset=UTF-8 | {"code":404,"message":"接口或资源不存在","data":null} |
| http://127.0.0.1:8080/actuator/health | **200** | application/vnd.spring-boot.actuator.v3+json | {"status":"UP"} |
| http://127.0.0.1:8080/api/stores（已知业务只读入口） | 401 | application/json;charset=UTF-8 | 未登录提示（鉴权正常，服务在线） |

判定：服务对外可用；脚本所用 URL 已不存在 → 404 是**探测器问题**，非服务故障。

## 四、日志与 journal 统计

healthcheck.log（175 行，全量）：

- 健康检查失败条目 60 条；重启触发 56 条；重启后仍不健康 56 条；重启后已恢复 **0** 条。
- 状态码分布：`HTTP=404` 共 **113** 行（含"失败"与"重启后仍不健康"两类）；`HTTP=000` 共 **3** 行（连接失败，属真实不可达场景）。
- 历史恢复记录（说明脚本曾经工作正常）：2026-09-05 03:10、2026-09-08 17:40、2026-09-08 19:25。
- **`HTTP=404` 首次出现：2026-09-13 13:40:01（第 1 次）**，紧接 JAR 重建/部署时间 13:37:32 之后。
- 最近 20 次检查（16:45:01 至 18:20:01）**全部 HTTP=404**，计数从第 38 次连续到第 57 次，期间 0 次恢复。

journal（banquet.service）：

- 今日 `Started banquet.service` 共 **57** 次；最近 10 次启停严格每 5 分钟一对（17:35、17:40、17:45、17:50、17:55、18:00、18:05、18:10、18:15、18:20）。
- 当前 `systemctl is-active banquet` = active；MainPID 3011412；**NRestarts=0** —— 说明这些重启来自外部 `systemctl restart`（即健康检查脚本），而非 systemd 自愈拉起。

## 五、三类现象判分

| 类别 | 证据 | 结论 |
|---|---|---|
| 404 误判 | 113 行 HTTP=404；同时刻 `/actuator/health`=200 UP；服务 active、NRestarts=0 | **主因**，服务健康被判失败 |
| 真实宕机 | 3 行 HTTP=000（连接失败），如 2026-09-05 03:05 首次失败，当日 03:10 已恢复 | 历史事件，与本轮无关 |
| 检查器自身失败 | 本周期未见：每次都拿到状态码并落日志，脚本退出正常 | 未发生 |

## 六、影响

- 每 5 分钟对生产后端执行一次 SIGTERM 重启：停服等待 + Spring 启动（本次实测约 13 秒）期间，真实用户请求会失败；5 小时已 57 次。
- 真实故障被噪音淹没：日志反复写"需要人工介入"，实际却无人处置，告警失去可信度。

## 七、最小补丁候选 v3（**仅文档候选，未应用**）

候选脚本：`banquet_healthcheck.candidate.sh`（同目录，头部已标注 NOT APPLIED；v3 已按 Codex R1+R2 退回修正）。六段改动：

1. **修正探测 URL** 为 `http://127.0.0.1:8080/actuator/health`，并允许用环境变量覆盖（默认值即正确值）。
2. **判定口径升级**：HTTP 200 且响应 JSON 中 `"status":"UP"` 才算健康；**404 单独归类为"检查器配置错误"**，只告警、不重启，**并清空连续失败计数**。
3. **重启前先看进程/单元**：`systemctl is-active banquet` 已 active 且端点返回 200 时不得仅凭状态字段差异重启；进程不可用或 5xx/超时才重启。
4. **重试与冷却**：连续失败阈值 3 次；同一小时最多重启 2 次；两次重启间隔至少 15 分钟；`flock` 防止 cron 重叠。
5. **无临时文件、无物理删除（R1）**：全部去掉 `mktemp` 与 `rm -f`；锁文件与状态文件只创建/覆写不删除。
6. **健康分支不再清空重启历史（R2，关键）**：删除原先的 `: > "$RESTART_STATE"`；健康时**只清失败计数**，重启历史文件保留，统计时按最近一小时过滤 —— 否则重启后下一轮一健康，限流就被归零绕过。

配套要求（同样只在候选里体现）：

- 应用前先备份原脚本：`cp -a /home/ubuntu/banquet_healthcheck.sh /home/ubuntu/banquet_healthcheck.sh.bak-20260913`
- 一条回退命令：`sudo cp -a /home/ubuntu/banquet_healthcheck.sh.bak-20260913 /home/ubuntu/banquet_healthcheck.sh`

### 七之一、离线分支测试（R1+R2 要求，已完成）

- 测试脚本：`test-candidate-offline.sh`；原始输出：`offline-test-results.txt`
- 方式：**完全离线**。假 `curl` / `systemctl` / `sudo` 覆盖外部命令，状态文件全部落在 `/tmp/hc31-offline-<ts>` 沙箱；不触网、不碰生产服务、不删除任何文件。
- 覆盖用例：`bash -n` 语法；200+UP 健康分支（只清失败计数 + 不清重启历史 + 不重启）；404 分支（只告警 + 清计数 + 不重启）；连续 3 次 500 触发一次重启；冷却期拦截；每小时上限拦截；并发锁跳过；**序列用例（R2 新增）**。
- **序列用例 T8**：预置两条一小时内重启记录 -> 中间一次 200/UP -> 再累计到失败阈值，仍必须命中每小时上限且不重启；并断言全程重启历史仍为 2 条（未被清空、未被追加）。
- **结果：PASS=20 / FAIL=0**（候选 sha256 11e8b3e71cb011afeb07d7790ecc3be2a9e858222e0a2b2be6ba595a92dbf8fe）
- 说明：R1 首轮曾 PASS=12/FAIL=3，失败点是**测试脚本自身**假 `sudo` 的 `shift` 写错（`exec: restart: not found`，重启计数读成 0），修正后全绿；候选脚本本体未受影响。


## 八、验证步骤（应用后如何证明修好，本任务不执行）

1. `bash -n` 语法检查候选脚本；`sha256sum` 记录新旧脚本指纹。
2. 手动单跑一次（不写状态文件、不重启）：确认探测到 `/actuator/health` = 200 UP 并输出"健康"。
3. 观察 15 分钟：healthcheck.log 不再出现"健康检查失败"；journal 无新增 Stop/Start。
4. 负例验证（可选，需另窗口）：临时把 URL 指向 404 路径，确认脚本只告警不重启。
5. 回退演练：执行第七节回退命令，确认哈希回到原值。

## 九、逐级回退方案

1. 一级（首选）：`sudo cp -a /home/ubuntu/banquet_healthcheck.sh.bak-20260913 /home/ubuntu/banquet_healthcheck.sh`（脚本恢复原样，cron 不变）。
2. 二级：若 cron 被改动，恢复为 `*/5 * * * * /home/ubuntu/banquet_healthcheck.sh >> /home/ubuntu/healthcheck.log 2>&1`。
3. 三级（兜底止血）：临时 `crontab -e` 注释健康检查条目，改为人工盯；服务本身不受影响（单元 Restart=always 仍在）。
4. 全过程不涉及 systemd 单元、JAR、数据库与网关门禁的修改。

## 十、边界与未做项

- 未修改/未重启任何生产服务、脚本、cron、systemd、网关、配置；未应用补丁。
- 未连接生产数据库、未读取真实业务行；未输出任何配置值或凭证明文；未碰法务。
- 未扫全仓库，仅查本任务列出的四个运维对象。
- 服务当前为 active 且健康端点 200 UP，**非真实宕机**，故未置 blocked。

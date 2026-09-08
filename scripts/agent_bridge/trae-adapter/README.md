# Trae CN 原生本机适配器

本机 Trae CN 3.3.98 产品代码中确认存在 `wx.bridge.sendAndWaitResponse(prompt, images?)` 和 `icube.chat.getCurrentSessionId`。前者调用真正的 `sendToAgent`，返回本轮提取出的 `{text}`。它没有 session 指定参数。`trae-cn chat` 则走另一套通用 VS Code Chat，不能代替本适配器的原生入口。

扩展仅在唯一工作区 `F:/solo/artifacts/bridge-evidence-20260907/trae-workspace` 且已信任时启动；其他用户窗口不启动监听。不自动创建或切换会话，不发送启动消息。必须先在此独立窗口创建专属测试会话，再读取 `/status` 得到 sessionId。使用期间勿切换该窗口的会话。前后核对只能发现切换，无法锁定 Trae 内部会话；测试证据必须明确这一限制。

启动后 `adapter-private.json` 保存随机端口和随机 token，仅供本机程序读取，禁止打印、提交或上传。Windows 的 mode 0600 不等于设置 NTFS ACL，沿用当前用户工作区权限；若需要抵御同机其他账户须另行设置 ACL。监听仅绑定 127.0.0.1，所有接口要求 `Authorization: Bearer <token>`，拒绝带 Origin 的浏览器请求，无任意命令执行入口。

- `GET /status` → `{ready,busy,sessionId}`。
- 0.1.1 增加 `POST /initialize`（认证相同）：已有会话仅返回其 ID；无会话调用原生 `workbench.action.chat.icube.open({keepOpen:true,newChat:true})`。产品实现转原生 `workbench.action.icube.aiChatSidebar.createNewSession`，不传 query 不发送 prompt。命令完成后仍无 ID 则 503 `native_session_not_ready`，不得视为成功。
- 0.1.3 替代上述初始化：已核实产品 V2 新建只是清空当前 ID，真实会话直到首次发送才建立。因此显式 /initialize 在专属工作区、原生明确空闲且无当前ID时，先原子写 `.adapter-initialization-attempt.json`，再仅发送固定文本“通信测试初始化，请只回复 TRAE-BRIDGE-READY，不执行工具不修改文件”。只有非空新ID和完全匹配的真实回执才 ready。失败保留guard，不自动重发、不删除guard。
- `POST /message` JSON `{id,prompt,expectedSessionId}`；id 为 1–100 字母数字下划线横线。
- 仅非空真实原生回复返回 200 `{id,passed:true,sessionId,text}`；空回复 502，会话错配/并发 409。
- 0.1.5 使用持久化 at-most-once 状态：调用原生命令前先原子落盘 `dispatching`；重启发现未终态 `dispatching` 时改为 `uncertain` 并永久停止该 ID 的原生重发。完成记录只保存请求指纹、会话 ID、结果状态和回复哈希，不保存 prompt 或回复正文。同进程同 ID 同请求可返回内存缓存；重启后已完成请求返回 `completed_response_unavailable_no_retry`，不会再次调用原生模型；同 ID 不同请求返回 `id_reused`。
- 扩展不自动审批 Trae 工具请求；原生模型可能等待用户确认，HTTP 此时保持等待。真实任务完成须另外核对验收证据，passed 只表示拿到非空通信回复。

运行 `npm test` 验证隔离、认证、串行、去重和结果约束。这些使用模拟 commands，不等于真实 Trae 模型已通过。安装/启动和端到端验证由统筹执行；在记录实际回执之前禁止声称已打通。

0.1.5 的 `busy` 仍仅代表适配器请求，`nativeState.workStatus` 是唯一空闲事实源：只有 `getContextKeyValue('AI_CHAT_WORK_STATUS')` 严格返回空字符串才设置 `knownIdle=true`。`WORK_STATUS_GENERATING`、`WORK_STATUS_APPLYING`、`WORK_STATUS_HAS_DIFF`、未知字符串、`undefined`、`null` 和读取异常全部 fail-closed。旧键 `icubeAiNgChatIng` 只保留在 `nativeState.generating` 供诊断，其任何值都不参与放行。发送前间隔150ms读两次，再复核session。读取与发送不是原子操作，仍无法完全阻止用户在最后一次读取后输入；此版继续限专属工作区，不能声称已有用户队列锁。

协调器另有独立持久状态文件 `~/.agent-bridge/trae-coordinator/delivery-state.json`。首次启动先把当时 inbox 的文件名形成明确 `startupFiles` 快照并持久化；快照内现有消息只记为 `quarantined/startup_snapshot`，原地保留且零发送，后续不依赖可能变化的 mtime。新消息先落 `dispatching` 再调用适配器。适配器明确证明未发送的 `busy`、`session_mismatch`、`native_not_known_idle` 可在后续扫描重试；网络中断或其他不确定结果记为 `uncertain`，不重发原生请求。

扫描使用 `delivery-state.json.scan.available` 与 `.scan.held` 的原子重命名令牌作为跨进程互斥，并在获得锁后重载状态，两个协调器不会凭各自旧内存重复发送。持锁进程异常退出时，其他进程只会在确认原 owner PID 已不存在后恢复令牌；无法确认就 fail-closed。消息 ID 必须符合安全格式并与 JSON 文件名基名完全一致；状态记录使用无原型映射，`__proto__`、`constructor` 等合法 ID 不会碰撞对象原型。

模型回复正文只驻留内存。桥回复前持久化由入站 ID、目标与回复哈希导出的固定出站消息 ID；回复调用在已送达后丢失 ACK 时，只用同一个出站 ID 重试回复，不重跑 Trae。若进程在回复完成前重启，因为正文未持久化，该记录转 `uncertain`。状态文件写入会 fsync 文件并同目录原子替换，可保证正常进程崩溃后的 fail-closed 恢复；未 fsync 父目录，因此不声明断电或存储控制器故障下的持久性保证。

打包与安装不属于本次修复。本次没有安装 VSIX、没有启动真实适配器，也没有读取或消费真实 inbox；验证使用临时目录和模拟命令。协调器负责读取 Agent Bridge inbox、POST 到本接口并回发结果，扩展本身不会主动读取 inbox 或其他会话内容。

0.1.3 启动最多等待原生命令注册30秒；授予workspace trust或扩展列表变化可再次触发检查。`adapter-startup-status.json` 只保存状态、版本、时间和缺失命令ID，供诊断；不含token。监听成功后才更新私有descriptor。存在descriptor不证明端口仍活跃。

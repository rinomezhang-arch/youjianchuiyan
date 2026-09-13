# CO-AUTH-CONTEXT-RECOVERY-26 started

- 执行人/工具：Codex
- 记录时间：2026-09-13 15:40:54 +08:00
- 基线：`4aecfa4413789a7023eb679779556d0b95022fb9`
- 意图：收口 iPad 写链的审计身份来源，统一 `UserContext` 空 subject 契约，并补对应定向反例。
- 修改范围：仅任务 JSON 允许的 `AuditLogAspect.java`、`UserContext.java`、`Auth*/Ipad*/Audit*` 定向测试及本记录目录。
- 验证方式：运行 AuthContext、AuthRealtime、IpadAuthChain、IpadBatch 与 Audit 最小定向测试；保存精确通过、失败、错误、跳过数字及隔离 schema/监听端口证据。
- 下一步：先补失败反例，再实施最小修复并复跑同一矩阵。
- 边界：不连接生产，不触碰法务、账号密码/JWT、部署、配置或物理删除。

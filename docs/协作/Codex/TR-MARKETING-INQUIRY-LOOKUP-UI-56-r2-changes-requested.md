# TR-MARKETING-INQUIRY-LOOKUP-UI-56 R2 技术验收

结论：CHANGES_REQUESTED。

- 固定候选：`3ea4e6386d49b5a395af0ee757ba195a9d4c6bcf`，远端分支读回一致，`git diff --check` 通过。
- R1 的字段和手机号问题已修正：页面使用 `createdAt`，前端手机号口径已收紧为 `^1[3-9]\d{9}$`。R2 的 390px 系统错误截图也已去掉英文 axios 提示，现有视觉证据可保留。
- 仍有一项会把真实系统错误伪装成“查无”，不能 reviewed：

`frontend_v3/src/api/marketing.js` 为回查另建裸 axios 实例，只依赖 HTTP 非 2xx 来 reject。真实后端统一返回 `Result` 业务体；`Result.error(500, ...)` 通常仍是 HTTP 200。此时 `lookupBookingInquiry` 会正常返回 `{code:500,data:null}`，页面再执行 `normalizeInquiryLookup(res?.data)`，最终进入统一空结果，而不是中文系统错误态。R2 mock 只覆盖了 HTTP 500，没有覆盖 HTTP 200 + 业务 `code=500`，因此 21/21 没有证明任务卡要求的错误语义。

请取消独立 axios 语义分叉，按 R1 已授权的窄范围给 `frontend_v3/src/utils/request.js` 增加默认不启用的“抑制全局错误提示”请求选项；回查继续使用共用 `request` 并显式启用该选项。这样保留共用层 `code!==200` 的 reject 语义，同时不弹英文 `ElMessage`。补一个 HTTP 200、业务体 `code=500` 的定向反例，断言页面进入系统错误态、输入保留、没有 `.el-message`，不得落入空结果。

只修改回查允许路径与上述共用请求层的 opt-in 分支，默认行为及其他调用必须保持不变。复跑 56 专属契约、SFC、浏览器证据和一次构建；R3 报告必须写精确修复提交与最终 HEAD。不得扩后端、配置、部署、其他页面或法务范围。

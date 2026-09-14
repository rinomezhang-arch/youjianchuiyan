# TR-MARKETING-INQUIRY-LOOKUP-UI-56 技术验收

结论：CHANGES_REQUESTED。

- 固定候选：`d877afe1044706139dc428fdc06a9b9a57eb2d6d`，远端分支读回一致；变更落在任务卡允许路径。API 契约测试 11/11、SFC 测试 11/11、前端生产构建均由 Codex 复跑通过；8 张 390px 页面截图和 21 项浏览器结果明确为 `MOCKED_CONTRACT`，视觉与隐私边界基本合格。
- 仍有三处不能进入生产候选：

1. 前端 `normalizeInquiryLookup` 只读取 `submitTime`，页面也只展示 `result.submitTime`；TL55 的真实回查白名单返回字段是 `createdAt`。将真实后端接入后，提交时间会稳定显示为 `—`。请统一为 TL55 的 `createdAt`，或做明确兼容并只向页面暴露一个规范化字段；mock、API 测试和浏览器结果必须改用真实字段名，新增“真实候选形状能显示提交时间”的反例。
2. 前端手机号校验为 `^1\d{10}$`，TL55 为 `^1[3-9]\d{9}$`。例如 `12000000000` 会被前端当作合法并发出请求，后端再作为非法输入返回空结果；这不符合任务卡中非法输入零请求的验收口径。请与后端统一为 `^1[3-9]\d{9}$`，补 `12` 号段被前端拦截且请求数为 0 的测试。
3. `08-system-error-input-kept-390.png` 底部出现两条英文原始错误 `Request failed with status code 500`。页面已有完整中文错误卡，公开 H5 不应再暴露 axios 技术文本或叠加重复提示。请为共用请求层增加默认关闭、仅由本请求显式启用的“由页面自行处理错误提示”选项，并由回查请求启用；其他请求行为保持不变。浏览器测试补充系统错误态不存在 `.el-message` 且输入仍保留。统筹为这一窄修复追加允许路径 `frontend_v3/src/utils/request.js`，不得借机改鉴权、门店注入或其他错误处理。

R1 报告和任务板事件请同时写明精确修复提交与最终 HEAD；继续保留 `MOCKED_CONTRACT` 边界。只修改 56 原允许路径及上述 `frontend_v3/src/utils/request.js` 窄扩展，复跑 56 专属 API/SFC/浏览器测试和一次前端生产构建；不得扩后端、配置、部署、其他页面或法务范围。TL55 仍在 changes_requested，修复后本任务仍只算前端候选，不宣称真实闭环。

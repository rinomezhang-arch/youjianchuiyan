# APP-CONTROL-SKILL-20260913 交付与测试记录

- 执行人：Codex
- 时间：2026-09-13 12:14-12:29 CST
- 状态：技能完成并安装；天龙实测完成；Claude/Trae 已通知但尚未取得有效测试回执
- 边界：未修改工具配置、网关配置、系统服务、法务、生产代码或生产数据

## 交付物

1. Windows 通用技能：`skills/windows-uia-app-control/`
   - `SKILL.md`
   - `agents/openai.yaml`
   - `scripts/uia-app.ps1`
   - `references/patterns.md`
2. Linux 通用技能：`skills/linux-atspi-app-control/`
   - `SKILL.md`
   - `agents/openai.yaml`
   - `scripts/atspi_app.py`
   - `references/patterns.md`
3. Windows 安装位置：Codex、Claude、Trae 各自用户技能目录；三个安装副本的脚本 SHA-256 与项目源一致。
4. Linux 安装位置：天龙用户级 `.agents/skills/linux-atspi-app-control`；四个交付文件的 SHA-256 与本地项目源逐一一致。

## 能力与保护

- Windows 使用 UI Automation 的 Value、Invoke、SelectionItem、ExpandCollapse、RangeValue、Toggle 与 ScrollItem 语义模式。
- Linux 使用 AT-SPI Accessible、EditableText、Action、Selection 与 Value 接口。
- 禁止鼠标坐标、模拟键盘、剪贴板和抢前台；写操作要求唯一窗口、唯一控件、幂等操作号与结果回读。
- 非空输入默认拒绝覆盖；日志只保留长度、哈希、操作状态与错误码，不保存正文。
- Linux 无桌面或无 AT-SPI 时明确失败，不退化为 xdotool 等键鼠模拟方式。

## 本机验证

- 技能结构：项目 Windows、项目 Linux、Codex 安装、Claude 安装、Trae 安装共 5/5 通过 `quick_validate.py`。
- PowerShell：语法解析通过。
- Claude 只读窗口枚举：1 个唯一 `claude/Claude` 窗口。
- Claude 控件检查：1 个唯一 `Prompt` 编辑控件，支持 `ValuePattern`；默认输出仅含长度与 SHA-256。
- DryRun：`set-value` 演练通过，未写入界面。
- 非空保护：真实调用在写入前返回 `NONEMPTY_VALUE_REQUIRES_ALLOW_REPLACE`，退出码 2；操作前后长度均为 20，SHA-256 不变。
- Trae 当前窗口只暴露 13 个 Pane/窗口按钮，未暴露聊天 Edit 控件；技能安全返回 0 个匹配，不使用坐标或键鼠兜底。
- 本机 Linux 脚本在无 Linux 桌面环境返回 `NO_DESKTOP_SESSION`。

## 协作者通知与回执

- 天龙：主会话消息已回读送达；天龙实际执行 `py_compile` 退出 0，`list-apps` 退出 2 并返回 `NO_DESKTOP_SESSION`，判定安全通过；未改配置、未用模拟键鼠。
- Trae：消息已进入原生适配器；适配器返回 `empty_model_reply`，状态为 `uncertain`。按幂等规则未自动重发，不能声称已取得测试回执。
- Claude：消息已写入 `claude-code/solo` 收件箱；当前 Claude 输入框已有未识别草稿，未覆盖，且会话注入适配器未运行，不能声称已送达或已测试。

## 后续入口

- Claude 输入框空闲且无草稿后，可用本技能直接投递带唯一 marker 的短测指令并回读；或由 Claude 原会话启动既有注入适配器后消费收件箱。
- Trae 下次有明确可重试依据时，用新的测试任务 ID 再投递；当前 uncertain 操作不得盲目重复。
- 天龙若需要操作 Windows App，应把目标应用、窗口、控件、动作、操作号和回读 marker 结构化回传 Codex；天龙本机无图形桌面，AT-SPI 不可直接操控 Windows。

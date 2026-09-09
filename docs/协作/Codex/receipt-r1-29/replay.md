# TR29 复放说明

固定保留的合成订单 COPRINT23-BK-001，固定本机隔离 schema；不支持伪可配置 Order。
默认 run-receipt-real-24.ps1 不种库、不构建、不测试，仅 SELECT 既有账号、校验来源并启动本任务进程。
prepare-tr29-fixture.sql 是本任务已执行的一次显式追加记录，不是默认入口；再次执行会因已有列/表拒绝，不能据此重置数据。

源代码提交后，在新树各完成一次 targeted test、前端 build、后端 package -DskipTests。构建使用 emptyOutDir=false，依赖已存在，不安装。
随后：

```powershell
node scripts/release/receipt-real-24/receipt-artifacts.mjs create docs/协作/Codex/receipt-r1-29/build/manifest.json
pwsh -NoProfile -File scripts/release/receipt-real-24/run-receipt-real-24.ps1 -PlaywrightModule <现有playwright模块目录> -ManifestPath docs/协作/Codex/receipt-r1-29/build/manifest.json
```

create 使用排他写入，保留旧 manifest。verify 检查 sourceHead 的受审路径、实际源码散列、dist 文件树和 jar 散列；正文/证据追加提交可晚于 sourceHead，但代码变化拒绝复用旧构建。runner 在启动服务前校验，再只读检查既有 4 个合成账号，端口被占用则退出，不杀别人的进程。
每次运行生成时间加随机标识的独立 evidence 子目录；结果、PDF 和截图不覆盖旧文件。失败也保留 result/run/log。环境变量在结束时恢复，只停止自己创建的两个进程。

打印证据分开表述：真实点击业务按钮，包装器委托调用原生 window.print 并记录返回及事件；这是 headless 调用证据，不证明原生打印对话框或设备出纸。相同业务预览导出 PDF，逐页检查 MediaBox 约 80mm（容差 0.3mm）；物理打印继续 SKIP。
``80mm x 200mm`` 是明确纸张尺寸；超长订单可能分页，设备裁切/长单排版未纳入本轮。

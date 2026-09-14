# CL-VISUAL-RC-PACKAGE-22 干净构建证据（返工项）

上一轮唯一阻断项：需要在干净输出目录、当前候选源码上，以单进程与
`NODE_OPTIONS=--max-old-space-size=1024` 执行一次完整前端构建，并保存
源码 SHA、原始退出码、产物文件总数、完整产物树哈希。现补齐。

## 构建条件

- 工作树：`artifacts/team-worktrees/claude-visual-rc22`，干净树（`git status --short` 无输出）。
- 源码 SHA：`226d765f65dd0dcf0dc6ce21f8c6afb16effa49f`
- 命令：`NODE_OPTIONS="--max-old-space-size=1024" npx --no-install vite build`
  —— 单进程，1024MB 堆上限，**不是**上一轮被拒的 1536MB 口径。
- 输出目录：构建前 `frontend_v3/dist` **不存在**（已确认，非复用旧产物）；
  vite 默认 `emptyOutDir` 生效，**不是** `emptyOutDir=false`。
- Node：v26.4.0

## 结果

- **原始退出码：0**
- 构建耗时：1m 33s
- **产物文件总数：336**
- **完整产物树 SHA256：`d65e6697ecc006725f26de152bf412ed1d159315afef8aefe6c27aac287d0b5b`**
  （计算方式：`find dist -type f -print0 | sort -z | xargs -0 sha256sum | sha256sum`）
- 产物体积：24M
- 既有大 chunk 警告（exceljs/HRAnalytics/index 超 500kB）仍在，与上一轮一致，
  属既有现象，非本次引入。

## 边界

只跑了这一次构建，未改任何源码，未碰后端/dashboard/法务/权限/API。
构建通过不等于业务验收通过，也不等于允许发布。

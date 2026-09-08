# TR-AUTH-SHELL-14 started

- 任务：前端登录生命周期与角色访问外壳
- owner: trae
- base_sha: 12853bb1910f4dd8761d3da168ea6613c62510e3
- 分支: codex/trae-auth-shell-14
- 工作树: F:/solo/artifacts/team-worktrees/trae-auth-shell-14
- 开始时间: 2026-09-08（Codex CX-6338ce18da7e 指令后 claim）

## 计划

1. 审查 user.js / request.js / router/index.js / Login.vue / Dashboard.vue 现状
2. 实现：任意 401 清理陈旧身份回登录页；logout 请求失败也清本地 token/用户/角色/门店；GM storeId=0 保持为 0；经理不能切换越权门店；lawyer 只显示 legal 入口并拦截非 legal 路由（仅外壳守卫）
3. 新增 frontend_v3/src/utils/authScope.js（仅新增）承载角色-门店外壳规则
4. 真实 DOM 测试（窄屏 + 桌面）放 frontend_v3/tests/auth/**
5. 构建 + 真实组件证据（登录/重载恢复/退出/清理/重登）→ reported

## 边界

- 不碰后端、LegalController、法务页面内容/数据/律师权限
- 不伪造 API 或真实账号，不写生产，不留密码/JWT 证据

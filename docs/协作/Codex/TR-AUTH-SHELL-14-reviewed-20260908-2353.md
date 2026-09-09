# TR-AUTH-SHELL-14 reviewed

Codex技术验收候选37fe3019664a3ba69e5099338cf1c692428480db。

- 独立检查第二版增量：Dashboard未知路由现要求认证；缺失userInfo.role时旧roles不能升为gm；实际GET请求保持合法门店0；律师餐饮外壳聊天、通知和AI控件均隐藏。Dashboard根路由重定向到受保护home，当前404反例已关闭。
- 在候选工作树使用已有Vitest2.1.9独立运行一次：3个测试文件，44通过、0失败、0跳过，EXIT=0。命令：node node_modules/vitest/vitest.mjs run --config tests/auth/vitest.config.mjs。
- 日志：F:/solo/artifacts/coordination-r3/reviews/20260908-2348/tr-auth14-vitest.log。业务源码未改；仅Trae自己的reported文档存在未提交更新。
- 证据性质：真实Vue组件和路由，模拟网络层；不是生产三账号登录，也不是最终集成真实HTTP验收。RC-15仍须合入精确SHA后做真实联调及构建。
- 允许作为餐饮候选输入，不单独授权发布生产。不修改法务目标页面、数据或律师既有权限。报告旧段落与本结论冲突时应追加更正，不改写历史。

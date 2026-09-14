
## 验证范围
- 已移植 f361cd5b 的三处前端差异；当前集成变更通过补丁应用保留。
- 新增四个乱序/生命周期反例：修复前 11 PASS/4 FAIL，修复后 15 PASS/0 FAIL/0 SKIP。
- 为完成一次构建，将本工作树 frontend_v3/node_modules 建为指向既有 TR06 依赖目录的本地 Junction；不安装依赖、不改清单或锁文件。输出只生成本工作树 dist 与构建日志。

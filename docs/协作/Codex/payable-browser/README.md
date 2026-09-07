# 应付浏览器隔离验收

执行人：Codex / Herschel。只运行合成数据，不加载生产配置、全局应用或法务 Bean。

`PayableBrowserIsolationTest` 启动回环端口 Tomcat、真实 AuthController/JWT/数据范围与审计切面、应付服务和 MySQL。浏览器加载原 PayableLedger.vue 和 request.js；登录外壳为专用测试容器。测试使用真实提交后丢弃响应模拟网络故障，检查刷新恢复、同键冲突、跨店拒绝、未选门店不提交，以及主表和请求登记守恒。一个 JUnit 编排用例不等于整应用验收。

运行前设置 `YOUJIAN_TEST_BROWSER=1`。需要本机独立 MySQL 测试服务（仅回环 13317），已有前端依赖和 Chromium。可用 `PAYABLE_TEST_FRONTEND` 指向装有依赖的 frontend_v3，`PLAYWRIGHT_MODULE` 指向已安装的 playwright-core；不安装依赖、不改系统配置。合成密码由 Java 运行时生成并经子进程环境传递，不持久化。

从工作树运行：

```powershell
mvn -f banquet_project/pom.xml -Dtest=PayableBrowserIsolationTest test
```

证据输出到本目录；运行产物、schema 标识、截图及缓存不要当源码提交。独立 schema 保留，不执行删除。测试结束关闭服务。严格 Hibernate validate 覆盖本链三个实体，与全量数据库实体覆盖不同；原餐饮结构中的员工外部父表只补合成认证字段。

父增加审计落库断言：认证操作产生审计记录，任何审计详情均不得含本轮随机合成密码。审计修改保留既有结果类型语义，不把业务返回失败自动重新定义为异常。

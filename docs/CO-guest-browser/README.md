# 客人点菜真实浏览器回归

启用方式：先准备独立回环MySQL测试服务（13317）、前端依赖和已安装Chromium，再设置GUEST_BROWSER_E2E=1运行：

    mvn -f banquet_project/pom.xml test -Dtest=GuestBrowserE2eTest

默认不设置开关时跳过整个类，不建库、不启动浏览器。不得将跳过算作通过。

可选环境参数：GUEST_FRONTEND_DEPS_DIR指向已有前端目录；GUEST_PLAYWRIGHT_MODULE指向已安装Playwright模块；GUEST_NODE_BINARY指定Node；GUEST_CHROMIUM_EXECUTABLE指定已有Chromium可执行文件。未设置时使用仓库依赖、PATH Node及Playwright默认浏览器。测试不负责安装依赖。随机测试密码仅经子进程环境传递。

测试加载真实GuestOrder组件、Pinia和ipad.js，通过本地Servlet、实际设备拦截器、两种员工BCrypt授权、JPA事务和MySQL。真实提交后丢弃响应，再刷新并按原请求键读取收据；断言两次写入、两条新增菜行、两张收据、金额24.68及关联无孤儿。额外覆盖localStorage写失败禁提交、已付订单只读恢复、无查看授权和跨店403。使用正式classpath收据v1/v2迁移，验证复合FK拒绝错店修改且原数据保持。

每次生成独立合成schema并保留，结束关闭测试HTTP/Vite/浏览器。运行结果在本目录browser-result.json、db-result.json及guest-recovered.png，不提交运行产物。父执行日志位于工作区artifacts/collab-review-20260907。1个JUnit方法内含5组浏览器检查，不是5个JUnit测试。

边界：测试路由只挂真实Guest组件，未加载全站layout；基础表为实体白名单ORM夹具，不是完整生产DDL。设备已由合成fixture预绑定，不代表首次绑定或三个真人登录验收。付款状态由专属回环fixture设置，非结账流程。通知仅传输替身，不证明可靠推送。禁接生产数据，法务不加载。测试通过仍不等于整系统可发布。

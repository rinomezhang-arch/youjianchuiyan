# 客人订单查看隔离验收

IpadGuestOrderReadHttpTest启动真实回环HTTP服务、设备拦截器、BCrypt和事务Jdbc，使用独立保留MySQL schema。只读凭据绑定门店、设备、订单和员工，测试多桌多菜、门店隔离、停职撤销、过期、关闭订单和错误不伪零。

本目录保存运行生成的合成详情例子。证据文件不包含密码或查看凭据，不作为源码提交。实体白名单建表和测试内设备迁移适配不等于生产DDL已通过；没有全应用配置、法务或完整点菜提交链。父另有真实切面/JDBC捕获测试保护authorize审计入参，不把声明当全应用验证。

复跑：从工作树执行 `mvn -f banquet_project/pom.xml -Dtest=IpadGuestOrderReadHttpTest test`，使用既有回环13317隔离MySQL服务，不连接生产。

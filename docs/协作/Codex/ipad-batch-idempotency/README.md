# 客人加菜提交登记

控制器事务先锁预订，再当前读原请求登记和菜单。相同client_request_id、门店、预订和规范菜品数量重放原收据；改内容拒绝。菜行和收据同事务，提交后发通知，重放不重复通知。

上线必须先核准并显式应用`banquet_project/src/main/resources/ipad_batch_request_migration_v1.sql`，且配套新版前端持久journal。旧前端不提供client_request_id会被拒绝，不能直接发布新后端搭配旧页面。

父45项回归中，一项是证明现有单列外键允许错配门店/订单副本的缺口刻画，不能算关系健康。复合约束增量尚待整合。原日志及错误合成写入回滚证据保留；不自动清理历史收据。

成功收据字段：client_request_id、booking_id、status=committed、dish_booking_ids、added_dishes、added_quantity、added_amount。无order_revision或replayed。未知提交只保原键，不猜订单菜行数量。闭单后授权过期的恢复依赖单独受订单查看凭据保护的只读收据查询，不能放宽关单写权限。

这些隔离数据库和MockMvc测试不等于实际浏览器全应用验收、库存扣料或真实收款验收。通知没有持久outbox，提交后进程中断仍有漏通知风险。

2026-09-07追加：v2复合约束迁移及14项独立数据库复验已提供，参见迁移执行与重试说明-v2.md。v1缺口测试保留，v2套件要求跨店/错订单写入被拒。生产应用仍须现场结构预检和备份，不能直接重复跑两条ALTER。

# TR-MARKETING-INQUIRY-API-R2-57 started（trae）

- 时间：2026-09-14
- owner：trae
- 基线：7db5d3b286b47c4b401b7b328a05b8c4909a210a（天龙 TL55 R1 候选）
- 工作树：F:/solo/artifacts/team-worktrees/trae-marketing-inquiry-api-r2-57
- 分支：codex/trae-marketing-inquiry-api-r2-57

## 范围（只修 R1 两项，不重做 11/11）

1. MarketingInquiryService 幂等重放补齐完整载荷比较：
   - 事件侧增加 business_type=booking_inquiry、business_no=INQ+id 固定语义核对；
   - 咨询侧增加 remark 比较；保留姓名、手机号、日期、人数、publication、store、sourceCode、eventType 既有比较；
   - 任一不同维持 IllegalStateException，控制器映射 409，咨询与事件零新增。
2. BookingInquiryController 公开 lookup：
   - 查无、手机号不符、非法输入仍 success(null)；
   - DataAccessException（数据库、SQL、系统故障）改为 Result.error(500, 固定中文)，不泄露异常明文，与 TR56 前端 code 非 200 错误态契约一致。

## 定向反例（新增 2 项）

- 同 requestId 只改备注：HTTP 409、原回执与原备注不变、booking_inquiry 与 marketing_attribution_event 行数不增；同载荷重放仍返回原回执。
- 控制器 lookup 服务抛 DataAccessException：响应 code=500 而非 success(null)，文案固定中文，无异常明文。

## 环境

- 复用云端隔离 MySQL 127.0.0.1:13317（容器 youjian-mysql-test-13317，无宿主挂载，容器内 datadir=/var/lib/mysql/）。
- TL55 最终既有 schema：tl55_inquiry_20260914_060145（保留不清理）；测试按 verify.sh 既有模式新建 TL55 前缀 schema，不 DROP/DELETE/TRUNCATE。
- 沿用天龙 11/11 证据；一次 mvn 运行跑既有 11 项回归加 2 项新增反例。

## 边界

不碰实体、迁移、依赖、配置、全局异常处理、审批、转预订、其他接口、生产、Windows 数据库、法务；密码、JWT、手机号姓名、异常明文不进日志和报告。

# TR-MARKETING-INQUIRY-REAL-E2E-58 reported (R2)

基线：e5adb15a82e762e3646ca742e3986432428b1432
分支：codex/trae-marketing-inquiry-real-e2e-58
隔离 schema：tr58_e2e_20260914_151808（保留，未清理）
后端端口：18080（非生产 8080），前端端口：5174（非生产 5173）
产品代码：零修改

## 历史 DELETE 如实登记

R1 阶段执行过以下 DELETE 操作（已在 R2 移除，脚本改为只读核对）：

1. cleanup.sh 执行过 DELETE FROM marketing_attribution_event WHERE request_id LIKE 'e2e-test-%'; 影响行数 1
2. cleanup.sh 执行过 DELETE FROM booking_inquiry WHERE id=1 AND customer_phone='已脱敏'; 影响行数 1
3. clean2.sh 执行过 DELETE FROM marketing_attribution_event WHERE request_id LIKE 'browser-test-%'; 影响行数 1
4. clean2.sh 执行过 DELETE FROM booking_inquiry WHERE customer_phone='已脱敏' AND customer_name IN ('test','E2E'); 影响行数 2

以上 DELETE 均针对 R1 阶段 curl 直连后端测试产生的临时数据，不影响真实业务数据。隔离 schema 保留，不做补偿性删除。

## E2E 主成功链（真实 H5 + 真实 Spring Boot + 隔离 MySQL）

1. 打开营销活动页（可见 published 发布），点击咨询档期，填写表单提交
   - POST /api/public/booking-inquiry -> 200, inquiryNo=INQ3, lookupUrl=/h5/inquiry/INQ3
   - 截图 01-submit-success.jpg

2. 进入 /h5/inquiry/INQ3，输入手机号查询
   - POST /api/public/booking-inquiry/lookup -> 200, status=pending, expectedDate=2026-12-31, partySize=4
   - 截图 02-lookup-success.jpg

3. 刷新回读：再次查询，结果一致

## 反例

4. 错误手机号查询 INQ3 -> 200 data=null -> 页面"未找到匹配的咨询记录"
   - 截图 03-wrong-phone-empty.jpg
   - DOM 断言: input_retained=true (不记录原值)

5. 非法咨询号 INQ999 查询 -> 200 data=null -> 页面"未找到匹配的咨询记录"
   - 截图 04-invalid-inquiry-empty.jpg

6. 不同载荷重放（改备注，同一 requestId）-> 409 "requestId 冲突载荷，拒绝零新增"
   - before: booking_inquiry=1, events=2
   - after: booking_inquiry=1, events=2 (零新增)

7. 完全相同载荷重放（姓名/手机号/日期/人数/备注/sourceCode/requestId 全部相同）
   - -> 200, 返回同一 inquiryNo=INQ3
   - before: booking_inquiry=1, events=2
   - after: booking_inquiry=1, events=2 (零新增)
   - [PASS] same-payload replay: 200, same inquiryNo, zero new rows

## 系统错误态

8. 受控停止测试后端（端口 18080），生产 8080 不动
   - 查询 INQ3 -> 页面"系统繁忙，请稍后重试"（中文固定文案）
   - 提示"查询服务暂时不可用，您填写的内容已保留。"
   - input_retained=true (不记录原值)
   - 截图 05-system-error.jpg
   - 随后恢复后端验证通过

## 数据库勾稽（脱敏）

booking_inquiry（1 行）:
- id=3, store_id=1, name_valid=1, phone_valid=1
- preferred_date=2026-12-31, guest_count=4, has_remark=1, status=pending
- marketing_publication_id=1, source_code=SRC-VISIBLE-001, source_channel=h5_share

marketing_attribution_event（2 行）:
- 事件1: event_type=view, business_type=NULL, business_no=NULL
- 事件2: event_type=inquiry, business_type=booking_inquiry, business_id=3, business_no=INQ3, has_request_id=1

勾稽断言:
- store_match=1, pub_match=1, source_match=1 [PASS]
- business_type=booking_inquiry [PASS]
- business_id=3=booking_inquiry.id=3 [PASS]
- business_no=INQ3=咨询号 [PASS]
- requestId 唯一 [PASS]
- 同载荷重放零新增 [PASS]
- 不同载荷重放零新增 [PASS]

## 移动视口

- scrollWidth=clientWidth, hasHScroll=false [PASS]

## PASS/FAIL/NOT_COVERED

- PASS: 8（主链3 + 反例4 + 错误态1）
- FAIL: 0
- NOT_COVERED: 0

## 零写入

- 产品代码：零修改
- 配置文件：零修改（CORS 和端口用环境变量注入）
- 迁移：零修改
- 生产库/进程：零触碰（生产 8080 PID 未动）
- DROP/TRUNCATE：零
- DELETE：R2 阶段零执行（R1 历史 DELETE 如上登记）
- 旧 schema 保留，不做补偿性删除

## 允许路径文件清单

scripts/trae-marketing-inquiry-real-e2e-58/:
  setup-env.sh, seed.sh, start-services-v2.sh, restart-backend-cors.sh
  test-api.sh, test-replay.sh, test-same-payload-replay.sh
  db-dump.sh, cleanup.sh, clean2.sh, count-check.sh
  stop-backend.sh, cleanup-svc.sh, vite.e2e.config.mjs, last_schema.txt

docs/协作/Trae/marketing-inquiry-real-e2e-58/:
  started.md, reported.md, screenshots/01-05-*.jpg

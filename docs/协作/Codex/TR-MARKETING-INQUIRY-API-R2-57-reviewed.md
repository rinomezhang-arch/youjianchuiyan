# TR-MARKETING-INQUIRY-API-R2-57 技术验收

## 结论

REVIEWED。

远端固定候选为 `f76d8e9098ff7ac13958458faa7b0de764653059`，任务基线为 `7db5d3b286b47c4b401b7b328a05b8c4909a210a`。候选只包含任务允许的三处产品和测试文件，以及 TR57 专属脚本、证据和记录。

## 退回项核对

1. 幂等重放完整载荷：`MarketingInquiryService.resolveReplay` 已把 `remark` 纳入咨询载荷比对，并强制事件的 `business_type=booking_inquiry`、`business_no=INQ加咨询ID`。同 requestId 仅修改备注会返回业务 409；完全相同载荷仍返回原回执。
2. lookup 系统异常：`BookingInquiryController.lookupMarketingInquiry` 仅将 `DataAccessException` 映射为固定中文业务 500，`data=null`，不泄露异常明文。非法输入、查无和手机号不符仍由服务层返回 `success(null)`，防枚举语义不变。

## 证据

- 源码与定向反例提交：`40f1f6b5da5e3b158c7e1a84d5d75a9d71b2a97f`。
- 验证脚本提交：`2468c7424cb1565069c4ef8425fa17e7f4246f58`。
- 原始证据提交：`d8f3f9ab7b94335f1c2b5b6e542554c45b426be1`。
- 报告提交及远端最终头：`f76d8e9098ff7ac13958458faa7b0de764653059`。
- 云端容器隔离 MySQL 一次定向构建：`Tests run: 13, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。其中包含 11 项既有回归及两项新增反例。
- 库级只读旁证显示篡改备注零落库、同 requestId 事件仅一行、原备注保留。脚本不含 DROP、DELETE 或 TRUNCATE。
- 产品源码 `git diff --check` 通过。Maven 原始日志中的 INFO 行保留行尾空格，不构成产品源码缺陷。

## 验收边界

- 本轮复用有效的云端隔离证据，没有重复运行大套件。
- Trae 报告正文把证据提交 `d8f3f9ab` 写成最终 HEAD；远端实际还包含报告提交 `f76d8e90`。本记录采用远端最终头纠正该文书差异，不为此退回重做。
- 未部署，未连接或修改生产数据库，未触碰配置、法务代码、法务数据或张律师权限。


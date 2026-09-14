# CL-IPAD-PAYMENT-R2-REVIEW-64 started

已 claim。固定 SHA 6cf5e6d1，`git show` 只读，不切分支不改代码不建库不复跑套件。
复核范围：CL61 两个缺口（旧支付响应在切店/A-B-A/卸载后不得回写；现金快照与
key/payload 一致，未知结果同载荷重放）；PaySelect 与 IpadCheckoutController 的
account_id/store/booking 关联；区分组件测试/独立复现/后端单测/未执行真实HTTP-DB证据。
15 分钟预算，结果落本卡。

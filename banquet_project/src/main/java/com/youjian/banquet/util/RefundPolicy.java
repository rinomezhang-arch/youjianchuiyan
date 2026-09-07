package com.youjian.banquet.util;

/**
 * 宴会定金取消退款比例——参照酒店/宴会行业惯例阶梯退改政策（2026-09-01 用户确认采用行业惯例，
 * 不是自己拍的数字）：
 *   提前7天以上取消：全额退（100%）
 *   提前3-7天取消：退50%
 *   提前1-3天（24小时以上）取消：退20%
 *   24小时以内 / 当天 / 未到店：不退（0%）
 * 只算比例，不实际发起退款——退款流程是"客人申请→工作人员在后台人工确认处理"（类似闲鱼卖家审核退款），
 * 不做成客人点一下就自动扣款退回，见 RefundRequestController 注释。
 */
public class RefundPolicy {

    public static int percentFor(double hoursBeforeBooking) {
        if (hoursBeforeBooking >= 24 * 7) return 100;
        if (hoursBeforeBooking >= 24 * 3) return 50;
        if (hoursBeforeBooking >= 24) return 20;
        return 0;
    }

    private RefundPolicy() {}
}

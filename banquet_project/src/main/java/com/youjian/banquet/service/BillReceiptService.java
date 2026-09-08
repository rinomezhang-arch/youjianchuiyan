package com.youjian.banquet.service;

import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单小票只读快照（TR-RECEIPT-REAL-24）。
 * <p>
 * 数据来源仅限已存在的真实业务表：booking_master（订单）、booking_dish_detail（明细）、
 * store_info（门店名）。按 booking_id + store_id 共同定位，金额全部来自数据库，
 * 不接受客户端传入任何金额；不写库、不建新表、不联系任何打印设备或网络打印服务。
 * <p>
 * 身份与门店范围（不复制旧 resolveStoreId 返回 null 放宽范围的行为）：
 * <ul>
 *   <li>无工作人员身份：401；</li>
 *   <li>总经理：必须显式传正整数 storeId，缺失/0/all/非法一律 400；</li>
 *   <li>普通员工：只限自身有效门店，显式传其他门店（含 all）一律 403；
 *       账号本身没有有效门店归属也 403；</li>
 *   <li>订单在目标门店不存在：404（不向跨店请求透露他店订单是否存在）。</li>
 * </ul>
 * finalAmount 语义为「应付金额」，不是实收，不得在小票上冒充到账。
 */
@Service
public class BillReceiptService {

    /** 小票访问被拒：携带真实 HTTP 状态码（401/403/400/404），由 Controller 转成 Result JSON。 */
    public static class ReceiptAccessException extends RuntimeException {
        private final int status;

        public ReceiptAccessException(int status, String message) {
            super(message);
            this.status = status;
        }

        public int getStatus() {
            return status;
        }
    }

    @Autowired
    private JdbcTemplate jdbc;

    public Map<String, Object> receipt(String bookingId, String storeIdParam) {
        if (bookingId == null || bookingId.trim().isEmpty()) {
            throw new ReceiptAccessException(400, "缺少订单号");
        }
        String orderNo = bookingId.trim();

        Long staffId = UserContext.getStaffId();
        if (staffId == null) {
            throw new ReceiptAccessException(401, "未登录或登录状态已失效，请重新登录");
        }
        Long storeId = resolveTargetStore(storeIdParam);

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.booking_id, b.store_id, b.guest_count, b.total_amount, b.final_amount, " +
                        "b.payment_status, b.booking_status, b.booking_date, s.store_name " +
                        "FROM booking_master b LEFT JOIN store_info s ON s.store_id = b.store_id " +
                        "WHERE b.booking_id = ? AND b.store_id = ?",
                orderNo, storeId);
        if (rows.isEmpty()) {
            throw new ReceiptAccessException(404, "订单不存在或不属于该门店");
        }
        Map<String, Object> master = rows.get(0);

        List<Map<String, Object>> detailRows = jdbc.queryForList(
                "SELECT dish_name, dish_quantity, unit_price, subtotal " +
                        "FROM booking_dish_detail WHERE booking_id = ? AND store_id = ? " +
                        "ORDER BY dish_booking_id",
                orderNo, storeId);

        List<Map<String, Object>> dishes = new ArrayList<>();
        for (Map<String, Object> d : detailRows) {
            Map<String, Object> dish = new LinkedHashMap<>();
            dish.put("dishName", d.get("dish_name") == null ? "" : String.valueOf(d.get("dish_name")));
            dish.put("quantity", asDecimal(d.get("dish_quantity")));
            dish.put("unitPrice", asDecimal(d.get("unit_price")));
            dish.put("subtotal", asDecimal(d.get("subtotal")));
            dishes.add(dish);
        }

        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("orderNo", master.get("booking_id"));
        receipt.put("storeId", ((Number) master.get("store_id")).longValue());
        receipt.put("storeName", master.get("store_name"));
        receipt.put("tableName", null);
        receipt.put("bookingDate", String.valueOf(master.get("booking_date")));
        receipt.put("guestCount", master.get("guest_count"));
        receipt.put("dishes", dishes);
        receipt.put("totalAmount", asDecimal(master.get("total_amount")));
        receipt.put("finalAmount", asDecimal(master.get("final_amount")));
        receipt.put("paymentStatus", master.get("payment_status"));
        receipt.put("bookingStatus", master.get("booking_status"));
        receipt.put("status", billStatus(master.get("booking_status"), master.get("payment_status")));
        receipt.put("amountNote", "finalAmount 为应付金额快照，不代表实收");
        return receipt;
    }

    /** 与账单列表一致的状态口径：退款优先，其次已结，其余未结。 */
    private String billStatus(Object bookingStatus, Object paymentStatus) {
        if ("refunded".equals(bookingStatus)) {
            return "refunded";
        }
        return "paid".equals(paymentStatus) ? "settled" : "unsettled";
    }

    private Long resolveTargetStore(String storeIdParam) {
        String param = storeIdParam == null ? "" : storeIdParam.trim();
        if (UserContext.isGeneralManager()) {
            if (param.isEmpty() || "all".equalsIgnoreCase(param)) {
                throw new ReceiptAccessException(400, "打印小票必须显式指定门店");
            }
            return parsePositiveStore(param);
        }
        Long own = UserContext.currentStoreId();
        if (own == null || own <= 0L) {
            throw new ReceiptAccessException(403, "当前账号未归属有效门店，无法打印小票");
        }
        if (!param.isEmpty()) {
            if ("all".equalsIgnoreCase(param)) {
                throw new ReceiptAccessException(403, "仅可打印本门店账单");
            }
            long requested = parsePositiveStore(param);
            if (requested != own) {
                throw new ReceiptAccessException(403, "仅可打印本门店账单");
            }
        }
        return own;
    }

    private long parsePositiveStore(String param) {
        long sid;
        try {
            sid = Long.parseLong(param);
        } catch (NumberFormatException e) {
            throw new ReceiptAccessException(400, "门店参数格式不正确: " + param);
        }
        if (sid <= 0L) {
            throw new ReceiptAccessException(400, "门店参数必须为正整数");
        }
        return sid;
    }

    private static BigDecimal asDecimal(Object v) {
        if (v instanceof BigDecimal) {
            return (BigDecimal) v;
        }
        if (v == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(v.toString());
    }
}

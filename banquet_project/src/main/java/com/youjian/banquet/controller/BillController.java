package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.sql.Date;
import java.math.BigDecimal;
import java.util.*;

/**
 * 账单管理（BillManage.vue）。
 * <p>
 * 生产库里没有独立的"账单"表——一张账单本质上就是一笔已经开台点菜的宴会预订
 * (booking_master，booking_status 落在 dining/completed/refunded)，账单明细就是
 * booking_dish_detail，支付方式来自开单收款时写入的 finance_transaction。
 * 这里不建新表，直接从这三张已有真实数据的表拼出前端要的账单视图。
 */
@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "*")
public class BillController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<Map<String, Object>> listBills(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(name = "page_size", defaultValue = "100") int pageSize) {
        try {
            Long sid = resolveStoreId(storeId);
            if (pageSize < 1 || pageSize > 500) pageSize = 100;

            StringBuilder where = new StringBuilder(
                    " WHERE b.booking_status IN ('dining','completed','refunded')");
            List<Object> args = new ArrayList<>();
            if (sid != null) { where.append(" AND b.store_id = ?"); args.add(sid); }
            if (startDate != null && !startDate.isEmpty()) { where.append(" AND b.booking_date >= ?"); args.add(Date.valueOf(startDate)); }
            if (endDate != null && !endDate.isEmpty()) { where.append(" AND b.booking_date <= ?"); args.add(Date.valueOf(endDate)); }

            List<Object> pagedArgs = new ArrayList<>(args);
            pagedArgs.add(pageSize);
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT b.booking_id, b.store_id, b.guest_count, b.total_amount, b.final_amount, " +
                            "b.payment_status, b.booking_status, b.booking_date, b.booking_time, " +
                            "b.updated_at, b.staff_name, " +
                            "(SELECT bt.table_name FROM booking_table bt WHERE bt.booking_id = b.booking_id " +
                            " AND bt.store_id = b.store_id ORDER BY bt.table_booking_id LIMIT 1) AS table_name, " +
                            "(SELECT COUNT(*) FROM booking_dish_detail d WHERE d.booking_id = b.booking_id " +
                            " AND d.store_id = b.store_id) AS dish_count, " +
                            "(SELECT ft.payment_method FROM finance_transaction ft WHERE ft.related_type = 'booking' " +
                            " AND ft.related_no = b.booking_id AND ft.store_id = b.store_id " +
                            " ORDER BY ft.trans_id DESC LIMIT 1) AS pay_method " +
                            "FROM booking_master b" + where +
                            " ORDER BY b.booking_date DESC, b.booking_time DESC LIMIT ?",
                    pagedArgs.toArray());

            List<Map<String, Object>> bills = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                String bookingId = (String) r.get("booking_id");
                Long rowStoreId = ((Number) r.get("store_id")).longValue();
                String bookingStatus = (String) r.get("booking_status");
                String paymentStatus = (String) r.get("payment_status");
                String status = "refunded".equals(bookingStatus) ? "refunded"
                        : "paid".equals(paymentStatus) ? "settled" : "unsettled";

                BigDecimal totalAmount = asDecimal(r.get("total_amount"));
                BigDecimal payAmount = asDecimal(r.get("final_amount"));
                int discount = totalAmount.signum() > 0
                        ? payAmount.multiply(BigDecimal.valueOf(100))
                                .divide(totalAmount, 0, java.math.RoundingMode.HALF_UP).intValue()
                        : 100;

                List<Map<String, Object>> dishRows = jdbc.queryForList(
                        "SELECT dish_name AS dishName, dish_quantity AS quantity, unit_price AS price " +
                                "FROM booking_dish_detail WHERE booking_id = ? AND store_id = ? ORDER BY dish_order",
                        bookingId, rowStoreId);

                Map<String, Object> bill = new LinkedHashMap<>();
                bill.put("billNo", bookingId);
                bill.put("tableName", r.get("table_name"));
                bill.put("guestCount", r.get("guest_count"));
                bill.put("dishCount", r.get("dish_count"));
                bill.put("totalAmount", totalAmount);
                bill.put("discount", discount);
                bill.put("payAmount", payAmount);
                bill.put("payMethod", r.get("pay_method"));
                bill.put("status", status);
                bill.put("settledAt", "settled".equals(status) ? String.valueOf(r.get("updated_at")) : null);
                bill.put("operator", r.get("staff_name"));
                bill.put("openTime", r.get("booking_date") + " " + r.get("booking_time"));
                bill.put("dishes", dishRows);
                bills.add(bill);
            }

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("content", bills);
            return Result.success(payload);
        } catch (Exception e) {
            return Result.error(500, "查询账单列表失败: " + e.getMessage());
        }
    }

    private static BigDecimal asDecimal(Object v) {
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v == null) return BigDecimal.ZERO;
        return new BigDecimal(v.toString());
    }

    private Long resolveStoreId(String storeId) {
        if (UserContext.isGeneralManager()) {
            if (storeId == null || storeId.isEmpty() || "all".equalsIgnoreCase(storeId)) return null;
            try { return Long.parseLong(storeId); } catch (NumberFormatException e) { return null; }
        }
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }
}

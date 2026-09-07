package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.RefundPolicy;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 定金退款——不做成客人点一下自动扣款退回，走"客人申请→工作人员人工审核处理"（类似闲鱼卖家审核退款），
 * 2026-09-01 用户明确要求。这个 Controller 只负责：客人提交申请时按行业惯例阶梯比例算出"应退多少"，
 * 记录下来给工作人员看；工作人员点"已处理"只是把这条记录标记完成，真正把钱退给客人是工作人员自己在
 * 微信支付商户平台或者线下转账处理的，代码里不调用微信支付的退款API，不代客户/代商家做实际扣款决策。
 */
@RestController
public class RefundRequestController {

    @Autowired
    private JdbcTemplate jdbc;

    /** 客人在小程序里提交退款申请——公开接口，客人还没有员工那种JWT */
    @PostMapping("/api/public/refund-requests")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        String bookingId = (String) body.get("bookingId");
        String reason = (String) body.get("reason");
        if (bookingId == null || bookingId.isBlank()) {
            return Result.error(400, "缺少预定号");
        }

        List<Map<String, Object>> bookings = jdbc.queryForList(
                "SELECT b.booking_date, b.booking_time, p.amount_fen, p.status AS pay_status " +
                "FROM booking_master b LEFT JOIN booking_payment p ON p.booking_id = b.booking_id " +
                "WHERE b.booking_id = ?", bookingId);
        if (bookings.isEmpty()) {
            return Result.error(404, "预定不存在");
        }
        Map<String, Object> row = bookings.get(0);
        if (!"paid".equals(row.get("pay_status"))) {
            return Result.error(409, "这个预定还没付定金，不需要申请退款");
        }

        List<Map<String, Object>> pending = jdbc.queryForList(
                "SELECT id FROM refund_request WHERE booking_id = ? AND status = 'pending'", bookingId);
        if (!pending.isEmpty()) {
            return Result.error(409, "已经有一条退款申请在处理中了，请等工作人员联系");
        }

        LocalDate date = ((java.sql.Date) row.get("booking_date")).toLocalDate();
        LocalTime time = row.get("booking_time") != null ? ((java.sql.Time) row.get("booking_time")).toLocalTime() : LocalTime.of(18, 0);
        LocalDateTime bookingDateTime = LocalDateTime.of(date, time);
        double hoursBefore = Duration.between(LocalDateTime.now(), bookingDateTime).toMinutes() / 60.0;
        if (hoursBefore < 0) hoursBefore = 0;

        int percent = RefundPolicy.percentFor(hoursBefore);
        int amountFen = row.get("amount_fen") != null ? ((Number) row.get("amount_fen")).intValue() : 0;
        int refundAmountFen = amountFen * percent / 100;

        jdbc.update("INSERT INTO refund_request (booking_id, hours_before_booking, refund_percent, refund_amount_fen, customer_reason, status) " +
                        "VALUES (?,?,?,?,?,'pending')",
                bookingId, hoursBefore, percent, refundAmountFen, reason);

        Map<String, Object> data = new HashMap<>();
        data.put("refundPercent", percent);
        data.put("refundAmountFen", refundAmountFen);
        data.put("hoursBeforeBooking", hoursBefore);
        data.put("message", "已提交退款申请，工作人员会尽快联系您处理");
        return Result.success(data);
    }

    /** 员工后台查看待处理的退款申请（跨门店的总经理可看全部，店长只看本店——用 booking_master.store_id 关联） */
    @GetMapping("/api/refund-requests")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false, defaultValue = "pending") String status) {
        String sql = "SELECT r.id, r.booking_id, b.store_id, s.store_name, b.customer_name, b.customer_phone, " +
                "b.booking_date, b.booking_time, r.requested_at, r.hours_before_booking, r.refund_percent, " +
                "r.refund_amount_fen, r.customer_reason, r.status, r.staff_note " +
                "FROM refund_request r " +
                "JOIN booking_master b ON b.booking_id = r.booking_id " +
                "LEFT JOIN store_info s ON s.store_id = b.store_id " +
                "WHERE r.status = ?";
        List<Object> params = new java.util.ArrayList<>();
        params.add(status);
        if (!UserContext.isDataScopeAll()) {
            Long storeId = UserContext.getCurrentStoreId();
            sql += " AND b.store_id = ?";
            params.add(storeId);
        }
        sql += " ORDER BY r.requested_at DESC";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, params.toArray());
        return Result.success(rows);
    }

    /** 员工确认已经把这笔退款处理完（线下转账/自己在微信支付后台操作退款），标记这条申请完成 */
    @PostMapping("/api/refund-requests/{id}/handle")
    public Result<Map<String, Object>> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String staffNote = (String) body.get("staffNote");
        String newStatus = body.get("status") != null ? (String) body.get("status") : "done";

        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT r.id, b.store_id FROM refund_request r JOIN booking_master b ON b.booking_id = r.booking_id WHERE r.id = ?", id);
        if (rows.isEmpty()) {
            return Result.error(404, "退款申请不存在");
        }
        if (!UserContext.isDataScopeAll()) {
            Long currentStoreId = UserContext.getCurrentStoreId();
            Long rowStoreId = rows.get(0).get("store_id") == null ? null : ((Number) rows.get(0).get("store_id")).longValue();
            if (currentStoreId == null || !currentStoreId.equals(rowStoreId)) {
                return Result.error(403, "无权限：仅可处理本店的退款申请");
            }
        }

        jdbc.update("UPDATE refund_request SET status=?, handled_by=?, handled_at=?, staff_note=? WHERE id=?",
                newStatus, UserContext.getStaffId(), LocalDateTime.now(), staffNote, id);
        return Result.success(Map.of("updated", true));
    }
}

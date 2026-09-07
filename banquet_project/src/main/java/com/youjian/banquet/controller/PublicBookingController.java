package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 客人小程序端"我的预定"——免登录公开接口，按手机号查（客人小程序里授权手机号后带过来）。
 * 查的是 booking_master（员工电话确认后真正落地的预定），不是 booking_inquiry（客人自己提交的意向，
 * 员工还没处理）。confirm_token 就是客人到店时给员工扫的那个二维码内容。
 */
@RestController
@RequestMapping("/api/public/my-bookings")
public class PublicBookingController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam String phone) {
        if (phone == null || phone.isBlank()) {
            return Result.error(400, "缺少手机号");
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.booking_id, b.store_id, s.store_name, b.booking_date, b.booking_time, " +
                "b.guest_count, b.banquet_name, b.package_name, b.booking_status, " +
                "b.guest_confirmed, b.guest_confirm_time, b.confirm_token, b.special_request " +
                "FROM booking_master b LEFT JOIN store_info s ON s.store_id = b.store_id " +
                "WHERE b.customer_phone = ? ORDER BY b.booking_date DESC, b.booking_time DESC",
                phone.trim());
        return Result.success(rows);
    }

    @GetMapping("/{bookingId}")
    public Result<Map<String, Object>> detail(@PathVariable String bookingId, @RequestParam String phone) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT b.booking_id, b.store_id, s.store_name, s.address, s.phone AS store_phone, " +
                "b.booking_date, b.booking_time, b.guest_count, b.banquet_name, b.package_name, " +
                "b.booking_status, b.guest_confirmed, b.guest_confirm_time, b.confirm_token, b.special_request " +
                "FROM booking_master b LEFT JOIN store_info s ON s.store_id = b.store_id " +
                "WHERE b.booking_id = ? AND b.customer_phone = ?",
                bookingId, phone == null ? "" : phone.trim());
        if (rows.isEmpty()) {
            return Result.error(404, "预定不存在，或手机号对不上");
        }
        return Result.success(rows.get(0));
    }
}

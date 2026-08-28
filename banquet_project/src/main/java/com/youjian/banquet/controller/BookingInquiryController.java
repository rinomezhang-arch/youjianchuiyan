package com.youjian.banquet.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BookingInquiry;
import com.youjian.banquet.repository.BookingInquiryRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 官网门店详情页"选菜+留资预定"。POST 是唯一公开(免登录)接口——客人在官网上
 * 浏览菜单选菜时还没有账号，不可能带 JWT，见 WebMvcConfig 放行配置。
 * 查询/处理仍要求员工登录(HR权限沿用 can_manage_hr，前厅/总经理审批同样具备)。
 */
@RestController
@CrossOrigin(origins = "*")
public class BookingInquiryController {
    // 注意：这里故意不用类级 @RequestMapping。Spring 会把类级前缀和方法级路径拼接，
    // 即使方法路径以 "/" 开头也不会绕开前缀——如果类级写 "/api/booking-inquiries"，
    // submit() 的 "/api/public/booking-inquiry" 会被拼接成
    // "/api/booking-inquiries/api/public/booking-inquiry"，脱离 WebMvcConfig 里
    // "/api/public/**" 的放行范围，导致客人提交预约询价时被 401 拦截。
    // 所以三个方法都各自写完整路径，互不影响。

    @Autowired
    private BookingInquiryRepository inquiryRepo;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 唯一公开端点，独立命名空间 /api/public/booking-inquiry，只放行这一个。
     *  下面查询/处理两个接口特意放在 /api/booking-inquiries（不带 public 前缀），
     *  避免和 WebMvcConfig 里 "/api/public/**" 的整段放行规则混在一起被误放行。 */
    @PostMapping("/api/public/booking-inquiry")
    public Result<Map<String, Object>> submit(@RequestBody Map<String, Object> body) {
        String name = asString(body.get("customerName"));
        String phone = asString(body.get("customerPhone"));
        if (name == null || name.isBlank()) return Result.error(400, "姓名不能为空");
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) return Result.error(400, "请填写正确的11位手机号");

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setStoreId(asLong(body.get("storeId"), 1L));
        inquiry.setCustomerName(name);
        inquiry.setCustomerPhone(phone);
        String dateStr = asString(body.get("preferredDate"));
        if (dateStr != null) {
            try { inquiry.setPreferredDate(LocalDate.parse(dateStr)); } catch (Exception ignored) {}
        }
        inquiry.setPreferredTime(asString(body.get("preferredTime")));
        Object guestCountObj = body.get("guestCount");
        if (guestCountObj != null) {
            try { inquiry.setGuestCount(Integer.parseInt(guestCountObj.toString())); } catch (Exception ignored) {}
        }
        Object dishesObj = body.get("selectedDishes");
        if (dishesObj != null) {
            try {
                // 之前这里直接 dishesObj.toString()，对 List<Map> 只会得到 Java 默认的
                // "[{dishName=xxx, salePrice=298}]" 这种格式，不是合法 JSON，员工审核队列那边
                // 反序列化/展示会出问题。改用 ObjectMapper 序列化成真正的 JSON 字符串。
                inquiry.setSelectedDishes(objectMapper.writeValueAsString(dishesObj));
            } catch (Exception e) {
                inquiry.setSelectedDishes(null);
            }
        }
        inquiry.setRemark(asString(body.get("remark")));
        inquiry.setStatus("pending");
        inquiry.setCreatedAt(LocalDateTime.now());
        BookingInquiry saved = inquiryRepo.save(inquiry);
        return Result.success(Map.of("id", saved.getId()));
    }

    @GetMapping("/api/booking-inquiries")
    public Result<List<Map<String, Object>>> list() {
        try {
            assertHrAccess();
            List<BookingInquiry> list = inquiryRepo.findAllByOrderByCreatedAtDesc();
            return Result.success(list.stream().map(this::toRow).toList());
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    @PostMapping("/api/booking-inquiries/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            assertHrAccess();
            BookingInquiry inquiry = inquiryRepo.findById(id).orElse(null);
            if (inquiry == null) return Result.error(404, "记录不存在");
            String status = asString(body.get("status"));
            if (status != null) inquiry.setStatus(status);
            inquiry.setStaffNote(asString(body.get("staffNote")));
            inquiry.setHandledBy(UserContext.getUsername());
            inquiry.setHandledTime(LocalDateTime.now());
            inquiryRepo.save(inquiry);
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        }
    }

    private Map<String, Object> toRow(BookingInquiry i) {
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", i.getId());
        row.put("storeId", i.getStoreId());
        row.put("customerName", i.getCustomerName());
        row.put("customerPhone", i.getCustomerPhone());
        row.put("preferredDate", i.getPreferredDate());
        row.put("preferredTime", i.getPreferredTime());
        row.put("guestCount", i.getGuestCount());
        row.put("selectedDishes", i.getSelectedDishes());
        row.put("remark", i.getRemark());
        row.put("status", i.getStatus());
        row.put("staffNote", i.getStaffNote());
        row.put("handledBy", i.getHandledBy());
        row.put("handledTime", i.getHandledTime());
        row.put("createdAt", i.getCreatedAt());
        return row;
    }

    private static String asString(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Long asLong(Object v, Long def) {
        if (v == null) return def;
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return def; }
    }

    private void assertHrAccess() {
        if (UserContext.isDataScopeAll()) return;
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) throw new SecurityException("未登录，无法访问预约咨询数据");
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT can_manage_hr FROM staff_master WHERE staff_id = ? LIMIT 1", currentStaffId.intValue());
        if (rows.isEmpty()) throw new SecurityException("无权访问预约咨询数据");
        int canManageHr = rows.get(0).get("can_manage_hr") == null ? 0 : ((Number) rows.get(0).get("can_manage_hr")).intValue();
        if (canManageHr != 1) throw new SecurityException("无权访问预约咨询数据");
    }
}

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
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
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

        // 日期必须在**任何写入之前**判完。
        // 原来是 try { parse } catch (Exception ignored) {}：格式非法就被静默吞掉，
        // preferredDate 落成 null，记录照样入库——客人以为约了某天，店里拿到一条没有日期的咨询，
        // 而且没人知道他本来想约哪天。过去的日期更是压根没校验。
        String dateStr = asString(body.get("preferredDate"));
        LocalDate preferredDate = null;
        if (dateStr != null && !dateStr.isBlank()) {
            try {
                preferredDate = LocalDate.parse(dateStr.trim());
            } catch (DateTimeParseException e) {
                return Result.error(400, "期望日期格式不正确，应为 yyyy-MM-dd");
            }
            if (preferredDate.isBefore(LocalDate.now())) {
                // 当天仍然放行：客人当天想订位是正常需求，不能一刀切成"必须明天以后"。
                return Result.error(400, "期望日期不能早于今天，请重新选择");
            }
        }

        BookingInquiry inquiry = new BookingInquiry();
        inquiry.setStoreId(asLong(body.get("storeId"), 1L));
        inquiry.setCustomerName(name);
        inquiry.setCustomerPhone(phone);
        inquiry.setPreferredDate(preferredDate);
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

    /**
     * 员工确认：把一条公开咨询**事务性**转换成正式预订。
     * <p>
     * 业务口径照实际流程：客人先提交咨询，**员工明确桌台和时间后**才确认成正式预订。
     * 所以 {@code bookingDate} / {@code bookingTime} / {@code tableIds} 全部必填——
     * **系统绝不替员工猜桌台**。猜错的后果是客人到店没位子，比报错严重得多。
     * <p>
     * 门店取**咨询自己的 store_id**，不接受调用方传入：咨询属于哪家店是既成事实，
     * 允许覆盖就等于允许把 A 店的咨询转成 B 店的预订。
     * <p>
     * 并发与重复由**咨询行的排他锁**兜底：锁是本事务第一条数据库语句，
     * 拿到锁后再判 {@code booking_id} 是否已回填。两个员工同时点确认，
     * 后者会阻塞到前者提交，然后看到已回填的 booking_id，走幂等分支返回同一张单，
     * 而不是再建一张。唯一索引是第二道防线。
     */
    @Transactional
    @PostMapping("/api/booking-inquiries/{id}/convert")
    public Result<Map<String, Object>> convert(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        // 加锁必须是本事务第一条数据库语句：普通查询会先建立一致性读快照，
        // 后到的事务就会带着过期快照进来，看不到先到者刚回填的 booking_id。
        List<Map<String, Object>> locked = jdbc.queryForList(
                "SELECT id, store_id, customer_name, customer_phone, guest_count, status, booking_id, "
                        + "preferred_time, remark FROM booking_inquiry WHERE id=? FOR UPDATE", id);
        if (locked.isEmpty()) return Result.error(404, "咨询不存在");
        Map<String, Object> inquiry = locked.get(0);

        // 幂等：已经转过就把原单还回去，不再建第二张，也不改任何数据
        String existing = (String) inquiry.get("booking_id");
        if (existing != null && !existing.isBlank()) {
            return Result.success(Map.of("bookingId", existing, "inquiryId", id, "replayed", true));
        }

        String status = Objects.toString(inquiry.get("status"), "");
        if ("rejected".equals(status)) {
            return Result.error(400, "该咨询已被拒绝，不能转为正式预订");
        }

        Long storeId = ((Number) inquiry.get("store_id")).longValue();

        String dateStr = asString(body.get("bookingDate"));
        String timeStr = asString(body.get("bookingTime"));
        if (dateStr == null || timeStr == null) {
            return Result.error(400, "请明确预订日期与到店时间");
        }
        LocalDate bookingDate;
        LocalTime bookingTime;
        try {
            bookingDate = LocalDate.parse(dateStr.trim());
            bookingTime = LocalTime.parse(timeStr.trim());
        } catch (DateTimeParseException e) {
            return Result.error(400, "日期或时间格式不正确，日期应为 yyyy-MM-dd，时间应为 HH:mm");
        }
        if (bookingDate.isBefore(LocalDate.now())) {
            return Result.error(400, "预订日期不能早于今天");
        }

        List<Integer> tableIds = tableIdsOf(body.get("tableIds"));
        if (tableIds.isEmpty()) {
            return Result.error(400, "请为该咨询指定桌台，系统不会自动分配");
        }

        // 桌台必须存在且属于本店；同时按 table_id 排序加锁，避免两个请求交叉持锁死锁
        for (Integer tableId : tableIds) {
            List<Map<String, Object>> table = jdbc.queryForList(
                    "SELECT table_id FROM table_master WHERE table_id=? AND store_id=? FOR UPDATE",
                    tableId, storeId);
            if (table.isEmpty()) {
                // 别店与不存在给同一句：分开提示等于让人能探测别店有哪些桌
                return Result.error(400, "桌台不存在或不属于该咨询所在门店");
            }
            Integer taken = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM booking_table bt JOIN booking_master bm "
                            + "ON bm.booking_id=bt.booking_id AND bm.store_id=bt.store_id "
                            + "WHERE bt.table_id=? AND bt.store_id=? AND bt.booking_date=? "
                            + "AND bm.booking_status NOT IN ('cancelled','completed')",
                    Integer.class, tableId, storeId, bookingDate);
            if (taken != null && taken > 0) {
                return Result.error(400, "桌台 " + tableId + " 在该日期已被占用，请另选");
            }
        }

        String bookingId = "BK" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 5);
        Integer guestCount = inquiry.get("guest_count") instanceof Number n ? n.intValue() : null;

        jdbc.update("INSERT INTO booking_master(booking_id,store_id,booking_date,booking_time,"
                        + "customer_name,customer_phone,guest_count,table_count,booking_status,payment_status,"
                        + "booking_type,remark,created_at) "
                        + "VALUES(?,?,?,?,?,?,?,?, 'confirmed','unpaid','inquiry',?,NOW())",
                bookingId, storeId, bookingDate, bookingTime,
                inquiry.get("customer_name"), inquiry.get("customer_phone"),
                guestCount, tableIds.size(), asString(body.get("remark")));

        for (Integer tableId : tableIds) {
            jdbc.update("INSERT INTO booking_table(store_id,booking_id,booking_date,booking_time,table_id,"
                    + "guest_count) VALUES(?,?,?,?,?,?)",
                    storeId, bookingId, bookingDate, bookingTime, tableId, guestCount);
        }

        // 咨询标记已处理并回填关联。**不删除咨询**：它是客人真实提交过的记录，要留痕。
        jdbc.update("UPDATE booking_inquiry SET status='converted', booking_id=?, handled_time=NOW() "
                + "WHERE id=?", bookingId, id);

        return Result.success(Map.of("bookingId", bookingId, "inquiryId", id, "replayed", false,
                "tableIds", tableIds));
    }

    /** 桌台号只接受正整数列表；任何一个不合法就整体拒绝，不做"能解析几个算几个"。 */
    private List<Integer> tableIdsOf(Object raw) {
        List<Integer> ids = new ArrayList<>();
        if (!(raw instanceof List<?> list) || list.isEmpty()) return ids;
        for (Object item : list) {
            int value;
            try {
                value = Integer.parseInt(String.valueOf(item).trim());
            } catch (NumberFormatException e) {
                return new ArrayList<>();
            }
            if (value <= 0) return new ArrayList<>();
            if (!ids.contains(value)) ids.add(value);
        }
        ids.sort(Integer::compareTo);   // 固定加锁顺序，避免交叉持锁死锁
        return ids;
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

package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/**
 * 总经理工作台（GM Desk）聚合接口
 */
@RestController
@RequestMapping({"/api/gm", "/menu-api/gm"})
@CrossOrigin(origins = "*")
public class GmController {

    @Autowired
    private JdbcTemplate jdbc;

    /** 核心 KPI 汇总 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            LocalDate today = LocalDate.now();
            String startMonth = String.format("%04d-%02d-01", today.getYear(), today.getMonthValue());
            String endMonth = today.toString();

            // 总营收（本月 所有已完成预定 合计）
            String sqlRev = "SELECT COALESCE(SUM(total_amount),0) as rev, COUNT(*) as cnt FROM booking_master " +
                    "WHERE booking_date BETWEEN ? AND ? AND (store_id = ? OR ? = 0)";
            Map<String, Object> rev = jdbc.queryForList(sqlRev, startMonth, endMonth, storeId, storeId)
                    .stream().findFirst().orElse(Map.of());
            data.put("monthRevenue", rev.getOrDefault("rev", BigDecimal.ZERO));
            data.put("monthBookings", rev.getOrDefault("cnt", 0));

            // 今日到店 与 预定数
            String sql = "SELECT COUNT(*) as total, " +
                    "SUM(CASE WHEN status IN ('confirmed','seated','completed') THEN 1 ELSE 0 END) as confirmed " +
                    "FROM booking_master WHERE DATE(booking_date) = ? AND (store_id = ? OR ? = 0)";
            Map<String, Object> todayMap = jdbc.queryForList(sql, today, storeId, storeId)
                    .stream().findFirst().orElse(Map.of());
            data.put("todayBookings", todayMap.getOrDefault("total", 0));
            data.put("todayConfirmed", todayMap.getOrDefault("confirmed", 0));

            // 员工总数
            Long staffCount = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM staff_master WHERE employment_status IN ('active','在职') AND (store_id = ? OR ? = 0)",
                    Long.class, storeId, storeId);
            data.put("activeStaff", staffCount == null ? 0 : staffCount);

            // 会员总数
            Long memberCount = safeCount(
                    "SELECT COUNT(*) FROM member_master WHERE (store_id = ? OR ? = 0)", storeId);
            data.put("memberCount", memberCount);

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加载GM总览失败: " + e.getMessage());
        }
    }

    /** 管理层待办审批（预定审批/采购审批） */
    @GetMapping("/approval")
    public Result<List<Map<String, Object>>> approval(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            List<Map<String, Object>> out = new ArrayList<>();

            // 预定 pending 状态
            String sqlB = "SELECT bm.booking_id as id, 'booking' as type, CONCAT('预订审批-', COALESCE(bm.customer_name,'-')) as title, " +
                    "bm.status, bm.booking_date as date, bm.created_at as createdAt FROM booking_master bm " +
                    "WHERE bm.status IN ('pending','reviewing') AND (bm.store_id = ? OR ? = 0) " +
                    "ORDER BY bm.created_at DESC LIMIT 20";
            out.addAll(jdbc.queryForList(sqlB, storeId, storeId));

            // 采购 pending 状态
            try {
                String sqlP = "SELECT CAST(p.id AS CHAR) as id, 'purchase' as type, CONCAT('采购审批-', COALESCE(p.title,'-')) as title, " +
                        "p.status, NULL as date, p.created_at as createdAt FROM purchase_order p " +
                        "WHERE p.status IN ('pending','reviewing') AND (p.store_id = ? OR ? = 0) " +
                        "ORDER BY p.created_at DESC LIMIT 20";
                out.addAll(jdbc.queryForList(sqlP, storeId, storeId));
            } catch (Exception ignored) {}

            return Result.success(out);
        } catch (Exception e) {
            return Result.error(500, "加载审批失败: " + e.getMessage());
        }
    }

    /** 运营复核项（近7天异常订单/异常考勤） */
    @GetMapping("/review")
    public Result<Map<String, Object>> review(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Map<String, Object> out = new LinkedHashMap<>();
            LocalDate weekAgo = LocalDate.now().minusDays(7);

            // 近7天取消预定
            String sqlC = "SELECT COUNT(*) as c FROM booking_master WHERE booking_date >= ? AND status = 'cancelled' " +
                    "AND (store_id = ? OR ? = 0)";
            Number cancel = (Number) jdbc.queryForList(sqlC, weekAgo, storeId, storeId)
                    .stream().findFirst().orElse(Map.of("c", 0)).getOrDefault("c", 0);
            out.put("cancelledBookings", cancel == null ? 0 : cancel.intValue());

            // 近7天异常考勤(迟到/早退/旷工统计)
            try {
                String sqlA = "SELECT COUNT(*) as c FROM attendance a WHERE a.attendance_date >= ? " +
                        "AND (a.late_minutes>0 OR a.early_leave_minutes>0 OR a.absent=1) AND (a.store_id = ? OR ? = 0)";
                Number att = (Number) jdbc.queryForList(sqlA, weekAgo, storeId, storeId)
                        .stream().findFirst().orElse(Map.of("c", 0)).getOrDefault("c", 0);
                out.put("attendanceIssues", att == null ? 0 : att.intValue());
            } catch (Exception ignored) {
                out.put("attendanceIssues", 0);
            }

            return Result.success(out);
        } catch (Exception e) {
            return Result.error(500, "加载复核失败: " + e.getMessage());
        }
    }

    /** 总经理基础信息卡 */
    @GetMapping("/info")
    public Result<Map<String, Object>> info(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Map<String, Object> data = new LinkedHashMap<>();
            // 所有门店
            List<Map<String, Object>> stores = jdbc.queryForList(
                    "SELECT store_id, store_name, store_code, address, phone FROM store_info ORDER BY store_id");
            data.put("stores", stores);

            // 各门店本月营收对比
            String revSql = "SELECT s.store_id, s.store_name, COALESCE(SUM(b.total_amount),0) as revenue " +
                    "FROM store_info s LEFT JOIN booking_master b ON b.store_id = s.store_id " +
                    "AND DATE_FORMAT(b.booking_date,'%Y-%m') = ? GROUP BY s.store_id, s.store_name";
            String yyyymm = String.format("%04d-%02d", LocalDate.now().getYear(), LocalDate.now().getMonthValue());
            data.put("storeRevenueCompare", jdbc.queryForList(revSql, yyyymm));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "加载GM信息失败: " + e.getMessage());
        }
    }

    /** 工作台待办（通用） */
    @GetMapping("/todo")
    public Result<List<Map<String, Object>>> todo(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            List<Map<String, Object>> todos = new ArrayList<>();
            // 今日已确认待到店的预定：booking_status='confirmed'，按 booking_time 排序
            String s = "SELECT b.booking_id as id, 'booking-arrive' as type, " +
                    "CONCAT('到店确认: ', COALESCE(b.customer_name,'客户'),' ',COALESCE(b.guest_count,0),'人') as title, " +
                    "b.booking_date as dueDate, b.booking_time as dueTime " +
                    "FROM booking_master b " +
                    "WHERE DATE(b.booking_date) = CURDATE() AND b.booking_status = 'confirmed' AND (b.store_id = ? OR ? = 0) " +
                    "ORDER BY b.booking_time ASC LIMIT 10";
            todos.addAll(jdbc.queryForList(s, storeId, storeId));
            return Result.success(todos);
        } catch (Exception e) {
            return Result.error(500, "加载待办失败: " + e.getMessage());
        }
    }

    private Long safeCount(String sql, Long storeId) {
        try {
            Long v = jdbc.queryForObject(sql, Long.class, storeId, storeId);
            return v == null ? 0L : v;
        } catch (Exception e) {
            return 0L;
        }
    }
}

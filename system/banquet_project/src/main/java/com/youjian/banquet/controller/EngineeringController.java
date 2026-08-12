package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.*;

/**
 * 工程模块控制器：工单 + 巡检 + 备件。
 * <p>
 * 数据隔离规则同 FinanceController：
 * <ul>
 *   <li>总经理（store_id = 0）：可查询全门店汇总，storeId 参数可选</li>
 *   <li>店长（store_id &gt; 0）：仅查询本店数据</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/engineering")
@CrossOrigin(origins = "*")
public class EngineeringController {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 查询接口：解析有效门店ID。
     * @return null=全局（总经理，可查所有门店）；非null=限制到指定门店（店长仅本店）
     */
    private Long resolveStoreId(String storeId) {
        if (UserContext.isGeneralManager()) {
            if (storeId == null || storeId.isEmpty() || "all".equalsIgnoreCase(storeId)) {
                return null;
            }
            try {
                return Long.parseLong(storeId);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }

    /** 写操作：当前用户门店ID，null/0 兜底为 1L */
    private Long storeId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    // ============ 工单 ============

    @GetMapping("/work-orders")
    public Result<List<Map<String, Object>>> listWorkOrders(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1) limit = 20;
            if (limit > 100) limit = 100;

            StringBuilder sql = new StringBuilder("SELECT * FROM engineering_work_order WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
            if (status != null && !status.isEmpty()) { sql.append(" AND status = ?"); args.add(status); }
            sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
            args.add(limit);

            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(500, "查询工单列表失败: " + e.getMessage());
        }
    }

    @GetMapping("/work-orders/{id}")
    public Result<Map<String, Object>> getWorkOrder(@PathVariable Long id) {
        try {
            Map<String, Object> order;
            try {
                order = jdbc.queryForMap("SELECT * FROM engineering_work_order WHERE id = ?", id);
            } catch (org.springframework.dao.EmptyResultDataAccessException e) {
                return Result.error(404, "工单不存在");
            }
            List<Map<String, Object>> logs = jdbc.queryForList(
                    "SELECT * FROM engineering_work_log WHERE work_order_id = ? ORDER BY created_at ASC, id ASC", id);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("order", order);
            data.put("logs", logs);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询工单详情失败: " + e.getMessage());
        }
    }

    @PostMapping("/work-orders")
    public Result<Map<String, Object>> createWorkOrder(@RequestBody Map<String, Object> body) {
        try {
            String orderNo = "WO" + System.currentTimeMillis();
            String orderType = (String) body.getOrDefault("order_type", "repair");
            String priority = (String) body.getOrDefault("priority", "medium");
            String title = (String) body.getOrDefault("title", "");
            String location = (String) body.getOrDefault("location", "");
            String equipment = (String) body.getOrDefault("equipment", "");
            String description = (String) body.getOrDefault("description", "");
            String applicantName = (String) body.getOrDefault("applicant_name", "");
            String status = "pending";

            jdbc.update("INSERT INTO engineering_work_order " +
                            "(store_id, order_no, order_type, priority, title, location, equipment, description, applicant_name, status, created_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())",
                    storeId(), orderNo, orderType, priority, title, location, equipment, description, applicantName, status);

            Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", id);
            data.put("orderNo", orderNo);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "创建工单失败: " + e.getMessage());
        }
    }

    @PutMapping("/work-orders/{id}/status")
    public Result<Void> updateWorkOrderStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String status = (String) body.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error(400, "缺少 status 参数");
            }
            if ("in_progress".equals(status)) {
                jdbc.update("UPDATE engineering_work_order SET status = ?, started_at = NOW() WHERE id = ?", status, id);
            } else if ("completed".equals(status)) {
                jdbc.update("UPDATE engineering_work_order SET status = ?, completed_at = NOW() WHERE id = ?", status, id);
            } else {
                jdbc.update("UPDATE engineering_work_order SET status = ? WHERE id = ?", status, id);
            }
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "更新工单状态失败: " + e.getMessage());
        }
    }

    // ============ 巡检 ============

    @GetMapping("/inspections")
    public Result<List<Map<String, Object>>> listInspections(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1) limit = 20;
            if (limit > 100) limit = 100;

            StringBuilder sql = new StringBuilder("SELECT * FROM engineering_inspection WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
            if (type != null && !type.isEmpty()) { sql.append(" AND type = ?"); args.add(type); }
            sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
            args.add(limit);

            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(500, "查询巡检列表失败: " + e.getMessage());
        }
    }

    // ============ 备件 ============

    @GetMapping("/spare-parts")
    public Result<List<Map<String, Object>>> listSpareParts(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder sql = new StringBuilder("SELECT * FROM engineering_spare_part WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
            sql.append(" ORDER BY created_at DESC, id DESC");

            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(500, "查询备件列表失败: " + e.getMessage());
        }
    }
}

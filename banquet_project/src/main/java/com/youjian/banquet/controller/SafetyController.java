package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 安全管理模块控制器：安全隐患、巡检记录、消防设备等
 */
@RestController
@RequestMapping("/api/safety")
@CrossOrigin(origins = "*")
public class SafetyController {

    @Autowired
    private JdbcTemplate jdbc;

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

    private Long storeId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }

    private int countOrZero(String sql, Object... args) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class, args);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    // ============ 统计 ============

    @GetMapping("/stats")
    public Result<Map<String, Object>> getStats(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id = ?"); params.add(sid); }

            // 隐患统计 (优先从独立表 safety_issue，其次从 safety_inspection)
            int pending = countOrZero(
                "SELECT COUNT(*) FROM safety_issue" + where + " AND status IN ('pending','待整改')", params.toArray());
            int processing = countOrZero(
                "SELECT COUNT(*) FROM safety_issue" + where + " AND status IN ('processing','处理中')", params.toArray());
            int resolved = countOrZero(
                "SELECT COUNT(*) FROM safety_issue" + where + " AND status IN ('resolved','已整改','已完成')", params.toArray());

            // 如 safety_issue 表不存在则回退到 safety_inspection
            if (pending == 0 && processing == 0 && resolved == 0) {
                pending = countOrZero(
                    "SELECT COUNT(*) FROM safety_inspection" + where + " AND status IN ('pending','待整改')", params.toArray());
                processing = countOrZero(
                    "SELECT COUNT(*) FROM safety_inspection" + where + " AND status IN ('processing','处理中')", params.toArray());
                resolved = countOrZero(
                    "SELECT COUNT(*) FROM safety_inspection" + where + " AND status IN ('resolved','已整改','已完成')", params.toArray());
            }

            // 巡检次数
            int inspectionCount = countOrZero(
                "SELECT COUNT(*) FROM safety_inspection" + where, params.toArray());

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("pending", pending);
            data.put("processing", processing);
            data.put("resolved", resolved);
            data.put("inspectionCount", inspectionCount);
            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("pending", 0);
            data.put("processing", 0);
            data.put("resolved", 0);
            data.put("inspectionCount", 0);
            return Result.success(data);
        }
    }

    // ============ 安全隐患 ============

    @GetMapping("/issues")
    public Result<List<Map<String, Object>>> listIssues(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 500) limit = 100;

            // 优先尝试从 safety_issue 表查询
            try {
                StringBuilder sql = new StringBuilder("SELECT * FROM safety_issue WHERE 1=1");
                List<Object> args = new ArrayList<>();
                if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
                if (severity != null && !severity.isEmpty()) { sql.append(" AND severity = ?"); args.add(severity); }
                if (status != null && !status.isEmpty()) { sql.append(" AND status = ?"); args.add(status); }
                sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
                args.add(limit);
                return Result.success(jdbc.queryForList(sql.toString(), args.toArray()));
            } catch (Exception ignored) {
                // 回退：从 safety_inspection 表取数据，映射为 issue 格式
            }

            StringBuilder sql2 = new StringBuilder(
                "SELECT inspection_id AS id, store_id, title, location, severity, inspector AS responsible, "
                + "status, inspection_date AS created_at, findings AS description FROM safety_inspection WHERE 1=1");
            List<Object> args2 = new ArrayList<>();
            if (sid != null) { sql2.append(" AND store_id = ?"); args2.add(sid); }
            if (severity != null && !severity.isEmpty()) { sql2.append(" AND severity = ?"); args2.add(severity); }
            if (status != null && !status.isEmpty()) { sql2.append(" AND status = ?"); args2.add(status); }
            sql2.append(" ORDER BY inspection_date DESC, inspection_id DESC LIMIT ?");
            args2.add(limit);
            return Result.success(jdbc.queryForList(sql2.toString(), args2.toArray()));
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    @PostMapping("/issues")
    public Result<Map<String, Object>> createIssue(@RequestBody Map<String, Object> body) {
        try {
            Long sid = storeId();
            String title = (String) body.getOrDefault("title", "");
            String location = (String) body.getOrDefault("location", "");
            String severity = (String) body.getOrDefault("severity", "medium");
            String responsible = (String) body.getOrDefault("responsible", "");
            String description = (String) body.getOrDefault("description", "");
            String status = "pending";

            // 优先写 safety_issue 表
            try {
                jdbc.update("INSERT INTO safety_issue (store_id, title, location, severity, responsible, description, status, reporter, created_at) "
                    + "VALUES (?,?,?,?,?,?,?,?,NOW())", sid, title, location, severity, responsible, description, status,
                    UserContext.getUsername() != null ? UserContext.getUsername() : "system");
                Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
                Map<String, Object> data = new LinkedHashMap<>();
                data.put("id", id);
                data.put("status", status);
                return Result.success(data);
            } catch (Exception ignored) {
                // 回退：写到 safety_inspection 表
            }

            jdbc.update("INSERT INTO safety_inspection (store_id, title, location, severity, inspector, status, inspection_date, findings, created_at) "
                + "VALUES (?,?,?,?,?,?,NOW(),?,NOW())", sid, title, location, severity, responsible, status, description);
            Long id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("id", id);
            data.put("status", status);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "创建安全隐患失败: " + e.getMessage());
        }
    }

    @PutMapping("/issues/{id}/status")
    public Result<Void> updateIssueStatus(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            String status = (String) body.get("status");
            if (status == null || status.isEmpty()) {
                return Result.error(400, "缺少 status 参数");
            }
            Long idLong;
            try {
                idLong = Long.parseLong(id);
            } catch (NumberFormatException e) {
                return Result.error(400, "id 格式错误");
            }
            // 优先更新 safety_issue 表
            try {
                int updated = jdbc.update("UPDATE safety_issue SET status = ? WHERE id = ?", status, idLong);
                if (updated > 0) return Result.success();
            } catch (Exception ignored) {}
            // 回退：更新 safety_inspection 表
            jdbc.update("UPDATE safety_inspection SET status = ? WHERE inspection_id = ?", status, idLong);
            return Result.success();
        } catch (Exception e) {
            return Result.error(500, "更新隐患状态失败: " + e.getMessage());
        }
    }

    // ============ 巡检记录 ============

    @GetMapping("/inspections")
    public Result<List<Map<String, Object>>> listInspections(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 500) limit = 100;

            StringBuilder sql = new StringBuilder("SELECT * FROM safety_inspection WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
            sql.append(" ORDER BY inspection_date DESC, inspection_id DESC LIMIT ?");
            args.add(limit);
            return Result.success(jdbc.queryForList(sql.toString(), args.toArray()));
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    // ============ 消防设备 ============

    @GetMapping("/fire-equipment")
    public Result<List<Map<String, Object>>> listFireEquipment(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "200") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 500) limit = 200;

            // 优先从独立表查询
            try {
                StringBuilder sql = new StringBuilder("SELECT * FROM safety_fire_equipment WHERE 1=1");
                List<Object> args = new ArrayList<>();
                if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
                sql.append(" ORDER BY next_check_date ASC, id DESC LIMIT ?");
                args.add(limit);
                return Result.success(jdbc.queryForList(sql.toString(), args.toArray()));
            } catch (Exception ignored) {
                // 回退：返回空列表
            }
            return Result.success(new ArrayList<>());
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }
}

package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 装修/装饰项目控制器
 * 数据优先从 engineering_work_order 读取(order_type='decoration')，无表时返空
 */
@RestController
@RequestMapping("/api/decoration")
@CrossOrigin(origins = "*")
public class DecorationController {

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

    /**
     * 装饰项目列表
     * 优先从独立表 decoration_project，其次从 engineering_work_order (装饰类)
     */
    @GetMapping("/projects")
    public Result<List<Map<String, Object>>> listProjects(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 500) limit = 100;

            // 优先从独立表
            try {
                StringBuilder sql = new StringBuilder("SELECT * FROM decoration_project WHERE 1=1");
                List<Object> args = new ArrayList<>();
                if (sid != null) { sql.append(" AND store_id = ?"); args.add(sid); }
                sql.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
                args.add(limit);
                return Result.success(jdbc.queryForList(sql.toString(), args.toArray()));
            } catch (Exception ignored) {}

            // 回退：从 engineering_work_order 取出装饰装修类工单
            StringBuilder sql2 = new StringBuilder(
                "SELECT id AS project_id, store_id, order_no AS project_no, title AS project_name, "
                + "location, order_type AS category, priority, status, "
                + "applicant_name AS manager, description, created_at AS start_date, "
                + "started_at AS active_time, completed_at AS finish_time "
                + "FROM engineering_work_order WHERE order_type IN ('decoration','装修','装饰')");
            List<Object> args2 = new ArrayList<>();
            if (sid != null) { sql2.append(" AND store_id = ?"); args2.add(sid); }
            sql2.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
            args2.add(limit);
            try {
                return Result.success(jdbc.queryForList(sql2.toString(), args2.toArray()));
            } catch (Exception e2) {
                return Result.success(new ArrayList<>());
            }
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 更新装饰项目。生产库没有独立的 decoration_project 表，实际改的是
     * engineering_work_order 里 order_type 为装修/装饰类的那一行（限定条件防止误改其它工单）。
     */
    @PutMapping("/projects/{id}")
    public Result<Void> updateProject(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (body.get("project_name") != null) { sets.add("title = ?"); args.add(body.get("project_name")); }
            if (body.get("location") != null) { sets.add("location = ?"); args.add(body.get("location")); }
            if (body.get("status") != null) { sets.add("status = ?"); args.add(body.get("status")); }
            if (body.get("priority") != null) { sets.add("priority = ?"); args.add(body.get("priority")); }
            if (body.get("description") != null) { sets.add("description = ?"); args.add(body.get("description")); }
            if (sets.isEmpty()) return Result.error(400, "无更新字段");
            args.add(id);
            int updated = jdbc.update(
                    "UPDATE engineering_work_order SET " + String.join(", ", sets) +
                            " WHERE id = ? AND order_type IN ('decoration','装修','装饰')",
                    args.toArray());
            if (updated == 0) return Result.error(404, "装饰项目不存在");
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "更新装饰项目失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/projects/{id}")
    public Result<Void> deleteProject(@PathVariable Long id) {
        try {
            int deleted = jdbc.update(
                    "DELETE FROM engineering_work_order WHERE id = ? AND order_type IN ('decoration','装修','装饰')", id);
            if (deleted == 0) return Result.error(404, "装饰项目不存在");
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "删除装饰项目失败：" + e.getMessage());
        }
    }
}

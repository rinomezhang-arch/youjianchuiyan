package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/gm")
public class GMOfficeController {
    private final JdbcTemplate jdbc;

    public GMOfficeController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Scope scope = scope();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("review", countNotifications(scope, "review"));
        data.put("info", countNotifications(scope, "info"));
        data.put("approval", countApprovals(scope));
        data.put("todo", countNotifications(scope, "todo"));
        return Result.success(data);
    }

    @GetMapping("/review")
    public Result<List<Map<String, Object>>> review() {
        return Result.success(notificationItems(scope(), "review"));
    }

    @GetMapping("/info")
    public Result<List<Map<String, Object>>> info() {
        return Result.success(notificationItems(scope(), "info"));
    }

    @GetMapping("/todo")
    public Result<List<Map<String, Object>>> todo() {
        return Result.success(notificationItems(scope(), "todo"));
    }

    @GetMapping("/approval")
    public Result<List<Map<String, Object>>> approval() {
        Scope scope = scope();
        String sql = "SELECT COALESCE(business_no, flow_no) AS title, '待批' AS tag, " +
                "'danger' AS tagType FROM approval_flow WHERE status = 'pending'" + scope.where +
                " ORDER BY created_time DESC LIMIT 20";
        return Result.success(jdbc.queryForList(sql, scope.args));
    }

    private int countApprovals(Scope scope) {
        String sql = "SELECT COUNT(*) FROM approval_flow WHERE status = 'pending'" + scope.where;
        Integer count = jdbc.queryForObject(sql, Integer.class, scope.args);
        return count == null ? 0 : count;
    }

    private int countNotifications(Scope scope, String category) {
        String sql = "SELECT COUNT(*) FROM sys_notification WHERE status = 'published' AND is_read = 0 " +
                "AND related_type = ?" + scope.where;
        Object[] args = prepend(category, scope.args);
        Integer count = jdbc.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }

    private List<Map<String, Object>> notificationItems(Scope scope, String category) {
        String sql = "SELECT notify_title AS title, " +
                "CASE priority WHEN 'urgent' THEN '紧急' WHEN 'high' THEN '重要' ELSE '待阅' END AS tag, " +
                "CASE priority WHEN 'urgent' THEN 'danger' WHEN 'high' THEN 'warning' ELSE 'info' END AS tagType " +
                "FROM sys_notification WHERE status = 'published' AND is_read = 0 AND related_type = ?" +
                scope.where + " ORDER BY send_time DESC LIMIT 20";
        return jdbc.queryForList(sql, prepend(category, scope.args));
    }

    private Scope scope() {
        Long staffId = UserContext.getStaffId();
        if (staffId == null) throw new SecurityException("未登录，无权访问总经办数据");
        Long storeId = UserContext.getStoreId();
        if (UserContext.isDataScopeAll() || storeId == null || storeId == 0L) {
            return new Scope("", new Object[0]);
        }
        return new Scope(" AND store_id = ?", new Object[]{storeId});
    }

    private Object[] prepend(Object first, Object[] rest) {
        Object[] values = new Object[rest.length + 1];
        values[0] = first;
        System.arraycopy(rest, 0, values, 1, rest.length);
        return values;
    }

    private record Scope(String where, Object[] args) {}
}

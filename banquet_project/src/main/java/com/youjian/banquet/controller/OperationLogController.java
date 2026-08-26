package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.AuditLog;
import com.youjian.banquet.entity.ChangeLog;
import com.youjian.banquet.repository.AuditLogRepository;
import com.youjian.banquet.repository.ChangeLogRepository;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * 操作日志 Controller（变更日志 + 审计日志）
 * 表: change_log / audit_logs
 * 路径:
 *   /api/change-logs   变更日志
 *   /api/audit-logs    审计日志
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class OperationLogController {

    @Autowired private ChangeLogRepository changeLogRepo;
    @Autowired private AuditLogRepository auditLogRepo;
    @Autowired private JdbcTemplate jdbcTemplate;

    private Long resolveQueryStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    // ============ 变更日志 ============

    @GetMapping("/change-logs")
    public Result<List<ChangeLog>> listChangeLogs(@RequestParam(defaultValue = "1") Long storeId,
                                                    @RequestParam(required = false) String operationType,
                                                    @RequestParam(required = false) String targetType) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<ChangeLog> list = changeLogRepo.findByStoreIdOrderByCreatedAtDesc(storeId);
            // 内存过滤（简单实现）
            if (operationType != null && !operationType.isEmpty()) {
                list.removeIf(l -> !operationType.equals(l.getOperationType()));
            }
            if (targetType != null && !targetType.isEmpty()) {
                list.removeIf(l -> !targetType.equals(l.getTargetType()));
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询变更日志失败: " + e.getMessage());
        }
    }

    @GetMapping("/change-logs/{id}")
    public Result<ChangeLog> getChangeLog(@PathVariable Long id) {
        try {
            ChangeLog log = changeLogRepo.findById(id).orElse(null);
            if (log == null) return Result.error(404, "日志不存在");
            return Result.success(log);
        } catch (Exception e) {
            return Result.error(500, "获取日志失败: " + e.getMessage());
        }
    }

    @PostMapping("/change-logs")
    public Result<ChangeLog> createChangeLog(@RequestBody ChangeLog log) {
        try {
            log.setLogId(null);
            return Result.success(changeLogRepo.save(log));
        } catch (Exception e) {
            return Result.error(500, "创建日志失败: " + e.getMessage());
        }
    }

    // ============ 审计日志 ============

    /**
     * 前端别名：AuditLog.vue 调用 /api/audit/logs。
     * <p>
     * 之前直接复用 listAuditLogs 返回裸数组，但前端要的是 {list,total} 分页包裹结构
     * (payload.list ?? payload.rows)，裸数组两边都取不到，页面一直空白。这里改成
     * 真正支持 page/pageSize/keyword/action/startDate/endDate 的分页查询，并把
     * created_at(秒级时间戳) 转成毫秒时间戳给前端 formatTime 直接 new Date() 用。
     */
    @GetMapping("/audit/logs")
    public Result<Map<String, Object>> listAuditLogsAlias(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize) {
        try {
            storeId = resolveQueryStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1 ");
            List<Object> args = new java.util.ArrayList<>();
            if (storeId != null) { where.append(" AND store_id = ? "); args.add(storeId); }
            if (keyword != null && !keyword.isEmpty()) {
                where.append(" AND (user_id LIKE ? OR target LIKE ? OR detail LIKE ?) ");
                String kw = "%" + keyword + "%";
                args.add(kw); args.add(kw); args.add(kw);
            }
            if (action != null && !action.isEmpty()) {
                where.append(" AND action LIKE ? ");
                args.add("%" + action.toUpperCase() + "%");
            }
            if (startDate != null && !startDate.isEmpty()) {
                where.append(" AND created_at >= UNIX_TIMESTAMP(?) ");
                args.add(startDate + " 00:00:00");
            }
            if (endDate != null && !endDate.isEmpty()) {
                where.append(" AND created_at <= UNIX_TIMESTAMP(?) ");
                args.add(endDate + " 23:59:59");
            }

            Long total = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM audit_logs" + where, Long.class, args.toArray());

            if (page < 1) page = 1;
            if (pageSize < 1 || pageSize > 500) pageSize = 50;
            int offset = (page - 1) * pageSize;
            List<Object> pagedArgs = new java.util.ArrayList<>(args);
            pagedArgs.add(pageSize);
            pagedArgs.add(offset);
            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    "SELECT id, user_id AS user, action, target, detail, created_at * 1000 AS time " +
                            "FROM audit_logs" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                    pagedArgs.toArray());

            Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("list", rows);
            payload.put("total", total == null ? 0 : total);
            return Result.success(payload);
        } catch (Exception e) {
            return Result.error(500, "查询审计日志失败: " + e.getMessage());
        }
    }

    /**
     * GET /api/audit/export —— 导出审计日志为 CSV（GBK BOM 避免 Excel 中文乱码）。
     * 复用与 listAuditLogsAlias 相同的筛选条件，但不分页，最多导出 5000 条防止内存暴涨。
     */
    @GetMapping("/audit/export")
    public void exportAuditLogs(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        storeId = resolveQueryStoreId(storeId);
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new java.util.ArrayList<>();
        if (storeId != null) { where.append(" AND store_id = ? "); args.add(storeId); }
        if (keyword != null && !keyword.isEmpty()) {
            where.append(" AND (user_id LIKE ? OR target LIKE ? OR detail LIKE ?) ");
            String kw = "%" + keyword + "%";
            args.add(kw); args.add(kw); args.add(kw);
        }
        if (action != null && !action.isEmpty()) {
            where.append(" AND action LIKE ? ");
            args.add("%" + action.toUpperCase() + "%");
        }
        if (startDate != null && !startDate.isEmpty()) {
            where.append(" AND created_at >= UNIX_TIMESTAMP(?) ");
            args.add(startDate + " 00:00:00");
        }
        if (endDate != null && !endDate.isEmpty()) {
            where.append(" AND created_at <= UNIX_TIMESTAMP(?) ");
            args.add(endDate + " 23:59:59");
        }
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT user_id, action, target, detail, created_at * 1000 AS time FROM audit_logs" +
                        where + " ORDER BY id DESC LIMIT 5000",
                args.toArray());

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=audit_logs.csv");
        response.getOutputStream().write(0xEF); response.getOutputStream().write(0xBB); response.getOutputStream().write(0xBF);
        java.io.PrintWriter writer = response.getWriter();
        writer.println("时间,操作人,操作类型,对象,详情");
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        for (Map<String, Object> row : rows) {
            Object timeMs = row.get("time");
            String timeStr = timeMs == null ? "" : fmt.format(new java.util.Date(((Number) timeMs).longValue()));
            writer.println(csvField(timeStr) + "," + csvField(String.valueOf(row.get("user_id"))) + "," +
                    csvField(String.valueOf(row.get("action"))) + "," + csvField(String.valueOf(row.get("target"))) + "," +
                    csvField(String.valueOf(row.get("detail"))));
        }
        writer.flush();
    }

    private String csvField(String value) {
        if (value == null || "null".equals(value)) value = "";
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    @GetMapping("/audit-logs")
    public Result<List<AuditLog>> listAuditLogs(@RequestParam(defaultValue = "1") Long storeId,
                                                  @RequestParam(required = false) String userId,
                                                  @RequestParam(required = false) String action) {
        try {
            storeId = resolveQueryStoreId(storeId);
            List<AuditLog> list;
            if (userId != null && !userId.isEmpty()) {
                list = auditLogRepo.findByUserId(userId);
            } else if (action != null && !action.isEmpty()) {
                list = auditLogRepo.findByAction(action);
            } else {
                list = auditLogRepo.findByStoreId(storeId);
            }
            return Result.success(list);
        } catch (Exception e) {
            return Result.error(500, "查询审计日志失败: " + e.getMessage());
        }
    }

    @PostMapping("/audit-logs")
    public Result<AuditLog> createAuditLog(@RequestBody AuditLog log) {
        try {
            log.setId(null);
            if (log.getCreatedAt() == null) {
                log.setCreatedAt(Instant.now().getEpochSecond());
            }
            return Result.success(auditLogRepo.save(log));
        } catch (Exception e) {
            return Result.error(500, "创建审计日志失败: " + e.getMessage());
        }
    }
}

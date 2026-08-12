package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 工程维护控制器：报修工单 + 资产盘点。
 * <p>
 * 路由：
 * <ul>
 *   <li>GET  /api/maintenance/requests              报修列表（门店过滤）</li>
 *   <li>POST /api/maintenance/requests              提交报修</li>
 *   <li>PUT  /api/maintenance/requests/{id}/dispatch 派单</li>
 *   <li>PUT  /api/maintenance/requests/{id}/complete 完成维修</li>
 *   <li>GET  /api/maintenance/assets                 资产列表</li>
 *   <li>POST /api/maintenance/assets                 新增资产（仅总经理）</li>
 *   <li>PUT  /api/maintenance/assets/{id}/check      资产盘点</li>
 * </ul>
 * <p>
 * 数据范围：店长仅本店（store_id 强制覆盖）；总经理可访问任意门店（storeId 参数生效）。
 */
@RestController
@RequestMapping("/api/maintenance")
@CrossOrigin(origins = "*")
public class MaintenanceController {

    @Autowired
    private JdbcTemplate jdbc;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ====================== 报修工单 ======================

    /** GET /api/maintenance/requests?storeId=&status=&keyword= */
    @GetMapping("/requests")
    public Result<List<Map<String, Object>>> listRequests(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                    "SELECT id, store_id, request_no, asset_name, location, priority, " +
                    "description, status, handler_id, handler_name, dispatch_note, complete_note, " +
                    "reporter_id, reporter_name, dispatched_at, completed_at, create_time, update_time " +
                    "FROM maintenance_request WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND store_id = ?");
                args.add(effective);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
                args.add(status);
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (request_no LIKE ? OR asset_name LIKE ? OR location LIKE ? OR description LIKE ?)");
                String k = "%" + keyword + "%";
                args.add(k); args.add(k); args.add(k); args.add(k);
            }
            sql.append(" ORDER BY create_time DESC, id DESC");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            for (Map<String, Object> r : rows) {
                formatTimestamp(r, "dispatched_at");
                formatTimestamp(r, "completed_at");
                formatTimestamp(r, "created_at");
                formatTimestamp(r, "updated_at");
            }
            return Result.success(rows);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取报修列表失败: " + e.getMessage());
        }
    }

    /** POST /api/maintenance/requests — 提交报修 */
    @PostMapping("/requests")
    public Result<Map<String, Object>> createRequest(@RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            String assetName = strOrThrow(body.get("assetName"), "assetName");
            String description = strOrThrow(body.get("description"), "description");
            String location = strOr(body.get("location"), null);
            String priority = strOr(body.get("priority"), "medium");
            if (!"high".equals(priority) && !"medium".equals(priority) && !"low".equals(priority)) {
                priority = "medium";
            }

            Long storeId = resolveWriteStoreId();
            if (storeId == null) {
                // 总经理：使用 body.storeId 或默认 1
                storeId = parseLong(body.get("storeId"), 1L);
            }

            String reporterName = UserContext.getUsername();
            Long reporterId = UserContext.getStaffId();
            String requestNo = generateRequestNo();

            String sql = "INSERT INTO maintenance_request " +
                    "(store_id, request_no, asset_name, location, priority, description, status, " +
                    "reporter_id, reporter_name, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, 'pending', ?, ?, NOW(), NOW())";
            jdbc.update(sql, storeId, requestNo, assetName, location, priority, description,
                    reporterId != null ? reporterId.intValue() : null, reporterName);

            Map<String, Object> created = jdbc.queryForMap(
                    "SELECT * FROM maintenance_request WHERE request_no = ?", requestNo);
            formatTimestamp(created, "dispatched_at");
            formatTimestamp(created, "completed_at");
            formatTimestamp(created, "created_at");
            formatTimestamp(created, "updated_at");
            return Result.success(created);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "提交报修失败: " + e.getMessage());
        }
    }

    /** PUT /api/maintenance/requests/{id}/dispatch — 派单 */
    @PutMapping("/requests/{id}/dispatch")
    public Result<Map<String, Object>> dispatch(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            Map<String, Object> row = jdbc.queryForList(
                    "SELECT store_id, status FROM maintenance_request WHERE id = ?", id)
                    .stream().findFirst().orElse(null);
            if (row == null) {
                return Result.error(404, "工单不存在");
            }
            // 校验门店权限：店长仅可派本店工单
            if (!UserContext.isGeneralManager()) {
                Long userStore = UserContext.getCurrentStoreId();
                Object rowStore = row.get("store_id");
                Long s = rowStore == null ? null : ((Number) rowStore).longValue();
                if (userStore == null || !userStore.equals(s)) {
                    return Result.error(403, "无权限：仅可派单本门店工单");
                }
            }
            String handlerName = strOrThrow(body.get("handlerName"), "handlerName");
            String dispatchNote = strOr(body.get("dispatchNote"), null);
            Integer handlerId = parseIntOrNull(body.get("handlerId"));

            jdbc.update(
                    "UPDATE maintenance_request SET status='dispatched', handler_name=?, handler_id=?, " +
                    "dispatch_note=?, dispatched_at=NOW(), update_time=NOW() WHERE id=?",
                    handlerName, handlerId, dispatchNote, id);
            Map<String, Object> updated = jdbc.queryForMap(
                    "SELECT * FROM maintenance_request WHERE id = ?", id);
            formatTimestamp(updated, "dispatched_at");
            formatTimestamp(updated, "completed_at");
            formatTimestamp(updated, "created_at");
            formatTimestamp(updated, "updated_at");
            return Result.success(updated);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "派单失败: " + e.getMessage());
        }
    }

    /** PUT /api/maintenance/requests/{id}/complete — 完成维修 */
    @PutMapping("/requests/{id}/complete")
    public Result<Map<String, Object>> complete(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            Map<String, Object> row = jdbc.queryForList(
                    "SELECT store_id, status FROM maintenance_request WHERE id = ?", id)
                    .stream().findFirst().orElse(null);
            if (row == null) {
                return Result.error(404, "工单不存在");
            }
            if (!UserContext.isGeneralManager()) {
                Long userStore = UserContext.getCurrentStoreId();
                Object rowStore = row.get("store_id");
                Long s = rowStore == null ? null : ((Number) rowStore).longValue();
                if (userStore == null || !userStore.equals(s)) {
                    return Result.error(403, "无权限：仅可完成本门店工单");
                }
            }
            String completeNote = strOr(body.get("completeNote"), null);
            jdbc.update(
                    "UPDATE maintenance_request SET status='done', complete_note=?, " +
                    "completed_at=NOW(), update_time=NOW() WHERE id=?",
                    completeNote, id);
            Map<String, Object> updated = jdbc.queryForMap(
                    "SELECT * FROM maintenance_request WHERE id = ?", id);
            formatTimestamp(updated, "dispatched_at");
            formatTimestamp(updated, "completed_at");
            formatTimestamp(updated, "created_at");
            formatTimestamp(updated, "updated_at");
            return Result.success(updated);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "完成维修失败: " + e.getMessage());
        }
    }

    // ====================== 资产 ======================

    /** GET /api/maintenance/assets?storeId=&category=&status=&keyword= */
    @GetMapping("/assets")
    public Result<List<Map<String, Object>>> listAssets(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                    "SELECT id, store_id, asset_no, asset_name, category, quantity, unit_price, " +
                    "department, location, purchase_date, status, last_check_time, last_check_user, " +
                    "last_check_note, remark, create_time, update_time " +
                    "FROM maintenance_asset WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND store_id = ?");
                args.add(effective);
            }
            if (category != null && !category.isEmpty()) {
                sql.append(" AND category = ?");
                args.add(category);
            }
            if (status != null && !status.isEmpty()) {
                sql.append(" AND status = ?");
                args.add(status);
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (asset_no LIKE ? OR asset_name LIKE ? OR location LIKE ?)");
                String k = "%" + keyword + "%";
                args.add(k); args.add(k); args.add(k);
            }
            sql.append(" ORDER BY create_time DESC, id DESC");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            for (Map<String, Object> r : rows) {
                formatTimestamp(r, "last_check_time");
                formatTimestamp(r, "created_at");
                formatTimestamp(r, "updated_at");
                formatDecimal(r, "unit_price");
                formatDate(r, "purchase_date");
                r.put("totalValue", r.get("unit_price") == null ? BigDecimal.ZERO
                        : ((BigDecimal) r.get("unit_price")).multiply(new BigDecimal(toInt(r.get("quantity"), 1))));
            }
            return Result.success(rows);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取资产列表失败: " + e.getMessage());
        }
    }

    /** POST /api/maintenance/assets — 新增资产（仅总经理） */
    @PostMapping("/assets")
    public Result<Map<String, Object>> createAsset(@RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            UserContext.assertGeneralManager();
            String assetName = strOrThrow(body.get("assetName"), "assetName");
            String category = strOr(body.get("category"), "其他");
            int quantity = parseInt(body.get("quantity"), 1);
            BigDecimal unitPrice = parseDecimal(body.get("unitPrice"), BigDecimal.ZERO);
            String department = strOr(body.get("department"), null);
            String location = strOr(body.get("location"), null);
            String purchaseDate = strOr(body.get("purchaseDate"), null);
            String status = strOr(body.get("status"), "在用");
            String remark = strOr(body.get("remark"), null);
            Long storeId = parseLong(body.get("storeId"), 1L);
            String assetNo = generateAssetNo();

            String sql = "INSERT INTO maintenance_asset " +
                    "(store_id, asset_no, asset_name, category, quantity, unit_price, department, " +
                    "location, purchase_date, status, remark, create_time, update_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())";
            jdbc.update(sql, storeId, assetNo, assetName, category, quantity, unitPrice,
                    department, location, purchaseDate, status, remark);

            Map<String, Object> created = jdbc.queryForMap(
                    "SELECT * FROM maintenance_asset WHERE asset_no = ? AND store_id = ?", assetNo, storeId);
            formatTimestamp(created, "last_check_time");
            formatTimestamp(created, "created_at");
            formatTimestamp(created, "updated_at");
            formatDecimal(created, "unit_price");
            formatDate(created, "purchase_date");
            return Result.success(created);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "新增资产失败: " + e.getMessage());
        }
    }

    /** PUT /api/maintenance/assets/{id}/check — 资产盘点 */
    @PutMapping("/assets/{id}/check")
    public Result<Map<String, Object>> checkAsset(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            Map<String, Object> row = jdbc.queryForList(
                    "SELECT store_id FROM maintenance_asset WHERE id = ?", id)
                    .stream().findFirst().orElse(null);
            if (row == null) {
                return Result.error(404, "资产不存在");
            }
            if (!UserContext.isGeneralManager()) {
                Long userStore = UserContext.getCurrentStoreId();
                Object rowStore = row.get("store_id");
                Long s = rowStore == null ? null : ((Number) rowStore).longValue();
                if (userStore == null || !userStore.equals(s)) {
                    return Result.error(403, "无权限：仅可盘点本门店资产");
                }
            }
            String checkNote = strOr(body.get("checkNote"), null);
            String newStatus = strOr(body.get("status"), null);
            String checkUser = UserContext.getUsername();

            if (newStatus != null && !newStatus.isEmpty()) {
                jdbc.update(
                        "UPDATE maintenance_asset SET last_check_time=NOW(), last_check_user=?, " +
                        "last_check_note=?, status=?, update_time=NOW() WHERE id=?",
                        checkUser, checkNote, newStatus, id);
            } else {
                jdbc.update(
                        "UPDATE maintenance_asset SET last_check_time=NOW(), last_check_user=?, " +
                        "last_check_note=?, update_time=NOW() WHERE id=?",
                        checkUser, checkNote, id);
            }
            Map<String, Object> updated = jdbc.queryForMap(
                    "SELECT * FROM maintenance_asset WHERE id = ?", id);
            formatTimestamp(updated, "last_check_time");
            formatTimestamp(updated, "created_at");
            formatTimestamp(updated, "updated_at");
            formatDecimal(updated, "unit_price");
            formatDate(updated, "purchase_date");
            return Result.success(updated);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "资产盘点失败: " + e.getMessage());
        }
    }

    // ====================== 辅助方法 ======================

    /**
     * 查询接口：解析有效门店ID。
     * @return null=全局（总经理，可查所有门店）；非null=限制到指定门店（店长仅本店）
     */
    private Long resolveQueryStoreId(String requestStoreId) {
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权访问维护数据");
        }
        if (UserContext.isGeneralManager()) {
            // 总经理：storeId 参数可选
            if (requestStoreId == null || requestStoreId.isEmpty() || "0".equals(requestStoreId)) {
                return null;
            }
            return parseLong(requestStoreId, null);
        }
        // 店长：仅本店，忽略前端传入的 storeId
        Long userStore = UserContext.getCurrentStoreId();
        if (userStore == null || userStore == 0L) {
            return null;
        }
        return userStore;
    }

    /**
     * 写操作：解析当前用户门店ID（店长仅本店）。
     * @return 店长门店ID；null=总经理（允许跨门店操作，需 body.storeId 兜底）
     */
    private Long resolveWriteStoreId() {
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权操作维护数据");
        }
        if (UserContext.isGeneralManager()) {
            return null;
        }
        Long userStore = UserContext.getCurrentStoreId();
        if (userStore == null) {
            throw new SecurityException("无门店信息，无权操作");
        }
        return userStore;
    }

    private String generateRequestNo() {
        // MR + yyyyMMdd + 4位随机数
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        String no = "MR" + date + rand;
        // 唯一性兜底
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM maintenance_request WHERE request_no = ?", Integer.class, no);
        if (cnt != null && cnt > 0) {
            no = "MR" + date + ThreadLocalRandom.current().nextInt(10000, 99999);
        }
        return no;
    }

    private String generateAssetNo() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        int rand = ThreadLocalRandom.current().nextInt(1000, 9999);
        String no = "AS" + date + rand;
        Integer cnt = jdbc.queryForObject(
                "SELECT COUNT(*) FROM maintenance_asset WHERE asset_no = ?", Integer.class, no);
        if (cnt != null && cnt > 0) {
            no = "AS" + date + ThreadLocalRandom.current().nextInt(10000, 99999);
        }
        return no;
    }

    private static String strOrThrow(Object obj, String fieldName) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            throw new IllegalArgumentException("缺少必填字段: " + fieldName);
        }
        return obj.toString().trim();
    }

    private static String strOr(Object obj, String def) {
        if (obj == null || obj.toString().trim().isEmpty()) {
            return def;
        }
        return obj.toString().trim();
    }

    private static int parseInt(Object obj, int def) {
        if (obj == null) return def;
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return def; }
    }

    private static Integer parseIntOrNull(Object obj) {
        if (obj == null) return null;
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return null; }
    }

    private static int toInt(Object obj, int def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return def; }
    }

    private static Long parseLong(Object obj, Long def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return def; }
    }

    private static BigDecimal parseDecimal(Object obj, BigDecimal def) {
        if (obj == null) return def;
        try { return new BigDecimal(obj.toString()); } catch (Exception e) { return def; }
    }

    /** 把 java.sql.Timestamp/LocalDateTime 转为字符串，方便序列化 */
    private static void formatTimestamp(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return;
        if (v instanceof java.sql.Timestamp ts) {
            r.put(key, ts.toLocalDateTime().format(TS_FMT));
        } else if (v instanceof LocalDateTime ldt) {
            r.put(key, ldt.format(TS_FMT));
        }
    }

    private static void formatDecimal(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return;
        if (v instanceof BigDecimal bd) {
            r.put(key, bd);
        }
    }

    private static void formatDate(Map<String, Object> r, String key) {
        Object v = r.get(key);
        if (v == null) return;
        if (v instanceof java.sql.Date d) {
            r.put(key, d.toLocalDate().toString());
        }
    }
}

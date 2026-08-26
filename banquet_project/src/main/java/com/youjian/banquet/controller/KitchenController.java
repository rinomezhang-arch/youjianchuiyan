package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.dto.InventoryDTO;
import com.youjian.banquet.service.InventoryService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 后厨看板 + 日志控制器。
 * <p>
 * 路由：
 * <ul>
 *   <li>GET  /api/kitchen/logs           后厨日志列表</li>
 *   <li>POST /api/kitchen/logs           创建后厨日志</li>
 *   <li>GET  /api/kitchen/orders         看板：待做/在做订单（Kitchen.vue）</li>
 *   <li>PUT  /api/kitchen/orders/{id}/status  更新某道菜的出品状态</li>
 *   <li>GET  /api/kitchen/stats          看板统计卡片</li>
 *   <li>GET  /api/kitchen/alerts         损耗/低库存预警（复用 InventoryService 低库存逻辑）</li>
 *   <li>GET  /api/kitchen/staffs         后厨排班（按 staff_master.department='后厨部' 过滤）</li>
 * </ul>
 * <p>
 * 数据范围：店长仅本店；总经理可访问任意门店。
 */
@RestController
@RequestMapping("/api/kitchen")
@CrossOrigin(origins = "*")
public class KitchenController {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private InventoryService inventoryService;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ====================== 看板：订单队列 ======================

    /** GET /api/kitchen/orders?storeId=&station= —— station 目前没有真实数据来源，先占位不过滤 */
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> listOrders(@RequestParam(required = false) String storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                    "SELECT d.dish_booking_id AS id, d.dish_id, d.dish_name, d.dish_quantity, " +
                    "d.kitchen_status AS status, d.created_at, bt.table_number AS tableNo " +
                    "FROM booking_dish_detail d " +
                    "LEFT JOIN booking_table bt ON bt.booking_id = d.booking_id AND bt.store_id = d.store_id " +
                    "WHERE d.kitchen_status IN ('pending','submitted','urgent') ");
            List<Object> args = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND d.store_id = ?");
                args.add(effective);
            }
            sql.append(" ORDER BY FIELD(d.kitchen_status,'urgent','submitted','pending'), d.created_at ASC LIMIT 200");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());

            // 按桌台聚合成前端要的 {id, priority, table, time, dishes:[...], status, statusText} 卡片
            Map<Object, Map<String, Object>> byTable = new java.util.LinkedHashMap<>();
            for (Map<String, Object> r : rows) {
                Object tableNo = r.get("tableNo") != null ? r.get("tableNo") : "散台";
                Map<String, Object> card = byTable.computeIfAbsent(tableNo, k -> {
                    Map<String, Object> c = new HashMap<>();
                    c.put("id", r.get("id"));
                    c.put("table", tableNo);
                    c.put("time", r.get("created_at") != null ? r.get("created_at").toString() : "");
                    c.put("dishes", new ArrayList<String>());
                    c.put("status", r.get("status"));
                    c.put("priority", "urgent".equals(r.get("status")) ? "urgent" : "normal");
                    c.put("statusText", statusText((String) r.get("status")));
                    return c;
                });
                @SuppressWarnings("unchecked")
                List<String> dishes = (List<String>) card.get("dishes");
                dishes.add(r.get("dish_name") + " x" + r.get("dish_quantity"));
                if ("urgent".equals(r.get("status"))) {
                    card.put("priority", "urgent");
                    card.put("status", "urgent");
                    card.put("statusText", statusText("urgent"));
                }
            }
            return Result.success(new ArrayList<>(byTable.values()));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取厨房订单失败: " + e.getMessage());
        }
    }

    private static String statusText(String status) {
        if (status == null) return "待处理";
        switch (status) {
            case "pending": return "待做";
            case "submitted": return "已提交";
            case "urgent": return "加急";
            default: return status;
        }
    }

    /** PUT /api/kitchen/orders/{id}/status —— id 对应 booking_dish_detail.dish_booking_id */
    @PutMapping("/orders/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            String status = body.get("status") != null ? body.get("status").toString() : null;
            if (status == null || status.isBlank()) return Result.error(400, "缺少 status");
            int updated = jdbc.update("UPDATE booking_dish_detail SET kitchen_status = ? WHERE dish_booking_id = ?", status, id);
            if (updated == 0) return Result.error(404, "订单菜品不存在");
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "更新出品状态失败: " + e.getMessage());
        }
    }

    /** GET /api/kitchen/stats */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) String storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            String tenantClause = effective != null ? " AND store_id = ?" : "";
            Object[] tenantArgs = effective != null ? new Object[]{effective} : new Object[]{};

            Integer pending = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM booking_dish_detail WHERE kitchen_status IN ('pending','submitted')" + tenantClause,
                    Integer.class, tenantArgs);
            Integer urgent = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM booking_dish_detail WHERE kitchen_status = 'urgent'" + tenantClause,
                    Integer.class, tenantArgs);

            List<Object> todayArgs = new ArrayList<>(java.util.Arrays.asList(tenantArgs));
            todayArgs.add(0, java.sql.Date.valueOf(LocalDate.now()));
            String todaySql = "SELECT COUNT(*) FROM booking_dish_detail d JOIN booking_master b " +
                    "ON b.booking_id = d.booking_id AND b.store_id = d.store_id " +
                    "WHERE b.booking_date = ?" + tenantClause.replace("store_id", "d.store_id");
            Integer todayTotal = jdbc.queryForObject(todaySql, Integer.class, todayArgs.toArray());

            Integer refunded = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM booking_dish_detail WHERE kitchen_status = 'refunded'" + tenantClause,
                    Integer.class, tenantArgs);

            Map<String, Object> data = new HashMap<>();
            data.put("pendingOrders", pending == null ? 0 : pending);
            data.put("timeoutAlerts", urgent == null ? 0 : urgent);
            data.put("todayTotal", todayTotal == null ? 0 : todayTotal);
            data.put("todayTrend", "");
            int total = (todayTotal == null ? 0 : todayTotal);
            int ref = (refunded == null ? 0 : refunded);
            data.put("returnRate", total > 0 ? String.format("%.1f%%", ref * 100.0 / total) : "-");
            data.put("returnRateNote", "");
            return Result.success(data);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取厨房统计失败: " + e.getMessage());
        }
    }

    /** GET /api/kitchen/alerts —— 复用库存低量预警（InventoryService），映射成看板需要的 {type,title,meta} */
    @GetMapping("/alerts")
    public Result<List<Map<String, Object>>> alerts(@RequestParam(required = false) String storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            String sid = effective != null ? String.valueOf(effective) : "1";
            List<InventoryDTO> low = inventoryService.getLowStockAlerts(sid);
            List<Map<String, Object>> data = new ArrayList<>();
            for (InventoryDTO dto : low) {
                Map<String, Object> item = new HashMap<>();
                item.put("type", "warning");
                item.put("title", dto.getIngredientName() + " 库存偏低");
                item.put("meta", dto.getNotes());
                data.add(item);
            }
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "获取损耗预警失败: " + e.getMessage());
        }
    }

    /** GET /api/kitchen/staffs —— 后厨排班，按部门='后厨部' 过滤员工，无实时打卡数据先统一标"在岗" */
    @GetMapping("/staffs")
    public Result<List<Map<String, Object>>> staffs(@RequestParam(required = false) String storeId) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                    "SELECT staff_name, staff_position FROM staff_master " +
                    "WHERE department = '后厨部' AND employment_status IN ('active','在职')");
            List<Object> args = new ArrayList<>();
            if (effective != null) { sql.append(" AND store_id = ?"); args.add(effective); }
            sql.append(" ORDER BY staff_name");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            List<Map<String, Object>> data = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                Map<String, Object> item = new HashMap<>();
                item.put("name", r.get("staff_name"));
                item.put("role", r.get("staff_position"));
                item.put("status", "on-duty");
                item.put("time", "");
                data.add(item);
            }
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "获取后厨排班失败: " + e.getMessage());
        }
    }

    // ====================== 后厨日志 ======================

    /** GET /api/kitchen/logs?storeId=&action=&keyword= */
    @GetMapping("/logs")
    public Result<List<Map<String, Object>>> listLogs(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String keyword) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                    "SELECT id, store_id, action, target_type, booking_id, dish_id, dish_name, " +
                    "operator_id, operator_name, note, created_at " +
                    "FROM kitchen_log WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND store_id = ?");
                args.add(effective);
            }
            if (action != null && !action.isEmpty()) {
                sql.append(" AND action = ?");
                args.add(action);
            }
            if (keyword != null && !keyword.isEmpty()) {
                sql.append(" AND (booking_id LIKE ? OR dish_name LIKE ? OR note LIKE ? OR operator_name LIKE ?)");
                String k = "%" + keyword + "%";
                args.add(k); args.add(k); args.add(k); args.add(k);
            }
            sql.append(" ORDER BY create_time DESC, id DESC");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            for (Map<String, Object> r : rows) {
                formatTimestamp(r, "created_at");
            }
            return Result.success(rows);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取后厨日志失败: " + e.getMessage());
        }
    }

    /** POST /api/kitchen/logs — 创建后厨日志 */
    @PostMapping("/logs")
    public Result<Map<String, Object>> createLog(@RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            String action = strOrThrow(body.get("action"), "action");
            String targetType = strOr(body.get("targetType"), "dish");
            String bookingId = strOr(body.get("bookingId"), null);
            String dishId = strOr(body.get("dishId"), null);
            String dishName = strOr(body.get("dishName"), null);
            String note = strOr(body.get("note"), null);

            Long storeId = resolveWriteStoreId();
            if (storeId == null) {
                storeId = parseLong(body.get("storeId"), 1L);
            }

            String operatorName = UserContext.getUsername();
            Long operatorId = UserContext.getStaffId();
            Integer opId = operatorId != null ? operatorId.intValue() : null;

            String sql = "INSERT INTO kitchen_log " +
                    "(store_id, action, target_type, booking_id, dish_id, dish_name, " +
                    "operator_id, operator_name, note) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
            jdbc.update(sql, storeId, action, targetType, bookingId, dishId, dishName,
                    opId, operatorName, note);

            Map<String, Object> created = jdbc.queryForMap(
                    "SELECT * FROM kitchen_log WHERE store_id = ? AND operator_name = ? " +
                    "ORDER BY id DESC LIMIT 1", storeId,
                    operatorName != null ? operatorName : "");
            formatTimestamp(created, "created_at");
            return Result.success(created);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建后厨日志失败: " + e.getMessage());
        }
    }

    // ====================== 能耗记录 ======================

    // 注：此前这里还有一套 /energy 接口，和真正在用的 EnergyController（/api/energy/*）
    // 是重复实现，且两边对 energy_record 表的列设计互相矛盾，该表在生产库里根本不存在。
    // 排查确认前端没有任何页面调用 /api/kitchen/energy，属于死代码，已删除，能耗功能
    // 统一走 EnergyController。

    // ====================== 辅助方法 ======================

    /**
     * 查询接口：解析有效门店ID。
     * @return null=全局（总经理，可查所有门店）；非null=限制到指定门店（店长仅本店）
     */
    private Long resolveQueryStoreId(String requestStoreId) {
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权访问后厨数据");
        }
        if (UserContext.isGeneralManager()) {
            if (requestStoreId == null || requestStoreId.isEmpty() || "0".equals(requestStoreId)) {
                return null;
            }
            return parseLong(requestStoreId, null);
        }
        Long userStore = UserContext.getCurrentStoreId();
        if (userStore == null || userStore == 0L) {
            return null;
        }
        return userStore;
    }

    private Long resolveWriteStoreId() {
        Long currentStaffId = UserContext.getStaffId();
        if (currentStaffId == null) {
            throw new SecurityException("未登录，无权操作后厨数据");
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

    private static String defaultUnit(String energyType) {
        switch (energyType) {
            case "electric": return "kWh";
            case "water": return "吨";
            case "gas": return "m³";
            default: return null;
        }
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

    private static Long parseLong(Object obj, Long def) {
        if (obj == null) return def;
        if (obj instanceof Number) return ((Number) obj).longValue();
        try { return Long.parseLong(obj.toString()); } catch (Exception e) { return def; }
    }

    private static BigDecimal parseDecimal(Object obj, BigDecimal def) {
        if (obj == null) return def;
        try { return new BigDecimal(obj.toString()); } catch (Exception e) { return def; }
    }

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

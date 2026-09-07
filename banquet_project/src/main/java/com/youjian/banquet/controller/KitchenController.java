package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.dto.InventoryDTO;
import com.youjian.banquet.service.InventoryService;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
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

    /** One row per dish_booking_id; table labels must never collapse actionable dishes. */
    @GetMapping("/orders")
    public Result<List<Map<String, Object>>> listOrders(@RequestParam(required = false) String storeId) {
        try {
            Long store;
            Long operationStore = null;
            if (UserContext.isGeneralManager()) {
                // Existing GET data-scope permits a selected store or all stores; it does not change operator identity.
                if (storeId != null && !storeId.isEmpty() && !"0".equals(storeId)
                        && (parseLong(storeId, null) == null || parseLong(storeId, null) <= 0)) {
                    return Result.error(400, "门店编号无效");
                }
                store = resolveQueryStoreId(storeId);
                try { operationStore = requireKitchenStore(); }
                catch (SecurityException ignored) { /* Cross-store/global GM remains read-only. */ }
            } else {
                store = requireKitchenStore();
                operationStore = store;
                if (storeId != null && !storeId.isBlank() && !store.toString().equals(storeId)) {
                    throw new SecurityException("只能查看当前门店的厨房菜品");
                }
            }
            List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT d.dish_booking_id AS id, d.store_id, d.booking_id, d.dish_id, d.dish_name,
                       d.dish_quantity, d.kitchen_status, d.created_at,
                       (SELECT GROUP_CONCAT(DISTINCT bt.table_number ORDER BY bt.table_number SEPARATOR ', ')
                        FROM booking_table bt WHERE bt.booking_id=d.booking_id AND bt.store_id=d.store_id) AS table_no
                FROM booking_dish_detail d
                JOIN booking_master b ON b.booking_id=d.booking_id AND b.store_id=d.store_id
                WHERE (%s) AND d.kitchen_status IN ('submitted','preparing','urgent')
                  AND b.booking_status NOT IN ('cancelled','completed')
                  AND COALESCE(b.payment_status,'unpaid') <> 'paid'
                ORDER BY FIELD(d.kitchen_status,'urgent','preparing','submitted'), d.created_at, d.dish_booking_id
                LIMIT 200
                """.formatted(store == null ? "1=1" : "d.store_id=?"), store == null ? new Object[0] : new Object[]{store});
            List<Map<String, Object>> data = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                Map<String, Object> item = new HashMap<>();
                String status = row.get("kitchen_status").toString();
                item.put("id", row.get("id"));
                Long rowStore = ((Number) row.get("store_id")).longValue();
                item.put("storeId", rowStore);
                item.put("canOperate", rowStore.equals(operationStore));
                item.put("bookingId", row.get("booking_id"));
                item.put("dishId", row.get("dish_id"));
                item.put("dishName", row.get("dish_name"));
                item.put("quantity", row.get("dish_quantity"));
                item.put("table", row.get("table_no") == null ? "散台" : row.get("table_no"));
                item.put("time", row.get("created_at") == null ? "" : row.get("created_at").toString());
                item.put("status", status);
                item.put("statusText", statusText(status));
                item.put("priority", "urgent".equals(status) ? "urgent" : "normal");
                data.add(item);
            }
            return Result.success(data);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取厨房菜品失败，请稍后重试");
        }
    }

    private static String statusText(String status) {
        if (status == null) return "未知状态";
        return switch (status) {
            case "submitted" -> "待制作";
            case "preparing" -> "制作中";
            case "served" -> "已出品";
            case "urgent" -> "催菜待确认";
            default -> status;
        };
    }

    private Long requireKitchenStore() {
        Long staff = UserContext.getStaffId();
        Long store = UserContext.getCurrentStoreId();
        if (staff == null || staff <= 0 || staff > Integer.MAX_VALUE || store == null || store <= 0) {
            throw new SecurityException("请使用已绑定门店的员工身份操作厨房");
        }
        Integer valid = jdbc.queryForObject("""
            SELECT COUNT(*) FROM staff_master WHERE staff_id=? AND store_id=?
            AND employment_status IN ('active','在职')
            """, Integer.class, staff, store);
        if (valid == null || valid != 1) throw new SecurityException("员工不属于当前门店或已离职");
        return store;
    }

    /** Explicit transitions, serialized with cancellation/payment through the booking row lock. */
    @PutMapping("/orders/{id}/status")
    @Transactional
    public Result<Void> updateOrderStatus(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long store = requireKitchenStore();
            if (id == null || id <= 0) return Result.error(400, "菜品明细编号无效");
            String next = body.get("status") instanceof String value ? value : "";
            if (!List.of("preparing", "served").contains(next)) return Result.error(400, "只允许开始制作或确认出品");
            List<String> parents = jdbc.queryForList(
                "SELECT booking_id FROM booking_dish_detail WHERE dish_booking_id=? AND store_id=?",
                String.class, id, store);
            if (parents.isEmpty()) return Result.error(404, "当前门店没有该菜品明细");
            String bookingId = parents.get(0);
            List<Map<String, Object>> bookings = jdbc.queryForList(
                "SELECT booking_status,payment_status FROM booking_master WHERE booking_id=? AND store_id=? FOR UPDATE",
                bookingId, store);
            if (bookings.isEmpty()) return Result.error(409, "菜品缺少有效的预订单，不能出品");
            Map<String, Object> booking = bookings.get(0);
            if (List.of("cancelled", "completed").contains(java.util.Objects.toString(booking.get("booking_status"), ""))
                    || "paid".equals(booking.get("payment_status"))) {
                return Result.error(409, "订单已取消或已结账，不能操作出品");
            }
            List<Map<String, Object>> details = jdbc.queryForList("""
                SELECT dish_id,dish_name,kitchen_status FROM booking_dish_detail
                WHERE dish_booking_id=? AND store_id=? AND booking_id=? FOR UPDATE
                """, id, store, bookingId);
            if (details.isEmpty()) return Result.error(404, "菜品明细已变化，请刷新后重试");
            Map<String, Object> detail = details.get(0);
            String previous = java.util.Objects.toString(detail.get("kitchen_status"), "");
            if (previous.equals(next)) return Result.success(null);
            // Existing urgent overwrites the old stage, so re-confirm preparing; never skip straight to served.
            boolean allowed = ("preparing".equals(next) && List.of("submitted", "urgent").contains(previous))
                || ("served".equals(next) && "preparing".equals(previous));
            if (!allowed) return Result.error(409, "当前状态不允许此操作，请刷新菜品后重试");
            String timestampColumn = "preparing".equals(next) ? "kitchen_started_at" : "kitchen_done_at";
            int changed = jdbc.update("UPDATE booking_dish_detail SET kitchen_status=?, " + timestampColumn
                + "=? WHERE dish_booking_id=? AND store_id=? AND kitchen_status=?",
                next, System.currentTimeMillis(), id, store, previous);
            if (changed != 1) throw new IllegalStateException("出品状态更新数量异常");
            String note = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of(
                "dishBookingId", id, "from", previous, "to", next));
            jdbc.update("""
                INSERT INTO kitchen_log(store_id,action,target_type,booking_id,dish_id,dish_name,
                    operator_id,operator_name,note,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)
                """, store, next, "booking_dish_detail", bookingId, detail.get("dish_id"), detail.get("dish_name"),
                UserContext.getStaffId().intValue(), UserContext.getUsername(), note, LocalDateTime.now());
            return Result.success(null);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return Result.error(500, "出品保存失败，状态和日志已撤回，请刷新后重试");
        }
    }

    /** GET /api/kitchen/stats */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats(@RequestParam(required = false) String storeId) {
        try {
            if (UserContext.isGeneralManager() && storeId != null && !storeId.isEmpty() && !"0".equals(storeId)
                    && (parseLong(storeId, null) == null || parseLong(storeId, null) <= 0)) {
                return Result.error(400, "门店编号无效");
            }
            Long effective = UserContext.isGeneralManager() ? resolveQueryStoreId(storeId) : requireKitchenStore();
            LocalDate period = LocalDate.now();
            // One snapshot, one meal-date population, one row per dish_booking_id (not order or quantity).
            Map<String, Object> counts = jdbc.queryForMap("""
                SELECT COUNT(*) AS total_details,
                       COALESCE(SUM(CASE WHEN d.kitchen_status='refunded' THEN 1 ELSE 0 END),0) AS refunded_details,
                       COALESCE(SUM(CASE WHEN d.kitchen_status IN ('submitted','preparing','urgent')
                           AND b.booking_status <> 'completed' AND COALESCE(b.payment_status,'unpaid') <> 'paid'
                           THEN 1 ELSE 0 END),0) AS pending_details,
                       COALESCE(SUM(CASE WHEN d.kitchen_status='urgent'
                           AND b.booking_status <> 'completed' AND COALESCE(b.payment_status,'unpaid') <> 'paid'
                           THEN 1 ELSE 0 END),0) AS urgent_details
                FROM booking_dish_detail d
                JOIN booking_master b ON b.booking_id=d.booking_id AND b.store_id=d.store_id
                WHERE b.booking_date=? AND b.booking_status <> 'cancelled'
                  AND COALESCE(d.kitchen_status,'pending') <> 'cancelled' AND (%s)
                """.formatted(effective == null ? "1=1" : "d.store_id=?"),
                effective == null ? new Object[]{java.sql.Date.valueOf(period)} : new Object[]{java.sql.Date.valueOf(period),effective});
            long total = ((Number) counts.get("total_details")).longValue();
            long refunded = ((Number) counts.get("refunded_details")).longValue();
            long pending = ((Number) counts.get("pending_details")).longValue();
            long urgent = ((Number) counts.get("urgent_details")).longValue();
            Map<String, Object> data = new HashMap<>();
            data.put("periodDate", period.toString());
            data.put("periodBasis", "booking_date");
            data.put("grain", "dish_booking_id");
            data.put("pendingDetails", pending);
            data.put("urgentDetails", urgent);
            data.put("totalDetails", total);
            data.put("refundedDetails", refunded);
            data.put("returnRate", total > 0 ? String.format(java.util.Locale.ROOT, "%.1f%%", refunded * 100.0 / total) : "-");
            // Compatibility keys retain the same detail counts, never order counts or measured timeouts.
            data.put("pendingOrders", pending);
            data.put("timeoutAlerts", urgent);
            data.put("todayTotal", total);
            data.put("todayTrend", "按今日用餐单，每条菜明细计1条");
            data.put("returnRateNote", "今日用餐单退菜明细 / 今日用餐单菜明细（排除已取消项）");
            return Result.success(data);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取厨房统计失败，请稍后重试");
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
            sql.append(" ORDER BY created_at DESC, id DESC");
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

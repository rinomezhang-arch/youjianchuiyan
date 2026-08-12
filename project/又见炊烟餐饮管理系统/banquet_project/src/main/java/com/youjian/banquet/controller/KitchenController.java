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

/**
 * 后厨日志 + 能耗记录控制器。
 * <p>
 * 路由：
 * <ul>
 *   <li>GET  /api/kitchen/logs    后厨日志列表</li>
 *   <li>POST /api/kitchen/logs    创建后厨日志</li>
 *   <li>GET  /api/kitchen/energy  能耗记录</li>
 *   <li>POST /api/kitchen/energy  记录能耗</li>
 * </ul>
 * <p>
 * 数据范围：店长仅本店；总经理可访问任意门店。
 * 后厨日志写入 kitchen_log 表；能耗写入 energy_record 表。
 */
@RestController
@RequestMapping("/api/kitchen")
@CrossOrigin(origins = "*")
public class KitchenController {

    @Autowired
    private JdbcTemplate jdbc;

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                    "operator_id, operator_name, note, create_time " +
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
                formatTimestamp(r, "create_time");
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
                    "operator_id, operator_name, note, create_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            jdbc.update(sql, storeId, action, targetType, bookingId, dishId, dishName,
                    opId, operatorName, note);

            Map<String, Object> created = jdbc.queryForMap(
                    "SELECT * FROM kitchen_log WHERE store_id = ? AND operator_name = ? " +
                    "ORDER BY id DESC LIMIT 1", storeId,
                    operatorName != null ? operatorName : "");
            formatTimestamp(created, "create_time");
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

    /** GET /api/kitchen/energy?storeId=&energyType=&start=&end= */
    @GetMapping("/energy")
    public Result<List<Map<String, Object>>> listEnergy(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String energyType,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end) {
        try {
            Long effective = resolveQueryStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                    "SELECT id, store_id, record_date, energy_type, meter_reading, consumption, " +
                    "unit, unit_price, cost, recorder_id, recorder_name, note, create_time " +
                    "FROM energy_record WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (effective != null) {
                sql.append(" AND store_id = ?");
                args.add(effective);
            }
            if (energyType != null && !energyType.isEmpty()) {
                sql.append(" AND energy_type = ?");
                args.add(energyType);
            }
            if (start != null && !start.isEmpty()) {
                sql.append(" AND record_date >= ?");
                args.add(java.sql.Date.valueOf(start));
            }
            if (end != null && !end.isEmpty()) {
                sql.append(" AND record_date <= ?");
                args.add(java.sql.Date.valueOf(end));
            }
            sql.append(" ORDER BY record_date DESC, id DESC");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), args.toArray());
            for (Map<String, Object> r : rows) {
                formatTimestamp(r, "create_time");
                formatDate(r, "record_date");
                formatDecimal(r, "meter_reading");
                formatDecimal(r, "consumption");
                formatDecimal(r, "unit_price");
                formatDecimal(r, "cost");
            }
            return Result.success(rows);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取能耗记录失败: " + e.getMessage());
        }
    }

    /** POST /api/kitchen/energy — 记录能耗 */
    @PostMapping("/energy")
    public Result<Map<String, Object>> createEnergy(@RequestBody Map<String, Object> body) {
        try {
            UserContext.ensureDataScopeFromStoreId();
            String recordDate = strOrThrow(body.get("recordDate"), "recordDate");
            String energyType = strOrThrow(body.get("energyType"), "energyType");
            if (!"electric".equals(energyType) && !"water".equals(energyType) && !"gas".equals(energyType)) {
                throw new IllegalArgumentException("energyType 取值非法，应为 electric/water/gas");
            }
            BigDecimal meterReading = parseDecimal(body.get("meterReading"), null);
            BigDecimal consumption = parseDecimal(body.get("consumption"), null);
            String unit = strOr(body.get("unit"), defaultUnit(energyType));
            BigDecimal unitPrice = parseDecimal(body.get("unitPrice"), null);
            BigDecimal cost = parseDecimal(body.get("cost"), null);
            // 自动计算费用（如有用量+单价）
            if (cost == null && consumption != null && unitPrice != null) {
                cost = consumption.multiply(unitPrice);
            }
            String note = strOr(body.get("note"), null);

            Long storeId = resolveWriteStoreId();
            if (storeId == null) {
                storeId = parseLong(body.get("storeId"), 1L);
            }
            String recorderName = UserContext.getUsername();
            Long recorderId = UserContext.getStaffId();
            Integer recId = recorderId != null ? recorderId.intValue() : null;

            // 同门店同日同类型唯一约束：先删后插或更新。这里采用 INSERT ... ON DUPLICATE KEY UPDATE
            String sql = "INSERT INTO energy_record " +
                    "(store_id, record_date, energy_type, meter_reading, consumption, unit, unit_price, " +
                    "cost, recorder_id, recorder_name, note, create_time) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW()) " +
                    "ON DUPLICATE KEY UPDATE " +
                    "meter_reading=VALUES(meter_reading), consumption=VALUES(consumption), unit=VALUES(unit), " +
                    "unit_price=VALUES(unit_price), cost=VALUES(cost), recorder_id=VALUES(recorder_id), " +
                    "recorder_name=VALUES(recorder_name), note=VALUES(note)";
            jdbc.update(sql, storeId, java.sql.Date.valueOf(recordDate), energyType, meterReading,
                    consumption, unit, unitPrice, cost, recId, recorderName, note);

            Map<String, Object> created = jdbc.queryForMap(
                    "SELECT * FROM energy_record WHERE store_id = ? AND record_date = ? AND energy_type = ?",
                    storeId, java.sql.Date.valueOf(recordDate), energyType);
            formatTimestamp(created, "create_time");
            formatDate(created, "record_date");
            formatDecimal(created, "meter_reading");
            formatDecimal(created, "consumption");
            formatDecimal(created, "unit_price");
            formatDecimal(created, "cost");
            return Result.success(created);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "记录能耗失败: " + e.getMessage());
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

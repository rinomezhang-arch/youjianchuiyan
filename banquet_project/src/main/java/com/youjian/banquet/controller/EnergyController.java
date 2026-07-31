package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

/**
 * 能耗管理 controller。
 * 数据源 energy_record(已存在表);后端无数据时返空,前端展示"未录入"+ 引导录入。
 * 所有接口支持 storeId 隔离(店长强制本人门店,总经理可指定)。
 */
@RestController
@RequestMapping("/api/energy")
@CrossOrigin(origins = "*")
public class EnergyController {

    @Autowired
    private JdbcTemplate jdbc;

    private Long resolveStoreId(Long requestStoreId) {
        Long sid = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && sid != null) return sid;
        return requestStoreId == null ? 1L : requestStoreId;
    }

    /** 列表:支持按日期范围/类型筛选 */
    @GetMapping("/records")
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) String type,
                                                    @RequestParam(required = false) String startDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(defaultValue = "1") Long storeId) {
        try {
            storeId = resolveStoreId(storeId);
            StringBuilder sql = new StringBuilder("SELECT id, store_id, record_date, energy_type, meter_reading, daily_usage, daily_cost, recorder, remark, created_at FROM energy_record WHERE store_id = ?");
            List<Object> args = new ArrayList<>();
            args.add(storeId);
            if (type != null && !type.isEmpty()) { sql.append(" AND energy_type = ?"); args.add(type); }
            if (startDate != null && !startDate.isEmpty()) { sql.append(" AND record_date >= ?"); args.add(LocalDate.parse(startDate)); }
            if (endDate != null && !endDate.isEmpty()) { sql.append(" AND record_date <= ?"); args.add(LocalDate.parse(endDate)); }
            sql.append(" ORDER BY record_date DESC LIMIT 200");
            List<Map<String, Object>> list = jdbc.queryForList(sql.toString(), args.toArray());
            return Result.success(list);
        } catch (Exception e) {
            return Result.success(new ArrayList<>());  // 表可能不存在,返空不抛 500
        }
    }

    /** 录入一条读数 */
    @PostMapping("/records")
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        try {
            Long storeId = resolveStoreId(1L);
            String sql = "INSERT INTO energy_record (store_id, record_date, energy_type, meter_reading, daily_usage, daily_cost, recorder, remark, created_at) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
            jdbc.update(sql,
                storeId,
                body.getOrDefault("recordDate", LocalDate.now()),
                body.getOrDefault("energyType", "electric"),
                body.get("meterReading"),
                body.get("dailyUsage"),
                body.get("dailyCost"),
                body.getOrDefault("recorder", ""),
                body.getOrDefault("remark", ""));
            return Result.success(body);
        } catch (Exception e) {
            return Result.error(500, "能耗录入失败: " + e.getMessage());
        }
    }

    /** 本月汇总:按类型 group by */
    @GetMapping("/monthly-summary")
    public Result<Map<String, Object>> monthlySummary(@RequestParam(required = false) String month,
                                                       @RequestParam(defaultValue = "1") Long storeId) {
        try {
            storeId = resolveStoreId(storeId);
            String m = month != null ? month : LocalDate.now().toString().substring(0, 7);
            String[] yearMonth = m.split("-");
            LocalDate first = LocalDate.of(Integer.parseInt(yearMonth[0]), Integer.parseInt(yearMonth[1]), 1);
            LocalDate last = first.plusMonths(1).minusDays(1);
            Map<String, Object> data = new LinkedHashMap<>();
            for (String type : new String[]{"electric", "water", "gas"}) {
                Double usage = nz(jdbc.queryForObject(
                    "SELECT IFNULL(SUM(daily_usage),0) FROM energy_record " +
                    "WHERE store_id=? AND energy_type=? AND record_date BETWEEN ? AND ?",
                    Double.class, storeId, type, first, last));
                Double cost = nz(jdbc.queryForObject(
                    "SELECT IFNULL(SUM(daily_cost),0) FROM energy_record " +
                    "WHERE store_id=? AND energy_type=? AND record_date BETWEEN ? AND ?",
                    Double.class, storeId, type, first, last));
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("usage", usage);
                item.put("cost", cost);
                data.put(type, item);
            }
            data.put("month", m);
            data.put("store_id", storeId);
            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("electric", Map.of("usage", 0, "cost", 0));
            empty.put("water", Map.of("usage", 0, "cost", 0));
            empty.put("gas", Map.of("usage", 0, "cost", 0));
            empty.put("unopened", true);
            return Result.success(empty);
        }
    }

    /** 月度趋势(近6月) */
    @GetMapping("/trend")
    public Result<List<Map<String, Object>>> trend(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            storeId = resolveStoreId(storeId);
            List<Map<String, Object>> result = new ArrayList<>();
            LocalDate base = LocalDate.now().withDayOfMonth(1);
            for (int i = 5; i >= 0; i--) {
                LocalDate first = base.minusMonths(i);
                LocalDate last = first.plusMonths(1).minusDays(1);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("month", first.toString().substring(0, 7));
                m.put("electric", nz(jdbc.queryForObject(
                    "SELECT IFNULL(SUM(daily_usage),0) FROM energy_record WHERE store_id=? AND energy_type='electric' AND record_date BETWEEN ? AND ?",
                    Double.class, storeId, first, last)));
                m.put("water", nz(jdbc.queryForObject(
                    "SELECT IFNULL(SUM(daily_usage),0) FROM energy_record WHERE store_id=? AND energy_type='water' AND record_date BETWEEN ? AND ?",
                    Double.class, storeId, first, last)));
                m.put("gas", nz(jdbc.queryForObject(
                    "SELECT IFNULL(SUM(daily_usage),0) FROM energy_record WHERE store_id=? AND energy_type='gas' AND record_date BETWEEN ? AND ?",
                    Double.class, storeId, first, last)));
                result.add(m);
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    private static Double nz(Double v) { return v == null ? 0.0 : v; }
}

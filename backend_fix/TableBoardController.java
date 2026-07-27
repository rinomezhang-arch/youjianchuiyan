package com.youjian.banquet.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import com.youjian.banquet.common.Result;

import java.util.*;

@RestController
public class TableBoardController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping({"/api/tables/board", "/menu-api/tables/board"})
    public Result<List<Map<String, Object>>> board(@RequestParam(defaultValue = "1") Long storeId,
                                            @RequestParam String date,
                                            @RequestParam(required = false) String period) {
        // 时段过滤：午餐 < 15:00, 晚餐 >= 15:00
        String timeFilter = "";
        if ("morning".equals(period) || "lunch".equals(period)) {
            timeFilter = " AND bt.booking_time < '15:00:00'";
        } else if ("afternoon".equals(period) || "dinner".equals(period)) {
            timeFilter = " AND bt.booking_time >= '15:00:00'";
        }

        String sql = """
            SELECT tm.*,
                bt.table_booking_id, bt.booking_id, bt.booking_date, bt.booking_time,
                bt.package_name, bt.guest_count AS bt_guest_count,
                bm.customer_name, bm.customer_phone, bm.booking_status,
                bm.banquet_name, bm.occasion_type, bm.guest_count AS bm_guest_count,
                (SELECT COUNT(*) FROM booking_dish_detail bdd
                 WHERE bdd.booking_id=bt.booking_id) AS dishes_count,
                (SELECT COUNT(*) FROM booking_master bm2
                 WHERE bm2.customer_phone=bm.customer_phone AND bm2.store_id=bm.store_id
                 AND bm2.booking_status != 'cancelled') AS visit_count
            FROM table_master tm
            LEFT JOIN booking_table bt ON tm.table_id=bt.table_id
                AND tm.store_id=bt.store_id
                AND bt.booking_date=?
                {TIME_FILTER}
            LEFT JOIN booking_master bm ON bt.booking_id=bm.booking_id
                AND bm.store_id=tm.store_id
            WHERE tm.store_id=? AND tm.is_active=1
            ORDER BY tm.table_area, tm.sort_order, tm.table_number
            """.replace("{TIME_FILTER}", timeFilter);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, date, storeId);

        if (period == null || period.isEmpty() || "all".equals(period)) {
            Map<Integer, Map<String, Object>> merged = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Integer tid = (Integer) row.get("table_id");
                Object existing = merged.get(tid);
                if (existing == null) {
                    merged.put(tid, row);
                } else {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> exist = (Map<String, Object>) existing;
                    // 全天模式下，如果已有预订，保存第二个预订信息
                    Object existBookingId = exist.get("booking_id");
                    Object newRowBookingId = row.get("booking_id");
                    if (existBookingId != null && newRowBookingId != null) {
                        // 两个时段都有预订
                        exist.put("booking_id2", newRowBookingId);
                        exist.put("customer_name2", row.get("customer_name"));
                        exist.put("dishes_count2", row.get("dishes_count"));
                        exist.put("booking_time2", row.get("booking_time"));
                    } else if (existBookingId == null && newRowBookingId != null) {
                        // 原来没有预订，新的有
                        exist.put("booking_id", newRowBookingId);
                        exist.put("customer_name", row.get("customer_name"));
                        exist.put("customer_phone", row.get("customer_phone"));
                        exist.put("booking_time", row.get("booking_time"));
                        exist.put("dishes_count", row.get("dishes_count"));
                        exist.put("bm_guest_count", row.get("bm_guest_count"));
                        exist.put("booking_status", row.get("booking_status"));
                        exist.put("banquet_name", row.get("banquet_name"));
                        exist.put("occasion_type", row.get("occasion_type"));
                        exist.put("visit_count", row.get("visit_count"));
                    }
                }
            }
            return Result.success(new ArrayList<>(merged.values()));
        }
        return Result.success(rows);
    }
}

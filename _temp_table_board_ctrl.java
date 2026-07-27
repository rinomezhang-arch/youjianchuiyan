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

    /**
     * GET /api/tables/board?storeId=1&date=2026-07-22&period=morning
     * 桌台看板：每个桌台的当日预订状态 + 菜品数
     */
    @GetMapping({"/api/tables/board", "/menu-api/tables/board"})
    public Result<List<Map<String, Object>>> board(@RequestParam(defaultValue = "1") Long storeId,
                                            @RequestParam String date,
                                            @RequestParam(required = false) String period) {
        String timeFilter = "";
        if ("morning".equals(period)) {
            timeFilter = " AND bt.booking_time='12:00:00'";
        } else if ("afternoon".equals(period)) {
            timeFilter = " AND bt.booking_time='18:00:00'";
        }

        String sql = """
            SELECT tm.*,
                bt.table_booking_id, bt.booking_id, bt.booking_date, bt.booking_time,
                bt.package_name, bt.guest_count AS bt_guest_count,
                bm.customer_name, bm.customer_phone, bm.booking_status,
                bm.banquet_name, bm.occasion_type, bm.guest_count AS bm_guest_count,
                (SELECT COUNT(*) FROM booking_dish_detail bdd
                 WHERE bdd.booking_id=bt.booking_id) AS dishes_count,
                cm.booking_count AS visit_count
            FROM table_master tm
            LEFT JOIN booking_table bt ON tm.table_id=bt.table_id
                AND tm.store_id=bt.store_id
                AND bt.booking_date=?
                {TIME_FILTER}
            LEFT JOIN booking_master bm ON bt.booking_id=bm.booking_id
                AND bm.store_id=tm.store_id
            LEFT JOIN customer_master cm ON bm.customer_phone=cm.customer_phone AND cm.store_id=tm.store_id
            WHERE tm.store_id=? AND tm.is_active=1
            ORDER BY tm.table_area, tm.sort_order, tm.table_number
            """.replace("{TIME_FILTER}", timeFilter);

        List<Map<String, Object>> rows = jdbc.queryForList(sql, date, storeId);

        // 合并同一桌台午/晚双预订(全天模式)
        if (period == null || period.isEmpty() || "all".equals(period)) {
            Map<Integer, Map<String, Object>> merged = new LinkedHashMap<>();
            for (Map<String, Object> row : rows) {
                Integer tid = (Integer) row.get("table_id");
                Object existing = merged.get(tid);
                if (existing == null) {
                    merged.put(tid, row);
                } else {
                    // 双时段: 保留第一个预订为主，追加第二个
                    @SuppressWarnings("unchecked")
                    Map<String, Object> exist = (Map<String, Object>) existing;
                    exist.put("booking_id2", row.get("booking_id"));
                    exist.put("customer_name2", row.get("customer_name"));
                    exist.put("dishes_count2", row.get("dishes_count"));
                }
            }
            return Result.success(new ArrayList<>(merged.values()));
        }

        return Result.success(rows);
    }
}

package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.*;

/**
 * 菜品成本分析控制器（对应前端 Cost.vue 页面）
 * 提供成本汇总、分类、排行等接口，数据来自 dish + recipe 表真实聚合
 */
@RestController
@RequestMapping("/api/cost")
@CrossOrigin(origins = "*")
public class CostController {

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
     * 成本汇总统计
     * 返回 dishTotal, costedCount, avgCostRate, avgMargin, maxCostRate, totalProfit
     */
    @GetMapping("/summary")
    public Result<Map<String, Object>> getSummary(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE d.is_active = 1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND d.store_id = ?"); params.add(sid); }

            // 菜品总数
            int dishTotal = countOrZero(
                "SELECT COUNT(*) FROM dish_master d" + where, params.toArray());

            // 已配成本的菜品（有成本价且>0）
            StringBuilder costWhere = new StringBuilder(where);
            costWhere.append(" AND d.cost_price IS NOT NULL AND d.cost_price > 0");
            int costedCount = countOrZero(
                "SELECT COUNT(*) FROM dish_master d" + costWhere, params.toArray());

            // 平均成本率、平均毛利率、最高成本率、理论总毛利
            // 成本率 = cost_price / sale_price * 100 (仅对有售价的菜品)
            StringBuilder aggWhere = new StringBuilder(where);
            aggWhere.append(" AND d.sale_price IS NOT NULL AND d.sale_price > 0 AND d.cost_price IS NOT NULL");
            List<Object> aggParams = new ArrayList<>(params);

            Map<String, Object> agg = null;
            try {
                agg = jdbc.queryForMap(
                    "SELECT "
                    + "COUNT(*) AS cnt, "
                    + "AVG(CASE WHEN d.sale_price>0 THEN d.cost_price*100.0/d.sale_price ELSE 0 END) AS avg_cost_rate, "
                    + "MAX(CASE WHEN d.sale_price>0 THEN d.cost_price*100.0/d.sale_price ELSE 0 END) AS max_cost_rate, "
                    + "AVG(CASE WHEN d.sale_price>0 THEN (d.sale_price-d.cost_price)*100.0/d.sale_price ELSE 0 END) AS avg_margin, "
                    + "COALESCE(SUM(d.sale_price-d.cost_price),0) AS total_profit "
                    + "FROM dish_master d" + aggWhere, aggParams.toArray());
            } catch (Exception ignored) {}

            double avgCostRate = 0.0;
            double avgMargin = 0.0;
            double maxCostRate = 0.0;
            double totalProfit = 0.0;
            if (agg != null) {
                Number acr = (Number) agg.get("avg_cost_rate");
                Number am = (Number) agg.get("avg_margin");
                Number mcr = (Number) agg.get("max_cost_rate");
                Number tp = (Number) agg.get("total_profit");
                if (acr != null) avgCostRate = acr.doubleValue();
                if (am != null) avgMargin = am.doubleValue();
                if (mcr != null) maxCostRate = mcr.doubleValue();
                if (tp != null) totalProfit = tp.doubleValue();
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("dishTotal", dishTotal);
            data.put("costedCount", costedCount);
            data.put("avgCostRate", Math.round(avgCostRate * 10.0) / 10.0);
            data.put("avgMargin", Math.round(avgMargin * 10.0) / 10.0);
            data.put("maxCostRate", Math.round(maxCostRate * 10.0) / 10.0);
            data.put("totalProfit", Math.round(totalProfit * 100.0) / 100.0);
            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("dishTotal", 0);
            data.put("costedCount", 0);
            data.put("avgCostRate", 0);
            data.put("avgMargin", 0);
            data.put("maxCostRate", 0);
            data.put("totalProfit", 0);
            return Result.success(data);
        }
    }

    /**
     * 菜品分类列表（去重）
     */
    @GetMapping("/categories")
    public Result<List<String>> getCategories(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder sql = new StringBuilder(
                "SELECT DISTINCT d.category FROM dish_master d WHERE d.is_active=1 AND d.category IS NOT NULL AND d.category<>''");
            List<Object> params = new ArrayList<>();
            if (sid != null) { sql.append(" AND d.store_id = ?"); params.add(sid); }
            sql.append(" ORDER BY d.category");
            List<Map<String, Object>> rows = jdbc.queryForList(sql.toString(), params.toArray());
            List<String> result = new ArrayList<>();
            for (Map<String, Object> r : rows) {
                Object v = r.get("category");
                if (v != null && !v.toString().isEmpty()) {
                    result.add(v.toString());
                }
            }
            return Result.success(result);
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    /**
     * 成本排行（分页+筛选+排序）
     */
    @GetMapping("/ranking")
    public Result<Map<String, Object>> getRanking(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "rate") String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE d.is_active = 1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND d.store_id = ?"); params.add(sid); }
            if (category != null && !category.isEmpty()) { where.append(" AND d.category = ?"); params.add(category); }
            if (search != null && !search.trim().isEmpty()) {
                // dish_master 没有 pinyin_code 列，此前这个查询整个 catch 到静默空结果，
                // 搜索框从来没真正生效过
                where.append(" AND d.dish_name LIKE ?");
                params.add("%" + search.trim() + "%");
            }

            // 总数
            int total = countOrZero("SELECT COUNT(*) FROM dish_master d" + where, params.toArray());

            // 排序
            String orderSql;
            switch (sortBy == null ? "rate" : sortBy) {
                case "profit":
                    orderSql = "ORDER BY profit DESC";
                    break;
                case "price":
                    orderSql = "ORDER BY d.sale_price DESC";
                    break;
                case "rate":
                default:
                    orderSql = "ORDER BY cost_rate DESC";
            }

            // 分页
            if (page < 1) page = 1;
            if (size < 1 || size > 500) size = 50;
            int offset = (page - 1) * size;

            StringBuilder sql = new StringBuilder();
            sql.append("SELECT d.dish_id AS dishId, d.dish_name AS dishName, d.category AS category, ");
            sql.append("d.sale_price AS salePrice, d.cost_price AS costPrice, ");
            sql.append("CASE WHEN d.sale_price>0 THEN ROUND(d.cost_price*100.0/d.sale_price,1) ELSE 0 END AS costRate, ");
            sql.append("CASE WHEN d.sale_price>0 THEN ROUND((d.sale_price-d.cost_price)*100.0/d.sale_price,1) ELSE 0 END AS marginRate, ");
            sql.append("(d.sale_price-d.cost_price) AS profit, d.unit AS unit ");
            sql.append("FROM dish_master d");
            sql.append(where);
            sql.append(" ");
            sql.append(orderSql);
            sql.append(" LIMIT ? OFFSET ?");
            params.add(size);
            params.add(offset);

            List<Map<String, Object>> content;
            try {
                content = jdbc.queryForList(sql.toString(), params.toArray());
            } catch (Exception e) {
                content = new ArrayList<>();
            }

            Map<String, Object> data = new LinkedHashMap<>();
            data.put("content", content);
            data.put("total", total);
            data.put("page", page);
            data.put("size", size);
            data.put("totalPages", (int) Math.ceil((double) total / size));
            return Result.success(data);
        } catch (Exception e) {
            Map<String, Object> data = new LinkedHashMap<>();
            data.put("content", new ArrayList<>());
            data.put("total", 0);
            data.put("page", page);
            data.put("size", size);
            data.put("totalPages", 0);
            return Result.success(data);
        }
    }

    private int countOrZero(String sql, Object... args) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class, args);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }
}

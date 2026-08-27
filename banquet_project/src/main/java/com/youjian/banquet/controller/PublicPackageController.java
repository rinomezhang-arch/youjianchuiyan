package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 官网"宴会套餐 · 婚宴与庆典"用的套餐信息。免登录公开接口，见 WebMvcConfig 放行配置。
 * 只返回展示所需字段，不含 cost_price/cost_rate 等内部成本数据。
 */
@RestController
@RequestMapping("/api/public/packages")
@CrossOrigin(origins = "*")
public class PublicPackageController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Long storeId) {
        String sql = "SELECT package_id, package_name, price, original_price, occasion_type, " +
                "min_guests, max_guests, dish_count, description, image_url " +
                "FROM package_master WHERE status = 1 AND store_id = ? " +
                "GROUP BY package_id ORDER BY sort_order, package_id";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, storeId != null ? storeId : 1L);
        return Result.success(rows);
    }

    /** 套餐详情页：套餐基础信息 + 真实"适用门店"列表（同一 package_id 在 package_master 里按门店各一行）。 */
    @GetMapping("/{packageId}")
    public Result<Map<String, Object>> detail(@PathVariable String packageId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT pm.package_id, pm.package_name, pm.price, pm.original_price, pm.occasion_type, " +
                "pm.min_guests, pm.max_guests, pm.dish_count, pm.description, pm.image_url, " +
                "pm.store_id, si.store_name " +
                "FROM package_master pm JOIN store_info si ON si.store_id = pm.store_id " +
                "WHERE pm.package_id = ? AND pm.status = 1 ORDER BY pm.store_id", packageId);
        if (rows.isEmpty()) return Result.error(404, "套餐不存在");
        Map<String, Object> first = rows.get(0);
        Map<String, Object> detail = new java.util.LinkedHashMap<>(first);
        detail.remove("store_id");
        detail.remove("store_name");
        List<Map<String, Object>> stores = rows.stream().map(r -> Map.of(
                "storeId", r.get("store_id"), "storeName", r.get("store_name")
        )).toList();
        detail.put("stores", stores);
        return Result.success(detail);
    }
}

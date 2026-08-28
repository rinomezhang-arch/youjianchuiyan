package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 官网首页"招牌菜品"预览。免登录公开接口——访客还没有账号，不可能带 JWT
 * (见 WebMvcConfig 里的放行配置)。只返回菜名/分类/售价这几个对外安全的字段，
 * 不走 DishController 现有的 /api/dishes（那个接口返回完整 DishDTO，
 * 包含成本/毛利等内部数据，不适合直接暴露给未登录的公网访客）。
 */
@RestController
@RequestMapping("/api/public/menu")
@CrossOrigin(origins = "*")
public class PublicMenuController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping("/preview")
    public Result<List<Map<String, Object>>> preview(@RequestParam(defaultValue = "1") Long storeId,
                                                       @RequestParam(defaultValue = "12") int limit) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT dish_name, dish_name_en, dish_category, sale_price, dish_intro FROM dish_master " +
                "WHERE store_id = ? AND is_active = 1 AND sale_price > 0 " +
                "ORDER BY is_specialty DESC, sale_price DESC LIMIT ?",
                storeId, limit);
        return Result.success(rows);
    }

    /** 门店详情页真正下单用的完整菜单——带 dish_id，不限量，前端按分类筛选后展示。
     *  和 /preview 分开是因为首页招牌菜橱窗只要几张图那种，不需要 dish_id/全量。 */
    @GetMapping("/full")
    public Result<List<Map<String, Object>>> full(@RequestParam(defaultValue = "1") Long storeId) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT dish_id, dish_name, dish_name_en, dish_category, sale_price, dish_intro FROM dish_master " +
                "WHERE store_id = ? AND is_active = 1 AND sale_price > 0 " +
                "ORDER BY dish_category, is_specialty DESC, sale_price DESC",
                storeId);
        return Result.success(rows);
    }
}

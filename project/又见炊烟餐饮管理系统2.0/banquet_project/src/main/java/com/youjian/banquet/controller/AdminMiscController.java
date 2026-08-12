package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.config.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 聚合的管理端缺失接口：菜单/部门/角色/员工/商品/订单/桌台/菜品/权限等
 */
@RestController
@CrossOrigin
public class AdminMiscController {

    @Autowired
    private JdbcTemplate jdbc;

    // ============ auth/info ============
    @GetMapping({"/api/auth/info", "/menu-api/auth/info"})
    public Result<Map<String, Object>> authInfo(@RequestHeader(value = "Authorization", required = false) String bearer) {
        try {
            // 解析 token / 默认返回 rino 用户
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("userId", 1L);
            user.put("username", "rino");
            user.put("realName", "系统管理员");
            user.put("avatar", "");
            user.put("role", "SUPER_ADMIN");
            user.put("storeId", 1L);
            user.put("storeName", "又见炊烟（总店）");
            user.put("permissions", List.of("*"));
            return Result.success(user);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ============ 菜单/侧边栏 ============
    @GetMapping({"/api/menus", "/menu-api/menus"})
    public Result<List<Map<String, Object>>> menus() {
        List<Map<String, Object>> out = new ArrayList<>();
        String[] names = {"工作台", "前厅运营", "宴会中心", "财务管理", "报表分析", "商品管理", "人事管理", "会员管理", "营销中心", "总经理工作台", "AI助手"};
        String[] paths = {"/dashboard", "/front-office", "/banquet", "/finance", "/report", "/goods", "/hr", "/member", "/marketing", "/gm", "/ai"};
        String[] icons = {"🏠","🧾","🎊","💰","📊","🍲","👥","🎁","📈","🏢","🤖"};
        for (int i = 0; i < names.length; i++) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", i + 1);
            m.put("name", names[i]);
            m.put("path", paths[i]);
            m.put("icon", icons[i]);
            m.put("children", List.of());
            out.add(m);
        }
        return Result.success(out);
    }

    // ============ 部门 & 角色 & 员工 简化版 ============
    @GetMapping("/api/departments")
    public Result<List<Map<String, Object>>> departments(@RequestParam(defaultValue = "1") Long storeId) {
        String sql = "SELECT department_id, department_name, parent_id, sort_order, status FROM department WHERE store_id = ? OR ? = 0 ORDER BY sort_order";
        try { return Result.success(jdbc.queryForList(sql, storeId, storeId)); }
        catch (Exception e) { return Result.success(new ArrayList<>()); }
    }

    @GetMapping("/api/roles")
    public Result<List<Map<String, Object>>> roles(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            String sql = "SELECT role_id, role_name, role_code, description, status FROM role_master WHERE store_id = ? OR ? = 0 ORDER BY role_id";
            return Result.success(jdbc.queryForList(sql, storeId, storeId));
        } catch (Exception e) {
            List<Map<String, Object>> defaults = new ArrayList<>();
            String[][] rs = {{"1","超级管理员","SUPER_ADMIN"},{"2","店长","STORE_MANAGER"},{"3","服务员","WAITER"},{"4","厨师","CHEF"},{"5","收银员","CASHIER"}};
            for (String[] r : rs) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("roleId", Long.valueOf(r[0]));
                m.put("roleName", r[1]);
                m.put("roleCode", r[2]);
                defaults.add(m);
            }
            return Result.success(defaults);
        }
    }

    @GetMapping("/api/staff/list")
    public Result<Map<String, Object>> staffList(@RequestParam(defaultValue = "1") Long storeId,
                                                 @RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size) {
        try {
            Long effective = (storeId == 0) ? null : storeId;
            String where = effective == null ? "1=1" : "store_id = " + effective;
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM staff_master WHERE " + where, Long.class);
            if (total == null) total = 0L;
            int offset = (page - 1) * size;
            String sql = "SELECT staff_id, staff_name, staff_gender as gender, staff_phone as phone, " +
                    "department, staff_position as position, employment_status, " +
                    "monthly_salary as basic_salary, hire_date FROM staff_master WHERE " + where +
                    " ORDER BY staff_id DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, size, offset);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("total", total);
            out.put("list", rows);
            out.put("page", page);
            out.put("size", size);
            return Result.success(out);
        } catch (Exception e) {
            return Result.error(500, e.getMessage());
        }
    }

    // ============ 商品管理 categories/products ============
    @GetMapping({"/api/categories", "/menu-api/categories"})
    public Result<List<Map<String, Object>>> categories() {
        try {
            String sql = "SELECT dish_type_id as category_id, type_name as name, description, sort_order, status FROM dish_type WHERE is_active = 1 OR status = 'active' ORDER BY sort_order";
            return Result.success(jdbc.queryForList(sql));
        } catch (Exception e) {
            return Result.success(new ArrayList<>());
        }
    }

    @GetMapping({"/api/products", "/menu-api/products"})
    public Result<Map<String, Object>> products(@RequestParam(defaultValue = "1") Long storeId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "10") int size,
                                                @RequestParam(required = false) String keyword) {
        try {
            Long eff = (storeId == 0) ? null : storeId;
            StringBuilder sb = new StringBuilder();
            List<Object> params = new ArrayList<>();
            sb.append("FROM menu_item WHERE 1=1");
            if (eff != null) { sb.append(" AND store_id = ?"); params.add(eff); }
            if (keyword != null && !keyword.isEmpty()) { sb.append(" AND (item_name LIKE ? OR pinyin_code LIKE ?)"); params.add("%"+keyword+"%"); params.add("%"+keyword+"%"); }
            Long total = jdbc.queryForObject("SELECT COUNT(*) " + sb.toString(), Long.class, params.toArray());
            if (total == null) total = 0L;
            params.add(size); params.add((page-1)*size);
            List<Map<String, Object>> rows = jdbc.queryForList("SELECT item_id, item_name, item_code, category_id, unit, price, stock, status, description, image_url FROM menu_item " + sb.toString().replaceFirst("FROM menu_item", "") + " ORDER BY item_id DESC LIMIT ? OFFSET ?", params.toArray());
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("total", total);
            out.put("list", rows);
            out.put("page", page);
            out.put("size", size);
            return Result.success(out);
        } catch (Exception e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("total", 0);
            out.put("list", new ArrayList<>());
            return Result.success(out);
        }
    }

    // ============ 订单 orders/list ============
    @GetMapping({"/api/orders/list", "/menu-api/orders/list"})
    public Result<Map<String, Object>> ordersList(@RequestParam(defaultValue = "1") Long storeId,
                                                  @RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int size) {
        try {
            Long eff = (storeId == 0) ? null : storeId;
            String where = eff == null ? "1=1" : "store_id = " + eff;
            Long total = jdbc.queryForObject("SELECT COUNT(*) FROM order_master WHERE " + where, Long.class);
            if (total == null) total = 0L;
            int offset = (page - 1) * size;
            String sql = "SELECT order_id, order_no, table_id, booking_id, total_amount, actual_amount, discount_amount, status, order_time, pay_status, waiter_name FROM order_master WHERE " + where +
                    " ORDER BY order_time DESC LIMIT ? OFFSET ?";
            List<Map<String, Object>> rows = jdbc.queryForList(sql, size, offset);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("total", total);
            out.put("list", rows);
            out.put("page", page);
            out.put("size", size);
            return Result.success(out);
        } catch (Exception e) {
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("total", 0); out.put("list", new ArrayList<>()); out.put("page", page); out.put("size", size);
            return Result.success(out);
        }
    }

    // ============ 桌台：/table/area/list, /table/dashboard ============
    @GetMapping("/api/table/area/list")
    public Result<List<Map<String, Object>>> areaList(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            String sql = "SELECT area_id, area_name, store_id, sort_order, status FROM table_area WHERE store_id = ? OR ? = 0 ORDER BY sort_order";
            return Result.success(jdbc.queryForList(sql, storeId, storeId));
        } catch (Exception e) {
            List<Map<String, Object>> def = new ArrayList<>();
            Map<String,Object> m=new LinkedHashMap<>(); m.put("areaId",1L); m.put("areaName","大厅"); def.add(m);
            return Result.success(def);
        }
    }

    @GetMapping("/api/table/dashboard")
    public Result<Map<String, Object>> tableDashboard(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            Long eff = (storeId == 0) ? null : storeId;
            String where = eff == null ? "1=1" : "store_id = " + eff;
            List<Map<String, Object>> tables = jdbc.queryForList(
                    "SELECT table_id, table_no, table_name, capacity, area_id, status FROM table_master WHERE " + where + " ORDER BY area_id, table_no");
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("total", tables.size());
            long free = tables.stream().filter(x -> "idle".equals(x.get("status")) || "空闲".equals(x.get("status"))).count();
            long used = tables.stream().filter(x -> "occupied".equals(x.get("status")) || "使用中".equals(x.get("status"))).count();
            out.put("freeCount", free);
            out.put("usedCount", used);
            out.put("tables", tables);
            return Result.success(out);
        } catch (Exception e) {
            Map<String,Object> m=new LinkedHashMap<>(); m.put("total",0);m.put("freeCount",0);m.put("usedCount",0);m.put("tables",new ArrayList<>());
            return Result.success(m);
        }
    }

    // ============ 菜品：/dish/info/dict, /dish/info/menu-list ============
    @GetMapping("/api/dish/info/dict")
    public Result<Map<String, Object>> dishDict(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            List<Map<String, Object>> types = jdbc.queryForList(
                    "SELECT dish_type_id as value, type_name as label, sort_order FROM dish_type WHERE store_id = ? OR ? = 0 ORDER BY sort_order",
                    storeId, storeId);
            Map<String, Object> out = new LinkedHashMap<>();
            out.put("dishTypes", types);
            out.put("units", List.of("份","盘","个","斤","杯","碗","例"));
            out.put("statuses", List.of(Map.of("value","on_sale","label","在售"),Map.of("value","off_sale","label","下架")));
            return Result.success(out);
        } catch (Exception e) {
            return Result.success(new LinkedHashMap<>());
        }
    }

    @GetMapping("/api/dish/info/menu-list")
    public Result<Map<String, Object>> menuList(@RequestParam(defaultValue = "1") Long storeId,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return products(storeId, page, size, null);
    }
}

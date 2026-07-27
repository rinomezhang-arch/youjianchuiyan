package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 数据字典 Controller
 * 基于 sys_dict / sys_dict_item 表，提供字典查询接口
 * 保证 occasion_type / source_type / booking_type / time_slot 等字段的数据有效性
 */
@RestController
@RequestMapping({"/api/dict", "/menu-api/dict"})
public class DictController {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 查询所有字典类型
     * GET /api/dict/types?storeId=1
     */
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> listTypes(
            @RequestParam(defaultValue = "1") Long storeId) {
        String sql = "SELECT dict_id, dict_code, dict_name, dict_type, description, sort_order, is_active " +
                     "FROM sys_dict WHERE store_id = ? AND is_active = 1 ORDER BY sort_order";
        return Result.success(jdbc.queryForList(sql, storeId));
    }

    /**
     * 按字典编码查询字典项
     * GET /api/dict/items/{dictCode}?storeId=1
     */
    @GetMapping("/items/{dictCode}")
    public Result<List<Map<String, Object>>> listItems(
            @PathVariable String dictCode,
            @RequestParam(defaultValue = "1") Long storeId) {
        String sql = """
            SELECT i.item_id, i.item_value, i.item_label, i.parent_id, i.sort_order, i.is_active, i.remark
            FROM sys_dict_item i
            JOIN sys_dict t ON i.dict_id = t.dict_id
            WHERE t.dict_code = ? AND t.store_id = ? AND i.is_active = 1
            ORDER BY i.sort_order
            """;
        return Result.success(jdbc.queryForList(sql, dictCode, storeId));
    }

    /**
     * 批量查询多个字典类型的字典项
     * GET /api/dict/batch?codes=occasion_type,source_type,booking_type&storeId=1
     * 返回: { occasion_type: [...], source_type: [...], ... }
     */
    @GetMapping("/batch")
    public Result<Map<String, List<Map<String, Object>>>> batchItems(
            @RequestParam String codes,
            @RequestParam(defaultValue = "1") Long storeId) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        String[] codeArray = codes.split(",");
        String sql = """
            SELECT i.item_value, i.item_label, i.parent_id, i.sort_order, i.remark
            FROM sys_dict_item i
            JOIN sys_dict t ON i.dict_id = t.dict_id
            WHERE t.dict_code = ? AND t.store_id = ? AND i.is_active = 1
            ORDER BY i.sort_order
            """;
        for (String code : codeArray) {
            code = code.trim();
            if (!code.isEmpty()) {
                result.put(code, jdbc.queryForList(sql, code, storeId));
            }
        }
        return Result.success(result);
    }

    /**
     * 新增字典项
     * POST /api/dict/items
     */
    @PostMapping("/items")
    public Result<Map<String, Object>> addItem(@RequestBody Map<String, Object> body) {
        String dictCode = (String) body.get("dict_code");
        String itemValue = (String) body.get("item_value");
        String itemLabel = (String) body.get("item_label");
        String remark = body.get("remark") != null ? (String) body.get("remark") : "";
        Integer sortOrder = body.get("sort_order") != null ? Integer.valueOf(body.get("sort_order").toString()) : 0;
        Long storeId = body.get("store_id") != null ? Long.valueOf(body.get("store_id").toString()) : 1L;

        // 查找 dict_id
        List<Map<String, Object>> types = jdbc.queryForList(
            "SELECT dict_id FROM sys_dict WHERE dict_code = ? AND store_id = ?", dictCode, storeId);
        if (types.isEmpty()) {
            return Result.error("字典类型不存在: " + dictCode);
        }
        Long dictId = ((Number) types.get(0).get("dict_id")).longValue();

        jdbc.update("INSERT INTO sys_dict_item (dict_id, dict_code, item_value, item_label, store_id, sort_order, remark) VALUES (?, ?, ?, ?, ?, ?, ?)",
            dictId, dictCode, itemValue, itemLabel, storeId, sortOrder, remark);

        Map<String, Object> result = new HashMap<>();
        result.put("dict_code", dictCode);
        result.put("item_value", itemValue);
        result.put("item_label", itemLabel);
        return Result.success(result);
    }

    /**
     * 更新字典项
     * PUT /api/dict/items/{itemId}
     */
    @PutMapping("/items/{itemId}")
    public Result<Void> updateItem(@PathVariable Long itemId, @RequestBody Map<String, Object> body) {
        List<String> sets = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (body.get("item_label") != null) {
            sets.add("item_label = ?");
            params.add(body.get("item_label"));
        }
        if (body.get("sort_order") != null) {
            sets.add("sort_order = ?");
            params.add(Integer.valueOf(body.get("sort_order").toString()));
        }
        if (body.get("is_active") != null) {
            sets.add("is_active = ?");
            params.add(Integer.valueOf(body.get("is_active").toString()));
        }
        if (body.get("remark") != null) {
            sets.add("remark = ?");
            params.add(body.get("remark"));
        }

        if (sets.isEmpty()) {
            return Result.error("无更新字段");
        }

        params.add(itemId);
        jdbc.update("UPDATE sys_dict_item SET " + String.join(", ", sets) + " WHERE item_id = ?", params.toArray());
        return Result.success(null);
    }

    /**
     * 删除字典项（软删除）
     * DELETE /api/dict/items/{itemId}
     */
    @DeleteMapping("/items/{itemId}")
    public Result<Void> deleteItem(@PathVariable Long itemId) {
        jdbc.update("UPDATE sys_dict_item SET is_active = 0 WHERE item_id = ?", itemId);
        return Result.success(null);
    }

    // ===== 员工列表（预定员下拉） =====

    /**
     * 获取在职员工列表（预定员下拉用）
     * GET /api/dict/staff?storeId=1&keyword=张
     */
    @GetMapping("/staff")
    public Result<List<Map<String, Object>>> listStaff(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String keyword) {
        StringBuilder sql = new StringBuilder(
            "SELECT staff_id, staff_name, staff_no, phone, department, position " +
            "FROM staff_master WHERE store_id = ? AND employment_status = 'active' ");
        List<Object> params = new ArrayList<>();
        params.add(storeId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (staff_name LIKE ? OR staff_no LIKE ? OR phone LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY staff_name");
        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }

    // ===== 客户模糊搜索 =====

    /**
     * 客户模糊搜索（客户姓名/手机号）
     * GET /api/dict/customers?storeId=1&keyword=张
     * 用于客户姓名、代订人、介绍人下拉搜索
     */
    @GetMapping("/customers")
    public Result<List<Map<String, Object>>> searchCustomers(
            @RequestParam(defaultValue = "1") Long storeId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "20") Integer limit) {
        StringBuilder sql = new StringBuilder(
            "SELECT customer_id, customer_name, customer_phone, gender, birthday, " +
            "member_level, total_visits, source, remark " +
            "FROM customer_master WHERE store_id = ? AND is_active = 1 ");
        List<Object> params = new ArrayList<>();
        params.add(storeId);
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" AND (customer_name LIKE ? OR customer_phone LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
        }
        sql.append(" ORDER BY total_visits DESC, customer_name LIMIT ?");
        params.add(limit);
        return Result.success(jdbc.queryForList(sql.toString(), params.toArray()));
    }
}

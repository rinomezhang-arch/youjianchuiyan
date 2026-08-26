package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 标签管理（Tags.vue：口味/特征/过敏原/饮食类型/烹饪方式标签库）。
 * 对应表 tag_master（见 scripts/migrations/create_tag_master_v1.sql）。
 * <p>
 * dish_count（标签关联菜品数）目前没有真实的"标签-菜品"关联表，如实固定返回 0，
 * 不编造假数字——之前前端在接口失败时会 catch 到一批带虚构 dishCount 的假数据。
 */
@RestController
@RequestMapping("/api/tags")
@CrossOrigin(origins = "*")
public class TagController {

    @Autowired
    private JdbcTemplate jdbc;

    @GetMapping
    public Result<List<Map<String, Object>>> listTags(@RequestParam(required = false) String storeId) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> args = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id = ?"); args.add(sid); }
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT tag_id AS id, tag_name AS name, tag_name_en AS nameEn, tag_group AS `group`, " +
                            "tag_color AS color, sort_order AS sort, 0 AS dishCount " +
                            "FROM tag_master" + where + " ORDER BY tag_group, sort_order, tag_id",
                    args.toArray());
            return Result.success(rows);
        } catch (Exception e) {
            return Result.error(500, "获取标签列表失败: " + e.getMessage());
        }
    }

    @PostMapping
    public Result<Map<String, Object>> createTag(@RequestBody Map<String, Object> body) {
        try {
            Long sid = writeStoreId();
            String name = requireString(body, "name", "标签名称不能为空");
            String nameEn = asString(body.get("nameEn"));
            String group = requireString(body, "group", "标签分组不能为空");
            String color = asString(body.get("color"));
            int sort = body.get("sort") != null ? Integer.parseInt(body.get("sort").toString()) : 0;

            jdbc.update("INSERT INTO tag_master (store_id, tag_name, tag_name_en, tag_group, tag_color, sort_order) " +
                    "VALUES (?,?,?,?,?,?)", sid, name, nameEn, group, color, sort);
            Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("id", newId);
            saved.put("name", name);
            saved.put("nameEn", nameEn);
            saved.put("group", group);
            saved.put("color", color);
            saved.put("sort", sort);
            saved.put("dishCount", 0);
            return Result.success(saved);
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建标签失败: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public Result<Void> updateTag(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            List<String> sets = new ArrayList<>();
            List<Object> args = new ArrayList<>();
            if (body.get("name") != null) { sets.add("tag_name = ?"); args.add(body.get("name")); }
            if (body.get("nameEn") != null) { sets.add("tag_name_en = ?"); args.add(body.get("nameEn")); }
            if (body.get("group") != null) { sets.add("tag_group = ?"); args.add(body.get("group")); }
            if (body.get("color") != null) { sets.add("tag_color = ?"); args.add(body.get("color")); }
            if (body.get("sort") != null) { sets.add("sort_order = ?"); args.add(Integer.parseInt(body.get("sort").toString())); }
            if (sets.isEmpty()) return Result.success(null);
            args.add(id);
            int updated = jdbc.update("UPDATE tag_master SET " + String.join(", ", sets) + " WHERE tag_id = ?", args.toArray());
            if (updated == 0) return Result.error(404, "标签不存在");
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "更新标签失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteTag(@PathVariable Long id) {
        try {
            int deleted = jdbc.update("DELETE FROM tag_master WHERE tag_id = ?", id);
            if (deleted == 0) return Result.error(404, "标签不存在");
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "删除标签失败: " + e.getMessage());
        }
    }

    private static String requireString(Map<String, Object> body, String key, String errMsg) {
        Object v = body.get(key);
        if (v == null || v.toString().trim().isEmpty()) throw new IllegalArgumentException(errMsg);
        return v.toString().trim();
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private Long resolveStoreId(String storeId) {
        if (UserContext.isGeneralManager()) {
            if (storeId == null || storeId.isEmpty() || "all".equalsIgnoreCase(storeId)) return null;
            try { return Long.parseLong(storeId); } catch (NumberFormatException e) { return null; }
        }
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }

    private Long writeStoreId() {
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? 1L : sid;
    }
}

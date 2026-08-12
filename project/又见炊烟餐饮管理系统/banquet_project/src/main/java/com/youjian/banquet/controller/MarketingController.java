package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.*;

/**
 * 营销活动管理控制器。
 * <p>
 * 业务规则：
 * <ul>
 *   <li>营销活动由总经理统一配置（POST/PUT/DELETE 仅总经理可执行）</li>
 *   <li>分店仅可上架本店活动（GET 按 store_id 隔离，店长强制限制为本店）</li>
 *   <li>优惠券、折扣规则查询同样按门店隔离</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/marketing")
@CrossOrigin(origins = "*")
public class MarketingController {

    @Autowired
    private JdbcTemplate jdbc;

    // ======================== 营销活动 ========================

    /**
     * GET /api/marketing/activities — 活动列表。
     * 总经理可传 storeId 指定门店；店长强制限制为本店。
     */
    @GetMapping("/activities")
    public Result<List<Map<String, Object>>> listActivities(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String activityType,
            @RequestParam(required = false) Integer isActive,
            @RequestParam(required = false) String keyword) {
        try {
            Long effectiveStoreId = resolveQueryStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (effectiveStoreId != null) {
                where.append(" AND store_id = ?");
                params.add(effectiveStoreId);
            }
            if (activityType != null && !activityType.isEmpty()) {
                where.append(" AND activity_type = ?");
                params.add(activityType);
            }
            if (isActive != null) {
                where.append(" AND is_active = ?");
                params.add(isActive);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                where.append(" AND (activity_name LIKE ? OR activity_code LIKE ?)");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw); params.add(kw);
            }
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT * FROM marketing_activity" + where + " ORDER BY activity_id DESC",
                    params.toArray());
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取活动列表失败: " + e.getMessage());
        }
    }

    /** POST /api/marketing/activities — 创建活动（仅总经理）。 */
    @PostMapping("/activities")
    @Transactional
    public Result<Map<String, Object>> createActivity(@RequestBody Map<String, Object> body) {
        try {
            UserContext.assertGeneralManager();
            Long targetStoreId = asLong(body.get("storeId"));
            if (targetStoreId == null) targetStoreId = 1L;
            String activityName = requireString(body, "activityName", "活动名称不能为空");
            String activityType = requireString(body, "activityType", "活动类型不能为空");
            String activityCode = body.get("activityCode") != null
                    ? body.get("activityCode").toString().trim()
                    : generateCode("ACT", targetStoreId);

            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM marketing_activity WHERE activity_code = ? AND store_id = ?",
                    Integer.class, activityCode, targetStoreId);
            if (exists != null && exists > 0) {
                return Result.error(400, "活动编码已存在: " + activityCode);
            }

            Date startDate = asSqlDate(body.get("startDate"));
            Date endDate = asSqlDate(body.get("endDate"));
            Integer isActive = body.get("isActive") == null ? 1 : asInt(body.get("isActive"), 1);
            String activityRules = asString(body.get("activityRules"));
            String activityContent = asString(body.get("activityContent"));
            String targetCustomers = asString(body.get("targetCustomers"));
            BigDecimal budgetAmount = asDecimal(body.get("budgetAmount"), null);
            BigDecimal expectedIncome = asDecimal(body.get("expectedIncome"), null);
            String description = asString(body.get("description"));
            String remark = asString(body.get("remark"));
            Integer operatorId = UserContext.getStaffId() == null ? null : UserContext.getStaffId().intValue();
            String operatorName = UserContext.getUsername();

            String sql = "INSERT INTO marketing_activity (store_id, activity_code, activity_name, activity_type, " +
                    "start_date, end_date, is_active, activity_rules, activity_content, target_customers, " +
                    "budget_amount, expected_income, operator_id, operator_name, description, remark) " +
                    "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            jdbc.update(sql, targetStoreId, activityCode, activityName, activityType, startDate, endDate,
                    isActive, activityRules, activityContent, targetCustomers, budgetAmount, expectedIncome,
                    operatorId, operatorName, description, remark);

            Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            Map<String, Object> saved = jdbc.queryForList(
                    "SELECT * FROM marketing_activity WHERE activity_id = ?", newId).get(0);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建活动失败: " + e.getMessage());
        }
    }

    /** PUT /api/marketing/activities/{id} — 修改活动（仅总经理）。 */
    @PutMapping("/activities/{id}")
    @Transactional
    public Result<Map<String, Object>> updateActivity(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            UserContext.assertGeneralManager();
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM marketing_activity WHERE activity_id = ? LIMIT 1", id);
            if (rows.isEmpty()) return Result.error(404, "活动不存在");

            StringBuilder set = new StringBuilder();
            List<Object> params = new ArrayList<>();
            appendUpdate(set, params, "activity_name", body.get("activityName"));
            appendUpdate(set, params, "activity_type", body.get("activityType"));
            appendUpdate(set, params, "start_date", asSqlDate(body.get("startDate")));
            appendUpdate(set, params, "end_date", asSqlDate(body.get("endDate")));
            if (body.get("isActive") != null) appendUpdate(set, params, "is_active", asInt(body.get("isActive"), null));
            appendUpdate(set, params, "activity_rules", asString(body.get("activityRules")));
            appendUpdate(set, params, "activity_content", asString(body.get("activityContent")));
            appendUpdate(set, params, "target_customers", asString(body.get("targetCustomers")));
            if (body.get("budgetAmount") != null) appendUpdate(set, params, "budget_amount", asDecimal(body.get("budgetAmount"), null));
            if (body.get("expectedIncome") != null) appendUpdate(set, params, "expected_income", asDecimal(body.get("expectedIncome"), null));
            appendUpdate(set, params, "description", asString(body.get("description")));
            appendUpdate(set, params, "remark", asString(body.get("remark")));
            // 总经理可调整门店归属
            Long newStoreId = asLong(body.get("storeId"));
            if (newStoreId != null) appendUpdate(set, params, "store_id", newStoreId);

            if (set.length() == 0) return Result.success(rows.get(0));
            set.append(", update_time = CURRENT_TIMESTAMP");
            params.add(id);
            jdbc.update("UPDATE marketing_activity SET " + set + " WHERE activity_id = ?", params.toArray());
            Map<String, Object> saved = jdbc.queryForList(
                    "SELECT * FROM marketing_activity WHERE activity_id = ?", id).get(0);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新活动失败: " + e.getMessage());
        }
    }

    /** DELETE /api/marketing/activities/{id} — 删除活动（仅总经理）。 */
    @DeleteMapping("/activities/{id}")
    @Transactional
    public Result<?> deleteActivity(@PathVariable Long id) {
        try {
            UserContext.assertGeneralManager();
            int affected = jdbc.update("DELETE FROM marketing_activity WHERE activity_id = ?", id);
            if (affected == 0) return Result.error(404, "活动不存在");
            return Result.success("活动已删除");
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "删除活动失败: " + e.getMessage());
        }
    }

    // ======================== 优惠券 ========================

    /** GET /api/marketing/coupons — 优惠券列表。 */
    @GetMapping("/coupons")
    public Result<List<Map<String, Object>>> listCoupons(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String couponType,
            @RequestParam(required = false) Integer isActive,
            @RequestParam(required = false) String keyword) {
        try {
            Long effectiveStoreId = resolveQueryStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (effectiveStoreId != null) {
                where.append(" AND store_id = ?");
                params.add(effectiveStoreId);
            }
            if (couponType != null && !couponType.isEmpty()) {
                where.append(" AND coupon_type = ?");
                params.add(couponType);
            }
            if (isActive != null) {
                where.append(" AND is_active = ?");
                params.add(isActive);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                where.append(" AND (coupon_name LIKE ? OR coupon_code LIKE ?)");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw); params.add(kw);
            }
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT * FROM marketing_coupon" + where + " ORDER BY coupon_id DESC",
                    params.toArray());
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取优惠券列表失败: " + e.getMessage());
        }
    }

    /** POST /api/marketing/coupons — 创建优惠券（仅总经理）。 */
    @PostMapping("/coupons")
    @Transactional
    public Result<Map<String, Object>> createCoupon(@RequestBody Map<String, Object> body) {
        try {
            UserContext.assertGeneralManager();
            Long targetStoreId = asLong(body.get("storeId"));
            if (targetStoreId == null) targetStoreId = 1L;
            String couponName = requireString(body, "couponName", "优惠券名称不能为空");
            String couponType = requireString(body, "couponType", "优惠券类型不能为空");
            String couponCode = body.get("couponCode") != null
                    ? body.get("couponCode").toString().trim()
                    : generateCode("CPN", targetStoreId);

            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM marketing_coupon WHERE coupon_code = ? AND store_id = ?",
                    Integer.class, couponCode, targetStoreId);
            if (exists != null && exists > 0) {
                return Result.error(400, "优惠券编码已存在: " + couponCode);
            }

            BigDecimal discountValue = asDecimal(body.get("discountValue"), null);
            BigDecimal minConsume = asDecimal(body.get("minConsume"), BigDecimal.ZERO);
            Integer totalCount = asInt(body.get("totalCount"), 0);
            Integer validDays = asInt(body.get("validDays"), null);
            Date startDate = asSqlDate(body.get("startDate"));
            Date endDate = asSqlDate(body.get("endDate"));
            String applicableType = body.get("applicableType") == null ? "all" : body.get("applicableType").toString();
            String applicableIds = asString(body.get("applicableIds"));
            Integer isActive = body.get("isActive") == null ? 1 : asInt(body.get("isActive"), 1);
            String description = asString(body.get("description"));
            String remark = asString(body.get("remark"));

            String sql = "INSERT INTO marketing_coupon (store_id, coupon_code, coupon_name, coupon_type, " +
                    "discount_value, min_consume, total_count, received_count, used_count, valid_days, " +
                    "start_date, end_date, applicable_type, applicable_ids, is_active, description, remark) " +
                    "VALUES (?,?,?,?,?,?,?,0,0,?,?,?,?,?,?,?,?)";
            jdbc.update(sql, targetStoreId, couponCode, couponName, couponType, discountValue, minConsume,
                    totalCount, validDays, startDate, endDate, applicableType, applicableIds, isActive,
                    description, remark);

            Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            Map<String, Object> saved = jdbc.queryForList(
                    "SELECT * FROM marketing_coupon WHERE coupon_id = ?", newId).get(0);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建优惠券失败: " + e.getMessage());
        }
    }

    // ======================== 折扣规则 ========================

    /** GET /api/marketing/discount-rules — 折扣规则列表。 */
    @GetMapping("/discount-rules")
    public Result<List<Map<String, Object>>> listDiscountRules(
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String ruleType,
            @RequestParam(required = false) Integer isActive) {
        try {
            Long effectiveStoreId = resolveQueryStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (effectiveStoreId != null) {
                where.append(" AND store_id = ?");
                params.add(effectiveStoreId);
            }
            if (ruleType != null && !ruleType.isEmpty()) {
                where.append(" AND rule_type = ?");
                params.add(ruleType);
            }
            if (isActive != null) {
                where.append(" AND is_active = ?");
                params.add(isActive);
            }
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT * FROM marketing_discount_rule" + where + " ORDER BY priority DESC, rule_id DESC",
                    params.toArray());
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取折扣规则失败: " + e.getMessage());
        }
    }

    // ======================== 门店隔离辅助方法 ========================

    /**
     * 查询接口：解析有效门店ID。
     * @return null=总经理且未指定门店（全部）；非null=限制到指定门店
     */
    private Long resolveQueryStoreId(Long requestedStoreId) {
        Long staffId = UserContext.getStaffId();
        if (staffId == null) {
            throw new SecurityException("未登录，无权访问营销数据");
        }
        if (UserContext.isDataScopeAll()) {
            return requestedStoreId;
        }
        Long current = UserContext.currentStoreId();
        if (current == null || current == 0L) {
            return requestedStoreId;
        }
        return current;
    }

    /** 生成唯一编码：前缀 + 门店ID + 时间戳后6位 + 4位随机数。 */
    private String generateCode(String prefix, Long storeId) {
        long sid = storeId == null ? 1L : storeId;
        long ts = System.currentTimeMillis() % 1_000_000;
        int rand = 1000 + new Random().nextInt(9000);
        return prefix + sid + String.format("%06d", ts) + rand;
    }

    // ======================== 类型转换辅助方法 ========================

    private static String requireString(Map<String, Object> body, String key, String errMsg) {
        Object v = body.get(key);
        if (v == null || v.toString().trim().isEmpty()) {
            throw new IllegalArgumentException(errMsg);
        }
        return v.toString().trim();
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer asInt(Object v, Integer def) {
        if (v == null) return def;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return def; }
    }

    private static BigDecimal asDecimal(Object v, BigDecimal def) {
        if (v == null) return def;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return BigDecimal.valueOf(((Number) v).doubleValue());
        try { return new BigDecimal(v.toString()); } catch (NumberFormatException e) { return def; }
    }

    private static Date asSqlDate(Object v) {
        if (v == null) return null;
        if (v instanceof Date) return (Date) v;
        if (v instanceof java.util.Date) return new Date(((java.util.Date) v).getTime());
        String s = v.toString();
        if (s.isEmpty()) return null;
        try { return Date.valueOf(s); } catch (IllegalArgumentException e) { return null; }
    }

    private static void appendUpdate(StringBuilder set, List<Object> params, String column, Object value) {
        if (value == null) return;
        if (set.length() > 0) set.append(", ");
        set.append(column).append(" = ?");
        params.add(value);
    }

    // ============ marketing_share 分享 ============
    @GetMapping("/shares")
    public Result<List<Map<String, Object>>> listShares(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            params.add(limit);
            return Result.success(jdbc.queryForList(
                "SELECT * FROM marketing_share" + where + " ORDER BY share_time DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询分享记录失败: " + e.getMessage());
        }
    }

    // ============ marketing_signin 签到 ============
    @GetMapping("/signins")
    public Result<List<Map<String, Object>>> listSignins(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            params.add(limit);
            return Result.success(jdbc.queryForList(
                "SELECT * FROM marketing_signin" + where + " ORDER BY signin_date DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询签到记录失败: " + e.getMessage());
        }
    }

    @GetMapping("/signin-records")
    public Result<List<Map<String, Object>>> listSigninRecords(
            @RequestParam(required = false) String storeId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            params.add(limit);
            return Result.success(jdbc.queryForList(
                "SELECT * FROM marketing_signin_record" + where + " ORDER BY signin_date DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询签到明细失败: " + e.getMessage());
        }
    }

    // ============ marketing_task 任务 ============
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> listTasks(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String status) {
        try {
            Long sid = resolveStoreId(storeId);
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            if (status != null && !status.isEmpty()) { where.append(" AND status=?"); params.add(status); }
            return Result.success(jdbc.queryForList(
                "SELECT * FROM marketing_task" + where + " ORDER BY created_at DESC", params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询营销任务失败: " + e.getMessage());
        }
    }

    private Long resolveStoreId(String storeId) {
        if (UserContext.isGeneralManager()) {
            if (storeId == null || storeId.isEmpty() || "all".equalsIgnoreCase(storeId)) return null;
            try { return Long.parseLong(storeId); } catch (Exception e) { return null; }
        }
        Long sid = UserContext.currentStoreId();
        return (sid == null || sid == 0L) ? null : sid;
    }
}

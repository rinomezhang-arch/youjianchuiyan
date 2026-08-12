package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

/**
 * 会员管理控制器。
 * <p>
 * 数据隔离规则：
 * <ul>
 *   <li>总经理（store_id = 0）：可查询/操作任意门店会员</li>
 *   <li>店长（store_id &gt; 0）：仅可查询/操作本店会员，store_id 强制覆盖</li>
 * </ul>
 * <p>
 * 业务规则：
 * <ul>
 *   <li>会员数据按门店隔离（store_id 必填）</li>
 *   <li>同一门店内 card_no 唯一</li>
 *   <li>充值操作需记录流水并同步 balance / total_recharge</li>
 * </ul>
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MemberController {

    @Autowired
    private JdbcTemplate jdbc;

    // ======================== 会员卡主档 ========================

    /**
     * GET /api/members — 会员列表（分页，门店过滤）。
     * 总经理可传 storeId 指定门店；店长强制限制为本店。
     */
    @GetMapping("/members")
    public Result<Map<String, Object>> listMembers(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long storeId,
            @RequestParam(required = false) String levelName,
            @RequestParam(required = false) String status) {
        try {
            if (page < 1) page = 1;
            if (size < 1 || size > 200) size = 20;
            Long effectiveStoreId = resolveQueryStoreId(storeId);

            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (effectiveStoreId != null) {
                where.append(" AND store_id = ?");
                params.add(effectiveStoreId);
            }
            if (keyword != null && !keyword.trim().isEmpty()) {
                where.append(" AND (member_name LIKE ? OR phone LIKE ? OR card_no LIKE ?)");
                String kw = "%" + keyword.trim() + "%";
                params.add(kw); params.add(kw); params.add(kw);
            }
            if (levelName != null && !levelName.isEmpty()) {
                where.append(" AND level_name = ?");
                params.add(levelName);
            }
            if (status != null && !status.isEmpty()) {
                where.append(" AND status = ?");
                params.add(status);
            }

            Long total = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM member_card" + where, Long.class, params.toArray());
            int offset = (page - 1) * size;
            List<Object> listParams = new ArrayList<>(params);
            listParams.add(size);
            listParams.add(offset);
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT * FROM member_card" + where + " ORDER BY member_id DESC LIMIT ? OFFSET ?",
                    listParams.toArray());

            Map<String, Object> result = new HashMap<>();
            result.put("list", list);
            result.put("total", total == null ? 0 : total);
            result.put("page", page);
            result.put("size", size);
            return Result.success(result);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取会员列表失败: " + e.getMessage());
        }
    }

    /** GET /api/members/{id} — 会员详情。 */
    @GetMapping("/members/{id}")
    public Result<Map<String, Object>> getMember(@PathVariable Long id) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM member_card WHERE member_id = ? LIMIT 1", id);
            if (rows.isEmpty()) return Result.error(404, "会员不存在");
            Map<String, Object> member = rows.get(0);
            assertMemberStoreAccess(asLong(member.get("store_id")));
            return Result.success(member);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取会员失败: " + e.getMessage());
        }
    }

    /** POST /api/members — 创建会员。 */
    @PostMapping("/members")
    @Transactional
    public Result<Map<String, Object>> createMember(@RequestBody Map<String, Object> body) {
        try {
            Long targetStoreId = resolveWriteStoreId(asLong(body.get("storeId")));
            String memberName = requireString(body, "memberName", "会员姓名不能为空");
            String phone = requireString(body, "phone", "手机号不能为空");
            String cardNo = body.get("cardNo") != null ? body.get("cardNo").toString().trim()
                    : generateCardNo(targetStoreId);

            Integer exists = jdbc.queryForObject(
                    "SELECT COUNT(*) FROM member_card WHERE card_no = ? AND store_id = ?",
                    Integer.class, cardNo, targetStoreId);
            if (exists != null && exists > 0) {
                return Result.error(400, "卡号已存在: " + cardNo);
            }

            Integer levelId = asInt(body.get("levelId"));
            String levelName = asString(body.get("levelName"));
            // 通过 levelId 自动回填 levelName
            if (levelId != null && (levelName == null || levelName.isEmpty())) {
                List<Map<String, Object>> lv = jdbc.queryForList(
                        "SELECT level_name FROM member_level WHERE level_id = ? LIMIT 1", levelId);
                if (!lv.isEmpty()) levelName = (String) lv.get(0).get("level_name");
            }

            String gender = asString(body.get("gender"));
            String idCard = asString(body.get("idCard"));
            Date birthday = asSqlDate(body.get("birthday"));
            BigDecimal balance = asDecimal(body.get("balance"), BigDecimal.ZERO);
            Integer totalPoints = asInt(body.get("totalPoints"), 0);
            String email = asString(body.get("email"));
            String address = asString(body.get("address"));
            String avatarUrl = asString(body.get("avatarUrl"));
            String remark = asString(body.get("remark"));
            String status = body.get("status") == null ? "active" : body.get("status").toString();
            Date registerDate = body.get("registerDate") == null
                    ? Date.valueOf(LocalDate.now()) : asSqlDate(body.get("registerDate"));
            Long referrerId = asLong(body.get("referrerId"));

            String sql = "INSERT INTO member_card (store_id, card_no, member_name, gender, phone, id_card, " +
                    "birthday, level_id, level_name, balance, total_points, total_recharge, total_consume, " +
                    "consume_count, register_date, register_store_id, referrer_id, avatar_url, email, address, " +
                    "status, remark) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
            jdbc.update(sql, targetStoreId, cardNo, memberName, gender, phone, idCard, birthday,
                    levelId, levelName, balance, totalPoints, BigDecimal.ZERO, BigDecimal.ZERO, 0,
                    registerDate, targetStoreId, referrerId, avatarUrl, email, address, status, remark);

            Long newId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            Map<String, Object> saved = jdbc.queryForList(
                    "SELECT * FROM member_card WHERE member_id = ?", newId).get(0);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "创建会员失败: " + e.getMessage());
        }
    }

    /** PUT /api/members/{id} — 修改会员。 */
    @PutMapping("/members/{id}")
    @Transactional
    public Result<Map<String, Object>> updateMember(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM member_card WHERE member_id = ? LIMIT 1", id);
            if (rows.isEmpty()) return Result.error(404, "会员不存在");
            Map<String, Object> existing = rows.get(0);
            assertMemberStoreAccess(asLong(existing.get("store_id")));

            StringBuilder set = new StringBuilder();
            List<Object> params = new ArrayList<>();
            appendUpdate(set, params, "member_name", body.get("memberName"));
            appendUpdate(set, params, "gender", body.get("gender"));
            appendUpdate(set, params, "phone", body.get("phone"));
            appendUpdate(set, params, "id_card", body.get("idCard"));
            appendUpdate(set, params, "birthday", asSqlDate(body.get("birthday")));
            Integer levelId = asInt(body.get("levelId"));
            if (levelId != null) {
                appendUpdate(set, params, "level_id", levelId);
                if (body.get("levelName") != null) {
                    appendUpdate(set, params, "level_name", asString(body.get("levelName")));
                } else {
                    List<Map<String, Object>> lv = jdbc.queryForList(
                            "SELECT level_name FROM member_level WHERE level_id = ? LIMIT 1", levelId);
                    if (!lv.isEmpty()) appendUpdate(set, params, "level_name", lv.get(0).get("level_name"));
                }
            } else if (body.get("levelName") != null) {
                appendUpdate(set, params, "level_name", asString(body.get("levelName")));
            }
            appendUpdate(set, params, "email", body.get("email"));
            appendUpdate(set, params, "address", asString(body.get("address")));
            appendUpdate(set, params, "avatar_url", asString(body.get("avatarUrl")));
            appendUpdate(set, params, "remark", asString(body.get("remark")));
            if (body.get("status") != null) appendUpdate(set, params, "status", body.get("status").toString());
            if (body.get("balance") != null) appendUpdate(set, params, "balance", asDecimal(body.get("balance"), null));
            if (body.get("totalPoints") != null) appendUpdate(set, params, "total_points", asInt(body.get("totalPoints")));

            if (set.length() == 0) return Result.success(existing);
            set.append(" update_time = CURRENT_TIMESTAMP");
            params.add(id);
            jdbc.update("UPDATE member_card SET " + set + " WHERE member_id = ?", params.toArray());
            Map<String, Object> saved = jdbc.queryForList(
                    "SELECT * FROM member_card WHERE member_id = ?", id).get(0);
            return Result.success(saved);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "更新会员失败: " + e.getMessage());
        }
    }

    /** GET /api/members/{id}/consume-records — 消费记录。 */
    @GetMapping("/members/{id}/consume-records")
    public Result<List<Map<String, Object>>> getConsumeRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT store_id FROM member_card WHERE member_id = ? LIMIT 1", id);
            if (rows.isEmpty()) return Result.error(404, "会员不存在");
            assertMemberStoreAccess(asLong(rows.get(0).get("store_id")));
            if (page < 1) page = 1;
            if (size < 1 || size > 200) size = 20;
            int offset = (page - 1) * size;
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT * FROM member_consume_record WHERE member_id = ? ORDER BY consume_id DESC LIMIT ? OFFSET ?",
                    id, size, offset);
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取消费记录失败: " + e.getMessage());
        }
    }

    /** GET /api/members/{id}/points — 积分记录。 */
    @GetMapping("/members/{id}/points")
    public Result<List<Map<String, Object>>> getPointLogs(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT store_id FROM member_card WHERE member_id = ? LIMIT 1", id);
            if (rows.isEmpty()) return Result.error(404, "会员不存在");
            assertMemberStoreAccess(asLong(rows.get(0).get("store_id")));
            if (page < 1) page = 1;
            if (size < 1 || size > 200) size = 20;
            int offset = (page - 1) * size;
            List<Map<String, Object>> list = jdbc.queryForList(
                    "SELECT * FROM member_point_log WHERE member_id = ? ORDER BY log_id DESC LIMIT ? OFFSET ?",
                    id, size, offset);
            return Result.success(list);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取积分记录失败: " + e.getMessage());
        }
    }

    /**
     * POST /api/members/{id}/recharge — 会员充值。
     * <p>
     * 请求体字段：
     * <ul>
     *   <li>rechargeAmount — 充值金额（必填，&gt;0）</li>
     *   <li>giftAmount     — 赠送金额（可选，默认 0）</li>
     *   <li>paymentMethod  — 支付方式（cash/wechat/alipay/bank/card，可选）</li>
     *   <li>rechargeType   — 充值类型（normal/promo，默认 normal）</li>
     *   <li>activityId     — 关联活动ID（可选）</li>
     *   <li>remark         — 备注（可选）</li>
     * </ul>
     */
    @PostMapping("/members/{id}/recharge")
    @Transactional
    public Result<Map<String, Object>> recharge(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT * FROM member_card WHERE member_id = ? LIMIT 1", id);
            if (rows.isEmpty()) return Result.error(404, "会员不存在");
            Map<String, Object> member = rows.get(0);
            assertMemberStoreAccess(asLong(member.get("store_id")));

            BigDecimal rechargeAmount = asDecimal(body.get("rechargeAmount"), null);
            if (rechargeAmount == null || rechargeAmount.compareTo(BigDecimal.ZERO) <= 0) {
                return Result.error(400, "充值金额必须大于0");
            }
            BigDecimal giftAmount = asDecimal(body.get("giftAmount"), BigDecimal.ZERO);
            if (giftAmount == null) giftAmount = BigDecimal.ZERO;
            BigDecimal totalAmount = rechargeAmount.add(giftAmount);
            BigDecimal balanceBefore = asDecimal(member.get("balance"), BigDecimal.ZERO);
            BigDecimal balanceAfter = balanceBefore.add(totalAmount);

            Long storeId = asLong(member.get("store_id"));
            String cardNo = (String) member.get("card_no");
            String memberName = (String) member.get("member_name");
            String rechargeNo = "RC" + System.currentTimeMillis() + new Random().nextInt(1000);
            Date rechargeDate = Date.valueOf(LocalDate.now());
            String paymentMethod = body.get("paymentMethod") == null ? null : body.get("paymentMethod").toString();
            String rechargeType = body.get("rechargeType") == null ? "normal" : body.get("rechargeType").toString();
            Long activityId = asLong(body.get("activityId"));
            String remark = asString(body.get("remark"));
            Long operatorId = UserContext.getStaffId();
            String operatorName = UserContext.getUsername();

            jdbc.update("INSERT INTO member_recharge_record (store_id, recharge_no, member_id, card_no, member_name, " +
                            "recharge_date, recharge_amount, gift_amount, total_amount, balance_before, balance_after, " +
                            "payment_method, recharge_type, activity_id, operator_id, operator_name, remark) " +
                            "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    storeId, rechargeNo, id, cardNo, memberName, rechargeDate, rechargeAmount, giftAmount,
                    totalAmount, balanceBefore, balanceAfter, paymentMethod, rechargeType, activityId,
                    operatorId, operatorName, remark);

            jdbc.update("UPDATE member_card SET balance = ?, total_recharge = total_recharge + ?, " +
                            "update_time = CURRENT_TIMESTAMP WHERE member_id = ?",
                    balanceAfter, totalAmount, id);

            Map<String, Object> result = new HashMap<>();
            result.put("rechargeNo", rechargeNo);
            result.put("balanceBefore", balanceBefore);
            result.put("balanceAfter", balanceAfter);
            result.put("rechargeAmount", rechargeAmount);
            result.put("giftAmount", giftAmount);
            result.put("totalAmount", totalAmount);
            return Result.success(result);
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            return Result.error(400, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "会员充值失败: " + e.getMessage());
        }
    }

    /** GET /api/member-levels — 会员等级列表。 */
    @GetMapping("/member-levels")
    public Result<List<Map<String, Object>>> listMemberLevels(
            @RequestParam(required = false) Long storeId) {
        try {
            Long effectiveStoreId = resolveQueryStoreId(storeId);
            if (effectiveStoreId != null) {
                return Result.success(jdbc.queryForList(
                        "SELECT * FROM member_level WHERE store_id = ? ORDER BY sort_order, level_id",
                        effectiveStoreId));
            }
            return Result.success(jdbc.queryForList(
                    "SELECT * FROM member_level ORDER BY store_id, sort_order, level_id"));
        } catch (SecurityException e) {
            return Result.error(403, e.getMessage());
        } catch (Exception e) {
            return Result.error(500, "获取会员等级失败: " + e.getMessage());
        }
    }

    /** POST /api/member-levels — 创建会员等级。 */
    @PostMapping("/member-levels")
    @Transactional
    public Result<Map<String, Object>> createMemberLevel(@RequestBody Map<String, Object> body) {
        try {
            Long currentStoreId = UserContext.ensureDataScopeFromStoreId();
            if (!UserContext.isGeneralManager() && currentStoreId == null) {
                return Result.error(403, "未识别到门店");
            }
            long id = System.currentTimeMillis();
            String code = (String) body.getOrDefault("levelCode", "L" + id);
            String name = (String) body.getOrDefault("levelName", "默认等级");
            long minPoints = body.get("minPoints") != null ? Long.parseLong(body.get("minPoints").toString()) : 0;
            double discount = body.get("discountRate") != null ? Double.parseDouble(body.get("discountRate").toString()) : 1.0;
            long storeId = currentStoreId == null ? 1L : currentStoreId;
            jdbc.update("INSERT INTO member_level (level_id, store_id, level_code, level_name, min_points, discount_rate, sort_order, create_time) VALUES (?,?,?,?,?,?,?,NOW())",
                id, storeId, code, name, minPoints, discount, 0);
            return Result.success(Map.of("levelId", id));
        } catch (Exception e) {
            e.printStackTrace();
            return Result.error(500, "创建会员等级失败: " + e.getMessage());
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
            throw new SecurityException("未登录，无权访问会员数据");
        }
        if (UserContext.isDataScopeAll()) {
            return requestedStoreId;
        }
        Long current = UserContext.currentStoreId();
        if (current == null || current == 0L) {
            // 兜底：写操作时 DATA_SCOPE_ALL 未设置，但 storeId=0
            return requestedStoreId;
        }
        return current;
    }

    /**
     * 写操作：解析目标门店ID。
     * @return 店长返回本店；总经理返回 requestedStoreId 或默认 1
     */
    private Long resolveWriteStoreId(Long requestedStoreId) {
        UserContext.ensureDataScopeFromStoreId();
        if (UserContext.isGeneralManager()) {
            return requestedStoreId != null ? requestedStoreId : 1L;
        }
        Long current = UserContext.currentStoreId();
        if (current == null) {
            throw new SecurityException("未登录，无权操作会员数据");
        }
        return current;
    }

    /** 校验当前用户可访问指定门店的会员数据。 */
    private void assertMemberStoreAccess(Long memberStoreId) {
        if (memberStoreId == null) return;
        UserContext.ensureDataScopeFromStoreId();
        if (UserContext.isGeneralManager()) return;
        Long current = UserContext.currentStoreId();
        if (current == null || !current.equals(memberStoreId)) {
            throw new SecurityException("无权访问非本店会员数据");
        }
    }

    /** 生成唯一卡号：M + 门店ID + 时间戳后6位 + 4位随机数。 */
    private String generateCardNo(Long storeId) {
        long sid = storeId == null ? 1L : storeId;
        long ts = System.currentTimeMillis() % 1_000_000;
        int rand = 1000 + new Random().nextInt(9000);
        return "M" + sid + String.format("%06d", ts) + rand;
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

    private static Integer asInt(Object v) {
        return asInt(v, null);
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

    // ============ member_master 会员档案 ============
    @GetMapping("/members/master")
    public Result<List<Map<String, Object>>> listMemberMaster(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            if (level != null && !level.isEmpty()) { where.append(" AND level=?"); params.add(level); }
            params.add(limit);
            return Result.success(jdbc.queryForList(
                "SELECT * FROM member_master" + where + " ORDER BY created_at DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询会员档案失败: " + e.getMessage());
        }
    }

    @GetMapping("/members/master/{id}")
    public Result<Map<String, Object>> getMemberMaster(@PathVariable Long id) {
        try {
            List<Map<String, Object>> list = jdbc.queryForList(
                "SELECT * FROM member_master WHERE id=? LIMIT 1", id);
            if (list.isEmpty()) return Result.error(404, "会员不存在");
            Map<String, Object> data = new LinkedHashMap<>(list.get(0));
            // 关联余额变动记录
            data.put("balanceLogs", jdbc.queryForList(
                "SELECT * FROM member_balance WHERE member_id=? ORDER BY created_at DESC LIMIT 20", id));
            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "查询会员详情失败: " + e.getMessage());
        }
    }

    // ============ member_balance 余额变动 ============
    @GetMapping("/members/balance")
    public Result<List<Map<String, Object>>> listMemberBalance(
            @RequestParam(required = false) String storeId,
            @RequestParam(required = false) Long memberId,
            @RequestParam(defaultValue = "50") int limit) {
        try {
            Long sid = resolveStoreId(storeId);
            if (limit < 1 || limit > 200) limit = 50;
            StringBuilder where = new StringBuilder(" WHERE 1=1");
            List<Object> params = new ArrayList<>();
            if (sid != null) { where.append(" AND store_id=?"); params.add(sid); }
            if (memberId != null) { where.append(" AND member_id=?"); params.add(memberId); }
            params.add(limit);
            return Result.success(jdbc.queryForList(
                "SELECT * FROM member_balance" + where + " ORDER BY created_at DESC LIMIT ?", params.toArray()));
        } catch (Exception e) {
            return Result.error(500, "查询余额变动失败: " + e.getMessage());
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

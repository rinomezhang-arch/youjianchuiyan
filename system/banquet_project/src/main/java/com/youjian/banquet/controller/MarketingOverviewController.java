package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.util.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * 营销会员总览聚合接口。
 * <p>
 * 数据来源说明（实时聚合，无硬编码占位）：
 * <ul>
 *   <li>会员指标 → customer_master（新增会员、本月累计、会员分层）</li>
 *   <li>宴会订单 → booking_master（本月订单数）</li>
 *   <li>储值金额 → 后端暂无储值表，固定返回 0 + unopened=true</li>
 *   <li>线上团购核销 → 后端暂无该表，固定返回 0 + unopened=true</li>
 *   <li>进行中活动 / 线上平台数据 → 后端暂无对应表，固定返回空数组 + unopened=true</li>
 * </ul>
 * 字段命名统一 snake_case（与 iPad 接口、数据库列名一致）。
 */
@RestController
@RequestMapping("/api/marketing")
@CrossOrigin(origins = "*")
public class MarketingOverviewController {

    @Autowired
    private JdbcTemplate jdbc;

    /**
     * 营销会员总览聚合数据。
     *
     * @param storeId 门店ID，店长强制覆盖为本人门店；总经理可传入任意门店
     */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "1") Long storeId) {
        try {
            // 门店数据隔离：店长仅可查本店
            storeId = resolveStoreId(storeId);

            Map<String, Object> data = new LinkedHashMap<>();

            // 1) 新增会员（本月累计 = 当月新建的会员数；总会员 = is_active=1 的全部）
            LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
            LocalDateTime monthStartTime = monthStart.atStartOfDay();
            Long newMembersThisMonth = countNewMembers(storeId, monthStartTime);
            Long totalActiveMembers = countActiveMembers(storeId);
            data.put("new_members", newMembersThisMonth);
            data.put("total_active_members", totalActiveMembers);
            data.put("new_members_sub_text", "本月累计 " + newMembersThisMonth);

            // 2) 储值总金额（后端无表）
            Map<String, Object> recharge = new LinkedHashMap<>();
            recharge.put("amount", "¥0");
            recharge.put("sub_text", "储值功能未开通");
            recharge.put("unopened", true);
            data.put("recharge", recharge);

            // 3) 线上团购核销（后端无表）
            Map<String, Object> groupBuy = new LinkedHashMap<>();
            groupBuy.put("count", 0);
            groupBuy.put("sub_text", "团购核销数据未接入");
            groupBuy.put("unopened", true);
            data.put("online_group_buy", groupBuy);

            // 4) 宴会订单数（本月）
            Long banquetOrders = countBanquetOrdersThisMonth(storeId, monthStart);
            Map<String, Object> banquet = new LinkedHashMap<>();
            banquet.put("count", banquetOrders);
            banquet.put("sub_text", "本月预定");
            banquet.put("unopened", false);
            data.put("banquet_orders", banquet);

            // 5) 会员分层视图（按 member_level 聚合）
            data.put("member_tiers", buildMemberTiers(storeId));

            // 6) 进行中活动（后端无表）
            List<Map<String, Object>> activities = new ArrayList<>();
            Map<String, Object> activityEmpty = new LinkedHashMap<>();
            activityEmpty.put("title", "暂无进行中活动");
            activityEmpty.put("tag", "待配置");
            activityEmpty.put("type", "empty");
            activityEmpty.put("date", "营销活动功能未上线");
            activities.add(activityEmpty);
            data.put("activities", activities);
            data.put("activities_unopened", true);

            // 7) 线上平台数据（后端无表）
            List<Map<String, Object>> platforms = new ArrayList<>();
            Map<String, Object> platformEmpty = new LinkedHashMap<>();
            platformEmpty.put("name", "线上平台数据");
            platformEmpty.put("icon", "—");
            platformEmpty.put("value", "美团/抖音/点评/小红书数据未接入");
            platforms.add(platformEmpty);
            data.put("platforms", platforms);
            data.put("platforms_unopened", true);

            data.put("store_id", storeId);
            data.put("generated_at", LocalDateTime.now().toString());

            return Result.success(data);
        } catch (Exception e) {
            return Result.error(500, "获取营销总览失败: " + e.getMessage());
        }
    }

    // ============ 私有方法 ============

    private Long resolveStoreId(Long requestStoreId) {
        Long currentStoreId = UserContext.getCurrentStoreId();
        if (!UserContext.isDataScopeAll() && currentStoreId != null) {
            return currentStoreId;
        }
        return requestStoreId;
    }

    private Long countNewMembers(Long storeId, LocalDateTime since) {
        try {
            String sql = "SELECT COUNT(*) FROM customer_master WHERE store_id = ? AND is_active = 1 AND create_time >= ?";
            Long n = jdbc.queryForObject(sql, Long.class, storeId, since);
            return n == null ? 0L : n;
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long countActiveMembers(Long storeId) {
        try {
            String sql = "SELECT COUNT(*) FROM customer_master WHERE store_id = ? AND is_active = 1";
            Long n = jdbc.queryForObject(sql, Long.class, storeId);
            return n == null ? 0L : n;
        } catch (Exception e) {
            return 0L;
        }
    }

    private Long countBanquetOrdersThisMonth(Long storeId, LocalDate sinceDate) {
        try {
            String sql = "SELECT COUNT(*) FROM booking_master WHERE store_id = ? AND booking_date >= ?";
            Long n = jdbc.queryForObject(sql, Long.class, storeId, sinceDate);
            return n == null ? 0L : n;
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 按 member_level 聚合真实数据。固定输出 4 个分层（普通/银卡/金卡/钻石），
     * 数据库里没有的等级 count=0、spending=¥0、avg_spend=¥0。
     */
    private List<Map<String, Object>> buildMemberTiers(Long storeId) {
        // 真实聚合
        Map<String, long[]> tierAgg = new LinkedHashMap<>(); // key=level, value=[count, sumAmount(cents)]
        try {
            String sql = "SELECT IFNULL(NULLIF(member_level, ''), '普通会员') AS lvl, " +
                    "COUNT(*) AS cnt, IFNULL(SUM(total_amount), 0) AS amt " +
                    "FROM customer_master WHERE store_id = ? AND is_active = 1 " +
                    "GROUP BY lvl";
            jdbc.query(sql, rs -> {
                String lvl = rs.getString("lvl");
                long cnt = rs.getLong("cnt");
                BigDecimal amt = rs.getBigDecimal("amt");
                long amtCents = amt == null ? 0L : amt.multiply(BigDecimal.valueOf(100)).longValue();
                tierAgg.put(lvl == null ? "普通会员" : lvl, new long[]{cnt, amtCents});
            }, storeId);
        } catch (Exception e) {
            // 聚合失败不影响整体响应
        }

        String[][] tierConfig = {
                {"普通会员", "普", "rgba(149,165,166,0.10)"},
                {"银卡会员", "银", "rgba(149,165,166,0.15)"},
                {"VIP会员", "VIP", "rgba(196,163,90,0.15)"},
                {"钻石大客户", "钻", "rgba(45,74,62,0.15)"}
        };

        List<Map<String, Object>> result = new ArrayList<>();
        for (String[] cfg : tierConfig) {
            String name = cfg[0];
            String icon = cfg[1];
            String bg = cfg[2];
            long[] agg = tierAgg.getOrDefault(name, new long[]{0L, 0L});
            long count = agg[0];
            long amountCents = agg[1];
            String spending = formatYuan(amountCents);
            String avg = count > 0 ? formatYuan(amountCents / count) : "¥0";

            Map<String, Object> tier = new LinkedHashMap<>();
            tier.put("name", name);
            tier.put("icon", icon);
            tier.put("count", count);
            tier.put("spending", spending);
            tier.put("avg_spend", avg);
            tier.put("bg_color", bg);
            result.add(tier);
        }
        return result;
    }

    private static String formatYuan(long cents) {
        if (cents <= 0) return "¥0";
        long yuan = cents / 100;
        return "¥" + String.format("%,d", yuan);
    }
}

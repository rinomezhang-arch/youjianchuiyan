package com.youjian.banquet.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营销客人 H5 公开读取与浏览归因服务（TR-MARKETING-PUBLIC-API-54）。
 * <p>
 * 只做公开读取（活动列表 / slug 快照）+ view 浏览事件这一条竖切；不扩内部状态机或咨询。
 * 公开响应白名单字段（不含预算/成本/操作人/审批备注/内部规则）：
 * publicationId, storeId, storeName, storeAddress, storePhone, sourceCode,
 * publicTitle, publicSummary, contentJson, heroAssetUrl, ctaLabel, status, validFrom, validTo。
 */
@Service
public class PublicMarketingService {

    private final JdbcTemplate jdbc;

    public PublicMarketingService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** 公开快照白名单列（列别名即公开 JSON 字段名，杜绝 SELECT * 泄露内部字段）。 */
    private static final String SNAPSHOT_COLUMNS =
            "p.publication_id AS publicationId, p.store_id AS storeId, " +
            "s.store_name AS storeName, s.address AS storeAddress, s.phone AS storePhone, " +
            "p.source_code AS sourceCode, p.title AS publicTitle, p.summary AS publicSummary, " +
            "p.content_json AS contentJson, p.hero_asset_url AS heroAssetUrl, p.cta_label AS ctaLabel, " +
            "p.status AS status, p.valid_from AS validFrom, p.valid_to AS validTo";

    /** 公开可见唯一权威：status=published 且当前时间落在 [valid_from, valid_to]（两端可空=不限）。 */
    private static final String VISIBLE_WHERE =
            "p.status = 'published' " +
            "AND (p.valid_from IS NULL OR p.valid_from <= NOW()) " +
            "AND (p.valid_to IS NULL OR p.valid_to >= NOW())";

    /**
     * GET /api/public/marketing/activities?storeId=... 公开活动列表。
     * 仅合法正门店；只返回可见快照。
     */
    public List<Map<String, Object>> listPublicActivities(Long storeId) {
        if (storeId == null || storeId <= 0) {
            throw new IllegalArgumentException("storeId 必须为正门店 ID");
        }
        String sql = "SELECT " + SNAPSHOT_COLUMNS + " FROM marketing_publication p " +
                "JOIN store_info s ON p.store_id = s.store_id " +
                "WHERE " + VISIBLE_WHERE + " AND p.store_id = ? " +
                "ORDER BY p.publication_id DESC";
        return jdbc.queryForList(sql, storeId);
    }

    /**
     * GET /api/public/marketing/a/{publicSlug} 单个公开快照。
     * 草稿/待批/暂停/过期/未来生效/未知 slug/错误门店关系统一不可见：返回 null（控制器统一 404）。
     */
    public Map<String, Object> getPublicActivityBySlug(String publicSlug) {
        if (publicSlug == null || publicSlug.trim().isEmpty()) {
            return null;
        }
        String sql = "SELECT " + SNAPSHOT_COLUMNS + " FROM marketing_publication p " +
                "JOIN store_info s ON p.store_id = s.store_id " +
                "WHERE " + VISIBLE_WHERE + " AND p.public_slug = ? LIMIT 1";
        List<Map<String, Object>> rows = jdbc.queryForList(sql, publicSlug.trim());
        return rows.isEmpty() ? null : rows.get(0);
    }

    /**
     * POST /api/public/marketing/events 脱敏浏览事件（首版仅 view）。
     * <p>
     * 规则：
     * <ul>
     *   <li>eventType 仅接受 view；</li>
     *   <li>sourceCode 反查当前可见发布取得 publicationId + storeId，不信客户端 storeId；</li>
     *   <li>客户端给 publicationId 时必须与反查一致，否则零新增；</li>
     *   <li>visitorKey 允许空或 64 位小写十六进制哈希；</li>
     *   <li>requestId 必填且 &lt;=64 字符；同 requestId 重试返回同一 eventId 且仅一行，冲突载荷拒绝零新增；</li>
     *   <li>归因与发布同店（storeId 取反查发布的门店）。</li>
     * </ul>
     */
    public Map<String, Object> recordViewEvent(Map<String, Object> body) {
        // 1) eventType 仅 view
        String eventType = asString(body.get("eventType"));
        if (!"view".equals(eventType)) {
            throw new IllegalArgumentException("首版仅接受 eventType=view");
        }
        // 2) requestId 必填且 <=64
        String requestId = asString(body.get("requestId"));
        if (requestId == null || requestId.trim().isEmpty() || requestId.length() > 64) {
            throw new IllegalArgumentException("requestId 必填且不超过 64 字符");
        }
        requestId = requestId.trim();
        // 3) visitorKey 空或 64 位小写十六进制
        String visitorKey = asString(body.get("visitorKey"));
        if (visitorKey != null && !visitorKey.isEmpty()) {
            if (!visitorKey.matches("^[0-9a-f]{64}$")) {
                throw new IllegalArgumentException("visitorKey 必须为空或 64 位小写十六进制哈希");
            }
        } else {
            visitorKey = null;
        }
        // 4) sourceCode 反查当前可见发布
        String sourceCode = asString(body.get("sourceCode"));
        if (sourceCode == null || sourceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceCode 必填");
        }
        sourceCode = sourceCode.trim();
        Map<String, Object> pub = queryVisiblePublicationBySourceCode(sourceCode);
        if (pub == null) {
            throw new IllegalArgumentException("sourceCode 无效或已不可见");
        }
        Long publicationId = asLong(pub.get("publication_id"));
        Long storeId = asLong(pub.get("store_id"));
        // 5) 客户端 publicationId 一致（给了必须一致，否则零新增）
        if (body.get("publicationId") != null) {
            Long clientPubId = asLong(body.get("publicationId"));
            if (clientPubId == null || !clientPubId.equals(publicationId)) {
                throw new IllegalStateException("publicationId 与 sourceCode 反查不一致");
            }
        }
        // 6) 幂等：同 requestId 已存在则返回同一 eventId；冲突载荷拒绝
        List<Map<String, Object>> existing = jdbc.queryForList(
                "SELECT event_id, publication_id, store_id, source_code, event_type, visitor_key " +
                "FROM marketing_attribution_event WHERE request_id = ? LIMIT 1", requestId);
        if (!existing.isEmpty()) {
            Map<String, Object> e = existing.get(0);
            boolean same = eq(e.get("publication_id"), publicationId)
                    && eq(e.get("store_id"), storeId)
                    && eq(e.get("source_code"), sourceCode)
                    && "view".equals(asString(e.get("event_type")))
                    && eq(e.get("visitor_key"), visitorKey);
            if (!same) {
                throw new IllegalStateException("requestId 冲突载荷，拒绝零新增");
            }
            return eventResult(asLong(e.get("event_id")), publicationId, storeId);
        }
        // 7) 插入（归因与发布同店）；eventId 用唯一 request_id 回查，避免非池化连接 LAST_INSERT_ID 失效
        try {
            jdbc.update(
                    "INSERT INTO marketing_attribution_event " +
                    "(publication_id, store_id, source_code, event_type, visitor_key, request_id, occurred_at) " +
                    "VALUES (?,?,?,?,?,?,NOW())",
                    publicationId, storeId, sourceCode, "view", visitorKey, requestId);
        } catch (DuplicateKeyException e) {
            // 并发重试：唯一键兜底，重新查询返回同一 eventId
            Map<String, Object> e2 = jdbc.queryForMap(
                    "SELECT event_id FROM marketing_attribution_event WHERE request_id = ? LIMIT 1", requestId);
            return eventResult(asLong(e2.get("event_id")), publicationId, storeId);
        }
        Map<String, Object> inserted = jdbc.queryForMap(
                "SELECT event_id FROM marketing_attribution_event WHERE request_id = ? LIMIT 1", requestId);
        return eventResult(asLong(inserted.get("event_id")), publicationId, storeId);
    }

    private Map<String, Object> queryVisiblePublicationBySourceCode(String sourceCode) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT publication_id, store_id, source_code FROM marketing_publication p " +
                "WHERE p.source_code = ? AND " + VISIBLE_WHERE + " LIMIT 1", sourceCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Map<String, Object> eventResult(Long eventId, Long publicationId, Long storeId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("eventId", eventId);
        m.put("publicationId", publicationId);
        m.put("storeId", storeId);
        return m;
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return String.valueOf(a).equals(String.valueOf(b));
    }
}

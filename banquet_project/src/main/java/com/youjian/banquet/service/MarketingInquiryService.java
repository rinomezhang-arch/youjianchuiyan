package com.youjian.banquet.service;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 营销 H5 咨询幂等落盘与客人回查（TR-MARKETING-INQUIRY-API-55）。
 * <p>
 * 只做 sourceCode 营销咨询 + inquiryNo/phone 双因子回查这一条竖切；不扩内部状态机。
 * <p>
 * 并发安全：以 {@code marketing_attribution_event.request_id} 唯一键为最终闸门——
 * 事务内先插咨询再插事件，事件插入遇唯一键冲突即回滚整个事务并回查完整载荷比对，
 * 并发同 requestId 同载荷只产生一条咨询 + 一条事件且返回同一回执；冲突载荷 409 零新增。
 */
@Service
public class MarketingInquiryService {

    private final JdbcTemplate jdbc;
    private final TransactionTemplate txTemplate;

    public MarketingInquiryService(JdbcTemplate jdbc, PlatformTransactionManager txManager) {
        this.jdbc = jdbc;
        this.txTemplate = new TransactionTemplate(txManager);
    }

    /** 公开可见唯一权威（与 PublicMarketingService 同口径）。 */
    private static final String VISIBLE_WHERE =
            "p.status = 'published' " +
            "AND (p.valid_from IS NULL OR p.valid_from <= NOW()) " +
            "AND (p.valid_to IS NULL OR p.valid_to >= NOW())";

    // ==================== 提交（sourceCode 营销咨询） ====================

    /**
     * POST /api/public/booking-inquiry（带 sourceCode）。
     * <p>
     * 只接受 customerName/phone/expectedDate/partySize/remark/sourceCode/requestId；
     * 服务端反查当前可见发布取得 publicationId/storeId/channel，忽略客户端 storeId（不默认门店 1）。
     */
    public Map<String, Object> submit(Map<String, Object> body) {
        // 1) requestId 必填且 <=64
        String requestIdRaw = asString(body.get("requestId"));
        if (requestIdRaw == null || requestIdRaw.trim().isEmpty() || requestIdRaw.length() > 64) {
            throw new IllegalArgumentException("requestId 必填且不超过 64 字符");
        }
        final String requestId = requestIdRaw.trim();
        // 2) sourceCode
        String sourceCodeRaw = asString(body.get("sourceCode"));
        if (sourceCodeRaw == null || sourceCodeRaw.trim().isEmpty()) {
            throw new IllegalArgumentException("sourceCode 必填");
        }
        final String sourceCode = sourceCodeRaw.trim();
        // 3) 反查当前可见发布
        Map<String, Object> pub = queryVisiblePublication(sourceCode);
        if (pub == null) {
            throw new IllegalArgumentException("sourceCode 无效或已不可见");
        }
        Long publicationId = asLong(pub.get("publication_id"));
        Long storeId = asLong(pub.get("store_id"));
        String channel = asString(pub.get("channel"));
        // 4) 姓名
        String name = asString(body.get("customerName"));
        if (name == null || name.isBlank()) throw new IllegalArgumentException("姓名不能为空");
        // 5) 手机号
        String phone = asString(body.get("phone"));
        if (phone == null || !phone.matches("^1[3-9]\\d{9}$")) throw new IllegalArgumentException("请填写正确的11位手机号");
        // 6) 日期
        String dateStr = asString(body.get("expectedDate"));
        if (dateStr == null || dateStr.isBlank()) throw new IllegalArgumentException("请选择期望日期");
        LocalDate date;
        try {
            date = LocalDate.parse(dateStr.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("期望日期格式不正确，应为 yyyy-MM-dd");
        }
        if (date.isBefore(LocalDate.now())) throw new IllegalArgumentException("期望日期不能早于今天，请重新选择");
        // 7) 人数
        Integer partySize = asInt(body.get("partySize"));
        if (partySize == null || partySize < 1 || partySize > 99) throw new IllegalArgumentException("人数为 1-99 的整数");
        // 8) remark
        String remark = asString(body.get("remark"));

        try {
            return txTemplate.execute(status -> insertInquiryAndEvent(
                    publicationId, storeId, channel, sourceCode, requestId, name, phone, date, partySize, remark));
        } catch (ReplayDetectedException e) {
            return resolveReplay(requestId, publicationId, storeId, sourceCode, name, phone, date, partySize, remark);
        }
    }

    /** 事务内：先插咨询，再插事件（request_id 唯一键为最终闸门）；冲突即回滚并抛 ReplayDetected。 */
    private Map<String, Object> insertInquiryAndEvent(Long publicationId, Long storeId, String channel,
            String sourceCode, String requestId, String name, String phone, LocalDate date, Integer partySize, String remark) {
        try {
            jdbc.update("INSERT INTO booking_inquiry (store_id, customer_name, customer_phone, preferred_date, "
                    + "guest_count, remark, status, marketing_publication_id, source_code, source_channel, created_at) "
                    + "VALUES (?,?,?,?,?,?, 'pending', ?,?,?, NOW())",
                    storeId, name, phone, Date.valueOf(date), partySize, remark, publicationId, sourceCode, channel);
            Long inquiryId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            String inquiryNo = "INQ" + inquiryId;
            jdbc.update("INSERT INTO marketing_attribution_event (publication_id, store_id, source_code, event_type, "
                    + "business_type, business_id, business_no, request_id, occurred_at) "
                    + "VALUES (?,?,?, 'inquiry', 'booking_inquiry', ?, ?, ?, NOW())",
                    publicationId, storeId, sourceCode, inquiryId, inquiryNo, requestId);
            return receipt(inquiryId, inquiryNo);
        } catch (DuplicateKeyException e) {
            throw new ReplayDetectedException(requestId);
        }
    }

    /** 回查已存在事件并做完整载荷比对；一致返回原回执，不一致 409。 */
    private Map<String, Object> resolveReplay(String requestId, Long publicationId, Long storeId,
            String sourceCode, String name, String phone, LocalDate date, Integer partySize, String remark) {
        List<Map<String, Object>> evts = jdbc.queryForList(
                "SELECT business_id, publication_id, store_id, source_code, event_type, business_type, business_no "
                + "FROM marketing_attribution_event WHERE request_id = ? LIMIT 1", requestId);
        if (evts.isEmpty()) {
            throw new IllegalStateException("requestId 冲突，回查无结果");
        }
        Map<String, Object> evt = evts.get(0);
        Long businessId = asLong(evt.get("business_id"));
        String expectedBusinessNo = "INQ" + businessId;
        // 事件载荷比对：publication/store/sourceCode/eventType 加两条固定业务语义
        // business_type 必须是 booking_inquiry，business_no 必须是 INQ+本咨询 id，
        // 防止同 requestId 的事件指向另一类业务或另一条业务结果时被误判为重放。
        boolean evtSame = eq(evt.get("publication_id"), publicationId)
                && eq(evt.get("store_id"), storeId)
                && eq(evt.get("source_code"), sourceCode)
                && "inquiry".equals(asString(evt.get("event_type")))
                && "booking_inquiry".equals(asString(evt.get("business_type")))
                && expectedBusinessNo.equals(asString(evt.get("business_no")));
        // 咨询载荷比对（姓名/手机号/日期/人数/备注）；备注也是客人提交载荷的一部分，
        // 同一个 requestId 只改备注必须按不同载荷拒绝，不能返回原回执。
        List<Map<String, Object>> inqs = jdbc.queryForList(
                "SELECT customer_name, customer_phone, preferred_date, guest_count, remark "
                + "FROM booking_inquiry WHERE id = ? LIMIT 1", businessId);
        boolean inquirySame = false;
        if (!inqs.isEmpty()) {
            Map<String, Object> inq = inqs.get(0);
            inquirySame = eq(inq.get("customer_name"), name)
                    && eq(inq.get("customer_phone"), phone)
                    && eq(inq.get("preferred_date"), Date.valueOf(date))
                    && eqInt(inq.get("guest_count"), partySize)
                    && eq(inq.get("remark"), remark);
        }
        if (!evtSame || !inquirySame) {
            throw new IllegalStateException("requestId 冲突载荷，拒绝零新增");
        }
        return receipt(businessId, expectedBusinessNo);
    }

    // ==================== 回查（inquiryNo + phone 双因子） ====================

    /**
     * POST /api/public/booking-inquiry/lookup。
     * <p>
     * 必须同时提供 inquiryNo 与 phone；正确组合只返回 inquiryNo/status/expectedDate/partySize/createdAt
     * （已转换时加 bookingId）；查无/电话不符/非法输入统一 success(null)；禁止按手机号列举。
     */
    public Map<String, Object> lookup(Map<String, Object> body) {
        String inquiryNo = asString(body.get("inquiryNo"));
        String phone = asString(body.get("phone"));
        if (inquiryNo == null || phone == null
                || !inquiryNo.startsWith("INQ") || inquiryNo.length() > 24
                || !phone.matches("^1[3-9]\\d{9}$")) {
            return null;
        }
        Long id;
        try {
            id = Long.parseLong(inquiryNo.substring(3));
        } catch (NumberFormatException e) {
            return null;
        }
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT id, status, preferred_date, guest_count, created_at, booking_id "
                + "FROM booking_inquiry WHERE id = ? AND customer_phone = ? LIMIT 1", id, phone);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> row = rows.get(0);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("inquiryNo", "INQ" + row.get("id"));
        out.put("status", row.get("status"));
        out.put("expectedDate", row.get("preferred_date"));
        out.put("partySize", row.get("guest_count"));
        out.put("createdAt", row.get("created_at"));
        if (row.get("booking_id") != null) {
            out.put("bookingId", row.get("booking_id"));
        }
        return out;
    }

    // ==================== 辅助 ====================

    private Map<String, Object> queryVisiblePublication(String sourceCode) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT publication_id, store_id, source_code, channel FROM marketing_publication p "
                + "WHERE p.source_code = ? AND " + VISIBLE_WHERE + " LIMIT 1", sourceCode);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static Map<String, Object> receipt(Long id, String inquiryNo) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("inquiryNo", inquiryNo);
        m.put("lookupUrl", "/h5/inquiry/" + inquiryNo);
        return m;
    }

    private static String asString(Object v) {
        if (v == null) return null;
        String s = v.toString().trim();
        return s.isEmpty() ? null : s;
    }

    private static Long asLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static Integer asInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private static boolean eq(Object a, Object b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return String.valueOf(a).equals(String.valueOf(b));
    }

    private static boolean eqInt(Object a, Integer b) {
        if (a == null) return b == null;
        if (b == null) return false;
        return ((Number) a).intValue() == b;
    }

    /** 触发事务回滚 + 外部回查的信号。 */
    private static final class ReplayDetectedException extends RuntimeException {
        ReplayDetectedException(String requestId) {
            super("requestId 重复：" + requestId);
        }
    }
}

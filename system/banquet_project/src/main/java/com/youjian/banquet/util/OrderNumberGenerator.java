package com.youjian.banquet.util;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * 订单号生成工具类
 * 硬约束：订单号使用固定前缀 YHTC-
 * 格式：YHTC-yyyyMMdd-XXXX（XXXX 为当日序号，每日重置）
 * 注意：前端生成的订单号仅供预览，数据库存储使用后端自增值
 */
@Component
public class OrderNumberGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private final JdbcTemplate jdbc;

    public OrderNumberGenerator(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * 生成门店级并发安全业务单号。
     * 格式：{TYPE}-{STORE}-{yyyyMMdd}-{000001}。
     * 通过 INSERT ... ON DUPLICATE KEY UPDATE + LAST_INSERT_ID 保证多实例部署下不重号。
     */
    @Transactional
    public String generate(String businessType, Long storeId) {
        String type = sanitizeType(businessType);
        long tenantId = storeId == null ? 0L : storeId;
        String day = LocalDate.now().format(DATE_FORMAT);
        String sequenceKey = type + ":" + tenantId + ":" + day;

        jdbc.update("INSERT INTO business_number_sequence(sequence_key,current_value) VALUES (?,LAST_INSERT_ID(1)) " +
                "ON DUPLICATE KEY UPDATE current_value=LAST_INSERT_ID(current_value+1)", sequenceKey);
        Long sequence = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        if (sequence == null || sequence < 1) {
            throw new IllegalStateException("业务单号序列生成失败: " + sequenceKey);
        }
        return String.format("%s-%03d-%s-%06d", type, tenantId, day, sequence);
    }

    public String generateOrder(Long storeId) {
        return generate("YHTC", storeId);
    }

    private String sanitizeType(String value) {
        if (value == null || value.isBlank()) return "YHTC";
        String normalized = value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
        return normalized.isBlank() ? "YHTC" : normalized.substring(0, Math.min(normalized.length(), 12));
    }
}

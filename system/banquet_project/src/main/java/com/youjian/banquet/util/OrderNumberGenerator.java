package com.youjian.banquet.util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 订单号生成工具类
 * 硬约束：订单号使用固定前缀 YHTC-
 * 格式：YHTC-yyyyMMdd-XXXX（XXXX 为当日序号，每日重置）
 * 注意：前端生成的订单号仅供预览，数据库存储使用后端自增值
 */
public class OrderNumberGenerator {

    private static final String PREFIX = "YHTC-";
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final AtomicInteger counter = new AtomicInteger(0);
    private static volatile String lastDate = "";

    /**
     * 生成订单号：YHTC-yyyyMMdd-XXXX
     */
    public static synchronized String generate() {
        String today = LocalDate.now().format(DATE_FORMAT);
        if (!today.equals(lastDate)) {
            lastDate = today;
            counter.set(0);
        }
        int seq = counter.incrementAndGet();
        return PREFIX + today + "-" + String.format("%04d", seq);
    }

    /**
     * 生成带随机后缀的订单号（避免并发冲突）
     */
    public static String generateWithRandom() {
        String today = LocalDate.now().format(DATE_FORMAT);
        int random = (int) (System.currentTimeMillis() % 10000);
        return PREFIX + today + "-" + String.format("%04d", random);
    }
}

package com.youjian.banquet.config;

import com.youjian.banquet.util.AESUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * P1-14 数据加密初始化器
 * 应用启动时自动检测并加密数据库中的明文银行账号/身份证号
 * 仅加密未带 ENC: 前缀的明文数据，已加密的跳过
 */
@Component
public class DataEncryptionInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataEncryptionInitializer.class);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private AESUtil aesUtil;

    @Override
    public void run(String... args) {
        int total = 0;
        total += encryptPlaintext("finance_account", "account_id", "bank_account");
        total += encryptPlaintext("staff_master", "staff_id", "bank_account");
        total += encryptPlaintext("staff_master", "staff_id", "id_card");
        total += encryptPlaintext("supplier_master", "supplier_id", "bank_account");
        total += encryptPlaintext("store_info", "store_id", "bank_account");
        if (total > 0) {
            log.info("[P1-14] 数据加密初始化完成，共加密 {} 条明文记录", total);
        }
    }

    /**
     * 加密指定表中指定列的明文数据
     * @return 加密的记录数
     */
    private int encryptPlaintext(String table, String idColumn, String dataColumn) {
        try {
            // 检查表是否存在
            List<Map<String, Object>> rows = jdbc.queryForList(
                    "SELECT `" + idColumn + "` AS id, `" + dataColumn + "` AS val FROM `" + table +
                    "` WHERE `" + dataColumn + "` IS NOT NULL AND `" + dataColumn + "` != '' " +
                    "AND `" + dataColumn + "` NOT LIKE 'ENC:%'");
            if (rows.isEmpty()) {
                return 0;
            }
            int count = 0;
            for (Map<String, Object> row : rows) {
                Object idVal = row.get("id");
                Object plainVal = row.get("val");
                if (plainVal == null || plainVal.toString().isEmpty()) {
                    continue;
                }
                String encrypted = aesUtil.encrypt(plainVal.toString());
                jdbc.update("UPDATE `" + table + "` SET `" + dataColumn + "` = ? WHERE `" + idColumn + "` = ?",
                        encrypted, idVal);
                count++;
            }
            if (count > 0) {
                log.info("[P1-14] {}.{} 加密 {} 条明文记录", table, dataColumn, count);
            }
            return count;
        } catch (Exception e) {
            log.warn("[P1-14] 加密 {}.{} 失败（可能表或列不存在）: {}", table, dataColumn, e.getMessage());
            return 0;
        }
    }
}

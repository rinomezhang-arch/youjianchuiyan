package com.youjian.banquet.config;

import com.youjian.banquet.util.AESUtil;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 银行账号 JPA 自动加解密转换器（P1-14）
 *
 * 在实体类的 bank_account 字段加 @Convert(converter = BankAccountConverter.class)
 * 即可实现：
 * - 写入数据库时自动加密（明文 → ENC:Base64...）
 * - 读取数据库时自动解密（ENC:Base64... → 明文）
 * - 兼容旧明文数据（无 ENC: 前缀的按明文返回）
 *
 * 涉及实体：FinanceAccount.bank_account / StaffMaster.bank_account / SupplierMaster.bank_account
 */
@Converter
@Component
public class BankAccountConverter implements AttributeConverter<String, String> {

    private static AESUtil aesUtilInstance;

    @Autowired
    private AESUtil aesUtil;

    @PostConstruct
    public void init() {
        BankAccountConverter.aesUtilInstance = this.aesUtil;
    }

    @Override
    public String convertToDatabaseColumn(String plaintext) {
        if (aesUtilInstance == null) {
            return plaintext; // Spring 未初始化时按明文存储（启动阶段）
        }
        return aesUtilInstance.encrypt(plaintext);
    }

    @Override
    public String convertToEntityAttribute(String ciphertext) {
        if (aesUtilInstance == null) {
            return ciphertext; // Spring 未初始化时按明文返回
        }
        return aesUtilInstance.decrypt(ciphertext);
    }
}

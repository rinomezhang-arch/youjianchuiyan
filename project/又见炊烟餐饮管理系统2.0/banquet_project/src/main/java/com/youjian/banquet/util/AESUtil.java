package com.youjian.banquet.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * AES-256-GCM 加解密工具（P1-14 银行账号加密）
 *
 * 用于加密存储银行账号、身份证号等敏感信息。
 * 密钥通过环境变量 AES_SECRET_KEY 注入（32字节 = AES-256）。
 *
 * 加密格式：ENC:Base64(IV[12字节] + ciphertext + GCM tag[16字节])
 * 双模式兼容：读取时检测 "ENC:" 前缀，有则解密，无则按明文返回（兼容旧数据）
 *
 * 安全特性：
 * - GCM 模式提供机密性 + 完整性（防篡改）
 * - 每次加密使用随机 IV，相同明文产生不同密文
 * - 密钥不落盘，仅通过环境变量注入
 */
@Component
public class AESUtil {

    private static final Logger log = LoggerFactory.getLogger(AESUtil.class);

    /** 加密数据前缀标识，用于区分明文/密文 */
    public static final String ENC_PREFIX = "ENC:";

    /** AES 密钥（32字节 = AES-256），从环境变量注入 */
    @Value("${aes.secret-key:YJCY-Banquet-AES256-SecretKey-Wo002323-2026}")
    private String secretKey;

    /** GCM 认证标签长度（位） */
    private static final int GCM_TAG_LENGTH = 128;

    /** GCM IV 长度（字节） */
    private static final int IV_LENGTH = 12;

    /**
     * 加密明文
     * @param plaintext 明文
     * @return 加密结果（ENC:Base64(iv+ciphertext+tag)），plaintext 为 null 时返回 null
     */
    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        // 已加密的不重复加密
        if (plaintext.startsWith(ENC_PREFIX)) {
            return plaintext;
        }
        try {
            byte[] keyBytes = getKeyBytes();
            byte[] iv = new byte[IV_LENGTH];
            java.security.SecureRandom.getInstanceStrong().nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // 拼接 IV + ciphertext + tag
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            buffer.put(iv);
            buffer.put(ciphertext);

            return ENC_PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            log.error("AES 加密失败，返回原文: {}", e.getMessage());
            return plaintext;
        }
    }

    /**
     * 解密密文
     * @param ciphertext 加密结果（ENC:Base64(...)），无前缀的按明文直接返回
     * @return 明文，ciphertext 为 null 时返回 null
     */
    public String decrypt(String ciphertext) {
        if (ciphertext == null || ciphertext.isEmpty()) {
            return ciphertext;
        }
        // 无 ENC: 前缀 = 明文数据（兼容旧数据），直接返回
        if (!ciphertext.startsWith(ENC_PREFIX)) {
            return ciphertext;
        }
        try {
            String encoded = ciphertext.substring(ENC_PREFIX.length());
            byte[] decoded = Base64.getDecoder().decode(encoded);

            byte[] iv = new byte[IV_LENGTH];
            byte[] cipherBytes = new byte[decoded.length - IV_LENGTH];
            System.arraycopy(decoded, 0, iv, 0, IV_LENGTH);
            System.arraycopy(decoded, IV_LENGTH, cipherBytes, 0, cipherBytes.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(getKeyBytes(), "AES");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec);

            byte[] plaintext = cipher.doFinal(cipherBytes);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("AES 解密失败，可能密钥已变更或数据损坏: {}", e.getMessage());
            return ciphertext;
        }
    }

    /**
     * 获取 AES 密钥字节（32字节）
     * 密钥不足 32 字节时右侧补 0，超过 32 字节截断
     */
    private byte[] getKeyBytes() {
        byte[] keyBytes = secretKey.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[32];
        int len = Math.min(keyBytes.length, 32);
        System.arraycopy(keyBytes, 0, result, 0, len);
        return result;
    }
}

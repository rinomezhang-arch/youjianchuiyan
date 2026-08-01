package com.youjian.banquet.util;

import org.springframework.stereotype.Component;

/**
 * 敏感数据脱敏工具
 * 用于返回前端时对银行账号、身份证号等敏感信息进行脱敏
 */
@Component
public class DataMaskUtil {

    /**
     * 银行账号脱敏：保留前4位和后4位，中间用 **** 替代
     * 例：6222021234567890 → 6222********7890
     */
    public String maskBankAccount(String account) {
        if (account == null || account.length() <= 8) {
            return "****";
        }
        String prefix = account.substring(0, 4);
        String suffix = account.substring(account.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * 身份证号脱敏：保留前3位和后4位，中间用 **** 替代
     * 例：342201199001011234 → 342**********1234
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() <= 7) {
            return "****";
        }
        String prefix = idCard.substring(0, 3);
        String suffix = idCard.substring(idCard.length() - 4);
        return prefix + "********" + suffix;
    }

    /**
     * 手机号脱敏：保留前3位和后4位
     * 例：13812345678 → 138****5678
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() <= 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}

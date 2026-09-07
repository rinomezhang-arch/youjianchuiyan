package com.youjian.banquet.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

/**
 * 企业微信回调消息签名校验 + AES 解密（WXBizMsgCrypt 方案）。
 * 参数细节来自官方文档"回调配置"/"加解密方案说明"（developer.work.weixin.qq.com/document/path/90930、91144），
 * 没有引入 WxJava 之类的第三方 SDK——协议本身不复杂，且 GET 验证环节本身就是对实现是否正确的现成自检
 * （算错了保存 URL 那一步就会直接失败，不会有"部分正确但线上偷偷出错"的风险）。
 */
public final class WeComCryptoUtil {

    private WeComCryptoUtil() {
    }

    /** sha1(sort(token, timestamp, nonce, encrypt)) —— 四个字符串按字典序排序后直接拼接，再取 SHA1 十六进制小写 */
    public static String sign(String token, String timestamp, String nonce, String encrypt) {
        String[] arr = {token, timestamp, nonce, encrypt};
        Arrays.sort(arr);
        String joined = String.join("", arr);
        try {
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            byte[] digest = sha1.digest(joined.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("sha1 sign failed", e);
        }
    }

    /**
     * 解密企业微信推送的密文（GET 验证时的 echostr，或 POST 消息体里的 Encrypt 字段），返回明文业务内容。
     * 明文结构：[16字节随机数][4字节消息长度，网络字节序][消息内容][receiveid]，PKCS7 填充到32字节的倍数。
     */
    public static String decrypt(String encodingAesKey, String encryptedBase64) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
            byte[] iv = Arrays.copyOfRange(aesKey, 0, 16);
            byte[] cipherBytes = Base64.getDecoder().decode(encryptedBase64);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
            byte[] padded = cipher.doFinal(cipherBytes);

            // 去掉 PKCS7 填充：最后一个字节的值就是填充长度
            int padLen = padded[padded.length - 1] & 0xff;
            byte[] plain = Arrays.copyOfRange(padded, 0, padded.length - padLen);

            // 跳过前16字节随机数，接着4字节大端序消息长度，再取出对应长度的消息正文
            byte[] msgLenBytes = Arrays.copyOfRange(plain, 16, 20);
            int msgLen = ((msgLenBytes[0] & 0xff) << 24) | ((msgLenBytes[1] & 0xff) << 16)
                    | ((msgLenBytes[2] & 0xff) << 8) | (msgLenBytes[3] & 0xff);
            return new String(plain, 20, msgLen, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("wecom decrypt failed", e);
        }
    }

    /**
     * 解密"智能机器人"URL回调模式下，图片/语音等媒体消息里 image.url 下载下来的密文字节。
     * <p>
     * 官方文档（developer.work.weixin.qq.com/document/path/101463）：URL回调模式的媒体文件用的是
     * 和消息正文解密同一把 EncodingAESKey（不是长连接模式那种"每个URL独立aeskey"），AES-256-CBC，
     * PKCS7 填充到32字节的倍数，IV是AESKey前16字节——跟 {@link #decrypt} 用的是同一套算法，
     * 区别只在于：媒体文件解密后不需要再剥"16字节随机数+4字节长度+receiveid"这个文本消息专属的包装，
     * 解密+去padding之后就是原始文件字节本身。
     */
    public static byte[] decryptMediaBytes(String encodingAesKey, byte[] cipherBytes) {
        try {
            byte[] aesKey = Base64.getDecoder().decode(encodingAesKey + "=");
            byte[] iv = Arrays.copyOfRange(aesKey, 0, 16);

            Cipher cipher = Cipher.getInstance("AES/CBC/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey, "AES"), new IvParameterSpec(iv));
            byte[] padded = cipher.doFinal(cipherBytes);

            int padLen = padded[padded.length - 1] & 0xff;
            if (padLen < 1 || padLen > 32 || padLen > padded.length) {
                // padding字节不合法，说明密文本身就不是标准PKCS7填充过的（比如根本没加密、或者密钥不对），
                // 原样返回未去padding的内容，交给调用方按"看起来不像图片"处理，而不是在这里抛异常掩盖真相
                return padded;
            }
            return Arrays.copyOfRange(padded, 0, padded.length - padLen);
        } catch (Exception e) {
            throw new RuntimeException("wecom media decrypt failed", e);
        }
    }
}

package com.youjian.banquet.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 腾讯云 COS 配置
 */
@Configuration
public class CosConfig {

    @Value("${cos.secret-id:}")
    private String secretId;

    @Value("${cos.secret-key:}")
    private String secretKey;

    @Value("${cos.region:ap-shanghai}")
    private String region;

    @Value("${cos.bucket:}")
    private String bucket;

    @Value("${cos.prefix:banquet/}")
    private String prefix;

    @Value("${cos.base-url:}")
    private String baseUrl;

    @Bean
    @ConditionalOnProperty(name = "cos.secret-id")
    public COSClient cosClient() {
        if (secretId == null || secretId.isEmpty()) {
            return null;
        }
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        return new COSClient(cred, clientConfig);
    }

    public String getBucket() { return bucket; }
    public String getPrefix() { return prefix; }
    public String getBaseUrl() { return baseUrl; }
    public String getRegion() { return region; }
}

package com.youjian.banquet.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpProtocol;
import com.qcloud.cos.region.Region;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 法务案卷专用 COS 客户端。
 *
 * 为什么不复用业务的 {@link CosConfig}：案卷证据存在另一个存储桶、另一个地域
 * （华东南京 vs 业务桶的上海），而且这个桶是私有读写，只用来出短时效的预签名
 * 链接，绝不放公开访问的业务附件。两者混用会让权限边界糊掉。
 *
 * 凭证优先取 LEGAL_COS_* 环境变量，没配则回落到业务的 COS_* —— 因为实践中
 * 两个桶在同一个腾讯云账号下，同一对密钥即可访问。
 */
@Configuration
public class LegalCosConfig {

    private static final Logger log = LoggerFactory.getLogger(LegalCosConfig.class);

    @Value("${legal.cos.secret-id:}")
    private String secretId;

    @Value("${legal.cos.secret-key:}")
    private String secretKey;

    @Value("${legal.cos.region:ap-nanjing}")
    private String region;

    @Value("${legal.cos.bucket:}")
    private String bucket;

    @Value("${legal.cos.root:}")
    private String root;

    @Value("${legal.cos.url-expire-seconds:900}")
    private int urlExpireSeconds;

    /**
     * 独立的 COS 客户端 Bean。名字必须区别于业务的 cosClient，注入时用 @Qualifier 取。
     * 凭证缺失时返回 null（Spring 允许 @Bean 返回 null，注入方按 required=false 处理），
     * 这样本地开发不配法务桶也能正常启动，只是案卷模块不可用。
     */
    @Bean(name = "legalCosClient")
    public COSClient legalCosClient() {
        if (secretId == null || secretId.isBlank() || secretKey == null || secretKey.isBlank()
                || bucket == null || bucket.isBlank()) {
            log.warn("[Legal] 法务 COS 未配置（legal.cos.secret-id / secret-key / bucket），案卷证据模块不可用");
            return null;
        }
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        ClientConfig cfg = new ClientConfig(new Region(region));
        cfg.setHttpProtocol(HttpProtocol.https);
        log.info("[Legal] 法务 COS 已就绪 bucket={} region={} root={}", bucket, region, root);
        return new COSClient(cred, cfg);
    }

    public String getBucket() { return bucket; }
    public String getRoot() { return root; }
    public String getRegion() { return region; }
    public int getUrlExpireSeconds() { return urlExpireSeconds; }
}

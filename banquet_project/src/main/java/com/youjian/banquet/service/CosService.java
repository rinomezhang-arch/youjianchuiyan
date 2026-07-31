package com.youjian.banquet.service;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.youjian.banquet.config.CosConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * 腾讯云 COS 上传服务
 * 硬约束：所有文件数据必须存入 COS，防止服务器本地磁盘无限膨胀
 */
@Service
public class CosService {

    @Autowired(required = false)
    private COSClient cosClient;

    @Autowired
    private CosConfig cosConfig;

    /**
     * 上传文件到 COS
     * @return 可访问的 URL
     */
    public String uploadFile(MultipartFile file) throws Exception {
        if (cosClient == null) {
            throw new RuntimeException("COS 未配置，请在环境变量中设置 COS_SECRET_ID 和 COS_SECRET_KEY");
        }

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String key = cosConfig.getPrefix() + UUID.randomUUID().toString().replace("-", "") + ext;

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putRequest = new PutObjectRequest(cosConfig.getBucket(), key, inputStream, metadata);
            cosClient.putObject(putRequest);
        }

        // 返回访问 URL
        String baseUrl = cosConfig.getBaseUrl();
        if (baseUrl != null && !baseUrl.isEmpty()) {
            return baseUrl + "/" + key;
        }
        // 默认 URL 格式
        return "https://" + cosConfig.getBucket() + ".cos." + cosConfig.getRegion() + ".myqcloud.com/" + key;
    }
}

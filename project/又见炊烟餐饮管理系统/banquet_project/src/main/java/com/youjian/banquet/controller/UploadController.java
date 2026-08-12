package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.CosService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * 文件上传控制器 - 所有文件存入腾讯云 COS
 * 硬约束：所有文件数据必须存入 COS，防止服务器本地磁盘无限膨胀
 */
@RestController
@RequestMapping("/api/upload")
@CrossOrigin
public class UploadController {

    @Autowired
    private CosService cosService;

    @PostMapping("/image")
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件为空");
        }

        // 文件大小校验（10MB）
        if (file.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "文件大小不能超过 10MB");
        }

        // 文件类型校验
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "仅支持图片文件");
        }

        try {
            String url = cosService.uploadFile(file);
            String originalFilename = file.getOriginalFilename();
            String filename = url.substring(url.lastIndexOf("/") + 1);

            Map<String, String> result = new HashMap<>();
            result.put("filename", filename);
            result.put("url", url);
            result.put("original_name", originalFilename);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error(500, "文件上传失败: " + e.getMessage());
        }
    }
}

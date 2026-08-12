package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.service.CostMenuImportService;
import jakarta.validation.constraints.Min;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/cost-menu-imports")
public class CostMenuImportController {
    private final CostMenuImportService importService;

    public CostMenuImportController(CostMenuImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, Object>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "1") @Min(1) long storeId,
            @RequestParam(defaultValue = "system") String operator) {
        try {
            return Result.success(importService.importWorkbook(file, storeId, operator));
        } catch (IllegalArgumentException ex) {
            return Result.error(400, ex.getMessage());
        } catch (Exception ex) {
            return Result.error(500, "导入失败：" + ex.getMessage());
        }
    }

    @GetMapping("/{batchId}")
    public Result<Map<String, Object>> detail(@PathVariable long batchId) {
        try {
            return Result.success(importService.batchSummary(batchId, false));
        } catch (Exception ex) {
            return Result.error(404, "导入批次不存在");
        }
    }
}

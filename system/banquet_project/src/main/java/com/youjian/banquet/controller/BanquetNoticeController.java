package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BanquetNotice;
import com.youjian.banquet.service.BanquetNoticeService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/banquet-notices")
public class BanquetNoticeController {
    private final BanquetNoticeService service;

    public BanquetNoticeController(BanquetNoticeService service) {
        this.service = service;
    }

    @GetMapping
    public Result<List<BanquetNotice>> list(@RequestParam(required = false) Long storeId,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status,
                                            @RequestParam(required = false) LocalDate startDate,
                                            @RequestParam(required = false) LocalDate endDate) {
        return Result.success(service.search(storeId, keyword, status, startDate, endDate));
    }

    @GetMapping("/{id}")
    public Result<BanquetNotice> detail(@PathVariable Long id,
                                        @RequestParam(required = false) Long storeId) {
        return Result.success(service.get(id, storeId));
    }

    @PostMapping
    public Result<BanquetNotice> create(@RequestBody BanquetNotice notice) {
        return Result.success(service.create(notice));
    }

    @PutMapping("/{id}")
    public Result<BanquetNotice> update(@PathVariable Long id, @RequestBody BanquetNotice notice) {
        return Result.success(service.update(id, notice));
    }

    @PostMapping("/{id}/copy")
    public Result<BanquetNotice> copy(@PathVariable Long id,
                                      @RequestParam(required = false) Long storeId) {
        return Result.success(service.copy(id, storeId));
    }

    @PostMapping("/{id}/transition")
    public Result<BanquetNotice> transition(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        Long storeId = body.get("storeId") == null ? null : Long.valueOf(body.get("storeId").toString());
        String status = body.get("status") == null ? "" : body.get("status").toString();
        return Result.success(service.transition(id, storeId, status));
    }

    @PostMapping("/{id}/scan")
    public Result<BanquetNotice> attachScan(@PathVariable Long id,
                                            @RequestBody Map<String, Object> body) {
        Long storeId = body.get("storeId") == null ? null : Long.valueOf(body.get("storeId").toString());
        String url = body.get("url") == null ? null : body.get("url").toString();
        String name = body.get("name") == null ? null : body.get("name").toString();
        return Result.success(service.attachScan(id, storeId, url, name));
    }
}

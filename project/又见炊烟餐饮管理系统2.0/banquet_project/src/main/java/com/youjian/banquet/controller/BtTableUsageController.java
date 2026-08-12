package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtTableUsage;
import com.youjian.banquet.service.BtTableUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐桌使用控制器
 * 来源：点餐系统 CanzhuoshiyongController
 */
@RestController
@RequestMapping("/api/bt/table-usage")
@CrossOrigin(origins = "*")
public class BtTableUsageController {

    @Autowired
    private BtTableUsageService btTableUsageService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) String yonghuming,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtTableUsage> pageResult;
        if (yonghuming != null) {
            pageResult = btTableUsageService.pageByUser(yonghuming, page, size, storeId);
        } else {
            pageResult = btTableUsageService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtTableUsage>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(btTableUsageService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<BtTableUsage> info(@PathVariable Long id) {
        return btTableUsageService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌使用记录不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtTableUsage> detail(@PathVariable Long id) {
        return btTableUsageService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌使用记录不存在"));
    }

    @PostMapping("/save")
    public Result<BtTableUsage> save(@RequestBody BtTableUsage entity) {
        return Result.success(btTableUsageService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtTableUsage> add(@RequestBody BtTableUsage entity) {
        return Result.success(btTableUsageService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtTableUsage> update(@RequestBody BtTableUsage entity) {
        return Result.success(btTableUsageService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btTableUsageService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btTableUsageService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
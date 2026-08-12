package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtTableUsage;
import com.youjian.banquet.service.BtTableUsageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/canzhuoshiyong")
@CrossOrigin(origins = "*")
public class CanzhuoshiyongController {

    @Autowired
    private BtTableUsageService btTableUsageService;

    @RequestMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(defaultValue = "id") String sort,
                                            @RequestParam(defaultValue = "desc") String order,
                                            @RequestParam(required = false) String yonghuming,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtTableUsage> pageResult;
        if (yonghuming != null) {
            pageResult = btTableUsageService.pageByUser(yonghuming, page, limit, storeId);
        } else {
            pageResult = btTableUsageService.page(page, limit, sort, order, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam Map<String, Object> params,
                                            @RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int limit,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtTableUsage> pageResult = btTableUsageService.page(page, limit, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "limit", limit
        ));
    }

    @RequestMapping("/lists")
    public Result<List<BtTableUsage>> lists(BtTableUsage query) {
        return Result.success(btTableUsageService.listAll(null));
    }

    @RequestMapping("/query")
    public Result<BtTableUsage> query(BtTableUsage query) {
        if (query.getId() != null) {
            return btTableUsageService.getById(query.getId())
                    .map(Result::success)
                    .orElse(Result.error(404, "餐桌使用记录不存在"));
        }
        return Result.success(null);
    }

    @RequestMapping("/info/{id}")
    public Result<BtTableUsage> info(@PathVariable("id") Long id) {
        return btTableUsageService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌使用记录不存在"));
    }

    @RequestMapping("/detail/{id}")
    public Result<BtTableUsage> detail(@PathVariable("id") Long id) {
        return btTableUsageService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐桌使用记录不存在"));
    }

    @RequestMapping("/save")
    public Result<BtTableUsage> save(@RequestBody BtTableUsage entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btTableUsageService.save(entity));
    }

    @RequestMapping("/add")
    public Result<BtTableUsage> add(@RequestBody BtTableUsage entity) {
        if (entity.getId() == null) {
            entity.setId(System.currentTimeMillis() + (long)(Math.random() * 1000));
        }
        return Result.success(btTableUsageService.save(entity));
    }

    @RequestMapping("/update")
    public Result<BtTableUsage> update(@RequestBody BtTableUsage entity) {
        return Result.success(btTableUsageService.update(entity));
    }

    @RequestMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btTableUsageService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @RequestMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable("columnName") String columnName,
                                                   @PathVariable("type") String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btTableUsageService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}

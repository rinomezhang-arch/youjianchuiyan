package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtStoreup;
import com.youjian.banquet.service.BtStoreupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏控制器
 * 来源：点餐系统 StoreupController
 */
@RestController
@RequestMapping("/api/bt/storeup")
@CrossOrigin(origins = "*")
public class BtStoreupController {

    @Autowired
    private BtStoreupService btStoreupService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long userid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtStoreup> pageResult;
        if (userid != null) {
            pageResult = btStoreupService.pageByUser(userid, page, size, storeId);
        } else {
            pageResult = btStoreupService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtStoreup>> list(@RequestParam(required = false) Long userid) {
        if (userid != null) {
            return Result.success(btStoreupService.listByUser(userid));
        }
        return Result.success(List.of());
    }

    @GetMapping("/info/{id}")
    public Result<BtStoreup> info(@PathVariable Long id) {
        return btStoreupService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "收藏记录不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtStoreup> detail(@PathVariable Long id) {
        return btStoreupService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "收藏记录不存在"));
    }

    @PostMapping("/save")
    public Result<BtStoreup> save(@RequestBody BtStoreup entity) {
        return Result.success(btStoreupService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtStoreup> add(@RequestBody BtStoreup entity) {
        return Result.success(btStoreupService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtStoreup> update(@RequestBody BtStoreup entity) {
        return Result.success(btStoreupService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btStoreupService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btStoreupService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
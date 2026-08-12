package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.DishStoreup;
import com.youjian.banquet.service.DishStoreupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 收藏控制器
 */
@RestController
@RequestMapping("/api/dish/storeup")
@CrossOrigin(origins = "*")
public class DishStoreupController {

    @Autowired
    private DishStoreupService dishStoreupService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<DishStoreup> pageResult = dishStoreupService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<DishStoreup>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(dishStoreupService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<DishStoreup> info(@PathVariable Long id) {
        return dishStoreupService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "收藏记录不存在"));
    }

    @PostMapping("/save")
    public Result<DishStoreup> save(@RequestBody DishStoreup entity) {
        return Result.success(dishStoreupService.save(entity));
    }

    @PutMapping("/update")
    public Result<DishStoreup> update(@RequestBody DishStoreup entity) {
        return Result.success(dishStoreupService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        dishStoreupService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = dishStoreupService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
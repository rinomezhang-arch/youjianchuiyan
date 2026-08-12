package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.DishCart;
import com.youjian.banquet.service.DishCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 购物车控制器
 */
@RestController
@RequestMapping("/api/dish/cart")
@CrossOrigin(origins = "*")
public class DishCartController {

    @Autowired
    private DishCartService dishCartService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<DishCart> pageResult = dishCartService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<DishCart>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(dishCartService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<DishCart> info(@PathVariable Long id) {
        return dishCartService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "购物车记录不存在"));
    }

    @PostMapping("/save")
    public Result<DishCart> save(@RequestBody DishCart entity) {
        return Result.success(dishCartService.save(entity));
    }

    @PutMapping("/update")
    public Result<DishCart> update(@RequestBody DishCart entity) {
        return Result.success(dishCartService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        dishCartService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = dishCartService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.DishType;
import com.youjian.banquet.service.DishTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜品类型控制器
 */
@RestController
@RequestMapping("/api/dish/type")
@CrossOrigin(origins = "*")
public class DishTypeController {

    @Autowired
    private DishTypeService dishTypeService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<DishType> pageResult = dishTypeService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<DishType>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(dishTypeService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<DishType> info(@PathVariable Long id) {
        return dishTypeService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品类型不存在"));
    }

    @PostMapping("/save")
    public Result<DishType> save(@RequestBody DishType entity) {
        return Result.success(dishTypeService.save(entity));
    }

    @PutMapping("/update")
    public Result<DishType> update(@RequestBody DishType entity) {
        return Result.success(dishTypeService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        dishTypeService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = dishTypeService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
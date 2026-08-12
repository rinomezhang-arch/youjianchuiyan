package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.DishOrder;
import com.youjian.banquet.service.DishOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 订单控制器
 */
@RestController
@RequestMapping("/api/dish/order")
@CrossOrigin(origins = "*")
public class DishOrderController {

    @Autowired
    private DishOrderService dishOrderService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<DishOrder> pageResult = dishOrderService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<DishOrder>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(dishOrderService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<DishOrder> info(@PathVariable Long id) {
        return dishOrderService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "订单不存在"));
    }

    @PostMapping("/save")
    public Result<DishOrder> save(@RequestBody DishOrder entity) {
        return Result.success(dishOrderService.save(entity));
    }

    @PutMapping("/update")
    public Result<DishOrder> update(@RequestBody DishOrder entity) {
        return Result.success(dishOrderService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        dishOrderService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = dishOrderService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
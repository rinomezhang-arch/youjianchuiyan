package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.RestaurantNews;
import com.youjian.banquet.service.RestaurantNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 餐厅资讯控制器
 */
@RestController
@RequestMapping("/api/dish/news")
@CrossOrigin(origins = "*")
public class RestaurantNewsController {

    @Autowired
    private RestaurantNewsService restaurantNewsService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<RestaurantNews> pageResult = restaurantNewsService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<RestaurantNews>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(restaurantNewsService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<RestaurantNews> info(@PathVariable Long id) {
        return restaurantNewsService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "餐厅资讯不存在"));
    }

    @PostMapping("/save")
    public Result<RestaurantNews> save(@RequestBody RestaurantNews entity) {
        return Result.success(restaurantNewsService.save(entity));
    }

    @PutMapping("/update")
    public Result<RestaurantNews> update(@RequestBody RestaurantNews entity) {
        return Result.success(restaurantNewsService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        restaurantNewsService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = restaurantNewsService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.DishReview;
import com.youjian.banquet.service.DishReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜品评论控制器
 */
@RestController
@RequestMapping("/api/dish/review")
@CrossOrigin(origins = "*")
public class DishReviewController {

    @Autowired
    private DishReviewService dishReviewService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long storeId) {
        Page<DishReview> pageResult = dishReviewService.page(page, size, sortField, sortOrder, storeId);
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<DishReview>> list(@RequestParam(required = false) Long storeId) {
        return Result.success(dishReviewService.listAll(storeId));
    }

    @GetMapping("/info/{id}")
    public Result<DishReview> info(@PathVariable Long id) {
        return dishReviewService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "菜品评论不存在"));
    }

    @PostMapping("/save")
    public Result<DishReview> save(@RequestBody DishReview entity) {
        return Result.success(dishReviewService.save(entity));
    }

    @PutMapping("/update")
    public Result<DishReview> update(@RequestBody DishReview entity) {
        return Result.success(dishReviewService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        dishReviewService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = dishReviewService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}
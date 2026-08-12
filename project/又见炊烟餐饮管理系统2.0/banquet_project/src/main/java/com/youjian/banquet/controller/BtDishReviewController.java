package com.youjian.banquet.controller;

import com.youjian.banquet.common.Result;
import com.youjian.banquet.entity.BtDishReview;
import com.youjian.banquet.service.BtDishReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 菜品评论控制器
 * 来源：点餐系统 DiscusscaipinxinxiController
 */
@RestController
@RequestMapping("/api/bt/dish-review")
@CrossOrigin(origins = "*")
public class BtDishReviewController {

    @Autowired
    private BtDishReviewService btDishReviewService;

    @GetMapping("/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") int page,
                                            @RequestParam(defaultValue = "10") int size,
                                            @RequestParam(defaultValue = "id") String sortField,
                                            @RequestParam(defaultValue = "desc") String sortOrder,
                                            @RequestParam(required = false) Long refid,
                                            @RequestParam(required = false) Long storeId) {
        Page<BtDishReview> pageResult;
        if (refid != null) {
            pageResult = btDishReviewService.pageByDish(refid, page, size, storeId);
        } else {
            pageResult = btDishReviewService.page(page, size, sortField, sortOrder, storeId);
        }
        return Result.success(Map.of(
                "list", pageResult.getContent(),
                "total", pageResult.getTotalElements(),
                "page", page,
                "size", size
        ));
    }

    @GetMapping("/list")
    public Result<List<BtDishReview>> list(@RequestParam(required = false) Long refid) {
        if (refid != null) {
            return Result.success(btDishReviewService.listByDish(refid));
        }
        return Result.success(List.of());
    }

    @GetMapping("/info/{id}")
    public Result<BtDishReview> info(@PathVariable Long id) {
        return btDishReviewService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "评论不存在"));
    }

    @GetMapping("/detail/{id}")
    public Result<BtDishReview> detail(@PathVariable Long id) {
        return btDishReviewService.getById(id)
                .map(Result::success)
                .orElse(Result.error(404, "评论不存在"));
    }

    @PostMapping("/save")
    public Result<BtDishReview> save(@RequestBody BtDishReview entity) {
        return Result.success(btDishReviewService.save(entity));
    }

    @PostMapping("/add")
    public Result<BtDishReview> add(@RequestBody BtDishReview entity) {
        return Result.success(btDishReviewService.save(entity));
    }

    @PutMapping("/update")
    public Result<BtDishReview> update(@RequestBody BtDishReview entity) {
        return Result.success(btDishReviewService.update(entity));
    }

    @DeleteMapping("/delete")
    public Result<String> delete(@RequestBody Long[] ids) {
        btDishReviewService.deleteBatch(List.of(ids));
        return Result.success("删除成功");
    }

    @GetMapping("/remind/{columnName}/{type}")
    public Result<Map<String, Object>> remindCount(@PathVariable String columnName,
                                                   @PathVariable String type,
                                                   @RequestParam Map<String, Object> map,
                                                   @RequestParam(required = false) Long storeId) {
        long count = btDishReviewService.count(storeId);
        return Result.success(Map.of("count", count));
    }
}